---
name: review-tests
description: Phase 3 — Test Review. Audits T1/T2/T3 test completeness for one aggregate, detects fake/weak tests by reading the implementation, adds missing edge cases and adversarial scenarios, and runs the full test suite. T1 = {Aggregate}IntraInvariantTest (full P1 matrix); T2 = service/functionality tests (no P1).
argument-hint: "<AggregateName> (e.g. Course, Execution, Tournament)"
---

# Phase 3 — Test Review

Phase 3 test review for one aggregate. Audits T1/T2/T3 completeness and quality against the
implementation, adds missing tests, fixes fake/wrong tests, and runs the build.

One aggregate per invocation.

**See also:**
- `docs/concepts/testing.md` — canonical T1/T2/T3 specification (this skill enforces it)
- `docs/concepts/testing.md:177-225` — Service-Command Tests (T2 variant, frequently missed)
- `docs/reviews/review-2026-05-23-deferred.md:11-27` — seven concrete examples of the systematic
  step-interleaving gap this skill is designed to catch
- `.claude/skills/implement-aggregate/session-b.md` — companion implementation skill; missing tests
  found here should be fed back to session-b.md so the gap is not re-introduced

---

## Step 1: Resolve Context

### 1.a — Locate plan.md

Run: `find applications -name plan.md`

Use the first result. Extract:
- `{app-name}` = directory containing `plan.md` (e.g., `quizzes-full`)
- `{pkg}` = `{app-name}` with hyphens removed, lowercase (e.g., `quizzesfull`)
- `{AppClass}` = PascalCase of `{app-name}` (e.g., `QuizzesFull`)

If no `plan.md` found, halt: "No plan.md found. Run /classify-and-plan first."

### 1.b — Resolve the aggregate

`{Aggregate}` = PascalCase argument (e.g., `Course`).
`{aggregate}` = lowercase (e.g., `course`).

Verify that a section `### N. {Aggregate}` exists in `plan.md`. If not found, halt:
"Aggregate '{Aggregate}' not found in plan.md. Check the name."

### 1.c — Derive path prefixes

```
{tgt-src}   = applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{tgt-test}  = applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{ref-test}  = applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/
{review-dir} = applications/{app-name}/reviews/
```

---

## Step 2: Read All Relevant Files in Parallel

Read every file in a single parallel batch. Missing files are noted, not errors.

**Test files (scope of this review):**
- `{tgt-test}sagas/{aggregate}/{Aggregate}IntraInvariantTest.groovy` — T1
- `{tgt-test}sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy` — T3 (may not exist)
- All `*.groovy` under `{tgt-test}sagas/coordination/{aggregate}/` — T2 (use `find` to enumerate)

**Implementation files (ground truth):**
- `{tgt-src}microservices/{aggregate}/aggregate/{Aggregate}.java` — invariant logic
- `{tgt-src}microservices/{aggregate}/aggregate/sagas/Saga{Aggregate}.java`
- `{tgt-src}microservices/{aggregate}/service/{Aggregate}Service.java` — service methods, P3 guards
- All `*.java` under `{tgt-src}microservices/{aggregate}/coordination/sagas/` — saga steps, forbidden states, semantic locks (use `find` to enumerate)

**Concept docs:**
- `docs/concepts/testing.md` — T1/T2/T3 required scenarios (canonical checklist)
- `docs/concepts/sagas.md` — semantic lock patterns

**Plan section:**
- The full `### N. {Aggregate}` section from `plan.md` — write/read functionalities, P1/P2/P3 rules, saga steps, subscribed events

---

## Step 3: Build the Expected-Test Inventory

From `docs/concepts/testing.md` and the `plan.md` aggregate section, derive the complete list of required
test scenarios. This is the baseline for Steps 4–7.

> **Spec-first principle:** Build this inventory from `plan.md` and the domain model before consulting implementation files. The implementation is ground truth for *how* tests are wired, not for *what* they should assert. When Step 5 finds that an existing test's assertion matches the implementation but not the spec, the implementation is the bug — not the spec.

### T1 — `{Aggregate}IntraInvariantTest.groovy`

All cases are **direct-aggregate** (construct and/or mutate the aggregate directly, then call `verifyInvariants()`). No saga path.

