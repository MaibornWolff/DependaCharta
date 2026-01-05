import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'help-popup',
  standalone: true,
  imports: [],
  templateUrl: './help-popup.component.html',
  styleUrl: './help-popup.component.css'
})
export class HelpPopupComponent {
  @Output() closePopup = new EventEmitter<void>()

  onOverlayClick(event: Event) {
    if ((event.target as HTMLElement).classList.contains('help-overlay')) {
      this.closePopup.emit()
    }
  }

  onCloseClick() {
    this.closePopup.emit()
  }
}
