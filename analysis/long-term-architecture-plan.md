# Long-Term Architecture Plan: Multi-Paradigm Dependency Resolution

## Current State (Synthetic Types Approach)

We've implemented a **tactical solution** that creates synthetic types from JavaScript imports, allowing the existing type-based resolution system to handle JavaScript dependencies. This works well and is architecturally sound as a short-term fix.

**Current workaround:**
```kotlin
// JavascriptAnalyzer creates synthetic types from imports
usedTypes = createSyntheticTypesFromImports(imports)
```

This treats import identifiers as "pseudo-types" that flow through the existing `Node.resolveTypes()` logic.

## The Fundamental Problem

The current architecture has an **implicit assumption** that doesn't match reality:

### Assumption (Built Into Node.resolveTypes)
```
All languages track dependencies through type declarations
```

### Reality
Languages have fundamentally different paradigms for tracking dependencies:

1. **Always Typed**: Java, Kotlin, C#, C++, Go
   - Strong static type systems
   - Dependencies inferred from type usage
   - Type annotations are mandatory

2. **Optionally Typed**: TypeScript, PHP (7.0+), Python (with type hints)
   - Type annotations optional
   - Mix of type-based and import-based dependencies
   - Dynamic typing coexists with static typing

3. **Never Typed**: JavaScript, plain PHP (pre-7.0), plain Python
   - No type system
   - Dependencies tracked purely through imports/requires
   - Runtime-only type checking

## Proposed Long-Term Solution: Strategy Pattern

Implement a **three-tier dependency resolution system** that explicitly acknowledges these different paradigms.

### Architecture Overview

```kotlin
// Core abstraction
interface DependencyResolutionStrategy {
    /**
     * Resolves dependencies for a node based on the language's paradigm
     */
    fun resolve(
        node: Node,
        projectDictionary: Map<String, List<Path>>,
        languageDictionary: Map<String, Path>,
        internalNodePaths: Set<String>
    ): NodeDependencies

    /**
     * Returns a descriptive name for logging/debugging
     */
    fun name(): String
}
```

### Strategy 1: Type-Based Resolution

**For:** Java, Kotlin, C#, C++, Go

**Philosophy:** Dependencies are determined by type usage in code

```kotlin
class TypeBasedResolution : DependencyResolutionStrategy {
    override fun resolve(
        node: Node,
        projectDictionary: Map<String, List<Path>>,
        languageDictionary: Map<String, Path>,
        internalNodePaths: Set<String>
    ): NodeDependencies {
        // Current Node.resolveTypes() logic
        val resolvedUsedTypes = node.usedTypes.map {
            it.toResolvedType(projectDictionary, languageDictionary)
        }.toSet()

        val resolvedDependencies = resolvedUsedTypes
            .flatMap { type -> type.containedTypes() }
            .filter { it.resolvedPath != null }
            .filter { !languageDictionary.containsValue(it.resolvedPath) }
            .filter { it.resolvedPath != node.pathWithName }
            .map { Dependency(path = it.resolvedPath!!, type = it.usageSource) }
            .toSet()

        return NodeDependencies(
            internalDependencies = resolvedDependencies
                .filter { internalNodePaths.contains(it.withDots()) }
                .toSet(),
            externalDependencies = resolvedDependencies
                .filter { !internalNodePaths.contains(it.withDots()) }
                .toSet()
        )
    }

    override fun name() = "TypeBased"
}
```

**Characteristics:**
- ✅ Precise - only shows actively used types
- ✅ Filters out unused imports automatically
- ⚠️ Requires complete type information
- ❌ Cannot handle untyped code

### Strategy 2: Import-Based Resolution

**For:** JavaScript, plain PHP, plain Python

**Philosophy:** Dependencies are determined by import/require statements

```kotlin
class ImportBasedResolution : DependencyResolutionStrategy {
    override fun resolve(
        node: Node,
        projectDictionary: Map<String, List<Path>>,
        languageDictionary: Map<String, Path>,
        internalNodePaths: Set<String>
    ): NodeDependencies {
        // Use raw dependencies from import statements
        // No type resolution needed

        val resolvedImports = node.dependencies.mapNotNull { dependency ->
            // Resolve relative paths and aliases through project dictionary
            resolveImportPath(dependency, projectDictionary, node.pathWithName)
        }.toSet()

        return NodeDependencies(
            internalDependencies = resolvedImports
                .filter { internalNodePaths.contains(it.withDots()) }
                .toSet(),
            externalDependencies = resolvedImports
                .filter { !internalNodePaths.contains(it.withDots()) }
                .toSet()
        )
    }

    private fun resolveImportPath(
        dependency: Dependency,
        projectDictionary: Map<String, List<Path>>,
        currentPath: Path
    ): Dependency? {
        // Handle wildcard imports
        if (dependency.isWildcard) {
            return dependency
        }

        // Look up identifier in project dictionary
        val identifier = dependency.path.parts.lastOrNull() ?: return null
        val matches = projectDictionary[identifier] ?: return dependency

        // Prefer matches in same package
        val samePackage = matches.find {
            it.withoutName() == currentPath.withoutName()
        }

        return if (samePackage != null) {
            Dependency(samePackage)
        } else {
            dependency
        }
    }

    override fun name() = "ImportBased"
}
```

