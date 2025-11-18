import {TestBed} from '@angular/core/testing';
import {AppComponent} from './app.component';
import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {ToggleButtonComponent} from './ui/toggle-button/toggle-button.component';
import {Action} from './model/Action';

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
});
