import {countParents, expand, getNodeDepth, isPackage, recursiveFind} from './GraphNode'
import type {VisibleGraphNode} from './GraphNode'
import type {GraphNode} from './GraphNode';
import {buildUniqueId} from '../common/test/TestUtils.spec';

describe('GraphNode', () => {
  describe('recursiveFind', () => {
    let rootNode: VisibleGraphNode
    let childNode1: VisibleGraphNode
    let childNode2: VisibleGraphNode
    let grandChildNode: VisibleGraphNode

    beforeEach(() => {
      grandChildNode = VisibleGraphNode.build({
        id: 'grandChild',
        level: 2,
        label: 'GrandChild'
      })
      childNode1 = VisibleGraphNode.build({
        id: 'child1',
        level: 1,
        label: 'Child1',
        visibleChildren: [grandChildNode]
      })
      childNode2 = VisibleGraphNode.build({
        id: 'child2',
        level: 1,
        label: 'Child2'
      })
      rootNode = VisibleGraphNode.build({
        id: 'root',
        level: 0,
        label: 'Root',
        visibleChildren: [childNode1, childNode2]
      })
    })

    it('finds a node at the root level', () => {
      expect(recursiveFind([rootNode], 'root')).toEqual(rootNode)
    })

    it('finds a direct child node', () => {
      expect(recursiveFind([rootNode], 'child1')).toEqual(childNode1)
    })

    it('finds a nested child node', () => {
      expect(recursiveFind([rootNode], 'grandChild')).toEqual(grandChildNode)
    })
    it('returns null for a non-existing node', () => {
      expect(recursiveFind([rootNode], 'nonExisting')).toBeNull()
    })

    it('returns null for an empty graph', () => {
      expect(recursiveFind([], 'anyId')).toBeNull()
    })
  })

  describe('expandDescendants', () => {
    it('expands nothing when there are no children', () => {
      const graphNode = GraphNode.build()

      expect(expand(graphNode)).toEqual([graphNode])
    })

    it('expands children and their children', () => {
      const grandchild = GraphNode.build()
      const child = GraphNode.build({children: [grandchild]})
      const graphNode = GraphNode.build({children: [child]})

      expect(expand(graphNode)).toEqual([
        grandchild,
        child,
        graphNode
      ])
    })
  })

  describe('getNodeDepth', () => {
    it('returns depth of 3', () => {
      const graphNode = GraphNode.build({id: 'a.b.c'})
      expect(getNodeDepth(graphNode)).toEqual(3)
    })

    it('returns depth of 1', () => {
      const graphNode = GraphNode.build({id: 'a'})
      expect(getNodeDepth(graphNode)).toEqual(1)
    })
  })

  describe('isPackage', () => {
    it('returns false if there are no children', () => {
      const graphNode = GraphNode.build({children: []})
      expect(isPackage(graphNode)).toEqual(false)
    })

    it('returns true if there are children', () => {
      const graphNode = GraphNode.build({children: [GraphNode.build()]})
      expect(isPackage(graphNode)).toEqual(true)
    })
  })

  describe('countParents', () => {
    it('returns 0 if it is a root node', () => {
      const graphNode = GraphNode.build({parent: undefined})
      expect(countParents(graphNode)).toEqual(0)
    })

    it('returns 2 if there are two parents', () => {
      const graphNode = GraphNode.build({parent: undefined})
      const child = GraphNode.build({parent: graphNode})
      const grandchild = GraphNode.build({parent: child})

      expect(countParents(grandchild)).toEqual(2)
    })
  })
})

namespace GraphNode {
  export function build(overrides: Partial<GraphNode> = {}): GraphNode {
    const defaults: GraphNode = {
      children: [],
      id: buildUniqueId(),
      label: 'id1',
      dependencies: [],
      level: 0
    }

    return { ...defaults, ...overrides }
  }
}
export { GraphNode }

namespace VisibleGraphNode {
  export function build(overrides: Partial<VisibleGraphNode> = {}): VisibleGraphNode {
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
}
export { VisibleGraphNode }