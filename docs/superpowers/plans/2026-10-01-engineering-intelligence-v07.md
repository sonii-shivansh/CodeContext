# Engineering Intelligence v0.7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extend CodeContext from deterministic change safety into a reproducible engineering-intelligence loop with architecture drift detection, engineering-context snapshots/diffs, and provenance records.

**Architecture:** Reuse the existing deterministic parser, dependency graph, Architecture Intelligence, evidence, and prepare/verify layers. Add small pure engines for drift/context/provenance so higher-level CLI and MCP surfaces can consume stable machine-readable contracts without making AI authoritative.

**Tech Stack:** Kotlin, kotlinx.serialization, Clikt, JGraphT, Kotest, Gradle, GitHub Actions.

**Spec:** `docs/ENTERPRISE_ROADMAP.md` plus the previous engineering-intelligence direction captured in the project planning document.

## Global Constraints

- Deterministic repository facts remain authoritative; AI remains optional and non-authoritative.
- Machine-readable contracts must be versioned and deterministic.
- Repository paths in evidence must remain repository-relative where applicable.
- Existing Java/Kotlin analysis scope remains unchanged.
- Existing CLI/API behavior must remain backward compatible.
- Verification must run through the existing GitHub Actions clean-environment workflow.

## Review Focus

- Baseline/current architecture results with different finding order must produce the same drift result.
- Empty or incompatible baseline artifacts must fail explicitly rather than silently producing misleading drift.
- Context snapshots from the same repository state must be byte-for-byte deterministic apart from explicitly timestamped provenance metadata.
- Missing Git metadata must degrade to explicit unknown provenance, never fabricated values.
- CLI output paths must not escape the working repository/output boundary.

### Task 1: Architecture Drift Engine

**Files:**
- Create: `src/main/kotlin/com/codecontext/core/intelligence/ArchitectureDrift.kt`
- Create: `src/test/kotlin/com/codecontext/core/intelligence/ArchitectureDriftTest.kt`

**Interfaces:**
- Consumes: `ArchitectureIntelligenceResult`.
- Produces: versioned `ArchitectureDriftResult` and pure `ArchitectureDriftEngine.compare(baseline, current)`.

- [ ] Write failing tests for added/removed findings, new cycles, changed layer counts, deterministic ordering, and identical snapshots producing zero drift.
- [ ] Implement the serializable result/model and deterministic comparison.
- [ ] Run the focused Kotest suite in CI.
- [ ] Commit the task.

### Task 2: Architecture Drift CLI

**Files:**
- Create: `src/main/kotlin/com/codecontext/cli/ArchitectureDriftCommand.kt`
- Modify: `src/main/kotlin/com/codecontext/Main.kt`
- Modify: `docs/API.md`
- Modify: `docs/ARCHITECTURE.md`

**Interfaces:**
- Consumes: repository path and a baseline `ArchitectureIntelligenceResult` JSON file.
- Produces: `architecture-drift` CLI output and `output/architecture-drift.json` when `--json` is supplied.

- [ ] Add CLI integration coverage for baseline/current comparison and invalid baseline handling.
- [ ] Implement the command by reusing the exact architecture analysis pipeline.
- [ ] Register the command and document it.
- [ ] Run CLI/build verification in GitHub Actions.
- [ ] Commit the task.

### Task 3: Engineering Context Snapshot + Diff

**Files:**
- Create: `src/main/kotlin/com/codecontext/core/intelligence/EngineeringContext.kt`
- Create: `src/test/kotlin/com/codecontext/core/intelligence/EngineeringContextTest.kt`
- Create: `src/main/kotlin/com/codecontext/cli/ContextDiffCommand.kt`
- Modify: `src/main/kotlin/com/codecontext/Main.kt`

**Interfaces:**
- Consumes: deterministic analysis/architecture/risk metadata plus repository Git state.
- Produces: versioned context snapshot and deterministic `context-diff` result.

- [ ] Add tests for deterministic snapshot identity, added/removed files, architecture changes, and unknown Git metadata.
- [ ] Implement snapshot and diff models with bounded deterministic fields.
- [ ] Add `context-diff <path> --base <snapshot> --json` CLI.
- [ ] Verify through CI.
- [ ] Commit the task.

### Task 4: Decision Provenance Contract

**Files:**
- Create: `src/main/kotlin/com/codecontext/core/intelligence/DecisionProvenance.kt`
- Create: `src/test/kotlin/com/codecontext/core/intelligence/DecisionProvenanceTest.kt`
- Modify: `src/main/kotlin/com/codecontext/core/workflow/EngineeringPreparation.kt`
- Modify: `src/main/kotlin/com/codecontext/core/workflow/EngineeringVerification.kt`
- Modify: `docs/CHANGE_SAFETY.md`

**Interfaces:**
- Consumes: repository commit, analysis schema/version, evidence IDs, and workflow operation.
- Produces: reproducible provenance metadata attached to prepare/verify artifacts.

- [ ] Add tests for stable provenance IDs, explicit unknown values, and serialization round trips.
- [ ] Implement the contract without changing existing workflow decisions.
- [ ] Verify prepare/verify behavior remains compatible.
- [ ] Commit the task.

### Task 5: Full Verification

**Files:**
- Modify: `.github/workflows/verification.yml`
- Modify: `docs/ENTERPRISE_ROADMAP.md`
- Modify: `CHANGELOG.md`

- [ ] Add end-to-end verification for architecture drift and context diff.
- [ ] Run the complete Verification workflow and inspect artifacts.
- [ ] Run platform verification where applicable.
- [ ] Review the full branch diff against `main`.
- [ ] Commit documentation/CI changes.

### Completion Contract

The branch is complete only when GitHub Actions verifies compilation, all tests, existing self-analysis, existing PR/Architecture intelligence, existing prepare/verify flows, plus the new architecture-drift and context-diff contracts. No feature is described as implemented until the clean-environment workflow passes.
