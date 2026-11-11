# Worked Example: Inter-Application Calls from EJB/SOAP to REST

> EXAMPLE ONLY — a minimal, illustrative sample project that pairs with the
> EJB-to-REST refactoring guide. It is a golden reference for calibrating
> (semi-)automated refactoring, not production code. The POMs target
> Jakarta EE 10 / JBoss EAP 8.0+ (using the JBoss EAP 8.1 BOMs from the Red Hat
> repository) and compile with Maven 3.9 / Java 21 (`mvn test-compile`). The
> after/ applications were also smoke-tested at runtime on base JBoss EAP
> 8.0.11 and 8.1 (provisioned via the OpenShift S2I builder images) — the
> runtime findings are in the guide (§2.4, §2.6, §3.1, §3.2). The integration
> tests (before/ old paths, after/ REST path) run against a provisioned EAP
> (`mvn test -Dcustomer.server=<host:port>`, see "Integration tests" below)
> and skip gracefully when no server is configured.

This example shows **one application pair before and after** the refactoring described
in the guide — a realistic starting point where **the guide's two starting points exist
as two couplings in the same system**:

- `customer-provider` — provides customer master data, exposed **both ways**:
  a `@Remote` EJB view **and** a `@WebService` SOAP endpoint (a common real-world shape).
- `order-consumer` — places orders and needs customer data, mixing **both couplings**:
  one call site on the remote EJB (shared `customer-api` jar, `@EJB(lookup=...)`),
  one on the SOAP client (WSDL-driven stubs, `@WebServiceRef`).
- `customer-api` — the shared jar with the `@Remote` interface and DTO. **This jar
  is the compile-time EJB coupling the migration narrows** (the SOAP coupling is the WSDL).

The guide's two starting points map onto this project as:

| Guide starting point | Shown in the example as |
|---|---|
| **Remote EJB** (`@Remote`, JNDI lookups — §1–§6 full path) | The `lookupCustomer` call site + the shared `customer-api` jar + the EJB-client artifacts |
| **EJB already exposed as SOAP** (`@WebService` — §2.6, REST added in addition) | The `placeOrder` call site + the `@WebService` endpoint + the WSDL |

## Directory layout

```
example/
├── before/                          # starting point: both couplings, no REST
│   ├── customer-api/                #   shared @Remote interface + DTO (the EJB coupling)
│   ├── customer-provider/           #   @Stateless bean, @Remote view + @WebService
│   │                                #   endpoint + WSDL — the bean exposed both ways
│   └── order-consumer/              #   MIXED couplings: @EJB(lookup=...) call site +
│                                    #   @WebServiceRef SOAP call site (wsconsume-style
│                                    #   stubs) + jboss-ejb-client.xml, jndi.properties
└── after/                           # target state: REST added, old paths kept
    ├── customer-api/                #   UNCHANGED — the remote view stays until the
    │                                #   §4.4 audit decides retirement separately
    ├── customer-provider/           #   @Remote view and SOAP endpoint UNTOUCHED (WSDL kept);
    │                                #   + rest/ package: JAX-RS facade over the LOCAL EJB
    │                                #     interface, ExceptionMapper, MP-JWT setup, and the
    │                                #     translatable design-work endpoints (§2.6)
    │                                #   + resources/META-INF/openapi.yaml (§2.7)
    └── order-consumer/              #   existing remote-EJB and SOAP call sites KEPT (§4.4);
                                     #   EJB-client artifacts KEPT while the remote path is
                                     #   in use; + rest/ client for NEW call sites
                                     #   (ReportingProcessor) + microprofile-config.properties
                                     #   + Citrus REST integration tests + dual-run
                                     #   contract test (all three paths)
```

## The delta (before → after)

