# Design: Well-Modeled Graph Class for State

## Philosophy

Instead of refactoring the existing tangled code, we introduce a **new, clean `Graph` class** that lives inside `State` and properly separates:

1. **Raw data from backend** (immutable, leaf-to-leaf only)
2. **Visualization state** (derived, can include aggregated edges)

The existing code can gradually migrate to use this new model.

## Core Principles

### Principle 1: Leaf-to-Leaf is the Source of Truth

**Only leaf-to-leaf edges exist in the raw data.** Everything else is derived.

```typescript
// Raw edge from backend JSON (leaves[].dependencies)
class RawEdge {
  readonly sourceLeafId: string      // ALWAYS a leaf
  readonly targetLeafId: string      // ALWAYS a leaf
  readonly weight: number
  readonly isCyclic: boolean
  readonly type: string
  readonly isPointingUpwards: boolean  // From backend
}
```

### Principle 2: Nodes are Just Hierarchy

Nodes represent the package/namespace hierarchy. They don't "own" edges - edges reference nodes.

```typescript
// Node in the tree (can be leaf or package)
class Node {
  readonly id: string
  readonly label: string
  readonly level: number
  readonly parentId: string | null
  readonly childIds: string[]
  readonly isLeaf: boolean
}
```

### Principle 3: Visualization Edges are Derived

When visualizing, we derive edges based on what's visible. An edge might represent:
- One raw edge (leaf-to-leaf, both visible)
- Multiple raw edges (aggregated when packages are collapsed)

```typescript
// Derived edge for visualization
class DrawnEdge {
  readonly sourceNodeId: string      // Could be leaf OR package
  readonly targetNodeId: string      // Could be leaf OR package
  readonly rawEdges: RawEdge[]       // Which raw edges does this represent?
  readonly weight: number            // Sum of raw edge weights
  readonly isCyclic: boolean         // Any raw edge cyclic?
  readonly type: string              // Dominant type
  
  isPointingUpwards(): boolean {
    // ANY raw edge pointing upwards means this drawn edge points upwards
    return this.rawEdges.some(e => e.isPointingUpwards)
  }
  
  isMixed(): boolean {
    // Contains both upward and downward raw edges
    const hasUpward = this.rawEdges.some(e => e.isPointingUpwards)
    const hasDownward = this.rawEdges.some(e => !e.isPointingUpwards)
    return hasUpward && hasDownward
  }
}
```

## The Graph Class

