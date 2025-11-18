import cytoscape, {LayoutPositionOptions, NodeCollection, NodeSingular} from 'cytoscape';
import {VisibleGraphNode} from '../../../model/GraphNode';
import {Coordinates, layout} from '../../../model/lsmLayouting';
import {toVisibleGraphNodes} from './converter/elementDefinitionConverter';
import {toAbsoluteCoordinates} from './converter/coordinateConverter';

export function lsmLayout(options: any): CyLsmLayout {
  return new CyLsmLayout(options)
}

class CyLsmLayout{
  options: any

  constructor(options: any) {
    this.options = options
  }

  draw(positionByNodeId: Map<string, Coordinates>, visibleNodes: VisibleGraphNode[], cyNodes: NodeCollection, layouting: any) {
    const nodesWithAbsoluteCoordinates = toAbsoluteCoordinates(positionByNodeId, visibleNodes)
    const manuallyPositionedNodes = this.options.manuallyPositionedNodes || new Map<string, Coordinates>()
    const layoutOptions: LayoutPositionOptions = {
      eles: cyNodes
    }
    cyNodes.layoutPositions(layouting, layoutOptions, (currentNode: NodeSingular) => {
      const nodeId = currentNode.data().id

      // Check if node has a manual position first
      const manualPosition = manuallyPositionedNodes.get(nodeId)
      if (manualPosition) {
        return manualPosition
      }

      // Fall back to computed position
      const coordinates = nodesWithAbsoluteCoordinates.get(nodeId)
      if (coordinates) {
        return coordinates
      } else {
        return { x: 0, y: 0 }
      }
    })
  }

  run(layouting: any) {
    const options = this.options
    const cy = options.cy
    cy.emit('layoutstart')

    const visibleNodes = toVisibleGraphNodes(cy.nodes())
    const positionByNodeId = layout(visibleNodes)
    const cyNodes = cy.nodes()

    this.draw(positionByNodeId, visibleNodes, cyNodes, layouting)
    cy.emit('layoutready')
    cy.emit('layoutstop')
  }
}

lsmLayout.prototype.run = function () {
  lsmLayout(this.options).run(this)
}
cytoscape('layout', 'lsmLayout', lsmLayout)
