# Plan: persisted RemoveTournament–AddParticipant benchmark

## Environment and execution mode

- Documentation route: spec and plan.
- Execution mode: reviewed execution with one independent code-and-evidence review before completion.
- Isolation: implement directly on the current `fault-analysis/scenarios` branch; do not create a worktree.
- Repository safety: preserve unrelated changes and stop if concurrent work makes the checkout unsafe to edit or validate.
- Commits and push: the user authorized one dedicated merge commit integrating `origin/master`. Later feature commits and push remain unauthorized until separately requested.
- User verdict pause: none during implementation unless discovery requires a material change to the benchmark, package contract, candidate space, or final-state rule.

## Implementation strategy

First merge `origin/master` into the current branch without rebasing. Preserve current repository guidance where master’s generic harness guidance conflicts. Combine master’s dependency-ready automatic `ExecutionPlan` scheduler with this branch’s assigned-fault injection, dynamic evidence, asynchronous handling, and exact ScenarioExecutor controls. Accept the merged Saga-state converter, explicit entity mappings, typed repository queries, DSL templates, and database round-trip tests. Validate this integration before treating prior benchmark evidence as current.

Then make the smallest generic generation change: allow the existing prerequisite descriptor to select an exact source-derived multi-Saga workload without requiring an event consequence. Preserve the current event descriptor behavior and v4 package.

Use that capability to select the fixed five-step `RemoveTournament`–`AddParticipant` order and bind both participants to runtime IDs from Quizzes setup. Prefer the existing setup provider; add a separate Quizzes provider only if focused proof shows that reusing it changes the benchmark meaning.

Prove the package before building landscape tooling. The target `00100` vector is already an eager single-point vector, and the all-zero vector is already eager. Their persisted recovery schedules and IDs provide the first positive/control evidence.

Keep final-state observation outside generic verifier production code. A Quizzes test-side runner calls ScenarioExecutor with one persisted scenario, then reads Tournament and Quiz state before the Spring context closes. A thin orchestrator launches one fresh attempt at a time and combines the normal execution report, ImpactV1 report, and Quizzes observation.

After positive/control repeatability is established, persist the remaining canonical vectors through the existing on-demand path and execute every distinct persisted recovery order under the package cap. The resulting JSON is authoritative; CSV is derived for inspection.

## Approved upstream-integration prerequisite

Before resuming milestone evidence:

- preserve the current dirty benchmark work outside the merge commit;
- merge `origin/master` with a dedicated merge commit and no history rewrite;
- resolve `ExecutionPlan` by composing master’s repeated dependency scan with this branch’s instrumented execution path and retaining all executor-step APIs;
- retain both branches’ compatible `WorkflowFunctionality` behavior;
- preserve current repository guidance when generic master guidance conflicts;
- run the upstream Saga-state round-trip tests, simulator execution-plan/executor-control tests, the affected simulator suite, verifier suite, and focused Quizzes Saga tests;
- regenerate the benchmark package from the merged source revision and discard pre-merge runtime evidence as superseded.

A failure that requires verifier package, executor, recovery, or impact architecture changes is a new material scope delta. Ordinary merge fixes and test updates preserving both approved behaviors are autonomous.

## Milestones M1–M3

### M1 — Generate and preflight the exact benchmark package

#### Outcome and spec coverage

A normal Quizzes generation run writes one valid no-event `RemoveTournament`–`AddParticipant` WorkloadPlan with the exact five-slot order, shared typed setup bindings, eager all-zero and `00100` FaultScenarios, and deterministic recovery schedules. ScenarioExecutor preflight reports it setup-ready. Covers FR-1 through FR-12.

#### Change boundary

May change the prerequisite descriptor machine contract, its generator/validation code, generic dummyapp-style descriptor fixtures, the Quizzes descriptor, and Quizzes setup-provider tests or provider selection. May update package-generation tests and diagnostics. Do not change general schedule generation, fault semantics, package v4, Quizzes production behavior, or final-state observation yet.

#### Known anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/PrerequisiteScenarioGenerator.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/PrerequisiteScenarioGeneratorSpec.groovy`
- `applications/quizzes/src/test/resources/verifier-prerequisite-scenarios.json`
- `applications/quizzes/src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/executor/QuizzesStaleReadPrerequisiteProvider.java`
- `RemoveTournamentFunctionalitySagas` and `AddParticipantFunctionalitySagas`

These paths orient implementation; they are not a complete file allowlist.

#### Discovery / preflight

- Confirm the existing provider creates a participant-free Tournament and an enrolled user that `AddParticipant` may add.
- Confirm a no-event descriptor selects exactly one base WorkloadPlan and does not accidentally select event-expanded variants.
- Confirm the generated compensation order for `00100` includes the real `getTournamentStep` rollback and identify any conservative no-op recovery action.
- Confirm exact expected workload and FaultScenario counts before freezing assertions.

If the existing provider's baseline is wrong, use a separate Quizzes benchmark provider without changing the generic provider interface. If the descriptor cannot distinguish base and event-expanded workloads cleanly, stop before broadening event generation.

#### Implementation strategy

