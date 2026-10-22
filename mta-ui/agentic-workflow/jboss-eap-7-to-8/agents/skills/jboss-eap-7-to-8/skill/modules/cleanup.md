# Phase: Cleanup and Verification

Remove legacy references and verify the complete migration.

## Steps

### 1. Remove old `javax.*` EE imports

Search for any remaining `javax.*` EE imports that should have been migrated:
- `javax.servlet.*`
- `javax.persistence.*`
- `javax.ws.rs.*`
- `javax.enterprise.*`
- `javax.inject.*`
- `javax.ejb.*`
- `javax.faces.*`
- `javax.jms.*`
- `javax.validation.*`
- `javax.xml.bind.*`
- `javax.xml.soap.*`
- `javax.json.*`
- `javax.websocket.*`
- `javax.mail.*`
- `javax.transaction.Transactional` (but NOT `javax.transaction.xa.*`)
- `javax.annotation.PostConstruct` etc. (but NOT `javax.annotation.processing.*`)

**Do NOT change** Java SE packages: `javax.sql`, `javax.naming`, `javax.crypto`, `javax.net`, `javax.xml.parsers`, `javax.xml.transform`, `javax.xml.datatype`, `javax.security.auth`, `javax.annotation.processing`, `javax.transaction.xa`.

### 2. Verify no legacy security references

Search for:
- `org.picketbox.*` imports
- `org.picketlink.*` imports
- `org.jboss.security.*` imports (legacy security framework)
- `<security-domain>` with PicketBox login modules in `jboss-web.xml`
- PicketBox vault references in server config

### 3. Verify deployment descriptors

Check all XML descriptors use Jakarta EE namespaces:
- `web.xml` — `https://jakarta.ee/xml/ns/jakartaee`
- `ejb-jar.xml` — `https://jakarta.ee/xml/ns/jakartaee`
- `persistence.xml` — `https://jakarta.ee/xml/ns/persistence` version `3.1`
- `beans.xml` — `https://jakarta.ee/xml/ns/jakartaee`
- JAXB bindings — `https://jakarta.ee/xml/ns/jaxb`

### 4. Verify no old artifacts remain

Search build files for:
- `jboss-eap-jakartaee8` BOM reference
- `jboss-javaee-6.0-with-*` BOM references
- `javax.servlet:javax.servlet-api` and other old `javax.*` dependency coordinates
- JBoss spec artifacts (`org.jboss.spec.javax.*`) that should be replaced
- `log4j:log4j` (v1)
- `org.picketbox:picketbox`
- `org.picketlink:picketlink-api`
- `org.jboss.resteasy:resteasy-jackson-provider` (Jackson 1)
- `org.jboss.resteasy:resteasy-jettison-provider`
- `org.hornetq:*` (HornetQ)
- `META-INF/services/javax.*` files (should be renamed to `jakarta.*`)

### 5. Verify behavioral changes

These won't cause compilation errors but alter runtime behavior:
- [ ] CDI bean discovery: all needed beans are discovered (empty `beans.xml` now defaults to `annotated`)
- [ ] Security: Elytron authentication/authorization working correctly
- [ ] OIDC: `elytron-oidc-client` subsystem correctly configured (if using SSO)
- [ ] `@Stateful` EJBs behave correctly under load
- [ ] Module dependencies resolved (no `ClassNotFoundException` from lazy loading)
- [ ] Monitoring: metrics available at `/metrics` endpoint
- [ ] Hibernate ORM 6 query results match expectations
- [ ] Log4j 2 logging configured correctly (if previously using Log4j v1)
- [ ] RESTEasy: `@RolesAllowed`/`@DenyAll` now return 403 (not 401); clients handle correctly
- [ ] RESTEasy: all REST endpoints have `@Produces`/`@Consumes` annotations
- [ ] JSON Binding: `@JsonbCreator` constructors with partial parameters work as expected
- [ ] EJB remote clients connect on port 8080 (not 4447)
- [ ] JNDI lookups use `http-remoting://` URL scheme
- [ ] HTTP Session ID format change doesn't break JSESSIONID cookie logic
- [ ] Messaging: ActiveMQ Artemis queues/topics accessible
- [ ] Vault expressions replaced with Elytron encrypted expressions

### 6. Final build and test

- Run the full build: `mvn clean verify`
- Deploy to EAP 8 and verify application starts
- Run integration tests against EAP 8
- Test under load for `@Stateful` EJB behavioral changes

### 7. Report to user

- List all changes made across all phases
- Flag `jboss-web-policy`/`jboss-ejb-policy` if manual authorization migration is needed
- Note Hibernate ORM/Search 6 changes requiring manual verification
- Report any third-party libraries that still use `javax.*` and need Eclipse Transformer
