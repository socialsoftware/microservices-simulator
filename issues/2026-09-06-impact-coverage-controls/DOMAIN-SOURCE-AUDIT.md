# Domain-case source audit

This audit checks `DOMAIN-CASES.md` against the current Quizzes production source and
the retained ImpactV2 qualification reports. It verifies factual causation and final
state only. It does not recommend a scoring-policy change.

## Verdict

The Course-count, Question-creation, Tournament/Quiz, Tournament-topic and delivered-event
examples are accurate. The deleted QuizAnswer example is also accurate as a historical
unhealthy control. One sentence should be phrased more carefully: `RemoveTournament`
declares no explicit step compensation in production source, so say that the report ends
with an active Tournament outside the Saga, rather than attributing that result to a
hand-written Tournament-status release.

## Course count remains reduced after failed removal

- The Saga reads the execution at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/coordination/sagas/RemoveCourseExecutionFunctionalitySagas.java:37-43`.
- Its only explicit compensation resets that execution's semantic lock at the same file,
  lines `45-50`.
- The next step sends a decrement (`increment=false`) at lines `52-55`; the removal is a
  later dependent step at lines `57-60`. There is no source compensation that increments
  the count.
- `CourseService.decrementCourseExecutionCount` copies the Course, subtracts one (bounded
  at zero), and registers the changed Course at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/course/service/CourseService.java:123-129`.
- Actual execution deletion would copy, mark, register, and emit the deletion event at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/service/ExecutionService.java:86-95`.

The old broader fault report
`verifiers/target/impact-v2-broader/run-01/broader/attempts/RemoveCourseExecution-basic-late-fault/execution.impact-v2.json`
is `COMPENSATED / EXACT / COMPLETE`, score 1. `SagaCourse(1)` changes from version 3,
`courseExecutionCount=1`, to version 5, count 0, and is the sole
`FAILED_OPERATION_RESIDUAL` finding. `SagaExecution(2)` remains active at version 2. The
paired control report is `SUCCESS / EXACT / COMPLETE`, score 0; its Course also reaches
count 0, while its execution reaches `DELETED`. Thus “the offering remains” describes
the fault run precisely, and the comparison is failed residual 1 versus successful
control 0.

## Question remains after failed creation

- Course and Topic reads have explicit semantic-lock release compensations at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/question/coordination/sagas/CreateQuestionFunctionalitySagas.java:49-89`.
- Question creation is the next step at lines `91-95`; the Course question-count update
  is later and depends on creation at lines `97-101`. No compensation is registered for
  the create step.
- The create service allocates an aggregate ID, constructs the Question, registers it as
  changed, and emits `CreateQuestionEvent` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/question/service/QuestionService.java:55-68`.

The old broader fault report
`verifiers/target/impact-v2-broader/run-01/broader/attempts/CreateQuestion-basic-late-fault/execution.impact-v2.json`
is `COMPENSATED / EXACT / COMPLETE`, score 1. Its baseline has no `SagaQuestion(4)`;
the final state contains active version 5, and that identity is the sole residual finding.
The Course remains at question count 0. The paired successful control scores 0 and ends
with the new Question plus Course question count 1. The note's distinction between a
successful creation and a surviving object from a failed creation is correct.

## Failed Tournament removal affects Tournament and Quiz

- The read marks the Tournament `IN_DELETE_TOURNAMENT` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/RemoveTournamentFunctionalitySagas.java:37-44`.
- Quiz removal precedes Tournament removal at lines `46-54`. The workflow registers no
  explicit compensation in this class.
- Quiz deletion is a real persisted change at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java:205-210`.
- An active Tournament declares the Quiz dependency through
  `Tournament.interInvariantQuizExists` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java:153-164` and `198-200`.

