---
name: import-alias-resolution-regression
issue:
state: progress
version:
---

## Goal

Restore import **alias resolution** (tsconfig/jsconfig path aliases, bundler aliases, Module
Federation remotes) for TypeScript/JavaScript/Vue, which was silently lost during the TSE
migration. Then add an end-to-end test so it cannot regress unnoticed again.

## Findings

### Summary

The TSE migration of the TS/JS/Vue analyzers dropped config-based import **alias resolution**.
Imports like `import x from "@shared/utils"` (tsconfig `paths`), webpack/vite aliases, and Module
Federation remotes are no longer resolved to their real target files — they pass through as
unresolved literal paths. This is a real capability regression that no existing safety net caught.

### How the loss happened

- `ImportPathResolver` was the orchestrator that resolved an import string through a fallback chain:
  tsconfig/jsconfig path aliases → bundler aliases (webpack/vite/vue.config) → Module Federation
  remotes → relative path resolution.
- It was deleted in commit `d8e94b046` ("refactor(analysis): delete legacy TypeScript/JavaScript
  query and model files"), the cleanup step at the end of the TS/JS/Vue → TSE migration.
- The same commit deleted the tsconfig resolvers entirely (`TsConfigResolver`, `PathAliasResolver`
  and their tests).
- The new resolution helper, `resolveImportPath` in
  `analyzers/common/utils/PathUtils.kt`, only handles **relative** (`./`, `../`) imports and strips
  file extensions. It does **not** do any alias resolution.

### TSE does not (and cannot) resolve aliases — verified against v0.9.1

Checked against TSE `v0.9.1` (the version pinned in `analysis/build.gradle.kts`):

- `TreeSitterDependencies.analyze(content: String, language)` takes **only the file's content** —
  no file path, no project root, no access to config files. Resolving `@shared/*` to a real
  directory fundamentally requires reading `tsconfig.json` / `webpack.config.js` / `package.json`
  and knowing the project layout, which TSE never sees.
- TSE's `ImportExtractor` builds the import path as the **literal string split on `/`**
  (`basePath = pathText.split("/")`). So `import x from "@shared/utils"` → `["@shared", "utils"]`,
  never resolved.
- Zero config-alias code anywhere in TSE main source (no `tsconfig` / `jsconfig` / `baseUrl` /
  `webpack` / `federation` references). TSE is a per-file analyzer by design — this will not change.

### Capability status after the migration

| Capability                          | Status                                                              |
|-------------------------------------|---------------------------------------------------------------------|
| tsconfig/jsconfig path aliases      | **Gone entirely** — resolver classes + tests deleted in `d8e94b046` |
| Bundler aliases (webpack/vite/vue)  | Code **still present** but **dead** — no production caller           |
| Module Federation remotes           | Code **still present** but **dead** — no production caller           |
| Relative imports (`./`, `../`)      | Still resolved (by `resolveImportPath`)                             |

The surviving bundler/federation resolvers live under
`analyzers/common/bundler/` and `analyzers/common/federation/`. They are referenced **only by their
own unit tests** — nothing in `src/main` calls them. They were wired in exclusively through the
deleted `ImportPathResolver`.

### Why `/dc-compare` did not detect it

`/dc-compare` is a whole-project output diff: run the analyzer on a real codebase with the current
branch, run it on `main`, compare the resulting `.cg.json`. It only flags regressions in features
the comparison project actually exercises.

1. **The test projects don't use aliases.** Per the migration plan, `/dc-compare` was run only on
   **Prisma (TS)** and **React (JS)**. React's source is almost entirely relative imports (no
   tsconfig `paths`); Module Federation is a micro-frontend pattern absent from both. The
   tsconfig/bundler/federation code paths were never executed → identical output → false green.
2. **The unit-test safety net is illusory.** `BundlerAliasResolverTest` and
   `FederationAliasResolverTest` still pass — but they test the resolver classes in isolation and
   keep passing precisely because the classes weren't deleted. They never asserted the resolvers are
   wired into the pipeline, so severing that wiring didn't turn them red. The tsconfig tests were
   deleted with their classes, removing even isolated coverage.
3. **No end-to-end fixture.** There is no DC test fixture (`tsconfig.json` / `webpack.config.js` +
   alias imports) that runs through the full analyzer pipeline.

### Root cause

The migration plan (`plans/tse-typescript-javascript-integration.md`) made the deletion explicitly
conditional — "delete `tsconfig/` files **if TSE handles path aliases**" — and listed "path alias
resolution" as a divergence to watch for. That `if` was never verified, and `/dc-compare` on two
alias-free projects produced the false green that let the unmet condition slip through. TSE does not,
and structurally cannot, resolve these aliases.

## Reproduction (confirmed 2026-06-03)

The regression is captured by in-repo fixtures driven through the full analyzer in
`ImportAliasResolutionTest` (`analysis/src/test/kotlin/.../analyzers/ImportAliasResolutionTest.kt`):

| Fixture | Purpose |
|---------|---------|
| `src/test/resources/typescript-alias/tsconfig.json` | maps `@shared/*` → `src/shared/*`, `@app/*` → `src/app/*` |
| inline `AliasConsumer` source | imports `@shared/logger` (`SharedLogger`) + `@app/userService` (`UserService`) |
| `src/test/resources/bundler-alias/webpack.config.js` | maps `@utils` → `<root>/src/utils` (`path.resolve(__dirname, …)`) |
| inline `BundlerConsumer` source | imports `@utils/calculator` (`Calculator`) |

Against the **pre-fix** analyzer these aliased imports drop out:

