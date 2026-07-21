# Remediation Review: 003 - Conservative Effect-Free Proof

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Remediation card and completion evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/003-conservative-effect-free-proof.md`
- Parent contract: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`, especially the effect-free rule and AC-8
- Independent follow-up: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Original compensation-evidence slice and review:
  - `issues/2026-07-19-compensation-aware-scenario-catalog/done/001-compensation-evidence-preservation.md`
  - `issues/2026-07-19-compensation-aware-scenario-catalog/review/001-compensation-evidence-preservation-review.md`
- Parent implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Canonical context: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- Reviewed dependencies, treated as fixed baselines rather than re-reviewed:
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md`
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md`
  - their latest remediation review reports
- Every item-003 production/fixture/test diff:
  - `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy`
- Relevant unchanged production anchors: `SagaStepBuildingBlock.java`, `ApplicationAnalysisScenarioModelAdapter.java`, `ScenarioGenerator.java`, `CommandGateway.java`, `TypeUtils.java`

## Summary

The focused fixture works, precedence is preserved, and the required 48-test command passes. The implementation nevertheless fails the core safety property.

`isResolvedInputCallInsideRecognizedCommand` treats **every resolved method call anywhere beneath a recognized command construction** as understood. A helper that performs a hidden write and returns a constructor key is therefore ignored. A fresh temporary parser-real probe produced one visible `READ`, `forwardAnalysisComplete=true`, no diagnostic, and `compensationEvidence=null`; the adapter consequently omits the checkpoint.

The supported-dispatch predicate is also broader than the stated rule. It invokes `call.resolve()` but never inspects the resolved declaration. Any one-argument method textually named `send` on a `CommandGateway` subtype is accepted, including an overload declared by that subtype. The same probe showed an overloaded `send(GetItemCommand)` remained complete and was omitted. Directly initialized command locals are tied to dispatch arguments by variable-name text rather than a resolved declaration binding.

These are not missing edge-case niceties: both paths can hide a write while leaving only visible read evidence, which is exactly the unsafe omission this remediation must prevent.

## Actual Implementation Rule

The current code implements this rule:

1. Traverse all `ObjectCreationExpr` descendants of the forward lambda, including nested scopes/lambdas.
2. Recognize a construction when its type resolves to a `Command` subtype and `ApplicationAnalysisState` has registry-backed dispatch information; emit its footprint.
3. Record the simple name of any ancestor `VariableDeclarator` whose initializer node is exactly that recognized construction.
4. Traverse all `MethodCallExpr` descendants of the forward lambda.
5. Treat a call as understood when either:
   - it resolves and has any recognized command construction as an ancestor before the next lambda boundary; the resolved declaration and effect are otherwise unrestricted; or
   - its source name is `send`, it has one argument and a scope, its argument is an inline recognized construction or a `NameExpr` whose simple text is in the recorded-name set, `call.resolve()` does not throw, and the scope expression's resolved type is a `CommandGateway` subtype.
6. Mark every remaining call incomplete with `FORWARD:UNANALYZED_METHOD_CALL: cannot prove effects of helper call <simpleName>`.
7. The adapter sorts diagnostic strings and applies `EXPLICIT_COMPENSATION` > visible write > complete non-empty all-read omission > `CONSERVATIVE_UNKNOWN`.

This is deterministic for a fixed AST, uses registry-backed command recognition, and does not use `get...`/`read...` names as purity evidence. It does **not** establish the required resolved declaring-method or resolved local-binding constraints, and its constructor-argument exemption is substantially broader than the permitted input/key getter shape.

## Contract Compliance

