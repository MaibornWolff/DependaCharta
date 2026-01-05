import {TestBed} from '@angular/core/testing';
import {HelpPopupComponent} from './help-popup.component';

describe('HelpPopupComponent', () => {

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HelpPopupComponent],
    }).compileComponents();
  });

  it('should create the component', () => {
    // Given
    const fixture = TestBed.createComponent(HelpPopupComponent);

    // When
    const component = fixture.componentInstance;

    // Then
    expect(component).toBeTruthy();
  });

  it('should emit closePopup when overlay is clicked', () => {
    // Given
    const fixture = TestBed.createComponent(HelpPopupComponent);
    const component = fixture.componentInstance;
    spyOn(component.closePopup, 'emit');
    fixture.detectChanges();

    // When
    const overlay = fixture.nativeElement.querySelector('.help-overlay');
    overlay.click();

    // Then
    expect(component.closePopup.emit).toHaveBeenCalled();
  });

  it('should not emit closePopup when popup content is clicked', () => {
    // Given
    const fixture = TestBed.createComponent(HelpPopupComponent);
    const component = fixture.componentInstance;
    spyOn(component.closePopup, 'emit');
    fixture.detectChanges();

    // When
    const popup = fixture.nativeElement.querySelector('.help-popup');
    popup.click();

    // Then
    expect(component.closePopup.emit).not.toHaveBeenCalled();
  });

  it('should emit closePopup when close button is clicked', () => {
    // Given
    const fixture = TestBed.createComponent(HelpPopupComponent);
    const component = fixture.componentInstance;
    spyOn(component.closePopup, 'emit');
    fixture.detectChanges();

    // When
    const closeButton = fixture.nativeElement.querySelector('.close-button');
    closeButton.click();

    // Then
    expect(component.closePopup.emit).toHaveBeenCalled();
  });

  it('should display keyboard shortcuts in the help content', () => {
    // Given
    const fixture = TestBed.createComponent(HelpPopupComponent);
    fixture.detectChanges();

    // When
    const helpContent = fixture.nativeElement.querySelector('.help-content');

    // Then
    expect(helpContent.textContent).toContain('Reset View');
    expect(helpContent.textContent).toContain('Toggle Labels');
    expect(helpContent.textContent).toContain('Interaction Mode');
  });
});
