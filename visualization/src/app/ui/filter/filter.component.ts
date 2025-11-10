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
    EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES,
    EdgeFilterType.FEEDBACK_EDGES_ONLY,
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
      case EdgeFilterType.FEEDBACK_EDGES_ONLY: name = "Show only feedback edges"; break
      case EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES: name = "Show feedback and twisted edges"; break
    }
    return name
  }

  onFilterChange(event: Event) {
    const selectElement = event.target as HTMLSelectElement
    const selectedValue = selectElement.value as EdgeFilterType
    this.filterSelected.emit(new Action.ChangeFilter(selectedValue))
  }


}
