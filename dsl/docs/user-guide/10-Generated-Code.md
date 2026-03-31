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
        ├── application.properties
        ├── application.yml
        ├── database.properties
        └── logback-spring.xml
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

### Entity (`Member.java`)

Root entities extend the simulator's `Aggregate` base class. They are generated as `abstract` classes, with a concrete saga subclass (`SagaMember`) for distributed transaction support:

```java
@Entity
public abstract class Member extends Aggregate {
    private String name;
    private String email;
    @Enumerated(EnumType.STRING)
    private MembershipType membership;

    public Member() { super(); }

    public Member(Integer aggregateId, MemberDto dto) {
        setAggregateId(aggregateId);
        setName(dto.getName());
        setEmail(dto.getEmail());
        setMembership(dto.getMembership());
    }

    public Member(Member other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());
        setMembership(other.getMembership());
    }

    // Getters, setters, verifyInvariants(), buildDto()
}
```

Three constructors are generated:
1. **Default**:for JPA
2. **DTO constructor**:for creating from request data
3. **Copy constructor**:for the immutable update pattern

A saga-specific subclass (`SagaMember extends Member`) is also generated with saga state tracking.

### DTO (`UserDto.java`)

DTOs include base fields plus entity properties (minus `dto-exclude` fields, see [Chapter 04](04-Types-Enums-Properties.md)):

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

### Service (`MemberService.java`)

Business logic with the Unit of Work pattern. The create method generates a new aggregate ID, converts the request DTO, and registers the change:

```java
@Service
public class MemberService {
    @Autowired
    private MemberFactory memberFactory;
    @Autowired
    private UnitOfWorkService unitOfWorkService;
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    public MemberDto createMember(CreateMemberRequestDto createRequest, UnitOfWork unitOfWork) {
        MemberDto memberDto = new MemberDto();
        memberDto.setName(createRequest.getName());
        memberDto.setEmail(createRequest.getEmail());
        memberDto.setMembership(createRequest.getMembership());

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Member member = memberFactory.createMember(aggregateId, memberDto);
        unitOfWorkService.registerChanged(member, unitOfWork);
        return memberFactory.createMemberDto(member);
    }

    public MemberDto updateMember(MemberDto memberDto, UnitOfWork unitOfWork) {
        Integer id = memberDto.getAggregateId();
        Member oldMember = (Member) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        Member newMember = memberFactory.createMemberFromExisting(oldMember);
        // Set updatable fields from DTO...
        unitOfWorkService.registerChanged(newMember, unitOfWork);
        // Register update event
        MemberUpdatedEvent event = new MemberUpdatedEvent(newMember.getAggregateId(), ...);
        event.setPublisherAggregateVersion(newMember.getVersion());
        unitOfWorkService.registerEvent(event, unitOfWork);
        return memberFactory.createMemberDto(newMember);
    }

    public void deleteMember(Integer aggregateId, UnitOfWork unitOfWork) {
        Member oldMember = (Member) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Member newMember = memberFactory.createMemberFromExisting(oldMember);
        newMember.remove();
        unitOfWorkService.registerChanged(newMember, unitOfWork);
        unitOfWorkService.registerEvent(new MemberDeletedEvent(newMember.getAggregateId()), unitOfWork);
    }
}
```

### Controller (`MemberController.java`)

REST endpoints. Note that the create path includes `/create`, update takes the ID from the request body, and path variables use the full `{memberAggregateId}` naming:

```java
@RestController
public class MemberController {
    @Autowired
    private MemberFunctionalities memberFunctionalities;

    @PostMapping("/members/create")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDto createMember(@RequestBody CreateMemberRequestDto createRequest) { ... }

    @GetMapping("/members/{memberAggregateId}")
    public MemberDto getMemberById(@PathVariable Integer memberAggregateId) { ... }

    @GetMapping("/members")
    public List<MemberDto> getAllMembers() { ... }

    @PutMapping("/members")
    public MemberDto updateMember(@RequestBody MemberDto memberDto) { ... }

    @DeleteMapping("/members/{memberAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable Integer memberAggregateId) { ... }
}
```

## Key Patterns

### Unit of Work

All operations go through the `UnitOfWork` for transaction tracking:

```java
public MemberDto createMember(CreateMemberRequestDto createRequest, UnitOfWork unitOfWork) {
    Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
    Member member = memberFactory.createMember(aggregateId, memberDto);
    unitOfWorkService.registerChanged(member, unitOfWork);  // Track change
    return memberFactory.createMemberDto(member);
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

Events are registered via the Unit of Work Service and emitted on commit (see [Chapter 07](07-Events-Reactive-Patterns.md)):

```java
MemberDeletedEvent event = new MemberDeletedEvent(member.getAggregateId());
event.setPublisherAggregateVersion(member.getVersion());
unitOfWorkService.registerEvent(event, unitOfWork);
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

Events flow through the system via scheduled polling:

```
1. Service registers event
   unitOfWorkService.registerEvent(new MemberDeletedEvent(...), unitOfWork)

2. UnitOfWork commits → event persisted

3. EventProcessing polls for new events (@Scheduled)
   eventApplicationService.handleSubscribedEvent(...)

4. Subscription classes match events to affected aggregates
   LoanSubscribesMemberDeleted

5. Event handlers process updates
   MemberDeletedEventHandler updates local state
```

## Collection Operations

For `Set<T>` and `List<T>` properties referencing cross-aggregate entities, five additional service methods are generated:

```java
// Add single item
public EnrollmentDto addEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId,
    EnrollmentTeacherDto dto, UnitOfWork unitOfWork) { ... }

// Add multiple items
public EnrollmentDto addEnrollmentTeachers(Integer enrollmentId,
    List<EnrollmentTeacherDto> dtos, UnitOfWork unitOfWork) { ... }

// Get single item
public EnrollmentTeacherDto getEnrollmentTeacher(Integer enrollmentId,
    Integer teacherAggregateId, UnitOfWork unitOfWork) { ... }

// Update item
public EnrollmentDto updateEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId,
    EnrollmentTeacherDto dto, UnitOfWork unitOfWork) { ... }

// Remove item
public EnrollmentDto removeEnrollmentTeacher(Integer enrollmentId,
    Integer teacherAggregateId, UnitOfWork unitOfWork) { ... }
```

## Code Volume

**Simple aggregate (Member, ~10 lines DSL):** ~1,200 lines Java

**Intermediate aggregate (Book, ~30 lines DSL):** ~1,500 lines Java

**Complex aggregate (Loan/Execution, ~55-65 lines DSL):** ~2,000 lines Java

**Reduction ratio:** ~60-75% less code to write.

> For details on how the generators produce this code, see the [Developer Guide](../developer-guide/03-Generator-System.md).

---

**Previous:** [09-Advanced-Patterns](09-Advanced-Patterns.md) | **Next:** [11-Reference](11-Reference.md)
