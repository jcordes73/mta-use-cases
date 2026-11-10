# citrus-tests skill dry-run protocol

Skills are static SKILL.md prompts — they are not trained. Refinement
happens by **executing them and folding the friction back in**. This
directory makes that loop repeatable: it holds the tooling and the
procedure used to validate the citrus-tests skills locally before a
skill change is committed and the agent image rebuilt.

## Tooling

- `citrus-doctor.sh` (repo root, baked into the agent image at
  `/usr/local/bin/citrus-doctor.sh`) — pre-flight checks distilled from
  every discover-fail cycle the dry runs and cluster runs produced: proxy
  reachability, Java baseline vs Citrus line, effective surefire
  (version floor, restrictive includes), checkstyle wiring, BOM-managed
  jackson line, the jandex 2.x/3.x clash, JUnit 5 engine presence, and
  the container profile's plugin/channels prerequisites. Three modes
  matching the author stage's timeline:

      citrus-doctor.sh pre       <app-path>   # before authoring
      citrus-doctor.sh deps      <app-path>   # after pom edits, before 1st test run
      citrus-doctor.sh container <app-path>   # before the container profile

  Exit 0/2/1 = clear / warnings / failures. New skill lessons that are
  mechanically checkable should become a doctor check *and* a recipe, so
  the next run detects instead of rediscovers.

- `mcp-client.py` — minimal stdio MCP client. Drives the same
  `citrus-mcp-server` uber-jar the agent image bakes:

      export CITRUS_MCP_JAR=/tmp/opencode/citrus-mcp/citrus-mcp-server-5.1.0-runner.jar
      python3 mcp-client.py citrus_catalog_endpoints '{"filter": "http"}'
      python3 mcp-client.py citrus_docs_page '{"kind": "runtime", "searchKey": "jupiter"}'

  Fetch the jar from Maven Central once
  (`org/citrusframework/citrus-mcp-server/<version>/citrus-mcp-server-<version>-runner.jar`).

- A local clone of the target application repository (e.g.
  `~/jboss-quickstarts-ssh`), with workstation `java` + `mvn` available.

## The protocol

For each skill change (or after a cluster run produced handoff
deviations):

1. **Mirror the cluster's execution context.** The harness clones the
   repository's *default branch* — check out exactly the commit the
   cluster used (or the default-branch tip) into a scratch branch
   `konveyor/citrus-tests-local`.

2. **Execute the plan skill.** Perform its steps by hand (or direct an
   agent to): inventory the application, run the MCP queries the skill
   names, choose lane/version, write `docs/citrus-test-plan.md`,
   commit. Record every point where the skill text was wrong, missing,
   or where you needed knowledge the skill does not contain.

3. **Execute the author skill.** Apply the pom changes, implement the
   tests (use the MCP client for the catalog consultations the skill
   mandates), commit. Same recording.

4. **Execute the verify skill.** Run the verification command from
   clean (in-JVM lane: JaCoCo-wrapped `mvn test`; container lane:
   `mvn verify -Pcitrus-container-tests`), parse the coverage CSV,
   write the `## Verify` handoff section. Same recording.

5. **Fold the findings back.** Each finding becomes either
   - a skill edit (if generic — recipes, failure signatures, tool
     mechanics), or
   - a README lesson (if environment- or application-specific),
   then commit and rebuild the agent image
   (`oc start-build mta-citrus-tests-agent -n openshift-mta
   --from-dir=mta-ui/agentic-workflow/citrus-tests`).

## Findings this protocol has produced

- Jackson 3 annotations pin (`2.22`) — caught as a real defect of the
  protocol's own authoring (compiled fine, failed at runtime)
- Catalog `version` argument broken for foreign versions
- docs-tools substring mechanics (`kind`/`searchKey`)
- Container lane: plugin-version floor, pluginRepositories, channel
  manifest, `provisioning-dir` mirror, failed-resolution caching
  (five defects in one dry run)
- Data-backed REST cluster run (`citrus-kitchensink-gc492`, verified,
  6/6, 0% → 53.3% instruction): jpamodelgen/hibernate-core
  nearest-wins scope crash (cold compiles only — now a doctor `deps`
  check + recipe), Weld 5 client-proxy laziness vs `@PostConstruct` PU
  bootstrap, `addAlternative()` needing `addBeanClasses()` (three
  defects the local warm run could not surface)

## Draft recipes pending validation

`citrus-test-author/recipes/` carries four DRAFT recipes
(ftp-sftp, graphql, kafka, jmx-rmi-ssh),
grounded in the Citrus 5.1.0 MCP catalog + docs but not validated
live. The harness earns each one its verified banner: run the
scenario on a matching quickstart (candidates:
`spring-kafka`-style producers for Kafka),
fold observed failure signatures into the recipe, flip the banner.
The REST/container/JMS recipes are the validated template for what
that output should look like.

