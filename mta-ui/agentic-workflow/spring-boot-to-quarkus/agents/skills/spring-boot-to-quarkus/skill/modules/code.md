# Phase: Code Migration

Replace Spring annotations, APIs, and code patterns with CDI/JAX-RS/MicroProfile equivalents.

## Steps

### Part 1: API replacements

1. Read `references/api-map.md`
2. Process rows in `kind` order to prevent conflicts:
   - First: `package` — apply package-level renames/moves
   - Then: `class` — update class references (SpringApplication, ResponseEntity, MockMvc, etc.)
   - Then: `annotation` — swap annotation imports and usages
   - Then: `interface` — update interface references
   - Then: `method` — update method calls
   - Finally: `field` — update field references
3. For each row:
   - Search the codebase for `old_api` (check imports, type references, annotations)
   - Apply the `action`:
     - `replace` → substitute with `new_api`
     - `remove` → delete usage; if `after` column has a replacement pattern, use it
     - `move_package` → update import statements to new package
   - Use the `before`/`after` columns as concrete examples

4. **If using Spring compatibility extensions** (from build-system phase):
   - Skip DI annotation changes (`@Autowired`, `@Component`, `@Service`, `@Repository`, `@Configuration`, `@Bean`) — these are handled by `quarkus-spring-di`
   - Skip web annotation changes (`@RestController`, `@GetMapping`, etc.) — these are handled by `quarkus-spring-web`
   - Still apply: entry point changes, `@Value` → `@ConfigProperty`, testing changes, event handling, caching, scheduling

### Part 2: Pattern changes

5. Read `references/pattern-map.md`
6. For each row:
   - Use the `before` code block to locate affected code
   - Transform to match the `after` pattern
   - Pay attention to `category`:
     - `structural` — code reorganization required (most rows in this migration)
     - `behavioral` — runtime behavior differs; check `notes`
     - `removal` — delete the code or replace with alternative from `notes`
     - `addition` — add new code/config as shown in `after`

### Key transformations

7. **Entry point:** Remove `@SpringBootApplication` class and `SpringApplication.run()`. Either delete the main class (Quarkus provides its own) or convert to `@QuarkusMain`.

8. **REST controllers (pure Quarkus path):**
   - `@RestController` → `@Path` on class
   - `@GetMapping("/path")` → `@GET @Path("/path")`
   - `@PathVariable` → `@PathParam("name")`
   - `@RequestParam` → `@QueryParam("name")`
   - `ResponseEntity<T>` → `Response`
   - `@ExceptionHandler` → `ExceptionMapper<T>` implementation

9. **Dependency injection (pure Quarkus path):**
   - `@Autowired` → `@Inject`
   - `@Component`/`@Service`/`@Repository` → `@ApplicationScoped`
   - `@Configuration` + `@Bean` → `@ApplicationScoped` + `@Produces`
   - `@Value("${prop}")` → `@ConfigProperty(name = "prop")`

10. Run the build gate

## Build gate

Run `mvn compile`. If it fails, check for:
- Missing imports after annotation replacements
- Incompatible return types (ResponseEntity → Response)
- CDI scope issues (Quarkus requires explicit scoping)
