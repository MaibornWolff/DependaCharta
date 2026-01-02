import {Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {FeedbackEdgeGroup, FeedbackListEntry, getHierarchy, groupFeedbackEdges, ShallowEdge} from '../../model/Edge';

type ResizeDirection = 'top' | 'right' | 'top-right';

export enum SortOption {
  SOURCE_ASC = 'sourceAsc',
  SOURCE_DESC = 'sourceDesc',
  TARGET_ASC = 'targetAsc',
  TARGET_DESC = 'targetDesc',
  WEIGHT_ASC = 'weightAsc',
  WEIGHT_DESC = 'weightDesc',
  TYPE_ASC = 'typeAsc',
  TYPE_DESC = 'typeDesc',
  HIERARCHY_ASC = 'hierarchyAsc',
  HIERARCHY_DESC = 'hierarchyDesc'
}

@Component({
  selector: 'feedback-edges-list',
  imports: [NgForOf, NgIf, NgClass, NgTemplateOutlet],
  templateUrl: './feedback-edges-list.component.html',
  standalone: true,
  styleUrl: './feedback-edges-list.component.css'
})
export class FeedbackEdgesListComponent implements OnChanges, OnDestroy {
  @Input() feedbackEdges: ShallowEdge[] = [];
  @Output() edgeClicked = new EventEmitter<ShallowEdge>();
  @Output() groupClicked = new EventEmitter<FeedbackListEntry>();

  isExpanded = false;
  selectedSort: SortOption = SortOption.HIERARCHY_ASC;
  expandedGroups = new Set<string>();

  // Cached computed values
  sortedFeedbackEntries: FeedbackListEntry[] = [];
  private commonPrefixCache = new Map<string, string>();

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

  private boundOnMouseMove = this.onResizeMove.bind(this);
  private boundOnMouseUp = this.onResizeEnd.bind(this);

  constructor(private elementRef: ElementRef) {}

  allSortOptions = [
    {value: SortOption.HIERARCHY_ASC, label: 'Hierarchy (Low-High)'},
    {value: SortOption.HIERARCHY_DESC, label: 'Hierarchy (High-Low)'},
    {value: SortOption.SOURCE_ASC, label: 'Source (A-Z)'},
    {value: SortOption.SOURCE_DESC, label: 'Source (Z-A)'},
    {value: SortOption.TARGET_ASC, label: 'Target (A-Z)'},
    {value: SortOption.TARGET_DESC, label: 'Target (Z-A)'},
    {value: SortOption.WEIGHT_DESC, label: 'Weight (High-Low)'},
    {value: SortOption.WEIGHT_ASC, label: 'Weight (Low-High)'},
    {value: SortOption.TYPE_ASC, label: 'Type (A-Z)'},
    {value: SortOption.TYPE_DESC, label: 'Type (Z-A)'}
  ];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['feedbackEdges']) {
      this.updateSortedEntries();
    }
  }

  private updateSortedEntries(): void {
    this.commonPrefixCache.clear();
    const grouped = groupFeedbackEdges(this.feedbackEdges);
    this.sortedFeedbackEntries = this.sortEntriesRecursively(grouped);
  }

  private sortEntriesRecursively(entries: FeedbackListEntry[]): FeedbackListEntry[] {
    const sortFn = this.getSortFunction();
    return [...entries].sort(sortFn).map(entry => {
      if (entry.isGroup && entry.children.length > 0) {
        const sortedChildren = this.sortEntriesRecursively(entry.children);
        return new FeedbackEdgeGroup(entry.source, entry.target, sortedChildren);
      }
      return entry;
    });
  }

  get hasFeedbackEdges(): boolean {
    return this.feedbackEdges.length > 0;
  }

  get feedbackEdgeCount(): number {
    return this.feedbackEdges.length;
  }

  trackByEntry(index: number, entry: FeedbackListEntry): string {
    return `${entry.source}→${entry.target}`;
  }

  toggleExpanded(): void {
    this.isExpanded = !this.isExpanded;
    if (this.isExpanded) {
      this.applyDefaultDimensions();
    }
  }

  private applyDefaultDimensions(): void {
    const overlay = this.elementRef.nativeElement.querySelector('.feedback-edges-overlay');
    if (overlay) {
      overlay.style.width = `${this.defaultWidth}px`;
      overlay.style.height = `${this.defaultHeight}px`;
    }
  }

  onEntryClick(entry: FeedbackListEntry): void {
    if (entry.isGroup) {
      this.groupClicked.emit(entry);
    } else {
      this.edgeClicked.emit(entry as ShallowEdge);
    }
  }

  handleEntryClick(entry: FeedbackListEntry): void {
    this.onEntryClick(entry);
  }

  onChevronClick(event: Event, entry: FeedbackListEntry): void {
    event.stopPropagation();
    this.toggleGroupExpanded(entry);
  }

  onPrefixClick(event: Event, entry: FeedbackListEntry): void {
    if (entry.isGroup) {
      this.onChevronClick(event, entry);
    }
  }

  toggleGroupExpanded(entry: FeedbackListEntry): void {
    const groupKey = this.getGroupKey(entry);
    if (this.expandedGroups.has(groupKey)) {
      this.expandedGroups.delete(groupKey);
    } else {
      this.expandedGroups.add(groupKey);
    }
  }

  isGroupExpanded(entry: FeedbackListEntry): boolean {
    return this.expandedGroups.has(this.getGroupKey(entry));
  }

  private getGroupKey(entry: FeedbackListEntry): string {
    return `${entry.source}→${entry.target}`;
  }

  onSortChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.selectedSort = selectElement.value as SortOption;
    this.updateSortedEntries();
  }

  isLeafLevelFeedback(entry: FeedbackListEntry): boolean {
    return entry.hasLeafLevel;
  }

  isContainerLevelFeedback(entry: FeedbackListEntry): boolean {
    return entry.hasContainerLevel;
  }

  getCommonPrefix(entry: FeedbackListEntry): string {
    const cacheKey = `${entry.source}→${entry.target}`;
    if (this.commonPrefixCache.has(cacheKey)) {
      return this.commonPrefixCache.get(cacheKey)!;
    }

    const source = entry.source.replace(/:leaf$/, '');
    const target = entry.target.replace(/:leaf$/, '');
    const sourceParts = source.split('.');
    const targetParts = target.split('.');

    const commonParts: string[] = [];
    const minLength = Math.min(sourceParts.length, targetParts.length);

    for (let i = 0; i < minLength - 1; i++) {
      if (sourceParts[i] === targetParts[i]) {
        commonParts.push(sourceParts[i]);
      } else {
        break;
      }
    }

    const result = commonParts.length > 0 ? commonParts.join('.') : '';
    this.commonPrefixCache.set(cacheKey, result);
    return result;
  }

  getSourceSuffix(entry: FeedbackListEntry): string {
    const source = entry.source.replace(/:leaf$/, '');
    const prefix = this.getCommonPrefix(entry);
    if (!prefix) return source;
    return source.slice(prefix.length + 1);
  }

  getTargetSuffix(entry: FeedbackListEntry): string {
    const target = entry.target.replace(/:leaf$/, '');
    const prefix = this.getCommonPrefix(entry);
    if (!prefix) return target;
    return target.slice(prefix.length + 1);
  }

  getSourceTooltip(entry: FeedbackListEntry): string {
    return entry.source.replace(/:leaf$/, '');
  }

  getTargetTooltip(entry: FeedbackListEntry): string {
    return entry.target.replace(/:leaf$/, '');
  }

  private getSortFunction(): (a: FeedbackListEntry, b: FeedbackListEntry) => number {
    switch (this.selectedSort) {
      case SortOption.HIERARCHY_ASC:
        return (a, b) => getHierarchy(a) - getHierarchy(b);
      case SortOption.HIERARCHY_DESC:
        return (a, b) => getHierarchy(b) - getHierarchy(a);
      case SortOption.SOURCE_ASC:
        return (a, b) => a.source.localeCompare(b.source);
      case SortOption.SOURCE_DESC:
        return (a, b) => b.source.localeCompare(a.source);
      case SortOption.TARGET_ASC:
        return (a, b) => a.target.localeCompare(b.target);
      case SortOption.TARGET_DESC:
        return (a, b) => b.target.localeCompare(a.target);
      case SortOption.WEIGHT_ASC:
        return (a, b) => a.weight - b.weight;
      case SortOption.WEIGHT_DESC:
        return (a, b) => b.weight - a.weight;
      case SortOption.TYPE_ASC:
        return (a, b) => this.getTypeString(a).localeCompare(this.getTypeString(b));
      case SortOption.TYPE_DESC:
        return (a, b) => this.getTypeString(b).localeCompare(this.getTypeString(a));
      default:
        return () => 0;
    }
  }

  private getTypeString(entry: FeedbackListEntry): string {
    if (entry.hasLeafLevel && entry.hasContainerLevel) return 'MIXED';
    if (entry.hasLeafLevel) return 'FEEDBACK_LEAF_LEVEL';
    if (entry.hasContainerLevel) return 'FEEDBACK_CONTAINER_LEVEL';
    return 'UNKNOWN';
  }

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

    const overlay = this.elementRef.nativeElement.querySelector('.feedback-edges-overlay');
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

    const overlay = this.elementRef.nativeElement.querySelector('.feedback-edges-overlay');
    if (!overlay) return;

    const deltaX = event.clientX - this.startX;
    const deltaY = event.clientY - this.startY;

    if (this.resizeDirection === 'right' || this.resizeDirection === 'top-right') {
      const newWidth = Math.max(this.minWidth, this.startWidth + deltaX);
      overlay.style.width = `${newWidth}px`;
    }

    if (this.resizeDirection === 'top' || this.resizeDirection === 'top-right') {
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
}
