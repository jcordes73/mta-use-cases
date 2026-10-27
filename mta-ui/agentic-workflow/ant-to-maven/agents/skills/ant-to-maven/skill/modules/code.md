# Phase: Code Migration

Move sources into the standard layout, map build steps to Maven plugins, and migrate custom Ant tasks.

## Steps

### Part 1: Layout and structure

1. **Move sources to the standard layout** (guide Chapter 3, preferred path): `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`
   - Use `git mv` in separate, reviewable commits; untangle interleaved main/test sources into separate trees
   - If a physical move is not feasible, use `<sourceDirectory>`/`<testSourceDirectory>`/`<resources>` overrides as a bridge and schedule the move as a dated follow-up — see `references/config-map.md`
2. **Map build steps to plugins**: read `references/pattern-map.md` and apply every row. The core mapping: `javac` → maven-compiler-plugin, `jar` → maven-jar-plugin, `copy`+`filterset` → maven-resources-plugin, `junit` → maven-surefire-plugin, `javadoc` → maven-javadoc-plugin, `zip`/`tar` → maven-assembly-plugin, fat-jar merges → maven-shade-plugin (with `ServicesResourceTransformer`), `delete`/`mkdir` → deleted, not converted
   - Keep compiler settings in properties; plugin configuration only for includes/excludes and processors
   - Filtered resources: write tokens as `@token@`, protect runtime `${...}` placeholders with `@`-only delimiters, keep binaries out of filtered directories
   - Generated sources: prefer dedicated generator plugins (`jaxb2-maven-plugin`, `antlr4-maven-plugin`); register in-house generator output via `build-helper-maven-plugin` `add-source` under `target/generated-sources/`
3. **Migrate custom Ant tasks** (guide Chapter 9): follow the strategy ladder in order — (1) find a plugin equivalent first, (2) wrap with `maven-antrun-plugin` (task jar in the plugin's `<dependencies>` block), (3) rewrite as a mojo or `exec-maven-plugin`
   - Plain `<exec>`/`<java fork="true">` invocations → `exec-maven-plugin` `exec`/`java` goals (note: `exec:java` does NOT fork — use the `exec` goal if isolation is required)
   - Wrapping is acceptable long-term only for lifecycle-external side effects; compile-classpath, test-execution, and artifact-assembly steps must become native Maven before cutover
4. Run the build gate

### Key areas to search

- `<target name=` — every Ant target in the inventory, mapped one row at a time
- `<taskdef` / `<typedef` — custom tasks
- `<exec executable=` / `<java classname=` — raw process invocations
- `<zip destfile=` / `<unzip dest=` — distribution archives and fat-jar merges
- `<filterset>` — token substitution in copies

## Build gate

Run `mvn clean package`. If it fails, check for:
- Missing generated-source registrations (generator ran after compile)
- Corrupted binary resources (filtering on binaries)
- `taskdef class ... cannot be found` — task jar missing from antrun's `<dependencies>`
- Artifacts the lifecycle plugins did not produce (wrapped assembly steps)
