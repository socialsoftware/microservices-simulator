# Commands

Commands are the messages that cross the boundary between the Functionality layer and the Service layer. Every inter-service call inside a workflow step is expressed as a command dispatched through `CommandGateway`.

---

## What a Command Is

A `Command` subclass carries the inputs for one service operation. It holds the unit of work, the target service name, the primary aggregate ID (passed to `super(...)`), and any domain-specific payload fields.

```java
public class AddShipmentItemCommand extends Command {
    private Integer shipmentAggregateId;
    private WarehouseDto warehouseDto;

    public AddShipmentItemCommand(UnitOfWork unitOfWork, String serviceName,
                                  Integer shipmentAggregateId, WarehouseDto warehouseDto) {
        super(unitOfWork, serviceName, shipmentAggregateId);
        this.shipmentAggregateId = shipmentAggregateId;
        this.warehouseDto = warehouseDto;
    }

    public Integer getShipmentAggregateId() { return shipmentAggregateId; }
    public WarehouseDto getWarehouseDto() { return warehouseDto; }
}
```

`super(unitOfWork, serviceName, aggregateId)`:
- `unitOfWork` — the active `SagaUnitOfWork` for this workflow execution.
- `serviceName` — the routing key. Always `ServiceMapping.{AGGREGATE}.getServiceName()` (e.g. `ServiceMapping.SHIPMENT.getServiceName()` → `"shipment"`), never a hardcoded literal and never `getAggregateTypeName()`'s PascalCase value — see § Routing Commands below for why the two differ.
- `aggregateId` — stored on the base `Command` as `rootAggregateId`. It is the aggregate whose
  **semantic lock lifecycle** this command participates in. It has no consumers in the `unitOfWork`
  package; under sagas it is read only by `SagaCommandHandler`, for
  `verifySagaState(rootAggregateId, forbiddenStates)` and for lock registration.

Commands are plain data carriers — no business logic, no Spring beans.

> **When there is no root aggregate to name, pass `null`.** Several step shapes have none: a
> **create** command, whose id the service has not minted yet (it generates it via
> `aggregateIdGeneratorService`); an **unfiltered collection read** - a `Get{Aggregates}Command` that
> returns every instance of its type and so takes no id at all; and a **composite-key read** - a
> `Get{Aggregate}By{Field}Command` that names its target by a compound domain key held on the
> aggregate, so the target's own id is unknown until the service has resolved that key.
>
> A **filtered** collection read is not one of them: its filter *is* a foreign aggregate id, which it
> passes.
>
> This is safe rather than merely tolerated, and the reason is a property of the *step*, not of
> createness: the field is dereferenced only by `SagaCommandHandler`, and only for a `SagaCommand`
> carrying forbidden states or a semantic lock. A step that declares neither never reaches that code.
> All the shapes above declare neither. Version conflict detection is unaffected either way; it operates
> on the aggregates registered read/changed with the unit of work, not on this field.
>
> The converse still holds: a step that *does* declare a semantic lock or forbidden states must name
> the aggregate whose lock lifecycle it participates in.

---

## Naming Conventions

| Purpose | Pattern | Example |
|---------|---------|---------|
| Read command | `Get<Xxx>Command` | `GetShipmentByIdCommand` |
| Mutate command | `<Operation><Xxx>Command` | `AddShipmentItemCommand`, `RemoveWarehouseCommand` |
| Event-driven update | `Update<Field>Command` | `UpdateUserNameCommand` |

---

## File Location

```
src/main/java/<pkg>/<appName>/commands/<aggregate>/
    Get<Xxx>Command.java
    <Operation><Xxx>Command.java
```

All commands for the same target aggregate live in one package under the application root (not inside a microservice package) so they can be shared across services.

---

## ServiceMapping Enum

Each application defines a `ServiceMapping` enum that maps aggregate names to their service routing strings. Use these constants when constructing commands — never hardcode string literals.

```java
public enum ServiceMapping {
    SHIPMENT("shipment"),
    WAREHOUSE("warehouse"),
    // ...
    ;
    private final String serviceName;
    ServiceMapping(String serviceName) { this.serviceName = serviceName; }
    public String getServiceName() { return serviceName; }
}
```

> **Multi-word aggregates — camelCase required:** The framework derives the commit/abort service bean at runtime by calling `resolveServiceName(aggregateClass)`, which strips "Saga" from the simple class name and lowercases the first character. For multi-word aggregates this produces camelCase: `SagaShipmentItem` → `"shipmentItem"`. The `ServiceMapping` value **must** match this result exactly. A shortened alias (e.g. `"item"`) causes a silent bean-lookup failure that only manifests when the aggregate is locked via `SagaCommand` at runtime. Example of a correct entry:
> ```java
> SHIPMENT_ITEM("shipmentItem"),  // SagaShipmentItem → resolveServiceName → "shipmentItem"
> ```

Pass `ServiceMapping.SHIPMENT.getServiceName()` as the `serviceName` argument to a command constructor.

---

## Sending Commands (Functionality Layer)

