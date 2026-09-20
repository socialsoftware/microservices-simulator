# TrainTicket — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

> **Provenance note — 2026-09-19.** This file and
> [`trainticket-domain-model.md`](trainticket-domain-model.md) were re-partitioned after the run that
> produced this application, so that the pair is one *plain domain* plus one *aggregate grouping*
> over it. Everything this file gained — the aggregate-to-service mapping, the consistency-policy
> preamble that used to open domain §3.2, §2.b, the §5 Rule realisation table and several notes —
> moved here from the domain model unchanged in substance. **Nothing the pair specifies changed**,
> and the application was not regenerated.

This file captures **one** aggregate partitioning of the [TrainTicket domain model](trainticket-domain-model.md).
Several such files may exist over that one domain; writing a second must require no edit to it.

The nine entities are placed in eight aggregates whose boundaries are **TrainTicket's own service
boundaries**, one aggregate per data-owning service in the book-a-ticket flow:

| Aggregate | TrainTicket service |
|---|---|
| Station | `ts-station-service` |
| Route | `ts-route-service` |
| TrainType | `ts-train-service` |
| PriceConfig | `ts-price-service` |
| Trip | `ts-travel-service` |
| Contacts | `ts-contacts-service` |
| User | `ts-user-service` |
| Order | `ts-order-service` |

The five services in the flow that own no data — `ts-preserve-service`, `ts-basic-service`,
`ts-seat-service`, `ts-cancel-service` and `ts-execute-service` — become saga steps rather than
aggregates. `RouteStation` is the only entity that is not an aggregate root: this grouping
co-locates it with `Route`.

Choosing the benchmark's boundaries rather than inventing new ones means the partitioning is not a
degree of freedom this thesis exercised — it is inherited.

> **References are by aggregate id, not by natural key.** TrainTicket references entities by name
> (`Route.stations` holds station *names*, `Trip.trainTypeName` holds a type *name*,
> `Order.trainNumber` holds a trip identifier string). The simulator identifies aggregates by
> `Integer aggregateId`, so every cross-aggregate reference in this realisation becomes an id, with
> the human-readable name cached alongside it only where the benchmark genuinely stores a copy. This
> is a realisation decision: the domain model states the references, not their encoding.

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Snapshot value objects | Service |
|---|---|---|---|---|
| Station | A stop on the network, with the dwell time a train spends there. Referenced by routes; shared across many of them. | Station | — | StationService |
| Route | An ordered sequence of stations with cumulative distances from the origin. The distances are what fares are computed from. | Route, RouteStation | — | RouteService |
| TrainType | A class of rolling stock: how many seats it has in each class and how fast it travels. Its seat counts are the capacity every booking is checked against. | TrainType | — | TrainTypeService |
| PriceConfig | The per-distance fare rates for one route operated by one train type. Its identity depends on both. | PriceConfig | — | PriceConfigService |
| Trip | A scheduled service: a train type running a route, departing and arriving at fixed times of day. A template — a concrete journey is a `(Trip, travelDate)` pair. | Trip | — | TripService |
| User | An account holder, with credentials and identity document. | User | — | UserService |
| Contacts | A passenger identity record owned by an account. One account may hold several, and books tickets for any of them. | Contacts | — | ContactsService |
| Order | A purchased ticket for one passenger on one journey. Freezes the fare, the seat, the passenger identity and the departure moment at purchase, and carries the ticket through its lifecycle. | Order | — | OrderService |

> **No snapshot value objects.** This grouping introduces no class that is not a domain entity.
> `RouteStation` is a domain entity (domain §1), co-located inside `Route` here, and it carries the
> cached `Station` fields listed in §2 — it is not a snapshot class this grouping invented. The
> fields this grouping adds to it are `stationAggregateId: Integer` and `stationName: String`,
> alongside its domain attributes `sequence: Integer` and `distanceFromStart: Integer`.

---

## §2 — Snapshots

Field names below match the entity attributes declared in §1 of the [domain model](trainticket-domain-model.md),
except for the `*AggregateId` fields, which exist only in this realisation.

> **No version fields.** Every row's "Updated on event" column reads `n/a`, so no aggregate ever
> builds an `EventSubscription`, and no aggregate caches a publisher `version` alongside an id. This
> is a direct consequence of the consistency policy in §3.a below.

