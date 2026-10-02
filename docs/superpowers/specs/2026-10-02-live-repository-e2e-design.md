# Live Repository E2E and Agent Gateway Design

## Goal
Make GitHub Actions the complete development and validation environment for CodeContext, so the project can exercise its real CLI, MCP, REST, AI, planning, verification, architecture, history, and change-safety capabilities against real repositories without requiring a local laptop.

## Constraints
- All authoritative validation must run in GitHub Actions.
- Use real public repositories as external integration targets; keep CodeContext itself as the self-analysis target.
- Never treat provider quota/rate-limit exhaustion as a CodeContext failure; classify it separately and keep deterministic checks authoritative.
- Do not mutate an external repository's remote state. Any change-safety exercise happens in a temporary clone.
- Preserve deterministic, evidence-backed behavior; live AI is an optional integration layer.
- Upload machine-readable evidence artifacts on success and failure.

## Design

### 1. Canonical live repository
Spring PetClinic is the canonical Java/Maven integration target. The clone uses enough Git history for revision-based intelligence and is isolated inside the GitHub Actions runner.

### 2. Full feature journey
The live workflow executes:

1. CLI surface/help validation
2. doctor/setup surface
3. analyze
4. architecture analysis
5. impact analysis against a real source file
6. architecture baseline + drift
7. architecture contract
8. engineering context snapshot + diff
9. evolution/history intelligence
10. PR intelligence on real revisions
11. grounded repository Q&A
12. evidence-backed plan
13. prepare
14. safe temporary working-tree mutation
15. context diff after mutation
16. working-tree PR intelligence
17. verify the prepared change
18. REST API against the live repository
19. MCP initialize/list/call for every exposed tool
20. optional live Gemini Q&A

### 3. Change-safety probe
Create one deterministic, repository-local temporary source change in the clone. The change is never pushed remotely. Capture before/after engineering-context digests, verify the mutation is detected, run preparation/verification, remove the mutation, and require the restored snapshot digest to equal the baseline digest.

### 4. MCP protocol contract
Exercise the actual packaged CLI over stdio, not only unit-level protocol code. Validate initialize, tools/list, every advertised tool, unknown-tool rejection, and remote repository-path rejection.

### 5. REST contract
Start the real packaged server against the live repository and exercise health, readiness, analyze, architecture, PR intelligence, and path-security boundaries.

### 6. Evidence bundle
Upload machine-readable JSON and command output from every stage, including context snapshots/diffs, MCP responses, REST responses, verification, and provider status. This makes GitHub Actions the observable development environment when no local machine is available.

## Success Criteria
A required Live E2E run is green only when all deterministic features complete successfully against the live repository, MCP and REST surfaces respond correctly, the temporary mutation is detected and verified, the repository is restored exactly, and the evidence bundle is produced. AI provider checks may be classified as external dependency skips only when the captured provider response explicitly indicates quota/rate limiting.
