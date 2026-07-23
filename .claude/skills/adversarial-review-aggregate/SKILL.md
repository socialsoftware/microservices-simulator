---
name: adversarial-review-aggregate
description: Adversarial deep review of one aggregate implementation. Assumes the code is wrong until a traced path proves otherwise, and must prove every defect with a test that actually fails. Checks P1 invariant falsification, copy-on-write aliasing, saga lock-lifecycle reachability, R1-R8 architectural conformance, event payload correctness, and re-derives rule classification independently of plan.md. Writes applications/{app-name}/reviews/adversarial-review-{Aggregate}.md plus proof tests. Structure and file inventory are out of scope - that is /review-aggregate. Invoke with /adversarial-review-aggregate <AggregateName>.
argument-hint: "<AggregateName> (e.g. Course, Execution, Tournament)"
---

# Adversarial Review — Aggregate

Phase 3, second half. An adversarial pass over one aggregate's implementation, run after
`/review-aggregate` has confirmed the structure is sound.

**Stance: assume the implementation is wrong until a traced code path proves otherwise.** This is the
inverse of `/review-aggregate`, which assumes conformance and looks for deviations from it. That skill
answers *does it exist and is it shaped right*; this one answers *does it do the right thing*.

**Every defect must be proved by a test that was written, run, and observed to fail.** A candidate you
cannot make fail is not a defect. Finding nothing is a valid and expected outcome on a well-implemented
aggregate.

One aggregate per invocation. Reads files directly from disk.

**See also:**
- `.claude/skills/review-aggregate/SKILL.md` — structural review; owns file inventory, annotations,
  package placement. Runs first in the same phase.
- `.claude/skills/review-tests/SKILL.md` — Phase 4; absorbs this skill's proof tests into the T1-T4
  taxonomy once the underlying defects are fixed.

---

## Out of Scope

Do not report any of the following. They belong to `/review-aggregate` and duplicating them here
buries the semantic findings this skill exists to produce:

- Missing or extra files, wrong package placement
- Missing annotations (`@Entity`, `@Service`, `@Component`, `@Transactional`, `@Profile("sagas")`)
- Naming deviations
- Missing test *files* or missing T1-T4 scenarios (that is `/review-tests`)
- Anything whose fix is "add the file" or "add the annotation"

If the structural review has not been run for this aggregate (no `{review-dir}review-{Aggregate}.md`
exists), say so in the summary and continue anyway — but do not start reporting structural findings.

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Resolve Context

Read `.claude/skills/_shared/conventions.md` § "Resolve app context" and § "Resolve aggregate
context". Derive `{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{aggregate}`, `{N}`,
`{tgt-src}`, `{tgt-test}`, `{review-dir}`.

Additionally derive:

```
{proof-test-dir}  = {tgt-test}sagas/adversarial/{aggregate}/
{proof-test}      = {proof-test-dir}{Aggregate}AdversarialTest.groovy
{report}          = {review-dir}adversarial-review-{Aggregate}.md
```

---

## Step 2: Read Context Files

Read in a single parallel batch. Absent files are noted, not errors.

### 2.a — Implementation (the thing under attack)

- Everything under `{tgt-src}microservices/{aggregate}/` — enumerate with `find`, read all
- `find {tgt-src} -path "*/commands/{aggregate}/*" -name "*.java"` — read all
- `{tgt-src}microservices/exception/{AppClass}ErrorMessage.java` — the error-message constants that
  invariant assertions must match

### 2.b — Spec

- The full `### {N}. {Aggregate}` section of `plan.md`
- The `## Rule Classification` section of `plan.md` (§3.1 and §3.2 tables), filtered to rows touching
  this aggregate
- `applications/{app-name}/{app-name}-domain-model.md` — §3.1/§3.2 rules and §4 functionalities, in
  the author's own words. Family F compares this against plan.md's classification.
