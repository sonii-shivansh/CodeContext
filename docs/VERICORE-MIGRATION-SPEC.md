# Vericore Migration Specification

## Status

Migration implementation and validation complete; pending merge of PR #99 and the subsequent Vericore release publication.

## Goal

Rename CodeContext to **Vericore** while preserving the known-good behavior of the PR #90 merged baseline (`390bfcf314f4e8ae749eb968d642d4594a3a3e18`).

## Product Identity

**Vericore**  
**Understand. Change. Verify.**  
**Evidence-grounded engineering intelligence for your codebase.**

## Migration Invariant

The migration changes identity, naming, packaging, and user-facing branding. It must not intentionally change engineering behavior, safety guarantees, deterministic analysis semantics, REST behavior, MCP behavior, or release-gate semantics unless a compatibility requirement explicitly requires it.

The PR #90 merged commit is the known-good behavioral baseline.

## Canonical Identity

- Product: `Vericore`
- CLI executable: `vericore`
- Kotlin package root: `com.vericore`
- Configuration namespace: `.vericore*`
- Environment namespace: `VERICORE_*`
- Generated architecture-contract artifact: `.vericore-architecture-contract.json`
- Distribution/install identity: `vericore`
- Release artifact prefix: `vericore-*`

## Legacy Compatibility

CodeContext is not a second canonical identity. Compatibility is retained only where removing it would unnecessarily break existing users, scripts, configurations, or integrations.

Every retained legacy surface must be:

1. explicitly identified as legacy/deprecated;
2. non-fatal where migration is possible;
3. accompanied by a migration path to Vericore;
4. covered by regression tests.

Historical changelog and Git history references are not rewritten merely to erase history.

## Behavior Preservation

The following must remain behaviorally equivalent to the PR #90 baseline unless explicitly required by the rename:

- command semantics and command set;
- analysis, architecture, impact, reality, evolution, repository QA, planning, preparation, and verification semantics;
- repository-state and generated-artifact handling;
- MCP protocol and tool behavior;
- REST endpoint behavior and response contracts;
- provider error handling and graceful AI-provider degradation;
- deterministic release gates;
- repository-integrity protections;
- cross-platform packaging and smoke tests.

## Migration Surfaces

The migration must account for:

- Kotlin/Java package declarations and imports;
- source/test directory structure;
- Gradle group, application main class, application name, and packaging;
- CLI root command, executable, help text, examples, and tests;
- configuration files/templates and environment variables;
- generated artifacts and repository dirty-state classification;
- REST/server internal identity without unnecessary API breaking changes;
- MCP internal identity without unnecessary tool/protocol breaking changes;
- CI workflows, scripts, release packaging, artifact names, and smoke tests;
- documentation and examples;
- current product branding and URLs;
- compatibility/migration warnings.

## Non-Goals

- Rewriting Git history.
- Changing product behavior merely because a component can be renamed.
- Introducing a second permanent CodeContext/Vericore operating mode.
- Deleting historical references solely for cosmetic reasons.
- Publishing a Vericore release before all migration gates pass.

## Phase Gates

No phase may start until the preceding phase has:

1. implemented its scoped changes;
2. passed its targeted tests;
3. passed relevant existing tests;
4. passed applicable live/release checks;
5. been inspected for user-visible regressions;
6. been committed on the migration branch.

If a gate fails, work stops in that phase until the failure is understood and fixed.

## Recovery Point

Known-good main baseline:

`390bfcf314f4e8ae749eb968d642d4594a3a3e18`

`main` must remain untouched while migration work is developed and validated.
