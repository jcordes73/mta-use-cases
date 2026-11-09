---
name: citrus-test-verify
description: Verifies committed Citrus tests for a JEE application in a fresh environment - runs the test plan's verification commands against the committed branch state under the JaCoCo agent, confirms the new tests actually executed (not skipped), quantifies the coverage the new tests add (per-package/class instruction and branch coverage vs. the pre-existing baseline), fixes test-only defects if needed, and records the verdict. Use as the final stage of a Citrus test-authoring workflow.
---

# Citrus Test Verification

Independently verify the Citrus tests added by the author stage. This is
the **verify** stage of a plan → author → verify workflow, running in a
fresh sandbox that cloned the repository at the target branch - so what
you see is exactly what was committed.

**Scope**: the application path named in the task instructions
($(workflow.path), empty = repository root). You may fix ONLY test-side
files: `src/test/` inside the application path, test-scoped `pom.xml`
changes inside the application path, and the plan/handoff documents. Never
modify `src/main/`.

## Workflow

### 1. Ground yourself

Read `docs/citrus-test-plan.md` and the `## Plan` / `## Author` sections of
`.konveyor/handoff.md`. Diff the committed state against the plan's test
inventory: every planned test class present? dependencies added as
planned? If the author stage deviated, treat the handoff's reasons as
authoritative unless they contradict the plan's invariants (deterministic,
green, surefire-run).

If the `MAVEN_SETTINGS_XML` environment variable is set, write its value
to `/home/harness/.m2/settings.xml` before any Maven call. If it is unset,
note that the sandbox image bakes a default settings file with a Squid
proxy (`squid.squid.svc:3128`) that is dead in a connected environment —
the author stage usually removed the `<proxies>` block already, but if
Maven downloads time out, probe and remove the proxy the same way before
diagnosing anything else.

### 2. Run the suite from clean, under the JaCoCo agent

Run the plan's verification command from the application path with the
JaCoCo agent attached - via command-line goals, so **no pom change is
needed** (goal order matters: prepare-agent before test, report after):

    mvn -pl <module> clean org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent \
        test org.jacoco:jacoco-maven-plugin:0.8.12:report

(0.8.12 covers Java 17+; verified live on a Maven 3.9/JDK 21 module.)
If the JaCoCo plugin cannot be resolved in the environment, fall back to
the plan's plain verification command and record coverage as
"not measured", with the reason - a missing report never blocks the
verdict.

Confirm from the surefire output that:

- the new test classes **ran** (they appear in the run's test list with
  passing counts - a test skipped or excluded by configuration is not
  verified);
- existing tests still pass (no regressions);
- the build is green overall.

This single run yields both the surefire proof and the coverage data.

### 3. Fix test-side defects if needed

If a test fails, diagnose and fix within the test-side scope (test code,
endpoint config, test dependencies). A known silent failure signature
(verified live): Citrus 5.x tests that compile fine but fail at runtime
with `Could not initialize class tools.jackson.databind.json.JsonMapper$Builder`
or `NoClassDefFoundError com/fasterxml/jackson/annotation/JsonSerializeAs`
mean the pom pins a Jackson 2 `jackson-annotations` while Citrus 5 needs
the Jackson-3-compatible `2.2x` line (e.g. `2.22`) — check the pom's test
pins first when you see these. Re-run until green or until the fix
clearly requires main-source changes - in that case, disable the failing
test with an explicit `@Disabled` and the reason, and flag it in the
handoff as requiring author-stage follow-up. Keep fixes bounded - a green,
honest suite is the goal, not a maximal one.

### 4. Quantify the coverage the new tests add

The run in step 2 produced `target/site/jacoco/jacoco.csv` (one row per
application class: `PACKAGE`, `CLASS`, `INSTRUCTION_MISSED/COVERED`,
`BRANCH_MISSED/COVERED`, ...). Parse it and aggregate:

- **per-package and per-class instruction and branch coverage** of the
  application classes under test (this is what the new tests exercised);
- the **pre-existing baseline**, to separate improved coverage from
  coverage the module already had: re-run with the new test classes
  excluded from surefire (no rebuild of the agent needed):

      mvn -pl <module> clean org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent \
          test org.jacoco:jacoco-maven-plugin:0.8.12:report \
          -Dtest='!NewTest1,!NewTest2' -DfailIfNoTests=false

  When the module had no prior tests, this baseline is 0% and the
  report reads "new tests brought coverage from 0% to X%" - the
  helloworld-rs validation case. When tests existed, report both
  numbers so the delta is the new tests' contribution.

The report is quantitative context for the verdict, **not** a pass/fail
gate: low coverage of elements the plan marked out-of-scope (container-
bound code) is expected, and the skill's gap accounting remains
element-based (the plan's inventory), not percentage-driven. Do not add
tests to chase the number - report honestly what the committed suite
covers.

### 5. Record the verdict

Append a `## Verify` section to `.konveyor/handoff.md`: the exact commands
run, test counts (run/passed/failed/skipped), whether every planned test
executed, fixes applied, the **coverage table** (per-package/per-class
instruction and branch coverage from the committed run, plus the baseline
and the delta attributable to the new tests; or "not measured" with the
reason), and the final verdict (verified / verified with exceptions,
listing exceptions).

**Commit** any test-side fixes plus the handoff update with a message
summarizing the verification outcome - the run's target branch is the
deliverable the user reviews.
