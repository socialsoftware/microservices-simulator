# Retro — 2.5.d — Question

**App:** quizzes-full-2
**Session:** 2.5.d (Event Wiring)
**Date:** 2026-08-06

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/notification/subscribe/QuestionSubscribesUpdateTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/notification/subscribe/QuestionSubscribesDeleteTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/notification/handling/QuestionEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/notification/handling/handlers/QuestionEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/eventProcessing/QuestionEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/Question.java` (modified — `getEventSubscriptions()` built out)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/service/QuestionService.java` (appended — `setTopicName`, `removeDeletedTopic`, `findTopic`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/functionalities/QuestionFunctionalities.java` (appended — `setTopicNameByEvent`, `removeDeletedTopicByEvent`, `isInSaga`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/question/QuestionInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended — three Question event beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (modified — `createQuestion` gained `topicAggregateIds`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (checkbox ticked, 2.5.d row amended)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | § Event Classes, § Publishing Events, § EventSubscription (incl. snapshot-seeded version note), § EventHandler, § Polling, § Canonical Wiring Snippet, § ByEvent sagaState guard, § Cascade Invalidation Pattern | Yes | Cascade Invalidation read but not applicable — `DeleteTopicEvent` removes a collection member, it does not invalidate the Question |
| `docs/concepts/testing.md` | § T3 — Subscription (Inter-Invariant) Test | Yes | Deletion-event `and:`-block pattern read but not needed: the Question survives topic deletion, so the load-and-assert form applies |
| `.claude/skills/implement-aggregate/session-d.md` | whole file | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/_shared/session-completion.md` | whole file | Yes | — |
| `AGENTS.md` | § Harness evolution | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The "Deletion events: `remove()` on the whole consumer vs. remove a sub-entity" table settled `DeleteTopicEvent` in one read: a Question with one fewer topic is still a Question, so the sub-entity branch applies with no inference.
- § "Re-affirming payloads" was checked and did not bind here — `UpdateTopicEvent` carries a topic name the Question can legally differ from, so the payload assertion is non-trivial on its own. The version-advance assertion was kept anyway as the evidence the handler ran.
- The `subscribedVersion` backlog note prompted a check for a fixture-generated backlog. `TopicService` publishes on update and delete only, never on create, so the `QuestionTopic` snapshot seeded at create time has no pending event and no drain helper was needed.

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

- (none — every pattern used this session is already documented in `events.md` or `session-d.md`)

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

Question's two Topic subscriptions wired with no harness friction: the docs covered the sub-entity-removal deletion branch, the ByEvent saga-state guard and the version-advance rule without inference.
