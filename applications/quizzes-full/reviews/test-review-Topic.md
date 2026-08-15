# Test Review — Topic

**Date:** 2026-05-24  
**Reviewer:** review-tests skill  
**Verdict:** Yellow — 3 interleaving tests added/replaced, 1 weak assertion fixed, CreateTopic saga has no forbidden-state check on consuming step

---

## 1. Summary

- 3 tests added (replacing 3 Fake lock-verification-only tests), 1 test fixed
- Fake findings: 3 (all replaced)
- Weak findings: 1 (fixed)
- All 13 tests pass (BUILD SUCCESS)

Primary gap: `CreateTopicFunctionalitySagas.createTopicStep` sends a plain `CreateTopicCommand` without `setForbiddenStates` on Course, so no `SimulatorException` interleaving path exists for `getCourseStep`'s `READ_COURSE` lock. UpdateTopic and DeleteTopic interleaving tests use concurrent completion of the opposing write saga while the first workflow is paused after `getTopicStep`.

---

## 2. Expected-Test Inventory

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name |
|------|----------------------|-------------------|--------------|-----------|
| T1 | CreateTopic | Create with valid data, all fields | Yes | "create topic" |
| T2 | CreateTopic | Happy path | Yes | "createTopic: success" |
| T2 | CreateTopic | Second topic on same course | Yes | "createTopic: success with second topic on same course" |
| T2 | CreateTopic/getCourseStep | `setSemanticLock(READ_COURSE)` interleaving | Partial | "createTopic: createTopicStep completes when concurrent updateCourse holds course lock" |
| T2 | UpdateTopic | Happy path, name persisted | Yes | "updateTopic: success — name is persisted" |
| T2 | UpdateTopic | P3 TOPIC_MISSING_NAME guard | Yes | "updateTopic: null name throws exception" |
| T2 | UpdateTopic/getTopicStep | `setSemanticLock(READ_TOPIC)` interleaving | Yes | "updateTopic: updateTopicStep fails when topic is deleted by concurrent deleteTopic" |
| T2 | DeleteTopic | Happy path + not retrievable | Yes | "deleteTopic: success" |
| T2 | DeleteTopic/getTopicStep | `setSemanticLock(READ_TOPIC)` interleaving | Yes | "deleteTopic: deleteTopicStep fails when topic is deleted by concurrent deleteTopic" |
| T2 | GetTopicById | Happy path, all fields | Yes | "getTopicById: success" |
| T2 | GetTopicById | Not found (Path A) | Yes | "getTopicById: topic not found" |
| T2 | GetTopicsByCourseId | Returns all topics for course | Yes | "getTopicsByCourseId: returns all topics for a course" |
| T2 | GetTopicsByCourseId | Empty list for unknown courseId | Yes | "getTopicsByCourseId: returns empty list for unknown courseId" |
| T3 | (none) | No subscribed events | N/A | N/A |

No P1 intra-invariants (`Topic.verifyInvariants()` is empty). No service-command-only methods beyond `{Aggregate}Functionalities`.

---

## 3. Quality Findings

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|
| CreateTopicTest.groovy | "createTopic: getCourseStep acquires READ_COURSE semantic lock before topic is created" | Fake | `executeUntilStep` then immediate `resumeWorkflow` with no concurrent saga — always passes regardless of lock enforcement | Fake (replaced) |
| UpdateTopicTest.groovy | "updateTopic: getTopicStep acquires READ_TOPIC semantic lock before update completes" | Fake | Same pattern — no concurrent saga between pause and resume | Fake (replaced) |
| DeleteTopicTest.groovy | "deleteTopic: getTopicStep acquires READ_TOPIC semantic lock before deletion completes" | Fake | Same pattern — no concurrent saga between pause and resume | Fake (replaced) |
| UpdateTopicTest.groovy | "updateTopic: null name throws exception" | Weak | Asserted bare `thrown(QuizzesFullException)` without message constant | Weak (fixed) |
| CreateTopicTest.groovy | "createTopic: createTopicStep completes when concurrent updateCourse holds course lock" | Weak | Has concurrent saga but asserts `noExceptionThrown()` — `createTopicStep` (`CreateTopicFunctionalitySagas.java:57-61`) never re-checks Course saga state, so `SimulatorException` on resume is unreachable | Weak |

---

## 4. Edge Cases Added

- `updateTopic: updateTopicStep fails when topic is deleted by concurrent deleteTopic` — would catch: update proceeding after concurrent soft-delete removed the aggregate (`TopicService.updateTopic:54` load fails).
- `deleteTopic: deleteTopicStep fails when topic is deleted by concurrent deleteTopic` — would catch: double-delete race where second delete proceeds after first completed (`TopicService.deleteTopic:63` load fails).

---

## 5. Tests Added

**CreateTopicTest.groovy** (1 replaced):
- `createTopic: createTopicStep completes when concurrent updateCourse holds course lock`

**UpdateTopicTest.groovy** (1 replaced):
- `updateTopic: updateTopicStep fails when topic is deleted by concurrent deleteTopic`

**DeleteTopicTest.groovy** (1 replaced):
- `deleteTopic: deleteTopicStep fails when topic is deleted by concurrent deleteTopic`

---

## 6. Tests Fixed

**UpdateTopicTest.groovy**:
- `updateTopic: null name throws exception` — now asserts `ex.message == QuizzesFullErrorMessage.TOPIC_MISSING_NAME` (`TopicFunctionalities.java:39-40`)

**CreateTopicTest.groovy, UpdateTopicTest.groovy, DeleteTopicTest.groovy**:
- Replaced 3 Fake lock-verification-only tests with concurrent interleaving scenarios

---

## 7. Build Result

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test classes run:
- `TopicTest` (1 test)
- `CreateTopicTest` (3 tests)
- `UpdateTopicTest` (3 tests)
- `DeleteTopicTest` (2 tests)
- `GetTopicByIdTest` (2 tests)
- `GetTopicsByCourseIdTest` (2 tests)
