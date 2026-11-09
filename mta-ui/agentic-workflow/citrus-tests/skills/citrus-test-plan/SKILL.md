---
name: citrus-test-plan
description: Plans Citrus test authoring for a JEE application - inventories the application's testable JEE surface (JAX-RS resources, EJBs, servlets, CDI beans, consumers of external endpoints), selects the compatible Citrus version line from the application's Java baseline, consults the Citrus MCP server's catalogs and documentation, and writes a structured test plan (docs/citrus-test-plan.md) that the author and verify stages execute. Use when a test-authoring workflow needs a Citrus test plan.
---

# Citrus Test Planning

Plan the Citrus tests to add to a JEE application, using the Citrus MCP
server (`citrus` tools) as the authoritative reference for the framework's
test vocabulary. This is the **plan** stage of a plan → author → verify
workflow: you only plan — do not write tests or modify source files.

**Scope**: only the code indicated by the task - the git repository the
workspace was cloned from, restricted to the application path named in the
task instructions ($(workflow.path), empty = repository root). The workspace
may be a multi-application repository (e.g. the jboss-quickstarts monorepo
carries 123 applications) - never plan tests for directories outside the
application path.

## Workflow

### 1. Inventory the application

**Re-run check first**: if the application path already contains citrus
test work (a `docs/citrus-test-plan.md`, `*Test`/`*IT` classes using
Citrus imports, a `citrus-container-tests` profile) from a previous
run of this workflow that was merged into the branch you cloned, switch
to **extend mode**: reuse the existing plan's lane, Citrus version and
dependency decisions (do not redeclare the BOM or profile), inventory
only the *untested* elements, and add rows to the existing plan with a
"## Status (this run)" note. Never duplicate an existing suite.

Read the application's `pom.xml` (and parent poms) and the sources under the
application path. Record:

- the **Java baseline** (`maven.compiler.release` / `source` / `target`, or
  the parent's), and the test stack already present (JUnit 4/5, TestNG,
  Arquillian, failsafe `*IT` tests) - the plan must fit alongside it, not
  replace it;
- the **JEE surface**: Jakarta REST resources (`@Path` classes and their
  produced/consumed media types), EJBs, servlets, CDI beans, JMS
  producers/consumers, and any external endpoints the application itself
  calls (HTTP clients, JMS brokers);
- what runs **in-JVM** without a full application server (a REST resource
  class bootable in an embedded container, plain CDI beans) versus what
  needs a deployed container (EJBs with container services, MDBs, servlet
  filters).

Then match each inventoried element against what the framework can
cover. The `citrus_catalog_endpoints` tool is the ground truth for the
exact endpoint list - query it (with no filter) before finalizing the
classification.

**Validation status of this map**: the HTTP/REST rows (both lanes,
including the data-backed REST+JPA recipe), and the JMS row are
validated end-to-end. The remaining rows are derived from the live
catalog and Citrus's architecture - **catalog-derived, not yet
validated end-to-end**; when a plan relies on one, treat the author
stage's Maven build as the validator and record deviations in the
handoff so the row gets validated or corrected.

The verified shape of the coverage map:

- **Jakarta REST / any HTTP entry point** → Citrus `http` client; the
  application side is an in-JVM embedded deployment (in-JVM lane) or
  the container-provisioned EAP (container lane).
  - REST resources that are **data-backed** (inject CDI beans which
    reach JPA) are also in-JVM-coverable with the validated
    data-backed REST recipe (Weld SE bootstrap + test-scoped H2
    persistence unit replacing the container datasource, plus the
    request-context/transaction filter) — see the author skill's
    recipe for the exact pattern. Not in-JVM-coverable: resources
    that inject what no SE bootstrap can supply and no test alternative
    can reasonably replace (e.g. FacesContext, remote EJBs) — plan
    those for the container lane or mark out of scope with the reason.
- **Outbound HTTP calls the application makes** → Citrus `http-server`
  endpoint simulating the external service on localhost.
