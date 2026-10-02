# Vericore Migration Specification

**Vericore — Understand. Change. Verify.**  
Evidence-grounded engineering intelligence for your codebase.

## Approved rules
- Vericore is the sole canonical identity.
- No blind global replacement.
- Retain CodeContext only where required for compatibility; every retained path is explicitly deprecated, warns where appropriate, documents the Vericore replacement, and has a removal strategy.
- Normal user-facing output uses Vericore only.
- New MCP integrations use `vericore_*`; legacy names are temporary aliases only when required.
- Historical facts and external identifiers are preserved.
- Functional REST routes and non-identity behavior remain unchanged.

## Impact matrix
| Area | Classification | Action |
|---|---|---|
| `com.codecontext.*` | CANONICAL | `com.vericore.*` |
| Gradle | CANONICAL | `vericore` / `com.vericore` |
| CLI | CANONICAL + COMPATIBILITY | `vericore`; deprecated legacy bridge if needed |
| config/env/generated state | COMPATIBILITY | Vericore canonical; temporary CodeContext read aliases |
| REST | CANONICAL | Vericore metadata; routes unchanged |
| MCP | CANONICAL + COMPATIBILITY | Vericore names; only necessary legacy aliases |
| AI/agents | CANONICAL | Vericore prompts/metadata |
| scripts/CI/release/Docker | CANONICAL | Update project identity and paths |
| tests | CANONICAL + COMPATIBILITY | Update canonical tests; explicit legacy tests |
| docs/examples/security | CANONICAL + COMPATIBILITY | Vericore current docs + migration guidance |
| website | EXTERNAL | Separate repository; do not silently modify |
| GitHub URLs | CANONICAL | Update after verified repository rename |

## Acceptance
Full build/tests/installDist, CLI smoke, REST, MCP initialize/list/call, release/package checks, repository-wide search/classification, actual command-output audit, and architecture/repository integrity gates must pass.