```typescript
/**
 * Immutable graph model that separates raw data from visualization state.
 * 
 * Raw data (from backend):
 * - nodes: The package/namespace hierarchy
 * - rawEdges: ONLY leaf-to-leaf edges from backend
 * 
 * Derived data (for visualization):
 * - getDrawnEdges(): Computes edges based on visible nodes
 */
class Graph {
  private readonly nodes: Map<string, Node>
  private readonly rawEdges: RawEdge[]
  private readonly rawEdgesBySourceLeaf: Map<string, RawEdge[]>
  
  constructor(nodes: Node[], rawEdges: RawEdge[]) {
    this.nodes = new Map(nodes.map(n => [n.id, n]))
    this.rawEdges = rawEdges
    
    // Index raw edges by source leaf for fast lookup
    this.rawEdgesBySourceLeaf = new Map()
    for (const edge of rawEdges) {
      const existing = this.rawEdgesBySourceLeaf.get(edge.sourceLeafId) || []
      this.rawEdgesBySourceLeaf.set(edge.sourceLeafId, [...existing, edge])
    }
  }
  
  // ========== Node Queries ==========
  
  getNode(nodeId: string): Node | undefined {
    return this.nodes.get(nodeId)
  }
  
  getAllNodes(): Node[] {
    return Array.from(this.nodes.values())
  }
  
  getLeafNodes(): Node[] {
    return this.getAllNodes().filter(n => n.isLeaf)
  }
  
  getChildren(nodeId: string): Node[] {
    const node = this.getNode(nodeId)
    if (!node) return []
    return node.childIds.map(id => this.getNode(id)).filter(n => n !== undefined)
  }
  
  getLeafDescendants(nodeId: string): Node[] {
    const node = this.getNode(nodeId)
    if (!node) return []
    if (node.isLeaf) return [node]
    
    const descendants: Node[] = []
    const queue = [nodeId]
    
    while (queue.length > 0) {
      const currentId = queue.shift()!
      const current = this.getNode(currentId)
      if (!current) continue
      
      if (current.isLeaf) {
        descendants.push(current)
      } else {
        queue.push(...current.childIds)
      }
    }
    
    return descendants
  }
  
  // ========== Raw Edge Queries ==========
  
  getRawEdges(): RawEdge[] {
    return this.rawEdges
  }
  
  getRawEdgesFromLeaf(leafId: string): RawEdge[] {
    return this.rawEdgesBySourceLeaf.get(leafId) || []
  }
  
  getRawEdgesBetweenLeaves(sourceLeafId: string, targetLeafId: string): RawEdge[] {
    return this.getRawEdgesFromLeaf(sourceLeafId)
      .filter(e => e.targetLeafId === targetLeafId)
  }
  
  // ========== Drawn Edge Computation ==========
  
  /**
   * Compute drawn edges based on visible nodes.
   * 
   * For each visible node:
   * 1. Find all leaf descendants
   * 2. Find all raw edges from those leaves
   * 3. For each raw edge, find the best visible target
   * 4. Aggregate raw edges that map to the same (source, target) pair
   * 
   * @param visibleNodeIds - IDs of nodes currently visible in the UI
   * @param hiddenNodeIds - IDs of nodes that are hidden
   */
  getDrawnEdges(visibleNodeIds: string[], hiddenNodeIds: string[]): DrawnEdge[] {
    const visibleNodes = visibleNodeIds
      .map(id => this.getNode(id))
      .filter(n => n !== undefined)
    
    // Map from "sourceId-targetId" to list of raw edges
    const edgeGroups = new Map<string, RawEdge[]>()
    
    for (const sourceNode of visibleNodes) {
      // Only create edges from leaf nodes (unexpanded packages don't show edges)
      if (!sourceNode.isLeaf && sourceNode.childIds.length > 0) {
        continue
      }
      
      // Find all leaf descendants of this source node
      const sourceLeaves = this.getLeafDescendants(sourceNode.id)
      
      for (const sourceLeaf of sourceLeaves) {
        const rawEdges = this.getRawEdgesFromLeaf(sourceLeaf.id)
        
        for (const rawEdge of rawEdges) {
          // Find the best visible target for this raw edge
          const targetNode = this.findBestVisibleTarget(
            rawEdge.targetLeafId,
            visibleNodeIds,
            hiddenNodeIds
          )
          
          if (!targetNode) continue
          
          // Don't create self-edges or edges to ancestors
          if (this.isAncestorOf(targetNode.id, sourceNode.id)) {
            continue
          }
          
          // Group raw edges by (source, target) pair
          const key = `${sourceNode.id}-${targetNode.id}`
          const existing = edgeGroups.get(key) || []
          edgeGroups.set(key, [...existing, rawEdge])
        }
      }
    }
    
    // Convert groups to DrawnEdges
    return Array.from(edgeGroups.entries()).map(([key, rawEdges]) => {
      const [sourceId, targetId] = key.split('-')
      return new DrawnEdge(
        sourceId,
        targetId,
        rawEdges,
        rawEdges.reduce((sum, e) => sum + e.weight, 0),
        rawEdges.some(e => e.isCyclic),
        this.getDominantType(rawEdges)
      )
    })
  }
  
  /**
   * Find the best visible target for a leaf node.
   * 
   * Strategy:
   * 1. If the leaf itself is visible, return it
   * 2. Otherwise, walk up the ancestor chain until we find a visible node
   * 3. If no visible ancestor, return null
   */
  private findBestVisibleTarget(
    leafId: string,
    visibleNodeIds: string[],
    hiddenNodeIds: string[]
  ): Node | undefined {
    if (hiddenNodeIds.includes(leafId)) {
      return undefined
    }
    
    let currentId: string | null = leafId
    
    while (currentId !== null) {
      if (visibleNodeIds.includes(currentId)) {
        return this.getNode(currentId)
      }
      
      const current = this.getNode(currentId)
      if (!current) break
      currentId = current.parentId
    }
    
    return undefined
  }
  
  private isAncestorOf(ancestorId: string, descendantId: string): boolean {
    let currentId: string | null = descendantId
    
    while (currentId !== null) {
      if (currentId === ancestorId) {
        return true
      }
      
      const current = this.getNode(currentId)
      if (!current) break
      currentId = current.parentId
    }
    
    return false
  }
  
  private getDominantType(rawEdges: RawEdge[]): string {
    // Simple strategy: return the most common type
    const typeCounts = new Map<string, number>()
    for (const edge of rawEdges) {
      typeCounts.set(edge.type, (typeCounts.get(edge.type) || 0) + 1)
    }
    
    let maxCount = 0
    let dominantType = 'usage'
    for (const [type, count] of typeCounts.entries()) {
      if (count > maxCount) {
        maxCount = count
        dominantType = type
      }
    }
    
    return dominantType
  }
}
```

