# Plan — Implement Part A of review-2026-05-26

## Context

`docs/reviews/review-2026-05-26.md` Part A audits the `quizzes-full` test suite (62 files, ~6k LOC). The suite is correct and complete but carries concentrated **scale-related boilerplate** and a few low-grade convention drifts. Part A is mostly refactor (low risk, high readability win) — no behavior change, no new test scenarios required for the bulk of the work; only Check A6 adds assertions.

The goal of this plan is to land all seven Part A checks as a sequence of small, independently-reviewable commits, each verifiable via `mvn clean -Ptest-sagas test` from `applications/quizzes-full`.

## Constraints

- Tests must continue to pass after every commit.
- No behavior change to production code, with two exceptions:
  - **A3** adds `public static final class Steps { … }` to each `*FunctionalitySagas.java` — pure constants, no logic.
  - **A6** is test-side only.
- Follow the commit format used on this branch: `fix(quizzes-full): test-review-<topic>` (single line, no body), matching commits like `b7358c55 fix(quizzes-full): test-review-Tournament`.

---

## Staging order (one commit per check)

Order is chosen so each step can use helpers from the prior step.

### Commit 1 — A4 + A7: Constants & helper improvements

**Why first:** Pure additions on the base class; non-breaking; later commits consume these.

Edit `applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy`:

- Add constants near the existing block (lines 39–47):
  - `public static final Integer NONEXISTENT_AGGREGATE_ID = 999_999`
  - `public static final String ACRONYM_1 = "SE01"`
  - `public static final String ACADEMIC_TERM_1 = "1st Semester 2024/2025"`
  - (Use the values already established in `EnrollStudentInExecutionTest.groovy` so no per-test fix-up needed.)
- Parameterize `createQuestion` (line 123): accept an optional `List<Map>` of options (default: current 2-option, first-correct shape) so T2 tests can vary correctness/count.
- Add overload `createStartedQuiz(executionId, questionIds)` next to `createQuiz` (line 131): `availableDate = now - 5min`, `conclusionDate = now + 1d`, `resultsDate = now + 2d`, `type = "GENERATED"`.

Update the four T2 execution tests currently redefining `ACRONYM_1` / `ACADEMIC_TERM_1` (`EnrollStudentInExecutionTest`, `DeleteExecutionTest`, `UpdateExecutionTest`, `DisenrollStudentTest`, `CreateExecutionTest`, `UpdateStudentNameTest`, `AnonymizeStudentTest`) to drop the local `public static final` and inherit from the base.

Update the four "get-by-id not found" tests (`GetTournamentByIdTest`, `GetUserByIdTest`, `GetTopicByIdTest`, `GetQuestionByIdTest`) to use `NONEXISTENT_AGGREGATE_ID` instead of literal `999`.

Verify: `mvn clean -Ptest-sagas test`.

Commit: `fix(quizzes-full): test-review-base-constants-helpers`

---

### Commit 2 — A2: `loadForCheck` helper

Add to `QuizzesFullSpockTest`:

```groovy
protected <T> T loadForCheck(Integer aggregateId, Class<T> type) {
    def uow = unitOfWorkService.createUnitOfWork("check")
    return type.cast(unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow))
}
```

Collapse the ~35 inline `createUnitOfWork("check") + aggregateLoadAndRegisterRead` pairs across the 5 T3 files (`ExecutionInterInvariantTest`, `QuestionInterInvariantTest`, `QuizInterInvariantTest`, `QuizAnswerInterInvariantTest`, `TournamentInterInvariantTest`) to one-line `loadForCheck(...)` calls.

Verify: `mvn clean -Ptest-sagas test`.

Commit: `fix(quizzes-full): test-review-loadForCheck`

---

### Commit 3 — A1: `InterInvariantTestBase`

Create `applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/InterInvariantTestBase.groovy` per the sketch in `review-2026-05-26.md:181–207`:

```groovy
abstract class InterInvariantTestBase extends QuizzesFullSpockTest {
    enum Stage { ENROLLMENT, QUESTION, QUIZ, TOURNAMENT }
    Integer courseId, userId, executionId, topicId, questionId, quizId, tournamentId

    protected void buildFixture(Set<Stage> stages = [Stage.QUESTION] as Set) { … }
}
```

Stage decision per file (mirroring the actual chains observed):

| File | Stages |
|---|---|
| `ExecutionInterInvariantTest` | `[ENROLLMENT]` |
| `QuestionInterInvariantTest` | `[QUESTION]` (no user — chain is Course → Execution → Topic → Question) |
| `QuizInterInvariantTest` | `[QUIZ]` |
| `QuizAnswerInterInvariantTest` | `[QUIZ, ENROLLMENT]` |
| `TournamentInterInvariantTest` | `[TOURNAMENT]` (extends QUIZ-equivalent with Tournament instead of Quiz) |

Each T3 file:
- extends `InterInvariantTestBase`
- `def setup() { buildFixture([Stage.X] as Set) }`
- drops local `setup()` field declarations and inline fixture chains
- references the inherited `courseId`/`userId`/… fields

Tests where the fixture variation is genuine (e.g. an extra `_2` user for "ignores unrelated user" cases) keep that single extra line in their `given:` block; everything else moves to the base.

Verify: `mvn clean -Ptest-sagas test`. Expected LOC delta ≈ −150 to −200 across the 5 files.

Commit: `fix(quizzes-full): test-review-InterInvariantTestBase`

---

### Commit 4 — A3: `Steps` constants on all 39 sagas

