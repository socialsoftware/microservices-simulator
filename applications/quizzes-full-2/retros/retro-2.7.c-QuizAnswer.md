# Retro — 2.7.c — QuizAnswer

**App:** quizzes-full-2
**Session:** 2.7.c (Write Functionalities)
**Date:** 2026-08-08

---

## Files Produced

List every file created or modified this session (absolute paths).

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/QuizAnswerQuestionAnswerEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quizanswer/CreateQuizAnswerCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quizanswer/AnswerQuestionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quizanswer/ConcludeQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/service/QuizAnswerService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/messaging/QuizAnswerCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/sagas/CreateQuizAnswerFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/sagas/AnswerQuestionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/sagas/ConcludeQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/functionalities/QuizAnswerFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/webapi/QuizAnswerController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quizanswer/QuizAnswerServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/CreateQuizAnswerTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/AnswerQuestionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/AnswerQuestionCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/ConcludeQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/ConcludeQuizCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/AnswerQuestionCompensationTest/AnswerQuestionFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/ConcludeQuizCompensationTest/ConcludeQuizFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns (Create / Mutate / Mutate with event publication), § Copy-on-Write Rule, § DTO Immutability, § Exception-Throw Convention, § P3 Guard Placement | Yes | The "widen the constructor **and** the `@Bean` method" note matched exactly what `AggregateIdGeneratorService` required here. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Yes | The `null` `rootAggregateId` rule for create commands settled `CreateQuizAnswerCommand` without inference. |
| `docs/concepts/sagas.md` | § Step Ordering, § Collection-valued data-assembly step, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Write Workflow Structure, § Semantic-lock release on abort is automatic | Yes | Shape 1 applied verbatim: `createQuizAnswerStep` is last, so no compensation. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § Event Publication, § Not-Found Paths, § T4 — Functionality Test, § Compensation Test, § Choosing Input Values, § Fake/Wrong/Weak Checklist | Partial | Silent on what a T2/T4 case should assert when plan.md states no rule for an out-of-domain input; see Documentation Gaps. |
| `docs/concepts/rule-enforcement-patterns.md` | not opened | — | plan.md §7 already classified both P3s and named their enforcement site, so the taxonomy was not needed. |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/_shared/session-completion.md` | all sections | Yes | — |

**Sufficient?** = `Yes` / `Partial` / `No`
- `Partial` = doc existed but was missing something important
- `No` = doc didn't address the need; fell back to inference or guessing

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" is now precise enough that the helper swap was mechanical: session `b` had already minted every foreign id from an upstream fixture helper in the state the P3 guards demand, so replacing the body with `quizAnswerFunctionalities.createQuizAnswer(...)` needed no call-site repair and the five 2.7.b read tests stayed green unchanged. The clock-stamped-constant clause added in 2.6.c did not fire here — `QuizAnswer` has no P1 rule ordering its stamped dates against a caller-supplied one.
- The compensation-test applicability test ("a `setSemanticLock` step with a dependent step registered after it") partitioned the three functionalities cleanly with no judgement call: `AnswerQuestion` and `ConcludeQuiz` in, `CreateQuizAnswer` out.
- § Compensation Test's mandated sanity check earned its keep as a check, not a formality: flipping both fault flags to `0` and re-running confirmed from the log that `getQuizAnswerStep` executes before the fault in both sagas, and that the tests fail without the fault.
- `docs/concepts/sagas.md` § "Collection-valued data-assembly step" answered the `getQuestionsStep` shape directly — one loop step, name independent of the fixture — which matters here because the loop is over the *fetched quiz's* question list rather than a caller-supplied id list, so a per-element step name would not have been knowable at all.

### What was unclear or missing

- session-c.md § "`{Aggregate}Service.java` (write methods)" enumerates P3 own-table guards, P3 DTO-field checks and deferred P3 guards, all keyed off plan.md's rule list. It has no branch for a service method whose input space is wider than any rule plan.md states — here, an `AnswerQuestion` naming a question the `QuizAnswer` never seeded. Halted per the Type 2 gate; the human chose a new `QUESTION_NOT_IN_QUIZ_ANSWER` guard. Logged as row 53.
- The corollary in `docs/concepts/testing.md` § Spec-First Ordering: it instructs the agent to cite plan.md's rule name in every `// Spec:` comment and to treat a disagreement as an implementation bug, but a test for a guard the spec never named has no rule to cite. The comment on that case names the seeding invariant and the session that added the guard instead, which is a convention this session invented.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-c.md` § "`{Aggregate}Service.java` (write methods)": add a clause for spec-silent inputs — when a write method can be called with a value no plan.md rule constrains, and the candidate behaviours differ observably (throw / no-op / reuse an existing constant), that is Type 2 friction, not a judgement call. Naming it there would have produced the same halt without first having to reason from `AGENTS.md`'s general definition.
- `docs/concepts/testing.md` § Spec-First Ordering: state what a `// Spec:` comment cites when the guard under test post-dates plan.md — the mechanism the guard protects plus the session that added it, so the added rule stays traceable to a decision rather than reading as invented.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

