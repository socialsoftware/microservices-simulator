# Testing

This document is the authoritative guide to test coverage in applications built on the
microservices-simulator. It defines the Test Taxonomy, locates each type in the workflow,
and provides structure templates for each category.

---

## Test Taxonomy

| Type | Naming pattern | What it validates |
|------|----------------|-------------------|
| **T1 Creation** | `<Aggregate>Test` | Aggregate instantiation + all intra-invariants pass on a fresh instance |
| **T2 Functionality** | `<FunctionalityName>Test` | Happy path · invariant/guard violations · ≥1 semantic-lock-acquisition case per saga lock step |
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
- Test class missing `@Transactional` (T2 classes must be `@DataJpaTest @Transactional @Import(LocalBeanConfiguration)`) — silently allows dirty state to bleed between tests.

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
explicitly so the on-point is exactly equal. Write temporal-boundary cases as **direct-aggregate unit
tests** (the `<Aggregate>Test.groovy` style that sets fields and asserts), **not** through the full
saga: the saga path stamps `lastModifiedTime = now()` and cannot pin the exact on-point. This is the
one place the suite deliberately calls `verifyInvariants()` directly — keep it consistent with how
`TournamentTest` already does it for `ANSWER_BEFORE_START`.

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
        ├── <Aggregate>Test.groovy                  (T1)
        └── <Aggregate>InterInvariantTest.groovy    (T3)
```

---

## T1 — Creation Test

**Purpose:** Prove the aggregate can be instantiated with valid state. Invariant violations are
**not** tested here — they belong in T2, where service method calls trigger `registerChanged →
verifyInvariants` automatically. Never call `verifyInvariants()` directly.

**Exception — temporal-boundary cases.** An invariant comparing two timestamps (e.g.
`ANSWER_BEFORE_START`) needs its on-point pinned to an *exact equal instant*, which the T2 saga path
cannot do because it stamps `lastModifiedTime = now()`. Those specific boundary pairs are written as
**direct-aggregate** tests in this file that set the fields and call `verifyInvariants()` explicitly —
the one sanctioned use of a direct call (see `TournamentTest`'s `ANSWER_BEFORE_START` cases). See
§ Choosing Input Values — Temporal-boundary mechanics.

**Assertion provenance:** Fields asserted in a T1 test must trace to the constructor's documented
intent — the aggregate field list in the `plan.md` aggregate section. Never read the constructor
body to decide what to assert; if the constructor sets a field the spec doesn't list, that is a
planning gap to flag, not a field to copy into the test.

**P1 Java-`final` fields need no test coverage anywhere** (T1, T2, or otherwise). The Java compiler enforces immutability; there is no write path that can violate the constraint, so testing it would be testing the language, not the domain logic.

**Boundary on-points (valid side):** where the aggregate has a comparison-predicate invariant (count /
collection-size limit), the T1 creation test may instantiate at the **valid** boundary — e.g. create
once at the minimum valid `numberOfQuestions` and once at the maximum — to prove a fresh on-point
instance passes. The violating off-point belongs in T2. See § Choosing Input Values.

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Aggregate>Test extends <AppName>SpockTest {

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
}
```

---

## T2 — Functionality Test

**Purpose:** Cover the happy path, every invariant/guard violation, and the semantic-lock-acquisition
case for each saga lock step.

**Semantic-lock-acquisition rule:**
- For each saga step that calls `setSemanticLock`, run the workflow through that step via
  `executeUntilStep`, assert the aggregate is in the expected `IN_{OPERATION}` saga state, then
  `resumeWorkflow(uow)` and assert `noExceptionThrown()`.
- Cross-aggregate `setForbiddenStates` conflict validation is **deferred — see Appendix T4**.

**Upstream-invariant rule:** When a saga increments a counter cached on an upstream aggregate (e.g., `questionCount` on `Course`), verify that the upstream aggregate's invariants permit the new counter value *before the first test runs*. If not, add the necessary prerequisite state to the `setup:` block. For example, if `Course` enforces `executionCount > 0` when `questionCount > 0`, every `CreateQuestion` test must call `createExecution(courseId, ...)` in `setup:` first. Place these prerequisites in the shared `setup()` block by default; only inline them in a per-method `given:` block when a single test needs a state variant the rest of the file does not share.

