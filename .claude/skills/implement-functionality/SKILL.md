---
name: implement-functionality
description: Phase 3 driver — fully implement one cross-service functionality: commands, Sagas workflow, TCC stub, command handler, service methods, controller, Layer 3 forbidden states, test. Reads all details from plan.md. Arguments: "<FunctionalityName>"
argument-hint: "<FunctionalityName>"
---

# Implement Functionality: $ARGUMENTS

You are completing all Phase 3 work for the functionality named in `$ARGUMENTS`. Do all steps
in order. Tick the checkbox in `plan.md` after the test passes. Do not stop early.

> **Sagas only.** TCC classes are empty stubs. See `docs/concepts/tcc-placeholder-pattern.md`.

---

## Step 0 — Gather context

Read before writing any code:

1. `plan.md` — find the `### <FunctionalityName>` entry under Phase 3. Extract:
   - The `/implement-functionality` invocation line (primary aggregate + other aggregates)
   - Layer 3 sub-bullets (which saga step each rule attaches to, what it checks)
   - Any Layer 2 guard checkbox (applied in Step 6b below)
2. The domain model template (`*-domain-model.md`) — §4 entry for this functionality.
   Authoritative source for what the operation does, its inputs, and business rules.
3. Read existing reference implementations before writing anything:
   - `applications/quizzes/microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` — multi-step workflow with forbidden states and shared state between steps
   - `applications/quizzes/microservices/<primaryAggregate>/coordination/` — existing functionalities in the same package
   - `applications/quizzes/command/<primaryAggregate>/` — existing command classes
   - `docs/concepts/sagas.md` — Sagas concurrency protocol

---

## Step 1 — Create command class(es)

For each step that sends a command, create `command/<aggregate>/<Xxx>Command.java`:

```java
public class <Xxx>Command extends Command {
    private final Integer targetAggregateId;
    // payload fields

    public <Xxx>Command(UnitOfWork unitOfWork, String serviceId, Integer targetAggregateId, ...) {
        super(unitOfWork, serviceId);
        this.targetAggregateId = targetAggregateId;
    }
    // getters
}
```

Read-only steps return a DTO; mutation steps return void or a DTO.

---

## Step 2 — Implement: Sagas functionality

File: `microservices/<primaryAggregate>/coordination/sagas/<FunctionalityName>FunctionalitySagas.java`

```java
public class <FunctionalityName>FunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private SomeDto intermediateResult;  // shared between steps if needed

    public <FunctionalityName>FunctionalitySagas(SagaUnitOfWorkService uows, ...,
            SagaUnitOfWork uow, CommandGateway gw) {
        this.unitOfWorkService = uows;
        this.commandGateway = gw;
        this.buildWorkflow(..., uow);
    }

    public void buildWorkflow(..., SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep step1 = new SagaStep("step1Name", () -> {
            GetXxxCommand cmd = new GetXxxCommand(unitOfWork,
                    ServiceMapping.XXX.getServiceName(), id);
            this.intermediateResult = (SomeDto) commandGateway.send(cmd);
        });

        SagaStep step2 = new SagaStep("step2Name", () -> {
            XxxCommand cmd = new XxxCommand(unitOfWork,
                    ServiceMapping.TARGET.getServiceName(), targetId, this.intermediateResult);
            // Layer 3 forbidden states are wired in Step 3 below (not here)
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(step1)));

        this.workflow.addStep(step1);
        this.workflow.addStep(step2);
    }
}
```

Key rules:
- Each `SagaStep` takes a name, a lambda, and optionally a dependency list
- Intermediate results shared across steps are stored as fields on the functionality class
- `setForbiddenStates(...)` is added in Step 3 (after the class exists)

---

## Step 3 — Implement: TCC stub

File: `microservices/<primaryAggregate>/coordination/causal/<FunctionalityName>FunctionalityTCC.java`

```java
public class <FunctionalityName>FunctionalityTCC extends WorkflowFunctionality {
    public <FunctionalityName>FunctionalityTCC(...) { /* TCC not implemented */ }

    @Override
    public void buildWorkflow(...) { /* TCC not implemented */ }
}
```

---

## Step 4 — Wire entry point in Functionalities

In `microservices/<primaryAggregate>/coordination/functionalities/<Primary>Functionalities.java`:

```java
public <ReturnDto> <functionalityName>(Integer primaryId, ...) {
    String name = "<FunctionalityName>";
    switch (workflowType) {
        case SAGAS:
            SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(name);
            <FunctionalityName>FunctionalitySagas f = new <FunctionalityName>FunctionalitySagas(
                    sagaUnitOfWorkService, primaryId, ..., uow, commandGateway);
            f.executeWorkflow(uow);
            sagaUnitOfWorkService.commit(uow);
            return f.getResult();
        case TCC:
            throw new UnsupportedOperationException("<FunctionalityName>: TCC not implemented.");
        default:
            throw new IllegalStateException("Unknown workflow type");
    }
}
```

