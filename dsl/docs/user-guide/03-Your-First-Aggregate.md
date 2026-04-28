# Your First Aggregate

Build your first microservice in 7 lines of DSL. This chapter introduces the core building blocks of Nebula and explains what gets generated for every aggregate.

> **Tied example:** [`01-helloworld`](../../abstractions/01-helloworld/): a single Task aggregate.

## The Complete Example

Create a file called `task.nebula`:

```nebula
Aggregate Task {

    Root Entity Task {
        String title
        String description
        Boolean done
    }
}
```

These 7 lines generate a complete microservice with entities, DTOs, factories, repositories, services, REST controllers, saga workflows, and concurrent-access protection.

## Line-by-Line Explanation

### `Aggregate Task`

Every domain concept is wrapped in an `Aggregate` block. The aggregate name determines:
- Java package names (`microservices.task`)
- Class name prefixes (`TaskService`, `TaskFactory`, `TaskController`)
- REST endpoint paths (`/tasks`)
- File organization on disk

### `Root Entity Task`

Every aggregate must have exactly one `Root Entity`. It becomes a JPA entity that extends the simulator's `Aggregate` base class, which automatically provides:

- `aggregateId`: unique identifier
- `version`: optimistic concurrency control (copy-on-write)
- `state`: lifecycle tracking (`ACTIVE`, `INACTIVE`, `DELETED`)

### Properties

Properties define the entity's fields:

```nebula
String title          // A text field
String description    // Another text field
Boolean done          // A boolean field
```

Each property generates:
- A private field in the JPA entity
- A getter and setter
- A corresponding field in the DTO
- Constructor parameters for creation

### Comments

Nebula supports single-line and multi-line comments:

```nebula
// This is a single-line comment

/* This is a multi-line comment */
Aggregate Task {
    Root Entity Task {
        String title       // Inline comments work too
    }
}
```

## What Gets Generated

The single `task.nebula` file generates **40+ Java files**. Here's the structure:

```
01-helloworld/
├── pom.xml
└── src/main/java/.../helloworld/
    ├── HelloworldSimulator.java               # Spring Boot application
    ├── ServiceMapping.java                    # Service routing
    ├── command/task/                          # CQRS command objects
    ├── events/
    │   ├── TaskDeletedEvent.java              # Auto-published on delete
    │   └── TaskUpdatedEvent.java              # Auto-published on update
    ├── microservices/task/
    │   ├── aggregate/
    │   │   ├── Task.java                      # JPA entity
    │   │   ├── TaskFactory.java               # Entity/DTO factory
    │   │   ├── TaskRepository.java            # Spring Data repository
    │   │   └── sagas/
    │   │       ├── SagaTask.java              # Saga-aware entity variant
    │   │       └── states/TaskSagaState.java  # Saga state enum
    │   ├── coordination/
    │   │   ├── functionalities/
    │   │   │   └── TaskFunctionalities.java   # Orchestration layer
    │   │   ├── sagas/
    │   │   │   ├── CreateTaskFunctionalitySagas.java
    │   │   │   ├── GetTaskByIdFunctionalitySagas.java
    │   │   │   ├── GetAllTasksFunctionalitySagas.java
    │   │   │   ├── UpdateTaskFunctionalitySagas.java
    │   │   │   └── DeleteTaskFunctionalitySagas.java
    │   │   └── webapi/
    │   │       ├── TaskController.java        # REST endpoints
    │   │       └── requestDtos/
    │   │           └── CreateTaskRequestDto.java
    │   └── service/
    │       └── TaskService.java               # Business logic
    └── shared/dtos/
        └── TaskDto.java                       # Response DTO
```

## Auto-Generated CRUD Operations

Every aggregate gets five CRUD operations, each flowing through three layers:

```
Controller (REST)               →  Functionalities  →  Service
POST /tasks/create              →  createTask()     →  TaskService.createTask()
GET  /tasks/{taskAggregateId}   →  getTaskById()    →  TaskService.getTaskById()
GET  /tasks                     →  getAllTasks()     →  TaskService.getAllTasks()
PUT  /tasks                     →  updateTask()     →  TaskService.updateTask()
DELETE /tasks/{taskAggregateId} →  deleteTask()     →  TaskService.deleteTask()
```

### Create

Builds a new aggregate instance from a request DTO:

```java
public TaskDto createTask(CreateTaskRequestDto createRequest, UnitOfWork unitOfWork) {
    TaskDto taskDto = new TaskDto();
    taskDto.setTitle(createRequest.getTitle());
    taskDto.setDescription(createRequest.getDescription());
    taskDto.setDone(createRequest.getDone());

    Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
    Task task = taskFactory.createTask(aggregateId, taskDto);
    unitOfWorkService.registerChanged(task, unitOfWork);
    return taskFactory.createTaskDto(task);
}
```

### Read (by ID)

Loads an aggregate by its ID and returns a DTO:

```java
public TaskDto getTaskById(Integer id, UnitOfWork unitOfWork) {
    Task task = (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
    return taskFactory.createTaskDto(task);
}
```

