# Train Ticket — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

This file captures the aggregate partitioning decision for the [Train Ticket domain model](train-ticket-domain-model.md).

The eighteen entities of the domain model are distributed over fifteen aggregates, one per root; the three remaining entities — `RouteStation`, `SeatAllocation` and `Food` — are owned. Thirteen of the roots correspond to entities of the FudanSELab reference system; two — `TicketInventory` and `Wallet` — do not, and are reifications of state the reference system derives on every request. Their justification is in [`decisions/001-ticket-inventory.md`](decisions/001-ticket-inventory.md) and [`decisions/002-wallet.md`](decisions/002-wallet.md). Two entities of the reference system, `Assurance` and `FoodOrder`, are not aggregates here: they collapse into fields of the `Order` ([decision 005 §4.4](decisions/005-ambito-da-aplicacao.md)).

The *Entities contained* column lists more names than the eighteen: besides the root and its owned entities, it names the seven snapshot entities (`ContactsUser`, `WalletUser`, `PriceConfigRoute`, `PriceConfigTrainType`, `ConsignRecordOrder`, `StationFoodStoreStation`, `TrainFoodTrip`) that a reference produces when it subscribes to an event. Why they appear there is explained under [On the *Entities contained* column](#on-the-entities-contained-column).

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Service |
|---|---|---|---|
| Station | A station on the rail network, identified by name. Reference data, read when routes are built. | Station | StationService |
| TrainType | A class of train, carrying the seat capacity declared per seat class and the average speed. Source of the capacity an inventory freezes when it opens. | TrainType | TrainTypeService |
| User | A user account. Holder of orders, contacts and a wallet. | User | UserService |
| Route | A route: the ordered sequence of stations with the distance accumulated from the origin. The positions in this sequence are the indices that denote allocation segments (decision 001 §4.1). | Route, RouteStation | RouteService |
| Contacts | A passenger registered by a user, with an identity document. Source of the snapshot an order freezes at issue time. | Contacts, ContactsUser | ContactsService |
| Wallet | A user's balance, held as persisted mutable state. Reification of what `ts-inside-payment-service` recomputes on every request (decision 002). | Wallet, WalletUser | WalletService |
| PriceConfig | The tariff for a (route, train type) pair: rates per unit of distance, economy and first class. Kept as its own aggregate on rate-of-change grounds (decision 003). | PriceConfig, PriceConfigRoute, PriceConfigTrainType | PriceConfigService |
| Trip | A recurring service: number, category, route, train type and schedule. Reference data, read — without a lock — by every reservation. | Trip | TripService |
| TicketInventory | Seat occupancy for one (trip, travel date, seat class). Reification of the availability `ts-seat-service` derives from the order table (decision 001). The only point of contention in the reservation. | TicketInventory, SeatAllocation | TicketInventoryService |
| Order | A ticket order: segment, allocated seat, tariff snapshot, passenger snapshot, assurance and meal choice, and lifecycle state. | Order | OrderService |
| Payment | An immutable record of a payment made against an order. | Payment | PaymentService |
| ConsignPrice | The luggage consignment tariff: weight allowance, base price, and rates per extra unit of weight within and beyond the region. A single global row (decision 005). | ConsignPrice | ConsignPriceService |
| ConsignRecord | A luggage consignment attached to an order, with its price frozen when computed and the order's segment cached. | ConsignRecord, ConsignRecordOrder | ConsignRecordService |
| StationFoodStore | A food store at a station, with its menu. Reference data, read by the food listing. | StationFoodStore, Food, StationFoodStoreStation | StationFoodStoreService |
| TrainFood | The on-board menu of a trip, at most one per trip. Reference data, read by the food listing. | TrainFood, Food, TrainFoodTrip | TrainFoodService |

### No co-location

Every root is its own aggregate. This is a decision rather than a transcription: five merges were considered and rejected, all under the criterion stated in decision 003 §3 — **decompose by rate of change, not by co-occurrence**.

- **`Wallet` into `User`** — rejected in [decision 002 §4](decisions/002-wallet.md). A balance on the `User` would put the `User` into a saga state on every payment, cancellation, rebooking and top-up, so account management (`UpdateUser`, `DeleteUser`) and balance movements would abort each other without competing for anything. Under a pattern that validates the versions of what it read, every payment would also abort the concurrent reservations that had read the `User` as reference data. The effect under sagas is modest; the argument rests on keeping the patterns comparable.
- **`PriceConfig` into `Trip`** — rejected in [decision 003 §3](decisions/003-price-config.md). Many trips share a (route, train type) pair, so replication would make a tariff change rewrite N trips and abort every reservation in flight over them.
- **`TicketInventory` into `Trip`** — rejected here, on the same criterion and with the strongest form of the argument. The `Trip` is read by every reservation without contending; the inventory is written by every reservation. Merging them would make each reservation abort every concurrent read of the trip, collapsing the two regimes decision 003 §3 set out to keep visible side by side: steps that lock a scarce resource, and steps that only read reference data.
- **`ConsignRecord` into `ConsignPrice`** — rejected. No invariant relates a record's price to the current tariff: the price is frozen when computed, and a tariff change must not reprice past consignments. With a single `ConsignPrice` row, co-location would put every consignment of the application in one aggregate, so that any two consignments for unrelated orders would conflict.
- **`TrainFood` into `Trip`** — rejected. The menu is set and replaced separately from the trip, by its own operation, so it is not a choice to be folded into the trip the way the assurance and the meal fold into the `Order`. And every menu change would bump the version of the `Trip`, which every reservation reads — the argument that keeps `PriceConfig` out of it.

`Food` appears in two aggregates, `StationFoodStore` and `TrainFood`. This is not co-location and does not link them: the two share the owned type, never an instance, and no event flows between them.

### On the *Entities contained* column

Most of this column is a mechanical consequence of §2 rather than a co-location decision. A reference to another aggregate becomes a contained entity exactly when the consumer subscribes to the publisher's events, because `EventSubscription` is constructed from the snapshot object. A reference that subscribes to nothing stays a pair of plain fields on the root: the id and a version seeded at creation (§2).

Three entries are settled independently of §2:

- `RouteStation` — cardinality N; it caches the station's id and name, subscribes to nothing, and is at the same time the `Station × N` snapshot of §2.
- `SeatAllocation` — cardinality N, own state, no subscription.
- `Food` — cardinality N, own state, no subscription; owned by both `StationFoodStore` and `TrainFood`.

The remaining seven — `ContactsUser`, `WalletUser`, `PriceConfigRoute`, `PriceConfigTrainType`, `ConsignRecordOrder`, `StationFoodStoreStation` and `TrainFoodTrip` — follow from the subscription rule: they are the only references in §2 that subscribe to an event. Every upstream service in the train-ticket benchmark exposes `update` and `delete` — verified across `ts-station-service`, `ts-train-service`, `ts-route-service`, `ts-contacts-service`, `ts-travel-service`, `ts-price-service`, `ts-user-service` and `ts-order-service` — but the deletion policy of §3.2 of the domain model refuses the delete of any reference data still in use by a route, a trip, an inventory or an order, and the fields the other references would cache are either immutable or frozen while referenced. What remains to follow by event is a user's deletion, for its contacts and wallet; a route's or a train type's deletion, for its tariffs; a station's or a trip's deletion, for its food stores or its menu; and an order's rebooking, cancellation and deletion, for its consignment.

Several references deliberately subscribe to nothing and therefore produce no contained entity:

- the five single `n/a` rows of §2 besides the `Payment`'s two, which cache an id that nothing upstream can invalidate while the reference exists;
- the `Order`'s passenger snapshot, which is frozen at issue time;
- the `Payment`'s references to its order and account, a payment being a historical fact that a later cancellation does not undo — the refund is a credit on the `Wallet`, not a retraction;
- the `Order`'s meal choice, which stores what the client sent and references neither `StationFoodStore` nor `TrainFood`;
- the `ConsignRecord`'s price, computed from `ConsignPrice` and then frozen, so there is no reference to the tariff at all.

---

## §2 — Snapshots

One row per relationship of §2 of the [domain model](train-ticket-domain-model.md) — fifteen rows, fifteen relationships. Field names match the attributes declared in §1 of the domain model.

> **Version fields:** every row also caches the publisher's version, `{publisher}Version: Long`, which the harness adds without it being listed. On a row that names an event, the pair (id, version) builds the `EventSubscription` and the row becomes a contained entity (§1). On an `n/a` row, the version is seeded at creation and never refreshed, and nothing reads it: the id and the version are plain fields on the root.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Route | Station × N | `stationAggregateId`, `stationName` | n/a — `Station.name` is immutable, and a referenced Station cannot be deleted |
| Contacts | User | `userAggregateId` | `DeleteUserEvent` |
| Wallet | User | `userAggregateId` | `DeleteUserEvent` |
| PriceConfig | Route | `routeAggregateId` | `DeleteRouteEvent` |
| PriceConfig | TrainType | `trainTypeAggregateId` | `DeleteTrainTypeEvent` |
| Trip | Route | `routeAggregateId` | n/a — id only, and a referenced Route cannot be deleted |
| Trip | TrainType | `trainTypeAggregateId` | n/a — id only, and a referenced TrainType cannot be deleted |
| TicketInventory | Trip | `tripAggregateId` | n/a — id only, and a referenced Trip cannot be deleted |
| Order | Trip | `tripAggregateId` | n/a — id only, and a referenced Trip cannot be deleted |
| Order | User | `userAggregateId` | n/a — id only; a User can be deleted only once its orders are terminal |
| Payment | Order | `orderAggregateId` | n/a — a payment is a historical fact |
| Payment | User | `userAggregateId` | n/a — a payment is a historical fact |
| ConsignRecord | Order | `orderAggregateId`, `userAggregateId`, `tripAggregateId`, `travelDate`, `fromIndex`, `toIndex` | `RebookOrderEvent`, `CancelOrderEvent`, `DeleteOrderEvent` |
| StationFoodStore | Station | `stationAggregateId` | `DeleteStationEvent` |
| TrainFood | Trip | `tripAggregateId` | `DeleteTripEvent` |

### Notes on the snapshots

**Seven rows subscribe; eight do not.** A reference subscribes only when something it caches can change, or its target can disappear, while the reference exists. The deletion policy of §3.2 of the domain model removes the second case for most references: `Station`, `TrainType`, `Route` and `Trip` refuse deletion while a route, a trip, an inventory or an order uses them (`*_NOT_DELETED_WHILE_USED`). The refusal is a synchronous guard in the deleting saga, not an event. What remains are the cases where the policy is to *react* downstream: contacts and wallet go with their user (`CONTACTS_DELETED_WITH_USER`, `WALLET_DELETED_WITH_USER`), tariffs go with their route or train type (`PRICE_CONFIG_DELETED_WITH_ROUTE`, `PRICE_CONFIG_DELETED_WITH_TRAIN_TYPE`), food stores and menus go with their station or trip (`STATION_FOOD_STORE_DELETED_WITH_STATION`, `TRAIN_FOOD_DELETED_WITH_TRIP`), and a consignment follows its order (`CONSIGN_FOLLOWS_ORDER`, `CONSIGN_CANCELLED_WITH_ORDER`).

**Tariffs go with their pair.** A `PriceConfig` configures a (route, train type) pair and means nothing once either is gone, so the deletion cascades instead of being refused. The window in which a tariff is still active for a deleted route or train type is harmless: no trip can be using the pair, because a trip on it would have refused the deletion, and `CreateTrip` checks that both targets exist.

**Food stores and menus go with their anchor.** A `StationFoodStore` configures a station and a `TrainFood` configures a trip; neither has a delete operation of its own. Refusing the deletion because of them would make a station with a store, or a trip with a menu, undeletable forever, so they cascade as tariffs do. While the event is in flight, `GetMealOptions` may still list the store or menu of a deleted station or trip. No write reads them: the meal of an order stores what the client sent.

**`RouteStation` is both an owned entity and a snapshot.** Its §2 fields are the two copied from the `Station`; `sequence` and `distanceFromStart` are the `Route`'s own state, declared in §1 of the domain model. It is one class holding all four attributes, plus the version and a Dto. `Station.stayTime` is not cached: no functionality outside the station reads it.

**Reference data is cached as an id only.** `SearchTrips`, `GetTripDetail` and `GetRemainingSeats` need the stops of the route and the speed of the train type. They read them when they run rather than from a copy on the `Trip`. The `Route` is frozen once a trip uses it (`ROUTE_FROZEN_WHEN_USED`), so a copy would need no event, but it would add replicated state that no rule checks.

**The `Order`'s user may be deleted.** `USER_NOT_DELETED_WITH_LIVE_ORDERS` refuses the deletion only while an order is `NOTPAID`, `PAID` or `COLLECTED`. Once every order is `USED` or `CANCELLED`, the user can go, and the orders keep a dangling `userAggregateId`. That is harmless, because a terminal order changes no field (`ORDER_TERMINAL`). The same holds for `Payment`, which is never updated, and for `ConsignRecord.userAggregateId`.

**Mutable references are moved by their owner, not by events.** `Trip → Route`, `Trip → TrainType` and `Order → Trip` can change, but only through an operation of the consumer itself (`UpdateTrip`, `RebookOrder`), which reads the new target in its own saga.

**No snapshot goes stale through the route.** The segment indices held by `Order`, `SeatAllocation` and `ConsignRecord`, and `Order.foodStationIndex`, are positions on a route that none of them caches. They need no subscription because the route under them cannot change: an order holds a seat in an inventory (`ORDER_SEAT_ALLOCATED`), an inventory freezes its trip's route (`TRIP_ROUTE_FROZEN_WHEN_SOLD`), and a route used by a trip freezes its stops (`ROUTE_FROZEN_WHEN_USED`).

**`ConsignRecord` caches what the `Order` stores.** It caches the user, the trip, the travel date and the segment indices. It does not cache stations, which are resolved through the trip's route when read (domain model, §1 notes), nor `seatClass`, on which nothing about a consignment depends. `userAggregateId` is seeded at creation and never refreshed, because `Order → User` is immutable; it serves `ListConsignsOfUser`. There is one event per trigger, because each produces a different reaction: a rebooking refreshes the trip, the date and the indices, while a cancellation and a deletion set the consignment's own `status` to `CANCELLED` and refresh nothing.

**`Contacts` and `Wallet` cache the user's id only.** `UpdateUser` changes nothing either of them caches, so the only event they need is the deletion.

---

## §3 — Upstream / Downstream Event Dependencies

There is one arrow per row of §2. Seven carry events: `User ──► Contacts`, `User ──► Wallet`, `TrainType ──► PriceConfig`, `Route ──► PriceConfig`, `Station ──► StationFoodStore`, `Trip ──► TrainFood` and `Order ──► ConsignRecord`. The other eight are seeded once, when the consumer is created, and never refreshed.

```
Station ─────────────────────────► Route
Station ─────────────────────────► StationFoodStore
TrainType ───────────────────────► PriceConfig
TrainType ───────────────────────► Trip
User ────────────────────────────► Contacts
User ────────────────────────────► Wallet
User ────────────────────────────► Order
User ────────────────────────────► Payment
Route ───────────────────────────► PriceConfig
Route ───────────────────────────► Trip
Trip ────────────────────────────► TicketInventory
Trip ────────────────────────────► Order
Trip ────────────────────────────► TrainFood
Order ───────────────────────────► Payment
Order ───────────────────────────► ConsignRecord
```

> An arrow `A ──► B` means that B caches A's id locally, together with any other fields §2 lists. B subscribes to A's events (§4) only on the seven arrows that carry them. On the other eight, A cannot be deleted, or cannot change anything B reads, while B references it, so the snapshot is seeded once when B is created.

### Notes on the dependencies

**The graph is acyclic and has five levels.**

| Level | Aggregates |
|---|---|
| 0 | `Station`, `TrainType`, `User`, `ConsignPrice` |
| 1 | `Route`, `Contacts`, `Wallet`, `StationFoodStore` |
| 2 | `PriceConfig`, `Trip` |
| 3 | `TicketInventory`, `Order`, `TrainFood` |
| 4 | `Payment`, `ConsignRecord` |

An aggregate is implemented only after every aggregate that has an arrow into it.

**`ConsignPrice` has no arrow.** `ReserveTicket`, `CreateConsign` and `UpdateConsign` read it to compute a price that is then frozen, so nothing caches it (domain model §2, *What is not a relationship*).

**Saga reads and writes are not arrows.** An arrow is a cached reference, not a call.

| Operations | Aggregates they read or write |
|---|---|
| `ReserveTicket` | reads `PriceConfig`, `Contacts` and `ConsignPrice`; writes `TicketInventory` and `ConsignRecord` |
| `PayOrder`, `CancelOrder`, `RebookOrder`, `DeleteOrder` | write `Wallet` |
| `PayOrder`, `RebookOrder` | also write `Payment` |
| `CancelOrder`, `RebookOrder`, `DeleteOrder` | also write `TicketInventory` |

None of these produces an arrow, because no aggregate they write keeps a reference to the order. A seat allocation is identified by its `(seatNumber, fromIndex, toIndex)` triple, not by an order id (domain model, §1 notes). A wallet holds a balance, not a ledger of the orders that moved it.

**Deletion guards run against the arrows.** The upstream aggregate checks every `*_NOT_DELETED_WHILE_USED` rule, and `USER_NOT_DELETED_WITH_LIVE_ORDERS`, by reading its downstream consumers: `DeleteStation` reads `Route`, and `DeleteTrip` reads `TicketInventory` and `Order`. Each guard therefore reads an aggregate from a later level, and cannot be completed until that consumer has been implemented.

**The deletion race is left open on purpose.** A guard reads the consumers without locking them, so a reference created concurrently with the delete can survive it. A subscription to the target's deletion event would work as a backstop, but the consumer would then have to react — invalidate a route, cancel a trip — which is the policy §3.2 of the domain model rejected in favour of refusal. The race is kept as one of the points where the consistency patterns differ (domain model, *Notes on the cross-entity rules*).

---

## §4 — Events

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `DeleteUserEvent` | User | `DeleteUser` — user soft-deleted | `userAggregateId` (anchor) | Contacts, Wallet |
| `DeleteTrainTypeEvent` | TrainType | `DeleteTrainType` — train type soft-deleted | `trainTypeAggregateId` (anchor) | PriceConfig |
| `DeleteRouteEvent` | Route | `DeleteRoute` — route soft-deleted | `routeAggregateId` (anchor) | PriceConfig |
| `DeleteStationEvent` | Station | `DeleteStation` — station soft-deleted | `stationAggregateId` (anchor) | StationFoodStore |
| `DeleteTripEvent` | Trip | `DeleteTrip` — trip soft-deleted | `tripAggregateId` (anchor) | TrainFood |
| `RebookOrderEvent` | Order | `RebookOrder` — order moved to another trip, date, class or seat | `orderAggregateId` (anchor), `tripAggregateId`, `travelDate`, `fromIndex`, `toIndex` | ConsignRecord |
| `CancelOrderEvent` | Order | `CancelOrder` — order status set to `CANCELLED` | `orderAggregateId` (anchor) | ConsignRecord |
| `DeleteOrderEvent` | Order | `DeleteOrder` — order soft-deleted | `orderAggregateId` (anchor) | ConsignRecord |

> **Anchor field:** the field marked `(anchor)` is the publisher aggregate's own ID. It is passed to `super(anchorAggregateId)` in the event constructor and must match the `subscribedAggregateId` used in the corresponding `EventSubscription` subclass. Without this, event filtering is broken. See [`docs/concepts/events.md`](../../docs/concepts/events.md) canonical wiring for the exact pattern.

### Notes on the events

**Eight events, exactly the ones §2 names.** No other operation publishes, because an event with no consumer would be wiring that nothing reads. None of these operations changes anything another aggregate caches:

- the updates of reference data: `UpdateStation`, `UpdateTrainType`, `UpdateRoute`, `UpdateTrip`, `UpdateUser`, `UpdatePriceRates`;
- the deletions of `Contacts` and `PriceConfig`, which nothing references;
- the order's other transitions: `PayOrder`, `CollectTicket`, `EnterStation`, `UpdateOrderAssurance`, `UpdateOrderMeal`.

Adding a consumer later means adding its event here, its arrow in §3 and its event name to its row in §2.

**`RebookOrderEvent` carries the whole new segment.** `tripAggregateId`, `travelDate`, `fromIndex` and `toIndex` are the `ConsignRecordOrder` fields a rebooking can change. `userAggregateId` is left out because `Order → User` is immutable. `seatClass`, `seatNumber` and `price` are left out because the consignment caches none of them. The event carries all four fields even when only some change, and the consumer stamps the new `orderVersion` either way.

**`CancelOrderEvent` and `DeleteOrderEvent` carry the anchor alone.** Both lead to the same change in the consumer: the consignment's own `status` becomes `CANCELLED`, and no cached field is refreshed. They stay two events because they have two triggers. A `DeleteOrderEvent` for an order that is already cancelled finds its consignment terminal and changes nothing (`CONSIGN_CANCELLED_TERMINAL`). `DeleteOrder` on a live order releases the seat and refunds as a cancellation would, but publishes only `DeleteOrderEvent`.

**The five deletion events remove their consumers.** `Contacts` and `Wallet` handle `DeleteUserEvent` by calling `remove()` on themselves (`CONTACTS_DELETED_WITH_USER`, `WALLET_DELETED_WITH_USER`), `PriceConfig` does the same with `DeleteTrainTypeEvent` and `DeleteRouteEvent` (`PRICE_CONFIG_DELETED_WITH_TRAIN_TYPE`, `PRICE_CONFIG_DELETED_WITH_ROUTE`), `StationFoodStore` with `DeleteStationEvent` (`STATION_FOOD_STORE_DELETED_WITH_STATION`) and `TrainFood` with `DeleteTripEvent` (`TRAIN_FOOD_DELETED_WITH_TRIP`). A tariff already removed by one of its two events ignores the other. The Cascade Invalidation Pattern of [`events.md`](../../docs/concepts/events.md) would have them publish an invalidation event in turn. It does not apply here, because no aggregate caches a `Contacts`, a `Wallet`, a `PriceConfig`, a `StationFoodStore` or a `TrainFood`: §3 has no arrow out of any of them.
