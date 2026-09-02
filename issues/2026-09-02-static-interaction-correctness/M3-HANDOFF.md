# M3 handoff — Quizzes qualification and current documentation

- **State:** complete; milestone and final review passed on pass 2.
- **Outcome:** the corrected static-interaction model is qualified against the retained
  Quizzes baseline, every bounded size-1/2/3 equation reconciles, the current reader
  accepts the fresh deterministic package, and materializability is still the next
  thesis bottleneck. Covers FR-18–FR-21.

## Exact run and artifacts

From the repository root:

```bash
env MEDIUM_MEM_LIMIT=5g MEDIUM_MEM_RESERVATION=2g /usr/bin/time -p docker compose run --rm \
  -e JAVA_TOOL_OPTIONS=-Xmx4g \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false \
  -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY \
  -e VERIFIERS_SCENARIO_CATALOG_INCLUDE_SINGLES=true \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_CATALOG_SCENARIOS=1 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=1000 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=20 \
  -e VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=false \
  -e VERIFIERS_SCENARIO_CATALOG_INPUT_POLICY=RESOLVED_OR_REPLAYABLE \
  -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=ORDER_PRESERVING_INTERLEAVING \
  fault-analysis-scenario-gen
```

The primary run completed successfully in 36.28 seconds and wrote
`/reports/quizzes-20260902-182645-973`, mounted at
`verifiers/target/quizzes-20260902-182645-973/`. Compose rebuilt the verifier image
from this worktree through the service's build policy. A second identical run completed
in 55.26 seconds at `verifiers/target/quizzes-20260902-183723-449/`; all five package
files were byte-identical.

## Baseline comparison

Baseline: `verifiers/target/quizzes-20260902-011240-501/`.

| Fact | Baseline | Corrected |
|---|---:|---:|
| Sagas; with/without accepted inputs | 68; 36/32 | 68; 36/32 |
| inputs; accepted/rejected | 884; 794/90 | 884; 794/90 |
| materializable/blocked | 91/703 | 91/703 |
| direct exact/symbolic/type-only | 0/152/612 | 0/535/250 |
| strict connected sets, size 2/3 | 140/1,299 | 382/3,594 |
| strict sets with accepted inputs, size 2/3 | 63/400 | 35/42 |
| fallback connected sets, size 2/3 | 540/7,005 | 547/7,190 |
| fallback sets with accepted inputs, size 2/3 | 223/1,840 | 227/1,904 |
| all Saga sets, size 1/2/3 | 36/630/7,140 | 36/630/7,140 |
| selected Saga sets, size 1/2/3 | 36/63/400 | 36/35/42 |
| all input-bound total | 1,247,308,000 | 1,247,308,000 |
| selected input-bound total | 45,968,225 | 74,273 |

The direction of a count is not the success condition. `CreateQuestionCommand` and
`CreateQuizCommand` declare null aggregate roots, so the old analysis wrongly treated
their third call arguments—a course and course execution—as Question and Quiz keys.
They are now correctly keyless. `AnswerQuestionCommand`, `RemoveQuestionCommand`, and
getter-based `UpdateQuestionCommand` calls now retain their declared semantic roots.
This increases strict type-level connectivity while the positive input-evidence rule
removes unsupported concrete tuples. The former AnswerQuestion/ConcludeQuiz questionable
pair remains fallback-only because the ConcludeQuiz access declares a null root.

All 884 input ids and non-evidence fields, plus discovery, acceptance, and
materializability totals, stayed unchanged. Aggregate-key evidence intentionally changed
on 84 inputs: 83 evidence values were semantically replaced and one was removed. The
baseline has 652 evidence-bearing inputs and the corrected package has 651. This is the
expected result of corrected semantic root paths, not an input-discovery or
materializability change. Serialized forward command accesses remain 132. The realistic
source proof finds 26 usable generic compensation footprints, compared with none before
wrapper/compensation handling. Each compensation aggregate is derived independently
from the bare payload's service token through its matching command handler; its root is
derived from the bare payload's third argument under the base `Command` root-key
contract. Limitations fell from 573 across all 134 steps to two limitations across two
steps: one unresolved wrapper payload and one unresolved helper dispatch.

