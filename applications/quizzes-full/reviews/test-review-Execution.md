# Test Review — Execution

**Date:** 2026-05-24
**Verdict:** Yellow — one missing interleaving test added, six fake/wrong/weak tests fixed.
**Tests added:** 1
**Tests fixed:** 9
**Build:** PASS — 37 tests across 11 test files, 0 failures.

---

## 1. Expected-Test Inventory

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name |
|------|----------------------|-------------------|--------------|-----------|
| T1 | create execution | all fields correct on fresh instance | Yes | "create execution" |
| T2 | CreateExecution | happy-path success | Yes | "createExecution: success" |
| T2 | CreateExecution / NO_DUPLICATE_COURSE_EXECUTION | P3 guard | Yes | "createExecution: NO_DUPLICATE_COURSE_EXECUTION violation" |
| T2 | CreateExecution / getCourseStep | setSemanticLock READ_COURSE | Yes | "createExecution: getCourseStep acquires READ_COURSE semantic lock" |
| T2 | CreateExecution / incrementCourseExecutionCountStep | setForbiddenStates [IN_UPDATE_COURSE, IN_DELETE_COURSE] | **Added** | "createExecution: incrementCourseExecutionCountStep sees forbidden state when course is locked by concurrent updateCourse" |
| T2 | UpdateExecution | happy-path success | Yes | "updateExecution: success" |
| T2 | UpdateExecution / getExecutionStep | setSemanticLock IN_UPDATE_EXECUTION | Yes | "updateExecution: getExecutionStep acquires IN_UPDATE_EXECUTION semantic lock" |
| T2 | DeleteExecution | happy-path success + post-delete not found | Yes | "deleteExecution: success" |
| T2 | DeleteExecution / REMOVE_NO_STUDENTS | P1 intra-invariant | Yes | "deleteExecution: REMOVE_NO_STUDENTS violation — cannot delete execution with enrolled students" |
| T2 | DeleteExecution / getExecutionStep | setSemanticLock IN_DELETE_EXECUTION | Yes | "deleteExecution: getExecutionStep acquires IN_DELETE_EXECUTION semantic lock" |
| T2 | DeleteExecution / decrementCourseExecutionCountStep | setForbiddenStates [IN_UPDATE_COURSE, IN_DELETE_COURSE] | Yes | "deleteExecution: decrementCourseExecutionCountStep sees forbidden state when course is locked by concurrent updateCourse" |
| T2 | EnrollStudentInExecution | happy-path success | Yes (fixed) | "enrollStudentInExecution: success" |
| T2 | EnrollStudentInExecution / STUDENT_ALREADY_ENROLLED | P1 intra-invariant | Yes | "enrollStudentInExecution: STUDENT_ALREADY_ENROLLED violation" |
| T2 | EnrollStudentInExecution / INACTIVE_USER | P3 guard | No — unreachable; `User.active` is always `true` on creation, no deactivate-without-delete path exists in the domain | n/a |
| T2 | EnrollStudentInExecution / deleted user | P4a failure on getUserStep | Yes (renamed) | "enrollStudentInExecution: deleted user causes data-assembly failure" |
| T2 | EnrollStudentInExecution / getExecutionStep | setSemanticLock IN_ENROLL_STUDENT | Yes | "enrollStudentInExecution: getExecutionStep acquires IN_ENROLL_STUDENT semantic lock" |
| T2 | DisenrollStudent | happy-path + student removed | Yes (fixed) | "disenrollStudent: success" |
| T2 | DisenrollStudent / getExecutionStep | setSemanticLock IN_DISENROLL_STUDENT | Yes | "disenrollStudent: getExecutionStep acquires IN_DISENROLL_STUDENT semantic lock" |
| T2 | GetExecutionById | happy-path success | Yes (fixed) | "getExecutionById: success" |
| T2 | GetExecutionById | not found (Path A) | Yes | "getExecutionById: execution not found" |
| T2 | GetStudentByExecutionIdAndUserId | happy-path success | Yes | "getStudentByExecutionIdAndUserId: success" |
| T2 | GetStudentByExecutionIdAndUserId | execution not found (Path A) | Yes | "getStudentByExecutionIdAndUserId: execution not found" |
| T2 | GetStudentByExecutionIdAndUserId | student not enrolled (Path B) | Yes (fixed) | "getStudentByExecutionIdAndUserId: student not enrolled throws" |
| T2 | UpdateStudentName | happy-path success | Yes (fixed) | "updateStudentName: success" |
| T2 | UpdateStudentName / getExecutionStep | setSemanticLock IN_UPDATE_STUDENT_NAME | Yes | "updateStudentName: getExecutionStep acquires IN_UPDATE_STUDENT_NAME semantic lock" |
| T2 | UpdateStudentName / updateUserNameStep | setForbiddenStates [READ_USER] | Yes | "updateStudentName: updateUserNameStep sees forbidden state when user is locked by concurrent createQuizAnswer" |
| T2 | AnonymizeStudent | happy-path success | Yes (fixed) | "anonymizeStudent: success" |
| T2 | AnonymizeStudent / getExecutionStep | setSemanticLock IN_ANONYMIZE_STUDENT | Yes | "anonymizeStudent: getExecutionStep acquires IN_ANONYMIZE_STUDENT semantic lock" |
| T2 | AnonymizeStudent / anonymizeUserStep | no setForbiddenStates — no interleaving test needed | n/a | n/a |
| T3 | DeleteUserEvent | consumer reflects event | Yes | "execution removes student when user is deleted" |
| T3 | DeleteUserEvent | unrelated entity ignored | Yes | "execution ignores DeleteUserEvent for unrelated user" |
| T3 | UpdateStudentNameEvent | consumer reflects event | Yes | "execution updates cached student name on UpdateStudentNameEvent" |
| T3 | UpdateStudentNameEvent | unrelated entity ignored | Yes | "execution ignores UpdateStudentNameEvent for unrelated user" |
| T3 | AnonymizeStudentEvent | consumer reflects event | Yes | "execution anonymizes cached student data on AnonymizeStudentEvent" |
| T3 | AnonymizeStudentEvent | unrelated entity ignored | Yes | "execution ignores AnonymizeStudentEvent for unrelated user" |

