# Remediation 003 - Conservative Effect-Free Proof

Parent spec: `../spec.md`
Independent review follow-up: `../independent-review-follow-up.md`
Depends on:
- `../remediation-done/001-shared-reader-checksum-verification.md`
- `../remediation-done/002-explicit-domain-failure-classification.md`
Risk: `medium`
Status: `review-complete`

## Finding

`ApplicationAnalysisScenarioModelAdapter` omits a compensation checkpoint when forward analysis is marked complete, at least one forward footprint exists, and every visible footprint is read-only. `WorkflowFunctionalityVisitor` currently marks a lambda complete after scanning its locally visible `new ...Command(...)` expressions, even when the lambda also invokes an ordinary helper method whose body is outside that local scan.

A step can therefore expose one recognized read command, invoke a helper that performs a write, remain marked complete, and be incorrectly omitted as effect-free.

## Plain-Language Rule

Absence of a visible write is not proof that a step has no write effect.

A compensation checkpoint may be omitted only when the visitor understands every relevant operation in the forward lambda under its current local analysis boundary. If an additional method call is not recognized command construction or dispatch plumbing, forward analysis is incomplete and the step remains `CONSERVATIVE_UNKNOWN`.

The verifier does not need to prove that the unknown helper writes. It only needs to admit that it did not prove the helper effect-free.

## Concrete Bad Example

```java
SagaStep mixedStep = new SagaStep("mixedStep", () -> {
    GetItemCommand command =
            new GetItemCommand(unitOfWork, "Item", itemDto.getAggregateId());
    commandGateway.send(command);
    updateItemThroughHelper();
});

private void updateItemThroughHelper() {
    UpdateItemCommand command =
            new UpdateItemCommand(unitOfWork, "Item", itemDto);
    commandGateway.send(command);
}
```

The direct lambda exposes one `READ` footprint. The helper body is outside the local lambda scan. The required result is:

```text
forward analysis complete: false
compensation evidence: CONSERVATIVE_UNKNOWN
compensation checkpoint: retained
```

with a deterministic diagnostic such as:

```text
FORWARD:UNANALYZED_METHOD_CALL: cannot prove effects of helper call updateItemThroughHelper
```

## Required Behavior

- Inspect method calls anywhere inside each forward lambda, including calls nested in assignments, conditions, loops, and return/value expressions.
- Preserve completeness only for method calls that are resolved as current recognized command plumbing:
  - dispatch through the supported `CommandGateway` call shape for a recognized command; and
  - input/key expressions nested inside a recognized command construction, including the existing DTO/key getter shape.
- Do not classify calls as safe from names such as `get...`, `read...`, or `prepare...` alone.
- Any other call, or a call whose relevant declaring/call shape cannot be resolved, must:
  - mark forward dispatch analysis incomplete;
  - add a structured `FORWARD:UNANALYZED_METHOD_CALL` diagnostic that identifies the call understandably;
  - force `CONSERVATIVE_UNKNOWN` when no higher-precedence explicit/write evidence applies; and
  - retain the compensation checkpoint.
- Keep evidence precedence unchanged:
  - explicit registration remains `EXPLICIT_COMPENSATION`;
  - a directly recognized write remains `IMPLICIT_SAGA_ROLLBACK` even if another call is unknown;
  - only a fully understood, non-empty, all-read forward lambda with no explicit compensation is omitted.
- Keep existing non-lambda/method-reference behavior conservative.
- Keep diagnostics deterministic and diagnostic-only; do not change semantic identity rules.
- Do not recursively analyze helper bodies or infer method purity.

## Narrow Implementation Boundary

Expected production scope:

- `WorkflowFunctionalityVisitor` forward-analysis completeness;
- existing building-block/adapter facts only if required to carry the diagnostic already supported by the pipeline;
- dummyapp fixture coverage;
- focused visitor, adapter, and generated-checkpoint/accounting tests;
- one concise clarification in current documentation only if current wording would otherwise overstate the proof.

Out of scope:

- recursive or whole-program effect analysis;
- purity annotations or method-name heuristics;
- new compensation evidence classes;
- runtime, executor, package schema, or report changes;
- recovery-schedule algorithm changes;
- items 4 and 5.

## Focused Fixture

Add a canonical dummyapp step whose direct lambda contains one recognized read dispatch followed by a helper call whose body contains a write dispatch. The helper body must remain outside the visitor's local analysis boundary so the regression proves conservative incompleteness rather than newly introducing interprocedural extraction.

Preserve the existing direct `readOnlyStep` as the positive omission control.

## Focused Tests

At minimum prove:

1. the mixed read/helper-write step has exactly the direct visible `READ` footprint and does not fabricate the hidden write footprint;
2. its forward analysis is incomplete;
3. it carries a deterministic `UNANALYZED_METHOD_CALL` forward diagnostic identifying the helper call;
4. its compensation evidence is `CONSERVATIVE_UNKNOWN`;
5. its compensation checkpoint is retained in a generated WorkloadPlan;
6. the existing direct read-only step remains complete, read-only, and omitted;
7. `commandGateway.send(...)` and getter/key expressions inside recognized command construction do not weaken that direct read-only proof;
8. explicit compensation still has highest precedence;
9. a directly visible write remains `IMPLICIT_SAGA_ROLLBACK`;
10. method-reference operations remain conservative;
11. dummyapp generation/accounting and recovery-checkpoint expectations remain deterministic after the retained checkpoint is added.

