# Static Event Semantics for Verifier Inputs — PRD

## Problem

The verifier currently creates saga input variants from direct Spock/Groovy saga construction and direct functionality/facade calls. It misses sagas invoked indirectly through application event handling.

In Quizzes, several runtime-tested sagas are still classified as having no accepted static input because tests call event handling methods such as:

```groovy
tournamentEventHandling.handleUpdateStudentNameEvent()
quizAnswerEventHandling.handleAnonymizeStudentEvents()
courseExecutionEventHandling.handleRemoveUserEvents()
```

The actual source path is static but currently invisible to the verifier:

```text
EventHandling method
  -> EventApplicationService.handleSubscribedEvent(...)
  -> EventHandler
  -> EventProcessing method
  -> Functionalities facade method
  -> Saga constructor
```

## Goal

Add static event semantics so event-handler calls in tests can produce explicit event-origin saga input traces and therefore `InputVariant`s.

## Target Behavior

When the verifier sees a Groovy test call like:

```groovy
tournamentEventHandling.handleUpdateStudentNameEvent()
```

and Java source statically maps that handler to:

```text
TournamentEventHandling.handleUpdateStudentNameEvent()
  consumes UpdateStudentNameEvent
  -> TournamentEventProcessing.processUpdateStudentNameEvent(...)
  -> TournamentFunctionalities.updateStudentName(...)
  -> UpdateUserNameFunctionalitySagas
```

then the verifier should emit an input trace for:

```text
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas
```

The trace must be marked as event-origin, not disguised as a direct facade call.

## Non-goals

- Do not use dynamic evidence to create static inputs directly.
- Do not add Quizzes-specific hardcoded mappings.
- Do not put source-code traversal into the final `ScenarioGenerator` combinator.
- Do not require full executor-ready event payload materialization in the first slice.
- Do not solve stream/gRPC/distributed event semantics in this slice.

## Important Distinctions

- **Event topology extraction**: finding `EventHandling -> EventProcessing -> Facade -> Saga`.
- **Event payload reconstruction**: mapping `event.getX()` values back to test source or upstream event publication.
- **Scenario generation**: combining already-extracted saga definitions and input variants.

This work should start with topology and conservative payload placeholders. Executor readiness can improve later.

## Quizzes Runtime-Evidence Target Group

The immediate runtime-evidence group from the audit is:

```text
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas
```

These should be used as realistic Quizzes targets, but implementation must be generic and dummyapp-first.
