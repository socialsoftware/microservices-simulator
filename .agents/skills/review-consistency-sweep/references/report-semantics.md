# Report semantics

## Layout

Default report root is `target/consistency-reports/`.

- `campaign-summary.json` contains application, master seed, iterations per group, duration, catalog summaries, group summaries, anomaly counts, and findings.
- Per-run files are under `<catalog>/<group>/test-report-NNNNN.json`.
- Finding entries point to their per-run report path.

## Findings are not all anomalies

Current `TestDriver.isFinding()` promotes runs containing any step exception or these statuses:

- `INTERNAL_SYSTEM_EXCEPTION`
- `CRITICAL_STEP_FAILURE`
- `INTER_INVARIANT_VIOLATION`
- `EXECUTION_LIMIT_EXCEEDED`

Actual anomaly types (`DIRTY_READ`, `NON_REPEATABLE_READ`, and `WRITE_SKEW`) are separately counted in group summaries. Statuses `ISOLATION_ANOMALY`, `ANOMALY_ANALYSIS_FAILED`, and `INTERDEPENDENCY_RESOLUTION_FAILED` are not alone sufficient for current finding predicate unless another promoted status or exception exists. Search per-run reports for these statuses, and inspect group `anomalyCounts`, even when findings list is empty.

## Interpretation rules

- Exception can be expected business rejection, not consistency defect.
- Inter-invariant violation is strong evidence only when invariant itself is explicitly documented and correctly implemented.
- Compensation failure may expose product defect, invalid adversarial setup, or missing test dependency. Trace all three.
- Isolation anomaly identifies execution-history property, not automatically user-visible invariant violation.
- One schedule is evidence of possibility, not prevalence.
- No finding after finite randomized iterations is absence of observed evidence, not proof of consistency.
- Planner explores pairs only. Cross-catalog and higher-order interactions are outside campaign.
- Solo profiling observes one conditional path; planner may miss conflicts absent from that path.

## Review priority

1. Internal and critical failures.
2. Confirmed inter-invariant violations.
3. Failed or stranded compensation.
4. Repeated exceptions under specific schedule.
5. Anomaly-only groups.
6. Coverage gaps and zero-group catalogs.

## Review report

Write one report per reviewed campaign at:

```text
docs/reviews/consistency-sweep-<app-slug>-<YYYY-MM-DD>.md
```

Start with campaign provenance and triage totals. Then list every reviewed signature under one of these sections:

- confirmed defects;
- candidates needing more evidence;
- expected, catalog, engine, or inconclusive outcomes.

Give every confirmed defect and candidate a trace ID stable within that report:

```text
CS-<APP>-NNN
```

For example, `CS-QF-001`. A later repair refers to this ID and report path; it does not rely on report line number or raw report filename alone.

Each trace-ID record contains:

- title, classification, and confidence;
- an `In brief` explanation: concurrent interaction, harmful final outcome, violated application rule, and immediate consequence when relevant;
- optional likely repair direction when code evidence supports one; describe ownership or semantic boundary to restore, not speculative patch;
- decisive per-run report path and campaign seed, catalog, group, and run coordinates;
- observed schedule and final state;
- expected rule or postcondition, with supporting code or domain evidence;
- relevant source paths and clearly marked cause inferences;
- optional application impact when it adds information beyond failed postcondition;
- next validation or reproduction step.

Do not call raw anomaly evidence a defect until code and rule analysis supports classification.

Use relative Markdown links to decisive reports and source files when available. Keep `In brief` before schedule detail and code evidence.
