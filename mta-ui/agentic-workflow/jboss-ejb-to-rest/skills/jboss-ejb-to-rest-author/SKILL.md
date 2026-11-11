---
name: jboss-ejb-to-rest-author
description: Stage 2 of 3 of the jboss-ejb-to-rest migration. Applies the
  provider-side (JAX-RS resources/facades over EJBs, OpenAPI) and consumer-side
  (MicroProfile REST Client, adapter seam) transformations to a JBoss EAP 8.0+
  codebase, one call site at a time, driven by the mapping tables in
  references/. Requires the inventory produced by the jboss-ejb-to-rest-plan
  skill; stage 3 is jboss-ejb-to-rest-verify.
license: Apache-2.0
metadata:
  stage: 2
  previous_skill: jboss-ejb-to-rest-plan
  next_skill: jboss-ejb-to-rest-verify
  language: java
  build_tool: "maven: mvn compile"
  guide_url: "README.md (repo-local: JBoss EAP EJB-to-REST refactoring guide)"
  generated_by: migration-skills-generator
  generated_at: "2026-11-10T00:00:00Z"
---

# JBoss EAP EJB-to-REST Migration — Stage 2: Author

Apply the transformations from the mapping tables in `references/`. Work **one
provider interface and one call site at a time** (strangler-fig, §4): add the
new path, cut over one call site, hand off to the Verify stage, repeat. **Never
modify or delete the old EJB/SOAP path** — the `@Remote` interface, SOAP
endpoints, and their clients stay supported (§4.4).

**Workflow position:** Stage 2 of 3. Requires the inventory and confirmed
migration order from the **`jboss-ejb-to-rest-plan`** skill. After each call-site
cutover, load the **`jboss-ejb-to-rest-verify`** skill to run the gates before
starting the next call site. Section references (§n) are citations into the
original guide; full provenance is in `references/sources.md`.

## Guardrails

- **The agent proposes, the human decides the seam.** Endpoint design (§2.3) and
  all semantic work (§5) are human decisions — draft, never finalize.
- **One call site at a time** (§4.2): each cutover is gated by the Verify stage —
  never batch-convert a whole application.
- **Old paths are off-limits** (§4.4): never delete the `@Remote` view, SOAP
  endpoints, remoting subsystem config, or EJB client artifacts — retirement is
  a separate, human, audited decision (§4.4 access-log audit).
- **Every change runs the contract tests** (§4.5, Verify stage) before it is
  committed; a failing gate means revert, not "fix forward".
- **Calibrate against the worked example** (`example/` in the guide repository):
  a well-calibrated transformation of `example/before/` matches `example/after/`.
- **Log every change** with the step number and gate results — each call-site
  pass appends to `.konveyor/handoff.md` (fixed format, see the handoff
  section below).

## Part 1: Dependencies (build-system)

1. Read `references/dependency-map.md` and apply each row.
2. Key changes:
   - Consumer side: plan the removal of `jboss-ejb-client` /
     `wildfly-ejb-client-bom` / ejb-client BOM imports — but only **after the last
     call site for that connection has cut over** (§4.2; deletion is a Verify-stage
     / cleanup action, gated by the §4.4 audit).
   - Consumers stop compiling against the shared remote-interface jar (`*-api`);
     the new client interfaces and DTOs come from the OpenAPI document instead
     (§3.7). Keep the shared jar available for consumers that never migrate.
   - Replace `wsimport`/`wsconsume` SOAP stubs with `openapi-generator-cli`
     output (`-g java --library microprofile -DuseJakartaEe=true`) at build time
     via the `openapi-generator-maven-plugin` — never hand-edit generated code
     (§3.7).

## Part 2: Provider-side transformation (§2)

1. Read `references/api-map.md` (provider rows) and `references/pattern-map.md`.
2. For each provider bean in the inventory, add a REST resource — do not touch the
   EJB:
   - `@Path("/api/v1/<noun>")` `@RequestScoped` resource injecting the bean's
     **local view** (`@EJB <Bean>Local`). Local calls are pass-by-reference and
     join the REST request's transaction — this keeps provider behavior identical
     (§2.5). If the bean has only a `@Remote` interface, add a `@Local` interface
     or `@LocalBean` (note: a `@Stateless` implementing an interface exposes no
     no-interface view without `@LocalBean`; without it injection fails at runtime
     with `WFLYEJB0406`).
   - **SOAP variant (§2.6)**: if the EJB carries `@WebService`, add only new
     classes. The REST facade calls the bean over its local view where available,
     falling back to the existing SOAP endpoint via `@WebServiceRef` only when no
     local view exists. Keep the choice in one injected field. Existing
     `@WebMethod` signatures are the endpoint-design blueprint.
   - Map exceptions to HTTP status codes with `jakarta.ws.rs.ext.ExceptionMapper`
     classes and a consistent JSON error body `{"code": "...", "message": "..."}`
     (§5.3 table: NotFound→404, Validation→400, Conflict→409,
     EJBAccessException→403, NoSuchEJBException/ConnectException→503, else 500).
   - Apply the §2.3 endpoint design table (`findX(id)`→`GET /api/v1/X/{id}`,
     `createX`→`POST`+201+Location, command ops→`POST .../activation` sub-resource,
     `@Asynchronous`→`202`+status URL). Idempotency for GET/PUT/DELETE; non-idempotent
     ops become `POST` (+`Idempotency-Key`). Paginate list endpoints. Annotate
     parameters with `@Valid`.
   - Optimistic locking: `@Version` checks become `ETag` on GET + `If-Match` on
     writes → `412 Precondition Failed`.
   - **Publish the OpenAPI document** (§2.7): static
     `src/main/resources/META-INF/openapi.yaml` (minimal-change option, merged with
     generated metadata) + the `microprofile-openapi-smallrye` subsystem on EAP XP.
     `operationId` values are load-bearing — treat a change as a breaking API
     change.
   - **Security (§2.4)**: keep `@RolesAllowed` on the resource; validate bearer
     tokens via MP-JWT (`web.xml` `<auth-method>MP-JWT</auth-method>` + subsystem +
     `mp.jwt.verify.*` config — note `@LoginConfig` does not exist in Jakarta EE
     10) or an Elytron `token-realm` (base EAP, no XP needed).

