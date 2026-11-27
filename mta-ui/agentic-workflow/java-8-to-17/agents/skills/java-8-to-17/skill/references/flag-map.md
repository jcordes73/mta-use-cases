# Flag Map — JVM flags removed or changed between JDK 8 and JDK 17

Unknown `-XX` options crash the JDK 17 JVM at startup with `Unrecognized VM option`. Apply every row to every launch surface (scripts, `JAVA_OPTS`, `JAVA_TOOL_OPTIONS`, Dockerfiles, Surefire `argLine`, Kubernetes manifests).

| removed_flag | replacement | removed_in | notes |
|---|---|---|---|
| -XX:+UseConcMarkSweepGC | (delete — G1 is default) | JDK 14 (JEP 366) | CMS removed; on 14+ startup error |
| -XX:CMSInitiatingOccupancyFraction | (delete) | JDK 14 | CMS-only tuning |
| -XX:+UseCMSInitiatingOccupancyOnly | (delete) | JDK 14 | CMS-only tuning |
| -XX:+UseParNewGC | (delete) | JDK 14 | ParNew was CMS-coupled; removed with it |
| -XX:ParallelCMSThreads=N | -XX:ConcGCThreads=N | JDK 14 | Only if parallel collection still wanted |
| -XX:PermSize / -XX:MaxPermSize | (delete — metaspace has no PermGen) | warning since 8, error later | Most common legacy leftover |
| -XX:+UseSplitVerifier | (delete) | JDK 9 error | Stack map tables mandatory since 7 |
| -XX:+UseBiasedLocking | (delete) | disabled 15 (JEP 374) | Ignored with warning on 15+, removed later |
| -XX:BiasedLockingStartupDelay=N | (delete) | with JEP 374 | |
| -XX:+PrintGCDetails / -XX:+PrintGCTimeStamps / -XX:+PrintGCDateStamps | -Xlog:gc* | JDK 9 (unified logging, JEP 158/271) | e.g. `-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m` |
| -Xloggc:<file> | -Xlog:gc:file=<file> | JDK 9 | Unified logging replaces -Xloggc |
| -XX:+UseGCLogFileRotation / -XX:NumberOfGCLogFiles / -XX:GCLogFileSize | -Xlog:gc:filecount=N,filesize=M | JDK 9 | |
| -XX:+PrintTenuringDistribution | -Xlog:gc+age=trace | JDK 9 | |
| -XX:+UseStringDeduplication | keep (G1-only now) | n/a | Was effective on G1 in 8 too; meaningless with CMS removed |
| -XX:+UseCompressedOops | (delete unless constraining) | n/a | On by default; explicit setting only pins behavior |
| -XX:MaxDirectMemorySize | keep | n/a | Still valid; unchanged |
| -XX:+HeapDumpOnOutOfMemoryError / -XX:HeapDumpPath | keep | n/a | Still valid and recommended |
| -Djava.security.manager=allow | (add only if SecurityManager genuinely still set) | JDK 17 (JEP 411) | Without it `System.setSecurityManager` throws UnsupportedOperationException |
| -XX:+UseNotificationThread … | n/a | n/a | Internal; no user action |

## New flags the migration typically must ADD

| flag | purpose | notes |
|---|---|---|
| --add-opens java.base/<package>=ALL-UNNAMED | Fix `InaccessibleObjectException` from reflective JDK-internal access | Narrowest package that fixes the failure; record why for cleanup |
| --add-exports java.base/<package>=ALL-UNNAMED | Fix compile-time-style access to non-exported packages | For `sun.security.*` etc. |
| --illegal-access=permit | **DO NOT USE** | Worked on 9–15, ignored-with-warning on 16, fatal error on 17 (JEP 403) — legacy scripts carrying it crash |
| -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m | GC logging replacement | One-line modern equivalent of the old flag block |

## Default behavior changes (no flag action, but affects verification)

- Default GC: G1 (since JDK 9) — CMS/Parallel pause-time expectations change
- Biased locking disabled (JDK 15): uncontended synchronization slightly slower; contended unaffected
- Compact strings (JDK 9): `String` memory footprint changes; `-XX:+PrintFlagsFinal | grep UseCompressedStrings` era flags gone
- `String.format`/`Math` minor behavior: exact-equality assertions may need re-baselining
- `file.encoding` default is UTF-8 since JDK 18 — on 17 still platform-dependent; set `-Dfile.encoding=UTF-8` explicitly to future-proof
