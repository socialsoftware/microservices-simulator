# Remediation 002 - Explicit Domain Failure Classification

Parent spec: `../spec.md`
Independent review follow-up: `../independent-review-follow-up.md`
Depends on: `../remediation-done/001-shared-reader-checksum-verification.md`
Risk: `medium`
Status: `review-complete`

## Finding

`ScenarioExecutor.isDomainFailure` currently treats every `SimulatorException` as a meaningful domain failure. That type is also used for infrastructure conditions such as command-gateway service unavailability after retries. A broken experiment can therefore be reported as `UNASSIGNED_RUNTIME / DEVIATED`, run fallback compensation, continue surviving participants, and remain eligible as a domain outcome.

## Plain-Language Rule

A **domain failure** means the application ran and rejected an operation for a business or transactional reason. It is meaningful experimental evidence, so zero-bit fallback may compensate that participant and continue valid survivors.

An **infrastructure failure** means the application did not get a valid chance to run, for example because a service, provider, version manager, configuration, reflection call, or executor component failed. It invalidates the attempt, so execution must hard-stop and must not be reported as an application outcome.

The executor must not guess from an exception message or assume that every `SimulatorException` is domain-level.

## Required Classification Contract

- Introduce one explicit simulator-level marker or equivalent stable type contract for meaningful domain failures.
- `ScenarioExecutor` uses that explicit contract as the only zero-bit domain-failure signal.
- Unknown, unmarked, plain `SimulatorException`, and ordinary runtime failures are infrastructure failures by default.
- Application domain exceptions used by the supported Quizzes Saga/local path carry the explicit domain signal.
- Meaningful Saga/local simulator transactional failures used by the executor carry the explicit domain signal.
- Command-gateway retry exhaustion, missing infrastructure/configuration, and leaked assigned-fault exceptions do not carry the domain signal.
- Do not classify by matching exception messages.
- Preserve exact exception type/classification through the supported command-response restoration path.
- Do not redesign unrelated causal, TCC, stream, or gRPC execution behavior. If a transport is touched, change only what is required to preserve the existing exception type/classification contract.

## Required Executor Behavior

### Explicit domain failure during a zero-bit forward body or automatic commit

- record `failureOrigin = UNASSIGNED_RUNTIME`;
- enter one immediate checkpoint-recovery episode;
- skip that participant's remaining forward actions;
- continue still-valid actions for other participants;
- report `DEVIATED` when fallback completes.

### Unmarked or infrastructure failure during a forward body or automatic commit

- do not invoke fallback recovery;
- do not continue later survivor actions;
- hard-stop with the existing forward/commit infrastructure reason;
- report `INCOMPLETE` once measured execution has begun;
- remain ineligible as a domain outcome.

## Concrete Examples

| Failure | Classification | Expected result |
|---------|----------------|-----------------|
| Aggregate is already used by another Saga | explicit domain | compensate-and-continue; `DEVIATED` |
| Quizzes business invariant rejects input | explicit domain | compensate-and-continue; `DEVIATED` |
| Service unavailable after command retries | infrastructure | hard-stop; `INCOMPLETE` |
| Version manager/configuration missing | infrastructure | hard-stop; `INCOMPLETE` |
| Unknown plain `SimulatorException` | infrastructure | hard-stop; `INCOMPLETE` |
| Assigned-fault exception leaks outside its planned path | infrastructure/contract failure | hard-stop; never relabel as zero-bit domain |

## Implementation Guidance

- Prefer an explicit marker interface plus a narrowly scoped marked simulator-domain exception over error-message parsing or an executor allowlist of text.
- Keep `SimulatorException` as the common transport/runtime base if needed, but do not let base-class membership imply domain meaning.
- Preserve application-specific exception identity and formatted/template messages across local command restoration.
- Migrate only the domain-producing sources required by the supported Saga/local executor and Quizzes evidence path.
- Do not reclassify compensation exceptions; their existing first-failure hard-stop policy remains unchanged.
- Do not change report schemas or status vocabulary.

## Understandable Documentation Required

Update the current contract documentation so a reader can answer all of these without reading code:

