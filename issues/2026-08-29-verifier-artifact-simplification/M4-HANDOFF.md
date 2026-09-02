# M4 handoff — realistic qualification and documentation

Date: 2026-09-02

## Outcome

M4 qualified the final current-only package on bounded Quizzes paths. Count-only,
catalog-writing, preflight, replay, on-demand mutation, and dynamic normalization were
all exercised without broad catalog materialization. No application semantics, impact
scoring, search behavior, deployment, push, merge, or commit changed.

The next ranked issue is the runtime input-map plan-id mismatch: the verifier writes
`workloadPlanIds` while the simulator reader expects `scenarioPlanIds`. The final dynamic
smoke reports this explicitly and does not invent exact input matches.

## Fresh count-only qualification

The first diagnostic run used `maxInputsPerSaga=3` and compressed scheduling. It was
safe and bounded but was not used as the equivalent comparison. One corrected run
changed only those evidenced variables to the retained baseline's input cap 1,000 and
order-preserving scheduling:

```bash
MEDIUM_MEM_LIMIT=5g MEDIUM_MEM_RESERVATION=2g docker compose run --rm -T \
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

Fresh package: `verifiers/target/quizzes-20260902-011240-501/`.
Docker log: `verifiers/target/m4-final/count-only-equivalent-docker.log`.

It declares exactly `accounting`, `sagas`, `inputs`, and `interactions`:

| Role/file | Records | Bytes | SHA-256 |
|---|---:|---:|---|
| `accounting.json` | 1 JSON document | 21,176 | `c1560373094ae24af2aab739726947ae0a6da289829bee61a2274badf16d126f` |
| `sagas.jsonl` | 68 | 99,191 | `74a0f6795cc37e84655a0752a732183b39c885c0c6c8316a05927760a8cb2dca` |
| `inputs.jsonl` | 884 | 4,066,512 | `9a5d3b27b4c64f256d01548a8d384fa7257ccd7e6a980cbeb72786d7e0123f4a` |
| `interactions.jsonl` | 764 | 402,946 | `110e86ac0ed48306f0c30b35f3db92a076354dc81743ddd9a623545f86e5f646` |
| `scenario-catalog-manifest.json` | 1 JSON document | 482 | `51f34fadf34dd057181319c0eb9b07b85ed45a2a47f19c528ec4c645f0b14ead` |

Observed accounting:

- 68 Sagas; 36 with and 32 without accepted inputs;
- 884 inputs; 794 accepted and 90 rejected; 91 materializable and 703 blocked;
- 764 direct interactions: 0 exact, 152 symbolic, 612 type-only;
- strict connected sets sizes 2/3: 140 / 1,299;
- fallback connected sets sizes 2/3: 540 / 7,005;
- all workloads sizes 1/2/3: 36 / 630 / 7,140;
- selected workloads sizes 1/2/3: 36 / 63 / 400;
- all/selected input-bound totals: 1,247,308,000 / 45,968,225;
- workloads written: 0.

Retained comparison:
`/Users/andre/meic/thesis/microservices-simulator/verifiers/target/overnight-20260827-232754/census-strict-size3/quizzes-20260828-000834-407/scenario-space-accounting.json`.
It excluded singles but used the equivalent size-2/3 strict/input/schedule bounds. The
topology is unchanged: 68 Sagas, 36/32 coverage, strict 140/1,299, fallback 540/7,005,
and 91 materializable inputs. The older analyzer epoch found 863 inputs, accepted 777,
and reported all/selected input-bound totals 1,161,251,056 / 42,079,271. M1 already
isolated the current analyzer's no-singles totals at 1,247,307,206 / 45,967,431.
Required size-1 analysis adds exactly 794 accepted-input singles to both, yielding the
fresh 1,247,308,000 / 45,968,225. The writer persists all 884 current input facts,
rejects 90, and accepts 794. This is a scoped analyzer-epoch comparison rather than a
false byte-identical input claim.

## Fresh executable package, preflight, request, and replay

The production writer was driven by the existing focused source-analysis proof:

```bash
cd verifiers
mvn -Dtest=SourceDerivedSharedSagaWorkloadAnalysisSpec \
  -DcheckpointC.packageOutput=$PWD/target/m4-final/quizzes-source-package test
