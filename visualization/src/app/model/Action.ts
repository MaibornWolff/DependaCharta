import {GraphNode} from './GraphNode';
import {EdgeFilterType} from './EdgeFilter';

export type Action =
  | InitializeState
  | ExpandNode
  | CollapseNode
  | ChangeFilter
  | ShowAllEdgesOfNode
  | HideAllEdgesOfNode
  | ToggleEdgeLabels
  | HideNode
  | RestoreNode
  | RestoreNodes
  | ToggleInteractionMode
  | ToggleUsageTypeMode
  | ResetView
  | ToggleNodeSelection
  | EnterMultiselectMode
  | LeaveMultiselectMode
  | PinNode
  | UnpinNode

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

export class HideAllEdgesOfNode {
  constructor(
    readonly nodeId: string
  ) {}
}

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
