# Data & Privacy

CodeContext is primarily a local analysis tool. This document describes what data stays on the machine, what can leave the machine, and which behavior requires explicit configuration.

## Default behavior

With the default configuration (`ai.enabled = false`), CodeContext performs repository scanning, parsing, dependency analysis, Git analysis, deterministic intelligence, report generation, and local API operations without sending repository content to an AI provider.

CodeContext does not include product telemetry or an analytics service in the application pipeline.

## What can leave the machine

There are two distinct outbound-data paths to be aware of.

### 1. Generated HTML report visualization

The generated HTML report currently references the `force-graph` JavaScript library from:

`https://unpkg.com/force-graph`

Opening the report therefore causes the browser to request that JavaScript asset from unpkg. The request is for the library itself; CodeContext does not upload the generated graph JSON or source files to unpkg as part of that script request.

This still means that a browser/network request is made to an external service. Users operating in air-gapped or strict no-network environments should not assume the generated report is completely self-contained.

### 2. Optional AI providers

AI analysis is disabled by default. When AI is explicitly configured and invoked, repository-derived context is sent to the configured provider over HTTPS using the provider API key supplied by the user.

The current implementation supports Gemini and Anthropic/Claude provider paths.

The approximate payload boundaries in the current implementation are:

| Operation | Repository-derived content sent |
| --- | --- |
| Single-file AI analysis | Up to 3,000 characters of the selected file, plus file metadata and deterministic context |
| Batch AI analysis | Up to 50 prioritized files by default; each file analysis uses the single-file boundary above |
| AI PR review | Changed-file paths, affected hotspot names, and up to 5,000 characters of the diff |
| Grounded repository Q&A | The user's question plus bounded deterministic evidence; the default evidence builder exposes at most 24 citations, with paths and aggregate metrics rather than raw file contents |

The exact provider request also contains the prompt instructions and structured metadata required for the selected operation.

### API keys

Provider API keys are configured locally. CodeContext does not send keys to any CodeContext-owned telemetry service. Provider requests authenticate directly with the selected external AI provider.

Never commit `.codecontext.json` containing a real API key.

## What stays local by default

The following operations are local unless an explicitly invoked feature requires an external service:

- source-file scanning and parsing;
- dependency graph construction;
- PageRank and hotspot analysis;
- Git history analysis;
- change-impact analysis;
- PR Intelligence and Architecture Intelligence deterministic analysis;
- grounded evidence construction;
- engineering-plan generation;
- local HTML report generation;
- local REST API execution.

## What CodeContext does not currently provide

CodeContext does not currently provide a CodeContext-hosted telemetry backend, centralized source-code storage, or a mandatory cloud account.

The local REST server also does not provide authentication or tenant isolation. Do not expose it to an untrusted network without adding an appropriate deployment boundary.

## Privacy considerations for generated reports

Generated reports can contain repository-derived information such as file names, relative paths, Git author names, commit-derived metadata, dependency relationships, hotspots, and generated descriptions. Treat reports as potentially sensitive artifacts before sharing them.

## Verifying the behavior

The authoritative implementation is the source code and configuration in this repository. In particular:

- `ReportGenerator.kt` defines the external visualization asset used by generated reports.
- `AICodeAnalyzer.kt` defines the AI provider requests and content-size boundaries.
- `GroundedAIService.kt` and `GroundedEvidence.kt` define the bounded evidence sent for grounded Q&A.
- `.codecontext.json.template` shows the default AI configuration.

If these implementation details change, this document must be updated in the same change.