```

`source-package-test.log` reports 1 test passed. Before on-demand/dynamic mutation the
package contained 68 Sagas, 847 inputs, 764 interactions, one reusable 12-action
source-derived setup, one five-slot Remove/Add WorkloadPlan, 13 eager FaultScenarios,
and an empty request stream.

Preflight command:

```bash
MEDIUM_MEM_LIMIT=5g MEDIUM_MEM_RESERVATION=2g \
PACKAGE_PATH=/reports/m4-final/quizzes-source-package/scenario-catalog-manifest.json \
OUTPUT_PATH=/reports/m4-final/source-preflight-report.json \
docker compose run --rm -T -e PREFLIGHT=true -e JAVA_TOOL_OPTIONS=-Xmx4g scenario-executor
```

`source-preflight-report.json` is `SUCCESS`: one candidate, two participants, source
setup succeeded, all 12 actions succeeded, four participant bindings resolved, and the
workload is `SETUP_READY`. The complete container log is
`source-preflight-docker.log`.

On-demand command used the verifier classes plus Maven dependency classpath:

```bash
java -cp "target/classes:$(tr -d '\n' < target/m4-final/request-classpath.txt)" \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.FaultScenarioRequestCli \
  --manifest-path target/m4-final/quizzes-source-package/scenario-catalog-manifest.json \
  --workload-plan-id 12f7f358f8c3c04a6541a8a86450639089e1922c355206871fc2df6047c68417 \
  --fault-vector 10100 --recovery-schedule-cap 20
```

`on-demand-request.json` reports `PERSISTED`, effective cap 20, uncapped/written count
1/1, one added scenario, id
`2dcaa575630b3c8e7bba41f27ef0717b5bc05a2a0f4df8c7d65f6965b783260a`.

That exact id was selected in a fresh Docker execution with the same 5 GiB container / 4
GiB heap bound. `source-on-demand-execution-report.json` reports
`PARTIAL_COMPENSATED / EXACT`; `source-on-demand-impact-report.json` is evaluated with
zero findings. The complete log is `source-on-demand-replay-docker.log`.

## Fresh dynamic smoke

A host Groovy invocation constructed `DynamicEnrichmentConfig` directly, loaded
WorkloadPlans through the current reader, and ran `DynamicEnrichmentOrchestrator` for
one class. These are the exact outer commands used from `verifiers/`:

```bash
mvn -q dependency:build-classpath \
  -Dmdep.outputFile=target/m4-final/request-classpath.txt

java -cp "target/classes:$(tr -d '\n' < target/m4-final/request-classpath.txt)" \
  groovy.ui.GroovyMain -e '
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.*
def manifest = Path.of("target/m4-final/quizzes-source-package/scenario-catalog-manifest.json").toAbsolutePath().normalize()
def contents = new ScenarioCatalogPackageReader().read(manifest)
def cfg = new DynamicEnrichmentConfig(
    true,
    false,
    "dynamic-evidence",
    "src/test/groovy",
    [],
    [],
    [],
    300,
    new DynamicEnrichmentConfig.DynamicEnrichmentMavenConfig("mvn", "test-sagas"))
def result = new DynamicEnrichmentOrchestrator().run(
    cfg,
    Path.of("../applications/quizzes").toAbsolutePath().normalize(),
    "quizzes",
    manifest.parent,
    ["pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.RemoveTournamentAddParticipantRecoveryWindowExploratoryTest"],
    contents.workloadPlans(),
    manifest,
    OffsetDateTime.now(ZoneOffset.UTC).toString())
