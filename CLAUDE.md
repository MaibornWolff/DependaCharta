# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DependaCharta is a multi-language code analysis and visualization tool that consists of two main components:
- **Analysis**: Kotlin-based CLI tool using Tree-sitter parsers to analyze codebases
- **Visualization**: Angular/TypeScript web app using Cytoscape.js for interactive dependency graphs

## Build and Development Commands

### Quick Commands (using justfile)

```bash
# Run unit tests
just test

# Build the analysis tool
just build

# Run analysis on a directory (builds first if needed)
just run <directory-to-analyze>

# Analyze a directory and prepare for visualization (builds first if needed)
just analyze <directory-to-analyze>

# Start frontend development server (installs deps automatically)
just frontend
```

**Note**: The difference between `run` and `analyze`:
- `just run <dir>` - Runs analysis and outputs to `output/analysis.cg.json`
- `just analyze <dir>` - Runs analysis and outputs to `visualization/public/analysis/analyzed-project.cg.json` and then opens the frontend instance and shows it directly to the user. 

### Analysis Component (Kotlin)

```bash
# Build the analysis tool
cd analysis
./gradlew fatJar

# Run tests
./gradlew test

# Run the analyzer
java -jar build/libs/dependacharta.jar -d <directory-to-analyze>
# Or use: bin/dependacharta.sh (Mac/Linux) or bin/dependacharta.bat (Windows)
```

### Visualization Component (Angular)

```bash
cd visualization

# Install dependencies
npm ci

# Development server (http://localhost:4200)
npm run start

# Run as Electron app
npm run start-electron

# Build
npm run build

# Run tests with coverage
npm run test

# Run E2E tests
npm run cypress:open  # Interactive
npm run cypress:run   # Headless

# Package for distribution
npm run package-win         # Windows
npm run package-mac-silicon # macOS ARM64
npm run package-mac-intel   # macOS x64
```

## Architecture Overview

### Analysis Flow
1. Tree-sitter parsers analyze source files without compilation
2. Creates dependency graph with cycle detection and levelization
3. Outputs `.cg.json` files containing the analysis results

For a detailed explanation of DependaCharta's domain concepts, see our [Domain Model Documentation](DOMAIN.md).

### Key Technologies
- **Analysis**: Kotlin, Tree-sitter, Gradle, Clikt CLI framework
- **Visualization**: Angular 20, TypeScript, Cytoscape.js, Electron
- **Supported Languages**: Java, C#, C++, TypeScript, PHP, Go, Python

### Project Structure
```
analysis/
├── src/main/kotlin/        # Core analysis logic
│   ├── de/maibornwolff/    # Main package
│   └── parsing/            # Language-specific parsers
└── src/test/               # Test files

visualization/
├── src/app/                # Angular components
│   ├── codeCharta/         # Core visualization
│   └── model/              # Data models
└── src/assets/             # Static assets
```

### Important Patterns
- Parser implementations extend `Parser` class and follow visitor pattern
- Each language has its own parser in `analysis/src/main/kotlin/de/maibornwolff/dependacharta/core/input/*/`
- Angular components follow standard Angular patterns with services for data management
- Visualization uses reactive patterns with RxJS for state management

## CI/CD Pipeline

The GitLab CI pipeline automatically:
1. Runs tests for both components
2. Builds distributable artifacts
3. Creates releases on tags matching semantic versioning (e.g., 0.42.23)
4. Deploys documentation to GitLab Pages

## Development Workflow

### Planning

**For every new instruction, create a plan** in the `plans/` folder using `template.md` as a base:

- Plans act as high-level guidance for implementation
- Focus on mandatory steps only, avoid excessive detail
- keep plans simple and concise
- avoid over-elaboration, detailed sections, or comprehensive documentation style
- plans should be brief, actionable outlines rather than detailed specifications
- Use checkable steps (markdown checkboxes) to track progress
- Update `state` field as work progresses: `todo` → `progress` → `complete`
- Always use the AskUserQuestion tool to clarify ambiguous requirements before finalizing the plan

### Branching Strategy

- Main branch: `main`
- Branch naming: `<type>/<issue-id>/<name>` (e.g., `feature/123/add-dark-mode`)
- Types: `feature`, `fix`, `docs`, `revert`, `codestyle`, `tech`
- Always rebase on main: `git rebase main`
- After rebase: `git push --force-with-lease`

### Commit Message Format

```
<type>(<scope?>): <subject>(#<issue-number?>)

<body-description?>
```

- Types: `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`
- Scopes: `analysis`, `visualization`, `docker`, `gh-pages`, `docs`, `readme`, `stg`, `config`
- Use imperative mood: "Add feature" not "Added feature"
- Limit subject line to 72 characters
- Breaking changes: Use `!` before `:` (e.g., `feat!: breaking change`)

