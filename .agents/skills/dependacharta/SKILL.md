---
name: dependacharta
description: Analyze codebase dependencies using DependaCharta. Detects dependency cycles, calculates levels, and outputs a structured dependency graph. Use when asked to analyze dependencies, find cycles, check architecture, or explore codebase structure.
---

# DependaCharta Analysis

Analyze source code dependencies without compilation using Tree-sitter parsers. Supports Java, C#, C++, TypeScript, JavaScript, PHP, Go, Python, Kotlin, and Vue.

## Setup

**IMPORTANT: Always ask the user for permission before running setup, as it downloads files or pulls Docker images.**

Run once to detect runtime and download the analyzer:

```bash
{baseDir}/scripts/setup.sh
```

This will:
1. Check if Java 17+ is available → download the fat JAR from the latest GitHub release
2. If no Java, check if Docker is available → pull the Docker image
3. If neither is available → print instructions and exit with error

To check if setup has already been done:

```bash
{baseDir}/scripts/setup.sh --check
```

## Analyze

```bash
{baseDir}/scripts/analyze.sh <directory>                          # Analyze directory, output to <directory>/.dependacharta/
{baseDir}/scripts/analyze.sh <directory> -o <output-dir>          # Custom output directory
{baseDir}/scripts/analyze.sh <directory> -f <filename>            # Custom output filename (default: analysis)
{baseDir}/scripts/analyze.sh <directory> -l debug                 # Set log level (debug|info|warn|error|fatal)
{baseDir}/scripts/analyze.sh <directory> -g                       # Skip graph analysis (cycles, levels)
```

Output file: `<output-dir>/<filename>.cg.json`

After analysis, tell the user they can visualize results at the **[Web Studio](https://maibornwolff.github.io/DependaCharta/)** by opening the `.cg.json` file there. All processing happens locally in the browser — no data leaves the machine.

## Understanding Results

The `.cg.json` file contains a dependency graph with:

- **projectTreeRoots**: Hierarchical tree of namespaces (packages/directories) and leaves (files/modules)
- **leaves**: Each has a `leafId`, `name`, `level` (0 = no deps, higher = depends on lower levels)
- **containedInternalDependencies**: Map of dependencies per namespace, each with:
  - `isCyclic`: whether this dependency is part of a cycle
  - `isPointingUpwards`: whether it violates layering (lower level depending on same/higher level)
  - `weight`: number of individual dependencies aggregated
  - `type`: dependency type (e.g., `usage`)
- **Edge types** (derived from isCyclic + isPointingUpwards):
  - Regular (not cyclic, not upward) — clean dependency
  - Cyclic (cyclic, not upward) — circular dependency following architectural flow
  - Feedback container level (not cyclic at leaf, but upward) — architectural violation at package level
  - Feedback leaf level (cyclic and upward) — worst case, cycle + architectural violation

### Quick Analysis with jq

```bash
# Count total leaves (files/modules)
jq '[.. | .leafId? // empty] | length' <output>.cg.json

# List all leaves with their levels
jq '[.. | select(.leafId?) | {id: .leafId, level: .level}] | sort_by(.level) | reverse' <output>.cg.json

# Find cyclic dependencies
jq '[.. | .containedInternalDependencies? // empty | to_entries[] | select(.value.isCyclic == true) | .key] | unique' <output>.cg.json

# Find upward-pointing dependencies (architectural violations)
jq '[.. | .containedInternalDependencies? // empty | to_entries[] | select(.value.isPointingUpwards == true) | .key] | unique' <output>.cg.json

# List top-level namespaces
jq '.projectTreeRoots[] | .name' <output>.cg.json
```
