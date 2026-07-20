# Run Using Docker

## Table of Contents

- [Technology Requirements](#technology-requirements)
- [Docker Compose Structure](#docker-compose-structure)
- [Version IDs: Centralized vs. Distributed](#version-ids-centralized-vs-distributed)
- [Environment Variables Quick Reference](#environment-variables-quick-reference)
- [Running as Centralized with Local Service Calls](#running-as-centralized-with-local-service-calls)
- [Running as Centralized with Remote Service Calls](#running-as-centralized-with-remote-service-calls)
- [Running as Distributed](#running-as-distributed)
- [Service Access & Ports](#service-access--ports)
- [Stopping Containers](#stopping-containers)
- [Troubleshooting](#troubleshooting)
- [Running Local Tests](#running-local-tests)

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

### Version IDs: Centralized vs. Distributed

For the full behavior and rationale, see **[README: Distributed Version Service](../README.md#distributed-version-service)**.

Quick run rules:
- Default (no `VERSION_MODE`) uses centralized version management.
- `VERSION_MODE=distributed-version` is supported only with `sagas`.
- TCC always requires centralized version management.

Operational commands:

```bash
# Start centralized version-service (when needed)
docker compose up version-service -d
# or with gRPC
COMM_LAYER=grpc docker compose up version-service -d

# Use distributed IDs (sagas only)
VERSION_MODE=distributed-version docker compose up quizzes-local -d
```

### Environment Variables Quick Reference

| Variable | Options | Default | Purpose | Applies To |
|----------|---------|---------|---------|------------|
| `TX_MODE` | `sagas`, `tcc` | `sagas` | Transaction consistency model | All topologies |
| `COMM_LAYER` | `stream`, `grpc` | `stream` | Command/event transport layer | Remote & Distributed |
| `VERSION_MODE` | `<unset>`, `distributed-version` | centralized | Version ID generation strategy | All topologies |

**Quick examples combining variables:**

```bash
# Sagas + Stream + Centralized IDs (typical default)
docker compose up quizzes-remote version-service -d

# TCC + gRPC + Centralized IDs (strongest consistency, point-to-point RPC)
TX_MODE=tcc COMM_LAYER=grpc docker compose up quizzes-remote version-service -d

# Sagas + Stream + Distributed IDs (high throughput, no version-service needed)
VERSION_MODE=distributed-version docker compose up quizzes-remote -d
```

### Running as Centralized with Local Service Calls

```bash
# Sagas (default)
docker compose up quizzes-local -d

# TCC
TX_MODE=tcc docker compose up quizzes-local -d
```

### Running as Centralized with Remote Service Calls

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

**Distributed ecosystem:**

**Infrastructure:**

* `eureka-server`: Service discovery
* `rabbitmq`: Message broker
* `jaeger`: Distributed tracing
* `gateway`: API Gateway (entry point for distributed application)

*(For microservice-to-database mapping, see the [Database Configuration](../README.md#database-configuration) section in the main README).*

### Service Access & Ports

See the **[Service URLs and Ports](../README.md#service-urls-and-ports)** section in the main README for a complete list of endpoints, including:
* Gateway and Microservice REST APIs
* Infrastructure UIs (Jaeger, RabbitMQ, Eureka)
* Default credentials for databases and message brokers

### Stopping Containers

```bash
# Stop running containers (keeps volumes)
docker compose stop

# Stop and remove containers (keeps volumes)
docker compose down

# Stop and remove containers and volumes (full reset)
docker compose down -v
```

### Troubleshooting

**Maven/JAR build issues ("simulator version not found")**

If you updated `simulator/pom.xml` but containers still use old version:

```bash
# Clean Maven cache volume
docker volume rm microservices-simulator_m2_cache

# Rebuild and restart
docker compose down -v
docker compose up quizzes-local -d
```

### Running Local Tests

Run automated test suites against the running containers:

```bash
# Simulator Sagas tests
docker compose up test-simulator-sagas

# Quizzes Sagas tests
docker compose up test-quizzes-sagas

# Quizzes TCC tests
docker compose up test-quizzes-tcc
```

Tests will output results to the console and exit with:
- Exit code `0`: All tests passed
- Exit code non-zero: One or more tests failed (check logs above)

To view test logs after completion:

```bash
docker compose logs test-quizzes-sagas | tail -100
```
