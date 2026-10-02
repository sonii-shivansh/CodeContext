# MCP / AI-agent integration

CodeContext exposes deterministic engineering intelligence to MCP-compatible AI agents through a local stdio server.

## Start the server

Build/install CodeContext, then run:

```bash
codecontext mcp
```

The process communicates using newline-delimited JSON-RPC messages over stdin/stdout. Do not pipe human-readable CLI output into the MCP process; stdout is reserved for protocol messages.

## Current tool surface

The current server exposes these tools:

| Tool | Purpose |
|---|---|
| `codecontext_analyze_repository` | Deterministic repository structure, graph, and hotspot evidence |
| `codecontext_impact_analysis` | Dependency-aware impact for repository-relative changed paths |
| `codecontext_architecture_analysis` | Current deterministic Architecture Intelligence |
| `codecontext_pr_intelligence` | Working-tree or revision-pair PR/change intelligence |
| `codecontext_get_engineering_reality` | Repository-state-bound engineering reality snapshot |
| `codecontext_get_context_snapshot` | Versioned engineering-context snapshot |
| `codecontext_get_context_diff` | Deterministic diff between two context snapshots |
| `codecontext_get_architecture_drift` | Deterministic baseline/current architecture drift |
| `codecontext_get_architecture_contract` | Deterministic architecture governance evaluation |
| `codecontext_prepare_change` | Grounded evidence + deterministic engineering plan + change contract |
| `codecontext_get_change_contract` | Generate a deterministic contract for a requested change summary |
| `codecontext_get_evidence` | Bounded grounded repository evidence |
| `codecontext_change_safety` | Current working-tree scope check against an EngineeringPlan |
| `codecontext_verify_change` | Verify the working tree against the persisted Agent Change Contract and plan |

All repository arguments use the property name `repoPath`. Remote repository URLs are rejected.

### Repository analysis

```json
{"repoPath":"/absolute/path/to/repository"}
```

### Impact analysis

```json
{
  "repoPath":"/absolute/path/to/repository",
  "changedPaths":["src/main/kotlin/com/example/PaymentService.kt"]
}
```

The current server accepts at most 100 changed paths per call.

### PR Intelligence

Working tree:

```json
{"repoPath":"/absolute/path/to/repository"}
```

Revision pair:

```json
{
  "repoPath":"/absolute/path/to/repository",
  "baseRevision":"main",
  "headRevision":"feature/payment-retry"
}
```

Both revisions must be supplied together.

### Prepare and verify

Prepare a change:

```json
{
  "repoPath":"/absolute/path/to/repository",
  "changeSummary":"Add payment retry validation"
}
```

The CLI prepare workflow persists `output/agent-change-contract.json`. The MCP verify tool loads that persisted contract when no explicit contract is supplied; verification therefore depends on the repository-scoped artifact produced by prepare.

The current MCP `codecontext_get_change_contract` operation is a **fresh contract-generation helper** for a requested change summary. It should not be treated as a replacement for the persisted contract used by verification.

## Agent workflow

The recommended workflow is:

```text
Engineering Reality
        ↓
Context / architecture / impact
        ↓
Grounded evidence
        ↓
Prepare change
        ↓
Persist Agent Change Contract
        ↓
Agent modifies repository
        ↓
Verify ORIGINAL persisted contract
        ↓
Tests / human review
```

The contract binds the prepared repository identity and Git `HEAD` to planned paths, expected components, verification commands, evidence IDs, architecture expectations, and a SHA-256 fingerprint.

A verification failure is a safety signal. It does not prove that the implementation is otherwise correct; normal tests and engineering review remain required.

CodeContext is deliberately the **evidence and verification layer**, not the coding agent. It does not autonomously modify repository source files.

## Security boundary

The MCP server is intended for trusted local use.

- Repository paths must resolve to readable directories permitted by the configured path-safety boundary.
- Remote repository URLs are rejected.
- The MCP transport does not implement authentication or tenant isolation.
- The server does not expose arbitrary filesystem reads; tools invoke CodeContext's repository analysis boundaries.

For a shared or remote deployment, put an authenticated service boundary in front of CodeContext rather than exposing the stdio process directly.

## Compatibility

The current implementation uses the **2025-11-25 MCP legacy handshake era**, which is the latest revision using `initialize`. It implements `initialize`, `notifications/initialized`, `ping`, `tools/list`, and `tools/call` for the current tool surface. Modern `2026-07-28` MCP lifecycle support is intentionally deferred until the server can implement its stateless discovery/request model correctly rather than advertising unsupported behavior.

Client configuration differs between agent products. Configure the client to launch the installed CodeContext executable with the `mcp` argument:

```text
command: /absolute/path/to/codecontext
args: ["mcp"]
```

Do not copy a client-specific configuration file into a repository unless that client format is actually required by your team; the executable and `mcp` argument are the stable CodeContext interface.
