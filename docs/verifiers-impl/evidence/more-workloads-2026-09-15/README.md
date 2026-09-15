# More fixed-workload exploration (2026-09-15)

This page retains the initial, pre-fix exploration. The subsequent
[recovery-history qualification](../recovery-history-2026-09-15/README.md) supersedes its
pending-recovery blocker and records the larger campaign launch.

This exploration found one larger, executable fixed workload with useful
impact variation. It uses four ordinary tournament Sagas against the same
source-created tournament: add a participant, update the tournament, let the
participant leave, and remove the tournament. The source story is a new normal
Quizzes application test; no application service, verifier production path,
detector, GA, or prerequisite provider was changed.

## Retained workload

The retained workload is
`ef019eb258c89262dddfeed7279c00a43b2620e2854274aefdefb4f644becf2a`.
Its fixed forward order is:

1. `AddParticipant`: `getUser`, `addParticipant`
2. `UpdateTournament`: `getOriginalTournament`, `getTopics`,
   `updateTournament`, `findQuestionsByTopicIds`, `updateQuiz`
3. `LeaveTournament`: `getOldTournament`, `leaveTournament`
4. `RemoveTournament`: `getTournament`, `removeQuiz`, `removeTournament`

The whole-participant ordering was selected after ordinary
`INTERACTION_PRUNED` generation. It preserves the normal source story and
avoids an early semantic-lock conflict found in the first retained interleaving: `RemoveTournament`
tried to read the tournament while `UpdateTournament` still held it. The
control for the retained workload is `SUCCESS`, `EXACT`, `I=0`, `A=0`, and
lost-copied-update count 0, with complete enabled coverage.

This ordering describes normal forward actions. When a fault interrupts one Saga,
generated recovery actions may execute between the later Sagas' actions. Thus the fault
domain still explores interactions with recovery; it does not merely repeat the serial
happy path.

There are 216 canonical fault vectors. Ordinary on-demand generation with
recovery cap 500 produced exactly 5,184 unique persisted candidates: all 216
vectors were counted, every reported recovery schedule was written, and no
vector was truncated. This is candidate multiplicity within one fixed
workload, not 5,184 alternative workloads.

The package contains nine extracted copy contracts. The currently exercised
histories produced no lost-copied-update finding; the useful signal in this
workload is impact variation across shared Tournament/Quiz state and recovery.

## Runtime qualification

Four declared fault histories were resolved through the ordinary on-demand
generator and executed in fresh Docker/JVM/H2 runtimes:

| Fault vector | Selected history | Outcome |
| --- | --- | --- |
| `000010000000` | last of 6 | process failure; compensation of the updated tournament failed after later removal, so all scores are unavailable |
| `010000000000` | only candidate | process failure; the faulted add was followed by a legitimate `User ... is not enrolled` rejection during leave, so all scores are unavailable |
| `000000001001` | last of 6 | `PARTIAL`, `EXACT`; one deleted dependency, plus an unknown failed-operation residual caused by a competing/unknown Quiz writer; `I` unavailable, `A=0`, lost copied=0 |
| `010010001001` | last of 54 | `COMPLETE`, `COMPENSATED`, `EXACT`; one deleted dependency and one failed-operation residual; `I=2`, `A=0`, lost copied=0 |

Unavailable values above remain unavailable; they are not treated as zero.

A modest random calibration used seed 15110 and budget 12 after the structural
probes. It evaluated 12 unique scenarios with no duplicate proposals. Seven
completed and five ended in process failure. Among completed attempts, three
had positive weighted-v2 impact (`I=2`, `I=2`, and `I=1`) and four had zero;
the best score was 2. All seven complete attempts had `A=0` and lost copied=0.
This is exploratory evidence, not a matched GA comparison or an effectiveness
estimate.

Four of those five process failures end with `PENDING_RUNTIME_RECOVERY` after
LeaveTournament rejects a student whose AddParticipant operation failed. The fifth
ends with `COMPENSATION_FAILED`: later removal deleted the Tournament that recovery
tries to update. The generic process-failure label does not establish an infrastructure
problem. These two causes must remain visible when comparing strategies.
The [follow-up diagnosis](RECOVERY-DIAGNOSIS.md) reproduces a recovery attribution defect
behind the four pending-checkpoint failures and records estimates for a 500-attempt horizon.
It proposes a fix; production recovery behavior has not yet changed.

Across both four-Saga histories, this exploration made 22 fresh application attempts:
two controls, eight targeted variants and twelve random attempts. Static domain counting
does not add application executions.

## Generation boundary and rejected probes

The source selection accepted exactly four inputs and materialized its setup.
Type-only fallback was disabled. Conflict construction used 33 symbolic
Tournament edges; warnings also report type-only footprints for Quiz and
Question, but no type-only conflict edge was admitted. The generator hit both
the 30,000-schedule and 30,000-workload catalog caps before the explicit
whole-participant filter. The retained serial history was present inside that
capped prefix; this run does not establish exhaustive coverage of alternative
forward schedules.

The first arbitrary retained four-Saga interleaving
`006abb88f1dce4a84f3cbd07a42752849d0bfc5a773611577d814d4eb7bcaac5`
also had 216 vectors and an exact 3,984-candidate recovery domain, but its
control was `PARTIAL_COMPENSATED`/`DEVIATED` because of the early remove read.
It is retained as diagnostic evidence and should not be the primary comparison
workload.

Existing-source calibrations were smaller: two AddParticipant +
UpdateTournament workloads had 36 and 23 candidates, and one AddParticipant +
UpdateTournament + FindTournament workload had 52. AnonymizeStudent +
UpdateTournament, with and without the test's explicit event call, generated no
exact tuple. The direct footprints had no conflict edge, and participant
selection occurs before event-consequence placement, so this is an indirect
event-pruning boundary rather than a materialization failure.

## Evidence and validation

- Frozen runtime base: `verifiers/target/integral-input-materialization-01/config.json`.
  It overlays the repaired integral-number reader/materializer classes and
  retains the complete source/class hash descriptor.
- Selected package: `verifiers/target/more-workloads-2026-09-15/generation-probes/add-update-leave-remove-serial/package/`.
- Exact domain: `verifiers/target/more-workloads-2026-09-15/domain-counts/add-update-leave-remove-serial-ef019/domain-count.json`.
- Control and targeted histories:
  `verifiers/target/more-workloads-2026-09-15/qualification/add-update-leave-remove-serial-ef019-{control,targeted}/`.
- Random calibration:
  `verifiers/target/more-workloads-2026-09-15/random/add-update-leave-remove-serial-ef019-seed15110-budget12/`.
- Generation probes and rejected histories:
  `verifiers/target/more-workloads-2026-09-15/generation-probes/`.
- Machine-readable index: `inventory.json` beside this file.

The normal application test was run with
`mvn -Ptest-sagas test -Dtest=AddParticipantAndUpdateTournamentTest`: 4 tests,
0 failures, 0 errors. The helper Java source compiled inside the same immutable
runtime image during both focused package generations. The three Python helpers
also pass `python3 -m py_compile`.

Independent review verified all 22 distinct execution-attempt identities and their
workload/scenario joins, 132 retained report hashes, the twelve recomputed fitness
values, and all five exact domain counts directly against distinct persisted action
sequences. See `review.json`, `artifact-hashes.json` and `application-test-summary.json`.

For the next matched comparison, use this retained workload with equal random
and GA evaluation budgets of 100 or 200 unique attempts and explicit seeds.
Keep null-fitness process failures in the denominator and report complete,
partial, and unavailable outcomes separately. The 5,184-candidate exact domain
is large enough for those budgets without relying on workload padding.