## Loading from Backend JSON

```typescript
/**
 * Load Graph from backend JSON.
 * 
 * Key insight: ONLY load leaf-to-leaf edges from leaves[].dependencies.
 * IGNORE containedInternalDependencies (they're aggregated, not useful).
 */
function loadGraphFromJson(json: any): Graph {
  const nodes: Node[] = []
  const rawEdges: RawEdge[] = []
  
  // 1. Build node hierarchy from projectTreeRoots
  function traverseProjectNode(
    projectNode: any,
    parentId: string | null,
    parentPath: string
  ): void {
    const nodeId = projectNode.leafId
      ? LeafIdCreator.createFrom(projectNode.leafId)
      : parentPath ? `${parentPath}.${projectNode.name}` : projectNode.name
    
    const node: Node = {
      id: nodeId,
      label: projectNode.name,
      level: projectNode.level,
      parentId: parentId,
      childIds: [],
      isLeaf: projectNode.leafId !== undefined
    }
    
    nodes.push(node)
    
    // Update parent's childIds
    if (parentId) {
      const parent = nodes.find(n => n.id === parentId)
      if (parent) {
        parent.childIds.push(nodeId)
      }
    }
    
    // Recursively process children
    if (projectNode.children) {
      for (const child of projectNode.children) {
        traverseProjectNode(child, nodeId, nodeId)
      }
    }
  }
  
  for (const root of json.projectTreeRoots || []) {
    traverseProjectNode(root, null, '')
  }
  
  // 2. Load ONLY leaf-to-leaf edges from leaves[].dependencies
  for (const [leafId, leafInfo] of Object.entries(json.leaves || {})) {
    const sourceLeafId = LeafIdCreator.createFrom(leafId)
    
    for (const [targetId, edgeInfo] of Object.entries(leafInfo.dependencies || {})) {
      const targetLeafId = LeafIdCreator.createFrom(targetId)
      
      rawEdges.push({
        sourceLeafId: sourceLeafId,
        targetLeafId: targetLeafId,
        weight: edgeInfo.weight,
        isCyclic: edgeInfo.isCyclic,
        type: edgeInfo.type,
        isPointingUpwards: edgeInfo.isPointingUpwards ?? false
      })
    }
  }
  
  return new Graph(nodes, rawEdges)
}
```

## Integration with State

```typescript
class State extends DataClass<State> {
  // ... existing fields ...
  declare readonly graph: Graph  // NEW: The well-modeled graph
  
  static fromRootNodes(rootNodes: GraphNode[]): State {
    // For backward compatibility, convert old GraphNode[] to new Graph
    // Eventually, we load Graph directly from JSON
    const graph = convertGraphNodesToGraph(rootNodes)
    
    return State.build({
      graph: graph,
      allNodes: rootNodes.flatMap(expand)  // Keep for now, migrate later
    })
  }
  
  static fromJson(json: any): State {
    // NEW: Load directly from JSON
    const graph = loadGraphFromJson(json)
    
    return State.build({
      graph: graph,
      allNodes: []  // Can be empty, we use graph instead
    })
  }
  
  getVisibleNodes(): VisibleGraphNode[] {
    // TODO: Migrate to use graph.getAllNodes() and filter based on state
    // For now, keep existing implementation
    return this.allNodes
      .filter(node => !node.parent || this.expandedNodeIds.includes(node.parent.id))
      .filter(node => !GraphNodeUtils.isNodeOrAncestorHidden(this.hiddenNodeIds, node))
      .map(node => this.toVisibleGraphNode(node))
  }
  
  createEdges(nodes: VisibleGraphNode[]): Edge[] {
    // NEW: Use graph to compute drawn edges
    const visibleNodeIds = this.getVisibleNodes().map(n => n.id)
    const drawnEdges = this.graph.getDrawnEdges(visibleNodeIds, this.hiddenNodeIds)
    
    // Convert DrawnEdge to Edge (for backward compatibility)
    return drawnEdges.map(drawnEdge => {
      const source = this.findGraphNode(drawnEdge.sourceNodeId)
      const target = this.findGraphNode(drawnEdge.targetNodeId)
      
      return new Edge(
        source,
        target,
        `${drawnEdge.sourceNodeId}-${drawnEdge.targetNodeId}`,
        drawnEdge.weight,
        drawnEdge.isCyclic,
        drawnEdge.type,
        drawnEdge.rawEdges  // NEW: Track provenance
      )
    })
  }
}
```

## Benefits of This Design

### 1. Clear Separation of Concerns

