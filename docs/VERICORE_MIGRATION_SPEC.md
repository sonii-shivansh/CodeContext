# Vericore Migration Specification

## Status
Approved migration design; implementation follows this specification.

## Identity
- Canonical product name: **Vericore**
- Tagline: **Understand. Change. Verify.**
- Positioning: **Evidence-grounded engineering intelligence for your codebase.**
- Target repository: `sonii-shivansh/Vericore`
- Canonical CLI: `vericore`
- Canonical Kotlin namespace: `com.vericore.*`
- Canonical Gradle project/group: `vericore` / `com.vericore`

## Migration principles
1. Vericore is the single canonical identity.
2. No blind global replacement.
3. Preserve compatibility only where required to avoid breaking existing users/scripts.
4. Every retained CodeContext compatibility path is explicitly deprecated, emits a non-fatal migration warning where appropriate, documents the Vericore replacement, and has a removal strategy.
5. Normal user-facing output must use Vericore only.
6. New MCP integrations use Vericore tool names; legacy names are compatibility aliases only where necessary.
7. Historical references remain factual and are not cosmetically rewritten.
8. External identifiers/dependencies are not renamed.
9. Functional API routes and unrelated behavior remain unchanged unless identity migration requires otherwise.
10. No release claim is made without build, test, packaging, live command-output, MCP, REST, and repository-integrity evidence.

## Impact classification
| Area | Old identifier(s) | Classification | Migration action |
|---|---|---|---|
| Package namespaces | `com.codecontext.*` | CANONICAL | Rename to `com.vericore.*` consistently |
| Gradle | `codecontext`, `com.codecontext` | CANONICAL | Rename project/group/application entry point |
| CLI | `codecontext` | CANONICAL + COMPATIBILITY | `vericore` canonical; retain deprecated launcher/alias only if required |
| Configuration | `.codecontext.json`, `codecontext.config.home` | COMPATIBILITY | Add Vericore canonical config; legacy read path deprecated with warning |
| Environment variables | `CODECONTEXT_*` | COMPATIBILITY | Add `VERICORE_*`; legacy aliases temporarily accepted with warning |
| Generated state | `.codecontext/`, `.codecontext-architecture-contract.json` | COMPATIBILITY | Canonical `.vericore*`; safely recognize/migrate legacy state |
| REST | server/product metadata | CANONICAL | Brand metadata becomes Vericore; functional routes stay stable |
| MCP | `CodeContext`, `codecontext_*` | CANONICAL + COMPATIBILITY | New Vericore names; old names only as temporary aliases if required |
| AI/agent integrations | prompts/tool metadata/provider-facing identity | CANONICAL | Vericore-only normal identity; preserve only machine compatibility that is necessary |
| Scripts | `codecontext` paths/commands | CANONICAL | Update scripts to `vericore`; compatibility only where scripts represent user-facing legacy entry points |
| CI/CD | install paths, workflow strings, release checks | CANONICAL | Update to Vericore |
| Release packaging | distribution name/bin/artifacts | CANONICAL | Rename to Vericore |
| Tests | packages, fixtures, expected output | CANONICAL + COMPATIBILITY | Update canonical assertions; add explicit legacy tests only for retained compatibility |
| Documentation | product identity, examples, links | CANONICAL | Vericore branding; preserve factual history |
| Website | CodeContext website/repository | EXTERNAL | Investigate and coordinate separately; do not silently mutate external repo |
| GitHub URLs | `sonii-shivansh/CodeContext` | CANONICAL | Update internal links after repository rename; verify redirects/location |
| Docker/container metadata | image names, labels, startup text | CANONICAL | Update project identity; do not alter unrelated base images |
| Examples | command/config/package examples | CANONICAL | Update to Vericore; preserve legacy examples only when explicitly documenting migration |
| Security/config docs | old config/env identifiers | COMPATIBILITY | Document canonical Vericore settings and deprecated legacy aliases |

## Compatibility policy
Legacy CodeContext identifiers are not a second product identity. They exist only as migration bridges. Each bridge must:
- be marked deprecated in code/docs;
- point users to the Vericore replacement;
- emit a non-fatal warning where doing so will not corrupt machine-readable output/protocols;
- have a documented removal target/version or explicit deprecation window.

Machine-readable protocols must not emit warnings into structured stdout/JSON-RPC streams. Warnings belong on stderr or protocol metadata where appropriate.

## MCP policy
- Canonical new tool names use `vericore_*`.
- Existing `codecontext_*` names are retained only if compatibility evidence shows existing consumers could break.
- No duplicate feature implementations: aliases delegate to the canonical Vericore implementation.
- Normal MCP server metadata identifies Vericore.

## Historical policy
Changelogs and migration documentation may state that Vericore was formerly CodeContext. Historical release names, commit references, and factual migration records remain intact.

## GitHub repository rename
Target: `sonii-shivansh/Vericore`. The migration must not claim repository rename completion unless the GitHub repository metadata confirms it. If repository-settings write access is unavailable, the remaining action is manual.

## Non-goals
- No feature rewrite.
- No architecture redesign unrelated to identity migration.
- No behavior changes to analysis, intelligence, safety, verification, REST routes, or evidence semantics.
- No renaming of external dependencies, third-party projects, usernames, or historical facts.

## Acceptance criteria
1. Canonical build and source identity is Vericore.
2. Normal CLI/help/API/MCP/report output contains Vericore, not CodeContext.
3. Required legacy bridges are explicitly deprecated and tested.
4. Full build/tests/installDist succeed.
5. CLI, REST, MCP, packaging, and release gates succeed.
6. Repository-wide old-name search has an intentional classification for every remaining reference.
7. Actual command-output audit passes.
8. Architecture/repository integrity remains unchanged except for intentional identity changes.
9. No release is declared complete without live verification.
