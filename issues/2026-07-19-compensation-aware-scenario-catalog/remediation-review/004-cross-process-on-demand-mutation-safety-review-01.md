# Remediation Review: 004 - Cross-Process On-Demand Mutation Safety

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no` (reviewer was explicitly instructed not to move the card)

Safe for caller to move the card to done: `no`

## Sources Reviewed

- Active remediation and completion evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/004-cross-process-on-demand-mutation-safety.md`
- Parent spec, especially the on-demand data rule and AC-22: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Parent plan and original on-demand slice mapping: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Independent follow-up/status: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Canonical context: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- Remediation dependencies and latest PASS reviews:
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md`
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md`
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/003-conservative-effect-free-proof.md`
  - matching latest reports under `remediation-review/`
- Item-004 production and tests:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
- Item-004 live documentation:
  - `docs/verifiers-impl/current-state.md`
  - `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
  - `docs/verifiers-impl/reference/scenario-executor.md`
  - `docs/verifiers-impl/advisor-brief.md`
  - `docs/verifiers-impl/roadmap.md`
- Full uncommitted state: all 32 tracked changed files and the complete 2,590-line / 177,668-byte tracked diff, plus all 22 untracked files. The tracked-diff snapshot SHA-256 is `2dce0236608e9fcec2a46f181859c52231476d4763f9feffdfcb603e38b338b1`.
- Protected meeting notes were not edited. Their retained mtimes are 2026-07-19 and current SHA-256 values are `8da2548d...` (`2026-W29-thesis-meeting.md`) and `7fd0ff54...` (`W28.md`).

## Summary

The core cross-process serialization design is mostly correct. One stable `.on-demand-fault-scenario.lock` path is derived from the normalized real package directory; `FileChannel.open(..., CREATE, WRITE, NOFOLLOW_LINKS)` and an exclusive `FileLock` occur before `readValidatedPackage`; the lock encloses generation, staging, promotion/rollback, and final validation; cooperating JVMs accumulate both updates; the lock file is retained and excluded from semantic package metadata; and the live lock-specific docs accurately exclude hard-crash recovery, network-filesystem correctness, and multi-host coordination.

The remediation cannot pass because the lock wrapper catches every `IOException` or `RuntimeException` from the complete locked request and labels it `PACKAGE_LOCK_FAILED`. That includes arbitrary request-body defects and exceptions thrown while closing `FileLock`/`FileChannel`, not only open/acquisition failures. A fresh adversarial probe made a body dependency return `null`; the service returned `PERSISTENCE_FAILED / PACKAGE_LOCK_FAILED` even though lock acquisition succeeded, semantic bytes were unchanged, and the lock was released. By direct control-flow inspection, the same outer catch can also replace an already computed `PERSISTED` result with `PACKAGE_LOCK_FAILED` if try-with-resources close throws after final validation. This violates two explicit review requirements and makes the current successful-return claim too strong.

The separate-JVM behavior passes repeatedly, but the checked-in blocking assertion is not deterministic evidence that either subprocess reached the OS lock: it sleeps 500 ms and checks only `Process.alive`. Fresh isolated runs took 13.36–19.25 seconds, so liveness after 500 ms can be explained by JVM startup or ordinary request work. The final accumulation assertions are meaningful, but the explicit “holder blocks waiter until release” proof required by the card needs a real cross-process readiness/contended-lock signal or deterministic process barrier.

The full-diff review also found stale present-tense Quizzes-classification claims in `advisor-brief.md` and `roadmap.md`, despite the accepted remediation-002 wording in current-state/reference/evidence. This is not a locking defect, but these live docs are changed in the same uncommitted diff and currently contradict that reviewed baseline.

## Contract Compliance