```
Raw Data (Immutable)          Visualization (Derived)
├─ Node (hierarchy)           ├─ VisibleGraphNode (UI state)
├─ RawEdge (leaf-to-leaf)     └─ DrawnEdge (aggregated)
└─ Graph (queries)
```

### 2. Single Source of Truth

- **RawEdge** is the only place where `isPointingUpwards` is stored
- **DrawnEdge** queries its raw edges, never recalculates
- No confusion about which edges are "real" vs "aggregated"

### 3. Testability

```typescript
// Easy to test: just create a Graph and query it
const graph = new Graph(
  [
    { id: 'pkg', label: 'pkg', level: 0, parentId: null, childIds: ['pkg.A', 'pkg.B'], isLeaf: false },
    { id: 'pkg.A', label: 'A', level: 0, parentId: 'pkg', childIds: [], isLeaf: true },
    { id: 'pkg.B', label: 'B', level: 1, parentId: 'pkg', childIds: [], isLeaf: true }
  ],
  [
    { sourceLeafId: 'pkg.A', targetLeafId: 'pkg.B', weight: 1, isCyclic: false, type: 'usage', isPointingUpwards: true }
  ]
)

// When pkg is collapsed, we get one drawn edge
const drawnEdges = graph.getDrawnEdges(['pkg'], [])
expect(drawnEdges).toHaveLength(1)
expect(drawnEdges[0].isPointingUpwards()).toBe(true)

// When pkg is expanded, we get one drawn edge (leaf-to-leaf)
const drawnEdges2 = graph.getDrawnEdges(['pkg.A', 'pkg.B'], [])
expect(drawnEdges2).toHaveLength(1)
expect(drawnEdges2[0].isPointingUpwards()).toBe(true)
```

### 4. Performance

- **Indexed lookups**: `rawEdgesBySourceLeaf` for O(1) edge queries
- **No tree traversal**: `isPointingUpwards()` just checks raw edges
- **Lazy computation**: Drawn edges only computed when needed

### 5. Extensibility

Easy to add new features:

```typescript
class DrawnEdge {
  // ... existing methods ...
  
  getUpwardRawEdges(): RawEdge[] {
    return this.rawEdges.filter(e => e.isPointingUpwards)
  }
  
  getDownwardRawEdges(): RawEdge[] {
    return this.rawEdges.filter(e => !e.isPointingUpwards)
  }
  
  getEdgeType(): EdgeType {
    if (this.isCyclic && this.isPointingUpwards()) return EdgeType.FEEDBACK
    if (this.isCyclic) return EdgeType.CYCLIC
    if (this.isPointingUpwards()) return EdgeType.TWISTED
    return EdgeType.REGULAR
  }
}
```

## Migration Strategy

### Phase 1: Introduce Graph (Non-Breaking)

1. Add `Graph`, `Node`, `RawEdge`, `DrawnEdge` classes
2. Add `graph: Graph` field to `State`
3. Keep existing `allNodes: GraphNode[]` for backward compatibility
4. Add `loadGraphFromJson()` function
5. Add tests for `Graph` class

### Phase 2: Use Graph for Edge Creation

1. Update `State.createEdges()` to use `graph.getDrawnEdges()`
2. Update `Edge` to accept `rawEdges: RawEdge[]` parameter
3. Update `Edge.isPointingUpwards()` to query raw edges
4. Keep existing code paths as fallback

### Phase 3: Use Graph for Node Queries

1. Update `State.getVisibleNodes()` to use `graph.getAllNodes()`
2. Update `State.findGraphNode()` to use `graph.getNode()`
3. Gradually remove dependency on `allNodes: GraphNode[]`

### Phase 4: Remove Old Code

1. Remove `allNodes: GraphNode[]` from `State`
2. Remove `ShallowEdge` class (replaced by `RawEdge`)
3. Remove old `GraphNode` interface (replaced by `Node`)
4. Remove `findSiblingsUnderLowestCommonAncestor()` (no longer needed)

## Key Takeaways

1. **`Graph` is the single source of truth** for both nodes and edges
2. **`RawEdge` is ALWAYS leaf-to-leaf** (from backend JSON)
3. **`DrawnEdge` is derived** based on visible nodes
4. **No more confusion** between raw data and visualization state
5. **Easy to test, extend, and reason about**
6. **Backward compatible** - can be introduced gradually

## Next Steps

1. Implement `Graph`, `Node`, `RawEdge`, `DrawnEdge` classes
2. Add comprehensive tests
3. Add `loadGraphFromJson()` function
4. Integrate with `State` (non-breaking)
5. Gradually migrate existing code to use `Graph`