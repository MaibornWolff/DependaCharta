# Frontend Edge Modeling Analysis

## Problem Statement

The current frontend edge model does not distinguish between:
1. **Leaf-to-leaf edges** - Original dependencies from the backend
2. **Collapsed edges** - Aggregated edges when packages are collapsed

This makes it impossible to correctly track `isPointingUpwards` when edges are aggregated during visualization.

## Current Architecture Issues

### 1. No Edge Type Distinction

**Current `ShallowEdge`** ([`Edge.ts:58-71`](visualization/src/app/model/Edge.ts:58-71)):
```typescript
export class ShallowEdge {
  constructor(
    readonly source: string,      // Could be leaf OR package ID
    readonly target: string,      // Could be leaf OR package ID
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string
  ) {}
}
```

**Missing:**
- `isPointingUpwards: boolean` from backend
- Indication of whether this is a leaf-to-leaf or aggregated edge
- Reference to original leaf edges (for aggregated edges)

### 2. No Provenance Tracking

**Current `Edge`** ([`Edge.ts:3-11`](visualization/src/app/model/Edge.ts:3-11)):
```typescript
export class Edge {
  constructor(
    readonly source: VisibleGraphNode,  // Current visualization state
    readonly target: VisibleGraphNode,  // Current visualization state
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string
  ) {}
}
```

**Missing:**
- Reference to the source `ShallowEdge`(s)
- Knowledge of whether this represents one or many original edges
- Ability to query original `isPointingUpwards` values

### 3. Incorrect `isPointingUpwards()` Calculation

**Current implementation** ([`Edge.ts:25-28`](visualization/src/app/model/Edge.ts:25-28)):
```typescript
isPointingUpwards(): boolean {
  const [sourceNode, targetNode] = findSiblingsUnderLowestCommonAncestor(this.source, this.target)
  return sourceNode.level <= targetNode.level
}
```

**Problems:**
- Calculates based on CURRENT visualization state (collapsed/expanded)
- Ignores backend's pre-calculated values
- Cannot handle aggregated edges correctly
- Recalculates on every call (performance issue)

## Backend Data Structure

The backend provides TWO types of edges in the JSON:

### 1. Leaf-Level Dependencies
Located in `leaves[leafId].dependencies`:
```json
{
  "leaves": {
    "de.sots.domain.model.ArmorClass": {
      "dependencies": {
        "de.sots.application.CreatureUtil": {
          "weight": 1,
          "isCyclic": false,
          "type": "USES",
          "isPointingUpwards": true  // ← Backend calculated this!
        }
      }
    }
  }
}
```

### 2. Package-Level Aggregated Dependencies
Located in `projectTreeRoots[].containedInternalDependencies`:
```json
{
  "projectTreeRoots": [
    {
      "name": "de",
      "level": 0,
      "containedInternalDependencies": {
        "de.sots.application": {
          "weight": 2,
          "isCyclic": false,
          "type": "USES",
          "isPointingUpwards": false  // ← This is the DEFAULT value, not calculated!
        }
      }
    }
  ]
}
```

**Key Insight:** The backend's `isPointingUpwards` for aggregated edges is NOT meaningful because it's just the default value. The aggregation loses the information about which original edges were pointing upwards.

## Proposed Solution: Two-Tier Edge Model

### Tier 1: Enhanced `ShallowEdge`

```typescript
export class ShallowEdge {
  constructor(
    readonly source: string,
    readonly target: string,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly isPointingUpwards: boolean,  // NEW: From backend
    readonly isLeafToLeaf: boolean,       // NEW: True if both endpoints are leaves
    readonly originalEdges?: ShallowEdge[] // NEW: For aggregated edges, track originals
  ) {}
}
```

**Benefits:**
- Preserves backend's `isPointingUpwards` calculation
- Distinguishes leaf-to-leaf from aggregated edges
- Maintains provenance chain for aggregated edges

### Tier 2: Enhanced `Edge`

```typescript
export class Edge {
  constructor(
    readonly source: VisibleGraphNode,
    readonly target: VisibleGraphNode,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly sourceEdges: ShallowEdge[]  // NEW: Track which ShallowEdges this represents
  ) {}

  isPointingUpwards(): boolean {
    // If ANY source edge points upwards, this edge points upwards
    return this.sourceEdges.some(edge => edge.isPointingUpwards)
  }

  hasUpwardComponent(): boolean {
    // Check if any contributing edge points upwards
    return this.sourceEdges.some(edge => edge.isPointingUpwards)
  }

  hasDownwardComponent(): boolean {
    // Check if any contributing edge points downwards
    return this.sourceEdges.some(edge => !edge.isPointingUpwards)
  }

  isMixed(): boolean {
    // True if this collapsed edge contains both upward and downward edges
    return this.hasUpwardComponent() && this.hasDownwardComponent()
  }
}
```

**Benefits:**
- Always knows which original edges it represents
- Can answer questions about edge composition
- Enables proper visualization of mixed edges
- No runtime calculation needed

## Implementation Strategy

### Phase 1: Update Data Loading

1. **Modify `EdgeMetaInformation`** in [`ProjectReport.ts:18-22`](visualization/src/app/adapter/analysis/internal/ProjectReport.ts:18-22):
```typescript
export interface EdgeMetaInformation {
  weight: number
  isCyclic: boolean
  type: string
  isPointingUpwards?: boolean  // NEW: Optional for backward compatibility
}
```

