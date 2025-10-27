import { Component, EventEmitter, Input, Output, inject, OnInit } from '@angular/core';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { MatIcon, MatIconRegistry } from '@angular/material/icon';
import { VisibleGraphNode } from '../../../../../../../model/GraphNode';
import { MatButton } from '@angular/material/button';
import { NgForOf } from '@angular/common';
import { MatActionList } from '@angular/material/list';
import { GraphState } from '../../../../../../../model/GraphState';
import { DomSanitizer } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'interaction-menu',
  imports: [
    MatMenu,
    MatIcon,
    MatButton,
    MatMenuItem,
    MatMenuTrigger,
    NgForOf,
    MatActionList,
    HttpClientModule
  ],
  templateUrl: './interaction-menu.component.html',
  standalone: true,
  styleUrl: './interaction-menu.component.css'
})
export class InteractionMenuComponent implements OnInit {
  @Input() node!: VisibleGraphNode
  @Input() isPinned: boolean = false
  @Input() state!: GraphState
  @Output() nodeHidden = new EventEmitter()
  @Output() nodeCollapsed = new EventEmitter()
  @Output() childRestored = new EventEmitter<string>()
  @Output() allChildrenRestored = new EventEmitter()
  @Output() nodePinned = new EventEmitter<void>();
  @Output() nodeUnpinned = new EventEmitter<void>()

  //TODO Code Duplication with InteractionBarComponent. necessary because of error log while testing
  private matIconRegistry = inject(MatIconRegistry);
  private domSanitizer = inject(DomSanitizer);

  ngOnInit() {
    for (const iconName of ['push-pin-slashed', 'push-pin']) {
      this.matIconRegistry.addSvgIcon(iconName, this.domSanitizer.bypassSecurityTrustResourceUrl(`icons/${iconName}.svg`));
    }
  }

  hiddenChildren(): { id: string; label: string }[] {
    return this.node.hiddenChildrenIds.map(hiddenNodeId => {
      const child = this.node.children.find(child => child.id === hiddenNodeId)
      return { id: hiddenNodeId, label: child?.label ?? '' }
    })
  }

  emitPin($event: MouseEvent) {
    $event.stopPropagation();
    if (this.isPinned) {
      this.nodeUnpinned.emit();
    } else {
      this.nodePinned.emit()
    }
  }

  hideNode($event: MouseEvent) {
    $event.stopPropagation()
    this.nodeHidden.emit()
  }

  collapseNode($event: MouseEvent) {
    $event.stopPropagation()
    this.nodeCollapsed.emit()
  }

  restoreAllChildren($event: MouseEvent) {
    $event.stopPropagation()
    this.allChildrenRestored.emit()
  }

  restoreChild(hiddenChildId: string, $event: MouseEvent) {
    $event.stopPropagation()
    this.childRestored.emit(hiddenChildId)
  }

}
