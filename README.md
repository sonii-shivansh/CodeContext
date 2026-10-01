# CodeContext

**Deterministic engineering intelligence for Java and Kotlin repositories.**

[🌐 Explore the CodeContext website](https://sonii-shivansh.github.io/CodeContext-Website/) · [📚 Documentation](https://github.com/sonii-shivansh/CodeContext/tree/main/docs) · [🚀 Releases](https://github.com/sonii-shivansh/CodeContext/releases)

CodeContext is a Kotlin/JVM CLI and local REST application that analyzes source code, dependency structure, Git history, and engineering signals to produce reproducible engineering intelligence. It also provides grounded repository Q&A, evidence-backed engineering planning, a local MCP interface for AI agents, and a prepare→change→verify safety loop.

## Current status

The `main` branch is validated against the **v0.6.0** release line. The repository implements:

- Java and Kotlin source analysis
- dependency graph construction
- PageRank knowledge hotspots
- cycle and architecture analysis
- Git authorship, churn, and evolution analysis
- learning-path generation
- deterministic change-impact analysis
- deterministic PR Intelligence
- Architecture Intelligence and deterministic architecture contracts
- versioned analysis/evidence artifacts
- deterministic engineering-context snapshots and diffs
- grounded repository Q&A retrieval
- deterministic, evidence-backed engineering planning
- evidence-first `prepare` workflow
- deterministic `verify` workflow for planned change scope, impact, PR, and architecture signals
- optional AI assistance over bounded repository-derived context
- local Ktor REST API
- local MCP stdio server for AI-agent integration
- path validation and rate limiting
- clean-environment end-to-end verification
- cross-platform distribution smoke verification for Linux x64, Windows x64, macOS x64, and macOS ARM64

Generated reports are self-contained and do not require a browser CDN request for their visualization code.

## Design principle

**Deterministic evidence first, AI reasoning second.**

Repository facts are produced by deterministic analysis. Grounded evidence preserves those facts for downstream Q&A, planning, agent workflows, and verification. AI is optional and is not treated as the authoritative source of repository truth.

## Quick start

For a released platform archive, extract it and run the launcher. Current release archives bundle the Java runtime, so Java 21/JAVA_HOME configuration is not required for normal use.

```text
Windows:  bin\\codecontext.bat --version
Linux/macOS: ./bin/codecontext --version
```

Then, from the repository you want to analyze:

```text
codecontext analyze .
```

The default report is written to `output/index.html` and can be opened without a network connection.

For source development:

```bash
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext
./gradlew clean test
./gradlew installDist
./build/install/codecontext/bin/codecontext --version
```

## AI setup — no manual config file required

AI is optional. When `ask` needs AI and no credential is configured, CodeContext can guide you through setup directly in the terminal:

```text
codecontext ask "What are the main architectural hotspots in this repository?"

CodeContext AI setup required.
Gemini is the default AI provider.
Add your Gemini API key now? [Y/n]:
Gemini API key: ********
✓ Gemini API key validated and saved securely.
```

The key is stored in the user's CodeContext configuration directory, not in the repository's `.codecontext.json`. The exact location is platform-specific. You can also use `GEMINI_API_KEY` or `GOOGLE_API_KEY` as environment variables for CI and non-interactive environments.

Use:

```bash
codecontext setup
codecontext doctor
```

`setup` interactively validates and stores a Gemini credential. `doctor` checks the Java runtime, repository, configuration source, and Gemini connectivity without printing the secret.

If you prefer project-specific configuration, `.codecontext.json` remains supported as an advanced override. Never commit API keys.

## CLI commands

```bash
# Repository analysis
codecontext analyze /path/to/repository

# Change impact
codecontext impact /path/to/repository src/main/Service.kt --json

# PR / change intelligence
codecontext pr-intelligence /path/to/repository --json
codecontext pr-intelligence /path/to/repository --base main --head feature/my-change --json

# Architecture intelligence and governance
codecontext architecture /path/to/repository --json
codecontext architecture-contract /path/to/repository --contract /path/to/.codecontext-architecture-contract.json --json

# Engineering context
codecontext context-snapshot /path/to/repository --json
codecontext context-diff output/before.json output/after.json --json

# Grounded repository Q&A
codecontext repo-qa "why is PaymentService risky?" --path /path/to/repository

# Evidence-backed change preparation
codecontext prepare "add payment validation" --path /path/to/repository

# Verify the implementation against the generated plan
codecontext verify --path /path/to/repository --plan output/engineering-plan.json

# Optional AI assistance — interactive setup happens automatically if needed
codecontext ask "What are the main architectural hotspots in this repository?"

# Git evolution
codecontext evolution /path/to/repository

# Local REST server
codecontext server --host 127.0.0.1 --port 8080

# AI-agent integration over MCP stdio
codecontext mcp
```

The recommended developer loop is:

```text
codecontext prepare "<change>"
        ↓
implement the change
        ↓
codecontext verify
        ↓
run tests / review findings
```

Use `codecontext <command> --help` for the exact options in the installed version.

See [docs/CHANGE_SAFETY.md](docs/CHANGE_SAFETY.md) for the workflow contract.

## MCP / AI-agent integration

CodeContext can expose deterministic engineering intelligence to MCP-compatible AI agents through a local stdio server:

```bash
codecontext mcp
```

The MCP server currently exposes deterministic repository analysis, impact, architecture, and PR-intelligence tools. All repository paths go through the same local path-safety boundary used by the REST API. The MCP server does not expose remote repository URLs and does not provide authentication or tenant isolation; use it as a trusted local integration.

See [docs/MCP.md](docs/MCP.md) for agent configuration guidance and protocol details.

## Architecture

```text
Repository + Git
       ↓
Scanner / Parsers / Git Intelligence
       ↓
Dependency Graph + Analysis Snapshot
       ↓
Deterministic Intelligence
 ┌─────┼─────┬──────┬──────────────┐
Risk  Impact  PR  Architecture  Evolution
                   ↓
             Architecture Contract
       ↓
Grounded Evidence
   ↙          ↘
Q&A          Planner
   ↘          ↙
 Optional AI reasoning
       ↓
 Prepare → Change → Verify
       ↓
 CLI / REST / MCP / CI
```

## Configuration

The recommended path is to use `codecontext setup` or environment variables. A repository-local configuration file is still supported for advanced settings:

```bash
cp .codecontext.json.template .codecontext.json
```

For architecture governance, copy the contract template and customize its limits:

```bash
cp .codecontext-architecture-contract.json.template .codecontext-architecture-contract.json
```

Important settings include source exclusions, file-count limits, Git history limits, caching, parallel parsing, reporting limits, AI configuration, architecture rules, and rate limiting. Never commit API keys.

The server also supports `CODECONTEXT_ALLOWED_PATHS`. Keep allowed roots as narrow as practical.

## Data & privacy

CodeContext is local-first. With AI disabled, repository analysis and deterministic intelligence do not send repository content to a CodeContext telemetry or storage service. AI is opt-in and sends bounded repository-derived context directly to the configured external provider when an AI operation is invoked.

Generated HTML reports are self-contained and do not require a third-party CDN at report-open time.

See [Data & Privacy](docs/DATA_PRIVACY.md) for the current implementation-level disclosure, including the approximate content limits used by AI operations.

## REST API

The local API provides health, analysis, reports, change-impact, PR Intelligence, repository Q&A/AI flows, and organization analysis. Local repository paths are validated against configured workspace roots; remote repository URLs are not accepted by the current local endpoints.

See [docs/API.md](docs/API.md).

The server does not currently provide authentication, tenant isolation, or deployment-level authorization. Those are responsibilities of any deployment boundary beyond trusted local use.

## Development and CI

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

GitHub Actions is the project's authoritative clean-environment verification path. CI validates compilation, tests, CLI flows, generated artifacts, intelligence flows, architecture contracts, server health, API input boundaries, cross-platform distribution smoke tests, and onboarding flows. Release packaging also validates that platform distributions can launch with an invalid system `JAVA_HOME` because the bundled runtime is used.

See:

- [Architecture](docs/ARCHITECTURE.md)
- [API reference](docs/API.md)
- [MCP / AI-agent integration](docs/MCP.md)
- [Change Safety Loop](docs/CHANGE_SAFETY.md)
- [Data & Privacy](docs/DATA_PRIVACY.md)
- [PR Intelligence](docs/PR_INTELLIGENCE.md)
- [Development guide](docs/DEVELOPMENT.md)
- [Current implementation status](docs/ENTERPRISE_ROADMAP.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## License

CodeContext is released under the MIT License. See [LICENSE](LICENSE).