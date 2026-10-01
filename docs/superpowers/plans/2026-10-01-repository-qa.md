# Repository Q&A Implementation Plan

1. Define Q&A domain contracts: question, intent, evidence candidate, retrieval result, answer, uncertainty.
2. Build deterministic intent classification and entity/path extraction without an LLM dependency.
3. Build evidence retrieval over existing analysis outputs with stable ranking and hard limits.
4. Add an application service that returns grounded structured results and delegates optional reasoning to a provider interface.
5. Add CLI and REST adapters sharing the same service.
6. Add security validation for paths, question size, provider boundaries, and context limits.
7. Add fixture-based unit/property tests for each intent and retrieval rule.
8. Add CLI/REST E2E tests and JSON contract validation.
9. Run full CI including Windows, lint, self-analysis, and existing intelligence E2E suites.
10. Fix failures from actual logs, rerun until green, then open a PR for review/merge.
