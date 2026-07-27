# Make scenario materializability trustworthy and practical

- Workflow: SP coordinator v1
- State: `complete`
- Work shape: `System`
- Route: `Guided`
- Updated: `2026-07-27`
- Workspace: current checkout on `fault-analysis/scenarios`
- Git: committed locally on `fault-analysis/scenarios`; no push, PR, or merge authorized
- Authority: implement and commit the approved package locally; no remote or other external actions authorized
- Implementation approval: `approved (user: “alright i approve - go ahead”, 2026-07-27)`

## Current Checkpoint

- Completed: Delivered both outcomes, repaired all four review findings, passed focused/full/Quizzes proof, updated canonical docs, and received independent `PASS`.
- Current: Complete.
- Problem: none.
- User input: none.
- Next: none for the approved package.

## Intent Brief

### Outcome

A Quizzes input reported as materializable can be constructed from its exact persisted tuple and used to start the exact Saga through ScenarioExecutor, without starting one Spring application per candidate. Known source-derived values must not be lost while tracing common test helpers. Whether an all-zero or faulty execution succeeds remains an observed execution result, not part of materializability.

### Representative Example

For Quizzes, the executor checks the currently marked candidates in one application process. `GetCourseExecutionsFunctionalitySagas` setup succeeds; incompatible `GetCourseExecutionByIdFunctionalitySagas` or `FindQuizFunctionalitySagas` arguments fail before replay with an exact constructor/type diagnostic. A helper call such as `createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)` produces a `UserDto` recipe containing those setter values rather than an empty DTO. A normal or faulty execution can then be run on demand and reports its own runtime outcome.

### In Scope

- One reusable executor-owned setup check that materializes persisted arguments, resolves runtime-owned dependencies, starts the exact Saga, and stops before the first workflow action.
- An optional batch preflight path that checks many candidates with one application/Spring startup and emits deterministic per-candidate pass/fail diagnostics; ordinary one-scenario execution does not require a separate preflight command because it already uses the same setup path.
- Clear separation between static candidacy, successful materialization/setup, and later execution outcomes.
- Generic helper tracing for caller arguments, local DTO construction, setter/property mutation, and the mutated value flowing into a facade/Saga call.
- Dummyapp-first regression coverage followed by a bounded Quizzes Docker accuracy run and refreshed materializability metrics.

### Non-goals

- Making all 587 accepted Quizzes inputs materializable in this package.
- Proving that every materializable input will complete successfully under every runtime state.
- A new batch all-zero qualification system or persistent replayability registry.
- Requiring a separate preflight run before every normal or faulty execution.
- Quizzes-specific default values or source-pattern exceptions.
- Event payload reconstruction and every remaining blocker family.
- External/ad hoc input tuple overlays.
- Running every all-zero or faulty scenario to qualify an input.
- TCC, stream, gRPC, distributed, or concurrent replay.

### Constraints

- Preflight must not execute forward actions, inject faults, compensate, or commit.
- Preflight must use the same argument materialization and Saga startup path as normal execution; duplicated approximations are not proof.
- Batch preflight must start the application context once, not once per candidate.
- Materializability must not grow into domain validation, fixture reconstruction, or a prediction of successful execution.
- Preserve deterministic IDs, stable ordering, the five-file package bytes during checking, and existing verifier pipeline boundaries.

### Acceptance Proof

- Representative outcome: Run the current Quizzes candidate set through one-context batch preflight; known setup-valid and constructor-invalid examples receive the expected results, and helper-built user/course DTOs retain discriminating source values.
- Focused regression: Dummyapp Spock coverage proves exact constructor/type checking, shared normal/preflight setup behavior, no workflow action during preflight, caller-to-helper parameter substitution, and setter/property mutation capture.
- Material-risk evidence: Verify one application startup for the batch, record total and per-candidate setup time, prove package checksums remain unchanged, and run one existing all-zero and one existing faulty scenario to confirm ordinary execution still reports runtime outcomes independently.
- Boundary/broad/real-environment checks: Run targeted verifier tests, then the relevant verifier suite if change radius requires it, and a Docker Quizzes smoke matching the user-observed environment.

