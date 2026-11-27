# Phase: Runtime Migration

Update JVM launch configuration: flags removed since JDK 8, the flags strong encapsulation forces on you, and the default-behavior changes to expect. Apply `references/flag-map.md` to every launch surface.

## Steps

1. Read `references/flag-map.md`
2. Inventory every launch surface: shell scripts, `JAVA_OPTS`/`JAVA_TOOL_OPTIONS`, Dockerfiles, systemd units, Kubernetes/OpenShift manifests, pom-exec configurations
3. Remove/replace flags per the map, then re-add only the `--add-opens` the code genuinely needs

### Remove or replace dead flags

Unknown `-XX` flags **crash the JVM at startup** ("Unrecognized VM option") — the most common first-launch failure of a JDK 8 → 17 migration:

- CMS flags: `-XX:+UseConcMarkSweepGC`, `-XX:CMSInitiatingOccupancyFraction`, `-XX:+UseCMSInitiatingOccupancyOnly`, `-XX:+UseParNewGC` — CMS was removed in JDK 14 (JEP 366). Replace with G1 (the JDK 17 default — usually deleting the flags is the correct move)
- PermGen flags: `-XX:PermSize`, `-XX:MaxPermSize`, `-XX:+UseSplitVerifier` — PermGen ceased to exist in JDK 8; they were warnings on 8 and are startup errors by 17 (or silent no-ops — remove them regardless)
- Biased locking: `-XX:+UseBiasedLocking` — disabled by default in JDK 15 (JEP 374) and removed later; delete
- `-XX:+UseCompressedOops`, `-XX:+UseCompressedClassPointers` — on by default; explicit settings only constrain
- `-XX:MaxMetaspaceSize` stays valid — re-tune only if metaspace warnings appear
- `-XX:+PrintGCDetails`, `-XX:+PrintGCDateStamps`, `-Xloggc:…` — unified logging since JDK 9; convert to `-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m`
- `-XX:+UseStringDeduplication` is G1-only — with CMS gone it now applies as before if G1 is in use

### Strong encapsulation: `--add-opens` / `--add-exports`

For every `InaccessibleObjectException` (or `IllegalAccessError` for `sun.*` classes) that survives the phase-2 code fixes, add the narrowest possible flag:

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-exports java.base/sun.security.action=ALL-UNNAMED
```

- `--add-opens` for reflection (`setAccessible`); `--add-exports` for plain compile-time-style access to non-exported packages
- Scope `ALL-UNNAMED` is the norm for classpath applications; named-module targets only for module-system apps
- Keep a running list of every flag added, each with the library or class that required it (phase 6 will try to eliminate them)
- These flags must reach **every** JVM in the deployment: the application, surefire forks, and any exec'd helper JVM

### Launch surface updates

- Dockerfiles: bump the base image to a JDK 17 distribution (e.g. `registry.access.redhat.com/ubi8/openjdk-17` / `eclipse-temurin:17`); remove JDK-8-era `JAVA_OPTS`
- `JAVA_TOOL_OPTIONS` is inherited by every JVM including build forks — flags that are fine for the app JVM can crash test JVMs; prefer explicit per-surface options
- GC: expect G1 defaults to change pause/throughput profiles vs Parallel/CMS on 8 — note for the verify stage's timing-sensitive checks

### Verify

- Start the application with the new flags: no "Unrecognized VM option", no `InaccessibleObjectException` during startup smoke path
- `-Xlog:gc*` (if enabled) reports G1
