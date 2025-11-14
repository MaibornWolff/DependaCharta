# Multi-Tier Model Design

This document provides a detailed design for the new multi-tier model structure for DependaCharta's frontend state.

## Design Goals

1. **Clear Separation of Concerns**: Each tier has a single responsibility
2. **Immutability**: Core data structures are immutable
3. **Performance**: Optimize for common operations
4. **Type Safety**: Strong typing throughout
5. **Extensibility**: Easy to add new features
6. **Testability**: Each tier can be tested independently

## Directory Structure

```
visualization/src/app/model/v2/
├── types/                  # Shared type definitions
│   ├── ids.ts              # ID types
│   ├── enums.ts            # Shared enums
│   └── common.ts           # Common interfaces
├── tier1-structure/        # Project structure (immutable)
│   ├── namespace.ts
│   ├── leaf.ts
│   └── project-structure.ts
├── tier2-dependencies/     # Dependency graph (immutable)
│   ├── dependency.ts
│   └── dependency-graph.ts
├── tier3-focus/            # Focus state (mutable)
│   └── focus-state.ts
├── tier4-aggregation/      # Edge aggregation (computed)
│   ├── aggregated-edge.ts
│   └── edge-cache.ts
├── tier5-hidden/           # Hidden nodes (mutable)
│   └── hidden-nodes-state.ts
├── ui/                     # UI state (mutable)
│   └── ui-state.ts
├── actions/                # Actions for state transitions
│   ├── action-types.ts
│   └── reducers/
├── state-v2.ts             # Composed state
└── feature-flag.ts         # Feature flag for switching models
```

## Detailed Type Definitions

### ID Types (`ids.ts`)

```typescript
// Base class for all node IDs
export class NodeId {
  constructor(readonly value: string) {}
  
  toString(): string {
    return this.value
  }
  
  equals(other: NodeId): boolean {
    return this.value === other.value
  }
  
  static create(id: string): NodeId {
    return new NodeId(id)
  }
}

// Namespace ID class
export class NamespaceId extends NodeId {
  private constructor(value: string) {
    super(value)
  }
  
  static create(id: string): NamespaceId {
    return new NamespaceId(id)
  }
  
  getParent(): NamespaceId | null {
    const parts = this.value.split('.')
    return parts.length > 1
      ? NamespaceId.create(parts.slice(0, -1).join('.'))
      : null
  }
  
  getDepth(): number {
    return this.value.split('.').length
  }
  
  isAncestorOf(other: NodeId): boolean {
    return other.value.startsWith(this.value + '.')
  }
}

// Leaf ID class
export class LeafId extends NodeId {
  private constructor(value: string) {
    super(value)
  }
  
  static create(id: string): LeafId {
    return new LeafId(id)
  }
  
  getNamespaceId(): NamespaceId {
    const parts = this.value.split('.')
    return NamespaceId.create(parts.slice(0, -1).join('.'))
  }
}
```

### Enums (`enums.ts`)

```typescript
export enum EdgeType {
  REGULAR = "REGULAR",   // !cyclic && !upward -> Grey
  CYCLIC = "CYCLIC",     // cyclic && !upward -> Blue
  FEEDBACK = "FEEDBACK", // cyclic && upward -> Red
  TWISTED = "TWISTED"    // !cyclic && upward -> Red
}

export enum EdgeColor {
  GREY = "GREY",
  BLUE = "BLUE",
  RED = "RED"
}

export enum NodeType {
  CLASS = "CLASS",
  INTERFACE = "INTERFACE",
  ANNOTATION = "ANNOTATION",
  ENUM = "ENUM",
  METHOD = "METHOD",
  UNKNOWN = "UNKNOWN"
}

export enum Language {
  JAVA = "JAVA",
  TYPESCRIPT = "TYPESCRIPT",
  PYTHON = "PYTHON",
  CSHARP = "CSHARP",
  CPP = "CPP",
  GO = "GO",
  PHP = "PHP"
}

export enum FilterType {
  ALL = "ALL",
  NONE = "NONE",
  CYCLES_ONLY = "CYCLES_ONLY",
  FEEDBACK_EDGES_ONLY = "FEEDBACK_EDGES_ONLY",
  FEEDBACK_EDGES_AND_TWISTED_EDGES = "FEEDBACK_EDGES_AND_TWISTED_EDGES",
}
```

### Common Types (`common.ts`)

```typescript
import { EdgeType, NodeType, Language } from './enums'
import { LeafId, NamespaceId, NodeId } from './ids'

export interface Position {
  x: number
  y: number
}

export interface Size {
  width: number
  height: number
}

export interface Bounds {
  position: Position
  size: Size
}

export interface EdgeMetadata {
  weight: number
  isCyclic: boolean
  isPointingUpwards: boolean
  type: string
}

export interface VisibleNode {
  id: NodeId
  label: string
  level: number
  isExpanded: boolean
  isLeaf: boolean
  bounds?: Bounds
}

export interface VisibleEdge {
  sourceId: NodeId
  targetId: NodeId
  weight: number
  edgeType: EdgeType
  color: EdgeColor
}
```

## Tier 1: Project Structure (Immutable)

### Namespace (`namespace.ts`)

