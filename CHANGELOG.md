# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Backend verification test suite
- Comprehensive testing coverage

### Changed
- Improved graph building logic
- Enhanced error handling

### Fixed
- Debug code cleanup

## [0.1.0] - 2025-12-14

### Added
- 🎯 Initial release of CodeContext
- 🗺️ Interactive dependency graph visualization
- 🔥 Knowledge hotspot detection using PageRank algorithm
- 🎓 Personalized learning path generation
- 📊 HTML report generation with D3.js force graph
- 🔍 Multi-language support (Java, Kotlin)
- 📜 Git history analysis for file metadata
- 👥 Team contribution mapping
- ⚡ Parallel file parsing for performance
- 💾 Caching system for faster re-analysis
- 🧪 Comprehensive test suite (19+ tests)
- 📚 CLI interface with multiple commands
- 🤖 AI-powered code insights (optional)
- 🌐 REST API server mode
- 📈 Codebase evolution tracking

### Core Components
- Repository scanner with gitignore support
- JavaParser for Java code analysis
- Regex-based Kotlin parser
- JGraphT for dependency graph management
- JGit for Git history analysis
- Ktor server for API mode
- Clikt for CLI framework

### Documentation
- README with quick start guide
- CONTRIBUTING guidelines
- CODE_OF_CONDUCT
- MIT License

## Release Notes

### v0.1.0 - "Foundation Release"

This is the first public release of CodeContext, providing essential codebase analysis features for developer onboarding.

**Highlights:**
- Analyze Java and Kotlin projects
- Generate interactive visual reports
- Identify critical files automatically
- Create personalized learning paths

**Known Limitations:**
- Limited to Java/Kotlin (more languages coming)
- Large repositories (>10k files) may be slow
- AI features require API key

**Next Steps:**
- Add support for TypeScript, Python, Go
- Improve performance for large codebases
- Add IDE plugins
- Package manager distribution

---

[Unreleased]: https://github.com/sonii-shivansh/CodeContext/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sonii-shivansh/CodeContext/releases/tag/v0.1.0
