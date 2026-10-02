# Live Repository E2E and Agent Gateway Implementation Plan

**Goal:** Turn GitHub Actions into a complete laptop-free validation environment that runs CodeContext end-to-end against a real repository and exercises every public integration surface.

**Architecture:** Keep the deterministic CodeContext engine unchanged where possible. Extend the existing Live E2E workflow with reusable Bash/Python helpers for cloning, assertions, mutation/restore, MCP stdio checks, and evidence collection. The workflow remains the orchestration boundary; product behavior remains in the existing CLI, MCP, and REST implementations.

**Tech Stack:** GitHub Actions, Bash, Python 3, Gradle, JDK 21, CodeContext CLI, MCP stdio, REST, public Git repository.

## Global constraints
- All authoritative validation runs in GitHub Actions.
- Real public repositories are temporary integration targets only; never push to them.
- Deterministic checks remain authoritative over optional provider integrations.
- Provider quota/rate-limit failures must be classified explicitly.
- Every live run uploads machine-readable evidence.
- The live workflow must be safe to run on pull requests.

## Tasks

### 1. Reusable live-E2E harness
- Add `.github/scripts/live-e2e.sh` for the complete CLI/change-safety journey.
- Add `.github/scripts/mcp-smoke.py` for actual packaged MCP stdio testing.
- Add `.github/scripts/live-e2e-assertions.py` for deterministic evidence-bundle validation.

### 2. Complete live workflow
- Use Spring PetClinic as the canonical external repository.
- Build and test the packaged CodeContext distribution.
- Validate every CLI surface.
- Run analysis, architecture, impact, drift, contracts, context, evolution, PR intelligence, Q&A/evidence, planning, prepare, mutation, and verify.
- Run MCP initialize/list/every advertised tool plus invalid and unsafe requests.
- Run REST health, readiness, analyze, architecture, PR intelligence, and path-security checks.
- Upload evidence even on failure.
- Treat Gemini quota/rate limits as an explicit external-dependency classification.

### 3. CI gate
- Use the existing `live-e2e.yml` so the new test suite is executed on pull requests without requiring a merge first.
- Inspect every required job and fix root causes rather than weakening assertions.

### 4. Operator documentation
- Document the live repository, test journey, evidence bundle, and provider failure classification.

### 5. Final gate
- Keep the PR unmerged until all required checks are green.
- The user only needs to merge/publish the release after the GitHub Actions gate is green.
