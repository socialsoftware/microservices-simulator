# Fixed-workload deterministic and random baselines

This small harness evaluates persisted candidates online without knowing their scores.
It reuses the prepared batch attempt primitive in explicit `assessment` mode; the
existing `regression` mode remains the default for known-outcome comparisons. The
[approved spec](../../../issues/2026-09-06-search-baselines/SPEC.md) and
[plan](../../../issues/2026-09-06-search-baselines/PLAN.md) define the boundary.

## Reproduce

From the repository root, with the retained source-matched build and current package:

```sh
python3 verifiers/experiments/search-baselines/search.py prepare \
  --snapshot-selection verifiers/target/batch-execution/selection.json \
  --benchmark-selection verifiers/target/impact-coverage-controls/selection-final/benchmark-selection.json \
  --output verifiers/target/search-baselines/prepared

python3 verifiers/experiments/search-baselines/search.py run \
  --experiment verifiers/target/search-baselines/prepared/experiment.json \
  --output verifiers/target/search-baselines/NEW_RUN

python3 verifiers/experiments/search-baselines/search.py summarize \
  --run verifiers/target/search-baselines/NEW_RUN
```

Optional historical benchmark cross-check, only after the online campaign completes:

```sh
python3 verifiers/experiments/search-baselines/compare_reference.py \
  --run verifiers/target/search-baselines/NEW_RUN \
  --historical-attempts verifiers/target/impact-coverage-controls/run-01/benchmark/attempts
```

This writes `reference-check.json` and adds zero new executions. It is not an offline
replacement for the online strategy runs.

