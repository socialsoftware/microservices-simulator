# Source-derived ordered setup and shared attempt-local results

Date: 2026-08-28

Status: active and implemented

## Context

Ordinary application tests can create one object and pass its returned identity to multiple target Sagas. Participant-local input recipes cannot express that one setup call must execute once, that required void effects must remain ordered, or that both participants consume the same fresh result. Copying call recipes would duplicate effects and could falsely claim shared identity. A prerequisite provider can construct such state manually, but it makes the application restate setup already present in its tests.

## Decision

The current package gives a workload one optional validated setup recipe:

- static analysis flattens only the approved straight-line fixture/helper shapes into source-ordered setup actions;
- each action names one Java-confirmed application facade method key and uses the closed persisted value language;
- separate source occurrences remain separate actions even when their text or arguments match;
- later actions and participant arguments may reference only earlier retained results or the approved `aggregateId`/`courseAggregateId` properties and exact `quiz.aggregateId` path;
- one validator owns action order, method/type/value compatibility, reference direction, and absence of runtime values;
- runtime authorization is independent: the application supplies a closed dispatch map with no arbitrary reflection fallback;
- setup runs once per attempt before target fault injection, retains results only in that attempt, clears setup-created pending events, and proves an empty baseline before target startup;
- source-setup preflight isolates each candidate in a bounded fresh JVM/Spring/H2 worker;
- SetupPlan semantics participate in deterministic workload identity, but runtime IDs and returned values do not.

A workload cannot mix a prerequisite baseline and a source-derived setup recipe.

The 2026-09-04 nested-property extension keeps the existing property string and DTO-root
contract. Runtime validation checks the exact public `getQuiz()` and `getAggregateId()`
getter signatures, including a DTO intermediate and Integer-compatible leaf, before any
setup action executes. It does not add arbitrary dotted traversal or new dispatcher
authority. A null intermediate result fails setup before participant startup.

The same extension retains exact typed target occurrences for preparation inside a
feature. It combines ordinary `setup()` with the supported same-feature prefix, stopping
before the earliest selected target. It preserves void effects, rejects selected-target
replay and omitted inter-target facade effects, and blocks ambiguous repeated occurrences.
Assertions, control flow, direct workflow calls, and event-handler calls close further
prefix extraction. Different feature executions are not synthesized into one setup.
Existing complete fixture-only candidates retain priority. Occurrence/frontier metadata
is analysis-only; persisted input identity and package schemas remain unchanged.

Direct facade argument recipes preserve setters and property assignments from before the
call. Self-rebinding consumes the old value before clearing its mutation scope, and later
assignments do not change the recorded recipe. Runtime qualification must still confirm
that the application authorizes every retained method and accepts the prepared values.

The Quizzes test-runtime dispatcher explicitly registers ten signatures, including the
2026-09-04 additions `createQuiz`, `startQuiz`, and `addParticipant`. An ordinary
QuizAnswer event-handling feature already supplies a coherent receiver fixture; its
existing source-derived setup can prepare the receiver without cross-test synthesis.
Method authorization remains distinct from successful setup and measured replay.

The 2026-09-05 participant extension recursively binds whole earlier setup results inside
the existing constructor/assignment/collection/transform language. An authoritative
whole-result reference replaces that value without revisiting historical provenance.
References must identify retained, type-compatible producers; conflicting definitions
of one occurrence block the plan. For measured targets inside setup, exact occurrence
metadata selects only the pre-target prefix and excludes the target from full-fixture
fallback. This adds no persisted schema or runtime dispatch authority. Nested scalar
property collections remain a separate extension; existing argument-root property
bindings are unchanged. The static validator also recognizes the canonical dummyapp
ItemDto fields to exercise the generic mechanism through parsed fixture source.

## Why this contract

- Shared identity follows one exact producer occurrence instead of names, equal-looking calls, or coincidentally equal runtime IDs.
- Required activation and enrollment effects are not lost merely because they return void.
- The persisted package remains deterministic and application-independent while runtime method authority remains application-owned.
- Setup failure cannot become an evaluated zero-impact target attempt.
- Existing scheduling, cap, fault, compensation, recovery, and ImpactV1 semantics remain unchanged.

## Consequences

- Current manifest-described records have exact, versionless kind-specific shapes. Historical versioned package records are not readable or executable.
- Supported extraction is deliberately narrow, not a Java/Groovy interpreter.
- Source-setup preflight costs one fresh process per candidate; ordinary selected execution already owns one fresh process per attempt.
- The historical Remove/Add descriptor/provider remains retained evidence, but the automatic ordinary-test path does not invoke it.
- Generic final-state impact, search/reward, cross-workload allocation, TCC, remote execution, and broader fixture languages remain separate work.

## Revisit when

- another representative ordinary test requires a genuinely new setup value/control-flow shape;
- the supported runtime needs an isolation boundary other than fresh process/H2;
- a later package contract supersedes SetupPlan while preserving exact source occurrence and shared-result semantics.

Current behavior and harmful/control evidence are in [`../current-state.md`](../current-state.md#bounded-current-executable-package).
