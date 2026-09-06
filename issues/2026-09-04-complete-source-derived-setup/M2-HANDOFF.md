# M2 handoff — exact feature-prefix setup

## Outcome

Implemented the occurrence/context and multi-participant setup slice on baseline
`36784346d1f5e988ad612132b6ed9f5db344ee78`. No commit was created and no shared
canonical documentation was changed.

The visitor now emits a typed `GroovySourceOccurrence` for traced inputs and
setup-capable facade effects. It records source class, exact call context, stable AST
occurrence token, context-local order, and whether the occurrence is still in the
feature's initial straight-line preparation phase. The same direct facade AST call is
used for its full trace, setup action, and producer reference.

The adapter preserves existing `setup()`-only bindings and additionally builds one
candidate per safe exact feature target frontier. Each candidate contains the class
`setup()` actions plus every supported same-feature facade action strictly before that
frontier. It rejects input identities collapsed from multiple exact occurrences and
retains feature owner, frontier, target occurrence, target order, and feature-action
order as analysis-only metadata.

The generator projects a candidate to the selected tuple only when:

- one binding completely covers the tuple;
- its frontier is the earliest selected target occurrence;
- no selected target is replayed as setup;
- no unselected facade effect lies between selected targets; and
- the remaining candidate is semantically unambiguous.

Existing complete `setup()`-only bindings retain priority, preserving prior behavior.

## Conservative boundaries

Feature-prefix extension permanently closes after an assertion/cleanup/where label,
control flow (`if`, loops, switch, try/catch), direct workflow execution, or direct
event-handler execution. Calls at or after the barrier can remain input evidence but
cannot extend setup.

Assigned direct facade calls already use the exact RHS `MethodCallExpression` token for
both the action and full trace; the regression proves this token is also the later
argument's producer reference and is distinct from the selected target token.
Helper-returned facade actions are caller-rebased by the existing helper tracing path.
A direct Saga constructor intentionally has its own constructor-expression occurrence,
not a facade SetupAction occurrence; executing that constructor's workflow closes later
prefix extension.

Persisted input/setup/workload/accounting schemas are unchanged. Nested result-property
support and runtime preflight changes are outside this worktree slice.

## Focused Quizzes evidence

`AddParticipantAndCreateTournamentTest#create add participant successfully` now has an
exact feature-derived candidate. Its final three feature-local actions are:

1. `createUser`
2. `activateUser`
3. `addStudent`

The selected `addParticipant` occurrence is absent from SetupActions. Constructor
argument 3 binds to the preceding `createUser` result's `aggregateId`. The candidate
contains only ordinary `setup()` actions or actions owned by that exact feature; a
same-looking `createUser` action from another test class is excluded.

A temporary read-only measurement before removing instrumentation observed 73 setup
bindings covering 642 distinct adapted input ids in this M2-only worktree. The prior
setup-only union was 559 ids (560 static single-input candidates including one input
needing no setup), so this indicates 83 additional setup-covered ids before the
coordinator's M1 merge. This is not the final accepted-input categorization; the
coordinator owns combined before/after generation and qualification.

## Regression proof

The bounded set comprises 81 tests. The combined command passed 80 and exposed one
over-specific cross-context fixture assertion:

```text
mvn -q -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec,GroovyConstructorInputTraceVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,InputTupleSelectionSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec test
```

The assertion was corrected to select a same-looking action from another source class;
the corrected `SourceDerivedSharedSagaWorkloadAnalysisSpec` was then rerun separately
and passed. The other four specs had already passed in the combined run. Covered
positives/negatives include:

- exact assigned-producer/action/target occurrence identity;
- strict target cutoff and post-assertion exclusion;
- permanent control-flow, workflow, and event-handler barriers;
- adapter rejection of two identical target expressions collapsed to one input id;
- exact singleton frontier selection;
- selected-target replay rejection;
- collapsed target metadata rejection;
- omitted inter-target effect rejection and coherent two-participant acceptance;
- real Quizzes feature-prefix binding and cross-class context isolation; and
- preservation of the existing RemoveTournament/AddParticipant setup and natural triple.

`git diff --check` passes.

## Files changed

- `GroovySourceOccurrence.java` (new)
- `GroovyFullTraceResult.java`
- `GroovyFacadeSetupActionTrace.java`
- `GroovyConstructorInputTraceVisitor.java`
- `ApplicationAnalysisScenarioModelAdapter.java`
- `SourceSetupPlanBinding.java`
- `ScenarioGenerator.java`
- `GroovySagaTracingSpec.groovy`
- `GroovyConstructorInputTraceVisitorDummyappSpec.groovy`
- `InputTupleSelectionSpec.groovy`
- `SourceDerivedSharedSagaWorkloadAnalysisSpec.groovy`
- issue `SPEC.md` and `PLAN.md`

## Remaining coordinator work

- Integrate with the independently implemented bounded nested-property and preflight
  slices, resolving record/constructor overlap mechanically.
- Run the combined full suite, final 796-input categorization, deterministic package
  comparison, and bounded new-prefix Docker sample.
- Perform final independent review and update canonical current-state/roadmap/ADR docs
  with combined measured evidence.

Explicit limitation: persisted InputVariant identity still does not distinguish repeated
occurrences. When identical traces collapse, feature-derived setup blocks rather than
choosing or merging their prefixes. Feature-prefix extraction remains deliberately
straight-line and does not interpret control flow or lifecycle semantics.
