# Microservices Generators

This chapter details the generators responsible for the domain layer: entities, DTOs, repositories, services, factories, and event handling.

## Entity Generator

**File:** `src/cli/generators/microservices/entity/entity-orchestrator.ts`

### Responsibilities

- Generate JPA entities from DSL
- Handle root vs. non-root entities
- Generate constructors (default, DTO, copy)
- Generate getters/setters
- Compile invariants to Java code
- Manage entity imports

### Generation Process

```typescript
export class EntityOrchestrator extends GeneratorBase {
    async generateEntity(entity: Entity, options: GenerationOptions): Promise<string> {
        // 1. Build package and imports
        const packageName = this.generatePackageName(options.projectName, ...) const imports = this.buildImports(entity);

        // 2. Build class signature
        const className = entity.name;
        const extendsClause = entity.isRoot ? 'extends Aggregate' : '';

        // 3. Build fields
        const fields = this.buildFields(entity);

        // 4. Build constructors
        const constructors = this.buildConstructors(entity);

        // 5. Build getters/setters
        const methods = this.buildMethods(entity);

        // 6. Build invariants (if root entity)
        const invariants = entity.isRoot ? this.buildInvariants(entity) : '';

        // 7. Assemble and return
        return this.assembleEntity({
            packageName, imports, className, extendsClause,
            fields, constructors, methods, invariants
        });
    }
}
```

### Field Generation

```typescript
private buildFields(entity: Entity): Field[] {
    return entity.properties.map(prop => ({
        annotations: this.buildFieldAnnotations(prop),
        type: this.resolveJavaType(prop.type),
        name: prop.name,
        final: prop.isFinal || false
    }));
}

private buildFieldAnnotations(prop: Property): string[] {
    const annotations: string[] = [];

    if (this.isCollectionType(prop.type)) {
        annotations.push('@OneToMany(cascade = CascadeType.ALL)');
    }
    if (this.isEntityType(prop.type)) {
        annotations.push('@ManyToOne');
    }

    return annotations;
}
```

**Generated field:**
```java
@OneToMany(cascade = CascadeType.ALL)
private Set<ExecutionUser> users;
```

### Constructor Generation

**File:** `src/cli/generators/microservices/entity/constructors.ts`

Three constructors generated:

1. **Default constructor:**
```java
public User() {
    super();
}
```

2. **DTO constructor:**
```java
public User(Integer aggregateId, UserDto dto) {
    setAggregateId(aggregateId);
    setName(dto.getName());
    setUsername(dto.getUsername());
    setRole(dto.getRole());
}
```

3. **Copy constructor (immutable pattern):**
```java
public User(User other) {
    super(other);  // Sets prev = other
    setName(other.getName());
    setUsername(other.getUsername());
    setRole(other.getRole());
    // Deep copy collections
    setUsers(other.getUsers().stream()
        .map(ExecutionUser::new)
        .collect(Collectors.toSet()));
}
```

### Invariant Compilation

**File:** `src/cli/generators/microservices/entity/invariants.ts`

```typescript
export class InvariantCompiler {
    compileInvariants(entity: Entity): string {
        const invariantMethods = entity.invariants.map(inv =>
            this.compileInvariantMethod(inv, entity)
        );

        const verifyMethod = this.buildVerifyInvariantsMethod(entity);

        return invariantMethods.join('\n\n') + '\n\n' + verifyMethod;
    }

    private compileInvariantMethod(invariant: Invariant, entity: Entity): string {
        const condition = this.convertDslToJava(invariant.expression, entity);

        return `
private boolean invariant${this.capitalize(invariant.name)}() {
    return ${condition};
}`;
    }

    private buildVerifyInvariantsMethod(entity: Entity): string {
        const checks = entity.invariants
            .map(inv => `invariant${this.capitalize(inv.name)}()`)
            .join(' &&\n           ');

        return `
@Override
public void verifyInvariants() {
    if (!(${checks})) {
        throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
    }
}`;
    }
}
```

**DSL:**
```nebula
invariants {
    check nameNotBlank { name.length() > 0 }
        error "Name cannot be blank"

    check roleNotNull { role != null }
        error "Role is required"
}
```

**Generated Java:**
```java
private boolean invariantNameNotBlank() {
    return this.name != null && this.name.length() > 0;
}

private boolean invariantRoleNotNull() {
    return this.role != null;
}

@Override
public void verifyInvariants() {
    if (!(invariantNameNotBlank() &&
           invariantRoleNotNull())) {
        throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
    }
}
```

## DTO Generator

**File:** `src/cli/generators/microservices/shared/dto-generator.ts`

### Responsibilities

