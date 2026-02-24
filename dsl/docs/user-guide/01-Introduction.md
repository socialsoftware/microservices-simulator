# Introduction to Nebula DSL

## What is Nebula DSL?

**Nebula** is a Domain-Specific Language (DSL) designed to generate complete Spring Boot microservices from high-level domain abstractions. It enables developers to define business domains declaratively and automatically generates production-ready Java code for the **Microservices Simulator** framework.

## Why Use Nebula?

Building microservices involves significant boilerplate code:
- JPA entities with constructors, getters, setters
- Data Transfer Objects (DTOs) for API contracts
- Repositories with custom queries
- Service layers with CRUD operations
- Event handling infrastructure
- REST controllers with endpoint mappings
- Saga coordination logic

Manually writing this code is **time-consuming**, **error-prone**, **difficult to maintain**, and **hard to evolve**.

Nebula solves this by:
- **Generating all layers** from a single domain definition
- **Ensuring consistency** through template-based generation
- **Reducing code volume by ~60%** compared to manual implementation
- **Enabling rapid iteration** - regenerate code in seconds

## Key Features

### 1. Declarative Domain Modeling
Define aggregates, entities, and relationships using intuitive syntax:

```nebula
Aggregate User {
    @GenerateCrud
    Root Entity User {
        String name
        String username
        final UserRole role
        Boolean active
    }
}
```

### 2. Cross-Aggregate References with Type Inference
Reference entities from other aggregates with automatic type resolution:

```nebula
Entity ExecutionCourse from Course {
    map name as courseName
    map type as courseType
}
```

Types are automatically inferred from the `Course` aggregate's root entity.

### 3. Event-Driven Architecture
Built-in support for publish/subscribe patterns:

```nebula
Events {
    publish UserDeleted {
        Integer userId
        String username
    }
    subscribe ExecutionDeletedEvent
}
```

### 4. Business Rules & Validation
Define invariants that are enforced at the aggregate level:

```nebula
invariants {
    check nameNotBlank { name.length() > 0 } error "User name cannot be blank"
    check roleNotNull { role != null } error "User role is required"
}
```

### 5. Custom Repository Queries
Write JPQL queries directly in the DSL:

```nebula
Repository {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds()
}
```

### 6. Complete Code Generation
Generates all layers of a microservices application:
- **Domain Layer**: JPA entities, factories, repositories
- **Service Layer**: CRUD operations, business methods, event handlers
- **Coordination Layer**: Functionalities, REST controllers, event processing
- **Saga Layer**: Distributed transaction workflows

## How Nebula Fits Into Your Workflow

### 1. Define Domain
Write `.nebula` files describing your business domain:

```
dsl/abstractions/myproject/
├── shared-enums.nebula
├── member.nebula
├── book.nebula
└── loan.nebula
```

### 2. Build the CLI
Install dependencies and compile the generator:

```bash
cd dsl/nebula
npm install
npm run langium:generate && npm run build
```

### 3. Generate Code
Run the Nebula CLI to generate a Spring Boot application:

```bash
./bin/cli.js generate ../abstractions/myproject/
```

### 4. Run Application
Build and run the generated microservices:

```bash
cd applications/myproject
mvn clean - Psagas spring-boot:run
```

### 5. Iterate
Modify DSL, regenerate, test. The cycle takes seconds, not hours.

## Technology Stack

### DSL Tooling
- **Langium** - Language framework for building DSLs
- **TypeScript** - Generator implementation
- **Node.js 18+** - Runtime environment
- **Handlebars** - Template engine for code generation
- **npm** - Package management

### Generated Code Stack
- **Java 21+** - Programming language
- **Spring Boot 3.5.3** - Framework
- **Spring Data JPA** - Persistence
- **PostgreSQL 14** - Database
- **Maven 3.9.9** - Build system
- **Groovy/Spock** - Testing framework

## What This Guide Covers

This guide is for anyone writing `.nebula` abstractions and generating Java code. It uses **7 progressive examples** (from a single-aggregate hello world to a complex e-commerce system) that introduce DSL features incrementally. Each chapter is tied to its example project in `dsl/docs/examples/`.

1. **[Getting Started](02-Getting-Started.md)** — Install, build, first generation
2. **[Your First Aggregate](03-Your-First-Aggregate.md)** — Build a microservice in 10 lines
3. **[Types, Enums, and Properties](04-Types-Enums-Properties.md)** — Data types, enums, modifiers
4. **[Business Rules and Repositories](05-Business-Rules-Repositories.md)** — Invariants and custom queries
5. **[Cross-Aggregate References](06-Cross-Aggregate-References.md)** — Reference data across aggregates
6. **[Events and Reactive Patterns](07-Events-Reactive-Patterns.md)** — Event publishing and subscriptions
7. **[Tutorial: Building a Library System](08-Tutorial-Library-System.md)** — Combines all features end-to-end
8. **[Advanced Patterns](09-Advanced-Patterns.md)** — Extract patterns, DTO entities, custom endpoints
9. **[Generated Code](10-Generated-Code.md)** — What gets generated and how to use it
10. **[Reference](11-Reference.md)** — Complete grammar and CLI reference

> For information on extending or modifying the DSL tooling itself (grammar, generators, templates), see the [Developer Guide](../developer-guide/01-Architecture.md).

---

**Next:** [02-Getting-Started](02-Getting-Started.md)
