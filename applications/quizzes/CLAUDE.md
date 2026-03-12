# applications/quizzes/ — Quizzes Application

A case study built on the simulator demonstrating all patterns: aggregates, events, sagas, TCC, functionalities, and inter-invariants.

---

## Existing Aggregates

| Aggregate | Service class | Package |
|-----------|-------------|---------|
| Course | `CourseService` | `microservices/course/` |
| CourseExecution (Execution) | `ExecutionService` | `microservices/execution/` |
| User | `UserService` | `microservices/user/` |
| Topic | `TopicService` | `microservices/topic/` |
| Question | `QuestionService` | `microservices/question/` |
| Quiz | `QuizService` | `microservices/quiz/` |
| QuizAnswer | `QuizAnswerService` | `microservices/answer/` |
| Tournament | `TournamentService` | `microservices/tournament/` |

---

## Shared Class Locations

| Concern | Path |
|---------|------|
| Domain events | `src/main/java/.../quizzes/events/` |
| Commands | `src/main/java/.../quizzes/command/` |
| Error messages | `src/main/java/.../quizzes/microservices/exception/QuizzesErrorMessage.java` |
| Exception class | `src/main/java/.../quizzes/microservices/exception/QuizzesException.java` |
| Service routing | `src/main/java/.../quizzes/ServiceMapping.java` |

---

## Test Infrastructure

| File | Purpose |
|------|---------|
| `src/test/groovy/.../quizzes/QuizzesSpockTest.groovy` | Base Spock test class; `createCourseExecution()`, `createUser()`, `createTopic()`, etc. |
| `src/test/groovy/.../quizzes/BeanConfigurationSagas.groovy` | `@TestConfiguration` for sagas profile tests |
| `src/test/groovy/.../quizzes/BeanConfigurationCausal.groovy` | `@TestConfiguration` for TCC profile tests |

Tests use `@DataJpaTest` with in-memory H2. Register as a static inner `@TestConfiguration` class in each test.

---

## Reference Implementations

Use these as templates when implementing new aggregates or functionalities:

| Pattern | Use this as template |
|---------|---------------------|
| Aggregate with inter-invariants | `microservices/execution/` |
| Multi-step saga with forbidden states | `microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` |
| TCC merge with mutable fields | `microservices/execution/aggregate/causal/CausalExecution.java` |
| Event polling + EventProcessing chain | `microservices/execution/events/` |
| Inter-invariant end-to-end | [`docs/examples/cannot-delete-last-execution-with-content.md`](../../docs/examples/cannot-delete-last-execution-with-content.md) |

---

## Build Commands

```bash
cd applications/quizzes

# All sagas tests
mvn clean -Ptest-sagas test

# All TCC tests
mvn clean -Ptest-tcc test

# Single test class
mvn clean -Ptest-sagas test -Dtest=AddParticipantAndUpdateStudentNameTest
```

---

## Adding New Aggregates or Features

- New aggregate: [`docs/guides/implement-aggregate.md`](../../docs/guides/implement-aggregate.md) or `/new-aggregate`
- New event: [`docs/guides/implement-event.md`](../../docs/guides/implement-event.md) or `/new-event`
- New functionality: [`docs/guides/implement-functionality.md`](../../docs/guides/implement-functionality.md) or `/new-functionality`
- New inter-invariant: [`docs/concepts/inter-invariants.md`](../../docs/concepts/inter-invariants.md) or `/inter-invariant`
