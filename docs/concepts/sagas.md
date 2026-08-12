# Sagas

## What They Are

Sagas implement **semantic locking** to prevent conflicting concurrent operations. Each saga aggregate holds a `SagaState` that steps can check or set. Conflicting operations declare which states they must not find the aggregate in; if the aggregate is already in a forbidden state, the operation aborts.

## Key Classes

| Class | Location | Role |
|-------|---------|------|
| `SagaAggregate` | `simulator/.../ms/transaction/sagas/aggregate/SagaAggregate.java` | Interface adding `getSagaState()` / `setSagaState()` |
| `SagaUnitOfWork` | `simulator/.../ms/transaction/sagas/unitOfWork/SagaUnitOfWork.java` | Coordinates reads/writes for a saga execution |
| `SagaUnitOfWorkService` | `simulator/.../ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` | Creates and commits/aborts `SagaUnitOfWork` instances |
| `SagaWorkflow` | `simulator/.../ms/transaction/sagas/workflow/SagaWorkflow.java` | Workflow engine for sagas |
| `SagaStep` | `simulator/.../ms/transaction/sagas/workflow/SagaStep.java` | A single step in a saga workflow |

## SagaState

Each saga aggregate has a corresponding `XxxSagaState` enum located at `microservices/<aggregate>/aggregate/sagas/states/XxxSagaState.java`.

States encode the semantic lock meaning:
```java
public enum WarehouseSagaState implements SagaAggregate.SagaState {
    IN_ADD_SHIPMENT("IN_ADD_SHIPMENT"),
    IN_UPDATE_WAREHOUSE("IN_UPDATE_WAREHOUSE");

    private final String stateName;
    WarehouseSagaState(String stateName) { this.stateName = stateName; }

    @Override
    public String getStateName() { return stateName; }
}
```

The quiescent state is `GenericSagaState.NOT_IN_SAGA`, supplied by the framework and set in the
`Saga{Aggregate}` constructor - never redeclare it in the per-aggregate enum. A second constant of
that name is not identity-equal to the framework's, which breaks the
`sagaStateOf(id) == GenericSagaState.NOT_IN_SAGA` assertions required by
[`testing.md`](testing.md) § T4 and § Compensation Test. This enum holds operation-specific locked
states only.

## Lock-Acquisition Step Pattern (Two-Step Write Sagas)

Write sagas that must lock the primary aggregate before mutating it use a two-step pattern:

- **Step 1 — read + lock**: wrap the read command in `SagaCommand`, then call `setSemanticLock(state)` to atomically check the current state and transition to the new one.
- **Step 2 — mutate**: send a plain (unwrapped) command that performs the mutation; declare step 1 as a dependency.

```java
// Step 1: acquire lock
SagaStep getWarehouseStep = new SagaStep("getWarehouseStep", () -> {
    GetWarehouseByIdCommand readCmd = new GetWarehouseByIdCommand(
            unitOfWork, ServiceMapping.WAREHOUSE.getServiceName(), warehouseAggregateId);
    SagaCommand sagaCommand = new SagaCommand(readCmd);
    sagaCommand.setSemanticLock(WarehouseSagaState.IN_UPDATE_WAREHOUSE);
    this.warehouseDto = (WarehouseDto) commandGateway.send(sagaCommand);
});

// Step 2: mutate (plain command, no SagaCommand wrapper)
SagaStep updateWarehouseStep = new SagaStep("updateWarehouseStep", () -> {
    UpdateWarehouseCommand cmd = new UpdateWarehouseCommand(
            unitOfWork, ServiceMapping.WAREHOUSE.getServiceName(), warehouseAggregateId, name);
    commandGateway.send(cmd);
}, new ArrayList<>(Arrays.asList(getWarehouseStep)));
```

Contrast with plain `setForbiddenStates(...)` (shown below): that checks whether an existing lock blocks this operation, but does **not** set a new lock. `SagaCommand` + `setSemanticLock(...)` both checks and transitions atomically.

