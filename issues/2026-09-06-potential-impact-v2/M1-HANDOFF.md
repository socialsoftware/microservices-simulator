# M1 handoff — deterministic checks and scored report

State: complete; integrated review and M2 qualification passed.

## Outcome

Implemented FR-5 through FR-11 over the shared M0 evidence. Every ordinary supported
ScenarioExecutor attempt now writes
`microservices-simulator.scenario-impact-v2-assessment.v1` at the existing derived
`*.impact-v2.json` path. The separately versioned sidecar preserves the raw evidence and
adds package-manifest identity, three deterministic category results, candidate scope,
findings, unknown reasons, the affected-object union, and completeness semantics.

ImpactV1 and `microservices-simulator.scenario-execution-report.v5` are unchanged.

## Actual files changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2Assessor.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2EvidenceCollector.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2EvidenceReport.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2AssessorSpec.groovy`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- this handoff

No simulator M0 source, application main code, package artifact, ImpactV1 contract, or
execution-v5 contract was changed in M1. No commit, push, merge, Docker run, production
change, or deployment was performed.

## Rules and report semantics

- `DELETED_DEPENDENCY` requires an ACTIVE final source, a resolved subscription target,
  an observed non-DELETED state during measurement, a committed DELETED transition, and
  a final DELETED target. A baseline-absent target can qualify after observed creation;
  a pre-existing deletion does not.
- `FAILED_OPERATION_RESIDUAL` derives failed and completed recovery from the production
  action, participant, and lifecycle records. It requires a committed failed-Saga write,
  sole Saga ownership across the whole attempt, and a latest tracked durable state that
  matches the final application-data/lifecycle projection. Unknown writers, other Sagas,
  event consumers, or an unobserved later change make that object unknown. Framework
  metadata, semantic locks and rejected writes cannot create a residual finding.
- `UNRESOLVED_DELIVERED_EVENT` matches the exact successful scheduled action to delivery
  writer/action, event type/id, publisher identity/version and receiver logical id. The
  same typed receiver must have unchanged application data and lifecycle across delivery,
  survive without DELETED lifecycle, and remain eligible through the final polymorphic
  predicate. Missing delivery/final eligibility is unknown; removal or final ineligibility
  clears the finding.
- Positive aggregate identities are unioned across categories. `COMPLETE` serializes a
  numeric `completeScore`, including zero. `PARTIAL` retains findings and a numeric
  `observedAffectedObjectCount` lower bound while `completeScore` is explicitly null.
  `INVALID` and `UNAVAILABLE` serialize both counts as null. All three category rows are
  always present in stable order.

## Discovery and classification

Autonomous implementation details:

- The M0 snapshot collector labels both baseline and final projection gaps `SNAPSHOT`.
  Persistent-comparison gaps conservatively prevent residual positives, while dependency
  findings remain usable when an unrelated application-data field is unsupported.
- The current framework represents deletion in aggregate lifecycle state. A missing final
  snapshot is therefore unknown rather than inferred physical deletion.
- Successful scheduled event actions are the event candidate source. A delivery fact by
  itself is insufficient, and a scheduled success without one exact matching fact is
  unknown.
- Assessment exceptions are contained independently of sidecar-write exceptions. The
  fallback preserves raw facts, records `ASSESSMENT_FAILED`, and leaves the execution
  outcome intact.

No material scope delta was found. Physical-delete observation outside the current
lifecycle model and field-level competing-writer attribution remain out of scope.

## Proof

- `mvn -DskipTests compile` in `verifiers/`: passed.
- `mvn -Dtest=ScenarioExecutorSpec,ImpactV2AssessorSpec test` in `verifiers/`: 153 passed
  (143 ScenarioExecutor plus 10 focused ImpactV2 contracts), 0 failures/errors/skips.
- `./scripts/verifier-docs build`: passed.
- `git diff --check`: passed.
- The focused contracts cover all three positives and union deduplication, baseline-absent
  deletion, pre-existing deletion, restored residual and converged event zero controls,
  competing event-consumer and unobserved final writes, malformed writer evidence,
  typed receiver mismatch/removal, partial lower-bound behavior, invalid/unavailable null
  fields, pre/post-event projection gaps, physical final absence without a delete fact,
  raw-fact preservation and assessment-failure containment.

Root completed the full verifier suite (763 tests, no failures/errors/skips); integrated
review and M2 Docker qualification passed. See HANDOFF.md and M2-HANDOFF.md.

## What to try

Run any supported Saga/local ScenarioExecutor attempt and inspect the sibling
`*.impact-v2.json`. A complete healthy attempt should show the three categories with
`coverageStatus: COMPLETE`, empty findings/unknown reasons and `completeScore: 0`.
Disabling collection should retain the sidecar with `assessmentStatus: UNAVAILABLE`,
`collectionReason: COLLECTION_DISABLED`, and explicit null count fields.
