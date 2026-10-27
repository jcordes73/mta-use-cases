# Dependency Map — Spring Boot 3.x to Quarkus 3.x

| old_artifact | new_artifact | version_constraint | action | notes | source_section |
|---|---|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | io.quarkus.platform:quarkus-bom | | replace | Remove `<parent>` block; add Quarkus BOM to `<dependencyManagement>` as `<type>pom</type>` `<scope>import</scope>` | Replace the Spring Boot parent with the Quarkus BOM |
| org.springframework.boot:spring-boot-starter-web | io.quarkus:quarkus-rest-jackson | | replace | For reactive stack; use `quarkus-resteasy-jackson` for classic servlet-style. Spring MVC annotations require `quarkus-spring-web` compat extension | Dependency Migration |
| org.springframework.boot:spring-boot-starter-webflux | io.quarkus:quarkus-rest-jackson | | replace | Quarkus RESTEasy Reactive replaces WebFlux for non-blocking HTTP | Dependency Migration |
| org.springframework.boot:spring-boot-starter-data-jpa | io.quarkus:quarkus-hibernate-orm-panache | | replace | Also add the appropriate `quarkus-jdbc-*` driver extension. For Spring Data compat, use `quarkus-spring-data-jpa` instead | Dependency Migration |
| org.springframework.boot:spring-boot-starter-security | io.quarkus:quarkus-security | | replace | For OAuth2/OIDC use `quarkus-oidc` instead. For Spring Security compat, add `quarkus-spring-security` | Dependency Migration |
| org.springframework.boot:spring-boot-starter-oauth2-client | io.quarkus:quarkus-oidc | | replace | Quarkus OIDC handles both client and resource server roles | Dependency Migration |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server | io.quarkus:quarkus-oidc | | replace | Quarkus OIDC handles both client and resource server roles | Dependency Migration |
| org.springframework.boot:spring-boot-starter-actuator | io.quarkus:quarkus-smallrye-health | | replace | Also add `quarkus-micrometer` for metrics. Health at `/q/health`, metrics at `/q/metrics` | Dependency Migration |
| org.springframework.boot:spring-boot-starter-test | io.quarkus:quarkus-junit5 | | replace | Also add `io.rest-assured:rest-assured` for HTTP testing (replaces MockMvc). Add `quarkus-junit5-mockito` for `@InjectMock` | Testing Migration |
| org.springframework.boot:spring-boot-starter-validation | io.quarkus:quarkus-hibernate-validator | | replace | Bean Validation API unchanged; implementation switches to Quarkus-managed Hibernate Validator | Dependency Migration |
| org.springframework.boot:spring-boot-starter-cache | io.quarkus:quarkus-cache | | replace | Annotations change: `@Cacheable` → `@CacheResult`, `@CacheEvict` → `@CacheInvalidate`. For Spring compat, add `quarkus-spring-cache` | Dependency Migration |
| org.springframework.boot:spring-boot-starter-data-mongodb | io.quarkus:quarkus-mongodb-panache | | replace | Config properties change: `spring.data.mongodb.uri` → `quarkus.mongodb.connection-string` | Dependency Migration |
| org.springframework.boot:spring-boot-starter-data-redis | io.quarkus:quarkus-redis-client | | replace | Redis client API differs significantly from Spring Data Redis | Dependency Migration |
| org.springframework.boot:spring-boot-starter-amqp | io.quarkus:quarkus-messaging-rabbitmq | | replace | Uses SmallRye Reactive Messaging; API is significantly different from Spring AMQP | Dependency Migration |
| org.springframework.kafka:spring-kafka | io.quarkus:quarkus-messaging-kafka | | replace | Uses SmallRye Reactive Messaging for Kafka; annotation-based with `@Incoming`/`@Outgoing` | Dependency Migration |
| org.springframework.boot:spring-boot-starter-websocket | io.quarkus:quarkus-websockets | | replace | Uses Jakarta WebSocket API instead of Spring WebSocket | Dependency Migration |
| org.springframework.boot:spring-boot-starter-thymeleaf | io.quarkus:quarkus-qute | | replace | Thymeleaf syntax must be converted to Qute syntax; see Thymeleaf to Qute Migration section | Thymeleaf to Qute Migration |
| org.springframework.boot:spring-boot-starter-quartz | io.quarkus:quarkus-quartz | | replace | Quartz API largely unchanged; configuration properties differ | Dependency Migration |
| org.springframework.boot:spring-boot-starter-artemis | io.quarkus:quarkus-artemis-jms | | replace | JMS API unchanged; connection configuration differs | Dependency Migration |
| org.springframework.boot:spring-boot-starter-mail | io.quarkus:quarkus-mailer | | replace | Quarkus Mailer has a different API from Spring's JavaMailSender | Dependency Migration |
| org.springframework.boot:spring-boot-maven-plugin | io.quarkus.platform:quarkus-maven-plugin | | replace | Add `build`, `generate-code`, `generate-code-tests` goals. Set `<extensions>true</extensions>` | Replace the Spring Boot plugin with the Quarkus plugin |
| org.springframework.boot:spring-boot-devtools | removed | | remove | Quarkus dev mode is built-in: `mvn quarkus:dev`. No dependency needed | Dev Mode |
| org.springframework.boot:spring-boot-starter-logging | removed | | remove | Quarkus logging is built-in (JBoss Logging). Configure via `quarkus.log.*` properties | Dependency Migration |
| org.springframework.boot:spring-boot-configuration-processor | removed | | remove | Quarkus handles configuration at build time; no annotation processor needed | Dependency Migration |
| org.postgresql:postgresql | io.quarkus:quarkus-jdbc-postgresql | | replace | Quarkus manages the driver; configure via `quarkus.datasource.db-kind=postgresql` | Database Drivers |
| com.mysql:mysql-connector-j | io.quarkus:quarkus-jdbc-mysql | | replace | Quarkus manages the driver; configure via `quarkus.datasource.db-kind=mysql` | Database Drivers |
| mysql:mysql-connector-java | io.quarkus:quarkus-jdbc-mysql | | replace | Old MySQL connector coordinate | Database Drivers |
| com.h2database:h2 | io.quarkus:quarkus-jdbc-h2 | | replace | Quarkus manages the driver; configure via `quarkus.datasource.db-kind=h2` | Database Drivers |
| org.mariadb.jdbc:mariadb-java-client | io.quarkus:quarkus-jdbc-mariadb | | replace | Quarkus manages the driver; configure via `quarkus.datasource.db-kind=mariadb` | Database Drivers |
| org.springframework.security:spring-security-test | io.quarkus:quarkus-test-security | | replace | Provides `@TestSecurity` annotation for test security context | Testing Migration |
