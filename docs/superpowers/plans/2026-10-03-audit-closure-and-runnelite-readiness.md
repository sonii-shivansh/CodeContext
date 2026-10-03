# Audit Closure and RuneLite Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining material findings from the original Vericore audit, establish real RuneLite compatibility/E2E coverage, and continuously update the plan with verified implementation and GitHub Actions evidence.

**Architecture:** Preserve Vericore's deterministic-first architecture. Fix correctness at the owning boundary rather than adding workarounds in downstream commands. Treat repository/commit identity, semantic evidence, Engineering Reality, and change-safety contracts as authoritative; use RuneLite as a real large Gradle/Java fixture rather than a synthetic approximation.

**Tech Stack:** Kotlin/JVM, Gradle, JUnit, Vericore CLI, GitHub Actions, MCP, Git history analysis, semantic evidence graph, Engineering Reality, real Git repositories.

**Spec:** `audit.pdf` supplied with the project conversation, together with the current `main` implementation and its existing regression/release-gate workflows.

## Global Constraints

- GitHub Actions is the authoritative execution environment; no local-laptop execution is assumed.
- Every implementation task must add or update deterministic regression coverage before being considered complete.
- Every PR must pass DCO and the complete applicable GitHub Actions matrix before merge.
- After every merge, validate the exact resulting `main` SHA; a green PR is not sufficient evidence of a green post-merge mainline.
- Do not claim RuneLite compatibility until Vericore has actually cloned/analyzed the real RuneLite repository in GitHub Actions and its artifacts have been audited.
- Preserve backward compatibility unless a deliberate breaking change is documented and tested.
- Do not weaken safety checks merely to make fixtures pass.
- Deterministic evidence must remain the source of truth; AI output must not manufacture repository facts.
- Update this plan after each completed task with the PR/commit, tests, observed failures, fixes, and remaining work.
- Keep architecture classifications explicitly heuristic unless the implementation provides compiler-grade proof.

## Review Focus

1. **Real large Gradle repository behavior:** RuneLite must be cloned and exercised end-to-end, not represented by a fixture that only resembles it. Test and audit command outputs and generated artifacts.
2. **Java 11 detection in nonstandard Gradle files:** `common.settings.gradle.kts` and equivalent repository-specific build conventions must be detected without hard-coding RuneLite as a special case.
3. **Change-safety semantics:** `affectedComponents` is analysis context, while the explicit allowed-change boundary must be unambiguous; an empty allowed-path list must never silently broaden to affected components.
4. **Large-history performance:** benchmark Git analysis separately from parsing/graph construction and capture runtime/resource data for shallow/full and merge-heavy histories.
5. **Protocol compatibility:** MCP version negotiation and transport behavior must be tested against the current supported protocol while retaining the existing supported revision where required.

---

## Phase 0: Baseline and plan instrumentation

### Task 0.1: Establish immutable audit-closure baseline

**Files:**
- Read: `docs/ENTERPRISE_ROADMAP.md`
- Read: existing `.github/workflows/*.yml` release/regression workflows
- Modify: `docs/superpowers/plans/2026-10-03-audit-closure-and-runnelite-readiness.md`

**Interfaces:**
- Consumes: current `main` at `9adb752ef2e2ecc3614f13abf7c36688c7901e91`.
- Produces: a tracked baseline section containing current commit, known audit findings, and the validation workflow names used for every subsequent task.

- [ ] **Step 1: Record current baseline**
  - Record exact `main` SHA.
  - Record current green/neutral/failed workflow state.
  - Record current open PR/issue state relevant to the audit.

- [ ] **Step 2: Map each audit finding to an explicit task**
  - Mark each finding as `OPEN`, `IN PROGRESS`, `VERIFIED`, or `WONT FIX / INTENTIONAL`.
  - Link the owning task below.

- [ ] **Step 3: Commit the plan baseline**
  - Commit only the plan document.
  - Open a plan PR and verify DCO/CI before treating the plan as repository state.

---

## Phase A: RuneLite compatibility and real E2E

### Task A1: Generalize Java language-level detection for Gradle convention files

**Files:**
- Modify: `src/main/kotlin/com/vericore/core/analysis/JavaLanguageLevelDetector.kt`
- Test: existing detector tests plus a new Gradle convention-file regression fixture/test

**Interfaces:**
- Consumes: repository root and existing Java language-level detection API.
- Produces: correct detection for Java release/toolchain declarations in conventional Gradle files, including `common.settings.gradle.kts`, without repository-specific filename exceptions.

