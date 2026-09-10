# Retro — 2.2.a — TrainType

**App:** trainticket
**Session:** 2.2.a (Domain Layer)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/TrainType.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/TrainTypeDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/TrainTypeFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/TrainTypeCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/TrainTypeRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/sagas/SagaTrainType.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/sagas/states/TrainTypeSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/sagas/factories/SagasTrainTypeFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/sagas/repositories/TrainTypeCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/TrainTypeServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/traintype/TrainTypeIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | The JDK-21 note (harness-log row 2) saved the `release version 21 not supported` failure from being misread as a scaffold bug. |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Neutral domain, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | all | Partial | Silent on realising a P1 `final` field under JPA - see Documentation Gaps. |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants, § Factories, § Repositories | Partial | Same gap: the worked aggregate has no immutable field. |
| `docs/concepts/testing.md` | § T1 — Aggregate Test, § Choosing Input Values — EP & BVA | Yes | The routing rule (P1 boundaries → T1) settled all six straddle pairs without inference. |

---

## Skill Instructions Feedback

### What worked well

- § "Domain enums" correctly produced nothing: TrainType has no enum-typed §1 attribute, and the rule
  "an aggregate with no enum-typed field has no such row and produces none" made that a decision
  rather than an omission to second-guess.
- § `{Aggregate}SagaState.java` is precise about which states exist in session `a`. TrainType is a
  cross-aggregate prerequisite for two later aggregates, so `READ_TRAIN_TYPE` is present; the
  `IN_UPDATE`/`IN_DELETE` states are correctly left to session `c` to add if its sagas need them.
- § `{Aggregate}CustomRepositorySagas.java`'s "leave the class body empty" removed the temptation to
  pre-invent a bulk-read query that session `b` will specify.

### What was unclear or missing

- Neither `session-a.md` § `{Aggregate}.java` nor `docs/concepts/aggregate.md` says how an aggregate
  field whose P1 rule is "immutable (Java `final`)" is actually written. The prescribed template has
  three constructors (no-arg, creating-from-DTO, copy) and getters/setters for "all mutable fields",
  which implies a `final` field is assigned directly in all three and gets no setter - but the
  implication is left to the reader, and the interaction with Hibernate hydration is not addressed at
  all. Both were verified mechanically here rather than read off the harness.

### Suggested wording / structure changes

- In `.claude/skills/implement-aggregate/session-a.md` § `{Aggregate}.java`, after the constructor
  bullet, add: a field whose P1 rule marks it immutable is declared `private final`, assigned by
  direct field write in every constructor including the no-arg JPA one (`this.{field} = null;`), and
  given a getter but no setter. Hibernate's field-access strategy hydrates it reflectively, so the
  `final` costs nothing at the persistence layer.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md`, `docs/concepts/aggregate.md` | How to realise a P1 `final` field: which constructors assign it, that it has no setter, and that JPA hydration of a `final` field is safe. | High - recurs on Trip (`tripNumber`), User (`userName`) and Order (twelve frozen fields), so an unstated convention will be re-derived at least three more times, possibly inconsistently. | Add the bullet proposed under § Suggested wording. |

---

## Patterns to Capture

- **Pattern:** Immutable aggregate field as a Java `final`
  **Observed in:** `microservices/traintype/aggregate/TrainType.java`
  **Description:** A P1 rule of the form "`{Aggregate}.{field}` is immutable (Java `final` field)" is
  realised as `private final` with a direct field write in each of the three constructors and no
  setter, rather than as a runtime check in `verifyInvariants()`. `verifyInvariants()` carries no
  clause for it and T1 has no case for it - the compiler is the enforcement. Confirmed by a
  throwaway persistence probe that Hibernate 6.6 round-trips such a field under the field-access
  strategy the base `Aggregate` selects by annotating `@Id` on a field.

- **Pattern:** Throwaway probe to demote a Type 2 to a settled fact
  **Observed in:** session 2.2.a (probe written, run, deleted; not committed)
  **Description:** Where the harness is silent but the candidate answer is mechanically checkable, a
  temporary `@DataJpaTest` that exercises the uncertain mechanism converts an apparent design
  decision into a demonstrated one, avoiding a halt. Only valid when the probe genuinely decides the
  question; a probe that merely fails to disprove is not an answer.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 12

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 12 | 2 | deferred | - |

---

## One-Line Summary

The harness has no stated pattern for a P1 `final` field, which this application needs four separate
times; it was settled here by persistence probe rather than by doc, and the answer is still unwritten.