- Generate DTOs from entities
- Handle cross-aggregate references
- Include base fields (aggregateId, version, state)
- Exclude fields marked with `dto-exclude`

### Generation Process

```typescript
export class DtoGenerator extends GeneratorBase {
    async generateDto(entity: Entity, options: GenerationOptions): Promise<string> {
        const packageName = this.buildPackageName('shared', 'dtos');
        const className = `${entity.name}Dto`;

        const fields = this.buildDtoFields(entity);

        return this.renderTemplate('shared/dto.hbs', {
            packageName,
            className,
            fields,
            imports: this.buildImports(fields)
        });
    }

    private buildDtoFields(entity: Entity): DtoField[] {
        const fields: DtoField[] = [
            { name: 'aggregateId', type: 'Integer' },
            { name: 'version', type: 'Integer' },
            { name: 'state', type: 'AggregateState' }
        ];

        for (const prop of entity.properties) {
            if (prop.dtoExclude) continue;

            fields.push({
                name: prop.name,
                type: this.resolveJavaType(prop.type)
            });
        }

        return fields;
    }
}
```

**Generated DTO:**
```java
package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

public class UserDto {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String username;
    private UserRole role;
    private Boolean active;

    public UserDto() {}

    // Getters and setters for all fields
}
```

## Repository Generator

### Repository Interface Generator

**File:** `src/cli/generators/microservices/repository/repository-interface-generator.ts`

```typescript
export class RepositoryInterfaceGenerator extends GeneratorBase {
    async generateRepositoryInterface(
        aggregate: Aggregate,
        options: GenerationOptions
    ): Promise<string> {
        const customMethods = this.extractRepositoryMethods(aggregate);

        return this.renderTemplate('repository/repository-interface.hbs', {
            packageName: this.generatePackageName(...),
            repositoryName: `${aggregate.name}Repository`,
            entityName: aggregate.name,
            customMethods
        });
    }

    private extractRepositoryMethods(aggregate: Aggregate): RepositoryMethod[] {
        const repository = aggregate.repository;
        if (!repository) return [];

        return repository.methods.map(method => ({
            name: method.name,
            returnType: this.resolveJavaType(method.returnType),
            parameters: this.buildParameters(method.parameters),
            query: method.query
        }));
    }
}
```

**Generated repository:**
```java
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Integer> {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds();

    Optional<Execution> findByAcronym(String acronym);
}
```

### Custom Repository Generator

**File:** `src/cli/generators/microservices/repository/repository-generator.ts`

Generates custom repository implementation for complex queries.

## Service Generator

**File:** `src/cli/generators/microservices/service/default/main.ts`

### CRUD Operations

Service generator coordinates specialized generators:

```typescript
export class ServiceGenerator extends GeneratorBase {
    async generateService(aggregate: Aggregate, options: GenerationOptions): Promise<string> {
        const structure = new StructureGenerator().generateStructure(aggregate, options);

        const crudMethods = aggregate.generateCrud
            ? await new CrudGenerator().generateAll(aggregate, options)
            : '';

        const collectionMethods = await new CollectionGenerator().generateAll(aggregate, options);

        return this.assembleService(structure, crudMethods, collectionMethods);
    }
}
```

### CRUD Method Generators

**Create:** `crud-create-generator.ts`
```java
public UserDto createUser(CreateUserRequestDto dto, UnitOfWork unitOfWork) {
    try {
        User user = userFactory.createUser(null, dto);
        user = unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    } catch (Exception e) {
        throw new AnswersException(ERROR_CREATING_USER, e.getMessage());
    }
}
```

**Read:** `crud-read-generator.ts`
```java
public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
    try {
        User user = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        return userFactory.createUserDto(user);
    } catch (Exception e) {
        throw new AnswersException(ERROR_RETRIEVING_USER, id);
    }
}
```

**Update:** `crud-update-generator.ts` (Immutable Pattern)
```java
public UserDto updateUser(Integer id, UserDto dto, UnitOfWork unitOfWork) {
    try {
        User oldUser = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setName(dto.getName());
        newUser.setActive(dto.getActive());
        newUser = unitOfWorkService.registerChanged(newUser, unitOfWork);
        return userFactory.createUserDto(newUser);
    } catch (Exception e) {
        throw new AnswersException(ERROR_UPDATING_USER, id);
    }
}
```

**Delete:** `crud-delete-generator.ts` (Immutable Pattern)
```java
public void deleteUser(Integer id, UnitOfWork unitOfWork) {
    try {
        User oldUser = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWorkService.registerChanged(newUser, unitOfWork);

        UserDeletedEvent event = new UserDeletedEvent(id);
        unitOfWork.registerEvent(event);
    } catch (Exception e) {
        throw new AnswersException(ERROR_DELETING_USER, id);
    }
}
```

