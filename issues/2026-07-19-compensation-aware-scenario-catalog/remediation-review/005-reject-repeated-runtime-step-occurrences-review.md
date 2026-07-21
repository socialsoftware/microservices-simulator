# Remediation Review: 005 - Reject Repeated Runtime Step Occurrences

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes` (caller transition after PASS)

Safe for caller to move the card to done: `yes`

## Sources Reviewed

- Remediation and completion evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/005-reject-repeated-runtime-step-occurrences.md`
- Updated parent contract, especially materializability/replay wording and AC-20, AC-21, AC-25: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Parent implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Independent follow-up/status: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Canonical context: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- Live documentation: `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- Production implementation and inherited boundaries:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/WorkloadPlanValidator.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGenerator.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriter.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`
- Focused changed tests:
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioModelSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGeneratorSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- Complete tracked working-tree diff from `b2161467`: 11 files, 241 insertions, 21 deletions; binary-form diff is 500 lines / 44,531 bytes, SHA-256 `ae999df911ad1b8174e66be1f03e55383509d09a9181a2e08a009d89ce9ffb81`.
- Untracked artifacts reviewed/accounted for: the 216-line remediation card and the two pre-existing meeting-note files. Prior remediation cards/reports were inspected for preservation and have no tracked diff from `b2161467`.
- Recorded full-suite evidence: the remediation completion table and all 36 current Surefire XML reports under `verifiers/target/surefire-reports`.

## Summary

The production change is the smallest correct boundary fix: only `WorkloadPlanValidator` changed. It indexes validated forward steps by a two-field record containing exactly `sagaInstanceId` and `runtimeStepName`, in forward-list order, and `putIfAbsent` retains the first occurrence for every later diagnostic. It does not use a global runtime-name set, `stepId`, occurrence id, or schedule position as the duplicate key.

The validator indexes a step only after both owner existence and runtime-name mapping pass. Fresh adversarial probing confirmed that unknown owners, null mappings, and invalid mappings retain their existing diagnostics without duplicate evidence. The same probe confirmed that different `stepId` values resolving to one runtime name still collide within one participant, a repeated occurrence id with distinct runtime names does not create this diagnostic, the same name across participants remains valid, distinct names within one participant remain valid, and third/later repeats continue to reference the first occurrence.

Materializability and eager generation inherit the structural result; recovery generation validates before preparing actions; writer and shared reader validate WorkloadPlans; on-demand and executor load through that shared reader. Focused tests cover every boundary and the required five-spec command passed all 236 tests fresh. No model, identity, schema, simulator, or occurrence-aware runtime implementation changed.

The updated spec and live docs state the current restriction, cross-participant allowance, preserved occurrence identity, and future occurrence-aware-runtime boundary without claiming runtime support that does not exist.

No blocking, major, or non-blocking finding remains.

## Contract Compliance

