#!/usr/bin/env python3
"""Minimal stdio MCP client for the Citrus MCP server.

Usage: client.py <tool-name> [json-arguments]
Spawns the server, initializes, calls the tool, prints the result text.
Used by the citrus-tests skill dry-run protocol (see README.md in this
directory) to drive the same MCP server the agent image bakes.

Environment:
  CITRUS_MCP_JAR  path to the citrus-mcp-server uber-jar
                  (default: /tmp/opencode/citrus-mcp/citrus-mcp-server-5.1.0-runner.jar)
"""
import json
import os
import subprocess
import sys

JAR = os.environ.get(
    "CITRUS_MCP_JAR",
    "/tmp/opencode/citrus-mcp/citrus-mcp-server-5.1.0-runner.jar")


def main():
    tool = sys.argv[1]
    args = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}
    p = subprocess.Popen(
        ["/usr/bin/java", "-jar", JAR],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL, text=True)

    def send(msg):
        p.stdin.write(json.dumps(msg) + "\n")
        p.stdin.flush()

    def recv(id_):
        while True:
            line = p.stdout.readline()
            if not line:
                raise RuntimeError("server closed")
            resp = json.loads(line)
            if resp.get("id") == id_:
                return resp

    send({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {
        "protocolVersion": "2024-11-05", "capabilities": {},
        "clientInfo": {"name": "skill-dryrun", "version": "0.1"}}})
    recv(1)
    send({"jsonrpc": "2.0", "method": "notifications/initialized"})
    send({"jsonrpc": "2.0", "id": 2, "method": "tools/call",
          "params": {"name": tool, "arguments": args}})
    resp = recv(2)
    if "result" in resp:
        print(resp["result"]["content"][0]["text"])
    else:
        print(json.dumps(resp, indent=2), file=sys.stderr)
        sys.exit(1)
    p.terminate()


if __name__ == "__main__":
    main()
