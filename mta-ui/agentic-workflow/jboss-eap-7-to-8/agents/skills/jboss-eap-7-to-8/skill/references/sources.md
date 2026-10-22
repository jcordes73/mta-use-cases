# Sources — JBoss EAP 7 to 8

Provenance links mapping each `source_section` value in the mapping tables to its original URL.

## Primary Guides

- [Red Hat JBoss EAP 8.0 Migration Guide](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index) — official guide (requires Red Hat account for full access)
- [How to migrate apps from JBoss EAP 7.x to JBoss EAP 8.0](https://developers.redhat.com/articles/2022/12/15/how-migrate-apps-jboss-eap-7x-jboss-eap-8-beta) — Red Hat Developer article (public)
- [From JBoss EAP 7 to 8: What Really Changed](https://www.dbi-services.com/blog/from-jboss-eap-7-to-8-what-really-changed/) — DBI Services blog (public)

## Section Mappings

- [Maven Artifact Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/migration_guide/migrate-a-jboss-eap-application-s-maven-project-to-jboss-eap-8-0_default) — BOM renames and dependency coordinate updates
- [Jakarta EE Namespace](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/migration_guide/application-migration-from-jakarta-ee-8-to-ee-10_default) — javax.* → jakarta.* package migration
- [Jakarta EE API Removals](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/migration_guide/application-migration-from-jakarta-ee-8-to-ee-10_default) — SOAPElementFactory, JAXB Validator removals
- [Security Removal](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/release_notes_for_red_hat_jboss_enterprise_application_platform_8.0/index#unsupported_deprecated_and_removed_functionality) — PicketBox, PicketLink removal
- [Security Subsystem](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/using_the_jboss_server_migration_tool/assembly_migrate-configs-to-current-version-server-migration-tool_server-migration-tool) — Legacy security to Elytron migration
- [OIDC Migration](https://developers.redhat.com/articles/2024/02/05/whats-new-jboss-enterprise-application-platform-8) — elytron-oidc-client subsystem
- [CDI 4.0 Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/migration_guide/application-migration-from-jakarta-ee-8-to-ee-10_default) — bean discovery mode default change
- [XML Namespace Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/migration_guide/application-migration-from-jakarta-ee-8-to-ee-10_default) — JAXB, web.xml, persistence.xml namespace updates
- [Hibernate Upgrade](https://docs.jboss.org/hibernate/orm/6.0/migration-guide/migration-guide.html) — Hibernate ORM 5→6 migration
- [Hibernate Search](https://docs.jboss.org/hibernate/search/6.0/migration/html_single/) — Hibernate Search 5→6 migration
- [Log4j Removal](https://www.dbi-services.com/blog/from-jboss-eap-7-to-8-what-really-changed/) — Log4j v1 APIs removed
- [Monitoring Changes](https://www.dbi-services.com/blog/from-jboss-eap-7-to-8-what-really-changed/) — Jolokia and Prometheus removal
- [JDK Requirements](https://developers.redhat.com/articles/2024/02/05/whats-new-jboss-enterprise-application-platform-8) — JDK 11/17 requirements
- [Server Provisioning](https://developers.redhat.com/articles/2024/02/05/whats-new-jboss-enterprise-application-platform-8) — eap-maven-plugin and Galleon
- [Cloud Deployment](https://developers.redhat.com/articles/2024/02/05/whats-new-jboss-enterprise-application-platform-8) — OpenShift S2I changes
- [Server Migration](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/using_the_jboss_server_migration_tool/assembly_migrate-configs-to-current-version-server-migration-tool_server-migration-tool) — Server Migration Tool usage
- [EJB Changes](https://kavindunilshanliyanage.medium.com/a-comprehensive-guide-to-upgrading-from-jboss-eap-7-4-cbe0b63129de) — @Stateful EJB behavioral changes
- [Classloading Changes](https://www.dbi-services.com/blog/from-jboss-eap-7-to-8-what-really-changed/) — Module lazy loading
- [JBoss Spec Artifacts](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#migrate-a-jboss-eap-application-s-maven-project-to-jboss-eap-8-0_default) — Chapter 5.5: 25+ JBoss spec artifact → Jakarta coordinate renames
- [CDI API Removals](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — CDI 4.0: `Bean.isNullable()`, `BeanManager.createInjectionTarget()`, `BeanManager.fireEvent()` removed
- [EJB API Removals](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — EJB: `getCallerIdentity()`, `isCallerInRole(Identity)`, `getMessageContext()`, `getEnvironment()` removed
- [EL API Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — EL 5.0: typo fix `isParmetersProvided()` → `isParametersProvided()`
- [Servlet 6.0 Removals](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — Servlet 6.0: `SingleThreadModel`, `HttpSessionContext`, `HttpUtils`, 15+ deprecated methods removed
- [Faces Removals](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — Faces 4.0: JSP views, `@ManagedBean`, `ResourceResolver` removed
- [JSON Binding Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — `@JsonbCreator` parameter requirements relaxed
- [JAXB Implementation](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#application-migration-from-jakarta-ee-8-to-ee-10_default) — `com.sun.xml.bind` → `org.glassfish.jaxb.runtime` package and property prefix
- [Vault Migration](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#security-application-changes_default) — `${VAULT::` expressions → Elytron encrypted expressions
- [EJB Client Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#understanding-application-migration-changes_default) — Remote port 4447→8080, `remote://`→`http-remoting://`, `wildfly-config.xml`
- [Messaging Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#messaging-server-configuration-changes_default) — HornetQ→Artemis, deployment descriptor namespace, embedded broker Galleon layer
- [Undertow Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#web-server-configuration-changes_default) — Response headers removed, HTTPS/HTTP-invoker Elytron migration, valves→handlers
- [mod_cluster Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#mod_cluster-configuration-changes_default) — `proxy-list`→`proxies`, `connector`→`listener`, config path change
- [Infinispan Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#infinispan-server-configuration-changes_default) — SFSB cache passivation: idle-timeout removed, lazy passivation only
- [EJB Subsystem Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#ejb-subsystem-configuration-changes-from-version-8-0-and-later_default) — `cache`/`passivation-store` → `simple-cache`/`distributable-cache`
- [RESTEasy Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#jakarta-restful-web-services-and-resteasy-application-changes_default) — Provider removals, interceptor→filter migration, SPI exception replacements, Jackson 2.x
- [Jackson Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#jackson-provider-changes_default) — `org.codehaus.jackson` → `com.fasterxml.jackson` package
- [Web Services Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#web-services-application-changes_default) — JAX-RPC removal, Spring CXF integration removal, CXF bus strategy
- [WS-Security Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#ws-security-changes_default) — WSPasswordCallback/SAML package moves, RSA v1.5 disallowed, CN check property
- [JBoss Logging Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#jboss-logging-changes_default) — Annotations moved to `org.jboss.logging.annotations` package
- [HTTP Session Changes](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html-single/migration_guide/index#http-session-id-change_default) — Session ID no longer includes instance ID suffix

## Supplementary: Konveyor Issues

- [#285](https://github.com/konveyor/rulesets/issues/285) — EAP 8 ruleset does not catch old EAP BOM dependencies
- [#268](https://github.com/konveyor/rulesets/issues/268) — Rules for EAP 8.1: org.apache.activemq.artemis module changes
