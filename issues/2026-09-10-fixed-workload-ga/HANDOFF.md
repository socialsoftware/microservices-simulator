# Fixed-workload GA handoff

## Delivered

A local CLI selects one existing WorkloadPlan and searches fault/recovery choices using
GA or a matched seeded random policy. Available COMPLETE ImpactV2 I drives the first GA;
the existing read-exposure A remains independently validated and reported. There are no
new detector rules, combined weights, application changes or production Java changes in
this package.

Implementation: `verifiers/experiments/fixed-workload-ga/`.

- `search.py`: per-Saga fault coordinates, conditional recovery choice, fitness-dependent
  parent selection, crossover/mutation, elitist population, duplicate cache and stop accounting.
- `runtime.py`: package selection/copy and invariant checks, on-demand Java requests,
  fresh Docker attempt adapter, existing I validation and independent A report joins.
- `run.py`: prepare/control/run/summary/replay commands, control scope/evidence checks,
  incremental lineage and final result summaries.
- `qualify.py`: the predeclared Quizzes selection, frozen current runtime and 53-attempt protocol.
- `test_search.py`, `test_runtime.py`: policy, package boundary, failure and replay tests.
- `README.md`, `RESULTS.md`: usage, sampling distribution, scope and concrete measured results.

Each real attempt saves the exact package revision for replay. On-demand requests can
change an arm's scenario/request/accounting files while input/setup/workload content stays
fixed. Historical scenario IDs with the same executable content share the cache key.
The original package and other arms are not mutated.

## Verification

The 17 search-layer tests passed, including fitness sensitivity for GA versus random,
null-fitness exclusion, equal-score exploration, canonical masking, aliases/cap boundaries,
missing setup, package drift, unavailable A with available I, invalid report joins,
process failures and exact replay after the arm package evolves. The existing 13 batch
and 13 search-baseline tests also passed. Production Java did not change, so no Java
contract test rerun was required for this package.

A separate real on-demand generator smoke requested `010001` on the fixed update/read
workload: two new scenarios were persisted; a second Java request returned the same
candidates with zero additions. This exercises package mutation/dedup without launching
the application or consuming the 53-attempt qualification budget. Evidence:
`verifiers/target/fixed-workload-ga/generation-smoke-01/validation.json`.

All 53 application assessments and read-coverage reports passed. Search found GA/random
positive counts 4/4 for seed 11 and 6/2 for seed 29; the latter arm executed only
initialization/random fallback after duplicate offspring, so it supplies no evolutionary
advantage claim. Seed 11 executed three new crossover children. The three update/read
witnesses retained I=1 and A=1/0/0. Runtime/package/report checks passed.

The application qualification and final integrity checks are summarized in
[RESULTS.md](../../verifiers/experiments/fixed-workload-ga/RESULTS.md). Raw reports,
configuration, request revisions, replay snapshots and search-source copies live under
`verifiers/target/fixed-workload-ga/qualification-01/`.

## Findings and practical boundaries

The no-fault benchmark is a valid measured domain rejection: RemoveTournament completes
before AddParticipant tries to use that Tournament. The inscription fails, execution
conformance is DEVIATED, recovery completes sufficiently for a COMPLETE I=0 assessment.
Qualification requires a valid assessment, not that every application operation succeeds.
The update/read no-fault control succeeds with EXACT conformance and I=0/A=0.

The benchmark has a small cap-limited domain and a coarse I landscape. Positive scenarios
may repeatedly expose the same partial-removal defect. The first experiment establishes
an adaptive implementation and comparable measurement; broader search claims require
more independently selected workloads, seeds and budgets.

The generic policy/package tests use synthetic evaluators and minimal exported package
shapes. Actual Java on-demand persistence and application integration are exercised with
Quizzes; no new dummyapp Java behavior was needed. This is narrower than claiming a fresh
full dummyapp application qualification.

## Documentation and remaining work

Canonical current state, roadmap and the documentation index now point to the search
command. The Portuguese meeting note receives only a short result/context update.
Choose the final I/A priority rule with the advisor, then predeclare a broader evaluation
cohort and budgets. Focused generation from a user-selected Saga set, further anomaly
families, cross-workload allocation and RL remain separate work.

Work stayed in the existing checkout. No branch/worktree, commit, merge, PR or push was
created. Inherited source/document changes and the personal note were preserved.

## Completed follow-up — Discovery speed with current I

The user subsequently approved completing the current reference and evaluating discovery
speed before changing feedback. `discovery.py` froze the follow-up protocol, reused 21
hash-verified reference keys, executed eight missing keys, and ran six independent
GA/random arms (seeds 11/29/47, budget 29, unchanged population/operators). All 182 new
application attempts completed with available I and read coverage complete within scope.
The reference is 15 I=2 / 14 I=0; it is never supplied as search feedback.

`discovery_analysis.py` validates the map and new report joins, per-attempt snapshots,
parent chronology, fresh attempt identities, runtime/package hashes and metric agreement.
It produces discovery curves and execution counts to 8/12/15 positives, preserving
unmet targets and stops. Its five new tests passed; the complete search-layer suite now
has 22 passing tests. The figure was rendered and visually inspected; docs build and
whitespace checks passed.

GA/random reached eight positives at 20/18, 15/20 and 20/22 executions; twelve at 26/25,
24/25 and 26/26. All reached fifteen at execution 29. Only 14 of 63 post-initialization
GA evaluations were new crossover children (ten positive); 49 used random exploration.
Results are mixed; operator novelty handling is a concrete follow-up to discuss separately
from feedback changes. No tuning or scoring changes were absorbed into this evaluation.

See [DISCOVERY-RESULTS.md](../../verifiers/experiments/fixed-workload-ga/DISCOVERY-RESULTS.md)
and the canonical discovery evidence page. Raw artifacts are retained under
`verifiers/target/fixed-workload-ga/discovery-01/`; the complete map, audited summary and
PNG/SVG/PDF curves are retained in `docs/verifiers-impl/evidence/ga-discovery-2026-09-10/`.