| Concern | Before | After | Guide section |
|---|---|---|---|
| Call sites | `@EJB(lookup="ejb:...")` + `@WebServiceRef` SOAP stubs | Old call sites **kept**; NEW call sites (`ReportingProcessor`) use `@Inject CustomerServiceClient` | §3.1, §3.2, §2.6 |
| Contract | Shared `customer-api` jar (EJB) + WSDL (SOAP) | Published OpenAPI document — shipped as a static `META-INF/openapi.yaml` (§2.7); WSDL and api jar stay the contracts for the old paths | §2.7, §3.7 |
| Call-site config | `jboss-ejb-client.xml` + `jndi.properties` (cleanup targets, kept while the remote path is in use) | `microprofile-config.properties` for the NEW REST call sites (§3.2); EJB-client artifacts deleted only after the §4.4 audit | §3.2, §4.2, §6 |
| Errors | `throws CustomerNotFoundException` over remoting; `@WebFault` → SOAP fault → `*_Exception` | REST: 404 + JSON error body; client-side `ResponseExceptionMapper` re-raises a consumer-owned exception | §2.2, §3.2, §5.3 |
| Security | Caller propagation over remoting; SOAP endpoint's auth setup | MP-JWT bearer token, auth method declared in `web.xml` (`@LoginConfig` does not exist in EE 10), `@RolesAllowed` on the resource (§2.4, §3.5) | §2.4, §3.5 |
| Old paths | — | Both the `@Remote` view and the SOAP endpoint remain supported; retirement is a separate, audited decision (§4.4) | §4.4 |

## Translatable design work shown in code (after, §2.6)

Several SOAP capabilities translate, but not 1:1 — each needs explicit design work at
the new REST seam. `after/customer-provider/.../rest/` shows the patterns in code
(all NEW classes; the SOAP-exposed EJB is untouched):

| SOAP capability | Shown in | Design work demonstrated |
|---|---|---|
| MTOM/XOP binary attachments | `CustomerResource.uploadAvatar/getAvatar`, `AvatarStore` | Attachment as its own resource, `application/octet-stream`, streaming with a size limit before buffering, object store instead of in-memory attachments |
| `@Oneway` fire-and-forget | `CustomerResource.requestWelcomePack`, `WelcomePackServiceBean` | `202 Accepted` + `Location` header pointing at a status resource; work runs in an `@Asynchronous` EJB method (§5.7); consumer polls |
| Strict XSD schema validation | `AddressChangeRequest`, `ValidationExceptionMapper` | Bean Validation constraints on the DTO, `@Valid` on the resource method, `ConstraintViolationException` → 400 with the §5.3 JSON error body |
| SOAP handler chains | `CorrelationIdFilter` | Handler-chain entry → `ContainerRequestFilter`/`ContainerResponseFilter` at the HTTP-header level; correlation propagation (§5.8) |

The consumer side mirrors this in `after/order-consumer/.../rest/CustomerServiceClient.java`:
octet-stream upload (no MTOM encoding) and a raw-`Response` method for the 202 + polling pattern.

Capabilities with **no** JAX-RS equivalent (WS-Security message-level security,
WS-ReliableMessaging, XTS transactions, WS-Addressing) are documented in §2.6 —
consumers depending on them stay on SOAP (§4.4).

## Integration tests (§4.5)

Both `order-consumer` trees have **Citrus-based** integration tests.

### Before: the two old paths

The before/ tests cover the **two old paths** the BEFORE consumer uses — the
executable baseline the after/ contract test (§4.5) compares the REST path against:

| Test | Path exercised | Assertions |
|---|---|---|
| `RemoteEjbIntegrationTest` | `@EJB`-style remote lookup over remoting (`remote+http`, JNDI `ejb:` URL — §10.3), run as a Citrus **custom action** (Citrus has no native EJB endpoint — it tests message transports) | DTO for a known id; the **checked** `CustomerNotFoundException` for an unknown id (with its `customerId`); deterministic error contract across repeated calls |
| `SoapIntegrationTest` | Citrus **SOAP client** at the message level (spring-ws based): send the request payload, receive/validate the response envelope, BASIC credentials as a **preemptive `Authorization` header** on a `HttpUrlConnectionMessageSender` subclass (§10.3) | DTO fields in the `findCustomerResponse` for a known id; the SOAP **fault** for an unknown id (asserted as `SoapFaultClientException`, §5.3) |

