import {Component, EventEmitter, Input, Output} from '@angular/core';
import {NgForOf} from '@angular/common';
import {EdgeFilterType} from '../../model/EdgeFilter';
import {Action} from '../../model/Action';

@Component({
  selector: 'filter',
  imports: [NgForOf],
  templateUrl: './filter.component.html',
  standalone: true,
  styleUrl: './filter.component.css'
})
export class FilterComponent {
  @Output() filterSelected = new EventEmitter<Action>()
  @Input() selectedFilter!: EdgeFilterType

  allEdgeFilterTypes = [
    EdgeFilterType.ALL_FEEDBACK_EDGES,
    EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY,
    EdgeFilterType.CYCLES_ONLY,
    EdgeFilterType.ALL,
    EdgeFilterType.NONE
  ]

  getName(edgeFilterType: EdgeFilterType): string {
    let name
    switch (edgeFilterType) {
      case EdgeFilterType.ALL: name = "Show All"; break
      case EdgeFilterType.NONE: name = "Hide All"; break
      case EdgeFilterType.CYCLES_ONLY: name = "Show only cycles"; break
      case EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY: name = "Show only leaf level feedback edges"; break
      case EdgeFilterType.ALL_FEEDBACK_EDGES: name = "Show all feedback edges"; break
    }
    return name
  }

  onFilterChange(event: Event) {
    const selectElement = event.target as HTMLSelectElement
    const selectedValue = selectElement.value as EdgeFilterType
    this.filterSelected.emit(new Action.ChangeFilter(selectedValue))
  }


}
