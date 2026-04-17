---
name: scaffold-aggregate
description: Phase 2 driver — fully scaffold one aggregate: create all files, add snapshot fields, add all intra-invariants, register in BeanConfigurationSagas, run creation test. Reads details from plan.md and the aggregate-grouping template. Arguments: "<AggregateName>"
argument-hint: "<AggregateName>"
---

# Scaffold Aggregate: $ARGUMENTS

You are completing all Phase 2 work for the aggregate named in `$ARGUMENTS`. Do all five steps
in order. Tick the corresponding checkbox in `plan.md` immediately after each step succeeds.
Do not stop early.

---

## Step 0 — Gather context

Read before writing any code:

1. `plan.md` — find the `### <AggregateName>` section. Extract:
   - The snapshot fields bullet (fields to cache and which events trigger them)
   - The intra-invariants bullet (list of rule names to add)
2. The aggregate-grouping template (`*-aggregate-grouping.md`) — find §2 for this aggregate.
   Authoritative source for snapshot field names, types, and event triggers.
3. The domain model template (`*-domain-model.md`) — §3.1 and §3.2 rules for this aggregate,
   needed to understand invariant semantics.

---

## Step 1 — Create aggregate files

Read `.claude/skills/scaffold-aggregate/create-aggregate.md` and follow every step in it for
`$ARGUMENTS`. This produces the base class, SagaXxx, factories, repositories,
service stub, and command handler stub.

Tick in `plan.md`:
- [x] Scaffolded (`/scaffold-aggregate <AggregateName>` — Step 1)

---

## Step 2 — Add snapshot fields

From §2 of the aggregate-grouping template, declare each snapshot field in the aggregate class
below the aggregate's own fields, grouped under a `// --- Snapshot fields ---` comment:

- Field type follows from the domain (e.g., `Long courseId`, `String courseName`)
- For per-entity caches (e.g., per-student data), use a `Map<Integer, ...>` keyed by entity ID;
  use the quizzes `Execution` aggregate as a reference
- Do **not** wire event subscriptions yet — that is Phase 4 (`/wire-event`)
- Initialise to safe defaults (`null` / `0` / empty collection)
- Copy all snapshot fields in the copy constructor

If the aggregate has no snapshot fields (e.g., a root publisher like Course or User), skip.

Tick in `plan.md`:
- [x] Snapshot fields added

---

## Step 3 — Add intra-invariants

Read `.claude/skills/scaffold-aggregate/add-invariant.md`. For each rule name listed in the
plan.md intra-invariants bullet for this aggregate, follow the instructions in that file once
per rule. Derive each rule's meaning from its name and the domain model template.

After all rules are added:

Tick in `plan.md`:
- [x] Layer 1 intra-invariants added: <list rule names>

---

## Step 4 — Register in BeanConfigurationSagas

Open `src/test/groovy/.../BeanConfigurationSagas.groovy` and add three beans for this aggregate,
following the style of existing declarations in the file:

```groovy
@Bean
<AggregateName>Repository <aggregateName>Repository() { new Sagas<AggregateName>Repository() }

@Bean
<AggregateName>Factory <aggregateName>Factory(<AggregateName>Repository repo) {
    new Sagas<AggregateName>Factory(repo)
}

@Bean
<AggregateName>Service <aggregateName>Service(
        SagaUnitOfWorkService uowService,
        <AggregateName>Repository repo,
        <AggregateName>Factory factory) {
    new <AggregateName>Service(uowService, repo, factory)
}
```

Tick in `plan.md`:
- [x] Registered in `BeanConfigurationSagas.groovy`

---

## Step 5 — Run the creation test

Write the T1 creation test at `src/test/groovy/.../sagas/<aggregate>/<AggregateName>Test.groovy`.
Follow the T1 template in `docs/concepts/testing.md`: one happy-path case + one failing case per
intra-invariant added in Step 3.

Then run:
```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<AggregateName>Test
```

Diagnose and fix any failures before ticking. Do not move on until the test is green.

Tick in `plan.md`:
- [x] Creation test passes (`<AggregateName>Test`)

---

## Done

All five sub-steps are checked off. Report:
- Aggregate scaffolded: list files created
- Snapshot fields: list field names (or "none")
- Intra-invariants: list rule names added
- Registered in BeanConfigurationSagas
- Creation test: green
