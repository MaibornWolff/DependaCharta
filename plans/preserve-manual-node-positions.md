# Plan: Preserve Manual Node Positions During Graph Updates

## Problem Statement

Currently, when a user manually drags nodes to reposition them in the Cytoscape graph, those positions are lost when another node is expanded or collapsed. This happens because the layout algorithm (`lsmLayout`) is re-run on every graph update, recalculating all node positions from scratch.

## User Requirements (from Q&A)

1. **Movement method**: User drags nodes with mouse (Cytoscape's default drag behavior)
2. **Trigger**: Positions reset when expanding/collapsing nodes to show/hide children
3. **Desired behavior**: Manually repositioned nodes should stay where placed, even after graph changes
4. **Scope**: Only manually dragged nodes should have preserved positions; other nodes should continue to use automatic layout
5. **Note**: Existing "pin" feature is unrelated to this issue

## Current Architecture Analysis

### Position Calculation Flow
1. **State change** (expand/collapse) → `State.getVisibleNodes()` returns updated node list
2. **Graph update** → `cytoscape.service.ts:updateGraph()` calls `cy.layout({name: 'lsmLayout'}).run()` (line 103)
3. **Layout calculation** → `lsmLayouting.ts:layout()` computes hierarchical positions for ALL nodes
4. **Position application** → `CyLsmLayout.ts` applies positions via `layoutPositions()` callback
5. **Rendering** → Positions converted to absolute coordinates and applied to DOM via `RenderInformation`

### Key Files Involved
- `visualization/src/app/adapter/cytoscape/internal/cytoscape.service.ts` - Layout triggering (line 103)
- `visualization/src/app/model/lsmLayouting.ts` - Layout algorithm implementation
- `visualization/src/app/model/State.ts` - Application state (currently no position storage)
- `visualization/src/app/adapter/cytoscape/internal/CyLsmLayout.ts` - Layout application
- `visualization/src/app/adapter/cytoscape/internal/converter/coordinateConverter.ts` - Coordinate conversion

## Proposed Solution

### Approach: Hybrid Layout System

Implement a system that:
1. **Tracks** which nodes have been manually repositioned by the user
2. **Stores** those manual positions in application state
3. **Applies hybrid layout**:
   - Run `lsmLayout` for all nodes to get computed positions
   - Override computed positions with stored manual positions for dragged nodes
   - New nodes (from expansion) automatically get positioned by layout algorithm

### Implementation Steps

#### 1. **Add Manual Position Tracking to State**
**File**: `visualization/src/app/model/State.ts`

Add new state field:
```typescript
declare readonly manuallyPositionedNodes: Map<string, Coordinates>
```

Add new actions:
```typescript
export class SetNodeManualPosition {
  constructor(public nodeId: string, public position: Coordinates) {}
}

export class ClearNodeManualPosition {
  constructor(public nodeId: string) {}
}

export class ClearAllManualPositions {}
```

Update state reducer to handle these actions.

**Why**: State is the single source of truth; positions should survive component re-renders.

#### 2. **Detect Manual Node Dragging**
**File**: `visualization/src/app/adapter/cytoscape/internal/cytoscape.service.ts`

Add event listener for manual node dragging:
```typescript
cy.on('drag', 'node', (event) => {
  // Emit event with node ID and new position
})

cy.on('free', 'node', (event) => {
  // When drag ends, store the final position in State
})
```

**Why**: `drag` fires during dragging, `free` fires when user releases the node. We need `free` to capture the final position.

#### 3. **Modify Layout Application to Respect Manual Positions**
**File**: `visualization/src/app/adapter/cytoscape/internal/CyLsmLayout.ts`

Update `layoutPositions` callback to:
1. Check if node has manual position in state
2. If yes, use manual position
3. If no, use computed layout position

```typescript
cyNodes.layoutPositions(layouting, layoutOptions, (currentNode: NodeSingular) => {
  const manualPosition = state.manuallyPositionedNodes.get(currentNode.data().id)
  if (manualPosition) {
    return manualPosition  // Use stored manual position
  }

  // Fall back to computed position
  const coordinates = nodesWithAbsoluteCoordinates.get(currentNode.data().id)
  return coordinates || { x: 0, y: 0 }
})
```

**Why**: This preserves manual positions while allowing automatic layout for other nodes.

#### 4. **Handle Edge Cases and Child Position Clearing**

**Child Position Clearing on Collapse** (User Requirement):
When a parent node is collapsed:
- **Parent behavior**: Keep the parent node's manual position (it remains positioned where the user placed it)
- **Children behavior**: Clear manual positions for ALL descendants (recursively)
- **Trigger**: Only on `CollapseNode` action (not on expand)
- **Rationale**: When children are hidden, their positions become irrelevant. When re-expanded, they should get fresh automatic layout positions relative to the parent.

**Implementation Details**:
```typescript
// In State reducer for Action.CollapseNode
case action instanceof Action.CollapseNode:
  // First find the node being collapsed
  const collapsedNode = this.allNodes.find(node => node.id === action.nodeId)

  // Get all descendant IDs (but not the collapsed node itself)
  const descendantIds = collapsedNode
    ? Array.from(getDescendants(collapsedNode))
        .map(node => node.id)
        .filter(id => id !== action.nodeId) // Exclude the parent
    : []

  // Remove descendant positions from manuallyPositionedNodes
  const updatedManualPositions = new Map(this.manuallyPositionedNodes)
  descendantIds.forEach(id => updatedManualPositions.delete(id))

  return this.copy({
    expandedNodeIds: this.expandedNodeIds.filter(id => !IdUtils.isDescendantOf([action.nodeId])(id)),
    manuallyPositionedNodes: updatedManualPositions
  })
```

**Why this approach**:
- Uses existing `getDescendants()` utility function from GraphNode.ts
- Preserves the parent node's position by filtering it out
- Cleans up all nested children recursively in one operation
- Maintains immutability by creating a new Map

**Deleted/Hidden Nodes**: Clean up manual positions for nodes that no longer exist:
- Add cleanup logic in state reducer when nodes are removed
- Or implement periodic garbage collection

#### 5. **Add Reset Functionality (Optional but Recommended)**

Add UI control to reset layout:
- Button: "Reset Layout"
- Action: Dispatches `ClearAllManualPositions`
- Triggers layout recalculation

**Why**: Users might want to start fresh if they mess up positioning.

#### 6. **Consider Position Persistence (Future Enhancement)**

For future consideration:
- Save manual positions to localStorage or file
- Restore positions when re-opening the same analysis
- Would require keying positions by project/file hash

**Not included in this plan** - can be added later if needed.

## Testing Strategy

### Unit Tests (TDD approach)

1. **State Management Tests** (`State.spec.ts`)
   - Test `SetNodeManualPosition` action
   - Test `ClearNodeManualPosition` action
   - Test `ClearAllManualPositions` action
   - Test position persistence through state updates
   - **NEW**: Test `CollapseNode` clears all descendant positions but keeps parent position
   - **NEW**: Test nested collapse clears all nested descendants
   - **NEW**: Test `ExpandNode` does NOT clear positions

2. **Layout Tests** (`CyLsmLayout.spec.ts`)
   - Test layout with no manual positions (existing behavior)
   - Test layout with manual positions - verify override
   - Test layout with mix of manual and auto positions

3. **Cytoscape Service Tests** (`cytoscape.service.spec.ts`)
   - Test drag event detection
   - Test position capture on `free` event
   - Test state update dispatch

### Integration Tests

4. **E2E Tests** (Cypress)
   - Test: Drag node → expand another node → verify first node stayed in place
   - Test: Drag multiple nodes → collapse/expand → verify all manual positions preserved
   - **NEW**: Test: Drag parent and children → collapse parent → verify parent position kept, children cleared
   - **NEW**: Test: Expand parent with previously positioned children → verify children get new automatic layout
   - Test: Reset layout button → verify all positions cleared

## Open Questions

**Q1**: Should there be a visual indicator for manually positioned nodes (e.g., different border color)?
- **Recommendation**: Not necessary for MVP, but could add subtle visual feedback (e.g., brief highlight when position is locked)

**Q2**: Should manual positioning work for both compound and non-compound nodes, or just non-compound?
- **Recommendation**: Both - no reason to restrict
- **Note**: Compound nodes can contain children, so dragging them should move the whole subtree

**Q3**: When a parent compound node is manually positioned, should children positions be relative or absolute?
- **Recommendation**: Keep children relative to parent - matches existing behavior
- **Implementation**: Store absolute position for parent; children maintain relative positions from layout

## Implementation Order (TDD Red-Green-Refactor)

1. **Test 1**: State stores manual position for a node
2. **Test 2**: State clears manual position for a node
3. **Test 3**: State clears all manual positions
4. **Test 4**: CollapseNode clears descendant positions but keeps parent position
5. **Test 5**: Nested collapse clears all nested descendants
6. **Test 6**: ExpandNode does NOT clear any positions
7. **Test 7**: Layout uses manual position when available
8. **Test 8**: Cytoscape service detects drag event and updates state
9. **Test 9**: E2E test - drag and expand scenario
10. **Test 10**: E2E test - collapse clears child positions
11. **Refactor**: Clean up, optimize, add reset functionality

## Risk Assessment

**Low Risk**:
- Clear separation of concerns (state, layout, rendering)
- Minimal changes to existing layout algorithm
- Backwards compatible (no manual positions = current behavior)

**Considerations**:
- State size could grow with many manually positioned nodes (acceptable for typical graphs)
- Coordinate system must be consistent between Cytoscape and our layout (verify coordinate transformations)

## Estimated Complexity

- **State changes**: Simple (map storage + actions)
- **Event handling**: Simple (Cytoscape events well-documented)
- **Layout modification**: Medium (requires understanding coordinate systems)
- **Testing**: Medium (need both unit and E2E tests)

**Overall**: Medium complexity, well-scoped change

## Success Criteria

✅ User can drag a node to reposition it
✅ Manually positioned node stays in place when other nodes are expanded/collapsed
✅ New nodes from expansion get automatic layout positions
✅ Multiple manually positioned nodes all retain their positions
✅ **NEW**: When a parent node is collapsed, its own position is preserved but all children positions are cleared
✅ **NEW**: Nested children positions are cleared recursively when ancestor is collapsed
✅ **NEW**: Expanding a node does NOT clear any positions
✅ Reset functionality clears all manual positions
✅ All tests passing (unit + E2E)
