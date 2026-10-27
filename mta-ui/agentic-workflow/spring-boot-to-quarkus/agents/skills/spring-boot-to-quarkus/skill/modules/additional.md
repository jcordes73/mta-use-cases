# Phase: Additional Changes

Handle template engine migration, health/metrics, scheduling, caching, events, and other non-standard changes.

## Steps

### Thymeleaf to Qute Migration

1. If the project uses Thymeleaf templates:
   - Read `references/pattern-map.md` — filter for rows with `source_section` = "Thymeleaf to Qute Migration"
   - Convert template syntax:
     - `th:text="${expr}"` → `{expr}`
     - `th:if="${cond}"` → `{#if cond}...{/if}`
     - `th:unless="${cond}"` → `{#if !cond}...{/if}`
     - `th:each="item : ${items}"` → `{#for item in items}...{/for}`
     - `th:include="fragment"` → `{#include fragment /}`
     - `#{message.key}` → `{msg:message_key}`
   - Rename template files from `.html` to `.html` (same extension, but update syntax inside)
   - Update controllers: replace `Model` parameter with `TemplateInstance` return type
   - Template location stays at `src/main/resources/templates/`

### Health and Metrics

2. If the project uses Spring Boot Actuator:
   - Custom `HealthIndicator` implementations → implement `org.eclipse.microprofile.health.HealthCheck` with `@Liveness` or `@Readiness`
   - Update any hardcoded actuator paths: `/actuator/health` → `/q/health`, `/actuator/metrics` → `/q/metrics`
   - If clients depend on actuator endpoint format, configure `quarkus.http.non-application-root-path`

### Caching

3. If the project uses Spring Cache:
   - `@Cacheable` → `@CacheResult(cacheName = "...")`
   - `@CacheEvict` → `@CacheInvalidate(cacheName = "...")`
   - `@CachePut` → `@CacheResult(cacheName = "...")` (no direct equivalent)
   - Remove `@EnableCaching` — caching is enabled by the `quarkus-cache` extension
   - Or add `quarkus-spring-cache` for compatibility

### Scheduling

4. If the project uses Spring Scheduling:
   - `@Scheduled(fixedRate = 5000)` → `@io.quarkus.scheduler.Scheduled(every = "5s")`
   - `@Scheduled(cron = "...")` → `@Scheduled(cron = "...")`
   - Remove `@EnableScheduling` — scheduling is enabled by the `quarkus-scheduler` extension
   - Or add `quarkus-spring-scheduled` for compatibility

### Events

5. If the project uses Spring events:
   - `ApplicationEventPublisher` → inject `Event<MyEvent>` via `@Inject`
   - `publisher.publishEvent(event)` → `event.fire(event)`
   - `@EventListener` → `void onEvent(@Observes MyEvent event)`

### Spring Compatibility Extensions (stepping stone)

6. If the project chose the compatibility path in the build-system phase, verify these extensions are working:
   - `quarkus-spring-di` — `@Autowired`, `@Component`, `@Service`, `@Repository`, `@Bean`, `@Configuration`
   - `quarkus-spring-web` — `@RestController`, `@GetMapping`, `@PostMapping`, `@RequestMapping`
   - `quarkus-spring-data-jpa` — `JpaRepository`, `CrudRepository`, derived query methods
   - `quarkus-spring-security` — `@Secured`, `@RolesAllowed`
   - `quarkus-spring-cache` — `@Cacheable`, `@CacheEvict`
   - `quarkus-spring-scheduled` — `@Scheduled` (Spring variant)

7. Run the build gate

## Build gate

Run `mvn compile`. Also verify the application starts with `mvn quarkus:dev` and check that health endpoints respond.
