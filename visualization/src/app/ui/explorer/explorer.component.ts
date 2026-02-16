import {Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {GraphNode, GraphNodeUtils} from '../../model/GraphNode';
import {ResizablePanel} from '../shared/resizable-panel';
import {fuzzyMatch, fuzzyHighlight, HighlightSegment} from './fuzzy-match';
import {collectAllNodes, deduplicateByParent, stripLeafSuffix} from './search-helpers';

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
  @Output() hideNodes = new EventEmitter<string[]>();
  @Output() hoverNode = new EventEmitter<string>();
  @Output() unhoverNode = new EventEmitter<void>();

  isExpanded = false;
  expandedTreeNodeIds = new Set<string>();

  filteredRootNodes: GraphNode[] = [];

  searchQuery = '';
  isSearchActive = false;
  searchResults: GraphNode[] = [];
  searchHighlightSegments = new Map<string, HighlightSegment[]>();

  private searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
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
      if (this.isSearchActive) {
        this.applySearch();
      }
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

  onSearchInput(query: string): void {
    this.searchQuery = query;
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
    }
    this.searchDebounceTimer = setTimeout(() => {
      this.applySearch();
    }, 150);
  }

  applySearch(): void {
    if (!this.searchQuery.trim()) {
      this.isSearchActive = false;
      this.searchResults = [];
      this.searchHighlightSegments.clear();
      return;
    }
    this.isSearchActive = true;
    this.searchHighlightSegments.clear();
    const allNodes = collectAllNodes(this.rootNodes, this.hiddenNodeIds);
    this.searchResults = allNodes.filter(node => {
      const displayId = stripLeafSuffix(node.id);
      if (fuzzyMatch(this.searchQuery, displayId)) {
        this.searchHighlightSegments.set(node.id, fuzzyHighlight(this.searchQuery, displayId));
        return true;
      }
      return false;
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.isSearchActive = false;
    this.searchResults = [];
    this.searchHighlightSegments.clear();
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
      this.searchDebounceTimer = null;
    }
  }

  onHideAllShownClick(): void {
    if (this.searchResults.length === 0) {
      return;
    }
    const matchedIds = this.searchResults.map(n => n.id);
    const deduplicated = deduplicateByParent(matchedIds);
    this.hideNodes.emit(deduplicated);
    this.clearSearch();
  }

  getHighlightSegments(nodeId: string): HighlightSegment[] {
    return this.searchHighlightSegments.get(nodeId) || [{text: stripLeafSuffix(nodeId), isMatch: false}];
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
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
    }
    this.resizablePanel.destroy();
  }
}
