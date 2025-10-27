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

## Development Practices

- Test-driven development with tests before implementation
- Clean Code principles
- Semantic versioning for releases
- Automated dependency updates via Renovate