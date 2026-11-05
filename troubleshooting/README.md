Prerequisite: `OPENAI_HOST` must be set (used as the default destination by the helper), e.g.
```bash
export OPENAI_HOST=maas.apps.ocp.cloud.rhai-tmm.dev
```
Create the mtu-test pod
```bash
oc apply -f mtest-pod.yaml
oc wait --for=condition=Ready pod/mtu-test --timeout=120s
```
Find which hop caps the size
The helper runs bounded tracepath (starting at the interface MTU) and analyzes
the output: identifies the hop that lowers the PMTU, unanswered hops
(firewalls), and whether the destination was reached. It then re-runs
tracepath at the lowest MTU identified to confirm the path is clean there.
If it ends before the destination, rerun with `MAX_HOPS=30` rather than
assuming nothing caps the path.
```bash
./analyze-tracepath.sh
```
Delete the mtu-test pod
```bash
oc delete pod mtu-test
```

## Keycloak: firstName/lastName required in the user profile

The MTA realm's user profile declares `firstName` and `lastName` as required
attributes (`"required": {"roles": ["user"]}`), so users are forced to fill
them in when completing their Keycloak profile. The `KeycloakRealmImport`
manifest (`mta-ui/mta-realm-import.yaml`) cannot carry the user profile
configuration — in Keycloak 26 the declarations live in a separate store
managed via the `/users/profile` admin endpoint, not part of the
`RealmRepresentation` — so the change must be applied to the running realm and
re-applied after a cluster rebuild.

To check the current configuration, query the Admin REST API from the
`mta-ui` pod (the Keycloak pod has no `curl`, and `kcadm.sh --insecure` does
not bypass the PKIX trust check for the self-signed serving cert):

```bash
KC_PASS=$(oc get secret mta-keycloak-rhbk -n openshift-mta -o jsonpath='{.data.password}' | base64 -d)
oc exec deploy/mta-ui -n openshift-mta -- bash -c "
TOKEN=\$(curl -sk https://mta-rhbk-service.openshift-mta.svc:8443/auth/realms/master/protocol/openid-connect/token -d grant_type=password -d client_id=admin-cli -d username=admin -d password='$KC_PASS' | sed -n 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/p');
curl -sk -H \"Authorization: Bearer \$TOKEN\" 'https://mta-rhbk-service.openshift-mta.svc:8443/auth/admin/realms/mta/users/profile'"
```

Each attribute carries a `required` field with the roles it is mandatory for.
Remove the flag from `firstName`/`lastName` by fetching the config, deleting
their `required` fields with `jq`, and PUT-ing the full object back:

```bash
KC_PASS=$(oc get secret mta-keycloak-rhbk -n openshift-mta -o jsonpath='{.data.password}' | base64 -d)
oc exec deploy/mta-ui -n openshift-mta -- bash -c "
TOKEN=\$(curl -sk https://mta-rhbk-service.openshift-mta.svc:8443/auth/realms/master/protocol/openid-connect/token -d grant_type=password -d client_id=admin-cli -d username=admin -d password='$KC_PASS' | sed -n 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/p');
curl -sk -H \"Authorization: Bearer \$TOKEN\" 'https://mta-rhbk-service.openshift-mta.svc:8443/auth/admin/realms/mta/users/profile'" \
| jq '.attributes |= map(if .name == "firstName" or .name == "lastName" then del(.required) else . end)' \
| oc exec deploy/mta-ui -n openshift-mta -i -- bash -c "
TOKEN=\$(curl -sk https://mta-rhbk-service.openshift-mta.svc:8443/auth/realms/master/protocol/openid-connect/token -d grant_type=password -d client_id=admin-cli -d username=admin -d password='$KC_PASS' | sed -n 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/p');
curl -sk -X PUT -H \"Authorization: Bearer \$TOKEN\" -H 'Content-Type: application/json' --data @- -o /dev/null -w 'PUT HTTP %{http_code}\n' 'https://mta-rhbk-service.openshift-mta.svc:8443/auth/admin/realms/mta/users/profile'"
```

The `PUT` is a full-object update — the fetched config is modified and written
back rather than a partial patch. Verify with the `GET` above: the
`firstName` and `lastName` attributes should no longer carry a `required`
field, while `username`/`email` keep their existing settings.

## Jira (Hub tracker): integration pitfalls

