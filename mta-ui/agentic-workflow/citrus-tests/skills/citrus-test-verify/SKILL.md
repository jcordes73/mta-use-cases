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
planned? **validation points implemented as contracted?** - check the
committed test sources for the plan's validation contract: every
`receive` action carries a `.message()` block with the planned status,
body and header validations, and simulation servers validate the
incoming request before responding. A `receive` without message
validation passes on any response - a green suite built on such actions
is **vacuous**, and vacuous-green is treated exactly like a failing
test: fix it (restore the planned validations, test-side) or record it
as an exception in the verdict. If the author stage deviated, treat the
handoff's reasons as authoritative unless they contradict the plan's
invariants (deterministic, green, surefire-run) or weaken a planned
validation.

If the `MAVEN_SETTINGS_XML` environment variable is set, write its value
to `/home/harness/.m2/settings.xml` before any Maven call. If it is unset,
note that the sandbox image bakes a default settings file with a Squid
proxy (`squid.squid.svc:3128`) that is dead in a connected environment —
the author stage usually removed the `<proxies>` block already, but if
Maven downloads time out, probe and remove the proxy the same way before
diagnosing anything else.

### 2. Run the suite from clean, under the JaCoCo agent

Run the plan's verification command from the application path.

**Container lane** (`test_kind: it` in the plan): the command is
`mvn verify -Pcitrus-container-tests` (the plan's exact command). This
provisions a real JBoss EAP server, boots it, deploys the application
WAR, runs the planned `*IT` classes via failsafe, shuts the server
down, and writes the server-side coverage to
`target/site/jacoco-it/jacoco.csv` — all in one invocation. Confirm
from the output that: the server provisioned and booted
(`WFLYSRV0025: JBoss EAP ... started`), the WAR deployed
(`WFLYSRV0010: Deployed "..."`), and the failsafe reports list every
planned `*IT` class with passing counts (failsafe reports live in
`target/failsafe-reports/`, not surefire's). A skipped provision step
(e.g. the profile not active) means the tests did not run against a
container — that is not verified. A first run provisions the server
from Maven (minutes of downloads); a resolution failure is not a test
failure — check the repositories/channels guidance in the author
skill before diagnosing further, and retry with `-U` if a previous
failed resolution was cached.

**In-JVM lane** (`test_kind: test`, default): run the plan's
verification command with the
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

Each lane yields both the test-execution proof and the coverage data in
one run.

### 3. Fix test-side defects if needed

If a test fails, diagnose and fix within the test-side scope (test code,
endpoint config, test dependencies). A known silent failure signature
(verified live): Citrus 5.x tests that compile fine but fail at runtime
with `Could not initialize class tools.jackson.databind.json.JsonMapper$Builder`
or `NoClassDefFoundError com/fasterxml/jackson/annotation/JsonSerializeAs`
mean the pom pins a Jackson 2 `jackson-annotations` while Citrus 5 needs
the Jackson-3-compatible `2.2x` line (e.g. `2.22`) - the author skill
carries the full recipe; check the pom's test pins first when you see
these. Re-run until green or until the fix
clearly requires main-source changes - in that case, disable the failing
test with an explicit `@Disabled` and the reason, and flag it in the
handoff as requiring author-stage follow-up. Keep fixes bounded - a green,
honest suite is the goal, not a maximal one.

### 4. Quantify the coverage the new tests add

The run in step 2 produced `target/site/jacoco/jacoco.csv` (one row per
application class: `PACKAGE`, `CLASS`, `INSTRUCTION_MISSED/COVERED`,
`BRANCH_MISSED/COVERED`, ...). Parse it and aggregate. In the container
lane the file is `target/site/jacoco-it/jacoco.csv` (the server-JVM
agent's report) and a class absent from it means it never executed in
the server — check the deployment, not the test.

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
  numbers so the delta is the new tests' contribution. **Baseline
  command caveat (observed live)**: a `-Dtest='!NewTest'` filter
  overrides surefire's default includes - the run may pick up other,
  foreign tests (e.g. a JUnit 4 failsafe `*IT`) that then fail against
  absent infrastructure. Exclude every non-planned test class
  explicitly (`-Dtest='!NewTest,!OtherTest,...' -DfailIfNoTests=false`),
  and when the new tests are the module's only surefire tests the
  baseline run executes nothing and produces no jacoco data - that
  absence IS the 0% baseline. (Container
  lane: exclude via failsafe instead, e.g.
  `-Dit.test='!HelloWorldResourceIT'` - documented from the failsafe
  conventions, not yet validated live; if the exclusion misbehaves,
  record the baseline as "not computed" with the reason. The
  helloworld-rs container validation measured the committed-run side:
  100% instruction coverage of the deployed application, agent on the
  provisioned server JVM.)

The report is quantitative context for the verdict, **not** a pass/fail
gate: low coverage of elements the plan marked out-of-scope (container-
bound code) is expected, and the skill's gap accounting remains
element-based (the plan's inventory), not percentage-driven. Do not add
tests to chase the number - report honestly what the committed suite
covers.

### 5. Record the verdict

Append a `## Verify` section to `.konveyor/handoff.md`, following the
fixed-format template (fixed section header, factual field lines), and
end with the verdict line in exactly one of the two forms the plan
skill defines:

    ## Verify
    - Commands run: <exact commands>
    - Executed: <class → counts; every planned test or the gap named>
    - Fixes applied: none | <numbered list>
    - Coverage: <per-package/per-class instruction and branch coverage
      table from the committed run, plus the baseline and the delta
      attributable to the new tests; or "not measured" + reason>
    Verdict: verified
    Verdict: verified with exceptions (<numbered list follows)

**Commit** any test-side fixes plus the handoff update with a message
summarizing the verification outcome - the run's target branch is the
deliverable the user reviews.
