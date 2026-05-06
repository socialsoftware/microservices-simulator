# 04 — Cross-Aggregate Dependencies (Train-Ticket)

Derived from REST call graph + reference fields in `02-fields.md`. Since the source has no events (`03-events.md`), inter-invariants and subscriptions below are **proposed**; `onDelete` policies are guesses based on domain semantics — each flagged for confirmation.

## Order
### References
- `Order.accountId -> User` — **onDelete: prevent**, message `"Cannot delete user with existing orders"`. Alt: cascade (delete user ⇒ delete orders). **⚠ Gap** — pick one.
- `Order.trainNumber -> TrainType` (by name) — **onDelete: prevent**, `"Train type is in use by existing orders"`.
- `Order.from / Order.to -> Station` (by name) — **onDelete: prevent**, `"Station has orders referring to it"`.

### Inter-invariants (INVENTED)
```
interInvariant orderRequiresLiveTrainType {
    subscribe TrainTypeDeletedEvent from TrainType { no order exists with trainNumber == event.name }
}
interInvariant orderRequiresLiveStation {
    subscribe StationDeletedEvent from Station { no order exists with from == event.name || to == event.name }
}
```

### Subscriptions (projection refresh, if we treat snapshots as projections — see `02-fields.md`)
- `ContactsUpdatedEvent` → refresh `contactsName`, `documentType`, `contactsDocumentNumber` on orders with matching `accountId`+contact
- `StationRenamedEvent` → refresh `from` / `to` on matching orders

## Trip
### References
- `Trip.trainNumber -> TrainType` — **onDelete: prevent**, `"Trip uses this train type"`.
- `Trip.routeId -> Route` — **onDelete: prevent**, `"Trip uses this route"`.
- `Trip.startingStationName / stationsName[] / terminalStationName -> Station` — **onDelete: prevent**, `"Station is part of a trip"`.

### Inter-invariants (INVENTED)
```
interInvariant tripRequiresLiveRoute {
    subscribe RouteDeletedEvent from Route { no trip exists with routeId == event.routeId }
}
interInvariant tripRequiresLiveTrainType {
    subscribe TrainTypeDeletedEvent from TrainType { no trip exists with trainNumber == event.name }
}
```

### Subscriptions
- `RouteUpdatedEvent` → refresh `stationsName`
- `StationRenamedEvent` → refresh name projections

## Contacts
### References
- `Contacts.accountId -> User` — **onDelete: cascade** (deleting the user removes their saved passengers).

### Subscriptions
- `UserDeletedEvent` → delete own rows with matching `accountId`

## PriceConfig
### References
- `PriceConfig.trainType -> TrainType` — **onDelete: prevent**, `"Price config in use"`.
- `PriceConfig.routeId -> Route` — **onDelete: prevent**, `"Price config in use"`.

## FoodOrder
### References
- `FoodOrder.orderId -> Order` — **onDelete: cascade** (order cancellation removes line-items).
- `FoodOrder.stationName -> Station` (nullable, when foodType=2) — **onDelete: setNull** or cascade. **⚠ Gap** — undecided.
- `FoodOrder.storeName -> StationFoodStore` — **onDelete: setNull**. **⚠ Gap**.
- `FoodOrder.foodName -> TrainFood` / `StationFoodStore` — snapshot; no reference needed unless projection.

### Inter-invariants
```
interInvariant foodOrderRequiresLiveOrder {
    subscribe OrderCancelledEvent from Order { on event, cancel this food order }
}
```
(Reactive — cascade cancellation rather than forbid-state.)

## Assurance
### References
- `Assurance.orderId -> Order` — **onDelete: cascade**.

### Inter-invariants
- Reactive cancel on `OrderCancelledEvent`.

## Consign
### References
- `Consign.orderId -> Order` — **onDelete: prevent** (consign may need to complete even if order is closed — **⚠ Gap**, could also be cascade).
- `Consign.accountId -> User` — **onDelete: prevent**.
- `Consign.from / to -> Station` — **onDelete: prevent**.

### Subscriptions
- `OrderCancelledEvent` → cancel consign if in an abortable state.

## Payment (outside) / InsidePayment
### References
- `Payment.orderId / InsidePayment.orderId -> Order` — **onDelete: prevent** (audit trail must survive).
- `Payment.userId / InsidePayment.userId -> User` — **onDelete: prevent** (audit trail).

### Inter-invariants (INVENTED)
```
interInvariant paymentRequiresPaidOrder {
    subscribe OrderPaidEvent from Order { mark this payment as confirmed }
}
interInvariant paymentRefundOnCancel {
    subscribe OrderCancelledEvent from Order { trigger refund if this payment exists for order }
}
```

## Balance
### References
- `Balance.accountId -> User` — **onDelete: prevent** (financial audit).

## Delivery
### References
- `Delivery.orderId -> Order` — **onDelete: cascade** (no fulfilment if order is gone).
- `Delivery.stationName / storeName` — snapshot.

### Inter-invariants
- Reactive cancel on `OrderCancelledEvent`.

## Route → Station
- `Route.stations[] / startStation / endStation -> Station` — **onDelete: prevent** with message `"Station is part of a route"`.

## Aggregates with no outgoing references
- `Station`, `TrainType`, `User`, `Config`, `ConsignPrice`, `StationFoodStore` (beyond its own station-name string which is effectively a lookup), `TrainFood` (beyond train number).

---

## ⚠ Gaps in this step
- Every `onDelete` policy is inferred. Train-Ticket doesn't actually enforce most of these (it deletes freely) — user must confirm the intended policy per reference.
- Payment onDelete policy: Train-Ticket retains payments forever, but the DSL requires a policy — `prevent` is the safe default.
- Consign onDelete-from-Order is ambiguous (cascade vs prevent).
- FoodOrder's station/store references: whether to cascade or set-null.
- Name-based references (`trainNumber`, `from`, `to`, `stationName`) cross aggregate boundaries using unique business keys rather than ids — the DSL's reference model may prefer id-based; needs a mapping decision.
