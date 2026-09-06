# Preflight failure reporting and bounded preparation qualification

State: complete and ready for adoption review. No commit, push, merge or primary-checkout
integration was performed.

## Outcome

The parent preflight process now reads a worker report before judging its exit. It accepts
only the worker CLI's `0/SUCCESS` and `1/SETUP_FAILED` pairs. A failure is retained only
when schema, manifest/runtime metadata, workload and participant identity, ordered setup
actions and bindings, participant states, blocker ownership, and failure stage agree with
the selected package and the executor's production contract. Missing output, malformed or
mismatched evidence, unsupported exits, contradictory success/failure, timeout, crash and
cleanup failure still become `FRESH_STATE_ISOLATION_FAILED`.

The implementation does not change the report schema, source setup authorization, input
recipes, ordinary execution exception classification or impact behavior.

## Isolated baseline and incremental source diff

Worktree: `/Users/andre/meic/thesis/microservices-simulator-b-preflight`, branch
`codex/preflight-failure-reporting`, base
`36784346d1f5e988ad612132b6ed9f5db344ee78`. The primary checkout's tracked dirty patch
was copied before editing and saved at `/tmp/preflight-b-inherited-tracked.patch`
(SHA-256 `a7df2703628c96626723ebde176bf4840c5ee450785341d8643b819df34f70e6`).
The sorted inherited untracked-path list hashed to
`984d0487c5cfcfe87e93bcb783c6c5c053a490491d5ea9a13a92373488cfe689`.

Relative to that inherited baseline, source changes are limited to:

- `ScenarioSetupPreflightProcessOrchestrator.java`: exit/report ordering and strict
  successful/failed evidence validation.
- `ScenarioExecutor.java`: package-derived expected participant, setup-action and binding
  identities carried in the internal preflight plan.
- `ScenarioSetupPreflightProcessOrchestratorSpec.groovy`: focused positive and negative
  worker-contract coverage.

The exact incremental source patch is
`verifiers/target/preflight-failure-reporting/incremental-source.patch` (803 lines, SHA-256
`660ca8eb655821e0a82335afdd18ac199a177d7ebd90476563f5719955e1271a`).

Canonical documentation changes are this handoff plus narrow updates to `current-state.md`
and `roadmap.md`. `BRIEF.md` is the parent's inherited assignment/approval pointer.

## Selected preparation evidence

Input package is an exact byte copy of
`verifiers/target/astra-nested-bindings/final-generated/quizzes-20260905-125205-380/`.
All nine SHA-256 values match the original; the manifest hash is
`ce47fdcd6f84cd04a27e9ad4a72476c0fb331410a0f4a57ba953e70590b7a2f0`.

| Mechanism | Persisted identity | Fresh result and boundary |
| --- | --- | --- |
| LeaveTournament source prefix | workload `736592be74b4c37cbecd6fa6070e54da4c9b30f2c75cd22ffc529b784c886ba2`, input `1e35306a54484147150382d8503a9c06a94a46b8e6a2e065a64d24851262c6b7` | `SETUP_READY`; 18/18 actions; Tournament 12 and user 13 bound from retained results; three events cleared; empty baseline; no LeaveTournament setup action |
| Target exclusion in a negative-test context | workload `b8e7a047300ae282c12f566585790de152eb348c3e6c6792eaec4bc459e20b7a`, input `5173febd3a7e2e076d04f7330736379fa680c5106e1f5abf45f648f710b21853` | `SETUP_READY`; 14/14 actions; Tournament 12, execution 2 and user 4 bound from retained results; three events cleared; empty baseline; no AddParticipant setup action |
| Shared retained identity | earlier qualified RemoveTournament/AddParticipant workload `12f7f358f8c3c04a6541a8a86450639089e1922c355206871fc2df6047c68417` | reused prior Docker evidence: 12/12 actions, four bindings, two participants and one retained Tournament result; no redundant rerun |
| Expected valid worker failure | focused worker-contract fixture | exit 1 plus `SETUP_FAILED` preserves `SETUP_METHOD_NOT_AUTHORIZED`; invented reason, wrong stage progress, wrong identity, malformed arrays and inconsistent exits are rejected |

The two current-package persisted fault-free scenarios were run separately after preflight:
`7b5b046bcc056765fb1d72e8ccf22daed41230dfd1364041274ebfce280c6197`
(LeaveTournament) and
`c1bb7be178a92593dd63f23c08d65c547f5205725efbdb1275ee6a069f2e1ec5`
(AddParticipant) both returned `SUCCESS / EXACT`. The second input comes from a source test
whose broader purpose is a negative RemoveTournament assertion; its extracted AddParticipant
target itself succeeds. These execution results are separate from preparation readiness.

Evidence is under `verifiers/target/preflight-failure-reporting/`, especially
`selected-preflight-final.json`, `selected-preflight-final.log`, the two `*-execution.json`
reports and logs, selection files, qualification-only drivers, and `latest-package/`.

## Verification and review

- Focused parent/executor suites after the final review fix: 169 passed (`30 + 139`).
- Full verifier suite on the final code: 749 passed, zero failures/errors/skips.
- Fresh Docker/H2 preflight after the ordered identity and phase validation tightening: two
  selected workloads passed. The later one-line failure-only binding-state check is covered
  by the focused negative test.
- Fresh separate Docker/H2 executions: two persisted fault-free scenarios returned
  `SUCCESS / EXACT`.
- Three independent review passes found four initial gaps, two follow-up gaps and one final
  impossible binding-prefix acceptance. All findings were fixed. `REVIEW.md` records the
  review result and the final focused self-verification after the three-pass review cap.

Known limit: this is selected-case evidence. It does not claim runtime execution of all
665 current candidates, and it adds no impact interpretation.
