# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