```typescript
import { NamespaceId, LeafId } from '../types/ids'

export interface NamespaceProps {
  id: NamespaceId
  name: string
  level: number
  parentId?: NamespaceId
  childNamespaceIds: NamespaceId[]
  leafIds: LeafId[]
}

export class Namespace {
  readonly id: NamespaceId
  readonly name: string
  readonly level: number
  readonly parentId?: NamespaceId
  readonly childNamespaceIds: ReadonlyArray<NamespaceId>
  readonly leafIds: ReadonlyArray<LeafId>
  
  constructor(props: NamespaceProps) {
    this.id = props.id
    this.name = props.name
    this.level = props.level
    this.parentId = props.parentId
    this.childNamespaceIds = [...props.childNamespaceIds]
    this.leafIds = [...props.leafIds]
  }
  
  static create(props: NamespaceProps): Namespace {
    return new Namespace(props)
  }
  
  // Immutable update
  with(updates: Partial<NamespaceProps>): Namespace {
    return new Namespace({
      id: this.id,
      name: this.name,
      level: this.level,
      parentId: this.parentId,
      childNamespaceIds: [...this.childNamespaceIds],
      leafIds: [...this.leafIds],
      ...updates
    })
  }
  
  // Helper methods
  hasChildren(): boolean {
    return this.childNamespaceIds.length > 0 || this.leafIds.length > 0
  }
  
  isRoot(): boolean {
    return !this.parentId
  }
  
  getDepth(): number {
    return this.id.split('.').length
  }
}
```

### Leaf (`leaf.ts`)

```typescript
import { LeafId, NamespaceId } from '../types/ids'
import { NodeType, Language } from '../types/enums'

export interface LeafProps {
  id: LeafId
  name: string
  level: number
  namespaceId: NamespaceId
  physicalPath: string
  nodeType: NodeType
  language: Language
}

export class Leaf {
  readonly id: LeafId
  readonly name: string
  readonly level: number
  readonly namespaceId: NamespaceId
  readonly physicalPath: string
  readonly nodeType: NodeType
  readonly language: Language
  
  constructor(props: LeafProps) {
    this.id = props.id
    this.name = props.name
    this.level = props.level
    this.namespaceId = props.namespaceId
    this.physicalPath = props.physicalPath
    this.nodeType = props.nodeType
    this.language = props.language
  }
  
  static create(props: LeafProps): Leaf {
    return new Leaf(props)
  }
  
  // Immutable update
  with(updates: Partial<LeafProps>): Leaf {
    return new Leaf({
      id: this.id,
      name: this.name,
      level: this.level,
      namespaceId: this.namespaceId,
      physicalPath: this.physicalPath,
      nodeType: this.nodeType,
      language: this.language,
      ...updates
    })
  }
}
```

### Project Structure (`project-structure.ts`)

```typescript
import { Namespace } from './namespace'
import { Leaf } from './leaf'
import { NamespaceId, LeafId, NodeId } from '../types/ids'

export class ProjectStructure {
  readonly rootNamespaces: ReadonlyArray<Namespace>
  readonly allNamespaces: ReadonlyMap<NamespaceId, Namespace>
  readonly allLeaves: ReadonlyMap<LeafId, Leaf>
  
  private constructor(
    rootNamespaces: Namespace[],
    allNamespaces: Map<NamespaceId, Namespace>,
    allLeaves: Map<LeafId, Leaf>
  ) {
    this.rootNamespaces = rootNamespaces
    this.allNamespaces = allNamespaces
    this.allLeaves = allLeaves
  }
  
  static create(
    rootNamespaces: Namespace[],
    allNamespaces: Map<NamespaceId, Namespace>,
    allLeaves: Map<LeafId, Leaf>
  ): ProjectStructure {
    return new ProjectStructure(rootNamespaces, allNamespaces, allLeaves)
  }
  
  // Lookup methods
  getNamespace(id: NamespaceId): Namespace | undefined {
    return this.allNamespaces.get(id)
  }
  
  getLeaf(id: LeafId): Leaf | undefined {
    return this.allLeaves.get(id)
  }
  
  getNode(id: NodeId): Namespace | Leaf | undefined {
    if (NodeId.isNamespaceId(id)) {
      return this.getNamespace(id as NamespaceId)
    } else if (NodeId.isLeafId(id)) {
      return this.getLeaf(id as LeafId)
    }
    return undefined
  }
  
  // Helper methods
  getChildNamespaces(namespaceId: NamespaceId): Namespace[] {
    const namespace = this.getNamespace(namespaceId)
    if (!namespace) return []
    
    return namespace.childNamespaceIds
      .map(id => this.getNamespace(id))
      .filter((ns): ns is Namespace => ns !== undefined)
  }
  
  getLeaves(namespaceId: NamespaceId): Leaf[] {
    const namespace = this.getNamespace(namespaceId)
    if (!namespace) return []
    
    return namespace.leafIds
      .map(id => this.getLeaf(id))
      .filter((leaf): leaf is Leaf => leaf !== undefined)
  }
  
  getParentNamespace(nodeId: NodeId): Namespace | undefined {
    if (NodeId.isNamespaceId(nodeId)) {
      const parentId = NamespaceId.getParent(nodeId as NamespaceId)
      return parentId ? this.getNamespace(parentId) : undefined
    } else if (NodeId.isLeafId(nodeId)) {
      const namespaceId = LeafId.getNamespace(nodeId as LeafId)
      return this.getNamespace(namespaceId)
    }
    return undefined
  }
  
  // Path methods
  getPathToRoot(nodeId: NodeId): NodeId[] {
    const path: NodeId[] = [nodeId]
    let current = this.getParentNamespace(nodeId)
    
    while (current) {
      path.push(current.id)
      current = this.getParentNamespace(current.id)
    }
    
    return path
  }
  
  findLowestCommonAncestor(nodeId1: NodeId, nodeId2: NodeId): NamespaceId | null {
    const path1 = new Set(this.getPathToRoot(nodeId1))
    const path2 = this.getPathToRoot(nodeId2)
    
    for (const id of path2) {
      if (path1.has(id) && NodeId.isNamespaceId(id)) {
        return id as NamespaceId
      }
    }
    
    return null
  }
  
  // Factory methods
  static fromProjectReport(json: any): ProjectStructure {
    // Implementation to convert from backend JSON format
    // ...
    return new ProjectStructure([], new Map(), new Map())
  }
}
```

