---
name: jboss-ejb-to-rest-verify
description: Stage 3 of 3 of the jboss-ejb-to-rest migration. Runs the
  machine-checkable postcondition gates, contract tests, runtime error triage
  (references/verify-errors.md), and the cleanup checklist after call-site
  cutovers performed by the jboss-ejb-to-rest-author skill in a JBoss EAP 8.0+
  codebase.
license: Apache-2.0
metadata:
  stage: 3
  previous_skill: jboss-ejb-to-rest-author
  language: java
  build_tool: "maven: mvn compile"
  guide_url: "README.md (repo-local: JBoss EAP EJB-to-REST refactoring guide)"
  generated_by: migration-skills-generator
  generated_at: "2026-11-10T00:00:00Z"
---

# JBoss EAP EJB-to-REST Migration — Stage 3: Verify

Each step is "done" only when its postconditions hold. Verification is the gate
between per-call-site cutovers — never skip it to save time.

**Workflow position:** Stage 3 of 3. Runs after cutovers made by the
**`jboss-ejb-to-rest-author`** skill — after every single call-site cutover, and
once more as the final cleanup pass per application. Section references (§n) are
citations into the original guide (see the `guide_url` metadata).

## 1. Per-call-site cutover gates (§9.3 step 4)

After each single call-site cutover, all must hold:

- [ ] Contract tests for the operation pass against **BOTH** paths (old EJB call
      and new REST endpoint) with the same test data — payloads match
      field-by-field (§4.5)
- [ ] Error scenarios map to the agreed HTTP statuses and are handled
      (every row of the §5.3 error table, plus a 401 — never test only the happy
      path)
- [ ] `grep -rn "ejb:" src/` no longer matches the cut-over call site
- [ ] Call-site signature unchanged (`git diff` shows no method-signature change)
- [ ] Auth works for the service identity
- [ ] Latency within tolerance (REST + JSON adds serialization overhead)

If the provider is deployed, also check: `curl -s -o /dev/null -w '%{http_code}'
<base>/openapi` returns 200 (XP) or the manual fallback document is served, and
`git diff` on the `@Remote` bean is empty (no changes to the old path).

## 2. Runtime error triage

Consult `references/verify-errors.md` for the known error→cause→fix table before
attempting ad-hoc fixes. Key runtime findings (verified on EAP 8.0.11 / 8.1):
`EJBCLIENT000409`, `WELD-001408`, `The required mechanism 'MP-JWT' is not
available`, `WFLYEJB0406`, and `NoClassDefFoundError` for the shared api module.

Reproduce whole flows, not unit slices: endpoint matrix against a provisioned
server (401 unauthenticated / 200 authenticated with DTO / OpenAPI availability /
WSDL still 200 on the SOAP variant), consumer-side probe that performs the exact
call site and returns result or exception (this is how the §3.1/§3.2 findings were
produced).

## 3. Consumer-side testing (§4.5)

- Unit tests mock the adapter; integration tests of the adapter use a stub REST
  endpoint (WireMock) serving the payloads from the OpenAPI document, including
  every error case and a 401.
- Optional: consumer-driven contract tests (Pact-style) derived from the OpenAPI
  document.
- Coverage gate pattern: enforce a JaCoCo `check` rule on hand-written classes
  (≥95% line), **excluding generated code** (wsconsume stubs, generated clients) —
  generated code is verified by integration tests, not the coverage bar.

## 4. Behavioral changes to verify manually

These come from the migration's semantic gotchas (§5) — code may compile and pass
tests while the behavior differs. Confirm each with the user:

- **Transactions no longer span applications** (§5.2): cross-app flows need
  remote-call-last ordering, idempotency, compensation/saga/LRA; never rely on
  the consumer's `@Transactional` wrapping a REST call
- **JSON is less strict than Java serialization** (§5.1): null-vs-absent fields,
  ISO-8601 dates, enum names, `BigDecimal` as string — pinned on both sides
- **Idempotency** (§2.3/§5.2): GET/PUT/DELETE idempotent; non-idempotent POSTs
  carry `Idempotency-Key`, implemented properly (replay returns original outcome)
- **Optimistic locking** (§2.3): `ETag`/`If-Match` → `412 Precondition Failed`
- **Async methods** (§5.7): `202` + status URL polling wrapped in the adapter;
  consumers never block indefinitely
- **Latency** (§5.5): batch endpoints replace per-item EJB loops; payload sizes
  trimmed
- **Observability** (§5.8): correlation ID / trace context propagates across the
  boundary and is logged on both sides

## 5. Cleanup checklist (§6) — only when all call sites of an app have cut over

- [ ] All `@EJB` remote injections and `InitialContext` lookups replaced
- [ ] `jboss-ejb-client.xml`, `jndi.properties` deleted
- [ ] `jboss-ejb-client` / `wildfly-ejb-client-bom` dependencies removed from `pom.xml`
- [ ] Deployment descriptors audited: `web.xml` (JAX-RS activation, security
      constraints scoped), `beans.xml` (discovery mode), `ejb-jar.xml`/`jboss-ejb3.xml`,
      `application.xml`, `jboss-deployment-structure.xml`
- [ ] Consumer-side timeout/retry/auth config present for every client
- [ ] Contract and error-path tests in place for every migrated operation
- [ ] Correlation ID or tracing propagates across the application boundary
- [ ] OpenAPI contract published by provider; consumer client interfaces validated
      against it
- [ ] SOAP variant: REST facade added alongside the untouched `@WebService` EJB;
      existing SOAP interface, WSDL, and clients verified as still working
- [ ] IIOP: `iiop-configuration` / `jacorb` / `iiop-openjdk` removed only after all
      IIOP clients cut over

**Hard rule:** all of the following stay until a separate, human, audited §4.4
decision (access-log audit over a window covering every consumer's usage pattern,
including batch and month-end jobs — silence in the logs is what makes removal
safe): the `@Remote` interface and its exposure, SOAP endpoints and WSDL,
`remote-outbound-connection` and remoting security realms, shared api jar.

## 6. Report to user and handoff

Append a `## Verify` section to `.konveyor/handoff.md`, one per run, in the
same fixed-format discipline as the `## Plan` and `## Author` sections
(fixed headers, factual field lines, numbered exceptions). It must end with a
verdict line in exactly one of these forms:

    ## Verify
    - Call sites verified: <list, with gate results pass/fail>
    - Cleanup checklist: pass | fail (<missing items>) | n/a (call sites remain)
    - Behavioral checks pending: <numbered list, or none>
    - Exceptions: <numbered list, or none>

    Verdict: verified
    Verdict: verified with exceptions (<numbered list follows)

- List all changes made, per call site, with gate results
- Flag items that require human decision: §1.3 order confirmations, endpoint
  design reviews (§2.3), transaction redesigns (§5.2), the eventual §4.4 retirement
- Note any behavioral changes from step 4 that the user should verify manually
- Record everything in the migration log (step numbers + gate results)

**Commit** the updated `.konveyor/handoff.md` (and any doc-only artifacts from
this stage) so the next sandbox picks up the verdict — continuity carries via
git on the target branch.

**Handoff:** if gates failed → back to **`jboss-ejb-to-rest-author`** with the
failing gate and the matched `verify-errors.md` row. If gates passed and call
sites remain → back to **`jboss-ejb-to-rest-author`** for the next call site. If
all call sites are done and cleanup passed → migration complete; only the §4.4
retirement audit remains (human decision).