---

## Step 5 — Command handler

In the command handler for the primary aggregate, add routing for each new command:

```java
if (command instanceof <Xxx>Command) {
    <Xxx>Command cmd = (<Xxx>Command) command;
    return service.<xxx>(cmd.getTargetAggregateId(), ..., cmd.getUnitOfWork());
}
```

---

## Step 6 — Service method(s)

In `<Primary>Service.java`, add one method per command type.

**Read-only method** (for Get*Command steps):
```java
public <Primary>Dto get<Primary>(Integer aggregateId, UnitOfWork unitOfWork) {
    return new <Primary>Dto(aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
}
```

**Mutating method** (for mutation command steps):
```java
public <ReturnDto> <xxx>(Integer aggregateId, <Input>Dto inputDto, UnitOfWork unitOfWork) {
    // 1. [Layer 2 guard] precondition check — added in Step 6b if listed in plan.md
    // 2. Load existing aggregate
    <Primary> old = aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
    // 3. Copy for mutation
    <Primary> next = factory.copy(old);
    // 4. Apply mutations
    next.set<Field>(inputDto.get<Field>());
    // 5. Register for commit (verifyInvariants called automatically at commit time)
    unitOfWorkService.registerChanged(next, unitOfWork);
    return new <Primary>Dto(next);
}
```

> **R1/R2**: Only call `aggregateLoadAndRegisterRead` for this aggregate's own IDs. Foreign
> aggregate data must arrive as method parameters from prior Get*Command steps — never load
> foreign aggregates directly from this service.

---

## Step 6b — Layer 2 guard (if listed in plan.md)

If the plan.md entry for this functionality has a `- [ ] Layer 2 guard applied: <GUARD_NAME>` line,
apply the guard **now**, before moving on.

1. Read the service method you just wrote in Step 6 and the existing service class for guard patterns.
2. Add the guard check **before** any aggregate mutation, with an inline label comment:
   ```java
   // GUARD_NAME — <one-line description of what is being checked>
   if (<precondition violated>) {
       throw new <App>Exception(<ERROR_CONSTANT>, <relevant ids...>);
   }
   ```
3. If the check requires a DB query not already available, add the method signature to the custom
   repository interface and implement it in both the Sagas and TCC repository classes.
4. Add the error constant to `<App>ErrorMessage.java` if it does not already exist.
5. Add guard-violation test cases to the test you will write in Step 9.
6. Tick the guard checkbox in `plan.md`:
   - [x] Layer 2 guard applied: <GUARD_NAME>

> The guard must only read the service's **own aggregate type**. If a foreign aggregate must be
> read to evaluate the precondition, use a Layer 3 saga step instead.

---

## Step 7 — REST controller (if required)

File: `microservices/<primaryAggregate>/coordination/webapi/<Primary>Controller.java`

```java
@PostMapping("/{aggregateId}/<operation>")
public ResponseEntity<XxxDto> <operation>(@PathVariable Integer aggregateId, ...) {
    return ResponseEntity.ok(functionalities.<functionalityName>(aggregateId, ...));
}
```

---

## Step 8 — Wire Layer 3 forbidden states

For each Layer 3 sub-bullet from plan.md:
```
Layer 3 — step "<StepName>": <RULE_NAME> (reads <Aggregate>.<field>)
```

Open `<FunctionalityName>FunctionalitySagas.java` and add `setForbiddenStates(...)` on the
named step, following the `AddParticipantFunctionalitySagas` pattern:

```java
SagaStep step = workflow.extractStep("<StepName>");
step.setForbiddenStates(List.of(<Aggregate>SagaState.IN_CONFLICTING_OP /*, ... */));
```

If there are no Layer 3 rules for this functionality, skip.

---

## Step 9 — Run the test

Write a T2 test at `src/test/groovy/.../sagas/coordination/<primaryAggregate>/<FunctionalityName>Test.groovy`.
Follow the T2 template in `docs/concepts/testing.md`. Cover:

1. **Happy path** — workflow completes, result is correct
2. **Invariant/guard violations** — one `thrown()` case per Layer 1 / Layer 2 rule
3. **Step-interleaving** — for each saga step that declares `setForbiddenStates` (Layer 3),
   add one case using `executeUntilStep("<stepName>", uow)` + `resumeWorkflow(uow)` where a
   conflicting operation locks the foreign aggregate between steps. Do not cover just one step
   when there are multiple; one interleaving case per guarded step boundary.

Then run:
```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<FunctionalityName>Test
```

Diagnose and fix any failures before ticking. Do not move on until the test is green.

---

## Step 10 — Tick plan.md

- [x] **<FunctionalityName>** — `/implement-functionality <FunctionalityName> ...`

---

## Done

Report:
- Files created: list command class(es), FunctionalitySagas, FunctionalityTCC, entry point method, service method(s), controller method (if any)
- Layer 3 rules wired (or "none")
- Test: green
