# Testing

Authoritative guide to test coverage in applications built on the microservices-simulator.
Four tiers, layered by what they exercise: the aggregate alone (T1), the service contract (T2),
event publication (T3), and saga orchestration + cross-aggregate consistency (T4).

## Test Taxonomy

| Tier | Name | Test class | Scope | Profile-agnostic? |
|------|------|-----------|-------|-------------------|
| **T1** | Aggregate | `<Aggregate>IntraInvariantTest` | Intra-invariants (P1): creation happy-path, one violation per non-`final` P1 rule (EP), boundary on/off-points (BVA). Direct construction + `verifyInvariants()`. | yes |
| **T2** | Service | `<Aggregate>ServiceTest` — **one class per aggregate**, all service methods | Service contract: change persisted to DB (read back via a **fresh** UnitOfWork), uniqueness / composite-key guards, not-found paths (Path A `SimulatorException` / Path B `<App>Exception`), P3 numeric-guard boundaries. Invoke the `*Service` bean directly with a `UnitOfWork` — no saga workflow. | yes |
| **T3** | Event Publication | `<Aggregate>EventPublicationTest` — only for aggregates that publish events | Publisher side: service op runs → event row exists in the event store with correct type + payload fields. Trigger **via service call**, assert against the event store. Abstracts away all consumers. | mostly |
| **T4** | Functionality | `<FunctionalityName>Test` + `<Consumer>InterInvariantTest` | Orchestration + cross-aggregate consistency: saga state-machine traversal (`NOT_IN_SAGA → IN_{OP} → NOT_IN_SAGA`), semantic-lock acquisition per lock step, P3 guard violations raised **through the saga path**, event subscription → cached-state update / unrelated-event-ignored / deletion-event cases. | no (sagas-specific) |

All test classes share the same skeleton, elided from the templates below:
`@DataJpaTest @Transactional @Import(LocalBeanConfiguration)`, extend `<AppName>SpockTest`, and
declare `@TestConfiguration static class LocalBeanConfiguration extends BeanConfigurationSagas {}`.

## Assertion Ownership

Each fact is asserted in **exactly one tier** — this is what prevents T2/T4 duplication:

- **T4 functionality tests do not assert** field-level persistence, uniqueness, or not-found —
  those belong to T2. A T4 happy path asserts orchestration outcomes only: the operation
  completes, the returned DTO is coherent, and `sagaStateOf(id) == NOT_IN_SAGA`.
- **T4 keeps:** lock-acquisition cases (`executeUntilStep`), P3 guard violations that involve
  cross-aggregate saga coordination, and all inter-invariant (subscription) tests.
- **T3 owns event-publication assertions.** T4 subscription tests may *trigger* publication via a
  functionality but must not re-assert event-store contents.
- **Known coupling:** `registerChanged` calls `verifyInvariants()`, so T2 fixtures can trip P1
  rules — T1 and T2 are not perfectly independent layers. Documented, not fixed.

## Fake / Wrong / Weak Detection Checklist

AI assistants given only a coverage metric will read the implementation they just wrote and mirror
it in assertions — tests that are trivially satisfied, break on correct refactors, and give false
security. This checklist is the authoritative smell list, consumed by
`.claude/skills/implement-aggregate/` and `.claude/skills/review-tests/SKILL.md`.

### Fake — always passes regardless of implementation correctness

- `then:` is only `noExceptionThrown()` with no field assertions — flag unless the scenario is explicitly "must not throw."
- `then:` checks only non-null / non-empty, or a trivially true condition, never actual values.
- `when:` does not call the method under test (bypasses it via a setup helper).
- **T2:** happy path reads back through the **same** UnitOfWork instance used for the write — the assertion never exercises the load path. Read-back must use a second, fresh UnitOfWork.
- **T4 subscription** "ignores unrelated": `originalValue` captured *after* the event was processed — the assertion is `x == x`. Capture in `given:`, before firing.

### Wrong — tests the wrong thing (implementation instead of spec, or a different code path)

