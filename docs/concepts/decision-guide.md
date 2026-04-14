# Consistency Layer Decision Guide

This document is for **AI agents**. When building a new application from human-authored domain templates, use this guide to classify each domain rule into the correct consistency layer before writing any code.

Human domain experts define the rules in the domain model template (`{App}-domain-model.md`). The AI agent's job is to read those rules and decide *how* to implement them — not to invent or change the rules themselves.

---

## Step 1 — Classify §3.1 rules (single-entity)

Every rule in §3.1 of the domain model involves only fields of a single aggregate.

→ **Always implement as Layer 1 (intra-invariant) inside `verifyInvariants()`.**

Use the `/intra-invariant` skill.

---

## Step 2 — Classify §3.2 rules (cross-entity)

For each rule in §3.2, answer the questions below in order:

```
Does the rule involve only data that lives inside a SINGLE aggregate
(including its cached snapshot fields)?
  YES → treat as Layer 1 — implement in verifyInvariants() with /intra-invariant
  NO  → continue ↓

Must the rule be checked SYNCHRONOUSLY (strong consistency — same UoW)?
  YES → Does the check read only the aggregate being mutated?
    YES → Layer 2 — implement a service-layer guard with /service-guard
    NO  → Layer 3 — implement a saga step with setForbiddenStates in /new-functionality
  NO  → Eventually consistent (~1 s lag) is acceptable?
    YES → Layer 4 — implement an inter-invariant with /inter-invariant
```

---

## Step 3 — Layer 4 rules never block operations

A Layer 4 inter-invariant is eventually consistent. It may only **cache state** in the consumer aggregate — no operation is blocked based on that cached state.

If you concluded that a rule belongs in Layer 4 but also requires blocking an operation, re-classify it:

- If blocking must be synchronous → **Layer 3** (`/new-functionality` with `setForbiddenStates`).
- If true eventual consistency is acceptable → **Layer 4 inter-invariant** (`/inter-invariant`); no guard is added.

---

## Quick-Reference Table

| §3.2 rule characteristic | Layer | Skill |
|--------------------------|-------|-------|
| All referenced data is already inside the same aggregate (including snapshots) | 1 | `/intra-invariant` |
| Synchronous check, reads only the mutated aggregate's own table | 2 | `/service-guard` |
| Synchronous check, reads a DIFFERENT aggregate | 3 | `/new-functionality` with `setForbiddenStates` |
| Eventual cache sync from another aggregate (no blocking) | 4 | `/inter-invariant` |

---

## Common Mistakes to Avoid

**Do NOT use Layer 4 for a rule that requires synchronous guarantees.**
Layer 4 is eventually consistent (~1 s lag). If the rule must hold at commit time, use Layer 2 or 3.

**Do NOT use Layer 2 if the check reads a different aggregate.**
Layer 2 reads only the aggregate being mutated, within the service's own `@Transactional(SERIALIZABLE)` boundary. Reading a foreign aggregate from Layer 2 violates the architectural restriction. Use Layer 3 instead.

**Do NOT duplicate a rule across layers.**
Once a rule is placed at Layer 1, do not also add it at Layer 3 or 5. Layer 1 fires on every commit regardless of which operation caused the change — this is the canonical single location for single-aggregate invariants.

**Layer 1 runs on every commit — keep it cheap.**
`verifyInvariants()` is called on every UoW commit. Do not put DB queries there. If a check requires reading the database, it belongs at Layer 2 or 3.

---

## How to Use This During `new-application`

In Phase 1 of `/new-application`, after reading the human-provided domain model and aggregate grouping templates, produce a **rule classification table** and confirm it with the user before writing any code:

| Rule | Type | Layer | Skill |
|------|------|-------|-------|
| `{RULE_NAME}` | §3.1 | 1 | `/intra-invariant` |
| `{RULE_NAME}` | §3.2 | 2 | `/service-guard` |
| `{RULE_NAME}` | §3.2 | 4 | `/inter-invariant` |

This table is the contract between planning and implementation. Do not start Phase 2 until the user confirms the classification.
