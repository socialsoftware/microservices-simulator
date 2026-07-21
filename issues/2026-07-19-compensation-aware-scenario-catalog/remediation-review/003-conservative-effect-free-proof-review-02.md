# Remediation Review: 003 - Conservative Effect-Free Proof

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes` (caller transition after PASS)

## Sources Reviewed

- Completed remediation card, original completion evidence, and appended attempt-01 fix evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/003-conservative-effect-free-proof.md`
- Parent contract: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`, especially lines 18, 42, 106-108, and AC-8 at line 255
- Independent follow-up: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Parent implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Original compensation-evidence slice: `issues/2026-07-19-compensation-aware-scenario-catalog/done/001-compensation-evidence-preservation.md`
- Attempt-01 report: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-review/003-conservative-effect-free-proof-review-01.md`
- Canonical context: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- Reviewed dependencies, treated as fixed reviewed baselines rather than re-reviewed:
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md`
  - `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md`
  - their existing remediation review reports
- Complete updated item-003 production/fixture/test diff:
  - `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy`
- Relevant unchanged production anchors: `SagaStepBuildingBlock.java`, `ApplicationAnalysisScenarioModelAdapter.java`, `ScenarioGenerator.java`, `TypeUtils.java`, and simulator `CommandGateway.java`

## Summary

Both attempt-01 findings are resolved, and the complete remediation now satisfies the approved conservative-effect boundary.

Constructor calls are no longer broadly exempted. The only supported nested input/key call is a resolved, non-static, zero-argument `getX` declaration on a direct `NameExpr` receiver where the receiver's resolved type exactly equals the declaring type and the FQN ends in `Dto`. A workflow helper named `getAndUpdateItemKey` is rejected, as are chained, field-access/`this`, argument-bearing, static, non-DTO, inherited/mismatched, or unresolved shapes. This combines call resolution, receiver shape, exact declaring type, and the approved DTO boundary; `get...` spelling alone is insufficient. Under the card's explicit DTO/key-access exception and no-helper-recursion constraint, no remaining accepted shape outside that approved boundary was found.

Command dispatch is accepted only when the resolved declaration is exactly base `CommandGateway.send(Command)`, with a resolved `CommandGateway`-subtype scope and a recognized inline command or exact directly initialized local. Subtype overloads and unrelated `send` methods are conservative. Local matching uses resolved declaration AST identity represented by compilation-unit path plus source range; it no longer uses source text. The checked-in parser-real fixture and an additional temporary probe both confirm outside-lambda and same-name-shadow declarations are rejected.

The required 49-test command and a targeted parser-real binding/getter probe pass. The 11-slot/9-checkpoint fixture expansion is directly tied to the original regression and two attempt-01 corrections plus one positive inline control; it does not change eager dummyapp accounting or introduce runtime/package/recovery changes.

## Attempt-01 Resolution

| Prior Finding | Verdict | Evidence |
|---|---|---|
| Blocking: arbitrary resolved constructor-argument calls were treated as understood | resolved | `WorkflowFunctionalityVisitor.java:303-326` now requires direct `NameExpr` scope, zero arguments, non-static resolved declaration, `get[A-Z].*`, exact receiver/declaring FQN equality, and `*Dto`. `constructorKeyHelperStep` at fixture lines 67-70 exposes only a read, becomes incomplete, and records `getAndUpdateItemKey`; tests at `WorkflowFunctionalityVisitorSpec.groovy:130-150` and adapter lines 380-390 pass. |
| Major: dispatch declaration was not checked and local matching used names | resolved | `WorkflowFunctionalityVisitor.java:344-369` validates exact base declaring FQN and exact `Command` parameter; lines 371-395 resolve the argument declaration and compare path/range identity. Overload, unrelated sender, checked-in same-name mismatch, temporary outside-local, and block-shadow probes all remain conservative. |

## Actual Understood-Call Rule

1. Traverse every `ObjectCreationExpr` descendant of the forward lambda.
2. Recognize a command construction only when its type resolves to a `Command` subtype and the command registry provides dispatch information; emit only that visible construction's footprint.
3. Retain the exact `VariableDeclarator` only when its initializer node is exactly the recognized command construction.
4. Traverse every `MethodCallExpr` descendant of the forward lambda, including assignment/value, condition, loop, and nested-lambda descendants.
5. Understand a nested input/key call only when all conditions hold:
   - it is under the recognized command construction before crossing a lambda boundary;
   - it has zero arguments;
   - its scope is directly a `NameExpr`;
   - the call resolves;
   - the declaration is non-static and zero-argument;
   - its resolved name has JavaBean `getX` form;
   - receiver type is resolved and exactly equals the declaring type;
   - that exact FQN ends in `Dto`.
6. Understand command dispatch only when all conditions hold:
   - source call shape is scoped, named `send`, and has one argument;
   - the argument is the exact recognized inline construction or resolves to the exact recognized direct-initializer local declaration;
   - the method resolves with declaring FQN exactly `CommandGateway`;
   - it has exactly one parameter whose type describes exactly `Command`;
   - the scope resolves to a `CommandGateway` subtype.
7. Every other or unresolved call records `FORWARD:UNANALYZED_METHOD_CALL: cannot prove effects of helper call <name>` and makes forward analysis incomplete.
8. The unchanged adapter sorts diagnostic strings and applies `EXPLICIT_COMPENSATION` > visible write > complete non-empty all-read omission > `CONSERVATIVE_UNKNOWN`.

## Contract Compliance

| Requirement | Verdict | Evidence |
|---|---|---|
| No checkpoint omitted merely because an operation was ignored | pass | All non-approved calls become incomplete at `WorkflowFunctionalityVisitor.java:289-299`; constructor helper, overload, unrelated sender, field/shadow mismatch, outside-local probe, and getter-name-only probe are conservative. |
| Calls inspected anywhere in forward lambda | pass | `lambda.findAll(MethodCallExpr.class)` at lines 289-299 covers assignments/value expressions, conditions, loops, returns where legal, and nested descendants. |
| Registry-backed command recognition | pass | Resolved `Command` subtype plus `state.getCommandDispatchInfo(...)` at lines 243-286. |
| DTO/key exception is structural, not method-name-only | pass | Lines 303-326 require resolution, direct receiver, exact type/declaration match, non-static zero-argument shape, and `*Dto`; temporary direct-NameExpr non-DTO `getItemKey` probe is conservative. |
| Getter-style workflow helper with hidden write is conservative | pass | `getAndUpdateItemKey` at fixture lines 110-113 is not a direct DTO getter; its step has one visible read, no hidden write footprint, one deterministic diagnostic, and `CONSERVATIVE_UNKNOWN`. |
| Exact supported base gateway dispatch | pass | Lines 344-369 require resolved base `CommandGateway.send(Command)`; checked-in subtype overload and unrelated sender tests pass. |
| Exact local declaration binding | pass | Lines 371-395 compare the resolved declaration's path/range against retained direct-initializer declarators. Checked-in field/nested-shadow and temporary outside-local/block-shadow cases are conservative. |
| Inline recognized command dispatch | pass | `inlineReadOnlyStep` at fixture lines 89-90 remains complete and omitted; visitor and adapter controls pass. |
| Direct-local recognized command dispatch | pass | `readOnlyStep` at fixture lines 92-95 remains complete and omitted; declaration resolution succeeds against its exact initializer. |
| Unresolved/scoped/chained/argument-bearing unsupported calls | pass | Any failed resolution or failed structural predicate returns false and produces an unknown-call diagnostic. Chained scopes are not `NameExpr`; unsupported constructor helper uses `this` plus an argument and is rejected. |
| Mixed direct-read/helper-write fixture | pass | Fixture lines 61-65; exactly one direct `READ`, incomplete analysis, exact `updateItemThroughHelper` diagnostic, `CONSERVATIVE_UNKNOWN`, retained checkpoint. Helper's `UpdateItemCommand` is absent from the step footprint. |
| Constructor-helper fixture | pass | Fixture lines 67-70 and 110-113; exactly one direct `READ`, incomplete analysis, exact getter-style helper diagnostic, `CONSERVATIVE_UNKNOWN`, retained checkpoint. |
| Explicit/write precedence | pass | Adapter remains unchanged. Explicit fixtures remain `EXPLICIT_COMPENSATION`; `implicitWriteStep` remains `IMPLICIT_SAGA_ROLLBACK` despite its unknown helper call (`ApplicationAnalysisScenarioModelAdapterSpec.groovy:357-378`). |
| Method-reference conservatism | pass | Non-lambda branch at visitor lines 229-237 and visitor test lines 166-173 remain unchanged and green. |
| No helper recursion or fabricated hidden helper footprint | pass | Only AST-local command constructions are extracted. Mixed and constructor helpers each expose one direct read; helper-body writes are not traversed or fabricated. |
| Deterministic diagnostic-only behavior | pass | AST traversal is deterministic for fixed source; adapter sorts diagnostics at `ApplicationAnalysisScenarioModelAdapter.java:192-195`; no identity code changed. Repeated distinct same-name calls can yield repeated diagnostic strings, but deterministically and outside semantic identity. |
| Scope/non-goals | pass | No building-block, adapter production, generator, schema, runtime, executor, package, report, documentation, or recovery-algorithm change; no helper recursion/purity framework and no item 4/5 work. |
| Dependencies/protected files preserved | pass | Remediation-001/002 cards, code, reports, and docs retain their pre-item-3 mtimes/content. Attempt-01 report SHA-256 remains `d3bb93b9a8b9866d370b36df505d4e73a4d6206fd0f0ccef1333baa34348aa8e`. Meeting-note mtimes remain 2026-07-19 and their hashes are unchanged. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-8 effect-free proof and precedence | pass | Exact adversarial parser fixtures and temporary probes now make all unsupported calls incomplete; only two fully resolved all-read controls omit checkpoints. Explicit/write/unknown precedence remains unchanged. | Both attempt-01 unsafe omissions are closed. |
| AC-44 dummyapp-focused evidence | pass | Visitor, adapter, and parser-real accounting tests pass with the expanded 11-step matrix, retained checkpoint set, recovery order, stable eager counts, and byte-stability checks. | This is the remediation-003 portion of AC-44. |

## Verified Fixture and Accounting

### Canonical order

Forward fault-slot/source order is:

1. `createItemStep`
2. `explicitWithoutRecognizedDispatchStep`
3. `implicitWriteStep`
4. `conservativeUnresolvedStep`
5. `mixedReadHelperStep`
6. `constructorKeyHelperStep`
7. `overloadedGatewayStep`
8. `unrelatedSendStep`
9. `mismatchedCommandBindingStep`
10. `inlineReadOnlyStep`
11. `readOnlyStep`

The first nine are retained checkpoints in that order. `inlineReadOnlyStep` and `readOnlyStep` are the only omitted checkpoints. This follows fixture source/order indexes and `ScenarioGenerator`'s ordered schedule/checkpoint construction.

### Recovery/accounting

- Fault slots: `11`.
- Retained checkpoints: `9`.
- Final read-only fault vector: `00000000001`.
- The final bit faults `readOnlyStep`; all first nine checkpoints are eligible, while completed `inlineReadOnlyStep` has no checkpoint.
- Uncapped recovery count: `1`; written recovery schedules: `1`.
- Compensation action order is the nine checkpoint IDs in exact reverse order: mismatched binding, unrelated sender, overloaded gateway, constructor helper, mixed helper, method-reference conservative, implicit write, explicit-without-dispatch, create-item explicit.
- Eager dummyapp accounting remains `7` WorkloadPlans, `6` materializable / `1` blocked, `14` computed vectors, and `14` FaultScenarios (`DummyappAccountingFixtureFoundationSpec.groovy:203-211` in the current file).
- The fixed configuration/timestamp byte-stability test remains in the passing foundation suite.
- Unrelated `loopedStaticReadStep` remains complete/omitted. `loopedRuntimeReadStep` remains deterministically conservative because `Arrays.asList`, two key getters outside recognized command construction, and `forEach` are not supported plumbing; the fresh logs show the same four ordered diagnostics. Other dummyapp read/write classifications and eager totals remain stable.

The expansion from 6 to 11 fixture steps is justified: four adversarial steps directly cover constructor-helper, overloaded-send, unrelated-send, and wrong-binding boundaries, while one inline step is the required positive dispatch control. No unrelated production feature or runtime behavior was added.

## Verification Evidence Check

| Command / method | Verdict | Notes |
|---|---|---|
| `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` | pass | Fresh `BUILD SUCCESS`; 49 tests, 0 failures, 0 errors, 0 skipped: visitor 20, adapter 14, foundation 15. Log SHA-256 `8ae53930df45a6289f794ad99de054f9f1143320eb22e503812cf7fc92327b62`. |
| Temporary parser-real getter/binding probe | pass | 1 test, 0 failures/errors/skips. Direct-NameExpr zero-argument getter on a non-DTO helper with hidden write, outside-lambda local command, and block-local same-name shadow all classify conservative. Log: `/tmp/remediation-003-review02-binding-probe.log`; SHA-256 `875d56730bdade25a4fab14bb5a50d862d2d84b8e8a849dd8043c9bac5ceab49`. Temporary files removed. |
| Required scoped `git diff --check -- ...` | pass | Exit 0, no output across every prescribed source, test, docs, and issue path. |
| Complete diff/status/conflict/protected-file hygiene | pass | Exactly five item-003 production/fixture/test files plus the active card contain item-003 implementation work. No conflict markers or temporary probes remain. Remediation-001/002 artifacts and meeting notes are untouched. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|---|---|---|
| Minimality | pass | Production correction is confined to the existing visitor boundary. Added fixture cases each cover a specific safety rule or positive control. |
| Existing patterns | pass | Uses JavaParser resolution, existing command registry facts, retained building-block diagnostics, and unchanged adapter precedence. |
| Test quality | pass | Parser-real tests assert footprints, completeness, diagnostics, evidence classes, omission controls, checkpoint counts, recovery order, and stable eager accounting; independent temporary probes cover outside-local and stricter getter-name-only cases. |
| Regression risk | pass | Focused affected suites and additional adversarial probe pass; unrelated fixture diagnostics/counts remain deterministic. |
| Security/data safety | n/a | Static-analysis/catalog semantics only; no persistence, runtime mutation, or external data path changed. |
| Change hygiene | pass | No dependency, schema, report, runtime, executor, documentation, prior-review, meeting-note, item-4, or item-5 edit. |

## Findings

None.

## Reviewer Notes

The accepted DTO/key getter rule is intentionally a narrow syntactic trust boundary, not whole-program proof that arbitrary Java getter bodies are pure. That limitation is explicit in the card's supported DTO/key-access exception and prohibition on helper-body recursion/purity inference. Within the approved boundary, the implementation additionally requires resolution, exact declaring/receiver type, direct receiver syntax, zero arguments, non-static declaration, and DTO FQN; no broader accepted shape was found.

## Done Transition

- Moved to done: `yes`, by the caller after the reviewer returned `PASS`.
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/003-conservative-effect-free-proof.md`
- Reviewer constraint respected: the reviewer did not move the card during attempt 02.

## Recommendation

Accept remediation 003 as review-complete. Preserve the current uncommitted implementation and both review attempts. Any card/status transition must be performed separately by the caller-authorized workflow. Do not start items 4 or 5 as part of this review.
