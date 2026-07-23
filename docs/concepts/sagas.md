# Sagas

## What They Are

Sagas implement **semantic locking** to prevent conflicting concurrent operations. Each saga aggregate holds a `SagaState` that steps can check or set. Conflicting operations declare which states they must not find the aggregate in; if the aggregate is already in a forbidden state, the operation aborts.

## Key Classes

| Class | Location | Role |
|-------|---------|------|
| `SagaAggregate` | `simulator/.../ms/sagas/aggregate/SagaAggregate.java` | Interface adding `getSagaState()` / `setSagaState()` |
| `SagaUnitOfWork` | `simulator/.../ms/sagas/unitOfWork/SagaUnitOfWork.java` | Coordinates reads/writes for a saga execution |
| `SagaUnitOfWorkService` | `simulator/.../ms/sagas/unitOfWork/SagaUnitOfWorkService.java` | Creates and commits/aborts `SagaUnitOfWork` instances |
| `SagaWorkflow` | `simulator/.../ms/sagas/workflow/SagaWorkflow.java` | Workflow engine for sagas |
| `SagaStep` | `simulator/.../ms/sagas/workflow/SagaStep.java` | A single step in a saga workflow |

## SagaState

Each saga aggregate has a corresponding `XxxSagaState` enum located at `microservices/<aggregate>/aggregate/sagas/states/XxxSagaState.java`.

States encode the semantic lock meaning:
```java
public enum CourseExecutionSagaState implements SagaAggregate.SagaState {
    NOT_IN_SAGA("NOT_IN_SAGA"),
    IN_ADD_PARTICIPANT("IN_ADD_PARTICIPANT"),
    IN_UPDATE_TOURNAMENT("IN_UPDATE_TOURNAMENT");

    private final String stateName;
    CourseExecutionSagaState(String stateName) { this.stateName = stateName; }

    @Override
    public String getStateName() { return stateName; }
}
```

## Lock-Acquisition Step Pattern (Two-Step Write Sagas)

Write sagas that must lock the primary aggregate before mutating it use a two-step pattern:

- **Step 1 — read + lock**: wrap the read command in `SagaCommand`, then call `setSemanticLock(state)` to atomically check the current state and transition to the new one.
- **Step 2 — mutate**: send a plain (unwrapped) command that performs the mutation; declare step 1 as a dependency.

```java
// Step 1: acquire lock
SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
    GetCourseByIdCommand readCmd = new GetCourseByIdCommand(
            unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
    SagaCommand sagaCommand = new SagaCommand(readCmd);
    sagaCommand.setSemanticLock(CourseSagaState.IN_UPDATE_COURSE);
    this.courseDto = (CourseDto) commandGateway.send(sagaCommand);
});

// Step 2: mutate (plain command, no SagaCommand wrapper)
SagaStep updateCourseStep = new SagaStep("updateCourseStep", () -> {
    UpdateCourseCommand cmd = new UpdateCourseCommand(
            unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId, name);
    commandGateway.send(cmd);
}, new ArrayList<>(Arrays.asList(getCourseStep)));
```

Contrast with plain `setForbiddenStates(...)` (shown below): that checks whether an existing lock blocks this operation, but does **not** set a new lock. `SagaCommand` + `setSemanticLock(...)` both checks and transitions atomically.

## Semantic-lock release on abort is automatic

**Application workflows must NOT manually release semantic locks.** The core owns the entire lock lifecycle - acquire, release-on-abort, and release-on-commit - and a manually-registered release compensation both duplicates that work and corrupts the abort ledger.

How the automatic release works:

- When a step runs `setSemanticLock`, the core (`SagaCommandHandler` → `registerSagaState()`) records the aggregate's **pre-lock** `SagaState`, keyed by the currently-executing step, via `savePreviousState(aggId, oldState)`, then applies the lock atomically. For a first-time lock, that recorded pre-lock state **is** `NOT_IN_SAGA`.
- On abort, `SagaUnitOfWorkService.abortUntilStep` walks the executed steps in reverse and replays each recorded previous state through an `AbortSagaCommand`, restoring every aggregate to its pre-lock state. For the lock step, that restores `NOT_IN_SAGA` - so the automatic path alone releases every lock. The successful-commit path likewise resets `SagaState` back to `NOT_IN_SAGA`.

