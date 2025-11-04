import {VisibleGraphNode} from './GraphNode';

// same as cytoscapeConfig.ts
export const CytoscapeValues = {
  MIN_WIDTH_OF_COMPOUND_NODE: 100,
  MIN_HEIGHT_OF_COMPOUND_NODE: 50,
  PADDING_OF_COMPOUND_NODE: 30,
  WIDTH_OF_SINGULAR_NODE: 100,
  HEIGHT_OF_SINGULAR_NODE: 50,
}

const PADDING_BETWEEN_NODES = 60
const PADDING_BETWEEN_LEVELS = 60

export interface Coordinates {
  x: number,
  y: number
}

type SpaceRequirement = Coordinates & {
  level: number
}

type RelativePositionOfNode = SpaceRequirement & { nodeId: string }

function calculateRequiredSpaceForLevel(nodesOfLevel: VisibleGraphNode[]) {
  const spaceRequirementsOfNodes = nodesOfLevel.map(node => calculateRequiredSpace(node))
  const widthOfChildren = spaceRequirementsOfNodes
    .map(requirement => requirement.x)
    .reduce((width, currentWidth) => width + currentWidth, 0)
  const heightOfChildren = Math.max(...spaceRequirementsOfNodes.map(requirement => requirement.y))

  const requiredXSpace = widthOfChildren + (nodesOfLevel.length - 1) * PADDING_BETWEEN_NODES
  return {
    x: requiredXSpace,
    y: heightOfChildren
  }
}

export function calculateRequiredSpace(node: VisibleGraphNode): SpaceRequirement {
  if (!node.isExpanded) {
    return {
      x: CytoscapeValues.WIDTH_OF_SINGULAR_NODE,
      y: CytoscapeValues.HEIGHT_OF_SINGULAR_NODE,
      level: node.level
    }
  }

  const levelsOfNode = [...new Set(node.visibleChildren.map(child => child.level))]
  const requiredSpaceByLevel: SpaceRequirement[] = []
  levelsOfNode.forEach(level => {
    const childrenOfLevel = node.visibleChildren.filter(child => child.level === level)
    const requiredSpaceForLevel = calculateRequiredSpaceForLevel(childrenOfLevel)
    requiredSpaceByLevel.push({...requiredSpaceForLevel, level})
  })

  const maxLevelWidth = Math.max(...requiredSpaceByLevel.map(requirement => requirement.x), 0)
  let totalLevelHeight = requiredSpaceByLevel
    .map(requirement => requirement.y)
    .reduce((height, currentHeight) => height + currentHeight, 0)
  if (requiredSpaceByLevel.length > 0) {
    totalLevelHeight += (requiredSpaceByLevel.length - 1) * PADDING_BETWEEN_LEVELS
  }
  const finalWidth = Math.max(maxLevelWidth, CytoscapeValues.MIN_WIDTH_OF_COMPOUND_NODE) + (CytoscapeValues.PADDING_OF_COMPOUND_NODE * 2)
  const finalHeight = Math.max(totalLevelHeight, CytoscapeValues.MIN_HEIGHT_OF_COMPOUND_NODE) + (CytoscapeValues.PADDING_OF_COMPOUND_NODE * 2)

  return {
    x: finalWidth,
    y: finalHeight,
    level: node.level
  }
}

function calculateSpaceRequirementsOfChildren(node: VisibleGraphNode): RelativePositionOfNode[] {
  if (!node.isExpanded) {
    return []
  }
  let currentX = 0;
  let currentY = 0;
  const result: RelativePositionOfNode[] = []
  const widthByLevel = new Map<number, number>()
  const levelsOfNode = [...new Set(node.visibleChildren.map(child => child.level))].sort((a, b) => a - b)
  levelsOfNode.forEach(level => {
    const childrenOfLevel = node.visibleChildren.filter(child => child.level === level)
    childrenOfLevel.sort((node1, node2) =>
      node2.label.localeCompare(node1.label)
    )
    const requiredSpaceForLevel = calculateRequiredSpaceForLevel(childrenOfLevel)
    childrenOfLevel.forEach(child => {
      const requiredSpace = calculateRequiredSpace(child)
      result.push({
        nodeId: child.id,
        level: child.level,
        x: currentX,
        y: currentY
      })
      currentX += requiredSpace.x + PADDING_BETWEEN_NODES
    })
    currentY += requiredSpaceForLevel.y + PADDING_BETWEEN_LEVELS
    widthByLevel.set(level, requiredSpaceForLevel.x)
    currentX = 0
  })

  const widthOfWidestLevel = Math.max(...widthByLevel.values())
  const xOffsetByLevel = new Map<number, number>()
  widthByLevel.forEach((widthOfLevel, level) => {
    const offset = (widthOfWidestLevel - widthOfLevel) / 2;
    xOffsetByLevel.set(level, offset)
  })

  return result.map(requirement => ({
      nodeId: requirement.nodeId,
      level: requirement.level,
      x: requirement.x + (xOffsetByLevel.get(requirement.level) || 0),
      y: requirement.y
    })
  )
}

function layoutSubgraph(node: VisibleGraphNode): RelativePositionOfNode[] {
  const spaceRequirementsOfDirectChildren = calculateSpaceRequirementsOfChildren(node);
  const spaceRequirementsOfGrandChildren = node.visibleChildren.flatMap(layoutSubgraph)
  return spaceRequirementsOfDirectChildren.concat(spaceRequirementsOfGrandChildren)
}

export function layout(leveledNodes: VisibleGraphNode[]): Map<string, Coordinates> {
  const rootWrapperNode: VisibleGraphNode = {
    id: 'root-wrapper',
    label: '',
    children: leveledNodes,
    level: 0,
    dependencies: [],
    hiddenChildrenIds: [],
    visibleChildren: leveledNodes,
    isExpanded: true,
    isSelected: false
  }
  const relativePositionsByNodeId = new Map<string, Coordinates>()
  layoutSubgraph(rootWrapperNode).forEach(relativePosition => {
    relativePositionsByNodeId.set(relativePosition.nodeId, { x: relativePosition.x, y: relativePosition.y })
  })
  return relativePositionsByNodeId
}
