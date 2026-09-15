# First GA implementation and qualification

## Environment and execution mode

Implements the user-approved [SPEC.md](SPEC.md). Work in the current checkout on
`fault-analysis/scenarios`; preserve all inherited dirty changes and the personal note.
No branches, worktrees, commits or external publishing. Direct implementation with
focused tests, root review of changes and runtime evidence. No additional agents or
intermediate user-verdict pauses are required within the approved boundary.

## Implementation strategy

Add a small fixed-workload search entry point under `verifiers/experiments/`; extract/reuse
the batch attempt primitive and validation where necessary without changing historical
baseline defaults. Accept workload/runtime configuration rather than hardcoding benchmark
IDs in the algorithm. The first application qualification uses Quizzes; core tests use
synthetic evaluators and existing dummyapp package fixtures without application-specific
search rules.

Approved initial algorithm:

- A fault gene per Saga chooses no fault or one of that participant's fault slots.
- A recovery choice resolves against a stable ordering of valid scenarios returned for
  the current vector. Recompute that set when fault genes change; an inherited recovery
  choice that is no longer valid is replaced by a seeded valid choice and recorded.
- Maintain a small population of evaluated, distinct candidates. Select parents through
  two-candidate fitness comparisons, combine participant fault choices and a recovery
  choice, then mutate one coordinate. Retain the best evaluated candidates, randomize
  equal-fitness ties using the declared seed and inject random exploration when offspring
  repeat. Do not create a parent from unavailable fitness.
- Default population 8, capped by the execution budget; mutation probability 0.3. These
  are configurable starting values, not tuned research findings. Record all operator
  choices, the initial-policy identifier and the scalar I used for each parent decision.
- Random proposals use the same seeded fault-coordinate sampler and conditional recovery
  sampler as GA initialization/exploration. This is not uniform sampling across all
  FaultScenarios: vectors with different recovery counts have different scenario-level
  probabilities. Name and report that distribution; keep the old uniform finite-catalogue
  permutation baseline unchanged.
- Stop after 100 consecutive duplicate/unresolvable proposals with an explicit stall
  result (configurable); this prevents a small saturated domain from looping forever.
  Distinguish unavailable fitness, proposal rejection and integrity failure. Only real
  launched executions consume the execution budget; record generation cost separately.

Freeze the cap and logical domain independently of scores. Each arm starts from the
same package copy. Cache vector requests within the arm, record legitimate package
evolution and verify untouched workload/setup content and runtime hashes. A retained
scenario generated with another cap cannot silently enlarge the arm's requested domain.

## M0 — Resolve and inspect one selected workload

**Outcome / coverage:** FR-1–4. The user can prepare a run and see its real search boundary.

**Boundary and anchors:** new search CLI/configuration; existing
`FaultScenarioRequestCli` / `OnDemandFaultScenarioService`, current package readers and
`batch-execution/run.py`. The on-demand service already returns both generated and
uncapped recovery counts and deduplicates executable content; use those contracts.

**Discovery:** verify how the matching current runtime is frozen/launched and how its
read sidecar is enabled. Confirm metadata updates after on-demand additions and canonical
fault masking. Inspect the selected known benchmark and current update/read package;
avoid rebuilding the whole application merely to wrap an already qualified runtime.

**Proof:** dummyapp-backed package tests for selection, canonical fault mapping, immutable
input copy, on-demand repeat/dedup, legal recovery selection, cap visibility and missing
setup/runtime errors. Show one concrete prepared configuration with no implementation
claim about Saga-set filtering.

## M1 — Evaluate, evolve and report

**Outcome / coverage:** FR-5–9. GA makes adaptive decisions; random remains outcome-independent.

**Boundary:** search policy, candidate cache, request/evaluation adapters, sidecar validation
and summaries. Preserve ordinary execution and detector semantics. Collect both I and A;
validate their attempt/workload/scenario joins before presenting them together.

