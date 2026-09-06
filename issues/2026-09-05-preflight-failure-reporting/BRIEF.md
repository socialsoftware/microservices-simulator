# Prepare and report verifier experiments correctly

Approved by the user in Thesis HQ after the concrete B1/B2 explanation.

## Assignment

The active implementer is the Sol medium subtask `/root/preflight_failure_reporting`,
working in `/Users/andre/meic/thesis/microservices-simulator-b-preflight` on
`codex/preflight-failure-reporting`. The app request for a separate task titled
"Prepare and report verifier experiments correctly" returned a queued creation and
has not provided a live task ID. If that task becomes available, first inspect this
assignment and the active implementer's handoff: coordinate review/adoption instead
of launching a duplicate implementation. No changes are to be merged into the primary
checkout automatically.

## Approved outcome

1. Preserve the actual cause from a valid expected worker failure report, rather than
   replacing every nonzero worker exit with an isolation-failure label. Validate identity,
   shape, expected exit/status combinations and relevant setup evidence. Keep crashes,
   timeouts, malformed/missing/mismatched reports and inconsistent results invalid.
2. Qualify a small justified selection of current generated workloads, using earlier
   proof where applicable. Verify exact preparation and identities, target exclusion,
   and pending-event separation. Distinguish setup readiness from target execution;
   negative-test inputs need not succeed as business operations.

This does not change impact semantics, normal target exception classification, package
schemas, supported setup methods or input recipes. No full 665-candidate scan, application
bug fixes, commits, pushes, merges, or production actions are authorized by this brief.

## Evidence and delivery

Use focused generic positive/negative tests and fresh Docker/H2 qualification where
runtime evidence is needed. Record the inherited dirty baseline and the incremental
source diff in the isolated handoff. Return a plain-language selected-case table,
reproduction paths, checks and remaining limits. Canonical-doc changes in the worktree
must be narrow because the parent is updating impact documentation in the primary checkout.
