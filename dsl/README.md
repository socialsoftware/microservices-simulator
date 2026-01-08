# Nebula DSL

Nebula is a Domain-Specific Language for generating Spring Boot microservices from high-level domain abstractions. It transforms declarative aggregate definitions into production-ready Java code designed for the [Microservices Simulator](../README.md).

## What It Does

Write domain models like this:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }

    WebAPIEndpoints {
        @GenerateCrud;
    }

    Service UserService {
        @GenerateCrud;
    }
}
```

Generate complete microservices:

```
applications/answers/
├── microservices/user/
│   ├── aggregate/          # JPA entities, factories
│   ├── repository/         # Spring Data repositories
│   ├── service/            # Business logic layer
│   └── events/             # Event publishing & handling
├── coordination/
│   ├── functionalities/    # Saga orchestration
│   └── webapi/             # REST controllers
├── sagas/                  # Distributed transaction workflows
└── shared/
    └── dtos/               # Data transfer objects
```

## Key Features

| Feature | Description |
|---------|-------------|
| **Aggregate-First Design** | Define bounded contexts with root entities and supporting entities |
| **Automatic CRUD** | Generate create, read, update, delete operations with `@GenerateCrud` |
| **Cross-Aggregate DTOs** | Reference external aggregates with `uses dto` mapping syntax |
| **Custom Repositories** | Define JPQL queries with `@Query` annotations |
| **REST API Generation** | Produce controllers with customizable endpoints |
| **Event-Driven Architecture** | Publish and subscribe to events across aggregates |
| **Saga Coordination** | Distributed transactions with compensation logic |

## Quick Start

```bash
# Install and build
cd dsl/nebula
npm install
npm run langium:generate && npm run build

# Generate from abstractions
./bin/cli.js generate ../abstractions/answers/
```

## Project Structure

```
dsl/
├── abstractions/           # Your .nebula domain files
│   └── answers/            # Example project
├── docs/
│   └── developer-guide/    # Step-by-step documentation
└── nebula/
    ├── src/
    │   ├── cli/            # Code generators
    │   └── language/       # Grammar definition
    └── bin/                # CLI entry point
```

## Documentation

📚 **[Developer Guide](docs/developer-guide/00-index.md)** — Complete step-by-step documentation

| Topic | Description |
|-------|-------------|
| [Setup & CLI](docs/developer-guide/01-setup-and-cli.md) | Installation and first generation |
| [Entities](docs/developer-guide/02-entities-and-dtos.md) | Root entities, relationships, JPA mappings |
| [DTO Mappings](docs/developer-guide/03-dto-mappings.md) | Cross-aggregate `uses dto` syntax |
| [Repositories](docs/developer-guide/04-repositories.md) | Custom queries, `@Query` annotations |
| [Services](docs/developer-guide/05-services.md) | `@GenerateCrud`, Unit of Work pattern |
| [Web API](docs/developer-guide/06-web-api.md) | Controllers, REST endpoints |
| [Events](docs/developer-guide/07-events.md) | Publishing, subscriptions, handlers |
| [Sagas](docs/developer-guide/08-sagas.md) | Distributed transactions, compensation |

## Example Abstraction

A complete example from the `answers` project:

```nebula
import shared-enums;

Aggregate Quiz {
    Entity QuizExecution uses dto ExecutionDto mapping {
        aggregateId -> executionAggregateId;
        acronym -> executionAcronym;
    } {
        Integer executionAggregateId;
        String executionAcronym;
    }

    Root Entity Quiz {
        String title;
        QuizType quizType;
        LocalDateTime availableDate;
        QuizExecution execution;
        Set<QuizQuestion> questions;
    }

    Repository {
        @Query("select q.aggregateId from Quiz q where q.state != 'DELETED'")
        Set<Integer> findActiveQuizIds();
    }

    WebAPIEndpoints {
        @GenerateCrud;
    }

    Service QuizService {
        @GenerateCrud;
    }
}
```

## CLI Reference

```bash
./bin/cli.js generate <abstractions-path> [options]

Options:
  -o, --output <dir>    Output directory (default: ../../applications)
```

**Examples:**

```bash
# Generate from a folder of .nebula files
./bin/cli.js generate ../abstractions/answers/

# Generate to a custom output directory
./bin/cli.js generate ../abstractions/answers/ -o ./output
```
