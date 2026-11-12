# Retro — 2.4.d — Execution

**App:** quizzes-full-2
**Session:** 2.4.d (Event Wiring)
**Date:** 2026-08-05

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/subscribe/ExecutionSubscribesActivateUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/subscribe/ExecutionSubscribesUpdateStudentName.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/subscribe/ExecutionSubscribesAnonymizeStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/subscribe/ExecutionSubscribesDeleteUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/handling/ExecutionEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/handling/handlers/ExecutionEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/eventProcessing/ExecutionEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/execution/ExecutionInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/Execution.java` (modified — `getEventSubscriptions()`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java` (appended — four ByEvent methods + shared saga-state guard)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/service/ExecutionService.java` (appended — four cached-student mutators)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended — three beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (checkbox + file-table amendment)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (rows 37, 38)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/implement-aggregate/session-d.md` | § Reads, § Produce (all subsections), § Update BeanConfigurationSagas, § ACTIVE guard | Partial | Complete on file shapes and the deletion-event decision table. Its T3 "reflects event" rule cannot be satisfied non-trivially for an event whose payload value is already the cached value — harness-log row 37. |
| `docs/concepts/events.md` | § Event Classes, § Publishing Events, § EventSubscription, § EventHandler, § Polling, § Canonical Wiring Snippet, § ByEvent sagaState guard | Partial | Wiring snippets transcribed directly. Silent on the version-counter skew between a DTO-seeded snapshot version and an event's stamped version — harness-log row 38. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak, § Spec-First Ordering, § T3 | Partial | T3 template and the "capture originalValue in `given:`" rule used as written. Its Fake rule and session-d.md's reflects rule collide on `ActivateUserEvent` (row 37). |
| `.claude/skills/_shared/conventions.md` | § Anchor to repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | The file-redirect form from row 35 worked; `MAVEN_EXIT` survived a full-suite run. |
| `.claude/skills/_shared/session-completion.md` | all | Yes | — |
| `AGENTS.md` | § Harness evolution | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- session-d.md § "Deletion events: `remove()` on the whole consumer vs. remove a sub-entity" decided `DeleteUserEvent` without ambiguity: the domain-model predicate for `USER_EXISTS (Execution)` is per-student, the execution stays valid without the student, so the sub-entity branch applies and `Execution` is never invalidated.
- The instruction to reuse an existing service mutator only when it performs *exactly* the mutation caught a real trap: `disenrollStudent` removes the same student but also publishes `DisenrollStudentFromCourseExecutionEvent`, whose spec trigger is the `DisenrollStudent` operation. A separate `removeDeletedStudent` mutator avoids emitting a spurious event on user deletion.
- Putting the `sagaState` guard in the ByEvent method rather than the shared service method was stated with its rationale, so the four ByEvent methods share one private `isInSaga` helper and the saga steps calling the same service methods are untouched.

### What was unclear or missing

- § "`{Aggregate}InterInvariantTest.groovy`" assumes every subscribed event produces an observable change in the cached field. It gives no fallback for an event whose payload value is already the cached value by construction. See row 37 — this should have been a halt, and was not.
- Neither session-d.md nor events.md says whether a ByEvent mutator should advance the cached publisher version. Without it the same event is redelivered on every poll and each redelivery bumps the consumer's aggregate version, so it is effectively mandatory; only the version-only snippet in § "`Update{Publisher}Event` for consumers that cache only `{publisher}Version`" hints at it.

### Suggested wording / structure changes

- session-d.md § "`{Aggregate}InterInvariantTest.groovy`": add a clause for events whose payload re-affirms the current cached value, naming the cached publisher version as the observable delta and requiring the payload assertion alongside it.
- session-d.md § "\"ByEvent\" methods in Functionalities": state that a ByEvent mutator advances the cached publisher version to `event.getPublisherAggregateVersion()`, with the redelivery loop as the reason.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-d.md` | T3 "reflects event" rule has no case for an event whose payload equals the pre-event cached value | High | Add the re-affirming-payload clause; see row 37 |
| `docs/concepts/events.md` | Snapshot version vs. event version come from the same counter but are stamped at different moments, so pre-snapshot events are delivered on first poll | High | Document the backlog in § EventSubscription and its consequence for T3 fixtures; see row 38 |
| `.claude/skills/implement-aggregate/session-d.md` | Silent on advancing the cached publisher version in ByEvent mutators | Medium | State it in § "\"ByEvent\" methods in Functionalities" |

---

## Patterns to Capture

- **Pattern:** Quiesce the fixture's own event backlog before a T3 "ignores unrelated" assertion
  **Observed in:** `applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/execution/ExecutionInterInvariantTest.groovy` (`drainActivationBacklog`)
  **Description:** When a fixture helper itself triggers a publishing operation (here `createActiveUser` calls `activateUser`), that event outranks the snapshot version a later link step seeds and is still pending when the test starts. Poll once in `given:` before capturing the before-value, or the assertion cannot separate the fixture's event from the one the test fires. Applies to any consumer whose fixture path publishes a subscribed event type.

- **Pattern:** Absent-member early return in an event-driven mutator
  **Observed in:** `applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/service/ExecutionService.java` (`findStudent` + null check)
  **Description:** A per-member-anchored event still reaches the whole consumer aggregate. Returning before `registerChanged` when the member is absent avoids a version bump with no state change — the collection-member analogue of session-d.md's shared-anchor service-layer filter.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 37, 38

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 37 | 2 | deferred | - |
| 38 | 2 | deferred | - |

---

## One-Line Summary

The first session-`d` of this application landed green, but both of its friction points are open Type 2 questions about event versioning — session-d.md's T3 "reflects" rule has no case for a payload that re-affirms the cached value, and events.md never says that events published before a snapshot was seeded are delivered on the consumer's first poll.
