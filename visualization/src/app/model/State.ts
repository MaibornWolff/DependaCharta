import {expand, getDescendants, GraphNode, VisibleGraphNode} from "./GraphNode";
import {EdgeFilterType} from "./EdgeFilter";
import {Action, InitializeState, ExpandNode, CollapseNode, ChangeFilter, ShowAllEdgesOfNode, HideAllEdgesOfNode, ToggleEdgeLabels, HideNode, RestoreNode, RestoreNodes, RestoreAllChildren, ToggleInteractionMode, ToggleUsageTypeMode, ResetView, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, PinNode, UnpinNode} from './Action';

// TODO avoid Maps (→ (de-)serialization issues)
export class State {
  public readonly allNodes: GraphNode[] = []
  public readonly hiddenNodeIds: string[] = []
  public readonly hiddenChildrenIdsByParentId: Map<string, string[]> = new Map<string, string[]>()
  public readonly expandedNodeIds: string[] = []
  public readonly hoveredNodeId: string = ''
  public readonly selectedNodeIds: string[] = []
  public readonly pinnedNodeIds: string[] = []
  public readonly selectedPinnedNodeIds: string[] = []
  public readonly showLabels: boolean = true
  public readonly selectedFilter: EdgeFilterType = EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES
  public readonly isInteractive: boolean = true
  public readonly isUsageShown: boolean = true
  public readonly multiselectMode: boolean = false

  constructor(from: Partial<State> = {}) {
    Object.assign(this, from)
  }

  copy(overrides: Partial<State> = {}): State {
    return new State(Object.assign({}, this, overrides))
  }

  reduce(action: Action): State {
    switch (true) {
      case action instanceof InitializeState:
        return this.copy({
          allNodes: action.rootNodes.flatMap(expand)
        })
      case action instanceof ExpandNode:
        return this.copy({
          expandedNodeIds: [...this.expandedNodeIds, action.nodeId]
        })
      case action instanceof CollapseNode:
        return this.copy({
          expandedNodeIds: this.expandedNodeIds.filter(id => !isDescendantOf([action.nodeId])(id))
        })
      case action instanceof ChangeFilter:
        return this.copy({
          selectedFilter: action.edgeFilter
        })
      case action instanceof ShowAllEdgesOfNode:
        return this.copy({
          hoveredNodeId: action.nodeId
        })
      case action instanceof HideAllEdgesOfNode:
        return this.copy({
          hoveredNodeId: ''
        })
      case action instanceof ToggleEdgeLabels:
        return this.copy({
          showLabels: !this.showLabels
        })
      case action instanceof HideNode: {
        const node = findGraphNode(action.nodeId, this)
        const hiddenChildrenIdsByParentId = this.hiddenChildrenIdsByParentId
        if (node.parent) {
          const previousHiddenChildren = hiddenChildrenIdsByParentId.get(node.parent.id) || []
          hiddenChildrenIdsByParentId.set(node.parent.id, [...previousHiddenChildren, node.id])
        }
        return this.copy({
          hiddenNodeIds: [...this.hiddenNodeIds, node.id],
          hiddenChildrenIdsByParentId: hiddenChildrenIdsByParentId,
          pinnedNodeIds: this.pinnedNodeIds.filter(id => id !== action.nodeId)
        })
      }
      case action instanceof PinNode: {
        const descendants = [...getDescendants(findGraphNode(action.nodeId, this))]
        const unpinnedDescendants = descendants.filter(node => !this.pinnedNodeIds.includes(node.id))
        return this.copy({
          pinnedNodeIds: [...this.pinnedNodeIds, ...(unpinnedDescendants.map(node => node.id))],
          selectedPinnedNodeIds: [...this.selectedPinnedNodeIds, action.nodeId]
        })
      }
      case action instanceof UnpinNode: {
        const newSelectedPinnedNodeIds = this.selectedPinnedNodeIds.filter(id => id !== action.nodeId)
        return this.copy({
          pinnedNodeIds: this.pinnedNodeIds.filter(isDescendantOf(newSelectedPinnedNodeIds)),
          selectedPinnedNodeIds: newSelectedPinnedNodeIds
        })
      }
      case action instanceof RestoreNodes:
        return this.copy({
          hiddenNodeIds: [],
          pinnedNodeIds: [],
          selectedPinnedNodeIds: [],
          hiddenChildrenIdsByParentId: new Map()
        })
      case action instanceof RestoreNode: {
        const updatedMap = new Map(this.hiddenChildrenIdsByParentId)
        const newChildrenIds = (updatedMap.get(action.parentNodeId) || [])
          .filter(id => id !== action.nodeIdToBeRestored)
        updatedMap.set(action.parentNodeId, newChildrenIds)
        return this.copy({
          hiddenNodeIds: this.hiddenNodeIds.filter(id => id !== action.nodeIdToBeRestored),
          hiddenChildrenIdsByParentId: updatedMap
        })
      }
      case action instanceof RestoreAllChildren: {
        const updatedMap = new Map(this.hiddenChildrenIdsByParentId)
        const hiddenChildrenIds = this.hiddenChildrenIdsByParentId.get(action.nodeId) || []
        updatedMap.set(action.nodeId, [])
        return this.copy({
          hiddenNodeIds: this.hiddenNodeIds.filter(id => !hiddenChildrenIds.includes(id)),
          hiddenChildrenIdsByParentId: updatedMap
        })
      }
      case action instanceof ToggleInteractionMode:
        return this.copy({
          isInteractive: !this.isInteractive
        })
      case action instanceof ToggleUsageTypeMode:
        return this.copy({
          isUsageShown: !this.isUsageShown
        })
      case action instanceof EnterMultiselectMode:
        // This event is emitted multiple times while the shift key is pressed.
        return this.copy({
          multiselectMode: true
        })
      case action instanceof LeaveMultiselectMode:
        return this.copy({
          multiselectMode: false,
          selectedNodeIds: []
        })
      case action instanceof ToggleNodeSelection:
        return this.copy({
          selectedNodeIds: this.selectedNodeIds.includes(action.nodeId)
            ? this.selectedNodeIds.filter(id => id !== action.nodeId)
            : [...this.selectedNodeIds, action.nodeId]
        })
      case action instanceof ResetView:
        return this
      default:
        action satisfies never
        return this
    }
  }

  getVisibleNodes(): VisibleGraphNode[] {
  const expandedNodes = this.allNodes.filter(node => this.expandedNodeIds.includes(node.id))
  return this.allNodes
    .filter(node => !node.parent || expandedNodes.includes(node.parent))
    .filter(node => !isNodeOrAncestorHidden(this.hiddenNodeIds, node))
    .map(node => toVisibleGraphNode(node, this))
}
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

// TODO move to an appropriate utility collection
const isDescendantOf = (ancestorNodeIds: string[]) => (descendantNodeId: string) =>
  ancestorNodeIds
    .filter(id => descendantNodeId.startsWith(id))
    .length > 0
