# Tutorial: Building a Library System

This chapter walks through building a complete library management system from scratch, combining every feature covered in chapters 03-07. No new syntax is introduced; this is a synthesis exercise.

> **Tied example:** [`06-tutorial`](../../abstractions/06-tutorial/): Member, Book, Loan aggregates with shared enums.

## What We're Building

A library system with three aggregates:

```
Member (simple CRUD)
   │
   └──── Loan (cross-references Member and Book)
   │
Book (invariants, events, repository)
```

- **Member**: library members with membership types (Chapter 03)
- **Book**: books with business rules and custom queries (Chapters 04-05)
- **Loan**: borrows that reference both Member and Book (Chapters 06-07)

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
    MembershipType {
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

    Root Entity Book {
        String title
        String author
        String genre
        Boolean available = true

        invariants {
            title.length() > 0 : "Book title cannot be blank"

            author.length() > 0 : "Author name cannot be blank"
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
- **Default value**: `available = true`
- **Invariants**: title and author can't be blank
- **Published event**: `BookDeletedEvent` is emitted when a Book is deleted (needed by Loan)
- **Repository queries**: custom lookup by genre and availability

> **Covered in:** [Chapter 04](04-Types-Enums-Properties.md) (defaults), [Chapter 05](05-Business-Rules-Repositories.md) (invariants, repository)

## Step 4: Build the Loan Aggregate

The most complex aggregate: cross-references, event-driven referential integrity, and temporal constraints. Create `loan.nebula`:

```nebula
Aggregate Loan {

    Entity LoanMember {
        from Member { name as memberName, email as memberEmail }
    }

    Entity LoanBook {
        from Book { title as bookTitle, author as bookAuthor, genre as bookGenre }
    }

    Root Entity Loan {
        LoanMember member
        LoanBook book
        LocalDateTime loanDate
        LocalDateTime dueDate

        invariants {
            member != null : "Loan must have a member"

            book != null : "Loan must have a book"

            loanDate.isBefore(dueDate) : "Loan date must be before due date"
        }
    }

    Events {
        subscribe MemberDeletedEvent from Member {
            when member.memberAggregateId == event.aggregateId
            action {
                this.state = INACTIVE
            }
        }

        subscribe BookDeletedEvent from Book {
            when book.bookAggregateId == event.aggregateId
            action {
                this.state = INACTIVE
            }
        }
    }
}
```

This aggregate demonstrates every major feature:

| Feature | Where | Covered In |
|---------|-------|-----------|
| Cross-aggregate references | `Entity LoanMember from Member` | [Chapter 06](06-Cross-Aggregate-References.md) |
| Field aliasing with type inference | `name as memberName` | [Chapter 06](06-Cross-Aggregate-References.md) |
| Temporal invariant | `loanDate.isBefore(dueDate)` | [Chapter 05](05-Business-Rules-Repositories.md) |
| Event-driven cascade | `subscribe MemberDeletedEvent ... action { this.state = INACTIVE }` | [Chapter 07](07-Events-Reactive-Patterns.md) |

### Delete Behavior

Both Member and Book deletions use the same event-driven cascade model:

- **Delete a Member** → `MemberDeletedEvent` fires → all Loans referencing that member cascade to INACTIVE
- **Delete a Book** → `BookDeletedEvent` fires → all Loans referencing that book cascade to INACTIVE

The delete always succeeds on the source side. Affected subscribers react asynchronously via event subscriptions.

## Step 5: Generate the Complete System

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/06-tutorial/
```

## Step 6: Compile and Verify

```bash
# Build simulator framework (if not done already)
cd simulator
mvn clean install -DskipTests

# Compile the generated application
cd ../applications/06-tutorial
mvn clean compile
```

## Step 7: Run the Tests

```bash
cd applications/06-tutorial
mvn clean test
```

## Design Decisions Recap

This tutorial illustrates several key DSL design decisions:

1. **One aggregate per file**: keeps each domain concept isolated and readable
2. **Shared enums in dedicated file**: enums are cross-cutting and belong in their own file
3. **Cross-references over duplication**: `LoanMember from Member` instead of copying fields
4. **Invariants for all business rules**: enforced automatically at the framework level
5. **Event-driven referential integrity**: reactive cascades via `subscribe` + `when` + `action` — matching the simulator's native architecture

---

**Previous:** [07-Events-Reactive-Patterns](07-Events-Reactive-Patterns.md) | **Next:** [09-Advanced-Patterns](09-Advanced-Patterns.md)
