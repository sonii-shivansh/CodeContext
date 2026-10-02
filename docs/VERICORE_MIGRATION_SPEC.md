# Vericore Identity Migration Specification

Status: Implementation-approved

## 1. Purpose

Rename the product from **CodeContext** to **Vericore** without changing application behavior or unrelated architectural contracts.

Canonical identity:

- Product: **Vericore**
- Tagline: **Understand. Change. Verify.**
- Description: **Evidence-grounded engineering intelligence for your codebase.**

Repository target: `sonii-shivansh/Vericore`.

## 2. Non-goals

- No feature rewrite.
- No redesign of analysis, planning, verification, repository-safety, REST, or MCP semantics.
- No blind global replacement.
- No modification of third-party identifiers.
- No rewriting of genuine historical references.

## 3. Identity rules

Vericore is the only canonical current identity. New documentation, code identifiers, CLI output, API metadata, MCP metadata, reports, CI, packaging, and examples use Vericore.

The namespace becomes `com.vericore.*` because the existing namespace is the simple product namespace `com.codecontext.*`; no organization/domain namespace is currently established that would justify a deeper package.

## 4. Compatibility policy

Legacy CodeContext identifiers are retained only where removing them would unnecessarily break an existing user/script contract.

Every retained compatibility path must:

1. be explicitly marked deprecated in code/docs;
2. emit a non-fatal migration warning where the interface is user-invoked and warning output is safe;
3. document the Vericore replacement;
4. have a stated removal/deprecation path.

Compatibility is transitional, not permanent.

## 5. Identifier impact matrix

| Area | CodeContext identifier/pattern | Classification | Vericore action |
|---|---|---|---|
| Kotlin packages | `com.codecontext.*` | CANONICAL | Rename to `com.vericore.*` |
| Main class | `com.codecontext.MainKt` | CANONICAL | Rename to `com.vericore.MainKt` |
| CLI executable | `codecontext` | CANONICAL | Canonical executable becomes `vericore` |
| CLI legacy executable | `codecontext` | COMPATIBILITY | Temporary deprecated launcher/alias where distribution supports it |
| Gradle group | `com.codecontext` | CANONICAL | `com.vericore` |
| Gradle project | `codecontext` | CANONICAL | `vericore` |
| Repository-local config | `.codecontext.json` | COMPATIBILITY | `.vericore.json` canonical; legacy read with warning |
| Config directory | `.codecontext/` | COMPATIBILITY | `.vericore/` canonical; legacy read/recognize during transition |
| Architecture contract | `.codecontext-architecture-contract.json` | COMPATIBILITY | `.vericore-architecture-contract.json` canonical; legacy recognition where required |
| Environment variables | `CODECONTEXT_*` | COMPATIBILITY | `VERICORE_*` canonical; legacy fallback + warning |
| System property | `codecontext.config.home` | COMPATIBILITY | `vericore.config.home` canonical; legacy fallback + warning |
| REST branding | CodeContext server name/messages | CANONICAL | Vericore branding; endpoint behavior unchanged |
| REST paths | existing `/analyze`, `/impact`, etc. | EXTERNAL/CONTRACT | Preserve; do not rename functional API paths |
| MCP server metadata | `CodeContext` | CANONICAL | `Vericore` |
| MCP tool names | `codecontext_*` | COMPATIBILITY | New integrations use `vericore_*`; retain legacy aliases only if existing clients would break; mark deprecated |
| AI/agent instructions | CodeContext branding | CANONICAL | Vericore branding |
| Kotlin type names | `CodeContextConfig`, `CodeContextException`, `CodeContextServer` | CANONICAL | Rename to Vericore equivalents where names are product identity |
| Generated reports | CodeContext titles | CANONICAL | Vericore titles |
| Scripts | hard-coded `build/install/codecontext/bin/codecontext` | CANONICAL | Vericore distribution path |
| CI/CD | CodeContext distribution references | CANONICAL | Vericore paths/metadata |
| Release metadata | CodeContext artifact identity | CANONICAL | Vericore coordinates and labels |
| Tests/fixtures | product-specific CodeContext identifiers | CANONICAL | Vericore; add explicit compatibility tests where needed |
| Docs | current product references | CANONICAL | Vericore |
| Changelog history | prior release identity | HISTORICAL | Preserve genuine historical CodeContext references |
| Migration specification/history | `Formerly CodeContext` | HISTORICAL | Preserve as explicit migration history |
| GitHub repository URL | `sonii-shivansh/CodeContext` | CANONICAL | Target `sonii-shivansh/Vericore` after repository rename |
| External website repository | `CodeContext-Website` | EXTERNAL | Do not silently rename or modify; handle separately |
| Third-party dependencies | unrelated occurrences of CodeContext/codecontext | EXTERNAL | Do not modify |
| Git metadata | commit history, authors, old branch names | HISTORICAL/EXTERNAL | Do not rewrite history |
| Docker/container metadata | product image/name references | CANONICAL | Rename product identity where present; preserve base images and external names |
| Examples | current usage examples | CANONICAL | Vericore; legacy examples only when explicitly testing compatibility |
| Security/config docs | current configuration names | CANONICAL | Vericore canonical names; document deprecated legacy aliases |
| Unknown occurrences | ambiguous matches | UNKNOWN | Investigate individually before modification |

## 6. Normal user-facing output

Normal output must not display CodeContext branding. Help, startup messages, report headers, REST metadata, MCP metadata, errors, and generated documentation use Vericore.

Legacy warnings may name CodeContext because they are migration notices; they must identify the Vericore replacement and must not present CodeContext as the current product identity.

## 7. MCP policy

`vericore_*` is canonical for new integrations. Existing `codecontext_*` names are not duplicated as permanent APIs. If compatibility is required, legacy names are thin deprecated aliases to the canonical implementation and are covered by explicit tests and removal documentation.

## 8. Historical policy

The changelog and other records retain CodeContext when it is historically true. Current release instructions and current documentation use Vericore. A concise migration note may state that Vericore was formerly CodeContext.

## 9. Repository rename

The code migration is performed on an isolated branch first. The GitHub repository is renamed only after validation proves the migration healthy. If repository-administration tooling cannot perform the rename, the remaining manual action is explicitly reported rather than assumed successful.

## 10. Completion criteria

The migration is complete only when:

- the Vericore identity is consistent;
- behavior and unrelated architectural contracts are unchanged;
- compatibility paths are explicit and temporary;
- build and all available tests pass;
- distribution packaging passes;
- CLI, REST, and MCP smoke tests pass;
- repository-wide old-name audit is reviewed;
- remaining CodeContext references are classified and intentional;
- release/architecture/repository gates pass;
- repository rename status is verified, not assumed.
