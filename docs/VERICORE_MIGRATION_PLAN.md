# Vericore Identity Migration Implementation Plan

Base: `main` at `025edaf9aeb5fab464d6f403a23b5d9f67a0d8b6`.

## Global constraints

- Work only on `refactor/vericore-identity-migration` until validation is complete.
- Do not modify application behavior unrelated to identity.
- Do not merge to `main` during implementation.
- Do not create chained migration PRs.
- Do not use blind global replacement.
- Keep genuine historical CodeContext references.
- Keep only necessary compatibility aliases, explicitly deprecated and documented.
- Treat the existing migration-executor workflow on main as migration scaffolding, not as permanent product behavior; remove it from the final migration branch unless a concrete release need proves otherwise.

## Phase 0 — Baseline and invariants

1. Inventory source modules, tests, docs, scripts, workflows, templates, packaging, Docker/container metadata, and external links.
2. Record current public CLI command names, REST routes, MCP tool semantics, configuration behavior, and generated-state contracts.
3. Search all old-name variants and classify each occurrence using the migration specification.
4. Preserve the exact behavior contract of commands and APIs.

Gate: no implementation changes beyond the migration specification/plan documentation.

## Phase 1 — Namespace and build identity

1. Rename `src/main/kotlin/com/codecontext` to `com/vericore` logically through file/package changes.
2. Rename product-specific Kotlin types such as `CodeContextConfig`, `CodeContextException`, and `CodeContextServer` where the type name itself is product identity.
3. Update all imports/package declarations.
4. Update Gradle group, root project name, main class, JAR manifest, and distribution identity.
5. Update tests to the new namespace.

Validation: compile/test immediately after namespace/build changes.

## Phase 2 — CLI identity and compatibility

1. Make `vericore` the canonical Clikt root command and distribution executable.
2. Update help/version/application identity.
3. Preserve a temporary `codecontext` compatibility entry only if the Gradle/application distribution can support it cleanly without duplicating application behavior.
4. The compatibility entry emits a migration warning and points to `vericore`.
5. Add tests for canonical and compatibility invocation.

Validation: `vericore --help`, `vericore --version`, every registered subcommand help, and compatibility invocation.

## Phase 3 — Configuration and generated state

1. Introduce canonical `.vericore.json`, `.vericore/`, and `.vericore-architecture-contract.json` identifiers.
2. Introduce canonical `VERICORE_*` environment variables and `vericore.config.home`.
3. Read legacy CodeContext config/env/property identifiers only as temporary fallbacks.
4. Emit non-fatal deprecation warnings when legacy configuration is actually selected.
5. Document migration precedence and removal path.
6. Ensure generated-state/repository-integrity logic treats both canonical generated state and transitional legacy state correctly.

Validation: canonical configuration tests, legacy fallback tests, warning tests, generated-state tests, repository-integrity tests.

## Phase 4 — REST/server identity

1. Rename server classes to Vericore equivalents.
2. Update server startup metadata and user-visible identity.
3. Preserve existing HTTP paths and request/response schemas.
4. Update server tests and startup smoke tests.

Validation: startup, health/status metadata, representative endpoints, and contract tests.

## Phase 5 — MCP and agent identity

1. Rename MCP server metadata/instructions to Vericore.
2. Make `vericore_*` tool names canonical.
3. Retain `codecontext_*` only as thin deprecated aliases where required for compatibility.
4. Emit migration warnings through a channel that cannot corrupt MCP stdout JSON-RPC responses.
5. Keep tool schemas and semantics unchanged.
6. Update agent prompts/templates and documentation.

Validation: MCP initialize, tools/list, canonical tools/call, compatibility tools/call, JSON-RPC framing, and no branding leakage in normal output.

## Phase 6 — Documentation, scripts, CI/CD, packaging, containers

1. Update README/docs/current examples to Vericore.
2. Preserve historical changelog references.
3. Update scripts and workflow distribution paths.
4. Update badges and repository links after the repository target is verified.
5. Update Docker/container product metadata where present.
6. Remove temporary migration-executor workflow and phase artifacts from the final product branch.
7. Update security/configuration documentation with canonical names and a compatibility migration section.

Validation: documentation/reference audit, workflow syntax/static inspection, packaging path checks.

## Phase 7 — Final repository-wide audit

Search separately for:

- `CodeContext`
- `codecontext`
- `CODECONTEXT`
- `Code Context`
- `code-context`
- `code_context`
- `com.codecontext`
- `sonii-shivansh/CodeContext`
- `build/install/codecontext`
- `.codecontext`
- `codecontext_*`
- `codecontext.config.home`

For every remaining occurrence, record: file, reason, classification, and removal target/date if compatibility.

## Phase 8 — Full validation

Run, in order:

1. full Gradle build;
2. unit tests;
3. integration tests if configured;
4. static/lint checks if configured;
5. `installDist`;
6. canonical CLI smoke tests for every command;
7. compatibility CLI smoke test if implemented;
8. REST/server startup and representative endpoint tests;
9. MCP initialize/list/call tests;
10. package/distribution validation;
11. release audit;
12. live output-quality audit;
13. final architecture/repository gate;
14. final old-name search.

Any failure is fixed on the migration branch and the affected validation is rerun before proceeding.

## Phase 9 — GitHub repository rename

Only after all validation is green:

1. Verify repository administration capability.
2. Rename `sonii-shivansh/CodeContext` to `sonii-shivansh/Vericore` if tooling permits.
3. Verify the new repository URL and metadata live.
4. Verify redirects/links where possible.
5. Update the local remote URL only after the new location is confirmed.
6. Re-run external-reference audit against the renamed repository.

If repository administration is unavailable, report the exact manual GitHub Settings action and do not claim the rename happened.

## Phase 10 — Final release gate

Perform one fresh whole-repository review of the migration branch. Confirm no feature behavior changed, compatibility paths are bounded and documented, current output uses Vericore, historical references are preserved, and all validation evidence is green.

Only then create a single migration PR to `main`. No chained migration PRs.
