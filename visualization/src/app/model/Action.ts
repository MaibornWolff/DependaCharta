import {GraphNode} from './GraphNode';
import {EdgeFilterType} from './EdgeFilter';

export namespace Action {

  export class InitializeState {
    constructor(
      readonly filename: string,
      readonly rootNodes: GraphNode[]
    ) {}
  }

  export class ExpandNode {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class CollapseNode {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class ChangeFilter {
    constructor(
      readonly edgeFilter: EdgeFilterType
    ) {}
  }

  export class ShowAllEdgesOfNode {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class ClearNodeHover {}

  export class ToggleEdgeLabels {}

  export class HideNode {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class RestoreNode {
    constructor(
      readonly nodeIdToBeRestored: string,
      readonly parentNodeId: string
    ) {}
  }

  export class RestoreNodes {}

  export class RestoreAllChildren {
    constructor(readonly nodeId: string) {}
  }

  export class RestoreAllHiddenNodes {}

  export class ToggleInteractionMode {}

  export class ToggleUsageTypeMode {}

  export class ResetView {}

  export class ToggleNodeSelection {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class EnterMultiselectMode {}

  export class LeaveMultiselectMode {}

  export class ResetMultiselection {}

  export class PinNode {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class UnpinNode {
    constructor(
      readonly nodeId: string
    ) {}
  }

  export class NavigateToEdge {
    constructor(
      readonly sourceNodeId: string,
      readonly targetNodeId: string
    ) {}
  }

  export class NavigateToNode {
    constructor(
      readonly nodeId: string
    ) {}
  }
}

export type Action =
  | Action.InitializeState
  | Action.ExpandNode
  | Action.CollapseNode
  | Action.ChangeFilter
  | Action.ShowAllEdgesOfNode
  | Action.ClearNodeHover
  | Action.ToggleEdgeLabels
  | Action.HideNode
  | Action.RestoreNode
  | Action.RestoreNodes
  | Action.RestoreAllChildren
  | Action.ToggleInteractionMode
  | Action.ToggleUsageTypeMode
  | Action.ResetView
  | Action.ToggleNodeSelection
  | Action.EnterMultiselectMode
  | Action.LeaveMultiselectMode
  | Action.ResetMultiselection
  | Action.PinNode
  | Action.UnpinNode
  | Action.RestoreAllHiddenNodes
  | Action.NavigateToEdge
  | Action.NavigateToNode
