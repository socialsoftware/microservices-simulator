# Step 9 · Architecture Options

[📚 Guide Index](00-index.md)

> **Goal:** understand the different architecture modes and when to use each one.

[← Back to Step 8](08-sagas.md)

---

## 9.1 Available Architectures

Nebula supports three architecture patterns:

| Architecture | Description | Use Case |
|--------------|-------------|----------|
| `default` | Standard microservices | Simple CRUD, no distributed transactions |
| `causal-saga` | Full saga coordination | Distributed transactions, eventual consistency |
| `external-dto-removal` | Optimized DTO handling | When external DTOs need cleanup |

Select via CLI:

```bash
./bin/cli.js generate user.nebula --architecture causal-saga
```

---

## 9.2 Default Architecture

The simplest pattern—generates standard Spring Boot microservices:

```bash
./bin/cli.js generate user.nebula --architecture default
```

**Generated components:**
- JPA entities with standard annotations
- Spring Data repositories
- Service layer with `@Transactional`
- REST controllers
- DTOs

**Best for:**
- Learning the DSL
- Simple CRUD applications
- Single-service deployments
- When you don't need distributed transactions

---

## 9.3 Causal-Saga Architecture

Full distributed transaction support with saga coordination:

```bash
./bin/cli.js generate user.nebula --architecture causal-saga
```

**Additional components:**
- Saga functionality classes
- Event publishing infrastructure
- Event subscription handlers
- Unit of Work with saga state tracking
- Compensation logic

**Best for:**
- Multi-aggregate operations
- Cross-service transactions
- Systems requiring eventual consistency
- Complex business workflows

---

## 9.4 Generated Output Comparison

### Default Architecture

```
applications/user/
├── microservices/user/
│   ├── aggregate/
│   │   ├── User.java
│   │   └── UserFactory.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       └── UserService.java
├── coordination/
│   ├── functionalities/
│   │   └── UserFunctionalities.java
│   └── webapi/
│       └── UserController.java
└── shared/
    └── dtos/
        └── UserDto.java
```

### Causal-Saga Architecture

```
applications/user/
├── microservices/user/
│   ├── aggregate/
│   │   ├── User.java
│   │   └── UserFactory.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── service/
│   │   └── UserService.java
│   └── events/
│       ├── publish/
│       │   ├── UserCreatedEvent.java
│       │   ├── UserUpdatedEvent.java
│       │   └── UserDeletedEvent.java
│       └── handling/
│           └── handlers/
├── coordination/
│   ├── functionalities/
│   │   └── UserFunctionalities.java
│   ├── webapi/
│   │   └── UserController.java
│   └── eventProcessing/
│       └── UserEventProcessing.java
├── sagas/
│   └── coordination/
│       └── user/
│           ├── CreateUserFunctionalitySagas.java
│           ├── UpdateUserFunctionalitySagas.java
│           ├── DeleteUserFunctionalitySagas.java
│           └── GetUserByIdFunctionalitySagas.java
└── shared/
    └── dtos/
        └── UserDto.java
```

---

## 9.5 Feature Flags

Control which components are generated:

```bash
./bin/cli.js generate user.nebula \
    --architecture causal-saga \
    --features events,validation,webapi,coordination
```

| Feature | Description |
|---------|-------------|
| `events` | Event publishing and handling |
| `validation` | Bean validation annotations |
| `webapi` | REST controllers |
| `coordination` | Functionalities layer |
| `sagas` | Saga functionality classes |

---

## 9.6 Choosing the Right Architecture

### Start with Default When:
- You're learning Nebula
- Building a prototype
- The aggregate is standalone
- You don't need cross-service coordination

### Upgrade to Causal-Saga When:
- Operations span multiple aggregates
- You need distributed transaction support
- Eventual consistency is acceptable
- Business workflows are complex

### Migration Path

Start simple and upgrade:

```bash
# Phase 1: Prototype with default
./bin/cli.js generate user.nebula --architecture default

# Phase 2: Add saga support when needed
./bin/cli.js generate user.nebula --architecture causal-saga
```

The DSL abstractions remain the same—only the generated infrastructure changes.

---

## 9.7 Architecture-Specific DSL Features

Some DSL features are only meaningful with specific architectures:

| Feature | Default | Causal-Saga |
|---------|---------|-------------|
| `@GenerateCrud` | Basic CRUD | CRUD + Sagas + Events |
| `Events { ... }` | Ignored | Full event system |
| `interInvariant` | Ignored | Cross-aggregate rules |
| `saga workflow` | Ignored | Workflow generation |
| `uses dto mapping` | Entity creation | + Version tracking |

---

## 9.8 Simulator Integration

The primary purpose of the causal-saga architecture is integration with the microservices simulator:

```
┌─────────────────────────────────────────────────────────┐
│                 Microservices Simulator                 │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │    User     │  │   Course    │  │    Quiz     │     │
│  │  Service    │  │   Service   │  │   Service   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│         │               │               │               │
│         └───────────────┴───────────────┘               │
│                         │                               │
│              ┌─────────────────────┐                   │
│              │  Saga Coordination  │                   │
│              │    & Event Store    │                   │
│              └─────────────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

The simulator provides:
- Event storage and delivery
- Saga state management
- Causal consistency enforcement
- Transaction coordination

---

## 9.9 Complete CLI Reference

```bash
./bin/cli.js generate <input> [options]

Options:
  -d, --destination <dir>      Output directory (default: ../../applications)
  -n, --name <name>            Project name
  -a, --architecture <arch>    Architecture: default, causal-saga
  -f, --features <features>    Comma-separated features
  --validate                   Validate DSL before generation

Examples:
  # Simple microservice
  ./bin/cli.js generate user.nebula

  # Full saga architecture
  ./bin/cli.js generate user.nebula --architecture causal-saga

  # Custom output location
  ./bin/cli.js generate user.nebula -d ./output --name my-service

  # Process entire project
  ./bin/cli.js generate ../abstractions/answers/
```

---

## Recap

Architecture selection determines the complexity and capabilities of the generated code. Start with `default` for simplicity, upgrade to `causal-saga` when you need distributed transaction support. The DSL abstractions remain portable across architectures, making it easy to evolve your system as requirements grow.

[← Back to Step 8](08-sagas.md)
