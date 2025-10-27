import {NodeCollection, NodeSingular} from 'cytoscape';

export function getSubtreeOf(node: NodeSingular): NodeCollection {
  if (node.children().length === 0) {
    return node
  }
  const successors = node.children().map(child => getSubtreeOf(child))
  const flattenedSuccessors = successors
    .reduce((previousValue, currentValue) => previousValue.union(currentValue))
  return flattenedSuccessors.union(node)
}

export function getAncestors(node: NodeSingular): NodeSingular[] {
  return Array.from(_getAncestors(node))
}

function* _getAncestors(node: NodeSingular): Generator<NodeSingular> {
  const parent: NodeCollection = node.parent()

  if (parent && parent.length > 0) {
    yield parent[0]
    yield* _getAncestors(parent[0])
  }
}