> **Snapshots are minimal by design.** A cached copy that is never refreshed, whose source is
> mutable, is stale data waiting to be read. This application therefore caches only two things:
> values the benchmark genuinely copies into storage, and the ids needed to fetch a source live.
> Everything a rule depends on is fetched by the saga at operation time — which is also what
> TrainTicket does, since it holds no local caches at all and calls the owning service over HTTP on
> every request.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Route / RouteStation × N | Station | `stationAggregateId`, `stationName` | n/a — no cascade (see §3.a) |
| Trip | Route | `routeAggregateId` | n/a — no cascade (see §3.a) |
| Trip | TrainType | `trainTypeAggregateId` | n/a — no cascade (see §3.a) |
| PriceConfig | Route | `routeAggregateId` | n/a — no cascade (see §3.a) |
| PriceConfig | TrainType | `trainTypeAggregateId` | n/a — no cascade (see §3.a) |
| Contacts | User | `userAggregateId` | n/a — no cascade (see §3.a) |
| Order | Trip | `tripAggregateId`, `tripNumber`, `departureTime` | n/a — frozen contract |
| Order | Contacts | `contactsAggregateId`, `contactsName`, `contactsDocumentType`, `contactsDocumentNumber` | n/a — frozen contract |
| Order | User (account) | `userAggregateId` | n/a — frozen contract |
| Order | Route (endpoints) | `fromStationName`, `toStationName` | n/a — frozen contract |
| Order | PriceConfig (fare) | `price` | n/a — frozen contract |

> **Single-reference snapshots live directly on the aggregate.** Where an aggregate holds exactly
> one reference to another (`Trip → Route`, `Trip → TrainType`, `PriceConfig → Route`,
> `PriceConfig → TrainType`, `Contacts → User`, `Order → Trip`, `Order → Contacts`,
> `Order → User`), the cached id is a field of the aggregate root. `RouteStation` carries the fields
> for the one **collection** reference.

> **`departureTime` is derived, not copied.** It is the only entry in the "Fields cached" column that
> is not a field of the source aggregate: it is `Order.travelDate` combined with the `Trip.startTime`
> in force at purchase, computed by the booking saga and frozen on the Order. It is listed on the Trip
> row because Trip is where its non-local half comes from.

> **`RouteStation.stationName` is a real copy.** TrainTicket's `Route` stores station **names**, not
> ids — `List<String> stations` — and every consumer matches on the name. Caching the name alongside
> the id preserves that, and it is the only collection snapshot in the application.

> **`n/a — frozen contract` is a different claim from `n/a — no cascade`.** The Order rows are not
> stale caches that this application declines to refresh; they are the domain's contract terms
> (domain §1, "`Order` is a frozen contract"), and they would be fields of `Order` under any
> grouping. The upstream rows, by contrast, *would* want refreshing and do not get it — that is the
> benchmark behaviour reproduced in §3.a.

---

## §2.b — Technical fields

**None.** No aggregate in this grouping carries a field that exists for implementation reasons only.

> **Why no `lastModifiedTime`.** Every field `Order` copies from another aggregate is Java `final`,
> so no aggregate here needs a modification timestamp to decide whether a cached value is current.
> The frozen-contract argument in §2 is what buys that; if a later change makes any of those fields
> mutable, it goes away.

> **Soft-delete state.** The simulator's `Aggregate` base class provides `state: AggregateState`
> (`ACTIVE`, `INACTIVE`, `DELETED`) and sets it via `remove()`. It is not a domain attribute and
> appears in no entity row of the domain model. Wherever a domain rule says "`X` has been removed",
> this realisation resolves it against that inherited field (`X.state == DELETED`).

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
> consuming aggregate's creation, and is never refreshed thereafter.
>
> **Not every arrow is a saga fetch.** Four are seeded from a value the caller supplies, because the
> rule that would have made the creating operation fetch the source was dropped at the §9 review for
> having no benchmark support: `User ──► Contacts`, `User ──► Order`, `Route ──► PriceConfig` and
> `TrainType ──► PriceConfig`. `CreateContacts` and `CreatePriceConfig` are therefore
> single-aggregate writes, and `PreserveTicket` does not read User. The reference is still stored and
> the arrow still holds; nothing validates that it points at a live aggregate, which is the same
> dangling-reference behaviour §3.a specifies everywhere else.

### 3.a — Consistency policy: no cascade

This is a deliberate decision, and the most consequential one in this file. It reproduces the
benchmark faithfully, and it is recorded as finding **F2** in the domain model's preamble.

TrainTicket propagates nothing. `deleteStation` removes a station that routes still list by name;
`deleteTravel` removes a trip that orders still reference by `trainNumber`; `deletePrice` and
`deleteTrain` behave the same way; and lowering `TrainType.economyClass` below the number of seats
already sold for a departure is accepted silently. No service subscribes to another's changes,
because no service publishes any.

