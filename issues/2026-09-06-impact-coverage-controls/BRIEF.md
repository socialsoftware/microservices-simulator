# Complete observation coverage and repair qualification prerequisites

The user approved steps 1 and 2 from the conversation: fix the nested participant/answer
observation cycle, repair experiment prerequisites, and refresh broader control/fault
qualification. They explicitly deferred scoring-policy decisions (deleted remnants,
application timestamps) and requested concrete domain examples for Tuesday's meeting.
This is a direct approved implementation brief, continuing in the existing
codex/potential-impact-v2 worktree and preserving inherited changes.

## Outcomes and boundaries

1. Extend the generic persistent observer to represent supported owned-entity back-links
without recursively re-reading the same object. Preserve meaningful persistent changes,
stable deterministic identity/path references, collection semantics and explicit unknowns
for unsupported mappings. Fix the TournamentParticipant -> ParticipantAnswer ->
TournamentParticipant shape; do not just suppress a cycle warning or discard fields.
2. Repair the observed UserDto/TopicDto materialization gaps and source/test prerequisites
for three missing QuizAnswer event receivers. Use valid control inputs for duplicate
CourseExecution creation, nonexistent FindQuiz identity and unenrolled SolveQuiz users.
Changes may touch generic verifier mechanisms, dummy fixtures and Quizzes test/setup
support. Do not repair production Quizzes domain bugs or alter the event horizon.
3. Regenerate the current package and execute the corrected 29-schedule benchmark plus
an explicitly selected broader control/late-fault cohort corresponding to the earlier
30 pairs where feasible. Select/freeze before observing outcomes, record every changed
fixture/workload and any exclusion. Do not replace unsuccessful attempts silently.
4. Prepare a plain-language, evidence-linked set of domain examples: user intent, steps,
fault, recovery, final facts, exact objects/reasons scored, control result and limits.
Include concrete pending scoring-policy questions without changing policy.

## Execution and proof

Observer and prerequisites can proceed independently. Use one bounded implementer per
slice and independent review of their integrated changes; root owns experiment selection,
Docker qualification, final composition, canonical docs and meeting examples. Coordinate
Maven runs/dependency installation; do not concurrently overwrite target outputs or
local simulator dependency jars. Use focused positive/negative generic tests and
Quizzes tests for application test support, then appropriate full module checks.

Run actual scenarios via ordinary ScenarioExecutor in fresh Docker/JVM/H2 environments
using frozen compiled source copies, at most two concurrent attempts. Preserve previous
reports. Validate report joins, execution/recovery completion, assessment completeness,
counts and controls; null must remain distinct from zero. Reuse existing harness pieces
and keep the implementation bounded. Report application defects separately.

Do not change ImpactV1/ImpactV2 categories, weights, lifecycle/timestamp equality policy,
search rewards, runtime command semantics, production app behavior, or silently run
extra recovery/events. No commits, merges, pushes or deployments. Canonical current-state
and roadmap, slice handoffs, final review and evidence must agree before completion.
