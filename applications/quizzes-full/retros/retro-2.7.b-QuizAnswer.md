# Retro — 2.7.b — QuizAnswer

**App:** quizzes-full
**Session:** 2.7.b (Write Functionalities)
**Date:** 2026-05-16

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/sagas/states/QuizAnswerSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/QuizAnswerQuestionAnswerEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quizanswer/GetQuizAnswerByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quizanswer/CreateQuizAnswerCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quizanswer/AnswerQuestionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quizanswer/ConcludeQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/service/QuizAnswerService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/messaging/QuizAnswerCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/sagas/CreateQuizAnswerFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/sagas/AnswerQuestionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/sagas/ConcludeQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/functionalities/QuizAnswerFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/webapi/QuizAnswerController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/quizanswer/CreateQuizAnswerTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/quizanswer/AnswerQuestionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/quizanswer/ConcludeQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md`

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | Create pattern, mutate pattern, copy-on-write, P3 guard placement | Yes | — |
| `docs/concepts/commands.md` | Command structure, ServiceMapping, CommandHandler routing, bean naming | Partial | Missing: ServiceMapping value must match `resolveServiceName(aggregateClassName)` — see gaps |
| `docs/concepts/sagas.md` | Lock-acquisition step pattern, SagaCommand, setSemanticLock, compensation | Yes | — |
| `docs/concepts/testing.md` | T2 template, T2 service-command variant | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- Lock-acquisition step pattern (SagaCommand + setSemanticLock + compensation) was clearly described and produced correct code without ambiguity.
- Session-b prerequisite note about creating `Get{Aggregate}ByIdCommand` proactively was essential; without it the AnswerQuestion/ConcludeQuiz sagas would have been missing a required command.
- Copy-on-write rule for mutate methods was clearly described.

### What was unclear or missing

- The `ServiceMapping` value constraint: the docs do not state that the service name in `ServiceMapping` **must** equal the result of `resolveServiceName(aggregateClassName)`. For multi-word aggregate names like `QuizAnswer`, `ANSWER("answer")` was chosen in session-a but the framework resolves "quizAnswer" from "SagaQuizAnswer". This caused a bean lookup failure at runtime for commit/abort commands.
- `ANSWER_BEFORE_START` P3 check requires `TournamentDto`, but Tournament (aggregate 8) is not yet implemented. The docs do not address this ordering problem — what to do when a P3 DTO check depends on a downstream aggregate that hasn't been implemented yet.
- `QuestionAnswer` constructor requires `correct` (Boolean) and `optionSequenceChoice`, but `QuestionDto` only exposes `optionKeys` (not correctness or sequence). The service has no way to determine correctness from available DTOs. The docs don't address this gap.

### Suggested wording / structure changes

- `docs/concepts/commands.md` — Add a note to the ServiceMapping section: "The service name must equal the lowercase-first result of stripping 'Saga' from the aggregate class name (e.g., `SagaQuizAnswer` → `quizAnswer`). Multi-word aggregates must use camelCase, not a shortened alias."
- `session-b.md` — Add a note: "If a P3 DTO check depends on an aggregate not yet implemented (e.g., in a later session), skip the check and flag it explicitly in the session report. Add a TODO comment in the saga class."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | ServiceMapping value must match `resolveServiceName(aggregateClassName)` — the framework derives the commit/abort service name from the aggregate class name, not from the command's stored service name | High — caused silent bean lookup failure at runtime for AnswerQuestion and ConcludeQuiz sagas | Add explicit rule: service name = lowercase-first(aggregateClassName.replace("Saga", "")) |
| `docs/concepts/service.md` | No guidance on how to handle domain object constructor fields (e.g., `QuestionAnswer.correct`) when the needed data is not available in upstream DTOs | Medium — required inference; set `correct = null` as placeholder | Add section on partial-data owned entities and placeholder values |
| `.claude/skills/implement-aggregate/session-b.md` | No guidance on P3 checks that depend on unimplemented downstream aggregates | Medium — required inference; skipped check entirely | Add note: if upstream DTO not yet available, skip the check, add TODO, flag in report |

---

## Patterns to Capture

- **Pattern:** ServiceMapping camelCase constraint for multi-word aggregates
  **Observed in:** `ServiceMapping.java`, `SagaUnitOfWorkService.resolveServiceName()`
  **Description:** `resolveServiceName()` strips "Saga" from the aggregate class simple name and lowercases the first character. `ServiceMapping` values must match this result exactly (e.g., `SagaQuizAnswer` → "quizAnswer"). Shortened aliases like "answer" cause commit/abort bean lookup failures that only manifest when the aggregate itself is locked via SagaCommand.

- **Pattern:** Spring bean name for CommandHandler must match `resolveServiceName` output
  **Observed in:** `BeanConfigurationSagas.groovy`
  **Description:** `@Bean` method name must equal `ServiceMapping.{AGGREGATE}.getServiceName() + "CommandHandler"` AND that service name must equal `resolveServiceName(aggregateType)`. For multi-word aggregates, use camelCase (e.g., `quizAnswerCommandHandler`).

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `docs/concepts/commands.md` | Add rule: ServiceMapping value must match `resolveServiceName(aggregateClass)` — multi-word aggregates use camelCase |
| High | `.claude/skills/implement-aggregate/session-a.md` | Add note: for multi-word aggregates, ServiceMapping value must be camelCase matching the class name (minus "Saga"), e.g., QuizAnswer → "quizAnswer" |
| Medium | `.claude/skills/implement-aggregate/session-b.md` | Add guidance: if P3 check requires a DTO from an unimplemented aggregate, skip and flag; add TODO comment |
| Medium | `docs/concepts/service.md` | Add guidance on placeholder values for owned entity fields not derivable from upstream DTOs |

---

## One-Line Summary

The critical finding is that `ServiceMapping` values for multi-word aggregates must use camelCase matching `resolveServiceName()` output — mismatching "answer" vs "quizAnswer" causes silent commit/abort routing failures only visible when the aggregate is locked via SagaCommand.
