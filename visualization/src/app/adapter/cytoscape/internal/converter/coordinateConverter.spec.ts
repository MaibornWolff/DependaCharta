import {toAbsoluteCoordinates} from './coordinateConverter';
import {buildVisibleGraphNode} from '../../../../model/ModelBuilders.spec';

describe('CoordinateConverter', () => {
  // We negate it to flip the graph to achieve the look of a LSM
  it('relative coordinates of a single root node without children, are only negated', () => {
    // given
    const visibleGraphNode = buildVisibleGraphNode()
    const positionOfNode = {
      nodeId: visibleGraphNode.id,
      x: 100,
      y: 200,
      level: 0,
    }
    const relativePositionByNodeId = new Map()
      .set(visibleGraphNode.id, positionOfNode)

    // when
    const coordinatesByNodeId = toAbsoluteCoordinates(relativePositionByNodeId,[visibleGraphNode])

    // then
    expect(coordinatesByNodeId.size).toEqual(1)
    expect(coordinatesByNodeId.get(visibleGraphNode.id)).toEqual({x: -100, y: -200})
  });

  it('the calculation of coordinates of a node, depend on the ancestors coordinates', () => {
    // given
    const parent = buildVisibleGraphNode()
    const child = buildVisibleGraphNode({
      parent: parent
    })
    const grandChild = buildVisibleGraphNode({
      parent: child
    })

    const positionOfParent = {
      nodeId: parent.id,
      x: 100,
      y: 200,
      level: 0,
    }
    const positionOfChild = {
      nodeId: child.id,
      x: 200,
      y: 300,
      level: 0,
    }
    const positionOfGrandChild = {
      nodeId: child.id,
      x: 300,
      y: 400,
      level: 0,
    }
    const relativePositionByNodeId = new Map()
      .set(parent.id, positionOfParent)
      .set(child.id, positionOfChild)
      .set(grandChild.id, positionOfGrandChild)

    parent.visibleChildren = [child]
    child.visibleChildren = [grandChild]

    // when
    const coordinatesByNodeId = toAbsoluteCoordinates(relativePositionByNodeId,[parent, child, grandChild])

    // then
    expect(coordinatesByNodeId.size).toEqual(3)
    expect(coordinatesByNodeId.get(grandChild.id)).toEqual({x: -660, y: -900})
  });

  it('calculated coordinates of node are smaller in relation to parent node', () => {
    // given
    const parent = buildVisibleGraphNode()
    const child = buildVisibleGraphNode({
      parent: parent
    })

    const positionOfParent = {
      nodeId: parent.id,
      x: 100,
      y: 200,
      level: 0,
    }
    const positionOfChild = {
      nodeId: child.id,
      x: 200,
      y: 300,
      level: 0,
    }
    const relativePositionByNodeId = new Map()
      .set(parent.id, positionOfParent)
      .set(child.id, positionOfChild)

    parent.visibleChildren = [child]

    // when
    const coordinatesByNodeId = toAbsoluteCoordinates(relativePositionByNodeId, [parent, child])

    // then
    expect(coordinatesByNodeId.get(child.id)!.x).toBeLessThan(coordinatesByNodeId.get(parent.id)!.x)
    expect(coordinatesByNodeId.get(child.id)!.y).toBeLessThan(coordinatesByNodeId.get(parent.id)!.y)
  })

  it('an empty list of root nodes, results in an empty result map', () => {
    // given & when
    const coordinatesByNodeId = toAbsoluteCoordinates(new Map(), [])

    // then
    expect(coordinatesByNodeId.size).toEqual(0)
  })
});
