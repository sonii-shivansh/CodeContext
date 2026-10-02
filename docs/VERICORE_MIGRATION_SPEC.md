# Vericore Migration Specification

**Vericore — Understand. Change. Verify.**  
**Evidence-grounded engineering intelligence for your codebase.**

## Rules
- Vericore is the sole canonical identity.
- No blind global replacement.
- CodeContext compatibility is temporary, explicit, deprecated, documented, warned where appropriate, and removable.
- Normal user-facing output uses Vericore only.
- New MCP integrations use `vericore_*`; old names are aliases only when required.
- Historical facts and external identifiers are preserved.
- Functional behavior, REST routes, evidence semantics, and architecture remain unchanged except for identity.

## Impact matrix
| Area | Class | Action |
|---|---|---|
| `com.codecontext.*` | CANONICAL | Rename to `com.vericore.*` |
| Gradle | CANONICAL | `vericore` / `com.vericore` |
| CLI | CANONICAL + COMPATIBILITY | `vericore`; deprecated legacy bridge if required |
| config/env/generated state | COMPATIBILITY | Vericore canonical; legacy read aliases temporarily |
| REST | CANONICAL | Vericore metadata; routes unchanged |
| MCP | CANONICAL + COMPATIBILITY | Vericore names; only required aliases |
| AI/agents | CANONICAL | Vericore prompts/metadata |
| scripts/CI/release/Docker | CANONICAL | Update project identity and paths |
| tests | CANONICAL + COMPATIBILITY | Canonical tests plus explicit legacy tests |
| docs/examples/security | CANONICAL + COMPATIBILITY | Vericore docs plus migration guidance |
| website | EXTERNAL | Separate repository; do not silently modify |
| GitHub URLs | CANONICAL | Update after verified repository rename |

## Acceptance
Full build/tests/installDist, CLI smoke, REST, MCP initialize/list/call, release/package checks, repository-wide search/classification, actual command-output audit, and architecture/repository gates must pass.
