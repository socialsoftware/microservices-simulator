# M1 handoff — positive strict input selection

- **State:** complete; milestone review passed on pass 2.
- **Outcome:** direct symbolic candidates now require resolved key paths on both
  accesses, and input-bound selection distinguishes positive equality from mere lack
  of contradiction. `strict` requires an exact-equal or canonical same-source path
  through a connected tuple graph; `withTypeOnlyFallback` admits missing evidence but
  rejects known unequal keys; `all` is the full bounded Cartesian input product.
  Catalog enumeration and count-only accounting use the same selection component.
  Covers FR-6–FR-12 and the M1 portions of FR-18–FR-21.

## Discovery and preflight

- Before M1, `ScenarioGenerator` received only `InputVariant` records even though
  canonical same-source evidence was already available separately on
  `ScenarioModelAdapterResult.aggregateKeyInputEvidence()`. Count-only accounting had
  the same omission. The feasible shared path was to carry that existing evidence
  into both consumers and centralize the rule; no package or persisted model change
  was necessary.
- The retained Quizzes baseline has 151 of 152 symbolic interaction facts with one
  access lacking usable key evidence, while `inputs.jsonl` has 652 same-source
  evidence records. This makes type-level candidate visibility and positive
  input-bound coverage intentionally different quantities.
- The previous graph admitted a symbolic candidate when only one side was symbolic,
  the tuple joiner treated absence of a contradiction as compatibility, and
  accounting implemented a separate binding-group rule. The previous `all` path was
  therefore not an unconditional Cartesian baseline.
- For size 3 and above, the applicable tuple rule is connectivity of the subgraph
  whose direct candidates are satisfied by that tuple. Every edge used to connect the
  selected Saga set is positive in strict mode; unrelated or unequal edges cannot
  connect it.
- No material scope delta, data-model expansion, API break, or cost expansion was
  found. Existing overloads remain available; new overloads only carry the existing
  canonical evidence through the internal generation/accounting boundary.
- Review preflight found that preserving candidate-specific same-source identity was
  feasible inside the existing internal model. It adds no persisted/package fields:
  an adapted input id plus constructor-argument/key-path identity travels only through
  the in-memory evidence and footprint records.

## Actual changes

- Added `InputTupleSelection` as the shared selection and counting owner. It:
  - classifies each relevant candidate as positive, missing, or unequal;
  - recognizes equal static exact values, equal relevant logical-key bindings, and
    equal canonical `GroovySourceValueReference` origins;
  - requires a connected positive candidate graph in strict mode;
  - permits missing but not unequal evidence in fallback mode;
  - counts equivalent evidence profiles with multiplicities rather than enumerating
    concrete tuples, and uses direct Cartesian multiplication for `all` and for the
    no-exact-binding fallback fast path.
- Tightened `ConflictGraphBuilder`: exact/exact candidates exist only when values are
  equal; symbolic candidates require a nonblank resolved key on each access;
  unresolved relationships exist only in the opt-in broad graph. Aggregate identity,
  cross-Saga, and at-least-one-write requirements remain unchanged.
- Changed `InputTupleJoiner` to enumerate the full Cartesian product by default and
  delegate strict/fallback acceptance to `InputTupleSelection`.
- Carried aggregate-key input evidence through `ScenarioGeneratorApplication`,
  `ScenarioGenerator`, `ScenarioSpaceAccountingCalculator`, and
  `StaticAnalysisArtifactWriter`.
- Made generated WorkloadPlan conflict evidence tuple-compatible. The schedule
  enumerator still receives the original type-level candidate set, preserving the
  M1 exclusion on schedule-generation behavior.
- Made count-only `strict.withAcceptedInputsBySize` and fallback input coverage use
  the shared positive/fallback rule. Direct evidence names and artifact roles/shapes
  remain unchanged.
