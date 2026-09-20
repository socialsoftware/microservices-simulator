---
name: author-spec
description: Author the spec pair a run starts from - {app-name}-domain-model.md and {app-name}-aggregate-grouping.md - by interviewing the human relentlessly about the application being modelled. Produces the two files only; it does not bootstrap, plan or implement. Invoke with /author-spec <pointer to the application being modelled>.
argument-hint: "<pointer to the application being modelled: a repo path, a URL, a paper, or a description>"
---

# Author the Spec Pair

The harness generates an application from a **spec pair**:

| File | Owns |
|------|------|
| `applications/{app-name}/{app-name}-domain-model.md` — **the plain domain** | Entities (§1), relationships including composition (§2), rules as standing invariants (§3.1 single-entity, §3.2 cross-entity), functionalities with Primary Entity / Other Entities (§4) |
| `applications/{app-name}/{app-name}-aggregate-grouping.md` | Aggregate partitioning and snapshot value objects (§1), snapshots (§2), technical fields (§2.b), the event DAG and the consistency policy (§3), events (§4), rule realisation and cross-file notes (§5) |

**One plain domain, N aggregate groupings.** The domain model describes the domain and nothing else;
the grouping file holds every consequence of one decomposition. Several grouping files may exist over
one domain model, and writing a second one must require **zero** edits to it. That invariant is the
premise of this whole interview, and it has a test:

> **If answering a question would change the domain model, the question was asked in the wrong
> interview.**

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
   shape of the plain domain, including the annotations (`immutable`, `default:`) that later phases
   depend on, and its "What this file must never say" block.
2. `docs/templates/aggregate-grouping-template.md` - the same for the grouping file, including the
   anchor-field rule for events.
