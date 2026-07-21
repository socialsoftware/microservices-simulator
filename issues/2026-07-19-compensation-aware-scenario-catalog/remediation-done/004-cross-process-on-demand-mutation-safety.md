# Remediation 004 - Cross-Process On-Demand Mutation Safety

Parent spec: `../spec.md`
Independent review follow-up: `../independent-review-follow-up.md`
Depends on:
- `../remediation-done/001-shared-reader-checksum-verification.md`
- `../remediation-done/002-explicit-domain-failure-classification.md`
- `../remediation-done/003-conservative-effect-free-proof.md`
Risk: `high`
Status: `review-complete`

## Finding

`OnDemandFaultScenarioService` serializes requests with a static in-memory map and Java `synchronized`. That protects callers sharing one JVM, but the documented `FaultScenarioRequestCli` runs in a separate JVM for each invocation.

Two processes can therefore read the same package revision, generate different vectors, and publish independently. The later publication can overwrite the earlier process's additions without reporting an error. The package can remain valid while silently losing an accepted request.

The service also promotes the FaultScenario catalog, accounting, and manifest one file at a time. Its rollback protects caught publication failures, but an abrupt process termination or host failure between replacements cannot run that rollback and can leave a checksum-inconsistent package. Current documentation overstates this as broadly atomic persistence.

## Plain-Language Rule

Every on-demand mutation of one package must have one OS-visible writer at a time. The writer must acquire the package lock before reading or validating the revision used to generate and publish its changes. A waiting writer must then re-read the revision after acquiring the lock, so independently requested vectors accumulate rather than overwrite one another.

The supported guarantee is:

- concurrent JVMs are serialized per real package directory;
- validation failures and caught generation/publication failures preserve or restore the prior package bytes;
- successful requests publish a validated revision before releasing the lock;
- abrupt process termination, JVM crash, kernel failure, or power loss during the multi-file promotion is not crash-atomic and has no automatic recovery in this PR.

The CLI and current docs must state that hard-crash limitation directly. They must not describe the current three-file replacement as transactionally or crash atomic.

## Concrete Bad Example

Process A requests vector `1001` while process B requests vector `0110` against the same manifest:

```text
A reads revision R
B reads revision R
A publishes R + 1001
B publishes R + 0110
```

Without a process-shared lock, the final valid package can omit `1001` even though A returned `PERSISTED`.

Required ordering:

```text
A acquires package lock
A reads R and publishes R + 1001
A releases package lock
B acquires package lock
B reads R + 1001 and publishes R + 1001 + 0110
B releases package lock
```

Request order does not need to be predetermined. Both successful vectors must be present, accounting must contain both computed-vector rows, and the final manifest hashes/counts must validate.

## Required Behavior

- Retain the existing normalized real package-directory identity and in-process guard.
- Add a dedicated, stable lock file inside that real package directory and hold an exclusive OS-level `FileChannel` lock across the complete read/validate/generate/publish operation.
- Acquire the OS lock before `readValidatedPackage`; no package content used for mutation may be read before acquiring it.
- Keep the lock file stable; do not delete it after each request because replacing/deleting the lock inode can let processes lock different files.
- Open the lock file without following symbolic links. Reject a pre-existing symlink, directory, or other unsupported lock-file shape without mutating package artifacts.
- Ensure package aliases resolving to the same real directory use the same lock file.
- Release the file lock and channel on every result path, including validation, generation, publication, and injected failure paths.
- A lock acquisition/open failure must return a structured unsuccessful result and leave semantic package artifacts unchanged.
- Preserve existing deterministic merge, deduplication, collision, checksum, cap, path-containment, staged validation, rollback-on-caught-failure, and canonical ordering behavior.
- Do not add stale-lock deletion, polling protocols, lease timeouts, host-distributed locks, or lock breaking. OS lock release on process exit is the boundary.
- Do not claim hard-crash recovery or crash-atomic multi-file publication.

A package-local lock file is operational metadata, not a semantic package artifact. It must not enter the manifest, checksums, deterministic ids, or scenario-space accounting.

## Narrow Implementation Boundary

Expected production scope:

- `OnDemandFaultScenarioService` package-lock acquisition/lifetime and structured lock failure;
- focused `OnDemandFaultScenarioServiceSpec` coverage, including a genuine separate-JVM boundary;
- current spec/ADR/reference/current-state/advisor/roadmap wording that presently calls on-demand publication atomic without qualification;
- concise lock-file and hard-crash operational documentation.

Out of scope:

- immutable revision directories or a current-revision pointer;
- write-ahead journals or automatic package recovery;
- schema/version changes;
- changes to package semantic identity or artifact lists;
- remote/distributed filesystem guarantees beyond the local OS `FileChannel` contract;
- executor runtime changes;
- remediation item 5.

## Focused Tests

At minimum prove:

1. two genuinely separate JVMs requesting different vectors against one package cannot lose either update;
2. both subprocess requests succeed, the final package passes `ScenarioCatalogPackageReader`, both vector scenario sets exist, both accounting rows exist exactly once, and manifest counts/hashes are current;
3. the second writer reads the first writer's published revision while holding the lock rather than relying on a pre-lock snapshot;
4. a process/JVM holding the package lock blocks another request until release;
5. two different package directories can still mutate independently;
6. same-process callers and manifest aliases remain safely serialized;
7. a pre-existing symlink or non-regular lock path is rejected before semantic artifact mutation;
8. lock-open/acquisition failure returns a structured unsuccessful result and preserves mutable artifact bytes;
9. validation/generation/publication early returns and failures release the lock so a later request can proceed;
10. the persistent lock file is absent from manifest metadata and does not alter deterministic package bytes beyond the three intended mutable semantic artifacts;
11. all existing handled publication-failure rollback tests remain green.

Use TDD where practical. Capture a deterministic pre-fix regression or probe showing that separate processes can overlap or lose an update; do not make the test depend only on the existing static JVM lock.

## Documentation Contract

Current wording such as `atomic on-demand persistence` must be replaced with precise language. Document all of these distinctions:

- package writers are serialized across local JVM processes with a stable OS lock;
- successful return means the resulting package was validated;
- invalid requests and caught failures preserve/restore prior bytes;
- readers remain checksum-protected and reject a torn package;
- a hard crash during multi-file replacement can leave an invalid package;
- there is no automatic crash recovery; regenerate the package before retrying if integrity validation fails after such a crash;
- this does not establish network-filesystem or multi-host distributed locking guarantees.

Historical evidence artifacts and the two pre-existing untracked meeting-note files must not be edited.

## Verification

Run from the verifier module:

```bash
cd verifiers
mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioCatalogJsonlWriterSpec test
```

Run any narrow separate-JVM probe needed to establish the actual file-lock boundary. Then run scoped hygiene across every changed path:

```bash
git diff --check -- \
  verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export \
  verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export \
  docs/verifiers-impl \
  issues/2026-07-19-compensation-aware-scenario-catalog
```

## Evidence to Record

Append completion evidence to this file with:

- implementation summary and exact lock lifetime;
- lock-file path/name and symlink/non-regular-file behavior;
- exact files changed;
- pre-fix and post-fix separate-JVM evidence;
- focused test commands and exact counts/results;
- documentation claims changed;
- deviations or remaining concerns.

Do not commit, move this remediation card, edit prior remediation review reports, begin item 5, or touch the two pre-existing meeting-note files. Leave review and the done transition to the orchestrator.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Retained the normalized real-package-directory in-process guard and added a stable package-local exclusive OS lock at `<real-package-directory>/.on-demand-fault-scenario.lock`.
- The service resolves only package boundary metadata before locking. Inside the existing per-real-directory JVM guard it opens the lock file with `CREATE`, `WRITE`, and `NOFOLLOW_LINKS`, acquires `FileChannel.lock()`, and only then calls `readValidatedPackage`.
- The file lock and channel remain held across package read/validation, request validation, deterministic generation/deduplication, snapshots, staging, three-file promotion or caught-failure rollback, final package validation, and every success/failure return from `requestLocked`. Try-with-resources releases both on all handled paths and on process exit.
- Lock open/acquisition/shape failures return structured `PERSISTENCE_FAILED / PACKAGE_LOCK_FAILED` results before semantic package artifacts are read or mutated.
- The lock file remains in place after requests. It is operational metadata only: it is absent from manifest artifact metadata, checksums, ids, and accounting.
- No revision directories, current-revision pointer, journal, stale-lock deletion, polling, lease, lock breaking, or crash recovery were added.

### Lock Path and Shape Contract

