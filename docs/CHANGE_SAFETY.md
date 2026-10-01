# Change Safety Loop

CodeContext provides a local, deterministic workflow for preparing and verifying engineering changes:

```text
prepare → change → verify
```

The workflow is designed to give developers and AI agents a stable engineering context before code is changed, then check whether the implementation stayed within that planned scope.

## 1. Prepare

Run from the repository you intend to change:

```bash
codecontext prepare "add OAuth login"
```

For another repository:

```bash
codecontext prepare "add OAuth login" --path /path/to/repository
```

The command creates:

- `output/engineering-context.json` — repository change set, grounded evidence, and the generated plan
- `output/engineering-plan.json` — the reusable plan consumed by `verify`

The plan is deterministic. It identifies affected components from repository evidence, highlights architecture/hotspot concerns, records uncertainties, and supplies verification commands.

Each prepare result also contains a `provenance` contract. It records the operation, analysis schema version, repository `HEAD` commit when available, the evidence IDs used by the plan, and a stable SHA-256 provenance ID. If Git metadata cannot be read, the result explicitly records that the repository commit is unavailable rather than inventing one.

## 2. Change the code

Implement the requested change using your normal workflow or an AI coding agent.

CodeContext does not modify source files during `prepare` or `verify`.

## 3. Verify

Run:

```bash
codecontext verify
```

Or explicitly specify the repository and plan:

```bash
codecontext verify \
  --path /path/to/repository \
  --plan output/engineering-plan.json \
  --output output/verification.json
```

Verification combines:

- working-tree scope comparison against the plan
- deterministic dependency impact analysis
- PR Intelligence
- Architecture Intelligence
- the plan's recommended verification commands
- provenance for the verification operation and evidence context

### Statuses

`PASS` means the detected working-tree paths remain within the planned scope.

`REVIEW_REQUIRED` means the change needs explicit review, such as a deleted file or a critical deterministic finding.

`FAIL` means at least one changed path falls outside the supplied plan. This is intended to catch accidental or agent-introduced scope expansion.

A status is a review signal, not a claim that the code is correct. Tests and human engineering judgment remain necessary.

## Agent workflow

The same loop can be used by an MCP-compatible coding agent:

```text
CodeContext evidence
        ↓
Prepare change plan
        ↓
Agent modifies repository
        ↓
CodeContext verifies scope + impact
        ↓
Agent runs tests / responds to findings
```

This keeps CodeContext in the role of an evidence and verification layer rather than an autonomous coding authority.

## Security boundary

All analysis is local. Repository paths are handled through CodeContext's existing local path validation. The workflow does not upload repository contents to CodeContext infrastructure.

AI remains optional; the prepare/verify workflow itself does not require an external model.
