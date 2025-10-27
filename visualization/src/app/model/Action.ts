import {GraphNode} from './GraphNode';
import {EdgeFilterType} from './EdgeFilter';

export abstract class Action {}

export class InitializeState extends Action {
  constructor(public readonly filename: string, public readonly rootNodes: GraphNode[]) { super(); }
}

export class ExpandNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class CollapseNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class ChangeFilter extends Action {
  constructor(public readonly edgeFilter: EdgeFilterType) { super(); }
}

export class ShowAllEdgesOfNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class HideAllEdgesOfNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class ToggleEdgeLabels extends Action {}

export class HideNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class RestoreNode extends Action {
  constructor(public readonly nodeIdToBeRestored: string, public readonly parentNodeId: string) { super(); }
}

export class RestoreNodes extends Action {}

export class RestoreAllChildren extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class ToggleInteractionMode extends Action {}

export class ToggleUsageTypeMode extends Action {}

export class ResetView extends Action {}

export class ToggleNodeSelection extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class EnterMultiselectMode extends Action {}

export class LeaveMultiselectMode extends Action {}

export class PinNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}

export class UnpinNode extends Action {
  constructor(public readonly nodeId: string) { super(); }
}