1. What counts as a domain failure?
2. What counts as an infrastructure failure?
3. Why is plain `SimulatorException` insufficient?
4. What does the executor do for each category?
5. What is the conservative behavior for an unknown/unmarked exception?
6. Which execution boundary is covered now: supported Saga/local replay, not generic distributed parity?

Clarify ambiguous phrases such as “domain/simulator exception” to mean **explicitly domain-classified simulator/application exception**, not every `SimulatorException`. Keep historical documents historical and avoid claiming generic stream/gRPC/TCC parity.

At minimum inspect and update the relevant current passages in:

- `spec.md`;
- `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`;
- `docs/verifiers-impl/reference/scenario-executor.md`;
- `docs/verifiers-impl/current-state.md` and glossary if needed for a clear live definition.

## Focused Tests

At minimum prove:

1. an explicitly marked domain body failure still compensates and continues a survivor;
2. an explicitly marked domain commit failure still compensates and continues a survivor;
3. a plain `SimulatorException` no longer triggers fallback;
4. service-unavailability-shaped simulator failure hard-stops during body execution;
5. infrastructure failure during commit hard-stops;
6. infrastructure failure invokes no fallback compensation and no later survivor action;
7. report conformance is `INCOMPLETE`, not `DEVIATED`, for infrastructure failure after measured execution starts;
8. application or simulator domain classification survives supported command exception restoration;
9. an assigned-fault exception cannot become an `UNASSIGNED_RUNTIME` fallback;
10. existing compensation-failure hard stops remain unchanged.

## Verification

Run targeted simulator tests covering the exception marker/type and command restoration, then install the simulator dependency used by the verifier:

```bash
cd simulator
mvn -Dtest=<targeted-test-list> test
mvn -DskipTests install
```

Run the focused executor suite from the verifier module:

```bash
cd ../verifiers
mvn -Dtest=ScenarioExecutorSpec test
```

Run targeted Quizzes tests if its exception contract or source is changed. Also run scoped `git diff --check` across every changed source, test, documentation, and issue path.

## Evidence to Record

Append completion evidence to this file with:

- the exact classification type contract;
- production throw sites/class declarations migrated and why;
- behavior and transport tests added;
- exact commands, test counts, and results;
- documentation updated;
- any intentionally unsupported boundary or remaining concern.

Do not commit, move this slice, edit existing review reports, start remediation item 3, or touch the two pre-existing meeting-note files.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added the stable simulator-level marker interface `DomainFailure` and the marked `SimulatorDomainException` subtype. `SimulatorException` remains the shared runtime/transport base but no longer implies domain meaning.
- Changed `ScenarioExecutor` to use `instanceof DomainFailure` as its only zero-bit domain-failure test. There is no exception-message matching or class-name allowlist.
- Marked `QuizzesException` explicitly and migrated the ten existing `SagaUnitOfWorkService` domain/transactional throw sites (aggregate missing/deleted/in-use and inactive-aggregate modification) from plain `SimulatorException` to `SimulatorDomainException`.
- Preserved the existing command-response restoration mechanism. `SimulatorDomainException.fromRemote` and the existing application `fromRemote` factory restore exact marked types while retaining the error template and formatted message; focused coverage includes the serialized local gateway path.
- Kept fallback body/commit behavior and compensation-failure hard stops unchanged for explicitly marked failures. Unmarked failures now hard-stop conservatively without fallback or survivor continuation.

