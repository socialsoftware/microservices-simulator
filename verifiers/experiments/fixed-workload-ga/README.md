# Fixed-workload genetic search

The [weighted-fitness qualification](../../../docs/verifiers-impl/evidence/weighted-fitness-2026-09-15/README.md)
checks the configurable criteria with 43 retained cases and 28 fresh executions.
The historical [discovery-speed results](DISCOVERY-RESULTS.md) compare GA/random against a
complete current reference. The [first qualification](RESULTS.md) establishes integration.

Select one existing WorkloadPlan and search its fault/recovery alternatives. The command
uses the existing on-demand Java generator and ordinary isolated ScenarioExecutor. It
collects COMPLETE ImpactV2 affected-object count **I** and Saga read exposure **A** with
its declared scope, coverage and findings. Fitness can use the historical distinct-object
count I or an explicit weighted sum of the three persistent criteria and compensated-read
exposures. The explicit `weighted-criteria-v2` policy additionally supports lost copied
updates. The weights express a user preference, not measured business severity.

## What the user chooses

A JSON configuration contains:

- `manifest`: absolute path to an existing current package manifest;
- `workload`: exact WorkloadPlan ID;
- `runtime`: frozen Docker image, classpath, file hashes, Java options and executor arguments;
- `recoveryCap`: positive cap per requested vector;
- `strategy`: `ga` or `random`; `seed`; positive `budget` of application executions;
- optional `fitness` (historical I policy; see below);
- optional `population` (8), `mutation` (0.3), `stallLimit` (100), `timeout` (180 seconds).

The runtime is a prepared, source-qualified application build. The qualification driver
shows how to freeze the retained Quizzes build and current overlay; the search does not
compile or infer application setup. All mounted runtime files are read-only. Every real
attempt uses a new container, JVM and application database. Runtime arguments, including
the qualification's fixed clock and enabled observation hooks, are shared by both arms.

From the repository root:

```bash
# Inspect participants, input IDs/setup, normal order, event horizon and inherited limits.
python3 verifiers/experiments/fixed-workload-ga/run.py prepare \
  --config /absolute/path/config.json --output verifiers/target/my-search/inspection

# Qualify the no-fault control separately; its execution does not consume search budget.
python3 verifiers/experiments/fixed-workload-ga/run.py control \
  --config /absolute/path/config.json --output verifiers/target/my-search/control

python3 verifiers/experiments/fixed-workload-ga/run.py run \
  --config /absolute/path/config.json --control verifiers/target/my-search/control/control.json \
  --output verifiers/target/my-search/ga-11
```

Every output directory must be new. Each command creates its own package copy; `prepare`
is an inspection, not a resumable execution session. A comparison arm can reuse the same
qualified control only when workload, source package, runtime and recovery cap agree.
Change strategy/seed and use another output directory to run the comparison arm.

## Configurable fitness

Omitting `fitness`, or setting `{"policy": "complete-impact-v2-object-count"}`, preserves
historical I-only selection. The weighted alternative requires all four weights explicitly:

```json
"fitness": {
  "policy": "weighted-criteria-v1",
  "weights": {
    "DELETED_DEPENDENCY": 1,
    "FAILED_OPERATION_RESIDUAL": 1,
    "UNRESOLVED_DELIVERED_EVENT": 1,
    "COMPENSATED_READ_EXPOSURE": 1
  }
}
```

The first three counts are distinct affected objects **within each criterion**. The fourth
is the existing distinct producer/reader/version exposure count, including compensated
creations and updates. Weights are finite, nonnegative numbers, with at least one positive;
zero excludes that criterion from selection. Unknown names and missing weights are errors.
Weights stay fixed during a run. There is no automatic normalization or recommended
business exchange rate; unit weights above illustrate configuration.

The score is the sum of weight × criterion count. An object matching two persistent
criteria contributes to both terms, while the separately reported I still counts that
object once. Neither score nor positive-scenario counts represent independent defects.

Only complete counts from enabled criteria enter the sum. Category completeness is supplied
by the existing ImpactV2 assessor, including relevant collector gaps; a partial category's
positive count remains visible as an observation, not a complete scoring value. An enabled
read criterion requires COMPLETE execution validity and COMPLETE_WITHIN_SCOPE collection
with no gaps. An incomplete or unavailable *disabled* criterion does not block other complete criteria. Invalid
execution/report outcomes never receive a weighted score. Numerical overflow also yields
an unavailable score with an explicit reason. Existing accepted execution-conformance
rules are unchanged. Null scores consume execution budget but never enter the population.

