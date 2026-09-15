# Saga recovery history qualification

The shared `SagaUnitOfWork.compensateStepForExecutor` now scopes explicit compensation
execution. Both ordinary `compensateStep`/abort and controlled executor recovery use this
path. `savePreviousState` excludes compensation-issued changes from forward undo history;
the commands still run and their actual effects remain observable. Original records and
the final pending-checkpoint check are preserved.

The scope is restored in `finally`, including on failure and nested calls. The serialized
UnitOfWork carries the scope through its explicit Jackson property. Failed compensation
is still retryable. No Quizzes business code, fitness criterion, weight or GA operator
was changed.

Five added assertions initially failed against the old runtime. The final simulator
qualification passes 16 tests: ten stepwise-recovery cases and six executor-control cases.
Coverage includes ordinary abort and executor recovery, a failed later step, recovery of
the current step, original semantic-state restoration, failure/retry, resumption of normal
history recording and serialized command transport. The experiment suite passes 49 tests,
including independent snapshot copies and fallback when filesystem cloning is unavailable.

## Runtime evidence

Final frozen overlay: `verifiers/target/recovery-history-fix-03/`, based on the prior
integral-input runtime, with only SagaUnitOfWork and its nested classes overlaid. Java
compilation retains parameter names required by command deserialization.

The final no-fault four-Saga control remains SUCCESS/EXACT with five complete zero counts.
The retained lost-copy positive remains SUCCESS/EXACT, I=0, A=0, lost-copy count=1.
Paired twelve-attempt random/GA pilots use seed 15110 and five unit weights; their evidence
is under `pilot-random/` and `pilot-ga/`. Both finish at budget with integrity PASS.
The random candidate-key sequence exactly matches the original twelve-case pilot.

| Identical random cases | Before | After |
| --- | --- | --- |
| Previously scored seven | Three positives (2, 2, 1), four zeros | Same scores and components |
| Pending recovery: attempts 5, 7, 9, 12 | Four unavailable | Complete scores 2, 2, 0, 2 |
| Attempt 6 | Compensation failed after target deletion | Same failure; fitness unavailable |
| Total | Three positive, four zero, five unavailable | Six positive, five zero, one unavailable |

The GA pilot has three positive, five zero and four unavailable attempts; all unavailable
cases have COMPENSATION_FAILED after target deletion. Neither pilot has a remaining
PENDING_RUNTIME_RECOVERY blocker. This is throughput/correctness qualification, not a
GA benefit claim. All 144 retained report hashes and package snapshots were verified,
report identity joins revalidated where scores are available, and all 24 fitness results
recomputed. See [qualification.json](qualification.json).

The paired 24 attempts took 416.4 seconds (6.94 minutes). Linear extrapolation gives
14.5 hours for the 3,000-attempt campaign; allow 15–18 hours for variable runtime and
contention. This measures full fresh attempts and generation, not learning-operator cost
alone. The campaign started after this qualification, under caffeinate to prevent idle
sleep; its live status is the authority for completion.

Two early overlay-build control attempts under `recovery-history-fix-01` failed in setup
because manual javac compilation omitted `-parameters`. They are build diagnostics, not
application impact observations. `fix-02` controls passed but predate explicit serialized
scope support; final qualification uses `fix-03` only. Neither earlier overlay is used in
the main campaign.

## Main comparison protocol

`verifiers/target/ga-500x3-2026-09-15/protocol.json` predeclares 500 attempts per method,
matched seeds 11/29/47, five unit weights, population 8, mutation probability 0.3,
recovery cap 500 and two concurrent workers. The 3,000-attempt campaign excludes pilot
feedback and uses independent caches. `status.json` records whether it has started,
completed, or stopped; `progress.jsonl` in each arm records its measured attempts.

The research launcher runs one matched seed pair at a time, verifies frozen inputs/code,
and stops before subsequent pairs on failed/short runs or insufficient disk. APFS clones
share underlying immutable bytes while keeping independent files; other filesystems fall
back to ordinary copies. All snapshots and unavailable results remain retained. Domain
size is 5,184 enumerated candidates, not an exhaustive outcome map. Report discovery,
best score, unavailable attempts and timings; positive scenarios do not count distinct bugs.
