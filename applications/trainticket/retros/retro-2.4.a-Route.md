# Retro — 2.4.a — Route

**App:** trainticket
**Session:** 2.4.a (Domain Layer)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/Route.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteStation.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteStationDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/RouteRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/sagas/SagaRoute.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/sagas/states/RouteSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/sagas/factories/SagasRouteFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/aggregate/sagas/repositories/RouteCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/route/RouteServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java` (appended: six Route P1 constants)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/route/RouteIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (appended: Route T1 literals)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy` (appended: factory and custom-repository beans)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (2.4.a checkbox ticked; 2.4.a file row amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md` (rows 16, 17)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands, § Module Map | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Harness log, § Neutral domain, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | all sections | Partial | Two gaps in § "Owned entity classes" for the collection cardinality - see Documentation Gaps. |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants, § Factories, § Repositories, § Naming Conventions | Yes | § getEventSubscriptions() skipped - this application has no events. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak, § Choosing Input Values, § Spec-First Ordering, § T1 | Yes | The EP/BVA decision rule cleanly partitioned Route's six P1 rules into three ordered-domain (straddled) and three categorical (single representative). |
| `applications/trainticket/plan.md` | § Application-wide facts, § Rule Classification, § 3.1 rules, § 4. Route | Yes | — |
| `applications/trainticket/trainticket-domain-model.md` | § 1 Entities, § 2 Relationships, § 3.1, § 4 | Yes | — |
| `applications/trainticket/trainticket-aggregate-grouping.md` | § 2 snapshot table | Yes | Confirmed `RouteStation` caches `stationAggregateId` + `stationName` and no version field. |
| `simulator/.../ms/aggregate/Aggregate.java` | field list, constructors, inheritance strategy | Yes | Read only; not modified. |

---

## Skill Instructions Feedback

### What worked well

- § "Verify Mandatory Files in plan.md" was a no-op here because `/classify-and-plan` had already
  listed both interface files; the check is cheap and worth keeping.
- The `Saga{Aggregate}` section's copy-constructor rule (inherit `other.getSagaState()`, never reset)
  is stated with its failure mode, which removed any temptation to "clean up" the state on copy.
- § "Update {AppClass}SpockTest.groovy" being explicit that session `a` adds constants but **no**
  `create{Aggregate}` helper prevented reaching for a functionality that does not exist yet.
- `docs/concepts/testing.md` § "Choosing Input Values" is the section that produced the test matrix.
  Naming which rules are categorical (uniqueness, set membership) as explicitly *not* needing
  boundaries is as useful as the straddle rule itself.

### What was unclear or missing

- § "Owned entity classes" gives the collection mapping's annotation but described the aggregate as
  "the inverse side with no FK column", which is the `@OneToOne` case's description, not this one.
  Fixed as Type 1 (harness-log row 16, commit `1ec57d719`).
- The same section mandates a deep copy in the copy constructor only for the nested entity-to-entity
  `@OneToOne` case. The collection case needs it for the same reason and the doc is silent
  (harness-log row 17).
- Neither `session-a.md` nor `aggregate.md` says whether an owned collection should be typed `Set`
  or `List`. Chosen `Set` because every Route rule is phrased "ordering by `sequence`", so a
  positional collection would invite predicates that silently depend on insertion order instead.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-a.md` § "Owned entity classes": lift the deep-copy
  sentence out of the `@OneToOne` bullet into a cardinality-independent rule - "the aggregate's copy
  constructor deep-copies every owned entity, one `new {Entity}(existing)` per instance" - since
  copy-on-write plus `orphanRemoval = true` makes a shared instance a data-loss bug at either
  cardinality.

---

## Semantic-Lock Coverage Audit (sessions `c` only)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | Collection `@OneToMany` described as the "inverse side"; a `mappedBy` derived from that reading has no back-reference to name | Medium | Fixed in commit `1ec57d719` |
| `.claude/skills/implement-aggregate/session-a.md` | No deep-copy rule for owned **collections** in the aggregate copy constructor; stated only for nested `@OneToOne` | High | Restate deep-copy as a cardinality-independent rule (row 17) |
| `.claude/skills/implement-aggregate/session-a.md` | No guidance on `Set` vs `List` for an owned collection, nor on whether invariants may rely on iteration order | Low | Prescribe `Set` plus an explicit ordering key when the domain defines one |
| `.claude/skills/implement-aggregate/session-a.md` | No note that an owned entity's field name may collide with a SQL reserved word (`sequence`, `order`, `user`) and needs an explicit `@Column(name = ...)` | Low | One sentence under § "Owned entity classes" |

---

## Patterns to Capture

- **Pattern:** Ordering an unordered owned collection by its domain sequence key before checking invariants
  **Observed in:** `microservices/route/aggregate/Route.java` (`orderedRouteStations()`)
  **Description:** When the domain phrases rules positionally ("the entry at index `i`") over a
  collection that carries its own explicit ordering attribute, sort by that attribute once at the top
  of `verifyInvariants()` and pass the resulting `List` to each per-rule helper. Keeps every predicate
  independent of JPA's iteration order, and makes the check-ordering dependency explicit: the
  size rule must run first so the positional helpers can index safely.

- **Pattern:** Null-tolerant comparator in the ordering step
  **Observed in:** `microservices/route/aggregate/Route.java` (`Comparator.nullsLast`)
  **Description:** `verifyInvariants()` runs on whatever the caller built, including a partially
  populated entity. Sorting on a possibly-null key with `Comparator.nullsLast` lets the dedicated
  contiguity rule report the violation with its own message constant, instead of the sort throwing an
  NPE that no rule name explains.

- **Pattern:** Deep-copied owned collection in the aggregate copy constructor
  **Observed in:** `microservices/route/aggregate/Route.java`
  **Description:** Each aggregate version owns its own owned-entity instances
  (`.map({Entity}::new)`), so a later mutation cannot reach back into a previous version through a
  shared reference and `orphanRemoval = true` cannot delete rows the previous version still needs.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 16, 17

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 16 | 1 | fixed | `1ec57d719` |
| 17 | 2 | deferred | - |

---

## One-Line Summary

Route is the application's first aggregate owning a collection, and `session-a.md` covers that
cardinality only for the annotation - not for the copy-constructor deep copy that copy-on-write
requires, nor for whether invariants may lean on iteration order.