The old broader late-fault report
`verifiers/target/impact-v2-broader/run-01/broader/attempts/RemoveTournament-basic-late-fault/execution.impact-v2.json`
is `COMPENSATED / EXACT / COMPLETE`, score 2. Final `SagaQuiz(9)` is deleted at version
20; final `SagaTournament(10)` remains active at version 19 and still declares that Quiz
dependency. Findings are one `DELETED_DEPENDENCY` on the Tournament and one
`FAILED_OPERATION_RESIDUAL` on the Quiz. The successful control scores 0 and ends with
both identities deleted. The two-point explanation and union-of-identities wording are
accurate. For source precision, replace “releasing the Tournament status” with “the
observed compensated run leaves the Tournament active and outside the Saga.”

## Tournament topic course IDs are lost during restoration

- `TournamentTopic(TopicDto)` copies `TopicDto.courseId` into
  `topicCourseAggregateId` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/TournamentTopic.java:26-31`.
- `TournamentTopic.buildDto()` copies aggregate ID, version, name and state, but omits
  course ID at the same file, lines `99-105`.
- `TournamentDto(Tournament)` obtains every topic through that lossy `buildDto()` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/TournamentDto.java:30-42`.
- Update compensation sends `originalTournamentDto.getTopics()` back through
  `UpdateTournamentCommand` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java:68-76`.
- `TournamentService.updateTournament` rebuilds embedded topics from those DTOs and
  registers the new Tournament at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/service/TournamentService.java:84-110`.
- Replacing topics updates `lastModifiedTime` at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java:492-503`.

The old M2 report
`verifiers/target/impact-v2-qualification/run-03/residual-assigned-normal.execution.impact-v2.json`
is `COMPENSATED / COMPLETE`, score 1 on `SagaTournament(11)`. Start/end dates, question
count, topic IDs/names/versions/states are restored. Both topics' course IDs change from
1 to null, and `lastModifiedTime` changes. The note is correct that this is not a
timestamp-only residual.

## Delivered Question update is not persisted in Quiz

- Event processing forwards `UpdateQuestionEvent` to the Quiz functionality at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/coordination/eventProcessing/QuizEventProcessing.java:23-26`.
- The Saga sends the Quiz update command at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/coordination/sagas/UpdateQuestionInQuizFunctionalitySagas.java:24-32`.
- `QuizService.updateQuestion` loads and copies the Quiz, mutates the embedded question's
  title/content/version, but never calls `unitOfWorkService.registerChanged(newQuiz, ...)`
  at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java:177-189`.
- The controlled repair is exactly one added `registerChanged` after the mutations:
  `verifiers/experiments/impact-v2/patches/quiz-update-register-changed.patch`.

The old current-build report
`verifiers/target/impact-v2-qualification/run-03/event-current.execution.impact-v2.json`
is `SUCCESS / COMPLETE`, score 1. Event 4 is delivered to active `SagaQuiz(10)`; receiver
version is 19 before, after and at the horizon; eligibility remains true; stored question
data is unchanged. The repaired report
`verifiers/target/impact-v2-qualification/run-03/event-repaired.execution.impact-v2.json`
scores 0, advances the Quiz from version 19 to 23, and makes it ineligible for that exact
event. “Missing persist” should be presented technically as “missing UnitOfWork changed
registration”; JPA `persist()` is not the API this application uses here.

## Deleted QuizAnswer is a historical unhealthy control

- `SolveQuiz` creates/registers the QuizAnswer first at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/SolveQuizFunctionalitySagas.java:92-105` and
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/answer/service/QuizAnswerService.java:64-88`.
- The later Tournament step runs at `SolveQuizFunctionalitySagas.java:107-110`.
- Production validation throws if the user is not a Tournament participant at
  `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/service/TournamentService.java:158-167`.
- Compensation removes the created answer at `SolveQuizFunctionalitySagas.java:100-105`;
  removal persists a deleted revision at `QuizAnswerService.java:114-120`.

The old broader control report
`verifiers/target/impact-v2-broader/run-01/broader/attempts/SolveQuiz-basic-control/execution.impact-v2.json`
is itself `COMPENSATED / COMPLETE`, score 1: no answer exists at baseline and final
`SagaQuizAnswer(13)` is deleted at version 25. The note correctly labels this an unhealthy
control rather than injected-fault uplift. Whether that deleted creation remnant should
count remains a deferred policy question.