The SOAP tests pin the **actual wire shapes** of the endpoint (the WSDL contract, §2.6):
request `<ns:findCustomer xmlns:ns="http://acme.com/customer"><arg0>…</arg0>…`, response
`findCustomerResponse` with the DTO fields — no stub-based client, mirroring how a Citrus
contract test treats a message interface.

**Running them** (against the §10.2/§10.3 smoke-test setup — provider deployed,
shared API jar installed as a server module, `ejb-http-invoker` and `webservices`
layers provisioned):

```
mvn test -Dcustomer.server=localhost:8080
```

- Without `-Dcustomer.server` the tests **skip** (JUnit `assumeTrue` guard) — plain
  `mvn test` and `mvn test-compile` work everywhere.
- Optional overrides: `-Dcustomer.deployment=<runtime-name without .war>`,
  `-Dcustomer.test.user=<user>`, `-Dcustomer.test.password=<password>`. The defaults
  match the §10.3 smoke-test deployment (`customer-provider-soap-2.0.0`,
  `testuser` / `testpassword1!` with role `customer-reader`).
- Citrus version: **4.10.3** (`citrus-junit5` + `citrus-ws` + `httpclient5`, test
  scope) — Citrus 4.x targets Jakarta EE 10 / Java 17+, matching the example stack.
  Citrus 5.x is out (JUnit 6 / Spring 7 stack) — a version bump would also change the
  JUnit runtime.
- The remote-EJB tests need the **full EJB client stack** on the test classpath:
  `jboss-ejb-client` alone does not pull the WildFly naming client, elytron, xnio,
  remoting, discovery, marshalling, or the transaction client — the §4.2 version-locked
  coupling, machine-visible. These are declared as BOM-managed **test-scope**
  dependencies in `before/order-consumer/pom.xml` (version-free entries resolved by the
  imported EAP BOM).

These tests encode the exact error shapes the migration must preserve (§4.5, §5.3):
the checked exception over remoting and the SOAP fault on the WSDL path. When the
REST path replaces either, the after/ contract test must show the same observable
behavior for the same inputs.

### After: the NEW REST path

The after/ `order-consumer` mirrors this with **REST tests** — the third leg of the
§4.5 dual-run (the after/ contract test compares all three paths):

| Test | Path exercised | Assertions |
|---|---|---|
| `RestEndpointIntegrationTest` | Citrus **HTTP client** at the message level: the provider's REST facade as an external consumer sees it (§2.2/§2.6) — the executable version of the REST rows of the §10.3 matrix | Unauthenticated call → **401**; known id → **200** + JSON DTO; unknown id → **404** + the NEW JSON error body (`CUSTOMER_NOT_FOUND`, `customerId`) + machine-readable `X-Customer-Id` header (§2.2, §5.3) |
| `RestClientIntegrationTest` | The consumer-side **MP REST Client** path (§3.2), via the programmatic `RestClientBuilder` fallback — the variant that deploys and works on base EAP, unlike the `@Inject` variant (XP-only, `WELD-001408`, §3.1) | Known id returns the **consumer-owned** DTO (no shared jar — the contract is the OpenAPI document, §2.7); a 404 is re-raised by the client-side `ResponseExceptionMapper` as the consumer-owned checked exception with the `X-Customer-Id`-derived id (§3.2, §5.3) |

Same invocation model as the before/ tests (`mvn test -Dcustomer.server=localhost:8080`,
same defaults, skip without the flag) against the §10.2/§10.3 smoke-test setup with the
after/ provider deployed. Auth notes:

