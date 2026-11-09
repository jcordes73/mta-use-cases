---
name: citrus-test-author
description: Authors Citrus tests for a JEE application from a committed test plan - adds the Citrus test dependencies to the pom.xml, writes the JUnit5-run Citrus test classes and endpoint configurations grounded in the Citrus MCP server's catalogs and schemas, and iterates mvn test until the new tests pass. Use when implementing a prepared Citrus test plan.
---

# Citrus Test Authoring

Implement the Citrus tests defined in the committed test plan, using the
Citrus MCP server (`citrus` tools) as the authoritative reference for the
framework's vocabulary. This is the **author** stage of a
plan → author → verify workflow.

**Scope**: the application path named in the task instructions
($(workflow.path), empty = repository root). You may modify ONLY:

- `pom.xml` files inside the application path (test-scoped dependencies and
  plugins only),
- files under `src/test/` inside the application path,
- `docs/citrus-test-plan.md` and `.konveyor/handoff.md` (status updates).

Never modify `src/main/` sources or configuration, existing tests, or
anything outside the application path.

## Workflow

### 1. Ground yourself

Read `docs/citrus-test-plan.md` (repository root) and the `## Plan` section
of `.konveyor/handoff.md`. The plan is the contract: Citrus version, test
inventory, dependencies, verification commands. If the plan is missing or
contradicts the repository, stop and record the blocker in
`.konveyor/handoff.md` (## Author section) instead of inventing a new plan.

If the `MAVEN_SETTINGS_XML` environment variable is set, write its value to
`/home/harness/.m2/settings.xml` so Maven uses the custom settings for
every build this stage runs. **If it is unset**: the sandbox image bakes a
default `/home/harness/.m2/settings.xml` containing a Squid proxy
(`squid.squid.svc:3128`) — verified live on both the author and verify
sandboxes, where the proxy made every Maven download time out (the
allowlist does not cover all repositories a JEE app needs, e.g.
`repository.jboss.org`). Probe the proxy (`curl -x` through it vs. direct)
and if it is unreachable, remove the `<proxies>` block from that
environment file before the first build — a dead proxy stalls Maven for
minutes per artifact and can burn the stage's turn budget.

### 2. Add the Citrus test dependencies

Per the plan: import the Citrus BOM (`org.citrusframework:citrus-bom` at
the plan's version) in the pom's `dependencyManagement`, and add the
test-scoped artifacts the plan names (typically `citrus-junit-jupiter` —
the JUnit 5 runtime module — and the endpoint modules, e.g. `citrus-http`).
Keep versions in one place (BOM or properties). Do not upgrade or touch any
existing (non-test) dependency.

**BOM conflicts (general rule, verified live)**: prefer the versions the
application's BOM manages; when a BOM-managed version is incompatible
with what the tests need, pin test-scoped and document the pin in the
handoff. Two known instances on the EAP quickstarts:

- **Jackson 3 pitfall**: Citrus 5.x uses **Jackson 3**
  (`tools.jackson` groupId), whose annotations artifact is still
  `com.fasterxml.jackson.core:jackson-annotations` but from the
  Jackson-3-compatible `2.2x` line (e.g. `2.22` for Citrus 5.1.0). The EAP
  BOM pins the Jackson 2 line (`2.18.4`), which breaks every Citrus test
  at runtime with `Could not initialize class
  tools.jackson.databind.json.JsonMapper$Builder` or
  `NoClassDefFoundError com/fasterxml/jackson/annotation/JsonSerializeAs` —
  compilation succeeds, so the failure only appears when the tests run.
- **Embedded container artifacts**: the EAP BOM manages RESTEasy
  (`6.2.12.Final-redhat-00001`), but `resteasy-undertow` is not
  BOM-managed — declare it test-scoped with an explicit upstream version
  (the BOM still forces its `resteasy-core`/core-spi transitives to the
  Red Hat build of the same 6.2.12 line, which is fine).

**Surefire must run the new tests**: the application may predate JUnit 5
(JUnit 4 dependency, surefire 2.x, failsafe-managed `*IT` classes). Check
the effective surefire configuration before writing tests: JUnit 5 needs
surefire ≥ 2.22 (the quickstarts' 3.2.3 auto-detects the Jupiter engine);
if the application pins an older surefire or restrictive `<includes>`,
add a test-scoped surefire upgrade/plugin configuration inside the
application path and confirm from the run output that the new `*Test`
classes execute - a suite that silently runs zero tests is not green.

### 3. Consult the Citrus MCP server while authoring

Ground every DSL construct in the catalog before writing it - do not write
Citrus syntax from recall:

- `citrus_catalog_endpoint` / `citrus_catalog_endpoint_schema` for every
  endpoint configuration you write (client `http` URL, timeouts, content
  type; server endpoints for simulations);
- `citrus_catalog_action` / `citrus_catalog_action_schema` for every test
  action and container you use (`send`, `receive`, `createVariables`, ...);
- `citrus_docs_page` for the runtime integration and any feature you use
  for the first time in this codebase — the docs tools match `kind` and
  `searchKey` as substrings of the documentation file names (e.g.
  `kind: "runtime"`, `searchKey: "jupiter"` for the JUnit 5 runtime page)
  and return the raw AsciiDoc in the `adoc` field.

The catalog tools are only reliable for the MCP server's own Citrus
version; for an older target line, ground the vocabulary in the default
catalog and let the Maven build validate against the actual artifacts.

### 4. Implement the tests

For each row of the plan's test inventory:

- create the test class (FQN from the plan) under `src/test/java` with the
  planned strategy - in-JVM embedded deployment for JAX-RS resources with
  Citrus `http` client tests, Citrus simulation servers for consumed
  endpoints;
- create the endpoint configuration the plan defines (test resources,
  properties, Spring/XML or Java DSL config per the plan and docs);
- keep every test deterministic: localhost only, no time-dependent
  assertions, no ordering dependencies between tests;
- keep the application code under test executing **in-process** (in-JVM
  deployment, as here): the verify stage measures coverage with the
  JaCoCo agent attached to the surefire JVM, so code exercised in a
  forked JVM or external server is invisible to the report.

DSL mechanics verified live on Citrus 5.1.0 - check the catalog/docs when
your target version differs:

- **Test action runner**: `GherkinTestActionRunner` has no `http()` method -
  wrap the actions in `runner.when(http().client(...).send()...)` /
  `runner.then(http().client(...).receive()...)` with
  `import static org.citrusframework.http.actions.HttpActionBuilder.http;`.
  Do NOT implement `TestActionSupport` in the test class: its default
  `startServer()/stopServer()` DSL methods clash with the static JUnit
  `@BeforeAll`/`@AfterAll` lifecycle methods an embedded server needs.
- **Validation matcher syntax**: only the function-style form works -
  `@contains('text/plain')@`. The `@contains@value@` form fails with
  `Illegal syntax for validation matcher expression`. Content-type
  assertions benefit from a contains-match anyway (servers append
  `;charset=...`).
- **Embedded JAX-RS deployments**: embedded RESTEasy deployments do NOT
  classpath-scan resources the way a servlet container does - the
  `UndertowJaxrsServer.deploy(Class)` shortcuts serve 404 for the
  resources. Build the deployment explicitly:
  `new ResteasyDeploymentImpl()` + `setApplicationClass(...)` +
  `getActualResourceClasses().add(...)` (explicit registration), deployed
  via `server.undertowDeployment(deployment, null)` with an explicit
  `DeploymentInfo.setContextPath(...)` (the prefix argument is the servlet
  mapping, not the context path) - mirroring the application's
  `@ApplicationPath`.

### 5. Verify and iterate

Run the plan's verification command (e.g. `mvn -pl <module> test`) from
the application path. Iterate until the new tests pass:

- fix issues in the test code, endpoint config, or test dependencies;
  never "fix" failures by weakening the assertion below what the plan
  states;
- bound the loop - if a test cannot be made green within a reasonable
  number of attempts, remove it (or mark it explicitly `@Disabled` with
  the reason), and record the outcome in the handoff. A suite that is
  green but smaller than planned beats a suite that does not build.
- `mvn test` must leave the build green: surefire runs the new `*Test`
  classes; existing tests keep passing (if a new test breaks an existing
  one, the new test is wrong - fix or drop it).

### 6. Hand off

Append an `## Author` section to `.konveyor/handoff.md`: tests added
(class list), dependencies added, `mvn test` result (counts), deviations
from the plan (with reasons), anything the verify stage must know.

**Commit** all changes (pom, tests, config, `docs/citrus-test-plan.md`
status edits, `.konveyor/handoff.md`) with a message summarizing the added
tests - each stage runs in a fresh sandbox and continuity carries via git
on the target branch.