- `AliasConsumer` → `dependencies: {}` (empty)
- `BundlerConsumer` → `dependencies: {}` (empty)
- the alias targets (`SharedLogger`, `UserService`, `Calculator`) get **zero incoming edges**
  (orphaned nodes)
- Control: relative imports (`./`, `../`) resolve correctly → relative imports are unaffected.

The aliased module path is not just left external — it is split on `/` (`["@shared","logger"]`),
fails to match any node id in the dependency-resolution phase, and is dropped silently.

## Fix design (the single chokepoint)

Both `TypescriptAnalyzer.convertImport()` and `JavascriptAnalyzer.convertImport()` call
`resolveImportPath(import.path)` → `PathUtils.resolveImportPath(tsePath, fileInfo)`. That function
currently resolves **only** relative (`.`/`..`) paths and passes bare specifiers through unchanged.
It is the one place to add alias resolution, and fixing it there covers TS + JS + Vue at once.

Proposed change to `PathUtils.resolveImportPath(tsePath, fileInfo)`:
1. Relative path (starts with `.`/`..`) → current behaviour (unchanged).
2. Otherwise (bare specifier, e.g. `@shared/logger`) → attempt the alias chain using
   `fileInfo.physicalPath` + `fileInfo.analysisRoot` (TSE's content-only API can't):
   tsconfig/jsconfig `paths` → bundler aliases → federation remotes.
   - Reconstruct the import string as `tsePath.joinToString("/")`, feed the recovered resolvers,
     convert the returned root-relative `Path` back to `List<String>`.
   - On a hit, return the resolved segments; on a miss, return the stripped path unchanged
     (today's behaviour) so non-alias bare imports (real npm packages) still drop out as external.
3. Cache config discovery (tsconfig/webpack/federation lookups walk the FS upward) per
   `analysisRoot` to avoid a filesystem walk per import.

## Tasks

### 1. Recover and review the deleted resolvers

- Recover `ImportPathResolver` and `TsConfigResolver` / `PathAliasResolver` from `d8e94b046^` to
  scope the rewire exactly.
- Review the surviving `bundler/` and `federation/` resolvers — confirm their public API still fits.

### 2. Re-integrate alias resolution into the TSE pipeline

- Add a resolution step in `BaseLanguageAnalyzer` (in/around `resolveImportPath` / `convertImport`)
  that, before falling back to relative resolution, tries: tsconfig/jsconfig `paths` → bundler
  aliases → federation remotes — the same chain `ImportPathResolver` had.
- The new step needs the file path + analysis root (available via `fileInfo`), unlike TSE's
  content-only API.

### 3. Add an end-to-end regression test

- Add a DC test fixture with a `tsconfig.json` (and/or bundler config) plus alias imports, run
  through the full analyzer, asserting alias imports resolve to the correct internal nodes.
- This guards the capability independently of `/dc-compare`.

### 4. Add an alias-using project to `/dc-compare`

- Document that `/dc-compare` must include at least one project using tsconfig `paths` (and ideally
  Module Federation), so the comparison actually exercises alias resolution.

## Steps

- [x] Add alias-import reproduction fixtures under `analysis/src/test/resources/` (tsconfig `paths` + webpack alias)
- [x] Confirm aliases drop out against the pre-fix analyzer
- [x] Recover tsconfig resolvers (`TsConfigData`/`TsConfigParser`/`TsConfigResolver`/`PathAliasResolver`) + tests from `d8e94b046^` (into `common/tsconfig/`)
- [x] Add `AliasPathResolver` orchestrator (tsconfig→bundler→federation) with synchronized config caches
- [x] Extend `PathUtils.resolveImportPath(tsePath, fileInfo)` to resolve bare specifiers via `AliasPathResolver`
- [x] Add end-to-end fixture tests for tsconfig path-alias + webpack bundler-alias resolution (`ImportAliasResolutionTest`)
- [ ] Run `mise run test-analysis` on host — must be green (cannot compile in-sandbox: TSE/jitpack); `ImportAliasResolutionTest` confirms the new edges
- [ ] Document the `/dc-compare` project-selection requirement
- [ ] Ask user to commit

## Follow-ups (post-implementation)

- **Verified end-to-end** via `ImportAliasResolutionTest`: post-fix, `AliasConsumer` resolves to
  `src.shared.logger.SharedLogger` + `src.app.userService.UserService` and `BundlerConsumer`
  resolves to `src.utils.calculator.Calculator`; pre-fix these edges are absent. `mise run
  test-analysis` green (incl. recovered unit tests + `ImportAliasResolutionTest`).
- **Done (perf)**: config *discovery* (the upward directory walk in
  `TsConfigResolver`/`BundlerConfigResolver`/`FederationConfigResolver`) is now cached per starting
  directory via a `ConcurrentHashMap<String, Optional<File>>` `lookupCache`, so the walk runs once
  per directory instead of once per bare import. Parsing was already cached.
- **Open**: decide whether to promote bundler/federation alias support to first-class (fixtures exist
  for tsconfig + bundler; federation is wired but unfixtured).

## Notes

- Verified against TSE `v0.9.1`; clone used for investigation: `/tmp/tse` (tag `v0.9.1`).
- Deletion commit: `d8e94b046`. Recover deleted files with
  `git show d8e94b046^:<path>`.
- Bundler/federation support was originally added as **experimental** (commit `39e26d99d`), so even
  on `main` these were niche — but the capability existed and now does not.
- Open question for the user: should bundler/federation alias support be promoted to first-class as
  part of this fix, or kept experimental? This affects whether to invest in fixtures for them too.
