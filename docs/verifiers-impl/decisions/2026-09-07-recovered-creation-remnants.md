# Recovered creations do not score for their deleted storage remnant alone

Accepted by the user on 7 September 2026; implemented as a direct refinement of the
ImpactV2 residual check. This decision amends FR-6 of the original ImpactV2 spec.

## Reason

The AddParticipant/SolveQuizAsync experiment creates a QuizAnswer and fails before
`solveQuizStep`. Recovery marks that new attempt DELETED. The final Tournament participant
has `quizAnswerAggregateId = null` and `answered = false`; the observed active aggregates
retain no dependency on that attempt. The old residual predicate nevertheless awarded a
point because baseline absence differed from a persisted deleted record.

That point describes normal logical-deletion storage without demonstrating a surviving
application effect. It should not reward search merely for exercising this compensation.

## Decision

After all existing completeness, recovery, sole-writer and tracked-final-state checks
pass, exclude the object from the residual findings when:

- it was absent from the observed baseline;
- its first observed write is ACTIVE in the failed Saga's FORWARD phase;
- its first observed DELETED write belongs to RECOVERY;
- its final tracked write remains DELETED in RECOVERY.

The candidate and raw evidence remain available. This is lifecycle equivalence for one
category, not proof of full correctness or compensation of every effect. It introduces
no Quizzes class/field rule and does not inspect arbitrary IDs to infer references.
A deletion first performed in FORWARD does not become a compensated creation simply
because a later recovery write leaves it deleted.

Preexisting objects left changed/deleted and new objects remaining ACTIVE/INACTIVE are
still assessed normally. `DELETED_DEPENDENCY` still counts any qualifying ACTIVE source
that depends on this deleted target. The separate read-exposure diagnostic can still
report a read of the compensated creation. Unknown evidence remains unknown.

## Evidence and compatibility

The JSON schema/shape is unchanged. Previously written reports keep their original
values; source revision and the explicitly labelled reassessment policy distinguish
old and new results. Do not reuse old expected scores as if produced by the new evaluator.
No data migration, application fix, observer change or deletion of history is involved.

[Reassessment](../evidence/recovered-creation-remnants-2026-09-07/comparison.json) applies
the new Java assessor to 188 retained observations, adding no application executions.
Ten values change 1 → 0, all space-map w07/w08 creation remnants; all other values,
coverage and the other categories are unchanged. Tests cover independent active
references, unremoved creations, preexisting changes, forward deletion, incomplete
recovery, competing writers and missing baseline evidence.
