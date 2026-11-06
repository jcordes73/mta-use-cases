# RHDA agent (draft)

A standalone agent image exposing the **Red Hat Dependency Analytics (RHDA)**
vulnerability tools as **MCP tools** to the agentic platform - separate from
the `migiq` agent image, which carries no MCP servers.

## What it provides

The image bakes [`ruromero/rhda-mcp-server`](https://github.com/ruromero/rhda-mcp-server)
(stdio MCP server wrapping the RHDA/Trustify backend) into the goose harness
configuration, so every `AgentRun` of this image gets the tools:

| Tool | Purpose |
|------|---------|
| `analyze_dependency_vulnerabilities` | Scan a manifest (pom.xml, package.json, …) for CVEs |
| `explain_vulnerability` | Detailed assessment of one CVE |
| `generate_remediation_plan` | Actionable fix/mitigation steps |
| `suppress_dependency_vulnerability` | Ignore a confirmed false positive |

## How the baking works

The migration harness starts `goose serve` with provider/model **env vars
only** and never writes goose's config file, so MCP servers ("extensions")
shipped in `$XDG_CONFIG_HOME/goose/config.yaml` load in every `AgentRun`.
The Containerfile pins `XDG_CONFIG_HOME` (uid 1001's `HOME` depends on the
base image's `/etc/passwd` entry), copies `goose-config.yaml`, installs
Node.js, and clones + installs the server under `/opt/rhda-mcp-server` (it
is not published to npm - `tsx` runs it from source).

## Build and run (draft - not yet built)

```bash
oc new-build --name mta-rhda-agent --strategy=docker --binary -n openshift-mta
oc patch bc mta-rhda-agent -n openshift-mta --type=json \
  -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'
oc start-build mta-rhda-agent -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/rhda --follow
oc apply -f mta-ui/agentic-workflow/rhda/rhda-agent.yaml
```

Verify by firing a test `AgentRun` that asks the agent to list its tools,
then checking the sandbox pod log for `MCP server started: rhda` (a failure
logs `Failed to create MCP server ...`).

## Open items

- **Pin the upstream revision**: the repo has no release tags; set
  `--build-arg RHDA_MCP_REF=<commit-sha>` (or edit the `ARG`) for
  reproducible builds.
- **Intel server URL**: `--intel-server-url` is required, but no production
  instance is publicly documented - the draft uses the documented stage
  example `https://intel.stage.devshift.net`.
- The agent pods have direct 443 egress (the `mta-egress` NetworkPolicy),
  so no Squid allowlist change is needed for `rhda.rhcloud.com` from agent
  runs; the Squid allowlist only matters for the workspaces/hub.
