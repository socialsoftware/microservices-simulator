# test-review-Question.md

**Date:** 2026-05-24  
**Aggregate:** Question  
**Verdict:** Yellow — 2 Missing T2 invariant tests, 1 Wrong T1 test (all fixed); 3 happy-path tests Weak (strengthened). Build: PASS.

---

## 1. Summary

- 2 tests added (TOPIC_BELONGS_TO_QUESTION_COURSE T2 in CreateQuestion and UpdateQuestion)
- 1 test removed (Wrong T1 test that called `verifyInvariants()` directly)
- 3 tests strengthened (createQuestion success read-back; getQuestionById success field coverage; getQuestionsByCourseExecutionId success field coverage)
- Final build: 25 tests, 0 failures

---

## 2. Expected-Test Inventory

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------|-------------------------|
| T1 | Question | create with valid data, all fields correct | Yes | "create question" | corrupt constructor leaves field null |
| T2 | CreateQuestion | happy-path success | Yes | "createQuestion: success" | create op not exercised |
| T2 | CreateQuestion | success with no topics | Yes | "createQuestion: success with no topics" | empty-topics path skipped |
| T2 | CreateQuestion | TOPIC_BELONGS_TO_QUESTION_COURSE P1 violation | Yes (added) | "createQuestion: TOPIC_BELONGS_TO_QUESTION_COURSE — topic from different course raises exception" | cross-course topic silently accepted |
| T2 | CreateQuestion | getCourseStep setSemanticLock(READ_COURSE) | Yes | "createQuestion: getCourseStep acquires READ_COURSE semantic lock before question is created" | lock not acquired |
| T2 | CreateQuestion | incrementCourseQuestionCountStep setForbiddenStates([IN_UPDATE_COURSE, IN_DELETE_COURSE]) | Yes | "createQuestion: incrementCourseQuestionCountStep sees forbidden state when course is locked by concurrent updateCourse" | concurrent course update not blocked |
| T2 | UpdateQuestion | happy-path title/content update | Yes | "updateQuestion: success — title and content updated" | title/content mutation not persisted |
| T2 | UpdateQuestion | happy-path topics change | Yes | "updateQuestion: success — topics changed" | topic replacement not persisted |
| T2 | UpdateQuestion | TOPIC_BELONGS_TO_QUESTION_COURSE P1 violation | Yes (added) | "updateQuestion: TOPIC_BELONGS_TO_QUESTION_COURSE — topic from different course raises exception" | cross-course topic silently accepted on update |
| T2 | UpdateQuestion | getQuestionStep setSemanticLock(IN_UPDATE_QUESTION) | Yes | "updateQuestion: getQuestionStep acquires IN_UPDATE_QUESTION semantic lock" | lock not acquired |
| T2 | DeleteQuestion | happy-path, aggregate not retrievable | Yes | "deleteQuestion: success" | delete not persisted or event not fired |
| T2 | DeleteQuestion | getQuestionStep setSemanticLock(IN_DELETE_QUESTION) | Yes | "deleteQuestion: getQuestionStep acquires IN_DELETE_QUESTION semantic lock before deletion completes" | lock not acquired |
| T2 | DeleteQuestion | decrementCourseQuestionCountStep setForbiddenStates([IN_UPDATE_COURSE, IN_DELETE_COURSE]) | Yes | "deleteQuestion: decrementCourseQuestionCountStep sees forbidden state when course is locked by concurrent updateCourse" | concurrent course update not blocked |
| T2 | GetQuestionById | happy-path success | Yes | "getQuestionById: success" | read path returns wrong data |
| T2 | GetQuestionById | not found (Path A via aggregateLoadAndRegisterRead) | Yes | "getQuestionById: aggregate not found" | missing question not caught |
| T2 | GetQuestionsByCourseExecutionId | happy-path returns questions | Yes | "getQuestionsByCourseExecutionId: success" | wrong questions returned |
| T2 | GetQuestionsByCourseExecutionId | no questions returns empty list | Yes | "getQuestionsByCourseExecutionId: no questions returns empty list" | empty list not returned |
| T2 | GetQuestionsByCourseExecutionId | execution not found (Path A) | Yes | "getQuestionsByCourseExecutionId: execution not found" | missing execution not caught |
| T3 | UpdateTopicEvent | question reflects topic name update | Yes | "question reflects UpdateTopicEvent" | cached topic name stale after update |
| T3 | UpdateTopicEvent | unrelated topic event ignored | Yes | "question ignores UpdateTopicEvent for unrelated topic" | unrelated event mutates wrong question |
| T3 | DeleteTopicEvent | question removes deleted topic | Yes | "question removes topic on DeleteTopicEvent" | topic not removed on delete |
| T3 | DeleteTopicEvent | unrelated topic deletion ignored | Yes | "question ignores DeleteTopicEvent for unrelated topic" | unrelated delete mutates wrong question |

