# Impact and anomaly comparison for the advisor discussion

This is an **offline comparison of measured results**, not a chosen fitness function or
search experiment. The first matrix reused retained results; the
[read-scope correction](../read-scope-2026-09-10/README.md) subsequently repeated all
14 histories and removed six false coverage gaps without changing their counts. The
[Portuguese discussion note](../../reunioes/2026-09-11.md) develops the example for the meeting.

## Measurements and evidence boundary

- **I** is the complete ImpactV2 count of distinct aggregate identities matching its
  three implemented persistent-state conditions. Several findings on one identity count
  once. It is not a count of changed fields or a general domain-correctness verdict.
- **A** is the observed compensated-read exposure count, deduplicated by producer,
  reader and produced revision. Three restored attributes in one finding still mean
  A=1. This is the implemented creation/update pattern, not all concurrency anomalies.
- Operation outcomes and coverage remain separate. A successful reader need not write
  anything; reading a subsequently compensated revision is sufficient for this diagnostic.

[matrix.json](matrix.json) retains all 14 source-derived update/read executions, exact
scenario/workload/attempt IDs, ordered actions, participant outcomes, positive findings,
coverage and SHA-256 references to the original reports. Values were cross-checked against
the [latest runtime qualification](../read-scope-2026-09-10/validation.json).
The [harness](../../../../verifiers/experiments/saga-update-read/source-inputs/README.md)
owns generation and reproduction. Original reports remain under
`verifiers/target/saga-update-read/read-scope-01/`; this extract does not replace them.
The [previous matrix](matrix-before-read-scope.json) retains the earlier source-input-02
measurements and their report hashes.

All 14 use ordinary source setup and EXACT execution, with complete I and
`COMPLETE_WITHIN_SCOPE` A. The six successful controls have I=0/A=0. The earlier six
CommitSagaCommand gaps are resolved by checking command scope before reader attribution;
the new executions preserve every impact score and exposure count. Complete scope means
the declared outer Quiz/Tournament response adapters, not all application reads.

For this offline table only, combined numbers require both measurements to be complete.
Other rows remain visible with null combined values. This conservative presentation rule
is not an adopted production/search eligibility policy or a reason to discard controls.

## Concrete update experiment

A updates an existing Tournament: save its original settings, obtain topics, update the
Tournament, find matching questions, then update its Quiz. B independently retrieves that
Tournament and completes. The successful source test supplies 2→3 questions and dates
12:05–13:05→12:25–13:25 at the fixed fixture clock. These are two operations on one
Tournament, not two Tournament creations.

The fault prevents the body of A's final `updateQuizStep` from running. Recovery restores
the original dates and question count. It also rebuilds embedded Tournament topics with
missing course IDs: the final changed top-level attribute is `tournamentTopics`. This
separate compensation defect accounts for I=1 in every fault history. The affected object
is the Tournament, not each embedded topic. B reading the transient revision accounts
for A=1 only in three histories; it does not cause the course-ID residue.

Below, “before/after” refers to the **persisted Tournament update/recovery**, not merely
the beginning/end of the whole Saga. Prefixes identify the exact labels in matrix.json.

| Run | B's observation | I | A observed | A coverage | I + 0.25A | I + A |
| --- | --- | ---: | ---: | --- | ---: | ---: |
| 00 control | Original revision | 0 | 0 | Complete within scope | 0 | 0 |
| 01 control | Original revision | 0 | 0 | Complete within scope | 0 | 0 |
| 02 control | Original revision | 0 | 0 | Complete within scope | 0 | 0 |
| 03 control | Updated revision; A later succeeds | 0 | 0 | Complete within scope | 0 | 0 |
| 04 control | Updated revision; A later succeeds | 0 | 0 | Complete within scope | 0 | 0 |
| 05 control | Updated revision after A succeeds | 0 | 0 | Complete within scope | 0 | 0 |
| 06 fault | Before update | 1 | 0 | Complete within scope | 1 | 1 |
| 07 fault | Before update | 1 | 0 | Complete within scope | 1 | 1 |
| 08 fault | Before update | 1 | 0 | Complete within scope | 1 | 1 |
| 09 fault | After update, before failure/recovery | 1 | 1 | Complete within scope | 1.25 | 2 |
| 10 fault | After update, before failure/recovery | 1 | 1 | Complete within scope | 1.25 | 2 |
| 11 fault | After failure, before recovery writes | 1 | 1 | Complete within scope | 1.25 | 2 |
| 12 fault | After settings recovery, before final lock rollback | 1 | 0 | Complete within scope | 1 | 1 |
| 13 fault | After both recovery actions | 1 | 0 | Complete within scope | 1 | 1 |

