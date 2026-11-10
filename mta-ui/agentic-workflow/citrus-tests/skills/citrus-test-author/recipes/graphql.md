# GraphQL recipe — `citrus-graphql`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, endpoint properties, `GraphQlEndpoints` builders and the
> `graphql().client()` query/variable/data DSL verified against the
> 5.1.0 endpoint-graphql docs); NOT validated live.** The pitfalls below
> are *expected*, not observed. Validate through the `skills-testing/`
> harness on a real quickstart before trusting this recipe; fold
> observed failure signatures back in and flip this banner to verified.

## When the plan routes here

Quarkus/SmallRye GraphQL or WildFly GraphQL applications. In-JVM: the
Citrus GraphQL *server* simulates the upstream GraphQL service the app
queries; the test's `GraphQlClient` drives the app's own GraphQL API.
No JBoss quickstart currently ships GraphQL — route here only for
applications in scope that expose it.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-graphql</artifactId>
</dependency>
```

## Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
private final GraphQlServer bookServer = GraphQlEndpoints.graphql().server()
        .port(8080)
        .autoStart(true)
        .schema("classpath:graphql/schema.graphqls")
        .build();

@BindToRegistry
private final GraphQlClient bookClient = GraphQlEndpoints.graphql().client()
        .requestUrl("http://localhost:8080")
        .schema("classpath:graphql/schema.graphqls")
        .build();
```

The `schema` is the application's SDL — both sides use it; the server
validates incoming operations against it, the client builds proper
GraphQL request documents.

## Test skeleton

```java
$(graphql().client(bookClient)
    .send()
    .query("query Book($id: ID!) { book(id: $id) { title } }")
    .operationName("Book")
    .variable("id", "${id}"));

$(graphql().client(bookClient)
    .receive()
    .data("$.book.title", "Dune")
    .message()
    .status(200));
```

Server-side simulation mirrors it: `graphql().server(...)` receives an
operation (`receive().query(...)`), validates it against the schema,
then `send().data(...)` scripts the response — validate the operation
before responding (vacuous-simulation rule).

`strict` (endpoint property) toggles strict document/response checks —
with `strict` the response must match the schema's declared fields.

## Expected pitfalls (UNVALIDATED — confirm each in the harness run)

- Missing schema file: both client and server fail on a missing
  `classpath:graphql/schema.graphqls` — ship the app's real SDL as a
  test resource.
- `data()` jsonPath expressions are against the GraphQL `data` object —
  `$.book.title`, not `$.data.book.title`.
- The HTTP status of a GraphQL response is 200 even on operation errors
  — assert errors via the `errors` payload, not the status code.
- Variables must match the SDL's declared types (`ID!` vs `String`)
  or the server rejects the operation before the handler runs.

## Harness validation checklist

- [ ] client → app round trip green (or app → simulated server)
- [ ] error-path scenario asserted via `errors`, not status
- [ ] failure signatures recorded below (none yet)
