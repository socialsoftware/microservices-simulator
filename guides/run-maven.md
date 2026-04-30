# Run Using Maven

## Table of Contents

- [Technology Requirements](#technology-requirements)
- [Setting up the Database](#setting-up-the-database)
  - [Running in a Docker container](#running-in-a-docker-container)
  - [Running on a local machine](#running-on-a-local-machine)
- [Setting up Jaeger Tracing](#setting-up-jaeger-tracing)
- [Simulator](#simulator)
  - [Install simulator library](#install-simulator-library)
  - [Run simulator tests](#run-simulator-tests)
- [Quizzes Centralized Simulation](#quizzes-centralized-simulation)
  - [Launch simulator for Sagas with local](#launch-simulator-for-sagas-with-local)
  - [Launch simulator for TCC](#launch-simulator-for-tcc)
  - [Running Sagas Tests](#running-sagas-tests)
  - [Running TCC Tests](#running-tcc-tests)
- [Quizzes Centralized Simulation with Remote Service Calls](#quizzes-centralized-simulation-with-remote-service-calls)
  - [Additional Requirements:](#additional-requirements)
  - [Running with Stream](#running-with-stream)
  - [Running with gRPC](#running-with-grpc)
- [Quizzes Distributed Simulation Deployment](#quizzes-distributed-simulation-deployment)
  - [Prerequisites](#prerequisites)
  - [Running the Microservices](#running-the-microservices)
- [Maven Profiles Quick Reference](#maven-profiles-quick-reference)
- [Service Access & Ports](#service-access--ports)
- [Running JMeter tests](#running-jmeter-tests)
  - [Viewing JMeter tests structure](#viewing-jmeter-tests-structure)

### Technology Requirements

- [Maven 3.9.9](https://archive.apache.org/dist/maven/maven-3/3.9.9/)
- [Java 21+](https://openjdk.org/projects/jdk/21/)
- [PSQL 14](https://www.postgresql.org/download/)
- [RabbitMQ 3.12+](https://www.rabbitmq.com/download.html) (required for stream profile)
- [Jaeger 1.75](https://www.jaegertracing.io/)
- [JMeter 5.6](https://jmeter.apache.org/download_jmeter.cgi)

### Setting up the Database

There are two ways to set up the database:

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
mvn clean install -DskipTests
```

#### Run simulator tests

```bash
mvn clean test
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

Running the application in distributed mode requires setting up individual databases for each service and running
RabbitMQ for inter-service communication.

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
    mvn clean install -DskipTests
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

Start each microservice in separate terminal windows. Use the corresponding Maven profiles for transaction model and communication layer:

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

**Distributed Version Profile (optional):** To use the `distributed-version` profile (no version-service needed), add `distributed-version` to the profiles list:

```bash
# Example: Answer Service with Sagas + Stream + Distributed Version
mvn spring-boot:run -Panswer-service,sagas,stream,distributed-version
```

**3. Start the Gateway (from `simulator/`):**

```bash
cd simulator/
mvn -Pgateway spring-boot:run
```

---

### Maven Profiles Quick Reference

Maven profiles in this project combine three dimensions: **transaction model**, **communication layer**, and **deployment mode**.

**Transaction Model:**
- `sagas` — Semantic locks (default)
- `tcc` — Transactional Causal Consistency

**Communication Layer:**
- `local` — In-process calls (centralized only)
- `stream` — RabbitMQ broker (default)
- `grpc` — Point-to-point gRPC

**Deployment Mode:**
- `<service-name>` — Distributed microservice (e.g., `answer-service`, `quiz-service`)
- `gateway` — API Gateway (requires distributed mode)
- `version-service` — Version ID generation service (optional if using `distributed-version`)
- `test-sagas` / `test-tcc` — Test suites

**Common combinations:**

```bash
# Centralized (single process)
mvn spring-boot:run -Psagas,local          # Sagas + Local (default)
mvn spring-boot:run -Ptcc,local            # TCC + Local

# Centralized with Remote Calls
mvn spring-boot:run -Psagas,stream         # Sagas + Stream (RabbitMQ)
mvn spring-boot:run -Psagas,grpc           # Sagas + gRPC
mvn spring-boot:run -Ptcc,stream           # TCC + Stream

# Distributed (requires version-service or distributed-version)
mvn spring-boot:run -Ptournament-service,sagas,stream   # Tournament service
mvn spring-boot:run -Pgateway,sagas,stream              # API Gateway
mvn spring-boot:run -Pversion-service,stream            # Version Service

# With Distributed Version (no version-service needed)
mvn spring-boot:run -Psagas,local,distributed-version
mvn spring-boot:run -Panswer-service,sagas,stream,distributed-version
```

---

### Service Access & Ports

See the **[Service URLs and Ports](../README.md#service-urls-and-ports)** section in the main README for a complete list of endpoints, including:
* Gateway and Microservice REST APIs
* Infrastructure UIs (Jaeger, RabbitMQ, Eureka)
* Default credentials for databases and message brokers

---

### Running JMeter tests

* After starting application with the tcc profile, either using Docker or Maven, and installing JMeter

```
cd applications/quizzes/jmeter/tournament/thesis-cases/
jmeter -n -t TEST.jmx
```

* Some test cases:
    * `5a-updateStudentName-addParticipant-processUpdateNameEvent.jmx`
    * `5b-addParticipant-updateStudentName-processUpdateNameEvent.jmx`
    * `5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent.jmx`
    * `5d-addParticipant1-updateStudentName-processUpdateNameEvent1-addParticipant2-processUpdateNameEvent2.jmx`
    * `8-5-update-tournament-concurrent-intention-pass.jmx`
    * `8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first.jmx`
    * `8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last.jmx`
    * `8-8-update-execution-student-add-participant-process-event-add-participant.jmx`
    * `8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first.jmx`
    * `8-10-concurrent-delete-tournament-add-participant.jmx`

### Viewing JMeter tests structure

```
cd applications/quizzes/jmeter/tournament/thesis-cases/
jmeter
```

* The command launches JMeter GUI. By clicking `File > Open` and selecting a test file it is possible to observe the
  test structure.
* Tests can also be run using the GUI, by clicking on the `Start` button.