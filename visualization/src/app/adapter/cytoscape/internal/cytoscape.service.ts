import {EventEmitter, inject, Injectable, NgZone, Output} from '@angular/core';
import cytoscape, {AbstractEventObject, BoundingBox12, Core, ElementDefinition, NodeCollection, Position} from 'cytoscape';
import {EdgeDisplayService} from './ui/edge-display.service';
import {toCytoscapeEdges, toCytoscapeNodes} from './converter/elementDefinitionConverter';
import {Action} from '../../../model/Action';
import {EdgeFilter} from '../../../model/EdgeFilter';
import {State} from "../../../model/State";
import {HighlightService} from './highlight.service';
import {lsmLayout} from './CyLsmLayout';
import {cytoscape_style, cytoscape_options} from './cytoscapeConfig';

@Injectable({
  providedIn: 'root'
})
// TODO we don't treat the internal cytoscape's state as a (reactive/angular-ish) state; choose better naming
export class CytoscapeService {
  // TODO cy = inject(CytoscapeCore osä)
  // TODO cy = inject(CytoscapeCore osä)
  cy!: Core
  private edgeDisplayService = inject(EdgeDisplayService)
  private highlightService = inject(HighlightService)
  private ngZone = inject(NgZone)

  private isPanning = false
  private lastPanPoint: Position = {x: 0, y: 0}

  // Reentrancy guard: prevents nested apply() calls (e.g., mouseleave during DOM teardown)
  private isApplying = false
  private pendingApply: { state: State, action: Action } | null = null

  // TODO naming
  @Output() graphActionHappened = new EventEmitter<Action>()
  @Output() interactionModeToggled = new EventEmitter<boolean>()
  @Output() layoutStopped = new EventEmitter<Core>()
  @Output() nodeDragged = new EventEmitter<Core>()
  @Output() panOrZoom = new EventEmitter<Core>()
  @Output() changeCursor = new EventEmitter<string>()

  apply(state: State, action: Action) {
    if (this.isApplying) {
      console.warn('⚠️ Re-entrant CytoscapeService.apply() detected, queueing:', action.constructor.name)
      this.pendingApply = { state, action }
      return
    }

    this.isApplying = true
    try {
      this.doApply(state, action)
    } finally {
      this.isApplying = false
    }

    if (this.pendingApply) {
      const { state: pendingState, action: pendingAction } = this.pendingApply
      this.pendingApply = null
      this.apply(pendingState, pendingAction)
    }
  }

  private doApply(state: State, action: Action) {
    if (action instanceof Action.InitializeState) {
      this.initialize()
    }
    const cy = this.get()
    switch (true) {
      case action instanceof Action.ChangeFilter: {
        this.updateGraph(cy, state, cy.nodes());
        break;
      }
      case action instanceof Action.ShowAllEdgesOfNode:
      case action instanceof Action.HideAllEdgesOfNode:
        this.applyFilters(cy, state)
        break
      case action instanceof Action.NavigateToEdge:
      case action instanceof Action.NavigateToNode:
        this.rerenderGraphFromState(cy, state)
        this.applyFilters(cy, state)
        break
      case action instanceof Action.InitializeState:
        this.initializeGraphFromState(cy, state)
        break
      case action instanceof Action.ResetView:
        cy.centre()
        break
      default:
        this.rerenderGraphFromState(cy, state)
        break
    }
  }

  private initializeGraphFromState(cy: Core, state: State) {
    this.renderGraphFromState(cy, state)
    cy.centre()
  }

  private rerenderGraphFromState(cy: Core, state: State) {
    const nodesInViewport = getNodesInViewport(cy)
    this.renderGraphFromState(cy, state)
    fitGraph(cy, renewNodes(cy, nodesInViewport))
  }

  private renderGraphFromState(cy: Core, state: State) {
    cy.remove(cy.nodes())
    const visibleNodes = state.getVisibleNodes()
    const elementDefinitions = toCytoscapeNodes(visibleNodes)
    cy.add(elementDefinitions)

    this.updateGraph(cy, state, cy.nodes())
  }

  private applyFilters(cy: Core, state: State) {
    const prominentNodeIds = [
      ...(state.hoveredNodeId ? [state.hoveredNodeId] : []),
      ...state.selectedNodeIds,
      ...state.pinnedNodeIds
    ]
    const filters = [
      EdgeFilter.fromEnum(state.selectedFilter),
      ...prominentNodeIds.map(EdgeFilter.forAllEdgeTypes)
    ]

    this.edgeDisplayService.applyFilters(cy, filters)
  }

