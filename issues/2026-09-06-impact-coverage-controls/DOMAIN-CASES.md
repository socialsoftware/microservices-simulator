# Concrete impact cases for Tuesday's discussion

Scoring policy is unchanged. This note separates what the current metric counts from
the judgment we will discuss later. The Course, Question, Tournament-removal and deleted
offering examples below use the refreshed ordinary executor campaign. The update and
event-consumer variants retain their earlier controlled-experiment provenance.

## What one point means

One point means one distinct aggregate identity matched at least one of the three
implemented potential-impact checks. An aggregate is the application's consistency unit:
for example, one Course, Question, Quiz or Tournament. Several changed fields in the
same Tournament still count as one object. Two objects implicated in the same incident
can count as two. This is not a severity scale or a count of failed commands.

The three checks ask:

1. Does an active final object still declare a dependency on another object that became
   deleted during this attempt and remains deleted?
2. Did a failed Saga finish its available recovery yet leave an object different from
   before, where it was the only observed writer of that object?
3. Did an explicitly scheduled event delivery succeed, leave its receiver unchanged,
   and leave that receiver eligible for the same event at the final horizon?

The observer records the evidence; deterministic checks evaluate it. A missing observation
is not zero. A complete score only covers these checks, not every possible application
problem. No LLM judges whether a state is harmful.

## A Course loses a count, but its offering remains

**User story:** an administrator removes one offering of a Course. A Course represents
the subject; a CourseExecution represents one offering and is a separate aggregate.

`RemoveCourseExecution` reads the offering, decrements the Course's stored offering count,
and then removes the offering. Injecting a fault before removal leaves the count update
already committed. Recovery can release the offering's Saga status, but the source has
no compensation that increments the Course count again.