- Path: `<packageIdentity>/.on-demand-fault-scenario.lock`, where `packageIdentity` is the normalized real parent directory already used by `PACKAGE_LOCKS`.
- Package-directory aliases therefore converge on the same JVM guard and lock path.
- A pre-existing regular lock file is reused. A pre-existing symbolic link, directory, or other non-regular shape is rejected. `LinkOption.NOFOLLOW_LINKS` is also passed to `FileChannel.open`, so opening does not follow a lock-path symlink.
- The file is intentionally never deleted between requests; deleting/replacing it could allow writers to lock different inodes.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java` — stable no-follow `FileChannel` lock acquisition, lifetime, and structured lock failure.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy` — genuine separate-JVM serialization/accumulation probe plus lock-shape, release, package-independence, and metadata coverage.
- `docs/verifiers-impl/current-state.md` — exact local cross-JVM guarantee, lock lifetime, successful-validation meaning, caught-failure behavior, reader protection, and hard-crash/network-filesystem limits.
- `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md` — replaced atomic revision wording with the accepted writer-serialization and non-crash-atomic contract.
- `docs/verifiers-impl/reference/scenario-executor.md` — CLI operation, persistent lock path, no-follow shape rule, recovery instruction, and OS/filesystem boundary.
- `docs/verifiers-impl/advisor-brief.md` — removed unqualified atomic claims and added crash/network exclusions.
- `docs/verifiers-impl/roadmap.md` — replaced atomic current-state wording and recorded hard-crash/recovery/filesystem limitations.
- `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md` — orchestration-level clarification of the approved local cross-JVM serialization guarantee and explicit hard-crash/network-filesystem exclusions in the data rule and AC-22.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/004-cross-process-on-demand-mutation-safety.md` — this completion evidence and orchestration note.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` (pre-fix red) | FAIL (expected) | 43 tests ran; 4 failures. Two real CLI subprocesses both returned success while the final valid package retained `0110` but lost accepted vector `1001`, proving the static JVM guard did not protect separate processes. Symlink and directory lock-path iterations incorrectly returned `PERSISTED`. The lock-release probe found no lock file because production had no OS lock. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` (first green) | PASS | 43 tests run, 0 failures, 0 errors, 0 skipped. Both subprocesses waited behind an externally held OS lock, then returned `PERSISTED`; the validated final package contained both requested vectors and exactly one accounting row for each. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioCatalogJsonlWriterSpec test` (final after test cleanup) | PASS | 64 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. Exact counts: on-demand service 43, package reader/writer 21. Existing handled publication rollback coverage remains green. |
| Genuine separate-JVM probe embedded in `OnDemandFaultScenarioServiceSpec` | PASS | Two `java -cp <surefire test classpath> FaultScenarioRequestCli ...` processes target one package while the Spock JVM holds the same lock inode. Both remain alive while blocked; after release both exit 0, final shared-reader validation succeeds, vectors `1001` and `0110` both exist, both accounting rows occur once, and manifest counts/hashes are current. |
| Required scoped `git diff --check -- ...` plus appended-evidence trailing-whitespace scan | PASS | No errors in the tracked scoped diff; the appended evidence has no trailing whitespace. The card's three pre-existing Markdown hard-break lines remain unchanged. |

### Acceptance Criteria Evidence

- Separate-process accumulation and post-lock reread: the forked CLI regression loses one successful update before the fix and retains both after it. Since both subprocesses are released against one package and the second validated revision contains the first vector, no pre-lock package snapshot can drive the second publication.
- Blocking boundary: both separate JVMs remain blocked while the test JVM owns `.on-demand-fault-scenario.lock`, directly proving OS-visible lock participation rather than shared-thread/static-map behavior.
- Final integrity: `ScenarioCatalogPackageReader` accepts the final package; both vectors have complete FaultScenario sets, their computed-vector rows occur exactly once, `computedOnDemandVectors == 2`, and exported scenario counts match.
- Independence: two package directories enter injected generation concurrently, proving locking remains per real package directory.
- Same-process/alias behavior: existing package-local concurrent and manifest-alias tests remain green under the retained static guard plus real-directory lock identity.
- Unsupported/open failure: symlink and directory lock paths return structured `PACKAGE_LOCK_FAILED` and preserve all three mutable semantic artifact byte snapshots.
- Release paths: an early invalid-vector return and an injected post-accounting-promotion failure/rollback both permit immediate `tryLock()` acquisition and a later successful request.
- Operational metadata: the persistent regular lock file exists after success but its name is absent from the manifest. Existing canonical-order/request-order and immutable workload/rejected-artifact assertions remain green.
- Rollback: every existing staging/promotion boundary failure test remains in the passing 43-test service suite and restores original mutable bytes.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the real subprocess and lock-shape/release regressions before production locking. The deterministic pre-fix failure included actual lost-update evidence from two successful separate JVM CLI requests, not only threads sharing `PACKAGE_LOCKS`.
- The first attempted Maven command was accidentally issued from the repository root and failed immediately because this repository has no root POM. The required red and all green runs were then executed from `verifiers/`.

