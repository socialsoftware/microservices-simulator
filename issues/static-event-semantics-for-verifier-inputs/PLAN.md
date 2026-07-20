# Static Event Semantics for Verifier Inputs — Implementation Plan

This is an implementation-ready plan. It assumes no conversation context.

## Existing Pipeline To Preserve

Current static pipeline in `ScenarioGeneratorApplication`:

```text
CommandHandlerIndexVisitor
ServiceVisitor
CommandHandlerVisitor
WorkflowFunctionalityVisitor
WorkflowFunctionalityCreationSiteVisitor
GroovySourceIndex
GroovyConstructorInputTraceVisitor
ApplicationAnalysisScenarioModelAdapter
ScenarioGenerator
```

Add event semantics after `WorkflowFunctionalityCreationSiteVisitor` and before `GroovyConstructorInputTraceVisitor`.

Do not change final `ScenarioGenerator` responsibilities.

## New Production-Side Model

Add a state building block similar to:

```text
EventDrivenFunctionalityInvocation
```

Fields should include at least:

```text
eventHandlingClassFqn
eventHandlingMethodName
eventTypeFqn
eventHandlerClassFqn
eventProcessingClassFqn
eventProcessingMethodName
facadeClassFqn
facadeMethodName
sagaClassFqn
argumentSources/resolution notes
```

Add to `ApplicationAnalysisState`:

```java
public final List<EventDrivenFunctionalityInvocation> eventDrivenFunctionalityInvocations = new ArrayList<>();
```

Report these in human/HTML reports for debugging.

## New Visitor

Add a Java visitor, likely:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/EventHandlingBridgeVisitor.java
```

Responsibilities:

1. Find `EventHandling` classes/methods with calls shaped like:

   ```java
   eventApplicationService.handleSubscribedEvent(EventType.class, new SomeEventHandler(...));
   ```

2. Extract:

   ```text
   event handling class/method
   event type
   event handler class
   ```

3. Resolve the event handler class to the event processing method it calls.

4. Resolve the event processing method to functionality/facade calls such as:

   ```java
   tournamentFunctionalities.updateStudentName(...)
   quizAnswerFunctionalities.removeQuizAnswer(...)
   ```

5. Join that facade class/method against existing `state.sagaCreationSites` to get the saga FQN.

6. Emit one `EventDrivenFunctionalityInvocation` per resolved event-driven facade invocation.

Keep the visitor conservative. If a link cannot be resolved, emit diagnostics rather than guessing.

## Groovy Visitor Integration

Extend `GroovyConstructorInputTraceVisitor` so that when it sees a Groovy method call whose receiver type and method match an `EventDrivenFunctionalityInvocation`, it emits a `GroovyFullTraceResult` for the bridge's `sagaClassFqn`.

The trace must be visibly event-origin. Add/extend origin metadata if needed, e.g.:

```text
EVENT_HANDLER_CALL
```

Resolution notes should include:

```text
resolved via event handler <EventHandlingClass>.<method>()
event type <EventType>
processing <EventProcessingClass>.<method>(...)
facade <FunctionalitiesClass>.<method>(...)
saga <SagaFqn>
```

## Initial Argument Strategy

First slice may use conservative event placeholders instead of full materialization.

Examples:

```text
EVENT_SUBSCRIBER_AGGREGATE_ID
EVENT_FIELD:UpdateStudentNameEvent.publisherAggregateId
EVENT_FIELD:UpdateStudentNameEvent.publisherAggregateVersion
EVENT_FIELD:UpdateStudentNameEvent.studentAggregateId
EVENT_FIELD:UpdateStudentNameEvent.updatedName
```

It is acceptable if these inputs are accepted but not executor-ready. Do not label them replayable unless they are actually materializable.

## Dummyapp Fixture First

Add a minimal dummyapp event path:

```text
DummyEventHandling.handleItemRenamedEvents()
  -> handleSubscribedEvent(ItemRenamedEvent.class, new ItemRenamedEventHandler(...))
  -> ItemEventProcessing.processItemRenamedEvent(...)
  -> ItemFunctionalitiesFacade.renameItemFromEvent(...)
  -> RenameItemFromEventFunctionalitySagas
```

Add/extend focused Spock coverage to assert:

1. Java visitor extracts the event bridge.
2. Groovy visitor emits an event-origin trace for the downstream saga.
3. Scenario adapter produces an `InputVariant`.

## Quizzes Expectations

The implementation should be generic, but the important Quizzes target group is:

```text
answer.RemoveQuizAnswerFunctionalitySagas
answer.UpdateUserNameInQuizAnswerFunctionalitySagas
execution.RemoveUserFromCourseExecutionFunctionalitySagas
tournament.AnonymizeUserTournamentFunctionalitySagas
tournament.UpdateUserNameFunctionalitySagas
```

If any remain missing after implementation, there must be a clear diagnostic explaining which static link was unresolved.

## Constraints

- Use FQNs for saga identity.
- Preserve deterministic ordering and IDs.
- Do not use dynamic evidence as a source of static inputs.
- Do not add Quizzes-specific mappings.
- Do not refactor unrelated verifier pipeline code.
- Keep event semantics additive and explicit.