- Updated focused generator, accounting, dummyapp fixture, and static-artifact tests
  for Cartesian `all` and positive selection semantics. Added a dedicated M1 spec for
  exact equality, two-sided symbolic candidates, canonical source equality,
  fallback, read/read exclusion, size-3 connectivity, catalog/accounting agreement,
  and grouped arithmetic.

Review correction pass:

- Replaced coarse source-field evidence grouping with exact adapted-input identity.
  The adapter now maps each raw trace/evidence record to its deterministic
  `InputVariant` id using the actual producer occurrence, then preserves the dispatch
  constructor argument and aggregate-key property path.
- Added candidate-specific evidence identity `(aggregate, constructor argument,
  key path)`. Same-source evidence can now satisfy only its intended input occurrence
  and intended aggregate-key extraction path, even when one test method performs
  multiple direct calls or one aggregate has multiple constructor/key paths.
- Added one shared conservative exact-value normalizer used by graph construction and
  tuple selection. Numeric `+42`, `42`, and an extracted numeric binding `42` compare
  equal; genuinely different numbers remain unequal, and quoted/nonnumeric identities
  are not coerced.
- Scoped static input-evidence export to the exact adapted input id while preserving
  the existing artifact shape.
- Added regressions for same-method/different-occurrence inputs, same-aggregate/
  different-key paths, signed numeric syntax across graph and tuple evidence, and 40
  distinct evidence profiles per Saga.

M1 implementation files:

