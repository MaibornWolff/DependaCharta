import {Edge} from '../../../../model/Edge';
import cytoscape, {ElementDefinition} from 'cytoscape';
import {buildUniqueId} from '../../../../common/test/TestUtils.spec';
import {VisibleGraphNode} from '../../../../model/GraphNode.spec';
import {ShallowGraphEdge} from '../../../../model/ShallowGraphEdge.spec';

export class ElementDefinitionBuilder {
  private elementId = buildUniqueId()
  private id = this.elementId
  private label = this.elementId
  private parent = ''
  private level = 0
  private containedDependencies: ShallowGraphEdge[] = []

  setContainedDependencies(dependencies: ShallowGraphEdge[]) {
    this.containedDependencies = dependencies
    return this
  }

  setId(id: string) {
    this.id = id
    return this
  }

  setLevel(level: number) {
    this.level = level
    return this
  }

  setParent(parent: string) {
    this.parent = parent
    return this;
  }

  build() {
    return {
      data: {
        id: this.id,
        label: this.label,
        parent: this.parent,
        level: this.level,
        containedDependencies: this.containedDependencies,
        visibleGraphNode: VisibleGraphNode.build({
          id: this.id,
          level: this.level
        })
      }
    }
  }
}

export class EdgeCollectionBuilder {
  private nodes: ElementDefinition[] = []
  private edges: ElementDefinition[] = []

  addGraphEdge(graphEdge: Edge) {
    this.nodes.push(
      createNodeDefinition(graphEdge.source),
      createNodeDefinition(graphEdge.target)
    )

    this.edges.push({
      data: {
        id: graphEdge.id,
        source: graphEdge.source.id,
        target: graphEdge.target.id,
        weight: graphEdge.weight,
        isCyclic: graphEdge.isCyclic,
        type: graphEdge.type
      }
    })
    return this
  }

  build() {
    const cy = cytoscape()
    cy.add(this.nodes)
    cy.add(this.edges)

    return cy.edges()
  }
}

/**
 * If you want to specify a parent for a node, you have to add that parent node to this builder before building aswell,
 * otherwise cytoscape deletes the parent attribute
 */
export class NodeCollectionBuilder {
  private nodes: ElementDefinition[] = []

  addElementDefinition(elementDefintion: ElementDefinition) {
    this.nodes.push(elementDefintion)
    return this
  }

  addGraphNode(graphNode: VisibleGraphNode) {
    this.nodes.push(createNodeDefinition(graphNode))
    return this
  }

  build() {
    const cy = cytoscape()
    cy.add(this.nodes)
    return cy.nodes()
  }
}

export class CytoscapeGraphBuilder {
  private nodes: ElementDefinition[] = []
  private edges: ElementDefinition[] = []

  newDefaultEdge(source: string, target: string) {
    const targetNode = new ElementDefinitionBuilder()
      .setId(target)
      .build();
    const dependency = ShallowGraphEdge.build({
      source: source,
      target: target
    })

    const sourceNode = new ElementDefinitionBuilder()
      .setId(source)
      .setContainedDependencies([dependency])
      .build();

    return this.newEdge(sourceNode, targetNode)
   }

  newEdge(source: ElementDefinition, target: ElementDefinition) {
    this.nodes.push(source)
    this.nodes.push(target)
    const containedDependency = source.data["containedDependencies"][0];
    this.edges.push({
      data: {
        id: source.data.id + "-" + target.data.id,
        source: source.data.id,
        target: target.data.id,
        weight: containedDependency.weight,
        isCyclic: containedDependency.isCyclic
      }
    })

    return this
  }

  newNode(name: string) {
    const node = new ElementDefinitionBuilder()
      .setId(name)
      .build()

    this.nodes.push(node)
    return this
  }

  build() {
    // If tests start to run slow, one can change this flag to false/emit it in some tests to speed them up
    const cy = cytoscape({styleEnabled: true})
    cy.add(this.nodes)
    cy.add(this.edges)

    return cy
  }
}

function createNodeDefinition(graphNode: VisibleGraphNode) {
  return {
    data: {
      id: graphNode.id,
      label: graphNode.label,
      parent: graphNode.parent?.id || '',
      level: graphNode.level,
      containedDependencies: graphNode.dependencies,
      visibleGraphNode: graphNode
    }
  };
}
