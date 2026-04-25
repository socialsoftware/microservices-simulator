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

// Compensation for step 1: release the lock
SagaStep getCourseStep = new SagaStep("getCourseStep",
    /* execute */ () -> { ... },
    /* compensate */ () -> {
        SagaCommand release = new SagaCommand(
                new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId));
        release.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
        commandGateway.send(release);
    });

// Step 2: mutate (plain command, no SagaCommand wrapper)
SagaStep updateCourseStep = new SagaStep("updateCourseStep", () -> {
    UpdateCourseCommand cmd = new UpdateCourseCommand(
            unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId, name);
    commandGateway.send(cmd);
}, new ArrayList<>(Arrays.asList(getCourseStep)));
```

Contrast with plain `setForbiddenStates(...)` (shown below): that checks whether an existing lock blocks this operation, but does **not** set a new lock. `SagaCommand` + `setSemanticLock(...)` both checks and transitions atomically.

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

## Workflow Structure

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