`registerCompensation` is reserved for **genuine domain-level compensating actions** - undoing a real side effect a step generated, e.g. deleting a child aggregate a step created (cf. `CreateTournamentFunctionalitySagas` removing a generated quiz). It is never used to release a lock.

> **⚠️ The `currentExecutingStep` re-lock pitfall.** A manual "release the lock" compensation on the lock-acquiring step does not just no-op - it actively re-locks the aggregate. When the injected fault throws before the faulting step's own `execute()` runs, `currentExecutingStep` stays pinned to the lock step. The manual release then re-enters `registerSagaState` → `savePreviousState` under that same frozen step key, appending a **spurious `(agg, LOCKED_STATE)`** record. On abort, `sendAbortCommandsForStep` replays both records in list order; the spurious `LOCKED` one is applied **last and wins**, leaving the aggregate locked. Empirically: re-adding one manual release block to `DeleteCourseFunctionalitySagas` makes `DeleteCourseCompensationTest` fail with persisted `IN_DELETE_COURSE` instead of `NOT_IN_SAGA`. The compensation test's `sagaStateOf(...) == NOT_IN_SAGA` assertion is the regression guard for exactly this class of lock-lifecycle bug.

## Semantic Locks in Practice

A step acquires a lock by setting the saga state on the command:

```java
SagaStep addParticipantStep = new SagaStep("addParticipantStep", () -> {
    List<SagaAggregate.SagaState> states = new ArrayList<>();
    states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);

    AddParticipantCommand cmd = new AddParticipantCommand(...);
    cmd.setForbiddenStates(states);   // abort if tournament is IN_UPDATE_TOURNAMENT
    commandGateway.send(cmd);
}, dependencies);
```

`setForbiddenStates(...)` causes the command handler to check the current saga state and throw if it matches any forbidden state.

## R4 Decision Table — `SagaCommand` vs `setForbiddenStates`

| Step target | Pattern | When to use |
|-------------|---------|-------------|
| **Primary aggregate** (the aggregate owning this saga) | `SagaCommand` wrapping the read command + `setSemanticLock(state)` | Lock acquisition before mutating the saga's own aggregate — see § Lock-Acquisition Step Pattern. The lock is released automatically on abort/commit — do **not** register a manual release compensation (see § Semantic-lock release on abort is automatic) |
| **Foreign aggregate** (upstream aggregate touched by a cross-aggregate step) | Plain command + `setForbiddenStates([...])` listing states that must block this step | Abort if the foreign aggregate is already mid-saga in a conflicting state; does **not** acquire a new lock |

**Rule of thumb:** if the step must **acquire** a lock on an aggregate before writing to it, use `SagaCommand` + `setSemanticLock`. If the step only needs to **check** that another aggregate is not already locked, use `setForbiddenStates`.

## Read Functionality Sagas

Read sagas are a thin single-step wrapper. They have no compensation, no forbidden states, and no semantic lock acquisition — reads are non-mutating.

```java
public class Get{Aggregate}ByIdFunctionalitySagas extends WorkflowFunctionality {
    private {Aggregate}Dto {aggregate}Dto;

    public Get{Aggregate}ByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer {aggregate}AggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, {aggregate}AggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
            Integer {aggregate}AggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep get{Aggregate}Step = new SagaStep("get{Aggregate}Step", () -> {
            Get{Aggregate}ByIdCommand cmd = new Get{Aggregate}ByIdCommand(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), {aggregate}AggregateId);
            this.{aggregate}Dto = ({Aggregate}Dto) commandGateway.send(cmd);
        });

        this.workflow.addStep(get{Aggregate}Step);
    }

    public {Aggregate}Dto get{Aggregate}Dto() {
        return {aggregate}Dto;
    }
}
```

The coordinator method creates it inline and returns the DTO via the getter:

```java
Get{Aggregate}ByIdFunctionalitySagas saga = new Get{Aggregate}ByIdFunctionalitySagas(
        unitOfWorkService, aggregateId, unitOfWork, commandGateway);
saga.executeWorkflow(unitOfWork);
return saga.get{Aggregate}Dto();
```

