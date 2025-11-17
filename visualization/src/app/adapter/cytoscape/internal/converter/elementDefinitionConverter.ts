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
      edge.data().type // type
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
      type: graphEdge.type
    },
    classes: []
  }

  // Add classes to control label visibility via stylesheet
  if (showLabels) {
    elementDefinition.classes = 'show-labels'

    if (usageTypeMode && graphEdge.type && graphEdge.type !== 'usage') {
      elementDefinition.classes = 'show-labels show-type'
      // Add typeLabel to data for stylesheet to use
      elementDefinition.data['typeLabel'] = `${convertTypeOfUsage(graphEdge.type)}\n‎ `
    }
  }

  return elementDefinition
}
