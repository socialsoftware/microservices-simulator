# Microservices Simulator

The artifact supports the test of business logic of a microservices application designed on the concept of Domain-Driven
Design Aggregate and using several transactional models.

The currently supported transactional models are:

* Eventual Consistency
    * Sagas applying the Orchestration variant
* Transactional Causal Consistency

The system allows testing the interleaving of functionalities execution in a deterministic context, such that it is
possible to evaluate the resulting behavior.

The description of the examples for Transactional Causal Consistency are
in [Transactional Causal Consistent Microservices Simulator](https://doi.org/10.1007/978-3-031-35260-7_4).

The simulator supports multiple execution modes to test different aspects of system behavior, ranging from simple local
execution to full distributed deployment.

| Mode            | Description                                                                                                                                                                                                                                                                                | Profiles                                                          | Infrastructure                                                                                                                            |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **Centralized** | Runs as a single application. Supports local (internal), stream (RabbitMQ), or gRPC service calls. Optionally uses `distributed-version` profile for Snowflake-based version IDs.                                                                                                          | `sagas\|tcc, local\|stream\|grpc`                                 | PostgreSQL, Jaeger, (RabbitMQ for stream)                                                                                                 |
| **Distributed** | Each domain service runs independently. Uses Eureka for discovery (or Spring Cloud Kubernetes on K8s) and RabbitMQ or gRPC. Optionally uses `distributed-version` profile for local version ID generation via Snowflake IDs. Can also be deployed on [Kubernetes](#kubernetes-deployment). | Service-specific (e.g., `answer-service,sagas\|tcc,stream\|grpc`) | PostgreSQL (**per service** in **Docker**, **centralized** with multiple databases with **Maven**), Jaeger, Eureka, (RabbitMQ for stream) |

## Run Using Docker

### Technology Requirements

- [Docker Compose V2](https://docs.docker.com/compose/install/)

### Build the Application

```bash
docker compose build
```

Or run the service with the flag `--build`

### Running as Centralized with Local Service Calls

```bash
# Sagas (default)
docker compose up quizzes-local -d

# TCC
TX_MODE=tcc docker compose up quizzes-local -d
```

### Running as Centralized with Remote Service Calls

```bash
# Sagas with Stream (default)
docker compose up quizzes-remote version-service -d

# Sagas with gRPC
COMM_LAYER=grpc docker compose up quizzes-remote version-service -d

# TCC with Stream
TX_MODE=tcc docker compose up quizzes-remote version-service -d

# TCC with gRPC
TX_MODE=tcc COMM_LAYER=grpc docker compose up quizzes-remote version-service -d
```

### Running as Distributed

First, build the gateway and all microservices:

```bash
docker compose build --with-dependencies gateway
```

Then, run the gateway and all microservices:

```bash
# Sagas (default) with Stream (default)
docker compose up gateway -d

# TCC with Stream (default)
TX_MODE=tcc docker compose up gateway version-service -d

# With gRPC instead of stream
COMM_LAYER=grpc docker compose up gateway version-service -d

# TCC + gRPC
TX_MODE=tcc COMM_LAYER=grpc docker compose up gateway version-service -d
```

#### Running with Distributed Version (no version-service needed)

Use the `VERSION_MODE` environment variable to enable the `distributed-version` profile. Each microservice will generate
version IDs locally using Snowflake IDs.

```bash
# Sagas + Stream + Distributed Version
VERSION_MODE=distributed-version docker compose up gateway -d

# Sagas + gRPC + Distributed Version
VERSION_MODE=distributed-version COMM_LAYER=grpc docker compose up gateway -d
```

Starting the gateway will automatically start the entire distributed ecosystem, including:

**Infrastructure:**

* `eureka-server`: Service discovery
* `rabbitmq`: Message broker for async communication
* `gateway`: API Gateway (entry point)

**Microservices:** (One Database per Service)

* `answer-service` -> `answer-db`
* `course-service` -> `course-db`
* `execution-service` -> `execution-db`
* `question-service` -> `question-db`
* `quiz-service` -> `quiz-db`
* `topic-service` -> `topic-db`
* `tournament-service` -> `tournament-db`
* `user-service` -> `user-db`

> **Note:** The `version-service` is **not** started automatically. If you need centralized versioning (i.e., you are *
*not** using the `distributed-version` profile), you must start it manually:
>
> ```bash
> docker compose up version-service -d
> ```

### Running Local Tests

> **Note:** Run `build-simulator` first before running tests.

```bash
docker compose up build-simulator
```

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

### Kubernetes Deployment

The distributed mode can also be deployed on Kubernetes, using Spring Cloud Kubernetes for service discovery instead of
Eureka.

##### Prerequisites

Install the following packages:

- [Docker](https://docs.docker.com/get-docker/) - Container runtime
- [kubectl](https://kubernetes.io/docs/tasks/tools/) - Kubernetes CLI
- [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) (recommended) – Local Kubernetes cluster

**Create a Kind cluster:**

```bash
kind create cluster --name microservices
```

##### Build and Load Images

```bash
# Build all Docker images
docker compose build --with-dependencies gateway

# Load images into Kind cluster
for img in gateway simulator quizzes; do
  kind load docker-image ${img}:latest --name microservices
done
```

##### Deploy to Kubernetes

```bash
# Create namespace and RBAC
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/configmap.yaml

# Deploy infrastructure
kubectl apply -f k8s/infrastructure/rabbitmq.yaml
kubectl apply -f k8s/infrastructure/jaeger.yaml

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=rabbitmq -n microservices-simulator --timeout=120s
kubectl wait --for=condition=ready pod -l app=jaeger -n microservices-simulator --timeout=60s

# Deploy microservices (choose one)
# For stream communication
kubectl apply -f k8s/services-stream/

# For gRPC communication
# kubectl apply -f k8s/services-grpc/

# Check status
kubectl get pods -n microservices-simulator
```

> **Note:** To change transactional model profile, edit `k8s/services-stream/` or `k8s/services-grpc/` and change the
> `SPRING_PROFILES_ACTIVE` environment variable of each service.

##### Access the Application

```bash
# Port-forward to gateway
kubectl port-forward svc/gateway 8080:8080 -n microservices-simulator
```

##### Access Jaeger UI

```bash
kubectl port-forward svc/jaeger 16686:16686 -n microservices-simulator
```

Then open [http://localhost:16686](http://localhost:16686) to view distributed traces.

##### Cleanup

```bash
kubectl delete namespace microservices-simulator
```

---

#### Azure Kubernetes Service (AKS) Deployment

Deploy the distributed mode to Azure Kubernetes Service for cloud-based deployments.

##### Prerequisites

- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) installed
- Active Azure subscription (e.g., Azure for Students)

##### Setup AKS Cluster

```bash
# Login to Azure
az login

# Create Resource Group
az group create --name simulator-rg-es --location spaincentral

# Create AKS Cluster (Free tier, minimal resources)
az aks create \
  --resource-group simulator-rg-es \
  --name simulator-cluster \
  --tier free \
  --node-count 1 \
  --node-vm-size Standard_B2s_v2 \
  --generate-ssh-keys

# Connect to the Cluster
az aks get-credentials --resource-group simulator-rg-es --name simulator-cluster

# Verify connection
kubectl get nodes
```

##### Register Azure Resource Providers (One-time setup)

```bash
# Register Container Registry provider (required for ACR)
az provider register --namespace Microsoft.ContainerRegistry

# Check registration status (wait until "Registered")
az provider show --namespace Microsoft.ContainerRegistry --query "registrationState"
```

##### Push Images to Azure Container Registry

```bash
# Run the push script (creates ACR, attaches to AKS, pushes images)
chmod +x scripts/push-to-acr.sh
./scripts/push-to-acr.sh
```

##### Deploy to Azure

```bash
# 1. Base setup
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/rbac.yaml

# 2. Infrastructure (Centralized PostgreSQL + RabbitMQ)
kubectl apply -f k8s/infrastructure/

# 3. Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n microservices-simulator --timeout=180s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n microservices-simulator --timeout=120s

# 4. Deploy Azure-optimized microservices (uses ACR images + centralized DB)
kubectl apply -f k8s/services-azure/

# 5. Check status
kubectl get pods -n microservices-simulator

# 6. Access the gateway
kubectl get svc gateway -n microservices-simulator
# Or use port-forward
kubectl port-forward -n microservices-simulator svc/gateway 8080:8080
```

**Save costs by stopping the cluster when not in use:**

```bash
# Stop the cluster
az aks stop --name simulator-cluster --resource-group simulator-rg-es

# Start the cluster again
az aks start --name simulator-cluster --resource-group simulator-rg-es
```

##### Cleanup Azure Resources

```bash
# Delete the cluster
az aks delete --name simulator-cluster --resource-group simulator-rg-es

# Delete everything (including ACR)
az group delete --name simulator-rg-es
```

##### Managing Multiple Clusters (Local vs Azure)

When using both local (Kind) and Cloud (Azure) clusters, your `kubectl` context may be pointing to the wrong cluster.

To see all available clusters:
```bash
kubectl config get-contexts
```

To switch back to your local Kind cluster:
```bash
kubectl config use-context kind-microservices
```

To switch to your Azure AKS cluster:
```bash
kubectl config use-context simulator-cluster
```

---

## Run Using IntelliJ

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

- Run the `quizzes-simulator` folder (contains `sagas-stream`, `sagas-grpc`, `tcc-stream`, `tcc-grpc` configurations)
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

## Run Using Maven

### Technology Requirements

- [Maven 3.9.9](https://archive.apache.org/dist/maven/maven-3/3.9.9/)
- [Java 21+](https://openjdk.org/projects/jdk/21/)
- [PSQL 14](https://www.postgresql.org/download/)
- [RabbitMQ 3.12+](https://www.rabbitmq.com/download.html) (required for stream profile)
- [Jaeger 1.75](https://www.jaegertracing.io/)
- [JMeter 5.6](https://jmeter.apache.org/download_jmeter.cgi)

### Setting up the Database

There is two ways to set up the database:

#### Running in a Docker container

1. **Start postgres container**

    ```bash
    docker compose up postgres -d
    ```
   > This will create all the necessary databases with user and password `postgres`

#### Running on a local machine

1. **Start PostgreSQL:**

    ```bash
    sudo service postgresql start
    ```

2. **Create the databases:**

    ```bash
    sudo su -l postgres
    dropdb msdb
    createdb msdb
    ```

3. **Create microservice databases (required for distributed mode):**

    ```bash
    psql -U postgres -d msdb -f data/init/init-databases.sh
    ```

4. **Create user to access db:**

    ```bash
    psql msdb
    CREATE USER your-username WITH SUPERUSER LOGIN PASSWORD 'yourpassword';
    \q
    exit
    ```

5. **Configure application properties:**
    - Fill in the placeholder fields with your database credentials in
      `applications/quizzes/src/main/resources/application.yaml`

### Setting up Jaeger Tracing

```bash
docker compose up jaeger -d
```

---

### Simulator

```bash
cd simulator
```

#### Install simulator library

```bash
mvn clean install
```

#### Run simulator tests

```bash
mvn clean -Ptest-sagas test
```

---

### Quizzes Centralized Simulation

```bash
cd applications/quizzes
```

#### Launch simulator for Sagas with local

```bash
mvn clean spring-boot:run -Psagas,local
```

#### Launch simulator for TCC

```bash
mvn clean spring-boot:run -Ptcc,local
```

#### Running Sagas Tests

```bash
mvn clean -Ptest-sagas test
```

#### Running TCC Tests

```bash
mvn clean -Ptest-tcc test
```

---

### Quizzes Centralized Simulation with Remote Service Calls

#### Additional Requirements:

1. **Start RabbitMQ (for stream profile):**

    ```bash
    docker compose up rabbitmq -d
    ```

#### Running with Stream

```bash
cd applications/quizzes
mvn spring-boot:run -Psagas,stream
```

#### Running with gRPC

```bash
cd applications/quizzes
mvn spring-boot:run -Psagas,grpc
```

---

### Quizzes Distributed Simulation Deployment

Running the application in distributed mode requires setting up individual databases for each service and
running RabbitMQ for inter-service communication.

#### Prerequisites

1. [Start PostgreSQL](#setting-up-the-database)

2. [Start RabbitMQ](#additional-requirements)

3. Start Eureka service discovery (required for local microservices):

    ```bash
    docker compose up eureka-server -d
    ```

4. **Install the simulator library (if not already done):**

    ```bash
    cd simulator
    mvn clean install
    cd ..
    ```

#### Running the Microservices

**1. Start the Version Service (stream or grpc)** (skip this step if using `distributed-version` profile)**:**

```bash
cd simulator
mvn clean -Pversion-service,stream spring-boot:run
```

**2. Start each Quizzes microservice (from `applications/quizzes`):**

```bash
cd applications/quizzes
```

| Service                  | Command                                                            |
|--------------------------|--------------------------------------------------------------------|
| Answer Service           | `mvn spring-boot:run -Panswer-service,sagas\|tcc,stream\|grpc`     |
| Course Service           | `mvn spring-boot:run -Pcourse-service,sagas\|tcc,stream\|grpc`     |
| Course Execution Service | `mvn spring-boot:run -Pexecution-service,sagas\|tcc,stream\|grpc`  |
| Question Service         | `mvn spring-boot:run -Pquestion-service,sagas\|tcc,stream\|grpc`   |
| Quiz Service             | `mvn spring-boot:run -Pquiz-service,sagas\|tcc,stream\|grpc`       |
| Topic Service            | `mvn spring-boot:run -Ptopic-service,sagas\|tcc,stream\|grpc`      |
| Tournament Service       | `mvn spring-boot:run -Ptournament-service,sagas\|tcc,stream\|grpc` |
| User Service             | `mvn spring-boot:run -Puser-service,sagas\|tcc,stream\|grpc`       |

To use the distributed version profile (no version-service needed), add `distributed-version` to the Maven profiles.
This also works in centralized mode with any communication profile:

```bash
# Distributed mode example
mvn spring-boot:run -Panswer-service,sagas,stream,distributed-version

# Centralized mode examples
mvn spring-boot:run -Psagas,local,distributed-version
mvn spring-boot:run -Psagas,stream,distributed-version
mvn spring-boot:run -Psagas,grpc,distributed-version
```

**3. Start the Gateway (from `simulator/`):**

```bash
cd simulator/
mvn -Pgateway spring-boot:run
```

---

## Test Cases

**Sagas test cases:**

- [Workflow Test Plan (Simulator)](simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/sagas/workflow/PlanOrderTest.groovy)
- [Tournament Functionality Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/)

**TCC test cases:**

- [Tournament Merge Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/aggregates/TournamentMergeUnitTest.groovy)
- [Tournament Functionality Tests (Quizzes)](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)

---

## Configuration

The application uses Spring Boot profiles and YAML configuration files to manage different deployment modes.

### Jaeger Tracing

The project uses [Jaeger](https://www.jaegertracing.io/) for distributed tracing to monitor and visualize the flow of
requests across microservices.

* **Dashboard**: Access the Jaeger UI at [http://localhost:16686](http://localhost:16686).
* **Collector**: The application sends traces to the Jaeger collector on `http://localhost:4317` using the OTLP gRPC
  protocol.
* **Instrumentation**: Custom instrumentation is implemented in `TraceManager` using the OpenTelemetry SDK to trace
  functionalities and their steps.

### Service Discovery

In distributed mode, local deployments use Eureka for service discovery. The gateway and each microservice register with
the Eureka
server at `http://${EUREKA_HOST:localhost}:8761/eureka/`. When deploying on Kubernetes, the `kubernetes` profile enables
Spring Cloud Kubernetes discovery instead of Eureka.

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

### gRPC Command Gateway

An alternative remote transport is available with the `grpc` profile. Each service exposes a gRPC endpoint for
commands (see `GrpcServerRunner`), and callers use `GrpcCommandGateway` with Eureka-based discovery. Default and
service-specific gRPC ports are configured in the `application-*-service.yaml` files (and exposed via Eureka metadata
key `grpcPort`). Override the default client port with `grpc.command.default-port` or per-service with
`grpc.command.<service>.port` when needed.

### Distributed Version Service

When running in distributed mode with the `distributed-version` profile active, each microservice generates version IDs
locally using a
[Snowflake ID](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/domain/version/SnowflakeIdGenerator.java)
generator, removing the need for a centralized version-service. This profile can also be used in centralized mode with
any communication profile (`local`, `stream`, or `grpc`). The 64-bit IDs are composed of a 41-bit
timestamp, a 10-bit machine ID (derived from `spring.application.name`), and a 12-bit sequence number,
guaranteeing globally unique, monotonically increasing versions across services.

This option is only supported with the **sagas** transactional model (TCC requires centralized version management).

| Profile               | Version Source               | Requires version-service? |
|-----------------------|------------------------------|---------------------------|
| *(default)*           | Centralized `VersionService` | Yes                       |
| `distributed-version` | Local `SnowflakeIdGenerator` | No                        |

### Service URLs and Ports

Each microservice runs on a dedicated port:

| Service            | Port | Profile File                                                                                                       |
|--------------------|------|--------------------------------------------------------------------------------------------------------------------|
| Gateway            | 8080 | [application.yaml](applications/gateway/src/main/resources/application.yaml)                                       |
| Version Service    | 8081 | -                                                                                                                  |
| Answer Service     | 8082 | [application-answer-service.yaml](applications/quizzes/src/main/resources/application-answer-service.yaml)         |
| Course Execution   | 8083 | [application-execution-service.yaml](applications/quizzes/src/main/resources/application-execution-service.yaml)   |
| Question Service   | 8084 | [application-question-service.yaml](applications/quizzes/src/main/resources/application-question-service.yaml)     |
| Quiz Service       | 8085 | [application-quiz-service.yaml](applications/quizzes/src/main/resources/application-quiz-service.yaml)             |
| Topic Service      | 8086 | [application-topic-service.yaml](applications/quizzes/src/main/resources/application-topic-service.yaml)           |
| Tournament Service | 8087 | [application-tournament-service.yaml](applications/quizzes/src/main/resources/application-tournament-service.yaml) |
| User Service       | 8088 | [application-user-service.yaml](applications/quizzes/src/main/resources/application-user-service.yaml)             |

Every service port can be changed, including `version-service` port 8081, and `gateway` port 8080. Service Discovery
will
map the service name to the service port automatically.

### API Gateway Configuration

The [Gateway application-gateway.yaml](simulator/src/main/resources/application-gateway.yaml) configures:

1. **Service discovery** ([lines 20-28](simulator/src/main/resources/application-gateway.yaml)): Eureka discovery for
   local distributed deployments; Kubernetes discovery is enabled via the `kubernetes` profile.

2. **Route definitions** ([lines 290-300 in application.yaml](applications/quizzes/src/main/resources/application.yaml)): The API Gateway is a Spring MVC-based application that dynamically proxies HTTP requests to backend services. Routes are configured via `gateway.routes.imports` referencing the target microservice application properties, which the `DynamicMVCProxyController` uses to forward REST calls.

3. **Version service URL**: The Admin controller endpoints directly interact with the remote microservices for configuration sync.

## Code structure

### Simulator

* The core concepts of [Domain-Driven Design](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/domain)
* The core concepts for the distributed
  functionalities [Coordination](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination)
* The core concepts for management of [Sagas](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/sagas)
* The core concepts for management of [TCC](simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/causal)

### Quizzes Microservice System

* A case study for [Quizzes Tutor](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes)
    * The transactional model
      independent [Microservices](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices)
    * The Sagas implementation for
        * Aggregates (per microservice, e.g.
          [Tournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate))
        * Coordination (per microservice, e.g.
          [Tournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination))
    * The TCC implementation for
        * Aggregates (per microservice, e.g.
          [Tournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate))
        * Coordination (per microservice, e.g.
          [Tournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination))
* The tests of the [Quizzes Tutor](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes) for
    * [Sagas](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination)
    * [TCC Aggregates](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/aggregates)
    * [TCC Coordination](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination)

The code follows the structure in the simulator library and application decomposition figures, where the packages in
blue and orange contain, respectively, the microservices domain specific code and the transactional causal consistency
domain specific code.

![Simulator Library Decomposition](data/figs/simulator_library_decomposition.png)

![Application Decomposition](data/figs/application_decomposition.png)

The API Gateway is used when running the quizzes application as microservices to route API requests to the appropriate
microservice. The gateway operates as an MVC application using a custom dynamic proxy controller to forward REST requests.

## How to implement and test your own business logic for Sagas and TCC (Illustrated with Quizzes Microservice System)

The figure shows the main classes to be extended for aggregates, their events and services.

![Aggregate Model](data/figs/aggregate_domain_model_extension.png)

Apply the following steps to define a domain-specific aggregate, its events and services, here illustrated with
the Quizzes Tutor system and its Tournament aggregate.

For the transactional model independent part:

1. **Define Aggregate**: Each microservice is modeled as an aggregate. The first step is to define the aggregates.
   The simulator uses Spring-Boot and JPA, so the domain entities definition uses the JPA notation.
   In [Tournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java)
   aggregate we can see the aggregate root entity and the reference to its internal entities.
   > **NOTE**: It's recomended to create Aggregates with a single name (Example: instead of `CourseExecution`, use
   `Execution`)
2. **Specify Invariants**: The aggregate invariants are defined by overriding
   method [verifyInvariants()](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java).
3. **Define Events**: Define the events published by upstream aggregates and subscribed by downstream aggregates,
   like [UpdateStudentNameEvent](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/events/UpdateStudentNameEvent.java).
4. **Subscribe Events**: The events published by upstream aggregates can be subscribed by overriding
   method [getEventSubscriptions()](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/Tournament.java).
5. **Define Event Subscriptions**: Events can be subscribed depending on its data. Therefore, define subscription
   classes
   like [TournamentSubscribesUpdateStudentName](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/events/subscribe/TournamentSubscribesUpdateStudentName.java).
6. **Define Event Handlers**: For each subscribed event define an event handler that delegates the handling in a
   handling functionality,
   like [UpdateStudentNameEventHandler](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/events/handling/handlers/UpdateStudentNameEventHandler.java)
   and its handling
   functionality [processUpdateStudentNameEvent(...)](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/eventProcessing/TournamentEventProcessing.java).
7. **Define Aggregate Services**: Define the microservice API, whose implementation interact with the unit of work to
   register changes and publish events, like
   service [updateExecutionStudentName(...)](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/execution/service/ExecutionService.java).
8. **Define Event Handling**: Define the aggregates event handling, that periodically polls the event table to process
   events,
   like [TournamentEventHandling](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/events/handling/TournamentEventHandling.java).
9. **Define Event Subscriber Service**: Define the event subscriber service, that subscribes to events published by
   other microservices via Spring Cloud Stream,
   like [TournamentEventSubscriberService](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/events/TournamentEventSubscriberService.java).

For the transactional model dependent part:

1. **Define Saga Aggregates**: Extend aggregates with the information required for semantic locks,
   like [SagaTournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/sagas/SagaTournament.java)
   and
   its [Semantic Lock](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/sagas/states/TournamentSagaState.java).
2. **Define Causal Aggregates**: Extend aggregates with the information required for causal consistency,
   like [CausalTournament](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/aggregate/causal/CausalTournament.java)

To define the system functionalities, it is necessary to extend the simulator part for coordination.

![Functionality Model](data/figs/functionality_domain_model_extension.png)

For the functionalities:

1. **Define Functionalities**: Functionalities coordinate the execution of aggregate services using sagas, like
   functionality [AddParticipantFunctionalitySagas(...)](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java)
   and [AddParticipantFunctionalityTCC(...)](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/causal/AddParticipantFunctionalityTCC.java)
2. **Define Commands**: Define the commands to be executed by the functionalities,
   like [AddParticipantCommand](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/command/tournament/AddParticipantCommand.java).
   Every method of the aggregate service should have a corresponding command.

For the inter-service communication:

1. **Create the CommandHandler of the aggregate**: It receives commands
   and calls the corresponding aggregate service method of that command,
   like [TournamentCommandHandler](applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/commandHandler/TournamentCommandHandler.java).
2. **Configure Spring Cloud Stream Bindings** (for `stream` profile): Define the command and event channels in
   `application.yaml`,
   like [tournament-service bindings](applications/quizzes/src/main/resources/application-tournament-service.yaml).
3. **Configure gRPC Server Port** (for `grpc` profile): Define the gRPC server port in the service profile file and
   expose it via Eureka metadata,
   like [tournament-service gRPC config](applications/quizzes/src/main/resources/application-tournament-service.yaml).
4. **Configure API Gateway Routes**: Define the route mappings in the microservice's application yaml file and import it via
   `gateway.routes.imports` properties to have the MVC proxy controller route HTTP requests to the
   new microservice.

To write tests:

1. **Design Test Cases**: Define tests cases for the concurrent execution of functionalities deterministically enforcing
   execution orders, like in
   the [Concurrent Execution of Update Name and Add Participant](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/AddParticipantAndUpdateStudentNameTest.groovy).
   Directory [coordination](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/)
   contains the test of more complex interleavings using the sagas transactional model.

## Running JMeter tests

* After starting application with the tcc profile, either using Docker or Maven, and installing JMeter

```
cd applications/quizzes/jmeter/tournament/thesis-cases/
jmeter -n -t TEST.jmx
```

* Some test cases:
    * [5a-updateStudentName-addParticipant-processUpdateNameEvent.jmx](applications/quizzes/jmeter/tournament/thesis-cases/5a-updateStudentName-addParticipant-processUpdateNameEvent.jmx)
    * [5b-addParticipant-updateStudentName-processUpdateNameEvent.jmx](applications/quizzes/jmeter/tournament/thesis-cases/5b-addParticipant-updateStudentName-processUpdateNameEvent.jmx)
    * [5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent.jmx](applications/quizzes/jmeter/tournament/thesis-cases/5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent.jmx)
    * [5d-addParticipant1-updateStudentName-processUpdateNameEvent1-addParticipant2-processUpdateNameEvent2.jmx](applications/quizzes/jmeter/tournament/thesis-cases/5d-addParticipant1-updateStudentName-processUpdateNameEvent1-addParticipant2-processUpdateNameEvent2.jmx)
    * [8-5-update-tournament-concurrent-intention-pass.jmx](applications/quizzes/jmeter/tournament/thesis-cases/8-5-update-tournament-concurrent-intention-pass.jmx)
    * [8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first.jmx](applications/quizzes/jmeter/tournament/thesis-cases/8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first.jmx)
    * [8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last.jmx](applications/quizzes/jmeter/tournament/thesis-cases/8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last.jmx)
    * [8-8-update-execution-student-add-participant-process-event-add-participant.jmx](applications/quizzes/jmeter/tournament/thesis-cases/8-8-update-execution-student-add-participant-process-event-add-participant.jmx)
    * [8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first.jmx](applications/quizzes/jmeter/tournament/thesis-cases/8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first.jmx)
    * [8-10-concurrent-delete-tournament-add-participant.jmx](applications/quizzes/jmeter/tournament/thesis-cases/8-10-concurrent-delete-tournament-add-participant.jmx)

### Viewing JMeter tests structure

```
cd applications/quizzes/jmeter/tournament/thesis-cases/
jmeter
```

* The command launches JMeter GUI. By clicking `File > Open` and selecting a test file it is possible to observe the
  test structure.
* Tests can also be run using the GUI, by clicking on the `Start` button.

## Spock Tests in [DAIS2023](https://link.springer.com/chapter/10.1007/978-3-031-35260-7_4) paper - 23nd International Conference on Distributed Applications and Interoperable Systems

To reproduce the paper results follow the steps:

* Analyze a figure in the paper, fig3a-d and fig4;
* Read the test case code for the figure, including the final assertions that define the expected behavior (see below);
* Run the test case (see below);
* Read the logger INFO messages, they use UPPERCASE. They identify when a functionality and event processing starts and
  ends and what its version number is.
    * For instance, in test-fig4 both functionalities start with the same version number (they are concurrent), but
      addParticipant finishes with a higher number, because it finishes after updateName. It can be observed in the log
      that an exception was thrown, due to the invariant break.

### Figure 3(a)

* [Test code](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)
* Run:

```
docker compose up test-fig3a
```

### Figure 3(b)

* [Test code](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)
* Run:

```
docker compose up test-fig3b
```

### Figure 3(c)

* [Test code](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)
* Run:

```
docker compose up test-fig3c
```

### Figure 3(d)

* [Test code](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)
* Run:

```
docker compose up test-fig3d
```

### Figure 4

* [Test code](applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/causal/coordination/TournamentFunctionalityCausalTest.groovy)
* Run:

```
docker compose up test-fig4
```
