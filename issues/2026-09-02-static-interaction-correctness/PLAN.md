# Plan: trustworthy static Saga interactions

## Environment and execution mode

- **Documentation route:** documented by the adjacent `SPEC.md` because this changes
  the meaning of a thesis metric and the generator's `strict` selection.
- **Execution rigor:** milestone-reviewed. Each semantic slice must agree with the same
  evidence rules before the Quizzes comparison is accepted.
- **Isolation:** continue in the existing verifier-artifact worktree at
  `e4d3cdee5`. Preserve its unrelated untracked `lib/` and `tmp/` directories.
- **Commits:** no branch, push, merge, or PR. Make one normal commit after final review
  only if the user's implementation approval also authorizes that commit.
- **User verdict:** no pause between milestones. The final before/after report is the
  user-facing verdict point.

Implementation approval is still required. This plan changes no source code by itself.

## Implementation strategy

Repair the evidence from its source outward. First derive the root aggregate key from
the application command's constructor delegation into `Command.rootAggregateId`, then
apply that path at Saga command-creation sites. This replaces the fixed third-argument
guess while remaining deliberately bounded to direct constructor flows visible in the
current applications.

Keep the three selection meanings explicit:

1. A direct symbolic interaction candidate requires two resolved root-key paths for the
   same aggregate and at least one write.
2. A concrete input tuple is selected by `strict` only when its corresponding key
   evidence positively agrees through an equal exact value or the same canonical
   source origin.
3. `withTypeOnlyFallback` accepts missing key evidence but still rejects known unequal
   keys. `all` applies no interaction or key-compatibility filter to the bounded
   Cartesian product.

`withTypeOnlyFallback` continues to answer the wider “these accesses may target the
same aggregate because their concrete keys are unknown” question. `all` remains the
bounded Cartesian baseline. Counting and catalog-writing must share these rules so the
accounting does not describe a different space from the generated workloads.

After key and selection behavior are correct, clean up wrapper, compensation, and
limitation analysis in the same visitor area. Recognized wrappers should be transparent
to the underlying application command. Generic compensations may use the base
`Command` contract directly. Ordinary Java and framework calls should disappear from
limitations unless they can conceal a command or aggregate access.

No artifact role or compatibility layer is added. Existing Saga, input, interaction,
and accounting records keep their approved shape and human-facing evidence names. Any
generated ids that depend on corrected evidence may change deterministically.

## Milestones M0–M3

### M0 — Semantic command root keys

#### Outcome and spec coverage

Saga-step accesses derive their root aggregate key from the command definition rather
than creation-site argument position. Covers FR-1–FR-5 and the command root-key path in
the data model.

#### Change boundary

Command-definition analysis, analysis-state command metadata, Saga-step command
application, source-key tracing, and dummyapp verifier fixtures. Quizzes source is
read-only evidence. Input recipes, materialization, scheduling, and artifact shape are
excluded.

#### Known anchors

- `CommandHandlerVisitor`
- `CommandDispatchInfo`
- `WorkflowFunctionalityVisitor`
- `ApplicationAnalysisState.sourceAggregateKeyInputEvidence()`
- `simulator/.../messaging/Command.java` as the read-only root-id contract
- `applications/dummyapp/` as the generic fixture surface

#### Discovery / preflight

Classify the current command constructors in dummyapp and Quizzes by root-id flow:
direct parameter, getter/property on a parameter, explicit `null`, direct base
`Command`, constructor chaining, or unsupported. Confirm visitor ordering and the
smallest analysis-state representation that lets a command definition supply its path
to every creation site. If Quizzes relies materially on a root-id flow outside the
approved direct forms, report it rather than silently expanding into general data-flow
analysis.

#### Implementation strategy

Record a compact semantic path from the application command constructor input to the
root-id argument of `super(...)`. At a creation site, substitute the actual argument
and preserve a direct property/getter suffix when present. Trace the resulting
expression to the Saga constructor input using the existing source path. Leave
explicitly null, ambiguous, or unsupported paths without a key.

#### Proof before continuing

- A command whose root id is not its third public constructor argument resolves the
  correct key.
