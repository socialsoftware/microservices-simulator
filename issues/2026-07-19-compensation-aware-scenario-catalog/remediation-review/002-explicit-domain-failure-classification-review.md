# Remediation Review: 002 - Explicit Domain Failure Classification

## Review Attempt

Attempt: `03`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes` (caller transition after PASS)

## Sources Reviewed

- Completed remediation and appended attempt-02 fix evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md`
- Prior reviews:
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-review/002-explicit-domain-failure-classification-review-01.md`
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-review/002-explicit-domain-failure-classification-review-02.md`
- Corrected documentation:
  - `docs/verifiers-impl/current-state.md`
  - `docs/verifiers-impl/evidence.md`
- Consistency anchors:
  - `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`, including AC-31 and AC-33
  - `docs/verifiers-impl/reference/scenario-executor.md`
  - `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
  - `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
  - `docs/verifiers-impl/glossary.md`
- Saved artifact: `verifiers/target/compensation-aware-v3-evidence/execution-report-25c0d61a.json`
- Attempt-02 tracked-diff snapshot: `/tmp/remediation-002-review02-tracked.diff`, SHA-256 `914f758b8d51a3c2d29ca0a5e98dea7008c0cd6f99aa6f1df228bdf811418518`.
- Prior fresh test logs from attempt 02 under `/tmp/remediation-002-review02-*.log`.

## Summary

The attempt-02 documentation finding is resolved. Current-state no longer presents the saved Quizzes execution as evidence of a current meaningful domain fallback. Both current-state and the evidence appendix identify it as pre-remediation historical evidence, retain its actual recorded `PARTIAL_COMPENSATED / DEVIATED` outcome, identify the actual exception as a plain unmarked `SimulatorException` reporting service unavailability after retries, and state the current expected no-fallback/no-survivor `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` result. The current v3 interpretation no longer attributes that failure to null input.

The separately headed historical v2 section remains historical and unchanged. Parent spec/AC, executor reference, accepted ADR, glossary, and the historical-policy supersession pointer remain consistent. The 14 tracked remediation-002 code/test diff blocks are byte-identical to the attempt-02 snapshot; the five relevant untracked production/test files also predate the attempt-02 report and retain the reviewed content. Production tests were not rerun because no remediation-002 source or test changed after the fresh attempt-02 passes.

No blocking or major finding remains.

## Attempt-02 Finding Resolution

| Finding | Verdict | Evidence |
|---|---|---|
| Current documentation gave the saved service-unavailability report a contradictory current-domain interpretation | resolved | `current-state.md:145,172` and `evidence.md:94,135` now distinguish the historical recorded outcome from current behavior and explicitly state no fallback, no survivor action, and `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`. |
| Evidence appendix attributed the saved v4 report failure to a null course input | resolved | `evidence.md:135` now identifies the plain unmarked service-unavailability-after-retries exception. The artifact itself confirms exact class `SimulatorException` and message `Service 'course' unavailable after retries exhausted for CreateCourseRemoteCommand: Name is null`. The docs no longer collapse that transport failure into a direct null-input domain rejection. |
| Historical evidence had to remain honest and unchanged | resolved | `evidence.md:106-133` still records exit 0, `PARTIAL_COMPENSATED`, `DEVIATED`, three measured actions, fallback lifecycle, masked slot, and survivor commit. The prose explains that these were old-classifier results rather than rewriting them as current execution. |

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Current-state does not claim a current marked-domain smoke | pass | `current-state.md:172` says no post-remediation Quizzes smoke demonstrates explicitly marked fallback and identifies the saved smoke as unmarked infrastructure under current execution. |
| Saved execution is explicitly historical | pass | `current-state.md:145`, `evidence.md:94`, and executor reference line 169 use `pre-remediation historical evidence`. |
| Artifact remains represented honestly | pass | Direct `jq` inspection matches the documented historical statuses/actions: `PARTIAL_COMPENSATED`, `DEVIATED`, immediate fallback policy, failed `createCourseStep`, completed survivor, and plain `SimulatorException`. Artifact SHA-256 is `f015bca85872daa041f511b9e8cfc4edcc7739dd673e921e1076b4abd010637d`. |
| Current expected behavior is explicit | pass | `current-state.md:145` and `evidence.md:135` state no fallback/no survivor and `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`. |
| Current v3 text does not misattribute failure to null input | pass | Exact current-v3 stale-phrase scan passes. Service unavailability is identified as the failure; the artifact's nested diagnostic text remains visible only through direct artifact inspection. |
| Explicitly historical v2 material remains historical | pass | `evidence.md:530` retains `## Historical v2 ScenarioExecutor multi-saga Quizzes smoke`; the attempt-03 edit does not change that section. |
| Parent/live contract remains consistent | pass | Spec/AC-31 and AC-33, current-state, executor reference, accepted ADR, and glossary all require explicit `DomainFailure`; plain/unmarked failures hard-stop. |
| Historical ADR remains appropriately historical | pass | The 2026-07-08 ADR status points to the superseding v3 decision and explicitly says plain `SimulatorException` is insufficient. Its old `domain/simulator` wording remains within the historical decision body, not current guidance. |
| Code/test resolution is unchanged | pass | Exact comparison of 14 tracked remediation-002 source/test diff blocks against the attempt-02 snapshot found 0 changes. Five relevant untracked source/test files have mtimes before the attempt-02 report and retain reviewed hashes/content. |
| Scope/non-goals | pass | Documentation-only correction; no schema, status vocabulary, runtime behavior, transport, distributed-parity claim, or item-3 work. |
| Dependency | pass | Remediation 001 remains reviewed PASS and was not modified by this review. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-31 / explicit-marker fallback | pass | Spec remains unchanged from attempt 02 and limits fallback to marked body/commit failures. | Current docs match. |
| AC-33 / infrastructure hard-stop | pass | Current-state/evidence now identify the saved unmarked service failure as infrastructure with the required hard stop. | No current-domain claim remains. |
| AC-38 / conformance interpretation | pass | Historical artifact remains `DEVIATED`; current replay of the same unmarked failure is documented as `INCOMPLETE`. | Historical versus current semantics are separated. |
| AC-46 / current/live documentation | pass | Docs build, links, structural checks, and semantic ambiguity assertions pass. | The prior major finding is closed. |

