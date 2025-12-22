---
name: Clarify feedback edge terminology with leaf/container level distinction
issue: #2
state: todo
version: 2
---

## Goal

Establish clear terminology that distinguishes between feedback edges at different architectural levels:
- **Feedback edges (leaf level)**: Cyclic architectural violations at the class/function level
- **Feedback edges (container level)**: Non-cyclic architectural violations at the package/module level
- **All feedback edges**: Both leaf-level and container-level feedback edges combined

This replaces the confusing term "twisted edge" with "feedback edge (container level)" and renames the existing "feedback edge" to "feedback edge (leaf level)" for clarity and consistency.

## Context

Currently, the codebase distinguishes between 4 edge types:
- **REGULAR**: Normal dependencies (grey, not cyclic, not pointing upwards)
- **CYCLIC**: Cyclic dependencies that follow architecture (blue, cyclic, not pointing upwards)
- **TWISTED**: Non-cyclic architectural violations (red dotted, not cyclic, pointing upwards) ← TO BE RENAMED
- **FEEDBACK**: Cyclic architectural violations (red solid, cyclic, pointing upwards) ← TO BE RENAMED

The current terminology is confusing:
- "Twisted" doesn't clearly indicate it's a container-level architectural violation
- "Feedback" without qualification doesn't clarify it's specifically for leaf-level (class/function) cycles

The new terminology creates a clear distinction:
- **Feedback edges (leaf level)**: Both cyclic AND pointing upwards - violations at the class/function level
- **Feedback edges (container level)**: Not cyclic but pointing upwards - violations at the package/module level
- **All feedback edges**: Any edge pointing upwards (architectural violation), regardless of cyclicity or level

## Replacement Strategy

### Terminology Mapping

#### EdgeType Enum Values
| Current | New | Rationale |
|---------|-----|-----------|
| `EdgeType.FEEDBACK` | `EdgeType.FEEDBACK_LEAF_LEVEL` | Clarifies these are leaf-level cycles |
| `EdgeType.TWISTED` | `EdgeType.FEEDBACK_CONTAINER_LEVEL` | Clarifies these are container-level violations |

#### EdgeFilterType Enum Values
| Current | New | Rationale |
|---------|-----|-----------|
| `FEEDBACK_EDGES_ONLY` | `FEEDBACK_LEAF_LEVEL_ONLY` | Shows only leaf-level feedback edges |
| `FEEDBACK_EDGES_AND_TWISTED_EDGES` | `ALL_FEEDBACK_EDGES` | Shows all feedback edges (both levels) |

#### Variable/Identifier Naming Patterns
| Context | Current | New |
|---------|---------|-----|
| camelCase | `feedbackEdge` | `feedbackLeafLevelEdge` |
| camelCase | `twistedEdge` | `feedbackContainerLevelEdge` |
| PascalCase | `FeedbackEdge` | `FeedbackLeafLevelEdge` |
| PascalCase | `TwistedEdge` | `FeedbackContainerLevelEdge` |
| SCREAMING_SNAKE | `FEEDBACK_EDGE_COLOR` | `FEEDBACK_LEAF_LEVEL_EDGE_COLOR` |
| SCREAMING_SNAKE | `TWISTED_EDGE_COLOR` | `FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR` |

#### UI Display Text
| Current | New |
|---------|-----|
| "feedback edge" | "leaf level feedback edge" |
| "twisted edge" | "container level feedback edge" |
| "Show only feedback edges" | "Show only leaf level feedback edges" |
| "Show feedback and twisted edges" | "Show all feedback edges" |

### Special Cases

1. **UI Labels**:
   - Use "Show all feedback edges" for the combined filter
   - Use "Show only leaf level feedback edges" for leaf-level only
   - Use "Show only container level feedback edges" if such a filter exists
   - Pattern: Place level qualifier BEFORE "feedback edges" for better readability
2. **Code Identifiers**:
   - Use `feedbackLeafLevel` / `feedbackLeafLevelEdge` for leaf-level feedback
   - Use `feedbackContainerLevel` / `feedbackContainerLevelEdge` for container-level feedback
3. **Constants**:
   - Use `FEEDBACK_LEAF_LEVEL` and `FEEDBACK_CONTAINER_LEVEL` (screaming snake case)
4. **Comments**:
   - Use full descriptive terms: "feedback edge (leaf level)" or "feedback edge (container level)"
   - Can abbreviate as "FLL edge" or "FCL edge" if context is very clear
5. **Filter Names**:
   - `FEEDBACK_EDGES_ONLY` → `FEEDBACK_LEAF_LEVEL_ONLY`
   - `FEEDBACK_EDGES_AND_TWISTED_EDGES` → `ALL_FEEDBACK_EDGES`

