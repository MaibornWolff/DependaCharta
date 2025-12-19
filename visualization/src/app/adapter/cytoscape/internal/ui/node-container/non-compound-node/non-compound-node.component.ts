import {Component, inject, Input} from '@angular/core';
import {countParents, isPackage, VisibleGraphNode} from '../../../../../../model/GraphNode';
import {InteractionBarComponent} from './interaction-bar/interaction-bar.component';
import {CytoscapeService} from '../../../cytoscape.service';
import {Action} from '../../../../../../model/Action';
import {RenderInformation} from '../node-container.component';
import {StateChange} from '../../../../../../app.component';

@Component({
  selector: 'non-compound-node',
  imports: [
    InteractionBarComponent
  ],
  templateUrl: './non-compound-node.component.html',
  standalone: true,
  styleUrl: './non-compound-node.component.css'
})
export class NonCompoundNodeComponent {
  private stateService = inject(CytoscapeService)
  @Input() node!: VisibleGraphNode
  @Input() renderInformation: RenderInformation | undefined
  @Input() zoom: number = 1
  @Input() stateChange!: StateChange
  clickCoordinates = {x: 0, y: 0}

  isInteractionBarVisible(): boolean {
    return this.isPinned() || this.stateChange.state.hoveredNodeId === this.node.id
  }

  togglePin() {
    if (this.isPinned()) {
      this.stateService.graphActionHappened.emit(new Action.UnpinNode(this.node.id))
    } else {
      this.stateService.graphActionHappened.emit(new Action.PinNode(this.node.id))
    }
  }

  onMouseDown(event: MouseEvent) {
    this.clickCoordinates = {x: event.clientX, y: event.clientY}
  }

  onMouseUp(event: MouseEvent) {
    if (this.clickCoordinates.x === event.clientX && this.clickCoordinates.y === event.clientY) {
      if (this.stateChange.state.multiselectMode) {
        this.stateService.graphActionHappened.emit(new Action.ToggleNodeSelection(this.node.id))
      } else {
        this.expandNode()
      }
    }
  }

  onMouseEnter() {
    this.stateService.graphActionHappened.emit(new Action.ShowAllEdgesOfNode(this.node.id))
  }

  onMouseLeave() {
    this.stateService.graphActionHappened.emit(new Action.HideAllEdgesOfNode(this.node.id))
  }

  expandNode() {
    if (this.node.children.length > 0) {
      this.stateService.graphActionHappened.emit(new Action.ExpandNode(this.node.id))
    }
  }

  hideNode() {
    this.stateService.graphActionHappened.emit(new Action.HideNode(this.node.id))
  }

  calculatedNodeStyle(): Partial<CSSStyleDeclaration> {
    const currentLightness = this.calculateLightnessOfPackageNodes()
    const borderWidth = this.calculateBorderWidth()
    const borderColor = this.isPinned() && this.stateChange.state.isInteractive
      ? 'green'
      : (isPackage(this.node) ? 'black' : 'teal');
    const outlineFactor = this.node.isSelected ? (this.isPinned() ? 1 : 2) : 0
    const style: Partial<CSSStyleDeclaration> = {
      outline: `${borderWidth}px solid ${borderColor}`,
      boxShadow: `0 0 0 ${borderWidth*outlineFactor}px rgba(120, 120, 120, .5)`,
      backgroundColor: isPackage(this.node) ? `hsl(204, 70%, ${currentLightness}%)` : 'white',
    }

    if (isPackage(this.node)) {
      style.cursor = 'pointer';
    }

    if (this.renderInformation) {
      const coordinates = this.renderInformation.getCoordinates();
      style.position = 'absolute';
      style.left = coordinates.x + 'px';
      style.top = coordinates.y + 'px';
      style.width = this.renderInformation.outerWidth + 'px';
      style.height = this.renderInformation.outerHeight + 'px';
    }

    return style;
  }

  calculateLightnessOfPackageNodes() {
    const parentCount = countParents(this.node);
    const maxLightness = 95;
    const minLightness = 30;

    const lightnessDecrement = (maxLightness - minLightness) / 10;
    return Math.max(minLightness, maxLightness - (Math.min(parentCount, 6) * lightnessDecrement));
  }

  calculateBorderWidth() {
    return this.isPinned() && this.stateChange.state.isInteractive
      ? 3 / this.zoom
      : 1 / this.zoom
  }

  calculateLabel() {
    return this.node.label
  }

  isPinned(): boolean {
    return this.stateChange.state.selectedPinnedNodeIds.includes(this.node.id)
  }
}
