# Plan: complete source-derived setup

## Environment and execution mode

This is a documented-route verifier change based on thesis baseline commit
`36784346d1f5e988ad612132b6ed9f5db344ee78`. Planning was prepared in the isolated
worktree `/Users/andre/.codex/worktrees/7159/microservices-simulator` on branch
`codex/source-setup-plan`; only this issue package is changed during planning.

The user's instruction to complete this work autonomously waives another implementation
approval checkpoint. Luna's bounded preflight repair and Sol's occurrence/prefix slice
use separate worktrees at that baseline. The coordinator implements M1 in the initially
clean primary checkout, reviews the isolated changes, and integrates reviewed patches
there for combined proof. No commits, pushes, merges, or deployments are part of this
run. The coordinator owns bounded Docker qualification and canonical documentation.
Review occurs after each behavior slice.

The coordinator owns implementation sequencing. The exact `quiz.aggregateId` property
slice can proceed independently after M0 and be integrated before the context/cutoff
slice. The occurrence/prefix slice should have one writer and an independent reviewer
because source-order mistakes can silently turn measured target work into setup.

## Implementation strategy

Keep three concerns separate:

```text
visitor: exact occurrence and feature-local order
  -> adapter: setup() + safe feature prefix at a target cutoff
  -> generator: common-context, full-coverage, selected-target-free attachment
```

Do not infer a target cutoff from `sourceExpressionText`, line-string parsing, or a first
matching facade call. Add a typed internal occurrence relation at the visitor/state
boundary and retain current persisted InputVariant identity. If several source
occurrences collapse to one input id and imply different prefixes, diagnose and block
that feature-derived candidate.

For one feature owner, construct candidates at exact target frontiers. Candidate actions
are the current supported `setup()` sequence followed by supported same-feature facade
actions strictly before the frontier. Project bindings against that candidate, and retain
an input only when all remaining arguments pass the existing readiness evaluator. At
attachment time require a common feature owner, complete tuple coverage, and absence of
every selected target occurrence from the actions. Semantically identical candidates
may collapse deterministically; non-identical matches remain ambiguous.

Only the feature's initial straight-line preparation phase is eligible. Assertion labels,
cleanup/where labels, control flow, and direct workflow execution permanently close
prefix extension for subsequent calls; direct event-handler execution does the same. A
singleton uses its own exact target cutoff. A
tuple uses its earliest selected target occurrence as the frontier and is rejected when
an unselected facade effect lies between selected targets.

Use the existing `ACTION_RESULT_PROPERTY` record for the bounded nested path. Reuse the
static validator's existing DTO-root rule and admit only `quiz.aggregateId`. Extend the
runner's pre-dispatch runtime contract validation to require declared public
zero-argument getters whose types are DTO then Integer-compatible, then invoke only that
accepted chain. Do not name a Quizzes/fixture FQN, introduce a generic dotted-path
resolver, or broaden the application setup dispatcher.

Preserve all currently supported calls inside the admitted prefix, including void calls.
This keeps feature-local sequences such as `createUser -> activateUser -> addStudent ->
addParticipant` faithful. Calls after the exact target—including assertion reads—are
excluded by construction. Calls from another feature are excluded by owner, even when
the class and expression text match.

## Milestone M0 — Pin the baseline and executable contracts

### Outcome and spec coverage

Record the current 560/796 single-input static result and pin the existing `setup()`-only,
exact-result, whole-prefix, and ambiguity behavior before changing production logic.
Establish the concrete dummyapp and Quizzes cases used by later milestones. Covers the
baseline and evidence parts of FR-17 through FR-20.

### Change boundary

Read-only classification plus focused test fixtures. Test-only dummyapp additions may be
prepared, but production behavior and package schemas do not change.

### Known anchors

- `docs/verifiers-impl/current-state.md`
- `issues/2026-09-03-workload-driven-source-setup/`
- `GroovyConstructorInputTraceVisitorDummyappSpec`
- `SourceDerivedSharedSagaWorkloadAnalysisSpec`
- `applications/quizzes/.../StartQuizTest.groovy`
- `applications/quizzes/.../AddParticipantAndCreateTournamentTest.groovy`

