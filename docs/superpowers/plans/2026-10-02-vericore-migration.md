# Vericore Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename CodeContext to Vericore while preserving the PR #90 known-good behavior and introducing only deliberate, tested compatibility paths.

**Architecture:** Execute the migration in gated phases on `feat/vericore-migration`, never directly on `main`. Each phase changes one identity boundary, runs focused regression tests plus the relevant existing suite, and stops on failure. The final phase compares the live Vericore command surface against the PR #90 baseline and validates CLI, MCP, REST, packaging, repository integrity, and release gates.

**Tech Stack:** Kotlin/JVM, Gradle, JGit, Clikt/CLI, MCP, REST server, GitHub Actions, existing CodeContext live/release audit infrastructure.

**Spec:** `docs/VERICORE-MIGRATION-SPEC.md`

## Global Constraints

- Known-good baseline: `390bfcf314f4e8ae749eb968d642d4594a3a3e18`.
- Canonical product identity is `Vericore`.
- Canonical CLI executable is `vericore`.
- Canonical Kotlin package root is `com.vericore`.
- Canonical configuration/environment/generated-artifact namespaces use `vericore`/`VERICORE`.
- Preserve behavior and contracts unless a rename explicitly requires an identity change.
- Retain legacy CodeContext compatibility only when necessary to avoid avoidable breakage; mark it deprecated and provide migration guidance.
- Do not rewrite Git history.
- Do not publish v0.7.0 until all migration gates pass.
- `main` remains the recovery point throughout migration development.

## Review Focus

- Package migration must leave no accidental `com.codecontext` runtime dependencies — covered by Phase 1 compile/test and repository scans.
- CLI rename must not change command semantics or user-visible output beyond canonical identity — covered by Phase 2 command audit.
- Generated Vericore artifacts must not falsely mark repositories dirty — covered by Phase 3 repository-state tests and live audit.
- MCP/REST contracts must remain behaviorally stable — covered by Phase 4 protocol/API checks.
- Distribution/release workflows must not ship mixed CodeContext/Vericore identities — covered by Phase 5 packaging and release audit.

---

### Phase 0: Baseline and Migration Contract

**Files:**
- Create: `docs/VERICORE-MIGRATION-SPEC.md`
- Create: `docs/superpowers/plans/2026-10-02-vericore-migration.md`

- [ ] Create migration branch from `390bfcf...`.
- [ ] Record the canonical identity, invariants, compatibility rules, and phase gates in the migration spec.
- [ ] Record this implementation plan alongside the spec.
- [ ] Verify branch starts from the known-good main commit and contains no source-code changes.
- [ ] Gate: inspect branch commit/tree and confirm only migration documentation was added.
- [ ] Commit the phase-0 contract.

### Phase 1: Core Namespace Migration

**Files:**
- Modify: `src/main/kotlin/com/codecontext/**`
- Modify: `src/test/kotlin/com/codecontext/**`
- Modify: all source imports/package declarations referencing `com.codecontext`
- Modify: Gradle/application main-class configuration as required by the package move

- [ ] Add/adjust a regression guard that the application starts from `com.vericore.MainKt`.
- [ ] Rename package declarations and imports from `com.codecontext` to `com.vericore`.
- [ ] Move source/test package directories to match the canonical namespace.
- [ ] Update Gradle group/main-class metadata without changing application behavior.
- [ ] Run focused compile/package tests.
- [ ] Run the full unit/integration test suite.
- [ ] Scan tracked source/configuration for unintended `com.codecontext` runtime references.
- [ ] Gate: all tests green and no accidental legacy runtime package remains.
- [ ] Commit Phase 1.

### Phase 2: CLI Identity

**Files:**
- Modify: CLI root/application command definitions
- Modify: CLI tests and help/output fixtures
- Modify: scripts/examples invoking `codecontext`
- Modify: distribution configuration for executable naming

- [ ] Add tests for `vericore --version` and `vericore --help`.
- [ ] Rename canonical executable/root command to `vericore`.
- [ ] Update command examples and user-visible branding.
- [ ] Preserve all existing command names and semantics (`analyze`, `architecture`, `impact`, `architecture-drift`, `architecture-contract`, `context-snapshot`, `context-diff`, `reality`, `evolution`, `pr-intelligence`, `repo-qa`, `plan`, `prepare`, `verify`, `setup`, `ask`, etc.).
- [ ] Add a controlled legacy `codecontext` compatibility path only if existing users would otherwise be unnecessarily broken; emit a migration warning.
- [ ] Run complete CLI command tests and help audit.
- [ ] Gate: every command remains functional and user-visible output is consistent except for intentional identity changes.
- [ ] Commit Phase 2.

