import {ComponentFixture, TestBed} from '@angular/core/testing';

import {NodeContainerComponent} from './node-container.component';

describe('NodeContainerComponent', () => {
  let component: NodeContainerComponent;
  let fixture: ComponentFixture<NodeContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NodeContainerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NodeContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
