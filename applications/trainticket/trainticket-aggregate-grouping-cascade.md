# TrainTicket — Aggregate Grouping (cascade variant)

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

> **This is the second grouping over one plain domain — 2026-09-19.** It is written over
> [`trainticket-domain-model.md`](trainticket-domain-model.md) **without a single edit to it**, and
> that is the point of its existence: the plain-domain separation claims exactly one property, that
> a second grouping can be written over an existing domain model with zero edits, and this file is
> the test of that claim. The sibling grouping
> [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md) is the first.
>
> **One variable.** The two files differ in exactly one decision: the sibling does not cascade, this
> one does. The aggregate boundaries are identical, the entity placement is identical, the §3 arrows
> are identical arrow for arrow, and the service list is identical. Everything else that differs —
> the version fields in §2, the nine events in §4, the six `eventual` rows in §5, three writes that
> become sagas — is a *consequence* of that one decision, and each is named as such where it
> appears.
>
> **Nothing is generated from this file.** `applications/trainticket/` holds the application
> generated from the no-cascade grouping. Implementing this one would be a separate run from a
> separate `plan.md` into its own application directory; it would not overwrite that one.

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

The boundaries are inherited from the benchmark rather than invented, exactly as in the sibling
file. Holding them fixed is what makes the comparison between the two groupings a comparison of
consistency policy and of nothing else.

> **References are by aggregate id, not by natural key.** TrainTicket references entities by name
> (`Route.stations` holds station *names*, `Trip.trainTypeName` holds a type *name*,
> `Order.trainNumber` holds a trip identifier string). The simulator identifies aggregates by
> `Integer aggregateId`, so every cross-aggregate reference in this realisation becomes an id, with
> the human-readable name cached alongside it only where the benchmark genuinely stores a copy. This
> is a realisation decision: the domain model states the references, not their encoding.

---

## §1 — Aggregate Grouping

Identical to the no-cascade grouping. Consistency policy is not a boundary decision, and moving a
boundary here would destroy the comparison.

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
> fields this grouping adds to it are `stationAggregateId: Integer`, `stationVersion: Integer` and
> `stationName: String`, alongside its domain attributes `sequence: Integer` and
> `distanceFromStart: Integer`. The `stationVersion` field is the one difference from the sibling
> file's version of this note, and §2 says why.

---

## §2 — Snapshots

Field names below match the entity attributes declared in §1 of the [domain model](trainticket-domain-model.md),
except for the `*AggregateId` and `*Version` fields, which exist only in this realisation.

> **Every subscribing snapshot carries a publisher version.** An `EventSubscription` is constructed
> from `(subscribedAggregateId, subscribedVersion, eventType)`
> ([`docs/concepts/events.md`](../../docs/concepts/events.md) § EventSubscription), so a snapshot
> that subscribes needs both an id and a version. The sibling grouping needs no version anywhere
> because it subscribes to nothing; here every row whose "Updated on event" column names an event
> gains one. This is the first mechanical consequence of the policy change, and it is the reason
> three creating operations become sagas — see §5.

> **The cascade follows stored references; it never refreshes a frozen value.** This one sentence
> decides every row below, and every `precondition` row that survives in §5. Where a row caches
> another aggregate's *identity*, that identity is a live reference and the cascade keeps it honest.
> Where a row caches a *value* read once at purchase, the domain model has already said what it is:
> "the terms of the purchase, agreed once and never tracking later edits to the entities they were
> read from" (domain §1, "`Order` is a frozen contract"). Refreshing one of those would not repair
> an invariant, it would rewrite a contract. The Order rows are therefore split below by which of
> the two they are, so the policy is visible in the table rather than only in the prose.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Route / RouteStation × N | Station | `stationAggregateId`, `stationVersion`, `stationName` | `UpdateStationEvent`, `DeleteStationEvent` |
| Trip | Route | `routeAggregateId`, `routeVersion` | `DeleteRouteEvent` |
| Trip | TrainType | `trainTypeAggregateId`, `trainTypeVersion` | `UpdateTrainTypeSeatsEvent`, `DeleteTrainTypeEvent` |
| PriceConfig | Route | `routeAggregateId`, `routeVersion` | `DeleteRouteEvent` |
| PriceConfig | TrainType | `trainTypeAggregateId`, `trainTypeVersion` | `DeleteTrainTypeEvent` |
| Contacts | User | `userAggregateId`, `userVersion` | `DeleteUserEvent` |
| Order | Trip (reference) | `tripAggregateId`, `tripVersion` | `TripCapacityChangedEvent`, `DeleteTripEvent` |
| Order | Trip (contract terms) | `tripNumber`, `departureTime` | n/a — frozen contract |
| Order | Contacts (reference) | `contactsAggregateId`, `contactsVersion` | `DeleteContactsEvent` |
| Order | Contacts (contract terms) | `contactsName`, `contactsDocumentType`, `contactsDocumentNumber` | n/a — frozen contract |
| Order | User (account) | `userAggregateId` | n/a — frozen contract |
| Order | Route (endpoints) | `fromStationName`, `toStationName` | n/a — frozen contract |
| Order | PriceConfig (fare) | `price` | n/a — frozen contract |

