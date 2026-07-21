# Remediation Review: 004 - Cross-Process On-Demand Mutation Safety

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes` (caller transition after PASS)

Safe for caller to move the card to done: `yes`

## Sources Reviewed

- Updated remediation and appended attempt-01 fix evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/004-cross-process-on-demand-mutation-safety.md`
- Parent spec, especially the on-demand data rule and AC-22: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Parent implementation plan and on-demand persistence mapping: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Independent follow-up/status: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Canonical context: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- Attempt-01 review, preserved unchanged: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-review/004-cross-process-on-demand-mutation-safety-review-01.md`
- Reviewed dependency baselines and latest PASS reports for remediations 001–003 under `remediation-done/` and `remediation-review/`
- Production and focused tests:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
  - `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioLockProcess.java`
- Live documentation:
  - `docs/verifiers-impl/current-state.md`
  - `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
  - `docs/verifiers-impl/reference/scenario-executor.md`
  - `docs/verifiers-impl/advisor-brief.md`
  - `docs/verifiers-impl/roadmap.md`
  - relevant historical/current evidence wording in `docs/verifiers-impl/evidence.md`
- Full uncommitted scope: 32 tracked changed files, the complete current 2,957-line / 199,224-byte tracked diff, and all 25 pre-review untracked files. Current tracked-diff SHA-256: `190ce6a1d62c486d244ccd1e4814a91bf497fe1205f06e07e0b43b55a10abbbd`.
- Attempt-01-to-attempt-02 delta: 604 diff-of-diff lines covering only the expected service, focused test, advisor, and roadmap corrections in the tracked diff; the active card and new Java subprocess helper are untracked artifacts reviewed directly.

## Summary

All three attempt-01 findings are resolved.

`PACKAGE_LOCK_FAILED` is now confined to provider/path open and handle acquisition failures. Once acquisition succeeds, the service executes the package request in an independent body phase: unexpected `RuntimeException` becomes `INTEGRITY_FAILURE / REQUEST_PROCESSING_FAILED`, while `Error` escapes unchanged. A `finally` always invokes handle close after successful acquisition, including when the body throws `Error`. The default NIO handle attempts both `FileLock.close()` and `FileChannel.close()`, preserving close diagnostics internally, while the service deliberately prevents cleanup exceptions from replacing the already established body/package result. Focused adversarial tests prove open failure, acquisition failure, null body result, escaping `AssertionError`, and close failure after a committed/finally validated revision; each applicable path preserves package bytes or validated publication truth and allows a later writer.

The separate-JVM regression no longer uses sleep or process-liveness inference. Two child JVMs wrap the real production `NioPackageLockProvider`; each writes `ready` immediately before its real `FileChannel.lock()` call, `acquired` only after that call returns, and `completed` only after the real service returns and result JSON is written. The parent owns the exact stable package lock path before launching the children, waits for both `ready` barriers, verifies its lock is still valid and no `acquired`/`completed` barrier exists, then releases. Both children subsequently acquire, return `PERSISTED`, and accumulate vectors `1001` and `0110`. The shared package reader validates all linked checksums and deterministic package consistency; the two accounting rows occur exactly once and manifest counts match.

Advisor and roadmap now preserve remediation 002's accepted historical/current distinction: the saved Quizzes report is pre-remediation evidence of old misclassification of unmarked service unavailability, while current execution would run no fallback or survivor continuation and report `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`. No stale current-classifier phrase remains in the scanned live docs.

No blocking, major, or non-blocking finding remains.

## Attempt-01 Finding Resolution

| Attempt-01 finding | Verdict | Evidence |
|---|---|---|
| Lock wrapper conflated acquisition, body, and close failures | resolved | Service lines 135–174 split open, acquire, body, and close phases. `PACKAGE_LOCK_FAILED` is returned only at open/acquire catches; body `RuntimeException` maps to `REQUEST_PROCESSING_FAILED`; acquired cleanup is in `finally`. Fresh five-test lifecycle gate passed. |
| Sleep/liveness-based cross-process proof | resolved | Service spec lines 550–612 uses ready/acquired/completed files and parent-owned real lock; no `Thread.sleep` or child `alive` assertion appears in the contention proof. Test helper lines 22–44 wraps the real `NioPackageLockProvider` and service. Fresh isolated run passed. |
| Advisor/roadmap contradicted remediation 002 | resolved | Advisor lines 11, 64, 91, and 104 plus roadmap lines 125–127 now identify historical unmarked service unavailability, old incorrect fallback, and current no-fallback/no-survivor `INCOMPLETE` behavior. Stale-phrase scan returned no matches. |

