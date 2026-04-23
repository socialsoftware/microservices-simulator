# Advanced Patterns

This chapter covers advanced DSL features that go beyond the basics: extract patterns, DTO entities, custom services and endpoints, exception messages, and real-world case study summaries.

> **Tied example:** [`07-advanced`](../examples/abstractions/07-advanced/): Customer, Product, Order, and Invoice aggregates with advanced patterns.

## Extract Pattern

The extract pattern lets you extract specific fields from collection properties in cross-aggregate references. Instead of referencing an entire collection of entities, you extract just the fields you need.

### Syntax

```nebula
Entity InvoiceOrder from Order {
    map items.key as orderItemKeys
    AggregateState orderState
}
```

The dotted path `items.key` means:
1. `items`:a `Set<OrderItem>` collection in the Order root entity
2. `.key`:the `key` field on each `OrderItem`
3. Result: `Set<String>`:the collection type is preserved, the element type becomes the field's type

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
    // ...
}
```

The extract `map items.key as orderItemKeys`:
1. Finds `items` in Order's root entity → `Set<OrderItem>`
2. Finds `key` in `OrderItem` → `String`
3. Wraps the field type in the collection type → `Set<String>`

### Generated Code

```java
// InvoiceOrder entity
private Set<String> orderItemKeys = new HashSet<>();

// InvoiceOrder DTO
private Set<String> orderItemKeys;
public Set<String> getOrderItemKeys() { ... }
public void setOrderItemKeys(Set<String> orderItemKeys) { ... }
```

### Common Use Cases

```nebula
// Extract aggregate IDs from a collection
map participants.aggregateId as participantIds    // Set<Integer>

// Extract business fields
map products.sku as productSkus                   // Set<String>

// Extract status fields
map orders.status as orderStatuses                // List<OrderStatus>
```

### Limitations

- **Single-level only**:`map a.b` works, `map a.b.c` does not
- **Root entity properties only**:the source collection must be on the root entity
- **No filtering**:extracts from all elements in the collection
- **List and Set only**:works with `List<T>` and `Set<T>` collection types

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
    // ...
}
```

## Custom Service Methods

When `@GenerateCrud` isn't enough, define additional service methods:

```nebula
Service UserService {
    @Transactional

    methods {
        activateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        deactivateUser(Integer userId, UnitOfWork unitOfWork): UserDto
    }
}
```

If the service name is omitted, it defaults to `<Aggregate>Service`:

```nebula
Service {
    @Transactional
}
```

Custom methods generate method stubs in the service class that you can implement with your specific business logic.

## Custom Web API Endpoints

Define REST endpoints when `@GenerateCrud` doesn't cover all your API needs:

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

This generates a controller method with the specified HTTP method, path, parameters, and return type.

## Exception Messages

Define project-specific error messages with format specifiers:

```nebula
exceptions {
    ORDER_NOT_FOUND: "Order with id %d does not exist"
    CUSTOMER_NOT_FOUND: "Customer with id %d does not exist"
    INVOICE_NOT_FOUND: "Invoice with id %d does not exist"
    INSUFFICIENT_STOCK: "Insufficient stock for product %s"
    INVALID_ORDER_STATUS: "Cannot transition order to status %s"
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
    enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
    enum PaymentMethod { CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, PAYPAL }
}
```

### Aggregate Relationships

```
Customer ◄────── Order ──────► Product
                  │
                  ▼
               Invoice (extract pattern: items.key)
```

### Key Features Demonstrated

| Feature | Where Used |
|---------|-----------|
| Extract pattern | `invoice.nebula`:`map items.key as orderItemKeys` |
| Dto Entity | `order.nebula`:`Dto Entity OrderItem` |
| AggregateState reference | `invoice.nebula`:`AggregateState orderState` |
| Custom JPQL query | `order.nebula`:`findOrderIdsAboveAmount` |
| Multiple shared enums | `shared-enums.nebula`:`OrderStatus`, `PaymentMethod` |
| Custom exceptions | `exceptions.nebula`:5 error message constants |
| Cascading references | `invoice.nebula`:cascade on Order delete |
| Prevent references | `order.nebula`:prevent Customer delete |

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/07-advanced/ -o ../docs/examples/generated
```

## Real-World Case Studies

### Answers Project (8 aggregates)

Located in `dsl/abstractions/answers/`, this is a complete course management system (plus `shared-enums.nebula` and `exceptions.nebula`):

| Aggregate | Features |
|-----------|----------|
| User | Basic CRUD, events, role-based |
| Course | Basic CRUD, events |
| Execution | Cross-refs to Course and User, collection references (`Set<ExecutionUser>`), multiple inter-invariants, custom JPQL |
| Topic | Basic CRUD, events |
| Question | Cross-ref to Topic, invariants |
| Quiz | Cross-ref to Question, collection references |
| Tournament | Cross-ref to Execution and Quiz, complex temporal invariants |
| Answer | Cross-refs, extract pattern (`map questions.aggregateId as quizQuestionsAggregateIds`), complex queries |

### TeaStore Project (5 aggregates)

Located in `dsl/abstractions/teastore/`, this is an e-commerce storefront (plus `shared-enums.nebula`):

| Aggregate | Features |
|-----------|----------|
| User | Basic CRUD |
| Category | Basic CRUD, events |
| Product | Cross-ref to Category, events, invariants |
| Cart | Cross-ref to User and Product, collection references |
| Order | Cross-ref to User and Product, collection references |

## Best Practices

### One Aggregate Per File

Keep each aggregate in its own `.nebula` file, named after the aggregate (lowercase). This keeps files focused and manageable.

### Use `@GenerateCrud` Liberally

Most aggregates need CRUD operations. Start with `@GenerateCrud` and add custom methods only when needed.

### Prefer Cross-References Over Duplication

Instead of duplicating fields, reference other aggregates:

```nebula
// ✅ Good: reference with type inference
Entity LoanMember from Member {
    map name as memberName
}

// ❌ Bad: manual duplication
Root Entity Loan {
    Integer memberId
    String memberName  // Duplicated, can go stale
}
```

### Always Define Invariants

Business rules in invariants are enforced automatically. Always include descriptive error messages:

```nebula
check pricePositive { price > 0.0 } error "Product price must be positive"
```

### Use References for Integrity

Always define `References` blocks when you have cross-aggregate references. They make delete handling declarative and consistent.

### Combine References with Inter-Invariants

For complete referential integrity, pair every reference with an inter-invariant:

```nebula
References {
    author -> Author {
        onDelete: cascade
        message: "Author deleted, removing posts"
    }
}

Events {
    interInvariant AUTHOR_EXISTS {
        subscribe AuthorDeletedEvent from Author {
            author.authorAggregateId == event.aggregateId
        }
    }
}
```

---

**Previous:** [08-Tutorial-Library-System](08-Tutorial-Library-System.md) | **Next:** [10-Generated-Code](10-Generated-Code.md)