### Discovery / preflight

Reproduce or inspect the current single-input categorization and save representative
records for: feature-local producer with missing binding, partial binding with a required
void effect, an exact nested `quiz.aggregateId` path, repeated identical target calls,
and one input with genuinely no usable producer. Determine whether any chosen input id
maps to several target occurrences before using it as positive evidence.

### Proof before continuing

- The starting result is recorded as 560 static candidates and 236 blocked out of 796,
  with the 3/175/58 blocker split.
- Each positive fixture names the exact producer occurrence, target occurrence, feature
  owner, expected retained prefix, and expected participant binding.
- Repeated/collapsed occurrences are explicitly classified rather than silently selected.

## Milestone M1 — Add the bounded nested result path

### Outcome and spec coverage

The existing result-property recipe can materialize the exact approved
`quiz.aggregateId` path for any structurally compatible DTO result while rejecting every
unapproved root, signature, or path. Covers FR-14 through FR-18.

### Change boundary

May change setup value mapping, SetupPlan validation, source setup execution, and focused
dummyapp/verifier tests. Preserve the persisted record structure, current one-property
behavior, closed dispatcher, and all unrelated materialization recipes.

### Known anchors

- `SetupPlanMapper.referenceValue`
- `SetupPlanValidator`
- `ScenarioSetupRunner`
- `SetupValueRecipe`
- `ExecutableArtifactWriter` and `ScenarioCatalogPackageReader`

### Implementation strategy

Serialize the approved path as the current property string `quiz.aggregateId`. The
static validator requires the declared action-result type to satisfy the existing
DTO-root contract. The runner's runtime contract check requires its declared public
zero-argument `getQuiz()` to return a DTO type and that type's declared public
zero-argument `getAggregateId()` to return an Integer-compatible type before dispatching
any setup action. At execution, invoke only those exact getters and check their returned
values/types. Preserve `aggregateId` and `courseAggregateId` unchanged.

### Proof before continuing

- Dummyapp positive: the exact approved two-segment path maps, validates, round-trips,
  participates in deterministic identity, and materializes.
- A structurally compatible DTO from a non-Quizzes dummyapp package passes without an
  application/fixture FQN in generic verifier code.
- Negative matrix: non-DTO root, missing/non-public getter, wrong intermediate/leaf type,
  unknown first/final segment, blank segment, third segment, wrong final expected type,
  and an arbitrary getter-like token are rejected before dispatch.
- Existing one-property tests remain green.
- A scoped code review confirms no arbitrary reflective fallback or dispatcher expansion.

## Milestone M2 — Build and attach exact, context-safe feature prefixes

### Outcome and spec coverage

The visitor and adapter construct setup candidates from ordinary `setup()` plus supported
same-feature actions strictly before exact targets, and the generator attaches one only
when a common feature context completely covers the tuple without replaying a selected
target. Covers FR-1 through FR-13 and FR-17.

### Change boundary

May change internal Groovy trace/state facts, visitor emission, adapter candidate
construction, `SourceSetupPlanBinding` metadata, generator/accounting matching,
diagnostics, and dummyapp/verifier tests. Persisted input, setup, workload, and accounting
shapes remain unchanged. Do not change interaction selection, participant limits,
scheduling, or add general control-flow evaluation.

### Known anchors

- `GroovyConstructorInputTraceVisitor.traceClass`
- `traceFacadeCall`, `registerNestedHelperFacadeTrace`, and `registerSetupActionTrace`
- `GroovyFullTraceResult` and `GroovyFacadeSetupActionTrace`
- `ApplicationAnalysisScenarioModelAdapter.adaptSetupBindings`
- `SetupPlanMapper`
- `SourceSetupPlanBinding`
- `ScenarioGenerator.setupPlanFor`
- `ScenarioSpaceAccountingCalculator`
- `InputVariant.owners`

### Discovery / preflight

