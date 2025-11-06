import {getChildren, getNodeId} from './ProjectReport';
import {buildUniqueId} from '../../../common/test/TestUtils.spec';
import type {ProjectNode} from './ProjectReport';

describe('ProjectReport', () => {
  describe('getChildren', () => {
    it('returns children if node is root node', () => {
      const childrenOfNode1 = ProjectNode.build()
      const node1 = ProjectNode.build({
        name: 'name1',
        children: [childrenOfNode1]
      })
      const node2 = ProjectNode.build({
        name: 'name2'
      })
      const nodesToCheck = [node1, node2]
      const children = getChildren('name1', nodesToCheck, '')
      expect(children).toEqual(node1.children)
    })

    it('returns empty array if nodeId is not found', () => {
      const node = ProjectNode.build({
        name: 'node'
      })
      const nodesToCheck = [node]
      const children = getChildren('nonExistingNode', nodesToCheck, '')
      expect(children).toEqual([])
    })

    it('recursively search children', () => {
      const grandChild = ProjectNode.build()
      const child = ProjectNode.build({
        name: 'childName',
        children: [grandChild]
      })
      const node = ProjectNode.build({
        name: 'parentName',
        children: [child]
      })
      const nodesToCheck = [node]
      const children = getChildren('parentName.childName', nodesToCheck, '')
      expect(children).toEqual([grandChild])
    })
  })

  describe('getNodeId', () => {
    it('returns leafId if exists', () => {
      const containedInternalDependencies = {
        '': {
          weight: 1,
          isCyclic: false,
          type: 'usage',
          isPointingUpwards: false
        }
      }
      const projectNode = ProjectNode.build({
        leafId: 'leafId',
        containedInternalDependencies: containedInternalDependencies
      })
      const nodeId = getNodeId(projectNode, '')
      expect(nodeId).toEqual(projectNode.leafId+":leaf"!);
    });

    it('returns name of package if node is not a leaf and has no parent', () => {
      const containedInternalDependencies = {
        '': {
          weight: 1,
          isCyclic: false,
          type: 'usage',
          isPointingUpwards: false
        }
      }
      const projectNode = ProjectNode.build({
        containedInternalDependencies: containedInternalDependencies
      })
      const nodeId = getNodeId(projectNode, '')
      expect(nodeId).toEqual(projectNode.name);
    });

    it('Prefixes nodeId with package name if node is not a leaf and has a parent', () => {
      const containedInternalDependencies = {
        '': {
          weight: 1,
          isCyclic: false,
          type: 'usage',
          isPointingUpwards: false
        }
      }
      const parentPath = 'parentPath'
      const projectNode = ProjectNode.build({
        containedInternalDependencies: containedInternalDependencies
      })
      const nodeId = getNodeId(projectNode, parentPath)
      const expectedNodeId = parentPath + "." + projectNode.name

      expect(nodeId).toEqual(expectedNodeId);
    });
  })
})

namespace ProjectNode {
  export function build(overrides: Partial<ProjectNode> = {}): ProjectNode {
    const defaults: ProjectNode = {
      name: buildUniqueId(),
      level: 0,
      children: [],
      containedLeaves: [],
      containedInternalDependencies: {},
    }

    return { ...defaults, ...overrides }
  }
}
export { ProjectNode }
