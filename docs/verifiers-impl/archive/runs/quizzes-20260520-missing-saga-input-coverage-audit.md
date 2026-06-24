# Missing Saga Input Coverage Audit

Run: `/home/andre/microservices-simulator/verifiers/target/quizzes-20260520-012233-812`

## Scope Notes
- This is an artifact-level verifier audit, not a claim that no application test executed the functionality.
- "No accepted verifier input variant" means the exact saga FQN has no accepted `InputVariant` in this run after static extraction, adaptation, input-policy filtering, source-mode filtering, and per-saga caps.
- Dynamic evidence is additive sidecar evidence. A runtime `NO_MATCH` event can prove that a saga executed during tests, but it still does not make the saga input-bound unless a static input variant was accepted for the same FQN.
- This run was intentionally capped: `maxSagaSetSize=1`, `maxScenarios=100`, `maxInputVariantsPerSaga=3`, `scheduleStrategy=SERIAL`, `allowTypeOnlyFallback=false`, `inputPolicy=RESOLVED_OR_REPLAYABLE`.
- The `66` accepted input count below is therefore capped-run evidence. Later full-input accounting runs raised accepted inputs to `485`, while the missing-saga set remained `40`.

## Artifact Counts
- `sagasAdapted`: 65
- `sagasWithoutUsableInputs`: 40
- `inputTracesSeen`: 1588
- `inputVariantsAdapted`: 554
- `inputVariantsAccepted`: 66
- `inputVariantsExcludedByPolicy`: 13
- `inputVariantsRejectedBySourceMode`: 69
- `inputVariantsCapped`: 406
- `inputSagasWithInputs`: 25
- `scenariosExported`: 66
- `rejectedInputsExported`: 69

## Classification Counts
- No accepted verifier input variant for exact saga FQN: 40
- Duplicate simple-name pitfall requiring FQN disambiguation: 1
- Runtime-only evidence without accepted static input variant: 1

## Important Interpretations
- `RemoveCourseExecutionFunctionalitySagas` is not one saga. The accepted static catalog rows belong to `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveCourseExecutionFunctionalitySagas`; the missing row below is `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveCourseExecutionFunctionalitySagas`.
- `CreateQuestionFunctionalitySagas` did execute at runtime in this run. It appears in `dynamic-evidence/dynamic-evidence.jsonl` with `inputVariantAttributionStatus="NO_MATCH"`, but it does not appear in `scenario-catalog.jsonl`, `scenario-catalog-enriched.jsonl`, or `scenario-catalog-rejected-inputs.jsonl` for this run.
- The original scratch CSV was `/tmp/opencode/missing-saga-input-coverage-audit.csv`. Treat this Markdown file as the durable record.

## Missing Sagas
| Classification | Bounded context | Saga FQN | Note |
|---|---|---|---|
| No accepted verifier input variant | answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.AnswerQuestionFunctionalitySagas` |  |
| No accepted verifier input variant | answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.ConcludeQuizFunctionalitySagas` |  |
| No accepted verifier input variant | answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuestionFromQuizAnswerFunctionalitySagas` |  |
| No accepted verifier input variant | answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas` |  |
| No accepted verifier input variant | answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveUserFromQuizAnswerFunctionalitySagas` |  |
| No accepted verifier input variant | answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas` |  |
| No accepted verifier input variant | execution | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.CreateQuestionFunctionalitySagas` | Runtime evidence exists, but all events are `NO_MATCH` against static input variants in this run. |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.DeleteTopicInQuestionFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.FindQuestionByAggregateIdFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.FindQuestionsByCourseFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.RemoveQuestionFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionTopicsFunctionalitySagas` |  |
| No accepted verifier input variant | question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateTopicInQuestionFunctionalitySagas` |  |
| No accepted verifier input variant | quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.GetAvailableQuizzesFunctionalitySagas` |  |
| No accepted verifier input variant | quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveCourseExecutionFromQuizFunctionalitySagas` |  |
| No accepted verifier input variant | quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveQuizQuestionFunctionalitySagas` |  |
| No accepted verifier input variant | quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuestionInQuizFunctionalitySagas` |  |
| No accepted verifier input variant | quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas` |  |
| No accepted verifier input variant | topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas` | Same simple name also exists in the tournament bounded context; use FQN. |
| No accepted verifier input variant | topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.FindTopicsByCourseFunctionalitySagas` |  |
| No accepted verifier input variant | topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.GetTopicByIdFunctionalitySagas` |  |
| No accepted verifier input variant | topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas` | Same simple name also exists in the tournament bounded context; use FQN. |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantAsyncFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.DeleteTopicFunctionalitySagas` | Same simple name also exists in the topic bounded context; use FQN. |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.FindParticipantFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetClosedTournamentsForCourseExecutionFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetOpenedTournamentsForCourseExecutionFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetTournamentsForCourseExecutionFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.InvalidateQuizFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveCourseExecutionFunctionalitySagas` | Accepted simple-name evidence belongs to the execution FQN, not this tournament FQN. |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveUserFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateParticipantAnswerFunctionalitySagas` |  |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTopicFunctionalitySagas` | Same simple name also exists in the topic bounded context; use FQN. |
| No accepted verifier input variant | tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas` |  |
| No accepted verifier input variant | user | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.FindUserByIdFunctionalitySagas` |  |
| No accepted verifier input variant | user | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.GetStudentsFunctionalitySagas` |  |
| No accepted verifier input variant | user | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.GetTeachersFunctionalitySagas` |  |
