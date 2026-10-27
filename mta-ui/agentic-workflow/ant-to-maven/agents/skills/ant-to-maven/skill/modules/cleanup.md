# Phase: Cleanup and Verification (Cutover)

Verify parity with the Ant build, decommission it, and audit remaining Antrun executions.

## Steps

1. **Verify parity** (guide Chapter 13's verification checklist — every item must pass before the authoritative flag flips):
   - The same unit tests pass under both builds (`ant test` and `mvn test` both green on the same revision)
   - Test-count parity: surefire reports the same number of tests as the Ant JUnit run — a missing test is usually a naming-convention mismatch or a test directory the POM does not pick up
   - Artifact contents compared: `unzip -l` (or `jar tf`) diff between the Ant-built and Maven-built artifacts — manifest and `META-INF/maven/` differences are expected; missing classes, stale resources, or extra files are migration gaps
   - Startup smoke test: the Maven-built artifact runs (application starts and answers a trivial request; a library resolves from the local repository and a consumer links against it)
   - CI produces equivalent outputs: artifacts, reports, coverage data
2. **Remove old build artifacts:** delete `build.xml`, the `lib/` directory, and Ant property files (`build.properties`, per-environment property files). Do not keep "just in case" — version control keeps the history
3. **Search for remaining Ant references:** grep the repository for `ant ` invocations and `build.xml` references — IDE configuration files and contribution guides are the usual hiding places; update README, onboarding docs, wikis, and CI config (remove `lib/` checkout logic, Ant home setup, Ant-era output archiving)
4. **Audit every `maven-antrun-plugin` execution** added during the migration (re-run the strategy ladder from the Code phase):
   - Side-effect wrappers (build-info files, notifications, internal scripts) can stay — antrun is a fine permanent home
   - Anything touching the compile classpath, test execution, or artifact assembly must become native Maven before cutover
   - Heuristic: if deleting an antrun execution would change what lands in `target/classes` or the packaged artifact, it is build-proper work and converts; if it only removes an external side effect, it stays
   - Record the audit outcome in the commit or PR that flips the authoritative flag
5. **Final build and test:** `mvn -B -ntp verify` runs unit tests, integration tests, and produces both Surefire and JaCoCo reports
6. **Report to user:**
   - List all changes made across all phases
   - Flag any items that could not be automatically migrated (require manual review)
   - Announce the cutover with the cheat-sheet: `mvn clean verify` (full build with gates), `mvn package` (quick iteration), `mvn test` (tests only), plus the project-specific goal equivalents of the old Ant targets
   - Note the sunset date for the Ant fallback — a fallback with no expiration date is just two builds forever
