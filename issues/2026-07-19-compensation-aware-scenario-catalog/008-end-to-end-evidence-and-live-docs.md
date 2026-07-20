# 008 - End-to-End Evidence and Live Docs

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `005-atomic-on-demand-multi-fault-persistence.md`, `007-runtime-fallback-and-hard-stops.md`  
ACs covered: `AC-1–AC-46 integration gate; primary ownership AC-43, AC-44, AC-45, AC-46`  
Risk: `high`

## Purpose

Reconcile all slices through the real parser/package/executor paths, capture bounded user-visible Quizzes evidence, and only then migrate live verifier documentation from implemented v2 terminology to validated v3 truth.

## Scope

- Add/complete dummyapp-first integration coverage through visitors, adapter, WorkloadPlan generation, eager FaultScenario generation, package writing/reading, enrichment linkage, on-demand persistence, and execution fixtures.
- Run the named targeted simulator controls/recovery/ordinary-workflow regressions, the full simulator suite, the full verifier suite, and the Docker verifier test service.
- Run S3’s high-cardinality non-exhaustive count/cap test and a bounded recovery-generation performance smoke before Quizzes.
- Run one explicitly bounded Quizzes v3 generation with a positive recovery cap (default/effective `20`), bounded workload materialization, and segment-compressed forward scheduling where configured.
- Inspect and record WorkloadPlan totals/materializable counts, eager vector counts, per-vector uncapped/written recovery counts, effective cap, artifact paths, and deterministic ids.
- Select at least one persisted, executable compensation-interleaving FaultScenario and run it through the updated Docker `scenario-executor` without a vector overlay.
- Record participants, assigned vector, planned/actual actions, lifecycle/conformance/terminal status, report path, relevant logs, and unchanged package checksums.
- Verify dynamic enrichment is WorkloadPlan-linked sidecar evidence and does not modify either semantic catalog.
- Verify the S2 temporary executor bridge is gone: no production `ScenarioPlan`/enriched-catalog execution, v2 artifact-reader, `--fault-vector`, or `FAULT_VECTOR` surface remains outside documented historical/test fixtures.
- After all validation passes, update current/live docs to use WorkloadPlan/FaultScenario terminology, state the v3-only boundary, cite the new evidence, and preserve remaining non-goals.

## Out of Scope

- Expanding the run merely to improve headline counts.
- Hiding environmental/domain deviations, resetting generic application state, implementing scoring/search, or claiming runtime parity outside Saga/local sequential execution.
- Rewriting archived historical v2 evidence as though it were produced by v3.

## Repo Anchors

- `applications/dummyapp/` and all verifier Spock fixtures — automated integration baseline.
- `applications/quizzes/` — realistic bounded smoke target.
- `docker-compose.yml`, `verifiers/scripts/run-scenario-executor.sh`, and `verifiers/target/` — user-visible run/log/artifact path.
- `docs/verifiers-impl/current-state.md` — canonical implementation truth.
- `docs/verifiers-impl/glossary.md`, `roadmap.md`, `evidence.md`, `advisor-brief.md`, `thesis-claims-evidence-map.md`, and `reference/scenario-executor.md` — required live documentation migration.
- `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md` — accepted design boundary; update status only if repository conventions require it after proof.

## Implementation Shape

- Prefer strengthening dummyapp fixtures over introducing Quizzes-specific branches.
- Keep Quizzes parameters explicit and bounded in recorded commands. Report the actual slot-length distribution/counts instead of relying on the spec’s illustrative `808` number.
- Use container logs and host-mounted artifacts as evidence, matching the project’s Docker guidance.
- Checksum workload/fault catalogs, manifest, accounting, and enrichment before and after execution.
- If the selected Quizzes scenario has a meaningful zero-bit runtime deviation, report it honestly; AC-45 still requires at least one executable compensation-interleaving scenario and its action-aware report, not a fabricated `EXACT` outcome.
- Update documentation only from observed commands/artifacts. Distinguish implemented/bounded/partial/deferred status and leave archives historical.

