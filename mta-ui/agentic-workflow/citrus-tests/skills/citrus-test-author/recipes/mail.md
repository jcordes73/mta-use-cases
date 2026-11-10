# Mail recipe — `citrus-mail`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, endpoint properties, builder/DSL verified against the 5.1.0
> endpoint-mail docs); NOT validated live.** The pitfalls below are
> *expected*, not observed. Validate through the `skills-testing/`
> harness (e.g. the `mail` quickstart) before trusting; fold observed
> failure signatures back in and flip this banner to verified.

## When the plan routes here

Applications that send mail via Jakarta Mail (`Session`, `Transport`).
The app keeps its real mail session code; the test points it at the
Citrus SMTP simulator via test-scoped configuration.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-mail</artifactId>
</dependency>
```

## Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public MailClient mailClient() {
    return new MailClientBuilder()
            .host("localhost")
            .port(25025L)
            .build();
}

@BindToRegistry
public MailServer mailServer() {   // SMTP simulator
    return new MailServerBuilder()
            .port(25025L)
            .autoStart(true)
            .autoAccept(true)          // skip the accept handshake by default
            .build();
}
```
Catalog properties: `authRequired`, `knownUsers`, `splitMultipart`,
`javaMailProperties`.

## Test skeleton

With `autoAccept(true)`, receive the mail as an XML-normalized
`mail-message` (namespace `http://www.citrusframework.org/schema/mail/message`):

```java
$(receive(mailServer).message().body("""
    <mail-message xmlns="http://www.citrusframework.org/schema/mail/message">
      <from>from@example.org</from>
      <to>to@example.org</to>
      <subject>@contains(Welcome)@</subject>
      <body>
        <contentType>text/plain; charset=utf-8</contentType>
        <content>Hello!</content>
      </body>
    </mail-message>
    """));
```

Advanced (autoAccept off): the server first receives an
`accept-request` and the test answers with an `accept-response`
(`<accept>true</accept>`); a scripted negative response (`mail-response`
with `<code>443</code>`) simulates SMTP errors.

## How the app is pointed at the simulator (in-JVM lane)

The application builds its `Session` from configuration — override the
mail host/port via test-scoped properties (e.g. a test
`mail.properties`/system properties `mail.smtp.host=localhost`,
`mail.smtp.port=25025`) so no source change is needed. Confirm the
quickstart's exact wiring during planning.

## Expected pitfalls (UNVALIDATED — confirm each in the harness run)

- The app may cache `Session`/`Transport` at startup — if mail is sent
  from a `@PostConstruct`, the simulator must be up before the
  deployment bootstraps (order the `@BeforeAll` accordingly).
- Multipart mails need `splitMultipart(true)` on the server or the
  body arrives as a single part.
- Subject/body matchers: the XML fields accept the usual Citrus
  matchers (`@contains()@`) — element-wise, per the JSON lesson.

## Harness validation checklist

- [ ] app → simulator round trip green in-JVM
- [ ] multipart + negative-SMTP scenario if the plan asks for it
- [ ] failure signatures recorded below (none yet)
