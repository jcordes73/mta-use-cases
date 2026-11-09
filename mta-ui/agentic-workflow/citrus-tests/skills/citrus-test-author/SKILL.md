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

**MCP-outage fallback**: if the MCP server fails to start or tool calls
fail unrecoverably, proceed with this skill's own verified recipes
(DSL mechanics, dependency patterns, validation rules) and the Maven
build as validator - do not stall the stage waiting for the server.
Record the outage and what was not consulted in the handoff.

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
  forked JVM or external server is invisible to the report;
- **validate what the plan's validation contract names - never less**:
  every `receive` action MUST carry a `.message()` block with at least
  the planned status, body and header validations. A `receive` without
  a `.message()` block passes on *any* response - it is not a test and
  must not be committed. Likewise a simulation server MUST validate the
  incoming request (body/headers per plan) in its `receive` before
  `send`ing the scripted response - responding unconditionally proves
  nothing about what the application sends. You may validate more than
  planned; weakening below the plan is the verify stage's definition of
  a defect.

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

### 5. Container lane (`test_kind: it`): the `citrus-container-tests` profile

> **Reference recipe** - consult when (and only when) the plan chose
> the container lane. In-JVM runs skip this section entirely. The
> recipe is validated live end-to-end (helloworld-rs against EAP 8.1
> Update 9.0, 2/2 ITs green, server-side JaCoCo 100%).

When the plan chose the container lane, create a
`citrus-container-tests` Maven profile (test-side pom change) that
provisions a real JBoss EAP server, deploys the application, and runs
the planned `*IT` classes via failsafe. The following recipe is
**verified live end-to-end** (helloworld-rs against EAP 8.1 Update 9.0,
2/2 ITs green, server-side JaCoCo 100%) — follow it closely; five
defects were found and fixed during validation:

```xml
<profile>
    <id>citrus-container-tests</id>
    <dependencies>
        <dependency> <!-- JaCoCo agent jar for the -javaagent opt below -->
            <groupId>org.jacoco</groupId>
            <artifactId>org.jacoco.agent</artifactId>
            <version>0.8.12</version>
            <classifier>runtime</classifier>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.wildfly.plugins</groupId>
                <artifactId>wildfly-maven-plugin</artifactId>
                <!-- reuse the application's managed version; MUST be >= 5.x
                     (4.x cannot parse the EAP 8.x feature-pack schema 3.0) -->
                <configuration>
                    <jbossHome>${project.build.directory}/eap-server</jbossHome>
                    <channels>
                        <channel>
                            <manifest>
                                <groupId>org.jboss.eap.channels</groupId>
                                <artifactId>eap-8.1</artifactId> <!-- match the app's EAP BOM line -->
                            </manifest>
                        </channel>
                    </channels>
                </configuration>
                <executions>
                    <execution>
                        <id>provision-server</id>
                        <phase>pre-integration-test</phase>
                        <goals><goal>provision</goal></goals>
                        <configuration>
                            <!-- provision ignores jbossHome; point it at the
                                 same dir start/deploy/shutdown read -->
                            <provisioning-dir>${project.build.directory}/eap-server</provisioning-dir>
                            <feature-packs>
                                <feature-pack>
                                    <location>org.jboss.eap:wildfly-ee-galleon-pack</location>
                                </feature-pack>
                            </feature-packs>
                            <galleon-options>
                                <jboss-fork-embedded>true</jboss-fork-embedded>
                            </galleon-options>
                        </configuration>
                    </execution>
                    <execution>
                        <id>start-server</id>
                        <phase>pre-integration-test</phase>
                        <goals><goal>start</goal></goals>
                        <configuration>
                            <!-- coverage of the deployed app: the classes run in
                                 the SERVER JVM, so the agent rides on the server -->
                            <javaOpts>
                                <javaOpt>-javaagent:${settings.localRepository}/org/jacoco/org.jacoco.agent/0.8.12/org.jacoco.agent-0.8.12-runtime.jar=destfile=${project.build.directory}/jacoco-it.exec,output=file,append=false</javaOpt>
                            </javaOpts>
                        </configuration>
                    </execution>
                    <execution>
                        <id>deploy-app</id>
                        <phase>pre-integration-test</phase>
                        <goals><goal>deploy</goal></goals>
                    </execution>
                    <execution>
                        <id>shutdown-server</id>
                        <phase>post-integration-test</phase>
                        <goals><goal>shutdown</goal></goals>
                    </execution>
                </executions>
            </plugin>
            <plugin> <!-- server-side coverage report, verify phase -->
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <id>report-it-coverage</id>
                        <phase>verify</phase>
                        <goals><goal>report</goal></goals>
                        <configuration>
                            <dataFile>${project.build.directory}/jacoco-it.exec</dataFile>
                            <outputDirectory>${project.build.directory}/site/jacoco-it</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <includes> <!-- exactly the planned IT classes -->
                        <include>**/HelloWorldResourceIT</include>
                    </includes>
                </configuration>
                <executions>
                    <execution>
                        <goals><goal>integration-test</goal><goal>verify</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    <pluginRepositories>
        <!-- Galleon resolves EAP feature-pack artifacts as PLUGIN dependencies,
             which do NOT inherit the project's <repositories> -->
        <pluginRepository>
            <id>redhat-ga-maven-plugin-repository</id>
            <url>https://maven.repository.redhat.com/ga/</url>
            <releases><enabled>true</enabled><updatePolicy>never</updatePolicy></releases>
            <snapshots><enabled>false</enabled></snapshots>
        </pluginRepository>
        <pluginRepository>
            <id>jboss-public-maven-plugin-repository</id>
            <url>https://repository.jboss.org/nexus/content/groups/public/</url>
            <releases><enabled>true</enabled><updatePolicy>never</updatePolicy></releases>
            <snapshots><enabled>false</enabled></snapshots>
        </pluginRepository>
    </pluginRepositories>
</profile>
```

