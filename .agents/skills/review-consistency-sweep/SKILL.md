---
name: review-consistency-sweep
description: Interpret completed consistency-testing sweep reports against application code and catalog evidence. Use only when user explicitly asks to review, analyze, triage, or explain consistency sweep results, findings, anomalies, or campaign-summary.json. This can require substantial report and code reading. Do not invoke implicitly during onboarding or after merely suggesting a sweep.
---

# Review Consistency Sweep

Review reports progressively, connect observed schedules to application behavior, and separate likely product defects from catalog defects, expected rejections, engine limitations, and inconclusive evidence. Stay read-only unless user separately asks for fixes.

## Required reference

Read [report-semantics.md](references/report-semantics.md) before interpreting status or anomaly fields.

## 1. Resolve campaign

1. Anchor to repository root and read governing `AGENTS.md` files.
2. Resolve target application and reports directory. Default is `applications/<app>/target/consistency-reports`.
3. Require `campaign-summary.json`. If missing, report exact expected path and stop.
4. Read application's `catalog-coverage.yaml`, catalogs provider, inter-invariants provider, and sweep class when present.
5. Remain read-only. Do not patch code, catalogs, or docs during review.

## 2. Triage summary first

Read `campaign-summary.json` before per-run reports. Report:

- seed, iterations, duration, and total runs;
- functionality and planned-pair counts per catalog;
- findings by group and status;
- anomaly counts by group, including groups with zero findings;
- unexplored groups or empty catalogs.

Do not equate `findings: []` with clean execution. Summary may contain anomaly counts that current finding predicate does not promote. Search per-run JSON without loading it all for non-promoted statuses `ISOLATION_ANOMALY`, `ANOMALY_ANALYSIS_FAILED`, and `INTERDEPENDENCY_RESOLUTION_FAILED`; count matching files and include them in triage.

## 3. Sample reports progressively

Group evidence by catalog, functionality pair, statuses, anomaly types, and exception-step signature. Read smallest representative set first:

1. One report for each distinct finding signature.
2. One report for each anomaly-only group/type absent from findings.
3. One report for each non-promoted status signature found by search.
4. Additional reports only when schedules or outcomes differ materially.

State sample size and skipped report count. Never dump all JSON into conversation.

## 4. Trace against code

For each representative:

1. Resolve functionality IDs through coverage manifest and provider factory.
2. Inspect saga steps, commands, compensations, event handlers, aggregates, repositories, and explicit invariants involved.
3. Reconstruct causal schedule: reads, writes, commits, aborts, compensation, and invariant checks.
4. Compare concurrent behavior with each functionality's intended solo behavior and documented business rules.
5. Check whether catalog state, stale injected helper, missing handle, invalid precondition, or uncertain invariant could explain result.

Treat code and reports as evidence. Mark inference explicitly.

## 5. Classify every reviewed signature

Use exactly one primary classification:

- `probable-consistency-defect`
- `expected-business-rejection`
- `catalog-or-invariant-defect`
- `engine-limitation-or-instrumentation`
- `inconclusive`

Give confidence (`high`, `medium`, `low`), decisive report path, supporting source paths, impact, and next validation step. Never call anomaly a product defect without code evidence.

## 6. Deliver verdict

Lead with whether campaign provides actionable evidence. Summarize counts, then prioritized signatures. Distinguish observed facts from inferred cause.

When reproduction helps and generated sweep supports properties, show:

```bash
mvn test -Pconsistency-sweep -Dconsistency.masterSeed=<campaign-seed> -Dconsistency.iterations=<campaign-iterations>
```

This replays whole campaign, not one group. Suggest code or catalog changes but do not implement without explicit request.
