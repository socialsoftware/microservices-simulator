# Testing

This document is the authoritative guide to test coverage in applications built on the
microservices-simulator. It defines the Test Taxonomy, locates each type in the workflow,
and provides structure templates for each category.

---

## Test Taxonomy

| Type | Naming pattern | What it validates |
|------|----------------|-------------------|
| **T1 Intra-Invariant** | `<Aggregate>IntraInvariantTest` | Creation happy-path (all P1 pass + fields) · one violation per non-`final` P1 rule (EP) · boundary straddle (BVA on/off-point) for ordered-domain predicates. All via direct construction/mutation + `verifyInvariants()`. |
| **T2 Functionality** | `<FunctionalityName>Test` | Happy path (full `NOT_IN_SAGA→IN_{OP}→NOT_IN_SAGA` traversal) · P3 guard violations · state-transition (lock-acquisition) case per saga lock step |
| **T3 Inter-Invariant** | `<Consumer>InterInvariantTest` | Event received → cached state updated; unrelated event → state unchanged |

---

## Anti-Pattern: Reverse-Engineering Tests from the Implementation

AI assistants will, given only a coverage metric, read the implementation they just wrote and produce assertions that mirror what the code does — not what the domain says it should do. These tests are trivially satisfied by the implementation, break on correct refactors, and provide a false sense of security. The checklist below is the authoritative list of test smells; both the implementation skill (`.claude/skills/implement-aggregate/`) and the review skill (`.claude/skills/review-tests/SKILL.md` Step 5) consume it.

---

## Fake / Wrong / Weak Detection Checklist

Findings against this checklist map to three severities used by `review-tests`:

- **Fake** — the test always passes regardless of implementation correctness.
- **Wrong** — the test tests the wrong thing (typically the implementation rather than the spec, or a different code path than the name suggests).
- **Weak** — the scenario is real but the assertions under-specify it; a regression could slip through.

### Fake

- `then:` block is only `noExceptionThrown()` with no field assertions — flag unless the scenario is explicitly "operation must not throw."
- `then:` assertions check only non-null / non-empty, never actual values.
- `then:` asserts a condition that is logically trivially true (e.g., `result != null` when the method always returns non-null).
- `when:` block does not call the method under test (bypasses the service via a setup helper).
- T3 "ignores unrelated": `<originalValue>` was captured *after* the event was processed rather than before — the assertion is `x == x`. The pre-event value must be captured in `given:` before firing.

### Wrong

- Test name and message constant were copied verbatim from the implementation without checking against `plan.md`'s rule list. If the implementation uses a different constant than `plan.md` names, the test validated the implementation deviation instead of catching it.
- Violation test asserts a message constant that matches what the implementation throws but differs from the constant named in `plan.md` for that rule.
- `then:` mirrors the sequence of `set…` calls in the service body — the test was derived from the implementation, not the spec.
- T3 deletion-event test puts the post-deletion load attempt in `then:` instead of an `and:` block — the load won't be in the exception-capture scope. Correct pattern: load in `and:`, assert `thrown(SimulatorException)` in `then:` (see § T3 Deletion-Event Tests).
- Not-found test exception type contradicts the actual lookup mechanism. **Read the service method first.** Rule of thumb: if the service calls `aggregateLoadAndRegisterRead` directly with an ID, expect `SimulatorException` (Path A). If the service first calls a custom repository returning `Optional` and throws on empty, expect `{App}Exception` (Path B). Flagging a correct Path B `thrown({AppClass}Exception)` as Fake is itself a Wrong finding.
- Test class missing `@Transactional` (T2/T3 classes must be `@DataJpaTest @Transactional @Import(LocalBeanConfiguration)`) — silently allows dirty state to bleed between tests.
- P1 intra-invariant violation asserted in a service/functionality test (misplaced; belongs in `{Aggregate}IntraInvariantTest`).

### Weak

