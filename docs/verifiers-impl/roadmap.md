# Verifier roadmap

Last updated: 2026-07-28

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

### Goal

Persist and replay at least one realistic multi-Saga or Saga/event interaction that produces a non-zero generic impact signal under a controlled fault schedule.

### Why it matters

The generic invariant detector already has a real Quizzes positive control, but the current generated action model cannot reproduce its prerequisite-heavy schedule. The available generated assigned-fault replay has impact zero. Search over a flat or unrepresentative fitness landscape has no thesis value.

### Work direction

- Choose one existing realistic harmful Quizzes interaction as the target oracle.
- Identify the smallest missing representation or materialization capability between its test setup and a persisted WorkloadPlan/FaultScenario.
- Extend the generic model only for that proven gap; do not hardcode the Quizzes feature.
- Preserve deterministic action identity and explicit evidence for every prerequisite.
- Demonstrate a negative/control scenario alongside the positive interaction.

### Done when

- the interaction is represented by a valid v3 package and exact persisted FaultScenario id;
- setup succeeds in the supported runtime;
- repeated execution produces the expected conformance boundary and non-zero generic impact finding;
- a nearby control produces zero impact;
- package bytes remain unchanged by execution;
- the claim does not depend on parsing application-specific log text.

## Outcome 3 — Define repeatable execution and reset

### Goal

Make repeated attempts comparable by defining who resets persistent application state and what constitutes an isolated execution environment.

### Why it matters

Fault-vector search assumes that reward differences come from the vector/scenario rather than residue from earlier attempts. The current executor leaves environment reset to the caller or orchestrator.

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

## Outcome 4 — Broaden impact only when a signal discriminates

### Goal

Add the smallest application-independent impact signal that detects a harmful outcome invisible to invariant-count ImpactV1.

### Why it matters

ImpactV1 observes thrown aggregate-invariant rejections. It cannot detect silent compensation mistakes, incorrect final state, missing postconditions, or other domain-visible divergence. Adding many weak metrics would recreate the current reporting problem and make search results hard to interpret.

### Candidate signal families

Evaluate only against explicit positive and negative controls:

- final aggregate/state divergence from the all-zero control;
- compensation postcondition failure;
- incomplete or inconsistent Saga lifecycle state after an otherwise evaluated attempt;
- explicit application-independent postcondition contracts, if the simulator can expose them generically.

Unhandled infrastructure failures, latency, logs, and trace errors should not automatically become impact. They need a clear domain interpretation and evaluation boundary first.

### Done when

- one new signal catches a demonstrated harmful case missed by ImpactV1;
- a safe compensation and ordinary failure control remain non-harmful or not evaluated as appropriate;
- the report separates raw finding facts from any scalar score;
- the model has a clear rule for invalid attempts and correlation to one execution attempt;
- the signal is useful enough to change ranking or search reward in a representative set.

## Outcome 5 — Refresh dynamic attribution only for a concrete need

### Goal

Use dynamic evidence when it can resolve a specific static-identity or coverage problem that blocks useful generation or interpretation.

### Why it matters

Dynamic enrichment is expensive and produces many raw artifacts. Historical broad exact/high-confidence counts showed that attribution can work, but those counts are not themselves execution or impact progress.

### Work direction

- First identify a current v3 workload whose usefulness depends on unresolved runtime identity.
- Run a bounded v3 enrichment baseline with explicit memory, profile, and test selection.
- Prefer direct input ids and structured simulator events over fuzzy value/name matching.
- Treat ambiguous, unmatched, and not-covered results as evidence boundaries, not metrics to optimize blindly.
- Do not let dynamic evidence rewrite package identity or persisted actions.

### Done when

- the run answers a named generation/execution question;
- package bytes remain unchanged;
- attribution improvement is measured against a controlled before/after case;
- raw artifacts and runtime cost are justified by information that changes a decision.

A broad Quizzes v3 refresh is not automatically a higher priority than executable harmful-scenario work.

## Outcome 6 — Local fault-vector search

### Goal

For one fixed useful WorkloadPlan, search persisted fault vectors under a finite execution budget and return high-impact distinct experiments.

### Entry conditions

Do not start until:

- the chosen workload is setup-ready and repeatably executable;
- environment reset is defined;
- at least one vector produces a discriminating evaluated impact;
- arbitrary valid vectors can be persisted idempotently through the v3 on-demand path;
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