- [ ] **Step 1: Write failing tests**
  - Add a fixture containing `common.settings.gradle.kts` with `options.release = 11`.
  - Assert detected language level is Java 11.
  - Add at least one equivalent declaration form already supported to prove no regression.

- [ ] **Step 2: Run the focused GitHub Actions test and verify failure**
  - Expected: the new convention-file case fails before implementation.

- [ ] **Step 3: Implement generalized convention-file scanning**
  - Discover relevant Gradle build/convention/settings files without embedding `RuneLite` as a special case.
  - Preserve existing Maven/Gradle/SDKMAN detection precedence and semantics.

- [ ] **Step 4: Add negative/conflicting-declaration tests**
  - Verify deterministic precedence when multiple files specify compatible values.
  - Verify conflicting values produce the existing documented behavior rather than silently choosing an arbitrary value.

- [ ] **Step 5: Run focused + full regression tests in GitHub Actions**
  - Expected: all detector and affected release-gate tests pass.

- [ ] **Step 6: Update plan**
  - Record exact PR/commit and the observed test output.

---

### Task A2: Add RuneLite as a real GitHub Actions integration fixture

**Files:**
- Create/modify: `.github/workflows/` RuneLite integration workflow or existing live-E2E workflow
- Modify: test/release-gate support code as required
- Modify: `docs/superpowers/plans/2026-10-03-audit-closure-and-runnelite-readiness.md`

**Interfaces:**
- Consumes: the published/buildable Vericore distribution and the public RuneLite repository.
- Produces: reproducible CI evidence proving Vericore can clone and analyze the real RuneLite repository.

- [ ] **Step 1: Define bounded RuneLite checkout strategy**
  - Pin a known RuneLite commit/ref for reproducibility.
  - Do not mutate RuneLite.
  - Capture repository identity and observed commit in test artifacts.

- [ ] **Step 2: Write failing smoke assertions**
  - Clone RuneLite.
  - Run Vericore against it.
  - Assert expected repository identity, Java level, file/module discovery, and successful core analysis.

- [ ] **Step 3: Add artifact capture**
  - Preserve command stdout/stderr and generated JSON artifacts for audit inspection.
  - Preserve enough metadata to distinguish infrastructure failure from Vericore failure.

- [ ] **Step 4: Run the real RuneLite smoke workflow**
  - Expected initial failure if any unsupported behavior remains.
  - Diagnose failures from actual logs; do not infer from fixture behavior.

- [ ] **Step 5: Fix only verified compatibility defects**
  - Each fix must receive a focused regression test.

- [ ] **Step 6: Re-run RuneLite smoke and existing release gates**
  - Expected: RuneLite smoke passes and existing suite remains green.

- [ ] **Step 7: Update plan with artifact references and exact RuneLite ref**

---

### Task A3: Build full RuneLite command-surface E2E audit

**Files:**
- Modify: live E2E/release-gate workflow(s)
- Create/modify: RuneLite E2E test harness
- Modify: plan document

**Interfaces:**
- Consumes: RuneLite checkout from Task A2.
- Produces: audited outputs for the supported command surface, with pass/fail classification per command.

- [ ] **Step 1: Define the supported command matrix**
  - Include repository analysis, architecture, impact, evolution, Repo-QA, Engineering Reality, semantic evidence graph, planning/preparation/verification where applicable, MCP/REST smoke where repository context is accepted, and any other canonical commands documented by the current CLI.

- [ ] **Step 2: Execute commands against RuneLite**
  - Record exit code, stdout, stderr, output path, output schema, and repository/commit identity.

- [ ] **Step 3: Add semantic artifact assertions**
  - Assert non-empty and internally consistent evidence where RuneLite should produce evidence.
  - Assert no generated artifact claims a repository identity or commit that does not match RuneLite.

- [ ] **Step 4: Audit failures individually**
  - Classify as product bug, unsupported repository shape, infrastructure/network failure, or expected signal.
  - Do not hide unsupported behavior behind fixture-specific skips.

- [ ] **Step 5: Add regression coverage for each real defect found**

- [ ] **Step 6: Re-run the complete RuneLite matrix and existing release gate**

- [ ] **Step 7: Update plan with command-by-command status**

---

## Phase B: Change-safety contract hardening

### Task B1: Separate affected components from explicit allowed change paths

**Files:**
- Modify: `EngineeringPlan` model file containing `plannedPaths`
- Modify: `EngineeringVerification` / change-safety implementation containing the `plannedPaths.ifEmpty { affectedComponents }` fallback
- Modify: MCP change-safety integration using the same fallback
- Test: existing Phase 4 regression suite plus new negative contract tests

