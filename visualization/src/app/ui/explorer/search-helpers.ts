import {GraphNode, GraphNodeUtils} from '../../model/GraphNode';

export function collectAllNodes(roots: GraphNode[], hiddenNodeIds: string[]): GraphNode[] {
  const result: GraphNode[] = [];

  function visit(node: GraphNode): void {
    if (GraphNodeUtils.isNodeOrAncestorHidden(hiddenNodeIds, node)) {
      return;
    }
    result.push(node);
    for (const child of node.children) {
      visit(child);
    }
  }

  for (const root of roots) {
    visit(root);
  }

  return result;
}

const LEAF_SUFFIX = ':leaf';

export function stripLeafSuffix(id: string): string {
  return id.endsWith(LEAF_SUFFIX) ? id.slice(0, -LEAF_SUFFIX.length) : id;
}

export function deduplicateByParent(ids: string[]): string[] {
  const idSet = new Set(ids);
  return ids.filter(id => {
    const parts = id.split('.');
    for (let i = 1; i < parts.length; i++) {
      const ancestor = parts.slice(0, i).join('.');
      if (idSet.has(ancestor)) {
        return false;
      }
    }
    return true;
  });
}
