# Event receiver setup qualification

## What this is

An event-expanded workload needs both its participant inputs and the state required by
its selected receiver. The existing natural triple prepares only CourseExecution/User;
it cannot deliver to an absent QuizAnswer or Tournament. An existing ordinary feature,
`QuizAnswerEventHandlingTest#AnonymizeStudentEvent updates the student name in the quiz answer`,
already describes one complete matching receiver fixture. Its generated eight-action
setup needs exact runtime method registrations. Qualification also exposed an export
bug: a workload-local route index can reference the wrong package-global Saga route.

## Goals

1. Execute that existing source-derived fixture and deliver anonymization to its exact
   matching QuizAnswer receiver through a persisted current package.
2. Preserve the exact selected handler route through export/read/replay.
3. Measure runtime-method coverage and remaining static blockers without treating static
   candidacy as execution proof.

## Non-goals

No cross-test fixture synthesis, arbitrary receiver creation, altered Quizzes production
behavior, broad reflection dispatch, fan-out, new impact metric, or full qualification of
all 577 static candidates. The original missing-receiver triple remains an honest negative.

## Functional requirements

- **FR-1.** Workload event references shall resolve against the authoritative Saga route
  catalogue for the exact triggering Saga/step/emission and selected handler semantics.
  Subsetting or reordering workloads shall not change that reference. Missing or ambiguous
  route matches fail export instead of selecting the first route.
- **FR-2.** Quizzes setup shall explicitly authorize the observed createQuiz and startQuiz
  signatures, preserving typed arguments/results and rejecting unknown methods. The
  similarly bounded addParticipant registration may enable the existing LeaveTournament
  preparation case; it grants no blanket facade authority.
- **FR-3.** Receiver setup shall come from the one existing ordinary test context and keep
  its CourseExecution, User, Question, Quiz and QuizAnswer relationships. No manual package
  editing or prerequisite provider may replace that source-derived proof.
- **FR-4.** Setup-created events shall be cleared before measured actions. Successful
  qualification must report one selected event and exactly one eligible matching receiver.
- **FR-5.** A persisted zero-fault case shall deliver successfully. A pre-emission fault
  shall mask the consequence. A selected route with no eligible receiver shall still fail
  explicitly; neither failure is an evaluated zero-impact success.
- **FR-6.** Generic export tests shall cover multiple routes, subset/permutation stability,
  round-trip exact identity, and invalid/ambiguous matching. Application tests own the
  concrete method registrations and receiver behavior. Docker supplies realistic proof.
- **FR-7.** Diagnosis shall reconcile all 796 accepted base singles (577 candidates and219
  blockers at entry), separate event/provider additions, and distinguish exact measured
  gains from prospective improvements and runtime method coverage.

## Architecture

The analyzer and generator retain their semantics. The executable exporter references
the route catalogue already written by static export. Quizzes owns its closed runtime
method map. Existing ScenarioExecutor event capture, selection, isolation, and reporting
remain authoritative; no new automatic setup composition policy is introduced.

## Data model

Persisted shapes stay unchanged. Corrected workloads may reference a different existing
Saga-local route token; workload/fault model IDs retain their existing authority. Earlier
bad exports must be regenerated, not relabeled. Runtime IDs remain attempt-local.

## Security model

Only explicit typed application-owned method registrations are added. Missing/ambiguous
route identity fails closed. No arbitrary reflection or cross-context value matching.

## Operating

Use fresh Docker Saga/local execution against ordinarily generated packages. Keep prior
artifacts and uncommitted work intact. Rollback is reverting the bounded exporter and
application test-support changes. Update canonical docs with qualified examples and limits.

## Future roadmap

Broader consumer setup inference, nested participant DTO producer binding, remaining source
patterns, and failed-worker diagnostic preservation stay separately diagnosable work.

## Open decisions

None blocking. The user authorized autonomous implementation in the same reviewed manner;
the existing coherent source fixture is selected over speculative cross-test synthesis.