**First promotion (2026-11-10): WebSocket.** Dry run on
`websocket-hello` (2/2 green, 0% → 100% instruction of `HelloName`)
promoted `recipes/websocket.md` to verified. Findings: citrus's
`WebSocketClient` hardcodes a `permessage-deflate` handshake offer
(built from Jetty 12 core internal classes) that Tyrus rejects with a
PROTOCOL_ERROR close — the receive then silently times out; strip
`WebSocketHttpHeaders` after building the client. A stale
`junit-jupiter-engine:5.9.1` pin crashed the surefire fork
(`NoClassDefFoundError OutputDirectoryCreator`) because
citrus-junit-jupiter 5.1.0 pulls JUnit 6.1.3 — same nearest-wins trap
class as the hibernate-core crash. Tyrus `Server` takes endpoint
classes (annotation path), not `ServerEndpointConfig`. And `-Dtest`
exclusion filters override surefire's default includes, dragging the
module's failsafe ITs into surefire (they fail on missing
`SERVER_HOST`) — exclude every IT class explicitly when baselining.

**Second promotion (2026-11-10): SOAP — first container-lane
exercise.** Dry run on `jaxws-ejb` (2/2 green under
`mvn verify -Pcitrus-container-tests`, server-side JaCoCo, 50% → 100%
instruction of `EJB3Bean` — deployment itself covers the bean
skeleton) promoted `recipes/soap.md` to verified and exercised the
validated helloworld-rs container profile as a copy-template for the
first time. Findings: the planned endpoint URL was wrong —
`jboss-web.xml` sets the context root to `/`, so the endpoint is
`http://localhost:8080/EJB3Bean`, and a wrong URL fails fast with
`Method Not Allowed [405]` (never derive the endpoint from the
artifact id; read `jboss-web.xml`/`web.xml` or the WSDL
`soap:address`). RPC/literal with no `@WebParam`: the request part is
`arg0`, namespace-unqualified — wrong shape → `soap:Client`
`Unmarshalling Error … Expected elements are <{}arg0>`. The XPath
validation DSL is `org.citrusframework.xml.dsl.XpathSupport`
(citrus-validation-xml, transitive via citrus-ws). JaCoCo
`IllegalClassFormatException` noise on JDK bootstrap classes during
server boot is non-fatal. Doctor pre/deps/container all behaved as
expected on the first container-lane exercise (checkstyle +
failsafe-includes WARNs only).

**Third promotion (2026-11-10): Mail — the recording-FacesContext
pattern.** Dry run on `mail` (5/5 green under plain `mvn test` with
checkstyle on, 0% → 54.6% instruction of `Email` — 24/25 methods, 100%
of all lane-reachable code; baseline 0 tests / 0%, no SE warm-up)
promoted `recipes/mail.md` to verified. Findings: the EAP BOM pins
`jackson-annotations` 2.18.4 while citrus-mail's Jackson 3 needs the
2.22 line (`NoClassDefFoundError JsonSerializeAs` — the doctor's
jackson check class again). A manually built `MailServer` needs
`initialize()` before `start()` or `deliver()` NPEs on the null
endpoint adapter, and `autoStart(true)` + manual `start()` double-binds
the port (GreenMail's "Could not start … set server startup timeout"
message is misleading). The forwarded payload is a **single line** with
its own XML declaration and no cc/bcc elements, validation is
positional text equality that does not honor `@ignore@`, and a
~250-char template line crashes checkstyle 8.5 ("Range out of bounds")
— build templates by concatenation. Faces-dependent beans need a
recording `FacesContext` (no-op subclass + protected static
`setCurrentInstance`) for BOTH paths — `send()` swallows exceptions
into Faces messages, so a failed send is otherwise invisible. And the
confirmed scope limit: the citrus mail server is SMTP-only, so the
app's POP3/IMAP read path is covered via error paths only.

## Doctor regression sweep

When the doctor itself changes, sweep the whole target repo once —
every check in every mode, against every project. Cost: `pre` and
`container` are grep-only (seconds per project); `deps` pays one
`mvn dependency:list` per project (~45 s each, ~35 min for all 47
quickstarts):

    for d in ~/jboss-quickstarts-ssh/*/; do
      [ -f "$d/pom.xml" ] || continue
      citrus-doctor.sh pre "$d"; citrus-doctor.sh deps "$d"; citrus-doctor.sh container "$d"
    done

Baseline (2026-11-10, doctor as of build 13): 47/47 projects exit
`deps` clean; `pre` reports only the parent-pom checkstyle WARN
(universal here); `container` flags missing `<channels>` on 2 EJB-only
modules (ejb-multi-server, ejb-txn-remote-call); 6 modules carry
`org.jboss:jandex` (helloworld-rs, jaxrs-client, jaxrs-jwt, logging,
thread-racing, todo-backend) — the clash check arms there once the
data-backed recipe adds Weld 5. Anything beyond this baseline after a
doctor edit is a regression, not a discovery.

The pattern: a skill edit is only trustworthy once a dry run executed
the edited text end-to-end. Never rebuild the image from unedited
aspiration.