For every saga produced this session, list every saga step that calls `setSemanticLock`, and the test
name covering its lock-acquisition case.

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `AnswerQuestionFunctionalitySagas` | `getQuizAnswerStep` | none (primary `QuizAnswer`) | `AnswerQuestionTest` — "answerQuestion: getQuizAnswerStep acquires IN_ANSWER_QUESTION semantic lock" | Yes |
| `ConcludeQuizFunctionalitySagas` | `getQuizAnswerStep` | none (primary `QuizAnswer`) | `ConcludeQuizTest` — "concludeQuiz: getQuizAnswerStep acquires IN_CONCLUDE_QUIZ semantic lock" | Yes |

`CreateQuizAnswerFunctionalitySagas` contributes no row: it is a Shape 1 create saga
(`sagas.md` § Create Functionality Sagas), so its four data-assembly steps are plain upstream reads
and the create step has no prior state to lock. This matches plan.md §7, which assigns
`CreateQuizAnswer` no `QuizAnswerSagaState`.

- One row per `setSemanticLock` call site.
- "Present? = No" rows are **Major** findings: add the test in this session, or open an explicit follow-up. **Never tick the session checkbox with an unresolved `No` row** unless the row is followed by an explicit deferral rationale written directly beneath this table.

---

## Documentation Gaps

Gaps in `docs/concepts/` files that caused friction or required inference or guessing.

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-c.md`, `docs/concepts/testing.md` | No guidance for a write method whose input space exceeds plan.md's rule list; Spec-First says what to do when plan.md *disagrees* with the code, not when it is *silent* | Medium — one halt this session; recurs for any aggregate with an owned sub-entity addressed by a caller-supplied key | Classify spec-silent inputs as Type 2 in session-c.md; state in testing.md § Spec-First Ordering what a `// Spec:` comment cites for a guard added after plan.md was written |
| `docs/concepts/testing.md` | § T2 — Service Test shows a direct service call, but a create method whose parameters are saga-assembled DTOs forces the test to rebuild them; the doc neither sanctions nor warns against the duplication of saga assembly logic that follows | Low — resolved by mirroring the sibling `QuizServiceTest` pattern within this application, no cross-app read needed | Note in § T2 that a test rebuilding saga-assembled inputs is expected, and that the rebuild belongs in a named private helper so the duplication is visible |

---

## Patterns to Capture

Patterns or conventions observed during this session that aren't yet documented. Candidates for
adding to docs or skills.

- **Pattern:** Data-assembly step whose iteration source is a *fetched* DTO, not a caller argument
  **Observed in:** `microservices/quizanswer/coordination/sagas/CreateQuizAnswerFunctionalitySagas.java`
  **Description:** `getQuestionsStep` loops over `this.quizDto.getQuestions()`, populated by the earlier `getQuizStep`, so it declares that step as a dependency rather than reading a caller-supplied id list. `sagas.md` § "Collection-valued data-assembly step" describes the loop shape but shows only the caller-supplied-ids form; the fetched-source variant adds the dependency edge and is what makes a single loop step mandatory (the element count is unknown until the saga runs).

- **Pattern:** A P3 equality guard between two saga-assembled DTOs, rather than a DTO and a scalar
  **Observed in:** `microservices/quizanswer/service/QuizAnswerService.java` (`createQuizAnswer`)
  **Description:** `COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION` compares `quizDto.getExecutionAggregateId()` against `executionDto.getAggregateId()`. plan.md phrases the rule against "the supplied `executionAggregateId`"; since the same id seeded the `getExecutionStep` fetch, the two are equal by construction and the DTO form avoids a redundant scalar parameter on the command. Worth a line in `service.md` § P3 Guard Placement so the next agent does not add the scalar back.

- **Pattern:** Fixture helper for an upstream aggregate whose *sub-entity* state a downstream write needs
  **Observed in:** `QuizzesFull2SpockTest.groovy` (`createQuestionWithOptions`)
  **Description:** session `b`'s `createQuestion` helper builds a minimal question with no options, which is the right default for its own tests. `AnswerQuestion` cannot be exercised against it, because the correctness rule reads a `correctOptionKey` cached from the question's correct option. The resolution is a *sibling* helper rather than widening session `b`'s signature — session-c.md protects that signature, and adding a defaulted parameter to a foreign aggregate's helper would put the fixture for one aggregate's rule inside another aggregate's helper.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 53

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 53 | 2 | deferred | - |

Rows whose outcome is `declined` or `deferred` have no sha - write `-`.

Row 53 is `deferred`, not `fixed`: the human settled the domain question (add the guard) so the
implementation could proceed, but no `docs/` or `.claude/skills/` edit was made, so the underlying
harness gap — no instruction for a spec-silent input — is still open and needs the wording changes
proposed above.

---

## One-Line Summary

The session's only real friction was a spec-silent input rather than a harness contradiction:
`AnswerQuestion` can name a question the `QuizAnswer` never seeded, plan.md §7 states no rule for it,
and session-c.md has no branch telling an agent that is a Type 2 halt rather than a judgement call.
