# Retro — 2.1.c — Course

**App:** quizzes-full
**Session:** 2.1.c (Read Functionalities)
**Date:** 2026-04-25

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/coordination/sagas/GetCourseByIdFunctionalitySagas.java` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/coordination/functionalities/CourseFunctionalities.java` (modified — added `getCourseById` method and import)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/course/GetCourseByIdTest.groovy` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified — ticked `[x] 2.1.c`)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | Read method pattern | Partial | Covered the read method signature well. Did not mention that the method may already exist from the previous session (moved from 2.1.c to 2.1.b in plan.md). No friction here since the plan.md note was clear. |
| `docs/concepts/commands.md` | Naming conventions (read commands) | Yes | Confirmed `GetCourseByIdCommand` naming; command was already produced in 2.1.b. |
| `docs/concepts/sagas.md` | Workflow structure section | Partial | Covered the single-step workflow pattern. No dedicated "read saga" subsection; had to infer from the write-saga examples and the reference app. |
| `docs/concepts/testing.md` | T2 section (happy-path and not-found) | Partial | Template showed happy-path and guard-violation cases but did not specify which exception type to use for not-found (infrastructure-level `SimulatorException` vs app-level exception). Required reference-app lookup. |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/coordination/sagas/GetCourseExecutionByIdFunctionalitySagas.java` | Needed to confirm single-step read saga constructor signature and the `setCourseExecutionDto` pattern (setter vs direct field assignment) | `docs/concepts/sagas.md` has no dedicated "read saga" section; the workflow structure examples only show write sagas |
| `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/tournament/UpdateTournamentTest.groovy` | Needed to determine correct exception type for aggregate-not-found assertions in tests | `docs/concepts/testing.md` T2 template says `thrown(<App>Exception)` but not-found is thrown by the infrastructure as `SimulatorException`, not the app exception |

---

## Skill Instructions Feedback

### What worked well

- The plan.md file list for 2.1.c was clear: three entries, well-defined.
- The note in the plan.md that `GetCourseByIdCommand` and `getCourseById` were moved from 2.1.c to 2.1.b prevented duplicate work.
- The session-c.md "Reads" section listed exactly the right prerequisite files to load before coding.

### What was unclear or missing

- **BeanConfigurationSagas update instruction is wrong.** session-c.md says: "add one `@Bean` method per read `FunctionalitySagas` class" with a no-arg constructor template. `FunctionalitySagas` classes take per-request constructor arguments (`SagaUnitOfWork`, aggregate IDs) and cannot be Spring singleton beans. The instruction is a copy-paste artifact from service-layer beans. No `BeanConfigurationSagas.groovy` change was made, and the tests pass.
- **No instruction to add the read method to `CourseFunctionalities.java`.** The skill lists `coordination/sagas/GetCourseByIdFunctionalitySagas.java` as a file to produce, but says nothing about wiring it into the `CourseFunctionalities` coordinator. The test calls `courseFunctionalities.getCourseById(...)`, so this is required. It was inferred from the 2.1.b pattern.
- **No guidance on which exception type to assert in not-found tests.** The T2 template in `docs/concepts/testing.md` uses `thrown(<App>Exception)` generically. For not-found, the infrastructure throws `SimulatorException`; the app exception is only thrown by domain/service guards.

### Suggested wording / structure changes

- **`session-c.md` — BeanConfigurationSagas section:** Replace the `@Bean FunctionalitySagas` template with: "The `*Functionalities` coordinator bean was already registered in session `b`. No new Spring bean registration is needed for `FunctionalitySagas` classes — they are per-request objects instantiated inside the coordinator methods."
- **`session-c.md` — Produce section:** Add a bullet: "Append a `getCourseById` method to the existing `{Aggregate}Functionalities.java` coordinator, following the same pattern as the write methods in session `b`. The method creates a `SagaUnitOfWork`, instantiates `{Query}FunctionalitySagas`, calls `executeWorkflow`, and returns the DTO from the saga."
- **`docs/concepts/testing.md` — T2 not-found subsection:** Add a note: "Aggregate-not-found is thrown by the infrastructure as `SimulatorException` (from `pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException`), not the app-level exception. Use `thrown(SimulatorException)` for these cases."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/sagas.md` | No dedicated section for read sagas (single-step, no compensation, no semantic lock) | Medium | Add a "Read Functionality Sagas" subsection showing the minimal single-step pattern with a getter for the result DTO |
| `docs/concepts/testing.md` | T2 template uses `thrown(<App>Exception)` for not-found, but infrastructure throws `SimulatorException` | Medium | Clarify: domain/guard violations → `<App>Exception`; infrastructure not-found → `SimulatorException`. Add import note. |

---

## Patterns to Capture

- **Pattern:** Read saga is a thin single-step wrapper with a result getter
  **Observed in:** `GetCourseByIdFunctionalitySagas.java`
  **Description:** A read `FunctionalitySagas` has one `SagaStep` that sends the read command and stores the returned DTO in an instance field. It provides a getter. No compensation, no forbidden states. The coordinator method creates it inline, calls `executeWorkflow`, and returns `saga.getXxxDto()`.

- **Pattern:** `FunctionalitySagas` are per-request objects, not Spring beans
  **Observed in:** all `*FunctionalitySagas` constructors; `CourseFunctionalities.createCourse`, `getCourseById`
  **Description:** `FunctionalitySagas` classes receive a `SagaUnitOfWork` in their constructor, making them unit-of-work–scoped. They are created `new` inside `*Functionalities` coordinator methods. Only the `*Functionalities` class is a Spring `@Service` / `@Bean`.

- **Pattern:** Not-found assertion uses `SimulatorException`, not app exception
  **Observed in:** `GetCourseByIdTest.groovy` (confirmed via `UpdateTournamentTest.groovy` reference)
  **Description:** When an aggregate ID does not exist, `SagaUnitOfWorkService.aggregateLoadAndRegisterRead` throws `SimulatorException`. Domain-rule violations (invariants, guards in the service layer) throw the app-level exception (e.g., `QuizzesFullException`). Tests must import the right type.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-c.md` | Replace `@Bean FunctionalitySagas` template with a note that no bean registration is needed; add instruction to append `getCourseById` to the `Functionalities` coordinator |
| High | `docs/concepts/testing.md` | Add a not-found subsection to T2 clarifying `SimulatorException` vs app exception with import |
| Medium | `docs/concepts/sagas.md` | Add "Read Functionality Sagas" subsection with a minimal single-step example |

---

## One-Line Summary

The `session-c.md` BeanConfigurationSagas instruction is actively wrong (FunctionalitySagas cannot be Spring beans) and the skill omits the required step of adding the read method to the Functionalities coordinator.
