# PR186 Review Fixes

| Field | Value |
|-------|-------|
| state | complete |
| branch | feature/mise-integration |

## Goal

Expand mise task coverage so every `npm`/`npx`/`gradlew`/`java -jar` call in CI goes through `mise run`. Establish naming conventions with clear task categories.

## Task Categories & Naming Convention

### 1. Dev tasks (no prefix)

High-level workflows for daily development. Compose granular tasks via `depends`.

| Task | Description | Composition |
|---|---|---|
| `test` | Run analysis unit tests | runs `./gradlew test` |
| `test-frontend` | Full frontend verification | depends: `install-frontend`, `build-frontend`, then runs headless tests |
| `test-e2e` | Run E2E tests | requires frontend server |
| `build` | Build analysis fat JAR | runs `./gradlew fatJar` |
| `frontend` | Start dev server | depends: `install-frontend` |
| `analyze` | Analyze dir + open viz | depends: `build` |
| `run` | Run analysis on a dir | depends: `build` |
| `format` | Auto-format Kotlin | |
| `help` | Show CLI help | depends: `build` |

### 2. Granular tasks (component-naming, no prefix)

Atomic reusable steps. Convention: bare name = analysis, `-frontend` suffix = visualization.

| Task | Description | Command |
|---|---|---|
| `lint` | Kotlin code style check | `./gradlew ktlintCheck` |
| `lint-frontend` | Angular linter | `npx ng lint` |
| `install-frontend` | Install npm dependencies | `npm ci` |
| `build-frontend` | Build frontend app | `npm run build` |

### 3. CI tasks (`ci-` prefix)

Tasks primarily for CI pipelines. Can be run locally to reproduce CI behavior.

| Task | Description | Command | Args |
|---|---|---|---|
| `ci-build-analysis` | Full analysis build+test with version | `./gradlew build -Pversion=$1` | version string |
| `ci-compile-analysis` | Compile only (for SonarCloud) | `./gradlew compileKotlin compileTestKotlin --no-daemon` | — |
| `ci-test-frontend` | Run frontend tests in CI mode | `npm run ci-test` | — |
| `ci-create-version` | Write version.json into build output | `npm run ci-createVersion -- $1` | version string |
| `ci-package-mac` | Package for Mac Silicon | `npm run package-mac-silicon` | — |
| `ci-package-win` | Package for Windows | `npm run package-win` | — |
| `ci-e2e` | Run E2E in CI mode | `npx cypress run --browser electron --config video=false,screenshotOnRunFailure=false` | — |
| `ci-prepare-release` | Prepare analysis release package | copies jar + scripts into `build/release` | — |

### 4. Docker tasks (`docker-` prefix)

Unchanged from current.

| Task | Description |
|---|---|
| `docker-build` | Build Docker image |
| `docker-build-multi` | Build multi-platform image |
| `docker-run` | Run analysis with Docker |
| `docker-analyze` | Docker analysis + visualization |

## What stays raw in CI workflows

These are GitHub Actions concerns or CI-specific orchestration, not tool invocations:

- `actions/checkout`, `actions/upload-artifact`, `actions/cache`, etc.
- `chmod +x gradlew` (git permission safety)
- `mkdir -p graphs`, `ls -R release-assets` (trivial filesystem ops)
- `git checkout $TAG` (deploy-pages tag checkout)
- Version detection logic (tag vs SHA if/else → sets env vars for mise tasks)
- `npm install -g http-server wait-on` (CI-only global install for integration test server)
- Integration test server orchestration (start http-server, wait-on, cypress, kill)
- Zip creation with parameterized filenames (release packaging, uses `${{ github.ref_name }}`)
- `sed` commands for landing page URL replacement (deploy-pages)
- Prerelease detection (if/else on tag name)

## CI Workflow Migration

### build-analysis.yml — build job
| Step | Before | After |
|---|---|---|
| ktlint | `./gradlew ktlintCheck` | `mise run lint` ✅ already done |
| Build and test | `./gradlew build -Pversion=$RELEASE` | `mise run ci-build-analysis $RELEASE` |

### build-analysis.yml — sonarcloud job
| Step | Before | After |
|---|---|---|
| Compile for bytecode | `./gradlew compileKotlin compileTestKotlin --no-daemon` | `mise run ci-compile-analysis` |

### build-visualization.yml — build job
| Step | Before | After |
|---|---|---|
| Install deps | `npm ci` | `mise run install-frontend` |
| Linter | `npx ng lint` | `mise run lint-frontend` |
| Build | `npm run build` | `mise run build-frontend` |
| Unit tests | `npm run ci-test` | `mise run ci-test-frontend` |
| Create version | `npm run ci-createVersion -- $RELEASE` | `mise run ci-create-version $RELEASE` |

### build-visualization.yml — integration-tests job
| Step | Before | After |
|---|---|---|
| Install deps | `npm ci` | `mise run install-frontend` |

### quality-dependacharta.yml
| Step | Before | After |
|---|---|---|
| Build JAR | `./gradlew build -x test` | `mise run build` ✅ already done |

### release.yml — build-visualization job
| Step | Before | After |
|---|---|---|
| Install deps | `npm ci` | `mise run install-frontend` |
| Build | `npm run build` | `mise run build-frontend` |
| Create version | `npm run ci-createVersion -- $TAG` | `mise run ci-create-version $TAG` |
| Package Mac | `npm run package-mac-silicon` | `mise run ci-package-mac` |
| Package Win | `npm run package-win` | `mise run ci-package-win` |

### release.yml — build-analysis job
| Step | Before | After |
|---|---|---|
| Build with version | `./gradlew build -Pversion=$TAG` | `mise run ci-build-analysis $TAG` |
| Prepare release | cp jar + scripts to build/release | `mise run ci-prepare-release` |

### deploy-pages.yml — build job
| Step | Before | After |
|---|---|---|
| Install deps | `npm ci` | `mise run install-frontend` |
| Build | `npm run build` | `mise run build-frontend` |
| Create version | `npm run ci-createVersion -- $TAG` | `mise run ci-create-version $TAG` |

## Caching

`mise-action` has `cache: true` by default (caches Java/Node binaries). npm cache (`~/.npm`) via `actions/cache@v4` is still needed. No changes required.

## Steps

- [x] Add `[tasks.lint]` to `.mise.toml` (done in prior commit)
- [x] Add all new granular and CI tasks to `.mise.toml`
- [x] Restructure `.mise.toml` with category comments
- [x] Refactor existing dev tasks to use `depends` where applicable
- [x] Update `build-analysis.yml` to use mise tasks
- [x] Update `build-visualization.yml` to use mise tasks
- [x] Update `release.yml` to use mise tasks
- [x] Update `deploy-pages.yml` to use mise tasks
- [x] Update `quality-dependacharta.yml` (already partially done)
- [x] Fix all hardcoded versions in PIPELINE.md
- [x] Update PIPELINE.md caching strategy section
