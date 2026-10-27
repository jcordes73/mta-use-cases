---
name: ant-to-maven
description: Migrates Apache Ant builds to Apache Maven. Use when converting a
  Java project from build.xml to pom.xml, replacing Ant targets/paths/custom
  tasks with the Maven lifecycle, plugins, and dependency management.
license: Apache-2.0
metadata:
  source: ant
  target: maven
  language: java
  build_tool: "maven: mvn compile"
  guide_url: local file README.md (repo: ant-to-maven)
  generated_by: migration-skills-generator
  generated_at: 2026-10-22T19:24:43+02:00
---

# Apache Ant to Apache Maven Migration

**Prerequisite:** Before touching any Maven configuration, complete an assessment inventory of the Ant build (guide Chapter 2) and keep it in a `MIGRATION-NOTES.md` next to the project. Record: every `<target>` and its `depends` chain; every `<path>`/`<classpath>` and the jars each pulls in; every `<taskdef>`/`<typedef>` and where its classes come from; compiler flags (`source`, `target`, `debug`, `encoding`, excludes) on every `javac` invocation; the JVM version used to run the build; the packaging artifacts produced and their names; signing/manifest steps; the test runner and test-specific classpath; and environment-specific property files. Also classify the build against three blockers (nonstandard source layout, heavy code generation, procedural steps with no plugin equivalent). If procedural steps dominate the build rather than decorating it, discuss "when not to migrate" with the user before investing further.

**Prerequisite:** During the migration, keep the Ant build working and authoritative while the Maven build grows next to it (parallel running). Flip to Maven-only only after the parity checklist passes (see Cleanup phase).

## Phases

Execute in order. After each phase, run the project build and stop if it fails.

1. **Build system** — Scaffold `pom.xml`, replace the `lib/` classpath with `<dependencies>`, and delete `build.xml` targets that the lifecycle makes redundant.
2. **Code** — Move sources into the standard layout, map build steps to plugins, and migrate custom Ant tasks (Antrun/exec escape hatch).
3. **Config** — Convert Ant properties, property files, and per-environment settings to POM properties and profiles.
4. **Testing** — Migrate `<junit>` targets to Surefire, coverage to JaCoCo, integration tests to Failsafe.
5. **Additional** — Package WARs and EARs, container deployment (Cargo), Arquillian tests, multi-module builds, and CI/CD + release.
6. **Cleanup** — Cut over: verify parity, decommission the Ant build, and audit remaining Antrun executions.

## How to use

Load each phase's module when starting that phase. Each module references mapping tables in `references/` — apply every row in the relevant table to the codebase. Use the before/after examples as guides for each transformation.

## Build gate

After completing each phase:
1. Detect the project's build tool (check metadata `build_tool` field above, or detect from project files: `pom.xml` → `mvn compile`, `build.gradle` → `gradle build`, `go.mod` → `go build ./...`, `package.json` → `npm run build`, `pyproject.toml` → `python -m build`, `Makefile` → `make`)
2. Run the build
3. If it fails, consult `references/verify-errors.md` for known error-fix mappings before attempting ad-hoc fixes
4. If you cannot fix it, stop and report to the user

## Smoke gate

After the final phase completes and the build gate passes:
1. Run the smoke verification appropriate to the project type (from guide Chapter 13's verification checklist): for an application, start the Maven-built artifact and answer a trivial request; for a library, resolve it from the local repository and link a consumer against it. Additionally, compare artifact contents: `unzip -l` (or `jar tf`) the Ant-built and Maven-built artifacts — manifest and `META-INF/maven/` differences are expected; missing classes, stale resources, or extra files are migration gaps.
2. The smoke verification proves the artifact is *loadable* — classpath wiring, main class, and dependency resolution that the build gate cannot catch
3. If it fails, consult `references/verify-errors.md` for known error-fix mappings
4. Apply the same fix loop as the build gate (fix, re-run, max iterations)
5. If you cannot fix it, stop and report to the user
