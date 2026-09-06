# Observer coverage and prerequisite requalification

This campaign keeps the earlier 30 named control/late-fault pairs, declares source-backed
input replacements, and also runs the corrected RemoveTournament/AddParticipant benchmark.
It does not change scoring policy, repair production application bugs, or silently replace
failed attempts. The approved scope and domain discussion are in
`issues/2026-09-06-impact-coverage-controls/`.

## Prepare and freeze

Use the current compiled generator to create a fresh Quizzes package with the same
qualification bounds: singles enabled, maximum Saga set size 1, catalogue cap 5000,
input cap 1000, schedule cap 20, recovery cap 20, SERIAL, strict inputs, no dynamic
enrichment. The explicit provider-backed benchmark is retained by its existing descriptor.
Record the exact generation command and log under `verifiers/target/impact-coverage-controls/`.

Use FaultScenarioRequestCli to add only the benchmark's missing canonical vectors:
`00101`, `00110`, `01001`, `01010`, `10001`, `10010`. Do not request eager vectors again.
Freeze the selection before the new runtime campaign:

```sh
python3 verifiers/experiments/impact-coverage-controls/select.py \
  --manifest PATH_TO_FRESH_MANIFEST \
  --old-manifest verifiers/target/impact-v2-broader/generated/quizzes-20260906-021039-016/scenario-catalog-manifest.json \
  --old-selection verifiers/target/impact-v2-broader/broader-selection.json \
  --overrides verifiers/experiments/impact-coverage-controls/selection-overrides.json \
  --output verifiers/target/impact-coverage-controls/selection
```

An override names a source test class/method (and other exact source fields when useful)
and its prerequisite-based reason. All other pairs retain their prior input ID. Candidate
ties prefer the previous action shape, then event count, setup cost and deterministic ID.
No metric result is used in selection. Missing pairs or duplicate exact benchmark action
sequences fail selection. The selector records old/new input, workload and source identity
for each pair. Unselected Saga types are outside this retained cohort, not declared
ineligible under the current generator.

## Execute

```sh
python3 verifiers/experiments/impact-coverage-controls/run.py \
  --manifest PATH_TO_FRESH_MANIFEST \
  --selection verifiers/target/impact-coverage-controls/selection \
  --output verifiers/target/impact-coverage-controls/run-01
```

The runner reuses the broader campaign's `prepare-build.sh` and `run-prepared.sh`.
It compiles one immutable source copy in Docker, checks source-copy hashes, then uses
ordinary ScenarioExecutorCli in a fresh JVM/H2 environment per attempt. It allows two
concurrent attempts, a 1536 MiB Java heap and 512 MiB metaspace per process. Compose
resource settings and exact per-attempt commands are retained. Existing services are
not stopped. Run output must be empty; selections and package hashes must remain fixed.

All selected attempts retain process exit, logs and reports, including invalid outcomes.
For a declared prerequisite repair discovered during qualification, regenerate and freeze
a new selection, then use `--only-pair PAIR_ID` with a new output directory to rerun both
roles. Preserve the first campaign; record the old and new source selection and report
which pair supersedes it for interpretation. This option does not run the benchmark.
Report validation checks attempt/workload/scenario identity, nullable counts, category
coverage and the distinct-object union. A runner validation PASS means the reports are
present and coherent; it does not mean all executions or assessments are complete.

The existing `impact-v2-broader/summarize-broader.py` can summarize the frozen broader
selection against its new attempt directory. The root handoff adds benchmark recovery
checks and source-linked domain examples. Previous campaign observations remain separate.
