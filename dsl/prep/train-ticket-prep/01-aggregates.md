# 01 — Aggregates & Roots (train-ticket, booking core)

Scope: booking-core services only — `ts-user`, `ts-contacts`, `ts-station`, `ts-train`,
`ts-route`, `ts-travel`(+`ts-travel2`), `ts-price`, `ts-order`(+`ts-order-other`).
Source of truth: JPA `@Entity` classes under each service's `entity/` package.

> **Architecture note:** train-ticket is a *synchronous REST/Feign* system with **no event
> bus and no event sourcing**. Each service owns one DB table. There are therefore no
> source events to read — all events in `03-events.md` are **inferred** from the projection
> and lifecycle needs of the Nebula model. See ⚠ Gaps in `03` and `SUMMARY.md`.

8 aggregates identified.

## User
- **Root:** `User` (`ts-user-service`, `user.entity.User`)
- **Why root:** owns identity (`userId`) and full lifecycle of an account; no parent entity.
- **Contained entities:** none.
- **Purpose:** A registered account holder who books trips.

## Contacts
- **Root:** `Contacts` (`ts-contacts-service`, `contacts.entity.Contacts`)
- **Why root:** owns its own `id` and lifecycle; belongs to a `User` by `accountId` but is
  independently created/edited/deleted.
- **Contained entities:** none. (`DocumentType` is an enum, not an entity.)
- **Purpose:** A passenger/traveller record (name + ID document) attached to an account;
  the "for whom" of an order.

## Station
- **Root:** `Station` (`ts-station-service`, `fdse.microservice.entity.Station`)
- **Why root:** standalone reference entity with its own `id`; `name` is unique.
- **Contained entities:** none.
- **Purpose:** A physical station, identified by a normalised (lowercased, space-stripped)
  unique `name`, with a dwell `stayTime`.

## TrainType
- **Root:** `TrainType` (`ts-train-service`, `train.entity.TrainType`)
- **Why root:** standalone reference entity; `name` unique.
- **Contained entities:** none.
- **Purpose:** A class of train (e.g. `G1234`) defining seat capacities and average speed.

## Route
- **Root:** `Route` (`ts-route-service`, `route.entity.Route`)
- **Why root:** owns `id` and the ordered station/distance lists.
- **Contained entities:** none. (`stations`/`distances` are `@ElementCollection` value
  lists, not separate aggregates. `RouteInfo` is a request DTO, not persisted.)
- **Purpose:** An ordered sequence of stations with inter-station distances, from a start to
  an end station.

## Trip
- **Root:** `Trip` (`ts-travel-service`, `travel.entity.Trip`)
- **Why root:** the only persisted JPA entity in the travel services; owns `id` + embedded
  `TripId`. `Travel` and `TripAllDetail` are request/response DTOs, not persisted.
- **Contained entities:** `TripId` (embedded value: `type` enum + `number`).
- **Purpose:** A scheduled run of a train type along a route, with start/terminal stations
  and times.
- ⚠ **Gap (shard duplication):** `ts-travel2-service` defines a **field-identical** `Trip`
  entity. The two services shard trips by train-type prefix; they are one logical aggregate.
  Model as a single `Trip`. Confirm no schema divergence before generating.

## PriceConfig
- **Root:** `PriceConfig` (`ts-price-service`, `price.entity.PriceConfig`)
- **Why root:** standalone entity with its own `id`; unique on (`trainType`,`routeId`).
- **Contained entities:** none.
- **Purpose:** Per (train-type, route) pricing rates for basic and first-class fares.

## Order
- **Root:** `Order` (`ts-order-service`, `order.entity.Order`)
- **Why root:** owns `id` and the full order lifecycle/status.
- **Contained entities:** none persisted. (`OrderInfo` = query DTO; `OrderAlterInfo` =
  alter-request DTO carrying a nested `Order`.)
- **Purpose:** A booked ticket: who (account + contact), which trip/seat, dates, price, status.
- ⚠ **Gap (shard duplication):** `ts-order-other-service` defines a **field-identical**
  `Order`. Same shard pattern as Trip. Model as a single `Order`.

---

## Not an aggregate (flagged)

- **Seat** (`ts-seat-service`) — ⚠ **Gap:** no persisted `@Entity`. It is a *computational*
  service that derives seat availability/allocation on the fly from existing `Order` rows +
  `Trip`/`Route`/`TrainType` data. There is no `Seat` aggregate to scaffold. Seat
  availability is a **cross-aggregate computed rule** (see `04`/`05` gaps), not owned state.
