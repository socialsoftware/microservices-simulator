# M0 — Baseline and execution boundary

State: complete.

The starting checkout was clean at `36784346d1f5e988ad612132b6ed9f5db344ee78`.
A fresh `mvn -q test` from `verifiers/` passed 743 tests with zero failures,
errors, or skips; log: `verifiers/target/astra-baseline-tests.log`.

The recorded single-input baseline is 796 accepted inputs: 559 with source setup,
one needing no setup, and 236 blocked (three rejected plans, 175 partial bindings,
58 missing bindings). The latest reduced writer package contains 1,620 workloads;
1,338 reference source-derived setups. The 402-workload runtime report instead
belongs to the earlier `event-route-proof/quizzes-20260904-021015-239` package.

Implementation is authorized autonomously by the user. Luna/max owns the separate
direct preflight aggregation fix; Sol/medium implements the documented feature-prefix
slice in an isolated worktree; the coordinator implements the bounded property path,
reviews source-prefix correctness, integrates reviewed diffs, and owns Docker proof
and canonical documentation. No push, merge, or release is authorized or performed.

The preflight aggregation fix was independently inspected and integrated as a direct
change, without a separate issue package. It passed 15 focused tests. A qualification-only
driver selected exactly the seven formerly rejected workloads from their unchanged
package and used the real parent orchestrator and fresh Docker JVM/Spring/H2 workers.
All seven passed in 109.23 seconds, with empty binding arrays, successful setup/startup,
and unchanged package hashes. Evidence and the driver are under
`verifiers/target/astra-qualification/`. This does not relabel the historical 395/402
report or establish runtime readiness for the newer package.

The first qualification-driver launch inherited Compose's `SPRING_PROFILES` variable,
which Spring rejects; the driver was corrected to unset it as the normal launcher does.
The failed launch report/log are retained separately as `state-only-first-launch.*`.

Product boundary: recover source preparation without synthesizing cross-test state.
Feature prefixes must exclude the selected target, respect exact occurrence identity,
and stop at unsupported control-flow/workflow/assertion barriers. A singleton uses its
own cutoff; a tuple uses the earliest selected target cutoff and must reject unselected
effects between participants rather than omit or hoist them. Fixture-only behavior
remains the baseline; this issue does not claim full test-method replay.
