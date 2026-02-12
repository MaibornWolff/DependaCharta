# DependaCharta CI/CD Pipeline Documentation

This document describes the GitHub Actions CI/CD pipeline for DependaCharta.

## Overview

The pipeline consists of five workflows that handle building, testing, quality checks, releases, and deployments:

1. **Build Analysis** - Builds and tests the Kotlin analysis component
2. **Build Visualization** - Builds and tests the Angular visualization component
3. **Quality DependaCharta** - Self-check workflow to analyze DependaCharta's own codebase
4. **Release** - Creates versioned releases with packaged artifacts
5. **Deploy Pages** - Deploys the visualization to GitHub Pages

## Workflows

### 1. Build Analysis

**File:** `.github/workflows/build-analysis.yml`

**Triggers:**
- Push to `main` branch (when `analysis/**` or workflow file changes)
- Pull requests (when `analysis/**` or workflow file changes)

**Jobs:**

#### build
- **Runner:** Ubuntu 24.04
- **Timeout:** 30 minutes
- **Steps:**
  1. Checkout code
  2. Setup mise (installs Java 17 from `.mise.toml`)
  3. Setup Gradle cache
  4. Run ktlint for code style checks
  5. Build and test with version set to tag or commit SHA
  6. Upload artifacts:
     - JAR file (`dependacharta.jar`)
     - SBOM (Software Bill of Materials - `bom.json`)
     - Test results (JUnit XML)
     - Coverage reports (JaCoCo XML)
  7. Publish test results to PR

**Artifacts Generated:**
- `dependacharta-jar-{run_id}` - Main JAR artifact (1 day retention)
- `analysis-sbom-{run_id}` - SBOM report (1 day retention)
- `analysis-test-results-{run_id}` - JUnit test results (1 day retention)
- `analysis-coverage-{run_id}` - JaCoCo coverage reports (1 day retention)

**Permissions:**
- `contents: read` - Read repository contents
- `checks: write` - Create check runs
- `pull-requests: write` - Comment on PRs

---

### 2. Build Visualization

**File:** `.github/workflows/build-visualization.yml`

**Triggers:**
- Push to `main` branch (when `visualization/**` or workflow file changes)
- Pull requests (when `visualization/**` or workflow file changes)

**Jobs:**

#### build
- **Runner:** Ubuntu 24.04
- **Timeout:** 30 minutes
- **Steps:**
  1. Checkout code
  2. Setup mise (installs Node.js 22.21.1 from `.mise.toml`)
  3. Install dependencies (`npm ci`)
  4. Run Angular linter
  5. Build application with 4GB memory limit
  6. Run unit tests in CI mode (headless Chrome)
  7. Create version file (tag or commit SHA)
  8. Upload artifacts:
     - Build output (`dist/`)
     - Test results
  9. Publish test results to PR

**Artifacts Generated:**
- `visualization-build-{run_id}` - Build output (1 day retention)
- `visualization-test-results-{run_id}` - Test results (1 day retention)

#### integration-tests
- **Runner:** Ubuntu 24.04
- **Timeout:** 20 minutes
- **Depends on:** build job
- **Steps:**
  1. Checkout code
  2. Setup Node.js and install dependencies
  3. Install http-server and wait-on globally
  4. Download build artifacts from build job
  5. Start http-server on port 4200
  6. Run Cypress tests (headless Electron)
  7. Upload screenshots/videos on failure (7 day retention)

**Artifacts Generated (on failure):**
- `cypress-screenshots-{run_id}` - Test failure screenshots (7 day retention)
- `cypress-videos-{run_id}` - Test failure videos (7 day retention)

**Permissions:**
- `contents: read` - Read repository contents
- `checks: write` - Create check runs
- `pull-requests: write` - Comment on PRs

---

### 3. Quality DependaCharta Self Check

**File:** `.github/workflows/quality-dependacharta.yml`

**Triggers:**
- Manual workflow dispatch with optional artifact upload

**Purpose:** Dogfooding - runs DependaCharta on its own codebase to verify it works correctly and generate dependency graphs.

**Jobs:**