Reference: `applications/quizzes/.../execution/coordination/sagas/GetCourseExecutionByIdFunctionalitySagas.java`

### List-return read variant

When a read functionality returns multiple aggregates (e.g., all topics for a course), the result field is `List<{Aggregate}Dto>`, the cast is `(List<{Aggregate}Dto>)`, and the getter returns the list:

```java
public class Get{Aggregates}By{Field}FunctionalitySagas extends WorkflowFunctionality {
    private List<{Aggregate}Dto> {aggregates};
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public Get{Aggregates}By{Field}FunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer {field}Id,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow({field}Id, unitOfWork);
    }

    public void buildWorkflow(Integer {field}Id, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep get{Aggregates}Step = new SagaStep("get{Aggregates}Step", () -> {
            Get{Aggregates}By{Field}Command cmd = new Get{Aggregates}By{Field}Command(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), {field}Id);
            this.{aggregates} = (List<{Aggregate}Dto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(get{Aggregates}Step);
    }

    public List<{Aggregate}Dto> get{Aggregates}() {
        return {aggregates};
    }
}
```

The coordinator method returns the list via the getter:

```java
Get{Aggregates}By{Field}FunctionalitySagas saga = new Get{Aggregates}By{Field}FunctionalitySagas(
        unitOfWorkService, {field}Id, unitOfWork, commandGateway);
saga.executeWorkflow(unitOfWork);
return saga.get{Aggregates}();
```

Reference: `applications/quizzes/.../topic/coordination/sagas/FindTopicsByCourseFunctionalitySagas.java`

### Two-step read saga variant

When a read functionality's filter parameter is a foreign aggregate's ID (e.g., `executionId`) that must be resolved to the primary aggregate's actual filter field (e.g., `courseAggregateId`), use a two-step saga:

- **Step 1** — fetch the foreign aggregate DTO (plain read, no lock, no compensation).
- **Step 2** — send the primary read command with the resolved field; declare step 1 as a dependency.

No compensation is needed on either step because reads are non-mutating.

```java
public class Get{Aggregates}By{ForeignField}FunctionalitySagas extends WorkflowFunctionality {
    private List<{Aggregate}Dto> {aggregates};
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public Get{Aggregates}By{ForeignField}FunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer {foreignField}Id,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow({foreignField}Id, unitOfWork);
    }

    public void buildWorkflow(Integer {foreignField}Id, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // Step 1: resolve the foreign aggregate to obtain the primary filter field
        final Integer[] resolvedFilterId = new Integer[1];
        SagaStep get{ForeignAggregate}Step = new SagaStep("get{ForeignAggregate}Step", () -> {
            Get{ForeignAggregate}ByIdCommand cmd = new Get{ForeignAggregate}ByIdCommand(
                    unitOfWork, ServiceMapping.{FOREIGN_AGGREGATE}.getServiceName(), {foreignField}Id);
            {ForeignAggregate}Dto dto = ({ForeignAggregate}Dto) commandGateway.send(cmd);
            resolvedFilterId[0] = dto.get{FilterField}();
        });

        // Step 2: fetch primary aggregates using the resolved filter
        SagaStep get{Aggregates}Step = new SagaStep("get{Aggregates}Step", () -> {
            Get{Aggregates}By{FilterField}Command cmd = new Get{Aggregates}By{FilterField}Command(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), resolvedFilterId[0]);
            this.{aggregates} = (List<{Aggregate}Dto>) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(get{ForeignAggregate}Step)));

        this.workflow.addStep(get{ForeignAggregate}Step);
        this.workflow.addStep(get{Aggregates}Step);
    }

    public List<{Aggregate}Dto> get{Aggregates}() {
        return {aggregates};
    }
}
```

The coordinator method returns the list via the getter (same as the list-return variant):

```java
Get{Aggregates}By{ForeignField}FunctionalitySagas saga = new Get{Aggregates}By{ForeignField}FunctionalitySagas(
        unitOfWorkService, {foreignField}Id, unitOfWork, commandGateway);
saga.executeWorkflow(unitOfWork);
return saga.get{Aggregates}();
```

