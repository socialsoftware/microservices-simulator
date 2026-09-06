# Observer coverage and control qualification

## Scope

Implements the approved first two priorities in [BRIEF.md](BRIEF.md). Scoring categories,
weights, timestamp comparison and treatment of newly created deleted records are unchanged.
Quizzes production defects remain separate work. The concrete domain cases for the next
discussion are in [DOMAIN-CASES.md](DOMAIN-CASES.md).

## Changes

- The persistent observer recognizes a proved inverse JPA backlink to a nested owned
  ancestor. It retains meaningful child fields and uses relative ancestry rather than
  generated row IDs or set iteration positions. Unsupported cycles remain explicit gaps.
- The package reader restores readiness per argument recipe. A setup-bound identity no
  longer blocks an independent complete DTO argument. Unsupported descendants remain
  blocked, and overall source materializability is not promoted.
- Quizzes test fixtures supply actual Quiz identities, enrolled Tournament participants
  and eligible QuizAnswer receivers. The existing successful CourseExecution feature
  supplies a distinct acronym. One exact setup dispatcher method supports disenrollment.
- Saga and Causal sliced test configurations supply the real observer bean required by
  the existing event service. This is test wiring, not a production configuration change.
- A small experiment selector retains all 30 earlier named control/fault pairs with eight
  explicit source replacements. It also selects all 29 corrected benchmark schedules.
  The runner freezes a Docker source build, preserves every attempt and validates report
  identities, package hashes and distinct-object counts.

## Discovery during qualification

The first regenerated package omitted two prerequisite calls written inside a feature:
the extra course offering and prior student disenrollment. The campaign did not run that
selection. A dedicated test places those calls in `setup()`, the supported source-extraction
path. The final package contains all ten setup actions in order, with the original offering
and student identities bound to disenrollment. The measured removal is excluded from setup.
No generated JSON was edited and no generic feature-body extraction was added.

The initial package, selection and setup audit remain under `generated/`, `selection/`
and `initial-setup-audit.json`. The accepted inputs are under `generated-final/`,
`selection-final/` and `final-setup-audit.json`. Selection was frozen before runtime results.

The main campaign then exposed a missing subscriber on `UpdateStudentName-events` after
its DTO became executable. A first repair supplied a QuizAnswer, but the persisted route
selected the Tournament handler for that same event; that pair remained invalid. The
final source fixture supplies a Tournament and the exact participant, and explicitly
checks that Tournament's subscription. Its generated schedule preserves the original
Tournament route. Both roles were rerun; the two invalid controls and their paired fault
attempts remain under `run-01/` and `run-02/`. The accepted replacement is `run-03/`, from
`generated-route-corrected/` and `selection-route-corrected/`.

The final test addition changes opaque SolveQuiz setup/workload/scenario IDs, but its
input, setup actions/bindings and selected fault actions are identical. All 58 unaffected
broader executable descriptions and all 29 benchmark selections match the first campaign;
see `unaffected-selection-comparison.json`. Only the corrected event pair was rerun.

## Validation

- Simulator: 136 tests passed, including the positive/negative ownership-cycle cases.
- Verifier: 775 tests passed; the reader/materializer focused subset passed 35 tests.
- Quizzes: focused Saga controls passed after real observer wiring. The dedicated
  course-removal receiver and dispatcher final subset passed 3 tests. A Causal/TCC
  invariant smoke passed 1 test with the same real observer wiring.
- Fresh Docker benchmark: all 29 COMPLETE, 14 score 0 and 15 score 2. The ten former
  partials resolve to four zeros and six twos. Previously complete scores, execution
  outcomes, conformance and implicit rollback lists are unchanged; 23 attempts retain
  the Tournament-status rollback. Proof: `benchmark-comparison.json`.
- Broader final selection: all 60 COMPLETE across 30 pairs, with 30 successful exact
  controls. Scores: 52 zero, seven one, one two. Seven fault runs have positive scores;
  one successful control also scores one. Equal numbers do not imply equal findings.
- Retained execution history: 93 attempts, 91 COMPLETE and two INVALID. The final
  comparison uses the benchmark and 29 broader pairs from `run-01`, plus the corrected
  name-update pair from `run-03`. No failed attempt was erased or counted as zero.
- Final Tournament receiver fixture: 2 focused Saga tests passed; the selected handler,
  participant bindings and full setup were checked in `route-corrected-setup-audit.json`.

Review is recorded in [REVIEW.md](REVIEW.md). Slice detail is in
[OBSERVER-HANDOFF.md](OBSERVER-HANDOFF.md) and
[PREREQUISITES-HANDOFF.md](PREREQUISITES-HANDOFF.md).

## Reproduction and boundaries

Commands, package/selection/source hashes, build log, setup audits and run reports are
under `verifiers/target/impact-coverage-controls/`. The checked-in entry points are
`verifiers/experiments/impact-coverage-controls/README.md`, `select.py` and `run.py`.
Execution uses the ordinary Saga/local ScenarioExecutor in a fresh JVM/H2 database per
attempt, at most two concurrent attempts. Existing Docker services are left running.

These are selected workload comparisons, not all possible inputs, fault positions or
recovery schedules for the broader application. Changing a fixture changes the comparison
input, so the recorded eight replacements are not claimed to be identical old experiments.
The benchmark retains its existing input and schedules. Completed observation means the
three declared checks were evaluable; it does not certify domain correctness.

The qualification did not apply a production Quizzes repair. Local consolidation and
commit validation are recorded below; no merge or push is part of that work.

## Primary-checkout consolidation

The completed implementation and qualification are now present in the primary checkout.
The final complete verifier run exposed an outdated source-analysis test expectation:
the dedicated `RemoveCourseExecutionQuizAnswerReceiverTest` fixture adds one nested
Question input and one nested Quiz input. The test now expects 88 and four respectively,
and explicitly verifies that fixture as the source of each additional input. No metric
or production-application behavior changed during consolidation.

Primary-checkout validation with JDK 21 passed: 775 verifier tests, 136 simulator tests,
17 selected Quizzes tests, and 14 Python experiment tests. The documentation site builds;
the seven retained meeting-evidence files match their recorded SHA-256 hashes and source
reports. Maven logs are under `verifiers/target/local-consolidation-validation/`.

The canonical summary and roadmap now distinguish completed implementation/qualification
from pending methodological discussion. The Portuguese advisor note develops the first
CourseExecution control/fault pair, with remaining detailed cases awaiting user review of
the format. Supporting report copies are retained under
`docs/verifiers-impl/evidence/impact-2026-09-06/`.
