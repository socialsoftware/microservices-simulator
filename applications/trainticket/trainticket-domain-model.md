# TrainTicket — Domain Model

> Follows the structure defined in [`docs/templates/domain-model-template.md`](../../docs/templates/domain-model-template.md).

**How to use this file:**
1. Read the preamble to understand what this application is a subset of, and which rules are the benchmark's and which are this file's.
2. Read §1–§2 to understand the entities, their attributes, and how they relate.
3. Read §3 to understand every consistency rule and its predicate.
4. See [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md) for the concrete aggregate partitioning decision and its consistency-policy consequences.

---

## Preamble — scope, provenance, and departures from the benchmark

### What this is a subset of

This application models the **canonical book-a-ticket flow** of the [TrainTicket](https://github.com/FudanSELab/train-ticket)
microservices benchmark, plus the order lifecycle that follows it. It is a deliberate subset: the
benchmark has 40+ services, this application has 8 aggregates.

The 8 aggregates map **1:1 onto TrainTicket's data-owning services** in that flow:

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

Five TrainTicket services in this flow own **no data at all** and therefore become saga steps rather
than aggregates: `ts-preserve-service` (booking orchestration), `ts-basic-service` (assembles
Route + TrainType + PriceConfig to compute a fare), `ts-seat-service` (derives availability by
querying orders), `ts-cancel-service` and `ts-execute-service` (order status transitions).

Deliberately **out of scope**: assurance, food, consign, delivery, voucher, rebook, payment/balance,
notification, auth, and the `ts-order-other-service` / `ts-travel2-service` duplicates.

### Rule provenance

§3 has two tiers, and every rule is traceable to the benchmark:

- **Enforced** — TrainTicket's code checks this somewhere.
- **Implied** — TrainTicket's *data model* requires it for the data to mean anything, but no code
  checks it.

No rule in §3 comes from railway domain knowledge that TrainTicket does not itself contain.

Provenance is recorded here rather than as a column in §3.1 or a marker on the §3.2 headings, because
both of those are positions `/classify-and-plan` parses.

**Enforced tier, §3.1:** `TRIP_NUMBER_FORMAT`, `ORDER_STATUS_TRANSITION`,
`ORDER_SEAT_NUMBER_POSITIVE`, `ORDER_REFUND_AMOUNT`. Every other §3.1 rule is implied.

**Enforced tier, §3.2:** `STATIONS_EXIST`, `ROUTE_AND_TRAIN_TYPE_EXIST` (both blocks),
`ACCOUNT_EXISTS` (both blocks), `TRIP_EXISTS`, `CONTACTS_EXIST`, `ENDPOINTS_ON_TRIP_ROUTE`,
`SEAT_CLASS_OFFERED`, `PRICE_MATCHES_TARIFF`, `SEAT_CAPACITY_NOT_EXCEEDED`,
`SEAT_NUMBER_UNIQUE_PER_DEPARTURE`, `ACCOUNT_ORDER_RATE_LIMIT`. Every other §3.2 rule is implied.

`SEAT_CAPACITY_NOT_EXCEEDED` is enforced-but-corrected: TrainTicket has the check, and the check is
defect **F1** below.

### Findings recorded against the benchmark

Four defects and modelling flaws surfaced while writing this specification. They are recorded here
because they are results, not incidental notes.

- **F1 — the second-class availability check is broken.** `PreserveServiceImpl` rejects a
  second-class booking only when `tripResponse.getEconomyClass() == SeatClass.SECONDCLASS.getCode()
  && tripResponse.getConfortClass() == 0`. `TripResponse.economyClass` holds the *remaining
  second-class ticket count* (`TravelServiceImpl:432`), while `SeatClass.SECONDCLASS.getCode()` is
  the constant `3`. The guard therefore fires only when exactly 3 second-class seats remain *and*
  first class is sold out, and a second-class booking against **zero** remaining seats is accepted.
  `SEAT_CAPACITY_NOT_EXCEEDED` in §3.2 is the corrected rule.
- **F2 — admin deletion leaves dangling references.** `deleteStation`, `deleteTrain`, `deletePrice`,
  `deleteRoute` and `deleteTravel` remove entities that other services still reference by name or id,
  with no propagation of any kind. This application reproduces that behaviour faithfully — see
  §3 of [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md) — which is why every
  §3.2 rule below is phrased as a **precondition that held at operation time**, never as a standing
  invariant over a live reference.
- **F3 — `Route` correlates two lists by index.** `Route` carries `List<String> stations` and
  `List<Integer> distances`, related only by position, with nothing enforcing equal length or
  ordering. This file normalises them into an owned `RouteStation` value object; the ordering
  invariants survive as `ROUTE_DISTANCES_MONOTONIC` and `ROUTE_SEQUENCE_CONTIGUOUS`.
- **F4 — dates and money are `String`.** `Order.price`, `Order.travelDate`, `Order.boughtDate` and
  `Order.travelTime` are all `String`, parsed ad hoc at each use site (`calculateRefund` builds a
  `java.util.Date` from parsed fragments via a deprecated constructor). This file uses
  `BigDecimal`, `LocalDate`, `LocalDateTime` and `LocalTime`.

### Other deliberate departures

- **References are by aggregate id, not by natural key.** TrainTicket references entities by name
  (`Route.stations` holds station *names*, `Trip.trainTypeName` holds a type *name*,
  `Order.trainNumber` holds a trip identifier string). The simulator identifies aggregates by
  `Integer aggregateId`, so every reference becomes an id, with the human-readable name cached
  alongside it only where the benchmark genuinely stores a copy.
- **`Trip` is a schedule template.** `Trip.startTime` / `endTime` are times of day; a concrete
  journey is a `(Trip, travelDate)` pair, which is why seat availability in §3.2 is keyed on
  `(tripAggregateId, travelDate, seatClass)`. This matches
  `OrderRepository.findByTravelDateAndTrainNumber`.
- **The scalper rate limit uses constants, not a `SecurityConfig` aggregate.** TrainTicket stores the
  thresholds as `name`/`value`/`description` rows in `ts-security-service`
  (`max_order_1_hour`, `max_order_not_use`). A generic key-value config table is not domain data, so
  `ACCOUNT_ORDER_RATE_LIMIT` uses fixed constants instead.
- **`Order.coachNumber` is dropped.** The field exists on TrainTicket's `Order` but is never assigned
  anywhere in the booking flow.

---

## §1 — Entities

Each entity lists only its own scalar attributes. Cross-entity references appear in §2. The **Owns**
column lists value objects that live inside this entity's boundary and have no independent identity
(they are created and deleted with the entity).

> **Soft-delete:** Every aggregate inherits `state: AggregateState` from the simulator `Aggregate` base class (values: `ACTIVE`, `INACTIVE`, `DELETED`). This field is **not** a domain attribute and must **not** appear in the entity table. It is set by `remove()` on the base class.

| Entity | Attributes | Owns |
|---|---|---|
| **Station** | `name: String`, `stayTime: Integer` | — |
| **Route** | `startStationName: String`, `endStationName: String` | RouteStation × N |
| **RouteStation** | `sequence: Integer`, `stationAggregateId: Integer`, `stationName: String`, `distanceFromStart: Integer` | — |
| **TrainType** | `name: String` (immutable), `economyClassSeats: Integer`, `firstClassSeats: Integer`, `averageSpeed: Integer` | — |
| **PriceConfig** | `basicPriceRate: BigDecimal`, `firstClassPriceRate: BigDecimal` | — |
| **Trip** | `tripNumber: String` (immutable), `startTime: LocalTime`, `endTime: LocalTime` | — |
| **User** | `userName: String` (immutable), `password: String`, `gender: Gender (NONE \| MALE \| FEMALE \| OTHER)`, `documentType: DocumentType (NONE \| ID_CARD \| PASSPORT \| OTHER)`, `documentNumber: String`, `email: String` | — |
| **Contacts** | `name: String`, `documentType: DocumentType (NONE \| ID_CARD \| PASSPORT \| OTHER)`, `documentNumber: String`, `phoneNumber: String` | — |
| **Order** | `boughtDate: LocalDateTime` (immutable), `travelDate: LocalDate` (immutable), `departureTime: LocalDateTime` (immutable), `tripNumber: String` (immutable), `fromStationName: String` (immutable), `toStationName: String` (immutable), `seatClass: SeatClass (FIRST_CLASS \| SECOND_CLASS)` (immutable), `seatNumber: Integer` (immutable), `contactsName: String` (immutable), `contactsDocumentType: DocumentType` (immutable), `contactsDocumentNumber: String` (immutable), `price: BigDecimal` (immutable), `status: OrderStatus (NOTPAID \| PAID \| COLLECTED \| USED \| CANCELLED)` (default: NOTPAID), `refundAmount: BigDecimal` (default: null), `cancelledTime: LocalDateTime` (default: null) | — |

> **Single-reference snapshots:** where an aggregate holds exactly one reference to an external
> aggregate (`Trip → Route`, `Trip → TrainType`, `PriceConfig → Route`, `PriceConfig → TrainType`,
> `Contacts → User`, `Order → Trip`, `Order → Contacts`, `Order → User`), the cached id is stored
> directly on the aggregate and is defined only in §2 of
> [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md). `RouteStation` above
> carries the cached fields for the one **collection** reference. Field names here and in grouping §2
> are deliberately identical, so drift between the two files is visible.

> **`Order` is a frozen contract.** Every purchased value on `Order` is immutable: the passenger
> identity, the fare, the seat, the endpoints and the departure moment are copied at purchase and
> never track later edits to their sources. Only `status`, `refundAmount` and `cancelledTime` mutate.
> This is what makes the immutability rules in §3.1 enforceable by Java `final` rather than by a
> runtime check, and it is why no `lastModifiedTime` technical field is needed.

> **`departureTime`** is `travelDate` combined with the `Trip.startTime` in force at purchase. It is
> stored so that `ORDER_REFUND_AMOUNT` can compare the cancellation instant against the departure
> instant from purely local state.

> **`cancelledTime`** is a domain value — the instant the cancellation was requested — not a
> technical timestamp. It is set once, at cancellation, and is what `ORDER_REFUND_AMOUNT` predicates
> on, so the refund is deterministic and does not depend on when `verifyInvariants()` happens to run.

---

## §2 — Relationships

The direction is always from the referencing entity to the referenced entity. **Immutable** means the
reference is set at creation and never changed.

| From | To | Cardinality | Immutable |
|---|---|---|---|
| RouteStation | Station | N → 1 | yes |
| Trip | Route | N → 1 | yes |
| Trip | TrainType | N → 1 | yes |
| PriceConfig | Route | N → 1 | yes |
| PriceConfig | TrainType | N → 1 | yes |
| Contacts | User | N → 1 | yes |
| Order | Trip | N → 1 | yes |
| Order | Contacts | N → 1 | yes |
| Order | User (account) | N → 1 | yes |

> **Route → Station is `N → M` in effect** but is expressed as `RouteStation → Station` above, since
> `RouteStation` is the owned value object that carries the position and the cumulative distance. A
> route's station list is mutable (`UpdateRoute` replaces it); an individual `RouteStation`'s
> reference is not.

---

## §3 — Rules

### 3.1 — Single-entity rules

These rules inspect only fields of a single entity.

| Rule | Entity | Predicate |
|---|---|---|
| STATION_STAY_TIME_NON_NEGATIVE | Station | `Station.stayTime >= 0` |
| ROUTE_HAS_AT_LEAST_TWO_STATIONS | Route | `count(Route.routeStations) >= 2` |
| ROUTE_SEQUENCE_CONTIGUOUS | Route | The multiset of `routeStations.sequence` is exactly `0 .. count(routeStations) - 1` |
| ROUTE_DISTANCES_MONOTONIC | Route | `∀ i ∈ 1 .. count(routeStations) - 1: routeStations[i].distanceFromStart > routeStations[i-1].distanceFromStart`, where `[i]` denotes ordering by `sequence` |
| ROUTE_FIRST_DISTANCE_IS_ZERO | Route | The `RouteStation` with `sequence == 0` has `distanceFromStart == 0` |
| ROUTE_STATIONS_DISTINCT | Route | All entries in `Route.routeStations` have distinct `stationAggregateId` |
| ROUTE_ENDPOINTS_MATCH_STATION_LIST | Route | `Route.startStationName == routeStations[0].stationName ∧ Route.endStationName == routeStations[last].stationName` |
| TRAIN_TYPE_NAME_FINAL | TrainType | `TrainType.name` is immutable (Java `final` field) |
| TRAIN_TYPE_SEATS_NON_NEGATIVE | TrainType | `TrainType.economyClassSeats >= 0 ∧ TrainType.firstClassSeats >= 0` |
| TRAIN_TYPE_HAS_SEATS | TrainType | `TrainType.economyClassSeats + TrainType.firstClassSeats > 0` |
| TRAIN_TYPE_SPEED_POSITIVE | TrainType | `TrainType.averageSpeed > 0` |
| PRICE_RATES_POSITIVE | PriceConfig | `PriceConfig.basicPriceRate > 0 ∧ PriceConfig.firstClassPriceRate > 0` |
| TRIP_NUMBER_FINAL | Trip | `Trip.tripNumber` is immutable (Java `final` field) |
| TRIP_NUMBER_FORMAT | Trip | `Trip.tripNumber` matches `^[GDZTK][0-9]+$` — a single train-category letter followed by digits |
| TRIP_START_BEFORE_END | Trip | `Trip.startTime < Trip.endTime` |
| USER_NAME_FINAL | User | `User.userName` is immutable (Java `final` field) |
| USER_DOCUMENT_NUMBER_PRESENT | User | `User.documentType != NONE ⟹ User.documentNumber` is non-blank |
| CONTACTS_DOCUMENT_NUMBER_PRESENT | Contacts | `Contacts.documentType != NONE ⟹ Contacts.documentNumber` is non-blank |
| ORDER_STATUS_TRANSITION | Order | `prev.status → status` is one of: `NOTPAID → {PAID, CANCELLED}`, `PAID → {COLLECTED, CANCELLED}`, `COLLECTED → {USED}`, `USED → {}`, `CANCELLED → {}`; `status == prev.status` is always permitted |
| ORDER_PRICE_POSITIVE | Order | `Order.price > 0` |
| ORDER_SEAT_NUMBER_POSITIVE | Order | `Order.seatNumber >= 1` |
| ORDER_DEPARTURE_AFTER_PURCHASE | Order | `Order.boughtDate <= Order.departureTime` |
| ORDER_CANCELLATION_FIELDS_SET | Order | `Order.status == CANCELLED ⟺ (Order.cancelledTime != null ∧ Order.refundAmount != null)` |
| ORDER_REFUND_AMOUNT | Order | `Order.status == CANCELLED ⟹ Order.refundAmount == (prev.status == NOTPAID ? 0 : (Order.cancelledTime > Order.departureTime ? 0 : Order.price × 0.80))` |

> **Immutability fields:** `ORDER_CONTRACT_FIELDS_FINAL` — `boughtDate`, `travelDate`,
> `departureTime`, `tripNumber`, `fromStationName`, `toStationName`, `seatClass`, `seatNumber`,
> `contactsName`, `contactsDocumentType`, `contactsDocumentNumber`, `price`, and the references to
> Trip, Contacts and User — is enforced by Java `final` fields and by the absence of setters after
> construction. No `verifyInvariants()` check is needed. The same applies to `TRAIN_TYPE_NAME_FINAL`,
> `TRIP_NUMBER_FINAL` and `USER_NAME_FINAL`.

> **`ORDER_REFUND_AMOUNT` is `[E]`, corrected.** TrainTicket's `calculateRefund` implements exactly
> this three-branch rule (`0.00` when unpaid, `0` once the departure has passed, otherwise 80% of the
> fare). The correction is that it builds the departure instant with a deprecated `java.util.Date`
> constructor from separately parsed date and time strings; here `departureTime` is frozen on the
> order at purchase.

---

### 3.2 — Cross-entity rules

> **All rules in this section are preconditions checked at operation time, not standing invariants.**
> This application reproduces TrainTicket's consistency policy faithfully: nothing propagates when
> referenced data changes or is deleted (finding **F2**). A rule phrased as "`Order.trip` references
> a Trip that has not been deleted" would be unenforceable, because no mechanism informs Order.
> Every rule below therefore reads as "held when the operation ran", and is enforced either by a saga
> fetch that fails when the precondition is unmet (**P4a**), by a service guard over a
> saga-assembled DTO or over the aggregate's own table (**P3**), or by the saga passing one computed
> value to the aggregate it constructs (**P4b**). **No rule in this application is P2.**

---

#### Rule: STATIONS_EXIST (Route)
| Field | Value |
|---|---|
| Entities | Route, Station |
| Predicate | `∀rs ∈ Route.routeStations: rs.stationAggregateId named a Station that was ACTIVE when the route was created or updated` |

---

#### Rule: UNIQUE_STATION_NAME
| Field | Value |
|---|---|
| Entities | Station |
| Predicate | No two active Stations share the same `name` |

---

#### Rule: ROUTE_AND_TRAIN_TYPE_EXIST (Trip)
| Field | Value |
|---|---|
| Entities | Trip, Route, TrainType |
| Predicate | `Trip.routeAggregateId` and `Trip.trainTypeAggregateId` named a Route and a TrainType that were ACTIVE when the trip was created |

---

#### Rule: UNIQUE_TRIP_NUMBER
| Field | Value |
|---|---|
| Entities | Trip |
| Predicate | No two active Trips share the same `tripNumber` |

---

#### Rule: ROUTE_AND_TRAIN_TYPE_EXIST (PriceConfig)
| Field | Value |
|---|---|
| Entities | PriceConfig, Route, TrainType |
| Predicate | `PriceConfig.routeAggregateId` and `PriceConfig.trainTypeAggregateId` named a Route and a TrainType that were ACTIVE when the price configuration was created |

---

#### Rule: UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE
| Field | Value |
|---|---|
| Entities | PriceConfig, Route, TrainType |
| Predicate | No two active PriceConfigs share the same `(routeAggregateId, trainTypeAggregateId)` pair — this is the lookup key `queryPriceConfigByRouteIdAndTrainType` assumes is unique |

---

#### Rule: ACCOUNT_EXISTS (Contacts)
| Field | Value |
|---|---|
| Entities | Contacts, User |
| Predicate | `Contacts.userAggregateId` named a User that was ACTIVE when the contact was created |

---

#### Rule: UNIQUE_USER_NAME
| Field | Value |
|---|---|
| Entities | User |
| Predicate | No two active Users share the same `userName` |

---

#### Rule: TRIP_EXISTS (Order)
| Field | Value |
|---|---|
| Entities | Order, Trip |
| Predicate | `Order.tripAggregateId` named a Trip that was ACTIVE when the order was created |

---

#### Rule: CONTACTS_EXIST (Order)
| Field | Value |
|---|---|
| Entities | Order, Contacts |
| Predicate | `Order.contactsAggregateId` named a Contacts that was ACTIVE when the order was created |

---

#### Rule: ACCOUNT_EXISTS (Order)
| Field | Value |
|---|---|
| Entities | Order, User |
| Predicate | `Order.accountAggregateId` named a User that was ACTIVE when the order was created |

---

#### Rule: CONTACTS_BELONG_TO_ACCOUNT (Order)
| Field | Value |
|---|---|
| Entities | Order, Contacts, User |
| Predicate | `Order.accountAggregateId == Contacts.userAggregateId` — a passenger may only be booked under the account that owns that contact record |

---

#### Rule: ENDPOINTS_ON_TRIP_ROUTE (Order)
| Field | Value |
|---|---|
| Entities | Order, Trip, Route, Station |
| Predicate | `Order.fromStationName` and `Order.toStationName` both name a `RouteStation` of the Trip's Route, and `sequence(from) < sequence(to)` |

---

#### Rule: SEAT_CLASS_OFFERED (Order)
| Field | Value |
|---|---|
| Entities | Order, Trip, TrainType |
| Predicate | `capacity(Trip.trainType, Order.seatClass) > 0`, where `capacity` is `firstClassSeats` for `FIRST_CLASS` and `economyClassSeats` for `SECOND_CLASS` |

---

#### Rule: PRICE_MATCHES_TARIFF (Order)
| Field | Value |
|---|---|
| Entities | Order, Trip, Route, PriceConfig |
| Predicate | `Order.price == (distanceFromStart(to) - distanceFromStart(from)) × rate`, where the distances are those of the Trip's Route and `rate` is `PriceConfig.firstClassPriceRate` for `FIRST_CLASS` and `PriceConfig.basicPriceRate` for `SECOND_CLASS`, taken from the PriceConfig for `(Trip.route, Trip.trainType)` |

---

#### Rule: DEPARTURE_TIME_MATCHES_TRIP (Order)
| Field | Value |
|---|---|
| Entities | Order, Trip |
| Predicate | `Order.departureTime == Order.travelDate` at `Trip.startTime` |

---

#### Rule: SEAT_CAPACITY_NOT_EXCEEDED
| Field | Value |
|---|---|
| Entities | Order, Trip, TrainType |
| Predicate | `count(Orders o where o.tripAggregateId == this.tripAggregateId ∧ o.travelDate == this.travelDate ∧ o.seatClass == this.seatClass ∧ o.status != CANCELLED ∧ o.state != DELETED) <= capacity(Trip.trainType, seatClass)` |

> The count is over the Order aggregate's own table, keyed exactly as
> `OrderRepository.findByTravelDateAndTrainNumber`. `capacity` is supplied to the Order service by
> the booking saga, which fetched the Trip's TrainType. TrainTicket's own version of this check is
> defect **F1**.

---

#### Rule: SEAT_NUMBER_UNIQUE_PER_DEPARTURE
| Field | Value |
|---|---|
| Entities | Order, Trip |
| Predicate | No two Orders with `status != CANCELLED ∧ state != DELETED` share the same `(tripAggregateId, travelDate, seatClass, seatNumber)` |

> TrainTicket's `SeatServiceImpl.distributeSeat` picks a random seat number and retries while
> `isContained(soldTickets, seat)`, so seat-number uniqueness within a departure is the property that
> loop is trying to establish. Its segment-reuse optimisation — handing out a seat whose previously
> sold journey ends at or before the new passenger's boarding station — is **not** modelled here;
> this application allocates one seat per journey for the whole trip.

---

#### Rule: ACCOUNT_ORDER_RATE_LIMIT
| Field | Value |
|---|---|
| Entities | Order, User |
| Predicate | For the booking account: `count(Orders with boughtDate within the last hour) <= MAX_ORDERS_PER_HOUR` **and** `count(Orders with status ∈ {NOTPAID, PAID, COLLECTED}) <= MAX_OUTSTANDING_ORDERS`, with `MAX_ORDERS_PER_HOUR = 5` and `MAX_OUTSTANDING_ORDERS = 5` |

> This is TrainTicket's ticket-scalper check (`SecurityServiceImpl`, step 1 of `preserve`). The
> thresholds are the `max_order_1_hour` and `max_order_not_use` `SecurityConfig` rows; they are
> constants here rather than a ninth aggregate, per the preamble.

---

## §4 — Functionalities

> This section is a complete inventory of every operation the application exposes, write and read, one row per operation regardless of how many aggregates it touches.
>
> **Other Aggregates** lists only aggregates the saga itself reads or writes; an empty cell means the operation needs no saga coordination. Because this application publishes no domain events, there are no aggregates reacting asynchronously — the "Other Aggregates" column is the complete picture of cross-aggregate interaction.

| Functionality | Primary Aggregate | Other Aggregates | Kind | Description |
|---|---|---|---|---|
| CreateStation | Station | — | Write | Create a station with its name and dwell time |
| UpdateStation | Station | — | Write | Update a station's name or dwell time |
| DeleteStation | Station | — | Write | Soft-delete a station |
| CreateRoute | Route | Station | Write | Create a route as an ordered list of stations with cumulative distances |
| UpdateRoute | Route | Station | Write | Replace a route's station list and distances |
| DeleteRoute | Route | — | Write | Soft-delete a route |
| CreateTrainType | TrainType | — | Write | Create a train type with its seat counts per class and average speed |
| UpdateTrainType | TrainType | — | Write | Update a train type's seat counts or average speed |
| DeleteTrainType | TrainType | — | Write | Soft-delete a train type |
| CreatePriceConfig | PriceConfig | Route, TrainType | Write | Create the per-distance fare rates for one route and train type |
| UpdatePriceConfig | PriceConfig | — | Write | Update the fare rates of an existing price configuration |
| DeletePriceConfig | PriceConfig | — | Write | Soft-delete a price configuration |
| CreateTrip | Trip | Route, TrainType | Write | Create a scheduled trip on a route with a train type and times of day |
| UpdateTrip | Trip | — | Write | Update a trip's start or end time |
| DeleteTrip | Trip | — | Write | Soft-delete a trip |
| CreateUser | User | — | Write | Create a user account |
| UpdateUser | User | — | Write | Update a user's password, gender, document or email |
| DeleteUser | User | — | Write | Soft-delete a user account |
| CreateContacts | Contacts | User | Write | Create a passenger contact record owned by an account |
| UpdateContacts | Contacts | — | Write | Update a contact's name, document or phone number |
| DeleteContacts | Contacts | — | Write | Soft-delete a contact record |
| PreserveTicket | Order | Trip, Route, TrainType, PriceConfig, Contacts, User | Write | Book a ticket: check the account's rate limit, resolve the passenger contact, validate the journey against the trip's route, compute the fare from distance and rates, allocate a seat within the train type's capacity, and create the order as NOTPAID |
| PayOrder | Order | — | Write | Move an order from NOTPAID to PAID |
| CollectTicket | Order | — | Write | Move a paid order to COLLECTED |
| UseTicket | Order | — | Write | Move a collected order to USED |
| CancelOrder | Order | — | Write | Cancel an unpaid or paid order, stamping the cancellation time and computing the refund |
| GetStationById | Station | — | Read | Retrieve a single station by its aggregate id |
| GetStations | Station | — | Read | List all stations |
| GetRouteById | Route | — | Read | Retrieve a single route by its aggregate id |
| GetRoutes | Route | — | Read | List all routes |
| GetRoutesByStation | Route | — | Read | List the routes that stop at a given station |
| GetTrainTypeById | TrainType | — | Read | Retrieve a single train type by its aggregate id |
| GetTrainTypes | TrainType | — | Read | List all train types |
| GetPriceConfigById | PriceConfig | — | Read | Retrieve a single price configuration by its aggregate id |
| GetPriceConfigs | PriceConfig | — | Read | List all price configurations |
| GetTripById | Trip | — | Read | Retrieve a single trip by its aggregate id |
| GetTrips | Trip | — | Read | List all trips |
| GetUserById | User | — | Read | Retrieve a single user by its aggregate id |
| GetUsers | User | — | Read | List all users |
| GetContactsById | Contacts | — | Read | Retrieve a single contact record by its aggregate id |
| GetContactsByAccount | Contacts | — | Read | List the contact records owned by an account |
| GetOrderById | Order | — | Read | Retrieve a single order by its aggregate id |
| GetOrders | Order | — | Read | List all orders |
| GetOrdersByAccount | Order | — | Read | List the orders placed by an account |
| GetLeftTicketCount | Order | Trip, TrainType | Read | Count the seats still available for a given trip, travel date and seat class, by subtracting the non-cancelled orders from the train type's capacity |
| SearchTrips | Trip | Route, TrainType, PriceConfig, Order | Read | Search the trips serving a departure and arrival station on a travel date, returning for each the departure and arrival times, the fare for both seat classes, and the remaining seats per class |

> **`SearchTrips` and `GetLeftTicketCount` are read sagas.** They assemble state from several
> aggregates without writing anything. `SearchTrips` is TrainTicket's `queryForTravels` /
> `ts-basic-service` pairing; `GetLeftTicketCount` is `ts-seat-service`'s
> `getLeftTicketOfInterval`.

> **`PreserveTicket` is the only multi-aggregate write.** The other four Order operations are
> single-aggregate state transitions, matching TrainTicket's `ts-cancel-service` and
> `ts-execute-service`, both of which do nothing but read an order and write its status.

---
