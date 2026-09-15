# Lost copied updates in ordinary execution

## Result

The generic observer and assessor now run in ordinary Saga/local ScenarioExecutor attempts.
Source analysis infers the supported copy relationships; Quizzes supplies no manual mapping
and its business source is unchanged. One normal application test supplies a useful input
history. The optional fifth fitness criterion is `LOST_COPIED_UPDATE`.

The controlled qualification reproduces **four positive executions and six controls** across
local serialization off/on, with no copied-update coverage gaps. The ordinary generated
qualification separately establishes a forward positive, a fresh-input control and a
recovery positive, all with exact schedule conformance and complete copied-update coverage.
These are observations within the declared copy scope, not an exhaustive lost-update detector.

## What actually happened

A updates a Tournament. It reads Topic data and keeps the returned DTOs. B renames one of
those Topics; its published event is delivered to the Tournament's handler, which commits
the new name into the Tournament's embedded Topic copy. A then constructs and persists its
Tournament update from the earlier DTO. The name in the Tournament changes back to the old
value, while the original Topic keeps its new name.

In the recovery case A first saves the original Tournament DTO. After the event updates its
Topic copy, an injected fault prevents the Quiz update and compensation reuses the saved DTO.
The same evidence rule identifies that recovery write overwriting the intervening change.

The observer proves the actual response, outgoing input, exact inbound transport link,
constructor invocation, registered object path and transaction-confirmed version. An object
with equal values but no tracked origin is not guessed to be the earlier DTO.

## Generated cases

Input comes from the new ordinary test
`UpdateTournamentTest.'update topic and tournament successfully'`: two functionality calls
with the existing source-derived setup. The generator emits 21 base interleavings. Event
expansion is bounded by a 100 persisted-workload cap; this is not a total application count.
The optional copy-contract artifact is included in the package manifest and its hashes.

| Retained attempt | Relevant order | Copied updates | Existing measurements |
|---|---|---:|---|
| 002 | A reads old Topic; B updates; event updates Tournament; A writes old copy | 1, FORWARD | I=0 and read A=0, complete |
| 003 | B updates and event runs before A reads | 0 | I=0; read count 0 has partial coverage |
| 005 | A saves original; event updates Tournament; fault; A compensates with original | 1, RECOVERY | I unavailable under the existing competing-writer residual rule; read A=0 |

The forward positive demonstrates new feedback: all four earlier criteria have complete
zero counts, while the new criterion has one proved occurrence. `SUCCESS` is the execution
outcome in that case; no fault is needed for a stale copied overwrite.

Two exploratory controls remain in the raw directory: 001 renames the source without an
intervening event write to the Tournament (zero), and 004 uses an initial fault selection
that does not exercise Tournament-update compensation (zero). They are not additional
positive claims. All exact IDs, fault vectors, findings and report hashes are retained in
[generated-cases.json](generated-cases.json).

**Static precision limit:** strict selection produced zero tuples because the Topic identity
inside UpdateTournament's input collection is not represented in its symbolic key evidence.
The qualification explicitly enables the existing `allowTypeOnlyFallback` option. Runtime
execution proves the concrete shared identity; the static match must not be called an exact
instance proof. No generator matching rule was widened for this experiment.

## Count and fitness

One overwriting committed version of one aggregate counts once; multiple proved overwritten
fields appear inside that occurrence. Different overwriting versions count separately.
Forward and recovery phases remain visible in the evidence. The historical I and read A
fields keep their original definitions.

[weighted-feedback.json](weighted-feedback.json) reuses retained reports with policy
`weighted-criteria-v2`: the first four weights are zero and `LOST_COPIED_UPDATE` has weight
one. The forward/control/recovery scores are **1 / 0 / 1**, with no additional application
executions. These illustrative weights isolate the new component; they do not prescribe
user priorities. An enabled criterion with incomplete evidence makes fitness unavailable;
disabled incomplete criteria do not block the score. Existing v1 and legacy policies remain
unchanged. Unit tests verify the resulting parent-selection preference and unchanged random
selection. This is not a new GA-versus-random campaign or a claim of improved search performance.

## Validation and cost

- 42 focused inference/package regression tests, including nine inferred Quizzes contracts.
- 11 attempt-session tests; 19 generic assessor tests, including evidence removal, aliases,
  transaction rollback, full-identity collisions and grouped counting.
- Existing read gateway tests: 23; read assessor tests: 57; ScenarioExecutor tests: 200.
- Python fitness/search/worker suite: 47 tests. Quizzes UpdateTournamentTest: 13 tests.
- [controlled-validation.json](controlled-validation.json) compares all ten runs with the
  uninstrumented histories: initial/final business observations and committed identities,
  revisions, writers, lifecycle, dependencies and business data match. Framework metadata
  and top-level `creationDate`/`lastModifiedTime` wall-clock audit fields are excluded.
- [non-interference.json](non-interference.json) records three additional ordinary generated
  replays with the copied-update observer disabled. Execution outcomes, I/read counts and
  coverage reasons, and committed/final application data match the enabled runs exactly.
  The fresh control retains its prior read gap; the recovery case retains its prior I gap.
- [cost-check.json](cost-check.json) contains two fresh-process timing pairs. Disabled/enabled
  times are 13.711/14.276 seconds, then 14.309/14.053 with order reversed. Business observations
  match. Timing includes Docker/JVM/Spring startup and does not establish isolated overhead
  or scaling. Controlled trace sizes are 52,354–65,819 bytes in compact JSON; not peak heap.

## Reproduction and raw evidence

Implementation/qualification helpers: [experiment directory](../../../../verifiers/experiments/lost-copied-update/).
Build the simulator with `mvn install` in `simulator/`, then `mvn package` in `verifiers/`.
The scripts use the earlier frozen Quizzes runtime and verify the application source hashes;
they freeze the current simulator/verifier classes for the new observer and assessor.

```bash
python3 verifiers/experiments/lost-copied-update/qualify.py \
  --output verifiers/target/lost-copied-update/my-qualification
python3 verifiers/experiments/lost-copied-update/validate.py \
  --run verifiers/target/lost-copied-update/my-qualification \
  --baseline verifiers/target/stale-write/run-01
python3 verifiers/experiments/lost-copied-update/measure.py \
  --run verifiers/target/lost-copied-update/my-qualification
```

For generated-package reproduction and the three accepted case selections, use the
[bounded generation/replay commands](../../../../verifiers/experiments/lost-copied-update/M3-BOUNDED.md).

Raw directories under `verifiers/target/lost-copied-update/`:

- `integration-03/`: ten controlled histories, frozen sources/classes/runtime, exact commands,
  inferred contracts, raw traces, validation and paired cost check.
- `generated-positive-discovery-01/current-package/`: generated package and source-derived setup.
- `generated-positive-noninterference-01/`: disabled-observer replays and full normalized comparison.
- `generated-positive-qualification-01/`: ordinary execution, impact/read/copy sidecars,
  per-attempt package snapshots, replay metadata and weighted rescoring proof.

The scope excludes computed replacements, arbitrary clones, general setter-based inference,
remote RPC and arbitrary concurrent-thread tracing. The immediate-predecessor rule is
conservative: it does not reconstruct all historical field writers across unrelated writes.
