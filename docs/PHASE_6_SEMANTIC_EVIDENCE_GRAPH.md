# Phase 6 — Semantic Evidence Graph Foundation

## Goal

Make deterministic evidence identity, provenance, and relationships explicit without replacing existing analysis, Repo-QA, AI grounding, or Engineering Reality contracts.

## Scope

Phase 6A introduces a bounded, immutable semantic evidence graph over already-produced evidence. It must:

- preserve existing evidence IDs and contracts;
- bind nodes to repository identity and observed commit/state;
- carry producer and content digest metadata;
- model explicit `DERIVED_FROM` and `SUPPORTS` relationships;
- provide deterministic node/edge ordering;
- reject duplicate node IDs, dangling edges, cross-repository nodes, and invalid self-relationships;
- avoid inventing evidence that is not supplied by deterministic producers.

## Non-goals

This phase does not replace the dependency graph, add a generic graph database, change the public CLI schema, or make AI conclusions authoritative.

## Invariants

1. A graph has one repository identity.
2. Every node has a stable ID, evidence type, producer, repository identity, observed commit, and content digest.
3. Nodes are immutable after construction.
4. Edge endpoints must exist in the graph.
5. Cross-repository edges are invalid.
6. Node and edge serialization is deterministic.
7. Empty graphs are valid.
8. Existing evidence remains the source of truth; the graph is a structured index/relationship layer.

## Follow-up phases

- 6B: connect architecture, hotspot, temporal, and Repo-QA evidence producers.
- 6C: consume graph relationships in Repo-QA.
- 6D: expose graph-backed evidence to AI grounding.
- 6E: bind planner/prepare provenance to graph identity.
