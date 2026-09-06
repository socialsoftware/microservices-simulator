# M2 repair handoff — direct facade DTO mutation snapshots

## Defect and fix

Docker showed that the feature-local `createUser(newUserDto)` action for
`AddParticipantAndCreateTournamentTest#create add participant successfully` serialized
`newUserDto` as a bare constructor. The preceding `setName`, `setUsername`, and `setRole`
mutations were missing, so runtime setup failed with `User requires a name`.

The direct `traceFacadeCall` path resolved facade arguments while
`activeMutationScopes` was `Map.of()`. It now receives the caller method's
`methodMutationScopes`, installs that snapshot only while resolving the facade call, and
restores the prior scope in `finally`. Both bare direct calls and assignment/self-rebinding
calls use the same bounded path. Existing assignment handling clears a rebound variable's
old mutations after the call, while already-created immutable recipes prevent later
mutations from leaking backward.

No runtime dispatcher, property authority, persisted schema, or shared documentation was
changed.

## Proof

- Dummyapp self-rebinding now starts from `new ItemDto()`, applies a setter and property
  assignment plus the original `orderId=81` setter before
  `itemDto = itemFunctionalities.createItem(itemDto)`, then sets `orderId=999` after the
  call. The traced facade argument contains exactly the three pre-call mutations in order,
  retains `81`, and excludes `999`.
- The Quizzes regression now asserts that the feature-local `createUser` SetupAction
  contains `name=NewUser`, `username=NewUsername`, and `role=STUDENT` in source order.
- `GroovyConstructorInputTraceVisitorDummyappSpec`: pass.
- `SourceDerivedSharedSagaWorkloadAnalysisSpec`: pass.
- `mvn -q -DskipTests compile`: pass.
- `git diff --check`: pass.

The coordinator owns combined full-suite, package determinism, and Docker reruns. Known
closed-method-authority failures for StartQuiz/LeaveTournament remain outside this repair.

## Incremental application

The exact four-file diff from the previously handed-off M2 tree is saved as
`M2-DIRECT-MUTATION-REPAIR.patch` in Sol's isolated issue directory at
`/Users/andre/.codex/worktrees/7159/microservices-simulator/issues/2026-09-04-complete-source-derived-setup/`.
It was produced against a temporary Git-index snapshot
taken before this repair. `git apply --check --reverse` succeeds in this repaired
worktree, confirming the patch matches the applied delta.

Astra subsequently reviewed the four-file delta and applied it to the primary checkout.
The final qualification handoff records combined proof after that integration.