- Version the descriptor schema if required to represent an optional event route unambiguously.
- Require either a complete exact event route or an explicit no-event selection; reject partial event fields.
- Keep participant binding, exact forward-order filtering, deterministic identity, and expected-count checks.
- Add the benchmark descriptor with shared Tournament, CourseExecution, and user bindings.
- Preserve existing event-consequence descriptor output exactly under the new reader/shape.
- Generate a fresh Quizzes package, locate the benchmark WorkloadPlan by its exact participant/order facts, and run setup preflight.

#### Proof before continuing

- Focused generic tests cover event and no-event descriptors, malformed mixed fields, exact counts, stable IDs, typed bindings, and no-event selection.
- Existing event descriptor tests still pass with unchanged semantic output.
- The generated benchmark WorkloadPlan has exactly two participants, five forward slots in FR-3 order, no event consequence, and a prerequisite baseline.
- Its inputs are statically setup candidates and runtime preflight is `SETUP_READY` for both participants.
- Eager output includes all-zero and `00100`; the target recovery set contains immediate recovery before `AddParticipant` continues.
- The five package files validate and their initial hashes are recorded.

Run focused verifier Maven tests first, then the affected verifier suite if the shared descriptor reader changes broadly. Use Docker generation and preflight for the real Quizzes proof.

### M2 — Replay the known harmful case and control

#### Outcome and spec coverage

A Quizzes benchmark runner executes persisted scenarios through ScenarioExecutor and records the known final-state rule. The `00100` immediate-recovery scenario repeatedly produces the active-Tournament-to-deleted-Quiz state with ImpactV1 zero, while a nearby persisted control repeatedly lacks that broken reference. Covers FR-13 through FR-24.

#### Change boundary

May add Quizzes test/evaluation runner code, additive observation/result records, focused tests, and orchestration needed for one fresh process per attempt. May make a narrow testability change to expose already public ScenarioExecutor execution inside a Quizzes Spring context. Do not add a generic final-state observer, change ImpactV1, change execution semantics, or modify the exploratory tests.

#### Known anchors

- `ScenarioExecutor` public execution API and reports
- `ScenarioExecutorCli` Spring-context lifecycle
- the benchmark's Quizzes setup provider
- `RemoveTournamentAddParticipantRecoveryWindowExploratoryTest.groovy`
- Tournament lookup and `SagaUnitOfWorkService.aggregateDeletedLoad`

#### Discovery / preflight

Before adding code, choose the smallest way for Quizzes test/evaluation code to call ScenarioExecutor and inspect state in the same context. Prefer composition around the public executor API. Do not add a generic extension interface merely to mirror one Quizzes assertion.

Confirm the all-zero persisted scenario completes through the supported domain-failure fallback and produces a stable no-broken-reference state. If it does not, select the nearest persisted canonical vector that gives a valid control and record why.

#### Implementation strategy

- Run one selected persisted FaultScenario in a Quizzes Spring/Saga/local context.
- After ScenarioExecutor returns, obtain the benchmark Tournament and referenced Quiz identities from setup/runtime evidence available to the Quizzes runner.
- Read final state without invoking repair or further business operations.
- Write one additive observation linked to the execution and scenario IDs.
- Combine execution, ImpactV1, and final-state facts without calculating a score.
- Run the known harmful scenario and control three times each in fresh processes/databases.

#### Proof before continuing

- The runner rejects missing/mismatched package, scenario, execution, or setup identities rather than guessing.
- `00100` with immediate recovery has the persisted action order expected by the package and completes in an ImpactV1-evaluable terminal state.
- Each harmful repetition records active Tournament, deleted referenced Quiz, broken reference, and ImpactV1 zero.
- Each control repetition is evaluable and records no broken reference.
- Attempt IDs differ while package, WorkloadPlan, FaultScenario, vector, and observation rule remain fixed.
- Setup or observation failure becomes `NOT_EVALUATED`.
- The benchmark observation does not change final state, execution report, ImpactV1 report, or package bytes.
- The exploratory tests remain unchanged.

Run focused Quizzes tests for the runner and baseline rule. Use fresh Docker processes for the six recorded positive/control attempts and compare package hashes afterward.

### M3 — Characterize the bounded landscape and review the result

#### Outcome and spec coverage

All 12 canonical vectors are persisted, every distinct recovery order retained under the configured cap is executed once from fresh state, and one reproducible result dataset summarizes execution, ImpactV1, and broken-reference outcomes without a combined score. Documentation and independent review agree with the evidence. Covers FR-25 through FR-30 and completes the feature.

#### Change boundary

May add a bounded local orchestration script/command, result aggregation and validation, generated evidence under ignored target output, canonical documentation updates, and review fixes inside the approved boundary. Do not implement search algorithms, normalize the production package candidate space, or expand the final-state rule.

#### Known anchors

- `FaultScenarioRequestCli` and `OnDemandFaultScenarioService`
- recovery cap and scenario-space accounting in the v4 package
- ScenarioExecutor wrapper/container
- M2 benchmark observation output
- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- `../ROADMAP.md`

#### Implementation strategy