## Tier 2: Dependency Graph (Immutable)

### Dependency (`dependency.ts`)

```typescript
import { LeafId } from '../types/ids'
import { EdgeType } from '../types/enums'

export interface DependencyProps {
  sourceLeafId: LeafId
  targetLeafId: LeafId
  weight: number
  isCyclic: boolean
  isPointingUpwards: boolean
  type: string
}

export class Dependency {
  readonly sourceLeafId: LeafId
  readonly targetLeafId: LeafId
  readonly weight: number
  readonly isCyclic: boolean
  readonly isPointingUpwards: boolean
  readonly type: string
  
  constructor(props: DependencyProps) {
    this.sourceLeafId = props.sourceLeafId
    this.targetLeafId = props.targetLeafId
    this.weight = props.weight
    this.isCyclic = props.isCyclic
    this.isPointingUpwards = props.isPointingUpwards
    this.type = props.type
  }
  
  static create(props: DependencyProps): Dependency {
    return new Dependency(props)
  }
  
  // Derived properties
  get edgeType(): EdgeType {
    if (this.isCyclic && this.isPointingUpwards) {
      return EdgeType.FEEDBACK
    } else if (this.isCyclic && !this.isPointingUpwards) {
      return EdgeType.CYCLIC
    } else if (!this.isCyclic && this.isPointingUpwards) {
      return EdgeType.TWISTED
    } else {
      return EdgeType.REGULAR
    }
  }
  
  // Immutable update
  with(updates: Partial<DependencyProps>): Dependency {
    return new Dependency({
      sourceLeafId: this.sourceLeafId,
      targetLeafId: this.targetLeafId,
      weight: this.weight,
      isCyclic: this.isCyclic,
      isPointingUpwards: this.isPointingUpwards,
      type: this.type,
      ...updates
    })
  }
}
```

### Dependency Graph (`dependency-graph.ts`)

```typescript
import { Dependency } from './dependency'
import { LeafId } from '../types/ids'
import { EdgeType } from '../types/enums'

export class DependencyGraph {
  // Map from source leaf ID to array of dependencies
  readonly outgoingDependencies: ReadonlyMap<LeafId, ReadonlyArray<Dependency>>
  
  // Map from target leaf ID to array of dependencies
  readonly incomingDependencies: ReadonlyMap<LeafId, ReadonlyArray<Dependency>>
  
  private constructor(
    outgoingDependencies: Map<LeafId, Dependency[]>,
    incomingDependencies: Map<LeafId, Dependency[]>
  ) {
    this.outgoingDependencies = outgoingDependencies
    this.incomingDependencies = incomingDependencies
  }
  
  static create(dependencies: Dependency[]): DependencyGraph {
    const outgoing = new Map<LeafId, Dependency[]>()
    const incoming = new Map<LeafId, Dependency[]>()
    
    for (const dep of dependencies) {
      // Add to outgoing
      if (!outgoing.has(dep.sourceLeafId)) {
        outgoing.set(dep.sourceLeafId, [])
      }
      outgoing.get(dep.sourceLeafId)!.push(dep)
      
      // Add to incoming
      if (!incoming.has(dep.targetLeafId)) {
        incoming.set(dep.targetLeafId, [])
      }
      incoming.get(dep.targetLeafId)!.push(dep)
    }
    
    return new DependencyGraph(outgoing, incoming)
  }
  
  // Query methods
  getOutgoingDependencies(leafId: LeafId): ReadonlyArray<Dependency> {
    return this.outgoingDependencies.get(leafId) || []
  }
  
  getIncomingDependencies(leafId: LeafId): ReadonlyArray<Dependency> {
    return this.incomingDependencies.get(leafId) || []
  }
  
  getAllDependencies(leafId: LeafId): ReadonlyArray<Dependency> {
    return [
      ...this.getOutgoingDependencies(leafId),
      ...this.getIncomingDependencies(leafId)
    ]
  }
  
  // Filter methods
  getDependenciesByType(edgeType: EdgeType): ReadonlyArray<Dependency> {
    const result: Dependency[] = []
    
    for (const deps of this.outgoingDependencies.values()) {
      for (const dep of deps) {
        if (dep.edgeType === edgeType) {
          result.push(dep)
        }
      }
    }
    
    return result
  }
  
  getCyclicDependencies(): ReadonlyArray<Dependency> {
    const result: Dependency[] = []
    
    for (const deps of this.outgoingDependencies.values()) {
      for (const dep of deps) {
        if (dep.isCyclic) {
          result.push(dep)
        }
      }
    }
    
    return result
  }
  
  getUpwardPointingDependencies(): ReadonlyArray<Dependency> {
    const result: Dependency[] = []
    
    for (const deps of this.outgoingDependencies.values()) {
      for (const dep of deps) {
        if (dep.isPointingUpwards) {
          result.push(dep)
        }
      }
    }
    
    return result
  }
  
  // Factory methods
  static fromProjectReport(json: any): DependencyGraph {
    // Implementation to convert from backend JSON format
    // ...
    return new DependencyGraph(new Map(), new Map())
  }
}
```