---

## 2. Quality Findings (pre-fix)

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|
| ExecutionTest.groovy | "verifyInvariants: REMOVE_NO_STUDENTS" | Wrong | Calls `verifyInvariants()` directly; `testing.md` line 43: "Never call `verifyInvariants()` directly." Coverage already exists in DeleteExecutionTest. | Wrong |
| ExecutionTest.groovy | "verifyInvariants: STUDENT_ALREADY_ENROLLED" | Wrong | Same: direct `verifyInvariants()` call, forbidden by spec; covered in EnrollStudentInExecutionTest. | Wrong |
| CreateExecutionTest.groovy | "createExecution: NO_DUPLICATE_COURSE_EXECUTION violation" | Weak | `then: thrown(QuizzesFullException)` — no `ex.errorMessage` check; any thrown QuizzesFullException would pass. | Weak |
| CreateExecutionTest.groovy | (missing) | Missing | `incrementCourseExecutionCountStep` has `setForbiddenStates([IN_UPDATE_COURSE, IN_DELETE_COURSE])` on Course (`CreateExecutionFunctionalitySagas.java:69-72`) but no interleaving test exists. Only the semantic-lock acquisition test is present. | Missing |
| DeleteExecutionTest.groovy | "deleteExecution: REMOVE_NO_STUDENTS violation" | Weak | `then: thrown(QuizzesFullException)` — no `ex.errorMessage` check. | Weak |
| EnrollStudentInExecutionTest.groovy | "enrollStudentInExecution: success" | Fake | `then:` block creates a result variable but never uses it for assertions; only `noExceptionThrown()` is asserted. Student enrollment is never verified. `ExecutionService.java:90-100` is exercised but the enrolled state is not checked. | Fake |
| EnrollStudentInExecutionTest.groovy | "enrollStudentInExecution: INACTIVE_USER — deleted user causes prerequisite failure" | Wrong | Test name claims INACTIVE_USER P3 guard; actual guard is at `ExecutionService.java:93` (`if (!userDto.isActive())`). The test uses a deleted user which fails at `getUserStep` (P4a SimulatorException) before the service method is ever called. Name mismatch; INACTIVE_USER guard is never exercised. | Wrong |
| EnrollStudentInExecutionTest.groovy | "enrollStudentInExecution: STUDENT_ALREADY_ENROLLED violation" | Weak | `then: thrown(QuizzesFullException)` — no `ex.errorMessage` check. | Weak |
| DisenrollStudentTest.groovy | "disenrollStudent: success" | Weak | `then: noExceptionThrown()` only. Does not verify student was removed from enrollment. `ExecutionService.java:103-114` removes the student but removal is never asserted. | Weak |
| GetExecutionByIdTest.groovy | "getExecutionById: success" | Weak | Does not assert `result.courseId`; `ExecutionDto.java:29` populates `courseId` from `executionCourse.getCourseAggregateId()` but it is not checked. | Weak |
| GetStudentByExecutionIdAndUserIdTest.groovy | "getStudentByExecutionIdAndUserId: student not enrolled throws" | Weak | `then: thrown(QuizzesFullException)` — no `ex.errorMessage` check. | Weak |
| UpdateStudentNameTest.groovy | "updateStudentName: success" | Weak | Verifies User aggregate name but not Execution's cached student name. `ExecutionService.java:117-127` updates the cached name but it is never asserted via `getStudentByExecutionIdAndUserId`. | Weak |
| AnonymizeStudentTest.groovy | "anonymizeStudent: success" | Weak | Verifies User aggregate anonymization but not Execution's cached student data. `ExecutionService.java:130-143` anonymizes the cached name/username in Execution but neither field is verified. | Weak |

