# Persisted RemoveTournament–AddParticipant benchmark

## What the benchmark is

The existing Quizzes exploratory test proves a useful harmful execution:

1. `RemoveTournament` reads a Tournament and deletes its Quiz.
2. A fault prevents `RemoveTournament` from deleting the Tournament.
3. Recovery releases the Tournament's Saga lock.
4. `AddParticipant` then updates the surviving Tournament.
5. The final state contains an active Tournament that still refers to the deleted Quiz.
6. ImpactV1 remains zero because no aggregate change was rejected.

Today this execution exists only as a hand-driven test. This feature moves it through the normal verifier pipeline.

A **benchmark workload** is the one fixed WorkloadPlan used for this experiment. A **benchmark observation** is the final Quizzes state recorded after ScenarioExecutor finishes. A **canonical fault vector** assigns at most one reachable fault to each Saga; it excludes later fault bits that would be masked after the same Saga has already failed.

The generator will derive the two Sagas and their steps from application source, select the exact benchmark order, and bind both Sagas to the same runtime-created Tournament. It will write normal v4 WorkloadPlan and FaultScenario records. ScenarioExecutor will replay those persisted records. A Quizzes test-side benchmark runner will inspect final state and produce a result table without introducing a general impact framework.

## Goals

1. Persist one exact source-derived `RemoveTournament`–`AddParticipant` WorkloadPlan in a normal v4 package.
2. Replay the known harmful fault scenario and a nearby non-broken control repeatedly from fresh state.
3. Characterize the 12 canonical fault vectors and every recovery order persisted for them under the package's existing recovery cap.
4. Record execution validity, schedule conformance, ImpactV1, and the known final-state condition separately.
5. Produce enough evidence to decide the minimum later impact contract and whether this workload has useful variation for local search.

## Non-goals

- Implementing a genetic algorithm, random search, or workload allocator.
- Defining a general impact model, category system, weight, severity, or combined score.
- Automatically understanding arbitrary application state relationships.
- Adding a general application-check or result-observer framework to ScenarioExecutor.
- Hand-writing WorkloadPlan, FaultScenario, action, or deterministic IDs.
- Changing Quizzes domain rules to create the benchmark.
- Rebasing or otherwise rewriting the published `fault-analysis/scenarios` branch history.
- Modifying the three existing recovery-window exploratory tests.
- Supporting TCC, stream, gRPC, distributed, or truly parallel execution.
- Claiming that absence of this one broken reference proves the whole application is safe.

## Functional requirements

### Upstream runtime prerequisite

**FR-0.** Before benchmark evidence is regenerated, `origin/master` shall be merged into `fault-analysis/scenarios` without rewriting feature-branch history. The merge shall preserve both master’s dependency-driven automatic workflow scheduling and this branch’s assigned-fault injection, dynamic evidence, and exact ScenarioExecutor step controls.

The merged runtime shall use master’s versioned Saga-state persistence contract: each Saga state is stored with enough type information to reconstruct its exact enum value after a real database reload. Generic and Quizzes-specific states shall pass database round-trip tests. The target execution must persist and reload explicit `NOT_IN_SAGA`; SQL null is not accepted as an equivalent value.

### Generated package

A **prerequisite descriptor** is the existing small application file that selects an exact source-derived workload and names runtime values supplied during setup.

**FR-1.** The prerequisite descriptor contract shall support an exact multi-Saga workload that has no event consequence. Existing event-consequence descriptors shall continue to produce their current workloads.

**FR-2.** The benchmark descriptor shall select exactly these participants:

- `RemoveTournamentFunctionalitySagas`;
- `AddParticipantFunctionalitySagas`.

**FR-3.** The benchmark WorkloadPlan shall have this fixed forward order and fault-slot order:

1. `RemoveTournament.getTournamentStep`;
2. `RemoveTournament.removeQuizStep`;
3. `RemoveTournament.removeTournamentStep`;
4. `AddParticipant.getUserStep`;
5. `AddParticipant.addParticipantStep`.

**FR-4.** The workload shall contain no event-consequence action.

**FR-5.** Setup shall provide one shared Tournament ID to both Sagas, plus the CourseExecution and enrolled user IDs required by `AddParticipant`. The existing Quizzes setup provider may be reused when it supplies the exact required state.

