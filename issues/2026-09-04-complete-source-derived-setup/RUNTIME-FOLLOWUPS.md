# Runtime follow-ups exposed by qualification

These are concrete limits discovered during the five-workload Docker sample, not claims
that all statically materializable workloads can run. The first sample is retained at
`verifiers/target/astra-qualification/feature-prefix-preflight.json` and its adjacent log.

## Explicit Quizzes setup method coverage — resolved for these examples

The follow-up `../2026-09-04-event-receiver-setup/` added exact registrations for
`createQuiz`, `startQuiz`, and `addParticipant`. Both previously blocked StartQuiz and
LeaveTournament examples passed fresh Docker preflight with empty post-setup event
baselines. The dispatcher remains closed to unregistered methods. This follow-up is done.

## Nested question references inside caller DTO mutations — resolved

`../2026-09-04-nested-participant-setup-bindings/FINAL-HANDOFF.md` closes the three
CreateQuiz cases and 85 CreateQuestion cases with the general whole-result fix.
Nested scalar property-result collections are a distinct deferred boundary, owned by
the current roadmap. These whole-DTO cases are no longer blockers.

## Failed-worker diagnostic preservation

`ScenarioSetupPreflightProcessOrchestrator` checks a worker's nonzero exit status before
reading its report. A legitimate setup failure therefore appears in the combined report
as `FRESH_STATE_ISOLATION_FAILED`, while the worker log contains the useful reason
(`SETUP_METHOD_NOT_AUTHORIZED` in the two examples above).

This is distinct from the fixed successful state-only/empty-binding false negative.
A future direct repair should retain a well-formed, metadata-matched failure report for
the expected failure exit status, while still rejecting malformed reports, conflicting
success/status combinations, missing output, timeouts, and process crashes. The current
qualification cites worker logs when the combined report loses the detailed cause.

## Event receiver prerequisites remain separate

The existing AnonymizeStudent/GetCourseExecutionById/RemoveStudentFromCourseExecution
triple creates CourseExecution and User state. An event route to QuizAnswer or Tournament
also needs exactly one matching receiver for those same identities. Feature-local input
preparation does not synthesize that additional state or unify independent tests.

The event-receiver follow-up qualifies one existing ordinary QuizAnswer fixture with
successful delivery, pre-emission fault masking, and an absent Tournament receiver control.
It also repairs workload-local route projection; old triple exports must be regenerated
before making claims about the exact intended consumer. Generic cross-test preparation
remains outside both issues.
