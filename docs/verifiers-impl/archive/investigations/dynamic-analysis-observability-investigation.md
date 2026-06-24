# Dynamic-analysis observability investigation

Date: 2026-04-28

Purpose: understand what Quizzes and the simulator already expose through logs, traces, tests, and runtime infrastructure before choosing a hybrid static/dynamic aggregate-key binding approach.

## Executive summary

The current codebase already has useful observability, but it is not enough by itself to solve exact aggregate-key binding reliably.

What exists today:

- JSON file logs via Logstash encoder.
- Console logs with workflow, step, command, handler, unit-of-work, and aggregate-load messages.
- Manual OpenTelemetry tracing exported to Jaeger/OTLP.
- Trace hierarchy for root -> functionality -> step -> delay.
- Behaviour/fault tests that explicitly start/end/flush root spans.
- Command objects with a standard `rootAggregateId` field.
- Unit-of-work methods that observe aggregate IDs at load/commit/abort points.
- Quizzes command-handler logs that often include domain IDs.

Main gap:

> Existing logs/traces usually identify functionality names, step names, command classes, and some aggregate IDs, but they do not consistently capture structured command/DTO fields, input argument values, or exact static anchors. Therefore logs/traces are useful evidence, but should not be the only foundation for exact key binding.

Recommended direction remains Option D:

> Static analysis chooses what to observe. Runtime observation records structured evidence at simulator/library hook points. Existing logs/traces are parsed as auxiliary evidence and diagnostics.

## Runtime check performed

A local Jaeger container was started:

```bash
docker compose up -d jaeger
```

Then a Quizzes behaviour test was run against it:

```bash
cd applications/quizzes
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 \
  mvn -Ptest-sagas test -Dtest=CreateTournamentFaultTest -DfailIfNoTests=false
```

Result:

- Test passed.
- Jaeger reported service `quizzes`.
- One trace was available through `http://localhost:16686/api/traces?service=quizzes`.
- Trace contained root, functionality, step, compensation, warning, and forced-end spans/events.

## Logging infrastructure

### Logback configuration

File:

- `simulator/src/main/resources/logback-spring.xml`

Behavior:

- Writes JSON logs to `./logs/app-test-${timestamp}.log` using `net.logstash.logback.encoder.LogstashEncoder`.
- Writes human-readable console logs using a pattern encoder.
- Root log level is `INFO`.

Example JSON fields:

```json
{
  "@timestamp": "2026-04-14T01:11:29.9683+01:00",
  "message": "Enrolling student: 3 in course execution: 2",
  "logger_name": "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.messaging.ExecutionCommandHandler",
  "thread_name": "main",
  "level": "INFO"
}
```

Important limitation:

- Current JSON logs do not include OpenTelemetry trace IDs/span IDs by default.
- Most domain data is embedded in free-text `message`, not structured fields.

## Existing log evidence quality

### Strong-ish evidence

Some logs are very useful because they include IDs and context.

Examples from `CreateTournamentFaultTest` run:

```text
START EXECUTION FUNCTIONALITY: CreateTournamentFunctionalitySagas with version ...
START EXECUTION STEP: getCourseExecutionStep with from functionality CreateTournamentFunctionalitySagas
Executing command via LocalCommandService: SagaCommand (serialization=false)
Delegating local command to handler for service: execution
Getting course execution by id: 2
Loaded and registered read for aggregate ID: 2 - SagaExecution
END EXECUTION STEP: getCourseExecutionStep with from functionality CreateTournamentFunctionalitySagas
```

This can support an inference like:

```text
CreateTournamentFunctionalitySagas/getCourseExecutionStep read SagaExecution(2)
```

Other useful examples:

```text
Getting user by ID: 3
Loaded and registered read for aggregate ID: 3 - SagaUser

Enrolling student: 4 in course execution: 2
Loaded and registered read for aggregate ID: 2 - SagaExecution

Finding questions by topic IDs: [5, 6]
Loaded and registered read for aggregate ID: 8 - SagaQuestion
Loaded and registered read for aggregate ID: 9 - SagaQuestion
```

### Weak evidence

Some logs do not include enough detail.

Examples:

```text
Creating user
Creating tournament:
Creating course execution: CourseExecutionDto@5cfd84e9
Creating question: QuestionDto@2c542a2a
```

These show that something happened, but not enough exact structured key data.

### Handler log coverage

Quizzes command handlers often log domain IDs manually:

- `applications/quizzes/src/main/java/.../execution/messaging/ExecutionCommandHandler.java`
  - `Getting course execution by id: ...`
  - `Getting student by execution id and user id: ..., ...`
  - `Enrolling student: ... in course execution: ...`
