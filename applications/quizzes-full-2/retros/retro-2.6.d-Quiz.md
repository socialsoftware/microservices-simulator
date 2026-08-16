# Retro — 2.6.d — Quiz

**App:** quizzes-full-2
**Session:** 2.6.d (Event Wiring)
**Date:** 2026-08-07

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/notification/subscribe/QuizSubscribesUpdateQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/notification/subscribe/QuizSubscribesDeleteQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/notification/subscribe/QuizSubscribesDeleteCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/notification/handling/QuizEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/notification/handling/handlers/QuizEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/eventProcessing/QuizEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/Quiz.java` (getEventSubscriptions)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/service/QuizService.java` (three ByEvent mutations)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/functionalities/QuizFunctionalities.java` (three ByEvent methods + `isInSaga`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quiz/QuizInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-d.md` | all sections | Yes | The deletion-event decision table settled both deletion subscriptions without inference |
| `docs/concepts/events.md` | § Event Classes, § EventSubscription (incl. snapshot-seeded version), § EventHandler, § Polling, § Canonical Wiring Snippet, § ByEvent sagaState guard, § Cascade Invalidation Pattern | Yes | — |
| `docs/concepts/testing.md` | § T3 — Subscription (Inter-Invariant) Test | Yes | The deletion-event `and:`-block pattern was needed verbatim for both invalidation tests |

---

## Skill Instructions Feedback

### What worked well

- The "Deletion events: `remove()` on the whole consumer vs. remove a sub-entity" table maps cleanly onto plan.md's two P2 rows: `QUESTION_EXISTS` (quiz unanswerable without the question → invalidate whole consumer + cascade event) and `COURSE_EXECUTION_EXISTS` (quiz cannot outlive its execution → remove, no cascade).
- plan.md's own note pinning `InvalidateQuizEvent`'s single publication site to the `DeleteQuestionEvent` chain removed the only real ambiguity in this session: whether the execution-deletion path should also cascade. It should not, because both downstream consumers subscribe to `DeleteCourseExecutionEvent` directly.

### What was unclear or missing

- (none)

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | — | — | — |

---

## Patterns to Capture

- **Pattern:** Membership-only freeze predicate keeps a cached-field refresh legal after the freeze point
  **Observed in:** `microservices/quiz/aggregate/Quiz.java` (`questionAggregateIds()`), `microservices/quiz/service/QuizService.java` (`setQuestionDetails`)
  **Description:** An aggregate whose P1 rule freezes a collection after some point compares *membership* rather than full sub-entity equality, and the cached-payload setters on the sub-entity do not stamp the aggregate's own last-modified field. The `Update{Publisher}Event` handler therefore keeps refreshing the cached snapshot after the freeze, which is what the freeze rule actually means. Written in 2.6.a; 2.6.d is the session that depends on it.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

Quiz event wiring landed with no harness friction: the session-d deletion decision table plus plan.md's publication-site note fully determined the two deletion paths (invalidate-and-cascade on question deletion, plain removal on execution deletion), and the full suite is green at 200 tests.
