#!/usr/bin/env bash
set -euo pipefail
APP="./build/install/codecontext/bin/codecontext"
PORT="18080"
LOG="/tmp/codecontext-rest-audit.log"
"$APP" server --host 127.0.0.1 --port "$PORT" >"$LOG" 2>&1 &
PID=$!
cleanup(){ kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; }
trap cleanup EXIT
for _ in $(seq 1 30); do if curl -fsS "http://127.0.0.1:${PORT}/health" >/tmp/codecontext-health.json 2>/dev/null; then break; fi; sleep 1; done
curl -fsS "http://127.0.0.1:${PORT}/" >/tmp/codecontext-root.json
curl -fsS "http://127.0.0.1:${PORT}/health" >/tmp/codecontext-health.json
curl -fsS "http://127.0.0.1:${PORT}/health/live" >/tmp/codecontext-live.json
curl -fsS "http://127.0.0.1:${PORT}/health/ready" >/tmp/codecontext-ready.json
status=$(curl -sS -o /tmp/codecontext-invalid.json -w '%{http_code}' -X POST "http://127.0.0.1:${PORT}/analyze" -H 'Content-Type: application/json' -d '{"repoPath":"https://github.com/example/example.git"}')
if [[ "$status" =~ ^2|^3 ]]; then echo "REST security audit failed: remote repository URL accepted" >&2; exit 1; fi
echo "REST audit: PASS"
