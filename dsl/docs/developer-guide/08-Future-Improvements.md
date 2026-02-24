# Future Improvements

This chapter documents planned enhancements, known limitations, and potential directions for the Nebula DSL. Items are organized by category and roughly prioritized within each section.

## Language Enhancements

### Nested Extraction Paths

Currently the extract pattern supports single-level paths (`map items.field as target`). Nested paths would enable deeper extraction:

```nebula
// Current (single-level only)
map items.key as orderItemKeys

// Future (nested paths)
map items.product.sku as orderProductSkus
```

### Filtering in Extract

Add filtering support to extract only matching elements:

```nebula
// Extract IDs only for active items
map items where status == 'ACTIVE'.aggregateId as activeItemIds
```

### Aggregation Functions

Enable computed aggregations over collections:

```nebula
map items.price.sum() as totalPrice
map items.quantity.count() as itemCount
map items.price.avg() as averagePrice
```

### Computed Properties

Properties derived from other properties, evaluated at runtime:

```nebula
Root Entity User {
    String firstName
    String lastName
    computed fullName = firstName + " " + lastName
}
```

### State Machine Transitions

Declarative state transitions with guards and actions:

```nebula
states OrderLifecycle {
    PENDING -> CONFIRMED { guard: items.size() > 0 }
    CONFIRMED -> SHIPPED
    SHIPPED -> DELIVERED
    CONFIRMED -> CANCELLED
    PENDING -> CANCELLED
}
```

### Value Objects

Reusable non-entity types (not aggregates, not DTOs):

```nebula
ValueObject Address {
    String street
    String city
    String postalCode
    String country
}

Root Entity Customer {
    String name
    Address billingAddress
    Address shippingAddress
}
```

## Code Generation Enhancements

### Multi-Step Saga Workflows

Support for saga workflows with explicit step dependencies and ordering:

```nebula
Saga CreateOrderSaga {
    step validateCustomer -> CustomerService.validate
    step reserveStock -> ProductService.reserve
    step processPayment -> PaymentService.charge

    compensate reserveStock -> ProductService.releaseReservation
    compensate processPayment -> PaymentService.refund
}
```

### Test Generation

Auto-generate test suites from DSL definitions:
- Spock/JUnit tests for each CRUD operation
- Invariant violation tests (negative cases)
- Event publishing and handling tests
- Cross-aggregate reference tests
- Integration tests with test data builders

### OpenAPI/Swagger Spec Generation

Generate OpenAPI 3.0 specifications from `WebAPIEndpoints` and `@GenerateCrud`:
- Complete API documentation from DSL
- Client SDK generation from OpenAPI specs
- API versioning support

### Database Migration Scripts

Generate Flyway or Liquibase migration scripts:
- Initial schema from entity definitions
- Incremental migrations when entities change
- Index creation for repository query patterns

### Docker Compose Generation

Generate `docker-compose.yml` from project configuration:
- One service per aggregate (or grouped)
- PostgreSQL database per service
- Network configuration
- Environment variable templates

## Validation Enhancements

### Cross-Aggregate Reference Validation at Parse Time

Validate that `from X` references point to existing aggregates during parsing, not just at generation time. This would provide immediate feedback in the VSCode extension.

### Circular Dependency Detection

Detect and warn about circular reference chains:
```
A → B → C → A  (circular)
```

Currently this is only caught at runtime.

### Unused Entity Warnings

Warn when an entity is defined but never referenced in a root entity or collection:
```
Warning: Entity 'OrderItem' is defined but never used in any root entity property
```

### JPQL Syntax Validation

Validate `@Query` JPQL syntax at parse time:
- Check entity names match defined entities
- Validate parameter references match method parameters
- Basic JPQL grammar checking

## Tooling Enhancements

### VSCode: Go to Definition

`Ctrl+Click` on `from Teacher` navigates to the `Teacher` aggregate definition, even across files.

### VSCode: Rename Refactoring

Rename an aggregate, entity, or property and automatically update all references across all `.nebula` files.

### VSCode: Aggregate Relationship Diagrams

Generate a visual diagram of aggregate relationships in the VSCode sidebar, showing:
- Cross-aggregate references
- Event publish/subscribe connections
- Reference delete policies

### CLI: Watch Mode

Auto-regenerate code when `.nebula` files change:

```bash
./bin/cli.js generate --watch ../abstractions/answers/
```

### CLI: Diff Mode

Show what files would change since the last generation:

```bash
./bin/cli.js generate --diff ../abstractions/answers/
```

### CLI: Dry Run

Preview generation without writing any files:

```bash
./bin/cli.js generate --dry-run ../abstractions/answers/
```

## Architecture Enhancements

### Plugin System for Custom Generators

Allow users to register custom generators without modifying the core DSL:

```typescript
// nebula.config.json
{
  "plugins": [
    "./my-custom-generator.ts"
  ]
}
```

### Multi-Target Generation

Generate code for platforms beyond Spring Boot:
- Quarkus
- Micronaut
- Go microservices
- Node.js/Express

### Incremental Generation

Only regenerate files for aggregates that have changed, reducing generation time for large projects.

## Known Limitations (Current)

These are documented limitations of the current implementation:

| Limitation | Scope | Notes |
|-----------|-------|-------|
| Extract pattern: single-level only | Language | `map a.b` works, `map a.b.c` does not |
| Extract pattern: root entity only | Language | Source collection must be on root entity |
| Extract pattern: no filtering | Language | Extracts from all elements |
| No nested collections | Type system | `List<List<T>>` is invalid |
| Workflow DSL in grammar but expert-level | Language | Exists in grammar, not documented for general use |
| No runtime JPQL validation | Validation | JPQL errors only caught at Java compile time |
| No incremental generation | Performance | Full regeneration on every run |

---

**Previous:** [07-VSCode-Extension](07-VSCode-Extension.md) | **Back to:** [01-Architecture](01-Architecture.md)