- Happy-path `then:` asserts only fields that were already set in `setup:` — the under-test call did not need to run for the assertions to hold. Verify at least one asserted field is a value the operation itself must produce, not pre-existing fixture data.
- Removing `unitOfWork.registerChanged(aggregate)` from the service would leave the test passing (kill-mutation thought experiment). Add an assertion on a field the operation changes that is only readable through the persisted aggregate (the `then:` reads back via `get{Aggregate}ById`, not from a local variable).
- Violation test asserts `thrown({AppClass}Exception)` without `ex.message == <ERROR_MESSAGE_CONSTANT>`. The bare-throw form passes on any thrown exception of that type, including unrelated bugs.
- Returned DTO assertions cover only a subset of the semantically important fields.
- Numeric, temporal, or collection-size boundary rule asserted with only a far-side representative value — see § Choosing Input Values. Missing the on-point **or** the off-point is a *Weak* finding.

---

## Choosing Input Values — Equivalence Partitioning & Boundary Value Analysis

Two complementary test-design techniques decide *which* values to feed a rule. A complete suite uses both.

- **Equivalence Partitioning (EP)** — split a rule's input domain into classes the system treats the same way (e.g. "valid count", "count too low"), then test **one representative per class**. This is the baseline most tests already do: one value that satisfies the rule, one that violates it.
- **Boundary Value Analysis (BVA)** — defects cluster at the **edges** between those classes (off-by-one, `<` written where `<=` was meant). EP's mid-class representatives sail straight past them. For a rule with an ordered domain, also test the two values **straddling** the boundary.

### Decision rule

> For every P1 invariant or P3 numeric guard whose predicate is a **comparison on an ordered domain**
> (`<`, `<=`, `>`, `>=`, `==`/`!=` over a **count**, a **timestamp**, or a **collection size**), EP's
> single representative is **not sufficient**. Add the two **boundary-straddling** cases:
> - **on-point** — the last value that **satisfies** the rule → assert `notThrown(...)`;
> - **off-point** — the first value that **violates** it → assert `thrown(...)` **and** `ex.message == <RULE>`.
>
> **P1 ordered-domain boundaries live in the intra-invariant test** (`{Aggregate}IntraInvariantTest`)
> — direct construction with pinned values, then `verifyInvariants()`. P3 numeric guard boundaries
> may be added to the corresponding T2 service test.
>
> For **categorical** invariants — uniqueness (e.g. `TOURNAMENT_UNIQUE_AS_PARTICIPANT`), boolean/state
> freezes (`TOURNAMENT_IS_CANCELED`), set membership, presence/absence — there is no ordered edge to
> probe. EP's one-representative-per-class is correct and complete; do **not** invent boundary cases.

### Worked patterns

| Rule shape | Example invariant | On-point (no throw) | Off-point (throws) |
|---|---|---|---|
| `count > 0` | `NUMBER_OF_QUESTIONS_POSITIVE` | `1` | `0` |
| `count <= N` | `MAX_QUESTIONS` (N=30) | `30` | `31` |
| `a < b` (timestamps) | `START_BEFORE_END_TIME` | `end − 1 tick` | `start == end` |
| `a >= b` (timestamps) | `ANSWER_BEFORE_START` (`firstAnswerTime >= startTime`) | `firstAnswerTime == startTime` | `startTime − 1 tick` |
| `size >= 1` | `MUST_HAVE_ONE_TOPIC` | `1` | `0` |

### Temporal-boundary mechanics

The smallest tick on a `LocalDateTime` is `.minusNanos(1)` / `.plusNanos(1)`. Pin **both** instants
explicitly so the on-point is exactly equal. All temporal P1 boundary cases belong in
`{Aggregate}IntraInvariantTest` as **direct-aggregate** tests (construct, set fields, call
`verifyInvariants()`) — the saga path stamps `lastModifiedTime = now()` and cannot pin the exact
on-point. See `TournamentIntraInvariantTest` for the canonical pattern (`ANSWER_BEFORE_START`).

---

## Spec-First Ordering

Before writing any T2 test for a write functionality, locate the **`plan.md` aggregate section** for the target aggregate. The happy-path postconditions, the events-published list, and the P1/P3 rule list in that section *are* the spec — assertions must trace to them. Do not read the implementation you just wrote to decide what to assert.

Each `plan.md` aggregate section is shaped roughly like:

