# Architecture Intelligence Design

## Goal

Add deterministic, explainable architecture analysis on top of the existing dependency graph, risk, impact, and PR intelligence engines.

## Principles

1. Deterministic evidence is the source of truth.
2. Architecture rules are configurable and repository-relative.
3. Findings contain stable rule IDs, severity, source/target evidence, and remediation context.
4. Existing graph/risk/impact models are reused rather than duplicated.
5. AI inference remains optional and is not used to decide CI outcomes.

## Scope

### Architecture model

Represent layers, path patterns, dependency-direction rules, exclusions, and rule configuration.

### Initial rules

- Forbidden layer dependency direction.
- Cross-boundary dependency.
- Dependency cycles.
- High-coupling architectural hotspots.
- Architecture drift between two analyzed revisions.

### Findings

Each finding contains:

- stable rule ID
- severity
- source path
- target path when applicable
- relationship type
- human-readable evidence
- deterministic confidence/category metadata

### CLI

Add an `architecture` command supporting repository analysis and JSON output. Revision comparison is available for drift analysis.

### REST

Add an architecture analysis endpoint using the same application service as the CLI.

### Configuration

Support optional repository architecture configuration. Without configuration, structural analysis still works using package/module relationships and graph evidence.

### Integration

Expose architecture evidence to PR Intelligence without duplicating graph traversal or risk calculation.

## Security

- Never execute source code.
- Normalize repository-relative paths.
- Reject paths outside the configured workspace.
- Do not expose absolute filesystem paths in JSON or findings.
- Validate Git revisions before analysis.
- Preserve existing rate limits and server path validation.

## Determinism

Sort paths, rules, findings, cycles, and serialized collections before output. Identical repository state and configuration must produce identical JSON.

## Testing

Create deterministic fixtures covering:

- clean layered architecture
- forbidden dependency
- circular dependency
- cross-module boundary
- architecture drift
- mixed Java/Kotlin

Test CLI JSON, REST JSON, exact findings, ordering, malformed configuration, invalid revisions, and path-security behavior.

## CI acceptance

The authoritative CI environment must run unit/property tests, build, CLI architecture E2E, REST architecture E2E, self-analysis, artifact validation, health/readiness checks, Windows build/tests, lint, and distribution build.

## Non-goals

- LLM-generated architecture as the source of truth.
- Remote repository fetching.
- Mandatory organization-wide governance blocking in the first release.
- Full language-specific semantic type analysis beyond the existing parser capabilities.
