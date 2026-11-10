# Kafka recipe — `citrus-kafka`

> **Status: VERIFIED — validated live through the `skills-testing/`
> harness on `microprofile-reactive-messaging-kafka` (EAP XP 6.0 line,
> in-JVM lane): 6/6 green under plain `mvn test` with checkstyle on,
> reproduced from clean twice; JaCoCo MessagingBean 7/7 methods.**
> The pitfalls below are *observed* failure signatures, not
> expectations. Quickstart commits: plan `7e627d55a`, author
> `81157033e`, verify `2e307fdcf`.

## When the plan routes here

Kafka producers/consumers in the application. **Citrus ships an embedded
Kafka broker** (`EmbeddedKafkaServerBuilder`), so a real produce→broker→
consume round trip is viable in the sandbox with no external broker.
Scope note for the plan: the smallrye connector hop (the app producing
*through* the WildFly-wired connector) stays out of in-JVM reach — the
lane exercises the app's own contributions (key/metadata construction,
serializer classes) and the round trip over the embedded broker.
Container lane with a real broker (Strimzi/AMQ charts) is the fallback
for full-connector coverage.

## Dependencies (test-scoped, BOM-managed where possible)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-kafka</artifactId>
    <scope>test</scope>
</dependency>
<!-- engine for the provided MP reactive-streams-operators API if the app
     exposes PublisherBuilder channel methods -->
<dependency>
    <groupId>io.smallrye.reactive</groupId>
    <artifactId>mutiny-reactive-streams-operators</artifactId>
    <version>3.1.1</version>
    <scope>test</scope>
</dependency>
<!-- jackson-annotations 2.22 pin — same citrus Jackson-3 conflict as every lane -->
```

Plus the version pins below — on EAP XP-line modules the BOM conflicts
are the rule, not the exception.

## Embedded broker (verified DSL)

```java
kafkaServer = new EmbeddedKafkaServerBuilder()
        .topics("testing", "roundtrip")   // multiple topics OK
        .kafkaServerPort(probedFreePort)  // probe with ServerSocket(0)
        .build();
kafkaServer.initialize();   // @BeforeAll
// ... @AfterAll: kafkaServer.stop();
```

- **Manual lifecycle is `initialize()` ONLY** — it starts the broker AND
  creates the configured topics (verified in 5.1.0 bytecode:
  `initialize()` → `start()` → `createKafkaTopics`). A second `start()`
  re-binds the port. This is the *opposite* pattern from MailServer,
  which needs `initialize()` then `start()` with `autoStart(false)`.
- The broker writes its log dir to the JVM temp dir and deletes it on
  exit — no cleanup needed.
- Port: probe a free port in `@BeforeAll`; one broker per suite, no
  parallel tests (same-JVM port discipline).

## Endpoint construction (5.1.0 builders — verified)

```java
KafkaEndpoint producer = new KafkaEndpointBuilder()
        .topic("testing")
        .server("localhost:" + brokerPort)
        .keySerializer(IntegerSerializer.class)       // Class overload exists
        .valueSerializer(TimedEntrySerializer.class)  // or FQN string via setValueSerializer
        .timeout(10000L)
        .build();

KafkaEndpoint consumer = new KafkaEndpointBuilder()
        .topic("testing")
        .server("localhost:" + brokerPort)
        .consumerGroup("citrus-kafka-<unique>")       // fresh per test
        .offsetReset("earliest")
        .keyDeserializer(IntegerDeserializer.class)
        .valueDeserializer(TimedEntryDeserializer.class)
        .build();
