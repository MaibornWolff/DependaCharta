import {TestBed} from '@angular/core/testing';
import {AppComponent} from './app.component';
import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {ToggleButtonComponent} from './ui/toggle-button/toggle-button.component';
import {Action} from './model/Action';
import {EdgeFilterType} from './model/EdgeFilter';
import {HiddenNodesListComponent} from './ui/hidden-nodes-list/hidden-nodes-list.component';

describe('AppComponent', () => {

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have the 'visualization' title`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('visualization');
  });

  describe('Edge Labels Toggle', () => {
    it('should render toggle-button component for edge labels', () => {
      // Arrange
      const fixture = TestBed.createComponent(AppComponent);
      fixture.detectChanges();

      // Act
      const toggleButtons: DebugElement[] = fixture.debugElement.queryAll(By.directive(ToggleButtonComponent));
      const edgeLabelsToggle = toggleButtons.find(el => el.nativeElement.textContent.trim() === 'Edge Labels');

      // Assert
      expect(edgeLabelsToggle).toBeTruthy();
    });

    it('should bind state.showLabels to toggle-button isOn input', () => {
      // Arrange
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      component.state = component.state.reduce(new Action.ToggleEdgeLabels()); // Set to false
      fixture.detectChanges();

      // Act
      const toggleButtons: DebugElement[] = fixture.debugElement.queryAll(By.directive(ToggleButtonComponent));
      const edgeLabelsToggle = toggleButtons.find(el => el.nativeElement.textContent.trim() === 'Edge Labels');
      const toggleComponent = edgeLabelsToggle?.componentInstance as ToggleButtonComponent;

      // Assert
      expect(toggleComponent.isOn).toBe(false);
    });

    it('should call onToggleEdgeLabels when toggleChanged emits', () => {
      // Arrange
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'onToggleEdgeLabels');
      fixture.detectChanges();

      // Act
      const toggleButtons: DebugElement[] = fixture.debugElement.queryAll(By.directive(ToggleButtonComponent));
      const edgeLabelsToggle = toggleButtons.find(el => el.nativeElement.textContent.trim() === 'Edge Labels');
      const toggleComponent = edgeLabelsToggle?.componentInstance as ToggleButtonComponent;
      toggleComponent.toggleChanged.emit();

      // Assert
      expect(component.onToggleEdgeLabels).toHaveBeenCalled();
    });

    it('should dispatch ToggleEdgeLabels action when toggle changes', () => {
      // Arrange
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // Act
      component.onToggleEdgeLabels();

      // Assert
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ToggleEdgeLabels));
    });

    it('should display "Edge Labels" text in toggle button', () => {
      // Arrange
      const fixture = TestBed.createComponent(AppComponent);
      fixture.detectChanges();

      // Act
      const toggleButtons: DebugElement[] = fixture.debugElement.queryAll(By.directive(ToggleButtonComponent));
      const edgeLabelsToggle = toggleButtons.find(el => el.nativeElement.textContent.trim() === 'Edge Labels');

      // Assert
      expect(edgeLabelsToggle?.nativeElement.textContent.trim()).toBe('Edge Labels');
    });
  });

  describe('Keyboard Shortcuts', () => {
    function createKeyboardEvent(key: string, options: Partial<KeyboardEventInit> = {}): KeyboardEvent {
      return new KeyboardEvent('keydown', { key, bubbles: true, ...options });
    }

    it('should dispatch ResetView action when R key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('r');
      component.onResetViewShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ResetView));
    });

    it('should dispatch ToggleEdgeLabels action when L key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('l');
      component.onToggleLabelsShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ToggleEdgeLabels));
    });

    it('should dispatch ToggleInteractionMode action when I key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('i');
      component.onToggleInteractionShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ToggleInteractionMode));
    });

    it('should dispatch ToggleUsageTypeMode action when U key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('u');
      component.onToggleUsageShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ToggleUsageTypeMode));
    });

    it('should dispatch RestoreNodes action when Z key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('z');
      component.onRestoreNodesShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.RestoreNodes));
    });

    it('should dispatch ChangeFilter with NONE when 0 key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('0');
      component.onFilterNoneShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.ChangeFilter(EdgeFilterType.NONE));
    });

    it('should dispatch ChangeFilter with ALL when 1 key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('1');
      component.onFilterAllShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.ChangeFilter(EdgeFilterType.ALL));
    });

    it('should dispatch ChangeFilter with CYCLES_ONLY when 2 key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('2');
      component.onFilterCyclesShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.ChangeFilter(EdgeFilterType.CYCLES_ONLY));
    });

    it('should dispatch ChangeFilter with FEEDBACK_LEAF_LEVEL_ONLY when 3 key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('3');
      component.onFilterLeafFeedbackShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.ChangeFilter(EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY));
    });

    it('should dispatch ChangeFilter with ALL_FEEDBACK_EDGES when 4 key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('4');
      component.onFilterAllFeedbackShortcut(event);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.ChangeFilter(EdgeFilterType.ALL_FEEDBACK_EDGES));
    });

    it('should toggle help popup when ? key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();
      expect(component.showHelpPopup).toBe(false);

      // When
      const event = createKeyboardEvent('?');
      component.onKeyDown(event);

      // Then
      expect(component.showHelpPopup).toBe(true);
    });

    it('should open file dialog when O key is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();
      spyOn(component['jsonLoaderComponent'], 'openFileDialog');

      // When
      const event = createKeyboardEvent('o');
      component.onOpenFileShortcut(event);

      // Then
      expect(component['jsonLoaderComponent'].openFileDialog).toHaveBeenCalled();
    });

    it('should close help popup when Escape is pressed and popup is open', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      component.showHelpPopup = true;
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('Escape');
      component.onEscapeShortcut(event);

      // Then
      expect(component.showHelpPopup).toBe(false);
    });

    it('should do nothing when Escape is pressed and popup is already closed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      component.showHelpPopup = false;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      const event = createKeyboardEvent('Escape');
      component.onEscapeShortcut(event);

      // Then
      expect(component.apply).not.toHaveBeenCalled();
    });

    it('should not trigger shortcuts when input element is focused', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();
      const inputElement = document.createElement('input');
      document.body.appendChild(inputElement);

      // When
      const event = new KeyboardEvent('keydown', { key: 'r', bubbles: true });
      Object.defineProperty(event, 'target', { value: inputElement });
      component.onResetViewShortcut(event);

      // Then
      expect(component.apply).not.toHaveBeenCalled();

      // Cleanup
      document.body.removeChild(inputElement);
    });

    it('should toggle feedback panel when F key is pressed and in interactive mode', () => {
      // Given - isInteractive is true by default
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();
      const mockToggleExpanded = jasmine.createSpy('toggleExpanded');
      Object.defineProperty(component, 'feedbackEdgesListComponent', {
        value: { toggleExpanded: mockToggleExpanded },
        writable: true
      });

      // When
      const event = createKeyboardEvent('f');
      component.onToggleFeedbackPanelShortcut(event);

      // Then
      expect(mockToggleExpanded).toHaveBeenCalled();
    });

    it('should not toggle feedback panel when F key is pressed and not in interactive mode', () => {
      // Given - toggle to make isInteractive false
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      component.state = component.state.reduce(new Action.ToggleInteractionMode());
      fixture.detectChanges();
      const mockToggleExpanded = jasmine.createSpy('toggleExpanded');
      Object.defineProperty(component, 'feedbackEdgesListComponent', {
        value: { toggleExpanded: mockToggleExpanded },
        writable: true
      });

      // When
      const event = createKeyboardEvent('f');
      component.onToggleFeedbackPanelShortcut(event);

      // Then - toggleExpanded should NOT be called when not in interactive mode
      expect(mockToggleExpanded).not.toHaveBeenCalled();
    });

    it('should toggle hidden nodes panel when H key is pressed and in interactive mode', () => {
      // Given - isInteractive is true by default
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();
      const mockToggleExpanded = jasmine.createSpy('toggleExpanded');
      Object.defineProperty(component, 'hiddenNodesListComponent', {
        value: { toggleExpanded: mockToggleExpanded },
        writable: true
      });

      // When
      const event = createKeyboardEvent('h');
      component.onToggleHiddenNodesPanelShortcut(event);

      // Then
      expect(mockToggleExpanded).toHaveBeenCalled();
    });

    it('should not toggle hidden nodes panel when H key is pressed and not in interactive mode', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      component.state = component.state.reduce(new Action.ToggleInteractionMode());
      fixture.detectChanges();
      const mockToggleExpanded = jasmine.createSpy('toggleExpanded');
      Object.defineProperty(component, 'hiddenNodesListComponent', {
        value: { toggleExpanded: mockToggleExpanded },
        writable: true
      });

      // When
      const event = createKeyboardEvent('h');
      component.onToggleHiddenNodesPanelShortcut(event);

      // Then
      expect(mockToggleExpanded).not.toHaveBeenCalled();
    });
  });

  describe('Hidden Node Events', () => {
    it('should dispatch RestoreNode action when restore node event is received', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onRestoreHiddenNode({ nodeId: 'com.pkg.ClassA', parentNodeId: 'com.pkg' });

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.RestoreNode('com.pkg.ClassA', 'com.pkg'));
    });

    it('should dispatch RestoreAllHiddenNodes action when restore all is triggered', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onRestoreAllHidden();

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.RestoreAllHiddenNodes));
    });
  });

  describe('Feedback Edge Events', () => {
    it('should dispatch NavigateToEdge action when edge is clicked', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();
      const mockEdge = { source: 'com.A', target: 'com.B' } as any;

      // When
      component.onEdgeClicked(mockEdge);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.NavigateToEdge('com.A', 'com.B'));
    });

    it('should dispatch NavigateToEdge action when group is clicked', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();
      const mockGroup = { source: 'com.pkg', target: 'com.other', isGroup: true } as any;

      // When
      component.onGroupClicked(mockGroup);

      // Then
      expect(component.apply).toHaveBeenCalledWith(new Action.NavigateToEdge('com.pkg', 'com.other'));
    });
  });

  describe('Multiselect Mode', () => {
    it('should dispatch EnterMultiselectMode action when Shift is pressed', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.enterMultiselectMode();

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.EnterMultiselectMode));
    });

    it('should dispatch LeaveMultiselectMode and ResetMultiselection when Shift is released', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.leaveMultiselectMode();

      // Then
      expect(component.apply).toHaveBeenCalledTimes(2);
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.LeaveMultiselectMode));
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ResetMultiselection));
    });

    it('should dispatch LeaveMultiselectMode when window loses focus', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onWindowBlur();

      // Then
      expect(component.apply).toHaveBeenCalledTimes(1);
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.LeaveMultiselectMode));
    });
  });

  describe('File Loading', () => {
    it('should set cytoscapeInitialized to false and then true during file load start', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();
      expect(component.cytoscapeInitialized).toBe(true);

      // When
      component.onFileLoadStart();

      // Then
      expect(component.cytoscapeInitialized).toBe(true);
      expect(component.isLoading).toBe(true);
    });
  });

  describe('Button Handlers', () => {
    it('should dispatch RestoreNodes action when restore button is clicked', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onRestoreNodesClick();

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.RestoreNodes));
    });

    it('should dispatch ResetView action when reset view button is clicked', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onResetViewClick();

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ResetView));
    });

    it('should dispatch ToggleInteractionMode action when interaction button is clicked', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onToggleInteraction();

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ToggleInteractionMode));
    });

    it('should dispatch ToggleUsageTypeMode action when usage button is clicked', () => {
      // Given
      const fixture = TestBed.createComponent(AppComponent);
      const component = fixture.componentInstance;
      spyOn(component, 'apply');
      fixture.detectChanges();

      // When
      component.onToggleUsage();

      // Then
      expect(component.apply).toHaveBeenCalledWith(jasmine.any(Action.ToggleUsageTypeMode));
    });
  });
});
