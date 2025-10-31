import {ComponentFixture, TestBed} from '@angular/core/testing';

import {InteractionMenuComponent} from './interaction-menu.component';
import { By } from '@angular/platform-browser';
import {State} from '../../../../../../../model/State';
import { VisibleGraphNode } from '../../../../../../../model/GraphNode.spec';

describe('InteractionMenuComponent', () => {
  let component: InteractionMenuComponent;
  let fixture: ComponentFixture<InteractionMenuComponent>;
  let iconElement: HTMLElement;

  function createMouseEvent(mouseEvent: string): MouseEvent {
    return new MouseEvent(mouseEvent, { bubbles: true, cancelable: true });
  }

  function getCollapseNodeButtonElement() {
    fixture.detectChanges();
    const iconDebugElement = fixture.debugElement.query(By.css('.collapseNodeButton'));
    if (iconDebugElement) {
      iconElement = iconDebugElement.nativeElement;
    } else {
      fail('The collapse node button was not found.');
    }
  }

  function getHideNodeButtonElement() {
    fixture.detectChanges();
    const iconDebugElement = fixture.debugElement.query(By.css('.hideNodeButton'));
    if (iconDebugElement) {
      iconElement = iconDebugElement.nativeElement;
    } else {
      fail('The hide node button was not found.');
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InteractionMenuComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InteractionMenuComponent);
    component = fixture.componentInstance;
    component.node = VisibleGraphNode.build()
    component.state = State.build({
      isInteractive: true
    })
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('CollapseNode', () => {
    beforeEach(async () => {
      getCollapseNodeButtonElement()
    })

    it('should call stopPropagation on mousedown in collapse node', () => {
      //given
      const mockMouseEvent = createMouseEvent('mousedown');
      spyOn(mockMouseEvent, 'stopPropagation').and.callThrough();

      //when
      iconElement.dispatchEvent(mockMouseEvent);

      //then
      expect(mockMouseEvent.stopPropagation).toHaveBeenCalled();
    });

    it('should call collapseNode on mouseup', () => {
      //given
      const mockMouseEvent = createMouseEvent('mouseup');
      spyOn(component, 'collapseNode').and.callThrough();
      spyOn(component.nodeCollapsed, 'emit');

      //when
      iconElement.dispatchEvent(mockMouseEvent);

      //then
      expect(component.collapseNode).toHaveBeenCalledWith(mockMouseEvent);
      expect(component.nodeCollapsed.emit).toHaveBeenCalled();
    });
  });

  describe('HideNode', () => {
    beforeEach(async () => {
      getHideNodeButtonElement()
    })

    it('should call stopPropagation on mousedown in hide node', () => {
      //given
      const mockMouseEvent = createMouseEvent('mousedown');
      spyOn(mockMouseEvent, 'stopPropagation').and.callThrough();

      //when
      iconElement.dispatchEvent(mockMouseEvent);

      //then
      expect(mockMouseEvent.stopPropagation).toHaveBeenCalled();
    });

    it('should call hiddenNode on mouseup', () => {
      //given
      const mockMouseEvent = createMouseEvent('mouseup');
      spyOn(component, 'hideNode').and.callThrough();
      spyOn(component.nodeHidden, 'emit');

      //when
      iconElement.dispatchEvent(mockMouseEvent);

      //then
      expect(component.hideNode).toHaveBeenCalledWith(mockMouseEvent);
      expect(component.nodeHidden.emit).toHaveBeenCalled();
    });

    it('should call emitPin on mouseup of pinNodeButton', () => {
      component.isPinned = false;
      fixture.detectChanges();

      spyOn(component, 'emitPin').and.callThrough();
      const pinButton = fixture.nativeElement.querySelector('.pinNodeButton');
      const event = new MouseEvent('mouseup');
      pinButton.dispatchEvent(event);

      expect(component.emitPin).toHaveBeenCalledWith(event);
    });
  });
});
