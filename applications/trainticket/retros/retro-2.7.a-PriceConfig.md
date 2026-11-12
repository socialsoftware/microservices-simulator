# Retro - 2.7.a - PriceConfig

**App:** trainticket
**Session:** 2.7.a (Domain Layer)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfig.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfigDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfigFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfigCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfigRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/sagas/SagaPriceConfig.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/sagas/states/PriceConfigSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/sagas/factories/SagasPriceConfigFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/sagas/repositories/PriceConfigCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/PriceConfigServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/priceconfig/PriceConfigIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | The JDK-21 warning was the exact failure hit on the first build; the shell defaulted to 17 and the compiler error read as a scaffold bug, as the doc predicts. |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | - |
| `.claude/skills/implement-aggregate/session-a.md` | all | Partial | § "{Aggregate}SagaState.java" - see Skill Instructions Feedback. |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants (Sagas), § Factories, § Repositories | Yes | § getEventSubscriptions() skipped - PriceConfig subscribes to nothing. |
| `docs/concepts/testing.md` | § T1 - Aggregate Test, § Choosing Input Values - EP & BVA, § Assertion Ownership, § Fake / Wrong / Weak | Partial | No BVA tick defined for a decimal domain; see harness-log row 21. |
| `applications/trainticket/plan.md` | §3.1 rule table, §7 PriceConfig | Yes | - |
| `applications/trainticket/trainticket-domain-model.md` | §1 attributes, §2 reference table, §3.1 PRICE_RATES_POSITIVE | Yes | Confirmed both cached ids are immutable, so both are Java `final`. |

---

## Skill Instructions Feedback

### What worked well

- § "Saga{Aggregate}.java" states the two-constructor rule and the reason the copy constructor must
  inherit `sagaState` rather than reset it. Both constructors were written correctly first time, and
  the `@Convert(converter = SagaStateConverter.class)` warning meant the EntityManagerFactory init
  failure it describes never occurred.
- § "{Aggregate}CustomRepositorySagas.java" mandating an empty body in session `a` was right for this
  aggregate: the only custom query PriceConfig needs is the composite-key lookup that session `b`'s
  `GetPriceConfigByRouteAndTrainType` introduces, and it could not have been specified now.
- The "Verify Mandatory Files in plan.md" step was a no-op here - plan.md listed both interfaces -
  but checking it cost nothing.

### What was unclear or missing

- § "{Aggregate}SagaState.java" splits the locked states across sessions (`READ_` in `a`, `IN_UPDATE_`
  / `IN_DELETE_` in `c` when the write saga needs them), but the plan.md `2.7.c` row for PriceConfig
  does not list the states file at all, while the `2.6.c` row for the previous aggregate does list it
  as "added 2.6.c". Read alone, the 2.7 rows suggest session `a` should emit the locked states up
  front. The enum was first written with all three states and then trimmed to `READ_PRICE_CONFIG`,
  which is what the skill actually prescribes; session `c` will have to amend its own plan row.
- § "Error message constants" and § "Update {AppClass}SpockTest.groovy" both mandate edits to files
  that no `2.{N}.a` plan row lists, so every session-`a` run produces the same two plan.md
  amendments. The amendment mechanism works, but the repetition suggests the omission is in
  `/classify-and-plan`'s row template rather than in any single plan.

### Suggested wording / structure changes

- `.claude/skills/classify-and-plan/SKILL.md`: have the `2.{N}.a` row template always include
  `{src}microservices/exception/{AppClass}ErrorMessage.java` and `{test}{AppClass}SpockTest.groovy`
  for any aggregate with at least one P1 rule, since `session-a.md` mandates both unconditionally.

---

## Semantic-Lock Coverage Audit (sessions `c` only)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` | § "Choosing Input Values - EP & BVA" defines the boundary tick for integer counts and for `LocalDateTime`, but not for a decimal-valued quantity, where the domain has no smallest positive value and the on-point can only come from the persisted column's scale. | Medium | Add a row to the "Worked patterns" table for `amount > 0` over a fixed-scale decimal, naming the column scale as the source of the on-point. |
| `.claude/skills/implement-aggregate/session-a.md` | § "{Aggregate}SagaState.java" is correct but is contradicted by plan rows that do not re-list the states file for session `c`; nothing tells the session-`a` agent which of the two to trust. | Low | State that the states enum is expected to grow in session `c` whether or not the `2.{N}.c` row lists it. |

---

## Patterns to Capture

- **Pattern:** Conjunctive P1 rule over several fields gets one violation case per conjunct
  **Observed in:** `applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/priceconfig/PriceConfigIntraInvariantTest.groovy`
  **Description:** `PRICE_RATES_POSITIVE` is a single named rule whose predicate ANDs two independent
  field comparisons. One violation case per rule would leave either conjunct free to be dropped from
  `verifyInvariants()` without any test failing, so the T1 matrix carries a violation case and a
  boundary pair per conjunct, all asserting the same message constant. `testing.md` § T1 counts cases
  per rule, not per conjunct, so this is an extrapolation.

- **Pattern:** Null-tolerant predicate helper in `verifyInvariants()`
  **Observed in:** `applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfig.java`
  **Description:** A boxed numeric field can be null before its setter runs, so the positivity check
  is expressed as a private `isPositive(BigDecimal)` folding the null case into the same throw rather
  than repeating a null guard per field. Matches how the previous aggregate folds nulls into
  `TRIP_START_BEFORE_END`.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 21

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 21 | 2 | deferred | - |

---

## One-Line Summary

PriceConfig's domain layer landed unchanged from the session-`a` template, and the only real friction
was that the harness defines BVA boundary ticks for integers and timestamps but not for the decimal
rates this aggregate's single P1 rule compares against zero.
