# Allocation follow-up agreed with André — 21 September

Paper updates remain paused until the new comparisons are reviewed. This is the
approved next step, not a manuscript draft or a final performance claim.

## Six matched variants

Compare three models, each with and without the same missing-feedback rule:

1. Independent adaptive UCB: learns a score estimate separately for each workload.
2. Shared linear UCB using only a bias and already observed progress.
3. Shared linear UCB using the existing structural features and observed progress.

The rule excludes the workload that returned unknown fitness from the next decision
if another workload is available. It remains eligible when it is the only active
workload. Known zero does not trigger the rule. Unknown feedback consumes one attempt
but provides no reward label, GA parent or model update in every variant.

Use the existing nine-map cohort (189 scenarios) and its extension with the complete
3,918-scenario map (4,107 total). Preserve the selected reference versions and all
unknowns. Use seeds 1–30, exploration/ridge 1, population 8 and mutation 0.3. Compare
all five unit-weight criteria and deleted dependencies alone. Budget checkpoints are
30/60/120/189 for the small cohort and 100/500/1,000 for the mixed cohort. This produces
720 recorded-feedback searches, with no new application executions. Do not tune on
these results. Preserve prior raw and guarded results for verification.

Report positive discoveries, accumulated available score, unknown attempts, allocation
by workload, variability and paired differences. Separate the effect of structural
features from the effect of the missing-feedback rule. This is an initial feature
comparison; it does not establish transfer to unseen Saga families.

## Unavailable-result audit

Count unavailable scenarios by workload, execution status, criterion and reason;
distinguish overlapping reasons from distinct scenarios. Inspect representative retained
reports and the verifier code to identify bounded fixes. Application and simulator
source are outside this change; no Quizzes edits. Preserve original measurements and
never turn unknowns into zeros or discard them to improve a comparison. If existing
evidence permits reassessment, create a separately labelled reference and verify exact
candidate/report identity. If an execution must be repeated, report the necessary
cases and infrastructure requirements before starting a new campaign.

The audit verified the execution report for all 450 unavailable scores. All 218
`PROCESS_FAILURE` outcomes are recovery failures on missing aggregate 12 (130 explicit
compensations and 88 implicit rollbacks), not retained timeouts. The remaining cases are
186 residual assessments with a competing lifecycle change, 16 with an event writer,
and 30 read assessments with intervening writes. The first two assessment guards admit
bounded verifier corrections using existing writer/version/field evidence; the read
extension and incomplete-recovery scoring remain outside this correction.

Reassess the 202 residual-unknown cases with a versioned pure assessor and verify them
against the original report hashes. If fitness changes, run the same six variants again
under the separately labelled reference, without tuning. For the deleted-dependency
profile, reuse earlier decisions only after proving every candidate's reward is unchanged.
This additional comparison uses existing observations and requires no application run.

## Verified assessment correction

The parent replayed all 202 retained execution/impact report pairs with the previous
assessor: category results, status and score matched in every case. The new v3 assessor
resolves all 186 lifecycle-related residual assessments; 184 obtain full weighted fitness,
while two still have incomplete read attribution. All 16 event cases now establish writer
provenance but still have overlapping field changes, so their residual assessment remains
unknown. Unrelated categories are identical in every reassessed report.

The revised mixed reference contains 3,228 positive, 613 zero and 266 unavailable scores.
The remaining unavailable cases are 218 incomplete recoveries, 32 intervening-writer read
assessments and 16 overlapping event/Saga residual assessments. The original 30 read cases
were those outside the residual-unknown group; the two overlapping cases explain why 186
resolved residual assessments yield 184 newly available full scores.

No application or simulator source changed. Old observations and their report hashes
remain preserved; revised observations explicitly identify their pure reassessment.
Every candidate's deleted-dependency reward is unchanged. Every small-cohort reward under
both profiles is also unchanged. Those 540 searches can be reused exactly; only the 180
mixed/all-criteria searches need repeating. Together with the original 720 searches this
produces 900 searches, rather than repeating application executions or identical searches.

## Ownership and next steps

Completed: Sol implemented and tested the six policy variants and bounded assessor fixes;
the parent independently reviewed the code, verified retained evidence and ran all 900
searches. [Results, interpretation and verification](../evidence/allocator-factorial-2026-09-21/RESULTS.md)
are ready for André. The guard does not improve the mixed-cohort results; structural
context is not consistently better than progress-only sharing. No policy default changed.
Output remains compact (about 65 MiB of local task artifacts and under 0.5 MiB of durable
summary evidence). No cluster jobs, live allocator integration or paper edits were made.
Next, review the allocation choice and scope live execution/resume; a separate experiment
would be needed to establish transfer to previously unexplored Saga families.
