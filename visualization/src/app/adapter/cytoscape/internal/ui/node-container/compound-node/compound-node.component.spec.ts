import {ComponentFixture, TestBed} from '@angular/core/testing';
import {CompoundNodeComponent} from './compound-node.component';
import {State} from '../../../../../../model/State';
import {ToggleEdgeLabels} from '../../../../../../model/Action';
import { VisibleGraphNode } from '../../../../../../model/GraphNode.spec';

describe('CompoundNodeComponent', () => {
  let component: CompoundNodeComponent;
  let fixture: ComponentFixture<CompoundNodeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompoundNodeComponent]
    })
    .compileComponents()

    fixture = TestBed.createComponent(CompoundNodeComponent);
    component = fixture.componentInstance
    component.node = VisibleGraphNode.build()
    component.stateChange = {
      state: State.build(),
      action: new ToggleEdgeLabels()
    }
    fixture.detectChanges()
  })

  // TODO add domain tests
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