## Tasks

### 1. Update TypeScript Model Layer
- **EdgeType.ts**:
  - Rename `EdgeType.FEEDBACK` → `EdgeType.FEEDBACK_LEAF_LEVEL`
  - Rename `EdgeType.TWISTED` → `EdgeType.FEEDBACK_CONTAINER_LEVEL`
  - Update `fromEnum()` method to use new enum values
- **EdgePredicate.ts**:
  - Update case statements to handle `FEEDBACK_LEAF_LEVEL` and `FEEDBACK_CONTAINER_LEVEL`
- **EdgeFilter.ts**:
  - Rename `FEEDBACK_EDGES_ONLY` → `FEEDBACK_LEAF_LEVEL_ONLY`
  - Rename `FEEDBACK_EDGES_AND_TWISTED_EDGES` → `ALL_FEEDBACK_EDGES`
  - Update `fromEnum()` logic to return new EdgeType values
- **Edge.ts**:
  - Update comments mentioning FEEDBACK and TWISTED
- **State.ts**:
  - Update default filter from `FEEDBACK_EDGES_AND_TWISTED_EDGES` to `ALL_FEEDBACK_EDGES`

### 2. Update UI Components
- **filter.component.ts**:
  - Update filter enum array to use new filter names
  - Update display names:
    - "Show only feedback edges" → "Show only leaf level feedback edges"
    - "Show feedback and twisted edges" → "Show all feedback edges"
- **edge-display.service.ts**:
  - Rename `FEEDBACK_EDGE_COLOR` → `FEEDBACK_LEAF_LEVEL_EDGE_COLOR`
  - Rename `TWISTED_EDGE_COLOR` → `FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR`
  - Update switch cases:
    - `EdgeType.FEEDBACK` → `EdgeType.FEEDBACK_LEAF_LEVEL`
    - `EdgeType.TWISTED` → `EdgeType.FEEDBACK_CONTAINER_LEVEL`

### 3. Update Test Files

#### Visualization Tests
- **State.spec.ts**:
  - Update default filter expectation: `ALL_FEEDBACK_EDGES`
- **EdgeFilter.spec.ts**:
  - Update test names mentioning "feedback" and "twisted"
  - Rename variables:
    - `feedbackEdge` → `feedbackLeafLevelEdge`
    - `twistedEdge` → `feedbackContainerLevelEdge`
  - Update filter enum references:
    - `FEEDBACK_EDGES_ONLY` → `FEEDBACK_LEAF_LEVEL_ONLY`
    - `FEEDBACK_EDGES_AND_TWISTED_EDGES` → `ALL_FEEDBACK_EDGES`
  - Update EdgeType enum references:
    - `EdgeType.FEEDBACK` → `EdgeType.FEEDBACK_LEAF_LEVEL`
    - `EdgeType.TWISTED` → `EdgeType.FEEDBACK_CONTAINER_LEVEL`
- **Edge.spec.ts**:
  - Update test descriptions mentioning "feedback" and "twisted"
  - Update variable names: `feedbackEdge`, `twistedEdge`
  - Update filter enum references
- **EdgePredicate.spec.ts**:
  - Update test descriptions for feedback and twisted edge tests
  - Update EdgeType enum references
- **Action.spec.ts**:
  - Update filter enum references
- **edge-display.service.spec.ts**:
  - Rename tests mentioning "feedback edge" and "twisted edge"
  - Update variable names
  - Update color constant imports and references:
    - `FEEDBACK_EDGE_COLOR` → `FEEDBACK_LEAF_LEVEL_EDGE_COLOR`
    - `TWISTED_EDGE_COLOR` → `FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR`

#### Analysis Tests
- **IsPointingUpwardsTest.kt**:
  - Update test name: "should detect feedback edges (container level) between multiple top-level roots"
  - Update comments mentioning "twisted" → "feedback edge (container level)"
  - Update assertion descriptions

### 4. Update Documentation
- **DOMAIN.md**:
  - Update edge type table:
    - FEEDBACK row → FEEDBACK_LEAF_LEVEL with "leaf level feedback edge" description
    - TWISTED row → FEEDBACK_CONTAINER_LEVEL with "container level feedback edge" description
  - Update bullet points:
    - "Red edges (TWISTED + FEEDBACK)" → "Red edges (all feedback edges)"
    - Add clarification about leaf vs container level
  - Update terminology table at bottom
- **visualization/README.md**:
  - Update filter descriptions:
    - "Show only feedback edges" → "Show only leaf level feedback edges"
    - "feedback and twisted edges" → "all feedback edges"
  - Add explanation of leaf vs container level distinction

