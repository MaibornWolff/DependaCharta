# DependaCharta Domain Model

## Overview

DependaCharta is a **dependency cycle explorer** that analyzes and visualizes the structure of software projects. It helps identify architectural issues, particularly circular dependencies and violations of layered architecture principles.

## Core Concepts

### Leaves

A **leaf** is the atomic unit of analysis - typically a source file or module in your codebase. Leaves are the nodes in the dependency graph that actually contain code.

### Dependencies

A **dependency** is a relationship where one leaf uses, imports, or references another leaf. Dependencies are directional: if leaf A depends on leaf B, an arrow points from A to B.

### Namespaces (Packages)

**Namespaces** (also called packages or directories) organize leaves into a hierarchy. A namespace can contain:
- Other namespaces (nested structure)
- Leaves (files)

The namespace hierarchy is visualized by **nesting** - inner boxes represent contained elements. No arrows are used to represent the hierarchy itself; arrows only represent dependencies.

### Levels

Each node (leaf or namespace) is assigned a **level** based on its dependencies:
- **Level 0**: Nodes with no dependencies (foundation layer)
- **Level 1**: Nodes that depend only on level 0 nodes
- **Level 2**: Nodes that depend on level 0 or level 1 nodes
- And so on...

Levels are calculated **relative to the containing namespace**. A node's level indicates its position in the dependency hierarchy within its context.

For details on the levelization algorithm, see [`analysis/.../levelization/README.md`](analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/levelization/README.md).

### Dependency Cycles

A **cycle** occurs when dependencies form a circular path. For example:
- A → B → C → A (3-node cycle)
- A → B → A (2-node cycle)

Cycles are problematic because they create tight coupling and make code harder to understand, test, and maintain.

For details on cycle detection, see [`analysis/.../cycledetection/README.md`](analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/cycledetection/README.md).

### Pointing Upward

A dependency **points upward** when it violates the normal architectural flow by going from a lower level to the same or higher level. This is determined by comparing levels within the same namespace context:

**Within the same namespace:**
- Source and target leaves are siblings in the same namespace
- **Upward** = target level ≥ source level

**Across different namespaces:**
- Find the lowest common ancestor namespace
- Compare the levels of the sub-namespaces (immediate children of the common ancestor) containing source and target
- **Upward** = target sub-namespace level ≥ source sub-namespace level

Upward-pointing dependencies often indicate architectural violations, such as a domain layer depending on an application layer.

### Edge Weight

The **weight** of an edge represents the number of individual dependencies it represents:
- For a single leaf-to-leaf dependency: weight = 1
- When edges are aggregated (e.g., during namespace collapse): weight = sum of all contributing dependencies

Weight helps identify the strength of coupling between components.

### Edge Types and Colors

Dependencies are classified into **4 edge types** based on two properties:

| Edge Type | Cyclic at Leaf Level? | Pointing Upward? | Color | Description |
|-----------|----------------------|------------------|-------|-------------|
| **REGULAR** | No | No | **Grey** | Normal dependencies following architecture |
| **CYCLIC** | Yes | No | **Blue** | Cyclic dependencies that follow architectural flow |
| **FEEDBACK_CONTAINER_LEVEL** | No | Yes | **Red (dotted)** | Container level feedback edges - may create cycles at package/module level, architectural violations |
| **FEEDBACK_LEAF_LEVEL** | Yes | Yes | **Red (solid)** | Leaf level feedback edges - create cycles at class/function level, architectural violations |

**Color Summary:**
- **Grey edges (REGULAR)**: Normal dependencies that follow the architectural flow (neither cyclic nor upward-pointing)
- **Blue edges (CYCLIC)**: Cyclic dependencies that still respect the architectural direction (downward-pointing cycles)
- **Red edges (all feedback edges)**: Architectural violations - dependencies that point upward
  - **Dotted red (FEEDBACK_CONTAINER_LEVEL)**: May create cycles at container (package/module) level
  - **Solid red (FEEDBACK_LEAF_LEVEL)**: Create cycles at leaf (class/function) level

