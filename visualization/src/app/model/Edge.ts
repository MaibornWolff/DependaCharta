import {getAncestors, GraphNode, VisibleGraphNode} from './GraphNode';
import {EdgeFilterType} from './EdgeFilter';
import {State} from './State';
import {ValueObject} from '../common/ValueObject';

export class Edge extends ValueObject<Edge> {
  declare readonly source: VisibleGraphNode
  declare readonly target: VisibleGraphNode
  declare readonly id: string
  declare readonly weight: number
  declare readonly isCyclic: boolean
  declare readonly type: string
}

// TODO `Edge` should have a property `isPointingUpwards: boolean`
// It should be set when when Edge is created in `toGraphEdges`
// TODO `Edge` should have a function `getType(): EdgeType`
// !isCyclic && !isPointingUpwards => REGULAR
// isCyclic && !isPointingUpwards => CYCLIC
// !isCyclic && isPointingUpwards => TWISTED
// isCyclic && isPointingUpwards => FEEDBACK
// TODO (next) `EdgePredicate`, `EdgeFilter`, `EdgeFilterResult` can be removed
export function isPointingUpwards(edge: Edge): boolean {
  const [sourceNode, targetNode] = findSiblingsUnderLowestCommonAncestor(edge.source, edge.target)
  return sourceNode.level <= targetNode.level
}

/*
    +----+
    |    |           Level 1 Sibling
    +----+
   ↙️     ↖️↘️
+----+   +----+
|    |   |    |      Level 0 Siblings
+----+   +----+
*/
function findSiblingsUnderLowestCommonAncestor(source: GraphNode, target: GraphNode): [GraphNode, GraphNode] {
  for (const sourceAncestor of getAncestors(source)) {
    for (const targetAncestor of getAncestors(target)) {
      if (sourceAncestor.parent?.id === targetAncestor.parent?.id) {
        return [sourceAncestor, targetAncestor]
      }
    }
  }

  throw new Error("No common ancestor found")
}

export function createEdges(nodes: VisibleGraphNode[], state: State): Edge[] {
  const visibleNodes = state.getVisibleNodes()
  const edges: Edge[] = nodes
    .filter(node => node.visibleChildren.length === 0) // Only render edges on unexpanded/leaf nodes
    .flatMap(node => {
      return createEdgesForNode(node, visibleNodes, state.hiddenNodeIds)
    })
  return aggregateEdges(edges, isFilterForcesEdgesAggregation(state.selectedFilter))
}

function createEdgesForNode(node: VisibleGraphNode, visibleNodes: VisibleGraphNode[], hiddenNodeIds: string[]): Edge[] {
  return node.dependencies.flatMap(dependency => {
    const bestTarget = findBestDependencyTarget(dependency.target, visibleNodes, hiddenNodeIds)
    if (bestTarget && !isIncludedIn(bestTarget.id, node.id)) {
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

function isIncludedIn(includingId: string, id: string) {
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

function findBestDependencyTarget(dependencyId: string, visibleNodes: VisibleGraphNode[], hiddenNodeIds: string[]): VisibleGraphNode | null {
  if (hiddenNodeIds.includes(dependencyId)) {
    return null
  }

  const visibleNode = visibleNodes.find(visibleNode => dependencyId === visibleNode.id);
  if (visibleNode) {
    return visibleNode
  }

  const dependencyParent = getParent(dependencyId)
  if (!dependencyParent) {
    return null
  }
  return findBestDependencyTarget(dependencyParent, visibleNodes, hiddenNodeIds)
}

function getParent(nodeId: string): string | null {
  const parent = nodeId.substring(0, nodeId.lastIndexOf('.'))
  if (parent.length === 0) {
    return null
  }
  return parent
}

function isFilterForcesEdgesAggregation(edgeFilterType: EdgeFilterType): boolean {
  return edgeFilterType !== EdgeFilterType.CYCLES_ONLY && edgeFilterType !== EdgeFilterType.FEEDBACK_EDGES_ONLY
}

function aggregateEdges(edges: Edge[], shouldAggregateEdges: boolean): Edge[] {
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