## Tier 3: Focus State (Mutable)

### Focus State (`focus-state.ts`)

```typescript
import { NamespaceId, NodeId } from '../types/ids'
import { ProjectStructure } from '../tier1-structure/project-structure'
import { VisibleNode } from '../types/common'

export class FocusState {
  readonly expandedNamespaceIds: ReadonlySet<NamespaceId>
  readonly pinnedNodeIds: ReadonlySet<NodeId>
  readonly hoveredNodeId: NodeId | null
  
  private constructor(
    expandedNamespaceIds: Set<NamespaceId>,
    pinnedNodeIds: Set<NodeId>,
    hoveredNodeId: NodeId | null
  ) {
    this.expandedNamespaceIds = expandedNamespaceIds
    this.pinnedNodeIds = pinnedNodeIds
    this.hoveredNodeId = hoveredNodeId
  }
  
  static create(): FocusState {
    return new FocusState(new Set(), new Set(), null)
  }
  
  // Query methods
  isExpanded(namespaceId: NamespaceId): boolean {
    return this.expandedNamespaceIds.has(namespaceId)
  }
  
  isPinned(nodeId: NodeId): boolean {
    return this.pinnedNodeIds.has(nodeId)
  }
  
  isHovered(nodeId: NodeId): boolean {
    return this.hoveredNodeId === nodeId
  }
  
  // Immutable update methods
  expand(namespaceId: NamespaceId): FocusState {
    const newExpanded = new Set(this.expandedNamespaceIds)
    newExpanded.add(namespaceId)
    return new FocusState(newExpanded, this.pinnedNodeIds, this.hoveredNodeId)
  }
  
  collapse(namespaceId: NamespaceId): FocusState {
    const newExpanded = new Set(this.expandedNamespaceIds)
    newExpanded.delete(namespaceId)
    return new FocusState(newExpanded, this.pinnedNodeIds, this.hoveredNodeId)
  }
  
  pin(nodeId: NodeId): FocusState {
    const newPinned = new Set(this.pinnedNodeIds)
    newPinned.add(nodeId)
    return new FocusState(this.expandedNamespaceIds, newPinned, this.hoveredNodeId)
  }
  
  unpin(nodeId: NodeId): FocusState {
    const newPinned = new Set(this.pinnedNodeIds)
    newPinned.delete(nodeId)
    return new FocusState(this.expandedNamespaceIds, newPinned, this.hoveredNodeId)
  }
  
  hover(nodeId: NodeId | null): FocusState {
    return new FocusState(this.expandedNamespaceIds, this.pinnedNodeIds, nodeId)
  }
  
  // Derived state
  getVisibleNodes(structure: ProjectStructure): VisibleNode[] {
    const result: VisibleNode[] = []
    
    // Helper function to recursively add visible nodes
    const addVisibleNodes = (namespaceId: NamespaceId, isExpanded: boolean) => {
      const namespace = structure.getNamespace(namespaceId)
      if (!namespace) return
      
      // Add namespace itself
      result.push({
        id: namespace.id,
        label: namespace.name,
        level: namespace.level,
        isExpanded,
        isLeaf: false
      })
      
      // If expanded, add children
      if (isExpanded) {
        // Add child namespaces
        for (const childId of namespace.childNamespaceIds) {
          const childExpanded = this.isExpanded(childId)
          addVisibleNodes(childId, childExpanded)
        }
        
        // Add leaves
        for (const leafId of namespace.leafIds) {
          const leaf = structure.getLeaf(leafId)
          if (leaf) {
            result.push({
              id: leaf.id,
              label: leaf.name,
              level: leaf.level,
              isExpanded: false,
              isLeaf: true
            })
          }
        }
      }
    }
    
    // Start with root namespaces
    for (const root of structure.rootNamespaces) {
      const isExpanded = this.isExpanded(root.id)
      addVisibleNodes(root.id, isExpanded)
    }
    
    return result
  }
}
```

## Tier 4: Aggregated Edge Cache (Computed)

### Aggregated Edge (`aggregated-edge.ts`)

