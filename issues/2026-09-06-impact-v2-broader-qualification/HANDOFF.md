# Broader impact qualification

> Subsequent correction: semantic-lock analysis and the executor completion check were
> incomplete during this campaign. The 34-to-17 projection does not establish equivalent
> recovery; some omitted read-payload checkpoints had real semantic-state rollback.
> Preserve the reports as observations, with fresh qualification in
> `../2026-09-06-semantic-lock-recovery/HANDOFF.md` governing the corrected behavior.

State: complete. Independent review passed; see `REVIEW.md`.

The campaign ran 77 ordinary ScenarioExecutor attempts in fresh Docker/JVM/H2 environments,
using one frozen build and at most two simultaneous attempts. It changed qualification
scripts and documentation, not ImpactV2 semantics or application behavior.

## The historical 34-case question

Today's generator produces 17 schedules for the same 12 fault combinations. Removing
only the historical `CONSERVATIVE_UNKNOWN` recovery actions maps all 34 old schedules
onto those 17, with a consistent old label within each group. Four schedules match
exactly; the others match through that projection. A projection is not an exact replay
or proof that removed runtime actions were behaviorally irrelevant.

| Historical group | Current counterparts | New generic observations |
| --- | ---: | --- |
| 19 `HARMFUL_FOR_RULE` rows | 8 | Five complete scores of 2; three partial assessments with an observed lower bound of 1 |
| 15 `NO_BROKEN_REFERENCE` rows | 9 | Seven complete scores of 0; two partial assessments with lower bound 0 |

All eight counterparts of the old flagged cases now have positive generic findings.
They detect a surviving Tournament dependency on a deleted Quiz. In the five complete
cases the deleted Quiz itself is also a failed-operation residual, giving two distinct
affected objects. Partial cases cannot claim a complete score.

A cross-check on the new raw final snapshots finds the same broken-reference pattern
in eight current schedules. Weighting those groups by their old multiplicities recovers
19/15. This is a comparison using the same captured evidence, not an independent oracle,
and neither the old labels nor the application-specific predicate enters scoring.

The newer application benchmark wrapper rejected the original provider-backed setup
before execution because it now requires the later automatic setup. Those 17 rejected
wrapper invocations are retained separately. The authoritative 17 attempts used the
ordinary executor; no independent new application benchmark verdict is claimed.

## The broader sample

We froze 30 control/final-slot-fault pairs before seeing outcomes: one workload per
eligible Saga type plus four event-bearing shapes. Selection covers 26 of 68 discovered
Saga types. It tests setup-eligible workloads; it does not predeclare them executable.

| Cohort | Reports | Complete | Partial | Invalid | Unavailable |
| --- | ---: | ---: | ---: | ---: | ---: |
| Historical family | 17 | 12 | 5 | 0 | 0 |
| Broader sample | 60 | 47 | 4 | 9 | 0 |

Among the 30 broader pairs, 21 have two complete assessments; 17 also have a healthy,
exact control. Three of those 17 demonstrate positive findings against zero controls:

- **CreateQuestion: 0 → 1.** Creation commits, a later step fails, and recovery leaves
  the new Question active.
- **RemoveCourseExecution: 0 → 1.** The Course count is reduced, removal later fails,
  and recovery leaves the count reduced while the CourseExecution survives.
- **RemoveTournament: 0 → 2.** The Quiz remains deleted while its Tournament survives.

The other 14 qualified pairs are zero/zero under the three implemented checks. The full
60-attempt distribution is 39 complete zeros, seven complete ones and one complete two,
plus four partial and nine invalid assessments. These are attempt counts, not counts of
independent bugs. The detailed examples and exclusions are in `BROADER-DIAGNOSIS.md`.

Two quiz-solving controls already fail without an injected fault and score 1, as do
their fault counterparts. Recovery removes the active QuizAnswer but leaves a deleted
record that was absent initially. The current comparison counts that remnant. It may be
expected compensation storage, and does not demonstrate additional harm from injection.

## What comes next, in order

1. **Measurement coverage:** support the nested participant/answer back-reference.
   It caused all nine partial assessments across both cohorts. Keep the existing
   fail-closed behavior for unsupported cycles until generic tests and runtime controls
   justify handling this shape.
2. **One methodological choice:** decide whether newly created objects left only as
   deleted records should count. Preserve the raw evidence either way. This campaign
   deliberately did not change that policy or quietly subtract positive controls.
3. **Executable, healthy comparisons:** address UserDto/TopicDto argument materialization
   (six invalid attempts), prepare the missing event receivers (three invalid controls),
   and choose healthy fixtures for duplicate acronyms, missing Quiz IDs and unenrolled
   solving users. These are separate from observer coverage.
4. **Application fixes separately:** restore the Course counter and remove the newly
   created Question on their failed paths, with application tests. Earlier topic-course-ID
   restoration and event-consumer persistence defects remain separately recorded.

No successful broader event-delivery pair was established: the selected event controls
were invalid, and their paired faults prevented those events. The earlier focused event
fixture remains the positive/repair proof for that category. This campaign also does
not exhaust inputs, fault slots, schedules or concurrent combinations.

## Proof and reproduction

- Raw/aggregate evidence: `verifiers/target/impact-v2-broader/run-01/`.
- Current package: `verifiers/target/impact-v2-broader/generated/quizzes-20260906-021039-016/`.
  Manifest SHA-256: `cd99c268cbba94f13e51b2f8845020957b9527e6ebef0c650eb451680342b436`.
- The six missing canonical vectors were persisted through the production request CLI.
  Package hashes remained unchanged throughout execution.
- Root validation passed for all 77 unique attempt IDs and 231 execution/V1/V2 report
  files; `run-01/validation.json` retains the report hashes. Seven focused qualification
  checks passed (five issue-local checks and two historical-selector/missing-report
  checks). Shell syntax, documentation build and `git diff --check` passed.
- Strict report joins, category/distinct-object counts, nullable-score semantics and
  control pairing are checked by the experiment validators. Missing reports are kept
  as campaign `MISSING_REPORTS` rows, never fabricated ImpactV2 statuses or zeros.
- Root checked 1,013 runtime source/configuration files against the prebuild manifest in
  both the host and compiled source copies: no mismatches. Postprocessing scripts and
  generated Python bytecode are excluded from that runtime-source comparison.
- The primary checkout's 77 inherited dirty/untracked files still match their saved
  hashes. All work remains in `microservices-simulator-impact-v2`, branch
  `codex/potential-impact-v2`; no commit, merge or push occurred.
- Reproduction commands and scripts: `verifiers/experiments/impact-v2-broader/README.md`.
  Start with `historical/results.csv` for the 17 schedules and `broader/results.csv` for
  the 60-attempt table; open the corresponding `execution.impact-v2.json` for raw facts.

Changed surfaces are the new experiment directory, this issue package, and the canonical
`docs/verifiers-impl/current-state.md` / `roadmap.md`. Existing source changes from the
prior ImpactV2 implementation were preserved. Full Java suites were not rerun because
this campaign changed no Java behavior; its actual Docker executions provide new runtime
qualification rather than an inflated regression-test total.
