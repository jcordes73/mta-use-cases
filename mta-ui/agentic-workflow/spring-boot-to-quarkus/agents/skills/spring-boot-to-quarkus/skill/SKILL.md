---
name: spring-boot-to-quarkus
description: Migrates Spring Boot 3.x applications to Quarkus 3.x. Use when
  converting a Spring Boot project to run on the Quarkus framework.
license: Apache-2.0
metadata:
  source: spring-boot-3
  target: quarkus-3
  language: java
  build_tool: "maven: mvn compile"
  guide_url: https://github.com/snowdrop/springboot-to-quarkus-migration-guide
  generated_by: migration-skills-generator
  generated_at: 2026-06-11T00:00:00Z
---

# Spring Boot 3.x to Quarkus 3.x Migration

Migrates a Spring Boot 3.x application to Quarkus 3.x. This is a cross-framework migration — not a version upgrade — that replaces Spring Boot starters with Quarkus extensions, Spring MVC with JAX-RS (or Spring compatibility extensions), Spring DI with CDI, and Spring Boot configuration with MicroProfile/SmallRye Config. Two migration paths are supported: a pure Quarkus path (higher effort, maximum performance) or a Quarkus Spring compatibility path (moderate effort, uses bridge extensions). The skill covers build system restructuring (BOM, plugins, dependencies), annotation and API replacements, configuration property migration, Thymeleaf-to-Qute template conversion, testing framework changes, and health/metrics endpoint migration.

## Phases

Execute in order. After each phase, run the project build and stop if it fails.

1. **Build system** — `modules/build-system.md` — Replace Spring Boot parent POM, starters, plugins, and database drivers with Quarkus equivalents
2. **Code** — `modules/code.md` — Replace Spring annotations, APIs, and code patterns with CDI/JAX-RS/MicroProfile equivalents (or add Spring compatibility extensions)
3. **Config** — `modules/config.md` — Migrate application.properties/yml from Spring Boot property names to Quarkus property names
4. **Testing** — `modules/testing.md` — Replace @SpringBootTest, @MockBean, MockMvc with @QuarkusTest, @InjectMock, REST Assured
5. **Additional** — `modules/additional.md` — Handle template engine migration (Thymeleaf → Qute), health/metrics, scheduling, caching, events
6. **Cleanup** — `modules/cleanup.md` — Remove remaining Spring imports, verify no old artifacts remain, final build

## How to use

Load each phase's module when starting that phase. Each module references mapping tables in `references/` — apply every row in the relevant table to the codebase. Use the before/after examples as guides for each transformation.

## Build gate

After completing each phase:
1. Detect the project's build tool (check metadata `build_tool` field above, or detect from project files: `pom.xml` → `mvn compile`, `build.gradle` → `gradle build`, `go.mod` → `go build ./...`, `package.json` → `npm run build`, `pyproject.toml` → `python -m build`, `Makefile` → `make`)
2. Run the build
3. If it fails, fix the issue before proceeding
4. If you cannot fix it, stop and report to the user