**Characteristics:**
- ✅ Works without type information
- ✅ Simple and predictable
- ✅ Captures side-effect imports
- ⚠️ Shows all imports (including unused)
- ⚠️ Less precise than type-based

### Strategy 3: Hybrid Resolution

**For:** TypeScript, PHP (7.0+), Python (with hints)

**Philosophy:** Combine type-based precision with import-based completeness

```kotlin
class HybridResolution(
    private val typeBased: TypeBasedResolution,
    private val importBased: ImportBasedResolution
) : DependencyResolutionStrategy {
    override fun resolve(
        node: Node,
        projectDictionary: Map<String, List<Path>>,
        languageDictionary: Map<String, Path>,
        internalNodePaths: Set<String>
    ): NodeDependencies {
        // Get dependencies from both strategies
        val fromTypes = typeBased.resolve(
            node, projectDictionary, languageDictionary, internalNodePaths
        )

        val fromImports = importBased.resolve(
            node, projectDictionary, languageDictionary, internalNodePaths
        )

        // Merge with priority to type-based (more precise)
        return NodeDependencies(
            internalDependencies = (fromTypes.internalDependencies +
                                   fromImports.internalDependencies).toSet(),
            externalDependencies = (fromTypes.externalDependencies +
                                   fromImports.externalDependencies).toSet()
        )
    }

    override fun name() = "Hybrid"
}
```

**Characteristics:**
- ✅ Best of both worlds
- ✅ Handles typed and untyped code
- ✅ Graceful degradation (if no types, falls back to imports)
- ⚠️ More complex
- ⚠️ May show duplicates in some cases

## Integration Points

### 1. Language Configuration

```kotlin
enum class SupportedLanguage(
    val suffixes: List<String>,
    val resolutionStrategy: DependencyResolutionStrategy
) {
    // Always typed
    JAVA(listOf("java"), TypeBasedResolution()),
    KOTLIN(listOf("kt", "kts"), TypeBasedResolution()),
    C_SHARP(listOf("cs"), TypeBasedResolution()),
    CPP(listOf("cpp", "c", "cc", "cxx", "h", "hpp", "hxx", "hh"), TypeBasedResolution()),
    GO(listOf("go"), TypeBasedResolution()),

    // Never typed
    JAVASCRIPT(listOf("js", "jsx"), ImportBasedResolution()),

    // Optionally typed (hybrid)
    TYPESCRIPT(listOf("ts", "tsx"), HybridResolution(TypeBasedResolution(), ImportBasedResolution())),
    PHP(listOf("php"), HybridResolution(TypeBasedResolution(), ImportBasedResolution())),
    PYTHON(listOf("py"), HybridResolution(TypeBasedResolution(), ImportBasedResolution())),
}
```

### 2. DependencyResolverService

```kotlin
class DependencyResolverService {
    companion object {
        fun resolveNodes(fileReports: Collection<FileReport>): Collection<Node> {
            val nodes = fileReports.flatMap { it.nodes }
            val dictionary = getDictionary(nodes)
            val knownNodePaths = getKnownNodePaths(nodes)

            return nodes.map { node ->
                // Use language-specific strategy
                val strategy = node.language.resolutionStrategy
                val resolvedDeps = strategy.resolve(
                    node,
                    dictionary,
                    standardLibraryFor(node.language).get(),
                    knownNodePaths
                )

                node.copy(resolvedNodeDependencies = resolvedDeps)
            }
        }

        // ... rest of existing code
    }
}
```

### 3. Node Model Changes

```kotlin
@Serializable
data class Node(
    val pathWithName: Path,
    val physicalPath: String,
    val nodeType: NodeType,
    val language: SupportedLanguage,
    val dependencies: Set<Dependency>,  // Raw imports (kept)
    val usedTypes: Set<Type>,           // Type usage (kept)
    val resolvedNodeDependencies: NodeDependencies = NodeDependencies(setOf(), setOf()),
) {
    // Remove resolveTypes() method - replaced by strategies

    fun toLeafInformationDto(...): LeafInformationDto {
        // Unchanged - still uses resolvedNodeDependencies
    }

    fun name() = pathWithName.parts.last()
}
```

