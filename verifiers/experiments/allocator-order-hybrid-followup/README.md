# Allocator order/hybrid follow-up

This directory is an isolated continuation of `allocator-order-hybrid`. It adds one
arm, `hybrid-order`, and repeats the fixed eight-arm comparison under the frozen
`dependency-and-read` reward profile. It does not change allocator defaults, the
recorded maps, the local GA, or application code.

`hybrid-order` uses original structural coordinates and the seven audited type-level
order coordinates as its shared vector. It removes every progress coordinate from
that vector. Its private vector is the unchanged `ProgressFeatureSpace`, including a
separate local bias and separate coefficients for each workload. The implementation
uses the already validated `HybridLinUcbPolicy` and `OrderFeatureSpace` modules.

The runner passes the selected profile to `_new_states`, which constructs every
`SearchSession` with that fitness. Recorded observations are therefore reassessed
before both GA parent selection and bandit feedback. Null feedback consumes budget,
does not become a GA parent, and does not update the allocation model.

The frozen design is in
`docs/verifiers-impl/evidence/allocator-order-hybrid-followup-2026-09-21/protocol.json`.
The preceding all-five traces for seven arms remain in their original target folder.
`provenance.py` verifies their protocol, run identities, receipts, trace hashes and
aggregate seal before any run or summary; it never copies or rewrites them.

After the frozen manifest is present, run the complete fresh matrix with:

```sh
python3 verifiers/experiments/allocator-order-hybrid-followup/run_study.py
```

This schedules `hybrid-order` for all-five and all eight arms for
dependency-and-read, using seeds 1–30 and 256 attempts. Narrow resumptions may select
only combinations already allowed by `freshRuns`; the runner rejects attempts to
rerun the seven reused all-five arms. Generate the complete paired summary only after
all required traces exist:

```sh
python3 verifiers/experiments/allocator-order-hybrid-followup/summarize.py
```

The 256-attempt cumulative weighted impact is primary. The 16, 32, 64 and 128
checkpoints are retained as horizon sensitivity and are never substitutes for the
primary endpoint. Raw scores from the two different reward profiles are not directly
comparable.

The [verified report](../../../docs/verifiers-impl/evidence/allocator-order-hybrid-followup-2026-09-21/RESULTS.md)
contains the final comparison and its interpretation. `independent_review.py` checks
every selected reward directly against the recorded criterion counts and compares local
GA prefixes across methods. `diagnostics.py` describes where the budget went; its
retrospective positive-workload labels are never passed to the search. `plot_results.py`
requires Matplotlib and produces the compact figure. All three are standalone review
scripts, separate from the frozen search dependencies.
