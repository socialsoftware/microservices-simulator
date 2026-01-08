# Step 8 · Saga Workflows

[📚 Guide Index](00-index.md)

> **Goal:** understand how the causal-saga architecture coordinates distributed transactions across microservices.

[← Back to Step 7](07-events.md) · [Next → Step 9](09-architecture-options.md)

---

## 8.1 What Are Sagas?

Sagas are a pattern for managing distributed transactions across microservices. Instead of a single ACID transaction, a saga consists of:

1. **Steps**: Individual operations that can succeed or fail
2. **Compensations**: Rollback actions if a step fails
3. **Coordination**: Orchestration logic that manages the flow

Nebula's causal-saga architecture generates this infrastructure automatically when you use `@GenerateCrud` with the `causal-saga` architecture option.

---

## 8.2 Generated Saga Structure

When you define:

```nebula
Aggregate Quiz {
    Root Entity Quiz {
        String title;
        QuizExecution execution;
    }

    WebAPIEndpoints {
        @GenerateCrud;
    }

    Service QuizService {
        @GenerateCrud;
    }
}
```

The generator creates:

```
sagas/
└── coordination/
    └── quiz/
        ├── CreateQuizFunctionalitySagas.java
        ├── UpdateQuizFunctionalitySagas.java
        ├── DeleteQuizFunctionalitySagas.java
        └── GetQuizByIdFunctionalitySagas.java
```

---

## 8.3 Saga Functionality Classes

Each operation gets a saga class that defines the workflow:

```java
public class CreateQuizFunctionalitySagas extends GenericSagaFunctionality<QuizDto> {
    private final QuizService quizService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final SagaUnitOfWork unitOfWork;
    private final QuizDto quizDto;

    private QuizDto result;

    public CreateQuizFunctionalitySagas(
            QuizService quizService,
            SagaUnitOfWorkService sagaUnitOfWorkService,
            SagaUnitOfWork unitOfWork,
            QuizDto quizDto) {
        this.quizService = quizService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.unitOfWork = unitOfWork;
        this.quizDto = quizDto;
    }

    @Override
    public void buildWorkflow() {
        SagaSyncStep createStep = new SagaSyncStep("createQuiz", () -> {
            this.result = quizService.createQuiz(quizDto, unitOfWork);
            sagaUnitOfWorkService.registerSagaState(
                result.getAggregateId(), SagaState.IN_SAGA, unitOfWork
            );
        });

        createStep.registerCompensation(() -> {
            sagaUnitOfWorkService.compensate(unitOfWork);
        }, unitOfWork);

        workflow.addStep(createStep);
    }

    @Override
    public QuizDto getResult() {
        return result;
    }
}
```

---

## 8.4 Saga States

Aggregates track their saga involvement:

| State | Meaning |
|-------|---------|
| `NOT_IN_SAGA` | No active distributed transaction |
| `IN_SAGA` | Currently participating in a saga |
| `COMPENSATING` | Rolling back due to failure |

```java
// Mark aggregate as participating in saga
sagaUnitOfWorkService.registerSagaState(aggregateId, SagaState.IN_SAGA, unitOfWork);

// Saga completed successfully
sagaUnitOfWorkService.registerSagaState(aggregateId, SagaState.NOT_IN_SAGA, unitOfWork);
```

---

## 8.5 Multi-Step Sagas

Complex operations span multiple services:

```java
public class EnrollUserInCourseFunctionalitySagas extends GenericSagaFunctionality<EnrollmentDto> {
    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    private UserDto userDto;
    private CourseDto courseDto;
    private EnrollmentDto result;

    @Override
    public void buildWorkflow() {
        // Step 1: Validate user exists
        SagaSyncStep validateUserStep = new SagaSyncStep("validateUser", () -> {
            userDto = userService.getUserById(userId, unitOfWork);
            if (!userDto.getActive()) {
                throw new BusinessException(USER_NOT_ACTIVE);
            }
        });

        // Step 2: Validate course exists
        SagaSyncStep validateCourseStep = new SagaSyncStep("validateCourse", () -> {
            courseDto = courseService.getCourseById(courseId, unitOfWork);
        });

        // Step 3: Create enrollment (depends on steps 1 and 2)
        SagaSyncStep createEnrollmentStep = new SagaSyncStep("createEnrollment", () -> {
            result = enrollmentService.createEnrollment(userDto, courseDto, unitOfWork);
        }, new ArrayList<>(Arrays.asList(validateUserStep, validateCourseStep)));

        // Register compensation for step 3
        createEnrollmentStep.registerCompensation(() -> {
            enrollmentService.deleteEnrollment(result.getAggregateId(), unitOfWork);
        }, unitOfWork);

        workflow.addStep(validateUserStep);
        workflow.addStep(validateCourseStep);
        workflow.addStep(createEnrollmentStep);
    }
}
```

