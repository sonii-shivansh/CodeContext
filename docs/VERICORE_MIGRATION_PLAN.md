# Vericore Migration Implementation Plan

1. Inventory/classify all old identifiers before edits.
2. Rename Kotlin packages/classes and Gradle coordinates.
3. Make `vericore` canonical for CLI, normal output and metadata.
4. Introduce canonical Vericore config/env/generated-state names; retain only required legacy bridges with deprecation warnings and removal strategy.
5. Make `vericore_*` MCP names canonical; preserve only necessary aliases.
6. Update REST identity metadata without changing routes.
7. Update scripts, CI/CD, release packaging, Docker, examples and current docs; preserve history/external identifiers.
8. Validate full build/tests/installDist, CLI, REST, MCP, release/package, static analysis, actual command-output, and architecture/repository gates.
9. Classify every remaining old-name reference.
10. Verify `sonii-shivansh/Vericore` repository rename or report the manual GitHub settings step.
