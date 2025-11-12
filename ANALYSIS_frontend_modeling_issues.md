# Frontend Modeling Issues: Edges AND Nodes

## Executive Summary

You are **absolutely correct** - there's a fundamental modeling issue in the frontend. The problem exists for BOTH edges and nodes:

1. **`ShallowEdge`** should ALWAYS represent **leaf-to-leaf edges** from the backend JSON
2. **`Edge`** represents **visualization edges** (which may aggregate multiple `ShallowEdge`s)
3. **Similar issue exists with nodes**: `GraphNode` vs `VisibleGraphNode`

The current implementation conflates these concepts, losing critical information during visualization transformations.

## The Core Insight

### What the Backend Provides

The backend JSON has a clear structure:

```json
{
  "leaves": {
    "leaf.A": {
      "dependencies": {
        "leaf.B": {
          "weight": 1,
          "isCyclic": false,
          "type": "USES",
          "isPointingUpwards": true  // ← ALWAYS leaf-to-leaf!
        }
      }
    }
  },
  "projectTreeRoots": [
    {
      "name": "package",
      "containedInternalDependencies": {
        "other.package": {
          "weight": 5,  // ← Aggregated from multiple leaf edges
          "isCyclic": false,
          "type": "USES",
          "isPointingUpwards": false  // ← Default value, NOT calculated!
        }
      }
    }
  ]
}
```

**Key insight:** 
- `leaves[].dependencies` = **ALWAYS leaf-to-leaf** (backend calculated `isPointingUpwards`)
- `projectTreeRoots[].containedInternalDependencies` = **Aggregated edges** (default `isPointingUpwards`, NOT meaningful)

### What the Frontend Does Wrong

Looking at [`ProjectNodeConverter.ts:35-50`](visualization/src/app/adapter/analysis/ProjectNodeConverter.ts:35-50):

```typescript
function toShallowEdge(nodeId: string, rawDependencies: Record<string, EdgeMetaInformation>): ShallowEdge[] {
  const edges: ShallowEdge[] = []
  for (const targetNodeId in rawDependencies) {
    const edgeMetaInformation = rawDependencies[targetNodeId]
    const leafId = LeafIdCreator.createFrom(targetNodeId);
    edges.push(new ShallowEdge(
      nodeId, // ← Could be a PACKAGE ID!
      leafId, // target
      nodeId + "-" + leafId,
      edgeMetaInformation.weight,
      edgeMetaInformation.isCyclic,
      edgeMetaInformation.type
    ))
  }
  return edges
}
```

**Problem:** This function is called for BOTH:
1. Leaf nodes (from `leaves[].dependencies`) ✅ Correct - these are leaf-to-leaf
2. Package nodes (from `containedInternalDependencies`) ❌ Wrong - these are aggregated!

The `ShallowEdge` constructor doesn't distinguish between these two cases!

## Edge Modeling Issue

### Current State

**`ShallowEdge`** ([`Edge.ts:58-71`](visualization/src/app/model/Edge.ts:58-71)):
```typescript
export class ShallowEdge {
  constructor(
    readonly source: string,      // Could be leaf OR package!
    readonly target: string,      // Could be leaf OR package!
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string
  ) {}
}
```

**`Edge`** ([`Edge.ts:3-11`](visualization/src/app/model/Edge.ts:3-11)):
```typescript
export class Edge {
  constructor(
    readonly source: VisibleGraphNode,  // Visualization state
    readonly target: VisibleGraphNode,  // Visualization state
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string
  ) {}
}
```

### The Problem

1. **`ShallowEdge` is polluted** with aggregated edges from `containedInternalDependencies`
2. **No way to distinguish** leaf-to-leaf from aggregated edges
3. **`isPointingUpwards` is missing** from `ShallowEdge` (should come from backend)
4. **`Edge` has no provenance** - doesn't know which `ShallowEdge`(s) it represents

### What Should Happen

**`ShallowEdge` should ONLY represent leaf-to-leaf edges:**

```typescript
export class ShallowEdge {
  constructor(
    readonly source: string,      // ALWAYS a leaf ID
    readonly target: string,      // ALWAYS a leaf ID
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly isPointingUpwards: boolean  // NEW: From backend
  ) {}
}
```

**`Edge` should track its source `ShallowEdge`(s):**

```typescript
export class Edge {
  constructor(
    readonly source: VisibleGraphNode,
    readonly target: VisibleGraphNode,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly sourceShallowEdges: ShallowEdge[]  // NEW: Provenance
  ) {}
  
  isPointingUpwards(): boolean {
    // ANY source edge pointing upwards means this edge points upwards
    return this.sourceShallowEdges.some(e => e.isPointingUpwards)
  }
}
```

