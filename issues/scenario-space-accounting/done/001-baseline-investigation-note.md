# 001 - Baseline Investigation Note

Mode: AFK
Parent PRD: ./prd.md
Blocked by: None
Feature Criteria Covered: AC-18
Verification Mode: docs
Proof Required: baseline investigation note exists with required explanation and references

## Slice Contract

Create the advisor-facing baseline investigation note for this PRD package. The note must explain why the existing simulator behavior CSV generator is not the thesis scenario-space baseline and how input-bound brute-force compressed accounting reframes the baseline.

## Acceptance Criteria

- `issues/scenario-space-accounting/baseline-investigation-note.md` exists.
- The note explains what the legacy behavior CSV generator does.
- The note explains why the legacy behavior CSV generator does not discover saga sets, input variants, aggregate interactions, compatible tuples, or schedules.
- The note explains why type-level shape space is report-only.
- The note explains why input-bound brute force is the executable baseline.
- The note explains why count-only compressed accounting is required for Quizzes-scale brute-force runs.
- The note documents same-saga multi-instance exclusion as a v1 limitation.
- The note documents that fault slots remain present but fault-vector combinatorics are out of scope for this stage.
- The note includes concrete code/doc references supporting the claims.

## Domain Context

- The legacy behavior CSV generator is existing impairment infrastructure, not scenario-space enumeration.
- Brute force in this PRD means all accepted input-bound saga sets and schedules under a declared run configuration.
- Fault vectors are execution-time semantics and are not multiplied into scenario-shape counts in this PRD.

## Implementation Notes

- Reference simulator impairment code such as `ImpairmentGenerator`, `ImpairmentService`, `ImpairmentHandler`, `ExecutionPlan`, Quizzes `GenerateBehaviourTest`, and `functionalities.txt` where useful.
- Reference verifier docs such as `docs/verifiers-impl/glossary.md`, `current-state.md`, and the scenario catalog ADR where useful.
- Keep this as an explanation note, not a design spec for implementation.

## Completion Evidence

- Implementation: Added `issues/scenario-space-accounting/baseline-investigation-note.md` explaining the legacy behavior CSV generator, why it is not scenario-space enumeration, why type-level shape space is report-only, why input-bound brute force is the executable baseline, why count-only compressed accounting is needed for Quizzes, v1 same-saga multi-instance exclusion, and fault-vector scope.
- Verification: PASS by reading `issues/scenario-space-accounting/baseline-investigation-note.md` and confirming all required sections/references are present.
- Slice compliance review: PASS, task `ses_1722c95fdffevYpu4Gf5FDmn3X`.
- Code quality review: PASS, task `ses_1722c95d2ffem9l3ewr1PFwFVB`.
- Notes: Docs-only slice; no code changes or Maven verification required.
