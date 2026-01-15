import {Injectable} from '@angular/core';
import {Core, EdgeCollection, EdgeSingular, NodeCollection, NodeSingular} from 'cytoscape';

@Injectable({
  providedIn: 'root'
})
export class HighlightService {
  currentlyFocusedEdges?: EdgeCollection
  currentlyFocusedNodes?: NodeCollection

  highlight(node: NodeSingular) {
    const edges = node.connectedEdges()
    const connectedNodes = edges.connectedNodes()

    this.currentlyFocusedNodes = connectedNodes
    this.currentlyFocusedEdges = edges

    edges.map((edge: EdgeSingular) => edge.style("width", 4))
    connectedNodes.map((node: NodeSingular) => node.style("font-size", 16))
  }

  undoHighlighting(cy: Core) {
    if (this.currentlyFocusedEdges) {
      this.currentlyFocusedEdges.forEach(edge => {
        cy.getElementById(edge.data().id).style("width", 1)
      })
      this.currentlyFocusedEdges = undefined
    }

    if (this.currentlyFocusedNodes) {
      this.currentlyFocusedNodes.forEach(node => {
        cy.getElementById(node.data().id).style("font-size", 12)
      })
      this.currentlyFocusedNodes = undefined
    }
  }

  clearReferences() {
    this.currentlyFocusedEdges = undefined
    this.currentlyFocusedNodes = undefined
  }

}
