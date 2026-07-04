# Testing Taxonomy Migration Plan

Migration from the 3-tier test taxonomy (T1 Intra-Invariant / T2 Functionality / T3 Inter-Invariant)
to a 4-tier, layer-oriented taxonomy. Each phase below is executed by an **independent fresh
session**: read this file, do the phase (or the single Phase-3 session) assigned, tick the
checkbox, run the affected tests, and stop. Do not start the next phase/session.

---

## Target Taxonomy (authoritative — Phase 1 encodes this in `docs/concepts/testing.md`)

| Tier | Name | Test class | Scope | Profile-agnostic? |
|------|------|-----------|-------|-------------------|
| **T1** | Aggregate | `<Aggregate>IntraInvariantTest` | Intra-invariants (P1): creation happy-path, one violation per non-`final` P1 rule (EP), boundary on/off-points (BVA). Direct construction + `verifyInvariants()`. Unchanged from old T1. | yes |
| **T2** | Service | `<Aggregate>ServiceTest` — **one class per aggregate**, all service methods | Service contract: change persisted to DB (read back via a **fresh** UnitOfWork), uniqueness / composite-key guards, not-found paths (Path A `SimulatorException` / Path B `<App>Exception`), P3 numeric-guard boundaries. Invoke the `*Service` bean directly with a `UnitOfWork` — no saga workflow. | yes |
| **T3** | Event Publication | `<Aggregate>EventPublicationTest` — only for aggregates that publish events | Publisher side: service op runs → event row exists in the event store with correct type + payload fields. Trigger **via service call** (not via `<X>Functionalities`), assert against the event repository. Abstracts away all consumers. | mostly |
| **T4** | Functionality | `<FunctionalityName>Test` + `<Consumer>InterInvariantTest` | Orchestration + cross-aggregate consistency: saga state-machine traversal (`NOT_IN_SAGA → IN_{OP} → NOT_IN_SAGA`), semantic-lock acquisition per lock step, P3 guard violations raised **through the saga path**, event subscription → cached-state update / unrelated-event-ignored / deletion-event cases (former T3 inter-invariant content). | no (sagas-specific) |

### Locked-in decisions (do not re-litigate in sessions)

1. **T2 shape:** one `<Aggregate>ServiceTest` class per aggregate covering all its service methods.
2. **Strict assertion ownership:** each fact is asserted in exactly one tier.
   - T4 functionality tests **stop** asserting field-level persistence, uniqueness, and not-found —
     those move to T2. T4 happy paths assert orchestration outcomes: the operation completes, the
     returned DTO is coherent, and `sagaStateOf(id) == NOT_IN_SAGA`.
   - T4 keeps: lock-acquisition cases (`executeUntilStep`), P3 guard violations that involve
     cross-aggregate saga coordination, and all inter-invariant (subscription) tests.
   - T3 owns event-publication assertions; T4 subscription tests may *trigger* publication via a
     functionality but must not re-assert event-store contents.
3. **T3 trigger mechanism:** call the service directly with a `UnitOfWork`, then assert on the event
   repository. This works because `SagaUnitOfWorkService.registerEvent` saves the event immediately
   via `eventService.saveEvent(event)` (and marks it published under the `local` profile).
4. **Why T2 needs no explicit commit:** in the sagas profile, `SagaUnitOfWorkService.registerChanged`
   versions, invariant-checks, and `entityManager.merge`s the aggregate **inside the service call**;
   the workflow-level `commit(uow)` only resets `SagaState`. Persistence is a per-service property.
   Read-back in T2 must still use a fresh UnitOfWork so the assertion goes through the load path.
5. **Known coupling to document, not fix:** `registerChanged` calls `verifyInvariants()`, so T2
   fixtures can trip P1 rules — T1 and T2 are not perfectly independent layers. One sentence in
   testing.md acknowledges this.
6. **Deferred tiers unchanged:** Cross-Functionality, Fault/Behavior, and Async tests stay in the
   appendix as future work.

---

## Phase 1 — Rewrite `docs/concepts/testing.md` (1 session)

- [x] **1.1** Rewrite `docs/concepts/testing.md` for the 4-tier taxonomy **and** cut verbosity: target ≤ 350 lines (currently ~650).

