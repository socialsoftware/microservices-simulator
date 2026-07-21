# Remediation Review: 002 - Explicit Domain Failure Classification

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Remediation card, original completion evidence, attempt-01 fix evidence, and caller completion: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/002-explicit-domain-failure-classification.md`
- Parent spec and AC-31: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Independent follow-up: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Attempt-01 review: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-review/002-explicit-domain-failure-classification-review-01.md`
- Context/plan/runtime anchors: `CONTEXT-MAP.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/done/007-runtime-fallback-and-hard-stops.md`
- Reviewed dependency, preserved without re-review: remediation 001 card and latest PASS report.
- Current documentation:
  - `docs/verifiers-impl/current-state.md`
  - `docs/verifiers-impl/reference/scenario-executor.md`
  - `docs/verifiers-impl/evidence.md`
  - `docs/verifiers-impl/glossary.md`
  - `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
  - `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
- Complete remediation-002 production/test diff, including:
  - simulator marker/domain exception, Saga unit-of-work migrations, command restoration, assigned-fault restoration, and focused tests;
  - Quizzes marker/configuration exception, all seven functionality-wrapper migrations, and focused tests;
  - executor predicate/fixtures/spec;
  - parent spec and all changed live/decision/glossary documentation.
- All 68 Quizzes Saga workflow classes and current production `SimulatorException`, `SimulatorDomainException`, `QuizzesException`, and `QuizzesConfigurationException` sources in the supported Saga/local trees.
- Existing bounded report: `verifiers/target/compensation-aware-v3-evidence/execution-report-25c0d61a.json`.
- Remediation-001 reader/on-demand diffs were treated only as the accepted dependency; its implementation and reports were not re-reviewed.

## Summary

Attempt-01 findings 1 and 2 are resolved. All 76 undefined-transactional-model sites now use exact unmarked `QuizzesConfigurationException`; representative Quizzes business/invariant failures remain marked. Command restoration ignores inherited factories, rejects wrong-result factories, and serialized local restoration preserves exact unmarked `FaultVectorInjectedFaultException` identity plus template/formatted diagnostic. Fresh targeted simulator, executor, and Quizzes tests all pass.

The documentation finding is only partially resolved. The parent spec, AC-31, current-state executor policy, and executor reference now state the explicit marker rule, and both live pages correctly label the old service-unavailability report as pre-remediation historical evidence. However, `current-state.md` still says that the current Quizzes smoke demonstrates a meaningful domain failure, and the linked live `evidence.md` appendix still says the same service-unavailability run proves valid zero-bit fallback/survivor continuation. Those statements directly contradict the new interpretation immediately above and leave the requested current documentation internally inconsistent. This is a major finding, so attempt 02 remains `FAIL`.

## Attempt-01 Resolution

