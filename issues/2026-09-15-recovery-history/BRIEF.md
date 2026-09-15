# Preserve forward undo history during Saga compensation

The user approved implementation after the isolated diagnosis and upstream comparison.
Keep the fix in the shared Saga compensation implementation and qualify both ordinary
abort and executor-controlled recovery. No Quizzes business rule changes are approved.

Compensation-issued semantic-state writes must not extend forward-step undo history.
Retain original records, execute compensation commands normally, restore the recovery
scope after exceptions, and preserve genuine failed-compensation retryability. The scope
must travel with serialized command UnitOfWork data. Retain the executor completeness
check and actual recovery-write observations.

Proof: failing/passing shared-runtime tests, serialized-scope test, fresh Docker controls,
and repetition of the twelve-case pilot containing the four pending-recovery failures.
Preserve the old evidence and distinguish later failures from the original stop.

Then qualify two-worker throughput and launch the approved 500-attempt-per-method
comparison over three matched seeds. Freeze weights and code, retain all unavailable
attempts, preserve per-attempt snapshots, and do not infer full-positive recall from the
enumerated domain size. APFS copy-on-write snapshot copies address the measured local
disk constraint while preserving independent replay files; no old evidence is deleted.
