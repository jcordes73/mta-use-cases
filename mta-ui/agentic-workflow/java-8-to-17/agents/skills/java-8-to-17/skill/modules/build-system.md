# Phase: Build System Migration

Move the build to JDK 17 before touching application code — the compiler's diagnostics under `--release 17` drive every later phase.

## Steps

1. Read `references/flag-map.md` (for `JAVA_TOOL_OPTIONS`/compiler flags the build may inject)
2. Confirm the build runs on a JDK 17 toolchain (`mvn -version` → `Java version: 17`); if the environment only has JDK 8, install/point `JAVA_HOME` at a JDK 17 distribution first
3. Open the project's `pom.xml` (and every parent/module POM)

### Compiler level: source/target → release

- Replace every `maven.compiler.source` / `maven.compiler.target` property pair with a single `maven.compiler.release`:

```xml
<!-- before -->
<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>

<!-- after -->
<properties>
    <maven.compiler.release>17</maven.compiler.release>
</properties>
```

- If the `maven-compiler-plugin` is configured directly, replace `<source>`/`<target>` with `<release>`:

```xml
<!-- before -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
    </configuration>
</plugin>

<!-- after -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <release>17</release>
    </configuration>
</plugin>
```

- `<release>` is strictly better than `source`/`target` on JDK 9+: it also checks the **API** against the requested platform (an SDK-17 build claiming `target 8` silently compiles against JDK 17 APIs without it — a classic source of `NoSuchMethodError` on real JDK 8 runtimes, and with `release 17` an in-between pitfall is avoided entirely)

### Plugin upgrades

Upgrade plugins that parse, generate, or instrument bytecode — their JDK-8-era versions fail on class file major version 61:

- `maven-compiler-plugin` → `3.11.0` or later (required for a reliable `<release>`)
- `maven-surefire-plugin` / `maven-failsafe-plugin` → `2.22.0` minimum, `3.2.5`+ recommended (older forks and bootclasspath tricks break; see `modules/testing.md`)
- `maven-shade-plugin` → `3.4.1`+ (older versions re-write module-info/class files badly)
- `jacoco-maven-plugin` → `0.8.8`+ (JDK 17 class file support; older agents crash the JVM with `Unsupported class file major version 61`)
- `exec-maven-plugin`, `maven-antrun-plugin`: check any embedded `<javac source=… target=…>` or `<java fork=… jvm=…>` arguments for JDK-8-era flags (list in `references/flag-map.md`)

### Build hygiene

- Remove any `<bootclasspath>`, `-bootclasspath`, `-Xbootclasspath` compiler arguments — meaningless since the module system and rejected by `--release`
- Remove `-source 1.8 -target 1.8` from `maven.compiler.compilerArguments` / `compilerArgs`
- Remove `-XX` flags set through `MAVEN_OPTS` that no longer exist (see `references/flag-map.md`) — unknown `-XX` flags crash the JDK 17 JVM at startup
- Multi-module builds: make `maven.compiler.release` a property in the **parent** POM only; child modules must not override it with divergent values
- If the build pins toolchains (`toolchains.xml`), add/switch to a JDK 17 toolchain entry

### Verify

- `mvn -version` reports Java 17
- `mvn compile` succeeds
- `javap -verbose target/classes/<AnyClass>.class | grep major` reports `major version: 61`
