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

Every assignment below names the site that justifies it. The tiers were re-derived rule by rule
against the pinned commit at the §9 review, which moved seven of them, and again at the §10 review,
which moved one more and re-declared two — see §9 and §10 of the rationale.

**Enforced tier, §3.1:** `ORDER_STATUS_TRANSITION` (`ExecuteServiceImpl` guards
`PAID → COLLECTED` and `COLLECTED → USED`; `CancelServiceImpl` guards `{NOTPAID, PAID} → CANCELLED`),
`ORDER_REFUND_AMOUNT` (`CancelServiceImpl.calculateRefund`, `CancelServiceImpl.java:200`). Every
other §3.1 rule is implied.

**Two rules straddle the tiers**, each with an enforced core and an implied strengthening this file
states on top of it. Both are declared here rather than passed off as wholly enforced.

- `ROUTE_ENDPOINTS_MATCH_STATION_LIST`. `AdminRouteServiceImpl.java:58` rejects a route whose
  declared start or end is absent from the station list, so **containment is enforced**; nothing
  anywhere requires the endpoints to be the **first and last** entries, so the positional
  strengthening is **implied**. It is admitted because `Route.stations` is `@OrderColumn` and
  `Route.startStation` means the origin: unless the first entry is the start, neither representation
  means what its name says.
- `ORDER_STATUS_TRANSITION`. The two `ExecuteServiceImpl` guards and the `CancelServiceImpl` guard
  above are real, so **every edge into `COLLECTED`, `USED` and `CANCELLED` is enforced**. The edge
  into `PAID` is not: `OrderServiceImpl.payOrder` sets `status = PAID` without reading the previous
  status at all, so the benchmark accepts paying an order that is already `CANCELLED` or `USED`.
  This file's table forbids those, and that half is **implied** — the state machine is not a state
  machine if any state can jump to `PAID`. Found at the §10 review, which is also where the claim
  that `ROUTE_ENDPOINTS_MATCH_STATION_LIST` was the only straddling rule was corrected.

