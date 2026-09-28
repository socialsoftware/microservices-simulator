# Questions fixed before the follow-up comparisons

This is a bounded continuation on previously examined complete maps. It is not an
untouched test set or a search for the best hyperparameters. No further application
executions, workload exclusions, feature redesign or parameter tuning are planned.

1. Does adding ordinary-order context help a hybrid model, after it already has
   private per-workload progress coefficients? Compare hybrid+order with hybrid.
2. Does private adaptation help when ordinary-order context is already present?
   Compare hybrid+order with shared order. Together these comparisons complete the
   structure/order × shared/hybrid comparison without changing the local GA.
3. Is the resulting gain useful beside independent adaptation and shared progress,
   which need fewer structural features? Keep both alternatives in every comparison.
4. Does the answer depend on the available budget and the user's preference?
   Retain all previously fixed checkpoints (16, 32, 64, 128, 256); 256 remains the
   primary endpoint. Repeat from empty state for a preference giving unit weight to
   deleted dependencies and compensated reads, zero to the other three criteria.
   Recompute both GA feedback and outer-model feedback, not merely the plotted metric.

The profile is selected from criterion support before the new search results. The
component inventory records this choice: reads alone are all zero in one collection,
whereas deleted dependencies alone are all zero in the other. Their pair expresses a
preference for dependency consistency and reads exposed to compensation. It gives 76
positive scenarios among 1,026 in the three-Saga collection and six among 485 in the
one/two-Saga collection. Neither allocation policy receives these totals or labels
before selecting a scenario. Keep the flat workloads; do not select a subset of positives.

Use paired seed differences with intervals, positive discoveries, accumulated weighted
impact and complete budget curves. Report both wins and losses. A change in the leading
method across groups is evidence of conditional performance, not a rule for selecting
that method on a new workload. The experiment allocates across collections of workloads;
it does not independently establish a best allocator for each workload.

Diagnostics may explain where choices went: number of workloads visited, choices spent
in workloads containing a positive for the selected profile, and choices within each
Saga family. Those are retrospective descriptions, never selection features or causal
proof. Existing traces can support smaller budgets because the policy is horizon-blind
and the local population remains eight at every checkpoint; verify this with a narrow
prefix check. Record local decision overhead as descriptive bookkeeping cost, without
claiming an end-to-end timing benchmark.

No model is preferred merely because its mean is highest. Check uncertainty, magnitude,
simplicity and stability. Do not infer that an interval containing zero proves equality.
