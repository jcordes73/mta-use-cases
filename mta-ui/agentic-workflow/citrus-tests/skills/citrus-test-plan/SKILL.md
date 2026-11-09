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

### 2. Choose the Citrus version line

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

### 4. Design the test cases

For each testable element, pick a strategy that **runs green in a plain
`mvn test` inside a sandbox** (no application server, no external
services, nothing beyond localhost):

- **JAX-RS resources** - the default strategy is an in-JVM deployment:
  boot the application's REST application/resource classes in an embedded
  Jakarta REST container (test-scoped dependency, e.g. the server stack's
  embedded-server artifact) and exercise them with the Citrus `http`
  client (`send`/`receive` with JSON payload and status-code validation).
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

Every test must be deterministic and side-effect free. Tests live in
`src/test/java` following the application's existing package convention,
named `*Test` (surefire picks them up; do not touch existing `*IT`
Arquillian tests).

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
absent): the plan file location, the chosen Citrus version, the test count,
and anything the author stage must know (e.g. Java version constraints,
proxy settings if Maven needs them - if the `MAVEN_SETTINGS_XML`
environment variable is set, write its value to
`/home/harness/.m2/settings.xml` before any Maven call).

**Commit** `docs/citrus-test-plan.md` and `.konveyor/handoff.md` - each
stage runs in a fresh sandbox and continuity carries via git on the target
branch. Do not commit anything else.