**Enforced tier, §3.2:** `STATIONS_EXIST` (`AdminRouteServiceImpl.checkStationsExists`, called from
`createAndModifyRoute`), `UNIQUE_STATION_NAME` (`StationServiceImpl.create` rejects a duplicate name;
`Station.name` is also `@Column(unique = true)`), `ROUTE_AND_TRAIN_TYPE_EXIST` (**Trip block only** —
`AdminTravelServiceImpl.checkTravelInfo`, called from `addTravel` and `updateTravel`),
`UNIQUE_TRIP_NUMBER` (`TravelServiceImpl.java:61`, which declines to save a second trip under an
existing id), `UNIQUE_USER_NAME` (`UserServiceImpl.saveUser`, whose own comment reads
`// avoid same user name`), `TRIP_EXISTS` (`TravelServiceImpl.getTripAllDetailInfo` returns
"Trip not found" and `PreserveServiceImpl` rejects the booking on it), `CONTACTS_EXIST`
(`PreserveServiceImpl` step 2 rejects the booking when the contacts fetch fails),
`ENDPOINTS_ON_TRIP_ROUTE` (`BasicServiceImpl.queryForTravel`, which rejects with
"Station not correct in Route" unless `indexOf(from) < indexOf(to)` on the route's station list),
`PRICE_MATCHES_TARIFF` (`BasicServiceImpl.java:102-107`: the distance subtraction at `:102`, the
multiplication by each class's rate at `:106` and `:107`), `SEAT_CAPACITY_NOT_EXCEEDED`. Every other
§3.2 rule is implied.

`SEAT_CAPACITY_NOT_EXCEEDED` is enforced-but-corrected: TrainTicket has the check, and the check is
defect **F1** below.

**Two rules in this file depart from benchmark behaviour, not one.** The second is
`PRICE_CONFIG_EXISTS`, which fails the booking when no price configuration exists for the route and
train type; TrainTicket instead books at a hard-coded default fare, which is defect **F5** below.
Both departures are corrections of a defect rather than additions of a constraint. Both are also
*corrections*, which is what distinguishes them from the *simplifications* recorded as threat 6 of
the rationale: a simplification makes this application stricter than the benchmark without claiming
the benchmark is wrong, and `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` is the rule that carries one.

Six candidate rules were **dropped** rather than admitted to either tier, because none survived the
provenance test. The first two went at the design review (§8 of the rationale), the last four at the
scope and provenance review (§9):

- **`TRIP_NUMBER_FORMAT`** (`^[GDZTK][0-9]+$`) — nothing in TrainTicket validates a trip number.
  `SeatServiceImpl` and `AdminOrderServiceImpl` only *branch* on the leading letter
  (`trainNumber.startsWith("G") || startsWith("D")`) to select high-speed behaviour; a malformed
  number is accepted and simply takes the other path. Seed data does use `[GDZTK]` plus four digits,
  but a format no code enforces and no rule depends on is decoration.
- **`ACCOUNT_ORDER_RATE_LIMIT`** — TrainTicket's scalper check reads its two thresholds from
  `ts-security-service`, which ships **both** as `Integer.MAX_VALUE` (`InitData.java:24,29`). The
  check therefore never fires in a stock deployment, and any usable threshold would have been an
  authored number. Keeping it would have made the application's one temporal-window rule an invention
  wearing a benchmark's name.
- **`ACCOUNT_EXISTS` (Contacts)** — `ts-contacts-service` never calls `ts-user-service`. `create`
  checks only that the same document is not already registered against the account; nothing verifies
  the account itself. The rule had been listed as enforced.
- **`ACCOUNT_EXISTS` (Order)** — `PreserveServiceImpl` fetches the account exactly once, at
  `PreserveServiceImpl.java:245`, **after** `createOrder` has already succeeded at `:170`, and uses
  the result only to populate the `NotifyInfo` for a notification whose send call (`:261`) is
  commented out. It never inspects the result to reject the booking. The only account-touching
  precondition on the preserve path was `checkSecurity`, whose rule was itself dropped at §8. The
  rule had been listed as enforced. *(§9 placed this fetch inside `sendEmail`; that method exists at
  `:288` but is dead code. Corrected at §10 — the conclusion is unchanged.)*
- **`ROUTE_AND_TRAIN_TYPE_EXIST` (PriceConfig)** — `AdminBasicInfoServiceImpl.addPrice` is a bare
  passthrough to `ts-price-service`, and `PriceServiceImpl.createNewPriceConfig` validates neither
  `routeId` nor `trainType`. The Trip block of the same rule **is** enforced and is kept; only the
  PriceConfig block goes. The rule had been listed as enforced in both blocks.
- **`SEAT_CLASS_OFFERED`** — nothing checks that a seat class is offered at all. The first-class
  branch in `PreserveServiceImpl` tests `tripResponse.getConfortClass() == 0`, which is the
  *remaining* seat count, so the benchmark cannot distinguish a class that does not exist from one
  that is sold out; that guard is `SEAT_CAPACITY_NOT_EXCEEDED`. With no independent site, the rule
  was a duplicate of `SEAT_NUMBER_WITHIN_CAPACITY`, which bounds the same quantity, and the tier
  difference that had justified keeping both did not survive re-checking either — see the note on
  `ORDER_SEAT_NUMBER_POSITIVE` in §3.1.

### Findings recorded against the benchmark

Five defects and modelling flaws surfaced while writing this specification. They are recorded here
because they are results, not incidental notes. F1–F4 were found while writing it; F5 at the §9
review.

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
- **F4 — dates, money and the seat number are `String`.** `Order.price`, `Order.travelDate`,
  `Order.boughtDate`, `Order.travelTime` and `Order.seatNumber` are all `String`, parsed ad hoc at
  each use site (`calculateRefund` builds a `java.util.Date` from parsed fragments via a deprecated
  constructor). This file uses `BigDecimal`, `LocalDate`, `LocalDateTime`, `LocalTime` and
  `Integer`.
- **F5 — a missing price configuration silently books at an invented fare.**
  `BasicServiceImpl.queryForTravel` computes the fare inside a `try` whose `catch (Exception e)`
  writes `prices.put("economyClass", "95.0")` and `prices.put("confortClass", "120.0")`.
  `queryPriceConfigByRouteIdAndTrainType` returns `null` when no configuration exists for the pair,
  the multiplication throws inside the try, and the booking proceeds at the hard-coded default. The
  same catch swallows any other arithmetic failure on that path. Two consequences: no route and train
  type combination can ever fail for want of a tariff, and `PRICE_MATCHES_TARIFF` — which the
  benchmark otherwise enforces — does not hold on the orders the catch produces.
  `PRICE_CONFIG_EXISTS` in §3.2 is the corrected rule, and it is the second of this file's two
  departures from benchmark behaviour.

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
- **The scalper rate limit is not modelled at all.** `ts-security-service` is out of scope, and the
  rule it would have contributed was dropped on provenance grounds — see § "Rule provenance" above.
  `ts-user-service` is still in scope; only the security check is gone.
- **`Order.coachNumber` is dropped.** The field exists on TrainTicket's `Order` (`Order.java:57`) but
  carries no information: the constructor hard-codes it to `5` (`Order.java:79`) and nothing in the
  booking flow ever assigns it again. Every order in the benchmark is in coach 5.

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
| TRIP_START_BEFORE_END | Trip | `Trip.startTime < Trip.endTime` |
| USER_NAME_FINAL | User | `User.userName` is immutable (Java `final` field) |
| USER_DOCUMENT_NUMBER_PRESENT | User | `User.documentType != NONE ⟹ User.documentNumber` is non-blank |
| CONTACTS_DOCUMENT_NUMBER_PRESENT | Contacts | `Contacts.documentType != NONE ⟹ Contacts.documentNumber` is non-blank |
| ORDER_STATUS_TRANSITION | Order | `prev == null ⟹ status == NOTPAID`; otherwise `prev.status → status` is one of: `NOTPAID → {PAID, CANCELLED}`, `PAID → {COLLECTED, CANCELLED}`, `COLLECTED → {USED}`, `USED → {}`, `CANCELLED → {}`; `status == prev.status` is always permitted |
| ORDER_PRICE_POSITIVE | Order | `Order.price > 0` |
| ORDER_SEAT_NUMBER_POSITIVE | Order | `Order.seatNumber >= 1` |
| ORDER_DEPARTURE_AFTER_PURCHASE | Order | `Order.boughtDate <= Order.departureTime` |
| ORDER_CANCELLATION_FIELDS_SET | Order | `Order.status == CANCELLED ⟺ (Order.cancelledTime != null ∧ Order.refundAmount != null)` |
| ORDER_REFUND_AMOUNT | Order | `Order.status == CANCELLED ∧ (prev == null ∨ prev.status != CANCELLED) ⟹ Order.refundAmount == (prev.status == NOTPAID ? 0 : (Order.cancelledTime > Order.departureTime ? 0 : Order.price × 0.80))` |

> **Immutability fields:** `ORDER_CONTRACT_FIELDS_FINAL` — `boughtDate`, `travelDate`,
> `departureTime`, `tripNumber`, `fromStationName`, `toStationName`, `seatClass`, `seatNumber`,
> `contactsName`, `contactsDocumentType`, `contactsDocumentNumber`, `price`, and the references to
> Trip, Contacts and User — is enforced by Java `final` fields and by the absence of setters after
> construction. No `verifyInvariants()` check is needed. The same applies to `TRAIN_TYPE_NAME_FINAL`,
> `TRIP_NUMBER_FINAL` and `USER_NAME_FINAL`.

> **`ORDER_SEAT_NUMBER_POSITIVE` is implied, not enforced.** It was listed as enforced until the §9
> review. Nothing in TrainTicket *checks* a seat number; `SeatServiceImpl.distributeSeat` *produces*
> one, as `rand.nextInt(range) + 1`. The `+ 1` is this rule's lower bound and `nextInt(range)` is
> `SEAT_NUMBER_WITHIN_CAPACITY`'s upper bound, so the two rules are the two halves of one expression
> and must sit in the same tier. Both are implied: the data model needs them, no code asserts them.

> **`ROUTE_ENDPOINTS_MATCH_STATION_LIST` straddles the tiers.** Containment is enforced
> (`AdminRouteServiceImpl.java:58`); the first-and-last positions are implied. See § "Rule
> provenance" in the preamble for why the strengthening is admitted.

> **`ORDER_STATUS_TRANSITION` straddles the tiers too.** Every edge into `COLLECTED`, `USED` and
> `CANCELLED` is enforced by the `ExecuteServiceImpl` and `CancelServiceImpl` guards named in the
> preamble. The edge into `PAID` is not: `OrderServiceImpl.payOrder` writes `status = PAID` without
> reading the previous status, so the benchmark accepts paying an order that is already `CANCELLED`
> or `USED`. The `⟹` clauses this table states for those two source states are therefore implied.
> Found at the §10 review; until then the preamble claimed
> `ROUTE_ENDPOINTS_MATCH_STATION_LIST` was the only rule that straddled.

> **`ORDER_REFUND_AMOUNT` is `[E]`, corrected.** TrainTicket's `calculateRefund` implements exactly
> this three-branch rule (`0.00` when unpaid, `0` once the departure has passed, otherwise 80% of the
> fare). The correction is that it builds the departure instant with a deprecated `java.util.Date`
> constructor from separately parsed date and time strings; here `departureTime` is frozen on the
> order at purchase.

> **Why `ORDER_REFUND_AMOUNT` guards on the transition, not the state.** The refund branch is chosen
> from `prev.status`, which only carries the intended meaning on the commit that performs the
> cancellation. Any later commit on an already-cancelled order — `DeleteOrder` is the one §4 provides
> — sees `prev.status == CANCELLED`, would fall through to the paid branch, and would demand
> `price × 0.80` from an order that was cancelled while `NOTPAID` and correctly refunded `0`. The
> `prev.status != CANCELLED` conjunct confines the rule to the cancelling commit;
> `ORDER_CANCELLATION_FIELDS_SET` continues to hold `refundAmount` non-null for the standing state.

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


#### Rule: UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE

| Field | Value |
|---|---|
| Entities | PriceConfig, Route, TrainType |
| Predicate | No two active PriceConfigs share the same `(routeAggregateId, trainTypeAggregateId)` pair — this is the lookup key `queryPriceConfigByRouteIdAndTrainType` assumes is unique |

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


#### Rule: CONTACTS_BELONG_TO_ACCOUNT (Order)

| Field | Value |
|---|---|
| Entities | Order, Contacts |
| Predicate | `Order.userAggregateId == Contacts.userAggregateId` — a passenger may only be booked under the account that owns that contact record |

> **User is deliberately absent from the Entities list.** The rule is *about* account ownership, but
> both sides of the comparison are local to the booking saga: `Order.userAggregateId` comes from the
> request and `Contacts.userAggregateId` from the `Contacts` fetch §4 already declares. No User read
> is needed, and §4 declares none — see §3 of
> [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md) on why `PreserveTicket`
> does not read User. Listing User here would make `/classify-and-plan` raise a cross-aggregate
> prerequisite with no operation to satisfy it.

---

#### Rule: ENDPOINTS_ON_TRIP_ROUTE (Order)

| Field | Value |
|---|---|
| Entities | Order, Trip, Route |
| Predicate | `Order.fromStationName` and `Order.toStationName` both name a `RouteStation` of the Trip's Route, and `sequence(from) < sequence(to)` |

> **Station is deliberately absent from the Entities list.** The predicate resolves entirely from
> `RouteStation.stationName` and `RouteStation.sequence`, which the Route fetch already carries, so
> no Station read is needed and §4 declares none for `PreserveTicket`. TrainTicket does call
> `checkStationExists` on both endpoints before this check (`BasicServiceImpl.java:49-51`), so adding
> the read would be faithful rather than authored; it is declined because the endpoint names on the
> order are validated against the route's own station list, which is the stronger of the two checks.

---


#### Rule: PRICE_CONFIG_EXISTS (Order)

| Field | Value |
|---|---|
| Entities | Order, Trip, PriceConfig |
| Predicate | A PriceConfig for the pair `(Trip.routeAggregateId, Trip.trainTypeAggregateId)` was ACTIVE when the order was created |

> Enforced by the `GetPriceConfigByRouteAndTrainType` fetch in the booking saga, which throws when no
> configuration exists for the pair — **P4a**. Stated as its own rule so that the saga step has a rule
> name to cite, as `docs/concepts/rule-enforcement-patterns.md` § P4 requires, and so the fare source
> gets the same explicit existence block as `TRIP_EXISTS` and `CONTACTS_EXIST`.
>
> **This is a correction, not a reproduction — finding F5.** TrainTicket does not fail the booking
> when the tariff is missing: `BasicServiceImpl.queryForTravel` catches the resulting exception and
> charges a hard-coded `95.0` or `120.0`. Implementing that faithfully would mean an application
> whose own `PRICE_MATCHES_TARIFF` rule its own booking path can violate, which is the same reason
> F1 is corrected rather than reproduced. It is one of this file's two behavioural departures, both
> declared in the preamble.

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
| Predicate | `count(existing Orders o where o.tripAggregateId == tripAggregateId ∧ o.travelDate == travelDate ∧ o.seatClass == seatClass ∧ o.status != CANCELLED ∧ o.state != DELETED) < capacity(Trip.trainType, seatClass)` |

> The count is over the Order aggregate's own table, keyed exactly as
> `OrderRepository.findByTravelDateAndTrainNumber`. `capacity` is supplied to the Order service by
> the booking saga, which fetched the Trip's TrainType. TrainTicket's own version of this check is
> defect **F1**.

> **Phrased pre-mutation, deliberately.** `existing` excludes the order being created, and the
> inequality is strict, because this rule is enforced as a **P3** service guard that runs *before* any
> aggregate mutation. Writing it as a post-state invariant (`count(...) <= capacity`) and transcribing
> it literally into that guard would admit `capacity + 1` bookings. See the §3.2 preamble: every rule
> in this section is a precondition, not a standing invariant.

---

#### Rule: SEAT_NUMBER_UNIQUE_PER_DEPARTURE

| Field | Value |
|---|---|
| Entities | Order, Trip |
| Predicate | No two Orders with `status != CANCELLED ∧ state != DELETED` share the same `(tripAggregateId, travelDate, seatClass, seatNumber)` |

> **Implied, not enforced — re-tiered at the §10 review.** `SeatServiceImpl.distributeSeat` picks a
> random seat number and retries while `isContained(soldTickets, seat)`
> (`SeatServiceImpl.java:109-111`), and that loop was cited as this rule's enforcement site until
> §10. It is not one. The loop is never reached on the segment-reuse path: at
> `SeatServiceImpl.java:100-108`, *before* any uniqueness check, `distributeSeat` walks the sold
> tickets and returns `soldTicket.getSeatNo()` outright for any ticket whose destination precedes the
> new passenger's boarding station. The benchmark therefore hands out a duplicate
> `(trip, date, seatClass, seatNumber)` deliberately, and the unconditional predicate above is false
> in it. `isContained` guards only the fall-through path — the right line for a claim it does not
> support, which is the §9 failure mode applied to §9's own residue.
>
> The rule is kept because the interval-packing optimisation is **not** modelled here: this
> application allocates one seat per journey for the whole trip, and once packing is gone the retry
> loop is the whole allocator and uniqueness does follow from it. That makes this rule a consequence
> of a declared simplification rather than a transcription of a benchmark check, which is what the
> implied tier is for. It joins `ORDER_SEAT_NUMBER_POSITIVE` and `SEAT_NUMBER_WITHIN_CAPACITY`, so
> all three seat rules now sit in one tier as three consequences of one allocator. See threat 6 of
> the rationale, which already recorded that seat uniqueness is stricter here than in the benchmark
> without that fact having reached this block.

---

#### Rule: SEAT_NUMBER_WITHIN_CAPACITY (Order)

| Field | Value |
|---|---|
| Entities | Order, Trip, TrainType |
| Predicate | `1 <= Order.seatNumber <= capacity(Trip.trainType, Order.seatClass)`, where `capacity` is `firstClassSeats` for `FIRST_CLASS` and `economyClassSeats` for `SECOND_CLASS` |

> Bounds the seat number above, which `ORDER_SEAT_NUMBER_POSITIVE` in §3.1 cannot: `capacity` lives on
> TrainType, so the limit crosses an aggregate boundary and the booking saga passes it in alongside
> the one it already passes for `SEAT_CAPACITY_NOT_EXCEEDED`. The two rules are the two halves of
> `rand.nextInt(range) + 1` in `SeatServiceImpl.distributeSeat`, which is why they share a tier.
>
> **This rule also covers the seat-class case.** When `capacity` is `0` the interval `[1, 0]` is
> empty, so no seat can be allocated for a class the train type does not offer. A separate
> `SEAT_CLASS_OFFERED` rule stated that condition until the §9 review, on the argument that it was
> enforced where this rule is implied. Re-checking found no site that tests capacity for zero —
> TrainTicket's first-class guard reads the *remaining* count, not the capacity — so the tier
> difference the argument rested on did not exist, and the rule was a duplicate. It was dropped.

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
| UpdateRoute | Route | Station | Write | Replace a route's station list and distances, and its start and end station names with them |
| DeleteRoute | Route | — | Write | Soft-delete a route |
| CreateTrainType | TrainType | — | Write | Create a train type with its seat counts per class and average speed |
| UpdateTrainType | TrainType | — | Write | Update a train type's seat counts or average speed |
| DeleteTrainType | TrainType | — | Write | Soft-delete a train type |
| CreatePriceConfig | PriceConfig | — | Write | Create the per-distance fare rates for one route and train type |
| UpdatePriceConfig | PriceConfig | — | Write | Update the fare rates of an existing price configuration |
| DeletePriceConfig | PriceConfig | — | Write | Soft-delete a price configuration |
| CreateTrip | Trip | Route, TrainType | Write | Create a scheduled trip on a route with a train type and times of day |
| UpdateTrip | Trip | — | Write | Update a trip's start or end time |
| DeleteTrip | Trip | — | Write | Soft-delete a trip |
| CreateUser | User | — | Write | Create a user account |
| UpdateUser | User | — | Write | Update a user's password, gender, document or email |
| DeleteUser | User | — | Write | Soft-delete a user account |
| CreateContacts | Contacts | — | Write | Create a passenger contact record owned by an account |
| UpdateContacts | Contacts | — | Write | Update a contact's name, document or phone number |
| DeleteContacts | Contacts | — | Write | Soft-delete a contact record |
| PreserveTicket | Order | Trip, Route, TrainType, PriceConfig, Contacts | Write | Book a ticket: resolve the passenger contact and its owning account, validate the journey against the trip's route, compute the fare from distance and rates, allocate the **lowest seat number in `[1, capacity]` not already held by a non-cancelled order for that departure**, and create the order as NOTPAID |
| PayOrder | Order | — | Write | Move an order from NOTPAID to PAID |
| CollectTicket | Order | — | Write | Move a paid order to COLLECTED |
| UseTicket | Order | — | Write | Move a collected order to USED |
| CancelOrder | Order | — | Write | Cancel an unpaid or paid order, stamping the cancellation time and computing the refund |
| DeleteOrder | Order | — | Write | Soft-delete an order |
| GetStationById | Station | — | Read | Retrieve a single station by its aggregate id |
| GetStations | Station | — | Read | List all stations |
| GetRouteById | Route | — | Read | Retrieve a single route by its aggregate id |
| GetRoutes | Route | — | Read | List all routes |
| GetRoutesByStation | Route | — | Read | List the routes that stop at a given station |
| GetTrainTypeById | TrainType | — | Read | Retrieve a single train type by its aggregate id |
| GetTrainTypes | TrainType | — | Read | List all train types |
| GetPriceConfigById | PriceConfig | — | Read | Retrieve a single price configuration by its aggregate id |
| GetPriceConfigs | PriceConfig | — | Read | List all price configurations |
| GetPriceConfigByRouteAndTrainType | PriceConfig | — | Read | Retrieve the fare rates configured for a given route and train type; fails when no configuration exists for the pair |
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

> **`GetLeftTicketCount` is the application's one read saga.** It assembles state from three
> aggregates without writing anything, and is TrainTicket's `ts-seat-service.getLeftTicketOfInterval`.
>
> A second read saga, `SearchTrips`, spanned five aggregates and was cut at the §9 review to keep the
> first delivery small; it is recorded in §7 of the rationale as the first planned extension once the
> core is built. Cutting it also removed `GetTripsByRoute` and returned `GetRoutesByStation` to a
> plain list, since both existed only to serve it.

> **`PreserveTicket` is the only multi-aggregate write.** The other four Order operations are
> single-aggregate state transitions, matching TrainTicket's `ts-cancel-service` and
> `ts-execute-service`, both of which do nothing but read an order and write its status.

---
