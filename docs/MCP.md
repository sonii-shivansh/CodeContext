# MCP / AI-agent integration

CodeContext can expose deterministic engineering intelligence to MCP-compatible AI agents through a local stdio server.

## Start the server

Build/install CodeContext, then run:

```bash
codecontext mcp
```

The process communicates using newline-delimited JSON-RPC messages over stdin/stdout. Do not pipe human-readable CLI output into the MCP process.

## Tools

### `codecontext_analyze_repository`

Returns repository-relative engineering facts including:

- parsed file count
- dependency graph node/edge counts
- top dependency/PageRank hotspots
- analysis schema version

Arguments:

```json
{"repoPath":"/absolute/path/to/repository"}
```

### `codecontext_impact_analysis`

Calculates deterministic reverse-dependency impact for changed repository-relative paths.

Arguments:

```json
{
  "repoPath":"/absolute/path/to/repository",
  "changedPaths":["src/main/kotlin/com/example/PaymentService.kt"]
}
```

The current server accepts at most 100 changed paths per call.

### `codecontext_architecture_analysis`

Returns the repository's current Architecture Intelligence result, including configured architectural signals and boundaries.

Arguments:

```json
{"repoPath":"/absolute/path/to/repository"}
```

### `codecontext_pr_intelligence`

Analyzes the working tree, or a specific Git revision pair when both revisions are supplied.

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

## Agent workflow

The intended workflow is:

```text
Understand repository
        ↓
Analyze architecture / dependencies
        ↓
Calculate change impact
        ↓
Inspect PR intelligence
        ↓
Plan or modify code
        ↓
Run tests / verification
```

CodeContext is deliberately the **evidence layer**, not the coding agent. An agent can use these tools to ground decisions before changing a repository.

## Security boundary

The MCP server is intended for trusted local use.

- Repository paths must resolve to readable directories permitted by `CODECONTEXT_ALLOWED_PATHS`, the current working directory, or the system temporary directory.
- Remote repository URLs are rejected.
- The MCP transport does not implement authentication or tenant isolation.
- The server does not expose arbitrary filesystem reads; tools invoke CodeContext's existing repository analysis boundaries.

For a shared or remote deployment, put an authenticated service boundary in front of CodeContext rather than exposing the stdio process directly.

## Compatibility

The server advertises MCP protocol version `2025-06-18` and implements the MCP initialization handshake plus the `tools/list`, `tools/call`, and `ping` methods needed for the current tool surface.

Client configuration differs between agent products. Configure the client to launch the installed CodeContext executable with the `mcp` argument, for example:

```text
command: /absolute/path/to/codecontext
args: ["mcp"]
```

Do not copy a client-specific configuration file into a repository unless that client format is actually required by your team; the executable and `mcp` argument are the stable CodeContext interface.