```
Functionality: {Op}{Aggregate}
Happy-path postconditions:
  - {Aggregate}.{field} == {expectedValue}
  - emitted event {Event} with payload {…}
  - SagaState after commit == NOT_IN_SAGA
Violations (one row per P1/P3 rule this op can trip):
  - {RULE_NAME} → throws {AppClass}Exception, message == {RULE_NAME}
```

This is the structure to **cite** when writing the test (see § T2 — Spec-first below for the recommended `// Spec:` comment), not a new artifact to author in the test file. Reading `plan.md` is the spec lookup; the test is the assertion of that spec.

If the implementation disagrees with the spec, the **implementation** is the bug — not the spec. Assertions should never be adjusted to match surprising implementation behavior; the discrepancy should be flagged and fixed in the implementation.

This ordering also applies to T3: before writing the test, look up the expected cached-field value in `plan.md`'s subscribed events table for the consumer aggregate — not from reading the `EventProcessing` class.

---

## Directory Layout

```
src/test/groovy/<pkg>/
├── BeanConfigurationSagas.groovy     ← test configuration (infrastructure beans)
├── SpockTest.groovy                  ← root Spock marker class
├── <AppName>SpockTest.groovy         ← base class: @Autowired services + factory helpers
└── sagas/
    ├── coordination/
    │   ├── <aggregate>/              ← one dir per primary aggregate
    │   │   ├── <FunctionalityName>Test.groovy      (T2)
    │   │   └── ...
    │   └── ...
    └── <aggregate>/                  ← one dir per consumer aggregate
        ├── <Aggregate>IntraInvariantTest.groovy    (T1)
        └── <Aggregate>InterInvariantTest.groovy    (T3)
```

---

## T1 — Intra-Invariant Test

**Purpose:** Own the complete P1 matrix for an aggregate: (1) creation happy-path proving all
intra-invariants pass on a fresh instance, (2) one violation per non-`final` P1 rule (EP), and
(3) boundary-straddling on/off-point pairs (BVA) for every ordered-domain P1 predicate. All cases
are **direct-aggregate** tests — construct the aggregate, optionally call aggregate mutators
(e.g. `addParticipant`), then call `verifyInvariants()` explicitly.

Direct construction is *the* way to test P1: the saga path stamps `lastModifiedTime = now()` and
cannot pin exact boundary instants. This generalises what was formerly a narrow "temporal-boundary
exception" — it is now the standard approach for all P1 coverage.

**Assertion provenance:** Fields asserted in the happy-path case must trace to the constructor's
documented intent — the aggregate field list in the `plan.md` aggregate section. Never read the
constructor body to decide what to assert; if the constructor sets a field the spec doesn't list,
that is a planning gap to flag, not a field to copy into the test.

**P1 Java-`final` fields need no test coverage anywhere** (T1, T2, T3, or otherwise). The Java compiler
enforces immutability; there is no write path that can violate the constraint, so testing it would
be testing the language, not the domain logic.

