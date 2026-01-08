# Step 3 · DTO Schemas and Cross-Aggregate Mappings

[📚 Guide Index](00-index.md)

> **Goal:** understand how DTOs are generated automatically and how to map fields from external aggregates using the `uses dto` syntax.

[← Back to Step 2](02-entities-and-dtos.md) · [Next → Step 4](04-repositories.md)

---

## 3.1 Automatic DTO Generation

When you define a root entity, Nebula automatically generates a corresponding DTO class that mirrors the entity's fields plus aggregate metadata:

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
public class UserDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String username;
    private Boolean active;

    public UserDto() { }

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.version = user.getVersion();
        this.state = user.getState();
        this.name = user.getName();
        this.username = user.getUsername();
        this.active = user.getActive();
    }

    // getters and setters...
}
```

Root entity DTOs automatically include:
- `aggregateId` - the unique identifier
- `version` - for optimistic concurrency
- `state` - the aggregate's lifecycle state

---

## 3.2 Cross-Aggregate References with `uses dto`

In microservices, aggregates often need to reference data from other aggregates. Instead of tight coupling, Nebula provides the `uses dto` pattern to embed denormalized copies of external aggregate data.

### Basic Pattern

```nebula
Aggregate Quiz {
    Entity QuizExecution uses dto ExecutionDto mapping {
        aggregateId -> executionAggregateId;
        acronym -> executionAcronym;
        academicTerm -> executionAcademicTerm;
    } {
        Integer executionAggregateId;
        String executionName;
        String executionAcronym;
        String executionAcademicTerm;
    }

    Root Entity Quiz {
        String title;
        QuizExecution execution;
    }
}
```

This generates:

```java
// QuizExecution.java - a supporting entity that holds denormalized Execution data
@Entity
public class QuizExecution {
    @Id @GeneratedValue
    private Integer id;
    
    private Integer executionAggregateId;
    private String executionName;
    private String executionAcronym;
    private String executionAcademicTerm;
    
    @OneToOne(mappedBy = "execution")
    private Quiz quiz;

    protected QuizExecution() { }

    // Constructor that copies from ExecutionDto using the mapping
    public QuizExecution(ExecutionDto executionDto) {
        this.executionAggregateId = executionDto.getAggregateId();
        this.executionAcronym = executionDto.getAcronym();
        this.executionAcademicTerm = executionDto.getAcademicTerm();
    }

    // getters and setters...
}
```

### Understanding the Mapping Syntax

```nebula
Entity QuizExecution uses dto ExecutionDto mapping {
    aggregateId -> executionAggregateId;  // source field -> local field
    acronym -> executionAcronym;
    academicTerm -> executionAcademicTerm;
} {
    Integer executionAggregateId;  // local field declarations
    String executionName;
    String executionAcronym;
    String executionAcademicTerm;
}
```

The mapping block tells Nebula:
1. **Source DTO**: `ExecutionDto` is the external DTO to read from
2. **Field mappings**: `sourceField -> localField` defines how to copy data
3. **Local fields**: The entity's own field declarations (may include unmapped fields like `executionName`)

---

## 3.3 Version Tracking for Consistency

When referencing external aggregates, you often need to track their version for causal consistency:

```nebula
Entity ExecutionUser uses dto UserDto mapping {
    aggregateId -> userAggregateId;
    version -> userVersion;
    state -> userState;
    name -> userName;
    username -> userUsername;
    active -> userActive;
} {
    Integer userAggregateId;
    Integer userVersion;
    AggregateState userState;
    String userName;
    String userUsername;
    Boolean userActive;
}
```

This pattern allows the saga coordination layer to:
- Track which version of the external aggregate was seen
- Detect stale reads during distributed transactions
- Support eventual consistency patterns

---

## 3.4 Collections of Cross-Aggregate References

You can reference collections of external entities:

```nebula
Aggregate Quiz {
    Entity QuizQuestion uses dto QuestionDto mapping {
        aggregateId -> questionAggregateId;
        version -> questionVersion;
        state -> questionState;
        title -> questionTitle;
        content -> questionContent;
    } {
        Integer questionAggregateId;
        Integer questionVersion;
        AggregateState questionState;
        String questionTitle;
        String questionContent;
        Integer questionSequence;
    }

    Root Entity Quiz {
        String title;
        Set<QuizQuestion> questions;
    }
}
```

The generated code handles the collection relationship:

```java
// Quiz.java excerpt
@OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<QuizQuestion> questions = new HashSet<>();

public void addQuestion(QuizQuestion question) {
    questions.add(question);
    question.setQuiz(this);
}
```

---

## 3.5 Complete Cross-Aggregate Example

Here's a complete example showing the Execution aggregate that references both Course and User:

```nebula
import shared-enums;

Aggregate Execution {
    Entity ExecutionCourse uses dto CourseDto mapping {
        aggregateId -> courseAggregateId;
        version -> courseVersion;
        type -> courseType;
        name -> courseName;
    } {
        Integer courseAggregateId;
        String courseName;
        CourseType courseType;
        Integer courseVersion;
    }

    Entity ExecutionUser uses dto UserDto mapping {
        aggregateId -> userAggregateId;
        version -> userVersion;
        state -> userState;
        name -> userName;
        username -> userUsername;
        active -> userActive;
    } {
        Integer userAggregateId;
        Integer userVersion;
        AggregateState userState;
        String userName;
        String userUsername;
        Boolean userActive;
    }

    Root Entity Execution {
        String acronym;
        String academicTerm;
        LocalDateTime endDate;
        ExecutionCourse course;
        Set<ExecutionUser> users;
    }
}
```

This generates:
- `Execution.java` - the root aggregate entity
- `ExecutionCourse.java` - denormalized course reference with constructor from `CourseDto`
- `ExecutionUser.java` - denormalized user reference with constructor from `UserDto`
- `ExecutionDto.java` - DTO for the Execution aggregate

---

## 3.6 When to Use `uses dto`

Use this pattern when:
- **Cross-aggregate references**: You need data from another bounded context
- **Denormalization**: Performance requires local copies of external data
- **Event consistency**: The saga coordination needs version tracking
- **Avoiding tight coupling**: You don't want direct entity dependencies

Avoid when:
- The relationship is within the same aggregate (use direct entity references)
- You don't need the mapping transformation
- The data is purely transient and doesn't need persistence

---

## Recap

The `uses dto` pattern enables clean cross-aggregate data sharing while maintaining bounded context boundaries. The mapping syntax gives you precise control over which fields are copied and how they're named locally. Combined with version tracking, this supports the causal-saga architecture's consistency requirements.

[← Back to Step 2](02-entities-and-dtos.md) · [Next → Step 4](04-repositories.md)
