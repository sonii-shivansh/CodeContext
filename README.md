# CodeContext 🎯

> Intelligent codebase context analyzer for faster developer onboarding

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Stable-brightgreen.svg)]()

## 📋 Overview

CodeContext is an open-source CLI tool that analyzes codebases to generate interactive context maps, identify knowledge hotspots, and create personalized onboarding paths for developers.

**Problem:** New developers take 1-3 months to become productive due to lack of codebase understanding.

**Solution:** Automated codebase analysis that makes understanding large projects 10x faster.

## ✨ Features

- 🗺️ **Interactive Dependency Maps** - Visualize your codebase structure with zoomable graphs.
- 🔥 **Knowledge Hotspots** - Automatically identify critical files using PageRank.
- 🎓 **Learning Paths** - "Start Here" recommended reading order generated via topological analysis.
- � **Smart Context** - See "Authors" and "Churn Rate" (Git history) directly on the graph nodes.
- 📊 **Comprehensive Reports** - Clean HTML reports with embedded data.
- 🚀 **Multi-Language Support** - Full support for Java and Kotlin.

## 🚀 Quick Start

### Prerequisites

- JDK 21 or higher
- Git

### Installation (Building from Source)

```bash
# Clone the repository
git clone https://github.com/yourusername/codecontext.git
cd codecontext

# Build the standalone functionality
./gradlew installDist
```

### Usage

You can run the tool using the generated launch script:

**Window:**
```cmd
build\install\codecontext\bin\codecontext.bat analyze .
```

**Linux/Mac:**
```bash
./build/install/codecontext/bin/codecontext analyze .
```

# View generated report
open output/index.html


## 🏗️ Project Structure

```
codecontext/
├── src/main/kotlin/com/codecontext/
│   ├── cli/              # CLI commands
│   ├── core/             # Core analysis logic
│   │   ├── scanner/      # File scanning
│   │   ├── parser/       # Code parsing
│   │   ├── graph/        # Dependency graphs
│   │   ├── analyzer/     # Analysis algorithms
│   │   └── generator/    # Context generation
│   └── output/           # Report generation
└── src/test/             # Tests
```

## 🛠️ Tech Stack

- **Language:** Kotlin 2.1.0
- **Build:** Gradle 8.5+
- **CLI:** Clikt
- **Parsing:** JavaParser, KotlinPoet
- **Graphs:** JGraphT
- **Git:** JGit

## 📊 Development Status

- [x] Project initialization
- [x] File scanner implementation
- [x] Java/Kotlin parser (JavaParser, Regex)
- [x] Dependency graph builder (JGraphT, PageRank)
- [x] HTML report generator (Interactive Force Graph)
- [x] CLI interface refinement
- [x] Smart Context (Git Integration)
- [x] Personalized Learning Paths (Reverse Topological Sort)

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built with ❤️ to solve real developer onboarding problems.
