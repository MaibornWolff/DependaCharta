import {getAncestors, GraphNode, VisibleGraphNode} from './GraphNode';
import {EdgeFilterType} from './EdgeFilter';
import {State} from './State';
import {ValueObject} from '../common/ValueObject';

export class Edge extends ValueObject<Edge> {
  declare readonly source: VisibleGraphNode
  declare readonly target: VisibleGraphNode
  declare readonly id: string
  declare readonly weight: number
  declare readonly isCyclic: boolean
  declare readonly type: string

  // TODO `Edge` should have a property `isPointingUpwards: boolean`
  // It should be set when when Edge is created in `toGraphEdges`
  // TODO `Edge` should have a function `getType(): EdgeType`
  // !isCyclic && !isPointingUpwards => REGULAR
  // isCyclic && !isPointingUpwards => CYCLIC
  // !isCyclic && isPointingUpwards => TWISTED
  // isCyclic && isPointingUpwards => FEEDBACK
  // TODO (next) `EdgePredicate`, `EdgeFilter`, `EdgeFilterResult` can be removed
  isPointingUpwards(): boolean {
    const [sourceNode, targetNode] = findSiblingsUnderLowestCommonAncestor(this.source, this.target)
    return sourceNode.level <= targetNode.level
  }
}

/*
    +----+
    |    |           Level 1 Sibling
    +----+
   ↙️     ↖️↘️
+----+   +----+
|    |   |    |      Level 0 Siblings
+----+   +----+
*/
function findSiblingsUnderLowestCommonAncestor(source: GraphNode, target: GraphNode): [GraphNode, GraphNode] {
  for (const sourceAncestor of getAncestors(source)) {
    for (const targetAncestor of getAncestors(target)) {
      if (sourceAncestor.parent?.id === targetAncestor.parent?.id) {
        return [sourceAncestor, targetAncestor]
      }
    }
  }

  throw new Error("No common ancestor found")
}