Example: `feat(visualization): add dark mode toggle (#123)`

### Pull Requests

- Name PR like branch name
- Follow PR template
- Add appropriate labels
- All tests must pass before merge
- Prefer rebase over squash merge for clean history
- Update CHANGELOG.md with changes (follow https://keepachangelog.com)

### Code Style

**Analysis (Kotlin)**:
- Based on official Kotlin Coding Conventions
- Auto-formatted via `./gradlew ktlintFormat`
- Rules defined in `.editorconfig`
- **Function syntax**: Use block-body style with braces `{ }` consistently, not expression-body style with `=`
- **Guard clauses**: Use early returns for error conditions and edge cases to reduce nesting
- **If expressions**: Prefer concise single-line style when possible:
    - ✅ `val x = if (condition) valueA else valueB`
    - ❌ `val x = if (condition) { valueA } else { valueB }`
    - Use multi-line only when branches contain multiple statements or complex logic
- **Magic strings/numbers**: Extract repeated literals to constants in `companion object`
- **Function organization**: Group related functions with section comments
- **Parameter naming**: Use consistent, descriptive names across related functions

**Visualization (TypeScript)**:
- Formatted with BiomeJS
- Install Biome extension and format on save
- Git hooks auto-format on commit via Husky

**Commits**:
- Husky runs pre-commit hooks automatically
- Lint-staged formats files before commit
- Analysis is NOT auto-linted on commit

### Code Quality Guidelines

**General Principles**:
- **DRY**: Extract repeated logic into reusable functions
- **Clean Code**: Self-documenting code with clear intent
- **SOLID**: Single responsibility, open/closed, dependency inversion
- **Expressive Naming**: Descriptive names that reveal intent
- **Fix Warnings**: Never suppress, always resolve
- **Consistent Style**: Match existing patterns
- **Comments**: Use sparingly for complex business logic rationale. Prefer clear function names over comments.
- **Metric Accuracy**: All metrics must be deterministic and reproducible across runs
- **Immutability**: Prefer immutable data structures, especially in the model layer
- **Backward Compatibility**: Changes to `.cc.json` format require careful versioning

**TDD Workflow** (Red → Green → Refactor):
1. **Write one failing test FIRST** - Never write implementation before tests
   - When reproducing bugs, tests MUST be red (failing) before the fix
   - If test is green before fixing bug, the test doesn't reproduce the bug correctly
2. Write minimum code to pass
3. **Run ALL tests** via `just test` (must be green before any commit)
4. Refactor if needed
5. **Request permission to commit** - NEVER commit without asking first
6. Repeat

**Test Quality Guidelines**:
- **Given-When-Then structure**: ALWAYS use Given-When-Then comments to structure tests
  - `// Given` - Setup and preconditions
  - `// When` - Action being tested
  - `// Then` - Expected outcomes (assertions)
  - Example:
    ```kotlin
    @Test
    fun `analyzes Vue component`() {
        // Given
        val vueCode = "..."
        val fileInfo = FileInfo(...)

        // When
        val result = analyzer.analyze()

        // Then
        assertThat(result.nodes).hasSize(1)
    }
    ```
- **No redundant assertions**: Remove unnecessary null checks before equality assertions
  - ❌ Bad: `assertNotNull(result); assertEquals(expected, result)`
  - ✅ Good: `assertEquals(expected, result)` (this already fails on null)
- **Keep tests concise**: Remove any assertion that doesn't add value
- Tests written to reproduce bugs must fail until the bug is fixed

### Committing Code

**CRITICAL RULES**:
- **NEVER commit code without asking first**
- **ALWAYS run `just test`** before any commit to ensure all tests pass
- Only commit after receiving explicit user permission
- Follow the commit message format defined above

### Post-Implementation Code Review

After implementing a feature and all tests pass, **always ask if you should commit**. After committing, **propose a code review** to check for:

**Code Structure**:
- Methods are short with single level of abstraction (SLA principle)
- No mixed abstraction levels within methods
- Proper separation of concerns

**Naming & Consistency**:
- Classes, methods, variables have correct and concise names
- Naming precision matches scope size (larger scope = more precise name)
- Consistent naming patterns throughout new code

**Test Quality**:
- Tests are concise without redundant assertions
- Tests follow existing structure (Given-When-Then or Arrange-Act-Assert)
- Test names are exhaustive and describe the specific scenario, not just the feature
- No implementation comments or plan relicts left in test code

**Code Quality**:
- KISS and DRY principles applied
- No duplication - new code should call existing code when possible
- No features implemented that weren't requested
- Code follows existing patterns in the codebase

**Before proposing changes**:
- Ask about missing important features or use cases if noticed
- Verify all implementation matches user's explicit requests