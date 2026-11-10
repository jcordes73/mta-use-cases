# SOAP recipe — `citrus-ws`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, endpoint properties, builder/DSL verified against the 5.1.0
> endpoint-soap docs); NOT validated live.** The pitfalls below are
> *expected*, not observed. Validate through the `skills-testing/`
> harness on a real quickstart (e.g. `helloworld-ws`, `jaxws-retail`)
> before trusting this recipe; fold observed failure signatures back
> in and flip this banner to verified.

## When the plan routes here

JAX-WS `@WebService` endpoints (in-JVM: simulate the service and drive
it with `soap-client`; container lane: drive the deployed endpoint).
Outbound SOAP calls the app makes → `soap-server` simulation.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-ws</artifactId>
</dependency>
```
(already version-managed by the `citrus-bom` import the base recipe adds).

## Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public WebServiceClient soapClient() {
    return new WebServiceClientBuilder()
            .defaultUri("http://localhost:8090/test")
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

## Test skeleton

The SOAP actions have their own `soap()` DSL (like `http()` — top-level):

```java
$(soap().client(soapClient)
    .send()
    .message()
    .soapAction("MyService/sayHello")
    .body("""
        <HelloRequest xmlns="http://example.org/soap">…</HelloRequest>
        """));

$(soap().client(soapClient)
    .receive()
    .message()
    .body("""
        <HelloResponse xmlns="http://example.org/soap">…</HelloResponse>
        """));
```

Server-side simulation — validate the incoming request before
responding (vacuous-simulation rule):

```java
$(receive(soapServer).message()
    .soapAction("MyService/sayHello")
    .body("…validate request XML…"));
$(send(soapServer).message().body("…scripted response…"));
```

## Expected pitfalls (UNVALIDATED — confirm each in the harness run)

- SOAP faults: with the default `faultStrategy`, a fault response may
  arrive as an exception rather than a receivable message — assert
  faults via `soap().client(...).receive().fault()` semantics or set
  the strategy explicitly.
- The client strips the SOAP envelope by default (`keepSoapEnvelope`)
  — body validation targets the *payload*, not the envelope.
- Namespace strictness: XML payload validation is namespace-sensitive;
  copy the exact targetNamespace from the WSDL.
- WS-Security headers are out of scope for the draft; if the quickstart
  uses them (`jaxws-*` secured variants), route to the container lane.

## Harness validation checklist

- [ ] in-JVM: client → embedded `WebServiceServer` round trip green
- [ ] fault scenario asserted explicitly
- [ ] failure signatures recorded below (none yet)
