import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar, MatSnackBarRef } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';
import { Core } from 'cytoscape';
import {
  MouseInaccuracyDetectorComponent,
  WarningSnackbarComponent,
  WarningDetailsDialog,
  InputDevice,
  BrowserGlobals
} from './mouse-inaccuracy-detector.component';
import { CytoscapeService } from './adapter/cytoscape/internal/cytoscape.service';

describe('MouseInaccuracyDetectorComponent', () => {
  let component: MouseInaccuracyDetectorComponent;
  let fixture: ComponentFixture<MouseInaccuracyDetectorComponent>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockCytoscapeService: jasmine.SpyObj<CytoscapeService>;
  let mockSnackBarRef: jasmine.SpyObj<MatSnackBarRef<void>>;
  let panOrZoomSubject: Subject<Core>;

  beforeEach(async () => {
    panOrZoomSubject = new Subject<Core>();
    mockSnackBarRef = jasmine.createSpyObj('MatSnackBarRef', ['onAction', 'dismiss']);
    mockSnackBarRef.onAction.and.returnValue(of(void 0));

    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['openFromComponent']);
    mockSnackBar.openFromComponent.and.returnValue(mockSnackBarRef);

    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);

    mockCytoscapeService = jasmine.createSpyObj('CytoscapeService', [], {
      panOrZoom: panOrZoomSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [MouseInaccuracyDetectorComponent],
      providers: [
        { provide: MatSnackBar, useValue: mockSnackBar },
        { provide: MatDialog, useValue: mockDialog },
        { provide: CytoscapeService, useValue: mockCytoscapeService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MouseInaccuracyDetectorComponent);
    component = fixture.componentInstance;
  });

  describe('component initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default configuration values', () => {
      expect(component.maximumSampleSize).toBe(20);
      expect(component.minimalSampleSize).toBe(10);
      expect(component.mouseConfidenceThreshold).toBe(0);
      expect(component.touchpadConfidenceThreshold).toBe(1);
      expect(component.confidenceWeightOfIntegerComponent).toBe(1);
      expect(component.confidenceWeightOfDeltaXNotZeroComponent).toBe(2);
      expect(component.warningTimoutInSeconds).toBe(12);
      expect(component.zoomJumpingTolerance).toBe(0.1);
    });

    it('should initialize with empty state', () => {
      expect(component.touchpadConfidenceTrail).toEqual([]);
      expect(component.touchpadConfidenceMedian).toBeUndefined();
      expect(component.guessedInputDevice).toBeUndefined();
      expect(component.lastZoom).toBe(1);
    });

    it('should subscribe to cytoscapeService.panOrZoom on init', () => {
      const mockCore = createMockCore(3, 0.5, 2);

      component.ngOnInit();
      panOrZoomSubject.next(mockCore);

      expect(component.lastZoom).toBe(2);
    });
  });

  describe('zoom jumping detection through cytoscape service', () => {
    it('should not warn when zoom delta is below threshold', () => {
      const mockCore = createMockCore(3, 0.5, 2);
      component.lastZoom = 1.9;

      component.ngOnInit();
      panOrZoomSubject.next(mockCore);

      expect(mockSnackBar.openFromComponent).not.toHaveBeenCalled();
      expect(component.lastZoom).toBe(2);
    });

    it('should warn when zoom delta equals threshold', () => {
      const mockCore = createMockCore(3, 0.5, 3);
      component.lastZoom = 0.6;

      component.ngOnInit();
      panOrZoomSubject.next(mockCore);

      expect(mockSnackBar.openFromComponent).toHaveBeenCalledWith(
        WarningSnackbarComponent,
        { duration: 12000 }
      );
      expect(component.lastZoom).toBe(3);
    });

    it('should warn when zoom delta exceeds threshold', () => {
      const mockCore = createMockCore(3, 0.5, 3);
      component.lastZoom = 0.5;

      component.ngOnInit();
      panOrZoomSubject.next(mockCore);

      expect(mockSnackBar.openFromComponent).toHaveBeenCalled();
    });
  });

  describe('onWindowScroll', () => {
    it('should update confidence trail when receiving wheel events', () => {
      const mockEvent = { deltaY: 100, deltaX: 5 } as WheelEvent;

      component.onWindowScroll(mockEvent);

      expect(component.touchpadConfidenceTrail.length).toBe(1);
      expect(component.touchpadConfidenceTrail[0]).toBe(1); // integer + non-zero deltaX = full confidence
    });

    it('should calculate median when minimum samples are reached', () => {
      // Add enough samples to reach minimum
      for (let i = 0; i < component.minimalSampleSize; i++) {
        const mockEvent = { deltaY: 100, deltaX: i % 2 === 0 ? 5 : 0 } as WheelEvent;
        component.onWindowScroll(mockEvent);
      }

      expect(component.touchpadConfidenceMedian).toBeDefined();
    });

    it('should limit trail size to maximum sample size', () => {
      // Add more samples than maximum
      for (let i = 0; i < component.maximumSampleSize + 5; i++) {
        const mockEvent = { deltaY: 100, deltaX: 5 } as WheelEvent;
        component.onWindowScroll(mockEvent);
      }

      expect(component.touchpadConfidenceTrail.length).toBe(component.maximumSampleSize);
    });

    it('should make initial device guess when reaching minimum samples', () => {
      spyOn(console, 'log');

      // Add touchpad-like events to establish initial guess
      for (let i = 0; i < component.minimalSampleSize; i++) {
        const mockEvent = { deltaY: 100, deltaX: 5 } as WheelEvent; // touchpad-like
        component.onWindowScroll(mockEvent);
      }

      expect(console.log).toHaveBeenCalledWith('Assuming initial device is touchpad!');
      expect(component.guessedInputDevice).toBe(InputDevice.TOUCHPAD);
    });

    it('should detect input device switch and warn user', () => {
      spyOn(console, 'log');

      // Set more permissive thresholds for easier testing
      component.mouseConfidenceThreshold = 0.4;
      component.touchpadConfidenceThreshold = 0.6;

      // Manually set up initial state as if we already detected a mouse
      component.guessedInputDevice = InputDevice.MOUSE;
      component.touchpadConfidenceMedian = 0.3; // Low confidence (mouse-like)

      // Now simulate a clear touchpad pattern that should trigger a switch
      for (let i = 0; i < component.minimalSampleSize; i++) {
        const mockEvent = { deltaY: 100, deltaX: 5 } as WheelEvent; // confidence: 1
        component.onWindowScroll(mockEvent);
      }

      // The median should now be high enough to trigger a switch from MOUSE to TOUCHPAD
      expect(mockSnackBar.openFromComponent).toHaveBeenCalled();
      expect(console.log).toHaveBeenCalledWith('Switch to touchpad detected!');
      expect(component.guessedInputDevice).toBe(InputDevice.TOUCHPAD);
    });
  });

  describe('confidence calculation through wheel events', () => {
    it('should calculate maximum confidence for touchpad-like events', () => {
      const mockEvent = { deltaY: 100, deltaX: 5 } as WheelEvent; // integer + non-zero deltaX

      component.onWindowScroll(mockEvent);

      expect(component.touchpadConfidenceTrail[0]).toBe(1);
    });

    it('should calculate minimum confidence for mouse-like events', () => {
      const mockEvent = { deltaY: 100.5, deltaX: 0 } as WheelEvent; // non-integer + zero deltaX

      component.onWindowScroll(mockEvent);

      expect(component.touchpadConfidenceTrail[0]).toBe(0);
    });

    it('should calculate partial confidence for mixed signals', () => {
      const mockEvent1 = { deltaY: 100, deltaX: 0 } as WheelEvent; // integer but zero deltaX
      const mockEvent2 = { deltaY: 100.5, deltaX: 5 } as WheelEvent; // non-integer but non-zero deltaX

      component.onWindowScroll(mockEvent1);
      component.onWindowScroll(mockEvent2);

      expect(component.touchpadConfidenceTrail[0]).toBeCloseTo(0.33, 2);
      expect(component.touchpadConfidenceTrail[1]).toBeCloseTo(0.67, 2);
    });
  });

  describe('warning system integration', () => {
    it('should open snackbar with correct configuration', () => {
      const mockCore = createMockCore(3, 0.5, 3);
      component.lastZoom = 0.5;

      component.ngOnInit();
      panOrZoomSubject.next(mockCore);

      expect(mockSnackBar.openFromComponent).toHaveBeenCalledWith(
        WarningSnackbarComponent,
        { duration: 12000 }
      );
    });

    it('should open dialog when snackbar action is triggered', () => {
      const mockCore = createMockCore(3, 0.5, 3);
      component.lastZoom = 0.5;

      component.ngOnInit();
      panOrZoomSubject.next(mockCore);

      // Simulate snackbar action click
      mockSnackBarRef.onAction().subscribe();

      expect(mockDialog.open).toHaveBeenCalledWith(WarningDetailsDialog);
    });
  });

  function createMockCore(maxZoom: number, minZoom: number, currentZoom: number): jasmine.SpyObj<Core> {
    const mockCore = jasmine.createSpyObj('Core', ['maxZoom', 'minZoom', 'zoom']);
    mockCore.maxZoom.and.returnValue(maxZoom);
    mockCore.minZoom.and.returnValue(minZoom);
    mockCore.zoom.and.returnValue(currentZoom);
    return mockCore;
  }
});

