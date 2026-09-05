# Service Layer

A `*Service` class holds the business logic for one aggregate type. It is the only class allowed to load, mutate, and persist that aggregate. All service methods are called through the aggregate's `CommandHandler` via `commandGateway`.

---

## Injected Dependencies

A service may inject only components that belong to its own aggregate, plus shared infrastructure:

```java
@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;             // own aggregate's JPA repo
    private final WarehouseCustomRepository warehouseCustomRepository; // own aggregate's custom repo
    private final WarehouseFactory warehouseFactory;                   // own aggregate's factory
    private final UnitOfWorkService unitOfWorkService;                 // raw type, deliberately
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            WarehouseCustomRepository warehouseCustomRepository,
                            WarehouseFactory warehouseFactory,
                            UnitOfWorkService unitOfWorkService,
                            AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseCustomRepository = warehouseCustomRepository;
        this.warehouseFactory = warehouseFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }
}
```

> **Every dependency goes through the constructor. A service declares no `@Autowired` field.** All
> fields are `final`, which makes a half-wired service impossible to construct and lets the compiler,
> rather than a runtime `NullPointerException`, catch a dependency that was added to the class but not
> to the `@Bean` method. It also keeps one rule instead of two: a session appending a method that
> needs a collaborator the service does not yet hold widens the constructor **and** the matching
> `@Bean` method in `BeanConfigurationSagas.groovy`, whichever collaborator it is — there is no
> second, field-injected category that would let it skip the `@Bean` edit.
>
> The list above is closed: own repository, own custom repository, own factory, `UnitOfWorkService`,
> `AggregateIdGeneratorService`. Omit any the service genuinely does not use; add nothing else.

Never inject a foreign service class or a foreign repository — see [R1, R2 in architecture.md](../architecture.md).

`aggregateLoadAndRegisterRead` is called only with ids of this service's own aggregate type (R1). A
foreign aggregate's state never arrives by loading it here; it arrives as a DTO parameter, assembled
by the Functionality from a `Get*Command` step.

Never hold a reference to another aggregate's **concrete class** either (R3). A service may accept and return any aggregate's `{Xxx}Dto`, but aggregate instances carry UoW registration state that must not cross service boundaries — cross-aggregate state flows as DTOs, assembled by the Functionality from a `Get*Command` step and passed downstream as plain values.

> **Inject factories and repositories via their abstract interfaces, not the concrete sagas-profile classes.** The example above injects `WarehouseFactory` (the interface defined in `aggregate/`) — `SagasWarehouseFactory` is never referenced in the service. This keeps the service layer profile-agnostic and allows a TCC or other implementation to be wired in without touching the service.

> **`UnitOfWorkService` is used raw, on both the field and the constructor parameter.** No type
> argument, no cast, no `@SuppressWarnings`. `UnitOfWorkService<U extends UnitOfWork>` is abstract
> with `U` in both parameter and return positions, and its concrete subclasses are
> `SagaUnitOfWorkService` and `CausalUnitOfWorkService`. Java generics are invariant, so
> `UnitOfWorkService<UnitOfWork>` is satisfied by neither and describes a type that cannot exist.
>
> The service is profile-agnostic, so it treats the unit of work as an opaque token that it hands
> back to the collaborator which issued it. It never inspects it, so the type parameter buys nothing
> here, and naming a concrete profile type would violate `AGENTS.md` § Architecture principle.
>
> Two alternatives were considered and rejected. Making the service itself generic
> (`{Aggregate}Service<U extends UnitOfWork>`) propagates `<U>` into every `Command` and
> `CommandHandler` signature for zero added safety at the only call site. Adding a non-generic facade
> to `simulator/` changes the framework to repair a documentation defect, which is disproportionate.

---

## Method Patterns

Every service method is annotated `@Transactional(isolation = Isolation.SERIALIZABLE)`. This makes P3 guards race-free.

