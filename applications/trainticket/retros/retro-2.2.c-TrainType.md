# Retro — 2.2.c — TrainType

**App:** trainticket
**Session:** 2.2.c (Write Functionalities)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/traintype/CreateTrainTypeCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/traintype/UpdateTrainTypeCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/traintype/DeleteTrainTypeCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/aggregate/sagas/states/TrainTypeSagaState.java` (amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/service/TrainTypeService.java` (amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/messaging/TrainTypeCommandHandler.java` (amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/coordination/sagas/CreateTrainTypeFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/coordination/sagas/UpdateTrainTypeFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/coordination/sagas/DeleteTrainTypeFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/coordination/functionalities/TrainTypeFunctionalities.java` (amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/traintype/coordination/webapi/TrainTypeController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/traintype/TrainTypeServiceTest.groovy` (amended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/traintype/CreateTrainTypeTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/traintype/UpdateTrainTypeTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/traintype/DeleteTrainTypeTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/traintype/UpdateTrainTypeCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/traintype/DeleteTrainTypeCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdateTrainTypeCompensationTest/UpdateTrainTypeFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteTrainTypeCompensationTest/DeleteTrainTypeFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (checkbox + 2.2.c file-row amendment)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/harness-log.md` (row 14)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns, § Copy-on-Write Rule (via session-c.md's restatement of the call shape) | Yes | The create/mutate/soft-delete shapes were fully determined by session-c.md's own summary plus the sibling aggregate already in the app. |
| `docs/concepts/commands.md` | § ServiceMapping Enum (verification only) | Yes | `TRAIN_TYPE("trainType")` was already present from 2.2.b. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § Semantic-lock release on abort is automatic, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Write Workflow Structure | Yes | Shape 1 applied verbatim to `CreateTrainTypeFunctionalitySagas` (single step, no lock, no compensation). |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § Not-Found Paths, § T4 — Functionality Test, § Soft-delete happy-path assertion, § Compensation Test, § CRITICAL gotcha, § Fake/Wrong/Weak, § Choosing Input Values | Partial | Complete on what to assert; the compensation-test sanity check it mandates could not be performed by the means the harness allows — see Documentation Gaps and harness-log row 14. |

---

## Skill Instructions Feedback

### What worked well

- `session-c.md` § "Update `{AppClass}SpockTest.groovy`" is precise about keeping the 2.2.b helper signature and defaults intact. Swapping only the body meant the four read tests written in 2.2.b started exercising the real create saga with no edits, and they passed unchanged.
- The "no `@Bean` in this session" instruction under § "BeanConfigurationSagas — No Change Needed" is unambiguous and correct: all three beans already existed from 2.2.b.
- § Compensation Test's "one saga class, one compensation test file" gotcha, together with its CSV naming rule (`{Op}FunctionalitySagas.csv` under `{Op}CompensationTest/`), was enough to get the fault injection right on the first run.
- Routing P1 rules away from T2/T4 (all four TrainType rules are P1 or compiler-enforced) left this session with no guard cases to write, and the docs say so explicitly rather than leaving it to inference.

### What was unclear or missing

- `session-c.md` § "Produce" lists `{Op}CompensationTest.groovy` as a required artifact, but plan.md's own `2.2.c` row does not — the same omission recorded for Station as harness-log row 10, which was fixed in the skill but leaves already-generated plan.md rows stale. Handled by the § "Amend plan.md for omitted files" path, which worked as designed.
- Nothing in `session-c.md` says whether the `BehaviourReport.txt` that `ImpairmentService` writes into the test-resource directory is a session artifact. Station's is committed; this session left the two new ones uncommitted, on the grounds that they are run output. See Patterns to Capture.

### Suggested wording / structure changes

- none

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateTrainTypeFunctionalitySagas` | (no `setSemanticLock` call — create step, per sagas.md § Create Functionality Sagas) | — | n/a | n/a |
| `UpdateTrainTypeFunctionalitySagas` | `getTrainTypeStep` | none (primary aggregate) | `updateTrainType: getTrainTypeStep acquires IN_UPDATE_TRAIN_TYPE semantic lock` | Yes |
| `DeleteTrainTypeFunctionalitySagas` | `getTrainTypeStep` | none (primary aggregate) | `deleteTrainType: getTrainTypeStep acquires IN_DELETE_TRAIN_TYPE semantic lock` | Yes |

Compensate transitions for both lock-holding sagas are covered by `UpdateTrainTypeCompensationTest`
and `DeleteTrainTypeCompensationTest`.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md`, `.claude/skills/_shared/conventions.md` | § Compensation Test mandates confirming a `START EXECUTION STEP` console line, while § "Run the test suite" forbids reading maven stdout and offers no capture mechanism that survives the output-filtering hook. | High — without it, a compensation test that never exercises its lock step passes silently, which is the exact false positive the check exists to catch. | Fixed this session (harness-log row 14, commit `59122e1c2`): conventions.md gains § "Inspecting maven output" with a subprocess capture recipe, and testing.md's sanity-check sentence points at it. |

---

## Patterns to Capture

- **Pattern:** Immutable-field update method
  **Observed in:** `microservices/traintype/service/TrainTypeService.java` (`updateTrainType`)
  **Description:** When an aggregate has a `final` field, the update service method takes the full DTO but sets only the mutable fields, silently ignoring the DTO's copy of the immutable one. The contract is pinned in T2 by asserting the read-back's immutable field still holds its original value alongside the fields that did change. This is a service-contract assertion, not a P1 violation case, so it belongs in T2 rather than T1.

- **Pattern:** `BehaviourReport.txt` is run output, not a session artifact
  **Observed in:** `src/test/resources/groovy/{Op}CompensationTest/`
  **Description:** `ImpairmentService` writes a `BehaviourReport.txt` beside the fault CSV on every run, so its contents change with the number of times the suite has executed. This session left the two new ones uncommitted; the equivalent files for the sibling aggregate are committed. The harness does not say which is intended, and one of the two states is wrong.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 14

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 14 | 1 | fixed | `59122e1c2` |

---

## One-Line Summary

TrainType's three write functionalities went in unchanged from the patterns the docs and the sibling
aggregate already establish; the only friction was that the compensation-test sanity check the docs
require was uncapturable by the means the docs allow, now fixed.
