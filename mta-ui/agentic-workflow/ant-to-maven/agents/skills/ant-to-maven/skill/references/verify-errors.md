# Verify Errors — Apache Ant to Apache Maven

## Scaffolding Phase

| Error | Cause | Fix |
|---|---|---|
| `package X does not exist` compile errors on Java-8-era APIs after `mvn clean package` | Baseline POM compiles at a newer Java level than the Ant build did | Fix compile errors now while the build is still trivial — this is the intended surface point; do not defer |
| `invalid target release` / class file version mismatch | `maven.compiler.target` set higher than the JVM running Maven | Run the build on a JVM >= the target level, or lower the target to the agreed version |

## Dependencies Phase

| Error | Cause | Fix |
|---|---|---|
| `package org.foo does not exist` after declaring a candidate dependency | Wrong coordinates for a mystery jar (bad identification) | Re-identify via `unzip -p lib/foo.jar 'META-INF/maven/*/pom.properties'`, `MANIFEST.MF`, SHA-1 search on Nexus/Artifactory, or `jdeps`; declare the corrected coordinates and re-run `mvn clean compile` |
| `omitted for conflict` in `mvn dependency:tree -Dverbose` | Nearest-wins resolution silently dropped a version Ant tolerated side-by-side in `lib/` | Check each omitted line; pin the version you want with `<dependencyManagement>` (for versions you control) or remove it with `<exclusions>` on the dependency that pulls it in; re-run the verbose tree to confirm |
| Compiler rejects imports of a jar marked `<scope>test</scope>` | A jar the main sources import was scoped test-only (accidental test-to-main dependency surfaced) | Re-scope to `compile` if main code uses it; this breakage is healthy and expected |
| `ClassNotFoundException` at runtime for a class that compiled fine | Dependency marked `provided` (servlet API, Lombok, annotation processor) actually needed at runtime | Re-scope honestly: `compile` for main-code usage, `provided` only for what the container/JVM supplies |
| Missing system-scope jar on another machine | `<systemPath>` referenced a local absolute path that defeats reproducibility | Replace with a real repository artifact (file-based `local-repo/` stepping stone or repository manager) |

## Plugin Mapping Phase

| Error | Cause | Fix |
|---|---|---|
| Corrupted binary resource after enabling filtering | Filtering enabled on a directory holding binary files (images, keystores) | Split binaries into a second `<resource>` entry with `<filtering>false</filtering>` |
| `${some.key}` placeholders in resources substituted away by Maven | Maven filtering substitutes both `${token}` and `@token@` by default, destroying framework placeholders (e.g. Spring `@Value("${some.key}")`) | Restrict delimiters: `<useDefaultDelimiters>false</useDefaultDelimiters><delimiters><delimiter>@</delimiter></delimiters>` on maven-resources-plugin; write tokens as `@token@` |
| Overlapping `META-INF/services/` files silently dropped from a shaded jar | Ant-style last-unpacked-wins merge behavior | Use `maven-shade-plugin` with `ServicesResourceTransformer`, which concatenates service-loader files |
| Generated-source step missing from the compile | Generator ran after compilation, or its output dir was never registered | Run the generator before `compile` (bind to `generate-sources`/`generate-resources`); register output via `build-helper-maven-plugin` `add-source` under `target/generated-sources/`, or use a dedicated generator plugin that registers automatically |

## Testing Phase

| Error | Cause | Fix |
|---|---|---|
| A test class silently not run by Surefire | Test name does not match the conventions (`*Test`, `Test*`, `*Tests`, `*TestCase`) — e.g. `LegacyChecks`, `AllTheTestsRunner` | Rename the class to match the conventions, or add explicit `<includes>` configuration and delete it as the class is renamed; verify with test-count parity against the Ant JUnit run |
| JUnit 4 tests not executed by `mvn test` | JUnit Platform does not run JUnit 4 tests by default | Add `junit-vintage-engine` as a test-scoped dependency alongside `junit-jupiter`; plan to remove vintage once the last JUnit 4 test is rewritten |
| Surefire fails with "no tests" or version errors | Parent POM pins Surefire < 2.22.0 (JUnit 5 needs >= 2.22.0) | Remove the old Surefire pin or bump it to a current release |

## WAR Packaging Phase

