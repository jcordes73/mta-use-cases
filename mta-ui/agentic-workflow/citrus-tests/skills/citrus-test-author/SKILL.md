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

## When to use this skill

Use this skill when a **committed test plan** exists (`docs/citrus-test-plan.md`
plus the `## Plan` section of `.konveyor/handoff.md`) and the task is to
implement it: add the dependencies, write the Citrus test classes and
endpoint configurations, and iterate until the suite is green.

Do **not** use this skill when:

- no plan is committed — use `citrus-test-plan` first. This stage's
  contract is that plan; authoring without one is how validation points
  get skipped and vacuous-green suites happen;
- the task is to re-run and audit already-committed tests in a fresh
  environment — use `citrus-test-verify`.

## Workflow

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
`repository.jboss.org`).

**Pre-flight (mandatory): run the doctor before anything else.** The
image bakes `citrus-doctor.sh` (also in the workflow directory next to
the skills):

    bash /usr/local/bin/citrus-doctor.sh pre <application-path>

It probes the Maven proxy, the Java baseline vs the planned Citrus line,
the effective surefire (version floor, restrictive includes), checkstyle
wiring, and BOM-managed versions — checks that otherwise surface one
failed `mvn` run each. Fix every FAIL it reports (fix recipes are in the
output and in this skill) and treat WARNs as standing instructions (e.g.
iterate with `-Dcheckstyle.skip=true`); record FAILs you cannot resolve
in the handoff. Re-run it until no FAILs remain.

### 2. Add the Citrus test dependencies