Reference: `applications/quizzes-full/.../question/coordination/sagas/GetQuestionsByCourseExecutionIdFunctionalitySagas.java`
(no equivalent two-step read saga exists in `applications/quizzes` — this pattern was first
introduced in `quizzes-full`).

## Step Ordering

The typical step order inside a write `FunctionalitySagas` class is:

1. *(Conditional)* **Validate-dates step** — if the saga creates or updates an aggregate with `startTime`/`endTime` fields **and** a later step also creates/updates a downstream aggregate that independently validates dates (e.g., a Quiz), add a dedicated `validateDatesStep` as the very first step to check date constraints on the primary aggregate's DTO. If omitted, the downstream aggregate's date invariant fires first and masks the primary aggregate's date error, making the wrong exception surface to tests.
2. **Data-assembly steps** — fetch DTOs from upstream aggregates (required for P4a and P3 DTO-check rules listed in plan.md cross-aggregate prerequisites).
3. **Primary lock step** — wrap the read command in `SagaCommand` and call `setSemanticLock(state)` on the primary aggregate. See § Lock-Acquisition Step Pattern. Do **not** register a compensation to release the lock — the core releases it automatically on abort (see § Semantic-lock release on abort is automatic). Do **not** use `setForbiddenStates` for primary-aggregate lock acquisition.
4. **Execute step** — send a plain (unwrapped) command to `{Aggregate}CommandHandler`; declare the lock step as a dependency.
5. *(For multi-aggregate sagas)* **Steps for other aggregates involved** — use `setForbiddenStates` only on steps that touch a **foreign** aggregate to abort if that aggregate is mid-saga (see § R4 Decision Table).

**R8 — upstream-only commands:** a saga may only send commands to aggregates that are upstream (aggregates this one depends on, not aggregates that depend on it). Never dispatch a write command to a downstream aggregate from within an upstream aggregate's saga.

`registerCompensation` is **only** for genuine domain-level undos — reversing a real side effect a step produced (e.g. deleting a child aggregate a step created). It is **never** used to release a semantic lock: lock release on abort is automatic (see § Semantic-lock release on abort is automatic). Each data-assembly step that enforces a **P4a rule** treats an upstream-command failure as the prerequisite violation — no extra guard is needed in the service layer.

---

## Write Workflow Structure

```java
public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    public AddParticipantFunctionalitySagas(..., SagaUnitOfWork unitOfWork, ...) {
        this.buildWorkflow(..., unitOfWork);
    }

    public void buildWorkflow(..., SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep step1 = new SagaStep("step1Name", () -> { /* action */ });
        SagaStep step2 = new SagaStep("step2Name", () -> { /* action */ },
                new ArrayList<>(Arrays.asList(step1)));  // depends on step1

        this.workflow.addStep(step1);
        this.workflow.addStep(step2);
    }
}
```

`WorkflowFunctionality` provides:
- `executeWorkflow(uow)` — run all steps
- `executeUntilStep(stepName, uow)` — run up to and including the named step (test use only)
- `resumeWorkflow(uow)` — continue from where it stopped (after `executeUntilStep`)

## Saga Class Hierarchy

```
Aggregate (abstract)
  └── Xxx (abstract)
        └── SagaXxx implements SagaAggregate
```

Example (Quizzes): `Execution (abstract) → SagaExecution implements SagaAggregate`

`SagaExecution` holds the `sagaState` field and implements `get/setSagaState()`.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Saga class | `SagaXxx` | `SagaExecution` |
| Saga state enum | `XxxSagaState` | `CourseExecutionSagaState` |
| Functionality | `XxxFunctionalitySagas` | `AddParticipantFunctionalitySagas` |
| Factory | `SagasXxxFactory` | `SagasExecutionFactory` |
| Repository | `XxxCustomRepositorySagas` | `CourseExecutionCustomRepositorySagas` |

## Reference Implementation

- `applications/.../tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` — two-step workflow with forbidden state
- `applications/.../execution/aggregate/sagas/states/CourseExecutionSagaState.java` — state enum