- `applications/{app-name}/{app-name}-aggregate-grouping.md` — §3 Upstream / Downstream Event
  Dependencies (the event DAG), §4 events. §3 is the authority for the R5 and R8 upstream tests in
  Step 6 — read it as a DAG, not as a list.

### 2.c — Consumers of this aggregate's events (for Family E)

If this aggregate publishes events:
`grep -rln "{Aggregate}" {tgt-src}*/notification/subscribe/` and read every hit, plus the
corresponding `*EventHandling.java`. The payload contract is only checkable against its consumers.

### 2.d — Docs (the standard being enforced)

- `docs/architecture.md` § Architectural Restrictions (R1-R8) — Family D
- `docs/concepts/service.md` § Copy-on-Write Rule, § Method Patterns, § P3 Guard Placement
- `docs/concepts/sagas.md` § Semantic-lock release on abort is automatic, § R4 Decision Table,
  § Step Ordering, § Write Workflow Structure
- `docs/concepts/rule-enforcement-patterns.md` § Decision Guide — Family F
- `docs/concepts/events.md` — only if this aggregate publishes or subscribes
- `docs/concepts/testing.md` § T1 / § T2 — for writing proof tests that compile and wire correctly

### 2.e — Do NOT read yet

**`{review-dir}review-{Aggregate}.md` must not be read until Step 11.** Reading the structural
review's verdict before forming your own anchors you to it and defeats the purpose of an independent
pass. Likewise do not read `{review-dir}test-review-{Aggregate}.md`.

---

## Step 3: Family A — Invariant Falsification (P1)

For every P1 rule on this aggregate that is **not** a Java-`final` field rule (final fields are
enforced by the compiler and need no attack):

1. State the rule's predicate as the domain model words it.
2. Locate the corresponding check in `verifyInvariants()`. Quote it.
3. Construct the concrete state that violates the rule — specific field values, not "an invalid X".
4. Trace from the service method that can produce that state, through `registerChanged`, to
   `verifyInvariants()`. Answer three questions:
   - **Is the check reached?** A check on a field assigned *after* the check runs, or inside a branch
     the mutation path never enters, is dead.
   - **Does the predicate have the right polarity?** Read it literally. `if (x > limit) throw` versus
     `if (x >= limit) throw` versus an inverted comparison are all shape-identical to a structural
     review.
   - **Does it throw the right error message?** A check that throws a different
     `{AppClass}ErrorMessage` than the rule names is a real defect: T1 tests assert on the constant,
     and the wrong constant means the wrong rule is reported to the caller.
5. For ordered predicates (counts, sizes, timestamps), evaluate the on-point and the off-point
   explicitly. Off-by-one is the single most common defect in this family.

| Rule | Predicate | Check in `verifyInvariants()` | Violating state | Reached? | Polarity | Error msg | Verdict |
|------|-----------|-------------------------------|-----------------|----------|----------|-----------|---------|

Verdict: `Sound` / `Candidate defect`. Every `Candidate defect` goes to Step 9.

---

## Step 4: Family B — Copy-on-Write Aliasing

`/review-aggregate` verifies that a factory copy is created. It does not verify the copy is
independent. A shallow copy means a mutation applied to the copy is visible on the original within the
same UnitOfWork, which silently defeats the whole copy-on-write design
(`docs/concepts/service.md` § Copy-on-Write Rule).

For each mutable field on `{Aggregate}` — every collection, every owned sub-entity, every mutable
value object:

1. Read the copy constructor `{Aggregate}({Aggregate} other)` and the `Saga{Aggregate}` copy
   constructor.
2. Determine whether the field is deep-copied, or the reference is assigned across.
3. A collection assigned by reference (`this.items = other.getItems()`) is a candidate defect. A
   collection copied but whose *elements* are shared references is also a candidate defect when those
   elements are mutable and any service method mutates them in place.

| Field | Type | Mutable? | Copy mechanism (quoted) | Mutated in place by | Verdict |
|-------|------|----------|-------------------------|---------------------|---------|

