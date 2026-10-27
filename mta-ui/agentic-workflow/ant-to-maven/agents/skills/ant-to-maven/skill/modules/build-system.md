# Phase: Build System Migration

Scaffold the `pom.xml`, replace the `lib/` classpath with Maven dependencies, and remove `build.xml` targets the lifecycle makes redundant.

## Steps

1. **Scaffold the POM** (guide Chapter 3):
   - Choose coordinates matching the existing jar names and package root: `acme-core-1.0.0.jar` from an Ant `dist` target → artifactId `acme-core`, version `1.0.0-SNAPSHOT` (or from a repo path like `com/acme/acme-core/...` → groupId `com.acme`)
   - Set `<packaging>` to what the Ant build produces (`jar` default, `war`/`ear` — see Additional phase)
   - Translate `javac source/target/encoding` into properties: `maven.compiler.source`, `maven.compiler.target`, `project.build.sourceEncoding`
2. **Replace the `lib/` classpath** (guide Chapter 4): read `references/dependency-map.md`
   - Each jar on every `<path>`/`<classpath>` becomes a `<dependency>`; jars present only for transitive needs get no entry at all
   - Assign scopes deliberately: `test` for test-only jars, `provided` for container-supplied ones, `compile` (default) for everything main code uses; never `system`
   - Compare `mvn dependency:tree` (and `-Dverbose`) against the `lib/` inventory; check each `omitted for conflict` line
   - Internal jars: file-based `local-repo/` stepping stone (`<repositories><repository><url>file://${project.basedir}/local-repo</url></repository></repositories>`), repository manager as the end state
3. **Delete redundant targets**: `clean` (directory delete) and `mkdir` targets are deleted, not converted — `target/` is managed by the lifecycle and `mvn clean`
4. Run the build gate

## Build gate

Run `mvn clean package`. If it fails, fix dependency issues before proceeding. Common issues:

- `package org.foo does not exist` — wrong or missing coordinates for a mystery jar (identify via `pom.properties`, `MANIFEST.MF`, SHA-1 search, `jdeps`)
- `omitted for conflict` — nearest-wins resolution; pin with `<dependencyManagement>` or `<exclusions>`
- Compile errors on Java-8-era APIs — expected and intended at this stage; fix them while the build is trivial