#### self-check
- **Runner:** Ubuntu 24.04
- **Timeout:** 20 minutes
- **Steps:**
  1. Checkout code
  2. Setup Java 17 with Gradle cache
  3. Build DependaCharta JAR (skip tests)
  4. Create output directory
  5. Analyze analysis component:
     - Input: `analysis/src/main/kotlin/de/maibornwolff/dependacharta`
     - Output: `graphs/dependacharta-analysis.cg.json`
  6. Analyze visualization component:
     - Input: `visualization/src`
     - Output: `graphs/dependacharta-visualization.cg.json`
  7. List generated graphs
  8. Upload graph artifacts (if enabled)

**Artifacts Generated:**
- `dependacharta-self-check-{run_id}` - Generated dependency graphs (7 day retention)

**Permissions:**
- `contents: read` - Read repository contents

---

### 4. Release

**File:** `.github/workflows/release.yml`

**Triggers:**
- Push tags matching `v*.*.*` or `*.*.*` (semantic versioning)

**Purpose:** Creates GitHub releases with packaged artifacts for distribution.

**Jobs:**

#### build-visualization
- **Runner:** Ubuntu 24.04
- **Timeout:** 30 minutes
- **Steps:**
  1. Checkout code
  2. Setup Node.js 22.12.0
  3. Install dependencies
  4. Build application
  5. Create version file with tag name
  6. Package for Mac Silicon (ARM64)
  7. Package for Windows (x64)
  8. Create zip archives:
     - `dependacharta-visualization-mac-silicon-{tag}.zip`
     - `dependacharta-visualization-win-{tag}.zip`
  9. Upload artifacts

**Artifacts Generated:**
- `visualization-mac-silicon` - Mac ARM64 package (1 day retention)
- `visualization-windows` - Windows x64 package (1 day retention)

#### build-analysis
- **Runner:** Ubuntu 24.04
- **Timeout:** 30 minutes
- **Steps:**
  1. Checkout code
  2. Setup Java 17
  3. Build with tag version
  4. Prepare release package:
     - `dependacharta.jar`
     - `dependacharta.sh` (Mac/Linux)
     - `dependacharta.bat` (Windows)
  5. Create zip: `dependacharta-analysis-{tag}.zip`
  6. Upload artifact

**Artifacts Generated:**
- `analysis-package` - Analysis CLI package (1 day retention)

#### create-release
- **Runner:** Ubuntu 24.04
- **Timeout:** 10 minutes
- **Depends on:** build-visualization, build-analysis
- **Steps:**
  1. Checkout code
  2. Download all artifacts from previous jobs
  3. Check if prerelease (alpha/beta/rc in tag)
  4. Create GitHub Release with:
     - All zip files as release assets
     - Generated release notes
     - Installation instructions
     - Marked as latest (if not prerelease)

**Release Assets:**
- `dependacharta-visualization-mac-silicon-{tag}.zip` - Mac visualization app
- `dependacharta-visualization-win-{tag}.zip` - Windows visualization app
- `dependacharta-analysis-{tag}.zip` - Java CLI tool

**Permissions:**
- `contents: write` - Create releases and upload assets

---

### 5. Deploy to GitHub Pages

**File:** `.github/workflows/deploy-pages.yml`

**Triggers:**
- Push tags matching `v*.*.*` or `*.*.*`
- Manual workflow dispatch (can specify tag or use latest)

**Purpose:** Deploys the visualization component to GitHub Pages for web access.

**Jobs:**

#### build
- **Runner:** Ubuntu 24.04
- **Timeout:** 30 minutes
- **Steps:**
  1. Checkout code with full history
  2. Determine version to deploy:
     - Manual: use input tag or latest tag
     - Tag push: use pushed tag
  3. Checkout specific tag (if manual)
  4. Setup Node.js 22.12.0
  5. Install dependencies and build visualization
  6. Create version file
  7. Prepare public directory structure:
     - Copy landing page (`index.html` → `dependacharta.html`)
     - Copy visualization files (JS, CSS, HTML, icons, resources)
     - Copy example graph file
  8. Update download links in landing page with release URLs
  9. Setup GitHub Pages
  10. Upload artifact for Pages

