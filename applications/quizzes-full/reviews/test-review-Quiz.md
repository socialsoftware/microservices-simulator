# Test Review — Quiz

**Date:** 2026-05-24
**Verdict:** Yellow — 2 tests added, 1 test fixed; interleaving for READ_EXECUTION has no consuming step (design observation)

---

## 1. Summary

- 2 missing tests added
- 1 Weak test fixed (added error message assertion)
- All 17 tests pass: BUILD SUCCESS
- One design observation: `CreateQuizFunctionalitySagas.getExecutionStep.setSemanticLock(READ_EXECUTION)` has no consuming step with `setForbiddenStates([READ_EXECUTION])` anywhere in the codebase — the lock is acquired but never enforced by a conflict check. No interleaving test is possible for it; the existing lock-verification test is the maximum feasible coverage.

---

## 2. Expected-Test Inventory

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------|-------------------------|
| T1 | CreateQuiz | Create with valid data, assert all fields | Yes | "create quiz" | constructor doesn't set fields |
| T2 | createQuiz | Happy-path success | Yes | "createQuiz: success with questions" | quiz not persisted |
| T2 | createQuiz / P1 | QUIZ_DATE_ORDERING violation | **No → Added** | "createQuiz: violates QUIZ_DATE_ORDERING" | invalid-date quiz accepted silently |
| T2 | createQuiz / lock | READ_EXECUTION lock set — no consuming step exists | Partial (lock-verify) | "getExecutionStep acquires READ_EXECUTION" | design gap: lock never conflict-checked |
| T2 | updateQuiz | Happy-path success | Yes | "updateQuiz: success" | fields not updated |
| T2 | updateQuiz / P1 | QUIZ_DATE_ORDERING violation | Yes (was Weak) | "updateQuiz: violates QUIZ_DATE_ORDERING" | any QuizzesFullException passes |
| T2 | updateQuiz / lock | IN_UPDATE_QUIZ blocks UpdateTournament.updateQuizStep | **No → Added** | "updateQuiz: IN_UPDATE_QUIZ lock blocks concurrent UpdateTournament" | concurrent modification not blocked |
| T2 | getQuizById | Happy-path success | Yes | "getQuizById: success" | quiz fields not returned |
| T2 | getQuizById | Not-found (Path A — aggregateLoadAndRegisterRead) | Yes | "getQuizById: not found throws exception" | missing aggregate not signaled |
| T3 | UpdateQuestionEvent | quiz reflects cached question update | Yes | "quiz reflects UpdateQuestionEvent" | stale cached question data |
| T3 | UpdateQuestionEvent | unrelated question ignored | Yes | "quiz ignores UpdateQuestionEvent for unrelated question" | wrong question updated |
| T3 | DeleteQuestionEvent | quiz invalidated | Yes | "quiz is invalidated on DeleteQuestionEvent" | deleted-question quiz persists |
| T3 | DeleteQuestionEvent | unrelated question ignored | Yes | "quiz ignores DeleteQuestionEvent for unrelated question" | wrong quiz invalidated |
| T3 | DeleteCourseExecutionEvent | quiz invalidated | Yes | "quiz is invalidated on DeleteCourseExecutionEvent" | deleted-execution quiz persists |
| T3 | DeleteCourseExecutionEvent | unrelated execution ignored | Yes | "quiz ignores DeleteCourseExecutionEvent for unrelated execution" | wrong quiz invalidated |

---

## 3. Quality Findings

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|
| UpdateQuizTest.groovy | "updateQuiz: violates QUIZ_DATE_ORDERING" | Weak | `thrown(QuizzesFullException)` only; passes on any `QuizzesFullException`, not specifically QUIZ_DATE_ORDERING. Fixed: added `ex.message == QUIZ_DATE_ORDERING`. | Weak → Fixed |

No Fake or Wrong findings.

---

## 4. Edge Cases Added

- `createQuiz: violates QUIZ_DATE_ORDERING` — catches scenario where a quiz with `availableDate > conclusionDate` would be silently accepted.
- `updateQuiz: IN_UPDATE_QUIZ lock blocks concurrent UpdateTournament` — catches scenario where concurrent `UpdateTournament.updateQuizStep` is not blocked when the quiz is mid-update.

---

## 5. Tests Added

**CreateQuizTest.groovy:**
- `"createQuiz: violates QUIZ_DATE_ORDERING — availableDate after conclusionDate"` — asserts `QuizzesFullException` with `QUIZ_DATE_ORDERING` message

**UpdateQuizTest.groovy:**
- `"updateQuiz: IN_UPDATE_QUIZ lock blocks concurrent UpdateTournament from modifying quiz"` — pauses UpdateTournament before `updateQuizStep`, pauses UpdateQuiz after `getQuizStep` (holding IN_UPDATE_QUIZ), resumes UpdateTournament, asserts `SimulatorException`

---

## 6. Tests Fixed

**UpdateQuizTest.groovy:**
- `"updateQuiz: violates QUIZ_DATE_ORDERING — availableDate after conclusionDate"` — changed `thrown(QuizzesFullException)` to `def ex = thrown(QuizzesFullException)` + `ex.message == QUIZ_DATE_ORDERING`

---

## 7. Build Result

```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

QuizTest:                 1/1
QuizInterInvariantTest:   6/6
CreateQuizTest:           4/4
UpdateQuizTest:           4/4
GetQuizByIdTest:          2/2
```

---

## Design Observation

`CreateQuizFunctionalitySagas.getExecutionStep` calls `setSemanticLock(ExecutionSagaState.READ_EXECUTION)` on the Execution aggregate. No saga in the codebase calls `setForbiddenStates([READ_EXECUTION])` on Execution — meaning the READ_EXECUTION state is set but never checked by a consuming step. The `verifySagaState` infrastructure check only fires when `setForbiddenStates` is called; `setSemanticLock` alone does not prevent a concurrent saga from overwriting the state. The existing lock-verification test ("getExecutionStep acquires READ_EXECUTION semantic lock") is the maximum feasible coverage for this lock.
