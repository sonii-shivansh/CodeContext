# Vericore Repository Governance Hardening — Implementation Plan

**Spec:** `docs/superpowers/specs/2026-10-03-repository-governance-hardening-design.md`

## Task 1 — Repository governance documentation

- Add a concise, version-controlled GitHub governance checklist under `docs/`.
- Document the exact intended `main` ruleset, required checks, merge policy, and repository feature settings.
- Clearly distinguish settings that are already enforced by repository files from settings that must be applied in GitHub UI.

**Expected:** maintainers have one source of truth for repository administration without pretending unavailable API operations were applied.

## Task 2 — Ownership and contribution metadata

- Add `.github/CODEOWNERS` for the current single-maintainer repository owner.
- Verify the existing PR template, issue templates, DCO workflow, and security policy remain current and use Vericore terminology.
- Only modify existing contribution files if a concrete stale/inconsistent reference is found.

**Expected:** ownership and contribution expectations are explicit without creating artificial review teams.

## Task 3 — Branding consistency

- Preserve `docs/images/vericore-icon.svg` as the canonical website-derived icon.
- Search for accidental product-level `CodeContext` branding in documentation and metadata.
- Do not modify intentional migration/compatibility references.

**Expected:** public-facing repository identity consistently says Vericore while compatibility code remains intact.

## Task 4 — Automation hygiene

- Verify Dependabot remains configured for Gradle and GitHub Actions.
- Verify DCO and verification workflows are present.
- Avoid dependency upgrades or workflow redesign unless a concrete defect is found.

**Expected:** automation is preserved and no unnecessary CI churn is introduced.

## Task 5 — Verification

- Review the complete branch diff.
- Validate changed YAML/Markdown/CODEOWNERS syntax using repository-native checks where available.
- Open a PR into `main`.
- Inspect PR checks and changed-file diff.
- Fix any failures found.
- Do not merge until the PR is green; merging is a separate repository side effect and will be performed only after the user explicitly asks for merge.

**Expected:** a small, reviewable governance PR with green checks and no product behavior changes.
