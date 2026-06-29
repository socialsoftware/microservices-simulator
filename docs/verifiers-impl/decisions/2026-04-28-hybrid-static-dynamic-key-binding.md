# Proposal: hybrid static + dynamic evidence for aggregate-key binding

Date: 2026-04-28

## Status

Accepted and partially implemented (2026-05-01).

Implemented in the first slice:

- simulator dynamic evidence JSONL + test-context capture;
- verifier orchestration of selected-test evidence collection;
- sidecar enrichment join/report artifacts.

2026-06-29 evolution:

- direct runtime `inputVariantId` propagation is implemented for the local/sagas bridge;
- the current Quizzes dynamic baseline writes a run-level `dynamic-input-map.json` for the selected-test-class Maven batch;
- the refreshed post-event baseline produced `MATCHED_EXACT=291`, `MATCHED_HIGH_CONFIDENCE=109`, `AMBIGUOUS=0`, `UNMATCHED=184`.

Still deferred from this decision:

- deeper runtime matching using command/aggregate values;
- stream/gRPC/distributed/TCC parity;
- executor/runtime materialization stages.

## Context

The current verifier can statically extract saga structure, ordered steps, command dispatches, aggregate read/write footprints, and Groovy test input variants. The scenario catalog can represent exact, symbolic, type-only, and unknown aggregate-key evidence.

The main remaining precision gap is exact aggregate-instance key binding:

> We often know that a saga input exists, and we often know that a step touches an aggregate type such as `Tournament`, but we do not always know which concrete input value is the `Tournament` id used by that step.

Pure static propagation would require following values through many layers:

```text
Groovy test input
  -> saga/functionality constructor parameter
  -> saga field
  -> command constructor argument
  -> command field/getter
  -> command handler/service method argument
  -> repository/load/registerChanged call
  -> aggregate instance key
```

This is possible for simple cases, but it can become brittle across helper methods, DTO construction, fixtures, runtime-generated IDs, service calls, and application-specific utility methods.

During the thesis meeting, the advisor suggested using a dynamic analysis step: run relevant tests, observe logs/traces/runtime events, and map static source locations to runtime values. Example target evidence:

```text
"this source line instantiates this DTO" -> "at runtime it produced dto.id = 2"
```

This dynamic step should not replace static analysis. It should run after the existing static Groovy/source analysis and use static facts to decide what to instrument, observe, parse, and join.

## Proposed decision

Add a hybrid key-binding stage after static analysis:

1. Run existing static verifier analysis first.
2. Use static results to identify observation targets:
   - Groovy test methods and source expressions that create saga/functionality inputs;
   - constructor/facade calls already traced statically;
   - DTO creation sites and command construction sites relevant to saga inputs;
   - command-handler/service aggregate access sites relevant to step footprints;
   - known log/tracing utilities and log formats discovered statically.
3. Run selected tests under an instrumentation/observation profile.
4. Capture runtime evidence such as:
   - source location or stable static anchor;
   - test class/method;
   - saga/functionality class;
   - constructor argument runtime values;
   - DTO/command object field values after construction;
   - aggregate load/write calls and observed ids;
   - logs/traces emitted by known application utilities;
   - timestamps/order/correlation ids where available.
5. Export dynamic evidence as a separate artifact.
6. Join dynamic evidence back into the scenario model to populate `InputVariant.logicalKeyBindings` and/or `StepFootprint.aggregateKey.keyText` with confidence labels.

The static scenario catalog remains the contract, but it may be enriched by dynamic evidence when available.

## Why hybrid instead of pure static

Advantages:

- Runtime evidence can observe generated IDs and fixture-created entities that static analysis can only describe symbolically.
- It avoids implementing a full interprocedural Java/Groovy value-propagation engine before proving the thesis pipeline.
- Existing tests already encode realistic happy-path setup and object creation.
- Static analysis still keeps the process targeted and explainable: dynamic analysis observes known candidate sites instead of blindly tracing everything.
- The result can improve multi-saga precision by distinguishing `Tournament(1)` from `Tournament(2)`.

Risks:

- Runtime observation may become application/framework dependent.
- Tests may be slow, flaky, or rely on external services/configuration.
- Logs are often not structured enough for reliable parsing.
- Source-line anchors can drift after code changes.
- Instrumentation may perturb timing-sensitive behavior.
- Dynamic coverage is limited to the tests that actually run.

Mitigation:

- Treat dynamic evidence as optional enrichment, not a required replacement for static facts.
- Record confidence and provenance for every inferred binding.
- Prefer structured instrumentation/events over parsing free-text logs when possible.
- Use stable static anchors where possible: FQN, method name/signature, constructor target type, argument index, command type, aggregate type, and source range/hash.
- Keep type-only/static behavior as a fallback when dynamic evidence is missing.
- Start with a bounded Quizzes smoke subset before broad test execution.

## Evidence model sketch

Introduce a dynamic-evidence artifact, separate from the scenario catalog at first. Possible format: JSONL plus manifest.

Example event kinds:

```text
TEST_STARTED
SAGA_INPUT_OBSERVED
DTO_CONSTRUCTED
COMMAND_CONSTRUCTED
AGGREGATE_ACCESSED
LOG_EVENT_PARSED
TRACE_SPAN_OBSERVED
TEST_FINISHED
```

Example simplified event:

