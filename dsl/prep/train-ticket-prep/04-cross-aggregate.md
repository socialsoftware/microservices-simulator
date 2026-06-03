# 04 — Cross-Aggregate Dependencies (train-ticket, booking core)

> **How the source actually links aggregates:** synchronous REST/Feign calls + foreign
> *values* (ids and **names**) stored on rows. There is **no DB-level FK, no cascade, no
> referential enforcement** anywhere in train-ticket — deleting a `User` does not touch its
> `Contacts`/`Order`s; deleting a `Station` does not check routes/trips. So **every
> `onDelete` policy below is a Nebula-introduced decision**, marked ⚠ with the
> recommended choice + alternative.

Organised by **consumer** aggregate. References use Nebula `source.field -> Target` form;
inter-invariants and projection subscriptions use the inferred events from `03`.

---

## Contacts (consumes User)
**Reference**
```
accountId -> User
    onDelete: cascade        # ⚠ Gap — recommended: contacts belong to an account; if the
                             #   account dies, its contacts should too.
                             #   Alternative: prevent (block deleting accounts with contacts).
    message: "Cannot delete account while it owns contacts"   # (only if 'prevent')
```
**Inter-invariant** (integrity, if not cascading)
```
interInvariant CONTACTS_ACCOUNT_EXISTS {
    subscribe UserDeletedEvent from User { accountId != event.aggregateId }
}
```

## Route (consumes Station, by name)
**Reference** — ⚠ soft, by *name* not id
```
stations[] -> Station (by name)
    onDelete: prevent        # ⚠ Gap — recommended: don't delete a station used by a route.
    message: "Cannot delete station used by an existing route"
```
**Inter-invariant**
```
interInvariant ROUTE_STATIONS_EXIST {
    subscribe StationDeletedEvent from Station { !stations.contains(event.name) }
}
```
⚠ Reference is by name, not aggregateId — `event` must carry the station `name`, or Route
must store station aggregateIds instead of names (schema change vs source).

## Trip (consumes TrainType, Route, Station)
**References**
```
trainTypeName -> TrainType (by name)
    onDelete: prevent        # ⚠ recommended; alt: cascade-delete trips of a removed train type
    message: "Cannot delete train type with scheduled trips"

routeId -> Route (by id)
    onDelete: prevent        # ⚠ recommended
    message: "Cannot delete route with scheduled trips"

startStationName / terminalStationName / stationsName -> Station (by name)
    onDelete: prevent        # ⚠ recommended
    message: "Cannot delete station used by a trip"
```
**Inter-invariants**
```
interInvariant TRIP_TRAINTYPE_EXISTS {
    subscribe TrainTypeDeletedEvent from TrainType { trainTypeName != event.name }
}
interInvariant TRIP_ROUTE_EXISTS {
    subscribe RouteDeletedEvent from Route { routeId != event.aggregateId }
}
interInvariant TRIP_STATIONS_EXIST {
    subscribe StationDeletedEvent from Station {
        startStationName != event.name && terminalStationName != event.name
    }
}
```

## PriceConfig (consumes TrainType, Route)
**References**
```
trainType -> TrainType (by name)
    onDelete: cascade        # ⚠ recommended: a price rule for a deleted train type is dead.
                             #   alt: prevent.
routeId -> Route (by id)
    onDelete: cascade        # ⚠ recommended: price rule for a deleted route is dead.
```
**Inter-invariants** (if prevent chosen instead of cascade)
```
interInvariant PRICE_TRAINTYPE_EXISTS {
    subscribe TrainTypeDeletedEvent from TrainType { trainType != event.name }
}
interInvariant PRICE_ROUTE_EXISTS {
    subscribe RouteDeletedEvent from Route { routeId != event.aggregateId }
}
```

## Order (consumes User, Contacts, TrainType/Trip, Station)
**References**
```
user (accountId)   -> User
    onDelete: prevent        # ⚠ recommended: orders are financial records; don't lose them.
    message: "Cannot delete account with existing orders"
contacts            -> Contacts
    onDelete: prevent        # ⚠ recommended (or setNull — order keeps the frozen snapshot)
    message: "Cannot delete contact referenced by an order"
train (trainNumber) -> TrainType
    onDelete: prevent
    message: "Cannot delete train type referenced by an order"
fromStation / toStation -> Station
    onDelete: prevent
    message: "Cannot delete station referenced by an order"
```
**Projection subscriptions** (only if snapshots are *not* frozen — see `03` gap 3)
```
subscribe ContactsUpdatedEvent     # refresh contactsName/documentType/contactsDocumentNumber
subscribe StationUpdatedEvent      # refresh from/to names
subscribe TrainTypeUpdatedEvent    # refresh trainNumber
```
**Inter-invariants** (integrity)
```
interInvariant ORDER_USER_EXISTS {
    subscribe UserDeletedEvent from User { user.accountId != event.aggregateId }
}
interInvariant ORDER_CONTACTS_EXISTS {
    subscribe ContactsDeletedEvent from Contacts { contacts.aggregateId != event.aggregateId }
}
interInvariant ORDER_TRAINTYPE_EXISTS {
    subscribe TrainTypeDeletedEvent from TrainType { train.trainNumber != event.name }
}
interInvariant ORDER_STATIONS_EXIST {
    subscribe StationDeletedEvent from Station {
        fromStation.fromName != event.name && toStation.toName != event.name
    }
}
```

---

## The booking saga (orchestration, not a reference)
`ts-preserve-service` orchestrates order creation as a synchronous pipeline — this is the
natural **Sagas workflow** for an `OrderPreserve`/`createOrder` functionality, not a static
reference. Observed steps (`PreserveServiceImpl`):
1. **Check security** (account not blacklisted / under order limit) — `ts-security` → ⚠ out of booking-core scope.
2. **Find contacts** by id (`Contacts`).
3. **Check ticket availability** for the trip+date+seatClass (`Trip` + `Seat` compute).
4. **Dispatch a seat** → fills `coachNumber`/`seatNumber` (`Seat` compute).
5. **Create order** (`Order`).
6. Buy assurance / add food / consign — ⚠ out of booking-core scope.

⚠ **Gap — seat availability & dispatch:** steps 3–4 are a *cross-aggregate computation*
(count existing `Order`s for the trip vs `TrainType` capacity), not expressible as a static
reference or a single-aggregate invariant. Candidates: a Sagas guard/forbidden-state check
in the create-order functionality, querying sold orders. Resolve before implementing
`createOrder`.

## ⚠ Gaps summary (this file)
- Every `onDelete` policy is introduced by Nebula (source enforces none) — confirm each.
- Reference-by-**name** (Station, TrainType) vs by-aggregateId: inter-invariants need the
  event to carry the name, or a schema change to store aggregateIds. Decide globally.
- Seat availability/dispatch (preserve steps 3–4) has no clean reference/invariant home.
- Security/assurance/food/consign steps are outside booking-core scope.
