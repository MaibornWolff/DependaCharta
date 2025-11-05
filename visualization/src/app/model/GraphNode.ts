import { Edge, ShallowEdge } from './Edge';
import { IdUtils } from './Id';

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
    .flatMap(child => expand(child))
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

export class GraphNodeUtils {
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

export class VisibleGraphNodeUtils {
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
