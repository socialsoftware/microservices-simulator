# Step 4 · Custom Repositories

[📚 Guide Index](00-index.md)

> **Goal:** define custom database queries using the `Repository` block and understand the generated Spring Data JPA code.

[← Back to Step 3](03-dto-mappings.md) · [Next → Step 5](05-services.md)

---

## 4.1 Default Repository Generation

Every aggregate automatically gets a repository interface that extends the simulator's base repository:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }
}
```

```java
@Repository
@Transactional
public interface UserRepository extends AggregateRepository<User, Integer> {
    // Inherited methods: save, findById, findAll, delete, etc.
}
```

The base `AggregateRepository` provides standard CRUD operations plus aggregate-specific methods for version management and saga coordination.

---

## 4.2 Custom Repository Queries

For domain-specific queries, use the `Repository` block:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }

    Repository {
        Optional<Integer> findUserIdByUsername(String username);
        List<User> findByActive(Boolean active);
        Set<Integer> findAllActiveUserIds();
    }
}
```

```java
public interface UserCustomRepository {
    Optional<Integer> findUserIdByUsername(String username);
    List<User> findByActive(Boolean active);
    Set<Integer> findAllActiveUserIds();
}
```

These method signatures follow Spring Data JPA conventions, so many queries are derived automatically from the method name.

---

## 4.3 Custom JPQL Queries with @Query

For complex queries, use the `@Query` annotation:

```nebula
Aggregate Execution {
    Root Entity Execution {
        String acronym;
        String academicTerm;
        LocalDateTime endDate;
    }

    Repository {
        @Query("select e1.aggregateId from Execution e1 where e1.aggregateId NOT IN (select e2.aggregateId from Execution e2 where e2.state = 'DELETED' AND e2.sagaState != 'NOT_IN_SAGA')")
        Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga();

        @Query("select e from Execution e where e.academicTerm = :term and e.state != 'DELETED'")
        List<Execution> findByAcademicTerm(String term);
    }
}
```

```java
public interface ExecutionCustomRepository {
    @Query("select e1.aggregateId from Execution e1 where e1.aggregateId NOT IN (select e2.aggregateId from Execution e2 where e2.state = 'DELETED' AND e2.sagaState != 'NOT_IN_SAGA')")
    Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga();

    @Query("select e from Execution e where e.academicTerm = :term and e.state != 'DELETED'")
    List<Execution> findByAcademicTerm(String term);
}
```

---

## 4.4 Return Types

Repositories support various return types:

```nebula
Repository {
    // Single entity (nullable)
    User findByUsername(String username);

    // Optional wrapper
    Optional<Integer> findUserIdByEmail(String email);

    // Collections
    List<User> findByNameContaining(String namePart);
    Set<Integer> findAllActiveUserIds();

    // Primitives for aggregate queries
    Long countByActive(Boolean active);
}
```

| Return Type | Use Case |
|-------------|----------|
| `Optional<T>` | Single result that may not exist |
| `List<T>` | Ordered collection of results |
| `Set<T>` | Unique collection (often for IDs) |
| `T` | Single result (throws if not found) |

---

## 4.5 Saga-Safe Queries

When working with the causal-saga architecture, queries often need to filter by saga state:

```nebula
Repository {
    @Query("select q.aggregateId from Quiz q where q.state != 'DELETED' AND q.sagaState = 'NOT_IN_SAGA'")
    Set<Integer> findAvailableQuizIds();

    @Query("select q from Quiz q where q.execution.executionAggregateId = :executionId AND q.state != 'DELETED'")
    List<Quiz> findQuizzesByExecutionId(Integer executionId);
}
```

Common patterns:
- Filter out `DELETED` aggregates
- Check `sagaState` for concurrent transaction safety
- Join through embedded entity references

---

## 4.6 Query Parameters

Parameters are bound by name using `:paramName` syntax in JPQL:

```nebula
Repository {
    @Query("select t from Topic t where t.course.courseAggregateId = :courseId AND t.state != 'DELETED'")
    List<Topic> findTopicsByCourseId(Integer courseId);

    @Query("select count(q) from Question q where q.topic.topicAggregateId = :topicId AND q.state = 'ACTIVE'")
    Long countActiveQuestionsByTopicId(Integer topicId);
}
```

Parameter names in the method signature must match the `:paramName` placeholders in the query.

---

## 4.7 Complete Repository Example

```nebula
Aggregate Question {
    Root Entity Question {
        String title;
        String content;
        QuestionTopic topic;
        Set<Option> options;
    }

    Repository {
        // Derived query from method name
        Optional<Integer> findQuestionIdByTitle(String title);

        // Custom JPQL for saga-safe queries
        @Query("select q.aggregateId from Question q where q.aggregateId NOT IN (select q2.aggregateId from Question q2 where q2.state = 'DELETED' AND q2.sagaState != 'NOT_IN_SAGA')")
        Set<Integer> findQuestionIdsOfAllNonDeletedForSaga();

        // Join query through embedded entity
        @Query("select q from Question q where q.topic.topicAggregateId = :topicId AND q.state != 'DELETED'")
        List<Question> findQuestionsByTopicId(Integer topicId);

        // Aggregation query
        @Query("select count(q) from Question q where q.state = 'ACTIVE'")
        Long countActiveQuestions();
    }
}
```

---

## Recap

Custom repositories let you define domain-specific queries while Nebula handles the Spring Data JPA boilerplate. Use method name conventions for simple queries and `@Query` annotations for complex JPQL. When building saga-aware systems, always consider filtering by `state` and `sagaState` to ensure data consistency.

[← Back to Step 3](03-dto-mappings.md) · [Next → Step 5](05-services.md)
