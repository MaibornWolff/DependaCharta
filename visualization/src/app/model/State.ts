import {expand, getDescendants, GraphNode, GraphNodeUtils, VisibleGraphNode, VisibleGraphNodeUtils} from "./GraphNode";
import {EdgeFilter, EdgeFilterType} from "./EdgeFilter";
import {Action} from './Action';
import {Edge} from "./Edge";
import {DataClass} from "../common/DataClass";
import { IdUtils } from "./Id";
import {Coordinates} from "./lsmLayouting";

// TODO avoid Maps (→ (de-)serialization issues)
export class State extends DataClass<State> {
  declare readonly allNodes: GraphNode[]
  declare readonly hiddenNodeIds: string[]
  declare readonly hiddenChildrenIdsByParentId: Map<string, string[]>
  declare readonly expandedNodeIds: string[]
  declare readonly hoveredNodeId: string
  declare readonly selectedNodeIds: string[]
  declare readonly pinnedNodeIds: string[]
  declare readonly selectedPinnedNodeIds: string[]
  declare readonly showLabels: boolean
  declare readonly selectedFilter: EdgeFilterType
  declare readonly isInteractive: boolean
  declare readonly isUsageShown: boolean
  declare readonly multiselectMode: boolean
  declare readonly manuallyPositionedNodes: Map<string, Coordinates>

  static build(overrides: Partial<State> = {}) {
    const defaults = State.make({
      allNodes: [],
      hiddenNodeIds: [],
      hiddenChildrenIdsByParentId: new Map<string, string[]>(),
      expandedNodeIds: [],
      hoveredNodeId: '',
      selectedNodeIds: [],
      pinnedNodeIds: [],
      selectedPinnedNodeIds: [],
      showLabels: true,
      selectedFilter: EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES,
      isInteractive: true,
      isUsageShown: true,
      multiselectMode: false,
      manuallyPositionedNodes: new Map<string, Coordinates>()
    })

    return defaults.copy(overrides)
  }

  static fromRootNodes(rootNodes: GraphNode[]) {
    return State.build({
      allNodes: rootNodes.flatMap(expand)
    })
  }

