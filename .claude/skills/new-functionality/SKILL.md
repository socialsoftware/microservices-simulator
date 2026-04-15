---
name: new-functionality
description: Implement a new cross-service operation in the microservices-simulator (command, Sagas functionality, TCC stub, command handler, controller, tests). Arguments: "<FunctionalityName> <PrimaryAggregate> [other aggregates...]"
argument-hint: "<FunctionalityName> <PrimaryAggregate> [OtherAggregate...]"
---

# Implement New Functionality: $ARGUMENTS

You are adding a new cross-service operation to the application currently being built on the simulator.

> **Sagas only.** The Sagas workflow is the authoritative implementation. The TCC class is an empty stub. See `docs/concepts/tcc-placeholder-pattern.md`.

---

## Step 0 — Parse arguments

From `$ARGUMENTS` identify:
- **FunctionalityName**: PascalCase name (e.g., `AddParticipant`, `UpdateStudentName`)
- **PrimaryAggregate**: the aggregate that owns this operation
- **OtherAggregates**: other aggregates read or written by this functionality
- **Steps**: what commands are sent in sequence, and which steps depend on prior results
- **Forbidden states**: which saga states on which aggregates block this operation
- **Return value**: what DTO (if any) is returned

Clarify before writing any code.

---

## Step 1 — Read existing templates

Before writing anything, read:
1. `microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` — multi-step workflow with forbidden states and shared state between steps
2. `microservices/execution/coordination/sagas/UpdateCourseQuestionCountFunctionalitySagas.java` (or similar) — simple single-step workflow
3. `microservices/<primaryAggregate>/coordination/` — existing functionalities in the same package
4. `command/<primaryAggregate>/` — existing command classes

Also read `docs/concepts/sagas.md` for the Sagas concurrency protocol.

---

## Step 2 — Create command class(es)

For each step that sends a command, create a command in `command/<aggregate>/<Xxx>Command.java`:

```java
public class <Xxx>Command extends Command {
    private final Integer targetAggregateId;
    // payload fields

    public <Xxx>Command(UnitOfWork unitOfWork, String serviceId, Integer targetAggregateId, ...) {
        super(unitOfWork, serviceId);
        this.targetAggregateId = targetAggregateId;
    }
}
```

Read-only steps return a DTO; write steps return void or a DTO.

---

## Step 3 — Implement: Sagas functionality

File: `microservices/<primaryAggregate>/coordination/sagas/<FunctionalityName>FunctionalitySagas.java`

```java
public class <FunctionalityName>FunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private SomeDto intermediateResult;  // shared between steps if needed

    public <FunctionalityName>FunctionalitySagas(SagaUnitOfWorkService uows, ..., SagaUnitOfWork uow, CommandGateway gw) {
        this.unitOfWorkService = uows;
        this.commandGateway = gw;
        this.buildWorkflow(..., uow);
    }

    public void buildWorkflow(..., SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep step1 = new SagaStep("step1Name", () -> {
            GetXxxCommand cmd = new GetXxxCommand(unitOfWork, ServiceMapping.XXX.getServiceName(), id);
            this.intermediateResult = (SomeDto) commandGateway.send(cmd);
        });

        SagaStep step2 = new SagaStep("step2Name", () -> {
            // declare semantic lock if needed
            List<SagaAggregate.SagaState> forbidden = new ArrayList<>();
            forbidden.add(TargetSagaState.IN_CONFLICTING_OP);

            XxxCommand cmd = new XxxCommand(unitOfWork, ServiceMapping.TARGET.getServiceName(), targetId, this.intermediateResult);
            cmd.setForbiddenStates(forbidden);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(step1)));

        this.workflow.addStep(step1);
        this.workflow.addStep(step2);
    }
}
```

Key rules:
- Each `SagaStep` takes a name (used in `executeUntilStep` for tests), a lambda, and optionally a list of dependencies
- `setForbiddenStates(...)` declares which saga states would block this step
- Intermediate results shared across steps are stored as fields on the functionality class

---

## Step 4 — Implement: TCC stub

File: `microservices/<primaryAggregate>/coordination/causal/<FunctionalityName>FunctionalityTCC.java`

This is a **stub only**. Do not implement real workflow logic:

```java
public class <FunctionalityName>FunctionalityTCC extends WorkflowFunctionality {

    public <FunctionalityName>FunctionalityTCC(CausalUnitOfWorkService uows, ..., CausalUnitOfWork uow, CommandGateway gw) {
        // TCC not implemented
    }

    @Override
    public void buildWorkflow(...) {
        // TCC not implemented
    }
}
```

---

## Step 5 — Wire entry point in Functionalities bean

In `microservices/<primaryAggregate>/coordination/functionalities/<Primary>Functionalities.java`:

```java
public <ReturnDto> <functionalityName>(Integer primaryAggregateId, ...) {
    String name = "<FunctionalityName>";

    switch (workflowType) {
        case SAGAS:
            SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(name);
            <FunctionalityName>FunctionalitySagas f = new <FunctionalityName>FunctionalitySagas(
                    sagaUnitOfWorkService, primaryAggregateId, ..., uow, commandGateway);
            f.executeWorkflow(uow);
            sagaUnitOfWorkService.commit(uow);
            return f.getResult();
        case TCC:
            throw new UnsupportedOperationException("<FunctionalityName>: TCC not implemented. Run with -Ptest-sagas.");
        default:
            throw new IllegalStateException("Unknown workflow type");
    }
}
```

---

## Step 6 — Implement: Command handler

In the local command handler for the primary aggregate, add routing for each new command:

```java
if (command instanceof <Xxx>Command) {
    <Xxx>Command cmd = (<Xxx>Command) command;
    return service.<xxx>(cmd.getTargetAggregateId(), ..., cmd.getUnitOfWork());
}
```

Register the command handler in `LocalCommandGateway` if not already picked up.

---

## Step 7 — Implement the service method(s)

In `<Primary>Service.java`, add one method per command type. Apply operations in this order:

### Read-only method (for Get*Command steps)

```java
public <Primary>Dto get<Primary>(Integer aggregateId, UnitOfWork unitOfWork) {
    return new <Primary>Dto(aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
}
```

Always use `aggregateLoadAndRegisterRead` — never call the repository directly from a service method.

### Mutating method (for mutation command steps)

```java
public <ReturnDto> <xxx>(Integer aggregateId, <Input>Dto inputDto, UnitOfWork unitOfWork) {
    // 1. [Layer 2 guard] Precondition check — before touching the aggregate.
    //    Reads own aggregate table; throws <App>Exception if violated.
    //    See /service-guard for the full pattern.

    // 2. Load existing aggregate (registers for UoW read tracking)
    <Primary> old = aggregateLoadAndRegisterRead(aggregateId, unitOfWork);

    // 3. Copy for mutation (creates the new version row — mutate the copy, never old)
    <Primary> next = factory.copy(old);

    // 4. Apply mutations — data from prior functionality steps arrives as method args
    next.set<Field>(inputDto.get<Field>());

    // 5. Register for commit (verifyInvariants() is called automatically at UoW commit time)
    unitOfWorkService.registerChanged(next, unitOfWork);

    return new <Primary>Dto(next);
}
```

**Ordering rules:**
- Layer 2 guards go **before** loading or copying the aggregate — abort before any state is dirtied.
- Mutate the **copy** (`next`), never `old`.
- Data fetched from other aggregates in prior steps arrives as method parameters — do **not** call `aggregateLoadAndRegisterRead` for foreign aggregate IDs (R1 violation).
- Do **not** inject another aggregate's repository to fetch its data — that is an R2 violation. Foreign aggregate data must arrive as method parameters from prior `Get*Command` steps.
- `verifyInvariants()` is called at commit time by the UoW; an explicit call before `registerChanged` is optional but useful for catching Layer 1 violations early.

> **Reference:** `applications/quizzes/...execution/service/ExecutionService.java` — guard + load/copy pattern; `applications/quizzes/...tournament/service/TournamentService.java` — embedding data from prior steps.

---

## Step 8 — REST controller (if required)

File: `microservices/<primaryAggregate>/coordination/webapi/<Primary>Controller.java`

```java
@PostMapping("/{aggregateId}/<operation>")
public ResponseEntity<XxxDto> <operation>(@PathVariable Integer aggregateId, ...) {
    return ResponseEntity.ok(functionalities.<functionalityName>(aggregateId, ...));
}
```

---

## Step 9 — Write tests

File: `src/test/groovy/.../sagas/coordination/<primaryAggregate>/<FunctionalityName>Test.groovy`

Cover:
- Happy path
- Any invariant violation that blocks the operation
- Concurrent interleaving using `executeUntilStep` + `resumeWorkflow`:

```groovy
def "concurrent interleaving"() {
    given:
    def f1 = new <FunctionalityName>FunctionalitySagas(...)
    def f2 = new OtherFunctionalitySagas(...)

    when:
    f1.executeUntilStep("step1Name", uow1)
    f2.executeWorkflow(uow2)   // f2 completes
    f1.resumeWorkflow(uow1)    // f1 tries to continue

    then:
    thrown(<App>Exception)   // or assert outcome
}
```

Run:
```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<FunctionalityName>Test
```

---

## Checklist

- [ ] Command class(es) created for each step
- [ ] `<FunctionalityName>FunctionalitySagas.java` with correct step dependencies and forbidden states
- [ ] `<FunctionalityName>FunctionalityTCC.java` — stub with empty `buildWorkflow()` body
- [ ] Entry point method added to `<Primary>Functionalities.java` (Sagas case wired; TCC case throws)
- [ ] Service method(s) implemented and calling `verifyInvariants()`
- [ ] Command handler routes new commands
- [ ] REST controller endpoint (if required)
- [ ] Tests: happy path, invariant violations, concurrent interleaving
- [ ] Tests passing: `mvn clean -Ptest-sagas test`
