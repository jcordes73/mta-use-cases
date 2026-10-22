# Dependency Map — JBoss EAP 7 to 8

| old_artifact | new_artifact | version_constraint | action | notes | source_section |
|---|---|---|---|---|---|
| org.jboss.bom:jboss-eap-jakartaee8 | org.jboss.bom:jboss-eap-ee | >= 8.0.0.GA | rename | Main Jakarta EE BOM renamed; update version to `8.0.0.GA-redhat-00009` or later | Maven Artifact Changes |
| org.jboss.bom:jboss-eap-jakartaee8-with-tools | org.jboss.bom:jboss-eap-ee-with-tools | >= 8.0.0.GA | rename | "With Tools" BOM renamed | Maven Artifact Changes |
| org.jboss.bom.eap:jboss-javaee-6.0-with-tools | org.jboss.bom:jboss-eap-ee-with-tools | >= 8.0.0.GA | replace | EAP 6-era BOM; update to EAP 8 unified BOM | Maven Artifact Changes |
| org.jboss.bom.eap:jboss-javaee-6.0-with-hibernate | org.jboss.bom:jboss-eap-ee-with-tools | >= 8.0.0.GA | replace | EAP 6-era Hibernate BOM; consolidated into unified BOM | Maven Artifact Changes |
| org.jboss.spec:jboss-jakartaee-8.0 | removed | | remove | Jakarta EE 8 Specification APIs BOM removed; use `jboss-eap-ee` BOM | Maven Artifact Changes |
| org.jboss.bom:jboss-eap-runtime-artifacts | removed | | remove | EAP Runtime Artifacts BOM removed; use `jboss-eap-ee` BOM | Maven Artifact Changes |
| org.jboss:jboss-ejb-client (legacy BOM) | removed | | remove | JBoss EJB client legacy BOM removed | Maven Artifact Changes |
| javax.servlet:javax.servlet-api | jakarta.servlet:jakarta.servlet-api | >= 6.0 | replace | Jakarta Servlet 6.0 in EAP 8; namespace `javax.servlet` → `jakarta.servlet` | Jakarta EE Namespace |
| javax.persistence:javax.persistence-api | jakarta.persistence:jakarta.persistence-api | >= 3.1 | replace | Jakarta Persistence 3.1 in EAP 8 | Jakarta EE Namespace |
| javax.ws.rs:javax.ws.rs-api | jakarta.ws.rs:jakarta.ws.rs-api | >= 3.1 | replace | Jakarta RESTful Web Services 3.1 in EAP 8 | Jakarta EE Namespace |
| javax.enterprise:cdi-api | jakarta.enterprise:jakarta.enterprise.cdi-api | >= 4.0 | replace | CDI 4.0 in EAP 8; bean discovery default changed | Jakarta EE Namespace |
| javax.ejb:javax.ejb-api | jakarta.ejb:jakarta.ejb-api | >= 4.0 | replace | Jakarta Enterprise Beans 4.0 in EAP 8 | Jakarta EE Namespace |
| javax.faces:javax.faces-api | jakarta.faces:jakarta.faces-api | >= 4.0 | replace | Jakarta Server Faces 4.0 in EAP 8 | Jakarta EE Namespace |
| javax.el:javax.el-api | jakarta.el:jakarta.el-api | >= 5.0 | replace | Jakarta Expression Language 5.0 in EAP 8 | Jakarta EE Namespace |
| javax.jms:javax.jms-api | jakarta.jms:jakarta.jms-api | >= 3.1 | replace | Java Message Service 3.1 in EAP 8 | Jakarta EE Namespace |
| javax.validation:validation-api | jakarta.validation:jakarta.validation-api | >= 3.0 | replace | Jakarta Bean Validation 3.0 in EAP 8 | Jakarta EE Namespace |
| javax.annotation:javax.annotation-api | jakarta.annotation:jakarta.annotation-api | >= 2.1 | replace | Jakarta Annotations 2.1 in EAP 8 | Jakarta EE Namespace |
| javax.xml.bind:jaxb-api | jakarta.xml.bind:jakarta.xml.bind-api | >= 4.0 | replace | Jakarta XML Binding 4.0 in EAP 8; JAXB 1.0 compat removed | Jakarta EE Namespace |
| javax.xml.soap:javax.xml.soap-api | jakarta.xml.soap:jakarta.xml.soap-api | >= 3.0 | replace | `SOAPElementFactory` removed; use `SOAPFactory` | Jakarta EE Namespace |
| javax.json:javax.json-api | jakarta.json:jakarta.json-api | >= 2.1 | replace | Jakarta JSON Processing 2.1 in EAP 8 | Jakarta EE Namespace |
| javax.json.bind:javax.json.bind-api | jakarta.json.bind:jakarta.json.bind-api | >= 3.0 | replace | Jakarta JSON Binding 3.0 in EAP 8 | Jakarta EE Namespace |
| org.hibernate:hibernate-core (5.x) | org.hibernate.orm:hibernate-core | >= 6.0 | replace | Hibernate ORM 6.x in EAP 8; major API changes | Hibernate Upgrade |
| org.hibernate.search:hibernate-search-mapper-orm (5.x) | org.hibernate.search:hibernate-search-mapper-orm | >= 6.0 | replace | Hibernate Search 6 APIs are backwards-incompatible with 5.x | Hibernate Search |
| log4j:log4j | removed | | remove | Apache Log4j v1 APIs removed from EAP 8; use JBoss Logging or Log4j 2 | Log4j Removal |
| org.picketbox:picketbox | removed | | remove | PicketBox removed; use Elytron credential store | Security Removal |
| org.picketlink:picketlink-api | removed | | remove | PicketLink removed; use Red Hat build of Keycloak | Security Removal |
| org.jboss.spec:jboss-jakartaee-web-8.0 | removed | | remove | Jakarta EE 8 Web APIs BOM removed; use `jboss-eap-ee` BOM | Maven Artifact Changes |
| org.jboss.bom:wildfly-ejb-client-legacy-bom | removed | | remove | EJB Client Legacy BOM removed; replace with EJB Client 4.x API | Maven Artifact Changes |
| com.sun.activation:jakarta.activation | jakarta.activation:jakarta.activation-api | | replace | Jakarta Activation API coordinate change | JBoss Spec Artifacts |
| org.jboss.spec.javax.annotation:jboss-annotations-api_1.3_spec | jakarta.annotation:jakarta.annotation-api | | replace | JBoss Annotations spec → Jakarta Annotations | JBoss Spec Artifacts |
| org.jboss.spec.javax.security.auth.message:jboss-jaspi-api_1.0_spec | jakarta.authentication:jakarta.authentication-api | | replace | JBoss JASPI spec → Jakarta Authentication | JBoss Spec Artifacts |
| org.jboss.spec.javax.security.jacc:jboss-jacc-api_1.5_spec | jakarta.authorization:jakarta.authorization-api | | replace | JBoss JACC spec → Jakarta Authorization | JBoss Spec Artifacts |
| org.jboss.spec.javax.batch:jboss-batch-api_1.0_spec | jakarta.batch:jakarta.batch-api | | replace | JBoss Batch spec → Jakarta Batch | JBoss Spec Artifacts |
| org.jboss.spec.javax.ejb:jboss-ejb-api_3.2_spec | jakarta.ejb:jakarta.ejb-api | | replace | JBoss EJB spec → Jakarta EJB | JBoss Spec Artifacts |
| org.jboss.spec.javax.el:jboss-el-api_3.0_spec | org.jboss.spec.jakarta.el:jboss-el-api_5.0_spec | | replace | JBoss EL spec → Jakarta EL 5.0 (still JBoss-published) | JBoss Spec Artifacts |
| org.jboss.spec.javax.enterprise.concurrent:jboss-concurrency-api_1.0_spec | jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api | | replace | JBoss Concurrency spec → Jakarta Concurrency | JBoss Spec Artifacts |
| org.jboss.spec.javax.faces:jboss-jsf-api_2.3_spec | jakarta.faces:jakarta.faces-api | | replace | JBoss JSF spec → Jakarta Faces | JBoss Spec Artifacts |
| org.jboss.spec.javax.interceptor:jboss-interceptors-api_1.2_spec | jakarta.interceptor:jakarta.interceptor-api | | replace | JBoss Interceptors spec → Jakarta Interceptors | JBoss Spec Artifacts |
| org.jboss.spec.javax.jms:jboss-jms-api_2.0_spec | jakarta.jms:jakarta.jms-api | | replace | JBoss JMS spec → Jakarta Messaging | JBoss Spec Artifacts |
| com.sun.mail:jakarta.mail | jakarta.mail:jakarta.mail-api | | replace | Sun JavaMail → Jakarta Mail API | JBoss Spec Artifacts |
| org.jboss.spec.javax.resource:jboss-connector-api_1.7_spec | jakarta.resource:jakarta.resource-api | | replace | JBoss Connector spec → Jakarta Connectors | JBoss Spec Artifacts |
| org.jboss.spec.javax.servlet:jboss-servlet-api_4.0_spec | jakarta.servlet:jakarta.servlet-api | | replace | JBoss Servlet spec → Jakarta Servlet | JBoss Spec Artifacts |
| org.jboss.spec.javax.servlet.jsp:jboss-jsp-api_2.3_spec | jakarta.servlet.jsp:jakarta.servlet.jsp-api | | replace | JBoss JSP spec → Jakarta Server Pages | JBoss Spec Artifacts |
| org.apache.taglibs:taglibs-standard-spec | jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api | | replace | Apache Taglibs → Jakarta JSTL | JBoss Spec Artifacts |
| org.jboss.spec.javax.transaction:jboss-transaction-api_1.3_spec | jakarta.transaction:jakarta.transaction-api | | replace | JBoss JTA spec → Jakarta Transactions | JBoss Spec Artifacts |
| org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec | jakarta.xml.bind:jakarta.xml.bind-api | | replace | JBoss JAXB spec → Jakarta XML Binding | JBoss Spec Artifacts |
| org.jboss.spec.javax.xml.ws:jboss-jaxws-api_2.3_spec | org.jboss.spec.jakarta.xml.ws:jboss-jakarta-xml-ws-api_4.0_spec | | replace | JBoss JAX-WS spec → Jakarta XML Web Services 4.0 | JBoss Spec Artifacts |
| javax.jws:jsr181-api | org.jboss.spec.jakarta.xml.ws:jboss-jakarta-xml-ws-api_4.0_spec | | replace | JSR-181 merged into Jakarta XML Web Services | JBoss Spec Artifacts |
| org.jboss.spec.javax.websocket:jboss-websocket-api_1.1_spec | jakarta.websocket:jakarta.websocket-api | | replace | JBoss WebSocket spec → Jakarta WebSocket | JBoss Spec Artifacts |
| org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec | jakarta.ws.rs:jakarta.ws.rs-api | | replace | JBoss JAX-RS spec → Jakarta REST | JBoss Spec Artifacts |
| org.jboss.spec.javax.xml.soap:jboss-saaj-api_1.4_spec | org.jboss.spec.jakarta.xml.soap:jboss-saaj-api_3.0_spec | | replace | JBoss SAAJ spec → Jakarta SOAP 3.0 | JBoss Spec Artifacts |
| org.hibernate:hibernate-jpamodelgen | org.hibernate.orm:hibernate-jpamodelgen | | replace | Hibernate JPA Metamodel Generator groupId changed | JBoss Spec Artifacts |
| org.jboss.narayana.xts:jbossxts | org.jboss.narayana.xts:jbossxts-jakarta (classifier: api) | | replace | Narayana XTS renamed to Jakarta variant | JBoss Spec Artifacts |
| org.jboss.resteasy:resteasy-jackson-provider | removed | | remove | Jackson 1 provider removed; use `resteasy-jackson2-provider` | RESTEasy Changes |
| org.jboss.resteasy:resteasy-jettison-provider | removed | | remove | Jettison JSON provider removed; use `resteasy-jackson2-provider` | RESTEasy Changes |
| org.jboss.resteasy:resteasy-yaml-provider | removed | | remove | YAML provider removed due to SnakeYAML security issues | RESTEasy Changes |
| org.hornetq:* | org.apache.activemq:artemis-* | | replace | HornetQ replaced by Apache ActiveMQ Artemis | Messaging Changes |
| org.jboss.eap:wildfly-client-properties | org.jboss.eap:wildfly-client-properties | | add | Required dependency for apps using Artemis client JARs directly | Messaging Changes |
