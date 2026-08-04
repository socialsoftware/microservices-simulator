# Train-Ticket — Domain Model (booking core)

Eight aggregates, organised by their position in the reference graph. An arrow
**`A → B`** reads "**A depends on B**": A holds a reference/projection of B, so **A is
*downstream*** and **B is *upstream***. Upstream aggregates are sources of truth; downstream
aggregates cache or point at them and must react when an upstream aggregate is deleted
(integrity) or changed (projection refresh).

> All cross-aggregate links and events are **Nebula-introduced** — train-ticket itself is a
> synchronous REST/Feign system with no event bus and no DB-level FKs. See `03-events.md`
> and `04-cross-aggregate.md` for the gaps each decision carries.

---

## Dependency layers (upstream → downstream)

```
 Layer 0  (pure upstream — referenced, reference nobody)
   ┌──────────┐   ┌───────────┐   ┌─────────────┐
   │   User   │   │  Station  │   │  TrainType  │
   └────┬─────┘   └─────┬─────┘   └──────┬──────┘
        │               │                │
 Layer 1│               │ (by name)      │
   ┌────▼─────┐    ┌────▼──────┐         │
   │ Contacts │    │   Route   │◄────────┼─────────┐
   └────┬─────┘    └────┬──────┘         │         │
        │               │                │         │
 Layer 2│               ├────────────────┤         │
        │          ┌────▼───────────┐ ┌──▼─────────▼──┐
        │          │      Trip      │ │  PriceConfig  │
        │          └────┬───────────┘ └───────────────┘
        │               │            (terminal — no consumers)
 Layer 3│               │
   ┌────▼───────────────▼───────────────────────────────┐
   │                      Order                          │
   │  (pure downstream — references User, Contacts,      │
   │   Station, TrainType, Trip; references nobody back) │
   └─────────────────────────────────────────────────────┘
```

| Layer | Aggregates | Role |
|-------|-----------|------|
| 0 | **User, Station, TrainType** | Pure **upstream** reference data. No outgoing references. |
| 1 | **Contacts** (→ User), **Route** (→ Station) | Mixed: downstream of layer 0, upstream of others. |
| 2 | **Trip** (→ TrainType, Route, Station), **PriceConfig** (→ TrainType, Route) | Mostly downstream; only Trip has a consumer (Order). |
| 3 | **Order** | Pure **downstream** sink — the booking artefact everything funnels into. |

---

## Aggregate-by-aggregate (upstream & downstream relations)

### User  — *Layer 0, pure upstream*
- **Owns:** `userId`, `userName`, `password`, `gender`, `documentType`, `documentNum`, `email`.
- **Upstream of (who depends on User):**
  - **Contacts** — `accountId → User` (`onDelete: cascade`, recommended).
  - **Order** — `accountId → User` (`onDelete: prevent` — orders are financial records).
- **Downstream of:** nobody.
- **Emits:** `UserDeletedEvent` (integrity for Contacts, Order).

### Station — *Layer 0, pure upstream (referenced by name)*
- **Owns:** `name` (unique, normalised), `stayTime`.
- **Upstream of:**
  - **Route** — `stations[] → Station` *(by name)*, `onDelete: prevent`.
  - **Trip** — `startStationName / terminalStationName / stationsName → Station` *(by name)*, `prevent`.
  - **Order** — `from / to → Station` *(by name, projected snapshot)*, `prevent`.
- **Downstream of:** nobody.
- **Emits:** `StationDeletedEvent` (integrity), `StationUpdatedEvent` (projection refresh — only if names are mutable).

### TrainType — *Layer 0, pure upstream (referenced by name)*
- **Owns:** `name` (unique), `economyClass`, `confortClass`, `averageSpeed`.
- **Upstream of:**
  - **Trip** — `trainTypeName → TrainType` *(by name)*, `prevent`.
  - **PriceConfig** — `trainType → TrainType` *(by name)*, `cascade`.
  - **Order** — `trainNumber → TrainType` *(by name)*, `prevent`.
- **Downstream of:** nobody.
- **Emits:** `TrainTypeDeletedEvent`, `TrainTypeUpdatedEvent`.

