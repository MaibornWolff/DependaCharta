# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Add Rust dependency analysis support (`.rs` files), including Cargo-workspace crate-aware node paths (cross-crate `use other_crate::Type` references resolve) and `pub use` re-export flattening (a consumer's `use crate::Type` resolves through the crate's `lib.rs` re-export to the real `crate::module::Type` definition).

### Fixed

- Restore import alias resolution for TypeScript/JavaScript/Vue (tsconfig/jsconfig `paths`, bundler aliases, Module Federation remotes), which was silently lost during the TreeSitterExcavationSite migration. Aliased imports now resolve to their target modules instead of being dropped from the dependency graph.

## [0.25.0] - 2026-05-04

### Changed

- Migrate C++ dependency analysis to TreeSitterExcavationSite library

## [0.24.0] - 2026-04-28

### Added

- Add Delphi dependency analysis support (`.pas`, `.dpr` files)

## [0.23.0] - 2026-04-17

### Changed

- Migrate C# dependency analysis to TreeSitterExcavationSite library

## [0.22.0] - 2026-03-31

### Changed

- Migrate Kotlin dependency analysis to TreeSitterExcavationSite library

### Fixed

- Skip implicit wildcard import for package-less Java and Kotlin files

## [0.21.0] - 2026-03-23

### Added

- Collapsible explorer panel with tree navigation
- Search/filter with hide-all in explorer panel
- File size limit, per-file timeout, and configurable exclusions for analysis
- Post-analysis visualization links

### Changed

- Migrate Java dependency analysis to TreeSitterExcavationSite library
- Replace just with mise for task running and runtime management

### Fixed

- Normalize file paths for Windows compatibility
- Flaky Cypress e2e tests
- npm audit vulnerabilities
- Analysis unit tests failing on Windows due to path separator handling

## [0.20.0] - 2026-02-13

### Added

- Hidden Nodes panel: collapsible overlay (bottom-right) listing all currently hidden nodes with sorting, per-entry restore, bulk restore, and keyboard navigation (`H` to toggle)
- Filter feedback edges list by hidden nodes

### Fixed

- Fix unresponsiveness with large graphs
- Fix scroll wheel zoom not working after right-click pan
- Restore node dragging functionality
- Use event.shiftKey for multiselect instead of state
- Prevent UI freeze from re-entrant cytoscape apply
- Prevent header toggle when activating restore-all button via keyboard

### Security

- Update Angular to 20.3.16 for XSS security fix
