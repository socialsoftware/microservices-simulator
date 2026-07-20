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

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Reconciled the complete simulator/verifier pipeline and fixed the two stale integration expectations exposed by the full verifier suite: dummyapp file-tree discovery now includes the existing setup-helper ownership fixture, and dynamic-enrichment integration now writes/snapshots a complete eager v3 package rather than the removed workload-only shape.
- Ran an explicitly bounded Quizzes multi-saga v3 generation with `SEGMENT_COMPRESSED`, recovery cap `20`, and dynamic enrichment disabled. The final package contains 2,000 WorkloadPlans, 12 materializable workloads, 60 eager vectors, and 84 FaultScenarios; the legacy `scenario-catalog.jsonl` is absent.
- Selected persisted FaultScenario `25c0d61a...` (`WorkloadPlan=01c49ae...`, vector `00010`) whose seven persisted actions contain `COMPENSATION -> survivor FORWARD -> COMPENSATION`, then executed it through Docker using only `PACKAGE_PATH` and `FAULT_SCENARIO_ID`.
- Recorded the honest runtime outcome: `PARTIAL_COMPENSATED / DEVIATED`. A zero-bit null-name failure occurred before the assigned slot, immediate no-work fallback compensated that participant, the assigned slot was masked, and the surviving participant committed. All five semantic package hashes remained byte-identical.
- Migrated required and directly linked live verifier documentation to the validated WorkloadPlan/FaultScenario v3 package, workload-linked dynamic sidecars, persisted-action executor, on-demand request path, recovery accounting, v4 report, historical-v2 boundary, and current non-goals. Historical evidence was retained and labeled rather than rewritten as v3.

### Files Changed

- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ApplicationsFileTreeParserSpec.groovy` — include the existing `GroovySetupHelperOwnershipSpec` fixture in exact dummyapp discovery expectations.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DummyappDynamicEnrichmentIntegrationSpec.groovy` — generate a complete eager v3 package and assert all five semantic artifacts remain byte-identical after workload-linked sidecar publication.
- `docs/verifiers-impl/current-state.md` — replace current v2 claims with validated v3 package/executor truth, Quizzes evidence, boundaries, and next work.
- `docs/verifiers-impl/reference/scenario-executor.md` — document package/FaultScenario invocation, no executor vector overlay, on-demand persistence, action semantics, v4 reports, sidecar boundary, and current smoke.
- `docs/verifiers-impl/glossary.md` — make WorkloadPlan/FaultScenario, recovery, conformance, and persisted execution the current terminology; mark ScenarioPlan historical.
- `docs/verifiers-impl/roadmap.md` — align implemented stages/status matrix and remaining gaps with the v3 package and executor.
- `docs/verifiers-impl/evidence.md` — record exact bounded generation/execution commands, counts, ids, actions, lifecycle, report/log paths, hashes, test summaries, and limitations.
- `docs/verifiers-impl/advisor-brief.md` — update the meeting summary, evidence, non-claims, and next decision to current v3 truth.
- `docs/verifiers-impl/thesis-claims-evidence-map.md` — add evidence-backed v3 package/recovery/executor claims and explicit overclaim guards.
- `docs/verifiers-impl/reference/dynamic-enrichment.md` — document the workload-linked v3 sidecar and label broad Quizzes counts as historical v2-era evidence.
- `docs/verifiers-impl/reference/input-recipes.md` — migrate current recipe locations/rejected schema and sidecar semantics while labeling the old smoke historical.
- `docs/verifiers-impl/reference/source-to-input-flow.md` — migrate generation/rejected-artifact terminology to WorkloadPlan/FaultScenario v3.
- `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` — update the end-to-end pipeline and bounded Quizzes execution explanation.
- `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` — mark the v1/v2 artifact contract historical and rejected by current readers.
- `docs/verifiers-impl/decisions/2026-05-12-runtime-input-variant-attribution.md` — add the v3 workload-sidecar migration boundary.
- `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md` — update the current materialized-row term to WorkloadPlan.
- `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md` — clarify the implemented v3 supersession and retained zero-bit fallback.
- `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md` — mark participant-report v3 as historical and v4 as the current action-aware schema.
- `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md` — mark the accepted contract implemented and link the current evidence.
- `issues/2026-07-19-compensation-aware-scenario-catalog/008-end-to-end-evidence-and-live-docs.md` — completion evidence only.

