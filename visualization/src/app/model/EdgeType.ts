import {EdgeFilterType} from './EdgeFilter';

export enum EdgeType {
  REGULAR = "REGULAR",
  CYCLIC = "CYCLIC",
  FEEDBACK_LEAF_LEVEL = "FEEDBACK_LEAF_LEVEL",
  FEEDBACK_CONTAINER_LEVEL = "FEEDBACK_CONTAINER_LEVEL",
  NONE = "NONE"
}

export namespace EdgeType {
  export function fromEnum(edgeFilterType: EdgeFilterType): EdgeType[] {
    switch (edgeFilterType) {
      case EdgeFilterType.ALL:
        return [
          EdgeType.REGULAR,
          EdgeType.CYCLIC,
          EdgeType.FEEDBACK_LEAF_LEVEL,
          EdgeType.FEEDBACK_CONTAINER_LEVEL]
      case EdgeFilterType.NONE:
        return []
      case EdgeFilterType.CYCLES_ONLY:
        return [
          EdgeType.CYCLIC,
          EdgeType.FEEDBACK_LEAF_LEVEL]
      case EdgeFilterType.FEEDBACK_LEAF_LEVEL_ONLY:
        return [
          EdgeType.FEEDBACK_LEAF_LEVEL]
      case EdgeFilterType.ALL_FEEDBACK_EDGES:
        return [
          EdgeType.FEEDBACK_LEAF_LEVEL,
          EdgeType.FEEDBACK_CONTAINER_LEVEL]
    }
  }
}
