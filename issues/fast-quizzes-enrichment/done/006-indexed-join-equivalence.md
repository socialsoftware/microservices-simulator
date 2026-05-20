## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Refactor dynamic evidence joining around compact events and plan-indexed accumulation so statuses and summaries are equivalent while avoiding all-analyses/all-plans rescanning.

This slice is the main join-side memory and scalability improvement. It must preserve the existing join status ladder and enriched output semantics.

## Acceptance criteria

- [x] Existing dynamic joiner behavior specs pass against compact evidence events.
- [x] Direct `inputVariantId` matching remains the strongest signal and still produces `MATCHED_EXACT` only when the id belongs to the scenario plan being enriched.
- [x] Fallback matching through test identity, functionality name, and step name does not broaden relative to the pre-refactor behavior.
- [x] Observed steps, commands, aggregate accesses, matched executions, warnings, and status counts remain equivalent for existing fixture evidence.
- [x] A generated fixture with 66 scenario plans and 20,000 evidence events joins successfully and completes under 10 seconds in the verifier module test environment.
- [x] The generated fixture assertion verifies public output counts and representative observed summaries rather than private index structures.

## Validation

- Refactored `DynamicEvidenceJoiner` to build a `JoinIndex` from compact events once, separating direct `inputVariantId` events by input id and fallback analyses by relevant plan id.
- Updated enrichment to read exact events and fallback analyses from the plan-oriented index instead of rescanning all event analyses for every scenario plan.
- Preserved the join status ladder: direct ids remain strongest and only produce `MATCHED_EXACT` for plans containing that input id; fallback identity/functionality/step behavior is still covered by existing joiner specs.
- Added a generated public-output fixture with 66 scenario plans and 20,000 evidence events that asserts `dynamicEventsRead=20000`, status counts (`MATCHED_EXACT=1`, `UNMATCHED=65`), no warnings, representative observed step kinds, and representative command summaries without asserting private index internals.
- Ran `mvn -Dtest=DynamicEvidenceJoinerSpec test` in `verifiers/`: passed, 18 tests.
- Ran `mvn -Dtest=DynamicEvidenceReaderSpec,DynamicEvidenceJoinerSpec test` in `verifiers/`: passed, 22 tests.

## Blocked by

- Blocked by `issues/fast-quizzes-enrichment/005-compact-evidence-reader.md`

## User stories addressed

- User story 4
- User story 15
- User story 16
- User story 17
- User story 23
