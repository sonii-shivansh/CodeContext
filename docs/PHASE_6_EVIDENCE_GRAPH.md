# Phase 6 — Semantic Evidence Graph

Phase 6 establishes a bounded semantic evidence layer over deterministic repository analysis.

## Guarantees

- Evidence nodes are bound to one repository identity and one observed commit.
- Evidence relationships are explicit; the graph never invents repository facts.
- Node and edge ordering is deterministic.
- Duplicate nodes, duplicate edges, dangling edges, self relationships, cross-repository evidence, and stale evidence are rejected.
- Cross-feature evidence can be resolved by stable source reference.
- The graph has a deterministic SHA-256 identity (`digest()`).
- Grounded AI receives the graph identity and explicit evidence relationships alongside deterministic citations.
- Engineering preparation persists graph identity and graph cardinality in its serialized result.
- Engineering Reality binds the graph digest into the overall reality identity when commit provenance exists.

## Evidence flow

```text
AnalysisSnapshot
      |
      +--> GroundedEvidence
      |       |
      |       +--> evidence nodes
      |       +--> source relationships
      |
      +--> Architecture findings
      +--> Temporal snapshots
      |
      v
SemanticEvidenceGraph
      |
      +--> deterministic digest
      +--> Repo-QA / AI grounding context
      +--> Engineering Preparation provenance
      +--> Engineering Reality identity
```

## Design boundary

The semantic graph is intentionally not a replacement for the dependency graph. The dependency graph models code relationships. The semantic evidence graph models provenance and relationships among already-produced facts from analysis, architecture, temporal analysis, Repo-QA, and grounding.

This keeps deterministic repository analysis authoritative while allowing downstream consumers to resolve related evidence without reconstructing or guessing provenance.
