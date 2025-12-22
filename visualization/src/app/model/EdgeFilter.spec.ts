import {EdgeFilter, EdgeFilterResult, EdgeFilterType} from './EdgeFilter';
import {EdgeType} from './EdgeType';
import {Edge} from './Edge.spec';
import {VisibleGraphNode} from './GraphNode.spec';

describe('New Graph Filter', () => {
  it('showNoEdges should not return any edges', () => {
    // given
    const edge = Edge.build()

    // when
    const edges = [edge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.NONE)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showOnlyFeedbackLeafLevelEdges should only return leaf level feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const feedbackLeafLevelEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })
    const nonFeedbackEdge = Edge.build({
      source: level1Node,
      target: level0Node,
      isCyclic: true,
      isPointingUpwards: false
    })

    // when
    const edges = [feedbackLeafLevelEdge, nonFeedbackEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(feedbackLeafLevelEdge.id, EdgeType.FEEDBACK_LEAF_LEVEL)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showOnlyFeedbackLeafLevelEdges should not return container level feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })


    const feedbackLeafLevelEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })
    const feedbackContainerLevelEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    // when
    const edges = [feedbackLeafLevelEdge, feedbackContainerLevelEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(feedbackLeafLevelEdge.id, EdgeType.FEEDBACK_LEAF_LEVEL)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showOnlyFeedbackLeafLevelEdges should not return any non-cyclic edges', () => {
    // given
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 0
      }),
      target: VisibleGraphNode.build({
        level: 1
      }),
      isCyclic: false,
      isPointingUpwards: true
    })

    // when
    const edges = [edge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showAllFeedbackEdges should return container level and leaf level feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const feedbackContainerLevelEdge = Edge.build({
      id: '1',
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    const feedbackLeafLevelEdge = Edge.build({
      id: '2',
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })

    // when
    const edges = [feedbackContainerLevelEdge, feedbackLeafLevelEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.ALL_FEEDBACK_EDGES)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(feedbackLeafLevelEdge.id, EdgeType.FEEDBACK_LEAF_LEVEL)
      .set(feedbackContainerLevelEdge.id, EdgeType.FEEDBACK_CONTAINER_LEVEL)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showAllFeedbackEdges should not return regular and cyclic edges which are no feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const regularEdge = Edge.build({
      source: level1Node,
      target: level0Node,
      isCyclic: false,
      isPointingUpwards: false
    })

    const regularCyclicEdge = Edge.build({
      source: level1Node,
      target: level0Node,
      isCyclic: true,
      isPointingUpwards: false
    })

    // when
    const edges = [regularEdge, regularCyclicEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.ALL_FEEDBACK_EDGES)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showCyclicEdges should show cyclic and leaf level feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const feedbackLeafLevelEdge = Edge.build({
      id: '1',
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })

    const regularCyclicEdge = Edge.build({
      id: '2',
      source: level1Node,
      target: level0Node,
      isCyclic: true,
      isPointingUpwards: false
    })

    // when
    const edges = [feedbackLeafLevelEdge, regularCyclicEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.CYCLES_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(regularCyclicEdge.id, EdgeType.CYCLIC)
      .set(feedbackLeafLevelEdge.id, EdgeType.FEEDBACK_LEAF_LEVEL)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showCyclicEdges should not show regular and container level feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const regularEdge = Edge.build({
      source: level1Node,
      target: level0Node,
      isCyclic: false,
      isPointingUpwards: false
    })

    const feedbackContainerLevelEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    // when
    const edges = [regularEdge, feedbackContainerLevelEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.CYCLES_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showAllEdges should show all edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const regularEdge = Edge.build({
      id: '1',
      source: level1Node,
      target: level0Node,
      isCyclic: false,
      isPointingUpwards: false
    })

    const feedbackContainerLevelEdge = Edge.build({
      id: '2',
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    const feedbackLeafLevelEdge = Edge.build({
      id: '3',
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })

    const regularCyclicEdge = Edge.build({
      id: '4',
      source: level1Node,
      target: level0Node,
      isCyclic: true,
      isPointingUpwards: false
    })

    // when
    const edges = [regularEdge, regularCyclicEdge, feedbackContainerLevelEdge, feedbackLeafLevelEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.ALL)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected: EdgeFilterResult = new Map()
      .set(regularEdge.id, EdgeType.REGULAR)
      .set(regularCyclicEdge.id, EdgeType.CYCLIC)
      .set(feedbackContainerLevelEdge.id, EdgeType.FEEDBACK_CONTAINER_LEVEL)
      .set(feedbackLeafLevelEdge.id, EdgeType.FEEDBACK_LEAF_LEVEL)

    expect(edgeFilterResult).toEqual(expected)
  })
})
