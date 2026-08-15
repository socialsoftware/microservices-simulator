# Deterministic event-consequence replay review

Authority:
- Intent: [`work.md`](./work.md)

Reviewer: `sp-reviewer` session `019fba96-b770-7a13-9b8f-55adfb27557b`
Reviewed: uncommitted final implementation diff, fresh package `verifiers/target/quizzes-20260801-014405-816/`, and renewed 3+3+1 Quizzes evidence
Verdict: `PASS`

## Review Boundary

Reviewed the approved v4 package/extraction contract, simulator replay gate, executor/report/baseline behavior, Quizzes positive/control/masking artifacts, package hashes, affected tests, canonical docs, and adjacent Saga/local regressions. Broader event shapes and the seven unchanged Quizzes async assertion failures are excluded from this outcome.

## Findings

### EC-REV-001 — `NEEDS_FIX`

- Contract: only trigger failure before emission may causally mask E.
- Evidence: `ScenarioExecutor` marks any trigger domain failure as failed before considering captured events; the fixture test emits and then fails but expects masking.
- Impact: an emitted event can remain undispatched while ImpactV1 is evaluated, allowing a false zero.
- Required correction: hard-stop and do not evaluate impact when the trigger fails after capturing an event; retain masking only before emission.
- Disposition: `fixed`

### EC-REV-002 — `NEEDS_FIX`

- Contract: event/prerequisite workload generation remains inside `maxCatalogScenarios`, with honest generated/selected/capped/exported accounting.
- Evidence: total export is now correctly capped at 100, but the repaired package reports `workloadsCapped=0` and no cap warning although event expansion produced 112 base candidates and only 98 were retained; `ScenarioGenerator` silently breaks at the outer-loop cap boundary.
- Impact: package accounting still claims no omitted workloads despite cap-driven omission.
- Required correction: record the outer-loop cap encounter and omitted generation stage honestly, add focused coverage, then regenerate the package and attempts because accounting/manifest bytes change.
- Disposition: `fixed`

### EC-REV-003 — `NEEDS_FIX`

- Contract: unsupported producer/consumer source shapes are rejected rather than guessed.
- Evidence: producer extraction does not resolve the `registerEvent` receiver or unit-of-work argument, mixed compensation-origin emission can still yield a forward candidate, and consumer bridge extraction does not require one unconditional direct delegation.
- Impact: unsupported routes can be persisted as exact consequences and fail after side effects.
- Required correction: enforce those conservative direct-call boundaries and add dummyapp negative tests.
- Disposition: `fixed`

### EC-REV-004 — `NEEDS_FIX`

- Contract: checksum-valid v4 packages reject incomplete event routes and all reader diagnostics identify v4.
- Evidence: `WorkloadPlanValidator` omits required emission-site fields/event type/ordinal checks; two reader paths still say v3 is required.
- Impact: malformed routes pass package selection and diagnostics contradict the clean v4 boundary.
- Required correction: validate every required emission/route field and ordinal, test malformed packages, and update stale messages.
- Disposition: `fixed`

### EC-REV-005 — `NEEDS_FIX`

- Contract: report v5 preserves occurrence-level action identity.
- Evidence: synthetic `NOT_REACHED` event outcomes use the trigger scheduled-step ID as `runtimeOccurrenceId` instead of the event consequence occurrence.
- Impact: one persisted event action changes occurrence identity depending on execution state.
- Required correction: use the event action/consequence occurrence ID and test a hard-stop-before-E report.
- Disposition: `fixed`

### EC-REV-006 — `BLOCKING`

- Contract: Docker generation and attempts must prove the complete final candidate.
- Evidence: the saved package and all attempts predate a later modification to `PrerequisiteScenarioGenerator.java`.
- Impact: artifacts prove an earlier candidate and cannot close the current implementation.
- Required correction: after fixes, generate a fresh final-source Docker package and repeat 3 positive, 3 control, and 1 masking attempts with five-file before/after hashes.
- Disposition: `fixed`

### EC-REV-007 — `NOTE`

- Contract: unrelated regression findings remain outside this outcome.
- Evidence: seven full Quizzes failures are in unchanged async tests/paths and expect a direct `SimulatorException` where the async path returns `CompletionException`.
- Impact: none on the approved event-consequence boundary.
- Disposition: `accepted limitation`

## Verification Cycles

### Cycle 1

- Fix state reviewed: repaired uncommitted diff plus fresh package `verifiers/target/quizzes-20260801-004834-047/` and 3+3+1 attempts.
- EC-REV-001, EC-REV-003–006: `resolved` — focused regressions and fresh final-source artifacts verify the corrections.
- EC-REV-002: `unresolved` — total cap is enforced, but a silent outer-loop cap path leaves capped counts/warnings false.
- New same-class defect: none.
- Verdict: `NEEDS_FIX`

### Cycle 2

- Fix state reviewed: final uncommitted diff plus fresh package `verifiers/target/quizzes-20260801-014405-816/` and renewed 3+3+1 attempts.
- EC-REV-002: `resolved` — total export is 100; manifest now records one cap encounter, 14 omitted base workloads, and the exact deterministic stage warning. Focused 47-test coverage and reviewer rerun pass.
- Fresh proof: positive 3/3 ImpactV1 `1`, control 3/3 `0`, masking exact with `MASKED_BY_TRIGGER_FAULT`; every setup succeeds and every five-file hash snapshot matches.
- New same-class defect: none.
- Verdict: `PASS`

## Residual Notes

The reviewer independently reproduced 5 simulator replay tests, 126 focused verifier tests, 10 focused Quizzes tests, and the verifier docs build. The saved positive/control/masking artifacts are internally consistent; fresh proof is required only because they predate the final generation source.
