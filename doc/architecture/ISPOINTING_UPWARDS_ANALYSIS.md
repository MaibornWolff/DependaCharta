# isPointingUpwards Calculation Analysis

## Current Situation

### Backend Data
The backend provides `isPointingUpwards: boolean` for each **leaf-to-leaf** dependency in the JSON:
```json
{
  "de.sots.cellarsandcentaurs.domain.model.HitPoints": {
    "isCyclic": false,
    "weight": 1,
    "type": "usage",
    "isPointingUpwards": true  // ← Backend calculates this
  }
}
```

### Frontend Problem
The frontend **recalculates** `isPointingUpwards` in [`Edge.isPointingUpwards()`](visualization/src/app/model/Edge.ts:25) because:

1. **Namespace Collapse**: When a namespace collapses, leaf-to-leaf edges aggregate into namespace-level edges
2. **New Source/Target**: The aggregated edge has collapsed namespaces as source/target, not the original leaves
3. **Current Approach**: Recalculates based on the visible (collapsed) nodes

### Example Scenario

```
Before collapse (leaf-to-leaf):
  domain.model.Creature (level 1) → application.CreatureFacade (level 0)
  isPointingUpwards: true (from backend, 1 ≥ 0)

After collapsing "domain" namespace:
  domain (level 0) → application (level 1)  
  Current: Recalculates based on collapsed nodes (0 < 1 = false)
  Desired: Aggregate from leaf edges (ANY was true = true)
```

## The Core Issue

The current implementation **recalculates** `isPointingUpwards` for aggregated edges, which:
- ❌ Duplicates backend logic
- ❌ Can produce different results than backend
- ❌ Goes against the principle of backend as single source of truth

## Proposed Solution: Aggregate Backend Data

**Never recalculate `isPointingUpwards`** - always derive it from backend data through aggregation.

### Key Insight
When aggregating edges, we can determine if the aggregated edge points upward by checking if **any** of the constituent leaf edges point upward.

### Aggregation Rules
```
When aggregating multiple leaf edges into one namespace-level edge:
- If ANY constituent edge has isPointingUpwards=true → aggregated edge is upward
- If ALL constituent edges have isPointingUpwards=false → aggregated edge is not upward

This is a logical OR operation: aggregated.isPointingUpwards = edges.some(e => e.isPointingUpwards)
```

### Implementation

```typescript
class Edge {
  readonly isPointingUpwards: boolean  // Property, not method
  
  constructor(
    source: VisibleGraphNode,
    target: VisibleGraphNode,
    isPointingUpwards: boolean,  // Always provided (from backend or aggregation)
    isCyclic: boolean,
    weight: number,
    // ... other params
  ) {
    this.isPointingUpwards = isPointingUpwards
    this.isCyclic = isCyclic
    this.weight = weight
  }
}

// In edge aggregation logic:
function aggregateEdges(edges: Edge[]): Edge {
  return new Edge(
    collapsedSource,
    collapsedTarget,
    edges.some(e => e.isPointingUpwards),  // ANY edge pointing upward
    edges.some(e => e.isCyclic),           // ANY edge cyclic (already done)
    edges.reduce((sum, e) => sum + e.weight, 0),  // Sum weights (already done)
    // ...
  )
}
```

### Example

```
Leaf edges being aggregated:
  domain.model.ClassA → application.ServiceX 
    (isPointingUpwards: false, weight: 1)
  domain.model.ClassB → application.ServiceY 
    (isPointingUpwards: true, weight: 2)
  
Aggregated edge:
  domain.model → application 
    (isPointingUpwards: true, weight: 3)  // ANY was true, sum weights
```

### Benefits
- ✅ **Never recalculates** - respects backend as single source of truth
- ✅ Simple aggregation rule (logical OR)
- ✅ No conditional logic in Edge constructor
- ✅ No extra fields needed (no originalSource/originalTarget)
- ✅ Consistent with how `isCyclic` is already aggregated
- ✅ Semantically correct: if any constituent edge violates architecture, the aggregate does too

## Implementation Steps

1. **Add `isPointingUpwards` to `ShallowEdge`**
   - Read from backend JSON in `ProjectNodeConverter.toShallowEdge()`
   - Store in `ShallowEdge` constructor

2. **Pass `isPointingUpwards` to `Edge` constructor**
   - Update `VisibleGraphNodeUtils.createEdgesForNode()` to pass the value
   - Make it a required parameter (not optional)

3. **Change `Edge.isPointingUpwards` from method to property**
   - Remove the `isPointingUpwards()` method
   - Add `readonly isPointingUpwards: boolean` property
   - Set in constructor

4. **Update edge aggregation in `Edge.aggregateEdges()`**
   - Use `edges.some(e => e.isPointingUpwards)` for aggregated value
   - This is consistent with existing `isCyclic` aggregation

5. **Remove `findSiblingsUnderLowestCommonAncestor` function**
   - No longer needed since we don't recalculate

6. **Add `EdgeType` enum and `getEdgeType()` method**
   - Use `isCyclic` and `isPointingUpwards` properties
   - Return REGULAR, CYCLIC, TWISTED, or FEEDBACK

7. **Update tests**
   - Verify aggregation works correctly
   - Test edge type classification

## Alternative Approaches (Rejected)

### Option B: Hybrid Approach
Use backend data for leaf edges, calculate for aggregated edges.

**Drawbacks:**
- ❌ Still recalculates for aggregated edges
- ❌ More complex (conditional logic)
- ❌ Against the taste preference

### Option C: Always Recalculate (Current)
Keep the current implementation.

**Drawbacks:**
- ❌ Recalculates everything
- ❌ Duplicates backend logic
- ❌ Potential inconsistency with backend
- ❌ Against the taste preference   - Add `readonly isPointingUpwards: boolean` property
   - Set in constructor

4. **Update edge aggregation in `Edge.aggregateEdges()`**
   - Use `edges.some(e => e.isPointingUpwards)` for aggregated value
   - This is consistent with existing `isCyclic` aggregation

5. **Remove `findSiblingsUnderLowestCommonAncestor` function**
   - No longer needed since we don't recalculate

6. **Add `EdgeType` enum and `getEdgeType()` method**
   - Use `isCyclic` and `isPointingUpwards` properties
   - Return REGULAR, CYCLIC, TWISTED, or FEEDBACK

7. **Update tests**
   - Verify aggregation works correctly
   - Test edge type classification

## Alternative Approaches (Rejected)

### Option B: Hybrid Approach
Use backend data for leaf edges, calculate for aggregated edges.

**Drawbacks:**
- ❌ Still recalculates for aggregated edges
- ❌ More complex (conditional logic)
- ❌ Against the taste preference

### Option C: Always Recalculate (Current)
Keep the current implementation.

**Drawbacks:**
- ❌ Recalculates everything
- ❌ Duplicates backend logic
- ❌ Potential inconsistency with backend
- ❌ Against the taste preference