```typescript
import { NodeId } from '../types/ids'
import { EdgeType, EdgeColor } from '../types/enums'
import { Dependency } from '../tier2-dependencies/dependency'

export interface AggregatedEdgeProps {
  sourceNodeId: NodeId
  targetNodeId: NodeId
  weight: number
  edgeType: EdgeType
  contributingDependencies: Dependency[]
}

export class AggregatedEdge {
  readonly sourceNodeId: NodeId
  readonly targetNodeId: NodeId
  readonly weight: number
  readonly edgeType: EdgeType
  readonly contributingDependencies: ReadonlyArray<Dependency>
  
  constructor(props: AggregatedEdgeProps) {
    this.sourceNodeId = props.sourceNodeId
    this.targetNodeId = props.targetNodeId
    this.weight = props.weight
    this.edgeType = props.edgeType
    this.contributingDependencies = [...props.contributingDependencies]
  }
  
  static create(props: AggregatedEdgeProps): AggregatedEdge {
    return new AggregatedEdge(props)
  }
  
  // Derived properties
  get color(): EdgeColor {
    switch (this.edgeType) {
      case EdgeType.REGULAR:
        return EdgeColor.GREY
      case EdgeType.CYCLIC:
        return EdgeColor.BLUE
      case EdgeType.FEEDBACK:
      case EdgeType.TWISTED:
        return EdgeColor.RED
    }
  }
  
  get id(): string {
    return `${this.sourceNodeId}-${this.targetNodeId}-${this.edgeType}`
  }
  
  // Immutable update
  with(updates: Partial<AggregatedEdgeProps>): AggregatedEdge {
    return new AggregatedEdge({
      sourceNodeId: this.sourceNodeId,
      targetNodeId: this.targetNodeId,
      weight: this.weight,
      edgeType: this.edgeType,
      contributingDependencies: [...this.contributingDependencies],
      ...updates
    })
  }
}
```

### Edge Cache (`edge-cache.ts`)

