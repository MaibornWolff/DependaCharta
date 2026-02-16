import {ChangeDetectorRef, Component, HostListener, inject, ViewChild} from '@angular/core'
import {RouterOutlet} from '@angular/router'
import {FilterComponent} from './ui/filter/filter.component'
import {JsonLoaderComponent} from './ui/json.loader/json.loader.component'
import {VersionComponent} from './ui/version/version.component'
import {convertToGraphNodes} from './adapter/analysis'
import {Action} from './model/Action'
import {CytoscapeComponent} from './adapter/cytoscape'
import {MouseInaccuracyDetectorComponent} from './mouse-inaccuracy-detector.component'
import {ProjectReport} from './adapter/analysis/internal/ProjectReport'
import {State} from './model/State'
import {ToggleButtonComponent} from "./ui/toggle-button/toggle-button.component";
import {FeedbackEdgesListComponent} from './ui/feedback-edges-list/feedback-edges-list.component';
import {HiddenNodesListComponent, RestoreNodeEvent} from './ui/hidden-nodes-list/hidden-nodes-list.component';
import {ExplorerComponent} from './ui/explorer/explorer.component';
import {HelpPopupComponent} from './ui/help-popup/help-popup.component';
import {FeedbackListEntry, ShallowEdge} from './model/Edge';
import {GraphNode} from './model/GraphNode';
import {EdgeFilterType} from './model/EdgeFilter';

