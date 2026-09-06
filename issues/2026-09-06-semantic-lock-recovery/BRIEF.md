# Semantic-lock writes and complete recovery

The user approved the two-part correction discussed in conversation and requested a
simple implementation. Continue in the existing ImpactV2 worktree, preserving inherited
changes. This is a direct approved execution brief.

A SagaCommand can carry a read payload while its handler persists a semantic state.
The generator currently treats that wrapper as transparent and can omit both a write
interaction and its implicit recovery checkpoint. The assigned-fault executor can then
report COMPENSATED from planned actions alone despite pending framework rollback.

## Approved outcome

- Recognize framework semantic-state writes on the exact dispatched SagaCommand target,
  independently of enum names. Retain the payload access and account for the additional
  write in conflict/recovery analysis, including clearing to NOT_IN_SAGA. A plain read
  without a state request remains a read. Unknown wrapper configuration stays uncertain,
  rather than becoming confidently effect-free. Keep supported source analysis bounded.
- Before declaring a failed participant fully compensated, query the existing runtime
  recovery checkpoints. If work remains, report incomplete execution/recovery with an
  explicit reason and null ImpactV2 score. Do not execute unplanned recovery silently or
  alter application behavior. Preserve hard-stop propagation if discovery fails.
- Use existing pipeline boundaries, evidence types and report contracts where sufficient;
  introduce no enum-name interpretation, new impact category, generic program-analysis
  framework, or application-specific rule.

## Proof and documentation

Use dummyapp-first positive/negative Spock coverage for read-only, state-setting and
clearing wrappers, uncertain configurations, and conflict/recovery effects. Exercise the
executor guard against a deliberately incomplete plan and a fully recovered control.
Regenerate the RemoveTournament/AddParticipant benchmark and run its current canonical
fault scenarios from fresh JVM/H2 instances via Docker. Let the new schedule count emerge;
do not force 17 or 34. Verify actual lock-recovery sub-outcomes and, where needed, retained
runtime pending-work evidence, in addition to the existing impact reports. Preserve older
qualification artifacts with an explicit correction to their interpretation.

Root owns executor guard, integration, benchmark qualification and canonical docs. A
bounded agent may own static wrapper analysis and its tests. Independent review checks
the integrated change. Update current-state/roadmap and write a concise handoff. No
application bug fixes, impact-policy changes, commits, merges or pushes are authorized.
