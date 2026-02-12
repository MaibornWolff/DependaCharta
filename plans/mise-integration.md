# Mise Integration Plan

| Field | Value |
|-------|-------|
| state | complete |
| branch | feature/mise-integration |

## Goal

Replace `just` with `mise` as the single tool for runtime version management AND task running, simplifying onboarding for occasional contributors.

## Current State

- **Runtime versions**: Not pinned (contributors must manually install correct Java/Node)
- **Task runner**: `justfile` with 13 tasks
- **CI/CD**: Uses `setup-java@v5` (Java 17) and `setup-node@v6` (Node 22.21.1)
- **Documentation**: Multiple places explain prerequisites and commands

## Target State

- **Runtime versions**: Pinned in `.mise.toml` (Java 17, Node 22.21.1)
- **Task runner**: `.mise.toml` `[tasks]` section
- **CI/CD**: Uses `jdx/mise-action` — same tool and versions as local development
- **Documentation**: Simplified "install mise, run mise install" flow

## Files to Create

- [x] `.mise.toml` — tools (java, node) + all tasks from justfile

## Files to Update

- [x] `README.md`
  - Replace "Prerequisites" section with mise install instructions
  - Remove detailed `just` command listings
  - Show `mise tasks` for discovery, highlight key tasks (`mise run analyze`)
  - Keep manual setup sections for users who don't want mise

- [x] `CLAUDE.md`
  - Update "Quick Commands" section from `just` to `mise run`
  - Update TDD workflow section (`mise run test` instead of `just test`)

- [x] `CONTRIBUTING.md`
  - Update development process to mention mise
  - Update unit test commands

- [x] `analysis/README.md`
  - Add note about mise for version management (optional)
  - Keep manual instructions as-is (for Docker users, etc.)

- [x] `PIPELINE.md`
  - Update to reflect mise-based CI
  - Remove references to setup-java/setup-node actions

- [x] `.github/workflows/build-analysis.yml`
  - Replace `setup-java` with `jdx/mise-action`
  - Use `mise run` for tasks where applicable

- [x] `.github/workflows/build-visualization.yml`
  - Replace `setup-node` with `jdx/mise-action`
  - Use `mise run` for tasks where applicable

- [x] `.github/workflows/release.yml`
  - Replace `setup-java` and `setup-node` with `jdx/mise-action`

- [x] `.github/workflows/deploy-pages.yml`
  - Replace `setup-node` with `jdx/mise-action`

- [x] `.github/workflows/quality-dependacharta.yml`
  - Replace `setup-java` with `jdx/mise-action`

## Files to Remove

- [x] `justfile` — fully replaced by `.mise.toml` tasks

## CI/CD Approach

**Use mise in CI** — single source of truth for runtime versions:
- Replace `setup-java` and `setup-node` with `jdx/mise-action`
- `.mise.toml` defines versions for both local and CI
- CI runs exactly what developers run locally

```yaml
- uses: jdx/mise-action@v2
  with:
    install: true
- run: mise run test
```

## Mise Task Mapping

| justfile task | mise task | notes |
|---------------|-----------|-------|
| `test` | `test` | analysis tests |
| `test-frontend` | `test-frontend` | visualization tests |
| `test-e2e` | `test-e2e` | cypress |
| `ktlintformat` | `format` | renamed for clarity |
| `build` | `build` | analysis JAR |
| `help` | `help` | show CLI help |
| `run DIR` | `run` | needs arg handling |
| `frontend` | `frontend` | dev server |
| `analyze DIR` | `analyze` | needs arg handling |
| `docker-build` | `docker-build` | |
| `docker-build-multi` | `docker-build-multi` | |
| `docker-run DIR` | `docker-run` | needs arg handling |
| `docker-analyze DIR` | `docker-analyze` | needs arg handling |

## Documentation Principles

- **Don't duplicate**: Show `mise tasks` for discovery, not full task listings
- **Highlight key workflows**: `mise run analyze <dir>` and `mise run test`
- **Keep alternatives**: Manual setup sections remain for non-mise users
- **Single source of truth**: Task details live in `.mise.toml`, not README

## Onboarding Comparison

### Before (current)
1. Install Java 17 (somehow)
2. Install Node.js 22 (somehow)
3. Install `just`
4. Run `just <command>`

### After (with mise)
1. Install mise
2. Run `mise install`
3. Run `mise run <command>`

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| mise-action bugs/outages | Pin action version, monitor releases |
| CI cache efficiency | Configure mise cache properly in workflows |
| Contributors unfamiliar with mise | Keep manual setup instructions as fallback |
| Mise task syntax limitations | Test all tasks, especially those with DIR arguments |

## Out of Scope

- Adding mise to Docker builds (Dockerfile unchanged)
- Pinning Gradle version (handled by wrapper)
