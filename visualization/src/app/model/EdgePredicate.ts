import {GraphEdge, isPointingUpwards} from './GraphEdge';
import {EdgeType} from './EdgeType';

type EdgePredicate = (edge: GraphEdge) => boolean

export namespace EdgePredicate {
  export function fromEnum(edgeType: EdgeType): EdgePredicate {
    switch (edgeType) {
      case EdgeType.REGULAR:
        return edge => !edge.isCyclic && !isPointingUpwards(edge)
      case EdgeType.CYCLIC:
        return edge => edge.isCyclic && !isPointingUpwards(edge)
      case EdgeType.FEEDBACK:
        return edge => edge.isCyclic && isPointingUpwards(edge)
      case EdgeType.TWISTED:
        return edge => !edge.isCyclic && isPointingUpwards(edge)
      case EdgeType.NONE:
        return (_) => false
    }
  }
}

