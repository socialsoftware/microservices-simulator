# Test Review — QuizAnswer

## 1. Summary

- **Verdict:** Green.
- **Tests added:** 0 new test methods.
- **Tests fixed:** 3 (`getQuizAnswerByQuizIdAndStudentId: not found`, `createQuizAnswer: success`, `answerQuestion: success`).
- **Build:** PASS — 26 tests, 0 failures.

Key findings: T1 covers all 12 fields. T2 covers all 4 functionalities with happy-path and intra/P3 violations. T3 covers all 7 subscribed events with both reflect- and ignore-unrelated tests (14 tests). Three existing happy/not-found assertions were under-asserted and have been strengthened. No saga step in QuizAnswer sagas calls `setForbiddenStates`, so no forbidden-state interleaving tests are required. The existing `setSemanticLock` tests follow the established codebase precedent (`CreateQuiz: getExecutionStep acquires READ_EXECUTION semantic lock`) of verifying lock acquisition + clean resume; the foreign-mutation interleaving for these locks is exercised on the producer-aggregate side (e.g., InvalidateQuiz tests).

## 2. Expected-test Inventory

### T1 — `QuizAnswerTest.groovy`

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------|-------------------------|
| T1 | Constructor | create with valid data — all fields correct | Yes | `create quiz answer` | constructor leaves field unset |

### T2 — Write functionalities

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------|-------------------------|
| T2 | CreateQuizAnswer | happy-path success | Yes | `createQuizAnswer: success` | mutated fields wrong / not persisted |
| T2 | CreateQuizAnswer (P3) | `UNIQUE_QUIZ_ANSWER_PER_STUDENT` | Yes | `createQuizAnswer: UNIQUE_QUIZ_ANSWER_PER_STUDENT violation` | duplicate quiz answer for same (quiz,user) |
| T2 | CreateQuizAnswer (lock) | `getQuizStep` acquires `READ_QUIZ` | Yes | `createQuizAnswer: getQuizStep acquires READ_QUIZ semantic lock` | lock not acquired; allows concurrent quiz mutation |
| T2 | CreateQuizAnswer (lock) | `getUserStep` acquires `READ_USER` | Yes | `createQuizAnswer: getUserStep acquires READ_USER semantic lock` | lock not acquired; allows concurrent user mutation |
| T2 | AnswerQuestion | happy-path success | Yes | `answerQuestion: success` | answer not appended to questionAnswers |
| T2 | AnswerQuestion (P1) | `QUESTION_ALREADY_ANSWERED` | Yes | `answerQuestion: QUESTION_ALREADY_ANSWERED violation` | duplicate question answers allowed |
| T2 | AnswerQuestion (lock) | `getQuizAnswerStep` acquires `IN_ANSWER_QUESTION` | Yes | `answerQuestion: getQuizAnswerStep acquires IN_ANSWER_QUESTION semantic lock` | concurrent answer/conclude conflicts not detected |
| T2 | ConcludeQuiz | happy-path success (completed=true) | Yes | `concludeQuiz: success` | completion flag not set |
| T2 | ConcludeQuiz (lock) | `getQuizAnswerStep` acquires `IN_CONCLUDE_QUIZ` | Yes | `concludeQuiz: getQuizAnswerStep acquires IN_CONCLUDE_QUIZ semantic lock` | concurrent conclude/answer conflicts not detected |

### T2 — Read functionalities

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------|-------------------------|
| T2 | GetQuizAnswerByQuizIdAndStudentId | happy-path success | Yes | `getQuizAnswerByQuizIdAndStudentId: success` | wrong DTO returned |
| T2 | GetQuizAnswerByQuizIdAndStudentId | not-found (Path B) | Yes | `getQuizAnswerByQuizIdAndStudentId: not found` | wrong exception or missing exception on empty Optional |

### T2 — Service-command methods (event-driven mutators)

`removeQuizAnswer`, `removeQuizAnswerIfUserMatches`, `updateStudentName`, `anonymizeStudent`, `updateQuestionVersionInQuizAnswer` are invoked from event handlers; they are covered transitively by the T3 inter-invariant tests below. No standalone T2 service-command tests required.

### T3 — `QuizAnswerInterInvariantTest.groovy`

