import {buildShallowGraphEdge, buildVisibleGraphNode} from './ModelBuilders.spec';
import {createEdges, GraphEdge} from './GraphEdge';
import {EdgeFilterType} from './EdgeFilter';
import {State} from './State';

describe('GraphEdge', () => {
  describe('createEdges', () => {
    it('should create edge between two leaves', () => {
      // given
      const leafNode1Id = 'leafNode1';
      const leafNode2Id = 'leafNode2';
      const leafNode1 = buildVisibleGraphNode({
        id: leafNode1Id
      })
      const leafNode2 = buildVisibleGraphNode({
        id: leafNode2Id,
        dependencies: [buildShallowGraphEdge({
          source: leafNode2Id,
          target: leafNode1Id
        })]
      })
      const state = State.buildFromRootNodes([leafNode1, leafNode2]);

      // when
      const edges = createEdges([leafNode1, leafNode2], state);

      // then
      const expectedEdge: GraphEdge = {
        id: leafNode2Id + '-' + leafNode1Id,
        source: leafNode2,
        target: leafNode1,
        isCyclic: false,
        weight: 1,
        type: 'usage'
      }
      expect(edges.length).toEqual(1);
      expect(edges[0]).toEqual(expectedEdge);
    })

    it('creates multiple edges for a leaf node', (() => {
      // given
      const leafNode1Id = 'leafNode1';
      const leafNode2Id = 'leafNode2';
      const leafNode3Id = "leafNode3"
      const leafNode1 = buildVisibleGraphNode({
        id: leafNode1Id
      })
      const leafNode2 = buildVisibleGraphNode({
        id: leafNode2Id
      })
      const leafNode3 = buildVisibleGraphNode({
        id: leafNode3Id,
        dependencies: [leafNode1Id, leafNode2Id].map(target => buildShallowGraphEdge({
          source: leafNode3Id,
          target: target}))
      })
      const state = State.buildFromRootNodes([leafNode1, leafNode2, leafNode3]);

      // when
      const edges = createEdges([leafNode1, leafNode2, leafNode3], state)

      // then
      expect(edges.length).toEqual(2)
    }))

    it('creates an edge between a leaf and a node if the original leaf target is collapsed', () => {
      // given
      const parentNodeId = 'parentNode';
      const collapsedLeafNodeId = parentNodeId + '.collapsedLeaf';
      const expandedLeafNodeId = "expandedLeaf"
      const parentNode = buildVisibleGraphNode({
        id: parentNodeId,
        visibleChildren: []
      })
      const collapsedLeaf = buildVisibleGraphNode({
        id: collapsedLeafNodeId,
        parent: parentNode
      })
      const expandedLeaf = buildVisibleGraphNode({
        id: expandedLeafNodeId,
        dependencies: [buildShallowGraphEdge({
          source: expandedLeafNodeId,
          target: collapsedLeafNodeId
        })]
      })

      const base = State.buildFromRootNodes([parentNode, collapsedLeaf, expandedLeaf])
      const state = new State({ allNodes: base.allNodes, expandedNodeIds: [] })

      // when
      const edges = createEdges([parentNode, collapsedLeaf, expandedLeaf], state)

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].source.id).toEqual(expandedLeaf.id)
      expect(edges[0].target.id).toEqual(parentNode.id)
    })

    it('does not create edges for expanded packages', () => {
      // given
      const parentNodeId = 'parentNode'
      const childNodeId = parentNodeId + '.childNode'
      const otherNode1Id = "otherNode1"
      const otherNode2Id = "otherNode2"

      const parentNode = buildVisibleGraphNode({
        id: parentNodeId,
        dependencies: [buildShallowGraphEdge({
          source: parentNodeId,
          target: otherNode2Id
        })]
      })
      const childNode = buildVisibleGraphNode({
        id: childNodeId,
        parent: parentNode,
        dependencies: [buildShallowGraphEdge({
          source: childNodeId,
          target: otherNode1Id
        })]
      })
      parentNode.visibleChildren = [childNode]
      const otherNode1 = buildVisibleGraphNode({
        id: otherNode1Id
      })
      const otherNode2 = buildVisibleGraphNode({
        id: otherNode2Id
      })

      const allNodes = [parentNode, childNode, otherNode1, otherNode2];
      const base = State.buildFromRootNodes(allNodes);
      const state = new State({ allNodes: base.allNodes, expandedNodeIds: [parentNodeId] })

      // when
      const edges = createEdges(allNodes, state)

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

      const parentNode = buildVisibleGraphNode({
        id: parentNodeId
      })
      const childNode = buildVisibleGraphNode({
        id: childNodeId,
        parent: parentNode,
        dependencies: [buildShallowGraphEdge({
          source: childNodeId,
          target: nonExistentChildNodeId
        })]
      })
      parentNode.visibleChildren = [childNode]

      const state = State.buildFromRootNodes([parentNode, childNode]);

      // when
      const edges = createEdges([parentNode, childNode], state)

      // then
      expect(edges.length).toEqual(0)
    })

    it('Does not create edge between a leaf and a nonexistent node', () => {
      // given
      const nonExistentNodeId = 'nonexistent node'
      const leafNodeId = 'leafNode'

      const leafNode = buildVisibleGraphNode({
        id: leafNodeId,
        dependencies: [buildShallowGraphEdge({
          source: leafNodeId,
          target: nonExistentNodeId
        })]
      })
      const state = State.buildFromRootNodes([leafNode])

      // when
      const edges = createEdges([leafNode], state)

      // then
      expect(edges.length).toEqual(0)
    })

    it('When creating edges, duplicates are aggregated', () => {
      // given
      const leafNodeId1 = "leafNode1"
      const leafNodeId2 = "leafNode2"
      const dependency1 = buildShallowGraphEdge({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: true,
        weight: 2
      })
      const dependency2 = buildShallowGraphEdge({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: true,
        weight: 1
      })

      const leafNode1 = buildVisibleGraphNode({
        id: leafNodeId1,
        dependencies: [dependency1, dependency2]
      })
      const leafNode2 = buildVisibleGraphNode({
        id: leafNodeId2
      })
      const state = State.buildFromRootNodes([leafNode1, leafNode2])

      // when
      const edges = createEdges([leafNode1, leafNode2], state)

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
      const leafNode = buildVisibleGraphNode({
        id: leafNodeId,
        dependencies: [buildShallowGraphEdge({
          source: leafNodeId,
          target: hiddenChildNodeId
        })]
      })
      const parentNode = buildVisibleGraphNode({
        id: parentNodeId,
        hiddenChildrenIds: [hiddenChildNodeId],
        visibleChildren: []
      })
      const hiddenChildNode = buildVisibleGraphNode({
        id: hiddenChildNodeId,
        parent: parentNode
      })

      const allNodes = [leafNode, parentNode, hiddenChildNode];
      const base = State.buildFromRootNodes(allNodes);
      const state = new State({ allNodes: base.allNodes, hiddenNodeIds: [hiddenChildNodeId] })

      // when
      const edges = createEdges([leafNode, hiddenChildNode], state)

      // then
      expect(edges.length).toEqual(0)
    })

    it('When creating edges, duplicates with different isCyclic are not aggregated', () => {
      // given
      const leafNodeId1 = "leafNode1"
      const leafNodeId2 = "leafNode2"
      const dependency1 = buildShallowGraphEdge({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: false,
        weight: 2
      })
      const dependency2 = buildShallowGraphEdge({
        source: leafNodeId1,
        target: leafNodeId2,
        isCyclic: true,
        weight: 1
      })

      const leafNode1 = buildVisibleGraphNode({
        id: leafNodeId1,
        dependencies: [dependency1, dependency2]
      })
      const leafNode2 = buildVisibleGraphNode({
        id: leafNodeId2
      })
      const state = State.buildFromRootNodes([leafNode1, leafNode2])

      // when
      const edges = createEdges([leafNode1, leafNode2], state)

      // then
      expect(edges.length).toEqual(1)
      expect(edges[0].weight).toEqual(3)
      expect(edges[0].isCyclic).toEqual(true)
    })

    it('merges feedback and twisted edges with same source/target into one feedback edge with summed weight when showFeedbackEdgesAndTwistedEdges is active', () => {
           // given
           const leafNodeId1 = "leafNode1";
           const leafNodeId2 = "leafNode2";
           const feedbackEdge = buildShallowGraphEdge({
             source: leafNodeId1,
             target: leafNodeId2,
             isCyclic: true,
             weight: 2
           })
           const twistedEdge = buildShallowGraphEdge({
             source: leafNodeId1,
             target: leafNodeId2,
             isCyclic: false,
             weight: 3
           })

           const leafNode1 = buildVisibleGraphNode({
             id: leafNodeId1,
             dependencies: [feedbackEdge, twistedEdge]
           })
           const leafNode2 = buildVisibleGraphNode({
             id: leafNodeId2
           })

           const base = State.buildFromRootNodes([leafNode1, leafNode2]);
           const state = new State({ allNodes: base.allNodes, selectedFilter: EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES });

           // when
           const edges = createEdges([leafNode1, leafNode2], state);

           // then
           expect(edges.length).toEqual(1);
           expect(edges[0].weight).toEqual(5);
           expect(edges[0].isCyclic).toEqual(true);
         });

  })
});
