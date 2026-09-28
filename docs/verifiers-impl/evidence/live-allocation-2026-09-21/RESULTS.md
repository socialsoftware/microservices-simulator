# Live allocation: implementation verification, 21 September 2026

## Implemented

The [live command](../../../../verifiers/experiments/fixed-workload-ga/README.md#cross-workload-live-allocation) connects the existing allocator to the ScenarioExecutor adapter, one dispatch at a time. Budget, seed, weight profile and policy are explicit and fixed for a run. Each workload retains its own GA state. The missing-feedback rule remains optional.

Inputs require sealed complete catalogues, a frozen runtime and qualified retained no-fault controls. Control summaries must match their original reports; enabled criteria must have complete zero scores with SUCCESS/EXACT execution. Reused controls are outside the search budget. No application-specific changes were made for this integration.

Dispatch records precede execution. Verified result receipts precede the decision journal. Pause drains the current attempt; resume validates evidence and reconstructs the same model and GA states without repeating completed attempts. Missing results after an interrupted dispatch require explicit resolution as spent/unavailable. Surviving executor containers block continuation; failed Docker-client calls do not establish that execution stopped. Output counts dispatches and verified attempt results, not presumed completed application executions.

## Verification

- The fixed-workload-ga Python suite passes **107 tests**, including 15 live-runner test methods.
- Root independently checked **24 contracts across all eight policies**, with three workloads, pauses after decisions 1, 6 and 13, and budget 21. Resumed decisions, model and local GA state equal uninterrupted execution. Missing journal rows are recovered from receipts without new dispatch; changed configuration refuses resume. The executor backend was deterministic and mocked. See [proof](resume-matrix-proof.json).
- The production input loader independently verified two retained real catalogues, with **72 and 12 candidates**, under the deleted-dependency profile. It rejected a tampered control summary and correctly rejected the second control under all five criteria because its read coverage is partial. Original evidence was unchanged. See [input checks](real-input-verification.json).
- Independent review reproduced and resolved unsafe continuation past a surviving executor and qualification from a modified control summary. Runtime/source validation now also covers transitive assessment helpers and runtime checks before each new dispatch.
- `git diff --check` passes. [Source hashes](source-hashes.json) identify the reviewed implementation.

## Real Docker smoke — complete

After the authorised Docker restart, the live command completed **four distinct application
attempts across two workloads**, with the contextual policy and deleted-dependency profile.
Scores in decision order were **0, 1, 1, 0**: two positive, two zero, none unavailable.
The first workload received one attempt and the second three; all four feedback values
updated the model. The positive reports include an active Tournament referring to a Quiz
deleted during execution. These two positive scenarios are not a count of distinct bugs.

All four executions followed the selected schedule exactly. Three terminated COMPENSATED
and one PARTIAL_COMPENSATED; the enabled dependency criterion was completely assessed in
all four. Assessment completeness is distinct from successful compensation.

The runner was paused after decision 1, then resumed for exactly three new attempts. A
second resume after completion dispatched nothing. Independent verification checked 24
report hashes, dispatch/receipt hashes, candidate identities, recomputed weighted scores,
four distinct attempt directories, and unchanged first-decision evidence across resume.
See [compact proof](live-smoke-verification.json). The two retained qualification controls
were verified and reused outside this four-attempt budget; no fresh controls were run.

This verifies live routing, feedback and pause/resume for the bounded integration. It is
not an effectiveness comparison or qualification of every workload/profile. The run uses
its frozen runtime, without rebuilding the application. No paper changes were made.

Detailed local review scripts remain under `verifiers/target/live-allocator-review-2026-09-21/`; compact proofs above are retained outside generated output.
