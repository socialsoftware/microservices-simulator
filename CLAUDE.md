# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Run Quizzes Tests
```bash
cd applications/quizzes

# Sagas tests
mvn clean -Ptest-sagas test

# TCC tests
mvn clean -Ptest-tcc test
```

### Run a Single Test Class
```bash
cd applications/quizzes
mvn clean -Ptest-sagas test -Dtest=AddParticipantAndUpdateStudentNameTest
```

## Architecture Overview

### Two-Module Structure

The project is split into two Maven modules:

1. **`simulator/`** — Core library providing transactional model abstractions. Published to local Maven repo via `mvn install`. Contains:
   - `ms.domain` — Base DDD aggregate (`Aggregate`), events (`Event`, `EventSubscription`, `EventHandler`), and versioning
   - `ms.coordination` — `WorkflowFunctionality`, `Workflow`, `Step`, `UnitOfWork`, `UnitOfWorkService`, and `CommandGateway`
   - `ms.sagas` — Saga-specific: `SagaAggregate` (semantic locks via `SagaState`), `SagaWorkflow`, `SagaUnitOfWork`, `SagaUnitOfWorkService`
   - `ms.causal` — TCC-specific: `CausalAggregate`, `CausalWorkflow`, `CausalUnitOfWork`, `CausalUnitOfWorkService`

2. **`applications/quizzes/`** — A case study (Quizzes Tutor) demonstrating how to build on top of the simulator. Contains:
   - `microservices/` — Domain aggregates (Course, CourseExecution, User, Topic, Question, Quiz, QuizAnswer, Tournament), each with:
     - Base aggregate + DTO
     - `sagas/` subfolder: `SagaXxx` class, saga states, saga factory, saga repository
     - `causal/` subfolder: `CausalXxx` class, TCC factory, TCC repository
     - `coordination/` subfolder: functionalities, event processing, web API controllers
     - `events/` subfolder: event handling (polling), event subscriptions, event handlers
     - `commandHandler/` subfolder: local, stream, and gRPC command handlers
   - `events/` — Shared event classes (e.g., `UpdateStudentNameEvent`, `AnonymizeStudentEvent`)
   - `command/` — Command objects sent through the `CommandGateway`

### Core Concepts

**Aggregate versioning**: Every `Aggregate` has a `version` (global, from the version service), an `aggregateId` (logical ID), and a JPA `id` (physical row). Each write creates a new row; reads fetch the latest version. The `prev` pointer chains versions.

**Unit of Work**: Coordinates reads and writes within a functionality execution. For Sagas: `SagaUnitOfWork`; for TCC: `CausalUnitOfWork`. Created at the start of each functionality, committed or aborted at the end.

**Functionalities**: Each cross-service operation is a `WorkflowFunctionality` subclass (e.g., `AddParticipantFunctionalitySagas`). It builds a `Workflow` from `Step`s with dependency declarations. Key methods:
- `executeWorkflow(uow)` — run all steps
- `executeUntilStep(stepName, uow)` — run up to (inclusive) a named step (used in tests to simulate interleaving)
- `resumeWorkflow(uow)` — continue from where it stopped

**Command Gateway**: Routes commands to the correct service handler. Three implementations selected by Spring profile: `LocalCommandGateway` (in-process), `StreamCommandGateway` (RabbitMQ), `GrpcCommandGateway`.

**Events**: Aggregates publish domain events on commit. Downstream aggregates poll for events via `XxxEventHandling` beans (scheduled) and process them via `XxxEventProcessing`. Event subscriptions are declared per-aggregate via `getEventSubscriptions()`.

**Sagas semantic locks**: `SagaAggregate` adds a `SagaState`. Steps can declare forbidden states via `Command.setForbiddenStates(...)` to prevent conflicting concurrent operations.

### Test Patterns

Tests use Spock (`@DataJpaTest`) with an in-memory H2 database. The test configuration is in `BeanConfigurationSagas` or `BeanConfigurationCausal` (registered as `@TestConfiguration` via a static inner class in each test).

- `QuizzesSpockTest` — base class with setup helpers (`createCourseExecution`, `createUser`, `createTopic`, etc.) and shared constants
- Concurrent interleaving tests use `executeUntilStep` + `resumeWorkflow` to control step-by-step execution between two concurrent functionalities
- Event processing is triggered manually in tests by calling `XxxEventHandling.handleXxxEvent()`


### Key File Locations

| Concern | Path |
|---------|------|
| Base aggregate | `simulator/.../ms/domain/aggregate/Aggregate.java` |
| Saga aggregate | `simulator/.../ms/sagas/aggregate/SagaAggregate.java` |
| Workflow base | `simulator/.../ms/coordination/workflow/WorkflowFunctionality.java` |
| Tournament saga example | `applications/.../tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` |
| Test bean config (Sagas) | `applications/.../quizzes/BeanConfigurationSagas.groovy` |
| Base Spock test class | `applications/.../quizzes/QuizzesSpockTest.groovy` |
| App config | `applications/quizzes/src/main/resources/application.yaml` |
