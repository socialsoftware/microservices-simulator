# Retro - 2.7.b - PriceConfig

**App:** trainticket
**Session:** 2.7.b (Read Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfigRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/PriceConfigCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/aggregate/sagas/repositories/PriceConfigCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/service/PriceConfigService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/messaging/PriceConfigCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/functionalities/PriceConfigFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/sagas/GetPriceConfigByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/sagas/GetPriceConfigsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/priceconfig/coordination/sagas/GetPriceConfigByRouteAndTrainTypeFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/priceconfig/GetPriceConfigByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/priceconfig/GetPriceConfigsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/priceconfig/GetPriceConfigByRouteAndTrainTypeCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/priceconfig/PriceConfigServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/GetPriceConfigByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/GetPriceConfigsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/priceconfig/GetPriceConfigByRouteAndTrainTypeTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns -> Read method, § Custom Repository - Latest-Active-Version Query, § Exception-Throw Convention | Yes | The three-file rule (JPA repo / abstract custom repo / sagas custom repo) transferred unchanged to the composite-key finder. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § ServiceMapping Enum, § Routing Commands | Partial | The `null` rootAggregateId enumeration was closed at two shapes and did not name the composite-key read; fixed this session (row 22). |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Unfiltered variant | Yes | The composite-key read is structurally the by-id template with two filter parameters; no new variant was needed. |
| `docs/concepts/testing.md` | § T2 - Service Test, § Not-Found Paths, § T4 - Functionality Test, § Assertion Ownership, § Spec-First Ordering | Yes | The Path A / Path B rule of thumb settled both not-found cases directly from the service bodies. |
| `docs/architecture.md` | § package layout | Yes | - |

---

## Skill Instructions Feedback

### What worked well

- `session-b.md` § "Update `{AppClass}SpockTest.groovy`" and its "Foreign aggregate ids are never defaulted to a constant" caveat: plan.md states PriceConfig has no cross-aggregate prerequisites, so the caveat's precondition (a create-saga data-assembly fetch of the foreign aggregate) does not hold and the two foreign ids stay literal constants. The caveat naming its own precondition is what made that decidable without guessing.
- The Path A / Path B split in `testing.md` mapped one-to-one onto the two by-key reads without interpretation.

### What was unclear or missing

- `session-b.md` § "One `{Query}FunctionalitySagas.java` per read functionality" frames the one-step / two-step decision purely around single-filter reads. A read filtered on two fields both stored on the aggregate is a one-step saga by the stated criterion, but the wording ("when every filter criterion is stored directly on the aggregate") only reads that way on a second pass. No change made - the criterion is correct as written.

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit (sessions `c` only)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | § "What a Command Is" enumerated the no-root-aggregate step shapes as a closed set of two, excluding the composite-key read whose target id is unresolved at send time. | Medium | Fixed this session - enumeration broadened, and the filtered collection read named explicitly as the case that does pass its foreign filter id. |

---

## Patterns to Capture

- **Pattern:** Composite-key read resolves the key, then loads through the unit of work
  **Observed in:** `microservices/priceconfig/service/PriceConfigService.java`
  **Description:** The custom-repository `Optional` supplies only the aggregate id (`.map(PriceConfig::getAggregateId).orElseThrow(...)`); the DTO is then built from `aggregateLoadAndRegisterRead` on that id rather than from the entity the query returned. This keeps the read registered with the unit of work exactly as a by-id read is, so Path B differs from Path A only in how the id is obtained.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 22

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 22 | 1 | fixed | 458f696f3 |

---

## One-Line Summary

The composite-key read is the by-id read with the id resolved one step earlier, and the only harness friction it produced was a closed enumeration in `commands.md` that its own governing rule already contradicted.