Generated evidence is intentionally under ignored `verifiers/target/compensation-aware-v3-evidence/`, including the package, generation/executor logs, v4 report, before/after hashes, and `verification-summary.json`.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| Dependency/preflight inspection of slices 001–007, current Compose/script/config, S2/S3/S6 inventories, and existing target artifacts | PASS | Dependencies 005 and 007 were already done; recovery cap/performance and removed-overlay boundaries were confirmed before Quizzes. |
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | PASS | Fresh final run: 17 tests, 0 failures/errors; Maven `BUILD SUCCESS`. |
| `cd simulator && mvn test` | PASS | Complete simulator run: 96 tests, 0 failures/errors; Maven `BUILD SUCCESS`. |
| Initial `cd verifiers && mvn test` integration gate | EXPECTED FAIL | Exposed only two stale expectations: missing `GroovySetupHelperOwnershipSpec` discovery and the removed workload-only writer call in dynamic integration. |
| `cd verifiers && mvn -Dtest=ApplicationsFileTreeParserSpec,DummyappDynamicEnrichmentIntegrationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final focused run: 21 tests, 0 failures/errors; complete package/sidecar immutability assertions passed. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | Fresh final run: 14 tests, 0 failures/errors; exact high-cardinality count `118264581564861424`, 20 written, 992 counting states, and fewer than 100 materialized leaves remain asserted. |
| `cd verifiers && mvn test` after fixes | PASS | Complete verifier run: 509 tests, 0 failures/errors; Maven `BUILD SUCCESS`. |
| `docker compose run --rm fault-analysis-scenario-gen-test` | PASS | Containerized verifier Maven suite exited 0. |
| Bounded Docker `fault-analysis-scenario-gen` with `BRUTE_FORCE`, `WRITE_WORKLOADS`, singles disabled, Saga-set cap 2, catalog cap 2,000, input cap 2, schedule cap 4, `SEGMENT_COMPRESSED`, recovery cap 20, dynamic enrichment disabled | PASS | `bounded-quizzes-v3-container.log`; package `quizzes-20260720-091007-712/`; 2,000 WorkloadPlans, 12 materializable, 84 FaultScenarios. |
| Docker `scenario-executor` with the generated manifest and persisted FaultScenario `25c0d61a...`; no execution-time vector | PASS | Exit 0; `execution-report-25c0d61a.json` is report v4 with `PARTIAL_COMPENSATED / DEVIATED`, three measured actions, explicit lifecycle, masked assigned slot, and participants `COMPENSATED + COMMITTED`. |
| SHA-256 before/after comparison over workload, rejected-input, FaultScenario, accounting, and manifest artifacts | PASS | `package.sha256.before` equals `package.sha256.after`; exact five hashes are recorded in `docs/verifiers-impl/evidence.md` and `verification-summary.json`. |
| S2/S6 production-reference scan with exact legacy patterns over `verifiers/src/main`, `verifiers/scripts`, and `docker-compose.yml` | PASS | `LEGACY_EXECUTOR_PRODUCTION_REFERENCE_COUNT=0`; no ScenarioPlan/v2 reader/old artifact/enriched execution/executor overlay surface remains. |
| Live Markdown local-link scan, evidence JSON/hash cross-check, and `git diff --check` | PASS | 25 live Markdown files checked with no missing local links; `V3_EVIDENCE_CROSSCHECK_OK=true`; no whitespace errors. |

The first two attempted Maven invocations from the repository root failed because this repository has no root POM. They were immediately rerun from `simulator/` and `verifiers/`; only successful module-scoped runs are used as validation evidence.

### Acceptance Criteria Evidence

- AC-1–AC-6 and AC-23–AC-24: the inspected Quizzes package has separate linked v3 artifacts, no legacy catalog, exact manifest metadata/counts/hashes, and exact per-computed-vector recovery accounting. Full deterministic writer/reader/accounting suites remain green.
- AC-7–AC-19: full verifier coverage plus the focused recovery suite preserves compensation evidence precedence, eager all-zero/single-point semantics, reverse recovery, survivor-forward preservation, automatic commit boundaries, deterministic cap representatives, positive cap validation, and non-exhaustive BigInteger counting.
- AC-20–AC-22: full on-demand service/CLI coverage remains green for atomic valid multi-fault persistence, invalid-input byte stability, deduplication, collision detection, cap consistency, and rollback boundaries.
- AC-25–AC-42: full executor/simulator coverage remains green; the Docker attempt additionally proves persisted-id-only selection, package materialization/startup, action-aware measured fallback, participant lifecycle, slot masking/skips, v4 conformance, and five-artifact immutability in a real Quizzes context.
- AC-43: `DummyappDynamicEnrichmentIntegrationSpec` links sidecar rows by WorkloadPlan/input identity, embeds no workload/scenario/schedule, and compares all five semantic package bytes before/after enrichment.
- AC-44: dummyapp-first parser/adapter/generator/package/enrichment/accounting/on-demand/executor suites pass in the full 509-test verifier gate and Docker test service; the focused reconciliation tests pass independently.
- AC-45: run `quizzes-20260720-091007-712` records workload/materializable counts `2000/12`, 60 eager vectors, 84 FaultScenarios, recovery cap `20`, selected vector counts `3/3`, exact ids/participants/vector/seven planned actions, three actual actions, lifecycle, `DEVIATED` conformance, `PARTIAL_COMPENSATED`, report/log paths, and unchanged hashes.
- AC-46: required live docs and their directly linked technical references now state current v3 package/executor truth, explicitly label old ScenarioPlan artifacts/evidence historical, cite the new bounded evidence, and retain non-goals for generic/distributed execution, reset, scoring, and search.

### Browser / Manual Evidence

- Browser evidence was not required. Container logs and host-mounted JSON/JSONL/hash artifacts were inspected directly. `verifiers/target/compensation-aware-v3-evidence/verification-summary.json` cross-links the final package, selected scenario, planned actions, measured actions/lifecycle/slots, report, logs, and hashes.

### TDD Notes

- This reconciliation slice used the required full verifier suite as its initial red integration gate. It failed on the two cross-slice stale expectations described above. The smallest expectation changes were made, then the focused 21-test run, full 509-test run, and Docker suite all passed.
- No Quizzes-specific production branch or new single-use end-to-end harness was added. Existing dummyapp fixtures and prior-slice package/on-demand/executor tests already owned the behavior; this slice strengthened the package/enrichment integration boundary and exercised the real Docker path.

### Deviations From Plan

- The first bounded multi-saga `INTERACTION_PRUNED` diagnostic (`quizzes-20260720-090609-170`) wrote 360 workloads but zero eager FaultScenarios because none had every participant materializable. A bounded single-saga diagnostic identified real materializable inputs, and the final evidence run used `BRUTE_FORCE` with a 2,000-workload cap to include materializable multi-saga pairs. Input readiness and production logic were unchanged; this did not expand beyond the explicit workload bound.
- The selected persisted scenario was structurally executable but encountered the allowed meaningful zero-bit runtime deviation from a null course name before the assigned slot. The report is intentionally recorded as `DEVIATED`, not presented as fabricated exact replay.
- Dynamic enrichment was explicitly disabled for the bounded Quizzes run to avoid an expensive unrelated application test batch. V3 workload-sidecar linkage and semantic-package immutability were instead verified in the required dummyapp integration/full/Docker test gates. Historical broad Quizzes dynamic counts are labeled v2-era rather than claimed as fresh v3 evidence.
- Docker generation created the timestamped package directory as root, so the report/log/hash evidence was written to the writable parent `verifiers/target/compensation-aware-v3-evidence/` rather than inside that timestamped directory. The report still references the exact package manifest.

### Review Attempt 01 Remediation

- Fixed only the two blocking live-documentation findings from `review/008-end-to-end-evidence-and-live-docs-review-01.md`.
- `docs/verifiers-impl/reference/scenario-executor.md` now distinguishes direct Java support from the Docker wrapper contract: direct Java accepts a manifest or containing package directory, while Docker `PACKAGE_PATH` must name the explicit `scenario-catalog-manifest.json` regular file, matching `run-scenario-executor.sh`'s `-f` preflight.
- `docs/verifiers-impl/glossary.md` now defines current dynamic join status and `MATCHED_EXACT` against a `WorkloadPlan`, removing the two current “scenario plan” references without changing explicitly historical v2 material.

#### Remediation Files Changed

- `docs/verifiers-impl/reference/scenario-executor.md` — corrected the Docker `PACKAGE_PATH` contract and kept direct-Java directory support distinct.
- `docs/verifiers-impl/glossary.md` — migrated the two current dynamic-join definitions to WorkloadPlan terminology.
- `issues/2026-07-19-compensation-aware-scenario-catalog/008-end-to-end-evidence-and-live-docs.md` — recorded review-attempt-01 remediation evidence only.

#### Remediation Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| Exact `rg` assertions over the Docker reference plus `verifiers/scripts/run-scenario-executor.sh` | PASS | Required direct-Java distinction and explicit-manifest wrapper text are present; the obsolete Docker directory claim is absent; script regular-file guard found at line 16; `DOCKER_EXECUTOR_DOC_CONTRACT_OK=true`. |
| Exact `rg` assertions over current glossary join definitions | PASS | `static WorkloadPlan` and `belonging to the WorkloadPlan` are present; both current lowercase “scenario plan” phrases are absent; `GLOSSARY_V3_JOIN_TERMINOLOGY_OK=true`. |
| Live non-archive Markdown local-link scan | PASS | 25 live Markdown files checked; `LIVE_MARKDOWN_LINKS_OK=25`. |
| `git diff --check` | PASS | No whitespace errors; `DIFF_CHECK_OK=true`. |

No Maven or Docker suite was rerun because this remediation changes documentation only. The full simulator, verifier, and Docker results recorded above—and independently rerun by review attempt 01—remain the source/runtime verification evidence.

#### Remediation Acceptance Criteria Evidence

- AC-46: Docker invocation guidance now matches the actual wrapper preflight, while the separate direct-Java capability remains accurately documented.
- AC-46: canonical current dynamic-enrichment join definitions now use WorkloadPlan terminology; explicitly historical v2 sections were not rewritten.

#### Remediation TDD Notes

- TDD was not applicable to these documentation-only contract corrections. Exact positive/negative text assertions cross-checked the edited claims against the existing runner implementation.

#### Remediation Deviations From Plan

- None. No source, test, runner, archived history, spec, plan, or review report was edited.

### Blockers / Follow-Ups

- None. Review attempt 01's two concrete findings are fixed. Ready for `sp-review-slice`; the slice remains active and was not moved to `done/`.
