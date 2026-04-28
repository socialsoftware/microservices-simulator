# Reference

Complete grammar syntax reference and CLI reference for the Nebula DSL.

## CLI Reference

### Generate Command

```bash
./bin/cli.js generate <abstractions-path> [options]
```

| Argument/Option | Description |
|-----------------|-------------|
| `<abstractions-path>` | Path to `.nebula` files (file or directory) |
| `-o, --output <dir>` | Output directory (default: `../../applications`) |
| `-d, --debug` | Enable debug mode with detailed error output |
| `-v, --verbose` | Enable verbose logging |
| `--no-validate` | Skip validation during generation |

The project name is derived from the abstractions folder name (e.g., `../abstractions/showcase/` produces project name `showcase`).

### Examples

```bash
./bin/cli.js generate ../abstractions/01-helloworld/
./bin/cli.js generate ../abstractions/showcase/
./bin/cli.js generate ../abstractions/answers/ -o ../../applications/answers
```

## Grammar Reference

### Top-Level Structure

A `.nebula` file contains one or more top-level declarations:

```nebula
Aggregate Name { ... }
SharedEnums { ... }
SagaStates { ... }
Workflow Name { ... }
exceptions { ... }
```

Multiple declarations can appear in a single file or across separate files in the same directory.

### Aggregate

```nebula
Aggregate Name {
    Root Entity Name { ... }
    Entity Name { ... }
    Methods { ... }
    Events { ... }
    Repository { ... }
}
```

Every aggregate must contain exactly one `Root Entity`. Other blocks are optional.

### Entity

```nebula
Root Entity Name {
    Type fieldName
    Type fieldName = defaultValue
    final Type fieldName
    Type fieldName dto-exclude
    Type a, b, c

    invariants {
        condition : "error message"
    }
}
```

#### Non-Root Entity (local)

```nebula
Entity RoomAmenity {
    Integer code
    String name
    String description
}
```

#### DTO Entity (value object)

```nebula
Dto Entity OrderItem {
    String key
    Integer quantity
    Double unitPrice
}
```

#### Projection Entity (cross-aggregate reference)

```nebula
Entity BookingUser {
    from User { username as userName, email as userEmail }
}
```

With bare field names (no alias):

```nebula
Entity BookingRoom {
    from Room { roomNumber, pricePerNight }
}
```

With empty mapping (base fields only):

```nebula
Entity EnrollmentTeacher {
    from Teacher {  }
}
```

#### Extract Pattern (collection field extraction)

```nebula
Entity InvoiceOrder {
    from Order { items.key as orderItemKeys }
}
```

### Types

| Category | Types |
|----------|-------|
| Primitive | `String`, `Integer`, `Long`, `Float`, `Double`, `Boolean`, `LocalDateTime` |
| Built-in | `AggregateState`, `UnitOfWork` |
| Collection | `List<T>`, `Set<T>`, `Optional<T>` |
| Entity | Any entity name defined in the same aggregate |
| Enum | Any enum defined in `SharedEnums` |

### Default Values

```nebula
Boolean active = true
Integer count = 0
Double price = 0.0
RoomStatus status = RoomStatus.AVAILABLE
MembershipTier tier = MembershipTier.BRONZE
```

### Invariants

```nebula
invariants {
    name.length() > 0 : "Name cannot be blank"
    price > 0.0 : "Price must be positive"
    startDate.isBefore(endDate) : "Start must precede end"
    member != null : "Member is required"
}
```

Operators: `<`, `>`, `<=`, `>=`, `==`, `!=`, `&&`, `||`, `!`
Methods: `.length()`, `.isBefore()`, `.isAfter()`, `.isEqual()`, `.size()`, `.isEmpty()`, `.unique(field)`
Quantifiers: `forall var : collection | body`, `exists var : collection | body`
Collection ops: `collection.allMatch(x -> expr)`, `.anyMatch(...)`, `.noneMatch(...)`

### Methods

```nebula
Methods {
    methodName(Type param1, Type param2) {
        action { ... }
    }
}
```

#### Action Body

```nebula
action {
    create AggregateName { field: value, field: value }
    load AggregateName(idExpr) as alias
    alias.field = value
    find alias.collection where field == value as elementAlias
    elementAlias.field = value
}
```

#### Precondition

```nebula
precondition {
    check expr op expr error "message"
}
```

Operators: `==`, `!=`, `<`, `>`, `<=`, `>=`

#### Publishes

```nebula
publishes EventName {
    field: expression,
    field: expression
}
```

#### Query Body

```nebula
getActiveUsers() {
    query findActiveUserIds()
}
```

#### HTTP Endpoint

```nebula
@PostMapping("/path/{param}")
methodName(Type param) { ... }
```

Methods without `@PostMapping` are internal (not exposed as REST endpoints).

### Events

```nebula
Events {
    publish EventName {
        Type field
    }

    subscribe EventType

    subscribe EventType from SourceAggregate {
        when condition
        action {
            this.state = INACTIVE
        }
    }
}
```

- `publish`: declares an event class with typed fields
- `subscribe` (bare): auto-refresh projections when source changes
- `subscribe` with `when` + `action`: reactive logic on event arrival

### Repository

```nebula
Repository {
    List<Entity> findByFieldTrue()
    Optional<Entity> findByField(Type param)

    @Query("select e.aggregateId from Entity e where e.field = :param")
    Set<Integer> customMethod(Type param)
}
```

Convention-based methods auto-generate JPQL. Custom JPQL uses `@Query`.

### SharedEnums

```nebula
SharedEnums {
    StatusName { VALUE1, VALUE2, VALUE3 }
    CategoryName { A, B, C }
}
```

### SagaStates

```nebula
SagaStates {
    GroupName { STATE1, STATE2 }
}
```

Each group generates a Java enum implementing `SagaState`.

### Workflow

```nebula
Workflow Name {
    input {
        Type param1,
        Type param2
    }

    step stepName {
        action: Aggregate.method(args)
        compensate: Aggregate.method(args)
        lock Aggregate(idExpr) as STATE
        lock Aggregate(idExpr) as STATE forbidden [STATE1, STATE2]
    }
}
```

- `input`: workflow parameters (becomes request DTO + execute method signature)
- `action`: the operation to perform (calls a service method on the target aggregate)
- `compensate` (optional): rollback operation if a later step fails
- `lock` (optional): saga-state locking — verify + register before action
- `forbidden` (optional): override default forbidden states (default: all states in the enum)
- Step results can be referenced by name in later steps: `stepName.field`

### Exception Messages

```nebula
exceptions {
    ERROR_NAME: "Error message with %d and %s format specifiers"
}
```

## File Organization

Recommended file structure for a project:

```
project/
├── nebula.config.json          # Required: base package + framework version
├── shared-enums.nebula         # Shared enumerations
├── saga-states.nebula          # Saga state declarations (if using workflows)
├── exceptions.nebula           # Custom exception messages
├── aggregate1.nebula           # One file per aggregate
├── aggregate2.nebula
└── workflow.nebula             # Cross-aggregate workflows
```

---

**Previous:** [12-Generated-Code](12-Generated-Code.md) | **Back to:** [01-Introduction](01-Introduction.md)
