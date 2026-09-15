# Lost copied update integration handoff

State: complete.

## Delivered outcome

M0–M3 implement FR-1–11: source-derived copy contracts, attempt-scoped native/constructor
observation, exact evidence assessment, grouped overwrite count, optional fifth weight,
legacy compatibility and generated-case qualification. One committed overwriting version
per aggregate counts once; fields stay in the evidence. No manual application mappings.

## Changed surfaces and actual anchors

- Inference/model: `ConstructorCopyVisitor`, `CopyContractArtifact`, `ApplicationAnalysisState`,
  `ScenarioGeneratorApplication`, `ApplicationAnalysisScenarioModelAdapter`,
  `ScenarioModelAdapterResult`, `StaticAnalysisArtifactWriter`, `ScenarioCatalogPackageReader`.
- Framework: new `monitoring/copiedupdate/{CopiedUpdateObservation,CopiedUpdateSession}`;
  native hooks in `LocalCommandGateway`, `LocalCommandService`, `SagaUnitOfWorkService`.
- Verifier: new `copiedupdate/agent/CopiedUpdateAgent`, `LostCopiedUpdateCollector`,
  `LostCopiedUpdateAssessor`, `LostCopiedUpdateReport`; composition in
  `ImpactV2EvidenceCollector` and `ScenarioExecutor`; agent build in `verifiers/pom.xml`.
- Ordinary launcher: `verifiers/scripts/run-scenario-executor.sh`, `docker-compose.yml`.
- Search: `verifiers/experiments/fixed-workload-ga/{fitness,runtime,run}.py` and their tests.
- Application: one new feature in Quizzes `sagas/coordination/tournament/UpdateTournamentTest.groovy`.
  Existing source-only dummyapp inferredcopy fixtures are reused, not replaced.
- Tests: new `ConstructorCopyVisitorDummyappSpec`, `ConstructorCopyVisitorQuizzesSpec`,
  `CopiedUpdateSessionSpec`, `LostCopiedUpdateAssessorSpec`; additive artifact expectations
  updated in parser/generator/package fixture tests. Existing unrelated dirty edits retained.
- Reproduction: `verifiers/experiments/lost-copied-update/`; retained evidence under
  `docs/verifiers-impl/evidence/lost-copied-update-2026-09-15/`.
- Canonical documentation: current-state glossary/behavior, roadmap and lost-update research brief.

## Decisions and discovery

The constructor agent is installed before target classes load. Native hooks cover framework
boundaries; the experimental process-global object maps were replaced with a per-attempt
session. Both agent and executor verify the same manifest hash; the agent checks source
provenance. Fixed package artifact name is `copy-contracts.json`; old packages may omit it.
Normal Compose launch builds the matching application immediately before execution; frozen
experiment workers additionally retain source/class hashes.

The generated positive needed one normal application test. It did not need a custom setup
provider or a manually constructed workload. Strict static matching cannot establish the
collection-held Topic identity, so qualification uses the supported explicit type-only
fallback. This is a precision limitation, not an implementation change to conflict matching.

Two exploratory generated attempts are retained transparently: no target event write (zero)
and an initial recovery fault choice that did not compensate the target update (zero).
The final target recovery choice produces one occurrence. No broad GA run or RL was started.

## Proof

- 42 focused inference/package tests; 11 session tests; 19 assessor tests.
- 23 existing gateway/read tests; 57 read-assessor tests; 200 ScenarioExecutor tests.
- 47 Python tests and 13 Quizzes UpdateTournament tests.
- Ten controlled application histories: four positives / six controls, both transport modes,
  no copied-update gaps; business observations and committed writes preserve the baseline
  under documented audit-field exclusions.
- Ordinary generated forward/fresh/recovery cases: copied-update counts 1/0/1, exact schedules.
  Forward case has complete zero counts for all earlier criteria.
- Three additional generated-case replays with the new observer disabled preserve
  execution outcomes, existing I/read assessments and committed/final application data.
- Retained-only fifth-weight feedback: scores 1/0/1 with no new executions. Parent-selection
  and old-policy compatibility are covered by focused search tests.
- Two paired fresh-process cost runs preserve business observations; times are noisy and
  include Docker/JVM/Spring startup. See evidence, not an isolated overhead claim.

## What to try / next decision

Open the retained integration README for the forward positive and fresh control. Use
`LOST_COPIED_UPDATE=true` with the ordinary Compose executor, or a frozen worker descriptor
with `lostCopiedUpdateAgent` and `lostCopiedUpdateSourceRoot`. Configure explicit five-weight
`weighted-criteria-v2`; leave old configurations unchanged to preserve historical fitness.

Next discuss a bounded compression comparison and the larger GA/random protocol. Paper
updates and RL follow that evaluation decision. No commit, push, merge or new branch.
