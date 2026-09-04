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
