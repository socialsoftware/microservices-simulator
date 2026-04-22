---
name: boot-strap
description: Bootstrap a new microservices-simulator application (Phase 0). Creates pom.xml, exception classes, BeanConfigurationSagas.groovy, and Spock test base classes from the quizzes reference app. Invoke with /boot-strap <App Name> (e.g., /boot-strap quizzes-full).
---

# Boot-Strap Phase 0 Application

This skill automates the Phase 0 bootstrap process. It creates the Maven project scaffold, exception infrastructure, and test configuration for a new application, ready for Phase 1 (plan generation) and Phase 2 (aggregate implementation).

The bootstrap establishes a minimal, working foundation: all infrastructure beans and base test classes are in place, but domain-specific code is deferred to Phase 1 planning and Phase 2 implementation.

## Input

The skill is invoked as: `/boot-strap <App Name>`

Examples:
- `/boot-strap quizzes-full`
- `/boot-strap my-app`

> **If no argument is provided**, ask the user: "What should the new application be named? (kebab-case, e.g. `my-app`)"

The `<App Name>` must be:
- Kebab-case (lowercase with hyphens)
- Will be converted to `pkg` (no hyphens, lowercase) and `AppClass` (PascalCase with each word capitalized)

## Process

### Step 1: Parse Arguments and Derive Naming Variables

Given the `<App Name>` argument:
1. `app-name` = the argument as-is (kebab case, e.g., `quizzes-full`)
2. `pkg` = remove all hyphens and convert to lowercase (e.g., `quizzesfull`)
3. `AppClass` = split on hyphens, capitalize each segment, join without separator (e.g., `QuizzesFull`)
4. `appClass` (camelCase, first segment lowercase) = first segment lowercase, rest capitalized (e.g., `quizzesFull`)

### Step 2: Read Reference Files

Read these files from the quizzes reference application to use as templates:

1. **pom.xml template:** `applications/quizzes/pom.xml`
2. **BeanConfigurationSagas template:** `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/BeanConfigurationSagas.groovy`
3. **SpockTest infrastructure template:** `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/SpockTest.groovy`
   (Note: the file is physically under `quizzes/` but its package declaration is `pt.ulisboa.tecnico.socialsoftware` — copy verbatim, output path is the parent package folder.)
4. **QuizzesSpockTest template:** `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/QuizzesSpockTest.groovy`
5. **QuizzesException template:** `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesException.java`
6. **QuizzesErrorMessage template:** `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesErrorMessage.java`

### Step 3: Create Directory Structure

Create the following directory structure (relative to repo root):

```
applications/{app-name}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
    │   │   └── microservices/exception/
    │   │       ├── {AppClass}Exception.java
    │   │       └── {AppClass}ErrorMessage.java
    │   └── resources/
    │       ├── application.yaml
    │       └── application-test.yaml
    └── test/groovy/pt/ulisboa/tecnico/socialsoftware/
        ├── SpockTest.groovy
        └── {pkg}/
            ├── BeanConfigurationSagas.groovy
            └── {AppClass}SpockTest.groovy
```

### Step 4: Produce Files

#### File 1: `applications/{app-name}/pom.xml`

**Source:** `applications/quizzes/pom.xml`

**Transformations:**
- Replace `<artifactId>QuizzesTutor</artifactId>` with `<artifactId>{AppClass}</artifactId>`
- Replace `<name>QuizzesTutor</name>` with `<name>{AppClass}</name>`
- Replace `<description>Quizzes Tutor</description>` with `<description>{AppClass}</description>`
- Replace `<start-class>pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator</start-class>` with `<start-class>pt.ulisboa.tecnico.socialsoftware.{pkg}.{AppClass}Simulator</start-class>`
- In the `test-sagas` profile, update the surefire include path from `**/quizzes/sagas/**` to `**/{pkg}/sagas/**`
- **Delete** the entire `test-tcc` profile (time-critical consistency is not in scope for Phase 0)
- **Delete** all 8 microservice deployment profiles: `answer-service`, `execution-service`, `course-service`, `question-service`, `quiz-service`, `topic-service`, `tournament-service`, `user-service`
- Keep all other profiles: `sagas`, `tcc`, `local`, `stream`, `grpc`, `distributed-version`, `kubernetes`

#### File 2: `applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/microservices/exception/{AppClass}Exception.java`

**Source:** `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesException.java`

**Transformations:**
- Package declaration: `pt.ulisboa.tecnico.socialsoftware.{pkg}.microservices.exception`
- Class name: `{AppClass}Exception`
- Field name: `{appClass}ErrorMessage` (camelCase, first segment lowercase)
- Logger class reference: `LoggerFactory.getLogger({AppClass}Exception.class)`
- Constructor parameter names: all instances of `quizzesErrorMessage` → `{appClass}ErrorMessage`
- Constructor method `getErrorMessage()` return value: same field reference
- `fromRemote()` factory return type: `{AppClass}Exception`
- All references to `QuizzesException` → `{AppClass}Exception`

