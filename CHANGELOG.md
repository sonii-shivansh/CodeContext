# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Health endpoint verification workflow for CI smoke testing
- Enhanced GitHub Actions validation for CLI startup and report generation
- Improved server startup validation for local smoke tests
- Better build and packaging validation in CI

### Changed
- Updated verification workflow to run on Ubuntu 24.04
- Switched Java setup to `actions/setup-java@v5`
- Refined verification to validate the CLI, self-analysis report, and local server root endpoint
- Simplified CI to focus on the key app-level smoke tests

### Fixed
- Verified the CLI app can build and run from `installDist`
- Verified the CLI generates an HTML report successfully
- Confirmed the root endpoint returns the expected API message
- Reduced workflow noise and improved verification clarity

---

## [0.2.0] - 2026-09-19

### Added
- Production-oriented CLI release packaging via GitHub Releases
- Automated verification workflow for build, test, packaging, report generation, and smoke testing
- Server startup smoke validation in CI
- Root endpoint validation for the local API
- Improved release readiness for GitHub distribution publishing

### Changed
- Improved server startup configuration with host binding support
- Improved parser concurrency safety for multi-file analysis
- Hardened rate limiter logic to rollback counters when limits are exceeded
- Refined server command configuration for local and CI execution
- Improved project release posture for public distribution

### Fixed
- Fixed rate limiter counter rollback logic during rejected requests
- Fixed thread-safety concern in parse progress tracking
- Improved CLI execution flow for local server startup and verification
- Corrected release validation path for local analysis and artifact generation

### Security
- Added explicit path validation patterns for remote/local repository analysis
- Reduced risk of unsafe repository-path handling in server endpoints
- Improved server-side validation for local analysis workflow

### Notes
- This release is intended as a public GitHub Release distribution for the CLI.
- The project remains focused on developer tooling and onboarding analysis.
- Maven Central publication is not yet part of this release scope.

---

## [0.1.0] - 2025-12-14

### Added
- Initial public release of CodeContext
- Interactive dependency graph visualization
- Knowledge hotspot detection using PageRank
- Personalized learning path generation
- HTML report generation
- Git history integration and file metadata analysis
- Team contribution mapping
- Parallel file parsing for improved speed
- Caching layer for faster re-analysis
- CLI-powered repository analysis
- Optional AI-based code insights
- Ktor-based REST API server mode

### Changed
- Improved project structure and modular separation between CLI and core logic
- Refined repository scanning and graph generation flow

### Fixed
- Initial bugfixes for graph generation and report generation
- Stabilized analysis pipeline for basic repository onboarding use cases

### Known Limitations
- Limited support for Java and Kotlin
- Regex-based Kotlin parsing may miss complex language constructs
- Performance on very large repositories may require tuning
- AI features require configuration and external API access
- Server security and distributed deployment hardening are still in progress

---

[Unreleased]: https://github.com/sonii-shivansh/CodeContext/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.2.0
[0.1.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.1.0