## Migration Strategy

### Phase 1: Preparation (1-2 days)
1. Create `DependencyResolutionStrategy` interface
2. Extract current `Node.resolveTypes()` logic into `TypeBasedResolution`
3. Add comprehensive tests for `TypeBasedResolution`
4. Verify no behavioral changes for typed languages

### Phase 2: Implement Strategies (2-3 days)
1. Implement `ImportBasedResolution` for JavaScript
2. Remove synthetic types from JavascriptAnalyzer
3. Test JavaScript resolution thoroughly
4. Implement `HybridResolution` for TypeScript/PHP
5. Test hybrid languages

### Phase 3: Integration (1-2 days)
1. Update `SupportedLanguage` enum with strategies
2. Modify `DependencyResolverService` to use strategies
3. Remove `Node.resolveTypes()` method
4. Update all tests

### Phase 4: Validation (1 day)
1. Run full test suite
2. Verify all languages work correctly
3. Test with real-world codebases
4. Performance testing

**Total Estimated Effort: 5-8 days**

## Benefits of This Refactoring

### 1. Conceptual Clarity
- ✅ Explicitly acknowledges different language paradigms
- ✅ Self-documenting code (strategy names explain approach)
- ✅ Easier onboarding for new contributors

### 2. Maintainability
- ✅ Each strategy is independently testable
- ✅ Clear separation of concerns
- ✅ Easy to add new languages (just choose strategy)
- ✅ Bug fixes isolated to specific strategies

### 3. Flexibility
- ✅ Can mix strategies (hybrid approach)
- ✅ Can add new strategies (e.g., AST-based, ML-based)
- ✅ Configuration per language explicit
- ✅ Easy to experiment with different approaches

### 4. Correctness
- ✅ No more "hacks" or workarounds
- ✅ Each language uses appropriate resolution method
- ✅ Better handling of edge cases
- ✅ More predictable behavior

## Risks and Mitigations

### Risk 1: Breaking Changes
**Mitigation:** Comprehensive test suite must pass before and after refactoring

### Risk 2: Performance Impact
**Mitigation:** Strategy pattern adds minimal overhead; can optimize specific strategies

### Risk 3: Increased Complexity
**Mitigation:** Clear documentation and well-named strategies make intent obvious

### Risk 4: Migration Effort
**Mitigation:** Phased approach allows incremental testing and validation

## Comparison with Current Synthetic Types

| Aspect | Synthetic Types (Current) | Strategy Pattern (Proposed) |
|--------|--------------------------|----------------------------|
| **Implementation effort** | ✅ Done (1-2 hours) | ⚠️ 5-8 days |
| **Conceptual clarity** | ⚠️ Workaround | ✅ Explicit design |
| **Maintainability** | ⚠️ Hidden assumptions | ✅ Clear contracts |
| **Testability** | ⚠️ Coupled to Node.kt | ✅ Isolated strategies |
| **Future languages** | ⚠️ Need adapters | ✅ Choose strategy |
| **Documentation value** | ⚠️ Hidden in code | ✅ Self-documenting |
| **Performance** | ✅ Minimal overhead | ✅ Minimal overhead |
| **Risk** | ✅ Low | ⚠️ Medium |

## Recommendation

### Short Term (Now)
Keep the synthetic types approach. It's:
- ✅ Working correctly
- ✅ Low risk
- ✅ Well tested
- ✅ Ships JavaScript support immediately

### Long Term (Future Sprint)
Plan the strategy pattern refactoring as **technical debt** to be addressed when:
- You're adding support for another untyped language (Ruby, Lua, etc.)
- You need to optimize resolution for specific languages
- You have 1-2 weeks for a refactoring sprint
- The team can handle the migration effort

## Documentation Debt

Until the refactoring happens, document the current workaround:

```kotlin
/**
 * Creates synthetic Type objects from import dependencies.
 *
 * ARCHITECTURAL NOTE: This is a workaround to make JavaScript imports
 * compatible with the type-based dependency resolution system.
 *
 * Long-term, we should implement a proper multi-paradigm resolution
 * system (see long-term-architecture-plan.md).
 *
 * @param imports The raw import dependencies from the JavaScript analyzer
 * @return A set of synthetic types representing the imported identifiers
 */
private fun createSyntheticTypesFromImports(imports: List<Dependency>): Set<Type> {
    // ...
}
```

## Conclusion

The synthetic types approach is a **good pragmatic solution** for now. The strategy pattern refactoring is the **correct long-term architecture**, but it's not urgent. Schedule it as technical debt for a future sprint when you have the bandwidth.

The important thing is: **JavaScript works now**, and we have a clear path forward for when we want to do it "right."
