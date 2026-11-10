# WebSocket recipe — `citrus-websocket`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, endpoint properties, client/server builders and the
> `citrus_websocket_is_last` header verified against the 5.1.0
> endpoint-websocket docs); NOT validated live.** The pitfalls below are
> *expected*, not observed. Validate through the `skills-testing/`
> harness on a real quickstart (e.g. `websocket-hello`) before trusting
> this recipe; fold observed failure signatures back in and flip this
> banner to verified.

## When the plan routes here

JSR-356 `@ServerEndpoint` / `@ClientEndpoint` usage, or any
bidirectional HTTP-upgrade channel. In-JVM: the Citrus WebSocket
*server* simulates the WS endpoint and the app's client connects to it,
or the test's `WebSocketClient` drives the app's `@ServerEndpoint`
bootstrapped in-process. Container lane: drive the deployed endpoint.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-websocket</artifactId>
</dependency>
```

## Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public WebSocketClient helloWebSocketClient() {   // drive the app's endpoint
    return new WebSocketClientBuilder()
            .requestUrl("http://localhost:8080/hello")
            .timeout(5000L)
            .build();
}

@BindToRegistry
public WebSocketServer helloWebSocketServer() {   // simulate the WS server
    return new WebSocketServerBuilder()
            .port(8080)
            .webSockets(Arrays.asList(new WebSocketConfig("/hello")))
            .autoStart(true)
            .build();
}
```

Note the server carries its socket path registrations through
`webSockets(Arrays.asList(...))` — a plain port is not enough; each
registered socket path becomes a routable endpoint
(`<server-id>/<path>`).

## Test skeleton

```java
$(send(helloWebSocketClient).message().body("Hello WS!"));

$(receive(helloWebSocketClient)
    .message()
    .type(MessageType.PLAINTEXT)
    .body("Hello from the endpoint")
    .validate(...)); // never a bare receive - validation contract
```

Fragmented/streamed sends are split by setting the
`citrus_websocket_is_last` header: `false` for all but the final
fragment, `true` on the last one. Receiving merges the fragments.

## Expected pitfalls (UNVALIDATED — confirm each in the harness run)

- **Missing `webSockets(list)` on the server** — the server starts but
  has no registered paths; connections to the path fail with a 404 on
  the upgrade handshake.
- Path routing on the server side goes through the socket path, not the
  endpoint id — the test must receive/send on
  `helloWebSocketServer/<path>`.
- In-JVM lane: the app's `@ServerEndpoint` must be discovered by a WS
  container that an SE bootstrap provides — check whether the quickstart
  wires a `ServerContainer` in `@BeforeAll`; otherwise the endpoint
  client connects to the container-lane deployment instead.
- The client `timeout` is the handshake/response budget — a WS endpoint
  that only replies on a broadcast trigger hangs until timeout.

## Harness validation checklist

- [ ] text round trip green in-JVM
- [ ] fragmented send (`citrus_websocket_is_last`) if the plan needs it
- [ ] failure signatures recorded below (none yet)
