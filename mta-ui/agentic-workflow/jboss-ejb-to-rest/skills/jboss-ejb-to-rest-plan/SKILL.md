---
name: jboss-ejb-to-rest-plan
description: Stage 1 of 3 of the jboss-ejb-to-rest migration. Audits
  inter-application EJB (remote @Remote, ejb:/JNDI lookups, SOAP/JAX-WS exposed)
  call sites in a JBoss EAP 8.0+ codebase, builds a migration inventory with
  per-call-site go/no-go, and proposes the migration order. Run this before any
  code changes; it makes no modifications. Stages 2 and 3 are the skills
  jboss-ejb-to-rest-author and jboss-ejb-to-rest-verify.
license: Apache-2.0
metadata:
  stage: 1
  next_skill: jboss-ejb-to-rest-author
  language: java
  guide_url: "README.md (repo-local: JBoss EAP EJB-to-REST refactoring guide)"
  generated_by: migration-skills-generator
  generated_at: "2026-11-10T00:00:00Z"
---

# JBoss EAP EJB-to-REST Migration — Stage 1: Plan

Before changing any code, inventory every inter-application EJB (or SOAP-exposed
EJB) call site. Automation does the discovery; humans interpret the inventory and
decide the order and the go/no-go per call site.

**Workflow position:** Stage 1 of 3. This skill makes **no code changes**. When
its output is approved, continue with the **`jboss-ejb-to-rest-author`** skill
(Stage 2), then **`jboss-ejb-to-rest-verify`** (Stage 3). Section references
(§n) are citations into the original guide (see the `guide_url` metadata).

## Prerequisites

Before starting, verify all of the following; if any fail, stop and resolve first:

- **JBoss EAP 8.0+ with `jakarta.*` namespace** — EAP 7.4 is `javax.*` (Jakarta EE 8).
  If the codebase is still on `javax.*`, stop — the namespace migration is a
  separate migration that must run first (upgrade to EAP 8 or Eclipse Transformer).
- **EAP XP (Expansion Pack)** for MicroProfile REST Client, OpenAPI, JWT, Fault
  Tolerance, Telemetry. Install with `jboss-eap-installation-manager.sh install
  --profile eap-xp-5.0`. Without the XP, the fallback paths become primary: plain
  JAX-RS Client, manual OpenAPI resource, Elytron `token-realm`.
- **TLS in production** — all inter-application REST calls run over `https://`.

## When to use / not use

Use this migration when **existing EJB couplings between applications must move
to REST** — with a clear functional or business domain boundary, consumers you
can coordinate with, and no hard dependency on cross-application atomicity. It
does not apply — or adds no value — in the following situations:

| Situation | Why not | What to do instead |
|---|---|---|
| Local EJB/CDI calls inside one deployment | Never cross an app boundary; REST adds latency and serialization for no gain | Keep local interfaces (only the monolith-split stage §7 applies, and only with a real domain crossing) |
| Not on JBoss EAP 8.0+ / still `javax.*` | `jakarta.*` examples and EAP XP prerequisites assume EAP 8.0+ | Namespace migration first (upgrade to EAP 8 or Eclipse Transformer) |
| Integration is asynchronous messaging (JMS) | Synchronous REST is the wrong tool for fire-and-forget | Keep or introduce JMS |
| Atomic transactions must span the two applications | REST cannot propagate transactions | Keep the coupled path (IIOP/XTS) until redesigned — saga, compensation, or LRA (EAP XP 6.0) |
| Working SOAP integration with WS-Security, no REST requirement | The migration keeps SOAP as the supported contract anyway | Leave it on SOAP unless new consumers actually need REST |
| Consumers you cannot coordinate with | They cannot be driven to cut over on a schedule | Keep the old contract (`@Remote`/SOAP stays supported); add REST only if you own the consumers |
| Latency-critical, very high-frequency calls | REST + JSON per-call overhead vs binary remoting | Batch endpoints first, or reconsider whether the call should cross an app boundary |
| Public/external APIs | This targets internal integration; public contracts need CORS, rate limiting, stricter versioning | Use an API-design practice for external contracts |
| New (greenfield) services | Migrating EJB patterns would be a detour | Design JAX-RS/messaging directly from the start |

When in doubt about a single call, the inventory items below (transactions,
security, data, consumers) double as the per-call-site go/no-go check.

## Guardrails

- **The agent proposes, the human decides the seam.** Migration ordering (§1.3)
  is a human decision — this stage may draft the order, never finalize it.
- **Old paths are off-limits** (§4.4): nothing in any stage may delete the
  `@Remote` view, SOAP endpoints, remoting subsystem config, or EJB client
  artifacts — retirement is a separate, human, audited decision.
- **Log findings** in the migration inventory file with section references, so
  the Author and Verify stages and the eventual §4.4 audit have a complete record.

## Workflow

### 1. Check prerequisites

Verify each Prerequisites item above.

### 2. Run the deterministic discovery commands (§9.2)

Run per application and collect output into an inventory file:

