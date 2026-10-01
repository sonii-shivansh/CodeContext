# Architecture

## Purpose

CodeContext is a local-first engineering-intelligence pipeline for Java and Kotlin repositories. It transforms source code, Git history, dependency structure, and engineering signals into deterministic analysis, grounded evidence, repository Q&A, engineering plans, reports, and optional AI-assisted reasoning.

## Current system

```text
CLI / REST / CI
      ↓
Configuration + path validation
      ↓
Repository Scanner
      ↓
Java / Kotlin Parsers + Git Intelligence
      ↓
Unified Dependency Graph
      ↓
Analysis Snapshot
      ↓
┌────────────────────────────────────────────┐
│ Deterministic Intelligence                 │
│ Risk · Impact · PR · Architecture          │
│ Evolution · Hotspots · Learning Paths      │
└──────────────────────┬─────────────────────┘
                       ↓
               Grounded Evidence
                 ↙           ↘
        Repository Q&A      Engineering Planner
                 ↘           ↙
                 Optional AI
                       ↓
                  CLI / REST / CI
```

## Command surface

`Main.kt` registers repository analysis, impact, architecture, PR Intelligence, repository Q&A, engineering planning, AI assistance, evolution, and server commands.

### Repository analysis

`RepositoryScanner` discovers supported Java and Kotlin source files while enforcing configured exclusions and file limits. Source is parsed rather than executed.

`ParserFactory` selects a parser by extension. Java uses JavaParser-based analysis. Kotlin uses the project's current lightweight Kotlin parsing support and therefore has known complex-syntax limitations.

`CodeParallelParser` coordinates parsing and cache access.

### Git intelligence

`OptimizedGitAnalyzer` supplies change frequency, modification time, authorship, and recent-change context. Git operations are read-only.

### Dependency graph

`RobustDependencyGraph` resolves local dependency relationships, wildcard imports, cycles, and PageRank. The graph is rebuilt for each analysis to avoid stale state.

### Analysis snapshot

`AnalysisSnapshot` is the boundary between low-level analysis and higher-level intelligence. It provides a structured representation of repository metrics, files, hotspots, and architecture information.

## Deterministic intelligence

The current system includes:

- engineering risk signals;
- dependency-aware change impact;
- PR Intelligence for Git diffs;
- Architecture Intelligence;
- Git evolution analysis;
- hotspots and learning paths.

These components produce machine-readable results and are designed to be reproducible for the same repository state and configuration.

## Grounded evidence

`GroundedEvidenceBuilder` converts deterministic analysis into bounded `EvidenceCitation` records. Citation IDs are stable within the generated evidence set, and paths exposed through evidence are repository-relative.

`GroundedAIService` can pass bounded evidence to the configured AI provider. AI is optional and does not replace deterministic repository facts.

## Repository Q&A

`RepositoryQuestionClassifier` classifies an initial set of repository-question intents. `RepositoryEvidenceRetriever` extracts and ranks relevant evidence with deterministic ordering and bounded result counts.

The `repo-qa` command can therefore provide useful grounded retrieval without requiring an external model.

## Engineering planner

`EngineeringPlanner` consumes grounded evidence and produces a structured, versioned engineering plan containing affected components, risk, implementation steps, evidence IDs, verification criteria, and uncertainty.

The current planner is read-only and provider-independent. It does not modify source code, commit changes, or autonomously execute repository actions.

## REST and security boundaries

The Ktor server exposes local analysis and intelligence flows. Repository paths are canonicalized and checked against configured allowed roots. Rate limiting is enabled by default.

The current server is intended for trusted local/internal deployment. Authentication, authorization, tenant isolation, TLS, and external deployment controls are not provided by the application itself.

AI credentials are configuration data and must not be embedded in evidence artifacts. External AI calls are opt-in.

## CI

GitHub Actions is the authoritative execution environment. The project verifies builds and tests across supported workflows, including Windows compatibility and end-to-end intelligence flows.

## Extension points

- add parsers through `LanguageParser` and `ParserFactory`;
- add deterministic intelligence under `core/`;
- add evidence types without changing existing evidence semantics;
- add retrieval/planning rules with deterministic ordering;
- add CLI/REST adapters around application services;
- extend AI providers behind the existing provider boundary.
