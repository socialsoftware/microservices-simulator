# Dynamic enrichment joining overview

This is the first-read overview for the verifier's dynamic-enrichment bridge. It explains the problem, the implemented approach, the result categories, and the current limits without requiring the full implementation trace.

For implementation-level detail, read the preserved [`dynamic-enrichment-joining-reference.md`](../archive/investigations/dynamic-enrichment-joining-reference.md). For terminology, keep [`glossary.md`](../glossary.md) open.

## Problem

The static verifier reads Java and Groovy source and produces deterministic v3 `WorkloadPlan` records from Saga structure, compensation/dispatch evidence, and test-derived input variants. Separate `FaultScenario` records add assigned vectors and action schedules.

Static analysis is strong at structure but weak at concrete runtime identity. It can often infer that a step reads `Order` or writes `Item`, but it cannot always prove which runtime aggregate instance was touched.

Dynamic enrichment addresses this correlation question:

```text
Which static InputVariant and WorkloadPlan does this runtime simulator event belong to?
```

The bridge is independent of ScenarioExecutor replay. It does not execute FaultScenarios or redefine their actions. It checks how static workload structure aligns with real application tests and writes additive workload-linked runtime evidence.

## Implemented approach

The current workflow has three layers.

| Layer | Role |
|---|---|
| Semantic package | The verifier analyzes source/tests and writes WorkloadPlan JSONL, FaultScenario JSONL, rejected-input diagnostics, accounting, and manifest. |
| Runtime evidence | The verifier optionally runs selected application test classes with simulator evidence enabled. The simulator emits JSONL events for test identity, Saga/functionality identity, steps, commands, aggregate accesses, and attribution diagnostics. |
| Join sidecars | The verifier joins runtime events back to WorkloadPlans and writes `workload-dynamic-evidence.jsonl`, `workload-dynamic-evidence-manifest.json`, and `dynamic-evidence-join-report.json` without modifying any semantic package file. |

The most important current improvement is direct input attribution. Before the dynamic Maven batch, the verifier writes a run-level `dynamic-input-map.json` for the selected test classes. The simulator loads that map and emits `inputVariantId` when the current test identity, runtime functionality class FQN, and runtime step name resolve to exactly one static input variant. The map includes `callContextMethodName`, `inputRole`, and `fixtureOrigin` so setup/helper fixture inputs can remain source-provenanced while still being eligible for the active Spock feature owner.

The input map uses explicit ownership metadata rather than treating source provenance as the owner identity. Provenance explains where the analyzer found an input, such as a direct feature method, helper call, `setup()`, field initializer, inherited fixture path, or `setupSpec()`. Ownership explains which feature methods the input is allowed to belong to at runtime. An input can therefore preserve `sourceClassFqn`, `sourceMethodName`, and `sourceBindingName` for explainability while also listing one or more owning feature methods for attribution.

Ownership contexts currently supported by the analyzed-input and dynamic-input-map model are: direct feature-created inputs, helper-created inputs, `setup()` fixture inputs, field-initialized inputs, inherited helper/setup/field variants, and `setupSpec()` metadata. `setupSpec()` ownership is exported as analysis metadata, but improved runtime attribution for `setupSpec()` execution events is intentionally out of scope for this work.

When that direct id appears in runtime evidence, the join can move from semantic matching to exact matching.

## Result categories

The workload evidence sidecar uses conservative join statuses. The names are defined in the [`glossary`](../glossary.md), but the practical reading is:

| Status | Meaning for the reader |
|---|---|
| `MATCHED_EXACT` | Runtime evidence carried a direct `inputVariantId` belonging to the WorkloadPlan. This is the strongest current result. |
| `MATCHED_HIGH_CONFIDENCE` | Runtime evidence matched by test identity and static semantic shape, but no direct input id was present. |
| `MATCHED_PARTIAL` | Some relevant shape matched, but not enough for high-confidence attribution. |
| `AMBIGUOUS` | Runtime evidence was relevant but still mapped to multiple static candidates; the verifier intentionally did not guess. |
| `UNMATCHED` | Runtime evidence existed, but the join could not connect it usefully to the static workload candidate. |
| `NOT_COVERED` | No useful runtime evidence was seen for that workload in the selected dynamic run. |