| Error | Cause | Fix |
|---|---|---|
| `ClassCastException` on `HttpServletRequest` / `LinkageError` at runtime | Container-supplied servlet API jar bundled into `WEB-INF/lib` (Ant bundled it because nothing kept it out of the archive) | Declare the servlet/JSP/CDI APIs `<scope>provided</scope>`; provided-scoped dependencies never enter the WAR |
| `ClassCastException`-adjacent linkage errors across container versions | Bundled copy of a container class found first by the classloader | Same fix: remove the JEE jar from the archive via `provided` scope |
| Namespace incompatibility errors after swapping to the servlet API | Mismatched `javax.*` vs `jakarta.*` namespace for the target container | Match the namespace the container implements — `javax.servlet:javax.servlet-api:4.0.1` for Java EE 8 servers, `jakarta.servlet:jakarta.servlet-api` for Jakarta EE; migrating javax→jakarta code is an import rewrite, not a dependency swap |

## Multi-Module Phase

| Error | Cause | Fix |
|---|---|---|
| Build of a single module fails to resolve a sibling artifact | Running `mvn` from inside one module directory — the reactor has no sibling to resolve, falls back to the local repository | Build from the aggregator root, or use `mvn -pl <module> -am package` to include the dependencies; or install the sibling first |
| `acme-web/build.xml` silently matches nothing / module missing | Ant `<subant>` fileset fragility (in the pre-migration state) — no check that the file exists | In Maven this class of error disappears; ensure the aggregator's `<modules>` lists every directory with a `pom.xml` |

## CI/CD Phase

| Error | Cause | Fix |
|---|---|---|
| Build behaves differently between developer and CI machines with no POM change | Plugin versions not declared; Maven resolves them from installed Maven's lifecycle bindings | Declare an explicit `<version>` for every plugin; enforce with `maven-enforcer-plugin` `requirePluginVersions`; pick current stable releases, bump deliberately |
| 401 on `mvn deploy` | `settings.xml` server `<id>` does not match `<distributionManagement>` repository `<id>` | Make the `<id>` values match exactly — the `<id>` is the join key |
| First build after a CI cache miss is very slow | Maven resolves the full dependency tree into `~/.m2/repository` on a fresh machine | Cache `~/.m2/repository` in CI (not all of `~/.m2`); treat the cache as disposable — builds must succeed on a cold cache |

## General

| Error | Cause | Fix |
|---|---|---|
| `Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!` | `project.build.sourceEncoding` not set; Maven falls back to the platform default charset | Set `project.build.sourceEncoding` (and `project.reporting.outputEncoding`) in the POM — parent POM for multi-module; match the encoding the Ant `javac` used |
| `taskdef class com.acme.build.VersionStampTask cannot be found` | Custom task's jar not on antrun's classpath — the part most teams miss | Declare the task jar in the `maven-antrun-plugin`'s own `<dependencies>` block |
| A resource or descriptor in the Ant-built artifact is missing from the Maven-built one | File not under `src/main/resources` (legacy layout), or produced by an Ant step (filter/replace/codegen) with no Maven equivalent configured | Move files into the resources directories; add the plugin configuration (resources filtering, build-helper source roots) or the Antrun/exec escape hatch — do not copy files into `target/` by hand |
| Builds that took seconds under Ant now take minutes | Maven runs the full lifecycle, resolves all dependencies, builds every module | Partial builds (`mvn -pl acme-web -am package`), `-o` offline mode, `-DskipTests`, `-T 1C` parallel reactor, repository manager in front of remote deps, cache `~/.m2/repository` in CI; profile with `mvn -X` if still slow |
| IDE (m2e/IntelliJ) shows red everywhere, sources not marked as source roots, or lifecycle-mapping errors | Legacy layout overrides or unusual plugin executions confuse the importer | Adopt the standard layout (preferred path); bind unusual goals to recognized phases (`generate-sources`, `process-classes`) or add m2e lifecycle-mapping metadata; fix the POM, not the IDE settings |
| `requirePluginVersions` enforcer failure naming a plugin (including the enforcer itself) | Undeclared plugin version flagged by the rule | Declare a `<version>` for each plugin; manage the enforcer's own version in `<pluginManagement>` rather than a pin in the POM |
