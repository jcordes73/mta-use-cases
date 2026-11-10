# Kafka recipe — `citrus-kafka`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, `KafkaEndpointBuilder`, `EmbeddedKafkaServerBuilder` and the
> "asynchronous communication only" constraint verified against the
> 5.1.0 endpoint-kafka docs); NOT validated live.** The pitfalls below
> are *expected*, not observed. Validate through the `skills-testing/`
> harness on a real quickstart before trusting this recipe; fold
> observed failure signatures back in and flip this banner to verified.

## When the plan routes here

Kafka producers/consumers in the application. The big upgrade vs the
plan skill's earlier "possible-but-fragile" note: **Citrus ships an
embedded Kafka broker** (`EmbeddedKafkaServerBuilder`), so a real
round-trip test is viable in the sandbox with no external broker.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-kafka</artifactId>
</dependency>
```

## Embedded broker for the sandbox (docs-verified DSL)

```java
@BindToRegistry
public EmbeddedKafkaServer kafkaServer() {
    return new EmbeddedKafkaServerBuilder()
            .topics("hello")
            .kafkaServerPort(9091)
            .build();
}
```

The embedded broker writes to the JVM temp directory and deletes it on
exit — no cleanup needed.

## Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public KafkaEndpoint helloKafkaEndpoint() {
    return new KafkaEndpointBuilder()
            .topic("hello")
            .server("localhost:9091")   // bootstrap servers
            .consumerGroup("citrus-group")
            .build();
}
```

Catalog properties: `partition`, `key`, `consumerGroup`,
`autoOffsetReset`, `allowNonEmptyKeys`.

## Test skeleton

Kafka is **asynchronous communication only** (docs: "In the current
version Citrus supports asynchronous communication only") — no
request/reply semantics; every side is `send` or `receive`:

```java
// produce (or receive what the app produced)
$(receive(helloKafkaEndpoint)
    .message()
    .type(MessageType.JSON)
    .body("...expected record value...")
    .header("kafka_key", "..."));   // validation contract, never bare

$(send(helloKafkaEndpoint)
    .message()
    .body("...record the test injects..."));
```

## Expected pitfalls (UNVALIDATED — confirm each in the harness run)

- **Consumer offset timing**: a `receive` issued after the record was
  produced can miss it if the consumer group's offsets are committed —
  use a fresh `consumerGroup` per test (or `autoOffsetReset=earliest`).
- The embedded broker needs a free port — pick a high, configurable
  port via a test property; two tests on 9091 in the same JVM must not
  run in parallel.
- Kafka headers are string-typed; `kafka_key` header validation compares
  serialized bytes — use matchers (`@contains()@`) for non-trivial keys.
- The app's bootstrap-servers config must be overridden to point at
  `localhost:9091` (test-scoped properties); the app's real wiring may
  cache the producer at bootstrap — same ordering caveat as the mail
  recipe.
- First record after broker start can hit the metadata-propagation
  delay — a short explicit poll timeout on the first test is normal.

## Harness validation checklist

- [ ] app produces → test receives, with payload + header validation
- [ ] test produces → app consumes (if the app has a consumer)
- [ ] embedded broker port conflict-free across the suite
- [ ] failure signatures recorded below (none yet)
