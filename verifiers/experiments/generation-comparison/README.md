# Quizzes generation comparison

This experiment evaluates the current static forward-scheduling model. It does not
execute application scenarios or enumerate fault vectors, compensation schedules,
event consequences, or prerequisite expansion. Counts are input-bound forward
schedule shapes, not executable FaultScenarios or confirmed bugs.

## Frozen design

- Target: current Quizzes sources, fingerprinted with the verifier and simulator sources.
- Every combination of 2, 3 and 4 distinct Saga types with accepted source inputs.
  Repeated instances of the same Saga type are outside the generator's set model.
- Production input policy `RESOLVED_OR_REPLAYABLE`, Saga source-mode filtering,
  deterministic input ID order; repeat with explicit input caps (default 1 and 3 per Saga).
- Tuple selection: `ALL` (brute force), `STRICT`, `WITH_TYPE_ONLY_FALLBACK`.
  The last mode includes strict evidence; it is not exclusively type-only matches.
- Scheduling: full order-preserving interleavings versus conflict-anchor segment
  compression. Compare these on the SAME selected tuples and conflict lens.
- Compute uncapped counts with arbitrary-precision multinomials. Use production
  `InputTupleSelection.count` and `ConflictGraphBuilder`. Schedule anchors are
  restricted to each Saga set, matching `ScenarioGenerator` (not filtered down to
  input-specific interaction evidence). Compare with the normal accounting output
  for all pairs and six configurations, including its 5,000-order cap.
- No claim about runtime, memory, serializability or preservation of every harmful
  execution. Only the orders covered by the extracted static conflict model.

## Enumeration verification

At the largest requested input cap, consider sets with selected tuples, at most 20,000 full orders and
at most 200,000 full-order step occurrences. Stratify by Saga count, strict/broad
conflict lens and full-order size (1–100, 101–1,000, 1,001–20,000). Retain up to two
sets per stratum using the smallest SHA-256 of the ordered Saga FQNs. This selects
without using compression ratio or impact. Empty strata stay empty.

Persist the chosen sets before verification. Call the production `ScheduleEnumerator`
for both strategies, completely and without truncation. Assert:

1. Enumerated counts equal the uncapped mathematical counts.
2. No duplicate schedules; every Saga retains all its steps in its original order.
3. Project each full schedule onto its conflict-anchor steps. The SET of complete
   anchor-order sequences equals the set produced by compressed schedules.

This checks whole anchor-order sequences, not just independent pair orientations.
Record every sampled case and result. No silent retry or dropping failed cases.
The global counts include unsupported setup recipes; accepted input is not a claim
that application execution is ready.

## Running

The runner takes repository root, a dedicated existing verifier build directory
(containing `classpath.txt` and `build-proof.json`), and a NEW output directory:

```
python3 verifiers/experiments/generation-comparison/run.py \
  --build verifiers/target/rq1-static-pilot-2026-09-18 \
  --output verifiers/target/generation-comparison-<run-id> \
  --plot-python /path/to/python-with-matplotlib
```

It checks that the isolated verifier build matches current Java sources, compiles
only this experiment's harness into its output directory, runs at low priority
with a 1.5 GiB heap and two JVM-visible processors, then creates the report and plots.
It never rebuilds or modifies the running runtime campaign's dependencies.
`run-status.json` owns overall completion/failure; `status.json` reports the Java
stage. Keep raw JSONL and source/build manifests for reproduction. Wall-clock logs
are operational only, not performance benchmark results. If interrupted, retain
the incomplete output and rerun into a new directory.

To measure input-cap sensitivity locally, add `--input-caps 1 3 10`. Counts and input
identities are recorded for each cap; enumeration and production-accounting checks
use the largest requested cap. The generated selection/compression figures also use
that largest cap. Existing evidence directories are never overwritten.

## Current three-variant recount

`ThreeVariantCounts.java` uses `GenerationComparison` only for extraction and bounded
enumeration checks (`extract-only` exits before its historical counting path). The
current harness uses `buildSelectionGraph`, tuple-applicable anchors and uncapped
accepted inputs, and independently checks production accounting on all pairs with one
input per Saga. The historical main counting path above belongs to the earlier study.

Compile both Java classes with the current verifier classpath, then run:

```sh
java -Xmx1500m -cp "<harness-classes>:<verifier-classpath>" ThreeVariantCounts \
  "$PWD/applications" quizzes "<new-output-directory>"
```

Repeat for `quizzes-full` and `quizzes-full-2`. The harness rejects exact logical input
bindings because its multiplication shortcut is valid only for the explicitly checked
input-independent broad-selection case. Counts cover normal step orders of two through
four distinct Saga types, not event placement, fault/recovery expansion or executable cases.
See `docs/verifiers-impl/evidence/three-variants-2026-09-20/RESULTS.md`.
