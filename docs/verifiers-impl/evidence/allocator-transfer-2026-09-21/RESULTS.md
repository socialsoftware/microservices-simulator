# Transfer to new Saga combinations — 21 September 2026

## Result

Previous learning did not improve the final discovery count in this experiment. Structural
linear UCB found 138.10 positives with prior learning versus 144.73 without it, after 256
target selections. The progress-only model found 146.77 versus 150.20. This is a result for
one fixed split of existing Quizzes workloads, not a general ranking of the models.

## Experiment

The [frozen protocol](protocol.json) uses 66 workloads containing one or two Saga types for
learning, and 15 workloads containing three types for testing. They contain 485 and 1,026
scenarios respectively. No target Saga set occurs in training; every target Saga type is
known from training. All selected maps share the same retained runtime descriptor and
have fewer than 500 scenarios. This tests new combinations of known Sagas, not unseen Sagas.

For each of 30 seeds, stable round-robin allocation generates one common prefix of 132
observations with the existing local GA. Both warm models learn from exactly those
observations and pre-feedback contexts. The structural model receives Saga membership,
Saga pairs, interactions, events and observed progress. The other model receives bias and
progress only. The known structural vocabulary includes training and target workloads;
no target result enters that vocabulary or model fitting before selection.

The four target arms combine these two feature sets with either the learned linear model
or a fresh model. All target workloads start with empty GA and progress state and matched
local seeds. Only the linear model arrays and update count transfer. Every arm then spends
256 selections in target workloads. Existing population 8, mutation 0.3, exploration/ridge
1 and uniform-unseen candidate selection are unchanged. The unavailable-feedback rule is
off in all arms. All five criteria have unit weights. The dependency-only profile has
no positive targets in this split and is not compared for positive discovery.

Training discoveries are outside the target curves, and the 132-observation cost is
explicit. The cold arms deliberately discard the common prior history to isolate its
effect; this is not an end-to-end cost comparison against a separately deployed cold model.
All feedback is replayed from retained measurements; no new application execution occurred.

## Discovery results

Means over 30 seeds; p10–p90 describes the spread across those seeds.

| Model | Prior learning | Positives at 256 | Positive p10–p90 | Accumulated score |
| --- | --- | ---: | ---: | ---: |
| Structure and progress | Yes | 138.10 | 120.8–156.0 | 162.50 |
| Structure and progress | No | 144.73 | 130.0–155.5 | 171.37 |
| Progress only | Yes | 146.77 | 123.8–159.0 | 171.80 |
| Progress only | No | 150.20 | 142.4–157.2 | 175.37 |

There are no unavailable scores in these selected references or runs. At the primary
256-selection checkpoint, structural transfer loses 6.63 positives on average (4.58%).
It wins in 15 seeds and loses in 15, with larger losses than gains. Its paired bootstrap
95% interval for the mean difference is [-12.97, -0.73]. Progress-only transfer loses 3.43;
its interval is [-8.40, 1.23]. These intervals describe seed variation conditional on the
fixed split, not uncertainty across applications. The difference between the two transfer
benefits is -3.20, with interval [-10.63, 4.60]; it does not establish a reliable difference
in transfer benefit between feature sets.

![Mean target discoveries and scores, with p10–p90 shading](transfer-curves.png)

## What changed in the choices

Structural transfer initially chooses the target Saga set that contains positives in
24 of 30 seeds. The fresh structural model first chooses a different set in every seed.
With all progress values initially zero, progress-only models give all target workloads
an equal initial index; the existing deterministic ID tie-break happens to select the
positive set. That initial choice is not evidence of semantic knowledge in that model.

The early structural benefit is modest: at 16 target selections, prior learning finds
6.93 positives versus 5.27 without it; at 32, 15.57 versus 13.33. By 64 the corresponding
means are 31.53 versus 32.13, and the final mean is lower with transfer. Checkpoints were
specified before running; the final budget remains the primary endpoint.

Both structural arms spend almost the same number of selections on the positive Saga set
(246.67 with learning, 245.97 without). The final difference therefore concerns allocation
among its ten workloads as well as their local search trajectories. The trained arm spends
55.67 selections on four orders whose full maps contain 25–33% positives, versus 30.67
without training. This is an observed allocation difference, not proof of a single cause.

Those ten workloads have the same initial structural profile. Their ordinary orders are
not encoded by the current model, so structural transfer cannot initially tell those orders
apart; only subsequent progress distinguishes them. Training is also sparse: the common
prefix has 8.43 positives on average in 132 selections, while targets contain 516 positives
in 1,026 scenarios. This distribution difference is context for the outcome, not an isolated
causal explanation.

## Scope and verification

- Target positives all belong to AddParticipant + FindTournament + UpdateTournament.
  The other three target Saga sets are all zero. More diverse evidence is needed for a
  broad transfer claim; increasing repetitions of this split does not remove that limit.
- 120 target searches and 30 common training prefixes were completed: 34,680 recorded
  selections, not 34,680 new application executions or distinct measured scenarios.
- All five phase traces per seed match evaluator calls exactly; feedback is revealed only
  after selection, with no repeated candidate within a phase. Target totals are recomputed.
- All target GA/progress states begin empty. Warm model state is the exact trained state;
  cold model updates begin at zero. Training contexts use only preceding feedback.
- The parent independently compared seed 1 against the original allocator: the training
  sequence and both cold target trajectories match exactly, including target UCB indices.
  Initial warm predictions and uncertainty match a separate batch linear solve to less
  than 7e-15. See [independent review](independent-review.json).
- The 115-test fixed-workload suite passes, including eight transfer tests. Existing
  allocator, GA, fitness, catalogue and cooldown source hashes remain unchanged.
- 78 complete enumerations were rechecked locally. Three creation maps use earlier verified
  completeness provenance. All 1,511 reference candidates and stored weighted assessments
  pass the production evaluator checks. Raw archives were not re-audited in this experiment.

The evidence supports reporting this bounded negative transfer result. It does not justify
tuning parameters until the model wins, nor a general claim that structural context cannot
help. Ordinary-order features or another structurally chosen split would be separate work.
No paper edits or cluster jobs were performed.

## Artifacts

- [Protocol fixed before transfer runs](protocol.json)
- [Inputs and source-reference hashes](inputs.json)
- [Input verification](input-verification.json)
- [Means, spread, paired differences, allocations and per-seed hashes](summary.json)
- [Independent review](independent-review.json)
- [Figure as SVG](transfer-curves.svg)
- [Reproduction commands](../../../../verifiers/experiments/allocator-transfer/README.md)

Per-seed compact traces and receipts remain under
`verifiers/target/allocator-transfer-2026-09-21/runs/` (about 24 MiB including local review
artifacts). No raw application reports were duplicated.
