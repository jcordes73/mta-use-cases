# SOAP recipe — `citrus-ws`

> **Status: VERIFIED — validated live on the `jaxws-ejb` quickstart
> (skills-testing harness dry run, 2026-11-10, container lane):
> 2/2 green under `mvn verify -Pcitrus-container-tests`, server-side
> JaCoCo, 50% → 100% instruction of `EJB3Bean`. Every pitfall below
> carries an observed failure signature from that run. The simulation
> (`WebServiceServer`) side and the fault-assertion path remain
> unvalidated.**

## When the plan routes here

JAX-WS `@WebService` endpoints (in-JVM: simulate the service and drive
it with `soap-client`; container lane: drive the deployed endpoint).
Outbound SOAP calls the app makes → `soap-server` simulation.
**An EJB-as-`@WebService` (`@Stateless` + `@WebService`) has no in-JVM
lane** — it needs the EJB container + WS runtime, so route it straight
to the container lane (validated: jaxws-ejb).

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-ws</artifactId>
</dependency>
```
(already version-managed by the `citrus-bom` import the base recipe
adds). `citrus-validation-xml` — needed for XPath validation — comes
transitively with citrus-ws.

## Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public WebServiceClient soapClient() {
    return new WebServiceClientBuilder()
            .defaultUri("http://localhost:8080/EJB3Bean")
            .timeout(60000L)
            .build();
}

@BindToRegistry
public WebServiceServer soapServer() {   // simulation for outbound calls
    return new WebServiceServerBuilder()
            .port(8090)
            .autoStart(true)
            .build();
}
```
Client properties worth knowing (catalog): `faultStrategy`
(`propagate`/`assert` — how SOAP faults surface to the test),
`keepSoapEnvelope`, `messageFactory`.

## Endpoint URL discipline (observed 405 trap)

**Never derive the endpoint URL from the Maven artifact id.** jaxws-ejb's
`WEB-INF/jboss-web.xml` sets the context root to `/`, so the endpoint is
`http://localhost:8080/EJB3Bean` — the `/jaxws-ejb/…` guess fails fast
with `CitrusJUnitException: Method Not Allowed [405]` (and the WSDL
404s). Read `jboss-web.xml`/`web.xml` — or the WSDL's
`soap:address location="…"` — before writing the plan.

## Test skeleton (validated DSL)

The SOAP actions have their own `soap()` DSL (like `http()` — top-level,
`org.citrusframework.ws.actions.SoapActionBuilder`):

```java
runner.when(soap().client(client)
        .send()
        .message()
        .body("""
            <ns2:echo xmlns:ns2="http://wsejb.quickstarts.jboss.org/">
              <arg0>Citrus</arg0>
            </ns2:echo>
            """));

runner.then(soap().client(client)
        .receive()
        .message()
        .validate(xpath()
                .expression("//*[local-name()='return']",
                        "@contains('EJB3Bean returning: Citrus')@")));
```

The XPath validation DSL is
`org.citrusframework.xml.dsl.XpathSupport` (citrus-validation-xml —
transitively present via citrus-ws). There is **no**
`org.citrusframework.xml.dsl.XmlPathSupport` (compile error if you
guess it).

## RPC/literal payload shape (observed on jaxws-ejb)

With `@SOAPBinding(style = RPC)` (use = LITERAL default) and **no
`@WebParam`** on `echo(String input)`:

- the request part is named **`arg0`** and is **namespace-unqualified**,
  inside the `{target-ns}echo` wrapper:
  `<ns2:echo xmlns:ns2="http://wsejb.quickstarts.jboss.org/"><arg0>…</arg0></ns2:echo>`;
- the response is `{target-ns}echoResponse` with an unqualified
  `<return>` part;
- a wrong part name/namespace fails with a `soap:Client` fault:
  `Unmarshalling Error: unexpected element (uri:"", local:"input").
  Expected elements are <{}arg0>`.

When the wrapper/part names are not pre-known (JBossWS/CXF generates
them), validate content, not exact element names — namespace-agnostic
XPath `//*[local-name()='return']` with `@contains(...)@` — then pin
the exact shape from the observed payload.

## Validated pitfalls (each with its observed failure signature)

- **Endpoint URL ≠ artifact id** — context root comes from
  `jboss-web.xml` (`/` here); wrong URL → `Method Not Allowed [405]`,
  WSDL 404. See the URL discipline section.
- **RPC/literal `arg0` unqualified part** — wrong shape →
  `soap:Client` `Unmarshalling Error … Expected elements are <{}arg0>`.
  See the payload shape section.
- **XPath DSL import** — `org.citrusframework.xml.dsl.XpathSupport`,
  not `XmlPathSupport` (compile error).
- **Container-lane profile includes must not leak the module's own
  ITs** — jaxws-ejb's pre-existing `ClientIT` is JUnit 4 + needs
  `SERVER_HOST`; the profile's own `<includes>` (only the Citrus IT
  class) keeps it out.
- **Baseline attribution in the container lane** — the EJB container
  instantiates the bean at deployment, so a 0-test baseline still
  covers the bean skeleton (3/6 instructions on jaxws-ejb); the
  test-attributable delta is the method body (50% → 100%).
- Server boot stderr shows JaCoCo `IllegalClassFormatException` noise
  while instrumenting JDK bootstrap classes (`javax/sql/DataSource`,
  `java/sql/Connection`) — **non-fatal**; the exec data is written
  correctly.
- SOAP faults (with the default `faultStrategy`, a fault response may
  arrive as an exception rather than a receivable message — assert via
  `soap().client(...).receive().fault()` semantics or set the strategy
  explicitly): **unvalidated** — the fault contract was observed
  manually (curl) but not pinned in a test.
- The client strips the SOAP envelope by default (`keepSoapEnvelope`)
  — body validation targets the *payload*, not the envelope
  (docs-verified, consistent with the validated run).
- WS-Security headers: out of scope for the validated scope; secured
  `jaxws-*` variants route to the container lane.

## Harness validation checklist

- [x] container lane: client → deployed EJB endpoint round trip green
      (jaxws-ejb `EchoServiceSoapIT`, 2/2, server-side JaCoCo)
- [x] coverage quantified: 50% → 100% instruction of `EJB3Bean`
      (baseline: deployment-only, 0 tests)
- [x] failure signatures recorded above (405 URL trap, arg0
      unmarshalling error, XpathSupport import)
- [ ] in-JVM: client → embedded `WebServiceServer` simulation round
      trip — pending
- [ ] fault scenario asserted explicitly (`faultStrategy`/`receive()
      .fault()`) — pending
