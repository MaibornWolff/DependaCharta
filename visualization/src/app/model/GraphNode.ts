import {ShallowEdge} from './ShallowEdge';

export interface GraphNode {
  id: string
  label: string
  level: number
  parent?: GraphNode
  children: GraphNode[],
  dependencies: ShallowEdge[]
}

export type VisibleGraphNode = GraphNode & {
  visibleChildren: VisibleGraphNode[]
  hiddenChildrenIds: string[]
  isExpanded: boolean
  isSelected: boolean
}

export function getNodeDepth(node: GraphNode) {
  return node.id.split('.').length;
}

export function isPackage(node: GraphNode): boolean {
  return node.children.length > 0
}

export function expand(node: GraphNode): GraphNode[] {
  return node.children
    .flatMap(expand)
    .concat(node)
}

export function recursiveFind(graphNodes: VisibleGraphNode[], nodeId: string): VisibleGraphNode| null {
  const found = graphNodes.find(graphNode => graphNode.id === nodeId)
  if (found) {
    return found
  }

  for (const graphNode of graphNodes) {
    const foundChild = recursiveFind(graphNode.visibleChildren, nodeId)
    if (foundChild) {
      return foundChild
    }
  }

  return null
}

// Note that the `node` itself is included in the list of its ancestors
export function* getAncestors(node: GraphNode): Generator<GraphNode> {
  yield node

  if (node.parent) {
    yield* getAncestors(node.parent)
  }
}

// Note that the `node` itself is included in the list of its descendants
export function* getDescendants(node: GraphNode): Generator<GraphNode> {
  yield node

  if (node.children) {
    for (const child of node.children) {
      yield* getDescendants(child)
    }
  }
}

export function countParents(node: GraphNode): number {
  return Array.from(getAncestors(node)).length - 1
}
