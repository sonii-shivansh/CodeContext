# Contributing to Vericore

Thank you for contributing to Vericore. Contributions should improve analysis accuracy, developer experience, reliability, or security without weakening the project's local-first safety model.

Vericore is intentionally open to community contributions. The source is released under the MIT License, and community contributors retain ownership of their contributions while licensing them under the project's terms. See [DCO.md](DCO.md) for the contribution provenance policy.

## Before you start

1. Search existing issues and pull requests.
2. For significant behavior or API changes, open an issue or discussion first.
3. Never include credentials, private source code, generated reports, `.vericore/`, or build output in a contribution.
4. For security vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.

## Development setup

Requirements:

- JDK 21 or newer
- Git
- Kotlin-capable editor

```bash
git clone https://github.com/sonii-shivansh/Vericore.git
cd Vericore
./gradlew --no-daemon clean test
```

## Workflow

1. Create a focused branch from `main`.
2. Implement the smallest coherent change.
3. Add or update tests, especially for public APIs and security boundaries.
4. Update documentation when behavior, configuration, or output changes.
5. Sign every contribution commit off with `git commit -s`.
6. Run the local validation commands.
7. Push the branch and open a pull request.

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon build installDist
```

The Verification and DCO workflows must pass before merging.

## Developer Certificate of Origin

Every contribution commit must contain a `Signed-off-by:` trailer confirming that the contributor has the right to submit the work under the project's licensing terms.

```bash
git commit -s -m "Describe the change"
```

If a commit was created without the sign-off, amend it before opening or updating the pull request:

```bash
git commit --amend -s
```

See [DCO.md](DCO.md) for details.

## Coding expectations

- Follow Kotlin coding conventions and existing formatting.
- Prefer structured concurrency; do not introduce `runBlocking` inside suspend code paths.
- Validate input at system boundaries.
- Keep errors stable and sanitized for API consumers.
- Do not expose absolute server filesystem paths, stack traces, provider response bodies, or credentials.
- Use configuration rather than duplicating operational limits.
- Preserve cancellation, bounded concurrency, and deterministic tests.
- Add KDoc for public APIs where useful.

## Tests

Tests are grouped under `src/test/kotlin/com/vericore`:

- `core/`: parser, graph, cache, property, edge-case, and stress tests;
- `server/`: path-security and rate-limit tests;
- `verification/`: backend verification coverage;
- `E2ETest.kt`: end-to-end behavior.

Security-sensitive changes should include tests for traversal, sibling-prefix paths, symlinks where applicable, malformed input, and error sanitization.

## Pull request checklist

- [ ] The change is focused and documented.
- [ ] Tests cover the changed behavior.
- [ ] Every contribution commit has a `Signed-off-by:` trailer.
- [ ] `./gradlew --no-daemon clean test` passes.
- [ ] `./gradlew --no-daemon build installDist` passes.
- [ ] The Verification workflow passes.
- [ ] The DCO workflow passes.
- [ ] No secrets or generated files are included.
- [ ] API, architecture, changelog, and security documentation are updated when applicable.
- [ ] The complete diff against `main` has been reviewed.

## Reporting bugs and requesting features

Use the GitHub issue templates where available. Include the Vericore version, JDK version, operating system, command, sanitized logs, and a minimal reproduction. Do not publish sensitive source code or security vulnerabilities in a public issue.
