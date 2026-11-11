# Verify Errors

Errors are grouped by the stage in which they surface.

## Plan Stage

| Error | Cause | Fix |
|---|---|---|
| Inventory missing a call site discovered later | Discovery ran only over `src/` of one module, missing other deployments or the server config | Re-run the full §9.2 command set per application, including `standalone.xml` (`remote-outbound-connections`, `iiop-configuration`) and WSDL/stub artifacts, before ordering the work |

## Author Stage

| Error | Cause | Fix |
|---|---|---|
| `EJBCLIENT000409: No more destinations are available` | The `ejb:<app>/<module>/<bean>!<view>` URL does not match the *deployed* artifact names (renamed during migration), or the consumer is in the same server — `ejb:` URLs never route in-VM even with matching names (verified on EAP 8.0.11/8.1) | Check every `ejb:` lookup against actual deployment names; for a consumer deployed to the same server, look up `java:global/<deployment-name>/<bean>!<view>` (the server prints these bindings at deploy time) |
| `WELD-001408: Unsatisfied dependencies for type <ClientFQN> with qualifiers @Default` | The MicroProfile REST Client CDI extension is part of the EAP XP, not base EAP (verified on EAP 8.0.11 and 8.1) | Install the EAP XP, or build the client programmatically: `RestClientBuilder.newBuilder().baseUri(...).register(mapper).build(Client.class)` (the API jar compiles on base EAP) |
| `WFLYEJB0406: No Jakarta Enterprise Beans found with interface of type ...` | A `@Stateless` bean that implements an interface (e.g. the `@Remote` one) exposes no no-interface view without `@LocalBean`; injecting the bean class fails at runtime | Add a `@Local` interface (preferred — pass-by-reference, joins the caller's transaction) or annotate the bean `@LocalBean`; do not inject over the remote view from the REST resource (pass-by-value, own transaction) |
| `@Provider` mapper/filter never fires | `@Provider` is not a bean-defining annotation; under `bean-discovery-mode="annotated"` the class is not discovered | Set `bean-discovery-mode="all"` in `beans.xml`, give the class a bean-defining annotation, or register it explicitly on the JAX-RS `Application` subclass |
| Generated OpenAPI client uses `javax.*` and does not compile | `useJakartaEe=true` missing from the generator config | Set `useJakartaEe=true` in `openapi-generator-cli` (`-DuseJakartaEe=true`) or the Maven plugin `configOptions` |
| `package javax.xml.ws does not exist` / SOAP stubs fail to compile | `javax`-namespace wsimport/wsconsume stubs on the jakarta target stack | Regenerate stubs with EAP 8 `wsconsume` (generates `jakarta.xml.ws.*`); or migrate those call sites to the REST client |
| `jakarta.ws.rs.RequestScoped` cannot be found at compile time | `@RequestScoped` lives in `jakarta.enterprise.context` (CDI), not in the JAX-RS API — there is no `jakarta.ws.rs.RequestScoped` | Import `jakarta.enterprise.context.RequestScoped` and ensure the CDI API dependency (provided scope) is present |
| OpenAPI document does not match the live endpoints | Resource paths relative to `@ApplicationPath("/api")` were written as absolute, or the static document drifted from the code | Make effective URLs match the published document exactly; annotate the facade (`@Operation`, `@APIResponse`) so generated metadata merges with the static file |

## Verify Stage

| Error | Cause | Fix |
|---|---|---|
| `The required mechanism 'MP-JWT' is not available` (deploy fails) | `MP-JWT` auth-method requires the `microprofile-jwt-smallrye` subsystem, which exists only on an EAP XP installation (verified: fails on base EAP 8 GA/8.1) | Install the EAP XP, or use an Elytron `token-realm` (JWT) — part of base EAP, no XP needed |
| `GET /openapi` returns 404 | MicroProfile OpenAPI is XP-only; base EAP has no `microprofile-openapi-smallrye` subsystem | Expected on base EAP — serve the document via the manual fallback resource (`@Path("/api/v1/openapi")` reading `META-INF/openapi.yaml`) or install the XP |
| `WFLYRS0015: No Servlet declaration found for Jakarta RESTful Web Services application` — every REST path returns 404 while the app deploys cleanly (verified on EAP 8.1) | EAP does not auto-activate JAX-RS for annotation-only deployments: a deployed WAR with `@Path` resources but no `Application` subclass and no web.xml servlet declaration gets no REST servlet | Add a class `public class RestApplication extends Application` annotated `@ApplicationPath("/api")`; resource `@Path` values are then relative to it (e.g. `@Path("/v1/echo")` serves `/api/v1/echo`); the SOAP/CXF servlet mapping is unaffected |
| Previously-working SOAP/other endpoint returns 404 after redeploy while the code is unchanged | A stale server instance from an earlier run is still holding the ports — the new deployment never came up (bind errors `Address already in use /127.0.0.1:8080` in the boot log) | Kill the stale server process (`pkill -f jboss-modules.jar`), confirm ports are free (`ss -ltn`), and re-run the endpoint matrix against the new boot log |
| REST path 404 although the boot log shows `RESTEASY002225: Deploying jakarta.ws.rs.core.Application` | The effective base URL includes the WAR context root: a deployment without a `jboss-web.xml` override serves under `/<war-name>` (verified on EAP 8.1: `ejb-remote.war` → `/ejb-remote/api/...`) | Compose the consumer URL as `<scheme>://<host>:<port>/<context-root>/<application-path>/...`; check `jboss-web.xml` / `finalName` for the context root, and add a `servers:` entry to the static OpenAPI document so the published contract matches the effective URLs |
| `EJBCLIENT000409` with `SaslException: all available authentication mechanisms failed` (JBOSS-LOCAL-USER / DIGEST-MD5 rejected) on a remote-interface call that used to work | The `@Remote` interface is secured — the client needs an application user, but the requirement lives only in the quickstart/README (easy to miss in CI or a fresh provisioned server; discovered during the ejb-remote cutover test) | Create the application user on the provisioned server: `add-user.sh -a -u <user> -p <password> -g <group>` per the README's user table, then restart the server; inventory step 3 must record `@RolesAllowed`/secured-interface findings per call site |
| `NoClassDefFoundError: com/acme/customer/api/...` at deploy | The shared API jar is `provided` in the WAR but the server does not expose it as a module | `module add --name=<module> --resources=<api jar>`, then a `jboss-deployment-structure.xml` deployment overlay adding the module dependency; redeploy |
| `EJBAccessException: WFLYEJB0364` surfaces as 500 instead of 401 | `@RolesAllowed` on the bean denies the call, but the web layer has no `security-constraint` triggering the auth challenge | Add a `security-constraint`/`login-config` in `web.xml` so the challenge happens at the web layer (401) before the EJB check |
| Contract-test payload mismatch on dates/enums/BigDecimal | JSON is less strict than Java serialization: `null` vs absent, date formats, float drift | Pin Jackson config on both sides: ISO-8601 date strings, enum names, `BigDecimal` as string; agree `null`-vs-absent handling (§5.1) |
| `Content-Length header already present` / SOAP test client sends no auth | spring-ws 4.x `HttpComponents5MessageSender` no-arg + `setCredentials` leaves the internal `HttpClient` null (verified) | Use `HttpUrlConnectionMessageSender` with a preemptive `Authorization` header in the integration test client |
| Remote-EJB integration test fails to find remoting classes | `jboss-ejb-client` alone does not pull the WildFly naming client, elytron, xnio, remoting, discovery, marshalling, transaction client | Add the full EJB client stack as BOM-managed test-scope dependencies (see the worked example's `order-consumer` POM) |

## General

| Error | Cause | Fix |
|---|---|---|
| Galleon provisioning fails: `Could not find artifact ...wildfly-ee-feature-pack-product-conf` | The 8.0/8.1 GA galleon packs reference a version that only the channel manifest resolves | Provision with `GALLEON_PROVISION_CHANNELS=org.jboss.eap.channels:eap-8.1:...` — never without a channel |
| `Unknown schema version` during provisioning | The 8.1 channel manifests use schema 1.1.0; the builder's pinned `eap-maven-plugin:1.0.1` cannot parse it | Override `PROVISIONING_MAVEN_PLUGIN_VERSION=2.0.1.Final-redhat-00021` |
| Server unreachable after provisioning | Provisioned config binds the HTTP listener to a stale IP instead of `0.0.0.0` | `/interface=public:write-attribute(name=inet-address,value=0.0.0.0)` then `:reload` via `jboss-cli.sh --connect` |
