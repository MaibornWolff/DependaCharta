# Plan: Fix Edge Numbers/Labels Not Rendering on Graphs

## Problem Statement
Edge weight numbers (dependency counts) are not displaying on the graph visualization despite:
- The feature being fully implemented in the codebase
- The toggle button being checked/tested
- No JavaScript console errors appearing
- Tests passing for the label display functionality

## Current Implementation (What Should Work)

### Feature Location
**File:** `visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.ts:56-81`

The `toCytoscapeEdge` function should display numbers when:
1. `showLabels` state is `true`
2. Edge `weight > 1`

**Display Logic:**
```typescript
if (showLabels && graphEdge.weight > 1) {
  elementDefinition.style = {
    label: graphEdge.weight,
    'font-size': 20
  }
}
```

### Toggle Control
- **UI Button:** `visualization/src/app/app.component.html:9`
- **State Management:** `visualization/src/app/model/State.ts:76-79`
- **Default:** `showLabels: true`

## Investigation Steps

### 1. Verify Toggle State Persistence
**Goal:** Confirm that clicking "Toggle Edge Labels" actually updates the state and triggers re-rendering

**Tasks:**
- Add console logging to `app.component.ts:onToggleEdgeLabelsClick()` to verify button clicks
- Add console logging to `State.ts` toggle handler to verify state updates
- Add logging to `elementDefinitionConverter.ts:toCytoscapeEdge()` to log `showLabels` parameter value
- Run the app and toggle the button while watching console output

**Expected Behavior:** Each toggle should log state changes and trigger converter calls with updated `showLabels` value

**Potential Issues:**
- State not updating (Action not dispatched)
- Re-rendering not triggered after state change
- Cytoscape not being notified of style updates

---

### 2. Verify Edge Weight Values
**Goal:** Ensure edges have `weight > 1` to trigger label display

**Tasks:**
- Add console logging to `elementDefinitionConverter.ts:toCytoscapeEdge()` to log each edge's weight
- Analyze test data or real project data to check weight distribution
- Verify the analysis component is correctly calculating edge weights

**Expected Behavior:** At least some edges should have `weight > 1`

**Potential Issues:**
- All edges have `weight === 1` (condition never met)
- Weight values not being set correctly during graph creation
- Analysis tool not aggregating duplicate edges properly

---

### 3. Verify Cytoscape Style Application
**Goal:** Ensure style objects are correctly applied to Cytoscape elements

**Tasks:**
- Add logging before returning from `toCytoscapeEdge()` to log complete `elementDefinition` with styles
- Use Cytoscape debugger/inspector to examine element data and styles at runtime
- Verify `elementDefinition.style` is not being overwritten downstream
- Check if Cytoscape is re-created vs. updated when state changes

**Expected Behavior:** Elements with labels should have `.style.label` property set

**Potential Issues:**
- Cytoscape instance being recreated without new styles
- Styles being overwritten by global stylesheet
- Style property name mismatch (Cytoscape expects different format)

---

### 4. Check for CSS/Styling Conflicts
**Goal:** Rule out CSS hiding the rendered labels

**Tasks:**
- Inspect Cytoscape canvas element styles in browser DevTools
- Check for `display: none`, `opacity: 0`, or `color: transparent` on edge labels
- Review Cytoscape global stylesheet configuration
- Test with forced inline style override

**Expected Behavior:** Labels should be visible with font-size 20px

**Potential Issues:**
- CSS rule hiding labels
- Text color matching background color
- Z-index issue placing labels behind other elements

---

### 5. Test Cytoscape Label Rendering Directly
**Goal:** Verify Cytoscape can render labels at all

**Tasks:**
- Create minimal test with hardcoded label in Cytoscape element
- Add temporary hardcoded label to all edges (bypass conditional logic)
- Verify Cytoscape version supports label rendering

**Expected Behavior:** Hardcoded labels should always render

**Potential Issues:**
- Cytoscape version incompatibility
- Label rendering disabled globally
- Canvas rendering context issue

---

## Potential Root Causes & Fixes

### Root Cause 1: State Update Not Triggering Re-render
**Symptoms:** Toggle button doesn't cause graph to update

**Fix:**
- Ensure `State.copy()` creates new object reference (triggers change detection)
- Verify Cytoscape service subscribes to state changes
- Add explicit `.update()` call to Cytoscape after style changes

**Files to Modify:**
- `visualization/src/app/adapter/cytoscape/internal/cytoscape.service.ts`
- `visualization/src/app/model/State.ts`

