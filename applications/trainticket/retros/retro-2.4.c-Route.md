# Retro — 2.4.c — Route

**App:** trainticket
**Session:** 2.4.c (Write Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/route/CreateRouteCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/route/UpdateRouteCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/route/DeleteRouteCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/service/RouteService.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/messaging/RouteCommandHandler.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/functionalities/RouteFunctionalities.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/sagas/CreateRouteFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/sagas/UpdateRouteFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/sagas/DeleteRouteFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/webapi/RouteController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (createRoute fixture rerouted onto CreateRoute)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/route/RouteServiceTest.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/CreateRouteTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/UpdateRouteTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/DeleteRouteTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/UpdateRouteCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/DeleteRouteCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdateRouteCompensationTest/UpdateRouteFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteRouteCompensationTest/DeleteRouteFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (2.4.c ticked, file row amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md` (row 19)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns (Create / Mutate), § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention, § P3 Guard Placement | Yes | Route has no P3 guard; the create/mutate/soft-delete shapes transcribed directly. |
| `docs/concepts/commands.md` | § What a Command Is (incl. `null` rootAggregateId for create), § Naming Conventions, § File Location, § Sending Commands, § Routing Commands | Yes | — |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Semantic-lock release on abort is automatic, § Write Workflow Structure | Partial | § Step Ordering describes a single data-assembly step per upstream aggregate; it says nothing about a step that must fetch N upstream aggregates because the request carries a collection of foreign ids. See Documentation Gaps. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § T2 Not-Found Paths, § T4 — Functionality Test, § Soft-delete functionalities, § Compensation Test, § CRITICAL gotcha, § Fake/Wrong/Weak Checklist, § Spec-First Ordering | Yes | The soft-delete happy-path substitution and the one-file-per-compensation-test rule both applied verbatim. |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Harness log, § Neutral domain, § Run the test suite (incl. § Inspecting maven output) | Yes | The subprocess capture recipe confirmed the lock step runs before the injected fault in both compensation tests. |
| `.claude/skills/implement-aggregate/session-c.md` | all | Partial | One Type 1 contradiction, fixed this session — see Harness Changes. |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" — session 2.4.b had already shaped `createRoute` so its
  station ids resolve to real Station aggregates (harness-log row 18), so rerouting the body onto
  `routeFunctionalities.createRoute` was a two-line change and every 2.4.b read test still passed
  unchanged.
- The § Compensation Test "one saga class, one compensation test file" rule and the CSV naming rule
  were precise enough to get both fault files right on the first run.

### What was unclear or missing

- The P4a violation-test assertion contradiction (harness-log row 19, fixed).
- Neither `sagas.md` nor `session-c.md` covers a data-assembly step whose fan-out is determined by
  the request (one fetch per element of a collection of foreign ids). Implemented as a single
  `getStationsStep` looping over the requested stations, which is what plan.md's cross-aggregate
  prerequisites cell prescribes for this aggregate ("data-assembly **step**: one
  `GetStationByIdCommand` per `stationAggregateId`"); the generic docs neither endorse nor forbid it.

### Suggested wording / structure changes

- `docs/concepts/sagas.md` § Step Ordering, item 2: state whether a data-assembly step may issue
  more than one command, and if so whether the fan-out belongs in one step or one step per fetch.
  A per-element step is not expressible anyway, since the count is only known at request time.

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `UpdateRouteFunctionalitySagas` | `getRouteStep` | none (primary: Route) | `updateRoute: getRouteStep acquires IN_UPDATE_ROUTE semantic lock` | Yes |
| `DeleteRouteFunctionalitySagas` | `getRouteStep` | none (primary: Route) | `deleteRoute: getRouteStep acquires IN_DELETE_ROUTE semantic lock` | Yes |

`CreateRouteFunctionalitySagas` has no `setSemanticLock` call site: per `sagas.md` § Create
Functionality Sagas the create step declares neither a lock nor forbidden states, and the create is
the saga's last step, so it registers no compensation and gets no compensation test either.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/sagas.md` | § Step Ordering does not say whether one data-assembly step may issue N commands when the request carries a collection of foreign ids | Medium | Add a "collection-valued prerequisite" note to § Step Ordering: one step, looping, with a deterministic order over the requested elements |
| `docs/concepts/sagas.md` | Nothing states how two sagas of the same aggregate share a data-assembly routine; `CreateRoute` and `UpdateRoute` resolve the station list identically and the resolution is duplicated across both classes | Low | State the intent explicitly (each `FunctionalitySagas` is self-contained, duplication preferred over a shared helper), or prescribe where a shared helper would live |

---

## Patterns to Capture

- **Pattern:** Collection-valued P4a data-assembly step
  **Observed in:** `microservices/route/coordination/sagas/CreateRouteFunctionalitySagas.java`,
  `.../UpdateRouteFunctionalitySagas.java`
  **Description:** When the request carries a collection of foreign aggregate ids, a single
  data-assembly step iterates the collection in a deterministic order (sorted by the element's own
  ordering field), sends one `Get{Foreign}ByIdCommand` per element, and rebuilds a new set of
  element DTOs seeded with the fetched values. The fetch throwing is the P4a enforcement; the input
  DTOs are never mutated (R7), a fresh DTO is constructed for the downstream command.

- **Pattern:** Asserting a data-assembly step in T4 without asserting persistence
  **Observed in:** `sagas/coordination/route/CreateRouteTest.groovy`
  **Description:** The happy path submits the collection elements with their cached foreign field
  left `null` and asserts the returned DTO carries the values fetched from the upstream aggregate.
  This pins the orchestration (the fetch happened and seeded the field) without straying into the
  field-level persistence assertions T2 owns.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 19

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 19 | 1 | fixed | `fe917c1c3` |

---

## One-Line Summary

Route's write layer is the application's first saga set with a cross-aggregate prerequisite, and it
exposed that the harness mandated an application-exception assertion for a P4a rule whose only
enforcement is the framework's own `SimulatorException` from the upstream fetch.