**Boundary on-points (valid side):** for comparison-predicate invariants (count / timestamp /
collection-size), pin the on-point exactly (e.g. `end − 1 tick` for a strict-less-than timestamp
rule) and assert `notThrown(...)`. Pin the off-point one step beyond (e.g. `start == end`) and
assert `thrown(...)` with the exact error-message constant. See § Choosing Input Values.

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Aggregate>IntraInvariantTest extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create <aggregate>"() {
        // If the constructor takes a DTO, build it in 'given:' first:
        // given:
        // def dto = new <Aggregate>Dto()
        // dto.set<Field>(...)

        when:
        def result = new Saga<Aggregate>(/* id, args or dto */)

        then:
        result.<field> == <expectedValue>
        // assert all expected fields
    }

    def "<aggregate>: <RULE_NAME> violation"() {
        given:
        def agg = new Saga<Aggregate>(/* valid args */)
        // set field(s) to the violating value directly

        when:
        agg.verifyInvariants()

        then:
        def ex = thrown(<App>Exception)
        ex.message == <RULE_NAME>
    }

    def "<aggregate>: <RULE_NAME> on-point (boundary — no throw)"() {
        given:
        def agg = new Saga<Aggregate>(/* args with field pinned to exact on-point value */)

        when:
        agg.verifyInvariants()

        then:
        notThrown(<App>Exception)
    }

    def "<aggregate>: <RULE_NAME> off-point (boundary — throws)"() {
        given:
        def agg = new Saga<Aggregate>(/* args with field pinned to first violating value */)

        when:
        agg.verifyInvariants()

        then:
        def ex = thrown(<App>Exception)
        ex.message == <RULE_NAME>
    }
}
```

---

## The Saga as a State Machine

Every write saga is a finite state machine over its aggregate's `SagaState`:

- **States** — `NOT_IN_SAGA` (the quiescent state; the framework sets it in the `Saga{Aggregate}`
  constructor) plus one `IN_{OP}` *locked* state per operation that must hold a semantic lock across
  steps.
- **Transitions** —
  - *acquire*: a step calling `setSemanticLock(IN_{OP})` drives `NOT_IN_SAGA → IN_{OP}`;
  - *complete*: a successful commit drives `IN_{OP} → NOT_IN_SAGA`;
  - *compensate*: a mid-saga failure releases the lock — also `IN_{OP} → NOT_IN_SAGA`.
- **Guards** — `setForbiddenStates([...])` on a step blocks a transition when a foreign aggregate is
  already in a listed state.

Testing a write saga means covering its transitions. The two existing T2 saga tests already do this:

| Test | Transition(s) covered |
|------|-----------------------|
| happy-path `success` | full traversal `NOT_IN_SAGA → IN_{OP} → NOT_IN_SAGA` (asserts the aggregate ends `NOT_IN_SAGA`) |
| `<step> acquires IN_{OP} semantic lock` | pins the intermediate `IN_{OP}` state after the *acquire* transition, then resumes to complete the traversal |

**Guard** transitions (`setForbiddenStates`) and **compensation** faults are out of current scope —
see Appendix (Cross-Functionality and Fault/Behavior tests).

---

## T2 — Functionality Test

**Purpose:** Cover the happy path (the full state-machine traversal), P3 guard violations, and the
state-transition (semantic-lock acquisition) case for each saga lock step. P1 intra-invariants are
**not** tested here — see § T1.

**State-transition rule (semantic-lock acquisition):** Each saga step that calls `setSemanticLock` is
an *acquire* transition into `IN_{OP}` (see § The Saga as a State Machine).
- For each such step, run the workflow through it via `executeUntilStep`, assert the aggregate is in
  the expected `IN_{OPERATION}` saga state (the post-*acquire* state), then `resumeWorkflow(uow)` and
  assert `noExceptionThrown()` (completing the traversal back to `NOT_IN_SAGA`).
- Cross-aggregate `setForbiddenStates` conflict validation is **deferred — see Appendix, Cross-Functionality Test**.

**Happy-path = full traversal:** the `success` test drives `NOT_IN_SAGA → IN_{OP} → NOT_IN_SAGA`.
Asserting `sagaStateOf(<aggregateId>) == GenericSagaState.NOT_IN_SAGA` at the end pins the *complete*
transition's endpoint (see § The Saga as a State Machine).

**Spec-first:** Before writing the happy-path test, locate the `plan.md` aggregate section for the
target aggregate. The fields-changed list, events-published list, and expected `SagaState` in that
section *are* the spec — cite it, do not duplicate it (see Spec-First Ordering above). Write a 1-line
`// Spec:` comment at the top of each test naming the plan.md section and the rule it asserts, e.g.
`// Spec: plan.md §3.5 Question / functionalities — UpdateQuestionContent; rule QUESTION_CONTENT_REQUIRED`.
Write assertions against the cited section — do not start from the service body.

**Kill-mutation check:** For each happy-path test ask: "If I remove `unitOfWork.registerChanged(aggregate)`
from the service, does this test still pass?" If yes, the test is not exercising the commit. Add an
assertion that can only pass if the aggregate state was actually persisted and returned through the
read path.

