import {LeafIdCreator} from './LeafIdCreator';

export interface ProjectReport {
  filename: string,
  projectTreeRoots: ProjectNode[],
  leaves: Record<string, NodeInformation>
}

export interface ProjectNode {
  name: string
  leafId?: string
  level: number
  children: ProjectNode[]
  containedLeaves: string[]
  containedInternalDependencies: Record<string, EdgeMetaInformation>
}

export interface EdgeMetaInformation {
  weight: number
  isCyclic: boolean
  isPointingUpwards: boolean
  type: string
}

export interface NodeInformation {
  name: string
  physicalPath: string
  language: Language
  nodeType: NodeType
  dependencies: Record<string, EdgeMetaInformation>
}

export enum Language {
  JAVA = "JAVA"
}

export enum NodeType {
  CLASS = "CLASS",
  INTERFACE = "INTERFACE",
  ANNOTATION = "ANNOTATION",
  ENUM = "ENUM",
  METHOD = "METHOD",
  UNKNOWN = "UNKNOWN"
}

export function getChildren(id: string, nodesToCheck: ProjectNode[], parentPath: string): ProjectNode[] {
  return nodesToCheck.flatMap(node => {
    const nodeId = getNodeId(node, parentPath)
    if (nodeId === id) {
      return node.children
    } else {
      if (!node.children) {
        return []
      }
      return getChildren(id, node.children, nodeId)
    }
  })
}

export function getNodeId(projectNode: ProjectNode, parentNodeId: string): string {
  if (projectNode.leafId) {
    return LeafIdCreator.createFrom(projectNode.leafId)
  }

  return parentNodeId ? `${parentNodeId}.${projectNode.name}` : `${projectNode.name}`
}