| Requirement | Verdict | Evidence |
|---|---|---|
| Exact duplicate key | pass | `ParticipantRuntimeStepKey` has exactly `sagaInstanceId` and `runtimeStepName`; the key is constructed only from those fields. The adversarial probe used different `stepId` prefixes with the same resolved runtime name and received duplicate diagnostics. |
| Participant-local, not global | pass | `ScenarioModelSpec` proves same-name/different-participant validation and materializability; the fresh probe also produced no diagnostic for `p1/shared` plus `p2/shared`. |
| Distinct names remain valid | pass | `ScenarioModelSpec` and `RecoveryScheduleGeneratorSpec` preserve two same-participant occurrences with distinct runtime names, ids, forward actions, and reverse compensation linkage. |
| First occurrence is deterministic | pass | Schedule iteration plus `LinkedHashMap.putIfAbsent` keeps the first step. Two validations compare equal in `ScenarioModelSpec`; the three-repeat probe named `occ-0` as first for both `occ-1` and `occ-2`. |
| Existing malformed mapping diagnostics remain authoritative | pass | Index insertion requires `participantsById.containsKey(owner) && validRuntimeMapping`. Probe results contained only `UNKNOWN_FORWARD_OWNER` for unknown owners and only `INVALID_RUNTIME_STEP_MAPPING` for invalid/null mappings, with zero duplicate diagnostics. |
| Materializability/eager inheritance | pass | Exact prefixed `STRUCTURAL:DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME:...` assertion, `materializable == false`, zero FaultScenarios, and zero computed vectors. |
| Recovery inheritance | pass | `RecoveryScheduleGenerator.validateInputs` calls `WorkloadPlanValidator` before `prepare`; focused direct-generation test asserts the duplicate code and first/repeated occurrence text. |
| Publication and reader inheritance | pass | Writer validates before parent/artifact creation; shared reader validates each workload after checksum verification. Focused tests assert duplicate diagnostics and no publication artifacts. |
| On-demand cannot bypass | pass | Service reads/validates the package through `ScenarioCatalogPackageReader` before request selection or mutation; focused test returns `REJECTED / INVALID_PACKAGE`, includes the duplicate code, and preserves all three mutable artifact byte arrays. |
| Executor cannot bypass | pass | Executor's first operation is package-reader loading. Focused test asserts duplicate rejection, zero fixture constructor calls, and no body execution. |
| Occurrence identity preserved | pass | No model/id production path changed. Existing occurrence fields remain on scheduled steps, slots, checkpoints, actions, and reports; focused recovery coverage proves distinct forward/action ids and reverse occurrence linkage for valid distinct names. |
| No occurrence-aware runtime/schema change | pass | The only changed production file is `WorkloadPlanValidator.java`; no `simulator/`, Saga unit-of-work/workflow, schema declaration, model record, or identity generator changed. |
| Dependencies/history protected | pass | Remediations 001–004 remain under `remediation-done/` with their PASS histories; tracked diff under prior remediation cards/reports is empty. Protected meeting-note hashes and 2026-07-19 mtimes match the previously recorded values. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-20 | pass | Positive same-name/cross-participant and distinct-name/same-participant controls are structurally valid; ready fixtures are materializable. | Occurrence ids remain semantic identities. |
| AC-21 | pass | Structural duplicate is propagated through materializability, writer, reader, on-demand, and recovery boundaries; on-demand mutable bytes remain unchanged. | Diagnostic code is stable and structured. |
| AC-25 | pass | Shared package validation rejects before executor selection/materialization/constructor/body execution. | Runtime remains name-keyed and unchanged. |
| Remediation focused tests 1–10 | pass | Exact validator message, first-occurrence determinism, eager suppression, direct recovery rejection, no writer artifacts, checksum-current reader rejection, on-demand byte preservation, executor pre-start rejection, both positive controls, and valid occurrence/action identity are covered. | Fresh five-spec gate passed. |
| Documentation contract | pass | Spec, current state, glossary, and executor reference all state participant-local uniqueness, cross-participant allowance, preserved occurrence identity, and future occurrence-aware support. | No runtime occurrence-awareness overclaim found. |

## Verification Evidence Check