### Route — *Layer 1, mixed*
- **Owns:** `distances[]`; holds `stations[]`, `startStation`, `endStation` as *references* to Station (by name).
- **Downstream of:** **Station** (subscribes `StationDeletedEvent`).
- **Upstream of:** **Trip** (`routeId → Route`, by id), **PriceConfig** (`routeId → Route`, by id).
- **Emits:** `RouteDeletedEvent`, `RouteUpdatedEvent`.

### Contacts — *Layer 1, mixed*
- **Owns:** `name`, `documentType`, `documentNumber`, `phoneNumber`.
- **Downstream of:** **User** (`accountId → User`; subscribes `UserDeletedEvent`).
- **Upstream of:** **Order** (`contacts → Contacts`, snapshotted into the order at booking).
- **Emits:** `ContactsDeletedEvent`, `ContactsUpdatedEvent`.

### Trip — *Layer 2, mostly downstream*
- **Owns:** `tripId` (embedded `type` + `number`), `startTime`, `endTime`.
- **Downstream of:** **TrainType**, **Route**, **Station** (subscribes their `*DeletedEvent`s).
- **Upstream of:** **Order** (an order's trip — emits `TripDeletedEvent` for order integrity).
- **Note:** logically one aggregate although source shards it across `ts-travel` + `ts-travel2`.

### PriceConfig — *Layer 2, terminal downstream*
- **Owns:** `basicPriceRate`, `firstClassPriceRate`.
- **Downstream of:** **TrainType** (`cascade`), **Route** (`cascade`).
- **Upstream of:** nobody (price is read at booking time but not referenced back).
- **Emits:** `PriceConfigDeletedEvent` (likely no consumer).

### Order — *Layer 3, pure downstream sink*
- **Owns:** `boughtDate`, `travelDate`, `travelTime`, `coachNumber`, `seatClass`, `seatNumber`, `status`, `price`.
- **Downstream of (everything it references):**
  - **User** — `accountId` reference (integrity: `UserDeletedEvent`).
  - **Contacts** — projected snapshot `contactsName / documentType / contactsDocumentNumber` (integrity: `ContactsDeletedEvent`).
  - **Station** — projected snapshot `from / to` (integrity: `StationDeletedEvent`).
  - **TrainType** — `trainNumber` reference (integrity: `TrainTypeDeletedEvent`).
  - **Trip** — implied by `trainNumber` + date (integrity: `TripDeletedEvent`).
- **Upstream of:** nobody.
- **Recommended snapshot policy:** **freeze** contact/station/train projections at booking →
  subscribe only to the `*DeletedEvent`s (integrity), not the `*UpdatedEvent`s.

---

## Cross-cutting: the booking saga (orchestration, *not* a reference)

`createOrder` (source `ts-preserve-service`) is a **Sagas workflow**, not a static reference:
find Contacts → check seat availability for Trip+date+seatClass → dispatch a seat → create
Order. **Seat availability/dispatch is a cross-aggregate *computation*** (count sold Orders for
a Trip vs TrainType capacity) with no clean reference/invariant home — it needs a Sagas
guard/forbidden-state check inside `createOrder`. Security/payment/assurance/food/consign steps
are out of booking-core scope.

---

## At-a-glance reference matrix

| Downstream ↓ \ Upstream → | User | Station | TrainType | Route | Contacts | Trip |
|---------------------------|:----:|:-------:|:---------:|:-----:|:--------:|:----:|
| **Contacts** | ✓ cascade | | | | | |
| **Route** | | ✓ prevent (name) | | | | |
| **Trip** | | ✓ prevent (name) | ✓ prevent (name) | ✓ prevent (id) | | |
| **PriceConfig** | | | ✓ cascade (name) | ✓ cascade (id) | | |
| **Order** | ✓ prevent | ✓ prevent (proj.) | ✓ prevent (name) | | ✓ prevent (proj.) | ✓ integrity |

*(name) = soft reference by `name`, not aggregateId — see the reference-by-name gap in `03`/`04`.*
