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
