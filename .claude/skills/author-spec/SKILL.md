---
name: author-spec
description: Author the spec pair a run starts from - {app-name}-domain-model.md and {app-name}-aggregate-grouping.md - by interviewing the human relentlessly about the application being modelled. Produces the two files only; it does not bootstrap, plan or implement. Invoke with /author-spec <pointer to the application being modelled>.
argument-hint: "<pointer to the application being modelled: a repo path, a URL, a paper, or a description>"
---

# Author the Spec Pair

The harness generates an application from a **spec pair**:

| File | Owns |
|------|------|
| `applications/{app-name}/{app-name}-domain-model.md` | Entities, relationships, rules (§3.1 single-entity, §3.2 cross-entity), functionalities (§4) |
| `applications/{app-name}/{app-name}-aggregate-grouping.md` | Aggregate partitioning (§1), snapshots (§2), the event DAG (§3), events (§4) |

The spec pair is the **input** to a run, not part of the harness (`AGENTS.md` § "What the harness
is"). Every later phase treats it as given: `/classify-and-plan` will not question an aggregate
boundary, and no Phase 2 session will add a functionality §4 omitted. Whatever is wrong here is
wrong for the whole run, which is why this skill is an interview and not a generator.

## What this skill will do to you

**It will challenge your premises, not just fill in your form.** Expect to be asked why an entity is
an entity, why two things that always change together live in different aggregates, and what happens
to a rule when the two sides of it are eventually consistent. When your answer implies something you
have not said, you will be told what it implies and asked whether you meant it. A spec that survives
this is worth more than a spec that was transcribed politely.

Interview stance adapted from the `grill-me` / `grilling` skills, which this skill is a
domain-specific instance of.

**The decisions are yours.** The templates say the spec pair is authored by the domain expert, and
that is still true: this skill asks, recommends and argues, and you decide. It never picks an
aggregate boundary on your behalf.

---

## Step 0: Anchor to the repository root

Read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository root". Do not run
any command until you have.

## Step 1: Load the shapes

Read, in full, before asking anything:

1. `docs/templates/domain-model-template.md` - the authority on every section, table column and block
   shape of the domain model, including the annotations (`immutable`, `technical`, `default:`) that
   later phases depend on.
2. `docs/templates/aggregate-grouping-template.md` - the same for the grouping file, including the
   anchor-field rule for events.
3. `docs/concepts/rule-enforcement-patterns.md` - not to classify anything here (that is
   `/classify-and-plan`'s job) but so the questions you ask about a §3.2 rule are the ones its
   classification will turn on.

**The worked example.** `applications/trainticket/` holds a completed spec pair -
`trainticket-domain-model.md`, `trainticket-aggregate-grouping.md`, and a
`trainticket-design-rationale.md` recording why the boundaries fell where they did. Read them for
**shape and depth**: how precise a §3.2 predicate has to be, how much a §4 row carries, how the §3
arrows and the §4 events line up. It is a finished run, so reading it here is a worked example rather
than a peer-as-specification read (`.claude/skills/_shared/conventions.md` § "Application isolation",
§ "Neutral domain"). Its **content** is not a source: never carry a trainticket entity, rule or
boundary into the spec you are authoring because it looks similar.

## Step 2: Understand what is being modelled

The argument points at the application being modelled: a repository path, a URL, a paper, or a prose
description. Read it, or ask for it if no argument was given.

**Look facts up; ask only for decisions.** If the source application is on disk, its schema, its
endpoints and its constraints are facts - read them rather than making the human recite them. What
you cannot read is the intent: which invariants actually matter, which are incidental, and where the
boundaries belong. Those are the interview.

Then agree `{app-name}` (kebab-case) before writing anything, since both filenames derive from it.

## Step 3: The interview

**Ask one question at a time and wait for the answer.** Several questions at once is bewildering and
produces thin answers to all of them. **Give your recommended answer with each question**, and the
reason for it, so the human is reacting to a proposal rather than facing a blank.

Walk the design tree in dependency order, resolving each decision before the ones that rest on it.
The order below is that dependency order, and it is also the order the two files are read in later:

1. **Entities and attributes** (§1). For each field: its type, whether it is immutable, whether it is
   technical rather than domain, its default. Immutability is not a detail - it decides whether a
   consumer needs an event subscription at all (grouping §2).
2. **Relationships and cardinalities** (§2), including conditional immutability.
3. **Single-entity rules** (§3.1). Every one becomes an intra-invariant, so a rule stated here that
   inspects a second entity is in the wrong section; catch that now.
4. **Cross-entity rules** (§3.2), one block per rule, predicate stated precisely enough to be
   implemented without further interpretation. **This is where to push hardest.** Ask what the rule
   should do when the two entities are in different aggregates and one side is stale, because that
   question is the whole of §3.2.
5. **Functionalities** (§4), writes and reads alike. §4 is the **complete** inventory: an operation
   absent from it is an operation that will never be implemented. Ask explicitly what is missing, by
   walking the source application's surface rather than waiting for the human to remember.
6. **Aggregate grouping** (grouping §1). Which entities co-locate. Every §3.2 rule whose entities
   land in different aggregates just became eventually consistent; say so, name the rules, and
   confirm the human accepts that consequence for each. This is the single highest-leverage decision
   in the pair.
7. **Snapshots** (grouping §2), then the **event DAG** (§3), then the **events** (§4). These three
   are derived from the boundary decision and must agree with each other: every arrow in §3 needs its
   event rows in §4 unless the cached fields are immutable, every §4 consumer needs its arrow, and
   every event payload needs its anchor field.

Where an answer contradicts an earlier one, say which two answers conflict and ask which stands. Do
not silently revise the earlier one.

## Step 4: Write the pair

Only after the human confirms the interview is finished. Write both files under
`applications/{app-name}/`, using the two templates exactly: the section numbers, table columns and
block shapes are the contract every later phase parses, so keep them intact and drop only rows that
do not apply.

Then re-read what you wrote against the human's answers and report any section you had to fill with
an inference rather than an answer. Those are the spec's weak points and the human should see them
listed.

## Step 5: Hand off

Report the two paths and say what comes next, in order:

1. `/boot-strap {app-name}` - Phase 0. It creates the scaffold and `harness-log.md`, and it is where
   the run's self-healing mode is declared (`AGENTS.md` § "Harness evolution").
2. `/classify-and-plan applications/{app-name}/{app-name}-domain-model.md applications/{app-name}/{app-name}-aggregate-grouping.md` - Phase 1.

This skill runs neither of them.

---

## Hard rules

1. **Writes exactly two files**, both under `applications/{app-name}/`. It creates no scaffold, no
   `plan.md`, no `harness-log.md` and no source, and it edits nothing under `docs/`, `.claude/` or
   `simulator/`.
2. **Never invent a domain decision.** An unanswered question is asked again, not resolved by
   picking the likely answer. A recommendation is a proposal for the human, never a default that
   applies by silence.
3. **Never import content from another application**, including the trainticket worked example. Shape
   yes, content no.
4. **One question at a time**, each with a recommended answer and its reason.
5. **Facts are looked up, decisions are asked.** Reciting a schema the human can point you at wastes
   the interview on the part that needed no human.
