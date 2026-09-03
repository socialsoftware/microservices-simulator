# M2 handoff — bounded Quizzes qualification

State: `complete` after independent re-review.

## Outcome

A fresh Docker size-1–3 count-only run reproduces the M0 selection baseline exactly.
A bounded stable-order writing run then emits ordinary Quizzes workloads with no
candidate reservation or reordering. Of its 1,415 WorkloadPlans, 408 reference a
source-derived setup: 94 singles, 182 pairs, and 132 triples. The package also contains
three provider-backed setup references, which are excluded from the source-setup count.

All 408 source-setup-bearing workloads pass the artifact-level complete-coverage check:
every setup binding targets an exact selected InputVariant, and every selected input
without a setup binding is independently materializable. All 94 referenced
source-derived SetupPlans are validator-materializable.

| Measure | M0 / before | M2 / after |
|---|---:|---:|
| Accepted / rejected inputs, count-only | 794 / 90 | 794 / 90 |
| Materializable / blocked inputs, count-only | 150 / 644 | 150 / 644 |
| Direct interactions (exact / symbolic / type-only) | 785 (0 / 535 / 250) | 785 (0 / 535 / 250) |
| Strict connected sets, sizes 2 / 3 | 382 / 3,594 | 382 / 3,594 |
| Strict sets with accepted positive tuples, sizes 2 / 3 | 35 / 42 | 35 / 42 |
| Selected / all input-bound total | 74,273 / 1,247,308,000 | 74,273 / 1,247,308,000 |
| Count-only written workloads | 0 | 0 |
| Complete non-pair source-setup workloads in bounded writer | 0 supported by pair-keyed attachment | 226 (94 singles + 132 triples) |
| Bounded writer source-setup workloads | not measured broadly in M0 | 408 |

The existing recovery-window pair remains present naturally. Representative WorkloadPlan
`6f5929507736311754102ea0f115c8ef79d42e4944d8c33a5dff9274f28efd51`
uses setup `setup-d1885b675486ca21f98077bc`: 12 source-ordered actions, four
bindings, and one retained Tournament result shared by RemoveTournament and
AddParticipant. A natural non-pair representative,
`0f3c210263e36febd0451e4fdf43a900f08dd12e6782517364170c65e075398a`,
combines AnonymizeStudent, GetCourseExecutionById, and
RemoveStudentFromCourseExecution with one four-action, five-binding setup.

## Caps and feasibility

The invariant count-only run used the M0 configuration: `maxSagaSetSize=3`,
`maxInputVariantsPerSaga=1000`, `maxSchedulesPerInputTuple=20`, strict selection,
order-preserving interleaving, and no type-only fallback.

An initial writing attempt retained 1,000 variants and used the M0 selected-input total
as `maxCatalogScenarios=74273`, with one schedule and one recovery schedule. It was
stopped after 8m42s before artifact writing because the writer must scan the
1,247,308,000 all-input Cartesian space before strict selection; a JVM stack sample
confirmed it was in `InputTupleJoiner` / `InputTupleSelection`. The attempt changed no
repository file and emitted no package.

The feasible writing run therefore used the predecessor evaluation bound
`maxInputVariantsPerSaga=10`, sizes 1–3, `maxCatalogScenarios=50000`, one schedule per
tuple, one recovery schedule per vector, strict selection, and the same stable order.
It completed in 56.19s and exhausted naturally at 1,415 workloads, so the total-workload
cap was not reached. The specific M0 `UpdateStudentNameFaultTest` triple falls outside
the stable first-ten input variants; it was not reserved or reordered. The run instead
contains 132 other natural setup-bearing triples, including the representative above.

## Runtime preflight

The two representatives were projected into one current-shape evaluation package with
only their two WorkloadPlans, two source setups, and twelve matching FaultScenarios.
The supported Docker CLI accepted the package and spawned one fresh Spring/H2 worker per
source-setup workload.

- report terminal status: `SUCCESS`; selection `MANIFEST_DECLARED_MATERIALIZABLE`;
  two candidates, five participants, no blockers;
- natural triple: `SETUP_READY`; all three participants `MATERIALIZED` and
  `STARTUP_READY`; 4/4 actions and 5/5 bindings succeeded;
- existing pair: `SETUP_READY`; both participants `MATERIALIZED` and `STARTUP_READY`;
  12/12 actions and 4/4 bindings succeeded; two pending setup events were cleared;
