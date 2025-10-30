import {expand, getDescendants, GraphNode, VisibleGraphNode} from "./GraphNode";
import {EdgeFilterType} from "./EdgeFilter";
import {Action, InitializeState, ExpandNode, CollapseNode, ChangeFilter, ShowAllEdgesOfNode, HideAllEdgesOfNode, ToggleEdgeLabels, HideNode, RestoreNode, RestoreNodes, RestoreAllChildren, ToggleInteractionMode, ToggleUsageTypeMode, ResetView, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, PinNode, UnpinNode} from './Action';

// TODO avoid Maps (→ (de-)serialization issues)
export class State {
  constructor(
    public readonly allNodes: GraphNode[],
    public readonly hiddenNodeIds: string[],
    public readonly hiddenChildrenIdsByParentId: Map<string, string[]>,
    public readonly expandedNodeIds: string[],
    public readonly hoveredNodeId: string,
    public readonly selectedNodeIds: string[],
    public readonly pinnedNodeIds: string[],
    public readonly selectedPinnedNodeIds: string[],

    public readonly showLabels: boolean,
    public readonly selectedFilter: EdgeFilterType,
    public readonly isInteractive: boolean,
    public readonly isUsageShown: boolean,
    public readonly multiselectMode: boolean
  ) {}
}

export interface State {
  reduce(action: Action): State
  copy(overrides: Partial<State>): State
  foo(): void
}

State.prototype.foo = function() {
  // throw new Error('H!')
}

State.prototype.reduce = function(action: Action) {
  return reduce(this, action)
}

State.prototype.copy = function (overrides: Partial<State> = {}): State {
  return new State(
    overrides.allNodes ?? this.allNodes,
    overrides.hiddenNodeIds ?? this.hiddenNodeIds,
    overrides.hiddenChildrenIdsByParentId ?? this.hiddenChildrenIdsByParentId,
    overrides.expandedNodeIds ?? this.expandedNodeIds,
    overrides.hoveredNodeId ?? this.hoveredNodeId,
    overrides.selectedNodeIds ?? this.selectedNodeIds,
    overrides.pinnedNodeIds ?? this.pinnedNodeIds,
    overrides.selectedPinnedNodeIds ?? this.selectedPinnedNodeIds,
    overrides.showLabels ?? this.showLabels,
    overrides.selectedFilter ?? this.selectedFilter,
    overrides.isInteractive ?? this.isInteractive,
    overrides.isUsageShown ?? this.isUsageShown,
    overrides.multiselectMode ?? this.multiselectMode
  )
}


export const initialState = () => new State(
  [],
  [],
  new Map<string, string[]>(),
  [],
  '',
  [],
  [],
  [],
  true,
  EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES,
  true,
  true,
  false
)

export function getVisibleNodes(state: State): VisibleGraphNode[] {
  const expandedNodes = state.allNodes.filter(node => state.expandedNodeIds.includes(node.id))
  return state.allNodes
    .filter(node => !node.parent || expandedNodes.includes(node.parent))
    .filter(node => !isNodeOrAncestorHidden(state.hiddenNodeIds, node))
    .map(node => toVisibleGraphNode(node, state))
}

function isNodeOrAncestorHidden(hiddenChildrenIds: string[], child: GraphNode): boolean {
  if (hiddenChildrenIds.includes(child.id)) {
    return true;
  }
  let parent = child.parent;
  while (parent) {
    if (hiddenChildrenIds.includes(parent.id)) {
      return true;
    }
    parent = parent.parent;
  }
  return false;
}

function toVisibleGraphNode(graphNode: GraphNode, state: State): VisibleGraphNode {
  const hiddenChildrenIds = state.hiddenChildrenIdsByParentId.get(graphNode.id) || []
  const isExpanded = state.expandedNodeIds.includes(graphNode.id)
  const isSelected = state.selectedNodeIds.includes(graphNode.id)
  const visibleChildren = isExpanded ?
    graphNode.children
      .map(child => toVisibleGraphNode(child, state))
      .filter(child => !isNodeOrAncestorHidden(hiddenChildrenIds, child))
    : [];
  return {
    ...graphNode,
    visibleChildren: visibleChildren,
    hiddenChildrenIds: hiddenChildrenIds,
    isExpanded: isExpanded,
    isSelected: isSelected,
  }
}

export function findGraphNode(nodeId: string, state: State): VisibleGraphNode {
  const graphNode = state.allNodes.find(node => node.id == nodeId)
  if (!graphNode) {
    throw new Error(`Node with id ${nodeId} not found`)
  }
  return toVisibleGraphNode(graphNode, state)
}

