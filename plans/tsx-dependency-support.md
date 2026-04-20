---
name: tsx-dependency-support
issue:
state: todo
version:
---

## Goal

Restore JSX/TSX dependency detection lost during the TSE migration. `.tsx` files must use the TSX tree-sitter grammar so JSX component usage (e.g. `<Routes />`) is detected as `usedTypes`. This worked before via the old `TypescriptAnalyzer` (see commit `c5bc1c47c`) and regressed when we replaced it with a TSE-based implementation.

## Tasks

### 1. Extend TSE's UsedTypeExtractor with JSX node support

In `TreeSitterExcavationSite` on branch `feat/typescript-javascript-dependency-support`:

- **TDD first**: Write a failing test in a new `TsxDependencyTest` (mirror of `TypescriptDependencyTest`) that calls `TreeSitterDependencies.analyze()` with `Language.TSX` on code containing `<Routes />` and asserts `Routes` appears in `usedTypes`
- Add `jsx_opening_element` and `jsx_self_closing_element` to `UsedTypeExtractor.ALL_NODE_TYPES`
- Add `extractJsxComponents()` that walks these nodes, extracts the component identifier (uppercase names only), and returns a list of `UsedType`
- Include the result in the final set returned by `extract()`

Note: verify the exact child node type to extract from JSX nodes (likely `identifier` or `jsx_identifier`) by inspecting the parsed AST during implementation.

### 2. Enable TSX dependency analysis in TsxDefinition

- Add `dependencyMapping` override to `TsxDefinition` pointing to `TypescriptDependencyMapping.dependencyMapping` — no separate mapping object needed since all extractors are shared

### 3. Pass Language.TSX for .tsx files in DC

In `DependaCharta` on branch `feat/tse-typescript-javascript-integration`:

- **Red tests already exist** at lines ~1135–1258 in `TypescriptAnalyzerTest` — no new test needed
- Add `open fun tseLanguage(): Language` to `BaseLanguageAnalyzer`, defaulting to `Language.valueOf(language.name)`
- Use `tseLanguage()` in `BaseLanguageAnalyzer.analyze()` instead of the inline `Language.valueOf(...)`
- Override `tseLanguage()` in `TypescriptAnalyzer`: return `Language.TSX` for `.tsx` files, `Language.TYPESCRIPT` otherwise

## Steps

- [x] Write failing `TsxDependencyTest` in TSE: JSX component asserts as `usedType`
- [ ] Extend `UsedTypeExtractor` with JSX node types → test goes green (TSE agent)
- [ ] Override `dependencyMapping` in `TsxDefinition` with `TypescriptDependencyMapping.dependencyMapping` (TSE agent)
- [x] Add `open fun tseLanguage()` to `BaseLanguageAnalyzer`; use it in `analyze()`
- [x] Override `tseLanguage()` in `TypescriptAnalyzer` for `.tsx`
- [ ] Run `TypescriptAnalyzerTest` JSX tests (lines ~1135–1258) → green
- [ ] Run full `mise run test-analysis` → all green

## Notes

- Regression introduced by our TSE migration; the old analyzer explicitly used `TreeSitterTsx()` for `.tsx` files
- `TsxDefinition.dependencyMapping` can directly reference `TypescriptDependencyMapping.dependencyMapping` — no new class needed
- `jsx_member_expression` (e.g. `<Form.Input />`) may need separate handling — existing `extractMemberAccesses` only covers `member_expression`, not `jsx_member_expression`; monitor during implementation
- `buildPathWithName` in `TypescriptAnalyzer` already handles `.tsx` correctly — no change needed there
- Also update the note in `plans/tse-typescript-javascript-integration.md` once this plan is complete
