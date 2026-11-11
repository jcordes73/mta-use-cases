# Sources

All mapping-table rows trace back to the guide in this repository.

## Primary guide

- **Guide:** `README.md` — "Refactoring Guide: Inter-Application Calls from EJB to
  REST" (JBoss EAP 8.0+ → Jakarta REST + MicroProfile REST Client)
- **Worked example:** `example/` (before/after EJB→REST; golden reference for
  calibrating automated refactoring, §9.4)
- **Example details:** `example/README.md` (integration tests, smoke-test setup)

## Section → mapping table routing

| Guide section | Mapped into |
|---|---|
| Introduction + prerequisites (EAP 8, EAP XP, TLS) | SKILL.md Prerequisites |
| When Not to Use This Guide | Plan stage, step 4 (go/no-go) |
| §1 Discovery / Audit (§1.1–§1.3) | Plan stage, steps 2–5 |
| §2 Provider-Side Transformation (§2.1–§2.7) | Author stage Part 2; api-map (provider rows), pattern-map (REST resource, SOAP facade, endpoint design, OpenAPI) |
| §3 Consumer-Side Transformation (§3.1–§3.7) | Author stage Part 3; api-map (consumer rows), pattern-map (adapter seam, `ejb:` gotcha), config-map (MP Config) |
| §4 Migration Sequencing (§4.1–§4.5) | Author stage (one-call-site-at-a-time, rollback), Verify stage (§4.3 checklist, §4.5 testing) |
| §5 Semantic Gotchas (§5.1–§5.8) | pattern-map (transactions, IIOP, performance, correlation ID), config-map (timeouts), verify-errors (payload mismatch) |
| Deployment Descriptors to Check | Author stage Part 4; config-map |
| §6 Cleanup Checklist | Verify stage, step 4 |
| §7 Splitting a Monolith | pattern-map (human-only decision row); flagged in Author/Verify reports |
| §9 Automation Notes (§9.1–§9.4) | SKILL.md Guardrails, Plan stage (§9.2 commands), Verify stage (§9.3 gates) |
| §10 Verification (§10.1–§10.3) | verify-errors (runtime findings), Verify stage step 2 |
