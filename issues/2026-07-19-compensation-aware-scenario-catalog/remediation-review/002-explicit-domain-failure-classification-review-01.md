# Remediation Review: 002 - Explicit Domain Failure Classification

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Remediation card and completion evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/002-explicit-domain-failure-classification.md`
- Parent spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan and original runtime slice: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/done/007-runtime-fallback-and-hard-stops.md`
- Independent follow-up: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Context map/glossary: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- Current decision/reference/status docs:
  - `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
  - `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
  - `docs/verifiers-impl/reference/scenario-executor.md`
  - `docs/verifiers-impl/current-state.md`
- Reviewed dependency, preserved without re-review: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md` and latest remediation-001 review report.
- Remediation-002 production/test changes reviewed:
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/exception/DomainFailure.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/exception/SimulatorDomainException.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java`
  - `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/CommandGatewayExceptionRestorationTest.java`
  - `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesException.java`
  - `applications/quizzes/src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/QuizzesExceptionDomainFailureTest.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`
  - `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- Related runtime sources audited: `SimulatorException`, `FaultVectorInjectedFaultException`, `CommandGateway`, `CommandResponse`, `LocalCommandGateway`, `LocalCommandService`, `Workflow`, `ExecutionPlan`, `CentralizedVersionService`, all 68 Quizzes Saga workflow classes, all Quizzes command handlers/services/aggregates, and all production `SimulatorException` / `QuizzesException` constructions in the supported source trees.
- Existing bounded report inspected: `verifiers/target/compensation-aware-v3-evidence/execution-report-25c0d61a.json`.
- Actual base-to-worktree diffs reviewed, with remediation-001 reader/on-demand changes and reports treated only as the accepted dependency.

## Summary

The executor's central behavior is substantially correct: `DomainFailure` is now the only fallback predicate; explicit body and commit failures still recover and continue survivors; plain/service-unavailability/runtime/assigned-leak failures hard-stop; and compensation failures retain their first-failure hard stop. Fresh targeted runs pass 21 simulator tests, simulator install, 62 executor tests, and the one Quizzes contract test.

The remediation cannot pass because three major contract/documentation defects remain. First, marking the broad `QuizzesException` class also marks 76 configuration failures (`UNDEFINED_TRANSACTIONAL_MODEL`) as domain failures. Those configuration branches are not measured forward-body paths in the current direct Saga replay, but the exception signal itself still violates the approved rule that configuration failures do not carry the marker. Second, command restoration accepts inherited static `fromRemote` methods; a serialized `FaultVectorInjectedFaultException` is restored as plain `SimulatorException`, violating exact exception-type restoration and leaving the assigned-leak transport case unproved. Third, the parent spec was not updated and the live current-state/reference docs contradict their new rule by continuing to present a service-unavailability `SimulatorException` report as a current meaningful `DEVIATED` outcome.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Explicit marker is the only executor fallback signal | pass | `ScenarioExecutor.java:270,320,555-557`; only `failure instanceof DomainFailure` is used. |
| Plain/unknown/runtime/service-unavailability failures hard-stop | pass | Fresh executor suite; `ScenarioExecutorSpec.groovy:387-489` asserts `INCOMPLETE`, infrastructure reasons, no compensation, and no survivor continuation. |
| Leaked assigned-fault exceptions hard-stop | partial | Direct executor case passes at `ScenarioExecutorSpec.groovy:443-464`; serialized local restoration does not preserve its exact type. |
| Application/configuration failures carry the correct signal | fail | `QuizzesException.java:11` marks every instance, including 76 `UNDEFINED_TRANSACTIONAL_MODEL` constructions. |
| Exact local command exception restoration | fail | Marked application/simulator cases pass, but inherited factory lookup loses `FaultVectorInjectedFaultException` identity. |
| Body and commit domain fallback preserved | pass | `ScenarioExecutorSpec.groovy:231-341,360-385`; explicit failures report `UNASSIGNED_RUNTIME / DEVIATED`, recover, and continue survivors. |
| Compensation hard stops preserved | pass | `ScenarioExecutorSpec.groovy:491-599`; scheduled and fallback failures remain `COMPENSATION_FAILED / INCOMPLETE` with no continuation. |
| Scope/non-goals respected | pass | No causal/TCC/stream/gRPC redesign, report schema change, dependency, item-3 work, commit, meeting-note edit, or source refactor was introduced. |
| Remediation-001 dependency preserved | pass | The 62-test executor suite includes the shared-reader checksum rejection regression and passes; remediation-001 code/reports were not re-reviewed or altered. |
| Understandable, internally consistent documentation | fail | Parent spec remains broad, and live evidence text contradicts the explicit service-unavailability rule. |
| Dependency done | pass | Remediation 001 is in `remediation-done/` with review attempt 02 `PASS`. |

## Acceptance Criteria Review

| AC / remediation requirement | Verdict | Evidence | Notes |
|---|---|---|---|
| AC-31 / explicit zero-bit body and commit fallback | pass | Fresh `ScenarioExecutorSpec` body and commit cases; report outcomes and survivor assertions pass. | Applies now only through the marker predicate. |
| AC-33 / infrastructure hard-stop | pass | Plain `SimulatorException`, service-unavailability body, ordinary runtime body/commit, and plain commit cases all report `INCOMPLETE` with no fallback. | Version-manager absence remains plain/unmarked at `CentralizedVersionService.java:48-59`. |
| AC-34 / compensation hard-stop | pass | Scheduled/fallback explicit and implicit compensation failure tests remain green. | No automatic retry/continuation regression. |
| AC-38 / conformance | pass | Explicit fallback completes as `DEVIATED`; measured infrastructure and compensation stops are `INCOMPLETE`. | Assertions inspect reports, not just thrown types. |
| Explicit application classification | fail | `QuizzesException` marks both business failures and configuration failures. | Narrow supported replay reach does not make the type contract correct. |
| Exact command restoration | fail | Own factories preserve current marked types, but inherited `SimulatorException.fromRemote` changes an assigned-fault subtype to the base class. | Missing serialized assigned-leak regression. |
| AC-46 / live documentation | fail | Current rule text is good, but current evidence and parent spec remain contradictory/stale. | Required remediation documentation is incomplete. |

## Reachability and Source Audit

- `SagaUnitOfWorkService` contains exactly ten `new SimulatorDomainException` constructions and no remaining `new SimulatorException` construction. They cover missing/deleted aggregate, aggregate-in-another-Saga, and inactive-aggregate modification conditions. These are transactional/domain rejection sources on the Saga path. The two rollback/commit helper occurrences do not weaken compensation policy because compensation catches every throwable and hard-stops independently of `DomainFailure`.
- All 68 Quizzes `*/coordination/sagas/*.java` workflow classes were inspected. They invoke local commands and contain no direct `QuizzesException` throw; one async class wraps an exception in ordinary `RuntimeException`, which remains unmarked.
- Quizzes command handlers delegate to services/aggregates. Their business/invariant failures use `QuizzesException`, so current supported domain sources are covered by the marker.
- Service retry exhaustion remains exact plain `SimulatorException` at `CommandGateway.java:68-83`; the fresh service-unavailability body report probe hard-stops.
- Missing version-manager state remains plain `SimulatorException` at `CentralizedVersionService.java:48-59`; it is reachable during Saga unit-of-work version operations and therefore conservatively hard-stops.
- Legacy behavior-file faults in `ExecutionPlan` remain plain `SimulatorException`; assigned executor faults use the unmarked `FaultVectorInjectedFaultException`.
- Causal `SimulatorException` sources remain unmarked and are outside the supported Saga/local executor boundary, as required.
- The configuration over-marking is real production type behavior: seven Quizzes functionality wrapper classes contain 76 `new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL)` sites. They are reachable during Spring `@PostConstruct`/wrapper dispatch under a bad profile. They are not currently reachable as a measured forward action because the executor materializes the direct Saga workflow FQN, not those wrapper services, and supported startup uses `sagas,local`. Thus the immediate report risk is bounded, but the required signal contract is still violated.

## Verification Evidence Check

| Command / method | Verdict | Notes |
|---|---|---|
| `cd simulator && mvn -Dtest=CommandGatewayExceptionRestorationTest,SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Fresh `BUILD SUCCESS`; 21 tests, 0 failures, 0 errors, 0 skipped. |
| `cd simulator && mvn -DskipTests install` | pass | Fresh `BUILD SUCCESS`; simulator `3.2.0-SNAPSHOT` installed. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Fresh `BUILD SUCCESS`; 62 tests, 0 failures, 0 errors, 0 skipped. This includes remediation-001's executor checksum guard. |
| Quizzes test via temporary same-directory POM with build directory `/tmp/remediation-002-review-quizzes-target` | pass | `QuizzesExceptionDomainFailureTest`: 1 test, 0 failures/errors/skips. Root-owned `applications/quizzes/target` was not modified, removed, or re-owned; the temporary POM was removed. |
| Report probes in `ScenarioExecutorSpec` | pass | Service-unavailable body, plain body/commit, explicit body/commit, ordinary runtime body/commit, assigned-fault leak, and compensation hard-stop cases assert outcome, conformance, no compensation/no continuation as appropriate. |
| Quizzes configuration marker JShell probe | fail | `new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL) instanceof DomainFailure` printed `true`. |
| Inherited `fromRemote` JShell probe | fail | `FaultVectorInjectedFaultException.class.getMethod("fromRemote", ...)` is declared by `SimulatorException`; invocation returns `SimulatorException`, not the requested subtype, while retaining template/message. |
| Existing bounded Quizzes report inspection | fail (documentation evidence) | Report failure class is plain `SimulatorException`; message is `Service 'course' unavailable after retries exhausted ...`; old report says `PARTIAL_COMPENSATED / DEVIATED`, contrary to the new current contract. |
| Relative Markdown-link check | pass | 15 relative links across the five current/decision docs plus parent spec resolve. |
| `git diff --check` | pass | No tracked whitespace errors. |
| Conflict marker/temp-file/status hygiene | pass | No conflict markers; reviewer temporary POM removed; meeting notes and remediation-001 review reports untouched. |

One preliminary Maven command was mistakenly invoked from the repository root and failed immediately because there is no root POM. It made no changes; the required command was rerun successfully from `simulator/` and the passing result above is the verification result.

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Marker, narrow subtype, ten migrations, and one executor predicate change are otherwise surgical. |
| Existing patterns | partial | Own `fromRemote` factories fit existing transport, but inherited static lookup is an existing trap the explicit exact-type contract needed to close or cover. |
| Test quality | partial | Executor report tests are strong. Transport tests cover marked simulator/application exceptions and retry exhaustion but omit a serialized unmarked subtype/assigned-leak case; Quizzes test does not distinguish its configuration failures. |
| Regression risk | fail | Broad Quizzes marking and subtype restoration violate explicit conservative classification/type contracts despite green suites. |
| Security/data safety | pass | No destructive operation, ownership change, package mutation, or external data change occurred. |
| Change hygiene | pass | Diff checks pass and only review artifacts are written by this review. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| major | The broad application marker classifies configuration failures as domain failures. This violates remediation lines 17, 28, and 58: missing configuration must not carry the signal. | `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesException.java:11`; 76 `new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL)` sites, including `TopicFunctionalities.java:40-48,68-69`; independent probe prints `domainMarker=true`. | Narrow the marked application type or migrate every configuration throw to an unmarked infrastructure type. Add focused coverage proving `UNDEFINED_TRANSACTIONAL_MODEL` is unmarked while representative Quizzes business/invariant failures remain marked. Do not use message matching in the executor. |
| major | Supported command restoration does not preserve exact exception identity for a `SimulatorException` subtype without its own static factory. The inherited-static trap turns `FaultVectorInjectedFaultException` into plain `SimulatorException`; the current assigned-leak test bypasses transport. | `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/CommandGateway.java:113-145`; `FaultVectorInjectedFaultException.java:5-21`; probe reports factory declaring class and restored class as `SimulatorException`. This violates remediation line 30 and the explicit assigned-leak transport concern. | Make restoration reject inherited factories and preserve the requested supported subtype through the response path (including an explicit restoration contract for the assigned-fault subtype if it can leak). Add serialized `LocalCommandGateway` coverage asserting exact unmarked subtype, template, formatted message, and hard-stop/no-fallback report behavior. |
| major | Required documentation remains inconsistent. The parent spec still says every “domain/simulator” exception falls back, while live current-state/reference text presents a service-unavailability `SimulatorException` report as a current `DEVIATED` domain outcome immediately after saying such failures hard-stop. | `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md:18,51,150,158,160,284`; `docs/verifiers-impl/current-state.md:100-107,145`; `docs/verifiers-impl/reference/scenario-executor.md:121-125,165-169`; inspected report records plain `SimulatorException` and `Service 'course' unavailable after retries exhausted...`. This violates `issues/2026-07-19-compensation-aware-scenario-catalog/remediation/002-explicit-domain-failure-classification.md:71-89`. | Update the parent spec's current contract/AC wording to “explicitly domain-classified simulator/application exception.” Reconcile current-state/reference evidence by rerunning it under the new classifier or clearly labeling the old report as pre-remediation historical evidence and stating its current expected hard-stop/`INCOMPLETE` interpretation. Keep the accepted ADR/glossary's current boundary wording. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: Three major findings violate the explicit classification, exact restoration, and documentation requirements.

## Recommendation

Return remediation 002 to implementation. Fix only the three findings, add the missing classification/transport regressions, rerun the same 21-test simulator set, simulator install, 62+ executor suite, safe Quizzes contract tests, links, and diff hygiene. Do not start remediation item 3 or alter remediation-001 artifacts.