## Visualization Features

### Namespace Collapsing

The visualization prominently features **namespace collapsing**:

1. **Collapsed state**: A namespace shrinks to hide its contents (leaves and nested namespaces)
2. **Edge aggregation**: When a namespace is collapsed, edges are preserved but aggregated:
   - Multiple edges between the collapsed namespace and another node are **split by color**
   - One **blue edge** represents all downward-pointing cyclic dependencies (sum of weights)
   - One **red edge** represents all other dependencies (sum of weights)
3. **Expanding**: Clicking a collapsed namespace expands it to show its contents

**Aggregation Rules:**
When collapsing a namespace, the frontend dynamically aggregates all dependencies from the namespace's children:
- **Weight**: Sum of all constituent edge weights
- **isCyclic**: Logical OR - true if ANY constituent edge is cyclic
- **isPointingUpwards**: Logical OR - true if ANY constituent edge points upward

This ensures architectural violations are preserved and visible even when namespaces are collapsed, allowing you to explore the architecture at different levels of detail.

### Edge Filtering

The visualization includes interactive edge filtering:

**Hover behavior:**
- Hovering over any node (leaf or collapsed namespace) shows **all connected edges** (both incoming and outgoing)

**Pin behavior:**
- Nodes can be "pinned" to keep their edges visible even when not hovering
- Click the pin icon to pin/unpin a node

**Default filter menu:**
Users can select which edges are displayed by default:
- **None**: Clean view with no edges (use hover to explore)
- **All edges**: Show all dependencies
- **Only cyclic edges**: Show only edges that are part of cycles
- **Only upward-pointing edges**: Show only architectural violations
- **Custom combinations**: Mix and match filters

## Data Flow

```
Backend Analysis (Kotlin)
    ↓
    Analyzes source code using Tree-sitter parsers
    ↓
    Detects cycles and calculates levels
    ↓
JSON file (.cg.json)
    ├─ Namespace hierarchy (tree structure)
    ├─ Leaf-to-leaf dependencies
    ├─ Cycle detection results (isCyclic flag)
    ├─ isPointingUpwards calculation
    └─ Edge weights
    ↓
Frontend Visualization (Angular + Cytoscape.js)
    ├─ Renders namespace hierarchy as nested boxes
    ├─ Draws dependencies as colored arrows
    ├─ Aggregates edges when namespaces collapse
    ├─ Colors edges based on cycle + upward properties
    └─ Filters edges based on user interaction
```

## Terminology Reference

| Term | Definition |
|------|------------|
| **Leaf** | A file or module (atomic unit of analysis) |
| **Namespace** | A package or directory containing leaves and/or other namespaces |
| **Dependency** | A directional relationship where one leaf uses another |
| **Level** | A number indicating a node's position in the dependency hierarchy (0 = no dependencies) |
| **Cycle** | A circular path of dependencies |
| **Pointing Upward** | A dependency from lower/same level to same/higher level (architectural violation) |
| **Weight** | The number of individual dependencies an edge represents |
| **Collapsed Namespace** | A namespace that is visually shrunk to hide its contents |
| **Edge Aggregation** | Combining multiple leaf-to-leaf edges into namespace-level edges |
| **REGULAR Edge** | Normal dependency (grey) - neither cyclic nor upward-pointing |
| **CYCLIC Edge** | Cyclic dependency (blue) - forms a cycle but follows architectural direction |
| **FEEDBACK_CONTAINER_LEVEL Edge** | Container level feedback edge (red dotted) - creates cycles at package/module level, violates architecture |
| **FEEDBACK_LEAF_LEVEL Edge** | Leaf level feedback edge (red solid) - creates cycles at class/function level, violates architecture |

## Related Documentation

- [README.md](README.md) - Getting started guide
- [Cycle Detection Algorithm](analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/cycledetection/README.md)
- [Levelization Algorithm](analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/levelization/README.md)
- [Architecture Decision Records](doc/architecture/decisions/)