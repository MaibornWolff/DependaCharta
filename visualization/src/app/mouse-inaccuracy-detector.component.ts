import {Component, HostListener, OnInit} from '@angular/core';
import {
  MatSnackBar,
  MatSnackBarAction,
  MatSnackBarActions,
  MatSnackBarLabel,
  MatSnackBarRef
} from '@angular/material/snack-bar';
import {
  MatDialog,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';
import {Core} from 'cytoscape';
import { CytoscapeService } from './adapter/cytoscape/internal/cytoscape.service';

// To properly mock reload
export class BrowserGlobals {
  static reloadBrowser() { window.location.reload() }
}

@Component({
  selector: 'mouse-inaccuracy-detector',
  template: '',
  standalone: true
})
export class MouseInaccuracyDetectorComponent implements OnInit{

  // Configuration
  maximumSampleSize = 20 // defines the number of event samples that are evaluated to calculate the median confidence
  minimalSampleSize = 10 // a minimum number of samples to be taken to make a guess what the user uses
  mouseConfidenceThreshold = 0 // will switch the current guess to "Mouse" if the median confidence drops below it or is equal to
  touchpadConfidenceThreshold = 1 // will switch the current guess to "Touchpad" if the median confidence rises over it or is equal to
  confidenceWeightOfIntegerComponent = 1 // positive integer. Scales the importance/weight of the assumption that a touchpad emits integers rather than non integer values
  confidenceWeightOfDeltaXNotZeroComponent = 2 // positive integer. Scales the importance/weight of the assumption that a touchpad user never scrolls perfectly straight up/down but rather incorporates a left/right movement
  warningTimoutInSeconds = 12
  zoomJumpingTolerance = 0.1 // The zoom jump bug makes the zoom level jump from min to max. If the algorithm detects a zoom level delta of MAX minus MIN then it triggers the warning. The tolerance makes the trigger delta a bit smaller to adjust for rounding errors.

  // State
  touchpadConfidenceTrail: number[] = []
  touchpadConfidenceMedian: number | undefined
  guessedInputDevice: InputDevice | undefined
  lastZoom = 1

  constructor(public snackBar: MatSnackBar, public dialog: MatDialog, public cytoscapeService: CytoscapeService) {}

  ngOnInit() {
    this.cytoscapeService.panOrZoom
      .subscribe(core => this.detectZoomJumping(core))
  }

  /*
  Example:

  min zoom = 0.5
  max zoom = 3
  tolerance = 0.1
  => allowed zoom delta per zoom = 3 - 0.5 - 0.1 = 2.4

  1) zoom from 2 to 2.5
  zoom delta = 0.5
  no warning bc 0.5 < 2.4

  1) zoom from 0.6 to 3
  zoom delta = 2.4
  warning bc 2.4 >= 2.4
  */
  private detectZoomJumping(cy: Core) {
    const faultyZoomDelta = cy.maxZoom() - cy.minZoom() - this.zoomJumpingTolerance
    const currentZoomDelta = Math.max(cy.zoom(), this.lastZoom) - Math.min(cy.zoom(), this.lastZoom)
    const detectedZoomJumping = currentZoomDelta >= faultyZoomDelta

    this.lastZoom = cy.zoom()

    if(detectedZoomJumping) {
      this.warnUser()
    }
  }

  @HostListener('document:mousewheel', ['$event'])
  @HostListener('document:DOMMouseScroll', ['$event'])
  onWindowScroll(e: WheelEvent) {
    this.updateConfidenceTrail(e)
    this.updateGuess()
  }

  private updateGuess() {
    if (this.guessedInputDevice !== undefined) { // an input device has already been guessed
      if (this.switchInputDevice(this.guessedInputDevice)) {
        console.log("Switch to " + this.guessedInputDevice + " detected!")
        this.warnUser()
      }
    } else if (this.touchpadConfidenceMedian !== undefined) { // a median has already been calculated
      for (const inputDevice of [InputDevice.MOUSE, InputDevice.TOUCHPAD]) {
        if (this.switchInputDevice(inputDevice)) {
          console.log("Assuming initial device is " + this.guessedInputDevice + "!")
        }
      }
    }
  }

  private switchInputDevice(inputDevice: InputDevice): boolean {
    if (this.isBadGuess(inputDevice)) {
      this.guessedInputDevice = InputDevice.invert(inputDevice)
      return true
    }

    return false
  }

  private isBadGuess(inputDevice: InputDevice): boolean {
    switch (inputDevice) {
      case InputDevice.MOUSE:
        return this.touchpadConfidenceMedian! >= this.touchpadConfidenceThreshold
      case InputDevice.TOUCHPAD:
        return this.touchpadConfidenceMedian! <= this.mouseConfidenceThreshold
    }
  }

  private warnUser() {
    const snackBarRef = this.snackBar.openFromComponent(
      WarningSnackbarComponent,
      { duration: this.warningTimoutInSeconds * 1000 }
    );
    snackBarRef.onAction().subscribe(() => {
      this.dialog.open(WarningDetailsDialog);
    });
  }

  private updateConfidenceTrail(e: WheelEvent) {
    const touchpadConfidence = this.calculateTouchpadConfidence(e.deltaY, e.deltaX)
    this.touchpadConfidenceTrail = this.appendTrail(this.touchpadConfidenceTrail, touchpadConfidence)
    if (this.touchpadConfidenceTrail.length >= this.minimalSampleSize) {
      this.touchpadConfidenceMedian = median(this.touchpadConfidenceTrail)
    }
  }

  private calculateTouchpadConfidence(deltaY: number, deltaX: number) {
    const isIntegerComponent = Number.isInteger(deltaY) // touchpads report integers on a macbook
    const isDeltaXNotZeroComponent = Math.abs(deltaX) != 0 // touchpads usually also scroll left and right while scrolling up and down

    let confidence = 0;
    if (isIntegerComponent) confidence = confidence + this.confidenceWeightOfIntegerComponent;
    if (isDeltaXNotZeroComponent) confidence = confidence + this.confidenceWeightOfDeltaXNotZeroComponent;
    return confidence / (this.confidenceWeightOfIntegerComponent + this.confidenceWeightOfDeltaXNotZeroComponent);
  }

  private appendTrail(trail: number[], value: number) {
    trail.push(value)
    if (trail.length > this.maximumSampleSize) {
      trail.shift()
    }
    return trail
  }
}

/*
[1] ⇒ 1
[1, 2] ⇒ 1.5
[1, 2, 3] ⇒ 2
[1, 2, 3, 4] ⇒ 2.5
[1000, 333, 211, 5] ⇒ 272
*/
function median(values: number[]): number {
  values = [...values].sort((a, b) => a - b)
  const half = Math.floor(values.length / 2)
  return (values.length % 2
      ? values[half]
      : (values[half - 1] + values[half]) / 2
  )
}

export enum InputDevice {
  MOUSE = 'mouse',
  TOUCHPAD = 'touchpad'
}

export namespace InputDevice {
  export function invert(inputDevice: InputDevice): InputDevice {
    switch (inputDevice) {
      case InputDevice.MOUSE: return InputDevice.TOUCHPAD
      case InputDevice.TOUCHPAD: return InputDevice.MOUSE
    }
  }
}

@Component({
  selector: 'warning-snackbar',
  standalone: true,
  template: `
    <span matSnackBarLabel>
      Zoom anomaly detected! To fix any zoom issues you can reload the page.
    </span>
    <span matSnackBarActions>
      <button matButton matSnackBarAction (click)="reload()">Reload</button>
      <button matButton matSnackBarAction (click)="showDetails()">Details</button>
    </span>
  `,
  styles: `
    :host {
      display: flex;
    }
  `,
  imports: [
    MatButton,
    MatSnackBarAction,
    MatSnackBarActions,
    MatSnackBarLabel
  ]
})
export class WarningSnackbarComponent {

  constructor(public dialog: MatDialog, public snackbar: MatSnackBarRef<any>) { }

  showDetails() {
    this.snackbar.dismiss()
    this.dialog.open(WarningDetailsDialog);
  }

  reload() {
    this.snackbar.dismiss()
    BrowserGlobals.reloadBrowser()
  }
}

@Component({
  selector: 'warning-details-dialog',
  template: `
    <h2 mat-dialog-title>Reload needed. Do you want to reload?</h2>
    <mat-dialog-content>
      Codegraph is facing difficulties with switching Input devices. If you switch between Touchpad and Mouse, zoom is not working properly. We kindly ask you to excuse this bug. We are already working hard on fixing it and hope to release a fix as soon as possible. In the meantime we can only offer you to reload the application without any loss of work and suggest you to avoid switching your input device while using Codegraph.
    </mat-dialog-content>
    <mat-dialog-actions>
      <button matButton mat-dialog-close>Cancel</button>
      <button matButton (click)="BrowserGlobals.reloadBrowser()" cdkFocusInitial>Reload</button>
    </mat-dialog-actions>
  `,
  imports: [
    MatDialogContent,
    MatDialogActions,
    MatDialogTitle,
    MatButton,
    MatDialogClose
  ],
  standalone: true
})
export class WarningDetailsDialog {
  constructor(public dialogRef: MatDialogRef<WarningDetailsDialog>) {}

  protected readonly BrowserGlobals = BrowserGlobals;
}
