# Change Safety Loop

CodeContext provides a local, deterministic workflow for preparing and verifying engineering changes:

```text
prepare → contract → change → verify
```

The workflow gives developers and AI agents a stable engineering context before code is changed, then checks whether the implementation stayed within the exact prepared scope and repository state.

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
- `output/engineering-plan.json` — the reusable engineering plan
- `output/agent-change-contract.json` — the persisted repository-bound contract consumed by `verify`

The contract is bound to the canonical repository path and the Git `HEAD` observed during `prepare` when Git metadata is available. Its SHA-256 fingerprint covers the change summary, repository identity, prepared `HEAD`, planned paths, expected components, verification commands, evidence IDs, and architecture expectations.

The plan is deterministic. It identifies affected components from repository evidence, highlights architecture/hotspot concerns, records uncertainties, and supplies verification commands.

Each prepare result also contains a `provenance` contract. It records the operation, analysis schema version, repository `HEAD` commit when available, the evidence IDs used by the plan, and a stable SHA-256 provenance ID.

## 2. Change the code

Implement the requested change using your normal workflow or an AI coding agent.

CodeContext does not modify source files during `prepare` or `verify`.

Do not edit `output/agent-change-contract.json` or substitute a newly generated contract after `prepare`. Verification is intentionally bound to the persisted contract.

## 3. Verify

Run:

```bash
codecontext verify
```

Or explicitly specify the repository, plan, and contract:

```bash
codecontext verify \
  --path /path/to/repository \
  --plan output/engineering-plan.json \
  --contract output/agent-change-contract.json \
  --output output/verification.json
```

Verification combines:

- persisted contract fingerprint validation
- repository identity validation
- prepared `HEAD` freshness validation
- working-tree scope comparison against the contract's planned paths
- deterministic dependency impact analysis
- PR Intelligence
- Architecture Intelligence
- the contract's recommended verification commands
- provenance for the verification operation and evidence context

A missing contract is an error. A tampered contract, mismatched plan, repository mismatch, or stale prepared `HEAD` produces `FAIL` rather than silently reconstructing a replacement contract.

### Statuses

`PASS` means the persisted prepared contract is valid and the detected working-tree paths remain within the planned scope.

`REVIEW_REQUIRED` means the contract is valid but the change needs explicit review, such as a deleted file or a critical deterministic finding.

`FAIL` means the contract is invalid/stale or at least one changed path falls outside the supplied plan. This is intended to catch accidental, stale, tampered, or agent-introduced scope expansion.

A status is a review signal, not a claim that the code is correct. Tests and human engineering judgment remain necessary.

## Agent workflow

The same loop can be used by an MCP-compatible coding agent:

```text
CodeContext evidence
        ↓
Prepare change plan + persisted contract
        ↓
Agent modifies repository
        ↓
CodeContext verifies ORIGINAL persisted contract
        ↓
Agent runs tests / responds to findings
```

The MCP verification tool uses the repository's persisted `output/agent-change-contract.json` when no explicit contract object is supplied. The current `codecontext_get_change_contract` MCP helper generates a fresh contract for a requested summary; it is not the persisted verification artifact.

## Security boundary

All analysis is local. Repository paths are handled through CodeContext's existing local path validation. The workflow does not upload repository contents to CodeContext infrastructure.

AI remains optional; the prepare/verify workflow itself does not require an external model.
