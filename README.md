# CodeContext

**Engineering evidence and verification for software built with humans and AI agents.**

CodeContext is a Kotlin/JVM CLI and local REST application that turns Java/Kotlin repositories, dependency structure, Git history, and engineering signals into deterministic intelligence. It adds grounded repository Q&A and evidence-backed engineering planning without making an external AI model the source of repository truth.

## Why CodeContext exists

AI coding systems can generate code very quickly. The harder engineering questions remain:

- What does this change actually affect?
- Which architecture boundaries does it cross?
- Which hidden dependencies, owners, migrations, or policies are involved?
- What evidence supports the proposed plan?
- What must CI verify before the change is trusted?

CodeContext is designed to answer those questions with **evidence, history, and verification**.

## Current status

- Java and Kotlin repository analysis.
- Dependency graph, PageRank hotspots, cycles, Git history, and learning paths.
- Deterministic Change Impact and PR Intelligence.
- Architecture Intelligence.
- Versioned grounded evidence and repository Q&A retrieval.
- Evidence-backed engineering planning.
- Optional AI reasoning over bounded repository evidence.
- Local Ktor REST API with path validation and rate limiting.
- Linux and Windows CI verification, including end-to-end intelligence flows.

## Core capabilities

| Capability | What it answers |
| --- | --- |
| Repository Intelligence | What is in this system and how is it connected? |
| Change Intelligence | What can this change affect? |
| PR Intelligence | What should reviewers inspect and test? |
| Architecture Intelligence | Where are boundaries, cycles, and structural risks? |
| Grounded Evidence | Which repository facts support a conclusion? |
| Repository Q&A | What evidence answers a developer's question? |
| Engineering Planner | What should be changed and verified, based on evidence? |
| AI Reasoning | How can a model explain or synthesize those facts? |

## Quick start

```bash
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext
./gradlew clean test
./gradlew installDist
./build/install/codecontext/bin/codecontext analyze .
```

The default report is written to `output/index.html`.

## CLI

```bash
# Repository analysis
codecontext analyze /path/to/repository

# Change impact
codecontext impact /path/to/repository src/main/Service.kt --json

# PR/change intelligence
codecontext pr-intelligence /path/to/repository --json
codecontext pr-intelligence /path/to/repository --base main --head feature/my-change --json

# Architecture analysis
codecontext architecture /path/to/repository --json

# Grounded repository evidence for a question
codecontext repo-qa "why is PaymentService risky?" --path /path/to/repository

# Evidence-backed engineering plan
codecontext plan "add payment validation" --evidence output/grounded-evidence.json

# Optional AI assistance
codecontext ai-assistant /path/to/repository

# Evolution analysis
codecontext evolution /path/to/repository

# Local REST server
codecontext server --host 127.0.0.1 --port 8080
```

Use `codecontext <command> --help` for the installed version's complete options.

## Architecture

```text
                 Repository + Git
                        ↓
             Scanner / Parsers / Git
                        ↓
                 Dependency Graph
                        ↓
              Analysis Snapshot
                        ↓
        ┌───────────────┴────────────────┐
        │ Deterministic Intelligence     │
        │ Risk · Impact · PR · Arch      │
        │ Evolution · Tests · Ownership  │
        └───────────────┬────────────────┘
                        ↓
                 Grounded Evidence
                    ↙        ↘
          Repository Q&A     Planner
                    ↘        ↙
                  Optional AI
                        ↓
              Developer / CI / PR
```

**Design rule:** deterministic evidence first, AI reasoning second.

## Configuration

Create a local configuration file:

```bash
cp .codecontext.json.template .codecontext.json
```

Important settings include `excludePaths`, `maxFilesAnalyze`, `gitCommitLimit`, caching/parallel parsing, reporting limits, AI configuration, and rate limiting. Never commit API keys.

## REST API

The local API provides health, analysis, report, change-impact, PR Intelligence, and organization analysis capabilities. Remote repository URLs are deliberately rejected by the current local endpoints.

See [docs/API.md](docs/API.md).

For remote or enterprise deployment, add authentication, authorization, TLS, trusted-origin controls, quotas, tenant isolation, and report retention at the deployment boundary.

## Development

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

CI is the authoritative execution environment for the project. It validates builds, tests, CLI flows, server health, intelligence artifacts, and end-to-end behavior.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [API reference](docs/API.md)
- [PR Intelligence](docs/PR_INTELLIGENCE.md)
- [Development guide](docs/DEVELOPMENT.md)
- [Enterprise roadmap](docs/ENTERPRISE_ROADMAP.md)
- [2027–2028 roadmap](docs/ROADMAP_2027_2028.md)
- [Product strategy](docs/PRODUCT_STRATEGY.md)
- [2027–2028 product vision](docs/VISION_2027_2028.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## 2027–2028 direction

CodeContext is deliberately **not** trying to become another generic coding chatbot. The long-term product is an engineering evidence platform built around problems that become more important as AI writes more code:

- Change Proof — trace a change from impact through verification.
- Architecture Constitution — executable architecture and governance rules.
- Counterfactual Change Simulation — reason about dependency and failure scenarios before merge.
- Migration Guardian — detect compatibility and rollout hazards.
- Knowledge Continuity Graph — detect ownership and institutional-knowledge gaps.
- AI Change Provenance — record requirement → evidence → plan → patch → CI → review → merge.
- Agent Firewall — mechanically constrain autonomous engineering actions.
- Production-to-Code Feedback — connect runtime symptoms to code and recent changes when telemetry is available.
- Organization Engineering Digital Twin — versioned cross-repository architecture and ownership intelligence.

See [docs/VISION_2027_2028.md](docs/VISION_2027_2028.md) for the full strategy.

## License

CodeContext is released under the MIT License. See [LICENSE](LICENSE).
