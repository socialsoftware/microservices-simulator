# Harness Retrospective — 2026-07-11

Summary from a `/grill-me` session on the `quizzes-full` implementation, done before applying
the same agent harness (`.claude/skills/{boot-strap,classify-and-plan,implement-aggregate,
review-aggregate,review-tests,review-artifacts}`) to a new, unfamiliar domain.

## Success bar

Success = the harness can take a domain spec to a working, tested implementation without the
user having to manually patch docs/skills for reasons unrelated to genuine domain difficulty.
Structural equivalence to `quizzes/` is a proxy, not the goal.

## What's confirmed solid

- All 8 aggregates complete, 288 tests passing, `plan.md` fully checked off.
- The one late-breaking miss (missing `SolveQuiz` saga surfaced at aggregate 8, session 2.8.d)
  was genuine domain difficulty, not a harness defect — and the resulting doc gaps (nullable-anchor
  subscriptions, ByEvent deletion semantics) were folded back into `session-d.md` (see
  "Conditional subscriptions (nullable anchor IDs)" and the shared-anchor events section).
- The review-to-fix loop for test-suite boilerplate (`docs/reviews/review-2026-05-26.md` →
  `InterInvariantTestBase.groovy`) actually landed and is in use by the final aggregate
  (`TournamentInterInvariantTest extends InterInvariantTestBase`), not just logged as resolved.
- The profile-agnostic service-layer boundary from `AGENTS.md` (`*Service` classes never import
  concrete `Sagas*` classes) verifiably held across all 8 aggregates — checked directly via grep,
  not assumed.

## Open items carried into the next application

1. **Must-fix — skill path robustness.** A stray tracked file,
   `applications/quizzes-full/applications/quizzes-full/reviews/test-review-QuizAnswer.md`,
   shows a skill was once invoked with cwd already inside `applications/quizzes-full/` and
   silently wrote to a wrong nested relative path instead of erroring. Audit skills for
   cwd-dependent relative paths and either make them cwd-independent or add a guard, before
   running them across a new app's many sessions.

2. **Must-build — spec authoring skill.** No skill exists for authoring
   `{App}-domain-model.md` / `{App}-aggregate-grouping.md` from scratch. `quizzes-full`'s copies
   were adapted from the reference `quizzes/` app's already-validated docs, not independently
   authored. Every phase validated so far (Phase 0-3) assumes these two files already exist and
   are correct — this is the highest-risk, least-tested part of the pipeline. A new extraction
   skill is needed, scoped strictly to reading TrainTicket's domain concepts (entities, business
   rules, service boundaries) and re-deriving `domain-model.md` / `aggregate-grouping.md` for
   this simulator's semantics — not preserving TrainTicket's actual service topology, APIs, or
   transaction boundaries, since the simulator models logical consistency patterns
   (`Aggregate`, `Workflow`, `UnitOfWork`, `CommandGateway`), not a network-level microservices
   runtime.

3. **Unverified — final artifact review.** `review-artifacts` was not confirmed re-run after the
   final aggregate (Tournament) finished. The 05-26 review's fixes are validated against 7/8
   aggregates of evidence, not 8/8. Cheap to close out before considering `quizzes-full` fully
   done.

## Next application

TrainTicket (https://github.com/FudanSELab/train-ticket) — a subset of its microservices, used
as an unfamiliar domain never exercised against this harness, specifically to stress-test the
gap identified above (spec authoring without a reference implementation to check against).