- **SOAP** → `soap-client` / `soap-server` (citrus-ws).
- **JMS producers/consumers** → `jms` endpoints (validated live:
  citrus-bom import, `citrus-junit-jupiter` + `citrus-jms` test-scoped,
  plus an embedded in-VM Artemis broker - `org.apache.activemq:artemis-jakarta-server`
  2.37.0 test-scoped, `EmbeddedActiveMQ` with an in-vm acceptor
  `vm://0`, auto-create address settings, endpoint via
  `JmsEndpoints.jms().asynchronous().connectionFactory(new
  ActiveMQJMSConnectionFactory("vm://0")).destination(name).timeout(...).build()`).
  DSL note verified live: the generic send/receive statics live on the
  **nested Builder classes**
  (`SendMessageAction.Builder.send` / `ReceiveMessageAction.Builder.receive`),
  unlike the http DSL's top-level `HttpActionBuilder.http`.
- **Kafka** → `kafka` endpoints; same broker-backed caveat, heavier -
  treat as possible-but-fragile under a time-boxed sandbox.
- **Mail** → `mail-server` simulation (citrus-mail); **FTP/SFTP/SCP**
  → `citrus-ftp` client+server endpoints; **WebSocket** →
  `citrus-websocket`; **GraphQL** → `citrus-graphql`; **JMX, RMI,
  SSH, Vert.x, Spring Integration channels, Camel routes** →
  dedicated catalog endpoints - all in-JVM simulation, no external
  infrastructure.
- **Plain CDI beans / POJOs** → testable in-JVM (Citrus `direct`
  endpoints if orchestration is wanted), but conventional unit tests
  are usually the better fit - say so explicitly in the plan rather
  than forcing Citrus.
- **EJBs with container services, MDBs, servlet filters** → not
  in-JVM-testable. The container lane can prove deployment-time
  behavior by driving the deployed application, but direct
  method-level testing with container-managed transactions/security
  is Arquillian territory - out of scope with that reason.
- **JPA/persistence** → not a Citrus target *per se*, but REST
  resources that persist through JPA ARE coverable in the in-JVM lane
  via the data-backed REST recipe (test persistence unit over H2,
  `@Alternative` producer replacing the container's `@PersistenceContext`
  EntityManager). The tests then validate the REST contract *and* the
  persistence wiring; container-managed transactions (CMT) remain a
  known limit (the filter supplies resource-local transactions).

Record the verdict per element in the inventory (coverable-in-JVM /
coverable-container-lane / better-as-unit-test / out-of-scope with
reason) - this becomes the plan's gap ledger.

### 2. Choose the Citrus version line

Before the version, choose the **lane** — record it in the plan as
`test_kind`, with the reason:

- **`test_kind: test` (default) — in-JVM lane.** Tests named `*Test`,
  run by surefire under plain `mvn test`; application code exercised
  in-JVM (embedded deployment). Works everywhere, runs in seconds, and
  is what the verify stage's JaCoCo command-line recipe expects. Choose
  it unless the container lane's trigger conditions apply.
- **`test_kind: it` — container lane.** Tests named `*IT`, run by
  failsafe under `mvn verify -Pcitrus-container-tests` against a real
  JBoss EAP server the build provisions from the EAP Galleon feature
  pack (`wildfly-maven-plugin` provision → start → deploy → failsafe →
  shutdown — the author stage owns the profile recipe). Choose it when
  the plan must prove **deployment-time behavior** the embedded lane
  cannot honor: container-managed wiring (the container's own JAX-RS
  scanning and `@ApplicationPath` handling, servlet mappings,
  datasources), or the user explicitly asked for in-container /
  integration tests. Constraints: the application must be deployable
  from its pom alone (WAR/JAR, no external services beyond the server);
  the build needs network access to the Red Hat Maven repositories; the
  run is slower (first provisioning of a server is minutes, not
  seconds). Elements needing infrastructure beyond a single EAP server
  (external brokers, cluster topology) remain out of scope with a
  reason — the lane provisions exactly one server.
- The lanes coexist: an application may end up with both `*Test`
  (in-JVM) and `*IT` (container) suites — plan them as separate
  inventories with separate verification commands.

Then choose the Citrus version line.

From the Java baseline:

