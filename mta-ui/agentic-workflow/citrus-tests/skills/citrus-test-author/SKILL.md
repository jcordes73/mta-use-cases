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
every build this stage runs.

### 2. Add the Citrus test dependencies

Per the plan: import the Citrus BOM (`org.citrusframework:citrus-bom` at
the plan's version) in the pom's `dependencyManagement`, and add the
test-scoped artifacts the plan names (typically `citrus-junit-jupiter` —
the JUnit 5 runtime module — and the endpoint modules, e.g. `citrus-http`).
Keep versions in one place (BOM or properties). Do not upgrade or touch any
existing (non-test) dependency.

**Jackson 3 pitfall (verified live)**: Citrus 5.x uses **Jackson 3**
(`tools.jackson` groupId), whose annotations artifact is still
`com.fasterxml.jackson.core:jackson-annotations` but from the
Jackson-3-compatible `2.2x` line (e.g. `2.22` for Citrus 5.1.0). Application
BOMs typically manage the old Jackson 2 annotations (e.g. the EAP BOM pins
`2.18.4`), which break every Citrus test at runtime with
`Could not initialize class tools.jackson.databind.json.JsonMapper$Builder`
or `NoClassDefFoundError com/fasterxml/jackson/annotation/JsonSerializeAs` —
compilation succeeds, so the failure only appears when the tests run. When
the application's BOM manages `jackson-annotations`, add a test-scoped pin
of the Jackson-3-compatible version (2.22 or newer within 2.2x) and note it
in the handoff.

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
  assertions, no ordering dependencies between tests.

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
