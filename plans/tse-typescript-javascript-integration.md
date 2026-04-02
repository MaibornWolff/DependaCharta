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
- [ ] Replace `TypescriptAnalyzer` body with `BaseLanguageAnalyzer` extension
- [ ] Run `TypescriptAnalyzerTest`, fix failures iteratively
- [ ] Delete TypeScript legacy query/model/tsconfig files
- [ ] Run existing `JavascriptAnalyzerTest` (baseline)
- [ ] Replace `JavascriptAnalyzer` body with `BaseLanguageAnalyzer` extension
- [ ] Run `JavascriptAnalyzerTest`, fix failures iteratively
- [ ] Delete JavaScript legacy query files and data classes
- [ ] Run full `mise run test-analysis` (all green)
- [ ] Run `/dc-compare` on a real TS project, iterate until output matches
- [ ] Run `/dc-compare` on a real JS project, iterate until output matches
- [ ] Remove composite build config once TSE version is released; ask user to commit

## Session notes (2026-04-02)

- Composite build set up and working; TSE branch `feat/typescript-javascript-dependency-support` is checked out manually by the user (had local uncommitted changes).
- Fixed `TseMappings.kt`: new TSE version added `DeclarationType.FUNCTION` and `DeclarationType.VARIABLE` — added mappings to `NodeType.FUNCTION` / `NodeType.VARIABLE`.
- `TypescriptAnalyzerTest` baseline: all green.
- Next session starts at: **Replace `TypescriptAnalyzer` body with `BaseLanguageAnalyzer` extension**

## Notes

- **Reference implementations**: `JavaAnalyzer.kt` (11 lines), `KotlinAnalyzer.kt` (overrides `buildPathWithName`)
- **BaseLanguageAnalyzer**: `analysis/src/main/kotlin/.../analyzers/BaseLanguageAnalyzer.kt` — handles the full TSE call and mapping; only override `buildPathWithName` if path structure differs
- **Key files to modify**: `analysis/settings.gradle.kts`, `typescript/TypescriptAnalyzer.kt`, `javascript/JavascriptAnalyzer.kt`
- **Key files to delete**: `typescript/queries/*`, `typescript/model/*`, `typescript/tsconfig/*`, `TypescriptExportNameExtractor.kt`, `javascript/queries/*`
- **Test files** (should pass unchanged after migration): `analyzers/TypescriptAnalyzerTest.kt` (1382 lines), `analyzers/JavascriptAnalyzerTest.kt` (611 lines)
- TSX files use `.tsx` extension; TSE's `TYPESCRIPT` language should handle both `.ts` and `.tsx` — verify this
- The `dependency-migration.md` file does not yet exist; create it in `.claude/rules/` after the migration to document lessons learned