**Message constant provenance:** In violation tests, `<RULE_NAME>` must be the constant that `plan.md`
names for this rule, not whatever constant the implementation happens to throw. If the implementation
uses a different constant, that is an implementation deviation from spec — flag the mismatch rather
than adjusting the test to match.

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <FunctionalityName>Test extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    // ─── Setup helpers ─────────────────────────────────────────────────────────

    def setup() { /* shared fixture creation */ }

    // ─── Happy path ────────────────────────────────────────────────────────────

    def "<functionalityName>: success"() {
        given: 'aggregates exist'
        // ...
        when:
        def result = <primary>Functionalities.<functionalityName>(/* args */)
        then:
        // assert expected outcome
        sagaStateOf(<aggregateId>) == GenericSagaState.NOT_IN_SAGA  // complete transition: IN_<OP> → NOT_IN_SAGA
    }

    // ─── Invariant / guard violations ─────────────────────────────────────────

    def "<functionalityName>: <RULE_NAME> violation"() {
        given: 'aggregate in violating state'
        // ...
        when:
        <primary>Functionalities.<functionalityName>(/* violating args */)
        then:
        def ex = thrown(<App>Exception)
        ex.message == <RULE_NAME>
    }
    // one P3 guard case per violation
    // Skip P1 tests — they belong in {Aggregate}IntraInvariantTest.

    // ─── State-transition (semantic-lock acquisition) ──────────────────────────
    // One case per saga step that calls setSemanticLock — the acquire transition

    def "<functionalityName>: <lockStep> acquires IN_<OP> semantic lock"() {
        given: 'acquire transition: NOT_IN_SAGA → IN_<OP>; resume completes IN_<OP> → NOT_IN_SAGA'
        def uow = unitOfWorkService.createUnitOfWork("<FunctionalityName>")
        def func = new <FunctionalityName>FunctionalitySagas(
                unitOfWorkService, /* args */, uow, commandGateway)
        func.executeUntilStep("<lockStep>", uow)

        expect:
        sagaStateOf(<aggregateId>) == <Aggregate>SagaState.IN_<OP>

        when:
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }
}
```

### Not-Found Assertions

There are two distinct not-found paths, and they throw different exception types:

**Path A — primary-key lookup via `aggregateLoadAndRegisterRead`**

The infrastructure method throws `SimulatorException` when no aggregate exists for a given primary ID. Use `thrown(SimulatorException)` for these cases:

```groovy
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException

def "<functionalityName>: aggregate not found"() {
    when:
    <primary>Functionalities.<functionalityName>(999 /* non-existent id */)
    then:
    thrown(SimulatorException)
}
```

**Path B — composite-key lookup via custom repository**

When a service method first queries by non-primary-key fields (e.g., `quizId + userId`) using a custom repository that returns `Optional`, an empty result is detected at the **service level** and the service throws `<App>Exception`. Use `thrown(<App>Exception)` for these cases:

```groovy
def "<functionalityName>: not found by composite key"() {
    when:
    <primary>Functionalities.<functionalityName>(999 /* non-existent quizId */, 999 /* non-existent userId */)
    then:
    def ex = thrown(<App>Exception)
    ex.message == <NOT_FOUND_ERROR_MESSAGE>
}
```

**Rule of thumb:** if the service calls `aggregateLoadAndRegisterRead` directly with an ID, expect `SimulatorException`. If the service first calls a custom repository returning `Optional` and throws on empty, expect `<App>Exception`.

---

## T3 — Inter-Invariant Test

**Purpose:** Verify that when a publisher emits an event, the consumer aggregate's cached state
is updated; and that an event for an unrelated entity leaves state unchanged.

**Key constraint:** `@Scheduled` does **not** run in `@DataJpaTest`. Call the polling method
directly: `<consumer>EventHandling.handle<Xxx>Events()`.

**Asserting cached sub-entity fields.** The template uses `<consumer>Service.get<Consumer>(...)`, which returns a DTO. If the DTO does not expose a cached sub-entity field (e.g., `topicName` stored inside a `QuestionTopic` embedded in the aggregate), load the aggregate directly instead:
```groovy
def agg = unitOfWorkService.aggregateLoadAndRegisterRead(
        consumer.aggregateId, unitOfWorkService.createUnitOfWork("check")) as <Consumer>
