import {buildVisibleGraphNode} from './ModelBuilders.spec';
import {EdgePredicate} from './EdgePredicate';
import {EdgeType} from './EdgeType';
import {GraphEdge} from './GraphEdge.spec';

describe('Layout', () => {
  it('Is no feedback edge, when edge points from lower level node to higher level node and is not cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1
      }),
      target: buildVisibleGraphNode({
        level: 2
      }),
      isCyclic: false
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.FEEDBACK)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is no feedback edge, when edge points from higher level node to lower level node and is cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 2
      }),
      target: buildVisibleGraphNode({
        level: 1
      }),
      isCyclic: true
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.FEEDBACK)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is feedback edge, when edge points from lower level node to higher level node and is cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1
      }),
      target: buildVisibleGraphNode({
        level: 2
      }),
      isCyclic: true
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.FEEDBACK)
    expect(predicate(edge)).toEqual(true);
  });

  it('Is no twisted edge, when edge points from higher level node to lower level node and is cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 2
      }),
      target: buildVisibleGraphNode({
        level: 1
      }),
      isCyclic: true
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is no twisted edge, when edge points from lower level node to higher level node and is cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1
      }),
      target: buildVisibleGraphNode({
        level: 2
      }),
      isCyclic: true
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is no twisted edge, when edge points from higher level node to lower level node and is not cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 2
      }),
      target: buildVisibleGraphNode({
        level: 1
      }),
      isCyclic: false
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Is twisted edge, when edge points from lower level node to higher level node and is not cyclic', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1
      }),
      target: buildVisibleGraphNode({
        level: 2
      }),
      isCyclic: false
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });

  it('Is pointing upwards, when edge points to other node on the same level', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1
      }),
      target: buildVisibleGraphNode({
        level: 1
      }),
      isCyclic: false
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });

  it('Only compares nodes of the same level and not the source of the edge itself', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1,
        parent: buildVisibleGraphNode({
          level: 2
        })
      }),
      target: buildVisibleGraphNode({
        level: 1
      })
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(false);
  });

  it('Only compares nodes of the same level and not the target of the edge itself', () => {
    const edge = GraphEdge.build({
      source: buildVisibleGraphNode({
        level: 1
      }),
      target: buildVisibleGraphNode({
        level: 1,
        parent: buildVisibleGraphNode({
          level: 2
        })
      })
    })

    const predicate = EdgePredicate.fromEnum(EdgeType.TWISTED)
    expect(predicate(edge)).toEqual(true);
  });
});