## Part 3: Consumer-side transformation (§3)

1. Read `references/api-map.md` (consumer rows) and `references/pattern-map.md`.
2. For each consumer call site in the inventory, **one at a time**:
   - Generate or write the REST client interface (`@RegisterRestClient`) mirroring
     the provider's OpenAPI document, with a MicroProfile
     `ResponseExceptionMapper` per business exception (`handles()` on status,
     `toThrowable()` — the server-side `ExceptionMapper` has **no effect** on a
     REST client). Configure `mp-rest/url`, `mp-rest/connectTimeout`,
     `mp-rest/readTimeout` per environment.
   - On base EAP (no XP), CDI injection of the client fails at deploy
     (`WELD-001408`) — build programmatically with `RestClientBuilder` instead
     (§3.2).
   - Keep the **adapter seam / anti-corruption layer** (§3.6): a thin
     `CustomerServiceAdapter implements CustomerService` that delegates to the REST
     client; business code keeps calling the old interface. This is also the
     rollback point — a config-selected `@Alternative` (or MP Config switch) lets
     you flip a misbehaving call site back to the EJB path without a code change
     (§4.2 step 5).
   - Replace `@EJB(lookup = "ejb:...")` / `InitialContext.lookup("ejb:...")` /
     `@WebServiceRef` with `@Inject @RestClient` behind the adapter. Signature of
     the adapter's business method must stay unchanged.
   - **Auth per request** (§3.5): service account + OAuth2 client-credentials
     bearer token (cache until expiry, refresh on 401) via a `ClientRequestFilter`,
     or mTLS. Agree the mechanism with the provider team; same on both sides.
   - Propagate a correlation ID (`X-Request-Id`) in the same filter (§5.8).

## Part 4: Config changes

1. Read `references/config-map.md` and apply each row.
2. Consumer: replace `jndi.properties` (`java.naming.provider.url=remote+http://...`)
   with MicroProfile Config entries per client (on base EAP: system properties /
   env vars).
3. Provider: `mp.jwt.verify.publickey.location` / `mp.jwt.verify.issuer`; check
   `web.xml` (`<security-constraint>`/`<login-config>` conflicts), `beans.xml`
   (`bean-discovery-mode="all"` so `@Provider` mappers register), `ejb-jar.xml`
   (`<business-remote>`, `<method-permission>`), `application.xml` (new WAR
   module), `jboss-deployment-structure.xml` (module dependencies for MP modules;
   shared API jar as module).

## Build gate and handoff

Run `mvn clean test-compile` (plus `mvn install` for the shared api jar first if
it changed). If it fails, load the **`jboss-ejb-to-rest-verify`** skill and
consult its `references/verify-errors.md` for known error→fix mappings before
attempting ad-hoc fixes.

Append an `## Author` section to `.konveyor/handoff.md`, one per call-site
pass, in the same fixed-format discipline as the `## Plan` section
(fixed headers, factual field lines, numbered exceptions):

    ## Author
    - Call site: <class/method cut over this pass>
    - Changes: <numbered list, with step numbers from this skill>
    - Build gate: pass | fail (<error summary>)
    - Iterations: <n build-gate runs for this pass, n failed — the failed
      ones by first error signature; loop length, for skill refinement>
    - Pitfalls hit: <verify-errors.md rows that fired, e.g.
      "WELD-001408", "WFLYEJB0406"; "none" if none — this is how the
      mapping tables and verify-errors.md learn which rows earn their place>
    - Deviations: none | <numbered deviations from the mapping tables in
      references/, each with the reason>
    - Verify: <verdict from the verify stage for this pass, once known>

**Commit** the code changes of the pass together with the updated
`.konveyor/handoff.md` — each stage runs in a fresh sandbox and continuity
carries via git on the target branch.

Then load **`jboss-ejb-to-rest-verify`** and run its per-call-site gates for the
call site(s) cut over in this pass before starting the next one.
