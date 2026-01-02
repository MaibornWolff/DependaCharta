import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FeedbackEdgesListComponent, SortOption} from './feedback-edges-list.component';
import {ShallowEdge} from '../../model/Edge';

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
});