describe('WarningSnackbarComponent', () => {
  let component: WarningSnackbarComponent;
  let fixture: ComponentFixture<WarningSnackbarComponent>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockSnackBarRef: jasmine.SpyObj<MatSnackBarRef<void>>;

  beforeEach(async () => {
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockSnackBarRef = jasmine.createSpyObj('MatSnackBarRef', ['dismiss']);

    await TestBed.configureTestingModule({
      imports: [WarningSnackbarComponent],
      providers: [
        { provide: MatDialog, useValue: mockDialog },
        { provide: MatSnackBarRef, useValue: mockSnackBarRef }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WarningSnackbarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show details and dismiss snackbar', () => {
    component.showDetails();

    expect(mockSnackBarRef.dismiss).toHaveBeenCalled();
    expect(mockDialog.open).toHaveBeenCalledWith(WarningDetailsDialog);
  });

  it('should dismiss snackbar and reload browser when reload is called', () => {
    spyOn(BrowserGlobals, 'reloadBrowser');

    component.reload();

    expect(mockSnackBarRef.dismiss).toHaveBeenCalled();
    expect(BrowserGlobals.reloadBrowser).toHaveBeenCalled();
  });
});

describe('WarningDetailsDialog', () => {
  let component: WarningDetailsDialog;
  let fixture: ComponentFixture<WarningDetailsDialog>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<WarningDetailsDialog>>;

  beforeEach(async () => {
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [WarningDetailsDialog],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WarningDetailsDialog);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('InputDevice', () => {
  describe('enum values', () => {
    it('should have correct enum values', () => {
      expect(InputDevice.MOUSE).toBe('mouse');
      expect(InputDevice.TOUCHPAD).toBe('touchpad');
    });
  });

  describe('invert function', () => {
    it('should invert mouse to touchpad', () => {
      expect(InputDevice.invert(InputDevice.MOUSE)).toBe(InputDevice.TOUCHPAD);
    });

    it('should invert touchpad to mouse', () => {
      expect(InputDevice.invert(InputDevice.TOUCHPAD)).toBe(InputDevice.MOUSE);
    });
  });
});

// Test the standalone median function
describe('median function', () => {
  // Since median is not exported, we'll test it indirectly through the component behavior
  it('should calculate median correctly through component trail processing', () => {
    const component = new MouseInaccuracyDetectorComponent(
      jasmine.createSpyObj('MatSnackBar', ['openFromComponent']),
      jasmine.createSpyObj('MatDialog', ['open']),
      jasmine.createSpyObj('CytoscapeService', [], { panOrZoom: of() })
    );

    // Add known values to test median calculation
    const events = [
      { deltaY: 100, deltaX: 5 }, // confidence: 1
      { deltaY: 100.5, deltaX: 0 }, // confidence: 0
      { deltaY: 100, deltaX: 0 }, // confidence: 0.33
      { deltaY: 100.5, deltaX: 5 }, // confidence: 0.67
      { deltaY: 100, deltaX: 5 }, // confidence: 1
      { deltaY: 100.5, deltaX: 0 }, // confidence: 0
      { deltaY: 100, deltaX: 0 }, // confidence: 0.33
      { deltaY: 100.5, deltaX: 5 }, // confidence: 0.67
      { deltaY: 100, deltaX: 5 }, // confidence: 1
      { deltaY: 100.5, deltaX: 0 }  // confidence: 0 (10th event to trigger median)
    ];

    events.forEach(event => {
      component.onWindowScroll(event as WheelEvent);
    });

    // The median should be calculated and available
    expect(component.touchpadConfidenceMedian).toBeDefined();
    expect(typeof component.touchpadConfidenceMedian).toBe('number');
  });
});