| Requirement | Verdict | Evidence |
|---|---|---|
| Lock acquired before package-content read | pass | `requestWithPackageLock` opens/acquires at service lines 130-132 and calls `requestLocked` only at line 136; the first `readValidatedPackage` is inside `requestLocked` at line 143. Pre-lock work is path/regular-file/real-directory metadata resolution, not package-content reading. |
| Lock held through final validation | pass | `publishRevision` performs final `readValidatedPackage(manifestPath)` at line 379 before returning through the still-active try-with-resources scope. |
| Separate JVM accumulation / no lost update | pass | Fresh 64-test run and three isolated cross-process runs passed; both subprocesses return `PERSISTED`, shared-reader validation accepts the final package, both vectors and both exact-once accounting rows remain, and manifest counts/hashes reconcile. |
| Stable identity and aliases | pass | `packageIdentity = packageRoot.toRealPath()` keys both `PACKAGE_LOCKS` and `<packageIdentity>/.on-demand-fault-scenario.lock`; accepted manifest aliases in one real directory use the same guard/path. The file is never deleted by production. |
| No-follow and non-regular handling | pass | Existing non-regular paths are rejected with `NOFOLLOW_LINKS`; the open also uses `NOFOLLOW_LINKS`; post-open path shape is rechecked. The docs correctly limit the claim to cooperating local processes and instruct operators not to delete the stable lock inode. |
| Every normal return/failure releases resources | pass with gap | Try-with-resources releases on handled body returns/failures; the adversarial probe confirmed release after an escaping body `NullPointerException`. Actual close-failure semantics are not correctly reported or tested. |
| Lock failures distinguished from body failures | fail | Outer catch at lines 100-104 encloses open, acquisition, the entire request body, and resource close. Probe result: `status=PERSISTENCE_FAILED code=PACKAGE_LOCK_FAILED semanticBytesUnchanged=true lockReleased=true` for a successful acquisition followed by a body failure. |
| Close errors cannot report failure after committed validation | fail | A `PERSISTED` result is returned from inside try-with-resources at line 136; a `FileLock.close()` or `FileChannel.close()` exception replaces that result and is caught as `PACKAGE_LOCK_FAILED`. Existing cleanup-failure coverage injects only temporary-file deletion failure, not lock/channel close. |
| Lock failure preserves semantic artifacts | pass | Shape-failure tests snapshot all three mutable semantic artifacts; the fresh body-failure probe also left them unchanged. The operational lock file is correctly outside semantic identity. |
| Caught rollback versus hard-crash/network/multi-host docs | pass | Spec AC-22/data rule, current-state, ADR, executor reference, advisor lock exclusions, and roadmap distinguish caught rollback from unsupported hard crash, no recovery, network filesystem, and multi-host coordination. |
| Dependencies and protected artifacts preserved | pass | Remediations 001–003 and their review history remain present; meeting-note mtimes/hashes are retained; no item-5 implementation was introduced. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-22 cross-process serialization and accumulation | pass | Genuine `java -cp ... FaultScenarioRequestCli` subprocesses, final shared-reader validation, both vectors, two exact-once rows, current manifest counts/hashes, and three repeated isolated passes. | Core AC-22 concurrency semantics work. |
| Remediation lock-failure classification | fail | Broad outer catch plus fresh null-generation-result probe. | Successful acquisition/body failure is mislabeled as lock failure. |
| Remediation post-commit result truth | fail | Try-with-resources close can override the computed successful result. | No injectable close-failure regression exists. |
| Remediation deterministic blocking evidence | fail | Checked-in test uses `Thread.sleep(500)` then only `first.alive` / `second.alive`; isolated method runs take much longer than 500 ms. | Accumulation is meaningful; explicit waiter-at-lock proof is not deterministic. |
| Documentation hard-crash boundary | pass | Current lock-specific docs consistently say writer-serialized, caught-failure rollback, checksum rejection of torn packages, no automatic recovery, and no network/multi-host guarantee. | Historical planning/slice names containing “atomic” remain provenance, not current operational guidance. |

## Verification Evidence Check

