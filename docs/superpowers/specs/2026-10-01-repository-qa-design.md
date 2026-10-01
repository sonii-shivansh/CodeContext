# Grounded Repository Q&A Design

## Goal

Provide repository questions and answers using deterministic CodeContext evidence rather than unconstrained source-code prompting.

## Principles

- Deterministic analyzers remain the source of repository facts.
- Retrieval is bounded, explainable, and repository-relative.
- Every factual answer can point to evidence IDs.
- Unknown or insufficient evidence is explicit.
- AI is an optional reasoning provider behind a stable interface.
- No source code or repository data is sent to an external provider unless the configured provider is explicitly enabled.

## Question intents

Initial intents:

- dependency explanation
- risk explanation
- impact explanation
- architecture explanation
- PR/change explanation
- test recommendation
- general repository question

Unknown questions must return a structured insufficient-evidence response rather than fabricated facts.

## Retrieval

Retrieve from existing deterministic outputs: dependency graph, risk, impact, architecture findings, PR intelligence, and grounded evidence. Rank by intent relevance, direct entity/path match, evidence severity, and deterministic tie-breakers. Apply strict result and context-size limits.

## Answer contract

Represent an answer with:

- question
- intent
- answer text
- evidence IDs
- known facts
- derived conclusions
- uncertainties
- provider metadata

Provider output must never create new repository facts without supporting evidence.

## CLI and REST

Expose repository Q&A through a CLI command and REST endpoint using the same application service. Both return deterministic structured retrieval results even when no AI provider is configured.

## Security

- repository-relative paths only
- workspace containment validation
- no secret/token retrieval
- no arbitrary URL fetching
- no source execution
- bounded question length
- bounded evidence/context size

## Testing

Cover exact entity retrieval, intent classification, deterministic ordering, context limits, unknown questions, path traversal, malformed input, and evidence citation preservation. Add CLI and REST end-to-end tests.

## Non-goals

- autonomous code modification
- unrestricted LLM access to the repository
- provider-specific business logic in core analysis
- claiming facts without evidence