## Semantic-lock release on abort is automatic

**Application workflows must NOT manually release semantic locks.** The core owns the entire lock lifecycle - acquire, release-on-abort, and release-on-commit - and a manually-registered release compensation both duplicates that work and corrupts the abort ledger.

How the automatic release works:

- When a step runs `setSemanticLock`, the core (`SagaCommandHandler` → `registerSagaState()`) records the aggregate's **pre-lock** `SagaState`, keyed by the currently-executing step, via `savePreviousState(aggId, oldState)`, then applies the lock atomically. For a first-time lock, that recorded pre-lock state **is** `NOT_IN_SAGA`.
- On abort, `SagaUnitOfWorkService.abortUntilStep` walks the executed steps in reverse and replays each recorded previous state through an `AbortSagaCommand`, restoring every aggregate to its pre-lock state. For the lock step, that restores `NOT_IN_SAGA` - so the automatic path alone releases every lock. The successful-commit path likewise resets `SagaState` back to `NOT_IN_SAGA`.

`registerCompensation` is reserved for **genuine domain-level compensating actions** - undoing a real side effect a step generated, e.g. deleting a child aggregate a step created. It is never used to release a lock.

> **The `currentExecutingStep` re-lock pitfall.** A manual "release the lock" compensation on the lock-acquiring step does not just no-op - it actively re-locks the aggregate. When the injected fault throws before the faulting step's own `execute()` runs, `currentExecutingStep` stays pinned to the lock step. The manual release then re-enters `registerSagaState` → `savePreviousState` under that same frozen step key, appending a **spurious `(agg, LOCKED_STATE)`** record. On abort, `sendAbortCommandsForStep` replays both records in list order; the spurious `LOCKED` one is applied **last and wins**, leaving the aggregate locked. Empirically: re-adding one manual release block to a delete workflow makes its compensation test fail with the persisted `IN_DELETE_{AGGREGATE}` state instead of `NOT_IN_SAGA`. The compensation test's `sagaStateOf(...) == NOT_IN_SAGA` assertion is the regression guard for exactly this class of lock-lifecycle bug.

## Semantic Locks in Practice

A step acquires a lock by setting the saga state on the command:

```java
SagaStep addShipmentItemStep = new SagaStep("addShipmentItemStep", () -> {
    List<SagaAggregate.SagaState> states = new ArrayList<>();
    states.add(ShipmentSagaState.IN_UPDATE_SHIPMENT);

    AddShipmentItemCommand cmd = new AddShipmentItemCommand(...);
    SagaCommand sagaCommand = new SagaCommand(cmd);
    sagaCommand.setForbiddenStates(states);   // abort if the shipment is IN_UPDATE_SHIPMENT
    commandGateway.send(sagaCommand);
}, dependencies);
```

`setForbiddenStates(...)` causes the command handler to check the current saga state and throw if it matches any forbidden state.

**Both saga-state mechanisms live on `SagaCommand`, not on `Command`.** `setForbiddenStates` and
`setSemanticLock` are declared by
`simulator/.../ms/transaction/sagas/messaging/SagaCommand.java`, so a guarded step wraps its command
exactly as a lock step does — the difference is which setter it calls, not whether it wraps. Calling
`setForbiddenStates` on the plain command does not compile.

The parameter type is `List<SagaAggregate.SagaState>`, and Java's generics are invariant: a
`List.of({Aggregate}SagaState.IN_{OP})` infers `List<{Aggregate}SagaState>` and will not convert.
Declare the list at the interface type and add to it, as above.

## R4 Decision Table — `SagaCommand` vs `setForbiddenStates`