Also check: does any service method mutate a field on the aggregate returned by
`aggregateLoadAndRegisterRead` before creating the copy? That mutates the read-registered instance and
is a candidate defect regardless of copy depth.

---

## Step 5: Family C — Saga Lock-Lifecycle Reachability

For each write functionality on this aggregate that acquires a semantic lock:

1. Enumerate every step in `buildWorkflow()` in order, marking which call `setSemanticLock`.
2. Enumerate every exit path: normal completion, abort at each step, and each
   `registerCompensation` body.
3. For each exit path, determine the persisted `sagaState` at exit. Every path must end at
   `GenericSagaState.NOT_IN_SAGA`.
4. Cross-check against `docs/concepts/sagas.md` § Semantic-lock release on abort is automatic. A
   `registerCompensation` that manually releases the lock re-locks the aggregate via the
   `currentExecutingStep` pitfall — a candidate defect, and one whose proof test is straightforward
   (inject a fault, assert persisted state). **This check is owned here, not by `/review-aggregate`**,
   because only a fault-injection test can separate a wrongful release from a legitimate
   domain-level undo (e.g. deleting a child aggregate an earlier step created).
5. Check step ordering against `docs/concepts/sagas.md` § Step Ordering: is there an ordering where a
   lock is acquired and a later step's failure leaves it held?

| Functionality | Steps acquiring lock | Exit paths | Path leaving lock held | Verdict |
|---------------|----------------------|------------|------------------------|---------|

---

## Step 6: Family D — R1-R8 Architectural Conformance

`docs/architecture.md` § Architectural Restrictions states these "are not enforced by the compiler"
and that violating them "produces subtle runtime failures". No other skill checks them against code.
Check each against this aggregate's files. Quote the offending line or write "no violation found".

| # | Check |
|---|-------|
| R1 | `aggregateLoadAndRegisterRead(...)` in `{Aggregate}Service` is called only with IDs of `{Aggregate}` itself. Any call taking an ID sourced from another aggregate's DTO is a violation. |
| R2 | `{Aggregate}Service` injects only `{Aggregate}Repository`, `{Aggregate}CustomRepository`, `{Aggregate}Factory`, `UnitOfWorkService`, `AggregateIdGeneratorService`. Any foreign service, repository, or factory is a violation. |
| R3 | No foreign concrete aggregate type (`Saga{Other}`, `{Other}`) appears in any `{Aggregate}Service` signature, field, or local variable. Foreign `{Other}Dto` is allowed. |
| R4 | Every saga step that mutates an aggregate declares `setForbiddenStates(...)`. Cross-check which steps need `SagaCommand` wrapping versus `setForbiddenStates` using `docs/concepts/sagas.md` § R4 Decision Table. |
| R5 | `{Aggregate}.getEventSubscriptions()` subscribes only to events published by aggregates **strictly upstream** of `{Aggregate}` (see § Deciding "upstream" below). A subscription to a downstream aggregate's event is a violation, and so is a subscription to `{Aggregate}`'s **own** events — `docs/architecture.md` R5 forbids the publisher subscribing to itself. |
| R6 | `verifyInvariants()` contains no repository call, no `Optional` lookup, no injected-service call. It may read only fields already on the instance. |
| R7 | No saga step or functionality mutates a DTO received from a `Get*Command`. Look for setter calls on a DTO local. |
| R8 | Every `commandGateway.send(...)` in `coordination/` targets an aggregate **strictly upstream** of `{Aggregate}` (see § Deciding "upstream" below), or `{Aggregate}` itself. |

| Restriction | Status | Evidence (quoted line + file:line) |
|-------------|--------|------------------------------------|

Status: `Conforms` / `Candidate violation` / `N/A`.

### Deciding "upstream" (R5 and R8)

`docs/architecture.md` defines upstream for both R5 and R8 in terms of the **event dependency graph**,
not the implementation order. Use §3 Upstream / Downstream Event Dependencies of
`applications/{app-name}/{app-name}-aggregate-grouping.md`, read in Step 2.b.

