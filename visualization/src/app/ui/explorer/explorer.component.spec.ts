import {ComponentFixture, TestBed} from '@angular/core/testing';
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
      component.onResizeStart(mockEvent, 'bottom');

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
});
