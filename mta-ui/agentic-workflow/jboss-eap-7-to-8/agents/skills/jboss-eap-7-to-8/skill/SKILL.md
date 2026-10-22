---
name: jboss-eap-7-to-8
description: Migrates JBoss EAP 7.x applications to JBoss EAP 8.0. Use when
  upgrading a JBoss Enterprise Application Platform project from version 7 to 8.
license: Apache-2.0
metadata:
  source: jboss-eap-7
  target: jboss-eap-8
  language: java
  build_tool: "maven: mvn compile"
  guide_url:
    - https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index
    - https://developers.redhat.com/articles/2022/12/15/how-migrate-apps-jboss-eap-7x-jboss-eap-8-beta
    - https://www.dbi-services.com/blog/from-jboss-eap-7-to-8-what-really-changed/
  generated_by: migration-skills-generator
  generated_at: 2026-06-12T00:00:00Z
---

# JBoss EAP 7 to 8 Migration

**Prerequisite:** Upgrade to JBoss EAP 7.4 (latest patch) before migrating to EAP 8.0. If running EAP 6 or earlier, you must first migrate to EAP 7.4.

**Prerequisite:** Migrate from PicketBox legacy security to the Elytron security subsystem before upgrading. PicketBox and PicketLink are fully removed in EAP 8.

**Prerequisite:** Ensure JDK 11 minimum (JDK 17 recommended). JDK 8 is no longer supported.

JBoss EAP 8.0 upgrades from Jakarta EE 8 to Jakarta EE 10, making the `javax.*` → `jakarta.*` namespace migration the single largest change. Additional breaking changes include: removal of PicketBox/PicketLink legacy security (replaced by Elytron), Hibernate ORM 5→6 and Hibernate Search 5→6 upgrades, removal of Log4j v1 APIs, removal of Jolokia/Prometheus monitoring, CDI 4.0 bean discovery default change, Maven BOM renames, 25+ JBoss spec artifact coordinate renames, Servlet 6.0/Faces 4.0/JAXB 4.0 API removals, JAXB implementation package relocation, RESTEasy 6.2 provider/interceptor changes, EJB client remote port/connector changes, HornetQ→ActiveMQ Artemis messaging migration, mod_cluster/Undertow/EJB subsystem configuration updates, XML descriptor namespace updates, and Galleon-based provisioning. This skill covers 225 migration items across dependency (58), API (71), configuration (29), and pattern (67) changes.

## Phases

Execute in order. After each phase, run the project build and stop if it fails.

1. **Build system** — Update Maven BOMs (`jboss-eap-jakartaee8` → `jboss-eap-ee`), replace 25+ JBoss spec artifact coordinates with Jakarta equivalents, replace `javax.*` dependency coordinates, remove Log4j v1/PicketBox/PicketLink/RESTEasy legacy providers, upgrade Hibernate/Jackson, add HornetQ→Artemis dependencies. Module: `modules/build-system.md`, Reference: `references/dependency-map.md`

2. **Code** — Migrate all `javax.*` EE imports to `jakarta.*`, apply Servlet 6.0/Faces 4.0/CDI 4.0/EJB API removals, relocate JAXB impl packages (`com.sun.xml.bind` → `org.glassfish.jaxb.runtime`), migrate RESTEasy interceptors/SPI exceptions to Jakarta REST, update JBoss Logging annotations, WS-Security packages, Jackson annotations, EJB client/JNDI naming. Module: `modules/code.md`, References: `references/api-map.md`, `references/pattern-map.md`

3. **Config** — Update server configuration (`standalone.xml`): remove legacy security subsystem, migrate to Elytron (including HTTPS/HTTP-invoker), update Undertow/mod_cluster/EJB subsystem/messaging configs, update CDI `beans.xml` discovery mode, update XML descriptor namespaces, update EJB client properties, rename `META-INF/services` files. Module: `modules/config.md`, Reference: `references/config-map.md`

4. **Testing** — Verify Jakarta EE namespace changes in test code, update mock/test configurations for Elytron, validate CDI bean discovery, test Servlet 6.0 deprecated method replacements, verify RESTEasy `@Produces` annotations. Module: `modules/testing.md`

5. **Additional** — Server provisioning (Galleon/`eap-maven-plugin`), OpenShift deployment changes, monitoring migration (Jolokia/Prometheus → `/metrics`), Hibernate ORM/Search 6 migration, messaging data migration (HornetQ→Artemis), OIDC adapter migration, vault→credential-store. Module: `modules/additional.md`

6. **Cleanup** — Remove old `javax.*` EE imports, verify no legacy security references, validate deployment descriptors. Module: `modules/cleanup.md`

## How to use

Load each phase's module when starting that phase. Each module references mapping tables in `references/` — apply every row in the relevant table to the codebase. Use the before/after examples as guides for each transformation.

## Build gate

After completing each phase:
1. Detect the project's build tool (check metadata `build_tool` field above, or detect from project files: `pom.xml` → `mvn compile`, `build.gradle` → `gradle build`)
2. Run the build
3. If it fails, fix the issue before proceeding
4. If you cannot fix it, stop and report to the user
