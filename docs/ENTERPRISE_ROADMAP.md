# CodeContext — Current Implementation Status

> This document describes what is implemented today. It intentionally does not present future ideas as shipped functionality.

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
- evidence-first `prepare → change → verify` workflow
- deterministic change-safety scope verification
- repository-bound Agent Change Contract with SHA-256 fingerprint
- persisted-contract validation for repository identity, prepared Git `HEAD`, plan binding, and tamper detection

## Architecture intelligence

Implemented:

- architecture-oriented findings over the dependency graph
- cycle/boundary signals
- deterministic architecture artifacts
- architecture drift comparison against an explicit baseline artifact
- deterministic drift reporting for findings, cycles, and layer counts
- deterministic architecture contracts for CI/governance enforcement
- `architecture-contract` CLI command
- contract templates with explicit finding/cycle/severity limits
- deterministic architecture contract decision records with repository commit, contract/result digests, and stable decision IDs
- idempotent contract decision history persistence via `--record`
- CI end-to-end verification of architecture analysis, drift, and contract evaluation

## Engineering context

Implemented:

- versioned deterministic engineering-context snapshot contract
- repository-relative file fingerprints
- repository `HEAD` capture when Git metadata is available
- working-tree dirty/change-path state
- stable snapshot digest
- deterministic snapshot-to-snapshot diffing for added, removed, and modified files
- `context-snapshot` CLI command
- `context-diff` CLI command
- clean-environment CI verification of snapshot and zero-drift diff behavior

## Engineering Reality

Implemented:

- versioned `EngineeringRealitySnapshot` contract
- deterministic composition of analysis + repository-context identities
- repository commit binding when Git metadata is available
- stable analysis/context digests
- explicit composite `realityDigest`
- protection against analysis wall-clock timestamps changing the identity
- `reality` CLI command
- repository-local `output/engineering-reality.json` artifact
- unit tests covering stable and state-changing reality identities

Engineering Reality is deliberately a composition boundary. It does not add model-generated claims or autonomous repository mutation.

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
- contract fingerprint binding
- bounded input/output
- `plan` CLI command

The current planner is read-only and does not modify repositories.

## Agent Change Contract

Implemented:

- versioned repository-bound `AgentChangeContract`
- canonical repository identity
- prepared Git `HEAD` binding when Git metadata is available
- planned-path and expected-component scope
- verification-command and evidence binding
- architecture expectation binding
- deterministic SHA-256 fingerprint
- persisted `output/agent-change-contract.json` artifact from `prepare`
- CLI verification against the persisted contract
- tamper, plan-mismatch, repository-mismatch, and stale-HEAD detection
- live release-gate mutation tests for contract tampering and unexpected source changes

Current boundary: the core workflow retains a compatibility overload that can derive a contract from a supplied plan, and the MCP `codecontext_get_change_contract` helper currently generates a fresh contract rather than retrieving a persisted artifact. These paths are explicitly treated as compatibility/preview behavior and are candidates for further hardening.

## Engineering provenance

Implemented:

- versioned `DecisionProvenance` contract
- stable SHA-256 provenance IDs
- repository `HEAD` capture through read-only Git metadata when available
- explicit unknown-Git state instead of fabricated provenance
- provenance attached to prepare and verify workflow results
- architecture contract decision records with idempotent persistence

## Temporal intelligence

Implemented:

- Git-history evolution analysis
- deterministic time-based commit sampling
- exact committed Java/Kotlin/Kotlin-script source line counts read directly from Git objects
- cumulative source-file change-frequency hotspots
- deterministic fallback hotspots based on historical file size when no touch history exists
- safe analysis of dirty working trees without destructive checkout operations
- no mutation of the repository working tree during temporal analysis

Temporal analysis currently provides source-history metrics; it does not reconstruct full semantic dependency graphs for arbitrary historical commits.

## AI integration

Implemented:

- optional AI provider abstraction
- configured Gemini/Anthropic provider support in the existing AI assistant flow
- grounded evidence passed as bounded context
- explicit separation between deterministic repository facts and model reasoning

AI is disabled unless configured.

## MCP integration

Implemented:

- local stdio JSON-RPC MCP server
- repository analysis, impact, architecture, PR Intelligence
- Engineering Reality and context snapshot/diff tools
- architecture drift and contract tools
- grounded evidence, prepare, change-safety, and verify tools
- local path-safety boundary and rejection of remote repository URLs

Current MCP boundary: the server is a trusted local integration without authentication or tenant isolation. The change-contract generation helper is not a replacement for the persisted contract used by verification.

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
- architecture-drift CLI verification
- architecture-contract CLI verification
- engineering-context snapshot/diff verification
- prepare/verify contract validation
- REST API end-to-end verification
- Linux x64 validation
- Windows x64 validation
- macOS x64 validation
- macOS ARM64 validation
- live-repository release-gate mutation and repository-immutability checks

The clean-environment workflows are the authoritative execution environment for release verification.

## Release line

The current `main` branch contains post-v0.6.0 engineering-intelligence work while the Gradle/application version remains **0.6.0**. These changes have not yet been published as a new release.

## Current limitations

The repository currently has important boundaries:

- analysis is focused on Java and Kotlin;
- Kotlin parsing has known complex-syntax limitations;
- remote repository URLs are not accepted by the local server endpoints;
- the REST server does not provide authentication or multi-tenant authorization;
- the planner is read-only;
- autonomous code modification is not implemented;
- production telemetry integrations are not implemented;
- organization-wide governance and cross-repository intelligence are not implemented;
- Engineering Reality currently composes analysis and repository-context artifacts but does not yet include the full semantic evidence graph;
- context snapshots currently fingerprint repository-scanned source files and do not yet capture the full semantic evidence graph;
- temporal archaeology provides deterministic source-history metrics but does not yet reconstruct full semantic dependency graphs for arbitrary historical commits;
- architecture contract history records deterministic evaluation decisions, but explicit human approval/exception workflows are not yet implemented;
- the current Agent Change Contract is a deterministic verification boundary, not an authorization system or autonomous coding mechanism.

This file should be updated when implementation changes materially. It should not describe unimplemented features as if they already exist.
