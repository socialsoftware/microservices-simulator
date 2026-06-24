# Thesis meeting notes — 2026-W21

Date: 2026-05-19

Purpose: draft notes for the next advisor meeting, focused on the completed runtime `inputVariantId` attribution slice and the remaining dynamic-enrichment work.

## One-sentence update

The verifier now has a real static-to-dynamic attribution path: Quizzes runtime evidence can carry verifier-generated `inputVariantId`s, and the comparable full/default sagas-only run moved from zero exact scenario matches to forty-six exact matches.

## What changed since the previous meeting

The previous meeting framed runtime `inputVariantId` propagation as the next precision step. That slice is now implemented and measured.

Completed pieces:

- the verifier writes one `dynamic-input-map.json` per selected test class;
- the simulator loads that map when dynamic evidence is enabled;
- runtime step, command, and aggregate-access events can carry a top-level `inputVariantId`;
- step events also record attribution diagnostics such as `MATCHED` or `NO_MATCH`;
- the dynamic joiner upgrades scenario plans to `MATCHED_EXACT` when runtime evidence carries an `inputVariantId` that belongs to the static plan being enriched;
- dummyapp tests prove the before/after shape on a controlled fixture;
- Quizzes was rerun with the same sagas-only 42-class selection used by the earlier Task 8 baseline.

## Pipeline narrative

Current implemented pipeline:

1. Static analysis parses application source and tests.
2. Static analysis extracts saga/functionality structure, workflow steps, command dispatches, aggregate footprints, and test-derived `InputVariant`s.
3. The verifier exports `scenario-catalog.jsonl`.
4. For dynamic enrichment, the verifier writes a per-test-class `dynamic-input-map.json` from accepted static input variants.
5. The verifier runs selected application tests one class at a time with simulator dynamic evidence enabled.
6. The simulator records runtime step, command, and aggregate-access events.
7. When runtime attribution is unambiguous, those events carry the static `inputVariantId`.
8. The verifier joins runtime evidence back into sidecar enriched artifacts.
9. The original static catalog remains unchanged; dynamic enrichment stays sidecar-only.

The important design choice is that static analysis still defines the candidate space. Dynamic analysis does not invent scenarios; it tells us which static inputs were actually seen at runtime and how confidently they can be joined.

## Measurement result

Comparable full/default Quizzes sagas-only run:

```text
selected test classes = 42
passed test classes   = 40
failed test classes   = 2
dynamic events read   = 18868
scenario plans read   = 66
```

Before runtime `inputVariantId` propagation:

```text
MATCHED_EXACT           = 0
MATCHED_HIGH_CONFIDENCE = 2
AMBIGUOUS               = 44
UNMATCHED               = 20
NOT_COVERED             = 0
warningCount            = 8238
```

After runtime `inputVariantId` propagation:

```text
MATCHED_EXACT           = 46
MATCHED_HIGH_CONFIDENCE = 0
MATCHED_PARTIAL         = 0
AMBIGUOUS               = 3
UNMATCHED               = 17
NOT_COVERED             = 0
warningCount            = 328
```

Runtime attribution diagnostics from the refreshed run:

```text
step events with inputVariantAttributionStatus=MATCHED = 226
command/aggregate events carrying propagated inputVariantId = 270
step events with inputVariantAttributionStatus=NO_MATCH = 8096
```

Run status:

```text
runStatus = PARTIAL
```

The two failed Quizzes classes are the same known failures as before and were already reproduced without dynamic-enrichment instrumentation. Current interpretation remains that they are existing Quizzes/runtime-test failures, not regressions introduced by the attribution slice.

## What this proves

- The static catalog and runtime evidence are now connected by a durable identifier, not only by semantic shape.
- Dynamic enrichment is no longer mostly ambiguous on the full Quizzes sagas-only run.
- The biggest improvement is certainty: the system can now say "this static input variant produced this runtime evidence" for most covered plans.
- The measurement surface is useful: exact, ambiguous, unmatched, warning count, and failed-test counts all move independently and expose different risks.

## What not to overclaim

- This is still dynamic enrichment, not the `ScenarioExecutor`.
- The verifier is not yet running generated fault scenarios.
- Total non-unmatched coverage only moved from `46` to `49` plans, so this should not be described as "dynamic now covers everything".
- `AMBIGUOUS=3` and `UNMATCHED=17` still need classification.
- Runtime attribution currently uses test identity, functionality class FQN, and step name. It does not yet use command payload fields, aggregate access ids, literal argument hints, or aggregate keys to prune candidates further.
- Dynamic parity is still local/sagas only: no stream/gRPC/distributed/TCC parity yet.

## Remaining work for next slice

First, classify the remaining misses before implementing more heuristics:

- `AMBIGUOUS=3`: inspect whether these are same-shape neighboring inputs where direct runtime ids point to a different static input variant than the plan being enriched.
- `UNMATCHED=17`: separate genuinely uncovered static inputs from joiner limitations.
- For each miss, record whether command fields, aggregate accesses, literal hints, or aggregate keys would be enough to resolve it.

Then implement the smallest useful refinement:

- prefer candidate pruning based on structured runtime facts already present in evidence;
- avoid broad name matching;
- keep direct `inputVariantId` exact only when it belongs to the scenario plan being enriched;
- report contradictions instead of guessing.

Sidecar cleanup:

- decide whether to populate enriched `matchedTestExecutions[].testRunStatus` from orchestrator metadata, or explicitly keep the join report as the source of truth for per-class pass/fail counts.

## Advisor discussion points

1. Is the exact-match improvement (`0 -> 46`) a strong enough milestone to move toward executor design while classifying the remaining misses in parallel?
2. Should the next evaluation emphasize exact-match rate, miss classification, or aggregate-key precision?
3. Is it acceptable that the dynamic stage remains sidecar-only, keeping the static `scenario-catalog.jsonl` reproducible?
4. For the next refinement, should command/aggregate/literal pruning come before more exact aggregate-key extraction?
5. What should count as sufficient evidence that selected tests exercise the useful static inputs?

## Suggested verbal update

Last week I had the static catalog and dynamic evidence bridge, but the full Quizzes run was still mostly ambiguous. This week I implemented the planned runtime input attribution path. The verifier writes a per-test input map, the simulator loads it during instrumented test runs, and runtime events can now carry the static `inputVariantId`. On the comparable Quizzes sagas-only run, exact matches went from zero to forty-six out of sixty-six plans, ambiguity dropped from forty-four to three, and warning volume dropped from over eight thousand to three hundred twenty-eight. The remaining work is to classify the three ambiguous and seventeen unmatched records carefully before adding more attribution rules, so that the next improvements do not create false exact matches.