## Fresh RemoveCourseExecution event pair: equal scores, different conditions

The refreshed event fixture is deliberately richer than the basic Course example. Its
source-derived setup creates `SagaExecution(2)`, student 3, `SagaQuiz(6)` for that
execution, and `SagaQuizAnswer(7)`, then creates a second `SagaExecution(8)` for the same
Course and disenrolls student 3 from execution 2. The exact setup is
`verifiers/target/impact-coverage-controls/generated-final/quizzes-20260906-133803-203/setups.jsonl`,
entry `setup-654acaf9d57124996c605c08`, actions 1-10. The second offering keeps the Course
count above zero when it still has a Question, satisfying
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/course/aggregate/Course.java:70-74`.
The disenrollment leaves execution 2 empty so deletion satisfies
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/aggregate/Execution.java:122-137`.

In the successful control, all three forward steps complete: the Course count changes
from 2 to 1, execution 2 changes from active version 15 to deleted version 18, and the
removal publishes `DeleteCourseExecutionEvent` as defined at
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/service/ExecutionService.java:86-95`.
The schedule contains exactly one event consequence. It selects the QuizAnswer route,
`QuizAnswerEventHandling.handleDeleteCourseExecutionEvents`, whose handler forwards to
`QuizAnswerEventProcessing.processDeleteCourseExecutionEvent` at
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/answer/notification/handling/QuizAnswerEventHandling.java:31-38` and
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/answer/coordination/eventProcessing/QuizAnswerEventProcessing.java:36-39`.
That selected delivery succeeds and deletes `SagaQuizAnswer(7)`, version 12 to 20.

The same event also has another source-supported receiver that is still eligible at the
horizon: active `SagaQuiz(6)`.
The Quiz still declares `QuizSubscribesDeleteCourseExecution` for execution 2, subscribed
version 6, at
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/aggregate/Quiz.java:169-180`.
That subscription matches the event's publisher ID 2 and publisher version 19 under the
generic exact predicate at
`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate/EventSubscription.java:29-33`.
If its own route ran, Quiz handling would call `QuizService.removeCourseExecution`, which
marks the matching Quiz inactive and registers the change at
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/notification/handling/QuizEventHandling.java:26-33`,
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/coordination/eventProcessing/QuizEventProcessing.java:18-21`, and
`applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java:163-174`.
That route is not in this persisted schedule, and the executor does not perform an extra
queue drain after the selected consequence. Therefore the precise statement is that an
available, still-eligible Quiz handler route was unselected and remains outside the
measured horizon. The evidence does not establish that propagation is still queued or
that a later runtime will deliver it, so it cannot be claimed that merely waiting would
repair the dependency.

The fresh control report
`verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveCourseExecution-events-control/execution.impact-v2.json`
is `SUCCESS / EXACT / COMPLETE`, score 1. Its one finding is
`DELETED_DEPENDENCY`: active, unchanged `SagaQuiz(6)` still depends on deleted
`SagaExecution(2)`. The selected QuizAnswer receiver is not unresolved because it changes
and becomes deleted/ineligible. The Course's expected count reduction is not a failed-Saga
residual because the control succeeds.

The paired late-fault report
`verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveCourseExecution-events-late-fault/execution.impact-v2.json`
is `COMPENSATED / EXACT / COMPLETE`, also score 1, but for a different identity and
category. The assigned fault masks the removal step and its event consequence, leaving
execution 2, Quiz 6, and QuizAnswer 7 active; no target is deleted, so there is no deleted
dependency. The already committed Course decrement from 2 to 1 remains after recovery,
making `SagaCourse(1)` the sole `FAILED_OPERATION_RESIDUAL`.

Thus `1 → 1` is not evidence that the fault and control have the same effect. The control's
one point is an active Quiz whose separate deletion-event route was outside this bounded
schedule. The fault's one point is the Course mutation left by a failed removal. The score
counts affected identities in the union of categories; it does not encode category,
causal path, or severity. This fresh pair is a useful example of why every score should
be shown with its category and object, and why a nonzero control must not be presented as
a clean fault-uplift comparison.
