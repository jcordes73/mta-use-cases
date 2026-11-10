# Mail recipe — `citrus-mail`

> **Status: VERIFIED — validated live on the `mail` quickstart
> (skills-testing harness dry run, 2026-11-10, in-JVM lane):
> 5/5 green under plain `mvn test` with checkstyle on, JaCoCo from
> clean, 0% → 54.6% instruction of `Email` (24/25 methods — 100% of all
> code reachable in the lane; the missed instructions are the POP3/IMAP
> inbox-listing path the simulator cannot serve). Every pitfall below
> carries an observed failure signature from that run. The accept-handshake
> (`autoAccept(false)`) and multipart paths remain unvalidated.**

## When the plan routes here

Applications that send mail via Jakarta Mail (`Session`, `Transport`).
The app keeps its real mail session code; the test points it at the
Citrus SMTP simulator via test-scoped configuration.

**Scope limit (observed + documented)**: the Citrus mail server is
**SMTP-only** — the docs are explicit ("just an SMTP mail server…,
persisting in a user inbox is not part of our business") and the catalog
confirms (`smtp` property, no POP3/IMAP serving). An app's POP3/IMAP
**read** path cannot be served positively; cover it via the error path
(store connect against a dead port) and state the limit in the plan.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-mail</artifactId>
</dependency>
<!-- jakarta.mail-api is provided-scope in the quickstarts: Transport/Store
     have NO runtime provider on the test classpath without this -->
<dependency>
    <groupId>org.eclipse.angus</groupId>
    <artifactId>angus-mail</artifactId>
    <version>2.0.5</version> <!-- upstream line, not EAP-BOM-managed -->
    <scope>test</scope>
</dependency>
<!-- citrus-bom's Jackson 3 (tools.jackson) needs the Jackson-3-compatible
     annotations line; the EAP BOM pins an older Jackson 2 line -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>2.22</version>
    <scope>test</scope>
</dependency>
```

Import the citrus-bom **after** the EAP BOM (its JUnit 6.x management
must win the parent's stale Jupiter pin — doctor check 8b).

## Endpoint construction (5.1.0 builders)

Built manually in `@BeforeAll` (probed free port), NOT via
`@BindToRegistry` — and then the lifecycle must be driven explicitly:

```java
mailServer = new MailServerBuilder()
        .port(smtpPort)
        .autoAccept(true)
        .autoStart(false)   // see pitfall 2: double-bind trap
        .build();
mailServer.initialize();    // wires the default queue-based adapter
mailServer.start();
```

Catalog properties: `authRequired`, `knownUsers`, `splitMultipart`,
`javaMailProperties`.

## Test skeleton

With `autoAccept(true)`, receive the mail as an XML message. The
template must be **byte-exact** (observed: validation is positional text
equality via `DefaultTextEqualsMessageValidator` — no `@ignore@`
support, whitespace-sensitive):

```java
runner.then(receive().endpoint(mailServer)
        .message()
        .body("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<mail-message xmlns=\"http://www.citrusframework.org/schema/mail/message\">"
                + "<from>user01@james.local</from>"
                + "<to>user02@james.local</to>"
                + "<subject>Greetings from Citrus</subject>"
                + "<body><contentType>text/plain; charset=us-ascii</contentType>"
                + "<content>Hello mail simulation!</content></body>"
                + "</mail-message>")
        .header("citrus_mail_from", "user01@james.local")
        .header("citrus_mail_to", "user02@james.local")
        .header("citrus_mail_subject", "Greetings from Citrus"));
```

Headers (`citrus_mail_from/to/cc/bcc/subject/replyTo/date/message_id`)
are validated exactly and are the safest contract surface; the payload
shape is pinned in the pitfalls below. Advanced (autoAccept off): the
server first receives an `accept-request` and the test answers with an
`accept-response` (`<accept>true</accept>`); a scripted negative
`mail-response` (`<code>443</code>`) simulates SMTP errors — unvalidated.

## How the app is pointed at the simulator (in-JVM lane)

For a backing bean whose `Session` is `@Resource`-injected: the bean is
a POJO in SE — instantiate directly and **reflectively inject** a plain
`Session.getInstance(props)` (`mail.smtp.host`/`mail.smtp.port` at the
simulation; POP3/IMAP props at a dead port with short
`connectiontimeout`/`timeout` for error paths). Beans that build their
own `Session` from configuration: override via test-scoped properties
instead. Faces-dependent beans also need a recording `FacesContext`
(no-op subclass recording `addMessage`, installed via the protected
static `setCurrentInstance`) — required for BOTH success and failure
paths, because beans swallow exceptions into Faces messages (see
pitfall 5).

## Validated pitfalls (each with its observed failure signature)

1. **jackson-annotations pin** — EAP BOM pins 2.18.4; citrus-mail's
   Jackson 3 (`tools.jackson`) needs the 2.22 line. Signature: every
   test dies at startup with
   `NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs`
   from `MailMarshaller.<init>`.
2. **Lifecycle: `initialize()` before `start()`; `autoStart(false)`** —
   a manually built server needs `initialize()` (an
   `InitializingPhase` normally invoked by the framework) or
   `deliver()` NPEs: `Cannot invoke "EndpointAdapter.getEndpoint()"
   because "this.endpointAdapter" is null`. And with
   `autoStart(true)` + manual `start()` the server binds twice →
   GreenMail `Could not start mail server smtp:127.0.0.1:<port>, try to
   set server startup timeout > 2000` — **misleading message; the real
   cause is the double bind**, not the timeout.
3. **Payload template must be byte-exact, single line** — the marshaller
   emits its own XML declaration
   (`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`)
   immediately followed by the elements (no newline), `<cc>`/`<bcc>`
   elements **absent** (not empty), and a platform charset on the
   content type (`text/plain; charset=us-ascii` for ASCII bodies).
   Signatures: `message payload not equal at position 2 expected
   'mail-message…' but was '?xml version…'` (missing declaration),
   `position 37 … expected '?>\n<mail-message' but was
   ' standalone="yes"?><mail-'` (declaration shape),
   `position 56 …` (whitespace), `position 246 expected '@ignore@…'`
   (the validator does not honor `@ignore@`).
4. **Checkstyle parser crash on very long lines** — a ~250-char
   single-line template crashes checkstyle 8.5 with
   `Range [131, 25) out of bounds for length 25`. Build the expected
   payload with string concatenation keeping lines short (no text-block
   line breaks inside the template).
5. **Faces-dependent beans need a recording FacesContext for both
   paths** — every `FacesContext.getCurrentInstance()` touch NPEs in SE
   (`addMessage` on null context), and beans like `Email` swallow
   exceptions into Faces error messages, so without a recording context
   a failed send is indistinguishable from a successful one. Pattern:
   no-op subclass overriding `addMessage` into a static list, installed
   via the protected static `setCurrentInstance` in `@BeforeEach`
   (jakarta.faces-api 4.0.1 exposes it; 20 abstract methods to stub).
6. **jakarta.mail-api is provided-scope** — without a test-scoped impl
   (angus-mail), `Transport.send`/`getStore` fail with
   `NoSuchProviderException` at runtime, not at compile time.

## Harness validation checklist

- [x] in-JVM lane: bean → SMTP simulation round trip green (send path)
- [x] error paths: send failure + POP3/IMAP retrieve failures green
  (dead-port Sessions + recorded Faces error messages)
- [x] coverage quantified: 0% → 54.6% instruction of `Email` (100% of
  lane-reachable code; baseline 0 tests / 0% — no SE warm-up)
- [x] failure signatures recorded above (Jackson pin, adapter NPE,
  double-bind GreenMail timeout, byte-exact payload, checkstyle crash,
  FacesContext recording, angus-mail impl)
- [ ] multipart (`splitMultipart(true)`) + accept-handshake /
  negative-SMTP scenarios — unvalidated
