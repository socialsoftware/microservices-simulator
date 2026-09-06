# ImpactV2 execution handoff

State: complete. M0 and the integrated M1/M2 review both passed.

## What is available

Ordinary supported Saga/local ScenarioExecutor attempts now write an automatic sibling
`*.impact-v2.json` assessment. It counts distinct aggregate identities with one or more
of three observed conditions: a dependency on a deleted target, a failed-operation
persistent residual, or an unchanged receiver still eligible after its exact event was
delivered. Each count has reasons and evidence. Complete zeros, partial lower bounds,
invalid execution and unavailable collection are distinct. ImpactV1 is unchanged.

The implementation uses one shared observer and three deterministic checks. It adds no
Quizzes-specific rule, domain-harm oracle, severity weights, extra event drains or future
business operations. Committed data is reloaded from the exact persisted revision after
commit, so rolled-back writes and database timestamp precision do not create false facts.

## Qualification and verification

- Full simulator suite: 134 tests, 0 failures/errors/skips.
- Full verifier suite: 763 tests, 0 failures/errors/skips.
- Eight persisted Quizzes cases: COMPLETE with zero coverage gaps; scores
  `2, 0, 1, 1, 0, 0, 1, 0` in the order shown in `M2-HANDOFF.md`.
- Three additional observer-disabled attempts: unavailable/null assessments, with matching
  execution outcomes, actions, inputs and selected independent state witnesses.
- Docker qualification exited 0; its validator passed inside the container and again
  from the host. Raw reports and provenance are in
  `verifiers/target/impact-v2-qualification/run-03/`.
- M0 independent review passed in pass 2; integrated M1/M2 review passed with no remaining
  actionable blocker. `REVIEW.md` records the findings, fixes and verdict.
- Verifier documentation build and `git diff --check`: passed.

The unexpected ordinary-compensation score is supported by actual data: restoring a
Tournament loses its embedded topics' course IDs because `TournamentTopic.buildDto()`
omits them. Its application mutation time changes too. The earlier narrow experiment
checked fewer fields and missed the course-ID loss. No application repair was made.
The no-op variant also counts one Tournament, but leaves more data changed: this scalar
counts objects, not severity. Application-owned timestamps remain part of the approved
comparison; no field was silently excluded to obtain a zero.

## Worktree and preservation

Execution is isolated in `/Users/andre/meic/thesis/microservices-simulator-impact-v2`,
branch `codex/potential-impact-v2`, based on
`36784346d1f5e988ad612132b6ed9f5db344ee78`. The primary checkout remains unchanged;
all 77 pre-existing changed/untracked files match their saved hashes. The implementation
also preserves their inherited changes in this worktree.

The reviewed B preflight-reporting repair was adopted as an incremental patch, not a
merge of its dirty worktree. Original patch SHA-256:
`660ca8eb655821e0a82335afdd18ac199a177d7ebd90476563f5719955e1271a`.
Baseline hashes, the inherited patch and B's normalized patch are under
`verifiers/target/impact-v2-base/`. The implementation review baseline includes that B
repair, so the ImpactV2 diff does not confuse it with newly implemented scoring.

A reviewable patch against the unchanged primary checkout is saved as
`verifiers/target/impact-v2-base/complete-session.patch`, with a file/hash inventory beside
it. It passed `git apply --check` against that checkout and has not been applied.

Actual changed surfaces: simulator observation and Saga/event hooks, verifier assessment
and ordinary-attempt integration, focused framework/verifier tests, the bounded
qualification harness, canonical current-state/roadmap docs and this issue package.
The milestone handoffs own the exact inventories and discoveries. No application main
source changed. No commit, push, merge, production deployment or service restart occurred.

## What to inspect and what remains

Start with `M2-HANDOFF.md` for the domain story and case matrix, then open a scored
`*.execution.impact-v2.json` under run-03 to see the object identities and raw facts.
`verifiers/experiments/impact-v2/README.md` gives the complete reproduction command.
Canonical behavior and terminology are in `docs/verifiers-impl/current-state.md`.

This finishes the approved first slice, not the all-case evaluation campaign. Next:
freeze a broader executable sample with matching unassigned controls, measure coverage,
and refresh the historical 34-case landscape. The old 19/15 split has not been reassessed.
The discovered Quizzes DTO omission and the event consumer's missing save remain separate
bounded application fixes. Whether to distinguish potentially benign application-owned
metadata is a methodological discussion; changing that policy is not hidden in this work.
