# Broader ImpactV2 qualification review

> Subsequent correction: semantic-lock analysis and the executor completion check were
> incomplete during this campaign. The 34-to-17 projection does not establish equivalent
> recovery; some omitted read-payload checkpoints had real semantic-state rollback.
> Preserve the reports as observations, with fresh qualification in
> `../2026-09-06-semantic-lock-recovery/HANDOFF.md` governing the corrected behavior.

Status: **PASS**. No actionable P0–P2 correctness findings remain within the approved
qualification boundary.

## Static review

- `qualification.py:81-96` resolves the exact two-Saga, five-step benchmark and rejects
  any schedule containing an event action. The issue-local regression check exercises a
  copied current workload with an injected event and observes the expected rejection.
- `select-broader.py:20-60` freezes one materializable single-participant workload per
  eligible Saga plus the maximum-event shape, then pairs persisted zero and last-slot
  vectors without reading outcomes. Re-running the selector produced the frozen 60 rows,
  30 pairs, 26 eligible of 68 Saga types, and the same package manifest hash.
- `summarize-broader.py:17-101` verifies the linked package hash, selected role/vector,
  report joins, package identity, distinct-object counts, and nullable score semantics.
  Missing artifacts are retained as `MISSING_REPORTS` with null ImpactV2 fields and an
  incomplete campaign result; no status or score is fabricated.
- `qualification.py:145-218` accounts for all 17 current schedules and all 34 historical
  rows. Four keys match exactly; the remaining rows are explicitly marked as a
  conservative-action projection and are not treated as exact replays or scoring input.

## Runtime evidence

The final run under `verifiers/target/impact-v2-broader/run-01/` provides 77 fresh
ScenarioExecutor/JVM/H2 attempts. Independent read-only checks validated every
execution, V1, and V2 report against its selected workload, scenario, vector, and
attempt identity.

- Broader: 60/60 reports present; 47 `COMPLETE`, 4 `PARTIAL`, and 9 `INVALID`, with no
  `UNAVAILABLE` rows. All 30 control/fault pairs are retained; 21 have two complete
  assessments.
- Historical family: 17/17 reports present; 12 `COMPLETE` and 5 `PARTIAL`. The weighted
  34-row projection preserves the 19/15 historical label split and reproduces the same
  final-snapshot cross-check. The old labels remain post-assessment comparison metadata.
- The package before/after diff is empty, the prepared-build source hash matches the
  captured runtime-source validation, and the frozen selection hash matches the package
  manifest. Generated Python bytecode and post-processing scripts are excluded from the
  runtime-source comparison.
- The issue-local checks pass: `python3 validation-checks.py` reports 5 tests successful.

The 17 application-wrapper rejections are retained separately from the authoritative
ordinary ScenarioExecutor executions. Their application-specific observation is
reported unavailable; the final-snapshot broken-reference check is explicitly labeled
as using the same ImpactV2 evidence. Partial/invalid attempts, unsupported cyclic
projections, and positive controls remain visible in the results and are not converted
to zero or treated as defects in this qualification review.