**Interfaces:**
- Consumes: existing `EngineeringPlan`, `affectedComponents`, and change-safety APIs.
- Produces: explicit allowed-change-path semantics that cannot be broadened by empty planned paths.

- [ ] **Step 1: Write failing negative tests**
  - Create a plan with `affectedComponents` populated and `plannedPaths` empty.
  - Attempt a change under an affected component that is not explicitly allowed.
  - Assert rejection.
  - Assert a valid explicit path is accepted.

- [ ] **Step 2: Introduce an explicit safety-boundary field or equivalent API**
  - Prefer a semantically named allowed-path concept over overloading `affectedComponents`.
  - Preserve serialization compatibility where practical.

- [ ] **Step 3: Remove the unsafe fallback**
  - Verification and MCP must consume only the explicit safety boundary.

- [ ] **Step 4: Add tamper tests**
  - Missing safety boundary.
  - Empty safety boundary.
  - Boundary modified after plan generation.
  - Path outside boundary.
  - Repository/commit mismatch.

- [ ] **Step 5: Run Phase 4, contract-tamper, MCP, and release-gate tests**

- [ ] **Step 6: Update plan**

---

## Phase C: Repository output-path consistency

### Task C1: Audit and eliminate remaining process-CWD output paths

**Files:**
- Search all `src/` and relevant scripts/workflows for `File("output/`, `Paths.get("output/`, `./output/`, and equivalent CWD-relative writes.
- Modify every confirmed repository-scoped writer.
- Add/update regression tests.

**Interfaces:**
- Consumes: target repository root.
- Produces: repository-scoped output paths under `<target-repository>/output/` for repository commands.

- [ ] **Step 1: Write a static audit/regression check**
  - Enumerate known unsafe output constructions.
  - Allow documented process-level outputs only where they are intentionally not repository-scoped.

- [ ] **Step 2: Fix the remaining `EngineeringContextCommand` context-diff path and any other confirmed violations**

- [ ] **Step 3: Add multi-CWD regression coverage**
  - Execute from a working directory different from the target repository.
  - Assert artifacts appear only under the target repository.

- [ ] **Step 4: Run full CLI/release-gate validation**

- [ ] **Step 5: Update plan**

---

## Phase D: Git-history scalability evidence

### Task D1: Build deterministic history benchmark fixtures

**Files:**
- Create: benchmark/test fixture generator or existing benchmark module location
- Modify: Git analysis test/benchmark infrastructure
- Modify: GitHub Actions workflow for bounded performance jobs

**Interfaces:**
- Consumes: repositories/history fixtures with controlled commit counts and merge patterns.
- Produces: repeatable timing/resource measurements without changing correctness behavior.

- [ ] **Step 1: Define benchmark matrix**
  - 1,000 commits.
  - 5,000 commits.
  - Shallow clone.
  - Full clone.
  - Merge-heavy history.

- [ ] **Step 2: Define measured stages**
  - repository scan
  - source parsing
  - Git history loading
  - graph construction
  - total command runtime
  - available memory/peak process resource metric supported by CI

- [ ] **Step 3: Run baseline benchmark in GitHub Actions**
  - Record raw results as CI artifacts.

- [ ] **Step 4: Analyze hotspots**
  - Only optimize code after measurements identify a material bottleneck.

- [ ] **Step 5: Add bounded safeguards if required**
  - Preserve deterministic output.
  - Preserve `gitCommitLimit` semantics.
  - Avoid silently truncating history beyond the documented limit.

- [ ] **Step 6: Add regression thresholds**
  - Use broad CI-safe thresholds to catch major regressions without making the suite flaky.

- [ ] **Step 7: Update plan with measured baseline and final thresholds**

---

## Phase E: MCP protocol modernization

### Task E1: Inventory current MCP implementation and supported protocol behavior

**Files:**
- Read/modify MCP server implementation
- Read MCP-related tests and workflows
- Modify plan

**Interfaces:**
- Consumes: current MCP transport/message implementation.
- Produces: a versioned compatibility matrix and implementation target based on the current official MCP specification at execution time.

- [ ] **Step 1: Verify the current official MCP specification and SDK requirements**
  - Record the exact specification revision used for implementation.
  - Do not hard-code a version from this plan without verification.

- [ ] **Step 2: Document current behavior**
  - Initialization/version negotiation.
  - tools/list.
  - tools/call.
  - ping.
  - error semantics.
  - transport/session behavior.

