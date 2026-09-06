# Experiment prerequisites handoff

State: **complete for source implementation and focused proof**. Package regeneration,
selector resolution to regenerated ids, preflight, and campaign execution remain owned by
the root campaign slice.

## Outcome

The current-package reader now restores readiness per persisted argument recipe instead
of copying the input record's global `materializable` value onto every argument. A source
input may therefore keep setup-bound unresolved identity arguments while independently
materializing a complete constructor argument. This repairs the observed
`CreateTopicFunctionalitySagas` argument 2 `TopicDto` and
`UpdateStudentNameFunctionalitySagas` argument 3 `UserDto` paths without changing either
Quizzes DTO.

Nested constructor arguments, fields, collections, properties, transforms, helper results,
and map entries now retain recursive readiness when read from the compact package.
Unsupported descendants remain blocked. The setup-aware evaluator still ignores only exact
persisted participant bindings; no global input is promoted to materializable.

Quizzes source fixtures now provide healthy, explicit workload sources:

- synchronous and asynchronous SolveQuiz fixtures enroll the selected solving user in the
  tournament during `setup()`, so setup-only source candidates contain the prerequisite;
- a direct FindQuiz feature reads `tournamentDto.quiz.aggregateId`, the actual identity
  returned by the setup-created Tournament;
- QuizAnswer event-route features create and retain an active answer for the exact selected
  user/course before AnonymizeStudent or RemoveStudentFromCourseExecution;
- the dedicated RemoveCourseExecution receiver fixture creates a second offering for the
  same course and removes the selected student in `setup()` before the measured removal.
  Its pending disenrollment event is a setup effect cleared by the existing executor
  baseline contract. The active QuizAnswer remains an eligible DeleteCourseExecution
  receiver.

The closed setup dispatcher adds only the exact two-Integer
`ExecutionFunctionalities.removeStudentFromCourseExecution` method needed by that prefix.

## Frozen campaign source selectors

Resolve regenerated workload/input ids by these exact source identities; do not fall back to
global workload-id ordering or an intentional negative feature.

```json
[
  {
    "case": "CreateCourseExecution-basic",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.CreateCourseExecutionTest",
    "method": "create course execution successfully",
    "call": "courseExecutionFunctionalities.createCourseExecution(new CourseExecutionDto([name:COURSE_EXECUTION_NAME, type:COURSE_EXECUTION_TYPE, acronym:(NEW_ + COURSE_EXECUTION_ACRONYM), academicTerm:COURSE_EXECUTION_ACADEMIC_TERM, ... ]))"
  },
  {
    "case": "FindQuiz-basic",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.RemoveTournamentTest",
    "method": "find the tournament quiz by its created identity",
    "call": "quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)"
  },
  {
    "case": "SolveQuiz-basic",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.AnonymizeStudentAndSolveQuizTest",
    "method": "sequential solve quiz and anonymize user",
    "call": "tournamentFunctionalities.solveQuiz(tournamentDto.aggregateId, userDto.getAggregateId())"
  },
  {
    "case": "SolveQuizAsync-basic",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.SolveQuizAsyncTest",
    "method": "solve quiz using async functionality",
    "call": "tournamentFunctionalities.solveQuizAsync(tournamentDto.getAggregateId(), userDto.getAggregateId())"
  },
  {
    "case": "AnonymizeStudent-events",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer.QuizAnswerEventHandlingTest",
    "method": "AnonymizeStudentEvent updates the student name in the quiz answer",
    "call": "courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())"
  },
  {
    "case": "RemoveCourseExecution-events",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer.RemoveCourseExecutionQuizAnswerReceiverTest",
    "method": "RemoveCourseExecution event route has an eligible quiz answer receiver",
    "call": "courseExecutionFunctionalities.removeCourseExecution(courseExecutionDto.aggregateId)"
  },
  {
    "case": "RemoveStudentFromCourseExecution-events",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer.QuizAnswerEventHandlingTest",
    "method": "RemoveStudentFromCourseExecution event route has an eligible quiz answer receiver",
    "call": "courseExecutionFunctionalities.removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)"
  },
  {
    "case": "UpdateStudentName-events",
    "testClass": "pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.AnonymizeStudentAndSolveQuizTest",
    "method": "UpdateStudentName event route has an eligible tournament receiver",
    "call": "courseExecutionFunctionalities.updateStudentName(courseExecutionDto.aggregateId, userDto.aggregateId, completeUserDto)"
  }
]
```

`CreateTopic-basic` and `UpdateStudentName-basic` retain their previous exact source
selections. Their persisted constructor recipes were already honest; the reader incorrectly
inherited global false readiness from separately setup-bound ids. `UpdateStudentName-events`
uses the explicit receiver-ready source above because the previous fault-test setup had no
QuizAnswer subscriber.

## Exact setup identities to inspect after regeneration

- `FindQuiz-basic`: the participant identity must be the `quiz.aggregateId` property of the
  exact `createTournament` result from `RemoveTournamentTest.setup`.
- `SolveQuiz-basic`: setup must contain the exact `addParticipant(tournamentDto.aggregateId,
  courseExecutionDto.aggregateId, userDto.aggregateId)` action now in
  `AnonymizeStudentAndSolveQuizTest.setup`; participant args use that Tournament and user.