| Step target | Pattern | When to use |
|-------------|---------|-------------|
| **Primary aggregate** (the aggregate owning this saga) | `SagaCommand` wrapping the read command + `setSemanticLock(state)` | Lock acquisition before mutating the saga's own aggregate — see § Lock-Acquisition Step Pattern. The lock is released automatically on abort/commit — do **not** register a manual release compensation (see § Semantic-lock release on abort is automatic) |
| **Foreign aggregate** (upstream aggregate touched by a cross-aggregate step) | `SagaCommand` wrapping the command + `setForbiddenStates([...])` on it, listing states that must block this step — no lock is acquired | Abort if the foreign aggregate is already mid-saga in a conflicting state; does **not** acquire a new lock |
| **Newly created aggregate** (the step creates it) | Plain command, no lock and no forbidden states; register a compensation that removes it iff a later step follows | The aggregate does not exist when the saga starts, so there is no prior state to guard - see § Create Functionality Sagas |

**Rule of thumb:** if the step must **acquire** a lock on an aggregate before writing to it, use `SagaCommand` + `setSemanticLock`. If the step only needs to **check** that another aggregate is not already locked, use `setForbiddenStates`. If the step brings the aggregate into existence, neither applies.

## Read Functionality Sagas

Read sagas are a thin single-step wrapper. They have no compensation, no forbidden states, and no semantic lock acquisition — reads are non-mutating.

```java
public class Get{Aggregate}ByIdFunctionalitySagas extends WorkflowFunctionality {
    private {Aggregate}Dto {aggregate}Dto;

    public Get{Aggregate}ByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer {aggregate}AggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, {aggregate}AggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
            Integer {aggregate}AggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep get{Aggregate}Step = new SagaStep("get{Aggregate}Step", () -> {
            Get{Aggregate}ByIdCommand cmd = new Get{Aggregate}ByIdCommand(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), {aggregate}AggregateId);
            this.{aggregate}Dto = ({Aggregate}Dto) commandGateway.send(cmd);
        });

        this.workflow.addStep(get{Aggregate}Step);
    }

    public {Aggregate}Dto get{Aggregate}Dto() {
        return {aggregate}Dto;
    }
}
```

The coordinator method creates it inline and returns the DTO via the getter:

```java
Get{Aggregate}ByIdFunctionalitySagas saga = new Get{Aggregate}ByIdFunctionalitySagas(
        unitOfWorkService, aggregateId, unitOfWork, commandGateway);
saga.executeWorkflow(unitOfWork);
return saga.get{Aggregate}Dto();
```

### List-return read variant

When a read functionality returns multiple aggregates (e.g., all shipments for a warehouse), the result field is `List<{Aggregate}Dto>`, the cast is `(List<{Aggregate}Dto>)`, and the getter returns the list:

```java
public class Get{Aggregates}By{Field}FunctionalitySagas extends WorkflowFunctionality {
    private List<{Aggregate}Dto> {aggregates};
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public Get{Aggregates}By{Field}FunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer {field}Id,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow({field}Id, unitOfWork);
    }

    public void buildWorkflow(Integer {field}Id, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep get{Aggregates}Step = new SagaStep("get{Aggregates}Step", () -> {
            Get{Aggregates}By{Field}Command cmd = new Get{Aggregates}By{Field}Command(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), {field}Id);
            this.{aggregates} = (List<{Aggregate}Dto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(get{Aggregates}Step);
    }

    public List<{Aggregate}Dto> get{Aggregates}() {
        return {aggregates};
    }
}
```

The coordinator method returns the list via the getter:

```java
Get{Aggregates}By{Field}FunctionalitySagas saga = new Get{Aggregates}By{Field}FunctionalitySagas(
        unitOfWorkService, {field}Id, unitOfWork, commandGateway);
saga.executeWorkflow(unitOfWork);
return saga.get{Aggregates}();
```

### Two-step read saga variant

When a read functionality's filter parameter is a foreign aggregate's ID (e.g., `shipmentId`) that must be resolved to the primary aggregate's actual filter field (e.g., `warehouseAggregateId`), use a two-step saga:

- **Step 1** — fetch the foreign aggregate DTO (plain read, no lock, no compensation).
- **Step 2** — send the primary read command with the resolved field; declare step 1 as a dependency.

No compensation is needed on either step because reads are non-mutating.

```java
public class Get{Aggregates}By{ForeignField}FunctionalitySagas extends WorkflowFunctionality {
    private List<{Aggregate}Dto> {aggregates};
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public Get{Aggregates}By{ForeignField}FunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer {foreignField}Id,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow({foreignField}Id, unitOfWork);
    }

    public void buildWorkflow(Integer {foreignField}Id, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // Step 1: resolve the foreign aggregate to obtain the primary filter field
        final Integer[] resolvedFilterId = new Integer[1];
        SagaStep get{ForeignAggregate}Step = new SagaStep("get{ForeignAggregate}Step", () -> {
            Get{ForeignAggregate}ByIdCommand cmd = new Get{ForeignAggregate}ByIdCommand(
                    unitOfWork, ServiceMapping.{FOREIGN_AGGREGATE}.getServiceName(), {foreignField}Id);
            {ForeignAggregate}Dto dto = ({ForeignAggregate}Dto) commandGateway.send(cmd);
            resolvedFilterId[0] = dto.get{FilterField}();
        });

        // Step 2: fetch primary aggregates using the resolved filter
        SagaStep get{Aggregates}Step = new SagaStep("get{Aggregates}Step", () -> {
            Get{Aggregates}By{FilterField}Command cmd = new Get{Aggregates}By{FilterField}Command(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), resolvedFilterId[0]);
            this.{aggregates} = (List<{Aggregate}Dto>) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(get{ForeignAggregate}Step)));

        this.workflow.addStep(get{ForeignAggregate}Step);
        this.workflow.addStep(get{Aggregates}Step);
    }

    public List<{Aggregate}Dto> get{Aggregates}() {
        return {aggregates};
    }
}
```

The coordinator method returns the list via the getter (same as the list-return variant):

```java
Get{Aggregates}By{ForeignField}FunctionalitySagas saga = new Get{Aggregates}By{ForeignField}FunctionalitySagas(
        unitOfWorkService, {foreignField}Id, unitOfWork, commandGateway);
saga.executeWorkflow(unitOfWork);
return saga.get{Aggregates}();
```

## Create Functionality Sagas

A create saga has no aggregate to lock: the aggregate it writes does not exist when the saga starts,
and the service mints its `aggregateId` via `aggregateIdGeneratorService`. Three consequences:

1. **The create step declares no semantic lock and no `forbiddenStates`.** There is no prior state to
   guard, so there is nothing for `setSemanticLock` to transition from and nothing for
   `verifySagaState` to reject. Do not wrap the create command in `SagaCommand`, and do not invent a
   saga state for the create operation. The create command's `rootAggregateId` is `null` - see
   [`concepts/commands.md`](commands.md) § What a Command Is.

2. **The create step registers a compensation that removes the created aggregate** - unless the
   create is the saga's last step, in which case the unit of work handles abort on its own and no
   compensation is needed. This is the part that generalises badly if omitted: a single-step create
   saga looks correct without it, but a create saga with *any* step after the create leaks the
   created aggregate on abort. Register the compensation if and only if a later step exists.

3. **Data-assembly steps are unchanged.** Reads of other aggregates keep their normal treatment: a
   plain read command where the read only supplies data, or the get-then-lock pattern where the saga
   also mutates that aggregate. Only the create step itself is special.

### Field provenance — where the created aggregate's unsupplied values come from

When a saga creates a **second** aggregate as part of a larger functionality, the functionality's own
signature was written for the first one, so the second aggregate needs field values no caller
supplies. Where those come from is not free: it is a published-API decision, and leaving it to the
implementing agent produces a different answer per aggregate. State it, in this order:

