# Application Implementation Workflow

Abstract workflow for implementing a microservices-simulator application aggregate by aggregate,
driven by AI agents. Each phase produces a stable artifact that becomes the entry point for the
next session — agents are told exactly what to read and what to produce, not overwhelmed up front.

**Scope:** Sagas transactional model only. TCC is out of scope.

**Canonical example:** `applications/quizzes/` and its spec files
`quizzes-domain-model.md` / `quizzes-aggregate-grouping.md`.

---

## Required Inputs

| File | Content |
|------|---------|
| `{App}-domain-model.md` | Entities, attributes, relationships, rules (§3.1/§3.2), functionalities (§4) |
| `{App}-aggregate-grouping.md` | Aggregate partitioning (§1), snapshots (§2), event DAG (§3), events (§4) |

---

## Conventions

### Package layout
```
applications/{app-name}/
├── pom.xml
└── src/
    ├── main/java/pt/ulisboa/tecnico/socialsoftware/{app}/
    │   ├── commands/
    │   │   └── {aggregate}/           ← one subpackage per aggregate
    │   ├── events/                    ← shared event classes (published by any aggregate)
    │   └── microservices/
    │       ├── exception/             ← {App}Exception.java + {App}ErrorMessage.java
    │       └── {aggregate}/           ← one subpackage per aggregate
    │           ├── aggregate/
    │           │   └── sagas/
    │           │       ├── factories/
    │           │       ├── repositories/
    │           │       └── states/
    │           ├── coordination/
    │           │   ├── eventProcessing/
    │           │   ├── functionalities/
    │           │   └── sagas/
    │           ├── messaging/
    │           ├── notification/
    │           │   ├── handling/
    │           │   │   └── handlers/
    │           │   └── subscribe/
    │           └── service/
    └── test/groovy/pt/ulisboa/tecnico/socialsoftware/{app}/
        ├── BeanConfigurationSagas.groovy
        ├── SpockTest.groovy
        ├── {App}SpockTest.groovy
        └── sagas/
            ├── coordination/
            │   └── {aggregate}/       ← T4 functionality tests
            └── {aggregate}/           ← T1 + T2 (incl. event pub.) + T3 subscription tests
```

### Naming conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Aggregate root | `{Aggregate}.java` | `Tournament.java` |
| Sagas extension | `Saga{Aggregate}.java` | `SagaTournament.java` |
| Saga state enum | `{Aggregate}SagaState.java` | `TournamentSagaState.java` |
| Factory | `Sagas{Aggregate}Factory.java` | `SagasTournamentFactory.java` |
| Custom repository | `{Aggregate}CustomRepositorySagas.java` | `TournamentCustomRepositorySagas.java` |
| Service | `{Aggregate}Service.java` | `TournamentService.java` |
| Command handler | `{Aggregate}CommandHandler.java` | `TournamentCommandHandler.java` |
| Write functionality | `{Operation}FunctionalitySagas.java` | `AddParticipantFunctionalitySagas.java` |
| Read functionality | `{Query}FunctionalitySagas.java` | `GetOpenedTournamentsFunctionalitySagas.java` |
| Event subscription | `{Aggregate}Subscribes{Event}.java` | `TournamentSubscribesUpdateStudentName.java` |
| Event handling | `{Aggregate}EventHandling.java` | `TournamentEventHandling.java` |
| Event handler | `{Aggregate}EventHandler.java` | `TournamentEventHandler.java` |
| Event processing | `{Aggregate}EventProcessing.java` | `TournamentEventProcessing.java` |

### Test naming

See [`docs/concepts/testing.md`](concepts/testing.md) for the full taxonomy (T1–T4).

| Type | Pattern | Session |
|------|---------|---------|
| T1 Aggregate | `{Aggregate}IntraInvariantTest.groovy` | 2.N.a |
| T2 Service | `{Aggregate}ServiceTest.groovy` — one class per aggregate; also owns event-publication assertions | 2.N.b (write methods + event publication; read-method cases appended in 2.N.c) |
| T3 Subscription (Inter-Invariant) | `{Aggregate}InterInvariantTest.groovy` | 2.N.d |
| T4 Write Functionality | `{Operation}Test.groovy` | 2.N.b |
| T4 Read Functionality | `{Query}Test.groovy` | 2.N.c |

---

## plan.md — The Job Queue

`plan.md` lives at `applications/{app-name}/plan.md`. It is produced by Phase 1 and updated
(checkbox ticked) at the end of every subsequent session. It is the single entry point for every
agent in Phase 2 and Phase 3: a Rule Classification table, an Aggregate Implementation Order table
(topological sort), one Aggregate Details section per aggregate (functionalities, events,
cross-aggregate prerequisites, the per-session file list, a checklist), and a Phase 3 test-review
table.

Illustrative excerpt (one row of the Implementation Order table):

```markdown
| # | Aggregate | Upstream deps | Events published | Events subscribed | Sessions |
|---|-----------|--------------|-----------------|-------------------|---------|
| 1 | Course    | —            | —               | —                 | a b c   |
```

> Sessions column: `a`=domain, `b`=write functionalities, `c`=read functionalities,
> `d`=event wiring (only when Events subscribed is non-empty).

The full output structure (every section, exact table columns, and generation rules) is
authoritatively defined in
[`.claude/skills/classify-and-plan/SKILL.md`](../.claude/skills/classify-and-plan/SKILL.md) Steps
7-8 — that skill is what generates `plan.md`, so it owns the shape. Do not restate the template
here; if it changes, edit the skill, not this file.

---

## Phase 0 — Bootstrap

