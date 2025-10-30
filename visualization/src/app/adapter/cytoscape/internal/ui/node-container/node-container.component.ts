import {Component, Input} from '@angular/core';
import {VisibleGraphNode} from '../../../../../model/GraphNode';
import {NonCompoundNodeComponent} from './non-compound-node/non-compound-node.component';
import {CompoundNodeComponent} from './compound-node/compound-node.component';
import {StateChange} from '../../../../../app.component';
import {Position} from 'cytoscape';

@Component({
  selector: 'node-container',
  standalone: true,
  imports: [
    NonCompoundNodeComponent,
    CompoundNodeComponent
  ],
  templateUrl: './node-container.component.html',
  styleUrl: './node-container.component.css'
})
export class NodeContainerComponent {
  @Input() pan: Position = { x: 0, y: 0 }
  @Input() zoom: number = 1
  @Input() stateChange!: StateChange
  
  // TODO remove state; use State.hiddenChildrenIds instead
  nodes: VisibleGraphNode[] = []
  renderInformationByNodeId = new Map<string, RenderInformation>()

  add(node: RenderableNode) {
    this.nodes.push(node.node)
    this.renderInformationByNodeId.set(node.node.id, node.renderInformation)
  }

  // TODO this code seems "dead", but it is used in the git pipeline
  update(node: RenderableNode) {
    this.remove(node)
    this.add(new RenderableNode(node.node, node.renderInformation))
  }

  private remove(node: RenderableNode) {
    const index = this.nodes.findIndex(n => n.id === node.node.id)
    if (index !== -1) {
      this.nodes.splice(index, 1)
      this.renderInformationByNodeId.delete(node.node.id)
    }
  }
  
  removeAll() {
    this.nodes = []
    this.renderInformationByNodeId.clear()
  }

  transformStyle() {
    return {
      "transform": `translate(${this.pan.x}px, ${this.pan.y}px) scale(${this.zoom})`
    }
  }
}

// TODO make this part of VisibleGraphNode, or remove VisibleGraphNode entirely (moving its information to State)
export class RenderableNode {
  constructor(
    public readonly node: VisibleGraphNode,
    public readonly renderInformation: RenderInformation
  ) {}
}

export class RenderInformation {
  constructor(
    public readonly positionX: number,
    public readonly positionY: number,
    public readonly outerWidth: number,
    public readonly outerHeight: number
  ) {}

  getCoordinates(): Position {
    return {
      x: this.positionX - this.outerWidth / 2,
      y: this.positionY - this.outerHeight / 2
    }
  }
}
