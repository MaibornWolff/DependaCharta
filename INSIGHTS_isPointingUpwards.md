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

`isPointingUpwards()` implements this logic. It can be found in `model/Edge.ts` of the `visualization` frontend.

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

1. Verify the correct logic is in place: `sourceLevel >= targetLevel` within common container
2. Rebuild the JAR
3. Regenerate the Java example from the correct source directory
4. Verify the output has correct `isPointingUpwards` values
5. Test in the visualization to ensure it works correctly