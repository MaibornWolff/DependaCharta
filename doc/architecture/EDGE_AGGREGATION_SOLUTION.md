# Edge Aggregation Solution: Never Recalculate isPointingUpwards

## Problem Statement

The current implementation recalculates `isPointingUpwards` for aggregated edges when namespaces collapse. This goes against the principle of using backend data as the single source of truth.

## Desired Approach

**Never recalculate `isPointingUpwards`** - always derive it from backend data through aggregation using logical OR.

## Key Insight

When multiple leaf edges are aggregated into a namespace-level edge, the aggregated edge should be considered "pointing upward" if **ANY** of its constituent leaf edges point upward.

### Semantic Justification

If even one dependency within a collapsed namespace violates the architectural flow (points upward), then the entire namespace-to-namespace relationship represents an architectural violation.

```
Example:
  domain.model.ClassA → application.ServiceX (isPointingUpwards: false)
  domain.model.ClassB → application.ServiceY (isPointingUpwards: true)  ← violation!
  
When aggregated:
  domain.model → application (isPointingUpwards: true)  ← ANY was true
```

This is semantically correct: the presence of any upward-pointing dependency means the aggregate relationship violates architecture.

## Solution: Aggregate Using Logical OR

### Aggregation Rule

```typescript
aggregated.isPointingUpwards = constituentEdges.some(e => e.isPointingUpwards)
```

This is consistent with how `isCyclic` is already aggregated:
```typescript
aggregated.isCyclic = constituentEdges.some(e => e.isCyclic)
```

### Implementation

```typescript
class Edge {
  readonly isPointingUpwards: boolean  // Property, not method
  readonly isCyclic: boolean
  readonly weight: number
  
  constructor(
    source: VisibleGraphNode,
    target: VisibleGraphNode,
    isPointingUpwards: boolean,  // Always provided (from backend or aggregation)
    isCyclic: boolean,
    weight: number,
    // ... other params
  ) {
    this.isPointingUpwards = isPointingUpwards
    this.isCyclic = isCyclic
    this.weight = weight
  }
}

// In Edge.aggregateEdges():
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
        isCyclic: duplicateEdge.isCyclic || edge.isCyclic,  // Logical OR (existing)
        isPointingUpwards: duplicateEdge.isPointingUpwards || edge.isPointingUpwards,  // Logical OR (new)
      })
    } else {
      aggregatedEdge = edge.copy({id: key})
    }

    aggregatedEdges.set(key, aggregatedEdge)
  })

  return [...aggregatedEdges.values()]
}
```

## Benefits

- ✅ **Never recalculates** - respects backend as single source of truth
- ✅ **Simple rule** - logical OR, same as `isCyclic`
- ✅ **No conditional logic** - Edge constructor is simple
- ✅ **No extra fields** - no originalSource/originalTarget needed
- ✅ **Semantically correct** - any violation means aggregate violates
- ✅ **Consistent** - matches existing `isCyclic` aggregation pattern

## Implementation Steps

### Step 1: Add isPointingUpwards to ShallowEdge

```typescript
// In ProjectNodeConverter.ts
function toShallowEdge(nodeId: string, rawDependencies: Record<string, EdgeMetaInformation>): ShallowEdge[] {
  const edges: ShallowEdge[] = []
  for (const targetNodeId in rawDependencies) {
    const edgeMetaInformation = rawDependencies[targetNodeId]
    const leafId = LeafIdCreator.createFrom(targetNodeId)
    edges.push(new ShallowEdge(
      nodeId,
      leafId,
      nodeId + "-" + leafId,
      edgeMetaInformation.weight,
      edgeMetaInformation.isCyclic,
      edgeMetaInformation.type,
      edgeMetaInformation.isPointingUpwards  // ← Add this
    ))
  }
  return edges
}

// Update ShallowEdge class
export class ShallowEdge {
  constructor(
    readonly source: string,
    readonly target: string,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly isPointingUpwards: boolean  // ← Add this
  ) {}
}
```

### Step 2: Pass isPointingUpwards to Edge constructor

```typescript
// In VisibleGraphNodeUtils.createEdgesForNode()
static createEdgesForNode(node: VisibleGraphNode, visibleNodes: VisibleGraphNode[], hiddenNodeIds: string[]): Edge[] {
  return node.dependencies.flatMap(dependency => {
    const bestTarget = VisibleGraphNodeUtils.findBestDependencyTarget(dependency.target, visibleNodes, hiddenNodeIds)
    if (bestTarget && !IdUtils.isIncludedIn(bestTarget.id, node.id)) {
      return new Edge(
        node,
        bestTarget,
        node.id + "-" + bestTarget.id,
        dependency.weight,
        dependency.isCyclic,
        dependency.type,
        dependency.isPointingUpwards  // ← Add this
      )
    }
    return []
  })
}

// Update Edge constructor
export class Edge {
  constructor(
    readonly source: VisibleGraphNode,
    readonly target: VisibleGraphNode,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly isPointingUpwards: boolean  // ← Add this (property, not method)
  ) {}
}
```

