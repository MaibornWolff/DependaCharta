---
name: Add Delphi Support
issue: ~
state: complete
version: ~
---

## Goal

Add Delphi dependency analysis to DependaCharta by integrating the TSE `Language.DELPHI` support already implemented in TreeSitterLibrary. Follows the same migration pattern as Java/Kotlin/C#.

## Tasks

### 1. Extend `SupportedLanguage` enum

Add a `DELPHI` entry to `analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/shared/SupportedLanguage.kt`:

```kotlin
DELPHI("Delphi", listOf("pas", "dpr")),
```

`SupportedLanguage.DELPHI.name = "DELPHI"` maps directly to `Language.DELPHI` in TSE via `Language.valueOf(language.name)` in `BaseLanguageAnalyzer`.

### 2. Create `DelphiAnalyzer`

Create `analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/analysis/analyzers/delphi/DelphiAnalyzer.kt`.

Identical pattern to `JavaAnalyzer` — extend `BaseLanguageAnalyzer`, no custom path building needed (Delphi uses the Class 1 namespace model: single `unit` name as package, top-level declarations only):

```kotlin
class DelphiAnalyzer(fileInfo: FileInfo) : BaseLanguageAnalyzer(fileInfo) {
    override val language = SupportedLanguage.DELPHI
}
```

### 3. Register in `LanguageAnalyzerFactory`

Add to the `when` expression in `LanguageAnalyzerFactory.kt`:

```kotlin
SupportedLanguage.DELPHI -> DelphiAnalyzer(fileInfo)
```

### 4. Write `DelphiAnalyzerTest`

Create `analysis/src/test/kotlin/de/maibornwolff/dependacharta/pipeline/analysis/analyzers/DelphiAnalyzerTest.kt`.

Cover Delphi-specific patterns (Given/When/Then structure):
- Unit name extracted as package path (dotted: `unit MyCo.Utils` → `["MyCo", "Utils"]`)
- `uses` clause entries extracted as dependencies
- Implicit wildcard dependency for own unit package
- Class, interface, record, enum declaration types
- Inheritance types extracted
- Field types extracted
- Method parameter and return types extracted
- Empty file / no declarations → empty report

### 5. Update documentation

- `DependaCharta/CLAUDE.md`: add Delphi to the "Supported Languages" list
- `DependaCharta/CHANGELOG.md`: add entry under `[Unreleased]` / `Added`

## Steps

- [x] Add `DELPHI` to `SupportedLanguage.kt`
- [x] Create `DelphiAnalyzer.kt`
- [x] Register `DelphiAnalyzer` in `LanguageAnalyzerFactory.kt`
- [x] Write `DelphiAnalyzerTest.kt`
- [x] Run `mise run test-analysis` — all tests green
- [x] Update `CLAUDE.md` supported languages list
- [x] Update `CHANGELOG.md`

## Notes

- No visualization changes needed — visualization is language-agnostic.
- No dc-compare verification needed — Delphi is a new language in DC, not a legacy migration.
- TSE's `DelphiDependencyMapping` handles: unit/program package paths, `uses` clause imports (no wildcards in Delphi), class/interface/record/enum declarations, and 7 used-type categories (inheritance, params, returns, fields, variables, constructors, method calls).
