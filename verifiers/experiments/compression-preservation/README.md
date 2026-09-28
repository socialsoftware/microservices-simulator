# Runtime preservation under segment compression

This experiment compares detector observations over all forward orders with the
subset selected by `SEGMENT_COMPRESSED`. It does not change the application,
production generator, executor or impact policy.

## Fixed design

- Quizzes `UpdateTournament` and `FindTournament`, using the two original input
  recipes from `UpdateTournamentTest#update tournament successfully`.
- Source-derived setup only; no prerequisite provider or manually assigned IDs.
- The same broad conflict lens, inputs and setup for both scheduling strategies.
- All six forward orders versus the three selected by segment compression.
- No selected event consequences. The application can still emit events; this is
  not an evaluation of event-delivery ordering or its preservation.
- Every canonical fault assignment: no fault or one forward-step fault per Saga,
  including simultaneous assignments in both Sagas. All recovery schedules emitted
  for those assignments are included. The recovery cap of 10,000 is a guard:
  generation fails if any vector is truncated.
- The generated catalogues contain 72 workload/vector combinations and 98 scenarios
  for the full set; 36 combinations and 40 scenarios for the compressed set.
- Compressed scenarios must be exact members of the full catalogue. Each scenario
  is executed once in a fresh process/database. This supplies both comparisons
  without executing the retained subset twice.
- Six no-fault controls execute first. Every control must succeed with exact
  schedule conformance and complete zero counts for all five impact criteria.
- One additional Docker worker, one CPU limit, 3 GiB memory, existing frozen
  executor image/classpath/agent and fixed clock. No rebuild or mutation of the
  concurrent 5,184-scenario campaign. Attempt timeout: 240 seconds. Stop below
  4 GiB free disk; infrastructure/report failures stop for diagnosis.

The source pair is selected because its ordinary preparation is already qualified
and it contains a known update/read interaction. It is a purposeful case study,
not a random representative sample. The experiment evaluates all generated faults
within this declared scope, rather than only the known positive fault.

## Comparison

Group by the named Saga/step fault assignment, not raw vector positions (positions
change with forward order). Compare both individual finding signatures and the
combinations of findings seen together in an execution. Record unknown observations
separately and require complete observations before claiming preservation for a group.

Persistent signatures retain category, reason, affected/related objects, originating
operations and observed before/after application projections. Read signatures retain
object identity, producer, reader, source step and restored/non-restored attributes.
Attempt IDs and evidence version counters are excluded. Object IDs remain present;
the observed initial application projections must agree across attempts.

Final-state projections are an additional comparison, not a harm oracle. Application
data, lifecycle and dependencies are retained; framework row/version/lock metadata
is excluded. Nested application version fields remain, so a state difference needs
inspection before interpreting it as a lost harmful outcome.

The chosen pair is not expected to produce lost-copied-update findings. An unexpected
positive stops automatic comparison rather than discarding an unmodelled signature.
Raw reports are retained for all findings, failures and incomplete observations.

This is one observation per scenario, without a determinism claim. Preservation of
these observations does not establish preservation of all application harms, other
inputs, other fault models, selected event deliveries or arbitrary interleavings.

## Reproduction

The preparation command performs the source-hash check and freezes both catalogues:

```sh
python3 verifiers/experiments/compression-preservation/prepare.py \
  --build verifiers/target/rq1-static-pilot-2026-09-18 \
  --runtime-config verifiers/target/full-map-5184-2026-09-17/config.json \
  --output verifiers/target/compression-preservation-NEW
```

Read the source/build fingerprints in the output's `generation-manifest.json`.
Compile `GenerateComparison.java` against a matching verifier build and dependencies,
then run with Java 21:

```text
GenerateComparison <repository>/applications quizzes <new-output-directory>
```

The completed static-comparison build is one qualified starting point:
`verifiers/target/rq1-static-pilot-2026-09-18/classpath.txt`. Verify its
`build-proof.json` source hashes before use. Do not rebuild running campaign jars.
The generator writes independent `full` and `compressed` packages, exact vector
accounting and `selection.json`. Preserve its logs, compiled harness and source copy.

Provide a frozen runtime descriptor at `<output>/run/runtime.json`. This run copies
the existing 5,184-campaign descriptor, changing only the CPU limit to one. The
runner verifies every descriptor hash and uses the same fixed-clock executor.

```sh
python3 verifiers/experiments/compression-preservation/run.py \
  verifiers/target/compression-preservation-2026-09-18 --controls-only

# After controls pass; also the resume command for an interrupted measurement:
python3 verifiers/experiments/compression-preservation/run.py \
  verifiers/target/compression-preservation-2026-09-18
```

The exclusive lock prevents duplicate workers. Completed attempts are hash-verified
and reused. An interrupted attempt is archived before rerunning that same scenario;
it is never silently replaced by another input or fault. A failed completed control
continues to block the run on resume.

Read `<output>/run/status.json` for progress and `<output>/run/RESULTS.md` for the
evolving comparison. `comparison.json` contains evidence signatures and source
scenario IDs. Differences during a partially measured group are provisional.

The runner seals its measurement/analysis code and runtime descriptor before the
first observation. Reanalysis with changed definitions must be explicitly identified
as a separate analysis rather than overwriting the frozen comparison.
