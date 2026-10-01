# CodeContext Launch Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the current CodeContext implementation internally consistent, reproducibly testable, secure within its documented local-first boundary, and ready for a public developer release.

**Architecture:** Preserve the existing deterministic analysis and intelligence architecture. Harden the release surface around it: one application version, strict CI, clean-room E2E verification, security regression coverage, self-contained reports, cross-platform packaging, and documentation that describes only implemented behavior.

**Tech Stack:** Kotlin/JVM 21, Gradle, Ktor, JUnit/Kotest, GitHub Actions, generated HTML reports.

**Spec:** This plan is the launch-hardening specification agreed in the current session; no future-product roadmap is part of the public documentation.

## Global Constraints

- Java/JVM runtime floor remains 21.
- CodeContext remains local-first; the current REST server is not marketed as a multi-tenant public SaaS API.
- AI remains optional and opt-in.
- Repository-derived evidence must remain bounded and must not expose absolute filesystem paths in generated evidence.
- CI failures must fail CI; no `|| true` on quality gates.
- Release artifacts must be reproducible enough to verify with SHA-256 checksums.

## Review Focus

- Version mismatch between Gradle, CLI/API, release tags, and docs — covered by version contract tests and CI.
- Clean installation and CLI execution — covered by distribution smoke tests.
- Path traversal and unsafe repository paths — covered by server security tests.
- Offline report generation — covered by generated-report asset assertions.
- Cross-platform release integrity — covered by Linux, Windows, and macOS packaging/build jobs where runners are available.

---

### Task 1: Version contract

**Files:**
- Modify: `build.gradle.kts`
- Create: `src/main/kotlin/com/codecontext/core/Version.kt`
- Modify: `src/main/kotlin/com/codecontext/server/CodeContextServer.kt`
- Test: `src/test/kotlin/com/codecontext/core/VersionTest.kt`

- [ ] Write a failing test asserting the application version is a single value exposed through `Version.current` and that the health endpoint reports the same value.
- [ ] Run the focused test and confirm it fails because the version contract does not exist.
- [ ] Implement the minimal version contract and replace the hardcoded server version.
- [ ] Update Gradle project version to the release candidate version `0.6.0`.
- [ ] Run the focused test and the relevant server tests.
- [ ] Commit the task.

### Task 2: Strict CI quality gate

**Files:**
- Modify: `.github/workflows/ci.yml`
- Test: CI workflow execution.

- [ ] Remove the `|| true` quality-gate bypass.
- [ ] Add explicit CLI/version smoke verification after distribution build.
- [ ] Keep Windows verification and ensure failed lint/build/test jobs fail the workflow.
- [ ] Commit the task.

### Task 3: Clean-room E2E fixture and workflow

**Files:**
- Create: `src/test/fixtures/java-sample/...`
- Create: `src/test/fixtures/kotlin-sample/...`
- Create: `scripts/verify-release-smoke.sh`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/e2e.yml`

- [ ] Add minimal deterministic Java/Kotlin fixtures covering dependency, cycle, and changed-file scenarios.
- [ ] Add a release smoke script that exercises `--help`, `--version`, analyze, impact, architecture, PR intelligence, repository QA, and plan where supported by deterministic configuration.
- [ ] Add a clean Ubuntu E2E job that installs the distribution and runs the smoke script against fixtures and the repository itself.
- [ ] Verify the workflow fails when a required artifact/route is absent.
- [ ] Commit the task.

### Task 4: Server security regression suite

**Files:**
- Modify: `src/test/kotlin/com/codecontext/server/...`
- Modify: `src/main/kotlin/com/codecontext/server/CodeContextServer.kt` only where a test proves a required fix.

- [ ] Add failing tests for traversal, remote URL, nonexistent path, oversized question, revision validation, and changed-path limits.
- [ ] Run focused tests and verify correct failures.
- [ ] Implement only the required fixes.
- [ ] Verify rate-limit behavior and non-secret error responses.
- [ ] Commit the task.

### Task 5: Self-contained report assets

**Files:**
- Modify: `src/main/kotlin/com/codecontext/output/ReportGenerator.kt`
- Modify: `docs/DATA_PRIVACY.md`
- Modify: `README.md`
- Test: `src/test/kotlin/com/codecontext/output/...`

- [ ] Add a failing report test asserting generated HTML does not depend on `unpkg.com`.
- [ ] Run it and verify failure against the current CDN-backed implementation.
- [ ] Bundle the required force-graph browser asset into the generated report or use an existing vendored project asset without introducing runtime network dependency.
- [ ] Verify generated reports are usable offline and update privacy documentation accordingly.
- [ ] Commit the task.

### Task 6: Release workflow hardening

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `SECURITY.md`
- Modify: `CHANGELOG.md`
- Modify: release documentation as needed.

- [ ] Make release workflow verify version consistency before publishing.
- [ ] Add macOS x64 and arm64 distribution jobs where practical; retain Linux and Windows artifacts.
- [ ] Add release smoke verification and SHA-256 manifest validation.
- [ ] Move implemented current changes into a `0.6.0` release entry and make support policy match the current release.
- [ ] Commit the task.

### Task 7: Documentation truth audit

**Files:**
- Modify: `README.md`
- Modify: `SECURITY.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/DATA_PRIVACY.md`
- Modify: current API/architecture/development documentation where stale.

- [ ] Search for stale versions, unsupported capabilities, and outdated commands.
- [ ] Correct only claims that conflict with current implementation.
- [ ] Ensure public docs clearly distinguish local-first operation from future deployment concerns.
- [ ] Commit the task.

### Task 8: Full verification and release candidate

**Files:**
- Modify only if verification exposes a defect.

- [ ] Run the full Gradle test suite.
- [ ] Run static analysis/lint.
- [ ] Run distribution build and installed CLI smoke tests.
- [ ] Run the complete E2E workflow on clean GitHub runners.
- [ ] Run security regression tests.
- [ ] Verify release artifacts and checksums.
- [ ] Inspect all CI logs and artifacts before claiming completion.
- [ ] Create the release PR only after every gate is green.

---