- Derive the 12 canonical vectors from the persisted five-slot participant ownership; do not maintain an unrelated handwritten scenario list.
- List the 20 excluded non-canonical vectors and the earlier same-participant fault that masks each one.
- Persist missing canonical vectors idempotently through the existing on-demand command.
- Read exact uncapped and persisted recovery counts from package accounting.
- Execute every distinct persisted FaultScenario for the canonical vectors, one fresh process/database per attempt.
- Validate and combine execution, ImpactV1, and observation records into authoritative JSON and derived CSV.
- Summarize counts without ranking: evaluable/not evaluated, ImpactV1 zero/non-zero, broken reference present/absent, and combinations by vector/recovery order.
- Update canonical docs with commands, pinned identities, results, and limits only after evidence exists.
- Request an independent review focused on package truth, vector inclusion/exclusion, reset isolation, result joins, overclaiming, and accidental impact/GA scope.

#### Proof before completion

- Exactly 12 canonical vectors are in scope and 20 masked variants are explained without execution.
- On-demand persistence is idempotent and package validation passes after all additions.
- Executed rows equal the sum of persisted recovery schedules for the 12 vectors; capped and uncapped counts remain distinct.
- Every row links to one valid execution report, ImpactV1 report, and benchmark observation for the same attempt and scenario.
- Duplicate scenario IDs or attempt rows fail aggregation.
- No setup/infrastructure/observation failure is counted as no broken reference.
- Package hashes change only during authorized on-demand persistence and remain unchanged during execution.
- JSON and CSV summaries agree.
- No combined score, category ordering, GA, random search, or workload allocation is introduced.
- Focused and affected-module tests pass; Docker logs and commands are retained.
- Independent review passes after any in-scope fixes.
- Current-state and roadmaps match shipped behavior and name the impact-contract decision as the next step.

## Validation strategy

Use four proof layers:

1. **Generic descriptor tests:** exact no-event selection, preserved event behavior, malformed input rejection, deterministic IDs, and typed bindings.
2. **Package proof:** fresh Quizzes generation, v4 validation, exact benchmark structure, eager target/control scenarios, accounting, and setup preflight.
3. **Execution proof:** focused Quizzes runner tests plus three fresh-process harmful/control repetitions with stable state and ImpactV1 observations.
4. **Landscape proof:** canonical-vector validation, idempotent on-demand persistence, one fresh execution per persisted recovery order, strict result joins, package immutability during execution, and independent review.

Run Maven from `verifiers/` and `applications/quizzes/` separately. Start with named focused tests. Broaden to affected module suites when shared production code changes. Use Docker for the evidence that must match the supported ScenarioExecutor environment.

Do not rerun the exploratory search. The existing exploratory tests establish the domain interaction; new runs prove only the persisted package and benchmark path.

## Risks and fallbacks

| Risk | Detection | Mitigation or fallback |
|---|---|---|
| Master’s automatic scheduler conflicts with current fault/evidence execution | Three-way merge and focused dependency-order, fault, evidence, and executor-control tests | Compose the scheduler with the existing instrumented step helper; do not select either whole file |
| Master’s messaging serialization change alters event replay | Existing event-consequence and verifier integration tests | Fix only concrete compatibility regressions; stop if event architecture must change |
| Typed Saga-state persistence changes benchmark raw evidence | Upstream round-trip tests plus fresh target/control replay | Validate the encoded value through the converter and regenerate all package/runtime evidence |
| Optional event fields make descriptor selection ambiguous | Generic exact-count and malformed-shape tests | Use an explicit versioned no-event/event shape and reject partial routes |
| Existing setup provider creates the wrong Tournament/user state | Focused provider assertions before package generation | Add one benchmark-specific Quizzes provider; do not weaken generic binding rules |
| Static recovery includes a no-op action absent from runtime checkpoint discovery | Inspect persisted and actual action outcomes for `00100` | Accept a truthful no-op only if execution remains exact and the real rollback occurs; otherwise stop and fix the narrow checkpoint mismatch |
| All-zero control does not complete as expected | First M2 control replay | Select the nearest evaluable persisted canonical control and record the reason |
| Final-state inspection pushes Quizzes logic into generic verifier code | Source review and boundary tests | Keep the runner and rule on the Quizzes test/evaluation side |
| Canonical-vector filtering hides real distinct behavior | Derive vectors from participant slot ownership and report all exclusions | Stop if a later assigned fault can be realized after an earlier same-participant fault under current executor semantics |
| Recovery cap hides part of the true landscape | Compare exact uncapped and persisted counts | Claim only the persisted bounded landscape; raise the cap only with explicit cost evidence and approval |
| Fresh-process landscape execution is too expensive | Record per-attempt time and projected total after M2 | Optimize shared build preparation, not state isolation; stop before reducing reset guarantees |
| Result aggregation joins the wrong attempts | Strict package/scenario/attempt identity checks | Fail closed on missing, duplicate, or mismatched records |
| Benchmark labels are overstated as global safety | Review terminology and summaries | Use `HARMFUL_FOR_RULE`, `NO_BROKEN_REFERENCE`, and `NOT_EVALUATED`; make the one-rule limit explicit |
