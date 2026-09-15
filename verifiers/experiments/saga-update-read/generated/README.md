# Generated update/read qualification

This experiment uses the existing source visitors and model adapter, prerequisite
scenario descriptors, order-preserving schedule generator, eager fault generator,
current package writer and ordinary ScenarioExecutor. It does not hand-edit schedules
or execute Saga actions itself.

`descriptor/` selects all six placements of FindTournament among UpdateTournament's
five ordered steps. Inputs come explicitly from the existing test-classpath
`quizzes-impact-v2-qualification@1` provider. This prepares a Tournament and an updated
DTO through application operations; it is not automatic extraction of these inputs.

The retained space-map campaign used `SERIAL` and one schedule per tuple. Its nine exact
FindTournament/UpdateTournament pairs have no setup and all 13 referenced input facts
are individually non-materializable. Merely changing the scheduling option is therefore
insufficient to execute those retained inputs. No generator capability was missing.

## Reproduction

From the repository root, with the hash-verified prepared runtime retained:

```sh
python3 verifiers/experiments/saga-update-read/generated/run.py \
  --output verifiers/target/saga-update-read/generated-new
```

An optional `--package PATH` reuses a previously generated package after validating its
manifest hashes, avoiding repeated source extraction. It does not regenerate inputs or
schedules. Every measured execution still uses a fresh JVM/Spring/H2 context, a fixed
`DateHandler.now()` of 2030-01-01T12:00, and local JSON serialization enabled.

`select_cases.py` freezes all six no-fault controls, followed by every generated scenario
whose sole assigned fault is the pre-body failure of `updateQuizStep`. There are eight
such fault schedules: when the read follows that fault in the forward schedule, it has
three possible positions around the two recovery checkpoints. The generated package
also retains other single-point faults that this qualification does not execute.

The runner verifies the retained production/build hashes, with an explicit current
source overlay for the diagnostic and the bounded prerequisite generic-type lookup fix.
It freezes measured sources and package evidence. `validate.py` independently predicts
the target anomaly from the persisted action order, checks actual action/fault/recovery
conformance, reader completion, exact revision delivery and the production sidecar.
Coverage gaps remain visible; a target read verdict is not a global absence claim.

See [handoff](../../../../issues/2026-09-09-generated-update-read/HANDOFF.md) for results,
failed development attempts, exact coverage and remaining work.