## Context and Route

### Current Behavior

- The latest Quizzes package contains 587 accepted inputs and marks 94 materializable across five Saga types.
- Twenty representative all-zero attempts produced four successes, fourteen null-name runtime failures, and two Saga startup constructor failures.
- The current static evaluator checks recipe shape/readiness flags, while `ScenarioMaterializer` and `ScenarioExecutor` separately perform real reflection and Saga construction.
- Current serial execution attempts each took roughly 29–39 seconds because every attempt paid application startup and, for failures, runtime retry costs.
- The dominant malformed positive recipes capture `new CourseExecutionDto()` or `new UserDto()` but omit setter mutations present in `QuizzesSpockTest` helpers.

### Key Invariants

- A materializable/setup result and normal executor setup cannot disagree because they use the same implementation.
- Materializable means the exact persisted tuple can construct and start the exact Saga; it does not predict domain or fault outcomes.
- The persisted tuple, not an out-of-band replacement, is the replay input.
- Normal and faulty executions remain on-demand executor operations with their own reports.
- Failure diagnostics identify the participant, input, stage, and concrete reason.

### Route Rationale

Guided is the smallest safe route because this changes a cross-cutting verifier/executor meaning, has two dependent but independently demonstrable outcomes, needs durable intent, real Quizzes evidence, and one fresh independent review. It has no migration, security, live-write, or external-service boundary requiring Governed handling.

### Material Risks

- Invoking a supported Saga constructor must remain setup-only; an unexpected constructor side effect would require re-triage.
- Batch preflight must not introduce application-state contamination or silently perform workflow actions.
- The static generator cannot claim a runtime preflight occurred unless that check actually ran; reporting must preserve this distinction without adding a runtime registry.
- Helper tracing can expand into general Groovy interpretation; implementation must stop at the concrete source patterns needed for the approved outcome.

### Canonical Docs

- `docs/verifiers-impl/glossary.md` — define materializability as exact persisted-tuple and Saga setup capability, separate from execution outcomes.
- `docs/verifiers-impl/current-state.md` — record implemented scope and limitations.
- `docs/verifiers-impl/reference/scenario-executor.md` — document preflight/batch operation and guarantees.
- `docs/verifiers-impl/evidence.md` — record final commands, metrics, timing, and Quizzes artifacts.

## Delivery

| Outcome | Status | Proof |
|---|---|---|
| Fast, shared executor setup preflight | complete | One-context Quizzes batch checked 82 candidates in 49,069,530 ns after startup: 80 setup-ready and two precise constructor-type failures; no workflow actions ran. |
| Faithful helper-built input recipes | complete | Dummyapp preserves point-in-time ordered caller-derived mutations; Quizzes has assignments on 75/75 user and 50/50 course helper candidates, with unsupported end-date calls conservatively blocked. |

## Decisions and Scope Deltas

| ID | Decision | Status | Reason |
|---|---|---|---|
| D1 | Coordinate the change through SP. | approved | User explicitly requested the SP workflow. |
| D2 | Use one Guided package with two staged outcomes rather than separate issue packages. | approved | The user rejected a separate batch runtime-qualification outcome as unnecessary; preflight and helper fidelity share one focused materializability goal. |
| D3 | Batch preflight in one application context; do not pay one JVM/Spring startup per candidate. | approved | The user approved the fast setup approach; repeated application startup caused the observed 29–39 second attempt cost. |
| D4 | Define materializable as exact persisted-tuple materialization plus Saga startup, not successful all-zero or faulty execution. | approved | The user clarified that actual execution should report its own outcome on demand rather than becoming a second qualification system. |
| D5 | Keep batch preflight optional; a requested scenario can simply execute and report setup or runtime failure through the same setup path. | approved | Preflight exists to measure/filter many candidates cheaply, not to add ceremony to ordinary execution. |

## Acceptance Evidence

