# Structural space map

Research harness for eight source-derived Quizzes pair/triple strata. It calls the
existing generator, on-demand request service, and prepared batch executor. It does
not implement a detector, fitness function, GA, new setup binding, or executor contract.

The approved scope and sampling policy are in
`issues/2026-09-07-space-map/{SPEC,PLAN}.md`. Selection precedes runtime outcomes.
The 10-input-per-Saga generation boundary is deliberate: the campaign covers a
bounded catalogue, not all Quizzes test inputs. The 50,000 workload cap should be
checked against accounting; selection is stratified across Saga combinations, not
a prefix of global workload IDs. Within each stratum, maximum event count is a
structural preference; seed 9072026 plus the one-based stratum index picks a workload.
Different event placements remain different workload identities.

Each workload keeps its setup, inputs, schedule, and deliveries fixed. Canonical
vectors contain at most one failure per participant. All-zero and all single-point
controls are mandatory; up to 24 vectors are selected. The request service enumerates
up to 10,000 recovery schedules per selected vector. Its uncapped count discloses
whether the persisted recovery universe is complete. A capped prefix cannot be
described as a uniform sample of every possible recovery schedule.

At most 32 scenario IDs per workload are selected: enumerate a small persisted
universe, otherwise sample one per vector and fill the remaining budget using the
same explicit seed. This is vector-stratified sampling, not uniform sampling over
all scenarios. Up to three distinct outcome signatures per workload receive one
extra execution, chosen by a frozen representative rule. No failed or zero scenario
is replaced. These repeats do not establish stability of the entire space.

## Commands

Run `generate-prepared.sh` inside the existing `scenario-executor` Compose service
with `BUILD_OUTPUT_DIR` and `GENERATION_OUTPUT_DIR` pointing under `/reports`.
It reads the prepared source snapshot. This campaign's preparation uses
`verifiers/target/batch-execution/selection.json` as its existing source provenance.
It checks checkout compatibility at initial selection; running an already frozen
plan subsequently permits checkout drift while verifying the complete build and
package. A campaign against changed production requires new build/provenance
qualification, rather than silently reusing these source expectations.

From the repository root:

```sh
python3 verifiers/experiments/space-map/map.py prepare \
  --manifest verifiers/target/space-map/generated/<package>/scenario-catalog-manifest.json \
  --build verifiers/target/combined-event-deliveries/run-02/prepared-build \
  --output verifiers/target/space-map/prepared
python3 verifiers/experiments/space-map/map.py run \
  --plan verifiers/target/space-map/prepared/experiment.json \
  --output verifiers/target/space-map/run-01
python3 verifiers/experiments/space-map/map.py summarize \
  --plan verifiers/target/space-map/prepared/experiment.json \
  --output verifiers/target/space-map/run-01
python3 verifiers/experiments/space-map/analyze.py \
  --plan verifiers/target/space-map/prepared/experiment.json \
  --output verifiers/target/space-map/run-01
python3 -m unittest discover -s verifiers/experiments/space-map -p 'test_*.py'
```

Preparation refuses an existing structural plan. Execution refuses an existing output
directory. Interrupted attempts and package mutations stay available for diagnosis;
there is no automatic retry or resume that would silently change the sample.

The batch runner validates report schemas, workload/scenario/vector identity, manifest
path, supported terminal/conformance status and COMPLETE score eligibility. Each
attempt has a fresh named Docker container, JVM and in-memory H2 database, a 120-second
timeout and exact-container cleanup. Concurrency is two. The map rechecks sampling,
report hashes, unique execution IDs, prepared files and external dependency hashes.
Raw evidence stays in `verifiers/target/space-map`; compact evidence and results are
committed separately. Setup failures and unsupported execution outcomes are evidence
about the executable boundary, never zero fitness.

`analyze.py` also validates complete report bundles emitted with a nonzero CLI exit.
The batch runner retains those attempts as `PROCESS_FAILURE`; the additional analysis
distinguishes their execution status, invalid assessment, hard-stop reason, setup
status and unassigned application failures. It never turns a nonzero exit into fitness.
Its attempt-ID check includes these invalid executions, and its repeat comparison uses
report semantics when both invalid runs have complete report bundles. The map runner's
own attempt-ID count covers only its successful-process report path.

The postprocessor also groups identical `(workload ID, fault vector, ordered compact
actions)` records under an analysis key. This was added after detecting eager/request
IDs with equal action content during the campaign. It reports raw ID counts and
distinct action sequences separately, compares the observed equivalent-ID outcomes,
and preserves the original ID-based sample. It is not a package migration or a new
production identity contract. Positive object lifecycle states remain visible so a
residual tombstone is not described as an active leaked object.

Capture `docker image inspect scenario-executor:latest --format '{{.Id}}'` in
`verifiers/target/space-map/image-id-start.txt` at campaign start and
`image-id-end.txt` at completion. After analysis, use `export.py --prepared ... --run ...
--evidence ...` to create a new compact evidence directory. The export records the
checkout drift at selection and export separately; live changes in other tasks do not
change the measured prepared build.