Inside a saga step, dispatch a command with `commandGateway.send(...)`. The return type is `Object` — cast to the expected DTO for read commands; ignore it for mutating commands.

```java
// Read step — returns a DTO
SagaStep getWarehouseStep = new SagaStep("getWarehouseStep", () -> {
    GetWarehouseByIdCommand cmd = new GetWarehouseByIdCommand(
            unitOfWork,
            ServiceMapping.WAREHOUSE.getServiceName(),
            warehouseAggregateId);
    this.warehouseDto = (WarehouseDto) commandGateway.send(cmd);
});

// Mutate step — depends on getWarehouseStep; sets forbiddenStates for Sagas
SagaStep addShipmentItemStep = new SagaStep("addShipmentItemStep", () -> {
    AddShipmentItemCommand cmd = new AddShipmentItemCommand(
            unitOfWork,
            ServiceMapping.SHIPMENT.getServiceName(),
            shipmentAggregateId,
            this.warehouseDto);
    cmd.setForbiddenStates(List.of(ShipmentSagaState.IN_UPDATE_SHIPMENT));
    commandGateway.send(cmd);
}, List.of(getWarehouseStep));
```

**Commands travel upstream only (R8).** A saga may only send commands to aggregates it depends on, never to an aggregate that depends on it — downstream aggregates learn of changes through events, not commands. See [`sagas.md`](sagas.md) § Step Ordering.

---

## Routing Commands (CommandHandler)

Each aggregate has one `CommandHandler` that receives all commands for that aggregate and dispatches them to the service. Use a `switch` over sealed/pattern-matched types.

```java
@Component
public class ShipmentCommandHandler extends CommandHandler {

    @Autowired
    private ShipmentService shipmentService;

    @Override
    public String getAggregateTypeName() {
        // PascalCase, used for decorator lookup - not the routing key (see below)
        return "Shipment";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetShipmentByIdCommand cmd -> shipmentService.getShipmentById(
                    cmd.getAggregateId(), cmd.getUnitOfWork());
            case AddShipmentItemCommand cmd -> {
                shipmentService.addShipmentItem(
                        cmd.getShipmentAggregateId(), cmd.getWarehouseDto(), cmd.getUnitOfWork());
                yield null;
            }
            // ... one case per command
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
```

`getAggregateTypeName()` returns a PascalCase name (e.g. `"Warehouse"`) used by `CommandHandlerDecorator` for decorator lookup — it is **not** the routing key.

**Actual routing:** `LocalCommandService.send()` resolves the handler via `applicationContext.getBean(command.getServiceName() + "CommandHandler")`. The Spring bean name of the `CommandHandler` **must** equal `ServiceMapping.{AGGREGATE}.getServiceName() + "CommandHandler"` (e.g. `"warehouseCommandHandler"`). The `@Bean` method in `BeanConfigurationSagas` must use that exact lowercase camelCase name. For multi-word aggregates this is a camelCase name — e.g. the bean method for ShipmentItem must be named `shipmentItemCommandHandler`, not `itemCommandHandler`.

Mutating handlers return `null`; read handlers return the DTO produced by the service method.

---

## Known DTO Gaps and Compensating Command Steps

Some DTOs returned by upstream service commands are structurally incomplete — they carry enough data for display or membership checks but omit fields (such as `version`) that a saga needs to construct a dependent aggregate. When this happens, insert an extra command step to fetch the missing data rather than inferring or hardcoding it.

### A nested member DTO lacks `version`

A DTO nested inside an owning aggregate's DTO (e.g. `WarehouseSlotDto`, returned as part of `WarehouseDto.slots`) may not carry a `version` field. Any saga that needs to record the member aggregate's `version` when creating or modifying an aggregate that embeds a snapshot of it (e.g. `Shipment.origin`, `Shipment.items`) cannot derive the version from the membership-check step alone.

**Compensating pattern:** insert a dedicated `Get{Publisher}ByIdCommand` step immediately after the membership-check step to retrieve the full publisher DTO, which does carry `version`:

```java
// Step 1 — verify the referenced member belongs to the owner (returns WarehouseDto)
SagaStep getWarehouseStep = new SagaStep("getWarehouseStep", () -> {
    this.warehouseDto = (WarehouseDto) commandGateway.send(
            new GetWarehouseByIdCommand(unitOfWork,
                    ServiceMapping.WAREHOUSE.getServiceName(), warehouseAggregateId));
});

// Step 2 — fetch the full CarrierDto so we have the version field
SagaStep getOriginCarrierStep = new SagaStep("getOriginCarrierStep", () -> {
    this.carrierDto = (CarrierDto) commandGateway.send(
            new GetCarrierByIdCommand(unitOfWork,
                    ServiceMapping.CARRIER.getServiceName(), carrierAggregateId));
}, List.of(getWarehouseStep));
```

Name the step after the role the fetched aggregate plays in the operation (`getOriginCarrierStep` when it is the single owner-like reference, `get{Publisher}Step` when it is one of many members). The DTO returned by `Get{Publisher}ByIdCommand` carries `version` and can be passed directly to the downstream command.
