import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Renderer2 } from '@angular/core';
import { Subject } from 'rxjs';
import { CytoscapeComponent } from './cytoscape.component';
import { CytoscapeService } from './internal/cytoscape.service';
import { NodeContainerComponent, RenderInformation } from './internal/ui/node-container/node-container.component';
import { Action, ExpandNode } from '../../model/Action';
import { Core } from 'cytoscape';

describe('CytoscapeComponent', () => {
  let component: CytoscapeComponent;
  let fixture: ComponentFixture<CytoscapeComponent>;
  let mockCytoscapeService: jasmine.SpyObj<CytoscapeService>;
  let mockRenderer: jasmine.SpyObj<Renderer2>;
  let mockNonCompoundContainer: jasmine.SpyObj<NodeContainerComponent>;
  let mockCompoundContainer: jasmine.SpyObj<NodeContainerComponent>;

  // Subject Mocks für Event Streams
  let graphActionHappenedSubject: Subject<Action>;
  let layoutStoppedSubject: Subject<Core>;
  let nodesPositionedSubject: Subject<any[]>;
  let panOrZoomSubject: Subject<Core>;
  let changeCursorSubject: Subject<string>;

  // Mock RenderInformation
  const mockRenderInfo = new RenderInformation(
    100,
    200,
    150,
    75
  )

  beforeEach(async () => {
    // Event Subjects erstellen
    graphActionHappenedSubject = new Subject<Action>();
    layoutStoppedSubject = new Subject<Core>();
    nodesPositionedSubject = new Subject<any[]>();
    panOrZoomSubject = new Subject<Core>();
    changeCursorSubject = new Subject<string>();

    // Service Mocks
    mockCytoscapeService = jasmine.createSpyObj('CytoscapeService', [
      'apply'
    ], {
      graphActionHappened: graphActionHappenedSubject.asObservable(),
      layoutStopped: layoutStoppedSubject.asObservable(),
      nodesPositioned: nodesPositionedSubject.asObservable(),
      panOrZoom: panOrZoomSubject.asObservable(),
      changeCursor: changeCursorSubject.asObservable()
    });

    mockRenderer = jasmine.createSpyObj('Renderer2', ['setStyle']);

    // NodeContainer Mocks
    mockNonCompoundContainer = jasmine.createSpyObj('NodeContainerComponent', [
      'add', 'replace', 'removeAll'
    ]);
    mockCompoundContainer = jasmine.createSpyObj('NodeContainerComponent', [
      'add', 'replace', 'removeAll'
    ]);

    await TestBed.configureTestingModule({
      imports: [CytoscapeComponent],
      providers: [
        { provide: CytoscapeService, useValue: mockCytoscapeService },
        { provide: Renderer2, useValue: mockRenderer }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CytoscapeComponent);
    component = fixture.componentInstance;

    // ViewChild Mocks setzen BEVOR detectChanges
    component.nonCompoundNodeContainer = mockNonCompoundContainer;
    component.compoundNodeContainer = mockCompoundContainer;

    // Manuell den Renderer injizieren
    component.renderer = mockRenderer;
  });

  afterEach(() => {
    // Subjects cleanup
    graphActionHappenedSubject.complete();
    layoutStoppedSubject.complete();
    nodesPositionedSubject.complete();
    panOrZoomSubject.complete();
    changeCursorSubject.complete();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should have initial pan and zoom values', () => {
      expect(component.pan).toEqual({ x: 0, y: 0 });
      expect(component.zoom).toBe(1);
    });

    it('should register event listeners on init', () => {
      spyOn(component as any, 'registerEventListeners');

      component.ngOnInit();

      expect((component as any).registerEventListeners).toHaveBeenCalled();
    });
  });

  describe('Event Listeners', () => {
    beforeEach(() => {
      // ngOnInit manuell aufrufen um Event-Listener zu registrieren
      component.ngOnInit();
    });

    describe('stateChangeRequested subscription', () => {
      it('should emit stateChangeRequested when cytoscapeService emits', () => {
        const mockAction: Action = new ExpandNode('test');
        spyOn(component.graphActionHappened, 'emit');
        
        graphActionHappenedSubject.next(mockAction);
        
        expect(component.graphActionHappened.emit).toHaveBeenCalledWith(mockAction);
      });
    });
  });

  describe('Component Properties', () => {
    it('should have injected services', () => {
      expect(component.cytoscapeService).toBe(mockCytoscapeService);
      expect(component.renderer).toBe(mockRenderer);
    });
  });
});