- One "create with valid data, all fields correct" scenario — every P1 rule passes; assert every field on the aggregate.
- For each non-`final` P1 rule enforced by `verifyInvariants()`: one **violation** case that puts the aggregate into the violating state and asserts `ex.getErrorMessage() == <RULE_CONSTANT>`. Skip `final`-field rules (compiler-enforced; no test needed).
- For each non-`final` P1 rule whose predicate is a **comparison on an ordered domain** (count, timestamp, collection size — `<`, `<=`, `>`, `>=`): two **boundary-straddling** cases:
  - **on-point** — the last value that satisfies the rule → `verifyInvariants()` does **not** throw.
  - **off-point** — the first value that violates the rule → `verifyInvariants()` throws and `ex.getErrorMessage() == <RULE_CONSTANT>`.
  Categorical rules (uniqueness, state-freeze, set-membership) get a single representative — no boundary pair.

Source: `plan.md` §3.1 / §3.2 P1 rule list for this aggregate.

### T2 — per **write** functionality (one test file per write operation under `coordination/{aggregate}/`)

File-naming convention: `{Operation}{Aggregate}Test.groovy` (e.g., `CreateCourseTest.groovy`,
`UpdateQuizTest.groovy`).

For each write functionality:

1. **Happy-path success** — the operation completes without exception; assert all mutated fields.
2. **One scenario per P3 guard** in the corresponding service method (read the service to enumerate these).
3. **Step-interleaving — `setSemanticLock`**: for each saga step that calls `setSemanticLock`, **without
   exception**, one test that pauses the workflow before the **following mutate step** (the step that uses
   the locked aggregate as a foreign target), has a concurrent saga acquire the same lock, resumes, and
   expects `thrown(SimulatorException)`. Every such step must have a matching test — no "appears safe by
   inspection" exceptions. See `docs/reviews/review-2026-05-23-deferred.md` for the recurring gap this
   rule is designed to catch.
4. **Step-interleaving — `setForbiddenStates`**: for each saga step that calls `setForbiddenStates`,
   **without exception**, one test that pauses the workflow **at that step** via `executeUntilStep`, has a
   concurrent saga acquire one of the forbidden states, resumes via `resumeWorkflow`, and expects
   `thrown(SimulatorException)`.

**P1 intra-invariants are not tested here** — they belong exclusively in `{Aggregate}IntraInvariantTest`. A P1 violation scenario in a service/functionality test is a **Wrong (misplaced)** finding.

**Upstream-invariant rule:** Every T2 `setup:` block must establish any upstream prerequisite state
(e.g., a `createExecution` call if `Course.executionCount` must be > 0) *before* creating the aggregate
under test. Read-functionality tests are not exempt. See `docs/concepts/testing.md:96-98`.

### T2 — per **read** functionality

1. **Happy-path success** — returns the expected DTO; assert all semantically important fields.
2. **Not-found case** — the correct exception type depends on which lookup mechanism the service uses.
   **Read the service method first**:
   - **Path A** — service calls `aggregateLoadAndRegisterRead` directly with an ID → infrastructure
     throws `SimulatorException`. Use `thrown(SimulatorException)`.
   - **Path B** — service calls a custom repository returning `Optional` (e.g.,
     `findByQuizIdAndUserId`), then throws on empty → service throws `{App}Exception`. Use
     `thrown({App}Exception)`.
   Rule of thumb from `docs/concepts/testing.md:262`: if the service calls
   `aggregateLoadAndRegisterRead` with an ID, expect `SimulatorException`; if it queries a custom
   repository and throws on empty, expect `{App}Exception`. Flagging a correct Path B test as Fake
   is itself a **Wrong** review finding.

### T2 — per **service-command method**

Some aggregates expose service methods invoked via command handlers from *other* aggregates' sagas
(e.g., `decrementExecutionCount` called when a related aggregate is deleted) but are NOT exposed
through `{Aggregate}Functionalities`. These still call `registerChanged` → `verifyInvariants()`.

**Discovery:** read `{tgt-src}microservices/{aggregate}/service/{Aggregate}Service.java` and identify
public methods that are NOT called from `{Aggregate}Functionalities`. These are typically annotated
`@CommandHandlerMethod` or are called from another aggregate's saga step.

For each such method:
1. **Happy-path success** — state changes as expected; assert the mutated fields.
2. **Floor/ceiling behaviour** — if the method decrements/increments a counter, test the boundary
   (e.g., decrement when already 0 should not produce a negative value).

No step-interleaving is needed (there are no saga steps to interleave).
P1 invariant violations reachable via these methods belong in `{Aggregate}IntraInvariantTest`, not here.
Naming: `{OperationName}Test.groovy` or `{Aggregate}CountsTest.groovy` when related operations share
setup (`docs/concepts/testing.md:191-193`).

