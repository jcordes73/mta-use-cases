---
name: mig-verify
description: Verifies a MigIQ migration by building, testing, and checking handoff artifacts against the migration plan. Use after mig-execute to confirm the target branch compiles and critical checks pass.
---

# MigIQ Verify

Follow this skill after the execute stage.

## Inputs

- `docs/plan.md` from the plan stage
- `.konveyor/handoff.md` with an `## Execute` section from the execute stage
- Optional Hub analysis at `.konveyor/analysis.json`

## Steps

1. Read `docs/plan.md` and identify the build/test commands for the target stack.
2. Run the build. Fix compile errors until the build succeeds (or document blockers).
3. Run available unit/integration tests; record failures with root cause notes.
4. Confirm key migration goals from the plan are reflected in the source tree.
5. Append a `## Verify` section to `.konveyor/handoff.md` with build/test results and remaining risks.

## Output

Update `.konveyor/handoff.md` with `## Verify` before finishing the stage.