### Files Changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/exception/DomainFailure.java` — explicit marker contract.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/exception/SimulatorDomainException.java` — marked simulator transactional exception with formatting constructors and remote restoration factory.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` — migrate the ten supported Saga transactional/domain throw sites.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/CommandGatewayExceptionRestorationTest.java` — marker, retry-exhaustion, response-envelope, exact application/simulator type, template/formatted-message, and serialized local-gateway restoration coverage.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesException.java` — carry the explicit domain marker.
- `applications/quizzes/src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/QuizzesExceptionDomainFailureTest.java` — Quizzes marker and remote template/formatted-message regression.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — classify only `DomainFailure` as domain.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — explicit-domain, plain-simulator, service-unavailability, infrastructure, and leaked-assigned-fault fixtures.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — body/commit classification, no-fallback/no-survivor, `INCOMPLETE`, and assigned-fault leakage regressions while retaining existing compensation hard-stop coverage.
- `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md` — current classification decision in plain language.
- `docs/verifiers-impl/reference/scenario-executor.md` — operator-facing category and behavior contract.
- `docs/verifiers-impl/current-state.md` — implemented boundary and conservative default.
- `docs/verifiers-impl/glossary.md` — live domain/infrastructure/fallback definitions.
- `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md` — one clarification pointer from the historical policy to the explicit current marker rule.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md` — this completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd simulator && mvn -Dtest=CommandGatewayExceptionRestorationTest,SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` (red phase) | FAIL (expected) | Test compilation failed because `DomainFailure` and `SimulatorDomainException` did not yet exist. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` (red phase) | FAIL (expected) | Test compilation failed because the explicit simulator-domain type did not yet exist. |
| `cd simulator && mvn -Dtest=CommandGatewayExceptionRestorationTest,SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` (final) | PASS | 21 tests run, 0 failures, 0 errors, 0 skipped; counts: restoration 4, Saga executor control 6, stepwise recovery 4, workflow regression 7. |
| `cd simulator && mvn -DskipTests install` | PASS | Simulator `3.2.0-SNAPSHOT` installed successfully for verifier consumption. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | 62 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. The first post-implementation run exposed only three incorrect test accessor names (`exceptionType` versus `exceptionClass`); after correcting those assertions, all 62 passed. |
| `cd applications/quizzes && mvn -Ptest-sagas -Dtest=QuizzesExceptionDomainFailureTest test` | BLOCKED, then equivalent PASS | The normal target was root-owned and failed resource copying with `Permission denied`. Running the same profile/test from a temporary same-directory POM that changed only Maven's build output to `/tmp/quizzes-remediation-002-target` passed 1 test, 0 failures/errors/skips; the temporary POM was removed. |
| Local Markdown-link script over the five changed current/decision docs | PASS | All relative Markdown targets exist. |
| `rg` inspection of `ScenarioExecutor` classification | PASS | The only domain predicate is `failure instanceof DomainFailure`; no message-content classification was found. |
| Scoped `git diff --check` plus trailing-whitespace scan over all 15 remediation-002 paths | PASS | No whitespace errors; the temporary Quizzes POM is absent. |

### Acceptance Criteria Evidence

- Explicit body and commit domain regressions still compensate, continue survivors, retain `UNASSIGNED_RUNTIME`, and complete as `DEVIATED`.
- Plain `SimulatorException`, service-unavailability-shaped `SimulatorException`, ordinary runtime body/commit failures, and leaked `FaultVectorInjectedFaultException` report infrastructure outcomes with no fallback compensation, no later survivor body, and `INCOMPLETE` after the first measured action.
- Simulator and application marked exception identity, marker, template, and formatted message survive command-response restoration, including `LocalCommandGateway` serialization.
- Existing scheduled/fallback compensation-failure tests remain in the passing 62-test executor suite and continue to hard-stop as `COMPENSATION_FAILED / INCOMPLETE`.
- No report schema/status vocabulary changed, and package/artifact behavior was not modified by this remediation.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the explicit contract/restoration and executor regressions before production classes and observed the expected compilation-red state. After implementation, the new behavior assertions passed; three test-only accessor typos were corrected before the final green run.

### Deviations From Plan

- The parent `spec.md` was inspected but not edited because the active implementation-worker contract forbids modifying spec/plan artifacts. The accepted ADR, live current-state, glossary, executor reference, and one necessary historical clarification pointer now state the explicit rule.
- The root-owned Quizzes `target/` prevented the literal targeted command. The equivalent test used an ephemeral same-directory POM with only `/tmp` build output; no repository POM was changed.

### Blockers / Follow-Ups

- No implementation blocker. Classification is intentionally limited to supported Saga/local replay and the Quizzes exception boundary. Causal, TCC, stream, gRPC, and generic distributed parity remain unsupported.
- The parent spec still contains historical shorthand such as “domain/simulator exception”; review may decide whether a later planning-artifact maintenance step should add a clarification despite the implementation-worker restriction.

## Review Attempt 01 Fix Evidence

Status: `partial`

### Fix Summary

- Added unmarked `QuizzesConfigurationException` and migrated all 76 `UNDEFINED_TRANSACTIONAL_MODEL` throws across the seven Quizzes functionality-wrapper classes to it. Genuine Quizzes business/invariant failures continue to use marked `QuizzesException`.
- Changed command restoration to inspect only a subtype's own declared `fromRemote` factory and to accept the result only when its runtime class exactly equals the requested exception class. An inherited `SimulatorException.fromRemote` can no longer masquerade as a subtype factory.
- Added an own `FaultVectorInjectedFaultException.fromRemote` factory. Serialized local restoration now preserves the exact unmarked subtype plus its template and formatted diagnostic message. Transport does not reconstruct unavailable structured fault fields; restored instances use null/`-1` sentinels for those fields.
- Reclassified the saved bounded service-unavailability report in live documentation as pre-remediation historical evidence. The saved artifact remains unchanged, while current docs state the expected no-fallback/no-survivor `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` behavior.

### Exact Files Changed for Attempt 01

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/CommandGateway.java` — reject inherited/wrong-result factories.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorInjectedFaultException.java` — exact unmarked remote restoration factory and diagnostic preservation.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/CommandGatewayExceptionRestorationTest.java` — inherited-factory and serialized assigned-fault subtype regressions.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesConfigurationException.java` — explicit unmarked configuration failure type.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/answer/coordination/functionalities/QuizAnswerFunctionalities.java` — configuration throws migrated.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java` — configuration throws migrated; business throws remain `QuizzesException`.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/question/coordination/functionalities/QuestionFunctionalities.java` — configuration throws migrated; business contract remains marked.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/coordination/functionalities/QuizFunctionalities.java` — configuration throws migrated.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/topic/coordination/functionalities/TopicFunctionalities.java` — configuration throws migrated; topic invariant throw remains marked.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java` — configuration throws migrated; tournament business throws remain marked.
- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/user/coordination/functionalities/UserFunctionalities.java` — configuration throws migrated; user invariant throws remain marked.
- `applications/quizzes/src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/QuizzesExceptionDomainFailureTest.java` — representative marked business and unmarked configuration assertions.
- `docs/verifiers-impl/current-state.md` — current Quizzes classification and historical-report interpretation.
- `docs/verifiers-impl/reference/scenario-executor.md` — operator-facing classification and historical-report interpretation.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/002-explicit-domain-failure-classification.md` — this attempt-01 fix evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd simulator && mvn -Dtest=CommandGatewayExceptionRestorationTest test` (red) | FAIL (expected) | 5 tests ran; the new serialized assigned-fault regression received plain `SimulatorException` instead of exact `FaultVectorInjectedFaultException`. |
| Safe temporary-POM Quizzes command (red) | FAIL (expected) | Test compilation failed because `QuizzesConfigurationException` did not yet exist. Root-owned `target/` was untouched. |
| `cd simulator && mvn -Dtest=CommandGatewayExceptionRestorationTest,SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` (final) | PASS | 23 tests run, 0 failures, 0 errors, 0 skipped: restoration 6, executor control 6, stepwise recovery 4, workflow regression 7. The non-failing local OpenTelemetry collector warning remained environmental. |
| `cd simulator && mvn -DskipTests install` | PASS | Simulator `3.2.0-SNAPSHOT` installed successfully after the production restoration changes. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | 62 tests run, 0 failures, 0 errors, 0 skipped. Existing report coverage proves leaked assigned-fault exceptions hard-stop as infrastructure with no fallback or survivor continuation. |
| Safe temporary-POM Quizzes command using `-Ptest-sagas -Dtest=QuizzesExceptionDomainFailureTest` and `/tmp/quizzes-remediation-002-fix-green-2` build output | PASS | 2 tests run, 0 failures, 0 errors, 0 skipped. The temporary POM was removed and root-owned output ownership was not changed. |
| Quizzes source audit | PASS | Exactly 76 undefined-model throws across seven files use `QuizzesConfigurationException`; zero still use marked `QuizzesException`. |
| Relative Markdown-link check | PASS | Six relative links across `current-state.md`, executor reference, and the inspected parent spec resolve. |
| Scoped `git diff --check` and temporary-file check | PASS | No scoped whitespace errors; temporary Quizzes POM absent. |

