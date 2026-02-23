# DSL Syntax

This chapter covers all Nebula DSL language constructs with real-world examples.

## Table of Contents

- [Aggregates](#aggregates)
- [Entities and Properties](#entities-and-properties)
- [Cross-Aggregate References](#cross-aggregate-references)
- [Extract Pattern](#extract-pattern)
- [Invariants](#invariants)
- [Events](#events)
- [References](#references)
- [Repositories](#repositories)
- [Services](#services)
- [Web API](#web-api)
- [Shared Enums](#shared-enums)
- [Exception Messages](#exception-messages)
- [Best Practices](#best-practices)

## Aggregates

### Basic Aggregate

The simplest aggregate definition:

```nebula
Aggregate User {
    Root Entity User {
        String name
        String username
    }
}
```

This generates: JPA entity, DTO, factory, repository, and service.

### Aggregate with CRUD Generation

Add `@GenerateCrud` to generate complete CRUD operations across all layers:

```nebula
Aggregate User {
    @GenerateCrud

    Root Entity User {
        String name
        String username
    }
}
```

This additionally generates: full CRUD service methods, REST controller endpoints, functionalities, and saga workflows.

## Entities and Properties

### Root Entity

Every aggregate must have exactly one root entity. It extends the simulator's `Aggregate` base class and has `aggregateId`, `version`, and `state` fields automatically.

```nebula
Aggregate Course {
    Root Entity Course {
        String name
        CourseType type
        Integer credits
    }
}
```

### Non-Root Entities

Aggregates can have child entities for composition:

```nebula
Aggregate Execution {
    Entity ExecutionCourse from Course {
        map name as courseName
        map type as courseType
    }

    Root Entity Execution {
        ExecutionCourse course
        Set<ExecutionUser> users
    }
}
```

Non-root entities are simple JPA entities (no version chain, no invariants).

### Data Types

**Primitives:**
```nebula
String name
Integer count
Long timestamp
Float price
Double percentage
Boolean active
```

**Temporal:**
```nebula
LocalDateTime createdAt
LocalDateTime availableDate
```

**Collections:**
```nebula
List<Integer> scores
Set<ExecutionUser> users
```

**Optional:**
```nebula
Optional<LocalDateTime> deadline
```

**Enums** (defined in `SharedEnums`):
```nebula
UserRole role
CourseType type
```

**Entity references** (composition):
```nebula
ExecutionCourse course
```

**Built-in types:**
```nebula
AggregateState state
UnitOfWork unitOfWork
```

### Property Modifiers

**Immutable (`final`)** -- cannot be changed after creation:
```nebula
final UserRole role
final LocalDateTime createdAt
```

**Default values:**
```nebula
Boolean active = true
Integer count = 0
AggregateState state = AggregateState.ACTIVE
```

**DTO exclusion** -- field not included in DTOs:
```nebula
String internalId dto-exclude
```

**Multiple properties** of same type:
```nebula
String firstName, lastName, email
```

## Cross-Aggregate References

### Basic Mapping

Reference entities from other aggregates with automatic type inference:

```nebula
Entity ExecutionCourse from Course {
    map name as courseName      // String (inferred from Course.name)
    map type as courseType       // CourseType (inferred from Course.type)
}
```

The generator:
1. Finds the `Course` aggregate
2. Gets its root entity
3. Infers types from `Course.name` and `Course.type`

**Generated code:**
```java
@Embeddable
public class ExecutionCourse {
    private Integer courseAggregateId;  // Auto-added
    private Integer courseVersion;      // Auto-added
    private String courseName;         // Inferred: String
    private CourseType courseType;      // Inferred: CourseType
}
```

### Cross-File Type Inference

Type inference works across file boundaries. A model registry tracks all aggregates:

```nebula
// user.nebula
Aggregate User {
    Root Entity User {
        String name
        String username
        Boolean active
    }
}
```

```nebula
// execution.nebula (different file)
Entity ExecutionUser from User {
    map name as userName          // String (from user.nebula)
    map username as userUsername   // String (from user.nebula)
    map active as userActive      // Boolean (from user.nebula)
}
```

### Base Fields

All cross-aggregate entities automatically include:

```java
private Integer <aggregate>AggregateId;  // e.g., courseAggregateId
private Integer <aggregate>Version;      // e.g., courseVersion
```

## Extract Pattern

Extract specific fields from collection properties in cross-aggregate references.

### Syntax

```nebula
Entity AnswerQuiz from Quiz {
    map questions.aggregateId as quizQuestionsAggregateIds
}
```

This extracts `Set<Integer>` from `Set<QuizQuestion>` by accessing `aggregateId` on each element.

### How It Works

1. `questions` is a `Set<QuizQuestion>` in the Quiz root entity
2. `.aggregateId` accesses the `aggregateId` field of `QuizQuestion`
3. The result is `Set<Integer>` -- the collection type is preserved

### Generated Code

```java
// Entity
private Set<Integer> quizQuestionsAggregateIds = new HashSet<>();

// DTO
private Set<Integer> quizQuestionsAggregateIds;
public Set<Integer> getQuizQuestionsAggregateIds() { ... }
public void setQuizQuestionsAggregateIds(Set<Integer> quizQuestionsAggregateIds) { ... }
```

### Common Use Cases

```nebula
// Extract aggregate IDs from collections
map participants.aggregateId as participantIds

// Extract business fields
map products.sku as productSkus

// Extract status fields
map orders.status as orderStatuses
```

### Limitations

- Single-level extraction only (no `a.b.c`)
- Root entity properties only
- No filtering support
- Works with `List<T>` and `Set<T>` only

## Invariants

### Simple Invariants

Business rules enforced at the aggregate level:

```nebula
Root Entity User {
    String name
    UserRole role

    invariants {
        check nameNotBlank { name.length() > 0 }
            error "User name cannot be blank"

        check roleNotNull { role != null }
            error "User role is required"
    }
}
```

Error messages are mandatory for all invariants.

### Complex Invariants

**Temporal constraints:**
```nebula
invariants {
    check dateOrdering {
        availableDate.isBefore(conclusionDate) &&
        conclusionDate.isBefore(resultsDate)
    }
        error "Dates must be in chronological order"
}
```

**Collection constraints:**
```nebula
invariants {
    check topicsNotEmpty { topics != null && !topics.isEmpty() }
        error "Question must have at least one topic"
}
```

### Supported Expressions

**Comparisons:** `<`, `>`, `<=`, `>=`, `==`, `!=`

**Boolean logic:** `&&`, `||`, `!`

**String operations:**
```nebula
name.length() > 0          // Automatic null check added
email.contains("@")
```

**Temporal operations:**
```nebula
startDate.isBefore(endDate)
deadline.isAfter(now())
```

**Collection operations:**
```nebula
users.size() > 0
questions.isEmpty()
topics.unique(topicId)     // All topicIds are unique
```

## Events

### Published Events

Define events that this aggregate emits:

```nebula
Events {
    publish UserDeleted {
        Integer userId
        String username
        UserRole role
    }

    publish UserUpdated {
        Integer userId
        String newName
    }
}
```

Generates event classes and publishing logic in services.

### Subscribed Events

Subscribe to events from other aggregates:

**Simple subscription:**
```nebula
subscribe CourseDeletedEvent
```

**With source aggregate:**
```nebula
subscribe UserDeletedEvent from User
```

**With routing** (only deliver if match found):
```nebula
subscribe UserUpdatedEvent from User routing (users.userAggregateId)
```

**With condition:**
```nebula
subscribe CourseDeletedEvent from Course {
    course.courseAggregateId == event.aggregateId
}
```

### Inter-Invariants

Group related subscriptions for referential integrity:

```nebula
Events {
    interInvariant COURSE_EXISTS {
        subscribe CourseDeletedEvent from Course {
            course.courseAggregateId == event.aggregateId
        }
    }

    interInvariant USERS_EXIST {
        subscribe UserDeletedEvent from User {
            users.userAggregateId == event.aggregateId
        }
        subscribe UserUpdatedEvent from User {
            users.userAggregateId == event.aggregateId
        }
    }
}
```

## References

### Reference Constraints

Define how to handle deletion of referenced aggregates:

```nebula
References {
    course -> Course {
        onDelete: prevent
        message: "Cannot delete course that has executions"
    }

    creator -> User {
        onDelete: setNull
        message: "Creator deleted, reference set to null"
    }

    participants -> User {
        onDelete: cascade
        message: "User deleted, removing from participants"
    }
}
```

### Actions

| Action | Behavior |
|--------|----------|
| `prevent` | Throw exception if referenced aggregate is deleted |
| `setNull` | Set reference to null when referenced aggregate is deleted |
| `cascade` | Delete this aggregate when referenced aggregate is deleted |

## Repositories

### Spring Data Methods

Method names are parsed by Spring Data:

```nebula
Repository {
    Optional<User> findByUsername(String username)
    List<User> findByRole(UserRole role)
    List<User> findByActiveTrue()
}
```

### Custom JPQL Queries

Use `@Query` annotation for complex queries:

```nebula
Repository {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds()

    @Query("select a from Answer a where a.quiz.quizAggregateId = :quizId")
    List<Answer> findByQuizId(Integer quizId)
}
```

Parameters are bound using `:paramName` syntax matching method parameter names.

## Services

### Auto-Generated Service

With `@GenerateCrud`, a complete service is generated:

```nebula
Aggregate User {
    @GenerateCrud
    Root Entity User {
        String name
        UserRole role
    }
}
```

Generated methods:
- `createUser(CreateUserRequestDto, UnitOfWork)`
- `getUserById(Integer, UnitOfWork)`
- `getAllUsers(UnitOfWork)`
- `updateUser(Integer, UserDto, UnitOfWork)`
- `deleteUser(Integer, UnitOfWork)`

Collection properties additionally get add/remove methods.

### Custom Service Methods

Define additional methods:

```nebula
Service UserService {
    @Transactional

    methods {
        activateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        deactivateUser(Integer userId, UnitOfWork unitOfWork): UserDto
    }
}
```

If the name is omitted, it defaults to `<Aggregate>Service`:

```nebula
Service {
    @Transactional
}
```

## Web API

### Auto-Generated Endpoints

`@GenerateCrud` creates REST endpoints:

```java
POST   /users                 createUser
GET    /users/{id}            getUser
GET    /users                 getAllUsers
PUT    /users/{id}            updateUser
DELETE /users/{id}            deleteUser
```

### Custom Endpoints

```nebula
WebAPIEndpoints {
    Endpoint activateUser {
        httpMethod: POST
        path: "/users/{id}/activate"
        methodName: activateUser
        parameters: [id: Integer: "@PathVariable"]
        returnType: UserDto
        desc: "Activate a user account"
    }
}
```

## Shared Enums

Define enumerations shared across aggregates:

```nebula
SharedEnums {
    enum UserRole {
        STUDENT, TEACHER, ADMIN
    }

    enum CourseType {
        TECNICO, EXTERNAL
    }
}
```

Generated as standard Java enums in the `shared/enums/` package.

## Exception Messages

Define project-specific error messages:

```nebula
exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
    INVALID_USERNAME: "Username '%s' is invalid"
    DUPLICATE_EMAIL: "Email '%s' is already registered"
}
```

Generated as static constants in an exception class.

## Complete Example

Here's the Execution aggregate from the Answers project, demonstrating multiple features together:

```nebula
Aggregate Execution {
    @GenerateCrud

    Entity ExecutionCourse from Course {
        map name as courseName
        map type as courseType
    }

    Entity ExecutionUser from User {
        map name as userName
        map username as userUsername
        map active as userActive
    }

    Root Entity Execution {
        String acronym
        String academicTerm
        LocalDateTime endDate
        ExecutionCourse course
        Set<ExecutionUser> users

        invariants {
            check acronymNotBlank { acronym.length() > 0 }
                error "Execution acronym cannot be blank"

            check academicTermNotBlank { academicTerm.length() > 0 }
                error "Academic term cannot be blank"

            check courseNotNull { course != null }
                error "Execution must be associated with a course"

            check usersNotNull { users != null }
                error "Execution must have a users collection"
        }
    }

    References {
        course -> Course {
            onDelete: prevent
            message: "Cannot delete course that has executions"
        }
    }

    Events {
        interInvariant COURSE_EXISTS {
            subscribe CourseDeletedEvent from Course {
                course.courseAggregateId == event.aggregateId
            }
        }

        interInvariant USERS_EXIST {
            subscribe UserDeletedEvent from User {
                users.userAggregateId == event.aggregateId
            }
            subscribe UserUpdatedEvent from User {
                users.userAggregateId == event.aggregateId
            }
        }
    }

    Repository {
        @Query("select e1.aggregateId from Execution e1 where e1.aggregateId NOT IN (select e2.aggregateId from Execution e2 where e2.state = 'DELETED' AND e2.sagaState != 'NOT_IN_SAGA')")
        Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga()
    }
}
```

This single file (~65 lines) generates ~2,000 lines of Java code:
- 3 JPA entities, 3 DTOs, 1 factory, 2 repositories
- 1 service with CRUD + collection methods
- 1 REST controller
- Event subscriptions and handlers
- Saga workflows
- Invariant validation

## Best Practices

### One Aggregate Per File
Keep each aggregate in its own `.nebula` file, named after the aggregate (lowercase).

### Use `@GenerateCrud` Liberally
Most aggregates need CRUD operations. Start with `@GenerateCrud` and add custom methods as needed.

### Use Cross-Aggregate References
Instead of duplicating data, reference other aggregates with `from`:
```nebula
Entity ExecutionUser from User {
    map name as userName
}
```

### Define Invariants
Business rules in invariants are enforced automatically. Always include descriptive error messages.

### Use References for Integrity
Define `References` blocks to handle cascading deletes and referential integrity.

---

**Previous:** [02-Getting-Started](02-Getting-Started.md) | **Next:** [04-Generated-Code](04-Generated-Code.md)
