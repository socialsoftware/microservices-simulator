# Retro — 2.4.a — Execution

**App:** quizzes-full
**Session:** 2.4.a (Domain Layer)
**Date:** 2026-05-02

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/Execution.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionCourse.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionStudent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionFactory.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionCustomRepository.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionDto.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionStudentDto.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionRepository.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/sagas/SagaExecution.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/sagas/states/ExecutionSagaState.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/sagas/factories/SagasExecutionFactory.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/sagas/repositories/ExecutionCustomRepositorySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/ExecutionServiceApplication.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/execution/ExecutionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Sagas variant, § Factories, § Repositories, § Naming Conventions, § getEventSubscriptions() | Yes | — |
| `docs/concepts/testing.md` | § T1 — Creation Test | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | All sections | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The session-a.md instructions for owned entity patterns (@OneToOne with mappedBy vs @OneToMany) were clear and directly applicable.
- The aggregate grouping doc (§2 Snapshots) was the authoritative source for which fields to cache in ExecutionCourse and ExecutionStudent — no guessing required.
- The existing TopicCourse pattern provided a clear reference for how to implement the @OneToOne bidirectional link (setExecution wiring).

### What was unclear or missing

- The plan.md 2.4.a file table omitted ExecutionCourse, ExecutionStudent, ExecutionFactory, ExecutionCustomRepository, and ExecutionStudentDto — all required to compile. The skill says to add unambiguous omissions but the pattern of missing entity/interface classes is recurring across aggregates that have owned entities or snapshot sub-entities. The plan.md template could be improved to auto-include these.

### Suggested wording / structure changes

- none

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `applications/quizzes-full/plan.md` template generation | Owned entity classes (ExecutionCourse, ExecutionStudent) and interface files (ExecutionFactory, ExecutionCustomRepository) are not included in the plan.md file table when classify-and-plan generates it | Medium | Update classify-and-plan skill to include owned entity and interface files in 2.N.a rows for aggregates with snapshots or collections |

---

## Patterns to Capture

- **Pattern:** ExecutionStudent as owned entity for N→M user snapshot
  **Observed in:** `Execution.java`, `ExecutionStudent.java`
  **Description:** When an aggregate holds a set of user references (students), each is an owned entity with cached fields (userId, name, username, active). The `@OneToMany(cascade = ALL, orphanRemoval = true)` join-table approach is used without a back-reference to the parent aggregate. Copy constructor iterates and creates new `ExecutionStudent` instances.

- **Pattern:** ExecutionSagaState has only READ_EXECUTION
  **Observed in:** `ExecutionSagaState.java`
  **Description:** All write operations on Execution are two-step sagas (read → write-as-final-step) or involve post-write steps on other aggregates (Course). No IN_UPDATE/IN_DELETE states are needed because the Execution aggregate is not re-read by the saga after its own primary write step.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `.claude/skills/classify-and-plan` | When generating 2.N.a file rows for aggregates with snapshot sub-entities, include owned entity classes and factory/repository interfaces in the file table |

---

## One-Line Summary

Plan.md omitted five required files (two entity classes, two interface files, one student DTO) — all unambiguously required to compile; added to plan.md under step 5b.