## TDD / Test Shape

- First behavior to test: the dummyapp end-to-end package contains stable WorkloadPlan/FaultScenario links and can replay a compensation interleaving with planned/actual action evidence.
- Expected red failure: individual slices may pass while application wiring, Docker inputs, artifact names, or sidecar references remain inconsistent.
- Additional coverage: repeated package bytes; v2 absence/rejection; on-demand multi-fault persisted before selection; package/enrichment immutability; all required accounting/manifest fields; full report field/lifecycle/conformance checks.

## Just-in-Time Preflight Required

Before source edits or smoke runs, the executor must re-check:

- All prior slice completion evidence and targeted tests.
- Current Compose environment variable/CLI names established by S4–S6; do not use the removed vector overlay.
- Current Quizzes materializable candidate distribution and a safe bounded package configuration.
- S3 high-cardinality instrumentation and bounded performance-smoke evidence; do not start Quizzes if capped generation still traverses/materializes the full recovery leaf space.
- S2’s recorded legacy-island inventory and S6’s removal evidence, including documented historical/test exclusions for repository-wide searches.
- Existing `verifiers/target/` artifacts so new run paths are unique and historical evidence is not overwritten.
- Live docs’ current status text immediately before updating it.

## Verification

- `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` — executor controls, stepwise recovery/retry, and ordinary Saga behavior pass.
- `cd simulator && mvn test` — full simulator suite passes; if blocked by an unrelated pre-existing failure, record the exact failure and retain successful targeted evidence rather than claiming a full pass.
- Run S3’s instrumented high-cardinality generator test and bounded performance smoke — exact count is correct, retained schedules stay within cap, and the full leaf set is neither traversed nor retained.
- `cd verifiers && mvn test` — full verifier suite passes.
- `docker compose run --rm fault-analysis-scenario-gen-test` — containerized verifier tests pass.
- Run `docker compose run --rm` with explicit `-e` overrides for the bounded `fault-analysis-scenario-gen` configuration established by S4 (multi-Saga bound, catalog bound/write mode, `SEGMENT_COMPRESSED`, recovery cap `20`, and explicit dynamic-enrichment choice) — v3 Quizzes package is produced under `verifiers/target/<run>/`.
- Run updated `scenario-executor` against that package and one persisted FaultScenario id — action-aware report is written under the run/evidence directory with no execution-time vector.
- Run repository-wide production-reference checks using the S2 inventory — no executor v2 reader/enriched-catalog execution, `scenario-catalog.jsonl`, `--fault-vector`, or `FAULT_VECTOR` remains except explicitly documented historical/test fixtures.
- Inspect container logs, manifest, accounting, selected records, report, and pre/post checksums — AC-45 fields and immutability are demonstrated.
- Inspect live-doc diffs against evidence — current truth, compatibility boundary, limitations, and remaining work agree.

## Evidence to Record

- files changed
- exact commands and successful outputs/container exits
- full test summaries
- Quizzes run directory and artifact paths
- WorkloadPlan/materializable/eager vector/uncapped/written counts and effective cap
- selected WorkloadPlan/FaultScenario ids, participants, vector, planned/actual actions, conformance, lifecycle, terminal status, and report path
- package/enrichment checksums before and after execution
- docs updated and claims cross-check
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Quizzes state/environment can create domain deviations; do not misclassify them as executor success or silently retry.
- Full dynamic enrichment is expensive. Its enabled/disabled choice must be explicit and evidence claims must match that choice.
- Documentation must not overstate all-vector completeness, generic execution, distributed parity, reset support, scoring, or search.
- A recovery cap is not a scalability guarantee if generation enumerates the uncapped leaf set first; require the S3 oracle before accepting Quizzes evidence.
- Do not call v3 canonical while the S2 executor-only legacy island remains; S6 removal and the production zero-reference gate are mandatory.
