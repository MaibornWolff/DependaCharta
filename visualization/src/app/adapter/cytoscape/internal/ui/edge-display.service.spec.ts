import {TestBed} from '@angular/core/testing';

import {
    CYCLE_EDGE_COLOR,
    EdgeDisplayService,
    FEEDBACK_EDGE_COLOR,
    REGULAR_EDGE_COLOR,
    TWISTED_EDGE_COLOR
} from './edge-display.service';
import {buildUniqueId} from '../../../../common/test/TestUtils.spec';
import {CytoscapeGraphBuilder, ElementDefinitionBuilder} from '../converter/CytoscapeModelBuilders.spec';
import {EdgeFilter, EdgeFilterType} from '../../../../model/EdgeFilter';
import {ShallowEdge} from '../../../../model/ShallowEdge.spec';

describe('EdgeDisplayService', () => {
  let service: EdgeDisplayService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EdgeDisplayService);
  });

  it('should color regular edge correctly', () => {
    //given
    const sourceNodeId = buildUniqueId()
    const regularEdgeNodeId = buildUniqueId()
    const dependency = ShallowEdge.build({
      source: sourceNodeId,
      target: sourceNodeId
    })
    const sourceNode = new ElementDefinitionBuilder()
      .setId(sourceNodeId)
      .setLevel(1)
      .setContainedDependencies([dependency])
      .build()
    const regularEdgeNode = new ElementDefinitionBuilder()
      .setId(regularEdgeNodeId)
      .setLevel(0)
      .build()
    const cy = new CytoscapeGraphBuilder()
      .newEdge(sourceNode, regularEdgeNode)
      .build()

    //when
    service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.ALL)])

    //then
    const regularEdge = cy.getElementById(sourceNodeId + "-" + regularEdgeNodeId)[0]
    expect(regularEdge.hidden()).toEqual(false)
    expect(regularEdge.style('line-color')).toEqual(REGULAR_EDGE_COLOR)
    expect(regularEdge.style('target-arrow-color')).toEqual(REGULAR_EDGE_COLOR)
  });

  it('should color cyclic edge correctly', () => {
    //given
    const sourceNodeId = buildUniqueId()
    const cyclicEdgeNodeId = buildUniqueId()
    const dependency = ShallowEdge.build({
      source: sourceNodeId,
      target: cyclicEdgeNodeId,
      isCyclic: true
    })
    const sourceNode = new ElementDefinitionBuilder()
      .setId(sourceNodeId)
      .setLevel(1)
      .setContainedDependencies([dependency])
      .build()
    const cyclicEdgeNode = new ElementDefinitionBuilder()
      .setId(cyclicEdgeNodeId)
      .setLevel(0)
      .build()
    const cy = new CytoscapeGraphBuilder()
      .newEdge(sourceNode, cyclicEdgeNode)
      .build()

    //when
    service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.ALL)])

    //then
    const cyclicEdge = cy.getElementById(sourceNodeId + "-" + cyclicEdgeNodeId)[0]
    expect(cyclicEdge.hidden()).toEqual(false)
    expect(cyclicEdge.style('line-color')).toEqual(CYCLE_EDGE_COLOR)
    expect(cyclicEdge.style('target-arrow-color')).toEqual(CYCLE_EDGE_COLOR)
  });

  it('should color twisted edge correctly', () => {
    //given
    const sourceNodeId = buildUniqueId()
    const twistedEdgeNodeId = buildUniqueId()
    const dependency = ShallowEdge.build({
      source: sourceNodeId,
      target: twistedEdgeNodeId,
      isPointingUpwards: true
    })
    const sourceNode = new ElementDefinitionBuilder()
      .setId(sourceNodeId)
      .setLevel(1)
      .setContainedDependencies([dependency])
      .build()
    const twistedEdgeNode = new ElementDefinitionBuilder()
      .setId(twistedEdgeNodeId)
      .setLevel(2)
      .build()
    const cy = new CytoscapeGraphBuilder()
      .newEdge(sourceNode, twistedEdgeNode)
      .build()

    //when
    service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.ALL)])

    //then
    const twistedEdge = cy.getElementById(sourceNodeId + "-" + twistedEdgeNodeId)[0]
    expect(twistedEdge.hidden()).toEqual(false)
    expect(twistedEdge.style('line-color')).toEqual(TWISTED_EDGE_COLOR)
    expect(twistedEdge.style('target-arrow-color')).toEqual(TWISTED_EDGE_COLOR)
    expect(twistedEdge.style('line-style')).toEqual('dotted')
  });

  it('should color feedback edge correctly', () => {
    //given
    const sourceNodeId = buildUniqueId()
    const feedbackEdgeNodeId = buildUniqueId()
    const dependency = ShallowEdge.build({
      source: sourceNodeId,
      target: feedbackEdgeNodeId,
      isCyclic: true,
      isPointingUpwards: true
    })
    const sourceNode = new ElementDefinitionBuilder()
      .setId(sourceNodeId)
      .setLevel(1)
      .setContainedDependencies([dependency])
      .build()
    const feedbackEdgeNode = new ElementDefinitionBuilder()
      .setId(feedbackEdgeNodeId)
      .setLevel(2)
      .build()
    const cy = new CytoscapeGraphBuilder()
      .newEdge(sourceNode, feedbackEdgeNode)
      .build()

    //when
    service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.ALL)])

    //then
    const feedbackEdge = cy.getElementById(sourceNodeId + "-" + feedbackEdgeNodeId)[0]
    expect(feedbackEdge.hidden()).toEqual(false)
    expect(feedbackEdge.style('line-color')).toEqual(FEEDBACK_EDGE_COLOR)
    expect(feedbackEdge.style('target-arrow-color')).toEqual(FEEDBACK_EDGE_COLOR)
  });

  it('should hide edges when they are not contained in EdgeFilterResult', () => {
      //given
      const cy = new CytoscapeGraphBuilder()
        .newDefaultEdge('source', 'target')
        .build()

      //when
      service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.NONE)])

      //then
      const edge = cy.getElementById('source-target')[0]
      expect(edge.hidden()).toEqual(true)
    }
  );
});
