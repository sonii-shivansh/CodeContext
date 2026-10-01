# PR Intelligence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a deterministic PR Intelligence layer that turns a local Git diff into an explainable engineering assessment reusable by CLI, REST, CI, and future GitHub/AI integrations.

**Architecture:** Keep Git transport separate from the core. `GitChangeSetBuilder` produces a normalized `ChangeSet`; `PRIntelligenceEngine` combines Phase 2 impact analysis, existing engineering risk, package boundaries, test candidates, and diff-size signals into a versioned deterministic result. CLI and REST are thin adapters.

**Tech Stack:** Kotlin 2.1, JGit 6.8, JGraphT, kotlinx.serialization, Clikt 4.2, Ktor 2.3, Kotlin/JUnit/Kotest.

**Spec:** `docs/superpowers/specs/2026-10-01-pr-intelligence-design.md`

## Global Constraints

- Deterministic analysis is the source of truth; no mandatory LLM dependency.
- Core intelligence must not depend on Ktor or GitHub API clients.
- Do not accept arbitrary remote repository URLs in the REST endpoint.
- Do not expose absolute filesystem paths in API responses.
- Preserve changed/deleted/renamed records even when source analysis cannot resolve them.
- Stable ordering is required for all serialized collections.
- CI is the authoritative execution environment and must run the full acceptance pipeline.

## Review Focus

- Deleted/renamed files must remain visible in the ChangeSet and produce explicit unresolved evidence rather than disappearing.
- Empty diffs must return a valid deterministic result with zero findings.
- Invalid revisions must fail clearly without a fabricated assessment.
- Large synthetic graphs must remain bounded and deterministic.
- REST input must reuse existing path validation and rate limiting and must not become a remote-fetch primitive.

### Task 1: Domain model and deterministic engine

**Files:**
- Create: `src/main/kotlin/com/codecontext/core/intelligence/PRIntelligence.kt`
- Create: `src/test/kotlin/com/codecontext/core/intelligence/PRIntelligenceEngineTest.kt`

**Interfaces:**
- `ChangeType`, `ChangedFile`, `ChangeSet`, `FindingSeverity`, `PRFinding`, `ChangeSummary`, `PRIntelligenceResult`.
- `PRIntelligenceEngine.analyze(changeSet, impact, risks, packageByPath, testCandidates)` returns `PRIntelligenceResult`.

- [ ] Write tests for empty diff, deleted/unresolved files, high-impact cross-package change, test candidates, deterministic ordering, and size findings.
- [ ] Implement serializable domain models with schema version `1.0` and stable collection ordering.
- [ ] Implement evidence-based severity rules without a single opaque score.
- [ ] Run the focused tests in CI and commit.

### Task 2: Git ChangeSet adapter

**Files:**
- Create: `src/main/kotlin/com/codecontext/core/intelligence/GitChangeSetBuilder.kt`
- Create: `src/test/kotlin/com/codecontext/core/intelligence/GitChangeSetBuilderTest.kt`

**Interfaces:**
- `GitChangeSetBuilder.fromWorkingTree(repoPath)`.
- `GitChangeSetBuilder.fromRevisions(repoPath, baseRevision, headRevision)`.

- [ ] Add fixture/repository tests for added, modified, deleted, renamed, empty, and invalid revision cases.
- [ ] Use JGit tree/diff APIs; do not shell out to Git.
- [ ] Normalize paths to repository-relative `/` paths and compute added/deleted line counts from diff edits.
- [ ] Preserve rename/copy metadata when available.
- [ ] Run focused tests and commit.

### Task 3: CLI adapter

**Files:**
- Create: `src/main/kotlin/com/codecontext/cli/PRIntelligenceCommand.kt`
- Modify: `src/main/kotlin/com/codecontext/Main.kt`
- Modify: `docs/API.md`
- Test: `src/test/kotlin/com/codecontext/cli/PRIntelligenceCommandTest.kt`

**Interfaces:**
- CLI: `pr-intelligence <path> [--base <rev> --head <rev>] [--json]`.

- [ ] Add command tests for working-tree mode, revision mode, empty diff, and invalid revision.
- [ ] Reuse scanner/parser/graph/Git analyzer/Phase 2 impact engine and risk engine.
- [ ] Emit `output/pr-intelligence.json` when `--json` is requested and keep human output derived from the same result.
- [ ] Never emit absolute repository paths in the machine-readable report.
- [ ] Run CLI smoke test and commit.

### Task 4: REST adapter

**Files:**
- Modify: `src/main/kotlin/com/codecontext/server/CodeContextServer.kt`
- Create: `src/test/kotlin/com/codecontext/server/PRIntelligenceRouteTest.kt`
- Modify: `docs/API.md`

**Interfaces:**
- `POST /pr-intelligence` with `{ "repoPath": "...", "baseRevision": "...", "headRevision": "..." }`; revisions are optional only when analyzing the working tree.

- [ ] Test valid local analysis, invalid path, invalid revision, empty diff, and oversized input.
- [ ] Reuse `sanitizePath`, `ConfigLoader`, existing rate limiter, and sanitized `ApiError` responses.
- [ ] Return only repository-relative paths in the response.
- [ ] Reject HTTP(S) repo paths and never clone/fetch remote repositories.
- [ ] Run server integration tests and commit.

### Task 5: CI end-to-end verification

**Files:**
- Modify: `.github/workflows/verification.yml`
- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/ENTERPRISE_ROADMAP.md`

- [ ] Add a deterministic fixture or self-diff PR Intelligence smoke test.
- [ ] Validate `output/pr-intelligence.json` schema and severity values.
- [ ] Exercise REST `/pr-intelligence` after server startup.
- [ ] Run full unit/property tests, build, distribution, CLI, self-analysis, PR Intelligence validation, REST health/API checks, and existing CodeQL/quality workflows.
- [ ] Review the complete branch diff for determinism, path leakage, and accidental network behavior.
- [ ] Create a PR and only merge after all required checks pass.

## Final Verification

Run through GitHub Actions: `./gradlew --no-daemon clean test`, `./gradlew --no-daemon build installDist`, CLI PR Intelligence smoke test, CodeContext self-analysis, JSON validation, REST `/pr-intelligence`, and existing CodeQL/quality checks. The branch is complete only when all required checks are green.
