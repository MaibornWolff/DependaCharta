import {GraphNode, VisibleGraphNode} from '../../../../model/GraphNode';
import {Coordinates, CytoscapeValues} from '../../../../model/lsmLayouting';

export function toAbsoluteCoordinates(positionByNodeId: Map<string, Coordinates>, visibleNodes: VisibleGraphNode[]): Map<string, Coordinates> {
  const result = new Map<string, Coordinates>()
  const flattenedGraph = flattenGraph(visibleNodes)
  const parentNodes = flattenedGraph.filter(node => node.isParent).map(node => node.node.id)
  flattenedGraph.forEach(node => {
    const absoluteCoordinates = calculateAbsoluteCoordinates(node.node, parentNodes, positionByNodeId)
    result.set(node.node.id, absoluteCoordinates)
  })
  return result
}

function flattenGraph(visibleNodes: VisibleGraphNode[]): {node: VisibleGraphNode, isParent: boolean}[] {
  return Array.from(new Set(visibleNodes.flatMap(node => {
    if (!node.isExpanded || node.visibleChildren.length === 0) {
      return [{node: node, isParent: false}]
    } else {
      return [...flattenGraph(node.visibleChildren), {node: node, isParent: true}]
    }
  })))
}

function calculateAbsoluteCoordinates(visibleNode: GraphNode, parentNodes: string[], positionByNodeId: Map<string, Coordinates>): Coordinates {
  const nodePosition = positionByNodeId.get(visibleNode.id) || { x: 0, y: 0 }
  if (!visibleNode.parent) {
    return {
      x: -nodePosition.x,
      y: -nodePosition.y
    }
  }
  return {
    x: calculateAbsoluteCoordinates(visibleNode.parent, parentNodes, positionByNodeId).x - CytoscapeValues.PADDING_OF_COMPOUND_NODE - nodePosition.x ,
    y: calculateAbsoluteCoordinates(visibleNode.parent, parentNodes, positionByNodeId).y - nodePosition.y
  }
}
