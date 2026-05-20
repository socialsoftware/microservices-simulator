## Parent PRD

`issues/input-ownership-ambiguity/prd.md`

## Type

AFK

## What to build

Complete the validation and documentation pass for ownership-aware attribution and plan-local ambiguity containment. Update implementation documentation so future work can understand the new ownership model, then run the planned dummyapp regression surface and Quizzes directional smoke check. Record interpretation boundaries without making brittle exact-count promises.

This slice is intentionally cross-cutting because it verifies and documents the completed behavior across the full dynamic-enrichment path.

## Acceptance criteria

- [x] Documentation explains the difference between provenance and ownership for analyzed saga inputs.
- [x] Documentation lists the ownership contexts supported by this work: direct feature, helper, `setup()`, field initializer, inherited variants, and `setupSpec()` metadata.
- [x] Documentation explains that `setupSpec()` runtime-attribution improvement is out of scope for this work.
- [x] Documentation explains plan-local ambiguity containment and why genuine same-feature sibling ambiguity can remain expected behavior.
- [x] Focused verifier and simulator tests relevant to ownership attribution and ambiguity containment pass in the appropriate Maven modules.
- [x] Dummyapp regression coverage is run or documented with the exact command and result.
- [x] A Quizzes directional smoke run is run or documented with the exact command, artifact path, and interpretation.
- [x] Quizzes smoke interpretation checks for directional improvement and absence of contradictions with focused tests, not brittle exact count targets.
- [x] Documentation or notes make clear that semantic deduplication, executor materialization, and stronger same-feature sibling disambiguation remain future work.

## Validation

Documentation updated:

- `docs/verifiers-impl/dynamic-enrichment-joining-explained.md` now distinguishes provenance from ownership, lists supported ownership contexts, states that `setupSpec()` runtime attribution is out of scope, explains plan-local ambiguity containment, and records future work for semantic deduplication, executor materialization, and stronger same-feature sibling disambiguation.
- `docs/verifiers-impl/current-state.md` now records ownership metadata in the implemented Groovy/input and dynamic-enrichment surfaces, the ownership-aware runtime/fallback matching behavior, plan-local ambiguity containment, and the same future-work boundaries.

Focused verifier and simulator tests:

- Command: `mvn -Dtest=DynamicEvidenceJoinerSpec,DynamicInputMapWriterSpec,GroovyConstructorInputTraceVisitorDummyappSpec test` from `verifiers/`.
- Result: BUILD SUCCESS, 42 tests, 0 failures.
- Command: `mvn -Dtest=DynamicInputMapTest test` from `simulator/`.
- Result: BUILD SUCCESS, 7 tests, 0 failures.

Dummyapp regression coverage:

- Command: `mvn -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec test` from `verifiers/`.
- Result: BUILD SUCCESS, 22 tests, 0 failures.
- Public surfaces covered include adapted input ownership for helper/setup/field/inherited/setupSpec contexts, dynamic input-map export, verifier fallback matching, and simulator runtime input-map resolution.

Quizzes directional smoke:

- Full default command attempted: `docker compose run --rm fault-analysis-scenario-gen` from the repository root.
- Result: exceeded the 900000 ms tool timeout after writing `/home/andre/microservices-simulator/verifiers/target/quizzes-20260519-011031-593/` and selecting 42 dynamic test classes; no enriched join report was produced before timeout.
- Narrow directional command completed: `docker compose run --rm -e VERIFIERS_DYNAMIC_ENRICHMENT_INCLUDE_TEST_DIRS=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/answer fault-analysis-scenario-gen` from the repository root.
- Artifact path: `/home/andre/microservices-simulator/verifiers/target/quizzes-20260519-012349-564/`.
- Key artifacts: `scenario-catalog-enriched-manifest.json`, `dynamic-evidence-join-report.json`, per-test `dynamic-input-map.json`, per-test `maven-output.log`.
- Result: `dynamic-evidence-join-report.json` reports `runStatus=COMPLETE`, `testClassesSelected=2`, `testClassesPassed=2`, `testClassesFailed=0`, `testClassesTimedOut=0`, `dynamicEventsRead=630`, `eventsMissingTestContext=0`, `MATCHED_EXACT=5`, `MATCHED_HIGH_CONFIDENCE=0`, `AMBIGUOUS=0`, `UNMATCHED=61`, `warningCount=0` in the enriched manifest.
- Interpretation: this is a directional smoke, not an exact-count acceptance target. The run shows exact attribution is still produced on the realistic Quizzes target, no ambiguity spillover appears in this narrowed slice, test-context attribution is present, and the result does not contradict the focused verifier/simulator tests. It is not a claim about the full 42-class Quizzes run because that broader run timed out in the tool window.

## Blocked by

- Blocked by `issues/input-ownership-ambiguity/002-contain-plan-local-ambiguity.md`
- Blocked by `issues/input-ownership-ambiguity/003-apply-ownership-to-fixtures.md`
- Blocked by `issues/input-ownership-ambiguity/004-use-ownership-in-verifier-fallback.md`

## User stories addressed

- User story 16
- User story 17
- User story 18
- User story 19
- User story 20
- User story 21
- User story 23
- User story 24
