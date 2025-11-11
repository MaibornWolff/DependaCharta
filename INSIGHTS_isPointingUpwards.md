# Insights: Adding isPointingUpwards to DependaCharta

## Task Overview
Add an `isPointingUpwards` boolean field to the `.cg.json` files produced by the analysis backend, so the visualization frontend doesn't need to calculate it at runtime by analyzing the entire graph structure.

## Key Concepts

### Levelization in DependaCharta
- Nodes are assigned **levels** based on their position **within containers** (packages/namespaces)
- **Level 0** = foundation nodes with no dependencies
- **Level 1** = nodes that depend on level 0 nodes
- **Level 2** = nodes that depend on level 1 nodes, etc.

### What "Pointing Upwards" Means
An edge "points upwards" when it violates the normal dependency flow:
- **Normal flow**: Higher level → Lower level (e.g., Leaf of Level 1 → Leaf of Level 0; Leaf in Container of Level 1 → Leaf in Container of Level 0)
- **Upward flow**: Lower level → Higher level (e.g., Leaf in Container of Level 0 → Leaf in Container of Level 1)

`isPointingUpwards()` implements this logic. It can be found in [`model/Edge.ts`](visualization/src/app/model/Edge.ts:25-28) of the `visualization` frontend:

```typescript
isPointingUpwards(): boolean {
  const [sourceNode, targetNode] = findSiblingsUnderLowestCommonAncestor(this.source, this.target)
  return sourceNode.level <= targetNode.level
}
```

The key insight: It finds siblings under the **lowest common ancestor** and compares their levels. This ensures the comparison happens within the same container context.

### Example from the Codebase
Looking at the Java example with the screenshot:
- `domain.model.ArmorClass` → `application.CreatureUtil`
- `domain.model.Creature` → `application.CreatureFacade`

Both dependencies point upwards, as (a leaf in a container in a container of) level 1 points to (a leaf in a container of) level 0. These are architectural violations where `domain` (level 0) depends on `application` layer (level 1).

## Implementation Details

### Backend Files Modified

1. **`analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/model/ProjectReportDto.kt`**
   - Added `isPointingUpwards: Boolean = false` to `EdgeInfoDto` data class
   - This is the DTO that gets serialized to JSON

2. **`analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/reporting/ExportService.kt`**
   - Added `encodeDefaults = true` to JSON configuration to ensure boolean fields are serialized

3. **`analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/ProcessingPipeline.kt`** (lines 119-129)
   - Fixed filename duplication bug where `.cg.json` was always appended
   - Now checks if filename already ends with `.cg.json` before appending

## What Still Needs to Be Done

The backend needs to implement the same logic as the frontend's `isPointingUpwards()` function:

1. **Find the lowest common ancestor** of source and target nodes
2. **Get the siblings** under that common ancestor (one containing source, one containing target)
3. **Compare their levels**: `sourceLevel <= targetLevel` means pointing upwards

### Files That Need Implementation:

1. **`analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/analysis/model/Node.kt`** (around line 36-37)
   - Currently has: `val isPointingUpwards = sourceLevel >= targetLevel`
   - Needs: Logic to find common ancestor and compare sibling levels

2. **`analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/levelization/model/GraphNode.kt`** (lines 71 and 81)
   - Currently has: `isPointingUpwards = sourceLevel >= targetLevel`
   - Needs: Same logic as above, but for aggregated parent nodes

### The Challenge:

The backend works with a flat structure of nodes and edges, while the frontend has a tree structure with parent-child relationships. The backend needs to:
- Build or traverse the tree structure to find common ancestors
- Determine which container each node belongs to at each level
- Compare levels within the same container context

## Common Pitfalls

1. **Filename Duplication**:
   - The code was appending `.cg.json` even when filename already had it
   - Fixed by checking: `if (fileName.endsWith(CG_JSON_FILE_TYPE)) fileName else fileName + CG_JSON_FILE_TYPE`

## Testing Strategy

1. **Verify field presence**: Check that `isPointingUpwards` appears in generated JSON
2. **Verify correct values**: 
   - Find edges that should point upward (e.g., `domain` → `application`)
   - Verify they have `isPointingUpwards: true`
3. **Test aggregation**: Check parent nodes have correct values based on their level, not children's
4. **Test visualization**: Ensure frontend displays upward-pointing edges correctly

## File Locations

### Java Example Source
- **Location**: `analysis/src/test/resources/analysis/contract/examples/java/src/main/java/de/sots/cellarsandcentaurs/`
- **NOT**: `exampleProjects/Test` (that's C# code)

### Testing Commands

#### Run all isPointingUpwards tests
```bash
cd analysis
./gradlew test --tests IsPointingUpwardsTest
```

#### Run a specific test
```bash
cd analysis
./gradlew test --tests "IsPointingUpwardsTest.should return false when source level is higher than target level - normal dependency flow"
```

#### Run tests with detailed output
```bash
cd analysis
./gradlew test --tests IsPointingUpwardsTest --info
```

#### View test report
```bash
# After running tests, open:
analysis/build/reports/tests/test/index.html
```

### Generation Command
```bash
cd analysis/bin
./dependacharta.sh --directory ../src/test/resources/analysis/contract/examples/java \
                   --outputDirectory ../../visualization/public/resources \
                   --filename java-example.cg.json
```

### Build Command
```bash
cd analysis
./gradlew clean build -x test
cp build/libs/dependacharta.jar bin/dependacharta.jar
```

## Next Steps After Reset

1. **Understand the tree structure**: Study how nodes are organized in containers (packages/namespaces)
2. **Implement common ancestor logic**: Find the lowest common ancestor for source and target
3. **Get sibling containers**: Find which containers (under the common ancestor) contain source and target
4. **Compare sibling levels**: Use `sourceLevel <= targetLevel` for those siblings
5. **Handle aggregation**: Ensure parent nodes recalculate based on their own context
6. **Rebuild and test**: Build JAR, regenerate examples, verify output

## Questions to Consider

1. **How is the tree structure represented in the backend?**
   - Look at `ProjectNodeDto` and how it stores parent-child relationships
   - Check if there's a way to traverse from a leaf to its ancestors

2. **Where is the level information stored?**
   - Levels are calculated in the levelization phase
   - They're stored per node, but are they relative to the whole graph or to the container?

3. **How to find the common ancestor?**
   - Can we traverse from both nodes upward until we find a common parent?
   - Is there a data structure that makes this efficient?

4. **What about edges between different top-level containers?**
   - If source and target have no common ancestor (different root packages), what should `isPointingUpwards` be?
   - The frontend throws an error in this case - should the backend do the same?