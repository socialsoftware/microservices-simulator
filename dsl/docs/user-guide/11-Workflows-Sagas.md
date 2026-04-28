# Workflows and Sagas

This chapter covers cross-aggregate saga orchestration: top-level `Workflow` blocks that coordinate multiple aggregates in a single transaction with compensation and saga-state locking.

> **Tied example:** [`showcase`](../../abstractions/showcase/): `reserve-room-workflow.nebula` and `saga-states.nebula`.

## Why Workflows

Individual aggregates handle their own CRUD and business logic. But some operations span multiple aggregates:

- "Create a user, reserve a room, and book it" — touches User, Room, and Booking
- If the booking fails (invariant violation), the room reservation and user creation must be rolled back

The `Workflow` block orchestrates these multi-aggregate operations as a saga: a sequence of steps with compensation for rollback.

## Workflow Syntax

```nebula
Workflow ReserveRoomForUser {
    input {
        String username,
        String email,
        Integer roomId,
        String checkIn,
        String checkOut,
        Integer nights,
        Double price
    }

    step createUser {
        action: User.signUp(username, email)
        compensate: User.deleteUser(createUser.aggregateId)
    }

    step loadRoom {
        action: Room.getRoomById(roomId)
    }

    step reserveRoom {
        action: Room.reserve(roomId)
        compensate: Room.release(roomId)
        lock Room(roomId) as IN_BOOK_ROOM
    }

    step bookRoom {
        action: Booking.bookRoom(createUser, loadRoom, checkIn, checkOut, nights, price)
    }
}
```

### Input Block

Declares the workflow's parameters — these become the `execute(...)` method signature and the fields of the generated request DTO:

```nebula
input {
    String username,
    String email,
    Integer roomId
}
```

### Steps

Each step has:
- **`action: Aggregate.method(args)`** — the operation to perform
- **`compensate: Aggregate.method(args)`** (optional) — the rollback operation if a later step fails
- **`lock Aggregate(id) as STATE`** (optional) — saga-state locking (see below)

Steps execute sequentially. Each step depends on the previous one completing.

### Step Result Chaining

A step's result is stored and can be referenced by later steps:

```nebula
step createUser {
    action: User.signUp(username, email)       // returns UserDto, stored as "createUser"
    compensate: User.deleteUser(createUser.aggregateId)   // uses createUser.aggregateId
}

step bookRoom {
    action: Booking.bookRoom(createUser, loadRoom, ...)   // passes prior step results
}
```

Return types are inferred automatically:
- Methods with `create` action bodies return `<Aggregate>Dto`
- CRUD methods (`getXById`, `createX`, `updateX`) return `<Aggregate>Dto`
- Methods with no return type return `void`

### Cross-Aggregate Projection Wrapping

When a step passes a prior step's result to a method expecting a projection entity, the generator auto-wraps:

```nebula
step bookRoom {
    action: Booking.bookRoom(createUser, loadRoom, ...)
}
```

`createUser` is a `UserDto`, but `bookRoom` expects a `BookingUser` (projection entity `from User`). The generator detects this and emits `new BookingUser(this.createUser)`.

## Compensation

When a step fails, previously completed steps are compensated in reverse order:

```
Step 1: createUser  ──→  success  ──→  Step 2: loadRoom  ──→  success
   ↓ compensate                           (no compensation)
   deleteUser
                                      Step 3: reserveRoom  ──→  success
                                         ↓ compensate
                                         release

                                      Step 4: bookRoom  ──→  FAILS (invariant)
```

If `bookRoom` fails:
1. `reserveRoom` compensates → `Room.release(roomId)` — room back to AVAILABLE
2. `loadRoom` has no compensation → skipped
3. `createUser` compensates → `User.deleteUser(createUser.aggregateId)` — user removed

Steps without `compensate` are read-only and need no rollback.

## Saga-State Locking

### The Problem

Without locking, two concurrent workflows targeting the same room could both reserve it. The copy-on-write version check catches this at commit time (optimistic), but the saga-state mechanism prevents it earlier (pessimistic).

### SagaStates Declaration

Declare saga states in a `SagaStates` block, typically in a dedicated `saga-states.nebula` file:

```nebula
SagaStates {
    ReservationSagaStates {
        IN_BOOK_ROOM,
        IN_CANCEL_BOOKING
    }
}
```

Each group generates a Java enum implementing `SagaState` in `shared/sagaStates/`.

### The `lock` Clause

