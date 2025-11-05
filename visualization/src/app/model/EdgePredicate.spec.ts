import {EdgePredicate} from './EdgePredicate';
import {EdgeType} from './EdgeType';
import {Edge} from './Edge.spec';
import {VisibleGraphNode} from './GraphNode.spec';

describe('Layout', () => {
  it('Is no feedback edge, when edge points from lower level node to higher level node and is not cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1
      }),
      target: VisibleGraphNode.build({
        level: 2
      }),
      isCyclic: false,
      isPointingUpwards: true // source level (1) <= target level (2)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.FEEDBACK)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is no feedback edge, when edge points from higher level node to lower level node and is cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 2
      }),
      target: VisibleGraphNode.build({
        level: 1
      }),
      isCyclic: true,
      isPointingUpwards: false // source level (2) > target level (1)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.FEEDBACK)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is feedback edge, when edge points from lower level node to higher level node and is cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1
      }),
      target: VisibleGraphNode.build({
        level: 2
      }),
      isCyclic: true,
      isPointingUpwards: true // source level (1) <= target level (2)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.FEEDBACK)
    expect(predicate(edge)).toEqual(true);
  });

  it('Is no twisted edge, when edge points from higher level node to lower level node and is cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 2
      }),
      target: VisibleGraphNode.build({
        level: 1
      }),
      isCyclic: true,
      isPointingUpwards: false // source level (2) > target level (1)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is no twisted edge, when edge points from lower level node to higher level node and is cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1
      }),
      target: VisibleGraphNode.build({
        level: 2
      }),
      isCyclic: true,
      isPointingUpwards: true // source level (1) <= target level (2)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is no twisted edge, when edge points from higher level node to lower level node and is not cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 2
      }),
      target: VisibleGraphNode.build({
        level: 1
      }),
      isCyclic: false,
      isPointingUpwards: false // source level (2) > target level (1)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is twisted edge, when edge points from lower level node to higher level node and is not cyclic', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1
      }),
      target: VisibleGraphNode.build({
        level: 2
      }),
      isCyclic: false,
      isPointingUpwards: true // source level (1) <= target level (2)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });

  it('Is pointing upwards, when edge points to other node on the same level', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1
      }),
      target: VisibleGraphNode.build({
        level: 1
      }),
      isCyclic: false,
      isPointingUpwards: true // source level (1) <= target level (1)
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });

  it('Only compares nodes of the same level and not the source of the edge itself', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1,
        parent: VisibleGraphNode.build({
          level: 2
        })
      }),
      target: VisibleGraphNode.build({
        level: 1
      }),
      isPointingUpwards: false // Based on sibling comparison, not direct levels
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Only compares nodes of the same level and not the target of the edge itself', () => {
    const edge = Edge.build({
      source: VisibleGraphNode.build({
        level: 1
      }),
      target: VisibleGraphNode.build({
        level: 1,
        parent: VisibleGraphNode.build({
          level: 2
        })
      }),
      isPointingUpwards: true // Based on sibling comparison
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });
});
