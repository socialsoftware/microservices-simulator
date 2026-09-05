# Retro — 2.3.a — User

**App:** trainticket
**Session:** 2.3.a (Domain Layer)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/enums/DocumentType.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/Gender.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/User.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/UserDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/UserFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/UserCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/UserRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/sagas/SagaUser.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/sagas/states/UserSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/sagas/factories/SagasUserFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/sagas/repositories/UserCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/UserServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java` (modified — added `USER_DOCUMENT_NUMBER_PRESENT`)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/user/UserIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (modified — User constants and their imports)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy` (modified — factory and custom-repository beans)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (modified — 2.3.a checkbox and file-row amendment)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md` (modified — row 15)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | JDK 21 selection was needed; shell default is still 17. |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | all sections except § getEventSubscriptions | Partial | § "Domain enums" covers the enum file but not the JPA mapping of the enum-typed aggregate field. |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants (Sagas), § Factories, § Repositories | Yes | § getEventSubscriptions skipped — User subscribes to nothing. |
| `docs/concepts/testing.md` | § T1, § Assertion Ownership, § Choosing Input Values (EP & BVA), § Spec-First Ordering | Yes | The EP/BVA decision rule settled the no-boundary-cases question for USER_DOCUMENT_NUMBER_PRESENT cleanly. |
| `.claude/skills/_shared/session-completion.md` | all | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- § "Saga{Aggregate}.java" states the `@Entity`, the `@Convert` and the two constructors' differing
  `sagaState` seeding explicitly enough that `SagaUser` needed no inference. Rows 6 and its fix are
  visibly paying off.
- § "{Aggregate}SagaState.java" gave a mechanical test for `READ_{AGGREGATE}` ("other aggregates use
  this aggregate as a cross-aggregate prerequisite"). plan.md answers it directly and negatively for
  User — Contacts and Order both hold a `userAggregateId` but neither performs a saga fetch — so
  `READ_USER` was correctly omitted rather than copied from the two preceding aggregates, which both
  have one.
- § "Domain enums" `(shared)` rule plus plan.md's § Application-wide facts made the `DocumentType`
  placement unambiguous: one file at the app source root, no per-package copy.
- `docs/concepts/testing.md` § "Choosing Input Values" is explicit that categorical rules get no
  boundary pair. USER_DOCUMENT_NUMBER_PRESENT is an implication over a set-membership test, so the
  T1 matrix is three cases (two violating representatives, one vacuous-antecedent satisfied case)
  and no straddle was invented.

### What was unclear or missing

- § "Domain enums" says the enum file carries "no JPA annotations" but is silent on how the
  aggregate *field* typed by that enum is mapped. `@Enumerated(EnumType.STRING)` and JPA's
  `ORDINAL` default both compile and both round-trip, so the build cannot distinguish them and the
  gap is invisible to verification. Recorded as harness-log row 15.

### Suggested wording / structure changes

- Add one line to `.claude/skills/implement-aggregate/session-a.md` § "Domain enums": the aggregate
  field typed by a domain enum is annotated `@Enumerated(EnumType.STRING)`, so persisted rows do not
  depend on the declaration order of the enum constants.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | § "Domain enums" does not state the JPA mapping of an enum-typed aggregate field | Medium — silently accepts `ORDINAL`, which couples stored rows to enum declaration order; recurs for Contacts, Trip, PriceConfig, Order | Prescribe `@Enumerated(EnumType.STRING)` on the field |
| `.claude/skills/implement-aggregate/session-a.md`, `docs/concepts/aggregate.md` | Still no statement of how a Java-`final` aggregate field coexists with JPA hydration and the mandated copy constructor (open row 12) | Medium — recurred here for `User.userName`; will recur for Trip and Order | Document the no-arg-constructor + field-access pattern once |

---

## Patterns to Capture

- **Pattern:** Implication-shaped P1 rule gets three T1 cases, not a boundary pair
  **Observed in:** `applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/user/UserIntraInvariantTest.groovy`
  **Description:** For a P1 of the form `{field} != {SENTINEL} ⟹ {otherField} is present`, the T1
  matrix is one case per violating representative of the consequent (null and blank are distinct
  code paths through `isBlank()`) plus one case pinning the vacuous antecedent — the state where the
  sentinel value makes the violating consequent legal. The vacuous case is what a predicate written
  without the antecedent guard fails, and no ordered boundary exists to straddle.

- **Pattern:** `READ_{AGGREGATE}` is omitted when the spec forecloses cross-aggregate reads
  **Observed in:** `applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/sagas/states/UserSagaState.java`
  **Description:** A reference arrow from another aggregate is not by itself a reason to emit
  `READ_{AGGREGATE}`. The test is whether some other aggregate's write saga sends a
  `Get{Aggregate}ByIdCommand`. When the plan states the reference is stored unvalidated, the state
  would be dead code and is left out — the first aggregate in this application where the two
  preceding ones' shape does not carry over.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 15

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 15 | 2 | deferred | - |

---

## One-Line Summary

The domain layer landed with no contradictions, and the one gap worth acting on is that the harness
never says how an enum-typed aggregate field is mapped to JPA — a silence the build cannot expose,
and one that four more aggregates will hit.
