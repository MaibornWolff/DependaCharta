import {Component, inject, Input} from '@angular/core';
import {CytoscapeService} from '../../../cytoscape.service';
import {getDescendants, VisibleGraphNode} from '../../../../../../model/GraphNode';
import {MatDivider} from '@angular/material/divider';
import {Action, UnpinNode, PinNode, HideNode, RestoreAllChildren, RestoreNode, CollapseNode} from '../../../../../../model/Action';
import {InteractionMenuComponent} from './interaction-menu/interaction-menu.component';
import {RenderInformation} from '../node-container.component';
import {StateChange} from '../../../../../../app.component';

@Component({
  selector: 'compound-node',
  imports: [
    MatDivider,
    InteractionMenuComponent
  ],
  templateUrl: './compound-node.component.html',
  standalone: true,
  styleUrl: './compound-node.component.css'
})
export class CompoundNodeComponent {
  private stateService = inject(CytoscapeService)
  @Input() node!: VisibleGraphNode
  @Input() renderInformation: RenderInformation | undefined;
  @Input() zoom: number = 1;
  @Input() stateChange!: StateChange

  togglePin() {
    if (this.isPinned()) {
      this.stateService.graphActionHappened.emit(new UnpinNode(this.node.id))
    } else {
      this.stateService.graphActionHappened.emit(new PinNode(this.node.id))
    }
  }

  hideNode() {
    for (const descendant of getDescendants(this.node)) {
      this.stateService.graphActionHappened.emit(new UnpinNode(descendant.id))
    }

    this.stateService.graphActionHappened.emit(new HideNode(this.node.id))
  }

  restoreAllChildren() {
    this.stateService.graphActionHappened.emit(new RestoreAllChildren(this.node.id))
  }

  restoreNode(nodeRestored: string) {
    this.stateService.graphActionHappened.emit(new RestoreNode(nodeRestored, this.node.id))
  }

  collapseNode() {
    this.stateService.graphActionHappened.emit(new CollapseNode(this.node.id))
  }

  calculatedStyle() {
    const borderWidth = this.calculateBorderWidth()
    const borderColor = this.isPinned() && this.stateChange.state.isInteractive
      ? 'green'
      : 'black'
    if (!this.renderInformation) {
      return {
        outline: borderWidth + 'px solid ' + borderColor,
      }
    }
    const coordinates = this.coordinates(this.renderInformation)
    return {
      position: 'absolute',
      left: coordinates.x + 'px',
      top: coordinates.y + 'px',
      width: this.renderInformation.outerWidth + 'px',
      height: this.renderInformation.outerHeight + 'px',
      outline: borderWidth + 'px solid ' + borderColor,
      zIndex: this.nodeDepth()
    }
  }

  private nodeDepth() {
    return this.node.id.split('.').length;
  }

  coordinates(renderInfo: RenderInformation): { x: number, y: number } {
    return {
      x: renderInfo.positionX - renderInfo.outerWidth / 2,
      y: renderInfo.positionY - renderInfo.outerHeight / 2
    }
  }

  private calculateBorderWidth() {
    return this.isPinned() && this.stateChange.state.isInteractive
      ? 3 / this.zoom
      : 1 / this.zoom
  }

  private isPinned(): boolean {
    return this.stateChange.state.selectedPinnedNodeIds.includes(this.node.id)
  }
}
