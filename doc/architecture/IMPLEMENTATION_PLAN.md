# Frontend State Model Re-implementation Plan

## Executive Summary

This document outlines the step-by-step plan to re-implement the frontend state model according to the multi-tier architecture described in [`MULTI_TIER_MODEL_DESIGN.md`](MULTI_TIER_MODEL_DESIGN.md).

**Approach**: Parallel implementation with feature flag to allow comparison and validation before switching over.

## Goals

1. **Separation of Concerns**: Clear boundaries between structure, dependencies, focus, aggregation, and UI state
2. **Immutability**: Core data structures are immutable for predictability
3. **Performance**: Cache computed state, only recalculate when necessary
4. **Type Safety**: Strong typing with opaque ID types
5. **Testability**: Each tier independently testable
6. **Extensibility**: Easy to add future features (e.g., hypothetical nodes for refactoring preview)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         StateV2                              │
├─────────────────────────────────────────────────────────────┤
│  Tier 1: ProjectStructure (immutable)                       │
│    - Namespace hierarchy                                     │
│    - Leaf nodes (files/modules)                             │
├─────────────────────────────────────────────────────────────┤
│  Tier 2: DependencyGraph (immutable)                        │
│    - Leaf-to-leaf dependencies                              │
│    - Edge metadata (cyclic, upward, type)                   │
├─────────────────────────────────────────────────────────────┤
│  Tier 3: FocusState (mutable)                               │
│    - Expanded/collapsed namespaces                          │
│    - Pinned nodes                                           │
│    - Hovered node                                           │
├─────────────────────────────────────────────────────────────┤
│  Tier 4: AggregatedEdgeCache (computed)                     │
│    - Cached aggregated edges                                │
│    - Invalidated on state change                            │
├─────────────────────────────────────────────────────────────┤
│  Tier 5: HiddenNodesState (mutable)                         │
│    - Hidden nodes (what-if feature)                         │
│    - Tracking by parent                                     │
├─────────────────────────────────────────────────────────────┤
│  UI State (mutable)                                         │
│    - Selection, labels, filters, modes                      │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Phases

### Phase 1: Foundation (Week 1)

**Goal**: Set up the basic structure and types

#### Step 1.1: Create Directory Structure
```
visualization/src/app/model/v2/
├── types/
│   ├── ids.ts
│   ├── enums.ts
│   └── common.ts
├── tier1-structure/
├── tier2-dependencies/
├── tier3-focus/
├── tier4-aggregation/
├── tier5-hidden/
├── ui/
├── actions/
├── state-v2.ts
└── feature-flag.ts
```

#### Step 1.2: Implement Core Types
- [ ] `types/ids.ts` - Opaque ID types (NodeId, NamespaceId, LeafId)
- [ ] `types/enums.ts` - EdgeType, EdgeColor, NodeType, Language, FilterType
- [ ] `types/common.ts` - Shared interfaces (Position, Size, Bounds, etc.)

**Acceptance Criteria**:
- All types compile without errors
- ID types prevent mixing different ID kinds
- Enums match current implementation

#### Step 1.3: Set Up Feature Flag
- [ ] `feature-flag.ts` - Toggle between V1 and V2 models
- [ ] Add environment variable or localStorage support
- [ ] Add UI toggle for testing

**Acceptance Criteria**:
- Can switch between models at runtime
- Default to V1 for production
- V2 available for development/testing

### Phase 2: Immutable Tiers (Week 2-3)

**Goal**: Implement the immutable data structures

#### Step 2.1: Tier 1 - Project Structure
- [ ] `tier1-structure/namespace.ts` - Namespace class
- [ ] `tier1-structure/leaf.ts` - Leaf class
- [ ] `tier1-structure/project-structure.ts` - ProjectStructure class
- [ ] Converter from backend JSON format
- [ ] Unit tests for all classes

**Acceptance Criteria**:
- Can load project structure from JSON
- All lookups work correctly (getNamespace, getLeaf, getNode)
- Path operations work (getPathToRoot, findLowestCommonAncestor)
- Immutability enforced (readonly properties)

#### Step 2.2: Tier 2 - Dependency Graph
- [ ] `tier2-dependencies/dependency.ts` - Dependency class
- [ ] `tier2-dependencies/dependency-graph.ts` - DependencyGraph class
- [ ] Converter from backend JSON format
- [ ] Unit tests for all classes

**Acceptance Criteria**:
- Can load dependencies from JSON
- EdgeType correctly derived from isCyclic and isPointingUpwards
- Query methods work (getOutgoing, getIncoming, getAll)
- Filter methods work (byType, cyclic, upward)

### Phase 3: Mutable Tiers (Week 3-4)

**Goal**: Implement the mutable state tiers

