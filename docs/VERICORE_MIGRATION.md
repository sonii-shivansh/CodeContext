# CodeContext → Vericore Migration

Vericore is the canonical product identity:

> **Understand. Change. Verify.**
>
> Evidence-grounded engineering intelligence for your codebase.

## Current identity

Use `vericore` for new scripts, integrations, documentation, CI, and AI-agent configuration.

## Temporary compatibility

The following CodeContext identifiers are retained only to avoid unnecessary breakage:

- legacy `codecontext` CLI launcher;
- `.codecontext.json` repository configuration;
- `CODECONTEXT_*` environment variables;
- `codecontext.config.home` system property and legacy user configuration location;
- `.codecontext/` and `.codecontext-architecture-contract.json` generated-state recognition;
- legacy `codecontext_*` MCP tool names.

Each compatibility path is deprecated, emits a non-fatal migration warning when selected, and points to the Vericore replacement.

### Removal policy

Compatibility is transitional. A future major release should remove these aliases after the deprecation window and migration documentation have been published. New integrations must use the Vericore identifiers immediately.

## MCP

New integrations must use `vericore_*` tool names. Legacy `codecontext_*` names are aliases only; they are not separate implementations and are not intended to become permanent APIs.

## Historical references

The changelog preserves CodeContext where it is part of release history. The separate `CodeContext-Website` identifier remains untouched because it belongs to a separate external website repository and is not part of this repository's identity migration.

## GitHub repository rename

The source migration targets `sonii-shivansh/Vericore`. The GitHub repository name itself must be changed only after the migration branch passes its complete validation. If repository-administration access is unavailable to the automation performing this migration, rename it manually in GitHub repository Settings → General → Repository name.
