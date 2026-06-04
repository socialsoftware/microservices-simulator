# Plan — Split intra-invariant tests from service tests (no repetition)

> Working document. Each **Phase** is sized for a single Claude session: independently executable,
> independently verifiable, and tickable. Execute phases top to bottom; tick the master checklist as
> you complete each. Do **not** commit this file (matches the `*-plan.md` no-commit rule).

## Context

Supervisor's suggestion: have **explicit intra-invariant tests** and **explicit service tests** — the
former test the intra-invariants, the latter test the changes each service performs and what is
verified *at the service level* — with **no repetition of test cases**.

This aligns the test taxonomy with the project's rule-enforcement model
(`docs/concepts/rule-enforcement-patterns.md`): **P1 intra-invariants** are enforced at the
aggregate's `verifyInvariants()`, **P3 guards** at the `*Service`, **P2 inter-invariants** via event
handlers. Tests should mirror that 1:1.

Today they do not. `docs/concepts/testing.md` has no intra-invariant category; P1 coverage is split
across T1 (`<Aggregate>Test`, only "invariants pass") and T2 (`<FunctionalityName>Test` service
tests, mandated to retest every P1 violation through the service — testing.md:298-309), with
temporal boundaries duplicated back into T1. E.g. `TournamentTest` tests `ANSWER_BEFORE_START` via
direct `verifyInvariants()`, while sibling `TOURNAMENT_START_BEFORE_END_TIME` is tested through the
service in `CreateTournamentTest`.

**Decisions (confirmed):** full restructure; rename the intra-invariant home to
`<Aggregate>IntraInvariantTest` (mirrors `<Aggregate>InterInvariantTest`); **strict no-repetition** —
drop all P1 testing from service tests. The `quizzes` reference app is **not** touched.

## Target taxonomy (after)

| Type | File | Owns |
|------|------|------|
| **T1 Intra-Invariant** | `sagas/{aggregate}/{Aggregate}IntraInvariantTest.groovy` | Creation happy-path (all P1 pass + fields) · one violation per non-`final` P1 rule (EP) · boundary straddle (BVA on/off-point) for ordered-domain predicates. All via direct construction/mutation + `verifyInvariants()`. |
| **T2 Service** | `sagas/coordination/{aggregate}/{Op}Test.groovy` | Happy-path postconditions (fields, events, `SagaState`, kill-mutation) · P3 guard violations · not-found (Path A/B) · saga step-interleaving. **No P1 predicate tests.** |
| **T3 Inter-Invariant** | `sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy` | Unchanged. Cached state updates on subscribed events; unrelated-entity events ignored. |

Mapping: **P1 → T1**, **P3 + saga → T2**, **P2 → T3**.

## Master checklist

- [x] **Phase 1** — Redefine the taxonomy in `docs/concepts/testing.md`
- [x] **Phase 2** — Update the `implement-aggregate` skill
- [x] **Phase 3** — Update the `review-tests` skill
- [x] **Phase 4** — Migrate tests: **Topic** (no P1 — establishes the rename pattern)
- [x] **Phase 5** — Migrate tests: **User**
- [x] **Phase 6** — Migrate tests: **Course**
- [x] **Phase 7** — Migrate tests: **Question**
- [x] **Phase 8** — Migrate tests: **QuizAnswer**
- [x] **Phase 9** — Migrate tests: **Execution**
- [x] **Phase 10** — Migrate tests: **Quiz**
- [x] **Phase 11** — Migrate tests: **Tournament**
- [x] **Phase 12** — Cross-cutting verification & consistency sweep

> Phases 1-3 are docs/skills (the spec). Phases 4-11 each migrate one aggregate's tests and are
> independent of each other (only depend on Phase 1). Phase 12 is the final gate.

---

## Phase 1 — Redefine the taxonomy in `docs/concepts/testing.md`

**Goal:** Make the canonical doc describe the new split. This is the source of truth both skills cite.
**Depends on:** nothing.
**File:** `docs/concepts/testing.md`.

**Steps:**
1. **Taxonomy table (lines 11-15):** rename row "T1 Creation / `<Aggregate>Test`" →
   "T1 Intra-Invariant / `<Aggregate>IntraInvariantTest`"; broaden its "What it validates" to the full
   P1 matrix (pass + per-rule violation + boundary). Edit the T2 row to drop "invariant": read
   "Happy path · guard violations · ≥1 step-interleaving per saga step boundary".
2. **Directory Layout (lines 126-142):** under `{aggregate}/` show `{Aggregate}IntraInvariantTest`
   (T1) and `{Aggregate}InterInvariantTest` (T3).
