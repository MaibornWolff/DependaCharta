import {InitializeState, ExpandNode, CollapseNode, ToggleNodeSelection, EnterMultiselectMode, LeaveMultiselectMode, ChangeFilter, ToggleEdgeLabels, HideNode, RestoreNodes} from './Action'
import {EdgeFilterType} from './EdgeFilter'
import {State} from './State'
import {GraphNode, VisibleGraphNode} from './GraphNode.spec'

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
      const newState = new State().reduce(new InitializeState('', [rootNode]))

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
      const state = State.buildFromRootNodes([rootNode])

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
      const state = State.buildFromRootNodes([rootNode]).copy({
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
      const rootNode = VisibleGraphNode.build({
        id: rootId,
        isSelected: false
      })
      const state = State.buildFromRootNodes([rootNode])

      // when
      const newState = state.reduce(new ToggleNodeSelection(rootId))

      // then
      expect(newState.selectedNodeIds).toEqual([rootId])
    })

    it('should remove id of selected node from selectedNodeIds when toggling a selected node', () => {
      // given
      const rootId = "de"
      const rootNode = VisibleGraphNode.build({
        id: rootId,
        isSelected: true
      })
      const state = State.buildFromRootNodes([rootNode]).copy({ selectedNodeIds: [rootId] })

      // when
      const newState = state.reduce(new ToggleNodeSelection(rootId))

      // then
      expect(newState.selectedNodeIds).toEqual([])
    })

    it('should enter multiselect mode', () => {
      // given
      const rootNode = GraphNode.build()
      const state = State.buildFromRootNodes([rootNode]).copy({ multiselectMode: false })

      // when
      const newState = state.reduce(new EnterMultiselectMode())

      // then
      expect(newState.multiselectMode).toEqual(true)
    })


    it('should leave multiselect mode', () => {
      // given
      const rootNode = GraphNode.build()
      const state = State.buildFromRootNodes([rootNode]).copy({ multiselectMode: true })

      // when
      const newState = state.reduce(new LeaveMultiselectMode())

      // then
      expect(newState.multiselectMode).toEqual(false)
    })

    it('should clear selected nodes when leaving multiselect mode', () => {
      // given
      const rootNode = GraphNode.build()
      const state = State.buildFromRootNodes([rootNode]).copy({ multiselectMode: true, selectedNodeIds: [rootNode.id] })

      // when
      const newState = state.reduce(new LeaveMultiselectMode())

      // then
      expect(newState.selectedNodeIds).toEqual([])
    })

    it('should change filter', () => {
      // given
      const rootNode = GraphNode.build()
      const state = State.buildFromRootNodes([rootNode])
      const newFilter = EdgeFilterType.ALL

      // when
      const newState = state.reduce(new ChangeFilter(newFilter))

      // then
      expect(newState.selectedFilter).toEqual(newFilter)
    })

    it('should toggle showLabels', () => {
      // given
      const rootNode = GraphNode.build()
      const state = State.buildFromRootNodes([rootNode])
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
      const state = State.buildFromRootNodes([rootNode])

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
      const state: State = State.buildFromRootNodes([rootNode]).copy({
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
      const childNode = VisibleGraphNode.build({
        id: childId
      })
      const rootNode = VisibleGraphNode.build({
        id: rootId,
        children: [childNode],
        hiddenChildrenIds: [childId],
        visibleChildren: []
      })
      const state: State = State.buildFromRootNodes([rootNode]).copy({
        expandedNodeIds: [rootId],
        hiddenNodeIds: [childId],
        hiddenChildrenIdsByParentId: new Map().set(rootId, [childId])
      })

      // when
      const visibleNodes = state.getVisibleNodes()

      // then
      expect(visibleNodes.map(node => node.id)).toContain(rootNode.id)
      expect(visibleNodes.map(node => node.id)).not.toContain(childNode.id)
    })
  })

  describe('findGraphNode', () => {
    it('should return GraphNode for id', () => {
      // given
      const rootId = "de"
      const rootNode = VisibleGraphNode.build({
        id: rootId
      })
      const state = State.buildFromRootNodes([rootNode])
      // when
      const foundNode = state.findGraphNode(rootId)

      // then
      expect(foundNode).toEqual(rootNode)
    })

    it('should throw an error if the node does not exist', () => {
      // given
      const state = new State()

      // when + then
      expect(() => state.findGraphNode("some random id")).toThrowError()
    })
  })
})
