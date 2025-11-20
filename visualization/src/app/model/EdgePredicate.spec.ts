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
      isPointingUpwards: true
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
      isPointingUpwards: false
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
      isPointingUpwards: true
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
      isPointingUpwards: false
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
      isPointingUpwards: true
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
      isPointingUpwards: false
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
      isPointingUpwards: true
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
      isPointingUpwards: true
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
      isCyclic: false,
      isPointingUpwards: false
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
      isCyclic: false,
      isPointingUpwards: true
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });
});
