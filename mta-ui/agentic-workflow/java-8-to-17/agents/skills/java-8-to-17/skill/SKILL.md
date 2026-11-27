---
name: java-8-to-17
description: Migrates Java applications from JDK 8 to JDK 17. Use when
  upgrading a Java project's compiler level, runtime, or both from Java 8
  to Java 17.
license: Apache-2.0
metadata:
  source: jdk-8
  target: jdk-17
  language: java
  build_tool: "maven: mvn compile"
  guide_url:
    - https://docs.oracle.com/en/java/javase/17/migrate/index.html
    - https://openjdk.org/jeps/320
    - https://openjdk.org/jeps/403
  generated_by: migration-skills-generator
  generated_at: 2026-11-27T00:00:00Z
---

# JDK 8 to 17 Migration

**Prerequisite:** Migrate to the latest JDK 8 patch release before starting; fix all deprecation warnings the build reports under JDK 8 — several of them are removals in later JDKs.

**Prerequisite:** Confirm no hard dependency on Java EE modules bundled in JDK 8 (`javax.xml.bind`, `javax.annotation`, `javax.transaction`, `java.corba`, `java.xml.ws`). They were removed from the JDK in JDK 11 (JEP 320) — applications must either add them as Maven dependencies or migrate to Jakarta equivalents.

JDK 8 to 17 spans nine releases, so the changes arrive in three distinct categories: **removed JDK-internal and Java EE modules** (JEP 320 in JDK 11, Nashorn in JDK 15, CORBA in JDK 11), **strong encapsulation of JDK internals** (reflection and `setAccessible` into `java.*` fails with `InaccessibleObjectException` by default since JDK 16/17 — JEP 396/403 — requiring `--add-opens` or code fixes), and **removed or re-routed JVM behavior** (CMS collector removed in JDK 14, biased locking disabled in JDK 15, SecurityManager effectively disabled in JDK 17). A fourth, equally common category is **bytecode-ecosystem breakage**: build tools and libraries that parse or generate class files (ASM, ByteBuddy, Mockito, JaCoCo, Lombok, Groovy, old Surefire forks) throw `UnsupportedClassVersionError` / "Unsupported class file major version 61" on JDK 17 output and must be version-bumped. This skill covers 60+ migration items across build system (12), code (24), dependency (14), runtime (10), and testing (8) changes.

## Phases

Execute in order. After each phase, run the project build and stop if it fails.

1. **Build system** — Switch the Maven compiler from `source`/`target` `1.8` to `<release>17</release>`, upgrade `maven-compiler-plugin`, pin `maven.compiler.release` consistently across modules, upgrade build plugins that parse or generate bytecode (Surefire, JaCoCo, error-prone, checkstyle). Module: `modules/build-system.md`

2. **Code** — Replace removed APIs (`sun.misc.BASE64Encoder`, `javax.xml.bind`, Nashorn `ScriptEngineManager` usage, CORBA), fix reflection into JDK internals that now throws `InaccessibleObjectException`, replace `SecurityManager`/`System.setSecurityManager`, replace deprecated-for-removal constructs. Module: `modules/code.md`, Reference: `references/api-map.md`

3. **Dependencies** — Add the Java EE modules JDK 8 bundled as explicit dependencies (JAXB API + runtime, `jakarta.annotation-api`), upgrade bytecode-sensitive libraries to JDK 17-compatible minimums (ASM, ByteBuddy, Mockito, JaCoCo, Lombok, Groovy), upgrade frameworks to versions supporting JDK 17 (Spring 5.3+, Spring Boot 2.5.5+, Hibernate 5.5+). Module: `modules/dependencies.md`, Reference: `references/dependency-map.md`

4. **Runtime** — Replace removed JVM flags (CMS collector, PermGen tuning, biased locking), re-add `--add-opens`/`--add-exports` only where the code fixes from phase 2 cannot cover, update launch scripts, Dockerfiles, and `JAVA_OPTS`, verify default GC (G1) and default behavior changes. Module: `modules/runtime.md`, Reference: `references/flag-map.md`

5. **Testing** — Upgrade test-side bytecode consumers (Surefire/Failsafe forks, JaCoCo agent, PowerMock), replace reflection-heavy tests that `setAccessible` into JDK classes, re-baseline flaky tests caused by GC/timing default changes. Module: `modules/testing.md`

6. **Cleanup** — Remove every `--add-opens` the code fixes made unnecessary, verify `<release>` consistency, confirm no `sun.*`/`com.sun.*` imports remain, re-enable deprecation warnings as errors where feasible. Module: `modules/cleanup.md`

## How to use

Load each phase's module when starting that phase. Each module references mapping tables in `references/` — apply every row in the relevant table to the codebase. Use the before/after examples as guides for each transformation.

## Build gate

After completing each phase:
1. Run the build: `mvn compile` (or `mvn test -DskipTests=false` for phases 5+)
2. Confirm the build runs on JDK 17 (`mvn -version` must report `Java version: 17`)
3. If it fails, fix the issue before proceeding
4. If you cannot fix it, stop and report to the user
