# SUMMARY — train-ticket DSL prep (booking core)

**Source:** `../train-ticket/` — the FudanSELab train-ticket benchmark (44 microservices).
**Scope (user-selected):** booking core only — `ts-user`, `ts-contacts`, `ts-station`,
`ts-train`, `ts-route`, `ts-travel`(+`ts-travel2`), `ts-price`, `ts-order`(+`ts-order-other`).
**Source of truth:** JPA `@Entity` classes (no domain-model docs, no `.nebula` source).

## Counts
| Item | Count |
|------|-------|
| Aggregates | **8** — User, Contacts, Station, TrainType, Route, Trip, PriceConfig, Order |
| Non-aggregate flagged | 1 — Seat (computational service, no entity) |
| Inferred events | 13 (`*Updated`/`*Deleted` per aggregate) — **none exist in source** |
| Cross-aggregate references | 13 across 5 consumers (all `onDelete` policies introduced by Nebula) |
| Inter-invariants (suggested) | 11 |
| Intra-invariants (suggested) | ~24 (most inferred; source is invariant-poor) |

> The existing `dsl/abstractions/trainTicket/` already has `.nebula` stubs for 5 of the 8:
> User, Contacts, Station, TrainType, Order. Missing: **Route, Trip, PriceConfig** (plus the
> Events/References/invariants blocks on the existing five).

## ⚠ Gaps (consolidated)

### Structural
- **`01` Trip** — `ts-travel2-service` is a field-identical shard of `Trip`; model as one.
- **`01` Order** — `ts-order-other-service` is a field-identical shard of `Order`; model as one.
- **`01` Seat** — no persisted entity; seat availability/dispatch is a *computation*, not an
  aggregate. No `Seat` to scaffold.

### Events (`03`)
- **`03` g1** — **All events are inferred**; train-ticket has no event bus/sourcing. Validate
  the whole event set before wiring — don't over-model.
- **`03` g2 / `04`** — **Reference-by-name** (Station.name, TrainType.name): rename
  propagation has no source precedent. Decide: treat names as immutable (drop `*Updated`
  projections) or carry name in events / store aggregateIds (schema change).
- **`03` g3** — Decide if Order's contact/station/train fields are **frozen snapshots**
  (recommended → drop `*Updated` subscriptions, keep only `*Deleted` integrity) or live.

### Cross-aggregate (`04`)
- **`04`** — **Every `onDelete` policy is Nebula-introduced** (source enforces zero
  referential integrity). Confirm each: Contacts→User (cascade?), Order→User/Contacts/
  TrainType/Station (prevent?), Trip→TrainType/Route/Station (prevent?), PriceConfig→
  TrainType/Route (cascade?).
- **`04` / `05`** — **Seat availability & dispatch** (preserve steps 3–4): cross-aggregate
  computation over `Order` count vs `TrainType` capacity. No clean reference/invariant home;
  needs a Sagas guard in `createOrder`. Resolve before implementing the booking functionality.

### Invariants (`05`)
- **`05`** — All **uniqueness** constraints (Station.name, TrainType.name, Contacts triple,
  PriceConfig pair) are cross-instance → not `check` booleans; need repository/Sagas guards.
- **`05`** — Route list-alignment (`stations`/`distances`) and embedded `TripId` may exceed
  simple-boolean grammar; verify or push to service code.
- **`05`** — Numeric-code vs enum modelling (status, seatClass, documentType) changes which
  checks apply.

### Config (`06`)
- **`06`** — `consistencyModels` is `["sagas"]`; confirm no TCC needed (seat hold/confirm).
- **`06`** — DB/package intentionally diverge from source (postgres + single base package);
  not a faithful DB port — confirmed acceptable.

## Next steps (suggested order)
1. **Resolve the snapshot-freeze decision (`03` g3)** — it determines half the Order event
   wiring. Recommended: freeze → keep only `*Deleted` integrity subscriptions.
2. **Pin the reference-by-name strategy (`03` g2 / `04`)** — affects Route/Trip/PriceConfig/
   Order events and inter-invariants globally. Recommended: treat Station/TrainType names as
   immutable.
3. **Decide seat availability handling (`04`/`05`)** — design the `createOrder` Sagas guard
   before scaffolding the booking functionality.
4. **Confirm `onDelete` policies (`04`)** table-by-table.
5. Then scaffold the 3 missing aggregates (**Route, Trip, PriceConfig**) and backfill
   Events/References/invariants on the existing 5.

## Assumptions made
- Booking-core scope per user selection; security/payment/assurance/food/consign excluded.
- Treated travel2/order-other as shard duplicates (verified field-identical).
- Treated Seat as non-aggregate (verified no `@Entity`).
- Reused the existing `nebula.config.json` rather than drafting a new one.
