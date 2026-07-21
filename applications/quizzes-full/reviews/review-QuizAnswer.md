# Review — QuizAnswer

**App:** quizzes-full
**Aggregate:** QuizAnswer (aggregate #7 in plan.md)
**Reference aggregate name:** Answer (in `applications/quizzes/`)
**Date:** 2026-05-16
**Verdict:** Yellow

> **Yellow** = minor deviations and missing test coverage (interleaving tests absent from 4 test classes); build passes, all operations implemented, structural patterns correct.

---

## Summary

QuizAnswer is structurally sound: all four sessions (2.7.a–d) are ticked in plan.md and all planned operations are present. P1 rules are enforced, P2 event subscriptions and handlers are complete and correct (14 T3 scenarios all pass), and the ServiceMapping camelCase fix from retro 2.7.b is in place. The primary deficit is test coverage: every saga test class that has a lock step is missing its required interleaving test case. Additionally, the P3 ANSWER_BEFORE_START guard is absent from `answerQuestion` — a known deferred item pending Tournament implementation. A dead-code `subscribesEvent()` override and a misleadingly named private helper are the only structural minor findings.

---

## File Inventory

| File (relative to microservices/quizanswer/ or commands/quizanswer/) | In Reference | In Target | Status | Notes |
|-----------------------------------------------------------------------|-------------|-----------|--------|-------|
| `aggregate/QuizAnswer.java` | Yes (as `QuizAnswer.java`) | Yes | OK | — |
| `aggregate/QuestionAnswer.java` | Yes | Yes | OK | — |
| `aggregate/QuestionAnswerDto.java` | Yes | No | Intentional | Target embeds question answer IDs in `QuizAnswerDto.questionAnswerIds`; plan does not list this file |
| `aggregate/AnswerCourseExecution.java` | Yes | No | Intentional | Reference helper entity for TCC snapshot; not part of quizzes-full domain |
| `aggregate/AnsweredQuiz.java` | Yes | No | Intentional | Same as above |
| `aggregate/AnswerStudent.java` | Yes | No | Intentional | Same as above |
| `aggregate/causal/` subtree | Yes | No | Intentional | sagas-only; TCC not implemented |
| `aggregate/sagas/SagaQuizAnswer.java` | Yes | Yes | OK | — |
| `aggregate/sagas/states/QuizAnswerSagaState.java` | Yes | Yes | OK | — |
| `aggregate/sagas/factories/SagasQuizAnswerFactory.java` | Yes | Yes | OK | — |
| `aggregate/sagas/repositories/QuizAnswerCustomRepositorySagas.java` | Yes | Yes | OK | — |
| `aggregate/QuizAnswerCustomRepository.java` | Yes | Yes | OK | — |
| `aggregate/QuizAnswerDto.java` | Yes | Yes | OK | — |
| `aggregate/QuizAnswerFactory.java` | Yes | Yes | OK | — |
| `aggregate/QuizAnswerRepository.java` | Yes | Yes | OK | — |
| `service/QuizAnswerService.java` | Yes | Yes | OK | — |
| `messaging/AnswerCommandHandler.java` (ref) | Yes | Yes (`QuizAnswerCommandHandler.java`) | Renamed | Consistent with app naming |
| `coordination/functionalities/QuizAnswerFunctionalities.java` | Yes | Yes | OK | — |
| `coordination/sagas/StartQuizFunctionalitySagas.java` (ref) | Yes | Yes (`CreateQuizAnswerFunctionalitySagas.java`) | Renamed | quizzes-full domain has explicit CreateQuizAnswer |
| `coordination/sagas/AnswerQuestionFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/ConcludeQuizFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/RemoveQuizAnswerFunctionalitySagas.java` (ref) | Yes | No | Intentional | No remove-by-user-action in quizzes-full plan; removal handled via ByEvent methods |
| `coordination/sagas/RemoveUserFromQuizAnswerFunctionalitySagas.java` (ref) | Yes | No | Intentional | Same as above |
| `coordination/sagas/RemoveQuestionFromQuizAnswerFunctionalitySagas.java` (ref) | Yes | No | Intentional | Same as above |
| `coordination/sagas/UpdateUserNameInQuizAnswerFunctionalitySagas.java` (ref) | Yes | No | Intentional | Name-update is a ByEvent path in quizzes-full, not a standalone saga |
| `coordination/sagas/GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas.java` | No | Yes | Extra | Planned in 2.7.c |
| `coordination/eventProcessing/QuizAnswerEventProcessing.java` | Yes | Yes | OK | — |
| `coordination/webapi/QuizAnswerController.java` | Yes | Yes | OK | — |
| `notification/handling/QuizAnswerEventHandling.java` | Yes | Yes | OK | — |
| `notification/handling/handlers/QuizAnswerEventHandler.java` | Yes | Yes | OK | Reference has per-event handlers; target uses single instanceof-dispatch handler |
| `notification/subscribe/QuizAnswerSubscribes*.java` (7 files) | Yes | Yes | OK | — |
| `commands/quizanswer/GetQuizAnswerByIdCommand.java` | No | Yes | Extra | Required for lock steps in AnswerQuestion/ConcludeQuiz sagas; planned in 2.7.b notes |
| `commands/quizanswer/CreateQuizAnswerCommand.java` | No | Yes | Extra | Planned in 2.7.b |
| `commands/quizanswer/AnswerQuestionCommand.java` | Yes | Yes | OK | — |
| `commands/quizanswer/ConcludeQuizCommand.java` | Yes | Yes | OK | — |
| `commands/quizanswer/GetQuizAnswerByQuizIdAndStudentIdCommand.java` | Yes | Yes | OK | — |
| `QuizAnswerServiceApplication.java` | Yes (as `AnswerServiceApplication.java`) | Yes | Renamed | — |

---

## Structural Review

### `aggregate/QuizAnswer.java`

**Expected:** `@Entity`, extends `Aggregate`, `final` fields for P1 rules, `verifyInvariants()` throws `QuizzesFullException` for QUESTION_ALREADY_ANSWERED.

**Actual:**
```java
@Entity
@Table(name = "quiz_answer")
public abstract class QuizAnswer extends Aggregate {
    private final LocalDateTime creationDate;
    private final Integer quizAggregateId;
    private final Integer userAggregateId;
    private final Integer executionAggregateId;
    ...
```
`final` applied to all four P1 final-field rules: `creationDate`, `quizAggregateId`, `userAggregateId`, `executionAggregateId`. `answerDate` is also final though it is set to `LocalDateTime.now()` at construction; not a plan rule but consistent.

**P1 QUESTION_ALREADY_ANSWERED — Minor naming deviation:**
```java
private boolean questionAlreadyAnswered() {
    Set<Integer> ids = questionAnswers.stream()
            .map(QuestionAnswer::getQuestionAggregateId)
            .collect(Collectors.toSet());
    return ids.size() == questionAnswers.size();
}
@Override
public void verifyInvariants() {
    if (!questionAlreadyAnswered()) {
        throw new QuizzesFullException(QuizzesFullErrorMessage.QUESTION_ALREADY_ANSWERED);
    }
}
```
Behavior is correct: throws when duplicate questionIds exist. However `questionAlreadyAnswered()` returns `true` when the invariant *holds* (no duplicates), which is the opposite of what the name implies. Expected name: `allQuestionsDistinct()` or `questionsHaveNoDuplicates()`. Status: **Minor deviation**.

**`getEventSubscriptions()`:** Builds subscriptions for all 7 subscribed events. UpdateQuestion subscription iterates `questionAnswers` and adds one subscription per QuestionAnswer. Correct.

**Copy constructor:** Copies all fields and deep-copies `questionAnswers` via `new QuestionAnswer(qa)`. Status: **Correct**.

---

### `aggregate/sagas/SagaQuizAnswer.java`

**Expected:** Extends `QuizAnswer`, implements `SagaAggregate`, `sagaState` typed as `SagaState` interface, default and copy constructors reset to `NOT_IN_SAGA`.

**Actual:**
```java
public class SagaQuizAnswer extends QuizAnswer implements SagaAggregate {
    private SagaState sagaState;
    public SagaQuizAnswer() { super(); this.sagaState = GenericSagaState.NOT_IN_SAGA; }
    public SagaQuizAnswer(SagaQuizAnswer other) { super(other); this.sagaState = GenericSagaState.NOT_IN_SAGA; }
```
Copy constructor resets to `NOT_IN_SAGA`. Status: **Correct**.

---

### `aggregate/sagas/states/QuizAnswerSagaState.java`

**Expected:** States for write ops that lock an existing instance + `READ_QUIZ_ANSWER` for downstream (Tournament) lock step.

**Actual:** `READ_QUIZ_ANSWER`, `IN_ANSWER_QUESTION`, `IN_CONCLUDE_QUIZ`. CreateQuizAnswer has no state (creates new instance — correct). Status: **Correct**.

---

### `aggregate/sagas/factories/SagasQuizAnswerFactory.java`

**Expected:** `@Service @Profile("sagas")`, implements `QuizAnswerFactory`, three methods.

**Actual:**
```java
@Service
@Profile("sagas")
public class SagasQuizAnswerFactory implements QuizAnswerFactory {
```
Three methods present: `createQuizAnswer(...)`, `createQuizAnswerCopy(QuizAnswer existing)`, `createQuizAnswerDto(QuizAnswer quizAnswer)`. Status: **Correct**.

---

### `aggregate/sagas/repositories/QuizAnswerCustomRepositorySagas.java`

**Expected:** `@Service @Profile("sagas")`, implements `QuizAnswerCustomRepository`, has `@Autowired QuizAnswerRepository`.

**Actual:** Correctly structured. Filters `state != DELETED`. Status: **Correct**.

---

### `service/QuizAnswerService.java`

**Expected:** `@Service`, constructor-injected repository and unitOfWorkService, `@Autowired` factory. Every method `@Transactional(SERIALIZABLE)`. Create: generates ID, calls factory, calls `registerChanged`. Mutate: load, copy-on-write, mutate copy, `registerChanged(copy)`. Delete: copy-on-write (per updated docs). P3 UNIQUE_QUIZ_ANSWER_PER_STUDENT guard in `createQuizAnswer`. P3 ANSWER_BEFORE_START guard in `answerQuestion` (requires Tournament — deferred).

**Actual — overall structure:** Correct. `AggregateIdGeneratorService` and factory `@Autowired`. Repository and unitOfWorkService constructor-injected.

**P3 UNIQUE_QUIZ_ANSWER_PER_STUDENT:** Present.
```java
if (quizAnswerRepository.findByQuizAggregateIdAndUserAggregateId(quizAggregateId, userAggregateId).isPresent()) {
    throw new QuizzesFullException(UNIQUE_QUIZ_ANSWER_PER_STUDENT);
}
```
Status: **Correct**.

**P3 ANSWER_BEFORE_START:** Absent from `answerQuestion`. Expected: saga fetches `TournamentDto`; service checks `tournament.startTime ≤ now`. Tournament not yet implemented (retro 2.7.b). Status: **Missing** (Major — deferred, must be added in session 2.8).

**Delete pattern (`removeQuizAnswer`):** Uses copy-on-write per updated docs/concepts/service.md. Status: **Correct**.

**`answerQuestion` — `QuestionAnswer` constructor:**
```java
newQA.addQuestionAnswer(new QuestionAnswer(questionAggregateId, questionVersion, optionKey, optionKey, null, timeTaken));
```
`optionSequenceChoice` and `optionKey` are both passed `optionKey`. `correct` is `null` (retro 2.7.b notes this as a known placeholder since `QuestionDto` doesn't expose correctness). Status: **Correct** (known limitation).

---

### `messaging/QuizAnswerCommandHandler.java`

**Expected:** `@Component`, extends `CommandHandler`, `getAggregateTypeName()` returns `"QuizAnswer"`, switch over all command classes, default logs warning.

**Actual:**
```java
@Component
public class QuizAnswerCommandHandler extends CommandHandler {
    @Override
    public String getAggregateTypeName() { return "QuizAnswer"; }
    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            ...
            default -> { logger.warning(...); yield null; }
        };
    }
```
Covers: `GetQuizAnswerByIdCommand`, `CreateQuizAnswerCommand`, `AnswerQuestionCommand`, `ConcludeQuizCommand`, `GetQuizAnswerByQuizIdAndStudentIdCommand`. Default warning present. Status: **Correct**.

---

### `coordination/functionalities/QuizAnswerFunctionalities.java`

**Expected:** `@Service`, one method per functionality (write + read), ByEvent methods for event-driven mutations, sagas instantiated with `new`.

**Actual:** All write sagas (`createQuizAnswer`, `answerQuestion`, `concludeQuiz`) use `new FunctionalitySagas(...)` and call `saga.executeWorkflow(unitOfWork)`. Read saga (`getQuizAnswerByQuizIdAndStudentId`) likewise. ByEvent methods (`removeQuizAnswerByEvent`, `removeQuizAnswerIfDisenrolledByEvent`, `updateStudentNameByEvent`, `anonymizeStudentByEvent`, `updateQuestionVersionByEvent`) call service directly and commit. Status: **Correct**.

---

### `coordination/sagas/CreateQuizAnswerFunctionalitySagas.java`

**Expected:** Two lock steps (Quiz + User with `setSemanticLock`) + create step. Uses `ServiceMapping.ANSWER.getServiceName()` which must equal `"quizAnswer"`.

**Actual:** `ServiceMapping.ANSWER("quizAnswer")` — correct. Quiz and User lock steps with compensations. Create step derives execution from `quizDto.getExecutionId()/getExecutionVersion()` — P4b holds by construction. No Execution fetch step. Status: **Correct** (plan description says "fetches Quiz + User + Execution" but P4b note clarifies execution is derived from QuizDto; Minor deviation from plan wording).

---

### `coordination/sagas/AnswerQuestionFunctionalitySagas.java`

**Expected:** Lock step on QuizAnswer (IN_ANSWER_QUESTION) + Question fetch step + answerQuestion step.

**Actual:** Three steps: `getQuizAnswerStep` (SagaCommand + `setSemanticLock(IN_ANSWER_QUESTION)` + compensation), `getQuestionStep`, `answerQuestionStep`. No Tournament step for ANSWER_BEFORE_START. Status: **Correct** for implemented steps; P3 guard missing (deferred to Tournament implementation).

---

### `coordination/sagas/ConcludeQuizFunctionalitySagas.java`

**Expected:** Lock step on QuizAnswer (IN_CONCLUDE_QUIZ) + conclude step.

**Actual:** Two steps: `getQuizAnswerStep` (SagaCommand + `setSemanticLock(IN_CONCLUDE_QUIZ)` + compensation), `concludeQuizStep`. Status: **Correct**.

---

### `coordination/sagas/GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas.java`

**Expected:** Single step sending the read command, result in instance field.

**Actual:** Single step, `quizAnswerDto` stored, exposed via getter. Status: **Correct**.

---

### `notification/subscribe/QuizAnswerSubscribesDisenrollStudentFromCourseExecution.java`

**Finding:** Overrides `subscribesEvent()` to check `userId`. Per retro 2.7.d, `EventApplicationService` never calls `subscribesEvent()` — this override is dead code. The actual filtering is correctly implemented in `QuizAnswerService.removeQuizAnswerIfUserMatches()`.

```java
@Override
public boolean subscribesEvent(Event event) {
    return super.subscribesEvent(event) &&
            userAggregateId != null &&
            userAggregateId.equals(((DisenrollStudentFromCourseExecutionEvent) event).getUserId());
}
```
Behavior correct at runtime (service-layer filtering handles it). Status: **Minor deviation** (dead code).

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|
| CreateQuizAnswer | Yes | `CreateQuizAnswerFunctionalitySagas` | `CreateQuizAnswerCommand` | `createQuizAnswer` | `CreateQuizAnswerTest` | Missing interleaving tests |
| AnswerQuestion | Yes | `AnswerQuestionFunctionalitySagas` | `AnswerQuestionCommand` | `answerQuestion` | `AnswerQuestionTest` | Missing interleaving test; P3 ANSWER_BEFORE_START absent |
| ConcludeQuiz | Yes | `ConcludeQuizFunctionalitySagas` | `ConcludeQuizCommand` | `concludeQuiz` | `ConcludeQuizTest` | Missing interleaving test |
| GetQuizAnswerByQuizIdAndStudentId | Yes | `GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas` | `GetQuizAnswerByQuizIdAndStudentIdCommand` | `getQuizAnswerByQuizIdAndStudentId` | `GetQuizAnswerByQuizIdAndStudentIdTest` | OK |

---

## Rule Enforcement

| Rule | Classification | Expected Implementation | Actual Implementation | Status |
|------|---------------|------------------------|----------------------|--------|
| QUIZANSWER_FINAL_USER | P1 (final field) | `final Integer userAggregateId` | `private final Integer userAggregateId` | Correct |
| QUIZANSWER_FINAL_QUIZ | P1 (final field) | `final Integer quizAggregateId` | `private final Integer quizAggregateId` | Correct |
| QUIZANSWER_FINAL_COURSE_EXECUTION | P1 (final field) | `final Integer executionAggregateId` | `private final Integer executionAggregateId` | Correct |
| QUIZANSWER_FINAL_CREATION_DATE | P1 (final field) | `final LocalDateTime creationDate` | `private final LocalDateTime creationDate` | Correct |
| QUESTION_ALREADY_ANSWERED | P1 (intra-invariant) | `verifyInvariants()` throws QuizzesFullException | Present; `questionAlreadyAnswered()` helper name is misleading but logic is correct | Minor deviation (naming) |
| UNIQUE_QUIZ_ANSWER_PER_STUDENT | P3 (service guard) | Own-table check in `createQuizAnswer()` | `quizAnswerRepository.findByQuizAggregateIdAndUserAggregateId(...)` before factory call | Correct |
| ANSWER_BEFORE_START | P3 (service guard) | `answerQuestion()` fetches TournamentDto; service checks `tournament.startTime ≤ now` | Absent — Tournament not yet implemented | Missing (Major) |
| COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION | P4b | Saga passes `quizDto.getExecutionId()/getExecutionVersion()` to CreateQuizAnswerCommand | `CreateQuizAnswerFunctionalitySagas` passes `quizDto.getExecutionId()/getExecutionVersion()` | Correct |
| USER_EXISTS (QuizAnswer) | P2 | Subscriptions to DeleteUserEvent, UpdateStudentNameEvent, AnonymizeStudentEvent, DisenrollStudentFromCourseExecutionEvent | All four subscriptions present and wired | Correct |
| QUIZ_EXISTS (QuizAnswer) | P2 | Subscription to InvalidateQuizEvent | Present | Correct |
| COURSE_EXECUTION_EXISTS (QuizAnswer) | P2 | Subscription to DeleteCourseExecutionEvent | Present | Correct |

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `QuizAnswerTest` | "create quiz answer" (constructor + field assertions) | — | Correct |
| T2 | `CreateQuizAnswerTest` | success; UNIQUE_QUIZ_ANSWER_PER_STUDENT violation | Interleaving for Quiz lock step; Interleaving for User lock step | 2 Major gaps |
| T2 | `AnswerQuestionTest` | success; QUESTION_ALREADY_ANSWERED violation | Interleaving for QuizAnswer lock step (IN_ANSWER_QUESTION); ANSWER_BEFORE_START violation (deferred) | 1 Major gap + 1 deferred |
| T2 | `ConcludeQuizTest` | success | Interleaving for QuizAnswer lock step (IN_CONCLUDE_QUIZ) | 1 Major gap |
| T2 | `GetQuizAnswerByQuizIdAndStudentIdTest` | success; not-found | — | Not-found correctly uses `thrown(QuizzesFullException)` (composite-key service path, per retro 2.7.c) |
| T3 | `QuizAnswerInterInvariantTest` | 2 cases per subscribed event (reflects + ignores); all 7 events covered (14 scenarios total) | — | Complete |

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|
| 2.7.a | Domain layer | Medium | `/classify-and-plan` skill/plan template: ensure `QuizAnswerCustomRepository.java` and `QuizAnswerFactory.java` (interface files) are always listed in the 2.N.a file table |
| 2.7.b | Write functionalities | High | `docs/concepts/commands.md`: add rule that ServiceMapping value must match `resolveServiceName(aggregateClass)` — multi-word aggregates use camelCase |
| 2.7.b | Write functionalities | High | `session-a.md`: add note for multi-word aggregates — ServiceMapping value must be camelCase matching class name minus "Saga" |
| 2.7.b | Write functionalities | Medium | `session-b.md`: add guidance — if P3 check requires DTO from unimplemented aggregate, skip and flag; add TODO comment |
| 2.7.b | Write functionalities | Medium | `docs/concepts/service.md`: add guidance on placeholder values for owned entity fields not derivable from upstream DTOs |
| 2.7.c | Read functionalities | Medium | `docs/concepts/testing.md`: clarify not-found assertion — distinguish infrastructure (`SimulatorException`) vs. service-level (`QuizzesFullException`) not-found paths |
| 2.7.d | Event wiring | High | `session-d.md`: add section — "Shared-anchor events require service-layer userId filtering; `subscribesEvent()` override is NOT called by `EventApplicationService`" |
| 2.7.d | Event wiring | High | `docs/concepts/events.md`: clarify that `subscribesEvent()` is not called in the event processing loop |
| 2.7.d | Event wiring | Medium | `session-d.md`: add guidance on UpdateQuestionEvent for consumers that don't cache question payload (update `questionVersion` only) |
| 2.7.d | Event wiring | Medium | `session-d.md`: T3 test section — never hardcode version numbers, always capture `versionBefore` after setup |

**High-priority items targeting source files — resolution check:**

- **2.7.b High: `docs/concepts/commands.md` ServiceMapping camelCase rule** — This is a docs/skill file target, not a source file. No source-file investigation needed.
- **2.7.d High: `docs/concepts/events.md` and `session-d.md` `subscribesEvent()` clarification** — This is a docs/skill file target, not a source file. No source-file investigation needed.

All High-priority items from retros target docs or skill files (not source files). No source-file-targeted High-priority item is unresolved.

---

## Build & Test Results

**Command:** `mvn clean -Ptest-sagas test -Dtest="QuizAnswerTest,CreateQuizAnswerTest,AnswerQuestionTest,ConcludeQuizTest,GetQuizAnswerByQuizIdAndStudentIdTest,QuizAnswerInterInvariantTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| `QuizAnswerTest` | PASS | 0 |
| `CreateQuizAnswerTest` | PASS | 0 |
| `AnswerQuestionTest` | PASS | 0 |
| `ConcludeQuizTest` | PASS | 0 |
| `GetQuizAnswerByQuizIdAndStudentIdTest` | PASS | 0 |
| `QuizAnswerInterInvariantTest` | PASS | 0 |

Total: 22 tests run, 0 failures, 0 errors, 0 skipped.

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Major | Test coverage | `sagas/coordination/quizanswer/CreateQuizAnswerTest.groovy` | Missing interleaving test for Quiz lock step (`SagaCommand` + `setSemanticLock(READ_QUIZ)` in `CreateQuizAnswerFunctionalitySagas`) | Add test: concurrently lock Quiz while CreateQuizAnswer is in-flight; verify saga aborts cleanly |
| Major | Test coverage | `sagas/coordination/quizanswer/CreateQuizAnswerTest.groovy` | Missing interleaving test for User lock step (`SagaCommand` + `setSemanticLock(READ_USER)` in `CreateQuizAnswerFunctionalitySagas`) | Add test: concurrently lock User while CreateQuizAnswer is in-flight; verify saga aborts cleanly |
| Major | Test coverage | `sagas/coordination/quizanswer/AnswerQuestionTest.groovy` | Missing interleaving test for QuizAnswer lock step (`setSemanticLock(IN_ANSWER_QUESTION)` in `AnswerQuestionFunctionalitySagas`) | Add test: concurrently lock QuizAnswer while AnswerQuestion is in-flight; verify saga aborts |
| Major | Test coverage | `sagas/coordination/quizanswer/ConcludeQuizTest.groovy` | Missing interleaving test for QuizAnswer lock step (`setSemanticLock(IN_CONCLUDE_QUIZ)` in `ConcludeQuizFunctionalitySagas`) | Add test: concurrently lock QuizAnswer while ConcludeQuiz is in-flight; verify saga aborts |
| Major | Rule enforcement | `coordination/sagas/AnswerQuestionFunctionalitySagas.java` + `service/QuizAnswerService.java` | P3 ANSWER_BEFORE_START guard absent — `AnswerQuestionFunctionalitySagas` does not fetch TournamentDto; service does not check `tournament.startTime ≤ now` | Implement in session 2.8 (Tournament): add Tournament fetch step to `AnswerQuestionFunctionalitySagas`; add `startTime` check in `answerQuestion` service method |
| Minor | Naming | `aggregate/QuizAnswer.java` | `questionAlreadyAnswered()` helper returns `true` when invariant holds (no duplicates), opposite of what the name implies | Rename to `allQuestionsDistinct()` or `noDuplicateQuestions()` |
| Minor | Dead code | `notification/subscribe/QuizAnswerSubscribesDisenrollStudentFromCourseExecution.java` | `subscribesEvent()` override is never called by `EventApplicationService`; actual filtering is in `QuizAnswerService.removeQuizAnswerIfUserMatches()` | Remove the override or add a comment noting it is dead code; no runtime impact |
| Minor | Plan deviation | `coordination/sagas/CreateQuizAnswerFunctionalitySagas.java` | Plan description says "saga fetches Quiz + User + Execution" but saga only fetches Quiz and User; Execution data derived from `quizDto` — consistent with P4b "holds by construction" | Update plan description wording to "saga fetches Quiz + User; execution data derived from QuizDto" |
