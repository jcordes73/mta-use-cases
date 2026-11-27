# Phase: Cleanup

Remove the migration scaffolding that the earlier phases only added as a safety net, and lock the new baseline in.

## Steps

1. Eliminate `--add-opens`/`--add-exports` that code fixes have made unnecessary
2. Verify the compiler configuration is release-pure
3. Remove transitional dependencies
4. Re-enable diagnostics

### Reduce `--add-opens`

For every flag recorded in phase 4, re-check whether it is still required:

- If the triggering reflection was replaced by a supported API, delete the flag
- Run the full test suite **without** the candidate flag; only keep flags that fail without them
- A flag nobody can attribute (added "just in case") is a candidate for removal — try removing it; the suite decides

### Compiler configuration

- Exactly one `maven.compiler.release` property in the parent POM; no child overrides
- No `<source>`/`<target>` anywhere (`grep -rn "maven.compiler.source\|<source>1.8" --include="pom.xml"`)
- `<release>17</release>` (or the property) present; `mvn help:evaluate -Dexpression=maven.compiler.release -q -DforceStdout` prints `17`

### Transitional dependencies

- If phase 2 chose the `javax.xml.bind:jaxb-api` route purely for compatibility, note it as follow-up work for the Jakarta migration — do not silently leave both `javax` and `jakarta` variants of one API on the classpath
- Remove `javassist`/`ow2 ASM` direct dependencies that existed only to support PowerMock (now replaced by Mockito 4+)
- Remove `-Djava.security.manager=allow` if SecurityManager code paths were removed rather than re-enabled

### Diagnostics

- Re-enable `-Xlint:deprecation` and consider `-Werror` on deprecations introduced by this migration (`--release` surfaces JDK 9-17 deprecations the 1.8 target hid)
- Confirm `mvn -q compile 2>&1 | grep -c "warning"` is not larger than the pre-migration baseline
- Final build gate: `mvn clean verify` succeeds on JDK 17

### Verify

- `grep -rn "\-\-add-opens" <launch surfaces>` matches exactly the irreducible list
- `mvn clean verify` green
- Application starts and the smoke path runs on JDK 17
