# Tutorial: Building a Library System

This chapter walks through building a complete library management system from scratch, combining every feature covered in chapters 03-07. No new syntax is introduced; this is a synthesis exercise.

> **Tied example:** [`06-tutorial`](../examples/abstractions/06-tutorial/): Member, Book, Loan aggregates with shared enums.

## What We're Building

A library system with three aggregates:

```
Member (simple CRUD)
   │
   └──── Loan (cross-references Member and Book)
   │
Book (invariants, events, repository)
```

- **Member**:library members with membership types (Chapter 03)
- **Book**:books with business rules and custom queries (Chapters 04-05)
- **Loan**:borrows that reference both Member and Book (Chapters 06-07)

By the end, 4 `.nebula` files (~100 lines total) generate a complete Spring Boot application with **124 Java files** and **~5,100 lines of code**.

## Step 1: Project Setup and Shared Enums

Create a `nebula.config.json` file in your abstractions directory:

```json
{
    "basePackage": "pt.ulisboa.tecnico.socialsoftware",
    "framework": {
        "version": "3.1.0-SNAPSHOT"
    }
}
```

Then start with the shared enumerations. Create `shared-enums.nebula`:

```nebula
SharedEnums {
    enum MembershipType {
        BASIC,
        PREMIUM
    }
}
```

This defines a `MembershipType` enum used by the Member aggregate. Enums are project-wide and can be referenced from any aggregate.

> **Covered in:** [Chapter 04: Types, Enums, and Properties](04-Types-Enums-Properties.md)

## Step 2: Build the Member Aggregate

The simplest aggregate: pure CRUD with no business rules. Create `member.nebula`:

```nebula
Aggregate Member {
    @GenerateCrud

    Root Entity Member {
        String name
        String email
        MembershipType membership
    }
}
```

This generates:
- Entity, DTO, Factory, Repository, Service
- REST endpoints: `POST/GET/PUT/DELETE /members`
- Saga workflows for distributed transactions

> **Covered in:** [Chapter 03: Your First Aggregate](03-Your-First-Aggregate.md)

## Step 3: Build the Book Aggregate

Add business rules, events, and custom queries. Create `book.nebula`:

```nebula
Aggregate Book {
    @GenerateCrud

    Root Entity Book {
        String title
        String author
        String genre
        Boolean available = true

        invariants {
            check titleNotBlank { title.length() > 0 }
                error "Book title cannot be blank"

            check authorNotBlank { author.length() > 0 }
                error "Author name cannot be blank"
        }
    }

    Events {
        publish BookDeletedEvent {
            Integer bookId
            String title
        }
    }

    Repository {
        List<Book> findByGenre(String genre)
        List<Book> findByAvailableTrue()
    }
}
```

New features compared to Member:
- **Default value**:`available = true`
- **Invariants**:title and author can't be blank
- **Published event**:`BookDeletedEvent` is emitted when a Book is deleted (needed by Loan)
- **Repository queries**:custom lookup by genre and availability

> **Covered in:** [Chapter 04](04-Types-Enums-Properties.md) (defaults), [Chapter 05](05-Business-Rules-Repositories.md) (invariants, repository)

## Step 4: Build the Loan Aggregate

The most complex aggregate: cross-references, referential integrity, and temporal constraints. Create `loan.nebula`:

```nebula
Aggregate Loan {
    @GenerateCrud

    Entity LoanMember from Member {
        map name as memberName
        map email as memberEmail
    }

    Entity LoanBook from Book {
        map title as bookTitle
        map author as bookAuthor
        map genre as bookGenre
    }

    Root Entity Loan {
        LoanMember member
        LoanBook book
        LocalDateTime loanDate
        LocalDateTime dueDate

        invariants {
            check memberNotNull { member != null }
                error "Loan must have a member"

            check bookNotNull { book != null }
                error "Loan must have a book"

            check dateOrdering { loanDate.isBefore(dueDate) }
                error "Loan date must be before due date"
        }
    }

    References {
        member -> Member {
            onDelete: cascade
            message: "Member deleted, removing their loans"
        }
        book -> Book {
            onDelete: prevent
            message: "Cannot delete book that has active loans"
        }
    }

    Events {
        interInvariant MEMBER_EXISTS {
            subscribe MemberDeletedEvent from Member {
                member.memberAggregateId == event.aggregateId
            }
        }

        interInvariant BOOK_EXISTS {
            subscribe BookDeletedEvent from Book {
                book.bookAggregateId == event.aggregateId
            }
        }
    }
}
```

This aggregate demonstrates every major feature:

