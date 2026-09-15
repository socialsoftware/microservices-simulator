# Recovery attribution diagnosis and larger-run estimate

## Observed failure

Random calibration attempts 5, 7, 9 and 12 all end with
`PENDING_RUNTIME_RECOVERY` for `leaveTournamentStep`. AddParticipant was faulted;
LeaveTournament first reads the Tournament with a semantic state and then correctly
rejects the absent participant. Its read-step recovery reports both explicit compensation
and implicit rollback as successful, but the final checkpoint query reports new work for
the failed leave step. Attempt 6 is different: a compensation fails because its Tournament
has been deleted. That failure is not explained by this attribution defect.

## Cause in the runtime

`SagaStep.execute` sets `currentExecutingStep` before invoking a step. After the rejected
leave operation, this field still names `leaveTournamentStep`.

`ScenarioExecutor.recoverAfterRuntimeFailure` obtains the pending checkpoints and calls
`SagaUnitOfWorkService.recoverStepForExecutor` for the earlier read. That method runs the
explicit compensation, whose SagaCommand changes semantic state. `registerSagaState`
calls `savePreviousState`; the latter indexes its history by `currentExecutingStep`.
The recovery-generated state record is therefore attached to the failed leave step.
That step was not pending when the executor obtained its checkpoint list, so it is left
pending after recovery of the read completes. The executor's final check exposes the
inconsistency and correctly refuses to treat the attempt as fully recovered.

## Isolated confirmation

`verifiers/target/recovery-attribution-diagnosis-2026-09-15/RecoveryAttributionProbe.java`
was compiled and run in the same frozen Docker image/classpath as the qualification.
It uses the actual SagaUnitOfWork and recovery-service implementations. The command's
state-history operation and aggregate-state storage are modeled in memory; this is a
mechanism reproduction, not a fresh end-to-end Quizzes run or a shipped fix.

| Variant | Pending work afterwards | Final modeled semantic state |
| --- | --- | --- |
| Current code | Failed leave step | Original state restored |
| Only retag recovery with the read step | None | Incorrect READ state restored |
| Exclude recovery-generated records from forward undo history | None | Original state restored |

Retagging alone adds the compensation's pre-state to the original read's rollback list.
Rollback then applies that new record after the original one, reintroducing the read
state. The proposed fix must keep recovery mutations out of forward undo history, while
preserving the existing original records, observable recovery writes and retryable genuine
failures. It must not simply clear pending checkpoints or weaken the completeness check.

The subsequent [implementation and replay](../recovery-history-2026-09-15/README.md)
complete this diagnosis. All four formerly pending-recovery cases now receive complete
scores (2, 2, 0, 2). The separate genuine compensation failure remains unscored. Original
pilot artifacts remain unchanged; the new evidence uses a separately frozen runtime.

## Upstream check and implemented change boundary

`git fetch origin master` on 2026-09-15 retrieved
`7f94539f7abe95d41893435898254258a5ecdec1` (2026-09-09). No checkout, merge or
application-code change was made. That master still records semantic-state changes using
`currentExecutingStep`, including the unlock command in LeaveTournament's compensation.
The latest relevant upstream recovery change is `49a67447c` (2026-07-02).

However, master does not contain `recoverStepForExecutor`, the pending-checkpoint query,
or the verifier's completeness check. Our branch added step recovery in `e72a081e6`
(2026-07-20), the pending-checkpoint query in `75b39e65a`, and the final
`PENDING_RUNTIME_RECOVERY` guard in `6e5949a5f`. Master's abort loop visits every executed step, including
failed steps with no state records, whereas the executor discovers only currently pending
work. Therefore this evidence establishes an integration defect exposed by our executor;
it does not establish that ordinary Quizzes execution on master produces the same terminal
failure. Updating from master would not supply a fix for this path.

The implemented bounded change scopes explicit compensation in the shared SagaUnitOfWork
path. It excludes compensation-issued semantic-state records from forward undo history,
preserving original records, actual commands, failure retryability and the final
completeness check. Both ordinary abort and executor recovery call this method; the final
implementation therefore covers both, rather than maintaining different compensation
bookkeeping rules. Tests cover both paths and serialized UnitOfWork transport.

The user authorized the fix and continuation of the comparison. The 500-by-three campaign
has started; its protocol/status and the updated measured cost are linked in the
[qualification report](../recovery-history-2026-09-15/README.md).

## Original cost estimate and recommendation

The twelve-case random calibration took 372.96 seconds in total (31.08 seconds per
attempt); its generation requests account for 22.68 seconds. These measurements include
fresh Docker/JVM/H2 attempts. They are not a measured two-worker throughput benchmark.

| Attempts per method per seed | Matched seeds | Total attempts, GA plus random | Sequential extrapolation | Ideal two-worker extrapolation |
| --- | --- | --- | --- | --- |
| 500 | 1 | 1,000 | 8.6 h | 4.3 h |
| 500 | 3 | 3,000 | 25.9 h | 13.0 h |
| 1,000 | 3 | 6,000 | 51.8 h | 25.9 h |

Recommend 500 attempts per method with three matched seeds after the recovery fix is
qualified. This supplies a 500-execution horizontal axis for each run; repetitions measure
variation. Planning allowance for two workers is roughly 15–18 hours, conditional on
throughput remaining near the pilot. Contention, longer histories, GA proposal overhead,
timeouts and package growth can change the estimate. Measure two-worker throughput before
launching the complete campaign. No main comparison was launched by this investigation.

Keep fixed weights and common budgets, count unavailable attempts against the budget,
and show positive discoveries, best score and unavailable counts. The 5,184 candidates
are enumerated possibilities; their full outcome map is not known, so the curve cannot
claim a fraction of all positives found. This workload currently exercises persistent
effects; complementary read/lost-copy workloads remain necessary for anomaly claims.
