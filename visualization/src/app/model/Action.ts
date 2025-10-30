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
    public readonly filename: string,
    public readonly rootNodes: GraphNode[]
  ) {}
}

export class ExpandNode {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class CollapseNode {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class ChangeFilter {
  constructor(
    public readonly edgeFilter: EdgeFilterType
  ) {}
}

export class ShowAllEdgesOfNode {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class HideAllEdgesOfNode {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class ToggleEdgeLabels {}

export class HideNode {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class RestoreNode {
  constructor(
    public readonly nodeIdToBeRestored: string,
    public readonly parentNodeId: string
  ) {}
}

export class RestoreNodes {}

export class RestoreAllChildren {
  constructor(public readonly nodeId: string) {}
}

export class ToggleInteractionMode {}

export class ToggleUsageTypeMode {}

export class ResetView {}

export class ToggleNodeSelection {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class EnterMultiselectMode {}

export class LeaveMultiselectMode {}

export class PinNode {
  constructor(
    public readonly nodeId: string
  ) {}
}

export class UnpinNode {
  constructor(
    public readonly nodeId: string
  ) {}
}
