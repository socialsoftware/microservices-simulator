## Problem Statement

Full Quizzes static catalog generation is already fast and deterministic, but full Quizzes dynamic enrichment is too slow and too memory hungry to be a practical feedback loop.

The current full dynamic run selects 42 Quizzes saga/local test classes and launches one Maven invocation per class. That repeats Maven and Spring test startup costs 42 times and stretches the dynamic phase to about 35-40 minutes. The resulting output is useful and should remain comparable, but the workflow is slow enough that normal iteration on dynamic enrichment becomes expensive.

The current verifier-side evidence read/join path also does not scale cleanly under the default Docker compose memory limit. A normal extended-time run hit `OutOfMemoryError` while reading accumulated dynamic evidence. The same run completed only after increasing compose memory to 4 GB and forcing a 3 GB Java heap. Repository exploration points to the reader and joiner retaining too much parsed evidence and derived analysis in memory: evidence lines are parsed into JSON trees, raw JSON is retained, and all event analyses are built before writing output.

The completed full-run baseline is valuable and must stay comparable. The relevant baseline has 66 enriched scenario records, 42 selected test classes, 18,868 dynamic events read, zero events missing test context, and join counts of `MATCHED_EXACT=52`, `MATCHED_HIGH_CONFIDENCE=1`, `MATCHED_PARTIAL=0`, `AMBIGUOUS=0`, `UNMATCHED=13`, `NOT_COVERED=0`, and `warningCount=0`. The run status is `PARTIAL` only because two Quizzes application test classes fail independently of dynamic-enrichment instrumentation.

The goal is not to change scenario generation, matching semantics, or Quizzes tests to improve those numbers. The goal is to preserve comparable output while making the full dynamic-enrichment pipeline materially faster and able to run under default Docker compose memory settings.

## Solution

Replace the current class-scoped dynamic-enrichment pipeline with a run-scoped pipeline.

The verifier will still discover the selected Quizzes test classes deterministically, but it will write one global dynamic input map for the run, invoke Maven once for all selected classes, collect one shared dynamic evidence stream, derive per-class status from Surefire reports, and then perform an evidence join that does not retain raw JSON trees or all derived event analyses in memory.

The run-scoped input map will contain all accepted inputs from the exported static scenario catalog, not only inputs for one test class at a time. The old singular root `testClassFqn` constraint will be removed. The map will instead include deterministic run metadata such as selected test classes, input count, generation time, and the accepted input entries with existing owner/source/functionality/step metadata. Selected-test-class metadata is audit-only; it must not prune accepted catalog inputs or broaden attribution eligibility. The simulator runtime resolver must preserve the current matching predicate except for removing the root per-class map gate: current test identity must match an explicit owner or the source class/method fallback, runtime functionality class must match the input saga/functionality identity, and runtime step name must match the static step hint.

The verifier will pass one evidence output directory and one input-map path to the Maven test process. The dynamic evidence recorder will write one shared `dynamic-evidence.jsonl` for the process. Event identity will come from the test identity fields already recorded on each event, not from one directory per test class.

The verifier will preserve per-class observability by parsing Surefire results after Maven exits. The run-scoped evidence directory should contain `dynamic-input-map.json`, `dynamic-evidence.jsonl`, `dynamic-evidence-manifest.json`, `maven-output.log`, `test-run.json`, and `test-runs/<safe-test-class-fqn>.json` audit sidecars. The authoritative summary remains `dynamic-evidence-join-report.json`, which owns run status, per-class status counts, evidence counts, evidence byte counts, and phase durations. The enriched manifest may mirror status counts for the existing enriched-catalog summary, but it is not the primary operational report. A nonzero Maven exit caused by test failures should still allow evidence read, join, and artifact writing when partial test runs are allowed. Strict mode should preserve artifacts and fail only after the sidecars are written.

The evidence reader and joiner will be redesigned around compact event projections and plan-indexed accumulation. They should keep only fields required for the current join statuses and enriched summaries, plus source location/order metadata needed for deterministic output. They should not retain the raw JSON tree for every event, and they should not build all per-event candidate analyses only to rescan them for every scenario plan. The enriched catalog schema and join status meanings should remain comparable to the baseline.