```typescript
import { AggregatedEdge } from './aggregated-edge'
import { ProjectStructure } from '../tier1-structure/project-structure'
import { DependencyGraph } from '../tier2-dependencies/dependency-graph'
import { FocusState } from '../tier3-focus/focus-state'
import { HiddenNodesState } from '../tier5-hidden/hidden-nodes-state'
import { NodeId, LeafId } from '../types/ids'
import { EdgeType, FilterType } from '../types/enums'
import { VisibleNode } from '../types/common'

export class EdgeCache {
  private cache: Map<string, AggregatedEdge[]>
  
  constructor() {
    this.cache = new Map()
  }
  
  // Cache key generation
  private getCacheKey(
    focusState: FocusState,
    hiddenState: HiddenNodesState,
    filterType: FilterType
  ): string {
    // Generate a unique key based on state
    const expandedKey = Array.from(focusState.expandedNamespaceIds).sort().join(',')
    const pinnedKey = Array.from(focusState.pinnedNodeIds).sort().join(',')
    const hoveredKey = focusState.hoveredNodeId || 'none'
    const hiddenKey = Array.from(hiddenState.hiddenNodeIds).sort().join(',')
    
    return `${expandedKey}|${pinnedKey}|${hoveredKey}|${hiddenKey}|${filterType}`
  }
  
  // Main method to get edges
  getAggregatedEdges(
    structure: ProjectStructure,
    dependencies: DependencyGraph,
    focusState: FocusState,
    hiddenState: HiddenNodesState,
    filterType: FilterType
  ): AggregatedEdge[] {
    const cacheKey = this.getCacheKey(focusState, hiddenState, filterType)
    
    // Check cache first
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey)!
    }
    
    // Get visible nodes
    const visibleNodes = focusState.getVisibleNodes(structure)
      .filter(node => !hiddenState.isHidden(node.id))
    
    // Calculate edges
    const edges = this.calculateEdges(
      structure,
      dependencies,
      visibleNodes,
      focusState,
      filterType
    )
    
    // Store in cache
    this.cache.set(cacheKey, edges)
    
    return edges
  }
  
  // Calculate edges based on current state
  private calculateEdges(
    structure: ProjectStructure,
    dependencies: DependencyGraph,
    visibleNodes: VisibleNode[],
    focusState: FocusState,
    filterType: FilterType
  ): AggregatedEdge[] {
    // Map of node IDs to visible nodes for quick lookup
    const visibleNodeMap = new Map(
      visibleNodes.map(node => [node.id, node])
    )
    
    // Get leaf nodes (unexpanded)
    const leafNodes = visibleNodes.filter(node => node.isLeaf)
    
    // Get unexpanded namespace nodes
    const unexpandedNamespaces = visibleNodes.filter(
      node => !node.isLeaf && !node.isExpanded
    )
    
    // Calculate edges for leaf nodes
    const leafEdges = this.calculateLeafEdges(
      structure,
      dependencies,
      leafNodes,
      visibleNodeMap
    )
    
    // Calculate edges for unexpanded namespaces
    const namespaceEdges = this.calculateNamespaceEdges(
      structure,
      dependencies,
      unexpandedNamespaces,
      visibleNodeMap
    )
    
    // Combine all edges
    let allEdges = [...leafEdges, ...namespaceEdges]
    
    // Apply filter
    allEdges = this.applyFilter(allEdges, filterType)
    
    // Apply focus (hover/pin)
    allEdges = this.applyFocus(allEdges, focusState)
    
    return allEdges
  }
  
  // Calculate edges for leaf nodes
  private calculateLeafEdges(
    structure: ProjectStructure,
    dependencies: DependencyGraph,
    leafNodes: VisibleNode[],
    visibleNodeMap: Map<NodeId, VisibleNode>
  ): AggregatedEdge[] {
    const result: AggregatedEdge[] = []
    
    for (const leaf of leafNodes) {
      const leafId = leaf.id as LeafId
      
      // Get outgoing dependencies
      const outgoing = dependencies.getOutgoingDependencies(leafId)
      
      for (const dep of outgoing) {
        // Find best visible target
        const targetId = this.findBestVisibleTarget(
          dep.targetLeafId,
          structure,
          visibleNodeMap
        )
        
        if (targetId && targetId !== leaf.id) {
          // Create aggregated edge
          result.push(new AggregatedEdge({
            sourceNodeId: leaf.id,
            targetNodeId: targetId,
            weight: dep.weight,
            edgeType: dep.edgeType,
            contributingDependencies: [dep]
          }))
        }
      }
    }
    
    return result
  }
  
  // Calculate edges for unexpanded namespaces
  private calculateNamespaceEdges(
    structure: ProjectStructure,
    dependencies: DependencyGraph,
    namespaces: VisibleNode[],
    visibleNodeMap: Map<NodeId, VisibleNode>
  ): AggregatedEdge[] {
    // Group edges by source, target, and type
    const edgeGroups = new Map<string, {
      sourceId: NodeId,
      targetId: NodeId,
      edgeType: EdgeType,
      weight: number,
      deps: Set<Dependency>
    }>()
    
    for (const namespace of namespaces) {
      // Get all leaves in this namespace
      const leaves = this.getAllLeavesInNamespace(namespace.id, structure)
      
      for (const leafId of leaves) {
        // Get outgoing dependencies
        const outgoing = dependencies.getOutgoingDependencies(leafId)
        
        for (const dep of outgoing) {
          // Find best visible target
          const targetId = this.findBestVisibleTarget(
            dep.targetLeafId,
            structure,
            visibleNodeMap
          )
          
          if (targetId && targetId !== namespace.id) {
            // Create or update edge group
            const key = `${namespace.id}-${targetId}-${dep.edgeType}`
            
            if (!edgeGroups.has(key)) {
              edgeGroups.set(key, {
                sourceId: namespace.id,
                targetId,
                edgeType: dep.edgeType,
                weight: 0,
                deps: new Set()
              })
            }
            
            const group = edgeGroups.get(key)!
            group.weight += dep.weight
            group.deps.add(dep)
          }
        }
      }
    }
    
    // Convert groups to aggregated edges
    return Array.from(edgeGroups.values()).map(group => 
      new AggregatedEdge({
        sourceNodeId: group.sourceId,
        targetNodeId: group.targetId,
        weight: group.weight,
        edgeType: group.edgeType,
        contributingDependencies: Array.from(group.deps)
      })
    )
  }
  
  // Helper to find best visible target for a dependency
  private findBestVisibleTarget(
    targetLeafId: LeafId,
    structure: ProjectStructure,
    visibleNodeMap: Map<NodeId, VisibleNode>
  ): NodeId | null {
    // Check if target leaf is visible
    if (visibleNodeMap.has(targetLeafId)) {
      return targetLeafId
    }
    
    // Walk up the namespace hierarchy
    let currentId: NodeId = targetLeafId
    let parent = structure.getParentNamespace(currentId)
    
    while (parent) {
      if (visibleNodeMap.has(parent.id)) {
        return parent.id
      }
      
      currentId = parent.id
      parent = structure.getParentNamespace(currentId)
    }
    
    return null
  }
  
  // Helper to get all leaves in a namespace (recursively)
  private getAllLeavesInNamespace(
    namespaceId: NodeId,
    structure: ProjectStructure
  ): LeafId[] {
    const result: LeafId[] = []
    
    const namespace = structure.getNamespace(namespaceId as any)
    if (!namespace) return result
    
    // Add direct leaves
    result.push(...namespace.leafIds)
    
    // Add leaves from child namespaces
    for (const childId of namespace.childNamespaceIds) {
      result.push(...this.getAllLeavesInNamespace(childId, structure))
    }
    
    return result
  }
  
  // Apply filter to edges
  private applyFilter(
    edges: AggregatedEdge[],
    filterType: FilterType
  ): AggregatedEdge[] {
    switch (filterType) {
      case FilterType.ALL:
        return edges
      case FilterType.NONE:
        return []
      case FilterType.CYCLES_ONLY:
        return edges.filter(edge => 
          edge.edgeType === EdgeType.CYCLIC || 
          edge.edgeType === EdgeType.FEEDBACK
        )
      case FilterType.FEEDBACK_EDGES_ONLY:
        return edges.filter(edge => 
          edge.edgeType === EdgeType.FEEDBACK
        )
      case FilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES:
        return edges.filter(edge => 
          edge.edgeType === EdgeType.FEEDBACK || 
          edge.edgeType === EdgeType.TWISTED
        )
    }
  }
  
  // Apply focus (hover/pin) to edges
  private applyFocus(
    edges: AggregatedEdge[],
    focusState: FocusState
  ): AggregatedEdge[] {
    // If nothing is hovered or pinned, return all edges
    if (!focusState.hoveredNodeId && focusState.pinnedNodeIds.size === 0) {
      return edges
    }
    
    // Get nodes in focus
    const nodesInFocus = new Set<NodeId>()
    
    if (focusState.hoveredNodeId) {
      nodesInFocus.add(focusState.hoveredNodeId)
    }
    
    for (const id of focusState.pinnedNodeIds) {
      nodesInFocus.add(id)
    }
    
    // Filter edges connected to focused nodes
    return edges.filter(edge => 
      nodesInFocus.has(edge.sourceNodeId) || 
      nodesInFocus.has(edge.targetNodeId)
    )
  }
  
  // Invalidate cache
  invalidate(): void {
    this.cache.clear()
  }
}
```

