# Smaller exhaustive reference with recorded-feedback search

The campaign completed in 53.76 minutes. All 186 candidates were measured; all six
repeat checks matched. The runner verified frozen runtime, package and search-source
hashes before completion. Full raw evidence and 60 search traces remain under
`verifiers/target/exhaustive-reference-2026-09-16/`; `results-summary.json` and
`discovery-curves.png` here retain compact results.

## Completed results

The reference contains 72 complete zeros, 60 scores of 1, 52 scores of 2 and two
unavailable combined scores. Both unavailable cases have persistent I=1 but an
`INTERVENING_WRITER` read gap. Thus 184/186 (98.92%) have complete enabled feedback;
112 are positive under the complete-score rule. The other two are not evidence-free:
their known residual is retained, while combined fitness remains unavailable.

| Distinct candidates evaluated | GA mean positives | Random mean positives |
| --- | --- | --- |
| 25 | 12.03 | 8.83 |
| 50 | 26.03 | 18.83 |
| 100 | 54.93 | 45.43 |
| 150 | 85.17 | 80.00 |
| 184 | 110.00 | 110.07 |

At 50 and 100 evaluations GA leads in all 30 paired seeds. Reaching 56 of the 112
known positives takes 101.27 evaluations on average for GA versus 116.37 for random.
This supports earlier discovery for this workload; the advantage shrinks near exhaustion.
The existing random sampler is per-Saga/per-recovery, not uniform over distinct candidates.

GA reaches all 112 in 23/30 seeds, random in 27/30. Every successful exhaustive search
needs all 186 evaluations. The remaining searches stop at the duplicate-proposal limit:
GA six times at 185 and once at 184; random three times at 185. These are proposal
stalls, not proof that the candidate domain was exhausted. No duplicate proposals cause
additional application executions. No fallback or operator was added for this comparison.

Forty application executions deviate from the planned schedule because LeaveTournament
rejects a student who is not enrolled (`User 4 is not enrolled in tournament 12`).
They finish recovery and retain their actual-history assessment under the existing
fitness policy. The remaining 146 conform exactly; no recovery fails. Do not describe
all measured candidates as exact schedule realizations or count rejections as impact.

The positive components are failed-operation residuals (114 observed, including both
unknown combined scores) and compensated-read exposures (52). Other criteria are zero.
These are repeated scenario manifestations, not 112 distinct application bugs.

## Added uniform-catalogue comparison

The subsequent [uniform baseline](uniform-baseline/REPORT.md) changes the interpretation
of the initial comparison. It shuffles the sorted keys of all 186 candidates without
replacement, using seeds 1–30; scores are looked up only after the full permutation is
chosen. Existing GA and two-stage random traces and all application results are unchanged.
Every permutation is complete, distinct and reproducible, all scores match the reference,
and source hashes are unchanged. No application was executed again.

| Evaluations | GA | Two-stage random | Uniform catalogue random |
| --- | --- | --- | --- |
| 25 | 12.03 | 8.83 | 14.83 |
| 50 | 26.03 | 18.83 | 29.33 |
| 100 | 54.93 | 45.43 | 59.37 |
| 150 | 85.17 | 80.00 | 89.93 |
| 184 | 110.00 | 110.07 | 110.83 |

These are means over 30 seeds. At 100 evaluations, uniform random beats GA in 24
same-numbered seed comparisons, ties in three and loses in three; shared seed numbers
do not couple the algorithms' random choices. The exact uniform expectation at 100
is 100 × 112 / 186 = 60.22 known positives. Uniform random reaches all known positives
in all 30 searches, at a mean 185.33 evaluations; GA does so in 23/30 searches.
Mean evaluations to 56 positives are 94.00 uniform, 101.27 GA and 116.37 two-stage random.
The figure averages all 30 seeds only through the common measured horizon of 184;
it does not fill in outcomes beyond stalled GA runs.

