# M0 handoff — honest pair-shaped baseline

State: `complete` after independent review.

## Outcome

The current attachment boundary is confirmed to be pair-shaped. Quizzes analysis emits
`363` `SourceSetupPlanBinding` records overall. The focused
`RemoveTournamentAddParticipantRecoveryWindowExploratoryTest` contributes `20` distinct
RemoveTournament/AddParticipant pair bindings. Selecting one exact pair and its ten
dependency-preserving schedules produces ten written WorkloadPlans, all referencing the
same validated 12-action setup.

| Measure | M0 before |
|---|---:|
| Quizzes source setup bindings | 363 |
| Focused Remove/Add pair bindings | 20 |
| Focused written workloads | 10 |
| Focused setup-bearing workloads | 10 |
| Larger workload can inherit an incomplete pair setup | yes |
| Accepted / rejected inputs, size 1–3 count-only | 794 / 90 |
| Materializable / blocked inputs | 150 / 644 |
| Strict connected sets, sizes 2 / 3 | 382 / 3,594 |
| Strict sets with accepted positive tuples, sizes 2 / 3 | 35 / 42 |
| Selected input-bound total | 74,273 |
| All input-bound total | 1,247,308,000 |
| Count-only written workloads | 0 |

The accounting row is from
`verifiers/target/date-recipe-verify/quizzes-20260903-201915-798/accounting.json`, the
latest relative-date-qualified size-1–3 run. It is the M2 selection baseline; setup
attachment is not expected to alter it.

## Concrete current behavior

For one focused RemoveTournament/AddParticipant pair, the generator emits ten workload
schedules. Every workload references the same setup, whose 12 source-ordered actions
include the four supported void activation/enrollment effects and whose four participant
bindings reconstruct the two selected Saga inputs.

When that exact pair is placed in a three-participant brute-force tuple with a
CreateUser input observed in a different test class, the current generator still
attaches the pair setup because `setupPlanFor` tests only whether the tuple contains its
left and right ids. The setup has no binding for the third input. This is the focused
larger-workload regression boundary for M1: after the fix, this tuple must not receive
that setup.

## Discovery and classification

- Read-only preflight inspected `ApplicationAnalysisScenarioModelAdapter.adaptSetupBindings`,
  `SourceSetupPlanBinding`, `ScenarioGenerator.setupPlanFor`, the source-derived setup
  fixture, commits `a94ccc2ff` and `3d8cd6ab9`, and the current size-1–3 accounting.
- Autonomous test detail: strengthened the existing realistic fixture to pin the 20
  focused pair bindings, ten focused written/setup-bearing workloads, and one
  three-participant incomplete-coverage example. This changes no production behavior or
  persisted artifact shape.
- Natural non-pair Quizzes evidence exists. The smallest clean M1 candidate found is
  `UpdateStudentNameFaultTest`: its one observed `setup()` context contributes three
  inputs across AddStudent, GetCourseExecutionById, and UpdateStudentName, currently
  represented by three pair bindings. M1 may use this one natural size-three case; no
  arity matrix is warranted.
- No material scope delta was found. The broader total of 363 pair records is larger
  than the previously highlighted single executable package, but it is the existing
  internal analysis result and does not change the approved strategy.
- `lib/` and `tmp/` remain untracked and untouched.

## Actual files changed

- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/SourceDerivedSharedSagaWorkloadAnalysisSpec.groovy`
- this handoff

## Proof

- `mvn -q -Dtest=SourceDerivedSharedSagaWorkloadAnalysisSpec test` — pass (1 test,
  including package write/read and setup materializability assertions).
- `git diff --check` — pass.
- Independent reviewer: pass with no blocking findings; independently reran the focused
  spec and `git diff --check` successfully.
- No production source or package schema changed in M0.

## What to inspect

Read the new three-participant assertion beside the existing Remove/Add pair regression.
It exposes the current subset-containment attachment directly; M1 should flip only that
attachment result while retaining the existing pair behavior and package round trip.