| Requirement | Verdict | Evidence |
|---|---|---|
| No checkpoint omitted merely because an operation was ignored | **fail** | `WorkflowFunctionalityVisitor.java:302-325` accepts arbitrary resolved calls below command construction; temporary probe logged `constructor complete=true evidence=null diagnostics=[]`. Adapter omission follows at `ApplicationAnalysisScenarioModelAdapter.java:237-240`. |
| Inspect calls in assignments, conditions, loops, returns/value expressions, and nested lambdas | pass, structurally | `lambda.findAll(MethodCallExpr.class)` at `WorkflowFunctionalityVisitor.java:288-299` traverses all descendant calls. This breadth does not make the unsafe understood-call exemptions sound. |
| Registry-backed recognized command construction | pass | Resolved `Command` subtype and `state.getCommandDispatchInfo(...)` are required at `WorkflowFunctionalityVisitor.java:242-285`. |
| Do not infer safety from `get`/`read`/`prepare` names | pass | No such name heuristic exists. Calls outside the two exemptions are conservative. |
| Supported one-argument `CommandGateway` dispatch only | **fail** | `WorkflowFunctionalityVisitor.java:328-347` checks source name and scope type, not the resolved method's declaring type/signature; a subtype overload is accepted. |
| Inline or directly initialized local recognized command only | **fail** | Inline identity is exact, but locals are stored and matched by simple string name at `WorkflowFunctionalityVisitor.java:273-277,335-339`, not by resolved `VariableDeclarator` identity. |
| Unresolved calls conservative | pass | Both understood-call helpers return false on resolution exceptions, so the call becomes `UNANALYZED_METHOD_CALL`. |
| Direct read-only command/getter/send remains complete and omitted | pass | Fixture and tests at `WorkflowFunctionalityVisitorSpec.groovy:130-138` and `ApplicationAnalysisScenarioModelAdapterSpec.groovy:380-383`; fresh focused suite passes. |
| Mixed direct-read/helper-write remains conservative | pass for the exact fixture | `CreateItemCompensationFunctionalitySagas.java:57-60`; tests prove one visible read, incomplete analysis, exact diagnostic, `CONSERVATIVE_UNKNOWN`, and retained checkpoint. No hidden helper footprint is fabricated. |
| Precedence remains explicit > visible write > unknown | pass | Adapter is unchanged at `ApplicationAnalysisScenarioModelAdapter.java:228-240`; fixture checks explicit and direct-write precedence at `ApplicationAnalysisScenarioModelAdapterSpec.groovy:357-378`. |
| Method references remain conservative | pass | Existing non-lambda branch at `WorkflowFunctionalityVisitor.java:228-236`; focused test at `WorkflowFunctionalityVisitorSpec.groovy:140-147`. |
| No helper-body recursion/fabricated hidden helper footprint | pass for ordinary helpers | The fixture helper body is not traversed; mixed step exposes only the direct read. |
| Deterministic diagnostics | pass with note | Calls are visited in deterministic AST order and adapter strings are sorted at `ApplicationAnalysisScenarioModelAdapter.java:192-195`. `SagaStepBuildingBlock.java:61-67` does not deduplicate; repeated same-name calls produce repeated indistinguishable messages, but this is deterministic and diagnostic-only. |
| Scope/non-goals | pass | No recursive effect analysis, purity annotations, schema/runtime/executor/package/recovery algorithm changes, or item-4/5 work. |
| Dependencies preserved | pass | Post-21:05 path/mtime audit found only the five expected item-003 source/test files, the active card, and the independent follow-up changed; remediation-001/002 code, cards, reports, and docs predate item 3 and were not modified by it. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-8 effect-free omission proof | **fail** | Fresh safety probe demonstrated complete all-read omission despite a resolved constructor-argument helper with a hidden write and despite a subtype `send` overload. | Core acceptance contract is violated. |
| AC-44 dummyapp-focused evidence | partial | The exact mixed fixture and accounting checks pass, but the test matrix does not cover constructor-argument helpers, resolved chained calls, overloaded gateway methods, or resolved local declaration identity. | Passing fixture is too narrow to establish the general safety rule. |

## Verified Fixture and Accounting

- Compensation fixture forward order is source/order-index order: `createItemStep`, `explicitWithoutRecognizedDispatchStep`, `implicitWriteStep`, `conservativeUnresolvedStep`, `mixedReadHelperStep`, `readOnlyStep`.
- The generated plan has `6` forward fault slots and `5` checkpoints; only `readOnlyStep` is omitted in this fixture.
- `mixedReadHelperStep` exposes exactly `[READ]`, is incomplete, has exact diagnostic `FORWARD:UNANALYZED_METHOD_CALL: cannot prove effects of helper call updateItemThroughHelper`, classifies `CONSERVATIVE_UNKNOWN`, and contributes the fifth retained checkpoint.
- Vector `000001` faults the final read-only step after all five retained checkpoints are eligible. The one recovery schedule compensates all five in reverse checkpoint order; `DummyappAccountingFixtureFoundationSpec.groovy:171-180` verifies the complete reverse-ID sequence.
- Eager accounting remains deterministic at `7` WorkloadPlans, `6` materializable / `1` blocked, `14` computed vectors, and `14` FaultScenarios (`DummyappAccountingFixtureFoundationSpec.groovy:197-204`).
- Direct static-loop read plumbing remains complete/omitted. The unrelated `loopedRuntimeReadStep` is now conservatively incomplete because `Arrays.asList`, two key getters outside command construction, and `forEach` are outside the whitelist; the fresh run emitted four deterministic diagnostics. This reclassification follows the card's narrow whitelist but is not asserted or disclosed by the item-003 tests.

