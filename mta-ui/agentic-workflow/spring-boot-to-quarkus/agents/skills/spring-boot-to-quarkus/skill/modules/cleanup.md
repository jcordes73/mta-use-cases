# Phase: Cleanup and Verification

Remove remaining Spring artifacts, verify no old imports remain, and validate the final build.

## Steps

1. **Remove remaining Spring imports:** Search for any remaining imports of Spring packages:
   - `org.springframework.boot.*`
   - `org.springframework.beans.*`
   - `org.springframework.context.*`
   - `org.springframework.web.*`
   - `org.springframework.stereotype.*`
   - `org.springframework.scheduling.*`
   - `org.springframework.cache.*`
   - `org.springframework.test.*`
   - `org.springframework.data.*` (unless using `quarkus-spring-data-jpa`)
   - `org.springframework.security.*` (unless using `quarkus-spring-security`)
   
   Exception: If using Spring compatibility extensions (`quarkus-spring-di`, `quarkus-spring-web`, etc.), Spring annotations from those extensions are valid and should NOT be removed.

2. **Verify no old artifacts remain:**
   - Search build files for Spring Boot starter dependencies that should have been replaced
   - Search for `spring-boot-maven-plugin` or `spring-boot-starter-parent`
   - Search for `spring-boot-devtools` or `spring-boot-configuration-processor`
   - Search config files for Spring Boot property names (`server.port`, `spring.datasource.*`, `spring.jpa.*`, `logging.level.*`)

3. **Remove Spring Boot auto-configuration artifacts:**
   - Delete `src/main/resources/META-INF/spring.factories` if it exists
   - Delete `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` if it exists

4. **Verify Quarkus-specific files:**
   - Ensure `application.properties` uses Quarkus property names
   - Check that `pom.xml` has the Quarkus BOM and plugin
   - Verify `quarkus.datasource.db-kind` is set if using a database

5. **Final build and test:**
   - Run the full build: `mvn compile test`
   - Verify the application starts: `mvn quarkus:dev`
   - Check health endpoint: `curl http://localhost:8080/q/health` (if SmallRye Health is configured)

6. **Report to user:**
   - List all changes made across all phases
   - Flag any items that could not be automatically migrated (require manual review)
   - Note behavioral changes the user should verify:
     - No OSIV — lazy loading outside transactions will fail
     - No Spring Boot auto-configuration — beans must come from extensions or be explicitly defined
     - `@ConditionalOn*` annotations have no Quarkus equivalent
     - Actuator endpoint paths changed (`/actuator/*` → `/q/*`)
     - Dev mode: `mvn quarkus:dev` instead of `mvn spring-boot:run`
     - Packaging: Quarkus produces a different JAR structure (fast-jar by default)
     - Spring Boot `@ConfigurationProperties` classes must be converted to `@ConfigMapping` interfaces
