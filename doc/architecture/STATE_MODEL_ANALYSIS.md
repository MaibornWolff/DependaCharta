# Current State Model Analysis

## Overview

This document analyzes the current frontend state model implementation and identifies areas for improvement based on the domain model described in DOMAIN.md.

## Current Architecture

### State Class (`State.ts`)

The current `State` class is a monolithic structure that mixes multiple concerns:

```typescript
class State {
  allNodes: GraphNode[]                                    // Tier 1: Project structure
  hiddenNodeIds: string[]                                  // Tier 5: Hidden nodes
  hiddenChildrenIdsByParentId: Map<string, string[]>      // Tier 5: Hidden nodes tracking
  expandedNodeIds: string[]                                // Tier 3: Focus state
  hoveredNodeId: string                                    // UI state (not domain)
  selectedNodeIds: string[]                                // UI state (not domain)
  pinnedNodeIds: string[]                                  // Tier 3: Focus state
  selectedPinnedNodeIds: string[]                          // Tier 3: Focus state
  showLabels: boolean                                      // UI preference
  selectedFilter: EdgeFilterType                           // UI preference
  isInteractive: boolean                                   // UI mode
  isUsageShown: boolean                                    // UI mode
  multiselectMode: boolean                                 // UI mode
}
```

### GraphNode Structure

```typescript
interface GraphNode {
  id: string
  label: string
  level: number
  parent?: GraphNode                    // Bidirectional reference (mutable)
  children: GraphNode[]                 // Mutable array
  dependencies: ShallowEdge[]           // Tier 2: Dependencies mixed with structure
}
```

### Edge Model

```typescript
class Edge {
  source: VisibleGraphNode
  target: VisibleGraphNode
  id: string
  weight: number
  isCyclic: boolean
  type: string                          // String instead of enum
  
  isPointingUpwards(): boolean          // Computed on-demand (no caching)
}
```

## Issues Identified

### 1. **Mixed Concerns**
- Domain model (structure, dependencies) mixed with UI state (hover, selection)
- Presentation logic (visible nodes) mixed with domain logic
- No clear separation between immutable data and derived state

### 2. **Mutable Structure**
- `GraphNode` has bidirectional parent/child references
- Children arrays are mutable
- Makes it hard to reason about state changes
- Difficult to implement undo/redo or time-travel debugging

### 3. **Performance Issues**
- `isPointingUpwards()` computed on every call
- Edge aggregation recalculated on every render
- No caching of derived state
- `getVisibleNodes()` filters entire tree on every call

### 4. **Terminology Mismatch**
- Uses "GraphNode" for both namespaces and leaves
- No explicit "Leaf" or "Namespace" types
- Doesn't match DOMAIN.md terminology

### 5. **Edge Type Confusion**
- `type` is a string, not an enum
- `isPointingUpwards()` is computed, not stored
- Edge color logic scattered across multiple files
- No single source of truth for edge classification

### 6. **Tight Coupling**
- `VisibleGraphNode` couples structure with visibility
- Edge creation tightly coupled to node visibility
- Hard to test individual concerns

### 7. **Missing Abstractions**
- No explicit "ProjectStructure" concept
- No "DependencyGraph" abstraction
- No "FocusState" abstraction
- Aggregated edges not explicitly modeled

## Proposed Multi-Tier Architecture

Based on your vision, here's the proposed structure:

### Tier 1: ProjectStructure (Immutable)
```typescript
class ProjectStructure {
  readonly rootNamespaces: Namespace[]
  readonly allLeaves: Map<LeafId, Leaf>
  readonly allNamespaces: Map<NamespaceId, Namespace>
}

class Namespace {
  readonly id: NamespaceId
  readonly name: string
  readonly level: number
  readonly parentId?: NamespaceId
  readonly childNamespaceIds: NamespaceId[]
  readonly leafIds: LeafId[]
}

class Leaf {
  readonly id: LeafId
  readonly name: string
  readonly level: number
  readonly namespaceId: NamespaceId
  readonly physicalPath: string
  readonly nodeType: NodeType
  readonly language: Language
}
```

### Tier 2: DependencyGraph (Immutable)
```typescript
class DependencyGraph {
  readonly dependencies: Map<LeafId, Dependency[]>
}

class Dependency {
  readonly sourceLeafId: LeafId
  readonly targetLeafId: LeafId
  readonly weight: number
  readonly isCyclic: boolean
  readonly isPointingUpwards: boolean  // Stored, not computed
  readonly edgeType: EdgeType          // Enum, not string
}

enum EdgeType {
  REGULAR,   // !cyclic && !upward -> Grey
  CYCLIC,    // cyclic && !upward -> Blue
  FEEDBACK,  // cyclic && upward -> Red
  TWISTED    // !cyclic && upward -> Red
}
```

