

# Train Ticket — Domain Model

> Follows the structure defined in [`docs/templates/domain-model-template.md`](../../docs/templates/domain-model-template.md).

**How to use this file:**
1. Read §1–§2 to understand the entities, their attributes, and how they relate.
2. Read §3 to understand every consistency rule and its predicate.
3. See [`train-ticket-aggregate-grouping.md`](train-ticket-aggregate-grouping.md) for the concrete aggregate partitioning decision and its event-dependency consequences.

This model is derived from the FudanSELab `train-ticket` benchmark and covers the whole application: the booking core plus luggage consignment and food. What the reference system persists but this model leaves out — projections, configuration tables, duplicated services, query-only and infrastructure services — is inventoried and justified in [decision 005](decisions/005-ambito-da-aplicacao.md). Every departure from the reference system is either annotated below or recorded in full under [`decisions/`](decisions/).

---

## §1 — Entities

Each entity lists only its own scalar attributes. Cross-entity references appear in §2. The **Owns** column lists value objects that live inside this entity's boundary and have no independent identity (they are created and deleted with the entity).

> **Soft-delete:** Every aggregate inherits `state: AggregateState` from the simulator `Aggregate` base class (values: `ACTIVE`, `INACTIVE`, `DELETED`). This field is **not** a domain attribute and must **not** appear in the entity table. It is set by `remove()` on the base class. Rules that predicate on deletion (e.g. `X.state == DELETED`) rely on this field.

| Entity | Attributes | Owns |
|---|---|---|
| **Station** | `name: String` (immutable), `stayTime: Integer` | — |
| **TrainType** | `name: String` (immutable), `economyClassSeats: Integer`, `firstClassSeats: Integer`, `averageSpeed: Integer` | — |
| **User** | `name: String`, `username: String` (immutable), `documentType: DocumentType (ID_CARD \| PASSPORT \| OTHER)`, `documentNumber: String`, `email: String` | — |
| **Route** | — | RouteStation × N |
| **RouteStation** | `stationAggregateId: Integer`, `stationName: String`, `sequence: Integer`, `distanceFromStart: Integer` | — |
| **Contacts** | `name: String`, `documentType: DocumentType (ID_CARD \| PASSPORT \| OTHER)`, `documentNumber: String`, `phoneNumber: String` | — |
| **Wallet** | `balance: Integer` (default: 0) | — |
| **PriceConfig** | `basicPriceRate: Integer`, `firstClassPriceRate: Integer` | — |
| **Trip** | `trainCategory: TrainCategory (G \| D \| Z \| T \| K)` (immutable), `trainNumber: String` (immutable), `startTime: LocalTime`, `endTime: LocalTime` | — |
| **TicketInventory** | `travelDate: LocalDate` (immutable), `seatClass: SeatClass (FIRST_CLASS \| SECOND_CLASS)` (immutable), `totalSeats: Integer` (immutable) | SeatAllocation × N |
| **SeatAllocation** | `seatNumber: Integer`, `fromIndex: Integer`, `toIndex: Integer` | — |
| **Order** | `boughtDate: LocalDateTime` (immutable), `travelDate: LocalDate`, `seatClass: SeatClass (FIRST_CLASS \| SECOND_CLASS)`, `seatNumber: Integer`, `fromIndex: Integer`, `toIndex: Integer`, `passengerName: String` (immutable), `passengerDocumentType: DocumentType` (immutable), `passengerDocumentNumber: String` (immutable), `price: Integer`, `status: OrderStatus (NOTPAID \| PAID \| COLLECTED \| USED \| CANCELLED)` (default: NOTPAID), `assuranceType: AssuranceType (NONE \| TRAFFIC_ACCIDENT)` (default: NONE), `foodType: FoodType (NONE \| TRAIN \| STATION)` (default: NONE), `foodName: String`, `foodPrice: Integer`, `foodStoreName: String`, `foodStationIndex: Integer`, `rebooked: Boolean` (default: false) | — |
| **Payment** | `amount: Integer` (immutable), `paymentDate: LocalDateTime` (immutable) | — |
| **ConsignPrice** | `initialWeight: Integer`, `initialPrice: Integer`, `withinPrice: Integer`, `beyondPrice: Integer` | — |
| **ConsignRecord** | `handleDate: LocalDate`, `consignee: String`, `phone: String`, `weight: Integer`, `withinRegion: Boolean`, `price: Integer`, `status: ConsignStatus (ACTIVE \| CANCELLED)` (default: ACTIVE) | — |
| **StationFoodStore** | `storeName: String` (immutable), `telephone: String`, `businessTime: String`, `deliveryFee: Integer` | Food × N |
| **TrainFood** | — | Food × N |
| **Food** | `foodName: String`, `price: Integer` | — |

> **Single-reference snapshots:** where an aggregate holds exactly one reference to another aggregate, the cached fields are declared only in §2 of [`train-ticket-aggregate-grouping.md`](train-ticket-aggregate-grouping.md). An owned value object appears in the **Owns** column above only when it is needed for one of two reasons: the reference has cardinality N (`RouteStation`), or the fields are the entity's own state and cannot be flattened (`SeatAllocation`, `Food`). `Food` is the only owned type held by two roots, `StationFoodStore` and `TrainFood`; what the two share is the type, never an instance. A reference that caches a version — and therefore subscribes to the publisher's events — becomes an owned entity as well, because `EventSubscription` is constructed from the snapshot object; those are enumerated in the grouping file, not here.

### Notes on the reference system

The observations below justify the attribute set above where it departs from the FudanSELab entities. They are not implementation guidance.

**Capacity fields renamed.** `TrainType.economyClass` and `TrainType.confortClass` are *seat counts* in the reference system despite their names. They become `economyClassSeats` and `firstClassSeats`. That `confortClass` denotes first class is confirmed in `BasicServiceImpl.queryForTravel()`, where `priceForConfortClass` is computed from `firstClassPriceRate`.

**Route's parallel lists are reified.** `Route.stations: List<String>` and `Route.distances: List<Integer>` are two lists correlated by position. They become an ordered collection of `RouteStation`, which makes the positional index — on which [decision 001 §4.1](decisions/001-ticket-inventory.md) depends — an explicit part of the model, and turns the soft reference by station name into a reference to the `Station` aggregate. `Route.startStation` and `Route.endStation` are dropped: they are the first and last entries of the sequence.

**Denormalised copies dropped.** `Trip.startStationName`, `Trip.stationsName` and `Trip.terminalStationName` replicate information held by the `Route`. `Trip.trainTypeName` becomes a reference to the `TrainType` aggregate, and `Order.trainNumber` a reference to the `Trip` aggregate. `Order.travelTime` replicates `Trip.startTime`. `Order.coachNumber` is a constant (`5`) in the reference system.

**Non-domain fields dropped.** `User.password` belongs to authentication, which lives in `ts-auth-service` and is orthogonal to consistency. `User.gender` is read by no functionality.