## Node Modeling Issue (Similar Pattern!)

### Current State

**`GraphNode`** ([`GraphNode.ts:4-11`](visualization/src/app/model/GraphNode.ts:4-11)):
```typescript
export interface GraphNode {
  id: string
  label: string
  level: number
  parent?: GraphNode
  children: GraphNode[],
  dependencies: ShallowEdge[]  // ← Contains BOTH leaf and aggregated edges!
}
```

**`VisibleGraphNode`** ([`GraphNode.ts:13-18`](visualization/src/app/model/GraphNode.ts:13-18)):
```typescript
export type VisibleGraphNode = GraphNode & {
  visibleChildren: VisibleGraphNode[]
  hiddenChildrenIds: string[]
  isExpanded: boolean
  isSelected: boolean
}
```

### The Problem

1. **`GraphNode.dependencies`** contains a mix of:
   - Leaf-to-leaf edges (when node is a leaf)
   - Aggregated edges (when node is a package)

2. **No distinction** between:
   - **Structural nodes** (from backend tree structure)
   - **Visualization nodes** (current UI state)

3. **`VisibleGraphNode`** is just `GraphNode` + UI state, but doesn't clarify the semantic difference

### The Pattern

Both edges and nodes suffer from the same issue:

| Concept | Backend Data | Frontend Model | Visualization State |
|---------|-------------|----------------|---------------------|
| **Edges** | Leaf-to-leaf only | `ShallowEdge` (polluted) | `Edge` (no provenance) |
| **Nodes** | Tree structure | `GraphNode` (mixed) | `VisibleGraphNode` (unclear) |

## Where the Pollution Happens

### For Edges

In [`ProjectNodeConverter.ts:23`](visualization/src/app/adapter/analysis/ProjectNodeConverter.ts:23):

```typescript
const graphNode: GraphNode = {
  id: id,
  children: [],
  label: node.name,
  level: node.level,
  dependencies: toShallowEdge(id, node.containedInternalDependencies)  // ← POLLUTION!
}
```

This calls `toShallowEdge()` with `containedInternalDependencies`, which are **aggregated package-level edges**, not leaf-to-leaf edges!

### For Nodes

The same function creates `GraphNode` from `ProjectNode`, but `ProjectNode` can be:
- A leaf (has `leafId`)
- A package (has `children` and `containedInternalDependencies`)

The `GraphNode` doesn't distinguish these cases clearly.

## The Solution: Separate Concerns

### Principle 1: Data Layer (from Backend)

**`ShallowEdge`** = ONLY leaf-to-leaf edges from `leaves[].dependencies`:
```typescript
export class ShallowEdge {
  constructor(
    readonly sourceLeafId: string,     // ALWAYS a leaf
    readonly targetLeafId: string,     // ALWAYS a leaf
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly isPointingUpwards: boolean  // From backend
  ) {}
}
```

**`GraphNode`** = Tree structure from backend:
```typescript
export interface GraphNode {
  id: string
  label: string
  level: number
  parent?: GraphNode
  children: GraphNode[]
  isLeaf: boolean  // NEW: Explicit distinction
  leafDependencies: ShallowEdge[]  // NEW: ONLY if isLeaf === true
}
```

### Principle 2: Visualization Layer

**`Edge`** = Visualization edge (may aggregate multiple `ShallowEdge`s):
```typescript
export class Edge {
  constructor(
    readonly source: VisibleGraphNode,
    readonly target: VisibleGraphNode,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly sourceShallowEdges: ShallowEdge[]  // Provenance chain
  ) {}
  
  isPointingUpwards(): boolean {
    return this.sourceShallowEdges.some(e => e.isPointingUpwards)
  }
}
```

**`VisibleGraphNode`** = UI state + reference to data:
```typescript
export type VisibleGraphNode = GraphNode & {
  visibleChildren: VisibleGraphNode[]
  hiddenChildrenIds: string[]
  isExpanded: boolean
  isSelected: boolean
}
```

## How Edges Should Be Created

### Current (Wrong) Flow

```
Backend JSON
  ↓
toShallowEdge() creates ShallowEdge for BOTH leaves AND packages
  ↓
GraphNode.dependencies contains mixed edges
  ↓
createEdgesForNode() creates Edge from ShallowEdge
  ↓
Edge has no idea if it came from leaf or package
```

### Correct Flow