Add `lock` to a workflow step to stamp the aggregate with a saga state:

```nebula
step reserveRoom {
    action: Room.reserve(roomId)
    compensate: Room.release(roomId)
    lock Room(roomId) as IN_BOOK_ROOM
}
```

This generates two calls before the action:

```java
sagaUnitOfWorkService.verifySagaState(roomId,
    new ArrayList<SagaState>(Arrays.asList(
        ReservationSagaStates.IN_BOOK_ROOM,
        ReservationSagaStates.IN_CANCEL_BOOKING)));
sagaUnitOfWorkService.registerSagaState(roomId,
    ReservationSagaStates.IN_BOOK_ROOM, unitOfWork);
roomService.reserve(roomId, unitOfWork);
```

1. **Verify**: check that the room isn't already held by another saga (any state in the enum is forbidden)
2. **Register**: stamp the room as `IN_BOOK_ROOM`
3. **Execute**: proceed with the action

On commit, the saga state resets to `NOT_IN_SAGA`. On abort, it restores to the previous state.

### Forbidden States Override

By default, all states in the enum are forbidden (block everything). Override with `forbidden [...]` for more selective blocking:

```nebula
lock Room(roomId) as IN_BOOK_ROOM forbidden [IN_CANCEL_BOOKING]
```

This only blocks if the room is in `IN_CANCEL_BOOKING`, allowing concurrent `IN_BOOK_ROOM` operations.

## CRUD Saga-State Enforcement

Beyond workflows, every auto-generated CRUD operation also uses saga-state locking. Each aggregate gets a per-aggregate saga state enum (e.g., `BookingSagaState`) with states for each CRUD operation:

- `CREATE_BOOKING` — used during create (no verify — aggregate doesn't exist yet)
- `READ_BOOKING` — used during read-by-id (verify against UPDATE, DELETE)
- `UPDATE_BOOKING` — used during update (verify against READ, UPDATE, DELETE)
- `DELETE_BOOKING` — used during delete (verify against READ, UPDATE, DELETE)

This means:
- Reads allow concurrent reads but block concurrent writes
- Writes block everything
- The locking is automatic — no DSL annotation needed for CRUD operations

## Generated Files

Each `Workflow` block generates three files:

1. **`<Name>Workflow.java`** — `@Component` extending `WorkflowFunctionality`, autowires all services referenced by steps, chains `SagaStep`s sequentially
2. **`<Name>WorkflowRequestDto.java`** — plain DTO with getters/setters for all inputs
3. **`<Name>WorkflowController.java`** — `@RestController` exposing `POST /workflows/<Name>`

Example HTTP call:

```bash
curl -X POST http://localhost:8923/workflows/ReserveRoomForUser \
    -H 'Content-Type: application/json' \
    -d '{
        "username": "alice",
        "email": "alice@example.com",
        "roomId": 1,
        "checkIn": "2026-07-01",
        "checkOut": "2026-07-03",
        "nights": 2,
        "price": 300.0
    }'
```

## Complete Example

### saga-states.nebula

```nebula
SagaStates {
    ReservationSagaStates {
        IN_BOOK_ROOM,
        IN_CANCEL_BOOKING
    }
}
```

### reserve-room-workflow.nebula

```nebula
Workflow ReserveRoomForUser {
    input {
        String username,
        String email,
        Integer roomId,
        String checkIn,
        String checkOut,
        Integer nights,
        Double price
    }

    step createUser {
        action: User.signUp(username, email)
        compensate: User.deleteUser(createUser.aggregateId)
    }

    step loadRoom {
        action: Room.getRoomById(roomId)
    }

    step reserveRoom {
        action: Room.reserve(roomId)
        compensate: Room.release(roomId)
        lock Room(roomId) as IN_BOOK_ROOM
    }

    step bookRoom {
        action: Booking.bookRoom(createUser, loadRoom, checkIn, checkOut, nights, price)
    }
}
```

This orchestrates 4 steps across 3 aggregates:
1. **createUser** — signs up a new user; compensates by deleting them on failure
2. **loadRoom** — fetches room data (read-only, no compensation)
3. **reserveRoom** — marks the room as RESERVED with saga-state lock; compensates by releasing
4. **bookRoom** — creates the booking using results from steps 1 and 2, with auto-wrapped projections

---

**Previous:** [10-Methods-Custom-Endpoints](10-Methods-Custom-Endpoints.md) | **Next:** [12-Generated-Code](12-Generated-Code.md)
