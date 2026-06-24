# Thesis meeting notes — 2026-W18

Date: 2026-04-28

Purpose: quick preparation notes for the weekly thesis/advisor meeting.

## One-sentence update

Since last Tuesday, we improved the verifier's ability to understand saga test inputs, made the HTML analysis report more useful for inspection, and introduced the first machine-readable scenario catalog that a future `ScenarioExecutor` can consume.

## Simple thesis narrative

The verifier is moving from:

> "the tool can inspect the application and show what it found"

into:

> "the tool can propose bounded, structured fault-injection experiments to run later."

Current pipeline narrative:

1. Parse application source and Groovy tests.
2. Extract saga structure, saga steps, command dispatches, and aggregate read/write effects.
3. Extract or classify possible saga inputs from tests.
4. Generate bounded scenario plans.
5. Export those plans as JSONL plus a manifest.
6. Later: execute them with injected faults.
7. Later: measure impact and prioritize interesting failures.

This week mainly strengthened steps 3 to 5 and clarified the boundary before runtime execution.

## What changed since ~last Tuesday

### 1. Better Groovy/test input tracing

The verifier can now better understand how Groovy tests create saga instances, including values passed through helpers, local transforms, casts/coercions, placeholders, and runtime-only values.

Completed improvements:

- Fixed false helper self-reference/cyclic-reference reports.
- Removed misleading depth-limit markers.
- Treated local transforms such as `.toSet()` and Groovy cast/coercion shapes as local transformations instead of unexplained unknowns.
- Added replay-oriented value categories:
  - resolved values;
  - source placeholders;
  - injectable placeholders;
  - runtime calls;
  - unknown unresolved values.
- Added expected constructor argument types to trace metadata.

Why this matters:

> If we want to execute generated scenarios later, we need to know which saga inputs are directly replayable, which can be reconstructed with placeholders/injection, and which are still unresolved.

### 2. Improved HTML analysis report

The HTML report remains the human-facing artifact, but it is now more useful for inspection and debugging.

Completed improvements:

- Better saga-target browsing and Groovy trace diagnostics.
- Clearer distinction between expected placeholders and severe unresolved values.
- Stable latest report path remains `analysis-report.html` unless configured otherwise.
- Timestamped archived HTML siblings are also generated so past reports are not lost.

Why this matters:

> The report is now useful as evidence that the static extractor is seeing the right saga/input structure, without becoming the runtime contract.

### 3. Fixed interface-based command-handler dispatch

The static analysis now handles a common Spring pattern better.

Completed improvement:

- If a command handler depends on an interface and there is exactly one implementation, the verifier can resolve that implementation and infer the dispatch/access behavior.

Why this matters:

> This removes false gaps for applications that use interface-based dependency injection.

### 4. Added the first machine-readable scenario catalog

This is the main architectural step from static analysis toward execution.

Previously, the verifier mainly produced a human HTML report. Now it also has a structured scenario catalog format:

- `scenario-catalog.jsonl`: one `ScenarioPlan` per line;
- `scenario-catalog-manifest.json`: schema, timestamp, counts, warnings, effective configuration, and output paths.

The catalog can represent:

- saga definitions;
- saga steps;
- step read/write footprints;
- input variants;
- conflict evidence;
- scheduled steps;
- fault spaces;
- generated scenario plans;
- manifest/configuration diagnostics.

Key design decision:

> The HTML report remains for humans. JSONL plus manifest becomes the future executor-facing contract.

Why this matters:

> A future `ScenarioExecutor` should not parse HTML. It should consume structured scenario plans.

Conservative modeling choices:

- Exact aggregate-instance matches are only claimed when supported by extracted facts.
- Type-only conflict matching is explicit and opt-in.
- Missing aggregate names remain unknown/non-matchable.
- Unresolved inputs are preserved as statuses/warnings instead of being fabricated.
- Scenario generation is bounded by configuration to avoid combinatorial explosion.

Safe current defaults include:

- catalog export disabled by default;
- single-saga scenarios enabled;
- max saga set size safely at `1`;
- max scenarios capped;
- type-only fallback disabled by default.

### 5. Documentation/knowledge migration

Verifier implementation knowledge was reorganized into durable docs:

- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- `docs/verifiers-impl/implementation-log/`
- `docs/verifiers-impl/decisions/`
- `docs/verifiers-impl/archive/`

Why this matters:

> The current implementation state, limitations, decisions, and next priorities are now explicit and easier to use for thesis tracking.

## What not to overclaim

Be explicit that this is not finished runtime fault injection yet.

Current limitations:

- `ScenarioExecutor` is not implemented yet.
- Runtime saga/functionality materialization from catalog recipes is not implemented yet.
- Runtime fault injection from generated schedules is not implemented yet.
- Behavior CSV generation is not implemented yet.
- Execution impact scoring is not implemented yet.
- Exact aggregate-instance key extraction is still incomplete.
- Multi-saga scenarios based only on aggregate type should be treated as exploratory, not exact shared-instance evidence.

Good wording:

> The scenario catalog is the missing intermediate representation. It is not the final executor yet, but it makes the next phase concrete.

## Suggested 1-minute verbal update

