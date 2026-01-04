# CodeContext

**Intelligent codebase context analyzer for faster developer onboarding**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Build Status](https://github.com/sonii-shivansh/CodeContext/workflows/CI%2FCD/badge.svg)](https://github.com/sonii-shivansh/CodeContext/actions)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## Why CodeContext?

### The Problem

New developers often take **1–3 months** to become productive in a new codebase due to:

* Difficulty finding where to start
* Understanding file and module dependencies
* Outdated or missing documentation
* Repeatedly asking common questions

### The Solution

CodeContext analyzes your codebase in seconds and generates:

* **Interactive dependency maps** to visualize structure
* **Knowledge hotspots** to identify critical files using PageRank
* **Personalized learning paths** that suggest a logical reading order
* **Contextual insights** including Git history, authorship, and change frequency

**Result:** Reduce onboarding time from **3 months to approximately 3 weeks**.

---

## Features

| Feature                     | Description                                               |
| --------------------------- | --------------------------------------------------------- |
| Interactive Dependency Maps | Zoomable force-directed graphs showing file relationships |
| Knowledge Hotspots          | PageRank-based identification of critical files           |
| Learning Paths              | Topologically sorted reading order                        |
| Git Integration             | Authors, change frequency, and recent commit data         |
| Team Contribution Map       | Visibility into knowledge silos and bus-factor risks      |
| Multi-Language Support      | Java and Kotlin (additional languages planned)            |
| Performance                 | Parallel parsing with intelligent caching                 |
| Reporting                   | Clean HTML reports with D3.js visualizations              |

---

## Demo
![CodeContext Demo](docs/images/demo.png)

*Interactive dependency graph with knowledge hotspot highlighting and learning path generation*

**Live Example**

```bash
./gradlew run --args="analyze ."
```

---

## Quick Start

### Prerequisites

* JDK 21+
* Git (required for Git history analysis)

---

### Installation

#### Option 1: Build from Source

```bash
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext
./gradlew build
./gradlew installDist
```

#### Option 2: Download Release (Planned)

Pre-built binaries will be available on the GitHub Releases page.

---

## Usage

### Analyze a Project

```bash
./gradlew run --args="analyze /path/to/project"
./build/install/codecontext/bin/codecontext analyze /path/to/project
./build/install/codecontext/bin/codecontext analyze .
```

---

### View the Report

```bash
open output/index.html        # macOS
xdg-open output/index.html   # Linux
start output/index.html      # Windows
```

---

### Sample Output

```text
Starting CodeContext analysis for: /path/to/project
Scanning repository...
Found 247 files
Parsing code...
Parsed 247 files
Analyzing Git history...
Building dependency graph...
Hot Zones (Top 5):
- UserService.kt (0.0847)
- DatabaseConfig.kt (0.0623)
- AuthMiddleware.kt (0.0521)
- ApiController.kt (0.0498)
- DataRepository.kt (0.0445)
Generating report...
Report generated at: /path/to/project/output/index.html
Completed in 3421ms
```

---

## Documentation

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Development Guide](docs/DEVELOPMENT.md)
- [API Documentation](docs/API.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

---

## Project Structure

```text
codecontext/
├── src/main/kotlin/com/codecontext/
│   ├── cli/              # CLI commands
│   ├── core/             # Core analysis engine
│   │   ├── scanner/      # File scanning and filtering
│   │   ├── parser/       # Language parsers
│   │   ├── graph/        # Dependency graph and PageRank
│   │   ├── analyzer/     # Code analysis algorithms
│   │   └── generator/    # Learning path generation
│   ├── output/           # Report generation
│   └── server/           # Optional REST API
└── src/test/             # Test suite
```

---

## Tech Stack

- **Language:** Kotlin 2.1.0
- **Build:** Gradle 8.5+
- **CLI:** [Clikt](https://github.com/ajalt/clikt)
- **Parsing:** [JavaParser](https://javaparser.org/), Regex
- **Graphs:** [JGraphT](https://jgrapht.org/) (PageRank, Topological Sort)
- **Git:** [JGit](https://www.eclipse.org/jgit/)
- **Visualization:** [D3.js Force Graph](https://github.com/vasturiano/force-graph)
- **Server:** [Ktor](https://ktor.io/)


---

## Contributing

Contributions are welcome.

### Contributor Workflow

1. Fork the repository
2. Clone your fork
3. Create a feature branch
4. Implement changes and tests
5. Run the test suite
6. Commit changes
7. Push and open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## Roadmap

- [x] Java & Kotlin support
- [x] Interactive dependency graphs
- [x] PageRank hotspot detection
- [x] Learning path generation
- [x] Git history integration
- [ ] TypeScript/JavaScript support
- [ ] Python support
- [ ] Go support
- [ ] IntelliJ IDEA plugin
- [ ] VS Code extension
- [ ] Package manager distribution (Homebrew, Scoop)
- [ ] Docker image
- [ ] Cloud-hosted analysis service

---

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## Support

-  **Bug Reports:** [Open an issue](https://github.com/sonii-shivansh/CodeContext/issues/new?template=bug_report.md)
-  **Feature Requests:** [Request a feature](https://github.com/sonii-shivansh/CodeContext/issues/new?template=feature_request.md)
-  **Discussions:** [GitHub Discussions](https://github.com/sonii-shivansh/CodeContext/discussions)
-  **Email:** [shivanshsoni568@gmail.com](mailto:shivanshsoni568@gmail.com)

---

<div align="center">

[Website](https://sonii-shivansh.github.io/CodeContext/) • [Documentation](docs/) • [Contributing](CONTRIBUTING.md) • [Changelog](CHANGELOG.md)

</div>