The score affects parent selection, population retention and discovery summaries. It does
not change generation, fault injection, detector predicates or random-policy sampling.
`bestI` and `positiveIScenarios` preserve object-count interpretation; `bestScore`,
`bestSoFar`, `firstPositive` and `positiveScenarios` refer to the selected fitness policy.
Comparisons must use the same policy and weights on both arms.

### Five-criterion policy

The integrated copied-update detector is an optional fifth component. Use the versioned
policy below; old four-weight configurations remain valid and unchanged:

```json
"fitness": {
  "policy": "weighted-criteria-v2",
  "weights": {
    "DELETED_DEPENDENCY": 1,
    "FAILED_OPERATION_RESIDUAL": 1,
    "UNRESOLVED_DELIVERED_EVENT": 1,
    "COMPENSATED_READ_EXPOSURE": 1,
    "LOST_COPIED_UPDATE": 1
  }
}
```

The fifth count is one proved overwriting committed version per aggregate per attempt,
with overwritten fields grouped inside the finding. It covers the supported constructor
copy chain, including normal and recovery writes, rather than every possible lost update.
The legacy I and read A report fields retain their meanings.

The frozen worker descriptor supplies `lostCopiedUpdateAgent` (a `path` and `sha256`) and
`lostCopiedUpdateSourceRoot` to enable observation with source-verified copy contracts.
The package supplies `copy-contracts.json`. The ordinary Compose executor alternatively
accepts `LOST_COPIED_UPDATE=true`. See the
[integration results and launch scope](../../../docs/verifiers-impl/evidence/lost-copied-update-2026-09-15/README.md).

A positive fifth weight requires a valid, complete copied-update report with no coverage
gaps. An old report without it cannot be treated as zero. Zero disables that component's
contribution and coverage requirement. Unit weights are an example, not a recommended
severity calibration or an assumption that the five counting units are equivalent.

A completed search can be revalued without executing the application:

```bash
python3 verifiers/experiments/fixed-workload-ga/run.py rescore \
  --results /absolute/path/search/results.json \
  --fitness /absolute/path/fitness.json \
  --output verifiers/target/my-rescore
```

Here `fitness.json` contains the policy object itself, without the enclosing `fitness`
key. The command verifies retained report hashes, package snapshots and report joins,
then writes a separate `rescore.json` with source references. It preserves original reports.
This revalues the already measured sequence; evaluating how weights change search decisions
requires another search run. Neither RL allocation nor new anomaly detectors are included.

## Search choices

For each Saga, choose no fault or one faultable step. Later fault bits in an already
failed Saga are masked by the generator, so they are not independent genes. Different
Sagas may both fail. The generator returns valid ordered actions for the vector; search
selects one returned ordering. It never constructs compensation actions itself.

Random sampling first chooses each Saga's fault coordinate uniformly, then a returned
recovery ordering uniformly. GA uses exactly that sampler for initialization/exploration.
This distribution is **not uniform over all FaultScenarios**: vectors can have different
numbers of recovery orderings. The older uniform finite-catalogue baseline is unchanged.

After initialization, GA compares pairs of evaluated candidates to select parents,
combines their fault coordinates and recovery choice, and may mutate one coordinate.
An inherited recovery choice that does not exist for the child vector is replaced by a
seeded valid choice. The population retains the best available configured scores, randomizing ties.
A duplicate offspring triggers random exploration. Null fitness never enters the parent
population. Population/operator defaults are starting settings, not tuned findings.

## Scope and accounting

Participants, inputs/setup, forward order, selected event deliveries and runtime stay
fixed. On-demand requests can add scenarios to the arm's package. Each request records
before/after hashes, requested cap, uncapped count, returned count and truncation. Old
scenarios outside that returned cap cannot silently enlarge the arm's domain.

Candidates are cached by workload, canonical vector and exact ordered actions. Historical
ID aliases are grouped; repeated proposals do not execute again. Invalid application
attempts consume budget with null I and are not automatically retried. Generation failures
have separate cost and diagnostics. Missing/partial A does not change I-only eligibility.

Termination is execution budget, proven exhaustion of the cap-limited domain, or a named
proposal stall. A stalled run has not proved exhaustion. Earlier catalogue limits still
exclude workloads; this command does not recover omitted Saga combinations or inputs.

## Evidence and replay

`config.json` and `scope.json` own selection/runtime and candidate scope. `requests.json`
records generator calls and package revisions. `progress.jsonl` records proposals,
parent identities/fitness and measured attempts incrementally. `results.json` and
`SUMMARY.md` report best-so-far configured score, best I, first positive score, distinct
positive-score scenarios, duplicates, null fitness, stop reason, I and A. Results retain
per-criterion counts and coverage, score-unavailability reasons, and the exact fitness
configuration. Parent lineage records both original I and the score used for selection. Positive scenarios can expose the same underlying bug.

