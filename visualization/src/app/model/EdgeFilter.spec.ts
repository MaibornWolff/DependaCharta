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

  it('showOnlyFeedbackEdges should only return feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const feedbackEdge = Edge.build({
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
    const edges = [feedbackEdge, nonFeedbackEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_EDGES_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(feedbackEdge.id, EdgeType.FEEDBACK)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showOnlyFeedbackEdges should not return twisted edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })


    const feedbackEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })
    const twistedEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    // when
    const edges = [feedbackEdge, twistedEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_EDGES_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(feedbackEdge.id, EdgeType.FEEDBACK)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showOnlyFeedbackEdges should not return any non-cyclic edges', () => {
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
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_EDGES_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showFeedbackEdgesAndTwistedEdges should return twisted and feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const twistedEdge = Edge.build({
      id: '1',
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    const feedbackEdge = Edge.build({
      id: '2',
      source: level0Node,
      target: level1Node,
      isCyclic: true,
      isPointingUpwards: true
    })

    // when
    const edges = [twistedEdge, feedbackEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(feedbackEdge.id, EdgeType.FEEDBACK)
      .set(twistedEdge.id, EdgeType.TWISTED)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showFeedbackEdgesAndTwistedEdges should not return regular and cyclic edges which are no feedback or twisted edge', () => {
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
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showCyclicEdges should show cyclic and feedback edges', () => {
    // given
    const level0Node = VisibleGraphNode.build({
      level: 0
    })
    const level1Node = VisibleGraphNode.build({
      level: 1
    })

    const feedbackEdge = Edge.build({
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
    const edges = [feedbackEdge, regularCyclicEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.CYCLES_ONLY)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected = EdgeFilterResult.empty()
      .set(regularCyclicEdge.id, EdgeType.CYCLIC)
      .set(feedbackEdge.id, EdgeType.FEEDBACK)
    expect(edgeFilterResult).toEqual(expected)
  })

  it('showCyclicEdges should not show regular and twisted edges', () => {
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

    const twistedEdge = Edge.build({
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    // when
    const edges = [regularEdge, twistedEdge]
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

    const twistedEdge = Edge.build({
      id: '2',
      source: level0Node,
      target: level1Node,
      isCyclic: false,
      isPointingUpwards: true
    })

    const feedbackEdge = Edge.build({
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
    const edges = [regularEdge, regularCyclicEdge, twistedEdge, feedbackEdge]
    const edgeFilter = EdgeFilter.fromEnum(EdgeFilterType.ALL)
    const edgeFilterResult = edgeFilter(edges)

    // then
    const expected: EdgeFilterResult = new Map()
      .set(regularEdge.id, EdgeType.REGULAR)
      .set(regularCyclicEdge.id, EdgeType.CYCLIC)
      .set(twistedEdge.id, EdgeType.TWISTED)
      .set(feedbackEdge.id, EdgeType.FEEDBACK)

    expect(edgeFilterResult).toEqual(expected)
  })
})