- `.../tournament/messaging/TournamentCommandHandler.java`
  - many logs include `tournamentAggregateId`, `executionAggregateId`, user/topic IDs;
  - but `Creating tournament:` lacks details.
- `.../course/messaging/CourseCommandHandler.java`
  - course IDs, names, counts.
- `.../user/messaging/UserCommandHandler.java`
  - user IDs for reads/updates; create has no id.
- `.../question/messaging/QuestionCommandHandler.java`
  - some IDs, but DTO object logs are weak unless DTO `toString()` is implemented.
- `.../quiz/messaging/QuizCommandHandler.java`
  - quiz/execution IDs for several operations.

Conclusion:

> Developer-made logs are useful as supporting evidence, but their formats are inconsistent and often free-text. They are not reliable enough as the primary exact-key mechanism.

## Tracing infrastructure

### Trace manager

File:

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/TraceManager.java`

Uses:

- OpenTelemetry SDK.
- OTLP gRPC exporter.
- Endpoint from `OTEL_EXPORTER_OTLP_ENDPOINT`, defaulting to `http://localhost:4317`.
- Jaeger can receive traces via OTLP when `COLLECTOR_OTLP_ENABLED=true`.

Compose wiring:

- Root `docker-compose.yml` starts `jaegertracing/all-in-one` with OTLP ports `4317` and `4318`.
- `applications/quizzes/docker-compose.yml` passes `OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4317` to services and test container definitions.

### Current span hierarchy

Observed trace hierarchy:

```text
quizzes-root1
  createCourseExecution::0
    getCourseStep
    createCourseStep
    createCourseExecutionStep
    updateCourseExecutionCountStep
  createUser::0
    createUserStep
  activateUser::0
    getUserStep
    activateUserStep
  ...
  CreateTournamentFunctionalitySagas::0
    getTopicsStep
    getCourseExecutionStep
    findQuestionsByTopicIdsStep
    getCreatorStep
    getCourseExecutionById
    generateQuizStep
  [Compensate]CreateTournamentFunctionalitySagas::0
```

Current span attributes:

- Root span:
  - `root=root`
  - `service=quizzes`
- Functionality span:
  - `func.name=<functionality>::<counter>`
  - `functionality=<functionality>`
  - `behaviour=<csv behavior map>`
  - `hasBehaviour=true|false`
- Step span:
  - `step.name=<step>`
  - `functionality=<functionality>`
- Delay span:
  - `functionality`
  - `step`
  - `value=<delay> ms`

Exception/warning events are attached to functionality spans. Example observed warning:

```text
warning: SimulatorException / Fault on createTournamentStep
```

Important limitation:

> Spans currently do not include command type, command fields, root aggregate id, DTO fields, aggregate load ids, or source/static anchors.

So traces are good for reconstructing workflow/step timing and fault behavior, but not enough for exact aggregate key binding unless enriched.

## Tests and trace usage

Base test:

- `applications/quizzes/src/test/groovy/.../QuizzesSpockTest.groovy`

Useful helper methods:

- `createCourseExecution(...)`
- `createUser(...)`
- `createTopic(...)`
- `createQuestion(...)`
- `createTournament(...)`
- `loadBehaviorScripts()`
- `sagaStateOf(aggregateId)`

Behavior tests:

- `applications/quizzes/src/test/groovy/.../sagas/behaviour/*`

Several behavior tests explicitly use:

```groovy
traceService.startRootSpan()
...
traceService.endRootSpan()
traceService.spanFlush()
```

Examples:

- `CreateTournamentFaultTest.groovy`
- `CreateTournamentRecoveryTest.groovy`
- `AddParticipantAndRecoverTest.groovy`
- `HandleEventBehaviour.groovy`

Observation:

> The existing behavior-test style is already compatible with a dynamic evidence collection phase. We can run selected tests under an observation profile and collect traces/logs/evidence.

## Simulator hook points

### 1. Command dispatch

Files:

- `simulator/src/main/java/.../messaging/Command.java`
- `simulator/src/main/java/.../messaging/local/LocalCommandGateway.java`
- stream/gRPC gateway/service variants under `simulator/src/main/java/.../messaging/`

`Command` has:

```java
private Integer rootAggregateId;
private UnitOfWork unitOfWork;
private String serviceName;
```

Many Quizzes command constructors call:

```java
super(unitOfWork, serviceName, someAggregateId);
```

or:

```java
super(unitOfWork, serviceName, null);
```

Examples:

