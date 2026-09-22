# Development Guide

## Prerequisites

- JDK 21 or newer
- Git
- IntelliJ IDEA or another Kotlin-capable editor
- Bash, PowerShell, or a compatible shell for the Gradle wrapper

## Build and test

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build
./gradlew --no-daemon installDist
```

Run a focused test class:

```bash
./gradlew test --tests "com.codecontext.core.parser.ParserTest"
```

Run the installed CLI:

```bash
./build/install/codecontext/bin/codecontext --help
./build/install/codecontext/bin/codecontext analyze .
```

## Local server

```bash
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
curl --fail http://127.0.0.1:8080/health
```

Keep the server bound to loopback for local development. Do not expose it publicly without an authentication and authorization layer, TLS, trusted-origin policy, request quotas, and report-retention controls.

## Configuration

Create a local configuration file from the template:

```bash
cp .codecontext.json.template .codecontext.json
```

The file is intentionally local and must not contain a credential committed to Git. `CodeContextConfig` controls exclusions, file limits, Git history, caching, parsing, reporting, AI, and rate limiting.

For server path validation, use the narrowest practical value for:

```bash
export CODECONTEXT_ALLOWED_PATHS="$PWD:/tmp"
```

## Project structure

```text
src/main/kotlin/com/codecontext/
  Main.kt
  cli/                       Clikt commands and parallel parser
  core/
    ai/                      Optional provider integration
    cache/                   Content-hash parse cache
    config/                  Configuration model and loader
    generator/               Learning paths
    graph/                   PageRank and dependency graph
    parser/                  Java and Kotlin parsers
    scanner/                 Repository and Git scanning
    temporal/                Evolution analysis
  enterprise/                Multi-repository analysis
  output/                    HTML report generation
  server/                    Ktor API and security controls
src/test/kotlin/com/codecontext/
  core/                      Parser, graph, cache, property, and edge tests
  server/                    Rate-limit and path-security tests
  verification/              Backend verification tests
  E2ETest.kt                 End-to-end coverage
docs/                        Project documentation
```

## Code standards

- Follow the Kotlin coding conventions.
- Prefer structured concurrency over `runBlocking` inside suspend flows.
- Validate external input at the boundary.
- Do not return stack traces, provider bodies, absolute server paths, or credentials to clients.
- Add tests for security-sensitive behavior and public API changes.
- Keep public APIs documented with KDoc where practical.
- Keep generated reports, `.codecontext/`, build output, and local configuration out of commits.

## Adding a parser

1. Implement `LanguageParser`.
2. Register the parser in `ParserFactory`.
3. Add representative unit tests, including malformed and empty files.
4. Update the supported-language documentation.

## Adding an API route

1. Define serializable request and response models.
2. Validate size, path, and content constraints before analysis.
3. Return a stable public error shape.
4. Avoid exposing local filesystem paths or internal exception messages.
5. Add route and security tests.
6. Update `docs/API.md`.

## Reports and frontend assets

`ReportGenerator` uses kotlinx.html and embeds serialized graph data. Treat source text, commit messages, author names, and descriptions as untrusted content. Any changes to HTML or JavaScript serialization must include escaping/regression tests.

## Pull requests

Before opening a pull request:

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

Also run the Verification workflow and inspect the complete diff against `main`. Pull requests should describe behavior changes, security impact, configuration changes, and validation results.

## Release checklist

1. Update `version` in `build.gradle.kts`.
2. Update `CHANGELOG.md`.
3. Run tests, packaging, CLI smoke tests, and server smoke tests.
4. Review generated artifacts and dependency changes.
5. Tag and publish only from a reviewed, passing commit.
