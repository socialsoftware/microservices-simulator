# Introduction to Nebula DSL

## What is Nebula DSL?

**Nebula** is a Domain-Specific Language (DSL) designed to generate complete Spring Boot microservices from high-level domain abstractions. It enables developers to define business domains declaratively and automatically generates production-ready Java code for the **Microservices Simulator** framework.

## Why Was Nebula Created?

Building microservices involves significant boilerplate code:
- JPA entities with constructors, getters, setters
- Data Transfer Objects (DTOs) for API contracts
- Repositories with custom queries
- Service layers with CRUD operations
- Event handling infrastructure
- REST controllers with endpoint mappings
- Saga/TCC coordination logic

Manually writing this code is:
- **Time-consuming** - Hundreds of lines per aggregate
- **Error-prone** - Copy-paste mistakes, inconsistent patterns
- **Difficult to maintain** - Changes cascade across layers
- **Hard to evolve** - Adding features requires touching multiple files

Nebula solves this by:
- **Generating all layers** from a single domain definition
- **Ensuring consistency** through template-based generation
- **Reducing code volume by 60%** compared to manual implementation
- **Enabling rapid iteration** - Regenerate code in seconds

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
Aggregate Execution {
    Entity ExecutionCourse from Course {
        map name as courseName
        map type as courseType
    }
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
- **Saga/TCC Layer**: Distributed transaction workflows

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Nebula DSL (.nebula files)              │
│                                                             │
│  Aggregate User {                                           │
│    Root Entity User { String name; UserRole role; }         │
│    Events { publish UserDeleted { ... } }                   │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Langium Parser                           │
│              (Grammar-based AST Construction)               │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Code Generation Pipeline                   │
│                                                             │
│  Discovery → Parsing → Validation → Generation → Output     │
│                                                             │
│  19 Specialized Generators:                                 │
│  • Entity Generator      • DTO Generator                    │
│  • Service Generator     • Repository Generator             │
│  • Factory Generator     • Event Generators                 │
│  • Controller Generator  • Saga Generators                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Generated Spring Boot Application              │
│                                                             │
│  applications/answers/src/main/java/                        │
│  ├── microservices/user/                                    │
│  │   ├── aggregate/User.java                                │
│  │   ├── service/UserService.java                           │
│  │   └── events/UserDeletedEvent.java                       │
│  ├── coordination/                                          │
│  │   ├── functionalities/UserFunctionalities.java           │
│  │   └── webapi/UserController.java                         │
│  └── sagas/coordination/user/                               │
│      └── CreateUserFunctionalitySagas.java                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Microservices Simulator Framework              │
│                                                             │
│  • Unit of Work Pattern (transaction boundaries)            │
│  • Saga Pattern (distributed transactions)                  │
│  • TCC Pattern (transactional causal consistency)           │
│  • Event System (pub/sub infrastructure)                    │
└─────────────────────────────────────────────────────────────┘
```

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

## Project Structure Overview

```
microservices-simulator/
├── dsl/
│   ├── abstractions/          # DSL source files (.nebula)
│   │   ├── answers/           # Answers case study (9 aggregates)
│   │   └── teastore/          # TeaStore case study (6 aggregates)
│   ├── docs/                  # Documentation
│   │   └── developer-guide/   # This guide
│   └── nebula/                # DSL implementation
│       ├── src/language/      # Langium grammar
│       ├── src/cli/           # Code generators
│       └── bin/cli.js         # CLI entry point
├── applications/              # Generated applications
│   ├── answers/               # Generated from answers abstractions
│   └── teastore/              # Generated from teastore abstractions
└── simulator/                 # Framework library
    └── src/main/java/         # Core abstractions (Aggregate, UnitOfWork, etc.)
```

## How Nebula Fits Into Your Workflow

### 1. Define Domain
Write `.nebula` files describing your business domain:

```bash
dsl/abstractions/myproject/
├── user.nebula
├── order.nebula
└── product.nebula
```

### 2. Generate Code
Run the Nebula CLI to generate Spring Boot application:

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/myproject/
```

### 3. Run Application
Build and run the generated microservices:

```bash
cd applications/myproject
mvn clean -Psagas spring-boot:run
```

### 4. Iterate
Modify DSL, regenerate, test. The cycle takes seconds, not hours.

## Design Philosophy

### Convention Over Configuration
Nebula provides smart defaults with override capability:
- Service names auto-generated from aggregates
- CRUD operations generated with `@GenerateCrud`
- Standard package structure follows Spring conventions

### Type Safety
Strong typing throughout:
- Grammar enforces valid DSL constructs
- Type inference reduces boilerplate
- Generated code is type-safe Java

### Separation of Concerns
Clear boundaries between:
- **Domain logic** (aggregates, entities, invariants)
- **Coordination logic** (functionalities, workflows)
- **Infrastructure** (repositories, event handling)

### Event-Driven by Default
Built-in pub/sub support:
- Aggregates publish domain events
- Aggregates subscribe to external events
- Event handlers automatically generated

## Who Should Use This Guide?

This guide is for anyone looking to understand, extend, or maintain Nebula, whether you're adding new generators, modifying templates, or learning how the DSL and code generation pipeline work.

## Prerequisites

To work with Nebula, you should be familiar with:
- **TypeScript** (generators are written in TypeScript)
- **Java & Spring Boot** (generated code targets Spring Boot)
- **Domain-Driven Design** (aggregates, entities, repositories)
- **Basic compiler concepts** (AST, parsers, code generation)

---

**Next:** [01-Quick-Start →](01-Quick-Start.md)
