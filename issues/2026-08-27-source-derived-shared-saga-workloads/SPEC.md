# Source-derived shared Saga workloads

## Status

Replanned after rejecting the first M1 implementation spike. No implementation from that spike remains in the checkout. This document defines the behaviour we want before another implementation begins.

## The problem in one example

The ordinary Quizzes recovery-window test creates one Tournament and then constructs two Sagas with its ID:

```text
setup creates one Tournament
-> tournamentDto.aggregateId goes to RemoveTournament argument 1
-> the same tournamentDto.aggregateId goes to AddParticipant argument 1
```

The verifier currently loses that relationship. It can notice that both Sagas access the `Tournament` type, but it cannot prove that these two inputs refer to one particular Tournament. It also cannot replay the ordinary test setup. The existing executable benchmark solves both problems manually with a prerequisite descriptor and provider.

This feature replaces that manual path for this example:

```text
ordinary source and test
-> identify the two Saga inputs and their shared Tournament producer
-> extract the required setup in source order
-> execute that setup once in fresh state
-> give the same fresh Tournament ID to both Sagas
-> run a harmful scenario and a nearby control
```

## What success means

A normal Quizzes generation run, with the Remove/Add prerequisite descriptor excluded, produces a WorkloadPlan that:

- uses `RemoveTournament` and `AddParticipant` inputs recovered from the unmodified ordinary recovery-window test;
- contains the setup needed to create the course execution, users, topics, questions, and one Tournament;
- has no `PrerequisiteBaseline` and does not need the Remove/Add prerequisite provider;
- gives both target Sagas the ID of that one freshly created Tournament;
- can execute the known harmful `00100` case and a nearby control in fresh attempts.

The complete historical 19/15 landscape belongs to the later impact feature, not this feature.

## Terms used in this document

- **Source occurrence:** one particular call site after following the test's supported helper calls. Two calls remain different even when their text and arguments look equal.
- **Setup action:** one supported application-facade call that must run before the target Sagas, such as creating or activating a user.
- **Setup result reference:** a reference to the object returned by an earlier setup action, optionally followed by a supported property such as `aggregateId`. It never contains a runtime database ID.
- **Forward order:** the order of the five normal Remove/Add steps before faults and recovery are added.
- **FaultScenario:** one selected forward order plus a fault assignment and an exact forward/recovery action order.

## Explicit assumptions and decisions

These are part of the contract. An implementation must not silently choose different behaviour.

### Ordinary source remains authoritative

- The recovery-window test is not modified or annotated.
- Its setup is not copied into another application-specific verifier file.
- Application developers continue writing ordinary application code and tests.

### Shared identity comes from one producer

- Both Saga arguments are considered the same object only when they refer to the same particular producer result and property.
- Variable names, helper names, equal arguments, equal-looking calls, test ownership, or equal runtime numeric IDs do not prove shared identity.
- Separate source calls remain separate setup actions.

### Caps only truncate existing deterministic order

- This feature does not change how input tuples, workloads, or forward orders are enumerated.
- If a configured cap is `N`, generation retains the first `N` items under the existing deterministic enumeration rule.
- It does not reserve, protect, reorder, or substitute particular candidates or forward orders to make this benchmark pass.
- Remove's three steps and Add's two steps have ten valid dependency-preserving forward orders. The acceptance run uses a cap large enough to include the required serial order, or reports honestly that its selected cap omitted it.
- Source/test support is recorded as evidence. It does not introduce a hidden generation order in this feature.

### Supported extraction is deliberately narrow

- Setup extraction follows the exact straight-line fixture/helper shapes used by this ordinary test.
- Supported values are the existing replayable literal, DTO-construction/property-assignment, collection, local date-conversion, and earlier-result/property forms needed by the fixture.
- Conditional, looping, switching, exception-controlled, cyclic, ambiguous, unresolved, arbitrary-service, network, file, environment, or secret-reading setup is blocked.
- Encountering another shape is a reason to stop and show the source expression. It is not permission to build a general Java or Groovy interpreter.

### Setup runs once per attempt

- Setup actions execute once, in source order, in fresh attempt-local state.
- Returned objects live only in an attempt-local result map.
- Later setup actions and target Saga inputs may read those retained results or supported properties.
- Setup runs before target fault injection. Setup itself is not faulted.
- The existing pending-event cleanup boundary must succeed before measured target actions begin.

