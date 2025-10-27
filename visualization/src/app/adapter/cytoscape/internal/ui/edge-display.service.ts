import {Injectable} from '@angular/core';
import {Core, EdgeSingular} from 'cytoscape';
import {EdgeFilter, EdgeFilterResult, EdgeId} from '../../../../model/EdgeFilter';
import {toGraphEdges, toVisibleGraphNodes} from '../converter/elementDefinitionConverter';
import {EdgeType} from '../../../../model/EdgeType';

export const REGULAR_EDGE_COLOR = "rgb(128,128,128)"
export const CYCLE_EDGE_COLOR = "rgb(0,0,255)";
export const FEEDBACK_EDGE_COLOR = "rgb(255,0,0)";
export const TWISTED_EDGE_COLOR = "rgb(255,0,0)";

@Injectable({
  providedIn: 'root'
})
export class EdgeDisplayService {

  applyFilters(cy: Core, filters: EdgeFilter[]) {
    const nodes = toVisibleGraphNodes(cy.nodes())
    const edges = toGraphEdges(cy.edges(), nodes)

    const edgeFilterResult = filters
      .map(filter => filter(edges))
      .reduce((acc, curr) => new Map([...acc, ...curr]))

    cy.edges().forEach(edge => {
      this.applyStyle(edge, this.getEdgeType(edgeFilterResult, edge.data().id))
    })
  }

  getEdgeType(filterResult: EdgeFilterResult, edgeId: EdgeId): EdgeType {
    return filterResult.get(edgeId) ?? EdgeType.NONE
  }

  applyStyle(edge: EdgeSingular, edgeType: EdgeType) {
    switch (edgeType) {
      case EdgeType.REGULAR:
        edge
          .style("display", "element")
          .style("line-color", REGULAR_EDGE_COLOR)
          .style("target-arrow-color", REGULAR_EDGE_COLOR)
        break
      case EdgeType.CYCLIC:
        edge
          .style("display", "element")
          .style("line-color", CYCLE_EDGE_COLOR)
          .style("target-arrow-color", CYCLE_EDGE_COLOR)
        break
      case EdgeType.FEEDBACK:
        edge
          .style("display", "element")
          .style("line-color", FEEDBACK_EDGE_COLOR)
          .style("target-arrow-color", FEEDBACK_EDGE_COLOR)
        break
      case EdgeType.TWISTED:
        edge
          .style("display", "element")
          .style("line-color", TWISTED_EDGE_COLOR)
          .style("target-arrow-color", TWISTED_EDGE_COLOR)
          .style('line-style', 'dotted')
        break
      case EdgeType.NONE:
        edge.style("display", "none")
    }
  }
}