Trace the exact emission order for direct facade calls, helper-return producers, nested
void helper calls, and direct Saga constructors. The implementation must answer how one
typed target occurrence is joined to its action-capable occurrence, how nested helper
actions retain caller-relative order without parsing the existing occurrence string, and
how exact feature owners constrain tuple attachment.

### Implementation strategy

1. Emit typed occurrence/context/order data for every relevant target and setup-capable
   call while preserving the current deterministic source-occurrence token.
2. Build one feature frontier per exact target occurrence.
3. Concatenate current supported `setup()` actions with the same-feature strict prefix.
4. Map bindings only to retained results, keep supported void effects, and exclude the
   target plus all later calls.
5. Carry exact feature-owner and excluded-target metadata with analysis-time candidates.
6. For a tuple, intersect exact owners, require complete projected binding coverage in
   one owner context, require the earliest selected occurrence as its frontier, and
   reject candidates containing any selected occurrence or omitting an inter-target
   facade effect.
7. Collapse only semantically identical candidates. If one persisted input id maps to
   distinct occurrences with different candidate semantics, or non-identical candidates
   cover one tuple, emit a stable ambiguity blocker.

### Proof before continuing

- Dummyapp positive: feature-local `create -> void activate/enrol -> target` retains all
  three prerequisite actions in order and binds the target argument.
- The exact target is absent from SetupActions; a facade read/assertion after it is absent.
- A same-looking producer in another feature cannot satisfy the target.
- Nested helper return and void calls retain their exact occurrence identity and order.
- Repeated identical targets with different prefixes block unless their candidate
  semantics are provably identical.
- Existing `setup()`-only RemoveTournament/AddParticipant action and binding regressions
  remain unchanged.
- Same-feature pair/triple positive: one prefix completely covers all selected inputs and
  no selected target is replayed.
- Cross-feature and cross-class tuples cannot combine feature-local actions.
- A later participant that depends on an earlier selected participant's result remains
  blocked before execution.
- A larger tuple cannot inherit a setup that covers only a subset.
- Count-only and written-workload matching agree, and deterministic ordering/identity
  tests pass.

## Milestone M3 — Qualify the bounded Quizzes gain

### Outcome and spec coverage

A fresh Quizzes analysis reports the real static gain, unresolved categories, concrete
new setup-bearing workloads, and bounded runtime readiness without overclaiming the
remaining population. Covers FR-19 and FR-20.

### Change boundary

May update focused Quizzes verifier fixtures/evidence and affected canonical verifier
documentation. Do not change Quizzes production code, test meaning, event-consumer
setup, preflight aggregation, package schemas, generation policy, or catalog caps merely
to improve the headline count.

### Discovery / preflight

Reclassify all 796 accepted single inputs after M1-M2. Select positives only where exact
occurrence/context evidence is unique. Start with:

- `AddParticipantAndCreateTournamentTest#create add participant successfully`, whose
  feature-local prefix creates and activates a user and enrolls it before AddParticipant;
- `RemoveStudentFromCourseExecutionTest#remove non-enrolled student throws exception`,
  whose feature-local user producer precedes the target;
- nested `TournamentDto.quiz.aggregateId` cases found in the three rejected plans.

Classify `StartQuizTest#a different student can start the same quiz independently`
carefully: its prefix creates/enrols a second student and performs an earlier StartQuiz,
but repeated same-feature StartQuiz calls may share one current input identity. It is a
positive only if the exact target remains unambiguous; otherwise it is the required safe
blocker example.

### Implementation strategy

Run the complete static single-input analysis first. Generate a small stable-order
package containing representative newly covered inputs, validate it through the current
reader, then preflight only a bounded sample in the existing isolated worker model. Do
not infer runtime readiness from static candidate counts.

### Proof before continuing

- Report before/after counts for: accepted inputs; source setup; no setup needed; rejected
  setup plan; partial bindings; no binding; and occurrence/context ambiguity.
- Identify exact newly covered Quizzes examples and the retained action/binding sequence.
- At least one feature-prefix example and the nested-path example pass current-package
  write/read validation; representative preflight either reaches `SETUP_READY` or reports
  a precise blocker.
