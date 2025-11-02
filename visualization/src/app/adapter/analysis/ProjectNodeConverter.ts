import {EdgeMetaInformation, getNodeId, ProjectNode} from './internal/ProjectReport';
import {GraphNode} from '../../model/GraphNode';
import {ShallowEdge} from '../../model/ShallowEdge';
import {LeafIdCreator} from './internal/LeafIdCreator';

export function convertToGraphNodes(json: any): GraphNode[] {
  const rootNodes = json.projectTreeRoots || []
  return convertToGraphNodesWithLightEdges(rootNodes)
}

export function convertToGraphNodesWithLightEdges(nodes: ProjectNode[], parentNode: GraphNode | null = null): GraphNode[] {
  return nodes.map(node => convertToGraphNodeWithoutEdge(node, parentNode))
}

function convertToGraphNodeWithoutEdge(node: ProjectNode, parentNode: GraphNode | null): GraphNode {
  const isRootNode = !parentNode;
  const id = getNodeId(node, parentNode?.id ? parentNode.id : "")
  const graphNode: GraphNode = {
    id: id,
    children: [],
    label: node.name,
    level: node.level,
    dependencies: toShallowEdge(id, node.containedInternalDependencies)
  }

  if (!isRootNode) {
    graphNode.parent = parentNode
  }
  if (node.children.length > 0) {
    graphNode.children = convertToGraphNodesWithLightEdges(node.children, graphNode)
  }
  return graphNode
}

function toShallowEdge(nodeId: string, rawDependencies: Record<string, EdgeMetaInformation>): ShallowEdge[] {
  const edges: ShallowEdge[] = []
  for (const targetNodeId in rawDependencies) {
    const edgeMetaInformation = rawDependencies[targetNodeId]
    const leafId = LeafIdCreator.createFrom(targetNodeId);
    edges.push(ShallowEdge.new({
      id: nodeId + "-" + leafId,
      source: nodeId,
      target: leafId,
      isCyclic: edgeMetaInformation.isCyclic,
      weight: edgeMetaInformation.weight,
      type: edgeMetaInformation.type
    }))
  }
  return edges
}
