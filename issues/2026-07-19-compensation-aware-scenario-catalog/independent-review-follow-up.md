# Independent Review Follow-up

PR: `#15` — `feat(verifiers): add compensation-aware scenario catalog`

Status: all five independent-review remediation items are implemented and review-complete; final merge validation remains.

## Discussion Order

1. Package checksum verification in the shared package reader.
2. Domain-failure versus infrastructure-failure classification.
3. Conservative proof before omitting compensation checkpoints as effect-free.
4. Cross-process and crash boundaries of on-demand package mutation.
5. Repeated runtime step occurrences versus exact replay support.

## Working Method

For each item:

1. Explain the current behavior in simple terms with a concrete example.
2. Confirm the intended contract and scope with the user.
3. Choose the smallest safe fix.
4. Add a focused regression test that fails before the fix.
5. Implement only that fix.
6. Run the narrow relevant test set and record the result here.
7. Continue to the next item only after the user agrees.

After all agreed fixes:

1. Run the complete simulator test suite.
2. Install the simulator dependency used by the verifier.
3. Run the complete verifier test suite.
4. Run the Docker verifier test service.
5. Run Compose and Git hygiene checks.
6. Re-review the final diff and update the PR verdict.

## Item Status

| Order | Item | Status | Next step |
|------:|------|--------|-----------|
| 1 | Shared-reader checksum verification | review-complete (`PASS`, attempt 02) | Completed in `remediation-done/001-shared-reader-checksum-verification.md`; await user direction before item 2. |
| 2 | Failure classification | review-complete (`PASS`, attempt 03) | Completed in `remediation-done/002-explicit-domain-failure-classification.md`; await user direction before item 3. |
| 3 | Effect-free compensation omission | review-complete (`PASS`, attempt 02) | Completed in `remediation-done/003-conservative-effect-free-proof.md`; await user direction before item 4. |
| 4 | On-demand mutation safety | review-complete (`PASS`, attempt 02) | Completed in `remediation-done/004-cross-process-on-demand-mutation-safety.md`; await user direction before item 5. |
| 5 | Repeated step occurrences | review-complete (`PASS`, attempt 01) | Completed in `remediation-done/005-reject-repeated-runtime-step-occurrences.md`; proceed to final merge validation. |

## Item 1 — Shared-reader Checksum Verification

Current finding: the package manifest records a SHA-256 checksum for each linked artifact, but `ScenarioCatalogPackageReader` does not compare those checksums with the files it reads. The executor uses this reader, so it can accept package files that no longer match the manifest. The on-demand mutation service performs a separate checksum check, which means the two entry points enforce different integrity rules.

Proposed intent: make `ScenarioCatalogPackageReader` the single integrity boundary. Before returning any package contents, it should compute each linked artifact's SHA-256 value and reject the package when that value differs from the manifest. Existing schema, path, count, and semantic validation should remain unchanged.

Proposed focused coverage:

- a valid package still loads;
- changing each linked artifact without updating its manifest checksum is rejected;
- the executor and on-demand service inherit the same behavior through the shared reader;
- rejection happens before a tampered record can be selected or executed.