### Method execution is closed

- A setup action must correspond to one exact facade method that Java analysis already maps to a known Saga creation site.
- The package requests that exact method key; it does not authorize itself.
- Runtime execution uses an independent closed dispatch boundary for those known methods and has no arbitrary-reflection fallback.

### Compatibility and evidence

- Existing valid v4 packages remain readable and executable without migration or rewriting.
- Latest packages include setup semantics in deterministic identity and checksums, but never runtime result values.
- Dynamic evidence is optional confirmation and does not define package identity.
- Failed setup, reset, cleanup, settling, target execution, or impact evaluation is `NOT_EVALUATED`, never impact zero.
- Raw impact findings remain separate from future search reward.

## Required behaviour

### 1. Recover the target Saga inputs

1. Java analysis records which constructor argument supplies the aggregate key used by each Saga command.
2. Groovy analysis recovers the target Saga constructor when the supported helper returns a map containing one unambiguous Saga.
3. Each recovered Saga argument retains its exact producer occurrence and property path.
4. The Remove/Add pair receives source/test-supported evidence only when both Tournament arguments point to the same `createTournament` occurrence and its `aggregateId`.
5. Weak type-only Remove/Add candidates remain visible and are not upgraded without this evidence.

### 2. Recover and persist ordered setup

1. The supported fixture calls are flattened into one ordered setup plan.
2. Required void effects, including user activation and course enrollment, remain in that plan.
3. Each action records its source occurrence, exact known facade method key, ordered arguments, declared return type or void, and blockers.
4. Later actions can reference only earlier results.
5. Participant arguments can reference setup results after all setup actions.
6. One validation authority checks action order, method keys, allowed value forms, result direction, property/type compatibility, and absence of runtime values. Other readers and readiness checks call that authority rather than reimplementing it.

### 3. Generate the automatic WorkloadPlan

With the target manual descriptor excluded, normal generation must emit at least one Remove/Add WorkloadPlan from the ordinary test with:

- `prerequisiteBaseline == null`;
- a non-empty valid setup plan;
- both target argument-1 values referencing one Tournament-producing action;
- fresh course and participant references for `AddParticipant`;
- the serial forward order `getTournamentStep`, `removeQuizStep`, `removeTournamentStep`, `getUserStep`, `addParticipantStep` when the configured deterministic cap includes it;
- a reachable `00100` FaultScenario with immediate Remove recovery before Add.

The implementation must obtain this by ordinary deterministic enumeration under a sufficient cap, not by pair-specific code or changed scheduling semantics.

### 4. Execute setup and targets

1. ScenarioExecutor validates the latest package before invoking setup.
2. It restores fresh state, executes setup once in order, retains returned objects for that attempt, and resolves later references with type checks.
3. Any setup failure stops before target startup and produces `NOT_EVALUATED` evidence.
4. After cleanup/settling succeeds, the existing target materialization, fault, recovery, event, and report paths run unchanged.
5. The legacy v4 prerequisite-provider path remains unchanged for v4 packages.

### 5. Prove harmful and control behaviour

Two fresh runs must prove:

- **Harmful:** the automatic `00100` immediate-recovery scenario leaves an active Tournament referring to its deleted Quiz.
- **Control:** a nearby executable scenario does not leave that broken reference.
- Both runs use the automatic WorkloadPlan and never invoke the Remove/Add prerequisite provider.
- The five package files have identical hashes before and after preflight/execution.

## Non-goals

- A general Java/Groovy evaluator or reflection API.
- Support for every test/helper/control-flow shape.
- New scheduling, fault, compensation, recovery, event, or impact semantics.
- A new candidate-priority policy hidden inside generation caps.
- Complete 19/15 impact parity.
- GA, reward, repeated-run output, cross-workload allocation, TCC, or remote execution.
- Removing the historical descriptor/provider or old evidence.

## Stop conditions

Stop and return to the user if any of these becomes necessary:

- modifying the ordinary recovery-window test;
- changing existing deterministic enumeration or cap semantics;
- executing a call not independently confirmed as a known Saga facade method;
- supporting arbitrary statements, methods, reflection, IO, or network access;
- breaking valid v4 package reading or execution;
- materially expanding beyond the exact fixture shapes described above.