The five forward steps of A admit six B placements. A final-step fault additionally
allows B before, between or after the two recovery actions at the last placement:
six controls plus eight fault histories, not 14 different application defects. Rows
09/10 differ by whether A has already found questions; row 11 reads after the injected
failure. Rows 12/13 deliberately preserve the real semantic-lock rollback distinction.

I alone ties all eight faults. Either illustrated positive anomaly weight, or sorting
first by I and then by A, places 09/10/11 ahead of the other five. **This data cannot
choose a relative weight:** I is constant and A is only 0 or 1. These are selected
histories, not an anomaly frequency estimate or evidence of improved GA discovery.
Forward placements belong to different fixed WorkloadPlans; the current within-workload
search does not mutate between all six placements. The last workload does contain the
three recovery alternatives 11/12/13 and can expose the A=1 versus A=0 distinction.

## Candidate policies: preferences, not measured severity

| Candidate | Meaning | Main trade-off |
| --- | --- | --- |
| I + λA | A second kind of evidence can outweigh an object-count difference | Simple scalar; λ declares an exchange rate between unlike units |
| Sort by I, break ties with A | Persistent-object evidence always takes precedence | Simple, but any anomaly count loses to one extra affected object |
| Keep both objectives without a scalar | Preserve alternatives when one has more I and the other more A | Avoids weights, but search selection needs a rule for incomparable cases |

I-only and A-only are useful experimental comparisons to understand each contribution.
They do not settle the final combined objective. Capping A to “any exposure” is another
simple option: it avoids rewarding many similar readers, but treats one and ten distinct
exposures equally. There is no measured need to adopt that cap now.

**Hypothetical trade-off, not a reproduced pair:** X=(I=2,A=0), Y=(I=1,A=3).
I+0.25A prefers X (2 versus 1.75); I+A prefers Y (2 versus 4). They tie at λ=1/3.
Impact-first prefers X. With two objectives, neither dominates the other. This is the
meeting decision that the real eight-fault sample cannot answer.

Adding I and A may reward two descriptions of the same incident. That is not necessarily
wrong for prioritization, but the total must not be called a count of independent harms.
Preserve the findings so the overlap can be inspected. Larger workloads also create more
opportunities for both counts; these examples do not authorize global cross-workload
ranking or justify a normalization denominator. Compare like horizons and declared scopes
first. Missing evidence remains missing, not zero; a positive lower bound is not complete.

## Other real stories that must not be forced into the formula

The historical controlled creation/read-only FindQuiz case proves A=1 with a successful
reader and zero reader writes. However, its retained ImpactV2 assessment is **PARTIAL**,
I=null, observed object lower bound 0, due to `FAILED_SAGA_RECOVERY_INCOMPLETE`. This is
not a measured (I=0,A=1) example. The exact reports and reason are retained in matrix.json;
no current-policy reassessment or fresh execution was performed here.

The [event-order experiment](../event-order-rejections-2026-09-08/README.md) gives another
contrast: adding the Tournament creator as participant succeeds after the name-update
event arrives, but is rejected if enrolment precedes that delivery. All eight assessments
of that experiment have complete I=0. The paired observation is useful, but it is not
this read-exposure diagnostic; its A is not supplied here and must not be fabricated as
zero or one. The removed-student control rejects the name update in both placements.
Neither rejection count nor the pair-level result is silently added to the proposed sum.

## Next decision and proof

Discuss whether repeated independent read exposures should eventually outweigh an extra
affected object, whether read-only successful exposure should contribute to search priority,
and how partial measurements should be handled. Keep the answer separate from claims of
business harm. Then implement the smallest agreed combination in reporting/search and
compare it with the single-component experimental arms under equal execution budgets.
New anomaly families and GA representation work can proceed separately; no weight choice
is needed to investigate whether a compensation overwrites another successful update.

Validation for this document: all 14 triplets of execution/impact/read reports agree on
attempt and scenario identity; all reported counts and coverage match runtime validation;
all 14 fully covered rows and their illustrative arithmetic were checked. The historical
creation-case null score is preserved. The scope-order correction changes observation
classification; the impact checks, read-exposure predicate and search policy are unchanged.
