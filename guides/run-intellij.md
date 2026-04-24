# Run Using IntelliJ

### Technology Requirements

- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Ultimate or Community Edition)

### Pre-configured Run Configurations

The project includes ready-to-use IntelliJ run configurations in the `.run/` directory. After opening the project in
IntelliJ, these configurations will be automatically available in the Run/Debug dropdown.

1. Open the project in IntelliJ IDEA
2. Run the `build-simulator` configuration to install the simulator library
3. Select a run configuration from the dropdown (e.g., **Quizzes**)
4. Click the **Run** button

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