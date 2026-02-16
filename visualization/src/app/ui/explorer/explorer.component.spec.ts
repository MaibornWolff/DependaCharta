import {ComponentFixture, TestBed, fakeAsync, tick} from '@angular/core/testing';
import {ExplorerComponent} from './explorer.component';
import {GraphNode} from '../../model/GraphNode';
import * as GraphNodeTest from '../../model/GraphNode.spec';

describe('ExplorerComponent', () => {
  let component: ExplorerComponent;
  let fixture: ComponentFixture<ExplorerComponent>;

  const createMockNode = (id: string, children: GraphNode[] = [], parent?: GraphNode): GraphNode => {
    const node = GraphNodeTest.GraphNode.build({id, children, label: id.split('.').pop() || id});
    if (parent) {
      node.parent = parent;
    }
    children.forEach(child => child.parent = node);
    return node;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExplorerComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ExplorerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('tree rendering', () => {
    it('should display root nodes', () => {
      // Given
      const root1 = createMockNode('com.pkg1');
      const root2 = createMockNode('com.pkg2');

      // When
      component.rootNodes = [root1, root2];
      component.ngOnChanges({
        rootNodes: {currentValue: [root1, root2], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      fixture.detectChanges();

      // Then
      expect(component.filteredRootNodes.length).toBe(2);
    });

    it('should identify container nodes with visible children', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);

      // When
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      fixture.detectChanges();

      // Then
      expect(component.isContainer(container)).toBe(true);
      expect(component.isContainer(child)).toBe(false);
    });

    it('should return visible children for a node', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);

      // When
      component.hiddenNodeIds = [];

      // Then
      expect(component.getVisibleChildren(container).length).toBe(2);
    });

    it('should return node label', () => {
      // Given
      const node = createMockNode('com.example.MyClass');
      node.label = 'MyClass';

      // Then
      expect(component.getNodeLabel(node)).toBe('MyClass');
    });

    it('should fall back to last segment of id when label is empty', () => {
      // Given
      const node = createMockNode('com.example.MyClass');
      node.label = '';

      // Then
      expect(component.getNodeLabel(node)).toBe('MyClass');
    });

    it('should strip :leaf suffix from fallback label', () => {
      // Given
      const node = createMockNode('com.example.MyClass:leaf');
      node.label = '';

      // Then
      expect(component.getNodeLabel(node)).toBe('MyClass');
    });
  });

  describe('expand/collapse tree nodes', () => {
    it('should start with all tree nodes collapsed', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);

      // When
      component.rootNodes = [container];
      fixture.detectChanges();

      // Then
      expect(component.isTreeNodeExpanded(container)).toBe(false);
    });

    it('should expand a tree node on toggle', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      fixture.detectChanges();

      // When
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);
      component.toggleTreeNode(mockEvent, container);

      // Then
      expect(component.isTreeNodeExpanded(container)).toBe(true);
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });

    it('should collapse an expanded tree node on toggle', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      fixture.detectChanges();

      // When
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);
      component.toggleTreeNode(mockEvent, container);
      component.toggleTreeNode(mockEvent, container);

      // Then
      expect(component.isTreeNodeExpanded(container)).toBe(false);
    });
  });

  describe('hidden node filtering', () => {
    it('should filter out hidden root nodes', () => {
      // Given
      const root1 = createMockNode('pkg1');
      const root2 = createMockNode('pkg2');

      // When
      component.rootNodes = [root1, root2];
      component.hiddenNodeIds = ['pkg1'];
      component.ngOnChanges({
        hiddenNodeIds: {currentValue: ['pkg1'], previousValue: [], firstChange: false, isFirstChange: () => false}
      });
      fixture.detectChanges();

      // Then
      expect(component.filteredRootNodes.length).toBe(1);
      expect(component.filteredRootNodes[0].id).toBe('pkg2');
    });

    it('should filter out hidden children from visible children list', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);

      // When
      component.hiddenNodeIds = ['pkg.ClassA'];

      // Then
      const visibleChildren = component.getVisibleChildren(container);
      expect(visibleChildren.length).toBe(1);
      expect(visibleChildren[0].id).toBe('pkg.ClassB');
    });

    it('should treat container as leaf when all children are hidden', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);

      // When
      component.hiddenNodeIds = ['pkg.ClassA'];

      // Then
      expect(component.isContainer(container)).toBe(false);
    });
  });

  describe('panel expand/collapse', () => {
    it('should start collapsed by default', () => {
      // When
      fixture.detectChanges();

      // Then
      expect(component.isExpanded).toBe(false);
    });

    it('should toggle expanded state', () => {
      // Given
      fixture.detectChanges();

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

  describe('output events', () => {
    it('should emit navigateToNode on row click', () => {
      // Given
      const node = createMockNode('com.pkg.ClassA');
      spyOn(component.navigateToNode, 'emit');

      // When
      component.onRowClick(node);

      // Then
      expect(component.navigateToNode.emit).toHaveBeenCalledWith('com.pkg.ClassA');
    });

    it('should emit hoverNode on row mouse enter', () => {
      // Given
      const node = createMockNode('com.pkg.ClassA');
      spyOn(component.hoverNode, 'emit');

      // When
      component.onRowMouseEnter(node);

      // Then
      expect(component.hoverNode.emit).toHaveBeenCalledWith('com.pkg.ClassA');
    });

    it('should emit unhoverNode on row mouse leave', () => {
      // Given
      spyOn(component.unhoverNode, 'emit');

      // When
      component.onRowMouseLeave();

      // Then
      expect(component.unhoverNode.emit).toHaveBeenCalled();
    });

    it('should emit hideNode on hide button click', () => {
      // Given
      const node = createMockNode('com.pkg.ClassA');
      spyOn(component.hideNode, 'emit');
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);

      // When
      component.onHideClick(mockEvent, node);

      // Then
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(component.hideNode.emit).toHaveBeenCalledWith('com.pkg.ClassA');
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

  describe('search filtering', () => {
    it('should show normal tree when search query is empty', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      fixture.detectChanges();

      // When
      component.searchQuery = '';
      component.applySearch();

      // Then
      expect(component.isSearchActive).toBe(false);
      expect(component.searchResults).toEqual([]);
    });

    it('should show flat search results when query is active', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });

      // When
      component.searchQuery = 'ClassA';
      component.applySearch();

      // Then
      expect(component.isSearchActive).toBe(true);
      expect(component.searchResults.map(n => n.id)).toEqual(['pkg.ClassA']);
    });

    it('should match substring against full node id', () => {
      // Given
      const leaf = createMockNode('com.example.services.UserService');
      const pkg = createMockNode('com.example.services', [leaf]);
      const root = createMockNode('com.example', [pkg]);
      component.rootNodes = [root];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [root], previousValue: [], firstChange: true, isFirstChange: () => true}
      });

      // When
      component.searchQuery = 'User';
      component.applySearch();

      // Then
      expect(component.searchResults.map(n => n.id)).toEqual([
        'com.example.services.UserService'
      ]);
    });

    it('should exclude hidden nodes from search results', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);
      component.rootNodes = [container];
      component.hiddenNodeIds = ['pkg.ClassA'];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true},
        hiddenNodeIds: {currentValue: ['pkg.ClassA'], previousValue: [], firstChange: false, isFirstChange: () => false}
      });

      // When
      component.searchQuery = 'Class';
      component.applySearch();

      // Then
      expect(component.searchResults.map(n => n.id)).toEqual(['pkg.ClassB']);
    });

    it('should clear search results when query is cleared', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      component.searchQuery = 'ClassA';
      component.applySearch();

      // When
      component.clearSearch();

      // Then
      expect(component.searchQuery).toBe('');
      expect(component.isSearchActive).toBe(false);
      expect(component.searchResults).toEqual([]);
    });

    it('should reapply search when rootNodes or hiddenNodeIds change', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      component.searchQuery = 'Class';
      component.applySearch();
      expect(component.searchResults.length).toBe(2);

      // When - hide a node
      component.hiddenNodeIds = ['pkg.ClassA'];
      component.ngOnChanges({
        hiddenNodeIds: {currentValue: ['pkg.ClassA'], previousValue: [], firstChange: false, isFirstChange: () => false}
      });

      // Then
      expect(component.searchResults.map(n => n.id)).toEqual(['pkg.ClassB']);
    });

    it('should debounce search input', fakeAsync(() => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });

      // When
      component.onSearchInput('ClassA');
      expect(component.searchResults).toEqual([]); // not yet applied

      // Then - after debounce
      tick(200);
      expect(component.searchResults.map(n => n.id)).toEqual(['pkg.ClassA']);
    }));
  });

  describe('hide all shown matches', () => {
    it('should emit hideNodes with all matched node ids', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      component.searchQuery = 'Class';
      component.applySearch();
      spyOn(component.hideNodes, 'emit');

      // When
      component.onHideAllShownClick();

      // Then
      expect(component.hideNodes.emit).toHaveBeenCalledWith(['pkg.ClassA', 'pkg.ClassB']);
    });

    it('should deduplicate: only emit parent when parent and child both match', () => {
      // Given
      const leaf = createMockNode('com.example.services.UserService');
      const pkg = createMockNode('com.example.services', [leaf]);
      const root = createMockNode('com.example', [pkg]);
      component.rootNodes = [root];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [root], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      component.searchQuery = 'serv';
      component.applySearch();
      // Both 'com.example.services' and 'com.example.services.UserService' contain 'serv'
      spyOn(component.hideNodes, 'emit');

      // When
      component.onHideAllShownClick();

      // Then
      expect(component.hideNodes.emit).toHaveBeenCalledWith(['com.example.services']);
    });

    it('should clear search after hiding all shown', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      component.searchQuery = 'ClassA';
      component.applySearch();
      spyOn(component.hideNodes, 'emit');

      // When
      component.onHideAllShownClick();

      // Then
      expect(component.searchQuery).toBe('');
      expect(component.isSearchActive).toBe(false);
    });

    it('should not emit when no search results', () => {
      // Given
      component.rootNodes = [];
      component.hiddenNodeIds = [];
      component.searchQuery = '';
      spyOn(component.hideNodes, 'emit');

      // When
      component.onHideAllShownClick();

      // Then
      expect(component.hideNodes.emit).not.toHaveBeenCalled();
    });
  });

  describe('search highlight segments', () => {
    it('should precompute highlight segments for search results', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });

      // When
      component.searchQuery = 'ClA';
      component.applySearch();

      // Then — 'C' matches C(4), 'l' matches l(5), 'A' matches a(6) (earliest match)
      const segments = component.getHighlightSegments('pkg.ClassA');
      expect(segments).toEqual([
        {text: 'pkg.', isMatch: false},
        {text: 'Cla', isMatch: true},
        {text: 'ssA', isMatch: false}
      ]);
    });

    it('should return plain text when no segments precomputed', () => {
      // When
      const segments = component.getHighlightSegments('some.node.id');

      // Then
      expect(segments).toEqual([{text: 'some.node.id', isMatch: false}]);
    });
  });

  describe('leaf suffix handling in search', () => {
    it('should strip :leaf suffix in search display', () => {
      // Given
      const leaf = createMockNode('com.example.MyClass:leaf');
      component.rootNodes = [leaf];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [leaf], previousValue: [], firstChange: true, isFirstChange: () => true}
      });

      // When
      component.searchQuery = 'MyClass';
      component.applySearch();

      // Then — :leaf stripped; 'MyClass' is a substring of 'com.example.MyClass'
      expect(component.searchResults.map(n => n.id)).toEqual(['com.example.MyClass:leaf']);
      const segments = component.getHighlightSegments('com.example.MyClass:leaf');
      expect(segments).toEqual([
        {text: 'com.example.', isMatch: false},
        {text: 'MyClass', isMatch: true}
      ]);
    });

    it('should not match :leaf suffix characters', () => {
      // Given
      const leaf = createMockNode('com.example.MyClass:leaf');
      component.rootNodes = [leaf];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [leaf], previousValue: [], firstChange: true, isFirstChange: () => true}
      });

      // When
      component.searchQuery = 'leaf';
      component.applySearch();

      // Then — 'leaf' does not appear in stripped id 'com.example.MyClass'
      expect(component.searchResults).toEqual([]);
    });
  });

  describe('tree state preservation across search', () => {
    it('should preserve expandedTreeNodeIds when search is active and cleared', () => {
      // Given
      const child = createMockNode('pkg.ClassA');
      const container = createMockNode('pkg', [child]);
      component.rootNodes = [container];
      component.hiddenNodeIds = [];
      component.ngOnChanges({
        rootNodes: {currentValue: [container], previousValue: [], firstChange: true, isFirstChange: () => true}
      });
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);
      component.toggleTreeNode(mockEvent, container);
      expect(component.isTreeNodeExpanded(container)).toBe(true);

      // When - activate search
      component.searchQuery = 'Class';
      component.applySearch();

      // Then - tree state is untouched
      expect(component.expandedTreeNodeIds.has('pkg')).toBe(true);

      // When - clear search
      component.clearSearch();

      // Then - tree state still intact
      expect(component.isTreeNodeExpanded(container)).toBe(true);
    });
  });
});
