import {recursiveFind, VisibleGraphNode} from '../../../../model/GraphNode';
import {EdgeCollection, ElementDefinition, NodeCollection} from 'cytoscape';
import {Edge} from '../../../../model/Edge';
import {convertTypeOfUsage} from './UsageTypeConverter';

export function toCytoscapeNodes(graphNodes: VisibleGraphNode[]): ElementDefinition[] {
  return graphNodes.map(node => toCytoscapeNode(node))
}

export function toCytoscapeEdges(graphEdges: Edge[], showLabels: boolean, usageTypeMode: boolean): ElementDefinition[] {
  return graphEdges.map(edge => toCytoscapeEdge(edge, showLabels, usageTypeMode))
}

export function toVisibleGraphNodes(nodeCollection: NodeCollection): VisibleGraphNode[] {
  const rawRootNodes = nodeCollection.filter(node => !node.data().parent)
  return rawRootNodes.map(node => node.data().visibleGraphNode)
}

export function toGraphEdges(cyEdges: EdgeCollection, graphNodes: VisibleGraphNode[]): Edge[] {
  return cyEdges.map(edge => {
    const target = recursiveFind(graphNodes, edge.data().target)
    const source = recursiveFind(graphNodes, edge.data().source)

    if (!target || !source) {
      return null
    }

    return new Edge(
      source, // source
      target, // target
      edge.data().id, // id
      edge.data().weight, // weight
      edge.data().isCyclic, // isCyclic
      edge.data().type, // type
      edge.data().isPointingUpwards // isPointingUpwards
    );
  }).filter(edge => edge !== null)
}

function toCytoscapeNode(visibleGraphNode: VisibleGraphNode): ElementDefinition {
  const result: ElementDefinition = {
    data: {
      id: visibleGraphNode.id,
      label: visibleGraphNode.label,
      level: visibleGraphNode.level,
      containedDependencies: visibleGraphNode.dependencies,
      visibleGraphNode: visibleGraphNode
    },
    classes: visibleGraphNode.isExpanded ? 'compound' : 'non-compound'
  }
  if (visibleGraphNode.parent) {
    result.data.parent = visibleGraphNode.parent.id
  }
  return result
}

function toCytoscapeEdge(graphEdge: Edge, showLabels: boolean, usageTypeMode: boolean): ElementDefinition {
  const elementDefinition : ElementDefinition = {
    data: {
      id: graphEdge.id,
      source: graphEdge.source.id,
      target: graphEdge.target.id,
      weight: graphEdge.weight,
      isCyclic: graphEdge.isCyclic,
      type: graphEdge.type,
      isPointingUpwards: graphEdge.isPointingUpwards
    }
  }

  if (showLabels && graphEdge.weight > 1) {
    elementDefinition.style = {
      label : graphEdge.weight,
      'font-size':  20
    }
  } else if (showLabels && usageTypeMode && graphEdge.type && graphEdge.type !== 'usage') {
    elementDefinition.style = {
      label : `${convertTypeOfUsage(graphEdge.type)}\n‎ `,
      'text-rotation': 'autorotate',
      'text-wrap': 'wrap',
      'font-size':  12
    }
  }

  return elementDefinition
}
