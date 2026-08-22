# Retro — 2.1.a — Station

**App:** trainticket
**Session:** 2.1.a (Domain Layer)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/Station.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/StationDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/StationFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/StationCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/StationRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/sagas/SagaStation.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/sagas/states/StationSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/sagas/factories/SagasStationFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/sagas/repositories/StationCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/StationServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java` (appended `STATION_STAY_TIME_NON_NEGATIVE`)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/station/StationIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (four Station constants)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy` (two beans)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (2.1.a ticked)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | JDK 21 note was needed - the shell defaults to 17. |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | all | Partial | § `Saga{Aggregate}.java` omits `@Entity` (harness-log row 6). |
| `.claude/skills/classify-and-plan/SKILL.md` | § Step 7 path roots | Yes | Resolved where `{Aggregate}ServiceApplication.java` lands. |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants, § Factories, § Repositories | Yes | — |
| `docs/concepts/testing.md` | § T1, § Choosing Input Values, § Assertion Ownership, § Spec-First Ordering | Yes | — |
| `docs/concepts/service.md` | § Method Patterns, § Exception-Throw Convention | Yes | Read to pin the DTO surface the service layer will call. |
| `docs/concepts/rule-enforcement-patterns.md` | § P3, § P4a | Partial | Used only to infer the DTO surface (`isActive()`, `getAggregateId()`); no doc states the DTO shape directly. |

---

## Skill Instructions Feedback

### What worked well

- The § `Saga{Aggregate}` copy-constructor rule (inherit `other.getSagaState()`, never reset to
  `NOT_IN_SAGA`) is stated with its failure mode, so it needed no reasoning to apply.
- § `{Aggregate}SagaState.java` decides the enum contents from the saga shape: Station's writes are
  all two-step (read -> write-as-final-step) and Route fetches Station as a P4a prerequisite, so
  `READ_STATION` is the only member. No guessing was required.
- The plan.md 2.1.a row already listed `StationFactory.java` and `StationCustomRepository.java`, so
  the § "Verify Mandatory Files" check passed with no amendment.

### What was unclear or missing

- No doc states the `{Aggregate}Dto` field surface. § `{Aggregate}Dto.java` says only "fields
  matching the aggregate's public surface", but the service and P3/P4a recipes call
  `dto.getAggregateId()` and `dto.isActive()`, so `aggregateId`, `version` and `state` are load-bearing
  and must be on every DTO. Inferred from the call sites rather than read from a spec.
- § `{Aggregate}.java` says "JPA annotated (`@Entity`, `@Table`, `@Id`, etc.)" without saying that
  `@Id` is inherited from `Aggregate` and must not be redeclared, or that the `@Table` on the
  abstract class does not name the concrete class's table under `TABLE_PER_CLASS`.
- Neither session-a.md nor `docs/concepts/aggregate.md` says what `aggregateType` should be set to,
  or where. `SagaUnitOfWorkService.resolveServiceName` strips a leading `Saga` and lowercases, which
  only works if the value is the concrete class's simple name; `setAggregateType(getClass().getSimpleName())`
  in the abstract constructor satisfies that, but it was derived from framework source, not from a doc.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-a.md` § `{Aggregate}Dto.java`: state the three
  framework-facing fields (`aggregateId`, `version`, `state`) and the `isActive()` accessor as
  mandatory, separate from the domain fields.
- `docs/concepts/aggregate.md` § Key Fields: say that `aggregateType` is set in the abstract
  aggregate's creating constructor as `getClass().getSimpleName()`, and connect it to
  `resolveServiceName`.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | `Saga{Aggregate}` not declared `@Entity` | High | Fixed this session - harness-log row 6, commit `7a1b1a477`. |
| `.claude/skills/implement-aggregate/session-a.md` | `{Aggregate}Dto` field surface unspecified; `aggregateId` / `version` / `state` / `isActive()` are required by the service and P3/P4a recipes | Medium | Name the four framework-facing members explicitly in § `{Aggregate}Dto.java`. |
| `docs/concepts/aggregate.md` | `aggregateType` value and assignment site unspecified, though `resolveServiceName` depends on it | Medium | Document `setAggregateType(getClass().getSimpleName())` in the creating constructor. |
| `.claude/skills/implement-aggregate/session-a.md` | `@Id` listed among the annotations to add, though it is inherited from `Aggregate`; `@Table` placement under `TABLE_PER_CLASS` not addressed | Low | Say `@Id` is inherited and `@Table` belongs on the abstract class. |

---

## Patterns to Capture

- **Pattern:** DTO-argument aggregate constructor
  **Observed in:** `.../station/aggregate/Station.java`, `.../station/aggregate/sagas/SagaStation.java`
  **Description:** The creating constructor takes `(Integer aggregateId, {Aggregate}Dto dto)` rather
  than a positional field list, so the factory signature stays stable as the aggregate grows fields.
  session-a.md permits both shapes but gives no rule for choosing; a stated default would remove the
  per-aggregate decision.

- **Pattern:** A T1 suite is blind to persistence-mapping defects
  **Observed in:** `.../sagas/station/StationIntraInvariantTest.groovy`
  **Description:** T1 constructs aggregates directly and never touches the EntityManager, so a
  missing `@Entity` on the concrete saga class is green at 2.N.a and fails at the first write in
  2.N.b/c. This is what made harness-log row 6 invisible until it was probed deliberately.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 6

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 6 | 1 | fixed | `7a1b1a477` |

---

## One-Line Summary

The domain layer for Station is green (4 T1 cases, no failures), and the session exposed that
session-a.md never required `@Entity` on the concrete saga aggregate - a defect T1 structurally
cannot catch, since it only surfaces on the first persist.