Compact runtime payload filtering is not required in this PRD. The completed full run has about 24 MB of JSONL evidence, which does not by itself explain a 3 GB heap requirement. Payload filtering should remain a contingency if streaming/indexed joining still misses the memory target or if profiling later identifies pathological single-event payloads.

The new pipeline should add simple operational metrics to the join report: dynamic run start/finish time, Maven duration, evidence read/join/write duration, evidence bytes read, dynamic events read, evidence files read, and per-class status counts. Profiler-grade heap/JMX metrics are not required.

## User Stories

1. As the verifier author, I want the full Quizzes dynamic-enrichment run to complete much faster, so that full-run feedback is practical during thesis implementation work.
2. As the verifier author, I want the full Quizzes dynamic-enrichment run to complete under default Docker compose memory settings, so that I do not need a 3 GB heap override for normal evaluation.
3. As the verifier author, I want static catalog generation to remain unchanged, so that dynamic pipeline work does not redefine the scenario catalog.
4. As the verifier author, I want dynamic matching semantics to remain unchanged, so that count changes are not confused with performance work.
5. As the verifier author, I want one Maven invocation for all selected dynamic test classes, so that repeated Maven and Spring startup costs are avoided.
6. As the verifier author, I want selected test classes to run serially inside Maven, so that speed improves without introducing parallel shared-state flakiness.
7. As the verifier author, I want one run-scoped input map, so that the simulator can attribute evidence across all selected classes without per-class orchestration.
8. As the verifier author, I want the input map to contain all accepted static catalog inputs, so that runtime attribution sees the same accepted candidate space as the enriched catalog.
9. As the verifier author, I want selected test classes recorded as run metadata, so that dynamic execution remains auditable without pruning accepted inputs accidentally.
10. As the verifier author, I want one shared dynamic evidence stream, so that evidence generation fits the single-Maven-run model.
11. As the verifier author, I want test identity fields to be the source of evidence ownership, so that evidence can be joined correctly without relying on directory names.
12. As the verifier author, I want per-class pass/fail/no-report statuses derived from Surefire reports, so that the join report remains comparable to previous per-class counts.
13. As the verifier author, I want partial-mode runs to write enriched artifacts even when some Quizzes tests fail, so that known application-test failures do not destroy useful dynamic evidence.
14. As the verifier author, I want strict-mode runs to preserve artifacts before failing, so that debugging information is not lost.
15. As the verifier author, I want the evidence reader to avoid retaining raw JSON trees for all events, so that memory use scales with the useful join projection rather than the raw payload representation.
16. As the verifier author, I want the joiner to avoid building and rescanning all derived event analyses per plan, so that joining remains bounded and predictable on full Quizzes evidence.
17. As the verifier author, I want enriched output counts to remain comparable to the current full-run baseline, so that performance improvements do not hide semantic regressions.
18. As the verifier author, I want dynamic event counts and test-context counts reported clearly, so that any small differences from batching can be explained rather than guessed.
19. As the verifier author, I want simple phase timings in the report, so that I can tell whether time is spent in Maven execution, evidence reading, joining, or writing.
20. As the verifier author, I want evidence byte counts in the report, so that future payload-size concerns can be evaluated with data.
21. As the verifier author, I want the old per-class orchestration path removed, so that the implementation does not carry duplicate modes with different artifact semantics.
22. As the verifier author, I want dummyapp coverage before full Quizzes smoke validation, so that the new pipeline is proved on a small fixture before spending time on the realistic target.
23. As a future maintainer, I want tests to verify observable pipeline behavior rather than internal helper structure, so that reader/joiner internals can be refactored without noisy tests.
24. As a future maintainer, I want enough run metadata preserved for a future resume/checkpointing feature, so that this work does not block later lifecycle improvements.
25. As a future maintainer, I want resume/checkpointing excluded from this PRD, so that the current work stays focused on speed and memory rather than a larger lifecycle feature.
26. As a future evaluator, I want the baseline comparison criteria documented explicitly, so that a faster run can be judged against the same full Quizzes output surface.