> **Decision: SKIPPED — not worth the cost.** The proposed fix touches 39 `*FunctionalitySagas.java` files plus every `executeUntilStep(...)` test call site — a churn-heavy change with no behaviour delta. The "silent typo" risk is overstated: a misspelled step name causes the workflow not to pause, surfacing as a test failure on the first run. A better alternative is a 5-LOC runtime check in `executeUntilStep(name)` that throws when the step name is unknown. Revisit only if a typo silently passes CI.

For each `*FunctionalitySagas.java` in `applications/quizzes-full/src/main/java/.../sagas/` (39 files across `course/`, `user/`, `topic/`, `execution/`, `question/`, `quiz/`, `quizanswer/`, `tournament/`):

Add a nested constants class:

```java
public static final class Steps {
    public static final String GET_COURSE   = "getCourseStep";
    public static final String DELETE_LOCAL = "deleteCourseStep";
    // … one entry per `new SagaStep("…", …)` in this saga
}
```

Replace the string literal in the `new SagaStep(...)` call with `Steps.GET_COURSE` etc. — keeps the runtime string identical so behavior is unchanged.

Update every test that calls `executeUntilStep("…")` to use `<SagaClass>.Steps.<NAME>`. Representative call sites:
- `DeleteCourseTest.groovy:52` → `DeleteCourseFunctionalitySagas.Steps.GET_COURSE`
- `UpdateTournamentTest.groovy` step references
- `CreateExecutionTest.groovy` step references
- (full sweep: grep `executeUntilStep\("` and `resumeWorkflow` — replace each literal)

After this commit, a typo on a step name is a compile error.

Verify: `mvn clean -Ptest-sagas test`.

Commit: `fix(quizzes-full): test-review-saga-step-constants`

---

### Commit 5 — A5: T3 phrasing convergence

Pick one template from `testing.md:347` — recommend the active-voice on-clause form:
`"<consumer> <action> on <Event>[ for <role>]"`

Examples:
- `"execution removes student when user is deleted"` → `"execution removes student on DeleteUserEvent"`
- `"tournament is deleted on DeleteUserEvent for creator"` → `"tournament deletes self on DeleteUserEvent for creator"` (or accept passive form as the chosen template — pick one and apply uniformly)

Also rename local role variables to match: `creatorId` for tournament creator, `userId` only for generic users.

Touches all 5 T3 file method headers and local-var names. No assertion changes.

Verify: `mvn clean -Ptest-sagas test`.

Commit: `fix(quizzes-full): test-review-T3-phrasing`

---

### Commit 6 — A6: Enrich thin happy-path create tests

For `CreateCourseTest`, `CreateUserTest`, `CreateTopicTest`, `CreateExecutionTest`, `CreateQuestionTest`, `CreateQuizTest`, `CreateQuizAnswerTest`, `CreateTournamentTest` happy-path methods, add the assertions required by `testing.md:39–47`:

- `SagaState == NOT_IN_SAGA` post-commit (via `sagaStateOf(aggregateId)` already on the base class)
- Event registration assertion where applicable (use the existing event-registry inspection pattern from other T2 tests)
- Add `// Spec:` comment block referencing `plan.md` §2.x to satisfy B3 partially (lightweight version — full B3 is out of Part A scope but this small win lands here)

Do **not** add new test methods; this is a per-method enrichment.

Verify: `mvn clean -Ptest-sagas test`.

Commit: `fix(quizzes-full): test-review-create-test-enrichment`

---

## Critical files

**Base class & new helpers:**
- `applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy`
- `applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/InterInvariantTestBase.groovy` (new)

**T3 files (all 5 refactored):**
- `sagas/execution/ExecutionInterInvariantTest.groovy`
- `sagas/question/QuestionInterInvariantTest.groovy`
- `sagas/quiz/QuizInterInvariantTest.groovy`
- `sagas/quizanswer/QuizAnswerInterInvariantTest.groovy`
- `sagas/tournament/TournamentInterInvariantTest.groovy`

**Sagas (pattern repeated across 39 files):**
- `src/main/java/.../sagas/{course,user,topic,execution,question,quiz,quizanswer,tournament}/*FunctionalitySagas.java`

Representative tests that consume saga step names:
- `sagas/coordination/course/DeleteCourseTest.groovy`
- `sagas/coordination/tournament/UpdateTournamentTest.groovy`
- (sweep all files under `sagas/coordination/`)

**Not-found test fix-up:**
- `GetTournamentByIdTest`, `GetUserByIdTest`, `GetTopicByIdTest`, `GetQuestionByIdTest`

---

## Out of scope

- **Part B** (testing.md + skill guidance changes) — separate plan; some items (B3 `// Spec:` comment) get a lightweight version in Commit 6.
- Adding new T2/T3 scenarios — coverage gap remediation from `review-2026-05-23-deferred.md` is tracked separately.
- Reference-app changes — `applications/quizzes/` is untouched.

---

## Verification

After every commit:

```bash
cd applications/quizzes-full && mvn clean -Ptest-sagas test
```

Expected: 176 tests passing (unchanged from `review-2026-05-23.md:362` baseline). Commit 6 may add assertions inside existing tests — count must stay 176; any failure indicates a real spec gap to chase.

After the full sequence, sanity-check LOC delta:

```bash
git diff --stat master..HEAD -- 'applications/quizzes-full/src/test/groovy/**/*InterInvariantTest.groovy'
```

Expect a net reduction of roughly 150–200 LOC across the 5 T3 files.