### T3 — `{Aggregate}InterInvariantTest.groovy`

Only required if the aggregate has **subscribed events** (listed in `plan.md`):

- For each subscribed event type:
  - "consumer reflects event" — fire the event from the producing aggregate, assert the consuming
    aggregate's state updated correctly.
  - "unrelated entity's event is ignored" — fire the event from a different entity ID, assert no state
    change on the consumer.

---

## Step 4: Completeness Check

For each expected scenario from Step 3, check whether a matching test exists in the test files.

Produce this table:

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name (if exists) | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------------------|-------------------------|

Flag every **No** as a **Missing** finding — these drive Step 7.

For every **Yes** row, complete the "Defect caught if absent" column with one phrase (e.g. "counter goes negative on decrement", "concurrent enrollment not blocked"). A row left blank in that column is a signal the test may be ceremonial — revisit in Step 5.

---

## Step 5: Quality Review of Existing Tests

Adopt an adversarial tester mindset. For each test that exists, examine it line by line against the
implementation files read in Step 2.

### A. Fake / Wrong / Weak detection

Apply the **Fake / Wrong / Weak Detection Checklist** in `docs/concepts/testing.md` § Fake / Wrong /
Weak Detection Checklist. That section is the authoritative list of test smells and the source of the
three severity levels used in this step's output table. Every Fake or Wrong finding must cite the
specific implementation file and line that proves the assertion is incorrect.

**Misplacement finding (Wrong):** If a T2 service/functionality test asserts a P1 intra-invariant
violation (i.e., it calls into a service and expects `verifyInvariants()` to throw for a P1 rule), flag
it as **Wrong (misplaced)**: "P1 violation asserted in a service test — move to
`{Aggregate}IntraInvariantTest`." P1 predicate tests belong exclusively in T1; the service test should
be deleted or replaced with the correct T1 direct-aggregate case.

### A2. Step-interleaving concurrent-op semantics

For saga-interleaving tests, verify not just that a concurrent operation exists but that it would put
the foreign aggregate into the *specific* forbidden state listed on that step. A concurrent op that
throws for an unrelated reason (e.g. its own guard violation rather than a lock conflict) makes the
test pass for the wrong cause — flag as **Wrong**.

Concretely: read the `setForbiddenStates(…)` call on the step under test and confirm the concurrent
saga transitions the target aggregate into one of the listed states. If the concurrent call fails
before reaching that state transition, the forbidden-state check never fires.

### B. Parameter quality

- Are test data values semantically realistic, not just non-null stubs?
- For any **comparison-predicate** rule (count, timestamp, or collection size): are **both** the
  on-point (last value that satisfies) and the off-point (first value that violates) present? A single
  far-side representative is **Weak (boundary under-coverage)** — cite `docs/concepts/testing.md`
  § Choosing Input Values. (Categorical rules — uniqueness, state flags — need no boundary case.)
- For invariant tests: does the `given:` actually put the system in the violating state, or does it just
  pass an invalid argument while the aggregate state is benign?

### C. Assertion quality

- Does the assertion verify the right field/state on the right object?
- Are returned DTOs verified for all semantically important fields?
- For interleaving tests: does `then:` assert `thrown(SimulatorException)`, not `noExceptionThrown()`?
- After a delete operation, does the test verify the aggregate is no longer retrievable?
- **Invariant/guard violation tests must assert `ex.message == <ERROR_MESSAGE_CONSTANT>`**, not just
  `thrown(<App>Exception)`. The bare-throw form is **Weak** — it passes on any thrown `{App}Exception`,
  including unrelated bugs that happen to throw the same exception type.

### D. Cross-reference with implementation

- Does the test exercise the code path its name suggests? (Confirm by reading the service and saga.)
- Does the `when:` call chain actually reach the method under test, or does it shortcut via a helper?
- **Test class annotations:** every T2 class must be annotated `@DataJpaTest @Transactional
  @Import(LocalBeanConfiguration)`. A missing `@Transactional` silently allows dirty state to bleed
  between tests. Flag as **Wrong**.
- **Exhaustive saga-step scan (interleaving completeness):** for the saga under test, enumerate every
  step that calls `setSemanticLock` or `setForbiddenStates`. For each such step, confirm a matching
  interleaving test exists by step name. A missing interleaving test is a **Missing** finding, not just
  an edge case. This is the most-missed check in practice — see
  `docs/reviews/review-2026-05-23-deferred.md:11-27` for seven concrete examples.
