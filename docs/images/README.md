# CodeContext demo assets

This directory contains screenshots and other non-runtime assets used by the project documentation.

## Recreate the report

From the repository root:

```bash
./gradlew installDist
./build/install/codecontext/bin/codecontext analyze .
```

Open the generated report at `output/index.html`.

## Demonstrated capabilities

- Java and Kotlin source discovery
- Dependency graph construction
- PageRank hotspot ranking
- Learning-path generation
- Git metadata enrichment
- Interactive HTML visualization

The screenshot is illustrative. File counts, dependency counts, and hotspot ordering depend on the current repository state and configuration.
