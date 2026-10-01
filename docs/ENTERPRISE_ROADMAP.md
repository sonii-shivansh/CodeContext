# CodeContext — Current Implementation Status

This document describes what is implemented in the repository today. It is intentionally not a future product roadmap.

## Repository intelligence

Implemented:

- Java and Kotlin source discovery and parsing
- configurable source exclusions and file limits
- parallel parsing and content-based caching
- dependency graph construction
- wildcard-import handling
- cycle detection
- PageRank knowledge hotspots
- Git authorship, churn, modification-time, and recent-change analysis
- learning-path generation
- HTML reporting

## Change and PR intelligence

Implemented:

- dependency-aware change impact analysis
- changed-file analysis
- blast-radius signals
- deterministic PR Intelligence
- change-size and risk signals
- architecture findings
- test recommendations/signals
- machine-readable intelligence artifacts

## Architecture intelligence

Implemented:

- architecture-oriented findings over the dependency graph
- cycle/boundary signals
- deterministic architecture artifacts
- CI end-to-end verification of the architecture flow

## Grounded evidence

Implemented:

- versioned evidence contracts
- `EvidenceCitation`
- bounded evidence generation
- repository-relative citation paths
- deterministic citation ordering
- evidence size limits
- safeguards against evidence paths escaping the repository

## Repository Q&A

Implemented:

- repository question intent classification
- entity/path extraction
- dependency, risk, impact, architecture, PR, and test-oriented retrieval intents
- deterministic evidence ranking
- bounded result sets
- explicit insufficient-evidence handling
- `repo-qa` CLI command

The first implementation does not require an external model.

## Engineering Planner

Implemented:

- versioned engineering-plan model
- deterministic plan synthesis from grounded evidence
- affected components
- risk
- implementation steps
- evidence IDs
- verification criteria
- uncertainty handling
- bounded input/output
- `plan` CLI command

The current planner is read-only and does not modify repositories.

## AI integration

Implemented:

- optional AI provider abstraction
- configured Gemini/Anthropic provider support in the existing AI assistant flow
- grounded evidence passed as bounded context
- explicit separation between deterministic repository facts and model reasoning

AI is disabled unless configured.

## Local REST API

Implemented:

- service banner
- health/readiness/liveness routes
- local repository analysis
- generated report serving
- repository question/AI flow
- organization analysis with bounded concurrency
- change-impact and PR Intelligence flows
- path validation
- rate limiting
- sanitized public errors

The current server is intended for trusted local/internal use and does not itself provide authentication, authorization, tenant isolation, or deployment-level TLS.

## CI and verification

Implemented CI verification includes:

- JVM compilation and tests
- lint/code-quality checks
- CLI verification
- generated artifact validation
- self-analysis
- PR Intelligence end-to-end verification
- Architecture Intelligence end-to-end verification
- REST API end-to-end verification
- Linux and Windows validation

CI is the authoritative execution environment for the repository.

## Current limitations

The repository currently has important boundaries:

- analysis is focused on Java and Kotlin;
- Kotlin parsing has known complex-syntax limitations;
- remote repository URLs are not accepted by the local server endpoints;
- the REST server does not provide authentication or multi-tenant authorization;
- the planner is read-only;
- autonomous code modification is not implemented;
- production telemetry integrations are not implemented;
- organization-wide governance and cross-repository intelligence are not implemented.

This file should be updated when implementation changes materially. It should not describe unimplemented features as if they already exist.
