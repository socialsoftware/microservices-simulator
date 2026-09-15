# Recovery-history fix and larger comparison

Implemented the approved bounded fix in the shared Saga compensation path. Compensation
commands run normally, but their semantic-state changes no longer append to forward undo
history. The scope crosses serialized command transport through `@JsonProperty` and is
restored in `finally`. Original rollback records, observable writes, genuine compensation
failures and the executor's completeness guard remain. Both ordinary abort and controlled
executor recovery are covered; no Quizzes business rule changed.

## Changed implementation

- `simulator/.../sagas/unitOfWork/SagaUnitOfWork.java`: compensation scope, transport and
  history guard.
- `simulator/.../unitOfWork/SagaStepwiseRecoveryTest.java`: ordinary/executor paths,
  failed/current steps, actual semantic-state restoration, retry and serialized transport.
- `verifiers/experiments/fixed-workload-ga/runtime.py` and `test_runtime.py`: independent
  APFS snapshot clones with ordinary-copy fallback, motivated by measured free disk.
- `verifiers/experiments/fixed-workload-ga/campaign.py`: bounded protocol launcher for
  matched pairs; retains status/evidence, checks frozen inputs and disk before each pair,
  stops subsequent pairs on a failed/short arm. No GA operator or fitness change.
- Canonical current-state/roadmap, original diagnosis and experiment README now link the
  completed qualification and active campaign.

## Proof

The initial regression run had five failures against the previous runtime. Final checks:
16 simulator tests (10 stepwise recovery, 6 executor control), 49 Python experiment tests,
and clean `git diff --check`. Final runtime overlay is `recovery-history-fix-03` with Java
parameter names retained and the serialized scope included. Earlier build diagnostics
remain retained and are excluded from final evidence.

Fresh Docker controls preserve the complete zero baseline and the count-one lost-copy
positive. The random pilot repeats the exact original twelve candidate keys: all seven
previously complete results retain their scores/components; the four pending-recovery
cases now have complete scores 2, 2, 0, 2; the real compensation failure remains null.
Random totals: six positives, five zeros, one unavailable. The separate GA pilot has three
positives, five zeros and four genuine compensation failures. All 144 report hashes and
package snapshots were checked; scored reports were revalidated and all 24 fitness values
recomputed. These pilots qualify runtime behavior and throughput, not search superiority.

## Continuing work

The approved campaign is running in a detached caffeinate-protected process: 500 attempts
per method, seeds 11/29/47, five unit weights, population 8, mutation probability 0.3,
recovery cap 500, two isolated fresh-runtime workers. Both first-seed arms produced their
first two complete observations before this handoff. Live authority:
`verifiers/target/ga-500x3-2026-09-15/status.json`, protocol and per-arm progress files.
Do not launch a duplicate. The 24-attempt paired pilot took 416.4 seconds, projecting
14.5 hours; allow 15–18 hours, dependent on machine availability and workload mix.

After completion, verify integrity and compare positive discovery curves, best score,
unavailable counts, duplicates and generation/runtime costs. There is no complete positive
reference map for the 5,184-candidate domain, so do not claim recall or time to all positives.
Do not turn failed compensation into zero impact. The evidence and machine-readable pilot
comparison are in `docs/verifiers-impl/evidence/recovery-history-2026-09-15/`.

No paper edits, commit, branch change, push, merge or old-evidence deletion performed.