- `verifiers/.../faults/ScenarioGeneratorApplication.java`
- `verifiers/.../faults/scenario/ConflictGraphBuilder.java`
- `verifiers/.../faults/scenario/ExactKeyValueNormalizer.java`
- `verifiers/.../faults/scenario/InputTupleJoiner.java`
- `verifiers/.../faults/scenario/InputTupleSelection.java`
- `verifiers/.../faults/scenario/ScenarioGenerator.java`
- `verifiers/.../faults/scenario/accounting/ScenarioSpaceAccountingCalculator.java`
- `verifiers/.../faults/scenario/export/StaticAnalysisArtifactWriter.java`
- `verifiers/.../faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- `verifiers/.../faults/scenario/model/AggregateKey.java`
- `verifiers/.../faults/state/ApplicationAnalysisState.java`
- `verifiers/.../faults/state/SourceAggregateKeyInputEvidence.java`
- focused Spock specifications on those scenario/accounting/static-package surfaces.

## Proof

From `verifiers/`:

```text
mvn -q -Dtest=InputTupleSelectionSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorApplicationSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,ApplicationAnalysisScenarioModelAdapterSpec test
```

Result: 124 tests, 0 failures, 0 errors, 0 skipped.

Additional static package contract proof:

```text
mvn -q -Dtest=StaticAnalysisArtifactWriterContractSpec,CurrentExecutableArtifactContractSpec test
```

Result: 30 tests, 0 failures, 0 errors, 0 skipped.

Final correction-pass rerun:

```text
mvn -q -Dtest=InputTupleSelectionSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorApplicationSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,ApplicationAnalysisScenarioModelAdapterSpec,ApplicationAnalysisStateSpec,WorkflowFunctionalityVisitorSpec,CommandHandlerVisitorSpec,StaticAnalysisArtifactWriterContractSpec,CurrentExecutableArtifactContractSpec test
mvn -q -DskipTests compile
git diff --check
```

Result: 213 tests passed with 0 failures, 0 errors, and 0 skipped; compilation and
whitespace validation passed.

The continuing reviewer then reran the corrected selector and adapter cases: 27 tests
passed with 0 failures, errors, or skips, and `git diff --check` passed. Both pass-1
blockers were resolved, and the reviewer reported no remaining blocking or advisory
findings.

Quizzes count-only characterization from `verifiers/`:

```text
VERIFIERS_APPLICATIONS_ROOT=../applications \
VERIFIERS_APPLICATION_BASE_DIR=quizzes \
VERIFIERS_OUTPUT_ROOT=target/m1-count-profile \
VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false \
VERIFIERS_SCENARIO_CATALOG_ENABLED=true \
VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY \
VERIFIERS_SCENARIO_CATALOG_INCLUDE_SINGLES=true \
VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 \
VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=1000 \
VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=20 \
VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=false \
VERIFIERS_SCENARIO_CATALOG_INPUT_POLICY=RESOLVED_OR_REPLAYABLE \
VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=ORDER_PRESERVING_INTERLEAVING \
mvn -q spring-boot:run -Dspring-boot.run.profiles=test,sagas,local
```

Result: exit 0 in 42.22 seconds. The run analyzed 68 Sagas, 884 inputs, and 785
direct candidates; grouped accounting counted the full `all` space of 1,247,308,000
input-bound scenarios while count-only wrote no catalog rows. Strict positive coverage
was 35 size-2 and 42 size-3 Saga sets (74,273 selected input-bound scenarios total).
The output is under `verifiers/target/m1-count-profile/quizzes-20260902-174354-386/`.
Together with the bounded 40-distinct-profile regression, this did not expose a
material M3 performance risk, so no additional counting optimization was added.

The proof includes:

- equal exact candidates retained and unequal exact candidates rejected;
- one-sided symbolic/type-only candidates absent from strict but visible in fallback;
- strict exact-equal and canonical same-source tuples retained;
- strict missing, unrelated-source, and unequal tuples rejected;
- fallback missing tuples retained and known unequal tuples rejected;
- read/read candidates ignored;
- a three-Saga tuple retained only when its positive candidate subgraph connects all
  three Sagas;
- generator enumeration and accounting arithmetic agreeing at 1, 3, and 4 tuples for
  strict, fallback, and `all`, while every `all` total remains 4;
- a one-billion-tuple bounded product counted from two evidence profiles per Saga,
  yielding 250,000,000 strict-positive tuples without enumerating the concrete
  Cartesian product;
- count-only direct symbolic visibility, strict connected-set count, positive
  accepted-input coverage, and selected workload total remaining separately visible.
- candidate-specific source evidence rejecting a different direct-call occurrence and
  a different constructor/key path for the same aggregate;
- numeric exact equality working across signed literal syntax and normalized extracted
  input values without coercing quoted strings.

## Exclusions and next milestone

- No materializability/readiness rule, input recipe, input acceptance, scheduling or
  fault-generation algorithm, wrapper/limitation analysis, package role/shape,
  dynamic analysis, execution behavior, or Quizzes source was changed.
- `SagaCommand` transparency, bare-`Command` compensation recognition, and limitation
  cleanup remain M2.
- The full Quizzes before/after comparison and canonical current-state/audit updates
  remain M3; M1 performed only the correction-pass performance characterization above.
- `lib/` and `tmp/` were not touched. No branch or commit was created.

## What to inspect / reasonable veto points

- Strict selection treats the tuple's positively satisfied direct-candidate subgraph
  as the required graph. It requires that graph to connect the whole Saga set; it does
  not require every extra candidate edge in a cyclic set to be positive.
- Canonical same-source equality uses the full existing source reference identity:
  occurrence id, producer method, and property path, scoped to the candidate aggregate
  and the selected input identity. Variable-name similarity alone never qualifies.
- Relevant exact logical bindings use the resolved key-path leaf plus aggregate-name
  aliases. When both sides provide relevant exact values, disjoint values are a known
  inequality even in fallback mode.
- Fallback can use direct Cartesian multiplication when none of the selected Sagas'
  inputs has an exact logical binding: without exact values, there is no known unequal
  tuple to remove, although the type-level candidate graph must still connect the set.
- Tuple-filtered candidates are written as WorkloadPlan conflict evidence, while
  schedule enumeration deliberately retains type-level candidates until scheduling
  changes are separately approved.
