# 005 - Type Level Coverage And Interaction Selection

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 004-compressed-brute-force-counting.md
Feature Criteria Covered: AC-4, AC-11
Verification Mode: test
Proof Required: passing tests for strict/broad type-level coverage and interaction-pruned selection

## Slice Contract

Add type-level coverage diagnostics and make `INTERACTION_PRUNED` selected counts/row selection follow the configured strict or broad interaction graph.

## Acceptance Criteria

- Accounting emits `typeLevelCoverage` with discovered production saga FQNs.
- `typeLevelCoverage` includes total discovered saga count.
- `typeLevelCoverage` includes sagas with accepted inputs and sagas without accepted inputs.
- `typeLevelCoverage.strict` includes interaction-pair counts, input-covered interaction-pair counts, missing-input interaction-pair counts, and connected-set counts by size.
- `typeLevelCoverage.broad` includes interaction-pair counts, input-covered interaction-pair counts, missing-input interaction-pair counts, and connected-set counts by size.
- Grouped row interaction summaries have separate `strict` and `broad` subsections.
- Each row interaction subsection includes `connected`, `directPairCount`, and evidence-kind counts for that subsection's graph.
- `INTERACTION_PRUNED` uses strict interaction evidence when type-only fallback is disabled.
- `INTERACTION_PRUNED` uses broad/type-level interaction evidence when type-only fallback is enabled.
- `INTERACTION_PRUNED` still prunes unrelated aggregate types when type-only fallback is enabled.
- `includeSingles=false` excludes singles consistently from selected generator counts.

## Domain Context

- Type-level coverage is pre-input diagnostic coverage over discovered production saga classes.
- Strict and broad interaction are static approximations, not semantic ground truth.
- Input-covered interaction pairs are structurally related pairs where all participating saga classes have accepted inputs.

## Implementation Notes

- Reuse or mirror `ConflictGraphBuilder` and `ConnectedSagaSetEnumerator` semantics.
- Keep type-level interactions that lack inputs out of `groupedSagaSets`; they belong in `typeLevelCoverage` counts.
- Do not add missing-input examples in v1.

## Completion Evidence

- Implementation: Added `typeLevelCoverage` to `ScenarioSpaceAccountingReport`; populated discovered saga FQNs/counts, sagas with accepted inputs, sagas without accepted inputs, strict/broad interaction coverage, and strict/broad grouped-row interaction summaries in `ScenarioSpaceAccountingCalculator`. `INTERACTION_PRUNED` now selects through the strict graph when `allowTypeOnlyFallback=false` and the broad graph when `allowTypeOnlyFallback=true`, while preserving unrelated aggregate pruning and `includeSingles=false` behavior.
- Verification: From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false` passed with 11 tests. From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ScenarioGeneratorSpec,DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` passed with 58 tests.
- Slice compliance review: PASS (`ses_171cf5ad0ffejEPn6lWDEudMRQ`); all acceptance criteria mapped as covered, no required fixes.
- Code quality review: PASS (`ses_171cf5aa7ffeATrsO4EnRUJdeb`); localized/minimal implementation, adequate tests, graph semantics reuse, low bounded regression risk, no required fixes.
- Notes: Quality re-review confirmed `directPairCount` now counts distinct direct saga pairs while evidence-kind counts still count multiple conflict candidates/evidence items; duplicate-evidence regression added.
