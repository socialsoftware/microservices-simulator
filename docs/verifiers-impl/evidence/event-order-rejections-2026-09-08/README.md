# Rejection dependent on event placement

The matched Tournament pair produced the expected contrast twice: AddParticipant
succeeded when the name-update event reached Tournament before enrolment, and was
rejected by invariants when that same delivery followed enrolment. The removed-student
negative control rejected its name update in both placements. All eight ImpactV2
assessments were complete and zero. This is a separate operation-outcome observation,
not a new persistent-object score or automatic business-harm verdict.

## What was executed

Every arrow is an existing scheduled action. Q denotes the selected QuizAnswer route,
which has no eligible receiver in these fixtures. T denotes the Tournament route.

| Pair/version | Exact normal action order | Target outcome, both repetitions | ImpactV2 |
| --- | --- | --- | --- |
| Tournament, early T | Read UC → update name → read student → T → add participant → Q | AddParticipant succeeds | 0 |
| Tournament, late T | Read UC → update name → read student → add participant → T → Q | AddParticipant rejected by invariants | 0 |
| Removed student, early first event | Read UC for removal → remove student → first removal-event attempt → update name → second removal-event attempt | Name update rejected: student not enrolled | 0 |
| Removed student, late first event | Read UC for removal → remove student → update name → first removal-event attempt → second removal-event attempt | Same name-update rejection | 0 |

The same extra UC read exists in both Tournament versions. Within each pair, the
participants, input variants, source setup, forward order, two selected routes and all
other action positions relative to each other are identical. Only e1 crosses the target
step. There are no injected faults. The four workload/scenario records already existed
in the retained catalogue; no package edit, new scenario identity, fixture provider or
application mutation was used.

The first pass launched Tournament late/early, then removed-student late/early. The
second pass reversed that order. All eight attempts used separate Docker/JVM/H2 instances,
serially, and took 130.6 seconds combined. No retries, process errors, setup failures,
timeouts or incomplete assessments occurred. Two executions ended SUCCESS/EXACT; six
ended PARTIAL_COMPENSATED/DEVIATED with their explicit target application rejection.

## What the evidence establishes

The Tournament fixture's creator is also the prospective participant. The UC's student
copy is updated to `UpdatedName`, and AddParticipant reads that value. If the event has
not reached Tournament, its creator copy still has `USER_NAME_1`. The code's
creator/participant consistency invariant forbids differing copies for that person.
The recorded invariant exception occurs at AddParticipant; the general exception text
does not identify an individual invariant. Code and observed values explain the concrete
mismatch, while the reusable detector does not infer that business rule.

Early delivery updates the creator before enrolment; final creator and participant names
are `UpdatedName`. Late delivery updates the creator after rejection; final creator name
is `UpdatedName`, but the participant collection remains empty. The rejected enrolment
was not automatically retried. The final states therefore differ in a user-visible way,
although neither triggers the current three persistent-state checks.

For the negative control, removing the student deletes their entry from the UC's
students collection, not the global User account. The later name update requires that
entry and fails before registering a change. Both final enrolment collections are empty.
Moving the removal-event attempt changes neither this prerequisite nor the rejection.
The tool reports the two failures without classifying that expected rejection as impact.

## Comparison contract and limits

The small comparison emits `ORDER_DEPENDENT_REJECTION` for the Tournament pair and
`REJECTED_IN_BOTH` for the negative control, consistently in both repetitions. A positive
requires actual deliveries, joined source-setup receiver/publisher roles, the same target
operation, and an invariant rejection on that receiver in the failed run. Missing controls,
other execution failures or a generic runtime exception without that invariant evidence
cannot earn a positive verdict. This initial contract covers invariant-based rejections;
it does not claim to classify every application exception or infrastructure failure.

Baseline application projections and lifecycle states matched within each pair after
excluding eight explicitly retained runtime timestamp fields. All four Tournament
attempts to add a participant occurred more than four minutes before Tournament start;
late enrolment is therefore not the observed cause. This is a controlled local experiment
with relative-date recipes, not identical physical time or a universal prerequisite oracle.

The result demonstrates a placement-dependent operation outcome in these fixtures. It
does not establish universal operation legitimacy, serializability, a named concurrency
anomaly, or unacceptable business harm. It is not ordinary-executor functionality or a
new search fitness component. The step after this qualification is deciding how to expose
and select such comparisons, not expanding ImpactV2 silently.

## Reproduction and proof

- [Selection](selection.json): exact existing workload/scenario IDs, all four schedules,
  package/build/image identities and frozen launch-code hashes.
- [Comparison](comparison.json): per-repetition verdicts, target exceptions, exact report
  hashes, publisher/receiver setup roles, event order and unchanged ImpactV2 scores.
- [Fixture checks](fixture-checks.json): retained times, enrolment timing margins and
  final participant/enrolment observations.
- [Proof](proof.json): raw artifact hashes, source/build integrity and comparison tools.
- [Tests](tests.txt): ten narrow classifier/contract regression checks passed.

Runner, comparison, Quizzes-specific fixture checks and commands are under
`verifiers/experiments/event-order-rejections/`. Raw logs and all execution/impact reports
remain in `verifiers/target/event-order-rejections/run-01/`. The original package and
qualified production build/dependencies were verified before and after execution.
The personal note and Portuguese meeting note were left unchanged for the planned review.
