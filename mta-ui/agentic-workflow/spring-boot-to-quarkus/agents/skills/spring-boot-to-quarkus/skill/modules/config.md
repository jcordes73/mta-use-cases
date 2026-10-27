# Phase: Configuration Migration

Migrate configuration properties from Spring Boot naming to Quarkus naming.

## Steps

1. Read `references/config-map.md`
2. Identify all configuration files in the project:
   - `application.properties`, `application.yml`
   - `application-*.properties`, `application-*.yml` (profiles)
   - `bootstrap.properties`, `bootstrap.yml`
3. For each row in the config map:
   - Search all config files for `old_property`
   - If found:
     - Replace with `new_property` (or remove if `new_property` is `removed`)
     - If `default_changed` is `true`, check whether the project relies on the old default
   - Check `notes` for migration context
4. Key property migrations:
   - `server.port` → `quarkus.http.port`
   - `server.servlet.context-path` → `quarkus.http.root-path`
   - `spring.datasource.url` → `quarkus.datasource.jdbc.url`
   - `spring.datasource.username` → `quarkus.datasource.username`
   - `spring.datasource.password` → `quarkus.datasource.password`
   - `spring.datasource.driver-class-name` → `quarkus.datasource.db-kind` (use `postgresql`, `mysql`, etc.)
   - `spring.jpa.hibernate.ddl-auto` → `quarkus.hibernate-orm.database.generation`
   - `logging.level.root` → `quarkus.log.level`
   - `logging.level.<pkg>` → `quarkus.log.category."<pkg>".level`
   - `spring.profiles.active` → `quarkus.profile`
5. **Handle OSIV removal:**
   - Quarkus has no Open Session in View. If the project relies on `spring.jpa.open-in-view=true` (Spring default), lazy loading outside `@Transactional` boundaries will fail
   - Add `@Transactional` to methods that access lazy-loaded entities, or switch to eager fetching
6. **Add Quarkus-specific config if needed:**
   - `quarkus.datasource.db-kind` (required — e.g., `postgresql`, `mysql`, `h2`)
   - `quarkus.http.cors=true` if CORS was configured via Spring
7. Run the build gate

## Build gate

Run `mvn compile`. Configuration errors often surface as startup failures rather than compilation errors — also verify the application starts with `mvn quarkus:dev`.
