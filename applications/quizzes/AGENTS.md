# applications/quizzes/ — Quizzes Application

A case study built on the simulator demonstrating all patterns: aggregates, events, sagas, TCC, functionalities, and inter-invariants.

---

## Domain Model

| File | Purpose |
|------|---------|
| [`quizzes-domain-model.md`](quizzes-domain-model.md) | Entities, relationships, and consistency rules (aggregate-independent) |
| [`quizzes-aggregate-grouping.md`](quizzes-aggregate-grouping.md) | Aggregate partitioning decision, snapshots, and event dependencies |

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

For **manual/human reference** — e.g. a developer reading code to understand a pattern before writing docs or reviewing a skill — these are useful templates:

| Pattern | Use this as template |
|---------|---------------------|
| Aggregate with inter-invariants | `microservices/execution/` |
| Multi-step saga with forbidden states | `microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` |
| TCC merge with mutable fields | `microservices/execution/aggregate/causal/CausalExecution.java` |
| Event polling + EventProcessing chain | `microservices/execution/events/` |

> **The automated implementation harness must NOT read files under `applications/quizzes/` during aggregate implementation.** See `implement-aggregate/SKILL.md` §"Anti-Pattern: Do Not Consult the Reference App" — consulting this app during automated implementation risks reproducing its bugs. The table above is for human reference only.
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