println "observations=${result.joinResult().observations().size()} attributions=${result.joinResult().attributions().size()} accounting=${result.joinResult().dynamicAccounting()}"
' > target/m4-final/dynamic-smoke-host.log 2>&1
```

Effective configuration was therefore: enabled; partial test runs disallowed;
diagnostic subdirectory `dynamic-evidence`; test source root `src/test/groovy`; empty
include/exclude directory and class lists; 300-second timeout; Maven executable `mvn`;
profile `test-sagas`. The application path was `../applications/quizzes`, package/run
directory was the manifest parent, workload input came from
`ScenarioCatalogPackageReader.read(manifest).workloadPlans()`, and the manifest path was
the same current package later mutated by the dynamic writer.

The Maven command recorded in `dynamic-evidence/test-run.json` selected that single
class under `test-sagas`; all 5 features passed in 9.728 seconds. The run normalized one
raw evidence file (1,333,941 bytes), then deleted raw `dynamic-evidence.jsonl` only after
the package finalized.

Final dynamic accounting:

- observations 1,038: 188 step-started, 188 step-finished, 422 command-sent, 239
  aggregate-accessed, 1 invariant violation, 0 without test context;
- invocation attribution 10: 0 exact-input, 2 test-and-shape, 8 shape-only, 0 ambiguous,
  0 unmatched;
- strongest unique input evidence: 0 exact, 2 test-and-shape, 0 shape-only;
- workload participant evidence: one all-inputs-observed-in-one-common-test workload.

`dynamic-normalization-diagnostics.json` explicitly reports the known
`workloadPlanIds` / `scenarioPlanIds` mismatch. It also reports 933 observations without
a uniquely resolved Saga/step and 123 without an invocation. These limitations are not
converted into exact matches.

## Final package integrity and sizes

After the request and dynamic publication, every declared role hash matched the file
bytes and `ScenarioCatalogPackageReader.read(...)` revalidated all references, returning
one WorkloadPlan and 14 FaultScenarios:

| Role/file | Records | Bytes | SHA-256 |
|---|---:|---:|---|
| `accounting.json` | 1 JSON document | 22,015 | `7ce9ce5ee519ca74d4e78ea074f5c557a31abceb55eb281ba5849bedd2c3d798` |
| `sagas.jsonl` | 68 | 90,446 | `8feba34caebf0367891ef6a7d28ebd6c2fe5c4219d42af46f689ad7442da2214` |
| `inputs.jsonl` | 847 | 3,959,368 | `d0bce2cffc9e5e95919e8440abd8b8df63832dbaa8ea959dbac10a99bf5679f3` |
| `interactions.jsonl` | 764 | 402,946 | `110e86ac0ed48306f0c30b35f3db92a076354dc81743ddd9a623545f86e5f646` |
| `setups.jsonl` | 1 | 9,643 | `87d479e523fc6d5faa00a51cd7a7ecc51c5181b272504d3007e82c01db6b3b3a` |
| `workloads.jsonl` | 1 | 1,050 | `4f832f7979a0592bda66f66d8f36e78bef23931b22fc7e0911d3fba2eb2b760f` |
| `fault-scenarios.jsonl` | 14 | 3,792 | `3f13a2793a2437112da771cb804bd7aa7c23c6d377f49cc8a1d0bba89fa2902f` |
| `requests.jsonl` | 1 | 262 | `3ca590ae5a88f151f7eda5941ce050acaac9ccd7ce7c5328c6fdfc1e93eb8418` |
| `dynamic-observations.jsonl` | 1,038 | 724,297 | `19183026cb1e793645a623e08f2410b8379a3470150b9cd5f38e8404c03f65c6` |
| `dynamic-attribution-links.jsonl` | 10 | 8,995 | `885615109e54a414f5557607bd279ec7f21c3beb31fe3ab1f443f904ef02e492` |
| `scenario-catalog-manifest.json` | 1 JSON document | 1,226 | `c39482ebaebeea4f0a6079b871bb35c59916995e94722941bedb6743e5839180` |

Diagnostics, logs, input maps, preflight, execution, and impact reports remain outside
the package roles. The executable package itself remains at
`verifiers/target/m4-final/quizzes-source-package/`.

## Documentation and stale-name sweep

Updated:

- `docs/verifiers-impl/current-state.md` — current role/file ownership, reader,
  on-demand, dynamic lifecycle, fresh M4 evidence, and limitations;
- `docs/verifiers-impl/roadmap.md` — exact input-map repair is the next ranked outcome;
- `docs/verifiers-impl/README.md` and retained active decisions — current package and
  normalized-dynamic terminology replaced stale five-file/versioned-package and
  sidecar-only claims while preserving explicitly dated historical evidence;
- `issues/2026-08-30-current-only-verifier-artifacts/AUDIT.md` — final evidence-based
  ranking and historical wording;
- current ScenarioGenerator defaults/config/tests — `workloads.jsonl`,
  `fault-scenarios.jsonl`, `accounting.json`, and `inputs.jsonl` now match the current
  contract rather than old catalog filenames.

Historical issue discussions retain old filenames only when explicitly describing
pre-change evidence or removed surfaces.

## Review-round current-only corrections

The M4 review sweep removed the remaining active compatibility surfaces rather than
leaving aliases:

- `ScenarioExecutorOrchestrator` now requires a “current package path”, with a direct
  message assertion;
- `StaticAnalysisArtifactWriter` describes the present static/current executable
  boundary;
- `WorkloadPlanValidator` no longer accepts the historical v4 workload/setup path,
  `ScenarioIdGenerator` no longer has a historical setup-excluding hash branch, and
  the unused legacy constants were removed from `WorkloadPlan`,
  `ScenarioCatalogManifest`, and `RejectedInputVariant`;
- `rejected-inputs-path` was removed from both application YAML files, Spring
  configuration, resolved-path/logging behavior, configuration tests, and the
  executor reader projections. Rejected input facts remain authoritative in
  `inputs.jsonl`; no no-op path alias remains;
- provider-backed setup locals/tests were renamed where “legacy” incorrectly
  described active behavior, while separate execution/report schema v5 and genuinely
  historical documentation remain labelled as such;
- the roadmap's Outcome 2 done criterion now owns explicitly historical 2026-08-01
  evidence and requires current regeneration for any new claim;
- duplicate/broad recovery and strict/broad accounting bullets were removed from the
  current-state document.

The exact reproducible dynamic-smoke command and every effective
`DynamicEnrichmentConfig` value are recorded in the “Fresh dynamic smoke” section
above. It uses the current reader for workload loading, names the one selected test
class, and records the application, run, manifest, and diagnostic paths; it is the
recovered command actually used, not a reconstructed approximation.

## Validation

Focused current-only/configuration validation:

```text
mvn -Dtest=ScenarioGeneratorApplicationSpec,ScenarioExecutorOrchestratorSpec,ScenarioSetupPreflightProcessOrchestratorSpec,ScenarioModelSpec,EventConsequenceStaticBoundarySpec,RecoveryScheduleGeneratorSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,CurrentExecutableArtifactContractSpec test
Tests run: 181, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn -Dtest=ScenarioExecutorSpec,CurrentExecutableArtifactContractSpec,ScenarioExecutorOrchestratorSpec test
Tests run: 156, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Final clean-state proof:

