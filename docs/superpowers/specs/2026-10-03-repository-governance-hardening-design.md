# Vericore Repository Governance Hardening

## Goal

Bring the Vericore repository's open-source presentation, contribution guardrails, CI/release hygiene, and GitHub governance model into one consistent operating standard without changing Vericore core behavior or undoing intentional CodeContext compatibility paths.

## Scope

### In scope

- Preserve the canonical Vericore website icon as the repository brand asset.
- Keep the repository description aligned with the product positioning: `Evidence-grounded engineering intelligence for your codebase.`
- Harden contribution metadata and templates where a repository-side file can enforce or guide the desired behavior.
- Add repository ownership metadata where it is useful and supported by the current single-maintainer structure.
- Review and improve GitHub Actions/Dependabot/release hygiene without changing the published `0.7.0` release architecture.
- Document the exact GitHub Settings / Ruleset configuration that must be applied in the GitHub UI for controls unavailable through the connected GitHub integration.
- Verify the resulting repository configuration and all changed workflows/configuration through GitHub Actions.

### Explicitly out of scope

- Rewriting Vericore core architecture or intelligence logic.
- Renaming or removing intentional `CodeContext` compatibility paths.
- Republishing or rewriting the already published `v0.7.0` release artifacts.
- Rebranding the website with a different icon; the website's existing favicon SVG is canonical.
- Enabling authentication, tenant isolation, or public-internet deployment for the local REST server.

## Current baseline

- Default branch: `main`.
- Repository is public.
- Current stable release: `v0.7.0`.
- Gradle/Kotlin/JVM stack is already established and is not part of this hardening effort.
- Dependabot is already configured for Gradle and GitHub Actions.
- DCO and verification workflows already exist.
- Bug and feature issue templates already exist.
- The repository currently allows merge commits, squash merges, and rebase merges; auto-merge is disabled.
- The repository currently has no repository ruleset exposed through the connected GitHub administration surface.

## Design

### 1. Repository identity

The GitHub repository description is the product-level description, not a historical implementation label. It should remain:

`Evidence-grounded engineering intelligence for your codebase.`

The homepage remains the Vericore website.

The canonical brand icon is `docs/images/vericore-icon.svg`, copied from the website's existing `public/favicon.svg` without stylistic reinterpretation.

### 2. Contribution governance

The repository should make the expected contribution contract obvious:

- work from focused branches;
- preserve DCO sign-off;
- run tests/build validation;
- update documentation for contract changes;
- avoid credentials, private source, generated output, and local state;
- explicitly describe compatibility, security/data handling, and verification in pull requests.

The existing PR template already covers most of this and should be preserved unless a concrete gap is identified during implementation.

A `CODEOWNERS` file may be added under `.github/CODEOWNERS` only if its ownership entries are valid for the current repository owner and do not create an artificial multi-team process. For a single-maintainer repository, the owner may be used as the default owner.

### 3. Issue intake

The existing Markdown bug and feature templates should remain. They should be checked for current Vericore terminology and security/privacy guidance. Security vulnerabilities continue to use `SECURITY.md` rather than public issues.

### 4. Dependency and action hygiene

Keep Dependabot weekly checks. Avoid broad dependency-policy changes that could destabilize the released 0.7.x line. Any workflow action update must remain compatible with the current GitHub Actions workflow set and pass the normal verification gates.

### 5. Branch governance target

The desired `main` policy is:

- pull request required for changes to `main`;
- required status checks selected from actual PR-triggered verification workflows;
- branch must be up to date before merge;
- conversation resolution required;
- force pushes and branch deletion blocked;
- squash merge is the default/primary merge strategy;
- automatic deletion of merged head branches enabled;
- auto-merge may be enabled at repository level but still obey required checks and review requirements.

The connected GitHub integration does not expose an administration write operation for repository rulesets/branch protection, so these settings are a manual GitHub UI step rather than a fabricated API change.

### 6. Release governance

The existing `v0.7.0` release remains immutable in practice: no rewrite of tags or release artifacts. Future releases should continue to be produced from CI after the repository verification gates pass. Repository-side documentation may clarify the release contract, but release architecture is not redesigned here.

### 7. Verification

Success means:

- repository identity has no accidental CodeContext product branding outside intentional compatibility/migration references;
- canonical icon exists and is referenced consistently;
- contribution templates remain valid and Vericore-specific;
- Dependabot remains valid;
- CI workflows remain syntactically valid and pass;
- no existing release artifacts are changed;
- repository settings that cannot be changed through the connector are documented precisely for manual application;
- `main` remains green after the hardening PR is merged.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Required checks are misidentified | Select only checks that actually run on pull requests and are stable required gates. |
| Governance blocks emergency maintenance | Preserve administrator bypass for genuine emergency work while preventing ordinary direct pushes. |
| Legacy compatibility is accidentally removed | Treat `CodeContext` references in explicit migration/compatibility paths as intentional unless proven otherwise. |
| Release 0.7.0 is altered | Do not rewrite tags or release artifacts. |
| Branding drifts between website and repo | Keep one canonical SVG asset sourced from the website favicon. |

## Acceptance criteria

1. The repository description is the canonical Vericore product description.
2. The repository README/docs use the canonical Vericore icon asset.
3. Contribution and issue intake files contain only current Vericore product identity except intentional migration guidance.
4. CI and dependency automation remain enabled and valid.
5. A concrete GitHub Settings/Ruleset checklist exists for controls unavailable through the connected administration API.
6. All repository-side changes pass the project's existing CI/verification gates.
7. No core product behavior or intentional CodeContext compatibility behavior is changed.
