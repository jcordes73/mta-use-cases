# Config Map — JBoss EAP 7 to 8

| old_property | new_property | default_changed | old_default | new_default | file_pattern | notes | source_section |
|---|---|---|---|---|---|---|---|
| urn:jboss:domain:security:2.0 (subsystem) | urn:jboss:domain:elytron:* | | | | standalone.xml, domain.xml | Legacy security subsystem removed; auto-migrated by Server Migration Tool | Security Subsystem |
| security-realm ManagementRealm | elytron ManagementRealm | | | | standalone.xml | Legacy security realm migrated to Elytron equivalent | Security Subsystem |
| security-realm ApplicationRealm | elytron ApplicationRealm | | | | standalone.xml | Legacy security realm migrated to Elytron equivalent | Security Subsystem |
| security-domain other | elytron security-domain | | | | standalone.xml | Legacy security domain migrated to Elytron | Security Subsystem |
| vault (PicketBox credential storage) | credential-store (Elytron) | | | | standalone.xml | PicketBox vault removed; use Elytron credential stores | Security Subsystem |
| keycloak subsystem | elytron-oidc-client subsystem | | | | standalone.xml | Keycloak adapter subsystem replaced by native Elytron OIDC | OIDC Migration |
| bean-discovery-mode (CDI beans.xml) | bean-discovery-mode | true | `all` (for empty beans.xml) | `annotated` (for empty beans.xml) | beans.xml | CDI 4.0: empty beans.xml now defaults to `annotated`; add `bean-discovery-mode="all"` to preserve old behavior | CDI 4.0 Changes |
| http://java.sun.com/xml/ns/jaxb (XML namespace) | https://jakarta.ee/xml/ns/jaxb | | | | JAXB binding files | XML binding namespace updated for Jakarta EE | XML Namespace Changes |
| META-INF/services/javax.* | META-INF/services/jakarta.* | | | | ServiceLoader files | Service provider interface files must use `jakarta.*` names | Jakarta EE Namespace |
| version.server.bom (Maven property) | version.server.bom | | `7.4.0.GA` | `8.0.0.GA-redhat-00009` | pom.xml | BOM version property must be updated | Maven Artifact Changes |
| urn:jboss:domain:undertow (response header filters) | removed | | Server/X-Powered-By headers present | Headers removed | standalone.xml | `server-header` and `x-powered-by-header` filter-refs removed since EAP 7.2 for info disclosure prevention | Undertow Changes |
| https-listener security-realm="ApplicationRealm" | https-listener ssl-context="applicationSSC" | | Legacy security realm | Elytron ssl-context | standalone.xml | Undertow HTTPS listener uses Elytron TLS config | Undertow Changes |
| http-invoker security-realm="ApplicationRealm" | http-invoker http-authentication-factory="application-http-authentication" | | Legacy security realm | Elytron auth factory | standalone.xml | HTTP invoker auth migrated to Elytron | Undertow Changes |
| /subsystem=ejb3/cache | /subsystem=ejb3/simple-cache (non-distributable) | | | | standalone.xml | Deprecated; use `simple-cache` for non-distributable SFSB | EJB Subsystem Changes |
| /subsystem=ejb3/passivation-store | /subsystem=ejb3/distributable-cache + /subsystem=distributable-ejb | | | | standalone.xml | Deprecated; use `distributable-cache` with `distributable-ejb` subsystem for distributable SFSB | EJB Subsystem Changes |
| /subsystem=ejb3/passivation-store idle-timeout | removed | | Eager passivation on idle timeout | Lazy passivation on max-size only | standalone.xml | Eager passivation via idle-timeout no longer supported since EAP 7.1 | Infinispan Changes |
| /subsystem=modcluster/mod-cluster-config=configuration | /subsystem=modcluster/proxy=default | | | | standalone.xml | mod_cluster configuration path changed; update CLI scripts | mod_cluster Changes |
| proxy-list (mod_cluster attribute) | proxies (list of outbound socket binding names) | | | | standalone.xml | Deprecated in EAP 7.4, removed in EAP 8.0 | mod_cluster Changes |
| connector (mod_cluster attribute) | listener | | | | standalone.xml | Deprecated `connector` attribute removed; use `listener` | mod_cluster Changes |
| ssl=configuration (mod_cluster) | elytron-based SSL configuration | | | | standalone.xml | Legacy mod_cluster SSL replaced by Elytron SSL context | mod_cluster Changes |
| replication-primary check-for-live-server | replication-primary check-for-live-server | true | `false` (EAP 7.0) | `true` (EAP 7.1+) | standalone-full-ha.xml | Default changed; affects HA messaging failover behavior | Messaging Changes |
| urn:jboss:messaging-deployment:1.0 | urn:jboss:messaging-activemq-deployment:1.0 | | | | *-jms.xml | HornetQ deployment descriptor namespace → ActiveMQ Artemis | Messaging Changes |
| `<hornetq-server>` | `<server>` | | | | *-jms.xml | Element renamed in messaging deployment descriptors | Messaging Changes |
| remote.connection.default.port | remote.connection.default.port | true | `4447` | `8080` | jboss-ejb-client.properties | Default EJB remote port changed | EJB Client Changes |
| java.naming.provider.url (remote://) | java.naming.provider.url (http-remoting://) | | `remote://localhost:4447` | `http-remoting://localhost:8080` | jboss-ejb-client.properties, JNDI config | Remote JNDI provider URL scheme and port changed | EJB Client Changes |
| jboss-ejb-client.properties | META-INF/wildfly-config.xml | | | | jboss-ejb-client.properties → wildfly-config.xml | Unified client config file introduced in EAP 7.1; recommended for EAP 8 | EJB Client Changes |
| resteasy.add.charset | resteasy.add.charset | true | `false` | `true` | web.xml (context-param) | RESTEasy now adds `charset=UTF-8` to `text/*` and `application/xml*` responses by default | RESTEasy Changes |
| org.jboss.ws.cxf.jaxws-client.bus.strategy | org.jboss.ws.cxf.jaxws-client.bus.strategy | true | `THREAD_BUS` | `TCCL_BUS` (in-container) | system property | CXF bus selection strategy changed for in-container clients | Web Services Changes |
| org.jboss.security.ignoreHttpsHost | cxf.tls-client.disableCNCheck | | | | system property | System property renamed for HTTPS CN check bypass | WS-Security Changes |