```text
cd verifiers && mvn clean test
Tests run: 654, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

cd verifiers && mvn -q compile test-compile
exit 0

cd verifiers && git diff --check
exit 0
```

Before `clean`, the authoritative M4 evidence directories were copied to a fresh
`mktemp` directory because Maven removes `target/`. The same directories and the full
`target/m4-final/clean-full-test-r1-final.log` were copied back afterward. The restored
manifests then passed exact hash/reference validation through
`ScenarioCatalogPackageReader.readCurrent(...)` plus
`readCurrentForExecution(...)` for the executable package and
`readCurrentStatic(...)` for count-only. The proof is recorded in
`target/m4-final/post-clean-reader-validation-r1-final.log`: executable package 1
workload, 14 scenarios, 1,038 observations, and 10 attributions; count-only package 68
Sagas, 884 inputs, and 764 interactions.

## Scope and remaining limitations

- No application behavior or static-selection/materialization algorithm changed.
- Exact runtime input identity remains blocked by the documented map-field mismatch.
- Only one source-derived workload, one requested vector, one replay, and one dynamic
  test class were qualified; this is not generic execution or broad dynamic coverage.
- The first count-only diagnostic package used non-equivalent caps and is not baseline
  evidence; the corrected package is authoritative.
- On-demand locking/publication remains local-filesystem/process scoped; network
  filesystems, multi-host coordination, and abrupt host/process death are unqualified.
- `lib/` and `tmp/` remain unrelated and untouched.