```

Real catalog properties (5.1.0): `topic`, `server`/`bootstrapServers`,
`consumerGroup`, `offsetReset` (default **earliest**), `partition`,
`autoCommit`/`autoCommitInterval`, `keySerializer`/`keyDeserializer`/
`valueSerializer`/`valueDeserializer`, `timeout` (default 5000),
`consumerProperties`/`producerProperties`, `useThreadSafeConsumer`,
`clientId`, `messageConverter`, `headerMapper`. (An earlier draft
listed `key`/`autoOffsetReset`/`allowNonEmptyKeys` — those do not exist
in 5.1.0.)

## Send / receive

Kafka is **asynchronous only** (docs constraint — verified). The record
key travels as the `citrus_kafka_messageKey` header
(`KafkaMessageHeaders.MESSAGE_KEY`); `KafkaMessage` has fluent
`.messageKey(Object)` / `.topic(...)` / `.partition(...)`:

```java
runner.then(send(producerEndpoint).message(new KafkaMessage(entry)   // payload object
        .messageKey(integerKey)));

runner.then(receive(consumerEndpoint)
        .validate((message, context) -> {          // ValidationProcessor — see pitfalls
            TimedEntry received = (TimedEntry) message.getPayload();
            assertEquals(expected, received.getMessage());
            assertEquals(integerKey, message.getHeaders().get(KafkaMessageHeaders.MESSAGE_KEY));
            assertEquals("testing", message.getHeaders().get(KafkaMessageHeaders.TOPIC));
        }));
```

## Observed pitfalls (validated signatures)

- **SmallRye ships its own `org.eclipse.microprofile.reactive.messaging.Message`**
  inside `io.smallrye.reactive:smallrye-reactive-messaging-api`, with
  `getMetadata(Class)`/`addMetadata` — the spec API (MP RM 3.0.1) dropped
  metadata methods (only `unwrap` remains). `smallrye-reactive-messaging-kafka-api`'s
  `KafkaMetadataUtil` bytecode invokes the SmallRye variants → in a plain
  spec-API test JVM every `KafkaMetadataUtil` call dies with
  `NoSuchMethodError`. Fix: add `smallrye-reactive-messaging-api`
  (WildFly-runtime line, e.g. 4.24.0.redhat-00001) **test-scoped and
  declared BEFORE the spec API** so its `Message` wins the test classpath.
  This is also how the deployed app works in WildFly.
- **citrus-kafka 5.1.0's embedded server is Kafka 4.4.0**: the BOM-pinned
  older kafka-clients (3.9.1.redhat) wins as the direct declaration →
  `NoSuchMethodError ConfigDef$ValidList.anyNonDuplicateValues(boolean,
  boolean)` in `@BeforeAll`. Pin kafka-clients to the server's line
  (4.4.0) in module dependencyManagement — provided scope, so WildFly
  still supplies its own client at deployment runtime.
- **kafka-server 4.4.0 needs log4j-api >= 2.25** (`FlowMessageFactory.INSTANCE`);
  EAP BOMs pin 2.23.x.redhat → `NoSuchFieldError ... FlowMessageFactory
  INSTANCE` at broker start. Pin log4j-api to the log4j-core line (2.25.5).
- **Per-test topic + consumer group**: with `offsetReset("earliest")` a
  shared topic delivers earlier tests' records to later tests' fresh
  consumers. Give each test its own topic (or a selector) and a distinct
  consumer group.
- **Non-text payloads**: Kafka record values that are not text (here:
  Java-serialization bytes from the app's own serializer) cannot be
  body-template validated. Set the endpoint's value deserializer to the
  app's class and validate via `receive(...).validate((message, context)
  -> ...)` on the deserialized object. Set the KEY serializers to match
  the app's key type (IntegerSerializer/IntegerDeserializer here) — the
  default StringSerializer throws ClassCastException on Integer keys.
- **provided-scope API mechanics**: `PublisherBuilder.toList()` returns a
  `CompletionRunner` — execute with `run()`, not `toCompletableFuture()`;
  and executing it needs a reactive-streams engine test-scoped.
- The jackson-annotations 2.22 pin (doctor deps check) applies here too.

## Harness validation checklist

- [x] app produces → test receives, with payload + key header validation
      (`sendToKafka` metadata contract → embedded broker → app
      deserializer)
- [x] serializer round-trip independent of the app path (own topic)
- [x] embedded broker port conflict-free (probed) across the suite
- [x] channel-chain methods without the broker (reactive-streams engine)
- [x] failure signatures recorded above
