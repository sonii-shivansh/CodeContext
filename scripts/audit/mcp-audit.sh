#!/usr/bin/env bash
set -euo pipefail
APP="./build/install/vericore/bin/vericore"
mkdir -p output
printf '%s\n' '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"audit-lab","version":"1.0"}}}' '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' '{"jsonrpc":"2.0","id":3,"method":"ping","params":{}}' '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"codecontext_get_engineering_reality","arguments":{"repoPath":"."}}}' '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"codecontext_get_context_snapshot","arguments":{"repoPath":"."}}}' '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"codecontext_get_change_contract","arguments":{"repoPath":"."}}}' | "$APP" mcp > output/mcp-audit.jsonl
python3 - <<'PY'
import json
responses=[json.loads(line) for line in open('output/mcp-audit.jsonl') if line.strip()]
by_id={x.get('id'):x for x in responses}
assert by_id[1].get('result',{}).get('protocolVersion')
assert by_id[3].get('result') == {}
tools=by_id[2].get('result',{}).get('tools',[])
names={t.get('name') for t in tools}
required={'codecontext_analyze_repository','codecontext_impact_analysis','codecontext_get_engineering_reality','codecontext_get_context_snapshot','codecontext_get_change_contract','codecontext_prepare_change','codecontext_verify_change'}
assert not required-names, required-names
for i in (4,5,6):
    result=by_id[i].get('result'); assert result and result.get('isError') is False
    assert ''.join(x.get('text','') for x in result.get('content',[]))
text=''.join(x.get('text','') for x in by_id[6]['result']['content']); contract=json.loads(text)
assert len(contract.get('fingerprint',''))==64 and contract.get('repository')
PY
echo "MCP audit: PASS"