## Tier 5: Hidden Nodes State (Mutable)

### Hidden Nodes State (`hidden-nodes-state.ts`)

```typescript
import { NodeId, NamespaceId } from '../types/ids'
import { ProjectStructure } from '../tier1-structure/project-structure'

export class HiddenNodesState {
  readonly hiddenNodeIds: ReadonlySet<NodeId>
  readonly hiddenChildrenByParent: ReadonlyMap<NamespaceId, ReadonlySet<NodeId>>
  
  private constructor(
    hiddenNodeIds: Set<NodeId>,
    hiddenChildrenByParent: Map<NamespaceId, Set<NodeId>>
  ) {
    this.hiddenNodeIds = hiddenNodeIds
    this.hiddenChildrenByParent = hiddenChildrenByParent
  }
  
  static create(): HiddenNodesState {
    return new HiddenNodesState(new Set(), new Map())
  }
  
  // Query methods
  isHidden(nodeId: NodeId): boolean {
    return this.hiddenNodeIds.has(nodeId)
  }
  
  getHiddenChildren(namespaceId: NamespaceId): ReadonlySet<NodeId> {
    return this.hiddenChildrenByParent.get(namespaceId) || new Set()
  }
  
  // Immutable update methods
  hide(nodeId: NodeId, structure: ProjectStructure): HiddenNodesState {
    const newHiddenIds = new Set(this.hiddenNodeIds)
    newHiddenIds.add(nodeId)
    
    const newHiddenByParent = new Map(this.hiddenChildrenByParent)
    
    // Update parent's hidden children
    const parent = structure.getParentNamespace(nodeId)
    if (parent) {
      const parentHidden = this.hiddenChildrenByParent.get(parent.id) || new Set()
      const newParentHidden = new Set(parentHidden)
      newParentHidden.add(nodeId)
      newHiddenByParent.set(parent.id, newParentHidden)
    }
    
    return new HiddenNodesState(newHiddenIds, newHiddenByParent)
  }
  
  restore(nodeId: NodeId): HiddenNodesState {
    const newHiddenIds = new Set(this.hiddenNodeIds)
    newHiddenIds.delete(nodeId)
    
    const newHiddenByParent = new Map(this.hiddenChildrenByParent)
    
    // Update all parents' hidden children
    for (const [parentId, hiddenChildren] of this.hiddenChildrenByParent.entries()) {
      if (hiddenChildren.has(nodeId)) {
        const newHiddenChildren = new Set(hiddenChildren)
        newHiddenChildren.delete(nodeId)
        newHiddenByParent.set(parentId, newHiddenChildren)
      }
    }
    
    return new HiddenNodesState(newHiddenIds, newHiddenByParent)
  }
  
  restoreAllChildren(namespaceId: NamespaceId): HiddenNodesState {
    const hiddenChildren = this.hiddenChildrenByParent.get(namespaceId)
    if (!hiddenChildren || hiddenChildren.size === 0) {
      return this
    }
    
    const newHiddenIds = new Set(this.hiddenNodeIds)
    for (const childId of hiddenChildren) {
      newHiddenIds.delete(childId)
    }
    
    const newHiddenByParent = new Map(this.hiddenChildrenByParent)
    newHiddenByParent.set(namespaceId, new Set())
    
    return new HiddenNodesState(newHiddenIds, newHiddenByParent)
  }
  
  restoreAll(): HiddenNodesState {
    return HiddenNodesState.create()
  }
}
```

## UI State (`ui-state.ts`)

```typescript
import { NodeId } from '../types/ids'
import { FilterType } from '../types/enums'

export interface UIStateProps {
  selectedNodeIds: NodeId[]
  showLabels: boolean
  selectedFilter: FilterType
  isInteractive: boolean
  multiselectMode: boolean
}

export class UIState {
  readonly selectedNodeIds: ReadonlyArray<NodeId>
  readonly showLabels: boolean
  readonly selectedFilter: FilterType
  readonly isInteractive: boolean
  readonly multiselectMode: boolean
  
  constructor(props: UIStateProps) {
    this.selectedNodeIds = [...props.selectedNodeIds]
    this.showLabels = props.showLabels
    this.selectedFilter = props.selectedFilter
    this.isInteractive = props.isInteractive
    this.multiselectMode = props.multiselectMode
  }
  
  static create(): UIState {
    return new UIState({
      selectedNodeIds: [],
      showLabels: true,
      selectedFilter: FilterType.FEEDBACK_EDGES_AND_TWISTED_EDGES,
      isInteractive: true,
      multiselectMode: false
    })
  }
  
  // Immutable update
  with(updates: Partial<UIStateProps>): UIState {
    return new UIState({
      selectedNodeIds: this.selectedNodeIds,
      showLabels: this.showLabels,
      selectedFilter: this.selectedFilter,
      isInteractive: this.isInteractive,
      multiselectMode: this.multiselectMode,
      ...updates
    })
  }
  
  // Helper methods
  isSelected(nodeId: NodeId): boolean {
    return this.selectedNodeIds.includes(nodeId)
  }
  
  toggleSelection(nodeId: NodeId): UIState {
    const index = this.selectedNodeIds.indexOf(nodeId)
    let newSelected: NodeId[]
    
    if (index >= 0) {
      newSelected = [
        ...this.selectedNodeIds.slice(0, index),
        ...this.selectedNodeIds.slice(index + 1)
      ]
    } else {
      newSelected = [...this.selectedNodeIds, nodeId]
    }
    
    return this.with({ selectedNodeIds: newSelected })
  }
  
  clearSelection(): UIState {
    return this.with({ selectedNodeIds: [] })
  }
  
  toggleLabels(): UIState {
    return this.with({ showLabels: !this.showLabels })
  }
  
  setFilter(filter: FilterType): UIState {
    return this.with({ selectedFilter: filter })
  }
  
  toggleInteractive(): UIState {
    return this.with({ isInteractive: !this.isInteractive })
  }
  
  setMultiselectMode(enabled: boolean): UIState {
    return this.with({ multiselectMode: enabled })
  }
}
```

