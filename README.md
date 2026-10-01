# CodeContext

CodeContext is a Kotlin/JVM command-line and local REST application for understanding Java and Kotlin codebases. It scans source files, extracts dependency metadata, analyzes graph centrality and Git history, and produces deterministic engineering intelligence plus an interactive HTML report.

## Status

- Java and Kotlin analysis is supported.
- CLI analysis and local REST serving are supported.
- Reports and machine-readable intelligence artifacts are generated locally under `output/`.
- Change Impact Intelligence and PR Intelligence are deterministic and provider-independent.
- AI assistance is optional and sends only the configured prompt context to the selected provider.
- The REST server accepts local paths within configured workspace roots. Remote repository URLs are not accepted by the current server endpoints.

## Features

- Dependency graph construction from package and import metadata
- PageRank-based knowledge hotspots
- Deterministic engineering-risk signals
- Change-impact and dependency blast-radius analysis
- PR Intelligence for Git diffs: risk, architecture, tests, and change-size signals
- Versioned JSON intelligence artifacts suitable for CI and future AI grounding
- Cycle detection and dependency visualization
- Learning-path generation for onboarding
- Git authorship, churn, and recent-change metadata
- Configuration-driven source exclusions and file-count limits
- Parallel parsing with content-based caching and atomic cache writes
- Optional AI analysis through configured Gemini or Anthropic providers
- Local Ktor REST API with rate limiting and path validation

## Requirements

- JDK 21 or newer
- Git for Git-history analysis
- Network access only when optional AI features are enabled

## Quick start

```bash
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext
./gradlew clean test
./gradlew installDist
./build/install/codecontext/bin/codecontext analyze .
```

The default CLI report is written to `output/index.html`.

## CLI commands

```bash
# Analyze a repository
./build/install/codecontext/bin/codecontext analyze /path/to/repository

# Analyze current working-tree changes
./build/install/codecontext/bin/codecontext pr-intelligence /path/to/repository --json

# Analyze two Git revisions
./build/install/codecontext/bin/codecontext pr-intelligence /path/to/repository \
  --base main --head feature/my-change --json

# Analyze explicit changed files
./build/install/codecontext/bin/codecontext impact /path/to/repository src/main/Service.kt --json

# Start the local API server
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080

# Generate AI-assisted insights, when configured
./build/install/codecontext/bin/codecontext ai-assistant /path/to/repository

# Inspect repository evolution
./build/install/codecontext/bin/codecontext evolution /path/to/repository
```

PR Intelligence writes `output/pr-intelligence.json` when `--json` is supplied. See [PR Intelligence](docs/PR_INTELLIGENCE.md) for the data model and rules.

The exact command options are available through:

```bash
./build/install/codecontext/bin/codecontext --help
./build/install/codecontext/bin/codecontext pr-intelligence --help
./build/install/codecontext/bin/codecontext impact --help
```

## Configuration

Copy the template into the repository you want to analyze or create a `.codecontext.json` file in the current working directory:

```bash
cp .codecontext.json.template .codecontext.json
```

Important configuration fields include:

| Field | Default | Purpose |
| --- | ---: | --- |
| `excludePaths` | standard build and tool directories | Directory names excluded during scanning |
| `maxFilesAnalyze` | `5000` | Maximum number of source files per analysis |
| `gitCommitLimit` | `1000` | Git history limit used by Git analysis |
| `enableCache` | `true` | Enables parse-result caching in supported CLI flows |
| `enableParallel` | `true` | Enables parallel parsing where supported |
| `hotspotCount` | `15` | Number of hotspots used by reporting flows |
| `learningPathLength` | `20` | Maximum learning-path length |
| `ai.enabled` | `false` | Enables optional AI analysis |
| `ai.provider` | `anthropic` | `anthropic` or `gemini` |
| `ai.apiKey` | empty | Provider credential; never commit it |
| `ai.model` | provider-specific | Provider model identifier |

The server also supports `CODECONTEXT_ALLOWED_PATHS`. Do not place API keys in source control, reports, logs, or issue descriptions.

## REST server

Start the server locally:

```bash
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
```

The server exposes health, analysis, change-impact, and PR Intelligence routes. See [docs/API.md](docs/API.md) and [docs/PR_INTELLIGENCE.md](docs/PR_INTELLIGENCE.md).

The server is intended to run behind an authenticated, trusted deployment boundary. It does not provide user authentication, tenant isolation, report expiration, or multi-tenant authorization by itself.

## Architecture

```text
Source + Git
    ↓
RepositoryScanner → Parser → Git metadata
    ↓
Unified dependency graph
    ↓
┌───────────────────────────────────────┐
│ deterministic intelligence            │
│                                       │
│ PageRank / Risk / Change Impact       │
│ Architecture / Test / PR Intelligence │
└───────────────────────────────────────┘
    ↓
Versioned evidence JSON
    ↓
CLI / REST / CI
    ↓
Future: GitHub integration + grounded AI
```

The key design principle is **deterministic evidence first, AI reasoning second**. AI should explain or reason over computed repository facts rather than replace them.

## Development

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

GitHub Actions additionally verifies CLI startup, CodeContext self-analysis, generated intelligence artifacts, PR Intelligence CLI and REST flows, and server health. The CI pipeline is the authoritative execution environment for the project.

See:

- [Architecture](docs/ARCHITECTURE.md)
- [API reference](docs/API.md)
- [PR Intelligence](docs/PR_INTELLIGENCE.md)
- [Development guide](docs/DEVELOPMENT.md)
- [Enterprise roadmap](docs/ENTERPRISE_ROADMAP.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## Roadmap

- Architecture rules and drift detection
- GitHub PR comments and status checks
- Configurable engineering governance policies
- Grounded AI repository Q&A and evidence citations
- AI-assisted change planning and PR review
- Incremental and watch-mode analysis
- Ownership and knowledge-concentration intelligence
- Cross-repository impact analysis
- TypeScript, JavaScript, Python, and Go parsers
- IDE integrations and package/container distribution

## License

CodeContext is released under the MIT License. See [LICENSE](LICENSE).
