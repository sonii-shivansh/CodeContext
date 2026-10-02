# Getting Started

> The shortest path from a fresh checkout to a useful Vericore analysis.

## Who this is for

Use this guide if you are new to Vericore and want to:

1. build it,
2. analyze a repository,
3. inspect engineering reality,
4. prepare and verify a change, or
5. integrate Vericore with an AI agent.

For architecture and contribution work, continue with the [documentation hub](INDEX.md).

## Prerequisites

- JDK 21+
- Git
- Bash, PowerShell, or another shell supported by the Gradle wrapper

Released platform archives bundle a Java runtime, so a separate JDK is not required for normal end-user use.

## 1. Build from source

```bash
git clone https://github.com/sonii-shivansh/Vericore.git
cd Vericore
./gradlew --no-daemon clean test
./gradlew --no-daemon installDist
```

The installed CLI is available at:

```text
build/install/vericore/bin/codecontext
```

Check the installation:

```bash
./build/install/vericore/bin/codecontext --version
./build/install/vericore/bin/codecontext --help
```

## 2. Analyze a repository

From the repository you want to understand:

```bash
codecontext analyze .
```

The default HTML report is written to `output/index.html`.

For machine-readable engineering state:

```bash
codecontext reality . --json
```

## 3. Ask repository questions

Grounded repository Q&A uses deterministic evidence before optional AI reasoning:

```bash
codecontext repo-qa "Why is PaymentService risky?" --path .
```

AI is optional. See [Data & Privacy](DATA_PRIVACY.md) before enabling provider-backed features.

## 4. Prepare a change safely

Create a repository-bound engineering plan and persisted change contract:

```bash
codecontext prepare "add payment validation"
```

This creates:

```text
output/engineering-context.json
output/engineering-plan.json
output/agent-change-contract.json
```

Make the code change with your normal workflow or coding agent. Then verify the original persisted contract:

```bash
codecontext verify \
  --plan output/engineering-plan.json \
  --contract output/agent-change-contract.json
```

Do not regenerate or replace the contract between `prepare` and `verify`.

Read [Change Safety](CHANGE_SAFETY.md) for the complete contract and failure semantics.

## 5. Run the local server

```bash
codecontext server --host 127.0.0.1 --port 8080
```

In another terminal:

```bash
curl --fail http://127.0.0.1:8080/health
```

Keep the server on loopback for local use. The application does not provide deployment-grade authentication, authorization, tenant isolation, or TLS.

See [API](API.md).

## 6. Connect an AI agent with MCP

Start the local MCP stdio server:

```bash
codecontext mcp
```

See [MCP](MCP.md) for the tool contract and safety model.

## Common next steps

| Goal | Read next |
|---|---|
| Understand the architecture | [Architecture](ARCHITECTURE.md) |
| Contribute code | [Development](DEVELOPMENT.md) |
| Understand agent-safe changes | [Change Safety](CHANGE_SAFETY.md) |
| Integrate REST | [API](API.md) |
| Integrate an AI agent | [MCP](MCP.md) |
| Understand repository state identity | [Engineering Reality](ENGINEERING_REALITY.md) |
| Understand data handling | [Data & Privacy](DATA_PRIVACY.md) |
| See implemented capabilities | [Implementation Status](ENTERPRISE_ROADMAP.md) |

## If something fails

Start with the exact command and its output. For CI-only failures, inspect the corresponding GitHub Actions job and artifact rather than guessing from a local result.

The project treats clean-environment GitHub Actions validation as the authoritative automated verification path.
