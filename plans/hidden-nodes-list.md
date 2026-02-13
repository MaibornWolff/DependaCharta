# Hidden Nodes List

---
name: hidden-nodes-list
issue:
state: complete
version: 1
---

## Goal

Add a collapsible overlay panel (bottom-right) that lists all currently hidden nodes, with sorting, per-entry restore, bulk restore (hidden only), and keyboard navigation. Mirrors the feedback edges list pattern.

## Steps

- [x] Complete Task 1: `RestoreAllHiddenNodes` action + reducer + tests
- [x] Complete Task 2: `State.getHiddenNodes()` + tests
- [x] Complete Task 3: `HiddenNodesListComponent` (ts, html, css)
- [x] Complete Task 4: Component tests (spec)
- [x] Complete Task 5: Integrate in `AppComponent`
- [x] Complete Task 6: Update help popup
- [x] Complete Task 7: Update CHANGELOG
- [x] Run `just test-frontend` — all green
