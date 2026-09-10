# Retro - 2.7.c - PriceConfig

**App:** trainticket
**Session:** 2.7.c (Write Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/priceconfig/CreatePriceConfigCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/priceconfig/UpdatePriceConfigCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/priceconfig/DeletePriceConfigCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/service/PriceConfigService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/messaging/PriceConfigCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/sagas/CreatePriceConfigFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/sagas/UpdatePriceConfigFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/sagas/DeletePriceConfigFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/functionalities/PriceConfigFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/webapi/PriceConfigController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/sagas/states/PriceConfigSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/priceconfig/PriceConfigServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/CreatePriceConfigTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/UpdatePriceConfigTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/DeletePriceConfigTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/UpdatePriceConfigCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/DeletePriceConfigCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdatePriceConfigCompensationTest/UpdatePriceConfigFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeletePriceConfigCompensationTest/DeletePriceConfigFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns (Create/Mutate), § Copy-on-Write Rule, § P3 Guard Placement, § Exception-Throw Convention | Yes | - |
| `docs/concepts/commands.md` | § What a Command Is (null rootAggregateId for create), § Naming Conventions, § Routing Commands, § ServiceMapping Enum | Yes | The row-22 broadening of the null-rootAggregateId enumeration already covered the create command here |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Semantic-lock release on abort is automatic | Yes | Shape 1 applied verbatim: the create is the only step, so no compensation is registered |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 - Service Test, § Not-Found Paths, § T4 - Functionality Test, § Soft-delete happy path, § Compensation Test, § Fake/Wrong/Weak Checklist | Yes | - |
| `.claude/skills/implement-aggregate/session-c.md` | whole file | Yes | - |
| `.claude/skills/_shared/conventions.md` | § Anchor, § Application isolation, § Harness log, § Run the test suite (incl. § Inspecting maven output) | Yes | The subprocess capture recipe was used for the compensation sanity check |

---

## Skill Instructions Feedback

### What worked well

- session-c.md's instruction to replace the `create{Aggregate}` helper body while keeping its
  signature was unambiguous, and the 2.7.b read tests passed unchanged after the reroute.
- The § Compensation Test rule ("a `setSemanticLock` step with a dependent step after it") decided
  all three functionalities without judgement: update and delete get a compensation test, the
  single-step create does not.

### What was unclear or missing

- none

### Suggested wording / structure changes

- none

---

## Semantic-Lock Coverage Audit (sessions `c` only - write "n/a" for `a`/`b`/`d`)

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `UpdatePriceConfigFunctionalitySagas` | `getPriceConfigStep` | none (primary aggregate) | `updatePriceConfig: getPriceConfigStep acquires IN_UPDATE_PRICE_CONFIG semantic lock` | Yes |
| `DeletePriceConfigFunctionalitySagas` | `getPriceConfigStep` | none (primary aggregate) | `deletePriceConfig: getPriceConfigStep acquires IN_DELETE_PRICE_CONFIG semantic lock` | Yes |

`CreatePriceConfigFunctionalitySagas` has no `setSemanticLock` call site: the aggregate does not
exist when the saga starts (sagas.md § Create Functionality Sagas), so it contributes no row.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | - | - | - |

---

## Patterns to Capture

- **Pattern:** create-only compound-key uniqueness guard
  **Observed in:** `microservices/priceconfig/service/PriceConfigService.java`
  **Description:** When every field of a P3 uniqueness key is Java-`final` on the aggregate, the
  guard runs in the create method only and the update path needs none. The same shape already
  appeared for a single-field key in session 2.6.c; PriceConfig is the compound-key instance of it.
  `docs/concepts/service.md` § Create method shows a compound-key guard, so this is a confirmation
  of documented guidance rather than an undocumented pattern.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | - | - | - |

---

## One-Line Summary

PriceConfig's write layer landed with no harness friction: the docs settled every decision, including
the create saga's no-lock/no-compensation shape and the compound-key create-only P3 guard.