## Implementation Decisions

- Dynamic enrichment will become run-scoped rather than test-class-scoped.
- The old per-class orchestration path will be removed instead of preserved as a compatibility mode.
- Backward compatibility with the old per-class dynamic input-map schema is not required.
- The run-scoped input map will contain all accepted static catalog inputs available to the enriched catalog.
- The run-scoped input map will remove the singular root test-class gate and include deterministic selected-test-class metadata for audit.
- Simulator runtime input attribution will resolve against the current test identity, input owners or source fallback, runtime functionality class, and runtime step name using the run-scoped map.
- The run-scoped resolver must be a semantics-preserving replacement for the current per-class resolver. Removing the root `testClassFqn` gate must not allow an input to match a test identity that would not have matched the existing owner/source-class rules.
- The Maven test command will be built once for the selected class set. It will keep the existing dynamic evidence and JUnit listener properties but point them at run-scoped paths.
- Maven/Surefire test execution will remain serial for this PRD. No Surefire class parallelism or external bounded parallelism is included.
- The dynamic evidence recorder will write one shared evidence file for the Maven process.
- Per-class status accounting will be derived after Maven execution by parsing test reports, not by relying on one exit code per test class.
- Per-class status mapping from test reports will be explicit. A report with failures or errors maps to `FAILED`. A report with no failures or errors maps to `PASSED`, including reports with zero executed tests. A report with all tests skipped and no failures or errors maps to `SKIPPED` only if the report exposes that state distinctly. If the Maven process times out, selected classes without completed reports map to `TIMED_OUT`. If the Maven process did not time out and a selected class has no report, it maps to `NO_REPORT` with a warning/count.
- The join report will continue to expose selected, passed, failed, timed out, skipped, and no-report class counts in a comparable way.
- Partial-mode behavior will allow read/join/write after a nonzero Maven exit caused by test failures.
- Strict-mode behavior will write artifacts before surfacing failure.
- The evidence reader will project each JSONL event into a compact verifier-side event model and avoid retaining raw JSON trees as part of normal production flow.
- The joiner will use indexing or streaming accumulation so that it does not need to retain every derived event analysis and repeatedly scan it for every scenario plan.
- The enriched catalog record schema and join status ladder will remain sidecar-only and comparable.
- Existing observed step, observed command, observed aggregate access, matched execution, warning, and count concepts remain the user-visible output surface.
- Compact runtime payload filtering is deferred unless the streaming/indexed join still cannot satisfy the memory target or evidence-size metrics expose a separate payload problem.
- Operational metrics will remain simple and additive: phase durations, evidence bytes, evidence files, event counts, and class status counts.
- Resume/checkpoint lifecycle is out of scope. Future-resume preparation is capped to metadata already required by this PRD: selected classes, command arguments, phase timestamps/durations, batch status, per-class report-derived status, static catalog path, and evidence root. No restart, checkpoint selection, or join-only behavior is included.

## Testing Decisions