`U` is upstream of `{Aggregate}` iff a directed path `U → ... → {Aggregate}` exists in that DAG — i.e.
`{Aggregate}` transitively depends on `U`'s events.

**Do not substitute plan.md's Implementation Order for this.** That table is a topological
linearization of the DAG: every ancestor of `{Aggregate}` precedes it, but *not every predecessor is
an ancestor*. Two aggregates with no dependency edge between them still get an arbitrary relative
order there. Testing against the table is therefore strictly more permissive than the doc, and passes
real R5/R8 violations between unrelated aggregates.

State the path you found (`U → X → {Aggregate}`) as the evidence for a `Conforms` verdict, and the
absence of any path as the evidence for a `Candidate violation`.

R5 and R8 violations are candidate defects but are often not provable by a unit test (they are
structural coupling defects). Where no failing test is possible, record them under **Spec
disagreement** in the report alongside Family F rather than dropping them — and say explicitly that
they are unproven.

---

## Step 7: Family E — Event Payload Correctness

Only if this aggregate publishes events. For each published event type:

1. Read the publishing service method. Determine at which point in the method the payload is
   constructed — before or after the mutation is applied to the copy.
2. Read every consumer's subscription and handler (from Step 2.c). Determine which payload fields the
   consumer's inter-invariant actually uses.
3. A payload built from the pre-mutation instance carries a stale value. The event fires, the consumer
   caches the old value, and the inter-invariant is silently wrong forever. This is invisible to a
   structural review and to any test that only asserts the event *exists*.

| Event | Payload fields | Built from (pre/post-mutation) | Consumer + field used | Verdict |
|-------|----------------|--------------------------------|-----------------------|---------|

Also check: is the event published on every path that changes the subscribed-to state, or only on
some? A mutation path that skips publication starves every consumer.

---

## Step 8: Family F — Spec Doubt (report-only)

`/review-aggregate` treats `plan.md` as authoritative, so a rule misclassified during Phase 1 is
invisible to it permanently. This is the only check that can catch that.

For each rule in the domain model touching this aggregate, re-derive the classification **from
`docs/concepts/rule-enforcement-patterns.md` § Decision Guide and the domain model text alone**.
Form your classification before looking at what `plan.md` assigned, then compare.

| Rule (domain model wording) | Your derivation | plan.md says | Agree? | Consequence if plan.md is wrong |
|-----------------------------|-----------------|--------------|--------|----------------------------------|

Findings here are labelled **Spec disagreement**, not Defect, and are **exempt from the proof-test
requirement** — a misclassification is a design disagreement, not a runnable failure. State the
consequence concretely: e.g. "classified P3 but is P1; the guard is bypassed by any path that does not
go through `createX`, so the invariant is not actually guaranteed".

Also flag: functionalities in the domain model §4 with no counterpart in plan.md's list for this
aggregate, and vice versa.

---

## Step 9: Write Proof Tests

For every `Candidate defect` from Families A-E, write a test that fails against the current code.

Create `{proof-test}` if it does not exist. Structure it like any T1/T2 test
(`docs/concepts/testing.md`): `@DataJpaTest @Transactional @Import(LocalBeanConfiguration)`, inner
`LocalBeanConfiguration extends BeanConfigurationSagas`.

```groovy
package pt.ulisboa.tecnico.socialsoftware.{pkg}.sagas.adversarial.{aggregate}

import spock.lang.PendingFeature   // spock-core 2.4-M6; verified present. Unused until Step 10.c.

@DataJpaTest
@Transactional
@Import({Aggregate}AdversarialTest.LocalBeanConfiguration)
class {Aggregate}AdversarialTest extends {AppClass}SpockTest {

    // Finding A1 — <one-line defect statement>
    // Expected per <source doc/rule>: <what should happen>
    // Actual: <what happens>
    // No @PendingFeature yet — Step 10.c adds it, after the failure has been observed.
    def "A1: <scenario>"() {
        ...
    }

    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
```

Rules for proof tests:

