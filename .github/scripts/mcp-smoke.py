#!/usr/bin/env python3
"""Exercise the packaged CodeContext MCP stdio server against a live repository."""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path


def request(proc: subprocess.Popen[str], payload: dict) -> dict:
    assert proc.stdin is not None and proc.stdout is not None
    proc.stdin.write(json.dumps(payload) + "\n")
    proc.stdin.flush()
    line = proc.stdout.readline()
    if not line:
        raise RuntimeError("MCP server closed stdout before returning a response")
    return json.loads(line)


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: mcp-smoke.py <codecontext-cli> <repo-path>", file=sys.stderr)
        return 2

    cli = Path(sys.argv[1]).resolve()
    repo = Path(sys.argv[2]).resolve()
    proc = subprocess.Popen(
        [str(cli), "mcp"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1,
    )

    responses: list[dict] = []
    try:
        responses.append(request(proc, {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}))
        responses.append(request(proc, {"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}))
        responses.append(request(proc, {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}))

        initialize = responses[0]
        if initialize.get("result", {}).get("serverInfo", {}).get("name") != "CodeContext":
            raise AssertionError("MCP initialize did not identify CodeContext")
        if initialize.get("result", {}).get("protocolVersion") != "2025-11-25":
            raise AssertionError("Unexpected MCP protocol version")

        tools = responses[2].get("result", {}).get("tools", [])
        names = {tool.get("name") for tool in tools}
        expected = {
            "codecontext_analyze_repository",
            "codecontext_impact_analysis",
            "codecontext_architecture_analysis",
            "codecontext_pr_intelligence",
        }
        if names != expected:
            raise AssertionError(f"MCP tool surface mismatch: expected {sorted(expected)}, got {sorted(names)}")

        source = next(repo.rglob("*.java"))
        relative_source = source.relative_to(repo).as_posix()
        calls = [
            (3, "codecontext_analyze_repository", {"repoPath": str(repo)}),
            (4, "codecontext_impact_analysis", {"repoPath": str(repo), "changedPaths": [relative_source]}),
            (5, "codecontext_architecture_analysis", {"repoPath": str(repo)}),
            (6, "codecontext_pr_intelligence", {"repoPath": str(repo), "baseRevision": "HEAD~1", "headRevision": "HEAD"}),
        ]
        for request_id, name, arguments in calls:
            response = request(proc, {
                "jsonrpc": "2.0",
                "id": request_id,
                "method": "tools/call",
                "params": {"name": name, "arguments": arguments},
            })
            responses.append(response)
            if "error" in response:
                raise AssertionError(f"MCP tool {name} returned an error: {response['error']}")
            if response.get("result", {}).get("isError") is True:
                raise AssertionError(f"MCP tool {name} returned isError=true")
            content = response.get("result", {}).get("content", [])
            if not content or not content[0].get("text"):
                raise AssertionError(f"MCP tool {name} returned no text content")

        bad = request(proc, {
            "jsonrpc": "2.0",
            "id": 7,
            "method": "tools/call",
            "params": {"name": "unknown_tool", "arguments": {"repoPath": str(repo)}},
        })
        responses.append(bad)
        if bad.get("error", {}).get("code") != -32602:
            raise AssertionError("Unknown MCP tool was not rejected with -32602")

        unsafe = request(proc, {
            "jsonrpc": "2.0",
            "id": 8,
            "method": "tools/call",
            "params": {"name": "codecontext_analyze_repository", "arguments": {"repoPath": "https://example.com/repo"}},
        })
        responses.append(unsafe)
        if unsafe.get("error", {}).get("code") != -32602:
            raise AssertionError("Remote MCP repository path was not rejected")

        Path(os.environ.get("MCP_EVIDENCE", repo / "output" / "mcp-responses.json")).write_text(
            json.dumps(responses, indent=2) + "\n",
            encoding="utf-8",
        )
        print(json.dumps({"status": "passed", "tools": sorted(names), "responses": len(responses)}))
        return 0
    finally:
        if proc.stdin:
            proc.stdin.close()
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait(timeout=5)
        stderr = proc.stderr.read() if proc.stderr else ""
        if stderr:
            Path(repo / "output" / "mcp-stderr.log").write_text(stderr, encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