agg.<subEntity>.<cachedField> == <expectedValue>
```

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Consumer>InterInvariantTest extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    <Consumer>EventHandling <consumer>EventHandling

    // ─── <INVARIANT_NAME> ────────────────────────────────────────────────────

    def "<consumer> <action> on <Xxx>Event[ for <role>]"() {
    // <action> should be a concrete verb: updates, removes, deletes self, anonymizes, invalidates self.
        given:
        def publisher = create<Publisher>(/* args */)
        def consumer  = create<Consumer>(/* args linked to publisher */)

        when: '<publisher> triggers the event'
        <publisher>Functionalities.<triggeringOp>(/* args */)

        and: 'consumer polls for the event'
        <consumer>EventHandling.handle<Xxx>Events()

        then: 'consumer cached field is updated'
        def updated = <consumer>Service.get<Consumer>(consumer.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        updated.<cachedField> == <newValue>
    }

    def "<consumer> ignores <Xxx> event for unrelated entity"() {
        given:
        def publisher1 = create<Publisher>(/* args */)
        def publisher2 = create<Publisher>(/* args — different entity */)
        def consumer   = create<Consumer>(/* linked to publisher1 */)
        // Capture the expected value BEFORE the event fires — never after
        def originalValue = <consumer>Service.get<Consumer>(consumer.aggregateId,
                unitOfWorkService.createUnitOfWork("check")).<cachedField>

        when:
        <publisher>Functionalities.<triggeringOp>(publisher2.aggregateId, /* args */)
        <consumer>EventHandling.handle<Xxx>Events()

        then: 'consumer state unchanged'
        def unchanged = <consumer>Service.get<Consumer>(consumer.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        unchanged.<cachedField> == originalValue
    }
}
```

### Deletion-Event Tests (T3)

When a deletion event causes the consumer aggregate itself to be marked `DELETED` (i.e., the processing method calls `remove()` on the aggregate), `aggregateLoadAndRegisterRead` filters out `DELETED` aggregates and throws `SimulatorException`. The standard `then:` assertion pattern — loading the aggregate and asserting a field value — does not work here.

**Structure:** move the load attempt into an `and:` block (which extends the `when:` phase in Spock), then assert `thrown(SimulatorException)` in `then:`.

```groovy
def "<consumer> is deleted when <Publisher> deletion event is processed"() {
    given:
    def publisher = create<Publisher>(/* args */)
    def consumer  = create<Consumer>(/* args linked to publisher */)

    when: '<publisher> is deleted'
    <publisher>Functionalities.delete<Publisher>(publisher.aggregateId)

    and: 'consumer polls for the deletion event'
    <consumer>EventHandling.handle<Xxx>Events()

    and: 'attempt to load the now-deleted consumer aggregate'
    unitOfWorkService.aggregateLoadAndRegisterRead(
            consumer.aggregateId, unitOfWorkService.createUnitOfWork("check"))

    then: 'DELETED aggregate is not loadable — infrastructure throws SimulatorException'
    thrown(SimulatorException)
}
```

> **Why `and:` not `then:`?** In Spock, `then:` captures the first exception thrown by the immediately preceding `when:` block; subsequent `and:` blocks inside the `when:` phase extend that same exception-capture scope. Moving the load into `and:` keeps it within that scope so `thrown(SimulatorException)` can match it. Placing the load in `then:` would require using `notThrown()` first and then separately calling the load, which breaks the Spock block model.

---

## Test Profile — Serialization Behavior

When `local.messaging.serialize: true` is set in the test profile (typically in `application-test-sagas.properties`), commands and their responses are serialized/deserialized through Jackson. This affects how `CommandResponse.result` — which is declared as `Object` — round-trips.

### List returns and type erasure

`CommandResponse.result` is typed as `Object`. When a read saga stores a `List<XxxDto>` in that field, Jackson cannot embed per-element `@class` metadata (the `isCollectionLikeType()` guard prevents it). On deserialization, each element comes back as a `LinkedHashMap` rather than the expected `XxxDto`.

