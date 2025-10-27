import {ComponentFixture, TestBed} from '@angular/core/testing';

import {InteractionBarComponent} from './interaction-bar.component';

describe('InteractionBarComponent', () => {
  let component: InteractionBarComponent;
  let fixture: ComponentFixture<InteractionBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InteractionBarComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InteractionBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