| Tier | Subscribed Event | Required Scenario | Test Exists? | Test Name | Defect caught if absent |
|------|------------------|-------------------|--------------|-----------|-------------------------|
| T3 | DeleteUserEvent | consumer reflects | Yes | `quizAnswer is deleted on DeleteUserEvent` | quizAnswer not removed when user deleted |
| T3 | DeleteUserEvent | ignores unrelated | Yes | `quizAnswer ignores DeleteUserEvent for unrelated user` | quizAnswer removed for wrong user |
| T3 | UpdateStudentNameEvent | consumer reflects | Yes | `quizAnswer reflects UpdateStudentNameEvent` | cached name not refreshed |
| T3 | UpdateStudentNameEvent | ignores unrelated | Yes | `quizAnswer ignores UpdateStudentNameEvent for unrelated user` | name overwritten by unrelated user's update |
| T3 | AnonymizeStudentEvent | consumer reflects | Yes | `quizAnswer reflects AnonymizeStudentEvent` | PII leaks after anonymize |
| T3 | AnonymizeStudentEvent | ignores unrelated | Yes | `quizAnswer ignores AnonymizeStudentEvent for unrelated user` | wrong user anonymized |
| T3 | DisenrollStudentFromCourseExecutionEvent | consumer reflects | Yes | `quizAnswer is deleted on DisenrollStudentFromCourseExecutionEvent` | disenrolled student keeps quiz answer |
| T3 | DisenrollStudentFromCourseExecutionEvent | ignores unrelated | Yes | `quizAnswer ignores DisenrollStudentFromCourseExecutionEvent for unrelated student` | wrong student's quizAnswer removed |
| T3 | UpdateQuestionEvent | consumer reflects | Yes | `quizAnswer reflects UpdateQuestionEvent` | stale cached questionVersion |
| T3 | UpdateQuestionEvent | ignores unrelated | Yes | `quizAnswer ignores UpdateQuestionEvent for unrelated question` | cached version overwritten by unrelated update |
| T3 | DeleteCourseExecutionEvent | consumer reflects | Yes | `quizAnswer is deleted on DeleteCourseExecutionEvent` | dangling quizAnswer after execution deleted |
| T3 | DeleteCourseExecutionEvent | ignores unrelated | Yes | `quizAnswer ignores DeleteCourseExecutionEvent for unrelated execution` | wrong execution's quizAnswers deleted |
| T3 | InvalidateQuizEvent | consumer reflects | Yes | `quizAnswer is deleted on InvalidateQuizEvent` | quizAnswer kept after quiz invalidated |
| T3 | InvalidateQuizEvent | ignores unrelated | Yes | `quizAnswer ignores InvalidateQuizEvent for unrelated quiz` | unrelated quiz's answers wiped |

## 3. Quality Findings

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|
| `coordination/quizanswer/GetQuizAnswerByQuizIdAndStudentIdTest.groovy` | `getQuizAnswerByQuizIdAndStudentId: not found` | bare-throw | `thrown(QuizzesFullException)` without asserting `ex.message == QUIZ_ANSWER_NOT_FOUND`; any unrelated `QuizzesFullException` would pass. Service throws explicitly at `QuizAnswerService.java:153` with this constant. | Weak |
| `coordination/quizanswer/CreateQuizAnswerTest.groovy` | `createQuizAnswer: success` | under-assertion | Only 4 of 12 mutated fields asserted; userName/userUsername/executionAggregateId/creationDate/answerDate/questionAnswerIds not verified. Kill-mutation check: removing the field assignment of any non-asserted field would still pass the test. | Weak |
| `coordination/quizanswer/AnswerQuestionTest.groovy` | `answerQuestion: success` | under-assertion | Asserted only `questionAnswerIds.contains(questionId)`. Did not assert collection size, leaving room for duplicate inserts or stale state to mask defects. | Weak |

No **Fake** or **Wrong** findings.

## 4. Edge Cases Considered

- Boundary: empty `questionAnswers` after create — already covered indirectly by `questionAnswerIds.isEmpty()` added in the strengthened happy-path.
- Duplicate `QuizAnswer` for same `(quizId, userId)` — already covered by `UNIQUE_QUIZ_ANSWER_PER_STUDENT` violation test.
- Duplicate `QuestionAnswer` for same `questionId` — already covered by `QUESTION_ALREADY_ANSWERED` test.
- Operating on deleted aggregate — exercised via T3 deletion events; aggregate `state` rendered inaccessible by `SimulatorException` on subsequent reads.
- Concurrent saga interleaving on `READ_QUIZ`/`READ_USER`/`IN_ANSWER_QUESTION`/`IN_CONCLUDE_QUIZ`: none of QuizAnswer's saga steps call `setForbiddenStates`, so the failure path is not on the QuizAnswer-saga side. The conflict surface is on producer-aggregate sagas (e.g., `invalidateQuiz` finding `READ_QUIZ` in its own forbidden states). Tests for those failures live with the producer aggregate.

No new edge-case tests added.

## 5. Tests Added

None.

## 6. Tests Fixed

- `GetQuizAnswerByQuizIdAndStudentIdTest.getQuizAnswerByQuizIdAndStudentId: not found` — assert `ex.message == QUIZ_ANSWER_NOT_FOUND`.
- `CreateQuizAnswerTest.createQuizAnswer: success` — assert `executionAggregateId`, `userName`, `userUsername`, `creationDate`, `answerDate`, `questionAnswerIds.isEmpty()`; added a read-back via `quizAnswerService.getQuizAnswerById` to verify persistence (kills the `registerChanged` mutation).
- `AnswerQuestionTest.answerQuestion: success` — assert `questionAnswerIds.size() == 1` and `completed == false`.

## 7. Build Result

```
cd applications/quizzes-full && mvn clean -Ptest-sagas test \
  -Dtest="QuizAnswerTest,QuizAnswerInterInvariantTest,CreateQuizAnswerTest,AnswerQuestionTest,ConcludeQuizTest,GetQuizAnswerByQuizIdAndStudentIdTest"
```

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 — QuizAnswerInterInvariantTest
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0 (aggregate total across 6 classes)
BUILD SUCCESS
```
