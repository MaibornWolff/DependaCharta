import {Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {GraphNode, GraphNodeUtils} from '../../model/GraphNode';
import {ResizablePanel} from '../shared/resizable-panel';

@Component({
  selector: 'explorer',
  imports: [NgForOf, NgIf, NgClass, NgTemplateOutlet],
  templateUrl: './explorer.component.html',
  standalone: true,
  styleUrl: './explorer.component.css'
})
export class ExplorerComponent implements OnChanges, OnDestroy {
  @Input() rootNodes: GraphNode[] = [];
  @Input() hiddenNodeIds: string[] = [];
  @Output() navigateToNode = new EventEmitter<string>();
  @Output() hideNode = new EventEmitter<string>();
  @Output() hoverNode = new EventEmitter<string>();
  @Output() unhoverNode = new EventEmitter<void>();

  isExpanded = false;
  expandedTreeNodeIds = new Set<string>();

  filteredRootNodes: GraphNode[] = [];

  private readonly resizablePanel: ResizablePanel;

  constructor(private readonly elementRef: ElementRef) {
    this.resizablePanel = new ResizablePanel(elementRef, {
      overlaySelector: '.explorer-overlay',
      minWidth: 220,
      minHeight: 150,
      defaultWidth: 300,
      defaultHeight: 400,
      horizontalSign: 1,
      verticalSign: 1
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['rootNodes'] || changes['hiddenNodeIds']) {
      this.updateFilteredNodes();
    }
  }

  private updateFilteredNodes(): void {
    this.filteredRootNodes = this.filterHiddenNodes(this.rootNodes);
  }

  private filterHiddenNodes(nodes: GraphNode[]): GraphNode[] {
    return nodes.filter(node => !GraphNodeUtils.isNodeOrAncestorHidden(this.hiddenNodeIds, node));
  }

  getVisibleChildren(node: GraphNode): GraphNode[] {
    return node.children.filter(child => !GraphNodeUtils.isNodeOrAncestorHidden(this.hiddenNodeIds, child));
  }

  isContainer(node: GraphNode): boolean {
    return this.getVisibleChildren(node).length > 0;
  }

  isTreeNodeExpanded(node: GraphNode): boolean {
    return this.expandedTreeNodeIds.has(node.id);
  }

  toggleTreeNode(event: Event, node: GraphNode): void {
    event.stopPropagation();
    if (this.expandedTreeNodeIds.has(node.id)) {
      this.expandedTreeNodeIds.delete(node.id);
    } else {
      this.expandedTreeNodeIds.add(node.id);
    }
  }

  onRowClick(node: GraphNode): void {
    this.navigateToNode.emit(node.id);
  }

  onRowMouseEnter(node: GraphNode): void {
    this.hoverNode.emit(node.id);
  }

  onRowMouseLeave(): void {
    this.unhoverNode.emit();
  }

  onHideClick(event: Event, node: GraphNode): void {
    event.stopPropagation();
    this.hideNode.emit(node.id);
  }

  getNodeLabel(node: GraphNode): string {
    return node.label || node.id.split('.').pop()?.replace(/:leaf$/, '') || node.id;
  }

  trackByNode(_index: number, node: GraphNode): string {
    return node.id;
  }

  toggleExpanded(): void {
    this.isExpanded = !this.isExpanded;
    if (this.isExpanded) {
      this.resizablePanel.applyDefaultDimensions();
    }
  }

  onResizeStart(event: MouseEvent, axis: 'horizontal' | 'vertical' | 'both'): void {
    this.resizablePanel.startResize(event, axis);
  }

  ngOnDestroy(): void {
    this.resizablePanel.destroy();
  }
}