- [ ] **Step 3: Add failing compatibility tests for the required modern behavior**

- [ ] **Step 4: Implement negotiated compatibility**
  - Retain the currently supported revision where feasible.
  - Reject unsupported versions explicitly and deterministically.

- [ ] **Step 5: Add protocol-level regression tests**
  - Supported modern client.
  - Older supported client.
  - Unsupported version.
  - malformed initialize.
  - malformed tool call.
  - repeated/invalid lifecycle messages.

- [ ] **Step 6: Run MCP + REST + release-gate validation**

- [ ] **Step 7: Update plan with exact supported version matrix**

---

## Phase F: Architecture semantics and audit wording

### Task F1: Make heuristic architecture signals explicit and auditable

**Files:**
- Modify architecture intelligence documentation/output schema as needed
- Modify architecture tests
- Modify README/architecture docs where wording currently implies proof

**Interfaces:**
- Consumes: current architecture signals/contracts/drift findings.
- Produces: explicit distinction between heuristic signals, deterministic structural facts, and proven contract violations.

- [ ] **Step 1: Inventory architecture claims exposed in CLI/JSON/AI grounding**

- [ ] **Step 2: Identify wording that overstates heuristic evidence**

- [ ] **Step 3: Add schema-level or documentation-level evidence classification**

- [ ] **Step 4: Add regression assertions preventing heuristic findings from being represented as proven violations**

- [ ] **Step 5: Run architecture, AI grounding, evidence graph, and release-gate tests**

- [ ] **Step 6: Update plan**

---

## Phase G: Final audit closure and release-quality acceptance

### Task G1: Re-run the original audit against current main

**Files:**
- Modify: this plan only, plus any audit report artifact chosen by the project

**Interfaces:**
- Consumes: all outputs from Tasks A–F and the original audit findings.
- Produces: a finding-by-finding closure report with evidence.

- [ ] **Step 1: Reproduce the original audit command matrix**

- [ ] **Step 2: Re-run real RuneLite E2E**

- [ ] **Step 3: Inspect every generated artifact and relevant log**
  - Do not rely solely on exit status.
  - Check repository identity, commit identity, counts, paths, provenance, confidence/grounding, and schema consistency.

- [ ] **Step 4: Reconcile every original finding**
  - `VERIFIED FIXED`
  - `VERIFIED INTENTIONAL`
  - `REMAINING`
  - `NEW REGRESSION`

- [ ] **Step 5: Require zero unresolved material findings**
  - Any exception must have explicit evidence and a documented rationale.

- [ ] **Step 6: Run final platform/release matrix**
  - Linux
  - Windows
  - macOS x64
  - macOS ARM64
  - DCO
  - CodeQL/relevant security checks
  - live E2E
  - release audit
  - output quality audit

- [ ] **Step 7: Validate exact post-merge `main` SHA**
  - Confirm no failed or in-progress checks.

- [ ] **Step 8: Update this plan to `COMPLETE` with exact PRs, commits, workflow evidence, and any intentionally deferred items**

---

## Definition of Done

The audit-closure milestone is complete only when all of the following are true:

- [ ] RuneLite is cloned and exercised for real in GitHub Actions.
- [ ] RuneLite Java 11 is detected through its actual Gradle configuration shape without a RuneLite-specific hack.
- [ ] The complete supported RuneLite command matrix has been executed and its outputs/logs audited.
- [ ] `affectedComponents` cannot silently become an allowed change boundary.
- [ ] Repository-scoped outputs are consistently rooted in the target repository.
- [ ] Git-history performance has a measured baseline and regression guardrails.
- [ ] MCP compatibility is verified against the current official target revision and the supported legacy revision(s).
- [ ] Architecture heuristic semantics are explicit and cannot be mistaken for compiler-grade proof.
- [ ] Every original audit finding has a verified disposition.
- [ ] No material audit finding remains unresolved.
- [ ] All applicable PR checks pass.
- [ ] Post-merge `main` checks pass for the exact final SHA.

## Plan Update Protocol

After every implementation PR:

1. Add the PR number and final commit SHA to the owning task.
2. Record the exact checks that passed and any failed check/root cause.
3. Record the actual test/artifact evidence, not only a statement that CI was green.
4. Mark the task `VERIFIED` only after post-merge `main` validation.
5. If a new issue is discovered, add a new task rather than silently expanding an existing task.
6. Re-run the self-review when task scope or architecture changes materially.
