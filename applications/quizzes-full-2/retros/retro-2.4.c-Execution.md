# Retro — 2.4.c — Execution

**App:** quizzes-full-2
**Session:** 2.4.c (Write Functionalities)
**Date:** 2026-08-04

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/CreateExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/UpdateExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/EnrollStudentInExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/DisenrollStudentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/DeleteExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/CreateExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/UpdateExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/EnrollStudentInExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/DisenrollStudentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/DeleteExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/webapi/ExecutionController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/DisenrollStudentFromCourseExecutionEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/DeleteCourseExecutionEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/CreateExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/UpdateExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/EnrollStudentInExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/DisenrollStudentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/DeleteExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/UpdateExecutionCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/EnrollStudentInExecutionCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/DisenrollStudentCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/DeleteExecutionCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/UpdateExecutionCompensationTest/UpdateExecutionFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/EnrollStudentInExecutionCompensationTest/EnrollStudentInExecutionFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/DisenrollStudentCompensationTest/DisenrollStudentFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/DeleteExecutionCompensationTest/DeleteExecutionFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/service/ExecutionService.java` (appended by all five slices: the five write methods, the `NO_DUPLICATE_COURSE_EXECUTION` guard, the `AggregateIdGeneratorService` constructor widening)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/messaging/ExecutionCommandHandler.java` (appended by all five slices: one case + handler each)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java` (appended by all five slices: one coordinator each)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/execution/ExecutionServiceTest.groovy` (appended by all five slices; c4 added the class-scoped `EventService` field)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (appended: `NO_DUPLICATE_COURSE_EXECUTION` by c1, `INACTIVE_USER` by c3)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (c1 re-pointed `createExecution` at the real saga; c3 re-pointed `enrollStudentInExecution` and added `createActiveUser`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (c1 widened the `executionService` bean with `AggregateIdGeneratorService`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (2.4.c row amended; session and all five slice checkboxes ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (rows 33-36)

### Application bug fixes (earlier-session files)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/Execution.java` — slice c4 added the missing `removeStudent(Integer userAggregateId)` collection mutator. Session 2.4.a owns this file, and harness-log row 30 settled that add/remove helpers belong there, but 2.4.a emitted only `addStudent`. An implementation defect in 2.4.a's output, not harness friction: session-a.md was already correct on the point.
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/GetUserExecutionsTest.groovy` and `.../sagas/execution/ExecutionServiceTest.groovy` — slice c3 repaired six 2.4.b call sites that passed a synthetic user id to `enrollStudentInExecution`. Once that helper's body called the real functionality, the saga fetched the user and rejected it. See harness-log row 33.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Application isolation, § Harness log, § Run the test suite | Partial | Every slice reported the stale-surefire-totals warning as directly load-bearing. c5 found that a full run's stdout can be truncated, taking the `MAVEN_EXIT` line with it (row 35). |
| `.claude/skills/implement-aggregate/session-c.md` | § Reads, § Produce (service / handler / command / saga / functionalities / T2 / T4 / compensation / event / error constants), § BeanConfigurationSagas, § Update `{AppClass}SpockTest.groovy` | Partial | Three gaps found: the `EventService` class-scoping (row 34), the negative-case scoping (row 36), and the sibling-helper call-site fallout (row 33). |
| `docs/concepts/sagas.md` | § Create Functionality Sagas (Shape 1/2), § Lock-Acquisition Step Pattern, § Step Ordering, § R4 Decision Table, § Semantic-lock release on abort is automatic, § Write Workflow Structure | Yes | All five slices independently reported the "never `registerCompensation` to release a lock" rule as unmissable. The R4 "register the compensation iff a later step exists" rule settled c1's no-compensation decision. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns (Create / Mutate / Mutate with event publication), § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention, § P3 Guard Placement | Yes | The Create-method worked example is exactly the shape `NO_DUPLICATE_COURSE_EXECUTION` needs, with no new repository query. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 (read-back, Event Publication, Not-Found Paths), § T4 (incl. "aggregate unresolvable" exception), § Compensation Test (+ read-back rule, ImpairmentService, CRITICAL gotcha), § Fake/Wrong/Weak, § Spec-First, § EP & BVA | Yes | The row-28 read-back clause pre-answered the compensation read-back for c2, c3, c4 and c5. c5 reported the T4 delete exception as one of the few places the docs pre-empt a decision outright. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping, § Sending / Routing Commands | Yes | § File Location's "app source root, not `microservices/{aggregate}/`" is easy to get wrong and was called out clearly. The mandatory no-arg ctor + setters note is load-bearing under `serialize:true`. |
| `docs/concepts/events.md` | § Event class, § Publishing Events, § Canonical Wiring Snippet, § choosing the anchor id | Yes | Anchor rule and the grouping §4 payload table agreed for both published events. |
| `docs/concepts/rule-enforcement-patterns.md` | P1 vs P3 placement | Yes | Kept `STUDENT_ALREADY_ENROLLED` and `REMOVE_NO_STUDENTS` out of the service. |
| `applications/quizzes-full-2/plan.md` | §3.1/§3.2 rule rows, §4 Execution, 2.4.c file row | Partial | The verbatim §3.2 rows decided every guard placement. The 2.4.c file row omits nine files that session-c.md mandates. |
| `applications/quizzes-full-2/quizzes-full-2-aggregate-grouping.md` | §4 event payload table | Yes | c4 noted this is the only place the exact payload field list is stated; plan.md §4 alone would have left it to inference. |
| `applications/quizzes-full-2/harness-log.md` | rows 16/17, 25/28, 27, 30 | Yes | Four closed questions that would otherwise have been re-litigated by four separate slices. |

---

## Skill Instructions Feedback

### What worked well

- 2.4.c1: session-c.md's "one required edit lives outside § Produce" blockquote did its job — the helper replacement would otherwise be easy to miss.
- 2.4.c1: the 2.3.c `Topic` precedent named in the brief made the "no compensation test for create" call verifiable rather than judgemental.
- 2.4.c2: session-c.md's per-functionality (not per-session) applicability test for the compensation file gave an unambiguous yes.
- 2.4.c2, c4, c5: the mandated compensation sanity check — flip the fault flag to 0, confirm the lock step ran before the fault — is cheap and is the only evidence distinguishing a real compensation test from one that faults before the lock is ever taken. All four lock-holding slices ran it.
- 2.4.c3: the "append, never rewrite" contract held for all four shared files across five slices with no collision.
- 2.4.c4: the brief's precedent pointers to c2/c3's compensation tests and CSVs made the shape unambiguous.
- 2.4.c5: testing.md § T4's exception for a functionality whose success makes its own aggregate unresolvable fully determined `DeleteExecutionTest`'s shape.

### What was unclear or missing

- 2.4.c1: the manager's brief said `ExecutionController` was "create endpoint only; later slices append their own", but session-c.md § "{Aggregate}Controller" mandates a `@RestController` with an empty body and no methods. The slice followed the doc.
- 2.4.c2: session-c.md § CommandHandler says mutating cases "yield null" without showing the code shape; both an arrow case with a null-returning handler and a block with an explicit `yield null` compile. Matched the sibling aggregate.
- 2.4.c3: session-c.md is silent on the call-site fallout of a sibling-helper swap, and on who owns adding a missing upstream fixture under delegated execution.
- 2.4.c4: `.claude/agents/aggregate-slice.md` has no category for "a prior session's output is incomplete in a way my slice needs" — it is neither Type 1/2 friction (conventions.md excludes implementation defects) nor within the slice's declared file list.
- 2.4.c5: session-c.md's "one negative case" for event publication does not say whether that is per class or per published event type.

### Suggested wording / structure changes

- Rows 33, 34 and 35 applied. Row 36 deferred to the aggregate boundary. See § Harness Changes.

### Manager observations

- Five slices, five `DONE` results, **zero re-spawns**. No slice halted, and the session-end full clean suite was green on the first run (`MAVEN_EXIT=0`, tests=122, failures=0, errors=0) — no cross-slice regression, despite four shared files each taking five appends and two helper bodies being swapped mid-session.
- The `ExecutionController` wording defect was the manager's, not the harness's: the c1 brief described a controller with endpoints where session-c.md mandates an empty stub. c1 followed the doc and flagged it. The c2-c5 briefs were corrected to name the doc's shape and forbid additions, so no later slice added an endpoint. Recorded here rather than in harness-log.md because the fault was in a brief, which is not a harness artifact.
- Row 33 is a defect the manager introduced in the row-31 fix during 2.4.b, caught one session later by c3. The row-31 text promised call-site stability across the swap while its own template made that impossible. Two of this run's five harness rows (32, 33) are manager-authored defects caught by the next slice to read them — the Type 1 fast path is working, but the manager's own edits are now the largest single source of friction and deserve the same scepticism a slice's report gets.
- Row 34 was fixed **between** c4 and c5, which is the whole point of sequential slices: c5 received a brief that named the constraint and reused c4's field instead of hitting the compile error.
- c4 repaired a 2.4.a defect in place (`Execution.removeStudent`). It correctly classified this as an implementation defect rather than harness friction, and its observation stands: harness-log row 30's note that "the slice's code already matched, so no application code changed" was true for `addStudent` only. c5 then found `setStudents` sufficient for clearing the roster and needed no further mutator.
- c3's call-site repair crossed slice boundaries: it rewrote six call sites in two files authored by 2.4.b, including one test class (`GetUserExecutionsTest`) belonging to another session entirely. It was the only way to satisfy the mandated helper swap, it reported the fact prominently, and the full suite confirms it. This is the case row 33's fix is meant to prevent recurring.
- 2.4.d was not run: the human ended the run at the 2.4.c boundary.

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateExecutionFunctionalitySagas` | — (no `setSemanticLock` call site: `getCourseStep` is a plain upstream read; `createExecutionStep` brings the aggregate into existence) | none | n/a | n/a |
| `UpdateExecutionFunctionalitySagas` | `getExecutionStep` (`setSemanticLock IN_UPDATE_EXECUTION`) | none — primary aggregate only | `UpdateExecutionTest."updateExecution: getExecutionStep acquires IN_UPDATE_EXECUTION semantic lock"` | Yes |
| `EnrollStudentInExecutionFunctionalitySagas` | `getExecutionStep` (`setSemanticLock IN_ENROLL_STUDENT_IN_EXECUTION`) | none — `getUserStep` is a plain read with no `forbiddenStates` | `EnrollStudentInExecutionTest."enrollStudentInExecution: getExecutionStep acquires IN_ENROLL_STUDENT_IN_EXECUTION semantic lock"` | Yes |
| `DisenrollStudentFunctionalitySagas` | `getExecutionStep` (`setSemanticLock IN_DISENROLL_STUDENT`) | none — primary aggregate only | `DisenrollStudentTest."disenrollStudent: getExecutionStep acquires IN_DISENROLL_STUDENT semantic lock"` | Yes |
| `DeleteExecutionFunctionalitySagas` | `getExecutionStep` (`setSemanticLock IN_DELETE_EXECUTION`) | none — primary aggregate only | `DeleteExecutionTest."deleteExecution: getExecutionStep acquires IN_DELETE_EXECUTION semantic lock"` | Yes |

No `Present? = No` rows. All four `ExecutionSagaState` constants declared in 2.4.a are acquired by exactly the saga plan.md assigns them to, and each has its lock-acquisition test.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-b.md` § "Fixture state a create cannot reach" | That the sibling helper's foreign id must be minted by the upstream fixture, in the state the 2.{N}.c functionality's P3 guards demand. | High — reported by 2.4.c3. Six call sites across two files had to be rewritten by a slice that did not own them. | Applied (row 33) — row 27's provenance rule carried into the section, plus the P3-state clause and a rule for adding a missing upstream helper. |
| `.claude/skills/implement-aggregate/session-c.md` § "{Aggregate}ServiceTest.groovy" | That the autowired `EventService` field is class-scoped, so under slicing only the first event-publishing slice declares it. | Medium — reported by 2.4.c4. Two slices of this session publish events; both declaring the field is a compile error. | Applied (row 34). |
| `.claude/skills/_shared/conventions.md` § "Run the test suite" | That a full run's stdout can exceed the tool output budget and be truncated, taking the `MAVEN_EXIT` verdict line with it. | Medium — reported by 2.4.c5 and hit by the manager. Leaves the agent inferring pass/fail from surefire totals alone, which the same section forbids. | Applied (row 35) — redirect to a file, which is not a pipe and preserves the exit code. |
| `.claude/skills/implement-aggregate/session-c.md` § "{Aggregate}ServiceTest.groovy" | Whether the mandated "one negative case" for event publication is per test class or per published event type. | Low — reported by 2.4.c5. Under slicing it is either duplicated or silently skipped, and no slice can tell which was intended. | Deferred (row 36) — a coverage-policy choice, not mechanically decidable. Raised at the aggregate boundary. |
| `.claude/agents/aggregate-slice.md` | No category for "a prior session's output is incomplete in a way my slice needs". Neither Type 1/2 friction nor within the slice's declared file list. | Low — reported by 2.4.c4. A slice either halts unnecessarily or writes outside its list with no sanctioned way to say why. | Add a `PRIOR-SESSION DEFECTS:` field to the return block, with a rule on repair-in-place versus halt. Not applied — no slice was blocked, and it changes the slice contract, so it belongs at a boundary review. |
| `.claude/skills/implement-aggregate/session-c.md` § CommandHandler | Does not show the mutating-case code shape, only the prose "mutating cases yield null". | Very low — reported by 2.4.c2. Both readings are correct and behave identically. | Add the two-line block form to the existing prose. Not applied. |
| `applications/quizzes-full-2/plan.md` (2.4.c row) | Omits the four compensation tests, their four CSVs, the error-message file, the two SpockTest helper swaps and the `BeanConfigurationSagas` widening — nine files session-c.md mandates. Every slice reported the delta independently. | Low — session-c.md is explicit that plan.md is a blueprint, so all nine were produced; but a slice treating the row as a manifest would ship four lock-lifecycle regressions untested. | Applied to the 2.4.c row. The generator-side fix (have `/classify-and-plan` emit the compensation test + CSV for each lock-holding write functionality) is a Phase 1 change, left for the end-of-run retrospective. |

---

## Patterns to Capture

- **Pattern:** Composite-key P3 uniqueness guard without a bespoke query
  **Observed in:** `.../execution/service/ExecutionService.java` (`createExecution`)
  **Description:** `customRepository.findAll{Aggregate}Ids()` (already latest-active-only from session `b`) + `aggregateLoadAndRegisterRead` per id + field comparison. This also registers each candidate as a UoW read, which a bespoke JPQL count query would not. Pair it with a positive T2 case that varies **one** component of the key and expects success — the only assertion distinguishing "guards the pair" from "guards the first field".

- **Pattern:** Two-step lock-then-mutate saga transfers verbatim between aggregates
  **Observed in:** all four lock-holding sagas in this session
  **Description:** The only per-aggregate decisions are the lock state constant and the mutated field list. A collection *shrink* is structurally identical to a grow: no extra data-assembly step, because the removal key is a plain id, not a foreign snapshot.

- **Pattern:** One data-assembly step serves both the P3 guard and the snapshot seed
  **Observed in:** `.../coordination/sagas/EnrollStudentInExecutionFunctionalitySagas.java`
  **Description:** The same `UserDto` is validated by the service (`isActive()`) and constructs the owned `ExecutionStudent`. Worth stating in service.md § Method Patterns next to the mutate example.

- **Pattern:** A mutate saga with two independent roots
  **Observed in:** `.../coordination/sagas/EnrollStudentInExecutionFunctionalitySagas.java` + its CSV
  **Description:** Foreign data-assembly plus primary lock works unchanged with `executeUntilStep`/`resumeWorkflow`, but its compensation CSV must list **both** roots before the faulted step. testing.md's CSV example only ever shows a linear chain.

- **Pattern:** Delete with an owned collection
  **Observed in:** `.../execution/service/ExecutionService.java` (`deleteExecution`)
  **Description:** When a P1 invariant reads `state == DELETED ⟹ collection.isEmpty()`, the service clears the collection on the factory copy and calls `remove()` on that same copy before `registerChanged`. No service-level P1 guard; the T2 assertion for the clearing is `notThrown(AppException)`, because the invariant itself is the oracle — the deleted aggregate is no longer readable.

- **Pattern:** A delete's compensation test is unproblematic to read back
  **Observed in:** `.../sagas/coordination/execution/DeleteExecutionCompensationTest.groovy`
  **Description:** On fault the aggregate is never soft-deleted, so the ordinary by-id read coordinator resolves. The unresolvability problem is confined to the success path, i.e. to T4's happy-path case.

- **Pattern:** Scope the negative event-publication case to the event under test
  **Observed in:** `.../sagas/execution/ExecutionServiceTest.groovy`
  **Description:** "enrollStudent publishes no `DisenrollStudentFromCourseExecutionEvent`" reads better than a generic non-publishing operation, and under slicing it avoids two slices writing the same test name into one class. Related to the open question in harness-log row 36.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 33, 34, 35, 36

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 33 | 1 | fixed | `3271780e` |
| 34 | 1 | fixed | `a153d946` |
| 35 | 1 | fixed | `42ba0086` |
| 36 | 2 | deferred | - |

---

## One-Line Summary

Five slices ran with zero re-spawns and a first-time-green session suite, and the session's three fixed harness defects were all in text the harness had gained during this same run — two of them manager-authored, each caught by the next slice to read them.