#### File 3: `applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/microservices/exception/{AppClass}ErrorMessage.java`

**Source:** `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/exception/QuizzesErrorMessage.java`

**Transformations:**
- Package declaration: `pt.ulisboa.tecnico.socialsoftware.{pkg}.microservices.exception`
- Class name: `{AppClass}ErrorMessage`
- Keep all generic infrastructure constants — identify by name, not by position (domain-specific constants are interspersed throughout the file):
  - `UNDEFINED_TRANSACTIONAL_MODEL`
  - `AGGREGATE_BEING_USED_IN_OTHER_SAGA`
  - `INVALID_AGGREGATE_TYPE`
  - `AGGREGATE_DELETED`
  - `AGGREGATE_NOT_FOUND`
  - `VERSION_MANAGER_DOES_NOT_EXIST`
  - `AGGREGATE_MERGE_FAILURE`
  - `AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT`
  - `CANNOT_PERFORM_CAUSAL_READ`
  - `CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED`
  - `INVALID_PREV`
  - `NO_PRIMARY_AGGREGATE_FOUND`
  - `TOO_MANY_PRIMARY_AGGREGATE_FOUND`
  - `INVARIANT_BREAK`
  - `INVALID_EVENT_TYPE`
  - `CANNOT_MODIFY_INACTIVE_AGGREGATE`
- Delete all domain-specific error message constants (those prefixed with domain names like `TOURNAMENT_`, `COURSE_EXECUTION_`, `USER_`, `QUESTION_`, `QUIZ_`, `TOPIC_`, `ANSWER_`)
- Add exactly one placeholder constant:
  ```java
  public static final String PLACEHOLDER = "placeholder";
  ```

#### File 4: `applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/SpockTest.groovy`

**Source:** `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/SpockTest.groovy`

**Transformations:** None — copy verbatim. This file is shared infrastructure (package is `pt.ulisboa.tecnico.socialsoftware`, not app-specific).

#### File 5: `applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/BeanConfigurationSagas.groovy`

**Source:** `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/BeanConfigurationSagas.groovy`

**Transformations:**
- Package declaration: `pt.ulisboa.tecnico.socialsoftware.{pkg}`
- Keep the `@PropertySource("classpath:application-test.properties")` annotation unchanged — this loads `application-test.properties` from the `simulator` library JAR (at `simulator/src/main/resources/`), not from the app itself. Do not remove or change the path.
- Keep **only** the infrastructure bean methods (no aggregate-specific beans):
  - `aggregateIdGeneratorService()`
  - `versionService(LocalCommandGateway commandGateway)`
  - `centralizedVersionService()`
  - `eventApplicationService()`
  - `eventService()`
  - `unitOfWorkService()`
  - `ImpairmentService()` (note the capital 'I' in method name)
  - `retryRegistry()`
  - `messagingObjectMapperProvider()`
  - `localCommandService(...)`
  - `commandGateway(...)`
  - `streamBridge()`
  - `commandResponseAggregator()`
  - `streamCommandGateway(...)`
  - `TraceService()` (note the capital 'T' in method name)
  - `sagaCommandHandler()`
  - `versionCommandHandler()`
- Delete all imports and bean methods related to domain aggregates. The categories to remove are:
  - **Functionalities & event processing:** e.g. `executionFunctionalities()`, `executionEventProcessing()`, `userFunctionalities()`, `topicFunctionalities()`, `questionFunctionalities()`, `questionEventProcessing()`, `quizFunctionalities()`, `quizEventProcessing()`, `answerFunctionalities()`, `answerEventProcessing()`, `tournamentFunctionalities()`, `tournamentEventProcessing()`
  - **Custom repositories:** e.g. `courseCustomRepositorySagas()`, `courseExecutionCustomRepositorySagas()`, `tournamentCustomRepositorySagas()`, `quizAnswerCustomRepositorySagas()`
  - **Aggregate factories:** e.g. `sagasQuizAnswerFactory()`, `sagasCourseFactory()`, `sagasCourseExecutionFactory()`, `sagasQuestionFactory()`, `sagasQuizFactory()`, `sagasTopicFactory()`, `sagasTournamentFactory()`, `sagasUserFactory()`
  - **Domain services:** e.g. `courseService(...)`, `answerService(...)`, `tournamentService(...)`, `executionService(...)`, `userService(...)`, `topicService(...)`, `questionService(...)`, `quizService(...)`
  - **Event handling/detection:** e.g. `courseExecutionEventDetection()`, `questionEventDetection()`, `quizEventDetection()`, `answerEventDetection()`, `tournamentEventDetection()`
  - **Domain command handlers:** e.g. `userCommandHandler()`, `tournamentCommandHandler()`, `questionCommandHandler()`, `topicCommandHandler()`, `executionCommandHandler()`, `courseCommandHandler()`, `answerCommandHandler()`, `quizCommandHandler()` — keep only `sagaCommandHandler()` and `versionCommandHandler()`
