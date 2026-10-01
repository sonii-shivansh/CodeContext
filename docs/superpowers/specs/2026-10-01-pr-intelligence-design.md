# PR Intelligence — Design Specification

## 1. Purpose

PR Intelligence turns a Git diff into a deterministic engineering assessment. It reuses CodeContext's repository analysis and Phase 2 Change Impact Engine to identify affected code, risk signals, architecture boundary crossings, and likely tests. An optional AI layer may later explain the evidence, but AI is not the source of truth.

## 2. Goals

- Analyze a GitHub pull request or an equivalent local Git diff.
- Normalize changed files into a stable `ChangeSet`.
- Run change-impact analysis against the repository graph.
- Combine impact with existing engineering-risk signals.
- Detect package/module boundary crossings.
- Identify likely affected tests without claiming that a test is definitely sufficient.
- Produce deterministic, versioned JSON suitable for CI, REST, CLI, and future GitHub comments.
- Provide actionable severity and evidence for each finding.
- Keep GitHub-specific transport separate from the core analysis engine.
- Work fully in CI so the complete feature can be tested without a developer laptop.

## 3. Non-goals for this phase

- No automatic code modification.
- No automatic merge decisions.
- No mandatory external LLM/provider dependency.
- No persistent multi-tenant database.
- No GitHub App installation/authentication implementation.
- No claim that static analysis proves runtime behavior.

## 4. Recommended architecture

```text
GitHub PR / Local Git Diff
          |
          v
     ChangeSetBuilder
          |
          v
   PR Intelligence Core
     /      |       \
    v       v        v
 Impact   Risk   Architecture/Test Signals
    \       |       /
     \      |      /
      v     v     v
       PRIntelligenceResult
              |
       +------+------+
       |             |
      JSON       Optional AI
       |          explanation
       v
 CI / REST / CLI / GitHub integration
```

The core must accept plain domain objects and must not depend on Ktor, GitHub API clients, or a specific AI provider.

## 5. Domain model

### ChangeSet

Contains the repository-relative changed paths and change type (`ADDED`, `MODIFIED`, `DELETED`, `RENAMED`, `COPIED` where available). Paths must be normalized and deterministic.

### PRIntelligenceResult

Contains:

- schema version
- change summary
- impacted nodes
- risk findings
- architecture findings
- test candidates
- aggregate severity
- deterministic evidence/reasons

All collections must have stable ordering.

### Finding

Every finding has:

- stable rule identifier
- severity (`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
- affected path(s)
- evidence values
- human-readable reason

The rule identifier allows future CI policy configuration without parsing prose.

## 6. Analysis rules — first version

### Impact

- Direct dependents of changed source files.
- Transitive dependents up to a configurable maximum depth.
- Unknown/deleted files are reported as unresolved rather than silently ignored.

### Risk

Reuse the existing deterministic engineering-risk signals rather than creating a second incompatible risk algorithm.

### Architecture

Flag changed files whose impacted dependency paths cross package/module boundaries. The first implementation uses repository/package structure already available to CodeContext; service-level architecture inference is explicitly deferred.

### Tests

Identify test files whose names/packages correspond to changed or impacted production files. Report them as `test candidates`, not guaranteed coverage.

### Change-size signal

Record added/deleted/modified file counts and line-change counts when Git metadata provides them. Large changes are a signal, not an automatic failure.

## 7. Severity policy

Severity is deterministic and evidence-based. Examples:

- `CRITICAL`: reserved for configured policy violations with strong evidence.
- `HIGH`: high-risk changed component plus substantial impact or architecture boundary crossing.
- `MEDIUM`: meaningful transitive impact, elevated risk, or missing likely test candidates.
- `LOW`: limited impact or informational engineering concern.
- `INFO`: useful context with no detected risk.

The first implementation must avoid a single opaque score replacing individual evidence.

## 8. Interfaces

### CLI

Add a command that can analyze the current working tree or a specified base/head revision and emit a machine-readable report. Human-readable output may summarize the same result.

### REST

Add an endpoint for local repository PR intelligence. It must reuse the existing path validation and rate limiting. It must not accept arbitrary remote URLs.

### GitHub integration boundary

Create an adapter boundary for GitHub PR metadata/diffs. The initial implementation may consume a local/base-head diff so the core remains testable without credentials. A future GitHub App/CLI adapter can implement the same interface.

## 9. Error handling

- Invalid repository path: existing sanitized API error.
- Invalid revision: explicit validation error.
- Deleted/unparseable files: preserve the change record and attach an analysis warning.
- Empty diff: valid result with zero findings.
- Analysis failure: fail the command/request rather than returning a partially fabricated risk assessment.

## 10. Security and privacy

- Never send repository source to an AI provider as part of deterministic PR analysis.
- Do not expose absolute filesystem paths in API responses.
- Treat Git author information as metadata and expose only what the existing product already exposes.
- Do not accept arbitrary remote repository URLs in the local REST endpoint.

## 11. Testing strategy

CI must test:

1. Added, modified, deleted, and renamed files.
2. Direct and transitive impact.
3. Cross-package impact.
4. Test candidate detection.
5. Deterministic ordering and serialization.
6. Empty and invalid diffs.
7. Unknown/deleted source files.
8. Large synthetic graphs for bounded runtime.
9. REST validation and rate limiting.
10. End-to-end analysis of CodeContext itself.

Golden fixtures should represent small repositories with known dependency graphs so results can be asserted exactly.

## 12. CI/CD acceptance criteria

A PR is not mergeable until GitHub Actions proves:

- compilation succeeds;
- unit/property tests pass;
- distribution builds;
- PR Intelligence CLI smoke test succeeds;
- CodeContext self-analysis succeeds;
- deterministic PR Intelligence JSON validates against its schema contract;
- REST endpoint starts and responds correctly;
- CodeQL and existing quality checks pass.

## 13. Future extensions enabled by this design

- GitHub PR comments and status checks.
- Configurable engineering policies.
- AI-generated evidence-based PR explanations.
- Ownership-aware review suggestions.
- Test-selection recommendations.
- Architecture drift detection.
- Cross-repository impact analysis.
- MCP tools for AI coding agents.

## 14. Success criteria

A developer should be able to provide a PR diff and receive a reproducible answer to:

> What changed, what can be affected, which engineering risks are present, which architecture boundaries are involved, and which tests should be reviewed?

The answer must be explainable from CodeContext's computed evidence and reproducible in CI.
