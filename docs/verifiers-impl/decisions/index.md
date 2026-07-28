# Verifier decisions

Read [`../current-state.md`](../current-state.md) for what the verifier does now. Read a decision only when you need the rationale behind a non-obvious active constraint.

A decision remains here only when:

- the choice still has an active consequence;
- rejected alternatives or tradeoffs are likely to matter again;
- a short explanation in current state would not preserve enough rationale.

Superseded designs, implementation chronology, and evidence baselines belong in Git history rather than an active decision tree.

## Active decisions

| Decision | Why it remains separate |
|---|---|
| [`Domain service vs coordination facade`](2026-04-06-domain-service-vs-coordination-facade.md) | Preserves the application-independent structural classification rule and rejects naming/package heuristics |
| [`Static package vs dynamic evidence`](2026-04-28-hybrid-static-dynamic-key-binding.md) | Defines which facts own reproducible scenario identity and why runtime evidence remains sidecar-only |
| [`Conflict-anchor segment compression`](2026-06-16-conflict-anchor-segment-compression.md) | Defines the non-obvious schedule reduction and its semantic limits |
| [`Compensation-aware v3 contract`](2026-07-19-compensation-aware-fault-scenario-contract.md) | Defines the current WorkloadPlan/FaultScenario and executor failure boundary |

## Maintenance rule

Link a retained decision from the exact `current-state.md` section it explains. When a decision is superseded, update current state first, then delete or consolidate the old record rather than keeping multiple active narratives.
