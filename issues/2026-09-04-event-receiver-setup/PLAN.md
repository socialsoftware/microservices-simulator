# Plan: event receiver setup and coverage diagnosis

## Environment and execution mode

Documented route, reviewed implementation, no further user checkpoint under the existing
autonomous instruction. Prior uncommitted work is the starting state, recorded under
`verifiers/target/astra-event-receiver/starting-state.json`. Source/test changes are
integrated as reviewed patches; no commits, pushes, or merges. Sol uses an isolated
snapshot for application support; Astra owns export correction, integration and Docker.
Luna diagnoses coverage read-only. Canonical documentation has one writer, Astra.

## Implementation strategy

Use the existing eight-action QuizAnswerEventHandlingTest setup. Bind exported event
references to the authoritative persisted Saga routes rather than the selected workload's
local event list. Add only the needed typed Quizzes facade registrations. Generate normal
packages and select persisted scenarios through the current reader; do not hand-edit them.

## M0 — Pin evidence and boundaries

Confirm exact receiver identity (same course execution/student), existing source fixture,
current 577/219 accounting, missing method signatures and the route projection defect.
Preserve starting dirty state. No full replay/GA or source-context merging.

## M1 — Correct route references and enable bounded setup

Astra changes the executable export projection and generic regression coverage. Sol adds
createQuiz/startQuiz/addParticipant registrations with focused application-side tests.
Review exact target/source identity, argument order/types, missing/ambiguous-route failure,
unknown-method rejection and prior behavior. Keep production application services unchanged.

## M2 — End-to-end qualification

Generate a current ordinary package; verify hashes and exact route round trip. In Docker,
run the existing matching QuizAnswer zero-fault case, an assigned pre-emission fault,
and an absent-receiver route control. Rerun prior StartQuiz/LeaveTournament setup examples
when enabled. Check same student/course identity and pending-event baseline through
application assertions and executor evidence. Run full verifier suite once integrated.

## M3 — Diagnosis and handoff

Reconcile base single-input accounting with event/provider rows, quantify dispatcher-method
coverage, and rank concrete small static fixes with evidence. Implement only bounded
necessary repairs; document any further decision-heavy work. Update current-state,
roadmap, relevant ADR and issue handoff with exact results and limits.

## Validation strategy

Focused Spock export/application tests first, independent diff review, integrated verifier
suite, deterministic ordinary generation and bounded Docker execution. Record exact
commands, selected IDs, reports and observed failures honestly. No broad 98-minute preflight.

## Risks and fallbacks

- A matched route is not a matching receiver: prove the latter in the real application.
- Local route numbering can misdirect replay: use package route authority and reject
  ambiguous matches; test subsetting/permutation.
- Setup method availability is not domain success: qualify representative new methods.
- Static candidate counts can hide missing inputs: retain unresolved dependencies and
  report prospective gains separately from measured results.
