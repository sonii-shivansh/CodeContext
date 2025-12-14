# CodeContext Demo

This directory contains demo assets for the CodeContext project.

## Demo Screenshot

![CodeContext Demo](codecontext_demo.png)

*Interactive dependency graph showing knowledge hotspots and file relationships*

## What the Demo Shows

1. **Interactive Force Graph** - D3.js visualization of codebase dependencies
2. **Knowledge Hotspots** - Files ranked by importance using PageRank algorithm
3. **Learning Path** - Recommended reading order for new developers
4. **Team Contribution Map** - Developer activity and ownership
5. **Smart Context** - Git metadata (authors, changes, last modified)

## Running the Demo Yourself

```bash
# Clone the repository
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext

# Build and run
./gradlew run --args="analyze ."

# Open the generated report
open output/index.html  # macOS
xdg-open output/index.html  # Linux
start output/index.html  # Windows
```

## Sample Output

When you run CodeContext on itself, you'll see:
- ~80 Kotlin files analyzed
- ~100 dependency edges
- Top hotspots: ParsedFile.kt, RobustDependencyGraph.kt, ReportGenerator.kt
- Learning path ordered from simple utilities to complex analyzers

## Features Demonstrated

- ✅ Multi-language parsing (Java, Kotlin)
- ✅ Dependency graph construction
- ✅ PageRank hotspot detection
- ✅ Topological learning path generation
- ✅ Git history integration
- ✅ Interactive HTML reports