- The smoke-test setup secures the REST path with the web.xml **BASIC overlay** (§10.3);
  in production it uses **MP-JWT bearer tokens** (§2.4, §3.5) — on base EAP (no XP)
  MP-JWT cannot deploy (§3.1). Both tests show where auth attaches: a request header
  (Citrus) / a `ClientRequestFilter` (MP REST Client) — the same seam a bearer token
  would use (§3.5).
- The standalone MP REST Client implementation (`org.jboss.resteasy.microprofile:
  microprofile-rest-client` + `resteasy-json-binding-provider`, test scope) is only
  needed because the test runs outside the container; in deployment the server provides
  it (§3.2). The `io.smallrye.config:smallrye-config` test dependency is the MP Config
  implementation the REST Client resolves at runtime outside the container (optional in
  the REST Client's own POM — the version it was built against).
- Error-contract note: the provider's stub fabricates a customer for **any positive
  id** — its error contract is "id ≤ 0" — so the 404 tests use a **negative** id
  (matching the before/ baseline tests). In production, the id contract is whatever
  the discovery step (§1) established.

### Unit tests and the coverage gate (§9.2)

Both consumer trees also have **pure-JVM unit tests** (no server needed — plain
`mvn test` runs them everywhere):

- The client-side error translation (`CustomerNotFoundResponseExceptionMapper` — the
  §3.2/§5.3 mapper branches), the consumer-owned exception and DTO types, and the
  processors (`OrderProcessor`, `ReportingProcessor`) with **test doubles** injected
  into the container-managed fields (`FieldInjector`) — delegation and checked-exception
  propagation, no JBoss needed.
- **Coverage gate** (JaCoCo in both consumer POMs, enforced on `mvn test`): the
  hand-written classes must stay at **≥ 95% line coverage** (currently 100%).
- **Generated code is excluded from the measurement**: the wsconsume-generated SOAP
  stubs (`com.acme.order.soap.*`) are not covered by the bar — they are verified by the
  integration tests against the live WSDL (§4.5), not by unit tests.
- Report: `target/site/jacoco/index.html` after a test run.

## Not shown (deliberately)

- **§1 Discovery and §9 automation**: processes and commands, not file trees — the
  discovery grep patterns and machine-checkable gates live in §1.1/§9.2 of the guide;
  the contract tests here are the verification part of them.
- **§7 Monolith splitting**: a **second phase**, out of scope for this example — the
  `after/` tree stops at the first-phase refactor; §7 of the guide is the split playbook.
- **Sequencing/parallel run**: the `after/` tree shows the end state of one seam. The
  intermediate states (all call paths live side by side, per-call-site cutover) are a
  process, not a file tree — see §4.
- **Transactions spanning applications**: `placeOrder` deliberately does not require an
  atomic customer update — such flows must be redesigned before migrating (§5.2).

## How to use it as a golden reference

1. Treat `before/` as the input state and `after/` as the expected output state for the
   exact same functionality — an automated refactoring (or an agent) is well-calibrated
   when its output for a given input matches the delta described in the table above.
2. The `after/customer-provider/src/main/java/com/acme/customer/provider/rest/` package is
   the reference for provider-side transforms; `after/order-consumer/src/main/java/com/acme/order/rest/`
   for consumer-side transforms.
3. `before/order-consumer/src/test/java/com/acme/order/` holds the runnable
   integration tests for the two old paths (remote EJB, SOAP) — the executable
   baseline; `after/order-consumer/src/test/java/com/acme/order/` holds the REST
   integration tests for the new path (HTTP-level endpoint rows + the §3.2
   consumer-side MP REST Client leg); `after/order-consumer/src/test/java/com/acme/order/OrderContractTest.java`
   is the contract-test skeleton (§4.5) used as the acceptance gate between migration
   steps — it dual-runs all three paths (remote EJB, SOAP, REST), and the before/
   and after/ integration tests are the reference for how each path is exercised
   and asserted.
