# Retro — 2.6.b — Trip

**App:** trainticket
**Session:** 2.6.b (Read Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/trip/GetTripByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/trip/GetTripsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/aggregate/TripRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/aggregate/TripCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/aggregate/sagas/repositories/TripCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/service/TripService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/messaging/TripCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/sagas/GetTripByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/sagas/GetTripsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/trip/coordination/functionalities/TripFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/trip/TripServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/GetTripByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/GetTripsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | Build Commands' JDK-21 note matched the observed `release version 21 not supported` failure exactly. |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Yes | The foreign-aggregate-id caveat under § Update `{AppClass}SpockTest.groovy` (added by row 18) settled `createTrip`'s route/train-type defaults with no inference. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query | Yes | The three-file table (JPA repo / abstract custom repo / sagas custom repo) was directly applicable to `findAllLatestActive`. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § ServiceMapping Enum, § Routing Commands | Yes | — |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Unfiltered variant | Yes | `GetTrips` is the unfiltered variant verbatim. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak, § Spec-First Ordering, § T2 — Service Test (incl. § Not-Found Paths), § T4 — Functionality Test | Yes | The "collection reads have neither not-found path" rule in session-b.md routed `getTrips` to an empty-result case. |

---

## Skill Instructions Feedback

### What worked well

- The one-step vs two-step read-saga decision tree resolved both reads immediately: `GetTripById` is
  a PK read, `GetTrips` has no filter at all, so both are single-step and neither needed the
  foreign-id resolution branch.
- session-b.md's instruction to produce `{Aggregate}CommandHandler` and `{Aggregate}Functionalities`
  "even if not listed in the plan.md file table" was unnecessary here (plan.md listed both), but the
  parallel instruction for files plan.md *does* omit is what covered the three repository files.
- The foreign-aggregate-id caveat on the fixture helper prevented the failure mode it describes:
  `TRIP_ROUTE_AGGREGATE_ID`/`TRIP_TRAIN_TYPE_AGGREGATE_ID` already existed as literal constants from
  2.6.a's T1 test and would otherwise have been the obvious defaults, breaking every call site in
  2.6.c when `CreateTrip`'s data-assembly steps start fetching them.

### What was unclear or missing

- (none)

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | — | — | — |

---

## Patterns to Capture

- **Pattern:** Fixture defaults that mint prerequisites must respect the prerequisite aggregate's own
  P3 uniqueness rules.
  **Observed in:** `applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/trip/GetTripsTest.groovy`
  **Description:** session-b.md tells the fixture helper to mint foreign ids by calling the foreign
  aggregate's own `create{Foreign}` helper rather than defaulting to a literal id. A collection-read
  test then creates two instances, and calling the helper twice with its defaults re-mints the same
  foreign aggregate twice — which throws when that aggregate (or one it transitively mints) carries
  an own-table uniqueness rule on a defaulted field. The overridable-parameter escape the caveat
  already requires is exactly what resolves it: the multi-instance test passes distinct foreign ids
  explicitly. Worth naming in the caveat, since the single-instance tests written alongside it never
  surface the collision.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

Both Trip reads were covered verbatim by existing doc patterns (PK read, unfiltered collection read),
and the fixture-helper caveat added during 2.4.b did its job unprompted — no harness friction.
