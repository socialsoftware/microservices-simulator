# Business Rules and Repositories

This chapter covers invariants (business rules enforced at the aggregate level) and repository queries for custom data access.

> **Tied example:** [`03-businessrules`](../../abstractions/03-businessrules/): a Product aggregate with invariants and custom queries.

## The Complete Example

```nebula
Aggregate Product {

    Root Entity Product {
        String name
        String sku
        Double price
        Integer stockQuantity
        Boolean active = true

        invariants {
            name.length() > 0 : "Product name cannot be blank"
            price > 0.0 : "Product price must be positive"
            stockQuantity >= 0 : "Stock quantity cannot be negative"
        }
    }

    Repository {
        List<Product> findByActiveTrue()
        Optional<Product> findBySku(String sku)

        @Query("select p.aggregateId from Product p where p.price <= :maxPrice AND p.state != 'DELETED'")
        Set<Integer> findAffordableProductIds(Double maxPrice)
    }
}
```

## Invariants

Invariants are business rules declared inside an `invariants` block on an entity. They are verified automatically whenever the aggregate is modified. **Error messages are mandatory**:they describe what went wrong when the invariant is violated.

### Basic Syntax

```nebula
invariants {
    <condition> : "<error message>"
}
```

Each invariant has:
- A **condition** (boolean expression that must be true)
- An **error message** (thrown when the condition is false)

### Simple Invariants

```nebula
invariants {
    name.length() > 0 : "User name cannot be blank"
    role != null : "User role is required"
}
```

### Temporal Constraints

Use `isBefore()` and `isAfter()` for date comparisons:

```nebula
invariants {
    availableDate.isBefore(conclusionDate) && conclusionDate.isBefore(resultsDate) : "Dates must be in chronological order"
}
```

### Collection Constraints

```nebula
invariants {
    topics != null : "Question must have a topics collection"
}
```

### Expression Language

The invariant expression language supports:

| Category | Operators / Methods | Examples |
|----------|-------------------|----------|
| **Comparisons** | `<`, `>`, `<=`, `>=`, `==`, `!=` | `price > 0.0` |
| **Boolean logic** | `&&`, `\|\|`, `!` | `a != null && b != null` |
| **String** | `.length()` | `name.length() > 0` |
| **Temporal** | `.isBefore()`, `.isAfter()` | `startDate.isBefore(endDate)` |
| **Collection** | `.size()`, `.isEmpty()`, `.unique()` | `users.size() > 0` |
| **Null checks** | `==`, `!=` with `null` | `role != null` |

### Quantifier Preview

For advanced collection constraints, Nebula supports quantifiers:

```nebula
forall u : users | u.active == true
exists t : topics | t.topicId != null
```

These are covered in the [Reference](13-Reference.md) chapter.

### Generated Code

Invariants generate a `verifyInvariants()` method on the entity. Each invariant is checked individually, and the custom error message is included in the exception:

```java
private boolean invariantNameNotBlank() {
    return this.name != null && this.name.length() > 0;
}

private boolean invariantPricePositive() {
    return price > 0.0;
}

private boolean invariantStockNonNegative() {
    return stockQuantity >= 0;
}

@Override
public void verifyInvariants() {
    if (!invariantNameNotBlank()) {
        throw new SimulatorException(INVARIANT_BREAK, "Product name cannot be blank");
    }
    if (!invariantPricePositive()) {
        throw new SimulatorException(INVARIANT_BREAK, "Product price must be positive");
    }
    if (!invariantStockNonNegative()) {
        throw new SimulatorException(INVARIANT_BREAK, "Stock quantity cannot be negative");
    }
}
```

This method is called automatically by the simulator framework whenever changes are registered with the Unit of Work.

## Repositories

Repositories define custom data access methods beyond the auto-generated CRUD operations.

### Convention-Based Methods

Write method signatures using Spring Data-style naming. Nebula automatically generates the corresponding JPQL `@Query` annotation:

```nebula
Repository {
    List<Product> findByActiveTrue()
    Optional<Product> findBySku(String sku)
    List<Book> findByGenre(String genre)
    List<Book> findByAvailableTrue()
    Optional<User> findByUsername(String username)
}
```

Common patterns:
- `findBy<Property>`:filter by a property value
- `findBy<Property>True` / `False`:filter boolean properties
- Return `List<T>` for multiple results, `Optional<T>` for single results

Nebula parses the method name and generates the query automatically. For example, `findByActiveTrue()` generates `@Query("SELECT e FROM Product e WHERE e.active = true")`.

### Custom JPQL Queries

For complex queries that can't be expressed with method names, use `@Query` with JPQL:

```nebula
Repository {
    @Query("select p.aggregateId from Product p where p.price <= :maxPrice AND p.state != 'DELETED'")
    Set<Integer> findAffordableProductIds(Double maxPrice)

    @Query("select a from Answer a where a.quiz.quizAggregateId = :quizId")
    List<Answer> findByQuizId(Integer quizId)
}
```

Key rules:
- Use standard JPQL syntax
- Parameters are bound using `:paramName`, matching method parameter names
- Return types can be entities, collections, `Set<Integer>`, or `Optional<T>`

### Generated Code

All repository methods (both convention-based and custom) are placed on the `ProductRepository` interface with explicit `@Query` annotations:

```java
public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Query("SELECT e FROM Product e WHERE e.active = true")
    List<Product> findByActiveTrue();

    @Query("SELECT e FROM Product e WHERE e.sku = :sku")
    Optional<Product> findBySku(String sku);

    @Query("select p.aggregateId from Product p where p.price <= :maxPrice AND p.state != 'DELETED'")
    Set<Integer> findAffordableProductIds(Double maxPrice);
}
```

A separate `ProductCustomRepository` interface is also generated with the same method signatures (without annotations), intended for custom implementations.

## Walkthrough: Product Aggregate

The `03-businessrules` example combines invariants and repositories:

1. **Three invariants** guard business rules:
   - Product name can't be blank
   - Price must be positive
   - Stock can't go negative

2. **Three repository methods** provide custom access:
   - `findByActiveTrue()`:all active products (auto-generated JPQL from method name)
   - `findBySku(String sku)`:lookup by SKU (auto-generated JPQL from method name)
   - `findAffordableProductIds(Double maxPrice)`:explicit JPQL query

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/03-businessrules/
```

Check `Product.java` for the invariant methods and `ProductRepository.java` for the query methods.

---

**Previous:** [04-Types-Enums-Properties](04-Types-Enums-Properties.md) | **Next:** [06-Cross-Aggregate-References](06-Cross-Aggregate-References.md)