Since last Tuesday I focused on making the verifier output usable for future execution. I improved Groovy test-input tracing so the tool can classify which saga constructor arguments are replayable, placeholders, runtime calls, or unresolved. I also improved the HTML report so these traces and diagnostics are easier to inspect. Then I introduced a machine-readable scenario catalog: JSONL scenario plans plus a manifest. This gives us a structured contract for a future `ScenarioExecutor` instead of parsing the HTML report. The catalog is intentionally conservative: it avoids claiming exact aggregate conflicts unless the static evidence supports it, and keeps uncertain cases as warnings. For this week, I want to validate the catalog on Quizzes with small bounds, improve exact aggregate-key extraction, and decide the minimal executor contract: whether it consumes JSONL directly or generates simulator CSVs.

## Future plan for this week

### Priority 1 — Run bounded Quizzes smoke with catalog export enabled

Goal:

> Check that the catalog works on the real case-study application, not only synthetic dummy examples.

Validation should check:

- JSONL is parseable;
- scenario IDs are unique;
- manifest counts match output;
- every scheduled step references a known saga/step;
- warnings are understandable;
- HTML report still generates;
- no unbounded explosion occurs.

Advisor-facing wording:

> I want to run the generator on Quizzes with small caps and inspect the manifest diagnostics before expanding scope.

### Priority 2 — Improve exact aggregate-key binding

Goal:

> Reduce reliance on type-only conflict evidence.

Why:

> Multi-saga scenarios are more meaningful when we know two sagas touch the same logical aggregate instance, not merely the same aggregate type.

Advisor-facing wording:

> Right now, the tool can often say "these sagas both touch Tournament", but the stronger claim is "these sagas both touch tournament id X". That is the next precision improvement.

### Priority 3 — Define minimal `ScenarioExecutor` contract

Goal:

> Decide how generated JSONL scenarios become runtime executions.

Main open question:

- Should the executor consume JSONL directly?
- Or should JSONL be translated into existing simulator behavior CSV files?

Suggested position:

> JSONL should probably remain the primary contract, and CSV can be an adapter if needed, but this design should be confirmed.

### Priority 4 — Implement single-saga execution first

Goal:

> Before multi-saga interleavings, execute one generated saga scenario with controlled faults.

Why:

> Single-saga execution validates input materialization, step mapping, and fault-slot semantics with less combinatorial complexity.

Later steps:

- multi-saga schedules;
- runtime fault injection;
- impact scoring;
- GA/bandit prioritization.

## Questions for the advisor

1. Is JSONL plus manifest an acceptable intermediate contract, or should the work align earlier with the simulator's existing behavior CSV format?
2. Should exact aggregate-instance key extraction come before executor work, or should we first build a minimal executor for conservative single-saga scenarios?
3. What is the minimum Quizzes smoke evidence needed before claiming static scenario generation works?
4. Which impact signal should come first once execution exists: invariant violations, exceptions, compensation failures, state divergence, latency, or a combination?
5. Should type-only multi-saga scenarios be allowed as exploratory candidates, or should multi-saga generation wait for stronger exact-key evidence?

## Simple explanation of Priority 2 — why exact aggregate IDs are still hard

Short version:

> We store saga inputs, but we do not yet always know which stored input value is the logical ID of which aggregate instance touched by a saga step.

There are currently two separate pieces of information:

1. From Groovy tests, the verifier sees how a saga/functionality is constructed.
   - Example: `new CancelTournamentFunctionality(tournamentId, userId, ...)`.
   - The verifier can store constructor argument summaries, provenance, and whether the values are resolved/replayable/partial/unresolved.
2. From production-code analysis, the verifier sees that saga steps touch aggregate types.
   - Example: a step writes `Tournament`.
   - But the footprint is often still type-only: "this step touches some `Tournament`", not "this step touches `Tournament(id = 42)`".

The missing bridge is:

```text
Groovy test input
  -> saga constructor parameter
  -> saga field
  -> command constructor argument
  -> command field/getter
  -> command handler/service method argument
  -> aggregate load/write
  -> exact StepFootprint key
```

So the issue is not that inputs are missing. The issue is that the verifier has not fully connected input values to aggregate-instance keys.

Advisor-facing explanation:

> We know the saga was called with some values, and we know a saga step touches `Tournament`, but we do not yet always know which constructor value is the `Tournament` id used by that step. Exact aggregate-key binding is the work needed to make that connection.

Why this matters for multi-saga scenarios:

```text
CancelTournament(tournamentId = 1)
EnrollStudent(tournamentId = 2)
```

Both may touch `Tournament`, so type-only analysis says they are possible conflicts. But exact-key analysis would distinguish `Tournament(1)` from `Tournament(2)` and avoid overclaiming a real shared-instance conflict.

Conversely:

```text
CancelTournament(tournamentId = 1)
EnrollStudent(tournamentId = 1)
```

would become a stronger candidate because both sagas touch the same logical aggregate instance.

Current state in simple terms:

- `InputVariant` stores saga input provenance, summaries, replay status, warnings, and has space for logical key bindings.
- `StepFootprint` stores which aggregate a step reads/writes and with what confidence.
- Many current footprints are still type-only, e.g. "writes Tournament" with no concrete key.
- The next precision improvement is to populate exact/symbolic key bindings where the code evidence supports it.

## For problem 2 (saga input ids)
Dynamic analysis - see logs, event subscriptions, traces etc... to determine those IDs at "runtime" but after runtime.