- Java 17 or newer → **Citrus 5.x** (GA, e.g. `5.1.0`); the MCP server's
  default catalog matches this line, so catalog queries need no `version`
  argument.
- Java 8 or 11 → **Citrus 3.x** (last 3.4.x). Citrus 4.x/5.x require Java
  17 (Spring 6/7 baseline). The MCP catalog is only reliable for the
  server's own version - foreign `version` queries failed live (see
  step 3) - so ground the vocabulary in the default catalog and let the
  author stage's Maven build validate the actual target version.

Record the chosen version and the reason in the plan.

### 3. Consult the Citrus MCP server

Use the `citrus` MCP tools to ground every framework decision in the
catalog, not in your own recall:

- `citrus_catalog_endpoints` / `citrus_catalog_endpoint` - pick and detail
  the endpoint implementations the tests need (e.g. `http` client/server,
  `jms`, `kafka`); capture the endpoint property schemas you rely on.
- `citrus_catalog_actions` / `citrus_catalog_action` /
  `citrus_catalog_action_schema` - the test actions and containers the
  planned cases use (`send`, `receive`, `createVariables`, `echo`,
  iteration/conditional containers, ...).
- `citrus_docs_index` / `citrus_docs_page` - the framework documentation
  (runtime integration, endpoint configuration, test variables). The docs
  tools work on **file-name substrings**, verified live: `citrus_docs_index`
  returns a plain newline-separated list of `.adoc` file names (not JSON),
  and `citrus_docs_page` matches `kind` AND `searchKey` as substrings of
  those file names - e.g. `kind: "runtime"`, `searchKey: "junit"` resolves
  `runtimes-junit-jupiter.adoc` (the JUnit Jupiter runtime page). It returns
  the raw AsciiDoc in the `adoc` field of the response.

**Catalog version coverage**: the catalog and docs tools are only reliable
for the MCP server's own Citrus version (the one baked into the agent
image). The `version` argument exists but resolving foreign versions
failed live for both the 3.x and 4.x lines (`Failed to list endpoints:
argument "src" is null`). When the plan targets an older Citrus line, use
the default catalog for the vocabulary (the http/jms endpoint and
send/receive action DSL is stable across lines), cite the version
differences you know from the migration guides, and let the author
stage's Maven build be the actual validator.

Cite the consulted catalog entries in the plan (names + one-line purpose).

**MCP-outage fallback**: if the Citrus MCP server fails to start or a
tool call fails unrecoverably (server jar problems in the sandbox,
process crashes), do not halt the workflow: proceed using this skill's
own recipes as the framework vocabulary (they are deliberately
self-sufficient - DSL mechanics, endpoint patterns, validation
guidance), let the Maven build be the validator, and record the outage
and which consultations were skipped in the plan's Notes section and
the handoff. The catalog is an grounding aid, not a dependency.

### 4. Design the test cases

For each testable element, pick a strategy that **runs green in a plain
`mvn test` inside a sandbox** (no application server, no external
services, nothing beyond localhost):

- **JAX-RS resources** - the default strategy is an in-JVM deployment:
  boot the application's REST application/resource classes in an embedded
  Jakarta REST container (test-scoped dependency, e.g. the server stack's
  embedded-server artifact) and exercise them with the Citrus `http`
  client (`send`/`receive` with JSON payload and status-code validation).
  For **data-backed resources** (resource → CDI → JPA), plan the
  data-backed REST recipe: the test must additionally provide what the
  container normally supplies - a test persistence unit over embedded H2,
  a `@Alternative` CDI producer replacing the container's
  `@PersistenceContext` EntityManager, a per-request filter that
  activates the CDI request context and commits a resource-local
  transaction, and a CDI-integrated RESTEasy deployment. The author
  skill owns the validated recipe; the plan just names the elements to
  replace (which beans inject the EntityManager, which beans cannot
  live without a container runtime and must be vetoed or covered by the
  container lane) and the test datasource (H2 unless the schema needs
  vendor specifics).
- **Applications consuming external endpoints** - a provider-side Citrus
  simulation: start Citrus endpoint servers (http/jms) that stand in for
  the external system, and drive the application's client code against
  them.
