# Run Using Docker

### Technology Requirements

- [Docker Compose V2](https://docs.docker.com/compose/install/)

### Docker Compose Structure

The project uses a two-layer Docker Compose configuration:

1. **Root `docker-compose.yml`**: Defines shared infrastructure components (PostgreSQL, RabbitMQ, Jaeger, Eureka) and
   simulator services (gateway, version-service, tests).
2. **Application `docker-compose.yml`** (in `applications/quizzes`): Extends the root configuration with quiz-specific
   services and test environments.

To run the full system, always execute Docker Compose commands from the `applications/quizzes` directory.

```bash
cd applications/quizzes
```

### Running as Centralized with Local Service Calls

![Centralized Local Topology](../data/figs/architecture/topology_local.svg)

```bash
# Sagas (default)
docker compose up quizzes-local -d

# TCC
TX_MODE=tcc docker compose up quizzes-local -d
```

### Running as Centralized with Remote Service Calls

![Centralized Stream Topology](../data/figs/architecture/topology_stream.svg)
*Centralized Stream Topology*

![Centralized gRPC Topology](../data/figs/architecture/topology_grpc.svg)
*Centralized gRPC Topology*

```bash
# Sagas with Stream (default) + centralized IDs (version-service)
docker compose up quizzes-remote version-service -d

# Sagas with gRPC + centralized IDs (version-service)
COMM_LAYER=grpc docker compose up quizzes-remote version-service -d

# TCC with Stream + centralized IDs (version-service)
TX_MODE=tcc docker compose up quizzes-remote version-service -d

# TCC with gRPC + centralized IDs (version-service)
TX_MODE=tcc COMM_LAYER=grpc docker compose up quizzes-remote version-service -d
```

### Running as Distributed

![Distributed Stream Topology](../data/figs/architecture/topology_distr_stream.svg)
*Distributed Stream Topology*

![Distributed gRPC Topology](../data/figs/architecture/topology_distr_grpc.svg)
*Distributed gRPC Topology*

Run the gateway and all microservices:

```bash
# Sagas (default) with Stream (default)
docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d

# TCC with Stream (default)
TX_MODE=tcc docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d

# With gRPC instead of stream
COMM_LAYER=grpc docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d

# TCC + gRPC
TX_MODE=tcc COMM_LAYER=grpc docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d
```

#### Version IDs: Centralized vs. Distributed

There are two supported strategies:

**Centralized ID generation (version-service container).** Required for `quizzes-remote` and **microservices** when you
want centralized IDs. Not required for `quizzes-local` (single process), so centralized IDs work without a separate
container. Start it with:

```bash
docker compose up version-service -d
# with gRPC
COMM_LAYER=grpc docker compose up version-service -d
```

**Distributed ID generation (no version-service).** Enable with `VERSION_MODE=distributed-version`. Works with
`quizzes-local`, `quizzes-remote`, and **microservices**.

**Quick examples (distributed IDs):**

```bash
# quizzes-local (distributed IDs)
VERSION_MODE=distributed-version docker compose up quizzes-local -d

# quizzes-remote (distributed IDs)
VERSION_MODE=distributed-version docker compose up quizzes-remote -d

# microservices (distributed IDs)
VERSION_MODE=distributed-version docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d
```

#### Running with Distributed Version (no version-service needed)

Use the `VERSION_MODE` environment variable to enable the `distributed-version` Spring profile. Each service will
generate
version IDs locally using Snowflake IDs.

```bash
# Sagas + Stream + Distributed Version
VERSION_MODE=distributed-version docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d

# Sagas + gRPC + Distributed Version
VERSION_MODE=distributed-version COMM_LAYER=grpc docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d
```

> **Note:** The `distributed-version` profile can also be used with the centralized quizzes-local and quizzes-remote.

Distributed ecosystem:

**Infrastructure:**

* `eureka-server`: Service discovery
* `rabbitmq`: Message broker
* `jaeger`: Distributed tracing
* `gateway`: API Gateway (entry point for distributed application)

**Microservices:** (One Database per Service)

* `answer-service` -> `answer-db`
* `course-service` -> `course-db`
* `execution-service` -> `execution-db`
* `question-service` -> `question-db`
* `quiz-service` -> `quiz-db`
* `topic-service` -> `topic-db`
* `tournament-service` -> `tournament-db`
* `user-service` -> `user-db`

### Stopping Containers

```bash
# Stop running containers (keeps volumes)
docker compose stop

# Stop and remove containers (keeps volumes)
docker compose down

# Stop and remove containers and volumes (full reset)
docker compose down -v
```

### Cleaning Maven Cache

If the simulator library version is updated in `simulator/pom.xml`, you must clean the Maven cache volume to ensure the
test containers use the updated version.

```bash
docker volume rm microservices-simulator_m2_cache
```

### Running Local Tests

**Simulator Sagas:**

```bash
docker compose up test-simulator-sagas
```

```bash
# Quizzes Sagas:
docker compose up test-quizzes-sagas

# Quizzes TCC:
docker compose up test-quizzes-tcc
```