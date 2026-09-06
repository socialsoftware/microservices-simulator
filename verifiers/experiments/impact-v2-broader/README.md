# ImpactV2 broader qualification

This experiment refreshes the historical Quizzes RemoveTournament/AddParticipant
landscape with the current executable package. `historical-baseline.json` retains only
the old vector, recovery ordinal, normalized structural-key hash, and application-rule
label. It records 19 `HARMFUL_FOR_RULE` and 15 `NO_BROKEN_REFERENCE` rows. These labels
are loaded only for post-assessment comparison.

`qualification.py plan` validates the current manifest and linked hashes, finds the
unique exact workload, derives the 12 participant-canonical vectors, and writes the
current immutable selection. Structural keys normalize each action as kind, Saga role,
runtime step, and compensation evidence class; package-local IDs are excluded from the
cross-version comparison.

The current package generated on 2026-09-06 has 17 persisted schedules after six
on-demand vector requests. The historical package had 34. Current analysis no longer
models the conservative unknown recovery actions on `getTournamentStep` and
`getUserStep`; therefore four current structural keys match historical rows exactly,
13 current keys are new, and 30 historical keys are absent. The runner executes the 17
current rows rather than fabricating 34.

`run-all.sh` performs no Docker build or pull. With `PREPARE_CLASSES=true`, it first
copies the three module source trees into `$OUTPUT_DIR/prepared-build/` and compiles only
those frozen copies once.
Each attempt then uses a separate disposable Compose container and fresh H2 database.
Set `PREPARE_CLASSES=false` only when a source-matched shared build has already been
prepared for this and the broader catalogue batch, and pass its host path through
`PREPARED_BUILD_DIR`.

```bash
PACKAGE_PATH=/absolute/path/to/scenario-catalog-manifest.json \
BROADER_SELECTION_PATH=/absolute/path/to/broader-selection.json \
OUTPUT_DIR=/absolute/path/under/verifiers/target/impact-v2-broader/run-01 \
SOURCE_REVISION="$(git rev-parse HEAD)" \
SOURCE_TREE_STATE=dirty \
MAX_PARALLEL=2 \
verifiers/experiments/impact-v2-broader/run-all.sh
```

The stock image does not need Python for this script because planning and aggregation
run on the host. If a combined in-container harness later needs Python, install it only
in that disposable container.

The fresh package uses the retained `quizzes-stale-read-baseline@1` provider. The current
application benchmark wrapper accepts only the newer 12-action source setup, so it cannot
observe this workload. Run 01 retains the wrapper's 17 selection rejections separately
from the 17 successful generic executions through `ScenarioExecutorCli`. Current runs use
the generic executor directly. A final broken-reference
cross-check may be derived from the ImpactV2 final snapshots, but it is labeled as using
the same observer and is not an independent application observation.

Run 01 predates the per-output default and used the sibling
`verifiers/target/impact-v2-broader/prepared-build/` directory. Its retained provenance
and runtime-source validation identify that build exactly.
