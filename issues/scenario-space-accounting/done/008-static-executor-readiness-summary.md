# 008 - Static Executor Readiness Summary

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 004-compressed-brute-force-counting.md
Feature Criteria Covered: AC-15
Verification Mode: test
Proof Required: passing tests for input-level static recipe readiness summary

## Slice Contract

Add a minimal input-level executor readiness diagnostic to the accounting report using only static input recipe readiness and blockers.

## Acceptance Criteria

- Accounting includes `executorReadiness` at input level only.
- `executorReadiness` reports accepted input variant count.
- `executorReadiness` reports executor-ready input variant count from `inputRecipe.executorReady=true`.
- `executorReadiness` reports blocked input variant count.
- `executorReadiness` reports blocker-reason counts from static recipe blockers.
- No scenario-level executor admissibility fields are emitted in v1.
- The scenario executor is not launched.
- The scenario materializer is not invoked.

## Domain Context

- Executor readiness here means static recipe readiness, not runtime replay success.
- The brute-force denominator must not be redefined by current executor limitations.
- Runtime materialization success belongs to later executor reports.

## Implementation Notes

- Reuse existing `inputRecipe.executorReady` and blocker fields.
- Keep the summary compact.
- Do not introduce executor dependencies into the accounting pipeline.

## Completion Evidence

- Implementation: Added top-level `executorReadiness` to `ScenarioSpaceAccountingReport` with accepted input count, executor-ready count, blocked count, and static blocker-reason counts. `ScenarioSpaceAccountingCalculator` derives it from normalized accepted input variants before known-saga filtering, using only `InputRecipe.executorReady` and `InputRecipe.blockers`; no executor or materializer dependency was introduced. Updated current-state docs.
- Verification: From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false` passed with 18 tests. From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ScenarioGeneratorSpec,DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` passed with 69 tests.
- Slice compliance review: PASS (`ses_171b67102ffe7NQP3NwFyW2S4T`); all acceptance criteria and AC-15 coverage mapped as covered, no required fixes.
- Code quality review: PASS (`ses_171b670d6ffepF2UB3kHqLTvtZ`); minimality, maintainability, test adequacy, integration, executor/materializer coupling risk, and regression risk passed, no required fixes.
- Notes: Initial quality review (`ses_171bb710cffe9i83ETUQzSBoF9`) found readiness was counted after known-saga filtering. Fixed by computing from normalized accepted inputs before known-saga filtering and adding regression coverage for unknown-saga accepted inputs and source-mode rejected input exclusion.
