# DSL Features

This chapter provides a comprehensive tour of all Nebula DSL features with real-world examples from the Answers and TeaStore projects.

## Table of Contents

- [Aggregates](#aggregates)
- [Entities and Properties](#entities-and-properties)
- [Cross-Aggregate References](#cross-aggregate-references)
- [Events](#events)
- [Invariants](#invariants)
- [Repositories](#repositories)
- [Services](#services)
- [Web API](#web-api)
- [Shared Enums](#shared-enums)
- [Exception Messages](#exception-messages)

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

**Generated output:**
- `User.java` - JPA entity
- `UserDto.java` - Data transfer object
- `UserFactory.java` - Factory interface
- `UserRepository.java` - Spring Data repository
- `UserService.java` - Service layer with basic operations

### Aggregate with CRUD Generation

Add `@GenerateCrud` to generate complete CRUD operations:

```nebula
Aggregate User {
    @GenerateCrud

    Root Entity User {
        String name
        String username
    }
}
```

**Generated CRUD methods:**

```java
// UserService.java
public UserDto createUser(CreateUserRequestDto dto, UnitOfWork unitOfWork)
public UserDto getUserById(Integer id, UnitOfWork unitOfWork)
public List<UserDto> getAllUsers(UnitOfWork unitOfWork)
public UserDto updateUser(Integer id, UserDto dto, UnitOfWork unitOfWork)
public void deleteUser(Integer id, UnitOfWork unitOfWork)
```

**Generated REST endpoints:**

```java
// UserController.java
@PostMapping
public UserDto createUser(@RequestBody CreateUserRequestDto dto)

@GetMapping("/{id}")
public UserDto getUser(@PathVariable Integer id)

@GetMapping
public List<UserDto> getAllUsers()

@PutMapping("/{id}")
public UserDto updateUser(@PathVariable Integer id, @RequestBody UserDto dto)

@DeleteMapping("/{id}")
public void deleteUser(@PathVariable Integer id)
```

## Entities and Properties

### Root Entity

Every aggregate must have exactly one root entity:

```nebula
Aggregate Course {
    Root Entity Course {
        String name
        CourseType type
        Integer credits
    }
}
```

**Root entity characteristics:**
- Extends `Aggregate` base class (simulator framework)
- Has `aggregateId`, `version`, `state` fields
- Manages version chain (`prev` pointer)
- Contains invariants

### Property Types

**Primitives:**
```nebula
String name
Integer count
Long timestamp
Float price
Double percentage
Boolean active
```

**Temporal types:**
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

**Enums:**
```nebula
UserRole role
CourseType type
```

**Entities (composition):**
```nebula
ExecutionCourse course
```

### Property Modifiers

**Immutable (final):**
```nebula
final UserRole role        // Cannot be changed after creation
final LocalDateTime createdAt
```

**Default values:**
```nebula
Boolean active = true
Integer count = 0
AggregateState state = AggregateState.ACTIVE
```

**DTO exclusion:**
```nebula
String internalId dto-exclude    // Not included in DTOs
```

**Multiple properties:**
```nebula
String firstName, lastName, email    // Same type shorthand
```

### Non-Root Entities

Aggregates can have child entities:

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

**Non-root entity characteristics:**
- Do NOT extend `Aggregate`
- Simple JPA entities with `@Embeddable` or `@Entity`
- No version chain
- No invariants (only root entities have invariants)

## Cross-Aggregate References

### Basic Mapping

Reference entities from other aggregates with type inference:

```nebula
// In execution.nebula
Entity ExecutionCourse from Course {
    map name as courseName      // String (inferred from Course.name)
    map type as courseType      // CourseType (inferred from Course.type)
}
```

**What happens:**
1. Parser identifies `Course` as referenced aggregate
2. Generator finds `Course` root entity
3. Types are inferred from `Course.name` and `Course.type`
4. Generated entity has correctly typed fields

**Generated code:**
```java
@Embeddable
public class ExecutionCourse {
    private Integer courseAggregateId;  // Always included
    private Integer courseVersion;      // Always included
    private String courseName;          // Inferred: String
    private CourseType courseType;      // Inferred: CourseType
}
```

### Cross-File Type Inference

Type inference works across file boundaries:

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
    map username as userUsername  // String (from user.nebula)
    map active as userActive      // Boolean (from user.nebula)
}
```

**Model registry:**
The code generator maintains a global registry of all aggregates from all files, enabling cross-file type resolution.

### Base Fields

All cross-aggregate entities automatically include:

```java
private Integer <aggregate>AggregateId;  // e.g., courseAggregateId
private Integer <aggregate>Version;      // e.g., courseVersion
```

**Example:**
```nebula
Entity ExecutionCourse from Course {
    map name as courseName
}
```

**Generated:**
```java
public class ExecutionCourse {
    private Integer courseAggregateId;  // Auto-added
    private Integer courseVersion;      // Auto-added
    private String courseName;          // Mapped
}
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

**Generated event class:**
```java
public class UserDeletedEvent extends Event {
    private Integer userId;
    private String username;
    private UserRole role;

    // Constructor, getters, setters
}
```

**Event publishing (in service):**
```java
public void deleteUser(Integer id, UnitOfWork unitOfWork) {
    User user = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
    user.remove();
    unitOfWorkService.registerChanged(user, unitOfWork);

    UserDeletedEvent event = new UserDeletedEvent(
        user.getAggregateId(),
        user.getUsername(),
        user.getRole()
    );
    unitOfWork.registerEvent(event);
}
```

### Subscribed Events

Subscribe to events from other aggregates:

**Simple subscription:**
```nebula
Events {
    subscribe CourseDeletedEvent
}
```

**Subscription with source:**
```nebula
Events {
    subscribe UserDeletedEvent from User
}
```

**Subscription with routing:**
```nebula
Events {
    subscribe UserUpdatedEvent from User routing (users.userAggregateId)
}
```

**Routing explanation:**
- `users.userAggregateId` - Check if event's aggregateId matches any user in `users` set
- Only deliver event if match found
- Prevents unnecessary event processing

**Subscription with condition:**
```nebula
Events {
    subscribe CourseDeletedEvent from Course {
        course.courseAggregateId == event.aggregateId
    }
}
```

**Generated event subscription:**
```java
public class ExecutionSubscribesCourseDeleted implements EventSubscription {
    private Execution execution;

    public ExecutionSubscribesCourseDeleted(Execution execution) {
        this.execution = execution;
    }

    @Override
    public Set<Integer> getSubscriptionIds() {
        return Set.of(execution.getCourse().getCourseAggregateId());
    }
}
```

### Inter-Invariants

Group related subscriptions for referential integrity:

```nebula
Events {
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

**Purpose:**
- Enforce referential integrity across aggregates
- React to changes in referenced aggregates
- Update or validate local data when remote aggregate changes

**Real example from execution.nebula:**
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

## Invariants

### Simple Invariants

Define business rules enforced at aggregate level:

```nebula
Root Entity User {
    String name
    String username
    UserRole role

    invariants {
        check nameNotBlank { name.length() > 0 }
            error "User name cannot be blank"

        check usernameNotBlank { username.length() > 0 }
            error "Username cannot be blank"

        check roleNotNull { role != null }
            error "User role is required"
    }
}
```

**Generated code:**
```java
private boolean invariantNameNotBlank() {
    return this.name != null && this.name.length() > 0;
}

private boolean invariantUsernameNotBlank() {
    return this.username != null && this.username.length() > 0;
}

private boolean invariantRoleNotNull() {
    return this.role != null;
}

@Override
public void verifyInvariants() {
    if (!(invariantNameNotBlank()
           && invariantUsernameNotBlank()
           && invariantRoleNotNull())) {
        throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
    }
}
```

**When verified:**
```java
// In UnitOfWorkService.registerChanged() - line 212
aggregate.verifyInvariants();  // Throws exception if violated
```

### Complex Invariants

**Temporal constraints:**
```nebula
Root Entity Quiz {
    LocalDateTime availableDate
    LocalDateTime conclusionDate
    LocalDateTime resultsDate

    invariants {
        check dateOrdering {
            availableDate.isBefore(conclusionDate) &&
            conclusionDate.isBefore(resultsDate)
        }
            error "Dates must be in chronological order: available < conclusion < results"
    }
}
```

**Collection constraints:**
```nebula
Root Entity Question {
    Set<QuestionTopic> topics

    invariants {
        check topicsNotEmpty { topics != null && !topics.isEmpty() }
            error "Question must have at least one topic"
    }
}
```

**Relational constraints:**
```nebula
Root Entity Tournament {
    LocalDateTime startTime
    LocalDateTime endTime

    invariants {
        check startTimeBeforeEndTime { startTime.isBefore(endTime) }
            error "Tournament start time must be before end time"
    }
}
```

### Supported Expression Types

**Comparisons:**
```nebula
age >= 18
count < maxCount
price <= budget
```

**Boolean logic:**
```nebula
active && verified
role == ADMIN || role == TEACHER
!deleted
```

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

**Generated interface:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(UserRole role);
    List<User> findByActiveTrue();
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

    @Query("select q from Quiz q where q.execution.executionAggregateId = :executionId")
    List<Quiz> findByExecutionId(Integer executionId)
}
```

**Parameter binding:**
```nebula
@Query("select u from User u where u.role = :role and u.active = :active")
List<User> findByRoleAndActive(UserRole role, Boolean active)
```

**Real example from execution.nebula:**
```nebula
Repository {
    @Query("select e1.aggregateId from Execution e1 where e1.aggregateId NOT IN (select e2.aggregateId from Execution e2 where e2.state = 'DELETED' AND e2.sagaState != 'NOT_IN_SAGA')")
    Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga()
}
```

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

**Generated service:**
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final UnitOfWorkService unitOfWorkService;

    public UserDto createUser(CreateUserRequestDto dto, UnitOfWork unitOfWork) { ... }
    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) { ... }
    public List<UserDto> getAllUsers(UnitOfWork unitOfWork) { ... }
    public UserDto updateUser(Integer id, UserDto dto, UnitOfWork unitOfWork) { ... }
    public void deleteUser(Integer id, UnitOfWork unitOfWork) { ... }
}
```

### Custom Service Methods

Define additional methods in DSL:

```nebula
Service UserService {
    @Transactional

    methods {
        activateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        deactivateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        findUsersByRole(UserRole role, UnitOfWork unitOfWork): List<UserDto>
    }
}
```

**Generated service with custom methods:**
```java
@Service
public class UserService {
    // CRUD methods (if @GenerateCrud)
    // ...

    public UserDto activateUser(Integer userId, UnitOfWork unitOfWork) {
        // Implementation generated or manual
    }

    public UserDto deactivateUser(Integer userId, UnitOfWork unitOfWork) {
        // Implementation generated or manual
    }

    public List<UserDto> findUsersByRole(UserRole role, UnitOfWork unitOfWork) {
        // Implementation generated or manual
    }
}
```

## Web API

### Auto-Generated Endpoints

`@GenerateCrud` creates REST endpoints:

```nebula
Aggregate User {
    @GenerateCrud
    // Generates POST, GET, GET all, PUT, DELETE
}
```

**Generated controller:**
```java
@RestController
@RequestMapping("/users")
public class UserController {
    @PostMapping
    public UserDto createUser(@RequestBody CreateUserRequestDto dto) { ... }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Integer id) { ... }

    @GetMapping
    public List<UserDto> getAllUsers() { ... }

    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable Integer id, @RequestBody UserDto dto) { ... }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Integer id) { ... }
}
```

### Custom Endpoints

Define custom REST endpoints in DSL (future feature):

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

### Enum Definition

Define enums shared across aggregates:

```nebula
SharedEnums {
    enum UserRole {
        STUDENT, TEACHER, ADMIN
    }

    enum CourseType {
        TECNICO, EXTERNAL
    }

    enum QuestionType {
        MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER
    }
}
```

**Generated Java enum:**
```java
public enum UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}
```

**Usage in entities:**
```nebula
Aggregate User {
    Root Entity User {
        UserRole role
    }
}
```

### Real Example from TeaStore

```nebula
SharedEnums {
    enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER
    }
}
```

## Exception Messages

### Custom Error Messages

Define project-specific error messages:

```nebula
exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
    INVALID_USERNAME: "Username '%s' is invalid"
    DUPLICATE_EMAIL: "Email '%s' is already registered"
    INSUFFICIENT_PERMISSIONS: "User does not have permission to %s"
}
```

**Generated exception class:**
```java
public class AnswersException extends SimulatorException {
    public static final String USER_NOT_FOUND = "User with ID %d not found";
    public static final String INVALID_USERNAME = "Username '%s' is invalid";
    public static final String DUPLICATE_EMAIL = "Email '%s' is already registered";
    public static final String INSUFFICIENT_PERMISSIONS = "User does not have permission to %s";
}
```

**Usage:**
```java
throw new AnswersException(USER_NOT_FOUND, userId);
throw new AnswersException(INVALID_USERNAME, username);
```

## References (Referential Integrity)

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

**Actions:**

1. **`prevent`** - Throw exception if referenced aggregate is deleted
   ```nebula
   course -> Course {
       onDelete: prevent
       message: "Cannot delete course that has executions"
   }
   ```

2. **`setNull`** - Set reference to null when referenced aggregate is deleted
   ```nebula
   creator -> User {
       onDelete: setNull
       message: "Creator deleted"
   }
   ```

3. **`cascade`** - Delete this aggregate when referenced aggregate is deleted
   ```nebula
   parent -> Category {
       onDelete: cascade
       message: "Parent category deleted"
   }
   ```

**Generated event handler:**
```java
// For prevent action
public void handleCourseDeleted(CourseDeletedEvent event) {
    if (this.course.getCourseAggregateId().equals(event.getAggregateId())) {
        throw new SimulatorException(
            "Cannot delete course that has executions",
            event.getAggregateId()
        );
    }
}

// For setNull action
public void handleUserDeleted(UserDeletedEvent event) {
    if (this.creator != null &&
        this.creator.getUserAggregateId().equals(event.getAggregateId())) {
        this.creator = null;
    }
}
```

## Complete Example: Execution Aggregate

Putting it all together - a real-world example from the Answers project:

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

**This single file generates:**
- 3 JPA entities (`Execution`, `ExecutionCourse`, `ExecutionUser`)
- 3 DTOs (`ExecutionDto`, `CreateExecutionRequestDto`, `UpdateExecutionRequestDto`)
- 1 factory interface (`ExecutionFactory`)
- 2 repositories (`ExecutionRepository`, `ExecutionCustomRepository`)
- 1 service (`ExecutionService`) with CRUD methods
- 1 controller (`ExecutionController`) with REST endpoints
- Event subscriptions and handlers for `CourseDeletedEvent`, `UserDeletedEvent`, `UserUpdatedEvent`
- Invariant validation methods
- Saga workflows for create/update/delete operations
- ~2,000 lines of Java code from ~65 lines of DSL

## Next Steps

Explore deeper topics:

- **[05-Generation-Pipeline](05-Generation-Pipeline.md)** - How code is generated from DSL
- **[08-Microservices-Generators](08-Microservices-Generators.md)** - Deep dive into generators
- **[11-Grammar-Reference](11-Grammar-Reference.md)** - Complete syntax reference

---

**Previous:** [03-Grammar-Overview](03-Grammar-Overview.md) | **Next:** [05-Generation-Pipeline](05-Generation-Pipeline.md)