| Attempt-01 finding | Verdict | Evidence |
|---|---|---|
| Broad `QuizzesException` marks configuration failures | resolved | Exact audit: 76 `new QuizzesConfigurationException(UNDEFINED_TRANSACTIONAL_MODEL)`, zero marked `QuizzesException` uses, across seven files. `QuizzesConfigurationException` does not implement `DomainFailure`; Quizzes test proves unmarked configuration and marked `TOPIC_MISSING_NAME`. |
| Inherited `fromRemote` loses assigned-fault identity | resolved | `CommandGateway.java:130-148` uses `getDeclaredMethod` and exact result-class equality. Fresh transport tests preserve exact `FaultVectorInjectedFaultException`; independent wrong-result probe restores the requested subtype rather than accepting the base result. |
| Parent/live documentation broad or contradictory | partial | Spec/AC-31 and main current-state/reference passages are fixed, but `current-state.md:172` and `evidence.md:133` retain the superseded interpretation. |

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Explicit marker is the only zero-bit fallback signal | pass | `ScenarioExecutor.java:270,320,555-557`; predicate is only `failure instanceof DomainFailure`; no message classification. |
| All undefined-model configuration failures are unmarked | pass | Exact source audit found 76/76 `QuizzesConfigurationException` sites and 0 marked sites. |
| Quizzes business/invariant failures remain marked | pass | `QuizzesException implements DomainFailure`; 47 active business/invariant constructions remain, with representative focused test. |
| Plain/unknown/runtime/service failures hard-stop | pass | Fresh executor report tests cover plain/service body, plain/runtime body/commit, no fallback, no survivor, and `INCOMPLETE`. |
| Assigned-fault leakage remains infrastructure | pass | Serialized transport retains exact unmarked subtype; executor report test records `INFRASTRUCTURE_FAILED`, no `UNASSIGNED_RUNTIME`, no compensation, and no survivor. |
| Exact command restoration | pass | Marked application/simulator types and unmarked assigned-fault subtype preserve exact class, marker state, template, and formatted message. Inherited/wrong-result factories are rejected. |
| Domain body and commit fallback | pass | Explicit marked body/commit cases recover, skip owner suffix, continue survivors, retain `UNASSIGNED_RUNTIME`, and complete `DEVIATED`. |
| Compensation hard stops | pass | Scheduled/fallback explicit and implicit compensation failure cases remain `COMPENSATION_FAILED / INCOMPLETE`, no continuation/retry. |
| Supported boundary remains Saga/local | pass | Docs and code make no causal/TCC/stream/gRPC/distributed parity claim or redesign. |
| Remediation-001 regression | pass | Fresh 62-test executor run includes and passes checksum mismatch rejection before selection/execution. |
| Documentation understandable and internally consistent | fail | Live current-state/evidence still give the old service-unavailability report a current meaningful-domain interpretation. |
| Dependency done | pass | Remediation 001 remains review-complete with PASS attempt 02. |

## Acceptance Criteria Review

| AC / requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-31 / explicit body and commit fallback | pass | Updated AC text plus fresh body/commit report tests. | Unknown/plain/unmarked complement is explicit. |
| AC-33 / infrastructure hard-stop | pass | Plain, service-unavailable, runtime, configuration type, and assigned-leak sources remain unmarked; executor tests assert `INCOMPLETE`. | Version-manager absence remains plain `SimulatorException`. |
| AC-34 / compensation hard-stop | pass | Scheduled/fallback compensation failure tests pass unchanged. | No survivor continuation or automatic retry. |
| AC-38 / report conformance | pass | Explicit fallback is `DEVIATED`; measured infrastructure/compensation prefixes are `INCOMPLETE`. | Direct report assertions. |
| Exact transport contract | pass | Six focused simulator restoration tests pass; exact assigned subtype and diagnostics are covered. | Remote structured fault fields intentionally use documented sentinels; required type/classification/template/message are preserved. |
| AC-46 / live documentation | fail | Main policy passages pass, but current evidence interpretation remains contradictory at current-state/evidence lines cited below. | Required correction remains. |

## Source and Reachability Audit

- Exactly 76 undefined-model constructions across seven Quizzes functionality-wrapper files use `QuizzesConfigurationException`; zero use `QuizzesException`.
- `QuizzesConfigurationException` extends `SimulatorException` and does not implement `DomainFailure`.
- Representative active business/invariant sites remain marked, including topic name, user input, tournament input, and execution input failures.
- `SagaUnitOfWorkService` still contains exactly ten `SimulatorDomainException` constructions and zero plain `SimulatorException` constructions. Their aggregate missing/deleted/in-use/inactive conditions remain semantic Saga/domain failures. Compensation policy remains independent and catches all throwables.
- All 68 Quizzes Saga workflow classes remain on the direct supported path. Their sole direct throw is an ordinary `RuntimeException` wrapper in an async workflow and therefore conservatively unmarked.
- Service retry exhaustion remains plain `SimulatorException` at `CommandGateway.java:81-83`; version-manager absence remains plain at `CentralizedVersionService.java:48-59`; assigned faults remain an unmarked subtype.
- Causal sources remain plain/unmarked and outside current executor parity.
- No executor exception-message allowlist or matching was found.

## Verification Evidence Check

