import {expand, getDescendants, GraphNode, VisibleGraphNode} from "./GraphNode";
import {EdgeFilterType} from "./EdgeFilter";
import {Action, InitializeState, ExpandNode, CollapseNode, ChangeFilter, ShowAllEdgesOfNode, HideAllEdgesOfNode, ToggleEdgeLabels, HideNode, RestoreNode, RestoreNodes, RestoreAllChildren, ToggleInteractionMode, ToggleUsageTypeMode, ResetView, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, PinNode, UnpinNode} from './Action';
import {Edge} from "./Edge";

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
    return EdgeUtils.aggregateEdges(edges, EdgeFilterTypeUtils.isFilterForcesEdgesAggregation(this.selectedFilter))
  }

  private toVisibleGraphNode(graphNode: GraphNode): VisibleGraphNode {
    const hiddenChildrenIds = this.hiddenChildrenIdsByParentId.get(graphNode.id) || []
    const isExpanded = this.expandedNodeIds.includes(graphNode.id)
    const isSelected = this.selectedNodeIds.includes(graphNode.id)
    const visibleChildren = isExpanded ?
      graphNode.children
        .map(child => this.toVisibleGraphNode(child))
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
}

// TODO move
class EdgeFilterTypeUtils {
  static isFilterForcesEdgesAggregation(edgeFilterType: EdgeFilterType): boolean {
    return edgeFilterType !== EdgeFilterType.CYCLES_ONLY && edgeFilterType !== EdgeFilterType.FEEDBACK_EDGES_ONLY
  }
}

// TODO move
class EdgeUtils {
  static aggregateEdges(edges: Edge[], shouldAggregateEdges: boolean): Edge[] {
    const aggregatedEdges = new Map<string, Edge>()

    edges.forEach(edge => {
      const key = shouldAggregateEdges
        ? edge.id
        : `${(edge.id)}-${(edge.isCyclic)}`
      const duplicateEdge = aggregatedEdges.get(key)

      let aggregatedEdge: Edge
      if (duplicateEdge) {
        aggregatedEdge = duplicateEdge.copy({
          weight: duplicateEdge.weight + edge.weight,
          isCyclic: shouldAggregateEdges
            ? duplicateEdge.isCyclic || edge.isCyclic
            : edge.isCyclic,
        })
      } else {
        aggregatedEdge = edge.copy({id: key})
      }

      aggregatedEdges.set(key, aggregatedEdge)
    });

    return [...aggregatedEdges.values()];
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
        return new Edge({
          id: node.id + "-" + bestTarget.id,
          source: node,
          target: bestTarget,
          isCyclic: dependency.isCyclic,
          weight: dependency.weight,
          type: dependency.type
        })
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

// TODO move to an appropriate utility collection
const isDescendantOf = (ancestorNodeIds: string[]) => (descendantNodeId: string) =>
  ancestorNodeIds
    .filter(id => descendantNodeId.startsWith(id))
    .length > 0