### Phase 3: Configuration and Generated State

**Files:**
- Modify: configuration loaders/templates
- Modify: repository-state/generated-artifact classification
- Modify: architecture-contract persistence
- Modify: environment-variable handling
- Modify: related tests/fixtures

- [x] Add failing tests for canonical `.vericore*` and `VERICORE_*` behavior.
- [x] Implement canonical Vericore configuration namespace.
- [x] Rename generated architecture-contract artifact to `.vericore-architecture-contract.json` and template.
- [x] Update generated-artifact filtering so Vericore-generated files never create false dirty state.
- [x] If legacy `.codecontext*`/`CODECONTEXT_*` compatibility is retained, mark it deprecated and test migration behavior.
- [x] Run repository-state and configuration tests.
- [x] Run live `context-snapshot`/`context-diff` checks against a clean repository.
- [x] Gate: generated Vericore artifacts produce `Dirty: false` and real user changes remain detectable.
- [x] Commit Phase 3.

### Phase 4: REST and MCP Identity

**Files:**
- Modify: server classes/internal package names
- Modify: MCP implementation/tool registration
- Modify: REST/MCP tests and metadata

- [ ] Rename internal server/MCP identities to Vericore.
- [ ] Preserve endpoint paths and response semantics unless an explicit identity surface requires a change.
- [ ] Preserve MCP protocol version, tool schemas, tool behavior, and error semantics.
- [ ] Update server startup/help/metadata branding.
- [ ] Run REST and MCP integration tests.
- [ ] Run live REST/MCP audit against the migration branch.
- [ ] Gate: all protocol/API checks pass with no unintended behavioral drift.
- [ ] Commit Phase 4.

### Phase 5: CI/CD, Distribution, and Release Identity

**Files:**
- Modify: `.github/workflows/**`
- Modify: Gradle packaging/distribution configuration
- Modify: release scripts and packaging assertions
- Modify: smoke/release audit workflows

- [ ] Rename install/distribution identity from `codecontext` to `vericore`.
- [ ] Rename release artifacts from `codecontext-*` to `vericore-*`.
- [ ] Update workflow assertions, cache paths, executable paths, and artifact paths.
- [ ] Update release version/identity checks without changing version semantics.
- [ ] Run platform smoke and distribution builds.
- [ ] Run complete live repository release gate.
- [ ] Gate: all distribution artifacts are internally consistent and contain no accidental CodeContext executable identity.
- [ ] Commit Phase 5.

### Phase 6: Documentation and Brand Surface

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md` current release section
- Modify: docs/examples/contributing/security/architecture/API/MCP documentation
- Modify: repository metadata references where appropriate

- [ ] Replace current product branding with the canonical Vericore identity.
- [ ] Apply exact positioning: `Vericore — Understand. Change. Verify.`
- [ ] Apply exact description: `Evidence-grounded engineering intelligence for your codebase.`
- [ ] Update commands/examples to `vericore`.
- [ ] Preserve historical release references and migration notes where they are factual history.
- [ ] Scan documentation for accidental mixed branding.
- [ ] Gate: repository documentation presents one consistent canonical identity.
- [ ] Commit Phase 6.

### Phase 7: Full Migration Verification

**Files:**
- Modify: audit fixtures/scripts only if required by verified migration behavior

- [ ] Run full unit/integration test suite.
- [ ] Run complete CLI command-output audit.
- [ ] Run MCP audit.
- [ ] Run REST audit.
- [ ] Run repository-integrity and mutation-safety audit.
- [ ] Run deterministic release audit.
- [ ] Run platform smoke/distribution checks.
- [ ] Compare key outputs against the PR #90 baseline, allowing only deliberate identity differences.
- [ ] Scan repository for unintended canonical CodeContext references.
- [ ] Scan compatibility paths to ensure every retained legacy surface is intentional and documented.
- [ ] Gate: all checks pass; no release-blocking output or compatibility defect remains.
- [ ] Commit Phase 7.

### Phase 8: External Repository Identity and Release

**Files:**
- Repository metadata and external links as supported
- Release configuration

- [ ] Confirm Phase 7 is fully green before changing external identity.
- [ ] Rename repository/website references only after software identity is proven stable.
- [ ] Verify redirects/links and release metadata.
- [ ] Tag and publish the Vericore release only after final main-branch validation.
- [ ] Run post-release smoke verification.
- [ ] Gate: published release, package names, CLI identity, documentation, and repository identity are consistent.
