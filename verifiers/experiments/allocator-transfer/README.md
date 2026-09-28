# Transfer between workloads

This recorded-feedback experiment tests whether earlier observations help allocation among
new combinations of known Sagas. It does not run Docker or change the live allocator.

The frozen 21 September protocol selects 66 one/two-Saga workloads for a common
132-decision training prefix and 15 three-Saga workloads for 256 target decisions.
Structural and progress-only linear UCB each run with and without the learned model state.
Every target local GA starts empty. All five criteria have weight one; no cooldown is used.
The combined structural vocabulary is known before execution, but target scores are hidden
until selected. Training observations are reported separately from target discovery curves.
Cold arms discard the same prior history: this is a transfer ablation, not a comparison of
standalone deployment cost.

From the repository root:

```sh
python3 verifiers/experiments/allocator-transfer/run_study.py \
  --protocol docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/protocol.json \
  --inputs docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/inputs.json \
  --output verifiers/target/allocator-transfer-2026-09-21/runs
```

`--seeds 1` runs one protocol seed for review. Existing verified seed results are reused;
changed input/source identity or invalid receipts refuse continuation. The manifest refers
to retained local artifacts and hashes; unavailable originals must be restored to those
paths before reproduction. No large archive is extracted automatically.

`inputs.py` validates canonical vector/request/candidate coverage for 78 maps and uses the
previous verified batch provenance for three creation maps whose full enumeration is not
local. All 1,511 stored candidate/feedback/fitness records are validated with the existing
`RecordedFeedbackEvaluator`. It reconstructs structural profiles from matching retained
interaction records. No missing interaction is guessed or silently omitted.

Plotting uses a Python environment with NumPy and Matplotlib:

```sh
python verifiers/experiments/allocator-transfer/summarize.py \
  --runs verifiers/target/allocator-transfer-2026-09-21/runs \
  --output docs/verifiers-impl/evidence/allocator-transfer-2026-09-21 \
  --protocol docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/protocol.json
```

The transfer loop and tests live in `../fixed-workload-ga/transfer.py` and
`test_transfer.py`. The evidence report owns the measured result and its limits.
