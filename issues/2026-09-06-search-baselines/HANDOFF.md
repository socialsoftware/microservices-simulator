# Handoff — fixed-workload search baselines

State: complete.

## Delivered and coverage

- FR-1: explicit assessment mode in the existing batch attempt primitive. Regression
  remains default. Validity/completeness is separate from expected-result matching;
  non-complete search scores are null.
- FR-2/3/6: current package copy, six missing-vector requests through the ordinary CLI,
  frozen workload/setup/event horizons, candidate IDs/actions, orders and budgets.
  Deterministic `(vector, ID)` order and five seeded permutations without replacement;
  no outcome-driven selection, retry, replacement or cross-workload ranking.
- FR-4/5: per-attempt artifacts, first-positive or explicit not-found, complete budget
  discovery curves, cost and assessment accounting. Every attempt is a new Docker/JVM/H2.
- FR-7/8: corrected 29-scenario benchmark plus CreateQuiz (4) and AnonymizeStudent with
  fixed event (3). Budgets 12/4/3, one deterministic and five random runs: 114 online
  attempts. All COMPLETE; 111 EXACT, three supported DEVIATED. Thirty candidates have
  stable complete repeats; three benchmark candidates were seen once and three unobserved.
  All seven additional candidates have six stable COMPLETE zero results each.

## Proof

- 13 search orchestration tests and 12 existing batch regression tests pass. They cover
  unknown positive/zero, partial/invalid scores, report joins, fixed horizons, budget/order
  tampering, no replacement/retry, duplicate prevention, not-found and snapshot drift.
- All 114 report joins and hashes rechecked, with 114 distinct execution-attempt IDs.
  Package, prepared build, external Maven artifacts, Docker image and measured scripts
  remained unchanged. Snapshot integrity is independent of later checkout edits.
- The 29 current IDs and normalized actions match the corrected structural reference.
  A separately labeled post-run historical comparison checks the 72 benchmark attempts:
  zero mismatches, zero new executions. Historical 14-zero/15-two counts are not claimed
  as a fresh complete landscape; actual online coverage is 26/29.
- Preparation 14.38 s; online execution/integrity checks 1317.02 s. No global time cap or
  retry. Two final isolated checks are separate from the comparison. Failures were
  exercised in tests, not fabricated
  as online attempts. Current Java behavior was unchanged; no stale Maven XML counted.
- Artifact inventory, document links and diff whitespace verified before local commit.

Final review correction: `ScenarioExecutionReport` omits null optional setup records.
The 72 provider-backed benchmark reports therefore legitimately lack `sourceSetup`;
the old batch wrapper left a metadata diagnostic after successfully validating their
scores. The final runner treats absent setup timers as unavailable, extracts the provider
timer, and rejects malformed present metadata without retaining complete/score claims.
Dedicated child-process tests and two fresh provider/source setup checks pass. The 114
original reports/scores remain unchanged; the 72 legacy wrapper diagnostics are retained
in raw artifacts and exposed in the compact CSV. The exact measured batch source is
archived under `verifiers/target/search-baselines/measurement-code/`; proof.json keeps
its hash separate from the final implementation hash. The two final checks are separate
from the 114 online comparison attempts.

Raw: `verifiers/target/search-baselines/`. Compact proof:
`docs/verifiers-impl/evidence/search-baselines-2026-09-06/`.
The [Portuguese results](../../verifiers/experiments/search-baselines/RESULTS.md) contain
the reproduction command, tables, costs, limitations and concrete GA gaps.

## Changes and discoveries

Changed surfaces: batch assessment/snapshot verification option and README; new small
search preparation/run/summary driver, request launcher, post-run reference checker,
tests and docs; current-state batch/search section and roadmap track E/Outcome 6.
No Java pipeline, scoring policy, Quizzes, anomaly, GA or meeting-note changes.

Autonomous detail: a new full generation was unnecessary. The existing package was
current and source-matched; a copy plus requests for only absent canonical vectors
produced the exact corrected 29 IDs/actions. Original evidence was never modified.
The initial prepare command checked IDs before execution; the separate normalized-action
cross-check completed after launch without changing any selection, order or outcome.

Coordination amendment: task `01a078c8-2203-76f3-8bfa-4047a6ef9fbd` committed
`3f331bde1` during measurement, adding GroovyClosureBoundarySpec and unrelated canonical
documentation. The new test is the sole source difference reported against the snapshot.
No shared-build mutation occurred. This task preserves that commit and stages only its
own files/hunks. Personal note and meeting note remain intact. No worktree, push, merge or PR.

## Limits and next

The known benchmark is easy. Random reached the first positive in attempts 1–3 versus
4 deterministically, but found 5–9 positive scenario IDs versus 9 deterministically at
budget 12. This is not general superiority. The additional spaces are flat zero and
retained as such. Five seeds yield only three distinct random orders for the 3-row space.
Observed scenario identity is not defect identity or severity.

A GA still needs an objective, valid fault/recovery encoding and operators, duplicate/
incomplete-result policy, and budget-matched evaluation on an informative fixed universe.
No material scope expansion or user decision was needed in this implementation.