- `SolveQuizAsync-basic`: setup must contain the equivalent action from
  `SolveQuizAsyncTest.setup` for `userDto`.
- `AnonymizeStudent-events`: setup-created QuizAnswer, CourseExecution, and `userDto` must be
  shared with the measured AnonymizeStudent call.
- `RemoveStudentFromCourseExecution-events`: the same setup-created answer/course/user must
  be shared with the measured removal.
- `UpdateStudentName-events`: setup-created Tournament, CourseExecution, and participating
  `userDto` must be shared with the measured update because the frozen event route is
  `TournamentEventHandling.handleUpdateStudentNameEvent`; argument 3 must be the
  feature-local `completeUserDto` whose `name` is `USER_NAME_3`.
- `RemoveCourseExecution-events`: setup must include, in source order, the ordinary
  QuizAnswer fixture, a second CourseExecution using `ACRONYM_1`, then
  `removeStudentFromCourseExecution(courseExecutionDto.aggregateId, userDto.aggregateId)`;
  the measured target is the original `courseExecutionDto.aggregateId`. The target action
  itself must not appear in setup.

## Changed files

- `verifiers/src/main/java/.../scenario/export/ScenarioCatalogPackageReader.java`
- `verifiers/src/test/groovy/.../scenario/export/CurrentExecutableArtifactContractSpec.groovy`
- `verifiers/src/test/groovy/.../executor/ScenarioDateMaterializationSpec.groovy`
- `applications/quizzes/src/test/java/.../executor/QuizzesSourceSetupActionDispatcher.java`
- `applications/quizzes/src/test/groovy/.../executor/QuizzesSourceSetupActionDispatcherTest.groovy`
- `applications/quizzes/src/test/groovy/.../coordination/answer/QuizAnswerEventHandlingTest.groovy`
- `applications/quizzes/src/test/groovy/.../coordination/answer/RemoveCourseExecutionQuizAnswerReceiverTest.groovy`
- `applications/quizzes/src/test/groovy/.../coordination/tournament/AnonymizeStudentAndSolveQuizTest.groovy`
- `applications/quizzes/src/test/groovy/.../coordination/tournament/SolveQuizAsyncTest.groovy`
- `applications/quizzes/src/test/groovy/.../coordination/tournament/RemoveTournamentTest.groovy`

No Quizzes production source, generated JSON, scoring, event replay, or schedule logic changed.

## Proof

- JDK 21 verifier focused suite:
  `mvn -Dtest=CurrentExecutableArtifactContractSpec,ScenarioDateMaterializationSpec test`
  passed **35/35**.
- JDK 21 integrated Quizzes Saga run after real observer test wiring: the source-control
  classes passed **13/14**; the sole failure was the first RemoveCourse receiver fixture,
  which exposed the existing `Execution.removedNoStudents()` precondition.
- After adding the source-backed disenrollment prefix and exact closed dispatcher method:
  `mvn -Ptest-sagas -Dtest=QuizAnswerEventHandlingTest,QuizzesSourceSetupActionDispatcherTest test`
  passed **7/7**.
- The first generated package proved that feature-body prefixes are correctly excluded from
  setup extraction. The RemoveCourse receiver was therefore moved to a dedicated class with
  both prerequisites in `setup()`. JDK 21
  `mvn -Ptest-sagas -Dtest=RemoveCourseExecutionQuizAnswerReceiverTest,QuizzesSourceSetupActionDispatcherTest test`
  passed **3/3**.
- The original 60-run campaign exposed `UpdateStudentName-events-control` as invalid with
  `SELECTED_SUBSCRIBER_NOT_FOUND`: its frozen `UpdateStudentNameFaultTest` source had only
  the four enrollment setup actions and no eligible subscriber. A first supplementary run
  proved that its frozen route 1 is the Tournament handler, so the temporary QuizAnswer
  route-0 fixture was removed. The final explicit source reuses the enrolled Tournament in
  `AnonymizeStudentAndSolveQuizTest` and supplies a complete feature-local `UserDto`.
  JDK 21 `mvn -Ptest-sagas -Dtest=AnonymizeStudentAndSolveQuizTest test` passed **2/2**.
- `SolveQuizAsyncTest` passed **2/2** in both Quizzes runs; the integrated run also passed the
  synchronous solve, direct FindQuiz, and healthy CreateCourseExecution features.
- `git diff --check` passed for the slice files before this handoff.

The initial Quizzes attempt failed at Spring context creation because inherited ImpactV2
instrumentation required a real `PersistentStateObserver` in sliced JPA contexts. Root added
that test-only bean to the Saga and Causal test configurations; the rerun reached application
behavior. This is integrated test wiring, not a prerequisite-slice production change.

## Known exclusions and findings

The slice does not fix the missing event-consumer save paths, topic restoration, Course count
restoration, failed Question creation cleanup, or any other Quizzes production defect. It does
not change whether deleted remnants or timestamps score. Event handlers are not inserted into
setup and no additional event is delivered. Regeneration must preserve unsuccessful selector
resolution or preflight attempts rather than silently choosing another input.
