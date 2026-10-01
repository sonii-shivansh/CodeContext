# API Reference

CodeContext exposes a CLI and a local REST API. The REST API is implemented by `com.codecontext.server.CodeContextServer` and is intended for trusted local or internal use.

## Build and start

```bash
./gradlew installDist
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
```

Keep the default bind address on loopback for local use. Deployments beyond loopback must provide authentication, trusted-origin controls, TLS, quotas, report authorization, and tenant isolation at the deployment boundary.

## CLI

```text
codecontext analyze <path>
codecontext impact <path> <changed-files...>
codecontext architecture <path>
codecontext architecture-drift <path> --baseline <architecture-json> [--json]
codecontext pr-intelligence <path>
codecontext repo-qa <question> [--path <path>] [--max-results <n>] [--evidence-output <file>]
codecontext plan <change-summary> [--evidence <file>] [--output <file>]
codecontext ai-assistant <path>
codecontext evolution <path>
codecontext server [--host <address>] [--port <number>]
```

Run `codecontext <command> --help` for the exact installed options.

## Architecture drift

`architecture-drift` compares a previously generated `ArchitectureIntelligenceResult` with the current deterministic architecture analysis. It reports added/removed findings, new/removed cycles, and changed layer counts.

```bash
codecontext architecture . --json
codecontext architecture-drift . --baseline output/architecture-baseline.json --json
```

The baseline must be a valid versioned architecture artifact. Drift comparison is deterministic and does not invoke an AI provider.

## Deterministic intelligence

The analysis layer produces machine-readable artifacts consumed by CI and downstream intelligence features. Deterministic artifacts are authoritative for repository facts.

Important principles:

- repository-relative paths are preferred in public evidence;
- cross-subsystem output schemas are versioned where applicable;
- findings describe evidence and analysis rules rather than runtime certainty;
- unknown and insufficient-evidence states remain explicit.

## Repository Q&A

`repo-qa` retrieves grounded evidence for a developer question without requiring an external AI provider.

```bash
codecontext repo-qa "why is PaymentService risky?" --path /workspace/example --max-results 8
```

The stdout result contains the classified intent and ranked evidence. Retrieval is bounded.

When the result will feed the engineering planner, export the reusable deterministic evidence artifact separately:

```bash
codecontext repo-qa "which files are the main architectural hotspots?" \
  --path /workspace/example \
  --evidence-output output/grounded-evidence.json

codecontext plan "add payment validation" \
  --evidence output/grounded-evidence.json \
  --output output/engineering-plan.json
```

`--evidence-output` writes the versioned `GroundedEvidence` contract. This keeps human-oriented ranked Q&A output separate from the machine-oriented planner input.

## Engineering planning

`plan` converts a `GroundedEvidence` artifact into a deterministic engineering plan.

```bash
codecontext plan "add payment validation" \
  --evidence output/grounded-evidence.json \
  --output output/engineering-plan.json
```

The current plan contract includes affected components, risk, implementation steps, evidence IDs, verification criteria, and uncertainties. The planner is read-only and provider-independent.

## REST endpoints

### `GET /`

Returns a service banner.

### `GET /health`

Returns service health and version information. Liveness and readiness endpoints are also available.

### `POST /analyze`

Analyzes an existing local repository and generates an HTML report.

```json
{
  "repoPath": "/workspace/example"
}
```

Remote URLs are rejected. The request path must resolve to a readable directory under a configured allowed root.

### `GET /reports/{id}.html`

Serves a generated report. Retention and authorization remain deployment responsibilities.

### `POST /ask`

Answers a repository question using the configured AI provider and repository-derived context.

```json
{
  "repoPath": "/workspace/example",
  "question": "Where is authentication configured?"
}
```

AI must be explicitly enabled. Provider data-handling requirements must be considered before sending source-derived context outside the local environment.

### `POST /analyze-org`

Analyzes multiple local repositories with bounded concurrency. At most 20 repositories may be submitted per request, and each repository is subject to `maxFilesAnalyze`.

## Change and PR intelligence

Local change-impact and PR Intelligence flows are exposed through the application and CLI. See [PR_INTELLIGENCE.md](PR_INTELLIGENCE.md) for the deterministic result model and rules.

## Path security

The server resolves paths with `Path.toRealPath()` and accepts only readable directories equal to or descendants of a configured allowed root.

Allowed roots are configured with `CODECONTEXT_ALLOWED_PATHS`, separated by the platform path separator. Configure the smallest practical set of roots.

## Rate limiting

Rate limiting is enabled by default. Configuration includes:

```json
{
  "rateLimit": {
    "enabled": true,
    "requestsPerMinute": 60,
    "requestsPerHour": 1000
  }
}
```

## AI boundary

AI is an optional reasoning layer. It must not be treated as the source of repository facts. Grounded evidence is deterministic and bounded before it is included in an AI request.

Do not place secrets, credentials, or unapproved confidential source code in AI prompts. Provider and internal failures are sanitized before public API clients receive them.

## Error shape

```json
{
  "error": "Invalid or unsafe repository path"
}
```

Provider and internal failures are sanitized before being returned to clients.
