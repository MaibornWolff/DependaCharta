import {buildGraphEdge, buildShallowGraphEdge, buildVisibleGraphNode} from '../../../../model/ModelBuilders.spec'
import {toCytoscapeEdges, toCytoscapeNodes, toGraphEdges, toVisibleGraphNodes} from './elementDefinitionConverter'
import {EdgeCollectionBuilder, NodeCollectionBuilder} from './CytoscapeModelBuilders.spec'
import {convertTypeOfUsage} from './UsageTypeConverter'
import {GraphNode} from '../../../../model/GraphNode.spec'

describe('ElementDefinitionConverter', () => {
  describe('GraphNode Conversion', () => {
    it('should convert GraphNode without children to ElementDefinition', () => {
      // given
      const id = "nodeId"
      const label = "some label"
      const parent = GraphNode.build()
      const dependency = buildShallowGraphEdge({
        id: id,
      })
      const graphNode = buildVisibleGraphNode({
        id: id,
        label: label,
        parent: parent,
        level: 1,
        dependencies: [dependency]
      })

      // when
      const elementDefinition = toCytoscapeNodes([graphNode])

      // then
      const expected = {
        data: {
          id: id,
          label: label,
          parent: parent.id,
          level: 1,
          containedDependencies: [dependency],
          visibleGraphNode: graphNode
        },
        classes: 'non-compound'
      }

      expect(elementDefinition.length).toEqual(1)
      expect(elementDefinition[0]).toEqual(expected)
    })


    it('should convert GraphNode with children to ElementDefinition', () => {
      // given
      const child = buildVisibleGraphNode()
      const graphNode = buildVisibleGraphNode({
        visibleChildren: [child]
      })

      // when
      const elementDefinition = toCytoscapeNodes([graphNode])

      // then
      const expected = {
        data: {
          id: graphNode.id,
          label: graphNode.label,
          level: graphNode.level,
          containedDependencies: [],
          visibleGraphNode: graphNode
        },
        classes: 'non-compound'
      }

      expect(elementDefinition.length).toEqual(1)
      expect(elementDefinition[0]).toEqual(expected)
    })

    it('should convert expanded GraphNode with children to Compound ElementDefinition', () => {
      // given
      const child = buildVisibleGraphNode()
      const graphNode = buildVisibleGraphNode({
        visibleChildren: [child],
        isExpanded: true
      })

      // when
      const elementDefinition = toCytoscapeNodes([graphNode])

      // then
      const expected = {
        data: {
          id: graphNode.id,
          label: graphNode.label,
          level: graphNode.level,
          containedDependencies: [],
          visibleGraphNode: graphNode
        },
        classes: 'compound'
      }

      expect(elementDefinition.length).toEqual(1)
      expect(elementDefinition[0]).toEqual(expected)
    })

    it('should convert selected GraphNode to valid ElementDefinition', () => {
      // given
      const graphNode = buildVisibleGraphNode({
        isSelected: true
      })

      // when
      const elementDefinition = toCytoscapeNodes([graphNode])

      // then
      const expected = {
        data: {
          id: graphNode.id,
          label: graphNode.label,
          level: graphNode.level,
          containedDependencies: [],
          visibleGraphNode: graphNode
        },
        classes: 'non-compound'
      }

      expect(elementDefinition.length).toEqual(1)
      expect(elementDefinition[0]).toEqual(expected)
    })

    it('Converts nodeCollection without children to graphNodes', () => {
      const expectedGraphNode = buildVisibleGraphNode()
      const nodeCollection = new NodeCollectionBuilder().addGraphNode(expectedGraphNode).build()
      const graphNodes = toVisibleGraphNodes(nodeCollection)
      expect(graphNodes[0]).toEqual(expectedGraphNode)
    })

    it('Converts nodeCollection with children to graphNodes', () => {
      const expectedParentNode = buildVisibleGraphNode()
      const child1 = buildVisibleGraphNode({
        parent: expectedParentNode
      })
      expectedParentNode.visibleChildren = [child1]
      const nodeCollection = new NodeCollectionBuilder()
        .addGraphNode(expectedParentNode)
        .addGraphNode(child1)
        .build()
      const graphNodes = toVisibleGraphNodes(nodeCollection)
      expect(graphNodes[0]).toEqual(expectedParentNode)
    })
  })

  describe('Edge Conversion', () => {
    it('Should convert GraphEdge to ElementDefiniton', () => {
      // given
      const source = buildVisibleGraphNode({
        id: 'source'
      })
      const target = buildVisibleGraphNode({
        id: 'target',
      })
      const graphEdge = buildGraphEdge({
        source: source,
        target: target,
        isCyclic: true,
        weight: 1,
        type: 'inheritance'
      })

      // when
      const edge = toCytoscapeEdges([graphEdge], true, true)[0]

      // then
      const expected = {
        data: {id: graphEdge.id, source: "source", target: "target", weight: 1, isCyclic: true, type: 'inheritance'},
        style: {
          label : `${convertTypeOfUsage(graphEdge.type)}\n‎ `,
          'text-rotation': 'autorotate',
          'text-wrap': 'wrap',
          'font-size':  12
        }
      }
      expect(edge).toEqual(expected)
    })

      it('Should hide label if usageType is false', () => {
        // given
        const source = buildVisibleGraphNode({
          id: 'source'
        })
        const target = buildVisibleGraphNode({
          id: 'target'
        })
        const graphEdge = buildGraphEdge({
          source: source,
          target: target,
          isCyclic: true,
          weight: 1,
          type: 'inheritance'
        })

        // when
        const edge = toCytoscapeEdges([graphEdge], true, false)[0]

        // then
        const expected = {
          data: {id: graphEdge.id, source: "source", target: "target", weight: 1, isCyclic: true, type: 'inheritance'},

        }
        expect(edge).toEqual(expected)
      })

    it('Should not label ElementDefiniton if showLabels is false', () => {
      // given
      const graphEdge = buildGraphEdge({
        weight: 2
      })

      // when
      const edge = toCytoscapeEdges([graphEdge], false, true)[0]

      // then
      expect(edge.style).toBeUndefined()
    })

    it('Should label ElementDefiniton with type if showLabels is true and weight is 1', () => {
      // given
      const graphEdge = buildGraphEdge({
        weight: 1,
        type: 'inheritance'
      })

      // when
      const edge = toCytoscapeEdges([graphEdge], true, true)[0]

      // then
      expect(edge.style.label).toEqual(`Inherits\n‎ `)
    })

    it('Should label ElementDefiniton if showLabels is true and weight is greater than 1', () => {
      // given
      const graphEdge = buildGraphEdge({
        weight: 5
      })

      // when
      const edge = toCytoscapeEdges([graphEdge], true, true)[0]

      // then
      expect(edge.style.label).toEqual(5)
    })

    it('Converts edgeCollection to graphEdges', () => {
      const target = buildVisibleGraphNode()
      const source = buildVisibleGraphNode()
      const expectedEdge = buildGraphEdge({
        source: source,
        target: target,
        isCyclic: true,
        weight: 1,
        type: 'usage'
      })

      const cyEdges = new EdgeCollectionBuilder()
        .addGraphEdge(expectedEdge)
        .build()

      const edges = toGraphEdges(cyEdges, [target, source])
      expect(edges[0]).toEqual(expectedEdge)
    })

    it('Returns no GraphEdges if there are no suitable GraphNodes', () => {
      const expectedEdge = buildGraphEdge()
      const cyEdges = new EdgeCollectionBuilder()
        .addGraphEdge(expectedEdge)
        .build()

      const edges = toGraphEdges(cyEdges, [])
      expect(edges.length).toEqual(0)
    })
  })
})
