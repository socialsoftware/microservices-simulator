# M3 handoff — review and report the gain

State: `complete` after independent final review.

## Outcome and contract coverage

The canonical verifier handbook and roadmap now describe workload-driven source setup
as shipped behavior. They separate three evidence claims:

1. **Setup attachment coverage:** the bounded stable-order writer emitted 1,415
   WorkloadPlans, of which 408 reference source-derived setup (94 singles, 182 pairs,
   132 triples). All 408 have complete source-context coverage: every binding targets an
   exact selected input, and selected arguments without bindings are independently
   materializable. All 94 referenced source SetupPlans validate as materializable.
2. **Runtime preflight coverage:** only two representative workloads were projected into
   the Docker preflight package. The natural triple and the existing Remove/Add pair both
   reached `SETUP_READY`; five participants, 16 setup actions, and nine bindings
   succeeded in total.
3. **Unchanged interaction/input-space metrics:** the full-cap size-1–3 count-only run
   remains 794/90 accepted/rejected inputs, 150/644 materializable/blocked inputs, 785
   direct interactions (0/535/250 exact/symbolic/type-only), 382/3,594 strict connected
   sets, 35/42 strict sets with accepted positive tuples, and
   74,273/1,247,308,000 selected/all input-bound totals.

This closes FR-1–FR-13 without claiming that all 408 setup-bearing workloads are runtime
ready. Attachment is arity-independent, requires complete selected-input source-context
coverage, and does not combine unrelated tests. Current `setups.jsonl`, WorkloadPlan
`setup`, and accounting shapes remain unchanged.

## Actual files changed in M3

- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- `issues/2026-09-03-workload-driven-source-setup/HANDOFF-M1.md` (corrected stale
  review-proof wording)
- this handoff

No production or test code changed in M3. Untracked `lib/` and `tmp/` remain untouched.

## Final composition inspected

- M0 (`955d38cef`) pinned the pair-shaped baseline and the larger-workload partial-setup
  defect in the existing realistic fixture.
- M1 (`6652161c3`) replaced pair-keyed applicability with one coherent setup candidate
  carrying a stable input-id set, required unique complete workload coverage, projected
  bindings to exact selected inputs, deduplicated actions by source occurrence, and
  added the natural single/triple and cross-test negative proofs.
- M2 (`26c95021c`) qualified the bounded Quizzes package and repaired the test-only
  benchmark runner's stale artifact-alias check against the current manifest.
- M3 changes canonical reporting, this evidence handoff, and one stale review-status
  sentence in the M1 handoff; it does not change implementation or test behavior.

The final diff contains no persisted-schema, accounting, CLI, scheduling, fault,
recovery, dynamic-evidence, or production Quizzes change. The implementation remains
generic verifier logic; no Quizzes Saga name or dispatch method entered production
code.

## Discovery and plan-versus-reality

- Autonomous M1 detail: setup discovery had to start from observed class-scoped
  `setup()` contexts rather than pair evidence so singles and natural larger workloads
  could use the same mechanism. Direct recipe source references also had to count as
  bindings, and unbound arguments delegate readiness to the executor evaluator.
- Autonomous M2 detail: the current-only package migration had left a stale test runner
  accessor. Its independent correction now rejects benchmark result aliases to every
  manifest-declared artifact.
- Planned fallback used: the full 1,000-input writer attempt was stopped after 8m42s
  before artifact publication while scanning the 1,247,308,000 all-input Cartesian
  space. The bounded writer therefore used ten variants per Saga and exhausted naturally
  at 1,415 workloads below its 50,000 cap. The full-cap count-only run remains the
  accounting authority. This limits the breadth of attachment evidence but does not
  weaken the approved mechanism or invariants.
- The M0-named UpdateStudentName triple fell outside stable first-ten input ordering and
  was not reserved. M2 used another natural triple found in ordinary stable output,
  honoring the no-reordering and no-arity-matrix constraints.
- Out of scope: optimizing the large Cartesian writer scan and broadening the executor's
  closed application dispatch map.
- No material scope delta was found.

## Proof

- Focused verifier command covering adapter, generator, model/validator, current
  writer-reader, executor preflight, isolated preflight orchestration, and realistic
  Quizzes analysis — pass, 294 tests, zero failures/errors/skips.
- `mvn -q -DskipTests compile` from `verifiers/` — pass.
- `mvn -q test-compile` from `verifiers/` — pass.
- Full `mvn -q test` from `verifiers/` — pass, 684 tests, zero
  failures/errors/skips. The separately corrected Dummyapp baseline is green.
- `mvn -q -Ptest-sagas -Dtest=QuizzesRemoveAddBenchmarkRunnerSpec test` from
  `applications/quizzes/` — pass, 50 tests.
- M2 Docker count-only generation, bounded writer generation, complete-coverage join,
  and two-candidate isolated preflight — pass; exact commands, hashes, timing, and paths
  are in `HANDOFF-M2.md`.
- `git diff --check` — pass before final review.
- Independent final review: pass with no blocking findings after inspection of the
  complete composition, recorded artifacts and hashes, test reports, compilation, and
  `git diff --check`.

## What the user should try

Compare the two workload records and their setup records in
`verifiers/target/m2-preflight/package-pair-triple/`, then inspect
`verifiers/target/m2-preflight/pair-triple-preflight-report.json`. The pair should show
12 actions and four bindings; the natural triple should show four actions and five exact
bindings; both should be `SETUP_READY` with all five participants materialized and
started.

## Reasonable veto points

- Treating one class-scoped Spock `setup()` as the coherent observed context. A narrower
  feature-scoped interpretation needs new source evidence and is a separate design.
- Using the reduced-input bounded writer for attachment counts while retaining the
  full-cap count-only run as the selection/accounting authority.
- Reporting runtime readiness only for the two preflighted representatives, not for all
  408 setup-bearing workloads.
