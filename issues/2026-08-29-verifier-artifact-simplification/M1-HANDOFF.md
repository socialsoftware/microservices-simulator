# M1 handoff — current count-only package

- **State:** implementation complete; no commit made.
- **Boundary:** count-only/static roles only. Catalog-writing, on-demand, dynamic,
  setup, and executor behavior remain deferred.
- **Preserved state:** the pre-existing `docs/verifiers-impl/current-state.md`
  edit and untracked `lib/` and `tmp/` were not touched.

## Changed files and discoveries

- Production: `ScenarioGeneratorApplication.java`, `InputVariantNormalizer.java`,
  `ApplicationAnalysisScenarioModelAdapter.java`, `ScenarioModelAdapterResult.java`,
  `ScenarioCatalogManifest.java`, `StaticAnalysisArtifactWriter.java`, and
  `ScenarioCatalogPackageReader.java`.
- Tests: `ScenarioGeneratorApplicationSpec.groovy`,
  `DummyappAccountingFixtureFoundationSpec.groovy`, and
  `StaticAnalysisArtifactWriterContractSpec.groovy`.
- Handoff: this file.

The one current package boundary is `ScenarioCatalogManifest.Current` plus
`ScenarioCatalogPackageReader.readCurrentStatic`. Its integer `formatVersion: 1`
manifest contains exactly the four roles (`accounting`, `sagas`, `inputs`,
`interactions`), each with only `path` and `sha256`. The writer emits exactly those
four artifacts; no executable placeholder, workload, vector, schedule, or runtime
artifact is produced. The old executable `read()` API remains only because its
catalog-writing tests are deferred to M2; it is not used by count-only.

Accounting is a compact projection of the existing
`ScenarioSpaceAccountingCalculator`/`Report`, so input-bound totals preserve the
calculator's logical-key contradiction filter and distinct `SEGMENT_COMPRESSED`
schedule semantics. It contains applicable configuration, equations, per-Saga rows,
direct evidence totals, strict/fallback connected-set counts, event emission/route
totals, and exact JSON integer values; it does not persist concrete sets or runtime
rows. Focused parity tests cover a contradictory exact-key pair and compressed
schedule counts.

Saga facts combine dispatch and aggregate footprints, use Saga-local step and route
ids, retain per-step limitations, and keep compensation `step` optional: the
current analyzer records explicit registration but does not provide a named target,
so no false self-target is emitted. If a future analyzer supplies a target, the
reader validates that it is a Saga-local step. Routes attach by the complete
analyzer `triggerStepKey`, not the display method name. Semantically duplicate
analyzer consequence rows collapse to one emitted event-handler-downstream chain;
distinct routes from one emission retain stable Saga-local ids such as
`step#0/event#0` and `-route#1`. Coverage includes repeated same-name steps, a
duplicate consequence row, a second distinct route, and reader rejection of
duplicate route ids.

Input facts retain all normalized records, including rejected records, and keep
acceptance separate from materializability. Resolution is one of
`fullyResolved`, `runtimeDependent`, `partial`, or `unresolved`; recipes use compact
kind-specific values. Runtime scope is derived only from the owned type
(`SagaUnitOfWorkService`/`CommandGateway` = `execution`, `SagaUnitOfWork` =
`participant`). Blockers are recursively evaluated from each top-level recipe
argument, so a failure nested under a property, transform, or constructor is
attributed to that owning argument and its source expression. The same reason on
multiple arguments retains both blocker records, while accounting counts one
affected input per reason rather than blocker occurrences. Input aggregate-key evidence is emitted only
from matching analyzer `SourceAggregateKeyInputEvidence`; logical bindings are not
paired with unrelated Saga footprints.

The reader validates required fields and types, requires rejected reasons only on
rejected inputs, rejects blockers on materializable inputs, requires non-empty
blocker objects on blocked inputs, package-contained non-symlink paths, hashes, UTF-8/JSONL,
duplicate Saga/input/interaction/step/route identities, and dangling Saga,
dependency, route, and interaction step references. The single shared M0 fixture is
projected through the actual writer publication path: emitted accounting and all
three JSONL roles are asserted byte-for-byte, the emitted manifest (including
hashes) is asserted value-for-value, and the central reader then observes the same
approved examples. No second fixture or production fixture special case exists.
Dummyapp tests cover accepted/rejected/materializable/blocked and strict/fallback facts.

## Proof

```text
cd verifiers && mvn -q -Dtest=DummyappAccountingFixtureFoundationSpec test    PASS (25)
cd verifiers && mvn -q -Dtest=StaticAnalysisArtifactWriterContractSpec test    PASS (1)
cd verifiers && mvn -q -Dtest=ScenarioGeneratorApplicationSpec test           PASS (19)
cd verifiers && mvn -q -Dtest=ScenarioSpaceAccountingCalculatorSpec test       PASS (31)
cd verifiers && mvn -q -DskipTests compile                                   PASS
cd verifiers && mvn -q -DskipTests test-compile                              PASS
git diff --check                                                               PASS
```

The retained M0 comparison report is
`/Users/andre/meic/thesis/microservices-simulator/verifiers/target/overnight-20260827-232754/census-strict-size3/quizzes-20260828-000834-407/scenario-space-accounting.json`.
It predates this worktree's HEAD and reports 68 Sagas (36/32 with/without accepted
inputs), 777/86 accepted/rejected inputs, 91/686 materializable/blocked accepted
inputs, strict connected sets 140/1,299 for sizes 2/3, fallback connected sets
540/7,005, all/selected input-bound totals
1,161,251,056/42,079,271, and zero written workloads.

A fresh Docker Compose run exercised the actual M1 adapter and writer with the same
count-only, no-singles, size-3, input-policy, schedule, fallback, and bounding
configuration. The default 768 MiB container was insufficient for Quizzes parser
analysis, so the bounded rerun used a 2 GiB container limit and 1.5 GiB Java heap;
dynamic enrichment remained disabled. The resulting five-file package is
`verifiers/target/quizzes-20260901-213305-280/` (4,590,291 bytes): 68 Saga rows,
884 input rows, 764 interaction rows, one accounting object, and one manifest.
Its equations all hold: `794 + 90 = 884` accepted/rejected and
`91 + 703 = 794` materializable/blocked. Its direct interaction evidence
(`0 exact + 152 symbolic + 612 typeOnly = 764`) and strict/fallback connected-set
totals match M0 exactly at both sizes. It emitted zero workload rows as required.

The current analyzer finds 17 additional accepted and 4 additional rejected input
variants, so the current input-bound totals are 1,247,307,206 (all) and 45,967,431
(selected), not the retained values. To isolate application-source drift, the
parent-of-HEAD Quizzes source was mounted read-only and analyzed through the current
M1 pipeline; `verifiers/target/quizzes-20260901-213508-948/` produced the same
current totals. Exact historical input/workload parity therefore cannot be recreated
without restoring the older verifier analyzer that produced the retained pre-HEAD
report, which is outside M1. Every invariant available across analyzer epochs was
compared, and the fresh package proves the current size-2/3 accounting equations
through the real adapter/writer boundary.

## Observational dummyapp package size

One focused run produced exactly 5 files / 107,164 bytes:

| role | bytes | records/lines |
| --- | ---: | ---: |
| accounting | 3,185 | 1 JSON object |
| sagas | 7,825 | 8 |
| inputs | 65,820 | 48 |
| interactions | 29,672 | 63 |
| manifest | 482 | 1 JSON object |

No material scope delta was found.
