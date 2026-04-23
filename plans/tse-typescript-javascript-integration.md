---
name: tse-typescript-javascript-integration
issue:
state: progress
version:
---

## Goal

Migrate `TypescriptAnalyzer` and `JavascriptAnalyzer` from custom TSQuery-based extraction to `TreeSitterDependencies.analyze()` from the TSE library, matching the pattern already used by `JavaAnalyzer` and `KotlinAnalyzer`. TSE is on feature branch `feat/typescript-javascript-dependency-support` (not yet released), so a Gradle composite build is used while iterating.

## Tasks

### 1. Create branch and set up composite build
- Branch: `feat/tse-typescript-javascript-integration`
- In `analysis/settings.gradle.kts`, add `includeBuild` pointing to the local TSE checkout at `C:\Development\CodeChartaEtc\TreeSitterExcavationSite` with explicit dependency substitution (JitPack artifact ID won't auto-match):
  ```kotlin
  includeBuild("../../TreeSitterExcavationSite") {
      dependencySubstitution {
          substitute(module("com.github.MaibornWolff:TreeSitterExcavationSite")).using(project(":"))
      }
  }
  ```
- Check out `feat/typescript-javascript-dependency-support` on the TSE repo

### 2. Migrate TypescriptAnalyzer (TDD: test first)
- The current `TypescriptAnalyzer` implements `LanguageAnalyzer` with ~250 lines of TSQuery logic.
- Target: extend `BaseLanguageAnalyzer`, set `language = SupportedLanguage.TYPESCRIPT`.
  - `Language.valueOf("TYPESCRIPT")` will resolve to TSE's `Language.TYPESCRIPT`
  - `SupportedLanguage.TYPESCRIPT` already exists
- **TDD**: Run existing `TypescriptAnalyzerTest` first (it will pass currently). After replacing the analyzer body, re-run — failures indicate divergence to fix in TSE or update tests for.
- Replace with minimal form:
  ```kotlin
  class TypescriptAnalyzer(fileInfo: FileInfo) : BaseLanguageAnalyzer(fileInfo) {
      override val language = SupportedLanguage.TYPESCRIPT
  }
  ```
- Iterate: run tests, fix TSE or DC mappings until all pass

### 3. Delete TypeScript legacy files
After tests pass:
- `typescript/queries/` — all `*Query.kt` files
- `typescript/model/Declaration.kt`, `DependenciesAndAliases.kt`, `IdentifierWithAlias.kt`
- `typescript/tsconfig/` — all tsconfig support files (if TSE handles path aliases)
- `TypescriptExportNameExtractor.kt`
- `DEFAULT_EXPORT_NODE_NAME` constant

### 4. Migrate JavascriptAnalyzer (TDD: test first)
- Current analyzer handles ES6 + CommonJS; about 120 lines.
- Same pattern: extend `BaseLanguageAnalyzer`, set `language = SupportedLanguage.JAVASCRIPT`.
- **TDD**: Run `JavascriptAnalyzerTest` before and after to track regressions.
- Iterate until tests pass.

### 5. Delete JavaScript legacy files
After tests pass:
- `javascript/queries/` — all `*Query.kt` files
- `JavascriptDeclaration` and `JavascriptReexport` data classes (in `JavascriptAnalyzer.kt`)
- `DEFAULT_EXPORT_NAME` constant

### 5a. Migrate VueAnalyzer to TSE (prerequisite for legacy deletion)
VueAnalyzer depends directly on legacy TS/JS query classes that will be deleted. Must be migrated first.

**Design**: VueAnalyzer calls `TreeSitterDependencies.analyze()` directly for script blocks (TS/JS), same as BaseLanguageAnalyzer does. Template component usage logic stays unchanged.

**Refactoring needed**: Extract `resolveImportPath` from `BaseLanguageAnalyzer` to a package-level function so VueAnalyzer (which doesn't extend BaseLanguageAnalyzer) can share the logic.

**Expected test failures (TSE-limited, not migration bugs)**:
- `tracks named imports from both module alias and relative paths` — needs TSE named import granularity (#1)
- `handles Vue SFC with TSX script` / `handles Vue SFC with JSX script` — needs TSE TSX/JSX support

**Steps**:
- Extract `resolveImportPath` to package-level function; update `BaseLanguageAnalyzer` to call it
- Replace legacy query calls in VueAnalyzer with direct TSE call + import mapping
- Remove `DependenciesAndAliases` usage from VueAnalyzer
- Run `VueAnalyzerTest` — confirm structure tests pass, note TSE-blocked failures

### 6. Verify with /dc-compare
- Run `/dc-compare` against a real TypeScript project; iterate until output matches `main`
- Run `/dc-compare` against a real JavaScript project; iterate until output matches `main`
- Common divergences to watch for: re-export nodes, default exports, wildcard exports, path alias resolution

### 7. Remove composite build / bump TSE version
- Once TSE releases the new version, revert `settings.gradle.kts` and update TSE version in `build.gradle.kts`

## Steps

- [x] Create branch `feat/tse-typescript-javascript-integration`
- [x] Set up composite build in `analysis/settings.gradle.kts` pointing to local TSE at `C:\Development\CodeChartaEtc\TreeSitterExcavationSite`
- [ ] Check out `feat/typescript-javascript-dependency-support` on the TSE repo (handled manually)
- [x] Verify composite build works: `./gradlew test` picks up local TSE
- [x] Run existing `TypescriptAnalyzerTest` (baseline: all green)
- [x] Replace `TypescriptAnalyzer` body with `BaseLanguageAnalyzer` extension
- [ ] Run `TypescriptAnalyzerTest`, fix failures iteratively (20/49 passing; 29 blocked on TSE changes)
- [-] Implement TSX dependency support — see `plans/tsx-dependency-support.md` (in progress)
- [ ] Delete TypeScript legacy query/model/tsconfig files
- [x] Run existing `JavascriptAnalyzerTest` (baseline: all green)
- [x] Replace `JavascriptAnalyzer` body with `BaseLanguageAnalyzer` extension
- [ ] Run `JavascriptAnalyzerTest`, fix failures iteratively (0/22 passing; blocked on TSE adding `DeclarationExtractor` to `JavascriptDependencyMapping`)
- [ ] Delete JavaScript legacy query files and data classes
- [x] Extract `resolveImportPath` to package-level function; update `BaseLanguageAnalyzer`
- [x] Migrate VueAnalyzer to TSE (replace legacy query calls)
- [x] Run `VueAnalyzerTest` — 8/10 green; 2 blocked on TSE (TSX + named import granularity)
- [ ] Run full `mise run test-analysis` (all green)
- [ ] Run `/dc-compare` on a real TS project, iterate until output matches
- [ ] Run `/dc-compare` on a real JS project, iterate until output matches
- [ ] Remove composite build config once TSE version is released; ask user to commit

## Session notes (2026-04-02)

- Composite build set up and working; TSE branch `feat/typescript-javascript-dependency-support` is checked out manually by the user (had local uncommitted changes).
- Fixed `TseMappings.kt`: new TSE version added `DeclarationType.FUNCTION` and `DeclarationType.VARIABLE` — added mappings to `NodeType.FUNCTION` / `NodeType.VARIABLE`.
- `TypescriptAnalyzerTest` baseline: all green.
- Next session starts at: **Replace `TypescriptAnalyzer` body with `BaseLanguageAnalyzer` extension**

## Session notes (2026-04-20)

- `TypescriptAnalyzer` replaced with `BaseLanguageAnalyzer` extension; ktlint clean.
- Added `convertImport` hook to `BaseLanguageAnalyzer` (protected open, used in `analyze()`).
- `TypescriptAnalyzer` overrides:
  - `buildPathWithName`: uses file path (not TSE `packagePath`) as node prefix; handles `.tsx`
  - `convertImport`: resolves relative paths (`./`, `../`) and strips file extensions
- `DEFAULT_EXPORT_NODE_NAME` kept as a package-level const in `TypescriptAnalyzer.kt` (still used by `TypescriptImportStatementQuery` for VueAnalyzer + by tests).
- 20/49 `TypescriptAnalyzerTest` tests passing. 29 blocked on TSE changes.
- **TSE changes needed** (see prompt below):
  1. Named import granularity: `import { A, B }` → one `ImportDeclaration` per name
  2. Default import: `import X from './foo'` → `[".", "foo", "DEFAULT_EXPORT"]`
  3. Named re-export granularity: `export { A, B }` → one `ImportDeclaration` per name
  4. Named re-export alias: `export { A as B }` → use original name `A` in import path
  5. CommonJS named destructuring: `const { A, B } = require(...)` → one per name
  6. CommonJS default: `const X = require(...)` → `[".", "foo", "DEFAULT_EXPORT"]`
  7. Fix: Add `variable_declaration` to handled declaration types (for `var`)
  8. Fix: Skip nested declarations (only top-level declarations should be extracted)
- Next session: after TSE implements changes, re-run `TypescriptAnalyzerTest` and fix remaining failures.

## Notes

- **Reference implementations**: `JavaAnalyzer.kt` (11 lines), `KotlinAnalyzer.kt` (overrides `buildPathWithName`)
- **BaseLanguageAnalyzer**: `analysis/src/main/kotlin/.../analyzers/BaseLanguageAnalyzer.kt` — handles the full TSE call and mapping; only override `buildPathWithName` if path structure differs
- **Key files to modify**: `analysis/settings.gradle.kts`, `typescript/TypescriptAnalyzer.kt`, `javascript/JavascriptAnalyzer.kt`
- **Key files to delete**: `typescript/queries/*`, `typescript/model/*`, `typescript/tsconfig/*`, `TypescriptExportNameExtractor.kt`, `javascript/queries/*`
- **Test files** (should pass unchanged after migration): `analyzers/TypescriptAnalyzerTest.kt` (1382 lines), `analyzers/JavascriptAnalyzerTest.kt` (611 lines)
- TSX support (detecting `<Routes />` as a usedType) is tracked separately in `plans/tsx-dependency-support.md` and must be completed as part of this migration — it is a regression, not a future feature.
- The `dependency-migration.md` file does not yet exist; create it in `.claude/rules/` after the migration to document lessons learned