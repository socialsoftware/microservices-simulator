# Decision: keep stable HTML report path and add timestamped archive siblings

Date: 2026-04-21

## Status

Accepted.

## Context

The verifier HTML report was overwriting the previous run. Persisting reports is useful for comparing analysis output over time, but existing Docker/test workflows and references expect a stable latest path such as `analysis-report.html`.

## Decision

Continue writing the stable latest HTML report path and additionally write a timestamped archive sibling with the same rendered content.

Example shape:

```text
analysis-report.html
analysis-report-YYYYMMDD-HHMMSS-SSS.html
```

## Rejected alternative

Rename the stable output to something like `analysis-report-latest.html`.

This was rejected because existing workflows and tests already assume `analysis-report.html` and do not need to change.

## Rationale

- Preserves compatibility with existing consumers.
- Gives historical report snapshots by default.
- Keeps report archiving independent from the later machine-readable scenario catalog.

## Revisit when

- Report retention becomes too noisy.
- Users need cleanup/rotation controls.
- Report generation becomes expensive enough that duplicate writes need optimization.
