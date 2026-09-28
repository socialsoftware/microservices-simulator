# Fixed-workload genetic search

The [smaller exhaustive reference](../../../docs/verifiers-impl/evidence/exhaustive-reference-2026-09-16/README.md)
uses `exhaustive_reference.py` to measure an uncapped generated domain once and compare
30 seed pairs with scores revealed only when candidates are selected. It retains unknowns
and proposal stalls, checks six repeated executions, and reports ordering rather than
live-search wall time. This is separate from the ordinary live commands below.

`uniform_reference.py --reference <completed-map> --output <new-directory>` adds a
uniform permutation baseline to a completed exhaustive reference. It preserves retained
GA/two-stage random traces, verifies scores against the map and runs no application.
On the 186-case map, uniform catalogue sampling discovers positives earlier on average
than the original on-demand GA; see the reference evidence for results and scope. This experiment
does not change the ordinary live search policy.

`uniform_exploration.py` retains the recorded-feedback comparison. Its uniform-unseen
GA variant improved early discovery on both the 186-case development map and the
[72-case second workload](../../../docs/verifiers-impl/evidence/ga-confirmation-2026-09-16/README.md).
The ordinary command now supports the same exploration with real application executions
through `--catalogue`; omitting it preserves the historical on-demand policy.

The [weighted-fitness qualification](../../../docs/verifiers-impl/evidence/weighted-fitness-2026-09-15/README.md)
checks the configurable criteria with 43 retained cases and 28 fresh executions.
The historical [discovery-speed results](DISCOVERY-RESULTS.md) compare GA/random against a
complete current reference. The [first qualification](RESULTS.md) establishes integration.

Select one existing WorkloadPlan and search its fault/recovery alternatives. The command
uses the ordinary isolated ScenarioExecutor with either a prepared complete catalogue
or the historical on-demand Java generator. It
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

## Complete-catalogue live search

Prepare one complete catalogue and reuse it across both methods and seeds:

```bash
python3 verifiers/experiments/fixed-workload-ga/run.py catalogue \
  --config /absolute/path/config.json --output verifiers/target/my-catalogue

python3 verifiers/experiments/fixed-workload-ga/run.py run \
  --config /absolute/path/config.json --control /absolute/path/control/control.json \
  --catalogue verifiers/target/my-catalogue --output verifiers/target/my-live-ga
```

Set `strategy` to `ga` or `random` in the configuration. With `--catalogue`, random
samples uniformly without replacement over distinct candidates. GA retains its existing
selection, crossover and mutation, using uniform unseen candidates for initialization
and duplicate fallback. The catalogue contains structure, not scores: each choice calls
the ordinary isolated executor before the next choice. Both modes use the same search
loop and assessment as recorded-feedback experiments. Seeded random choices use successive
unseen sampling; this is uniform but need not match an older shuffle's exact seeded order.

Catalogue preparation enumerates every canonical fault vector through the ordinary Java
generator. A truncated or failed request stops preparation without publishing a usable
`catalogue.json`. Raise `recoveryCap` and prepare a new catalogue after truncation. Exactness
is within the selected workload's fixed inputs, forward order and event choices; it does
not recover workloads omitted by upstream generation. Enumeration may be expensive.

The catalogue seals request evidence and package hashes. Reuse requires the same source
package, workload, runtime descriptor and recovery cap; strategy, seed, budget and weights
may differ. A live catalogue search requires a measured no-fault control with a valid
terminal/schedule status and complete score for its enabled weights. Positive and
compensated/deviated controls are eligible. Its score is used only for
qualification and never initializes search feedback. It preserves unknown scores as budget-consuming
attempts, never population members. Repeated proposals do not reexecute the application.

Results record `candidateDomain`, `samplingPolicy`, catalogue identity/count, shared
catalogue generation/enumeration cost, per-search application time and command wall time.
Shared preparation is outside search wall time and must be included explicitly when
reporting end-to-end cost. Legacy commands and results remain reproducible; they do not
silently adopt the new policy. These commands do not implement cluster scheduling or
resumption of interrupted runs.

## Cross-workload recorded allocation

