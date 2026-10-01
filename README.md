# CodeContext

**Deterministic engineering intelligence for Java and Kotlin repositories.**

[🌐 Explore the CodeContext website](https://sonii-shivansh.github.io/CodeContext-Website/) · [📚 Documentation](https://github.com/sonii-shivansh/CodeContext/tree/main/docs) · [🚀 Releases](https://github.com/sonii-shivansh/CodeContext/releases)

CodeContext is a Kotlin/JVM CLI and local REST application that analyzes source code, dependency structure, Git history, and engineering signals to produce reproducible engineering intelligence. It also provides grounded repository Q&A and evidence-backed engineering planning.

## Current status

The current release candidate targets **v0.6.0**. The repository implements:

- Java and Kotlin source analysis
- dependency graph construction
- PageRank knowledge hotspots
- cycle and architecture analysis
- Git authorship, churn, and evolution analysis
- learning-path generation
- deterministic change-impact analysis
- deterministic PR Intelligence
- Architecture Intelligence
- versioned analysis/evidence artifacts
- grounded repository Q&A retrieval
- deterministic, evidence-backed engineering planning
- optional AI assistance over bounded repository-derived context
- local Ktor REST API
- path validation and rate limiting
- Linux and Windows CI verification

Generated reports are self-contained and do not require a browser CDN request for their visualization code.

## Design principle

**Deterministic evidence first, AI reasoning second.**

Repository facts are produced by deterministic analysis. Grounded evidence preserves those facts for downstream Q&A and planning. AI is optional and is not treated as the authoritative source of repository truth.

## Quick start

```bash
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext
./gradlew clean test
./gradlew installDist
./build/install/codecontext/bin/codecontext --version
./build/install/codecontext/bin/codecontext analyze .
```

The default report is written to `output/index.html` and can be opened without a network connection.

## CLI commands

```bash
# Repository analysis
codecontext analyze /path/to/repository

# Change impact
codecontext impact /path/to/repository src/main/Service.kt --json

# PR / change intelligence
codecontext pr-intelligence /path/to/repository --json
codecontext pr-intelligence /path/to/repository --base main --head feature/my-change --json

# Architecture intelligence
codecontext architecture /path/to/repository --json

# Grounded repository Q&A
codecontext repo-qa "why is PaymentService risky?" --path /path/to/repository

# Evidence-backed engineering plan
codecontext repo-qa "which files are the main architectural hotspots?" --path /path/to/repository > output/grounded-evidence.json
codecontext plan "add payment validation" --evidence output/grounded-evidence.json

# Optional AI assistance
codecontext ai-assistant /path/to/repository

# Git evolution
codecontext evolution /path/to/repository

# Local REST server
codecontext server --host 127.0.0.1 --port 8080
```

Use `codecontext <command> --help` for the exact options in the installed version.

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
Grounded Evidence
   ↙          ↘
Q&A          Planner
   ↘          ↙
 Optional AI reasoning
       ↓
 CLI / REST / CI
```

## Configuration

Create a local configuration file when needed:

```bash
cp .codecontext.json.template .codecontext.json
```

Important settings include source exclusions, file-count limits, Git history limits, caching, parallel parsing, reporting limits, AI configuration, and rate limiting. Never commit API keys.

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
./gradlew --no-daemon ktlintCheck
./gradlew --no-daemon build installDist
```

GitHub Actions is the project's authoritative clean-environment verification path. CI validates compilation, tests, code quality, CLI flows, generated artifacts, intelligence flows, server health, API input boundaries, and report portability.

See:

- [Architecture](docs/ARCHITECTURE.md)
- [API reference](docs/API.md)
- [Data & Privacy](docs/DATA_PRIVACY.md)
- [PR Intelligence](docs/PR_INTELLIGENCE.md)
- [Development guide](docs/DEVELOPMENT.md)
- [Enterprise implementation status](docs/ENTERPRISE_ROADMAP.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## License

CodeContext is released under the MIT License. See [LICENSE](LICENSE).