#### Step 3.1: Tier 3 - Focus State
- [ ] `tier3-focus/focus-state.ts` - FocusState class
- [ ] Immutable update methods (expand, collapse, pin, unpin, hover)
- [ ] getVisibleNodes() implementation
- [ ] Unit tests

**Acceptance Criteria**:
- State updates are immutable (return new instance)
- getVisibleNodes() correctly computes visible nodes based on expansion
- Pinning/unpinning works correctly
- Hover state tracked correctly

#### Step 3.2: Tier 5 - Hidden Nodes State
- [ ] `tier5-hidden/hidden-nodes-state.ts` - HiddenNodesState class
- [ ] Immutable update methods (hide, restore, restoreAll)
- [ ] Parent tracking works correctly
- [ ] Unit tests

**Acceptance Criteria**:
- State updates are immutable
- Hidden nodes correctly tracked
- Parent-child relationships maintained
- Restore operations work correctly

#### Step 3.3: UI State
- [ ] `ui/ui-state.ts` - UIState class
- [ ] Immutable update methods
- [ ] Unit tests

**Acceptance Criteria**:
- All UI state properly encapsulated
- Immutable updates work
- Selection, filters, modes all functional

### Phase 4: Computed Tier (Week 4-5)

**Goal**: Implement the edge aggregation cache

#### Step 4.1: Tier 4 - Aggregated Edge Cache
- [ ] `tier4-aggregation/aggregated-edge.ts` - AggregatedEdge class
- [ ] `tier4-aggregation/edge-cache.ts` - EdgeCache class
- [ ] Edge aggregation algorithm
- [ ] Cache invalidation logic
- [ ] Unit tests

**Acceptance Criteria**:
- Edges correctly aggregated when namespaces collapsed
- Cache hit/miss working correctly
- Invalidation happens at right times
- Performance improvement measurable

**Key Algorithms**:
1. **calculateLeafEdges**: For visible leaf nodes, find best visible target
2. **calculateNamespaceEdges**: For collapsed namespaces, aggregate all contained dependencies
3. **findBestVisibleTarget**: Walk up namespace hierarchy to find visible ancestor
4. **applyFilter**: Filter edges based on EdgeFilterType
5. **applyFocus**: Show only edges connected to hovered/pinned nodes

### Phase 5: Composition (Week 5)

**Goal**: Compose all tiers into StateV2

#### Step 5.1: State Composition
- [ ] `state-v2.ts` - StateV2 class
- [ ] Compose all tiers
- [ ] Implement reduce() for actions
- [ ] Factory methods (create, fromProjectReport)
- [ ] Unit tests

**Acceptance Criteria**:
- All tiers properly composed
- State transitions work via reduce()
- Can load from backend JSON
- Derived state methods work (getVisibleNodes, getVisibleEdges)

#### Step 5.2: Actions
- [ ] `actions/action-types.ts` - Action type definitions
- [ ] `actions/reducers/` - Reducer functions for each tier
- [ ] Unit tests for all actions

**Acceptance Criteria**:
- All existing actions supported
- Reducers are pure functions
- State transitions are predictable
- Tests cover all action types

### Phase 6: Integration (Week 6-7)

**Goal**: Integrate new model with existing components

#### Step 6.1: Adapter Layer
- [ ] Create adapter to convert StateV2 to State (for backward compatibility)
- [ ] Create adapter to convert State to StateV2 (for migration)
- [ ] Unit tests for adapters

**Acceptance Criteria**:
- Can convert between models losslessly
- Adapters handle all edge cases
- Performance acceptable

#### Step 6.2: Component Integration
- [ ] Update app.component to support both models
- [ ] Update filter.component to work with both models
- [ ] Update other components as needed
- [ ] Integration tests

**Acceptance Criteria**:
- Components work with both models
- Feature flag switches between models seamlessly
- No visual differences between models

### Phase 7: Validation (Week 7-8)

**Goal**: Ensure new model is correct and performant

#### Step 7.1: Comparison Tests
- [ ] Create test suite that runs same operations on both models
- [ ] Compare outputs (visible nodes, visible edges)
- [ ] Identify and fix discrepancies
- [ ] Document any intentional differences

**Acceptance Criteria**:
- Both models produce identical results for all test cases
- All edge cases covered
- No regressions

#### Step 7.2: Performance Benchmarks
- [ ] Create benchmark suite
- [ ] Measure performance of key operations
- [ ] Compare V1 vs V2 performance
- [ ] Optimize bottlenecks

**Key Metrics**:
- Initial load time
- State transition time
- Edge aggregation time
- Memory usage
- Render time

**Acceptance Criteria**:
- V2 is at least as fast as V1
- Memory usage is reasonable
- No performance regressions

#### Step 7.3: End-to-End Testing
- [ ] Test with real project data
- [ ] Test all user interactions
- [ ] Test edge cases (large projects, deep hierarchies, many cycles)
- [ ] Fix any issues found

