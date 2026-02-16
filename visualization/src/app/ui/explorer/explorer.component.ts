import {Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {GraphNode, GraphNodeUtils} from '../../model/GraphNode';

type ResizeDirection = 'bottom' | 'right' | 'bottom-right';

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

  // Resize state
  private isResizing = false;
  private resizeDirection: ResizeDirection | null = null;
  private startX = 0;
  private startY = 0;
  private startWidth = 0;
  private startHeight = 0;
  private readonly minWidth = 220;
  private readonly minHeight = 150;
  private readonly defaultWidth = 300;
  private readonly defaultHeight = 400;

  private readonly boundOnMouseMove = this.onResizeMove.bind(this);
  private readonly boundOnMouseUp = this.onResizeEnd.bind(this);

  constructor(private readonly elementRef: ElementRef) {}

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
      this.applyDefaultDimensions();
    }
  }

  private applyDefaultDimensions(): void {
    const overlay = this.elementRef.nativeElement.querySelector('.explorer-overlay');
    if (overlay) {
      overlay.style.width = `${this.defaultWidth}px`;
      overlay.style.height = `${this.defaultHeight}px`;
    }
  }

  // Resize
  ngOnDestroy(): void {
    this.removeResizeListeners();
  }

  onResizeStart(event: MouseEvent, direction: ResizeDirection): void {
    event.preventDefault();
    event.stopPropagation();

    this.isResizing = true;
    this.resizeDirection = direction;
    this.startX = event.clientX;
    this.startY = event.clientY;

    const overlay = this.elementRef.nativeElement.querySelector('.explorer-overlay');
    if (overlay) {
      const rect = overlay.getBoundingClientRect();
      this.startWidth = rect.width;
      this.startHeight = rect.height;
    }

    document.addEventListener('mousemove', this.boundOnMouseMove);
    document.addEventListener('mouseup', this.boundOnMouseUp);
  }

  private onResizeMove(event: MouseEvent): void {
    if (!this.isResizing || !this.resizeDirection) return;

    const overlay = this.elementRef.nativeElement.querySelector('.explorer-overlay');
    if (!overlay) return;

    const deltaX = event.clientX - this.startX;
    const deltaY = event.clientY - this.startY;

    if (this.resizeDirection === 'right' || this.resizeDirection === 'bottom-right') {
      const newWidth = Math.max(this.minWidth, this.startWidth + deltaX);
      overlay.style.width = `${newWidth}px`;
    }

    if (this.resizeDirection === 'bottom' || this.resizeDirection === 'bottom-right') {
      const newHeight = Math.max(this.minHeight, this.startHeight + deltaY);
      overlay.style.height = `${newHeight}px`;
    }
  }

  private onResizeEnd(): void {
    this.isResizing = false;
    this.resizeDirection = null;
    this.removeResizeListeners();
  }

  private removeResizeListeners(): void {
    document.removeEventListener('mousemove', this.boundOnMouseMove);
    document.removeEventListener('mouseup', this.boundOnMouseUp);
  }
}