  // TODO cy = ui interaction ⇒ move to component
  private updateGraph(cy: Core, state: State, nodesToAddEdgesFor: NodeCollection) {
    const newEdges = this.createNewEdges(nodesToAddEdgesFor, state)
    cy.remove(cy.edges())

    // remove style to get rid of browser warning in console:
    // "Setting a `style` bypass at element creation should be done only when absolutely necessary.  Try to use the stylesheet instead."
    const newEdgesWithoutStyle: ElementDefinition[] = newEdges.map(({style, ...rest}) => rest)
    cy.add(newEdgesWithoutStyle)
    cy.layout({name: 'lsmLayout'}).run()
    this.applyFilters(cy, state)
  }

  private createNewEdges(nodesToRerender: NodeCollection, state: State) {
    const allNodeIdsToRerender = nodesToRerender.map(node => node.data().id)
    const flattenedGraphNodes = allNodeIdsToRerender
      .map(nodeId => state.findGraphNode(nodeId))
      .filter(node => node.visibleChildren.length === 0) // Only render edges on unexpanded/leaf nodes
    const graphEdges = state.createEdges(flattenedGraphNodes)
    return toCytoscapeEdges(graphEdges, state.showLabels, state.isUsageShown)
  }

  get(): Core {
    return this.cy ?? this.initialize()
  }

  initialize(): Core {
    this.destroyExistingInstance()
    cytoscape.use(lsmLayout)
    this.ngZone.runOutsideAngular(() => {
      this.cy = this.createCytoscape()
      this.registerEventListeners(this.cy)
    })
    return this.cy
  }

  private destroyExistingInstance() {
    if (this.cy) {
      this.cy.removeAllListeners()
      this.cy.destroy()
    }
    this.highlightService.clearReferences()
  }

  private createCytoscape(): Core {
    return cytoscape({
      container: document.getElementById('cy'),
      style: cytoscape_style,
      layout: {
        name: 'lsmLayout',
      },
      ...cytoscape_options,
      minZoom: 0.01,
      maxZoom: 3.0,
    })
  }

  private registerEventListeners(cy: Core) {
    cy.on('mouseover', 'node', (event: AbstractEventObject) => {
      this.highlightService.highlight(event.target)
    })

    cy.on('mouseout', 'node', () => {
      this.highlightService.undoHighlighting(cy)
    })

    cy.on("layoutstop", () => {
      this.ngZone.run(() => this.layoutStopped.emit(cy))
    })

    cy.on('drag', 'node', () => {
      this.ngZone.run(() => this.nodeDragged.emit(cy))
    })

    cy.on('pan zoom', () => {
      this.ngZone.run(() => this.panOrZoom.emit(cy))
    })

    cy.on('cxttapstart', (event) => {
      this.isPanning = true
      this.lastPanPoint = {
        x: event.originalEvent.clientX,
        y: event.originalEvent.clientY
      }
      if (event.target !== cy) {
        event.target.addClass('no-overlay')
      }
      this.ngZone.run(() => this.changeCursor.emit('grabbing'))
    });

    cy.on('cxtdrag', (event) => {
      if (this.isPanning) {
        const currentPoint = { x: event.originalEvent.clientX, y: event.originalEvent.clientY };
        const deltaX = currentPoint.x - this.lastPanPoint.x;
        const deltaY = currentPoint.y - this.lastPanPoint.y;

        const pan = cy.pan();
        cy.pan({
          x: pan.x + deltaX,
          y: pan.y + deltaY
        });

        this.lastPanPoint = currentPoint;
      }
    });

    cy.on('cxttapend', (event) => {
      this.isPanning = false;
      if (event.target !== cy) {
        event.target.removeClass('no-overlay')
      }
      this.ngZone.run(() => this.changeCursor.emit('auto'))
    });
  }

}

const getNodesInViewport = (cy: Core) =>
  cy.nodes().filter(node =>
    isContained(node.boundingbox(), cy.extent())
  )

const isContained = (element: BoundingBox12, container: BoundingBox12) => 
  element.x1 >= container.x1 && element.x2 <= container.x2 &&
  element.y1 >= container.y1 && element.y2 <= container.y2

function renewNodes(cy: Core, oldNodes: NodeCollection) {
  return cy.nodes().filter(node => oldNodes.map(oldNode => oldNode.id()).includes(node.id()))
}

function fitGraph(cy: Core, nodesToShow: NodeCollection) {
  if (!getNodesInViewport(cy).contains(nodesToShow)) {
    cy.animate({
      fit: {
        eles: nodesToShow,
        padding: 50
      }
    }, {
      duration: 500
    })
  }
}
