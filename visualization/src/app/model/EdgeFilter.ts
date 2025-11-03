import {Edge} from './Edge';
import {EdgePredicate} from './EdgePredicate';
import {EdgeType} from './EdgeType';

export enum EdgeFilterType {
  ALL = "ALL",
  NONE = "NONE",
  CYCLES_ONLY = "CYCLES_ONLY",
  FEEDBACK_EDGES_ONLY = "FEEDBACK_EDGES_ONLY",
  FEEDBACK_EDGES_AND_TWISTED_EDGES = "FEEDBACK_EDGES_AND_TWISTED_EDGES",
}

export type EdgeFilter = (edges: Edge[]) => EdgeFilterResult

export namespace EdgeFilter {
  export function fromEnum(edgeFilterType: EdgeFilterType): EdgeFilter {
    return fromEnums(EdgeType.fromEnum(edgeFilterType))
  }

  export function fromEnums(edgeTypes: EdgeType[], graphNodeId: string = ""): EdgeFilter {
    return (edgesIn: Edge[]) => {
      let edges = edgesIn

      if (graphNodeId !== "") {
        edges = edgesIn.filter(edge => [edge.source.id, edge.target.id].includes(graphNodeId))
      }

      const edgeFilterResult: EdgeFilterResult = EdgeFilterResult.empty()

      for (const edgeType of edgeTypes) {
        const filteredEdges = edges.filter(EdgePredicate.fromEnum(edgeType))

        filteredEdges.forEach(edge => {
          edgeFilterResult.set(edge.id, edgeType)
        })
      }

      return edgeFilterResult
    }
  }

  export function forAllEdgeTypes(id: string): EdgeFilter {
    const allEdgeTypes = EdgeType.fromEnum(EdgeFilterType.ALL)
    return EdgeFilter.fromEnums(allEdgeTypes, id)
  }

  export function isFilterForcesEdgesAggregation(edgeFilterType: EdgeFilterType): boolean {
    return edgeFilterType !== EdgeFilterType.CYCLES_ONLY && edgeFilterType !== EdgeFilterType.FEEDBACK_EDGES_ONLY
  }
}

export type EdgeId = string

export type EdgeFilterResult = Map<EdgeId, EdgeType>

export namespace EdgeFilterResult {
  export function empty(): EdgeFilterResult {
    return new Map()
  }
}
