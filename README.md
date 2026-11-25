# Migration Toolkit for Applications Use-Cases

The [**Migration Toolkit for Applications**](https://developers.redhat.com/products/mta) (MTA) helps you migrate existing applications. Its main use cases:

1. **Legacy app assessment** — Static analysis of source code and archives: application inventory, technologies used, migration effort/cost estimates (sizing for modernization programs).
2. **Migration to newer platforms** — Move Java apps from older servers (WebLogic, WebSphere, JBoss EAP 4/5/6/7, Tomcat) to newer JBoss EAP or other runtimes.
3. **Lift-and-shift to cloud / containers** — Migrate monoliths to OpenShift/Kubernetes, containerize applications, move from on-premise to hybrid cloud.
4. **Java version upgrades** — Upgrade JDK versions, migrate from proprietary APIs to open standards (e.g., Java EE → Jakarta EE, Oracle JDK → OpenJDK).
5. **Monolith decomposition** — Find candidates for refactoring into microservices; highlight hard-coded dependencies and configuration issues.
6. **Governance at scale** — Central reporting across a large portfolio (wave planning): plan, prioritize, and track modernization programs.

MTA works in two modes: the **web console** (portfolio analysis, project/wave management) and the **CLI** (developer-driven, single-app analysis). Both produce detailed reports of issues, triggered rules, and suggested code changes. There's also IDE integration (VS Code) for inline remediation hints.

This PoC has two parts:

1. **[Automated Migration Using the MTA UI](#automated-migration-using-the-mta-ui)** — portfolio analysis, agentic migrations, and wave management through the MTA web console.
2. **[Developer-Infused Migration Using OpenShift Dev Spaces and the MTA Plugin](#developer-infused-migration-using-openshift-dev-spaces-and-the-mta-plugin)** — developers drive analysis and remediation from their IDE — in a [Dev Spaces workspace](#mta-plugin-setup) or [locally in desktop VS Code](#local-installation-desktop-vs-code-without-dev-spaces).

## Contents

<!-- TOC: regenerate from the headings after structural edits; slugs follow GitHub's anchor rules -->

- [Contents](#contents)
- [Architecture Overview](#architecture-overview)
- [Components and Versions](#components-and-versions)
  - [Platform & Infrastructure](#platform--infrastructure)
  - [MTA Stack](#mta-stack)
  - [Developer-Infused Migration](#developer-infused-migration)
  - [Workstation Tooling](#workstation-tooling)
- [Prerequisites](#prerequisites)
- [Squid proxy deployment (optional, disconnected environments)](#squid-proxy-deployment-optional-disconnected-environments)
- [Gitea installation](#gitea-installation)
  - [Install Gitea with Helm](#install-gitea-with-helm)
  - [Verify the OpenAPI endpoint](#verify-the-openapi-endpoint)
  - [Create an admin access token](#create-an-admin-access-token)
  - [Clone the application repositories](#clone-the-application-repositories)
  - [Migrating SSH-key-authenticated source repositories](#migrating-ssh-key-authenticated-source-repositories)
  - [Proxy settings (disconnected environments)](#proxy-settings-disconnected-environments)
  - [OIDC SSO login via Keycloak](#oidc-sso-login-via-keycloak)
  - [SSH Keys](#ssh-keys)
- [Automated Migration Using the MTA UI](#automated-migration-using-the-mta-ui)
  - [Agent Sandbox Installation](#agent-sandbox-installation)
  - [Keycloak for MTA](#keycloak-for-mta)
  - [Migration Toolkit for Applications (MTA) 8.3](#migration-toolkit-for-applications-mta-83)
    - [Installation](#installation)
      - [Deploy the Migration Toolkit for Applications Operator](#deploy-the-migration-toolkit-for-applications-operator)
      - [Create the Model Secret](#create-the-model-secret)
      - [Configure RWX storage](#configure-rwx-storage)
      - [Configure the Tackle Instance](#configure-the-tackle-instance)
      - [Workaround for a stuck cache PVC](#workaround-for-a-stuck-cache-pvc)
      - [Automate cleanup of dead task pods](#automate-cleanup-of-dead-task-pods)
      - [Create a Konveyor Gateway](#create-a-konveyor-gateway)
      - [Trusting a private root CA on the LLM endpoint (optional)](#trusting-a-private-root-ca-on-the-llm-endpoint-optional)
      - [Accessing the MTA UI](#accessing-the-mta-ui)
      - [Add an MTA entry to the OpenShift console](#add-an-mta-entry-to-the-openshift-console)
      - [Add a Gitea entry to the OpenShift console](#add-a-gitea-entry-to-the-openshift-console)
    - [Proxy settings for disconnected environments (optional)](#proxy-settings-for-disconnected-environments-optional)
    - [Setting the Hub proxy via the Tackle Hub API](#setting-the-hub-proxy-via-the-tackle-hub-api)
    - [Automation via REST API](#automation-via-rest-api)
      - [Creating Stakeholders, Stakeholder Groups and Business Service](#creating-stakeholders-stakeholder-groups-and-business-service)
      - [Configure Maven settings](#configure-maven-settings)
      - [Bulk Uploading Applications](#bulk-uploading-applications)
      - [Importing dependencies for an existing portfolio](#importing-dependencies-for-an-existing-portfolio)
      - [Create default Git credentials](#create-default-git-credentials)
      - [Issue Tracking (Jira integration, optional)](#issue-tracking-jira-integration-optional)
      - [Define an Analysis Profile](#define-an-analysis-profile)
      - [Create an Assessment Questionnaire](#create-an-assessment-questionnaire)
      - [Create an Archetype](#create-an-archetype)
      - [Add a Review to the Archetype](#add-a-review-to-the-archetype)
      - [Create a Migration Wave](#create-a-migration-wave)
  - [Your first Agentic Workflow](#your-first-agentic-workflow)
    - [Assemble the Agentic Workflow components](#assemble-the-agentic-workflow-components)
    - [Start an Agent Workflow Run](#start-an-agent-workflow-run)
  - [Additional Agentic Workflows](#additional-agentic-workflows)
    - [Spring Boot to Quarkus](#spring-boot-to-quarkus)
    - [Apache Ant to Apache Maven](#apache-ant-to-apache-maven)
    - [MigIQ](#migiq)
    - [RHDA (dependency CVE/license scanner with Hub reporting)](#rhda-dependency-cvelicense-scanner-with-hub-reporting)
    - [Application dependency map (portfolio interdependencies)](#application-dependency-map-portfolio-interdependencies)
    - [Citrus tests (test authoring via the Citrus MCP server)](#citrus-tests-test-authoring-via-the-citrus-mcp-server)
  - [The container lane: in-container tests against JBoss EAP](#the-container-lane-in-container-tests-against-jboss-eap)
    - [JBoss EJB to REST (inter-application EJB couplings to REST)](#jboss-ejb-to-rest-inter-application-ejb-couplings-to-rest)
- [Developer-Infused Migration Using OpenShift Dev Spaces and the MTA Plugin](#developer-infused-migration-using-openshift-dev-spaces-and-the-mta-plugin)
  - [OpenShift Dev Spaces Installation](#openshift-dev-spaces-installation)
    - [Extension marketplace configuration](#extension-marketplace-configuration)
    - [Devfile registry](#devfile-registry)
    - [Proxy configuration](#proxy-configuration)
  - [MTA Plugin Setup](#mta-plugin-setup)
    - [Create the LLM credentials Secret](#create-the-llm-credentials-secret)
    - [Create the clone-config Secret](#create-the-clone-config-secret)
    - [Create the hub-config Secret](#create-the-hub-config-secret)
  - [Git Authentication with SSH Keys](#git-authentication-with-ssh-keys)
  - [Local Installation (Desktop VS Code Without Dev Spaces)](#local-installation-desktop-vs-code-without-dev-spaces)
- [Appendix](#appendix)
  - [Create your own skills](#create-your-own-skills)
  - [Create your own rules](#create-your-own-rules)
  - [Suggested design: Agentic run observability and a persistent event log](#suggested-design-agentic-run-observability-and-a-persistent-event-log)
## Architecture Overview

How the components of this PoC relate:

```mermaid
flowchart LR
    subgraph openshift["OpenShift Container Platform"]
        direction TB
        subgraph automated["Automated Migration Using the MTA UI"]
            direction TB
            UI["MTA UI<br/>(web console)"]
            HUB["MTA Hub<br/>(mta-hub, kai-api)<br/>incl. Solution Server / LLM proxy"]
            SANDBOX["Agent Sandbox<br/>(agents running<br/>plan → execute → verify)"]
            REWRITER["Jira rewriter<br/>(nginx: /rest/api/2/search<br/>→ /rest/api/3/search/jql)"]
            UI -->|"assessments, waves,<br/>agentic migrations"| HUB
            UI -->|"OIDC SSO login"| KC
            HUB -->|"agent runs"| SANDBOX
            HUB -->|"LLM proxy"| LLM
            HUB -->|"ticket export &<br/>status sync"| REWRITER
            REWRITER -->|"Jira Cloud<br/>via proxy"| SQUID
            SANDBOX -->|"LLM access via<br/>Konveyor Gateway"| LLM
            SANDBOX -->|"Maven builds<br/>via proxy"| SQUID
            SANDBOX -->|"commit migrated code"| GITEA
            HUB -.->|"RWX PVCs<br/>(Maven cache, bucket)"| ODF
        end
        subgraph developer["Developer-Infused Migration Using Dev Spaces"]
            direction TB
            WS["Dev Spaces Workspace<br/>(devfile)"]
            PLUGIN["MTA Plugin<br/>(redhat.mta-core + redhat.mta-java)"]
            OPENCODE["OpenCode<br/>(extension + CLI)"]
            WS --> PLUGIN
            WS --> OPENCODE
        end
        subgraph shared["Shared Platform Services"]
            direction TB
            KC["Keycloak (RHBK)<br/>(SSO for MTA UI<br/>and Gitea)"]
            ODF["ODF<br/>(RWX storage class)"]
        end
    end
    LLM["Self-hosted LLM<br/>(OpenAI-compatible endpoint)"]
    GITEA["Gitea<br/>(application source code,<br/>OIDC SSO via Keycloak)"]
    SQUID["Squid proxy<br/>(disconnected environments)"]
    JIRA["Jira Cloud<br/>(migration-wave tickets)"]
    EXT["External hosts<br/>(GitHub, Open VSX, Maven repos,<br/>OpenCode install, ...)"]
    PLUGIN -->|"Hub connection<br/>(analysis, profiles)"| HUB
    OPENCODE -->|"opencode.json<br/>(OPENAI_BASE_URL)"| LLM
    WS -->|"clone source code"| GITEA
    WS -->|"GitHub / Open VSX /<br/>LLM endpoint via proxy"| SQUID
    GITEA -->|"repo migration<br/>via proxy"| SQUID
    SQUID -->|"allowlisted<br/>destinations"| EXT
    SQUID -->|"atlassian.net<br/>(allowlisted)"| JIRA
```

Both parts share the same **self-hosted LLM**:

- The MTA plugin reaches it through the Hub's LLM proxy, agentic workflows through the Konveyor Gateway, and the OpenCode agent directly via `OPENAI_BASE_URL` in `opencode.json`.
- **Keycloak** (Red Hat build of Keycloak) is the shared identity provider: MTA UI logins use OIDC SSO against the `mta` realm, and Gitea uses the same realm as an OIDC auth source.
- **ODF** provides the RWX storage class for the Tackle instance's PVCs (Maven cache, Hub bucket) — auto-detected, or installed via ODF when the platform ships none (see the *Configure RWX storage* step in the [MTA installation](#migration-toolkit-for-applications-mta-83)).
- The **Jira integration** exports migration tickets from the Hub to Jira Cloud. Jira Cloud removed the `/rest/api/2/search` endpoint the Hub still calls for status sync, so this traffic flows through a small in-cluster rewriter (see [Issue Tracking (Jira integration)](#issue-tracking-jira-integration-optional)).
- The **Squid proxy** is optional, only deployed in disconnected environments, and provides allowlisted outbound egress — GitHub, Open VSX, the Maven repositories (`repo.maven.apache.org`, `maven.repository.redhat.com`), Atlassian, and the LLM endpoint — for the workspaces, Gitea repo migration, the Hub (LLM calls, Jira sync), and agentic Maven builds. In a connected environment these components reach external hosts directly.

## Components and Versions

Versions validated for this PoC: OpenShift 4.22 sandbox, November 2026. Operator versions marked *(catalog)* are the newest CSVs the connected OperatorHub catalog served at validation time — the manifests pin them via `startingCSV`; remove or bump that line to install newer versions. The Gitea Helm chart is pinned via `--version` in the install commands.

### Platform & Infrastructure

| Component | Version | Notes |
|---|---|---|
| OpenShift Container Platform | 4.22 (Kubernetes v1.35) | cluster-admin access required |
| OpenShift Data Foundation (ODF) | 4.22 (`odf-operator.v4.22.7-rhodf`, pinned) | Provides the RWX storage class (`ocs-storagecluster-cephfs` internal / `ocs-external-storagecluster-cephfs` external); installed automatically by the storage detection step when the platform ships no RWX-capable class. Pinned manifests: `storage/odf-subscription.yaml` + `storage/storage-cluster.yaml` |
| cert-manager Operator | 1.20.1 *(catalog)* | Usually pre-installed |

### MTA Stack

| Component | Version | Source |
|---|---|---|
| MTA Operator | **8.3.3** (pinned) | `redhat-operators`, channel `stable-v8` |
| └ mta-ui / mta-hub / mta-solution-server / mta-agentic-controller | 8.3.3 (CSV digest-pinned) | `registry.redhat.io/mta/*-rhel9` |
| RHBK Operator (Red Hat build of Keycloak) | 26.6.9-opr.1 (pinned) | `redhat-operators`, channel `stable-v26` — `keycloak/keycloak-operator.yaml` |
| └ Keycloak server | 26.6.9.redhat-00002 | `rhbk/keycloak-rhel9` |
| PostgreSQL (Keycloak database) | 16 (`rhel9/postgresql-16`) | `registry.redhat.io` |
| Agent Sandbox Operator | 0.9.0 (pinned) | `redhat-operators`, channel `preview-0.9` |
| Migration skill images (built on-cluster) | 0.0.1 | UBI 9 micro base, one per agentic workflow |

### Developer-Infused Migration

| Component | Version | Source |
|---|---|---|
| OpenShift Dev Spaces Operator | 3.31.3 (pinned) + DevWorkspace Operator 1.0.0 | `redhat-operators`, channel `stable` — `devspaces/openshift-devspaces-operator.yaml` |
| Gitea | 1.27 (rootless) | Helm chart `gitea-12.7.0` (`dl.gitea.com/charts`), pinned via `--version` |
| Squid proxy (disconnected environments) | 5.5 (`rhel9/squid`, pinned) | `registry.redhat.io` |
| MTA VS Code extensions | 8.3.1 (`redhat.mta-core`, `redhat.mta-java`) | Open VSX |
| Dependency Analytics extension | 1.1.0 (`redhat.fabric8-analytics`) | Open VSX |
| YAML / XML extensions | rolling (`redhat.vscode-yaml`, `redhat.vscode-xml`) | Open VSX |
| OpenCode extension + CLI | `sst-dev.opencode` (latest) | Open VSX |

### Workstation Tooling

| Tool | Version |
|---|---|
| `oc` CLI | 4.20+ |
| `helm` CLI | 3.x/4.x (v4.2.2 used for validation) |
| `yq` | 4.x (v4.53.3 used for validation) |
| `jq` | any recent |

## Prerequisites

To get started with this PoC, you need:

- **OpenShift CLI (oc)**: installed and logged in to the target cluster with `cluster-admin` privileges.
- **Helm CLI**: installed (v3.x) for deploying the Gitea chart.
- **OpenShift Cluster Access**: an operational OpenShift cluster (v4.22 recommended) with enough resources for Dev Spaces and MTA workloads.
- **Subscription Requirements**: an active Red Hat Advanced Developer Suite (RHADS) subscription, for Developer Lightspeed services.
- **Storage & Registry Access**: a default StorageClass with dynamic PVC provisioning, plus access to the Red Hat Ecosystem Catalog/OperatorHub.
- **OpenAI-Compatible LLM Access**: an OpenAI-compatible LLM endpoint and API key, required for Red Hat Developer Lightspeed and agentic migration features.
- **Environment variables**: define these now to ease the later deployment of Kubernetes resources:

```bash
export OPENAI_HOST=<Your LLM endpoint host>
export OPENAI_BASE_URL=https://${OPENAI_HOST}/v1
export OPENAI_API_KEY="<Your API Key>"
export OPENAI_MODEL_NAME="<Your Model name>"
# RWX storage class for the MTA PVCs - auto-detected, or installed via ODF,
# by the "Configure RWX storage" step of the MTA installation. Override here
# only if the class is known upfront (e.g. ocs-external-storagecluster-cephfs
# for an external ODF).
export STORAGECLASS_RWX=ocs-external-storagecluster-cephfs
# Passwords for the MTA Keycloak instance (keycloak/, see "Keycloak for MTA")
# - generate fresh values, e.g. with `openssl rand -hex 16`
export KEYCLOAK_DB_PASSWORD=<password>
export KEYCLOAK_ADMIN_PASSWORD=<password>
# The Gitea OIDC client secret, defined in the realm import
# (mta-ui/mta-realm-import.yaml - change it there and here if you rotate it)
export KEYCLOAK_GITEA_CLIENT_SECRET=gitea-client-secret
export OPENSHIFT_APPS_DOMAIN=$(oc get ingresses.config.openshift.io cluster -o jsonpath='{.spec.domain}')
# Jira Cloud (optional - only for the issue-tracking integration, see
# "Issue Tracking (Jira integration)"): the <site>.atlassian.net host, the
# Atlassian account email and an API token (create one at
# https://id.atlassian.com -> Security -> API tokens)
export JIRA_HOST=<your-site>.atlassian.net
export JIRA_EMAIL=<Atlassian account email>
export JIRA_API_TOKEN=<API token>
```

Verify the LLM endpoint is reachable and the model responds with a test request via `curl`. All `curl` commands in this guide use `-k` (`--insecure`) because the cluster routes and the LLM endpoint present certificates not in the workstation's trust store (self-signed sandbox certificates) — remove the flag for endpoints with publicly trusted certificates:

```bash
curl -k -s ${OPENAI_BASE_URL}/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${OPENAI_API_KEY}" \
  -d '{
    "model": "'"${OPENAI_MODEL_NAME}"'",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 262000
  }' | jq .
```

The response should contain the model's completion, e.g.:

```json
{
  "id": "chatcmpl-...",
  "model": "<Your Model name>",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I help you today?"
      },
      "finish_reason": "stop"
    }
  ],
  ...
}
```

> ⚠️ NOTE
>
> The Konveyor Gateway verifies its credential by calling the endpoint's **`/v1/models`** route, which is typically stricter about authentication than `chat/completions` (some gateways accept an arbitrary key for completions but return `401` for the model listing). Verify `/v1/models` explicitly with the real API key before deploying — if it returns `401`, the Gateway reports `Ready=False` with *"Endpoint ... returned HTTP 401 - check the credential secret's API key"* even though test completions succeed:
>
> ```bash
> curl -k -s -o /dev/null -w "%{http_code}\n" ${OPENAI_BASE_URL}/models \
>   -H "Authorization: Bearer ${OPENAI_API_KEY}"
> ```
>
> The expected output is `200`.

## Squid proxy deployment (optional, disconnected environments)

In a disconnected environment, workloads still need controlled outbound access to external hosts. Deploy an in-cluster Squid proxy for this egress:

- Which workloads: Gitea (migrates original repositories from GitHub), Dev Spaces workspaces (GitHub, Open VSX registry), and the MTA Hub (analyzer package repositories).
- How it works: a forward proxy — workloads point at it via standard `HTTP_PROXY`/`HTTPS_PROXY` env vars, set from the `MTA_`-prefixed shell variables in [Proxy configuration](#proxy-configuration).
- Restriction: only allowlisted destinations are reachable.

Deploy the proxy with `squid/squid.yaml`, which creates:

- The `squid` namespace, a `squid-config` ConfigMap with the `squid.conf.template` configuration, a Deployment running the Red Hat `rhel9/squid` image, and a Service exposing port 3128.
- A dynamic LLM endpoint: the template carries an `$OPENAI_HOST` placeholder, **not hardcoded**, substituted at pod start by a `render-config` init container.
- Its value comes from the `OPENAI_HOST` env var of a small `llm-endpoint` Secret in the `squid` namespace.
- The Squid pod cannot reference the workspace `llm-credentials` Secret — cross-namespace Secret references are not allowed.

Create that Secret first with the same envsubst pattern as the Dev Spaces secrets, then apply the manifest:

```bash
export OPENAI_HOST=${OPENAI_HOST:-$(echo "$OPENAI_BASE_URL" | sed -E 's#https?://([^/]+).*#\1#')}
oc apply -f squid/squid-namespace.yaml
cat squid/llm-endpoint-secret.yaml | envsubst | oc apply -f -
oc apply -f squid/squid.yaml
oc wait --for=create deployment squid -n squid --timeout=300s
oc wait --for=condition=Available deployment squid -n squid --timeout=300s
```

`OPENAI_BASE_URL` is the same variable exported for the [LLM credentials Secret](#create-the-llm-credentials-secret), so the allowlist follows the LLM endpoint automatically — set `OPENAI_BASE_URL` (or override `OPENAI_HOST` directly) before running the commands. The Secret is only created when it does not already exist — an `OPENAI_HOST` set earlier (e.g. from a previous run) is kept and the Secret is not recreated.

The Squid configuration only permits `CONNECT` (HTTPS) requests to port 443, restricts proxy clients to the cluster networks (adjust the `cluster_networks` ACL to the cluster's Pod CIDR), and only allows destinations in the `allowed_domains` ACL. The pid file and coredump directory are configured as `/tmp` because OpenShift runs containers with a random UID (restricted SCC). The allowlist covers:

- `.github.com`, `.open-vsx.org` — GitHub and Open VSX.
- `.githubusercontent.com` — a different parent domain that must be allowlisted separately: the Dev Spaces factory devfile fetch pulls the devfile from `raw.githubusercontent.com`, so without it workspace creation fails with "failed to fetch devfile" and falls back to the empty default stack (no devfile extensions installed).
- `.eclipsecontent.org` — the Open VSX extension-download CDN.
- `.opencode.ai` — the OpenCode CLI installer.
- `.developers.redhat.com` and `access.cdn.redhat.com` — Red Hat product documentation and CDN content.
- `rhda.rhcloud.com` — the Red Hat Dependency Analytics backend; the `redhat.fabric8-analytics` extension sends dependency manifests there for vulnerability and license analysis. The extension supports HTTP proxies since 0.10.0, so the workspace's `HTTPS_PROXY` routes it through Squid.
- `repo.maven.apache.org` (Maven Central) and `maven.repository.redhat.com` (Red Hat GA/EAP artifacts) — needed for the Maven builds and the `eap-maven-plugin` server provisioning in the agentic workflow's verify stage. Without them the build fails with `403 Forbidden` from the proxy and the agent has to bypass the proxy in its own `~/.m2/settings.xml`.
- `.atlassian.net` — Jira Cloud, used by the Hub's [tracker](#issue-tracking-jira-integration-optional); the Hub routes its egress through the proxy.
- The **OpenCode LLM endpoint** host (`$OPENAI_HOST`, rendered from the `llm-endpoint` Secret) — the workspace containers' `HTTPS_PROXY` routes all external HTTPS traffic through the proxy, so without it every OpenCode request fails with `Proxy refused to open a tunnel: 403 Forbidden` even though the model is configured correctly.

> Verified live: the model appeared in `opencode models`, but the request still died at the proxy with `Proxy refused to open a tunnel: 403 Forbidden` — the LLM endpoint host was missing from the allowlist.

The rendered file is produced at pod start, so changing the LLM endpoint requires re-creating the `llm-endpoint` Secret **and** a pod restart. To allow further destinations, edit the `squid.conf.template` in the `squid-config` ConfigMap and restart the deployment — the rendered file is generated only by the init container, so changes are not picked up by the running pod:

```bash
oc edit configmap squid-config -n squid
oc rollout restart deployment squid -n squid
```

Verify that the proxy is running and forwards traffic to an allowed destination:

```bash
oc exec -n squid deploy/squid -- curl -k -s -o /dev/null -w '%{http_code}\n' -x http://localhost:3128 -I https://github.com
```

The command should return `200`. Note that requests from inside the Squid pod match the unrestricted `http_access allow localhost` ACL, so this check does not exercise the `cluster_networks` and `allowed_domains` restrictions — to verify the proxy the way a workload sees it, run the same check from a Dev Spaces workspace terminal against `http://squid.squid.svc:3128` instead.

Finally, export the `MTA_`-prefixed proxy environment variables to point at the in-cluster service (used by the [Gitea](#gitea-installation), [MTA](#proxy-settings-for-disconnected-environments-optional) and Dev Spaces proxy sections). The `MTA_` prefix avoids conflicts with the proxy settings of the shell you run the subsequent commands from:

```bash
export MTA_HTTP_PROXY=http://squid.squid.svc:3128
export MTA_HTTPS_PROXY=http://squid.squid.svc:3128
export MTA_NO_PROXY=localhost,127.0.0.1,.svc,.cluster.local,.${OPENSHIFT_APPS_DOMAIN}
```

> ⚠️ NOTE
>
> - `MTA_NO_PROXY` lists the cluster-internal hosts, so traffic between the workloads (e.g. Gitea, MTA Hub, Dev Spaces) bypasses the proxy.
> - The Squid pod itself needs outbound connectivity through the corporate firewall to the allowed destinations (e.g. via the node IP or a dedicated egress IP). If the cluster nodes have no outbound connectivity at all, deploy the proxy outside the cluster instead and point the environment variables at it.
> - In a disconnected environment the `rhel9/squid` image (only available from `registry.redhat.io` and requiring a Red Hat pull secret) must be mirrored into the internal image registry; update `squid/squid.yaml` accordingly before applying it. In a connected environment, attach the pull secret to the default service account of the `squid` namespace before applying the manifest.
> - OpenShift runs containers with a random UID (restricted SCC), so `squid.conf` configures the pid file (`pid_filename /tmp/squid.pid`) and the coredump directory (`coredump_dir /tmp`) as writable locations.

## Gitea installation

To protect the original source code from unwanted changes, the repos are cloned into a Gitea instance running on OpenShift.

### Install Gitea with Helm

To install the Gitea Helmchart run
```bash
oc apply -f gitea/gitea-namespace.yaml
helm repo add gitea-charts https://dl.gitea.com/charts/
helm repo update
cat gitea/values.yaml | envsubst | helm install -n gitea -f - gitea gitea-charts/gitea --version 12.7.0
oc wait --for=jsonpath='{.status.readyReplicas}'=1 statefulset gitea-valkey-primary -n gitea --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 statefulset gitea-postgresql -n gitea --timeout=600s
cat gitea/create-db.sh | oc exec statefulset/gitea-postgresql -n gitea -i -t -- bash -il
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment gitea -n gitea --timeout=600s
```
Verify the deployment by opening the URL printed by:
```bash
echo "Gitea URL: https://$(oc get route gitea -n gitea -o jsonpath='{.spec.host}')"
``` 
You should see the following page:
![image](images/gitea_ui.png)

Log in via `Sign in` with `gitea-admin/giteaftw!`.    

> ⚠️ NOTE
>
> The chart defaults to `RollingUpdate`, which deadlocks on the single RWO data PVC — the new pod waits with *"Multi-Attach error for volume ... Volume is already used by pod(s) ..."* while the old pod keeps serving. `gitea/values.yaml` sets `strategy.type: Recreate`; without it a later `helm upgrade` stalls the rollout.
### Verify the OpenAPI endpoint

Check the Gitea Open API endpoint with
```bash
curl -k -s -o /dev/null -w "%{http_code}\n" https://git.${OPENSHIFT_APPS_DOMAIN}/api/swagger
```
Expected output: `200`.
### Create an admin access token

You'll need an access token for later steps.

```bash
export GITEA_ADMIN_TOKEN=$(curl -k -s -H "Content-Type: application/json" -d '{"name":"admin_token","scopes":["all"]}' -u 'gitea-admin:giteaftw!' "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/users/gitea-admin/tokens" | jq -r '.sha1')
```
Verify the token works:
```bash
curl -k -s -o /dev/null -w "%{http_code}\n" \
"https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/user" \
-H "Authorization: token ${GITEA_ADMIN_TOKEN}"
```
This should return `200`.

To delete the token:
```bash
curl -k --request DELETE https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/users/gitea-admin/tokens/admin_token   --header "Authorization: Basic $(echo -n 'gitea-admin:giteaftw!' | base64)"
```

### Clone the application repositories

Clone the [JBoss EAP Quickstarts](https://github.com/jboss-developer/jboss-eap-quickstarts) repository into Gitea via the REST API. This also creates the organization `jboss`.

```bash
curl -k -s -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/orgs" \
-H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
-H "Content-Type: application/json" \
-d '{"username": "jboss", "visibility": "public"}'

curl -k -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/repos/migrate" \
-H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
-H "Content-Type: application/json" \
-d '{
    "clone_addr": "https://github.com/jboss-developer/jboss-eap-quickstarts",
    "repo_name": "jboss-quickstarts",
    "repo_owner": "jboss"
}'

curl -k -s -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/admin/users" \
-H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
-H "Content-Type: application/json" \
-d '{
    "username": "jboss-dev",
    "email": "jboss-dev@example.com",
    "password": "jboss-dev",
    "must_change_password": false
}'

export GITEA_OWNERS_TEAM_ID=$(curl -k -s "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/orgs/jboss/teams"   -H "Authorization: token ${GITEA_ADMIN_TOKEN}" | jq '.[].id')

curl -k -s -X PUT "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/teams/${GITEA_OWNERS_TEAM_ID}/members/jboss-dev"   -H "Authorization: token ${GITEA_ADMIN_TOKEN}"
```
Verify the clone succeeded:
```bash
git clone https://git.${OPENSHIFT_APPS_DOMAIN}/jboss/jboss-quickstarts ~/jboss-quickstarts
```
### Migrating SSH-key-authenticated source repositories

The `repos/migrate` endpoint authenticates the source with `auth_username`/`auth_password` — credentials Gitea injects **only into HTTP(S) clone addresses**. Migrating an `ssh://` `clone_addr` from a key-authenticated repository fails with a public-key error: the API has no field for an SSH private key, and Gitea's server-side `git clone` has no agent to offer one. If your source authenticates via SSH keys but **also serves HTTPS with a personal-access token** (most SSH-only setups do), migrate over that HTTPS path instead — the SSH keys stay irrelevant to the migration:

```bash
curl -k -s -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/repos/migrate" \
-H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
-H "Content-Type: application/json" \
-d '{
    "clone_addr": "https://source-git.internal.example.com/team/the-repo.git",
    "auth_username": "<source-user>",
    "auth_password": "<source-personal-access-token>",
    "repo_name": "the-repo",
    "repo_owner": "jboss",
    "mirror": false
}'
```

Set `mirror: false` deliberately: the Gitea copy is frozen (it is MTA's migration source), so Gitea should not re-fetch from the original. If the source serves HTTPS **without** token auth (SSH only), the migrate API can't be used at all — clone locally with the SSH key and push the mirror into Gitea over HTTPS with the `gitea-admin` token. After migrating, verify as above and reference the repo by its **HTTPS web URL** (`https://git.${OPENSHIFT_APPS_DOMAIN}/...`) in the application records — the analyzer and agentic harness clone with HTTP basic auth only (see [Bulk Uploading Applications](#bulk-uploading-applications)).
### Proxy settings (disconnected environments)

Gitea needs outbound access to GitHub for the migration. In a disconnected environment, route this through a proxy: define the `MTA_`-prefixed proxy variables (see [Proxy configuration](#proxy-configuration)) and install with the extra values file `gitea/values-proxy.yaml`. Both values files go through `envsubst`, so the `MTA_`-prefixed shell variables resolve automatically while the Gitea container receives standard `HTTP_PROXY`/`HTTPS_PROXY`/`NO_PROXY`:

```bash
oc apply -f gitea/gitea-namespace.yaml
helm repo add gitea-charts https://dl.gitea.com/charts/
helm repo update
helm install -n gitea \
  -f <(envsubst < gitea/values.yaml) \
  -f <(envsubst < gitea/values-proxy.yaml) \
  gitea gitea-charts/gitea --version 12.7.0
oc wait --for=jsonpath='{.status.readyReplicas}'=1 statefulset gitea-valkey-primary -n gitea --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 statefulset gitea-postgresql -n gitea --timeout=600s
cat gitea/create-db.sh | oc exec statefulset/gitea-postgresql -n gitea -i -t -- bash -il
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment gitea -n gitea --timeout=600s
```

The chart sets the proxy variables on all Gitea containers via its `deployment.env` property. Verify they propagated:

```bash
oc get pod -n gitea -o yaml | grep -iE 'proxy'
```

### OIDC SSO login via Keycloak

Gitea can authenticate users against MTA's Keycloak (see [Keycloak for MTA](#keycloak-for-mta)) — users log in with their MTA credentials, and Gitea auto-creates the local account from the token's email on first login. The chart's `configure-gitea` init container runs `gitea admin auth add-oauth` for each `gitea.oauth` entry in `gitea/values-keycloak.yaml` (its `autoDiscoverUrl` points at the MTA UI route's `/auth` path, so the MTA route must exist — deploy MTA first or re-run the `helm upgrade` afterwards). Install with all three values files (this also upgrades an existing installation):

```bash
# OPENSHIFT_APPS_DOMAIN and KEYCLOAK_GITEA_CLIENT_SECRET from the
# prerequisites section must be set
helm upgrade -n gitea \
  -f <(envsubst < gitea/values.yaml) \
  -f <(envsubst < gitea/values-proxy.yaml) \
  -f <(envsubst < gitea/values-keycloak.yaml) \
  gitea gitea-charts/gitea --version 12.7.0
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment gitea -n gitea --timeout=600s
```

Verify the auth source was registered (the `configure-gitea` init log ends with `...installed.`):

```bash
oc logs -n gitea deploy/gitea -c configure-gitea | grep -E 'oauth|installed'
```

> ⚠️ NOTE
>
> The init container fails with *"Non-success code for Discovery URL: 503"* if the discovery URL's host has no matching Route — the host is `mta-openshift-mta.${OPENSHIFT_APPS_DOMAIN}`, not `mta.${OPENSHIFT_APPS_DOMAIN}`. The failed init crash-loops the Gitea pod until you fix the values and `helm upgrade` again.

<a id="ssh-keys"></a>
### SSH Keys

To use SSH key authentication against the Git server in addition to basic auth, first generate a keypair (skip if you already have one):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/mta-gitea -N "" -C "mta-agents"
```

Then add the public key to the Git server (for Gitea, also possible via the REST API). `jq` passes the key to `curl` to ensure proper expansion and JSON escaping:

```bash
curl -k -s -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/user/keys" \
  -H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"mta-agents\",
    \"key\": \"$(cat ~/.ssh/mta-gitea.pub)\"
  }"
```

To verify the key works, clone a repository over SSH. Three details of the deployed Gitea matter:

- **SSH is exposed through a TLS-passthrough Route**: Routes carry HTTP(S) only, so the plain-SSH server (the chart's `gitea-ssh` Service, container port 2222) is wrapped in TLS by a socat proxy and published via a passthrough Route on `git-ssh.${OPENSHIFT_APPS_DOMAIN}`. The Route doesn't terminate TLS — the socat proxy presents the certificate, so a cert for the Route host is generated explicitly (the service CA operator only issues for `<service>.<namespace>.svc` names, not Route hosts):

    ```bash
    openssl req -x509 -newkey rsa:4096 -sha256 -days 365 -nodes \
      -keyout gitea-ssh-route-key.pem -out gitea-ssh-route-cert.pem \
      -subj "/CN=git-ssh.${OPENSHIFT_APPS_DOMAIN}" \
      -addext "subjectAltName=DNS:git-ssh.${OPENSHIFT_APPS_DOMAIN}"

    oc create secret tls gitea-ssh-route-cert -n gitea \
      --cert=gitea-ssh-route-cert.pem --key=gitea-ssh-route-key.pem
    ```

    Deploy the tunnel:

    ```bash
    cat gitea/gitea-ssh-route.yaml | envsubst | oc apply -f -
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment gitea-ssh-tunnel -n gitea --timeout=300s
    ```

    The self-signed cert expires after a fixed 365 days and doesn't auto-renew — re-run the `openssl` and `oc create secret` commands (and restart the tunnel deployment) to rotate it. Verify the presented cert's subject matches the Route host:

    ```bash
    echo "" | openssl s_client -connect git-ssh.${OPENSHIFT_APPS_DOMAIN}:443 -servername git-ssh.${OPENSHIFT_APPS_DOMAIN} 2>/dev/null | openssl x509 -noout -subject
    ```

    In-cluster consumers (e.g. the agentic Sandboxes) bypass the tunnel and reach the SSH server directly at `gitea-ssh.gitea.svc:2222` — the Service is **headless**, so its DNS resolves straight to the pod IP with no port translation: the URL must use the container port 2222 (the same port the tunnel's socat forwards to), not the Service's 22. Cross-namespace egress on port 2222 also requires the `allow-egress-git-ssh` NetworkPolicy (see [Allow egress on port 22 for SSH-based git](#allow-egress-on-port-22-for-ssh-based-git)).

- **The SSH username is `git`**: Gitea's built-in SSH server requires one fixed username for all git operations — the `[server] BUILTIN_SSH_SERVER_USER` setting, which defaults to the OS user running Gitea. On OpenShift that's the random container UID (e.g. `1000810000`), so `gitea/values.yaml` pins it to `git` (`gitea.config.server.BUILTIN_SSH_SERVER_USER: git`, plus `SSH_USER: git` for the clone URLs the UI shows). Any other username is rejected with `Permission denied (publickey)`; the server log records `Invalid SSH username <name> - must use ...`. The username is only a gate — the key is matched against the Gitea account it's registered with (`gitea-admin` here), not the username.

Clone the migrated repository over SSH. Because the Route is TLS-passthrough, ssh unwraps the TLS layer with an `openssl` ProxyCommand — `%h` expands to the clone URL's host (the Route host). `GIT_SSH_COMMAND` also pins the key — `IdentitiesOnly=yes` stops ssh from offering the agent's other keys first — and skips the host-key prompt and TLS verification, since the self-signed cert isn't in a trust store (verify it separately with the `openssl s_client` check above if needed):

```bash
GIT_SSH_COMMAND="ssh -i ~/.ssh/mta-gitea -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ProxyCommand='openssl s_client -quiet -connect %h:443 -servername %h'" \
git clone git@git-ssh.${OPENSHIFT_APPS_DOMAIN}:jboss/jboss-quickstarts ~/jboss-quickstarts-ssh
```

On success git prints `Cloning into ...` and the working tree appears under the target directory; the server greeting (`Hi there, gitea-admin! You've successfully authenticated with the key named mta-agents ...`) confirms the key matched.

## Automated Migration Using the MTA UI

This part covers portfolio-wide automated migration with the MTA web console, including agentic migration workflows running on the cluster.

### Agent Sandbox Installation
Agentic Migrations run agents in sandboxes, so install the **Agent Sandbox Operator** first:

```bash
oc apply -f agent-sandbox/agent-sandbox-operator.yaml
```
Verify the deployment:

```bash
oc wait --for=create deployment agent-sandbox-controller -n agent-sandbox-system --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment agent-sandbox-controller -n agent-sandbox-system --timeout=600s
```
Each command should return **`condition met`**.

### Keycloak for MTA

MTA 8.3 requires a Red Hat build of Keycloak (RHBK) for authentication. The MTA operator auto-detects a StatefulSet named `mta-rhbk` in its namespace and points the UI and Hub at it instead of deploying its own — deploying it explicitly gives you control over the database and realm content:

- **Deploy the RHBK operator, PostgreSQL and the Keycloak instance**: The manifests in `keycloak/` create the shared `openshift-mta` namespace/OperatorGroup (idempotent with the MTA operator manifest), the RHBK operator Subscription, a PostgreSQL StatefulSet (`mta-kcpgsql`), and the `mta-rhbk` Keycloak CR. Cluster domain and admin password are `envsubst` placeholders:

    ```bash
    oc apply -f keycloak/keycloak-operator.yaml
    cat keycloak/keycloak-postgresql.yaml | envsubst | oc apply -f -
    cat keycloak/keycloak.yaml | envsubst | oc apply -f -
    oc wait --for=jsonpath='{.status.conditions[?(@.type=="Ready")].status}'=True keycloak mta-rhbk -n openshift-mta --timeout=600s
    ```

    > ⚠️ NOTE
    >
    > The Keycloak CR references the TLS Secret `mta-rhbk-service-serving-cert`, minted by the service CA operator only **after** the `mta-rhbk-service` Service is annotated. If `mta-rhbk-0` sticks in `ContainerCreating` with `FailedMount: secret "mta-rhbk-service-serving-cert" not found`, annotate the Service — the pod recovers on the next mount attempt:
    >
    > ```bash
    > oc annotate svc mta-rhbk-service -n openshift-mta service.beta.openshift.io/serving-cert-secret-name=mta-rhbk-service-serving-cert --overwrite
    > ```
    >
    > The PostgreSQL password is baked in at first boot (initdb). If `keycloak-db-secret` was applied with a wrong/empty value first, later secret updates are not picked up — delete the `mta-kcpgsql-0` pod **and** its `data-mta-kcpgsql-0` PVC to re-initialize the database (symptom: `password authentication failed for user "keycloak"` in the pod log).

- **Import the `mta` realm**: The import (`mta-ui/mta-realm-import.yaml`) creates the `mta` realm with the `admin` user, the `mta-ui` client (redirect URIs for the MTA route incl. logout), the `gitea` OIDC client, and the `+role.admin` client scope granting hub scopes to federated sessions (see the troubleshooting notes):

    ```bash
    cat mta-ui/mta-realm-import.yaml | envsubst | oc apply -f -
    oc get keycloakrealmimport mta-realm-import -n openshift-mta -o jsonpath='{.status.conditions[?(@.type=="Done")].status}{"\n"}'
    ```

    Expected output: `True`.

    > ⚠️ NOTE
    >
    > Two things the import cannot carry — apply them to the running realm after each fresh deploy (exact commands in [Troubleshooting](troubleshooting/README.md)):
    >
    > - **User profile**: `firstName`/`lastName` are declared required, so login stops at a *Verify profile* form. Make both optional via the `/users/profile` Admin endpoint.
    > - A re-import resets manifest-defined users — the `admin` user's `enabled: true` and password re-apply with it.

### Migration Toolkit for Applications (MTA) 8.3

#### Installation
MTA 8.3 automates analysis, assessment, and refactoring for application modernization:
- **Prerequisites**:
    - OpenShift with cluster-admin privileges
    - Storage: 1 RWO PV (10Gi) for the Hub database, 2 RWX PVs (100Gi each) for Maven cache and Hub bucket storage (if `rwx_supported` is enabled), 1 RWO PV (5Gi) for the Developer Lightspeed database
    - Access to a Large Language Model
##### Deploy the Migration Toolkit for Applications Operator

via the CLI, creating the target namespace, OperatorGroup, and Subscription:

```bash
oc apply -f mta-ui/mta-operator.yaml
```
Verify the operator deployment:
```bash
oc wait --for=create csv mta-operator.v8.3.3 -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.phase}'=Succeeded csv mta-operator.v8.3.3 -n openshift-mta --timeout=600s
oc wait --for=create deployment mta-operator -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-operator -n openshift-mta --timeout=600s
```
Each command should return **`condition met`**.
##### Create the Model Secret

a Secret with the API key and Base URL for the self-hosted LLM provider, needed by the Solution Server (an MCP server with persistent memory of refactoring resolutions, reaching LLMs via Retrieval-Augmented Generation, RAG):
```bash
cat mta-ui/mta-ls-llm-provider-secret.yaml | envsubst | oc apply -f -
```
> ⚠️ NOTE
>
> The Solution Server's LLM proxy also consumes a Secret named **`kai-api-keys`** in `openshift-mta` — the operator's built-in default name, not referenced by the Tackle CR. Its keys become the environment variable names injected into the `kai-api` and LLM-proxy pods (e.g. an `OPENAI_API_KEY` key). Without it, the Tackle CR reports `KaiAPIKeysConfigured=False` with *"No API keys secret found"*. A template is at `mta-ui/kai-api-keys-secret.yaml`, applied with the same envsubst pattern:
> ```bash
> cat mta-ui/kai-api-keys-secret.yaml | envsubst | oc apply -f -
> ```
> The condition only updates on the operator's next reconciliation — delete the operator pod (OLM reverts `oc rollout restart` on its own deployment) to trigger one.
##### Configure RWX storage

Tackle provisions 2 RWX PVCs (100Gi each: Maven cache, Hub bucket) on top of its RWO databases, so you need an RWX-capable (`ReadWriteMany`) storage class. Many platforms ship one out of the box (Amazon EFS, Azure Files, Google Filestore, NFS, existing ODF/CephFS). Storage classes don't declare access modes, so the probe below tests each class with a throwaway `ReadWriteMany` PVC and keeps the first that binds:

```bash
RWX_SC=""
for SC in $(oc get sc -o json | jq -r '.items[].metadata.name'); do
  cat <<EOF | oc apply -f - > /dev/null
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: rwx-probe
  namespace: default
spec:
  accessModes: ["ReadWriteMany"]
  storageClassName: $SC
  resources:
    requests:
      storage: 1Gi
EOF
  if oc wait --for=jsonpath='{.status.phase}'=Bound pvc/rwx-probe -n default --timeout=90s > /dev/null 2>&1; then
    RWX_SC=$SC
  fi
  oc delete pvc rwx-probe -n default --ignore-not-found --wait=false > /dev/null 2>&1
  [ -n "$RWX_SC" ] && break
done
echo "RWX-capable storage class: ${RWX_SC:-none found}"
```

When a class binds, export it for the Tackle instance:

```bash
export STORAGECLASS_RWX=$RWX_SC
```

When **no class binds** (`none found`), detect the platform and install ODF (OpenShift Data Foundation), which provides the RWX class `ocs-storagecluster-cephfs`:

```bash
oc get infrastructure cluster -o jsonpath='{.status.platformStatus.type}{"\n"}'
```

On **AWS** the shipped manifests work as-is (3 OSDs × 512Gi on `gp3-csi`, portable across workers): label the workers so the ODF scheduler can place Ceph, install the pinned ODF operator, create the StorageCluster, wait for the storage class (~15-20 minutes). The first `oc apply` of the StorageCluster can fail with `error: unable to recognize ... ensure CRDs are installed first` — the `odf-operator` CSV only installs the OCS CRDs (incl. `storageclusters.ocs.openshift.io`) after reporting `Succeeded`, and the apply can race that. Verify the CRD exists and re-apply if needed:

```bash
oc label nodes -l node-role.kubernetes.io/worker cluster.ocs.openshift.io/openshift-storage="" --overwrite
oc apply -f storage/odf-subscription.yaml
oc wait --for=create csv odf-operator.v4.22.7-rhodf -n openshift-storage --timeout=900s
oc wait --for=jsonpath='{.status.phase}'=Succeeded csv odf-operator.v4.22.7-rhodf -n openshift-storage --timeout=900s
until oc get crd storageclusters.ocs.openshift.io > /dev/null 2>&1; do sleep 10; done
oc apply -f storage/storage-cluster.yaml
oc wait --for=create sc ocs-storagecluster-cephfs --timeout=1800s
export STORAGECLASS_RWX=ocs-storagecluster-cephfs
```

Other platforms (Azure, GCP, vSphere, bare metal): same flow, but the `storage/storage-cluster.yaml` StorageCluster must reference the platform's block storage class for the OSD data PVCs (`spec.storageDeviceSets[].dataPVCTemplate.spec.storageClassName` — e.g. `managed-csi` or `premium_LRS` on Azure, `pd-balanced` on GCP); worker labeling is identical. See the [ODF documentation](https://docs.redhat.com/en/documentation/openshift_data_foundation) for sizing, then export `STORAGECLASS_RWX=ocs-storagecluster-cephfs` as above.

##### Configure the Tackle Instance

create a Tackle instance with the storage sizing, Developer Lightspeed enabled, and the LLM configured:

```bash
cat mta-ui/tackle.yaml | envsubst | oc apply -f -
```
Wait for the installation to finish:
```bash
oc wait --for=create deployment mta-ui -n openshift-mta --timeout=600s
oc wait --for=create deployment mta-hub -n openshift-mta --timeout=600s
oc wait --for=create deployment kai-db -n openshift-mta --timeout=600s
oc wait --for=create deployment kai-api -n openshift-mta --timeout=600s
oc wait --for=create deployment agentic-controller -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-ui -n openshift-mta --timeout=600s
oc wait --for=create deployment mta-hub -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-hub -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment kai-db -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment kai-api -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment agentic-controller -n openshift-mta --timeout=600s
```
All commands should return `condition met`.
##### Workaround for a stuck cache PVC

sometimes the cache PVC has an issue (`/cache/m2 can't be created`). Fix it like this:

```bash
oc apply -f mta-ui/cache-pvc-inspector.yaml
oc wait --for=create deployment cache-pvc-inspector -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment cache-pvc-inspector -n openshift-mta --timeout=600s
echo "mkdir /cache/m2 && chmod 777 /cache/m2" | oc exec deployment/cache-pvc-inspector -n openshift-mta -i -t -- bash -il
```
Verify the directory exists and is writable — task pods run as random UIDs, so the directory **must** be world-writable, not just owned by the creating pod's UID:
```
oc exec deployment/cache-pvc-inspector -n openshift-mta -- \
  bash -c "if [ -d /cache/m2 ] && [ -w /cache/m2 ]; then echo '/cache/m2 exists and is writable'; else echo '/cache/m2 missing or not writable'; fi"
```
Expected output: `/cache/m2 exists`.

Then delete the PVC inspector deployment:
```bash
oc delete -f mta-ui/cache-pvc-inspector.yaml
```
##### Automate cleanup of dead task pods

the hub's task scheduler enforces a pod limit (10 here) counted over **existing** task pods, including `Error` ones — dead pods block the queue: the scheduler logs `quota=10/10 requested=N scheduled=0` and no task, even explicitly triggered analyses, is ever scheduled again (the hub reaps pods of succeeded tasks but never failed ones). The hourly `CronJob` in `mta-ui/task-pod-cleanup-cronjob.yaml` deletes terminal-state task pods, so the queue can never wedge permanently:

```bash
oc apply -f mta-ui/task-pod-cleanup-cronjob.yaml
```

It creates a `ServiceAccount`, a `Role` (pods get/list/delete in `openshift-mta`), a `RoleBinding`, and the `CronJob` — the job container uses the cluster's own CLI imagestream (`openshift/cli:latest` from the internal registry, matching the running OpenShift version, so no external pulls). Trigger a run manually to verify:

```bash
oc create job --from=cronjob/task-pod-cleanup task-pod-cleanup-manual -n openshift-mta
oc logs job/task-pod-cleanup-manual -n openshift-mta
oc delete job task-pod-cleanup-manual -n openshift-mta
```

Full failure mode and manual fix: [the troubleshooting section](troubleshooting/README.md#mta-analyses-and-discovery-tasks-fail-or-never-start).
<a id="create-a-konveyor-gateway"></a>
##### Create a Konveyor Gateway

abstracts access to the LLM.
```bash
cat mta-ui/llm-gateway.yaml | envsubst | oc apply -f -
```
Verify the gateway instantiation:
```bash
oc wait --for create gateways.konveyor.io openai -n openshift-mta --timeout=600s
oc wait --for=jsonpath='{.status.connectionVerified}'="true" gateways.konveyor.io openai -n openshift-mta --timeout=600s
```
Both commands should return `condition met`.

To change the model, edit the current `Konveyor Gateway` or create another one. When you later run an `Agentic Migration`, you're prompted for the gateway and the target branch, prefilled with the UI default `konveyor/migration-<timestamp>` (hardcoded as `TARGET_BRANCH_PREFIX` in the MTA UI) but editable per run — any valid git refname except the application's source branch is accepted (the harness refuses to push onto it). Results, including committed handoff files, go to this branch.
##### Trusting a private root CA on the LLM endpoint (optional)

only needed if the endpoint presents a certificate from a private/corporate CA. The `maas.apps...` endpoint this PoC uses serves a public Let's Encrypt chain (leaf → `YR1` → `ISRG Root YR`, cross-signed by `ISRG Root X1`, in every standard trust store), so none of this is required for it — but the procedure was validated end-to-end against that endpoint and is ready if it ever moves behind a private CA. To see the chain the endpoint serves (the last certificate is the root to install):

```bash
echo | openssl s_client -connect ${OPENAI_HOST}:443 -servername ${OPENAI_HOST} -showcerts 2>/dev/null \
  | awk '/BEGIN CERT/,/END CERT/{print}' > /tmp/llm-chain.pem
```

The `Gateway` CRD has no CA bundle field (`spec` is only `provider`, `endpoint`, `credentialRef`, `model`), and the Gateway doesn't need one: its probe runs `curl -sk` (verification disabled), so it reports `Ready` with `connectionVerified=true` regardless of the chain. The client that enforces TLS is the agent runtime (goose) inside the Agent Sandbox pod — the CA must reach that container. Two mechanisms, in order of preference:

- **Bake the CA into the agent image (recommended — also covers UI-launched runs).** Derive from the agent base image and extract the trust store at *build* time — the sandbox runs with a random UID under the restricted SCC and cannot run `update-ca-trust` at pod start:

  ```dockerfile
  FROM registry.redhat.io/mta/mta-agent-java-rhel9:8.3.1
  COPY corp-root-ca.crt /etc/pki/ca-trust/source/anchors/
  RUN update-ca-trust extract
  ```

  Build with the same `oc new-build --binary` pattern as the custom agent images (see the workflow sections below) and point the workflow's `Agent` manifests' `spec.image` at it. Each workflow owns its agent images, so this is per-workflow with no run-time changes — verified live: an image built this way reaches the endpoint with no environment variables set (`ssl_verify=0`).
- **Mount the CA per run.** `AgentRun` and `AgentWorkflowRun` support `spec.env`, `spec.envFrom` and `spec.fileMounts`, forwarded to every stage run the workflow run creates (the `AgentWorkflow`/`Agent` definition CRs deliberately carry none — execution-time concerns). Create a ConfigMap with the root CA and reference it on the run:

  ```bash
  oc create configmap corp-root-ca --from-file=corp-root-ca.crt=/path/to/root-ca.pem -n openshift-mta
  ```

  ```yaml
  # on the AgentWorkflowRun / AgentRun spec, alongside gateway/params/agentRef
  env:
    - name: SSL_CERT_FILE
      value: /etc/certs/corp-root-ca.crt
  fileMounts:
    - configMapName: corp-root-ca
      mountPath: /etc/certs/corp-root-ca.crt
      subPath: corp-root-ca.crt
  ```

  The mount path must not collide with the controller-managed mounts (`/opt/skills`, `/opt/skills-src`, `/run/konveyor`, `/workspace`, `/tmp`) — a collision fails the run terminally with `InvalidFileMounts`; a missing ConfigMap retries until it appears. Applies only to YAML/CLI-launched runs — the MTA UI's run dialog creates the run resource itself.

Which variable switches a client to the mounted file is client-specific — a classic verification trap: `curl` ignores `SSL_CERT_FILE` (its variable is `CURL_CA_BUNDLE`; missing file → exit 77, verification failure → exit 60), while `SSL_CERT_FILE` is honored by OpenSSL-default-path clients, Go, and goose's TLS stack. Go's `crypto/x509` also merges `SSL_CERT_FILE` with the default certificate directories (upstream golang/go#79496), so a Go diagnostic that "passes despite a bogus `SSL_CERT_FILE`" proves nothing — set `SSL_CERT_DIR=/nonexistent` alongside to actually restrict the pool (verified: unrelated CA → `x509: certificate signed by unknown authority`; endpoint's root → succeeds).
##### Accessing the MTA UI


```bash
echo "MTA UI: https://$(oc get route mta -n openshift-mta -o jsonpath='{.spec.host}')"
``` 
You should see a screen like this:

![image](images/mta_login.png)    

Login with `admin/admin`.
##### Add an MTA entry to the OpenShift console

a `ConsoleLink` puts an MTA entry into the web console's application menu, so users don't need the route URL. The manifest is cluster-independent — the `href` carries the same `${OPENSHIFT_APPS_DOMAIN}` placeholder as the other manifests (the domain already includes `apps.`, so the URL is `https://mta-openshift-mta.${OPENSHIFT_APPS_DOMAIN}`); apply with the usual envsubst pattern (plain `oc apply -f -` would store the placeholder literally):

```bash
cat console/console-link.yaml | envsubst | oc apply -f -
```

The entry appears under **Red Hat Applications** in the application launcher, effective immediately (a console reload may be needed). The manifest embeds the MTA logo as a data URI in `spec.applicationMenu.imageURL` — the MTA UI's own favicon, fetched from the live deployment, so the launcher needs no external URL. Verify the icon round-trips with

```bash
oc get consolelink mta-ui -o jsonpath='{.spec.applicationMenu.imageURL}' | \
  sed 's|^data:image/x-icon;base64,||' | base64 -d | file -
```

Two field-name pitfalls, both verified live: the CRD field is `imageURL` (capital `URL`) — an `imageUrl` typo is **silently pruned** by CRD validation while `oc apply` reports success, like the CheCluster v2 pruning ([Devfile registry](#devfile-registry)); and the `.ico` data URI mediatype is `image/x-icon` (a `png`/`svg` label renders nothing). The `ConsoleLink` is cluster-scoped and shown to all console users; `oc delete consolelink mta-ui` removes the entry. Verify with

```bash
oc get consolelink mta-ui -o jsonpath='{.spec.href}'
```
##### Add a Gitea entry to the OpenShift console

a second `ConsoleLink` adds Gitea to the same menu section, pointing at the Gitea web UI route (`https://git.${OPENSHIFT_APPS_DOMAIN}`):

```bash
cat console/gitea-console-link.yaml | envsubst | oc apply -f -
```

Same pattern — the icon is Gitea's own favicon (`assets/img/favicon.png` served by the Gitea route) inlined as a PNG data URI. `envsubst` pitfall: it only substitutes **exported** shell variables — sourcing an env file that sets `OPENSHIFT_APPS_DOMAIN` without `export` leaves the placeholder empty (href becomes `https://git.`), so `export` first or pipe through `envsubst '$OPENSHIFT_APPS_DOMAIN'` with it exported. Verify with

```bash
oc get consolelink gitea -o jsonpath='{.spec.href}'
```

#### Proxy settings for disconnected environments (optional)

The `Tackle` and `Konveyor Gateway` CRs provide no proxy settings. The MTA Operator does propagate the standard `HTTP_PROXY`/`HTTPS_PROXY`/`NO_PROXY` from its own pod environment to the `mta-hub` container on reconciliation. In a disconnected environment:

- **LLM access**: none needed if the LLM is hosted internally — point the LLM provider Secret (`mta-ls-llm-provider-secret`) and the `Konveyor Gateway` endpoint at the internal LLM.
- **Other outbound traffic**: define the `MTA_`-prefixed proxy variables (see [Proxy configuration](#proxy-configuration)) and set them on the MTA operator **via the Subscription's `spec.config.env`** — OLM reverts any `oc set env` drift on the deployment it manages, so the Subscription is the durable place. The operator renders the variables into `mta-hub` on its next reconciliation (delete the operator pod to trigger one — OLM also reverts `oc rollout restart`):

    ```bash
    oc patch subscription mta -n openshift-mta --type=merge -p "$(jq -n \
      --arg hp "${MTA_HTTP_PROXY}" --arg hps "${MTA_HTTPS_PROXY}" --arg np "${MTA_NO_PROXY}" \
      '{spec: {config: {env: [
        {name: "HTTP_PROXY", value: $hp},
        {name: "HTTPS_PROXY", value: $hps},
        {name: "NO_PROXY", value: $np}]}}}')"
    oc delete pod -n openshift-mta $(oc get pods -n openshift-mta -o name | grep '^pod/mta-operator' | cut -d/ -f2)
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-operator -n openshift-mta --timeout=600s
    ```

    Verify propagation (the operator pod carries them; the hub deployment inherits them):

    ```bash
    oc get deployment mta-hub -n openshift-mta -o jsonpath='{.spec.template.spec.containers[0].env}' | jq '.[] | select(.name | test("PROXY"; "i"))'
    ```

    > ⚠️ NOTE
    >
    > Only the `mta-hub` container receives the proxy variables — the `kai-api` and `agentic-controller` deployments render no proxy settings from the `Tackle` CR or the operator environment. The `NO_PROXY` list must include the cluster-internal hosts (e.g. the wildcard domain of internal routes such as Gitea and Keycloak) **and the pod/service networks (`10.128.0.0/14`, `172.30.0.0/16`)** — without the service network, the operator's leader-election traffic to the Kubernetes API is routed into the proxy and breaks (the operator pod restarts with `leader election lost`).

#### Setting the Hub proxy via the Tackle Hub API

Independent of the `mta-hub`-container propagation above, the Hub also stores proxy settings **centrally**:

- Same data as the UI's *Settings → Proxy* table, exposed via the [Tackle2-Hub API](https://github.com/konveyor/tackle2-hub/blob/main/docs/swagger.yaml) at `/hub/proxies` — an internal, unsupported surface (see the warning below).
- Two defaults are seeded: `http` (id `1`) and `https` (id `2`), both `enabled: false` with empty host/port.

Inspect:

```bash
curl -k -s "${MTA_URL}/hub/proxies" -u admin:admin | jq '.[] | {id, kind, enabled, host, port, excluded}'
```

These configure the Hub's **own git/svn operations**: when it fetches or pushes a repository (e.g. migrating the original GitHub repositories), it derives the `http.proxy` of the generated `~/.gitconfig` from the `enabled: true` entry matching the clone URL's scheme, and resolves the entry's `identity` reference to add `user:password@` proxy authentication. `ssh`-scheme clone URLs bypass the proxy. The entry fields:

| Field      | Description                                                                                                        |
|------------|--------------------------------------------------------------------------------------------------------------------|
| `enabled`  | Only `true` entries are used by the Hub.                                                                           |
| `kind`     | Scheme the proxy serves: `http` or `https`.                                                                         |
| `host`     | Proxy host (built without a scheme — the Hub always produces `http://<host>:<port>`).                              |
| `port`     | Proxy port; `0` omits it.                                                                                           |
| `excluded` | Hosts that bypass the proxy. **Exact string match** against the clone URL's host — no wildcards or suffix matching. |
| `identity` | Reference to an Identity with `user`/`password` for proxy authentication, or `null`.                                |

> ⚠️ NOTE
>
> `excluded` semantics differ from `NO_PROXY`: the Hub compares each entry **exactly** against the clone URL's host, so suffix-style entries such as `.svc` or `.apps.<domain>` have no effect on Hub git operations. Add the exact internal route hosts instead — e.g. `git.${OPENSHIFT_APPS_DOMAIN}` (the Gitea HTTPS route) — so the Hub reaches them directly rather than through the proxy, which would otherwise only allowlist external destinations.

Set both entries from the `MTA_`-prefixed proxy variables, deriving `host` and `port` from `MTA_HTTP_PROXY` the same way as the [Maven settings](#configure-maven-settings) and appending the exact internal hosts to the `excluded` list:

```bash
PROXY_HOST=$(echo "${MTA_HTTP_PROXY#*://}" | cut -d: -f1)
PROXY_PORT=$(echo "${MTA_HTTP_PROXY#*://}" | cut -s -d: -f2)
PROXY_PORT=${PROXY_PORT:-80}
PROXY_EXCLUDED=${MTA_NO_PROXY},git.${OPENSHIFT_APPS_DOMAIN},git-ssh.${OPENSHIFT_APPS_DOMAIN}

for kind in http https; do
  id=$([ $kind = http ] && echo 1 || echo 2)
  curl -k -s -X PUT "${MTA_URL}/hub/proxies/${id}" \
    -u admin:admin \
    -H "Content-Type: application/json" \
    -d "$(jq -n \
      --arg kind "$kind" \
      --arg host "$PROXY_HOST" \
      --argjson port "$PROXY_PORT" \
      --argjson excluded "$(printf '%s' "$PROXY_EXCLUDED" | jq -R 'split(",")')" \
      '{enabled: true, kind: $kind, host: $host, port: $port, excluded: $excluded, identity: null}')"
done
```

Each `PUT` returns `204 No Content` on success; verify with the `GET` above. To disable again, `PUT` with `"enabled": false`. Like the identity resources, the `PUT` is a full-object update — fetch first and modify rather than write blind if an `identity` reference is set.

> ⚠️ NOTE
>
> This configures only the Hub's own git/svn operations. The analyzer task pods (`kantra`) and the agentic Sandboxes do not consume these settings — their outbound traffic still needs the environment-variable propagation above (and the Maven proxy identity for builds, see [Configure Maven settings](#configure-maven-settings)).

#### Automation via REST API

Use the Konveyor [Tackle2-Hub API](https://github.com/konveyor/tackle2-hub/blob/main/docs/swagger.yaml) to automate MTA configuration.

> ⚠️ WARNING
>
> The Tackle2-Hub API is an internal implementation detail, not a supported product feature.

First, export the endpoint:

```bash
export MTA_URL="https://$(oc get route mta -n openshift-mta -o jsonpath='{.spec.host}')"
```

##### Creating Stakeholders, Stakeholder Groups and Business Service

Create a stakeholder, add him to the **JBoss EAP Quickstarts** stakeholder group, and create a **JBoss Onboarding** business service:

```bash
MTA_USER_ID=$(curl -k -s -X POST "${MTA_URL}/hub/stakeholders"   -u admin:admin   -H "Content-Type: application/json"   -d '{
"name": "John Doe",
"email": "john@example.com",
"jobFunction": {"id": 1, "name": "IT"}
}' | jq '.id')

curl -k -s -X POST "${MTA_URL}/hub/stakeholdergroups" \
-u admin:admin \
-H "Content-Type: application/json" \
-d "{
    \"name\": \"JBoss EAP Quickstarts\",
    \"description\": \"Stakeholders owning migrated JBoss EAP Quickstarts applications\",
    \"stakeholders\": [
    {\"id\": ${MTA_USER_ID}, \"name\": \"John Doe\"}
    ]
}"

curl -k -s -X POST "${MTA_URL}/hub/businessservices" \
-u admin:admin \
-H "Content-Type: application/json" \
-d "{
    \"name\": \"JBoss Onboarding\",
    \"description\": \"Onboarding Developers onto JBoss\",
    \"owner\": {\"id\": ${MTA_USER_ID}, \"name\": \"John Doe\"}
}"
```

##### Configure Maven settings

Need a custom Maven `settings.xml`, e.g. for [JFrog Artifactory](https://jfrog.com/artifactory/) or [Sonatype Nexus](https://www.sonatype.com/products/sonatype-nexus-repository)? This example:

- Configures the `Red Hat GA` repository for dependencies and plugins.
- In a disconnected environment, uses the `<proxies>` block in `mta-ui/maven-settings.xml`, populated from the `MTA_`-prefixed proxy environment variables (see [Proxy configuration](#proxy-configuration)).
- Derives protocol, host, port, and non-proxy hosts separately from `MTA_HTTP_PROXY` and `MTA_NO_PROXY` first (Maven needs them individually):

```bash
export MAVEN_PROXY_PROTOCOL=${MTA_HTTP_PROXY%%://*}
export MAVEN_PROXY_HOST=$(echo "${MTA_HTTP_PROXY#*://}" | cut -d: -f1)
export MAVEN_PROXY_PORT=$(echo "${MTA_HTTP_PROXY#*://}" | cut -s -d: -f2)
export MAVEN_PROXY_PORT=${MAVEN_PROXY_PORT:-80}
export MAVEN_PROXY_NONPROXYHOSTS=${MTA_NO_PROXY//,/\|}
export MAVEN_SETTINGS=$(envsubst < mta-ui/maven-settings.xml | sed 's/\"/\\\"/g' | tr '\n' ' ')
curl -k -s -X POST "${MTA_URL}/hub/identities" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"maven-redhat-ga-settings\",
    \"kind\": \"maven\",
    \"description\": \"Maven settings for Red Hat GA repository\",
    \"default\": true,
    \"settings\": \"${MAVEN_SETTINGS}\"
  }"

export MAVEN_IDENTITY_ID=$(curl -k -s "${MTA_URL}/hub/identities" -u admin:admin \
  | jq -r '.[] | select(.name=="maven-redhat-ga-settings") | .id')
```

Notes on the Maven identity:

- `nonProxyHosts`: `MTA_NO_PROXY` with commas replaced by the `|` separator Maven expects, including the cluster-internal hosts so internal traffic bypasses the proxy.
- No proxy needed? Remove the `<proxies>` block from `mta-ui/maven-settings.xml` before creating the identity.
- As with the Git credentials, the POST returns `409 Conflict` if the identity exists — the ID is looked up by name, not captured from the response.

Only analyses consume a `maven`-kind identity, resolved like the Git credentials:

- First: an identity **linked to the application with the role `maven`** (the direct search filters on the role exactly).
- Fallback: the global **default** (`kind: maven` and `default: true`).
- As the default, it resolves for every application without linking.
- Per-app override (e.g. per-app repositories): link a different `maven`-kind identity with role `maven` via the same full-object `PUT` pattern as the Git credentials — the direct link wins:

```bash
curl -k -s -X PUT "${MTA_URL}/hub/applications/${APP_ID}" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d "$(curl -k -s "${MTA_URL}/hub/applications/${APP_ID}" -u admin:admin \
    | jq --argjson id "${MAVEN_IDENTITY_ID}" \
        '.identities = (((.identities // []) | map(select(.id != $id))) + [{id: $id, role: "maven"}])')"
```

An application can carry both links side by side:

- `git-credentials` with role `source` (clone/push); the Maven identity with role `maven` (builds).
- On resolution, the injector writes the identity's `settings` field to `/shared/creds/maven/settings.xml` in the analysis task.
- It passes the path to the Java provider as `mavenSettingsFile`.
- **Agentic Workflow runs** are different: the deployed MTA Hub's REST binding rejects `spec.fileMounts` (a newer Hub accepts it).
- They carry custom Maven settings via the `MAVEN_SETTINGS_XML` environment variable instead — see [Start an Agent Workflow Run](#start-an-agent-workflow-run).

##### Bulk Uploading Applications

```bash
cat mta-ui/template_application_import.csv | envsubst > mta-ui/application_import.csv
curl -k -s -X POST "${MTA_URL}/hub/importsummaries/upload" \
  -u admin:admin \
  -F "file=@mta-ui/application_import.csv"
```

Success returns `201 Created` with an `ImportSummary` JSON object.

The template uses **HTTPS repository URLs** (`https://git.${OPENSHIFT_APPS_DOMAIN}/...`), so the analyzer clones over HTTPS with basic auth. The `git-credentials` identity from [Create default Git credentials](#create-default-git-credentials) is the global default, so it resolves for every imported application. `envsubst` is also where a stale `OPENSHIFT_APPS_DOMAIN` shell variable would bake a wrong hostname into the application records.

> ⚠️ WARNING
>
> Repository URLs must use the **HTTPS scheme**: the agentic harness clones/pushes in-process with the Go git library, whose only auth method is HTTP basic auth. An `ssh://git@git-ssh.${OPENSHIFT_APPS_DOMAIN}:443/...` URL (the SSH route from the [SSH clone test](#ssh-keys)) fails the agentic run at the clone step — the harness cannot traverse the TLS-passthrough SSH route. SSH URLs serve the workstation-side SSH flow only. Likewise, a `source`-kind SSH-key identity (e.g. `git-ssh-credentials`) linked to an application or set as default does not affect the harness; it authenticates the analyzer's clone, which over HTTPS uses the basic-auth identity instead.

##### Importing dependencies for an existing portfolio

The bulk upload imports **applications** (record type `1`) — no dependency data:

- For an already-onboarded portfolio, add or correct dependencies with **dependency-only records** (record type `2`), uploaded through the same `importsummaries/upload` endpoint.
- The importer matches applications by name and only creates `Dependency` edges.
- `mta-ui/application_dependency_import.csv` holds the complete intra-portfolio dependency graph derived from the quickstarts' POMs (see [the per-row dependency table](mta-ui/template_application_import.dependencies.md)).
- It is maintained alongside the application template as the single source for dependency data:

```bash
curl -k -s -X POST "${MTA_URL}/hub/importsummaries/upload" \
  -u admin:admin \
  -F "fileName=application_dependency_import.csv" \
  -F "file=@mta-ui/application_dependency_import.csv"
```

Record layout (Hub import format):

- Column 1: record type `2`; column 2: application name; columns 6 and 7: dependency and direction.
- Row pattern: `2,<app>,,,,<dep>,<direction>` with **exactly four commas** between application name and dependency.
- `southbound`: the application depends on the named application (Hub edge application → dependency).
- `northbound`: the named application depends on this one (edge dependency → application).

> ⚠️ WARNING
>
> Two pitfalls with dependency-only imports:
>
> - **Column layout**: one comma too many puts the dependency in the direction column and every record fails with `Application dependency '' could not be found.` Just delete the failed `ImportSummary` (`DELETE ${MTA_URL}/hub/importsummaries/:id`) and re-upload.
> - **No deduplication**: the importer matches application names but never checks whether the edge exists — every record creates a Hub `Dependency`. An edge expressed twice (e.g. southbound from the consumer and northbound from the consumed) or re-imported creates duplicates. The file expresses each of the 42 POM-verified edges exactly once. If duplicates appear, remove the extras via the Hub REST API, keeping one per `(from, to)` pair:
>
>   ```bash
>   # list edges with ids
>   curl -k -s "${MTA_URL}/hub/dependencies" -u admin:admin | \
>     jq -r '.[] | "\(.id)|\(.from.name) -> \(.to.name)"'
>   curl -k -s -X DELETE "${MTA_URL}/hub/dependencies/:id" -u admin:admin
>   ```
>
> Also note the importer resolves application names with a `LIKE` match — a name that is a prefix of another application's name can resolve to the wrong row; the quickstarts' exact names avoid this.

##### Create default Git credentials

**Agentic Workflows** store information in Gitea, which is protected by basic auth — so set up a default credential:

> The POST returns `409 Conflict` with an error object if the identity already exists — capturing `.id` from that response would yield nothing, so the ID is looked up by name instead:

```bash
curl -k -s -X POST "${MTA_URL}/hub/identities" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "name": "git-credentials",
    "description": "Default Git credentials for cloning quickstarts",
    "kind": "source",
    "user": "jboss-dev@example.com",
    "password": "jboss-dev",
    "default": true
  }'

export GIT_CREDENTIALS_ID=$(curl -k -s "${MTA_URL}/hub/identities" -u admin:admin \
  | jq -r '.[] | select(.name=="git-credentials") | .id')
```

Using SSH keys against the Git repository? Create an identity with the private SSH key in the `key` field. `password` optionally holds the key's passphrase; `user` is the Git server's SSH user — `git`, which Gitea's built-in SSH server requires (pinned via `BUILTIN_SSH_SERVER_USER` in the [Gitea installation](#ssh-keys) values):

```bash
curl -k -s -X POST "${MTA_URL}/hub/identities" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg key "$(cat ~/.ssh/mta-gitea)" '{
    "name": "git-ssh-credentials",
    "description": "SSH key credentials for the Git repository",
    "kind": "source",
    "user": "git",
    "key": $key,
    "default": false
  }')"

export GIT_SSH_IDENTITY_ID=$(curl -k -s "${MTA_URL}/hub/identities" -u admin:admin \
  | jq -r '.[] | select(.name=="git-ssh-credentials") | .id')
```

How Agentic Workflows resolve their push credentials:

- The harness doesn't receive them in the run — it looks them up from the Hub at runtime.
- First: an identity **linked to the application** with role `source`; fallback: the global **default** `source` identity (`git-credentials` above).
- `git-credentials` is the default, so no linking is needed.
- A `source` identity that is neither linked nor default resolves to nothing — the run pushes anonymously.
- Only link an identity to an application to override the default with per-application credentials (see [Start an Agent Workflow Run](#start-an-agent-workflow-run)).
- `git-ssh-credentials` serves the analyzer's and workstation's SSH flow only.
- The harness clones over HTTPS basic auth and does not use SSH-key identities (see the warning in [Bulk Uploading Applications](#bulk-uploading-applications)).

Verify the identity exists:

```bash
curl -k -s -o /dev/null -w "%{http_code}\n" \
  "${MTA_URL}/hub/identities/${GIT_SSH_IDENTITY_ID}" \
  -u admin:admin
```

This should return `200`. Identities live in the MTA Hub database, not as Kubernetes resources — unlike the `SkillCard`, `Agent`, and `AgentWorkflow` custom resources there is no `oc get` check. List all source-control identities with:

```bash
curl -k -s "${MTA_URL}/hub/identities" -u admin:admin | jq '[.[] | select(.kind=="source") | {name, user}]'
```

##### Issue Tracking (Jira integration, optional)

MTA synchronizes migration tickets with Jira Cloud through the Hub's `jira-cloud` tracker kind:

- The connector talks to the Jira Cloud REST API (`rest/api/2` — `myself` for the connection test, `project`, `issue`, `search` for the ticket lifecycle).
- Auth is Basic: the **Atlassian account email** and an **API token** (create the token at [id.atlassian.com](https://id.atlassian.com) → Security → API tokens).
- The Hub routes egress through the Squid proxy, so the proxy allowlist must cover the Jira host — `.atlassian.net` is already in the list (see [Squid proxy deployment](#squid-proxy-deployment-optional-disconnected-environments)).

The `JIRA_HOST`, `JIRA_EMAIL`, and `JIRA_API_TOKEN` environment variables from the [prerequisites](#prerequisites) are used throughout.

The tracker URL points at a small in-cluster rewriter (`jira/jira-cloud-search-rewriter.yaml`), not the Jira site directly:

- Reason: Jira Cloud **removed the `/rest/api/2/search` endpoint** (Atlassian changelog [CHANGE-2046](https://developer.atlassian.com/changelog/#CHANGE-2046)), but the Hub's embedded go-jira client still uses it for the ticket status refresh.
- The rewriter maps the removed search path to the `/rest/api/3/search/jql` replacement.
- It also injects the required `fields=status` parameter — the new endpoint returns no fields by default.
- Ticket creation, the connection test, and project browsing pass through unchanged.
- Register via the same `${MTA_URL}/hub` endpoint exported in [Automation via REST API](#automation-via-rest-api); `admin:admin` is the Hub basic-auth account set up during the MTA deployment.
- Deploy the rewriter first (note the `envsubst` whitelist — nginx's own `$args` variable in the config must survive substitution):

```bash
cat jira/jira-cloud-search-rewriter.yaml | envsubst '$JIRA_HOST' | oc apply -f -
oc rollout status deploy/jira-rewriter -n openshift-mta --timeout=300s
```

Then create the identity and register the tracker:

```bash
# Identity carrying the Jira credentials (the Hub encrypts them at rest)
IDENTITY_ID=$(curl -k -s -u admin:admin -H "Content-Type: application/json" \
  -d "{\"name\":\"jira-cloud\",\"kind\":\"basic-auth\",\"user\":\"${JIRA_EMAIL}\",\"password\":\"${JIRA_API_TOKEN}\"}" \
  "${MTA_URL}/hub/identities" | jq -r '.id')

# Tracker pointing at the rewriter in front of the Jira Cloud site
curl -k -s -u admin:admin -H "Content-Type: application/json" \
  -d "{\"name\":\"jira-cloud\",\"kind\":\"jira-cloud\",\"url\":\"http://jira-rewriter.openshift-mta.svc:8080\",\"identity\":{\"id\":${IDENTITY_ID},\"name\":\"jira-cloud\"},\"insecure\":false}" \
  "${MTA_URL}/hub/trackers" | jq
```

> ⚠️ NOTE
>
> The identity kind must be exactly **`basic-auth`** (hyphenated) — other spellings fail the connection test with *"unsupported identity kind"*, and omitting the tracker's `name` field fails the POST with a `Name ... required` validation error. The Hub runs a background manager that tests disconnected trackers every 10 seconds and connected ones every 60, so a tracker flips to `connected: true` on its own once credentials and URL are correct — `GET /trackers/{id}/projects` only reports the last test's stored result, it does not test on demand. A `401` in the tracker's `message` field means the identity's `user` (the Atlassian email) is empty or wrong — e.g. when the environment variable was unset at identity-creation time — even if the API token is valid; see [troubleshooting/README.md](troubleshooting/README.md) for the Jira symptom→cause table.

Verify the integration: the tracker's `connected` field (checked by the background manager against `/rest/api/2/myself` through the rewriter), the reachable Jira projects, and the issue types of the project tickets should be filed in (non-subtask, non-Epic types are eligible):

```bash
TRK_ID=$(curl -k -s -u admin:admin "${MTA_URL}/hub/trackers" | jq -r '.[0].id')
curl -k -s -u admin:admin "${MTA_URL}/hub/trackers/${TRK_ID}/projects" | jq
# pick a project ID from the list, then:
curl -k -s -u admin:admin "${MTA_URL}/hub/trackers/${TRK_ID}/projects/<project-id>" | jq '.issueTypes'
```

Tickets are created from the MTA UI (e.g. from an application's issues or a migration wave): each becomes a Jira issue with the summary `Migrate <application name>` in the chosen project and issue type, and the Hub periodically refreshes the ticket status from Jira (To Do / In Progress / Done, from the issue's status category).

> Verified live: ticket `KAN-4` (`Migrate helloworld`, Task in the `MTA` project) was created by the Hub against a live Jira Cloud site, transitioned to Done in Jira, and the Hub picked up the new status within its 30-second refresh cycle.

##### Define an Analysis Profile

An analysis profile defines how an analysis runs: rulesets (selected by labels), package scope, and dependency analysis:

- Manage profiles in the `Analysis Profiles` view of the MTA UI, or via the REST API below.
- This example creates the profile used in this PoC (hub profile id 1, used by the validated helloworld analysis) for migrating JBoss EAP 7.4 to JBoss EAP 8.
- `konveyor.io/target=eap8`: rulesets targeting EAP 8.
- `konveyor.io/target=openjdk21`: the OpenJDK 21 baseline — EAP 8 runs on OpenJDK 21, so rules targeting Java versions up to 21 are in scope.
- `konveyor.io/source=eap7`: narrows to rules valid when coming from EAP 7.
- `withDeps` and `withKnownLibs`: dependencies and known (open-source) libraries are included.

```bash
curl -k -s -X POST "${MTA_URL}/hub/analysis/profiles" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JBoss EAP 7.4 to JBoss EAP 8",
    "description": "Analysis profile for migrating JBoss EAP 7.4 to JBoss EAP 8",
    "mode": { "withDeps": true },
    "scope": { "withKnownLibs": true },
    "rules": {
      "labels": {
        "included": [
          "konveyor.io/target=eap8",
          "konveyor.io/target=openjdk21",
          "konveyor.io/source=eap7"
        ]
      }
    }
  }'
```

An empty profile is valid; a useful one sets mode, scope, and rules:

- `source=eap7` narrows the rules to those valid when coming from EAP 7 — without it, rules aimed at other source platforms also run.
- The label selector's `+` suffix means "this version or any later one".
- E.g. `konveyor.io/target=openjdk17+` matches the OpenJDK 17 baseline and above; `konveyor.io/target=eap8+` matches any EAP 8.x target.
- `konveyor.io/target=jakarta-ee9+` adds the Jakarta EE namespace migration rules — the EAP 8 rules already carry the relevant Jakarta EE 10 targets.
- `scope.packages.included` (e.g. `org.jboss.eap.quickstarts.*`) narrows findings to first-party code, at the cost of skipping matching references inside dependencies.
- `scope.withKnownLibs=false` excludes known open-source libraries entirely.
- Optionally add `konveyor.io/target=cloud-readiness` for a containerization push — it flags hard-coded IPs, filesystem assumptions, and the like, at the cost of additional findings.

##### Create an Assessment Questionnaire

An **assessment** collects organizational and architectural answers about an application and computes an effort/confidence rating:

- Per application, or aggregated over a stakeholder group.
- Driven by a **questionnaire**: sections of questions, each answer carrying a risk level (`green`/`yellow`/`red`/`unknown`) with a rationale and mitigation.
- The Hub ships the builtin `Legacy Pathfinder` questionnaire; upload custom ones via the REST API.
- The example uses `mta-ui/eap7-to-8-questionnaire.yaml` for the JBoss EAP 7.4 → EAP 8 migration — 5 sections, 14 questions, 42 answers.
- It covers: the application profile; the current platform (EAP/Java versions, deployment style).
- APIs and dependencies: the `javax.*` → `jakarta.*` namespace migration, proprietary JBoss APIs, Hibernate 6, removed subsystems.
- Plus build and tooling (Maven/BOM/CI) and testing and delivery.
- The `thresholds` define how many answers of each risk level move the overall rating; the `riskMessages` summarize the result per risk level.

> ⚠️ NOTE
>
> The REST schema is manual-only: it does not accept the `includeFor`, `applyTags`, and `autoAnswerFor` fields that the MTA UI's YAML questionnaire upload supports (UI-uploaded questionnaires can auto-apply tags and pre-answer questions from application tags). A REST-created questionnaire must be answered manually in the UI.

The questionnaire file is YAML for readability; the API expects JSON. Convert with `yq` (or `python3` + PyYAML) and upload with `POST /hub/questionnaires`:

```bash
yq -o=json eval mta-ui/eap7-to-8-questionnaire.yaml > mta-ui/eap7-to-8-questionnaire.json

curl -k -s -X POST "${MTA_URL}/hub/questionnaires" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d @mta-ui/eap7-to-8-questionnaire.json
```

The POST returns `201 Created` with the questionnaire (e.g. `{"id": 2, "name": "JBoss EAP 7 to EAP 8 Assessment"}`):

- Re-running the POST creates a second copy, not a conflict — check for an existing copy first.
- If replacing, `DELETE /hub/questionnaires/{id}` the old one.
- Verify with `GET /hub/questionnaires`: each section, question, and answer is returned with its `order`, `risk`, `rationale`, and `mitigation`:

```bash
curl -k -s "${MTA_URL}/hub/questionnaires" -u admin:admin | jq -r '.[] | "\(.id): \(.name) — \(.sections | length) sections"'
```

Use it in the `Assessments` view of the MTA UI, or via the REST API's sub-resource routes:

- `POST /hub/applications/{id}/assessments` or `POST /hub/archetypes/{id}/assessments`.
- The Hub populates the assessment's sections, questions, and answers from the referenced questionnaire and copies its `thresholds` and `riskMessages`.
- Once the questions are answered, the Hub computes the application's risk rating from the `thresholds` and reports it with the matching `riskMessages` summary.
- The assessment complements the analysis profile from [Define an Analysis Profile](#define-an-analysis-profile): the analysis surfaces the technical findings (e.g. every `javax.*` import).
- The assessment captures the organizational context (team knowledge, testing coverage, release cadence) that determines how those findings translate into migration effort.

##### Create an Archetype

An **archetype** groups applications that share a *criteria tag*, letting you configure the group once: analyses and assessments apply to all members, which also inherit its stakeholders, *target profiles*, and *review* (the recorded migration decision).

The example below creates the EAP 7 → 8 archetype:

- Criteria tag `JBoss EAP` (id 427), plus the existing stakeholder and stakeholder group.
- Target profile referencing the analysis profile from [Define an Analysis Profile](#define-an-analysis-profile).
- The tag already exists: the CSV's `Tag 2` column (`Runtime` category) creates it and applies it to all 123 applications during the import.
- Verify the id with `curl -k -s "${MTA_URL}/hub/tags" -u admin:admin | jq '.[] | select(.name=="JBoss EAP") | .id'` and substitute it.
- Applications can also pick the tag up from analysis findings, which the archetype description anticipates.

```bash
curl -k -s -X POST "${MTA_URL}/hub/archetypes" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JBoss EAP 7 to EAP 8",
    "description": "Archetype for applications migrating from JBoss EAP 7.4 to JBoss EAP 8 (Jakarta EE 10, Java SE 17). Membership is determined by the JBoss EAP tag applied during analysis.",
    "comments": "Links the JBoss EAP 7.4 to JBoss EAP 8 analysis profile. Assessments created for this archetype apply to all applications carrying the JBoss EAP criteria tag.",
    "criteria": [{"id": 427, "name": "JBoss EAP"}],
    "stakeholders": [{"id": 1, "name": "John Doe"}],
    "stakeholderGroups": [{"id": 1, "name": "JBoss EAP Quickstarts"}],
    "profiles": [{"name": "JBoss EAP 8", "analysisProfile": {"id": 1, "name": "JBoss EAP 7.4 to JBoss EAP 8"}}]
  }'
```

The POST returns `201 Created`; the response's `applications` array already lists every matching application (all 123 here). Re-running the POST creates a second copy rather than a conflict — check for an existing copy first. Verify:

```bash
curl -k -s "${MTA_URL}/hub/archetypes" -u admin:admin | jq -r '.[] | "\(.id): \(.name) — criteria: \([.criteria[].name] | join(", ")) — \(.applications | length) applications"'
```

The criteria tags come from the analysis (the discovery ruleset applies `JBoss EAP` etc. from the application's dependencies), so an application only joins the archetype after it has been analyzed and tagged.

To assess the whole group in one shot, POST the questionnaire to the archetype's assessment sub-resource. The Hub populates all sections, questions, and answers from the questionnaire and copies its `thresholds` and `riskMessages`:

> Verified live: the top-level `POST /hub/assessments` is not routed on the deployed Hub — only the archetype sub-resource routes are.

```bash
curl -k -s -X POST "${MTA_URL}/hub/archetypes/1/assessments" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{"questionnaire": {"id": 2}}'
```

The POST returns `201 Created` with the assessment (`status: "empty"`, `risk: "unknown"` until answered). Stakeholders and stakeholder groups are **not** inherited automatically via the REST API — add them with a full-object `PUT /hub/assessments/{id}`, the same pattern as the identity links:

```bash
curl -k -s "${MTA_URL}/hub/assessments/1" -u admin:admin \
  | jq '.stakeholders = [{"id": 1}] | .stakeholderGroups = [{"id": 1}]' \
  | curl -k -s -X PUT "${MTA_URL}/hub/assessments/1" -u admin:admin -H "Content-Type: application/json" -d @- -o /dev/null -w "PUT HTTP %{http_code}\n"
```

Now answer the questions in the `Assessments` view of the MTA UI (or via `PUT /hub/assessments/{id}`). The Hub aggregates the risk over all 123 member applications, computing a group-wide rating from the questionnaire's `thresholds`.

Via the REST API, you answer by setting `selected: true` on the chosen answers and PUT-ing the full assessment. The following answers the 14 questions for the quickstart group — grounded in the actual repositories: the applications track the `7.4.x` branch, so EAP 7.4 with the default Java 8 compiler target.

```bash
curl -k -s "${MTA_URL}/hub/assessments/1" -u admin:admin -o /tmp/assessment.json

# select answers by (section order, question order, answer order)
python3 - <<'EOF'
import json
selection = {(1,1,1),(1,2,1),(2,1,1),(2,2,3),(2,3,2),(3,1,2),(3,2,2),(3,3,2),
             (3,4,2),(4,1,1),(4,2,1),(4,3,2),(5,1,2),(5,2,3)}
q = json.load(open('/tmp/assessment.json'))
for s in q['sections']:
    for qu in s['questions']:
        for a in qu['answers']:
            a['selected'] = (s['order'], qu['order'], a['order']) in selection
json.dump(q, open('/tmp/assessment-answered.json','w'))
EOF

curl -k -s -X PUT "${MTA_URL}/hub/assessments/1" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d @/tmp/assessment-answered.json -o /dev/null -w "PUT HTTP %{http_code}\n"
```

The archetype's `risk` field derives **only** from questionnaires marked `required: true`:

- `RequiredAssessments()` filters on the questionnaire's `required` flag; set it via a full-object `PUT /hub/questionnaires/{id}` (as in the upload above), or the answers won't count.
- The answers above: 5 green, 7 yellow, 2 red (Java SE 8 and ad-hoc releases); the 2 red answers meet `red: 2`:

```bash
curl -k -s "${MTA_URL}/hub/archetypes/1" -u admin:admin | jq -c '{assessed, risk, confidence}'
# {"assessed":true,"risk":"red","confidence":15}
```

##### Add a Review to the Archetype

A **review** records the stakeholder's formal migration decision for the group: proposed action, effort estimate, business criticality, and work priority. Member applications **inherit** it — the MTA UI shows the archetype's review on each application's detail view until the application has its own review.

Create it via the MTA UI's `Archetypes` view (select the archetype, open **Review**), or via the REST API. A `Review` links either an application or an archetype ref, and the Hub back-links it on the archetype's `review` field:

```bash
curl -k -s -X POST "${MTA_URL}/hub/reviews" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "archetype": {"id": 1, "name": "JBoss EAP 7 to EAP 8"},
    "proposedAction": "refactor",
    "effortEstimate": "medium",
    "businessCriticality": 3,
    "workPriority": 1,
    "comments": "Refactor the JBoss EAP 7 quickstarts to EAP 8. Medium effort due to Jakarta EE namespace updates."
  }'
```

The POST returns `201 Created` with the review (id 1 here). Verify that the archetype links it:

```bash
curl -k -s "${MTA_URL}/hub/archetypes/1" -u admin:admin | jq -c '{review, assessed, risk}'
# {"review":{"id":1},"assessed":true,"risk":"red"}
```

The review fields:

- `proposedAction`: the migration strategy — `rehost`, `replatform`, `refactor`, `repurchase`, `retire`, or `retain`.
- `effortEstimate`: qualitative work size — `small`, `medium`, `large`, or `extra_large`.
- `businessCriticality` and `workPriority`: numbers used by the adoption/reports views to prioritize the group.
- `comments`: free-form context.

To change the review later, use a full-object `PUT /hub/reviews/{id}` (same pattern as the archetype's stakeholder links). `DELETE /hub/reviews/{id}` removes it and clears the archetype's `review` field.

##### Create a Migration Wave

A **migration wave** groups applications that move together in a scheduled time window.

- Stakeholders are assigned for the group; the MTA UI's timeline/reports views use it to plan the migration sequence.
- Create it via the `Migration waves` view, or via `POST /hub/migrationwaves`.
- Dates must be in **RFC3339 format** — date-only strings are rejected with `parsing time ... cannot parse "" as "T"`:

```bash
curl -k -s -X POST "${MTA_URL}/hub/migrationwaves" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "name": "EAP 8 Wave 1",
    "startDate": "2026-11-02T00:00:00Z",
    "endDate": "2026-12-18T00:00:00Z",
    "stakeholders": [{"id": 1, "name": "John Doe"}],
    "stakeholderGroups": [{"id": 1, "name": "JBoss EAP Quickstarts"}]
  }'
```

The POST returns `201 Created` with the wave (id 1 here). Assign applications via a full-object `PUT /hub/migrationwaves/{id}` — the Hub sets each application's `migrationWave` back-link automatically. Application ids are DB-generated and differ per cluster (on a fresh import `helloworld` is id 1), so look the id up by name instead of hardcoding it:

```bash
APP_ID=$(curl -k -s "${MTA_URL}/hub/applications" -u admin:admin \
  | jq -r '.[] | select(.name=="helloworld") | .id')

curl -k -s -X PUT "${MTA_URL}/hub/migrationwaves/1" \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d "{
    \"id\": 1,
    \"name\": \"EAP 8 Wave 1\",
    \"startDate\": \"2026-11-02T00:00:00Z\",
    \"endDate\": \"2026-12-18T00:00:00Z\",
    \"stakeholders\": [{\"id\": 1, \"name\": \"John Doe\"}],
    \"stakeholderGroups\": [{\"id\": 1, \"name\": \"JBoss EAP Quickstarts\"}],
    \"applications\": [{\"id\": ${APP_ID}, \"name\": \"helloworld\"}]
  }"
```

Verify that the application is linked:

```bash
curl -k -s "${MTA_URL}/hub/applications/${APP_ID}" -u admin:admin | jq -c '{id, name, migrationWave}'
# {"id":1,"name":"helloworld","migrationWave":{"id":1,"name":"EAP 8 Wave 1"}}
```

Bulk assignment also works: retrieve the ids (e.g. via `GET /hub/applications` filtered by the archetype's criteria tags) and send them all in the wave's `applications` array. `DELETE /hub/migrationwaves/{id}` removes the wave and clears the assigned applications' `migrationWave` links.

### Your first Agentic Workflow

#### Assemble the Agentic Workflow components

For Agentic Migrations, MTA composes agent workloads from **Skills**:

> Authoring a new skill is covered in [Create your own skills](#create-your-own-skills); authoring analysis rules in [Create your own rules](#create-your-own-rules). This section wires finished skills into the cluster: SkillCard, Agent, and AgentWorkflow.

- SKILL.md files following the [Agent Skills](https://agentskills.io) format, packaged as OCI images or defined inline.
- Referenced via `SkillCard`, `Agent`, and `AgentWorkflow` custom resources.
- A workflow runs stages sequentially — typically `plan` → `execute` → `verify` — each in a fresh Agent Sandbox.
- Continuity comes from committed handoff files (`docs/plan.md`, `.konveyor/handoff.md`) on the shared target branch.
- The [Konveyor agentic-controller](https://github.com/konveyor/agentic-controller) documentation covers the details.

> ⚠️ NOTE
>
> The agentic-controller operator already installs working examples (SkillCards `plan`/`execute`/`verify`, Agents `migration-plan/execute/verify-agent`, and the `java-ee-to-quarkus` AgentWorkflow) — copy those instead of authoring from scratch.

- **Allow egress to the internal image registry**: the `openshift-mta` namespace ships a default-deny egress NetworkPolicy (`mta-egress`) allowing only ports 443, 80, 6443, 8443, 8081, and 8082 — not port 5000, which the internal image registry uses. Both the build pod pushing the image and the agentic-controller validating the `SkillCard` image (a registry HEAD request) need egress to `openshift-image-registry`, so apply this NetworkPolicy before building:

    ```bash
    oc apply -f mta-ui/allow-egress-to-image-registry.yaml
    ```

    Verify:

    ```bash
    oc get networkpolicy allow-egress-to-image-registry -n openshift-mta
    ```

<a id="allow-egress-on-port-22-for-ssh-based-git"></a>
- **Allow egress on port 22 for SSH-based git**: the same default-deny posture blocks port 22, which Agent Sandbox pods need when their git operations run over SSH remotes — the in-cluster Gitea SSH service (`gitea-ssh.gitea.svc:22`, see [SSH Keys](#ssh-keys)) or external git hosts on the standard SSH port. This NetworkPolicy is scoped to the Agent Sandbox pods (`app.kubernetes.io/managed-by: agentic-controller`):

    ```bash
    oc apply -f mta-ui/allow-egress-git-ssh.yaml
    ```

    Verify:

    ```bash
    oc get networkpolicy allow-egress-git-ssh -n openshift-mta
    ```

    Two details:

    - The in-cluster rule allows port **2222**: the `gitea-ssh` Service is headless, so its DNS name resolves straight to the pod IP and no port translation happens. Both the URL (`ssh://git@gitea-ssh.gitea.svc:2222/...`) and the policy must carry the container's listening port, not the Service's 22 (see [SSH Keys](#ssh-keys)).
    - The harness itself pushes over HTTPS only (the go-git-based `migration-harness` has no SSH support). This policy enables the agent runtime's own git-over-SSH operations (skills running `git` against SSH remotes); the TLS-passthrough SSH route is unaffected — it's HTTPS on port 443, already allowed.

    For ease of use, build the `SkillCard` container directly on OpenShift. The `--binary` flag creates the `BuildConfig` with binary input and **without automatically triggering a build** — the build only starts on the explicit `oc start-build`. `--binary` can't be combined with `--dockerfile`, and the `Containerfile` isn't named `Dockerfile`, so its path within the build context is set with `oc patch`:

    ```bash
    oc new-build --name mta-jboss-eap-7-to-8-skills --strategy=docker --binary -n openshift-mta

    oc patch bc mta-jboss-eap-7-to-8-skills -n openshift-mta --type=json \
    -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'

    oc start-build mta-jboss-eap-7-to-8-skills -n openshift-mta \
    --from-dir=mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/skills/jboss-eap-7-to-8 --follow
    ```

    The image is pushed to the `latest` tag of the `mta-jboss-eap-7-to-8-skills` ImageStream, so the `SkillCard` references it by its cluster-internal path `image-registry.openshift-image-registry.svc:5000/openshift-mta/mta-jboss-eap-7-to-8-skills:latest`.

    Verify the build:

    ```bash
    oc logs build/mta-jboss-eap-7-to-8-skills-1 -n openshift-mta --follow
    oc get imagestreamtag mta-jboss-eap-7-to-8-skills:latest -n openshift-mta
    ```

- **Create a SkillCard**: represents exactly one skill and can point at an OCI image, a git repository, or inline content. The loader validates the image at pod initialization and assembles the skill at `/opt/skills/<frontmatter-name>/SKILL.md`. For an image holding several skills, select one immediate directory with `subPath` — a bundle without `subPath` is rejected as ambiguous, not silently treated as several skills. The `type` field is the load policy and lives on the CR, not in `SKILL.md`:

    - `type: skill` — on-demand: the runtime starts with only the name and description and reads the full content when it invokes the skill.
    - `type: rule` — always-loaded: the harness includes the full content in every agent prompt (a stage's driving skill should be a rule).

    An inline skill (no registry involved) uses `inline:` instead of `image:` and is materialized as a ConfigMap. Inline skills are a single `SKILL.md` only (a ConfigMap key can't contain a path separator) — anything needing `references/` must be an image or git source.

    ```yaml
    apiVersion: konveyor.io/v1alpha1
    kind: SkillCard
    metadata:
      name: jboss-eap-7-to-8
      namespace: openshift-mta
    spec:
       image: image-registry.openshift-image-registry.svc:5000/openshift-mta/mta-jboss-eap-7-to-8-skills:latest
       displayName: JBoss EAP 7 to 8 Migration Skill
       version: "0.0.1"
       description: Migrates from JBoss EAP 7 to JBoss EAP 8.
       type: skill
       subPath: skill
       tags:
         - java
         - javaee
         - eap
         - migration
    ```

    ```bash
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/skills/jboss-eap-7-to-8/jboss-eap-7-to-8-skillcard.yaml
    ``` 

    Verify the `SkillCard`:

    ```bash
    oc wait --for create skillcard jboss-eap-7-to-8 -n openshift-mta --timeout=600s
    oc wait --for jsonpath='{.status.conditions[0].status}'="True" skillcard jboss-eap-7-to-8 -n openshift-mta --timeout=600s
    ```

- **Plan-stage Hub reporting (derived plan-agent image)**: the plan stage can report the migration issues it identifies back to the MTA Hub, so they appear as first-class issues in the application's Issues view — the same mechanism the [RHDA workflow](#rhda-dependency-cvelicense-scanner-with-hub-reporting) uses for CVEs. `POST /applications/:id/analyses` accepts the GS-delimited Analysis/Insight manifest the static analyzer writes; no new Hub endpoints. Two pieces, mirroring the rhda pattern:

    - A **derived agent image** (`plan-agent-image/Containerfile`) — stock `mta-agent-java` plus an ENTRYPOINT wrapper and the deterministic reporter. The wrapper runs *before* `migration-harness`, while the run's `HUB_BASE_URL`/`HUB_TOKEN`/`APP_ID` env is still set (the harness clears it before the agent starts), and provisions `/tmp/konveyor/hub-report/{hub-url,app-id,token,app-name,app-path}`; the reporter converts a findings JSON into the manifest and POSTs it. The manifest groups incidents per rule (one Insight per migration item, one incident per affected file — the static analyzer's own shape, so the Issues view shows one issue per rule). Only the **plan agent** runs this image: the execute/verify agents keep the stock image, whose sandboxes have no token file, so a misplaced reporter invocation no-ops with exit 0 — the decision lives in the script, not the model.
    - A **skill step** (`agents/skills/jboss-eap-7-to-8/skill/SKILL.md`, *Report planning findings to the MTA Hub*): while planning, the agent transcribes every applicable migration item into the findings JSON (rule slug, repository-relative file path prefixed with the provisioned `app-path`, category, phase, effort) and always runs `python3 /usr/local/bin/eap-hub-report.py --findings /tmp/eap-findings.json` — with zero findings as `[]` too. The model never hand-writes the manifest YAML.

    Build the image before applying the plan agent (the plan-agent manifest below references the internal registry tag):

    ```bash
    oc new-build --name mta-jboss-eap-7-to-8-plan-agent --strategy=docker --binary -n openshift-mta

    oc patch bc mta-jboss-eap-7-to-8-plan-agent -n openshift-mta --type=json \
    -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'

    oc start-build mta-jboss-eap-7-to-8-plan-agent -n openshift-mta \
    --from-dir=mta-ui/agentic-workflow/jboss-eap-7-to-8/plan-agent-image --follow
    ```

    Verify the build:

    ```bash
    oc get imagestreamtag mta-jboss-eap-7-to-8-plan-agent:latest -n openshift-mta
    ```

- **Create the Agent**: an Agent is a template — it declares the container image, skills, and turn limits but executes nothing. Omitting `spec.gateways` is intentional: the Gateway is then named at run time on the `AgentRun`/`AgentWorkflowRun` (this is what the [Konveyor Gateway](#create-a-konveyor-gateway) set up earlier is used for). All three agents also reference the `commit-when-done` **rule** (`mta-ui/agentic-rules/commit-when-done.yaml`, `type: rule`): a stage that ends without commits pushes nothing — the harness pushes committed work only — so the always-loaded commit instruction keeps the plan's `docs/plan.md` (and every execute-stage change) off the "written but never pushed" path (see the [troubleshooting notes](troubleshooting/README.md#agentic-run-the-stage-wrote-its-files-but-the-target-branch-has-no-commits)). The rule is plain cluster YAML (inline, no image, no `envsubst`):

    ```bash
    oc apply -f mta-ui/agentic-rules/commit-when-done.yaml
    oc wait --for=create skillcard commit-when-done -n openshift-mta --timeout=120s
    oc wait --for=jsonpath='{.status.conditions[0].status}'="True" skillcard commit-when-done -n openshift-mta --timeout=300s
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/jboss-eap-7-to-8-plan-agent.yaml
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/jboss-eap-7-to-8-execute-agent.yaml
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/jboss-eap-7-to-8-verify-agent.yaml
    ```

    ```bash
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/jboss-eap-7-to-8-plan-agent.yaml
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/jboss-eap-7-to-8-execute-agent.yaml
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/jboss-eap-7-to-8-verify-agent.yaml
    ```

    Several Agents sharing skills can reference a `SkillCollection` grouping multiple `SkillCard`s instead.

    Check that the agents were defined successfully:

    ```bash
    oc wait --for create agents.konveyor.io jboss-eap-7-to-8-plan-agent -n openshift-mta --timeout=600s
    oc wait --for create agents.konveyor.io jboss-eap-7-to-8-execute-agent -n openshift-mta --timeout=600s
    oc wait --for create agents.konveyor.io jboss-eap-7-to-8-verify-agent -n openshift-mta --timeout=600s
    oc wait --for jsonpath='{.status.conditions[0].status}'="True" agents.konveyor.io jboss-eap-7-to-8-plan-agent -n openshift-mta --timeout=600s
    oc wait --for jsonpath='{.status.conditions[0].status}'="True" agents.konveyor.io jboss-eap-7-to-8-execute-agent -n openshift-mta --timeout=600s
    oc wait --for jsonpath='{.status.conditions[0].status}'="True" agents.konveyor.io jboss-eap-7-to-8-verify-agent -n openshift-mta --timeout=600s
    ```

- **Create the AgentWorkflow**: combines an ordered sequence of stages, each referencing an Agent; stages run sequentially, each in a fresh Sandbox. `plan.md` is not something you author — it's the output contract of the plan stage: the plan skill defines the exact `docs/plan.md` structure (Goal, Source → Target, Scope, Steps with Phase/File/Action/Depends-on/Verify, Verification, Notes), and the execute and verify stages parse it. A custom plan skill must reproduce this output structure exactly.

    ```bash
    oc apply -f mta-ui/agentic-workflow/jboss-eap-7-to-8/jboss-eap-7-to-8-agent-workflow.yaml
    ```

    Verify the `AgentWorkflow`:

    ```bash
    oc wait --for create agentworkflow jboss-eap-7-to-8 -n openshift-mta --timeout=600s
    oc wait --for jsonpath='{.status.conditions[0].status}'="True" agentworkflow jboss-eap-7-to-8 -n openshift-mta --timeout=600s
    ```

#### Start an Agent Workflow Run

You can also start a run via the Hub API by creating an `AgentWorkflowRun` at `${MTA_URL}/hub/agentic/workflowruns`.

- The label `konveyor.io/application` links the run to the application.
- The creating client owns the `APP_ID` and `TARGET_BRANCH` env variables — the harness fail-fasts without `APP_ID`.
- The Hub owns `HUB_BASE_URL` and the minted API-token Secret, appended server-side.
- On success the API returns `201 Created` with the `AgentWorkflowRun` JSON object.

> ⚠️ NOTE
>
> Creating the `AgentWorkflowRun` directly as a CR (`oc apply` against `openshift-mta`) is **not** a working alternative to the REST API. On the REST path, the Hub mints a scoped API-token Secret and injects `HUB_BASE_URL` and the token into the run's `spec.env`/`spec.envFrom` server-side — the harness uses that token to resolve the application's repository and push the migrated code. A CR-only run bypasses the Hub, has no token, and under Hub authentication (all Hub calls here use `-u admin:admin`) fails with `401` immediately — resolving the repository is the harness's first step. The CR-only path only works with Hub authentication disabled (`AUTH_REQUIRED=false`, a dev-cluster option). This is why runs are created through the Hub API (or the MTA UI, which goes through the Hub).

Maven settings are **not** resolved from the Hub:

- Unlike git push credentials, which the harness resolves at runtime from the application's linked identities.
- The harness has no Maven identity lookup; a `maven`-kind identity is only consumed by analyses.
- The `AgentWorkflowRun` CRD supports `fileMounts`: mounting Secrets or ConfigMaps read-only into every stage's Sandbox.
- The sandbox image sets `HOME=/home/harness`, so a Secret at `/home/harness/.m2` would place `settings.xml` at Maven's default user-settings location.
- But the deployed Hub's REST binding predates the feature and rejects the field with `{"error":"json: unknown field \"fileMounts\""}`.
- The run CR's admission would accept the field, but a CR-only run has no Hub token — it fails under Hub authentication (see above).
- Agentic runs therefore carry Maven settings via the `MAVEN_SETTINGS_XML` env variable (see [Start an Agent Workflow Run](#start-an-agent-workflow-run)).
- Analyses consume them via the `maven`-kind identity (see [Configure Maven settings](#configure-maven-settings)).

- **Start the Workflow Run**: the migration is triggered by creating an `AgentWorkflowRun`, in the MTA UI (agentic migrations) or via the Hub API as in [Start an Agent Workflow Run](#start-an-agent-workflow-run) above. No credential linking is needed: the run's spec carries no credentials — the harness resolves the push credential from the Hub at runtime, preferring an identity linked to the application over the global default. The `git-credentials` identity above is the default, so it resolves for every application. To override it per application (e.g. per-repo credentials), link a basic-auth identity with the role `source`. The update is a full-object `PUT` (fetch, modify, write back) — the `jq` removes any existing link for the same identity first, since the Hub enforces a unique constraint on `(ApplicationID, IdentityID, Role)` and a plain append fails if the identity is already linked.

    ```bash
    APP_ID=$(curl -k -s "${MTA_URL}/hub/applications" -u admin:admin \
      | jq -r '.[] | select(.name=="helloworld") | .id')

    IDENTITY_ID=$(curl -k -s "${MTA_URL}/hub/identities" -u admin:admin \
      | jq -r '.[] | select(.name=="git-credentials") | .id')

    curl -k -s -X PUT "${MTA_URL}/hub/applications/${APP_ID}" \
      -u admin:admin \
      -H "Content-Type: application/json" \
      -d "$(curl -k -s "${MTA_URL}/hub/applications/${APP_ID}" -u admin:admin \
        | jq --argjson id "${IDENTITY_ID}" \
            '.identities = (((.identities // []) | map(select(.id != $id))) + [{id: $id, role: "source"}])')"
    ```

    > ⚠️ NOTE
    >
    > The harness resolves the application's repository URL verbatim and clones it with HTTP basic auth only — an application whose URL uses the SSH scheme cannot be cloned by the run and fails at the clone step. Import applications that run through Agent Workflow migrations with HTTPS URLs (see the warning in [Bulk Uploading Applications](#bulk-uploading-applications)).

    Then create the run referencing the `jboss-eap-7-to-8` workflow. The creating client owns `APP_ID` and `TARGET_BRANCH`; the Hub appends `HUB_BASE_URL` and the minted API-token Secret server-side.

    Custom Maven settings ride along as the `MAVEN_SETTINGS_XML` env variable — only needed if you configured custom Maven settings; omit the entry otherwise. The settings are `envsubst`'ed (the `MAVEN_PROXY_*` placeholders and `MTA_`-prefixed proxy variables resolve — export the `MAVEN_PROXY_*` derivations from [Configure Maven settings](#configure-maven-settings) first), quote-escaped for JSON embedding, and collapsed to one line (newlines are insignificant in XML). The payload uses a heredoc (`--data @-`): a quoting error in a multi-line `-d "..."` string makes curl send an empty body, which the Hub rejects with `{"error":"EOF"}`:

    ```bash
    export MAVEN_SETTINGS_XML=$(envsubst < mta-ui/maven-settings.xml | sed 's/\"/\\\"/g' | tr '\n' ' ')

    curl -k -s -X POST "${MTA_URL}/hub/agentic/workflowruns" \
      -u admin:admin \
      -H "Content-Type: application/json" \
      --data @- <<EOF
    {
      "metadata": {
        "generateName": "api-",
        "labels": {"konveyor.io/application": "${APP_ID}"}
      },
      "spec": {
        "workflowRef": "jboss-eap-7-to-8",
        "gateway": "openai",
        "env": [
          {"name": "APP_ID", "value": "${APP_ID}"},
          {"name": "TARGET_BRANCH", "value": "konveyor/migration-jboss-eap-7-to-8-$(date +%s)"},
          {"name": "MAVEN_SETTINGS_XML", "value": "${MAVEN_SETTINGS_XML}"}
        ]
      }
    }
    EOF
    ```

    The REST binding accepts `spec.env`, and the controller passes it to every stage's Sandbox — each stage is instructed to write the value to `/home/harness/.m2/settings.xml` when the variable is set (the sandbox sets `HOME=/home/harness`, Maven's default user-settings location). The instruction is on every stage because each runs in a fresh Sandbox and `/home/harness` does not persist between them — continuity carries via git on the target branch.

    The controller creates an `AgentRun` per stage (`plan` → `execute` → `verify`) sequentially, each in a fresh Sandbox. The harness clones the application's repository from the Hub record and pushes the results to the target branch. Verify the run:

    ```bash
    oc get agentworkflowrun,agentrun -n openshift-mta
    oc get agentworkflowrun -n openshift-mta -o jsonpath='{range .items[*]}{.metadata.name}{": "}{.status.conditions[0].type}{"\n"}{end}'
    ```

### Additional Agentic Workflows

#### Spring Boot to Quarkus

> The `new-build --dockerfile="-"` pattern reads the Containerfile from stdin and auto-triggers a first build that **fails** (`DockerBuildFailed`) — it has no binary input yet. Ignore it (or delete it with `oc delete build <name>-1`). Build 2 from `oc start-build --from-dir` is the one that produces the image.

```bash
oc new-build --name mta-spring-boot-to-quarkus-skills -n openshift-mta \
  --dockerfile="-" < mta-ui/agentic-workflow/spring-boot-to-quarkus/agents/skills/spring-boot-to-quarkus/Containerfile

oc start-build mta-spring-boot-to-quarkus-skills -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/spring-boot-to-quarkus/agents/skills/spring-boot-to-quarkus --follow

oc apply -f mta-ui/agentic-workflow/spring-boot-to-quarkus/agents/skills/spring-boot-to-quarkus/spring-boot-to-quarkus-skillcard.yaml

oc apply -f mta-ui/agentic-workflow/spring-boot-to-quarkus/agents/spring-boot-to-quarkus-plan-agent.yaml
oc apply -f mta-ui/agentic-workflow/spring-boot-to-quarkus/agents/spring-boot-to-quarkus-execute-agent.yaml
oc apply -f mta-ui/agentic-workflow/spring-boot-to-quarkus/agents/spring-boot-to-quarkus-verify-agent.yaml

oc apply -f mta-ui/agentic-workflow/spring-boot-to-quarkus/spring-boot-to-quarkus-agent-workflow.yaml
```

#### Apache Ant to Apache Maven

> As with [Spring Boot to Quarkus](#spring-boot-to-quarkus), the auto-triggered first build fails and is superseded by the `oc start-build` build.

```bash
oc new-build --name mta-ant-to-maven-skills -n openshift-mta \
  --dockerfile="-" < mta-ui/agentic-workflow/ant-to-maven/agents/skills/ant-to-maven/Containerfile

oc start-build mta-ant-to-maven-skills -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/ant-to-maven/agents/skills/ant-to-maven --follow

oc apply -f mta-ui/agentic-workflow/ant-to-maven/agents/skills/ant-to-maven/ant-to-maven-skillcard.yaml

oc apply -f mta-ui/agentic-workflow/ant-to-maven/agents/ant-to-maven-plan-agent.yaml
oc apply -f mta-ui/agentic-workflow/ant-to-maven/agents/ant-to-maven-execute-agent.yaml
oc apply -f mta-ui/agentic-workflow/ant-to-maven/agents/ant-to-maven-verify-agent.yaml

oc apply -f mta-ui/agentic-workflow/ant-to-maven/ant-to-maven-agent-workflow.yaml
```

#### MigIQ

[MigIQ](https://www.npmjs.com/package/@sshaaf/migiq) is an AI-powered migration orchestrator for coding agents:

- the skills ship as a **multi-skill image**: the `migiq` orchestrator, the `mig-*` phase skills, and the `rgctl`/`rgctl-review` analysis skills;
- one `SkillCard` per skill selects its skill via `subPath`;
- the image builds on `quay.io/konveyor/agent-java` and compiles the `rgctl` CLI (v0.4.24) from source, which the skills drive for knowledge-graph code analysis.

```bash
oc new-build --name mta-migiq-agent --strategy=docker --binary -n openshift-mta

oc patch bc mta-migiq-agent -n openshift-mta --type=json \
-p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'

oc start-build mta-migiq-agent -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/migiq/agents --follow

oc apply -f mta-ui/agentic-workflow/migiq/agents/skills/migiq-skillcards.yaml
oc apply -f mta-ui/agentic-workflow/migiq/agents/skills/migiq-skillcollection.yaml
oc apply -f mta-ui/agentic-workflow/migiq/agents/migiq-agent.yaml
oc apply -f mta-ui/agentic-workflow/migiq/migiq-agent-workflow.yaml
```

The build takes ~10 minutes — it compiles `rgctl` with the Rust toolchain. What the manifests create:

- **SkillCards**: one per skill (`migiq`, `mig-rgctl`, `mig-prompt-builder`, `mig-plan`, `mig-execute`, `mig-test-gen`, `mig-containerize`, `mig-deploy`, `mig-verify`, `rgctl`, `rgctl-review`), all pointing at `image-registry.openshift-image-registry.svc:5000/openshift-mta/mta-migiq-agent:latest`, with `subPath` naming the skill's directory at the image root.
- **SkillCollection**: groups the eleven cards as `migiq-skills`.
- **Agent**: the single `migiq-agent` orchestrator on the built image, referencing the collection. Unlike ant-to-maven's plan/execute/verify trio, MigIQ chains the phases itself.
- **AgentWorkflow**: wraps the agent as a single-stage `migiq` workflow with a required `target_stack` parameter (the migration destination, referenced as `$(workflow.target_stack)` in the stage instructions);
- the `AgentWorkflowRun` supplies `target_stack`; an `AgentRun`/`AgentWorkflowRun` names the gateway to run it with.

Verify them as follows:

```bash
oc get skillcards -n openshift-mta | grep -E "migiq|rgctl|mig-"
oc get skillcollection migiq-skills -n openshift-mta
oc get agents.konveyor.io migiq-agent -n openshift-mta
oc get agentworkflow migiq -n openshift-mta
```

#### RHDA (dependency CVE/license scanner with Hub reporting)

The `rhda` workflow uses a **standalone agent image** (separate from the migiq image):

- it bakes the [`ruromero/rhda-mcp-server`](https://github.com/ruromero/rhda-mcp-server) stdio MCP server into the goose harness config (`$XDG_CONFIG_HOME/goose/config.yaml`);
- the config path is pinned via `XDG_CONFIG_HOME` because uid 1001's `HOME` depends on the base image;
- the server exposes Red Hat Dependency Analytics (RHDA) as agent tools: `analyze_dependency_vulnerabilities`, `explain_vulnerability`, `generate_remediation_plan`, `suppress_dependency_vulnerability`;
- the server is not published to npm, so the Containerfile clones it and runs it with `tsx` from source, pinned by the `RHDA_MCP_REF` build arg.

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

The image also ships the **`rhda-cve-scan`** skill (selected by the `rhda-skillcards` SkillCard / `rhda-skills` SkillCollection). It enumerates the Maven manifests, analyzes each with the RHDA backend, translates the poms' declared licenses to SPDX identifiers and reconciles them against the backend-resolved licenses, writes a consolidated `rhda-cve-report.md`, and gates remediation/suppression behind user confirmation.

**Scan scope**: the sandbox harness clones the ENTIRE repository of the application's Hub record, never exposes the record's `repository.path`, and clears the `HUB_*` env before the agent starts. In a monorepo like the jboss quickstarts (123 applications), an unscoped scan would analyze every other application. Scoping is enforced at two layers:

- the workflow's optional **`path`** parameter (the Hub record's repository path, e.g. `helloworld`), bound into the stage instructions;
- the image ENTRYPOINT (`entrypoint-wrapper.sh`, wrapping `migration-harness`): while the `HUB_*` env is still set, it provisions `/tmp/konveyor/hub-report/{hub-url,app-id,token,app-name,app-path}` — the token as a 0600 file;
- `app-path` (from the Hub record) is the authoritative scope, overriding the instructions.

**Hub reporting**: CVEs found during a scan are ingested through the same path the static analyzer uses — `POST /applications/:id/analyses` accepts a GS-delimited (`\x1D`) manifest of Analysis / Insight / TechDependency resources (sections are YAML **document streams**, one resource per `---`-separated document). The ingested insights appear as first-class issues in the application's Issues/Reports views; no new hub endpoints are needed. The skill's final step transcribes the MCP scan results into a findings-JSON schema and runs the deterministic `/usr/local/bin/rhda-hub-report.py` — the model never hand-writes the manifest YAML. Reporting is skipped when no token file was provisioned (standalone runs). Hub tokens are minted with hub **Basic auth** (`POST /auth/tokens -u admin:admin`) — the hub is its own OIDC provider and rejects Keycloak JWTs.

Launch from the MTA UI with the application's repository path in the `path` parameter, or via the Hub REST API as with the migration workflow. Two requirements on every run:

- The harness requires the `APP_ID` and `TARGET_BRANCH` env vars. The UI supplies `TARGET_BRANCH` from its `konveyor/migration-<timestamp>` default; a REST launch must set both explicitly.
- Workflow parameters travel as the `params` array of `{name, value}` objects.

> Verified live: a run without `TARGET_BRANCH` fails with `config: required env var TARGET_BRANCH is not set`; a run without `APP_ID` fails the same way before any stage starts.

```bash
APP_ID=$(curl -k -s "${MTA_URL}/hub/applications" -u admin:admin \
  | jq -r '.[] | select(.name=="helloworld") | .id')

curl -k -s -X POST "${MTA_URL}/hub/agentic/workflowruns" \
  -u admin:admin -H "Content-Type: application/json" \
  --data @- <<EOF
{
  "metadata": {
    "generateName": "api-rhda-",
    "labels": {"konveyor.io/application": "${APP_ID}"}
  },
  "spec": {
    "workflowRef": "rhda",
    "gateway": "openai",
    "params": [{"name": "path", "value": "helloworld"}],
    "env": [
      {"name": "APP_ID", "value": "${APP_ID}"},
      {"name": "TARGET_BRANCH", "value": "konveyor/rhda-scan-$(date +%s)"}
    ]
  }
}
EOF
```

Verify the run with:

```bash
oc get agentworkflow rhda -n openshift-mta
# after a run - the analysis with ruleset rhda insights:
oc exec deploy/mta-ui -n openshift-mta -- curl -k -s -u admin:admin \
  "http://mta-hub.openshift-mta.svc:8080/applications/<app-id>/analyses" | jq .
```

#### Application dependency map (portfolio interdependencies)

The `app-dependency-map` workflow maps **inter-application dependencies across the whole portfolio**:

- it records edges in the Hub's application-dependency graph (`POST /dependencies {"from": {"id": A}, "to": {"id": B}}`);
- that graph drives the UI's application dependency views and migration-wave planning;
- it's a standalone agent/workflow pair in `mta-ui/agentic-workflow/app-dependency-map/`, own image `mta-app-dependency-map-agent`;
- no MCP servers — the mapping is deterministic code plus Hub REST.

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

The `app-dependency-map.py` mapper (baked to `/usr/local/bin/`) enumerates the Hub's applications (`GET /applications`), resolves each application's Maven coordinates from its `pom.xml` (located via the Hub record's `repository.path`), matches every pom's `<dependencies>` against the other applications' coordinates (EAR aggregators and their modules fall out naturally), and reconciles the Hub's dependency graph:

- create missing edges (`--apply`);
- keep existing ones (idempotent — the static analyzer may already have created edges);
- prune stale edges only on explicit user request, and only those created by the same Hub user (`GET /auth/self`).

The model orchestrates and sanity-checks but never hand-writes edges:

- aggregator-pom tooling dependencies are the known false-positive class;
- launch from the MTA UI on an application whose repository is the monorepo holding the portfolio's sources (e.g. the quickstarts);
- the optional `dry_run` parameter computes and reports the map without writing to the Hub;
- applications whose sources aren't in the workspace are skipped and reported as the map's coverage boundary.

> Verified live on the quickstarts dry-run: module poms inheriting coordinates via Maven properties (`${project.groupId}`-style parent refs) aren't resolved by the deterministic matcher, so genuine EAR→module edges can be invisible to it. The mapper reported 0 edges; the model sanity-checked against the repository and the Hub's import-created edges and correctly reported the ~19 genuine EAR→module edges as runtime-coupling candidates for manual review rather than writing a wrong map. The map and the analysis are committed to the target branch as `app-dependency-map.md` and the `.konveyor/handoff.md` summary.

Via the Hub REST API the same launch looks like this — as with every run, the harness requires the `APP_ID` and `TARGET_BRANCH` env vars, and `params` is a list of `{name, value}` objects, not a map:

```bash
APP_ID=$(curl -k -s "${MTA_URL}/hub/applications" -u admin:admin \
  | jq -r '.[] | select(.name=="helloworld") | .id')

curl -k -s -X POST "${MTA_URL}/hub/agentic/workflowruns" \
  -u admin:admin -H "Content-Type: application/json" \
  --data @- <<EOF
{
  "metadata": {
    "generateName": "api-depmap-",
    "labels": {"konveyor.io/application": "${APP_ID}"}
  },
  "spec": {
    "workflowRef": "app-dependency-map",
    "gateway": "openai",
    "params": [{"name": "dry_run", "value": "true"}],
    "env": [
      {"name": "APP_ID", "value": "${APP_ID}"},
      {"name": "TARGET_BRANCH", "value": "konveyor/depmap-$(date +%s)"}
    ]
  }
}
EOF
```

#### Citrus tests (test authoring via the Citrus MCP server)

The `citrus-tests` workflow **adds Citrus framework tests to a JEE application**:

- a plan → author → verify pipeline in `mta-ui/agentic-workflow/citrus-tests/` (own image `mta-citrus-tests-agent`, three per-stage agents sharing it);
- the image bakes the **Citrus MCP server** ([`org.citrusframework:citrus-mcp-server`](https://central.sonatype.com/artifact/org.citrusframework/citrus-mcp-server), Quarkus-based STDIO server of the Citrus 5 line for AI-assisted test development) into the goose harness config;
- same image-baked-extension pattern as the rhda agent, minus the Hub reporting wrapper;
- the server exposes the framework's test vocabulary as eight MCP tools: `citrus_catalog_actions`/`action`/`action_schema`, `citrus_catalog_endpoints`/`endpoint`/`endpoint_schema`, `citrus_docs_index`/`docs_page`;
- its catalog tools accept a `version` argument to query other Citrus versions' catalogs, resolved from Maven Central at request time.

Build and register it like the other standalone workflows — the build takes a few minutes because it downloads the MCP server uber-jar from Maven Central:

```bash
oc new-build --name mta-citrus-tests-agent --strategy=docker --binary -n openshift-mta
oc patch bc mta-citrus-tests-agent -n openshift-mta --type=json \
  -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'
oc start-build mta-citrus-tests-agent -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/citrus-tests --follow
oc apply -f mta-ui/agentic-workflow/citrus-tests/skills/citrus-test-skillcards.yaml
oc apply -f mta-ui/agentic-workflow/citrus-tests/citrus-tests-agents.yaml
oc apply -f mta-ui/agentic-workflow/citrus-tests/citrus-tests-agent-workflow.yaml
```

The three stages — each in a fresh sandbox, with continuity via git commits on the target branch (as with the migration workflow):

- **plan** (`citrus-tests-plan-agent`, `citrus-test-plan` skill) — inventories the application's testable JEE surface (JAX-RS resources, EJBs, servlets, CDI beans, consumed endpoints), derives the Citrus version from the Java baseline (Java 17+ → Citrus 5.x, Java 8/11 → Citrus 3.x, or pin it with the `citrus_version` run parameter), consults the MCP catalogs/docs, and writes `docs/citrus-test-plan.md` with the test inventory, the exact test-scoped dependencies to add, and the verification commands. Only strategies that run green under plain `mvn test` are planned (in-JVM embedded deployments, Citrus endpoint simulations — no application server, nothing beyond localhost).
- **author** (`citrus-tests-author-agent`, `citrus-test-author` skill) — implements the plan: imports the Citrus BOM, adds the test-scoped artifacts, writes the JUnit 5-run Citrus test classes grounded in the MCP catalog/schemas (never from recall), and iterates `mvn test` until the new suite is green. Only `src/test/` and test-scoped pom changes are allowed — `src/main/` is never touched, and existing tests (e.g. Arquillian `*IT`s run by failsafe) stay untouched.
- **verify** (`citrus-tests-verify-agent`, `citrus-test-verify` skill) — re-runs the suite from clean in the fresh sandbox, confirms the new tests actually executed (not skipped), fixes test-side defects only, and records the verdict in the `## Verify` section of `.konveyor/handoff.md`.

Launch it like the other workflows (the harness requires `APP_ID` and `TARGET_BRANCH`; the `path` parameter scopes to the application's repository path):

```bash
APP_ID=$(curl -k -s "${MTA_URL}/hub/applications" -u admin:admin \
  | jq -r '.[] | select(.name=="helloworld-rs") | .id')

curl -k -s -X POST "${MTA_URL}/hub/agentic/workflowruns" \
  -u admin:admin -H "Content-Type: application/json" \
  --data @- <<EOF
{
  "metadata": {
    "generateName": "api-citrus-",
    "labels": {"konveyor.io/application": "${APP_ID}"}
  },
  "spec": {
    "workflowRef": "citrus-tests",
    "gateway": "openai",
    "params": [{"name": "path", "value": "helloworld-rs"}],
    "env": [
      {"name": "APP_ID", "value": "${APP_ID}"},
      {"name": "TARGET_BRANCH", "value": "konveyor/citrus-tests-$(date +%s)"}
    ]
  }
}
EOF
```

The run's deliverable is the target branch: the plan document, the committed test classes, and the handoff (`## Plan`/`## Author`/`## Verify` sections, fixed-format per the plan skill's template, ending in exactly one of the two legal verdict lines).

> **Editing the workflow requires two deployments, not one.** The skills ride in the agent image (rebuild via `oc start-build mta-citrus-tests-agent ...`), but the **workflow CR itself is a cluster resource** — after changing `citrus-tests-agent-workflow.yaml` (params, stage instructions, guide text), re-apply it (`oc apply -f citrus-tests-agent-workflow.yaml -n openshift-mta`) or runs against the old CR silently ignore new parameters.

> Verified live: a `test_kind: "it"` launch against a stale CR ran the in-JVM lane — the plan stage never saw the parameter because the live CR's `spec.params` lacked it.

The verify stage also quantifies the coverage the new tests add:

- it runs the suite under the JaCoCo agent (command-line goals, no pom change) and parses `target/site/jacoco/jacoco.csv`;
- it records per-package/per-class instruction and branch coverage in the `## Verify` section, together with the pre-existing baseline (the same run with the new tests excluded);
- on `helloworld-rs`, which had no prior tests, the report reads 0% → 100% instruction coverage of the application classes;
- coverage is reported, not gated: the workflow's gap accounting stays element-based (the plan's inventory), with JaCoCo as the quantitative fifth net.

### The container lane: in-container tests against JBoss EAP

Beyond the default in-JVM lane, the workflow supports a **container lane** (`test_kind: it`).

- The author stage creates a `citrus-container-tests` Maven profile that provisions a **real JBoss EAP server from the EAP Galleon feature pack**.
- Mechanics: `wildfly-maven-plugin` provision → start → deploy around failsafe, server shutdown in `post-integration-test`; planned `*IT` classes drive the deployed application with Citrus `http` clients.
- The verify stage runs `mvn verify -Pcitrus-container-tests` and reads the failsafe reports.

Choose this lane when the plan must prove deployment-time behavior an embedded deployment cannot honor (container-managed `@ApplicationPath` handling, servlet mappings, container services). The lane provisions exactly one server, so anything needing infrastructure beyond it stays out of scope — with a reason.

> Verified live: validated end-to-end on `helloworld-rs` against **EAP 8.1 Update 9.0** (provisioned from `org.jboss.eap:wildfly-ee-galleon-pack` via the `org.jboss.eap.channels:eap-8.1` channel manifest): 2/2 Citrus ITs green, server-side JaCoCo at 100% instruction coverage. Five defects are now encoded in the author skill:
>
> - wildfly-maven-plugin must be ≥ 5.x (4.x cannot parse the EAP feature-pack schema 3.0).
> - Galleon resolves feature-pack artifacts as *plugin* dependencies, so the profile needs its own `<pluginRepositories>`.
> - The EAP channel manifest is the real artifact resolver — several EAP artifacts are not on `redhat-ga`.
> - The `provision` goal ignores `jbossHome` (mirror it with `provisioning-dir`).
> - Maven caches failed resolutions; retries after a repository fix need `-U`.
>
> Coverage here rides on the **server's** JVM (`-javaagent` via the `start` goal); the exec file flushes on server shutdown and `jacoco:report` runs in the `verify` phase. The verify skill documents the lane-specific report path and baseline exclusion.

Two lessons from the validated run:

- **The harness clones the repository's default branch**, not the application record's `branch` metadata — the citrus run planned against the `8.1.x`-layout sources although the record carries `7.4.x`. Keep this in mind when comparing run results against a recorded branch.
- **Citrus 5.x needs the Jackson-3-compatible annotations line**: the author stage must pin `com.fasterxml.jackson.core:jackson-annotations` to `2.22`+ (test scope) when the application BOM manages the Jackson 2 line. Compilation succeeds with the wrong pin, but every Citrus test fails at runtime (`Could not initialize class tools.jackson.databind.json.JsonMapper$Builder`). The skills carry this recipe and the failure signatures.
- **The sandbox bakes a dead Maven proxy**: the agent image ships `/home/harness/.m2/settings.xml` with a Squid proxy (`squid.squid.svc:3128`). With `MAVEN_SETTINGS_XML` unset, the citrus author/verify stages and the rhda scan hit every-download timeouts and had to strip the `<proxies>` block first. The skills now probe and remove the proxy when unreachable. For disconnected environments the proxy is correct: pass `MAVEN_SETTINGS_XML` from the Hub's Maven settings identity instead, and make sure the Squid allowlist covers every repository the application builds from (e.g. `repository.jboss.org`).

**Gap closed — data-backed REST in the in-JVM lane** (validated live on `kitchensink`, 4/4 tests green in plain `mvn test`): REST resources that reach JPA through CDI no longer need the container lane. The recipe — now encoded in the plan and author skills — bootstraps the application's CDI beans with **Weld SE using explicit registration** (`disableDiscovery()` + `addPackage`/`addBeanClasses`; classpath discovery is unreliable in a surefire JVM — archives without `beans.xml` may be skipped, leaving the resource a non-bean), then:

- replaces container-provided resources with a test `@Alternative` producer (request-scoped `EntityManager` over a test-only H2 persistence unit, Validator, Logger);
- vetoes beans that inject what no SE bootstrap can supply (a JSF controller injecting `FacesContext`);
- registers one test filter on the embedded RESTEasy deployment that per request activates the CDI request context (built-in `RequestContextController` — Weld 5 removed `ContextControl`) and commits a resource-local transaction (CMT is inactive in-JVM, and Hibernate silently skips writes outside a transaction).

Integration defects found and encoded in the skills:

- resteasy-cdi's `ResteasyCdiExtension` must be added programmatically under `disableDiscovery()`.
- `CdiInjectorFactory` is preset by instance (`setInjectorFactory(new CdiInjectorFactory(beanManager))`).
- RESTEasy's transitive `org.jboss:jandex` 2.x shadows the `io.smallrye:jandex` 3.x Weld 5 needs (`NoSuchMethodError: Indexer.indexWithSummary`).
- POSTs need an explicit `Content-Type` (Citrus sends none — 415).
- Values inside JSON payloads must be asserted with JSON-path validation (`citrus-validation-json`; the element-wise JSON validator ignores plain-text matchers on whole payloads).

The recipe's first **autonomous** cluster run (`citrus-kitchensink-gc492`) verified end to end:

- Plan 34m / author 30m / verify 10m — `Verdict: verified`, 6/6 tests, 0% → 53.3% instruction / 71.4% branch coverage, repository + service layers at 100%.
- New `Iterations`/`Pitfalls hit` handoff fields measured the loop: 12 mvn runs, 6 failed — all cleanly diagnosed.

Three pitfalls surfaced that a warm local dry run could not:

- the jpamodelgen/hibernate-core nearest-wins scope crash (now a doctor `deps` check + recipe item);
- Weld 5 client-proxy laziness (`container.select(...).get()` never runs `@PostConstruct` — the test PU must be bootstrapped eagerly);
- `addAlternative()` requiring an explicit `addBeanClasses()` under `disableDiscovery()`.

The doctor's pre-flight also fired in every sandbox (dead Squid proxy), and its `deps` mode caught the jandex and jackson conflicts before any test boot.

#### JBoss EJB to REST (inter-application EJB couplings to REST)

The `jboss-ejb-to-rest` workflow **migrates inter-application EJB couplings to REST** — remote `@Remote` interfaces, `ejb:/` JNDI lookups, and SOAP/JAX-WS-exposed EJBs — in a JBoss EAP 8.0+ codebase. It is a plan → author → verify pipeline in `mta-ui/agentic-workflow/jboss-ejb-to-rest/` (own image `mta-jboss-ejb-to-rest-agent`, three per-stage agents sharing it).

The image bakes the three stage skills (each an agentskills.io SKILL.md with its own `references/` mapping tables) plus the **original refactoring guide** with its worked `example/` (a `before/` tree that must transform into `after/`). The skills cite the guide's sections as `§n` references and use the example to calibrate the transformations, so both ride in the image at `/guide/`:

```bash
oc new-build --name mta-jboss-ejb-to-rest-agent --strategy=docker --binary -n openshift-mta
oc patch bc mta-jboss-ejb-to-rest-agent -n openshift-mta --type=json \
  -p='[{"op":"add","path":"/spec/strategy/dockerStrategy/dockerfilePath","value":"Containerfile"}]'
oc start-build mta-jboss-ejb-to-rest-agent -n openshift-mta \
  --from-dir=mta-ui/agentic-workflow/jboss-ejb-to-rest --follow
oc apply -f mta-ui/agentic-workflow/jboss-ejb-to-rest/skills/jboss-ejb-to-rest-skillcards.yaml
oc apply -f mta-ui/agentic-workflow/jboss-ejb-to-rest/jboss-ejb-to-rest-agents.yaml
oc apply -f mta-ui/agentic-workflow/jboss-ejb-to-rest/jboss-ejb-to-rest-agent-workflow.yaml
```

The three stages run each in a fresh sandbox, with continuity via git commits on the target branch (as with the other migration workflows):

- **plan** (`jboss-ejb-to-rest-plan-agent`, `jboss-ejb-to-rest-plan` skill) — runs the deterministic discovery commands (grep sweeps for `@Remote`/`@EJB`/`ejb:` lookups/`@WebService`, EJB client artifacts, shared API jars, async/state that does not survive a split) and writes the inventory to `docs/jboss-ejb-to-rest-inventory.md` with a per-call-site go/no-go verdict. It **proposes** (never finalizes) the migration order: simple read-only calls first, transaction-participating calls last, one provider interface at a time. No code changes.
- **author** (`jboss-ejb-to-rest-author-agent`, `jboss-ejb-to-rest-author` skill) — applies the transformations one call site at a time (strangler-fig), driven by the mapping tables (`references/api-map.md`, `pattern-map.md`, `dependency-map.md`, `config-map.md`): provider-side JAX-RS resources over the beans' local views (the EJBs stay untouched), exception→status mappers, OpenAPI publication; consumer-side MicroProfile REST Client generated from the OpenAPI document behind an adapter seam that doubles as the rollback point. Every change is gated by `mvn clean test-compile`; old EJB/SOAP paths are never modified or deleted.
- **verify** (`jboss-ejb-to-rest-verify-agent`, `jboss-ejb-to-rest-verify` skill) — runs the per-call-site cutover gates (contract tests against both paths, error scenarios including 401, no remaining `ejb:` lookups, unchanged call-site signatures) and triages runtime errors against `references/verify-errors.md` (EAP-verified signatures: `EJBCLIENT000409`, `WELD-001408`, `WFLYEJB0406`, ...). The cleanup checklist runs only when all call sites have cut over. The verdict (`verified` / `verified with exceptions`) is recorded in the `## Verify` section of `.konveyor/handoff.md`.

The workflow carries an optional `path` parameter (the application's repository path in a multi-module repository, e.g. a quickstart) scoping the entire analysis and transformation to that directory. Launch it like the other workflows (the harness requires `APP_ID` and `TARGET_BRANCH`):

```bash
APP_ID=$(curl -k -s "${MTA_URL}/hub/applications" -u admin:admin \
  | jq -r '.[] | select(.name=="helloworld") | .id')

curl -k -s -X POST "${MTA_URL}/hub/agentic/workflowruns" \
  -u admin:admin -H "Content-Type: application/json" \
  --data @- <<EOF
{
  "metadata": {
    "generateName": "api-ejbrest-",
    "labels": {"konveyor.io/application": "${APP_ID}"}
  },
  "spec": {
    "workflowRef": "jboss-ejb-to-rest",
    "gateway": "openai",
    "params": [{"name": "path", "value": "helloworld"}],
    "env": [
      {"name": "APP_ID", "value": "${APP_ID}"},
      {"name": "TARGET_BRANCH", "value": "konveyor/ejb-to-rest-$(date +%s)"}
    ]
  }
}
EOF
```

The run's deliverable is the target branch: the inventory, the REST endpoints and consumers added alongside the untouched EJB/SOAP paths, and the handoff (`## Plan`/`## Author`/`## Verify` sections, fixed-format per the plan skill's template, ending in exactly one of the two legal verdict lines).

⚠️ NOTE: the workflow is scoped to **EAP 8.0+ codebases** already on the `jakarta.*` namespace. The plan stage stops and reports when the application is still on EAP 7.4/`javax.*` — the namespace migration is a separate migration that must run first (e.g. the `jboss-eap-7-to-8` workflow above).

## Developer-Infused Migration Using OpenShift Dev Spaces and the MTA Plugin

Developers can migrate their own applications from inside their IDE, alongside the [MTA UI](#automated-migration-using-the-mta-ui)'s portfolio-wide automated migration:

- **OpenShift Dev Spaces**: Kubernetes-based cloud development environments, installed via the OpenShift Dev Spaces Operator from OperatorHub.
- **MTA plugin** (Visual Studio Code extension): inline analysis, remediation hints, and AI-assisted refactoring (Developer Lightspeed) in the editor.
- Backed by the MTA Hub and Solution Server deployed in the [automated migration](#automated-migration-using-the-mta-ui) part.
- The same plugin flow also works without Dev Spaces in desktop VS Code — see [Local Installation](#local-installation-desktop-vs-code-without-dev-spaces).

### OpenShift Dev Spaces Installation

- **Prerequisites**: OpenShift Container Platform v4.22 running, cluster-admin login, a default StorageClass, and adequate cluster CPU/memory.
- **Dev Spaces Operator Installation**: Create the namespace, OperatorGroup, and Subscription:

  ```bash
  oc apply -f devspaces/openshift-devspaces-operator.yaml
  ```
  Verify the operator deployment:

  ```bash
  oc wait --for=create deployment devspaces-operator -n openshift-devspaces --timeout=600s
  oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment devspaces-operator -n openshift-devspaces --timeout=600s
  ```
  Each command should return **`condition met`**.
- **Create the CheCluster**: Create the `CheCluster` custom resource that configures and starts Dev Spaces. The manifest is cluster-independent — the cluster domain is a `${OPENSHIFT_APPS_DOMAIN}` placeholder (devfile-registry URL and proxy `nonProxyHosts`), so apply it with the same envsubst pattern as the secrets. Plain `oc apply -f` would store the placeholder literally:

    ```bash
    cat devspaces/che-cluster.yaml | envsubst | oc apply -f -
    ```
    Wait for the instance to become active:

    ```bash
    oc wait --for=jsonpath='{.status.chePhase}'=Active checluster devspaces -n openshift-devspaces --timeout=600s
    ```
    The expected output is **`condition met`**.
- **Accessing Dev Spaces**: Get the dashboard URL:

    ```bash
    echo "Dev Spaces URL: https://$(oc get route devspaces -n openshift-devspaces -o jsonpath='{.spec.host}')"
    ```
    Log in with your OpenShift credentials.

#### Extension marketplace configuration

The editor's extension gallery (the `Extensions` view used to install the MTA plugin) is served from an **Open VSX registry**:

- The embedded instance holds only a curated subset of the extensions published on public `open-vsx.org` (for air-gapped and proxy-restricted environments).
- On Dev Spaces 3.31 it is no longer a separate `plugin-registry` pod — the default gallery is empty until you switch to the public registry below.
- The Microsoft VS Code Marketplace is not supported in Dev Spaces due to Microsoft's Terms of Use — Open VSX is the supported gallery.

This deployment switches the gallery to the **public Open VSX registry** via the `pluginRegistry.openVSXURL` field of the CheCluster CR (see `devspaces/che-cluster.yaml`), so extensions install from `open-vsx.org`:

```yaml
spec:
  components:
    pluginRegistry:
      openVSXURL: https://open-vsx.org
```

Set `openVSXURL: ''` to switch back to the embedded curated registry — the editor queries exactly **one** Open VSX endpoint at a time. In the disconnected environment, `open-vsx.org` traffic goes through the Squid proxy, which already allowlists it. Caveats:

- Extension installs are fetched by the **workspaces**; their proxy env must cover `open-vsx.org` — the [Squid allowlist](#squid-proxy-deployment-optional-disconnected-environments) does.
- The [factory devfile fetch](#squid-proxy-deployment-optional-disconnected-environments) additionally needs `raw.githubusercontent.com`.
- For full catalog control (strictly air-gapped), deploy a standalone Open VSX registry in-cluster via `spec.components.openVSXRegistry.enable: true` and publish extensions manually from `.vsix` files.

#### Devfile registry

The devfile registry serves the **sample devfiles list** on Dashboard → `Create Workspace`. A registry is a simple static HTTP server: an index JSON listing the samples (name, display name, description, tags, icon, and a `links.v2` URL to the devfile) plus the devfiles themselves.

This PoC runs one as an **Apache HTTP Server container** serving static content from a ConfigMap:

- Manifests: `devspaces/devfile-registry.yaml` — a `ubi9/httpd-24` Deployment, a Service, and an edge Route.
- The content ConfigMap is **generated by kustomize** from the repo's real files via `devspaces/kustomization.yaml`:

```bash
oc apply -k devspaces/
```

The generator's inputs are `devspaces/devfile.yaml` (the sample devfile — the registry serves the actual file, not a copy) and `devspaces/registry-index.json` (the sample index).

It serves `<url>/devfiles/index.json` and the sample devfile at `<url>/devfiles/mta-migration-workspace/devfile.yaml`. Layout details that matter, verified from the Dev Spaces dashboard source:

- The dashboard first fetches `<url>/index/all`, falling back to the classic `<url>/devfiles/index.json` — a plain httpd 404 on the first is sufficient (both locations are tried; only the last failure is fatal).
- Index entries must have `displayName`, `icon`, `links`, and `tags` — all four are validated. The `icon` can be an inline data URI (`{"mediatype": ..., "base64data": ...}`).
- Relative `links.v2` paths are resolved against the registry base URL, so `devfiles/<name>/devfile.yaml` works without hardcoding the route host.
- Index and devfile content are fetched **server-side** via the dashboard backend's data-resolver proxy (`POST /dashboard/api/data/resolver`) — the registry needs **no CORS headers**.
- The `ubi9/httpd-24` image is used because the cluster's restricted SCC runs containers as an arbitrary non-root UID, which cannot bind port 80 — the image listens on 8080 natively.

Register the registry cluster-wide via the `devfileRegistry.externalDevfileRegistries` field of the CheCluster CR (see `devspaces/che-cluster.yaml`):

```yaml
spec:
  components:
    devfileRegistry:
      externalDevfileRegistries:
        - url: https://devfile-registry-openshift-devspaces.${OPENSHIFT_APPS_DOMAIN}/
```

Two CheCluster v2 CRD pitfalls:

> Verified live: the CRD **prunes unknown fields silently**. The v1-era placements (`spec.devfileRegistry` at top level, `spec.devWorkspace.storageStrategy`) are dropped on `oc apply` **without an error**, leaving the setting unset — the registration only appears to work until the next reconciliation drops it. In the v2 schema, `devfileRegistry` and `devWorkspace` live under `spec.components`. Likewise, the per-user workspace PVC strategy moved: v1 `devWorkspace.storageStrategy` is gone, and the v2 field is `spec.devEnvironments.storage.pvcStrategy`:

```yaml
spec:
  devEnvironments:
    storage:
      pvcStrategy: per-user
```

Three notes on the sample's configuration:

- The internal registry stays enabled (`devfileRegistry.disableInternalRegistry` unset) — disabling it breaks the empty-workspace flow, which relies on the internal registry's `Empty`-tagged editor devfile.
- The sample's index entry is tagged **`Empty`** (alongside its descriptive tags) — the dashboard's `selectDefaultDevfile` picks the sample tagged `Empty` as the **default devfile** for workspaces created without one, so users get the MTA workspace devfile (UDI container, extensions, postStart setup, auto-clone) by default.
- Since the served sample devfile **is** `devspaces/devfile.yaml` (consumed directly by the kustomize generator), there is no copy to keep in sync: edit it and re-apply with `oc apply -k devspaces/` — the generated ConfigMap name carries a content hash (`devfile-registry-content-<hash>`), so kustomize rewrites the Deployment's volume reference and the pod restarts immediately, with no kubelet ConfigMap sync delay.

The `Empty` tag has a side effect: the dashboard forces **`perclick` (ephemeral) storage** for Empty-tagged samples:

- The devfile's `controller.devfile.io/storage-type: per-user` attribute overrides this at the DevWorkspace level, so workspaces keep persistent storage — required while the devfile carries the `Empty` tag.
- Registration takes effect immediately (the dashboard fetches the CheCluster per request); verify with the dashboard's data-resolver proxy:

```bash
oc exec -n openshift-devspaces deploy/devspaces-dashboard -- sh -c \
  'TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token); \
   curl -k -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
   -d "{\"url\": \"https://devfile-registry-openshift-devspaces.${OPENSHIFT_APPS_DOMAIN}/devfiles/index.json\"}" \
   http://localhost:8080/dashboard/api/data/resolver'
```

#### Proxy configuration

On OpenShift, the Dev Spaces Operator automatically uses the cluster-wide proxy configuration, but you can override it in the `CheCluster` CR. This is useful in restricted environments where workspaces need to reach GitHub or the Open VSX registry through a proxy.

This PoC configures the proxy at the CheCluster level (see `devspaces/che-cluster.yaml`) — the CheCluster is the **single source of truth**.

> Verified live: the operator renders `spec.components.cheServer.proxy` as Java system properties (`-Dhttp.proxyHost=...`) into the `che` ConfigMap consumed by the che-server pod, and che-server **syncs the proxy environment variables into the `che-user-settings` ConfigMap** of each workspace namespace, which is `envFrom`-mounted into every workspace container. Proxy entries set directly in `che-user-settings` are overwritten by the reconcile (deleting the proxy keys re-adds them within seconds, with the CheCluster `nonProxyHosts` merged into `NO_PROXY`). Changing the CheCluster `proxy` rolls the che-server, dashboard, and gateway pods automatically; running workspaces pick up the new env on their next restart.

The proxy configuration below (and in the [Gitea](#gitea-installation) and [MTA](#proxy-settings-for-disconnected-environments-optional) proxy sections) uses the standard HTTP proxy environment variables, exported with an `MTA_` prefix to avoid conflicts with your shell's own proxy settings. Export them if not already defined. If the [Squid proxy](#squid-proxy-deployment-optional-disconnected-environments) is deployed on the cluster, point the proxy URL at the in-cluster service:

```bash
export MTA_HTTP_PROXY=http://squid.squid.svc:3128
export MTA_HTTPS_PROXY=http://squid.squid.svc:3128
export MTA_NO_PROXY=localhost,127.0.0.1,.svc,.cluster.local,.${OPENSHIFT_APPS_DOMAIN}
```

If the proxy requires authentication, create a Secret in the `openshift-devspaces` namespace with the credentials. It must carry the `app.kubernetes.io/part-of=che.eclipse.org` label:

```bash
oc apply -f - <<EOF
kind: Secret
apiVersion: v1
metadata:
  name: devspaces-proxy-credentials
  namespace: openshift-devspaces
  labels:
    app.kubernetes.io/part-of: che.eclipse.org
type: Opaque
stringData:
  user: <user>
  password: <password>
EOF
```

Configure the proxy in the `CheCluster` CR by deriving `url`, `port`, and `nonProxyHosts` from the environment variables with `jq`:

```bash
jq -n --arg httpProxy "$MTA_HTTP_PROXY" --arg noProxy "$MTA_NO_PROXY" \
  '($httpProxy | capture("^(?<scheme>[a-z]+://)(?<host>[^:/]+)(:(?<port>[0-9]+))?$")) as $p |
   {"spec":
      {"components":
        {"cheServer":
          {"proxy":
            ({"url": ($p.scheme + $p.host),
              "nonProxyHosts": ($noProxy | split(","))}
              + (if $p.port then {"port": $p.port} else {} end))}}}}' \
  | oc patch checluster/devspaces --namespace openshift-devspaces --type=merge -p "$(cat)"
```

If the proxy requires authentication, create the `devspaces-proxy-credentials` Secret above and add `"credentialsSecretName": "devspaces-proxy-credentials"` to the `proxy` object of the JSON payload.

The `url` is set to `<scheme>://<host>` from `MTA_HTTP_PROXY`; `port` is only set when the proxy URL includes one. `nonProxyHosts` is `MTA_NO_PROXY` split on commas — hosts reached directly without the proxy. The operator automatically adds `.svc` and the Kubernetes service hosts to this list.

To verify, start a workspace and check its containers' environment for `HTTP_PROXY`, `HTTPS_PROXY`, `http_proxy`, and `https_proxy`, plus `NO_PROXY`/`no_proxy` with the comma-separated non-proxy hosts. The proxy env is injected via `envFrom` (the `che-user-settings` ConfigMap), so it does **not** appear in the pod spec — verify from inside a container:

```bash
oc exec -n <workspace-namespace> deploy/<workspace-name> -c tools -- env | grep -iE 'proxy'
```

### MTA Plugin Setup

The MTA plugin is the [Migration Toolkit for Applications (MTA) Visual Studio Code extension](https://marketplace.visualstudio.com/items?itemName=redhat.mta-core). It lets you analyze, assess, and migrate source code from within the IDE. Full details: [Visual Studio Code extension guide](https://docs.redhat.com/en/documentation/migration_toolkit_for_applications/8.3/html/configuring_and_using_the_visual_studio_code_extension_for_mta).

- **Install the plugin in a workspace**: Start a workspace in Dev Spaces, open the `Extensions` view, search for `mta`, and install the **Migration Toolkit for Applications (MTA)** extension from the Open VSX registry.
- **Pre-install the plugin for all users**: devfiles can no longer declare IDE extensions — since devfile v2 (schema 2.2.0), `customizations.vscode.extensions` fails schema validation ("instance.components[0].container is not allowed to have the additional property customizations"), and a `.vscode/extensions.json` devfile attribute is **not honored** by che-code (a Che-Theia-era mechanism). The deterministic way is the `install-extensions` `postStart` command in the devfile below: it runs the workspace's **che-code CLI** (`/checode/checode-linux-libc/ubi9/bin/che-code --install-extension <ext-id>`, bundled Node libs on the `LD_LIBRARY_PATH`), installing immediately into `/checode/remote/extensions` on the per-user PVC (persisted across restarts), with no prompt and no editor reload:

    ```bash
    export LD_LIBRARY_PATH=/checode/checode-linux-libc/ubi9/ld_libs/openssl:/checode/checode-linux-libc/ubi9/ld_libs/core:/usr/lib64
    /checode/checode-linux-libc/ubi9/bin/che-code \
      --user-data-dir /checode/remote/data --extensions-dir /checode/remote/extensions \
      --install-extension redhat.mta-core --install-extension redhat.mta-java
    /checode/checode-linux-libc/ubi9/bin/che-code \
      --user-data-dir /checode/remote/data --extensions-dir /checode/remote/extensions \
      --install-extension redhat.fabric8-analytics
    /checode/checode-linux-libc/ubi9/bin/che-code \
      --user-data-dir /checode/remote/data --extensions-dir /checode/remote/extensions \
      --install-extension redhat.vscode-yaml --install-extension redhat.vscode-xml
    /checode/checode-linux-libc/ubi9/bin/che-code \
      --user-data-dir /checode/remote/data --extensions-dir /checode/remote/extensions \
      --install-extension sst-dev.opencode
    ```

    What gets installed:
    - **`redhat.mta-core`** (8.3.1) — analysis and migration features — and **`redhat.mta-java`** (8.3.1) — the Java analysis rules. Installed directly instead of the `redhat.mta-vscode-extension` pack, which bundles unneeded `mta-go`, `mta-javascript`, and `mta-csharp`.
    - **`redhat.fabric8-analytics`** (1.1.0) — the dependency-analytics panel (license and CVE checks against the quickstarts' Maven dependencies).
    - **`redhat.vscode-yaml`, `redhat.vscode-xml`** — the YAML and XML language servers (schema validation, hover, formatting). The quickstarts carry kustomize/OpenShift manifests and `pom.xml`/descriptor files.

    All IDs verified on the Open VSX registry (YAML/XML carry rolling date-based versions). `redhat.java` is already preinstalled in the UDI image.

    Fallback: a `.vscode/extensions.json` in the **repository cloned as the project source** makes the editor recommend the extensions at startup — a prompt scanned at editor startup only, so a file created by a `postStart` command takes effect on the next reload:

    ```json
    {
      "recommendations": [
        "redhat.mta-core",
        "redhat.mta-java",
        "redhat.fabric8-analytics",
        "redhat.vscode-yaml",
        "redhat.vscode-xml",
        "sst-dev.opencode"
      ]
    }
    ```

    This repository ships that file at `.vscode/extensions.json`. The IDs must exist on the Open VSX registry the gallery is served from (see [Extension marketplace configuration](#extension-marketplace-configuration)) — the embedded curated subset is small, hence the public registry here. Gallery *queries* go to `open-vsx.org`, but extension **downloads** (`.vsix`) come from the CDN host `openvsx.eclipsecontent.org` — both must be reachable (add both to the [Squid allowlist](#squid-proxy-deployment-optional-disconnected-environments) when disconnected), or every install fails with `Server returned 403`.

#### Create the LLM credentials Secret

The devfile expects the `OPENAI_*` variables from the [Prerequisites](#prerequisites) as environment variables in the workspace container. A ready-to-use template ships at `devspaces/llm-credentials-secret.yaml`. Create the Secret in the user's workspace namespace; the `controller.devfile.io/mount-to-devworkspace` label and `controller.devfile.io/mount-as: env` annotation mount it as environment variables into the workspace containers:

```bash
# admin-devspaces is the namespace of the workspace used in this PoC - for other
# users it is <username>-devspaces, created on their first workspace start
export WORKSPACE_NAMESPACE="admin-devspaces"
cat devspaces/llm-credentials-secret.yaml | sed "s/<workspace-namespace>/${WORKSPACE_NAMESPACE}/" | envsubst | oc apply -f -
```

The Secret must live in the namespace the workspaces run in. With `controller.devfile.io/watch-secret: 'true'`, changes are picked up when the workspace is restarted.

#### Create the clone-config Secret

The devfile's `clone-git` `postStart` command clones the application source (the `jboss/jboss-quickstarts` Gitea repository) into the workspace, using `PROJECT_REPO_URL` and `PROJECT_REPO_BRANCH` from this Secret. A ready-to-use template ships at `devspaces/git-clone-secret.yaml` — set `PROJECT_REPO_URL` to the SSH clone URL and `PROJECT_REPO_BRANCH` to the branch to clone/track — and create it like the [LLM credentials Secret](#create-the-llm-credentials-secret):

```bash
export PROJECT_REPO_URL="ssh://git@git-ssh.${OPENSHIFT_APPS_DOMAIN}:443/jboss/jboss-quickstarts.git"
export PROJECT_REPO_BRANCH="7.4.x"
export PROJECT_REPO_WEB_URL="https://git.${OPENSHIFT_APPS_DOMAIN}/jboss/jboss-quickstarts"
export PROJECT_REPO_PATH="helloworld"
cat devspaces/git-clone-secret.yaml | sed "s/<workspace-namespace>/${WORKSPACE_NAMESPACE}/" | envsubst | oc apply -f -
```

Constraints on these variables:

- The clone URL must use the `ssh://` form with port 443 (the TLS-passthrough Route, see [Git Authentication with SSH Keys](#git-authentication-with-ssh-keys)). Env variables expand in the `exec` `commandLine` because it runs via a shell — unlike `projects` remotes, which the DevWorkspace Operator resolves as literal arguments before any container exists.
- `PROJECT_REPO_BRANCH` matters: without it the clone lands on the repository's **default** branch — `8.1.x` for `jboss-quickstarts` — while the application records imported into MTA reference `7.4.x` (the [import template](#bulk-uploading-applications)). Analyzing a different working tree than the MTA records would compare inconsistent sources.
- Idempotent: it clones (at the configured branch) on first start and checks out/pulls on restarts (`/projects` persists on the per-user PVC).
- The clone authenticates via the mounted `git-ssh-key` and `git-ssh-proxy` Secrets (see [Git Authentication with SSH Keys](#git-authentication-with-ssh-keys)) — without them it fails with a public-key error and the workspace reports a warning.

> Verified live: the remote reads the **HTTPS web URL** (`PROJECT_REPO_WEB_URL`), not the SSH clone URL:
>
> - The profile sync matches hub records by URL; those records carry the HTTPS web route. With an SSH remote: `No Hub applications match repository URL`, no profiles download.
> - Fetches work anonymously over HTTPS; `clone-git` rewrites **pushes** to the authenticated SSH transport via `url.<ssh-base>.pushInsteadOf <web-base>`.
> - `pushInsteadOf` rewrites push URLs only, so the fetch URL the extension reads stays the HTTPS form.

`PROJECT_REPO_PATH` selects the folder the editor opens. The MTA extension supports a **single workspace folder** only (`workspaceFolders[0]`; it warns multi-root workspaces are unsupported), and the profile sync matches the folder's path relative to the repo root against each hub application's repository path (set per application by the [import template](#bulk-uploading-applications)). With the repo root open, the sync fails with `Multiple Hub applications found for this repository, but workspace is at repository root` and no hub profiles download — `clone-git` opens the application subfolder (here `helloworld`) instead.

> Verified live: after the clone/pull, the command adds that folder to the editor's workspace via the che-code remote CLI (`/checode/checode-linux-libc/ubi9/bin/remote-cli/code-oss --add <folder>`; talks to the running editor over `/tmp/vscode-ipc-*.sock` via `VSCODE_IPC_HOOK_CLI`; exits `0` on success, `1` when no editor is reachable; `--add` of an open folder is a no-op). The editor can take longer than 2 minutes to become reachable on a cold start, so the add retries in the background for up to ~5 minutes without blocking or failing the `postStart` event — a too-short loop leaves the folder out of the workspace. Fix: run the `--add` from a terminal or restart the workspace (the loop runs again on every start; the addition persists on the per-user PVC).

#### Create the hub-config Secret

The hub connection lives in VS Code secret storage — it cannot go into `settings.json`. Normally you set it through the **Configure Hub Settings** dialog of the Analysis View, but the extension reads **env vars at activation** that override the stored config (its `hubConfigStorage.ts`):

- Overrides: `HUB_URL`, `HUB_USERNAME`, `HUB_PASSWORD`, `FORCE_HUB_ENABLED`, `HUB_INSECURE`, `HUB_SOLUTION_SERVER_ENABLED`, `HUB_PROFILE_SYNC_ENABLED`.
- A ready-to-use template ships at `devspaces/hub-connection-secret.yaml`; it provides these to every workspace container.
- Result: the workspace connects to the Hub out of the box, with [analysis profiles](#define-an-analysis-profile) synced and the Solution Server (the Developer Lightspeed LLM proxy) reachable:

```bash
export HUB_URL="https://mta-openshift-mta.${OPENSHIFT_APPS_DOMAIN}"
export HUB_USERNAME="admin"
export HUB_PASSWORD="admin"
cat devspaces/hub-connection-secret.yaml | sed "s/<workspace-namespace>/${WORKSPACE_NAMESPACE}/" | envsubst | oc apply -f -
```

- `HUB_URL` is the MTA UI route (the extension appends the hub API paths itself); the credentials are the MTA UI login (the Keycloak `mta` realm).
- `HUB_INSECURE=true` is required — the route certificate is the cluster's self-signed CA.
- As with the other Secrets, changes are picked up on workspace restart (`controller.devfile.io/watch-secret: 'true'`). After a restart the Analysis View shows the hub as **Connected** with the logged-in user, plus Profile Sync and LLM proxy status.
- If the credentials are rotated (see the Keycloak notes), update the Secret the same way.

- **Create a workspace with the MTA plugin enabled**: The full devfile — including per-command comments explaining every mechanism — ships in this repository at [`devspaces/devfile.yaml`](devspaces/devfile.yaml). Its five `postStart` commands install the extensions, install and configure the OpenCode CLI, create the MTA plugin settings, and clone the application source:

    | Command | What it does |
    |---|---|
    | `install-extensions` | Installs the MTA core + Java analyzer, Dependency Analytics, YAML/XML, and OpenCode extensions via the che-code CLI (into the per-user PVC, persisted). |
    | `install-opencode` | Installs the OpenCode CLI via the upstream install script and puts it on the `PATH`. |
    | `configure-opencode` | Writes `~/.config/opencode/opencode.json` with the custom OpenAI-compatible provider (`OPENAI_BASE_URL`), reading only the API key from the environment. |
    | `configure-mta-settings` | Merges `mta-core.genai.enabled` / `mta-core.genai.agentMode` into the che-code machine-level `settings.json`. |
    | `clone-git` | Clones the application source (from `PROJECT_REPO_URL`/`PROJECT_REPO_BRANCH`) into `/projects` and adds the application subfolder to the editor workspace. |

    Dev Spaces only searches a repository's root for a `devfile.yaml` or `.devfile.yaml`, so pass the relative path via the `devfilePath` URL parameter. Create the workspace in the Dev Spaces dashboard via `Create Workspace` and the factory URL, or open it directly:

    ```bash
    echo "https://$(oc get route devspaces -n openshift-devspaces -o jsonpath='{.spec.host}')/#https://github.com/jcordes73/mta-use-cases?devfilePath=devspaces/devfile.yaml"
    ```

    Creating the workspace from this repository would normally clone it as the project — che-server's factory resolution injects it as the workspace's project because the devfile has no `projects` element. The devfile's `controller.devfile.io/project-clone: disable` attribute disables the DevWorkspace Operator's `project-clone` init container, so the use-case repository is **not** cloned; the application source (the `jboss/jboss-quickstarts` Gitea repository) is cloned exclusively by `clone-git` (see [Create the clone-config Secret](#create-the-clone-config-secret)).

    Because `project-clone` is disabled, cloning always comes from `clone-git` — the `remotes=` URL parameter only changes which repository is **declared** as the workspace's project, not what is actually cloned. Still, `remotes=` can clone the application source instead: when one of the additional remotes is named `origin`, the linked repository is **not** cloned, and the `origin` remote is cloned with its default branch checked out. The devfile is still located via `devfilePath` in the linked repository:

    ```bash
    echo "https://$(oc get route devspaces -n openshift-devspaces -o jsonpath='{.spec.host}')/#https://github.com/jcordes73/mta-use-cases?devfilePath=devspaces/devfile.yaml&remotes={{origin,https://$(oc get route gitea -n gitea -o jsonpath='{.spec.host}')/jboss/jboss-quickstarts.git}}"
    ```

    > ⚠️ NOTE
    >
    > With this variant the cloned project is the Gitea repository, which lacks this repository's `.vscode/extensions.json` — only the startup recommendation prompt is lost; the extensions install via `install-extensions`, independent of the cloned repository. Private Gitea repositories still require Git credentials in the workspace for the project clone to succeed — SSH keys (see [Git Authentication with SSH Keys](#git-authentication-with-ssh-keys)) or an HTTPS credentials secret mounted the same way.

- **Run an analysis**: Clone the application source (e.g. from the Gitea instance) into the workspace, create a run configuration, and run an analysis. The results, issue descriptions, and suggested code changes appear inline in the editor, and AI-assisted remediation (Developer Lightspeed) can resolve the issues directly. Select the analysis profile created in [Define an Analysis Profile](#define-an-analysis-profile) in the run configuration — without a profile the analysis runs with no rule selection and does not surface the EAP 8 findings, which the agentic workflow's plan stage builds the migration plan from.

### Git Authentication with SSH Keys


SSH keys are not carried in the devfile — they are provisioned as secrets in the workspace namespace, and the DevWorkspace Operator injects them into the project-clone container and all workspace containers. Two secrets are required:

- **`git-ssh-key`** — the private key (`dwo_ssh_key`), the public key (`dwo_ssh_key.pub`), and an explicit `ssh_config` key carrying the `IdentityFile`. The Dev Spaces server's `SshConfigConfigurator` is *supposed* to auto-generate that key (`Host *` + `IdentityFile /etc/ssh/dwo_ssh_key` + `StrictHostKeyChecking = no` + `Include /etc/ssh/ssh_config.d/*.conf`), but it does not run in this setup: the Secret keeps only the keys it was created with, `/etc/ssh/ssh_config` stays the stock file, and SSH then offers only the default identity keys (`~/.ssh/id_*`), failing every clone with `Permission denied (publickey)` even though the tunnel, `ProxyCommand`, and Gitea key registration are all fine. Ship the `ssh_config` in the Secret explicitly (the same content the configurator would generate — but do **not** put a `ProxyCommand` here, the `git-ssh-proxy` Secret owns that; see below). Create the key Secret in the workspace namespace (e.g. `admin-devspaces`):

```bash
oc create secret -n admin-devspaces generic git-ssh-key \
  --from-file=dwo_ssh_key="$SSH_KEY" \
  --from-file=dwo_ssh_key.pub="$SSH_PUB_KEY"

oc patch secret -n admin-devspaces git-ssh-key --type merge -p '{
  "stringData": {
    "ssh_config": "Host *\n  IdentityFile /etc/ssh/dwo_ssh_key\n  StrictHostKeyChecking no\n  Include /etc/ssh/ssh_config.d/*.conf\n"
  },
  "metadata": {
    "labels": {
      "controller.devfile.io/mount-to-devworkspace": "true",
      "controller.devfile.io/watch-secret": "true"
    },
    "annotations": {
      "controller.devfile.io/mount-path": "/etc/ssh/",
      "controller.devfile.io/mount-as": "subpath"
    }
  }
}'
```

The `watch-secret` label propagates the change into running workspaces (which restart to pick up the new mount). Verify inside the workspace: `ssh -G git-ssh.${OPENSHIFT_APPS_DOMAIN} | grep identityfile` should show `identityfile /etc/ssh/dwo_ssh_key` alongside the defaults. A workspace started before the Secret existed or was patched fails its clone once with the public-key error; restarting the workspace (or re-running `clone-git` from a terminal) completes it.

- **`git-ssh-proxy`** — the `ProxyCommand`, mounted as a config file into `/etc/ssh/ssh_config.d/` and included by the `ssh_config` above. A ready-to-use template ships at `devspaces/git-ssh-proxy-secret.yaml`:

```bash
cat devspaces/git-ssh-proxy-secret.yaml | sed "s/<workspace-namespace>/${WORKSPACE_NAMESPACE}/" | envsubst | oc apply -f -
```

The file is scoped with `Host git-ssh.*`, so other SSH hosts are unaffected. `controller.devfile.io/mount-to-devworkspace` mounts both secrets into every DevWorkspace in the namespace — creating `/etc/ssh/dwo_ssh_key`, `/etc/ssh/dwo_ssh_key.pub`, and `/etc/ssh/ssh_config.d/99-git-proxy.conf` (the image's own `50-redhat.conf` remains alongside) — and the generated `/etc/ssh/ssh_config` includes the directory, so git and the project-clone container use the key and the `ProxyCommand` automatically. `watch-secret` updates the mounts when the secrets change; new automount secrets apply on workspace restart.

The SSH URL is consumed by `clone-git` (via `PROJECT_REPO_URL` from the clone-config Secret, see [Create the clone-config Secret](#create-the-clone-config-secret)) or by the `remotes=` factory URL parameter — this devfile has no `projects` element (see the note on why below). `remotes=` accepts SSH URLs directly: the dashboard builds the project from the SSH URL (the `FactoryLocationAdapter` handles `ssh://` locations), and the project-clone container clones it with the mounted secrets — the mounts apply to the clone container too.

The Gitea SSH route is a **TLS-passthrough route backed by a TLS tunnel**: a `gitea-ssh-tunnel` deployment (socat with an `OPENSSL-LISTEN:8443` listener) forwards TLS-wrapped traffic to Gitea's plain SSH port 2222. TLS is required because passthrough routes route by TLS SNI, which plain SSH does not send — a plain `ssh -p 443 git@git-ssh...` is closed immediately (the tunnel expects a TLS handshake). `99-git-proxy.conf` carries a `ProxyCommand` (`openssl s_client -quiet`, in the UDI image) that unwraps TLS before speaking SSH. Since the scp-like `git@host:path` syntax cannot carry a port, the `ssh://` form with port 443 is required — otherwise `%p` expands to port 22 and the TLS client fails with `wrong version number`:

```bash
export PROJECT_REPO_URL="ssh://git@git-ssh.${OPENSHIFT_APPS_DOMAIN}:443/jboss/jboss-quickstarts.git"
```

The username is pinned to `git` (Gitea's SSH username convention); add the public key to the Gitea user's `SSH / GPG Keys` settings.

Notes:

- `StrictHostKeyChecking = no` (from the che-server-generated `ssh_config`) disables host-key verification — acceptable for a PoC. For production, add Gitea's host key to a `known_hosts` file in the secret referenced via `UserKnownHostsFile /etc/ssh/known_hosts`.
- Passphrase-protected keys are an experimental DevWorkspace Operator feature: `enableExperimentalFeatures: true` in the DevWorkspaceOperatorConfig, a `passphrase` key in the secret, and an operator-added `postStart` event that starts the ssh-agent and exports `SSH_AGENT_PID`/`SSH_AUTH_SOCK`.
- The Hub's `git-ssh-credentials` identity (id 3) is the *Hub-side* SSH credential for agentic clones via the Solution Server — separate from the mounted workspace secret above; both are needed when the MTA agentic workflow clones the repository.
- The Gitea SSH route is on the cluster's apps domain, covered by Dev Spaces' `nonProxyHosts` — no Squid allowlist change is needed for SSH git access.
- **Why a `postStart` command instead of a `projects` element**: the DevWorkspace Operator resolves `projects` git URLs as literal arguments in a dedicated init container that runs before any workspace container exists — no env expansion applies (Kubernetes `$(VAR)` expansion only applies to `env:` values in the same `env:` list, never to command arguments, and no user secret is env-mounted into the clone container). `clone-git` runs inside the workspace container, where the secret-injected `PROJECT_REPO_URL` is expanded by the shell. This also keeps the devfile cluster-independent: the clone URL is set per cluster by re-creating the clone-config Secret, not by editing the committed devfile. And don't replace `project-clone: disable` with a `postStart` `rm -rf` of the injected use-case repository: the init container re-clones it on every restart before postStart commands run (DWO runs `project-clone` first for exactly this reason, see devworkspace-operator#1454).

At workspace start, `install-extensions` installs the MTA extension from Open VSX — verify via `Extensions: Show Installed Extensions` (or `/checode/checode-linux-libc/ubi9/bin/che-code --list-extensions` in the terminal); the **Migration Toolkit for Applications (MTA)** extension should be listed. The `sst-dev.opencode` extension additionally provides the [OpenCode](https://opencode.ai/v2/docs/) AI coding agent in the IDE. The devfile:
- relies on the `OPENAI_*` variables provided by the mounted [llm-credentials Secret](#create-the-llm-credentials-secret),
- installs the extensions and the OpenCode CLI via `postStart` commands,
- configures `~/.config/opencode/opencode.json` with a custom OpenAI-compatible provider pointing at the same LLM endpoint used by the [MTA UI](#automated-migration-using-the-mta-ui) part,
- creates the MTA plugin settings via a third `postStart` command (see below) — the repository's `.vscode/extensions.json` additionally makes the editor recommend both extensions at startup — and
- clones the application source from the Gitea instance via `clone-git`, using `PROJECT_REPO_URL` from the mounted clone-config Secret.

The MTA extension settings (see below) are IDE-specific configuration, written by `configure-mta-settings` into the che-code **machine-level settings file** (`/checode/remote/data/Machine/settings.json`). The file applies to all workspace folders, persists on the per-user PVC, and is merged with the existing settings (e.g. `git.defaultCloneDirectory`, set by Dev Spaces) using `jq` (in the UDI image), so it is idempotent. Being editor-level rather than repo-level, this also works when the workspace clones a different repository than the one hosting the devfile (see the [`remotes=` factory URL variant](#mta-plugin-setup)):

The settings pre-configure the plugin's AI-assisted remediation (the setting IDs were verified against `redhat.mta-core` 8.3.1):

```json
{
  "mta-core.genai.enabled": true,
  "mta-core.genai.agentMode": true
}
```

- `mta-core.genai.enabled` enables the GenAI functionality (Solution Server) of the extension.
- `mta-core.genai.agentMode` enables the experimental agentic flow for solutions, which requires automatic analysis (`mta-core.analysis.analyzeOnSave`, enabled by default).

No LLM credentials are configured in the extension: with the [Hub connection](#configure-the-connection-to-the-mta-hub) enabled, GenAI requests go through the Solution Server's LLM proxy and the API key stays on the cluster (see the note under the Hub connection step). The direct-LLM configuration (`provider-settings.yaml`, only consulted for analyses run with local profiles without a Hub connection) is deliberately not written — the devfile instead removes any stale copy from earlier revisions, so a local-profile analysis cannot silently fall back to a leftover direct-LLM configuration.

> ⚠️ NOTE
>
> The command runs as a `postStart` event, after the workspace (and editor) has started. VS Code applies settings-file changes live, so the MTA extension reads them in the first session; if a setting is not picked up, reload the editor window. Alternatively, define the same settings via a `.vscode/settings.json` file in the cloned repository (workspace-level settings, applied on clone).

> ℹ️ NOTE
>
> **OpenCode CLI on the PATH**: no devfile `env` configuration needed. The installer (`install-opencode` command) installs the binary to `~/.opencode/bin` and appends `export PATH=$HOME/.opencode/bin:$PATH` to the shell config file it detects via `$SHELL` (typically `~/.bashrc`, idempotent via an exact-line guard; on the per-user PVC, so it persists across restarts). After the first start, verify the `# opencode` line is present in `~/.bashrc` — if the exec context's `$SHELL` is not `bash`, the installer targets a different config file (or none if unwritable); add the export line to `~/.bashrc` manually or extend `install-opencode`. Adding PATH via the devfile is not possible cleanly: Kubernetes `$(VAR_NAME)` expansion in container `env` values only resolves variables defined earlier in the same `env:` list — never image-provided variables like `PATH` — so `value: "...:$(PATH)"` would replace the image's PATH with a literal, unexpanded string and break the tools on it.

> ⚠️ NOTE
>
> The `sst-dev.opencode` extension is not wired into the MTA plugin via settings — the Red Hat build does not provide an ACP agent backend setting. It is a separate AI coding assistant in the IDE, using the LLM configured via the `opencode.json` above. The setting IDs can also be reviewed and changed via `Extensions` > `Migration Toolkit for Applications - Core` > `Settings`.

> ⚠️ NOTE
>
> In a disconnected environment, Dev Spaces cannot reach the public Open VSX registry. Either route the workspace traffic through a proxy (see [Proxy configuration](#proxy-configuration)) or mirror the extension into your own Open VSX registry and point the `CheCluster` at it via `spec.components.pluginRegistry.openVSXURL`.

<a id="configure-the-connection-to-the-mta-hub"></a>
- **Configure the connection to the MTA Hub**: To reuse the MTA Hub deployed as part of the [automated migration](#automated-migration-using-the-mta-ui) part, open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`), run the `MTA: Konveyor Analysis View` command, click the settings icon and select **Configure Hub Settings**:
- Toggle on **Enable connection to MTA Hub**.
- Enter the MTA Hub URL (the `${MTA_URL}` from the [automated migration](#automated-migration-using-the-mta-ui) part) in the **Hub URL** field.
- Enable **Skip SSL certificate verification** if you want to connect to the Hub by using an insecure connection.
- Enable the authentication option and select the method. The plugin supports **basic auth** and **OIDC** (the latter being the plugin's default, `mta-core.hub.auth.method`). This PoC does not configure an OIDC client on the Hub, so use **basic auth** with the Hub credentials `admin`/`admin`.
- Click **Save**, then click **Sign In** in the Authentication box.

After signing in, the Authentication box shows the status as Connected plus the status for **LLM proxy**, **Solution Server**, and **Profile Sync**. The MTA plugin never connects to the LLM directly — the LLM is configured centrally on the cluster via the Konveyor Gateway and reached through the Solution Server's LLM proxy, so a connected **LLM proxy** status confirms the plugin can access the LLM.

> ⚠️ NOTE
>
> The Hub URL and credentials are stored in the extension's own state and cannot be pre-seeded via the devfile — configuration and sign-in are per user and workspace.


### Local Installation (Desktop VS Code Without Dev Spaces)

The MTA plugin is a plain VS Code extension — same experience, desktop VS Code:

- Runs locally: the editor and the **analysis** (the extension drives the MTA analyzer RPC binary in containerless mode for Java — no Podman/Docker needed).
- Still cluster-backed (the [automated migration](#automated-migration-using-the-mta-ui) deployment): the Hub connection, Hub-synced analysis profiles, and the Solution Server (the LLM proxy behind Developer Lightspeed).
- Nothing extra is deployed on the cluster.
- Dev Spaces mechanics don't apply (devfile, postStart commands, mounted Secrets, che-code CLI paths) — your shell, `~/.config`, and VS Code `settings.json` take their place.

**Prerequisites** (per the [Visual Studio Code extension guide](https://docs.redhat.com/en/documentation/migration_toolkit_for_applications/8.3/html/configuring_and_using_the_visual_studio_code_extension_for_mta)):

- **JDK 17 or later** with `JAVA_HOME` set (the containerless analyzer runs the Java analysis in-process; only non-Java languages need a container runtime). The analysis wants **8 GB of RAM** available.
- **Visual Studio Code** (desktop) with network access to the cluster's Routes: the MTA UI route (Hub, profiles, Solution Server) and the Gitea routes (source clone/push). All Routes present the cluster's self-signed certificate, which matters for the Hub setting below.
- The `OPENAI_*` variables from the [Prerequisites](#prerequisites) exported in the shell that launches VS Code — only needed if OpenCode is used; the MTA plugin itself needs no LLM credentials (see the note under the Hub connection step of the [Dev Spaces flow](#mta-plugin-setup)).

**Install the extensions**: the extension IDs are the same ones the devfile installs — from the VS Code Marketplace (or the [Open VSX registry](https://open-vsx.org), which desktop VS Code can be pointed at via `extensions.experimental.useOpenVSXRegistry` if Marketplace access is restricted):

```bash
code --install-extension redhat.mta-core --install-extension redhat.mta-java
code --install-extension redhat.fabric8-analytics
code --install-extension sst-dev.opencode   # optional: the OpenCode AI agent in the IDE
```

First activation: the MTA extension downloads the latest **analyzer RPC binary** from the Red Hat Developer portal.

- If that link is unreachable (restricted network): download the MTA 8.3 analyzer RPC manually, configure its path (below), then reload the window.
- Path config: the **Analyzer Path** field of the extension settings or the `mta-vscode-extension.analyzerPath` setting (the `MTA: Override Analyzer Binary` Command Palette entry does the same).

**Configure the plugin settings**: write the same two settings the devfile creates into the user-level `settings.json` (they are editor-level, not workspace-level, so a repository `.vscode/settings.json` works equally well):

```json
{
  "mta-core.genai.enabled": true,
  "mta-core.genai.agentMode": true
}
```

**Configure the connection to the MTA Hub**: identical to the Dev Spaces flow — Command Palette → `MTA: Konveyor Analysis View` → settings icon → **Configure Hub Settings** → enable the Hub, enter the MTA UI URL (`${MTA_URL}`), enable **Skip SSL certificate verification** (the Route certificate is the cluster's self-signed CA), enable authentication with the `admin`/`admin` credentials, and enable **Solution Server** and **Profile Sync**. The environment-variable overrides the extension reads at activation (`HUB_URL`, `HUB_USERNAME`, `HUB_PASSWORD`, `HUB_INSECURE`, …) work on the desktop as well — export them in the shell that launches VS Code to skip the dialog entirely.

**Clone the application source**: the Hub profile-sync matches the workspace repository against the Hub's application records by git remote URL, with the same constraints as in Dev Spaces — the fetch remote must be the **HTTPS web URL** the records carry (`https://git.${OPENSHIFT_APPS_DOMAIN}/jboss/jboss-quickstarts`), and the opened folder must be the **application subfolder** (`helloworld`), since the extension supports a single workspace folder and matches its path relative to the repository root against the application's repository path. Two workstation-friendly options:

- **HTTPS basic auth** (simplest): `git clone https://jboss-dev%40example.com:jboss-dev@git.${OPENSHIFT_APPS_DOMAIN}/jboss/jboss-quickstarts` — the `jboss-dev` service account's password works over git HTTP even though the web UI's password login is disabled (SSO-only, see the [Gitea SSO configuration](#gitea-installation)). Fetches and pushes both work, and the remote URL matches the Hub records.
- **SSH** (per the [SSH clone test](#ssh-keys), the Route is TLS-passthrough and needs the `openssl` `ProxyCommand`): clone via SSH, then keep the fetch remote in the HTTPS form and rewrite only pushes, exactly like the devfile's `clone-git` does with `pushInsteadOf`:

  ```bash
  GIT_SSH_COMMAND="ssh -i ~/.ssh/mta-gitea -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ProxyCommand='openssl s_client -quiet -connect %h:443 -servername %h'" \
    git clone git@git-ssh.${OPENSHIFT_APPS_DOMAIN}:jboss/jboss-quickstarts ~/jboss-quickstarts
  cd ~/jboss-quickstarts
  git remote set-url origin "https://git.${OPENSHIFT_APPS_DOMAIN}/jboss/jboss-quickstarts"
  git config url."git@git-ssh.${OPENSHIFT_APPS_DOMAIN}:jboss/jboss-quickstarts".pushInsteadOf \
    "https://git.${OPENSHIFT_APPS_DOMAIN}/jboss/jboss-quickstarts"
  ```

  With an SSH fetch remote the profile sync logs `No Hub applications match repository URL` and no profiles download (verified live in the Dev Spaces setup) — the `set-url` above avoids that while pushes still traverse the SSH route with the key.

Then open the application subfolder (e.g. `~/jboss-quickstarts/helloworld`) as the workspace folder — `File → Open Folder` — so the profile sync resolves to the `helloworld` application record; opening the repository root fails the sync with `Multiple Hub applications found for this repository`.

**Run an analysis and remediate**: select the Hub-synced profile in the run configuration and run the analysis — same as in Dev Spaces, results appear inline and **Developer Lightspeed** proposes resolutions through the Solution Server. Optionally configure [OpenCode](https://opencode.ai/v2/docs/) as a separate AI agent in the IDE: install the CLI with `curl -fsSL https://opencode.ai/install | bash` and write the same `~/.config/opencode/opencode.json` the devfile's `configure-opencode` command generates (same field-name caveats — `npm`, `options.baseURL`, top-level `provider`), with `baseURL`/model values expanded from the `OPENAI_*` variables and the API key read from the environment via `"env": ["OPENAI_API_KEY"]`.

## Appendix

### Create your own skills

Want to create your own skills? Here's how. To wire a finished skill into a workflow (SkillCard, Agent, AgentWorkflow), see [Assemble the Agentic Workflow components](#assemble-the-agentic-workflow-components).

First, install the `migrations-skills` skill, which helps you create a skill from a migration guide:

```bash
npx skills add https://github.com/savitharaghunathan/migration-skills
```
You'll be asked a few questions:

- Select `generate-migration-skill`, press <space>, then <enter> twice
- Select `global` as `Installation scope`, then press <enter>
- Select `yes` to proceed, press <enter>
- Select `no` for installing the `find-skills` skill

The skill is written to `.agents/skills/generate-migration-skill`.

**Claude Code** users: copy it to `~/.claude/skills`:

```bash
cp -rf ~/.agents/skills/generate-migration-skill/ ~/.claude/skills/
```
Generate a skill from a migration guide contained in `README.md`:

```
/generate-migration-skill README.md
```
The skill is written to the current directory with this structure:

```
.
├── docs
│   └── superpowers
│       ├── plans
│       │   └── 2026-10-22-ant-to-maven-migration-guide.md
│       └── specs
│           └── 2026-10-22-ant-to-maven-migration-guide-design.md
├── README.md
├── skills
│   └── java
│       └── ant-to-maven
│           ├── guide.md
│           ├── modules
│           │   ├── additional.md
│           │   ├── build-system.md
│           │   ├── cleanup.md
│           │   ├── code.md
│           │   ├── config.md
│           │   └── testing.md
│           ├── references
│           │   ├── api-map.md
│           │   ├── config-map.md
│           │   ├── dependency-map.md
│           │   ├── pattern-map.md
│           │   ├── sources.md
│           │   └── verify-errors.md
│           └── SKILL.md
└── tools
    └── check_snippets.py
```

The `README.md` and resulting skill from this example live at https://github.com/jcordes73/ant-to-maven. See [Apache Ant to Apache Maven](#apache-ant-to-apache-maven) for how to build an Agentic Workflow with this skill.

- **Author the skill**: Create a directory with a `SKILL.md` containing YAML frontmatter `name` and `description`:

    ```
    my-skill/
    ├── SKILL.md
    ├── references/       optional supporting material
    ├── scripts/          optional executable helpers
    └── assets/           optional templates or assets
    ```

    ```markdown
    ---
    name: maven-migration
    description: Migrates Maven POM files from Java EE to Jakarta EE.
    ---

    # Maven Migration

    Inspect every POM, replace `javax.*` dependencies with Jakarta equivalents,
    and run the project's build to verify the result.
    ```

    The frontmatter field set is closed — don't add `type` to `SKILL.md`. The load policy (`skill` = on-demand vs. `rule` = always-loaded in every prompt) lives on the `SkillCard` CR, not the content.

- **Login into registry.redhat.io**: Building the `SkillCard` requires a ```Service Account``` login. See [Red Hat Container Registry Authentication](https://access.redhat.com/RegistryAuthentication) for how to create one.
    ```bash
    podman login -u <service-account-username> registry.redhat.io
    ```
- **Package as an OCI image**: Single skill → `SKILL.md` at the image root; multiple skills → one directory per skill. Discovery is filesystem-based: a `SKILL.md` at the image root means one skill; otherwise each immediate subdirectory containing a `SKILL.md` is discovered as its own skill.

    ```bash
    podman build -t quay.io/telco-coe-emea/mta-jboss-eap-7-to-8-skills:8.3.1 -f mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/skills/jboss-eap-7-to-8/Containerfile mta-ui/agentic-workflow/jboss-eap-7-to-8/agents/skills/jboss-eap-7-to-8
    podman tag quay.io/telco-coe-emea/mta-jboss-eap-7-to-8-skills:8.3.1 quay.io/telco-coe-emea/mta-jboss-eap-7-to-8-skills:latest
    podman push quay.io/telco-coe-emea/mta-jboss-eap-7-to-8-skills:8.3.1 quay.io/telco-coe-emea/mta-jboss-eap-7-to-8-skills:8.3.1    
    ```
    Verify the content:

    ```bash
    podman run -d -ti --replace --name skillcard --entrypoint='["/bin/bash","-c","while true; do sleep 30; done"]' quay.io/telco-coe-emea/mta-jboss-eap-7-to-8-skills:latest
    podman exec skillcard ls /skill
    ```

    Expected output:
    ```bash
    SKILL.md
    modules
    references
    ```
    Clean up:
    ```bash
    podman kill skillcard
    ```

### Create your own rules

Skills guide agents, but the analysis findings come from **rules** — YAML files processed by the [analyzer-lsp](https://github.com/konveyor/analyzer-lsp) engine. The [konveyor/rulesets](https://github.com/konveyor/rulesets) repository (source of the EAP 8, Jakarta EE, and OpenJDK rulesets in this PoC) maintains an authoring workflow as skills under [`.skills`](https://github.com/konveyor/rulesets/tree/main/.skills) (`create-new-rule.md`, `run-rule-tests.md`, `add-or-update-tests.md`), with format references under `.cursor/rules/`.

This PoC ships two example rulesets in [`custom-rulesets/`](custom-rulesets/README.md):

- `eap8-custom` — the EAP 7 → EAP 8 path, with two rules covering both condition families: `builtin.filecontent` (hard-coded IPs in config files) and `java.dependency` (Commons Lang 2.x → commons-lang3), plus a Kantra test file and test data.
- [`openjdk17-custom/`](custom-rulesets/stable/java/openjdk17-custom/) — Oracle JDK 8 → OpenJDK 17 (JEP 320 removed Java EE modules, `sun.misc` internals, and legacy JVM flags), filling gaps the upstream `stable/java/openjdk17` ruleset leaves open.

```
custom-rulesets/stable/java/eap8-custom/
├── ruleset.yaml                       ruleset name + description
├── eap8-custom-example.yaml           the rules
└── tests/
    ├── eap8-custom-example.test.yaml  one test file per rule file, rulesPath → ../
    └── data/                          per-provider, per-test-case data folders
        ├── builtin/tc-hardcoded-ip/
        └── java/tc-commons-lang/      minimal Maven project for the java provider
```

Essential conventions, from the upstream skills:

- Rules live under `stable/<language>/<technology>/` (or `preview/...`), one directory per target technology with a `ruleset.yaml`; one rule file per concern.
- Each rule has a `ruleID` of the form `<ruleset-prefix>-<5-digit-number>`, incremented by 10 so rules can be inserted later, plus `category` (`mandatory`/`optional`/`potential`), `effort` (Fibonacci), `labels` (e.g. `konveyor.io/source=eap7`, `konveyor.io/target=eap8+`), a `description`, and a `message` with before/after examples and `links` — the message is what both the report and AI agents act on.
- Every rule file gets a sibling `tests/<basename>.test.yaml` declaring `rulesPath`, the `providers` (`java`, `builtin`) with their `dataPath`s, and per-rule test cases with `hasIncidents` expectations. Test data lives under `tests/data/<provider-dataPath>/<test-case-name>/`.

Run tests with [Kantra](https://github.com/konveyor/kantra), the same way upstream CI does:

```bash
kantra test custom-rulesets/stable/java/eap8-custom/tests/eap8-custom-example.test.yaml
```

Once validated, upload the ruleset in the MTA web console (*Migration → Rulesets*) and select it in an analysis profile via its labels (see [Define an Analysis Profile](#define-an-analysis-profile)). The example rules carry this PoC's `konveyor.io/source=eap7` / `konveyor.io/target=eap8+` labels, so they slot into the existing EAP 7 → 8 profile unchanged. See [custom-rulesets/README.md](custom-rulesets/README.md) for the full walkthrough.

### Suggested design: Agentic run observability and a persistent event log

The PoC's agentic architecture has two observability gaps: runs are invisible once no client is attached to the ACP stream, and nothing persists after the sandbox pod dies. The full design proposal — variant decision (Hub as recorder/broker), topology change, normalized event schema, PostgreSQL persistence, analytics, upstream alignment (#341/#192/#282), and six implementation phases — lives in [design/agentic-observability-event-log.md](design/agentic-observability-event-log.md).