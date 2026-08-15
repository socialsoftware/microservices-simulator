# Retro — 2.8.c — Tournament

**App:** quizzes-full-2
**Session:** 2.8.c (Write Functionalities)
**Date:** 2026-08-08

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/service/TournamentService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/messaging/TournamentCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/webapi/TournamentController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/CreateTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/CancelTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/DeleteTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/CreateTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/AddParticipantCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/UpdateTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/CancelTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/DeleteTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/domain/QuizzesFull2DomainConstants.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/tournament/TournamentServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/CreateTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/AddParticipantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/UpdateTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/CancelTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/DeleteTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/CreateTournamentCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/AddParticipantCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/UpdateTournamentCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/CancelTournamentCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/DeleteTournamentCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/CreateTournamentCompensationTest/CreateTournamentFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/AddParticipantCompensationTest/AddParticipantFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/UpdateTournamentCompensationTest/UpdateTournamentFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/CancelTournamentCompensationTest/CancelTournamentFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/DeleteTournamentCompensationTest/DeleteTournamentFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/GetOpenedTournamentsForExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/GetClosedTournamentsForExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md`

### Application bug fixes (earlier-session files)

Not bug fixes, but edits to files owned by earlier sessions, each forced by this session:

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/service/QuizService.java` — `deleteQuiz` added (2.6.c reopened; `CreateTournamentFunctionalitySagas`' compensation has nothing to send otherwise)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quiz/DeleteQuizCommand.java` — new, same reason
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/messaging/QuizCommandHandler.java` — one routing case, same reason
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quiz/QuizServiceTest.groovy` — one T2 case for `deleteQuiz`

No files under `simulator/` were modified.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | The Type 1 / Type 2 split is what routed the question-selection contradiction to a unilateral fix and the four date/design questions to a halt. |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Neutral domain, § Run the test suite | Yes | The never-pipe-maven rule earned its keep again: `rg` is shadowed in this shell and several `grep`/`find` invocations were rewritten or rejected by the rtk hook, so exit status plus surefire reports were the only trustworthy verdict. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns (Create/Mutate), § Copy-on-Write Rule incl. sub-collection clearing, § DTO Immutability, § Exception-Throw Convention, § P3 Guard Placement | Yes | § Sub-collection clearing before `remove()` described `deleteTournament` exactly: clear participants on the copy, then `remove()`, so `TOURNAMENT_DELETE` sees an empty list. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 2), § Collection-valued data-assembly step, § Semantic-lock release on abort is automatic | Partial | Shape 2 is the exact template `CreateTournamentFunctionalitySagas` needed. Two `setForbiddenStates` snippets did not compile — harness-log row 70. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Routing Commands | Yes | The `rootAggregateId = null` rule for creates covered `CreateTournamentCommand`. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § Event Publication, § T4 — Functionality Test (incl. the unresolvable-aggregate exception), § Compensation Test, § Fake/Wrong/Weak, § Choosing Input Values | Partial | Everything about writing the tests was covered. What is not covered anywhere is how a test reaches a **time-gated** state — see Documentation Gaps and harness-log row 68. |
| `docs/concepts/rule-enforcement-patterns.md` | § Common Mistakes, § Decision Guide Step 4.3 | Yes | § Common Mistakes is why the shortfall guard sits in `TournamentService` and not in `selectQuestionsStep`, even though the saga is where the shortfall becomes knowable. |
| `.claude/skills/implement-aggregate/session-c.md` | whole file | Partial | See Skill Instructions Feedback. |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" pre-empted most of the swap's cost. Its warning that the
  replacement "calls a functionality that fetches the foreign aggregate and runs its P3 guards"
  named the failure mode before it happened, and its instruction to keep the signature and defaults
  fixed is what pushed the question-seeding into the helper body instead of into 8 call sites.
- The § Produce preamble's explicit note that plan.md is "a blueprint, not a manifest" made the
  Quiz-side delete trio an obvious *produce-and-amend*, not a scope question.
- § "One `{Op}Test.groovy` per write functionality (T4)"'s anti-pattern warning — do not read the
  service or the saga to decide what to assert — is the reason `DeleteTournamentTest` carries a
  documented omission rather than a `noExceptionThrown()` filler case.
- The compensation-test § CRITICAL gotcha about one CSV block per saga class per test file was
  correct and load-bearing: five separate `*CompensationTest` files, each with block 1 armed.

### What was unclear or missing

- § "Update `{AppClass}SpockTest.groovy`" covers a fixture constant that must be re-pinned **forward**
  of `DateHandler.now()`, which is 2.8.b's problem, but has nothing for a constant whose purpose is
  to be in the **past**. Four call sites in this session needed an elapsed tournament window, and the
  real create path cannot produce one. The section's escalation route (take it to the self-healing
  gate) was the right one and was used, but the harness offers no pattern for the resolution.