- Test name / message constant copied verbatim from the implementation without checking `plan.md`'s rule list — the test validates the implementation deviation instead of catching it.
- `then:` mirrors the sequence of `set…` calls in the service body — derived from the implementation, not the spec.
- **T4 subscription** deletion-event test puts the post-deletion load in `then:` instead of an `and:` block — outside the exception-capture scope (see § T4).
- Not-found exception type contradicts the lookup mechanism — **read the service method first** (see § T2 Not-Found Paths). Flagging a correct Path B `thrown(<App>Exception)` as Fake is itself a Wrong finding.
- Test class missing `@Transactional` — dirty state bleeds between tests.
- P1 intra-invariant violation asserted in a T2/T4 test (belongs in `<Aggregate>IntraInvariantTest`).

### Weak — real scenario, under-specified assertions

- Happy-path `then:` asserts only fields already set in `setup:` — at least one asserted field must be a value the operation itself produces.
- Removing `unitOfWorkService.registerChanged(aggregate)` from the service would leave the test passing (kill-mutation thought experiment) — add an assertion readable only through the persisted aggregate.
- Violation test asserts `thrown(<App>Exception)` without `ex.message == <RULE_NAME>` — passes on any unrelated bug of that type.
- **T3:** asserts only that "an event exists" (type/count) without asserting the payload fields — a wrong-payload regression slips through.
- Returned DTO assertions cover only a subset of the semantically important fields.
- Ordered-domain boundary rule asserted with only a far-side value — missing the on-point **or** off-point (see § Choosing Input Values).

## Choosing Input Values — EP & BVA

- **Equivalence Partitioning (EP)** — split a rule's input domain into classes treated the same way; test **one representative per class** (one satisfying value, one violating value).
- **Boundary Value Analysis (BVA)** — defects cluster at class **edges** (off-by-one, `<` vs `<=`). For ordered domains, also test the two values **straddling** the boundary.

### Decision rule

> For every P1 invariant or P3 numeric guard whose predicate is a **comparison on an ordered
> domain** (`<`, `<=`, `>`, `>=`, `==`/`!=` over a count, timestamp, or collection size), EP's
> single representative is **not sufficient**. Add the boundary-straddling pair:
> **on-point** — last value that satisfies the rule → `notThrown(...)`;
> **off-point** — first value that violates it → `thrown(...)` **and** `ex.message == <RULE>`.
>
> Routing: **P1 boundaries → T1**; **P3 numeric-guard boundaries → T2**.
>
> For **categorical** invariants — uniqueness, boolean/state freezes, set membership — there is no
> ordered edge; EP's one-representative-per-class is complete. Do **not** invent boundary cases.

### Worked patterns

| Rule shape | Example invariant | On-point (no throw) | Off-point (throws) |
|---|---|---|---|
| `count > 0` | `NUMBER_OF_QUESTIONS_POSITIVE` | `1` | `0` |
| `count <= N` | `MAX_QUESTIONS` (N=30) | `30` | `31` |
| `a < b` (timestamps) | `START_BEFORE_END_TIME` | `end − 1 tick` | `start == end` |
| `a >= b` (timestamps) | `ANSWER_BEFORE_START` | `firstAnswerTime == startTime` | `startTime − 1 tick` |
| `size >= 1` | `MUST_HAVE_ONE_TOPIC` | `1` | `0` |

**Temporal mechanics:** the smallest `LocalDateTime` tick is `.minusNanos(1)` / `.plusNanos(1)`;
pin **both** instants explicitly so the on-point is exactly equal. All temporal P1 boundary cases
are T1 direct-aggregate tests — the saga path stamps `lastModifiedTime = now()` and cannot pin the
on-point. Canonical pattern: `TournamentIntraInvariantTest` (`ANSWER_BEFORE_START`).

## Spec-First Ordering

Before writing any test, locate the **`plan.md` aggregate section** for the target aggregate. Its
happy-path postconditions, events-published list, subscribed-events table, and P1/P3 rule list
*are* the spec — assertions must trace to them, never to the implementation just written (not the
service body, not the `EventProcessing` class). Write a 1-line `// Spec:` comment at the top of
each test naming the plan.md section and rule, e.g.
`// Spec: plan.md §3.5 Question — UpdateQuestionContent; rule QUESTION_CONTENT_REQUIRED`.
If the implementation disagrees (e.g. throws a different message constant than plan.md names), the
**implementation** is the bug: flag the mismatch, do not adjust the test.

