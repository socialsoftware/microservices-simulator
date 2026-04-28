# Advanced Patterns

This chapter covers advanced DSL features that go beyond the basics: extract patterns, DTO entities, exception messages, and real-world case study summaries.

> **Tied example:** [`07-advanced`](../../abstractions/07-advanced/): Customer, Product, Order, and Invoice aggregates with advanced patterns.

## Extract Pattern

The extract pattern lets you extract specific fields from collection properties in cross-aggregate references. Instead of referencing an entire collection of entities, you extract just the fields you need.

### Syntax

```nebula
Entity InvoiceOrder {
    from Order { items.key as orderItemKeys }
    AggregateState orderState
}
```

The dotted path `items.key` means:
1. `items`: a `Set<OrderItem>` collection in the Order root entity
2. `.key`: the `key` field on each `OrderItem`
3. Result: `Set<String>` — the collection type is preserved, the element type becomes the field's type

### How It Works

Given this source aggregate:

```nebula
Dto Entity OrderItem {
    String key
    String productName
    Integer quantity
    Double unitPrice
}

Root Entity Order {
    Set<OrderItem> items
}
```

The extract `items.key as orderItemKeys`:
1. Finds `items` in Order's root entity -> `Set<OrderItem>`
2. Finds `key` in `OrderItem` -> `String`
3. Wraps the field type in the collection type -> `Set<String>`

### Generated Code

```java
private Set<String> orderItemKeys = new HashSet<>();

public Set<String> getOrderItemKeys() { ... }
public void setOrderItemKeys(Set<String> orderItemKeys) { ... }
```

### Common Use Cases

```nebula
from Quiz { questions.aggregateId as quizQuestionsAggregateIds }    // Set<Integer>
from Order { items.key as orderItemKeys }                            // Set<String>
```

### Limitations

- **Single-level only**: `items.key` works, `items.nested.key` does not
- **Root entity properties only**: the source collection must be on the root entity
- **No filtering**: extracts from all elements in the collection
- **List and Set only**: works with `List<T>` and `Set<T>` collection types

## Dto Entity

Mark an entity with `Dto` to create a value object within the aggregate that is not a reference to another aggregate:

```nebula
Dto Entity OrderItem {
    String key
    String productName
    Integer quantity
    Double unitPrice
}
```

DTO entities are generated as JPA `@Entity` classes with their own `@Id` and database table, but they:
- Do not have `aggregateId` or `version` fields
- Are linked to the root entity via a `@OneToOne` or `@ManyToOne` relationship
- Are useful for line items, nested value objects, and structured data within an aggregate

### Usage in Root Entity

```nebula
Root Entity Order {
    Set<OrderItem> items
}
```

## Exception Messages

Define project-specific error messages with format specifiers in a dedicated `exceptions.nebula` file:

```nebula
exceptions {
    ORDER_NOT_FOUND: "Order with id %d does not exist"
    CUSTOMER_NOT_FOUND: "Customer with id %d does not exist"
    INSUFFICIENT_STOCK: "Insufficient stock for product %s"
    AGGREGATE_NOT_FOUND: "Aggregate with aggregate id %d does not exist."
    INVARIANT_BREAK: "Aggregate %d breaks invariants"
}
```

These generate static constants in an exception messages class:

```java
public static final String ORDER_NOT_FOUND = "Order with id %d does not exist";
public static final String CUSTOMER_NOT_FOUND = "Customer with id %d does not exist";
```

Use `%d` for integers, `%s` for strings, following standard Java format specifiers.

## Walkthrough: E-Commerce System

The `07-advanced` example builds a complete e-commerce system with 4 aggregates and shared enums:

### Shared Enums

```nebula
SharedEnums {
    OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
    PaymentMethod { CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, PAYPAL }
}
```

### Aggregate Relationships

```
Customer <────── Order ──────> Product
                  │
                  v
               Invoice (extract pattern: items.key)
```

### Key Features Demonstrated

