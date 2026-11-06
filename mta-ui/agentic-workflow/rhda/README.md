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

## Skills

The image hosts **two skills** (both registered via the `rhda-skillcards`
SkillCards and grouped in the `rhda-skills` SkillCollection):

- **`rhda-cve-scan`** (`skills/rhda-cve-scan/SKILL.md`): a guided workflow
  for CVE-scanning Maven projects - it enumerates the project's `pom.xml`
  manifests (including multi-module aggregators), runs
  `analyze_dependency_vulnerabilities` per manifest, writes a consolidated
  `rhda-cve-report.md`, and gates the remediation/suppression tools behind
  user confirmation (remediation plans are presented, never auto-applied;
  suppression only on explicit request). The scan is strictly scoped to
  the application's repository path, and the findings are reported back
  to the MTA Hub (see below).
- **`app-dependency-map`** (`skills/app-dependency-map/SKILL.md`): a
  portfolio-wide interdependency mapper - it enumerates every Hub
  application, resolves Maven coordinates from the workspace sources,
  matches dependencies between applications via the deterministic
  `app-dependency-map.py` (baked to `/usr/local/bin/`; EAR aggregators and
  their modules fall out naturally), and reconciles the Hub's
  application-dependency graph (`POST /dependencies {"from": ...,"to":...}`).
  Dry-run first, idempotent on apply; stale edges are only ever pruned on
  explicit user request, and only those created by the same Hub user.
  Launch the `app-dependency-map` workflow (`app-dependency-map-workflow.yaml`)
  from the MTA UI on an application whose repository is the monorepo
  holding the portfolio's sources.

## Scan scope

The harness clones the ENTIRE repository of the application's Hub record -
the record's `repository.path` is never used by the clone. The scan is
scoped by two layers:

- the workflow's optional **`path`** parameter (the Hub record's repository
  path, e.g. `helloworld`), bound into the stage instructions;
- the image entrypoint, which (while the `HUB_*` env is still set) fetches
  the application record from the Hub and writes its `repository.path` to
  `/tmp/konveyor/hub-report/app-path` - the **authoritative** source,
  overriding whatever the instructions say.

## Hub reporting

CVEs identified during a scan are reported back to the MTA Hub, where they
appear as first-class issues in the application's Issues/Reports views -
ingested through the same path the static analyzer uses,
`POST /applications/:id/analyses`.

Two image components make this work around the harness's deliberate
credential isolation (`hub.ClearEnv()` strips `HUB_BASE_URL`/`HUB_TOKEN`/
`HUB_TOKEN_ID`/`APP_ID` before the agent starts):

- **`entrypoint-wrapper.sh`** (the image ENTRYPOINT, wrapping
  `migration-harness`): runs while the Hub env is still set and provisions
  `/tmp/konveyor/hub-report/{hub-url,app-id,token,app-name,app-path}` -
  the token as a 0600 file, revoked by the harness at run end as usual.
- **`hub-report.py`** (installed as `/usr/local/bin/rhda-hub-report.py`):
  deterministic converter from a findings JSON (schema documented in the
  skill's step 7) to the hub's GS-delimited (`\x1D`) analysis manifest -
  one Insight per CVE (`ruleset: rhda`, incident on the pom manifest,
  severity-mapped effort, links to the advisory) plus TechDependency
  records - and POST of that manifest. The agent never hand-writes the
  YAML; the model only transcribes the MCP scan results into JSON.

If `/tmp/konveyor/hub-report/token` does not exist (standalone runs without
Hub credentials), the reporting step is skipped.

## How the baking works

The migration harness starts `goose serve` with provider/model **env vars
only** and never writes goose's config file, so MCP servers ("extensions")
shipped in `$XDG_CONFIG_HOME/goose/config.yaml` load in every `AgentRun`.
The Containerfile pins `XDG_CONFIG_HOME` (uid 1001's `HOME` depends on the
base image's `/etc/passwd` entry), copies `goose-config.yaml`, installs
Node.js, and clones + installs the server under `/opt/rhda-mcp-server` (it
is not published to npm - `tsx` runs it from source).

## Workflows vs. standalone runs

- **`rhda-agent-workflow.yaml`** wraps the agent as the UI-launchable
  **`rhda`** workflow (single `scan` stage, optional `path` parameter -
  the application's repository path, e.g. `helloworld`). Launch it from
  the MTA UI: the UI provisions the Hub credentials the sandbox needs
  (the Hub requires authentication since Keycloak was provisioned).
- **`rhda-agentrun.yaml`** is a standalone `AgentRun` draft. It must carry
  an `envFrom` secret with `HUB_TOKEN`/`HUB_TOKEN_ID` (a hub token minted
  via `POST /auth/tokens` with a federated Keycloak JWT) or the run fails
  at hub resolution with `Token [] not-authenticated`.

## Build and run

```bash
oc new-build --name mta-rhda-agent --strategy=docker --binary -n openshift-mta
oc patch bc mta-rhda-agent -n openshift-mta --type=json \
  -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'
oc start-build mta-rhda-agent -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/rhda --follow
oc apply -f mta-ui/agentic-workflow/rhda/skills/rhda-skillcards.yaml
oc apply -f mta-ui/agentic-workflow/rhda/skills/rhda-skillcollection.yaml
oc apply -f mta-ui/agentic-workflow/rhda/rhda-agent.yaml
oc apply -f mta-ui/agentic-workflow/rhda/rhda-agent-workflow.yaml
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