For thesis writing, `MATCHED_EXACT` supports the claim that runtime attribution can connect observed simulator evidence to static input variants. `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED` are equally important because they bound what should not be overclaimed. `UNMATCHED` records include `unmatchedReason` only when unmatched; manifests and join reports include deterministic `unmatchedReasonCounts` for failed test classes, non-selected classes, helper-owner mismatches, and unclassified residuals.

## Evidence

Current v3 compatibility is covered by `DummyappDynamicEnrichmentIntegrationSpec`: it writes and snapshots all five semantic package artifacts, publishes workload-linked sidecars, and proves package bytes are unchanged. The latest broad Quizzes counts below are retained as a historical v2-era attribution baseline, not a fresh v3 package run:

```text
run: verifiers/target/feature-helper-owner-fix-dynamic-smoke/quizzes-20260630-122219-034/
historical v2 scenario records: 584
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26815
MATCHED_EXACT=435
MATCHED_HIGH_CONFIDENCE=125
MATCHED_PARTIAL=0
AMBIGUOUS=0
UNMATCHED=24
NOT_COVERED=0
unmatchedReasonCounts={FAILED_TEST_CLASS=8, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=9}
```

The older comparable baseline before runtime input attribution was:

```text
MATCHED_EXACT=0
MATCHED_HIGH_CONFIDENCE=2
AMBIGUOUS=44
UNMATCHED=20
warningCount=8238
```

That supports a narrow historical claim: direct runtime input attribution and ownership metadata improved exact static/dynamic joining for the comparable local/sagas Quizzes target, eliminated ambiguity in that run, and reduced unmatched records from `184` to `24`. It does not establish current broad v3 enrichment counts, distributed/stream/gRPC/TCC behavior, or ScenarioExecutor conformance. The remaining unmatched records remain evidence boundaries.

## Current limits

Do not overclaim the dynamic-enrichment bridge.

The join also contains plan-local ambiguity containment. A direct runtime `inputVariantId` still wins for the plan that owns that input, but a direct id for a different input is not reused to semantically promote neighboring plans. `AMBIGUOUS` should stay local to the plan inputs that participate in the ambiguous identity set. Genuine same-feature sibling ambiguity can still remain expected behavior when current runtime evidence cannot distinguish two sibling inputs from the same feature.

Known limits include:

- It is additive sidecar evidence; it does not redefine InputVariants, WorkloadPlans, FaultScenarios, vectors, or action schedules.
- It does not run persisted FaultScenarios.
- Current runtime attribution is conservative and first-pass.
- The latest Quizzes baseline has zero ambiguous joins, but ambiguity can still return with multiple same-feature static inputs, weaker runtime names, missing test context, async boundaries, capped/rejected static inputs, and weak aggregate-key evidence.
- The latest Quizzes baseline still has `UNMATCHED=24` (`UNCLASSIFIED=9`), so residual triage should happen before deciding whether runtime-value and aggregate-key based refinement is worth the scope.
- Semantic deduplication of value-equivalent inputs, broader executor materialization, and stronger same-feature sibling disambiguation remain future work.
- Stream/gRPC/distributed parity and causal/TCC runtime hooks are not established by this baseline.
- Domain-impact scoring, GA local search, and bandit scenario prioritization remain future stages.

## When to use the detailed reference

Use [`dynamic-enrichment-joining-reference.md`](../archive/investigations/dynamic-enrichment-joining-reference.md) when you need:

- the static pipeline step-by-step;
- dynamic test selection and Maven orchestration detail;
- dynamic input map schema intent;
- simulator runtime attribution flow;
- join algorithm phases;
- examples of high-confidence versus exact matching;
- ambiguity and unmatched failure patterns;
- sidecar artifact details;
- next implementation improvements and code references.

## Related pages

- [`current-state.md`](../current-state.md) for the current implemented baseline and validation counts.
- [`roadmap.md`](../roadmap.md) for how dynamic enrichment fits between static scenario synthesis and future execution/search stages.
- [`glossary.md`](../glossary.md) for terminology and status labels.
- [`dynamic-enrichment-joining-reference.md`](../archive/investigations/dynamic-enrichment-joining-reference.md) for the preserved detailed technical material.
