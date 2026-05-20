# Intake

## Goal

Design a faster and less memory-hungry full Quizzes dynamic-enrichment pipeline while preserving comparable output to `verifiers/target/quizzes-20260519-100927-216/`.

The goal is pipeline mechanics, not scenario semantics. The new pipeline should make the 42-class full Quizzes dynamic run practical under default Docker compose memory and keep the enriched catalog/join report comparable to the current full-run baseline.

## Context

Full Quizzes static catalog generation is already fast: 66 scenarios in about 1 second.

Current dynamic enrichment selects 42 Quizzes saga/local test classes and runs one Maven invocation per class, taking about 35-40 minutes.

The default compose memory limit caused `OutOfMemoryError` in `DynamicEvidenceReader`. The run completed only after raising memory to `MEDIUM_MEM_LIMIT=4g`, `MEDIUM_MEM_RESERVATION=2g`, and `JAVA_TOOL_OPTIONS=-Xmx3g`.

Completed full-run artifact baseline: `verifiers/target/quizzes-20260519-100927-216/`.

Baseline comparable counts: `MATCHED_EXACT=52`, `MATCHED_HIGH_CONFIDENCE=1`, `MATCHED_PARTIAL=0`, `AMBIGUOUS=0`, `UNMATCHED=13`, `NOT_COVERED=0`, `recordCount=66`, `warningCount=0`.

Baseline join report: `testClassesSelected=42`, `testClassesPassed=40`, `testClassesFailed=2`, `dynamicEventsRead=18868`, `eventsMissingTestContext=0`.

Repo exploration found the execution bottleneck in `DynamicEnrichmentOrchestrator`: it loops selected test classes and invokes Maven once per class.

Repo exploration found the memory bottleneck in `DynamicEvidenceReader` and `DynamicEvidenceJoiner`: the reader loads all JSONL lines as Jackson trees and retains raw JSON, while the joiner builds all derived analyses before writing output.

Evidence payload size is not currently the main proof of the memory problem: the full run has about 24 MB of JSONL evidence and 29 MB under `dynamic-evidence/`.

## Decisions

Replace per-class orchestration with one global dynamic-enrichment run: one input map, one Maven invocation, one shared evidence file.

Do not preserve backward compatibility with the old per-class input-map/orchestration schema.

Remove the old per-class execution path rather than keeping both modes.

Run all selected Quizzes classes serially inside one Maven/Surefire invocation. Do not introduce test parallelism in this PRD.

Use a single run-scoped `dynamic-input-map.json` containing all accepted `ScenarioPlan.inputs()` from the static catalog. In the current baseline this is 66 accepted inputs from 28 source classes, all within the 42 selected dynamic test classes.

Store selected test classes separately for execution/audit, not as a pruning rule for accepted catalog inputs.

Use one shared `dynamic-evidence.jsonl` for the Maven process. Evidence identity must come from event test identity fields, not directory names.

Derive per-class pass/fail/no-report status from Surefire reports and write per-class metadata under a simple `test-runs/` area.

When `allowPartialTestRun=true`, a nonzero Maven exit from failing tests must still allow evidence read/join/write and preserve artifacts.

When strict mode is used, artifacts should still be written before the verifier fails.

Do not require compact payload filtering now. Treat it as a contingency only if streaming/indexed join still cannot meet memory targets or profiling finds pathological single-event payloads.

Implement streaming or indexed evidence reading/joining so the verifier does not retain raw JSON trees or all derived event analyses.

Keep resume/checkpointing out of scope. Record enough simple phase/per-class metadata to make a future lifecycle/resume feature possible.

## Recommended Defaults Accepted

Default full Quizzes dynamic enrichment should use one serial Maven invocation for all selected classes.

Default artifact identity should be test identity plus Surefire-derived class status, not one physical evidence directory per class.

Default observability should stay simple: phase durations, evidence bytes read, event counts, evidence file counts, and class status counts.

Default acceptance target: full 42-class Quizzes dynamic enrichment completes in under 15 minutes under default compose memory settings, without `JAVA_TOOL_OPTIONS=-Xmx3g`.

Default comparability target: preserve the 66-record enriched catalog and the key join status counts from the baseline, with `dynamicEventsRead` reported and explainable within a tight tolerance such as +/-5%.

Default testing strategy: dummyapp-first vertical tracer-bullet coverage, then full Quizzes compose smoke against the current baseline.

## Out Of Scope

Changing static scenario generation.

Changing dynamic matching semantics to improve counts.

Changing Quizzes application tests to make the run pass or counts look better.

Implementing general resume/checkpoint lifecycle.

Adding bounded parallelism or Surefire/test-class parallel execution.

Treating larger Docker memory limits as the real fix.

Implementing compact/default payload filtering unless memory targets remain unmet after streaming/indexed join.

Scenario execution, fault injection, domain-impact scoring, GA search, or bandit prioritization.

## Risks And Tradeoffs

A single Maven invocation depends on reliable Surefire report parsing to preserve per-class status accounting.

A shared evidence file makes test identity correctness more important.

Removing per-class artifacts simplifies implementation but intentionally breaks old artifact layout compatibility.

Streaming/indexed join is more invasive internally than merely increasing heap, but it directly addresses the observed OOM root cause.

Serial batching avoids parallel flakiness risk, but it may not eliminate all Spring context startup cost.
