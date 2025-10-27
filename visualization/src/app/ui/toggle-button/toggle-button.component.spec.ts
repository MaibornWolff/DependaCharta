import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToggleButtonComponent } from './toggle-button.component';

describe('ToggleButtonComponent', () => {
  let component: ToggleButtonComponent;
  let fixture: ComponentFixture<ToggleButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToggleButtonComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToggleButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
