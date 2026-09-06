# Final handoff: preflight and source-derived setup

Status: the bounded implementation and qualification are complete in the primary working
tree. Changes are uncommitted. The user's autonomous-execution instruction supplied the
implementation authority; no additional approval checkpoint was needed.

## What changed

1. **Direct preflight repair.** Successful state-only setup accepts an explicit empty
   participant-binding array. Missing, null, malformed, unresolved, or inconsistent worker
   evidence remains rejected. Shared report-record normalization was not changed.
2. **Bounded nested result access.** Setup can bind `quiz.aggregateId` through the existing
   property string. The exact getter chain must declare DTO → DTO → Integer-compatible
   types before any dispatch; other paths, wrong signatures, and null intermediate values
   fail explicitly.
3. **Exact feature-local preparation.** New candidates retain ordinary `setup()` plus
   supported facade calls before the exact selected target in the same feature. Void
   effects and source order remain intact. Tuple attachment rejects selected-target
   replay, omitted inter-target effects, ambiguous repeated occurrences, and mixed
   feature contexts. Assertions, control flow, direct workflows, and event handlers close
   further prefix extension. Existing complete fixture-only candidates retain priority.
4. **Correct argument snapshots.** Direct facade calls now retain prior DTO setters and
   property assignments, including self-rebinding. Later changes do not alter that recipe.

The visitor/state → adapter → scenario → executor boundaries remain intact. Internal
occurrence metadata does not change persisted record shapes or the application's closed
setup dispatcher.

## Delegation and review

- Luna Max: direct preflight repair; independent review of Astra's property slice and the
  final accounting-test expectation changes.
- Sol Medium: documented spec/plan, occurrence/prefix implementation, and the DTO snapshot
  repair exposed by Docker.
- Astra: sequencing, property implementation, independent code review, patch integration,
  complete suite, deterministic generation, Docker qualification, and canonical docs.

`REVIEW.md` records material findings and their resolution. `M0-HANDOFF.md`,
`M1-HANDOFF.md`, `M2-HANDOFF.md`, and `M2-REPAIR-HANDOFF.md` preserve milestone-specific
facts. `SPEC.md` and `PLAN.md` own the implemented boundaries.

## Final static result

All runs use ordinary Quizzes generation, all 796 accepted inputs, maximum Saga set size
one, maximum 1,000 input variants per Saga, maximum 5,000 catalog workloads, recovery
schedule cap one, and dynamic enrichment disabled.

| Code state | With source setup | Needs no setup | Blocked | Static candidates |
|---|---:|---:|---:|---:|
| Baseline | 559 | 1 | 236 | 560 |
| Nested-property slice | 562 | 1 | 233 | 563 |
| Prefix slice before DTO repair | 579 | 1 | 216 | 580 |
| **Final, with correct DTO snapshots** | **576** | **1** | **219** | **577** |

The change adds three nested-property and 17 feature-prefix inputs. Correct DTO snapshots
also remove three old CreateQuiz false positives: their now-visible `questionDtos`
collections contain unresolved producer references. Net improvement is 17 candidates;
the intermediate 580 count is not the final result. The removed examples are in setup
methods of StartQuizTest, StartQuizCompensationTest, and QuizAnswerEventHandlingTest.

Final package:
`verifiers/target/astra-repaired-singles/quizzes-20260904-220420-118/`.
Repeated package:
`verifiers/target/astra-repaired-repeat-singles/quizzes-20260904-220530-173/`.
All nine files are byte-identical, including the manifest, and their hashes remained
unchanged after runtime qualification. The 944 written workload records also include
normal event expansion/provider cases; that number is not a runtime-ready count.

## Validation

- Baseline complete verifier suite: 743 tests passed.
- Final complete verifier suite: **774 tests, zero failures/errors/skips** using
  `mvn -q test` from `verifiers/`.
- Final log: `verifiers/target/astra-final-repaired-tests.log`.
- The first combined run exposed expanded dummy-fixture accounting totals and a test
  selecting the first Saga match. Expectations now reflect nine capped singles
  (six ready/three blocked), while the event-origin test selects its exact source method.
  Luna independently reviewed these changes; all substantive invariants remain checked.
- `git diff --check` passed.

Docker uses the repository's `scenario-executor` Compose service, fresh isolated
JVM/Spring/H2 workers, real application dispatch, and the normal parent orchestrator.
A qualification-only driver selects exact workload IDs from an unchanged package and
validates selection through the current reader/preflight planner. It writes evidence
under the ignored target directory; no product CLI or package projection was introduced.

| Qualification | Result | Worker orchestration duration |
|---|---|---:|
| Seven formerly rejected state-only setups in their original package | **7/7 SETUP_READY** | 109.23 s |
| Three nested `quiz.aggregateId` inputs in the M1 package | **3/3 SETUP_READY** | 49.54 s |
| Final repaired feature-prefix sample | **3/3 SETUP_READY** | 43.08 s |

The final prefix sample is:

- AddParticipantAndCreateTournamentTest, creating/activating/enrolling NewUser before
  addParticipant: 17 setup actions, three bindings.
- AddStudentTest, adding the second student after the first: six actions, two bindings.
- ExecutionReadOperationsTest, querying courses for a new unenrolled user: six actions,
  one binding.

The initial five-example prefix sample produced two ready workloads, the DTO mutation
failure subsequently repaired, and two explicit method-authority failures. StartQuiz
requires an unregistered createQuiz method (and a prior startQuiz); LeaveTournament
requires an unregistered addParticipant method. Those methods were not silently authorized.
The parent's nonzero-worker handling masks their detailed cause in the combined report;
the exact reasons are retained in the worker logs.

Selected IDs, reports, full logs, qualification driver/script, and hash proof are under
`verifiers/target/astra-qualification/`. The final prefix files are
`repaired-prefix-selected.json`, `repaired-prefix-preflight.json`,
`repaired-prefix-preflight.log`, and `repaired-package-determinism.json`.
The driver script copies source into the container, builds there, and clears the Compose
SPRING_PROFILES environment override before launching with explicit test,sagas,local
profiles, matching the normal launcher's behavior.

## Remaining limits and next work

Static candidacy, setup readiness, and successful fault execution remain distinct. This
run does not requalify the earlier full size-one-through-three space, all 577 candidates,
or a harmful/control benchmark. The seven-case regression does not rewrite the old
402-workload report or establish runtime proof for newer packages.

`RUNTIME-FOLLOWUPS.md` records concrete work beyond this issue: exact application setup
method registrations, nested question producer references inside DTOs, failed-worker
reason preservation, and selected event-receiver prerequisites. The existing triple
creates a course execution and user, but its QuizAnswer/Tournament event route needs a
receiver attached to those same identities. Feature-prefix extraction does not synthesize
that receiver or combine independent tests.

Canonical current-state, roadmap, ordered-setup ADR, and the prior audit were updated with
these final boundaries and measurements. No unrelated source, generated target evidence,
commit, push, or merge is included in the working-tree change.
