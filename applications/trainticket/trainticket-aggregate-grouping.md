# TrainTicket — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

This file captures the aggregate partitioning decision for the [TrainTicket domain model](trainticket-domain-model.md).

The nine entities are placed in eight aggregates whose boundaries are **TrainTicket's own service
boundaries**, one aggregate per data-owning service in the book-a-ticket flow. `RouteStation` is the
only entity that is not an aggregate root: it is a value object owned by `Route`.

Choosing the benchmark's boundaries rather than inventing new ones means the partitioning is not a
degree of freedom this thesis exercised — it is inherited.

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Service |
|---|---|---|---|
| Station | A stop on the network, with the dwell time a train spends there. Referenced by routes; shared across many of them. | Station | StationService |
| Route | An ordered sequence of stations with cumulative distances from the origin. The distances are what fares are computed from. | Route, RouteStation | RouteService |
| TrainType | A class of rolling stock: how many seats it has in each class and how fast it travels. Its seat counts are the capacity every booking is checked against. | TrainType | TrainTypeService |
| PriceConfig | The per-distance fare rates for one route operated by one train type. Its identity depends on both. | PriceConfig | PriceConfigService |
| Trip | A scheduled service: a train type running a route, departing and arriving at fixed times of day. A template — a concrete journey is a `(Trip, travelDate)` pair. | Trip | TripService |
| User | An account holder, with credentials and identity document. | User | UserService |
| Contacts | A passenger identity record owned by an account. One account may hold several, and books tickets for any of them. | Contacts | ContactsService |
| Order | A purchased ticket for one passenger on one journey. Freezes the fare, the seat, the passenger identity and the departure moment at purchase, and carries the ticket through its lifecycle. | Order | OrderService |

---

## §2 — Snapshots

Field names below match the entity attributes declared in §1 of the [domain model](trainticket-domain-model.md).

> **No version fields.** Every row's "Updated on event" column reads `n/a`, so no aggregate ever
> builds an `EventSubscription`, and no aggregate caches a publisher `version` alongside an id. This
> is a direct consequence of the consistency policy in §3 below.

> **Snapshots are minimal by design.** A cached copy that is never refreshed, whose source is
> mutable, is stale data waiting to be read. This application therefore caches only two things:
> values the benchmark genuinely copies into storage, and the ids needed to fetch a source live.
> Everything a rule depends on is fetched by the saga at operation time — which is also what
> TrainTicket does, since it holds no local caches at all and calls the owning service over HTTP on
> every request.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Route / RouteStation × N | Station | `stationAggregateId`, `stationName` | n/a — no cascade (see §3) |
| Trip | Route | `routeAggregateId` | n/a — no cascade (see §3) |
| Trip | TrainType | `trainTypeAggregateId` | n/a — no cascade (see §3) |
| PriceConfig | Route | `routeAggregateId` | n/a — no cascade (see §3) |
| PriceConfig | TrainType | `trainTypeAggregateId` | n/a — no cascade (see §3) |
| Contacts | User | `userAggregateId` | n/a — no cascade (see §3) |
| Order | Trip | `tripAggregateId`, `tripNumber`, `departureTime` | n/a — frozen contract |
| Order | Contacts | `contactsAggregateId`, `contactsName`, `contactsDocumentType`, `contactsDocumentNumber` | n/a — frozen contract |
| Order | User (account) | `userAggregateId` | n/a — frozen contract |
| Order | Route (endpoints) | `fromStationName`, `toStationName` | n/a — frozen contract |
| Order | PriceConfig (fare) | `price` | n/a — frozen contract |

> **`departureTime` is derived, not copied.** It is the only entry in the "Fields cached" column that
> is not a field of the source aggregate: it is `Order.travelDate` combined with the `Trip.startTime`
> in force at purchase, computed by the booking saga and frozen on the Order. It is listed on the Trip
> row because Trip is where its non-local half comes from.

> **`RouteStation.stationName` is a real copy.** TrainTicket's `Route` stores station **names**, not
> ids — `List<String> stations` — and every consumer matches on the name. Caching the name alongside
> the id preserves that, and it is the only collection snapshot in the application.

> **`n/a — frozen contract` is a different claim from `n/a — no cascade`.** The Order rows are not
> stale caches that this application declines to refresh; they are contract terms. The fare, the
> passenger's name and document, the endpoints and the departure moment are what the customer bought,
> and they must not change when a price rate is edited or a contact record is corrected afterwards.
> Those fields are Java `final` on `Order`. The upstream rows, by contrast, *would* want refreshing
> and do not get it — that is the benchmark behaviour reproduced in §3.

---

## §3 — Upstream / Downstream Event Dependencies

```
Station ──────────────────────────► Route
Route ────────────────────────────► Trip
Route ────────────────────────────► PriceConfig
TrainType ────────────────────────► Trip
TrainType ────────────────────────► PriceConfig
User ─────────────────────────────► Contacts
Trip ─────────────────────────────► Order
Route ────────────────────────────► Order
PriceConfig ──────────────────────► Order
Contacts ─────────────────────────► Order
User ─────────────────────────────► Order
```

> **`Route ──► Order` and `PriceConfig ──► Order` are fare-and-endpoint arrows.** The booking saga
> reads the Trip's Route to resolve `fromStationName` / `toStationName` and validate their ordering,
> and reads the PriceConfig to compute `price`. Order stores the results, so the arrows exist even
> though Order holds no `routeAggregateId` or `priceConfigAggregateId` — what it caches from those
> two aggregates is contract values, not references.