| Feature | Where | Covered In |
|---------|-------|-----------|
| Cross-aggregate references | `Entity LoanMember from Member` | [Chapter 06](06-Cross-Aggregate-References.md) |
| Field mapping with type inference | `map name as memberName` | [Chapter 06](06-Cross-Aggregate-References.md) |
| Temporal invariant | `loanDate.isBefore(dueDate)` | [Chapter 05](05-Business-Rules-Repositories.md) |
| Referential integrity | `References` block | [Chapter 06](06-Cross-Aggregate-References.md) |
| Cascade delete | `onDelete: cascade` for member | [Chapter 06](06-Cross-Aggregate-References.md) |
| Prevent delete | `onDelete: prevent` for book | [Chapter 06](06-Cross-Aggregate-References.md) |
| Inter-invariants | `MEMBER_EXISTS`, `BOOK_EXISTS` | [Chapter 07](07-Events-Reactive-Patterns.md) |

### Delete Behavior

The two delete policies create different behaviors:

- **Delete a Member** → All their Loans are automatically deleted (`cascade`)
- **Delete a Book** → Fails if any Loan references it (`prevent`), protecting against orphaned loans

## Step 5: Generate the Complete System

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ../docs/examples/generated
```

Expected output:

```
Starting generation for: ../docs/examples/abstractions/06-tutorial/
Found 4 Nebula files
Validating DSL files... OK

Generating code...
  Book                13 files
  Loan                27 files
  Member              14 files

Generated project files (integration, pom.xml, .gitignore, 1 shared enum)

Code generation completed successfully!
```

## Step 6: Compile and Verify

```bash
# Build simulator framework (if not done already)
cd simulator
mvn clean install -DskipTests

# Compile the generated application
cd dsl/docs/examples/generated/06-tutorial
mvn clean compile
```

## Step 7: Run the App

The generated project is a Spring Boot application backed by PostgreSQL. Boot it end-to-end in three commands:

```bash
# 1. Start Postgres and create the tutorial database
docker compose up -d postgres
docker exec -i postgres psql -U postgres -c "CREATE DATABASE tutorial_db;"

# 2. Run the application (sagas profile is the default)
cd dsl/docs/examples/generated/06-tutorial
mvn spring-boot:run
```

When you see `Started TutorialSimulator in N seconds`, the app is ready on `http://localhost:8923`.

> Running on the host instead of inside docker-compose? The default `application.properties` points at host `postgres`. Override the URL on the command line:
>
> ```bash
> mvn spring-boot:run -Dspring-boot.run.arguments="\
>     --spring.datasource.url=jdbc:postgresql://localhost:5432/tutorial_db \
>     --spring.datasource.password=postgres"
> ```

Try it out with `curl`:

```bash
# Create a member
curl -X POST http://localhost:8923/members/create \
    -H 'Content-Type: application/json' \
    -d '{"name":"Alice","email":"alice@example.com","membership":"BASIC"}'

# List all members
curl http://localhost:8923/members
```

To run the Spock/Groovy tests instead:

```bash
mvn test -P test-sagas
```

For distributed deployments (`stream`, `grpc`, microservices), see the [project README](../../../README.md).

## What We Built: By the Numbers

| Metric | Value |
|--------|-------|
| DSL files | 4 |
| DSL lines | ~100 |
| Generated Java files | 124 |
| Generated Java lines | ~5,100 |
| Code reduction | ~50x |

### DSL Files vs Generated Structure

```
DSL (4 files, ~100 lines)         Generated (124 files, ~5,100 lines)
─────────────────────────          ──────────────────────────────────
shared-enums.nebula         →     shared/enums/MembershipType.java
member.nebula               →     microservices/member/ (5 files)
                                  coordination/ (3 files)
                                  sagas/ (3 files)
                                  shared/dtos/ (2 files)
book.nebula                 →     microservices/book/ (7 files)
                                  coordination/ (3 files)
                                  sagas/ (3 files)
                                  shared/dtos/ (2 files)
loan.nebula                 →     microservices/loan/ (8 files)
                                  coordination/ (3 files)
                                  sagas/ (5 files)
                                  shared/dtos/ (2 files)
                            →     pom.xml, application configs, main class
```

## Design Decisions Recap

This tutorial illustrates several key DSL design decisions:

1. **One aggregate per file**:keeps each domain concept isolated and readable
2. **Shared enums in dedicated file**:enums are cross-cutting and belong in their own file
3. **`@GenerateCrud` on every aggregate**:start with CRUD, add custom methods later
4. **Cross-references over duplication**:`LoanMember from Member` instead of copying fields
5. **Invariants for all business rules**:enforced automatically at the framework level
6. **References for delete handling**:declarative instead of hand-coded cascade logic
7. **Inter-invariants for event-driven integrity**:reactive enforcement across aggregate boundaries

---

**Previous:** [07-Events-Reactive-Patterns](07-Events-Reactive-Patterns.md) | **Next:** [09-Advanced-Patterns](09-Advanced-Patterns.md)