## Contract Compliance

| Requirement | Verdict | Evidence |
|---|---|---|
| Lock before package-content read | pass | Provider open/acquire occurs at service lines 140–145; `requestLocked` begins only after acquisition, and its first package read/validation remains inside the acquired body. Pre-lock work is package path/identity metadata only. |
| Lock held through final validation | pass | `requestLocked` includes staging, promotion/rollback, and final `readValidatedPackage(manifestPath)` before returning; the handle closes only in the enclosing `finally`. |
| `PACKAGE_LOCK_FAILED` limited to open/acquire | pass | Only the provider-open catch and handle-acquire catch call `packageLockFailure`; body exceptions use separate classification and cleanup exceptions are non-result-bearing. |
| Unexpected body failure truth | pass | Null generation result returns `INTEGRITY_FAILURE / REQUEST_PROCESSING_FAILED`, not lock failure. `AssertionError` escapes unchanged. Both paths release the real lock and permit a later successful request. |
| Acquired cleanup on every path, including `Error` | pass | The body is enclosed by `finally { closeIgnoringFailure(lockHandle); }`. Fresh escaping-body test throws `AssertionError`, then immediately reacquires the OS lock and persists with a later request. |
| Close failure cannot replace committed validation | pass | Close runs after the body result is established and checked cleanup exceptions are ignored. The injected-close test returns `PERSISTED`, shared-reader validation succeeds, the lock is reacquirable, and a second vector persists. |
| Real handle attempts lock and channel close | pass | `NioPackageLockHandle.close()` attempts `lock.close()` and then `channel.close()` even when the first reports `IOException`, retaining the first and suppressing the second before the service's non-result-bearing cleanup boundary. |
| Stable real-directory identity / aliases | pass | Existing normalized `toRealPath()` package identity still keys both `PACKAGE_LOCKS` and `.on-demand-fault-scenario.lock`; alias and same-process tests remain in the passing suite. |
| No-follow and non-regular lock path handling | pass | Default provider retains pre-open `NOFOLLOW_LINKS` shape validation, `CREATE/WRITE/NOFOLLOW_LINKS` open, post-acquisition regular-file validation, and no lock-file deletion. Symlink/directory tests preserve semantic bytes. |
| Genuine cross-process serialization | pass | Two separate helper JVMs signal immediately before/after the real production NIO lock call while the parent owns the same package-local inode; both acquire only after release and both complete successfully. |
| Accumulation and final integrity | pass | Final shared-reader load accepts checksums and consistency; both vectors exist, exactly two requested accounting rows exist with unique vectors, `computedOnDemandVectors == 2`, and exported scenario count matches records. |
| Per-package independence | pass | Existing two-package generation barrier remains green; locks are not global. |
| Caught rollback / hard-crash boundary | pass | Existing staging/promotion failure matrix remains green. Spec and live docs explicitly exclude crash atomicity, automatic recovery, network filesystems, and multi-host coordination. |
| Dependencies / protected artifacts | pass | Attempt 01 remains SHA-256 `6bd20983...`; remediation 001–003 history remains present; meeting-note mtimes and SHA-256 values are unchanged; no remediation item 5 work exists. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-22 deduplication and collision behavior | pass | Existing repeat request, deterministic mismatch, duplicate-id, and semantic-collision tests pass in the 48-test service suite. | No existing semantic behavior regressed. |
| AC-22 local cross-JVM serialization | pass | Deterministic ready/acquired/completed subprocess test around production NIO provider. | No sleep/liveness inference. |
| AC-22 successful accumulation | pass | Both child results are `PERSISTED`; final package contains both vectors and exact accounting with valid hashes/counts. | Second owner necessarily validates the revision published by the first while holding the same lock. |
| AC-22 exclusions | pass | Spec, ADR, current-state, reference, advisor, and roadmap reject hard-crash atomicity/recovery and network/multi-host guarantees. | Reader checksum protection remains the torn-package boundary. |
| AC-46 live documentation consistency for this remediation | pass | Docs build and stale/current wording scans pass. | Advisor/roadmap now match remediation 002 and lock limitations. |

