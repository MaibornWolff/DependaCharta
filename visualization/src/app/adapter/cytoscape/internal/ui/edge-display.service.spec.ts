import {TestBed} from '@angular/core/testing';

import {
    CYCLE_EDGE_COLOR,
    EdgeDisplayService,
    FEEDBACK_LEAF_LEVEL_EDGE_COLOR,
    REGULAR_EDGE_COLOR,
    FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR
} from './edge-display.service';
import {buildUniqueId} from '../../../../common/test/TestUtils.spec';
import {CytoscapeGraphBuilder, ElementDefinitionBuilder} from '../converter/CytoscapeModelBuilders.spec';
import {EdgeFilter, EdgeFilterType} from '../../../../model/EdgeFilter';
import {ShallowEdge} from '../../../../model/Edge.spec';

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
      target: sourceNodeId,
      isCyclic: false,
      isPointingUpwards: false
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
      isCyclic: true,
      isPointingUpwards: false
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

  it('should color container level feedback edge correctly', () => {
    //given
    const sourceNodeId = buildUniqueId()
    const feedbackContainerLevelEdgeNodeId = buildUniqueId()
    const dependency = ShallowEdge.build({
      source: sourceNodeId,
      target: feedbackContainerLevelEdgeNodeId,
      isCyclic: false,
      isPointingUpwards: true
    })
    const sourceNode = new ElementDefinitionBuilder()
      .setId(sourceNodeId)
      .setLevel(1)
      .setContainedDependencies([dependency])
      .build()
    const feedbackContainerLevelEdgeNode = new ElementDefinitionBuilder()
      .setId(feedbackContainerLevelEdgeNodeId)
      .setLevel(2)
      .build()
    const cy = new CytoscapeGraphBuilder()
      .newEdge(sourceNode, feedbackContainerLevelEdgeNode)
      .build()

    //when
    service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.ALL)])

    //then
    const feedbackContainerLevelEdge = cy.getElementById(sourceNodeId + "-" + feedbackContainerLevelEdgeNodeId)[0]
    expect(feedbackContainerLevelEdge.hidden()).toEqual(false)
    expect(feedbackContainerLevelEdge.style('line-color')).toEqual(FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR)
    expect(feedbackContainerLevelEdge.style('target-arrow-color')).toEqual(FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR)
  });

  it('should color leaf level feedback edge correctly', () => {
    //given
    const sourceNodeId = buildUniqueId()
    const feedbackLeafLevelEdgeNodeId = buildUniqueId()
    const dependency = ShallowEdge.build({
      source: sourceNodeId,
      target: feedbackLeafLevelEdgeNodeId,
      isCyclic: true,
      isPointingUpwards: true
    })
    const sourceNode = new ElementDefinitionBuilder()
      .setId(sourceNodeId)
      .setLevel(1)
      .setContainedDependencies([dependency])
      .build()
    const feedbackLeafLevelEdgeNode = new ElementDefinitionBuilder()
      .setId(feedbackLeafLevelEdgeNodeId)
      .setLevel(2)
      .build()
    const cy = new CytoscapeGraphBuilder()
      .newEdge(sourceNode, feedbackLeafLevelEdgeNode)
      .build()

    //when
    service.applyFilters(cy, [EdgeFilter.fromEnum(EdgeFilterType.ALL)])

    //then
    const feedbackLeafLevelEdge = cy.getElementById(sourceNodeId + "-" + feedbackLeafLevelEdgeNodeId)[0]
    expect(feedbackLeafLevelEdge.hidden()).toEqual(false)
    expect(feedbackLeafLevelEdge.style('line-color')).toEqual(FEEDBACK_LEAF_LEVEL_EDGE_COLOR)
    expect(feedbackLeafLevelEdge.style('target-arrow-color')).toEqual(FEEDBACK_LEAF_LEVEL_EDGE_COLOR)
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