- For saga-interleaving tests: does `executeUntilStep` pause at the correct step (immediately before the
  forbidden-state check), and does the concurrent saga acquire the expected lock?

### D2. Kill-mutation thought experiment

Covered by the **Weak** section of the checklist in `docs/concepts/testing.md` § Fake / Wrong / Weak
Detection Checklist. Apply it to every happy-path test under review.

Cite the specific implementation file and line number for every Fake or Wrong finding.

Produce this table:

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|

Severity levels: **Fake** (never catches a bug), **Wrong** (tests the wrong thing), **Weak** (real
scenario but under-asserts), **Style** (cosmetic).

---

## Step 6: Identify Missing Edge Cases

Beyond the required scenarios from Step 3, look for realistic edge cases not currently covered. For each
gap, state: (a) the scenario, (b) what defect it would catch.

Focus on:
- **Intra-invariant test boundary coverage:** for every non-`final` P1 rule with an ordered predicate
  (count, timestamp, collection size), check that `{Aggregate}IntraInvariantTest` has both the on-point
  (`notThrown`) and the off-point (`thrown` + `ex.getErrorMessage() == <RULE>`) cases. A single
  far-side representative is **Weak (boundary under-coverage)**; propose the missing straddle explicitly.
  Categorical P1 rules (uniqueness, state-freeze) need no boundary pair.
- Boundary values for **P3 numeric guards** — on-point and off-point straddling the threshold (see
  `docs/concepts/testing.md` § Choosing Input Values).
- Empty collections where the code iterates
- Duplicate/already-existing entities where uniqueness is enforced
- Operations on already-deleted aggregates
- Operations applied in wrong order (e.g., answer before quiz starts, enroll after disenroll)
- Concurrent saga interleavings not yet covered: for each step with `setSemanticLock`, is there a test
  for every state in the forbidden list on the consuming step?

Produce a list:

> Missing edge case: `<scenario>` — would catch: `<defect class>`

---

## Step 7: Write Missing Tests

For every **Missing** scenario from Step 4 and every significant gap from Step 6, write the test and add
it to the appropriate test file.

Follow the patterns in `docs/concepts/testing.md` exactly:

- Correct Spock blocks: `given:`, `when:`, `then:`, `and:`
- Not-found cases: use Path A or Path B per the service lookup mechanism (see Step 3 and the
  "Not-Found Test Pattern" appendix)
- Interleaving tests: `executeUntilStep` + `resumeWorkflow` pattern with a concurrent saga in between
- T3 deletion-event tests: use the `and:` extension-block pattern from `testing.md`

**Do NOT add tests for:**
- P1 Java-final field violations
- Infrastructure details (Spring wiring, bean configuration)

---

## Step 8: Fix Fake and Wrong Tests

For tests flagged as **Fake** or **Wrong** in Step 5, fix them in place.
For **Weak** tests, strengthen the assertions.
Keep test names unchanged unless they are actively misleading.

---

## Step 9: Run the Full Test Suite

Use a wildcard pattern to run all test classes for this aggregate without manual enumeration:

```bash
cd applications/{app-name} && mvn clean -Ptest-sagas test \
  -Dtest="*{Aggregate}*Test" 2>&1 | tail -100
```

`*{Aggregate}*Test` matches `{Aggregate}IntraInvariantTest` (T1), all `{Operation}{Aggregate}Test`
files (T2), and `{Aggregate}InterInvariantTest` (T3) — no separate `-Dtest` entries needed.

If the wildcard is too broad (picks up unrelated aggregates with similar names), narrow it to explicit
class names: `-Dtest="{Aggregate}IntraInvariantTest,Create{Aggregate}Test,..."`. The wildcard form
avoids stale `-Dtest=` lists when new test files are added mid-review.

Report:
- BUILD SUCCESS / FAILURE
- Per-class pass/fail counts
- Any compilation errors (flag as Critical action items)

---

## Step 10: Write the Review Report

Write to `applications/{app-name}/reviews/test-review-{Aggregate}.md`.

Sections:

1. **Summary** — verdict (Green / Yellow / Red), key findings, count of tests added and fixed
2. **Expected-test inventory** — full table from Step 4
3. **Quality findings** — table from Step 5
4. **Edge cases added** — list from Step 6
5. **Tests added** — list of new test names per file
6. **Tests fixed** — list of fixed test names with what changed
7. **Build result** — from Step 9

