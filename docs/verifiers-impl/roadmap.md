# Verifier roadmap

Last updated: 2026-08-28

[`current-state.md`](current-state.md) describes what exists now and the evidence behind it. This roadmap describes the remaining outcomes, why they matter to the thesis, their dependencies, and what would count as done. It deliberately avoids repeating current metrics and commands.

## End goal

The thesis direction is a reproducible pipeline that can:

1. derive meaningful Saga workloads from application source and tests;
2. persist concrete fault experiments;
3. execute those experiments repeatedly under a controlled runtime;
4. measure application-independent evidence of harmful behavior;
5. search and prioritize the experiment space under a finite budget.

The current implementation reaches a bounded Saga/local version of steps 1–4. The main bottleneck is no longer producing more catalog rows. It is producing **representative, setup-ready, repeatably executable scenarios whose outcomes provide useful impact variation**.

## Priority order

```text
input quality and executable harmful scenarios
  -> repeatable execution boundary
  -> useful impact signal
  -> local fault-vector search
  -> cross-workload prioritization
```

Do not start search or prioritization while generated scenarios are mostly blocked, cannot be reset reliably, or produce flat impact values.

## Outcome 1 — Improve useful input coverage

**Status:** The source-derived shared-setup vertical slice is complete for one representative Remove/Add fixture shape; broader input families remain outcome work.

### Goal

Increase the number and variety of generated WorkloadPlans that can pass runtime setup without weakening the truthfulness of the static setup gate.

### Why it matters

Static Saga discovery alone does not create executable experiments. Every useful experiment needs an exact persisted input tuple. Missing or fabricated values make schedule and fault-space counts irrelevant to runtime evaluation.

### Current entry point

The current handbook records the supported recipe model, the latest setup-ready baseline, and the remaining Sagas without accepted inputs. Known blocker families include unsupported local transforms, unresolved helper/property values, and event payload placeholders.

### Work direction

- Classify the remaining Sagas without accepted inputs by concrete missing pattern rather than adding broad heuristics.
- Add generic recipe support only for patterns observed in dummyapp and justified by a realistic Quizzes case.
- Prioritize patterns that unlock a representative harmful interaction or materially broaden Saga shapes, not patterns that only increase the accepted-input headline.
- Keep source-mode filtering conservative and application-independent.
- Preserve exact argument types and ordered mutations through helper/facade boundaries.

### Done when

- each targeted input family has dummyapp-first positive and negative coverage;
- the static setup gate does not gain false positives in the representative runtime;
- a refreshed Quizzes package and preflight show which additional exact workloads became setup-ready;
- the result explains why those workloads matter to later execution/impact evaluation.

## Outcome 2 — Make a harmful generated interaction executable