### Tier 3: FocusState (Mutable)
```typescript
class FocusState {
  readonly expandedNamespaceIds: Set<NamespaceId>
  readonly pinnedNodeIds: Set<NodeId>
  
  isExpanded(namespaceId: NamespaceId): boolean
  isPinned(nodeId: NodeId): boolean
  
  // Returns which nodes are currently "in focus" (visible)
  getVisibleNodes(structure: ProjectStructure): VisibleNode[]
}
```

### Tier 4: AggregatedEdgeCache (Computed)
```typescript
class AggregatedEdgeCache {
  private cache: Map<CacheKey, AggregatedEdge[]>
  
  // Recompute when structure or focus changes
  getAggregatedEdges(
    structure: ProjectStructure,
    dependencies: DependencyGraph,
    focus: FocusState
  ): AggregatedEdge[]
  
  invalidate(): void
}

class AggregatedEdge {
  readonly sourceNodeId: NodeId  // Could be Namespace or Leaf
  readonly targetNodeId: NodeId
  readonly weight: number
  readonly edgeType: EdgeType
  readonly contributingDependencies: Dependency[]
}
```

### Tier 5: HiddenNodesState (Mutable)
```typescript
class HiddenNodesState {
  readonly hiddenNodeIds: Set<NodeId>
  readonly hiddenChildrenByParent: Map<NamespaceId, Set<NodeId>>
  
  isHidden(nodeId: NodeId): boolean
  hide(nodeId: NodeId, structure: ProjectStructure): void
  restore(nodeId: NodeId): void
  restoreAll(): void
}
```

### Composed State
```typescript
class StateV2 {
  // Immutable tiers (loaded once)
  readonly structure: ProjectStructure
  readonly dependencies: DependencyGraph
  
  // Mutable tiers (user interactions)
  readonly focus: FocusState
  readonly hidden: HiddenNodesState
  
  // Computed tier (cached)
  private edgeCache: AggregatedEdgeCache
  
  // UI state (separate from domain)
  readonly ui: UIState
  
  reduce(action: Action): StateV2
}

class UIState {
  readonly hoveredNodeId: string
  readonly selectedNodeIds: string[]
  readonly showLabels: boolean
  readonly selectedFilter: EdgeFilterType
  readonly isInteractive: boolean
  readonly multiselectMode: boolean
}
```

## Benefits of New Architecture

### 1. **Clear Separation of Concerns**
- Each tier has a single responsibility
- Domain model separated from UI state
- Immutable data separated from mutable state

### 2. **Better Performance**
- Cached aggregated edges
- Incremental updates possible
- Only recompute what changed

### 3. **Easier Testing**
- Each tier can be tested independently
- Pure functions for most logic
- Predictable state transitions

### 4. **Better Terminology**
- Matches DOMAIN.md exactly
- Clear distinction between Namespace and Leaf
- Explicit EdgeType enum

### 5. **Extensibility**
- Easy to add hypothetical nodes (future feature)
- Easy to add new tiers (e.g., annotations, metrics)
- Easy to implement undo/redo

### 6. **Type Safety**
- Strong typing throughout
- No string-based types
- Compile-time guarantees

## Migration Strategy

### Phase 1: Parallel Implementation
1. Create new model classes in `visualization/src/app/model/v2/`
2. Keep existing model working
3. Add feature flag to switch between models

### Phase 2: Validation
1. Create comparison tests
2. Validate both models produce same results
3. Performance benchmarks

### Phase 3: Integration
1. Update components to use new model
2. Gradual migration of features
3. Keep old model as fallback

### Phase 4: Cleanup
1. Remove old model once validated
2. Update documentation
3. Remove feature flag

## Next Steps

1. ✅ Document current architecture (this document)
2. ⏳ Design detailed class structure for each tier
3. ⏳ Implement Tier 1: ProjectStructure
4. ⏳ Implement Tier 2: DependencyGraph
5. ⏳ Implement Tier 3: FocusState
6. ⏳ Implement Tier 4: AggregatedEdgeCache
7. ⏳ Implement Tier 5: HiddenNodesState
8. ⏳ Compose StateV2
9. ⏳ Add feature flag
10. ⏳ Create comparison tests
11. ⏳ Update DOMAIN.md
12. ⏳ Document migration path

## Open Questions

1. Should `isPointingUpwards` be computed during analysis (backend) or frontend?
   - **Recommendation**: Backend computes it, frontend stores it
   
2. How to handle the transition period with two models?
   - **Recommendation**: Feature flag + adapter pattern
   
3. Should we use classes or interfaces for immutable data?
   - **Recommendation**: Classes with readonly properties for better type safety
   
4. How to handle the `parent` reference in immutable structure?
   - **Recommendation**: Store only IDs, use maps for lookups

5. Should edge aggregation happen on-demand or be pre-computed?
   - **Recommendation**: Pre-compute and cache, invalidate on state change