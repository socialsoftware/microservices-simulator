# Microservices Simulator

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Running an Application](#running-an-application)
- [Configuration Reference](#configuration-reference)
  - [Jaeger Tracing](#jaeger-tracing)
  - [Service Discovery](#service-discovery)
  - [Database Configuration](#database-configuration)
  - [Spring Cloud Stream Bindings](#spring-cloud-stream-bindings)
  - [gRPC Command Gateway](#grpc-command-gateway)
  - [Distributed Version Service](#distributed-version-service)
  - [Service URLs and Ports](#service-urls-and-ports)
  - [API Gateway Configuration](#api-gateway-configuration)
- [Test Cases](#test-cases)
- [Benchmarking and Performance Tests](#benchmarking-and-performance-tests)
- [Code Structure](#code-structure)
  - [Simulator](#simulator)
  - [Quizzes Microservice System](#quizzes-microservice-system)
- [How to Implement Your Own Business Logic](#how-to-implement-your-own-business-logic)
  - [Developer Effort Checklist](#developer-effort-checklist)
  - [Implementing a Single Aggregate](#implementing-a-single-aggregate)
  - [Implementing a Single Functionality](#implementing-a-single-functionality)
- [Publications](#publications)

## Overview

Developing business-logic-rich microservices requires navigating complex trade-offs between data consistency and
distributed coordination. Although patterns like Sagas and Transactional Causal Consistency (TCC) provide mechanisms to
manage distributed state, validating their behavior before production is challenging.

The **Microservices Simulator** is a Domain-Driven Design (DDD) microservice simulator that isolates core business logic
from communication and transactional infrastructure. By modeling distributed systems around aggregates, the simulator
allows developers to evaluate identical application code under varying consistency guarantees and network constraints.
It features support for multiple transactional models (Sagas, TCC) and seamless transitions across diverse deployment
topologies, ranging from centralized execution to fully distributed environments.

In practice, this allows developers to compare Saga semantic-lock strategies against TCC snapshot-based conflict
resolution without rewriting domain services, and benchmark the same workflows across local, stream, and gRPC
deployment profiles.

This tool acts as a deterministic sandbox for the shift-left validation and optimization of microservice architectures,
minimizing developer effort while enabling robust architectural validation.

## Architecture

![Architecture](data/figs/architecture/layers/layers_apis.svg)

The system architecture is divided into three primary layers:

* **Application Layer**: Contains the concrete domain logic, specifically, the Application Functionality and Application
  Domain components. This layer is entirely decoupled from the underlying infrastructural complexities.
* **Business Layer**: Provides the core coordination and domain structuring mechanisms. It encompasses the Coordination
  Module, the Transaction Module, and the Aggregate Module.
* **Infrastructure Layer**: Manages cross-cutting technical concerns and network operations, including the Messaging
  Module, Notification Module, Impairment Module, Monitoring Module, and Versioning Module.

The simulator supports multiple execution topologies, ranging from deterministic single-process runs to fully
distributed microservice deployments.

| Topology | Process and Data Layout | Command Transport | Event Transport | Typical Profiles | Strategic Value | Core Infrastructure |
|----------|-------------------------|-------------------|-----------------|------------------|-----------------|---------------------|
| **Centralized Local** | Single application process, shared database | In-memory (local) | Internal event persistence and polling | `sagas\|tcc, local` | Deterministic baseline to debug invariants, workflow ordering, and concurrency interleavings | PostgreSQL, Jaeger |
| **Centralized Stream** | Single application process, shared database | RabbitMQ command channels | RabbitMQ `event-channel` | `sagas\|tcc, stream` | Intermediate topology to benchmark broker-based communication with shared persistence | PostgreSQL, RabbitMQ, Jaeger |
| **Centralized gRPC** | Single application process, shared database | gRPC (discovery-based resolution) | RabbitMQ `event-channel` | `sagas\|tcc, grpc` | Intermediate topology to benchmark point-to-point RPC and discovery with shared persistence | PostgreSQL, Eureka, RabbitMQ, Jaeger |
| **Distributed Stream** | Independent service processes, database-per-service | RabbitMQ command channels | RabbitMQ `event-channel` | Service profile + `sagas\|tcc, stream` (e.g., `quiz-service, sagas, stream`) | Production-like isolation with broker-mediated coordination across independent services | PostgreSQL per service, Eureka or Spring Cloud Kubernetes, API Gateway, RabbitMQ, Jaeger |
| **Distributed gRPC** | Independent service processes, database-per-service | gRPC (service-to-service via discovery) | RabbitMQ `event-channel` | Service profile + `sagas\|tcc, grpc` (e.g., `quiz-service, tcc, grpc`) | Production-like hybrid model with direct RPC commands and asynchronous event propagation | PostgreSQL per service, Eureka or Spring Cloud Kubernetes, API Gateway, RabbitMQ, Jaeger |

Versioning option across topologies: add `distributed-version` only with `sagas` to use local Snowflake ID generation.
TCC requires centralized version management because causal conflict resolution depends on a centralized, monotonically
increasing version sequence.

## Running an Application

The simulator framework acts as the foundation for microservice applications. You can implement multiple applications in
the `applications/` directory. The **Quizzes** application is provided as a complete reference implementation and case
study.

Running the simulator effectively means running an application (like Quizzes) built on top of it. The execution
framework provides extensive flexibility depending on your goals, whether debugging domain logic locally or testing
distributed resilience on Kubernetes.

| Execution Environment        | Best For                                                                                                | Documentation                                       |
|------------------------------|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| **Docker Compose**           | Local testing, switching between centralized/distributed topologies quickly without local dependencies. | [Run Using Docker](guides/run-docker.md)            |
| **Maven**                    | Development, running individual microservices, and load testing with JMeter.                            | [Run Using Maven](guides/run-maven.md)              |
| **IntelliJ IDEA**            | Debugging and stepping through execution flows using pre-configured run profiles.                       | [Run Using IntelliJ](guides/run-intellij.md)        |
| **Kubernetes (Local/Cloud)** | Testing production-grade orchestration (Kind) or cloud latency (Azure AKS).                             | [Deploy to Kubernetes](guides/deploy-kubernetes.md) |

## Configuration Reference

The application uses Spring Boot profiles and YAML configuration files to manage different deployment modes.

### Jaeger Tracing

The project uses [Jaeger](https://www.jaegertracing.io/) for distributed tracing to monitor and visualize the flow of
requests across microservices.

* **Dashboard**: Access the Jaeger UI at [http://localhost:16686](http://localhost:16686).
* **Collector**: The application sends traces to the Jaeger collector on `http://localhost:4317` using the OTLP gRPC
  protocol.
* **Instrumentation**: Custom instrumentation is implemented in `TraceManager` using the OpenTelemetry SDK to trace
  functionalities and their steps.
* **Cross-service correlation**: Trace IDs are propagated through workflow execution, command dispatching, and event
  processing, allowing end-to-end inspection of one functionality across microservices.

### Service Discovery

In distributed mode, local deployments use Eureka for service discovery. The gateway and each microservice register with
the Eureka server at `http://${EUREKA_HOST:localhost}:8761/eureka/`. When deploying on Kubernetes, the `kubernetes`
profile enables Spring Cloud Kubernetes discovery instead of Eureka.

### Database Configuration

Database settings are defined in [application.yaml](applications/quizzes/src/main/resources/application.yaml):

| Profile     | Database        | Description                                                        |
|-------------|-----------------|--------------------------------------------------------------------|
| Centralized | `msdb`          | Single database for all aggregates                                 |
| Distributed | Per-service DBs | Each service has its own database (e.g., `tournamentdb`, `userdb`) |

Service-specific database URLs are configured in profile files
like [application-tournament-service.yaml](applications/quizzes/src/main/resources/application-tournament-service.yaml).

### Spring Cloud Stream Bindings

When running with the `stream` profile, inter-service communication uses RabbitMQ. Bindings are configured
in [application.yaml](applications/quizzes/src/main/resources/application.yaml):

| Binding Type      | Example                                | Purpose                         |
|-------------------|----------------------------------------|---------------------------------|
| Command Channels  | `tournament-command-channel`           | Send commands to services       |
| Command Consumers | `tournamentServiceCommandChannel-in-0` | Receive and process commands    |
| Event Channel     | `event-channel`                        | Broadcast events to subscribers |
| Event Subscribers | `tournamentEventSubscriber-in-0`       | Receive events for processing   |
| Response Channel  | `commandResponseChannel-in-0`          | Receive command responses       |

Service-specific bindings override only the channels relevant to that service, as shown
in [application-tournament-service.yaml](applications/quizzes/src/main/resources/application-tournament-service.yaml).

Operational intent of channel types:

* **Command channels** are directional, aggregate-targeted paths for explicit downstream-to-upstream requests.
* **Event channel** is a shared broadcast path for upstream-to-downstream propagation and eventual consistency.
* **Response channel** supports correlated replies for synchronous behavior over asynchronous transports.

### gRPC Command Gateway

Alternative remote transport is available with the `grpc` profile. Each service exposes a gRPC endpoint for
commands (see `GrpcServerRunner`), and callers use `GrpcCommandGateway` with Eureka-based discovery. Default and
service-specific gRPC ports are configured in the `application-*-service.yaml` files (and exposed via Eureka metadata
key `grpcPort`). Override the default client port with `grpc.command.default-port` or per-service with
`grpc.command.<service>.port` when needed.

### Distributed Version Service

When running in distributed mode with the `distributed-version` profile active, each microservice generates version IDs
locally using
a [Snowflake ID](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/versioning/SnowflakeIdGenerator.java)
generator, removing the need for a centralized version-service. This profile can also be used in centralized mode with
any communication profile (`local`, `stream`, or `grpc`). The 64-bit IDs are composed of a 41-bit timestamp, a 10-bit
machine ID (derived from `spring.application.name`), and a 12-bit sequence number, guaranteeing globally unique,
monotonically increasing versions across services.

This option is only supported with the **sagas** transactional model (TCC requires centralized version management).
In TCC, centralized ordering is part of causal conflict detection and merge correctness, so decentralized Snowflake
generation is intentionally disabled.

| Profile               | Version Source               | Requires version-service? |
|-----------------------|------------------------------|---------------------------|
| *(default)*           | Centralized `VersionService` | Yes                       |
| `distributed-version` | Local `SnowflakeIdGenerator` | No                        |

### Service URLs and Ports

Each microservice runs on a dedicated port:

| Service            | Port | Profile File                                                                                                       |
|--------------------|------|--------------------------------------------------------------------------------------------------------------------|
| Gateway            | 8080 | [application-gateway.yaml](simulator/src/main/resources/application-gateway.yaml)                                  |
| Version Service    | 8081 | [application-version-service.yaml](simulator/src/main/resources/application-version-service.yaml)                  |
| Answer Service     | 8082 | [application-answer-service.yaml](applications/quizzes/src/main/resources/application-answer-service.yaml)         |
| Course Execution   | 8083 | [application-execution-service.yaml](applications/quizzes/src/main/resources/application-execution-service.yaml)   |
| Question Service   | 8084 | [application-question-service.yaml](applications/quizzes/src/main/resources/application-question-service.yaml)     |
| Quiz Service       | 8085 | [application-quiz-service.yaml](applications/quizzes/src/main/resources/application-quiz-service.yaml)             |
| Topic Service      | 8086 | [application-topic-service.yaml](applications/quizzes/src/main/resources/application-topic-service.yaml)           |
| Tournament Service | 8087 | [application-tournament-service.yaml](applications/quizzes/src/main/resources/application-tournament-service.yaml) |
| User Service       | 8088 | [application-user-service.yaml](applications/quizzes/src/main/resources/application-user-service.yaml)             |

Every service port can be changed, including `version-service` port 8081, and `gateway` port 8080. Service Discovery
will map the service name to the service port automatically.

### API Gateway Configuration

The [Gateway application-gateway.yaml](simulator/src/main/resources/application-gateway.yaml) configures:

1. **Service discovery**: Eureka discovery for local distributed deployments; Kubernetes discovery is enabled via the
   `kubernetes` profile.
2. **Route definitions**: The API Gateway is a Spring MVC-based application that dynamically proxies HTTP requests to
   backend services. Routes are configured via `gateway.routes.imports` referencing the target microservice application
   properties, which the `DynamicMVCProxyController` uses to forward REST calls.
3. **Version service URL**: The Admin controller endpoints directly interact with the remote microservices for
   configuration sync.

## Test Cases

How these tests map to simulator capabilities:

* **Functionality orchestration and ordering**: validate workflow step dependencies and execution order.
* **Concurrency interleavings**: force race scenarios with controlled step progression (for example,
  `executeUntilStep(...)` in coordination tests).
* **Fault and recovery behavior**: inject failures and verify compensation and abort paths.
* **Model comparison**: run analogous Sagas and TCC tests to compare lock-based and merge-based conflict handling.

Sagas-focused test suites:

- [Workflow Test Plan (Simulator)](simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/workflow/PlanOrderTest.groovy)
- [Circuit Breaker Tests (Simulator)](simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/workflow/CircuitBreakerTest.groovy)
- [Tournament Functionality Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/)
- [Tournament Async Coordination Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/tournament/AddParticipantAsyncTest.groovy)
- [Fault and Recovery Behavior Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/)

TCC-focused test suites:

- [Tournament Merge Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/aggregates/TournamentMergeUnitTest.groovy)
- [Tournament Functionality Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)

For deterministic concurrency scenarios and paper-aligned walkthroughs, see
[Reproducing DAIS2023 Paper Tests](guides/reproduce-dais2023.md).

## Benchmarking and Performance Tests

For repeated deployment benchmarking of tournament scenarios, use:

```bash
./scripts/benchmark-deployments.sh --test concurrentAddparticipant.jmx
```

Run with a specific transaction mode and number of repetitions:

```bash
./scripts/benchmark-deployments.sh --test concurrentAddparticipant.jmx --tx-mode sagas --repetitions 5
./scripts/benchmark-deployments.sh --test 5a-updateStudentName-addParticipant-processUpdateNameEvent.jmx --tx-mode tcc --repetitions 5
```

What this script does:

* Boots each configured topology (`centralized-*`, `distributed-*`, and `distributed-version` variants).
* Runs the selected `.jmx` test for each deployment, across the configured repetitions.
* Writes per-run and aggregate results under [jmeter-results](jmeter-results) in a timestamped benchmark folder.
* Returns non-zero on benchmark failures, so it can be used in CI/regression workflows.

Requirements and setup details are in [Run Using Maven](guides/run-maven.md#running-jmeter-tests). DAIS paper
scenario mapping is in [Reproducing DAIS2023 Paper Tests](guides/reproduce-dais2023.md).

## Code Structure

### Simulator

| Module | Purpose | Main Code |
|--------|---------|-----------|
| Aggregate | Aggregate identity, invariants, and event subscription contracts | [simulator aggregate package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate) |
| Coordination | Workflow orchestration, step dependency ordering, and execution plans | [simulator coordination package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination) |
| Transaction (Sagas) | Semantic locks, compensation, and Saga-specific command wrappers | [simulator sagas package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas) |
| Transaction (TCC) | Causal snapshots, optimistic conflict detection, and merge commit path | [simulator causal package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/causal) |
| Notification | Event persistence, publication/subscription transport, and polling support | [simulator notification package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/notification) |
| Messaging | Command gateway abstraction across local, stream, and gRPC modes | [simulator messaging package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging) |
| Impairment | Fault and delay injection hooks for resilience and behavior testing | [simulator impairment package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment) |
| Monitoring | Tracing and observability support for functionalities and workflow steps | [simulator monitoring package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring) |
| Versioning | Version ID generation and version-service support across topologies | [simulator versioning package](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/versioning) |

### Quizzes Microservice System

* A case study for [Quizzes Tutor](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes)
    * The transactional model
      independent [Microservices](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices)
    * The Sagas implementation for Aggregates and Coordination
    * The TCC implementation for Aggregates and Coordination
* The tests of the [Quizzes Tutor](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes) for
  Sagas and TCC

![Application Decomposition](data/figs/application_decomposition.svg)

The API Gateway is used when running the quizzes application as microservices to route API requests to the appropriate
microservice. The gateway operates as an MVC application using a custom dynamic proxy controller to forward REST
requests.

## How to Implement Your Own Business Logic

The framework significantly minimizes the cognitive load for developers by abstracting distributed infrastructure. The
workflow focuses strictly on domain modeling, defining events, and orchestrating business logic.

### Developer Effort Checklist

This checklist summarizes recurring effort for simulator integration in a new application.

1. Define one command contract per aggregate service method that must be remotely invoked.
2. Create and tune shared and service-specific YAML profiles (`application.yaml` and `application-<service>.yaml`).
3. Ensure aggregate serialization boundaries are explicit (for example, avoid leaking nested entities through JSON
  getters).
4. Implement one command handler per microservice to route commands to domain services.
5. Wire event subscriptions and handlers for cross-aggregate eventual consistency where needed.
6. Validate deterministic functionality behavior before running distributed benchmarks.

### Implementing a Single Aggregate

| Development Task                    | Implementation Details & Example                                                                                                                                                                                                                                                                                                                                                                                         | Rationale (Why)                                                                                    |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| **Define Spring Boot Application**  | Create the microservice entry point, e.g., [`TournamentServiceApplication.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/TournamentServiceApplication.java) with `@SpringBootApplication`.                                                                                                                                                                 | Establishes the bounded context runtime and independent deployability.                             |
| **Define Aggregate**                | Define the JPA root entity, e.g., [`Tournament.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java), and associated value objects, e.g., [`TournamentCreator`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/TournamentCreator.java).                               | Defines the transactional consistency boundary where invariants are enforced.                      |
| **Define DTOs and Repositories**    | Create data transfer objects and Spring Data JPA interfaces for data access, e.g., [`TournamentDto.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/TournamentDto.java), [`TournamentRepository.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/TournamentRepository.java). | Separates persistence/API contracts from domain behavior and supports query/update paths.          |
| **Specify Invariants**              | Override the [`verifyInvariants()`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java) method, e.g., asserting tournament start date is before end date.                                                                                                                                                                                   | Prevents invalid aggregate versions from being committed.                                          |
| **Define Events**                   | Define the events published/subscribed, e.g., [`UpdateStudentNameEvent.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/events/UpdateStudentNameEvent.java).                                                                                                                                                                                                                          | Makes upstream changes observable by downstream aggregates for eventual consistency.               |
| **Subscribe Events**                | Override the [`getEventSubscriptions()`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java) method, adding concrete subscriptions.                                                                                                                                                                                                         | Declares upstream-downstream dependencies explicitly at the domain level.                          |
| **Define Event Subscriptions**      | Define subscription conditions, e.g., in [`TournamentSubscribesUpdateStudentName.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/notification/subscribe/TournamentSubscribesUpdateStudentName.java) a tournament subscribes to creator/participant name updates.                                                                                            | Filters only relevant upstream events, avoiding unnecessary or inconsistent updates.               |
| **Define Event Handlers**           | Delegate handling to processing functionalities, e.g., [`UpdateStudentNameEventHandler.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/notification/handling/handlers/UpdateStudentNameEventHandler.java).                                                                                                                                                  | Converts raw event intake into deterministic domain actions.                                       |
| **Define Aggregate Services**       | Define the microservice API to register changes, e.g., [`updateUserName(...)`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/service/TournamentService.java).                                                                                                                                                                                                    | Provides stable operation-level contracts used by commands and controllers.                        |
| **Define Web Controllers**          | Expose REST API endpoints to external clients, e.g., [`TournamentController.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/webapi/TournamentController.java).                                                                                                                                                                                 | Enables external access while preserving application/domain layering.                              |
| **Define Event Handling**           | Define polling logic for the event table, e.g., [`TournamentEventHandling.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/notification/handling/TournamentEventHandling.java).                                                                                                                                                                              | Drives periodic event processing cycles for eventual consistency.                                  |
| **Define Event Subscriber Service** | Subscribe to Spring Cloud Stream events, e.g., [`EventSubscriberService.java`](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/notification/EventSubscriberService.java).                                                                                                                                                                                                                                   | Bridges broker transport to local event persistence/processing.                                    |
| **Define Transactional Aggregates** | Extend aggregate for specific models, e.g., [`SagaTournament.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/sagas/SagaTournament.java) (locks) and [`CausalTournament.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/causal/CausalTournament.java) (merging).            | Adapts the same domain to model-specific consistency semantics without duplicating business logic. |
| **Define Commands**                 | Define remote commands for aggregate services, e.g., [`AddParticipantCommand.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/commands/tournament/AddParticipantCommand.java).                                                                                                                                                                                                        | Formalizes inter-service invocation contracts independent of transport protocol.                   |
| **Create CommandHandler**           | Receive remote commands and map to services, e.g., [`TournamentCommandHandler.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/messaging/TournamentCommandHandler.java).                                                                                                                                                                                     | Centralizes command routing and isolates transport concerns from domain services.                  |
| **Configure Network Bindings**      | Set `stream` channels or `grpc` ports in [`application-tournament-service.yaml`](applications/quizzes/src/main/resources/application-tournament-service.yaml).                                                                                                                                                                                                                                                           | Activates a deployment topology without changing business code.                                    |
| **Configure API Gateway Routes**    | Define route mappings in the microservice yaml to route HTTP requests.                                                                                                                                                                                                                                                                                                                                                   | Decouples external API paths from internal service locations.                                      |

### Implementing a Single Functionality

| Development Task           | Implementation Details & Example                                                                                                                                                                                                                                                                                                                                                           | Rationale (Why)                                                                                    |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| **Define Functionality**   | Extend [`WorkflowFunctionality`](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java) to coordinate a specific use-case, e.g., [`AddParticipantFunctionalitySagas.java`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java). | Encapsulates one business use case as a reusable coordination unit.                                |
| **Workflow Orchestration** | Map execution [`Step`](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Step.java)s, dependencies, and transaction triggers within [`buildWorkflow()`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java), e.g., defining `getUserStep` and `addParticipantStep` dependencies.                                                              | Makes ordering, dependency, and rollback/compensation boundaries explicit.                         |
| **Command Dispatching**    | Instantiate remote [`Command`](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/Command.java)s and dispatch via the abstract [`CommandGateway`](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/CommandGateway.java), e.g., sending [`AddParticipantCommand`](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/commands/tournament/AddParticipantCommand.java) wrapped in a [`SagaCommand`](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/messaging/SagaCommand.java) with semantic locks.                                                                                                                          | Executes distributed steps through transport-agnostic contracts while preserving domain isolation. |

## Publications

* **DAIS 2023**: D. Pereira and A. R.
  Silva, "[Transactional Causal Consistent Microservices Simulator](https://doi.org/10.1007/978-3-031-35260-7_4)," in
  *Distributed Applications and Interoperable Systems (DAIS)*, 2023.

## License

[MIT License](LICENSE)