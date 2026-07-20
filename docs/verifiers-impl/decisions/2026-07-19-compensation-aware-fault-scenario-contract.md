# Compensation-aware fault scenario contract

Date: 2026-07-19

Status: accepted for the v3 catalog feature; not yet implemented

The verifier will replace the v2 `ScenarioPlan` contract with a clean two-level v3 catalog: a reusable `WorkloadPlan` captures participants, inputs, the normal forward interleaving, conflict evidence, and fault slots, while each persisted `FaultScenario` adds one assigned vector and one concrete compensation-aware action schedule. This keeps vectors and recovery ordering in reproducible scenario identity without eagerly duplicating the full workload universe; eager generation is limited to materializable all-zero and single-point cases, while arbitrary multi-fault vectors remain available through bounded on-demand persistence.

Assigned faults follow their persisted recovery schedule. Compensation checkpoints use explicit, implicit Saga-rollback, or conservative-unknown evidence; their reverse order may interleave with still-live participants' forward actions. A participant commits automatically with its final successful forward step rather than through a separately schedulable commit action. A zero-bit domain/simulator failure remains a meaningful outcome and uses immediate compensate-and-continue as a reported deviation, while executor/infrastructure failures and any thrown compensation action hard-stop the measured attempt. Compensation remains retryable by later explicit invocation, but this feature adds no automatic retry loop, compensation faults, retry count, or backoff.

This decision supersedes the v2 single-record catalog shape in [`2026-04-27-scenario-catalog-export-contract.md`](2026-04-27-scenario-catalog-export-contract.md) and supersedes the assigned-fault immediate-compensation and compensation-failure-continuation parts of [`2026-07-08-multi-saga-executor-failure-policy.md`](2026-07-08-multi-saga-executor-failure-policy.md) for v3 FaultScenario execution. It retains deterministic sequential replay, immediate compensate-and-continue for unassigned domain failures, and hard stops for executor/infrastructure failures.
