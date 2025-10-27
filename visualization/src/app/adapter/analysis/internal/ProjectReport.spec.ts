import {buildProjectNode} from './ProjectReportBuilders';
import {getChildren, getNodeId} from './ProjectReport';

describe('ProjectReport', () => {
  describe('getChildren', () => {
    it('returns children if node is root node', () => {
      const childrenOfNode1 = buildProjectNode()
      const node1 = buildProjectNode({
        name: 'name1',
        children: [childrenOfNode1]
      })
      const node2 = buildProjectNode({
        name: 'name2'
      })
      const nodesToCheck = [node1, node2]
      const children = getChildren('name1', nodesToCheck, '')
      expect(children).toEqual(node1.children)
    })

    it('returns empty array if nodeId is not found', () => {
      const node = buildProjectNode({
        name: 'node'
      })
      const nodesToCheck = [node]
      const children = getChildren('nonExistingNode', nodesToCheck, '')
      expect(children).toEqual([])
    })

    it('recursively search children', () => {
      const grandChild = buildProjectNode()
      const child = buildProjectNode({
        name: 'childName',
        children: [grandChild]
      })
      const node = buildProjectNode({
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
          type: 'usage'
        }
      }
      const projectNode = buildProjectNode({
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
          type: 'usage'

        }
      }
      const projectNode = buildProjectNode({
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
          type: 'usage'

        }
      }
      const parentPath = 'parentPath'
      const projectNode = buildProjectNode({
        containedInternalDependencies: containedInternalDependencies
      })
      const nodeId = getNodeId(projectNode, parentPath)
      const expectedNodeId = parentPath + "." + projectNode.name

      expect(nodeId).toEqual(expectedNodeId);
    });
  })
})