1. **Derive it in the saga from parameters already present.** Preferred, and the default. If a field
   of the created aggregate is a function of arguments the functionality already takes or of a DTO an
   earlier step already fetched, compute it in the create step. The signature does not change.
2. **Use a domain sentinel for a genuine constant.** A field with no defensible derivation but one
   correct value for every aggregate created down this path takes a named constant, declared
   alongside the other domain constants, not a literal at the call site.
3. **Widen the functionality's signature only when neither fits** - when the value is a real caller
   choice the domain model gives no way to derive. This changes the published API, so it is the last
   resort, not the convenient one.

Do **not** discharge the problem by defaulting the field on the created aggregate itself. A default
inside `{Aggregate}` applies to every create path, including the direct one, and silently weakens
whatever P1 invariant the field participates in. Record the chosen provenance in the retro.

### The compensating delete — when the created aggregate ships no delete

Item 2 requires a removal compensation whenever a later step follows the create, and the Shape 2
snippet below assumes `Delete{Aggregate}Command` exists. It often does not: the created aggregate's
own session may have shipped no delete of any kind - no service method, no command, no handler case -
because nothing in that aggregate's own functionality list needed one.

**Then the saga's session writes it, by reopening the created aggregate's session `c`** for that one
method: `{Aggregate}Service.delete{Aggregate}`, `Delete{Aggregate}Command`, and its
`{Aggregate}CommandHandler` case, with the T2 coverage that session's conventions require. The
compensation is a step of the created aggregate's own contract, so it belongs in that aggregate's
service, not assembled inline in the saga.

Two alternatives are **forbidden**, however much cheaper they look:

- **Reordering the steps so the create is last**, to fall back on item 2's no-compensation branch.
  The order is fixed by the data dependency - a later step consumes the created aggregate's id or
  DTO - and where it is not, moving a create to the end to dodge writing a delete makes the step
  order an artifact of what was convenient to implement.
- **Skipping the compensation.** An abort then leaves the created aggregate behind with nothing
  referencing it, and no test in the aggregate's own suite can see it.

### Shape 1 — single-step create

The create is the only step, so abort is the unit of work's responsibility and no compensation is
registered.

```java
public class Create{Aggregate}FunctionalitySagas extends WorkflowFunctionality {
    private {Aggregate}Dto {aggregate}Dto;

    public Create{Aggregate}FunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            {Aggregate}Dto {aggregate}Dto,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, {aggregate}Dto, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
            {Aggregate}Dto {aggregate}Dto,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep create{Aggregate}Step = new SagaStep("create{Aggregate}Step", () -> {
            Create{Aggregate}Command cmd = new Create{Aggregate}Command(
                    unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(), {aggregate}Dto);
            this.{aggregate}Dto = ({Aggregate}Dto) commandGateway.send(cmd);
        });

        this.workflow.addStep(create{Aggregate}Step);
    }

    public {Aggregate}Dto get{Aggregate}Dto() {
        return {aggregate}Dto;
    }
}
```

### Shape 2 — multi-step create, with compensation

Here the create is followed by a step that can fail, so the create step registers a compensation that
removes what it created. The compensation reads the id off the DTO the step captured, which is why
the create command's result is stored on the functionality rather than discarded.

