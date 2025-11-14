import {ComponentFixture, TestBed} from '@angular/core/testing';

import {FilterComponent} from './filter.component';
import {EdgeFilterType} from '../../model/EdgeFilter';
import {Action} from '../../model/Action';

describe('FilterComponent', () => {
  let component: FilterComponent;
  let fixture: ComponentFixture<FilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(FilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getName', () => {
    it('should return "Show All" for ALL filter type', () => {
      // Arrange
      const filterType = EdgeFilterType.ALL;

      // Act
      const result = component.getName(filterType);

      // Assert
      expect(result).toBe("Show All");
    });

    it('should return "Hide All" for NONE filter type', () => {
      // Arrange
      const filterType = EdgeFilterType.NONE;

      // Act
      const result = component.getName(filterType);

      // Assert
      expect(result).toBe("Hide All");
    });

    it('should return "Show only cycles" for CYCLES_ONLY filter type', () => {
      // Arrange
      const filterType = EdgeFilterType.CYCLES_ONLY;

      // Act
      const result = component.getName(filterType);

      // Assert
      expect(result).toBe("Show only cycles");
    });

    it('should return "Show only feedback edges" for FEEDBACK_EDGES_ONLY filter type', () => {
      // Arrange
      const filterType = EdgeFilterType.FEEDBACK_EDGES_ONLY;

      // Act
      const result = component.getName(filterType);

      // Assert
      expect(result).toBe("Show only feedback edges");
    });

    it('should return "Show feedback and twisted edges" for FEEDBACK_EDGES_AND_TWISTED_EDGES filter type', () => {
      // Arrange
      const filterType = EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES;

      // Act
      const result = component.getName(filterType);

      // Assert
      expect(result).toBe("Show feedback and twisted edges");
    });
  });

  describe('onFilterChange', () => {
    it('should emit ChangeFilter action with selected filter value', () => {
      // Arrange
      const selectedFilterType = EdgeFilterType.CYCLES_ONLY;
      const mockSelectElement = document.createElement('select');
      const option = document.createElement('option');
      option.value = selectedFilterType;
      mockSelectElement.appendChild(option);
      mockSelectElement.value = selectedFilterType;
      const mockEvent = new Event('change');
      Object.defineProperty(mockEvent, 'target', {value: mockSelectElement, writable: false});
      let emittedAction: Action | undefined;
      component.filterSelected.subscribe(action => emittedAction = action);

      // Act
      component.onFilterChange(mockEvent);

      // Assert
      expect(emittedAction).toBeInstanceOf(Action.ChangeFilter);
      if (emittedAction instanceof Action.ChangeFilter) {
        expect(emittedAction.edgeFilter).toBe(selectedFilterType);
      }
    });
  });
});
