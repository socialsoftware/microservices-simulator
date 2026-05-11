# Run Using IntelliJ

## Table of Contents

- [Technology Requirements](#technology-requirements)
- [Pre-configured Run Configurations](#pre-configured-run-configurations)
- [Running as Centralized with Local Service Calls](#running-as-centralized-with-local-service-calls)
- [Running as Centralized with Remote Service Calls](#running-as-centralized-with-remote-service-calls)
- [Running as Distributed](#running-as-distributed)
  - [Running with Distributed Version](#running-with-distributed-version)
- [Service Access & Ports](#service-access--ports)

### Technology Requirements

- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Ultimate or Community Edition)

The runtime prerequisites are the same as in [Run Using Maven](run-maven.md): database setup, Jaeger tracing, and any
broker or service-discovery infrastructure needed by the selected profile.

### Pre-configured Run Configurations

The project includes ready-to-use IntelliJ run configurations in the `.run/` directory. After opening the project in
IntelliJ, these configurations will be automatically available in the Run/Debug dropdown.

1. Open the project in IntelliJ IDEA
2. Run the `build-simulator` configuration to install the simulator library
3. Select a run configuration from the dropdown (e.g., **Quizzes**)
4. Click the **Run** button

Minimum run configurations by mode:

| Mode | Run Configurations to Start |
|------|------------------------------|
| Centralized Local | `sagas local` or `tcc local` |
| Centralized Remote (Stream/gRPC) | One from `quizzes` (`sagas-stream`, `sagas-grpc`, `tcc-stream`, `tcc-grpc`) + matching `version-service` (`version-stream` or `version-grpc`) |
| Distributed (Centralized version IDs) | One from `microservices-*` (`microservices-sagas-stream`, `microservices-sagas-grpc`, `microservices-tcc-stream`, `microservices-tcc-grpc`) + matching `version-service` + `api-gateway` |
| Distributed (Distributed version IDs, Sagas only) | `microservices-sagas-stream-distributed` or `microservices-sagas-grpc-distributed` + `api-gateway` |

### Running as Centralized with Local Service Calls

- Run the `sagas local` or the `tcc local` configuration

### Running as Centralized with Remote Service Calls

- Run the `quizzes` folder (contains `sagas-stream`, `sagas-grpc`, `tcc-stream`, `tcc-grpc`, etc. configurations)
- Run one of the `version-service` folder configurations (`version-stream` or `version-grpc`) matching the communication
  layer

### Running as Distributed

- Run one of the microservices folders to start all domain services:
    - `microservices-sagas-stream` — Sagas with RabbitMQ
    - `microservices-sagas-grpc` — Sagas with gRPC
    - `microservices-tcc-stream` — TCC with RabbitMQ
    - `microservices-tcc-grpc` — TCC with gRPC
- Run the matching `version-service` configuration (`version-stream` or `version-grpc`)
- Run the `api-gateway` configuration

#### Running with Distributed Version

Uses the `distributed-version` profile so each service generates version IDs locally via Snowflake IDs — **no
version-service needed**.

- Run one of the distributed microservices folders:
    - `microservices-sagas-stream-distributed` — Sagas with RabbitMQ (distributed version)
    - `microservices-sagas-grpc-distributed` — Sagas with gRPC (distributed version)
- Run the `api-gateway` configuration

---

### Service Access & Ports

See the **[Service URLs and Ports](../README.md#service-urls-and-ports)** section in the main README for a complete list of endpoints, including:
* Gateway and Microservice REST APIs
* Infrastructure UIs (Jaeger, RabbitMQ, Eureka)
* Default credentials for databases and message brokers