> **Read tests are not exempt.** Even though a read functionality itself does not mutate state, its `setup:` block typically creates the aggregate under test (e.g., `createQuestion`), which may increment a counter on an upstream aggregate. That increment can trigger the upstream's invariants, so any prerequisite state must be established in `setup:` *before* the aggregate creation call, regardless of whether the test exercises a read or write functionality.

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
    // one case per invariant/guard
    // Skip P1 tests for Java `final` fields — no write path can violate them.

    // ─── Semantic-lock acquisition ─────────────────────────────────────────────
    // One case per saga step that calls setSemanticLock

    def "<functionalityName>: <lockStep> acquires IN_<OP> semantic lock"() {
        given:
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

**P1 intra-invariant violation coverage:** For every rule checked in `verifyInvariants()` (non-`final`-field P1 rules from `plan.md §3.1`), add one T2 scenario that triggers the violation. Skip only rules marked "Java `final` field". For rules whose predicate is a **comparison on an ordered domain** (count / timestamp / collection size), one violation case is not enough — add the boundary-straddling off-point per § Choosing Input Values (and cover the on-point as a direct-aggregate case; see § Choosing Input Values — Temporal-boundary mechanics).

For **time-based invariants** (e.g., `ENROLL_UNTIL_START_TIME`, `FINAL_AFTER_START`), create the aggregate with a past `startTime` in `given:`:

```groovy
def pastTournamentId = createTournament(executionId, creatorId, [topicId], 1,
        LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1)).aggregateId
```

Then call the functionality and assert `thrown(<App>Exception)` with the exact error message constant. This works because `verifyInvariants()` fires inside `registerChanged` — when the setters stamp `lastModifiedTime = now() > prev.startTime`, the invariant detects the violation.

This `now()`-stamping is exactly why it cannot pin the **boundary on-point** (the equal-instant case). For comparison-on-timestamp invariants, complement this far-side T2 violation with a boundary-straddling pair — equal-instant on-point (`notThrown`) and one-nanosecond-across off-point (`thrown`) — written as **direct-aggregate** cases in the `<Aggregate>Test.groovy` file (see § T1 — Creation Test, *Exception — temporal-boundary cases*, and § Choosing Input Values).

### Service-Command Tests (T2 variant)

Some aggregates expose service methods that are invoked via command handlers from **other**
aggregates' sagas (e.g., `decrementExecutionCount` called when a CourseExecution is deleted)
but are NOT exposed through `{Aggregate}Functionalities`. These methods still call
`registerChanged`, which triggers `verifyInvariants()`, so they can raise invariant violations.

For each such method, write T2-style tests covering:
- **Happy path** — state changes as expected, no exception
- **Floor/ceiling behaviour** — e.g., decrement at zero stays at zero
- **Invariant violations** — every `verifyInvariants()` path the method can reach

**No semantic-lock case is needed** (there are no saga steps).

**Location:** `sagas/coordination/{aggregate}/`  
**Naming:** `{OperationName}Test.groovy`, or combine related operations into one file
(e.g., `{Aggregate}CountsTest.groovy`) when they share setup.

**Template:**
```groovy
def "setup"() {
    aggregate = create<Aggregate>(/* valid args */)
}

def "<op>: success"() {
    when:
    def uow = unitOfWorkService.createUnitOfWork("<op>")
    <aggregate>Service.<op>(aggregate.aggregateId, uow)
    unitOfWorkService.commit(uow)

    then:
    def result = <aggregate>Functionalities.get<Aggregate>ById(aggregate.aggregateId)
    result.<field> == <expectedValue>
}

def "<op>: <RULE_NAME> violation"() {
    given: '<aggregate> in state that will violate invariant after <op>'
    // set up prerequisite state via prior service calls + commit

    when:
    def uow = unitOfWorkService.createUnitOfWork("<op>")
    <aggregate>Service.<op>(aggregate.aggregateId, uow)
    unitOfWorkService.commit(uow)   // verifyInvariants fires here

    then:
    def ex = thrown(<App>Exception)
    ex.message == <RULE_NAME>
}
```

---

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

**Upstream-invariant rule applies.** T3 tests typically create the consumer aggregate in `given:`, which may increment a counter on an upstream aggregate and trigger its invariants. Apply the same rule as T2: establish any prerequisite upstream state (e.g., `createExecution()`) *before* the consumer aggregate creation call.

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

## Appendix — Deferred Test Types (T4/T5/T6)

These test types are documented for future work but are **not** part of the current workflow. They are preserved here for reference only.

---

### T4 — Cross-Functionality Test

**Purpose:** Validate that two concurrent operations on overlapping aggregates either produce a
consistent result or correctly reject one operation via semantic locks.

**Selection rule:** Create one T4 test for each pair (A, B) of functionalities that share at
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

### T5 — Fault / Behavior Test

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

### T6 — Async Test

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
