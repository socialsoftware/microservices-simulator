# Empty event receiver qualification

Six fresh Docker/JVM/H2 attempts qualify execution-report v6. They reuse retained
packages and exact scenario IDs. Empty receiver selection now finishes the selected
route attempt and permits the remaining schedule to run. It does not fabricate a
delivery or contribute an impact finding by itself.

| Case | Event action outcomes | Execution | ImpactV2 |
| --- | --- | --- | --- |
| Corrected AddStudent + RemoveStudent, no fault | No QuizAnswer receiver; no Tournament receiver | SUCCESS / EXACT | COMPLETE, 0 |
| UpdateStudentName to prepared Tournament | One actual delivery | SUCCESS / EXACT | COMPLETE, 0 |
| Same Tournament case, fault before event emission | Delivery masked by trigger fault | COMPENSATED / EXACT | COMPLETE, 0 |
| RemoveCourseExecution, QuizAnswer → Quiz → Tournament | Two deliveries, then no Tournament receiver | SUCCESS / EXACT | COMPLETE, 0 |
| Existing UpdateQuestion positive | One actual delivery, receiver unchanged and still eligible | SUCCESS / EXACT | COMPLETE, 1 |
| First case, observation disabled | Same two empty selections and full action trace | SUCCESS / EXACT | UNAVAILABLE, null |

The first case's retained v5 control completed its four forward actions, then stopped at
the absent QuizAnswer route. The new execution also reaches and checks Tournament's
route. The course-removal case previously stopped after two actual deliveries because
Tournament was absent; the same scheduled routes now complete with that absence explicit.
These are two selected former-invalid cases, not forty repaired executions.

The positive protects the distinction between absence of a receiver and a handler that
processes an event without persisting progress. The Question update still leaves its
Quiz receiver unchanged and eligible for the same event at the final horizon: one
`UNRESOLVED_DELIVERED_EVENT` finding. The metric's production code is unchanged.

The positive uses the existing unchanged qualification-only provider and retained
package from the original ImpactV2 experiment. It is not source-extracted and does not
claim new input coverage. The other cases use source-derived setups. No application
production mutation, receiver synthesis, extra event drain or automatic retry was used.

## Evidence and limits

- [Selection](selection.json) fixes cases, package paths/hashes and the provider's UTC
  fixture instant before execution.
- [Comparison](comparison.json) records old/new execution outcomes, exact report hashes,
  real delivery and empty-selection counts, score categories and observer parity.
- [Proof](proof.json) identifies the image, source baseline and raw build manifests.
  All 791 frozen production files match the checkout; 1,777 compiled/dependency files
  remain unchanged across the run. The only extra application test source is the
  unchanged pre-existing qualification provider.

Focused regression checks passed: 228 Java/Groovy tests and 13 Python batch-runner
tests. Their [outputs](tests/) are retained; this is not a full-suite qualification.

Raw logs, execution/impact reports and build artifacts are retained under
`verifiers/target/empty-event-delivery/run-01/`. Reproduction and assertion code are in
`verifiers/experiments/empty-event-delivery/`. Each attempt used its own disposable
container and H2 database; the build was reused. Historical results were not rewritten.

Five observed attempts have complete coverage within the existing three checks; the
disabled-observer attempt has an explicitly unavailable score. This is not a global
claim about events never delivered, future eligible receivers, all event routes, or
domain correctness. Multiple eligible receivers and actual replay failures remain
unsupported/error outcomes rather than empty selections.
