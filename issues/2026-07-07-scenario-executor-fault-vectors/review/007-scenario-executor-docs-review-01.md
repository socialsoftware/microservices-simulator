# Slice Review: 007 - ScenarioExecutor Docs

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/007-scenario-executor-docs.md`
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/README.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/reading-guide.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`, `docs/verifiers-impl/evidence.md`, `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/007-scenario-executor-docs.md` `## Completion Evidence`
- Changed files reviewed: `docs/verifiers-impl/reference/scenario-executor.md`, `docs/verifiers-impl/README.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/reading-guide.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/evidence.md`, `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`, `mkdocs.verifier.yml`, relevant archive link retargets
- Prior review reports: None for slice 007
- Commands run by reviewer: `find issues/2026-07-07-scenario-executor-fault-vectors/review -maxdepth 1 -name '007-scenario-executor-docs-review-*.md' -printf '%f\n' | sort`; `rg -n "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**' || true`; `rg -n "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml || true`; `rg -n "CONTEXT\.md" issues/2026-07-07-scenario-executor-fault-vectors docs/verifiers-impl mkdocs.verifier.yml || true`; `[ -e CONTEXT.md ] && echo ROOT_CONTEXT_EXISTS || echo NO_ROOT_CONTEXT`; `rg -n "scenario-executor-poc|ScenarioExecutor POC|POC|generated fault injection" docs/verifiers-impl/*.md docs/verifiers-impl/reference mkdocs.verifier.yml || true`; `rg -n "fault-vector|FAULT_VECTOR|--fault-vector|scenario-id|SCENARIO_ID|SUCCESS|FAULT_COMPENSATED|DRY_RUN|INVALID_FAULT_VECTOR" docs/verifiers-impl/reference/scenario-executor.md docs/verifiers-impl/glossary.md docs/verifiers-impl/current-state.md docs/verifiers-impl/evidence.md`; `./scripts/verifier-docs build`

## Summary

The docs rename, nav update, reference page, command examples, status vocabulary, glossary terms, evidence section, and unsupported-scope wording are mostly present and the MkDocs build succeeds. The slice cannot pass because a live current-path documentation page still frames the execution stage as `narrow executor POC / future generic execution`, directly violating AC-23 and the slice purpose to stop POC framing for the implemented supported path.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Most scoped docs were updated, but `docs/verifiers-impl/verifier-pipeline-plain-explanation.md:39` still says `-> narrow executor POC / future generic execution`. |
| Slice out-of-scope respected | pass | No root `CONTEXT.md` exists; archive edits were limited to link retargeting, which is justified by docs build hygiene. |
| Spec non-goals respected | pass | Reviewed docs keep unsupported multi-saga, TCC, stream/gRPC/distributed parity, compensation-step faults, delay/non-binary impairments, batch/search/scoring/prioritization, generic reset, and impact telemetry explicit. |
| Dependencies done | pass | Dependency slice files `001` through `006` are present under `issues/2026-07-07-scenario-executor-fault-vectors/done/`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-23 | fail | `docs/verifiers-impl/reference/scenario-executor.md` is present and `mkdocs.verifier.yml` points to it; `rg "scenario-executor-poc|ScenarioExecutor POC" ... -g '!archive/**'` found no old-path/live exact-label refs; however broader POC search found `docs/verifiers-impl/verifier-pipeline-plain-explanation.md:39:  -> narrow executor POC / future generic execution`. | AC-23 requires docs to stop calling/framing the implemented path as a POC after the feature lands. This live explainer is not archive material and is listed in the landing page as a thesis/pipeline entry point. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `rg "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'` | pass | No live exact old-path/exact-label references found by reviewer. Archive historical mentions remain. |
| Broader live POC framing inspection | fail | `rg -n "scenario-executor-poc|ScenarioExecutor POC|POC|generated fault injection" docs/verifiers-impl/*.md docs/verifiers-impl/reference mkdocs.verifier.yml` found `docs/verifiers-impl/verifier-pipeline-plain-explanation.md:39` still using `narrow executor POC / future generic execution`. |
| `./scripts/verifier-docs build` | pass | Built successfully to `target/verifier-docs-site` in 1.40s. Material/MkDocs 2.0 future warning was informational and did not fail the build. |
| Manual inspection of `docs/verifiers-impl/reference/scenario-executor.md` | pass | The page documents supported single-saga saga/local scope, default and explicit vector commands, explicit `--scenario-id`/`SCENARIO_ID` requirement for explicit vectors, v2 status vocabulary, exit-code contract, smoke evidence, and limitations. |
| Glossary/context routing | pass | `CONTEXT-MAP.md` routes verifier terminology to `docs/verifiers-impl/glossary.md`; no root `CONTEXT.md` exists. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are docs/navigation focused, plus limited archive link retargets for build hygiene. |
| Existing patterns | pass | Docs use existing current-state/evidence/reference/glossary structure and MkDocs navigation. |
| Test quality | n/a | Docs-only slice; verification is search, manual inspection, and docs build. |
| Regression risk | fail | The stale live POC wording can mislead thesis/meeting readers and contradicts the new current-state/reference pages. |
| Security/data safety | n/a | Documentation-only changes. |
| Change hygiene | pass | Old reference path is removed from live nav and replaced with `reference/scenario-executor.md`; no root `CONTEXT.md` was created. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | A live documentation entry point still frames the ScenarioExecutor stage as a POC. | `docs/verifiers-impl/verifier-pipeline-plain-explanation.md:39` contains `-> narrow executor POC / future generic execution`; this was found with `rg -n "scenario-executor-poc|ScenarioExecutor POC|POC|generated fault injection" docs/verifiers-impl/*.md docs/verifiers-impl/reference mkdocs.verifier.yml`. | Replace this stale POC wording with supported-scope language consistent with `current-state.md` and `reference/scenario-executor.md`, e.g. narrow supported single-saga fault-vector executor path / future generic execution. Re-run the stale-reference search and docs build. |
| non-blocking | `docs/verifiers-impl/evidence.md` still says `Last updated: 2026-06-30` despite adding 2026-07-08 ScenarioExecutor smoke evidence. | `docs/verifiers-impl/evidence.md:3` and `docs/verifiers-impl/evidence.md` ScenarioExecutor fault-vector smoke section. | Update the date for documentation consistency when fixing the blocking POC wording. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` due stale live POC framing in `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`.

## Reviewer Notes

Archive mentions of `ScenarioExecutor POC` are acceptable as historical context. The blocking issue is only the live non-archive pipeline explainer, which is part of the current docs reading path.