**Acceptance Criteria**:
- All features work correctly
- No crashes or errors
- User experience is smooth

### Phase 8: Migration (Week 8-9)

**Goal**: Switch to new model as default

#### Step 8.1: Documentation
- [ ] Update architecture documentation
- [ ] Create migration guide for developers
- [ ] Document new APIs
- [ ] Update code comments

**Acceptance Criteria**:
- All documentation up to date
- Migration guide is clear
- API documentation complete

#### Step 8.2: Switch Default
- [ ] Change feature flag default to V2
- [ ] Monitor for issues
- [ ] Keep V1 as fallback
- [ ] Gather feedback

**Acceptance Criteria**:
- V2 is default in production
- No major issues reported
- Performance is good
- Users don't notice the change

### Phase 9: Cleanup (Week 9-10)

**Goal**: Remove old model and finalize

#### Step 9.1: Remove V1 Model
- [ ] Remove old State class
- [ ] Remove old GraphNode classes
- [ ] Remove adapters
- [ ] Remove feature flag
- [ ] Update all imports

**Acceptance Criteria**:
- Old code completely removed
- No references to V1 model
- All tests pass
- Build is clean

#### Step 9.2: Final Documentation
- [ ] Update README
- [ ] Update DOMAIN.md if needed
- [ ] Create architecture diagrams
- [ ] Document lessons learned

**Acceptance Criteria**:
- Documentation is complete
- Diagrams are clear
- Future developers can understand the architecture

## Testing Strategy

### Unit Tests
- Each class has comprehensive unit tests
- Test all public methods
- Test edge cases and error conditions
- Aim for >90% code coverage

### Integration Tests
- Test tier interactions
- Test state transitions
- Test derived state calculations
- Test with realistic data

### Comparison Tests
- Run same operations on V1 and V2
- Compare outputs
- Ensure identical behavior
- Catch regressions early

### Performance Tests
- Benchmark key operations
- Compare V1 vs V2
- Identify bottlenecks
- Optimize as needed

### End-to-End Tests
- Test with real project data
- Test all user interactions
- Test edge cases
- Ensure smooth UX

## Risk Mitigation

### Risk 1: Breaking Changes
**Mitigation**: Parallel implementation with feature flag allows gradual migration

### Risk 2: Performance Regression
**Mitigation**: Continuous benchmarking, optimization before switching default

### Risk 3: Behavioral Differences
**Mitigation**: Comprehensive comparison tests, careful validation

### Risk 4: Complexity
**Mitigation**: Clear documentation, incremental implementation, code reviews

### Risk 5: Timeline Slippage
**Mitigation**: Phased approach allows partial delivery, feature flag allows rollback

## Success Criteria

1. ✅ New model implements all features of old model
2. ✅ New model is at least as fast as old model
3. ✅ New model has better separation of concerns
4. ✅ New model is more maintainable
5. ✅ New model is well-documented
6. ✅ New model is thoroughly tested
7. ✅ Migration is smooth with no user-visible issues
8. ✅ Code is cleaner and easier to understand

## Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| 1. Foundation | 1 week | Types, IDs, feature flag |
| 2. Immutable Tiers | 2 weeks | ProjectStructure, DependencyGraph |
| 3. Mutable Tiers | 2 weeks | FocusState, HiddenNodesState, UIState |
| 4. Computed Tier | 1 week | EdgeCache with aggregation |
| 5. Composition | 1 week | StateV2, Actions |
| 6. Integration | 2 weeks | Adapters, component updates |
| 7. Validation | 2 weeks | Tests, benchmarks, fixes |
| 8. Migration | 1 week | Switch default, monitor |
| 9. Cleanup | 1 week | Remove old code, finalize docs |
| **Total** | **13 weeks** | **Complete re-implementation** |

## Next Steps

1. Review this plan with the team
2. Get approval to proceed
3. Start Phase 1: Foundation
4. Set up regular check-ins to track progress
5. Adjust timeline as needed based on learnings

## Questions for Discussion

1. Should we implement all tiers before integration, or integrate incrementally?
2. What's the minimum viable version we can ship with the feature flag?
3. How long should we keep V1 as a fallback?
4. What metrics should we track to measure success?
5. Who will be responsible for each phase?

## Related Documents

- [`STATE_MODEL_ANALYSIS.md`](STATE_MODEL_ANALYSIS.md) - Analysis of current model
- [`MULTI_TIER_MODEL_DESIGN.md`](MULTI_TIER_MODEL_DESIGN.md) - Detailed design
- [`DOMAIN.md`](../../DOMAIN.md) - Domain model specification
- [`0002-use-a-rich-domain-model-in-visualization.md`](decisions/0002-use-a-rich-domain-model-in-visualization.md) - ADR on rich domain model