### 5. Update Test Fixtures
- **visualization/cypress/fixtures/analysis.cg.json**:
  - Update all occurrences of "feedback" identifiers (NOT inside actual code strings):
    - "isFeedbackEdge" → "isFeedbackLeafLevelEdge" (in leaf IDs and names)
    - "feedbackEdge" → "feedbackLeafLevelEdge" (in node names)
  - Update all occurrences of "twisted" identifiers:
    - "isTwistedEdge" → "isFeedbackContainerLevelEdge" (in leaf IDs and names)
    - "twistedEdge" → "feedbackContainerLevelEdge" (in node names)
  - Update constant names:
    - "FEEDBACK_EDGE_COLOR" → "FEEDBACK_LEAF_LEVEL_EDGE_COLOR"
    - "TWISTED_EDGE_COLOR" → "FEEDBACK_CONTAINER_LEVEL_EDGE_COLOR"
  - Update function names:
    - "showOnlyFeedbackEdges" → "showOnlyFeedbackLeafLevelEdges"
    - "showFeedbackEdgesAndTwistedEdges" → "showAllFeedbackEdges"
  - This is a large JSON file with many occurrences - need careful search and replace

### 6. Verify No Breaking Changes
- Ensure backward compatibility if .cg.json format is part of public API
- Check if any external tools depend on these naming conventions
- Consider adding migration notes if needed

## Steps

- [ ] Complete Task 1: Update TypeScript Model Layer (EdgeType, EdgePredicate, EdgeFilter, State)
- [ ] Complete Task 2: Update UI Components (filter component, edge display service)
- [ ] Complete Task 3: Update Visualization Test Files (State, EdgeFilter, Edge, EdgePredicate, Action, edge-display tests)
- [ ] Complete Task 4: Update Analysis Test Files (IsPointingUpwardsTest.kt)
- [ ] Complete Task 5: Update Documentation (DOMAIN.md, visualization/README.md)
- [ ] Complete Task 6: Update Test Fixtures (analysis.cg.json)
- [ ] Run all tests to ensure nothing broke (`just test`)
- [ ] Manual verification: Check UI labels display correctly
- [ ] Manual verification: Test all filter options work correctly
- [ ] Request permission to commit changes

## Files Affected

### Visualization Source Code (16 files)
1. `visualization/src/app/model/EdgeType.ts` - Enum definition
2. `visualization/src/app/model/EdgePredicate.ts` - Predicate logic
3. `visualization/src/app/model/EdgeFilter.ts` - Filter enum
4. `visualization/src/app/model/State.ts` - Default filter
5. `visualization/src/app/ui/filter/filter.component.ts` - UI labels
6. `visualization/src/app/adapter/cytoscape/internal/ui/edge-display.service.ts` - Color constants and styling

### Visualization Test Files (7 files)
7. `visualization/src/app/model/State.spec.ts`
8. `visualization/src/app/model/EdgeFilter.spec.ts`
9. `visualization/src/app/model/Edge.spec.ts`
10. `visualization/src/app/model/EdgePredicate.spec.ts`
11. `visualization/src/app/model/Action.spec.ts`
12. `visualization/src/app/adapter/cytoscape/internal/ui/edge-display.service.spec.ts`
13. `visualization/cypress/fixtures/analysis.cg.json`

### Analysis Test Files (1 file)
14. `analysis/src/test/kotlin/de/maibornwolff/dependacharta/pipeline/processing/levelization/IsPointingUpwardsTest.kt`

### Documentation (2 files)
15. `DOMAIN.md`
16. `visualization/README.md`

## Notes

### Rationale for Leaf/Container Level Distinction
The current terminology is ambiguous and doesn't clearly distinguish between two fundamentally different types of architectural violations:

**Feedback edges (leaf level)** - Currently just "feedback edges":
- Both **cyclic** (form a cycle at class/function level) AND **pointing upwards** (violate architecture)
- Example: Class A in package X depends on Class B in package Y, and B depends back on A
- These are the most serious violations: both architectural AND circular dependencies

**Feedback edges (container level)** - Currently "twisted edges":
- **Not cyclic** (no class-level cycle) but **pointing upwards** (violate architecture)
- Example: Class in lower-level package depends on class in higher-level package
- Architectural violations that don't create circular dependencies at the code level

The new terminology makes this distinction explicit:
1. "Leaf level" = class/function level (the leaves of the package tree)
2. "Container level" = package/module level (the containers in the package tree)
3. "All feedback edges" = any architectural violation (upward-pointing), regardless of cyclicity or level

