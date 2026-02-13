import {Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, OnDestroy, Output, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {GraphNode} from '../../model/GraphNode';

type ResizeDirection = 'top' | 'left' | 'top-left';

export enum SortOption {
  ALPHABETICAL_ASC = 'alphabeticalAsc',
  ALPHABETICAL_DESC = 'alphabeticalDesc',
  HIERARCHY_ASC = 'hierarchyAsc',
  HIERARCHY_DESC = 'hierarchyDesc',
  PARENT_ASC = 'parentAsc',
  PARENT_DESC = 'parentDesc',
  TYPE_ASC = 'typeAsc',
  TYPE_DESC = 'typeDesc'
}

export interface RestoreNodeEvent {
  nodeId: string;
  parentNodeId: string;
}

@Component({
  selector: 'hidden-nodes-list',
  imports: [NgForOf, NgIf, NgClass],
  templateUrl: './hidden-nodes-list.component.html',
  standalone: true,
  styleUrl: './hidden-nodes-list.component.css'
})
export class HiddenNodesListComponent implements OnChanges, OnDestroy {
  @Input() hiddenNodes: GraphNode[] = [];
  @Output() restoreNode = new EventEmitter<RestoreNodeEvent>();
  @Output() restoreAllHidden = new EventEmitter<void>();

  isExpanded = false;
  selectedSort: SortOption = SortOption.ALPHABETICAL_ASC;
  focusedEntryKey: string | null = null;

  sortedHiddenNodes: GraphNode[] = [];

  // Resize state
  private isResizing = false;
  private resizeDirection: ResizeDirection | null = null;
  private startX = 0;
  private startY = 0;
  private startWidth = 0;
  private startHeight = 0;
  private readonly minWidth = 280;
  private readonly minHeight = 150;
  private readonly defaultWidth = 600;
  private readonly defaultHeight = 250;

  private readonly boundOnMouseMove = this.onResizeMove.bind(this);
  private readonly boundOnMouseUp = this.onResizeEnd.bind(this);

  constructor(private readonly elementRef: ElementRef) {}

  allSortOptions = [
    {value: SortOption.ALPHABETICAL_ASC, label: 'Alphabetical (A-Z)'},
    {value: SortOption.ALPHABETICAL_DESC, label: 'Alphabetical (Z-A)'},
    {value: SortOption.HIERARCHY_ASC, label: 'Hierarchy (Shallow-Deep)'},
    {value: SortOption.HIERARCHY_DESC, label: 'Hierarchy (Deep-Shallow)'},
    {value: SortOption.PARENT_ASC, label: 'By Parent (A-Z)'},
    {value: SortOption.PARENT_DESC, label: 'By Parent (Z-A)'},
    {value: SortOption.TYPE_ASC, label: 'Type (A-Z)'},
    {value: SortOption.TYPE_DESC, label: 'Type (Z-A)'}
  ];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['hiddenNodes']) {
      this.updateSortedEntries();
    }
  }

  private updateSortedEntries(): void {
    const sortFn = this.getSortFunction();
    this.sortedHiddenNodes = [...this.hiddenNodes].sort(sortFn);
  }

  get hasHiddenNodes(): boolean {
    return this.hiddenNodes.length > 0;
  }

  get hiddenNodeCount(): number {
    return this.hiddenNodes.length;
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
    const overlay = this.elementRef.nativeElement.querySelector('.hidden-nodes-overlay');
    if (overlay) {
      overlay.style.width = `${this.defaultWidth}px`;
      overlay.style.height = `${this.defaultHeight}px`;
    }
  }

  onRestoreNodeClick(event: Event, node: GraphNode): void {
    event.stopPropagation();
    this.restoreNode.emit({
      nodeId: node.id,
      parentNodeId: node.parent?.id ?? ''
    });
  }

  onRestoreAllClick(): void {
    this.restoreAllHidden.emit();
  }

  getNodePath(node: GraphNode): string {
    return node.id.replace(/:leaf$/, '');
  }

  getPathPrefix(node: GraphNode): string {
    const path = this.getNodePath(node);
    const lastDot = path.lastIndexOf('.');
    if (lastDot === -1) return '';
    return path.substring(0, lastDot + 1);
  }

  getPathSuffix(node: GraphNode): string {
    const path = this.getNodePath(node);
    const lastDot = path.lastIndexOf('.');
    if (lastDot === -1) return path;
    return path.substring(lastDot + 1);
  }

  getNodeType(node: GraphNode): string {
    return node.children.length === 0 ? 'Leaf' : 'Container';
  }

  isLeaf(node: GraphNode): boolean {
    return node.children.length === 0;
  }

  onSortChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.selectedSort = selectElement.value as SortOption;
    this.updateSortedEntries();
  }

  private getSortFunction(): (a: GraphNode, b: GraphNode) => number {
    switch (this.selectedSort) {
      case SortOption.ALPHABETICAL_ASC:
        return (a, b) => this.getNodePath(a).localeCompare(this.getNodePath(b));
      case SortOption.ALPHABETICAL_DESC:
        return (a, b) => this.getNodePath(b).localeCompare(this.getNodePath(a));
      case SortOption.HIERARCHY_ASC:
        return (a, b) => this.getNodeDepth(a) - this.getNodeDepth(b);
      case SortOption.HIERARCHY_DESC:
        return (a, b) => this.getNodeDepth(b) - this.getNodeDepth(a);
      case SortOption.PARENT_ASC:
        return (a, b) => this.getParentId(a).localeCompare(this.getParentId(b));
      case SortOption.PARENT_DESC:
        return (a, b) => this.getParentId(b).localeCompare(this.getParentId(a));
      case SortOption.TYPE_ASC:
        return (a, b) => this.getNodeType(a).localeCompare(this.getNodeType(b));
      case SortOption.TYPE_DESC:
        return (a, b) => this.getNodeType(b).localeCompare(this.getNodeType(a));
      default:
        return () => 0;
    }
  }

  private getNodeDepth(node: GraphNode): number {
    return node.id.split('.').length;
  }

  private getParentId(node: GraphNode): string {
    return node.parent?.id ?? '';
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

    const overlay = this.elementRef.nativeElement.querySelector('.hidden-nodes-overlay');
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

    const overlay = this.elementRef.nativeElement.querySelector('.hidden-nodes-overlay');
    if (!overlay) return;

    const deltaX = event.clientX - this.startX;
    const deltaY = event.clientY - this.startY;

    if (this.resizeDirection === 'left' || this.resizeDirection === 'top-left') {
      const newWidth = Math.max(this.minWidth, this.startWidth - deltaX);
      overlay.style.width = `${newWidth}px`;
    }

    if (this.resizeDirection === 'top' || this.resizeDirection === 'top-left') {
      const newHeight = Math.max(this.minHeight, this.startHeight - deltaY);
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

  // Keyboard navigation
  @HostListener('document:keydown.arrowdown', ['$event'])
  onArrowDown(event: KeyboardEvent): void {
    if (!this.isExpanded || !this.hasHiddenNodes) return;
    if (this.isInputFocused(event)) return;
    event.preventDefault();
    this.moveFocus(1);
  }

  @HostListener('document:keydown.arrowup', ['$event'])
  onArrowUp(event: KeyboardEvent): void {
    if (!this.isExpanded || !this.hasHiddenNodes) return;
    if (this.isInputFocused(event)) return;
    event.preventDefault();
    this.moveFocus(-1);
  }

  @HostListener('document:keydown.s', ['$event'])
  onCycleSort(event: KeyboardEvent): void {
    if (!this.isExpanded || !this.hasHiddenNodes) return;
    if (this.isInputFocused(event)) return;
    event.preventDefault();
    this.cycleSort();
  }

  private cycleSort(): void {
    const sortValues = this.allSortOptions.map(opt => opt.value);
    const currentIndex = sortValues.indexOf(this.selectedSort);
    const nextIndex = (currentIndex + 1) % sortValues.length;
    this.selectedSort = sortValues[nextIndex];
    this.updateSortedEntries();
  }

  private isInputFocused(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement | null;
    if (!target) return false;
    return target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT';
  }

  private moveFocus(direction: number): void {
    const nodes = this.sortedHiddenNodes;
    if (nodes.length === 0) return;

    if (!this.focusedEntryKey) {
      const entry = direction > 0 ? nodes[0] : nodes[nodes.length - 1];
      this.focusedEntryKey = entry.id;
      return;
    }

    const currentIndex = nodes.findIndex(n => n.id === this.focusedEntryKey);
    if (currentIndex === -1) {
      this.focusedEntryKey = nodes[0].id;
      return;
    }

    const newIndex = currentIndex + direction;
    if (newIndex >= 0 && newIndex < nodes.length) {
      this.focusedEntryKey = nodes[newIndex].id;
    }
  }

  isFocused(node: GraphNode): boolean {
    return this.focusedEntryKey === node.id;
  }
}
