# CodeContext — Product Strategy

## Positioning

CodeContext is an **engineering evidence and verification platform** for software built and changed with humans and AI agents.

It should answer questions that code-generation tools alone cannot reliably answer: impact, architecture, ownership, history, governance, migration safety, and proof of change correctness.

## What makes the product durable

### Evidence graph

A unified graph connects code structure, dependencies, changes, ownership, architecture, tests, policies, and eventually runtime signals.

### Temporal intelligence

The system stores versioned snapshots so teams can compare architecture and risk over time.

### Verification, not confidence

AI suggestions are useful only when bounded by deterministic evidence and CI/runtime verification.

### Governance for agents

As agents gain the ability to change repositories, teams need a policy layer that can mechanically constrain what agents may do.

## Problems worth solving

| Problem | CodeContext capability |
| --- | --- |
| AI-generated changes miss hidden dependencies | Change Proof + impact graph |
| Architecture silently drifts | Architecture Constitution + drift detection |
| Large migrations fail because consumers are missed | Migration Guardian |
| Nobody knows who understands a critical component | Knowledge Continuity Graph |
| AI agents act outside intended boundaries | Agent Firewall |
| Risk scores are hard to trust | Explainable Risk Budget |
| Teams cannot reconstruct why an AI change was accepted | AI Change Provenance |
| Incidents require manual code archaeology | Production-to-Code Feedback |
| AI plans are disconnected from repository reality | Grounded Engineering Planner |

## Product layers

```text
Layer 0 — Repository understanding
  scanners / parsers / graph / Git

Layer 1 — Deterministic intelligence
  risk / impact / architecture / PR / ownership

Layer 2 — Evidence
  versioned citations / snapshots / uncertainty

Layer 3 — Verification
  CI / tests / policies / simulations / proofs

Layer 4 — AI reasoning
  Q&A / planning / review / explanation

Layer 5 — Controlled agents
  bounded implementation / validation / PR creation

Layer 6 — Organization intelligence
  cross-repository architecture / governance / historical trends
```

## Prioritization framework

Prioritize features that satisfy at least three properties:

1. They become more valuable as AI generates more code.
2. They depend on repository-specific evidence that generic models do not possess.
3. They produce an objective artifact or decision that can be verified in CI.
4. They solve an expensive engineering failure mode.
5. They create a reusable data model for later capabilities.

## Recommended sequence

### Near term

- stabilize documentation and public contracts;
- complete planner and grounded AI APIs;
- SARIF and CI-native reporting;
- architecture-as-policy engine;
- ownership and knowledge concentration;
- incremental analysis and persistent snapshots.

### Medium term

- Change Proof;
- counterfactual impact simulation;
- migration guardian;
- GitHub PR checks/comments;
- historical architecture comparison;
- provenance ledger for AI-assisted changes.

### Long term

- agent firewall;
- controlled patch generation in an isolated workspace;
- CI-driven autonomous repair loops;
- runtime-to-code correlation;
- organization-wide engineering digital twin.

## What not to build

Avoid competing directly with:

- generic autocomplete;
- generic chat over source files;
- another vector-search-only code assistant;
- opaque AI review scores;
- autonomous agents without verification boundaries.

The moat should be the **structured evidence model + temporal graph + verification + governance**.