---

## 3. Edge Cases Added

- Missing edge case: `incrementCourseExecutionCountStep` forbidden-state interleaving — would catch: a concurrent `updateCourse` leaving the course in `IN_UPDATE_COURSE` while `createExecution` is mid-flight, silently succeeding instead of aborting.

---

## 4. Tests Added

**CreateExecutionTest.groovy:**
- "createExecution: incrementCourseExecutionCountStep sees forbidden state when course is locked by concurrent updateCourse"

---

## 5. Tests Fixed

**ExecutionTest.groovy:**
- Removed "verifyInvariants: REMOVE_NO_STUDENTS" (Wrong — direct `verifyInvariants()` call; T2 coverage exists)
- Removed "verifyInvariants: STUDENT_ALREADY_ENROLLED" (Wrong — same reason)
- Removed now-unused imports for `QuizzesFullException`, `ExecutionStudent`, `UserDto`

**CreateExecutionTest.groovy:**
- "createExecution: NO_DUPLICATE_COURSE_EXECUTION violation" → changed `thrown(QuizzesFullException)` to `def ex = thrown(QuizzesFullException); ex.errorMessage == QuizzesFullErrorMessage.NO_DUPLICATE_COURSE_EXECUTION`

**DeleteExecutionTest.groovy:**
- "deleteExecution: REMOVE_NO_STUDENTS violation" → added `def ex = thrown(QuizzesFullException); ex.errorMessage == QuizzesFullErrorMessage.REMOVE_NO_STUDENTS`

**EnrollStudentInExecutionTest.groovy:**
- "enrollStudentInExecution: success" → replaced unused `result` + bare `noExceptionThrown()` with `getStudentByExecutionIdAndUserId` assertions (`student.userAggregateId`, `student.userName`, `student.userUsername`)
- "enrollStudentInExecution: INACTIVE_USER" → renamed to "enrollStudentInExecution: deleted user causes data-assembly failure"; added clarifying comment explaining why the actual INACTIVE_USER P3 guard is unreachable (no deactivate-without-delete path; `User.active` forced to `true` in `User.java:50`)
- "enrollStudentInExecution: STUDENT_ALREADY_ENROLLED violation" → added `ex.errorMessage` check

**DisenrollStudentTest.groovy:**
- "disenrollStudent: success" → added second `when:`/`then:` block verifying `getStudentByExecutionIdAndUserId` throws `QuizzesFullException` after disenrollment

**GetExecutionByIdTest.groovy:**
- "getExecutionById: success" → added `result.courseId == executionDto.courseId`

**GetStudentByExecutionIdAndUserIdTest.groovy:**
- "getStudentByExecutionIdAndUserId: student not enrolled throws" → added `def ex = thrown(QuizzesFullException); ex.errorMessage == QuizzesFullErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND`

**UpdateStudentNameTest.groovy:**
- "updateStudentName: success" → added `and:` block asserting `getStudentByExecutionIdAndUserId(...).userName == NEW_NAME`

**AnonymizeStudentTest.groovy:**
- "anonymizeStudent: success" → added `and:` block asserting `getStudentByExecutionIdAndUserId(...)` returns student with `userName == "ANONYMOUS"` and `userUsername == "ANONYMOUS"`

---

## 6. Infrastructure Gap Note

The `INACTIVE_USER` P3 guard in `ExecutionService.java:93` (`if (!userDto.isActive())`) is structurally unreachable through normal test operations. `User.java:50` forces `active=true` in the creation constructor, and no domain operation deactivates a user without also deleting it. The guard can only be hit by injecting a `UserDto` with `active=false` directly, bypassing the service layer. This gap is recorded here for future consideration (e.g., a separate `DeactivateUser` operation).

---

## 7. Build Result

```
BUILD SUCCESS
Tests run: 37 (30 via *Execution*Test wildcard + 7 in DisenrollStudentTest, UpdateStudentNameTest, AnonymizeStudentTest)
Failures: 0
Errors: 0
```