---

### Root Cause 2: All Edge Weights Are 1
**Symptoms:** No labels appear because condition `weight > 1` is never met

**Fix:**
- Option A: Change condition to `weight >= 1` to show all weights
- Option B: Improve weight display to show type labels when weight is 1
- Option C: Fix analysis tool to properly aggregate edge weights

**Files to Modify:**
- `visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.ts:68`
- Possibly `analysis/` component for weight calculation

---

### Root Cause 3: Cytoscape Not Receiving Style Updates
**Symptoms:** Styles defined but not applied to rendered elements

**Fix:**
- Call `cy.style().update()` after adding elements with new styles
- Use Cytoscape style property approach instead of inline styles
- Ensure element definitions are passed correctly to `cy.add()`

**Files to Modify:**
- `visualization/src/app/adapter/cytoscape/internal/cytoscape.service.ts`
- Consider moving style logic to Cytoscape stylesheet

---

### Root Cause 4: Inline Styles vs. Stylesheet Conflict
**Symptoms:** Inline element styles being overridden by global stylesheet

**Fix:**
- Move label styles from `elementDefinition.style` to Cytoscape global stylesheet
- Use selectors like `edge[weight > 1]` to conditionally apply styles
- Ensure stylesheet rules have proper specificity

**Files to Modify:**
- `visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.ts`
- Create or modify Cytoscape stylesheet configuration

---

### Root Cause 5: Font Not Loaded or Text Color Issue
**Symptoms:** Labels rendered but invisible

**Fix:**
- Add explicit `color` property to label style
- Verify font is loaded
- Add text outline or background for visibility

**Files to Modify:**
- `visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.ts:70-72`

---

## Implementation Strategy (TDD Approach)

### Phase 1: Diagnostic Tests
1. **Write test:** "Should log showLabels state when toggling edge labels"
2. **Implement:** Add console logging to track state flow
3. **Run:** Execute and analyze logs

### Phase 2: Fix Root Cause
Based on diagnostic findings, implement appropriate fix using:
1. **Red:** Write failing test demonstrating the bug
2. **Green:** Implement minimal fix to make test pass
3. **Refactor:** Clean up code while maintaining passing tests

### Phase 3: Integration Testing
1. **Manual Test:** Run `just frontend` and verify numbers display
2. **Test Toggle:** Verify button toggles labels on/off
3. **Test Data:** Analyze sample project with known edge weights > 1
4. **Visual Regression:** Ensure other graph features still work

### Phase 4: Documentation
1. Document the fix in commit message
2. Update tests to prevent regression
3. Consider adding user-facing documentation about toggle feature

---

## Testing Checklist

- [ ] Unit tests pass for `elementDefinitionConverter.spec.ts`
- [ ] Toggle button click triggers state change (verified via logs)
- [ ] State change triggers Cytoscape update (verified via logs)
- [ ] Edge weights are correctly calculated and > 1 for some edges
- [ ] Label styles are applied to Cytoscape elements (verified via inspector)
- [ ] Labels are visible in the rendered graph (manual verification)
- [ ] Toggle on shows labels, toggle off hides labels
- [ ] No console errors or warnings
- [ ] Other graph features (node selection, layout, etc.) still work
- [ ] Performance is acceptable with labels enabled

---

## Files to Focus On

### Primary Investigation:
1. `visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.ts` - Label logic
2. `visualization/src/app/adapter/cytoscape/internal/cytoscape.service.ts` - Rendering orchestration
3. `visualization/src/app/model/State.ts` - State management
4. `visualization/src/app/app.component.ts` - Toggle handler

### Secondary:
5. `visualization/src/app/adapter/cytoscape/internal/converter/elementDefinitionConverter.spec.ts` - Tests
6. Analysis component - Edge weight calculation (if needed)

---

## Success Criteria

✅ Edge weights (numbers) are visible on graph edges when:
  - `showLabels` is true
  - Edge weight > 1

✅ "Toggle Edge Labels" button correctly shows/hides labels

✅ No console errors or warnings

✅ All existing tests continue to pass

✅ Feature works consistently across different analyzed projects

---

## Next Steps

1. Start with **Investigation Step 1** (Verify Toggle State Persistence)
2. Add diagnostic logging to understand current behavior
3. Identify root cause based on log analysis
4. Implement fix following TDD methodology
5. Verify fix with manual testing
6. Clean up diagnostic code and commit