- The resulting file should have only infrastructure imports from `pt.ulisboa.tecnico.socialsoftware.ms.*`

#### File 6: `applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/{AppClass}SpockTest.groovy`

**Source:** `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/QuizzesSpockTest.groovy`

**Transformations:**
- Package declaration: `pt.ulisboa.tecnico.socialsoftware.{pkg}`
- Class name: `{AppClass}SpockTest`
- Keep infrastructure annotations and fields:
  - `@Autowired` for `ImpairmentService impairmentService`
  - `@Autowired(required = false)` for `SagaUnitOfWorkService unitOfWorkService`
- Keep infrastructure helper methods:
  - `loadBehaviorScripts()` — unchanged
  - `sagaStateOf(Integer aggregateId)` — unchanged
- Delete all domain-specific `@Autowired` fields (ExecutionFunctionalities, UserFunctionalities, TopicFunctionalities, QuestionFunctionalities, TournamentFunctionalities)
- Delete all domain-specific constant declarations (times, aggregate IDs, names, usernames, titles, content, options)
- Delete all domain-specific helper methods (createCourseExecution, createUser, createTopic, createQuestion, createTournament)
- Delete all domain-specific imports
- Add a single line comment after the class declaration:
  ```groovy
  // Domain @Autowired fields and helper methods will be added as aggregates are implemented
  ```
- Keep only the `mavenBaseDir` constant (shared infrastructure for script loading)

#### File 7: `applications/{app-name}/src/main/resources/application.yaml`

**Source:** `applications/quizzes/src/main/resources/application.yaml`

**Transformations:**
- Replace `name: quizzes` with `name: {app-name}` (under `spring.application`)
- Remove the `spring.profiles.group` block (`stream: remote` / `grpc: remote`) — these group mappings only apply when the `remote` profile exists, which is being deleted
- Delete the entire `remote` profile section (Spring Cloud Stream / RabbitMQ bindings) — the `quizzes.function-definition.events` block and all aggregate-specific subscriber bindings reference the quizzes domain and must not be copied
- Delete the `stream` profile section (command channel bindings for all quizzes aggregates)
- Delete the `grpc` profile section
- Keep: root defaults, `local` profile, `kubernetes` profile
- Note: there is no `distributed-version` profile section in `application.yaml` — that profile only exists in `pom.xml`

#### File 8: `applications/{app-name}/src/main/resources/application-test.yaml`

**Source:** `applications/quizzes/src/main/resources/application-test.yaml`

**Transformations:** None — copy verbatim. This file configures the H2 in-memory database and disables service discovery for all tests.

---

### Step 5: Confirm Success

After creating all 8 files:

1. Report that bootstrap completed successfully
2. List the full paths of all created files
3. Confirm the structure and mention that Phase 1 (plan generation) is the next step

## Critical Transformations Summary

| Item | From | To |
|------|------|-----|
| Artifact ID | `QuizzesTutor` | `{AppClass}` |
| Package | `pt.ulisboa.tecnico.socialsoftware.quizzes` | `pt.ulisboa.tecnico.socialsoftware.{pkg}` |
| Class names | `Quizzes*` | `{AppClass}*` |
| Field names | `quizzesErrorMessage` | `{appClass}ErrorMessage` |
| Test include path | `**/quizzes/sagas/**` | `**/{pkg}/sagas/**` |
| pom.xml `<description>` | `Quizzes Tutor` | `{AppClass}` |
| pom.xml profiles removed | `test-tcc` + 8 microservice profiles | — |
| BeanConfig beans | All quizzes aggregates (6 categories) | Infrastructure only |
| SpockTest fields | Domain functionalities | Infrastructure only |
| SpockTest helpers | Domain create* methods | Infrastructure only |
| application.yaml | `name: quizzes` + remote/stream/grpc sections | `name: {app-name}`, stripped |
| application-test.yaml | — | Copied verbatim |

---

## Notes

- The skill does not create a `plan.md` — that is Phase 1's responsibility.
- The skill does not create any `{AppClass}Simulator.java` entry point (that is outside the bootstrap scope).
- All 8 files are ready for Phase 1 planning immediately after bootstrap completes.
- For quizzes-full bootstrap, the app-name is `quizzes-full`, pkg is `quizzesfull`, AppClass is `QuizzesFull`, and appClass is `quizzesFull`.
