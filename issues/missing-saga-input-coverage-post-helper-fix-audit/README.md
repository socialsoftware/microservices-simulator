# Missing Saga Input Coverage Audit After Helper-Facade Fix

## Scope

This audit records the state after committing:

```text
bd208d54 fix(verifiers): trace facade calls inside setup helpers
```

It is an audit only. It does not propose or make further code changes.

Important wording:

- "Missing" means **no accepted static verifier input variant for the exact saga FQN**.
- It does **not** necessarily mean no application/runtime test executed the saga.
- Dynamic runtime evidence is sidecar evidence; it does not make a saga input-bound unless the static verifier emits an accepted `InputVariant` for the same FQN.

## Artifact Used

Post-fix Quizzes static smoke artifact:

```text
verifiers/target/missing-input-investigation-after-helper/quizzes-20260623-110739-661
```

Relevant files:

```text
scenario-space-accounting.json
scenario-catalog.jsonl
scenario-catalog-rejected-inputs.jsonl
```

Comparison baseline from before the helper-facade fix:

```text
verifiers/target/missing-input-investigation/quizzes-20260623-110006-321
```

The older dynamic evidence artifact checked for runtime-only evidence was:

```text
verifiers/target/quizzes-20260520-012233-812/dynamic-evidence/dynamic-evidence.jsonl
```

## Summary Counts

| Metric | Before helper fix | After helper fix | Delta |
|---|---:|---:|---:|
| Discovered sagas | 65 | 65 | 0 |
| Sagas with accepted inputs | 25 | 26 | +1 |
| Sagas without accepted inputs | 40 | 39 | -1 |
| Accepted input variants | 169 | 179 | +10 |
| Input-bound single-saga scenario space | 169 | 179 | +10 |
| Catalog written | 80 | 80 | 0, capped |
| Rejected inputs exported | 69 | 70 | +1 |
| Executor-ready inputs | 0 | 0 | 0 |

The catalog remained capped at 80 because the smoke run used:

```text
--verifiers.scenario-catalog.max-catalog-scenarios=80
```

## CreateQuestion Result

`CreateQuestionFunctionalitySagas` was the known confusing case. After the helper-facade fix it is no longer missing.

Before:

```text
scenario-catalog.jsonl: 0 CreateQuestion entries
scenario-catalog-rejected-inputs.jsonl: 0 CreateQuestion entries
```

After:

```text
scenario-catalog.jsonl: 4 CreateQuestion entries
scenario-catalog-rejected-inputs.jsonl: 1 rejected TCC entry
scenario-space-accounting.json: 10 accepted CreateQuestion input variants
```

The +10 accepted variants are consistent with the smoke cap:

```text
--verifiers.scenario-catalog.max-input-variants-per-saga=10
```

Many tests call `createQuestion(...)`, but variants deduplicate and then hit the per-saga cap. Only four were written to `scenario-catalog.jsonl` because the whole catalog was capped at 80 scenarios.

## Remaining Missing Sagas

Post-fix missing count: **39**.

Important audit result: none of these 39 had accepted or rejected static input variants in the post-fix artifact. In this run, the remaining missing set is not explained by source-mode rejection or input-policy rejection; the static tracing/adaptation pipeline emitted no accepted/rejected usable input variant for those exact FQNs.

## Group A: Runtime Evidence Exists, But No Static Input Variant

These are the highest-priority audit targets. Runtime evidence from the older dynamic run indicates these sagas executed, but the post-fix static catalog still has no accepted input variant for the exact FQN.

