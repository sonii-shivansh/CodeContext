#!/usr/bin/env bash
set -euo pipefail
APP="./build/install/codecontext/bin/codecontext"
REPO="$(pwd)"
[[ -x "$APP" ]] || { echo "CLI not installed: $APP" >&2; exit 1; }
run(){ echo "==> $*"; "$@"; }
run "$APP" --version
run "$APP" --help
for command in analyze impact architecture architecture-drift architecture-contract context-snapshot reality pr-intelligence repo-qa plan prepare verify ask evolution server mcp setup doctor; do run "$APP" "$command" --help >/dev/null; done
run "$APP" analyze "$REPO" >/dev/null
run "$APP" reality "$REPO" --json >/dev/null
run "$APP" architecture "$REPO" --json >/dev/null
run "$APP" context-snapshot "$REPO" --json >/dev/null
# evolution operates on the current working repository and intentionally has no positional repo argument.
run "$APP" evolution >/dev/null
rm -rf output
run "$APP" prepare "audit release workflow" --path "$REPO" >/dev/null
[[ -s output/engineering-context.json && -s output/engineering-plan.json && -s output/agent-change-contract.json ]]
run "$APP" verify --path "$REPO" --plan output/engineering-plan.json --contract output/agent-change-contract.json --output output/verification.json >/dev/null
[[ -s output/verification.json ]]
echo "CLI audit: PASS"