Per the plan: import the Citrus BOM (`org.citrusframework:citrus-bom` at
the plan's version) in the pom's `dependencyManagement`, and add the
test-scoped artifacts the plan names (typically `citrus-junit-jupiter` —
the JUnit 5 runtime module — the endpoint modules, e.g. `citrus-http`,
and `citrus-validation-json` when the plan validates values inside JSON
payloads via JSON-path).
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

**Dependency pre-flight: after the pom edits, before the first test
run**, resolve the classpath once and check it:

    bash /usr/local/bin/citrus-doctor.sh deps <application-path>

It runs `mvn dependency:list` (the same resolution the first test run
would pay) and checks the classpath-level defects that only surface as
*runtime* errors inside a full test boot: the jandex 2.x/3.x clash
(`NoSuchMethodError: Indexer.indexWithSummary`), the resolved
jackson-annotations line vs Citrus 5, and a missing JUnit 5 engine. Fix
every FAIL with the recipe in the output (also in this skill's data-backed
REST recipe and dependency-conflict list) before running tests.

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

**Recipe library** — per-scenario recipes live in `recipes/` next to
this file. Read the matching one on demand, don't guess the builder DSL
from memory:

| Recipe | Scenario | Status |
|---|---|---|
| `recipes/data-backed-rest.md`* | data-backed REST in-JVM (Weld SE + H2) | validated live |
| (container recipe in step 5 below) | container lane `*IT` vs real EAP | validated live |
| (JMS recipe in `citrus-test-plan` SKILL.md) | JMS in-JVM (embedded Artemis) | validated live |
| `recipes/soap.md` | SOAP (`citrus-ws`) | validated live (jaxws-ejb, container lane) |
| `recipes/mail.md` | mail (`citrus-mail`) | **DRAFT — unvalidated** |
| `recipes/ftp-sftp.md` | FTP/SFTP (`citrus-ftp`) | **DRAFT — unvalidated** |
| `recipes/websocket.md` | WebSocket (`citrus-websocket`) | validated live (websocket-hello) |
| `recipes/graphql.md` | GraphQL (`citrus-graphql`) | **DRAFT — unvalidated** |
| `recipes/kafka.md` | Kafka + embedded broker (`citrus-kafka`) | **DRAFT — unvalidated** |
| `recipes/jmx-rmi-ssh.md` | JMX / RMI / SSH (`citrus-jmx`, `citrus-rmi`, `citrus-ssh`) | **DRAFT — unvalidated** |

\* The validated REST/container recipes live inline in this skill
(steps 4–5) until the reference-card split moves them out.

**Promotion protocol**: a DRAFT recipe's DSL is grounded in the Citrus
5.1.0 MCP catalog/docs, but its pitfalls are *expected*, not observed.
When a harness or cluster run exercises a draft family, fold the
observed failure signatures into the recipe and flip its banner to
verified — never present a draft's pitfalls as validated experience in
the handoff.

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

**Data-backed REST recipe (REST + CDI + JPA in-JVM)** — validated live
end-to-end on kitchensink (REST resource → `@Inject` CDI service →
Hibernate over H2; 4/4 tests green in plain `mvn test`). Use it whenever
the planned resource is data-backed; seven defects were found and fixed
during validation, so follow the pattern closely:

1. **CDI bootstrap — explicit registration, never classpath discovery.**
   `new Weld().disableDiscovery().addPackage(false, <resource-or-bean
   class>)...` . Full classpath discovery (`new Weld().initialize()`) is
   unreliable in a surefire JVM: archives without a `beans.xml` may be
   skipped entirely, the resource silently ends up a non-bean, and
   RESTEasy's `CdiInjectorFactory` falls back to plain construction
   (symptom: `NullPointerException` on an injected field). Register every
   package the REST surface reaches, plus:
   - `@Stateless`/`@Singleton` EJB beans: NOT bean-defining in an SE
     bootstrap — `addBeanClasses(...)` them explicitly;
   - `addAlternative(...)` for the test producer (below);
   - `addExtension(new ResteasyCdiExtension())` — with `disableDiscovery()`
     the services-file registration of resteasy-cdi's own extension is
     skipped and `CdiInjectorFactory` fails with `RESTEASY010615: Unable
     to obtain ResteasyCdiExtension instance`;
   - a test CDI extension vetoing application beans that inject container
     runtime objects with no SE substitute (e.g. a JSF controller
     injecting `FacesContext`) — otherwise the bootstrap fails with
     `WELD-001408: Unsatisfied dependencies` before any test runs.
2. **Test resources via `@Alternative` producer** (`@ApplicationScoped
   @Alternative` test class, selected via `addAlternative`): replace the
   container-provided beans the application injects —
   `@Produces` a request-scoped `EntityManager` (see 3), the
   `jakarta.validation.Validator` (`Validation.buildDefaultValidatorFactory()`,
   with `hibernate-validator` + an EL implementation like `expressly`
   test-scoped for message interpolation), and a `Logger` producer (copy
   the application's if it has one). Add a **test-only persistence unit**
   (`src/test/resources/META-INF/persistence.xml`, name distinct from the
   app's, `RESOURCE_LOCAL`, `jdbc:h2:mem:<name>;DB_CLOSE_DELAY=-1`,
   `schema-generation.database.action=drop-and-create`) so the main PU
   (JNDI datasource) is never bootstrapped.
3. **Request context + transactions via one servlet filter.** A Weld SE
   bootstrap does not manage web scopes or transactions against servlet
   requests, so a `@RequestScoped` resource dies with
   `WELD-001303: No active contexts for scope type RequestScoped`. Register
   a test filter on the embedded deployment (`Servlets.filter(...)` +
   `addFilterUrlMapping("/*", REQUEST)`) that, per request: activates the
   request context (built-in `jakarta.enterprise.context.control.
   RequestContextController` bean — note **Weld 5 removed
   `ContextControl`**), selects the request-scoped `EntityManager`
   (produce it `@Produces @RequestScoped` so the filter and the resource
   share ONE instance), `begin()`s a resource-local transaction, commits
   after the chain (rollback on RuntimeException), closes the EM,
   deactivates. The transaction is what makes writes persist: **Hibernate
   silently skips writes outside a transaction** even with
   `hibernate.allow_update_outside_transaction=true` (the property only
   silences the check) — symptom: POST returns 200 but the row never lands.
4. **RESTEasy-CDI integration.** Construct
   `deployment.setInjectorFactory(new CdiInjectorFactory(cdi.getBeanManager()))`
   — presetting the *instance* bypasses the no-arg factory's BeanManager
   lookup (JNDI → `CDI.current()` → servlet attribute), which is
   unreliable in this setup. Register the JSON-B provider explicitly
   (`getActualProviderClasses().add(JsonBindingProvider.class)`;
   resteasy-json-binding-provider test-scoped — embedded deployments do
   not auto-discover providers).
5. **Dependency conflicts to expect** (all found live):
   - `resteasy-core` drags in the pre-3.x `org.jboss:jandex`, which
     shadows `io.smallrye:jandex` 3.x on the test classpath and Weld 5
     blows up with `NoSuchMethodError: Indexer.indexWithSummary` —
     exclude `org.jboss:jandex` and add `io.smallrye:jandex` test-scoped;
     several REST quickstarts ship the 2.x jandex transitively (repo
     sweep: helloworld-rs, jaxrs-client, jaxrs-jwt, logging,
     thread-racing, todo-backend), so expect the clash there as soon as
     the recipe adds Weld 5 - apply the exclude up front rather than
     waiting for the red first run;
   - Citrus 5.1.0 needs the Jackson-annotations line matching its Jackson
     version — the EAP BOM pins an older one; add the newer version
     test-scoped if validation fails with Jackson errors;
   - checkstyle runs on test sources in the quickstarts — match the
     ruleset (no redundant/unused imports) or the build dies before tests.
6. **HTTP client details**: send actions must set an explicit
   `.contentType("application/json")` on POSTs with bodies — Citrus sends
   no Content-Type by default and the resource's `@Consumes` match
   returns 415. Request paths carry the `@ApplicationPath` prefix
   (context path mirrors it).
7. **JSON content assertions**: the message validator is selected by the
   response's `Content-Type` (a JSON response is validated by the JSON
   validator element-wise, regardless of a `MessageType.PLAINTEXT` you
   declare). For values inside JSON payloads use **JSON-path
   validation** (`citrus-validation-json` test-scoped):
   `.message().type(MessageType.JSON).validate(jsonPath().expression("$[0].email", "jane@example.org"))`
   — expected values support validation matchers
   (`@contains('well-formed email')@`). The `jsonPath()` static lives in
   `org.citrusframework.json.dsl.JsonPathSupport`, NOT
   `org.citrusframework.validation.json`. Exact-body element-wise
   comparison still works for small deterministic payloads.
8. **Test isolation**: the in-memory H2 lives for the whole JVM — wipe
   rows between tests (`@AfterEach`) and clear seed data
   (`import.sql`-style fixtures load at EMF creation) in a
   `@BeforeEach`/startup step, or the "empty list" test sees other tests'
   rows.
9. **Weld 5 API and bootstrap mechanics** (all found live in a fresh
   sandbox):
   - `addAlternative(...)` alone is NOT enough under `disableDiscovery()`
     — it only marks the class as an enabled alternative; the class must
     ALSO be registered via `addPackage`/`addBeanClasses`, or its
     producers stay invisible (`WELD-001408` on Logger/EntityManager);
   - `WeldContainer.instance()` is gone in Weld 5 — use
     `(WeldContainer) CDI.current()`;
   - RESTEasy 6.2 moved `ResteasyDeploymentImpl` to
     `org.jboss.resteasy.core` (the `org.jboss.resteasy.spi` import is
     the 5.x location);
   - **client-proxy laziness**: a bean obtained via
     `container.select(...).get()` returns the client proxy without
     running its `@PostConstruct` — if the test's EMF bootstrap lives in
     a `@PostConstruct`, the persistence unit is never created and the
     first query dies with `Table ... not found`. Force an eager probe
     invocation on the bean in `@BeforeAll` (e.g. call an `isReady()`
     method) or bootstrap the EMF from a static initializer.
10. **`hibernate-core` scope vs `hibernate-jpamodelgen`** (found live in
   a fresh sandbox; the quickstarts carry the jpamodelgen annotation
   processor at provided scope): declaring `org.hibernate.orm:
   hibernate-core` test-scoped wins Maven nearest-wins mediation over the
   *provided-scope transitive* from `hibernate-jpamodelgen` — which
   removes hibernate-core from the **main-compile** classpath and crashes
   the processor with `NoClassDefFoundError:
   ...ArrayHelper` in `HibernateProcessor.init`. The failure is
   **cold-compile only** — a warm `target/classes` masks it, so it looks
   transient. Do NOT declare hibernate-core test-scoped in these repos:
   rely on the provided transitive (provided scope is test-classpath-
   visible, so tests see it). Related rule for the fast inner loop:
   after touching provided/transitive dependencies, run one **clean**
   compile before trusting a green incremental loop. The doctor's
   `deps` mode checks this pom shape.

### 5. Container lane (`test_kind: it`): the `citrus-container-tests` profile

> **Reference recipe** - consult when (and only when) the plan chose
> the container lane. In-JVM runs skip this section entirely. The
> recipe is validated live end-to-end (helloworld-rs against EAP 8.1
> Update 9.0, 2/2 ITs green, server-side JaCoCo 100%).

When the plan chose the container lane, **first run the container-mode
pre-flight** (it detects the profile defects that otherwise surface as
failed provisionings — each one minutes of build time):

    bash /usr/local/bin/citrus-doctor.sh container <application-path>

Then create a
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
- **failsafe is the whole test phase of this lane - it fails silently**:
  JUnit 5 ITs need failsafe >= 2.22 (the doctor's `container` mode
  checks the effective version), and 40/47 quickstart modules declare
  restrictive `<includes>` naming their own IT classes - a new `*IT`
  class outside those patterns is skipped and `mvn verify` reports
  green with zero tests. The recipe above writes the profile's own
  `<includes>` (overriding the module's), so the new ITs always run;
  confirm from the verify report's test count, never from BUILD
  SUCCESS alone.

### 6. Verify and iterate

Run the plan's verification command (e.g. `mvn -pl <module> test`) from
the application path. Iterate until the new tests pass:

- **fast inner loop**: while iterating on one test class, run only that
  class with style gates off —
  `mvn -q -Dtest=<NewTestClass> test -Dcheckstyle.skip=true` — and run
  the **full clean suite exactly once** before committing (the
  plan's verification command, without skips). A full `mvn` cycle in the
  sandbox costs minutes plus a reasoning turn; the cheap loop is where
  edits happen. Do not commit with skip flags active;
- batch work between runs: write all test files and pom edits for a
  verification round in one go, then run once — one build per edit-batch,
  not per file;
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

Append an `## Author` section to `.konveyor/handoff.md`, following the
fixed-format template the plan stage established (same discipline:
fixed section header, factual field lines, numbered deviations):

    ## Author
    - Result: green | red (+ counts: Tests run N, Failures F, Errors E)
    - Tests added: <class FQN list, one per line>
    - Dependencies added: <artifact:version list>
    - Iterations: <n mvn runs, n failed - the loop length, for skill
      refinement; the failed ones by first error signature>
    - Pitfalls hit: <doctor/recipe entries that fired, e.g. "jandex
      clash", "checkstyle"; "none" if none - this is how the skills
      learn which recipes earn their place>
    - Deviations: none | <numbered list, each with the reason>
    - Environment notes: <anything verify needs about the sandbox>

**Commit** all changes (pom, tests, config, `docs/citrus-test-plan.md`
status edits, `.konveyor/handoff.md`) with a message summarizing the added
tests - each stage runs in a fresh sandbox and continuity carries via git
on the target branch.