| Saga FQN | Runtime evidence count checked | Notes |
|---|---:|---|
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas` | 10 | Runtime-only evidence remains; static input still missing. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas` | 5 | Runtime-only evidence remains; static input still missing. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas` | 5 | Runtime-only evidence remains; static input still missing. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas` | 74 | Runtime-only evidence remains; static input still missing. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas` | 92 | Runtime-only evidence remains; static input still missing. |

Interpretation: these are closest to the former `CreateQuestionFunctionalitySagas` situation. They may still indicate verifier extraction gaps rather than missing application tests.

## Group B: Method-Name Occurrences Found, But Not Proof of Exact Saga Input

Simple method-name grep found occurrences for these, but the hits are likely not exact missing-saga facade calls or are ambiguous by bounded context. These require receiver-aware inspection before classifying as verifier bugs.

| Saga FQN | Method-name grep | Observed caveat |
|---|---:|---|
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantAsyncFunctionalitySagas` | 1 | Has `addParticipantAsync(...)` call in tests; static extraction still emits no input. Needs receiver/source-mode inspection. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveCourseExecutionFunctionalitySagas` | 5 | Grep hits are `courseExecutionFunctionalities.removeCourseExecution(...)`, i.e. execution bounded context, not tournament FQN. This is the duplicate-simple-name pitfall. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveUserFunctionalitySagas` | 2 | Grep hits need receiver inspection; likely comments or non-facade context. |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.GetStudentsFunctionalitySagas` | 51 | Hits are mostly DTO/assertion `getStudents()` calls, not `userFunctionalities.getStudents(...)`. |

Interpretation: these are not enough to claim either application test gaps or verifier bugs.

## Group C: No Obvious Test Call or Runtime Evidence Found In Checked Artifacts

For these, no method-name test occurrence and no runtime evidence was found in the checked artifacts. They may be actual application coverage gaps, indirectly event-driven paths, or unsupported/static-invisible invocation patterns. Do not state "no test exists" without further source inspection.

| Bounded context | Saga FQN |
|---|---|
| answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.AnswerQuestionFunctionalitySagas` |
| answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.ConcludeQuizFunctionalitySagas` |
| answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuestionFromQuizAnswerFunctionalitySagas` |
| answer | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveUserFromQuizAnswerFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.DeleteTopicInQuestionFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.FindQuestionByAggregateIdFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.FindQuestionsByCourseFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.RemoveQuestionFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionTopicsFunctionalitySagas` |
| question | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateTopicInQuestionFunctionalitySagas` |
| quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.GetAvailableQuizzesFunctionalitySagas` |
| quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveCourseExecutionFromQuizFunctionalitySagas` |
| quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveQuizQuestionFunctionalitySagas` |
| quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuestionInQuizFunctionalitySagas` |
| quiz | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas` |
| topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas` |
| topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.FindTopicsByCourseFunctionalitySagas` |
| topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.GetTopicByIdFunctionalitySagas` |
| topic | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.DeleteTopicFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.FindParticipantFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetClosedTournamentsForCourseExecutionFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetOpenedTournamentsForCourseExecutionFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetTournamentsForCourseExecutionFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.InvalidateQuizFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateParticipantAnswerFunctionalitySagas` |
| tournament | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTopicFunctionalitySagas` |
| user | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.FindUserByIdFunctionalitySagas` |
| user | `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.GetTeachersFunctionalitySagas` |

## Current Interpretation

The remaining 39 should not be treated as a uniform category.

- Group A likely contains the next verifier extraction gaps or event-driven/static-invisible paths.
- Group B contains name hits that need receiver/FQN disambiguation.
- Group C may include actual application test gaps, but this has not been proven.

Recommended next audit step: inspect Group A first with receiver-aware source paths and dynamic event provenance. Do not change extractor behavior until a concrete recurring pattern is identified.

## Commands Used

Focused validation for helper-facade fix:

```bash
cd verifiers && mvn -q -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec,ApplicationAnalysisScenarioModelAdapterSpec test
```

Quizzes static smoke used to produce the post-fix artifact:

```bash
cd verifiers && mvn -q spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/home/andre/microservices-simulator/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/home/andre/microservices-simulator/verifiers/target/missing-input-investigation-after-helper --verifiers.report-html-path=analysis-report.html --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=false --verifiers.scenario-catalog.max-catalog-scenarios=80 --verifiers.scenario-catalog.max-input-variants-per-saga=10 --verifiers.scenario-catalog.max-schedules-per-input-tuple=20"
```