> **A frozen reference still has a moving version.** `Order → Trip` and `Order → Contacts` are
> declared immutable in domain §2, and `tripAggregateId` / `contactsAggregateId` stay Java `final`
> here. `tripVersion` and `contactsVersion` are not part of that reference; they are the subscriber's
> watermark over the publisher's event stream, and they advance as events are folded in. Freezing
> the reference and advancing the watermark are not in tension: the Order never points at a
> different Trip, it only learns what happened to the one it points at.

> **`Order → User (account)` carries no version, because Order subscribes to no User event.** No
> rule in domain §3.2 constrains the account's existence — `ACCOUNT_EXISTS (Order)` was dropped at
> the §9 review of the domain model for having no benchmark site — and `CONTACTS_BELONG_TO_ACCOUNT`
> is settled by an immutable reference (§5). The account reaches Order along the Contacts chain
> instead: removing a User withdraws its Contacts, which withdraws the Orders booked on them.

> **`departureTime` is derived, not copied.** It is the only entry in the "Fields cached" column that
> is not a field of the source aggregate: it is `Order.travelDate` combined with the `Trip.startTime`
> in force at purchase, computed by the booking saga and frozen on the Order. It is listed on the Trip
> contract-terms row because Trip is where its non-local half comes from, and it is frozen there for
> the reason `DEPARTURE_TIME_MATCHES_TRIP` stays a `precondition` in §5: `UpdateTrip` may move
> `Trip.startTime`, and the cascade is not permitted to move the departure moment a passenger bought.

> **`RouteStation.stationName` is a real copy, and it is the only cached value the cascade does
> refresh.** TrainTicket's `Route` stores station **names**, not ids — `List<String> stations` — and
> every consumer matches on the name. It is not a contract term of anything: a route's station list
> is mutable in the domain, and a renamed station is the same station. `UpdateStationEvent`
> therefore refreshes it, which is what keeps `ROUTE_ENDPOINTS_MATCH_STATION_LIST` true — see the
> handler obligation in §4.

> **Each `RouteStation` subscribes on its own anchor.** A Route with N stations builds N Station
> subscriptions, one per `RouteStation`, each anchored on that row's `stationAggregateId` and
> watermarked by its own `stationVersion`. This is why the version field sits on `RouteStation` and
> not on the `Route` root: it is the collection snapshot that subscribes.

---

## §2.b — Technical fields

**None** — the same answer the sibling grouping gives, and it was re-derived rather than copied.

Cascading does introduce new fields, and none of them is a technical field:

- The six `{publisher}Version` fields are **§2 fields**. The template makes §2 "the sole home of
  `{entity}AggregateId` and `{entity}Version` fields", so a version belongs to the snapshot it
  watermarks, not here.