- Existing RemoveTournament/AddParticipant pair and natural triple remain valid.
- Accepted-input and interaction totals change only if exact occurrence preservation
  exposes a previously collapsed trace; any such delta is reconciled, not treated as a
  success metric.

## Milestone M4 — Review and document shipped behavior

### Outcome and spec coverage

The implementation has independent correctness review and canonical documentation states
the actual supported boundary and measured result. Covers FR-17 through FR-20.

### Change boundary

Update only affected verifier canonical docs, the ordered-setup ADR, and issue handoff
evidence. No new implementation scope.

### Known anchors

- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- `docs/verifiers-impl/decisions/2026-08-28-source-derived-ordered-setup.md`

### Proof before continuing

- Independent review checks exact cutoff, feature isolation, selected-target exclusion,
  repeated-occurrence blocking, multi-participant readiness, closed property access, and
  absence of package-shape drift.
- Documentation distinguishes static candidates from preflighted workloads and reports
  remaining blockers honestly.
- `git diff --check` passes and no unrelated source or generated evidence is committed.

## Validation strategy

Run narrow proof first, then the complete verifier suite. Stable commands are:

```bash
cd simulator
mvn -q -DskipTests install

cd ../verifiers
mvn -q -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec test
mvn -q -Dtest=GroovyConstructorInputTraceVisitorSpec test
mvn -q -Dtest=ApplicationAnalysisScenarioModelAdapterSpec test
mvn -q -Dtest=InputTupleSelectionSpec test
mvn -q -Dtest=ScenarioExecutorSpec test
mvn -q -Dtest=SourceDerivedSharedSagaWorkloadAnalysisSpec test
mvn -q test
```

Where a focused Spock selector is unreliable under Surefire, run the containing spec
class. Quizzes package generation should use the existing bounded stable-order test hook;
run isolated preflight only for the selected representative package. Heavy/full Docker
qualification is not required for this issue package and should be scheduled separately
if the coordinator wants environment-parity evidence.

Final evidence must include:

1. dummyapp positive/negative matrices for occurrence, cutoff, context, participant, and
   path rules;
2. current writer/reader/validator determinism and shape checks;
3. the complete Quizzes static before/after classification;
4. a bounded Quizzes preflight result; and
5. an independent diff review.

## Risks and fallbacks

| Risk | Detection | Mitigation or fallback |
|---|---|---|
| Target call is replayed as setup and again as measured work | Selected-occurrence intersection assertion on every candidate/workload | Reject the candidate; never remove the measured participant to make setup pass. |
| Calls from another feature leak through class-wide state | Same-class/different-feature negative fixture | Key candidates by exact feature owner and keep feature scope copies isolated. |
| Repeated identical calls are assigned the wrong cutoff | One input id maps to multiple occurrences/prefixes | Block with stable ambiguity diagnostics; defer persisted occurrence identity. |
| Whole-prefix replay includes assertion reads | Target-followed-by-facade-read negative fixture | Strict target cutoff; no post-target action is admitted. |
| A prior selected participant is required as a setup producer | Multi-participant exclusion test | Keep the tuple blocked because participants are materialized before measured execution. |
| Void effects are lost by result-only slicing | create/activate/enrol/target sequence test | Retain every currently supported action in the admitted prefix. |
| Dotted property handling becomes arbitrary reflection | Negative path matrix and code review | Exact root/path/type allowlist; exact getters only; no fallback. |
| Candidate multiplication makes accounting expensive | Candidate count and size-1-3 timing comparison | Build frontiers once per context, canonicalize identical semantics, and stop for redesign if growth is material. |
| Static counts rise but runtime setup fails | Bounded current-package preflight | Report candidate and setup-ready counts separately; keep failed families blocked or document precise runtime gaps. |
| Accepted-input identity changes unexpectedly | 796 baseline reconciliation | Do not change persisted identity in this issue; stop as a scope delta if correctness requires it. |
