import {VisibleGraphNode} from './GraphNode';
import {buildUniqueId} from '../common/test/TestUtils.spec';
import {ShallowGraphEdge} from './ShallowGraphEdge';

export function buildShallowGraphEdge(overrides: Partial<ShallowGraphEdge> = {}): ShallowGraphEdge {
  const defaults: ShallowGraphEdge = {
    id: "source-target",
    isCyclic: false,
    source: "source",
    target: "target",
    weight: 1,
    type: 'usage'
  }

  return { ...defaults, ...overrides }
}

export function buildVisibleGraphNode(overrides: Partial<VisibleGraphNode> = {}): VisibleGraphNode {
  const defaults: VisibleGraphNode = {
    children: [],
    id: buildUniqueId(),
    label: 'id1',
    dependencies: [],
    level: 0,
    visibleChildren: [],
    hiddenChildrenIds: [],
    isExpanded: false,
    isSelected: false
  }

  return { ...defaults, ...overrides }
}
