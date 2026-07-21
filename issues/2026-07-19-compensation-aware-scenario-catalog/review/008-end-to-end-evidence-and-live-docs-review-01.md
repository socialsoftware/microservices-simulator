# Slice Review: 008 - End-to-End Evidence and Live Docs

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/008-end-to-end-evidence-and-live-docs.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-05-12-runtime-input-variant-attribution.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
- Completion evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/008-end-to-end-evidence-and-live-docs.md` → `## Completion Evidence` (`Status: implemented-awaiting-review`)
- Dependency evidence: slices `001`–`007` under `issues/2026-07-19-compensation-aware-scenario-catalog/done/`, `execution-log.md`, and latest PASS review reports for all seven slices
- Changed files reviewed: the two S8 Spock changes; all changed live docs listed by the slice; the complete feature diff from planning commit `53d2af91` through implementation HEAD `75b39e65`; current executor CLI/options/script/Compose surfaces; recovery high-cardinality test; package reader; saved Quizzes package/report/log/hash evidence
- Saved evidence reviewed: `verifiers/target/compensation-aware-v3-evidence/verification-summary.json`, `bounded-quizzes-v3-container.log`, `executor-25c0d61a-container.log`, `package.sha256.before`, `package.sha256.after`, `execution-report-25c0d61a.json`, and all five files under `bounded-quizzes-v3/quizzes-20260720-091007-712/`
- Prior review reports: none for slice 008; latest reviews for slices 001–007 were reviewed as dependency/feature evidence
- Commands run by reviewer:
  - two initial `mvn test` invocations from the repository root — failed because this repository has no root POM; rerun correctly below
  - `cd simulator && mvn test`
  - `cd verifiers && mvn test`
  - `docker compose run --rm fault-analysis-scenario-gen-test`
  - independent Python inspection of manifest, accounting, 2,000 WorkloadPlan rows, 84 FaultScenario rows, selected workload/scenario, report, and SHA-256 values
  - production-reference `rg --pcre2` scans for removed v2 models/schemas/artifacts, enriched execution, executor vector overlay, and old environment names
  - live Markdown local-link scan
  - `git diff --check`, feature diff/status inspection, and done-path collision check

## Summary

The implementation and saved runtime evidence are credible. Fresh reviewer runs passed the complete simulator suite (`96` tests), complete verifier suite (`509` tests), and Docker verifier-test service (`509` tests). Independent artifact inspection reproduced the bounded Quizzes counts (`2,000` workloads, `12` materializable, `60` computed eager vectors, `84` FaultScenarios), cap `20`, selected compensation interleaving, v4 `PARTIAL_COMPENSATED / DEVIATED` report, and all five unchanged SHA-256 values. The production scan found no v2 executor/overlay surface, and the dummyapp enrichment test now snapshots the complete package.

