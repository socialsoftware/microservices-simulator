# Step 5 · Services and CRUD Generation

[📚 Guide Index](00-index.md)

> **Goal:** configure service layer generation with `@GenerateCrud` and understand the generated service patterns.

[← Back to Step 4](04-repositories.md) · [Next → Step 6](06-web-api.md)

---

## 5.1 Basic Service Generation

Every aggregate gets a service class that coordinates business operations:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }

    Service UserService {
        // Service configuration goes here
    }
}
```

```java
@Service
public class UserService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    public UserService(UnitOfWorkService unitOfWorkService, UserRepository userRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.userRepository = userRepository;
    }
}
```

---

## 5.2 Automatic CRUD Generation

The `@GenerateCrud` annotation generates complete CRUD operations:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }

    Service UserService {
        @GenerateCrud;
    }
}
```

This generates five standard methods:

```java
@Service
public class UserService {
    // ... dependencies ...

    @Retryable(
        value = { SQLException.class, CannotAcquireLockException.class },
        maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    @Retryable(...)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserById(Integer aggregateId, UnitOfWork unitOfWork) {
        return userFactory.createUserDto(
            (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork)
        );
    }

    @Retryable(...)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto updateUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = userDto.getAggregateId();
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setName(userDto.getName());
        newUser.setUsername(userDto.getUsername());
        newUser.setActive(userDto.getActive());
        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(new UserUpdatedEvent(newUser.getAggregateId(), ...), unitOfWork);
        return userFactory.createUserDto(newUser);
    }

    @Retryable(...)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUser(Integer aggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(new UserDeletedEvent(newUser.getAggregateId()), unitOfWork);
    }

    @Retryable(...)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getAllUsers(UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = userRepository.findAll().stream()
            .map(User::getAggregateId)
            .collect(Collectors.toSet());
        return aggregateIds.stream()
            .map(id -> (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
            .map(userFactory::createUserDto)
            .collect(Collectors.toList());
    }
}
```

---

## 5.3 Understanding the Generated Patterns

### Unit of Work Pattern

All operations use the Unit of Work pattern for transaction management:

```java
// Load and track the aggregate
User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);

// Register changes for commit
unitOfWorkService.registerChanged(newUser, unitOfWork);

// Register events to publish after commit
unitOfWorkService.registerEvent(new UserUpdatedEvent(...), unitOfWork);
```

### Factory Pattern

Entity creation goes through a factory that handles:
- Constructor selection based on parameters
- DTO-to-entity mapping
- Copy construction for updates

```java
// Create new aggregate
User user = userFactory.createUser(aggregateId, userDto);

// Create copy for modification (immutability pattern)
User newUser = userFactory.createUserFromExisting(oldUser);

// Create DTO from entity
UserDto dto = userFactory.createUserDto(user);
```

### Retry and Transaction Annotations

```java
@Retryable(
    value = { SQLException.class, CannotAcquireLockException.class },
    maxAttemptsExpression = "${retry.db.maxAttempts}",
    backoff = @Backoff(
        delayExpression = "${retry.db.delay}",
        multiplierExpression = "${retry.db.multiplier}"
    ))
@Transactional(isolation = Isolation.SERIALIZABLE)
```

- **@Retryable**: Handles transient database failures with exponential backoff
- **@Transactional(SERIALIZABLE)**: Ensures strong isolation for saga consistency

---

## 5.4 Cross-Aggregate Service Operations

When your aggregate references external DTOs, the create method handles the relationships:

```nebula
Aggregate Execution {
    Entity ExecutionCourse uses dto CourseDto mapping {
        aggregateId -> courseAggregateId;
        name -> courseName;
    } {
        Integer courseAggregateId;
        String courseName;
    }

    Root Entity Execution {
        String acronym;
        ExecutionCourse course;
    }

    Service ExecutionService {
        @GenerateCrud;
    }
}
```

The generated create method expects the course DTO to be embedded in the execution DTO:

```java
public ExecutionDto createExecution(ExecutionDto executionDto, UnitOfWork unitOfWork) {
    Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
    // The factory handles extracting ExecutionCourse from executionDto
    Execution execution = executionFactory.createExecution(aggregateId, executionDto);
    unitOfWorkService.registerChanged(execution, unitOfWork);
    return executionFactory.createExecutionDto(execution);
}
```

---

## 5.5 Service + WebAPI Combined Pattern

For complete functionality, pair the service with WebAPI endpoints:

```nebula
Aggregate Quiz {
    Root Entity Quiz {
        String title;
        QuizType quizType;
        LocalDateTime creationDate;
    }

    WebAPIEndpoints {
        @GenerateCrud;
    }

    Service QuizService {
        @GenerateCrud;
    }
}
```

This generates:
1. **QuizService** - Business logic and data access
2. **QuizFunctionalities** - Coordination layer that orchestrates services
3. **QuizController** - REST endpoints that call functionalities

The separation allows the saga coordination layer to intercept and manage distributed transactions.

---

## 5.6 Custom Service Methods

You can add custom methods alongside CRUD:

```nebula
Service QuizService {
    @GenerateCrud;
    
    methods {
        findQuizzesByExecution(Integer executionId, UnitOfWork unitOfWork): List<QuizDto>;
        updateQuizDates(Integer quizId, LocalDateTime availableDate, LocalDateTime conclusionDate, UnitOfWork unitOfWork): QuizDto;
    }
}
```

Custom methods are declared but require manual implementation. The generator creates method stubs with the correct signatures.

---

## Recap

The service layer bridges your domain model with the coordination infrastructure. `@GenerateCrud` provides standard operations with proper transaction handling, retry logic, and event publishing. The Unit of Work and Factory patterns ensure aggregate consistency and support the saga architecture's requirements.

[← Back to Step 4](04-repositories.md) · [Next → Step 6](06-web-api.md)
