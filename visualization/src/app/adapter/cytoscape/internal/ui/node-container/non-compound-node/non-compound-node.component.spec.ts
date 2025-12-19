import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NonCompoundNodeComponent} from './non-compound-node.component';
import {CytoscapeService} from '../../../cytoscape.service';
import {Action} from '../../../../../../model/Action';
import {RenderInformation} from '../node-container.component';
import {Subject} from 'rxjs';
import {State} from '../../../../../../model/State';
import { VisibleGraphNode } from '../../../../../../model/GraphNode.spec';

describe('NonCompoundNodeComponent', () => {
  let component: NonCompoundNodeComponent;
  let fixture: ComponentFixture<NonCompoundNodeComponent>;
  let mockStateService: jasmine.SpyObj<CytoscapeService>;
  let interactionModeToggled: Subject<boolean>;

  beforeEach(async () => {
    interactionModeToggled = new Subject<boolean>();

    mockStateService = jasmine.createSpyObj('CytoscapeService', [
      'getIfInteractionModeIsOn'
    ], {
      interactionModeToggled: interactionModeToggled.asObservable(),
      graphActionHappened: jasmine.createSpyObj('EventEmitter', ['emit'])
    });

    await TestBed.configureTestingModule({
      imports: [NonCompoundNodeComponent],
      providers: [
        { provide: CytoscapeService, useValue: mockStateService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NonCompoundNodeComponent);
    component = fixture.componentInstance;

    component.node = VisibleGraphNode.build();
    component.stateChange = { state: State.build(), action: new Action.ToggleEdgeLabels() }
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('togglePin', () => {
    it('should handle node pinning', () => {
      component.node = VisibleGraphNode.build();
      component.stateChange = { state: State.build({ selectedPinnedNodeIds: [component.node.id] }), action: new Action.ToggleEdgeLabels() }
      component.togglePin();

      expect(mockStateService.graphActionHappened.emit).toHaveBeenCalledWith(new Action.UnpinNode(component.node.id));
    });
  });

  describe('onMouseDown', () => {
    it('should store click coordinates', () => {
      const mouseEvent = new MouseEvent('mousedown', { clientX: 100, clientY: 200 });

      component.onMouseDown(mouseEvent);

      expect(component.clickCoordinates).toEqual({ x: 100, y: 200 });
    });
  });

  describe('onMouseUp', () => {
    it('should handle click (same coordinates) in normal mode by expanding node', () => {
      component.clickCoordinates = { x: 100, y: 200 };
      const mouseEvent = new MouseEvent('mouseup', { clientX: 100, clientY: 200 });
      spyOn(component, 'expandNode');

      component.onMouseUp(mouseEvent);

      expect(component.expandNode).toHaveBeenCalled();
    });

    it('should do nothing when coordinates differ (drag)', () => {
      component.clickCoordinates = { x: 100, y: 200 };
      const mouseEvent = new MouseEvent('mouseup', { clientX: 150, clientY: 250 });
      spyOn(component, 'expandNode');

      component.onMouseUp(mouseEvent);

      expect(component.expandNode).not.toHaveBeenCalled();
      expect(mockStateService.graphActionHappened.emit).not.toHaveBeenCalled();
    });
  });

  describe('onMouseEnter', () => {
    it('should emit show edges action', () => {
      component.onMouseEnter();

      expect(mockStateService.graphActionHappened.emit).toHaveBeenCalledWith(new Action.ShowAllEdgesOfNode(component.node.id));
    });
  });

  describe('onMouseLeave', () => {
    it('should emit hide edges action when node is not pinned', () => {
      component.onMouseLeave();

      expect(mockStateService.graphActionHappened.emit).toHaveBeenCalledWith(new Action.HideAllEdgesOfNode(component.node.id));
    });

    it('should emit hide edges action when node is pinned', () => {
      component.onMouseLeave();

      expect(mockStateService.graphActionHappened.emit).toHaveBeenCalledWith(new Action.HideAllEdgesOfNode(component.node.id));
    });
  });

  describe('expandNode', () => {
    it('should emit expand action when node has children', () => {
      const childNode = VisibleGraphNode.build();
      component.node.children = [childNode];

      component.expandNode();

      expect(mockStateService.graphActionHappened.emit).toHaveBeenCalledWith(new Action.ExpandNode(component.node.id));
    });

    it('should not emit expand action when node has no children', () => {
      component.node.children = [];

      component.expandNode();

      expect(mockStateService.graphActionHappened.emit).not.toHaveBeenCalled();
    });
  });

  describe('hideNode', () => {
    // TODO there is no pinned service any longer (implementation detail removed)
    it('should remove node from pinned service and emit hide action', () => {
      component.hideNode();

      expect(mockStateService.graphActionHappened.emit).toHaveBeenCalledWith(new Action.HideNode(component.node.id));
    });
  });

  describe('calculatedNodeStyle', () => {
    beforeEach(() => {
      spyOn(component, 'calculateLightnessOfPackageNodes').and.returnValue(50);
      spyOn(component, 'calculateBorderWidth').and.returnValue(2);
      component.stateChange = { state: State.build({ isInteractive: true }), action: new Action.ToggleEdgeLabels() }
    });

    it('should not set cursor for non-package nodes', () => {
      // Create a non-package node
      const nonPackageNode = VisibleGraphNode.build();
      (nonPackageNode as any).type = 'CLASS'; // Adjust based on your GraphNode structure
      component.node = nonPackageNode;

      const style = component.calculatedNodeStyle();

      expect(style.cursor).toBeUndefined();
    });

    it('should not set position styles when renderInformation is undefined', () => {
      component.renderInformation = undefined;

      const style = component.calculatedNodeStyle();

      expect(style.position).toBeUndefined();
      expect(style.left).toBeUndefined();
      expect(style.top).toBeUndefined();
      expect(style.width).toBeUndefined();
      expect(style.height).toBeUndefined();
    });

    it('should use green border color for pinned nodes in interaction mode', () => {
      component.stateChange = { state: State.build({ selectedPinnedNodeIds: [component.node.id], isInteractive: true }), action: new Action.ToggleEdgeLabels() }

      const style = component.calculatedNodeStyle();

      expect(style.outline).toContain('green');
    });

    it('should use teal border color for non-package nodes', () => {
      const nonPackageNode = VisibleGraphNode.build();
      (nonPackageNode as any).type = 'CLASS';
      component.node = nonPackageNode;

      const style = component.calculatedNodeStyle();

      expect(style.outline).toContain('teal');
    });

    it('should set white background for non-package nodes', () => {
      const nonPackageNode = VisibleGraphNode.build();
      (nonPackageNode as any).type = 'CLASS';
      component.node = nonPackageNode;

      const style = component.calculatedNodeStyle();

      expect(style.backgroundColor).toBe('white');
    });

    it('should handle renderInformation when provided', () => {
      component.renderInformation = new RenderInformation(
        0,
        0,
        100,
        50
      )

      // Test that the method doesn't throw and returns a style object
      const style = component.calculatedNodeStyle();

      expect(style).toBeDefined();
      expect(style.width).toBe('100px');
      expect(style.height).toBe('50px');
    });
  });

  describe('calculateLightnessOfPackageNodes', () => {
    it('should return a lightness value within the expected range', () => {
      const lightness = component['calculateLightnessOfPackageNodes']();

      expect(lightness).toBeGreaterThanOrEqual(30);
      expect(lightness).toBeLessThanOrEqual(95);
      expect(typeof lightness).toBe('number');
    });

    it('should calculate different lightness for nodes with different parent structures', () => {
      // Create node with no parent
      const rootNode = VisibleGraphNode.build();
      rootNode.parent = undefined;
      component.node = rootNode;
      const rootLightness = component['calculateLightnessOfPackageNodes']();

      // Create node with parent
      const parentNode = VisibleGraphNode.build();
      const childNode = VisibleGraphNode.build();
      childNode.parent = parentNode;
      component.node = childNode;
      const childLightness = component['calculateLightnessOfPackageNodes']();

      // Root should be lighter than child (higher lightness value)
      expect(rootLightness).toBeGreaterThanOrEqual(childLightness);
    });
  });

  describe('calculateBorderWidth', () => {
    it('should return thick border for pinned nodes in interaction mode', () => {
      component.stateChange = { state: State.build({ selectedPinnedNodeIds: [component.node.id], isInteractive: true }), action: new Action.ToggleEdgeLabels() }
      component.zoom = 2;

      expect(component.calculateBorderWidth()).toBe(1.5); // 3 / 2
    });

    it('should return normal border for non-pinned nodes', () => {
      component.zoom = 2;

      expect(component.calculateBorderWidth()).toBe(0.5); // 1 / 2
    });

    it('should return normal border for pinned nodes not in interaction mode', () => {
      component.stateChange = { state: State.build({ isInteractive: false }), action: new Action.ToggleEdgeLabels() }
      component.zoom = 2;

      expect(component.calculateBorderWidth()).toBe(0.5); // 1 / 2
    });

    it('should handle different zoom levels correctly', () => {
      component.zoom = 1;
      expect(component.calculateBorderWidth()).toBe(1); // 1 / 1
    });
  });

  describe('calculateLabel', () => {
    it('should return original label for package nodes', () => {
      const packageNode = VisibleGraphNode.build();
      (packageNode as any).type = 'PACKAGE';
      packageNode.label = 'test package';
      component.node = packageNode;

      const label = component.calculateLabel();

      expect(label).toBe('test package');
    });

    it('should return normal label for non-package nodes', () => {
      const nonPackageNode = VisibleGraphNode.build();
      (nonPackageNode as any).type = 'CLASS';
      nonPackageNode.label = 'Test Method';
      component.node = nonPackageNode;

      const label = component.calculateLabel();

      expect(label).toBe('Test Method');
    });

    it('should handle empty labels', () => {
      const node = VisibleGraphNode.build();
      node.label = '';
      component.node = node;

      const label = component.calculateLabel();

      expect(label).toBe('');
    });
  });
});
