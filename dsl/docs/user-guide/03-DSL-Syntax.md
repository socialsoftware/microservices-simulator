# DSL Syntax

This chapter covers all Nebula DSL language constructs, progressing from simple to complex. Examples use the **tutorial project** (library domain) to build understanding incrementally, then reference the Answers and TeaStore projects for advanced patterns.

## Table of Contents

- [Shared Enums](#shared-enums)
- [Your First Aggregate: Member](#your-first-aggregate-member)
- [Adding Invariants, Events, and Repositories: Book](#adding-invariants-events-and-repositories-book)
- [Cross-Aggregate References: Loan](#cross-aggregate-references-loan)
- [Extract Pattern](#extract-pattern)
- [Services](#services)
- [Web API](#web-api)
- [Exception Messages](#exception-messages)
- [Best Practices](#best-practices)

## Shared Enums

Before defining aggregates, define shared enumerations used across them:

```nebula
SharedEnums {
    enum MembershipType {
        BASIC,
        PREMIUM
    }
}
```

Generated as standard Java enums in the `shared/enums/` package.

## Your First Aggregate: Member

The simplest aggregate -- a standalone entity with CRUD operations:

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

This is the minimum needed to generate a complete microservice. Let's break it down:

### Aggregate

Every domain concept is wrapped in an `Aggregate` block. The aggregate name determines package names, class names, and file paths.

### `@GenerateCrud`

This annotation generates complete CRUD operations across all layers: service methods, REST controller endpoints, functionalities, and saga workflows.

### Root Entity

Every aggregate must have exactly one root entity. It extends the simulator's `Aggregate` base class and automatically gets `aggregateId`, `version`, and `state` fields.

### Data Types

**Primitives:**
```nebula
String name
Integer count
Long timestamp
Float price
Double percentage
Boolean active
```

**Temporal:**
```nebula
LocalDateTime createdAt
LocalDateTime availableDate
```

**Collections:**
```nebula
List<Integer> scores
Set<ExecutionUser> users
```

**Optional:**
```nebula
Optional<LocalDateTime> deadline
```

**Enums** (defined in `SharedEnums`):
```nebula
MembershipType membership
```

**Entity references** (composition):
```nebula
LoanBook book
```

**Built-in types:**
```nebula
AggregateState state
UnitOfWork unitOfWork
```

### Property Modifiers

**Immutable (`final`)** -- cannot be changed after creation:
```nebula
final String genre
final LocalDateTime createdAt
```

**Default values:**
```nebula
Boolean available = true
Integer count = 0
AggregateState state = AggregateState.ACTIVE
```

**DTO exclusion** -- field not included in DTOs:
```nebula
String internalId dto-exclude
```

**Multiple properties** of same type:
```nebula
String firstName, lastName, email
```

## Adding Invariants, Events, and Repositories: Book

The Book aggregate builds on Member by adding business rules, events, and custom queries:

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

### Invariants

Business rules enforced at the aggregate level. Error messages are mandatory.

**Simple invariants:**
```nebula
invariants {
    check nameNotBlank { name.length() > 0 }
        error "User name cannot be blank"

    check roleNotNull { role != null }
        error "User role is required"
}
```

**Temporal constraints:**
```nebula
invariants {
    check dateOrdering {
        availableDate.isBefore(conclusionDate) &&
        conclusionDate.isBefore(resultsDate)
    }
        error "Dates must be in chronological order"
}
```

**Collection constraints:**
```nebula
invariants {
    check topicsNotEmpty { topics != null && !topics.isEmpty() }
        error "Question must have at least one topic"
}
```

**Supported expressions:**

| Category | Operators |
|----------|-----------|
| Comparisons | `<`, `>`, `<=`, `>=`, `==`, `!=` |
| Boolean logic | `&&`, `||`, `!` |
| String | `name.length() > 0`, `email.contains("@")` |
| Temporal | `startDate.isBefore(endDate)`, `deadline.isAfter(now())` |
| Collection | `users.size() > 0`, `questions.isEmpty()`, `topics.unique(topicId)` |

### Published Events

Define events that this aggregate emits:

```nebula
Events {
    publish BookDeletedEvent {
        Integer bookId
        String title
    }
}
```

Generates event classes and publishing logic in services.

### Repositories

**Spring Data method names** (parsed automatically):
```nebula
Repository {
    List<Book> findByGenre(String genre)
    List<Book> findByAvailableTrue()
    Optional<User> findByUsername(String username)
}
```

**Custom JPQL queries:**
```nebula
Repository {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds()

    @Query("select a from Answer a where a.quiz.quizAggregateId = :quizId")
    List<Answer> findByQuizId(Integer quizId)
}
```

Parameters are bound using `:paramName` syntax matching method parameter names.

## Cross-Aggregate References: Loan

The Loan aggregate demonstrates cross-aggregate references, event subscriptions, and referential integrity -- the most powerful DSL features:

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

### Non-Root Entities with `from`

Reference entities from other aggregates with automatic type inference:

```nebula
Entity LoanMember from Member {
    map name as memberName      // String (inferred from Member.name)
    map email as memberEmail    // String (inferred from Member.email)
}
```

The generator:
1. Finds the `Member` aggregate
2. Gets its root entity
3. Infers types from `Member.name` and `Member.email`

**Generated code:**
```java
@Embeddable
public class LoanMember {
    private Integer memberAggregateId;  // Auto-added
    private Integer memberVersion;      // Auto-added
    private String memberName;          // Inferred: String
    private String memberEmail;         // Inferred: String
}
```

Non-root entities are simple JPA entities (no version chain, no invariants).

### Cross-File Type Inference

Type inference works across file boundaries. A model registry tracks all aggregates:

```nebula
// member.nebula
Aggregate Member {
    Root Entity Member {
        String name
        String email
    }
}
```

```nebula
// loan.nebula (different file)
Entity LoanMember from Member {
    map name as memberName      // String (from member.nebula)
    map email as memberEmail    // String (from member.nebula)
}
```

### Base Fields

All cross-aggregate entities automatically include:

```java
private Integer <aggregate>AggregateId;  // e.g., memberAggregateId
private Integer <aggregate>Version;      // e.g., memberVersion
```

### References

Define how to handle deletion of referenced aggregates:

```nebula
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
```

| Action | Behavior |
|--------|----------|
| `prevent` | Throw exception if referenced aggregate is deleted |
| `setNull` | Set reference to null when referenced aggregate is deleted |
| `cascade` | Delete this aggregate when referenced aggregate is deleted |

### Subscribed Events

Subscribe to events from other aggregates for data synchronization:

```nebula
subscribe ExecutionUpdatedEvent
subscribe TopicUpdatedEvent
```

These generate event handlers that update local copies of data when the source aggregate changes.

### Inter-Invariants

Inter-invariants enforce referential integrity across aggregates. They use `subscribe ... from Aggregate { condition }` to match events against local data:

```nebula
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
```

For a more complex example with multiple subscriptions per inter-invariant, see the Answers project's `execution.nebula`:

```nebula
Events {
    interInvariant USERS_EXIST {
        subscribe UserDeletedEvent from User {
            users.userAggregateId == event.aggregateId
        }
        subscribe UserUpdatedEvent from User {
            users.userAggregateId == event.aggregateId
        }
    }
}
```

## Extract Pattern

Extract specific fields from collection properties in cross-aggregate references. This is an advanced feature used in the Answers project.

### Syntax

```nebula
Entity AnswerQuiz from Quiz {
    map questions.aggregateId as quizQuestionsAggregateIds
}
```

This extracts `Set<Integer>` from `Set<QuizQuestion>` by accessing `aggregateId` on each element.

### How It Works

1. `questions` is a `Set<QuizQuestion>` in the Quiz root entity
2. `.aggregateId` accesses the `aggregateId` field of `QuizQuestion`
3. The result is `Set<Integer>` -- the collection type is preserved

### Generated Code

```java
// Entity
private Set<Integer> quizQuestionsAggregateIds = new HashSet<>();

// DTO
private Set<Integer> quizQuestionsAggregateIds;
public Set<Integer> getQuizQuestionsAggregateIds() { ... }
public void setQuizQuestionsAggregateIds(Set<Integer> quizQuestionsAggregateIds) { ... }
```

### Common Use Cases

```nebula
// Extract aggregate IDs from collections
map participants.aggregateId as participantIds

// Extract business fields
map products.sku as productSkus

// Extract status fields
map orders.status as orderStatuses
```

### Limitations

- Single-level extraction only (no `a.b.c`)
- Root entity properties only
- No filtering support
- Works with `List<T>` and `Set<T>` only

## Services

### Auto-Generated Service

With `@GenerateCrud`, a complete service is generated:

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

Generated methods:
- `createMember(CreateMemberRequestDto, UnitOfWork)`
- `getMemberById(Integer, UnitOfWork)`
- `getAllMembers(UnitOfWork)`
- `updateMember(Integer, MemberDto, UnitOfWork)`
- `deleteMember(Integer, UnitOfWork)`

Collection properties additionally get add/remove methods.

### Custom Service Methods

Define additional methods:

```nebula
Service UserService {
    @Transactional

    methods {
        activateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        deactivateUser(Integer userId, UnitOfWork unitOfWork): UserDto
    }
}
```

If the name is omitted, it defaults to `<Aggregate>Service`:

```nebula
Service {
    @Transactional
}
```

## Web API

### Auto-Generated Endpoints

`@GenerateCrud` creates REST endpoints:

```java
POST   /members                 createMember
GET    /members/{id}            getMember
GET    /members                 getAllMembers
PUT    /members/{id}            updateMember
DELETE /members/{id}            deleteMember
```

### Custom Endpoints

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

## Exception Messages

Define project-specific error messages:

```nebula
exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
    INVALID_USERNAME: "Username '%s' is invalid"
    DUPLICATE_EMAIL: "Email '%s' is already registered"
}
```

Generated as static constants in an exception class.

## Complete Example

Here's the full tutorial project -- three aggregates that progressively demonstrate DSL features:

### 1. Member (simplest -- pure CRUD)

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

### 2. Book (intermediate -- invariants, events, repository)

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

### 3. Loan (advanced -- cross-aggregate refs, references, inter-invariants)

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

For a more complex real-world example, see the Answers project's `execution.nebula` which adds custom JPQL queries, collection-type cross-aggregate references (`Set<ExecutionUser>`), and multiple event subscriptions per inter-invariant.

## Best Practices

### One Aggregate Per File
Keep each aggregate in its own `.nebula` file, named after the aggregate (lowercase).

### Use `@GenerateCrud` Liberally
Most aggregates need CRUD operations. Start with `@GenerateCrud` and add custom methods as needed.

### Use Cross-Aggregate References
Instead of duplicating data, reference other aggregates with `from`:
```nebula
Entity LoanMember from Member {
    map name as memberName
}
```

### Define Invariants
Business rules in invariants are enforced automatically. Always include descriptive error messages.

### Use References for Integrity
Define `References` blocks to handle cascading deletes and referential integrity.

---

**Previous:** [02-Getting-Started](02-Getting-Started.md) | **Next:** [04-Generated-Code](04-Generated-Code.md)
