# Selected event delivery with no eligible receiver — plan

## Environment and execution mode

Documented route, one Sol medium implementer and Astra low/medium review. Work in the
current `fault-analysis/scenarios` checkout; implementation was explicitly approved on
7 September 2026;
no worktree, merge, PR, or push. Finish with a local linear commit. No intermediate
user-verdict pause is needed unless discovery changes the agreed behavior.

Preserve `note-04-09-2026.md` and
`docs/verifiers-impl/reunioes/2026-09-08.md`. Use isolated JDK 21 build outputs/cache;
never clean the primary `verifiers/target`, which holds historical evidence. Docker
qualification must not interfere with existing user containers.

## Implementation strategy

Introduce an explicit empty-selection completion in the replay scope. Record it only
after the real eligibility enumeration finishes successfully. Teach the executor to
report `NO_ELIGIBLE_SUBSCRIBER` with event/route evidence and continue. Keep actual
delivery status and evidence distinct, preserving ImpactV2's current delivered-event
candidate selection. Update status-dependent reporting and verification consistently.

Use one coherent implementation slice, then targeted qualification. No generator changes
or broad event audit are necessary. Retained experiment validators describe historical
policies; update reusable affected tooling without rewriting old evidence.

## M0 — Explicit empty selection with preserved assessment

### Outcome and spec coverage

FR-1–FR-7 work together: an empty route does not stop later actions, creates no delivery
finding, and cannot conceal an actual control or application failure.

### Change boundary and known anchors

- Simulator replay: `EventApplicationService`, `EventReplayCoordinator` and their tests.
- Verifier execution/reporting: `ScenarioExecutor`, `ScenarioExecutionReport`, affected
  status consumers, and generic Spock coverage using dummyapp/established fixtures.
- Assessment regression: `ImpactV2Assessor` and collector integration. Change assessment
  production only if needed to preserve the existing contract; do not broaden the score.

### Discovery / preflight

Trace all assumptions that a selected action dispatches exactly once, including
`verifyCompleted`, body status, failure propagation, and serialized status validation.
Determine whether report versioning is needed for the additive status and document the
decision. Check the final checkout and concurrent work before editing.

### Proof before continuing

- Zero eligible receivers: explicit result, no consumer invocation, no delivery record,
  later route/forward/recovery actions run, replay scope closes correctly.
- One eligible receiver: existing delivery behavior and evidence remain unchanged.
- Earlier route removes or acknowledges the next receiver: next action reports empty
  selection and a subsequent independent route still runs.
- Selection throws, handler throws, multiple receivers, missing event, recursive or
  incomplete scope: existing failure behavior remains visible.
- Empty selection alone adds no event finding; a separate deleted dependency or failed
  operation residual remains positive in the same completed execution.
- Delivered-but-unresolved positive remains positive; resolved/deleted receiver controls
  remain negative; missing exact delivery observation remains unknown, not zero.
- Observer on/off preserves execution outcomes, including empty selection.

## M1 — Quizzes proof and documentation

### Outcome and spec coverage

FR-8: demonstrate the revised policy on real application execution, with explicit
coverage and comparisons against preserved reports.

### Change boundary and strategy

Use a frozen current build and fresh Docker/JVM/H2 attempts. Start with the corrected
w01 control from `space-map-setup-qualification-2026-09-07`; compare its now-reachable
later actions with its historical stop. Add one retained multi-route case where a real
delivery precedes an absent receiver, selected by structure before inspecting new scores.
Include a known unresolved-delivery positive and a healthy receiver control. Use existing
prepared runners and source-backed fixtures for the continuation cases. The retained
UpdateQuestion positive has no source-backed executable input in the selected catalogue;
reuse its existing, unchanged qualification-only provider fixture and package, explicitly
labelled as such. This validates the established metric rather than claiming new input
coverage. No new provider, ad hoc receiver creation, or package edits are introduced.

Write compact qualification evidence and a short handoff. Update
`docs/verifiers-impl/current-state.md` (glossary, replay, assessment coverage) and
`docs/verifiers-impl/roadmap.md` (completed change and remaining limits). Preserve the
meeting note and historical result tables. Link the new policy/results where relevant.

### Proof before completion

Retain source/build/package identities and exact report paths. Report actual attempts,
empty selections, delivered routes, terminal outcomes, score/coverage, and any remaining
failures. Do not claim all forty historical invalids are repaired or have zero impact.
Review the final implementation, test evidence and documentation before committing.

## Validation strategy

Run focused simulator replay tests and verifier executor/assessment tests in isolated
outputs. Broaden only for a demonstrated cross-cutting concern. Existing fixture-inventory
failures omitting `ReadResponseFixture` are unrelated; identify rather than absorb them.
Run the M1 small Docker comparison after focused checks pass. If runtime infrastructure
blocks it, report the blocker and retain honest partial proof instead of substituting a
static-only completion claim.

## Risks and fallbacks

- A bare return would trip `verifyCompleted` or conceal a broken delegate: require an
  explicit, mutually exclusive empty outcome.
- Reusing delivered status creates false missing-evidence gaps: preserve distinct status
  and prove both empty-selection and missing-observation cases.
- The absence may follow legitimate acknowledgement or deletion: report the observation,
  not an inferred business cause; other metric categories remain independent.
- Continuing exposes previously unreachable actions and can reveal other failures. Keep
  those results and diagnose them; do not extend scope merely to make the batch pass.
- The current score is not a global backlog detector. No automatic retry or final scan
  of every emitted event is added under this package.
