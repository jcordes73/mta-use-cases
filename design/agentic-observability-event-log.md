# Agentic run observability and a persistent event log

Design proposal for closing two structural observability gaps in the PoC's agentic architecture:

- Runs are invisible once no client is attached to the ACP stream.
- Nothing persists after the sandbox pod dies.

This document is referenced from the README's appendix; the proposal is not implemented — it is a suggested design for discussion and upstream alignment.

## Proposal

The PoC's agentic architecture has two observability gaps: runs are invisible once no client is attached to the ACP stream, and nothing persists after the sandbox pod dies. [design/agentic-observability-event-log.md](design/agentic-observability-event-log.md) fixes both:

- **Fix**: the in-pod harness keeps driving the run, but its ACP tee pushes a normalized event stream to the Hub — tool calls, HITL permission round-trips, steering, token usage, skill markers.
- **Storage**: the Hub persists events in its existing PostgreSQL database, serves live subscribers and retrospective replay from that store, and exposes analytics aggregates.

- **Variant decision**: Hub as recorder/broker (chosen) vs. Hub as ACP driver (rejected — breaks the harness-owned git lifecycle, batch playbooks, and fail-closed HITL).
- **Topology change**: the Hub's pod-dial WebSocket proxy (with its `X-Secret-Key` credential swap) is replaced by an outbound, cursor-resumable uplink from the pod — the per-run ACP key never leaves the pod, the browser keeps the hub token as its only credential, and the controller stays stateless.
- **Event schema**: append-only, `seq`-ordered, normalized at capture time — including the `skill.loaded` markers only the harness can emit, which enable token-to-skill attribution.
- **Persistence**: PostgreSQL with JSONB in the Hub's existing database — append-only and idempotent (`(run_id, seq)` upsert), monthly partitions for retention, rollup tables kept forever for analytics; no new datastore in disconnected clusters.
- **Analytics**: effectiveness measured per successful outcome (tokens per migrated file, tool failure rates, skill-version A/B, HITL friction, loop detection).
- **Related upstream work**: aligned with open proposals #341 (transcript persistence with a working prototype), #192 (usage accounting/telemetry research), and #282 (usage telemetry ADR) — this design's unique contributions are the normalized event schema, relational persistence/analytics, and skill attribution.
- **Six implementation phases**; the first two (always-on recorder + Hub persistence) deliver reviewability on their own, before any console change.
## Status

Suggested design — not implemented. Feedback and upstream coordination welcome (see the related proposals above).
