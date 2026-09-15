# Configurable criterion weights: bounded qualification

The user-approved change introduces one weight for each existing persistent-state criterion
and one for compensated-read exposures. The old distinct-object I policy remains available.
The [configuration guide](../../../../verifiers/experiments/fixed-workload-ga/README.md#configurable-fitness)
describes the command, coverage rules and offline revaluation.

## What was checked

The qualification first revalued 43 retained cases: the complete 29-case
RemoveTournament/AddParticipant reference and the 14 UpdateTournament/FindTournament
histories. It verified report hashes and identities before applying each policy. It also
replayed recorded feedback through both the frozen original search and the new default
policy, using GA/random and seeds 11/29/47: all six comparisons preserved the candidate
sequence, duplicate count and best I. These checks performed no application executions.

The new runtime campaign used the last update/read workload, where the read follows the
failed Quiz update and can fall before, between or after the two recovery actions.
It ran one successful no-fault control, three fixed recovery witnesses, and three
independently measured search arms of eight executions each: **28 fresh executions**.
Every attempt had COMPLETE I, EXACT schedule conformance and COMPLETE_WITHIN_SCOPE read
evidence. The fixed witnesses were separate from the search and supplied no search feedback.

The protocol fixed seed 29, population 4, mutation 0.3 and recovery cap 20 before execution.
The smaller population lets an eight-execution integration check exercise crossover;
it is not a tuned replacement for the normal population default of eight.

## Concrete score difference

In this example, failed Tournament-update recovery leaves the embedded topics without
course identifiers. The residual-state criterion counts the Tournament once. A concurrent
FindTournament can also read the updated revision before it is compensated.

| Read position in the three fixed witnesses | I | Compensated-read exposures | Persistent weights 1/1/1; read weight 0 | All four weights 1 | Read only |
| --- | ---: | ---: | ---: | ---: | ---: |
| Before recovery writes | 1 | 1 | 1 | 2 | 1 |
| After settings recovery, before lock rollback | 1 | 0 | 1 | 1 | 0 |
| After both recovery actions | 1 | 0 | 1 | 1 | 0 |

The new executions reproduced all three cases. Revaluing the full 14-case history gives
six controls with score 0, five fault histories with combined score 1 and three with score 2.
The 29-case removal reference stays at 15 cases with score 2 and 14 with score 0 under
unit persistent/combined weights; it has no observed compensated-read exposures.
Neither result chooses a business exchange rate between criteria.

## Short search results

| Arm | Executions | Positive-score scenarios | Read exposures | Best configured score | New crossover executions |
| --- | ---: | ---: | ---: | ---: | ---: |
| GA, persistent criteria only | 8 | 2 | 1 | 1 | 2 |
| GA, persistent + read criteria | 8 | 2 | 1 | 2 | 2 |
| Random, persistent + read criteria | 8 | 1 | 0 | 1 | 0 |

Both GA arms made the same candidate choices. Their first read-positive result occurred
at execution eight, when their budget ended. Its increased weight therefore had no
opportunity to affect a later choice. This qualifies score/report integration, not a
benefit from weighting or an evolutionary advantage. Controlled search tests separately
verify that changing criterion weights changes GA choices while preserving random choices.
A wider comparison needs longer runs and representative workloads, not a claim based on
this single seed. The three search arms are not all one matched-policy comparison: the
combined GA/random pair is matched; the persistent arm checks a second configuration.

## Coverage, overlap and proof

The 35 passing Python tests include invalid/partial/unavailable evidence, zero-weight
exclusion, finite/nonnegative weight validation, category overlap, fractional weights,
score-driven parent selection, legacy behavior, duplicate budget accounting and retained
report integrity. An object in two persistent categories still has I=1, but contributes
once to each enabled category term. Unknown counts never become complete zeros.

A post-campaign review allowed complete read-only scoring when all disabled persistent
criteria are unavailable, and prevented newly added read sidecars from being treated as
original evidence during offline revaluation. Targeted tests cover these two cases. The
final code revalidated all measured scores and parent fitness values unchanged, and
repeated the 43-case revaluation and six legacy comparisons without additional application
executions. The campaign's source snapshot and final-source hashes are both retained.

[summary.json](summary.json) records counts, final source hashes and the post-campaign
review. [offline.json](offline.json) retains component counts, coverage, policy values
and measurement hashes for all 43 cases. [protocol.json](protocol.json) records the
predeclared live experiment; [legacy-regression.json](legacy-regression.json) identifies
the frozen original search used for comparison. Full reports, package snapshots, source
snapshot, lineage and final audit remain in
`verifiers/target/fixed-workload-ga/weighted-01/`.

No detector, Java executor, generation algorithm, crossover/mutation rule or RL allocator
changed. The next research task is selecting a broader workload cohort and a small set of
predeclared preferences for an equal-budget comparison. New anomaly families remain a
separate implementation task.
