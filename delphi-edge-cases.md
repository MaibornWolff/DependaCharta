# Delphi Edge Cases — DependaCharta

Known issues in DC's Delphi support, discovered via Spring4D analysis (408 files, 1925 declarations).

---

## 1. RECORD declarations mapped to CLASS — **Low Priority**

**File:** `analysis/src/main/kotlin/de/maibornwolff/dependacharta/pipeline/analysis/analyzers/TseMappings.kt`

```kotlin
// Current
DeclarationType.CLASS, DeclarationType.RECORD -> NodeType.CLASS

// After fix
DeclarationType.CLASS -> NodeType.CLASS
DeclarationType.RECORD -> NodeType.RECORD   // or NodeType.VALUECLASS
```

TSE correctly identifies Delphi records as `DeclarationType.RECORD`. DC's `TseMappings` downgrades them to `NodeType.CLASS` because `NodeType` has no RECORD entry.

**Impact:** Zero visual impact — the visualization does not use `nodeType` for rendering (all leaf nodes look identical). The field is metadata only in the `.cg.json` output.

**Spring4D:** 1493 CLASS entries, 0 RECORD entries despite Spring4D containing records such as `TBitmapFileHeader`, `TItem<TKey>`, `THashEntry`.

**Fix options:**
- Add `RECORD` to `NodeType.kt` and map `DeclarationType.RECORD -> NodeType.RECORD`
- Or map to the existing `VALUECLASS` (semantically closer — Delphi records are value types like C# structs)

Check how `VALUECLASS` is currently used (C# structs) before deciding which to prefer.
