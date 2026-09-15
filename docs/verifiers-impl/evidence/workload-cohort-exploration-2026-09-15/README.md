# Fixed-workload cohort exploration, 2026-09-15

The subsequent [integral-input follow-up](../integral-input-materialization-2026-09-15/README.md)
fixes Candidate 6's materialization failure. The results below retain the exploration's
original runtime and decisions; they are not silently replaced by the follow-up.

## Result

This exploration did not find an eligible fixed workload with a domain of 100
or more unique application executions. The largest source-backed workload that
both materialized and passed a fresh no-fault execution in this shortlist has exactly 72
candidates, but its 40-execution random sample was flat at fitness zero. The
only workload in this exploration with both zero and positive observations has
exactly 44 candidates and also produces incomplete enabled evidence for some
faults. It is useful as a small calibration workload, not as the requested
100--200-execution comparison horizon.

The largest counted domain has 156 candidates. It is not runnable: source
setup succeeds, then the `CreateTournamentFunctionalitySagas` input fails at
argument 4 with `MATERIALIZATION_EXCEPTION: argument type mismatch`. Treating
156 as an available search horizon would therefore overstate the executable
domain.

## Selection contract

The runtime shortlist was chosen from existing generated packages after the
domain inventory and before each candidate-specific qualification. A candidate
needed source-derived inputs and source setup, no manual
prerequisite provider, a count through the ordinary on-demand generator,
successful fresh no-fault execution, complete enabled fitness evidence, useful
zero/positive variation, and a domain substantially larger than the prior
29-case experiment. Counts below are for one fixed workload: participants,
inputs, forward order, and event deliveries remain fixed; only canonical fault
coordinates and generated recovery schedules vary.

The count helper requested every canonical fault vector with recovery cap 500.
Every completed count reported `anyTruncated=false`, so those counts are exact
for that fixed workload rather than merely cap-limited. Generator wall time is
reported because domain construction is a material cost even when executions
are cached.

## Inventory

| Candidate | Fixed workload story | Vectors | Exact candidates | Generator wall time | Runtime qualification | Decision |
|---|---|---:|---:|---:|---|---|
| 1 | Update Topic + Update Tournament, no delivered event | 18 | 35 | 36.36 s | Not repeated; same small family | Exclude: too small |
| 2 | Update Topic + Update Tournament, route 1 | 18 | 36 | 36.35 s | Existing known positive point | Exclude: too small and known-positive-biased |
| 3 | Update Topic + Update Tournament, route 0 | 18 | 36 | 27.05 s | Not repeated; same small family | Exclude: too small |
| 4 | Update Topic + Update Tournament, late route 1 | 18 | 62 | 50.14 s | Existing recovery positive has incomplete residual evidence | Exclude: too small and incomplete evidence |
| 5 | Add Student + Add Participant + Solve Quiz Async | 72 | 72 | 596.30 s | No-fault complete, `I=0`, `A=0`; 40 random executions all scorable zero | Exclude: flat sampled objective and below horizon |
| 6 | Anonymize Student + Get Course Execution + Create Tournament | 48 | 156 | 575.54 s | No-fault materialization failed after successful setup | Exclude: not executable |
| 7 | Find Tournament + Remove Tournament + Update Tournament | planned 48 | not countable | stopped after 18 rejected requests | Every request rejected as `WORKLOAD_NOT_MATERIALIZABLE` | Exclude: not a domain |
| 8 | Update Topic + Update Tournament, early route 1 | 18 | 44 | 51.93 s | No-fault process completed, but read coverage was partial; exhaustive map has zero, positive, and unavailable observations | Keep only as small calibration |

Candidate IDs, package paths, exact counts, seeds, result aggregates, and raw
artifact paths are retained in [inventory.json](inventory.json).

## Runtime observations

Candidate 5 uses three different Saga shapes and is the best executable size
found in the existing packages. The no-fault control was complete for the four
enabled v1 criteria. The random sample budget of 40, seed 1105, population 8,
mutation 0.3, and stall limit 1000 was declared before outcomes. All 40 unique
application executions completed and scored zero. The run made 58 proposals,
discarded 18 duplicates without application execution, issued 40 generation
requests, spent 258.89 seconds in generation, and took 1,297.15 seconds wall
clock while sharing the two-container bound with Candidate 8 for part of the
run. Lost copied update was explicitly disabled because this older package
supplies no inferred copy contracts; enabling it would make fitness
unavailable rather than test the candidate's intended evidence scope. The flat sample does not prove all 72
points are zero, but it is enough to reject this workload for a costly first
GA/random study.

Candidate 8 is source-derived from `update topic and tournament successfully`
and delivers route 1 immediately after the topic update. Its no-fault process
completed with persistent impact zero and complete lost copied update evidence,
but compensated-read coverage was partial because a producer action was
unproven. The control therefore does not meet the complete-enabled-evidence
selection requirement. All five v2 criteria were nevertheless retained with
unit weights for the exploratory map; incomplete evidence was reported as
unavailable rather than diluted or coerced to zero.