```bash
# Remote EJB surfaces (provider + consumer)
grep -rn --include="*.java" -E "@Remote|@EJB|InitialContext|javax\.naming|jakarta\.naming" src/

# Concrete ejb: lookups (the call sites to cut over)
grep -rn --include="*.java" -E 'lookup\s*=\s*"ejb:|new InitialContext' src/

# SOAP-exposed EJBs (§2.6 variant)
grep -rn --include="*.java" -E "@WebService|@WebMethod|@WebServiceRef" src/

# EJB client artifacts and config (cleanup gates)
grep -rn --include="pom.xml" -E "ejb-client|jboss-ejb-client|wildfly-ejb-client-bom" .
find . -name "jboss-ejb-client.xml" -o -name "jndi.properties" -o -name "jboss-deployment-structure.xml"

# Shared API jar — the compile-time coupling (the file this migration removes)
grep -rn --include="pom.xml" -B2 -A2 "<artifactId>.*-api</artifactId>" .

# Async/state that does not survive a split (§5.7, §7.6)
grep -rn --include="*.java" -E "@Asynchronous|@Singleton|@Schedule|@ApplicationScoped|ThreadLocal" src/
```

Also check WSDL artifacts (`*.wsdl`, `*_PortType.java`, `wsimport`/`wsconsume`
generated stubs) and server config (`remote-outbound-connections`,
`iiop-configuration` in `standalone.xml`).

### 3. Build the inventory (§1.2)

**Scope note:** the discovery commands above are written for one application's
`src/` layout. In a multi-module or monorepo project, run them repo-wide and
group the hits per application/deployment (e.g. pipe through
`sed 's|^\./||;s|/src.*||' | sort -u`) — the postcondition then applies per
application. Write the inventory to **`docs/jboss-ejb-to-rest-inventory.md` at
the repository root** (git-tracked, not inside the application path — the
author and verify stages of the next sandboxes read it after cloning the
target branch). If the user has already agreed a different location, use that
instead.

For each call site, record:

- **Provider bean**: target app name, bean class, remote interface
- **Consumer call sites**: class, method, call frequency (if known)
- **Data exchanged**: parameter/return types — flag types needing DTO/JSON mapping
- **Error contract**: checked exceptions on the interface, unchecked thrown
- **Transactionality**: does the call join the caller's transaction
  (`MANDATORY`/spanning `REQUIRED`, IIOP/XTS)? Flag these — see §5.2/§5.6.
- **Security**: `@RolesAllowed` on the remote interface; for SOAP endpoints,
  whether WS-Security headers are used (the most common hidden cost — replaced by
  §2.4/§3.5 auth options)
- **Consumers outside your control**: third-party WSDL consumers → keep SOAP,
  add REST in addition
- **Network path**: firewall/LB/DNS entries pointing at the remoting or SOAP port

**Postcondition:** every line of `grep -E 'lookup\s*=\s*"ejb:'` output appears in
the inventory with target application, call-site class/method, semantics
(read/write, idempotent?), consumers, and a go/no-go per §1.2.

### 4. Apply the go/no-go checks

Apply the "When to use / not use" table above to every call site in the
inventory — skip and report any call site that matches a row.

### 5. Propose the migration order (§1.3) — human decision

Propose: simple read-only calls first → complex write calls →
transaction-participating calls last. Migrate **one provider interface at a
time**, old and new endpoints live in parallel. **The human decides** the final
order (domain judgment, team knowledge).

### 6. Calibrate against the worked example

If the guide repository's `example/` directory is present: a transformation of
`example/before/` should match `example/after/` — use it to sanity-check the
planned transformations before touching real code.

## Output and handoff

- Inventory file covering every discovered call site with go/no-go
  (`docs/jboss-ejb-to-rest-inventory.md` at the repository root)
- Proposed (not final) migration order
- List of human decisions required before the Author stage starts

Append a `## Plan` section to `.konveyor/handoff.md` (create the file if
absent). The handoff is a **fixed-format document** — the author and verify
stages (and any tooling that refines the skills) parse it, so follow the
template exactly; put narrative detail inside the fields, never change the
field names or order:

    # JBoss EJB→REST — <application name> (<run label>)
    ## Plan
    - Inventory file: docs/jboss-ejb-to-rest-inventory.md
    - Prerequisites: <pass/fail per check, incl. EAP XP availability>
    - Inventory verdicts: <call site/app> → go | go-with-redesign | conditional | no-go (<reason>); one per element
    - Proposed order: <ordered list, marked PROPOSAL>
    - Human decisions pending: <numbered list>
    - Notes: <assumptions, risks, skipped checks>

**Commit** `docs/jboss-ejb-to-rest-inventory.md` and `.konveyor/handoff.md` —
each stage runs in a fresh sandbox and continuity carries via git on the
target branch. Do not commit anything else in this stage (it makes no code
changes).

**Handoff:** with the inventory approved, load the **`jboss-ejb-to-rest-author`**
skill and pass it the inventory file and the confirmed order.
