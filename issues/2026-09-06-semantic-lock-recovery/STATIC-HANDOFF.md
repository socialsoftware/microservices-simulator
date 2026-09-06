# Static semantic-lock correction handoff

- **State:** complete; source frozen for integration and runtime qualification.
- **Outcome:** an exact dispatched `SagaCommand` with a supported, non-null
  `setSemanticLock(...)` configuration now preserves the payload access and adds one
  same-aggregate, same-key `WRITE` dispatch. The existing adapter therefore classifies
  a read payload with a semantic-state write as `IMPLICIT_SAGA_ROLLBACK`, and the
  existing conflict and recovery generators consume it without a new model type.

## Actual changes

- `WorkflowFunctionalityVisitor` associates a locally constructed wrapper with its
  exact locally constructed payload and exact `CommandGateway.send`/`sendAsync`
  occurrence. It uses the last supported setter before each send, treats explicit
  `null` as no state write, and does not apply setters that occur only after a send.
- The dummyapp compensation fixture covers an arbitrary semantic state,
  `GenericSagaState.NOT_IN_SAGA`, forbidden-state-only verification, post-send
  configuration, and helper-hidden configuration.
- Visitor tests additionally cover an undispatched locked wrapper, conditional null
  override, declaration and assignment aliases, a wrapper reused across a loop,
  custom `SagaCommand` subtypes, and anonymous subclasses.
- Adapter, conflict, recovery, and realistic Quizzes source assertions prove the
  downstream effects. The current Quizzes source yields 64 semantic-state writes: 38
  forward and 26 compensation. RemoveTournament's `getTournamentStep` retains its
  payload `READ`, gains the same-key `WRITE`, and now owns an implicit rollback
  checkpoint.

Files changed for this static slice:

- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitorSpec.groovy`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/SourceDerivedSharedSagaWorkloadAnalysisSpec.groovy`
- this handoff

No static building-block, adapter implementation, scenario-model, conflict, recovery,
executor, application production source, or package-contract change was needed.

## Discovery and decisions

- `setForbiddenStates(...)` is a framework read check. The payload already supplies
  the same aggregate/key read dependency; a write payload subsumes it for conflict
  purposes, so no duplicate read footprint was added.
- Enum member names have no role. `NOT_IN_SAGA` is treated as a write because the
  runtime persists every non-null semantic lock.
- The existing dispatch footprint is sufficient evidence: a second `SagaCommand`
  `WRITE` feeds current adapter, conflict, implicit-checkpoint, and export paths.
- Review found bounded-control-flow false-confidence cases. Wrapper reassignment,
  aliases, and differing branch/loop/nested-lambda contexts now produce
  `UNRESOLVED_SAGA_COMMAND_CONFIGURATION` and incomplete analysis instead of a
  confident read-only result. A helper receiving the wrapper remains incomplete under
  the existing unresolved command-typed-call diagnostic.

## Proof

From `verifiers/`, with JDK 21:

```text
mvn -q -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec test
```

The combined run passed visitor, adapter, dummy conflict/recovery, and Quizzes source
checks after one expected-list correction in the expanded fixture. After the final
review hardening, `WorkflowFunctionalityVisitorSpec` passed 37/37. Current reports for
the four suites total 79 tests with zero failures, errors, or skips. `git diff --check`
passes. The continuing reviewer cleared the occurrence/configuration blockers on pass
three; integrated full-suite, package, Docker, and canonical-document qualification
remain with the root task.

## Supported boundary

Precise write extraction intentionally covers exact non-anonymous framework
`SagaCommand` construction, a locally resolved payload, direct resolved setters, and
exact gateway sends. Custom subtypes, anonymous subclasses, aliases,
reassignment, helper configuration, and mismatched control contexts remain explicit
uncertainty; the visitor does not chase those flows or infer possible status values.
No setter name is accepted without both an exact wrapper occurrence and resolved
`SagaCommand` method identity.
