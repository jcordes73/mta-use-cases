# Phase: Testing Migration

The test toolchain is the part of a JDK migration most likely to break — test JVMs fork, agents instrument bytecode, and tests deliberately poke JDK internals.

## Steps

1. Verify the Surefire/Failsafe configuration (the fork that runs tests)
2. Upgrade/replace test-side libraries per `references/dependency-map.md`
3. Fix tests that reflect into JDK classes
4. Re-baseline timing/GC-sensitive tests

### Surefire / Failsafe

- Minimum `2.22.0`, recommended `3.2.5`+: older versions pass JDK-8-era arguments (`-Xbootclasspath`, `-XX:PermSize`) to forks, which **crash the forked JVM** on 17
- `<argLine>` content: remove dead flags (see `references/flag-map.md`), add any `--add-opens` the test JVM needs here rather than globally
- `forkCount`/`reuseForks` behavior is unchanged; `-DfailIfNoTests` etc. unchanged
- If `argLine` is composed with `@{argLine}` (JaCoCo late binding), keep that structure — it still works

### Test-side libraries

- JaCoCo agent → `0.8.8`+ (crash: `Unsupported class file major version 61` at instrument time)
- PowerMock → **replace with plain Mockito 4+** (PowerMock's Javassist/`MockClassLoader` does not survive strong encapsulation; there is no JDK 17-compatible release)
- Mockito → `4.1`+; `mockito-inline` for static/final mocking (built into Mockito 5)
- JUnit 4 stays fine; JUnit 5 requires `5.8`+ on JDK 17
- Testcontainers, Awaitility, RestAssured: bump to current minors — several older versions use `sun.misc` internally

### Reflection into JDK classes in tests

- `setAccessible(true)` on JDK fields (e.g. white-box tests on `java.util.HashMap`, `String.value`) now throws `InaccessibleObjectException`
  - Preferred: re-target the test at the application's own API instead of JDK internals
  - If unavoidable: add the corresponding `--add-opens` **to the Surefire `argLine`**, not only to the production launcher — test JVMs are separate processes
- Serialization round-trip tests of JDK-internal classes (`sun.awt.*`, `java.time` internals) frequently break on changed serialVersionUIDs — re-baseline or drop those assertions

### Default-behavior re-baselining

- GC is G1 by default (JDK 9+): pause profiles change — timing-sensitive assertions (`assertTrue(elapsed < X)`) may need relaxed bounds or `-XX:+UseParallelGC` pinning if they exist to assert GC behavior
- String deduplication, `String.concat`/`+` compaction changes and compact strings (JDK 9) can shift memory numbers — re-baseline memory-assertion tests
- Floating-point `Math` functions: JDK 8→11 changed some `StrictMath` results; exact-equality assertions on transcendental values may need tolerance

### Verify

- `mvn test` succeeds with the upgraded plugins
- No test log contains `InaccessibleObjectException`, `UnsupportedClassVersionError`, or "Unrecognized VM option"