Use TDD where practical: establish at least one failing regression showing the mixed step was previously omitted before changing production code.

## Verification

Run from the verifier module:

```bash
cd verifiers
mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test
```

Add a narrower visitor-only red/green run if useful. Run scoped hygiene across every changed path:

```bash
git diff --check -- \
  applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination \
  verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor \
  verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock \
  verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter \
  verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor \
  verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario \
  docs/verifiers-impl \
  issues/2026-07-19-compensation-aware-scenario-catalog
```

## Evidence to Record

Append completion evidence to this file with:

- implementation summary;
- exact understood-call rule;
- exact files changed;
- red/green tests and what they prove;
- commands run and exact counts/results;
- deterministic fixture/accounting changes;
- deviations or remaining concerns.

Do not commit, move this remediation card, edit existing remediation review reports, begin item 4, or touch the two pre-existing meeting-note files.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- The visitor now inspects every method call in each forward lambda after extracting recognized command constructions.
- A call is understood only when it is resolved input/key evaluation nested within a recognized command construction, or a resolved one-argument `CommandGateway.send(...)` whose argument is that recognized construction or its directly initialized local variable. Recognition requires a resolved `Command` subtype with registry-backed dispatch evidence. Method names such as `get...`, `read...`, or `prepare...` are never purity evidence.
- Every other or unresolved forward-lambda call records deterministic `FORWARD:UNANALYZED_METHOD_CALL` evidence and makes forward analysis incomplete. Existing adapter precedence then retains `CONSERVATIVE_UNKNOWN` unless explicit registration or a visible write wins.
- No helper body is traversed: the fixture helper's hidden `UpdateItemCommand` is deliberately absent from the mixed step's footprints.

### Files Changed

- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java` — added the mixed direct-read/helper-write fixture and an unknown-call precedence check on the direct-write step.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java` — added the local forward-lambda understood-call boundary and deterministic diagnostic.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy` — proves one visible read only, unknown helper diagnostics, and the direct read-only plumbing control.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy` — proves unknown/read classification, direct-write precedence, diagnostic text, and read-only omission.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy` — updates parser-real checkpoint/recovery accounting and fixture tiers.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/003-conservative-effect-free-proof.md` — records this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` (red) | FAIL (expected) | 48 tests ran; 4 regressions failed because `mixedReadHelperStep` remained complete, was omitted, and produced only 4 checkpoints. |
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec test` | PASS | 19 tests run, 0 failures, 0 errors, 0 skipped after one compile-only correction for a missing `CommandGateway` import. |
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` (final) | PASS | 48 tests run, 0 failures, 0 errors, 0 skipped; Maven `BUILD SUCCESS`. Counts: visitor 19, adapter 14, dummyapp foundation 15. |
| Required scoped `git diff --check -- ...` | PASS | No whitespace errors in the tracked scoped diff; a separate scan confirms the appended completion-evidence section has no trailing whitespace. |

### Acceptance Criteria Evidence

- AC-8: `mixedReadHelperStep` has exactly one direct `READ` footprint, incomplete forward analysis, the exact diagnostic `FORWARD:UNANALYZED_METHOD_CALL: cannot prove effects of helper call updateItemThroughHelper`, `CONSERVATIVE_UNKNOWN`, and a retained checkpoint. `readOnlyStep` remains complete and omitted; `implicitWriteStep` remains `IMPLICIT_SAGA_ROLLBACK` despite the unknown helper call; existing explicit and method-reference cases remain unchanged.
- AC-44: The parser-real dummyapp fixture now produces 6 fault slots and 5 retained checkpoints instead of 5 and 4. The final-step recovery vector changes deterministically from `00001` to `000001`; its one recovery schedule contains all 5 eligible checkpoints in reverse order. Existing eager accounting remains 7 WorkloadPlans, 6 materializable/1 blocked, 14 computed vectors, and 14 FaultScenarios.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the dummyapp fixture and focused visitor/adapter/accounting expectations first. The required three-spec command failed in the expected pre-fix state with 4 failures across 48 tests, including the previously omitted mixed checkpoint. The same command is green after the visitor-only fix.

### Deviations From Plan

- No production-boundary deviation. No building-block, adapter, generator, schema, runtime, executor, package, recovery-algorithm, or documentation change was needed.
- One intermediate visitor-only run failed compilation because the new `CommandGateway` reference lacked its import; the import was added and all subsequent focused runs passed.

### Blockers / Follow-Ups

- None. Helper bodies remain intentionally unanalyzed, so the retained checkpoint is conservative evidence rather than a fabricated hidden write footprint.

## Review Attempt 01 Fix Evidence

Status: `implemented-awaiting-review`

### Fix Summary and Exact Safety Rules

- Narrowed constructor-argument access to one explicit local shape: the call must be inside a recognized registry-backed `Command` construction, have a direct `NameExpr` receiver, resolve to a non-static zero-argument JavaBean `getX` declaration, and have matching receiver/declaring types whose resolved FQN ends in `Dto`. A workflow/`this` receiver, chained or field-access receiver, arguments, static call, non-DTO declaration, or any resolution failure is unknown. The `getX` spelling is therefore insufficient by itself; the direct resolved DTO receiver/declaration constraints are mandatory.
- Narrowed dispatch plumbing to calls whose resolved declaration is exactly base `CommandGateway.send(Command)`: declaring FQN `pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway`, one parameter whose resolved type is exactly `Command`, and a resolved `CommandGateway`-subtype scope. Subtype overloads, unrelated `send`, other signatures, and unresolved declarations are unknown.
- Inline recognized command construction remains supported. A local argument is supported only when its resolved `NameExpr` declaration maps by compilation-unit path and source range to the exact in-lambda `VariableDeclarator` whose direct initializer is the recognized construction. Simple-name equality is not used.
- Every rejected call retains the existing deterministic `FORWARD:UNANALYZED_METHOD_CALL: cannot prove effects of helper call <name>` diagnostic. No helper body is traversed.

### Adversarial Parser-Real Coverage

- `constructorKeyHelperStep`: a resolved workflow helper deliberately named `getAndUpdateItemKey` performs a hidden `UpdateItemCommand` dispatch and returns the read key. The step exposes exactly one visible `READ`, is incomplete, carries the exact helper diagnostic, classifies `CONSERVATIVE_UNKNOWN`, and contributes a checkpoint without fabricating the hidden write.
- `overloadedGatewayStep`: a `CommandGateway` subtype's `send(GetItemCommand)` overload resolves but is rejected because it is not the base declaration.
- `unrelatedSendStep`: an unrelated resolved one-argument `send(GetItemCommand)` is rejected.
- `mismatchedCommandBindingStep`: a nested-lambda local recognized command deliberately shadows an outside-lambda field of the same text; dispatch of the field is rejected by resolved declaration identity.
- `inlineReadOnlyStep` and the existing direct-local `readOnlyStep`: resolved direct DTO key access plus exact base dispatch remains complete and omitted for both inline and exact directly initialized-local command shapes.

### Files Changed for Attempt 01 Fix

- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java` — added constructor-helper, overload, unrelated-send, binding-shadow, and inline controls.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java` — enforce direct DTO access, exact base dispatch declaration, and resolved local declaration identity.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy` — parser-real adversarial call-shape and positive-control assertions.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy` — conservative evidence/diagnostic and omission assertions across adversarial shapes.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy` — parser-real matrix and deterministic checkpoint accounting updates.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/003-conservative-effect-free-proof.md` — this attempt-01 fix evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` (attempt-01 red) | FAIL (expected) | 49 tests ran with 4 failures: constructor helper, subtype overload, and same-text wrong binding remained complete/omitted, leaving only 6 checkpoints instead of the expected 9. The unrelated `send` regression was already conservative. |
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec test` (narrow green) | PASS | 20 tests run, 0 failures, 0 errors, 0 skipped after declaration/binding correction. |
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` (final) | PASS | 49 tests run, 0 failures, 0 errors, 0 skipped; Maven `BUILD SUCCESS`. Counts: visitor 20, adapter 14, dummyapp foundation 15. |
| Required scoped `git diff --check -- ...` | PASS | No whitespace errors in the tracked scoped diff; no temporary probe markers or stale helper names remain. |

### Accounting and Precedence

- The parser-real compensation fixture now has 11 forward fault slots and 9 retained checkpoints; both supported read-only controls are omitted.
- Final read-only fault vector is deterministically `00000000001`; its single recovery schedule contains all 9 eligible checkpoints in reverse checkpoint order.
- Eager accounting remains 7 WorkloadPlans, 6 materializable/1 blocked, 14 computed vectors, and 14 FaultScenarios.
- Existing explicit precedence, visible-write `IMPLICIT_SAGA_ROLLBACK` despite unknown calls, method-reference conservatism, mixed helper behavior, diagnostic ordering, and semantic identity rules remain unchanged.

### Deviations / Remaining Concerns

- No scope deviation and no remaining implementation concern. No building-block, adapter production, generator, schema, runtime, executor, package, recovery algorithm, or documentation change was required.

## Review Outcome

- Attempt 01: `FAIL` — arbitrary constructor-argument calls and textual gateway/local matching left unsafe omission paths. See `../remediation-review/003-conservative-effect-free-proof-review-01.md`.
- Attempt 02: `PASS` — both findings are resolved, with no blocking, major, minor, or note finding remaining. See `../remediation-review/003-conservative-effect-free-proof-review-02.md`.
- Fresh reviewer verification: 49 focused tests and one additional parser-real binding/getter probe passed with 0 failures, errors, or skips.
- Final state: remediation item 3 is review-complete; no commit has been made.