### Implementation Order
The renaming should proceed in dependency order:
1. Model layer first (EdgeType, EdgePredicate, EdgeFilter, State)
2. UI components second (they depend on model types)
3. Tests third (they depend on both model and UI)
4. Documentation last (no code dependencies)

### Testing Strategy
- All unit tests must pass after each step
- Manual testing of UI filter functionality required
- Verify that edge coloring still works correctly (red dotted line for FCL edges)
- Check that filter combinations work as expected

### Capitalization Preservation
When doing search-and-replace operations, it's critical to preserve the exact capitalization:

**For "feedback" → "feedbackLeafLevel":**
- `feedback` → `feedbackLeafLevel` (camelCase)
- `Feedback` → `FeedbackLeafLevel` (PascalCase)
- `FEEDBACK` → `FEEDBACK_LEAF_LEVEL` (SCREAMING_SNAKE_CASE)
- `feedback edge` → `leaf level feedback edge` (lowercase phrase for UI)
- `Feedback edge` → `Leaf level feedback edge` (sentence case for UI)

**For "twisted" → "feedbackContainerLevel":**
- `twisted` → `feedbackContainerLevel` (camelCase)
- `Twisted` → `FeedbackContainerLevel` (PascalCase)
- `TWISTED` → `FEEDBACK_CONTAINER_LEVEL` (SCREAMING_SNAKE_CASE)
- `twisted edge` → `container level feedback edge` (lowercase phrase for UI)
- `Twisted edge` → `Container level feedback edge` (sentence case for UI)

### Potential Issues
1. **Long identifier names**:
   - `feedbackLeafLevelEdge` and `feedbackContainerLevelEdge` are quite long
   - May need line breaks in some places or use abbreviations in very constrained contexts
2. **UI space constraints**:
   - "feedback edge (leaf level)" and "feedback edge (container level)" are verbose
   - UI should prefer these full terms for clarity, but tooltips can provide more detail
3. **Backward compatibility**:
   - This is a breaking change if .cg.json format is part of a public API
   - All existing analysis results with old enum values will need regeneration
4. **Git history**:
   - The term "twisted" appears in commit messages and branch names (e.g., current branch "agency/2-twisted")
   - Historical references remain unchanged (only code/docs are updated)
5. **Scope of changes**:
   - This is a large refactoring touching many files
   - Must be done atomically to avoid inconsistent state
   - All tests must pass before committing

### Alternative Shorter Terms (if needed)
If space becomes severely constrained in UI:
- **For leaf level**: "leaf feedback edges", "cyclic feedback", "FLL edges"
- **For container level**: "container feedback edges", "architectural feedback", "FCL edges"
- **For all**: "all feedback edges" (already concise)

However, prefer the full terms for clarity in all user-facing text:
- "Show only leaf level feedback edges" (preferred)
- "Show all feedback edges" (preferred)

## Summary of Changes

### Core Terminology Changes
| Old | New | Reason |
|-----|-----|--------|
| FEEDBACK | FEEDBACK_LEAF_LEVEL | Clarify it's a leaf (class) level cycle |
| TWISTED | FEEDBACK_CONTAINER_LEVEL | Clarify it's a container (package) level violation |
| FEEDBACK_EDGES_ONLY | FEEDBACK_LEAF_LEVEL_ONLY | Consistent with new terminology |
| FEEDBACK_EDGES_AND_TWISTED_EDGES | ALL_FEEDBACK_EDGES | Simpler and clearer |

### Scope of Impact
- **17 files** total need updating
- **6 source files** in visualization
- **7 test files** in visualization
- **1 test file** in analysis
- **2 documentation files**
- **1 large test fixture** (JSON)

### Expected Benefits
1. **Clearer mental model**: Users understand leaf vs container level distinction
2. **Consistent terminology**: All feedback edges are explicitly qualified by level
3. **Better discoverability**: "All feedback edges" is more intuitive than "feedback and twisted"
4. **Reduced confusion**: No more wondering what "twisted" means

### Breaking Changes
- **EdgeType enum**: `FEEDBACK` and `TWISTED` values change
- **EdgeFilterType enum**: `FEEDBACK_EDGES_ONLY` and `FEEDBACK_EDGES_AND_TWISTED_EDGES` values change
- **Constants**: Color constant names change
- **Generated .cg.json files**: Will need regeneration with new terminology

### Migration Path
1. Update all code/tests atomically in one commit
2. Regenerate all test fixtures
3. Update documentation to explain new terminology
4. All existing .cg.json files generated with old CLI will still work (no file format change)
5. Future .cg.json files will use new terminology in any embedded identifiers
