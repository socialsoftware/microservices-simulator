# Test Review — Course

**Date:** 2026-05-24  
**Reviewer:** review-tests skill  
**Verdict:** Yellow — 4 missing tests added, no Fake/Wrong tests found, build passes

---

## 1. Summary

- 4 tests added, 0 tests fixed
- No Fake, Wrong, or Weak findings
- All 17 tests pass (BUILD SUCCESS)

Missing tests were:
1. `UpdateCourseTest`: no lock-verification test for `getCourseStep` (`setSemanticLock(IN_UPDATE_COURSE)`)
2. `DeleteCourseTest`: no lock-verification test for `getCourseStep` (`setSemanticLock(IN_DELETE_COURSE)`)
3. `CourseCountsTest`: no floor-at-zero test for `decrementQuestionCount`
4. `CourseCountsTest`: no invariant-violation test for `incrementQuestionCount` when `executionCount == 0`

---

## 2. Expected-Test Inventory

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name |
|------|----------------------|-------------------|--------------|-----------|
| T1 | CreateCourse | Create with valid data, all fields | Yes | "create course" |
| T2 | CreateCourse | Happy path (TECNICO type) | Yes | "createCourse: success" |
| T2 | CreateCourse | Happy path (EXTERNAL type) | Yes | "createCourse: success with EXTERNAL type" |
| T2 | UpdateCourse | Always-fail COURSE_FIELDS_IMMUTABLE | Yes | "updateCourse: COURSE_FIELDS_IMMUTABLE…" |
| T2 | UpdateCourse/getCourseStep | `setSemanticLock(IN_UPDATE_COURSE)` lock verification | **No → Added** | "updateCourse: getCourseStep acquires IN_UPDATE_COURSE…" |
| T2 | DeleteCourse | Happy path + not retrievable after | Yes | "deleteCourse: success" |
| T2 | DeleteCourse/getCourseStep | `setSemanticLock(IN_DELETE_COURSE)` lock verification | **No → Added** | "deleteCourse: getCourseStep acquires IN_DELETE_COURSE…" |
| T2 | GetCourseById | Happy path, all fields | Yes | "getCourseById: success" |
| T2 | GetCourseById | Not found (Path A → `thrown(SimulatorException)`) | Yes | "getCourseById: not found throws exception" |
| T2 | incrementExecutionCount | Happy path | Yes | "incrementExecutionCount: success" |
| T2 | incrementQuestionCount | Happy path | Yes | "incrementQuestionCount: success" |
| T2 | incrementQuestionCount | CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT (executionCount=0) | **No → Added** | "incrementQuestionCount: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT…" |
| T2 | decrementExecutionCount | Happy path (questionCount=0) | Yes | "decrementExecutionCount: success when questionCount is zero" |
| T2 | decrementExecutionCount | Floor at zero | Yes | "decrementExecutionCount: floor at zero" |
| T2 | decrementExecutionCount | CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT | Yes | "decrementExecutionCount: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT" |
| T2 | decrementQuestionCount | Happy path | Yes | "decrementQuestionCount: success" |
| T2 | decrementQuestionCount | Floor at zero | **No → Added** | "decrementQuestionCount: floor at zero" |
| T3 | (none) | Course has no subscribed events | N/A | N/A |

---

## 3. Quality Findings

No Fake, Wrong, or Weak findings.

All existing tests:
- Correct `@DataJpaTest @Transactional @Import(LocalBeanConfiguration)` annotations
- Correct exception types (`SimulatorException` for Path A lookups, `QuizzesFullException` for invariant violations)
- Invariant violation tests assert `ex.message == QuizzesFullErrorMessage.XXX`
- `DeleteCourseTest: success` correctly uses `and:` extension block for the post-deletion load, keeping it in the `thrown()` capture scope

---

## 4. Edge Cases Added

- `incrementQuestionCount: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` — would catch a regression where `verifyInvariants()` fails to detect `executionCount == 0 && questionCount > 0` after an increment.
- `decrementQuestionCount: floor at zero` — would catch a regression where `Math.max(0, ...)` guard is removed and questionCount goes negative, breaking the invariant or producing corrupt state.

---

## 5. Tests Added

**UpdateCourseTest.groovy** (1 added):
- `updateCourse: getCourseStep acquires IN_UPDATE_COURSE semantic lock before updateCourseStep runs`

**DeleteCourseTest.groovy** (1 added):
- `deleteCourse: getCourseStep acquires IN_DELETE_COURSE semantic lock before deletion completes`

**CourseCountsTest.groovy** (2 added):
- `incrementQuestionCount: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT — executionCount is 0`
- `decrementQuestionCount: floor at zero`

---

## 6. Tests Fixed

None.

---

## 7. Build Result

```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test classes run:
- `CourseTest` (1 test)
- `CreateCourseTest` (2 tests)
- `UpdateCourseTest` (2 tests)
- `DeleteCourseTest` (2 tests)
- `GetCourseByIdTest` (2 tests)
- `CourseCountsTest` (8 tests)