### Update

Uses copy-on-write: loads the current version, creates a mutable copy, applies changes, and registers the new version:

```java
public TaskDto updateTask(TaskDto taskDto, UnitOfWork unitOfWork) {
    Task oldTask = (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
    Task newTask = taskFactory.createTaskFromExisting(oldTask);

    if (taskDto.getTitle() != null) newTask.setTitle(taskDto.getTitle());
    if (taskDto.getDescription() != null) newTask.setDescription(taskDto.getDescription());
    newTask.setDone(taskDto.getDone());

    unitOfWorkService.registerChanged(newTask, unitOfWork);
    unitOfWorkService.registerEvent(new TaskUpdatedEvent(...), unitOfWork);
    return taskFactory.createTaskDto(newTask);
}
```

Update auto-publishes a `TaskUpdatedEvent` so other aggregates that reference this one can sync their projections.

### Delete

Marks the aggregate as removed (soft delete) and publishes a `TaskDeletedEvent`:

```java
public void deleteTask(Integer id, UnitOfWork unitOfWork) {
    Task oldTask = (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
    Task newTask = taskFactory.createTaskFromExisting(oldTask);
    newTask.remove();
    unitOfWorkService.registerChanged(newTask, unitOfWork);
    unitOfWorkService.registerEvent(new TaskDeletedEvent(...), unitOfWork);
}
```

## DTOs

Two DTOs are generated per aggregate:

- **`CreateTaskRequestDto`**: used for creation — contains only the user-supplied fields (no `aggregateId`, no `version`)
- **`TaskDto`**: used for responses and updates — contains all fields including `aggregateId` and `version`

## Saga-State Enforcement

Each CRUD operation runs inside a saga with automatic concurrent-access protection. A per-aggregate saga state enum is generated:

```java
public enum TaskSagaState implements SagaState {
    CREATE_TASK,
    READ_TASK,
    UPDATE_TASK,
    DELETE_TASK
}
```

Before each operation, the saga verifies that the aggregate isn't already held by another saga, then stamps it:

| Operation | Verify Against | Register As |
|-----------|---------------|-------------|
| **Create** | — (new aggregate) | — |
| **Read** | UPDATE, DELETE (allows concurrent reads) | READ_TASK |
| **Read All** | — (operates on list) | — |
| **Update** | READ, UPDATE, DELETE (blocks everything) | UPDATE_TASK |
| **Delete** | READ, UPDATE, DELETE (blocks everything) | DELETE_TASK |

This is the simulator's **semantic lock** pattern. It prevents concurrent sagas from conflicting — for example, two concurrent updates on the same task will not both succeed; the second saga sees `UPDATE_TASK` and throws `AGGREGATE_BEING_USED_IN_OTHER_SAGA`.

On commit, the saga state resets to `NOT_IN_SAGA`. On abort, it restores to the previous state.

## Auto-Published Events

Two domain events are auto-generated for every aggregate:

- **`TaskUpdatedEvent`**: published on update, carries the aggregate's current field values. Other aggregates subscribing to this event can refresh their local projections.
- **`TaskDeletedEvent`**: published on delete, carries the `aggregateId`. Other aggregates subscribing can cascade to INACTIVE (see [Chapter 07](07-Events-Reactive-Patterns.md)).

Events are registered on the Unit of Work and dispatched only when the transaction commits.

## Collection CRUD

If a root entity has collection properties (e.g., `Set<RoomAmenity> amenities`), additional service methods are auto-generated:

| Method | Description |
|--------|-------------|
| `add<Element>(rootId, key, elementDto, uow)` | Add an element |
| `add<Elements>(rootId, elementDtos, uow)` | Bulk add |
| `get<Element>(rootId, key, uow)` | Get by key |
| `update<Element>(rootId, key, elementDto, uow)` | Update |
| `remove<Element>(rootId, key, uow)` | Remove |

See [Chapter 04](04-Types-Enums-Properties.md) for collection types.

## Generate and Explore

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/01-helloworld/
```

## What's Next

This chapter covered the foundation: a standalone aggregate with automatic CRUD, saga enforcement, and event publishing. The next chapters build on this:

- **[Chapter 04](04-Types-Enums-Properties.md)**: Enumerations, all data types, and property modifiers
- **[Chapter 05](05-Business-Rules-Repositories.md)**: Invariants and custom repository queries
- **[Chapter 06](06-Cross-Aggregate-References.md)**: Referencing data from other aggregates
- **[Chapter 07](07-Events-Reactive-Patterns.md)**: Event publishing and reactive subscriptions
- **[Chapter 10](10-Methods-Custom-Endpoints.md)**: Custom methods and HTTP endpoints
- **[Chapter 11](11-Workflows-Sagas.md)**: Cross-aggregate workflows and saga orchestration

---

**Previous:** [02-Getting-Started](02-Getting-Started.md) | **Next:** [04-Types-Enums-Properties](04-Types-Enums-Properties.md)