```
Backend JSON
  ↓
ONLY leaves[].dependencies → ShallowEdge (leaf-to-leaf only)
  ↓
GraphNode.leafDependencies (only for leaf nodes)
  ↓
createEdgesForNode() finds relevant ShallowEdges
  ↓
When source/target are collapsed, aggregate multiple ShallowEdges
  ↓
Edge tracks which ShallowEdges it represents
```

## Implementation Strategy

### Phase 1: Fix Data Loading

1. **Stop creating `ShallowEdge` from `containedInternalDependencies`**
   - These are aggregated edges, not leaf-to-leaf
   - They should be ignored during loading

2. **Only create `ShallowEdge` from `leaves[].dependencies`**
   - These are the true leaf-to-leaf edges
   - Add `isPointingUpwards` field from backend

3. **Store all `ShallowEdge`s in a global registry**
   - Indexed by source and target leaf IDs
   - Available for lookup during visualization

### Phase 2: Fix Edge Creation

In [`GraphNode.ts:108-123`](visualization/src/app/model/GraphNode.ts:108-123), change `createEdgesForNode()`:

```typescript
static createEdgesForNode(
  node: VisibleGraphNode, 
  visibleNodes: VisibleGraphNode[], 
  hiddenNodeIds: string[],
  allShallowEdges: ShallowEdge[]  // NEW: Global registry
): Edge[] {
  // Find all leaf descendants of this node
  const sourceLeaves = node.isLeaf ? [node] : getAllLeafDescendants(node)
  
  return sourceLeaves.flatMap(sourceLeaf => {
    // Find all ShallowEdges from this leaf
    const relevantShallowEdges = allShallowEdges.filter(e => e.sourceLeafId === sourceLeaf.id)
    
    return relevantShallowEdges.flatMap(shallowEdge => {
      // Find the best visible target for this edge
      const bestTarget = findBestDependencyTarget(shallowEdge.targetLeafId, visibleNodes, hiddenNodeIds)
      
      if (bestTarget && !IdUtils.isIncludedIn(bestTarget.id, node.id)) {
        return new Edge(
          node,
          bestTarget,
          node.id + "-" + bestTarget.id,
          shallowEdge.weight,
          shallowEdge.isCyclic,
          shallowEdge.type,
          [shallowEdge]  // NEW: Track source
        )
      }
      return []
    })
  })
}
```

### Phase 3: Fix Edge Aggregation

In [`Edge.ts:30-55`](visualization/src/app/model/Edge.ts:30-55), update `aggregateEdges()`:

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
        sourceShallowEdges: [
          ...duplicateEdge.sourceShallowEdges,
          ...edge.sourceShallowEdges
        ]  // NEW: Merge provenance
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

1. **Correctness**: `ShallowEdge` always represents leaf-to-leaf edges with backend's `isPointingUpwards`
2. **Clarity**: Clear separation between data (ShallowEdge) and visualization (Edge)
3. **Traceability**: Every `Edge` knows which `ShallowEdge`(s) it represents
4. **Flexibility**: Can implement different aggregation strategies
5. **Performance**: No runtime graph traversal needed for `isPointingUpwards()`
6. **Debuggability**: Can trace any visualization edge back to original leaf edges

## Key Takeaways

1. **`ShallowEdge` should ONLY come from `leaves[].dependencies`** (leaf-to-leaf)
2. **Ignore `containedInternalDependencies`** during loading (they're aggregated, not useful)
3. **`Edge` should track its source `ShallowEdge`(s)** for provenance
4. **Similar pattern exists for nodes** (structural vs visualization)
5. **The backend already provides the right data** - we just need to use it correctly!

## Related Code Locations

- Edge creation: [`GraphNode.ts:108-123`](visualization/src/app/model/GraphNode.ts:108-123)
- Data loading: [`ProjectNodeConverter.ts:35-50`](visualization/src/app/adapter/analysis/ProjectNodeConverter.ts:35-50)
- Edge aggregation: [`Edge.ts:30-55`](visualization/src/app/model/Edge.ts:30-55)
- Cytoscape conversion: [`elementDefinitionConverter.ts:19-37`](visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.ts:19-37)
- State management: [`State.ts:185-193`](visualization/src/app/model/State.ts:185-193)

## Next Steps

1. Add `isPointingUpwards: boolean` to `ShallowEdge`
2. Add `sourceShallowEdges: ShallowEdge[]` to `Edge`
3. Modify `toShallowEdge()` to ONLY process leaf dependencies
4. Create global `ShallowEdge` registry during data loading
5. Update `createEdgesForNode()` to use the registry
6. Update `aggregateEdges()` to merge provenance
7. Update `isPointingUpwards()` to query source edges
8. Add tests for edge composition