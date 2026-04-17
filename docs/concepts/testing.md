# Testing

This document is the authoritative guide to test coverage in applications built on the
microservices-simulator. It defines the Test Taxonomy, locates each type in the workflow,
and provides structure templates for each category.

---

## Test Taxonomy

Six test types exist, spread across Phases 2–5. Every application must have all six.

| Type | Phase | Naming pattern | What it validates |
|------|-------|----------------|-------------------|
| **T1 Creation** | 2 | `<Aggregate>Test` | Aggregate instantiation + all intra-invariants pass on a fresh instance |
| **T2 Functionality** | 3 | `<FunctionalityName>Test` | Happy path · invariant/guard violations · ≥1 step-interleaving case per saga step boundary |
| **T3 Inter-Invariant** | 4 | `<Consumer>InterInvariantTest` | Event received → cached state updated; unrelated event → state unchanged |
| **T4 Cross-Functionality** | 5 | `<Op1>And<Op2>Test` | Two operations on overlapping aggregates, step-interleaved to expose semantic lock conflicts |
| **T5 Fault/Behavior** | 5 | `<Functionality>FaultTest` / `<Functionality>RecoveryTest` | Saga compensation correctness under injected failures; consistent state after abort |
| **T6 Async** | 5 | `<Functionality>AsyncTest` | Multiple concurrent fire-and-forget executions without step coordination |

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
    ├── <aggregate>/                  ← one dir per consumer aggregate
    │   ├── <Aggregate>Test.groovy                  (T1)
    │   └── <Aggregate>InterInvariantTest.groovy    (T3)
    └── behaviour/                    ← Phase 5 tests
        ├── <Op1>And<Op2>Test.groovy                (T4)
        ├── <Functionality>FaultTest.groovy          (T5)
        ├── <Functionality>RecoveryTest.groovy       (T5)
        ├── <Functionality>AsyncTest.groovy          (T6)
        └── <aggregate>/              ← sub-folder if many behaviour tests for one aggregate
```

---

## T1 — Creation Test

**When:** Written by `/scaffold-aggregate`. Runs at end of Phase 2.

**Purpose:** Prove the aggregate can be instantiated with valid state and that all intra-invariants
are enforced on a fresh instance.

**Template:**
```groovy
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class <Aggregate>Test extends <AppName>SpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create <aggregate> with valid data"() {
        when:
        def result = <aggregate>Functionalities.create<Aggregate>(/* valid args */)

        then:
        result != null
        result.<field> == <expectedValue>
    }

    def "create <aggregate> with blank <field> throws exception"() {
        when:
        <aggregate>Functionalities.create<Aggregate>(/* invalid arg */)

        then:
        def ex = thrown(<App>Exception)
        ex.message == <RULE_NAME>
    }
    // one 'throws exception' case per intra-invariant
}
```

---

## T2 — Functionality Test

**When:** Written by `/implement-functionality`. Runs at end of Phase 3.

**Purpose:** Cover the happy path, every invariant/guard violation, and the concurrent-interleaving
cases that validate semantic locks at each saga step boundary.

**Step-interleaving rule:**
- For each saga step that reads a foreign aggregate (Layer 3 `setForbiddenStates`), add one
  interleaving case where a conflicting operation locks the foreign aggregate *between* steps.
- Use `executeUntilStep("stepName", uow)` to pause before the named step, then
  `resumeWorkflow(uow)` to continue after injecting the conflict.

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

    // ─── Concurrent interleaving ───────────────────────────────────────────────
    // One case per saga step that declares setForbiddenStates

    def "<functionalityName>: step <stepName> sees forbidden state"() {
        given:
        def uow1 = unitOfWorkService.createUnitOfWork("<FunctionalityName>")
        def func1 = new <FunctionalityName>FunctionalitySagas(
                unitOfWorkService, /* args */, uow1, commandGateway)

        when: 'first workflow pauses before <stepName>'
        func1.executeUntilStep("<stepName>", uow1)

        and: 'conflicting operation runs and completes'
        <conflicting>Functionalities.<conflictingOp>(/* args */)

        and: 'first workflow resumes into the forbidden state'
        func1.resumeWorkflow(uow1)

        then:
        def ex = thrown(<App>Exception)
        ex.message == <RULE_NAME>
    }
}
```

---

## T3 — Inter-Invariant Test

**When:** Written or extended by `/wire-event`. Runs at end of Phase 4.

**Purpose:** Verify that when a publisher emits an event, the consumer aggregate's cached state
is updated; and that an event for an unrelated entity leaves state unchanged.

**Key constraint:** `@Scheduled` does **not** run in `@DataJpaTest`. Call the polling method
directly: `<consumer>EventHandling.handle<Xxx>Events()`.

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

    def "<consumer> reflects <Xxx> event"() {
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

        when:
        <publisher>Functionalities.<triggeringOp>(publisher2.aggregateId, /* args */)
        <consumer>EventHandling.handle<Xxx>Events()

        then: 'consumer state unchanged'
        def unchanged = <consumer>Service.get<Consumer>(consumer.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        unchanged.<cachedField> == <originalValue>
    }
}
```

---

## T4 — Cross-Functionality Test

**When:** Written in Phase 5.

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
        func1.executeUntilStep("<sharedStep>", uow1)

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

## T5 — Fault / Behavior Test

**When:** Written in Phase 5.

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

## T6 — Async Test

**When:** Written in Phase 5.

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

---

## Phase 5 — How to Derive the Test List

When populating Phase 5 of `plan.md`, follow this procedure:

### T4 selection

1. Build a map: `aggregate → [functionalities that read or mutate it]`
2. For each aggregate that appears in ≥2 functionalities, enumerate pairs (A, B) where concurrent
   execution poses a real consistency risk.
3. Create one `<A>And<B>Test` per high-risk pair. At minimum cover:
   - The aggregate that is most shared (appears in the most functionalities)
   - Every pair where A mutates a field that B reads

### T5 selection

1. Identify every functionality whose Sagas workflow has ≥2 steps.
2. Create one `<Functionality>FaultTest` per such functionality (fault the first mutating step).
3. Optionally add a `<Functionality>RecoveryTest` for complex sagas with compensating logic.

### T6 selection

1. Identify the 2–3 operations most likely to be invoked concurrently in production
   (typically: add-participant-style operations, enrollment operations).
2. Create one `<Functionality>AsyncTest` per selected operation.