| Command / method | Verdict | Notes |
|---|---|---|
| `cd simulator && mvn -Dtest=CommandGatewayExceptionRestorationTest,SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Fresh `BUILD SUCCESS`; 23 tests, 0 failures, 0 errors, 0 skipped. Counts: restoration 6, control 6, stepwise 4, workflow 7. |
| `cd simulator && mvn -DskipTests install` | pass | Fresh `BUILD SUCCESS`; simulator installed for verifier consumption. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Fresh `BUILD SUCCESS`; 62 tests, 0 failures, 0 errors, 0 skipped. |
| Safe Quizzes temporary-POM test with `/tmp/remediation-002-review02-quizzes-target` | pass | 2 tests, 0 failures/errors/skips. `applications/quizzes/target` remains root-owned and untouched; temporary POM absent. |
| Independent wrong-result factory probe | pass | A declared factory returning base `SimulatorException` was rejected; restoration fell back to exact requested unmarked subtype with formatted diagnostic. |
| Undefined-model source audit | pass | 76 configuration constructions, zero marked constructions, seven files. |
| Supported-source audit | pass | 68 Saga workflows, ten marked Saga unit-of-work sources, zero plain sources there, infrastructure anchors remain unmarked. |
| Relative Markdown link check | pass | 15 relative links across spec and changed current/decision/glossary docs resolve. |
| Markdown structural check | pass | Balanced fences and readable heading structure in spec/current-state/reference/ADR/glossary. No repository docs build tool/configuration is present. |
| Documentation ambiguity scan | fail | Found stale current-domain-smoke claim at `current-state.md:172`; linked current evidence appendix retains old interpretation at `evidence.md:133`. Historical 2026-07-08 ADR wording is explicitly historical and not treated as a defect. |
| `git diff --check` | pass | No tracked whitespace errors. Untracked remediation files have no unexpected trailing whitespace. |
| Conflict/temp/status/ownership hygiene | pass | No conflict markers or temporary POM; root-owned Quizzes target ownership unchanged; meeting notes and remediation-001 reports untouched. |

OpenTelemetry localhost collector warnings occurred after assertions in simulator/verifier runs; Maven reported success and the exact counts above.

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Configuration subtype/migrations and transport guard/factory are narrow fixes to the findings. |
| Existing patterns | pass | Uses the existing simulator exception hierarchy and declared `fromRemote` factory convention. |
| Test quality | pass | Tests distinguish marked/unmarked types, exact transport identity/diagnostics, inherited factories, report outcomes, fallback, no continuation, and compensation stops. |
| Regression risk | pass | Fresh simulator, install, executor, Quizzes, and remediation-001 executor guard all pass. |
| Security/data safety | pass | No destructive operation, ownership change, package mutation, or external data change. |
| Change hygiene | pass | No dependency/schema/status-vocabulary change or scope expansion. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| major | Current documentation still contradicts the explicit-marker interpretation of the saved Quizzes service-unavailability report. The corrected evidence paragraphs call it pre-remediation infrastructure, but the current limitations section says that this same current smoke demonstrates a meaningful domain failure; the linked live evidence appendix still says it proves zero-bit fallback and survivor continuation. | `docs/verifiers-impl/current-state.md:145,162,172`; `docs/verifiers-impl/reference/scenario-executor.md:169-171`; `docs/verifiers-impl/evidence.md:105-133`. The saved report's exception is plain `SimulatorException` with service-unavailability-after-retries text, so current behavior is `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`, not meaningful fallback. This violates `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/002-explicit-domain-failure-classification.md:71-89` and the attempt-02 requirement for consistent current documentation. | Remove or correct the `current-state.md:172` claim, and label the v3 evidence appendix's saved execution/interpretation as pre-remediation historical evidence with the current no-fallback/no-survivor `INCOMPLETE` interpretation. Preserve the report artifact itself and explicitly historical v2 sections. Rerun link/ambiguity/diff checks. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: One major documentation consistency finding remains.

## Recommendation

Return remediation 002 for one documentation-only correction. Preserve the resolved production/tests and do not start item 3. After the current-state/evidence wording is reconciled, rerun focused docs link/ambiguity/diff hygiene; source/test reruns need only be repeated if code changes.