**One session. No plan.md exists yet.** Produces the Maven scaffold, exception classes,
`BeanConfigurationSagas.groovy` (infrastructure beans only — no domain beans yet), and Spock test
base classes, all cloned and transformed from the `quizzes` reference app.

The full procedure — exact files read, every transformation applied, and the complete produced-file
list — is authoritatively defined in
[`.claude/skills/boot-strap/SKILL.md`](../.claude/skills/boot-strap/SKILL.md). Invoke it with
`/boot-strap <app-name>`.

### Does not update
plan.md does not exist yet. Phase 1 creates it.

---

## Phase 1 — Classify & Plan

**One session. plan.md does not exist yet.**

### Reads
- `{App}-domain-model.md` — all sections
- `{App}-aggregate-grouping.md` — all sections
- `docs/concepts/rule-enforcement-patterns.md` — the pattern taxonomy and classification flowchart

### Produces
`applications/{app-name}/plan.md` using the structure defined in
`.claude/skills/classify-and-plan/SKILL.md` (see **plan.md — The Job Queue** above). The agent
must:

1. Apply the decision guide to every §3.2 rule → populate the Rule Classification table.
2. Topological-sort aggregates by the dependency DAG (§3 of aggregate-grouping) → the
   Implementation Order table. Aggregates with no upstream deps come first.
3. For each aggregate in order, fill the Aggregate Details section: write/read functionalities
   (split from §4 of domain-model), events published/subscribed (from aggregate-grouping §4),
   cross-aggregate prerequisites (P4a rules and P3 DTO-check rules) with their step names, and the full file list per session.
4. Set the `d` session checkbox only for aggregates that have a non-empty Events subscribed list.
5. Populate the Phase 3 section: one session per aggregate in the same order as the
   Implementation Order table. No per-scenario analysis needed.

### Does not modify
Any source file. Output is plan.md only.

---

## Phase 2 — Aggregate Implementation

**Loop: repeat sessions a → b → c → d for each aggregate in plan.md order.**

Each session agent follows these steps:
1. Open `plan.md`. Find the first unchecked session for the current aggregate.
2. Read only the docs listed for that session type (below).
3. Produce the files listed in the aggregate's file table in plan.md.
4. Update `BeanConfigurationSagas.groovy` (see per-session instructions).
5. Tick the checkbox in plan.md before finishing.

---

Each session type's exact reads, produced files, and `BeanConfigurationSagas.groovy` updates are
authoritatively defined in its sub-file under `.claude/skills/implement-aggregate/` — that sub-file
is what an agent actually executes, so it owns the detail. This table is a one-line orientation
only:

| Session | Name | Sub-file | Adds to BeanConfigurationSagas.groovy |
|---------|------|----------|----------------------------------------|
| 2.N.a | Domain Layer | [`session-a.md`](../.claude/skills/implement-aggregate/session-a.md) | `Sagas{Aggregate}Factory`, `{Aggregate}CustomRepositorySagas` |
| 2.N.b | Write Functionalities | [`session-b.md`](../.claude/skills/implement-aggregate/session-b.md) | `{Aggregate}Service`, `{Aggregate}CommandHandler`, `{Aggregate}Functionalities` |
| 2.N.c | Read Functionalities | [`session-c.md`](../.claude/skills/implement-aggregate/session-c.md) | none — `{Op}FunctionalitySagas` are per-request objects, not Spring beans |
| 2.N.d | Event Wiring *(only if aggregate has subscribed events)* | [`session-d.md`](../.claude/skills/implement-aggregate/session-d.md) | `{Aggregate}EventHandling`, `{Aggregate}EventHandler`, `{Aggregate}EventProcessing` |

---

## Phase 3 — Test Review

**One session per aggregate in plan.md's Implementation Order.**

### Reads
- `plan.md` — Phase 3 session row being processed (aggregate name and ordinal)
- (no other reads required — the review-tests skill reads all necessary files internally)

### Produces (per session)
- `applications/{app-name}/reviews/test-review-{Aggregate}.md` — audit report
- Fixes to existing `*.groovy` test files (fake/wrong/weak tests corrected; missing tests added)

### How to run

Invoke the review-tests skill:

```
/review-tests {Aggregate}
```

The skill reads all test files across the four tiers (T1–T4) and the implementation files for the aggregate, audits completeness,
adds missing tests, fixes fake/wrong tests, runs the full test suite, and writes the review report.

**Ticks:** `- [ ] 3.{N} — {Aggregate}` in plan.md upon completion.

---

## Post-Session Retrospective

**Generated automatically by `/implement-aggregate` as Step 7.**

No separate invocation required. After all session files are produced and the plan.md checkbox is
ticked, `/implement-aggregate` synthesises the retro from conversation context and writes it to:

`applications/{app-name}/retros/retro-{session-id}-{Aggregate}.md`

Example: `applications/{app-name}/retros/retro-2.3.b-Tournament.md`

A single commit covering both the implementation files and the retro file is then issued
automatically (Step 8), with message: `feat({app-name}): 2.{N}{type} ({Aggregate} {session-type-name})`.

### What it produces

| Section | Purpose |
|---------|---------|
| Files Produced | Audit trail of what was shipped |
| Docs Consulted | Which concept docs were read and whether they were sufficient |
| Reference App Consulted | Files read from `applications/quizzes/` — each entry is a gap signal |
| Skill Instructions Feedback | What worked / what was unclear in the skill sub-file |
| Documentation Gaps | Specific missing or ambiguous content in `docs/concepts/` |
| Patterns to Capture | Undocumented patterns discovered during implementation |
| Action Items | Prioritised list of improvements to make to docs and skills |
| One-Line Summary | The single most important finding |
