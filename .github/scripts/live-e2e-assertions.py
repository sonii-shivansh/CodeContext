#!/usr/bin/env python3
"""Validate the complete live-repository evidence bundle."""
from __future__ import annotations

import json
import sys
from pathlib import Path


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: live-e2e-assertions.py <repo>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1]).resolve()
    out = root / "output"
    required = [
        "analysis-snapshot.json",
        "engineering-risks.json",
        "architecture.json",
        "architecture-drift.json",
        "architecture-contract.json",
        "engineering-context.json",
        "engineering-context-diff.json",
        "pr-intelligence.json",
        "grounded-evidence.json",
        "engineering-plan.json",
        "engineering-context-baseline.json",
        "engineering-context-mutated.json",
        "engineering-context-mutated-diff.json",
        "engineering-context-restored.json",
        "verify.json",
        "mcp-responses.json",
        "rest-analyze.json",
        "rest-architecture.json",
        "rest-pr-intelligence.json",
    ]
    for name in required:
        path = out / name
        if not path.is_file() or path.stat().st_size == 0:
            raise AssertionError(f"missing live E2E artifact: {path}")

    snapshot = load(out / "analysis-snapshot.json")
    assert snapshot["schemaVersion"] == "1.1"
    assert snapshot["metrics"]["totalFiles"] > 0
    assert snapshot["repository"]["repositoryCommit"]
    assert snapshot["repository"]["repositoryStateDigest"]

    baseline = load(out / "engineering-context-baseline.json")
    mutated = load(out / "engineering-context-mutated.json")
    diff = load(out / "engineering-context-mutated-diff.json")
    restored = load(out / "engineering-context-restored.json")
    assert baseline["schemaVersion"] == "1.0"
    assert baseline["snapshotDigest"]
    assert mutated["snapshotDigest"] != baseline["snapshotDigest"]
    assert diff["summary"]["added"] + diff["summary"]["modified"] > 0
    assert restored["snapshotDigest"] == baseline["snapshotDigest"]

    verification = load(out / "verify.json")
    assert verification["schemaVersion"] == "1.0"
    assert verification["status"] in {"PASS", "REVIEW_REQUIRED"}
    assert verification["provenance"]["operation"] == "verify"

    contract = load(out / "architecture-contract.json")
    assert contract["schemaVersion"] == "1.0"
    assert contract["passed"] is True

    print(json.dumps({
        "status": "passed",
        "repositoryCommit": snapshot["repository"]["repositoryCommit"],
        "files": snapshot["metrics"]["totalFiles"],
        "mutationChanges": diff["summary"],
        "verificationStatus": verification["status"],
        "restoredToBaseline": restored["snapshotDigest"] == baseline["snapshotDigest"],
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