3. **§ T1 — Creation Test (lines 146-195):** retitle **§ T1 — Intra-Invariant Test**. New remit:
   creation happy-path **plus** one violation per non-`final` P1 rule **plus** BVA straddles — all as
   **direct-aggregate** cases (construct, optionally call aggregate mutators e.g. `addParticipant`,
   then `verifyInvariants()`). State plainly that direct construction is *the* way to test P1 (the
   saga path stamps `now()` and cannot pin boundaries) — i.e. the old "temporal boundary exception"
   becomes the general rule. Keep the `final`-field skip note and the valid-side on-point note. Update
   the template class name to `{Aggregate}IntraInvariantTest` and add a violation + boundary example.
4. **§ T2 — Functionality Test (lines 199-309):** delete "**P1 intra-invariant violation coverage**"
   (298-309) and the "time-based invariant" recipe (300-307). Keep spec-first, kill-mutation, P3
   guard violations, not-found, step-interleaving. Add one line: "P1 intra-invariants are **not**
   tested here — see § T1."
5. **§ Service-Command Tests (311-327):** keep happy-path + floor/ceiling; redirect "Invariant
   violations" to the intra-invariant test.
6. **§ Choosing Input Values (62-98):** reframe so BVA on/off-points for *all* ordered P1 predicates
   (not just temporal) live in the intra-invariant test. Keep the worked-patterns table; drop the
   "complement a T2 far-side violation" wording.
7. **Fake/Wrong/Weak checklist (25-58):** add one smell under **Wrong** — "P1 intra-invariant
   violation asserted in a service/functionality test (misplaced; belongs in
   `{Aggregate}IntraInvariantTest`)".

**Acceptance:** Re-read testing.md end to end; no remaining "violations belong in T2" or "P1 in T2"
language; T1 section, taxonomy table, and directory layout all use `{Aggregate}IntraInvariantTest`
and describe the full P1 matrix.

---

## Phase 2 — Update the `implement-aggregate` skill

**Goal:** Future aggregate implementations produce the new split by default.
**Depends on:** Phase 1.
**Files:** `.claude/skills/implement-aggregate/session-a.md`, `session-b.md`, `session-c.md`,
`session-d.md`, `SKILL.md`.

