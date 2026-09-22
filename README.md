# CodeContext

CodeContext is a Kotlin/JVM command-line and local REST application for understanding Java and Kotlin codebases. It scans source files, extracts dependency metadata, analyzes graph centrality and Git history, and produces an interactive HTML report to support onboarding, architecture review, and knowledge-risk analysis.

## Status

- Java and Kotlin analysis is supported.
- CLI analysis and local REST serving are supported.
- Reports are generated locally under `output/`.
- AI assistance is optional and sends only the configured prompt context to the selected provider.
- The REST server accepts local paths within configured workspace roots. Remote repository URLs are not accepted by the current server endpoint.

## Features

- Dependency graph construction from package and import metadata
- PageRank-based knowledge hotspots
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

The default CLI report is written to:

```text
output/index.html
```

Open it with the appropriate command for your platform:

```bash
open output/index.html       # macOS
xdg-open output/index.html   # Linux
start output/index.html      # Windows
```

## CLI commands

```bash
# Analyze a repository
./build/install/codecontext/bin/codecontext analyze /path/to/repository

# Start the local API server
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080

# Generate AI-assisted insights, when configured
./build/install/codecontext/bin/codecontext ai-assistant /path/to/repository

# Inspect repository evolution
./build/install/codecontext/bin/codecontext evolution /path/to/repository
```

The exact command options are available through:

```bash
./build/install/codecontext/bin/codecontext --help
./build/install/codecontext/bin/codecontext analyze --help
./build/install/codecontext/bin/codecontext server --help
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

The server also supports these environment variables:

- `CODECONTEXT_ALLOWED_PATHS`: path-separated roots accepted by server path validation.
- `CODECONTEXT_ALLOWED_GIT_HOSTS`: reserved for deployments that add a controlled remote-clone implementation.

Do not place API keys in source control, reports, logs, or issue descriptions.

## REST server

Start the server locally:

```bash
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
```

The server exposes health endpoints and local analysis routes. See [docs/API.md](docs/API.md) for request and response details.

The server is intended to run behind an authenticated, trusted deployment boundary. It does not provide user authentication, tenant isolation, report expiration, or multi-tenant authorization by itself.

## Architecture

```text
src/main/kotlin/com/codecontext/
  Main.kt                    Application entry point
  cli/                       Clikt commands and parallel parsing
  core/
    ai/                      Optional AI provider integration
    cache/                   Content-addressed parse cache
    config/                  JSON configuration loading
    generator/               Learning-path generation
    graph/                   Dependency graph and PageRank
    parser/                  Java AST and Kotlin parsing
    scanner/                 Source discovery and Git analysis
    temporal/                Codebase evolution analysis
  enterprise/                Multi-repository analysis and licensing
  output/                    HTML and graph report generation
  server/                    Ktor routes, validation, and rate limiting
src/test/kotlin/              Unit, integration, security, and verification tests
docs/                         Architecture, API, and development documentation
```

The main analysis pipeline is:

```text
CLI or REST request
  -> configuration and path validation
  -> RepositoryScanner
  -> CodeParallelParser
  -> OptimizedGitAnalyzer
  -> RobustDependencyGraph
  -> LearningPathGenerator
  -> ReportGenerator
```

## Development

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

The GitHub verification workflow additionally checks CLI startup, self-analysis, generated report content, and server startup.

See:

- [Architecture](docs/ARCHITECTURE.md)
- [API reference](docs/API.md)
- [Development guide](docs/DEVELOPMENT.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)
- [Changelog](CHANGELOG.md)

## Roadmap

- TypeScript, JavaScript, Python, and Go parsers
- More accurate Kotlin parsing using a dedicated syntax model
- Incremental and watch-mode analysis
- Report retention and authenticated deployment support
- Plugin APIs for custom analyzers
- IDE integrations
- Package-manager and container distribution

## License

CodeContext is released under the MIT License. See [LICENSE](LICENSE).