- **No modification timestamp is needed.** Event ordering is already total and already carried: the
  cached version is the watermark, `EventRepository.findUnprocessedEvents` keeps only events whose
  `publisherAggregateVersion` exceeds it, and a ByEvent mutation rejects an event that does not
  advance it ([`docs/concepts/events.md`](../../docs/concepts/events.md) § "Reject an event that
  does not advance the cached version"). A `lastModifiedTime` would be a second, weaker ordering
  over the same facts.
- **No flag distinguishes a cascade withdrawal from a passenger cancellation.** It was considered
  and declined: no rule in domain §3 predicates on the difference, `ORDER_REFUND_AMOUNT` computes
  the same refund either way, and a field the domain does not ask for is exactly the contamination
  this split exists to keep out. If a later read functionality needs the distinction, it is added
  here with its reason.

> **Soft-delete state.** The simulator's `Aggregate` base class provides `state: AggregateState`
> (`ACTIVE`, `INACTIVE`, `DELETED`) and sets it via `remove()`. It is not a domain attribute and
> appears in no entity row of the domain model. Wherever a domain rule says "`X` has been removed",
> this realisation resolves it against that inherited field (`X.state == DELETED`). Under this
> grouping that field is also what the cascade *writes*: a withdrawal is a `remove()` on the
> downstream aggregate, which is why a withdrawal restores an existence invariant rather than merely
> recording that it broke — a removed entity "no longer counts as existing" (domain §3.2 preamble),
> so an invariant quantified over existing entities holds again once the dangling one is withdrawn.

---

## §3 — Upstream / Downstream Event Dependencies

**Identical to the no-cascade grouping, arrow for arrow.** Nothing is added and nothing is removed.
This is worth stating rather than assuming: the arrows record which aggregate holds a reference to
which, and that is a boundary fact, not a policy one. What the policy changes is whether an arrow
carries an event, not whether it exists.

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

> **The graph is acyclic and at most four hops deep.** The longest propagation path is
> `Station ──► Route ──► Trip ──► Order`; `User ──► Contacts ──► Order` is the other multi-hop path.
> No aggregate subscribes to an event it published, directly or transitively, so R5 holds and the
> event pipeline terminates.

> **Three arrows carry no event, and that is the frozen-value principle, not an oversight.**
> `Route ──► Order`, `PriceConfig ──► Order` and `User ──► Order` exist because Order caches
> something from each: the endpoints, the fare, and the account id. The first two are contract terms
> (§2), and the third is constrained by no rule. Under this policy an arrow without an event means
> "the value was read once and is now the purchase's own", where under the sibling policy it meant
> "nothing propagates anywhere".

> **`Route ──► Order` and `PriceConfig ──► Order` are fare-and-endpoint arrows.** The booking saga
> reads the Trip's Route to resolve `fromStationName` / `toStationName` and validate their ordering,
> and reads the PriceConfig to compute `price`. Order stores the results, so the arrows exist even
> though Order holds no `routeAggregateId` or `priceConfigAggregateId` — what it caches from those
> two aggregates is contract values, not references. That is also precisely why neither arrow can
> carry an event: there is no reference to repair.

> **Every arrow is now a saga fetch.** The sibling file records four arrows seeded from a
> caller-supplied value — `User ──► Contacts`, `User ──► Order`, `Route ──► PriceConfig` and
> `TrainType ──► PriceConfig` — because the rules that would have forced a fetch were dropped at the
> §9 review. Three of the four can no longer be seeded that way: a subscription needs the
> publisher's current version, and `docs/concepts/events.md` states that a missing version field is
> a planning defect rather than something to default to `0L`. `CreateContacts` and
> `CreatePriceConfig` therefore become sagas. `User ──► Order` is the exception that stays a plain
> stored value, because Order subscribes to no User event. See §5, "Other notes".

### 3.a — Consistency policy: cascade, via events

This grouping **cascades**. Every arrow above whose upstream holds a *reference* that can be removed,
or a *cached value* that is not a contract term, carries an event subscription (§4), and the
downstream aggregate's handler repairs itself when the event arrives. The domain's §3.2 standing
invariants are therefore maintained as standing invariants — eventually, within the event poll
interval — rather than merely checked once and then allowed to lapse.

It is the deliberate opposite of the sibling grouping, and it is deliberately **unfaithful to the
benchmark**. TrainTicket propagates nothing: `deleteStation` removes a station that routes still list
by name, `deleteTravel` removes a trip that orders still reference by `trainNumber`, and lowering a
train type's seat count below the number already sold is accepted silently. That is finding **F2** in
the domain model's preamble, and the sibling grouping reproduces it. This file specifies what the
same domain looks like when the defect is repaired by propagation instead.

**The whole policy is two rules.**

1. **Removal propagates along stored references.** When an aggregate learns that an aggregate it
   holds a reference to has been removed, it withdraws itself — `remove()` — and publishes its own
   removal event in turn. The invariant is restored because a withdrawn aggregate no longer counts
   as existing, so the quantifier no longer reaches the dangling reference.
2. **Value copies are never refreshed if they are contract terms.** `Order`'s frozen fields, and the
   rules predicated on them, are outside the cascade entirely. The one cached value that is not a
   contract term, `RouteStation.stationName`, *is* refreshed.

**What this does to each family of domain §3.2 rule.**

- **Existence rules** — `STATIONS_EXIST (Route)`, `ROUTE_AND_TRAIN_TYPE_EXIST (Trip)`,
  `TRIP_EXISTS (Order)`, `CONTACTS_EXIST (Order)` — become **eventual**. A delete event reaches the
  holder of the reference, and the holder withdraws. These four are the rules that make this file
  worth writing: in the sibling they are the four that nothing ever restores.
- **Capacity rules** — `SEAT_CAPACITY_NOT_EXCEEDED` and `SEAT_NUMBER_WITHIN_CAPACITY (Order)` —
  become **eventual**, through a capacity change relayed by the Trip. This is the one family where
  the cascade repairs an *update* rather than a removal, and the design decision it forces is set
  out in full below.
- **Uniqueness rules** — `UNIQUE_STATION_NAME`, `UNIQUE_TRIP_NUMBER`, `UNIQUE_USER_NAME`,
  `UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE`, `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` — are
  unaffected. Each is a predicate over one aggregate's own table; the policy cannot reach them and
  does not need to.
- **Structural rules over the Route's own station list** — the four `ROUTE_*` ordering and distance
  rules, plus `ROUTE_STATIONS_DISTINCT` and `ROUTE_ENDPOINTS_MATCH_STATION_LIST` — stay **intra**
  and hold transactionally, because `RouteStation` is co-located with `Route`. Two of them acquire a
  **handler obligation** they did not have: the event handlers must not break them. See §4.
- **Frozen-contract rules** — `PRICE_MATCHES_TARIFF (Order)`, `PRICE_CONFIG_EXISTS (Order)`,
  `ENDPOINTS_ON_TRIP_ROUTE (Order)`, `DEPARTURE_TIME_MATCHES_TRIP (Order)` — stay **precondition**,
  by decision. This is the selectivity the policy requires, and §5 names the four rows and the
  reason for each.
- **Immutable-reference rules** — `CONTACTS_BELONG_TO_ACCOUNT (Order)` — stay **precondition**
  because there is nothing to propagate: `Contacts → User` is declared immutable in domain §2, so
  once the predicate is established no operation in §4 of the domain model can falsify it.

#### Design decision 1 — `SEAT_CAPACITY_NOT_EXCEEDED` under a capacity reduction

**The problem.** `UpdateTrainType` may lower `economyClassSeats` or `firstClassSeats` below the
number of seats already sold for some `(trip, travelDate, seatClass)`. In the sibling grouping this
is accepted silently, exactly as the benchmark accepts it. Under a cascade it must do *something*,
because the domain states a standing invariant that the reduction has just falsified — and it
falsifies `SEAT_NUMBER_WITHIN_CAPACITY (Order)` at the same moment, for every order whose allocated
seat number now exceeds the new capacity.

**Rejected: refuse the reduction.** The natural first answer is to make `UpdateTrainType` fail when
orders would be orphaned. It is not available. TrainType would have to count Orders, which means the
upstream aggregate reading the downstream one, forbidden by R5
([`docs/architecture.md`](../../docs/architecture.md)) and structurally impossible here since Order
is three hops downstream. It is also not a cascade: it converts a propagation policy into a new
precondition, which would be a second variable.

**Rejected: renumber the surviving orders.** Compacting seat numbers into `[1, newCapacity]` would
restore both invariants without cancelling anyone, but `Order.seatNumber` is Java `final` — a
contract term, domain §1 — and a passenger's seat is part of what they bought.

**Decided: the highest-numbered orders are bumped.** On a capacity reduction, every non-withdrawn
Order on a trip of that train type whose `seatNumber` exceeds the new capacity **for its own seat
class** is cancelled where the state machine permits it, and then withdrawn. Orders at or below the
new capacity are untouched.

**Why bumping on `seatNumber` alone restores both invariants, exactly.**
`SEAT_NUMBER_UNIQUE_PER_DEPARTURE` guarantees that no two non-cancelled orders for one
`(trip, travelDate, seatClass)` share a seat number. The survivors of the bump therefore hold
*distinct* seat numbers drawn from `[1, newCapacity]`, so there are at most `newCapacity` of them —
which is `SEAT_CAPACITY_NOT_EXCEEDED`. And each survivor satisfies
`1 <= seatNumber <= newCapacity` by construction — which is `SEAT_NUMBER_WITHIN_CAPACITY`. One
per-order test, decided from fields the Order already holds, restores both counted invariants with
no cross-order query and no coordination between Orders. That is why this is the chosen rule and not
merely a defensible one.

**Cancel, then withdraw.** Cancelling first is what pays the passenger:
`ORDER_CANCELLATION_FIELDS_SET` and `ORDER_REFUND_AMOUNT` (domain §3.1) then apply unchanged, so the
refund is the domain's refund — `0` if the order was `NOTPAID`, `0` if the departure has passed,
otherwise 80% of the fare. The cascade does **not** invent a full refund for an operator-caused
withdrawal; inventing one would be a rule the domain model does not state. Withdrawing second is
what restores the invariant, since a removed Order no longer counts as existing.

**Where the state machine forbids the cancellation, only the withdrawal happens.**
`ORDER_STATUS_TRANSITION` allows `NOTPAID → CANCELLED` and `PAID → CANCELLED` and nothing else, so a
`COLLECTED` or `USED` order cannot be cancelled and is withdrawn without a refund, and an already
`CANCELLED` order is withdrawn with its existing refund intact — which is also what the
`prev.status != CANCELLED` conjunct of `ORDER_REFUND_AMOUNT` is there for. Withdrawing a `USED`
ticket with no refund is the accepted consequence of the policy, and it is accepted because the
alternative is to leave the invariant broken, which is the sibling grouping's answer and not this
one's.

**Raising the capacity propagates too, and does nothing.** The event carries both counts and every
Order on the trip receives it; an Order whose seat number is within the new capacity folds the event
in, advances its `tripVersion` and mutates nothing. Handlers must be idempotent under re-application
in any case ([`docs/concepts/events.md`](../../docs/concepts/events.md) § polling backlog).

**Under `/implement-aggregate` this is a Type 2 halt.** It is a design decision, not a
contradiction, and the harness gate would require it to be asked rather than assumed. It is answered
here, in the specification, which is where it belongs.

#### Design decision 2 — `PRICE_MATCHES_TARIFF`, and the three rules that skip with it

**The problem.** A cascade that propagated every upstream change would reprice a purchased ticket
when `UpdatePriceConfig` changes a rate, and would invalidate one when `DeletePriceConfig` removes
the tariff the fare was computed from. Neither is acceptable, and neither is possible:
`Order.price` is Java `final`.

**Decided: the cascade is selective, and the criterion is stated once rather than per rule.** The
cascade follows stored references and never refreshes a frozen value. Four rules fall on the frozen
side, and §5 marks all four `precondition` with this as the reason:

| Rule | What it predicates on | Why the cascade cannot and must not restore it |
|---|---|---|
| `PRICE_MATCHES_TARIFF (Order)` | `Order.price`, against the PriceConfig rates | The fare is the purchase's price. A later rate change is a change to what *future* bookings cost |
| `PRICE_CONFIG_EXISTS (Order)` | a PriceConfig for `(Trip.route, Trip.trainType)` | Order stores no PriceConfig reference at all (§2) — there is nothing dangling to repair. Removing the tariff withdraws no order |
| `ENDPOINTS_ON_TRIP_ROUTE (Order)` | `Order.fromStationName` / `toStationName` against the route's station list | Both endpoints are `final`. `UpdateRoute` may drop a station an order departs from; the order's own endpoints cannot move to follow it |
| `DEPARTURE_TIME_MATCHES_TRIP (Order)` | `Order.departureTime` against `Trip.startTime` | `departureTime` is `final` and is what `ORDER_REFUND_AMOUNT` predicates on. Moving it would silently re-decide a refund |

**What this costs, stated plainly.** These four invariants can be violated in a running system under
this grouping and will not be repaired, exactly as they can under the sibling one. The cascade
narrows the set of unrepaired invariants from ten to four; it does not empty it, and a grouping that
claimed to empty it would be claiming to mutate `final` fields. The honest summary is that the
frozen contract is a boundary the consistency policy does not cross, and the domain model draws that
boundary itself in its `Order` note — the grouping only reads it.

**A tempting non-answer.** "Cancel the order when its tariff or route changes" would technically
restore `PRICE_CONFIG_EXISTS` and `ENDPOINTS_ON_TRIP_ROUTE` by withdrawal, the same device the
existence rules use. It is declined because the analogy fails: withdrawal is right when the *service
sold* no longer exists, and wrong when an *input consulted once* has been edited. A tariff revision
is a routine administrative act, and a policy that cancelled every historical order on every price
edit would be a worse specification than the one it replaced.

#### Consequences

1. **§4 is non-empty: nine events.** The sibling grouping has none.
2. **This grouping reaches P2.** Six domain rules are realised by event propagation here, so the
   inter-invariant pattern is exercised; the sibling grouping states that no rule in it is P2, which
   is the acknowledged gap this variant closes. Which pattern each rule actually receives is still
   derived by `/classify-and-plan` and is not stated in §5.
3. **Removal is transitive, up to four hops.** `DeleteStation` can withdraw a Route, its Trips, its
   PriceConfigs, and the Orders on those Trips. `DeleteUser` can withdraw its Contacts and their
   Orders. A specification reader should expect a single administrative delete to have a wide blast
   radius; that is what a cascade is.
4. **References still dangle where the frozen contract says they should.** Four rules, listed above.
5. **A precondition guard is still phrased pre-mutation.** `SEAT_CAPACITY_NOT_EXCEEDED` is *also*
   checked at booking time, and that check is unchanged from the sibling grouping: it runs before
   the aggregate is mutated, so it is written `count(existing) < capacity` rather than as the
   domain's post-state `count(all) <= capacity`. Transcribing the post-state form literally into a
   pre-mutation guard would admit `capacity + 1` bookings. The cascade adds a repair path; it does
   not remove the booking-time guard, and a rule can be both.
6. **Three writes become sagas** — `CreateContacts`, and `CreatePriceConfig` twice over — because a
   subscription must be seeded with the publisher's version. See §5.
7. **Phase 2 is 29 sessions, not 24 and not 32.** See §5, "Other notes", which works the arithmetic.

---

## §4 — Events

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `UpdateStationEvent` | Station | `UpdateStation` changes `name` | `stationAggregateId` (anchor), `name` | Route |
| `DeleteStationEvent` | Station | `DeleteStation` | `stationAggregateId` (anchor) | Route |
| `DeleteRouteEvent` | Route | `DeleteRoute`, or Route withdrawing itself on `DeleteStationEvent` | `routeAggregateId` (anchor) | Trip, PriceConfig |
| `UpdateTrainTypeSeatsEvent` | TrainType | `UpdateTrainType` changes `economyClassSeats` or `firstClassSeats` | `trainTypeAggregateId` (anchor), `economyClassSeats`, `firstClassSeats` | Trip |
| `DeleteTrainTypeEvent` | TrainType | `DeleteTrainType` | `trainTypeAggregateId` (anchor) | Trip, PriceConfig |
| `TripCapacityChangedEvent` | Trip | Trip folds in `UpdateTrainTypeSeatsEvent` | `tripAggregateId` (anchor), `economyClassSeats`, `firstClassSeats` | Order |
| `DeleteTripEvent` | Trip | `DeleteTrip`, or Trip withdrawing itself on `DeleteRouteEvent` or `DeleteTrainTypeEvent` | `tripAggregateId` (anchor) | Order |
| `DeleteUserEvent` | User | `DeleteUser` | `userAggregateId` (anchor) | Contacts |
| `DeleteContactsEvent` | Contacts | `DeleteContacts`, or Contacts withdrawing itself on `DeleteUserEvent` | `contactsAggregateId` (anchor) | Order |

> **Anchor field:** the field marked `(anchor)` is the publisher aggregate's own ID. It is passed to
> `super(anchorAggregateId)` in the event constructor and must match the `subscribedAggregateId`
> used in the corresponding `EventSubscription` subclass. Without this, event filtering is broken.
> See [`docs/concepts/events.md`](../../docs/concepts/events.md) canonical wiring for the exact
> pattern.

> **`TripCapacityChangedEvent` is a relay, and the relay is the design.** The capacity lives on
> TrainType, the invariant is enforced over Orders, and Order holds no `trainTypeAggregateId`. The
> two ways to close that gap are to give Order a TrainType snapshot — which adds an arrow to §3 and
> so changes a second variable — or to let the Trip, which already sits between them and already
> subscribes to TrainType, republish the new capacity on its own anchor. The second keeps §3
> identical to the sibling file's, and it is semantically the better reading: capacity is a property
> of the *departure* an order was sold on, and Trip is the aggregate the order references. The
> precedent is `InvalidateQuizEvent` in `quizzes-full-2`, where a consumer republishes on its own
> anchor after folding in an upstream event.

> **Trip caches no seat counts.** The relay forwards the payload it received; it does not store it.
> The booking-time capacity check is a saga fetch of the TrainType, unchanged from the sibling
> grouping, so nothing needs a second copy of the number.

> **Two handler obligations on Route.** `UpdateStationEvent` refreshes `RouteStation.stationName`,
> and the handler must also update `Route.startStationName` / `Route.endStationName` when the
> renamed station is the route's first or last by `sequence` — otherwise the refresh itself breaks
> `ROUTE_ENDPOINTS_MATCH_STATION_LIST`, which is an `intra` rule the cascade is not allowed to
> falsify. `ROUTE_STATIONS_DISTINCT` resolves on `stationAggregateId`, which a rename does not
> touch, so it needs nothing.

> **Why `DeleteStationEvent` withdraws the whole Route.** The alternative is to excise the affected
> `RouteStation`, and it was rejected: excision forces a resequence to keep
> `ROUTE_SEQUENCE_CONTIGUOUS`, and either a rebase or a gap in `distanceFromStart`, which silently
> changes the fare of every future booking on that route — `PRICE_MATCHES_TARIFF` reads exactly
> those distances. Excising the first or last station also moves the route's endpoints, and excising
> from a two-station route falsifies `ROUTE_HAS_AT_LEAST_TWO_STATIONS` with no repair available.
> Withdrawal keeps every `ROUTE_*` structural invariant true of the route as authored and lets the
> operator re-author it. Note that domain §2 marks `RouteStation → Station` as **not** a
> composition, so nothing in the domain forces either answer: this is the grouping's decision to
> make, and it is made here.

> **No `UpdateRouteEvent`, `UpdatePriceConfigEvent`, `UpdateContactsEvent` or `UpdateUserEvent`.**
> Each would have exactly one candidate consumer, and in each case what that consumer holds is a
> frozen contract term or nothing at all. Publishing an event no aggregate can act on would be
> machinery without a rule behind it.

> **No `DeletePriceConfigEvent`.** PriceConfig is a pure sink in §3: no aggregate stores a reference
> to it. `PRICE_CONFIG_EXISTS (Order)` is the rule that would have wanted one, and it is one of the
> four frozen-contract skips.

> **Handlers are idempotent and version-monotonic.** A consumer's first poll drains the publisher's
> whole backlog, and a batch is delivered newest-first, so a ByEvent mutation must reject an event
> that does not advance the cached version
> ([`docs/concepts/events.md`](../../docs/concepts/events.md)). Withdrawal handlers get this for
> free — `remove()` on an already-removed aggregate is a no-op — but the `UpdateStationEvent` and
> `TripCapacityChangedEvent` handlers carry payloads and need the check.

---

## §5 — Cross-file notes

### Rule realisation

One row per cross-entity rule in domain §3.2. `intra` means the rule resolves inside a single
aggregate of this grouping — either because the entities are co-located, or because the fields it
needs are carried by a snapshot in §2 that nothing can invalidate. `precondition` means it is
established when the operation runs and is not restored if it is later violated. `eventual` means an
event in §4 restores it after an upstream change.

**Six rows differ from the sibling grouping, and no row differs in the other direction.** Every
`intra` row is `intra` in both files; six `precondition` rows become `eventual`; four
`precondition` rows stay `precondition` by the frozen-value decision above; one stays
`precondition` because nothing can invalidate it. That the `intra` rows do not move is the
structural check on the claim that only one variable changed: `intra` is a function of the
boundaries, and the boundaries did not move.

| Rule (domain §3.2) | Realisation | Note |
|---|---|---|
| ROUTE_HAS_AT_LEAST_TWO_STATIONS | intra | `RouteStation` is co-located with `Route` |
| ROUTE_SEQUENCE_CONTIGUOUS | intra | co-located |
| ROUTE_DISTANCES_MONOTONIC | intra | co-located |
| ROUTE_FIRST_DISTANCE_IS_ZERO | intra | co-located |
| ROUTE_STATIONS_DISTINCT | intra | resolves on the cached `stationAggregateId` of each `RouteStation`, which a rename does not change |
| ROUTE_ENDPOINTS_MATCH_STATION_LIST | intra | resolves on the cached `stationName` of each `RouteStation`; the `UpdateStationEvent` handler must update `Route.startStationName` / `endStationName` with it — §4, handler obligations |
| STATIONS_EXIST (Route) | **eventual** | `DeleteStationEvent` reaches the Route, which withdraws itself and publishes `DeleteRouteEvent`; the Station fetch in `CreateRoute` / `UpdateRoute` still establishes it at operation time |
| UNIQUE_STATION_NAME | intra | own-table uniqueness inside the Station aggregate |
| ROUTE_AND_TRAIN_TYPE_EXIST (Trip) | **eventual** | `DeleteRouteEvent` or `DeleteTrainTypeEvent` withdraws the Trip, which publishes `DeleteTripEvent`; the fetches in `CreateTrip` still establish it |
| UNIQUE_TRIP_NUMBER | intra | own-table uniqueness inside the Trip aggregate |
| UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE | intra | own-table uniqueness over the cached `(routeAggregateId, trainTypeAggregateId)` pair |
| UNIQUE_USER_NAME | intra | own-table uniqueness inside the User aggregate |
| TRIP_EXISTS (Order) | **eventual** | `DeleteTripEvent` cancels the Order where the state machine permits and then withdraws it; the Trip fetch in `PreserveTicket` still establishes it |
| CONTACTS_EXIST (Order) | **eventual** | `DeleteContactsEvent`, same handling; the Contacts fetch in `PreserveTicket` still establishes it |
| CONTACTS_BELONG_TO_ACCOUNT (Order) | precondition | nothing to propagate: `Contacts → User` is immutable in domain §2, so no operation can falsify the predicate once `PreserveTicket` has compared the requested account against the fetched contact's |
| ENDPOINTS_ON_TRIP_ROUTE (Order) | precondition | **frozen-value skip** — `Order.fromStationName` / `toStationName` are `final`; a later `UpdateRoute` is not propagated. Design decision 2 |
| PRICE_CONFIG_EXISTS (Order) | precondition | **frozen-value skip** — Order stores no PriceConfig reference, so nothing dangles. The `GetPriceConfigByRouteAndTrainType` fetch still fails the booking when no configuration exists, which is the rule name `docs/concepts/rule-enforcement-patterns.md` § P4 requires that saga step to cite. Design decision 2 |
| PRICE_MATCHES_TARIFF (Order) | precondition | **frozen-value skip** — `Order.price` is `final`; a later rate change reprices nothing already sold. Design decision 2 |
| DEPARTURE_TIME_MATCHES_TRIP (Order) | precondition | **frozen-value skip** — `Order.departureTime` is `final` and `ORDER_REFUND_AMOUNT` predicates on it; a later `UpdateTrip` is not propagated. Design decision 2 |
| SEAT_CAPACITY_NOT_EXCEEDED | **eventual** | `TripCapacityChangedEvent` bumps every Order on the trip whose `seatNumber` exceeds the new capacity for its class; the booking-time guard is unchanged and still phrased pre-mutation. Design decision 1 |
| SEAT_NUMBER_UNIQUE_PER_DEPARTURE | intra | counted over the Order aggregate's own table; it is also what makes the bump in design decision 1 sufficient |
| SEAT_NUMBER_WITHIN_CAPACITY (Order) | **eventual** | restored by the same bump, per order rather than per departure; still tested at booking time against the capacity scalar the saga passes in |

> **This table is a policy record, not a classification.** Which pattern (P1–P4) each rule gets is
> derived from it by `/classify-and-plan`, not stated in it.

> **`eventual` does not replace the operation-time check.** Every `eventual` row above is *also*
> established by a saga fetch when the operation runs — booking a ticket on an already-removed Trip
> still fails immediately rather than succeeding and being withdrawn a poll later. The difference
> between the two groupings is what happens *after* the operation, not during it.

### Other notes

- **What this file is for.** The sibling grouping's own §5 predicted it: "A cascade variant is the
  planned follow-up... It requires **no edit to the domain model**: the invariants are already
  stated as standing invariants there, and all that changes is this file's §3.a and the Realisation
  column above... Writing it is the falsification test of the plain-domain split." That prediction
  is confirmed, with one qualification and one correction, both below.
- **Zero edits to the domain model.** Writing this file required no change to
  `trainticket-domain-model.md` — not a rule restatement, not a field, not a word. The
  `/review-artifacts` Check 5 contamination scan passes over it before and after, and the file is
  byte-identical across the exercise.
- **The qualification: the domain model names one grouping, six times.** Its "How to use this file"
  pointer, three preamble sentences, the §3.2 preamble and the §4 note each link to
  `trainticket-aggregate-grouping.md` in the singular. None of those sentences states a
  decomposition decision — each defers one to "the grouping" and links an example of one — so none
  is contamination and none had to change. It is a cross-reference convention that a
  two-grouping application makes incomplete rather than wrong, and whether the templates should say
  that a domain model's grouping pointers are exemplary rather than exhaustive is a question for the
  human, between runs.
- **The correction: three writes become sagas.** The sibling grouping records that `CreateContacts`
  and `CreatePriceConfig` are single-aggregate writes, because the rules that would have made them
  fetch their upstreams were dropped at the §9 review for lack of benchmark support. Under this
  policy they cannot be: `Contacts` must seed `userVersion`, and `PriceConfig` must seed
  `routeVersion` and `trainTypeVersion`, and a subscription version cannot be supplied by a caller.
  Both become sagas that fetch the upstream aggregate. A side effect worth being explicit about:
  those fetches would *incidentally* fail when the upstream does not exist, which looks like
  `ACCOUNT_EXISTS (Contacts)` and the PriceConfig block of `ROUTE_AND_TRAIN_TYPE_EXIST` — two rules
  the domain model deliberately dropped. This file does not reinstate them. The fetch exists to seed
  a version, no rule name is claimed for it, and the domain model stays as it is.
- **Two cascades run past the end of the rule set, and that is disclosed rather than hidden.**
  `DeleteRouteEvent` / `DeleteTrainTypeEvent` withdraw the `PriceConfig`, and `DeleteUserEvent`
  withdraws the `Contacts`, yet neither withdrawal appears as an `eventual` row in the table above —
  because no rule in domain §3.2 requires it. They follow from the policy's first rule (removal
  propagates along stored references) applied uniformly, and uniformity is the reason they are kept:
  a tariff keyed on a route that no longer exists is unreachable through
  `GetPriceConfigByRouteAndTrainType`, and a contact record owned by no account cannot be booked
  against. A grouping is entitled to propagate further than the stated invariants require; it is not
  entitled to do so silently, which is what this note is for.