- A command carrying another aggregate's id does not mistake that id for its root key.
- Direct parameter, DTO getter/property, direct base `Command`, and explicit-null create
  cases have positive fixtures.
- Ambiguous and unsupported constructor paths remain type-only and emit no fabricated
  exact or symbolic value.
- Repeated analysis of the same fixture produces identical paths and ids.

### M1 — Positive strict input selection

#### Outcome and spec coverage

The strict Saga-set and workload counts no longer accept mixed symbolic/type-only
relationships or input tuples that merely lack contradictory values. Covers FR-6–FR-12
and FR-18–FR-21 where they concern selection and counting.

#### Change boundary

Direct-interaction matching, canonical input key evidence, tuple compatibility,
count-only combinatorial accounting, workload generation, and focused scenario tests.
Materializability and schedule/fault generation are excluded.

#### Known anchors

- `ConflictGraphBuilder`
- `InputTupleJoiner`
- `ScenarioSpaceAccountingCalculator`
- `ScenarioGenerator`
- `SourceAggregateKeyInputEvidence`
- `ScenarioModelAdapterResult.aggregateKeyInputEvidence()`

#### Discovery / preflight

Trace the exact path used by catalog-writing and by count-only accounting for the same
Saga set and input tuple. Determine how to carry existing exact logical-key bindings
and same-source provenance into both paths without duplicating two compatibility
algorithms. Confirm how a size-3 connected set identifies the direct edges whose key
relationships its input tuple must satisfy.

#### Implementation strategy

Separate “no contradiction” from “positive match.” Use positive matching for `strict`:
equal exact values or equal canonical source origins satisfy an applicable edge;
missing, unrelated, type-only, and unequal evidence do not. Use contradiction filtering
for `withTypeOnlyFallback`: missing evidence may pass, but known unequal values do not.
Use the full bounded Cartesian product for `all`. For count-only, group equivalent
evidence signatures and count them arithmetically instead of enumerating millions of
concrete tuples.

#### Proof before continuing

- Exact-equal and same-source inputs are strict-positive.
- Exact-different, unrelated symbolic, symbolic/type-only, and missing-key pairs are
  absent from strict selection.
- The uncertain cases remain visible with `withTypeOnlyFallback` when aggregate and
  access-mode requirements hold.
- Read/read pairs remain ignored.
- A size-3 fixture proves that every required connecting edge is satisfied by the
  selected tuple.
- Generator enumeration and accounting arithmetic agree on bounded fixtures under
  `strict`, `withTypeOnlyFallback`, and `all`.
- The `all` input-bound total equals the bounded Cartesian product and is unchanged by
  either strict or fallback interaction pruning.

### M2 — Transparent wrappers and useful limitations

#### Outcome and spec coverage

Typed commands survive `SagaCommand` wrapping without duplicate or false diagnostics,
generic semantic-lock compensation accesses are described when resolvable, and step
limitations identify actual analysis gaps. Covers FR-13–FR-17.

#### Change boundary

Forward and compensation dispatch recognition, wrapper variable tracing, step analysis
limitations, adapter projection, Saga facts, and focused visitor/artifact tests. Runtime
Saga behavior and compensation scheduling are excluded.

#### Known anchors

- `WorkflowFunctionalityVisitor.extractStepFootprints(...)`
- `WorkflowFunctionalityVisitor.extractStepCompensations(...)`
- `SagaStepBuildingBlock`
- `ApplicationAnalysisScenarioModelAdapter`
- Saga-fact projection in `StaticAnalysisArtifactWriter`

#### Discovery / preflight

Classify the method calls currently responsible for the 134 false Quizzes forward
incompleteness results. Separate recognized command construction, wrapper setup,
gateway dispatch, and harmless local operations from calls that can genuinely hide a
dispatch. Confirm the exact bare-`Command` compensation shape used by Quizzes.

#### Implementation strategy

Resolve a `SagaCommand` variable to its payload command and treat the wrapper as
transparent. For a bare `Command` payload, use its service and root-id arguments to
describe the compensation access when both are known. Replace blanket unrecognized-call
failure with targeted limitations for unresolved command types, sends, keys, targets,
or compensation payloads. Deduplicate limitations per step and phase.

