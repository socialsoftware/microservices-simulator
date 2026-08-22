# Retro — 2.1.c — Station

**App:** trainticket
**Session:** 2.1.c (Write Functionalities)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/service/StationService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/messaging/StationCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/coordination/functionalities/StationFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/coordination/sagas/CreateStationFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/coordination/sagas/UpdateStationFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/coordination/sagas/DeleteStationFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/coordination/webapi/StationController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/station/aggregate/sagas/states/StationSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/station/CreateStationCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/station/UpdateStationCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/station/DeleteStationCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/station/StationServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/station/CreateStationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/station/UpdateStationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/station/DeleteStationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/station/UpdateStationCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/station/DeleteStationCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdateStationCompensationTest/UpdateStationFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteStationCompensationTest/DeleteStationFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns (Create / Mutate), § Copy-on-Write Rule, § Exception-Throw Convention, § P3 Guard Placement, § Injected Dependencies | Yes | The copy-on-write soft-delete rationale carried straight into `deleteStation`. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Partial | § Sending Commands shows `cmd.setForbiddenStates(...)` on a plain `Command`; `setForbiddenStates` is declared on `SagaCommand`, not `Command`, so that snippet does not compile as written. Not exercised this session (Station has no foreign-aggregate step) and therefore not logged as friction - it will bite the first aggregate that has one. |
| `docs/concepts/sagas.md` | § Lock-Acquisition Step Pattern, § Semantic-lock release on abort is automatic, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Step Ordering, § Write Workflow Structure | Yes | Shape 1 matched `CreateStation` exactly: one step, so no compensation registered. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2, § T4, § Compensation Test, § ImpairmentService mechanism, § CRITICAL gotcha, § Choosing Input Values, § Fake / Wrong / Weak Checklist, § Spec-First Ordering | Partial | Two defects found and fixed - see § Harness Changes. The `ImpairmentService` mechanism description was accurate enough to write both CSVs correctly first time. |
| `.claude/skills/implement-aggregate/session-c.md` | all | Partial | Omitted the compensation-test file (row 10). |
| `.claude/skills/_shared/conventions.md` | § Anchor to repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | The no-pipe / surefire-aggregation rule mattered: the Bash hook truncated maven stdout on every run, so the verdict came from `MAVEN_EXIT` plus the report files as prescribed. |

---

## Skill Instructions Feedback

### What worked well

- The R4 decision table settled all three sagas without deliberation: create takes neither lock nor
  forbidden states, update and delete take the get-then-lock pair on the primary aggregate.
- The explicit "do not register a manual lock-release compensation" rule, plus the
  `currentExecutingStep` re-lock pitfall note, pre-empted the obvious wrong instinct on both
  two-step sagas. Both compensation tests pass with no compensation registered anywhere.
- session-c.md's instruction to replace the `create{Aggregate}` helper body while keeping its
  signature meant 2.1.b's six read tests kept passing untouched and now exercise the real create
  saga.
- The § Fake / Wrong / Weak checklist directly produced two tests that would otherwise have been
  missed: the self-exclusion case on the update guard, and the "name freed by a soft-deleted
  station is reusable" case that pins the guard to *active* rows.

### What was unclear or missing

- session-c.md § "Produce" never named the T4 compensation test even though `testing.md` requires
  one per lock-holding write functionality and session `c` is the only session that could produce
  it (row 10).
- `testing.md` § T4's happy-path template is written as if every write functionality leaves a
  loadable aggregate behind; a soft-delete functionality is a standing counter-example and the doc
  had no branch for it (row 11).
- session-c.md prescribes the service method shape for create and mutate but says nothing about the
  **return type** of an update or delete coordinator. Chose `void` for both, matching
  `service.md` § Mutate method, with `StationDto` returned from create because the caller needs the
  minted `aggregateId`. Worth stating once rather than re-deciding per aggregate.

### Suggested wording / structure changes

- `docs/concepts/commands.md` § Sending Commands: the mutate-step snippet calls
  `cmd.setForbiddenStates(...)` on a plain `Command`. Either wrap it in `SagaCommand` in the snippet
  or say the wrap is required. Left alone this session because Station has no foreign-aggregate
  step, so the claim could not be demonstrated by a build failure here.

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateStationFunctionalitySagas` | (none - create step takes no lock) | — | n/a | n/a |
| `UpdateStationFunctionalitySagas` | `getStationStep` | none (primary: Station) | `updateStation: getStationStep acquires IN_UPDATE_STATION semantic lock` | Yes |
| `DeleteStationFunctionalitySagas` | `getStationStep` | none (primary: Station) | `deleteStation: getStationStep acquires IN_DELETE_STATION semantic lock` | Yes |

No unresolved `No` rows. Both lock-holding sagas additionally carry a compensation test, and both
were sanity-checked per `testing.md` § ImpairmentService mechanism: with the fault flag flipped to
`0` each test fails with "Expected exception ... but no exception was thrown", and the application
log shows `START EXECUTION STEP: getStationStep` preceding the `EXCEPTION THROWN` line, confirming
the lock is genuinely held when the fault fires.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | § Sending Commands calls `setForbiddenStates` on a plain `Command`, but the method is declared on `SagaCommand` | High for the first aggregate with a foreign-aggregate write step; none for Station | Wrap the mutate command in `SagaCommand` in the snippet, or state that foreign-aggregate guard steps require the wrap |
| `.claude/skills/implement-aggregate/session-c.md` | No rule for the return type of update / delete coordinator methods | Low - `service.md` § Mutate implies `void`, but it is re-decided per aggregate | One line: mutating coordinators return `void`; create returns the DTO because the caller needs the minted id |

---

## Patterns to Capture

- **Pattern:** Uniqueness guard with self-exclusion on the update path
  **Observed in:** `microservices/station/service/StationService.java` (`checkNameIsUnique`)
  **Description:** A P3 own-table uniqueness rule that guards both create and update needs one
  helper taking the aggregate id under change (`null` on create), skipping that row when scanning
  the latest-active set. Without the exclusion, re-committing an aggregate's own value is rejected
  as a duplicate - a bug no create-only test can see. It deserves its own T2 case
  ("keeping its own name is not a duplicate"), which is a guard *branch*, not a boundary, so
  § Choosing Input Values does not ask for it.

- **Pattern:** A uniqueness rule phrased over *active* rows is freed by a soft delete
  **Observed in:** `sagas/station/StationServiceTest.groovy`
  **Description:** When the rule reads "no two **active** X share ...", the guard scanning
  `findAllLatestActive()` implies a soft-deleted row releases its value for reuse. That is a
  consequence of the rule worth pinning with a test, because a guard written against
  `findAll()` would pass every other case and fail only this one.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 10, 11

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 10 | 1 | fixed | `318318b43` |
| 11 | 1 | fixed | `97fa24f8a` |

---

## One-Line Summary

Both harness defects this session were the same shape - a required scenario stated in
`testing.md` that no session sub-file routed to a producing session, and a T4 template rule that
silently assumes the operation leaves its aggregate loadable.
