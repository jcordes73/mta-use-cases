# AGENTS.md

## What this repo is

An OpenShift deployment guide + manifests for a **Migration Toolkit for Applications (MTA) PoC**. There is no application code, no build, no CI. `README.md` (~2700 lines) is the guide and the source of truth — every directory maps to a README section. `troubleshooting/README.md` documents known failure modes with exact fixes; check it before debugging any component from scratch.

## Manifests require envsubst — never plain `oc apply`

Every YAML here carries `${VAR}` placeholders. Applying without substitution stores the placeholders literally:

```bash
cat <file>.yaml | envsubst | oc apply -f -
```

- `envsubst` only substitutes **exported** shell variables — sourcing an env file without `export` silently produces empty values (e.g. href `https://git.`).
- Some files intentionally use a whitelist (`envsubst '$OPENSHIFT_APPS_DOMAIN'`), e.g. `jira/jira-cloud-search-rewriter.yaml`, where nginx's own `$args` must survive.
- Required variables are listed in the README *Prerequisites* section (`OPENSHIFT_APPS_DOMAIN`, `OPENAI_*`, `KEYCLOAK_*`, …). `OPENSHIFT_APPS_DOMAIN` comes from `oc get ingresses.config.openshift.io cluster`.

## Deployment order matters

The README's section order is the dependency order: RWX storage (probe classes; if none binds → install ODF, AWS manifests work as-is) → Agent Sandbox operator → Keycloak (`mta-rhbk` **in `openshift-mta`**) → MTA operator → Tackle CR → Konveyor Gateway → Gitea → Dev Spaces. Gitea's OIDC config needs the MTA route to exist first — if deployed earlier, re-run `helm upgrade` afterwards. Gitea is Helm (chart pinned via `--version 12.7.0`) with stackable envsubst'd values files (`values.yaml` + `values-proxy.yaml` + `values-keycloak.yaml`).

## Cluster reality checks

- Target clusters are **OpenShift sandboxes that expire**. The kubeconfig accumulates dead contexts — verify `oc whoami` (and DNS) before running any `oc` commands; a `no such host` error means the sandbox is gone, not a tooling problem.
- **Iron rule: never touch an existing Keycloak installation outside the MTA namespaces.** Sandboxes may ship a pre-existing `keycloak` namespace (the sandbox's own SSO) or other third-party Keycloak/RHBK instances — do not modify, restart, delete, or reconfigure any Keycloak CR, pod, route, realm, or operator outside `openshift-mta`. The PoC Keycloak is exclusively the `mta-rhbk` CR in `openshift-mta`; all realm/client work happens there (via the realm import or the `mta-rhbk` Admin API).
- Operator versions are pinned via `startingCSV` in the Subscription manifests. Remove/bump that line to install newer versions (see README *Components and Versions*).

## Rule tests (the only test suite)

`custom-rulesets/` follows the konveyor/rulesets layout (`stable/<lang>/<tech>/`, one `ruleset.yaml` per directory, rule IDs increment by 10). Tests run with [Kantra](https://github.com/konveyor/kantra), not `oc`:

```bash
# single test file
kantra test custom-rulesets/stable/java/eap8-custom/tests/eap8-custom-example.test.yaml
# all tests (CI-style: mirror rulesets into ~/.config/.kantra/rulesets/{stable,preview} first)
JVM_MAX_MEM=3072m RUN_LOCAL=1 kantra test ~/.config/.kantra/rulesets
```

Java test data must be buildable Maven projects; needs podman/docker locally.

## Repo hygiene

- `*.pem` (e.g. `gitea-ssh-route-{cert,key}.pem`) are gitignored, locally generated, 365-day artifacts — regenerate per the README SSH section; don't commit them and don't be surprised they're untracked.
- Secrets (`OPENAI_API_KEY`, `KEYCLOAK_*` passwords, Gitea tokens) live only in shell env vars / cluster Secrets, never in files. The `*_secret.yaml` manifests are templates for envsubst.
- Agentic migration workflows live under `mta-ui/agentic-workflow/<name>/` (workflow YAML + agent YAMLs + `skills/*/SKILL.md`); each workflow's skills are consumed by MTA agentic runs, so edits there change agent behavior, not just docs.
