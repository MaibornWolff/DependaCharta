import { HttpClientModule } from '@angular/common/http';
import { Component, EventEmitter, Input, Output, inject, OnInit } from '@angular/core';
import { MatIcon, MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'interaction-bar',
  imports: [
    MatIcon,
    HttpClientModule,
  ],
  templateUrl: './interaction-bar.component.html',
  standalone: true,
  styleUrl: './interaction-bar.component.css'
})
export class InteractionBarComponent implements OnInit {
  @Input() nodeId!: string
  @Input() isPinned: boolean = false;
  @Output() nodeHidden = new EventEmitter()
  @Output() nodePinned = new EventEmitter<void>();
  @Output() nodeUnpinned = new EventEmitter<void>();

  //TODO Code Duplication with InteractionMenuComponent. necessary because of error log while testing
  private matIconRegistry = inject(MatIconRegistry);
  private domSanitizer = inject(DomSanitizer);

  ngOnInit() {
    for (const iconName of ['push-pin-slashed', 'push-pin']) {
      this.matIconRegistry.addSvgIcon(iconName, this.domSanitizer.bypassSecurityTrustResourceUrl(`icons/${iconName}.svg`));
    }
  }

  emitHide($event: MouseEvent) {
    $event.stopPropagation()
    this.nodeHidden.emit(this.nodeId)
  }

  emitPin($event: MouseEvent) {
    $event.stopPropagation();
    if (this.isPinned) {
      this.nodeUnpinned.emit();
    } else {
      this.nodePinned.emit()
    }
  }
}
