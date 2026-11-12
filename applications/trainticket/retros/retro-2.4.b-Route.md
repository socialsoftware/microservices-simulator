# Retro - 2.4.b - Route

**App:** trainticket
**Session:** 2.4.b (Read Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/route/GetRouteByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/route/GetRoutesCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/route/GetRoutesByStationCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/service/RouteService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/messaging/RouteCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/sagas/GetRouteByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/sagas/GetRoutesFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/sagas/GetRoutesByStationFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/coordination/functionalities/RouteFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/sagas/repositories/RouteCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/route/RouteServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/GetRouteByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/GetRoutesTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/route/GetRoutesByStationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | - |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Partial | Fixture-helper section conflicts with itself for an aggregate whose create saga fetches a foreign aggregate; see § Skill Instructions Feedback and harness-log row 18 |
| `.claude/skills/implement-aggregate/session-c.md` | § Update `{AppClass}SpockTest.groovy` | Yes | Read to pin the exact contract the 2.4.b fixture signature has to survive |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns (read), § Custom Repository - Latest-Active-Version Query | Yes | The "or a narrower variant" allowance covers the station-join query directly |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Unfiltered variant | Yes | The list-return template's own worked example filters on a foreign aggregate id, which settled `GetRoutesByStationCommand`'s `rootAggregateId` |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § Routing Commands | Yes | - |
| `docs/concepts/testing.md` | § T2 - Service Test, § Not-Found Paths, § T4 - Functionality Test, § Assertion Ownership, § Test Profile - Serialization Note | Yes | The collection-read "empty-result instead of not-found" rule in session-b.md removed the only ambiguity |
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | - |

---

## Skill Instructions Feedback

### What worked well

- session-b.md's per-file `###` subheadings map one-to-one onto the produced artifacts; nothing had to be inferred about which class gets which responsibility.
- The explicit "collection reads have neither Path A nor Path B - write an empty-result case instead" rule pre-empted the obvious wrong move of inventing a `ROUTES_NOT_FOUND` constant for `GetRoutes` / `GetRoutesByStation`.
- The `@Bean` split (constructor takes UoW service + repo + custom repo; factory and id generator are `@Autowired` fields) is stated precisely enough to write the bean method without opening the service template - a direct payoff of harness-log row 13's fix in 2.2.b.
- plan.md's note that `GetRoutesByStation` is "a plain list read, not a saga: it filters Route's own `RouteStation.stationAggregateId` and issues no command to Station" pre-classified the one read that would otherwise have looked like the two-step variant.

### What was unclear or missing

- § "Update `{AppClass}SpockTest.groovy`" requires both "each defaulted to the domain constant" and "call sites written this session survive that swap unchanged". For Route those are incompatible: `CreateRoute`'s STATIONS_EXIST data-assembly step fetches every `stationAggregateId`, so a constant placeholder id throws the moment 2.4.c reroutes the helper - and 2.4.b's suite cannot catch it, because the fixture bypasses the saga. Fixed as Type 1 (harness-log row 18, commit `7957ce979`): foreign ids are minted through the foreign aggregate's own fixture helper, and stay overridable so a test can make two aggregates share or not share a prerequisite.
- The fixture template shows a single `create{Aggregate}` helper and says nothing about aggregates whose fixture needs a collection argument assembled per call site. `createRoute` needed two companions on the base class (`routeStationsOf`, `twoStationRoute`) for the three read tests to express "these two routes share a station, that one does not". The skill neither blesses nor forbids companions; the choice was made on the same "no per-test-class fixtures" reasoning the section already gives.

### Suggested wording / structure changes

- (none beyond the Type 1 fix already applied to `session-b.md`)

---

## Semantic-Lock Coverage Audit (sessions `c` only)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-b.md` | Fixture defaults for a parameter naming a foreign aggregate whose create saga fetches it | High - silently defers a break to session `c` | Fixed this session (row 18, `7957ce979`) |
| `.claude/skills/implement-aggregate/session-b.md` | Whether the fixture may add companion builder helpers on the base test class for collection-valued arguments | Low - both readings produce working code | State that companions belong on the base class alongside `create{Aggregate}`, under the same no-per-test-class-fixture rule |

---

## Patterns to Capture

- **Pattern:** Narrowed latest-active-version query over an owned collection
  **Observed in:** `applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteRepository.java`
  **Description:** `docs/concepts/service.md` § "Custom Repository" allows "a narrower variant" of `findAllLatestActive()` but shows only the unfiltered JPQL. Filtering on a field of the aggregate's *owned collection* needs a join plus `distinct` on top of the latest-active subquery: `select distinct r from Route r join r.routeStations rs where rs.stationAggregateId = :id and r.state = 'ACTIVE' and r.version = (select max(...))`. The `distinct` is defensive - a P1 rule already forbids the same station appearing twice on one route - but the join's fan-out is the kind of thing that silently duplicates rows once that invariant is relaxed. Worth a worked example in that section.

- **Pattern:** Fixture-minted prerequisites for a read session
  **Observed in:** `applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
  **Description:** Session `b` builds its fixture directly on the aggregate, which makes prerequisite aggregates optional at that point and mandatory in session `c`. Minting them from the start (rather than after `c` breaks) costs nothing in `b` and is the only version of the helper that survives the reroute. Now stated in `session-b.md`.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 18

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 18 | 1 | fixed | `7957ce979` |

---

## One-Line Summary

Route's three reads landed unchanged from the templates; the session's one real finding is that session-b.md's "default every argument to a domain constant" fixture rule silently breaks in session `c` for any aggregate whose create saga fetches a foreign aggregate, since the fixture bypasses the saga that would have caught it.
