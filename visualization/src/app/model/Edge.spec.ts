import {Edge, FeedbackEdgeGroup, groupFeedbackEdges, ShallowEdge} from './Edge';
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

describe('groupFeedbackEdges', () => {
  it('should group edges with same container paths together', () => {
    // Given - edges with same source and target container paths (diverge at same point)
    const edge1 = ShallowEdge.build({
      source: 'project.domain.model.ArmorClass:leaf',
      target: 'project.application.CreatureUtil:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });
    const edge2 = ShallowEdge.build({
      source: 'project.domain.model.Creature:leaf',
      target: 'project.application.CreatureFacade:leaf',
      weight: 1,
      isCyclic: false,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge1, edge2]);

    // Then - both edges share project.domain→project.application container paths
    expect(result.length).toBe(1);
    expect(result[0].isGroup).toBe(true);
    expect(result[0].source).toBe('project.domain');
    expect(result[0].target).toBe('project.application');
    expect(result[0].weight).toBe(2);
    expect(result[0].children.length).toBe(2);
  });

  it('should separate edges from different top-level packages', () => {
    // Given - edges from different projects should be in separate groups
    const edge1 = ShallowEdge.build({
      source: 'projectA.domain.ClassA:leaf',
      target: 'projectA.application.ClassB:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });
    const edge2 = ShallowEdge.build({
      source: 'projectB.domain.ClassC:leaf',
      target: 'projectB.application.ClassD:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge1, edge2]);

    // Then - edges are in separate groups (different container paths)
    expect(result.length).toBe(2);
    expect(result[0].isGroup).toBe(false);
    expect(result[1].isGroup).toBe(false);
  });

  it('should group edges that diverge at the same level', () => {
    // Given - all edges diverge at domain vs application level
    const edge1 = ShallowEdge.build({
      source: 'project.domain.model.ClassA:leaf',
      target: 'project.application.util.HelperB:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });
    const edge2 = ShallowEdge.build({
      source: 'project.domain.service.ClassC:leaf',
      target: 'project.application.ClassD:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge1, edge2]);

    // Then - grouped by divergent containers
    expect(result.length).toBe(1);
    expect(result[0].isGroup).toBe(true);
    expect(result[0].source).toBe('project.domain');
    expect(result[0].target).toBe('project.application');
    expect(result[0].weight).toBe(2);
  });

  it('should return single edge flat without grouping', () => {
    // Given - single edge
    const edge = ShallowEdge.build({
      source: 'project.domain.ClassA:leaf',
      target: 'project.application.ClassB:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge]);

    // Then
    expect(result.length).toBe(1);
    expect(result[0].isGroup).toBe(false);
    expect(result[0]).toBe(edge);
  });

  it('should not group edges with different container paths', () => {
    // Given - edges with completely different divergence patterns
    const edge1 = ShallowEdge.build({
      source: 'project.foo.ClassA:leaf',
      target: 'project.bar.ClassB:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });
    const edge2 = ShallowEdge.build({
      source: 'project.baz.ClassC:leaf',
      target: 'project.qux.ClassD:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge1, edge2]);

    // Then - each edge has different container paths, not grouped
    expect(result.length).toBe(2);
    expect(result[0].isGroup).toBe(false);
    expect(result[1].isGroup).toBe(false);
  });

  it('should handle empty array', () => {
    // When
    const result = groupFeedbackEdges([]);

    // Then
    expect(result.length).toBe(0);
  });

  it('should group all edges with same divergence point', () => {
    // Given - three edges all diverging at domain vs application
    const edge1 = ShallowEdge.build({
      source: 'project.domain.model.sub.ClassA:leaf',
      target: 'project.application.ClassX:leaf',
      weight: 1,
      isCyclic: true,
      isPointingUpwards: true
    });
    const edge2 = ShallowEdge.build({
      source: 'project.domain.model.sub.ClassB:leaf',
      target: 'project.application.ClassY:leaf',
      weight: 2,
      isCyclic: true,
      isPointingUpwards: true
    });
    const edge3 = ShallowEdge.build({
      source: 'project.domain.service.ClassC:leaf',
      target: 'project.application.ClassZ:leaf',
      weight: 3,
      isCyclic: true,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge1, edge2, edge3]);

    // Then - all grouped: project.domain → project.application
    expect(result.length).toBe(1);
    expect(result[0].isGroup).toBe(true);
    expect(result[0].source).toBe('project.domain');
    expect(result[0].target).toBe('project.application');
    expect(result[0].weight).toBe(6);
  });
});

describe('groupFeedbackEdges - real-world scenarios', () => {
  it('should group edges from app_component to adapter when they share container paths', () => {
    // Given - edges from app_component to adapter share the same divergence point
    const edge1 = ShallowEdge.build({
      source: 'visualization.src.app.app_component.AppComponent:leaf',
      target: 'visualization.src.app.adapter.cytoscape.index.CytoscapeComponent:leaf',
      weight: 1,
      isCyclic: false,
      isPointingUpwards: true
    });
    const edge2 = ShallowEdge.build({
      source: 'visualization.src.app.app_component.AppComponent:leaf',
      target: 'visualization.src.app.adapter.analysis.internal.ProjectReport:leaf',
      weight: 1,
      isCyclic: false,
      isPointingUpwards: true
    });
    const edge3 = ShallowEdge.build({
      source: 'visualization.src.app.app_component.AppComponent:leaf',
      target: 'visualization.src.app.adapter.analysis.Analysis:leaf',
      weight: 1,
      isCyclic: false,
      isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([edge1, edge2, edge3]);

    // Then - all edges share visualization.src.app.app_component→visualization.src.app.adapter
    expect(result.length).toBe(1);
    expect(result[0].isGroup).toBe(true);
    expect(result[0].source).toBe('visualization.src.app.app_component');
    expect(result[0].target).toBe('visualization.src.app.adapter');
    expect(result[0].children.length).toBe(3);
    expect(result[0].weight).toBe(3);
  });

  it('should create separate groups for edges with different container paths', () => {
    // Given - two groups of edges with different divergence points
    // Group 1: app_component → adapter
    const appToAdapter1 = ShallowEdge.build({
      source: 'visualization.src.app.app_component.AppComponent:leaf',
      target: 'visualization.src.app.adapter.cytoscape.CytoscapeComponent:leaf',
      weight: 1, isCyclic: false, isPointingUpwards: true
    });
    const appToAdapter2 = ShallowEdge.build({
      source: 'visualization.src.app.app_component.AppComponent:leaf',
      target: 'visualization.src.app.adapter.analysis.ProjectReport:leaf',
      weight: 1, isCyclic: false, isPointingUpwards: true
    });
    const appToAdapter3 = ShallowEdge.build({
      source: 'visualization.src.app.app_component.AppComponent:leaf',
      target: 'visualization.src.app.adapter.analysis.Analysis:leaf',
      weight: 1, isCyclic: false, isPointingUpwards: true
    });
    // Group 2: model → ui
    const modelToUi1 = ShallowEdge.build({
      source: 'visualization.src.app.model.State:leaf',
      target: 'visualization.src.app.ui.filter.FilterComponent:leaf',
      weight: 1, isCyclic: false, isPointingUpwards: true
    });
    const modelToUi2 = ShallowEdge.build({
      source: 'visualization.src.app.model.Edge:leaf',
      target: 'visualization.src.app.ui.list.ListComponent:leaf',
      weight: 1, isCyclic: false, isPointingUpwards: true
    });

    // When
    const result = groupFeedbackEdges([appToAdapter1, appToAdapter2, appToAdapter3, modelToUi1, modelToUi2]);

    // Then - should create 2 groups with different container paths
    expect(result.length).toBe(2);

    // Find the app_component → adapter group
    const appToAdapterGroup = result.find(entry =>
      entry.isGroup && entry.source.includes('app_component') && entry.target.includes('adapter')
    );
    expect(appToAdapterGroup).toBeDefined();
    expect(appToAdapterGroup!.source).toBe('visualization.src.app.app_component');
    expect(appToAdapterGroup!.target).toBe('visualization.src.app.adapter');
    expect(appToAdapterGroup!.children.length).toBe(3);

    // Find the model → ui group
    const modelToUiGroup = result.find(entry =>
      entry.isGroup && entry.source.includes('model') && entry.target.includes('ui')
    );
    expect(modelToUiGroup).toBeDefined();
    expect(modelToUiGroup!.source).toBe('visualization.src.app.model');
    expect(modelToUiGroup!.target).toBe('visualization.src.app.ui');
    expect(modelToUiGroup!.children.length).toBe(2);
  });
});

describe('FeedbackListEntry', () => {
  describe('ShallowEdge as FeedbackListEntry', () => {
    it('should have hasLeafLevel true when isCyclic and isPointingUpwards', () => {
      // Given
      const edge = ShallowEdge.build({
        isCyclic: true,
        isPointingUpwards: true
      });

      // Then
      expect(edge.hasLeafLevel).toBe(true);
    });

    it('should have hasLeafLevel false when not cyclic', () => {
      // Given
      const edge = ShallowEdge.build({
        isCyclic: false,
        isPointingUpwards: true
      });

      // Then
      expect(edge.hasLeafLevel).toBe(false);
    });

    it('should have hasContainerLevel true when not cyclic but isPointingUpwards', () => {
      // Given
      const edge = ShallowEdge.build({
        isCyclic: false,
        isPointingUpwards: true
      });

      // Then
      expect(edge.hasContainerLevel).toBe(true);
    });

    it('should have hasContainerLevel false when cyclic', () => {
      // Given
      const edge = ShallowEdge.build({
        isCyclic: true,
        isPointingUpwards: true
      });

      // Then
      expect(edge.hasContainerLevel).toBe(false);
    });

    it('should have isGroup false', () => {
      // Given
      const edge = ShallowEdge.build({});

      // Then
      expect(edge.isGroup).toBe(false);
    });

    it('should have empty children array', () => {
      // Given
      const edge = ShallowEdge.build({});

      // Then
      expect(edge.children).toEqual([]);
    });
  });

  describe('FeedbackEdgeGroup', () => {
    it('should have isGroup true', () => {
      // Given
      const group = new FeedbackEdgeGroup('source.package', 'target.package', []);

      // Then
      expect(group.isGroup).toBe(true);
    });

    it('should compute weight as sum of children weights', () => {
      // Given
      const child1 = ShallowEdge.build({ weight: 2 });
      const child2 = ShallowEdge.build({ weight: 3 });
      const group = new FeedbackEdgeGroup('source.package', 'target.package', [child1, child2]);

      // Then
      expect(group.weight).toBe(5);
    });

    it('should have hasLeafLevel true if any child has leaf level', () => {
      // Given
      const leafLevelChild = ShallowEdge.build({ isCyclic: true, isPointingUpwards: true });
      const containerLevelChild = ShallowEdge.build({ isCyclic: false, isPointingUpwards: true });
      const group = new FeedbackEdgeGroup('source', 'target', [leafLevelChild, containerLevelChild]);

      // Then
      expect(group.hasLeafLevel).toBe(true);
    });

    it('should have hasLeafLevel false if no child has leaf level', () => {
      // Given
      const containerLevelChild1 = ShallowEdge.build({ isCyclic: false, isPointingUpwards: true });
      const containerLevelChild2 = ShallowEdge.build({ isCyclic: false, isPointingUpwards: true });
      const group = new FeedbackEdgeGroup('source', 'target', [containerLevelChild1, containerLevelChild2]);

      // Then
      expect(group.hasLeafLevel).toBe(false);
    });

    it('should have hasContainerLevel true if any child has container level', () => {
      // Given
      const leafLevelChild = ShallowEdge.build({ isCyclic: true, isPointingUpwards: true });
      const containerLevelChild = ShallowEdge.build({ isCyclic: false, isPointingUpwards: true });
      const group = new FeedbackEdgeGroup('source', 'target', [leafLevelChild, containerLevelChild]);

      // Then
      expect(group.hasContainerLevel).toBe(true);
    });

    it('should have hasContainerLevel false if no child has container level', () => {
      // Given
      const leafLevelChild1 = ShallowEdge.build({ isCyclic: true, isPointingUpwards: true });
      const leafLevelChild2 = ShallowEdge.build({ isCyclic: true, isPointingUpwards: true });
      const group = new FeedbackEdgeGroup('source', 'target', [leafLevelChild1, leafLevelChild2]);

      // Then
      expect(group.hasContainerLevel).toBe(false);
    });

    it('should store children', () => {
      // Given
      const child1 = ShallowEdge.build({ source: 'a', target: 'b' });
      const child2 = ShallowEdge.build({ source: 'c', target: 'd' });
      const group = new FeedbackEdgeGroup('source', 'target', [child1, child2]);

      // Then
      expect(group.children).toEqual([child1, child2]);
    });

    it('should support nested groups', () => {
      // Given
      const edge = ShallowEdge.build({ weight: 1, isCyclic: true, isPointingUpwards: true });
      const innerGroup = new FeedbackEdgeGroup('inner.source', 'inner.target', [edge]);
      const outerGroup = new FeedbackEdgeGroup('outer.source', 'outer.target', [innerGroup]);

      // Then
      expect(outerGroup.weight).toBe(1);
      expect(outerGroup.hasLeafLevel).toBe(true);
      expect(outerGroup.children[0]).toBe(innerGroup);
    });
  });
});