## Verification Evidence Check

| Command / method | Verdict | Notes |
|---|---|---|
| `./scripts/verifier-docs build` | pass | MkDocs Material built `target/verifier-docs-site` in 13.00 seconds. Only informational notices listed pages outside nav; there were no build errors. |
| Relative-link checker over spec, remediation card, current-state, evidence, reference, glossary, and both ADRs | pass | 18 relative links checked; every target exists. |
| Markdown fence/heading structural checker | pass | 8 files checked; fences balanced and heading levels valid. |
| Exact stale-claim/current-v3 assertions | pass | Old current-smoke/domain-fallback/null-input phrases absent from current-state and the v3 evidence section; required historical/service-unavailability/no-fallback/no-survivor/status phrases present. |
| Ambiguity `rg` scan | pass | Only broad `domain/simulator` match is inside the explicitly historical 2026-07-08 ADR, whose status and supersession pointer state the current marker rule. |
| Direct saved-report `jq` inspection | pass | Confirms historical status, conformance, policy, actions, participant outcomes, exact exception class, and service-unavailability message. |
| Saved artifact SHA-256 | pass | `f015bca85872daa041f511b9e8cfc4edcc7739dd673e921e1076b4abd010637d`. The correction changed prose, not the artifact. |
| Attempt-02 diff-block comparison | pass | 14 tracked remediation-002 code/test blocks compared; 0 changed. Spec, reference, glossary, accepted ADR, and historical ADR blocks are also unchanged. Only `current-state.md`, newly changed `evidence.md`, and appended remediation evidence implement this fix. |
| `git diff --check` | pass | No tracked whitespace errors. The appended attempt-02 evidence has no trailing whitespace; the card's only trailing spaces are four pre-existing Markdown hard breaks on lines 3-6. |
| Conflict/status/protected-file hygiene | pass | No conflict markers in attempt-03 files. Meeting notes, prior numbered reports, remediation-001 reports, source code, tests, and root-owned Quizzes output were not touched by this review. |
| Production tests | not rerun | Acceptable for this documentation-only delta. Attempt-02 fresh results remain applicable: simulator targeted 23/23, simulator install PASS, executor 62/62, and safe Quizzes 2/2, all with zero failures/errors/skips. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Correction is limited to 7 additions/4 deletions in current-state, 3 additions/1 deletion in evidence, and appended completion evidence. |
| Existing patterns | pass | Uses the repository's current-truth versus historical-evidence convention. |
| Test quality | pass | Production test evidence remains fresh and applicable because source/test content is unchanged. |
| Regression risk | pass | Documentation build and semantic/structural checks pass; no runtime code changed. |
| Security/data safety | pass | Historical artifact was read only; no ownership, package, or external data mutation. |
| Change hygiene | pass | Prior reviews and protected notes remain untouched; item 3 was not started. |

## Findings

None.

## Done Transition

- Moved to done: `yes`, by the caller after the reviewer returned `PASS`.
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md`
- Reviewer constraint respected: the reviewer did not move the slice during attempt 03.

## Recommendation

Accept remediation 002. The explicit domain/infrastructure classification contract, transport behavior, executor regressions, parent/live documentation, and historical evidence interpretation are now consistent. Move the remediation card to the appropriate done location only through the caller-authorized workflow; do not start item 3 as part of this review.
