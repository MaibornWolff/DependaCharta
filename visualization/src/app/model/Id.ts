export class IdUtils {
  static getParent(nodeId: string): string | null {
    const parent = nodeId.substring(0, nodeId.lastIndexOf('.'))
    if (parent.length === 0) {
      return null
    }
    return parent
  }

  static isIncludedIn(includingId: string, id: string) {
    if (includingId === id) {
      return true
    }
    const includingIdParts = includingId.split(".")
    const idParts = id.split(".")
    if (includingIdParts.length >= idParts.length) {
      return false
    }
    for (let i = 0; i < includingIdParts.length; i++) {
      if (includingIdParts[i] !== idParts[i]) {
        return false
      }
    }
    return true
  }

  static isDescendantOf = (ancestorNodeIds: string[]) => (descendantNodeId: string) =>
    ancestorNodeIds
      .filter(id => descendantNodeId.startsWith(id))
      .length > 0

}