The Jira tracker integration (see "Issue Tracking (Jira integration)" in the
main README) surfaced four distinct failure modes. All of them surface through
the same place: the tracker's stored `message` field (`GET ${MTA_URL}/hub/trackers/<id>`)
— the Hub's background manager tests trackers on a 10-second (disconnected) /
60-second (connected) cycle and writes the last test's result there; the
`GET /trackers/{id}/projects` endpoint never tests on demand, it only replays
that stored result.

### 1. `unsupported identity kind`

The tracker package resolves the identity kind against exact string constants:
the Basic-auth kind is **`basic-auth`** (hyphenated), not `basicAuth`. An
identity with a different kind fails every connector operation. Fix: `PUT`
the identity with `kind: "basic-auth"` (full object, including `user` and
`password` — the API does not return secrets on `GET`, so fetch nothing, send
the full payload).

### 2. Connection test returns `401` although the token is valid

The connector authenticates with Basic auth `user:password` = Atlassian
account email : API token. A **401 with a valid token means the email was
empty or wrong** — e.g. the environment variable was unset when the identity
was created, which silently produces an identity with `user: ""`. Diagnose by
testing the credential pair directly, outside the cluster:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -u "${JIRA_EMAIL}:${JIRA_API_TOKEN}" \
  "https://${JIRA_HOST}/rest/api/2/myself"
```

If this returns 200 while the tracker's message reports 401, the identity is
the problem: re-`PUT` it with the correct `user`. (The authoritative email is
also visible in the JSON returned by `https://<site>/rest/api/2/myself` from
a logged-in browser session.) Also verify the Hub pod's egress path: the Hub
routes through the Squid proxy, so the Jira host domain must be in the proxy
allowlist — a blocked host fails with `403`, not `401`.

### 3. Tickets are created but their status never syncs

Hub log (`oc logs deploy/mta-hub -n openshift-mta | grep -A1 'Failed to refresh'`)
shows: *"The requested API has been removed. Please migrate to the
/rest/api/3/search/jql API"* — Atlassian removed `/rest/api/2/search` from
Jira Cloud (changelog CHANGE-2046) while the go-jira client embedded in the
Hub still uses it for the refresh cycle. Ticket creation, the connection test,
and project browsing are unaffected. Fix: deploy the search-API rewriter
(`jira/jira-cloud-search-rewriter.yaml`) and point the tracker's URL at
`http://jira-rewriter.openshift-mta.svc:8080` — the rewriter rewrites the
removed path to `/rest/api/3/search/jql` and injects the mandatory
`fields=status` parameter (the v3 endpoint returns no fields by default).
The Hub's `NO_PROXY` covers `.svc`, so the rewriter hop bypasses Squid while
the rewriter itself talks to Jira Cloud directly.

### 4. Tracker POST rejected: `Key: 'Tracker.Name' Error:Field validation for 'Name' failed on the 'required' tag`

The `name` field is mandatory on `POST /trackers` (and distinct from the
`identity.name`). The example command in the main README includes it.


## Dev Spaces: workspace git clone fails with `Permission denied (publickey)`

The devfile's `clone-git` `postStart` command (or a manual `git clone
ssh://git@git-ssh...:443/...` in the workspace terminal) fails with
`git@git-ssh...: Permission denied (publickey)` although the TLS tunnel, the
`ProxyCommand`, and the Gitea key registration are all fine. `ssh -vv` in the
workspace shows the config being read (including
`99-git-proxy.conf`) but **no key being offered** — no `Offering public key`
line before the denial.

