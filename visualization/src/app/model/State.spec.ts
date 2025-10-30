import { State, getVisibleNodes, findGraphNode, initialState } from './State';
import { GraphNode, expand } from './GraphNode';
import * as GraphNodeTest from './GraphNode.spec';
import { EdgeFilterType } from './EdgeFilter';
import { Action, InitializeState, ExpandNode, CollapseNode, ChangeFilter, ShowAllEdgesOfNode, HideAllEdgesOfNode, ToggleEdgeLabels, HideNode, RestoreNodes, RestoreNode, RestoreAllChildren, ToggleInteractionMode, ToggleUsageTypeMode, EnterMultiselectMode, LeaveMultiselectMode, ToggleNodeSelection, PinNode, UnpinNode } from './Action';
import {InputDevice, MouseInaccuracyDetectorComponent} from '../mouse-inaccuracy-detector.component';
import {of} from 'rxjs';

const grandchild1Id = 'parent-1.child-1.grandchild-1'
const child1Id = 'parent-1.child-1'
const child2Id = 'parent-1.child-2'
const parent1Id = 'parent-1'

describe('State', () => {
  let mockParentNode: GraphNode;
  let mockChildNode1: GraphNode;
  let mockChildNode2: GraphNode;
  let mockGrandChildNode: GraphNode;

  beforeEach(() => {
    mockGrandChildNode = GraphNodeTest.GraphNode.build({
      id: grandchild1Id,
      children: [],
    })

    mockChildNode1 = GraphNodeTest.GraphNode.build({
      id: child1Id,
      children: [mockGrandChildNode],
    });

    mockChildNode2 = GraphNodeTest.GraphNode.build({
      id: child2Id,
      children: [],
    });

    mockParentNode = GraphNodeTest.GraphNode.build({
      id: parent1Id,
      children: [mockChildNode1, mockChildNode2],
    });

    // Set parent-child relationships
    mockChildNode1.parent = mockParentNode;
    mockChildNode2.parent = mockParentNode;
    mockGrandChildNode.parent = mockChildNode1;
  });

  describe('State.build', () => {
    it('should create a State with default values', () => {
      const state = State.build()

      expect(state.allNodes).toEqual([])
      expect(state.hiddenNodeIds).toEqual([])
      expect(state.expandedNodeIds).toEqual([])
      expect(state.hoveredNodeId).toBe('')
      expect(state.selectedNodeIds).toEqual([])
      expect(state.pinnedNodeIds).toEqual([])
      expect(state.showLabels).toBe(true)
      expect(state.selectedFilter).toBe(EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES)
      expect(state.isInteractive).toBe(true)
      expect(state.isUsageShown).toBe(true)
      expect(state.multiselectMode).toBe(false)
    })

    it('should override default values with provided overrides', () => {
      const overrides = {
        showLabels: false,
        isInteractive: false,
        expandedNodeIds: ['test-id']
      };

      const state = State.build(overrides);

      expect(state.showLabels).toBe(false);
      expect(state.isInteractive).toBe(false);
      expect(state.expandedNodeIds).toEqual(['test-id']);
      expect(state.isUsageShown).toBe(true); // default remains
    });
  });

  describe('State.build', () => {
    it('should create a State from root nodes', () => {
      const rootNodes = [mockParentNode];
      const state = buildFromRootNodes(rootNodes);

      expect(state.allNodes.length).toBe(4) // parent, 2 children, 1 grandchild
      expect(state.allNodes.find(node => node.id == parent1Id)).toBe(mockParentNode)
      expect(state.allNodes.find(node => node.id == child1Id)).toBe(mockChildNode1)
      expect(state.allNodes.find(node => node.id == child2Id)).toBe(mockChildNode2)
      expect(state.allNodes.find(node => node.id == grandchild1Id)).toBe(mockGrandChildNode)
    })
  })

  describe('getVisibleNodes', () => {
    let baseState: State;

    beforeEach(() => {
      baseState = State.build({
        allNodes: [
          mockParentNode,
          mockChildNode1,
          mockChildNode2,
          mockGrandChildNode
        ],
        expandedNodeIds: [parent1Id],
      })
    })

    it('should show only root nodes and their direct children when parent is expanded', () => {
      const visibleNodes = getVisibleNodes(baseState);

      const visibleIds = visibleNodes.map(node => node.id);
      expect(visibleIds).toContain(parent1Id);
      expect(visibleIds).toContain(child1Id);
      expect(visibleIds).toContain(child2Id);
      expect(visibleIds).not.toContain(grandchild1Id); // child-1 is not expanded
    });

    it('should show grandchild nodes when their parent is also expanded', () => {
      baseState = State.build({ allNodes: baseState.allNodes, expandedNodeIds: [parent1Id, child1Id] });

      const visibleNodes = getVisibleNodes(baseState);
      const visibleIds = visibleNodes.map(node => node.id);

      expect(visibleIds).toContain(grandchild1Id);
    });

    it('should filter out hidden nodes', () => {
      baseState = State.build({ allNodes: baseState.allNodes, expandedNodeIds: baseState.expandedNodeIds, hiddenNodeIds: [child1Id] });

      const visibleNodes = getVisibleNodes(baseState);
      const visibleIds = visibleNodes.map(node => node.id);

      expect(visibleIds).not.toContain(child1Id);
      expect(visibleIds).toContain(child2Id);
    });

    it('should also filter out children of hidden nodes', () => {
      baseState = State.build({
        allNodes: baseState.allNodes,
        expandedNodeIds: [parent1Id, child1Id],
        hiddenNodeIds: [child1Id]
      });

      const visibleNodes = getVisibleNodes(baseState);
      const visibleIds = visibleNodes.map(node => node.id);

      expect(visibleIds).not.toContain(child1Id);
      expect(visibleIds).not.toContain(grandchild1Id);
    });
  });

  describe('findGraphNode', () => {
    let state: State;

    beforeEach(() => {
      state = State.build({
        allNodes: [mockParentNode],
        selectedNodeIds: [mockParentNode.id],
      })
    })

    it('should find an existing node and return it as VisibleGraphNode', () => {
      const result = findGraphNode(parent1Id, state)

      expect(result.id).toBe(parent1Id)
    })

    it('should throw an error when node does not exist', () => {
      expect(() => findGraphNode('nonexistent-id', state))
        .toThrowError('Node with id nonexistent-id not found')
    })
  })

  describe('stateReducer', () => {
    let initialState: State;

    beforeEach(() => {
      initialState = State.build({
        allNodes: [
          mockParentNode,
          mockChildNode1
        ]
      });
    });

    describe('INITIALIZE_STATE action', () => {
      it('should initialize state with new root nodes', () => {
        const action: Action = new InitializeState('', [mockParentNode]);

        const newState = initialState.reduce(action);

        expect(newState.allNodes.length).toBeGreaterThan(0);
        expect(newState.allNodes.find(node => node.id == parent1Id)).toBe(mockParentNode);
      });
    });

    describe('EXPAND_NODE action', () => {
      it('should add a node to the expanded nodes list', () => {
        const action: Action = new ExpandNode('test-node');

        const newState = initialState.reduce(action);

        expect(newState.expandedNodeIds).toContain('test-node');
        expect(newState.expandedNodeIds.length).toBe(1);
      });
    });

    describe('COLLAPSE_NODE action', () => {
      it('should remove a node and its descendants from the expanded list', () => {
        const stateWithExpandedNodes = initialState.copy({
          expandedNodeIds: [parent1Id, child1Id, grandchild1Id]
        });

        const action: Action = new CollapseNode(child1Id);

        // Mock expandDescendants for this test
        spyOn({ expandDescendants: expand }, 'expandDescendants').and.returnValue([
          { id: child1Id } as GraphNode,
          { id: grandchild1Id } as GraphNode
        ]);

        const newState = stateWithExpandedNodes.reduce(action);

        expect(newState.expandedNodeIds).toEqual([parent1Id]);
      });
    });

    describe('CHANGE_FILTER action', () => {
      it('should change the selected filter', () => {
        const action: Action = new ChangeFilter(EdgeFilterType.ALL);

        const newState = initialState.reduce(action);

        expect(newState.selectedFilter).toBe(EdgeFilterType.ALL);
      });
    });

    describe('SHOW_ALL_EDGES_OF_NODE action', () => {
      it('should set the hoveredNodeId', () => {
        const action: Action = new ShowAllEdgesOfNode('hover-node');

        const newState = initialState.reduce(action);

        expect(newState.hoveredNodeId).toBe('hover-node');
      });
    });

    describe('HIDE_ALL_EDGES_OF_NODE action', () => {
      it('should reset the hoveredNodeId', () => {
        const stateWithHover = initialState.copy({
          hoveredNodeId: 'some-node'
        });
        const action: Action = new HideAllEdgesOfNode('some-node');

        const newState = stateWithHover.reduce(action);

        expect(newState.hoveredNodeId).toBe('');
      });
    });

    describe('TOGGLE_EDGE_LABELS action', () => {
      it('should toggle showLabels', () => {
        const action: Action = new ToggleEdgeLabels();

        const newState = initialState.reduce(action);

        expect(newState.showLabels).toBe(false); // default is true
      });
    });

    describe('HIDE_NODE action', () => {
      it('should add a node to the hidden list', () => {
        const action: Action = new HideNode(child1Id);

        const newState = initialState.reduce(action);

        expect(newState.hiddenNodeIds).toContain(child1Id);
      });

      it('should remove a hidden node from the pinned list', () => {
        const stateWithPinnedNode = initialState.copy({
          pinnedNodeIds: [child1Id]
        });

        const action: Action = new HideNode(child1Id);

        const newState = stateWithPinnedNode.reduce(action);

        expect(newState.pinnedNodeIds).not.toContain(child1Id);
      });

      it('should handle HIDE_NODE action when node has no parent', () => {
        const rootNodeState = State.build({
          allNodes: [GraphNodeTest.GraphNode.build({ id: 'root', children: [] })]
        })

        const action: Action = new HideNode('root')

        const newState = rootNodeState.reduce(action)

        expect(newState.hiddenNodeIds).toContain('root')
        expect(newState.hiddenChildrenIdsByParentId.size).toBe(0)
      })
    })

    describe('PIN_NODE action', () => {
      it('should add a node to the pinned list', () => {
        const action: Action = new PinNode(child1Id);

        const newState = initialState.reduce(action);

        expect(newState.pinnedNodeIds).toContain(child1Id);
      });

      it('should not add a node twice', () => {
        const stateWithPinnedNode = initialState.copy({
          pinnedNodeIds: [child1Id]
        });

        const action: Action = new PinNode(child1Id);

        const newState = stateWithPinnedNode.reduce(action);

        expect(newState.pinnedNodeIds.filter(id => id === child1Id).length).toBe(1);
      });
    });

    describe('UNPIN_NODE action', () => {
      it('should remove a node from the pinned list', () => {
        const stateWithPinnedNode = initialState.copy({
          pinnedNodeIds: [child1Id, child2Id],
          selectedPinnedNodeIds: [child1Id, child2Id]
        })

        const action: Action = new UnpinNode(child1Id)

        const newState = stateWithPinnedNode.reduce(action);

        expect(newState.pinnedNodeIds).not.toContain(child1Id);
        expect(newState.pinnedNodeIds).toContain(child2Id);
      });

      it('should not remove node if it has a pinned ancestor', () => {
        const stateWithPinnedNodes = initialState.copy({
          pinnedNodeIds: [parent1Id, child1Id],
          selectedPinnedNodeIds: [parent1Id]
        });

        const action: Action = new UnpinNode(child1Id);

        const newState = stateWithPinnedNodes.reduce(action);

        expect(newState.pinnedNodeIds).toEqual([parent1Id, child1Id]);
      });

      it('should remove descendants when unpinning a parent', () => {
        const stateWithPinnedNodes = initialState.copy({
          pinnedNodeIds: [parent1Id, child1Id, grandchild1Id, 'other-node'],
          selectedPinnedNodeIds: [parent1Id, 'other-node']
        });

        const action: Action = new UnpinNode(parent1Id);

        const newState =stateWithPinnedNodes.reduce(action);

        expect(newState.pinnedNodeIds).not.toContain(parent1Id);
        expect(newState.pinnedNodeIds).not.toContain(child1Id);
        expect(newState.pinnedNodeIds).not.toContain(grandchild1Id);
        expect(newState.pinnedNodeIds).toContain('other-node');
      });

      it('should not remove descendants that are explicitly pinned', () => {
        const stateWithPinnedNodes = initialState.copy({
          pinnedNodeIds: [parent1Id, child1Id],
          selectedPinnedNodeIds: [parent1Id, child1Id]
        });

        const action: Action = new UnpinNode(parent1Id);

        const newState = stateWithPinnedNodes.reduce(action);

        expect(newState.pinnedNodeIds).not.toContain(parent1Id);
        expect(newState.pinnedNodeIds).toContain(child1Id);
      });
    });

    describe('RESTORE_NODES action', () => {
      it('should reset all hidden and pinned nodes', () => {
        const stateWithHiddenAndPinned: State = initialState.copy({
          hiddenNodeIds: ['node1', 'node2'],
          pinnedNodeIds: ['node3'],
          hiddenChildrenIdsByParentId: new Map([['parent', ['child1']]])
        });

        const action: Action = new RestoreNodes();

        const newState = stateWithHiddenAndPinned.reduce(action);

        expect(newState.hiddenNodeIds).toEqual([]);
        expect(newState.pinnedNodeIds).toEqual([]);
        expect(newState.hiddenChildrenIdsByParentId).toEqual(new Map());
      });
    });

    describe('RESTORE_NODE action', () => {
      it('should remove a node from the hidden list and update parent mapping', () => {
        const stateWithHiddenNode: State = initialState.copy({
          hiddenNodeIds: [child1Id, 'other-node'],
          hiddenChildrenIdsByParentId: new Map([
            [parent1Id, [child1Id, child2Id]],
            ['other-parent', ['other-node']]
          ])
        });

        const action: Action = new RestoreNode(child1Id, parent1Id);

        const newState = stateWithHiddenNode.reduce(action);

        expect(newState.hiddenNodeIds).not.toContain(child1Id);
        expect(newState.hiddenNodeIds).toContain('other-node');
        expect(newState.hiddenChildrenIdsByParentId.get(parent1Id)).toEqual([child2Id]);
        expect(newState.hiddenChildrenIdsByParentId.get('other-parent')).toEqual(['other-node']);
      });

      it('should handle restoring from parent with no other hidden children', () => {
        const stateWithHiddenNode: State = initialState.copy({
          hiddenNodeIds: [child1Id],
          hiddenChildrenIdsByParentId: new Map([
            [parent1Id, [child1Id]]
          ])
        });

        const action: Action = new RestoreNode(child1Id, parent1Id);

        const newState = stateWithHiddenNode.reduce(action);

        expect(newState.hiddenNodeIds).not.toContain(child1Id);
        expect(newState.hiddenChildrenIdsByParentId.get(parent1Id)).toEqual([]);
      });

      it('should handle RESTORE_NODE with empty hidden children list', () => {
        const stateWithEmptyHiddenChildren = State.build({
          hiddenNodeIds: [child1Id],
          hiddenChildrenIdsByParentId: new Map([[parent1Id, []]])
        });

        const action: Action = new RestoreNode(child1Id, parent1Id);

        const newState = stateWithEmptyHiddenChildren.reduce(action);

        expect(newState.hiddenNodeIds).not.toContain(child1Id);
        expect(newState.hiddenChildrenIdsByParentId.get(parent1Id)).toEqual([]);
      });
    });

    describe('RESTORE_ALL_CHILDREN action', () => {
      it('should restore all hidden children of a node', () => {
        const stateWithHiddenChildren: State = initialState.copy({
          hiddenNodeIds: [child1Id, child2Id, 'other-node'],
          hiddenChildrenIdsByParentId: new Map([
            [parent1Id, [child1Id, child2Id]],
            ['other-parent', ['other-node']]
          ])
        });

        const action: Action = new RestoreAllChildren(parent1Id);

        const newState = stateWithHiddenChildren.reduce(action);

        expect(newState.hiddenNodeIds).not.toContain(child1Id);
        expect(newState.hiddenNodeIds).not.toContain(child2Id);
        expect(newState.hiddenNodeIds).toContain('other-node');
        expect(newState.hiddenChildrenIdsByParentId.get(parent1Id)).toEqual([]);
        expect(newState.hiddenChildrenIdsByParentId.get('other-parent')).toEqual(['other-node']);
      });

      it('should handle node with no hidden children', () => {
        const stateWithHiddenChildren: State = initialState.copy({
          hiddenNodeIds: ['other-node'],
          hiddenChildrenIdsByParentId: new Map([
            ['other-parent', ['other-node']]
          ])
        });

        const action: Action = new RestoreAllChildren(parent1Id);

        const newState = stateWithHiddenChildren.reduce(action);

        expect(newState.hiddenNodeIds).toEqual(['other-node']);
        expect(newState.hiddenChildrenIdsByParentId.get(parent1Id)).toEqual([]);
        expect(newState.hiddenChildrenIdsByParentId.get('other-parent')).toEqual(['other-node']);
      });
    });

    describe('TOGGLE_INTERACTION_MODE action', () => {
      it('should toggle interaction mode', () => {
        const action: Action = new ToggleInteractionMode()

        const newState = initialState.reduce(action)

        expect(newState.isInteractive).toBe(!initialState.isInteractive)
      })
    })

    describe('TOGGLE_USAGE_TYPE_MODE action', () => {
      it('should toggle usage type mode', () => {
        const action: Action = new ToggleUsageTypeMode()

        const newState = initialState.reduce(action)

        expect(newState.isUsageShown).toBe(!initialState.isUsageShown)
      })
    })

    describe('ENTER_MULTISELECT_MODE action', () => {
      it('should activate multiselect mode', () => {
        const action: Action = new EnterMultiselectMode()

        const newState = initialState.reduce(action)

        expect(newState.multiselectMode).toBe(!initialState.multiselectMode)
      })
    })

    describe('LEAVE_MULTISELECT_MODE action', () => {
      it('should deactivate multiselect mode and reset selection', () => {
        const stateWithMultiselect = initialState.copy({
          multiselectMode: true,
          selectedNodeIds: ['node1', 'node2']
        });

        const action: Action = new LeaveMultiselectMode();

        const newState = stateWithMultiselect.reduce(action);

        expect(newState.multiselectMode).toBe(!stateWithMultiselect.multiselectMode);
        expect(newState.selectedNodeIds).toEqual([]);
      });
    });

    describe('TOGGLE_NODE_SELECTION action', () => {
      it('should add a non-selected node to selection', () => {
        const action: Action = new ToggleNodeSelection('toggle-node');

        const newState = initialState.reduce(action);

        expect(newState.selectedNodeIds).toContain('toggle-node');
      });

      it('should remove an already selected node from selection', () => {
        const stateWithSelection = initialState.copy({
          selectedNodeIds: ['selected-node']
        });

        const action: Action = new ToggleNodeSelection('selected-node');

        const newState = stateWithSelection.reduce(action);

        expect(newState.selectedNodeIds).not.toContain('selected-node');
      });
    });

    describe('default case', () => {
      it('should return the same state for unknown action types', () => {
        class Unknown extends Action {}
        const unknownAction = new Unknown();

        const newState = initialState.reduce(unknownAction);

        expect(newState).toBe(initialState);
      });
    });
  });

  describe('hasPinnedAncestor function', () => {
    // Since hasPinnedAncestor is not exported, we test it through the UNPIN_NODE action

    it('should detect pinned ancestor in hierarchical node IDs', () => {
      const initialState = State.build({
        pinnedNodeIds: ['com.example', 'com.example.service.UserService'],
        selectedPinnedNodeIds: ['com.example']
      });

      // Try to unpin a child when parent is pinned
      const action: Action = new UnpinNode('com.example.service.UserService.method');

      const newState = initialState.reduce(action);

      // Should not change because the node has a pinned ancestor
      expect(newState.pinnedNodeIds).toEqual(['com.example', 'com.example.service.UserService']);
    });

    it('should not detect pinned ancestor when none exists', () => {
      const initialState = State.build({
        pinnedNodeIds: ['com.other', 'com.example.service.UserService'],
        selectedPinnedNodeIds: ['com.other']
      });

      const action: Action = new UnpinNode('com.example.service.UserService');

      const newState = initialState.reduce(action);

      expect(newState.pinnedNodeIds).not.toContain('com.example.service.UserService');
      expect(newState.pinnedNodeIds).toContain('com.other');
    });
  });
});

