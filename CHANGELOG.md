# Changelog

All notable changes to CodeContext are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Configuration-driven source exclusions through `excludePaths`.
- Configured file-count enforcement through `maxFilesAnalyze`.
- Bounded multi-repository analysis concurrency.
- Content-hash-based parse cache invalidation.
- Atomic cache replacement with a fallback for filesystems without atomic moves.
- Dependency graph rebuild safety and indexed wildcard-import resolution.

### Changed

- Local API reports now use random identifiers and public report URLs.
- Organization analysis uses structured coroutines instead of nested `runBlocking`.
- Documentation now reflects the local-only REST endpoint and current AI-provider behavior.

### Security

- Preserved strict canonical-path validation and sibling-path protection.
- Removed permissive CORS behavior from the default server.
- Prevented absolute server filesystem paths from being returned by the API.
- Kept provider errors and AI credentials out of public API responses and prompt content where recognizable.

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

- Kotlin parsing is regex-based and may miss complex constructs.
- Reports currently depend on a browser visualization asset loaded from a CDN.
- The server does not provide authentication, tenant isolation, or report retention.
- AI analysis requires external provider access and explicit configuration.

[Unreleased]: https://github.com/sonii-shivansh/CodeContext/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.2.0
[0.1.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.1.0
