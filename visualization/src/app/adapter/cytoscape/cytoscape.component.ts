import {Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, Renderer2, SimpleChanges, ViewChild} from '@angular/core';
import {CytoscapeService} from './internal/cytoscape.service';
import {Action} from '../../model/Action';
import {Core, NodeSingular} from 'cytoscape';
import {NodeContainerComponent, RenderableNode, RenderInformation} from './internal/ui/node-container/node-container.component';
import {StateChange} from '../../app.component';

@Component({
  selector: 'cytoscape',
  standalone: true,
  imports: [
    NodeContainerComponent,
  ],
  templateUrl: './cytoscape.component.html',
  styleUrl: './cytoscape.component.css'
})
export class CytoscapeComponent implements OnInit, OnChanges {
  cytoscapeService = inject(CytoscapeService)
  renderer = inject(Renderer2)
  // TODO naming
  @Output() graphActionHappened = new EventEmitter<Action>()
  // TODO use "required" parameter of @Input, research
  @Input() stateChange!: StateChange
  @ViewChild("non_compound_node_container") nonCompoundNodeContainer!: NodeContainerComponent
  @ViewChild("compound_node_container") compoundNodeContainer!: NodeContainerComponent

  pan: { x: number, y: number } = {x: 0, y: 0}
  zoom: number = 1

  ngOnInit() {
    this.registerEventListeners()
  }

  ngOnChanges(changes: SimpleChanges) {
    // Checking for `firstChange` fixes/hides a bug.
    // When loading a file, the CytoscapeComponent reacts via `ngOnChanges`,
    // thereby re-applying the last event.
    // When the last event is `INITIALIZE_STATE`, the graph disappears. Bug.
    if (this.stateChange && !changes['stateChange'].firstChange) {
      this.cytoscapeService.apply(this.stateChange.state, this.stateChange.action)
    }
  }

  // layoutStopped, nodesPositioned
  private rebuildNodeContainers(cy: Core) {
    this.compoundNodeContainer.removeAll()
    this.nonCompoundNodeContainer.removeAll()
    const nodes = cy.nodes()
    const renderableNodes = nodes.map(toRenderableNode)

    renderableNodes.forEach(renderableNode => {
      if (renderableNode.node.isExpanded) {
        this.compoundNodeContainer.add(renderableNode)
      } else {
        this.nonCompoundNodeContainer.add(renderableNode)
      }
    })

    nodes.forEach(hideCytoscapeNode)
  }

  private registerEventListeners() {
    // TODO streamline propagation of events
    this.cytoscapeService.graphActionHappened.subscribe(action =>
      this.graphActionHappened.emit(action)
    )
    // state is missing
    this.cytoscapeService.layoutStopped.subscribe(cy => {
      this.rebuildNodeContainers(cy)
    })
    // state is missing
    this.cytoscapeService.nodesPositioned.subscribe(cy => {
      this.rebuildNodeContainers(cy)
    })
    this.cytoscapeService.panOrZoom.subscribe(cy => {
      this.pan = cy.pan()
      this.zoom = cy.zoom()
    })
    this.cytoscapeService.changeCursor.subscribe(style =>
      this.renderer.setStyle(document.body, 'cursor', style)
    )
  }
}

function getRenderInformation(node: NodeSingular) {
  return new RenderInformation(
    node.position().x,
    node.position().y,
    node.outerWidth(),
    node.outerHeight()
  )
}

function hideCytoscapeNode(node: NodeSingular) {
  node.style({"border-opacity": 0})
  node.style({"background-opacity": 0})
  node.style({"text-opacity": 0})
}

function toRenderableNode(node: NodeSingular) {
  return new RenderableNode(
    node.data().visibleGraphNode,
    getRenderInformation(node)
  )
}
