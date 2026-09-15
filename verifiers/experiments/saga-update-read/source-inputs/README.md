# Source-derived Tournament update/read qualification

Uses the existing `UpdateTournamentTest#update tournament successfully` inputs for
FindTournament and UpdateTournament. Selection is by exact source class/method, Saga
mode and Saga type, before outcomes. No provider-bound input replacement and no Quizzes
source edit. The existing generator enumerates all six read placements and writes an
ordinary source-setup package.

The runner freezes an explicit reviewed Java source overlay over the hash-verified
prepared simulator/application runtime, compiles it in Docker, extracts source facts,
writes the package, and executes fresh JVM/Spring/H2 attempts with a fixed clock and
local JSON serialization. The existing selection rule retains six no-fault controls and
eight final-step-fault schedules, including recovery placement alternatives.

```sh
python3 verifiers/experiments/saga-update-read/source-inputs/run.py \
  --overlay issues/2026-09-09-source-update-read-inputs/source-overlay.json \
  --output verifiers/target/saga-update-read/source-input-new
```

The overlay is a list of current verifier/simulator Java sources explicitly qualified with this
change. Drift in other production files or the prepared runtime is rejected. The runner
also checks source/package integrity after execution. Failed generations/executions are
retained; no retry substitutes a different input or fault.

The validator checks ordinary source setup, exact scenario identity and action order,
assigned failure before updateQuizStep, reader completion and diagnostic evidence. It
also checks the effective input through the actual persisted Tournament update: initial
2 questions at 12:05–13:05, requested 3 questions at 12:25–13:25 (the existing test constants
at the fixed 2030-01-01T12:00 clock). Recovery must restore the original dates/count.
The separate topic course-ID recovery defect is preserved, not repaired by input work.

The read-scope correction uses the same generated package and inputs. Its overlay adds
only `ReadResponseObservation` to the previously qualified verifier sources:

```sh
python3 verifiers/experiments/saga-update-read/source-inputs/run.py \
  --overlay docs/verifiers-impl/evidence/read-scope-2026-09-10/overlay.json \
  --output verifiers/target/saga-update-read/read-scope-new
python3 verifiers/experiments/saga-update-read/source-inputs/compare-read-scope.py \
  verifiers/target/saga-update-read/source-input-02 \
  verifiers/target/saga-update-read/read-scope-new
```

The current validator additionally requires complete read coverage with no gaps in all
14 histories. The original source-input qualification retains its frozen validator and
six partial controls; do not apply the stricter expectation retrospectively. The comparison
checks package byte equality, all execution/ImpactV1/ImpactV2 report pairs, unchanged
read findings and the exact six CommitSagaCommand classification changes. It normalizes
only attempt IDs, source-setup duration and the setup binding's default TournamentDto
Object.toString identity, leaving application projections intact.

`--generation-only` freezes and checks a new package without executing it. It is useful
for a mapper-only review correction: previously executed histories can be reused only
when every manifest-listed package artifact is byte-identical and runtime sources/build
are unchanged. This mode itself supplies no runtime results.
