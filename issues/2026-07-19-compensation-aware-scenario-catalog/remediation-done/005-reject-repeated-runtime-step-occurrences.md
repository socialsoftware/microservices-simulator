# Remediation 005 - Reject Repeated Runtime Step Occurrences

Parent spec: `../spec.md`
Independent review follow-up: `../independent-review-follow-up.md`
Depends on:
- `../remediation-done/001-shared-reader-checksum-verification.md`
- `../remediation-done/002-explicit-domain-failure-classification.md`
- `../remediation-done/003-conservative-effect-free-proof.md`
- `../remediation-done/004-cross-process-on-demand-mutation-safety.md`
Risk: `medium`
Status: `review-complete`

## Finding

The v3 model gives every scheduled forward step an occurrence identity, and recovery-generation tests currently accept two occurrences of the same runtime step for one participant. The supported Saga/local runtime is not occurrence-aware: unit-of-work compensation, previous-state rollback, aborted-step state, and workflow execution are keyed by runtime step name.

For one participant, two scheduled occurrences with the same `runtimeStepName` therefore collide at runtime even when their catalog occurrence ids differ. A package can currently pass structural validation and be advertised as executor-materializable although exact replay cannot distinguish the occurrences.

## Plain-Language Rule

Until the Saga/local runtime is occurrence-aware, one WorkloadPlan participant may schedule a given runtime step name at most once.

The unsafe key is:

```text
(sagaInstanceId, runtimeStepName)
```

The same runtime step name remains valid in different participants because each participant owns a distinct workflow and unit of work. Distinct runtime step names within one participant also remain valid.

Occurrence ids remain part of the v3 semantic identity and action/source linkage. This remediation narrows executor admissibility; it does not remove occurrence identity from the model.

## Concrete Bad Example

Unsafe:

```text
participant-a / reserveStep / occurrence-1
participant-a / reserveStep / occurrence-2
```

The catalog distinguishes `occurrence-1` and `occurrence-2`, but runtime maps such as compensation and previous state use only `reserveStep`. The second execution can overwrite or reuse the first runtime state, and recovery checkpoint discovery can collapse both names.

Still valid:

```text
participant-a / reserveStep / occurrence-1
participant-b / reserveStep / occurrence-2
```

The participants have separate runtime state, so the shared step name is not itself ambiguous.

## Required Behavior

- `WorkloadPlanValidator` must deterministically reject a repeated `(sagaInstanceId, runtimeStepName)` in the forward schedule.
- Use one stable structured diagnostic code, recommended:

```text
DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME
```

- The diagnostic must identify the participant, runtime step name, first occurrence id, and repeated occurrence id understandably.
- Detection must use participant identity plus the validated runtime step name, not `stepId` alone, occurrence id, schedule position, or a global step-name set.
- A runtime step name repeated across different participants must remain valid.
- Different runtime step names for one participant must remain valid.
- Existing malformed owner/runtime-name mapping diagnostics must remain intact and deterministic; avoid misleading duplicate diagnostics for missing/null keys.
- `EagerFaultScenarioGenerator.evaluateMaterializability` must inherit the structural diagnostic and mark the workload non-materializable.
- Recovery generation must reject the repeated-name workload through its existing WorkloadPlan validation boundary rather than creating apparently replayable actions.
- Package publication and package reading must reject a structurally invalid repeated-name WorkloadPlan through their existing shared validator boundary.
- On-demand generation and execution must not bypass this check.
- Do not alter deterministic ids or remove occurrence fields from `ScheduledStep`, slots, checkpoints, actions, or reports.
- Do not change the runtime to key state by occurrence in this remediation.

## Narrow Implementation Boundary

Expected production scope:

- `WorkloadPlanValidator` only, unless a current boundary bypass is proven;
- focused model, recovery-generator, package/materializability, and executor-boundary tests;
- concise current documentation/spec clarification of the unique per-participant runtime-step-name restriction.

Out of scope:

- changing `SagaUnitOfWork`, `SagaUnitOfWorkService`, `ExecutionPlan`, `SagaWorkflow`, or executor runtime state to occurrence keys;
- schema or deterministic identity changes;
- removing occurrence-aware model fields;
- globally forbidding the same runtime step name across participants;
- changing schedule enumeration for already valid workloads;
- item 1–4 behavior.

## Focused Tests

At minimum prove:

1. a workload with two distinct occurrence ids but the same participant/runtime step name is rejected with the exact structured diagnostic;
2. the diagnostic deterministically names the participant, runtime name, first occurrence, and duplicate occurrence;
3. `evaluateMaterializability` returns false and includes the prefixed structural diagnostic;
4. eager generation writes no FaultScenarios or computed vectors for that workload;
5. direct recovery generation rejects before action generation;
6. package publication rejects the invalid workload rather than writing an executable-looking package;
7. a checksum-current package containing such a workload is rejected by the shared reader before selection/execution;
8. an on-demand request cannot materialize or mutate from such a workload package;
9. the same runtime name in two different participants remains structurally valid and materializable when inputs are ready;
10. distinct runtime names in one participant remain valid;
11. valid existing workload identity, recovery ordering, materializability, accounting, and executor tests remain unchanged.

