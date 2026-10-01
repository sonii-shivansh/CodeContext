# API Reference

CodeContext exposes a CLI and a local REST API. The REST API is implemented by `com.codecontext.server.CodeContextServer` and is intended for trusted local or internal use.

## Build and start

```bash
./gradlew installDist
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
```

Keep the default bind address on loopback for local use. Deployments beyond loopback must provide authentication, trusted-origin controls, TLS, request quotas, report authorization, and tenant isolation at the deployment boundary.

## CLI

```text
codecontext analyze <path>
codecontext impact <path> <changed-files...>
codecontext architecture <path>
codecontext pr-intelligence <path>
codecontext repo-qa <question> [--path <path>]
codecontext plan <change-summary> [--evidence <file>]
codecontext ai-assistant <path>
codecontext evolution <path>
codecontext server [--host <address>] [--port <number>]
```

Run `codecontext <command> --help` for the exact options in the installed version.

## Deterministic intelligence contract

The analysis layer is designed to produce machine-readable artifacts that can be consumed by CI and later AI layers. Deterministic artifacts are authoritative for repository facts.

Important principles:

- repository-relative paths are preferred in public evidence;
- output schemas are versioned where they cross subsystem boundaries;
- findings describe evidence and rules rather than claiming runtime certainty;
- unknown and insufficient-evidence states must remain explicit.

## Repository Q&A

`repo-qa` retrieves grounded evidence for a developer question. It does not require an external AI provider.

```bash
codecontext repo-qa "why is PaymentService risky?" --path /workspace/example --max-results 8
```

The output contains the classified intent and ranked evidence. The retriever is bounded to prevent unbounded context growth.

## Engineering planning

`plan` converts grounded evidence into a deterministic engineering plan.

```bash
codecontext plan "add payment validation" \
  --evidence output/grounded-evidence.json \
  --output output/engineering-plan.json
```

The plan is read-only and may include:

- affected components;
- risk level;
- implementation steps;
- evidence IDs;
- verification criteria;
- uncertainties.

It is intentionally provider-independent.

## REST endpoints

### `GET /`

Returns a plain-text service banner.

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

Serves a generated report. Report retention and authorization remain deployment responsibilities.

### `POST /ask`

Answers a repository question using the configured AI provider and repository-derived context.

```json
{
  "repoPath": "/workspace/example",
  "question": "Where is authentication configured?"
}
```

AI must be explicitly enabled. The provider may receive repository-derived prompt context. Use only providers and data-handling arrangements approved for the source code involved.

### `POST /analyze-org`

Analyzes multiple local repositories with bounded concurrency. At most 20 repositories may be submitted per request, and each repository is subject to `maxFilesAnalyze`.

### Change and PR intelligence

The application also exposes local change-impact and PR Intelligence flows. See [PR_INTELLIGENCE.md](PR_INTELLIGENCE.md) for the deterministic result model and rules.

## Path security

The server resolves paths with `Path.toRealPath()` and accepts only readable directories that are equal to or descendants of an allowed root.

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

The server returns standard rate-limit headers where applicable.

## AI boundary

AI is an optional reasoning layer. It must not be treated as the source of repository facts. Grounded evidence is deterministic and bounded before it is included in an AI request.

Do not place secrets, credentials, or unapproved confidential source code in AI prompts. Provider failures are sanitized before reaching public API clients.

## Error shape

Errors use a small public shape:

```json
{
  "error": "Invalid or unsafe repository path"
}
```

Provider and internal failures are sanitized before being returned to clients.

## Future API direction

Planned platform endpoints include policy evaluation, Change Proof artifacts, architecture snapshots, simulation results, provenance records, and controlled agent execution. These should remain separate from the deterministic analysis contracts and require explicit authentication/authorization in remote deployments.