The slice nevertheless fails its primary live-documentation gate. The Docker executor reference claims `PACKAGE_PATH` accepts a package directory, but the actual Docker runner rejects every directory before the Java executor starts. The canonical glossary also retains current join-status definitions in terms of a “static scenario plan” rather than `WorkloadPlan`, despite AC-46 and the glossary's own v3 terminology rule. These are small edits, but they are user-visible contract defects in a slice whose primary ownership includes live documentation.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Integration, saved evidence, tests, and most documentation are complete, but the Docker invocation contract and two current glossary definitions are not aligned with implementation/v3 terminology. |
| Slice out-of-scope respected | pass | The bounded run did not add reset, scoring/search, distributed parity, Quizzes-specific production branches, or headline-count expansion; the meaningful zero-bit deviation and disabled dynamic enrichment were reported honestly. |
| Spec non-goals respected | pass | No v2 compatibility, eager all-vector expansion, uncomputed all-vector exact total, generic/distributed execution, compensation faults/retries, reset, scoring, or search was introduced. |
| Dependencies done | pass | Required dependencies `005` and `007`, and all other slices `001`–`007`, are under `done/` with latest PASS reviews. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Saved run contains separate WorkloadPlan/FaultScenario JSONL, accounting, and manifest; independent listing found no v2 `scenario-catalog.jsonl`. | Rejected-input diagnostics are also linked. |
| AC-2 | pass | V3 package reader/executor tests pass; exact production scan found no v2 executor model/schema/artifact reader. | Historical docs are labeled as such. |
| AC-3 | pass | S2 attempt-03 review verified the full WorkloadPlan identity matrix and exact numeric round trip; full verifier suite passes. | |
| AC-4 | pass | S3 review verified action/FaultScenario identity closure; selected saved record links one workload, vector, and seven persisted actions. | |
| AC-5 | pass | Prior writer/dummyapp byte-stability coverage remains green in the 509-test suite. | Saved evidence is one bounded run, not the sole determinism proof. |
| AC-6 | pass | Manifest independently inspected: linked schemas/paths/counts/hashes, effective configuration, materializability policy/rows, vector source, and cap `20` are present. | |
| AC-7 | pass | S1 visitor/adapter phase-preservation coverage remains green in the full verifier run. | |
| AC-8 | pass | S1 precedence/effect-free fixture matrix remains green. | |
| AC-9 | pass | S4 eager all-zero coverage and S6 automatic-commit execution coverage remain green. | |
| AC-10 | pass | Exact all-zero/one-hot publication validation and pre-body fault coverage remain green. | |
| AC-11 | pass | Generator/executor tests retain failed-owner suffix diagnostics; saved report shows skipped and masked owner slots. | |
| AC-12 | pass | Recovery generator and exact replay tests retain reverse checkpoint order. | |
| AC-13 | pass | Recovery property/table tests preserve the residual global forward subsequence. | |
| AC-14 | pass | Boundary representative and automatic-commit tests pass; saved survivor commits at its final action. | |
| AC-15 | pass | Multi-fault generation/on-demand/replay coverage passed in the full suite. | |
| AC-16 | pass | Cap default/positive/malformed tests and package pre-mutation checks pass. | Saved package freezes `20`. |
| AC-17 | pass | Below-cap exhaustive recovery fixtures pass; selected Quizzes vector records `3/3`. | |
| AC-18 | pass | Deterministic representative priority, dedup, boundary suffix, and lazy fill coverage passes. | |
| AC-19 | pass | Eager/on-demand per-vector frozen-cap coverage passes; manifest cap is `20`. | |
| AC-20 | pass | S5 valid arbitrary multi-fault persistence coverage remains green. | |
| AC-21 | pass | S5 invalid request/package matrix remains green with byte-stability assertions. | |
| AC-22 | pass | S5 dedup, collision, and deterministic reconciliation coverage remains green. | |
| AC-23 | pass | Accounting independently reports exact decimal `n`/`2^n` layers, 60 computed vectors, and exact `84/84` computed sums. | |
| AC-24 | pass | Saved accounting says `allVectorRecoveryTotalStatus=NOT_COMPUTED` and scopes exact sums to computed vectors only. | |
| AC-25 | pass | Persisted-id executor, action validation, CLI/script/Compose migration, and zero legacy-reference scan pass. | On-demand generation still legitimately accepts a vector as generation input. |
| AC-26 | pass | Preparation-gate tests remain green. | Saved report shows both participants materialized and startup-ready. |
| AC-27 | pass | Exact action replay fixture coverage remains green. | Selected Quizzes attempt intentionally deviated and is not used as exact-replay proof. |
| AC-28 | pass | Assigned pre-body abort/body-not-run coverage remains green. | Saved assigned slot was masked by an earlier zero-bit deviation. |
| AC-29 | pass | One-checkpoint stepwise recovery and no compensation fault-slot coverage remains green. | |
| AC-30 | pass | Body/commit phase matrix remains green; saved survivor reports both body and commit success. | |
| AC-31 | pass | Zero-bit body/commit fallback and survivor-continuation coverage passes; saved Quizzes run demonstrates the body-failure path. | |
| AC-32 | pass | Fallback checkpoint/sub-outcome and package immutability coverage passes; saved run records no-work fallback without rewriting the scenario. | |
| AC-33 | pass | Infrastructure/provider/reflection/config/report failure matrix remains green. | |
| AC-34 | pass | Scheduled/fallback compensation hard-stop coverage remains green. | |
| AC-35 | pass | Simulator retry-marker/no-auto-retry coverage remains green. | |
| AC-36 | pass | Terminal idempotence/no-work coverage passes; saved report emits `ABORTED`, `NO_COMPENSATION_WORK`, then `COMPENSATED`. | |
| AC-37 | pass | Saved report is schema v4 and includes attempt/workload/scenario identity, vector, actions, lifecycle, participants, blockers, and terminal status. | |
| AC-38 | pass | Full test matrix covers exact/deviated/incomplete/absent; saved run truthfully reports `DEVIATED`. | |
| AC-39 | pass | Report action/sub-outcome serialization coverage passes; saved actions include positions, source ids, body/commit outcomes, fault origin, and exception. | |
| AC-40 | pass | Saved report distinguishes unassigned and masked slots and retains participant-local skipped forwards; full matrix remains green. | |
| AC-41 | pass | Lifecycle/body/commit coverage passes; saved report includes abort, no-work, compensated, and automatic commit events. | |
| AC-42 | pass | Independent SHA-256 recomputation matches before/after files and manifest hashes for all five package files. | |
| AC-43 | pass | Updated dummyapp integration writes a complete eager package, snapshots all five files, writes workload-linked sidecars, and asserts no embedded workload/scenario/schedule; full and Docker suites pass. | Fresh broad Quizzes enrichment is correctly not claimed. |
| AC-44 | pass | Fresh reviewer runs: simulator `96/0/0`, verifier `509/0/0`, Docker verifier `509/0/0`; dummyapp-first behavior remains covered across the full suite. | |
| AC-45 | pass | Saved bounded run records `2000/12` workloads, `60` eager vectors, `84` FaultScenarios, cap `20`, selected ids/vector/participants, `3/3` recovery count, compensation-survivor interleaving, v4 actual actions/lifecycle/status, logs/report, and unchanged hashes. | Meaningful zero-bit deviation is explicitly not presented as exact compensation replay. |
| AC-46 | fail | Most live docs are migrated and historical v2 evidence is labeled, but `reference/scenario-executor.md:72` overstates Docker directory support and `glossary.md:86-87` retains current “scenario plan” terminology. | Fix both live-documentation defects before PASS. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Completion evidence records a fresh 17-test pass; the reviewer reran the stricter full suite successfully. |
| `cd simulator && mvn test` | pass | Fresh reviewer run: `Tests run: 96, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| High-cardinality recovery count/cap/performance oracle | pass | Full verifier run executed the 14-case spec; source assertion fixes exact count `118264581564861424`, cap `20`, counting states `992`, and materialized leaves `<100`. |
| `cd verifiers && mvn test` | pass | Fresh reviewer run: `Tests run: 509, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `docker compose run --rm fault-analysis-scenario-gen-test` | pass | Fresh reviewer run exited 0 with `509` tests and Maven `BUILD SUCCESS`. |
| Bounded Quizzes Docker generation | pass | Saved container log exits successfully and reports the exact package path, 2,000 workloads, and 84 FaultScenarios; manifest/accounting independently confirm all effective bounds and counts. |
| Persisted-id Docker execution without overlay | pass | Saved log shows only package path plus FaultScenario id and ends `PARTIAL_COMPENSATED conformance=DEVIATED`; report and actual actions match. |
| Package/accounting/report/hash inspection | pass | Independent parser/hash script reproduced schemas, links, materializability `12/1988`, vector sources `12` all-zero + `48` single-point, exact `84/84`, selected seven actions, report fields, and all five hashes. |
| Production legacy-reference scan | pass | No removed legacy class, v2 schema/exact old catalog, enriched executor path, direct executor vector option/environment, or old catalog environment remains. |
| Live Markdown local-link scan | pass | 25 non-archive live Markdown files checked; no missing local links. |
| Live docs behavior/terminology cross-check | fail | Two user-visible current-doc mismatches remain, detailed in Findings. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | S8 source changes are two narrow integration-test corrections; documentation edits are confined to required current/live pages and directly linked ADR/reference pages. |
| Existing patterns | pass | Dummyapp-first fixtures, complete package writer, sidecar writer, Docker Compose evidence path, and existing Markdown structure are reused. |
| Test quality | pass | The enrichment change verifies complete-package existence and byte immutability rather than only adapting a removed method signature; parser expectation reflects the real fixture tree. |
| Regression risk | pass | Fresh complete module and Docker suites pass; saved Quizzes evidence is bounded and immutable. |
| Security/data safety | pass | No dependency, migration, destructive operation, secret, or new external data surface was introduced; execution output remains separate from protected package artifacts. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. The active slice remains separate from review artifacts. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | The Docker executor reference advertises a package-directory `PACKAGE_PATH` that the Docker runner rejects before Java execution. | `docs/verifiers-impl/reference/scenario-executor.md:72` says “v3 manifest or package-directory path”; `verifiers/scripts/run-scenario-executor.sh:16` requires `[[ -f "$PACKAGE_PATH" ]]`. The Java reader supports directories, but the documented Compose wrapper does not. | Make the Docker section require the manifest file (smallest fix), or change and verify the runner so a directory resolves to `scenario-catalog-manifest.json`. Keep the direct-Java support statement distinct from Docker behavior. |
| blocking | The canonical current glossary has not fully migrated dynamic join terminology to `WorkloadPlan`. | `docs/verifiers-impl/glossary.md:86-87` defines current join status as matching a “static scenario plan” and an input “belonging to the scenario plan”, while lines 21/44 reserve `ScenarioPlan` for superseded v1/v2 and AC-46 requires current WorkloadPlan/FaultScenario terminology. | Replace those current definitions with `WorkloadPlan` terminology and re-run the live-doc link/diff checks. Do not rewrite explicitly historical v2 sections. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — AC-46/live-documentation contract has two concrete blocking mismatches.

## Reviewer Notes

The two repository-root Maven invocations were reviewer command-location mistakes, not product failures; the correctly scoped module commands passed and are the evidence used above. Browser evidence was not required. The selected Quizzes scenario is structurally compensation-interleaving but the measured run deviated before its assigned slot; the docs correctly preserve that limitation rather than claiming exact compensation replay.
