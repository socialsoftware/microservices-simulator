# Retro — 2.1.c — Course

**App:** quizzes-full-2
**Session:** 2.1.c (Write Functionalities)
**Date:** 2026-08-02

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/course/CreateCourseCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/coordination/sagas/CreateCourseFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/coordination/webapi/CourseController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/course/CreateCourseTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/service/CourseService.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/messaging/CourseCommandHandler.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/coordination/functionalities/CourseFunctionalities.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/course/CourseServiceTest.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (`createCourse()` helper body replaced)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (checkbox + 2.1.c file-row amendment)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (rows 8-10)

### Application bug fixes (earlier-session files)

- (none) — the `createCourse()` helper replacement is the planned 2.1.b→2.1.c handover, not a fix.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/implement-aggregate/session-c.md` | Reads; Produce (all subsections); BeanConfigurationSagas — No Change Needed; Update `{AppClass}SpockTest.groovy` | Partial | The corrected `{src}commands/{aggregate}/` path matched `commands.md` and the two commands 2.1.b already wrote. § Produce omitted the SpockTest edit — see Documentation Gaps |
| `docs/concepts/service.md` | Injected Dependencies; Method Patterns (Read/Create); Copy-on-Write; Exception-Throw Convention; P3 Guard Placement | Partial | The Create example carries a P3 uniqueness guard inline; Course has no P3 rule and the surrounding prose makes the guard conditional, so stripping it was unambiguous. Injection-style mix is undocumented |
| `docs/concepts/commands.md` | What a Command Is; Naming Conventions; File Location; ServiceMapping; Sending Commands; Routing Commands | Yes | "A create command has no aggregate id — pass `null`" was the most load-bearing line for this slice; it pre-empted the natural but wrong instinct to invent an id in the functionality. The no-arg-ctor + setters note (added this run) was likewise decisive |
| `docs/concepts/sagas.md` | Create Functionality Sagas (Shape 1); Step Ordering; R4 Decision Table; Write Workflow Structure | Yes | Shape 1 is exactly this case and says so explicitly, including *why* no compensation is registered when the create is the last step |
| `docs/concepts/testing.md` | Assertion Ownership; T2; T4; Compensation Test; Fake/Wrong/Weak checklist; Spec-First Ordering | Yes | Assertion Ownership cleanly resolved the T2/T4 split. "Skip the compensation test for a functionality whose only step has no dependents" ruled out a `CreateCourseCompensationTest` without a judgement call |
| `applications/quizzes-full-2/plan.md` | §1 Course; Rule Classification §3.1; Implementation Order | Yes | The "COURSE_TYPE_FINAL / COURSE_NAME_FINAL are Java `final` — no predicate, no error-message constants" line prevented a search for guards that do not exist |

---

## Skill Instructions Feedback

### What worked well

- 2.1.c: the four-way alignment on the create case (`session-c.md` "one command per write functionality" → `commands.md` "the create command's `rootAggregateId` is null" → `sagas.md` Shape 1 "no lock, no forbiddenStates, no compensation" → `testing.md` "skip the compensation test") meant zero re-derivation. The degenerate DAG-root create is fully specified.
- 2.1.c: `session-c.md` § "BeanConfigurationSagas — No Change Needed" plus `service.md`'s `@Autowired`-field example for `AggregateIdGeneratorService` together answered the session's one open question without a round trip to the manager.
- 2.1.c: keeping the `createCourse()` helper signature fixed while swapping its body was frictionless — no 2.1.b call site changed, and the read tests silently upgraded to exercising the production create path.

### What was unclear or missing

- 2.1.c: `session-c.md` § `{Aggregate}Functionalities` step 1 prescribes deriving `functionalityName` via `new Throwable().getStackTrace()[0].getMethodName()`, but 2.1.b's existing coordinator methods pass string literals. Both evaluate to the same value; the slice matched the file rather than introduce a second idiom into a shared file mid-append. The same choice recurs in every session `c` on every aggregate.

### Suggested wording / structure changes

- One line in `session-c.md` (and the mirroring silence in `session-b.md`) stating which `functionalityName` form wins when appending to a file that already uses the other.

### Manager observations

- No slice re-spawns. Session `c` for Course was a single whole-session run (one write functionality).
- **Type 1 triaged and fixed (row 8, commit `688cb5f7`).** The slice filed this as a low-impact documentation gap; the manager verified it as a self-contradiction and fixed it. `session-c.md:47` declares the `###` subheadings under § Produce "the authority on what this session must emit", but the mandatory `create{Aggregate}()` helper replacement is a top-level `##` section outside § Produce. A session treating § Produce as its manifest would ship a stale placeholder helper — and, worse, would leave session `b`'s read tests permanently running against fixture-built aggregates rather than the create path, which is a silent loss of coverage rather than a failure. § Produce now points at it.
- **Two Type 2 items logged deferred rather than escalated (rows 9, 10).** Neither blocked code: the `functionalityName` idiom divergence and the `service.md` injection-style mix were both resolved by the slice in a defensible way, and the aggregate is complete. They are raised to the human in the boundary report instead, since both recur on every remaining aggregate and are exactly what `/review-artifacts` exists to settle.
- **The slice's semantic-lock audit is empty for a structural reason, and the manager accepts it.** `CreateCourse` is a single-step create saga on a DAG root: the aggregate does not exist when the saga starts, so there is no prior state to transition from, no `setSemanticLock` call site, no lock-acquisition test, and no compensation test. There are no unresolved `Present? = No` rows, so nothing blocks this commit.
- **The create-helper swap made 2.1.b's read tests end-to-end**, and the session-end full `clean` suite confirms they still pass against the real create path (`GetCourseByIdTest`, `GetCoursesTest`, plus the six `CourseServiceTest` cases). This is the cross-session check no individual slice could have run.
- Session-end full `clean` suite: `MAVEN_EXIT=0`, 10 tests, 0 failures, 0 errors, across all five test classes freshly compiled.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateCourseFunctionalitySagas` | `createCourseStep` | — (none) | — | n/a |

**No `setSemanticLock` call site exists in this session.** Course is a DAG root and `CreateCourse` is a single-step create saga: per `docs/concepts/sagas.md` § "Create Functionality Sagas", the create step declares neither a semantic lock nor `forbiddenStates`, because the aggregate does not exist when the saga starts and there is no prior state to transition from. Consequently there is no lock-acquisition test (`testing.md` § T4 requires one per `setSemanticLock` step — there are zero) and no `CreateCourseCompensationTest` (`testing.md` § "Compensation Test" exempts a functionality whose only step has no dependents). The table is empty for a structural reason, not an omission; there are no unresolved `Present? = No` rows.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-c.md` | § Produce lists `{AppClass}SpockTest.groovy` nowhere; the helper replacement is mandated only in its own top-level section, outside the section declared authoritative over what the session emits | Medium — reported by 2.1.c. A slice treating § Produce as the manifest ships a stale placeholder helper and silently drops session `b`'s read tests off the create path | **Fixed** in commit `688cb5f7`: § Produce now carries a blockquote pointing at the section and stating the consequence of missing it. Worth also having `/classify-and-plan` emit the file into the 2.{N}.c plan row |
| `.claude/skills/implement-aggregate/session-b.md`, `session-c.md` | `session-c.md` prescribes the `new Throwable().getStackTrace()` form for `functionalityName`; `session-b.md` is silent, so session `b` uses string literals and session `c` must choose when appending to the same file | Low — reported by 2.1.c. Both forms evaluate identically; the cost is a second idiom in one file, repeated on every aggregate | State which form wins when appending to a file that already uses the other, in both sub-files |
| `docs/concepts/service.md` | § "Injected Dependencies" shows `AggregateIdGeneratorService` as an `@Autowired` field while the same example takes repositories via the constructor, without stating whether the mix is deliberate | Low — reported by 2.1.c. Following the example verbatim works and is what makes `session-c.md` § "BeanConfigurationSagas — No Change Needed" true, but a reader appending to an all-constructor-injection service must re-derive that | One sentence: infrastructure collaborators introduced by a later session use field injection so the existing `@Bean` method stays untouched |