- **One test per finding**, named with the finding ID (`A1`, `B2`, `C1`, ...) so the report and the
  test file cross-reference cleanly.
- **The test must assert the correct behaviour**, so that it fails now and passes once fixed. Do not
  write a test that asserts the buggy behaviour.
- Assert on `{AppClass}ErrorMessage` constants, never on a bare exception type — a bare
  `thrown({AppClass}Exception)` passes on any unrelated failure and proves nothing.
- Do **not** add `@PendingFeature` yet. Step 10 adds it, after the failure has been observed.

---

## Step 10: Run, Prune, Quarantine

### 10.a — Run and observe

Follow `.claude/skills/_shared/conventions.md` § "Run the test suite", narrowing with
`-Dtest="{Aggregate}AdversarialTest"`. Do not pipe maven through `tail` — the exit code and the
`Tests run:` lines do not survive it.

Record the verbatim failure output for each test, taken from the surefire report body the shared
block's script prints.

### 10.b — Prune

**A test that passes disproves its finding.** Delete the test method and drop the finding entirely. Do
not downgrade it to "possible issue", do not carry it into the report as a caveat, do not mention it.
The whole point of the proof requirement is that unprovable suspicions cost nothing to emit and are
therefore worthless.

A test that fails to *compile* proves nothing either. Fix the test; if it cannot be made to compile
because the scenario it describes is not expressible, the finding is dropped.

### 10.c — Quarantine

Add `@PendingFeature` to each surviving test. Spock inverts the semantics: the test is expected to
fail, so the suite stays green while the defect is open, and goes **red the moment the defect is
fixed** — which is the signal to remove the annotation and promote the test into its proper T1-T4 tier
(`/review-tests` does this in Phase 4).

Re-run the **full** suite to confirm, again per `.claude/skills/_shared/conventions.md` § "Run the
test suite" (no `-Dtest=` narrowing this time).

The run must come back green — `MAVEN_EXIT=0` **and** `failures=0 errors=0` — before you finish. If it
does not, either a `@PendingFeature` is missing, a `@PendingFeature` test unexpectedly passed (which
means its finding should have been pruned in 10.b), or a proof test has broken unrelated state. Fix it
before writing the report.

Record the observed `MAVEN_EXIT` and the totals — Step 12's Build Result section reports these
numbers, not a `BUILD SUCCESS` string scraped from stdout.

If there are zero surviving findings, delete `{proof-test}` and the `{proof-test-dir}` directory
entirely. Do not leave an empty test class behind.

---

## Step 11: Read the Structural Review and Tag Overlap

**Now** read `{review-dir}review-{Aggregate}.md` (and `test-review-{Aggregate}.md` if present).

For each surviving finding, tag it:
- **New** — not mentioned in the structural review
- **Overlaps** — the structural review raised the same underlying issue (cite its action-item row)
- **Contradicts** — the structural review marked this specific thing `Correct`

`Contradicts` findings are the most valuable output of this skill: they are exactly the cases where a
shape check passed something semantically wrong. Call them out in the summary.

---

## Step 12: Write the Report

Write `{report}`. Create `{review-dir}` if needed. Never omit a section — write "nothing to report".