| Feature | Where Used |
|---------|-----------|
| Extract pattern | `invoice.nebula`: `from Order { items.key as orderItemKeys }` |
| Dto Entity | `order.nebula`: `Dto Entity OrderItem` |
| AggregateState reference | `invoice.nebula`: `AggregateState orderState` |
| Custom JPQL query | `order.nebula`: `findOrderIdsAboveAmount` |
| Multiple shared enums | `shared-enums.nebula`: `OrderStatus`, `PaymentMethod` |
| Event-driven cascade | `order.nebula` and `invoice.nebula`: `subscribe ... when ... action { this.state = INACTIVE }` |

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/07-advanced/
```

## Real-World Case Studies

### Answers Project (8 aggregates)

Located in `dsl/abstractions/answers/`, this is a complete course management system:

| Aggregate | Features |
|-----------|----------|
| User | CRUD, custom methods (`signUp`, `awardLoyaltyPoints`), events |
| Course | CRUD, events |
| Execution | Cross-refs to Course and User, collection references (`Set<ExecutionUser>`), custom JPQL |
| Topic | Cross-ref to Course, events |
| Question | Cross-ref to Topic, `find` action statement, invariants |
| Quiz | Cross-ref to Question, collection references |
| Tournament | Cross-ref to Execution and Quiz, complex temporal invariants, custom methods |
| Answer | Cross-refs, extract pattern (`questions.aggregateId as quizQuestionsAggregateIds`), complex queries |

### TeaStore Project (5 aggregates)

Located in `dsl/abstractions/teastore/`, this is an e-commerce storefront:

| Aggregate | Features |
|-----------|----------|
| User | CRUD |
| Category | CRUD, events |
| Product | Cross-ref to Category, events, invariants |
| Cart | Cross-ref to User and Product, collection references |
| Order | Cross-ref to User and Product, collection references |

### Showcase Project (3 aggregates + workflow)

Located in `dsl/abstractions/showcase/`, this is a hotel booking system demonstrating all advanced features:

| Aggregate/File | Features |
|-----------|----------|
| User | Custom methods (`signUp`, `awardLoyaltyPoints`), `@PostMapping` endpoints, preconditions, `publishes`, `query` method body |
| Room | State transitions (`reserve`, `checkIn`, `checkOut`, `release`, `retire`), `find ... where ... as` action, collection entities |
| Booking | Cross-aggregate projections with aliases, event cascade, invariants, custom `confirmBooking` method |
| Workflow | Cross-aggregate saga orchestration with compensation and saga-state locking |
| SagaStates | Shared saga state declarations for concurrent access control |

See [Chapter 10: Methods and Custom Endpoints](10-Methods-Custom-Endpoints.md) and [Chapter 11: Workflows and Sagas](11-Workflows-Sagas.md) for detailed coverage of the showcase features.

## Best Practices

### One Aggregate Per File

Keep each aggregate in its own `.nebula` file, named after the aggregate (lowercase). This keeps files focused and manageable.

### Prefer Cross-References Over Duplication

Instead of duplicating fields, reference other aggregates:

```nebula
// Good: reference with type inference
Entity LoanMember {
    from Member { name as memberName }
}

// Bad: manual duplication
Root Entity Loan {
    Integer memberId
    String memberName  // Duplicated, can go stale
}
```

### Always Define Invariants

Business rules in invariants are enforced automatically. Always include descriptive error messages:

```nebula
invariants {
    price > 0.0 : "Product price must be positive"
}
```

### Event-Driven Referential Integrity

For cross-aggregate references, always subscribe to the source's delete event with an explicit cascade action:

```nebula
subscribe AuthorDeletedEvent from Author {
    when author.authorAggregateId == event.aggregateId
    action {
        this.state = INACTIVE
    }
}
```

This matches the simulator's native architecture: deletes always succeed, affected subscribers react asynchronously.

---

**Previous:** [08-Tutorial-Library-System](08-Tutorial-Library-System.md) | **Next:** [10-Methods-Custom-Endpoints](10-Methods-Custom-Endpoints.md)
