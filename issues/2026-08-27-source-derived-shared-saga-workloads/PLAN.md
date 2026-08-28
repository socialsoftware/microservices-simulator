# Plan: source-derived shared Saga workloads

## Status and execution rule

This plan replaces the rejected first M1 spike. The checkout has been restored to committed `HEAD` `57910ab80` plus the pre-existing roadmap and planning files.

No implementation begins until the user approves this revised plan. A future implementer must stop after each checkpoint below. It must not continue automatically into the next checkpoint or repair a failed review in the same long-running session.

## The complete path

```text
A. Prove the source relationship
   one createTournament result -> both Saga Tournament arguments

B. Persist the ordinary setup
   twelve source-ordered facade calls -> one SetupPlan

C. Execute it in fresh state
   one fresh Tournament -> same fresh ID given to both Sagas

D. Prove one harmful and one control result
   no Remove/Add prerequisite descriptor/provider
```

Each checkpoint answers:

1. What worked before?
2. What works now?
3. What does the Remove/Add example mean in plain language?
4. What exact test/evidence proves it?
5. Which files and concepts changed?
6. What remains for the next checkpoint?

## Rules that apply to every checkpoint

- Do not modify `RemoveTournamentAddParticipantRecoveryWindowExploratoryTest.groovy`.
- Do not edit the final-state impact issue while this feature is active.
- Do not start M2/runtime work during a static checkpoint.
- Do not change `ScheduleEnumerator` semantics or any existing tuple/workload ordering to make the target survive a cap.
- A cap means “take the first N under the existing deterministic order.” If N omits the target, increase N for the acceptance run or report the omission.
- Do not add a global reservation pass for source-supported candidates.
- Do not build a generic statement evaluator, call evaluator, or reflection fallback.
- Use one setup validation authority. Do not duplicate setup checks in readers, WorkloadPlan validation, and readiness.
- Existing v4 package reading/execution must remain explicit and unchanged.
- Preserve weak type-only candidates and their uncertainty.
- No staging, commit, push, merge, rebase, reset, clean, or history operation is authorized by this plan.

## Checkpoint A — Recover only the source relationship

### Plain-language outcome

The analyser can show:

```text
RemoveTournament argument 1
        \
         -> tournamentDto.aggregateId
        /
AddParticipant argument 1

and tournamentDto came from one exact createTournament call
```

It also recovers both Saga constructors from their map-returning helpers.

### Allowed production changes

Only the narrow analysis path needed for:

- Java command aggregate key -> Saga constructor argument position;
- one unambiguous Saga constructor inside the supported returned-map shape;
- caller-expanded source occurrence identity;
- participant argument -> producer occurrence/property evidence.

Do not add SetupPlan/package/runtime/scheduler changes in this checkpoint.

### Implementation approach

1. Extend Java creation/command evidence with the constructor argument index used as the aggregate key.
2. Extend the existing Groovy constructor resolver only for a map containing exactly one resolvable Saga constructor.
3. Give each expanded producer call an identity based on the real caller occurrence, so repeated helper calls remain distinct.
4. Attach producer occurrence and property path to the relevant participant argument.
5. Join Remove/Add only as source/test-supported when both constructor positions point to the same producer occurrence and property.
6. Leave type-only candidates unchanged.

### Proof

Focused tests must show:

- both target Saga inputs are recovered from the unmodified test;
- both command footprints identify constructor argument 1 for Tournament;
- both input argument-1 records point to one exact `createTournament` occurrence and `aggregateId`;
- two equal-looking but separate helper calls do not share identity;
- an ambiguous map or helper cycle remains blocked;
- existing relevant analysis tests pass.

### Mandatory pause

Return the plain-language evidence above. Do not add setup models until the user approves Checkpoint B.

## Checkpoint B — Persist the ordered setup and generate the WorkloadPlan

### Plain-language outcome

The package contains the setup already present in the ordinary test:

```text
1. create course execution
2. create creator
3. activate creator
4. create participant
5. activate participant
6. enroll creator
7. enroll participant
8. create topic 1
9. create topic 2
10. create question 1
11. create question 2
12. create Tournament
```

Later actions reuse earlier returned objects. The target Sagas both reference the result of action 12.

### Allowed production changes

- a compact optional SetupPlan on the latest WorkloadPlan;
- setup actions and typed earlier-result/property references;
- latest package schema, identity, checksums, accounting, reading, writing, and one validation authority;
- adapter/generation changes needed to attach the plan;
- explicit unchanged v4 package path.

Do not change ScenarioExecutor runtime behaviour, scheduling semantics, or global candidate ordering.

### Required representation

Use the smallest representation that expresses the actual fixture:

- literal values;
- the existing replayable DTO constructor and ordered property assignments;
- list/set values needed by questions and Tournament topics;
- the existing local date-to-string conversion used by the helpers;
- an earlier action result;
- `aggregateId` or `courseAggregateId` of an earlier result.

Unknown calls, arbitrary methods, unresolved values, unsupported properties, and control flow are blockers. Do not copy and reinterpret every possible `InputRecipeNode` shape merely because the generic model can represent it.

A SetupAction carries one Java-analysis creation-site/method key. Persisted method metadata is evidence; M2 runtime authorization remains an independent closed dispatch check.

### Implementation approach

