# Nebula DSL

Nebula is a Domain-Specific Language for generating Spring Boot microservices from high-level domain abstractions. It transforms declarative aggregate definitions into production-ready Java code designed for the [Microservices Simulator](../README.md).

## What It Does

Write domain models like this:

```nebula
Aggregate User {
    @GenerateCrud

    Root Entity User {
        String name
        String username
        final UserRole role
        Boolean active

        invariants {
            check nameNotBlank { name.length() > 0 }
                error "User name cannot be blank"
        }
    }
}
```

Generate complete microservices:

```
applications/answers/
├── microservices/user/
│   ├── aggregate/          # JPA entities, factories, repositories
│   ├── service/            # Business logic layer
│   └── events/             # Event publishing & handling
├── coordination/
│   ├── functionalities/    # Orchestration layer
│   ├── eventProcessing/    # Event coordination
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
| **Cross-Aggregate References** | Reference external aggregates with `from` and `map...as` type-inferred mappings |
| **Business Invariants** | Define validation rules with `check` blocks and custom error messages |
| **Custom Repositories** | Define JPQL queries with `@Query` annotations |
| **REST API Generation** | Produce controllers with customizable endpoints |
| **Event-Driven Architecture** | Publish and subscribe to events across aggregates |
| **Referential Integrity** | Declare cross-aggregate constraints with `References` blocks |
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
│   ├── answers/            # Answers case study (11 aggregates)
│   └── teastore/           # TeaStore case study (6 aggregates)
├── docs/
│   ├── user-guide/         # DSL syntax and usage guide
│   └── developer-guide/    # Generator internals and architecture
└── nebula/
    ├── src/
    │   ├── cli/            # Code generators
    │   └── language/       # Grammar definition
    └── bin/                # CLI entry point
```

## Documentation

### User Guide

| Chapter | Description |
|---------|-------------|
| [Introduction](docs/user-guide/01-Introduction.md) | Overview and key features |
| [Getting Started](docs/user-guide/02-Getting-Started.md) | Installation and first generation |
| [DSL Syntax](docs/user-guide/03-DSL-Syntax.md) | All language constructs with examples |
| [Generated Code](docs/user-guide/04-Generated-Code.md) | What gets generated and how to use it |
| [Reference](docs/user-guide/05-Reference.md) | Complete grammar and CLI reference |

### Developer Guide

| Chapter | Description |
|---------|-------------|
| [Architecture](docs/developer-guide/01-Architecture.md) | Codebase structure and generation pipeline |
| [Grammar Internals](docs/developer-guide/02-Grammar-Internals.md) | Langium grammar and AST design |
| [Generator System](docs/developer-guide/03-Generator-System.md) | Registry, facades, and generator categories |
| [Template System](docs/developer-guide/04-Template-System.md) | Handlebars templates and helpers |
| [Adding Features](docs/developer-guide/05-Adding-Features.md) | How to extend the DSL |
| [Patterns Reference](docs/developer-guide/06-Patterns-Reference.md) | Design patterns and naming conventions |
| [VSCode Extension](docs/developer-guide/07-VSCode-Extension.md) | Extension architecture and packaging |

## Example Abstraction

A complete example from the `answers` project:

```nebula
Aggregate Quiz {
    @GenerateCrud

    Entity QuizExecution from Execution {
        map acronym as executionAcronym
        map academicTerm as executionAcademicTerm
    }

    Entity QuizQuestion from Question {
        AggregateState questionState
        map title as questionTitle
        map content as questionContent
        Integer questionSequence
    }

    Root Entity Quiz {
        String title
        QuizType quizType
        LocalDateTime availableDate
        LocalDateTime conclusionDate
        QuizExecution execution
        Set<QuizQuestion> questions

        invariants {
            check titleNotBlank { title.length() > 0 }
                error "Quiz title cannot be blank"
            check dateOrdering {
                availableDate.isBefore(conclusionDate)
            }
                error "Available date must be before conclusion date"
        }
    }

    References {
        execution -> Execution {
            onDelete: prevent
            message: "Cannot delete execution that has quizzes"
        }
    }

    Events {
        subscribe ExecutionUpdatedEvent

        interInvariant QUIZ_EXECUTION_EXISTS {
            subscribe ExecutionDeletedEvent from Execution {
                execution.executionAggregateId == event.aggregateId
            }
        }
    }
}
```

## CLI Reference

```bash
./bin/cli.js generate <abstractions-path> [options]

Options:
  -o, --output <dir>    Output directory (default: ../../applications)
  -d, --debug           Enable debug mode with detailed error output
  -v, --verbose         Enable verbose logging
  --no-validate         Skip validation during generation
```

**Examples:**

```bash
# Generate from a folder of .nebula files
./bin/cli.js generate ../abstractions/answers/

# Generate to a custom output directory
./bin/cli.js generate ../abstractions/answers/ -o ./output

# Generate with debug output
./bin/cli.js generate ../abstractions/answers/ --debug --verbose
```
