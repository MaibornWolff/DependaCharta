import {Edge} from './Edge';
import {EdgeType} from './EdgeType';

type EdgePredicate = (edge: Edge) => boolean

export namespace EdgePredicate {
  export function fromEnum(edgeType: EdgeType): EdgePredicate {
    switch (edgeType) {
      case EdgeType.REGULAR:
        return edge => !edge.isCyclic && !edge.isPointingUpwards
      case EdgeType.CYCLIC:
        return edge => edge.isCyclic && !edge.isPointingUpwards
      case EdgeType.FEEDBACK_LEAF_LEVEL:
        return edge => edge.isCyclic && edge.isPointingUpwards
      case EdgeType.FEEDBACK_CONTAINER_LEVEL:
        return edge => !edge.isCyclic && edge.isPointingUpwards
      case EdgeType.NONE:
        return (_) => false
    }
  }
}