---

## 3. Quality Findings

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|
| `QuestionTest.groovy` | "TOPIC_BELONGS_TO_QUESTION_COURSE: topic from different course raises exception" | Wrong | Calls `question.verifyInvariants()` directly — `testing.md:79` forbids this; invariant tests belong in T2 via service path. Removed from T1. | Wrong |
| `CreateQuestionTest.groovy` | "createQuestion: success" | Weak | `then:` asserts only the in-memory DTO returned by the service. Removing `unitOfWorkService.registerChanged(question, unitOfWork)` from `QuestionService.createQuestion:55` leaves the test passing. Missing read-back. Also missing `creationDate` and `optionKeys` assertions. | Weak |
| `GetQuestionByIdTest.groovy` | "getQuestionById: success" | Weak | Missing `topicIds`, `creationDate`, `optionKeys` assertions — the question DTO carries all three but none are verified. | Weak |
| `GetQuestionsByCourseExecutionIdTest.groovy` | "getQuestionsByCourseExecutionId: success" | Weak | Missing `topicIds` and `optionKeys` assertions on returned DTO element. | Weak |

---

## 4. Edge Cases Added

- Missing edge case: `createQuestion with topic from a different course` — would catch: TOPIC_BELONGS_TO_QUESTION_COURSE silently accepted, producing corrupt aggregate state
- Missing edge case: `updateQuestion with topic from a different course` — would catch: same invariant reachable via update path

---

## 5. Tests Added

**CreateQuestionTest.groovy:**
- "createQuestion: TOPIC_BELONGS_TO_QUESTION_COURSE — topic from different course raises exception"

**UpdateQuestionTest.groovy:**
- "updateQuestion: TOPIC_BELONGS_TO_QUESTION_COURSE — topic from different course raises exception"

---

## 6. Tests Fixed / Strengthened

**Removed (Wrong):**
- `QuestionTest.groovy`: "TOPIC_BELONGS_TO_QUESTION_COURSE: topic from different course raises exception" — called `verifyInvariants()` directly; removed from T1; covered by new T2 tests

**Strengthened (Weak):**
- `CreateQuestionTest.groovy` "createQuestion: success" — added `result.creationDate != null`, `result.optionKeys.size() == 2`, and read-back assertion via `unitOfWorkService.aggregateLoadAndRegisterRead`
- `GetQuestionByIdTest.groovy` "getQuestionById: success" — added `topicIds.contains(topicDto.aggregateId)`, `creationDate != null`, `optionKeys.size() == 2`
- `GetQuestionsByCourseExecutionIdTest.groovy` "getQuestionsByCourseExecutionId: success" — added `result[0].topicIds.contains(topicDto.aggregateId)`, `result[0].optionKeys.size() == 2`

---

## 7. Build Result

```
Tests run: 5, Failures: 0, Errors: 0 -- CreateQuestionTest
Tests run: 4, Failures: 0, Errors: 0 -- UpdateQuestionTest
Tests run: 3, Failures: 0, Errors: 0 -- DeleteQuestionTest
Tests run: 2, Failures: 0, Errors: 0 -- GetQuestionByIdTest
Tests run: 3, Failures: 0, Errors: 0 -- GetQuestionsByCourseExecutionIdTest
Tests run: 1, Failures: 0, Errors: 0 -- QuestionTest
Tests run: 4, Failures: 0, Errors: 0 -- QuestionInterInvariantTest
Total: 25 tests, 0 failures — BUILD SUCCESS
```