```json
{
  "schema": "microservices-simulator.dynamic-evidence.v1",
  "eventKind": "COMMAND_CONSTRUCTED",
  "testClass": "...CancelTournamentTest",
  "testMethod": "cancel tournament",
  "staticAnchor": {
    "sourceFile": "CancelTournamentTest.groovy",
    "line": 42,
    "targetType": "CancelTournamentCommand"
  },
  "objectType": "CancelTournamentCommand",
  "fields": {
    "tournamentId": "1",
    "studentId": "7"
  },
  "timestamp": "..."
}
```

The follow-up join stage would translate this into scenario evidence such as:

```text
InputVariant.logicalKeyBindings["Tournament.id"] = "1"
StepFootprint.aggregateKey = AggregateKey(..., keyText="1", confidence=EXACT or SYMBOLIC)
```

## Candidate implementation approaches

### Option A — Structured test observer library

Add a small test/runtime observer that records selected events from known framework points or helper methods.

Pros:

- Most reliable evidence.
- Produces structured JSON directly.
- Easier to test.

Cons:

- May require touching application/test code or adding test-only dependencies/hooks.
- Less automatic if many object creation sites need explicit hooks.

### Option B — Java agent / bytecode instrumentation

Use runtime instrumentation to intercept constructors, selected methods, repositories/services, or aggregate access APIs.

Pros:

- Can avoid modifying application source.
- Can observe DTO/command construction and service calls directly.

Cons:

- More complex and potentially fragile.
- Harder to implement and debug quickly.
- Needs careful filtering to avoid too much data.

### Option C — Parse logs and traces only

Run tests and parse existing logs/traces.

Pros:

- Lowest intrusion if logs already contain useful ids.
- Aligns with later impact-analysis observability.

Cons:

- Existing logs may not contain enough source/object/id detail.
- Free-text log formats are brittle.
- Hard to map a log line back to a specific static constructor/input unless correlation is already present.

### Option D — Hybrid observer: structured events first, logs/traces as auxiliary evidence

Use structured runtime events for key binding, and also collect/parse logs/traces where available to improve diagnostics and later impact analysis.

Pros:

- Best balance for thesis progress.
- Keeps exact key binding on reliable structured evidence.
- Still allows exploration of logs/traces as advisor suggested.

Cons:

- Requires designing two evidence paths: primary structured observations and secondary parsed observations.

## Recommended plan

Prefer Option D.

Use static analysis to choose observation targets, then collect structured runtime evidence for those targets. Parse logs/traces only as auxiliary evidence unless they are already structured and correlated.

Phased plan:

### Phase 0 — Design and fixture

- Define `dynamic-evidence.jsonl` and `dynamic-evidence-manifest.json` schema.
- Define stable static anchors.
- Add a tiny dummyapp fixture where static analysis cannot confidently bind an id but runtime can observe it.
- Decide confidence labels for joined evidence:
  - `EXACT_RUNTIME`: concrete value observed at runtime for a specific static anchor;
  - `SYMBOLIC_RUNTIME`: value path observed but concrete value not stable/generalizable;
  - existing `TYPE_ONLY` fallback remains unchanged.

### Phase 1 — Minimal runtime observation

- Run one selected Groovy/Spock test under an observation profile.
- Capture saga/functionality constructor arguments and/or command constructor fields.
- Capture aggregate access calls with observed aggregate ids if there is a stable framework/service point to intercept.
- Write dynamic evidence JSONL/manifest.

### Phase 2 — Join static + dynamic evidence

- Match dynamic events to existing `GroovyFullTraceResult` / `InputVariant` using static anchors.
- Populate `InputVariant.logicalKeyBindings` where the event proves that an input value corresponds to an aggregate id.
- Populate or enrich `StepFootprint.aggregateKey.keyText` where aggregate access evidence proves the concrete key.
- Emit diagnostics when evidence conflicts or is missing.

### Phase 3 — Quizzes bounded smoke

- Run a small Quizzes subset with static analysis + dynamic evidence.
- Compare catalog diagnostics before/after dynamic enrichment:
  - fewer type-only footprints;
  - more exact/symbolic key bindings;
  - better multi-saga candidate filtering.

### Phase 4 — Broaden observability carefully

- Add log-format/tracing parser support only after structured evidence works.
- Use parsed logs/traces for diagnostics and impact-analysis groundwork, not as the only source for exact key binding unless correlation is strong.

## Open questions

1. What is the least invasive way to observe constructor/command/aggregate id values in the current test runtime?
2. Which anchors are stable enough across Java/Groovy source changes: file+line, AST path, FQN+method+argument index, source hash, or a combination?
3. Should runtime-observed concrete ids be treated as exact only for that test run, or generalized to all matching static traces?
4. Do Quizzes tests expose enough aggregate access points through common services/repositories to intercept ids generically?
5. Should the first implementation use dummyapp only, or immediately include one Quizzes smoke test?
6. How should conflicting dynamic evidence be represented in the manifest and catalog warnings?

## Non-goals for the first iteration

- Full distributed tracing across deployed microservices.
- Full bytecode instrumentation of every DTO constructor.
- Replacing the static verifier.
- Guaranteeing coverage for code not exercised by tests.
- Runtime fault injection or impact scoring.

## Success criteria for the first iteration

A minimal successful prototype should show:

1. Static analysis identifies a saga/input/footprint that is type-only or missing an exact key.
2. A selected test is run with observation enabled.
3. Runtime evidence records a concrete id with provenance.
4. The join stage enriches the scenario model with an exact or stronger symbolic binding.
5. The resulting catalog/manifest explains the improvement and preserves warnings for anything still uncertain.
