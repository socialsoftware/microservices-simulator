# 003 - Dummyapp Accounting Fixture Foundation

Mode: AFK
Parent PRD: ./prd.md
Blocked by: None
Feature Criteria Covered: AC-16
Verification Mode: test
Proof Required: passing static-analysis tests proving each required fixture shape is visible

## Slice Contract

Add the minimal dummyapp fixture shapes needed by later accounting tests. This slice is intentionally fixture-focused, but it must be tightly bounded and prove that static analysis can see each required shape before accounting uses it.

## Acceptance Criteria

- Dummyapp fixture code exposes unrelated sagas with accepted inputs.
- Dummyapp fixture code exposes strict interacting sagas with exact or symbolic shared interaction evidence.
- Dummyapp fixture code exposes broad/type-only interacting sagas.
- Dummyapp fixture code exposes an interacting saga that lacks accepted input coverage.
- Dummyapp fixture code exposes a three-saga connected chain.
- Dummyapp fixture code exposes multi-step sagas suitable for order-preserving schedule-count validation.
- Dummyapp fixture code exposes compatible and incompatible logical-key input variants.
- Existing or new static-analysis tests prove each required fixture shape is visible in the verifier model.

## Domain Context

- `applications/dummyapp/` is the canonical verifier fixture corpus for parser/scenario/accounting edge cases.
- This slice should not modify Quizzes.
- Fixture additions are verifier test fixtures, not product application behavior.

## Implementation Notes

- Keep fixture additions minimal and deterministic.
- Prefer explicit fixture names that make later test expectations readable.
- Do not implement scenario-space accounting in this slice.
- If existing dummyapp shapes already satisfy a requirement, add targeted assertions instead of duplicating fixtures.

## Completion Evidence

- Implementation: added the bounded dummyapp cancellation saga fixture, accepted-input Groovy traces, symbolic dispatch key extraction, logical-key binding extraction, parser expectation update, and model-level fixture visibility coverage in `DummyappAccountingFixtureFoundationSpec`.
- Verification: PASS, `mvn test -Dtest=DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` from `verifiers/` passed with 7 tests; PASS, `mvn test -Dtest=DummyappAccountingFixtureFoundationSpec,WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec -DfailIfNoTests=false` from `verifiers/` passed with 76 tests after final boundary fix.
- Slice compliance review: PASS, task `ses_171e7bc60ffemeVnZC8JVTL10A`; reviewer confirmed every issue acceptance criterion and PRD AC-16 are covered, including the real three-saga chain topology.
- Code quality review: PASS, task `ses_171e7bc3cffeiWzYl39oy669TC`; reviewer confirmed minimality, maintainability, test adequacy, adapter boundary preservation, and low regression risk.
- Notes: a reviewer-identified topology gap was fixed by asserting adjacent chain edges and no direct endpoint edge; a reviewer-identified boundary issue was fixed by keeping dispatch key confidence in `StepDispatchFootprint` and mapping to scenario `FootprintConfidence` only in the adapter.