## Directory Layout

```
src/test/groovy/<pkg>/
├── BeanConfigurationSagas.groovy     ← test configuration (infrastructure beans)
├── SpockTest.groovy                  ← root Spock marker class
├── <AppName>SpockTest.groovy         ← base class: @Autowired services + factory helpers
└── sagas/
    ├── coordination/
    │   └── <aggregate>/              ← one dir per primary aggregate
    │       └── <FunctionalityName>Test.groovy          (T4)
    └── <aggregate>/                  ← one dir per aggregate
        ├── <Aggregate>IntraInvariantTest.groovy        (T1)
        ├── <Aggregate>ServiceTest.groovy               (T2)
        ├── <Aggregate>EventPublicationTest.groovy      (T3, publishers only)
        └── <Aggregate>InterInvariantTest.groovy        (T4, consumers only)
```

## T1 — Aggregate Test

**Purpose:** own the complete P1 matrix (see taxonomy table). All cases are **direct-aggregate**:
construct, optionally call aggregate mutators, then call `verifyInvariants()` explicitly — the
saga path cannot pin exact boundary instants. Happy-path fields must trace to the aggregate field
list in plan.md (a constructor field the spec doesn't list is a planning gap to flag, not a field
to copy). **P1 Java-`final` fields need no test coverage in any tier** — the compiler enforces
immutability; testing it tests the language, not the domain.

```groovy
class <Aggregate>IntraInvariantTest extends <AppName>SpockTest {

    def "create <aggregate>"() {
        when:
        def result = new Saga<Aggregate>(/* id, args or dto */)
        then:
        result.<field> == <expectedValue>   // all fields from plan.md's field list
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

    // Boundary pair per ordered-domain P1 rule — same shape as the violation case:
    //   on-point:  pin the last satisfying value → notThrown(<App>Exception)
    //   off-point: pin the first violating value → thrown + ex.message == <RULE_NAME>
}
```

## T2 — Service Test

**Purpose:** pin the service contract (see taxonomy table), one class per aggregate covering all
its service methods, invoked directly on the `*Service` bean with a `UnitOfWork` — no saga
workflow. No explicit commit is needed: in the sagas profile, `registerChanged` versions,
invariant-checks, and merges the aggregate **inside the service call**; the workflow-level
`commit(uow)` only resets `SagaState`. **Read-back must still use a fresh UnitOfWork** so the
assertion goes through the load path — reading via the write UoW is Fake.

```groovy
class <Aggregate>ServiceTest extends <AppName>SpockTest {

    def "create<Aggregate>: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §<n> <Aggregate> — Create<Aggregate> postconditions
        when:
        def dto = <aggregate>Service.create<Aggregate>(/* args */,
                unitOfWorkService.createUnitOfWork("create<Aggregate>"))
        then: 'read back through a second, fresh UnitOfWork'
        def readBack = <aggregate>Service.get<Aggregate>ById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.<field> == <expectedValue>
    }

    def "<serviceMethod>: <RULE_NAME> violation"() {
        // Spec: plan.md §<n> <Aggregate> — rule <RULE_NAME> (P3 guard / uniqueness)
        given:
        def existing = create<Aggregate>(/* fixture via base-class helper */)
        when:
        <aggregate>Service.<serviceMethod>(/* violating args */,
                unitOfWorkService.createUnitOfWork("<serviceMethod>"))
        then:
        def ex = thrown(<App>Exception)
        ex.message == <RULE_NAME>
    }

    // P3 numeric-guard boundaries: on-point (notThrown) / off-point (thrown + message) pairs.
    // Not-found per read/mutate method: call with NONEXISTENT_AGGREGATE_ID →
    //   Path A: thrown(SimulatorException); Path B: thrown(<App>Exception) + message (see below).
}
```

### Not-Found Paths

Two distinct not-found paths throw different exception types — **read the service method first**:

- **Path A — primary-key lookup.** The service calls `aggregateLoadAndRegisterRead` directly with
  an ID; the infrastructure throws `SimulatorException`
  (`pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException`).
- **Path B — composite-key lookup.** The service first queries a custom repository returning
  `Optional` (e.g. `quizId + userId`) and throws on empty at the service level: expect
  `<App>Exception` with `ex.message == <NOT_FOUND_CONSTANT>`.