`allocate.py` spends one configurable global attempt budget over a bounded list of
complete workload catalogues. This first implementation is a sequential
**recorded-feedback experiment**: each decision selects one workload, its persistent GA
selects one unseen FaultScenario, and only then does the evaluator reveal that candidate's
retained observation. It does not execute ScenarioExecutor live. The evaluator boundary is
separate; `live_allocate.py` supplies the same one-candidate contract with real isolated
executions without putting outcomes into policy inputs.

Each workload entry uses the existing map configuration, complete catalogue and
`combined-reference.json`:

```json
{
  "schemaVersion": "contextual-workload-allocation.v1",
  "seed": 11,
  "budget": 60,
  "policy": "contextual-linucb-cooldown",
  "policyParameters": {"exploration": 1.0, "ridge": 1.0},
  "fitness": {
    "policy": "weighted-criteria-v2",
    "weights": {
      "DELETED_DEPENDENCY": 1,
      "FAILED_OPERATION_RESIDUAL": 1,
      "UNRESOLVED_DELIVERED_EVENT": 1,
      "COMPENSATED_READ_EXPOSURE": 1,
      "LOST_COPIED_UPDATE": 1
    }
  },
  "workloads": [
    {
      "name": "remove-course",
      "config": "/absolute/path/to/config.json",
      "catalogue": "/absolute/path/to/catalogue",
      "reference": "/absolute/path/to/combined-reference.json"
    }
  ]
}
```

Run it from the repository root:

```bash
python3 verifiers/experiments/fixed-workload-ga/allocate.py \
  --config /absolute/path/to/allocation.json \
  --output verifiers/target/my-allocation
```

The two nonadaptive baselines are:

- `round-robin`: balanced turns in stable workload-ID order, skipping exhausted workloads;
- `uniform`: seeded uniform selection among nonexhausted workloads.

The six matched adaptive policy labels form three model-by-guard pairs:

- `adaptive-ucb` and `adaptive-ucb-cooldown`: independent per-workload mean-score UCB with
  an allocation-count exploration bonus;
- `progress-linucb` and `progress-linucb-cooldown`: one shared linear-UCB model using only a
  bias and already observed workload progress;
- `contextual-linucb` and `contextual-linucb-cooldown`: the existing shared linear-UCB model
  over structural Saga membership, Saga-pair, interaction/access and event-route features
  plus observed progress.

The existing `adaptive-ucb`, `contextual-linucb` and
`contextual-linucb-cooldown` names keep their earlier behavior. The `-cooldown` suffix always
means the same deterministic one-other-attempt eligibility guard after missing feedback.
It prevents a missing-only workload from monopolizing consecutive decisions, but has not
been established as the best policy.

Both linear models have one shared parameter vector and no workload-ID feature. The
progress-only model records exactly `bias` plus the existing observed-progress features and
receives no structural token or count. The full contextual model additionally uses static
binary feature groups with `1/sqrt(group size)` scaling and structural counts with
`count/(1+count)` scaling. Observed score progress uses `score/(1+score)`. Construction and
feature names are recorded in the result. Neither unseen scores nor known positive density
enter the context.

Every local session keeps its population, seen candidates, RNG and proposal history when
the outer policy switches workloads. Local search is fixed at GA population 8, mutation
0.3 and uniform-unseen catalogue initialization/fallback. Reward is exactly the newly
observed configured weighted score. An unavailable score consumes the global budget,
remains an unknown, enters no GA population and performs no adaptive-model update. The
guarded variant then excludes that workload from the next completed decision when another
workload is active; it re-enters immediately afterward. A sole active workload remains
eligible, and an available zero updates the model and does not start a cooldown. Each raw
variant remains available so the missing-feedback behavior stays measurable.
The catalogue loader verifies seals, completeness and candidate identity; the recorded
evaluator also verifies exact candidate/observation joins and the map's original fitness assessment.
Retained `PROCESS_FAILURE`, `TIMEOUT`, `INVALID_REPORT` and `INFRASTRUCTURE_FAILURE`
records are admitted only with the expected unavailable measurement shape and a null stored
assessment. Arbitrary status names and non-null failure scores are rejected.

`results.json` contains aggregate and per-workload allocation, positive discoveries,
cumulative score, unknowns, model-update count and allocation-plus-local-GA overhead.
`decisions.jsonl` retains one compact row per global attempt. Selection and update timing
have the following boundary:

- selection includes active-workload/exhaustion scans, outer-policy choice and local-GA
  `ask`, including duplicate fallback and the final scan when catalogue exhaustion ends a run;
- update includes local-GA `tell` and fitness assessment, observed-progress bookkeeping and
  an adaptive-model update when feedback is available;
- input/reference validation, feature/model initialization, evaluator/application latency
  and final result serialization are excluded.

`SUMMARY.md` is the short human-readable view.
This bounded allocator does not admit workloads lazily, run controls, schedule containers,
or establish a reinforcement-learning claim.

### Held-out workload transfer harness

`transfer.py` exposes `run_transfer(workloads, evaluator_factory, train_ids, test_ids,
seed, ...)` for the bounded recorded-feedback transfer comparison. It creates one stable
round-robin training prefix, updates the existing structural and progress-only linear-UCB
models from pre-update contexts, and runs four matched target arms: structural warm/cold
and progress-only warm/cold. Target GA sessions and progress are fresh in every arm. Warm
arms transfer only `inverse`, `response`, `theta`, and `updates`; all feature names are
frozen from outcome-free train and test profiles before feedback is requested.

The fixed protocol uses population 8, mutation 0.3, uniform-unseen catalogue exploration,
linear exploration/ridge 1, no missing-feedback cooldown, and unit weights for all five
criteria. Defaults are 132 shared training attempts, 256 target attempts, and checkpoints
16/32/64/128/256. The returned object keeps the common training cost separate and includes
compact decisions, checkpoint curves, input/history/model hashes, fresh-state evidence,
and initial target rankings. The caller must supply a factory that returns a fresh
validated evaluator for the training prefix and for each arm. The harness does not load
references, choose a split, or run a multi-seed campaign.

## Cross-workload live allocation

`live_allocate.py` uses the same eight outer-policy labels, seeded local GA sessions,
fitness reward and one shared global attempt budget as recorded allocation. Its inputs are
still an explicit finite workload list. Each entry replaces `reference` with the qualified
no-fault `control` for that workload:

```json
{
  "schemaVersion": "contextual-workload-live-allocation.v1",
  "seed": 11,
  "budget": 60,
  "policy": "contextual-linucb-cooldown",
  "policyParameters": {"exploration": 1.0, "ridge": 1.0},
  "fitness": {
    "policy": "weighted-criteria-v2",
    "weights": {
      "DELETED_DEPENDENCY": 1,
      "FAILED_OPERATION_RESIDUAL": 1,
      "UNRESOLVED_DELIVERED_EVENT": 1,
      "COMPENSATED_READ_EXPOSURE": 1,
      "LOST_COPIED_UPDATE": 1
    }
  },
  "workloads": [
    {
      "name": "remove-course",
      "config": "/absolute/path/to/config.json",
      "catalogue": "/absolute/path/to/catalogue",
      "control": "/absolute/path/to/control/control.json"
    }
  ]
}
```

The command verifies every existing runtime descriptor, source package, complete catalogue
and control before the first search attempt. A control needs a valid measured outcome and
complete score under the live allocation weights; positive and compensated/deviated controls
are eligible. Controls are never run by this
command. The gate recomputes feedback from the control's hash-verified retained attempt and
raw reports; edited summary fields cannot qualify it. Control scores are not supplied to the
allocator or local GA. `results.json` records retained control
evidence cost separately and excludes it from the global search budget.

```bash
python3 verifiers/experiments/fixed-workload-ga/live_allocate.py run \
  --config /absolute/path/live-allocation.json \
  --output verifiers/target/my-live-allocation

# Request a clean boundary pause, inspect it, then continue with the same frozen inputs.
python3 verifiers/experiments/fixed-workload-ga/live_allocate.py pause \
  --output verifiers/target/my-live-allocation
python3 verifiers/experiments/fixed-workload-ga/live_allocate.py status \
  --output verifiers/target/my-live-allocation
python3 verifiers/experiments/fixed-workload-ga/live_allocate.py resume \
  --config /absolute/path/live-allocation.json \
  --output verifiers/target/my-live-allocation
```