Guidance:

- New taxonomy table (from this file) replaces the current one. One section per tier, each with:
  purpose (≤ 1 short paragraph), what it must cover, one Groovy template. Templates for T1 and the
  functionality/inter-invariant parts of T4 already exist — reuse, trim comments. Write new
  templates for T2 (`<Aggregate>ServiceTest`) and T3 (`<Aggregate>EventPublicationTest`).
- Keep, in compressed form: the Fake/Wrong/Weak checklist (retag T-numbers: old T3 smells → T4;
  add T2/T3-specific smells — e.g. a T3 test asserting only "an event exists" without payload
  fields is Weak; a T2 happy path reading back through the same UnitOfWork instance is Fake),
  the EP/BVA decision rule + worked-patterns table, Spec-First Ordering, the
  Saga-as-a-State-Machine section (fold into T4's intro; it justifies the lock tests),
  Not-Found Path A/B rule of thumb (now lives under T2), the T4 deletion-event `and:`-block
  pattern, and the Appendix of deferred types (may be trimmed to one template each).
- Move P3 numeric-guard boundary cases from functionality tests to T2 (update the EP/BVA section's
  routing sentence accordingly: "P1 boundaries → T1; P3 numeric-guard boundaries → T2").
- Cut candidates: duplicated prose between sections, the Test Profile/Serialization section can
  shrink to a short note, long inline comment blocks in templates.
- State the strict-ownership rules (decision 2 above) explicitly in a short "Assertion Ownership"
  section — this is what prevents T2/T4 duplication.
- Update the Directory Layout section: `<Aggregate>ServiceTest` and `<Aggregate>EventPublicationTest`
  live under `sagas/<aggregate>/` next to the intra/inter-invariant tests.
- Do **not** touch other docs, skills, or test code in this session.

---

## Phase 2 — Propagate to other docs and skills (1 session)

- [x] **2.1** Update every file that references the old tier numbering/names. Known list (verify with
  `grep -rln "T1\|T2\|T3" docs/ .claude/skills/ applications/quizzes-full/plan.md`, excluding
  `docs/reviews/` which is historical and must not be edited):
  - `docs/workflow.md` — session structure references to test tiers
  - `docs/architecture.md`, `docs/concepts/aggregate.md`, `docs/concepts/events.md`,
    `docs/concepts/rule-enforcement-patterns.md` — tier mentions/cross-links
  - `.claude/skills/implement-aggregate/SKILL.md` + `session-a.md`–`session-d.md` — which tier is
    written in which session. New mapping: session `a` writes T1 + T2 + T3 (all service-level and
    publication tests are available once the domain layer + services exist); sessions `b`/`c`
    write T4 functionality tests; session `d` writes T4 inter-invariant (subscription) tests.
    If session `a` becomes too large, T2/T3 may be assigned to `b` instead — the session author
    decides and records the choice in the skill.
  - `.claude/skills/review-tests/SKILL.md` — audit steps per tier (add T2/T3 audit steps; retag
    old T2→T4, old T3→T4-subscription)
  - `.claude/skills/review-aggregate/SKILL.md`, `.claude/skills/review-artifacts/SKILL.md`,
    `.claude/skills/classify-and-plan/SKILL.md` — tier vocabulary
  - `applications/quizzes-full/plan.md` — tier vocabulary in job-queue/session descriptions
- [x] **2.2** Consistency pass: after editing, grep for orphaned references
  (`Inter-Invariant.*T3`, `T2 Functionality`, `Service-Command`) and fix stragglers.
- Do **not** touch test code in this session. `docs/concepts/testing.md` (Phase 1 output) is the
  authority; where a skill contradicts it, the skill changes.

---

## Phase 3 — Migrate `applications/quizzes-full` tests (8 sessions, one per aggregate)

Topological order (same as plan.md). Per-session scope:

| Session | Aggregate | New T2 class | New T3 class (events published) | T4 trim scope |
|---------|-----------|--------------|--------------------------------|---------------|
| - [x] 3.1 | Course | `CourseServiceTest` | — (publishes none) | `CreateCourseTest`, `UpdateCourseTest`, `DeleteCourseTest`, `GetCourseByIdTest` |
| - [x] 3.2 | User | `UserServiceTest` | `UserEventPublicationTest` — `DeleteUserEvent`, `UpdateStudentNameEvent`, `AnonymizeStudentEvent` | `CreateUserTest`, `DeleteUserTest`, `UpdateUserNameTest`, `UpdateStudentNameTest`, `AnonymizeUserTest`, `AnonymizeStudentTest`, `GetUserByIdTest`, `GetStudentByExecutionIdAndUserIdTest` |
| - [x] 3.3 | Topic | `TopicServiceTest` | `TopicEventPublicationTest` — `UpdateTopicEvent`, `DeleteTopicEvent` | `CreateTopicTest`, `UpdateTopicTest`, `DeleteTopicTest`, `GetTopicByIdTest`, `GetTopicsByCourseIdTest` |
| - [ ] 3.4 | Execution | `ExecutionServiceTest` | `ExecutionEventPublicationTest` — `DeleteCourseExecutionEvent`, `DisenrollStudentFromCourseExecutionEvent` | `CreateExecutionTest`, `UpdateExecutionTest`, `DeleteExecutionTest`, `EnrollStudentInExecutionTest`, `DisenrollStudentTest`, `GetExecutionByIdTest`; keep `ExecutionInterInvariantTest` (relabel comments to T4) |
| - [ ] 3.5 | Question | `QuestionServiceTest` | `QuestionEventPublicationTest` — `UpdateQuestionEvent`, `DeleteQuestionEvent` | `CreateQuestionTest`, `UpdateQuestionTest`, `DeleteQuestionTest`, `GetQuestionByIdTest`, `GetQuestionsByCourseExecutionIdTest`; keep `QuestionInterInvariantTest` |
| - [ ] 3.6 | Quiz | `QuizServiceTest` | `QuizEventPublicationTest` — `InvalidateQuizEvent` | `CreateQuizTest`, `UpdateQuizTest`, `ConcludeQuizTest`, `SolveQuizTest`, `GetQuizByIdTest`; keep `QuizInterInvariantTest` |
| - [ ] 3.7 | QuizAnswer | `QuizAnswerServiceTest` | `QuizAnswerEventPublicationTest` — `QuizAnswerQuestionAnswerEvent` | `CreateQuizAnswerTest`, `AnswerQuestionTest`, `GetQuizAnswerByQuizIdAndStudentIdTest`; keep `QuizAnswerInterInvariantTest` |
| - [ ] 3.8 | Tournament | `TournamentServiceTest` | — (publishes none) | `CreateTournamentTest`, `UpdateTournamentTest`, `CancelTournamentTest`, `DeleteTournamentTest`, `AddParticipantTest`, `GetTournamentByIdTest`, `GetOpenTournamentsTest`; keep `TournamentInterInvariantTest` |

Per-session procedure (identical for every aggregate; substitute `<Aggregate>`):

1. **Read first:** `docs/concepts/testing.md` (rewritten), this file's Locked-in decisions,
   `applications/quizzes-full/plan.md` § the aggregate's section (the spec), and the aggregate's
   `*Service` class. Spec-first ordering applies: assertions trace to plan.md, not to the
   implementation.
2. **Write T2** `<Aggregate>ServiceTest` under
   `src/test/groovy/.../sagas/<aggregate>/`: per service method, a happy path (call service with a
   fresh UnitOfWork, read back through a *second* fresh UnitOfWork via the read service method or
   `aggregateLoadAndRegisterRead`), plus the uniqueness / not-found / P3-numeric-boundary cases
   **relocated from the existing functionality tests**. Relocate = move, don't copy.
3. **Write T3** `<Aggregate>EventPublicationTest` (only if the aggregate publishes events): per
   event type, trigger the publishing operation via the service, then assert the event repository
   contains the event with correct type and payload fields (per plan.md's events-published list).
   Add a negative case: an operation that must *not* publish leaves the store unchanged.
4. **Trim T4** functionality tests per strict ownership: remove assertions/cases now owned by
   T2/T3; keep happy-path traversal (`sagaStateOf == NOT_IN_SAGA`), lock-acquisition cases, and
   saga-path guard violations. If removing a case would leave a functionality test class empty of
   meaningful assertions, keep the happy-path traversal test — every write functionality retains at
   least the full-traversal case. Read-only `Get*Test` classes: their not-found cases move to T2;
   the happy read stays as-is (there is no saga state machine to assert for reads — if nothing
   remains, delete the class and note it in the commit message).
5. **Relabel** the aggregate's `<Consumer>InterInvariantTest` header comments to T4-subscription
   (no behavioral change; the deletion-event `and:` pattern and unrelated-event cases stay).
6. **Run:** `cd applications/quizzes-full && mvn clean -Ptest-sagas test` (full suite — trims can
   break other aggregates' fixtures). All green before ticking the box.
7. Tick this session's checkbox in this file. Commit message style: follow repo convention (see
   `git log`), e.g. `feat: migrate <Aggregate> tests to 4-tier taxonomy (T2 service + T3 publication, trim T4)`.

Session 3.1 (Course) is the **pilot**: it additionally validates the T2 template from Phase 1
against reality (e.g. exact repository bean to query, event-store access pattern for later
sessions) and updates `docs/concepts/testing.md` if the template proves wrong. Later sessions
follow the corrected template without re-opening it. Note for 3.1: `CourseService.updateCourse`
unconditionally throws `COURSE_FIELDS_IMMUTABLE` — its T2 test asserts exactly that.

---

## Out of scope (all phases)

- No production-code changes (services, factories, sagas, events) — tests and docs only. If a
  session finds an implementation/spec mismatch, flag it in the commit message and in a note at
  the bottom of this file; do not fix silently.
- No changes to the `applications/quizzes` reference app.
- No new deferred-tier (Cross-Functionality / Fault / Async) tests.
- `docs/reviews/*` are historical records — never edit.

## Discovered issues (append during sessions)

- Phase 2: `.claude/skills/classify-and-plan/SKILL.md` was missing from the known file list —
  its Step 7 plan.md file-list templates (2.N.a/b/c) mirror `docs/workflow.md`'s and needed the
  same T1 rename + T2/T3 additions. Fixed as part of 2.1.
- Phase 2: the migration plan's default session mapping ("session a writes T1+T2+T3") is
  infeasible — `*Service` classes don't exist until session `b`/`c`. Used the plan's explicit
  escape hatch: T2/T3 assigned to session `b` (write cases) with read-method T2 cases appended in
  session `c`. Recorded in `.claude/skills/implement-aggregate/SKILL.md`.
- Session 3.2: `UpdateStudentNameTest`/`AnonymizeStudentTest` in this row's T4 trim scope do not
  exist as separate files — an earlier pre-4-tier commit (`36c6a982`) already merged their cases
  into `UpdateUserNameTest`/`AnonymizeUserTest`. Trimmed those two classes in place; no action
  needed for the (non-existent) Student-named files.
- Session 3.2: this row's T4 trim scope also lists `GetStudentByExecutionIdAndUserIdTest`, but
  that class lives under `sagas/coordination/execution/` and drives
  `ExecutionFunctionalities.getStudentByExecutionIdAndUserId`, whose not-found logic is
  implemented in `ExecutionService`, not `UserService`. Left untouched in 3.2 — session 3.4
  (Execution) should trim its not-found cases into `ExecutionServiceTest` instead; its row's T4
  trim scope list is missing this file.
- Session 3.3: `TOPIC_MISSING_NAME` (`UpdateTopicTest`'s `"updateTopic: null name throws
  exception"`) is thrown only in `TopicFunctionalities.updateTopic()` (coordination layer),
  checked before the saga/`UnitOfWork` even exists — never in `TopicService`. Calling
  `topicService.updateTopic(...)` directly with a null name does not throw, so this guard is
  structurally unreachable from a T2 test. It is also not documented anywhere in plan.md's rule
  classification tables (§3.1/§3.2) — an implementation-only check with no spec backing. Left in
  T4 unchanged (it's the only tier that can exercise it); unlike other aggregates' P3 guards this
  one could not be relocated. A future session could consider promoting it into `TopicService`
  or a `Topic` P1 invariant so it naturally lands in T2/T1, at which point this T4 test should be
  deleted and replaced.
