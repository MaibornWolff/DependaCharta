import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HiddenNodesListComponent, SortOption} from './hidden-nodes-list.component';
import {GraphNode} from '../../model/GraphNode';
import * as GraphNodeTest from '../../model/GraphNode.spec';

describe('HiddenNodesListComponent', () => {
  let component: HiddenNodesListComponent;
  let fixture: ComponentFixture<HiddenNodesListComponent>;

  const createMockNode = (id: string, children: GraphNode[] = [], parent?: GraphNode): GraphNode => {
    const node = GraphNodeTest.GraphNode.build({id, children, label: id.split('.').pop() || id});
    if (parent) {
      node.parent = parent;
    }
    return node;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HiddenNodesListComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(HiddenNodesListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('hidden nodes display', () => {
    it('should display provided hidden nodes', () => {
      // Given
      const nodes = [createMockNode('com.pkg.ClassA'), createMockNode('com.pkg.ClassB')];

      // When
      component.hiddenNodes = nodes;
      fixture.detectChanges();

      // Then
      expect(component.hiddenNodes.length).toBe(2);
    });

    it('should return empty array when no hidden nodes provided', () => {
      // Given/When
      component.hiddenNodes = [];
      fixture.detectChanges();

      // Then
      expect(component.hiddenNodes.length).toBe(0);
    });
  });

  describe('empty state', () => {
    it('should have empty hidden nodes list when no nodes provided', () => {
      // Given/When
      component.hiddenNodes = [];
      fixture.detectChanges();

      // Then
      expect(component.hasHiddenNodes).toBe(false);
    });

    it('should indicate when there are hidden nodes', () => {
      // Given/When
      component.hiddenNodes = [createMockNode('A')];
      fixture.detectChanges();

      // Then
      expect(component.hasHiddenNodes).toBe(true);
    });
  });

  describe('node count', () => {
    it('should provide correct count of hidden nodes', () => {
      // Given
      const nodes = [createMockNode('A'), createMockNode('B')];

      // When
      component.hiddenNodes = nodes;
      fixture.detectChanges();

      // Then
      expect(component.hiddenNodeCount).toBe(2);
    });
  });

  describe('sorting', () => {
    const parentNode = createMockNode('com.pkg', []);
    const leafA = createMockNode('com.pkg.Apple', [], parentNode);
    const leafZ = createMockNode('com.pkg.deep.Zebra', [], parentNode);
    const containerM = createMockNode('com.other.Mango', [createMockNode('com.other.Mango.child')]);

    const hiddenNodes = [leafZ, leafA, containerM];

    beforeEach(() => {
      component.hiddenNodes = hiddenNodes;
      component.ngOnChanges({
        hiddenNodes: {
          currentValue: hiddenNodes,
          previousValue: [],
          firstChange: true,
          isFirstChange: () => true
        }
      });
      fixture.detectChanges();
    });

    it('should default to alphabetical ascending sort', () => {
      // Then
      expect(component.selectedSort).toBe(SortOption.ALPHABETICAL_ASC);
    });

    it('should sort alphabetically ascending (A-Z)', () => {
      // When
      triggerSortChange(SortOption.ALPHABETICAL_ASC);

      // Then
      const sorted = component.sortedHiddenNodes;
      expect(sorted[0].id).toBe('com.other.Mango');
      expect(sorted[1].id).toBe('com.pkg.Apple');
      expect(sorted[2].id).toBe('com.pkg.deep.Zebra');
    });

    it('should sort alphabetically descending (Z-A)', () => {
      // When
      triggerSortChange(SortOption.ALPHABETICAL_DESC);

      // Then
      const sorted = component.sortedHiddenNodes;
      expect(sorted[0].id).toBe('com.pkg.deep.Zebra');
      expect(sorted[1].id).toBe('com.pkg.Apple');
      expect(sorted[2].id).toBe('com.other.Mango');
    });

    it('should sort by hierarchy ascending (shallow first)', () => {
      // When
      triggerSortChange(SortOption.HIERARCHY_ASC);

      // Then
      const sorted = component.sortedHiddenNodes;
      // com.other.Mango = depth 3, com.pkg.Apple = depth 3, com.pkg.deep.Zebra = depth 4
      expect(sorted[2].id).toBe('com.pkg.deep.Zebra');
    });

    it('should sort by hierarchy descending (deep first)', () => {
      // When
      triggerSortChange(SortOption.HIERARCHY_DESC);

      // Then
      const sorted = component.sortedHiddenNodes;
      expect(sorted[0].id).toBe('com.pkg.deep.Zebra');
    });

    it('should sort by parent ascending (A-Z)', () => {
      // When
      triggerSortChange(SortOption.PARENT_ASC);
      const sorted = component.sortedHiddenNodes;

      // Then — containerM has no parent (''), leafA and leafZ have parent 'com.pkg'
      expect(sorted[0].id).toBe('com.other.Mango');
      // leafA and leafZ share parent 'com.pkg', order among them is unspecified
      const lastTwo = sorted.slice(1).map(n => n.id);
      expect(lastTwo).toContain('com.pkg.Apple');
      expect(lastTwo).toContain('com.pkg.deep.Zebra');
    });

    it('should sort by parent descending (Z-A)', () => {
      // When
      triggerSortChange(SortOption.PARENT_DESC);
      const sorted = component.sortedHiddenNodes;

      // Then — leafA and leafZ (parent 'com.pkg') before containerM (no parent '')
      const firstTwo = sorted.slice(0, 2).map(n => n.id);
      expect(firstTwo).toContain('com.pkg.Apple');
      expect(firstTwo).toContain('com.pkg.deep.Zebra');
      expect(sorted[2].id).toBe('com.other.Mango');
    });

    it('should sort by type ascending (Container before Leaf)', () => {
      // When
      triggerSortChange(SortOption.TYPE_ASC);
      const sorted = component.sortedHiddenNodes;

      // Then — 'Container' < 'Leaf' alphabetically
      expect(component.getNodeType(sorted[0])).toBe('Container');
    });

    it('should sort by type descending (Leaf before Container)', () => {
      // When
      triggerSortChange(SortOption.TYPE_DESC);
      const sorted = component.sortedHiddenNodes;

      // Then
      expect(component.getNodeType(sorted[0])).toBe('Leaf');
      expect(component.getNodeType(sorted[sorted.length - 1])).toBe('Container');
    });

    function triggerSortChange(sortOption: SortOption): void {
      const mockEvent = {target: {value: sortOption}} as unknown as Event;
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

  describe('restore events', () => {
    it('should emit restoreNode event with nodeId and parentNodeId', () => {
      // Given
      const parent = createMockNode('com.pkg');
      const node = createMockNode('com.pkg.ClassA', [], parent);
      component.hiddenNodes = [node];
      fixture.detectChanges();
      spyOn(component.restoreNode, 'emit');

      // When
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);
      component.onRestoreNodeClick(mockEvent, node);

      // Then
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(component.restoreNode.emit).toHaveBeenCalledWith({
        nodeId: 'com.pkg.ClassA',
        parentNodeId: 'com.pkg'
      });
    });

    it('should emit restoreNode with empty parentNodeId for root nodes', () => {
      // Given
      const node = createMockNode('root');
      component.hiddenNodes = [node];
      fixture.detectChanges();
      spyOn(component.restoreNode, 'emit');

      // When
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);
      component.onRestoreNodeClick(mockEvent, node);

      // Then
      expect(component.restoreNode.emit).toHaveBeenCalledWith({
        nodeId: 'root',
        parentNodeId: ''
      });
    });

    it('should emit restoreAllHidden event when restore all button is clicked', () => {
      // Given
      fixture.detectChanges();
      spyOn(component.restoreAllHidden, 'emit');

      // When
      component.onRestoreAllClick();

      // Then
      expect(component.restoreAllHidden.emit).toHaveBeenCalled();
    });

    it('should collapse panel when restore all is clicked', () => {
      // Given
      component.isExpanded = true;
      fixture.detectChanges();

      // When
      component.onRestoreAllClick();

      // Then
      expect(component.isExpanded).toBe(false);
    });

    it('should not toggle header when Enter is pressed on restore all button', () => {
      // Given
      component.hiddenNodes = [createMockNode('com.pkg.ClassA')];
      component.isExpanded = true;
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('.restore-all-button');

      // When
      button.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
      fixture.detectChanges();

      // Then — panel should still be expanded (Enter on button should not trigger header toggle)
      expect(component.isExpanded).toBe(true);
    });

    it('should not toggle header when Space is pressed on restore all button', () => {
      // Given
      component.hiddenNodes = [createMockNode('com.pkg.ClassA')];
      component.isExpanded = true;
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('.restore-all-button');

      // When
      button.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));
      fixture.detectChanges();

      // Then — panel should still be expanded (Space on button should not trigger header toggle)
      expect(component.isExpanded).toBe(true);
    });
  });

  describe('node label formatting', () => {
    it('should strip :leaf suffix from path', () => {
      // Given
      const node = createMockNode('com.example.MyClass:leaf');

      // When
      const path = component.getNodePath(node);

      // Then
      expect(path).toBe('com.example.MyClass');
    });

    it('should return full path when no :leaf suffix', () => {
      // Given
      const node = createMockNode('com.example.MyPackage');

      // When
      const path = component.getNodePath(node);

      // Then
      expect(path).toBe('com.example.MyPackage');
    });

    it('should extract path prefix up to last segment', () => {
      // Given
      const node = createMockNode('com.example.domain.MyClass');

      // When
      const prefix = component.getPathPrefix(node);

      // Then
      expect(prefix).toBe('com.example.domain.');
    });

    it('should return empty prefix for single-segment path', () => {
      // Given
      const node = createMockNode('root');

      // When
      const prefix = component.getPathPrefix(node);

      // Then
      expect(prefix).toBe('');
    });

    it('should extract last segment as suffix', () => {
      // Given
      const node = createMockNode('com.example.domain.MyClass');

      // When
      const suffix = component.getPathSuffix(node);

      // Then
      expect(suffix).toBe('MyClass');
    });

    it('should return full path as suffix for single-segment path', () => {
      // Given
      const node = createMockNode('root');

      // When
      const suffix = component.getPathSuffix(node);

      // Then
      expect(suffix).toBe('root');
    });
  });

  describe('node type detection', () => {
    it('should identify leaf nodes', () => {
      // Given
      const node = createMockNode('com.pkg.ClassA');

      // Then
      expect(component.getNodeType(node)).toBe('Leaf');
      expect(component.isLeaf(node)).toBe(true);
    });

    it('should identify container nodes', () => {
      // Given
      const child = createMockNode('com.pkg.child');
      const node = createMockNode('com.pkg', [child]);

      // Then
      expect(component.getNodeType(node)).toBe('Container');
      expect(component.isLeaf(node)).toBe(false);
    });
  });

  describe('trackByNode', () => {
    it('should return node id as key', () => {
      // Given
      const node = createMockNode('com.pkg.ClassA');

      // When
      const key = component.trackByNode(0, node);

      // Then
      expect(key).toBe('com.pkg.ClassA');
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
      component.onResizeStart(mockEvent, 'horizontal');

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
      component.onResizeStart(mockEvent, 'vertical');

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

  describe('keyboard navigation', () => {
    beforeEach(() => {
      const nodes = [
        createMockNode('org.foo.ClassA'),
        createMockNode('net.baz.ClassC')
      ];
      component.hiddenNodes = nodes;
      component.isExpanded = true;
      component.ngOnChanges({
        hiddenNodes: {
          currentValue: nodes,
          previousValue: [],
          firstChange: true,
          isFirstChange: () => true
        }
      });
      fixture.detectChanges();
    });

    function createKeyboardEvent(key: string, target?: HTMLElement): KeyboardEvent {
      const event = new KeyboardEvent('keydown', {key, bubbles: true});
      if (target) {
        Object.defineProperty(event, 'target', {value: target});
      }
      return event;
    }

    describe('onArrowDown', () => {
      it('should focus first entry when no focus exists', () => {
        // Given
        component.focusedEntryKey = null;
        const event = createKeyboardEvent('ArrowDown');

        // When
        component.onArrowDown(event);

        // Then
        expect(component.focusedEntryKey).not.toBeNull();
      });

      it('should move focus to next entry', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        component.focusedEntryKey = sorted[0].id;
        const event = createKeyboardEvent('ArrowDown');

        // When
        component.onArrowDown(event);

        // Then
        expect(component.focusedEntryKey).toBe(sorted[1].id);
      });

      it('should not move past last entry', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        const lastId = sorted[sorted.length - 1].id;
        component.focusedEntryKey = lastId;
        const event = createKeyboardEvent('ArrowDown');

        // When
        component.onArrowDown(event);

        // Then
        expect(component.focusedEntryKey).toBe(lastId);
      });

      it('should not trigger when collapsed', () => {
        // Given
        component.isExpanded = false;
        component.focusedEntryKey = null;
        const event = createKeyboardEvent('ArrowDown');

        // When
        component.onArrowDown(event);

        // Then
        expect(component.focusedEntryKey).toBeNull();
      });

      it('should not trigger when input is focused', () => {
        // Given
        const inputElement = document.createElement('input');
        document.body.appendChild(inputElement);
        component.focusedEntryKey = null;
        const event = createKeyboardEvent('ArrowDown', inputElement);

        // When
        component.onArrowDown(event);

        // Then
        expect(component.focusedEntryKey).toBeNull();
        document.body.removeChild(inputElement);
      });

      it('should not trigger when no hidden nodes', () => {
        // Given
        component.hiddenNodes = [];
        fixture.detectChanges();
        const event = createKeyboardEvent('ArrowDown');

        // When
        component.onArrowDown(event);

        // Then
        expect(component.focusedEntryKey).toBeNull();
      });
    });

    describe('onArrowUp', () => {
      it('should focus last entry when no focus exists', () => {
        // Given
        component.focusedEntryKey = null;
        const event = createKeyboardEvent('ArrowUp');

        // When
        component.onArrowUp(event);

        // Then
        const sorted = component.sortedHiddenNodes;
        expect(component.focusedEntryKey!).toBe(sorted[sorted.length - 1].id);
      });

      it('should move focus to previous entry', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        component.focusedEntryKey = sorted[1].id;
        const event = createKeyboardEvent('ArrowUp');

        // When
        component.onArrowUp(event);

        // Then
        expect(component.focusedEntryKey).toBe(sorted[0].id);
      });

      it('should not move before first entry', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        component.focusedEntryKey = sorted[0].id;
        const event = createKeyboardEvent('ArrowUp');

        // When
        component.onArrowUp(event);

        // Then
        expect(component.focusedEntryKey).toBe(sorted[0].id);
      });

      it('should not trigger when collapsed', () => {
        // Given
        component.isExpanded = false;
        component.focusedEntryKey = null;
        const event = createKeyboardEvent('ArrowUp');

        // When
        component.onArrowUp(event);

        // Then
        expect(component.focusedEntryKey).toBeNull();
      });
    });

    describe('onCycleSort', () => {
      it('should cycle to next sort option', () => {
        // Given
        component.selectedSort = SortOption.ALPHABETICAL_ASC;
        const event = createKeyboardEvent('s');

        // When
        component.onCycleSort(event);

        // Then
        expect(component.selectedSort).toBe(SortOption.ALPHABETICAL_DESC);
      });

      it('should wrap around to first option', () => {
        // Given
        component.selectedSort = SortOption.TYPE_DESC;
        const event = createKeyboardEvent('s');

        // When
        component.onCycleSort(event);

        // Then
        expect(component.selectedSort).toBe(SortOption.ALPHABETICAL_ASC);
      });

      it('should not trigger when collapsed', () => {
        // Given
        component.isExpanded = false;
        component.selectedSort = SortOption.ALPHABETICAL_ASC;
        const event = createKeyboardEvent('s');

        // When
        component.onCycleSort(event);

        // Then
        expect(component.selectedSort).toBe(SortOption.ALPHABETICAL_ASC);
      });

      it('should not trigger when no hidden nodes', () => {
        // Given
        component.hiddenNodes = [];
        fixture.detectChanges();
        component.selectedSort = SortOption.ALPHABETICAL_ASC;
        const event = createKeyboardEvent('s');

        // When
        component.onCycleSort(event);

        // Then
        expect(component.selectedSort).toBe(SortOption.ALPHABETICAL_ASC);
      });
    });

    describe('isFocused', () => {
      it('should return true when node is focused', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        component.focusedEntryKey = sorted[0].id;

        // Then
        expect(component.isFocused(sorted[0])).toBe(true);
      });

      it('should return false when node is not focused', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        component.focusedEntryKey = sorted[1].id;

        // Then
        expect(component.isFocused(sorted[0])).toBe(false);
      });

      it('should return false when no node is focused', () => {
        // Given
        const sorted = component.sortedHiddenNodes;
        component.focusedEntryKey = null;

        // Then
        expect(component.isFocused(sorted[0])).toBe(false);
      });
    });

    describe('moveFocus edge cases', () => {
      it('should reset focus to first node when focused key not found', () => {
        // Given
        component.focusedEntryKey = 'NonExistent';
        const event = createKeyboardEvent('ArrowDown');

        // When
        component.onArrowDown(event);

        // Then
        const sorted = component.sortedHiddenNodes;
        expect(component.focusedEntryKey).toBe(sorted[0].id);
      });
    });
  });

  describe('getSortFunction default case', () => {
    it('should return 0 for unknown sort option', () => {
      // Given
      component.selectedSort = 'unknownSort' as SortOption;
      const nodes = [createMockNode('A'), createMockNode('B')];
      component.hiddenNodes = nodes;
      component.ngOnChanges({
        hiddenNodes: {
          currentValue: nodes,
          previousValue: [],
          firstChange: true,
          isFirstChange: () => true
        }
      });
      fixture.detectChanges();

      // Then
      expect(component.sortedHiddenNodes.length).toBe(2);
    });
  });
});