Execution is sequential. Each selection is durably recorded before `Runtime.evaluate`
launches the ordinary isolated ScenarioExecutor. A verified completion receipt is durable
before its compact decision enters `decisions.jsonl`. Resume validates the frozen
configuration, runner sources, controls, catalogues, package hashes and runtime identity,
then reconstructs the allocator by deterministic replay of completed feedback. Completed
results, including unavailable scores, are not executed again. A file lock rejects a second
coordinator, and atomic fsynced protocol, dispatch, receipt and journal replacements make
torn or changed durable state an explicit integrity error.

SIGINT, SIGTERM and `pause` finish the one in-flight attempt and stop before the next
selection. Wait for `PAUSED` or `COMPLETE` with `inFlight: 0` before closing the terminal.
If a coordinator disappears after durable dispatch but before a verified receipt, resume
first salvages a complete valid `attempt.json`. Otherwise it reports `REVIEW_REQUIRED` and
will not retry that candidate. After checking the retained attempt directory and ensuring
no matching executor container remains, explicitly consume that ambiguous decision without
a score:

```bash
python3 verifiers/experiments/fixed-workload-ga/live_allocate.py resolve-spent \
  --output verifiers/target/my-live-allocation --decision 17
```

`resolve-spent` is deliberately separate from `resume`. It records unavailable feedback,
uses one unit of the global budget, supplies no GA parent or adaptive-model update, and avoids
a possible second application execution. Output distinguishes dispatches, verified attempt
results and ambiguous dispatches marked spent; these are not a count of completed
application executions. This command does not schedule cluster jobs,
qualify controls lazily or expand the admitted workload/catalogue set.

## Resumable measurement campaign

`measure_map.py` collects a measured map for later recorded-feedback search. It does not
run a search method or automatically start the historical replay policies in
`exhaustive_reference.py`. Use the qualified config/control and a complete enumeration
directory (including one produced by `run.py catalogue`):

```bash
python3 verifiers/experiments/fixed-workload-ga/measure_map.py run \
  --config /absolute/path/config.json --enumeration /absolute/path/catalogue \
  --control /absolute/path/control/control.json \
  --output verifiers/target/my-map --workers 2

# In another terminal:
python3 verifiers/experiments/fixed-workload-ga/measure_map.py pause --output verifiers/target/my-map
python3 verifiers/experiments/fixed-workload-ga/measure_map.py status --output verifiers/target/my-map
```

Wait for `PAUSED` (or `COMPLETE`) with `inFlight: 0` before closing the laptop. A pause
stops dispatch and lets the running attempts finish. Ctrl-C and SIGTERM request the same
drain. Resume with the original `run` command plus `--resume`; `--workers` may change.
The explicit resume clears the previous pause request. The command runs in the foreground;
keep its terminal alive. This is not a background service or protection against laptop sleep.

The campaign freezes the configuration, selected keys, control, enumeration, package and
runner hashes. Resume validates retained report bundles and skips completed candidates,
including completed assessments with unavailable scores. It refuses changed evidence or a
second coordinator. Following an abrupt coordinator death, wait for any orphaned containers
to finish before resuming; incomplete attempt directories remain as evidence and their
candidates get new attempt numbers. There is no automatic retry of completed infrastructure
failures, and no promise of exactly-once execution across a hard crash.

Optional `--keys /absolute/path/keys.json` selects a fixed pilot subset, frozen across resume.
`status.json` reports progress for the current session; `sessions.json` retains elapsed time
and worker count for every invocation. Atomic completion receipts protect finished attempt
metadata. `reference.json` is published once every selected candidate has a recorded result;
this means measurement finished, not that all scores are available or repeatability has
been established. Repetitions and the final uniform-exploration GA/random comparison remain
separate evaluation steps. More workers means more isolated JVMs, not more concurrency
inside any one scenario.

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
requires another search run. Rescoring does not rerun cross-workload allocation, and no new
anomaly detector is included.

## Search choices

For each Saga, choose no fault or one faultable step. Later fault bits in an already
failed Saga are masked by the generator, so they are not independent genes. Different
Sagas may both fail. The generator returns valid ordered actions for the vector; search
selects one returned ordering. It never constructs compensation actions itself.

In the historical on-demand mode, random sampling first chooses each Saga's fault coordinate uniformly, then a returned
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
the GA or reuse its score. General resume and distributed scheduling remain outside this
implementation; cross-workload allocation is available separately for complete recorded maps.

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