The random run budget was fixed at the exact 44-candidate domain, seed 1108,
population 8, mutation 0.3, and stall limit 1000. It executed all 44 unique
candidates: 22 scored zero, 17 scored one, and 5 had unavailable fitness. The
17 positives were failed-operation residual findings; lost copied update,
deleted dependency, unresolved delivered event, and compensated read had no
positive counts in this map. Forty-two executions had executor status
`COMPLETE` and two were `PARTIAL`; fitness was still unavailable for five
because compensated-read coverage was partial, including some otherwise
complete executions. The sampler made 299 proposals, rejected 255 duplicates
without application execution, issued 18 generation requests, spent 47.14
seconds in generation, and took 1,060.01 seconds wall clock. This is an
exhaustive map of the fixed 44-candidate domain even though the recorded stop
reason is `BUDGET`: budget and exact domain size were equal. The observed
variation makes Candidate 8 useful for regression and calibration, while its
ceiling and 39/44 scorable fraction disqualify it as the larger comparison
cohort.

Candidate 6 demonstrates why static domain size is not sufficient. Its source
setup ran successfully, including its retained bindings, but participant p3
could not materialize the source-derived Create Tournament argument. Candidate
7 is weaker still: the ordinary generator rejected all 18 requests made before
the count was stopped, citing unresolved p1/p2 arguments and a not-ready
property receiver for p3. Neither count is reported as executable coverage.

## Attempt to construct a larger source-backed workload

Two bounded generation probes were made without changing production source.
Selecting Update Tournament and Remove Tournament source inputs produced no
workload with source setup for that exact pair. Selecting four exact inputs for
Add Student, Add Participant, Solve Quiz Async, and Create Tournament produced
390 workloads before filtering, but none combined all four selected inputs
with source setup and positive static materializability. These failures are
retained under `generated-update-remove/` and `generated-four-saga/` in the raw
artifact root. They show that simply increasing the requested participant cap
does not create a valid larger workload from the currently extracted setup
bindings.

## Recommendation

Do not force a nominal 100--200-execution GA/random comparison using the
currently qualified shortlist. This exploration does not exhaust the global catalog. A longer seed cannot increase a 44- or 72-candidate
domain, and duplicate proposals do not count as new application executions.

The most direct next cohort target is the 156-candidate Candidate 6 shape,
after the source-derived `CreateTournamentFunctionalitySagas` argument can be
materialized through the normal verifier contract. Requalify it in this order:

1. Repeat the fresh no-fault control and require a valid, scorable execution.
   Interpret its result; zero is not an eligibility requirement. A no-fault
   concurrent history can itself reveal an anomaly.
2. Run a deterministic exhaustive or 156-unique random map with all applicable
   criteria explicitly enabled; report null fitness rather than coercing it.
3. Continue only if the map contains both zero and positive scorable results
   and an acceptable scorable fraction.
4. Then compare GA and random at 100 unique application executions with the
   same five declared seeds, runtime, weights, caps, and fixed workload. Use a
   150-execution sensitivity horizon only if the 156-point map shows that it
   remains meaningful.

Candidate 8 should accompany that future study only as a fully mapped small
calibration case, with its 5 unavailable fitness observations retained. It must
not be pooled with Candidate 6 to claim a single larger per-run search space.

## Reproduction and provenance

Retained `config.json` files beside each run or domain count replace the original
temporary config paths. For example, `domain-counts/candidate-5/config.json` and
`candidate-8-random-1108/config.json` under the raw root preserve the exact selections.
The core commands were:

```sh
python3 verifiers/experiments/workload-cohort-exploration/count_domain.py \
  --config /tmp/candidate-N-config.json \
  --output verifiers/target/workload-cohort-exploration-01/domain-counts/candidate-N

python3 verifiers/experiments/fixed-workload-ga/run.py control \
  --config /tmp/candidate-N-run-config.json \
  --output verifiers/target/workload-cohort-exploration-01/candidate-N-control

python3 verifiers/experiments/fixed-workload-ga/run.py run \
  --config /tmp/candidate-N-run-config.json \
  --output verifiers/target/workload-cohort-exploration-01/candidate-N-random-SEED \
  --control verifiers/target/workload-cohort-exploration-01/candidate-N-control/control.json
```

The fixed runtime descriptor is
`verifiers/target/lost-copied-update/integration-03/runtime.json`. It freezes
Docker image
`sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282`,
the compatible class/source overlay hashes, and lost-copy agent
`b3e8340380fee3976a650f7b55251fc1981403cb1153eedead0bff4dc9d9fdf7`.
Each application execution used a fresh process and database through the
existing fixed-workload runtime. This exploration made 84 search executions and three control attempts, including
the failed materialization control. No new GA arm was executed: these are random
pilot/mapping runs, not a new effectiveness comparison. No business code, verifier,
detector, search algorithm, or paper was changed.

Raw evidence is under
`verifiers/target/workload-cohort-exploration-01/`. Each completed domain count
contains its copied package, request commands and logs, `requests.json`,
`scope.json`, `source-package-hashes.json`, and `domain-count.json`. Each
runtime run contains its config, copied package, source hashes, request logs,
fresh attempt directories, replay descriptors, progress, and final summary.

## Limits

This exploration qualifies fixed forward schedules. The GA does not choose the
forward order, participant inputs, or event-delivery placement. Exact counts
therefore describe the current fixed-workload search contract, not the global
scenario space. Candidate 5 was sampled rather than exhaustively mapped; its
40 zeros do not prove that its 32 unexecuted candidates are zero.
Candidate 7 was deliberately stopped after repeated generator rejection, so it
has no domain-size claim. The proposed Candidate 6 comparison remains
conditional on materialization, complete enabled evidence, and observed
fitness variation; this evidence does not recommend changing detector weights.