## Verification Evidence Check

| Command / method | Verdict | Fresh result |
|---|---|---|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioCatalogJsonlWriterSpec test` | pass | `BUILD SUCCESS`; 69 tests, 0 failures, 0 errors, 0 skipped. Exact suites: service 48, package reader/writer 21. Total Maven time 35.806 s. Log SHA-256 `199d9d3403246287fe3efdf3ebe856be6f2bd54c650bc3254fd56bb081003840`. |
| `cd verifiers && mvn -Dtest='OnDemandFaultScenarioServiceSpec#separate*' surefire:test` | pass | 1 isolated cross-process test, 0 failures/errors/skips; test elapsed 6.001 s, Maven total 12.403 s. Log SHA-256 `f214f9d75b052979684beb997e7229b345fcd24d256464877d172976e83497fc`. |
| `cd verifiers && mvn -Dtest='OnDemandFaultScenarioServiceSpec#injected*+request*+escaping*+lock*' surefire:test` | pass | 5 lifecycle/adversarial tests, 0 failures/errors/skips: open failure, acquisition failure, body failure, escaping body `Error`, and post-commit close failure. Log SHA-256 `203b24cd7d7c5fc053ac39d355735c7167e82c7e86e9488b8f20eb1400499c7d`. |
| `./scripts/verifier-docs build` | pass | Documentation built successfully in 1.65 s. Only informational notices listed pages outside nav and the upstream MkDocs warning; no build error. |
| Stale/current classifier scan | pass | No stale current-domain/null-input/fallback phrase matched. Required historical unmarked-service-unavailability and current no-fallback/no-survivor `INCOMPLETE` wording is present in advisor, roadmap, current-state, evidence, and executor reference. |
| Contention/source structural probe | pass | Target test block contains both ready barriers, parent `heldLock.valid`, and absent acquired barriers; it contains no sleep or child-liveness condition. Helper directly constructs `NioPackageLockProvider`, calls delegate `acquire()`, and brackets it with ready/acquired/completed writes. |
| Required scoped `git diff --check -- ...` | pass | Exit 0, no output across export source, Groovy/Java export tests, docs, and issue paths. |
| Full diff/status/conflict/protected-file inspection | pass | 32 tracked changed files, 2,957 tracked diff lines, 25 pre-review untracked files, no conflict markers, no temporary probe residue, attempt 01 unchanged, meeting notes retain 2026-07-19 mtimes/hashes, and no item-5 artifact exists. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|---|---|---|
| Minimality | pass | One package-private provider/handle seam supports exact lifecycle testing; public/default behavior remains the NIO implementation. No dependency, schema, protocol, or executor change was introduced by this review fix. |
| Existing patterns | pass | Retains real-directory JVM guard, stable path, NIO lock, structured results, deterministic package reader, staged validation, and rollback. |
| Test quality | pass | Cross-process evidence uses explicit barriers around the real provider; lifecycle tests distinguish each failure phase and verify later progress/package truth. |
| Regression risk | pass | Full focused service/reader suites and isolated adversarial gates pass; merge, dedup, accounting, checksum, rollback, canonical ordering, alias, and package-independence coverage remains green. |
| Security/data safety | pass | Stable no-follow regular-file lock and checksum-validating reader remain intact; unsupported external inode replacement and distributed filesystems are documented non-guarantees. |
| Change hygiene | pass | Attempt 01, prior remediation history, historical artifacts, and meeting notes remain untouched; advisor/roadmap correction is limited to accepted current-truth wording. |

## Findings

None.

## Done Transition

- Moved to done: `yes`, by the caller after the reviewer returned `PASS`.
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/004-cross-process-on-demand-mutation-safety.md`
- Reason: the verdict is `PASS`; the caller completed the done transition while preserving the attempt history.

## Recommendation

Accept remediation 004 as review-complete. The caller may move the active card to `remediation-done/` while preserving this attempt history. Do not commit or start remediation item 5 as part of that transition.