- `EnrollStudentCommand`: root aggregate is `courseExecutionAggregateId`.
- `CancelTournamentCommand`: root aggregate is `tournamentAggregateId`.
- `UpdateTournamentCommand`: root aggregate is `tournamentDto.getAggregateId()`.
- `CreateTournamentCommand`: root aggregate is `null` because it creates a new aggregate.

This is a very strong existing abstraction.

Potential instrumentation:

```text
COMMAND_SENT event:
  command class
  serviceName
  rootAggregateId
  unitOfWork.functionalityName
  unitOfWork.version/id
  current step span if available
  reflected command scalar fields / DTO aggregateId fields
```

Why good:

- One library-level hook can cover many commands.
- Static analysis can already inspect command constructors and `super(...)` calls.
- Command root aggregate often gives the exact instance key for update/read commands.

Limitation:

- Create commands may have `rootAggregateId=null` until the service creates the aggregate.
- Some commands carry secondary IDs that matter for logical conflicts, e.g. student/user/topic IDs.

### 2. Unit-of-work aggregate load/read/write

File:

- `simulator/src/main/java/.../transactional/sagas/unitOfWork/SagaUnitOfWorkService.java`

Existing useful log:

```java
logger.info("Loaded and registered read for aggregate ID: {} - {}", aggregateId, aggregate.getAggregateType());
```

Potential instrumentation:

```text
AGGREGATE_ACCESSED event:
  access=READ/WRITE/COMMIT/ABORT
  aggregateId
  aggregateType
  unitOfWork.functionalityName
  unitOfWork.version
  current step/functionality span
```

Why good:

- This observes actual runtime aggregate instances.
- It works even when command logs omit IDs.
- It is closer to the thing the scenario catalog needs: `StepFootprint.aggregateKey.keyText`.

Limitation:

- `registerChanged(Aggregate aggregate, ...)` currently does not log aggregate id/type. It can be instrumented.
- Need to associate aggregate access with the correct step. Existing `TraceManager` can find current step by functionality/step if enriched, but direct API for current active step is weak.

### 3. Workflow/step execution

Files:

- `simulator/src/main/java/.../coordination/Workflow.java`
- `simulator/src/main/java/.../coordination/ExecutionPlan.java`

Existing:

- Starts functionality spans.
- Starts step spans.
- Logs start/end of functionality and steps.
- Applies behavior/fault/delay CSVs.

Potential instrumentation:

```text
STEP_STARTED / STEP_FINISHED event:
  functionality invocation id
  functionality name
  step name
  step order/static key if available
  unitOfWork version/id
```

Why good:

- Gives correlation context for command/aggregate events.
- Already exists as trace spans; we mostly need richer attributes/events.

Limitation:

- Current step correlation uses queues keyed by functionality name and step name; concurrent same-functionality invocations may be ambiguous.

### 4. Test helper factories

File:

- `applications/quizzes/src/test/groovy/.../QuizzesSpockTest.groovy`

Existing helpers construct DTOs and call functionalities. They return DTOs with aggregate IDs after creation.

Example:

```groovy
def createTournament(...) {
    def tournamentDto = new TournamentDto()
    ...
    tournamentDto = tournamentFunctionalities.createTournament(..., tournamentDto)
    tournamentDto
}
```

Potential instrumentation:

```text
TEST_HELPER_RESULT event:
  helper=createTournament
  returned dto type=TournamentDto
  returned aggregateId=...
  arguments: userCreatorId, courseExecutionId, topicIds, etc.
```

Why good:

- Very useful for mapping fixture setup values.
- Low-cost for Quizzes.

Limitation:

- Application/test-specific, less generic than simulator hooks.
- Better as a Quizzes smoke aid than the core thesis mechanism.

### 5. Logs/traces parser

Existing artifacts:

- `applications/quizzes/logs/app-test-*.log`
- Jaeger API/UI.

Potential parser:

- Parse JSON logs for known logger names/message regexes.
- Query Jaeger for traces by service and extract spans/events/attributes.

Why useful:

- Non-invasive.
- Good for diagnostics and impact analysis.

Limitation:

- Free-text logs are brittle.
- No trace/span ID in logs today, so log-to-span joining is by timestamp/thread/functionality/step patterns, not a strong correlation key.

## Static anchors already available or promising

Useful anchors from static analysis and code conventions:

1. Groovy test class/method/source expression from `GroovyFullTraceResult`.
2. Saga/functionality FQN and constructor argument summaries.
3. Workflow step names from static saga extraction.
4. Command construction sites inside saga steps.
5. Command class and constructor `super(unitOfWork, serviceName, rootAggregateId)` argument.
6. Command getter names used by handlers/services.
7. Unit-of-work aggregate load/register calls.
8. Log statement source class/method and message templates.

