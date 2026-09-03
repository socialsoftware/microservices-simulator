# Plan: workload-driven source setup

## Environment and execution mode

This is the documented follow-on to
`issues/2026-08-27-source-derived-shared-saga-workloads/`. It generalizes that working
vertical slice; it does not reopen its execution semantics.

The checkout currently contains the separately approved relative-date implementation.
That work should be completed and committed independently before setup implementation
begins. The known standalone `DummyappAccountingFixtureFoundationSpec` failure must also
be diagnosed separately so this feature is not credited or blamed for an existing red
baseline.

Implementation should run in an isolated Codex worktree from the resulting
`fault-analysis/scenarios` commit. Local milestone commits are allowed after validation.
There will be no PR, push, merge, rebase, or deployment. Integration back into the user's
checkout will be discussed after final review.

Use substantial execution rigor: one implementation agent and one consistent independent
reviewer. Milestones run consecutively without user pauses because the output shape and
boundary are already agreed. The only user verdict is after complete proof.

## Implementation strategy

Keep relationship selection and setup reconstruction separate:

```text
existing selection chooses an input tuple
  -> arity-independent setup matching asks whether one observed setup covers it fully
  -> the existing generator attaches that setup
  -> existing writer, reader, validator, preflight, and executor consume it
```

Replace pair-keyed setup applicability with an internal setup binding that lists the
InputVariants it can supply. For a selected tuple, require complete coverage before
attaching it. Preserve the whole currently supported `setup()` action sequence so
activation and enrollment effects are not lost.

The current persisted setup and workload shapes should not change. If implementation
shows that an artifact change is unavoidable, stop as a scope delta rather than adding a
new field opportunistically.

## Milestone M0 — Establish the honest baseline

### Outcome and spec coverage

Record how many setup bindings and setup-bearing workloads the current pair-shaped
mechanism produces, and whether a larger workload can inherit an incomplete pair setup.

### Change boundary

Read-only measurement and focused regression fixtures. No production behavior or artifact
shape changes.

### Known anchors

- `ApplicationAnalysisScenarioModelAdapter.adaptSetupBindings`
- `SourceSetupPlanBinding`
- `ScenarioGenerator.setupPlanFor`
- `SourceDerivedSharedSagaWorkloadAnalysisSpec`

### Discovery / preflight

Measure the current setup bindings, written workloads that reference them, and any larger
workload whose setup covers only a subset of its participants. Retain the latest size-1–3
accounting as the selection baseline.

### Proof before continuing

Save a compact before table and one example of the current pair behavior.

## Milestone M1 — Make setup applicability arity-independent

### Outcome and spec coverage

Any emitted workload can receive one observed source setup when that setup completely
covers its selected inputs. Pair-only and partial-containment behavior is removed. Covers
FR-1 through FR-9.

### Change boundary

May change internal source-setup candidate/binding models, scenario-adapter setup mapping,
generator attachment, and focused dummyapp/verifier tests. Preserve interaction selection,
input recipes, persisted artifacts, scheduling, faults, and execution.

### Known anchors

- `ApplicationAnalysisScenarioModelAdapter`
- `SetupPlanMapper`
- `SourceSetupPlanBinding`
- `ScenarioGenerator.setupPlanFor`
- `SetupPlanValidator`

### Implementation strategy

1. Represent one coherent source setup with the complete stable set of InputVariant ids
   it can bind.
2. Map each source occurrence once and retain the existing ordered actions, including
   supported void effects.
3. Attach a candidate only when every setup-dependent selected input has complete,
   unambiguous bindings.
4. If current source facts produce multiple plausible setups for one workload, keep the
   workload blocked and report the ambiguity rather than designing a new resolver.
5. Keep artifact serialization unchanged.

### Proof before continuing

- The existing pair still receives its exact setup and shared actions remain single.
- A three-participant tuple cannot inherit an incomplete two-input setup.
- One naturally available non-pair case is covered if M0 finds one; do not manufacture an
  arity matrix solely for this proof.
- Existing deterministic identity tests remain green.

## Milestone M2 — Qualify bounded Quizzes workloads

### Outcome and spec coverage

A fresh size-1–3 Quizzes package shows the actual increase in setup-bearing workloads and
a bounded representative sample passes setup preflight. Covers FR-10 through FR-13.

### Change boundary

May adjust generic tests and Quizzes evaluation fixtures needed to exercise existing
ordinary tests. Do not change Quizzes production behavior, test meaning, package schemas,
or generation caps to reserve favored workloads.

### Discovery / preflight

Choose explicit caps after the M0 counts are known. The run must remain feasible on the
MacBook and must report if stable catalog order omits an eligible workload.

### Implementation strategy

1. Generate the same count-only size-1–3 package and confirm selection/accounting totals
   are unchanged.
2. Generate a bounded workload-writing package large enough to include the existing pair
   and any natural additional case found in M0.
3. Report the setup-bearing workload count.
4. Preflight the existing pair and a small number of natural new candidates, isolating source setup candidates as the
   existing executor requires.
5. Compare setup-ready results with the M0 baseline without extrapolating to all Quizzes
   combinations.

### Proof before continuing

- Count-only accepted inputs, interaction totals, connected sets, and input-bound totals
  match the pre-change package.
- Every setup-bearing workload has complete bindings for its exact selected participants.
- The existing pair and any reported new representative either pass preflight or report a
  precise setup blocker.
- Existing deterministic package tests pass; no second complete Quizzes run is required.

## Milestone M3 — Review and report the gain

### Outcome and spec coverage

The implementation has one independent correctness review and the handbook reports what
became possible in thesis language.

### Change boundary

Update only affected verifier canonical documentation and issue handoff evidence. Do not
add product telemetry or an accounting redesign.

### Implementation strategy

Report three separate outcomes:

1. setup attachment coverage;
2. successful runtime preflight coverage;
3. unchanged interaction/input-space metrics.

### Proof before continuing

- Focused adapter, generator, writer/reader, validator, and preflight tests pass.
- `mvn -q -DskipTests compile`, `mvn -q test-compile`, and `git diff --check` pass.
- The full verifier suite is compared with the independently recorded starting baseline;
  the known Dummyapp failure is resolved separately or remains exactly unchanged and is
  reported honestly.
- The reviewer checks the pair assumption is gone, larger workloads cannot receive partial
  setup, and artifact shapes did not drift.

## Validation strategy

Validation has three layers:

1. focused model/generator and current package round-trip tests;
2. one bounded Quizzes size-1–3 generation plus representative preflight;
3. full verifier test comparison and independent diff review.

The final before/after table will contain:

- accepted and materializable inputs;
- strict connected and selected input-bound counts;
- written and setup-bearing workloads;
- representative setup-ready/blocked outcomes.

Only the setup-bearing and setup-ready columns are expected to improve. The interaction
and count-only columns are expected to remain stable.

## Risks and fallbacks

| Risk | Detection | Mitigation or fallback |
|---|---|---|
| Pair setup is still attached by subset containment | Three-plus-participant negative fixture | Require complete selected-input coverage before attachment. |
| Setup matching becomes combinatorial | Size-1–3 generation time | Represent setup coverage once; do not precompute every input subset. |
| Dependency slicing drops activation or enrollment calls | Remove/Add action-list regression and preflight | Preserve the complete currently supported setup sequence. |
| Persisted schema appears insufficient | Writer/reader contract review | Stop for user decision; do not redesign artifacts inside this feature. |
| Existing red test obscures regression status | Clean-HEAD reproduction | Resolve it as separate baseline work or compare exact unchanged failure evidence. |