Prepare once; the checked-in machine has that frozen experiment already. Every new run
needs a new output directory. Missing local build/package prerequisites require the
[existing build/generation route](../batch-execution/README.md#reproduce) and a compatible
snapshot selection. Historical schemas/IDs are never translated. The current preparation
copies a validated current package, then uses FaultScenarioRequestCli only for the six
missing canonical benchmark vectors. It does not modify the retained original package,
re-request eager vectors or manufacture scenarios in Python.

The benchmark reference selection supplies **structural IDs only**, to verify the
corrected 29-scenario universe. Its old scores are not read by prepare/order/run. The
snapshot selection supplies source/build provenance, not candidate outcomes. The new
`experiment.json` contains no expected scores.

## Frozen universes and budgets

| Workload | Structure / fixed horizon | Eligible scenarios | Budget per strategy |
| --- | --- | ---: | ---: |
| RemoveTournament/AddParticipant | Two participants, five steps, no event actions | 29 / 12 canonical vectors | 12 |
| CreateQuiz | One participant, three steps, no event actions | 4 / 4 vectors | 4 |
| AnonymizeStudent with event | One participant, two steps and one selected event consequence | 3 / 3 vectors | 3 |

Every row is a separate comparison. Exact workload/setup IDs, participants, ordered
schedule/event horizon, scenario IDs, vectors and recovery actions are frozen. No
no-event sibling or different setup competes with the selected event workload. The
additional workloads were chosen for execution diversity and prior qualified preparation
before new scores; all their persisted scenarios are retained, including outcomes that
were not present in the old control/last-fault pairs.

The universe is deliberately bounded. Singles contain the zero vector and one fault per
forward slot. The benchmark permits at most one fault per participant and retains its
persisted recovery variants. Missing vectors `00101`, `00110`, `01001`, `01010`, `10001`,
`10010` are requested with recovery cap 20. The source package was generated with catalogue
cap 10000, max inputs/Saga 1000, max schedules/input tuple 20, recovery cap 20, SERIAL,
single-Saga generation plus the existing benchmark prerequisite contribution, and maximum
three selected event consequences. Its accounting remains in the copied package. These
caps do not constitute an exhaustive binary-vector, interleaving or catalogue claim.

There is one deterministic order sorted by `(faultVector, faultScenarioId)` and five
random permutations without replacement, seeds **11, 29, 47, 71, 101**. `--seeds` makes
alternative seed sets explicit during preparation. Random uses Python's seeded shuffle;
the Python version and full realized permutations are persisted. Every strategy uses
the same eligible set and budget within its workload. The benchmark budget of 12 equals
its canonical-vector count but samples **scenarios**, not one row per vector: recovery
variants consume separate slots. Each small universe is exhausted. There are 114 planned
fresh online attempts and no redundant deterministic replicas.

Orders are frozen before execution and rechecked on replay. Observed scores never feed
selection, ordering, retries or replacement. Repeated candidates across random seeds
are new executions, not cached results or offline reshuffling. The runner continues
after finding a positive so the whole budget-prefix discovery curve is observed.

## Evaluation and evidence contract

Every attempt has a fresh named Docker container, JVM and in-memory H2 database. At most
two strategy runs execute concurrently; attempts within one strategy run are sequential.
The existing limits are 2 CPUs/3 GiB per container and a 180-second individual timeout.
There is no global temporal budget and no retry. The timeout stops only its own container.

The ordinary report validator checks schema, package/workload/scenario/vector joins,
attempt IDs, category counts and score consistency. A completed execution with EXACT
or supported DEVIATED conformance can be evaluated; both remain explicit in its evidence.
`assessment` returns COMPLETE, PARTIAL, INVALID, UNAVAILABLE or EXECUTION_INVALID,
alongside the existing process/timeout/malformed-report failure statuses. Only COMPLETE
has an available search score. PARTIAL retains its raw lower-bound evidence but gets no
complete search score. A valid COMPLETE zero remains an evaluated zero.
Optional setup records omitted by the report model have unavailable timing, not an
inferred zero or an execution failure. Malformed present metadata fails report validation
without retaining complete/score claims.

**Positive** means a valid completed execution with ImpactV2 COMPLETE and score greater
than zero. Discovery counts distinct positive **FaultScenario IDs** inside this workload.
Different recovery schedules may expose the same condition; counts do not mean distinct
defects, severity, business harm or a global ranking of workloads.

Each arm preserves its plan, sequential attempts and discovery summary. At every prefix,
the report includes attempt count, distinct positives, statuses and cumulative process
wall time. The first positive is one-based; absence is explicitly
`NOT_FOUND_WITHIN_BUDGET`, with a null first-positive index/time. Every failed or incomplete
attempt consumes one budget slot and remains in the denominator.

`summary.json` and `discovery.md` provide per-workload/per-seed tables and curves by
budget. They include actual strategy wall time and process wall cost; concurrency and
host contention make time descriptive, not a clean causal comparison of algorithms.
No offline analysis is included in the online attempt count. Any post-run comparison
with the known benchmark landscape must be labeled separately.

Repeatability is assessed after execution for every eligible candidate. The report
distinguishes unobserved candidates, insufficient repeats, stable complete repetitions,
and divergent/incomplete observations. All seven additional candidates get six independent
executions. Benchmark coverage depends on the frozen budget prefixes and remains explicit.
No candidate is dropped because it produces zero, fails or disagrees across repetitions.

The measured source/build snapshot is authoritative. Package/build/Maven/image and
runner/validator hashes are checked before/after. Current-checkout source differences
are reported separately so another task's later edits do not invalidate a frozen-snapshot
campaign. The cache's simulator/verifier JARs must match the prepared snapshot. If another
build changes the shared Docker Maven cache during measurement, integrity fails visibly.

Exit status is nonzero for integrity failures, non-complete attempts or repeatability
divergence, while evidence is retained. An externally interrupted run preserves finished
arms and per-attempt artifacts; it is not silently declared complete or automatically resumed.

## Tests and next boundary

```sh
python3 -m unittest discover -s verifiers/experiments/search-baselines -v
python3 -m unittest discover -s verifiers/experiments/batch-execution -v
```

Tests cover unknown positive/zero results, unavailable scores, report identity, fixed
horizons, budget exhaustion including failures, no-positive reporting, reproducible
orders independent of scores, duplicates, no retries and snapshot-vs-checkout integrity.

This is a baseline with no adaptive fitness policy. A GA still needs a valid genome for
faults **and recovery choices**, operators/repair, duplicate accounting, incomplete-result
handling and an explicit budget-matched protocol over useful fixed workloads. It must
call the same real evaluator rather than treating a random permutation as evolution.

See [RESULTS.md](RESULTS.md) for the measured comparison and retained evidence.
