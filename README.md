# Migration Toolkit for Applications Use-Cases

The [**Migration Toolkit for Applications**](https://developers.redhat.com/products/mta) (MTA) helps you to migrate your existing application. Its main use cases are:

1. **Legacy app assessment** — Static analysis of application source code and archives to inventory applications, identify technologies used, and estimate effort/cost of migration (sizing for modernization programs).
2. **Migration to newer platforms** — Moving Java applications from older application servers (WebLogic, WebSphere, JBoss EAP 4/5/6/7, Tomcat) to newer versions of JBoss EAP or other runtimes.
3. **Lift-and-shift to cloud / containers** — Migrating monoliths to OpenShift/Kubernetes, containerizing applications, and moving from on-premise to hybrid cloud.
4. **Java version upgrades** — Upgrading JDK versions and migrating from proprietary APIs to open standards (e.g., Java EE → Jakarta EE, Oracle JDK → OpenJDK).
5. **Monolith decomposition** — Identifying candidates for refactoring into microservices, highlighting hard-coded dependencies and configuration issues.
6. **Governance at scale** — Central reporting across a large application portfolio (wave planning), enabling organizations to plan, prioritize, and track modernization programs.

In practice MTA works in two modes: the **web console** (portfolio analysis, project/wave management) and the **CLI** (developer-driven, single-app analysis), producing detailed reports of issues, migration rules triggered, and suggested code changes. There's also IDE integration (VS Code) for inline remediation hints.

## Prerequisites

Below you can find the requirements for getting started with this PoC:

- **OpenShift CLI (oc)**: Ensure the `oc` command-line tool is installed and logged in to the target OpenShift cluster with `cluster-admin` privileges.
- **OpenShift Cluster Access**: An operational Red Hat OpenShift Container Platform cluster (v4.22 recommended) with sufficient resources for running OpenShift Dev Spaces and MTA workloads.
- **Subscription Requirements**: Ensure an active Red Hat Advanced Developer Suite (RHADS) subscription is available to access Developer Lightspeed services.
- **Storage & Registry Access**: A configured default StorageClass supporting dynamic persistent volume provisioning and access to the Red Hat Ecosystem Catalog/OperatorHub.
- **OpenAI-Compatible LLM Access**: Access to an OpenAI-compatible Large Language Model (LLM) endpoint and API key, required for Red Hat Developer Lightspeed and agentic migration features.
- **Define environment variables**: To ease the deployment of the Kubernetes resources later define these environment variables

```bash
export OPENAI_HOST=<Your LLM endoint host>
export OPENAI_BASE_URL=https://${OPENAI_HOST}/v1
export OPENAI_API_KEY="<Your API Key>"
export OPENAI_MODEL_NAME="<Your Model name>"
export STORAGECLASS_RWX=ocs-external-storagecluster-cephfs
export OPENSHIFT_APPS_DOMAIN=$(oc get ingresses.config.openshift.io cluster -o jsonpath='{.spec.domain}')
```

## Gitea installation

To prevent unwanted changes and/or commits to existing source code, the original repos will be cloned into a GitLab instance that will be running on OpenShift.

- **Gitea Helmchart**: To install the Gitea Helmchart run
    ```bash
    oc new-project gitea
    helm repo add gitea-charts https://dl.gitea.com/charts/
    helm repo update
    cat gitea/values.yaml | envsubst | helm install -f - gitea gitea-charts/gitea
    cat gitea/create-db.sh | oc exec statefulset/gitea-postgresql -n gitea -i -t -- bash -il
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment gitea -n gitea --timeout=600s
    ```
For any actions later we will need an access token

```bash
export GITEA_ADMIN_TOKEN=$(curl -H "Content-Type: application/json" -d '{"name":"admin_token","scopes":["all"]}' -u 'gitea-admin:gitea' "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/users/gitea-admin/tokens" | jq -r '.sha1')
```
Now we will clone the [JBoss EAP Quickstarts](https://github.com/jboss-developer/jboss-eap-quickstarts) repository into Gitea using the REST-API:

```bash
curl -s -X POST "https://gitea.${OPENSHIFT_APPS_DOMAIN}/api/v1/orgs" \
  -H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"username": "jboss", "visibility": "public"}'

curl -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/repos/migrate" \
  -H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clone_addr": "https://github.com/jboss-developer/jboss-eap-quickstarts",
    "repo_name": "jboss-quickstarts",
    "repo_owner": "jboss"
  }'

curl -s -X POST "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/admin/users" \
  -H "Authorization: token ${GITEA_ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jboss-dev",
    "email": "jboss-dev@example.com",
    "password": "jboss-dev",
    "must_change_password": false
  }'
export GITEA_OWNERS_TEAM_ID=$(curl -s "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/orgs/jboss/teams"   -H "Authorization: token ${GITEA_ADMIN_TOKEN}" | jq '.[].id'
curl -s -X PUT "https://git.${OPENSHIFT_APPS_DOMAIN}/api/v1/teams/${GITEA_OWNERS_TEAM_ID}/members/jboss-dev"   -H "Authorization: token ${GITEA_ADMIN_TOKEN}"
```

## Agent Sandbox Installation
For Agentic Migrations via MTA we need to have the **Agent Sandbox Operator** installed as the agents will be running in sandboxes:

    ```bash
    oc apply -f agent-sandbox/agent-sandbox-operator.yaml
    oc wait --for=jsonpath='{status.readyReplicas}'=1 deployment agent-sandbox-controller -n agent-sandbox-system
    ```

## Migration Toolkit for Applications (MTA) 8.3 Installation
MTA 8.3 provides automated analysis, assessment, and refactoring tooling for application modernization:
- **Prerequisites**: OpenShift Container Platform installed with cluster-admin privileges, along with persistent storage requirements including 1 RWO PV (10Gi) for the Hub database, 2 RWX PVs (100Gi each) for Maven cache and Hub bucket storage (if `rwx_supported` is enabled), and 1 RWO PV (5Gi) for the Developer Lightspeed database. You will also need access to a Large Language Model.
- **Deploy the Migration Toolkit for Applications Operator**:  Via the CLI by creating the target namespace, OperatorGroup, and Subscription resources:

    ```bash
    oc apply -f mta-ui/mta-operator.yaml
    oc wait --for=jsonpath='{.status.phase}'=Succeeded csv mta-operator.v8.3.2 -n openshift-mta --timeout=600s
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-operator -n openshift-mta --timeout=600s
    ```
- **Create the Model Secret**: Create a Kubernetes Secret containing the API key and Base URL required for the self-hosted model LLM provider. This is needed for the Solution Server that is a core component deployed into the cluster that acts as an MCP server, maintaining persistent memory of refactoring resolutions and interfacing with Large Language Models (LLMs) via Retrieval-Augmented Generation (RAG):
    ```bash
    cat mta-ui/mta-ls-llm-provider-secret.yaml | envsubst | oc apply -f -
    ```
- **Configure the Tackle Instance**: Create a Tackle instance specifying the amount of storage needed, enabling Developer Lightspeed and configuring the Large Language Model (LLM):

    ```bash
    cat mta-ui/tackle.yaml | envsubst | oc apply -f -
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-hub       -n openshift-mta --timeout=600s
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment mta-ui       -n openshift-mta --timeout=600s
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment kai-db       -n openshift-mta --timeout=600s
    oc wait --for=jsonpath='{.status.readyReplicas}'=1 deployment kai-api       -n openshift-mta --timeout=600s
    ```
- **Accessing the MTA UI**: The MTA URL can be accessed like this:

    ```bash
    echo "MTA URL: https://$(oc get route mta -n openshift-mta -o jsonpath='{.spec.host}')"
    ``` 
    You should be presented with a screen like this:

    ![image](images/mta_login.png)    

    Login with `admin/admin`.

- **Automation via REST API**: Using the Konveyor Tackle2-Hub API you can automate the configuration of the MTA.

    For this we first need to get the endpoint:
    ```bash
    export MTA_URL="https://$(oc get route mta -n openshift-mta -o jsonpath='{.spec.host}')"
    ```
    Then we can go ahead and create a stakeholder and assign him to the stakeholder group named **JBoss Quickstarts** and a business service named **JBoss Onboarding**.
    ```bash
    MTA_USER_ID=$(curl -s -X POST "${MTA_URL}/hub/stakeholders"   -u admin:admin   -H "Content-Type: application/json"   -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "jobFunction": {"id": 1, "name": "IT"}
  }' | jq '.id')
    
    curl -s -X POST "${MTA_URL}/hub/stakeholdergroups" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"JBoss EAP Quickstarts\",
    \"description\": \"Stakeholders owning migrated JBoss EAP Quickstarts applications\",
    \"stakeholders\": [
      {\"id\": ${MTA_USER_ID}, \"name\": \"John Doe\"}
    ]
  }"

  curl -s -X POST "${MTA_URL}/hub/businessservices" \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"JBoss Onboarding\",
    \"description\": \"Onboarding Developers onto JBoss\",
    \"owner\": {\"id\": ${MTA_USER_ID}, \"name\": \"John Doe\"}
  }"
  ```

## OpenShift Dev Spaces Installation

**OpenShift Dev Spaces** provides Kubernetes-based cloud development environments and is installed on the OpenShift Container Platform using the OpenShift Dev Spaces Operator via OperatorHub:

- **Prerequisites**: Ensure OpenShift Container Platform v4.22 is running, log in as a user with cluster-admin privileges, ensure a default StorageClass is configured, and verify adequate cluster CPU and memory resources.
- **Dev Spaces Operator Installation**: Deploy the OpenShift Dev Spaces Operator via the CLI by creating the target namespace, OperatorGroup, and Subscription resources:

```bash
oc apply -f devspaces/openshift-devspaces-operator.yaml
```