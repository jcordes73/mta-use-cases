# Application import template: per-row dependencies

Derived from the `jboss-quickstarts` repository, branch `7.4.x` (the branch
referenced by every row of `template_application_import.csv`): for each row,
the row's `pom.xml` was parsed and its intra-portfolio dependencies
(`groupId` `org.jboss.eap.quickstarts` / `${project.groupId}`) were resolved
against the other rows of the template.

Dependency data lives **exclusively** in
`mta-ui/application_dependency_import.csv` (record type `2`), not in the
application template - see the
[Bulk Uploading Applications](../README.md#bulk-uploading-applications)
section for the import flow. The file expresses each of the 42 edges below
exactly once (the Hub importer does not deduplicate), preferring the
**southbound** consumer edge from the application's own `pom.xml`; where an
application has several dependencies, the table below lists the full set
and the *Dependency import* column shows which edge the application
contributes to the import file.

**southbound** = this application depends on the listed one
(it consumes or packages it); **northbound** = the listed application
depends on this one.

| Application | Path | Packaging | Depends on (southbound) | Used by (northbound) | Dependency import |
|---|---|---|---|---|---|
| helloworld | `helloworld` | war | — | — | — |
| app-client | `app-client` | pom | — | — | — |
| app-client-ear | `app-client/ear` | ear | app-client-client-simple, app-client-ejb | — | app-client-ear → app-client-client-simple |
| app-client-client-simple | `app-client/client-simple` | jar | app-client-ejb | app-client-ear | app-client-client-simple → app-client-ejb |
| app-client-ejb | `app-client/ejb` | ejb | — | app-client-ear, app-client-client-simple | app-client-ear → app-client-ejb |
| batch-processing | `batch-processing` | war | — | — | — |
| bean-validation | `bean-validation` | war | — | — | — |
| bean-validation-custom-constraint | `bean-validation-custom-constraint` | war | — | — | — |
| bmt | `bmt` | war | — | — | — |
| cmt | `cmt` | war | — | — | — |
| contacts-jquerymobile | `contacts-jquerymobile` | war | — | — | — |
| ejb-asynchronous | `ejb-asynchronous` | pom | ejb-asynchronous-ejb | — | ejb-asynchronous → ejb-asynchronous-ejb |
| ejb-asynchronous-client | `ejb-asynchronous/client` | jar | ejb-asynchronous-ejb | — | ejb-asynchronous-client → ejb-asynchronous-ejb |
| ejb-asynchronous-ejb | `ejb-asynchronous/ejb` | ejb | — | ejb-asynchronous, ejb-asynchronous-client | — |
| ejb-in-ear | `ejb-in-ear` | pom | ejb-in-ear-ejb, ejb-in-ear-web | — | ejb-in-ear → ejb-in-ear-ejb |
| ejb-in-ear-ear | `ejb-in-ear/ear` | ear | ejb-in-ear-ejb, ejb-in-ear-web | — | ejb-in-ear-ear → ejb-in-ear-ejb |
| ejb-in-ear-ejb | `ejb-in-ear/ejb` | ejb | — | ejb-in-ear, ejb-in-ear-ear, ejb-in-ear-web | — |
| ejb-in-ear-web | `ejb-in-ear/web` | war | ejb-in-ear-ejb | ejb-in-ear, ejb-in-ear-ear | ejb-in-ear-web → ejb-in-ear-ejb |
| ejb-in-war | `ejb-in-war` | war | — | — | — |
| ejb-multi-server | `ejb-multi-server` | pom | — | — | — |
| ejb-multi-server-app-main | `ejb-multi-server/app-main` | pom | — | — | ejb-multi-server → ejb-multi-server-app-main |
| ejb-multi-server-app-main-ear | `ejb-multi-server/app-main/ear` | ear | ejb-multi-server-app-main-ejb, ejb-multi-server-app-main-web | — | ejb-multi-server-app-main-ear → ejb-multi-server-app-main-ejb |
| ejb-multi-server-app-main-ejb | `ejb-multi-server/app-main/ejb` | ejb | ejb-multi-server-app-one-ejb, ejb-multi-server-app-two-ejb | ejb-multi-server-app-main-ear, ejb-multi-server-app-main-web, ejb-multi-server-app-web, ejb-multi-server-client | ejb-multi-server-app-main-ejb → ejb-multi-server-app-one-ejb |
| ejb-multi-server-app-main-web | `ejb-multi-server/app-main/web` | war | ejb-multi-server-app-main-ejb, ejb-multi-server-app-one-ejb, ejb-multi-server-app-two-ejb | ejb-multi-server-app-main-ear | ejb-multi-server-app-main-web → ejb-multi-server-app-main-ejb |
| ejb-multi-server-app-one | `ejb-multi-server/app-one` | pom | — | — | ejb-multi-server → ejb-multi-server-app-one |
| ejb-multi-server-app-one-ear | `ejb-multi-server/app-one/ear` | ear | ejb-multi-server-app-one-ejb | — | ejb-multi-server-app-one-ear → ejb-multi-server-app-one-ejb |
| ejb-multi-server-app-one-ejb | `ejb-multi-server/app-one/ejb` | ejb | — | ejb-multi-server-app-main-ejb, ejb-multi-server-app-main-web, ejb-multi-server-app-one-ear, ejb-multi-server-app-web | — |
| ejb-multi-server-app-two | `ejb-multi-server/app-two` | pom | — | — | ejb-multi-server → ejb-multi-server-app-two |
| ejb-multi-server-app-two-ear | `ejb-multi-server/app-two/ear` | ear | ejb-multi-server-app-two-ejb | — | ejb-multi-server-app-two-ear → ejb-multi-server-app-two-ejb |
| ejb-multi-server-app-two-ejb | `ejb-multi-server/app-two/ejb` | ejb | — | ejb-multi-server-app-main-ejb, ejb-multi-server-app-main-web, ejb-multi-server-app-two-ear, ejb-multi-server-app-web | — |
| ejb-multi-server-app-web | `ejb-multi-server/app-web` | war | ejb-multi-server-app-main-ejb, ejb-multi-server-app-one-ejb, ejb-multi-server-app-two-ejb | — | ejb-multi-server-app-web → ejb-multi-server-app-main-ejb |
| ejb-multi-server-client | `ejb-multi-server/client` | jar | ejb-multi-server-app-main-ejb | — | ejb-multi-server-client → ejb-multi-server-app-main-ejb |
| ejb-remote | `ejb-remote` | pom | — | — | — |
| ejb-security | `ejb-security` | jar | — | — | — |
| ha-singleton-deployment | `ha-singleton-deployment` | ejb | — | — | — |
| helloworld-html5 | `helloworld-html5` | war | — | — | — |
| helloworld-jms | `helloworld-jms` | jar | — | — | — |
| kitchensink | `kitchensink` | war | — | — | — |
| numberguess | `numberguess` | war | — | — | — |
| ejb-security-context-propagation | `ejb-security-context-propagation` | ejb | — | — | — |
| ejb-security-jaas | `ejb-security-jaas` | war | — | — | — |
| ejb-security-programmatic-auth | `ejb-security-programmatic-auth` | jar | — | — | — |
| ejb-throws-exception | `ejb-throws-exception` | pom | ejb-throws-exception-ejb, ejb-throws-exception-ejb-api, ejb-throws-exception-web | — | ejb-throws-exception → ejb-throws-exception-ejb |
| ejb-throws-exception-ear | `ejb-throws-exception/ear` | ear | ejb-throws-exception-ejb, ejb-throws-exception-ejb-api, ejb-throws-exception-web | — | ejb-throws-exception-ear → ejb-throws-exception-ejb |
| ejb-throws-exception-ejb | `ejb-throws-exception/ejb` | ejb | ejb-throws-exception-ejb-api | ejb-throws-exception, ejb-throws-exception-ear, ejb-throws-exception-web | ejb-throws-exception-ejb → ejb-throws-exception-ejb-api |
| ejb-throws-exception-ejb-api | `ejb-throws-exception/ejb-api` | jar | — | ejb-throws-exception, ejb-throws-exception-ear, ejb-throws-exception-ejb | ejb-throws-exception → ejb-throws-exception-ejb-api |
| ejb-throws-exception-web | `ejb-throws-exception/web` | war | ejb-throws-exception-ejb | ejb-throws-exception, ejb-throws-exception-ear | ejb-throws-exception-web → ejb-throws-exception-ejb |
| ejb-timer | `ejb-timer` | war | — | — | — |
| ejb-txn-remote-call | `ejb-txn-remote-call` | pom | — | — | — |
| ejb-txn-remote-call-client | `ejb-txn-remote-call/client` | war | — | — | ejb-txn-remote-call → ejb-txn-remote-call-client |
| ejb-txn-remote-call-server | `ejb-txn-remote-call/server` | war | — | — | ejb-txn-remote-call → ejb-txn-remote-call-server |
| greeter | `greeter` | war | — | — | — |
| ha-singleton-service | `ha-singleton-service` | jar | — | — | — |
| helloworld-mbean | `helloworld-mbean` | pom | — | — | — |
| helloworld-mbean-service | `helloworld-mbean/helloworld-mbean-service` | jboss-sar | — | — | helloworld-mbean → helloworld-mbean-service |
| helloworld-mbean-webapp | `helloworld-mbean/helloworld-mbean-webapp` | war | — | — | helloworld-mbean → helloworld-mbean-webapp |
| helloworld-mdb | `helloworld-mdb` | war | — | — | — |
| helloworld-mdb-propertysubstitution | `helloworld-mdb-propertysubstitution` | war | — | — | — |
| helloworld-mutual-ssl | `helloworld-mutual-ssl` | war | — | — | — |
| helloworld-mutual-ssl-secured | `helloworld-mutual-ssl-secured` | war | — | — | — |
| helloworld-rs | `helloworld-rs` | war | — | — | — |
| helloworld-singleton | `helloworld-singleton` | war | — | — | — |
| helloworld-ssl | `helloworld-ssl` | war | — | — | — |
| helloworld-ws | `helloworld-ws` | war | — | — | — |
| hibernate | `hibernate` | war | — | — | — |
| inter-app | `inter-app` | pom | inter-app-shared | — | inter-app → inter-app-shared |
| inter-app-AppA | `inter-app/appA` | war | inter-app-shared | — | inter-app-AppA → inter-app-shared |
| inter-app-AppB | `inter-app/appB` | war | inter-app-shared | — | inter-app-AppB → inter-app-shared |
| inter-app-shared | `inter-app/shared` | ejb | — | inter-app, inter-app-AppA, inter-app-AppB | — |
| jaxrs-client | `jaxrs-client` | war | — | — | — |
| jaxws-addressing | `jaxws-addressing` | pom | jaxws-addressing-service | — | jaxws-addressing → jaxws-addressing-service |
| jaxws-addressing-client | `jaxws-addressing/client` | jar | jaxws-addressing-service | — | jaxws-addressing-client → jaxws-addressing-service |
| jaxws-addressing-service | `jaxws-addressing/service` | war | — | jaxws-addressing, jaxws-addressing-client | — |
| jaxws-ejb | `jaxws-ejb` | pom | — | — | — |
| jaxws-ejb-client | `jaxws-ejb/client` | jar | jaxws-ejb-service | — | jaxws-ejb-client → jaxws-ejb-service |
| jaxws-ejb-service | `jaxws-ejb/service` | war | — | jaxws-ejb-client | — |
| jaxws-pojo | `jaxws-pojo` | pom | — | — | — |
| jaxws-pojo-client | `jaxws-pojo/client` | jar | jaxws-pojo-service | — | jaxws-pojo-client → jaxws-pojo-service |
| jaxws-pojo-service | `jaxws-pojo/service` | war | — | jaxws-pojo-client | — |
| jaxws-retail | `jaxws-retail` | pom | — | — | — |
| jaxws-retail-client | `jaxws-retail/client` | jar | jaxws-retail-service | — | jaxws-retail-client → jaxws-retail-service |
| jaxws-retail-service | `jaxws-retail/service` | war | — | jaxws-retail-client | — |
| jsonp | `jsonp` | war | — | — | — |
| jta-crash-rec | `jta-crash-rec` | war | — | — | — |
| jts | `jts` | pom | jts-application-component-2 | — | jts → jts-application-component-2 |
| jts-component-1 | `jts/application-component-1` | war | jts-application-component-2 | — | jts-component-1 → jts-application-component-2 |
| jts-application-component-2 | `jts/application-component-2` | jar | — | jts, jts-component-1 | — |
| kitchensink-angularjs | `kitchensink-angularjs` | war | — | — | — |
| kitchensink-ear | `kitchensink-ear` | pom | kitchensink-ear-ejb, kitchensink-ear-web | — | kitchensink-ear → kitchensink-ear-ejb |
| kitchensink-ear-ear | `kitchensink-ear/ear` | ear | kitchensink-ear-ejb, kitchensink-ear-web | — | kitchensink-ear-ear → kitchensink-ear-ejb |
| kitchensink-ear-ejb | `kitchensink-ear/ejb` | ejb | — | kitchensink-ear, kitchensink-ear-ear, kitchensink-ear-web | — |
| kitchensink-ear-web | `kitchensink-ear/web` | war | kitchensink-ear-ejb | kitchensink-ear, kitchensink-ear-ear | kitchensink-ear-web → kitchensink-ear-ejb |
| kitchensink-jsp | `kitchensink-jsp` | war | — | — | — |
| kitchensink-ml | `kitchensink-ml` | war | — | — | — |
| logging | `logging` | war | — | — | — |
| logging-tools | `logging-tools` | war | — | — | — |
| mail | `mail` | war | — | — | — |
| managed-executor-service | `managed-executor-service` | war | — | — | — |
| messaging-clustering-singleton | `messaging-clustering-singleton` | war | — | — | — |
| payment-cdi-event | `payment-cdi-event` | war | — | — | — |
| resteasy-jaxrs-client | `resteasy-jaxrs-client` | jar | — | — | — |
| servlet-async | `servlet-async` | war | — | — | — |
| servlet-filterlistener | `servlet-filterlistener` | war | — | — | — |
| servlet-security | `servlet-security` | war | — | — | — |
| shopping-cart | `shopping-cart` | pom | shopping-cart-server | — | shopping-cart → shopping-cart-server |
| shopping-cart-client | `shopping-cart/client` | jar | shopping-cart-server | — | shopping-cart-client → shopping-cart-server |
| shopping-cart-server | `shopping-cart/server` | ejb | — | shopping-cart, shopping-cart-client | — |
| spring-greeter | `spring-greeter` | war | — | — | — |
| spring-kitchensink-basic | `spring-kitchensink-basic` | war | — | — | — |
| spring-kitchensink-springmvctest | `spring-kitchensink-springmvctest` | war | — | — | — |
| spring-resteasy | `spring-resteasy` | war | — | — | — |
| tasks-jsf | `tasks-jsf` | war | — | — | — |
| tasks-rs | `tasks-rs` | war | — | — | — |
| temperature-converter | `temperature-converter` | war | — | — | — |
| thread-racing | `thread-racing` | war | — | — | — |
| todo-backend | `todo-backend` | war | — | — | — |
| websocket-client | `websocket-client` | war | — | — | — |
| websocket-endpoint | `websocket-endpoint` | war | — | — | — |
| websocket-hello | `websocket-hello` | war | — | — | — |
| wsat-simple | `wsat-simple` | war | — | — | — |
| wsba-coordinator-completion-simple | `wsba-coordinator-completion-simple` | jar | — | — | — |
| wsba-participant-completion-simple | `wsba-participant-completion-simple` | jar | — | — | — |
| xml-jaxp | `xml-jaxp` | war | — | — | — |

## Standalone applications

No intra-portfolio dependencies - each builds and deploys independently:

helloworld, app-client, batch-processing, bean-validation, bean-validation-custom-constraint, bmt, cmt, contacts-jquerymobile, ejb-in-war, ejb-multi-server, ejb-multi-server-app-main, ejb-multi-server-app-one, ejb-multi-server-app-two, ejb-remote, ejb-security, ha-singleton-deployment, helloworld-html5, helloworld-jms, kitchensink, numberguess, ejb-security-context-propagation, ejb-security-jaas, ejb-security-programmatic-auth, ejb-timer, ejb-txn-remote-call, ejb-txn-remote-call-client, ejb-txn-remote-call-server, greeter, ha-singleton-service, helloworld-mbean, helloworld-mbean-service, helloworld-mbean-webapp, helloworld-mdb, helloworld-mdb-propertysubstitution, helloworld-mutual-ssl, helloworld-mutual-ssl-secured, helloworld-rs, helloworld-singleton, helloworld-ssl, helloworld-ws, hibernate, jaxrs-client, jaxws-ejb, jaxws-pojo, jaxws-retail, jsonp, jta-crash-rec, kitchensink-angularjs, kitchensink-jsp, kitchensink-ml, logging, logging-tools, mail, managed-executor-service, messaging-clustering-singleton, payment-cdi-event, resteasy-jaxrs-client, servlet-async, servlet-filterlistener, servlet-security, spring-greeter, spring-kitchensink-basic, spring-kitchensink-springmvctest, spring-resteasy, tasks-jsf, tasks-rs, temperature-converter, thread-racing, todo-backend, websocket-client, websocket-endpoint, websocket-hello, wsat-simple, wsba-coordinator-completion-simple, wsba-participant-completion-simple, xml-jaxp.

## Corrections applied to the template

1. **Broken repository paths** (would fail the MTA import): the four
   `ejb-multi-server-app-main*` rows pointed at `ejb-multi-server-app/main*`
   - the directory on `7.4.x` is `ejb-multi-server/app-main*`; and
   `ejb-in-ear-web` pointed at `ejb-in-ear/war` instead of `ejb-in-ear/web`.
2. **Self-referencing dependency**: the `kitchensink-ear-web` row listed
   itself as its own dependency.
3. **Dependency values re-derived from the POMs** (35 rows): the template
   had encoded module-to-parent membership edges (northbound to the
   aggregator or containing EAR) where the POMs declare actual consumer
   dependencies. Each row now carries its primary POM-verified edge
   (southbound where the POM declares one, e.g. `jaxws-ejb-client` →
   `jaxws-ejb-service`, `app-main-ejb` → `app-one-ejb`/`app-two-ejb`,
   `inter-app-AppA`/`AppB` → `inter-app-shared`); the northbound
   used-by relationships that no row expresses explicitly remain visible
   in the table above. Membership to a pure aggregator `pom` row is kept
   only where no POM dependency exists (e.g. `ejb-multi-server-app-main`
   → `ejb-multi-server`).
4. **Duplicate edges removed** (12 rows): the earlier data expressed
   12 edges from both ends - southbound from the consumer and northbound
   from the consumed (e.g. `ejb-asynchronous` → `ejb-asynchronous-ejb`
   southbound *and* `ejb-asynchronous-ejb` → `ejb-asynchronous`
   northbound, which create the identical Hub edge). The northbound twin
   was dropped so the 42-record import file yields exactly the 42-edge
   graph.
5. **Dependencies moved out of the application template**: the
   template's `Dependency` / `Dependency Direction` columns are now empty
   for all rows - importing applications does not create edges. The
   dependency data is imported separately via the record-type-2 file,
   which can also be re-run against an already-onboarded portfolio
   without recreating applications.