```java
public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
        Integer {foreignAggregate}Id, {Aggregate}Dto {aggregate}Dto,
        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
    this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

    // Data assembly — plain read, unchanged by the create
    SagaStep get{ForeignAggregate}Step = new SagaStep("get{ForeignAggregate}Step", () -> {
        Get{ForeignAggregate}ByIdCommand cmd = new Get{ForeignAggregate}ByIdCommand(
                unitOfWork, ServiceMapping.{FOREIGN_AGGREGATE}.getServiceName(), {foreignAggregate}Id);
        this.{foreignAggregate}Dto = ({ForeignAggregate}Dto) commandGateway.send(cmd);
    });

    // Create — no SagaCommand, no semantic lock, no forbiddenStates
    SagaStep create{Aggregate}Step = new SagaStep("create{Aggregate}Step", () -> {
        Create{Aggregate}Command cmd = new Create{Aggregate}Command(
                unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(),
                {aggregate}Dto, this.{foreignAggregate}Dto);
        this.{aggregate}Dto = ({Aggregate}Dto) commandGateway.send(cmd);
    }, new ArrayList<>(Arrays.asList(get{ForeignAggregate}Step)));

    // Genuine domain-level undo: a later step failing must not leave the new aggregate behind
    create{Aggregate}Step.registerCompensation(() -> {
        Delete{Aggregate}Command cmd = new Delete{Aggregate}Command(
                unitOfWork, ServiceMapping.{AGGREGATE}.getServiceName(),
                this.{aggregate}Dto.getAggregateId());
        commandGateway.send(cmd);
    }, unitOfWork);

    SagaStep {operation}Step = new SagaStep("{operation}Step", () -> {
        List<SagaAggregate.SagaState> forbiddenStates = new ArrayList<>();
        forbiddenStates.add({ForeignAggregate}SagaState.IN_{OPERATION}_{FOREIGN_AGGREGATE});

        {Operation}Command cmd = new {Operation}Command(
                unitOfWork, ServiceMapping.{FOREIGN_AGGREGATE}.getServiceName(),
                {foreignAggregate}Id, this.{aggregate}Dto);
        SagaCommand sagaCommand = new SagaCommand(cmd);
        sagaCommand.setForbiddenStates(forbiddenStates);
        commandGateway.send(sagaCommand);
    }, new ArrayList<>(Arrays.asList(create{Aggregate}Step)));

    this.workflow.addStep(get{ForeignAggregate}Step);
    this.workflow.addStep(create{Aggregate}Step);
    this.workflow.addStep({operation}Step);
}
```

Note that the compensation removes a real side effect, which is exactly the remit
`registerCompensation` is reserved for. It is not a lock release - the create step never took a lock.

## Step Ordering

The typical step order inside a write `FunctionalitySagas` class is:

1. *(Conditional)* **Validate-dates step** — if the saga creates or updates an aggregate with `startTime`/`endTime` fields **and** a later step also creates/updates a downstream aggregate that independently validates dates (e.g., a Shipment), add a dedicated `validateDatesStep` as the very first step to check date constraints on the primary aggregate's DTO. If omitted, the downstream aggregate's date invariant fires first and masks the primary aggregate's date error, making the wrong exception surface to tests.

   ```java
   SagaStep validateDatesStep = new SagaStep("validateDatesStep", () -> {
       if (this.{aggregate}Dto.getStartTime().isAfter(this.{aggregate}Dto.getEndTime())) {
           throw new {AppClass}Exception({AppClass}ErrorMessage.{DATE_ORDERING_CONSTANT});
       }
   });
   ```

   Register it with **no dependencies** — being unconditionally first is the entire point, so it must
   not be chained behind a data-assembly step.
2. **Data-assembly steps** — fetch DTOs from upstream aggregates (required for P4a and P3 DTO-check rules listed in plan.md cross-aggregate prerequisites).
3. **Primary lock step** — wrap the read command in `SagaCommand` and call `setSemanticLock(state)` on the primary aggregate. See § Lock-Acquisition Step Pattern. Do **not** register a compensation to release the lock — the core releases it automatically on abort (see § Semantic-lock release on abort is automatic). Do **not** use `setForbiddenStates` for primary-aggregate lock acquisition.
4. **Execute step** — send a plain (unwrapped) command to `{Aggregate}CommandHandler`; declare the lock step as a dependency.
5. *(For multi-aggregate sagas)* **Steps for other aggregates involved** — use `setForbiddenStates` only on steps that touch a **foreign** aggregate to abort if that aggregate is mid-saga (see § R4 Decision Table).

