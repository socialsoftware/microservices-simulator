# Your First Aggregate

Build your first microservice in 10 lines of DSL. This chapter introduces the core building blocks of Nebula using the simplest possible example.

> **Tied example:** [`01-helloworld`](../examples/abstractions/01-helloworld/) — a single Task aggregate.

## The Complete Example

Create a file called `task.nebula`:

```nebula
Aggregate Task {
    @GenerateCrud

    Root Entity Task {
        String title
        String description
        Boolean done
    }
}
```

That's it. These 9 lines generate a complete microservice with entities, DTOs, factories, repositories, services, REST controllers, saga workflows, and more.

## Line-by-Line Explanation

### `Aggregate Task`

Every domain concept is wrapped in an `Aggregate` block. The aggregate name determines:
- Java package names (`microservices.task`)
- Class name prefixes (`TaskService`, `TaskFactory`, `TaskController`)
- REST endpoint paths (`/tasks`)
- File organization on disk

### `@GenerateCrud`

This annotation tells Nebula to generate complete CRUD operations across all layers:

| Layer | What's Generated |
|-------|-----------------|
| **Service** | `createTask`, `getTaskById`, `getAllTasks`, `updateTask`, `deleteTask` |
| **Controller** | `POST /tasks`, `GET /tasks/{id}`, `GET /tasks`, `PUT /tasks/{id}`, `DELETE /tasks/{id}` |
| **Functionalities** | Orchestration wrappers for each operation |
| **Sagas** | Distributed transaction workflows for create, update, and delete |

### `Root Entity Task`

Every aggregate must have exactly one `Root Entity`. It becomes a JPA entity that extends the simulator's `Aggregate` base class, which automatically provides:

- `aggregateId` — unique identifier
- `version` — optimistic concurrency control
- `state` — lifecycle tracking (`ACTIVE`, `INACTIVE`, `DELETED`)

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

## Generate and Explore

Run the Nebula CLI to generate code:

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/01-helloworld/ -o ../docs/examples/generated
```

### What Gets Generated

The single `task.nebula` file produces this directory tree (showing the key files):

```
01-helloworld/
├── pom.xml
└── src/main/java/.../helloworld/
    ├── HelloworldSimulator.java                    # Spring Boot application
    ├── ServiceMapping.java                         # Service routing
    ├── command/task/                               # Command objects (CQRS)
    │   ├── CreateTaskCommand.java
    │   ├── GetTaskByIdCommand.java
    │   ├── GetAllTasksCommand.java
    │   ├── UpdateTaskCommand.java
    │   └── DeleteTaskCommand.java
    ├── coordination/
    │   ├── validation/                             # Business rule validation
    │   │   └── TaskBusinessRuleValidator.java (+ annotations)
    │   └── webapi/
    │       ├── BehaviourController.java            # Simulator behaviour API
    │       └── TracesController.java               # Tracing API
    ├── events/
    │   ├── TaskDeletedEvent.java                   # Domain events
    │   └── TaskUpdatedEvent.java
    ├── microservices/task/
    │   ├── aggregate/
    │   │   ├── Task.java                           # JPA entity
    │   │   ├── TaskFactory.java                    # Factory interface
    │   │   ├── TaskRepository.java                 # Spring Data repository
    │   │   ├── TaskCustomRepository.java           # Custom query interface
    │   │   └── sagas/                              # Saga-specific variants
    │   │       ├── SagaTask.java
    │   │       ├── dtos/SagaTaskDto.java
    │   │       ├── factories/SagasTaskFactory.java
    │   │       ├── repositories/TaskCustomRepositorySagas.java
    │   │       └── states/TaskSagaState.java
    │   ├── commandHandler/
    │   │   ├── TaskCommandHandler.java             # Command dispatch
    │   │   └── TaskStreamCommandHandler.java       # Stream command dispatch
    │   ├── coordination/
    │   │   ├── eventProcessing/
    │   │   │   └── TaskEventProcessing.java        # Event coordination
    │   │   ├── functionalities/
    │   │   │   └── TaskFunctionalities.java        # Orchestration layer
    │   │   ├── sagas/
    │   │   │   ├── CreateTaskFunctionalitySagas.java
    │   │   │   ├── GetTaskByIdFunctionalitySagas.java
    │   │   │   ├── GetAllTasksFunctionalitySagas.java
    │   │   │   ├── UpdateTaskFunctionalitySagas.java
    │   │   │   └── DeleteTaskFunctionalitySagas.java
    │   │   └── webapi/
    │   │       ├── TaskController.java             # REST controller
    │   │       └── requestDtos/
    │   │           └── CreateTaskRequestDto.java   # Creation request DTO
    │   └── service/
    │       └── TaskService.java                    # Business logic
    ├── microservices/exception/                    # Project-wide exceptions
    │   ├── HelloworldErrorMessage.java
    │   ├── HelloworldException.java
    │   └── HelloworldExceptionHandler.java
    └── shared/
        ├── dtos/
        │   └── TaskDto.java                        # Response DTO
        └── enums/                                  # Shared enumerations
```

That's **40+ files** from 9 lines of DSL — a significant reduction in code to write.

## What `@GenerateCrud` Produces

The five CRUD operations flow through three layers:

```
Controller (REST)  →  Functionalities (orchestration)  →  Service (business logic)
POST /tasks        →  createTask()                     →  TaskService.createTask()
GET /tasks/{id}    →  getTaskById()                    →  TaskService.getTaskById()
GET /tasks         →  getAllTasks()                     →  TaskService.getAllTasks()
PUT /tasks/{id}    →  updateTask()                     →  TaskService.updateTask()
DELETE /tasks/{id} →  deleteTask()                     →  TaskService.deleteTask()
```

Each operation uses the **Unit of Work** pattern for transaction safety:

```java
public TaskDto createTask(CreateTaskRequestDto dto, UnitOfWork unitOfWork) {
    Task task = taskFactory.createTask(null, dto);
    task = unitOfWorkService.registerChanged(task, unitOfWork);
    return taskFactory.createTaskDto(task);
}
```

## Try It Yourself

1. **Create a new file** in `dsl/docs/examples/abstractions/01-helloworld/` — for example, `note.nebula`:

```nebula
Aggregate Note {
    @GenerateCrud

    Root Entity Note {
        String content
        Boolean pinned
    }
}
```

2. **Regenerate:**

```bash
./bin/cli.js generate ../docs/examples/abstractions/01-helloworld/ -o ../docs/examples/generated
```

3. **Explore the output** — you'll see a new `note/` package alongside `task/`.

## What's Next

This chapter covered the bare minimum: a standalone aggregate with automatic CRUD. The next chapters build on this foundation:

- **[Chapter 04](04-Types-Enums-Properties.md)** — Enumerations, all data types, and property modifiers
- **[Chapter 05](05-Business-Rules-Repositories.md)** — Invariants and custom repository queries
- **[Chapter 06](06-Cross-Aggregate-References.md)** — Referencing data from other aggregates
- **[Chapter 07](07-Events-Reactive-Patterns.md)** — Event publishing and reactive subscriptions

---

**Previous:** [02-Getting-Started](02-Getting-Started.md) | **Next:** [04-Types-Enums-Properties](04-Types-Enums-Properties.md)
