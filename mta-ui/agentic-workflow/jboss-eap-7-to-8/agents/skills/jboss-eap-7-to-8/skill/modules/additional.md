# Phase: Additional Changes

Handle server provisioning, monitoring, cloud deployment, and infrastructure changes.

## Steps

1. Read `references/pattern-map.md` — filter for `addition` category and infrastructure-related rows

### JDK upgrade
- JDK 8 no longer supported
- JDK 11 minimum (deprecated but supported)
- JDK 17 recommended; Java SE 21 planned for future EAP 8 release
- Update CI/CD pipelines, Dockerfiles, and build scripts

### Monitoring migration
- Jolokia (JMX-over-HTTP) removed — no longer supported
- Prometheus subsystem removed — no longer supported
- Use built-in metrics endpoint: `<server-address>:<management-port>/metrics`
- Update monitoring dashboards and alerting to use the new endpoint

### Server provisioning (Galleon)
- New `eap-maven-plugin` for Galleon-based server provisioning
- Produces trimmed servers with only required layers — reduces image size and attack surface
- Supports execution of JBoss CLI scripts for server customization
- New Galleon layer `ee-core-profile-server` for Jakarta EE 10 Core Profile

### OpenShift/Cloud deployment
- OpenShift S2I images no longer contain pre-installed EAP binaries
- EAP is provisioned at build time via `eap-maven-plugin`
- Update `Dockerfile` and OpenShift build configs
- `eap-datasources-galleon-pack` available for Oracle, SQL Server, PostgreSQL drivers

### Hibernate ORM 5 → 6
- Major breaking changes in HQL, Criteria API, and type system
- See [Hibernate ORM 6 Migration Guide](https://docs.jboss.org/hibernate/orm/6.0/migration-guide/migration-guide.html)
- Key changes: implicit joins, HQL syntax, `@Type` annotations, basic type mappings

### Hibernate Search 5 → 6
- Completely rewritten API — not backwards-compatible
- See [Hibernate Search 6 Migration Guide](https://docs.jboss.org/hibernate/search/6.0/migration/html_single/)
- `FullTextEntityManager` → `Search.session(entityManager)`
- Programmatic mapping API completely different

### Log4j v1 removal
- Applications relying on EAP-provided Log4j v1 must update
- Either package Log4j 2 within the deployment or use JBoss Logging
- Update `log4j.properties`/`log4j.xml` to Log4j 2 format (`log4j2.xml`)

### OIDC migration
- Keycloak OIDC client adapter not supported in EAP 8
- Replace `<auth-method>KEYCLOAK</auth-method>` with `<auth-method>OIDC</auth-method>` in `web.xml`
- Rename `WEB-INF/keycloak.json` → `WEB-INF/oidc.json`
- `keycloak` subsystem replaced by `elytron-oidc-client` subsystem (auto-migrated by Server Migration Tool)

### PicketLink migration
- PicketLink SP → Keycloak SAML adapter (install via `jboss-eap-installation-manager`)
- PicketLink IDP → Red Hat build of Keycloak
- PicketLink STS → Apache CXF STS

### Vault migration
- `${VAULT::vault_block::attribute::sharedKey}` expressions → Elytron encrypted expressions
- Search all deployment files and annotations for `${VAULT::` references

### Messaging data migration
- Export data from EAP 7: `/subsystem=messaging-activemq/server=default:export-journal()`
- Import into EAP 8: `/subsystem=messaging-activemq/server=default:import-journal(file=...)`
- Alternative: use a JMS bridge for live migration
- `-jms.xml` deployment descriptors: update namespace to `urn:jboss:messaging-activemq-deployment:1.0`, rename `<hornetq-server>` → `<server>`

### Web services changes
- JAX-RPC not supported — migrate to Jakarta XML Web Services (`@WebService`)
- `jbossws-cxf.xml` Spring integration removed — use `jaxws-endpoint-config.xml` descriptors
- CXF bus strategy: default changed from `THREAD_BUS` to `TCCL_BUS` for in-container clients

### RESTEasy behavioral changes
- Security filters (`@RolesAllowed`, `@DenyAll`) now return 403 Forbidden instead of 401
- `resteasy.add.charset` defaults to `true` — adds `charset=UTF-8` to `text/*` responses
- `@Produces`/`@Consumes` required on endpoints to avoid `NoMessageBodyWriterFoundFailure`

### Eclipse Transformer (for third-party JARs)
- For dependencies without Jakarta EE 10-compatible versions, use Eclipse Transformer
- Performs bytecode-level `javax.*` → `jakarta.*` transformation on existing JARs
- Alternative when source code migration is not possible

### Module lazy loading
- EAP 8 loads modules lazily for smaller footprint
- Applications with implicit module dependencies may encounter `ClassNotFoundException`
- Declare explicit module dependencies in `jboss-deployment-structure.xml` if needed

2. Run the build gate

## Build gate

Run `mvn compile` and deploy to EAP 8. Verify:
- Application starts successfully on EAP 8 with JDK 17
- Monitoring endpoints respond at `<mgmt-port>/metrics`
- All datasources connect (`/subsystem=datasources/data-source=<name>:test-connection-in-pool`)