The reference has 54 fault-vector groups: 36 contain only zero-score cases (72 cases
in total), while 18 contain all 112 complete positives and both unavailable cases.
Uniform per-vector proposal sampling therefore gives only one third of proposals to
those positive-bearing groups. Uniform candidate sampling gives those groups 114/186
of its initial probability mass. Accounting for the two unavailable cases, the exact
first-proposal complete-positive probabilities are 32.72% and 60.22%, respectively.
Duplicate rejection changes later proposal distributions. This explains a structural
weakness of the old random baseline; it does not isolate which GA operator limits GA.

The supported claim is now **GA outperforms the two-stage random sampler here, but does
not outperform uniform sampling of the complete catalogue on positive-case discovery**.
Do not generalize the earlier comparison to all random baselines. The larger 500×3
campaign still compares against its original sampler; this smaller experiment does not
establish the outcome of adding uniform sampling to that larger domain.
Full enumeration/storage is required for the uniform permutation and its cost is not
included in these recorded-feedback discovery curves. Different input/workload families,
weight choices and live costs remain separate questions.

Reproduce into a new output directory (requires matplotlib):

```sh
python verifiers/experiments/fixed-workload-ga/uniform_reference.py \
  --reference verifiers/target/exhaustive-reference-2026-09-16 \
  --output verifiers/target/my-uniform-reference
```

## GA diagnosis after the uniform comparison

The [frozen-search replay diagnosis](ga-diagnosis/README.md) reproduces every choice
in all 30 retained GA searches. In the first 100 evaluations, 64.27% of evaluated
candidates come from random initialization/fallback; only 35.73% are novel offspring.
Those offspring are positive in 88.43% of cases, versus 36.31% of evaluated random
candidates. Most offspring proposals duplicate a seen case and immediately switch the
search to its old random sampler. The subsequently approved [uniform-exploration experiment](ga-uniform-exploration/README.md)
changes only finite-map initialization/fallback to uniform unseen sampling. At 100
evaluations it finds 66.80 positives on average, above original GA (54.93) and uniform
random (59.37); all 30 searches cover the catalogue. The default live policy remains
unchanged. This is a development-map improvement requiring confirmation elsewhere.

## Selected application story

Use the same source-derived setup and inputs as the larger four-Saga campaign, selecting
AddParticipant, UpdateTournament and LeaveTournament. A student joins the Tournament;
the teacher changes its question settings; the student leaves. The Tournament and its Quiz
remain present. The source is the normal application test `add participant, update
tournament, leave, and remove tournament`; the fourth operation is not selected here.
No input recipes or prerequisite provider were hand-written for this experiment.

Forward steps keep that whole-Saga order. Fault positions and the placement/order of
recovery actions vary through the ordinary generator. For this fixed workload there are
54 canonical fault vectors and exactly 186 distinct fault/action candidates, with every
vector enumerated and no recovery-cap truncation. This is not exhaustive enumeration of
all possible inputs, forward histories, workloads or application behavior.

The alternative AddParticipant → UpdateTournament → RemoveTournament has 384 candidates,
but its no-fault execution fails an invariant while deleting the Tournament: a deleted
Tournament must not retain participants. Its omitted LeaveTournament step matters.
The final evidence has score 2 after recovery, but this is not the intended successful
baseline for the comparison. The candidate and its diagnostic probes are retained, not fixed
or silently dropped. See `qualification.json` for both candidates and exact paths.

## Qualification before search

The selected no-fault control is SUCCESS/EXACT, score 0, with complete enabled criteria
and the `exclusive-observed-fields-v2` assessment policy. Four structural probes gave:

| Probe | Score |
| --- | --- |
| Update faults before finding Quiz questions; first generated recovery order | 1 |
| Same update fault; last generated recovery order | Unavailable |
| Update and leave both fault; last generated recovery order | 2 |
| Participant addition faults | 0 |

The unavailable probe has complete persistent I=1, but its read criterion has
`INTERVENING_WRITER`. The reference therefore cannot be assumed to have complete labels.
Retain unknown scores, report the scorable fraction, and use **known positives** for
discovery fractions. Reaching all known positives does not classify the unknown cases.