```markdown
# Adversarial Review — {Aggregate}

**App:** {app-name}
**Aggregate:** {Aggregate} (aggregate #{N} in plan.md)
**Date:** {today}
**Verdict:** Green | Yellow | Red
**Proof tests:** {count} written, {count} confirmed failing, {count} pruned

> **Green** = no confirmed defects. Spec disagreements may still be listed.
> **Yellow** = confirmed defects, none of which can corrupt persisted state or bypass an invariant.
> **Red** = at least one confirmed defect that bypasses an invariant, leaks a lock, corrupts state,
> or starves a consumer.

---

## Summary

(One paragraph. Lead with confirmed defects and any `Contradicts` findings. If nothing was found, say
so plainly and state what was attacked — a clean result is informative only if the attack surface is
named.)

---

## A. Invariant Falsification (P1)

## B. Copy-on-Write Aliasing

## C. Saga Lock-Lifecycle Reachability

## D. R1-R8 Architectural Conformance

## E. Event Payload Correctness

## F. Spec Disagreements (unproven by design)

---

## Proof Tests

| ID | Test name | Defect | Failure output (verbatim) | Overlap tag |
|----|-----------|--------|---------------------------|-------------|

---

## Pruned Candidates

| ID | Hypothesis | Why it was dropped |
|----|-----------|--------------------|

(Candidates whose proof test passed. Listing them is not a caveat list — it is evidence the protocol
ran. These are NOT findings and must not appear in Action Items.)

---

## Build Result

**Command:** `mvn clean -Ptest-sagas test`
**Maven exit status:** {MAVEN_EXIT}
**Surefire totals:** tests={count} failures={count} errors={count} skipped={count}
**Outcome:** GREEN / RED

(Must be GREEN: exit status `0` and zero failures/errors — all confirmed proof tests are
`@PendingFeature`. Report the observed numbers; see `.claude/skills/_shared/conventions.md`
§ "Run the test suite".)

---

## Action Items

| Priority | Family | File | Finding | Proof test | Fix |
|----------|--------|------|---------|-----------|-----|

**Critical** = bypasses an invariant, leaks a semantic lock, or corrupts persisted state.
**Major** = wrong error message, stale event payload, R1-R8 violation.
**Minor** = spec disagreement with no runtime consequence.
```

---

## Step 13: Print Summary to Conversation

1. Absolute path to `{report}`
2. Verdict and one-sentence justification
3. Every confirmed defect, one line each, with its proof-test ID
4. Any `Contradicts` findings called out explicitly
5. Count of pruned candidates
6. Build result — the observed `MAVEN_EXIT` and surefire totals (must be green)

---

## Step 14: Tick plan.md

The `3.{N}` checkbox means **both** halves of Phase 3 are done (`docs/workflow.md` § Phase 3). This
skill is only the second half, so tick conditionally:

- **If `{review-dir}review-{Aggregate}.md` exists** — `/review-aggregate` has run. Replace
  `- [ ] 3.{N} — {Aggregate}` with `- [x] 3.{N} — {Aggregate}` in `applications/{app-name}/plan.md`.
- **If it does not exist** — leave the checkbox unticked. State in the Step 13 summary that `3.{N}`
  was left unticked because the structural half of Phase 3 has not been run, and that
  `/review-aggregate {Aggregate}` must run before the phase is complete.

Never tick on the strength of this skill alone.

---

## Hard Rules

1. **No finding without a failing proof test.** Families A-E only. A candidate whose test passes, or
   cannot be expressed as a test, is dropped — not softened, not caveated, not mentioned in Action
   Items. Families D-R5/R8 and F are the only exceptions and must be labelled unproven.
2. **Zero findings is a valid and expected outcome.** Do not manufacture findings to fill a table. A
   report that says "nothing confirmed, here is what was attacked" is a successful run. Padding the
   report with speculation is a worse failure than missing a defect, because it destroys the signal
   that makes this skill worth running.
3. **Never modify `src/main/**`.** Writes are limited to `{proof-test}` and `{report}` (plus the
   plan.md checkbox). Reporting a defect and fixing it in the same pass removes the human checkpoint.
4. **Structure is out of scope.** See § Out of Scope. Do not report missing files, annotations, or
   naming.
5. **Do not read the structural review before Step 11.** Anchoring control.
6. **Quote everything.** Every candidate defect cites `file:line` and the quoted source. Every
   confirmed defect quotes verbatim test failure output.
7. **The build must be green when you finish** — verified as `MAVEN_EXIT=0` with zero surefire
   failures and errors, per `.claude/skills/_shared/conventions.md` § "Run the test suite". Never
   claim a build result read from piped maven stdout. All confirmed proof tests carry
   `@PendingFeature`.
8. **One aggregate per invocation. No emojis. Terse and specific.**