- **Plain CDI beans / POJO services** - direct invocation wrapped in a
  Citrus test case is possible but usually not worth it; prefer leaving
  them to conventional unit tests and note that in the plan.
- **Container-bound elements** (EJBs with container services, MDBs,
  servlet filters) - do not fake them; list them as out of scope with the
  reason.

**Validation contract (mandatory per test row)** - a Citrus `receive`
without message validation passes on *any* response, and a simulation
that responds without validating the incoming request accepts anything:
a green suite with such actions proves nothing. Every inventory row must
therefore name its validation points explicitly:

- for **client-side tests**: the request aspects sent (method, path,
  body, `Accept`/`Content-Type`) and the response aspects **validated**
  - status, body value or fragment, `Content-Type`, other headers. The
  author stage may validate more than planned, never less;
- for **simulations**: the request aspects the simulation
  **validates on receipt** before responding (the point of the
  simulation is to assert what the application actually sends);
- mark deliberately-loose validations as such (e.g.
  `@contains('...')@` for a body with volatile fields, `@ignore()` for
  timestamps) with the reason - looseness is a plan decision, not an
  author shortcut.

Every test must be deterministic and side-effect free. Tests live in
`src/test/java` following the application's existing package convention.
Naming follows the lane: `test_kind: test` → `*Test` classes (surefire
picks them up; do not touch existing `*IT` Arquillian tests);
`test_kind: it` → `*IT` classes (failsafe picks them up; do not touch
existing `*Test` classes or other `*IT` machinery such as
`RESTEndPointIT`). For the container lane, the plan's Dependencies
section must also include the `citrus-container-tests` Maven profile the
author stage will create (see the author skill for the verified
recipe) and the verification command
`mvn verify -Pcitrus-container-tests`.

### 5. Write the plan

Write `docs/citrus-test-plan.md` at the **repository root** (git-tracked,
not inside the application path - the author and verify stages of the next
sandboxes read it after cloning the target branch) with exactly these
sections:

- **Goal** - one paragraph: application, path, what the tests should
  establish.
- **Application profile** - Java baseline, chosen Citrus version (+ why),
  existing test stack, in-JVM vs container-bound surface.
- **Test inventory** - one row per planned test class: class name (FQN),
  what it exercises, Citrus endpoints/actions used (catalog-cited),
  strategy, the `mvn` command that runs it.
- **Dependencies** - the exact test-scoped dependencies (groupId:artifactId
  and version, aligned with the chosen Citrus BOM) to add to the pom, and
  any property/config files to create.
- **Verification** - the commands the author and verify stages run
  (e.g. `mvn -pl <module> test`), and what "green" means.
- **Out of scope** - elements deliberately not tested and why.
- **Notes** - MCP catalog entries consulted, risks, assumptions.

### 6. Hand off

Append a `## Plan` section to `.konveyor/handoff.md` (create the file if
absent). The handoff is a **fixed-format document** - the author and
verify stages (and any tooling that refines the skills) parse it, so
follow the template exactly; put narrative detail inside the fields,
never change the field names or order:

    # Citrus Tests — <application name> (<run label>)
    ## Plan
    - Lane: test | it (+ one-line reason)
    - Citrus version: <version> (+ one-line reason)
    - Inventory verdicts: <element> → tested-in-JVM | tested-container |
      better-as-unit-test | out-of-scope (<reason>); one per element
    - Dependencies planned: <artifact:version list>
    - Commands: <the exact mvn commands each stage runs>
    - Environment notes: <proxy/settings/network facts the next stages need>
    - Notes: <assumptions, risks, skipped MCP consultations>

The author stage appends `## Author`, the verify stage appends
`## Verify` (see their skills) using the same discipline: fixed section
headers, factual field lines, numbered exceptions. The `## Verify`
section must end with a verdict line in exactly one of these forms:

    Verdict: verified
    Verdict: verified with exceptions (<numbered list follows)

**Commit** `docs/citrus-test-plan.md` and `.konveyor/handoff.md` - each
stage runs in a fresh sandbox and continuity carries via git on the target
branch. Do not commit anything else.
