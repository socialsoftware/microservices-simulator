# Live cross-workload allocator implementation brief

## Approved outcome

Connect the bounded cross-workload allocator to the existing isolated
`ScenarioExecutor` runtime. Keep the eight existing outer-policy labels, local GA
parameters, reward, seed derivation, missing-feedback behavior and one shared global
attempt budget unchanged. Workloads remain an explicit finite list backed by complete
catalogues; no unseen outcome enters selection or model state.

## Runtime boundary

Each workload declares its existing fixed-workload configuration, complete catalogue and
qualified no-fault control. Before dispatch, validate the catalogue seal, immutable source
package, frozen runtime descriptor and runtime file hashes, then require a `SUCCESS`/`EXACT`
control whose enabled weighted score is complete and zero. Reused control evidence is an
explicit qualification cost outside the live search budget. Dispatch sequentially through
the existing `Runtime.evaluate` one-candidate contract.

## Durability and resume

Create a new live-allocation command with `run`, `pause`, `status` and explicit ambiguous
dispatch resolution. Freeze allocation configuration, source files, workload/catalogue,
control, package and runtime identities in a protocol record. Hold an exclusive coordinator
lock. Persist selection before dispatch and a verified completion receipt before appending
the decision journal. On resume, validate every retained report bundle and deterministically
replay completed feedback through the unchanged allocator; refuse any mismatch.

A dispatch that has no verified completion after interruption is ambiguous. Resume may
salvage a complete, valid `attempt.json`; otherwise it stops without retrying. An operator
may explicitly mark that dispatch spent, which records unavailable feedback, consumes one
global attempt, and permits deterministic continuation without a second application run.
Malformed/torn protocol, dispatch, receipt or journal data is an integrity failure.

Pause and signals take effect only between completed sequential attempts. A clean pause
therefore has no pending allocator `ask`. Status and the decision journal are refreshed after each completed decision. Results and
the human summary are written at pause/completion.

## Proof

Add mocked-runtime contract tests for qualification, successful sequential execution,
unknown feedback, pause/resume trajectory equality, completed-attempt salvage, ambiguous
interruption refusal and explicit spent resolution, concurrent coordinator exclusion, and
tampered durable state. Run only the focused Python test suite; Docker/runtime smoke is a
separate bounded check because the local daemon is unavailable.

## Implementation and independent review

Implemented in `live_allocate.py`; see [verified scope and completed Docker smoke](../evidence/live-allocation-2026-09-21/RESULTS.md). Counters distinguish dispatches and verified attempt results from proven application executions. Failure or timeout results can consume budget without proving that the application completed.

The authorised four-attempt Docker smoke subsequently passed: scores 0/1/1/0, two workloads, no unavailable enabled scores, pause after one and three new attempts after resume. A completed-run resume issued zero dispatches. Full verification and scope are in the evidence report above.