**Output Structure:**
```
public/
├── dependacharta.html      # Landing page
├── index.html              # Visualization app
├── *.js, *.css            # App bundles
├── favicon.ico
├── *.json                 # Manifests
├── resources/
│   └── java-example.cg.json
└── icons/
    └── *.svg
```

#### deploy
- **Runner:** Ubuntu 24.04
- **Timeout:** 10 minutes
- **Depends on:** build job
- **Environment:** github-pages
- **Steps:**
  1. Deploy to GitHub Pages using uploaded artifact

**Permissions:**
- `contents: read` - Read repository contents
- `pages: write` - Deploy to Pages
- `id-token: write` - OIDC authentication

---

## Pipeline Features

### Concurrency Control

All workflows implement concurrency control to prevent redundant runs:

- **Build workflows:** Cancel in-progress runs on same PR/branch
- **Release:** Prevent concurrent releases
- **Deploy Pages:** Prevent concurrent deployments

### Security & Permissions

All workflows follow the **principle of least privilege**:
- Explicitly defined permissions per workflow
- No workflows request unnecessary permissions
- Read-only access by default

### Artifact Management

- **Short retention (1 day):** Build artifacts for immediate downstream use
- **Medium retention (7 days):** Debug artifacts (screenshots, videos, self-check graphs)
- Artifacts are clearly named with run IDs for traceability

### Caching Strategy

- **Gradle:** Java dependencies cached by setup-java action
- **npm:** Node dependencies cached by setup-node action
- Cache key based on lock files for optimal invalidation

### Testing Strategy

1. **Analysis Component:**
   - Code style (ktlint)
   - Unit tests (JUnit)
   - Coverage reports (JaCoCo)
   - SBOM generation

2. **Visualization Component:**
   - Linting (Angular ESLint)
   - Unit tests (Karma + Jasmine)
   - Integration tests (Cypress)
   - Build verification

### Version Management

- **Tags:** Use tag name for version
- **Non-tags:** Use commit SHA for version
- Version embedded in artifacts via build-time flags

## Workflow Dependencies

```
Tag Push (v*.*.*)
    ├─> Release Workflow
    │   ├─> build-analysis
    │   ├─> build-visualization
    │   └─> create-release (depends on both)
    │
    └─> Deploy Pages Workflow
        ├─> build
        └─> deploy (depends on build)

Push to main
    ├─> Build Analysis (if analysis/** changed)
    └─> Build Visualization (if visualization/** changed)
        └─> integration-tests (depends on build)

Pull Request
    ├─> Build Analysis (if analysis/** changed)
    └─> Build Visualization (if visualization/** changed)
        └─> integration-tests (depends on build)

Manual Trigger
    ├─> Deploy Pages (specify tag or use latest)
    └─> Quality DependaCharta (dogfooding check)
```

## Environment Requirements

Runtime versions are defined in `.mise.toml` and used by both local development and CI.

### Analysis Component
- **OS:** Ubuntu 24.04
- **Java:** 17 (Temurin, via mise)
- **Build Tool:** Gradle (wrapper)
- **Memory:** Default

### Visualization Component
- **OS:** Ubuntu 24.04
- **Node.js:** 22.21.1 (via mise)
- **Package Manager:** npm
- **Browser:** Chrome (for tests)
- **Memory:** 4GB (for build)

## Monitoring & Debugging

### Test Results
- Published to GitHub Checks API
- Visible in PR reviews
- Historical tracking in Actions tab

### Failure Artifacts
- Cypress screenshots/videos (7 days)
- Test result XMLs (1 day)
- Build logs in workflow runs

### Self-Check
- Run quality-dependacharta.yml manually to verify tool health
- Generates graphs of DependaCharta's own architecture
- Useful for regression testing and validation

## Future Improvements

Potential enhancements to consider:

1. **Mac Intel Support:** Add packaging for x64 macOS
2. **Linux Support:** Add packaging for Linux distributions
3. **E2E Tests on Release:** Run full E2E suite before creating release
4. **Security Scanning:** Add dependency vulnerability scanning
5. **Performance Tests:** Add benchmark tests for large codebases
6. **Docker Images:** Publish Docker images for containerized usage
7. **Changelog Generation:** Automated changelog from conventional commits
