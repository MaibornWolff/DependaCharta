import {ElementRef} from '@angular/core';

export interface ResizeConfig {
  overlaySelector: string;
  minWidth: number;
  minHeight: number;
  defaultWidth: number;
  defaultHeight: number;
  horizontalSign: 1 | -1;
  verticalSign: 1 | -1;
}

type ResizeAxis = 'horizontal' | 'vertical' | 'both';

export class ResizablePanel {
  private isResizing = false;
  private resizeAxis: ResizeAxis | null = null;
  private startX = 0;
  private startY = 0;
  private startWidth = 0;
  private startHeight = 0;

  private readonly boundOnMouseMove = this.onResizeMove.bind(this);
  private readonly boundOnMouseUp = this.onResizeEnd.bind(this);

  constructor(
    private readonly elementRef: ElementRef,
    private readonly config: ResizeConfig
  ) {}

  applyDefaultDimensions(): void {
    const overlay = this.getOverlay();
    if (overlay) {
      overlay.style.width = `${this.config.defaultWidth}px`;
      overlay.style.height = `${this.config.defaultHeight}px`;
    }
  }

  startResize(event: MouseEvent, axis: ResizeAxis): void {
    event.preventDefault();
    event.stopPropagation();

    this.isResizing = true;
    this.resizeAxis = axis;
    this.startX = event.clientX;
    this.startY = event.clientY;

    const overlay = this.getOverlay();
    if (overlay) {
      const rect = overlay.getBoundingClientRect();
      this.startWidth = rect.width;
      this.startHeight = rect.height;
    }

    document.addEventListener('mousemove', this.boundOnMouseMove);
    document.addEventListener('mouseup', this.boundOnMouseUp);
  }

  destroy(): void {
    this.removeResizeListeners();
  }

  private onResizeMove(event: MouseEvent): void {
    if (!this.isResizing || !this.resizeAxis) return;

    const overlay = this.getOverlay();
    if (!overlay) return;

    const deltaX = event.clientX - this.startX;
    const deltaY = event.clientY - this.startY;

    if (this.resizeAxis === 'horizontal' || this.resizeAxis === 'both') {
      const newWidth = Math.max(this.config.minWidth, this.startWidth + deltaX * this.config.horizontalSign);
      overlay.style.width = `${newWidth}px`;
    }

    if (this.resizeAxis === 'vertical' || this.resizeAxis === 'both') {
      const newHeight = Math.max(this.config.minHeight, this.startHeight + deltaY * this.config.verticalSign);
      overlay.style.height = `${newHeight}px`;
    }
  }

  private onResizeEnd(): void {
    this.isResizing = false;
    this.resizeAxis = null;
    this.removeResizeListeners();
  }

  private removeResizeListeners(): void {
    document.removeEventListener('mousemove', this.boundOnMouseMove);
    document.removeEventListener('mouseup', this.boundOnMouseUp);
  }

  private getOverlay(): HTMLElement | null {
    return this.elementRef.nativeElement.querySelector(this.config.overlaySelector);
  }
}
