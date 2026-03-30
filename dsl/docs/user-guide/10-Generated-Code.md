# Generated Code

This chapter explains what Nebula generates, how the generated code is structured, and how the key patterns work. Examples use the **tutorial project** (Member, Book, Loan) for simplicity.

> For the DSL syntax that produces this code, see chapters [03](03-Your-First-Aggregate.md)-[09](09-Advanced-Patterns.md).

## Generated Directory Structure

For each project, Nebula generates a complete Spring Boot application. The structure shown below uses the **Member** aggregate as a representative example; Book and Loan follow the same pattern within their own `microservices/<aggregate>/` directories:

```
dsl/docs/examples/generated/06-tutorial/
├── pom.xml                                        # Maven build configuration
├── .gitignore
└── src/main/
    ├── java/pt/ulisboa/tecnico/socialsoftware/tutorial/
    │   ├── TutorialSimulator.java                 # Main application class
    │   ├── ServiceMapping.java                    # Service routing
    │   ├── command/                               # Command objects (CQRS)
    │   │   ├── member/
    │   │   │   ├── CreateMemberCommand.java
    │   │   │   ├── GetMemberByIdCommand.java
    │   │   │   └── ...                            # (one per CRUD operation)
    │   │   ├── book/
    │   │   └── loan/
    │   ├── coordination/
    │   │   ├── validation/                        # Business rule validation
    │   │   │   ├── MemberBusinessRuleValidator.java
    │   │   │   ├── MemberInvariants.java
    │   │   │   ├── MemberValidationAnnotations.java
    │   │   │   ├── ValidMemberBusinessRule.java
    │   │   │   └── ...                            # (one set per aggregate)
    │   │   └── webapi/
    │   │       ├── BehaviourController.java       # Simulator behaviour API
    │   │       └── TracesController.java          # Tracing API
    │   ├── events/                                # Domain events
    │   │   ├── MemberDeletedEvent.java
    │   │   ├── MemberUpdatedEvent.java
    │   │   ├── BookDeletedEvent.java
    │   │   └── ...
    │   ├── microservices/                         # Domain layer (per aggregate)
    │   │   ├── exception/
    │   │   │   ├── TutorialErrorMessage.java
    │   │   │   ├── TutorialException.java
    │   │   │   └── TutorialExceptionHandler.java
    │   │   ├── member/
    │   │   │   ├── aggregate/
    │   │   │   │   ├── Member.java                # JPA entity
    │   │   │   │   ├── MemberFactory.java         # Factory interface
    │   │   │   │   ├── MemberRepository.java      # Spring Data interface
    │   │   │   │   ├── MemberCustomRepository.java
    │   │   │   │   └── sagas/                     # Saga-specific variants
    │   │   │   │       ├── SagaMember.java
    │   │   │   │       ├── dtos/SagaMemberDto.java
    │   │   │   │       ├── factories/SagasMemberFactory.java
    │   │   │   │       ├── repositories/MemberCustomRepositorySagas.java
    │   │   │   │       └── states/MemberSagaState.java
    │   │   │   ├── commandHandler/
    │   │   │   │   ├── MemberCommandHandler.java
    │   │   │   │   └── MemberStreamCommandHandler.java
    │   │   │   ├── coordination/
    │   │   │   │   ├── eventProcessing/
    │   │   │   │   │   └── MemberEventProcessing.java
    │   │   │   │   ├── functionalities/
    │   │   │   │   │   └── MemberFunctionalities.java
    │   │   │   │   ├── sagas/
    │   │   │   │   │   ├── CreateMemberFunctionalitySagas.java
    │   │   │   │   │   ├── GetMemberByIdFunctionalitySagas.java
    │   │   │   │   │   ├── GetAllMembersFunctionalitySagas.java
    │   │   │   │   │   ├── UpdateMemberFunctionalitySagas.java
    │   │   │   │   │   └── DeleteMemberFunctionalitySagas.java
    │   │   │   │   └── webapi/
    │   │   │   │       ├── MemberController.java  # REST controller
    │   │   │   │       └── requestDtos/
    │   │   │   │           └── CreateMemberRequestDto.java
    │   │   │   └── service/
    │   │   │       └── MemberService.java         # Business logic
    │   │   ├── book/                              # Same structure as member
    │   │   └── loan/                              # + events/handling/ + events/subscribe/
    │   └── shared/                                # Shared components
    │       ├── dtos/
    │       │   ├── MemberDto.java
    │       │   ├── BookDto.java
    │       │   ├── LoanDto.java
    │       │   ├── LoanMemberDto.java
    │       │   └── LoanBookDto.java
    │       └── enums/
    │           └── MembershipType.java
    └── resources/
        └── application.yml
```

