# Master merge handoff

- **State:** complete for the upstream-integration prerequisite; benchmark package/evidence regeneration has not started.
- **Merge commit:** `45fcdb81a130c4df99418c5060468753b70b6fcd` (`Merge origin/master into fault-analysis/scenarios`). Parents: feature head `65934edea1c2d421bfcffbfb2ec3aac683f5aaf0` and fetched `origin/master` `e2482d68c567cd26ed41eebbb08271fdcde2af16`. This is one normal merge commit, not a rebase. Nothing was pushed.
- **Pre-merge safety:** `/tmp/microservices-simulator-pre-master-merge-20260816-161943/SHA256SUMS` passed. Before committing, the current unstaged patch, every backed-up untracked file, and `../ROADMAP.md` were byte-compared with the backup and matched.

## Conflict resolution

Three paths conflicted:

- `AGENTS.md` (`AA`): retained the verifier/module/canonical-document/workflow/safety guidance and added only compatible master guidance for explicitly requested application-generation work, profile-agnostic services, local module instructions, and build entry points. Master’s harness-specific autonomous commit policy was not made repository-wide and no verifier gate was weakened.
- `CLAUDE.md` (`UU`): retained the verifier canonical-context pointers and explicit statement that the historical application-generation workflow is not the default; also retained master’s `@AGENTS.md` include.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` (`UU`): normal `execute()` now repeatedly schedules dependency-ready futures for arbitrary depth and plan order, while every actual step still uses this branch’s assigned-fault injection, dynamic-evidence lifecycle, null-safe tracing/delays, and async completion handling. `executeSteps`, `executeStepForExecutor`, `executeUntilStep`, and `resume` remain.

The auto-merged `WorkflowFunctionality` retains master’s `SimulatorException` unwrapping and all branch executor-control APIs. Master’s typed `SagaStateConverter`, explicit `@Convert` mappings, typed repository queries, persistence tests, generated templates, `MessagingObjectMapperProvider`, and other nonconflicting content were accepted. The converter, messaging provider, persistence tests, and Saga templates are byte-identical to `origin/master` in the merge tree.

Integration-only test changes committed with the merge:

- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlanDynamicEvidenceTest.java`: added one reverse-ordered, four-level dependency test proving prerequisites run through dynamic instrumentation before the assigned target fault and later work does not run.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderTest.java`: updated the legacy CSV-fault assertion for master’s dependency-chained future semantics; it still proves the cause is `SimulatorException`, not `FaultVectorInjectedFaultException`.

The staged merge contained 1,074 master/integration paths. Inspection before commit found no benchmark issue, handoff, verifier canonical-doc, descriptor, runner, or script path staged.

## Validation

Passed:

1. `cd simulator && mvn -Dtest=ExecutionPlanDynamicEvidenceTest,FaultVectorProviderTest,SagaExecutorControlTest,WorkflowExecutionPlanTest,SagaStatePersistenceTest test` — 36 tests after the focused integration fix.
2. `cd simulator && mvn install` — full simulator module, 119 tests, and installed the merged `3.2.0-SNAPSHOT` for dependent modules.
3. `cd verifiers && mvn test` — 634 tests. The logged OpenTelemetry exporter connection refusal is non-fatal test noise; Maven succeeded.
4. `cd applications/quizzes && mvn -Ptest-sagas -Dtest=SagaStatePersistenceTest,QuizAnswerEventHandlingTest,AddParticipantTest,RemoveTournamentTest test` — 13 tests. This includes the three real-H2 persistence-context reload cases for generic `NOT_IN_SAGA`, generic `IN_SAGA`, and Quizzes-specific `CourseExecutionSagaState.READ_COURSE`, plus focused local Saga/event messaging paths.
5. Conflict-marker scan, conflict-index check, merge-resolution `git diff --check`, exact-master checks for converter/messaging/templates/persistence tests, and preservation checks for pre-merge dirty work.

Observed and retained failures:

- The first 36-test simulator integration run had one failure: `FaultVectorProviderTest.legacyCsvFaultStillAppliesWhenNoProviderIsActive` expected a synchronous throw. Master’s scheduler returns that failure through the execution future. The test was corrected without changing runtime semantics; the rerun and full simulator suite passed.
- A broader 39-test Quizzes command that also included the dirty benchmark runner/provider had one failure: `QuizzesStaleReadPrerequisiteProviderTest` still expects raw `NOT_IN_SAGA`, while merged master correctly persists `pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState:NOT_IN_SAGA`. This is the expected pre-merge benchmark-observer incompatibility, not an upstream runtime failure. It remains uncommitted for the next benchmark step; no evidence was regenerated.
- A whole staged-merge whitespace check reports whitespace already present in accepted master harness/review files. The five manually resolved/integration paths are whitespace-clean. Unrelated upstream whitespace was not rewritten.

The full Quizzes suite was not run; the requested converter/messaging/Saga paths were run narrowly. No verifier architecture change was needed.

## Remaining risks and next benchmark steps

The upstream prerequisite is complete, so benchmark work may resume after this handoff. Before measured evidence:

1. Update the uncommitted Quizzes test-side persisted-state observer and its tests to retain the raw typed value but decode/validate it through `SagaStateConverter`; do not compare the raw column with the obsolete untyped string.
2. Rerun the focused benchmark/provider tests and require explicit reloaded `GenericSagaState.NOT_IN_SAGA`.
3. Generate a new package from merge revision `45fcdb81a130c4df99418c5060468753b70b6fcd`; do not reuse the pre-merge package or runtime evidence.
4. Repeat M1 preflight, M2 target/control, package immutability checks, and only then the M3 landscape/review.

No package generation, Docker benchmark attempt, evidence refresh, feature commit, or push occurred in this prerequisite.
