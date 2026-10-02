# Vericore Migration Implementation Plan

## Phase 0 — Baseline and safety
1. Freeze `main` as the migration baseline.
2. Work only on `refactor/vericore-migration` until validation passes.
3. Capture current version, build coordinates, CLI identity, package roots, config/env contracts, generated-state paths, MCP metadata/tool names, REST metadata, release scripts, CI workflows, Docker metadata, docs, and external URLs.
4. Preserve a searchable inventory of all old-name references and classify each as CANONICAL, COMPATIBILITY, HISTORICAL, EXTERNAL, or UNKNOWN.

## Phase 1 — Canonical application identity
1. Rename Gradle project/group/application entry point to Vericore.
2. Rename Kotlin package namespace from `com.codecontext` to `com.vericore`.
3. Rename classes/files only where the class name is product identity; avoid gratuitous renames.
4. Update imports, reflection, service metadata, tests, fixtures, and generated references.

## Phase 2 — CLI and configuration
1. Make `vericore` the only canonical CLI identity.
2. Update help, version, startup, errors, reports, and normal logs to Vericore.
3. Introduce canonical `.vericore` configuration/state names.
4. Retain legacy `.codecontext*` configuration/state only where needed for migration.
5. Add deprecation warnings on legacy config/env/CLI paths without contaminating structured stdout.
6. Document the replacement and removal strategy.

## Phase 3 — MCP, REST, AI and agent contracts
1. Update server metadata and prompts to Vericore.
2. Introduce canonical `vericore_*` MCP names.
3. Retain old MCP names only when compatibility evidence warrants it; aliases delegate to canonical implementations.
4. Preserve REST functional endpoints and schemas; update identity metadata only.
5. Update agent prompts/tool descriptions/templates to Vericore.
6. Ensure structured protocols never receive human deprecation warnings on stdout.

## Phase 4 — Tooling, packaging and infrastructure
1. Update scripts, Gradle tasks, installDist paths, Docker/container labels, CI workflows, audit workflows, release scripts, examples, and packaging.
2. Update GitHub links and badges after confirming repository rename state.
3. Update website references only in this repository; treat the separate website repository as external unless separately authorized.

## Phase 5 — Documentation and migration guide
1. Update README and all current docs to Vericore.
2. Add a migration section documenting old-to-new identifiers.
3. Preserve factual historical CodeContext references.
4. Document deprecation and removal strategy for every retained legacy path.

## Phase 6 — Validation
1. Repository-wide search for every old-name variant.
2. Full build.
3. All unit/integration tests.
4. `installDist` and installed CLI verification.
5. Canonical Vericore CLI smoke suite.
6. Legacy compatibility smoke suite where retained.
7. REST startup and endpoint tests.
8. MCP initialize/list/call tests.
9. Release/package validation.
10. Static/lint/CodeQL/CI validation where configured.
11. Actual command-output audit for every command.
12. Repository-integrity and architecture gate.
13. Re-run old-reference search and classify every remaining hit.

## Phase 7 — GitHub repository rename
1. Verify code migration and checks first.
2. Rename repository to `sonii-shivansh/Vericore` only through GitHub repository settings if tooling exposes that operation.
3. Verify repository metadata, clone URLs, workflows, badges, and documentation after rename.
4. Update local/CI remotes only after verifying the new canonical URL.
5. Do not claim completion if the rename must be performed manually.

## Phase 8 — Final release gate
The migration is complete only when:
- canonical identity is Vericore;
- required compatibility is tested and explicitly deprecated;
- all builds/tests pass;
- installed CLI and package artifacts work;
- REST/MCP work;
- actual command outputs are correct;
- no unintended architecture/behavior changes are detected;
- all remaining CodeContext references are classified;
- GitHub repository state is verified;
- final release audit passes.

## Rollback
If migration validation fails, do not partially merge. Revert the migration branch or correct the affected compatibility/identity change before merge. Functional behavior and architecture must remain equivalent except for intentional naming/identity changes.