**FR-6.** The generator shall derive Saga definitions, steps, fault slots, compensation checkpoints, and action identities from normal analysis. The descriptor may select the participants, forward order, and setup bindings, but shall not contain hand-written package records or action IDs.

**FR-7.** The generated WorkloadPlan, its inputs, and its FaultScenarios shall pass current v4 validation and package checksum rules.

**FR-8.** The known harmful vector shall be `00100`, meaning an assigned fault at `RemoveTournament.removeTournamentStep` and no other assigned fault.

**FR-9.** Recovery generation for `00100` shall persist the immediate-recovery ordering needed by the known interaction. A conservative recovery action that performs no runtime work may remain visible, but it shall not prevent the real `getTournamentStep` rollback or change the final harmful state.

### Setup and execution

**FR-10.** ScenarioExecutor preflight shall materialize both exact participants from the persisted inputs and report the workload setup-ready.

**FR-11.** Every measured attempt shall select one persisted FaultScenario by ID. No runtime vector overlay or hand-built action order is allowed.

**FR-12.** Each measured attempt shall start in a fresh process/container and fresh H2 state, using the existing reset boundary.

**FR-13.** The `00100` immediate-recovery scenario shall complete within the supported Saga/local boundary and leave:

- the Tournament active;
- the Tournament outside an active Saga;
- the referenced Quiz deleted;
- the Tournament still referring to that Quiz;
- ImpactV1 equal to zero.

**FR-14.** A nearby persisted control shall complete within the supported boundary without the broken Tournament-to-Quiz reference. The all-zero scenario is the preferred control if its current domain-failure fallback completes as expected; otherwise another persisted vector from the same WorkloadPlan shall be used and the reason recorded.

**FR-15.** The known harmful scenario and selected control shall each be repeated at least three times. Their execution, conformance, ImpactV1, and final-state observations shall be stable within the declared fresh-process boundary.

**FR-16.** Execution shall not change the five semantic package files.

### Final-state observation

**FR-17.** A Quizzes test-side benchmark runner shall invoke the real ScenarioExecutor with the persisted package and scenario ID, then inspect Quizzes state in the same application context before it closes.

**FR-18.** The benchmark's one final-state rule shall be:

> An evaluated execution is harmful for this benchmark when an active Tournament still refers to a deleted Quiz.

**FR-19.** The runner shall not manually assign an expected result to each FaultScenario. It shall apply the same rule automatically to every attempt.

**FR-20.** A result shall be `HARMFUL_FOR_RULE` only when measured execution completed in an impact-evaluable terminal state, final-state observation completed, and the broken reference exists.

**FR-21.** An impact-evaluable execution without the broken reference shall be `NO_BROKEN_REFERENCE`. This label shall not be described as proof of global application safety.

**FR-22.** Setup, selection, executor infrastructure, observation, or report-write failure shall produce `NOT_EVALUATED`, not a harmless result. Any partial observation may remain diagnostic evidence but shall not change that classification.

**FR-23.** The result artifact shall record at least:

- package, WorkloadPlan, FaultScenario, and execution-attempt IDs;
- assigned vector and persisted action order;
- execution terminal status and schedule conformance;
- ImpactV1 evaluation status, finding count, and score;
- Tournament ID and state when observed;
- referenced Quiz ID and state when observed;
- broken-reference result and reason;
- recovery schedule identity or exact persisted action order;
- repetition number and runtime/reset context.

**FR-24.** The result artifact shall live outside the five-file semantic package and shall not rewrite execution or ImpactV1 reports.

### Bounded landscape

**FR-25.** The characterization set shall contain the 12 canonical vectors formed by:

- `RemoveTournament`: no fault, or first fault at one of its three slots;
- `AddParticipant`: no fault, or first fault at one of its two slots.

The Cartesian product is `4 × 3 = 12` vectors.

**FR-26.** Vectors assigning more than one fault to the same Saga shall be listed as excluded masked variants rather than executed as distinct benchmark candidates.

**FR-27.** The existing on-demand package path shall persist the canonical vectors not already generated eagerly. Persistence shall remain deterministic and idempotent.

**FR-28.** Every distinct FaultScenario persisted for the 12 vectors under the configured recovery cap shall be executed once for landscape characterization. Repetition beyond the known harmful/control pair is not required in this iteration.

**FR-29.** When recovery schedules are capped, the result shall report both the exact uncapped count and the number persisted and executed. It shall not claim full uncapped enumeration.