| Command / method | Verdict | Fresh result |
|---|---|---|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioCatalogJsonlWriterSpec test` | pass | `BUILD SUCCESS`; 64 tests, 0 failures, 0 errors, 0 skipped: service 43, reader/writer 21. Log SHA-256 `03810a2424f961c5bb4e38c4a7396f4204670700535d0fa033e2e5814a857633`. |
| `cd verifiers && mvn -Dtest='OnDemandFaultScenarioServiceSpec#separate*' surefire:test` repeated three times | pass | Each run executed 1 test with 0 failures/errors/skips. Test elapsed times: 15.22 s, 13.36 s, 19.25 s. Log SHA-256 values: `604facdb...`, `d52c3d17...`, `e930aa13...`. |
| Temporary out-of-tree lock-classification probe | fail as expected / finding reproduced | Injected `RecoveryScheduleSource` returned `null` after successful lock acquisition. Output: `status=PERSISTENCE_FAILED code=PACKAGE_LOCK_FAILED semanticBytesUnchanged=true lockReleased=true`. Probe source/classes were under `/tmp` and removed; no repository source/test file was added. Log SHA-256 `2d276adc24b2d643435f68bc24e209418d2d0dc6a1c2afc1bdd29d3eb08f21d3`. |
| Required scoped `git diff --check -- ...` | pass | Exit 0, no output. |
| Full status/diff/conflict/protected-file inspection | pass with documentation finding | 32 tracked changed files; 2,590 tracked diff lines; 22 untracked files; no conflict markers; no probe residue; protected meeting notes retained. The live-doc contradiction described below remains. |
| Initial Maven invocation from repository root | invocation error | The no-root-POM command was accidentally issued twice and failed before tests with `MissingProjectException`; it provides no verification evidence. All reported test counts above come from the corrected `cd verifiers` runs. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|---|---|---|
| Minimality | pass | Production lock change is localized to `OnDemandFaultScenarioService`; no dependency/schema/distributed protocol was added. |
| Existing patterns | pass | Retains normalized real-directory JVM guard, NIO APIs, structured results, deterministic package validation, and existing staged publication/rollback. |
| Test quality | fail | Cross-process accumulation is real and repeatedly green, but the explicit blocked-waiter assertion is sleep/liveness based; actual open/acquisition failure and lock/channel close failure are not injected. |
| Regression risk | fail | Broad exception scope can misreport body and post-commit close failures. |
| Security/data safety | pass with stated boundary | No-follow/non-regular checks and stable non-deleted lock path are appropriate for cooperating local writers. External replacement/deletion of the lock inode, network filesystems, and multi-host coordination are not guaranteed and are not claimed. |
| Change hygiene | fail | Lock-specific docs are precise, but the changed live advisor/roadmap retain stale current-classifier claims from the wider uncommitted diff. |

## Findings

### 1. BLOCKING — The lock wrapper conflates acquisition, request-body, and resource-close failures and can turn a committed success into `PACKAGE_LOCK_FAILED`

**Evidence**

- `OnDemandFaultScenarioService.java:100-104` catches `IOException | RuntimeException` around the whole call to `requestWithPackageLock`.
- `requestWithPackageLock` includes channel open, `channel.lock()`, the complete `requestLocked` body, and implicit `FileLock`/`FileChannel` close at lines 130-136.
- Fresh adversarial probe after successful acquisition produced:

```text
status=PERSISTENCE_FAILED code=PACKAGE_LOCK_FAILED semanticBytesUnchanged=true lockReleased=true
```

The injected body source returned `null`; no lock open/acquisition failure occurred.
- A close exception after `requestLocked` computes `PERSISTED` would unwind the try-with-resources and be converted by the same catch into `PACKAGE_LOCK_FAILED`, despite the already promoted and finally validated revision.

**Why it matters**

The remediation explicitly requires lock open/acquisition failures to be distinguished from arbitrary request-body failures and forbids resource-close errors from falsely reporting failure after a committed validated revision. A caller receiving false failure may retry a request that actually committed, and diagnostics currently point operators at the wrong subsystem.

**Required fix before PASS**

Narrow `PACKAGE_LOCK_FAILED` handling to lock path/open/acquisition failures only. Keep body result/failure classification independent after acquisition. Release lock/channel on every path, but do not replace an already committed/finally validated `PERSISTED` result with failure if release/close reports an error. Add deterministic injection coverage for (a) actual open failure, (b) actual acquisition failure, (c) an arbitrary body failure escaping normal body classification, and (d) lock/channel close failure after a committed validated revision. Assert semantic artifact bytes for each applicable path and assert that a later writer can proceed.

### 2. MAJOR — The checked-in “blocked on the OS lock” assertion is timing-based and does not prove either subprocess reached lock acquisition

**Evidence**

- `OnDemandFaultScenarioServiceSpec.groovy:560-564` sleeps 500 ms and checks only `first.alive` and `second.alive` while the test JVM holds the lock.
- Three fresh isolated executions passed, but the one-test elapsed times were 15.22 s, 13.36 s, and 19.25 s. A process being alive after 0.5 s therefore does not distinguish lock contention from JVM startup/class loading or ordinary request work.
- Final accumulation, exact accounting, and shared-reader validation are meaningful and passed; the narrow issue is the card's separate requirement to prove that a holder blocks a waiter until release.

**Required fix before PASS**

Replace sleep-plus-liveness as the contention proof with a deterministic cross-process barrier/readiness mechanism that establishes the child reached the service's lock acquisition and is waiting on the same lock inode before release. Keep the existing final accumulation, exact-once accounting, manifest, and shared-reader assertions. The regression must fail deterministically if the service stops participating in the OS lock, rather than depending on subprocess timing.

### 3. MAJOR (full-diff documentation hygiene) — Changed live docs still contradict remediation 002's explicit failure classifier

**Evidence**

- `docs/verifiers-impl/advisor-brief.md:11` calls the saved Quizzes execution an honestly reported current “zero-bit input-quality deviation,” and line 104 calls it a null-name “domain/simulator failure.”
- `docs/verifiers-impl/roadmap.md:127` likewise presents that smoke as a zero-bit null-input failure with fallback.
- The reviewed current truth in `current-state.md`, `evidence.md`, and `reference/scenario-executor.md` says the artifact is pre-remediation, its actual failure was unmarked service unavailability, and current behavior is infrastructure hard-stop with no fallback/survivor continuation.

**Why it matters**

Both files are present-tense live documentation changed by the full uncommitted diff. The contradiction weakens the explicit failure-classification baseline that this remediation is required to preserve. It is separate from the lock/crash wording, which is otherwise precise.

**Required fix before PASS**

Align the current advisor summary/evidence/limitation and roadmap current-state paragraph with the already reviewed historical-versus-current wording. Preserve the saved artifact and meeting notes unchanged.

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason: `FAIL`; blocking exception-scope/post-commit result semantics and required deterministic verification remain unresolved. The reviewer also followed the explicit instruction not to move the card.

## Recommendation

Do not move remediation 004 to done. Preserve all prior remediation changes and review history, fix only the findings above, append fix evidence to the active card, and request a new independent review attempt. Do not commit or begin remediation item 005 as part of that fix.