> An arrow `A ──► B` means B holds a reference to A and caches the fields listed in §2. **In this
> application no arrow carries an event subscription.** Every snapshot is seeded once, at the
> consuming aggregate's creation, by a direct service call in the creating saga, and is never
> refreshed thereafter.

### Consistency policy: no cascade

This is a deliberate decision, and the most consequential one in this file. It reproduces the
benchmark faithfully, and it is recorded as finding **F2** in the domain model's preamble.

TrainTicket propagates nothing. `deleteStation` removes a station that routes still list by name;
`deleteTravel` removes a trip that orders still reference by `trainNumber`; `deletePrice` and
`deleteTrain` behave the same way; and lowering `TrainType.economyClass` below the number of seats
already sold for a departure is accepted silently. No service subscribes to another's changes,
because no service publishes any.

Three consequences follow, and they are the reason this file states the policy rather than leaving it
implicit:

1. **§4 is empty.** The application defines no domain events.
2. **No rule in the domain model is P2.** Every §3.2 rule is phrased as a precondition that held at
   operation time, and is enforced by a saga fetch that fails when it is unmet (**P4a**), by a
   service guard over a saga-assembled DTO or the aggregate's own table (**P3**), or by the saga
   passing one computed value into the aggregate it constructs (**P4b**).
3. **References can dangle, and that is the specified behaviour.** An `Order` may reference a
   soft-deleted `Trip`; a `Route` may list a soft-deleted `Station`. Nothing detects this and nothing
   repairs it. Reads return the frozen local copy.

---

## §4 — Events

**This application publishes no domain events.**

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| — | — | — | — | — |

> The table is intentionally empty; see §3 § "Consistency policy: no cascade". Since there are no
> events, there are no `EventSubscription` subclasses, no `getEventSubscriptions()` overrides
> returning anything but an empty list, and no inter-invariant handlers in any aggregate.

---

## §5 — Cross-file consistency notes

Points where these two files constrain each other, recorded so they get re-checked when either changes:

- **A cascade variant is the planned follow-up.** The template allows several grouping files per
  domain model. A second file over this *same* domain model, adding event propagation for reference-
  data change and deletion, is planned. It would give two implementations of one domain differing
  only in consistency policy — a sharper comparison than either alone, and the only way this domain
  exercises P2. If that file is written, §3.2 of the domain model must be re-read: rules currently
  phrased as "held when the operation ran" become standing invariants over live references, and
  several move from P4a/P3 to P2.
- **Empty §4 means session `d` is never generated.** Session `d` produces
  `{Aggregate}InterInvariantTest.groovy`, which tests event subscriptions. `docs/workflow.md` sets the
  `d` checkbox "only for aggregates that have a non-empty Events subscribed list", and
  `classify-and-plan` § "Step 8" omits the section entirely when that list is empty — so no aggregate
  here gets a session `d` at all. Phase 2 is **24 sessions (8 × `a b c`), not 32 with eight empty
  ones**. The cost is that the T3 Subscription test type goes unexercised, which is the acknowledged
  price of the no-cascade decision, not an oversight, and it is what the cascade variant above would
  recover.

- **`SearchTrips` is scheduled before the aggregate it reads.** It is Trip-primary, so it lands in
  Trip's session `b`, but it reads Order, which the topological sort places last. `classify-and-plan`
  § "Step 5.5" detects reverse dependencies only for P3/P4a *rules* bound to write guards, so nothing
  in the generated plan catches a *read* functionality in the same position. `SearchTrips` must be
  deferred to a revisit session after Order's session `c`; the domain model's §4 marks it `⚠️`. Every
  other Trip read is implementable in session `b` as normal.
- **Order's immutability is what makes empty §4 safe.** Every field `Order` copies from another
  aggregate is Java `final`. If a later change makes any of them mutable, the frozen-contract
  argument in §2 collapses and those rows need real event subscriptions.
- **Seat capacity is enforced inside one aggregate.** `SEAT_CAPACITY_NOT_EXCEEDED`,
  `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` and `SEAT_NUMBER_WITHIN_CAPACITY` all resolve against the Order
  aggregate's own table; only the capacity *limit* crosses a boundary, and the booking saga passes it
  in once from the Trip's TrainType, serving all three. The seat number itself is allocated by the
  saga as the lowest free value in `[1, capacity]`, so the allocation and the rules that bound it read
  the same rows. Introducing a seat-inventory aggregate later would move all three from P3 to P1 and
  would add an aggregate the benchmark does not have.
- **`PriceConfig` is the only aggregate keyed on two foreign aggregates.**
  `UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE` is an own-table uniqueness check over the
  `(routeAggregateId, trainTypeAggregateId)` pair. Co-locating `PriceConfig` inside `Route` in a
  future grouping would turn it into a P1 intra-invariant and remove one aggregate.
- **`Trip` is a template; `Order` carries the date.** Nothing in `Trip` is per-departure. Every rule
  about a concrete journey — capacity, seat uniqueness, the refund window — keys on
  `(tripAggregateId, travelDate)` held by `Order`. A future grouping that introduces a per-departure
  aggregate would move those keys out of `Order`.

---