**FR-30.** The final table shall preserve one row per execution attempt and support summaries by vector, recovery order, execution status, ImpactV1, and broken-reference result. It shall contain no combined fitness score.

## Architecture

The implementation first merges `origin/master`, which already contains the accepted Saga-state persistence repair and its round-trip tests. The one code conflict combines master’s repeated dependency-ready workflow scheduling with this branch’s instrumented step execution and ScenarioExecutor-specific controls; neither behavior may replace the other.

The static visitor and adapter pipeline continues to discover the two Saga definitions. The prerequisite descriptor receives one narrow extension: it may select either an exact event workload or an exact workload with no event. In both cases it still selects source-derived participants and forward order and replaces runtime-owned constructor values with typed setup bindings.

The existing Quizzes setup provider is reused if focused proof confirms that its Tournament starts without participants and its enrolled user can be added by `AddParticipant`. If that assumption is false, Quizzes may add a benchmark-specific setup provider; the generic provider interface does not change.

ScenarioExecutor remains the execution authority. It reads the validated package, selects the persisted FaultScenario, injects assigned faults, and follows the persisted action order. No final-state rule is added to generic verifier production code.

The Quizzes benchmark runner lives on the application test/evaluation side. It starts or receives the Quizzes Spring context, calls ScenarioExecutor through its public API, queries the known Tournament and Quiz, and writes an additive benchmark observation. A small orchestration command runs one fresh process per persisted scenario and combines the outputs into the result table.

## Data model

The semantic package remains v4 and gains no benchmark-only field.

The merged Saga-state persistence format is master’s existing typed value, `fully.qualified.EnumClass:ENUM_VALUE`. Fresh H2 state requires no migration. Benchmark observation may retain the raw column value for proof, but it shall validate it through the merged persistence contract rather than compare it to the obsolete untyped string format.

The prerequisite descriptor shall clearly distinguish an event workload from a no-event workload. If its machine schema changes, it shall use a new schema version and reject malformed partial event selections rather than guessing.

The benchmark result is an additive evaluation artifact. JSON is the authoritative format because it preserves nested execution and observation details. A CSV table may be derived for inspection and thesis analysis. Neither artifact participates in package identity.

A canonical vector contains five bits in the FR-3 slot order and at most one `1` within each participant's slots. FaultScenario identity remains the current deterministic identity derived from WorkloadPlan, vector, and action order.

## Security model

This feature adds no production or remote trust boundary. The Quizzes benchmark runner and setup provider are trusted test/evaluation code. They may create baseline data and read final state, but final-state observation shall not invoke new business actions or repair the measured state.

## Operating

Generation and preflight use the existing verifier and ScenarioExecutor commands. Canonical multi-fault vectors use the existing on-demand persistence command. Attempts use the current fresh-process/container reset rule.

The benchmark shall pin the generated package, configured recovery cap, scenario IDs, source revision, runtime profile, and commands used. Expensive rebuild work may be shared outside measured execution, but application state and process/database state may not be shared between measured attempts.

Rollback requires no package migration: remove the new no-event descriptor entry and use the previous generator build. Existing event-consequence packages and saved evidence remain valid for their recorded source revision.

Canonical current-state documentation shall be updated only after the generated package and execution evidence exist.

## Future roadmap

The result landscape will drive the next decision. A later issue may define a broader impact contract, choose a GA reward, normalize masked vectors in the search space, or add a reusable observation interface. None of those choices is made here.

After a reward is justified, local GA search can use this fixed WorkloadPlan and compare against exhaustive truth and budget-matched random search over the same declared candidate space.

## Open decisions

| # | Decision | Default | Alternatives | Impact |
|---|---|---|---|---|
| 1 | Authoritative landscape artifact | JSON with a derived CSV table | JSON only or CSV only | No change to measured behavior; JSON best preserves evidence while CSV is easier to inspect |
| 2 | Repetitions for the known harmful scenario and control | Three each | More repetitions if results are unstable | Three matches the established repeatability style without expanding every landscape row |
| 3 | Existing versus new Quizzes setup provider | Reuse the existing provider if its focused baseline proof passes | Add one benchmark-specific provider | Does not change the generic contract; a new provider is allowed only if the existing baseline is semantically wrong |

The user approved merging `origin/master` rather than porting only the Saga-state commits. The merge shall be a normal merge commit, not a rebase; later feature commits and push remain separately unauthorized.
