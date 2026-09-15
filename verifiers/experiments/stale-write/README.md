# Stale-write investigation

A controlled Quizzes experiment checks whether cached data overwrites another workflow's
applied update in a normal Saga step and in compensation. This is research evidence;
it does not implement a detector, alter scoring, or claim generated-workload readiness.

The [results and literature mapping](../../../docs/verifiers-impl/evidence/stale-write-2026-09-15/README.md)
explain the five cases and the remaining observation gaps.

```bash
python3 verifiers/experiments/stale-write/run.py \
  --output verifiers/target/stale-write/my-run
python3 verifiers/experiments/stale-write/validate.py \
  --run verifiers/target/stale-write/my-run \
  --output verifiers/target/stale-write/my-run/validation.json
```

The runner verifies the existing prepared Quizzes build and current read-scope overlay,
compiles only StaleWriteExperiment, then runs five cases with serialization disabled and
enabled (ten isolated JVM/H2 executions). Setup and fault/recovery helpers follow the
existing SagaUpdateReadExperiment. A selected UpdateTopicEvent is delivered through the
real Tournament handler; the event-triggered Saga completes before A resumes. Polling is
suppressed and other event handlers are outside the chosen horizon.

The harness records observations without assuming a lost-update result. The independent
validator checks the declared application histories against raw committed writes,
writer phases, cached DTO names, exact event identity and final state. It intentionally
contains Quizzes-specific witness checks; it is not a generic impact oracle.
