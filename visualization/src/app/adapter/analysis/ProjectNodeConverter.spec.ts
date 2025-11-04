import {convertToGraphNodes, convertToGraphNodesWithLightEdges} from './ProjectNodeConverter'
import {EdgeMetaInformation} from './internal/ProjectReport'
import {exampleJson} from '../../common/test/exampleJson.spec'
import {GraphNode} from '../../model/GraphNode.spec'
import {ShallowEdge} from '../../model/ShallowEdge.spec'
import {ProjectNode} from './internal/ProjectReport.spec'

describe('ProjectNodeConverter', () => {
  it('should convert json to GraphNodes', () => {
    // given + when
    const graphNodes = convertToGraphNodes(exampleJson)

    // then
    expect(graphNodes.length).toEqual(1)
  })

  it('should convert ProjectNode without children to GraphNode', () => {
    // given
    const id = "nodeId"
    const name = "some name"
    const level = 1
    const dependencyTargetId = "otherNodeId"
    const isCyclic = false
    const weight = 12
    const type = 'usage'
    const dependencies: Record<string, EdgeMetaInformation> = {
      [dependencyTargetId]: {
        isCyclic: isCyclic,
        weight: weight,
        type: type
      }
    }
    const projectNode = ProjectNode.build({
      leafId: id,
      name: name,
      level: level,
      containedInternalDependencies: dependencies
    })

    // when
    const graphNodes = convertToGraphNodesWithLightEdges([projectNode])

    // then
    const expectedDependency = ShallowEdge.build({
      id: id + ":leaf" + "-" + dependencyTargetId + ":leaf",
      source: id + ":leaf",
      target: dependencyTargetId + ":leaf",
      isCyclic: isCyclic,
      weight: weight
    })

    const expected = GraphNode.build({
      id: id + ":leaf",
      children: [],
      dependencies: [expectedDependency],
      label: name,
      level: level
    })

    expect(graphNodes.length).toEqual(1)
    expect(graphNodes[0]).toEqual(expected)
  })

  it('should convert ProjectNode with children to GraphNode', () => {
    // given
    const parentName = "some name"
    const childName = "childId"
    const child = ProjectNode.build({
      leafId: parentName + "." + childName,
      name: childName
    })
    const parent = ProjectNode.build({
      children: [child],
      name: parentName
    })

    // when
    const graphNodes = convertToGraphNodesWithLightEdges([parent])

    // then
    const expectedChild: GraphNode = {
      id: parentName + "." + childName + ":leaf",
      children: [],
      dependencies: [],
      label: childName,
      level: child.level,
    }
    const expected: GraphNode = {
      id: parentName,
      children: [expectedChild],
      dependencies: [],
      label: parentName,
      level: parent.level
    }
    expectedChild.parent = expected

    expect(graphNodes.length).toEqual(1)
    expect(graphNodes[0]).toEqual(expected)
  })
})
