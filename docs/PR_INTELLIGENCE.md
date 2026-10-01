# PR Intelligence

PR Intelligence converts a local Git change set into a deterministic engineering assessment. It combines Git diff metadata, dependency impact, existing engineering-risk signals, package boundaries, and likely test candidates.

## CLI

Analyze the current working tree:

```bash
codecontext pr-intelligence /path/to/repository --json
```

Analyze two revisions:

```bash
codecontext pr-intelligence /path/to/repository \
  --base main \
  --head feature/my-change \
  --json
```

The JSON report is written to `output/pr-intelligence.json`.

## REST

`POST /pr-intelligence` accepts a local repository path and optionally a base/head revision pair.

```json
{
  "repoPath": "/workspace/example",
  "baseRevision": "main",
  "headRevision": "feature/my-change"
}
```

Omit both revisions to analyze the working tree. Supplying only one revision is rejected.

Remote repository URLs are deliberately rejected. A future GitHub adapter can provide remote PR data without making the local endpoint a network-fetch primitive.

## Result

The result is versioned and machine-readable:

- `changeSummary`: file and line change counts;
- `impactedFiles`: dependency-aware blast radius;
- `impactedPackages`: affected package count;
- `crossPackageImpacts`: impact crossing changed package boundaries;
- `testCandidates`: likely tests, explicitly described as candidates rather than coverage proof;
- `findings`: deterministic rule-based evidence;
- `aggregateSeverity`: highest finding severity.

Every finding has a stable `ruleId`, severity, affected repository-relative paths, evidence values, and a human-readable reason.

## First-version rules

| Rule | Meaning |
| --- | --- |
| `CHANGE_UNRESOLVED` | A changed/deleted path could not be resolved in the analyzed graph. |
| `IMPACT_BROAD` | The dependency blast radius is substantial. |
| `ARCH_CROSS_PACKAGE` | Impact crosses the package boundary of the changed files. |
| `CHANGED_HIGH_RISK_COMPONENT` | A high/critical deterministic risk component was modified. |
| `TEST_CANDIDATE_MISSING` | No likely test candidate was identified. |
| `CHANGE_LARGE` | The change is large by file or line count. |

These rules are review signals. They do not prove runtime correctness, test sufficiency, or production impact.

## Architecture

```text
Git working tree / revision pair
              |
              v
       GitChangeSetBuilder
              |
              v
       PRIntelligenceAnalyzer
          /    |     \
         v     v      v
      Impact  Risk  Test/Architecture signals
         \     |     /
          \    |    /
           v   v   v
        PRIntelligenceResult
              |
        +-----+------+
        |            |
       JSON         REST
        |
      Future GitHub adapter / CI check / grounded AI
```

The deterministic core has no dependency on an LLM or GitHub API. AI can later explain evidence produced here without becoming the source of truth.