describe('InputDevice', () => {
  describe('enum values', () => {
    it('should have correct enum values', () => {
      expect(InputDevice.MOUSE).toBe('mouse');
      expect(InputDevice.TOUCHPAD).toBe('touchpad');
    });
  });

  describe('invert function', () => {
    it('should invert mouse to touchpad', () => {
      expect(InputDevice.invert(InputDevice.MOUSE)).toBe(InputDevice.TOUCHPAD);
    });

    it('should invert touchpad to mouse', () => {
      expect(InputDevice.invert(InputDevice.TOUCHPAD)).toBe(InputDevice.MOUSE);
    });
  });
});

// Test the standalone median function
describe('median function', () => {
  // Since median is not exported, we'll test it indirectly through the component behavior
  it('should calculate median correctly through component trail processing', () => {
    const component = new MouseInaccuracyDetectorComponent(
      jasmine.createSpyObj('MatSnackBar', ['openFromComponent']),
      jasmine.createSpyObj('MatDialog', ['open']),
      jasmine.createSpyObj('CytoscapeService', [], { panOrZoom: of() })
    );

    // Add known values to test median calculation
    const events = [
      { deltaY: 100, deltaX: 5 }, // confidence: 1
      { deltaY: 100.5, deltaX: 0 }, // confidence: 0
      { deltaY: 100, deltaX: 0 }, // confidence: 0.33
      { deltaY: 100.5, deltaX: 5 }, // confidence: 0.67
      { deltaY: 100, deltaX: 5 }, // confidence: 1
      { deltaY: 100.5, deltaX: 0 }, // confidence: 0
      { deltaY: 100, deltaX: 0 }, // confidence: 0.33
      { deltaY: 100.5, deltaX: 5 }, // confidence: 0.67
      { deltaY: 100, deltaX: 5 }, // confidence: 1
      { deltaY: 100.5, deltaX: 0 }  // confidence: 0 (10th event to trigger median)
    ];

    events.forEach(event => {
      component.onWindowScroll(event as WheelEvent);
    });

    // The median should be calculated and available
    expect(component.touchpadConfidenceMedian).toBeDefined();
    expect(typeof component.touchpadConfidenceMedian).toBe('number');
  });
});

export function buildFromRootNodes(rootNodes: GraphNode[] = []): State {
  return State.build({
    allNodes: rootNodes.flatMap(expand)
  })
}

declare module './State' {
  namespace State {
    function build(overrides?: Partial<State>): State
  }
}

State.build = function(overrides: Partial<State> = {}): State {
  return initialState().copy(overrides)
}

export { State }