- § Produce lists the compensation-test pair under its own `###` heading with an applicability test,
  but the applicability test is stated only in terms of `setSemanticLock`. `CreateTournament` holds
  no lock and is therefore outside the mandated set, yet it is the only saga in this application with
  a genuine `registerCompensation` — the case the whole mechanism exists for. The applicability test
  reads as if lock-holding and compensation-holding were the same property; here they are disjoint.
  The pair was written anyway and is flagged below as beyond the mandated set.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-c.md` § "One `{Op}CompensationTest.groovy` per
  lock-holding write functionality (T4)": widen the applicability test to the union of "a
  `setSemanticLock` step with a dependent step after it" **and** "any step carrying a
  `registerCompensation`". The heading itself says "lock-holding", which is the narrower of the two.

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `AddParticipantFunctionalitySagas` | `getTournamentStep` | none (primary: `Tournament`, `IN_ADD_PARTICIPANT`) | `AddParticipantTest` — "addParticipant: getTournamentStep acquires IN_ADD_PARTICIPANT semantic lock" | Yes |
| `UpdateTournamentFunctionalitySagas` | `getTournamentStep` | none (primary: `Tournament`, `IN_UPDATE_TOURNAMENT`) | `UpdateTournamentTest` — "updateTournament: getTournamentStep acquires IN_UPDATE_TOURNAMENT semantic lock" | Yes |
| `CancelTournamentFunctionalitySagas` | `getTournamentStep` | none (primary: `Tournament`, `IN_CANCEL_TOURNAMENT`) | `CancelTournamentTest` — "cancelTournament: getTournamentStep acquires IN_CANCEL_TOURNAMENT semantic lock" | Yes |
| `DeleteTournamentFunctionalitySagas` | `getTournamentStep` | none (primary: `Tournament`, `IN_DELETE_TOURNAMENT`) | `DeleteTournamentTest` — "deleteTournament: getTournamentStep acquires IN_DELETE_TOURNAMENT semantic lock" | Yes |

`CreateTournamentFunctionalitySagas` contributes no row: it takes no semantic lock, as
plan.md §8 § Saga states states. `UpdateTournamentFunctionalitySagas`' `updateQuizStep` uses
`setForbiddenStates`, not `setSemanticLock`, so it is a guard transition — deferred by
`docs/concepts/testing.md` § Appendix — Cross-Functionality Test, not a row here.

No `Present? = No` rows.

**Compensation coverage, for the same call sites:** all four have a `{Op}CompensationTest` with an
armed CSV, and `CreateTournamentCompensationTest` was added on top of the mandated set because the
create saga carries the session's only real `registerCompensation` (the `DeleteQuizCommand` undo).
Every one of the five was run in both configurations — flag `1` (green, fault propagates,
`sagaStateOf` back to `NOT_IN_SAGA`, mutation not applied) and flag `0` (all five fail on the
missing `SimulatorException`, and the log shows `getTournamentStep` executing immediately before
each faulted step, and `createQuizStep` before `createTournamentStep`).

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` | No rule for reaching a **time-gated** aggregate state in a test. § Choosing Input Values covers pinning instants for boundary cases, and `session-c.md` covers re-pinning a fixture constant forward, but a read functionality partitioning on `now()` needs a fixture on the far side of that boundary, and where the create path forbids constructing one directly the only remaining route is to wait. | High | Add a short § "Reaching a time-gated state" naming the two cases: the state is constructible (pin the constant) versus it is only reachable by elapsed time (create the shortest legal window and poll the same clock the predicate reads). Cross-reference from `session-c.md` § "Update `{AppClass}SpockTest.groovy`". |
| `docs/concepts/sagas.md` | § Create Functionality Sagas assumes the compensating delete command for the created aggregate already exists. When the created aggregate's own session shipped no delete, the doc does not say who writes it. | Medium | One sentence in item 2: if the child aggregate exposes no delete, the create session adds the service method, command and handler case, and amends the child's plan.md row. |
| `.claude/skills/implement-aggregate/session-c.md` | The compensation-test applicability test is stated purely in terms of `setSemanticLock`, which misses a create saga whose `registerCompensation` is the only genuine undo in the application. | Medium | See Suggested wording changes above. |

---

## Patterns to Capture

- **Pattern:** Fixture helper stocks the prerequisites its own P3 guard demands
  **Observed in:** `QuizzesFull2SpockTest.createTournament`
  **Description:** After the session-`c` swap, the create functionality enforces a guard against
  upstream stock (here: the course must hold `numberOfQuestions` questions the tournament's topics
  cover). Putting the seeding inside the helper, keyed off its own parameters, keeps the signature
  and every existing call site unchanged; pushing it to the call sites would have edited eight
  `given:` blocks to say the same thing eight times. Applies whenever the swapped-in create path
  guards a countable upstream prerequisite the helper already knows the size of.

- **Pattern:** Reaching an elapsed-window state by waiting rather than by back-dating
  **Observed in:** `QuizzesFull2SpockTest.createClosedTournament`
  **Description:** Where an aggregate's create path structurally forbids a past timestamp — because a
  downstream aggregate created in the same saga orders that timestamp against its own creation clock
  — a test that needs the elapsed state creates the shortest legal future window and polls
  `DateHandler.now()` until it passes. Costs the window's length per call, so keep it to seconds and
  keep it in one helper. The alternative, a direct-on-aggregate fixture, hides the fact that
  production can never reach the state under test.

- **Pattern:** A containment filter stated by the rule, not an overlap filter
  **Observed in:** `CreateTournamentFunctionalitySagas.selectQuestionsStep`
  **Description:** When a P4b rule reads `A.members ⊇ members(B)`, the assembly step's filter must be
  the containment itself. An overlap filter ("shares at least one member") is the intuitive reading
  of the prose and admits exactly the elements the rule forbids. Read the predicate, not the
  sentence above it.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 65, 66, 67, 68, 69, 70.

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 65 | 2 | deferred | - |
| 66 | 2 | deferred | - |
| 67 | 2 | deferred | - |
| 68 | 2 | deferred | - |
| 69 | 1 | fixed | - (no harness artifact at fault; the fix is a plan.md row, in the application commit) |
| 70 | 1 | fixed | a17ec02fd |

---

## One-Line Summary

Binding a `Quiz` creation into `CreateTournament` made two things impossible that the domain model
asserts are possible — a tournament drawn from an overlap-filtered question set, and a tournament
whose window has already closed — and both only became visible when the session-`c` swap pointed the
2.8.b read fixtures at the real create path.