**Proof:** deterministic fake-evaluator tests for real fitness-dependent parent choices,
crossover/mutation validity, variable recovery counts, equal-score populations, null
fitness, duplicate aliases, budget consumption, all-invalid runs, stalls and replay.
Demonstrate that changing measured fitness changes GA decisions but not the random arm's
proposal sequence. If Java generic contracts change, add positive/negative Spock tests
using dummyapp fixtures; otherwise keep tests with the search layer.

## M2 — Small real qualification and user-facing evidence

**Outcome / coverage:** FR-6–10. A user can inspect and replay selected results, and compare
GA/random on the same fixed workload under equal execution budgets.

**Protocol:** select the qualified RemoveTournament/AddParticipant workload before new
outcomes, plus the latest fixed update/read workload as an integration control for A.
Run one fresh no-fault control per selected workload. On the benchmark, compare GA/random
with seeds 11 and 29 and budget 12 per arm: at most 48 search attempts. On update/read,
replay the three final-fault recovery placements (known A=1/0/0) through the shared
evaluator to prove sidecar/report integration: 3 additional attempts. Total first
qualification: at most 53 application executions, with controls/probes reported separately
from the 48 search slots. No automatic larger campaign or tuning sweep.

If the benchmark no-fault control is invalid or no compatible runtime can be established
within the approved adapter boundary, retain the diagnosis and resolve that prerequisite;
do not silently substitute a workload selected for better scores. A zero/flat or losing
GA result is valid evidence. No win is required to finish engineering qualification.

**Proof:** each launched attempt has a fresh container/JVM/H2, actual reports, explicit
seed/budget, source/build/package provenance and no outcome-label input. Check unchanged
input packages, replay records, cap accounting and duplicate behavior. Publish a short
English results/handoff document with exact commands and links; update canonical current
state and roadmap. Add a brief Portuguese explanation only where it helps the meeting note.

## Validation strategy

Run narrow search-layer tests, affected existing batch/baseline tests, and generic Java
tests only if those contracts change. Then execute M2 once on the final code, inspect
the real report joins and retained artifacts, and run documentation/link/diff checks.
Use repeated runs only to resolve a failure or material remaining concern.

## Risks and fallbacks

- Recovery generation still computes counts and may be expensive for a large vector;
  cap visibility is not a performance fix. Retain a named generation failure rather
  than claiming candidate-space exhaustion; defer counting optimization unless it blocks
  this small qualification and fits a separately agreed change.
- A flat I landscape provides little direction. Report it; do not add anomaly weights
  to manufacture variation. The three update/read placements qualify A collection only.
- Dynamic package additions differ from the old frozen-universe baseline. Use per-arm
  copies and recorded request/revision hashes; never relax runtime integrity validation.
- A recovery choice is conditional on its vector. Test its re-resolution explicitly;
  never execute a parent scenario's action ordering against another vector.
- Broader anomaly work, focused Saga-set generation, Quizzes repair and cross-workload
  allocation remain outside this package.

## Approved follow-up — Positive-discovery evaluation

After reviewing the first result, the user approved completing the 29-case reference map
and comparing discovery speed under current I before changing the feedback. Preserve
search operators, population 8, mutation 0.3 and stall limit 100. Complete the eight unseen
candidate keys using the same frozen runtime; reuse the 21 already measured keys with
report hashes and explicit provenance. This map is evaluation-only.

Run independent fresh application attempts for GA/random with seeds 11, 29 and 47,
budget 29 per arm, up to two concurrent containers with separate package/output/database
state. This permits 21 post-initialization evaluations. Maximum new application attempts:
8 map completions + 6 × 29 search attempts = 182. Reuse the identical-runtime qualified
no-fault control with hash verification. The preset budget and seeds are frozen before
the missing map cases are measured; no tuning or outcome-driven replacement.

Report cumulative distinct I-positive discoveries, first positive, execution counts to
at least 50%/80%/100% of reference positives, best I, crossover/fallback contributions,
duplicate proposals, wall time and stop reason. Retain unmet targets and proposal stalls;
do not infer all positives have been found from an exhausted budget. Reference completeness
applies to this workload and recovery cap only. Any score change requires a newly assessed
reference map. No combined feedback, new application fixes or anomaly detectors in this step.
