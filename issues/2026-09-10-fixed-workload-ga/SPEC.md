# Fixed-workload genetic search

## What this is

A local command lets a developer select one existing WorkloadPlan and search its fault
and recovery alternatives under an execution budget. The search uses the existing
generator, ordinary Saga/local executor and measured reports. It can request candidates
on demand instead of requiring their complete enumeration before the run.

The user approved this package for local implementation and its first qualification.
The implementation and evidence are recorded in HANDOFF.md.

This is the historical first-GA contract. The direct brief approved on 15 September
extends FR-6/FR-7/FR-9 with optional per-criterion weighted fitness and offline rescoring;
the legacy policy remains available. See the [current fitness contract](../../docs/verifiers-impl/current-state.md#search-fitness)
and [configuration guide](../../verifiers/experiments/fixed-workload-ga/README.md#configurable-fitness).

## Goals

1. Run a real adaptive GA and a seeded random comparison through the same evaluator.
2. Preserve exact reproducible scenarios, both measurements and explicit coverage.
3. Make inherited generation limits and unexplored alternatives visible to the user.

## Non-goals

No GUI, automatic selection across workloads, focused generation from a user-supplied
Saga set, new input synthesis, anomaly detector, Quizzes repair, combined weights or RL.
The first qualification establishes correctness and integration, not GA superiority.

## Functional requirements

- **FR-1 — User flow.** Accept a current package manifest, exact workload ID, compatible
  prepared runtime, strategy, seed, execution budget and output location. Show the chosen
  participants, inputs/setup, normal order, selected events and inherited generation limits
  before measured search. A preparation failure must identify its cause.
- **FR-2 — Fixed experiment.** Participants, inputs, setup, normal schedule, event horizon
  and runtime configuration stay fixed. Each strategy/seed receives its own working copy
  of the package; the input package and other strategies' packages remain unchanged.
- **FR-3 — Candidate domain.** Search faults and valid recovery alternatives. Represent
  each participant by no assigned fault or one selected faultable step. Later faults on
  that participant are masked by the existing generator after its first failure; do not
  spend evaluations on those redundant encodings. Several participants may fail.
- **FR-4 — Generator authority.** Resolve candidate vectors through the existing on-demand
  service, retain returned scenarios and choose only a returned valid recovery ordering.
  Record requested vectors, cap, uncapped/generated recovery counts and truncation. Freeze
  one recovery cap per experiment. Do not imply that search can explore alternatives
  omitted by that cap or workloads omitted by earlier catalogue limits.
- **FR-5 — Adaptive search.** GA parent selection depends on completed earlier fitness
  evaluations, and offspring combine and mutate fault/recovery choices. Persist the realized
  lineage/choices. Random selection must not inspect scores and must have access to the
  same candidate domain, generator and cap. Do not reuse scores across comparison arms.
- **FR-6 — Measurements.** Initial fitness is the available COMPLETE ImpactV2 object count
  I, identified explicitly as the initial policy. Also collect and validate the existing
  read-exposure report A, coverage and findings. Missing/partial A remains explicit and
  does not redefine eligibility under the I-only policy. No combined weights are selected.
- **FR-7 — Invalid measurements.** Incomplete/invalid/unavailable I is null fitness. It
  remains reported and consumes an execution slot; it cannot compete as zero or serve as
  an evaluated parent. Continue exploration when no eligible parent exists. Infrastructure
  or integrity failure is retained with its cause; package/build integrity failure stops
  the affected run.
- **FR-8 — Accounting and termination.** Cache candidates within one strategy run by
  fixed-workload, canonical-vector and exact ordered-action content, including historical
  ID aliases. A duplicate proposal reuses its result and consumes no execution slot;
  invalid executions are not automatically retried. Stop at the execution budget, proven
  domain exhaustion or an explicit proposal-stall limit. Do not label a stall as exhaustion.
- **FR-9 — Results.** Write configuration, candidate requests, attempts, measurements,
  best-so-far curve, time to first I-positive, distinct positive scenarios, invalid statuses,
  duplicate/proposal counts, stop reason and replay references. Include I and A separately
  in the readable summary. Counts of scenarios are not counts of independent defects.
- **FR-10 — Qualification and comparison.** Qualify the chosen workload's no-fault control
  before comparison, recording its cost separately. Both strategies receive equal
  execution budgets, scopes and declared seeds; failed evaluations stay in denominators.
  Reproduction uses persisted configuration/choices and matching build/package evidence.

## Architecture

Keep search in a thin experiment/CLI layer above the existing Java scenario-generation
and executor boundaries. Reuse the isolated attempt launcher and report-validation logic.
Production Java changes, if required, are limited to exposing existing on-demand/evaluation
contracts; do not move search logic into source visitors or application code.

## Data model

A search configuration references a workload and frozen runtime. A proposal resolves to
a canonical vector and exact persisted recovery actions. An attempt references that
candidate, its package revision and its execution/impact/read reports. Fitness, assessment
availability and stop/accounting state are explicit fields. Search records are additive;
current package and detector schemas keep their meaning.

## Security model

Local disposable runtime only. Preserve inherited dirty work, source packages and previous
reports. No commits, branches, worktrees, push, merge or application mutations in this task.

## Operating

Command-line preparation/run/summary, fresh JVM/H2 per real attempt and existing container
isolation. Seeds, budgets, GA settings, generation cap and per-attempt timeout are recorded.
Rollback consists of leaving the new search command unused; existing baseline commands
and historical evidence stay reproducible. General resume/distributed execution is deferred.

## Future roadmap

Add the agreed I/A priority rule after the advisor discussion. Later, focused generation
can accept a user-selected Saga set and expose subsets, inputs and normal orders. That
produces workloads for this search to consume. Budget allocation across workloads belongs
above the GA and can later support the planned bandit/contextual approach.

## Open decisions

| # | Decision | Default | Alternatives | Impact |
| --- | --- | --- | --- | --- |
| 1 | Final I/A combination | Deferred; I-only is the initial experimental policy | Weighted combination or explicit objective ordering after discussion | Does not block this implementation |
| 2 | Large evaluation cohort and tuning | Separate follow-up after this qualification | Broader budget/seed sweep | This package makes no superiority claim |

No new glossary term is needed: existing WorkloadPlan, FaultScenario and measurement
definitions remain authoritative. Initial algorithm parameters are recorded in PLAN.md.