## Reconciled accounting and package proof

- all Saga sets: `36 + 630 + 7,140 = 7,806`;
- baseline selected sets: `36 + 63 + 400 = 499`;
- corrected selected sets: `36 + 35 + 42 = 113`;
- all input-bound: `794 + 2,460,298 + 1,244,846,908 = 1,247,308,000`;
- baseline selected input-bound:
  `794 + 543,911 + 45,423,520 = 45,968,225`;
- corrected selected input-bound: `794 + 7,067 + 66,412 = 74,273`.

The size-2 corrected component was independently obtained with the otherwise identical
max-size-2 run at `verifiers/target/quizzes-20260902-183205-571/` (25.87 seconds); size 3
is the difference from the full cumulative total. Count-only wrote zero catalog rows and
no `workloads.jsonl`.

`ScenarioCatalogPackageReader.readCurrentStatic` accepted the primary package with 68
Sagas, 884 inputs, and 785 interactions. Independent checks verified manifest SHA-256,
stable ordering, unique Saga/input/interaction identities, references, and absence of a
workload role/file. Package sizes are accounting 21,202 bytes; Sagas 55,815; inputs
4,066,606; interactions 487,506; manifest 482.

## Proof and one corrected test inventory

From `verifiers/`:

```text
mvn -q -Dtest=CommandHandlerVisitorSpec,WorkflowFunctionalityVisitorSpec,ApplicationAnalysisStateSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,InputTupleSelectionSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorApplicationSpec,ApplicationAnalysisScenarioModelAdapterSpec,StaticAnalysisArtifactWriterContractSpec,CurrentExecutableArtifactContractSpec test
mvn -q test
mvn -q -DskipTests compile test-compile
git diff --check
```

The focused set passed 196 tests with zero failures, errors, or skips. The first full run
ran 675 tests and found two failures in `ApplicationsFileTreeParserSpec`: both exact
dummyapp inventories omitted the M0 `SemanticRootItemCommand` fixture. The expectations
were corrected without changing production behavior. The second full run passed all 675
tests with zero failures, errors, or skips in 30.87 seconds. Compile/test-compile and
whitespace validation also passed.

## Excluded default-run finding

An accidental run without Compose `-e` overrides used the service defaults, wrote
`verifiers/target/quizzes-20260902-182552-906/`, and failed reader validation because a
WorkloadPlan `16bb4068…` referenced missing inputs `001ab291…` and `9d63e608…`;
`db6ade87…` referenced `607a131d…` and `48399729…`. Prerequisite generation creates
these four synthetic participant inputs while the static
writer receives only adapter inputs, so the workload references are not backed by
`inputs.jsonl`. The relevant ownership boundary predates M0–M2 and their diffs do not
alter it. This is a separate catalog-writing defect, not a regression in the corrected
static analysis; it was not fixed or used as qualification evidence.

## Files and exclusions

M3 changed only:

- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ApplicationsFileTreeParserSpec.groovy`;
- `docs/verifiers-impl/current-state.md`;
- `docs/verifiers-impl/roadmap.md`;
- `issues/2026-08-30-current-only-verifier-artifacts/AUDIT.md`;
- this handoff.

No Quizzes source or production semantics changed. Dynamic enrichment was not rerun.
`lib/` and `tmp/` were preserved. No branch, commit, push, merge, deployment, or release
action was performed.

## Review correction pass 1

- Corrected the current-state, audit, and this handoff to describe independent generic
  compensation derivation from the service token/handler and base-command root argument;
  no forward-step access is reused.
- Replaced the overbroad input-fact stability statement with the measured evidence delta:
  84 changed inputs, comprising 83 semantic replacements and one removal, while ids,
  non-evidence fields, and discovery/acceptance/materializability totals stayed fixed.
- Documentation-only correction; `git diff --check` passed. No production or Quizzes
  source was edited.
