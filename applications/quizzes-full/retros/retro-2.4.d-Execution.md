# Retro — 2.4.d — Execution

**App:** quizzes-full
**Session:** 2.4.d (Event Wiring)
**Date:** 2026-05-03

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/notification/subscribe/ExecutionSubscribesDeleteUser.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/notification/subscribe/ExecutionSubscribesUpdateStudentName.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/notification/subscribe/ExecutionSubscribesAnonymizeStudent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/notification/handling/ExecutionEventHandling.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/notification/handling/handlers/ExecutionEventHandler.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/eventProcessing/ExecutionEventProcessing.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/execution/ExecutionInterInvariantTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/Execution.java` (modified: implemented `getEventSubscriptions()`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java` (modified: added `removeStudentFromExecutionByEvent`, `updateStudentNameByEvent`, `anonymizeStudentByEvent`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/service/ExecutionService.java` (modified: added `removeStudentFromExecutionByEvent`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified: added `ExecutionEventHandling`, `ExecutionEventHandler`, `ExecutionEventProcessing` beans)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | EventSubscription structure, EventHandler, polling pattern, EventProcessing | Partial | EventSubscription constructor takes `String eventType`, not `Class<? extends Event>` — the example shows a `Class` arg but the actual constructor takes `String`. Had to inspect the simulator source directly. |
| `docs/concepts/testing.md` | T3 section (Inter-Invariant Tests) | Yes | Template was clear. |

---

## Skill Instructions Feedback

### What worked well

- The plan.md section was clear on what events are subscribed and what files to produce.
- The T3 test template from testing.md mapped cleanly to the three event types.
- Using `userService` directly in T3 tests (bypassing the Execution saga) cleanly isolates the event-handling path from the saga update path.

### What was unclear or missing

- The `EventSubscription` constructor signature: docs show `super(ref.getAnchorAggregateId(), ref.getAnchorVersion(), CreateQuestionEvent.class)` passing a `Class`, but the actual constructor in the simulator takes `(Integer, Long, String)`. Had to inspect `EventSubscription.java` directly.
- The session-d.md template for `{Aggregate}EventHandler` describes it as "extends the {Aggregate}EventHandler base class" and "concrete subclass per event type", but also says the file IS `{Aggregate}EventHandler.java`. This is self-referential and ambiguous. Resolved by making `ExecutionEventHandler` a concrete `@Component` that dispatches all event types in a single `handleEvent` method.
- The BeanConfig template shows `new {Aggregate}EventHandler()` with no args, but `EventHandler` requires a `JpaRepository` in its constructor. Adapted to `new ExecutionEventHandler(executionRepository)`.
- `ExecutionStudent` has no version field tracking the user's current version, so `subscribedVersion = 0L` was used for all subscriptions. This is functionally correct but means all events from that user since the beginning are eligible.
- The existing Execution saga methods (`updateStudentName`, `anonymizeStudent`) would cause circular event loops if called from EventProcessing. Required adding new event-specific methods (`updateStudentNameByEvent`, `anonymizeStudentByEvent`, `removeStudentFromExecutionByEvent`) to `ExecutionFunctionalities` and `ExecutionService`.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-d.md`: Update the `EventSubscription` constructor example to show `String` eventType (`DeleteUserEvent.class.getSimpleName()`) rather than `Class` object. Add a note about the version field pattern when the subscriber entity has no version tracking.
- `.claude/skills/implement-aggregate/session-d.md`: Clarify that `{Aggregate}EventHandler.java` is a single concrete class (not abstract base + subclasses) when all event types can share a common dispatch pattern. The current description implies multiple files but plan.md only lists one.
- `docs/concepts/events.md`: Add a note explaining that `EventSubscription` constructor takes `String eventType` (via `EventClass.class.getSimpleName()`), not a `Class` object. The canonical wiring snippet shows a `Class` which is misleading.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/events.md` | EventSubscription constructor shows `Class` arg but actual signature takes `String` | High | Update canonical snippet to use `DeleteUserEvent.class.getSimpleName()` |
| `docs/concepts/events.md` | No guidance on what to use when the subscriber entity (e.g., `ExecutionStudent`) doesn't track the publisher's version | Medium | Add a note: use `0L` as `subscribedVersion` in this case; explain the implication (all events since creation are eligible) |
| `.claude/skills/implement-aggregate/session-d.md` | BeanConfig template uses no-arg `new {Aggregate}EventHandler()` but EventHandler requires a repository | Medium | Show `new ExecutionEventHandler(executionRepository)` pattern and note that `ExecutionRepository` is provided by `@DataJpaTest` auto-configuration |
| `.claude/skills/implement-aggregate/session-d.md` | EventProcessing is described as delegating to Functionalities, but existing saga Functionalities methods cause loops for cached-field events | High | Add explicit guidance: for events that mirror saga operations (e.g., name update, anonymize), add NEW non-saga "ByEvent" methods to Functionalities that call service directly without triggering another saga |

---

## Patterns to Capture

- **Pattern:** Single-class EventHandler with instance dispatch
  **Observed in:** `ExecutionEventHandler.java`
  **Description:** Instead of separate handler classes per event type, a single `@Component` extending `EventHandler` receives all event types and dispatches via `instanceof` in `handleEvent`. Requires only one bean in BeanConfig and simplifies EventHandling (one `@Autowired` field instead of multiple).

- **Pattern:** Non-saga event-update methods in Functionalities
  **Observed in:** `ExecutionFunctionalities.java` (`removeStudentFromExecutionByEvent`, `updateStudentNameByEvent`, `anonymizeStudentByEvent`)
  **Description:** When events mirror operations that also have saga Functionalities methods, the existing saga methods cannot be called from EventProcessing (they would trigger circular sagas). Add separate "ByEvent" methods that call the service directly with their own UoW lifecycle — no saga, no semantic lock, just a simple service call + commit.

- **Pattern:** T3 test trigger via direct service call (bypassing saga)
  **Observed in:** `ExecutionInterInvariantTest.groovy` (`UpdateStudentNameEvent`, `AnonymizeStudentEvent` tests)
  **Description:** To isolate event-handling logic from saga logic in T3 tests, trigger the publisher's event by calling the service method directly (`userService.updateUserName(...)`) rather than calling the consumer's saga functionality. This avoids the saga pre-updating the field before the event can be observed.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `docs/concepts/events.md` | Fix EventSubscription canonical snippet: use `EventClass.class.getSimpleName()` for eventType arg |
| High | `.claude/skills/implement-aggregate/session-d.md` | Add guidance on "ByEvent" non-saga Functionalities methods for event types that mirror saga operations |
| Medium | `docs/concepts/events.md` | Add note on `subscribedVersion = 0L` when subscriber entity doesn't track publisher version |
| Medium | `.claude/skills/implement-aggregate/session-d.md` | Fix BeanConfig template for EventHandler to pass repository as constructor arg |
| Low | `.claude/skills/implement-aggregate/session-d.md` | Clarify that `{Aggregate}EventHandler.java` is a single concrete dispatcher, not an abstract base |

---

## One-Line Summary

The key finding is that existing Execution saga methods cannot be called from EventProcessing (they would trigger circular sagas), requiring new "ByEvent" non-saga methods in Functionalities that call the service directly.
