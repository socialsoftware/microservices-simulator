# Parent review

Reviewed the retained outputs after the Sol Medium exploration finished. Independently
checked report hashes, joins, recomputed configured scores and candidate-key uniqueness
for all 84 search executions. Results reproduce 40 zeros in the first sample and
17 positives / 22 zeros / 5 unavailable in the second. Three additional control attempts
include the materialization failure. No new GA arm ran.

Corrected the recommendation: a no-fault concurrent control must be valid and scorable,
but need not score zero. Corrected scope: this was an adaptive exploratory shortlist,
not an exhaustive global catalogue or a pre-frozen publication cohort. The 44-candidate
run exhausted its candidate set but five unavailable scores prevent a full binary
reference map. Its execution count and stop reason remain as emitted (budget 44).

The candidate-6 blocker is at materialization of CreateTournament argument 4 after
successful setup. Its exact internal type-conversion cause is not established here.
Investigating that boundary is a proposed next task; there is no claim that fixing it
will yield positive observations or a useful search landscape.

Canonical documentation links this evidence and keeps the larger comparison pending.
Production code, operators, weights and paper prose were not changed. The helper scripts
are research reproduction tools, not a new production generation entry point.
