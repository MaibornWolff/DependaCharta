import {TestBed} from '@angular/core/testing';
import {NodeSingular} from 'cytoscape';
import {HighlightService} from './highlight.service';
import {CytoscapeGraphBuilder} from './converter/CytoscapeModelBuilders.spec';

describe('HighlightService', () => {
  const highlightedNode = 16;
  const highlightedEdge = 4;
  let service: HighlightService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(HighlightService);
  })

  it('should be created', () => {
    expect(service).toBeTruthy();
  })

  it('should highlight connected edges and nodes', () => {
    //given
    const cy = new CytoscapeGraphBuilder()
      .newDefaultEdge("a", "b")
      .build()

    const node: NodeSingular = cy.getElementById('a');

    //when
    service.highlight(node);

    //then
    const edges = node.connectedEdges();
    expect(parseInt(node.style('font-size'), 10)).toBe(highlightedNode);
    expect(parseInt(edges.style('width'), 10)).toBe(highlightedEdge);

    cy.destroy()
  })

  it('should unhighlight connected edges and nodes', () => {
    //given
    const cy = new CytoscapeGraphBuilder()
      .newDefaultEdge("a", "b")
      .build()

    const node: NodeSingular = cy.getElementById('a');
    service.highlight(node);

    //when
    service.undoHighlighting(cy);

    //then
    const edges = node.connectedEdges();
    expect(parseInt(node.style('font-size'), 10)).toBeLessThan(highlightedNode);
    expect(parseInt(edges.style('width'), 10)).toBeLessThan(highlightedEdge);

    cy.destroy()
  })

  it('edges and nodes not connected to given node should not be highlighted', () => {
    //given
    const cy = new CytoscapeGraphBuilder()
      .newDefaultEdge("givenNode", "connectedNode")
      .newDefaultEdge("notConnectedToGiven1", "notConnectedToGiven2")
      .build()

    // set style values, so no random values are chosen
    cy.getElementById('notConnectedToGiven1').style('font-size', 12);
    cy.getElementById('notConnectedToGiven1-notConnectedToGiven2').style('width', 1);

    //when
    service.highlight(cy.getElementById('givenNode'));

    //then
    const notConnectedNode = cy.getElementById('notConnectedToGiven1');
    const notConnectedEdges = notConnectedNode.connectedEdges();
    expect(parseInt(notConnectedNode.style('font-size'), 10)).toBeLessThan(highlightedNode);
    expect(parseInt(notConnectedEdges.style('width'), 10)).toBeLessThan(highlightedEdge);

    cy.destroy()
  })

  it('should handle highlighting when no nodes are connected', () => {
    // given
    const cy = new CytoscapeGraphBuilder()
      .newNode("a")
      .newNode("b")
      .build()

    const node: NodeSingular = cy.getElementById('a');

    // when
    service.highlight(node);

    // then
    expect(service.currentlyFocusedNodes?.length).toBe(0);
    expect(service.currentlyFocusedEdges?.length).toBe(0);

    cy.destroy()
  });

  it('should handle undo highlighting when no nodes are highlighted', () => {
    // given
    const cy = new CytoscapeGraphBuilder()
      .newNode("a")
      .newNode("b")
      .build()

    // when
    service.undoHighlighting(cy);

    // then
    expect(service.currentlyFocusedNodes).toBeUndefined();
    expect(service.currentlyFocusedEdges).toBeUndefined();

    cy.destroy()
  });
});
