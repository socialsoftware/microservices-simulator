# Order context and hybrid allocation

This bounded study separates the choice of allocation model from adding ordinary-order
information. It uses previously verified complete feedback maps; it executes no application
and changes neither the production allocator defaults nor the manuscript.

The frozen protocol is in `docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21/`.
It compares seven policies on the complete earlier 15-workload and 66-workload collections,
separately. Every arm begins without training or GA history, uses all five unit weights,
256 global selections, the same local GA and seeds 1–30. The two new comparisons are
shared-order versus shared-structure, and hybrid-original versus shared-structure.
There is no combined hybrid-order arm or hyperparameter search.

Both collections and their outcomes were previously examined. The second collection
checks the new comparisons in a different group; it is not untouched test data. Candidate
selection remains blind to unselected outcomes. Unknowns consume budget and never become
zero labels. The changed policies are experimental injected policies, not new live modes.

Run from the repository root into a fresh output directory for reproduction:

```sh
python3 verifiers/experiments/allocator-order-hybrid/run_study.py \
  --arms round-robin uniform independent progress structural \
  --output verifiers/target/allocator-order-hybrid-reproduction
python3 verifiers/experiments/allocator-order-hybrid/run_study.py --arms order hybrid \
  --output verifiers/target/allocator-order-hybrid-reproduction
```

Use the existing plotting Python environment for PNG/SVG output when matplotlib is absent
from the system Python. Outputs are compressed per-seed traces with SHA256 receipts;
input and source identity must match on resume. Preserve the original retained references.
No application logs or large archives are duplicated. The measured selection/update time
excludes loading, reference lookup and application execution; it is descriptive local
Python overhead, not a controlled end-to-end runtime benchmark.

The baseline seed-1 structural and progress decisions are independently matched against
the previously checked original allocator cold traces: all 256 candidate identities,
operators and rewards agree. `test_study.py` additionally checks fresh state, exhaustion,
pre-feedback context and unavailable feedback without learning.

The retained results and figure are linked from the [evidence report](../../../docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21/RESULTS.md). The summarizer defaults to the original evidence directory; adapt its RUNS/OUT for a distinct reproduction. The original baseline runner snapshot is preserved with the report because the subsequent runner added pinning of the order metadata hash; existing receipts deliberately refuse changed source identities.
