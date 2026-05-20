# Dynamic enrichment joining overview

This is the first-read overview for the verifier's dynamic-enrichment bridge. It explains the problem, the implemented approach, the result categories, and the current limits without requiring the full implementation trace.

For implementation-level detail, read the preserved [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md). For terminology, keep [`glossary.md`](glossary.md) open.

## Problem

The static verifier can read Java and Groovy source code and produce deterministic `ScenarioPlan` records from saga structure, command-handler access policies, and test-derived input variants.

Static analysis is strong at structure, but weak at concrete runtime identity. It can often infer that a step reads `Order` or writes `Item`, but it cannot always prove which runtime aggregate instance was touched.

Dynamic enrichment addresses that correlation problem:

```text
Which static InputVariant and ScenarioPlan does this runtime simulator event belong to?
```

The bridge exists before the future scenario executor. It does not execute generated fault schedules. It checks whether static scenario-generation output aligns with real application executions and enriches the catalog with observed runtime evidence.

## Implemented approach

The current workflow has three layers.

| Layer | Role |
|---|---|
| Static catalog | The verifier analyzes source and tests, then writes `scenario-catalog.jsonl`, manifest, and rejected-input diagnostics. |
| Runtime evidence | The verifier optionally runs selected application test classes with simulator evidence enabled. The simulator emits JSONL events for test identity, saga/functionality identity, steps, commands, aggregate accesses, and attribution diagnostics. |
| Join sidecars | The verifier joins runtime events back to static scenario plans and writes sidecar artifacts such as `scenario-catalog-enriched.jsonl`, `scenario-catalog-enriched-manifest.json`, and `dynamic-evidence-join-report.json`. |

The most important current improvement is direct input attribution. Before each dynamic test-class run, the verifier writes a per-class `dynamic-input-map.json`. The simulator loads that map and emits `inputVariantId` when the current test identity, runtime functionality class FQN, and runtime step name resolve to exactly one static input variant.

The input map uses explicit ownership metadata rather than treating source provenance as the owner identity. Provenance explains where the analyzer found an input, such as a direct feature method, helper call, `setup()`, field initializer, inherited fixture path, or `setupSpec()`. Ownership explains which feature methods the input is allowed to belong to at runtime. An input can therefore preserve `sourceClassFqn`, `sourceMethodName`, and `sourceBindingName` for explainability while also listing one or more owning feature methods for attribution.

Ownership contexts currently supported by the analyzed-input and dynamic-input-map model are: direct feature-created inputs, helper-created inputs, `setup()` fixture inputs, field-initialized inputs, inherited helper/setup/field variants, and `setupSpec()` metadata. `setupSpec()` ownership is exported as analysis metadata, but improved runtime attribution for `setupSpec()` execution events is intentionally out of scope for this work.

When that direct id appears in runtime evidence, the join can move from semantic matching to exact matching.

## Result categories

The enriched catalog uses conservative join statuses. The names are defined in the [`glossary`](glossary.md), but the practical reading is:

| Status | Meaning for the reader |
|---|---|
| `MATCHED_EXACT` | Runtime evidence carried a direct `inputVariantId` belonging to the static scenario plan. This is the strongest current result. |
| `MATCHED_HIGH_CONFIDENCE` | Runtime evidence matched by test identity and static semantic shape, but no direct input id was present. |
| `MATCHED_PARTIAL` | Some relevant shape matched, but not enough for high-confidence attribution. |
| `AMBIGUOUS` | Runtime evidence was relevant but still mapped to multiple static candidates; the verifier intentionally did not guess. |
| `UNMATCHED` | Runtime evidence existed, but the join could not connect it usefully to the static scenario candidate. |
| `NOT_COVERED` | No useful runtime evidence was seen for that scenario in the selected dynamic run. |

For thesis writing, `MATCHED_EXACT` supports the claim that runtime attribution can connect observed simulator evidence to static input variants. `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED` are equally important because they bound what should not be overclaimed.

## Current evidence

The current baseline is documented in [`current-state.md`](current-state.md). In the refreshed Quizzes sagas-only run after runtime input attribution, comparable status counts moved from:

```text
MATCHED_EXACT=0
MATCHED_HIGH_CONFIDENCE=2
AMBIGUOUS=44
UNMATCHED=20
warningCount=8238
```

to:

```text
MATCHED_EXACT=46
MATCHED_HIGH_CONFIDENCE=0
AMBIGUOUS=3
UNMATCHED=17
warningCount=328
```

That supports a narrow claim: direct runtime input attribution substantially improved exact static/dynamic joining for the comparable local sagas Quizzes target. It does not prove general distributed, stream/gRPC, TCC, or future executor behavior.

## Current limits

Do not overclaim the dynamic-enrichment bridge.

The join also contains plan-local ambiguity containment. A direct runtime `inputVariantId` still wins for the plan that owns that input, but a direct id for a different input is not reused to semantically promote neighboring plans. `AMBIGUOUS` should stay local to the plan inputs that participate in the ambiguous identity set. Genuine same-feature sibling ambiguity can still remain expected behavior when current runtime evidence cannot distinguish two sibling inputs from the same feature.

Known limits include:

- It is additive sidecar evidence; it does not redefine the static scenario catalog.
- It is not the arbitrary scenario executor and does not run generated fault schedules.
- Current runtime attribution is conservative and first-pass.
- Remaining ambiguity can come from multiple same-feature static inputs, weaker runtime names, missing test context, async boundaries, capped/rejected static inputs, and weak aggregate-key evidence.
- Semantic deduplication of value-equivalent inputs, executor materialization, and stronger same-feature sibling disambiguation remain future work.
- Stream/gRPC/distributed parity and causal/TCC runtime hooks are not established by this baseline.
- Domain-impact scoring, GA local search, and bandit scenario prioritization remain future stages.

## When to use the detailed reference

Use [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md) when you need:

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

- [`current-state.md`](current-state.md) for the current implemented baseline and validation counts.
- [`roadmap.md`](roadmap.md) for how dynamic enrichment fits between static scenario synthesis and future execution/search stages.
- [`glossary.md`](glossary.md) for terminology and status labels.
- [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md) for the preserved detailed technical material.
