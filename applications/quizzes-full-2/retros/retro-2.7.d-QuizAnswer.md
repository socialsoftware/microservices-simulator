# Retro — 2.7.d — QuizAnswer

**App:** quizzes-full-2
**Session:** 2.7.d (Event Wiring)
**Date:** 2026-08-08

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesUpdateStudentName.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesAnonymizeStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesDeleteUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesUpdateQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesDeleteCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesDisenrollStudentFromCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesInvalidateQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/handling/QuizAnswerEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/notification/handling/handlers/QuizAnswerEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/eventProcessing/QuizAnswerEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswer.java` (getEventSubscriptions)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/service/QuizAnswerService.java` (five ByEvent mutate helpers)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/functionalities/QuizAnswerFunctionalities.java` (seven ByEvent methods, isInSaga)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (three event-wiring beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quizanswer/QuizAnswerInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md`

### Application bug fixes (earlier-session files)

Committed with the `harness:` commit below, since they are the call-site half of the framework change:

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/notification/handling/handlers/ExecutionEventHandler.java` (2.4.d — added `aggregateType()`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/notification/handling/handlers/QuestionEventHandler.java` (2.5.d — added `aggregateType()`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/notification/handling/handlers/QuizEventHandler.java` (2.6.d — added `aggregateType()`)

---

> ## ⚠️ SIMULATOR FRAMEWORK CHANGES
>
> The following files belong to `simulator/` — the **shared core library** used by all applications. Changes here affect every consumer of the library and must be treated as framework patches, not application fixes. Each change below includes a root-cause explanation and a justification for why the fix belongs in the framework rather than in application code.

### `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate/EventHandler.java`

**What changed (diff summary):**
```diff
  public Set<Integer> getAggregateIds() {
     return aggregateRepository.findAll().stream()
+            .filter(aggregate -> aggregateType().isInstance(aggregate))
             .map(Aggregate::getAggregateId)
             .collect(Collectors.toSet());
  }

  public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
     return aggregateRepository.findAll().stream()
+            .filter(aggregate -> aggregateType().isInstance(aggregate))
             .filter(aggregate -> Objects.equals(aggregate.getAggregateId(), subscriberAggregateId))

+ protected abstract Class<? extends Aggregate> aggregateType();
  public abstract void handleEvent(Integer subscriberAggregateId, Event event);
```

**Root cause:** every `{Xxx}Repository` derives from `AggregateRepository`, which is declared
`JpaRepository<Aggregate, Integer>`, so `findAll()` returns every aggregate of every type.
`EventApplicationService.handleSubscribedEvent` feeds those ids straight to the consumer's
`handleEvent`, and the only downstream filter — the aggregate's own `getEventSubscriptions()` — does
not narrow by consumer type. Any event type with two consumers therefore delivered foreign aggregate
ids to both handlers, and the ByEvent service method cast them to its own aggregate class. Symptom:
5 `ClassCastException`s in `QuizAnswerInterInvariantTest` (`SagaExecution`/`SagaQuiz` cast to
`QuizAnswer`).

**Fix rationale:** the id set a handler iterates is framework-owned; every consumer in every
generated application needs the same narrowing, and the type is already knowable at the handler. The
application-side alternatives (an `instanceof` guard in each ByEvent method, or a type lookup inside
each `handleEvent`) repeat the workaround at every call site of every generated app and mask genuine
type errors. Human-decided under the `2-fw` gate; the two application-side options were presented and
rejected.

**Impact scope:** every application using the library. `EventHandler` gains an abstract method, so
all existing subclasses must implement it — the three pre-existing handlers in this application were
updated in the same commit. Sagas and TCC profiles alike; no behavioural change beyond dropping
deliveries that would previously have thrown.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | § Event Classes, § Publishing Events, § EventSubscription (+ snapshot-seeded version backlog), § EventHandler, § Polling, § Canonical Wiring Snippet, § ByEvent sagaState guard, § Cascade Invalidation Pattern | No | Nothing described `EventHandler` receiving foreign aggregate ids; the canonical handler snippet was the exact shape that fails. Fixed this session. |
| `docs/concepts/testing.md` | § T3 — Subscription (Inter-Invariant) Test, § Assertion Ownership, deletion-event `and:`-block pattern, `loadForCheck` | Yes | — |
| `.claude/skills/implement-aggregate/session-d.md` | § Produce (all subsections), § Shared-anchor events, § Deletion events table, § ACTIVE guard, § BeanConfiguration | Partial | The deletion-events decision table resolved the case plan.md left vague, but the § Cascade Invalidation cross-reference reads as unconditional (see below). |
| `.claude/skills/_shared/conventions.md` | § Anchor to repository root, § Resolve app context, § Harness log, § Application isolation, § Neutral domain, § Run the test suite | Yes | — |
| `AGENTS.md` | § Harness evolution | Yes | The `2-fw` halt gate fired exactly as written and was the right call. |

---

## Skill Instructions Feedback

### What worked well

- § "Shared-anchor events: service-layer filtering" named the exact shape of
  `DisenrollStudentFromCourseExecutionEvent` (anchored on the execution, discriminated on the
  student) before the code was written, and explicitly forbade the `subscribesEvent()` override that
  would have looked like the natural fix.
- § "Deletion events: remove() on the whole consumer vs. remove a sub-entity" resolved what plan.md
  §3.2 left as prose ("mark the answer session's student reference stale" — a mechanism no field on
  `QuizAnswerStudent` provides). The question the table poses — can the consumer still fulfil its
  purpose without the referenced entity — answered it unambiguously for a single required snapshot.
- § "Always advance the cached publisher version" made the T3 "reflects event" assertions non-trivial
  for `UpdateQuestionEvent`, whose payload carries nothing the `QuestionAnswer` caches.

### What was unclear or missing

- § "Deletion events" says whole-consumer invalidation must "publish an outbound invalidation event
  so downstream aggregates can react", and `events.md` § Cascade Invalidation Pattern states its
  trigger as "when the ByEvent method calls `copy.remove()`" — both unconditional. `QuizAnswer` has
  four such handlers and the spec pair declares no `Invalidate{Consumer}Event` for it, even though
  `Tournament` caches a `QuizAnswer` reference. Followed the spec pair (the §4 event table is the
  authority on the event set) and the 2.6.d precedent (`QuizService.removeForDeletedExecution`
  removes without publishing). The docs do not say the spec pair wins, so this was inferred.
- Nothing in the reading list or the § Produce subsections warned that a consumer's handler is
  reachable at all by another aggregate's id. Now documented.

### Suggested wording / structure changes

- `docs/concepts/events.md` § Cascade Invalidation Pattern: state the precondition as "when a
  downstream aggregate subscribes to an invalidation event for this consumer", and say what to do
  when the spec pair declares none.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/events.md` | § EventHandler and § Canonical Wiring Snippet described a handler that receives only its own aggregate's ids; the framework delivered every aggregate's. | High | Fixed this session: `aggregateType()` added to both snippets plus a paragraph on why it is mandatory. |
| `docs/concepts/events.md` | § Cascade Invalidation Pattern's trigger is stated unconditionally, but a consumer with no downstream subscriber and no spec-declared invalidation event cannot follow it. | Medium | Gate the pattern on a downstream subscriber existing; name the spec pair's §4 event table as the authority. |
| `.claude/skills/implement-aggregate/session-d.md` | § T3 requires "two tests per subscribed event type" but does not say which discriminator the "ignores unrelated" twin should use for a shared-anchor event — a different anchor, or the same anchor with a different discriminating field. Only the second exercises the service-layer filter the same file mandates. | Low | Say the twin must vary the discriminating field for shared-anchor events. |

---

## Patterns to Capture

- **Pattern:** one service mutate method serving two events whose payloads differ
  **Observed in:** `QuizAnswerService.setStudentName`, called by both `setStudentNameByEvent` and
  `anonymizeStudentByEvent`
  **Description:** `UpdateStudentNameEvent.updatedName` and `AnonymizeStudentEvent.name` reach the
  same cached field, and the student snapshot caches no username, so anonymization *is* the name
  replacement. session-d.md's reuse rule ("reuse the existing service method when it already performs
  exactly this mutation") covers it, but the case where two distinct events collapse onto one service
  method is worth naming: the ByEvent methods stay one-per-event as mandated, only the service method
  is shared.

- **Pattern:** guard-then-`remove()` shape for whole-consumer invalidation
  **Observed in:** `QuizAnswerService.removeForDeletedStudent`, `removeForDisenrolledStudent`,
  `removeForDeletedExecution`, `removeForInvalidatedQuiz`
  **Description:** four handlers with the same body (`load → copy → discriminating guard → remove()
  → registerChanged`) differing only in the guard predicate. Keeping them as four named methods
  rather than one parameterised one makes each rule (`USER_EXISTS`, `COURSE_EXECUTION_EXISTS`,
  `QUIZ_EXISTS`) greppable from its own service method. `verifyInvariants()` is deliberately not
  called on these paths — no field an invariant reads changes — matching the 2.6.d precedent.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 54

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 54 | 2-fw | fixed | `73d29e458` |

---

## One-Line Summary

`EventHandler` handed every consumer the ids of every aggregate in the database, so the first
aggregate to share an event type with an already-implemented consumer exposed a latent
`ClassCastException` that had been sitting in the framework since 2.4.d.

---

## Addendum - 2026-08-08

The `EventHandler.aggregateType()` filter recorded above was reverted the same day; see harness-log
row 55. The diagnosis held (a handler really was receiving foreign aggregate ids) but the cause was
misattributed to `simulator/`. `AggregateRepository` is polymorphic by design, and no generated
repository is obliged to extend it - `master`'s applications type theirs as
`JpaRepository<{ConcreteEntity}, Integer>` and never reach the fault. The defect was in
session-a.md's repository template, which this run had followed seven times. `EventHandler.java` is
back to `master`'s version and the seven repositories are typed against their concrete aggregates.

The rest of this retro stands as written.
