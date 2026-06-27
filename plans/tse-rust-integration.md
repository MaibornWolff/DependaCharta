---
name: tse-rust-integration
issue:
state: complete
version: v0.11.0
---

## Goal

Add **Rust** as a supported language in DependaCharta by consuming TSE's new Rust dependency analysis
(`TreeSitterDependencies.analyze(content, Language.RUST)`). This is **PR 2 of 2** — it depends on the
TSE Rust dependency support (PR 1: `TreeSitterExcavationSite/plans/add-rust-dependency-support.md`)
being merged and **released as a tag**. Rust is brand-new to DC (no legacy analyzer), so this is a
fresh language addition, not a migration — closest precedents: `plans/2026-04-24-add-delphi-support.md`
(new language) and `plans/tse-typescript-javascript-integration.md` (composite-build workflow).

Rust is a **Class-2 / multi-namespace language** (nested `mod` blocks), so `RustAnalyzer` follows
`CSharpAnalyzer` (implements `LanguageAnalyzer` directly, derives per-declaration path from
`parentPath`), **plus** a Rust-specific step C# doesn't need: derive the file's crate-root module path
from `physicalPath` (modeled on Go's `GoPackageQuery.derivePackagePathFromFilePath`) and normalize
`crate`/`self`/`super` in imports, because a `.rs` file's module path is filesystem-derived, not in its
content.

## Tasks

### 1. Set up composite build for local iteration (revert before final PR)
- TSE's Rust support won't be tagged while iterating. In `analysis/settings.gradle.kts` add
  `includeBuild("../../TreeSitterExcavationSite")` with `dependencySubstitution` substituting
  `com.github.MaibornWolff:TreeSitterExcavationSite` → the local project (precedent:
  `plans/tse-typescript-javascript-integration.md`). Keep the JitPack repo for transitive deps.
- This lets `RustAnalyzerTest` run against the in-progress local TSE extractors before any tag exists.
- **Revert** `settings.gradle.kts` and pin the published tag (Task 7) before opening the final PR.

### 2. Register the language (compile-breaker wiring)
- `analysis/.../pipeline/shared/SupportedLanguage.kt`: add `RUST("Rust", listOf("rs")),`. The enum
  name **must** be exactly `RUST` so `Language.valueOf(name)` resolves to TSE's `Language.RUST`.
  `suffixes` is the single source of truth — extension detection (walker + `determineLanguage`) and
  the help text adapt automatically; `RootDirectoryWalker`/`FileInfo` need no change.
- `analysis/.../analyzers/LanguageAnalyzerFactory.kt`: add `SupportedLanguage.RUST -> RustAnalyzer(fileInfo)`
  (+ import). Exhaustive `when`, no `else` → won't compile without it.
- `analysis/.../processing/dependencies/DependencyResolverService.kt`: add
  `SupportedLanguage.RUST -> EmptyStandardLibrary()` to `standardLibraryFor`. Second exhaustive `when`.
  (Std/core/alloc are out-of-project externals; `EmptyStandardLibrary` is the right default — Rust
  has no bundled stdlib dictionary in v1.)

### 3. Implement RustAnalyzer (TDD, Class-2 like CSharpAnalyzer)
- `analysis/.../analyzers/rust/RustAnalyzer.kt`, implementing `LanguageAnalyzer` (not
  `BaseLanguageAnalyzer`, because of the file-module-path step). Per `result.declarations`:
  - `fileModulePath = deriveModulePathFromPhysicalPath(fileInfo.physicalPath)` — crate root via
    nearest `Cargo.toml`/`src`; `lib.rs`/`main.rs`/`mod.rs` → the dir module; `foo.rs` → `…::foo`
    (Rust 2018 path rules). Model on Go's `GoPackageQuery.derivePackagePathFromFilePath`.
  - `nodePath = fileModulePath + declaration.parentPath` (TSE's `parentPath` = in-file inline-`mod`
    chain; `result.packagePath` is ignored, as in `CSharpAnalyzer`).
  - imports: normalize leading segments against `fileModulePath` (`self`→fileModulePath,
    `super`→drop last, `crate`→crate root), then `ImportDeclaration.toDependency()`.
  - self-wildcard: `Dependency(Path(nodePath), isWildcard = true)` per declaration (intra-module
    resolution).
  - `namespacePrefix` on used types: emit a synthetic `Dependency(Path(prefix), isWildcard=true)` per
    `UsedType` with a non-empty `namespacePrefix` (mirror `CppAnalyzer`; resolver consumes it via the
    existing wildcard-prepend loop).
  - Map `declaration.type.toNodeType()` and `usedTypes.map { it.toType() }` via the existing
    `TseMappings.kt` — **no change there** (TSE reuses the existing `DeclarationType` values).
- `analysis/src/test/.../analyzers/RustAnalyzerTest.kt` (pattern from `DelphiAnalyzerTest`, inline
  source, Given/When/Then): module path as node prefix, `use` imports → deps incl. glob/alias/`pub use`,
  `crate`/`self`/`super` normalization, intra-module self-wildcard, struct/enum/trait/fn/type-alias
  node types, nested `mod` → `parentPath`, `impl Trait for Type` → inheritance edge, signature used
  types (fields/params/returns/bounds/where/supertraits), qualified `namespacePrefix` synthetic dep,
  empty file → empty report.

### 4. Update the factory test
- `analysis/src/test/.../analyzers/LanguageAnalyzerFactoryTest.kt`: add a `rustFileInfo` + an
  `Arguments.of(rustFileInfo, RustAnalyzer(rustFileInfo))` row (+ import). The parameterized test
  enumerates every language and must include Rust.

### 5. Docs + changelog
- Add "Rust" to the supported-languages lists: `README.md`, `CLAUDE.md` (Key Technologies +
  Migrating-a-Language note), `.agents/skills/dependacharta/SKILL.md`.
- `CHANGELOG.md` `[Unreleased]/Added`: "Add Rust dependency analysis support (.rs files)".
- **Visualization: no change** — the Angular app is language-agnostic (`language` is a pass-through
  string, never branched on). No legend/color/icon entry needed.

### 6. Validate (no legacy analyzer → /dc-compare is N/A)
- `mise run test-analysis` green (ktlint runs in the test task). `RustAnalyzerTest` is the primary
  gate since there's no DC-main baseline to diff against.
- Manual: `mise run analyze <a real Rust repo>` (e.g. a mid-size crate with nested modules, traits,
  generics) and eyeball the `.cg.json` — sane nodes/edges, no empty-named nodes, externals
  (`std`/`tokio`/…) classified out of project. Optionally add `exampleProjects/RustExample/`.

### 7. Switch to the released TSE tag (final PR step)
- After TSE PR 1 is tagged: revert the Task-1 composite build and bump
  `analysis/build.gradle.kts:23` `com.github.MaibornWolff:TreeSitterExcavationSite:v0.9.1` → the new
  tag (e.g. `v0.10.0`). This is the **only** place the TSE version is referenced.
- Re-run `mise run test-analysis` green against the published artifact.

## Steps

- [x] Complete Task 1: composite build for local iteration
- [x] Complete Task 2: register `RUST` in `SupportedLanguage`, `LanguageAnalyzerFactory`,
  `DependencyResolverService` (3 compile-breaker spots)
- [x] Complete Task 3: `RustAnalyzer` + `RustAnalyzerTest` (TDD, Class-2 + file-module-path)
- [x] Complete Task 4: `LanguageAnalyzerFactoryTest` Rust row
- [x] Complete Task 5: docs (README, CLAUDE.md, SKILL.md) + CHANGELOG
- [x] Complete Task 6: `mise run test-analysis` green (614 tests, 0 fail; run on JDK 17) + manual
  `analyze` on a multi-module crate — cross-file `crate::` imports resolved, impl-folded method
  types produce real cross-module edges, externals (`std`/`Display`/…) classified out of project,
  no empty-named nodes, correct module paths from physical paths.
- [x] Complete Task 7: reverted the composite build in `settings.gradle.kts` and bumped
  `build.gradle.kts` TSE dependency `v0.9.1` → **`v0.11.0`** (the released tag with Rust support +
  `ImportKind.REEXPORT`). Full suite green against the published JitPack artifact (621 tests, 0
  failures) and `./gradlew fatJar` packages cleanly again (the composite-build `beforeEvaluate`
  quirk is gone).

## Notes

- **SHCBarber validation (real Tauri Cargo workspace, 10 crates / 140 src files) drove two
  improvements beyond the original plan scope** (orphan-node double-check found both):
  1. **Crate-aware node paths** — node paths now include the crate name (dir before `src`, via
     `RustAnalyzer.deriveCrateRoot`), and `crate::` resolves to it. Without this, cross-crate
     `use other_crate::Type` imports never matched the (crate-name-stripped) definition node, so
     every cross-crate edge was silently dropped. This overrides the plan's accepted "no cross-crate
     resolution" v1 limitation. Recovered ~340 cross-crate edges; ~1.6k after re-exports.
  2. **`pub use` re-export flattening** — crates flatten their public API at `lib.rs`
     (`pub use module::Type`), and consumers import the short `crate::Type`. TSE now tags `pub use`
     with `ImportKind.REEXPORT`; `RustAnalyzer` emits a `NodeType.REEXPORT` carrier
     (`crate::Type` → `crate::module::Type`) which `DependencyResolverService.applyRustReexportAliases`
     folds into an **alias** on the real node (and drops the carrier) — so consumers resolve straight
     to the definition with no forwarding-decoy cycles. This was explicitly out-of-scope originally;
     implemented on request. SHCBarber result: 791→841 nodes, 1154→**2294 edges**, **0 cycles**,
     non-test isolated 128→**59** (all legit value types/consts/string helpers; 0 bugs, 0 spurious).
- **Hard ordering**: TSE PR 1 must merge **and tag a release** before this PR can be finalized
  (against `v0.9.1`, `analyze(_, RUST)` throws `UnsupportedOperationException`). Use the composite
  build (Task 1) to develop in parallel before the tag exists.
- **No `TseMappings.kt` change expected** — TSE maps Rust items onto the existing `DeclarationType`
  values (struct→CLASS, trait→INTERFACE, enum→ENUM, fn→FUNCTION, const/static→VARIABLE). If PR 1
  ends up adding a new `DeclarationType`, extend `toNodeType()`; otherwise leave it untouched.
- **Why `RustAnalyzer` ≠ `BaseLanguageAnalyzer`**: the base class assumes Class-1 (`packagePath`-driven
  node paths, one self-wildcard from `packagePath`). Rust needs per-declaration `parentPath` paths
  (Class-2) **and** a filesystem-derived file-module prefix — neither fits the base class, so it
  follows `CSharpAnalyzer` directly. Revisit extracting shared Class-2 logic only if a third Class-2
  language lands.
- **No /dc-compare**: that tool diffs against DC main's legacy analyzer output; Rust has none. Unit
  tests + manual `.cg.json` inspection are the validation path.
- **Out of scope** (mirrors TSE PR 1 non-goals): macro-generated types, `#[cfg]` evaluation
  (test modules included), re-export transitivity, function-body type usage, a Rust stdlib dictionary.

## References

- Class-2 analyzer template: `analysis/.../analyzers/csharp/CSharpAnalyzer.kt`
- File-path→module derivation template: `analysis/.../analyzers/golang/queries/GoPackageQuery.kt`
  (`derivePackagePathFromFilePath`), called from `golang/GoAnalyzer.kt`
- `namespacePrefix` synthetic-wildcard precedent: `analysis/.../analyzers/cpp/CppAnalyzer.kt`
- New-language precedent: `plans/2026-04-24-add-delphi-support.md`
- Composite-build workflow: `plans/tse-typescript-javascript-integration.md`
- TSE ↔ DC migration guidance: `CLAUDE.md` ("Migrating a Language Analyzer to TSE")
- Producer (PR 1): `TreeSitterExcavationSite/plans/add-rust-dependency-support.md`
</content>
