# Consistency Layer Decision Guide

This document is for **AI agents**. When building a new application from human-authored domain templates, use this guide to classify each domain rule into the correct consistency layer before writing any code.

Human domain experts define the rules in the domain model template (`{App}-domain-model.md`). The AI agent's job is to read those rules and decide *how* to implement them — not to invent or change the rules themselves.

---

## Step 1 — Classify §3.1 rules (single-entity)

Every rule in §3.1 of the domain model involves only fields of a single aggregate.

→ **Always implement as Layer 1 (intra-invariant) inside `verifyInvariants()`.**


---

## Step 2 — Classify §3.2 rules (cross-entity)

For each rule in §3.2, answer the questions below in order:

```
Does the rule involve only data that lives inside a SINGLE aggregate
(including its cached snapshot fields)?
  YES → Layer 1 — implement in verifyInvariants()
  NO  → continue ↓

Must the rule be checked SYNCHRONOUSLY (strong consistency — same UoW)?
  YES → Layer 2 — service guard in *Service.java
        If the check validates a field on a DTO received from a preceding saga
        data-assembly step (e.g. checking `userDto.isActive()`), this is still
        Layer 2 — it is input validation on the parameters passed to the service.
        If the precondition is implicit in the fetch query (query fails when
        precondition is unmet), no explicit service check is needed
        → classify as Construction prerequisite.
  NO  → Eventually consistent (~1 s lag) is acceptable?
    YES → Layer 4 — inter-invariant (event subscription + handler chain)
```

---

## Step 3 — Layer 4 rules never block operations

A Layer 4 inter-invariant is eventually consistent. It may only **cache state** in the consumer aggregate — no operation is blocked based on that cached state.

If you concluded that a rule belongs in Layer 4 but also requires blocking an operation, re-classify it:

- If blocking must be synchronous → **Layer 2** (service guard; add a saga data-assembly step if the needed data lives in another aggregate).
- If true eventual consistency is acceptable → **Layer 4** (inter-invariant); no guard is added.

---

## Quick-Reference Table

| §3.2 rule characteristic | Layer | Implemented by |
|--------------------------|-------|---------------|
| All referenced data is already inside the same aggregate (including snapshots) | 1 | intra-invariant |
| Synchronous check — own-table read, or input validation (including fields on DTO received from saga step) | 2 | service guard |
| Eventual cache sync from another aggregate (no blocking) | 4 | inter-invariant |

---

## Common Mistakes to Avoid

**Do NOT duplicate a rule across layers.**
Once a rule is placed at Layer 1, do not also add it at Layer 2 or 3. Layer 1 fires on every commit regardless of which operation caused the change — this is the canonical single location for single-aggregate invariants.