### Finding Evidence

- Finding 1 code/tests: representative `TOPIC_MISSING_NAME` remains a marked `QuizzesException`; `UNDEFINED_TRANSACTIONAL_MODEL` is an exact unmarked `QuizzesConfigurationException`; all 76 production sites use the latter.
- Finding 2 code/tests: `getDeclaredMethod` rejects inherited static factories, exact-class validation rejects wrong factory results, and `FaultVectorInjectedFaultException.fromRemote` preserves exact unmarked identity/template/message through serialized `LocalCommandGateway`. The focused executor suite retains direct report-level hard-stop/no-fallback coverage.
- Finding 3 live docs: both current-state and executor reference label the old `PARTIAL_COMPENSATED / DEVIATED` service-unavailability report as pre-remediation historical evidence and state today's `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` interpretation without rewriting the artifact.

### Deviations / Remaining Blocker

- The requested parent `spec.md` wording/AC correction was not applied because the active implementation-worker contract explicitly prohibits editing spec/plan artifacts. This leaves the third review finding only partially resolved despite corrected live documentation.
- No other deviation. Remediation 001, review reports, item 3, meeting notes, report schemas, and historical runtime artifacts were not changed.

## Review Attempt 01 Caller Completion

Status: `implemented-awaiting-review`

The caller completed the planning-artifact clarification that the implementer profile was not allowed to make:

