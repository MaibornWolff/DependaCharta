import {Action, InitializeState, ExpandNode, CollapseNode, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, ChangeFilter, ToggleEdgeLabels, HideNode, RestoreNodes} from './Action'
import {buildVisibleGraphNode} from './ModelBuilders.spec'
import {EdgeFilterType} from './EdgeFilter'
import {findGraphNode, getVisibleNodes, State, reduce} from './State'
import {GraphNode} from './GraphNode.spec'
import { buildFromRootNodes } from './State.spec'

describe('State Handler', () => {
  describe('Graph State Reducer', () => {

    it('should create the initial state', () => {
      // given
      const rootId = "de"
      const childId = "de.org";
      const childNode = GraphNode.build({
        id: childId
      })
      const rootNode = GraphNode.build({
        id: rootId,
        children: [childNode]
      })

      // when
      const newState = State.build().reduce(new InitializeState('', [rootNode]))

      // then
      expect(newState.showLabels).toEqual(true)
      expect(newState.selectedFilter).toEqual(EdgeFilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES)
      expect(newState.allNodes.find(node => node.id == rootId)).toEqual(rootNode)
      expect(newState.allNodes.find(node => node.id == childId)).toEqual(childNode)
      expect(newState.hiddenNodeIds).toEqual([])
      expect(newState.hiddenChildrenIdsByParentId).toEqual(new Map())
      expect(newState.expandedNodeIds).toEqual([])
      expect(newState.selectedNodeIds).toEqual([])
    })

    it('should add id of expanded node to expandedNodeIds', () => {
      // given
      const rootId = "de"
      const childId = "de.org";
      const childNode = GraphNode.build({
        id: childId
      })
      const rootNode = GraphNode.build({
        id: rootId,
        children: [childNode]
      })
      const state = buildFromRootNodes([rootNode])

      // when
      const newState = state.reduce(new ExpandNode(rootId))

      // then
      expect(newState.expandedNodeIds).toEqual([rootId])
    })

    it('should remove id of collapsed node from expandedNodeIds', () => {
      // given
      const rootId = "de"
      const childId = "de.org";
      const grandchildId = "de.org.sub";
      const grandchildNode = GraphNode.build({
        id: grandchildId
      })
      const childNode = GraphNode.build({
        id: childId,
        children: [grandchildNode]
      })
      const rootNode = GraphNode.build({
        id: rootId,
        children: [childNode]
      })
      const state = buildFromRootNodes([rootNode]).copy({
        expandedNodeIds: [rootId]
      })

      // when
      const newState = state.reduce(new CollapseNode(rootId))

      // then
      expect(newState.expandedNodeIds).toEqual([])
    })

    it('should add id of selected node to selectedNodeIds when toggling an unselected node', () => {
      // given
      const rootId = "de"
      const rootNode = buildVisibleGraphNode({
        id: rootId,
        isSelected: false
      })
      const state = buildFromRootNodes([rootNode])

      // when
      const newState = state.reduce(new ToggleNodeSelection(rootId))

      // then
      expect(newState.selectedNodeIds).toEqual([rootId])
    })

    it('should remove id of selected node from selectedNodeIds when toggling a selected node', () => {
      // given
      const rootId = "de"
      const rootNode = buildVisibleGraphNode({
        id: rootId,
        isSelected: true
      })
      const base = buildFromRootNodes([rootNode])
      const state = State.build({ allNodes: base.allNodes, selectedNodeIds: [rootId] })

      // when
      const newState = state.reduce(new ToggleNodeSelection(rootId))

      // then
      expect(newState.selectedNodeIds).toEqual([])
    })

    it('should enter multiselect mode', () => {
      // given
      const rootNode = GraphNode.build()
      const base = buildFromRootNodes([rootNode])
      const state = State.build({ allNodes: base.allNodes, multiselectMode: false })

      // when
      const newState = state.reduce(new EnterMultiselectMode())

      // then
      expect(newState.multiselectMode).toEqual(true)
    })


    it('should leave multiselect mode', () => {
      // given
      const rootNode = GraphNode.build()
      const base = buildFromRootNodes([rootNode])
      const state = State.build({ allNodes: base.allNodes, multiselectMode: true })

      // when
      const newState = state.reduce(new LeaveMultiselectMode())

      // then
      expect(newState.multiselectMode).toEqual(false)
    })

    it('should clear selected nodes when leaving multiselect mode', () => {
      // given
      const rootNode = GraphNode.build()
      const base = buildFromRootNodes([rootNode])
      const state = State.build({ allNodes: base.allNodes, multiselectMode: true, selectedNodeIds: [rootNode.id] })

      // when
      const newState = state.reduce(new LeaveMultiselectMode())

      // then
      expect(newState.selectedNodeIds).toEqual([])
    })

    it('should change filter', () => {
      // given
      const rootNode = GraphNode.build()
      const state = buildFromRootNodes([rootNode])
      const newFilter = EdgeFilterType.ALL

      // when
      const newState = state.reduce(new ChangeFilter(newFilter))

      // then
      expect(newState.selectedFilter).toEqual(newFilter)
    })

    it('should toggle showLabels', () => {
      // given
      const rootNode = GraphNode.build()
      const state = buildFromRootNodes([rootNode])
      const showLabelsBefore = state.showLabels

      // when
      const newState = state.reduce(new ToggleEdgeLabels())

      // then
      expect(newState.showLabels).toEqual(!showLabelsBefore)
    })

    it('should add id of node to hiddenNodeIds and hiddenChildren', () => {
      // given
      const rootId = "de"
      const childId = "de.org";
      const grandchildId = "de.org.sub";
      const grandchildNode = GraphNode.build({
        id: grandchildId
      })
      const childNode = GraphNode.build({
        id: childId,
        children: [grandchildNode]
      })
      const rootNode = GraphNode.build({
        id: rootId,
        children: [childNode]
      })
      childNode.parent = rootNode
      const state = buildFromRootNodes([rootNode])

      // when
      const newState = state.reduce(new HideNode(childId))

      // then
      expect(newState.hiddenNodeIds).toContain(childId)
      expect(newState.hiddenChildrenIdsByParentId.get(rootId)).toEqual([childId])
    })

    it('should restore all hidden nodes', () => {
      // given
      const rootId = "de"
      const childId = "de.org";
      const grandchildId = "de.org.sub";
      const grandchildNode = GraphNode.build({
        id: grandchildId
      })
      const childNode = GraphNode.build({
        id: childId,
        children: [grandchildNode]
      })
      const rootNode = GraphNode.build({
        id: rootId,
        children: [childNode]
      })
      const state: State = buildFromRootNodes([rootNode]).copy({
        hiddenNodeIds: [rootId, childId, grandchildId]
      })

      // when
      const newState = state.reduce(new RestoreNodes())

      // then
      expect(newState.hiddenNodeIds).toEqual([])
    })
  })

  describe('getVisibleNodes', () => {
    it('should not return hidden nodes', () => {
      // given
      const rootId = "de"
      const childId = "de.org";
      const childNode = buildVisibleGraphNode({
        id: childId
      })
      const rootNode = buildVisibleGraphNode({
        id: rootId,
        children: [childNode],
        hiddenChildrenIds: [childId],
        visibleChildren: []
      })
      const state: State = buildFromRootNodes([rootNode]).copy({
        expandedNodeIds: [rootId],
        hiddenNodeIds: [childId],
        hiddenChildrenIdsByParentId: new Map().set(rootId, [childId])
      })

      // when
      const visibleNodes = getVisibleNodes(state)

      // then
      expect(visibleNodes.map(node => node.id)).toContain(rootNode.id)
      expect(visibleNodes.map(node => node.id)).not.toContain(childNode.id)
    })
  })

  describe('findGraphNode', () => {
    it('should return GraphNode for id', () => {
      // given
      const rootId = "de"
      const rootNode = buildVisibleGraphNode({
        id: rootId
      })
      const state = buildFromRootNodes([rootNode])
      // when
      const foundNode = findGraphNode(rootId, state)

      // then
      expect(foundNode).toEqual(rootNode)
    })

    it('should throw an error if the node does not exist', () => {
      // given
      const state = State.build()

      // when + then
      expect(() => findGraphNode("some random id", state)).toThrowError()
    })
  })
})
