# Synchronous Saga copy transport qualification — 28 September 2026

The generic observer and assessor now retain copies constructed between synchronous
Saga/local commands, including transport with serialization enabled or disabled.
The fixed selection contains all 270 synchronous command-scope-only cases; four
explicitly asynchronous cases were excluded before new outcomes were observed.

| Final cohort verdict | Positive | Negative | Unavailable |
| --- | ---: | ---: | ---: |
| `LOST_COPIED_UPDATE` | 0 | 269 | 1 |
| Joint fitness | 56 | 213 | 1 |

All 270 cases were attempted through Docker Compose: 269 completed, and one failed
before application startup because Mockito/ByteBuddy could not self-attach. That
case remains unavailable and was not selectively retried. Other criterion counts
and execution outcomes are unchanged across all 269 completed executions.

Validation passed: 57 focused tests and ten real forward/recovery controls
(four positives, six negatives), covering both serialization modes. Review caught
and fixed an in-command return regression before the final campaign; the initial
32-case campaign and its summaries remain separate. The final campaign reran the
entire fixed selection without carrying over reward-selected results.

The audit verified seven historical references and 1,620 retained report hashes,
plus replay, runtime and package identities. Frozen maps, historical unknowns and
bandit replays remain unchanged. Async/thread provenance and duplicate identities
remain outside this extension.

Evidence: [selection](selection.json), [results](results.json), [audit](audit.json),
[controls](regression-controls.json), [review](review.json), and
[startup failure](startup-failure.json). Raw attempts are retained under
`verifiers/target/saga-copy-transport-2026-09-28-final/`; the resumable runner and
runtime-isolation details are in [the experiment guide](../../../../verifiers/experiments/saga-copy-transport/README.md).