### Documentation Claims Changed

- Replaced “atomic on-demand persistence/revisions” wording with local cross-JVM writer serialization using one stable real-package-directory lock.
- Stated that successful return follows final package validation; invalid requests and caught failures preserve/restore prior semantic bytes; checksum-verifying readers reject torn packages.
- Stated that separate replacement of FaultScenario JSONL, accounting, and manifest is not hard-crash atomic, has no automatic recovery, and requires package regeneration before retry after post-crash integrity failure.
- Stated that local `FileChannel` behavior does not establish network-filesystem correctness or multi-host distributed locking.

### Orchestration-Level Spec Clarification

- The implementation worker correctly left `spec.md` unchanged under its worker contract. Before independent review, the orchestrator updated the parent data rule and AC-22 to encode the approved boundary: local cross-JVM writer serialization from pre-read validation through validated publication, no lost successful updates, caught-failure preservation/rollback, and explicit exclusion of hard-crash atomicity, automatic crash recovery, network-filesystem locking, and multi-host coordination.

### Deviations From Plan

- No implementation deviation.

### Blockers / Follow-Ups

- None for review. The intentional limitation is unchanged: abrupt process/JVM termination, kernel/host failure, or power loss during separate three-file promotion can leave a checksum-invalid package. There is no automatic recovery; regenerate before retry. Lock semantics on network/distributed filesystems are outside the supported guarantee.

## Review Attempt 01 Fix Evidence

Status: `implemented-awaiting-review`

### Fix Summary and Exact Failure Rules

- Introduced the smallest lock lifecycle seam: package-private `PackageLockProvider.open(Path)` returns a `PackageLockHandle` with separate `acquire()` and `close()` phases. Public/default construction still uses `NioPackageLockProvider`; no production CLI behavior changed.
- `PACKAGE_LOCK_FAILED` is now produced only when lock-path/provider open fails or when the opened handle's acquisition fails. Acquisition failure always attempts handle close before returning.
- After successful acquisition, an unexpected request-body `RuntimeException` is independently classified as `INTEGRITY_FAILURE / REQUEST_PROCESSING_FAILED`; it is never relabeled as a lock failure. Errors outside that classification still escape truthfully.
- A `finally` closes the acquired handle on every body return and escaping path. Close/release exceptions are intentionally ignored after the body outcome is known, so they cannot replace a finally validated `PERSISTED` result or any truthful body result.
- The real NIO handle attempts both `FileLock.close()` and `FileChannel.close()`, preserving the first close exception only for its caller; the service suppresses that cleanup exception after an acquired request result is established.
- The real NIO provider retains the stable no-follow path checks, `CREATE/WRITE/NOFOLLOW_LINKS` open, exclusive blocking `FileChannel.lock()`, post-acquisition regular-file check, and stable undeleted inode contract.

### Deterministic Cross-Process Barrier

- Added test-only `OnDemandFaultScenarioLockProcess`, which uses the production `NioPackageLockProvider` and real service request path.
- Its observer writes `ready` immediately before the production `FileChannel.lock()` call. Its wrapping handle writes `acquired` only after that real call returns, and the helper writes `completed` only after the service has returned and the result JSON is durable.
- The parent opens and exclusively locks the exact package lock path before starting either child, waits for both independent `ready` files, verifies its `FileLock` is still valid, and verifies neither child has emitted `acquired` or `completed`. It then releases the lock and requires both children to acquire, complete, and return `PERSISTED`.
- No sleep or process-liveness condition is used as contention evidence. The short polling loop only waits for explicit cross-process readiness files with a bounded deadline.
- Final assertions retain shared-reader validation, both requested vectors, exactly one accounting row per vector, current computed-vector/exported-scenario counts and manifest hashes, and lock-file exclusion from semantic metadata.

### Files Changed for Attempt 01

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java` — split open/acquire/body/close phases, narrow diagnostics, cleanup-result semantics, and default NIO provider/handle.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy` — deterministic process barrier plus open, acquisition, body, escaping-body, and close-failure adversarial coverage.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioLockProcess.java` — test-only separate-JVM helper around the real NIO provider and service.
- `docs/verifiers-impl/advisor-brief.md` — classify the saved Quizzes execution as pre-remediation unmarked service-unavailability history and state current no-fallback/no-survivor `INCOMPLETE` behavior.
- `docs/verifiers-impl/roadmap.md` — align current executor and saved-smoke wording with the explicit `DomainFailure` contract and historical/current boundary.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/004-cross-process-on-demand-mutation-safety.md` — this attempt-01 evidence only.

