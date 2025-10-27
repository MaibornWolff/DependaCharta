import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'toggle-button',
  standalone: true,
  imports: [],
  templateUrl: './toggle-button.component.html',
  styleUrl: './toggle-button.component.css'
})
export class ToggleButtonComponent {
  @Output() toggleChanged = new EventEmitter()
  @Input() isOn: boolean = true

  toggleState() {
    this.toggleChanged.emit()
  }
}
