# app-dependency-map agent (portfolio interdependency mapper)

A standalone agent image that maps **inter-application dependencies across
the whole MTA application portfolio** and records them in the MTA Hub's
application-dependency graph (`POST /dependencies {"from": {"id": A},
"to": {"id": B}}` - the model the UI's application dependency views and
migration-wave planning use). Separate from the `rhda` agent image (no MCP
servers needed here - the mapping is deterministic code plus Hub REST).

## What it provides

The image bakes the **`app-dependency-map`** skill
(`skills/app-dependency-map/SKILL.md`, registered via the
`app-dependency-map-skillcards` SkillCard and the
`app-dependency-map-skills` SkillCollection) and its deterministic mapper
(`app-dependency-map.py`, installed as `/usr/local/bin/app-dependency-map.py`):

1. **Enumerate** the Hub's applications (`GET /applications`).
2. **Resolve** each application's Maven coordinates (`groupId:artifactId`)
   from its `pom.xml` in the workspace, located via the Hub record's
   `repository.path`.
3. **Match** every application's pom `<dependencies>` against the other
   applications' coordinates - each match is an inter-application edge
   (EAR aggregators and their modules fall out naturally: an ear pom
   declares its sibling war/jar artifacts as dependencies).
4. **Reconcile** with the Hub's dependency graph: create missing edges
   (`--apply`), keep existing ones (idempotent - the static analyzer may
   already have created edges), and only ever **prune stale edges on
   explicit user request** (`--apply --prune`), and only those created by
   the same Hub user this run authenticates as (`GET /auth/self`).

The model never writes edges by hand: it orchestrates, sanity-checks the
map (aggregator-pom tooling dependencies are the known false-positive
class), applies, and reports. Applications whose sources are not in the
workspace are skipped and reported as the map's coverage boundary.

## Launching

Launch the **`app-dependency-map`** workflow from the MTA UI on an
application whose repository is the monorepo holding the portfolio's
sources (e.g. the quickstarts): the sandbox clones the whole repository, so
every Hub application whose `repository.path` resolves in the workspace is
in scope. The workflow takes an optional **`dry-run`** parameter - set it
to `true` to compute and report the map without writing anything to the
Hub.

## Hub credentials

The entrypoint (`entrypoint-wrapper.sh`, wrapping `migration-harness`) runs
while the run's `HUB_*` env is still set and provisions
`/tmp/konveyor/hub-report/{hub-url, app-id, token (0600), app-name,
app-path}` - the mapper reads `hub-url` and `token` from there
(`/run/konveyor` is mounted READ-ONLY in the sandbox, so the directory
lives under `/tmp`). Standalone runs without Hub credentials fail fast
with a clear message.

## Build and run

```bash
oc new-build --name mta-app-dependency-map-agent --strategy=docker --binary -n openshift-mta
oc patch bc mta-app-dependency-map-agent -n openshift-mta --type=json \
  -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'
oc start-build mta-app-dependency-map-agent -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/app-dependency-map --follow
oc apply -f mta-ui/agentic-workflow/app-dependency-map/skills/app-dependency-map-skillcards.yaml
oc apply -f mta-ui/agentic-workflow/app-dependency-map/skills/app-dependency-map-skillcollection.yaml
oc apply -f mta-ui/agentic-workflow/app-dependency-map/app-dependency-map-agent.yaml
oc apply -f mta-ui/agentic-workflow/app-dependency-map/app-dependency-map-workflow.yaml
```

Verify the Hub contract independently:

```bash
# edge lifecycle (the mapper's write path):
oc exec deploy/mta-ui -n openshift-mta -- curl -s -u admin:admin -X POST \
  -H "Content-Type: application/json" \
  -d '{"from": {"id": 3}, "to": {"id": 4}}' \
  http://mta-hub.openshift-mta.svc:8080/dependencies
# list and delete by id when done testing
```