- Representative Quizzes package: `verifiers/target/outcome2-helper-tracing/quizzes-20260727-180306-391/` — 732 accepted inputs, 82 manifest-declared setup candidates, 650 statically blocked inputs, 164 eager FaultScenarios.
- Setup preflight: `verifiers/target/outcome2-helper-tracing/setup-preflight-report.json` — one Spring context; 80 `SETUP_READY`; two `STARTUP_FAILED` (`BigInteger` persisted values versus `Integer` Saga constructors); zero forward/fault/compensation/commit actions; report outside the package.
- Helper fidelity: dummyapp asserts caller values `701`, `source-helper-name`, and `809` in ordered setter/property assignments and stable repeated ids/fingerprints. Refreshed Quizzes user helpers retain `name`, `username`, and `role`; course helpers retain five mutations and expose the unsupported `DateHandler.toISOString` call rather than an empty DTO.
- Focused post-review regression: 177 tests passed across executor, wrapper, readiness, visitor, dummyapp, recipe mapper, export, and accounting specs.
- Broad post-review regression: complete verifier suite passed — 573 tests, zero failures/errors/skips. An initial pre-review run exposed two stale dummyapp fixture-count assertions; they were reconciled semantically before the final full-suite pass.
- Targeted runtime boundary: all-zero report `execution-all-zero.json` is `SUCCESS / EXACT`; single-fault report `execution-single-fault.json` is `COMPENSATED / EXACT` with the assigned fault realized. These are execution evidence, not a qualification registry.
- Package integrity: the five semantic package hashes remained unchanged after preflight and both targeted executions.
- Operational notes: the original Outcome 2 child session was truncated when disk space was exhausted; recovery re-inspected the diff and reran every claimed check. One Docker generation attempt at the default 768 MiB limit failed with `OutOfMemoryError`; the approved 3 GiB / `-Xmx2500m` smoke resources passed.

## Completion

Delivered:

- Optional `--preflight` / `PREFLIGHT=true` setup batch that validates the complete static candidate table, starts one application context, and uses the exact normal-executor materialization/Saga startup implementation without workflow actions.
- Purpose-specific setup report with stable candidate order, participant states, precise blockers, and observed setup timing.
- Strict mode validation and preserved Java reflection constructor conversions/overload behavior.
- Generic caller-to-helper substitution and point-in-time setter/property mutation snapshots for helper-built DTOs, including self-rebinding facade flows and conservative ambiguous-control-flow behavior.
- Updated executor reference, glossary, current-state page, and evidence appendix.

Acceptance result:

- Quizzes: 732 accepted inputs; 82 static setup candidates; 80 setup-ready; two exact `BigInteger` versus `Integer` startup failures; setup loop 49,069,530 ns after one Spring startup; no preflight workflow actions.
- Helper fidelity: 75/75 user and 50/50 course helper candidates retain ordered source mutations; post-facade mutations are excluded and separate helper calls remain isolated.
- Tests: 177 focused and 573 full verifier tests passed with zero failures/errors/skips.
- Targeted execution remains independent: all-zero `SUCCESS / EXACT`; assigned-fault `COMPENSATED / EXACT`.
- Package: all semantic hashes unchanged after preflight/execution.
- Independent review: `PASS`; `MAT-REV-001` through `MAT-REV-004` resolved in `review.md`.

Known limitations:

- Existing v3 manifest `materializable=true` remains a deterministic static setup-candidate field; actual setup truth is `SETUP_READY` from preflight or normal execution.
- `DateHandler.toISOString(endDate)` in course helpers is now preserved but remains an unsupported local call, so those inputs are honestly blocked instead of emitted as empty DTO false positives.
- Two Quizzes integer inputs remain startup failures because persisted values materialize as `BigInteger` while Saga constructors require `Integer`.
- No all-zero qualification batch, runtime registry, fixture reconstruction, or domain-success prediction was added.

Git/external state:

- The completed package was committed locally on `fault-analysis/scenarios` after explicit user authorization.
- No push, PR, merge, deployment, or live/private system mutation was performed.