Source: [removal workflow](../../applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/coordination/sagas/RemoveCourseExecutionFunctionalitySagas.java#L37).

The fresh experiment observed Course.courseExecutionCount changing from 1 to 0 while
the offering was not removed. The Course matched the failed-operation-residual check:
**one Course, one point**. The successful control scored zero. The meaningful fact is
that a failed removal left the Course's application data changed; the detector did not
need a handwritten rule equating the count to a query over offerings.

Fresh evidence: [control, score 0](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveCourseExecution-basic-control/execution.impact-v2.json)
and [fault, score 1](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveCourseExecution-basic-late-fault/execution.impact-v2.json).

What we should discuss: the generic residual is evidence of a lasting failed-operation
effect. The domain explanation makes its significance clear. The metric does not prove
that every lasting effect of every failed operation is undesirable.

## A Question survives a failed creation

**User story:** a teacher adds a Question to the question bank. Creation also updates the
Course's question count.

`CreateQuestion` reads its Course and Topics, creates the Question, and then increments
the Course question count. A fault before that final update occurs after the Question
already exists. The source registers recovery for the earlier reads/statuses, but no
compensation that removes the created Question.

Source: [creation and subsequent count update](../../applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/question/coordination/sagas/CreateQuestionFunctionalitySagas.java#L91).

The fresh fault run left a new active Question absent from the baseline. The failed
Saga was its sole observed writer: **one Question, one point**. Successful creation
also leaves a new Question, but scores zero under this check because its Saga committed.
Thus “an object changed” alone is insufficient: the failed-operation context matters.

Fresh evidence: [control, score 0](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/CreateQuestion-basic-control/execution.impact-v2.json)
and [fault, score 1](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/CreateQuestion-basic-late-fault/execution.impact-v2.json).

## One failed removal counts two objects

**User story:** remove a Tournament. Its Quiz is a separate aggregate containing the
selected questions; the Tournament stores a dependency on that Quiz.

`RemoveTournament` reads/tags the Tournament, deletes the Quiz, then deletes the
Tournament. A fault before the last step can leave the Tournament active and its Quiz
deleted. This class declares no explicit step compensation. The framework can undo the
Tournament's semantic lock; that rollback does not recreate the deleted Quiz.

Source: [removal workflow](../../applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/RemoveTournamentFunctionalitySagas.java#L37).

The current rules can count **two objects from this one incident**:

- the active Tournament, for its surviving dependency on the deleted Quiz;
- the Quiz, because its deletion remains after the failed Saga's recovery.

A finding in each category does not automatically mean two points: the score takes the
union of affected identities. This case is two because the identities are different.
This is the strongest simple example for explaining why the number measures affected
objects rather than incidents, exceptions or severity.

Fresh evidence: [control, score 0](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveTournament-basic-control/execution.impact-v2.json)
and [fault, score 2](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveTournament-basic-late-fault/execution.impact-v2.json).

## An update is mostly restored and still scores one

**User story:** change a Tournament's dates, topics and question count. If its Quiz cannot
be updated afterward, compensation attempts to restore the Tournament settings.

The earlier focused experiment found that normal compensation restored the main settings
but lost the embedded Topics' course IDs and changed the Tournament's lastModifiedTime.
`TournamentTopic.buildDto()` omits the course ID used when rebuilding those embedded
Topics. The Tournament therefore remained different from its baseline and scored one.
A controlled no-op-compensation build left many more settings changed, but also scored
one: it was still the same affected Tournament.

This is not a timestamp-only example: the measured normal-compensation case also lost
course-reference data. It would be incorrect to claim that its point came only from a
clock field. The separate timestamp-only policy question remains open.

Source: [topic DTO conversion](../../applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/TournamentTopic.java#L99).
Retained controlled reports: [normal compensation](../../verifiers/target/impact-v2-qualification/run-03/residual-assigned-normal.execution.impact-v2.json)
and [experiment provenance](../2026-09-06-potential-impact-v2/M2-HANDOFF.md).

## Equal scores, different conditions and a bounded stopping point

The refreshed **RemoveCourseExecution-events** pair scores **1 in both runs**:

| Run | What happens | Object counted |
| --- | --- | --- |
| No injected fault | The offering is removed. The selected deletion-event handler removes its QuizAnswer. Its Quiz remains active and depends on the deleted offering. | Quiz: deleted dependency |
| Fault before offering removal | The offering remains active. The Course count has already fallen from 2 to 1 and is not restored. No removal event is delivered. | Course: failed-operation residual |

This fixture has two offerings so normal removal satisfies the Course's prerequisite.
The count is two before the scenario, unlike the basic one-offering example above.

There is a separate eligible Quiz handler for the deletion event. It is **not selected
in this schedule**; its source would mark the matching Quiz inactive. The evidence does
not show that this handler is still queued or will eventually run. Our final state is
the state after the chosen scenario actions, not a promise that every possible event
consumer has finished. No extra handler was delivered to force the score down.

This leaves two concrete discussion points: one point does not identify its cause, and
a positive control can reflect the chosen stopping point. The score alone cannot prove
that injecting this fault made things worse, or that the observed dependency will persist
after later application work.

Fresh evidence: [control](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveCourseExecution-events-control/execution.impact-v2.json)
and [fault](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/RemoveCourseExecution-events-late-fault/execution.impact-v2.json).
Exact handler/subscription source is recorded in [the source audit](DOMAIN-SOURCE-AUDIT.md).

## A delivered update does not reach the Quiz's stored copy

**User story:** update a Question and propagate that change to a Quiz holding copied
Question data through an event.

The earlier focused event experiment delivered the selected event successfully, yet
the stored Quiz remained unchanged and eligible for that exact event afterward. The
Quiz matched the unresolved-delivered-event check: **one Quiz, one point**. A temporary
repaired consumer persisted the change and scored zero in the controlled comparison.

Delivery success alone did not establish successful propagation. Conversely, this check
only examines a delivery explicitly included in the scenario. It does not invent extra
events or drain every queue after execution. The consumer repair was an experimental
variant, not a production fix in the current work.

The concrete missing call is `unitOfWorkService.registerChanged(newQuiz, ...)`:
the handler changes a copy in memory without registering that copy for storage.
Source: [Quiz update method](../../applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java#L177).
Retained controlled reports: [current consumer](../../verifiers/target/impact-v2-qualification/run-03/event-current.execution.impact-v2.json)
and [temporary repaired consumer](../../verifiers/target/impact-v2-qualification/run-03/event-repaired.execution.impact-v2.json).

## A deleted creation record: the deferred policy question

The refreshed **CreateCourseExecution** pair demonstrates this with a healthy control.
Successful creation of a second offering scores zero. In the late-fault run, creation
has already stored the new offering; compensation marks it deleted. The Course's count
and existing offering are unchanged from baseline, but the new deleted `SagaExecution(3)`
record remains. Current rules score **one residual object**. This is a concrete example
where the storage-level difference is proved while user-visible harm remains undecided.

Fresh reports: [successful creation](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/CreateCourseExecution-basic-control/execution.impact-v2.json)
and [failed creation with deleted remnant](../../verifiers/target/impact-coverage-controls/run-01/broader/attempts/CreateCourseExecution-basic-late-fault/execution.impact-v2.json).

The earlier SolveQuiz controls used a user who was not enrolled in the Tournament.
Solving created a QuizAnswer attempt, later failed, and recovery marked that new attempt
deleted. The baseline had no QuizAnswer; the final database retained a deleted record.
Current lifecycle comparison treats those as different and counted one residual object.
The control and injected-fault run both scored one, so that pair did not demonstrate an
increase caused by the injected fault.

The refreshed SolveQuiz and SolveQuizAsync controls now succeed and score zero; their
late-fault runs score one on a new deleted QuizAnswer. We preserve the earlier unhealthy
control for comparison. We have not changed whether that deleted record counts.
Questions for later: does a fully deleted creation remnant deserve a headline point?
Does an application timestamp alone deserve one? Keep the raw facts available whatever
we decide; neither decision is needed to fix observation or execution coverage.

Retained historical evidence: [unhealthy SolveQuiz control](../../verifiers/target/impact-v2-broader/run-01/broader/attempts/SolveQuiz-basic-control/execution.impact-v2.json).
The detailed source audit is [here](DOMAIN-SOURCE-AUDIT.md).

## Suggested meeting sequence

Start with Course removal to explain updates, then Tournament removal to explain the
object count, and then the event example to explain propagation. Use the mostly-restored
update and deleted creation record to expose the metric's limits. For every example,
show the control beside the fault, the actual recovery actions, the final persisted facts,
and the exact finding. Keep past controlled variants distinct from fresh source-generated
runs and distinguish an ordinary application failure from an injected fault.
