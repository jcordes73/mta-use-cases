---
name: app-dependency-map
description: Discovers inter-application dependencies across the whole MTA application portfolio and records them in the MTA Hub's application-dependency graph. Use when the user asks to map application interdependencies, find which applications depend on each other, build an application dependency graph, update application dependencies in MTA, or detect interconnected applications (e.g. EAR aggregators and their modules). Triggers on phrases like "application dependencies", "interdependencies", "which apps depend on each other", "dependency graph", "interconnected applications", "update dependencies in MTA". Requires Hub access (the image entrypoint provisions the token) and the application sources in the workspace.
---

# Application Dependency Map (MTA portfolio)

Discover how the applications registered in the MTA Hub depend on **each
other**, and record those edges in the Hub's application-dependency graph
(`POST /dependencies {"from": {"id": A}, "to": {"id": B}}` - the same model
the UI's application dependency views and migration-wave planning use).

The mapping is **deterministic**: the script computes the map from Maven
coordinates; you orchestrate the run, investigate what it flags, and write
the report. Never hand-write dependency edges into the Hub.

## Prerequisites

- **Hub access**: the image entrypoint provisions `/tmp/konveyor/hub-report/
  {hub-url, token}` while the run's Hub env is set (UI-launched runs do this
  automatically). If the token file is missing, the script exits 1 - ask the
  user for a Hub token or launch the run from the MTA UI.
- **The sources in the workspace must cover the applications**: the sandbox
  clones the repository of the application this run belongs to. For a
  portfolio mapping, launch the run on an application whose repository is
  the monorepo containing the other applications (e.g. a quickstarts
  monorepo) - every Hub application whose `repository.path` exists in the
  workspace can be mapped; the rest are skipped and reported.

## Workflow

### 1. Survey the portfolio

Run the mapper in **dry-run mode first** (no writes):

```bash
python3 /usr/local/bin/app-dependency-map.py \
  --workspace /workspace/repo --output /tmp/app-deps.json
```

It prints and writes a JSON map:

- `applications_resolved` - Hub applications whose `repository.path` exists
  in the workspace and whose `pom.xml` parses (the Maven-coordinate
  registry)
- `applications_skipped` - applications not mappable from this workspace,
  each with a reason (no pom.xml / not in this repo / unreadable pom)
- `edges` - the discovered inter-application dependencies
  (`{from_app_id: [to_app_id, ...]}`), derived by matching every
  application's pom `<dependencies>` against the registry. EAR aggregators
  fall out naturally: an ear module's pom declares its sibling war/jar
  artifacts as dependencies.

Read the skipped list carefully: applications skipped because their source
is not in this workspace are simply out of scope for this run - say so in
the report rather than treating them as dependency-free.

### 2. Sanity-check the discovered edges

Before writing anything, verify the map is plausible:

- spot-check 2-3 edges by reading the dependent pom yourself: the
  `groupId:artifactId` of the dependency must really be the target
  application's coordinate
- treat aggregator poms (packaging `pom`, e.g. a monorepo root or
  `<modules>` aggregator) with care: their `<dependencies>` often describe
  shared tooling, not runtime coupling. If an aggregator produces edges
  that look like build-tooling rather than application coupling, exclude
  that application's edges and say so in the report
- look for near-miss coordinates in the skipped/duplicate warnings (two Hub
  applications with the same coordinates are both excluded from matching)

### 3. Apply the map to the Hub

Once the edges are sane, create the missing ones:

```bash
python3 /usr/local/bin/app-dependency-map.py \
  --workspace /workspace/repo --output /tmp/app-deps-applied.json --apply
```

Reconciliation semantics (deterministic, in the script):

- **create**: edges the map found that the Hub lacks
  (`POST /dependencies` with `{"from": {"id": A}, "to": {"id": B}}`)
- **keep**: edges the Hub already has (`GET /dependencies?from.id=A`) - idempotent
- **stale**: Hub edges this map does not justify - **reported, not deleted**.
  Only with `--apply --prune` are stale edges deleted, and only those
  created by the same Hub user that this run authenticates as (edges from
  other users/tools are always kept and listed)

If the user explicitly asks to prune stale edges, run with `--apply
--prune` and report exactly which edges were deleted; never prune without
that explicit request.

### 4. Report

Write `app-dependency-map.md` at the repository root:

- the edge list in human form: `A (id) depends on B (id)` with the shared
  Maven coordinate and whether the dependency is direct in B's pom
- the applications skipped (with reasons) - the map's coverage boundary
- the reconciliation outcome: created / already present / stale (kept or
  pruned) / failures, verbatim from the script's summary line
- anything excluded by the sanity check in step 2, and why

Then record a `## Dependency map` summary in `.konveyor/handoff.md`
(applications mapped, edges found, hub writes applied) and commit the
report so it persists as git history.

## Constraints

- All Hub writes go through the script - do not POST/PUT/DELETE
  `/dependencies` by hand with curl.
- The map only covers Maven-coordinate matching from the workspace sources.
  Runtime coupling that never appears as a pom dependency (JNDI lookups,
  HTTP clients, message queues between applications) is out of scope for
  the automatic edges; if you notice evidence of it while reading poms,
  list it as "candidates for manual review" in the report - do not create
  edges for it.
- If the token file is missing (standalone run without Hub env), do not
  emulate the Hub API; state that Hub access is not provisioned and stop
  at the report.
