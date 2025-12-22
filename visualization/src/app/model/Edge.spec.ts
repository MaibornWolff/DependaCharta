import {Edge, ShallowEdge} from './Edge';
import {EdgeFilterType} from './EdgeFilter';
import {State} from './State';
import {VisibleGraphNode} from './GraphNode.spec';

describe('Edge', () => {
  describe('createEdges', () => {
    it('should create edge between two leaves', () => {
      // given
      const leafNode1Id = 'leafNode1';
      const leafNode2Id = 'leafNode2';
      const leafNode1 = VisibleGraphNode.build({
        id: leafNode1Id
      })
      const leafNode2 = VisibleGraphNode.build({
        id: leafNode2Id,
        dependencies: [ShallowEdge.build({
          source: leafNode2Id,
          target: leafNode1Id
        })]
      })
      const state = State.fromRootNodes([leafNode1, leafNode2]);

      // when
      const edges = state.createEdges([leafNode1, leafNode2]);

      // then
      const expectedEdge = Edge.build({
        id: leafNode2Id + '-' + leafNode1Id,
        source: leafNode2,
        target: leafNode1,
        isCyclic: false,
        isPointingUpwards: false,
        weight: 1,
        type: 'usage'
      })
      expect(edges.length).toEqual(1);
      expect(edges[0]).toEqual(expectedEdge);
    })

    it('creates multiple edges for a leaf node', (() => {
      // given
      const leafNode1Id = 'leafNode1';
      const leafNode2Id = 'leafNode2';
      const leafNode3Id = "leafNode3"
      const leafNode1 = VisibleGraphNode.build({
        id: leafNode1Id
      })
      const leafNode2 = VisibleGraphNode.build({
        id: leafNode2Id
      })
      const leafNode3 = VisibleGraphNode.build({
        id: leafNode3Id,
        dependencies: [leafNode1Id, leafNode2Id].map(target => ShallowEdge.build({
          source: leafNode3Id,
          target: target}))
      })
      const state = State.fromRootNodes([leafNode1, leafNode2, leafNode3]);

      // when
      const edges = state.createEdges([leafNode1, leafNode2, leafNode3])

      // then
      expect(edges.length).toEqual(2)
    }))

    it('creates an edge between a leaf and a node if the original leaf target is collapsed', () => {
      // given
      const parentNodeId = 'parentNode';
      const collapsedLeafNodeId = parentNodeId + '.collapsedLeaf';
      const expandedLeafNodeId = "expandedLeaf"
      const parentNode = VisibleGraphNode.build({
        id: parentNodeId,
        visibleChildren: []
      })
      const collapsedLeaf = VisibleGraphNode.build({
        id: collapsedLeafNodeId,
        parent: parentNode
      })
      const expandedLeaf = VisibleGraphNode.build({
        id: expandedLeafNodeId,
        dependencies: [ShallowEdge.build({
          source: expandedLeafNodeId,
          target: collapsedLeafNodeId
        })]
      })

      const state = State.fromRootNodes([parentNode, collapsedLeaf, expandedLeaf]).copy({ expandedNodeIds: [] })

      // when
      const edges = state.createEdges([parentNode, collapsedLeaf, expandedLeaf])

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].source.id).toEqual(expandedLeaf.id)
      expect(edges[0].target.id).toEqual(parentNode.id)
    })

    it('preserves isPointingUpwards when target node is collapsed', () => {
      // given
      const parentNodeId = 'parentNode';
      const collapsedLeafNodeId = parentNodeId + '.collapsedLeaf';
      const expandedLeafNodeId = "expandedLeaf"
      const parentNode = VisibleGraphNode.build({
        id: parentNodeId,
        visibleChildren: []
      })
      const collapsedLeaf = VisibleGraphNode.build({
        id: collapsedLeafNodeId,
        parent: parentNode
      })
      const expandedLeaf = VisibleGraphNode.build({
        id: expandedLeafNodeId,
        dependencies: [ShallowEdge.build({
          source: expandedLeafNodeId,
          target: collapsedLeafNodeId,
          isPointingUpwards: true  // Original edge points upwards
        })]
      })

      const state = State.fromRootNodes([parentNode, collapsedLeaf, expandedLeaf]).copy({ expandedNodeIds: [] })

      // when
      const edges = state.createEdges([parentNode, collapsedLeaf, expandedLeaf])

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].source.id).toEqual(expandedLeaf.id)
      expect(edges[0].target.id).toEqual(parentNode.id)
      expect(edges[0].isPointingUpwards).toEqual(true)  // Should be preserved
    })

    it('preserves isPointingUpwards when source node with nested children is collapsed', () => {
      // given: grandparent.parent.child -> otherNode (with isPointingUpwards=true)
      // when: grandparent is collapsed
      // then: edge from grandparent -> otherNode should preserve isPointingUpwards=true
      const grandparentId = 'grandparent';
      const parentId = grandparentId + '.parent';
      const childId = parentId + '.child';
      const otherNodeId = 'otherNode';

      const grandparent = VisibleGraphNode.build({
        id: grandparentId,
        visibleChildren: []
      })
      const parent = VisibleGraphNode.build({
        id: parentId,
        parent: grandparent
      })
      const child = VisibleGraphNode.build({
        id: childId,
        parent: parent,
        dependencies: [ShallowEdge.build({
          source: childId,
          target: otherNodeId,
          isPointingUpwards: true,  // Original edge points upwards
          weight: 5
        })]
      })
      parent.children = [child]
      grandparent.children = [parent]
      
      const otherNode = VisibleGraphNode.build({
        id: otherNodeId
      })

      const state = State.fromRootNodes([grandparent, parent, child, otherNode]).copy({ expandedNodeIds: [] })

      // when - only pass visible nodes (grandparent and otherNode, not the collapsed children)
      const edges = state.createEdges([grandparent, otherNode])

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].source.id).toEqual(grandparent.id)
      expect(edges[0].target.id).toEqual(otherNode.id)
      expect(edges[0].isPointingUpwards).toEqual(true)  // Should be preserved through multi-level collapse
      expect(edges[0].weight).toEqual(5)  // Weight should be preserved
    })

    it('does not create edges for expanded packages', () => {
      // given
      const parentNodeId = 'parentNode'
      const childNodeId = parentNodeId + '.childNode'
      const otherNode1Id = "otherNode1"
      const otherNode2Id = "otherNode2"

      const parentNode = VisibleGraphNode.build({
        id: parentNodeId,
        dependencies: [ShallowEdge.build({
          source: parentNodeId,
          target: otherNode2Id
        })]
      })
      const childNode = VisibleGraphNode.build({
        id: childNodeId,
        parent: parentNode,
        dependencies: [ShallowEdge.build({
          source: childNodeId,
          target: otherNode1Id
        })]
      })
      parentNode.visibleChildren = [childNode]
      const otherNode1 = VisibleGraphNode.build({
        id: otherNode1Id
      })
      const otherNode2 = VisibleGraphNode.build({
        id: otherNode2Id
      })

      const allNodes = [parentNode, childNode, otherNode1, otherNode2];
      const state = State.fromRootNodes(allNodes).copy({ expandedNodeIds: [parentNodeId] });

      // when
      const edges = state.createEdges(allNodes)

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].source.id).toEqual(childNodeId)
      expect(edges[0].target.id).toEqual(otherNode1Id)
    })

    it('Does not create edge between a package and a parent', () => {
      // given
      const parentNodeId = "parent"
      const childNodeId = parentNodeId + '.child1'
      // By choosing a nonexistent child node, the algorithm looks for the node,
      // does not find it and then tries again with the parent of the nonexistent child node which is our parent node
      const nonExistentChildNodeId = parentNodeId + '.child2'

      const parentNode = VisibleGraphNode.build({
        id: parentNodeId
      })
      const childNode = VisibleGraphNode.build({
        id: childNodeId,
        parent: parentNode,
        dependencies: [ShallowEdge.build({
          source: childNodeId,
          target: nonExistentChildNodeId
        })]
      })
      parentNode.visibleChildren = [childNode]

      const state = State.fromRootNodes([parentNode, childNode]);

      // when
      const edges = state.createEdges([parentNode, childNode])

      // then
      expect(edges.length).toEqual(0)
    })

    it('Does not create edge between a leaf and a nonexistent node', () => {
      // given
      const nonExistentNodeId = 'nonexistent node'
      const leafNodeId = 'leafNode'

      const leafNode = VisibleGraphNode.build({
        id: leafNodeId,
        dependencies: [ShallowEdge.build({
          source: leafNodeId,
          target: nonExistentNodeId
        })]
      })
      const state = State.fromRootNodes([leafNode])

      // when
      const edges = state.createEdges([leafNode])

      // then
      expect(edges.length).toEqual(0)
    })

    it('When creating edges, duplicates are aggregated', () => {
      // given
      const leafNodeId1 = "leafNode1"
      const leafNodeId2 = "leafNode2"
      const dependency1 = ShallowEdge.build({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: true,
        isPointingUpwards: false,
        weight: 2
      })
      const dependency2 = ShallowEdge.build({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: true,
        isPointingUpwards: false,
        weight: 1
      })

      const leafNode1 = VisibleGraphNode.build({
        id: leafNodeId1,
        dependencies: [dependency1, dependency2]
      })
      const leafNode2 = VisibleGraphNode.build({
        id: leafNodeId2
      })
      const state = State.fromRootNodes([leafNode1, leafNode2])

      // when
      const edges = state.createEdges([leafNode1, leafNode2])

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].weight).toEqual(3)
      expect(edges[0].isCyclic).toEqual(true)
    })

    it('Should not create edges to hidden nodes', () => {
      // given
      const leafNodeId = 'leafNode'
      const parentNodeId = 'parentNode'
      const hiddenChildNodeId = parentNodeId + '.hiddenChildNode'
      const leafNode = VisibleGraphNode.build({
        id: leafNodeId,
        dependencies: [ShallowEdge.build({
          source: leafNodeId,
          target: hiddenChildNodeId
        })]
      })
      const parentNode = VisibleGraphNode.build({
        id: parentNodeId,
        hiddenChildrenIds: [hiddenChildNodeId],
        visibleChildren: []
      })
      const hiddenChildNode = VisibleGraphNode.build({
        id: hiddenChildNodeId,
        parent: parentNode
      })

      const allNodes = [leafNode, parentNode, hiddenChildNode];
      const state = State.fromRootNodes(allNodes).copy({ hiddenNodeIds: [hiddenChildNodeId] });

      // when
      const edges = state.createEdges([leafNode, hiddenChildNode])

      // then
      expect(edges.length).toEqual(0)
    })

    it('When creating edges, duplicates with different isCyclic are not aggregated', () => {
      // given
      const leafNodeId1 = "leafNode1"
      const leafNodeId2 = "leafNode2"
      const dependency1 = ShallowEdge.build({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: false,
        isPointingUpwards: false,
        weight: 2
      })
      const dependency2 = ShallowEdge.build({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: true,
        isPointingUpwards: false,
        weight: 1
      })

      const leafNode1 = VisibleGraphNode.build({
        id: leafNodeId1,
        dependencies: [dependency1, dependency2]
      })
      const leafNode2 = VisibleGraphNode.build({
        id: leafNodeId2
      })
      const state = State.fromRootNodes([leafNode1, leafNode2])

      // when
      const edges = state.createEdges([leafNode1, leafNode2])

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].weight).toEqual(3)
      expect(edges[0].isCyclic).toEqual(true)
    })

    it('merges leaf level and container level feedback edges with same source/target into one feedback edge with summed weight when showAllFeedbackEdges is active', () => {
           // given
           const leafNodeId1 = "leafNode1";
           const leafNodeId2 = "leafNode2";
           const feedbackLeafLevelEdge = ShallowEdge.build({
             source: leafNodeId1,
             target: leafNodeId2,
             isCyclic: true,
             isPointingUpwards: true,
             weight: 2
           })
           const feedbackContainerLevelEdge = ShallowEdge.build({
             source: leafNodeId1,
             target: leafNodeId2,
             isCyclic: false,
             isPointingUpwards: true,
             weight: 3
           })

           const leafNode1 = VisibleGraphNode.build({
             id: leafNodeId1,
             dependencies: [feedbackLeafLevelEdge, feedbackContainerLevelEdge]
           })
           const leafNode2 = VisibleGraphNode.build({
             id: leafNodeId2
           })

           const state = State.fromRootNodes([leafNode1, leafNode2]).copy({ selectedFilter: EdgeFilterType.ALL_FEEDBACK_EDGES });

           // when
           const edges = state.createEdges([leafNode1, leafNode2]);

           // then
           expect(edges.length).toEqual(1);
           expect(edges[0].weight).toEqual(5);
           expect(edges[0].isCyclic).toEqual(true);
         });

  })
});

declare module './Edge' {
  namespace Edge {
    function build(overrides?: Partial<Edge>): Edge
  }
  namespace ShallowEdge {
    function build(overrides?: Partial<ShallowEdge>): ShallowEdge
  }
}

Edge.build = function(overrides: Partial<Edge> = {}): Edge {
  const defaultTarget = VisibleGraphNode.build()
  const defaultSource = VisibleGraphNode.build()

  const defaults = new Edge(
    defaultSource, // source
    defaultTarget, // target
    defaultSource + "-" + defaultTarget, // id
    1, // weight
    false, // isCyclic
    false, // isPointingUpwards
    'usage' // type
  )

  return defaults.copy(overrides)
}

export { Edge }

ShallowEdge.build = function(overrides: Partial<ShallowEdge> = {}): ShallowEdge {
  const defaults = new ShallowEdge(
    "source", // source
    "target", // target
    "source-target", // id
    1, // weight
    false, // isCyclic
    false, // isPointingUpwards
    'usage' // type
  )

  return defaults.copy(overrides)
}

export { ShallowEdge }