Root cause: the Dev Spaces server's `SshConfigConfigurator` is documented to
auto-generate an `ssh_config` key (with `IdentityFile /etc/ssh/dwo_ssh_key`)
in the `git-ssh-key` Secret, but **verified live it never runs** in this
setup — the Secret keeps only the keys it was created with, `/etc/ssh/ssh_config`
stays the stock file, and SSH offers only the default `~/.ssh/id_*` identities,
none of which Gitea knows. Fix: ship the `ssh_config` in the Secret explicitly
(see the [Git Authentication with SSH Keys](../README.md#git-authentication-with-ssh-keys)
section) — the `watch-secret` label propagates it into running workspaces,
which restart to pick up the mount. Verify with
`ssh -G git-ssh.$OPENSHIFT_APPS_DOMAIN | grep identityfile` (expect
`identityfile /etc/ssh/dwo_ssh_key`).

Two adjacent pitfalls hit while debugging:

- **scp-style `ssh git@host:443` is not a port specification** — `ssh -T
  git@host:443` treats `host:443` as the hostname, the `ProxyCommand` expands
  to `-connect host:443:22` and OpenSSL 3.5 fails with `s_client: -connect
  argument or target parameter malformed or ambiguous`. Use the `ssh://` URL
  form (`ssh://git@host:443/...`) or `ssh -p 443 git@host`.
- **The key must be registered on a Gitea account with access to the repo** —
  SSH keys authenticate as their owning user; a key that Gitea does not know
  surfaces as `Permission denied (publickey)` (as does an unoffered key per
  the root cause above), while a key that authenticates but lacks access to
  the repository returns `Repository does not exist or you do not have
  access` instead.

## MTA: analyses and discovery tasks fail or never start

Analyses in MTA are **on-demand** — no setup step triggers them automatically
(only the automatic `language-discovery`/`tech-discovery` tasks are created
per imported application). If neither the discovery tasks nor an explicitly
triggered analysis ever completes, two independent pitfalls apply, both
verified live:

### 1. `mkdir /cache/m2: permission denied` — every task pod fails

The task pods (analyzer `kantra`, `tech-discovery`, `tech-discovery` addons)
create `/cache/m2` (the shared Maven cache) at startup. If the directory does
not exist — or exists but is only writable by the creating pod's random UID —
every task fails within seconds with `mkdir /cache/m2: permission denied`
(`oc logs <task-pod> -c addon`). Note that `language-discovery` does not
touch the cache, so it succeeds while everything else fails — a
language-succeeds/tech-fails split is the tell-tale sign.

Fix: create the directory world-writable via the
`cache-pvc-inspector` deployment (the [MTA installation](../README.md#installation)
step now does both):

```bash
echo "mkdir /cache/m2 && chmod 777 /cache/m2" | oc exec deployment/cache-pvc-inspector -n openshift-mta -i -t -- bash -il
```

A plain `mkdir` is not enough: the inspector pod runs as a random UID, so the
directory it creates is owned by that UID and every other task pod (also a
random UID) is denied.

### 2. Dead task pods exhaust the scheduler's pod quota

The hub's task scheduler enforces a pod limit (10 in this deployment) counted
over **existing** task pods, including pods in `Error` or `Completed` state
that have not been reaped. When the quota is exhausted the scheduler logs

```
[task-scheduler] Task pods created. capacity=1 created=0 quota=10/10 requested=118 scheduled=0
```

and **no task is ever scheduled again** — tasks pile up in `Ready`,
`Postponed` (the `Rule:Unique` one-task-per-application policy) and
`QuotaBlocked` states, and explicitly triggered analyses sit queued
indefinitely. The failed task *records* show no error (`error: null`) because
the pods died before reporting.

Fix: delete the dead pods to free the quota — the scheduler immediately
schedules fresh pods for the queued tasks:

```bash
oc get pods -n openshift-mta | grep -E '^task-' | awk '$3=="Error"{print $1}' | \
  xargs -r oc delete pod -n openshift-mta
```

### 3. Retrying a failed task requires a new task, not a re-submit

A task in `Failed` state cannot be re-queued: `PUT /tasks/:id/submit` returns
204 but leaves the state untouched. Re-run it by creating a fresh task with
the same `kind`, `application`, and `data` (fetch the shape from
`GET /tasks/:id`) and submitting **with the task object as the PUT body** — an
empty-body `PUT /tasks/:id/submit` (or `PUT /taskgroups/:id/submit` without
the taskgroup object, as the UI sends it) fails silently or returns
`400 EOF`:

```bash
curl -s "${MTA_URL}/hub/tasks" -u admin:admin | jq '.[] | select(.state=="Failed")'
NEW=$(curl -s -X POST "${MTA_URL}/hub/tasks" -u admin:admin -H "Content-Type: application/json" \
  -d '{"name": "<app>.<id>.tech-discovery", "kind": "tech-discovery", "priority": 1,
       "application": {"id": <id>, "name": "<app>"}, "data": <data-from-the-failed-task>}')
curl -s -X PUT "${MTA_URL}/hub/tasks/$(echo $NEW | jq -r .id)/submit" -u admin:admin \
  -H "Content-Type: application/json" -d "$NEW"
```

Analyses are triggered the same way the UI's Analyze wizard does it: a
taskgroup of `kind: analyzer` referencing the analysis profile
(`data.profile`), submitted with `PUT /taskgroups/:id/submit` and the
taskgroup object as body.
