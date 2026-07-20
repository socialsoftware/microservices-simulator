# 001 - Compensation Evidence Preservation

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `None`  
ACs covered: `AC-7, AC-8, AC-44`  
Risk: `medium`

## Purpose

Prove that the existing static-analysis pipeline retains enough phase, registration, write/rollback, and unresolved-analysis truth to classify compensation checkpoints without fabricating certainty.

## Scope

- Preserve forward versus compensation dispatch evidence through `WorkflowFunctionalityVisitor` -> `ApplicationAnalysisState` building blocks -> `ApplicationAnalysisScenarioModelAdapter`.
- Retain explicit `registerCompensation` presence even when its body has no recognized command dispatch.
- Retain resolution diagnostics needed to decide whether a step is confidently effect-free.
- Produce deterministic checkpoint candidates with primary evidence precedence `EXPLICIT_COMPENSATION` > `IMPLICIT_SAGA_ROLLBACK` > `CONSERVATIVE_UNKNOWN`.
- Omit a step only when all required read-only/resolved/no-registration/no-rollback conditions are proven.
- Add dummyapp fixture shapes for explicit-with-dispatch, explicit-without-recognized-dispatch, implicit write/rollback, conservative unresolved, and confidently effect-free read-only cases.

## Out of Scope

- WorkloadPlan/FaultScenario package records or identities.
- Recovery schedule enumeration, caps, persistence, or execution.
- Broad static-analysis refactoring beyond facts required by the classification rule.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java` — current phase extraction and `registerCompensation` detection.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock/SagaStepBuildingBlock.java` — per-step analysis facts crossing the visitor boundary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock/StepDispatchFootprint.java` — current dispatch phase evidence.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java` — currently merges all dispatches into undifferentiated footprints.
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/` — canonical static-analysis fixtures.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy` and `.../scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy` — test style.

## Implementation Shape

- Extend existing step-analysis facts rather than parsing source again in the scenario layer.
- Model phase-specific footprints and analysis completeness explicitly enough that absence of a dispatch is not mistaken for proof of no effect.
- Keep supporting compensation-body footprints separate from the primary evidence class.
- Keep warnings/diagnostics deterministic and auditable but outside future semantic identity.
- Preserve current conflict analysis over forward footprints; compensation footprints must not become normal forward conflicts.

## TDD / Test Shape

- First behavior to test: an explicit `registerCompensation` with an unrecognized/empty body remains a retained checkpoint classified `EXPLICIT_COMPENSATION`.
- Expected red failure: the current visitor/adapter retains no registration fact when no recognized compensation dispatch exists and merges recognized phases.
- Additional coverage: explicit precedence over write evidence; write/Saga rollback as `IMPLICIT_SAGA_ROLLBACK`; unresolved analysis as `CONSERVATIVE_UNKNOWN`; fully resolved read-only step omitted; compensation footprint does not alter forward conflict evidence; deterministic diagnostics.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current visitor handling of lambdas, method references, unresolved commands, and `registerCompensation` calls.
- Current building-block/state fields used by HTML reporting so new facts do not break that path.
- Existing dummyapp compensation/read-only fixtures and targeted Spock command names.

## Verification

- `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` — all evidence tiers, phase separation, omission proof, and real-parser fixture pass.

## Evidence to Record

- files changed
- commands run and outputs
- fixture cases and resulting evidence classes
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Logger-only warnings are insufficient input to the omission proof; retain structured completeness facts.
- Do not classify every write as explicit compensation or every missing footprint as read-only.
- Do not let compensation dispatches contaminate forward conflict/schedule semantics.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Preserved explicit compensation registration, phase-specific dispatch footprints, per-phase analysis completeness, and structured deterministic diagnostics across the visitor/building-block/adapter boundary.
- Added checkpoint evidence classification with `EXPLICIT_COMPENSATION` > `IMPLICIT_SAGA_ROLLBACK` > `CONSERVATIVE_UNKNOWN`; only resolved non-empty all-read forward evidence is omitted.
- Extended the dummyapp compensation fixture with explicit-without-dispatch, implicit write, unresolved method-reference, and resolved read-only shapes while retaining the existing explicit-with-dispatch shape.

### Files Changed

- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java` — added the five-case static-analysis fixture matrix.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock/SagaStepBuildingBlock.java` — retained registration, analysis-completeness, and diagnostic facts.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock/StepAnalysisDiagnostic.java` — added structured phase/code/message diagnostics.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java` — retained registration independently of body dispatches and recorded unsupported/unresolved analysis.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/CompensationEvidenceClass.java` — added the three primary evidence classes.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/StepDefinition.java` — carried separate forward/compensation footprints and checkpoint-analysis facts.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java` — separated phases and classified/omitted candidates conservatively.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioGenerator.java` — preserved the added step facts through existing normalization.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy` — covered registration without dispatch and unresolved method references.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy` — covered phase separation, evidence precedence/tiers, omission, and deterministic diagnostics.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy` — asserted the real-parser fixture matrix.
- `issues/2026-07-19-compensation-aware-scenario-catalog/001-compensation-evidence-preservation.md` — recorded this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh run: `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `git diff --check` | PASS | No whitespace errors reported. |

### Acceptance Criteria Evidence

- AC-7: Adapter tests prove forward `Order` read footprints remain in `footprints()` while compensation `Item` write footprints remain separately in `compensationFootprints()`; existing conflict consumers continue to read forward footprints only.
- AC-8: Dummyapp parser coverage proves explicit-with-dispatch and explicit-without-dispatch classify `EXPLICIT_COMPENSATION`, unregistered writes classify `IMPLICIT_SAGA_ROLLBACK`, unresolved method references classify `CONSERVATIVE_UNKNOWN`, and fully resolved read-only steps are omitted (`null` evidence).
- AC-44: The required visitor, adapter, and dummyapp foundation specs pass against the real dummyapp parser fixture.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the focused visitor/adapter expectations first and observed the expected red compile failure because `CompensationEvidenceClass` and the new retained facts did not yet exist. Implemented the facts/classification, then ran the required suite to green.

### Deviations From Plan

- None.

### Blockers / Follow-Ups

- None. Ready for `sp-review-slice`.