## Verification Evidence Check

| Command / method | Verdict | Notes |
|---|---|---|
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` | pass | Fresh `BUILD SUCCESS`; 48 tests, 0 failures, 0 errors, 0 skipped: visitor 19, adapter 14, dummyapp foundation 15. Full output: `/tmp/pi-bash-ba82317449f65d63.log`. |
| Temporary parser-real safety probe: constructor-argument write helper and overloaded gateway `send` | **fail as required safety assertion** | 1 test, 1 failure. Logged `constructor complete=true evidence=null diagnostics=[]` and `overloaded complete=true evidence=null diagnostics=[]`. Probe log: `/tmp/remediation-003-safety-probe-2.log`, SHA-256 `4b709a19617bbb2b0dde70cad91f79cbc88fe220937467b48fedab2084018c85`. Temporary fixture/spec were removed. |
| Required scoped `git diff --check -- ...` | pass | Exit 0, no output. |
| Scoped diff/status/conflict/protected-file inspection | pass | Exactly five item-003 production/fixture/test paths are changed; no conflict markers; temporary probe files absent; the two meeting-note files retain 2026-07-19 mtimes and were untouched. |

## Findings

| Severity | Finding | Evidence | Violated Contract | Required Correction |
|---|---|---|---|---|
| **blocking** | Arbitrary resolved calls nested in recognized command construction are treated as effect-free input/key plumbing. A helper can write and return the read command's key, leaving one visible read and causing checkpoint omission. | `WorkflowFunctionalityVisitor.java:302-325`; adapter omission at `ApplicationAnalysisScenarioModelAdapter.java:237-240`; temporary probe: `constructor complete=true evidence=null diagnostics=[]`. | Remediation plain-language/core safety rule; Required Behavior restricting preservation to understood input/key expressions; spec AC-8 requiring no unresolved diagnostic before omission. | Narrow this exemption to the explicitly supported, resolved input/key access shape. Any other constructor-argument/chained/scoped call must emit deterministic `FORWARD:UNANALYZED_METHOD_CALL` and make analysis incomplete. Add a parser-real regression where a resolved helper writes and returns a constructor key, and prove the checkpoint is retained without fabricating the hidden write footprint. |
| **major** | Supported dispatch is not established from the resolved method declaration, and local-command association is name-based. A `CommandGateway` subtype overload `send(GetItemCommand)` is accepted even though it is not the supported base dispatch; a same-text `NameExpr` is accepted without resolving to the directly initialized `VariableDeclarator`. | `WorkflowFunctionalityVisitor.java:273-277,328-347`; `call.resolve()` result is discarded at `:344-346`; temporary probe: `overloaded complete=true evidence=null diagnostics=[]`. | Required resolved call/declaring-type rule; supported one-argument `CommandGateway` dispatch only when tied to recognized inline/direct local construction; conservative handling of overloaded/unresolved shapes. | Validate the resolved declaration/signature as the supported `CommandGateway.send(Command)` contract, not merely a subtype scope and method text. Resolve a local argument to the exact in-lambda `VariableDeclarator` whose direct initializer is the recognized construction. Treat overloads, inherited unrelated shapes, ambiguous bindings, and failed resolution as unknown. Add focused overload, outside-lambda/local-binding, and unrelated-`send` regressions. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|---|---|---|
| Minimality | pass | Change stays in the visitor and canonical dummyapp/tests. |
| Existing patterns | partial | Registry and structured diagnostic patterns are reused, but declaration/binding resolution is not carried through the new predicate. |
| Test quality | **fail** | Exact happy-path fixture is meaningful, but required adversarial call shapes are absent and two unsafe omissions pass unnoticed. |
| Regression risk | **fail** | The focused suite is green while the core safety invariant is false. |
| Security/data safety | n/a | Static-analysis/catalog semantics only. |
| Change hygiene | pass | Dependencies, reports, docs, meeting notes, runtime/package/executor code, and item 4/5 remain untouched by item 3. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason: `FAIL`; blocking and major safety findings remain. The remediation card stays active as explicitly required.

## Recommendation

Do not accept or move remediation 003. Correct both understood-call predicates and add the adversarial parser-real regressions above, then rerun the exact 48-test command plus the new safety matrix. Preserve the reviewed remediation-001/002 changes and do not begin items 4 or 5.
