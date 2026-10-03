# Vericore Repository Governance

This document is the version-controlled source of truth for the GitHub repository's intended governance. It deliberately separates repository-file controls from GitHub UI controls that are not represented in the source tree.

## Repository identity

- **Name:** `Vericore`
- **Description:** `Evidence-grounded engineering intelligence for your codebase.`
- **Default branch:** `main`
- **Visibility:** Public
- **Homepage:** `https://sonii-shivansh.github.io/Vericore-Website/`
- **Canonical icon:** `docs/images/vericore-icon.svg`
- **Stable release:** `v0.7.0`

The website favicon SVG is the canonical project icon. Do not create a second logo variant in the repository unless the website branding is intentionally changed first.

## Repository-file controls

These controls live in Git and should be reviewed like code:

- DCO verification runs on pull requests targeting `main`.
- CI/CD runs on pull requests targeting `main` and `develop`.
- Verification runs on pull requests targeting `main`.
- Dependabot configuration covers Gradle and GitHub Actions.
- Security vulnerability reporting remains governed by `SECURITY.md`; vulnerabilities should not be disclosed through public issues.
- Issue templates and the pull-request template are the contribution intake contract.
- Intentional `CodeContext` migration/compatibility references must not be removed merely to make a text search return zero results.

## GitHub Settings target

Apply these settings in **Repository → Settings**. Where a setting is already correct, leave it unchanged.

### Features

- Issues: **ON**
- Projects: **ON**
- Discussions: **ON**
- Wiki: **OFF** when repository documentation is maintained exclusively under `docs/`
- Preserve the existing public visibility and GitHub Pages website

### Pull requests

Preferred merge policy:

- Allow squash merging: **ON**
- Allow merge commits: **OFF**
- Allow rebase merging: **OFF**
- Automatically delete head branches: **ON**
- Auto-merge: **OFF unless deliberately adopted later**
- Require conversation resolution through the `main` ruleset

Squash merging keeps `main` history focused on reviewable changes while preserving the complete development history in pull requests.

### `main` ruleset

Create or update a ruleset for `main` with these requirements:

- Require a pull request before merging.
- Require at least **1 approving review** when an independent reviewer is available.
- Require conversation resolution.
- Require branches to be up to date before merging.
- Block force pushes.
- Block branch deletion.
- Allow administrators to bypass rules for emergency maintenance.

#### Required status checks

Only require checks that are stable and actually run on pull requests targeting `main`:

1. **CI/CD**
2. **Verification**
3. **DCO**

Do **not** require manual-only workflows such as `workflow_dispatch`-only integrity checks unless their trigger model is deliberately changed first. Do not require live or quota-sensitive checks merely because they exist; they should remain informative or be made deterministic before becoming merge-blocking.

### Releases

- Do not rewrite the published `v0.7.0` tag or its artifacts.
- Future releases should originate from the repository's release workflow after the normal verification gates pass.
- Release artifacts must remain reproducible and accompanied by checksums where the release workflow provides them.

## Security and quality

- Keep `SECURITY.md` as the private vulnerability-reporting path.
- Keep dependency automation enabled.
- Review GitHub Actions dependencies during routine maintenance.
- Never add credentials, private source material, or generated local state to the repository.

## Branding / migration rule

`CodeContext` is retained only where it represents intentional compatibility or migration behavior. Examples include legacy configuration/environment variables, legacy directory names, migration tooling, and backward-compatible aliases. Public product copy, repository identity, and current documentation should use **Vericore**.

## Change checklist

Before merging a repository-governance change:

- [ ] Product identity is Vericore.
- [ ] Canonical icon is unchanged unless branding was intentionally updated.
- [ ] No intentional compatibility path was removed.
- [ ] PR template and issue templates remain valid.
- [ ] DCO passes.
- [ ] CI/CD passes.
- [ ] Verification passes.
- [ ] Release tags/artifacts were not rewritten.
- [ ] The final GitHub Settings state matches this document.