export function reduce(state: State, action: Action): State {
  switch (true) {
    case action instanceof InitializeState:
      return state.copy({
        allNodes: action.rootNodes.flatMap(expand)
      })
    case action instanceof ExpandNode:
      return state.copy({
        expandedNodeIds: [...state.expandedNodeIds, action.nodeId]
      })
    case action instanceof CollapseNode:
      return state.copy({
        expandedNodeIds: state.expandedNodeIds.filter(id => !isDescendantOf([action.nodeId])(id))
      })
    case action instanceof ChangeFilter:
      return state.copy({
        selectedFilter: action.edgeFilter
      })
    case action instanceof ShowAllEdgesOfNode:
      return state.copy({
        hoveredNodeId: action.nodeId
      })
    case action instanceof HideAllEdgesOfNode:
      return state.copy({
        hoveredNodeId: ''
      })
    case action instanceof ToggleEdgeLabels:
      return state.copy({
        showLabels: !state.showLabels
      })
    case action instanceof HideNode: {
      const node = findGraphNode(action.nodeId, state)
      const hiddenChildrenIdsByParentId = state.hiddenChildrenIdsByParentId
      if (node.parent) {
        const previousHiddenChildren = hiddenChildrenIdsByParentId.get(node.parent.id) || []
        hiddenChildrenIdsByParentId.set(node.parent.id, [...previousHiddenChildren, node.id])
      }
      return state.copy({
        hiddenNodeIds: [...state.hiddenNodeIds, node.id],
        hiddenChildrenIdsByParentId: hiddenChildrenIdsByParentId,
        pinnedNodeIds: state.pinnedNodeIds.filter(id => id !== action.nodeId)
      })
    }
    case action instanceof PinNode: {
      const descendants = [...getDescendants(findGraphNode(action.nodeId, state))]
      const unpinnedDescendants = descendants.filter(node => !state.pinnedNodeIds.includes(node.id))
      return state.copy({
        pinnedNodeIds: [...state.pinnedNodeIds, ...(unpinnedDescendants.map(node => node.id))],
        selectedPinnedNodeIds: [...state.selectedPinnedNodeIds, action.nodeId]
      })
    }
    case action instanceof UnpinNode: {
      const newSelectedPinnedNodeIds = state.selectedPinnedNodeIds.filter(id => id !== action.nodeId)
      return state.copy({
        pinnedNodeIds: state.pinnedNodeIds.filter(isDescendantOf(newSelectedPinnedNodeIds)),
        selectedPinnedNodeIds: newSelectedPinnedNodeIds
      })
    }
    case action instanceof RestoreNodes:
      return state.copy({
        hiddenNodeIds: [],
        pinnedNodeIds: [],
        selectedPinnedNodeIds: [],
        hiddenChildrenIdsByParentId: new Map()
      })
    case action instanceof RestoreNode: {
      const updatedMap = new Map(state.hiddenChildrenIdsByParentId)
      const newChildrenIds = (updatedMap.get(action.parentNodeId) || [])
        .filter(id => id !== action.nodeIdToBeRestored)
      updatedMap.set(action.parentNodeId, newChildrenIds)
      return state.copy({
        hiddenNodeIds: state.hiddenNodeIds.filter(id => id !== action.nodeIdToBeRestored),
        hiddenChildrenIdsByParentId: updatedMap
      })
    }
    case action instanceof RestoreAllChildren: {
      const updatedMap = new Map(state.hiddenChildrenIdsByParentId)
      const hiddenChildrenIds = state.hiddenChildrenIdsByParentId.get(action.nodeId) || []
      updatedMap.set(action.nodeId, [])
      return state.copy({
        hiddenNodeIds: state.hiddenNodeIds.filter(id => !hiddenChildrenIds.includes(id)),
        hiddenChildrenIdsByParentId: updatedMap
      })
    }
    case action instanceof ToggleInteractionMode:
      return state.copy({
        isInteractive: !state.isInteractive
      })
    case action instanceof ToggleUsageTypeMode:
      return state.copy({
        isUsageShown: !state.isUsageShown
      })
    case action instanceof EnterMultiselectMode:
      // This event is emitted multiple times while the shift key is pressed.
      return state.copy({
        multiselectMode: true
      })
    case action instanceof LeaveMultiselectMode:
      return state.copy({
        multiselectMode: false,
        selectedNodeIds: []
      })
    case action instanceof ToggleNodeSelection:
      return state.copy({
        selectedNodeIds: state.selectedNodeIds.includes(action.nodeId)
          ? state.selectedNodeIds.filter(id => id !== action.nodeId)
          : [...state.selectedNodeIds, action.nodeId]
      })
    default:
      return state
  }
}

// TODO move to an appropriate utility collection
const isDescendantOf = (ancestorNodeIds: string[]) => (descendantNodeId: string) =>
  ancestorNodeIds
    .filter(id => descendantNodeId.startsWith(id))
    .length > 0