Each attempt retains execution/ImpactV1/ImpactV2/read reports, hashes, a snapshot of the
exact package revision, Docker argv and runtime configuration. Replay into a fresh output:

```bash
python3 verifiers/experiments/fixed-workload-ga/run.py replay \
  --attempt verifiers/target/my-search/ga-11/attempt-001/attempt.json \
  --output verifiers/target/my-search/replay-001
```

Replay verifies the recorded runtime hashes, including its source/runner provenance; keep
that snapshot available. The qualification retains a copy of the search sources as well.
This re-executes the persisted scenario with a new attempt identity. It does not continue
the GA or reuse its score. General resume, distributed scheduling and cross-workload
budget allocation are outside this implementation.

## Qualification

```bash
python3 -m unittest discover -s verifiers/experiments/fixed-workload-ga -v
python3 verifiers/experiments/fixed-workload-ga/qualify.py \
  --output verifiers/target/fixed-workload-ga/qualification-01
```

The declared protocol is two no-fault controls, GA/random × seeds 11/29 × budget 12 on
the preselected RemoveTournament/AddParticipant workload, and three fixed update/read
recovery witnesses through the same evaluator: at most 53 application executions. Results
are recorded in `RESULTS.md`; raw reports stay in `verifiers/target/`. Run
`summarize_qualification.py --run <qualification-directory> --output <summary.json>`
to audit the retained campaign and regenerate its compact evidence index.

## Discovery-speed follow-up with unchanged I

The user-approved follow-up completes the eight previously unmeasured candidates, then
runs GA/random with seeds 11, 29 and 47 and budget 29 per arm. It preserves population 8,
mutation 0.3 and stall limit 100; it changes no scoring or search operator. The fully
measured reference has 15 I-positive and 14 zero-score candidates. Reference observations
are used only by the final analysis; every arm obtains fresh application feedback.

```bash
python3 verifiers/experiments/fixed-workload-ga/discovery.py \
  --output verifiers/target/fixed-workload-ga/discovery-01
uv run --with matplotlib python verifiers/experiments/fixed-workload-ga/discovery_analysis.py \
  --run verifiers/target/fixed-workload-ga/discovery-01 \
  --output docs/verifiers-impl/evidence/ga-discovery-2026-09-10 --plot
```

The protocol freezes seeds and limits before completing the reference map. Maximum new
cost is 182 application executions (eight reference completions + 174 search attempts),
with two isolated containers at a time. The identical-runtime no-fault control is reused
with hash verification. Outputs retain real attempted execution counts if an arm stalls.

The analysis reports cumulative distinct positive discoveries, the first positive and
execution counts to reach 50%, 80% and all reference positives (8, 12 and 15 cases).
Missing targets stay `NOT_REACHED`; a stalled run is not assigned a fictional completion
at the budget limit. It also retains the best I, actual offspring/fallback executions,
duplicate proposals and wall time. The plots show actual executed prefixes; a marker
identifies an early stop. An auxiliary fixed-budget curve-area statistic carries the last
observation forward after a stall and is labelled separately from actual executions.

The complete-map denominator belongs to this workload, recovery cap and frozen impact
policy. Reassess the map before evaluating a changed I/A feedback definition.

## Weighted-criteria qualification

```bash
python3 verifiers/experiments/fixed-workload-ga/qualify_weights.py \
  --output verifiers/target/fixed-workload-ga/my-weighted-qualification
```

This bounded integration protocol revalues 43 retained cases, checks legacy selection
against the frozen old search, then runs one no-fault control, three recovery witnesses
and three eight-execution arms on UpdateTournament/FindTournament. It uses the existing
qualified Quizzes runtime with current search-source hashes. The
[results and evidence](../../../docs/verifiers-impl/evidence/weighted-fitness-2026-09-15/README.md)
explain what changed in scores and why the small sample cannot establish search benefit.

## Paired campaign launcher

`campaign.py --protocol <protocol.json>` runs predeclared matched seed pairs, two isolated
search processes at a time. `--dry-run` prints commands without executing. The protocol
records configs, a qualified control, frozen file hashes, per-method budget and a free-disk
threshold. Each pair must finish its declared budget with integrity PASS before the next
starts. Existing status files are never overwritten by a new invocation; inspect retained
results before preparing a separate continuation. `status.json` and each arm's
`progress.jsonl` distinguish running, complete and stopped work.

Per-attempt package snapshots use independent APFS copy-on-write clones on macOS where
available, falling back to ordinary copies elsewhere. This saves duplicate storage without
hardlinks or deleting prior evidence. The
[500-by-three recovery-qualified protocol](../../../docs/verifiers-impl/evidence/recovery-history-2026-09-15/README.md)
records the first larger campaign and its measured throughput.