> **Never call `verifyInvariants()` from a service method.** `registerChanged` invokes it on the
> aggregate it is given (`SagaUnitOfWorkService.registerChanged`), so an explicit call is at best a
> duplicate and at worst fires against a half-applied mutation. This holds for every mutation path
> without exception - saga steps and event-driven (`ByEvent`) updates alike. This section is the single
> owner of the rule; other docs point here rather than restating it.

### Read method

Loads the latest version and converts it to a DTO. No mutation, no `registerChanged`.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public WarehouseDto getWarehouseById(Integer warehouseAggregateId, UnitOfWork unitOfWork) {
    return warehouseFactory.createWarehouseDto(
            (Warehouse) unitOfWorkService.aggregateLoadAndRegisterRead(warehouseAggregateId, unitOfWork));
}
```

### Create method

Generates a new aggregate ID, constructs the aggregate via the factory, registers it as changed.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public WarehouseDto createWarehouse(WarehouseDto dto, UnitOfWork unitOfWork) {
    // [P3] uniqueness guard — reads own table, inside @Transactional(SERIALIZABLE)
    Set<Integer> existingIds = warehouseCustomRepository.findWarehouseIdsOfAllNonDeleted();
    for (Integer id : existingIds) {
        Warehouse existing = (Warehouse) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        if (existing.getCode().equals(dto.getCode())
                && existing.getRegion().equals(dto.getRegion())) {
            throw new {AppClass}Exception(DUPLICATE_WAREHOUSE, dto.getCode(), dto.getRegion());
        }
    }

    Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
    Warehouse warehouse = warehouseFactory.createWarehouse(aggregateId, dto, ...);

    unitOfWorkService.registerChanged(warehouse, unitOfWork);
    return warehouseFactory.createWarehouseDto(warehouse);
}
```

### Mutate method

Loads the current version, copies it via the factory's `create{Aggregate}Copy` method, applies mutations on the copy, then registers the copy as changed.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void registerShipment(Integer warehouseAggregateId, ShipmentDto shipmentDto, UnitOfWork unitOfWork) {
    // [P3] guard — validates DTO field assembled by preceding saga step
    Warehouse oldWarehouse = (Warehouse) unitOfWorkService.aggregateLoadAndRegisterRead(
            warehouseAggregateId, unitOfWork);

    if (!shipmentDto.isActive()) {
        throw new {AppClass}Exception(INACTIVE_SHIPMENT, shipmentDto.getAggregateId());
    }

    // copy-on-write: mutations go on the new version, not the loaded one
    Warehouse newWarehouse = warehouseFactory.createWarehouseCopy(oldWarehouse);
    newWarehouse.addShipment(new WarehouseShipment(shipmentDto));

    unitOfWorkService.registerChanged(newWarehouse, unitOfWork);
}
```

### Mutate method with event publication

After `registerChanged`, register the event. The event is persisted atomically when the UoW commits.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void removeWarehouse(Integer warehouseAggregateId, UnitOfWork unitOfWork) {
    Warehouse oldWarehouse = (Warehouse) unitOfWorkService.aggregateLoadAndRegisterRead(
            warehouseAggregateId, unitOfWork);
    Warehouse newWarehouse = warehouseFactory.createWarehouseCopy(oldWarehouse);

    newWarehouse.remove();   // sets state = DELETED

    unitOfWorkService.registerChanged(newWarehouse, unitOfWork);
    unitOfWorkService.registerEvent(
            new DeleteWarehouseEvent(newWarehouse.getAggregateId()), unitOfWork);
}
```

### Mutate method with optional sub-collection parameter