### Collection-valued data-assembly step

When the data a saga must assemble is a **collection** — one upstream fetch per element of a
caller-supplied id list, each seeding one owned snapshot entity — express it as **one step whose
action loops over the ids**, not one step per element:

```java
SagaStep get{Members}Step = new SagaStep("get{Members}Step", () -> {
    for (Integer {member}AggregateId : {member}AggregateIds) {
        Get{Member}ByIdCommand cmd = new Get{Member}ByIdCommand(
                unitOfWork, ServiceMapping.{MEMBER}.getServiceName(), {member}AggregateId);
        {Member}Dto {member}Dto = ({Member}Dto) commandGateway.send(cmd);
        this.{members}.add(new {Aggregate}{Member}Dto({member}Dto.getAggregateId(), /* cached fields */));
    }
});
```

The step name must stay independent of the input, because three mechanisms key on it as a literal:
`executeUntilStep("{step}", uow)` in T4 lock-acquisition tests, the impairment CSV rows of a
compensation test (`testing.md` § Compensation Test), and the step names a retro's Semantic-Lock
Coverage Audit cites. A step per element forces the name to carry an aggregate id or an index, so
every one of those call sites has to know the fixture's data — and the id is not knowable at all
where the collection is empty or caller-sized.

The cost is that fault injection cannot target one element's fetch: the whole loop faults or none of
it does. That is the accepted trade — a per-element fault has no distinct domain meaning, since any
one failed fetch aborts the saga exactly as the loop does.

A single-valued fetch keeps its own dedicated step (§ Step Ordering item 2); this pattern applies
only where the fetch is `× N`.

**R8 — upstream-only commands:** a saga may only send commands to aggregates that are upstream (aggregates this one depends on, not aggregates that depend on it). Never dispatch a write command to a downstream aggregate from within an upstream aggregate's saga.

`registerCompensation` is **only** for genuine domain-level undos — reversing a real side effect a step produced (e.g. deleting a child aggregate a step created). It is **never** used to release a semantic lock: lock release on abort is automatic (see § Semantic-lock release on abort is automatic). Each data-assembly step that enforces a **P4a rule** treats an upstream-command failure as the prerequisite violation — no extra guard is needed in the service layer.

---

## Write Workflow Structure

```java
public class AddShipmentItemFunctionalitySagas extends WorkflowFunctionality {
    public AddShipmentItemFunctionalitySagas(..., SagaUnitOfWork unitOfWork, ...) {
        this.buildWorkflow(..., unitOfWork);
    }

    public void buildWorkflow(..., SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep step1 = new SagaStep("step1Name", () -> { /* action */ });
        SagaStep step2 = new SagaStep("step2Name", () -> { /* action */ },
                new ArrayList<>(Arrays.asList(step1)));  // depends on step1

        this.workflow.addStep(step1);
        this.workflow.addStep(step2);
    }
}
```

`WorkflowFunctionality` provides:
- `executeWorkflow(uow)` — run all steps
- `executeUntilStep(stepName, uow)` — run up to and including the named step (test use only)
- `resumeWorkflow(uow)` — continue from where it stopped (after `executeUntilStep`)

## Saga Class Hierarchy

```
Aggregate (abstract)
  └── Xxx (abstract)
        └── SagaXxx implements SagaAggregate
```

Example: `Warehouse (abstract) → SagaWarehouse implements SagaAggregate`

`SagaWarehouse` holds the `sagaState` field and implements `get/setSagaState()`.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Saga class | `SagaXxx` | `SagaWarehouse` |
| Saga state enum | `XxxSagaState` | `WarehouseSagaState` |
| Functionality | `XxxFunctionalitySagas` | `AddShipmentItemFunctionalitySagas` |
| Factory | `SagasXxxFactory` | `SagasWarehouseFactory` |
| Repository | `XxxCustomRepositorySagas` | `WarehouseCustomRepositorySagas` |