## T3 — Event Publication Test

**Purpose:** pin the publisher side of every event (see taxonomy table); consumers are out of
scope (they are T4 subscription tests). Trigger via a direct service call with a `UnitOfWork` —
`SagaUnitOfWorkService.registerEvent` saves the event immediately via
`eventService.saveEvent(event)` (and marks it published under the `local` profile). Assert via the
`EventService` bean (`pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService`): per event
type, one payload-asserting case (type/count alone is Weak), plus one negative case — an operation
that must *not* publish leaves the store unchanged.

```groovy
class <Aggregate>EventPublicationTest extends <AppName>SpockTest {

    @Autowired
    EventService eventService

    def "<serviceOp> publishes <Xxx>Event with correct payload"() {
        // Spec: plan.md §<n> <Aggregate> — events published by <ServiceOp>
        given:
        def publisher = create<Aggregate>(/* fixture via base-class helper */)
        when:
        <aggregate>Service.<serviceOp>(publisher.aggregateId, /* args */,
                unitOfWorkService.createUnitOfWork("<serviceOp>"))
        then:
        def events = eventService.getAllEvents().findAll { it instanceof <Xxx>Event }
        events.size() == 1
        def event = events[0] as <Xxx>Event
        event.publisherAggregateId == publisher.aggregateId
        event.<payloadField> == <expectedValue>   // every payload field from plan.md
    }

    // Negative case: capture countBefore = eventService.getAllEvents().size() in given:,
    // run the non-publishing service op, assert getAllEvents().size() == countBefore.
}
```

## T4 — Functionality Test

Every write saga is a finite state machine over its aggregate's `SagaState`: **states** —
`NOT_IN_SAGA` (quiescent) plus one `IN_{OP}` *locked* state per operation holding a semantic lock
across steps; **transitions** — *acquire* (`setSemanticLock(IN_{OP})` drives
`NOT_IN_SAGA → IN_{OP}`), *complete* (successful commit drives `IN_{OP} → NOT_IN_SAGA`),
*compensate* (mid-saga failure also releases to `NOT_IN_SAGA`); **guards** —
`setForbiddenStates([...])` blocks a transition when a foreign aggregate is in a listed state.
Testing a write saga means covering its transitions: the happy-path `success` case covers the full
traversal; one lock-acquisition case per `setSemanticLock` step pins the intermediate `IN_{OP}`
state, then resumes to complete. Guard transitions and compensation faults are deferred — see
Appendix. Everything else is owned elsewhere (see § Assertion Ownership).

```groovy
class <FunctionalityName>Test extends <AppName>SpockTest {

    def "<functionalityName>: success"() {
        given: 'aggregates exist'
        // ...
        when:
        def result = <primary>Functionalities.<functionalityName>(/* args */)
        then: 'orchestration outcome only — persistence is asserted in T2'
        result.<keyField> == <coherentValue>
        sagaStateOf(<aggregateId>) == GenericSagaState.NOT_IN_SAGA
    }

    // Saga-path P3 guard violations: same shape as the T2 violation case,
    // but driven through <primary>Functionalities.<functionalityName>(...).

    // One case per saga step that calls setSemanticLock — the acquire transition
    def "<functionalityName>: <lockStep> acquires IN_<OP> semantic lock"() {
        given:
        def uow = unitOfWorkService.createUnitOfWork("<FunctionalityName>")
        def func = new <FunctionalityName>FunctionalitySagas(
                unitOfWorkService, /* args */, uow, commandGateway)
        func.executeUntilStep("<lockStep>", uow)
        expect: 'acquire transition: NOT_IN_SAGA → IN_<OP>'
        sagaStateOf(<aggregateId>) == <Aggregate>SagaState.IN_<OP>
        when:
        func.resumeWorkflow(uow)
        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }
}
```

### Subscription (Inter-Invariant) Tests

`<Consumer>InterInvariantTest` covers the consumer side: event received → cached state updated;
unrelated event → state unchanged; deletion event → consumer deleted. `@Scheduled` does **not**
run in `@DataJpaTest` — call the polling method directly:
`<consumer>EventHandling.handle<Xxx>Events()`. These tests trigger publication via a functionality
but must not re-assert event-store contents (T3 owns that). If the consumer DTO does not expose a
cached sub-entity field, load the aggregate via `aggregateLoadAndRegisterRead` and assert on
`agg.<subEntity>.<cachedField>`.

