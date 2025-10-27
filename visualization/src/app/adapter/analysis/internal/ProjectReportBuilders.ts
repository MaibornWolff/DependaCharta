import {ProjectNode} from './ProjectReport';
import {buildUniqueId} from '../../../common/test/TestUtils.spec';

export function buildProjectNode(overrides: Partial<ProjectNode> = {}): ProjectNode {
  const defaults: ProjectNode = {
    name: buildUniqueId(),
    level: 0,
    children: [],
    containedLeaves: [],
    containedInternalDependencies: {},
  }

  return { ...defaults, ...overrides }
}