  reduce(action: Action): State {
    switch (true) {
      case action instanceof Action.InitializeState:
        return this.copy({
          allNodes: action.rootNodes.flatMap(expand)
        })
      case action instanceof Action.ExpandNode:
        return this.copy({
          expandedNodeIds: [...this.expandedNodeIds, action.nodeId]
        })
      case action instanceof Action.CollapseNode:
        return this.copy({
          expandedNodeIds: this.expandedNodeIds.filter(id => !IdUtils.isDescendantOf([action.nodeId])(id))
        })
      case action instanceof Action.ChangeFilter:
        return this.copy({
          selectedFilter: action.edgeFilter
        })
      case action instanceof Action.ShowAllEdgesOfNode:
        return this.copy({
          hoveredNodeId: action.nodeId
        })
      case action instanceof Action.HideAllEdgesOfNode:
        return this.copy({
          hoveredNodeId: ''
        })
      case action instanceof Action.ToggleEdgeLabels:
        return this.copy({
          showLabels: !this.showLabels
        })
      case action instanceof Action.HideNode: {
        const node = this.findGraphNode(action.nodeId)
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
      case action instanceof Action.PinNode: {
        const descendants = [...getDescendants(this.findGraphNode(action.nodeId))]
        const unpinnedDescendants = descendants.filter(node => !this.pinnedNodeIds.includes(node.id))
        return this.copy({
          pinnedNodeIds: [...this.pinnedNodeIds, ...(unpinnedDescendants.map(node => node.id))],
          selectedPinnedNodeIds: [...this.selectedPinnedNodeIds, action.nodeId]
        })
      }
      case action instanceof Action.UnpinNode: {
        const newSelectedPinnedNodeIds = this.selectedPinnedNodeIds.filter(id => id !== action.nodeId)
        return this.copy({
          pinnedNodeIds: this.pinnedNodeIds.filter(IdUtils.isDescendantOf(newSelectedPinnedNodeIds)),
          selectedPinnedNodeIds: newSelectedPinnedNodeIds
        })
      }
      case action instanceof Action.RestoreNodes:
        return this.copy({
          hiddenNodeIds: [],
          pinnedNodeIds: [],
          selectedPinnedNodeIds: [],
          hiddenChildrenIdsByParentId: new Map()
        })
      case action instanceof Action.RestoreNode: {
        const updatedMap = new Map(this.hiddenChildrenIdsByParentId)
        const newChildrenIds = (updatedMap.get(action.parentNodeId) || [])
          .filter(id => id !== action.nodeIdToBeRestored)
        updatedMap.set(action.parentNodeId, newChildrenIds)
        return this.copy({
          hiddenNodeIds: this.hiddenNodeIds.filter(id => id !== action.nodeIdToBeRestored),
          hiddenChildrenIdsByParentId: updatedMap
        })
      }
      case action instanceof Action.RestoreAllChildren: {
        const updatedMap = new Map(this.hiddenChildrenIdsByParentId)
        const hiddenChildrenIds = this.hiddenChildrenIdsByParentId.get(action.nodeId) || []
        updatedMap.set(action.nodeId, [])
        return this.copy({
          hiddenNodeIds: this.hiddenNodeIds.filter(id => !hiddenChildrenIds.includes(id)),
          hiddenChildrenIdsByParentId: updatedMap
        })
      }
      case action instanceof Action.ToggleInteractionMode:
        return this.copy({
          isInteractive: !this.isInteractive
        })
      case action instanceof Action.ToggleUsageTypeMode:
        return this.copy({
          isUsageShown: !this.isUsageShown
        })
      case action instanceof Action.EnterMultiselectMode:
        // This event is emitted multiple times while the shift key is pressed.
        return this.copy({
          multiselectMode: true
        })
      case action instanceof Action.LeaveMultiselectMode:
        return this.copy({
          multiselectMode: false
        })
      case action instanceof Action.ResetMultiselection:
        return this.copy({
          selectedNodeIds: []
        })
      case action instanceof Action.ToggleNodeSelection:
        return this.copy({
          selectedNodeIds: this.selectedNodeIds.includes(action.nodeId)
            ? this.selectedNodeIds.filter(id => id !== action.nodeId)
            : [...this.selectedNodeIds, action.nodeId]
        })
      case action instanceof Action.SetNodeManualPosition: {
        const updatedMap = new Map(this.manuallyPositionedNodes)
        updatedMap.set(action.nodeId, action.position)
        return this.copy({
          manuallyPositionedNodes: updatedMap
        })
      }
      case action instanceof Action.ClearNodeManualPosition: {
        const updatedMap = new Map(this.manuallyPositionedNodes)
        updatedMap.delete(action.nodeId)
        return this.copy({
          manuallyPositionedNodes: updatedMap
        })
      }
      case action instanceof Action.ClearAllManualPositions:
        return this.copy({
          manuallyPositionedNodes: new Map()
        })
      case action instanceof Action.ResetView:
        return this.copy({
          manuallyPositionedNodes: new Map()
        })
      default:
        action satisfies never
        return this
    }
  }

  getVisibleNodes(): VisibleGraphNode[] {
    const expandedNodes = this.allNodes.filter(node => this.expandedNodeIds.includes(node.id))
    return this.allNodes
      .filter(node => !node.parent || expandedNodes.includes(node.parent))
      .filter(node => !GraphNodeUtils.isNodeOrAncestorHidden(this.hiddenNodeIds, node))
      .map(node => this.toVisibleGraphNode(node))
  }

  findGraphNode(nodeId: string): VisibleGraphNode {
    const graphNode = this.allNodes.find(node => node.id == nodeId)
    if (!graphNode) {
      throw new Error(`Node with id ${nodeId} not found`)
    }
    return this.toVisibleGraphNode(graphNode)    
  }

  createEdges(nodes: VisibleGraphNode[]): Edge[] {
    const visibleNodes = this.getVisibleNodes()
    const edges: Edge[] = nodes
      .filter(node => node.visibleChildren.length === 0) // Only render edges on unexpanded/leaf nodes
      .flatMap(node => {
        return VisibleGraphNodeUtils.createEdgesForNode(node, visibleNodes, this.hiddenNodeIds)
      })
    return Edge.aggregateEdges(edges, EdgeFilter.isFilterForcesEdgesAggregation(this.selectedFilter))
  }

  private toVisibleGraphNode(graphNode: GraphNode): VisibleGraphNode {
    const hiddenChildrenIds = this.hiddenChildrenIdsByParentId.get(graphNode.id) || []
    const isExpanded = this.expandedNodeIds.includes(graphNode.id)
    const isSelected = this.selectedNodeIds.includes(graphNode.id)
    const visibleChildren = isExpanded ?
      graphNode.children
        .map(child => this.toVisibleGraphNode(child))
        .filter(child => !GraphNodeUtils.isNodeOrAncestorHidden(hiddenChildrenIds, child))
      : [];
    return {
      ...graphNode,
      visibleChildren: visibleChildren,
      hiddenChildrenIds: hiddenChildrenIds,
      isExpanded: isExpanded,
      isSelected: isSelected,
    }
  }
}
