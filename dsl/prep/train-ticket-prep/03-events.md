# 03 — Events (Train-Ticket)

## ⚠ MAJOR GAP — no domain events in the source

Train-Ticket is a **REST-only** benchmark. The survey of every service turned up:
- no Kafka / RabbitMQ publishers
- no `ApplicationEventPublisher` / `@EventListener` usage for cross-service events
- no outbox tables

Every cross-aggregate flow is a synchronous REST call (see `04-cross-aggregate.md` for the graph). The closest thing to an asynchronous trigger is Spring Cloud's service-discovery, not domain eventing.

This is a fundamental mismatch with the Nebula DSL's event-driven consistency model (sagas subscribe to events; projections refresh on events). To express Train-Ticket in Nebula we have to **invent the events** that the current REST-based flows would emit if re-implemented asynchronously.

## Proposed event set (to be validated)

Below is a minimal, inferred event catalogue derived from the REST operations observed. Each event is labelled **INVENTED** to mark it as not present in the source.

### Order (publishes)
- **OrderCreatedEvent** (INVENTED) — `orderId:String, accountId:String, trainNumber:String, travelDate:String, from:String, to:String, price:String, status:int`
- **OrderPaidEvent** (INVENTED) — `orderId:String, accountId:String`
- **OrderCancelledEvent** (INVENTED) — `orderId:String, accountId:String`
- **OrderRebookedEvent** (INVENTED) — `previousOrderId:String, newOrderId:String, accountId:String`
- **OrderStatusChangedEvent** (INVENTED) — `orderId:String, status:int`

### Order (subscribes)
- `ContactsUpdatedEvent` → refresh projected `contactsName`, `documentType`, `contactsDocumentNumber` (only if we decide these are projections vs snapshots — see `02-fields.md`)
- `StationRenamedEvent` → refresh `from`, `to`

### Trip (publishes)
- **TripCreatedEvent** (INVENTED) — `tripId, trainNumber, routeId, startingStationName, terminalStationName, startingTime, endTime`
- **TripUpdatedEvent** (INVENTED) — same payload
- **TripDeletedEvent** (INVENTED) — `tripId`

### Trip (subscribes)
- `RouteUpdatedEvent` → invalidate cached station-name list
- `StationRenamedEvent` → refresh station-name projections
- `TrainTypeDeletedEvent` → enforce inter-invariant (prevent / cascade)

### TrainType (publishes)
- **TrainTypeCreatedEvent** / **TrainTypeUpdatedEvent** / **TrainTypeDeletedEvent** — `trainTypeId, name`

### Station (publishes)
- **StationCreatedEvent** / **StationRenamedEvent** / **StationDeletedEvent** — `stationId, name`

### Route (publishes)
- **RouteCreatedEvent** / **RouteUpdatedEvent** / **RouteDeletedEvent** — `routeId, startStation, endStation, stations[]`

### User (publishes)
- **UserCreatedEvent** / **UserUpdatedEvent** / **UserDeletedEvent** — `userId, userName, email`

### Contacts (publishes)
- **ContactsCreatedEvent** / **ContactsUpdatedEvent** / **ContactsDeletedEvent** — `contactsId, accountId, name, documentType, documentNumber`

### Contacts (subscribes)
- `UserDeletedEvent` → cascade delete contacts for that account

### PriceConfig (publishes)
- **PriceConfigUpdatedEvent** (INVENTED) — `priceConfigId, trainType, routeId, basicPriceRate, firstClassPriceRate`

### FoodOrder (publishes)
- **FoodOrderCreatedEvent** / **FoodOrderCancelledEvent** — `foodOrderId, orderId`

### FoodOrder (subscribes)
- `OrderCancelledEvent` → cascade cancel food line-items

### StationFoodStore / TrainFood
- CRUD events if renames should propagate to `FoodOrder.foodName` snapshots.

### Assurance (publishes)
- **AssuranceCreatedEvent** / **AssuranceCancelledEvent** — `assuranceId, orderId, type`

### Assurance (subscribes)
- `OrderCancelledEvent` → cascade cancel assurance

### Consign (publishes)
- **ConsignCreatedEvent** / **ConsignStatusChangedEvent** — `consignId, orderId, status`

### Consign (subscribes)
- `OrderCancelledEvent` → cancel consign if present

### Payment (publishes)
- **PaymentSucceededEvent** (INVENTED) — `paymentId, orderId, userId, price`
- **PaymentRefundedEvent** (INVENTED) — same

### Payment / InsidePayment (subscribes)
- `OrderCreatedEvent` → enable payment for that order
- `OrderCancelledEvent` → trigger refund

### Balance (publishes)
- **BalanceCreditedEvent** / **BalanceDebitedEvent** — `balanceId, accountId, amount`

### Delivery (subscribes)
- `FoodOrderCreatedEvent` → schedule delivery
- `OrderCancelledEvent` → cancel delivery

---

## ⚠ Gaps in this step
- **All of the above events are inferred; none exist in the source.** Every event name / payload is a design proposal that the user must validate before Nebula generation.
- Order-status workflow: Train-Ticket uses a single `status:int` field with opaque integer codes. The exact status-transition graph (paid → collected → used → refunded, etc.) is not explicit in the entity; it lives in controller logic we did not exhaustively trace. Need the user to supply the status-machine or we risk missing events.
- Whether "rename" events (StationRenamedEvent, etc.) should exist depends on whether projected name-fields on Order/Trip are snapshots or live — pending decision from `02-fields.md`.
- No evidence that Train-Ticket supports idempotent retries; if events are introduced, idempotency keys must be specified per handler.
