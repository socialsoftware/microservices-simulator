# Space-map handoff

Completed: eight workloads, 159 discovery attempts and sixteen stable representative
repetitions, with 175 distinct validated execution-attempt IDs. Discovery yielded
119 COMPLETE assessments (111 zero, eight score one) and forty INVALID executions.
The ID sample represents 117 distinct action sequences: 94 COMPLETE, including six
positive sequences, and 23 invalid. The 42 equivalent-ID comparisons also matched.

## Changed boundary

- Added `verifiers/experiments/space-map/`: supported generation/request launchers,
  structural selection, canonical-vector and scenario sampling, frozen batch
  orchestration, report analysis, compact evidence export, tests, README and results.
- Added this issue's SPEC, PLAN, FINDINGS and handoff.
- Canonical documentation and compact evidence are updated with the completed campaign.

For the original campaign, no Java production code, Quizzes domain behavior, ImpactV2 formula/category,
anomaly policy, GA, meeting note, personal note, branch/worktree creation, push or merge is
part of this change. The Java file in the experiment is a source-launched wrapper
around the existing OnDemandFaultScenarioService.

## Implementation discoveries

1. A process exit failure can still have a complete, validly shaped report bundle
   describing an INVALID execution. `analyze.py` validates and accounts for these
   bundles, while preserving the batch runner's process status and null score.
2. Source setup success and shared participant bindings do not guarantee a useful
   no-fault control or an eligible subscriber for every selected event.
3. Eager/on-demand IDs can have identical compact executable content. The frozen
   ID sample was retained; a later analysis view groups exact workload/vector/action
   sequences, reports both denominators and compares equivalent-ID outcomes.
   No identity or package migration was performed.
4. The newly observed FAILED_OPERATION_RESIDUAL signal includes an object absent
   before execution and persisted as DELETED after compensation. The report retains
   that lifecycle interpretation and does not label it a demonstrated functional defect.

The source build is the prepared `combined-event-deliveries/run-02` snapshot. At
selection, its production matched `ebea5526d`; only the later
GroovyClosureBoundarySpec test was absent. A parallel task subsequently changed
production in the checkout using its own Maven cache/targets and waited for this
campaign before using Docker. This campaign measures the earlier frozen build,
not that new diagnostic implementation.

The original 159-ID sample, eight workloads, fixed schedules, vector choices,
recovery caps and score definition were not changed after observing results.
Equivalent-ID and invalid-report analyses are additive interpretation/proof.

## Proof and recommendation

- All 175 setups succeeded and all 175 validated report bundles have unique attempt IDs.
  All forty discovery failures are selected-subscriber hard stops, not zero fitness.
- Build, package, measured scripts and external dependency hashes matched before/after;
  image identity matched at start/end. Runtime production is the frozen pre-Saga-read
  snapshot, while checkout drift is recorded separately.
- Twelve sampling/analysis tests and twelve reused-runner regression tests passed;
  shell syntax, 105 real request results and all report joins were checked.
- Generation: 23.88 s; on-demand preparation: 224.08 s; execution campaign and checks:
  2604.49 s. Attempt median: 28.26 s, range 16.90–51.41 s. The prepared
  simulator/verifier/Quizzes build was reused; the campaign had no global duration cap.
- w07 is the concrete additional calibration recommendation: 24 canonical action
  sequences, three positive, 21 zero and a SUCCESS/EXACT no-fault control. Its score
  signal is tied to a final SolveQuizAsync fault and a DELETED residual; it does not
  establish functional-defect discovery, interaction synergy or GA superiority.

Results: `verifiers/experiments/space-map/RESULTS.md`. Compact evidence:
`docs/verifiers-impl/evidence/space-map-2026-09-07/`. Raw evidence:
`verifiers/target/space-map/`. Canonical current-state and Outcome 6 roadmap were
updated after the parallel task's M1 commit, preserving that task's changes.

## Corrective follow-up

The generic adapter now recognizes direct facade calls in `setup()` as selected targets,
not only the earlier nested-reference subset. It emits a strict prefix before such a
target, shares that prefix with compatible later participants from the same source class,
and retains frontier provenance so the prefix cannot become an ambiguous setup for tuples
that do not select that target. No persisted schema, executor contract, event semantics,
ImpactV2 policy, or Quizzes application code changed.

The regenerated `w01` structural equivalent has a three-action materializable setup with
no AddStudent call. Docker replay confirms setup success and four completed forward steps;
the remaining INVALID result is solely the absent first QuizAnswer receiver. A separate
source-backed Tournament receiver completes `SUCCESS / EXACT`, and its trigger-fault
variant completes `COMPENSATED / EXACT` with a masked event. `w02` and `w07` also gain
strict-prefix setups, `w03`–`w06` retain their setup IDs, and `w08` remains blocked because
no single prefix can prepare all three selected setup targets.

Focused proof: 16 Spock tests pass in an isolated JDK 21 snapshot, including missing
exact-occurrence metadata with another materializable setup frontier. The complete
verifier suite executes 892 tests: 890 pass, while the two pre-existing
`ApplicationsFileTreeParserSpec` fixture-inventory assertions still omit
`com.example.dummyapp.diagnostics.ReadResponseFixture`. The qualified Docker
package contains 3,820 workloads and 12,395 initial FaultScenarios. Compact evidence and
hashes are in
`docs/verifiers-impl/evidence/space-map-setup-qualification-2026-09-07/`; raw artifacts
remain in `verifiers/target/space-map-setup-qualification-03/`. The original package,
selection, 159 discovery attempts, and sixteen repeats remain unchanged.
