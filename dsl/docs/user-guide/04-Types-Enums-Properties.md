# Types, Enums, and Properties

This chapter covers the Nebula type system: shared enumerations, all supported data types, and property modifiers that control how fields behave.

> **Tied example:** [`02-typesenums`](../examples/abstractions/02-typesenums/) — a Contact aggregate with enums, temporal types, and default values.

## Shared Enums

Enumerations are defined in a `SharedEnums` block, typically in a dedicated `shared-enums.nebula` file. They are shared across all aggregates in the project:

```nebula
SharedEnums {
    enum ContactCategory { PERSONAL, WORK, FAMILY }
}
```

Multiple enums can be defined in one block:

```nebula
SharedEnums {
    enum MembershipType { BASIC, PREMIUM }
    enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
    enum PaymentMethod { CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, PAYPAL }
}
```

Each enum generates a standard Java enum in the `shared/enums/` package:

```java
public enum ContactCategory {
    PERSONAL,
    WORK,
    FAMILY
}
```

## Complete Type Reference

### Primitive Types

| Type | Java Equivalent | Example |
|------|----------------|---------|
| `String` | `String` | `String name` |
| `Integer` | `Integer` | `Integer count` |
| `Long` | `Long` | `Long timestamp` |
| `Float` | `Float` | `Float price` |
| `Double` | `Double` | `Double percentage` |
| `Boolean` | `Boolean` | `Boolean active` |

### Temporal Types

```nebula
LocalDateTime createdAt
LocalDateTime availableDate
```

Maps to `java.time.LocalDateTime`. Useful for timestamps, scheduling, and date-based invariants (see [Chapter 05](05-Business-Rules-Repositories.md)).

### Collection Types

```nebula
List<Integer> scores
Set<ExecutionUser> users
```

- `List<T>` — ordered, allows duplicates
- `Set<T>` — unordered, no duplicates

Collections of entities generate additional add/remove service methods (see [Chapter 10](10-Generated-Code.md)).

**Constraint:** Nested collections (`List<List<T>>`) are not supported.

### Optional Types

```nebula
Optional<LocalDateTime> deadline
```

Wraps a value that may or may not be present.

### Enum Types

Any enum defined in `SharedEnums` can be used as a property type:

```nebula
ContactCategory category
MembershipType membership
```

### Entity Reference Types

Reference another entity within the same aggregate (composition):

```nebula
LoanBook book               // Single entity reference
Set<EnrollmentTeacher> teachers   // Collection of entity references
```

Entity references across aggregates use the `from` keyword — covered in [Chapter 06](06-Cross-Aggregate-References.md).

### Built-in Types

```nebula
AggregateState state      // ACTIVE, INACTIVE, DELETED
UnitOfWork unitOfWork     // Transaction context (used in service method parameters)
```

## Property Modifiers

### Immutable (`final`)

Properties marked `final` cannot be changed after entity creation. The update DTO excludes them, and no setter is generated in the update flow:

```nebula
final String genre
final LocalDateTime createdAt
```

### Default Values

Assign a default value that applies when the property is not explicitly set:

```nebula
Boolean available = true
Integer count = 0
Boolean favorite = false
AggregateState state = AggregateState.ACTIVE
```

In the generated entity, this becomes:

```java
private Boolean available = true;
private Integer count = 0;
```

### DTO Exclusion (`dto-exclude`)

Exclude a property from DTOs — useful for internal fields that shouldn't be exposed via the API:

```nebula
String internalId dto-exclude
```

The field exists in the JPA entity but not in `TaskDto` or `CreateTaskRequestDto`.

### Multi-Declaration

Declare multiple properties of the same type on a single line:

```nebula
String firstName, lastName, email
```

This is equivalent to:

```nebula
String firstName
String lastName
String email
```

## Walkthrough: Contact Aggregate

The `02-typesenums` example puts these features together:

```nebula
SharedEnums {
    enum ContactCategory { PERSONAL, WORK, FAMILY }
}

Aggregate Contact {
    @GenerateCrud

    Root Entity Contact {
        String firstName
        String lastName
        String email
        ContactCategory category
        LocalDateTime createdAt
        Boolean favorite = false
        Integer callCount = 0
    }
}
```

This demonstrates:
- **Enum type** — `ContactCategory category` uses a shared enum
- **Temporal type** — `LocalDateTime createdAt` for a timestamp
- **Default values** — `favorite = false` and `callCount = 0`
- **Multiple string properties** — could also be written as `String firstName, lastName, email`

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/02-typesenums/ -o ../docs/examples/generated
```

The generated `Contact.java` entity includes all types with correct Java mappings, and the `ContactCategory.java` enum is placed in `shared/enums/`.

---

**Previous:** [03-Your-First-Aggregate](03-Your-First-Aggregate.md) | **Next:** [05-Business-Rules-Repositories](05-Business-Rules-Repositories.md)