// TODO move to better location (model?)
// TODO naming
export type StateChange = {state: State, action: Action}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, FilterComponent, JsonLoaderComponent, VersionComponent, CytoscapeComponent, MouseInaccuracyDetectorComponent, ToggleButtonComponent, FeedbackEdgesListComponent, HiddenNodesListComponent, ExplorerComponent, HelpPopupComponent],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'visualization'
  @ViewChild(CytoscapeComponent)
  // cytoscapeComponent is only used in the git pipeline (cypress test)
  private cytoscapeComponent!: CytoscapeComponent
  @ViewChild(FeedbackEdgesListComponent)
  private readonly feedbackEdgesListComponent?: FeedbackEdgesListComponent
  @ViewChild(HiddenNodesListComponent)
  private readonly hiddenNodesListComponent?: HiddenNodesListComponent
  @ViewChild(ExplorerComponent)
  private readonly explorerComponent?: ExplorerComponent
  @ViewChild(JsonLoaderComponent)
  private readonly jsonLoaderComponent!: JsonLoaderComponent
  private changeDetector = inject(ChangeDetectorRef)
  isLoading: boolean = false
  cytoscapeInitialized: boolean = true
  state: State = State.build()
  stateChange!: StateChange
  cachedRootNodes: GraphNode[] = []
  cachedFeedbackEdges: ShallowEdge[] = []
  cachedHiddenNodes: GraphNode[] = []

  apply(action: Action) {
    this.isLoading = true
    this.state = this.state.reduce(action)
    this.stateChange = {state: this.state, action: action}
    this.cachedRootNodes = this.state.getRootNodes()
    this.cachedFeedbackEdges = this.state.getVisibleFeedbackEdges()
    this.cachedHiddenNodes = this.state.getHiddenNodes()
    this.isLoading = false
  }

  // Button handlers (template cannot use `new` directly)
  onRestoreNodesClick() {
    this.apply(new Action.RestoreNodes())
  }

  onToggleEdgeLabels() {
    this.apply(new Action.ToggleEdgeLabels())
  }

  onResetViewClick() {
    this.apply(new Action.ResetView())
  }

  onToggleInteraction() {
    this.apply(new Action.ToggleInteractionMode())
  }

  onToggleUsage() {
    this.apply(new Action.ToggleUsageTypeMode())
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
    this.apply(new Action.InitializeState(
      jsonData.filename,
      convertToGraphNodes(jsonData)
    ))
  }

  @HostListener('document:keydown.shift', [])
  enterMultiselectMode() {
    this.apply(new Action.EnterMultiselectMode())
  }

  @HostListener('document:keyup.shift', [])
  leaveMultiselectMode() {
    this.apply(new Action.LeaveMultiselectMode())
    this.apply(new Action.ResetMultiselection())
  }

  @HostListener('window:blur')
  onWindowBlur() {
    this.apply(new Action.LeaveMultiselectMode())
  }

  // Keyboard shortcuts
  showHelpPopup = false

  private isInputFocused(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement | null
    if (!target) return false
    return target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT'
  }

  @HostListener('document:keydown.r', ['$event'])
  onResetViewShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ResetView())
  }

  @HostListener('document:keydown.l', ['$event'])
  onToggleLabelsShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ToggleEdgeLabels())
  }

  @HostListener('document:keydown.i', ['$event'])
  onToggleInteractionShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ToggleInteractionMode())
  }

  @HostListener('document:keydown.u', ['$event'])
  onToggleUsageShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ToggleUsageTypeMode())
  }

  @HostListener('document:keydown.z', ['$event'])
  onRestoreNodesShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.RestoreNodes())
  }

  @HostListener('document:keydown.f', ['$event'])
  onToggleFeedbackPanelShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    if (!this.state.isInteractive) return
    event.preventDefault()
    this.feedbackEdgesListComponent?.toggleExpanded()
  }

  @HostListener('document:keydown.e', ['$event'])
  onToggleExplorerPanelShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    if (!this.state.isInteractive) return
    event.preventDefault()
    this.explorerComponent?.toggleExpanded()
  }

  @HostListener('document:keydown.h', ['$event'])
  onToggleHiddenNodesPanelShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    if (!this.state.isInteractive) return
    event.preventDefault()
    this.hiddenNodesListComponent?.toggleExpanded()
  }

  @HostListener('document:keydown.0', ['$event'])
  onFilterNoneShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ChangeFilter(EdgeFilterType.NONE))
  }

  @HostListener('document:keydown.1', ['$event'])
  onFilterAllShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ChangeFilter(EdgeFilterType.ALL))
  }

  @HostListener('document:keydown.2', ['$event'])
  onFilterCyclesShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ChangeFilter(EdgeFilterType.CYCLES_ONLY))
  }

  @HostListener('document:keydown.3', ['$event'])
  onFilterLeafFeedbackShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ChangeFilter(EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY))
  }

  @HostListener('document:keydown.4', ['$event'])
  onFilterAllFeedbackShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.apply(new Action.ChangeFilter(EdgeFilterType.ALL_FEEDBACK_EDGES))
  }

  @HostListener('document:keydown.o', ['$event'])
  onOpenFileShortcut(event: KeyboardEvent) {
    if (this.isInputFocused(event)) return
    event.preventDefault()
    this.jsonLoaderComponent.openFileDialog()
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (event.key === '?' || (event.shiftKey && event.key === '/')) {
      if (this.isInputFocused(event)) return
      event.preventDefault()
      this.showHelpPopup = !this.showHelpPopup
    }
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeShortcut(event: KeyboardEvent) {
    if (this.showHelpPopup) {
      event.preventDefault()
      this.showHelpPopup = false
    }
  }

  onEdgeClicked(edge: ShallowEdge): void {
    this.apply(new Action.NavigateToEdge(edge.source, edge.target))
  }

  onRestoreHiddenNode(event: RestoreNodeEvent): void {
    this.apply(new Action.RestoreNode(event.nodeId, event.parentNodeId))
  }

  onRestoreAllHidden(): void {
    this.apply(new Action.RestoreAllHiddenNodes())
  }

  onExplorerNavigate(nodeId: string): void {
    this.apply(new Action.NavigateToNode(nodeId))
  }

  onExplorerHide(nodeId: string): void {
    this.apply(new Action.HideNode(nodeId))
  }

  onExplorerHideMany(nodeIds: string[]): void {
    this.apply(new Action.HideNodes(nodeIds))
  }

  onExplorerHover(nodeId: string): void {
    this.apply(new Action.ShowAllEdgesOfNode(nodeId))
  }

  onExplorerUnhover(): void {
    this.apply(new Action.ClearNodeHover())
  }

  onGroupClicked(group: FeedbackListEntry): void {
    // Group source/target are already valid container paths
    this.apply(new Action.NavigateToEdge(group.source, group.target))
  }
}
