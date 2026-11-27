# Phase: Code Migration

Replace APIs removed or encapsulated between JDK 8 and JDK 17. Apply every row of `references/api-map.md` whose `old_api` appears in the codebase.

## Steps

1. Read `references/api-map.md`
2. Search the source tree for each `old_api` and apply the mapped replacement
3. Pay special attention to the three failure classes below — they only surface **at runtime**, not compile time

### 1. Compile-time removals (JEP 320, Nashorn, CORBA)

Java EE modules shipped inside JDK 8 were removed in JDK 11; these fail compilation immediately:

- `javax.xml.bind.*` (JAXB), `javax.activation.*`, `javax.annotation.*` EE annotations (`@PostConstruct`, `@PreDestroy`, `@Resource` — **not** `javax.annotation.processing`), `javax.transaction.*` (JTA — **not** `javax.transaction.xa`), `javax.xml.ws.*` (JAX-WS), `javax.xml.soap.*`
  - Preferred fix: migrate imports to `jakarta.*` and add the Jakarta API + runtime dependencies (see `modules/dependencies.md`)
  - Acceptable fix (maximally compatible with existing code): add the still-published `javax` artifacts (`javax.xml.bind:jaxb-api:2.3.1` + `org.glassfish.jaxb:jaxb-runtime:2.3.x`, `javax.annotation:javax.annotation-api:1.3.2`) — imports stay unchanged
- `org.omg.CORBA.*`, `javax.rmi.CORBA.*` — CORBA removed in JDK 11, no bundled replacement; migrate to a modern RPC mechanism or a CORBA library if genuinely required
- Nashorn JavaScript engine removed in JDK 15 — `new ScriptEngineManager().getEngineByName("nashorn")` returns `null`:
  - Replace with the standalone Nashorn artifact (`org.openjdk.nashorn:nashorn-core`) or GraalJS (`org.graalvm.js:js` + `js-scriptengine`)
- `sun.misc.BASE64Encoder` / `sun.misc.BASE64Decoder` — replaced by `java.util.Base64` (available since JDK 8):

```java
// before
String encoded = new sun.misc.BASE64Encoder().encode(bytes);
byte[] decoded = new sun.misc.BASE64Decoder().decodeBuffer(text);

// after
String encoded = Base64.getEncoder().encodeToString(bytes);
byte[] decoded = Base64.getDecoder().decode(text);
```

### 2. Runtime-only failures: strong encapsulation (JEP 396/403)

Since JDK 16, reflective access to non-exported JDK internals throws `InaccessibleObjectException` — code that compiled and ran fine on JDK 8 fails at runtime:

- `field.setAccessible(true)` on a field of a JDK class (`java.lang`, `java.util`, `sun.security.*`, …)
- Reflection into `sun.misc.Unsafe`, `sun.nio.ch.DirectBuffer.cleaner()`, JDK collection internals
- Libraries doing the above internally (old Jackson/GSON versions, caches, serialization frameworks)

Fix in order of preference:
1. Replace the reflection with a supported API (e.g. `java.lang.invoke.VarHandle`, `MethodHandles.privateLookupIn` with an opens, public alternatives)
2. If the code is in the project: restructure to avoid the internal API
3. Last resort: `--add-opens <module>/<package>=ALL-UNNAMED` at launch (see `modules/runtime.md`) — record every add-opens added so cleanup (phase 6) can revisit

### 3. Removed / disabled platform behaviors

- `System.setSecurityManager(...)` throws `UnsupportedOperationException` on JDK 17 unless launched with `-Djava.security.manager=allow`; SecurityManager is deprecated for removal (JEP 411) — remove SecurityManager-based code paths
- `Thread.stop()`, `Thread.suspend()`, `Thread.resume()` — removed/terminal; replace with interruption/collaboration patterns (note: `Thread.stop` still exists but deprecated in 17, removed in 20 — fix now)
- `Runtime.runFinalization` / reliance on `Object.finalize()` — finalize deprecated for removal; replace with `Cleaner`/`AutoCloseable` where finalizers do resource release
- `new Integer(...)`, `new Long(...)` etc. — deprecated for removal since 9; use `Integer.valueOf(...)`
- `Applet`/`java.applet` — deprecated for removal (JEP 398); migrate to a standalone UI technology
- `Class.newInstance()` — deprecated since 9 (propagates checked exceptions badly); use `getDeclaredConstructor().newInstance()`

### Verify

- `grep -rn "sun.misc\|sun.reflect\|com.sun.image\|javax.xml.bind\|javax.activation" src/` returns no hits (or only dependency-provided ones)
- `grep -rn "setAccessible(true)" src/` — every remaining call targets application classes, not JDK classes
- `mvn compile` succeeds
