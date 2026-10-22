# Phase: Configuration Migration

Update server configuration, deployment descriptors, and application config files.

## Steps

1. Read `references/config-map.md`

### Server configuration (`standalone.xml` / `domain.xml`)

**Recommended: use the JBoss Server Migration Tool** to auto-migrate most server config:
```bash
./jboss-server-migration.sh -s $EAP_PREVIOUS_HOME -t $JBOSS_HOME
```

The tool automatically:
- Removes `org.jboss.as.security` extension and `urn:jboss:domain:security:2.0` subsystem
- Migrates `ManagementRealm` and `ApplicationRealm` to Elytron
- Migrates legacy security domain `other` to Elytron
- Preserves JDBC drivers and datasource configurations

**Manual steps after tool migration:**
- Verify `jboss-web-policy` and `jboss-ejb-policy` authorization — migration NOT supported by tool
- Verify JASPI (`authentication-jaspi`) — migration NOT supported by tool
- Migrate PicketBox vault to Elytron credential store; replace `${VAULT::...}` expressions
- Replace `keycloak` subsystem with `elytron-oidc-client` for OIDC

### Undertow subsystem
- HTTPS listener: `security-realm="ApplicationRealm"` → `ssl-context="applicationSSC"` (Elytron TLS)
- HTTP invoker: `security-realm="ApplicationRealm"` → `http-authentication-factory="application-http-authentication"`
- `Server`/`X-Powered-By` response headers removed from defaults (since EAP 7.2)
- Custom Catalina valves in `jboss-web.xml` → implement Undertow handlers

### mod_cluster subsystem
- Config path: `/subsystem=modcluster/mod-cluster-config=configuration` → `/proxy=default`
- `proxy-list` attribute → `proxies` (list of outbound socket binding names)
- `connector` attribute → `listener`
- Replace `ssl=configuration` with Elytron-based SSL config

### EJB subsystem
- `cache`/`passivation-store` → `simple-cache`/`distributable-cache` + `distributable-ejb` subsystem
- `passivation-store` `idle-timeout` removed — only lazy passivation via `max-size`

### Messaging subsystem
- `replication-primary` `check-for-live-server` default changed from `false` to `true` (since EAP 7.1)
- Embedded broker no longer included by default — add `embedded-activemq` Galleon layer if needed

### EJB client configuration
- Default remote port: `4447` → `8080` in `jboss-ejb-client.properties`
- JNDI provider URL: `remote://localhost:4447` → `http-remoting://localhost:8080`
- Consider migrating from `jboss-ejb-client.properties` to `META-INF/wildfly-config.xml`

### CDI `beans.xml`
- If `beans.xml` is empty (no `bean-discovery-mode` attribute), CDI 4.0 defaults to `annotated`
- To preserve EAP 7 behavior, add `bean-discovery-mode="all"`:
  ```xml
  <beans xmlns="https://jakarta.ee/xml/ns/jakartaee" version="4.0"
         bean-discovery-mode="all">
  </beans>
  ```

### XML deployment descriptors
Update namespaces in all EE deployment descriptors:
- `web.xml`: `xmlns="http://xmlns.jcp.org/xml/ns/javaee"` → `xmlns="https://jakarta.ee/xml/ns/jakartaee"`
- `ejb-jar.xml`: same namespace update
- `persistence.xml`: namespace → `https://jakarta.ee/xml/ns/persistence`, version → `3.1`
- JAXB binding files: `http://java.sun.com/xml/ns/jaxb` → `https://jakarta.ee/xml/ns/jaxb`

### ServiceLoader files
- Rename all `META-INF/services/javax.*` files to `META-INF/services/jakarta.*`

### Maven properties
- Update `version.server.bom` to `8.0.0.GA-redhat-00009` or later

3. Run the build gate

## Build gate

Run `mvn compile`. Configuration errors surface as deployment failures — also start the application in EAP 8 to verify:
- Security realm configuration (Elytron)
- CDI bean discovery
- Datasource connectivity (`/subsystem=datasources/data-source=<name>:test-connection-in-pool`)
- EJB remote connections on port 8080
- Messaging subsystem connectivity
- mod_cluster load balancer registration