- Updated the six current failure-policy passages in `spec.md`, including AC-31.
- Replaced broad `domain/simulator exception` shorthand with the explicit rule: only simulator/application failures carrying the domain-failure marker use zero-bit fallback.
- Stated the conservative complement directly: plain or unmarked `SimulatorException`, unknown, configuration, executor, and infrastructure failures hard-stop.
- Preserved the existing Saga/local scope and all other acceptance criteria.

Verification:

- `rg` finds no remaining `domain/simulator`, `domain or simulator`, or ambiguous `simulator exception` failure-policy phrase in `spec.md`.
- Relative documentation links and `git diff --check` pass after the clarification.

All three attempt-01 findings are now implemented and ready for review attempt 02.

## Review Attempt 02 Fix Evidence

Status: `implemented-awaiting-review`

The remaining documentation-only finding is corrected:

- `current-state.md` no longer claims that the saved Quizzes smoke demonstrates a meaningful current domain failure. It now states that no post-remediation Quizzes domain-fallback smoke has been recorded and that the saved service-unavailability run would currently hard-stop.
- `evidence.md` labels the saved Docker execution as pre-remediation historical evidence.
- The appendix now reports its actual plain, unmarked service-unavailability exception instead of attributing the failure to a null course name.
- The old recorded `PARTIAL_COMPENSATED / DEVIATED` artifact remains unchanged, while the text states the current expected behavior: no fallback, no survivor action, and `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`.

Only documentation changed after review attempt 02. Focused link, ambiguity, documentation-build, and diff-hygiene checks are required before attempt 03; production test reruns remain valid because no code changed.

## Review Outcome

- Attempt 01: `FAIL` — broad Quizzes configuration marking, inherited remote-factory identity loss, and inconsistent contract/evidence documentation.
- Attempt 02: `FAIL` — both code findings resolved; one stale evidence-interpretation statement remained.
- Attempt 03: `PASS` — no blocking, major, minor, or note finding remains.
- Final code verification retained from fresh attempt 02: 23 simulator tests, simulator install, 62 executor tests, and two Quizzes classification tests passed.
- Final documentation verification: docs build, 18 relative links, structure checks, stale-claim scans, saved-report inspection, and `git diff --check` passed.
- Final state: remediation item 2 is review-complete; no commit has been made.
