# Synchronous Saga copy transport qualification

`qualify.py` freezes all 274 command-scope-only diagnostic rows before new results,
excluding four rows whose workload family explicitly contains `Async`. It keeps
all 270 synchronous candidates, including failures and unavailable feedback.

The historical runtime, clock, source-derived package, agent and application
remain frozen. Two freshly compiled classes precede the original classpath:
`CopiedUpdateSession` and `LostCopiedUpdateAssessor`. Their sources and class
hashes are retained under `verifiers/target/saga-copy-transport-2026-09-28-final/`.
Each attempt uses Docker Compose and a fresh process; three cohort attempts run concurrently,
with the base service's 2 CPU / 3 GiB limits. A 2 GiB free-disk floor stops new
attempts. No cluster or allocation-policy changes are involved.

From the repository root:

```sh
python3 verifiers/experiments/saga-copy-transport/qualify.py prepare
python3 verifiers/experiments/saga-copy-transport/qualify.py smoke
python3 verifiers/experiments/saga-copy-transport/qualify.py run
python3 verifiers/experiments/saga-copy-transport/controls.py
python3 verifiers/experiments/saga-copy-transport/audit.py
```

`run --limit N` bounds the number of additional attempts. Completed attempts,
including unavailable ones, are verified and skipped on resume. An interrupted
attempt directory is preserved and requires inspection before an explicit retry.
The source hashes must still match; source changes require a new campaign.

The initial campaign’s `attempt-001` contains three Compose startup failures caused by the
inherited `SPRING_PROFILES` variable. The override unsets that variable, as the
ordinary launcher does. These failures are retained; all measured cohort
results use `attempt-002`, without selecting retries by score. The initial
smoke selects one case per three families; additional controls select the
fewest injected faults by fault vector, before their new results.

`controls.py` replays the ten established forward/recovery histories with and
without serialization. `audit.py` verifies original reference and report
hashes, package/candidate identity, other criterion counts, and execution
status. Durable selection, results and audit are in
`docs/verifiers-impl/evidence/saga-copy-transport-2026-09-28/`.
The initial implementation campaign stopped after 32 completed cohort attempts when
review found a conservative regression in returned in-command copies. Its directory
and `initial-*` summaries remain separate. The final snapshot reruns the entire
fixed cohort; no result is carried over based on its reward.

Frozen maps and bandit replays are never rewritten.
