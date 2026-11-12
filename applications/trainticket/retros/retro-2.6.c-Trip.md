# Retro — 2.6.c — Trip

**App:** trainticket
**Session:** 2.6.c (Write Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/service/TripService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/aggregate/sagas/states/TripSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/messaging/TripCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/functionalities/TripFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/sagas/CreateTripFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/sagas/UpdateTripFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/sagas/DeleteTripFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/webapi/TripController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/trip/CreateTripCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/trip/UpdateTripCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/trip/DeleteTripCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/trip/TripServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/CreateTripTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/UpdateTripTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/DeleteTripTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/UpdateTripCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/DeleteTripCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdateTripCompensationTest/UpdateTripFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteTripCompensationTest/DeleteTripFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns (Create / Mutate), § Copy-on-Write Rule, § Exception-Throw Convention, § P3 Guard Placement, § Custom Repository | Yes | The create-only own-table uniqueness guard mapped directly onto the Create method pattern. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Yes | The `null` `rootAggregateId` carve-out for create commands settled `CreateTripCommand` without inference. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Semantic-lock release on abort is automatic | Yes | `CreateTrip` is Shape 1 with two data-assembly steps ahead of the create; the create is last, so no compensation was registered. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § Not-Found Paths, § T4 — Functionality Test, § Soft-delete happy-path assertion, § Compensation Test, § CRITICAL gotcha, § Fake/Wrong/Weak Checklist | Yes | The soft-delete carve-out and the one-file-per-compensation-test rule were both needed verbatim. |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Application isolation, § Run the test suite, § Inspecting maven output | Yes | The subprocess capture recipe produced the compensation-test sanity check. |

---

## Skill Instructions Feedback

### What worked well

- `session-c.md` § "Update `{AppClass}SpockTest.groovy`" told the session to replace the helper body while keeping the 2.6.b signature and defaults. Session 2.6.b had already written `createTrip` with `createRoute()` / `createTrainType()` as its defaults and left a comment anticipating the reroute, so the swap was a body-only edit and the 2.6.b read tests kept passing unchanged.
- The P4a carve-out in § "One `{Op}Test.groovy` per write functionality" — assert `thrown(SimulatorException)` and nothing further where the fetch itself is the enforcement — matched plan.md's ROUTE_AND_TRAIN_TYPE_EXIST note exactly, so no rule constant had to be invented for a rule that has none.
- `docs/concepts/testing.md` § CRITICAL gotcha correctly predicted that the two compensation cases could not live inside `UpdateTripTest` / `DeleteTripTest`; the separate files with their own CSVs worked first try.

### What was unclear or missing

- (none)

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateTripFunctionalitySagas` | (no `setSemanticLock` call site) | — | — | n/a |
| `UpdateTripFunctionalitySagas` | `getTripStep` | none (primary aggregate `Trip`) | `updateTrip: getTripStep acquires IN_UPDATE_TRIP semantic lock` (`UpdateTripTest`) | Yes |
| `DeleteTripFunctionalitySagas` | `getTripStep` | none (primary aggregate `Trip`) | `deleteTrip: getTripStep acquires IN_DELETE_TRIP semantic lock` (`DeleteTripTest`) | Yes |

`CreateTripFunctionalitySagas` has no row to satisfy: per `docs/concepts/sagas.md` § Create Functionality Sagas the create step declares neither a semantic lock nor forbidden states, and its two data-assembly steps are plain upstream reads.

Both lock call sites additionally carry a compensate-transition case (`UpdateTripCompensationTest`, `DeleteTripCompensationTest`). Both were sanity-checked against maven's real stdout per `docs/concepts/testing.md` § Compensation Test: `getTripStep` logs `START EXECUTION STEP` / `END EXECUTION STEP` before `Fault on updateTripStep` / `Fault on deleteTripStep` fires, so neither is a false positive.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | — | — | — |

---

## Patterns to Capture

- **Pattern:** Independent P4a data-assembly steps as parallel roots of a create saga
  **Observed in:** `applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/sagas/CreateTripFunctionalitySagas.java`
  **Description:** When a create saga must prove that two unrelated upstream aggregates exist, and neither fetch feeds the other, both fetch steps are declared as dependency-free roots and the create step lists both in its dependency list. `docs/concepts/sagas.md` § Step Ordering describes "data-assembly steps" in the plural but its worked examples all chain a single one, so the two-root shape is currently inferred rather than shown. Worth a sentence in that section if a second application hits it.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

Trip's three write functionalities landed with no harness friction: the docs covered the create-only own-table uniqueness guard, the two-root P4a create saga, and both compensation tests without a single point of inference.
