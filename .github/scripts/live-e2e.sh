#!/usr/bin/env bash
set -euo pipefail

CLI="${1:?CodeContext CLI path required}"
REPO="${2:?repository path required}"
ROOT="$(cd "$REPO" && pwd)"
OUT="$ROOT/output"
mkdir -p "$OUT"

run_cc() {
  echo "+ codecontext $*"
  timeout --signal=TERM --kill-after=30s 8m "$CLI" "$@"
}

first_source() {
  find "$ROOT/src" -type f \( -name '*.java' -o -name '*.kt' \) | sort | head -1
}

assert_file() {
  test -s "$1" || { echo "Missing required artifact: $1" >&2; exit 1; }
}

SOURCE_FILE="$(first_source)"
test -n "$SOURCE_FILE"

run_cc doctor | tee "$OUT/doctor.txt"
run_cc analyze .
assert_file "$OUT/index.html"
assert_file "$OUT/analysis-snapshot.json"
assert_file "$OUT/engineering-risks.json"

run_cc architecture . --json
assert_file "$OUT/architecture.json"
run_cc impact . "$SOURCE_FILE" --json
assert_file "$OUT/change-impact.json"

cp "$OUT/architecture.json" "$OUT/architecture-baseline.json"
run_cc architecture-drift . --baseline "$OUT/architecture-baseline.json" --json
assert_file "$OUT/architecture-drift.json"

cat > "$ROOT/.codecontext-architecture-contract.json" <<'JSON'
{
  "schemaVersion": "1.0",
  "maxFindings": 100000,
  "maxCycles": 100000,
  "allowedSeverities": ["LOW", "MEDIUM", "HIGH", "CRITICAL"],
  "requiredRules": []
}
JSON
PROBE=""
cleanup() {
  if [ -n "$PROBE" ]; then rm -f "$PROBE"; fi
  rm -f "$ROOT/.codecontext-architecture-contract.json"
}
trap cleanup EXIT

run_cc architecture-contract . --json
assert_file "$OUT/architecture-contract.json"

run_cc context-snapshot . --json
assert_file "$OUT/engineering-context.json"
cp "$OUT/engineering-context.json" "$OUT/engineering-context-baseline.json"
run_cc context-diff "$OUT/engineering-context-baseline.json" "$OUT/engineering-context.json" --json
assert_file "$OUT/engineering-context-diff.json"

# One-month temporal sampling keeps the live test representative while bounded on public CI runners.
run_cc evolution --months 1 --interval 30
run_cc pr-intelligence . --base HEAD~1 --head HEAD --json
assert_file "$OUT/pr-intelligence.json"

run_cc repo-qa "Which files are the main architectural hotspots?" --path . --evidence-output "$OUT/grounded-evidence.json" > "$OUT/repo-qa.json"
assert_file "$OUT/repo-qa.json"
assert_file "$OUT/grounded-evidence.json"
run_cc plan "Improve the repository safely" --evidence "$OUT/grounded-evidence.json" --output "$OUT/engineering-plan.json"
assert_file "$OUT/engineering-plan.json"
run_cc prepare "Improve the repository safely"
assert_file "$OUT/engineering-context.json"
assert_file "$OUT/engineering-plan.json"

# Mutate only the temporary clone. Never push this change.
PROBE_DIR="$ROOT/src/test/java/org/codecontext/e2e"
mkdir -p "$PROBE_DIR"
PROBE="$PROBE_DIR/CodeContextLiveProbe.java"
printf '%s\n' 'package org.codecontext.e2e;' 'public final class CodeContextLiveProbe { private CodeContextLiveProbe() {} }' > "$PROBE"

run_cc context-snapshot . --json
cp "$OUT/engineering-context.json" "$OUT/engineering-context-mutated.json"
run_cc context-diff "$OUT/engineering-context-baseline.json" "$OUT/engineering-context-mutated.json" --json
cp "$OUT/engineering-context-diff.json" "$OUT/engineering-context-mutated-diff.json"

run_cc pr-intelligence . --json
cp "$OUT/pr-intelligence.json" "$OUT/pr-intelligence-working-tree.json"
run_cc prepare "Safely validate the temporary live repository change"
run_cc verify --output "$OUT/verify.json"
assert_file "$OUT/verify.json"

rm -f "$PROBE"
PROBE=""
run_cc context-snapshot . --json
cp "$OUT/engineering-context.json" "$OUT/engineering-context-restored.json"

export MCP_EVIDENCE="$OUT/mcp-responses.json"
python3 "$GITHUB_WORKSPACE/.github/scripts/mcp-smoke.py" "$CLI" "$ROOT"

printf '%s\n' "Live E2E harness completed successfully for $ROOT"