The most promising bridge is:

```text
static step -> command construction -> command rootAggregateId/fields -> runtime COMMAND_SENT event -> runtime AGGREGATE_ACCESSED event
```

## Assessment of possible approaches

### Logs only

Not recommended as the primary mechanism.

Good:

- Already available.
- JSON logs are easy to read.
- Some messages contain exactly the IDs we want.

Bad:

- IDs are in free-text `message`.
- Many messages are incomplete or object identity strings.
- Logs lack trace/span IDs.
- Message formats are developer-controlled and inconsistent.

Use for:

- diagnostics;
- fallback evidence;
- impact-analysis groundwork;
- validating structured evidence.

### Existing Jaeger traces only

Not enough for exact key binding today.

Good:

- Clean hierarchy: root -> functionality -> step.
- Captures behavior/fault metadata.
- Captures warnings/exceptions.

Bad:

- No command/DTO/aggregate IDs as attributes.
- No source/static anchors.
- No command payload fields.

Use for:

- step/functionality correlation;
- execution timing/order;
- fault/compensation diagnostics;
- later impact analysis.

### Structured library instrumentation

Recommended primary path.

Why:

- Simulator already has natural generic hook points.
- It can produce machine-readable evidence without parsing free text.
- It can be run under a test/profile flag.
- It keeps the thesis application-agnostic enough.

Best initial hooks:

1. `CommandGateway.send(Command)` / transport gateways:
   - command class;
   - service name;
   - root aggregate id;
   - unit-of-work functionality/version;
   - reflected scalar fields and DTO aggregate IDs.
2. `SagaUnitOfWorkService.aggregateLoadAndRegisterRead(...)`:
   - aggregate id/type;
   - read access.
3. `SagaUnitOfWorkService.registerChanged(...)`:
   - aggregate id/type;
   - write access.
4. `ExecutionPlan` step boundaries:
   - stable functionality/step correlation id.

## Recommended evidence format

Keep runtime evidence separate initially:

- `dynamic-evidence.jsonl`
- `dynamic-evidence-manifest.json`

Example event:

```json
{
  "eventKind": "COMMAND_SENT",
  "testClass": "pt...CreateTournamentFaultTest",
  "testMethod": "...",
  "functionality": "CreateTournamentFunctionalitySagas",
  "stepName": "getCourseExecutionStep",
  "commandType": "GetCourseExecutionByIdCommand",
  "serviceName": "execution",
  "rootAggregateId": "2",
  "fields": {
    "executionAggregateId": "2"
  },
  "source": "runtime-observer"
}
```

Example aggregate event:

```json
{
  "eventKind": "AGGREGATE_ACCESSED",
  "functionality": "CreateTournamentFunctionalitySagas",
  "stepName": "getCourseExecutionStep",
  "accessMode": "READ",
  "aggregateType": "SagaExecution",
  "aggregateId": "2",
  "source": "SagaUnitOfWorkService.aggregateLoadAndRegisterRead"
}
```

Then the join stage can infer:

```text
StepFootprint(CreateTournamentFunctionalitySagas::getCourseExecutionStep)
  aggregate = SagaExecution
  keyText = 2
  confidence = runtime-exact
```

## Practical recommendation

Proceed with Option D, but with a narrower interpretation:

1. Do not rely on developer logs as the source of truth.
2. Reuse existing traces for workflow/step context.
3. Add structured dynamic evidence at simulator-level hooks.
4. Parse existing JSON logs as auxiliary validation and diagnostics.
5. Later, add trace/span attributes or events for command/aggregate IDs if useful for Jaeger inspection.

Concrete next plan:

1. Add a read-only parser prototype over existing JSON logs to quantify how much can be recovered without instrumentation.
2. Design dynamic evidence records and manifest.
3. Add a dummyapp fixture proving that runtime evidence can resolve a key missed by static analysis.
4. Add test/profile-gated structured observer hooks in simulator:
   - command sent;
   - aggregate read;
   - aggregate write;
   - step start/end correlation.
5. Run `CreateTournamentFaultTest` or a smaller Quizzes coordination test and compare static-only vs dynamic-enriched catalog.

## Bottom line

The existing observability is already valuable, especially for workflow/step ordering and some aggregate IDs. But it is not currently structured or correlated enough to solve exact aggregate-key binding automatically.

The best path is not "parse whatever logs exist" and not "build a pure Java/Groovy dataflow engine". The best path is:

> static analysis selects targets; structured runtime observation records command/aggregate facts; logs and Jaeger traces provide supporting context and diagnostics.