**Deletion events:** when processing calls `remove()` on the consumer, `aggregateLoadAndRegisterRead`
filters out `DELETED` aggregates and throws `SimulatorException` — the load-and-assert pattern
cannot work. Move the load attempt into an `and:` block (extending the `when:` phase's
exception-capture scope, which a `then:`-placed load would sit outside of), then assert
`thrown(SimulatorException)`.

```groovy
class <Consumer>InterInvariantTest extends <AppName>SpockTest {

    @Autowired
    <Consumer>EventHandling <consumer>EventHandling

    def "<consumer> <action> on <Xxx>Event"() {
        // <action> is a concrete verb: updates, removes, deletes self, anonymizes, invalidates self
        given:
        def publisher = create<Publisher>(/* args */)
        def consumer  = create<Consumer>(/* args linked to publisher */)
        when: '<publisher> triggers the event'
        <publisher>Functionalities.<triggeringOp>(/* args */)
        and: 'consumer polls for the event'
        <consumer>EventHandling.handle<Xxx>Events()
        then: 'consumer cached field is updated'
        <consumer>Service.get<Consumer>(consumer.aggregateId,
                unitOfWorkService.createUnitOfWork("check")).<cachedField> == <newValue>
    }

    // "ignores <Xxx> event for unrelated entity" twin: create a second publisher,
    // capture originalValue in given: BEFORE firing (capturing after is Fake — x == x),
    // trigger the op on publisher2, poll, assert consumer.<cachedField> == originalValue.

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
        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }
}
```

## Test Profile — Serialization Note

With `local.messaging.serialize: true` (test profile), commands round-trip through Jackson.
`CommandResponse.result` is typed `Object`, so a `List<XxxDto>` stored there deserializes as
`LinkedHashMap` elements unless `MessagingObjectMapperProvider.useForType()` returns `true` for
`raw == Object.class`, which embeds `@class` metadata per element. Without that fix, read sagas
returning lists fail with `ClassCastException`. Irrelevant when the flag is off.

## Appendix — Deferred Test Types

Documented for future work; **not** part of the current workflow.

### Cross-Functionality Test

Two concurrent operations on overlapping aggregates must either produce a consistent result or
correctly reject one via semantic locks (`setForbiddenStates` guard transitions). One test per
functionality pair sharing an aggregate with real consistency risk.

```groovy
def "concurrent: <op1> step1 → <op2> completes → <op1> resumes"() {
    given: 'func1 = new <Op1>FunctionalitySagas(unitOfWorkService, /* args */, uow1, commandGateway)'
    when: 'func1.executeUntilStep("<stepBeforeShared>", uow1); <primary2>Functionalities.<op2>(...); func1.resumeWorkflow(uow1)'
    then: 'both succeed consistently, or one throws a meaningful exception'
    noExceptionThrown()  // or: thrown(<App>Exception)
}
```

### Fault / Behavior Test

Saga compensation when a step fails mid-workflow: inject a fault via `ImpairmentService`, then
verify the aggregate is fully applied or fully rolled back.

```groovy
def "<functionality>: step <faultStep> fails → aggregate rolls back"() {
    given: 'impairmentService.addCommandImpairment("<FunctionalityName>", "<faultStep>", ImpairmentType.EXCEPTION_ON_EXECUTE)'
    when: '<primary>Functionalities.<functionalityName>(/* args */)'
    then:
    thrown(Exception)
    and: 'read back via a fresh UnitOfWork: <mutatedField> == <originalValue>'
}
```

### Async Test

N concurrent invocations of the same operation must all complete without corrupting state.

```groovy
def "<functionality>: N concurrent invocations all succeed"() {
    given: 'one shared aggregate + N participants'
    when: 'fire all concurrently'
    participants.collect { p ->
        Thread.start { <primary>Functionalities.<functionalityName>(shared.aggregateId, p.aggregateId) }
    }*.join()
    then: 'read back via a fresh UnitOfWork: <collection>.size() == N'
}
```