2. **Update `toShallowEdge()`** in [`ProjectNodeConverter.ts:35-50`](visualization/src/app/adapter/analysis/ProjectNodeConverter.ts:35-50):
```typescript
function toShallowEdge(
  nodeId: string, 
  rawDependencies: Record<string, EdgeMetaInformation>,
  isLeafNode: boolean  // NEW: Know if source is a leaf
): ShallowEdge[] {
  const edges: ShallowEdge[] = []
  for (const targetNodeId in rawDependencies) {
    const edgeMetaInformation = rawDependencies[targetNodeId]
    const leafId = LeafIdCreator.createFrom(targetNodeId)
    
    // Determine if target is a leaf (check if it exists in leaves map)
    const isTargetLeaf = /* check if targetNodeId is in leaves */
    const isLeafToLeaf = isLeafNode && isTargetLeaf
    
    edges.push(new ShallowEdge(
      nodeId,
      leafId,
      nodeId + "-" + leafId,
      edgeMetaInformation.weight,
      edgeMetaInformation.isCyclic,
      edgeMetaInformation.type,
      edgeMetaInformation.isPointingUpwards ?? false,  // NEW
      isLeafToLeaf  // NEW
    ))
  }
  return edges
}
```

### Phase 2: Update Edge Creation

**Modify `createEdgesForNode()`** in [`GraphNode.ts:108-123`](visualization/src/app/model/GraphNode.ts:108-123):
```typescript
static createEdgesForNode(
  node: VisibleGraphNode, 
  visibleNodes: VisibleGraphNode[], 
  hiddenNodeIds: string[]
): Edge[] {
  return node.dependencies.flatMap(dependency => {
    const bestTarget = VisibleGraphNodeUtils.findBestDependencyTarget(
      dependency.target, 
      visibleNodes, 
      hiddenNodeIds
    )
    
    if (bestTarget && !IdUtils.isIncludedIn(bestTarget.id, node.id)) {
      return new Edge(
        node,
        bestTarget,
        node.id + "-" + bestTarget.id,
        dependency.weight,
        dependency.isCyclic,
        dependency.type,
        [dependency]  // NEW: Track source edge
      )
    }
    return []
  })
}
```

### Phase 3: Handle Edge Aggregation

**Update `aggregateEdges()`** in [`Edge.ts:30-55`](visualization/src/app/model/Edge.ts:30-55):
```typescript
static aggregateEdges(edges: Edge[], shouldAggregateEdges: boolean): Edge[] {
  const aggregatedEdges = new Map<string, Edge>()

  edges.forEach(edge => {
    const key = shouldAggregateEdges
      ? edge.id
      : `${edge.id}-${edge.isCyclic}`
    const duplicateEdge = aggregatedEdges.get(key)

    let aggregatedEdge: Edge
    if (duplicateEdge) {
      aggregatedEdge = duplicateEdge.copy({
        weight: duplicateEdge.weight + edge.weight,
        isCyclic: shouldAggregateEdges
          ? duplicateEdge.isCyclic || edge.isCyclic
          : edge.isCyclic,
        sourceEdges: [...duplicateEdge.sourceEdges, ...edge.sourceEdges]  // NEW: Merge sources
      })
    } else {
      aggregatedEdge = edge.copy({id: key})
    }

    aggregatedEdges.set(key, aggregatedEdge)
  })

  return [...aggregatedEdges.values()]
}
```

## Benefits of This Approach

1. **Correctness**: Preserves backend's `isPointingUpwards` calculation
2. **Transparency**: Always know which original edges contribute to a visualization edge
3. **Flexibility**: Can implement different visualization strategies for mixed edges
4. **Performance**: No runtime graph traversal needed
5. **Backward Compatibility**: Can handle old JSON files without `isPointingUpwards`
6. **Debuggability**: Can trace any edge back to its origins

## Visualization Implications

With this model, you can implement sophisticated edge visualization:

### Option 1: Conservative (Current Behavior)
```typescript
isPointingUpwards(): boolean {
  // Edge points upward if ANY component points upward
  return this.sourceEdges.some(edge => edge.isPointingUpwards)
}
```

### Option 2: Strict
```typescript
isPointingUpwards(): boolean {
  // Edge points upward only if ALL components point upward
  return this.sourceEdges.every(edge => edge.isPointingUpwards)
}
```

### Option 3: Mixed Edge Visualization
```typescript
getEdgeType(): EdgeType {
  const hasUpward = this.hasUpwardComponent()
  const hasDownward = this.hasDownwardComponent()
  
  if (hasUpward && hasDownward) {
    return EdgeType.MIXED  // Could be visualized with special styling
  } else if (hasUpward) {
    return EdgeType.UPWARD
  } else {
    return EdgeType.DOWNWARD
  }
}
```

## Next Steps

1. **Decide on visualization strategy** for mixed edges
2. **Update `ShallowEdge` class** with new fields
3. **Update `Edge` class** with source tracking
4. **Modify data loading** to populate new fields
5. **Update edge creation** to track sources
6. **Update edge aggregation** to merge sources
7. **Add tests** for edge composition queries
8. **Update visualization** to use new edge types

## Related TODOs in Code

The code already has TODOs pointing to this issue ([`Edge.ts:17-24`](visualization/src/app/model/Edge.ts:17-24)):

```typescript
// TODO `Edge` should have a property `isPointingUpwards: boolean`
// It should be set when when Edge is created in `toGraphEdges`
// TODO `Edge` should have a function `getType(): EdgeType`
// !isCyclic && !isPointingUpwards => REGULAR
// isCyclic && !isPointingUpwards => CYCLIC
// !isCyclic && isPointingUpwards => TWISTED
// isCyclic && isPointingUpwards => FEEDBACK
// TODO (next) `EdgePredicate`, `EdgeFilter`, `EdgeFilterResult` can be removed
```

This analysis provides the foundation for implementing these TODOs correctly.