Selection prefers the smaller qualified domain and is made before GA/random outcomes.
These are calibrated examples from the same application family, not independent randomly
sampled applications or an unbiased prevalence study.

## Execution and comparison protocol

The user selected full application measurement followed by search over recorded feedback,
rather than fresh application execution in each method and seed.

- Reuse the selected control and four probes after report/package/runtime checks.
- Execute the remaining 181 cases in fresh Docker/JVM/database instances, at most two
  concurrently. Every candidate is selected from the complete generator responses.
- Repeat six keys chosen deterministically before measuring the map. Require matching
  fitness components and execution status/conformance; stop comparison on disagreement.
- Compare GA and random for seeds 1–30 with the unchanged search loop, population 8,
  mutation 0.3, stall limit 1,000 and a budget of 186 distinct candidate evaluations.
- Use five unit weights. The structural domain has no scores; only the evaluation
  callback returns a case's recorded feedback after the policy selects it.
- Keep the existing per-Saga fault / returned-recovery random sampler. It is not uniform
  over all 186 candidates. Unknown results consume budget and cannot become GA parents.
- Preserve proposal stalls. No new fallback guarantees exhaustion. Record actual
  evaluations, duplicate proposals, stop reason and whether all known positives were reached.

Metrics include cumulative known positives, evaluations to all known positives where
reached, the number of seeds reaching that threshold, and mean positives at the common
measured horizon. Seeds 1, 11 and 29 are chosen in advance for the example figure; all
30 pairs are retained. Replayed search wall time is not live-search performance. The
separately measured application/generation costs and the earlier live campaign support
cost discussion; do not invent a new online timing curve by adding cached durations.

Six repeat controls check some repeatability; they do not prove that every candidate is
deterministic in arbitrary deployments. This protocol evaluates ordering against this
measured map under the frozen simulator setup.

## Runtime, proof and operation

The runtime reuses the hash-frozen recovery-qualified application and overlays only the
current ImpactV2 assessor/report classes, compiled in the same immutable JDK image.
The new policy was verified in fresh controls. No application, generator or GA operator
was changed. The new `exhaustive_reference.py` is an experiment driver using the existing
runtime and search code. Its structural-domain checks and the existing search/fitness
suite pass 53 Python tests; a two-candidate synthetic smoke exercised the 60 replay
outputs, summary and plots before launch. Synthetic output is not application evidence.

Selection, generation commands, full uncapped counts, controls, probes, source snapshots,
runtime hashes and logs live in `verifiers/target/exhaustive-qualification-2026-09-16/`.
`prepare.py` here retains the source-selection/runtime-freezing recipe. Its initial run's
Python finalizer used an incorrect catalogue filename after Java successfully exported
both packages; configuration was finalized from the existing `workloads.jsonl`. No
application observations or generated schedules were changed by that correction.

The campaign `protocol.json` records package/source hashes and reused keys. All derived
outputs are separate from the completed 500×3 campaign. Full report evidence is stored
once in the reference; per-seed outputs retain selected keys, scores and parent lineage
instead of duplicating every application report.

The measured controls/probes took approximately 30–38 seconds per application attempt.
The completed campaign took **53.76 minutes** with two workers, including six repeats
and recorded-feedback comparisons (five qualification cases were reused). This is not
the cost of 60 live searches. The process-bound idle-sleep inhibitor ends with the process.

From the repository root, inspect progress with:

```sh
cat verifiers/target/exhaustive-reference-2026-09-16/status.json
tail -n 5 verifiers/target/exhaustive-qualification-2026-09-16/campaign.log
```

`measured` includes the five reused cases. Stages are MEASURING_REFERENCE,
CHECKING_REPEATS, REPLAYING_SEARCH, COMPLETE or FAILED. If repeat evidence disagrees,
retain the map and investigate before making ordering claims. On completion, inspect
coverage, all seed stop reasons and the figure before selecting claims for the paper.
