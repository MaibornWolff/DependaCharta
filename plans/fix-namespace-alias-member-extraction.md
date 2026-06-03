---
name: fix-namespace-alias-member-extraction
issue:
state: complete
version:
---

## Goal

`import * as ns from './module'` followed by `new ns.Logger()` or `ns.Logger.method()` produces no
dependency edge in the graph. The namespace alias fix in TSE (bindingName + aliasMap) is a
prerequisite, but the actual type extraction is still broken: `extractMemberAccesses` filters out
lowercase leftmost identifiers unconditionally, without checking whether they are namespace aliases.

## Root Cause Analysis

TSE's `UsedTypeExtractor.extractMemberAccesses` contains:

```kotlin
val root = findLeftmostIdentifier(node) ?: return@mapNotNull null
val name = TreeTraversal.getNodeText(root, sourceCode).trim()
if (name.firstOrNull()?.isUpperCase() != true) return@mapNotNull null   // ← drops "types"
UsedType(name = name)
```

For `types.Logger` the leftmost identifier is `types` (lowercase) → filtered out.
`Logger` is never emitted as a `UsedType`.

The same gap exists in `extractConstructorCalls` for `new types.Logger()`:
the constructor child is a `member_expression`, not a bare `IDENTIFIER`/`TYPE_IDENTIFIER`, so
the method returns null immediately.

DC's resolver already handles the resulting `UsedType("Logger")` correctly via the wildcard
import — no DC-side resolver change is needed. Only TSE needs fixing.

## What Already Works (after TSE commit ttwmxqpu)

- `bindingName` is set on wildcard imports: `ImportDeclaration(bindingName="types", isWildcard=true)`
- `types` is identity-mapped in the aliasMap
- `extractRelevantIdentifiers` emits `UsedType("types")` for identifier-only usages
- Type annotations like `private animal: types.Animal` work in both before/after (via `type_identifier` extraction)

## What Does NOT Work

- `new types.Logger()` — constructor through member expression, `Logger` never extracted
- `types.Logger.staticMethod()` — same gap
- In both cases DC shows the same edges before and after the fix

## Tasks

### 1. TSE: Fix `extractMemberAccesses` for namespace alias member expressions

In `UsedTypeExtractor.kt`, when the leftmost identifier is lowercase but IS in the aliasMap,
extract the next-level property identifier as the UsedType:

```kotlin
if (name.firstOrNull()?.isUpperCase() != true) {
    if (name !in aliasMap) return@mapNotNull null
    // namespace alias: types.Logger → emit Logger
    val prop = node.children()
        .firstOrNull { it.type == PROPERTY_IDENTIFIER }
        ?.let { TreeTraversal.getNodeText(it, sourceCode).trim() }
    if (prop?.firstOrNull()?.isUpperCase() != true) return@mapNotNull null
    return@mapNotNull UsedType(name = prop)
}
```

`extractMemberAccesses` must receive `aliasMap` as a parameter (pass it through from `extract()`).

Also fix `extractConstructorCalls`: when the constructor child is a `member_expression`
(not a bare identifier), apply the same aliasMap-aware logic.

Tests: add to `TypescriptDependencyTest` / `JavascriptDependencyTest` in TSE.

### 2. TSE: Update `ns-consumer.ts` demo file

Remove type annotations from `Logger` usage so the demo actually requires the fix to produce
the edge. Currently `Animal` has a type annotation so the dep exists even without the fix.

A clean demo:
```typescript
log(): void {
    new types.Logger().log(this.animal?.name ?? '')  // no intermediate `const logger: types.Logger`
}
```

Before fix: `NsConsumer` has no `src.types.Logger` dep (Logger only in constructor, no annotation).
After fix: `Logger` is emitted as UsedType → DC resolves via wildcard → edge appears.

### 3. DC: Regenerate demo JSONs

After TSE fix, against a local TS project exercising a namespace alias (`import * as ns from '...'` + `new ns.Class()`):
- `jj edit <pre-fix-commit>` in TSE → `publishToMavenLocal` → `clean fatJar` → run DC → save `analysis-before-fix.cg.json`
- `jj edit <post-fix-commit>` in TSE → `publishToMavenLocal` → `clean fatJar` → run DC → save `analysis-after-fix.cg.json`
- Verify: before has no `src.types.Logger` dep on NsConsumer; after has it

### 4. DC: Add TypescriptAnalyzerTest case for namespace alias

Add a test fixture demonstrating `import * as ns from '...'` + `new ns.Class()` and verify
that the resulting node has the correct dependency edge.

## Steps

- [x] Task 1: Fix `extractMemberAccesses` + `extractConstructorCalls` in TSE with failing tests first
- [x] Task 2: Update the local `ns-consumer.ts` demo
- [x] Task 3: Regenerate `analysis-before-fix.cg.json` and `analysis-after-fix.cg.json`
- [x] Task 4: Add TypescriptAnalyzerTest case for namespace alias

## Notes

- TSE version bump needed after the fix (0.9.0 → 0.10.0 or patch version)
- DC build.gradle.kts currently references 0.10.0-local; local composite build instructions in DC CLAUDE.md
- The existing `UsedType("types")` emission stays — it doesn't hurt, and may be useful in future
- Only `PROPERTY_IDENTIFIER` (first level after the alias) should be extracted; deeper chains like
  `types.sub.Class` are not common in TS but should not crash if encountered (just skip or take first)
- `extractMemberAccesses` currently does not take `aliasMap` — signature change required; propagate
  from the public `extract()` entry point