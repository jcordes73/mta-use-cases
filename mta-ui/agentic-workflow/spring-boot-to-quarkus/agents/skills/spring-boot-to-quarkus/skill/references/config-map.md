# Config Map — Spring Boot 3.x to Quarkus 3.x

| old_property | new_property | default_changed | old_default | new_default | file_pattern | notes | source_section |
|---|---|---|---|---|---|---|---|
| server.port | quarkus.http.port | | 8080 | 8080 | application.properties | Same default; different property name | Server |
| server.servlet.context-path | quarkus.http.root-path | | / | / | application.properties | | Server |
| server.ssl.key-store | quarkus.http.ssl.certificate.key-store-file | | | | application.properties | | Server |
| server.ssl.key-store-password | quarkus.http.ssl.certificate.key-store-password | | | | application.properties | | Server |
| server.ssl.key-store-type | quarkus.http.ssl.certificate.key-store-file-type | | | | application.properties | | Server |
| spring.datasource.url | quarkus.datasource.jdbc.url | | | | application.properties | | Datasource |
| spring.datasource.username | quarkus.datasource.username | | | | application.properties | | Datasource |
| spring.datasource.password | quarkus.datasource.password | | | | application.properties | | Datasource |
| spring.datasource.driver-class-name | quarkus.datasource.db-kind | | | | application.properties | Use db-kind value (e.g., `postgresql`, `mysql`, `h2`) instead of JDBC driver class | Datasource |
| spring.datasource.hikari.maximum-pool-size | quarkus.datasource.jdbc.max-size | | 10 | 20 | application.properties | Quarkus default pool size is 20 | Datasource |
| spring.datasource.hikari.minimum-idle | quarkus.datasource.jdbc.min-size | | 10 | 0 | application.properties | Quarkus default min-size is 0 (dynamic sizing) | Datasource |
| spring.jpa.hibernate.ddl-auto | quarkus.hibernate-orm.database.generation | | none | none | application.properties | Values: `none`, `create`, `drop-and-create`, `update`, `validate` | JPA / Hibernate |
| spring.jpa.show-sql | quarkus.hibernate-orm.log.sql | | false | false | application.properties | | JPA / Hibernate |
| spring.jpa.properties.hibernate.format_sql | quarkus.hibernate-orm.log.format-sql | | false | false | application.properties | | JPA / Hibernate |
| spring.jpa.properties.hibernate.dialect | quarkus.hibernate-orm.dialect | | | | application.properties | Quarkus auto-detects dialect from db-kind; usually not needed | JPA / Hibernate |
| spring.jpa.open-in-view | removed | | true | | application.properties | No OSIV in Quarkus; lazy loading outside transactions will fail. Use `@Transactional` or fetch eagerly | JPA / Hibernate |
| logging.level.root | quarkus.log.level | | INFO | INFO | application.properties | | Logging |
| logging.level.{package} | quarkus.log.category."{package}".level | | | | application.properties | Package name must be quoted in Quarkus config | Logging |
| logging.file.name | quarkus.log.file.path | | | | application.properties | Also set `quarkus.log.file.enable=true` | Logging |
| logging.pattern.console | quarkus.log.console.format | | | | application.properties | Quarkus uses JBoss Log Manager format patterns | Logging |
| spring.profiles.active | quarkus.profile | | | | application.properties | Profile names work the same; `application-{profile}.properties` convention is shared | Profiles |
| spring.data.mongodb.uri | quarkus.mongodb.connection-string | | | | application.properties | | MongoDB |
| spring.data.mongodb.database | quarkus.mongodb.database | | | | application.properties | | MongoDB |
| spring.data.redis.host | quarkus.redis.hosts | | localhost | | application.properties | Format: `redis://host:port`; combines host and port | Redis |
| spring.data.redis.port | quarkus.redis.hosts | | 6379 | | application.properties | Included in the `redis://host:port` URL | Redis |
| spring.data.redis.password | quarkus.redis.password | | | | application.properties | Or use `redis://:password@host:port` in hosts URL | Redis |
| spring.jackson.serialization.* | quarkus.jackson.serialization-inclusion | | | | application.properties | Not all Spring Jackson properties have direct Quarkus equivalents; configure via `ObjectMapperCustomizer` | Configuration |
| spring.jackson.date-format | quarkus.jackson.date-format | | | | application.properties | | Configuration |
| management.server.port | quarkus.management.port | | | | application.properties | Requires `quarkus-management` extension for separate management port | Health and Metrics |
| management.endpoints.web.base-path | quarkus.http.non-application-root-path | | /actuator | /q | application.properties | Quarkus uses `/q` prefix by default for non-application endpoints | Health and Metrics |
| spring.flyway.enabled | quarkus.flyway.migrate-at-start | | | | application.properties | Different property name and semantics; `migrate-at-start=true` to auto-migrate | JPA / Hibernate |
| spring.flyway.locations | quarkus.flyway.locations | | | | application.properties | | JPA / Hibernate |
| spring.liquibase.change-log | quarkus.liquibase.change-log | | | | application.properties | | JPA / Hibernate |