### Step 3: Update Edge.aggregateEdges()

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
        isCyclic: duplicateEdge.isCyclic || edge.isCyclic,
        isPointingUpwards: duplicateEdge.isPointingUpwards || edge.isPointingUpwards,  // ← Add this
      })
    } else {
      aggregatedEdge = edge.copy({id: key})
    }

    aggregatedEdges.set(key, aggregatedEdge)
  })

  return [...aggregatedEdges.values()]
}
```

### Step 4: Remove isPointingUpwards() method

Delete the `isPointingUpwards()` method and `findSiblingsUnderLowestCommonAncestor()` function from Edge.ts.

### Step 5: Add EdgeType enum

```typescript
export enum EdgeType {
  REGULAR = 'REGULAR',    // !isCyclic && !isPointingUpwards
  CYCLIC = 'CYCLIC',      // isCyclic && !isPointingUpwards
  TWISTED = 'TWISTED',    // !isCyclic && isPointingUpwards
  FEEDBACK = 'FEEDBACK'   // isCyclic && isPointingUpwards
}

export class Edge {
  // ... existing properties
  
  getEdgeType(): EdgeType {
    if (this.isCyclic && this.isPointingUpwards) return EdgeType.FEEDBACK
    if (this.isCyclic) return EdgeType.CYCLIC
    if (this.isPointingUpwards) return EdgeType.TWISTED
    return EdgeType.REGULAR
  }
}
```

### Step 6: Update EdgeFilter

Simplify EdgeFilter to use EdgeType instead of predicates.

### Step 7: Add tests

Test that aggregation correctly uses logical OR for `isPointingUpwards`.

## Expansion Scenario

When you **expand** a collapsed namespace, edges are recreated based on the new visible nodes:

```
Initial (both collapsed):
  domain → application
    (aggregates ALL leaf edges, isPointingUpwards: true if ANY leaf edge is true)

After expanding "domain" (application still collapsed):
  domain.model → application
    (aggregates leaf edges from domain.model.* to application.*, isPointingUpwards: true if ANY is true)
  domain.service → application
    (aggregates leaf edges from domain.service.* to application.*, isPointingUpwards: true if ANY is true)

After expanding both:
  domain.model.ClassA → application.ServiceX (leaf edge, uses backend value)
  domain.model.ClassB → application.ServiceY (leaf edge, uses backend value)
  domain.service.CreatureService → application.CreatureFacade (leaf edge, uses backend value)
```

### Key Points

1. **Edges are always recreated** when visibility changes (expand/collapse)
2. **Aggregation happens at the current visibility level** - we aggregate the edges that would be visible at the next level down
3. **The aggregation rule stays the same** - logical OR of constituent edges
4. **No state is "lost"** - we always go back to the backend data (leaf edges) and aggregate up to the current visibility level

### How It Works

The edge creation process in `State.createEdges()`:
```typescript
createEdges(nodes: VisibleGraphNode[]): Edge[] {
  const visibleNodes = this.getVisibleNodes()
  const edges: Edge[] = nodes
    .filter(node => node.visibleChildren.length === 0)  // Only unexpanded nodes
    .flatMap(node => {
      return VisibleGraphNodeUtils.createEdgesForNode(node, visibleNodes, this.hiddenNodeIds)
    })
  return Edge.aggregateEdges(edges, EdgeFilter.isFilterForcesEdgesAggregation(this.selectedFilter))
}
```

This means:
- When `domain` is collapsed: creates edges from `domain` (unexpanded)
- When `domain` is expanded: creates edges from `domain.model` and `domain.service` (unexpanded)
- When both are expanded: creates edges from leaf nodes

The aggregation happens in `VisibleGraphNodeUtils.createEdgesForNode()` which:
1. Looks at the node's dependencies (leaf-to-leaf from backend)
2. Finds the best visible target (might be collapsed namespace)
3. Creates edges with backend's `isPointingUpwards` value
4. Then `Edge.aggregateEdges()` combines edges with same source/target using OR

## Comparison with Current Approach

| Aspect | Current (Recalculate) | Proposed (Aggregate) |
|--------|----------------------|---------------------|
| Leaf edges | Recalculates | Uses backend value |
| Aggregated edges | Recalculates | Aggregates with OR |
| Expansion/Collapse | Recalculates each time | Aggregates from backend each time |
| Consistency | May differ from backend | Always matches backend |
| Complexity | High (findSiblings logic) | Low (simple OR) |
| Performance | Slower (recalc every time) | Faster (no recalc) |
| Taste | ❌ Recalculates | ✅ Never recalculates |