The verified pitfalls, in the order they bite:

- **Plugin version ≥ 5.x** — 4.x fails provisioning with
  `Failed to parse /feature-pack.xml ... unexpected content: element
  start{urn:jboss:galleon:feature-pack:3.0}`. Prefer the application's
  managed `wildfly-maven-plugin` version (the EAP quickstarts manage
  5.1.x); do not pin your own.
- **`<channels>` is the resolver** — EAP 8.x artifacts are not all on
  `redhat-ga` (e.g. `org.wildfly.core:wildfly-elytron-tool-wrapper`
  404s); the channel manifest
  (`org.jboss.eap.channels:eap-8.1`, matching the application's EAP BOM
  line) tells Galleon where every artifact lives. Mirror the
  application's own `eap-maven-plugin` channel config if present.
- **`<pluginRepositories>` in the profile** — without them, resolution
  fails with `Could not find artifact ... in central`.
- **`provisioning-dir` mirrors `jbossHome`** — the `provision` goal
  ignores `jbossHome` and defaults to `target/server`, leaving
  `start` with `JBOSS_HOME ... is not a valid directory`.
- **Failed resolutions are cached** (`*.lastUpdated` markers) — after
  fixing a repository problem, re-run with `-U` or the build keeps
  failing from cache.

IT-class mechanics for this lane (differ from the in-JVM lane):

- the test class owns **no server lifecycle** — no embedded deployment,
  no `@BeforeAll` server start; the profile did provision/start/deploy.
  Drive the deployed application with a Citrus `http` client at
  `http://localhost:8080/<finalName>` (make the base URL a
  `server.url` system property with a default);
- the context path is the WAR `finalName` (default: artifactId) plus
  the `@ApplicationPath` prefix;
- activating the profile explicitly (`mvn verify
  -Pcitrus-container-tests`) deactivates the quickstarts'
  `activeByDefault` `provisioned-server` profile automatically — do
  not try to neutralize it another way;
- keep failsafe `<includes>` exactly the planned `*IT` classes; the
  quickstarts' pre-existing `RESTEndPointIT` (JUnit 4, needs
  SERVER_HOST env and an external server) must not be picked up.

### 6. Verify and iterate

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

### 7. Hand off

Append an `## Author` section to `.konveyor/handoff.md`: tests added
(class list), dependencies added, `mvn test` result (counts), deviations
from the plan (with reasons), anything the verify stage must know.

**Commit** all changes (pom, tests, config, `docs/citrus-test-plan.md`
status edits, `.konveyor/handoff.md`) with a message summarizing the added
tests - each stage runs in a fresh sandbox and continuity carries via git
on the target branch.