- **Phase 2 is 29 sessions.** Session `d` produces `{Aggregate}InterInvariantTest.groovy` and is
  generated only for aggregates with a non-empty Events subscribed list — `docs/workflow.md` sets
  the `d` checkbox "only for aggregates that have a non-empty Events subscribed list", and
  `classify-and-plan` § "Step 8" omits the section entirely when that list is empty. Five of the
  eight aggregates subscribe to something: Route, Trip, PriceConfig, Contacts and Order. Station,
  TrainType and User are pure publishers and get no session `d`. So Phase 2 is 8 × `a b c` plus five
  `d` sessions = **29**, against the sibling grouping's 24. It is *not* 32: that figure assumes
  every aggregate subscribes, which a DAG with three sources cannot do.
- **The T3 Subscription test type is recovered.** The sibling grouping gives it up as the
  acknowledged price of no-cascade — no events means no subscriptions means nothing for a T3 test to
  exercise. Here, five aggregates get one. Exercising T3 on a second application is the concrete
  research return on writing this file, and it is available on a domain model that was authored for
  the other policy.
- **`CONTACTS_BELONG_TO_ACCOUNT` declares no User read.** Both sides of its predicate are local to
  the booking saga: the account comes from the request and the contact's account from the Contacts
  fetch that `PreserveTicket` already declares. Listing User among the rule's entities would make
  `/classify-and-plan` raise a cross-aggregate prerequisite with no operation to satisfy it, which
  is why the domain model's note says the account appears only as a shared reference target. This is
  unchanged from the sibling grouping, and it is why `PreserveTicket` still does not read User even
  though `CreateContacts` now does.