---

## Patterns to Capture

- **Pattern:** DAG-root create is the degenerate saga
  **Observed in:** `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/coordination/sagas/CreateCourseFunctionalitySagas.java`
  **Description:** Single step, `null` `rootAggregateId`, no `SagaCommand`, no semantic lock, no `forbiddenStates`, no compensation, no compensation test, no lock-acquisition test. Each omission is individually documented across three docs; a short "DAG-root create checklist" in `session-c.md` would let an agent confirm all of them at once.

- **Pattern:** The create-helper swap is when session `b`'s read tests become end-to-end
  **Observed in:** `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
  **Description:** Re-running session `b`'s read tests after the swap — not just the new session `c` tests — is the cheap check that the create path and the read path agree on aggregate identity and versioning. Worth stating as a required verification step rather than an aside.

- **Pattern:** An immutable aggregate produces an asymmetric session `c`
  **Observed in:** `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/course/CreateCourseTest.groovy`
  **Description:** One command, one saga, one T4 test, no error-message constants, no T4 violation cases, with the T2 tier carrying nearly all the weight. Useful as the harness's worked "minimum viable session `c`".

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 8, 9, 10

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 8 | 1 | fixed | `688cb5f7` |
| 9 | 2 | deferred | - |
| 10 | 2 | deferred | - |

---

## One-Line Summary

CreateCourse is the harness's degenerate write session — no lock, no compensation, no error constants — and its one real finding was that `session-c.md` declared a manifest that omitted the SpockTest helper swap, the single edit that turns session `b`'s read tests into end-to-end coverage.
