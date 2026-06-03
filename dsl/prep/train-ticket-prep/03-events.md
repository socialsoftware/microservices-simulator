# 03 — Events (train-ticket, booking core)

> ⚠ **GLOBAL GAP — no source events.** train-ticket has **no event bus, no event sourcing,
> no `@EventListener`s, no message broker**. All cross-service communication is synchronous
> REST/Feign (see `04`). Therefore **every event below is INFERRED**, not read from source.
> They are derived from two needs in the Nebula model:
>   1. **Projection refresh** — keep snapshotted copies (Order's contact/station/train names)
>      and any cached fields up to date → `*UpdatedEvent`.
>   2. **Referential integrity** — react when a referenced aggregate is deleted →
>      `*DeletedEvent` consumed by inter-invariants (`04`).
>
> Payloads follow the Nebula convention: every event carries `aggregateId` + `version`;
> `*UpdatedEvent`s additionally carry the projected fields actually consumed downstream.

Format per aggregate: **Publishes** (what the Nebula model should emit) / **Subscribes**
(what it consumes). "Consumed by" links to the downstream need.

## User
- **Publishes:**
  - `UserUpdatedEvent { aggregateId, userName }` — consumed by Contacts/Order projections of account data. *(inferred)*
  - `UserDeletedEvent { aggregateId }` — consumed by Contacts, Order integrity. *(inferred)*
- **Subscribes:** none.

## Contacts
- **Publishes:**
  - `ContactsUpdatedEvent { aggregateId, name, documentType, documentNumber }` — consumed by Order projection. *(inferred)*
  - `ContactsDeletedEvent { aggregateId }` — consumed by Order integrity. *(inferred)*
- **Subscribes:**
  - `UserDeletedEvent` from User — integrity / cascade decision (see `04`). *(inferred)*

## Station
- **Publishes:**
  - `StationUpdatedEvent { aggregateId, name }` — consumed by Order `from`/`to` projections. *(inferred)*
  - `StationDeletedEvent { aggregateId }` — consumed by Route, Trip, Order integrity. *(inferred)*
- **Subscribes:** none.

## TrainType
- **Publishes:**
  - `TrainTypeUpdatedEvent { aggregateId, name }` — consumed by Order `trainNumber`, Trip, PriceConfig. *(inferred)*
  - `TrainTypeDeletedEvent { aggregateId }` — consumed by Trip, PriceConfig, Order integrity. *(inferred)*
- **Subscribes:** none.

## Route
- **Publishes:**
  - `RouteUpdatedEvent { aggregateId, startStation, endStation }` — consumed by Trip/PriceConfig if they cache route data. *(inferred)*
  - `RouteDeletedEvent { aggregateId }` — consumed by Trip, PriceConfig integrity. *(inferred)*
- **Subscribes:**
  - `StationDeletedEvent` from Station — a route references stations by name. *(inferred — see ⚠ below)*

## Trip
- **Publishes:**
  - `TripDeletedEvent { aggregateId }` — consumed by Order integrity (an order's trip vanished). *(inferred)*
- **Subscribes:**
  - `TrainTypeDeletedEvent` from TrainType — integrity. *(inferred)*
  - `RouteDeletedEvent` from Route — integrity. *(inferred)*
  - `StationDeletedEvent` from Station — start/terminal/intermediate references. *(inferred)*

## PriceConfig
- **Publishes:**
  - `PriceConfigDeletedEvent { aggregateId }` — *(inferred; likely no downstream consumer)*
- **Subscribes:**
  - `TrainTypeDeletedEvent` from TrainType — integrity. *(inferred)*
  - `RouteDeletedEvent` from Route — integrity. *(inferred)*

## Order
- **Publishes:**
  - `OrderCreatedEvent` / `OrderStatusChangedEvent` — *(inferred; only needed if payment/
    consign/assurance services are later brought into scope — out of booking-core)*.
- **Subscribes:**
  - `UserDeletedEvent` from User — integrity. *(inferred)*
  - `ContactsDeletedEvent` from Contacts — integrity. *(inferred)*
  - `ContactsUpdatedEvent` from Contacts — refresh projected contact snapshot (if not frozen). *(inferred)*
  - `StationDeletedEvent` / `StationUpdatedEvent` from Station — integrity / projection. *(inferred)*
  - `TrainTypeDeletedEvent` / `TrainTypeUpdatedEvent` from TrainType — integrity / projection. *(inferred)*
  - `TripDeletedEvent` from Trip — integrity. *(inferred)*

---

## ⚠ Gaps
1. **All events inferred** — none exist in source; validate the set against intended
   behaviour before writing `.nebula`. Over-modelling events adds wiring cost.
2. **Reference-by-name** (Station/TrainType) complicates `*UpdatedEvent` projection: if a
   station/train is renamed, every downstream copy keyed by name must update. Source never
   handles this (names are effectively immutable in practice). Decide: (a) treat names as
   immutable → drop the `*UpdatedEvent` projections, or (b) carry both aggregateId + name.
3. **Order snapshot freeze** — if Order's contact/station/train projections are *frozen at
   booking* (recommended, matches source), Order should **not** subscribe to the
   corresponding `*UpdatedEvent`s; keep only `*DeletedEvent` integrity subscriptions.
4. **Seat availability is not event-driven** in source; it is recomputed per request. No
   event models it. See `04`/`05`.