**What this does to the domain's §3.2 rules.** The domain model states every cross-entity rule as a
standing invariant over live references. Under this grouping **none of them is maintained as one**:
with nothing propagating, a rule phrased as "`Order.trip` has not been removed" would be
unenforceable, because no mechanism informs Order when the Trip goes. Every such rule is instead
realised as a **precondition checked at operation time** and is not restored afterwards — see the
Rule realisation table in §5 for the per-rule record. The rules that fall entirely inside one
aggregate are unaffected and hold transactionally.

Four consequences follow, and they are the reason this file states the policy rather than leaving it
implicit:

1. **§4 is empty.** The application defines no domain events.
2. **No rule in this application is P2.** Each cross-aggregate rule is enforced by a saga fetch that
   fails when the precondition is unmet (**P4a**), by a service guard over a saga-assembled DTO or
   the aggregate's own table (**P3**), or by the saga passing one computed value into the aggregate
   it constructs (**P4b**).
3. **References can dangle, and that is the specified behaviour.** An `Order` may reference a
   removed `Trip`; a `Route` may list a removed `Station`. Nothing detects this and nothing
   repairs it. Reads return the frozen local copy.
4. **A precondition guard is phrased pre-mutation.** Where a domain invariant is a bound on a count
   over rows the operation is about to add to — `SEAT_CAPACITY_NOT_EXCEEDED` — the guard runs
   *before* the aggregate is mutated, so it is written as `count(existing) < capacity` rather than
   as the domain's post-state `count(all) <= capacity`. Transcribing the post-state form literally
   into a pre-mutation guard would admit `capacity + 1` bookings.

---

## §4 — Events

**This application publishes no domain events.**

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| — | — | — | — | — |

> The table is intentionally empty; see §3.a. Since there are no
> events, there are no `EventSubscription` subclasses, no `getEventSubscriptions()` overrides
> returning anything but an empty list, and no inter-invariant handlers in any aggregate.

---

## §5 — Cross-file notes

### Rule realisation

One row per cross-entity rule in domain §3.2. `intra` means the rule resolves inside a single
aggregate of this grouping — either because the entities are co-located, or because the fields it
needs are carried by a snapshot in §2. `precondition` means it is checked when the operation runs
and is not restored if it is later violated. `eventual` would mean event-propagated; the no-cascade
policy of §3.a means **no rule in this grouping is `eventual`**.

| Rule (domain §3.2) | Realisation | Note |
|---|---|---|
| ROUTE_HAS_AT_LEAST_TWO_STATIONS | intra | `RouteStation` is co-located with `Route` |
| ROUTE_SEQUENCE_CONTIGUOUS | intra | co-located |
| ROUTE_DISTANCES_MONOTONIC | intra | co-located |
| ROUTE_FIRST_DISTANCE_IS_ZERO | intra | co-located |
| ROUTE_STATIONS_DISTINCT | intra | resolves on the cached `stationAggregateId` of each `RouteStation` |
| ROUTE_ENDPOINTS_MATCH_STATION_LIST | intra | resolves on the cached `stationName` of each `RouteStation` |
| STATIONS_EXIST (Route) | precondition | the Station fetch in `CreateRoute` / `UpdateRoute` fails when a station is gone; a later `DeleteStation` is not propagated |
| UNIQUE_STATION_NAME | intra | own-table uniqueness inside the Station aggregate |
| ROUTE_AND_TRAIN_TYPE_EXIST (Trip) | precondition | the Route and TrainType fetches in `CreateTrip` |
| UNIQUE_TRIP_NUMBER | intra | own-table uniqueness inside the Trip aggregate |
| UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE | intra | own-table uniqueness over the cached `(routeAggregateId, trainTypeAggregateId)` pair |
| UNIQUE_USER_NAME | intra | own-table uniqueness inside the User aggregate |
| TRIP_EXISTS (Order) | precondition | the Trip fetch in `PreserveTicket` |
| CONTACTS_EXIST (Order) | precondition | the Contacts fetch in `PreserveTicket` |
| CONTACTS_BELONG_TO_ACCOUNT (Order) | precondition | compares the requested account against the one on the fetched Contacts; a later re-assignment is not propagated |
| ENDPOINTS_ON_TRIP_ROUTE (Order) | precondition | resolves against the Route fetched by `PreserveTicket`; a later `UpdateRoute` is not propagated |
| PRICE_CONFIG_EXISTS (Order) | precondition | the `GetPriceConfigByRouteAndTrainType` fetch, which fails when no configuration exists for the pair; the domain states it as its own rule so that this saga step has a rule name to cite, as `docs/concepts/rule-enforcement-patterns.md` § P4 requires |
| PRICE_MATCHES_TARIFF (Order) | precondition | the fare is computed by the booking saga from the fetched Route and PriceConfig and frozen on the Order; a later rate change is not propagated |
| DEPARTURE_TIME_MATCHES_TRIP (Order) | precondition | `departureTime` is computed by the saga from the fetched Trip and frozen |
| SEAT_CAPACITY_NOT_EXCEEDED | precondition | counted over the Order aggregate's own table, with the capacity limit passed in from the Trip's TrainType; phrased pre-mutation — see §3.a consequence 4 |
| SEAT_NUMBER_UNIQUE_PER_DEPARTURE | intra | counted over the Order aggregate's own table |
| SEAT_NUMBER_WITHIN_CAPACITY (Order) | precondition | tested against the capacity scalar the booking saga passes in |