- both workers began from an empty pending-event baseline.

## Artifacts and hashes

- count-only package:
  `verifiers/target/m2-count-only/quizzes-20260903-205314-778/`; manifest SHA-256
  `9c471acfede173f8e8a180b34e0bf94dbb0b12b5a2fa0ce9dd3409f9ccf5bcdf`;
  accounting SHA-256
  `cc01b7db6df3cf7383dbe8e799aee926a07ece7be1e8e584f7091aa77e270b3b`;
- bounded writing package:
  `verifiers/target/m2-write/quizzes-20260903-210351-440/`; manifest SHA-256
  `3a723e38541603c5ac2753d43a6927b5e0fa55360c36d4286a2ff8552f3f83da`;
  workloads SHA-256
  `b6b9653f15f3dd111553177ecb140fa16938c505a9fd13bb99f01b5a3a701784`;
  setups SHA-256
  `1a84515d6cc95aade67f6d3383ca1ed31e36ea5e88171be88f26a3a9a0888e8f`;
- isolated evaluation package:
  `verifiers/target/m2-preflight/package-pair-triple/`; manifest SHA-256
  `db0242772f89d050e2410de1b32d4ec937f60b5b152edb18e1b7eae3f2ec6afb`;
- preflight report:
  `verifiers/target/m2-preflight/pair-triple-preflight-report.json`; SHA-256
  `e5d45eb00e3fca8c6d75d64ee64275fb04e4f71e743cedd818f16b7a0c5a85bd`.

No duplicate full Quizzes generation was run for byte equality. Existing deterministic
contract tests provide that proof; package hashes were unchanged by preflight.

## Changes and classifications

- Autonomous evaluation-fixture repair:
  `applications/quizzes/src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/executor/QuizzesRemoveAddBenchmarkRunner.java`
  no longer calls the removed `SelectedPackageContents.rejectedInputsPath()` accessor.
  The current-only package migration removed that artifact in commit `a6219f93b`, but
  this test-only benchmark runner retained the stale alias check. The correction changes
  neither production Quizzes behavior nor verifier schemas.
- Review pass 1 found that merely removing the stale accessor weakened result-output
  alias protection: `inputs.jsonl` and other current manifest-declared artifacts were no
  longer checked. The runner now derives every package artifact path from the already
  reader-validated current manifest and rejects aliases to any of them, as well as the
  manifest and execution/impact outputs. The focused regression in
  `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/executor/QuizzesRemoveAddBenchmarkRunnerSpec.groovy`
  pins an `inputs.jsonl` alias. No package schema or production behavior changed.
- Autonomous evaluation decision: reduced only the writing run's input cap to ten after
  the 1,000-input Cartesian scan proved infeasible. Count-only accounting remains the
  full 1,000-input authority.
- Out of scope: optimizing `InputTupleJoiner`'s large Cartesian scan and adding runtime
  dispatcher methods beyond the existing closed map.
- No material scope delta was found. Persisted artifact shapes and accounting are
  unchanged. Untracked `lib/` and `tmp/` remain untouched.

## Proof commands

- Docker count-only generation with the canonical size-1–3 environment above — pass,
  39.30s.
- Docker bounded writing generation with input cap 10, workload cap 50,000, schedule
  cap 1, and recovery cap 1 — pass, 56.19s.
- Artifact join over `workloads.jsonl`, `setups.jsonl`, and `inputs.jsonl` — 408/408
  source-setup workloads complete and 94/94 referenced setups materializable.
- `docker compose run --rm ... scenario-executor` against the isolated two-candidate
  package — pass, 63.45s after compilation; two fresh workers, terminal `SUCCESS`.
- `mvn -q -Ptest-sagas -Dtest=QuizzesRemoveAddBenchmarkRunnerSpec test` from
  `applications/quizzes/` — pass after review-pass-1 remediation (50 tests).
- `mvn -q -Dtest=ScenarioModelSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec test`
  from `verifiers/` — pass.
- `git diff --check` — pass after review-pass-1 remediation.

## What to inspect

Open the preflight report and compare the natural triple's five exact participant
bindings with the existing pair's four bindings. The intended result is that both are
setup-ready through the same persisted model while count-only selection totals remain
identical to M0.
