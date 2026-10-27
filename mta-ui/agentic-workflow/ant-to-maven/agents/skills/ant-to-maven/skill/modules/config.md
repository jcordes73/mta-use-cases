# Phase: Configuration Migration

Convert Ant properties, property files, and per-environment settings to POM properties and profiles.

## Steps

1. Read `references/config-map.md`
2. Identify the configuration surfaces: `build.xml` properties and property files (`build.properties`, per-environment `dev.properties`/`prod.properties`), plus the POM(s)
3. For each row in the config map:
   - Hand-defined `version`, `build.dir`-style properties → built-ins: `${project.version}`, `${project.artifactId}`, `${project.groupId}`, `${project.build.directory}`, `${project.build.outputDirectory}`
   - `<property file="build.properties"/>` values → POM `<properties>`; machine-local credentials/mirrors → `settings.xml`
   - `<property file="${env}.properties"/>` with `ant -Denv=prod` → profiles with per-profile `<properties>`, everyday environment as `activeByDefault`; verify with `mvn help:active-profiles`
   - Per-environment `<filterset>` values → profile-selected `<filter>` files: `<build><filters><filter>src/main/filters/${env}.properties</filter></filters></build>` with `<filtering>true</filtering>`
   - Do NOT bake deploy-specific URLs, credentials, or endpoints into artifacts via profiles — build once, promote the same binary; runtime config is externalized
4. Run the build gate

## Build gate

Run the project build command. Configuration errors often surface as build-behavior changes rather than compilation errors:

- `Using platform encoding ... build is platform dependent!` — set `project.build.sourceEncoding` (and `project.reporting.outputEncoding`) in the parent POM
- `mvn help:active-profiles` shows the wrong profile active — check `activeByDefault` semantics (any other activated profile deactivates it)