- Good tests for this work should verify externally observable behavior through public interfaces and artifacts, not private helper functions or incidental implementation structure.
- Testing should follow vertical tracer-bullet development. The first slice should prove a tiny run-scoped input map can drive multi-class simulator attribution. Later slices should add single-Maven orchestration, test-report-derived class statuses, streaming/indexed join equivalence, dummyapp end-to-end validation, and finally full Quizzes smoke validation.
- The highest-value public behaviors to test are run-scoped input-map serialization, run-scoped simulator lookup across multiple test identities, single-command dynamic orchestration, partial-mode artifact preservation, strict-mode artifact preservation before failure, per-class status extraction from reports, and join-output comparability.
- The run-scoped input-map tests should prove that inputs from more than one test class can live in one map and still resolve only for the active owner/source identity, runtime functionality class, and step name.
- The simulator-side tests should use the public input-map loader/resolver behavior rather than private matching helpers.
- The orchestrator tests should use the public dynamic-enrichment run entry point with a fake process runner and fixture test reports to prove one Maven command, run-scoped artifact paths, per-class status records, and partial/strict behavior.
- The report parser tests should use representative Surefire report fixtures to prove the defined status mapping: passed reports, zero-test passed reports, failed/error reports, skipped reports when distinct, timed-out missing reports, and no-report missing reports.
- The evidence reader/joiner tests should prove that the same semantic evidence produces the same join statuses, observed summaries, counts, and warnings as the existing behavior.
- The memory-oriented join tests should prefer observable bounded behavior on generated evidence or fixture evidence over assertions on private data structures.
- Dummyapp dynamic enrichment should provide the first end-to-end tracer bullet because it is the canonical verifier fixture for parser/scenario/enrichment edge cases.
- Full Quizzes validation should be a compose-level smoke check against the current baseline after dummyapp passes.
- Full Quizzes acceptance should require the 66-record enriched catalog and key join status counts to match the baseline: `MATCHED_EXACT=52`, `MATCHED_HIGH_CONFIDENCE=1`, `MATCHED_PARTIAL=0`, `AMBIGUOUS=0`, `UNMATCHED=13`, `NOT_COVERED=0`, and `warningCount=0`.
- Full Quizzes acceptance should require `eventsMissingTestContext=0`.
- Full Quizzes acceptance should require `testClassesSelected=42`. If the Quizzes tests are otherwise unchanged, the expected report-derived class status counts are `testClassesPassed=40`, `testClassesFailed=2`, `testClassesTimedOut=0`, `testClassesSkipped=0`, and `testClassesNoReport=0`.
- Full Quizzes acceptance should require `dynamicEventsRead` to be reported. The target is exactly 18,868 events. A batched run may still pass with 17,925-19,812 events, but only if the exact join counts, warning count, test-context count, and observed-summary semantics remain stable; values outside that range fail acceptance.
- Full Quizzes acceptance should require the dynamic-enrichment phase to complete in under 15 minutes under default compose memory settings without `JAVA_TOOL_OPTIONS=-Xmx3g`.
- Existing test styles to reuse include dynamic input-map resolution tests, dynamic evidence reader/joiner specs, dynamic enrichment orchestrator specs, enriched catalog writer specs, and dummyapp dynamic-enrichment integration specs.
- Tests should avoid brittle assertions on exact JSON pretty-printing, private traversal order, exact Maven log text, or implementation-specific index structures.
- Tests should not assert improved Quizzes match counts, because improving counts is out of scope.

## Out of Scope

- Changing static scenario generation.
- Changing dynamic matching semantics to improve counts.
- Changing Quizzes application tests to make the run pass or counts look better.
- Preserving backward compatibility with the old per-class dynamic input-map schema or old per-class evidence layout.
- Keeping the old per-class orchestration path as a fallback mode.
- Implementing general resume/checkpoint lifecycle.
- Implementing join-only mode as a standalone feature.
- Adding bounded parallelism or Surefire/test-class parallel execution.
- Treating larger Docker memory limits as the primary fix.
- Implementing compact/default runtime payload filtering unless memory targets remain unmet after streaming/indexed join.
- Scenario execution, fault injection, domain-impact scoring, GA search, or bandit prioritization.
- Stream/gRPC/distributed/TCC dynamic-enrichment parity.

## Further Notes

- This PRD is intentionally about pipeline performance and memory behavior, not semantic enrichment quality.
- The current full-run numbers are the comparison baseline because they represent the latest successful full Quizzes run with runtime input attribution and default scenario-catalog settings.
- A single Maven invocation may not collapse all Spring context startup cost, but it should remove the dominant repeated Maven/class-run orchestration overhead.
- A future resume feature should be designed separately around lifecycle checkpoints, phase status, and deterministic re-entry. This PRD should not smuggle that lifecycle into a simple join-only flag.
- If the streaming/indexed join meets the memory target, compact evidence should remain deferred to avoid unnecessary runtime schema complexity.
