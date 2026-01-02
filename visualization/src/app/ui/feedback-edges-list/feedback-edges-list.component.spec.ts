import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FeedbackEdgesListComponent, SortOption} from './feedback-edges-list.component';
import {FeedbackEdgeGroup, ShallowEdge} from '../../model/Edge';

describe('FeedbackEdgesListComponent', () => {
  let component: FeedbackEdgesListComponent;
  let fixture: ComponentFixture<FeedbackEdgesListComponent>;

  const createMockFeedbackEdge = (
    sourceId: string,
    targetId: string,
    weight: number,
    isCyclic: boolean
  ): ShallowEdge => new ShallowEdge(
    sourceId,
    targetId,
    `${sourceId}->${targetId}`,
    weight,
    isCyclic,
    true, // isPointingUpwards is always true for feedback edges
    isCyclic ? 'FEEDBACK_LEAF_LEVEL' : 'FEEDBACK_CONTAINER_LEVEL'
  );

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeedbackEdgesListComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(FeedbackEdgesListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('feedback edges display', () => {
    it('should display provided feedback edges', () => {
      // Given
      const feedbackEdges = [
        createMockFeedbackEdge('A', 'B', 1, false),
        createMockFeedbackEdge('E', 'F', 3, true)
      ];

      // When
      component.feedbackEdges = feedbackEdges;
      fixture.detectChanges();

      // Then
      expect(component.feedbackEdges.length).toBe(2);
    });

    it('should return empty array when no feedback edges provided', () => {
      // Given/When
      component.feedbackEdges = [];
      fixture.detectChanges();

      // Then
      expect(component.feedbackEdges.length).toBe(0);
    });
  });

  describe('sorting', () => {
    const feedbackEdges = [
      createMockFeedbackEdge('Zebra', 'Apple', 5, false),
      createMockFeedbackEdge('Apple', 'Zebra', 1, true),
      createMockFeedbackEdge('Mango', 'Banana', 3, false)
    ];

    beforeEach(() => {
      component.feedbackEdges = feedbackEdges;
      fixture.detectChanges();
    });

    it('should default to hierarchy ascending sort', () => {
      // Then
      expect(component.selectedSort).toBe(SortOption.HIERARCHY_ASC);
    });

    it('should sort by source ascending (A-Z)', () => {
      // When
      triggerSortChange(SortOption.SOURCE_ASC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect(sorted[0].source).toBe('Apple');
      expect(sorted[1].source).toBe('Mango');
      expect(sorted[2].source).toBe('Zebra');
    });

    it('should sort by source descending (Z-A)', () => {
      // When
      triggerSortChange(SortOption.SOURCE_DESC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect(sorted[0].source).toBe('Zebra');
      expect(sorted[1].source).toBe('Mango');
      expect(sorted[2].source).toBe('Apple');
    });

    it('should sort by target ascending (A-Z)', () => {
      // When
      triggerSortChange(SortOption.TARGET_ASC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect(sorted[0].target).toBe('Apple');
      expect(sorted[1].target).toBe('Banana');
      expect(sorted[2].target).toBe('Zebra');
    });

    it('should sort by target descending (Z-A)', () => {
      // When
      triggerSortChange(SortOption.TARGET_DESC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect(sorted[0].target).toBe('Zebra');
      expect(sorted[1].target).toBe('Banana');
      expect(sorted[2].target).toBe('Apple');
    });

    it('should sort by weight ascending (Low-High)', () => {
      // When
      triggerSortChange(SortOption.WEIGHT_ASC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect(sorted[0].weight).toBe(1);
      expect(sorted[1].weight).toBe(3);
      expect(sorted[2].weight).toBe(5);
    });

    it('should sort by weight descending (High-Low)', () => {
      // When
      triggerSortChange(SortOption.WEIGHT_DESC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect(sorted[0].weight).toBe(5);
      expect(sorted[1].weight).toBe(3);
      expect(sorted[2].weight).toBe(1);
    });

    it('should sort by type ascending (A-Z)', () => {
      // When
      triggerSortChange(SortOption.TYPE_ASC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect((sorted[0] as ShallowEdge).type).toBe('FEEDBACK_CONTAINER_LEVEL');
      expect((sorted[1] as ShallowEdge).type).toBe('FEEDBACK_CONTAINER_LEVEL');
      expect((sorted[2] as ShallowEdge).type).toBe('FEEDBACK_LEAF_LEVEL');
    });

    it('should sort by type descending (Z-A)', () => {
      // When
      triggerSortChange(SortOption.TYPE_DESC);
      const sorted = component.sortedFeedbackEntries;

      // Then
      expect((sorted[0] as ShallowEdge).type).toBe('FEEDBACK_LEAF_LEVEL');
    });

    describe('hierarchy sorting', () => {
      const hierarchyEdges = [
        createMockFeedbackEdge('com.example.domain.service.MyService', 'com.example.domain.model.MyModel', 1, false), // hierarchy 4
        createMockFeedbackEdge('com.example.AppConfig', 'com.example.domain.MyClass', 2, false), // hierarchy 2
        createMockFeedbackEdge('infrastructure', 'domain', 3, false) // hierarchy 0
      ];

      beforeEach(() => {
        component.feedbackEdges = hierarchyEdges;
        fixture.detectChanges();
      });

      it('should sort by hierarchy ascending (low hierarchy first = top-level)', () => {
        // When
        triggerSortChange(SortOption.HIERARCHY_ASC);
        const sorted = component.sortedFeedbackEntries;

        // Then - hierarchy 0, 2, 4
        expect(sorted[0].source).toBe('infrastructure');
        expect(sorted[1].source).toBe('com.example.AppConfig');
        expect(sorted[2].source).toBe('com.example.domain.service.MyService');
      });

      it('should sort by hierarchy descending (high hierarchy first = deep nested)', () => {
        // When
        triggerSortChange(SortOption.HIERARCHY_DESC);
        const sorted = component.sortedFeedbackEntries;

        // Then - hierarchy 4, 2, 0
        expect(sorted[0].source).toBe('com.example.domain.service.MyService');
        expect(sorted[1].source).toBe('com.example.AppConfig');
        expect(sorted[2].source).toBe('infrastructure');
      });
    });

    function triggerSortChange(sortOption: SortOption): void {
      const mockEvent = { target: { value: sortOption } } as unknown as Event;
      component.onSortChange(mockEvent);
    }
  });

  describe('expand/collapse behavior', () => {
    it('should start collapsed by default', () => {
      // When
      fixture.detectChanges();

      // Then
      expect(component.isExpanded).toBe(false);
    });

    it('should toggle expanded state when toggle is called', () => {
      // Given
      fixture.detectChanges();
      expect(component.isExpanded).toBe(false);

      // When
      component.toggleExpanded();

      // Then
      expect(component.isExpanded).toBe(true);

      // When
      component.toggleExpanded();

      // Then
      expect(component.isExpanded).toBe(false);
    });
  });

  describe('edge click events', () => {
    it('should emit edgeClicked event when an edge is clicked', () => {
      // Given
      const edge = createMockFeedbackEdge('A', 'B', 1, false);
      component.feedbackEdges = [edge];
      fixture.detectChanges();
      spyOn(component.edgeClicked, 'emit');

      // When
      component.onEntryClick(edge);

      // Then
      expect(component.edgeClicked.emit).toHaveBeenCalledWith(edge);
    });
  });

  describe('empty state', () => {
    it('should have empty feedback edges list when no edges provided', () => {
      // Given/When
      component.feedbackEdges = [];
      fixture.detectChanges();

      // Then
      expect(component.feedbackEdges.length).toBe(0);
      expect(component.hasFeedbackEdges).toBe(false);
    });

    it('should indicate when there are feedback edges', () => {
      // Given/When
      component.feedbackEdges = [createMockFeedbackEdge('A', 'B', 1, false)];
      fixture.detectChanges();

      // Then
      expect(component.hasFeedbackEdges).toBe(true);
    });
  });

  describe('edge count', () => {
    it('should provide correct count of feedback edges', () => {
      // Given
      const feedbackEdges = [
        createMockFeedbackEdge('A', 'B', 1, false),
        createMockFeedbackEdge('C', 'D', 2, false)
      ];

      // When
      component.feedbackEdges = feedbackEdges;
      fixture.detectChanges();

      // Then
      expect(component.feedbackEdgeCount).toBe(2);
    });
  });

  describe('edge type detection', () => {
    it('should correctly identify leaf level feedback edges', () => {
      // Given
      const edge = createMockFeedbackEdge('A', 'B', 1, true);

      // Then
      expect(component.isLeafLevelFeedback(edge)).toBe(true);
    });

    it('should correctly identify container level feedback edges', () => {
      // Given
      const edge = createMockFeedbackEdge('A', 'B', 1, false);

      // Then
      expect(component.isContainerLevelFeedback(edge)).toBe(true);
    });
  });

  describe('node label formatting', () => {
    it('should strip :leaf suffix from source tooltip', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'com.example.MyClass:leaf',
        'com.example.OtherClass:leaf',
        1,
        false
      );

      // When
      const sourceTooltip = component.getSourceTooltip(edge);

      // Then
      expect(sourceTooltip).toBe('com.example.MyClass');
    });

    it('should strip :leaf suffix from target tooltip', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'com.example.MyClass:leaf',
        'com.example.OtherClass:leaf',
        1,
        false
      );

      // When
      const targetTooltip = component.getTargetTooltip(edge);

      // Then
      expect(targetTooltip).toBe('com.example.OtherClass');
    });

    it('should extract common prefix from source and target', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'com.example.domain.model.ClassA:leaf',
        'com.example.domain.service.ClassB:leaf',
        1,
        false
      );

      // When
      const prefix = component.getCommonPrefix(edge);

      // Then
      expect(prefix).toBe('com.example.domain');
    });

    it('should return source suffix without common prefix', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'com.example.domain.model.ClassA:leaf',
        'com.example.domain.service.ClassB:leaf',
        1,
        false
      );

      // When
      const suffix = component.getSourceSuffix(edge);

      // Then
      expect(suffix).toBe('model.ClassA');
    });

    it('should return target suffix without common prefix', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'com.example.domain.model.ClassA:leaf',
        'com.example.domain.service.ClassB:leaf',
        1,
        false
      );

      // When
      const suffix = component.getTargetSuffix(edge);

      // Then
      expect(suffix).toBe('service.ClassB');
    });

    it('should return empty prefix when no common path exists', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'org.foo.ClassA',
        'com.bar.ClassB',
        1,
        false
      );

      // When
      const prefix = component.getCommonPrefix(edge);

      // Then
      expect(prefix).toBe('');
    });

    it('should return full path as suffix when no common prefix exists', () => {
      // Given
      const edge = createMockFeedbackEdge(
        'org.foo.ClassA',
        'com.bar.ClassB',
        1,
        false
      );

      // When
      const sourceSuffix = component.getSourceSuffix(edge);
      const targetSuffix = component.getTargetSuffix(edge);

      // Then
      expect(sourceSuffix).toBe('org.foo.ClassA');
      expect(targetSuffix).toBe('com.bar.ClassB');
    });
  });

  describe('group expansion', () => {
    it('should emit groupClicked event when a group entry is clicked', () => {
      // Given
      const childEdge = createMockFeedbackEdge('com.pkg.A', 'com.pkg.B', 1, false);
      const group = new FeedbackEdgeGroup('com.pkg', 'com.pkg', [childEdge]);
      fixture.detectChanges();
      spyOn(component.groupClicked, 'emit');

      // When
      component.onEntryClick(group);

      // Then
      expect(component.groupClicked.emit).toHaveBeenCalledWith(group);
    });

    it('should toggle group expanded state', () => {
      // Given
      const childEdge = createMockFeedbackEdge('com.pkg.A', 'com.pkg.B', 1, false);
      const group = new FeedbackEdgeGroup('com.pkg', 'com.pkg', [childEdge]);
      fixture.detectChanges();

      // When/Then - initially not expanded
      expect(component.isGroupExpanded(group)).toBe(false);

      // When - expand
      component.toggleGroupExpanded(group);

      // Then
      expect(component.isGroupExpanded(group)).toBe(true);

      // When - collapse
      component.toggleGroupExpanded(group);

      // Then
      expect(component.isGroupExpanded(group)).toBe(false);
    });

    it('should stop event propagation and toggle group on chevron click', () => {
      // Given
      const childEdge = createMockFeedbackEdge('com.pkg.A', 'com.pkg.B', 1, false);
      const group = new FeedbackEdgeGroup('com.pkg', 'com.pkg', [childEdge]);
      fixture.detectChanges();
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);

      // When
      component.onChevronClick(mockEvent, group);

      // Then
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(component.isGroupExpanded(group)).toBe(true);
    });

    it('should toggle group on prefix click for group entries', () => {
      // Given
      const childEdge = createMockFeedbackEdge('com.pkg.A', 'com.pkg.B', 1, false);
      const group = new FeedbackEdgeGroup('com.pkg', 'com.pkg', [childEdge]);
      fixture.detectChanges();
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);

      // When
      component.onPrefixClick(mockEvent, group);

      // Then
      expect(component.isGroupExpanded(group)).toBe(true);
    });

    it('should not toggle on prefix click for non-group entries', () => {
      // Given
      const edge = createMockFeedbackEdge('A', 'B', 1, false);
      fixture.detectChanges();
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);

      // When
      component.onPrefixClick(mockEvent, edge);

      // Then - no error thrown, nothing happens
      expect(component.isGroupExpanded(edge)).toBe(false);
    });
  });

  describe('handleEntryClick', () => {
    it('should delegate to onEntryClick', () => {
      // Given
      const edge = createMockFeedbackEdge('A', 'B', 1, false);
      fixture.detectChanges();
      spyOn(component, 'onEntryClick');

      // When
      component.handleEntryClick(edge);

      // Then
      expect(component.onEntryClick).toHaveBeenCalledWith(edge);
    });
  });

  describe('trackByEntry', () => {
    it('should return unique key for entry', () => {
      // Given
      const edge = createMockFeedbackEdge('Source', 'Target', 1, false);

      // When
      const key = component.trackByEntry(0, edge);

      // Then
      expect(key).toBe('Source→Target');
    });
  });

  describe('resize functionality', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.isExpanded = true;
      fixture.detectChanges();
    });

    it('should initialize resize state on resize start', () => {
      // Given
      const mockEvent = {
        preventDefault: jasmine.createSpy('preventDefault'),
        stopPropagation: jasmine.createSpy('stopPropagation'),
        clientX: 100,
        clientY: 200
      } as unknown as MouseEvent;

      // When
      component.onResizeStart(mockEvent, 'right');

      // Then
      expect(mockEvent.preventDefault).toHaveBeenCalled();
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });

    it('should add document event listeners on resize start', () => {
      // Given
      const mockEvent = {
        preventDefault: jasmine.createSpy('preventDefault'),
        stopPropagation: jasmine.createSpy('stopPropagation'),
        clientX: 100,
        clientY: 200
      } as unknown as MouseEvent;
      spyOn(document, 'addEventListener');

      // When
      component.onResizeStart(mockEvent, 'top');

      // Then
      expect(document.addEventListener).toHaveBeenCalledWith('mousemove', jasmine.any(Function));
      expect(document.addEventListener).toHaveBeenCalledWith('mouseup', jasmine.any(Function));
    });

    it('should remove document event listeners on destroy', () => {
      // Given
      spyOn(document, 'removeEventListener');

      // When
      component.ngOnDestroy();

      // Then
      expect(document.removeEventListener).toHaveBeenCalledWith('mousemove', jasmine.any(Function));
      expect(document.removeEventListener).toHaveBeenCalledWith('mouseup', jasmine.any(Function));
    });
  });

  describe('sorting with groups', () => {
    it('should sort group children recursively', () => {
      // Given - Create edges that will be grouped together
      const edge1 = createMockFeedbackEdge('com.pkg.service.Z', 'com.pkg.model.A', 5, false);
      const edge2 = createMockFeedbackEdge('com.pkg.service.A', 'com.pkg.model.B', 1, false);
      component.feedbackEdges = [edge1, edge2];
      fixture.detectChanges();

      // When - Sort by source ascending
      const mockEvent = { target: { value: SortOption.SOURCE_ASC } } as unknown as Event;
      component.onSortChange(mockEvent);

      // Then - Entries should be sorted
      const sorted = component.sortedFeedbackEntries;
      expect(sorted.length).toBeGreaterThan(0);
    });
  });
});