> **This table is a policy record, not a classification.** Which pattern (P1–P4) each rule gets is
> derived from it by `/classify-and-plan`, not stated in it.

### Other notes

- **A cascade variant is the planned follow-up.** Several grouping files may exist over one plain
  domain. A second file over this *same* domain model, adding event propagation for reference-data
  change and deletion, is planned. It would give two implementations of one domain differing only in
  consistency policy — a sharper comparison than either alone, and the only way this domain
  exercises P2. It requires **no edit to the domain model**: the invariants are already stated as
  standing invariants there, and all that changes is this file's §3.a and the Realisation column
  above, where the `precondition` rows become `eventual`. Writing it is the falsification test of
  the plain-domain split.
- **`CONTACTS_BELONG_TO_ACCOUNT` declares no User read.** Both sides of its predicate are local to
  the booking saga: the account comes from the request and the contact's account from the Contacts
  fetch that `PreserveTicket` already declares. Listing User among the rule's entities would make
  `/classify-and-plan` raise a cross-aggregate prerequisite with no operation to satisfy it, which
  is why the domain model's note says the account appears only as a shared reference target.
- **`ENDPOINTS_ON_TRIP_ROUTE` declares no Station read.** The predicate resolves entirely from
  `RouteStation.stationName` and `RouteStation.sequence`, which the Route fetch already carries, so
  `PreserveTicket` declares no Station read.
- **Empty §4 means session `d` is never generated.** Session `d` produces
  `{Aggregate}InterInvariantTest.groovy`, which tests event subscriptions. `docs/workflow.md` sets the
  `d` checkbox "only for aggregates that have a non-empty Events subscribed list", and
  `classify-and-plan` § "Step 8" omits the section entirely when that list is empty — so no aggregate
  here gets a session `d` at all. Phase 2 is **24 sessions (8 × `a b c`), not 32 with eight empty
  ones**. The cost is that the T3 Subscription test type goes unexercised, which is the acknowledged
  price of the no-cascade decision, not an oversight, and it is what the cascade variant above would
  recover.
- **No functionality is scheduled before an aggregate it reads.** `SearchTrips` was, being
  Trip-primary while reading Order, which the topological sort places last; it was cut at the §9
  review. Phase 2 is therefore 24 sessions with no revisit session, and every read is implementable
  in its primary aggregate's session `b`. If `SearchTrips` returns as the extension §7 of the
  rationale plans, this constraint returns with it, and `classify-and-plan` § "Step 5.5b" is the
  mechanism that catches it.
- **`GetLeftTicketCount` is the application's one read saga.** It assembles state from three
  aggregates without writing anything, and is TrainTicket's `ts-seat-service.getLeftTicketOfInterval`.
  `PreserveTicket` is correspondingly the only multi-aggregate write.
- **Order's immutability is what makes empty §4 safe.** Every field `Order` copies from another
  aggregate is Java `final`. If a later change makes any of them mutable, the frozen-contract
  argument in §2 collapses and those rows need real event subscriptions.
- **Seat capacity is enforced inside one aggregate.** `SEAT_CAPACITY_NOT_EXCEEDED` and
  `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` resolve against the Order aggregate's own table;
  `SEAT_NUMBER_WITHIN_CAPACITY` reads no rows at all and tests the allocated number against a single
  passed-in scalar. Only the capacity *limit* crosses a boundary, and the booking saga passes it in
  once from the Trip's TrainType, serving all three. The seat number itself is allocated by the
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
