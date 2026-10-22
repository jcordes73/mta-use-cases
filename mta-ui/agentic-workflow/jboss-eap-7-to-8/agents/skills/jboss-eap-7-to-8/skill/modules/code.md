# Phase: Code Migration

Migrate all `javax.*` EE imports to `jakarta.*` and replace removed APIs.

## Steps

### Part 1: API replacements

1. Read `references/api-map.md`
2. Process rows in `kind` order:

#### Package-level namespace migration (highest priority)
Apply these in bulk — every `javax.*` EE import must become `jakarta.*`:
- `javax.servlet.*` → `jakarta.servlet.*`
- `javax.persistence.*` → `jakarta.persistence.*`
- `javax.ws.rs.*` → `jakarta.ws.rs.*`
- `javax.enterprise.*` → `jakarta.enterprise.*`
- `javax.inject.*` → `jakarta.inject.*`
- `javax.ejb.*` → `jakarta.ejb.*`
- `javax.faces.*` → `jakarta.faces.*`
- `javax.el.*` → `jakarta.el.*`
- `javax.jms.*` → `jakarta.jms.*`
- `javax.validation.*` → `jakarta.validation.*`
- `javax.annotation.*` → `jakarta.annotation.*` (NOT `javax.annotation.processing` — that's Java SE)
- `javax.xml.bind.*` → `jakarta.xml.bind.*`
- `javax.xml.soap.*` → `jakarta.xml.soap.*`
- `javax.json.*` → `jakarta.json.*`
- `javax.json.bind.*` → `jakarta.json.bind.*`
- `javax.websocket.*` → `jakarta.websocket.*`
- `javax.mail.*` → `jakarta.mail.*`
- `javax.transaction.*` → `jakarta.transaction.*` (NOT `javax.transaction.xa` — Java SE)
- `javax.security.enterprise.*` → `jakarta.security.enterprise.*`

**IMPORTANT:** Do NOT change Java SE `javax.*` packages: `javax.xml.datatype`, `javax.xml.parsers`, `javax.xml.transform`, `javax.crypto`, `javax.net`, `javax.sql` (the JDBC base), `javax.naming`, `javax.security.auth`, `javax.annotation.processing`, `javax.transaction.xa`.

#### Class/method-level removals and replacements

**CDI 4.0 API removals:**
- `Bean.isNullable()` — remove call
- `BeanManager.createInjectionTarget(AnnotatedType)` → `getInjectionTargetFactory(at).createInjectionTarget(null)`
- `BeanManager.fireEvent(Object, Annotation...)` → `getEvent().select(qualifiers).fire(event)`
- `BeforeBeanDiscovery.addAnnotatedType(AnnotatedType)` → `addAnnotatedType(at, "uniqueId")`

**EJB API removals:**
- `EJBContext.getCallerIdentity()` → `getCallerPrincipal()`
- `EJBContext.isCallerInRole(Identity)` → `isCallerInRole(String)`
- `SessionContext.getMessageContext()` — remove call
- `EJBContext.getEnvironment()` — remove; use JNDI lookup

**Servlet 6.0 removals:**
- `SingleThreadModel` interface — remove; use proper synchronization
- `HttpSessionContext` — remove usage
- `HttpUtils` — remove; parse query strings manually
- `encodeUrl()`/`encodeRedirectUrl()` → `encodeURL()`/`encodeRedirectURL()` (case fix)
- `setStatus(int, String)` → `sendError(int, String)`
- `HttpSession.getValue()`/`putValue()`/`removeValue()`/`getValueNames()` → `getAttribute()`/`setAttribute()`/`removeAttribute()`/`getAttributeNames()`
- `ServletContext.getServlet()`/`getServlets()`/`getServletNames()` — remove
- `ServletRequest.getRealPath()` → `ServletContext.getRealPath()`

**Faces 4.0 removals:**
- `@ManagedBean` → CDI `@Named` with CDI scope annotations
- `@ManagedProperty("#{expr}")` → `@Inject @ManagedProperty("#{expr}")`
- `ResourceResolver`/`FaceletsResourceResolver` — remove; use `ResourceHandler`
- JSP view technology for Faces — migrate to Facelets (`.xhtml`)

**SOAP/JAXB removals:**
- `javax.xml.soap.SOAPElementFactory` → use `jakarta.xml.soap.SOAPFactory`
- `javax.xml.bind.Validator` → remove; use schema validation directly
- JAXB implementation: `com.sun.xml.bind.*` → `org.glassfish.jaxb.runtime.*`
- JAXB Marshaller properties: `"com.sun.xml.bind.*"` → `"org.glassfish.jaxb.*"`

**EL 5.0:**
- `isParmetersProvided()` → `isParametersProvided()` (typo fix)

#### Package-level removals and relocations
- `org.picketbox.*` — remove all usage; migrate to Elytron
- `org.picketlink.*` — remove all usage; use Keycloak
- `org.jboss.security.*` (legacy) → `org.wildfly.security.*` (Elytron)
- `org.jboss.logging.Cause`/`Field`/`FormatWith`/`LoggingClass` → `org.jboss.logging.annotations.*`
- `org.apache.ws.security.WSPasswordCallback` → `org.apache.wss4j.common.ext.WSPasswordCallback`
- `org.apache.ws.security.saml.ext.*` → `org.apache.wss4j.common.saml.*`
- `org.codehaus.jackson.annotate.*` → `com.fasterxml.jackson.annotation.*` (Jackson 1→2)

#### RESTEasy API migration
- `PreProcessInterceptor` → `ContainerRequestFilter`
- `MessageBodyWriterInterceptor` → `WriterInterceptor`
- `ClientRequest`/`ClientResponse` → `ResteasyClient`/`Response`
- `StringConverter` → `ParamConverterProvider`
- SPI exceptions: `o.j.r.spi.ForbiddenException` → `jakarta.ws.rs.ForbiddenException` (and NotFoundException, UnauthorizedException)
- Add `@Produces`/`@Consumes` to all REST endpoints — missing annotations now cause `NoMessageBodyWriterFoundFailure`

#### EJB client / JNDI naming
- `org.jboss.naming.remote.client.InitialContextFactory` → `org.wildfly.naming.client.WildFlyInitialContextFactory`
- Remote JNDI URL: `remote://localhost:4447` → `http-remoting://localhost:8080`

### Part 2: Pattern changes

1. Read `references/pattern-map.md`
2. Key areas:

- **CDI bean discovery**: Add `@ApplicationScoped`, `@RequestScoped`, or other CDI annotations to beans that rely on discovery via empty `beans.xml`
- **Hibernate ORM 6**: Update HQL queries, Criteria API usage, and entity mappings per Hibernate 6 migration guide
- **Hibernate Search 6**: Rewrite search integration — APIs are completely different from 5.x
- **ServiceLoader files**: Rename `META-INF/services/javax.*` → `META-INF/services/jakarta.*`
- **OIDC**: `<auth-method>KEYCLOAK</auth-method>` → `OIDC`; rename `WEB-INF/keycloak.json` → `oidc.json`
- **Vault**: Replace `${VAULT::...}` expressions with Elytron encrypted expressions
- **HornetQ**: Update messaging descriptor namespace and element names
- **Custom valves**: Replace Catalina `ValveBase` subclasses with Undertow handlers

3. Run the build gate

## Build gate

Run `mvn compile`. Check for:
- Remaining `javax.*` EE imports (except Java SE packages)
- PicketBox/PicketLink API references
- `com.sun.xml.bind` JAXB implementation references
- RESTEasy deprecated interceptor/client API references
- `org.codehaus.jackson` Jackson 1.x references
- `org.hornetq` HornetQ API references
- Hibernate 6 API incompatibilities
