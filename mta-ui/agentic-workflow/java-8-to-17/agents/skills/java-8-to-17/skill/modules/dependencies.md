# Phase: Dependency Migration

Add the modules JDK 8 bundled that JDK 17 no longer ships, and upgrade every bytecode-sensitive or framework library to its JDK 17-compatible minimum. Apply the version floors from `references/dependency-map.md`.

## Steps

1. Read `references/dependency-map.md`
2. Add the missing Java EE module dependencies (they were removed from the JDK, not deprecated)
3. Bump every dependency below its JDK 17 minimum version
4. Check frameworks against their support matrix

### Add Java EE modules as dependencies (JEP 320 removals)

JDK 8 bundled these; JDK 17 does not — the code compiles only once they are added:

```xml
<!-- JAXB -->
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>2.3.3</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>2.3.9</version>
    <scope>runtime</scope>
</dependency>

<!-- EE annotations (@PostConstruct, @Resource, @Generated) -->
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>1.3.5</version>
</dependency>
```

- If the code keeps `javax.*` imports (see `modules/code.md`), use the `javax` coordinates instead (`javax.xml.bind:jaxb-api:2.3.1`, `javax.annotation:javax.annotation-api:1.3.2`) — do **not** mix `javax` and `jakarta` versions of the same API on one classpath
- JAX-WS (`javax.xml.ws.*`): `jakarta.xml.ws:jakarta.xml.ws-api:2.3.3` + `com.sun.xml.ws:jaxws-rt:2.3.x`
- JTA: `jakarta.transaction:jakarta.transaction-api:1.3.3` (or let the transaction manager supply it)

### Bytecode-sensitive libraries (the "major version 61" family)

These fail at **runtime** with `UnsupportedClassVersionError` or "Unsupported class file major version 61" when they encounter JDK 17-compiled classes. Enforce the minimums from `references/dependency-map.md`:

- ASM (often transitive) → `9.1`+ (9.3+ recommended)
- ByteBuddy → `1.12.7`+ (or `-experimental` variant; exact release or the agent throws at instrumentation)
- Mockito → `4.1`+ (3.x can instrument but breaks on JDK 17 internals)
- JaCoCo → `0.8.8`+
- Lombok → `1.18.24`+ (annotation processing crashes on older versions)
- Groovy → `3.0.9`+ (Groovy 2.x does not run on JDK 17)
- Check transitive pulls: `mvn dependency:tree -Dincludes=net.bytebuddy,org.ow2.asm` — an old transitive ByteBuddy silently overrides the managed version unless `dependencyManagement` pins it

### Framework floors

- Spring Framework → `5.3.x` (JDK 17 supported from 5.3); Spring 4/5.0-5.2 are unsupported on 17
- Spring Boot → `2.5.5`+ (2.7 recommended; Boot 3 requires the Jakarta migration, do not mix into this upgrade)
- Hibernate ORM → `5.5`+ (5.6 recommended for JDK 17)
- Jackson → `2.13`+ (older versions reflect into JDK internals and trip JEP 403)
- Apache Commons / logging: commons-lang3 `3.12`, log4j2 `2.17.2`+ (also for Log4Shell)
- Anything bundling its own JVM arguments (`JAVA_OPTS`-injected agents: APM agents, profilers) — verify each agent advertises JDK 17 support before launch

### Verify

- `mvn dependency:tree -Dincludes=org.ow2.asm,net.bytebuddy,org.jacoco,org.projectlombok` shows only versions ≥ the floors in `references/dependency-map.md`
- `mvn compile` succeeds
- `mvn dependency:analyze` does not flag the newly added API/runtime pairs as unused (API at compile scope, runtime at runtime scope)