---

## 8.6 Step Dependencies

Steps can declare dependencies for execution ordering:

```java
// Independent steps (can run in parallel)
SagaSyncStep stepA = new SagaSyncStep("stepA", () -> { ... });
SagaSyncStep stepB = new SagaSyncStep("stepB", () -> { ... });

// Dependent step (waits for A and B)
SagaSyncStep stepC = new SagaSyncStep("stepC", () -> { ... },
    new ArrayList<>(Arrays.asList(stepA, stepB)));
```

The saga executor handles the ordering automatically.

---

## 8.7 Compensation Logic

When a step fails, compensations run in reverse order:

```java
createStep.registerCompensation(() -> {
    // Undo the creation
    sagaUnitOfWorkService.compensate(unitOfWork);
}, unitOfWork);

updateStep.registerCompensation(() -> {
    // Restore previous state
    quizService.updateQuiz(previousQuizDto, unitOfWork);
}, unitOfWork);
```

Compensation best practices:
- Keep compensations idempotent
- Log compensation actions for debugging
- Handle partial failures gracefully

---

## 8.8 Cross-Aggregate Coordination

When operations span aggregates, the saga coordinates:

```nebula
Aggregate Execution {
    Entity ExecutionCourse uses dto CourseDto mapping {
        aggregateId -> courseAggregateId;
    } {
        Integer courseAggregateId;
    }

    Root Entity Execution {
        ExecutionCourse course;
    }
}
```

The generated create saga:
1. Validates the referenced Course exists
2. Creates the Execution with embedded course data
3. Registers saga state for both aggregates
4. Commits or compensates as a unit

---

## 8.9 Saga Execution Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Saga Workflow                        │
├─────────────────────────────────────────────────────────┤
│  1. Create UnitOfWork                                   │
│  2. Build workflow (define steps)                       │
│  3. Execute steps in dependency order                   │
│  4. If success: commit all changes                      │
│  5. If failure: run compensations in reverse            │
└─────────────────────────────────────────────────────────┘

    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │  Step 1  │───>│  Step 2  │───>│  Step 3  │
    └──────────┘    └──────────┘    └──────────┘
         │               │               │
         │    ┌──────────┴──────────┐    │
         │    │   If Step 3 fails   │    │
         │    └──────────┬──────────┘    │
         ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Comp. 1  │<───│ Comp. 2  │<───│ Comp. 3  │
    └──────────┘    └──────────┘    └──────────┘
```

---

## 8.10 Custom Saga Workflows

For complex business processes, define explicit workflows:

```nebula
Aggregate Answer {
    Functionalities {
        functionality submitAnswer(Integer questionId, String answer) {
            step validateQuestion {
                call questionService.getQuestionById(questionId) -> questionDto;
            }
            
            step createAnswer depends [validateQuestion] {
                call answerService.createAnswer(questionDto, answer) -> answerDto;
                registerSagaState(answerDto.aggregateId, IN_SAGA);
            } compensation {
                call answerService.deleteAnswer(answerDto.aggregateId);
                registerSagaState(answerDto.aggregateId, NOT_IN_SAGA);
            }
            
            step updateStatistics depends [createAnswer] {
                call statisticsService.recordAnswer(answerDto);
            }
            
            return answerDto;
        }
    }
}
```

---

## Recap

The causal-saga architecture provides distributed transaction support through:
- **Saga functionality classes** that define workflows
- **Steps** with dependencies for execution ordering
- **Compensations** for rollback on failure
- **Saga states** for tracking aggregate involvement

Nebula generates this infrastructure automatically for CRUD operations and lets you define custom workflows for complex business processes.

[← Back to Step 7](07-events.md) · [Next → Step 9](09-architecture-options.md)