**Enumerations narrowed.** `SeatClass` has nine values in the reference system, of which the booking core uses two. `DocumentType` loses `NONE`. `OrderStatus` loses `CHANGE` and `REFUNDS`. `AssuranceType` keeps its single value, `TRAFFIC_ACCIDENT`, and gains `NONE` for an order without assurance (the reference system's `assurance == 0`). `FoodType` replaces the integer `foodType` (`0` none, `1` on board, `2` station store).

**`OrderStatus.CHANGE` dropped.** `RebookServiceImpl.updateOrder()` mutates the existing order in place — `setTrainNumber`, `setTravelDate`, `setSeatClass`, `setSeatNumber`, `setPrice` — and then sets the status to `CHANGE`. Because `OrderServiceImpl.checkSecurityAboutOrder()` counts only `NOTPAID`, `PAID` and `COLLECTED` as valid, every rebooked order escapes the per-account quota of [decision 004](decisions/004-order-limits.md). In this model a rebooked order stays `PAID`, which closes that leak and makes the state redundant. `CHANGE` also carried a second rule: `RebookServiceImpl.rebook()` refuses an order in `CHANGE` ("You have already changed your ticket and you can only change one time."). Dropping the state would drop that rule silently, so it is kept as `Order.rebooked`, set by the rebooking and checked in §3.1 — a flag, not a status, so the quota still counts the order. `REFUNDS` is never assigned by the reference system: `ts-cancel-service` sets `CANCEL` and credits the refund.

**Order mutability follows rebooking.** Because rebooking mutates the order rather than replacing it, `travelDate`, `seatClass`, `seatNumber`, `fromIndex`, `toIndex`, `price` and the `Trip` reference are all mutable. Only `boughtDate`, the account, and the passenger snapshot are immutable. The assurance and meal fields are mutable too: the reference system's `AssuranceServiceImpl.modify()` changes the assurance type and `FoodServiceImpl.updateFoodOrder()` changes the meal.

**The passenger snapshot is not a cache.** `passengerName`, `passengerDocumentType` and `passengerDocumentNumber` are a frozen historical record, like `Order.price` is of the tariff. They carry no version and subscribe to no event: a ticket issued against a document number stays issued against that number even if the `Contacts` entry is later corrected or deleted.

**Monetary amounts are integers.** `Order.price`, `Order.foodPrice`, `Wallet.balance`, `Payment.amount`, the `PriceConfig` and `ConsignPrice` rates, `ConsignRecord.price`, `StationFoodStore.deliveryFee` and `Food.price` are integer minor units, not floating point, so that `balance >= 0` and the price-against-balance comparison are exact inside `verifyInvariants()`. Rounding happens once, when distance is multiplied by the rate. The reference system stores prices as `String` and rates as `double`, so neither representation is the faithful one.

**Seat allocations are identified by their triple.** `(seatNumber, fromIndex, toIndex)` is a key: [decision 001](decisions/001-ticket-inventory.md)'s non-overlap invariant forbids two allocations sharing a seat and a segment. Release therefore identifies an allocation by that triple, which the `Order` already carries, rather than by an order id. This keeps a level-3 aggregate free of a reference to a level-4 one — a back-reference against the direction of the event graph — and decouples the ordering of the reservation saga's steps.

**`TicketInventory.totalSeats` is frozen at creation.** The saga that opens an inventory reads the `TrainType` and fixes the number; neither the `Trip` nor the inventory subscribes to capacity changes. The argument is a domain one rather than one of convenience: reducing a train's seat count while allocations already exist would retroactively violate the invariant `seatNumber ∈ [1, totalSeats]`, so propagation is not even desirable. This supersedes the wording of decision 001 §4 ("replicated from the `TrainType` by way of the `Trip`").

**Scope extended to consignment and food.** Beyond the booking core, the model takes in `ConsignPrice` and `ConsignRecord` (`ts-consign-price-service`, `ts-consign-service`) and `StationFoodStore`, `TrainFood` and `Food` (`ts-station-food-service`, `ts-train-food-service`, `ts-common`), under the criterion of [decision 005 §1](decisions/005-ambito-da-aplicacao.md): an entity enters when it holds domain state of its own — written by the application, not derivable from another entity, and observed by another operation.

**Assurance and FoodOrder collapse into Order.** In the reference system both are separate tables, but each holds nothing beyond a choice made in the same step, and with the same lifecycle, as the order it points to ([decision 005 §4.4](decisions/005-ambito-da-aplicacao.md)). `AssuranceServiceImpl.create()` and `FoodServiceImpl.createFoodOrder()` each reject a second record for the same order, so both are at most one per order, and two records carrying the same choice cannot be told apart. They become fields: `assuranceType`, and `foodType`, `foodName`, `foodPrice`, `foodStoreName`, `foodStationIndex`. Three consequences follow by construction: no uniqueness rule is needed, cancelling an order can no longer orphan an assurance or a meal (`ts-cancel-service` touches neither), and steps 5 and 6 of the reservation, whose failure the reference system never compensates, stop being steps. Two behaviours are kept rather than fixed. The assurance premium (`3.0`, a constant of the enum) and the meal price are shown but **never charged**: `PreserveServiceImpl` sets `Order.price` from the travel tariff alone. And the meal fields are stored **as the client sends them**: the reservation never reads the menu or the store to check the dish or its price. The meal station is stored as a position on the `Trip`'s `Route`, like `fromIndex` and `toIndex`, rather than as a reference to `Station`, so that the meal adds no edge; the store is identified by that station and `foodStoreName`. Referencing the catalogue from the order was considered and rejected: `Food` has no identity, the on-board menu is already fixed by the `Trip`, and the price would have to be frozen anyway.

**ConsignPrice is a single row.** `ConsignPrice.index` is dropped. The field exists, but every one of the four methods of `ConsignPriceServiceImpl` calls `repository.findByIndex(0)`, so it is one global row dressed up as a keyed table. The price of a consignment is `initialPrice` up to `initialWeight`, plus the extra weight times `withinPrice` or `beyondPrice`.

**ConsignRecord caches its order instead of copying it.** In `PreserveServiceImpl` (step 7), `ConsignRecord.accountId`, `targetDate`, `from` and `to` are copied straight from the order, and neither rebooking nor cancellation updates them. Here they become a cache of the `Order`, refreshed by its events and declared in §2 of the grouping file. The cache holds what the `Order` actually stores (the account, the trip, the travel date and the segment indices), and the stations are resolved through the `Trip`'s `Route` when read. Referencing `User` and `Station` directly would listen to the wrong publisher: a rebooking changes the order, not the station. `status` is added so that a cancelled order marks its consignment `CANCELLED` rather than leaving it orphaned; the record is not deleted, because its computed price is a fact. `ConsignRecord` holds no reference to `ConsignPrice`: the tariff is read when the price is computed, and the result is frozen, as `Order.price` is with `PriceConfig`.

**Region is a client-supplied flag.** `ConsignRecord.withinRegion` is added. The reference system receives `isWithin` with the request but does not store it, so `updateConsignRecord()` cannot recompute a price from the record alone. Region is not a concept of the reference system: no entity defines one, the flag is never derived from the stations, and the UI hard-codes `isWithin = false` wherever it books or consigns (`client_ticket_book.js`, `flowPreserve.js`, `flowAdvancedSearch.js`, `client_order_list.js`). It is kept as an unverified client parameter rather than inventing a `Station.region`.

**Consignment types follow the UI.** `ConsignRecord.weight` is an integer because the UI accepts only a positive integer (`client_order_list.js`, `checkNum`: "Please input a positive integer (weight)!"). `handleDate` is sent as `yyyy-mm-dd`, so it is a `LocalDate`, not the reference system's `String`.

**Catalogue anchors hardened.** `StationFoodStore.stationName` becomes a reference to the `Station` aggregate and `TrainFood.tripId` a reference to the `Trip` aggregate, following the precedent of `RouteStation`. `StationFoodStore.storeName` is immutable: together with the station it forms the store's unique key (`station_store_idx`), and the reference system offers no update.

---

## §2 — Relationships

The direction is always from the referencing entity to the referenced entity. A reference held by an owned entity counts as a reference of its root (`Route → Station` is held by `RouteStation`). **Immutable** means the reference is set at creation and never pointed at another target; a condition means it may change until the stated point and is frozen from then on.

| From | To | Cardinality | Immutable |
|---|---|---|---|
| Route | Station (via RouteStation) | N → M | frozen once a `Trip` references the route |
| Contacts | User | N → 1 | yes |
| Wallet | User | 1 → 1 | yes |
| PriceConfig | Route | N → 1 | yes |
| PriceConfig | TrainType | N → 1 | yes |
| Trip | Route | N → 1 | frozen once a `TicketInventory` exists for the trip |
| Trip | TrainType | N → 1 | no |
| TicketInventory | Trip | N → 1 | yes |
| Order | Trip | N → 1 | no (rebooking moves the order to another trip) |
| Order | User | N → 1 | yes |
| Payment | Order | N → 1 | yes |
| Payment | User | N → 1 | yes |
| ConsignRecord | Order | 1 → 1 (at most one consignment per order) | yes |
| StationFoodStore | Station | N → 1 | yes |
| TrainFood | Trip | 1 → 1 (at most one menu per trip) | yes |

### Notes on the relationships

**Segment indices pin the route.** `Order.fromIndex`/`toIndex`, `SeatAllocation`, the segment cached by `ConsignRecord` and `Order.foodStationIndex` are positions on the `Route` of the `Trip`, meaningful only relative to the sequence that produced them. Reordering, inserting or removing a stop — or pointing the trip at another route — would silently reinterpret segments already sold. The reference system allows both (`TravelServiceImpl.update()` sets `routeId`; routes are editable), so the two conditional rows above are a departure: a route freezes once a trip uses it, and a trip keeps its route once an inventory is open for it. The rules that enforce this are in §3.2.

**`PriceConfig` keeps its pair.** The reference system lets an update change a tariff's `routeId` and train type (`PriceServiceImpl`). The pair is the tariff's identity — the key of its uniqueness rule — so changing it is deleting one tariff and creating another. Only the rates are mutable.

**`Trip → TrainType` may change.** Capacity is frozen into each `TicketInventory` when it opens, so moving a trip to another train type affects only inventories opened afterwards.

**`Payment → Order` is N → 1.** A rebooking that costs more records a second payment against the same order (`InsidePaymentServiceImpl.payDifference()`, payment type `E`).

**What is not a relationship.** The following look like references and are deliberately not:

- `Order` → `Contacts` — the passenger is copied and frozen at issue time (§1 notes); deleting or correcting the contact does not touch the ticket.
- `Order` → `StationFoodStore` / `TrainFood` — the meal stores what the client sent, not a reference to a menu.
- `ConsignRecord` → `ConsignPrice` — the tariff is read when the price is computed; the result is frozen.
- `SeatAllocation` → `Order` — an allocation is identified by `(seatNumber, fromIndex, toIndex)`, which the order carries.
- `Order.foodStationIndex` → `Station` — a position on the route, like the segment indices.

---

## §3 — Rules

### 3.1 — Single-entity rules

These rules inspect only fields of a single aggregate: its own scalars, its owned entities, and its previous version (`prev`) for transition rules. None reads another aggregate, the database or the clock. A rule is listed when the reference system enforces it, or when the model depends on it — another rule, a computation or a segment index would be meaningless without it. Generic hygiene (non-blank strings, e-mail format) is left out.

| Rule | Entity | Predicate |
|---|---|---|
| STATION_NAME_FINAL | Station | `Station.name` is immutable (Java `final` field) |
| STATION_STAY_TIME_NON_NEGATIVE | Station | `stayTime >= 0` |
| TRAIN_TYPE_NAME_FINAL | TrainType | `TrainType.name` is immutable (Java `final` field) |
| TRAIN_TYPE_SEATS_NON_NEGATIVE | TrainType | `economyClassSeats >= 0 ∧ firstClassSeats >= 0` |
| TRAIN_TYPE_SPEED_POSITIVE | TrainType | `averageSpeed > 0` |
| USER_USERNAME_FINAL | User | `User.username` is immutable (Java `final` field) |
| ROUTE_AT_LEAST_TWO_STOPS | Route | the route has at least two `RouteStation` entries |
| ROUTE_SEQUENCE_CONTIGUOUS | Route | the `sequence` values of its `RouteStation` entries are exactly `{0, …, n−1}` |
| ROUTE_ORIGIN_AT_ZERO | Route | the `RouteStation` with `sequence == 0` has `distanceFromStart == 0` |
| ROUTE_DISTANCE_INCREASING | Route | `∀ r, s ∈ RouteStation: r.sequence < s.sequence ⟹ r.distanceFromStart < s.distanceFromStart` |
| ROUTE_NO_REPEATED_STATION | Route | the `RouteStation` entries have pairwise distinct `stationAggregateId` |
| WALLET_BALANCE_NON_NEGATIVE | Wallet | `balance >= 0` |
| PRICE_CONFIG_RATES_NON_NEGATIVE | PriceConfig | `basicPriceRate >= 0 ∧ firstClassPriceRate >= 0` |
| TRIP_CATEGORY_FINAL | Trip | `Trip.trainCategory` is immutable (Java `final` field) |
| TRIP_NUMBER_FINAL | Trip | `Trip.trainNumber` is immutable (Java `final` field) |
| TRIP_NUMBER_DIGITS | Trip | `trainNumber` matches `[0-9]+` — the category letter is held apart, in `trainCategory` |
| TICKET_INVENTORY_KEY_FINAL | TicketInventory | `travelDate`, `seatClass` and `totalSeats` are immutable (Java `final` fields) |
| TICKET_INVENTORY_CAPACITY_POSITIVE | TicketInventory | `totalSeats > 0` |
| SEAT_NUMBER_IN_RANGE | TicketInventory | `∀ a ∈ SeatAllocation: 1 <= a.seatNumber <= totalSeats` |
| SEAT_SEGMENT_VALID | TicketInventory | `∀ a ∈ SeatAllocation: 0 <= a.fromIndex < a.toIndex` |
| SEAT_NO_OVERLAP | TicketInventory | `∀ a ≠ b ∈ SeatAllocation: a.seatNumber == b.seatNumber ⟹ a.toIndex <= b.fromIndex ∨ b.toIndex <= a.fromIndex` |
| ORDER_BOUGHT_DATE_FINAL | Order | `Order.boughtDate` is immutable (Java `final` field) |
| ORDER_PASSENGER_FINAL | Order | `passengerName`, `passengerDocumentType` and `passengerDocumentNumber` are immutable (Java `final` fields) |
| ORDER_SEGMENT_VALID | Order | `0 <= fromIndex < toIndex` |
| ORDER_PRICES_NON_NEGATIVE | Order | `price >= 0 ∧ (foodPrice == null ∨ foodPrice >= 0)` |
| ORDER_STATUS_TRANSITIONS | Order | `status == prev.status`, or `(prev.status, status) ∈ {(NOTPAID, PAID), (NOTPAID, CANCELLED), (PAID, COLLECTED), (PAID, CANCELLED), (COLLECTED, USED)}` |
| ORDER_TERMINAL | Order | `prev.status ∈ {USED, CANCELLED} ⟹` no field differs from `prev` |
| ORDER_REBOOK_ONLY_WHEN_PAID | Order | trip, `travelDate`, `seatClass`, `seatNumber`, `fromIndex`, `toIndex` or `price` differs from `prev` ⟹ `prev.status == PAID ∧ status == PAID` |
| ORDER_REBOOK_ONCE | Order | `prev.rebooked ⟹ rebooked ∧` trip, `travelDate`, `seatClass`, `seatNumber`, `fromIndex`, `toIndex` and `price` equal `prev`; and any of those fields differing from `prev` ⟹ `rebooked` |
| ORDER_MEAL_CONSISTENT | Order | `foodType == NONE ⟹` all of `foodName`, `foodPrice`, `foodStoreName`, `foodStationIndex` are null; `foodType == TRAIN ⟹ foodName, foodPrice ≠ null ∧ foodStoreName, foodStationIndex == null`; `foodType == STATION ⟹` all four `≠ null` |
| ORDER_MEAL_STATION_ON_SEGMENT | Order | `foodType == STATION ⟹ fromIndex <= foodStationIndex <= toIndex` |
| PAYMENT_FINAL | Payment | `amount` and `paymentDate` are immutable (Java `final` fields) |
| PAYMENT_AMOUNT_POSITIVE | Payment | `amount > 0` |
| CONSIGN_PRICE_NON_NEGATIVE | ConsignPrice | `initialWeight >= 0 ∧ initialPrice >= 0 ∧ withinPrice >= 0 ∧ beyondPrice >= 0` |
| CONSIGN_WEIGHT_POSITIVE | ConsignRecord | `weight > 0` |
| CONSIGN_RECORD_PRICE_NON_NEGATIVE | ConsignRecord | `price >= 0` |
| CONSIGN_CANCELLED_TERMINAL | ConsignRecord | `prev.status == CANCELLED ⟹` no field differs from `prev` |
| STATION_FOOD_STORE_NAME_FINAL | StationFoodStore | `StationFoodStore.storeName` is immutable (Java `final` field) |
| STATION_FOOD_STORE_FEE_NON_NEGATIVE | StationFoodStore | `deliveryFee >= 0` |
| STATION_FOOD_STORE_FOOD_PRICE_NON_NEGATIVE | StationFoodStore | `∀ f ∈ Food: f.price >= 0` |
| TRAIN_FOOD_PRICE_NON_NEGATIVE | TrainFood | `∀ f ∈ Food: f.price >= 0` |

> **Immutable references:** the references marked `yes` in §2 — `Contacts → User`, `Wallet → User`, `PriceConfig → Route` and `→ TrainType`, `TicketInventory → Trip`, `Order → User`, `Payment → Order` and `→ User`, `ConsignRecord → Order`, `StationFoodStore → Station`, `TrainFood → Trip` — are enforced by Java `final` fields or by the absence of a setter after construction. No `verifyInvariants()` check is needed.

#### Notes on the single-entity rules

**Order lifecycle follows the reference system.** Paying requires `NOTPAID` (`InsidePaymentServiceImpl`), collecting the ticket requires `PAID` (`ExecuteServiceImpl.ticketCollect()`), entering the station requires `COLLECTED` (`ExecuteServiceImpl.ticketExecute()`), cancelling requires `NOTPAID` or `PAID` (`CancelServiceImpl`), and rebooking requires `PAID` (`RebookServiceImpl`). `CHANGE`, accepted by the reference system wherever `PAID` is, is folded into `PAID` plus `rebooked`.

**The seat rules are those of [decision 001 §4](decisions/001-ticket-inventory.md).** Together they imply that no segment carries more passengers than the class has seats, without stating that per-segment rule. The seat number is scoped to one inventory, and an inventory to one seat class, so seat numbering is per class. The upper bound of `toIndex` — the length of the route — is not checkable here, because the inventory does not hold the route; the reservation guarantees it (§3.2).

**`trainNumber` holds digits only.** In the reference system the category is the first character of the train number (`TripId(String)` splits `"G1234"` into `G` and `1234`). Here the two are separate attributes, and storing the letter in both would be redundant.

**No rule on `startTime` and `endTime`.** Z, T and K services run overnight — leaving at 22:00 and arriving at 06:00 is normal — so `endTime > startTime` would reject valid trips, and a trip has no date to disambiguate the two.

**The meal station lies on the segment.** The UI offers only stores at stations between the passenger's origin and destination (`FoodServiceImpl.getAllFood()`), but the server never checks it. The rule is local to the `Order` and costs nothing, so it is stated.

**What is not here.** Dish names are not required to be unique within a menu, as in the reference system. The remaining constraints need another aggregate and belong to §3.2: the eight uniqueness rules, the freezing of routes and of a trip's route, the consignment price against `ConsignPrice`, the order price against `PriceConfig`, the per-account order quota ([decision 004](decisions/004-order-limits.md)), and the rebooking time window, which needs the clock.

---

### 3.2 — Cross-entity rules

These rules need data from more than one aggregate. They state *what* must hold; the enforcement pattern of each (P2–P4) is decided when the application is planned, not here. *Active* means `state ≠ DELETED`. A rule phrased "when X is created" holds at that moment and is not re-checked when the upstream later changes — the value it constrains is a frozen fact.

---

#### Uniqueness

#### Rule: STATION_NAME_UNIQUE

| Field | Value |
|---|---|
| Entities | Station |
| Predicate | No two active Stations share `name`. |

#### Rule: TRAIN_TYPE_NAME_UNIQUE

| Field | Value |
|---|---|
| Entities | TrainType |
| Predicate | No two active TrainTypes share `name`. |

#### Rule: USER_USERNAME_UNIQUE

| Field | Value |
|---|---|
| Entities | User |
| Predicate | No two active Users share `username`. |

#### Rule: TRIP_NUMBER_UNIQUE

| Field | Value |
|---|---|
| Entities | Trip |
| Predicate | No two active Trips share `(trainCategory, trainNumber)`. |

#### Rule: PRICE_CONFIG_UNIQUE

| Field | Value |
|---|---|
| Entities | PriceConfig, Route, TrainType |
| Predicate | No two active PriceConfigs reference the same `(Route, TrainType)` pair. |

#### Rule: TICKET_INVENTORY_UNIQUE

| Field | Value |
|---|---|
| Entities | TicketInventory, Trip |
| Predicate | No two active TicketInventories share `(Trip, travelDate, seatClass)`. |

#### Rule: CONTACTS_UNIQUE_PER_USER

| Field | Value |
|---|---|
| Entities | Contacts, User |
| Predicate | No two active Contacts of the same User share `(documentType, documentNumber)`. |

#### Rule: WALLET_ONE_PER_USER

| Field | Value |
|---|---|
| Entities | Wallet, User |
| Predicate | A User has at most one active Wallet. |

#### Rule: STATION_FOOD_STORE_UNIQUE

| Field | Value |
|---|---|
| Entities | StationFoodStore, Station |
| Predicate | No two active StationFoodStores share `(Station, storeName)`. |

#### Rule: TRAIN_FOOD_ONE_PER_TRIP

| Field | Value |
|---|---|
| Entities | TrainFood, Trip |
| Predicate | A Trip has at most one active TrainFood. |

#### Rule: CONSIGN_ONE_PER_ORDER

| Field | Value |
|---|---|
| Entities | ConsignRecord, Order |
| Predicate | An Order has at most one ConsignRecord. |

---

#### Frozen routes

#### Rule: ROUTE_FROZEN_WHEN_USED

| Field | Value |
|---|---|
| Entities | Route, Trip |
| Predicate | An active Trip references the Route ⟹ the Route's `RouteStation` entries (`stationAggregateId`, `sequence`, `distanceFromStart`) do not change. |

#### Rule: TRIP_ROUTE_FROZEN_WHEN_SOLD

| Field | Value |
|---|---|
| Entities | Trip, TicketInventory |
| Predicate | An active TicketInventory exists for the Trip ⟹ the Trip's Route reference does not change. |

---

#### Deletion of referenced aggregates

#### Rule: STATION_NOT_DELETED_WHILE_USED

| Field | Value |
|---|---|
| Entities | Station, Route |
| Predicate | `Station.state == DELETED` ⟹ no active Route has a `RouteStation` with that `stationAggregateId`. |

#### Rule: TRAIN_TYPE_NOT_DELETED_WHILE_USED

| Field | Value |
|---|---|
| Entities | TrainType, Trip |
| Predicate | `TrainType.state == DELETED` ⟹ no active Trip references it. |

#### Rule: ROUTE_NOT_DELETED_WHILE_USED

| Field | Value |
|---|---|
| Entities | Route, Trip |
| Predicate | `Route.state == DELETED` ⟹ no active Trip references it. |

#### Rule: TRIP_NOT_DELETED_WHILE_USED

| Field | Value |
|---|---|
| Entities | Trip, TicketInventory, Order |
| Predicate | `Trip.state == DELETED` ⟹ no active TicketInventory and no Order references it. |

#### Rule: USER_NOT_DELETED_WITH_LIVE_ORDERS

| Field | Value |
|---|---|
| Entities | User, Order |
| Predicate | `User.state == DELETED` ⟹ no active Order of that User has `status ∈ {NOTPAID, PAID, COLLECTED}`. |

#### Rule: CONTACTS_DELETED_WITH_USER

| Field | Value |
|---|---|
| Entities | Contacts, User |
| Predicate | `User.state == DELETED` ⟹ every Contacts of that User is eventually `DELETED`. |

#### Rule: WALLET_DELETED_WITH_USER

| Field | Value |
|---|---|
| Entities | Wallet, User |
| Predicate | `User.state == DELETED` ⟹ the Wallet of that User is eventually `DELETED`. |

#### Rule: PRICE_CONFIG_DELETED_WITH_TRAIN_TYPE

| Field | Value |
|---|---|
| Entities | PriceConfig, TrainType |
| Predicate | `TrainType.state == DELETED` ⟹ every PriceConfig referencing that TrainType is eventually `DELETED`. |

#### Rule: PRICE_CONFIG_DELETED_WITH_ROUTE

| Field | Value |
|---|---|
| Entities | PriceConfig, Route |
| Predicate | `Route.state == DELETED` ⟹ every PriceConfig referencing that Route is eventually `DELETED`. |

#### Rule: STATION_FOOD_STORE_DELETED_WITH_STATION

| Field | Value |
|---|---|
| Entities | StationFoodStore, Station |
| Predicate | `Station.state == DELETED` ⟹ every StationFoodStore referencing that Station is eventually `DELETED`. |

#### Rule: TRAIN_FOOD_DELETED_WITH_TRIP

| Field | Value |
|---|---|
| Entities | TrainFood, Trip |
| Predicate | `Trip.state == DELETED` ⟹ the TrainFood of that Trip is eventually `DELETED`. |

---

#### Reservation

#### Rule: ORDER_SEAT_ALLOCATED

| Field | Value |
|---|---|
| Entities | Order, TicketInventory |
| Predicate | For every Order that holds a seat, the TicketInventory for `(Order.Trip, Order.travelDate, Order.seatClass)` holds a `SeatAllocation` equal to `(Order.seatNumber, Order.fromIndex, Order.toIndex)`; and every `SeatAllocation` corresponds to exactly one such Order. An Order holds a seat when it is active with `status ≠ CANCELLED`, or when `status == USED`, deleted or not. Cancelling an order releases its allocation, and so does deleting it before use; rebooking moves it. |

#### Rule: ORDER_SEGMENT_WITHIN_ROUTE

| Field | Value |
|---|---|
| Entities | Order, Trip, Route |
| Predicate | `Order.toIndex` ≤ the highest `sequence` of the Route of `Order.Trip`. |

#### Rule: ORDER_PRICE_FROM_TARIFF

| Field | Value |
|---|---|
| Entities | Order, Trip, Route, PriceConfig |
| Predicate | When an Order is created or rebooked: `Order.price == (d(toIndex) − d(fromIndex)) × rate`, where `d(i)` is the `distanceFromStart` of the stop with `sequence == i` on the Route of `Order.Trip`, and `rate` is `firstClassPriceRate` if `seatClass == FIRST_CLASS`, else `basicPriceRate`, of the PriceConfig for `(Trip.Route, Trip.TrainType)`. |

#### Rule: INVENTORY_CAPACITY_FROM_TRAIN_TYPE

| Field | Value |
|---|---|
| Entities | TicketInventory, Trip, TrainType |
| Predicate | When a TicketInventory is created: `totalSeats == firstClassSeats` if `seatClass == FIRST_CLASS`, else `economyClassSeats`, of the TrainType of its Trip. |

#### Rule: ORDER_PASSENGER_IS_OWN_CONTACT

| Field | Value |
|---|---|
| Entities | Order, Contacts, User |
| Predicate | When an Order is created: `(passengerName, passengerDocumentType, passengerDocumentNumber)` equal the fields of an active Contacts of `Order.User`. |

#### Rule: ORDER_QUOTA

| Field | Value |
|---|---|
| Entities | Order, User |
| Predicate | The number of active Orders of a User with `status ∈ {NOTPAID, PAID, COLLECTED}` never exceeds `MAX_ACTIVE_ORDERS`. |

#### Rule: REBOOK_SAME_STATIONS

| Field | Value |
|---|---|
| Entities | Order, Trip, Route |
| Predicate | When an Order is rebooked: the stations at `fromIndex` and `toIndex` on the Route of the new Trip are the same stations as at the old `fromIndex` and `toIndex` on the Route of the old Trip. |

---

#### Payment

#### Rule: PAYMENT_MATCHES_ORDER

| Field | Value |
|---|---|
| Entities | Payment, Order, User |
| Predicate | `Payment.User == Payment.Order.User`; the first Payment of an Order has `amount == Order.price` at payment time; each later Payment of the same Order has `amount` equal to the price increase of a rebooking. |

#### Rule: ORDER_PAID_HAS_PAYMENT

| Field | Value |
|---|---|
| Entities | Order, Payment |
| Predicate | `Order.status ∈ {PAID, COLLECTED, USED}` ⟹ at least one Payment references the Order. |

#### Rule: PAYMENT_DEBITS_WALLET

| Field | Value |
|---|---|
| Entities | Payment, Wallet, User |
| Predicate | Every Payment debits `amount` from the Wallet of `Payment.User`. A payment that the balance cannot cover does not happen. |

#### Rule: REFUND_ON_CANCEL

| Field | Value |
|---|---|
| Entities | Order, Wallet, User |
| Predicate | Cancelling an Order credits the Wallet of `Order.User` with `price × 80 / 100` (rounded down) if the Order was `PAID`, and nothing if it was `NOTPAID`. Deleting an Order credits the same amount if it was `PAID` or `COLLECTED`, and nothing if it was `NOTPAID`, `USED` or `CANCELLED`. |

#### Rule: REBOOK_PRICE_DIFFERENCE

| Field | Value |
|---|---|
| Entities | Order, Payment, Wallet |
| Predicate | When a rebooking changes `Order.price`: an increase is paid with a new Payment of the difference; a decrease credits the difference to the Wallet of `Order.User`. |

---

#### Consignment

#### Rule: CONSIGN_PRICE_FROM_TARIFF

| Field | Value |
|---|---|
| Entities | ConsignRecord, ConsignPrice |
| Predicate | When a ConsignRecord is created, or its `weight` or `withinRegion` changes: `price == initialPrice` if `weight <= initialWeight`, else `initialPrice + (weight − initialWeight) × (withinRegion ? withinPrice : beyondPrice)`, from the ConsignPrice at that moment. |

#### Rule: CONSIGN_FOLLOWS_ORDER

| Field | Value |
|---|---|
| Entities | ConsignRecord, Order |
| Predicate | The Order fields cached by a ConsignRecord (user, trip, travel date, segment indices) eventually equal those of the Order. |

#### Rule: CONSIGN_CANCELLED_WITH_ORDER

| Field | Value |
|---|---|
| Entities | ConsignRecord, Order |
| Predicate | `Order.status == CANCELLED` or `Order.state == DELETED` ⟹ the ConsignRecord of that Order is eventually `CANCELLED`. |

---

#### Notes on the cross-entity rules

**Uniqueness.** The eight keys of §1 plus three the model implies: `USER_USERNAME_UNIQUE` (`UserServiceImpl.saveUser()` rejects an existing user name), and the two `1 → 1` relationships of §2. The reference system relies on database indices for most of them; the simulator has none, so each is stated. `CONTACTS_UNIQUE_PER_USER` is scoped to the account: two users may register the same document. The reference system's own check is broken (`findByAccountIdAndDocumentTypeAndDocumentType` never compares the number), and only the index saves it.

**Deletion policy.** In the reference system every delete is unconditional (`StationServiceImpl.delete()` calls `repository.delete()` and nothing else), leaving dangling references. Here the rule is: reference data is not deleted while something uses it, and what only configures it goes with it. Station, TrainType, Route and Trip deletions are therefore refused while referenced by a route, a trip, an inventory or an order — which chains: a station under a sold ticket can never be deleted. A tariff is different: it configures a (route, train type) pair and is meaningless without either, so deleting the route or the train type deletes its tariffs rather than being refused by them. No trip can be using such a tariff, because the trip itself would have refused the deletion. Food stores and on-board menus follow the same policy: a store configures a station and a menu configures a trip, so they go with them. The reference system offers no operation to delete either, and without the cascade a station with a store, or a trip with a menu, could never be deleted. A User with orders not yet used or cancelled cannot be deleted; its Contacts and Wallet follow it. A deleted Order cancels its consignment, as a cancellation does. `Payment` holds plain ids and is a historical fact, so no deletion affects it. A deletion check reads other aggregates without locking them, so it races with a concurrent creation of a reference — this is one of the rules where consistency patterns differ.

**Reservation.** `ORDER_SEAT_ALLOCATED` is the central cross-aggregate invariant of the model: the order and the inventory are separate aggregates ([decision 001](decisions/001-ticket-inventory.md)), and the reservation, cancellation and rebooking sagas exist to keep them in step. Seats of `USED` orders stay allocated — the trip took place — even if the order is later deleted. `ORDER_QUOTA` is [decision 004](decisions/004-order-limits.md); the reference system's default for `max_order_not_use` is `Integer.MAX_VALUE`, so `MAX_ACTIVE_ORDERS` is a configuration value, and the reference system's hourly limit is dropped because it needs the clock. `REBOOK_SAME_STATIONS` transcribes `RebookServiceImpl` ("The departure and destination cannot be changed"); the indices may differ when the new trip runs on another route.

**Refund.** The reference system refunds nothing for an unpaid order, nothing after departure, and 80 % of the price before departure (`CancelServiceImpl.calculateRefund()`). The departure cut-off compares against the clock and is dropped, on the same ground as the hourly quota in decision 004; a paid order always refunds 80 %. Deleting an order refunds as cancelling it would, extended to `COLLECTED`: a collected ticket has been paid and not used, and deletion, unlike cancellation, accepts it. The reference system's `deleteOrder` removes the row and refunds nothing; its seat was freed only implicitly, because availability was derived from the order table ([decision 001](decisions/001-ticket-inventory.md)). The rebooking difference follows `RebookServiceImpl`: `payDifference()` for an increase, `drawBackMoney()` for a decrease.

**Not a rule.** Nothing ties `Trip.trainCategory` to its `TrainType`: in the reference system `G` trains run `GaoTieOne` only by convention of the seed data, and a `Z9999` of type `GaoTieOne` can be created. The category stays a free label. The meal fields of an `Order` are not checked against any menu, and a consignment's `withinRegion` against any region (§1 notes).

---

## §4 — Functionalities

One row per operation the application exposes. *Other Aggregates* lists the aggregates an operation reads or writes besides its primary one; aggregates that only react to a published event (a consignment cancelled after its order, contacts and wallet deleted after their user, tariffs deleted after their route or train type, food stores and menus deleted after their station or trip) are not listed.

| Functionality | Primary Aggregate | Other Aggregates | Kind | Description |
|---|---|---|---|---|
| CreateStation | Station | — | Write | Creates a station with a unique name and its stay time. |
| UpdateStation | Station | — | Write | Changes the stay time of a station. |
| DeleteStation | Station | Route | Write | Deletes a station; refused while an active route references it. Its food stores follow by event. |
| GetStationById | Station | — | Read | Returns a station by id. |
| GetStationByName | Station | — | Read | Returns a station by name. |
| ListStations | Station | — | Read | Lists all stations. |
| CreateTrainType | TrainType | — | Write | Creates a train type with its seats per class and average speed. |
| UpdateTrainType | TrainType | — | Write | Changes the seats per class and the average speed of a train type. |
| DeleteTrainType | TrainType | Trip | Write | Deletes a train type; refused while an active trip references it. Its tariffs follow by event. |
| GetTrainTypeById | TrainType | — | Read | Returns a train type by id. |
| GetTrainTypeByName | TrainType | — | Read | Returns a train type by name. |
| ListTrainTypes | TrainType | — | Read | Lists all train types. |
| CreateRoute | Route | Station | Write | Creates a route from an ordered list of stations and cumulative distances. |
| UpdateRoute | Route | Station, Trip | Write | Replaces the stops of a route; refused once an active trip references it. |
| DeleteRoute | Route | Trip | Write | Deletes a route; refused while an active trip references it. Its tariffs follow by event. |
| GetRouteById | Route | — | Read | Returns a route with its ordered stops. |
| ListRoutes | Route | — | Read | Lists all routes. |
| FindRoutesBetween | Route | — | Read | Lists the routes on which a given origin station precedes a given destination station. |
| CreatePriceConfig | PriceConfig | Route, TrainType | Write | Creates the tariff for a (route, train type) pair. |
| UpdatePriceRates | PriceConfig | — | Write | Changes the economy and first-class rates of a tariff; the pair is fixed. |
| DeletePriceConfig | PriceConfig | — | Write | Deletes a tariff. |
| GetPriceConfig | PriceConfig | — | Read | Returns the tariff of a (route, train type) pair. |
| ListPriceConfigs | PriceConfig | — | Read | Lists all tariffs. |
| CreateTrip | Trip | Route, TrainType | Write | Creates a recurring trip: category, number, route, train type and times. |
| UpdateTrip | Trip | Route, TrainType, TicketInventory | Write | Changes the times, train type or route of a trip; the route change is refused once an inventory exists for the trip. |
| DeleteTrip | Trip | TicketInventory, Order | Write | Deletes a trip; refused while an inventory or an order references it. Its on-board menu follows by event. |
| GetTrip | Trip | — | Read | Returns a trip with its route and train type references. |
| ListTrips | Trip | — | Read | Lists all trips. |
| ListTripsOfRoute | Trip | — | Read | Lists the trips that run on a route. |
| SearchTrips | Trip | Route, TrainType, PriceConfig, TicketInventory | Read | For an origin, a destination and a date, lists the trips that serve both in order, with departure and arrival times of the segment, price per class and remaining seats, sorted by price, duration or number of stops. |
| SearchTripsWithTransfer | Trip | Route, TrainType, PriceConfig, TicketInventory | Read | Runs SearchTrips for origin → via and for via → destination on the same date and returns both lists, without combining them. |
| GetTripDetail | Trip | Route, TrainType, PriceConfig, TicketInventory | Read | Returns one trip for a segment and a date, with times, price per class and remaining seats. |
| RegisterUser | User | — | Write | Creates a user account with a unique username. |
| UpdateUser | User | — | Write | Changes the name, document and e-mail of a user. |
| DeleteUser | User | Order | Write | Deletes a user; refused while the user has orders not yet used or cancelled. Contacts and wallet follow by event. |
| GetUserById | User | — | Read | Returns a user by id. |
| GetUserByUsername | User | — | Read | Returns a user by username. |
| ListUsers | User | — | Read | Lists all users. |
| CreateContacts | Contacts | User | Write | Registers a passenger for a user; the document is unique within that user's contacts. |
| UpdateContacts | Contacts | — | Write | Changes the name, document or phone of a passenger. |
| DeleteContacts | Contacts | — | Write | Deletes a passenger; orders already issued keep their frozen copy. |
| GetContacts | Contacts | — | Read | Returns a passenger by id. |
| ListContactsOfUser | Contacts | — | Read | Lists the passengers registered by a user. |
| ListAllContacts | Contacts | — | Read | Lists all passengers. |
| CreateWallet | Wallet | User | Write | Opens the wallet of a user with a zero balance; at most one per user. |
| TopUpWallet | Wallet | — | Write | Adds money to a wallet. |
| GetWalletOfUser | Wallet | — | Read | Returns the balance of a user's wallet. |
| ListWallets | Wallet | — | Read | Lists all wallets and their balances. |
| OpenTicketInventory | TicketInventory | Trip, TrainType | Write | Opens the inventory of a (trip, date, seat class), freezing its capacity from the train type; unique per triple. |
| GetRemainingSeats | TicketInventory | Trip, Route | Read | Returns the number of seats of a (trip, date, class) free over a given segment. |
| ReserveTicket | Order | Trip, Route, PriceConfig, TicketInventory, Contacts, User, ConsignRecord, ConsignPrice | Write | Books a seat: checks the per-account quota, allocates a seat on the segment in the inventory, prices the segment from the tariff, copies the passenger from a contact of the user, records the assurance and meal choice, and optionally creates a luggage consignment. |
| PayOrder | Order | Payment, Wallet | Write | Pays an unpaid order: debits the wallet, records a payment and marks the order paid. |
| CancelOrder | Order | TicketInventory, Wallet | Write | Cancels an unpaid or paid order: releases its seat allocation and credits the refund (80 % of the price if paid) to the wallet. |
| RebookOrder | Order | Trip, Route, PriceConfig, TicketInventory, Payment, Wallet | Write | Moves a paid, never-rebooked order to another trip, date, class or seat between the same stations: releases the old allocation, takes a new one, reprices, and pays or refunds the difference. |
| CollectTicket | Order | — | Write | Marks a paid order as collected. |
| EnterStation | Order | — | Write | Marks a collected order as used. |
| UpdateOrderAssurance | Order | — | Write | Changes the assurance type of an order. |
| UpdateOrderMeal | Order | — | Write | Changes or removes the meal of an order. |
| DeleteOrder | Order | TicketInventory, Wallet | Write | Deletes an order in any status. An order not yet used or cancelled is released as a cancellation would release it — its seat allocation freed and 80 % of the price refunded if it was paid — before it is deleted; its consignment is cancelled by event. |
| GetOrder | Order | — | Read | Returns an order by id. |
| ListOrdersOfUser | Order | — | Read | Lists the orders of a user. |
| ListAllOrders | Order | — | Read | Lists all orders. |
| GetRefundAmount | Order | — | Read | Returns the amount a cancellation of the order would refund. |
| ListPayments | Payment | — | Read | Lists all payments. |
| UpdateConsignPrice | ConsignPrice | — | Write | Sets the luggage tariff, creating its single row if absent. |
| GetConsignPrice | ConsignPrice | — | Read | Returns the luggage tariff. |
| QuoteConsignPrice | ConsignPrice | — | Read | Returns the price of consigning a given weight within or beyond the region. |
| CreateConsign | ConsignRecord | Order, ConsignPrice | Write | Creates the luggage consignment of an order, caching the order's segment and freezing the price computed from the tariff. |
| UpdateConsign | ConsignRecord | ConsignPrice | Write | Changes a consignment; recomputes the price when weight or region changes. |
| ListConsignsOfUser | ConsignRecord | — | Read | Lists the consignments of a user. |
| GetConsignOfOrder | ConsignRecord | — | Read | Returns the consignment of an order. |
| ListConsignsByConsignee | ConsignRecord | — | Read | Lists the consignments addressed to a consignee name. |
| CreateStationFoodStore | StationFoodStore | Station | Write | Creates a food store with its menu at a station; the store name is unique within the station. |
| GetStationFoodStore | StationFoodStore | — | Read | Returns a food store with its menu. |
| ListStationFoodStores | StationFoodStore | — | Read | Lists all food stores. |
| ListStationFoodStoresOfStation | StationFoodStore | — | Read | Lists the food stores at a station. |
| SetTrainFoodMenu | TrainFood | Trip | Write | Creates or replaces the on-board menu of a trip. |
| GetTrainFoodOfTrip | TrainFood | — | Read | Returns the on-board menu of a trip. |
| ListTrainFoods | TrainFood | — | Read | Lists all on-board menus. |
| GetMealOptions | TrainFood | Trip, Route, StationFoodStore | Read | For a trip and a segment, returns the on-board menu and the food stores at the stations of the segment. |

### Notes on the functionalities

**Correspondence with the reference system.** Every row transcribes an operation of an in-scope service, with these consolidations: `ts-preserve-service.preserve` becomes `ReserveTicket`, its assurance and meal steps folded into the order (§1 notes); `ts-inside-payment-service.pay`, `ts-cancel-service.cancelTicket`, `ts-rebook-service.rebook`/`payDifference` and `ts-execute-service.collectTicket`/`executeTicket` become `PayOrder`, `CancelOrder`, `RebookOrder`, `CollectTicket` and `EnterStation`; `ts-seat-service.getLeftTicketOfInterval` and the sold-ticket queries of `ts-order-service` become `GetRemainingSeats`, read from the inventory ([decision 001](decisions/001-ticket-inventory.md)); `ts-assurance-service.modify` and `ts-food-service.updateFoodOrder`/`deleteFoodOrder` become `UpdateOrderAssurance` and `UpdateOrderMeal`. Batch variants (`queryForIdBatch`, `queryByIds`, …) and lookups derivable from another row (`getRouteByTripId`, `getTrainTypeByTripId`, `getOrderPrice`) are not listed separately.

**Inventories are opened in advance.** A `Trip` has no date, so its possible inventories are unbounded. `OpenTicketInventory` opens one per (trip, date, class) before sale, rather than on first booking: lazy creation would add a create-if-absent step to `ReserveTicket` and make two concurrent first bookings race for the creation — in the very aggregate that exists to be the point of contention, mixing a second race into its results. This operation has no counterpart in the reference system, which has no inventory.

**Journey planning is a sort order.** `ts-route-plan-service` (cheapest, quickest, fewest stops) calls the trip search, re-sorts it and keeps the first five; `ts-travel-plan-service` wraps it and adds the transfer search, which runs the search for each leg and returns the two lists side by side, without matching arrival to departure. None of them holds or reads state the search does not, so they become the `sort` parameter of `SearchTrips` and one `SearchTripsWithTransfer` row. Segment times are computed from distance and `TrainType.averageSpeed` (`TravelServiceImpl.setResponse()`); duration adds 24 h when the arrival time is earlier than the departure time. No service outside `ts-station-service` reads `Station.stayTime`, so it is not cached in `RouteStation`.

**Generic order administration is excluded.** `ts-order-service.modifyOrder` (set any status), `updateOrder` (rewrite the order) and `saveOrderInfo` (store a ready-made order) bypass the order lifecycle: they could mark an order paid without a payment (`ORDER_PAID_HAS_PAYMENT`) or move a seat without touching the inventory (`ORDER_SEAT_ALLOCATED`). Orders change only through the operations above. `DeleteOrder` is kept; `CONSIGN_CANCELLED_WITH_ORDER` depends on it. Because it accepts an order in any status, it releases the seat and refunds before deleting (`ORDER_SEAT_ALLOCATED`, `REFUND_ON_CANCEL`) — otherwise a deleted live order would leave its allocation orphaned in the inventory and its price unrefunded.

**Wallets are opened separately.** As in the reference system, registering a user does not open a wallet (`ts-inside-payment-service.createAccount` is its own operation), so `RegisterUser` touches one aggregate. Top-up history is not kept: the `Money` ledger was replaced by the balance ([decision 002](decisions/002-wallet.md)).

**Upserts are split.** `ts-consign-service.updateConsign` creates the record when it does not exist; here creation is `CreateConsign` only, and `UpdateConsign` requires an existing record. `ts-consign-price-service.modifyPriceConfig` keeps its upsert shape as `UpdateConsignPrice`, because the tariff is a single row with no separate creation. `ts-train-food-service.createTrainFood` replaces the menu when one exists, and becomes `SetTrainFoodMenu`.

**No deletion of food stores or menus.** The reference system exposes none, and none is added. A food store is deleted only with its station, and a menu only with its trip (`STATION_FOOD_STORE_DELETED_WITH_STATION`, `TRAIN_FOOD_DELETED_WITH_TRIP`).