- **`ENDPOINTS_ON_TRIP_ROUTE` declares no Station read.** The predicate resolves entirely from
  `RouteStation.stationName` and `RouteStation.sequence`, which the Route fetch already carries, so
  `PreserveTicket` declares no Station read.
- **No functionality is scheduled before an aggregate it reads.** `SearchTrips` was, being
  Trip-primary while reading Order, which the topological sort places last; it was cut at the §9
  review. Every read is implementable in its primary aggregate's session `b`, and there is no
  revisit session. If `SearchTrips` returns as the extension §7 of the rationale plans, this
  constraint returns with it, and `classify-and-plan` § "Step 5.5b" is the mechanism that catches
  it. The event DAG adds no ordering constraint of its own beyond the one the arrows already impose
  on the aggregate sort.
- **`GetLeftTicketCount` is the application's one read saga.** It assembles state from three
  aggregates without writing anything, and is TrainTicket's `ts-seat-service.getLeftTicketOfInterval`.
  `PreserveTicket` is correspondingly the only multi-aggregate write — `CreateContacts` and
  `CreatePriceConfig` become multi-aggregate *reads* under this policy, but neither writes more than
  its own aggregate.
- **Order's immutability is what makes the cascade selective.** The sibling grouping says the same
  fact the other way round — "Order's immutability is what makes empty §4 safe". Both readings
  depend on the identical domain fact, that every field `Order` copies from another aggregate is
  Java `final`. If a later change makes any of them mutable, the sibling's frozen-contract argument
  collapses *and* the four frozen-value skips here would have to be re-decided. That the same
  sentence of the domain model is load-bearing for two opposite policies is the clearest evidence
  that it belongs in the domain model and not in either grouping.
