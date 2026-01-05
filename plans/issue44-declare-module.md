# Declare Module Support

---
name: declare-module-support
issue: #44
state: complete
version: 1
---

## Goal

Add support for TypeScript `declare module` statements so the parser creates nodes for declared modules and their exported members.

## Tasks

### 1. Write failing tests (TDD - Red phase)
- Test: `declare module "MyModule" { export function foo(): void; }` creates node for `foo` with path including module name
- Test: exported class inside `declare module` creates node
- Test: multiple exports create multiple nodes
- Note: Wildcard modules like `declare module "*.md" {}` should remain ignored (no useful nodes to create)

### 2. Handle ambient module declarations in TypescriptDeclarationsQuery
- Remove filter for `ambient_declaration` when it contains a named module (not wildcard)
- Extract module name from the string literal
- Parse declarations inside the module body
- Prefix child node paths with the module name

### 3. Update Declaration model if needed
- May need to pass module context to `fromExportStatement`/`fromUnexportedDeclaration`

## Steps

- [x] Write failing test for `declare module` with exported function
- [x] Write failing test for `declare module` with exported class
- [x] Write failing test for multiple exports
- [x] Implement ambient module parsing in TypescriptDeclarationsQuery
- [x] Update Declaration to handle `function_signature` type
- [x] Verify wildcard modules still produce no nodes
- [x] Run full test suite

## Notes

- Current filter location: `TypescriptDeclarationsQuery.kt` lines 44, 49
- Tree-sitter node type: `ambient_declaration`
- Wildcard patterns (e.g., `"*.md"`) should continue to be skipped
