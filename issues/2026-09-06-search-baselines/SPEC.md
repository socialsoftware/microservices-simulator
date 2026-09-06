# Fixed-workload search baselines

## What this is

A small experiment harness compares stable deterministic ordering with reproducible
random sampling without replacement over explicitly persisted FaultScenarios. It
executes candidates online through the existing isolated ScenarioExecutor and discovers
positive COMPLETE ImpactV2 results without needing prior expected scores.

## Goals

1. Separate report validity/assessment from known-outcome regression matching.
2. Reproduce budgeted discovery within each fixed workload and observation horizon.
3. Exercise the corrected 29-scenario benchmark and two additional diverse workloads.

## Non-goals

No GA/adaptive fitness, impact/anomaly/domain changes, cross-workload ranking, catalogue
qualification, meeting-note/domain-sheet writing, same-process reset or global time cap.

## Functional requirements

- FR-1: Preserve the existing batch regression mode; explicit assessment mode requires
  no expected result and validates report identities, execution/conformance and completeness.
- FR-2: Freeze package/build identities, eligible scenario IDs, workload/setup/horizon,
  ordering rules, seeds and positive integer attempt budgets before any new outcomes.
  Candidate selection/order must not read observed scores.
- FR-3: Deterministic order is `(faultVector, scenarioId)`; random order is a seeded
  permutation of the same eligible IDs. No candidate repeats within a strategy run.
  Each comparison uses the same budget. Invalid attempts consume budget; no retries.
- FR-4: A positive is a COMPLETE ImpactV2 score greater than zero from a valid completed
  execution. Partial/invalid/unavailable results have unavailable search score, never zero.
  COMPLETE zero is evaluated non-positive. Preserve all reports, failures and blockers.
- FR-5: Report one-based attempts until first positive or explicit NOT_FOUND_WITHIN_BUDGET,
  cumulative distinct positive scenario IDs at every budget prefix, wall/process costs,
  validity/completeness and exact artifact links. Distinct scenarios are not distinct defects.
- FR-6: Compare within one workload/setup and declared event horizon. Never select fewer
  event deliveries to improve reward or merge scores into a global workload ranking.
- FR-7: Use the corrected 29-scenario/12-vector benchmark as an easy known control,
  not evidence of superiority. Label any historical/offline check separately from new
  online executions. Select at least two additional workloads by structural diversity
  and prior execution readiness before new scores; retain every selected failure.
- FR-8: Qualify repeatability from independently repeated candidate executions across
  seeds, after selection is fixed. Never remove a candidate due to its observed score.

## Architecture

Experiment scripts reuse the existing batch Docker/process launcher and report validator.
Java pipeline boundaries and existing reset semantics remain unchanged. One fresh
container/JVM/in-memory H2 per attempt; at most two independent strategy runs concurrently,
with sequential attempts inside each run. No cache of scores substitutes for executions.

## Data model

A frozen experiment references one current package/build and workload-specific candidate
universes. Strategy plans persist the full permutation and budget prefix. Each attempt
joins its workload, scenario, vector, package and unique execution ID. Discovery summaries
derive only from retained attempts; incomplete assessment retains its status and null score.

## Security model

Use only the current local checkout and disposable named containers. Preserve unrelated
files and retained evidence. No push, merge, PR, production changes or IDE configuration.

## Operating

Explicit CLI freeze/run/summarize commands; new output directories, normal per-attempt
180-second timeout, no global temporal budget. Hash-check prepared sources/classes,
Maven artifacts, package and launcher. Reject incompatible inputs rather than migrating IDs.
Implementation, tests, campaign, documentation and a linear local commit are approved.

## Future roadmap

A GA would need candidate encoding/operators constrained to valid persisted scenarios,
duplicate policy and budget-matched adaptive evaluation. These baselines expose the
evaluation boundary but implement none of those policies.

## Open decisions

None. Routine campaign choices are frozen in PLAN.md and then machine-readable artifacts.