### Collection Operations

**File:** `src/cli/generators/microservices/service/default/collection-generator.ts`

Generated methods for collection properties:

**Add:**
```java
public ExecutionDto addUserToExecution(
    Integer executionId,
    ExecutionUserDto userDto,
    UnitOfWork unitOfWork
) {
    Execution oldExecution = unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
    Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);

    ExecutionUser user = new ExecutionUser(userDto);
    newExecution.addUser(user);

    newExecution = unitOfWorkService.registerChanged(newExecution, unitOfWork);
    return executionFactory.createExecutionDto(newExecution);
}
```

**Remove:**
```java
public ExecutionDto removeUserFromExecution(
    Integer executionId,
    Integer userId,
    UnitOfWork unitOfWork
) {
    Execution oldExecution = unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
    Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);

    newExecution.removeUser(userId);

    newExecution = unitOfWorkService.registerChanged(newExecution, unitOfWork);
    return executionFactory.createExecutionDto(newExecution);
}
```

## Factory Generator

**File:** `src/cli/generators/microservices/factory/factory-generator.ts`

### Factory Interface

**Template:** `entity/factory-interface.hbs`

```java
public interface UserFactory {
    User createUser(Integer aggregateId, UserDto dto);
    User createUserFromExisting(User existingUser);
    UserDto createUserDto(User user);
}
```

### Factory Implementation

```java
@Component
public class UserFactoryImpl implements UserFactory {
    @Override
    public User createUser(Integer aggregateId, UserDto dto) {
        return new User(aggregateId, dto);
    }

    @Override
    public User createUserFromExisting(User existingUser) {
        return new User(existingUser);  // Copy constructor
    }

    @Override
    public UserDto createUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setAggregateId(user.getAggregateId());
        dto.setVersion(user.getVersion());
        dto.setState(user.getState());
        dto.setName(user.getName());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        return dto;
    }
}
```

## Event Generator

**File:** `src/cli/generators/microservices/events/event-generator.ts`

### Published Events

**Generator:** `published-event-generator.ts`

```java
public class UserDeletedEvent extends Event {
    private Integer userId;
    private String username;

    public UserDeletedEvent(Integer userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // Getters and setters
}
```

### Event Subscriptions

**Generator:** `event-subscription-generator.ts`

```java
public class ExecutionSubscribesUserDeleted implements EventSubscription {
    private Execution execution;

    public ExecutionSubscribesUserDeleted(Execution execution) {
        this.execution = execution;
    }

    @Override
    public Set<Integer> getSubscriptionIds() {
        return execution.getUsers().stream()
            .map(ExecutionUser::getUserAggregateId)
            .collect(Collectors.toSet());
    }
}
```

### Event Handlers

**Generator:** `event-handler-generator.ts`

```java
public class UserDeletedEventHandler implements EventHandler<UserDeletedEvent> {
    private Execution execution;

    @Override
    public void handleEvent(UserDeletedEvent event, UnitOfWork unitOfWork) {
        // Update execution when user deleted
        execution.getUsers().removeIf(user ->
            user.getUserAggregateId().equals(event.getAggregateId())
        );
        unitOfWorkService.registerChanged(execution, unitOfWork);
    }
}
```

## Summary

### Generated Files per Aggregate

For an aggregate with `@GenerateCrud`:

| Component | Files | Purpose |
|-----------|-------|---------|
| **Entity** | 1-3 | JPA entities (root + child entities) |
| **DTO** | 1-3 | DTOs (Dto, CreateRequestDto, UpdateRequestDto) |
| **Factory** | 1 | Factory interface + implementation |
| **Repository** | 2 | Spring Data interface + Custom repository |
| **Service** | 1 | CRUD + collection + custom methods |
| **Events** | N | Published events (one per event) |
| **Event Handlers** | M | Event handlers (one per subscription) |
| **Total** | 10-20+ | Varies by aggregate complexity |

### Code Volume

**Example: User aggregate (simple)**
- DSL: 20 lines
- Generated Java: ~1,500 lines

**Example: Execution aggregate (complex)**
- DSL: 65 lines
- Generated Java: ~2,000 lines

**Reduction ratio:** ~60-75% less code to write

## Next Steps

- **[09-Design-Patterns](09-Design-Patterns.md)** - Detailed pattern examples
- **[10-Adding-DSL-Features](10-Adding-DSL-Features.md)** - Create new generators
- **[11-Grammar-Reference](11-Grammar-Reference.md)** - Complete DSL syntax

---

**Previous:** [07-Template-System](07-Template-System.md) | **Next:** [09-Design-Patterns](09-Design-Patterns.md)