**Steps:**
1. **session-a.md (18-19, 155-165):** session-a creates the aggregate + `verifyInvariants()`, so it
   now owns the **intra-invariant test**. Replace the "`{Aggregate}Test.groovy` (T1) — happy-path
   creation only" guidance with "`{Aggregate}IntraInvariantTest.groovy` — creation happy-path + one
   violation per non-`final` P1 rule (from this aggregate's plan.md P1 list) + BVA straddles for
   ordered predicates, all via direct `verifyInvariants()`." Reference `docs/concepts/testing.md`
   § T1 — Intra-Invariant Test.
2. **session-b.md (119-167):** remove the "P1 invariant violation tests" + BVA bullet (146-148) from
   the service-test guidance. Service tests keep happy-path, P3 guards, interleaving. In
   "Service-command method tests" (153-167) drop "invariant violations" (route to session-a); keep
   happy-path + floor/ceiling.
3. **session-c.md:** update any T1/T2 naming references only (read functionalities are unaffected).
4. **session-d.md (152-163):** keep T3 as-is; clarify that an event driving `verifyInvariants()` to
   throw is tested here as an *event-processing outcome* (event not marked processed), not a re-test
   of the predicate (the predicate lives in T1).
5. **SKILL.md:** update overview wording to the new taxonomy / file names.

**Acceptance:** grep the skill dir for `{Aggregate}Test.groovy` and "P1 invariant violation" — no
service-test guidance still asks for P1 violations; session-a asks for the full intra-invariant test.

---

## Phase 3 — Update the `review-tests` skill

**Goal:** The audit enforces the new split and flags misplaced P1 tests.
**Depends on:** Phase 1.
**File:** `.claude/skills/review-tests/SKILL.md`.

**Steps:**
1. **description (line 3) + Step 2 (60-64):** `{Aggregate}Test.groovy` → `{Aggregate}IntraInvariantTest.groovy`.
2. **Step 3 — T1 (87-89):** expand expected inventory to the full P1 matrix (pass + per non-`final`
   rule violation + boundaries), direct-aggregate style.
3. **Step 3 — T2 (96-112):** delete item 2 ("One scenario per P1 intra-invariant"). Keep happy-path,
   P3 guards, interleaving.
4. **Step 3 — Service-command (133-151):** delete item 3 ("Invariant violations").
5. **Steps 5-6:** apply boundary-coverage checks to the intra-invariant test; add a finding:
   "P1 violation asserted in a service test → **Wrong (misplaced)**, move to `{Aggregate}IntraInvariantTest`."
6. **Step 9 wildcard (310):** confirm `*{Aggregate}*Test` still matches the renamed files (it does —
   both Intra/Inter end in `Test`).

**Acceptance:** Step 3 T1 lists the P1 matrix; no step still expects P1 in T2/service-command; the new
misplacement finding is present.

---

## Per-aggregate session procedure (Phases 4-11)

Each per-aggregate phase follows the **same** procedure. Read `docs/concepts/testing.md` (post-Phase-1)
and this aggregate's `plan.md` section before starting.

1. **Rename (git mv):** `sagas/{aggregate}/{Aggregate}Test.groovy` →
   `{Aggregate}IntraInvariantTest.groovy`; rename the class to match.
2. **Populate the intra-invariant test:** for each non-`final` P1 rule in the per-phase list below:
   - a creation/positive case proving it holds, and one **violation** case via direct
     construction/mutation + `verifyInvariants()`, asserting `ex.getErrorMessage() == <RULE>`;
   - if the rule is **ordered** (count / timestamp / collection-size comparison — flagged **BVA**
     below), add the on-point (`notThrown`) and off-point (`thrown`) straddle, pinning instants
     exactly. Categorical rules (uniqueness, state freezes) get a single representative — no boundary.
   - Keep the existing "create {Aggregate}" happy-path (fields + all-pass).
3. **Strip P1 from service tests:** grep `sagas/coordination/{aggregate}/*Test.groovy` for this
   aggregate's P1 error-message constants; **move** each such case into the intra-invariant test and
   delete it from the service test. Service tests retain happy-path postconditions, P3 guards,
   not-found, and interleavings.
4. **Run:** `cd applications/quizzes-full && mvn clean -Ptest-sagas test -Dtest="*{Aggregate}*Test"`
   → BUILD SUCCESS.
5. **Tick** the phase in the master checklist.

> `{aggregate}` = lowercase, `{Aggregate}` = PascalCase. P1 rule sources: `plan.md` §3.1 / §3.2.

---

## Phase 4 — Topic

**P1 (verifyInvariants):** none. **Final/skip:** n/a.
**Action:** rename `TopicTest.groovy` → `TopicIntraInvariantTest.groovy` (creation happy-path only,
proves invariants vacuously pass). Confirm no Topic P1 constants appear in `coordination/topic/*`.
**Run:** `-Dtest="*Topic*Test"`.

## Phase 5 — User

**P1 (verifyInvariants):** `USER_DELETED_STATE` (categorical). **Skip (final):** `USER_ROLE_FINAL`.
**Action:** rename `UserTest` → `UserIntraInvariantTest`; add the `USER_DELETED_STATE` violation case;
strip any from `coordination/user/*` (`DeleteUserTest`, `AnonymizeUserTest`).
**Run:** `-Dtest="*User*Test"`.

## Phase 6 — Course

**P1:** `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` (`executionCount == 0 ⟹ questionCount == 0`).
**Skip (final):** `COURSE_TYPE_FINAL`, `COURSE_NAME_FINAL`.
**Action:** rename `CourseTest` → `CourseIntraInvariantTest`; add the counter-invariant violation
(construct Course, set counts directly, `verifyInvariants()`); move any counter-invariant case out of
`CourseCountsTest.groovy` (keep its happy-path + floor/ceiling decrement-at-zero).
**Run:** `-Dtest="*Course*Test"`.

## Phase 7 — Question

**P1:** `TOPIC_BELONGS_TO_QUESTION_COURSE` (categorical — all `topic.courseId == question.courseId`).
**Action:** rename `QuestionTest` → `QuestionIntraInvariantTest`; add the violation (a cached topic
snapshot with a mismatched courseId); strip from `coordination/question/*`.
**Run:** `-Dtest="*Question*Test"`.

## Phase 8 — QuizAnswer

**P1:** `QUESTION_ALREADY_ANSWERED` (uniqueness — distinct `questionId` in `QuestionAnswer` collection).
**Skip (final):** `QUIZANSWER_FINAL_{USER,QUIZ,COURSE_EXECUTION,CREATION_DATE}`.
**Action:** rename `QuizAnswerTest` → `QuizAnswerIntraInvariantTest`; add the duplicate-questionId
violation; strip from `coordination/quizanswer/*` (`AnswerQuestionTest`).
**Run:** `-Dtest="*QuizAnswer*Test"`.

## Phase 9 — Execution

**P1:** `REMOVE_NO_STUDENTS`, `STUDENT_ALREADY_ENROLLED` (both categorical — collection checks).
**Action:** rename `ExecutionTest` → `ExecutionIntraInvariantTest`; add both violation cases via
direct `students` manipulation; strip from `coordination/execution/*` (`DisenrollStudentTest`,
`EnrollStudentInExecutionTest`).
**Run:** `-Dtest="*Execution*Test"`.

## Phase 10 — Quiz

**P1:** `QUIZ_DATE_ORDERING` (**BVA** — ordered timestamps), `QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE`
(**BVA temporal** — immutability-after-instant variant). **Skip (final):** `QUIZ_CREATION_DATE_FINAL`,
`QUIZ_COURSE_EXECUTION_FINAL`.
**Action:** rename `QuizTest` → `QuizIntraInvariantTest`; **move** the three `QUIZ_DATE_ORDERING`
cases out of `CreateQuizTest.groovy` into it and add the full BVA straddle pinned to exact instants;
add `QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE` boundary cases. `CreateQuizTest` keeps the happy paths
and the `READ_EXECUTION` semantic-lock interleaving.
**Run:** `-Dtest="*Quiz*Test"` (excludes QuizAnswer? verify — narrow to `QuizTest,CreateQuizTest,UpdateQuizTest,GetQuizByIdTest,QuizIntraInvariantTest,QuizInterInvariantTest` if the wildcard pulls QuizAnswer).

## Phase 11 — Tournament

**P1 (BVA ordered):** `TOURNAMENT_START_BEFORE_END_TIME`, `TOURNAMENT_ENROLL_UNTIL_START_TIME`,
`TOURNAMENT_FINAL_AFTER_START`, `ANSWER_BEFORE_START`.
**P1 (categorical):** `TOURNAMENT_UNIQUE_AS_PARTICIPANT`, `TOURNAMENT_IS_CANCELED`, `TOURNAMENT_DELETE`,
`TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY`, `CREATOR_IS_NOT_ANONYMOUS`.
**Skip (final):** `TOURNAMENT_CREATOR_IS_FINAL`, `TOURNAMENT_COURSE_EXECUTION_IS_FINAL`,
`TOURNAMENT_QUIZ_IS_FINAL`.
**Action:** rename `TournamentTest` → `TournamentIntraInvariantTest` (already holds ANSWER/ENROLL +
their boundaries — the template). Add the remaining P1 violations + BVA straddles. **Move**
`TOURNAMENT_START_BEFORE_END_TIME` (violation + `startTime == endTime` boundary) out of
`CreateTournamentTest.groovy`. Grep all `coordination/tournament/*Test.groovy` for Tournament P1
constants and move every match in (`AddParticipantTest`, `SolveQuizTest`, `CancelTournamentTest`,
`DeleteTournamentTest`, etc.); leave P3 guards (`CREATOR_COURSE_EXECUTION`, `TOPIC_COURSE_MISMATCH`)
and interleavings in place.
**Run:** `-Dtest="*Tournament*Test"`.

---

## Phase 12 — Cross-cutting verification & consistency sweep

**Goal:** Prove the restructure is complete, with no repetition and no regressions.
**Depends on:** Phases 1-11.

**Steps:**
1. **Full suite:** `cd applications/quizzes-full && mvn clean -Ptest-sagas test` → BUILD SUCCESS.
2. **No-repetition grep:** for every P1 error-message constant, confirm references exist only in
   `sagas/**/ *IntraInvariantTest.groovy` and **never** in `sagas/coordination/**/ *Test.groovy`.
   Any hit in `coordination/**` is a leftover P1 case to move.
3. **Rename completeness:** confirm no `sagas/{aggregate}/{Aggregate}Test.groovy` remains (all 8 are
   now `{Aggregate}IntraInvariantTest.groovy`); class names match file names.
4. **Coverage spot-check (Tournament, Quiz, Course):** every non-`final` P1 rule from plan.md has a
   violation case (plus boundary for ordered predicates) in its intra-invariant test.
5. **Doc/skill consistency:** re-read the testing.md taxonomy table, `implement-aggregate/session-a`
   and `session-b`, and `review-tests` Step 3 — all describe the same split with no "P1 in T2"
   language.

**Acceptance:** all five checks pass; master checklist fully ticked.

## Notes / edge cases

- **`final`-field P1 rules:** no test anywhere (compiler-enforced).
- **Event-triggered P1 violation:** stays in T3 as an event-processing assertion (event not marked
  processed), not a predicate re-test.
- **`quizzes` reference app:** untouched.
- **Renames:** use `git mv` to preserve history; do not commit this plan file.