## Composed State (`state-v2.ts`)

```typescript
import { ProjectStructure } from './tier1-structure/project-structure'
import { DependencyGraph } from './tier2-dependencies/dependency-graph'
import { FocusState } from './tier3-focus/focus-state'
import { EdgeCache } from './tier4-aggregation/edge-cache'
import { HiddenNodesState } from './tier5-hidden/hidden-nodes-state'
import { UIState } from './ui/ui-state'
import { AggregatedEdge } from './tier4-aggregation/aggregated-edge'
import { VisibleNode } from './types/common'
import { NodeId, NamespaceId, LeafId } from './types/ids'
import { Action } from './actions/action-types'

export class StateV2 {
  // Immutable tiers (loaded once)
  readonly structure: ProjectStructure
  readonly dependencies: DependencyGraph
  
  // Mutable tiers (user interactions)
  readonly focus: FocusState
  readonly hidden: HiddenNodesState
  
  // Computed tier (cached)
  private edgeCache: EdgeCache
  
  // UI state (separate from domain)
  readonly ui: UIState
  
  private constructor(
    structure: ProjectStructure,
    dependencies: DependencyGraph,
    focus: FocusState,
    hidden: HiddenNodesState,
    edgeCache: EdgeCache,
    ui: UIState
  ) {
    this.structure = structure
    this.dependencies = dependencies
    this.focus = focus
    this.hidden = hidden
    this.edgeCache = edgeCache
    this.ui = ui
  }
  
  static create(
    structure: ProjectStructure,
    dependencies: DependencyGraph
  ): StateV2 {
    return new StateV2(
      structure,
      dependencies,
      FocusState.create(),
      HiddenNodesState.create(),
      new EdgeCache(),
      UIState.create()
    )
  }
  
  // Derived state
  getVisibleNodes(): VisibleNode[] {
    return this.focus.getVisibleNodes(this.structure)
      .filter(node => !this.hidden.isHidden(node.id))
  }
  
  getVisibleEdges(): AggregatedEdge[] {
    return this.edgeCache.getAggregatedEdges(
      this.structure,
      this.dependencies,
      this.focus,
      this.hidden,
      this.ui.selectedFilter
    )
  }
  
  // State transition
  reduce(action: Action): StateV2 {
    // Handle action based on type
    // ...
    
    // For now, just return this
    return this
  }
  
  // Factory methods
  static fromProjectReport(json: any): StateV2 {
    const structure = ProjectStructure.fromProjectReport(json)
    const dependencies = DependencyGraph.fromProjectReport(json)
    
    return StateV2.create(structure, dependencies)
  }
}
```

## Feature Flag (`feature-flag.ts`)

```typescript
export enum ModelVersion {
  V1 = 'v1',
  V2 = 'v2'
}

export class FeatureFlag {
  private static instance: FeatureFlag
  private currentVersion: ModelVersion = ModelVersion.V1
  
  private constructor() {}
  
  static getInstance(): FeatureFlag {
    if (!FeatureFlag.instance) {
      FeatureFlag.instance = new FeatureFlag()
    }
    return FeatureFlag.instance
  }
  
  useV2Model(): boolean {
    return this.currentVersion === ModelVersion.V2
  }
  
  setVersion(version: ModelVersion): void {
    this.currentVersion = version
  }
}
```

## Migration Strategy

### Phase 1: Parallel Implementation

1. Create the new model classes in `visualization/src/app/model/v2/`
2. Implement adapters to convert between old and new models
3. Add feature flag to switch between models

### Phase 2: Validation

1. Create comparison tests to ensure both models produce the same results
2. Add performance benchmarks
3. Fix any discrepancies

### Phase 3: Integration

1. Update components to use new model through adapters
2. Gradually migrate features to use new model directly
3. Keep old model as fallback

### Phase 4: Cleanup

1. Remove old model once fully validated
2. Update documentation
3. Remove feature flag and adapters

## Next Steps

1. Implement the core types and IDs
2. Implement Tier 1: ProjectStructure
3. Implement Tier 2: DependencyGraph
4. Implement Tier 3: FocusState
5. Implement Tier 4: AggregatedEdgeCache
6. Implement Tier 5: HiddenNodesState
7. Implement UIState
8. Compose StateV2
9. Add feature flag
10. Create comparison tests
11. Update DOMAIN.md