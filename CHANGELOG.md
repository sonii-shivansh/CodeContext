# Changelog

All notable changes to CodeContext are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Deterministic Change Impact Intelligence and PR Intelligence.
- Architecture Intelligence and architecture evidence.
- Versioned `AnalysisSnapshot` and grounded `EvidenceCitation` contracts.
- Grounded repository Q&A retrieval through the `repo-qa` CLI command.
- Evidence-backed engineering planning through the `plan` CLI command.
- Linux and Windows CI verification for the current intelligence flows.

### Changed

- Documentation now describes the implemented evidence-first architecture and current command/API surface.
- AI is documented as an optional reasoning layer rather than a repository source of truth.

### Security

- Grounded evidence exposes repository-relative paths instead of absolute filesystem paths.
- AI context remains bounded and opt-in.
- Server path validation and rate limiting remain enabled for the local API.

## [0.2.0] - 2026-09-19

### Added

- CLI release packaging and verification workflow.
- Build, test, packaging, report-generation, and server smoke validation.
- Ktor REST server health endpoints.
- Java and Kotlin repository analysis with dependency visualization.
- PageRank hotspot detection, learning paths, Git metadata, caching, and optional AI assistance.

### Changed

- Improved parser concurrency and rate-limit rollback behavior.
- Added server-side path validation and configurable resource limits.
- Improved local distribution and release validation.

## [0.1.0] - 2025-12-14

### Added

- Initial public release.
- Java and Kotlin parsing.
- Interactive dependency graph reports.
- PageRank-based hotspots.
- Learning-path generation.
- Git history integration.
- CLI and Ktor server modes.
- Optional AI code insights.

### Known limitations

- Kotlin parsing may miss complex constructs.
- Reports currently depend on a browser visualization asset loaded from a CDN.
- The server does not provide authentication, tenant isolation, or report retention.
- AI analysis requires external provider access and explicit configuration.

[Unreleased]: https://github.com/sonii-shivansh/CodeContext/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.2.0
[0.1.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.1.0
