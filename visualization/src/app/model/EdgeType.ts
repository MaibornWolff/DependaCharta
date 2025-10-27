import {EdgeFilterType} from './EdgeFilter';

export enum EdgeType {
  REGULAR = "REGULAR",
  CYCLIC = "CYCLIC",
  FEEDBACK = "FEEDBACK",
  TWISTED = "TWISTED",
  NONE = "NONE"
}

export namespace EdgeType {
  export function fromEnum(edgeFilterType: EdgeFilterType): EdgeType[] {
    switch (edgeFilterType) {
      case EdgeFilterType.ALL:
        return [
          EdgeType.REGULAR,
          EdgeType.CYCLIC,
          EdgeType.FEEDBACK,
          EdgeType.TWISTED]
      case EdgeFilterType.NONE:
        return []
      case EdgeFilterType.CYCLES_ONLY:
        return [
          EdgeType.CYCLIC,
          EdgeType.FEEDBACK]
      case EdgeFilterType.FEEDBACK_EDGES_ONLY:
        return [
          EdgeType.FEEDBACK]
      case EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES:
        return [
          EdgeType.FEEDBACK,
          EdgeType.TWISTED]
    }
  }
}