When a saga updates an aggregate that contains a sub-collection, but cannot reconstruct the full sub-objects from the DTOs available at that point (e.g. the DTO only carries IDs, not full objects), pass `null` for the sub-collection parameter and guard the setter with an explicit null check:

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void updateShipment(Integer shipmentAggregateId, ShipmentDto shipmentDto,
                           Set<ShipmentItem> shipmentItems,  // may be null
                           UnitOfWork unitOfWork) {
    Shipment oldShipment = (Shipment) unitOfWorkService
            .aggregateLoadAndRegisterRead(shipmentAggregateId, unitOfWork);
    Shipment newShipment = shipmentFactory.createShipmentCopy(oldShipment);

    if (shipmentDto.getStartTime() != null) {
        newShipment.setStartTime(shipmentDto.getStartTime());
    }
    // ...
    if (shipmentItems != null) {          // skip if caller couldn't reconstruct sub-objects
        newShipment.setShipmentItems(shipmentItems);
    }

    unitOfWorkService.registerChanged(newShipment, unitOfWork);
}
```

**When to use:** The calling saga fetches an upstream aggregate's DTO but that DTO only returns IDs for sub-objects (e.g. `WarehouseDto.shipmentIds`). Fetching each sub-object individually would require N extra command steps, which buy nothing when the sub-collection is not the operation's primary intent (see § When not to use). Passing `null` and guarding the setter keeps the update intent explicit without polluting the saga with unnecessary reads.

**When not to use:** If the sub-collection update is the primary intent of the operation (the caller always has the data), make the parameter non-null and remove the guard — a missing `null` check is then a silent data loss bug.

---

## Copy-on-Write Rule

Never mutate the aggregate instance returned by `aggregateLoadAndRegisterRead`. Always create a new version via `factory.create{Aggregate}Copy(old)` and mutate that copy. The old version remains in the UoW read set for conflict detection; the new version is the write target.

**Soft-delete (`remove()`) — use copy-on-write:** Even for soft-delete, always create a factory copy before calling `remove()`:

```java
Warehouse oldWarehouse = (Warehouse) unitOfWorkService.aggregateLoadAndRegisterRead(warehouseAggregateId, unitOfWork);
Warehouse newWarehouse = warehouseFactory.createWarehouseCopy(oldWarehouse);
newWarehouse.remove();
unitOfWorkService.registerChanged(newWarehouse, unitOfWork);
```

**Sub-collection clearing before `remove()`:** When an aggregate's `verifyInvariants()` enforces a rule like "must have no members when deleted" (e.g., `SHIPMENT_DELETE` requires `items.isEmpty()` when `state == DELETED`), clear the collection on the copy *before* calling `remove()`:

```java
Shipment oldShipment = (Shipment) unitOfWorkService.aggregateLoadAndRegisterRead(
        shipmentAggregateId, unitOfWork);
Shipment newShipment = shipmentFactory.createShipmentCopy(oldShipment);
newShipment.setItems(new HashSet<>());   // satisfy SHIPMENT_DELETE invariant
newShipment.remove();
unitOfWorkService.registerChanged(newShipment, unitOfWork);
```

This is required when two invariants interact: a deletion invariant checks a collection, and a cancellation invariant forbids mutations on cancelled aggregates. The clear step satisfies the deletion invariant before `remove()` fires `verifyInvariants()`.

**Why copy-on-write is required here:** If you call `remove()` on the managed JPA entity returned by `aggregateLoadAndRegisterRead`, JPA marks that entity dirty immediately. Before the saga abort path can run its `findNonDeletedSagaAggregate` JPQL query, JPA may auto-flush the dirty state — setting the aggregate's state to `DELETED` in the DB. The abort query then finds nothing (it filters `state != DELETED`), causing the abort to fail silently. Copy-on-write keeps the original managed entity unmodified; only the new (unmanaged) copy carries the `DELETED` state, so the abort query always succeeds. This problem surfaces specifically when `verifyInvariants()` contains a rule that checks `state == DELETED` (e.g., `REMOVE_NO_ITEMS`): the abort path re-loads the aggregate and calls `verifyInvariants`, which fires the check — but JPA's auto-flush has already written the `DELETED` state, so the invariant throws a `SimulatorException` instead of the expected application exception.

> **Rule:** Soft-delete goes through copy-on-write like any other mutation, unconditionally — not only where an invariant is known to check `state == DELETED`. In-place `remove()` appears to work whenever an aggregate's invariants happen not to fire on the abort path, so a service that mutates in place is a latent bug that surfaces the moment such an invariant is added.

---

## DTO Immutability (R7)

A Functionality method receives saga-assembled DTOs from preceding steps. Never mutate a DTO passed into a service method — it is a read-only snapshot from an upstream aggregate. Extract the field values you need and pass them directly to the aggregate or a new value object; do not call any setters on the DTO itself.

---

## Partial-Data Owned Entities

When a service method creates an owned entity (e.g. `ShipmentItem`) and one of its constructor fields cannot be derived from the DTOs available at that point in the implementation order, initialize that field to `null` as a placeholder:

```java
// TODO: inStock cannot be determined from WarehouseDto — no stock data
// available until Inventory (session 2.8) is implemented.
new ShipmentItem(warehouseId, sequenceNumber, /*inStock=*/ null);
```

Rules:
- Add the `// TODO` comment inline explaining which field is missing and why.
- Flag the placeholder explicitly in the session retro — it is **not** an implicit placeholder; a future session must revisit it.
- When the missing upstream aggregate is implemented, search for the `TODO` and fill in the field.