Replace the existing positive repeated-step recovery-generation expectation with the current safe boundary. Keep separate occurrence-identity tests for distinct runtime step names so occurrence semantics remain covered rather than being removed accidentally.

Use TDD where practical: establish a failing validator/materializability regression before changing production code.

## Documentation Contract

Current docs and the parent spec must say:

- occurrence ids remain semantic catalog/action identities;
- current Saga/local executor materializability requires runtime step names to be unique within each participant;
- the same name in separate participants is allowed;
- repeated same-participant runtime names remain a future runtime occurrence-awareness task rather than supported exact replay.

Do not claim the runtime was made occurrence-aware.

## Verification

Run from the verifier module:

```bash
cd verifiers
mvn -Dtest=ScenarioModelSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test
```

Add a narrower model/materializability red/green run if useful. Run documentation and scoped hygiene checks across every changed path.

## Evidence to Record

Append completion evidence to this file with:

- implementation summary and exact duplicate key;
- exact diagnostic code/message;
- files changed;
- red/green test commands and exact counts;
- positive same-name/different-participant control;
- package, on-demand, and executor boundary evidence;
- documentation changes;
- deviations or remaining concerns.

Do not commit, move this remediation card, edit prior remediation/review reports, modify items 1–4, implement occurrence-aware runtime state, or touch the two pre-existing meeting-note files. Leave review and the done transition to the orchestrator.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added one validator-only production rule in `WorkloadPlanValidator`: after owner existence and runtime-name mapping are validated, forward occurrences are indexed by the exact key `(sagaInstanceId, runtimeStepName)` in schedule order. A repeat is rejected against the first occurrence.
- Stable diagnostic code: `DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME`.
- Exact tested message: `participant participant-1 repeats runtime step name step: first occurrence scheduled-0, repeated occurrence scheduled-1`.
- Null/missing/invalid owner or runtime-name mappings do not enter the duplicate index, so existing `UNKNOWN_FORWARD_OWNER` and `INVALID_RUNTIME_STEP_MAPPING` diagnostics remain authoritative instead of producing a misleading duplicate.
- No production bypass was found: eager materializability, recovery generation, package publication, shared package reading, on-demand mutation, and executor package loading already cross `WorkloadPlanValidator`. No production class beyond the validator changed.
- Occurrence fields, deterministic-id generation, action/source linkage, runtime state, and valid-workload schedule enumeration are unchanged.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/WorkloadPlanValidator.java` — participant-local runtime-name uniqueness rule and structured diagnostic.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioModelSpec.groovy` — exact deterministic diagnostic, materializability/eager suppression, same-name/different-participant control, and distinct-name occurrence controls.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGeneratorSpec.groovy` — direct recovery rejection and safe distinct-runtime-name occurrence/action identity coverage replacing the unsafe positive repeated-name expectation.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy` — pre-publication rejection with no artifacts and checksum-current shared-reader rejection.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy` — invalid-package rejection and byte-unchanged mutable artifacts.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — checksum-current package rejection before constructor/body execution.
- `docs/verifiers-impl/current-state.md` — current admissibility rule, occurrence identity preservation, cross-participant allowance, and runtime limitation.
- `docs/verifiers-impl/glossary.md` — scheduled-step and materializability definitions.
- `docs/verifiers-impl/reference/scenario-executor.md` — package/executor boundary and explicit unsupported repeated-name runtime shape.
- `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md` — orchestration-level clarification of per-participant runtime-name uniqueness, cross-participant allowance, preserved occurrence identity, validation gates, and the future occurrence-aware runtime boundary.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/005-reject-repeated-runtime-step-occurrences.md` — this completion evidence, orchestration/full-suite evidence, and review outcome.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` (red, before production rule) | FAIL as expected | 236 tests run; 6 failures, 0 errors. The new validator/materializability, writer, reader, on-demand, recovery, and executor expectations exposed the missing diagnostic/rule. One recovery assertion also exposed a test-local Groovy name-shadowing mistake; it was corrected before the production edit was assessed green. |
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` (green) | PASS | 236 tests run; 0 failures, 0 errors, 0 skipped. Per-spec counts: `ScenarioExecutorSpec` 63, `RecoveryScheduleGeneratorSpec` 14, `ScenarioModelSpec` 87, `ScenarioCatalogJsonlWriterSpec` 23, `OnDemandFaultScenarioServiceSpec` 49. |
| `cd verifiers && mvn test` (initial non-clean full run) | FAIL from stale review build output | 550 tests ran with 0 failures and 2 errors from `EffectFreeAttempt02ProbeSpec` and `EffectFreeSafetyProbeSpec`. Filesystem inspection proved these classes existed only under `verifiers/target/test-classes` and had no source file; their stale surefire reports were also target-only. |
| `cd verifiers && mvn clean test` | BLOCKED before clean/test execution | Maven could not download `maven-clean-plugin:3.4.1` into the permission-restricted shared `~/.m2` and stopped with `AccessDeniedException`; no test result was claimed. |
| Remove only `EffectFreeAttempt02ProbeSpec*` / `EffectFreeSafetyProbeSpec*` target classes and reports, then `cd verifiers && mvn test` | PASS | Full verifier suite: 548 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` in 3:18. The two removed classes/reports were temporary prior-review build residue, not repository sources or semantic artifacts. |
| `git diff --check -- <all item-005 changed paths>` | PASS | No whitespace errors across production, tests, docs, and this card. |
| Scoped `rg` inspection for `DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME`, uniqueness wording, and occurrence-aware limitations | PASS | Diagnostic appears only in the validator and focused boundary assertions/docs; no runtime occurrence-key implementation was introduced. |

### Acceptance / Boundary Evidence

- **Exact duplicate:** one participant with two distinct occurrence ids and the same validated runtime name produces exactly `DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME`, naming participant, runtime name, first occurrence, and repeated occurrence deterministically.
- **Positive participant-local control:** `participant-1/shared` plus `participant-2/shared` is structurally valid and materializable with ready inputs.
- **Positive same-participant control:** `participant-1/first` plus `participant-1/second` remains valid; scheduled occurrence ids and slot occurrence ids remain `scheduled-0`, `scheduled-1`.
- **Occurrence identity:** recovery coverage now uses distinct runtime names and still proves distinct forward action ids/occurrence ids plus reverse compensation occurrence linkage.
- **Materializability/eager:** the structural diagnostic is prefixed exactly as `STRUCTURAL:DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME:...`; materializability is false, with zero FaultScenarios and zero computed vectors.
- **Recovery:** direct `RecoveryScheduleGenerator.generate` throws at its existing WorkloadPlan validation boundary before action generation.
- **Publication:** `ScenarioCatalogJsonlWriter` rejects before creating any package artifact.
- **Reader:** a checksum-current package whose WorkloadPlan JSON contains the repeat is rejected by `ScenarioCatalogPackageReader`.
- **On demand:** request status is `REJECTED` / `INVALID_PACKAGE`, includes the duplicate diagnostic, and leaves FaultScenario, accounting, and manifest bytes unchanged.
- **Executor:** the shared package reader rejects before selection/materialization; `FixtureWorkflow.constructorCalls == 0` and no bodies run.

### TDD Notes

- Focused boundary regressions were added before production. The first verifier command produced the expected missing-rule failures. A test-local method/variable shadowing error in the recovery case was fixed, then the single validator production change made the complete required gate green.

### Orchestration-Level Spec and Full-Suite Evidence

- The implementation worker correctly left `spec.md` unchanged under its worker contract. Before independent review, the orchestrator updated the parent materializability, replay-validation, AC-20, AC-21, AC-25, and assumption wording with the approved per-participant uniqueness rule, cross-participant allowance, preserved occurrence identity, and future occurrence-aware-runtime limitation.
- The orchestrator confirmed the initial full-suite errors came only from two prior-review `.class` files and reports under `verifiers/target`, with no matching source. `mvn clean test` could not start because the shared Maven repository denied creation of the missing clean-plugin directory. After deleting only those named target residues, a full `mvn test` passed all 548 repository tests.

### Deviations From Plan

- No production or test-boundary deviation.

### Blockers / Follow-Ups

- No blocker for review. The Saga/local runtime remains keyed by runtime step name; supporting repeated same-participant runtime names still requires a separate future occurrence-aware runtime-state change.
- No item-005 commit was created; HEAD remained `b2161467`. Prior remediation/review reports and the two pre-existing meeting-note files remain untouched.

## Review Outcome

- Attempt 01: `PASS` — no blocking, major, minor, or note finding. See `../remediation-review/005-reject-repeated-runtime-step-occurrences-review-01.md`.
- Fresh reviewer verification: 236 focused tests passed; a seven-group adversarial validator probe covered participant-local key semantics, deterministic first-occurrence evidence, positive controls, malformed mappings, and occurrence identity; documentation links and diff hygiene passed.
- The reviewer inspected the recorded full 548-test verifier pass and confirmed no stale probe suite remained.
- Final state: remediation item 5 is review-complete; no item-005 commit has been made.
