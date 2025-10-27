import {expand, getDescendants, GraphNode, VisibleGraphNode} from "./GraphNode";
import {EdgeFilterType} from "./EdgeFilter";
import {Action, InitializeState, ExpandNode, CollapseNode, ChangeFilter, ShowAllEdgesOfNode, HideAllEdgesOfNode, ToggleEdgeLabels, HideNode, RestoreNode, RestoreNodes, RestoreAllChildren, ToggleInteractionMode, ToggleUsageTypeMode, ResetView, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, PinNode, UnpinNode} from './Action';

// TODO avoid Maps (→ (de-)serialization issues)
export interface GraphState {
  allNodes: GraphNode[]

  hiddenNodeIds: string[]
  hiddenChildrenIdsByParentId: Map<string, string[]>
  expandedNodeIds: string[]
  hoveredNodeId: string
  selectedNodeIds: string[]
  pinnedNodeIds: string[]
  selectedPinnedNodeIds: string[]

  showLabels: boolean
  selectedFilter: EdgeFilterType
  isInteractive: boolean
  isUsageShown: boolean
  multiselectMode: boolean
}

export function getVisibleNodes(graphState: GraphState): VisibleGraphNode[] {
  const expandedNodes = graphState.allNodes.filter(node => graphState.expandedNodeIds.includes(node.id))
  return graphState.allNodes
    .filter(node => !node.parent || expandedNodes.includes(node.parent))
    .filter(node => !isNodeOrAncestorHidden(graphState.hiddenNodeIds, node))
    .map(node => toVisibleGraphNode(node, graphState))
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

function toVisibleGraphNode(graphNode: GraphNode, graphState: GraphState): VisibleGraphNode {
  const hiddenChildrenIds = graphState.hiddenChildrenIdsByParentId.get(graphNode.id) || []
  const isExpanded = graphState.expandedNodeIds.includes(graphNode.id)
  const isSelected = graphState.selectedNodeIds.includes(graphNode.id)
  const visibleChildren = isExpanded ?
    graphNode.children
      .map(child => toVisibleGraphNode(child, graphState))
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

export function findGraphNode(nodeId: string, graphState: GraphState): VisibleGraphNode {
  const graphNode = graphState.allNodes.find(node => node.id == nodeId)
  if (!graphNode) {
    throw new Error(`Node with id ${nodeId} not found`)
  }
  return toVisibleGraphNode(graphNode, graphState)
}

export namespace GraphState {
  export function build(overrides: Partial<GraphState> = {}): GraphState {
    const defaults: GraphState = {
      allNodes: [],
      hiddenNodeIds: [],
      hiddenChildrenIdsByParentId: new Map(),
      expandedNodeIds: [],
      hoveredNodeId: '',
      selectedNodeIds: [],
      pinnedNodeIds: [],
      selectedPinnedNodeIds: [],
      selectedFilter: EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES,
      showLabels: true,
      isInteractive: true,
      isUsageShown: true,
      multiselectMode: false
    }

    return {...defaults, ...overrides}
  }
}

export function reduce(state: GraphState, action: Action): GraphState {
  switch (true) {
    case action instanceof InitializeState:
      return {
        ...state,
        allNodes: action.rootNodes.flatMap(expand)
      }
    case action instanceof ExpandNode:
      return {
        ...state,
        expandedNodeIds: [...state.expandedNodeIds, action.nodeId]
      }
    case action instanceof CollapseNode:
      return {
        ...state,
        expandedNodeIds: state.expandedNodeIds.filter(id => !isDescendantOf([action.nodeId])(id))
      }
    case action instanceof ChangeFilter:
      return {
        ...state,
        selectedFilter: action.edgeFilter
      }
    case action instanceof ShowAllEdgesOfNode:
      return {
        ...state,
        hoveredNodeId: action.nodeId
      }
    case action instanceof HideAllEdgesOfNode:
      return {
        ...state,
        hoveredNodeId: ''
      }
    case action instanceof ToggleEdgeLabels:
      return {
        ...state,
        showLabels: !state.showLabels
      }
    case action instanceof HideNode: {
      const node = findGraphNode(action.nodeId, state)
      const hiddenChildrenIdsByParentId = state.hiddenChildrenIdsByParentId
      if (node.parent) {
        const previousHiddenChildren = hiddenChildrenIdsByParentId.get(node.parent.id) || []
        hiddenChildrenIdsByParentId.set(node.parent.id, [...previousHiddenChildren, node.id])
      }
      return {
        ...state,
        hiddenNodeIds: [...state.hiddenNodeIds, node.id],
        hiddenChildrenIdsByParentId: hiddenChildrenIdsByParentId,
        pinnedNodeIds: state.pinnedNodeIds.filter(id => id !== action.nodeId)
      }
    }
    case action instanceof PinNode: {
      const descendants = [...getDescendants(findGraphNode(action.nodeId, state))]
      const unpinnedDescendants = descendants.filter(node => !state.pinnedNodeIds.includes(node.id))
      return {
        ...state,
        pinnedNodeIds: [...state.pinnedNodeIds, ...(unpinnedDescendants.map(node => node.id))],
        selectedPinnedNodeIds: [...state.selectedPinnedNodeIds, action.nodeId]
      }
    }
    case action instanceof UnpinNode: {
      const newSelectedPinnedNodeIds = state.selectedPinnedNodeIds.filter(id => id !== action.nodeId)
      return {
        ...state,
        pinnedNodeIds: state.pinnedNodeIds.filter(isDescendantOf(newSelectedPinnedNodeIds)),
        selectedPinnedNodeIds: newSelectedPinnedNodeIds
      }
    }
    case action instanceof RestoreNodes:
      return {
        ...state,
        hiddenNodeIds: [],
        pinnedNodeIds: [],
        selectedPinnedNodeIds: [],
        hiddenChildrenIdsByParentId: new Map()
      }
    case action instanceof RestoreNode: {
      const updatedMap = new Map(state.hiddenChildrenIdsByParentId)
      const newChildrenIds = (updatedMap.get(action.parentNodeId) || [])
        .filter(id => id !== action.nodeIdToBeRestored)
      updatedMap.set(action.parentNodeId, newChildrenIds)
      return {
        ...state,
        hiddenNodeIds: state.hiddenNodeIds.filter(id => id !== action.nodeIdToBeRestored),
        hiddenChildrenIdsByParentId: updatedMap
      }
    }
    case action instanceof RestoreAllChildren: {
      const updatedMap = new Map(state.hiddenChildrenIdsByParentId)
      const hiddenChildrenIds = state.hiddenChildrenIdsByParentId.get(action.nodeId) || []
      updatedMap.set(action.nodeId, [])
      return {
        ...state,
        hiddenNodeIds: state.hiddenNodeIds.filter(id => !hiddenChildrenIds.includes(id)),
        hiddenChildrenIdsByParentId: updatedMap
      }
    }
    case action instanceof ToggleInteractionMode:
      return {
        ...state,
        isInteractive: !state.isInteractive
      }
    case action instanceof ToggleUsageTypeMode:
      return {
        ...state,
        isUsageShown: !state.isUsageShown
      }
    case action instanceof EnterMultiselectMode:
      // This event is emitted multiple times while the shift key is pressed.
      return {
        ...state,
        multiselectMode: true
      }
    case action instanceof LeaveMultiselectMode:
      return {
        ...state,
        multiselectMode: false,
        selectedNodeIds: []
      }
    case action instanceof ToggleNodeSelection:
      return {
        ...state,
        selectedNodeIds: state.selectedNodeIds.includes(action.nodeId)
          ? state.selectedNodeIds.filter(id => id !== action.nodeId)
          : [...state.selectedNodeIds, action.nodeId]
      }
    default:
      return state
  }
}

// TODO move to an appropriate utility collection
const isDescendantOf = (ancestorNodeIds: string[]) => (descendantNodeId: string) =>
  ancestorNodeIds
    .filter(id => descendantNodeId.startsWith(id))
    .length > 0
