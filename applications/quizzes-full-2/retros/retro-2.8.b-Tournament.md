# Retro — 2.8.b — Tournament

**App:** quizzes-full-2
**Session:** 2.8.b (Read Functionalities)
**Date:** 2026-08-08

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/service/TournamentService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/messaging/TournamentCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/GetTournamentByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/GetTournamentsForExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/GetOpenedTournamentsForExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/sagas/GetClosedTournamentsForExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/GetTournamentByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/GetTournamentsForExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/GetOpenedTournamentsForExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/tournament/GetClosedTournamentsForExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/sagas/repositories/TournamentCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/tournament/TournamentServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/GetTournamentByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/GetTournamentsForExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/GetOpenedTournamentsForExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/tournament/GetClosedTournamentsForExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | Read in full at Step 3b before implementing; it is what routed the open/closed question to a halt rather than to a guess. |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Neutral domain, § Run the test suite | Yes | The "never pipe maven, redirect instead" rule mattered: the rtk hook mangled several `grep`/`cat` outputs this session, so the exit-status + surefire-report path was the only trustworthy verdict. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository - Latest-Active-Version Query | Yes | The closed five-item dependency list settled dropping both `TournamentRepository` and `AggregateIdGeneratorService` from a read-only service without a round trip. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Two-step read saga variant | Yes | The one-step vs two-step decision was unambiguous: `executionAggregateId` is stored on the aggregate's own `TournamentExecution` snapshot, so no foreign-id resolution step is needed. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Routing Commands | Yes | The "bulk read filtered by a non-PK field passes `null` as `rootAggregateId`" note covered all three list commands directly. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 - Service Test (incl. § Not-Found Paths), § T4 - Functionality Test, § Fake/Wrong/Weak, § Choosing Input Values | Yes | § Assertion Ownership kept the T4 tests to orchestration outcomes and pushed the field-level and empty-list cases into T2. The temporal-mechanics note is why the past-dated fixtures are pinned against `DateHandler.now()` rather than `LocalDateTime.now()`. |
| `docs/concepts/rule-enforcement-patterns.md` | § Decision Guide, Step 4 | Partial | Supplies an ordered default for plan.md silence on a *write* method's out-of-domain input, and explicitly scopes itself there. The read-side analogue does not exist - see Documentation Gaps and harness-log row 64. |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Partial | See Skill Instructions Feedback. |

---

## Skill Instructions Feedback

### What worked well

- § "Fixture state a create cannot reach" is the section that shaped this session most. Both the
  `cancelTournament` sibling helper and the decision *not* to widen `createTournament` with a
  `cancelled` parameter come straight from it, and it pre-empted the obvious wrong move of setting
  the flag inline inside `TournamentServiceTest`.
- The "foreign-aggregate-id parameters required and leading, own fields defaulted" contract produced a
  `createTournament` signature that is a prefix-match for the 2.8.c `CreateTournament` parameter list,
  so the swap should not touch a single call site.
- § Produce's "plan.md is a blueprint, not a manifest" plus § "Amend plan.md for omitted files"
  removed all hesitation about emitting the three repository files plan.md did not list.

### What was unclear or missing

- The `create{Aggregate}` fixture template builds the aggregate "directly", with every field from a
  domain constant. For an aggregate whose 2.N.c create saga seeds most of its state from *fetched
  upstream DTOs* - here `executionVersion`, `courseAggregateId`, and the creator's name, username and
  version - filling those from constants would make every read-side DTO assertion break on the 2.N.c
  swap, which is exactly what the signature contract exists to prevent. The fixture reads them off
  the real upstream aggregates instead. The skill neither prescribes nor forbids this; it was
  inferred from the contract's stated purpose.
- Relatedly, the template has no advice for a snapshot field the fixture genuinely *cannot* obtain
  because the aggregate that supplies it is created inside the 2.N.c saga itself - `quizAggregateId`
  and `quizVersion` here. `service.md` § Partial-Data Owned Entities covers the analogous case for
  service code (`null` plus a `TODO`), but a `null` here would be a fixture that constructs an
  aggregate no real create ever produces. A constant placeholder plus an inline comment was used.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-b.md` § "Update `{AppClass}SpockTest.groovy`": after
  "Only the aggregate's **own** fields get constant defaults", add that a *snapshot* field the 2.N.c
  create saga reads off a fetched upstream DTO is seeded in the fixture from that same upstream
  aggregate, not from a constant - the reason is the same one that fixes the parameter list, namely
  that the call sites and their assertions must survive the swap.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/rule-enforcement-patterns.md`, `.claude/skills/implement-aggregate/session-b.md` | No guidance for plan.md being silent about a **read** functionality's filter predicate. § Decision Guide Step 4 gives an ordered default for the write-side analogue and scopes itself to writes; § Spec-First Ordering covers plan.md *disagreeing* with the implementation, not being silent. | High — caused this session's only halt; recurs for any read named by an adjective the spec does not define. harness-log row 64, open. | Either extend § Decision Guide with a read-side step (fields the predicate may read are the aggregate's own; halt only when two fields give different partitions), or state in `session-b.md` that an undefined filter adjective is always a halt. The choice between the two is itself a design decision, same class as row 56. |
| `.claude/skills/implement-aggregate/session-b.md` | § "Update `{AppClass}SpockTest.groovy`" is silent on how the fixture seeds snapshot fields that the 2.N.c create saga obtains from fetched upstream DTOs, and on fields no fixture can obtain at all. | Medium — a constant-filled snapshot compiles and passes in session `b`, then breaks every DTO assertion in 2.N.c, which is the failure mode the signature contract was written to prevent. | Apply the wording change proposed above. |

---

## Patterns to Capture

- **Pattern:** Predicate-parameterised bulk read
  **Observed in:** `microservices/tournament/service/TournamentService.java`
  **Description:** Three list reads differing only in a selector over the loaded aggregate share one
  private `findForExecution(executionAggregateId, Predicate<{Aggregate}>, unitOfWork)` and one JPQL
  latest-active-by-foreign-key query. The repository narrows by the stored foreign key; the selector
  runs in Java on the aggregate returned by `aggregateLoadAndRegisterRead`. Use when the extra filter
  reads a field the domain also stamps through a clock (`DateHandler.now()`) or a flag another
  session mutates - keeping it out of JPQL means the read and the invariants see the same clock, and
  one query serves every variant.

- **Pattern:** Session-`b` fixture mirrors the session-`c` saga's data assembly
  **Observed in:** `QuizzesFull2SpockTest.createTournament`
  **Description:** Where 2.N.c's create saga seeds owned snapshots from upstream DTOs it fetches, the
  session-`b` fixture fetches the same DTOs through the upstream coordinators rather than filling the
  snapshot from constants. The aggregate is still constructed directly, so the helper stays a
  fixture; but every field a read test asserts now holds the value the real create will produce.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 64

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 64 | 2 | deferred | - |

---

## One-Line Summary

The read side went in green on the documented patterns with no Type 1 friction, and its single halt
exposed that the harness has an ordered default for plan.md's silence on write-method inputs but
nothing at all for its silence on a read functionality's filter predicate.