**Status: complete (2026-08-01).** The persisted Quizzes event-consequence positive/control pair now provides repeatable ImpactV1 discrimination. See [`current-state.md`](current-state.md#quizzes-event-consequence-replay-positive-control-and-masking). Further event breadth is not part of this outcome.

### Goal

Persist and replay at least one realistic multi-Saga or Saga/event interaction that produces a non-zero generic impact signal under a controlled fault schedule.

### Why it matters

The generic invariant detector needed a persisted scenario with enough prerequisite and event structure to reproduce the real Quizzes stale-read interaction. That gap is now closed for one conservative unique local event route; broader event patterns remain deferred.

### Work direction

- Choose one existing realistic harmful Quizzes interaction as the target oracle.
- Identify the smallest missing representation or materialization capability between its test setup and a persisted WorkloadPlan/FaultScenario.
- Extend the generic model only for that proven gap; do not hardcode the Quizzes feature.
- Preserve deterministic action identity and explicit evidence for every prerequisite.
- Demonstrate a negative/control scenario alongside the positive interaction.

### Done when

- the interaction is represented by a valid v4 package and exact persisted FaultScenario id;
- setup succeeds in the supported runtime;
- repeated execution produces the expected conformance boundary and non-zero generic impact finding;
- a nearby control produces zero impact;
- package bytes remain unchanged by execution;
- the claim does not depend on parsing application-specific log text.

## Outcome 3 — Define repeatable execution and reset

**Status: complete for the supported boundary (2026-08-01).** Isolation is one fresh ScenarioExecutor container/process with a fresh H2 database per attempt; same-process reset is unsupported. Repeated positive/control evidence is linked from [`current-state.md`](current-state.md#quizzes-event-consequence-replay-positive-control-and-masking).

### Goal

Make repeated attempts comparable by defining who resets persistent application state and what constitutes an isolated execution environment.

### Why it matters

Fault-vector search assumes that reward differences come from the vector/scenario rather than residue from earlier attempts. The supported reset owner is the caller/orchestrator, which must create a fresh process/container and H2 database for each attempt. The executor does not claim same-process cleanup.

### Work direction

- Define the supported reset boundary for the chosen runtime profile before implementing batch execution.
- Prefer a simple process/container/database reset contract over application-specific cleanup APIs.
- Record reset identity and failure in execution orchestration evidence.
- Separate package immutability from environment reset: both are required, but they solve different problems.
- Prove deterministic replay under repeated all-zero and selected-fault controls.

### Done when

- the same persisted FaultScenario can be executed repeatedly from equivalent initial state;
- reset failure prevents impact evaluation rather than producing a misleading score;
- repeated controls have stable terminal/conformance and impact results within the defined boundary;
- the reset mechanism remains application-agnostic for the supported Saga/local profile.

## Outcome 4 — Characterize one harmful workload before broadening impact

**Status: complete (2026-08-16).** After merging master’s typed Saga-state persistence, a fresh v4 package, target/control repetitions, and all 34 retained schedules pass exact converter-decoded `GenericSagaState.NOT_IN_SAGA` proof. Landscape schema v2 records 19 bounded harmful rows and 15 no-broken-reference rows while ImpactV1 remains zero throughout. See [`current-state.md`](current-state.md#historical-prerequisite-backed-removetournamentaddparticipant-benchmark).

### Goal

Turn one already demonstrated harmful Quizzes interaction into a persisted, generated, repeatably executable benchmark. Run its bounded fault and recovery space and record execution validity, existing ImpactV1 evidence, and the known final-state condition separately.

Use that result landscape to decide the smallest broader impact contract and later GA reward. Do not choose weights, category ordering, or a general application-check framework before this evidence exists.

### Why it matters

ImpactV1 observes thrown aggregate-invariant rejections, but the exploratory Quizzes tests already prove repeatable harmful final states that it misses. The next uncertainty is not whether those states exist. It is whether the normal generator and executor can reproduce one of them, which fault scenarios produce it, and whether the observed landscape is useful for search.

### First experiment

- reuse the existing `RemoveTournament` and `AddParticipant` recovery-window evidence;
- identify only the missing generation, shared-input, recovery, execution, and final-state observation capabilities;
- persist the exact workload and its bounded FaultScenarios through the normal package path;
- replay a known harmful case and a safe control from fresh state;
- produce a simple per-scenario table rather than a premature combined score.

The final-state rule is applied once across executions: an active Tournament referring to its deleted Quiz is harmful for this experiment. Setup or infrastructure failure remains not evaluated. Existing invariant rejections stay separately visible.

The completed landscape shows useful final-state variation that ImpactV1 does not detect: all schedules for `00100`, `00101`, and `00110` satisfy the bounded harmful rule, while all other canonical rows do not, yet every ImpactV1 score is zero. The next decision is the minimum broader versioned impact contract that keeps invariant and final-state evidence separately inspectable. GA reward design remains downstream; no weights or category ordering are implied by these labels.

### Done when

- the selected interaction is a valid persisted WorkloadPlan with executable FaultScenarios rather than only a hand-driven test;
- repeated execution reproduces the known harmful final state and a safe control;
- the bounded fault/recovery landscape records valid, harmful-for-this-rule, safe-for-this-rule, and not-evaluated outcomes without manual per-scenario labels;
- package, scenario, execution, and result identities are reproducible;
- the evidence supports a concrete decision about the minimum later impact contract and whether a GA reward has useful variation.

## Outcome 5 — Refresh dynamic attribution only for a concrete need

### Goal

Use dynamic evidence when it can resolve a specific static-identity or coverage problem that blocks useful generation or interpretation.

### Why it matters

Dynamic enrichment is expensive and produces many raw artifacts. Historical broad exact/high-confidence counts showed that attribution can work, but those counts are not themselves execution or impact progress.

### Work direction

- First identify a current latest-package workload whose usefulness depends on unresolved runtime identity.
- Run a bounded latest-package enrichment baseline with explicit memory, profile, and test selection.
- Prefer direct input ids and structured simulator events over fuzzy value/name matching.
- Treat ambiguous, unmatched, and not-covered results as evidence boundaries, not metrics to optimize blindly.
- Do not let dynamic evidence rewrite package identity or persisted actions.

### Done when

- the run answers a named generation/execution question;
- package bytes remain unchanged;
- attribution improvement is measured against a controlled before/after case;
- raw artifacts and runtime cost are justified by information that changes a decision.

A broad Quizzes v5 refresh is not automatically a higher priority than the reduced generic impact contract.

## Outcome 6 — Local fault-vector search

### Goal

For one fixed useful WorkloadPlan, search persisted fault vectors under a finite execution budget and return high-impact distinct experiments.

### Entry conditions

Do not start until:

- the chosen workload is setup-ready and repeatably executable;
- environment reset is defined;
- at least one vector produces a discriminating evaluated result under an approved versioned impact contract; the automatic source-derived benchmark's final-state rule is evidence for designing that contract, not yet the generic search reward;
- arbitrary valid vectors can be persisted idempotently through the v4 on-demand path;
- invalid/infrastructure attempts cannot masquerade as zero fitness.

### Minimal design boundary

- Search unit: one fixed WorkloadPlan.
- Candidate identity: persisted FaultScenario/vector identity.
- Reward: one explicitly versioned impact result.
- Duplicate policy: never spend budget re-evaluating the same deterministic candidate unless repeatability is itself under test.
- Seed and budget: explicit and recorded.
- Baseline: compare against eager all-zero/single-point vectors and a deterministic random or exhaustive bounded baseline before claiming benefit from a genetic algorithm.

Do not commit prematurely to tournament selection, crossover, mutation, or population parameters. Choose an algorithm only after the executable vector space and reward distribution are measured.

### Done when

- a bounded search run is reproducible from package id, seed, budget, and runtime configuration;
- every evaluated reward links to a valid execution and impact report;
- duplicate and invalid-attempt accounting is explicit;
- the selected method outperforms or usefully differs from the declared baseline on a representative workload.

## Outcome 7 — Prioritize across workloads

### Goal

Allocate execution/search budget across multiple useful WorkloadPlans using their structural context and observed rewards.

### Entry conditions

Do not start until several workloads:

- are repeatably executable;
- expose meaningful structural differences;
- have non-flat, comparable impact rewards;
- support a stable local-search/evaluation cost.

### Work direction

- Define workload-level context from existing package facts such as participant count, step/fault-slot count, conflict evidence, aggregate footprint, and setup history.
- Establish simple baselines first: random, deterministic structural ordering, and equal budget.
- Use a bandit/contextual method only if shared context measurably improves discovery under budget.
- Keep workload selection separate from vector search so each result remains explainable.

### Done when

- the prioritizer's decision and observed reward are reproducible;
- comparison against simple baselines is fair and budget-matched;
- selected context features are current package facts, not application-specific labels;
- the result improves harmful-scenario discovery or reduces required executions on a representative workload set.

## Deferred breadth

The following may be valid later, but they are not prerequisites for the Saga/local thesis path unless evaluation scope changes:

- TCC-specific generation and execution;
- stream/gRPC/distributed runtime parity;
- true parallel execution;
- delay and non-binary impairments;
- compensation fault injection and retry/backoff policies;
- network-filesystem/multi-host package writers;
- generic support for every application framework pattern.

Broaden only when a thesis claim or representative scenario requires it.

## Roadmap decision rule

Before adding a new stage, metric, artifact, or abstraction, answer:

1. Which current thesis or user question does it answer?
2. Which decision changes because the result exists?
3. What is the representative positive and negative evidence?
4. Is the information already available from a current artifact?
5. Can an existing concept be deleted or reused instead?

If those answers are unclear, defer the addition.
