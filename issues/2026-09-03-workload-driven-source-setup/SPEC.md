# Workload-driven source setup

## What workload-driven source setup is

The verifier already extracts one executable source-derived setup for the
RemoveTournament/AddParticipant pair. The extraction itself can hold many setup actions
and participant bindings, but the internal attachment is keyed to exactly two input ids.
As a result, source setup is discovered and attached as a special pair instead of as a
property of the complete selected WorkloadPlan.

This change makes one observed test setup reusable by any selected workload whose inputs
that setup can completely supply. A workload may contain one, two, three, or more Saga
inputs. The implementation must not contain an arity limit; the configured generator cap
still decides which workload sizes are actually enumerated.

Input relationship selection remains unchanged. `strict` still requires positive exact
or same-source evidence. This feature reconstructs selected inputs; it does not invent a
shared Tournament or other entity across unrelated tests.

## Goals

1. Attach source-derived setup using the complete selected input set rather than a
   left/right pair.
2. Reuse each observed setup action once and bind its results to every covered
   participant that needs them.
3. Let the same mechanism work for any workload size accepted by the generator.
4. Prevent a setup for two inputs from being attached accidentally to a larger workload
   it cannot completely prepare.
5. Measure the additional Quizzes workloads that receive source setup and pass preflight.

## Non-goals

- Changing `strict`, `withTypeOnlyFallback`, or `all` input selection.
- Combining unrelated inputs from different test executions by synthesizing shared
  entities.
- Adding new input recipe kinds or fixing other materializability blockers.
- Supporting new Groovy control-flow shapes or building a general test interpreter.
- Changing scheduling, fault vectors, recovery, execution, ImpactV1, or dynamic evidence.
- Redesigning the current package or accounting schemas.

## Functional requirements

### Workload applicability

**FR-1.** Source setup applicability shall use the complete set of selected InputVariant
ids and shall have no hard-coded participant-count limit.

**FR-2.** A WorkloadPlan shall receive a source-derived setup only when one coherent
observed test setup can supply every selected participant argument that depends on that
setup. Runtime-owned or independently materializable arguments need no setup binding.

**FR-3.** A pair setup shall not be attached to a larger workload merely because the
larger workload contains those two inputs.

**FR-4.** Inputs from different setup contexts shall not be presented as one observed
setup.

### Setup construction and reuse

**FR-5.** Each source occurrence shall appear at most once in a SetupPlan, even when
multiple participants consume its result.

**FR-6.** Setup actions shall preserve their observed order. Existing supported void
effects between value-producing calls shall remain present.

**FR-7.** Participant bindings shall name the exact InputVariant and constructor argument
they supply. Multiple bindings may reference one retained setup result and property.

**FR-8.** A reusable setup may contain bindings for multiple eligible inputs from its
source context. A WorkloadPlan shall use only a setup that covers its exact participants.

**FR-9.** Unsupported or unresolved setup values shall remain blocked with the existing
source-facing diagnostics.

### Determinism and evidence

**FR-10.** Setup identity, action order, binding order, workload identity, and package
bytes shall remain deterministic.

**FR-11.** The persisted `setups.jsonl` and WorkloadPlan `setup` reference shall retain
their current shapes unless implementation proves that they cannot represent this
behavior.

**FR-12.** Evaluation shall report how many written WorkloadPlans reference source-derived
setup and which representative workloads pass setup preflight.

**FR-13.** Accepted-input totals, strict interaction totals, and count-only workload-space
totals shall not change as a consequence of setup attachment.

## Architecture

The existing pipeline boundary remains:

```text
visitor facts
  -> ApplicationAnalysisState
  -> scenario adapter: coherent source-setup candidates
  -> scenario generator: exact workload applicability
  -> current setup/workload artifacts
  -> existing reader, validator, preflight, and executor
```

The adapter should describe which input ids one observed setup can supply. The generator
then decides whether that setup completely covers a selected workload. Existing SetupPlan
validation and execution remain the authorities for persisted and runtime correctness.

No application-specific Saga names or Quizzes-only dispatch behavior may enter verifier
production code.

## Data model

`SourceSetupPlanBinding` is currently an internal pair of `leftInputVariantId`,
`rightInputVariantId`, and `SetupPlan`. It will become an internal arity-independent
association between a coherent source setup and the InputVariant ids it can supply.

The current persisted SetupPlan already contains an ordered action list and a list of
participant bindings. The current WorkloadPlan already references one setup by id.
Those artifact shapes are expected to be sufficient.

Runtime results remain attempt-local and are never persisted or included in deterministic
identity.

## Security model

Not affected. Setup execution continues to use the existing closed map of Java-confirmed
facade methods.

## Operating

No new command-line option is introduced. `maxSagaSetSize` continues to control the
largest enumerated workload; source setup itself accepts any emitted size.

The representative Quizzes evaluation continues to use sizes one through three. This is
an evaluation bound, not a setup limit.

Rollback is a normal revert of this isolated change. Existing pair behavior remains the
minimum regression case.

## Future roadmap

A later feature may synthesize one shared producer for compatible inputs taken from
different tests. Such workloads would be generated relationships, not relationships
observed in source, and would need separate evidence and evaluation terminology.

## Open decisions

| # | Decision | Default | Alternatives | Impact |
|---|---|---|---|---|
| 1 | How much of an observed `setup()` should execute? | Reuse the complete currently supported source-ordered setup for that context | Slice only the data-dependency prefix | Complete reuse preserves void semantic prerequisites and is simpler; slicing risks dropping required effects. |
| 2 | What Quizzes workload sizes qualify this change? | Sizes 1–3 | Add 4–5 | Sizes 4–5 add large enumeration cost without testing a different setup mechanism. |
| 3 | Should accounting gain new fields? | No; derive evaluation counts from setup and workload artifacts | Add aggregate setup metrics | Avoids reopening the recently simplified accounting contract for one implementation measurement. |
