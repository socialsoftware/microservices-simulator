# Source-derived ordered setup and shared attempt-local results

Date: 2026-08-28

Status: active and implemented

## Context

Ordinary application tests can create one object and pass its returned identity to multiple target Sagas. Participant-local input recipes cannot express that one setup call must execute once, that required void effects must remain ordered, or that both participants consume the same fresh result. Copying call recipes would duplicate effects and could falsely claim shared identity. The existing v4 prerequisite provider can construct such state manually, but it makes the application restate setup already present in its tests.

## Decision

Package v5 gives WorkloadPlan one optional validated `SetupPlan`:

- static analysis flattens only the approved straight-line fixture/helper shapes into source-ordered setup actions;
- each action names one Java-confirmed application facade method key and uses the closed persisted value language;
- separate source occurrences remain separate actions even when their text or arguments match;
- later actions and participant arguments may reference only earlier retained results or the approved `aggregateId`/`courseAggregateId` properties;
- one validator owns action order, method/type/value compatibility, reference direction, and absence of runtime values;
- runtime authorization is independent: the application supplies a closed dispatch map with no arbitrary reflection fallback;
- setup runs once per attempt before target fault injection, retains results only in that attempt, clears setup-created pending events, and proves an empty baseline before target startup;
- source-setup preflight isolates each candidate in a bounded fresh JVM/Spring/H2 worker;
- SetupPlan semantics participate in deterministic workload identity, but runtime IDs and returned values do not.

Valid v4 packages continue through the unchanged prerequisite-provider path. A WorkloadPlan cannot mix a prerequisite baseline and a SetupPlan.

## Why this contract

- Shared identity follows one exact producer occurrence instead of names, equal-looking calls, or coincidentally equal runtime IDs.
- Required activation and enrollment effects are not lost merely because they return void.
- The persisted package remains deterministic and application-independent while runtime method authority remains application-owned.
- Setup failure cannot become an evaluated zero-impact target attempt.
- Existing scheduling, cap, fault, compensation, recovery, and ImpactV1 semantics remain unchanged.

## Consequences

- The latest manifest and WorkloadPlan schemas are v5; SetupPlan is v1. Valid v4 packages remain readable/executable, while v3 stays unsupported.
- Supported extraction is deliberately narrow, not a Java/Groovy interpreter.
- Source-setup preflight costs one fresh process per candidate; ordinary selected execution already owns one fresh process per attempt.
- The historical Remove/Add descriptor/provider remains retained evidence, but the automatic ordinary-test path does not invoke it.
- Generic final-state impact, search/reward, cross-workload allocation, TCC, remote execution, and broader fixture languages remain separate work.

## Revisit when

- another representative ordinary test requires a genuinely new setup value/control-flow shape;
- the supported runtime needs an isolation boundary other than fresh process/H2;
- a later package contract supersedes SetupPlan while preserving exact source occurrence and shared-result semantics.

Current behavior and harmful/control evidence are in [`../current-state.md`](../current-state.md#automatic-source-derived-removetournamentaddparticipant-proof).
