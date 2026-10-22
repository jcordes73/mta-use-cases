# Phase: Build System Migration

Update Maven BOMs and dependencies before touching application code.

## Steps

1. Read `references/dependency-map.md`
2. Open the project's `pom.xml` (and any parent/module POMs)

### Maven BOM updates
- Replace `org.jboss.bom:jboss-eap-jakartaee8` with `org.jboss.bom:jboss-eap-ee`
- Replace `org.jboss.bom:jboss-eap-jakartaee8-with-tools` with `org.jboss.bom:jboss-eap-ee-with-tools`
- If using EAP 6 BOMs (`org.jboss.bom.eap:jboss-javaee-6.0-with-tools`, `jboss-javaee-6.0-with-hibernate`), replace with `org.jboss.bom:jboss-eap-ee-with-tools`
- Remove `org.jboss.spec:jboss-jakartaee-8.0` (Jakarta EE 8 Spec APIs BOM — removed)
- Remove `org.jboss.bom:jboss-eap-runtime-artifacts` (Runtime Artifacts BOM — removed)
- Remove `org.jboss:jboss-ejb-client` legacy BOM
- Update `version.server.bom` property to `8.0.0.GA-redhat-00009` or later

### Jakarta EE dependency coordinate updates
- Replace all `javax.*` EE API artifacts with `jakarta.*` equivalents:
  - `javax.servlet:javax.servlet-api` → `jakarta.servlet:jakarta.servlet-api`
  - `javax.persistence:javax.persistence-api` → `jakarta.persistence:jakarta.persistence-api`
  - `javax.ws.rs:javax.ws.rs-api` → `jakarta.ws.rs:jakarta.ws.rs-api`
  - `javax.enterprise:cdi-api` → `jakarta.enterprise:jakarta.enterprise.cdi-api`
  - `javax.ejb:javax.ejb-api` → `jakarta.ejb:jakarta.ejb-api`
  - `javax.faces:javax.faces-api` → `jakarta.faces:jakarta.faces-api`
  - `javax.jms:javax.jms-api` → `jakarta.jms:jakarta.jms-api`
  - `javax.validation:validation-api` → `jakarta.validation:jakarta.validation-api`
  - `javax.annotation:javax.annotation-api` → `jakarta.annotation:jakarta.annotation-api`
  - `javax.xml.bind:jaxb-api` → `jakarta.xml.bind:jakarta.xml.bind-api`
  - `javax.json:javax.json-api` → `jakarta.json:jakarta.json-api`
  - `javax.json.bind:javax.json.bind-api` → `jakarta.json.bind:jakarta.json.bind-api`

### JBoss spec artifact coordinate updates
Replace all JBoss-published spec artifacts with their Jakarta equivalents (25+ artifacts from dependency-map `JBoss Spec Artifacts` section):
  - `org.jboss.spec.javax.annotation:jboss-annotations-api_1.3_spec` → `jakarta.annotation:jakarta.annotation-api`
  - `org.jboss.spec.javax.ejb:jboss-ejb-api_3.2_spec` → `jakarta.ejb:jakarta.ejb-api`
  - `org.jboss.spec.javax.servlet:jboss-servlet-api_4.0_spec` → `jakarta.servlet:jakarta.servlet-api`
  - `org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec` → `jakarta.ws.rs:jakarta.ws.rs-api`
  - `org.jboss.spec.javax.jms:jboss-jms-api_2.0_spec` → `jakarta.jms:jakarta.jms-api`
  - `com.sun.activation:jakarta.activation` → `jakarta.activation:jakarta.activation-api`
  - `com.sun.mail:jakarta.mail` → `jakarta.mail:jakarta.mail-api`
  - And all other JBoss spec artifacts — apply every row with `source_section = JBoss Spec Artifacts`

### Removed dependencies
- Remove `org.picketbox:picketbox` — PicketBox removed
- Remove `org.picketlink:picketlink-api` — PicketLink removed
- Remove `log4j:log4j` — Log4j v1 APIs removed; use JBoss Logging or Log4j 2
- Remove `org.jboss.resteasy:resteasy-jackson-provider` — replaced by `resteasy-jackson2-provider`
- Remove `org.jboss.resteasy:resteasy-jettison-provider` — removed
- Remove `org.jboss.resteasy:resteasy-yaml-provider` — removed (SnakeYAML security issues)

### ORM upgrades
- Upgrade `org.hibernate:hibernate-core` to 6.x (`org.hibernate.orm:hibernate-core`)
- Upgrade `org.hibernate:hibernate-jpamodelgen` to `org.hibernate.orm:hibernate-jpamodelgen`
- Upgrade `org.hibernate.search:hibernate-search-mapper-orm` to 6.x

### Messaging upgrades
- Replace `org.hornetq:*` with `org.apache.activemq:artemis-*`
- If using Artemis client JARs directly, add `org.jboss.eap:wildfly-client-properties` dependency

4. Run the build gate

## Build gate

Run `mvn compile`. Common issues:
- BOM resolution failures from old `jboss-eap-jakartaee8` coordinates
- Missing `jakarta.*` API artifacts not yet covered by the new BOM
- Hibernate 6 API incompatibilities surfacing as compilation errors
- Transitive dependencies still pulling in `javax.*` artifacts
