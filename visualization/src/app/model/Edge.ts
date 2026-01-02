import {getAncestors, GraphNode, VisibleGraphNode} from './GraphNode';

export class Edge {
  constructor(
    readonly source: VisibleGraphNode,
    readonly target: VisibleGraphNode,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly isPointingUpwards: boolean,
    readonly type: string
  ) {}

  copy(overrides: Partial<Edge>): Edge {
    return Object.assign(this, overrides)
  }

  // TODO `Edge` should have a function `getType(): EdgeType`
  // !isCyclic && !isPointingUpwards => REGULAR
  // isCyclic && !isPointingUpwards => CYCLIC
  // !isCyclic && isPointingUpwards => FEEDBACK_CONTAINER_LEVEL
  // isCyclic && isPointingUpwards => FEEDBACK_LEAF_LEVEL
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
          isPointingUpwards: shouldAggregateEdges
            ? duplicateEdge.isPointingUpwards || edge.isPointingUpwards
            : edge.isPointingUpwards,
        })
      } else {
        aggregatedEdge = edge.copy({id: key})
      }

      aggregatedEdges.set(key, aggregatedEdge)
    });

    return [...aggregatedEdges.values()];
  }
}

export interface FeedbackListEntry {
  readonly source: string;
  readonly target: string;
  readonly weight: number;
  readonly hasLeafLevel: boolean;
  readonly hasContainerLevel: boolean;
  readonly isGroup: boolean;
  readonly children: FeedbackListEntry[];
}

export class ShallowEdge implements FeedbackListEntry {
  constructor(
    readonly source: string,
    readonly target: string,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly isPointingUpwards: boolean,
    readonly type: string
  ) {}

  get hasLeafLevel(): boolean {
    return this.isCyclic && this.isPointingUpwards;
  }

  get hasContainerLevel(): boolean {
    return !this.isCyclic && this.isPointingUpwards;
  }

  get isGroup(): boolean {
    return false;
  }

  get children(): FeedbackListEntry[] {
    return [];
  }

  copy(overrides: Partial<ShallowEdge>): ShallowEdge {
    return Object.assign(this, overrides)
  }
}

export class FeedbackEdgeGroup implements FeedbackListEntry {
  constructor(
    readonly source: string,
    readonly target: string,
    readonly children: FeedbackListEntry[]
  ) {}

  get weight(): number {
    return this.children.reduce((sum, child) => sum + child.weight, 0);
  }

  get hasLeafLevel(): boolean {
    return this.children.some(child => child.hasLeafLevel);
  }

  get hasContainerLevel(): boolean {
    return this.children.some(child => child.hasContainerLevel);
  }

  get isGroup(): boolean {
    return true;
  }
}

// Helper function for grouping algorithm

function getPathSegments(nodeId: string): string[] {
  const withoutLeafSuffix = nodeId.replace(/:leaf$/, '');
  return withoutLeafSuffix.split('.');
}

export function getHierarchy(entry: FeedbackListEntry): number {
  const sourceWithoutLeaf = entry.source.replace(/:leaf$/, '');
  const targetWithoutLeaf = entry.target.replace(/:leaf$/, '');
  const sourceDots = (sourceWithoutLeaf.match(/\./g) || []).length;
  const targetDots = (targetWithoutLeaf.match(/\./g) || []).length;
  return Math.min(sourceDots, targetDots);
}

export function groupFeedbackEdges(edges: ShallowEdge[]): FeedbackListEntry[] {
  if (edges.length === 0) return [];
  if (edges.length === 1) return edges;

  // Group edges by their full container paths
  const groups = new Map<string, { sourceContainer: string, targetContainer: string, edges: ShallowEdge[] }>();

  for (const edge of edges) {
    const sourcePath = getPathSegments(edge.source);
    const targetPath = getPathSegments(edge.target);

    // Find where source and target paths diverge
    let divergeIndex = 0;
    const minLen = Math.min(sourcePath.length, targetPath.length);
    while (divergeIndex < minLen - 1 && sourcePath[divergeIndex] === targetPath[divergeIndex]) {
      divergeIndex++;
    }

    // Build container paths (path up to and including the divergent segment)
    const sourceContainer = sourcePath.slice(0, divergeIndex + 1).join('.');
    const targetContainer = targetPath.slice(0, divergeIndex + 1).join('.');
    const groupKey = `${sourceContainer}→${targetContainer}`;

    if (!groups.has(groupKey)) {
      groups.set(groupKey, { sourceContainer, targetContainer, edges: [] });
    }
    groups.get(groupKey)!.edges.push(edge);
  }

  // Convert groups to FeedbackListEntry[]
  const result: FeedbackListEntry[] = [];

  for (const { sourceContainer, targetContainer, edges: groupEdges } of groups.values()) {
    if (groupEdges.length === 1) {
      result.push(groupEdges[0]);
    } else {
      result.push(new FeedbackEdgeGroup(sourceContainer, targetContainer, groupEdges));
    }
  }

  return result;
}
