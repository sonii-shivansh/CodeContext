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

GitHub Actions is the authoritative validation environment. When local hardware is unavailable, use CI to validate the application and inspect workflow logs and artifacts before declaring a change complete.

## Run the CLI

```bash
./build/install/codecontext/bin/codecontext --help
./build/install/codecontext/bin/codecontext --version
./build/install/codecontext/bin/codecontext analyze .
./build/install/codecontext/bin/codecontext reality . --json
./build/install/codecontext/bin/codecontext repo-qa "why is this component risky?" --path . --evidence-output output/grounded-evidence.json
./build/install/codecontext/bin/codecontext plan "change the component" --evidence output/grounded-evidence.json --output output/engineering-plan.json
./build/install/codecontext/bin/codecontext prepare "change the component"
./build/install/codecontext/bin/codecontext verify --plan output/engineering-plan.json --contract output/agent-change-contract.json
```

A successful `prepare` produces repository-scoped context, plan, and Agent Change Contract artifacts under `output/`. Do not replace the persisted contract before `verify`.

## Local server

```bash
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
curl --fail http://127.0.0.1:8080/health
```

Keep the server bound to loopback for local development. Do not expose it publicly without authentication, authorization, TLS, trusted-origin controls, quotas, and report-retention controls.

## Configuration

Create a local configuration file from the template:

```bash
cp .codecontext.json.template .codecontext.json
```

`CodeContextConfig` controls exclusions, file limits, Git history, caching, parsing, reporting, AI, and rate limiting. Never commit credentials.

For server path validation, configure the narrowest practical value for `CODECONTEXT_ALLOWED_PATHS`.

## Project structure

```text
src/main/kotlin/com/codecontext/
  Main.kt
  cli/                       Clikt commands and application adapters
  core/
    ai/                      Grounded evidence and optional AI providers
    cache/                   Content-hash parse cache
    config/                  Configuration model and loader
    generator/               Learning/report helpers
    graph/                   Dependency graph and PageRank
    intelligence/            Analysis snapshots and deterministic intelligence
    parser/                  Java and Kotlin parsers
    planner/                 Evidence-backed engineering planning
    qa/                      Grounded repository Q&A retrieval
    reality/                 Repository-state identity composition
    scanner/                 Repository and Git scanning
    temporal/                Evolution analysis
    workflow/                Prepare/verify and change-safety contracts
  enterprise/                Multi-repository analysis
  mcp/                       Local MCP protocol and gateway
  output/                    Self-contained HTML report generation
  server/                    Ktor API and security controls
tests and verification       Unit, property, CLI, server, and E2E validation
docs/                        Architecture, API, development, PR, security, and status docs
```

## Engineering boundaries

- deterministic analysis is the source of repository facts;
- evidence is bounded and repository-relative;
- AI is optional and must not overwrite deterministic facts;
- planner output is read-only;
- Agent Change Contracts are deterministic verification artifacts, not authorization tokens;
- external input is validated at the boundary;
- source is not executed by analysis;
- public APIs must not expose stack traces, provider bodies, credentials, or unnecessary absolute server paths.

## Adding deterministic intelligence

1. Define a stable result contract.
2. Implement the signal under `core/`.
3. Make ordering and tie-breaking deterministic.
4. Add unit/property tests.
5. Convert important facts to grounded evidence where applicable.
6. Add CLI/REST/MCP adapters only after the core contract is stable.
7. Update API, architecture, MCP, and implementation-status documentation.

## Adding a parser

1. Implement `LanguageParser`.
2. Register it in `ParserFactory`.
3. Add representative tests, including malformed and empty files.
4. Update supported-language documentation.

## Adding an API route

1. Define serializable request and response models.
2. Validate size, path, revision, and content constraints before analysis.
3. Return a stable public error shape.
4. Avoid exposing local filesystem paths or internal exception messages.
5. Add route and security tests.
6. Update `docs/API.md`.

## Adding AI behavior

AI features follow the evidence-first boundary:

```text
Deterministic analysis
       ↓
Grounded evidence
       ↓
Bounded context
       ↓
AI reasoning
       ↓
Validated response
```

Do not introduce model calls directly into parsers, graph algorithms, or security boundaries. Provider-specific behavior belongs behind an abstraction.

## Reports and frontend assets

`ReportGenerator` uses kotlinx.html and embeds the graph data and visualization JavaScript directly into the generated HTML report. Treat source text, commit messages, author names, and descriptions as untrusted content. Changes to HTML or JavaScript serialization require escaping/regression tests. Generated reports must remain usable without a browser CDN dependency.

## Pull requests

Before opening a pull request:

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

Then run the complete GitHub Actions verification matrix and inspect the complete diff against `main`. Pull requests should describe behavior changes, security impact, configuration changes, schema changes, and validation results.

For AI-assisted features, additionally document evidence sources, model/provider boundaries, data exposure, uncertainty behavior, and CI verification requirements.

## Release checklist

1. Confirm the application version contract and `version` in `build.gradle.kts` agree.
2. Update `CHANGELOG.md` and `SECURITY.md` for the supported release line.
3. Run tests, packaging, CLI smoke tests, server smoke tests, cross-platform distribution smoke tests, and the clean-room live-repository E2E workflow.
4. Review generated artifacts, checksums, and dependency changes.
5. Review security and data-handling implications.
6. Confirm the Agent Change Contract live-gate mutation tests pass.
7. Tag and publish only from a reviewed, passing commit.