---

## Step 11: Print Summary to Conversation

- Review file path
- Verdict + one-sentence justification
- Number of tests added, number of tests fixed
- Build result: PASS/FAIL, test classes run and passed
- Any Fake or Critical findings verbatim

---

## Step 12: Tick plan.md Checkbox

After writing the review report and printing the summary, tick the Phase 3 checkbox in plan.md.

The aggregate's ordinal `{N}` is the number from the plan.md section header `### {N}. {Aggregate}`.
Replace `- [ ] 3.{N} — {Aggregate}` with `- [x] 3.{N} — {Aggregate}` in plan.md.

---

## Hard Rules

1. **Read implementation before reviewing.** Every Fake or Wrong finding must cite the specific
   implementation file and line that proves the test is incorrect.
2. **Not-found exception type is conditional.** Read the service method to determine Path A vs Path B
   before flagging. Path A (`aggregateLoadAndRegisterRead` by ID) → `thrown(SimulatorException)`.
   Path B (custom repository returning `Optional`, service throws on empty) → `thrown({App}Exception)`.
   Flagging a correct Path B test as Fake is itself a Wrong review finding
   (`docs/concepts/testing.md:229-262`).
3. **P1 final-field rules need no tests.** Skip them in Steps 3 and 7.
3a. **P1 predicate tests belong only in T1 (`{Aggregate}IntraInvariantTest`).** A P1 violation
    asserted in a T2 service/functionality test is **Wrong (misplaced)**; flag and move it.
4. **Do not modify non-test files.** This review touches only `*.groovy` test files and the review
   report.
5. **Build must run.** Do not skip Step 9.
6. **One aggregate per invocation.**
7. **No emojis. Terse and specific. File paths, method names, line numbers.**
8. **Every `setSemanticLock` and `setForbiddenStates` step must have a matching interleaving test.**
   "Appears safe by inspection" is not an exception. A missing interleaving test is a **Missing**
   finding in the Step 4 inventory. Walk the saga file step-by-step in Step 5.D — do not rely on
   counting existing tests to determine completeness.

---

## Interleaving Pattern Reference

**`setSemanticLock` (step that acquires a lock):**
The lock is acquired *by* this step. The forbidden-state check occurs on the *consuming* step (a later
step that targets this aggregate as a foreign object). The interleaving test must:
1. `executeUntilStep` to pause at the step **immediately before** the consuming step.
2. Have a concurrent saga call the operation that acquires the same lock.
3. `resumeWorkflow` and expect `thrown(SimulatorException)`.

**`setForbiddenStates` (step that checks for forbidden states):**
This step itself performs the check. The interleaving test must:
1. `executeUntilStep` to pause **at** this step (i.e., before it runs).
2. Have a concurrent saga acquire one of the listed forbidden states on the target aggregate.
3. `resumeWorkflow` and expect `thrown(SimulatorException)`.

A fake interleaving test calls `executeUntilStep` then immediately `resumeWorkflow` with no concurrent
saga in between — the forbidden state is never set, so the test always passes. Flag this as Fake.

---

## Not-Found Test Pattern

There are two distinct not-found paths. **Read the service method first** to determine which applies.

**Path A — primary-key lookup via `aggregateLoadAndRegisterRead`:**
The infrastructure throws `SimulatorException`. Use `thrown(SimulatorException)`.

```groovy
given:
def id = // create the aggregate and capture its ID
// delete the aggregate so it no longer exists

when:
{aggregate}Functionalities.get{Aggregate}ById(id)

then:
thrown(SimulatorException)
```

**Path B — composite-key lookup via custom repository:**
The service calls a custom repository (e.g., `findByQuizIdAndUserId`) returning `Optional`. When empty,
the service throws `{App}Exception`. Use `thrown({App}Exception)`.

```groovy
given:
// do NOT create the entity, or create then delete it

when:
{aggregate}Functionalities.get{Aggregate}ByKey(nonExistentQuizId, nonExistentUserId)

then:
def ex = thrown({App}Exception)
ex.message == {App}ErrorMessage.AGGREGATE_NOT_FOUND  // or the specific error constant
```

**Rule of thumb** (`docs/concepts/testing.md:262`): if the service calls `aggregateLoadAndRegisterRead`
with an ID, expect `SimulatorException`; if the service calls a custom repository method and throws on
empty Optional, expect `{App}Exception`. Flagging a correct Path B test as Fake is itself a **Wrong**
review finding.
