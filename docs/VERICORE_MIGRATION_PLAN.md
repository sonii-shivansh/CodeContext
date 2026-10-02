# Vericore Migration Implementation Plan

## Phase 0 — Baseline and safety
1. Freeze `main` as the migration baseline.
2. Work only on the migration branch until validation passes.
3. Inventory old identifiers and classify them as CANONICAL, COMPATIBILITY, HISTORICAL, EXTERNAL, or UNKNOWN.

## Phase 1 — Canonical application identity
1. Rename Gradle project/group/application entry point to Vericore.
2. Rename Kotlin namespace from `com.codecontext` to `com.vericore`.
3. Rename identity-bearing classes/files where appropriate.
4. Update imports, reflection, service metadata, tests, fixtures, and generated references.

## Phase 2 — CLI and configuration
1. Make `vericore` canonical.
2. Update help, version, startup, errors, reports, and normal logs.
3. Introduce canonical `.vericore*` configuration/state names.
4. Retain required legacy `.codecontext*` paths only as deprecated bridges.
5. Add non-fatal migration warnings without contaminating structured stdout.
6. Document removal strategy.

## Phase 3 — MCP, REST, AI and agents
1. Update server metadata/prompts to Vericore.
2. Introduce canonical `vericore_*` MCP names.
3. Retain old MCP names only when compatibility evidence warrants it, as aliases.
4. Preserve REST functional routes and schemas; update identity metadata only.
5. Update agent prompts/tool descriptions/templates.

## Phase 4 — Tooling, packaging and infrastructure
1. Update scripts, Gradle tasks, installDist paths, Docker metadata, CI workflows, audit workflows, release scripts, and examples.
2. Update internal GitHub links after repository rename is verified.
3. Treat the separate website repository as external.

## Phase 5 — Documentation
1. Update current documentation to Vericore.
2. Add migration/deprecation documentation.
3. Preserve factual historical CodeContext references.

## Phase 6 — Validation
1. Repository-wide search for all old-name variants.
2. Full build and all tests.
3. `installDist` and installed CLI verification.
4. Canonical Vericore CLI smoke suite.
5. Legacy compatibility smoke suite where retained.
6. REST startup/endpoints.
7. MCP initialize/list/call.
8. Release/package validation.
9. Static/lint/CI validation.
10. Actual command-output audit for every command.
11. Repository-integrity and architecture gate.
12. Final old-reference classification.

## Phase 7 — GitHub repository rename
1. Verify migration and checks first.
2. Rename repository to `sonii-shivansh/Vericore` only through GitHub repository settings if supported.
3. Verify metadata, clone URLs, workflows, badges, and docs.
4. Update remotes only after verifying the new canonical URL.

## Phase 8 — Final release gate
Migration is complete only when canonical identity is Vericore, required compatibility is deprecated/tested, all builds/tests/packages pass, CLI/REST/MCP work, actual outputs are correct, all remaining old references are classified, repository state is verified, and final release/architecture gates pass.