1. Flatten supported setup/helper facade calls into source order, including void activation/enrollment effects.
2. Map action arguments directly to their final persisted value recipes. Avoid a second broad recursive “map then rewrite every node” pass.
3. Replace references to known earlier facade results during mapping, not through a generic post-processing language.
4. Validate setup in one place. Package readers, WorkloadPlan validation, and readiness call that validator.
5. Include setup semantics in the latest WorkloadPlan/package identity and hashes; include no runtime values.
6. Keep the existing scheduler untouched.
7. For the focused acceptance generation, use a deterministic forward-order cap sufficient to include the ten possible Remove/Add forward orders. Select the required serial WorkloadPlan from the generated output.
8. For normal whole-Quizzes generation, use explicit finite caps large enough to include the target under existing ordering. Record all cap counts. If the target is omitted, report it rather than adding reservation logic.

### Proof

Focused tests and one fresh normal generation must show:

- the exact ordered setup actions above, including four void effects;
- later arguments reference only earlier action results;
- the two Tournament arguments reference action 12 and `aggregateId`;
- `AddParticipant` also references the matching fresh course and participant producers;
- malformed/unsupported setup is blocked by the single validator;
- the target descriptor is excluded;
- a generated WorkloadPlan has `prerequisiteBaseline == null` and a valid SetupPlan;
- the required serial forward order appears under the stated cap;
- existing recovery generation produces a `00100` immediate-recovery FaultScenario;
- latest package round-trip/identity/checksum tests pass;
- saved valid v4 package reading and prerequisite binding still pass;
- package files contain no runtime database IDs.

### Mandatory pause

Present the generated action list, WorkloadPlan ID, FaultScenario ID, configured caps, cap counts, and package hashes. Do not begin runtime execution until the user approves Checkpoint C.

## Checkpoint C — Execute setup once and reuse fresh results

### Plain-language outcome

For each attempt:

```text
restore fresh state
-> run the twelve setup actions once
-> retain their returned objects for this attempt
-> give action 12's fresh Tournament ID to both Sagas
-> clear/settle pending setup events
-> begin the selected target FaultScenario
```

### Allowed production changes

Only the narrow latest-package setup runtime path and its tests/reports:

- ScenarioExecutor preflight/runtime orchestration;
- attempt-local setup result storage;
- exact known-method dispatch;
- setup status evidence;
- participant materialization from setup results.

Do not change fault, recovery, event-consequence, impact, or legacy provider semantics.

### Implementation approach

1. Validate the complete setup before invoking anything.
2. Resolve each action through an independent closed map of Java-confirmed facade method keys. No arbitrary reflection fallback.
3. Materialize only the value forms approved in Checkpoint B.
4. Invoke each action once in order and retain non-void results by action ID for that attempt.
5. Type-check direct results and supported property access.
6. Run the existing pending-event cleanup/settling boundary.
7. Materialize both target Sagas from the same retained Tournament result.
8. Start target fault injection only after setup succeeds.
9. On reset/setup/cleanup failure, stop before target startup and report `NOT_EVALUATED`.
10. Dispatch v4 Workloads through the unchanged legacy provider path.

### Proof

Tests must prove:

- actions execute exactly once and in order;
- two references to action 12 reuse one object rather than creating two Tournaments;
- void activation/enrollment effects occur;
- fresh attempts receive fresh runtime IDs;
- no runtime result enters package identity or files;
- invalid action keys, properties, types, order, null results, and mixed provider/setup configurations fail before target startup;
- setup is outside target fault injection;
- setup events do not leak into measured execution;
- existing v4 provider execution still works.

### Mandatory pause

Report one readable setup execution trace and proof of shared fresh Tournament reuse. Do not run the harmful/control benchmark until the user approves Checkpoint D.

## Checkpoint D — Prove harmful and control executions

### Plain-language outcome

The automatic path replaces the manual Remove/Add setup for two fresh proof runs.

### Implementation approach

1. Generate a fresh latest package with dynamic enrichment disabled and the target descriptor excluded.
2. Select the automatic WorkloadPlan and its `00100` immediate-recovery FaultScenario.
3. Record the five package hashes.
4. Run one fresh harmful attempt.
5. Run one fresh nearby control attempt.
6. Verify the hashes are unchanged.
7. Use the existing bounded Quizzes broken-reference observer only for proof; do not build the later generic impact feature here.

### Proof

- Harmful: active Tournament, deleted referenced Quiz, broken Tournament-to-Quiz reference.
- Control: evaluated execution without that broken reference.
- Both use `prerequisiteBaseline == null` and the extracted SetupPlan.
- Remove/Add prerequisite-provider invocation is absent.
- Both prove one fresh Tournament result supplied both Saga Tournament arguments.
- Package hashes are identical before and after execution.
- Focused verifier tests, affected Quizzes tests, and the appropriate broader verifier suite pass.

## Review and recovery policy

- A reviewer inspects the actual diff and evidence after each checkpoint.
- A failed review stops the checkpoint. It does not automatically authorize a long repair loop.
- Reviewer findings are grouped and explained to the user in plain language before more implementation.
- Fixes occur in a fresh bounded session only after the desired correction is clear.
- The rejected first spike remains an external read-only archive; it is not reapplied wholesale.

## Documentation after successful Checkpoint D

Only after the end-to-end proof:

- update `docs/verifiers-impl/current-state.md`;
- update both roadmaps with stable outcome-level facts;
- add one short ADR for the final ordered-setup design;
- retain the old descriptor/provider and historical evidence;
- state clearly that generic final-state impact, GA, reward, output cleanup, and cross-workload allocation remain later work.
