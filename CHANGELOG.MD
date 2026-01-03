# Changelog

## 1.1
---
### 1.2.0
- Package structure change, `fallen_lib` -> `fallen.lib`
- Introduce standard method inserter.
- Introduce patch util.
### 1.1.2
- Fixed entry targets don't include nest members.
- Improved logger behaviors, prevent spamming in log.
- API change, `IFallenPatchContext#getAppliedPatches` -> `IFallenPatchContext#currentClassAppliedPatches`
- Fixed a null point exception when resource is not found.
### 1.1.1
- Added debug infos in patch processing procedure.
- Changed `copyOf()` to unmodifiable version.
- Added context for ctor and patch method `apply()`
- Fixed entries sorting.
- Added an empty check before traversing classes.
- Filtered `minecraft` and `forge` class targets for now.
### 1.1.0
- Reimplemented fallen patch as a configurable API.

## 1.0
---
### 1.0.2
- Aligned mod id and mod main class.
### 1.0.1
- Fixed a `String index out of range` problem when applying transformer.
### 1.0.0
- Initialized.