| Command / method | Verdict | Fresh/inspected result |
|---|---|---|
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` | pass | Fresh `BUILD SUCCESS`; 236 tests, 0 failures, 0 errors, 0 skipped. Per suite: executor 63, recovery 14, model 87, writer/reader 23, on-demand 49. Maven time 52.801 s. OpenTelemetry exporter connection warnings did not affect tests or build status. |
| `mvn -Dtest=ScenarioModelSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` from repository root | reviewer invocation error, not product failure | Maven stopped in 0.164 s with `MissingProjectException` because the repository has no root POM; 0 tests ran. The required module-scoped command above was then run successfully. |
| `javac -cp verifiers/target/classes -d /tmp /tmp/Remediation005ValidatorProbe.java && java -cp /tmp:verifiers/target/classes Remediation005ValidatorProbe` | pass | Fresh probe passed seven adversarial groups: three repeats/different step ids, cross-participant same name, same-participant distinct names, unknown owner, invalid mapping, null mapping, and duplicate occurrence id with distinct names. |
| Surefire XML aggregation under `verifiers/target/surefire-reports` | pass (recorded full-suite inspection) | 36 suite reports sum to 548 tests, 0 failures, 0 errors, 0 skipped; no `*Probe*` report remains. The five fresh reports have the same 236-test counts as the recorded full run. |
| Recorded `cd verifiers && mvn test` after removal of two named target-only stale probe classes/reports | pass (not rerun) | Completion evidence records `BUILD SUCCESS`, 548 tests, 0 failures/errors/skips, 3:18. Source lookup confirms neither stale probe has a repository source file and current reports contain no probe suite. |
| Recorded `cd verifiers && mvn clean test` | environment-blocked, not product failure | The documented attempt did not reach clean/test because shared `~/.m2` denied creation/download of `maven-clean-plugin:3.4.1` (`AccessDeniedException`). No test result is inferred from it. A broader rerun was unnecessary after the inspected 548-test pass plus the fresh 236-test gate. |
| `git diff --check b2161467 --` plus explicit 12-path UTF-8/final-newline/trailing-whitespace probe | pass | Exit 0; 12 changed/item paths checked, 0 hygiene problems. |
| Changed-doc local-link probe | pass | 11 local Markdown links checked across current state, glossary, and executor reference; 0 missing. |
| Scoped claim scan | pass | Required diagnostic/restriction/cross-participant/occurrence/future-runtime wording is present in spec and live docs; no occurrence-aware implementation claim was found. |
| Production/runtime/schema scope scan | pass | Exactly one changed production path: `WorkloadPlanValidator.java`; zero simulator/runtime/model/schema declaration changes. |
| Prior-history/protected-file inspection | pass | No tracked diff in `remediation-done/` or prior `remediation-review/`. Meeting-note SHA-256 values remain `8da2548d...` and `7fd0ff54...`, with preserved 2026-07-19 mtimes. |

## Test Integrity Review

The isolated model/recovery/writer fixtures recompute semantic WorkloadPlan ids after mutation where identity validation would otherwise obscure the intended case. The serialized reader/on-demand/executor tamper fixtures refresh the manifest SHA-256 but retain the original WorkloadPlan id and unchanged slot mapping, so the aggregate validator message also contains `WORKLOAD_ID_MISMATCH` and/or `MALFORMED_FAULT_SLOT`. This does not let those tests pass for an unrelated reason: each test explicitly requires `DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME`, and schedule validation emits that diagnostic before slot/id validation. Without the new duplicate rule, those assertions fail, as recorded by the red run. The isolated current-id writer and model cases additionally prove the duplicate as the intended structural defect without relying on package tampering side effects.

This is acceptable evidence rather than a blocker. A future cleanup could make every serialized tamper fixture single-cause, but no such cleanup is required for remediation correctness.

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|---|---|---|
| Minimality | pass | One validator-local map and one private two-field record; no new dependency or boundary refactor. |
| Existing patterns | pass | Uses deterministic list iteration, `LinkedHashMap`, structured validator diagnostics, and existing shared validation gates. |
| Test quality | pass | Exact diagnostic/message, positive controls, inheritance boundaries, mutation safety, and occurrence identity are asserted. Red/green evidence is credible; serialized tamper nuance is documented above. |
| Regression risk | pass | Fresh 236-test gate passed; recorded 548-test suite was inspected; no broader production path changed. |
| Security/data safety | pass | Invalid on-demand packages remain non-mutating; reader checksum protection and executor pre-start rejection remain intact. |
| Change hygiene | pass | Full tracked diff is scoped; no prior remediation/report, meeting note, runtime class, schema, or identity generator was changed. |

## Findings

None.

## Done Transition

- Moved to done: `yes`, by the caller after the reviewer returned `PASS`.
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/005-reject-repeated-runtime-step-occurrences.md`
- Reason: the verdict is `PASS`; the caller completed the done transition while preserving the attempt history.
- Caller may move to done: `yes`

## Recommendation

Accept remediation 005 as review-complete. The caller may move the active card to `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/005-reject-repeated-runtime-step-occurrences.md` while preserving this attempt history. Do not commit as part of that transition unless separately requested.
