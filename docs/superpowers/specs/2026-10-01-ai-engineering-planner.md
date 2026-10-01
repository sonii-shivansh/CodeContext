# AI Engineering Planner Design

## Goal

Generate deterministic, evidence-backed engineering plans for a proposed Git change or PR. The planner explains affected components, risks, architecture concerns, candidate tests, and implementation steps without modifying source code.

## Principles

- Deterministic repository analysis remains authoritative for repository facts.
- AI reasoning may explain or synthesize evidence but may not invent repository facts.
- Every factual plan item references evidence IDs where available.
- Unknown or insufficient evidence is explicit.
- The planner is read-only in its first release.
- The core service is provider-independent.

## Inputs

- repository path
- optional base and head Git revisions
- optional explicit changed files
- existing PR Intelligence, impact, architecture, risk, and grounded evidence outputs

## Plan model

A plan contains:

- schema version
- change summary
- affected components
- dependency/architecture concerns
- risk assessment
- implementation steps
- test recommendations
- verification commands
- evidence references
- uncertainties

Each step contains a stable ID, description, rationale, evidence IDs, and verification criteria.

## Planning pipeline

1. Validate repository and revisions.
2. Obtain deterministic PR/change intelligence.
3. Retrieve relevant grounded evidence.
4. Build a bounded planning context.
5. Produce a structured plan through the application service.
6. Validate that referenced evidence exists and that no step claims unsupported repository facts.
7. Serialize deterministic JSON.

## AI boundary

The planner must work without an AI provider by producing a deterministic baseline plan. An optional provider may enrich explanations, but provider output is validated against the evidence set before being returned.

## Security

- repository-relative paths only
- workspace containment validation
- no source execution
- no arbitrary network access
- bounded context and output sizes
- no credentials in prompts or artifacts
- validate Git revisions

## CLI / REST

Expose the same planner service through CLI and local REST adapters. Both return the same versioned structured plan contract.

## Testing

Cover unchanged revisions, explicit changed files, dependency impact, architecture findings, missing evidence, invalid revisions, path traversal, deterministic ordering, bounded context, malformed provider output, and CLI/REST parity.

## Non-goals

- automatic code modification
- automatic commits or pushes
- autonomous PR approval
- unrestricted LLM access to the repository
- treating model confidence as repository truth