- **Seat capacity is still enforced inside one aggregate at booking time.**
  `SEAT_CAPACITY_NOT_EXCEEDED` and `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` resolve against the Order
  aggregate's own table; `SEAT_NUMBER_WITHIN_CAPACITY` reads no rows at all and tests the allocated
  number against a single passed-in scalar. Only the capacity *limit* crosses a boundary, and the
  booking saga passes it in once from the Trip's TrainType, serving all three. What this grouping
  adds is a second, later path by which the limit can change under the rows already written.
  Introducing a seat-inventory aggregate would turn the capacity relay into a single subscriber and
  is the obvious third grouping over this domain.
- **`PriceConfig` is the only aggregate keyed on two foreign aggregates.**
  `UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE` is an own-table uniqueness check over the
  `(routeAggregateId, trainTypeAggregateId)` pair. Co-locating `PriceConfig` inside `Route` in a
  future grouping would turn it into an intra-invariant and remove one aggregate — a boundary
  change, and therefore a different experiment from this one.
- **`Trip` is a template; `Order` carries the date.** Nothing in `Trip` is per-departure. Every rule
  about a concrete journey — capacity, seat uniqueness, the refund window — keys on
  `(tripAggregateId, travelDate)` held by `Order`. This is why `TripCapacityChangedEvent` cannot
  itself decide which orders to bump: the Trip does not know its departures, and the decision is
  made per Order from fields the Order holds.

---