#### Proof before continuing

- One wrapped typed forward command produces one application access and no wrapper or
  gateway limitation.
- One supported bare-`Command` compensation produces the expected aggregate and key.
- An unresolved wrapper/payload produces one useful limitation without a guessed
  access.
- Representative getters, setters, collection operations, and recognized framework
  calls do not make analysis incomplete.
- A helper that may conceal a command dispatch remains visibly unresolved.
- Compensation registration and generated recovery behavior remain unchanged.

### M3 — Quizzes size-1–3 comparison and documentation

#### Outcome and spec coverage

Fresh Quizzes evidence shows exactly how the corrected static rules change the thesis
metrics, and the canonical docs state the new current behavior and next priority.
Covers FR-18–FR-21 and final composition of all requirements.

#### Change boundary

Focused and full verifier validation, one bounded count-only Quizzes run, artifact
inspection, current-state/roadmap/audit updates, and final review. Dynamic enrichment,
Docker application execution, preflight, and FaultScenario replay are excluded.

#### Known anchors

- baseline package
  `verifiers/target/quizzes-20260902-011240-501/`
- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- `issues/2026-08-30-current-only-verifier-artifacts/AUDIT.md`

#### Implementation strategy

Run the same size-1–3 count-only configuration used by the retained baseline. Compare
the 0/152/612 exact/symbolic/type-only direct evidence, strict and fallback connected
sets for sizes 2 and 3, strict sets with accepted inputs, and all/selected input-bound
workload totals. Inspect representative removed, retained, and strengthened
interactions so the report explains the numerical delta instead of treating any
direction as success.

Update current-state with only the final behavior and fresh evidence. Mark the four
audit findings resolved or narrowed, and place materializability back at the top of the
roadmap only after the static selection evidence is qualified.

#### Proof before continuing

- Focused visitor, adapter, interaction, accounting, generator, and artifact tests pass.
- `cd verifiers && mvn -q test` passes.
- The fresh package passes current reader, hash, ordering, uniqueness, and reference
  validation.
- Accounting equations reconcile for sizes 1, 2, and 3.
- Every material change in direct evidence or selected counts is supported by at least
  one inspected source example.
- Input discovery, acceptance, rejection, and materializability totals are compared
  with the baseline and any unexpected delta is investigated before completion.
- No new executable or dynamic claim is made from this static run.

## Validation strategy

Validation has four layers:

1. dummyapp-first positive and negative fixtures for semantic root-key tracing;
2. focused unit/specification tests for interaction matching, positive tuple evidence,
   wrappers, compensation, and limitations;
3. the complete verifier Maven suite;
4. the equivalent bounded Quizzes size-1–3 count-only comparison and current-package
   reader validation.

The final report must include the exact benchmark command, output path, baseline and
new metrics, representative classification changes, test totals, and remaining
limitations. It must explicitly report that materializability was measured for
regression only and was not improved in this change.

## Risks and fallbacks

- **Constructor flows exceed the bounded model.** Detect during M0 classification.
  Keep unsupported paths type-only and return to the user before broadening into
  interprocedural data-flow analysis.
- **Existing input provenance is too weak for positive strict selection.** Report how
  many relationships lose strict qualification. Do not restore them through variable
  names or aggregate type alone; the fallback view preserves them for exploration.
- **Count-only becomes expensive.** Compare grouped arithmetic with enumerated small
  fixtures, then use grouped evidence signatures for Quizzes rather than constructing
  every tuple.
- **Counting and generation diverge.** Treat any disagreement on the same bounded
  fixture as a blocker; share one compatibility rule instead of patching totals.
- **Wrapper resolution duplicates typed accesses.** Assert one semantic access per
  command creation/payload identity before accepting M2.
- **Corrected evidence changes generated identities.** Regenerate packages. Do not add
  aliases or historical-reader behavior.
- **Input or materializability totals unexpectedly change.** Trace the dependency. If
  it is not a direct consequence of correcting root-key evidence, revert that part as
  scope drift; if it is necessary, report it for user approval before completion.
