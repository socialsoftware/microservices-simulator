# Baseline Investigation Note

## Summary

The legacy simulator behavior CSV generator is impairment infrastructure, not scenario-space enumeration. It expands a manually supplied `functionalities.txt` file into per-functionality CSV files that drive fault and delay choices during runtime execution. It does not discover saga classes, accepted input variants, aggregate interactions, compatible input tuples, or schedules.

For this PRD, the executable baseline is the input-bound brute-force universe: all distinct saga sets up to the configured size, all compatible accepted input tuples for those sets, and all schedules selected by the configured schedule strategy. The verifier should count that universe with compressed accounting instead of attempting to write every represented `ScenarioPlan`, especially for Quizzes-scale runs.

## What The Legacy Behavior CSV Generator Does

`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentGenerator.java` reads an input file grouped by functionality and step. For each step it parses three option groups: fault value, delay-before value, and delay-after value. It computes Cartesian products of those per-step options, balances functionality blocks through a cross-product, and writes one CSV file per functionality.

`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentService.java` exposes this behavior through `generateTestBehaviour`, which looks for a file under the configured test resource directory and constructs `ImpairmentGenerator` when the file exists.

`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentHandler.java` loads the generated `<Functionality>.csv` block for a runtime functionality invocation, increments a per-functionality counter, and returns step-level impairment values.

`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` consumes those values while executing a planned workflow. It reads the behavior map for the current functionality, throws a simulator exception when a step fault value is configured, and applies delay-before/delay-after values around the step execution.

The Quizzes smoke fixture shows the input shape directly: `applications/quizzes/src/test/resources/groovy/GenerateBehaviourTest/functionalities.txt` lists functionality and step rows such as `AddParticipantFunctionalitySagas, getUserStep, [0], [0,500], [0,1000]`. `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/GenerateBehaviourTest.groovy` calls `impairmentService.generateTestBehaviour("functionalities.txt")` to generate those behavior CSV files.

## Why It Is Not Scenario-Space Enumeration

The legacy behavior generator starts from a manually authored list of functionality and step names. It does not parse production saga definitions, does not inspect Groovy test construction traces, and does not build verifier scenario models.

Specifically, it does not discover:

- Saga sets: there is no enumeration of one-saga, two-saga, or three-saga combinations.
- Input variants: it does not consume verifier `InputVariant` records or saga construction recipes.
- Aggregate interactions: it does not inspect command-handler footprints, aggregate keys, or type-only fallback evidence.
- Compatible tuples: it does not reject exact logical-key contradictions or preserve multi-key input signatures.
- Schedules: it does not compute serial or order-preserving interleavings across multiple saga instances.

The generated CSV rows are runtime impairment choices for already-selected functionality executions. They are not `ScenarioPlan` records and they do not define the scenario denominator used for verifier evaluation.

## Why Type-Level Shape Space Is Report-Only

Type-level shape space counts discovered production saga classes and static interaction structure before requiring concrete accepted inputs. It is useful for explaining extraction coverage and missing-input losses, especially when a production saga participates in an interaction but the accepted test-derived input universe has no usable input variant for that saga.

It is report-only because type-level shapes are not directly executable. A future executor needs concrete scenario inputs, not just saga class names or aggregate-type overlap. The glossary distinguishes `Type-level shape space` from the `Input-bound brute-force universe` in `docs/verifiers-impl/glossary.md`, and the PRD keeps missing-input interactions in `typeLevelCoverage` rather than grouped executable rows.

## Why Input-Bound Brute Force Is The Executable Baseline

The input-bound brute-force universe is the fair executable denominator for this stage because every counted shape has accepted input variants and a declared schedule strategy. It is bounded by one concrete run configuration: target application, input policy, source-mode handling, include-singles setting, maximum saga-set size, input-variant bounds, schedule bounds, generation strategy, and catalog write mode.

This matches the current verifier handoff contract. `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` establishes `scenario-catalog.jsonl` and `scenario-catalog-manifest.json` as the structured catalog contract. `docs/verifiers-impl/current-state.md` documents the current normalized scenario IR, deterministic scenario IDs, source-mode filtering, connected saga-set enumeration, input tuple compatibility filtering, and schedule enumeration. Those are the ingredients needed to count or write executable `ScenarioPlan` shapes.

Fault slots can remain present in `ScenarioPlan.faultSpace`, but this PRD does not multiply counts by fault-vector combinations. Fault choices are execution-time semantics for a selected scenario, not part of the static scenario-shape identity used here.

## Why Count-Only Compressed Accounting Is Required For Quizzes

Quizzes has enough saga classes, accepted inputs, and multi-step workflows that brute-force materialization can explode combinatorially. The PRD records that size-3 order-preserving interleavings can exceed one hundred billion represented shapes under current accepted inputs. Writing one JSONL line per shape would be destructive and would obscure the actual thesis claim.

Compressed accounting addresses this by grouping candidates by saga set and computing compatible input-tuple counts and schedule counts mathematically. Count-only catalog mode then writes an empty `scenario-catalog.jsonl` while still producing the manifest, rejected-input diagnostics, and full `scenario-space-accounting.json` counts. This preserves Quizzes-scale measurement without pretending that billions of scenario plans can safely be materialized.

## V1 Limitations

Same-saga multi-instance scenarios are excluded from the v1 comparison universe. A shape where the same saga class appears more than once with different inputs may be valuable for future concurrency analysis, but the current comparison universe enumerates sets of distinct saga FQNs.

Fault-vector combinatorics are also excluded. A counted scenario shape may expose fault slots for later execution, but this accounting stage counts the fault-free/default scenario shape and does not multiply by every possible fault configuration.

## Supporting References

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentGenerator.java` parses behavior option files and writes per-functionality CSV files.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentService.java` wires behavior generation from test resources.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentHandler.java` loads generated CSV blocks for runtime impairment values.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` applies step fault and delay values while executing an already planned workflow.
- `applications/quizzes/src/test/resources/groovy/GenerateBehaviourTest/functionalities.txt` is an example manual behavior option input.
- `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/GenerateBehaviourTest.groovy` calls the generator in a Quizzes behavior test.
- `docs/verifiers-impl/glossary.md` defines legacy behavior CSV generation, type-level shape space, input-bound brute force, scenario-space accounting, count-only catalog mode, and same-saga multi-instance scenarios.
- `docs/verifiers-impl/current-state.md` documents the current verifier scenario catalog pipeline and limitations.
- `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` documents the JSONL/manifest scenario catalog handoff contract.
