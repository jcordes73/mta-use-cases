# Phase: Testing Migration

Migrate Ant `<junit>` targets to Surefire, coverage to JaCoCo, and integration tests to Failsafe.

## Steps

1. Read `references/pattern-map.md` — apply the testing rows
2. **Unit tests** (guide Chapter 6): the `<junit>` block with hand-assembled classpath, `<batchtest>`, and `<formatter>` is replaced by `mvn test` — Surefire discovers `*Test`, `Test*`, `*Tests`, `*TestCase` in `src/test/java`; reports under `target/surefire-reports/`
   - Rename nonconforming test classes (e.g. `LegacyChecks`) or add explicit `<includes>` and delete it as they are renamed — silent skips are worse than Ant's visible include list
   - TestNG also auto-detected; `<suiteXmlFiles>` for XML suite files
   - JUnit 4 legacy tests: add `junit-vintage-engine` (test scope) alongside `junit-jupiter`; needs Surefire >= 2.22.0; track removal of vintage
3. **Test fixtures**: move to `src/test/resources`; delete the Ant copy steps; add `<filtering>true</filtering>` if fixtures contain tokens
4. **Coverage**: JaCoCo/Cobertura Ant instrumentation → `jacoco-maven-plugin` with `prepare-agent` + `report` executions; `mvn verify` produces the HTML report under `target/site/jacoco` (do not port Cobertura — unmaintained)
5. **Integration tests**: the separate `itest` target → `maven-failsafe-plugin` with `integration-test`/`verify` goals and `*IT` naming; reports under `target/failsafe-reports/`; delete the `itest` target
6. Run the full test suite (not just compilation)

## Build gate

Run the project's test suite:

- `mvn test` for unit tests, then `mvn verify` for integration tests and coverage
- Test failures or missing tests usually indicate: naming-convention mismatches (compare test counts against the Ant JUnit run), test source directories the POM does not pick up, or scoping errors (`test`-scoped jar the main code imports)