### Adversarial Coverage

- Open failure: injected `PackageLockProvider.open` throws; result is `PERSISTENCE_FAILED / PACKAGE_LOCK_FAILED`, semantic bytes are unchanged, and a default later writer persists successfully.
- Acquisition failure: open returns a handle whose `acquire` throws; service invokes its close exactly once, returns `PACKAGE_LOCK_FAILED`, preserves semantic bytes, and a later writer succeeds.
- Body failure after acquisition: generation returns null after real NIO acquisition; result is `INTEGRITY_FAILURE / REQUEST_PROCESSING_FAILED`, semantic bytes remain unchanged, the OS lock is immediately reacquirable, and a later writer succeeds.
- Escaping body failure: generation throws `AssertionError`; the same error escapes unchanged, `finally` releases the real OS lock, and a later writer succeeds.
- Close failure after commit: a wrapper first closes the real NIO lock/channel and then throws; the already committed and shared-reader-valid revision still returns `PERSISTED` with no diagnostic, the lock is reacquirable, and a second vector persists and validates.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| Review-attempt-01 adversarial baseline | FAIL reproduced by reviewer | A null generation result after successful acquisition was previously mislabeled `PERSISTENCE_FAILED / PACKAGE_LOCK_FAILED`; control-flow inspection showed close could replace committed success. The checked-in contention test used sleep/liveness only. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioCatalogJsonlWriterSpec test` (first post-fix run) | PASS | 68 tests run, 0 failures, 0 errors, 0 skipped: service 47, reader/writer 21. |
| `cd verifiers && mvn -Dtest='OnDemandFaultScenarioServiceSpec#separate*' surefire:test` | PASS | Isolated deterministic cross-process test: 1 test, 0 failures/errors/skips; both children reached the pre-lock signal while blocked on the parent-owned inode, then accumulated both revisions after release. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioCatalogJsonlWriterSpec test` (final after escaping-error coverage) | PASS | 69 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. Exact counts: service 48, reader/writer 21. |
| Required scoped `git diff --check -- ...` | PASS | No whitespace errors in changed export source, Java/Groovy tests, current docs, or tracked issue diff. |
| Live-doc stale-claim scan | PASS | Advisor and roadmap now identify unmarked service unavailability, pre-remediation historical evidence, no fallback/survivor continuation, and current `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`; saved artifacts remain untouched. |

### Documentation Alignment

- `advisor-brief.md` no longer calls the saved execution a current meaningful input-quality/domain deviation. It states the actual unmarked service-unavailability failure, old misclassification, current infrastructure hard stop, and lack of a post-remediation domain-fallback smoke.
- `roadmap.md` now states that only explicitly marked domain failures use fallback. It labels the saved smoke historical and states current no-fallback/no-survivor `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` behavior.
- No saved evidence artifact, meeting note, parent spec, or review report was edited.

### TDD / Review-Fix Notes

- The independent review's deterministic null-generation probe and control-flow finding supplied the red baseline. Fix tests were added directly against each required lifecycle phase before final verification; no unrelated behavior was changed.

### Deviations / Remaining Concerns

- No deviation from the approved review fix. Cleanup failures are deliberately non-result-bearing once acquisition succeeded; callers receive the package/body truth, while process exit and the real handle's dual close attempts remain the cleanup boundary.
- Hard-crash atomicity, automatic recovery, network filesystems, multi-host coordination, and external deletion/replacement of the stable lock inode remain intentionally unsupported as previously documented.

## Review Outcome

- Attempt 01: `FAIL` — lock errors were conflated with body/close failures, the subprocess contention assertion was timing-based, and changed live docs retained stale failure-classification wording. See `../remediation-review/004-cross-process-on-demand-mutation-safety-review-01.md`.
- Attempt 02: `PASS` — all findings are resolved, with no blocking, major, minor, or note finding remaining. See `../remediation-review/004-cross-process-on-demand-mutation-safety-review-02.md`.
- Fresh reviewer verification: 69 focused tests, one isolated cross-process test, five targeted lock-lifecycle tests, the documentation build, stale-claim scans, and scoped diff hygiene all passed.
- Final state: remediation item 4 is review-complete; no commit has been made.
