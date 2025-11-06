import {VisibleGraphNode} from './GraphNode';

export class Edge {
  constructor(
    readonly source: VisibleGraphNode,
    readonly target: VisibleGraphNode,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string,
    readonly isPointingUpwards: boolean
  ) {}

  copy(overrides: Partial<Edge>) {
    return Object.assign(this, overrides)
  }

  // TODO `Edge` should have a function `getType(): EdgeType`
  // !isCyclic && !isPointingUpwards => REGULAR
  // isCyclic && !isPointingUpwards => CYCLIC
  // !isCyclic && isPointingUpwards => TWISTED
  // isCyclic && isPointingUpwards => FEEDBACK
  // TODO (next) `EdgePredicate`, `EdgeFilter`, `EdgeFilterResult` can be removed

  static aggregateEdges(edges: Edge[], shouldAggregateEdges: boolean): Edge[] {
    const aggregatedEdges = new Map<string, Edge>()

    edges.forEach(edge => {
      const key = shouldAggregateEdges
        ? edge.id
        : `${(edge.id)}-${(edge.isCyclic)}`
      const duplicateEdge = aggregatedEdges.get(key)

      let aggregatedEdge: Edge
      if (duplicateEdge) {
        aggregatedEdge = duplicateEdge.copy({
          weight: duplicateEdge.weight + edge.weight,
          isCyclic: shouldAggregateEdges
            ? duplicateEdge.isCyclic || edge.isCyclic
            : edge.isCyclic,
        })
      } else {
        aggregatedEdge = edge.copy({id: key})
      }

      aggregatedEdges.set(key, aggregatedEdge)
    });

    return [...aggregatedEdges.values()];    
  }
}

