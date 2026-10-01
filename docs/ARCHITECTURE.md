# Architecture

## Purpose

CodeContext is a local-first engineering-intelligence pipeline. It transforms Java and Kotlin source, Git history, dependency structure, and engineering signals into deterministic evidence, reports, repository Q&A, and evidence-backed engineering plans.

The long-term architecture is designed for an AI-heavy development world: deterministic analysis remains the source of truth, while AI is an optional reasoning layer over validated evidence.

## Current system shape

```text
CLI / REST / CI
      ↓
Configuration + path validation
      ↓
Repository Scanner
      ↓
Language Parsers + Git Intelligence
      ↓
Unified dependency graph
      ↓
Analysis Snapshot
      ↓
┌─────────────────────────────────────────────┐
│ Deterministic Intelligence                  │
│                                             │
│ Risk · Impact · PR · Architecture · Tests   │
│ Evolution · Hotspots · Learning Paths       │
└──────────────────────┬──────────────────────┘
                       ↓
              Grounded Evidence
                       ↓
        ┌──────────────┴──────────────┐
        ↓                             ↓
 Repository Q&A              Engineering Planner
        ↓                             ↓
        └──────────────┬──────────────┘
                       ↓
                 Optional AI
                       ↓
              Developer / CI / PR
```

## Current command surface

`Main.kt` registers analysis, impact, architecture, PR Intelligence, repository Q&A, engineering planning, AI assistant, evolution, and server commands.

### Repository Q&A

`repo-qa` builds a deterministic analysis snapshot, converts it to bounded grounded evidence, classifies the question, and retrieves ranked evidence. The first Q&A implementation does not require an external model.

### Engineering planning

`plan` consumes a versioned grounded-evidence artifact and produces a deterministic engineering plan with risk, implementation steps, evidence IDs, verification criteria, and uncertainty. It is read-only and provider-independent.

## Core pipeline

### Scanning

`RepositoryScanner` discovers supported Java and Kotlin source files while enforcing configured exclusions and file limits. Source is parsed, not executed.

### Parsing

`ParserFactory` selects a parser by extension. Java uses JavaParser-based analysis. Kotlin currently uses a lightweight parser with known complex-syntax limitations. `CodeParallelParser` coordinates parsing and cache access.

### Git intelligence

`OptimizedGitAnalyzer` adds change frequency, modification time, authorship, and recent-change context. Git operations are read-only.

### Dependency graph

`RobustDependencyGraph` resolves local dependency relationships, wildcard imports, cycles, and PageRank. The graph is rebuilt for each analysis to avoid stale state.

### Analysis snapshot

`AnalysisSnapshot` is the boundary between raw analysis and higher-level intelligence. It gives downstream features a stable, structured representation of repository metrics, files, hotspots, and architecture information.

### Evidence

`GroundedEvidenceBuilder` converts deterministic analysis into bounded `EvidenceCitation` records. Citation IDs are unique and repository paths are normalized to repository-relative paths. Evidence is the authoritative substrate for AI reasoning.

### Repository Q&A

`RepositoryQuestionClassifier` identifies an initial intent and `RepositoryEvidenceRetriever` ranks relevant evidence. Retrieval is bounded and explicit about insufficient evidence.

### Engineering planner

`EngineeringPlanner` converts grounded evidence into a structured plan. It does not modify source code, commit changes, or make autonomous decisions.

### AI layer

`GroundedAIService` can combine deterministic evidence with the existing provider abstraction. AI is optional and disabled by default. Provider output must be treated as reasoning over evidence, not as an authoritative repository fact source.

## Future architecture

The target 2027–2028 architecture adds a temporal evidence graph and verification/governance layers:

```text
Source + Git + CI + optional runtime signals
                     ↓
            Unified Engineering Model
                     ↓
       Versioned Evidence + Snapshots
                     ↓
      ┌──────────────┼─────────────────┐
      ↓              ↓                 ↓
 Governance      Simulation       Change Proof
      ↓              ↓                 ↓
      └──────────────┼─────────────────┘
                     ↓
             AI Reasoning Layer
                     ↓
        Controlled Agent Execution
                     ↓
          Isolated Workspace + CI
                     ↓
             Verified Change
```

## Security boundaries

- Source code is parsed, not executed.
- Local paths are canonicalized and constrained to configured roots where server APIs accept paths.
- File-count and rate limits reduce resource abuse.
- Evidence uses repository-relative paths.
- AI credentials remain provider configuration and must not enter evidence artifacts.
- External AI calls are opt-in.
- The current server does not provide authentication, tenant isolation, or report authorization by itself.
- Future agentic execution must use an isolated workspace, explicit policy checks, bounded permissions, and CI verification before changes can be proposed for merge.

## Determinism and reproducibility

For the same repository snapshot and relevant configuration, deterministic analysis should produce equivalent structured evidence. AI enrichment may vary and therefore must never overwrite deterministic facts.

## Extension points

- add a parser through `LanguageParser` and `ParserFactory`;
- add deterministic intelligence under `core/`;
- add evidence types without changing existing evidence IDs;
- add planner/retrieval rules with deterministic ordering;
- add CI/REST adapters around application services;
- add future policy, simulation, and provenance layers without coupling them to a model provider.