---

## Exception-Throw Convention

**Always throw exceptions with the raw error-message constant and no format arguments**, unless the constant string contains a `%` placeholder that must be filled in and the test assertion checks the formatted value.

```java
// Correct — test can assert: ex.message == SHIPMENT_ALREADY_CANCELLED
throw new {AppClass}Exception(SHIPMENT_ALREADY_CANCELLED);

// Avoid unless the test expects the formatted string, not the constant
throw new {AppClass}Exception(SHIPMENT_ALREADY_CANCELLED, shipmentId);
```

**Why:** Test assertions use `ex.message == CONSTANT` (raw format string comparison). When a format argument is passed, `getMessage()` returns the interpolated string (e.g., `"Shipment 42 is already cancelled"`), not the literal constant `"SHIPMENT_ALREADY_CANCELLED"`, breaking the equality check. Only use format args when the assertion explicitly verifies the interpolated value.

---

## Custom Repository — Latest-Active-Version Query

`findAll()` (or `jpaRepo.findAll()`) returns **all versions** of every aggregate, including historical, superseded, and soft-deleted ones. Any bulk read that must reflect current state must use a "latest active version per aggregateId" query.

**JPQL pattern:**

```java
@Query("select s from Shipment s " +
       "where s.state = 'ACTIVE' " +
       "and s.version = (select max(s2.version) from Shipment s2 where s2.aggregateId = s.aggregateId)")
List<Shipment> findAllLatestActive();
```

**When to add this:** Whenever a custom repository method performs a bulk read (returns multiple aggregate instances) — e.g., `findAll`, `findAllByWarehouseId`, `findAllOpen`. Scoped reads via `aggregateLoadAndRegisterRead` are unaffected (they already load the latest version).

Add the JPQL method to `{Aggregate}Repository.java` (JPA repo interface) and call it from `{Aggregate}CustomRepositorySagas` — never call `jpaRepo.findAll()` directly in bulk-read implementations.

**Declare it on the abstract `{Aggregate}CustomRepository` too.** The service injects the interface,
never the concrete `Sagas` class (§ Injected Dependencies, and `AGENTS.md` § Architecture principle),
so a method that exists only on `{Aggregate}CustomRepositorySagas` is unreachable from the service and
the call does not compile. All three files carry it:

| File | Role |
|------|------|
| `{Aggregate}Repository.java` | the `@Query` JPQL itself |
| `{Aggregate}CustomRepository.java` | the abstract declaration the service calls through |
| `{Aggregate}CustomRepositorySagas.java` | `@Override`, delegating to the JPA repo |

---

## P3 Guard Placement

P3 guards (own-table reads, uniqueness checks, and DTO field validation from preceding saga steps) belong at the **top** of the service method, before any `create{Aggregate}Copy` call. Throwing at this point ensures no aggregate is dirtied before the guard fires. See [`rule-enforcement-patterns.md`](rule-enforcement-patterns.md) for the full taxonomy.
