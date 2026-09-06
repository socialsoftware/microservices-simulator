# Semantic-state writes and complete recovery

The mapping-cycle follow-up is completed in
[observer coverage qualification](../2026-09-06-impact-coverage-controls/HANDOFF.md):
all 29 assessments are now complete, with the same execution and recovery outcomes.
The counts below preserve this earlier campaign's evidence.

The generator now recognizes a status write carried by a SagaCommand, even when its
payload only reads an aggregate. The executor also checks for remaining framework
recovery before declaring a failed participant compensated. These are two bounded
verifier corrections; application behavior and impact scoring rules are unchanged.

## What changed

For RemoveTournament, `getTournamentStep` reads the Tournament and then requests
`IN_DELETE_TOURNAMENT`. That second effect persists a status and saves its previous
value for rollback. The step therefore needs both READ and WRITE evidence and an
implicit rollback checkpoint. Names such as `READ_TOURNAMENT` and `NOT_IN_SAGA` do
not change the fact that a non-null requested status is persisted.

The visitor adds the write to existing dispatch footprints, on the payload's exact
aggregate/key. Existing conflict analysis and recovery generation consume it directly.
A plain read, a forbidden-state check alone, or a setter after a single dispatch does
not gain a status write. Aliases, reassignment, helper configuration and unsupported
control flow, custom subclasses and anonymous wrappers stay uncertain instead of being declared effect-free. This does not
attempt general Java dataflow analysis. See `STATIC-HANDOFF.md` for the source boundary.

The executor checks the framework's pending recovery list at the point it would
otherwise report `COMPENSATED`, including assigned faults and runtime fallback. If
work remains, it stops with `PENDING_RUNTIME_RECOVERY`, records incomplete execution,
and leaves the ImpactV2 score null. A failed discovery query has its own explicit
reason. It does not silently perform missing scheduled recovery. The regression tests
cover no planned checkpoint, one completed checkpoint with earlier work still pending,
a complete explicit/implicit recovery control, and failed discovery.

## Why the benchmark count changes

The historical package had 34 schedules. The earlier September 6 package had 17
because it omitted both the Tournament-read recovery and the ordinary User-read
recovery. That was an incorrect simplification for the Tournament step: the payload
was a read, but its surrounding SagaCommand wrote status.

The corrected package has **29 schedules for the same 12 fault vectors**. It restores
Tournament status recovery while continuing to omit a recovery checkpoint for the
ordinary User read. This is a change in the actions being explored, not a statement
that all historical schedules were equivalent. The earlier qualification reports are
preserved with a correction to their interpretation.

| Fault vector | Corrected schedules |
| --- | ---: |
| 00000 | 1 |
| 00001 | 1 |
| 00010 | 1 |
| 00100 | 6 |
| 00101 | 6 |
| 00110 | 3 |
| 01000 | 3 |
| 01001 | 3 |
| 01010 | 2 |
| 10000 | 1 |
| 10001 | 1 |
| 10010 | 1 |

The five bit positions remain: RemoveTournament reads Tournament, removes Quiz,
removes Tournament; AddParticipant reads User, adds participant. A 1 injects a fault
before that step runs. Recovery can be placed at different points among the other
participant's remaining actions, hence several schedules for some vectors.

## Validation

- Full verifier suite: **773 tests, 48 suites, zero failures/errors/skips**, JDK 21.
- Source review: PASS after correcting the bounded configuration edge cases in review.
- Fresh Docker benchmark: **29/29 corrected scenarios reach valid terminal outcomes**;
  all 23 applicable schedules execute successful implicit rollback for `getTournamentStep`.
- Old `01001` negative control: `UNEXPECTED_EXECUTION_FAILURE`, `INCOMPLETE`, and
  `PENDING_RUNTIME_RECOVERY` naming `getTournamentStep`; ImpactV2 is INVALID with null
  score and observed count. No unplanned recovery executes.
- Impact results: 19 COMPLETE (10 score 0, nine score 2), 10 PARTIAL (six observed lower
  bound 1, four lower bound 0). The known participant/answer persistent mapping cycle
  accounts for partial coverage. No corrected execution is INVALID or UNAVAILABLE.

The simulator source was unchanged by this correction, so its previously passing
suite was not repeated. All source work remains in the existing
`codex/potential-impact-v2` worktree, preserving inherited changes. No commit, merge,
push, or production application fix was performed.

## Reproduction and evidence

Artifacts are under `verifiers/target/semantic-lock-recovery/`:

- `generation-command.json`, `generation.log`, and `generated-corrected/`: current
  generation and package; six missing multi-fault vectors requested through the normal
  FaultScenarioRequestCli. The plan records package hashes and exact selected actions.
- `prepared-build/`, `prepare-command.json`, `prepare-build.log`, `source-hashes.json`:
  frozen Docker build using the existing broader-qualification preparation script.
  Every captured source hash was checked against the compiled source copy.
- `run-02/`: one fresh JVM/H2 per attempt, ordinary ScenarioExecutorCli, at most two
  concurrent attempts. Each directory retains its exact command, Docker log, execution,
  ImpactV1 and ImpactV2 reports. The old-package negative control is separate.
- `generated-final/`, `plan-final.json`, `generation-command-final.json`,
  `generation-final.log`, and `final-package-comparison.json`: regeneration after the
  final custom/anonymous-wrapper guard. All package file hashes and the manifest are
  identical to the executed package. Only the static visitor and its test changed
  after the frozen runtime build; executor, simulator and application production
  sources were verified unchanged. The 29 runs were therefore not repeated for an
  unsupported-wrapper guard with no effect on this application's generated package.
- `test-summary.json`, `final-surefire-reports/`, `verifiers-full-final.log`, and `summary.json`: integration proof.

An initial qualification preparation mistakenly requested eager vectors again, which
created additional IDs for identical action sequences. It was stopped and retained
under `generated/`, `preliminary-*`, and `run-01/`. The corrected campaign requests only
missing vectors and uses the 29 generated schedules without projection or deduplication.
Those preliminary attempts do not contribute to the reported results. The overlapping
request-ID behavior is a separate follow-up, not part of this correction.
