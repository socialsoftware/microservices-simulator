# Deferred Work — review-2026-05-23

**Source review:** [review-2026-05-23.md](review-2026-05-23.md)  
**Date:** 2026-05-23  
**Status:** Out of scope for the May 2026 review action-item pass

These items were identified during the artifacts review or quizzes-full investigation but were intentionally left for a follow-up pass. They do not block the doc/skill fixes from the parent review and were not required to resolve the Critical/Major action items there.

---

## T2 step-interleaving test gaps

`docs/concepts/testing.md` (lines 89–93) requires one interleaving test per saga step that acquires a semantic lock (`SagaCommand` + `setSemanticLock`). `session-b.md` does not restate this rule, so coverage gaps persist in quizzes-full even though the sagas themselves use the correct lock pattern.

| Test class | Missing scenario | Lock step | Notes |
|------------|------------------|-----------|-------|
| `DeleteUserTest.groovy` | Step-interleaving after `getUserStep` | `setSemanticLock(READ_USER)` on User | `review-User.md` Major finding; happy-path only today |
| `CreateTopicTest.groovy` | Step-interleaving after `getCourseStep` | `READ_COURSE` on Course | `review-Topic.md` Major finding |
| `UpdateTopicTest.groovy` | Step-interleaving after `getTopicStep` | `READ_TOPIC` on Topic | `review-Topic.md` Major finding |
| `CreateQuizAnswerTest.groovy` | Interleaving on Quiz lock step | `READ_QUIZ` in `CreateQuizAnswerFunctionalitySagas` | `review-QuizAnswer.md` — 2 missing interleaving cases (Quiz + User) |
| `CreateQuizAnswerTest.groovy` | Interleaving on User lock step | `READ_USER` in `CreateQuizAnswerFunctionalitySagas` | Same saga; separate foreign-aggregate lock |
| `AnswerQuestionTest.groovy` | Interleaving on QuizAnswer lock step | `IN_ANSWER_QUESTION` | `review-QuizAnswer.md` Major finding |
| `ConcludeQuizTest.groovy` | Interleaving on QuizAnswer lock step | `IN_CONCLUDE_QUIZ` | `review-QuizAnswer.md` Major finding |

**Suggested follow-up:** add interleaving cases using `executeUntilStep("…", uow)` / `resumeWorkflow(uow)` and `sagaStateOf(aggregateId)` assertions; optionally restate the rule explicitly in `session-b.md` T2 section so agents do not rely on reading `testing.md` alone.

**Aggregates with complete interleaving coverage (for reference):** Execution (all write tests), Question (all write tests), DeleteTopic, most Tournament write tests.

---

## `UpdateTournamentFunctionalitySagas` — Quiz step without lock

In `UpdateTournamentFunctionalitySagas.java`, `updateQuizStep` (lines 81–87) sends a plain `UpdateQuizCommand` to the Quiz aggregate with no `SagaCommand` wrapper and no `setForbiddenStates` check. The Tournament primary aggregate is correctly locked in `getTournamentStep`, but the downstream Quiz mutation runs unprotected.

**Risk:** a concurrent `UpdateQuiz` / `CreateQuizAnswer` / other Quiz write saga could mutate the same Quiz while `UpdateTournament` is in flight, causing lost updates or inconsistent tournament–quiz date alignment.

**Why deferred:** this is a cross-aggregate saga design choice, not a doc bug from this review. Fixing it requires deciding whether Quiz should get a semantic lock (`SagaCommand` + `READ_QUIZ` or `IN_UPDATE_QUIZ`) or a forbidden-state check per the new R4 table — and adding a corresponding T2 interleaving test.

---

## Foreign-aggregate `setForbiddenStates` vs primary-lock pattern

The reference `applications/quizzes/` app uses `setForbiddenStates` on mutate steps that touch **foreign** aggregates (e.g. `AddParticipantFunctionalitySagas` sets forbidden states on the Tournament when checking concurrent updates). quizzes-full uses **zero** `setForbiddenStates` calls — every saga relies on `SagaCommand` + `setSemanticLock` on the primary aggregate only, including multi-aggregate sagas like `CreateQuizAnswerFunctionalitySagas` (separate lock steps for Quiz and User).

**Why deferred:** R4 guidance was added to `sagas.md` so future implementations can choose correctly, but retrofitting quizzes-full would touch many sagas and tests without evidence of current test failures. This is a **design divergence** from the reference app, not an implementation error caused by the May review's doc bugs.

---

## Other quizzes-full gaps noted in aggregate reviews (not doc-driven)

| Item | Source | Priority | Notes |
|------|--------|----------|-------|
| `CreateUserTest.groovy` absent | `review-User.md` | Major | Listed in plan.md 2.2.b; infrastructure exists but no dedicated T2 happy-path test |
| P1 invariant tests missing (Tournament) | `review-Tournament.md` | Major | e.g. `TOURNAMENT_ENROLL_UNTIL_START_TIME`, `TOURNAMENT_FINAL_AFTER_START` |
| P3 `ANSWER_BEFORE_START` guard in `answerQuestion` | `review-QuizAnswer.md` | Major | Requires Tournament DTO in saga; domain rule deferred until Tournament session |
| `SagaUser` copy constructor copies `sagaState` | `review-User.md` | Minor | quizzes-full convention is reset-to-`NOT_IN_SAGA`; both patterns noted as valid |
| Dead `subscribesEvent()` override | `review-QuizAnswer.md` | Minor | `QuizAnswerSubscribesDisenrollStudentFromCourseExecution` — filtering done in service |
| Check 7 partial items | review-2026-05-23 | Minor | e.g. "do not subscribe to own events" not stated in session-d; ServiceMapping full-enum check only for current aggregate |

---

## Doc/skill follow-ups related to deferred work

| Item | Target | Notes |
|------|--------|-------|
| T2 interleaving rule in session-b | `session-b.md` | Cross-reference `testing.md` lines 89–93 explicitly in the T2 produce section |
| Phase 3 / T4–T6 implementation | New skill or workflow expansion | Phase 3 pointer fixed in `implement-aggregate/SKILL.md`, but no dedicated Phase 3 skill exists yet |
| Open Retro table row retro-2.5.d sagaState | `review-2026-05-23.md` Check 8 | Row still marked Open in the historical table; code and session-d template now address it — update on next `/review-artifacts` run |
