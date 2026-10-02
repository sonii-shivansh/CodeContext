# Vericore Migration Specification

## Identity
**Vericore** — **Understand. Change. Verify.**

**Evidence-grounded engineering intelligence for your codebase.**

Canonical repository target: `sonii-shivansh/Vericore`  
Canonical CLI: `vericore`  
Canonical Kotlin namespace: `com.vericore.*`  
Canonical Gradle group/project: `com.vericore` / `vericore`

## Rules
1. Vericore is the only canonical identity.
2. No blind global replacement.
3. Retain CodeContext compatibility only where required to avoid breaking users/scripts; every retained path is deprecated, warns where appropriate, documents the Vericore replacement, and has a removal strategy.
4. Normal user-facing output must never use CodeContext branding.
5. New MCP integrations use `vericore_*`; old names are temporary aliases only where required.
6. Preserve factual historical references and external identifiers.
7. Functional behavior, API routes, evidence semantics, and architecture remain unchanged except for identity migration.

## Impact matrix
| Area | Classification | Action |
|---|---|---|
| `com.codecontext.*` | CANONICAL | Rename to `com.vericore.*` |
| Gradle/project/group | CANONICAL | Rename to Vericore coordinates |
| CLI | CANONICAL + COMPATIBILITY | `vericore` canonical; deprecated legacy alias only if needed |
| config files | COMPATIBILITY | `.vericore*` canonical; legacy `.codecontext*` migration path |
| environment variables | COMPATIBILITY | `VERICORE_*` canonical; `CODECONTEXT_*` temporary aliases |
| generated state | COMPATIBILITY | `.vericore*` canonical; recognize legacy state safely |
| REST | CANONICAL | Update identity metadata; preserve routes |
| MCP | CANONICAL + COMPATIBILITY | `vericore_*` canonical; old aliases only when necessary |
| AI/agents | CANONICAL | Update prompts/tool metadata; preserve required machine contracts |
| scripts/CI/CD | CANONICAL | Update install paths, commands and workflow metadata |
| release packaging | CANONICAL | Vericore executable/artifacts |
| tests | CANONICAL + COMPATIBILITY | Update canonical assertions and add legacy tests where required |
| documentation | CANONICAL + HISTORICAL | Current docs use Vericore; factual history remains |
| website | EXTERNAL | Do not silently modify separate website repository |
| GitHub URLs | CANONICAL | Update after repository rename is verified |
| Docker/container | CANONICAL | Update identity metadata only |
| examples/security docs | CANONICAL + COMPATIBILITY | Vericore docs plus explicit legacy migration guidance |

## Acceptance
Full build/tests/installDist, CLI/REST/MCP smoke tests, release/package validation, repository-wide old-reference classification, actual command-output audit, and architecture/repository integrity gate must pass before completion.
