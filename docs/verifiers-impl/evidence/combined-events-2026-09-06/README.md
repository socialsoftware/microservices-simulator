# Combined-event evidence

Exact report copies from the eight-attempt Docker campaign. [comparison.json](comparison.json)
records every outcome, including the missing-third-receiver attempt. [plan.json](plan.json)
and [broader-selection.json](broader-selection.json) identify the source-selected workloads,
route orders and fault scenarios. [provenance.json](provenance.json) records source paths
and hashes. Internal report paths remain unchanged.

| Selected horizon | Execution | Impact evidence |
| --- | --- | --- |
| answer | [execution](answer/execution.json) | [assessment](answer/execution.impact-v2.json) |
| answer-quiz | [execution](answer-quiz/execution.json) | [assessment](answer-quiz/execution.impact-v2.json) |
| answer-quiz-missing-tournament | [execution](answer-quiz-missing-tournament/execution.json) | [assessment](answer-quiz-missing-tournament/execution.impact-v2.json) |
| answer-quiz-trigger-fault | [execution](answer-quiz-trigger-fault/execution.json) | [assessment](answer-quiz-trigger-fault/execution.impact-v2.json) |
| none | [execution](none/execution.json) | [assessment](none/execution.impact-v2.json) |
| quiz | [execution](quiz/execution.json) | [assessment](quiz/execution.impact-v2.json) |
| quiz-answer | [execution](quiz-answer/execution.json) | [assessment](quiz-answer/execution.impact-v2.json) |
| quiz-answer-trigger-fault | [execution](quiz-answer-trigger-fault/execution.json) | [assessment](quiz-answer-trigger-fault/execution.impact-v2.json) |

Full generated package, source snapshot, build/generation logs, ImpactV1 sidecars and
per-attempt container logs remain in `verifiers/target/combined-event-deliveries/run-02/`.
These copies preserve the evidence needed to interpret the meeting example outside
Maven's disposable output directory. They are not a replacement for the full build.
