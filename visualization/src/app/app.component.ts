import {ChangeDetectorRef, Component, HostListener, inject, ViewChild} from '@angular/core'
import {RouterOutlet} from '@angular/router'
import {FilterComponent} from './ui/filter/filter.component'
import {JsonLoaderComponent} from './ui/json.loader/json.loader.component'
import {VersionComponent} from './ui/version/version.component'
import {convertToGraphNodes} from './adapter/analysis'
import {Action, InitializeState, EnterMultiselectMode, LeaveMultiselectMode, RestoreNodes, ToggleEdgeLabels, ResetView, ToggleInteractionMode, ToggleUsageTypeMode} from './model/Action'
import {CytoscapeComponent} from './adapter/cytoscape'
import {MouseInaccuracyDetectorComponent} from './mouse-inaccuracy-detector.component'
import {ProjectReport} from './adapter/analysis/internal/ProjectReport'
import {State} from './model/State'
import {ToggleButtonComponent} from "./ui/toggle-button/toggle-button.component";

// TODO move to better location (model?)
// TODO naming
export type StateChange = {state: State, action: Action}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, FilterComponent, JsonLoaderComponent, VersionComponent, CytoscapeComponent, MouseInaccuracyDetectorComponent, ToggleButtonComponent],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'visualization'
  @ViewChild(CytoscapeComponent)
  // cytoscapeComponent is only used in the git pipeline (cypress test)
  private cytoscapeComponent!: CytoscapeComponent
  private changeDetector = inject(ChangeDetectorRef)
  isLoading: boolean = false
  cytoscapeInitialized: boolean = true
  state: State = State.build()
  stateChange!: StateChange

  apply(action: Action) {
    this.isLoading = true
    this.state = this.state.reduce(action)
    this.stateChange = {state: this.state, action: action}
    this.isLoading = false
  }

  // Button handlers (template cannot use `new` directly)
  onRestoreNodesClick() {
    this.apply(new RestoreNodes())
  }

  onToggleEdgeLabelsClick() {
    this.apply(new ToggleEdgeLabels())
  }

  onResetViewClick() {
    this.apply(new ResetView())
  }

  onToggleInteraction() {
    this.apply(new ToggleInteractionMode())
  }

  onToggleUsage() {
    this.apply(new ToggleUsageTypeMode())
  }

  onFileLoadStart() {
    // this is a required workaround to throw away the cytoscape component and let angular reinitialize it
    // this is needed, because when cytoscape re-initializes in the element with id "cy", it throws away the two
    // child components "non-compound-node-container" and "compound-node-container" and does not reinitialize them
    this.cytoscapeInitialized = false
    this.changeDetector.detectChanges()
    this.cytoscapeInitialized = true
    this.isLoading = true
  }

  async initializeCytoscape(jsonData: ProjectReport) {
    this.apply(new InitializeState(
      jsonData.filename,
      convertToGraphNodes(jsonData)
    ))
  }

  @HostListener('document:keydown.shift', [])
  enterMultiselectMode() {
    this.apply(new EnterMultiselectMode())
  }

  @HostListener('document:keyup.shift', [])
  leaveMultiselectMode() {
    this.apply(new LeaveMultiselectMode())
  }
}
