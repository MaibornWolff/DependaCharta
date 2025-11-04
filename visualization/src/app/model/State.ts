import {expand, getDescendants, GraphNode, VisibleGraphNode} from "./GraphNode";
import {EdgeFilter, EdgeFilterType} from "./EdgeFilter";
import {Action, InitializeState, ExpandNode, CollapseNode, ChangeFilter, ShowAllEdgesOfNode, HideAllEdgesOfNode, ToggleEdgeLabels, HideNode, RestoreNode, RestoreNodes, RestoreAllChildren, ToggleInteractionMode, ToggleUsageTypeMode, ResetView, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, PinNode, UnpinNode} from './Action';
import {Edge} from "./Edge";
import {DataClass} from "../common/DataClass";

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
      multiselectMode: false
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
          expandedNodeIds: this.expandedNodeIds.filter(id => !IdUtils.isDescendantOf([action.nodeId])(id))
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
      case action instanceof PinNode: {
        const descendants = [...getDescendants(this.findGraphNode(action.nodeId))]
        const unpinnedDescendants = descendants.filter(node => !this.pinnedNodeIds.includes(node.id))
        return this.copy({
          pinnedNodeIds: [...this.pinnedNodeIds, ...(unpinnedDescendants.map(node => node.id))],
          selectedPinnedNodeIds: [...this.selectedPinnedNodeIds, action.nodeId]
        })
      }
      case action instanceof UnpinNode: {
        const newSelectedPinnedNodeIds = this.selectedPinnedNodeIds.filter(id => id !== action.nodeId)
        return this.copy({
          pinnedNodeIds: this.pinnedNodeIds.filter(IdUtils.isDescendantOf(newSelectedPinnedNodeIds)),
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

// TODO move
class VisibleGraphNodeUtils {
  static findBestDependencyTarget(dependencyId: string, visibleNodes: VisibleGraphNode[], hiddenNodeIds: string[]): VisibleGraphNode | null {
    if (hiddenNodeIds.includes(dependencyId)) {
      return null
    }

    const visibleNode = visibleNodes.find(visibleNode => dependencyId === visibleNode.id);
    if (visibleNode) {
      return visibleNode
    }

    const dependencyParent = IdUtils.getParent(dependencyId)
    if (!dependencyParent) {
      return null
    }
    return VisibleGraphNodeUtils.findBestDependencyTarget(dependencyParent, visibleNodes, hiddenNodeIds)
  }

  static createEdgesForNode(node: VisibleGraphNode, visibleNodes: VisibleGraphNode[], hiddenNodeIds: string[]): Edge[] {
    return node.dependencies.flatMap(dependency => {
      const bestTarget = VisibleGraphNodeUtils.findBestDependencyTarget(dependency.target, visibleNodes, hiddenNodeIds)
      if (bestTarget && !IdUtils.isIncludedIn(bestTarget.id, node.id)) {
        return new Edge(
          node, // source
          bestTarget, // target
          node.id + "-" + bestTarget.id, // id
          dependency.weight, // weight
          dependency.isCyclic, // isCyclic
          dependency.type // type
        )
      }
      return []
    })
  }
}

// TODO move
class IdUtils {
  static getParent(nodeId: string): string | null {
    const parent = nodeId.substring(0, nodeId.lastIndexOf('.'))
    if (parent.length === 0) {
      return null
    }
    return parent
  }

  static isIncludedIn(includingId: string, id: string) {
    if (includingId === id) {
      return true
    }
    const includingIdParts = includingId.split(".")
    const idParts = id.split(".")
    if (includingIdParts.length >= idParts.length) {
      return false
    }
    for (let i = 0; i < includingIdParts.length; i++) {
      if (includingIdParts[i] !== idParts[i]) {
        return false
      }
    }
    return true
  }

  static isDescendantOf = (ancestorNodeIds: string[]) => (descendantNodeId: string) =>
    ancestorNodeIds
      .filter(id => descendantNodeId.startsWith(id))
      .length > 0

}

// TODO move
class GraphNodeUtils {
  static isNodeOrAncestorHidden(hiddenChildrenIds: string[], child: GraphNode): boolean {
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
}