## What Gets Generated Per Aggregate

For an aggregate with `@GenerateCrud` ([Chapter 03](03-Your-First-Aggregate.md)):

| Component | Files | Purpose |
|-----------|-------|---------|
| Entity | 1-3 | JPA entities (root + child entities) |
| Saga entity | 1 | Saga-specific aggregate variant |
| DTO | 2-4 | DTOs (Dto, CreateRequestDto, SagaDto, child Dtos) |
| Factory | 1-2 | Factory interface + saga factory implementation |
| Repository | 2-3 | Spring Data interface + custom repository + saga repository |
| Service | 1 | CRUD + collection + custom methods |
| Controller | 1 | REST endpoints |
| Functionalities | 1 | Orchestration layer |
| Event Processing | 1 | Event coordination |
| Commands | 5 | CQRS command objects (one per CRUD operation) |
| Command Handlers | 2 | Command dispatch + stream dispatch |
| Saga State | 1 | Saga state tracking |
| Events | N | Domain events (deleted + updated per aggregate) |
| Event Handlers | M | Event handlers (one per subscription) |
| Sagas | 5 | Saga workflows (one per CRUD operation) |
| Validation | 3-4 | Business rule validators and annotations |

**Typical totals:** 25-35+ files per aggregate, depending on complexity.

## Key Generated Files

### Entity (`User.java`)

Root entities extend the simulator's `Aggregate` base class:

```java
@Entity
public class User extends Aggregate {
    private String name;
    private String username;
    private UserRole role;
    private Boolean active = true;

    public User() { super(); }

    public User(Integer aggregateId, UserDto dto) {
        setAggregateId(aggregateId);
        setName(dto.getName());
        setUsername(dto.getUsername());
        setRole(dto.getRole());
        setActive(dto.getActive());
    }

    public User(User other) {
        super(other);
        setName(other.getName());
        setUsername(other.getUsername());
        setRole(other.getRole());
        setActive(other.getActive());
    }

    // Getters, setters, invariant verification
}
```

Three constructors are generated:
1. **Default** — for JPA
2. **DTO constructor** — for creating from request data
3. **Copy constructor** — for the immutable update pattern

### DTO (`UserDto.java`)

DTOs include base fields plus entity properties (minus `dto-exclude` fields — see [Chapter 04](04-Types-Enums-Properties.md)):

```java
public class UserDto {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String username;
    private UserRole role;
    private Boolean active;

    // No-arg constructor, getters, setters
}
```

### Factory (`UserFactory.java`)

Factory interface for creating entities and DTOs:

```java
public interface UserFactory {
    User createUser(Integer aggregateId, UserDto dto);
    User createUserFromExisting(User existingUser);
    UserDto createUserDto(User user);
}
```

### Service (`UserService.java`)

Business logic with the Unit of Work pattern:

```java
@Service
public class UserService {
    private final UserFactory userFactory;
    private final UnitOfWorkService unitOfWorkService;

    public UserDto createUser(CreateUserRequestDto dto, UnitOfWork unitOfWork) {
        User user = userFactory.createUser(null, dto);
        user = unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
        User user = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        return userFactory.createUserDto(user);
    }

    public UserDto updateUser(Integer id, UserDto dto, UnitOfWork unitOfWork) {
        User oldUser = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setName(dto.getName());
        newUser.setActive(dto.getActive());
        newUser = unitOfWorkService.registerChanged(newUser, unitOfWork);
        return userFactory.createUserDto(newUser);
    }

    public void deleteUser(Integer id, UnitOfWork unitOfWork) {
        User oldUser = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }
}
```

### Controller (`UserController.java`)

REST endpoints:

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

## Key Patterns

### Unit of Work

All operations go through the `UnitOfWork` for transaction tracking:

```java
public UserDto createUser(CreateUserRequestDto dto, UnitOfWork unitOfWork) {
    User user = userFactory.createUser(null, dto);
    user = unitOfWorkService.registerChanged(user, unitOfWork);  // Track change
    return userFactory.createUserDto(user);
}
```

### Immutable Updates

Updates create a new version of the aggregate (copy-on-write):

```java
User oldUser = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
User newUser = userFactory.createUserFromExisting(oldUser);  // Copy
newUser.setName(dto.getName());                               // Modify copy
newUser = unitOfWorkService.registerChanged(newUser, unitOfWork);
```

### Event Publishing

Events are registered with the Unit of Work and emitted on commit (see [Chapter 07](07-Events-Reactive-Patterns.md)):

```java
UserDeletedEvent event = new UserDeletedEvent(user.getAggregateId());
unitOfWork.registerEvent(event);
```

### Event Subscription

Aggregates declare subscriptions via `getEventSubscriptions()`:

```java
public class Execution extends Aggregate {
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> subscriptions = new HashSet<>();
        subscriptions.add(new ExecutionSubscribesUserDeleted(this));
        return subscriptions;
    }
}
```

### Invariant Verification

Invariants ([Chapter 05](05-Business-Rules-Repositories.md)) are verified when changes are registered. Each invariant is checked individually with its custom error message:

```java
@Override
public void verifyInvariants() {
    if (!invariantNameNotBlank()) {
        throw new SimulatorException(INVARIANT_BREAK, "Name cannot be blank");
    }
    if (!invariantRoleNotNull()) {
        throw new SimulatorException(INVARIANT_BREAK, "Role is required");
    }
}
```

## Event Flow

Events flow through the system as follows:

```
1. Service publishes event
   unitOfWork.registerEvent(new UserDeletedEvent(...))

2. UnitOfWork commits → events dispatched

3. EventProcessing routes to subscriber aggregates
   UserEventProcessing.handleUserDeletedEvent(event)

4. Subscriber aggregate processes event
   ExecutionEventHandling.handleUserDeleted(event)

5. Handler updates local state
   execution.getUsers().removeIf(u -> u.getUserAggregateId().equals(event.getAggregateId()))
```

## Collection Operations

For `Set<T>` and `List<T>` properties, additional service methods are generated:

```java
// Add to collection
public ExecutionDto addUserToExecution(
    Integer executionId,
    ExecutionUserDto userDto,
    UnitOfWork unitOfWork
) { ... }

// Remove from collection
public ExecutionDto removeUserFromExecution(
    Integer executionId,
    Integer userId,
    UnitOfWork unitOfWork
) { ... }
```

## Code Volume

**Simple aggregate (Member, ~10 lines DSL):** ~1,200 lines Java

**Intermediate aggregate (Book, ~30 lines DSL):** ~1,500 lines Java

**Complex aggregate (Loan/Execution, ~55-65 lines DSL):** ~2,000 lines Java

**Reduction ratio:** ~60-75% less code to write.

> For details on how the generators produce this code, see the [Developer Guide](../developer-guide/03-Generator-System.md).

---

**Previous:** [09-Advanced-Patterns](09-Advanced-Patterns.md) | **Next:** [11-Reference](11-Reference.md)
