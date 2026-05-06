# SUMMARY — Train-Ticket DSL Prep

Output folder: `dsl/prep/train-ticket-prep/`
Source analysed: `/home/tomas/Tese/train-ticket/` (47 services; ~25 core domain services inspected).

## Counts

| Metric | Count |
|---|---|
| Candidate aggregates | 20 (18 after collapsing Order/OrderOther and Trip/Trip2 duplicates) |
| Events discovered in source | **0** |
| Events proposed (INVENTED) | ~30 (Step 3) |
| References between aggregates | ~25 (Step 4) |
| Intra-invariants proposed | ~55 across 18 aggregates (Step 5) |
| ⚠ Gaps flagged | 29 (see below) |

## Consolidated ⚠ Gaps

### 01-aggregates.md
1. **Order / OrderOther duplicate** — collapse into one aggregate or keep partition? (Order section)
2. **Trip / Trip2 duplicate** — same question as above. (Trip2 section)
3. **FoodOrder root ambiguity** — standalone aggregate vs child of Order. (FoodOrder section)
4. **Payment vs InsidePayment.Payment overlap** — two `Payment` entities; collapse or keep distinct? (Payment, InsidePayment sections)
5. **FoodOrder vs Delivery overlap** — two aggregates modelling food fulfilment. (Delivery section)

### 02-fields.md
6. **Cross-aggregate name copies without events** — `Order.contactsName/document*`, `Order.from/to`, `Trip.*StationName`, `FoodOrder.foodName/price/stationName/storeName`, `Payment.price`, `InsidePayment.price`, `Delivery.foodName/storeName/stationName` are all copies with no refresh mechanism in source. Each needs a per-field decision: **snapshot** vs **projection** (and, if projection, an invented refresh event).

### 03-events.md
7. **No events exist in source** — all ~30 events in Step 3 are inventions. Every event/payload requires user validation.
8. **Order status machine unknown** — `status:int` is opaque; transition graph (paid → collected → used → refunded …) must be supplied by the user before lifecycle events can be finalised.
9. **Rename events depend on Gap #6.**
10. **Idempotency keys undefined** — if events are added, per-handler idempotency keys must be specified.

### 04-cross-aggregate.md
11. **All `onDelete` policies inferred** — none are actually enforced in Train-Ticket source. Every reference in Step 4 needs the user to pick `prevent` / `cascade` / `setNull`.
12. **Payment onDelete from Order / User** — audit-trail concerns vs DSL requirement.
13. **Consign cancellation policy on order cancel** — cascade vs prevent.
14. **FoodOrder.stationName / storeName nullability on store deletion** — setNull vs cascade.
15. **Name-based vs id-based references** — Train-Ticket uses unique-name cross-references (`trainNumber`, station names); the DSL reference model may prefer ids. Needs mapping decision.

### 05-invariants.md
16. **`Order.status` range speculative** — unknown status enum.
17. **Most invariants inferred, not enforced in source** — every rule needs confirmation.
18. **Regex formats illustrative** — phone, email, business hours.
19. **Route membership rule** (`stations.contains(startStation)`) — grammar-support for `.contains` on list fields unknown; may require refactor.
20. **`Balance` overdraft rule** — unclear if negative balances are permitted.
21. **`Order.fromAndToDiffer` location** — entity invariant vs creation-time precondition.
22. **`PriceConfig.firstClassHigherThanBasic`** — domain assumption, not enforced.

### 06-config.md
23. **Base package** placeholder — `com.trainticket` guessed.
24. **Database type** — source is MongoDB; check Nebula support, else translate.
25. **Sagas vs TCC vs both** — needs user decision.
26. **Project / package naming** — `train-ticket` vs `trainticket` vs `ts`.

### General
27. **Status-machine dump required** — tracing controller logic in `ts-order-service` for all `status`-mutating endpoints is a prerequisite for correct event modelling.
28. **~20 admin/orchestrator services skipped** — `ts-admin-*`, `ts-preserve*`, `ts-execute-service`, `ts-rebook-service`, `ts-cancel-service`, `ts-basic-service`, `ts-wait-order-service`. Their business logic becomes Nebula *functionalities* (cross-aggregate workflows) rather than aggregates. Not modelled here; needs a separate pass once aggregates are agreed.
29. **Authentication / authorisation** — `ts-auth-service`, `ts-security-service`, `ts-verification-code-service` were skipped as infra; decide whether to model auth inside the DSL project at all.

## Source-location ambiguities / assumptions made

- **Aggregate partitioning (Order/OrderOther, Trip/Trip2):** assumed both halves of the partition have identical schemas (they do, per source survey). Real-world traffic is split by `trainNumber` prefix — a routing concern, not a domain concern.
- **Event absence:** assumed there is genuinely no eventing layer; I did not exhaustively grep every service for RabbitMQ. If the user knows of one, it overrides Step 3.
- **FoodOrder as independent aggregate:** assumed it is separate from Order because Train-Ticket places it in its own service with its own repository; could also be modelled as a child.
- **`ts-inside-payment-service` contains two aggregates** (`Payment` + `Balance`) — assumed they are distinct because they have independent repositories.
- **Admin & orchestrator services deferred** to a later step (Phase-3 functionalities) rather than folding them into aggregates.

## Recommended next steps (in priority order)

1. **Resolve duplicate aggregates** (Gaps #1, #2) — biggest simplification.
2. **Dump the Order status machine** (Gap #8) — unlocks event design.
3. **Decide snapshot vs projection for every cross-copied field** (Gap #6) — determines whether rename/update events exist at all.
4. **Pick `onDelete` policies** for every reference in `04-cross-aggregate.md`.
5. **Confirm database target** (Gap #24) — may require denormalisation planning for list-valued fields on `Route` / `Trip`.
6. **Confirm consistency model(s)** — sagas / tcc / both (Gap #25).
7. Only *after* the above, start drafting `.nebula` files (recommended order: Station → TrainType → User → Route → Contacts → Trip → PriceConfig → Order → Payment / InsidePayment / Balance → Assurance → Consign → ConsignPrice → Food family → Delivery → Config).