**The fix is in `MessagingObjectMapperProvider.useForType()`:** the method must return `true` when `raw == Object.class`. This tells Jackson to embed `@class` on any value stored in an `Object`-typed field, which in turn enables element-level type information even inside an untyped collection. Without this fix, any read saga that returns a list via `CommandResponse.result` will fail at deserialization with a `ClassCastException` (`LinkedHashMap` cannot be cast to `XxxDto`).

**When this matters:** Only when `local.messaging.serialize: true` is active. Tests that run without this flag are unaffected; production code (which does not serialize locally) is unaffected.

---

## Appendix — Deferred Test Types

These test types are documented for future work but are **not** part of the current workflow. They are preserved here for reference only.

---

### Cross-Functionality Test

**Purpose:** Validate that two concurrent operations on overlapping aggregates either produce a
consistent result or correctly reject one operation via semantic locks.

**Selection rule:** Create one Cross-Functionality test for each pair (A, B) of functionalities that share at
least one aggregate **and** where concurrent execution poses a real consistency risk (e.g., one
modifies a field the other reads from).

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Op1>And<Op2>Test extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "sequential: <op1> then <op2>"() {
        given: 'shared aggregate setup'
        // ...
        when:
        <primary1>Functionalities.<op1>(/* args */)
        def result = <primary2>Functionalities.<op2>(/* args */)
        then:
        // assert consistent final state
    }

    def "sequential: <op2> then <op1>"() { /* reverse ordering */ }

    def "concurrent: <op1> step1 → <op2> completes → <op1> resumes"() {
        given:
        def uow1 = unitOfWorkService.createUnitOfWork("<Op1>")
        def func1 = new <Op1>FunctionalitySagas(unitOfWorkService, /* args */, uow1, commandGateway)

        when:
        // executeUntilStep completes <stepBeforeShared>, halting before the shared/conflict step
        func1.executeUntilStep("<stepBeforeShared>", uow1)

        and:
        <primary2>Functionalities.<op2>(/* args */)

        and:
        func1.resumeWorkflow(uow1)

        then:
        // either both succeed with consistent state, or one throws a meaningful exception
        noExceptionThrown()  // or: thrown(<App>Exception)
    }
}
```

---

### Fault / Behavior Test

**Purpose:** Verify saga compensation when a step fails mid-workflow. The aggregate must be
in a consistent state (either fully applied or fully rolled back) after the fault.

**How to inject faults:** Use `ImpairmentService` to impair a specific step of a specific workflow.
After the fault, verify the aggregate is back to its pre-operation state and the saga state is
`NOT_IN_SAGA` or `PENDING_COMPENSATION` (depending on the step).

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Functionality>FaultTest extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired ImpairmentService impairmentService

    def "<functionality>: step <faultStep> fails → aggregate rolls back"() {
        given:
        def aggregate = create<Primary>(/* valid args */)

        and: 'inject fault at target step'
        impairmentService.addCommandImpairment("<FunctionalityName>", "<faultStep>",
                ImpairmentType.EXCEPTION_ON_EXECUTE)

        when:
        <primary>Functionalities.<functionalityName>(/* args */)

        then:
        thrown(Exception)

        and: 'aggregate unchanged / rolled back'
        def state = <primary>Service.get<Primary>(aggregate.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        state.<mutatedField> == <originalValue>
    }
}
```

---

### Async Test

**Purpose:** Verify that multiple concurrent invocations of the same operation (fire-and-forget,
no step coordination) all complete without corrupting aggregate state.

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Functionality>AsyncTest extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "<functionality>: N concurrent invocations all succeed"() {
        given:
        def shared = create<SharedAggregate>(/* args */)
        def participants = (1..N).collect { create<OtherAggregate>(/* args */) }

        when: 'fire all concurrently'
        def threads = participants.collect { p ->
            Thread.start {
                <primary>Functionalities.<functionalityName>(shared.aggregateId, p.aggregateId)
            }
        }
        threads*.join()

        then:
        def result = <primary>Service.get<Primary>(shared.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        result.<collection>.size() == N
    }
}
```