3. `docs/concepts/rule-enforcement-patterns.md` - not to classify anything here (that is
   `/classify-and-plan`'s job) but so the questions you ask about a §3.2 rule are the ones its
   classification will turn on.

**The worked example.** `applications/trainticket/` holds a completed spec pair, re-partitioned
onto the plain-domain contract -
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

## Step 3: The interview, part A

**Ask one question at a time and wait for the answer.** Several questions at once is bewildering and
produces thin answers to all of them. **Give your recommended answer with each question**, and the
reason for it, so the human is reacting to a proposal rather than facing a blank.

The interview runs in **two parts with a hard boundary between them**. Interview A produces a
complete plain domain that mentions no aggregate. Interview B opens only after the human has signed
off on A and the self-audit in Step 3.5 has run. The boundary is not a formality: it is what makes
the "one domain, N groupings" invariant true by construction rather than by inspection afterwards.

### Interview A — the plain domain

No question in this part may be about aggregates, co-location, snapshots, caching, events, sagas or
enforcement patterns. If the human raises one, note it for Interview B and carry on.

1. **Entities and attributes** (§1). For each field: its type, whether it is immutable, its default.
   Immutability is not a detail - it decides whether a consumer needs an event subscription at all,
   which Interview B will turn on.
   - **An attribute that duplicates another entity's attribute is not an attribute.** It is a copy,
     which is a grouping decision. Model the relationship instead.
   - **An attribute of a pairing is an associative entity.** If a value belongs to neither end of a
     relationship but to the relationship itself (an enrolment time, a position in a sequence), it
     gets its own entity row and two `N → 1` relationships. Do not park it on whichever end looks
     convenient.
   - **A field that exists for implementation reasons is not a domain attribute.** Note it for
     Interview B's §2.b and leave it out here.
2. **Relationships and cardinalities** (§2), including conditional immutability and the
   **Composition** column. Ask, for each relationship: can the referencing thing exist on its own?
   Composition is a domain fact and it *constrains* a grouping; it does not decide one, and an
   answer of "yes" is not an answer about aggregates.
3. **Single-entity rules** (§3.1). Strictly one **entity**'s own fields. A rule that inspects a
   second entity — including an associative entity you just introduced — is a §3.2 rule, however
   local it will turn out to be once the grouping is chosen.
4. **Cross-entity rules** (§3.2), one block per rule, predicate stated precisely enough to be
   implemented without further interpretation. **This is where to push hardest.**
   - State every rule as a **standing invariant over the domain**: something that is true or false
     of a domain state, in domain vocabulary. `Order.trip has not been removed`, never
     "`Order.tripAggregateId` named a Trip that was ACTIVE when the operation ran".
   - When the human answers with a *when* ("we check it at booking time"), that is the realisation
     and it belongs to Interview B. Write down what they said, then ask again for the invariant it
     is a realisation of.
   - When a rule turns out to say only that two copies of one thing must agree, it is not a domain
     rule at all. It is a coherence obligation one grouping creates, and it goes to grouping §5.
5. **Functionalities** (§4), writes and reads alike, with **Primary Entity** and **Other Entities**.
   §4 is the **complete** inventory: an operation absent from it is an operation that will never be
   implemented. Ask explicitly what is missing, by walking the source application's surface rather
   than waiting for the human to remember. Do not name aggregates in either column and do not
   describe saga steps in the description; `/classify-and-plan` derives the aggregate mapping by
   joining these columns against grouping §1.

Where an answer contradicts an earlier one, say which two answers conflict and ask which stands. Do
not silently revise the earlier one.

## Step 3.5: Write the plain domain, then self-audit it

Write `applications/{app-name}/{app-name}-domain-model.md` now, at the end of Interview A — not at
the end of both interviews. Use the domain-model template exactly: the section numbers, table
columns and block shapes are the contract every later phase parses.

Then run the plain-domain contamination check over it — the one `/review-artifacts` applies to every
`applications/*/*-domain-model.md` — and **report every hit to the human**, one line each, before
Interview B opens. A hit is either a word to rewrite or a fact to move to the grouping file; it is
never something to leave and explain. Report also any section you had to fill with an inference
rather than an answer.

Ask for sign-off on the plain domain before continuing. Interview B does not open until it is given.

## Step 4: Interview B — the aggregate grouping

Only after sign-off on the plain domain. Everything in this part is a consequence of one
decomposition, and **nothing decided here may send you back to edit the domain model**. If it seems
to, that is the signal that Interview A missed a domain question: say so explicitly, reopen A for
that one question, and re-run the self-audit.

1. **Aggregate grouping** (grouping §1). Which entities co-locate. Every §3.2 rule whose entities
   land in different aggregates just became eventually consistent, or a precondition nothing
   restores; say so, name the rules, and confirm the human accepts that consequence for each. This
   is the single highest-leverage decision in the pair. Record any class this decomposition
   introduces that is not a domain entity in the **Snapshot value objects** column.
2. **Snapshots** (grouping §2) and **technical fields** (§2.b) — including every field Interview A
   set aside as non-domain, with the reason it is needed.
3. **The consistency policy** (§3.a): does this grouping cascade or not, and what does that do to
   each family of §3.2 rule? This subsection is required.
4. **The event DAG** (§3) and the **events** (§4). These must agree with each other and with §2:
   every arrow in §3 needs its event rows in §4 unless the cached fields are immutable, every §4
   consumer needs its arrow, and every event payload needs its anchor field.
5. **Rule realisation** (§5): one row per plain-domain §3.2 rule, valued `intra`, `precondition` or
   `eventual`. Do not write pattern names here — `/classify-and-plan` assigns P1–P4 from this table,
   and pre-empting it in the spec is how a classification stops being derived. Then the cross-file
   notes: the snapshot-coherence obligations, and anything else that is a consequence of this
   decomposition rather than of the domain.

Write `applications/{app-name}/{app-name}-aggregate-grouping.md`, then re-read both files together
and report any section you filled by inference.

## Step 5: Hand off

Report the two paths and say what comes next, in order:

1. `/boot-strap {app-name}` - Phase 0. It creates the scaffold and `harness-log.md`, and it is where
   the run's self-healing mode is declared (`AGENTS.md` § "Harness evolution").
2. `/classify-and-plan applications/{app-name}/{app-name}-domain-model.md applications/{app-name}/{app-name}-aggregate-grouping.md` - Phase 1.

This skill runs neither of them.

---

## Hard rules

1. **Writes exactly two files**, both under `applications/{app-name}/` — the plain domain at the
   end of Interview A, the grouping at the end of Interview B. It creates no scaffold, no
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
6. **Interview B never edits the plain domain.** If a grouping question appears to require an edit
   there, stop, name the domain question Interview A missed, ask it, and re-run the self-audit. A
   silent edit destroys the one property the split exists to give.
