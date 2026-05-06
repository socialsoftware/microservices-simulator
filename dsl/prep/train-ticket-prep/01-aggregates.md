# 01 — Aggregates & Roots (Train-Ticket)

Source: `/home/tomas/Tese/train-ticket/` — one Spring Boot microservice per aggregate. Each entity with `@Id` is treated as a candidate root.

Services without domain entities (pure orchestrators / BFFs) are listed at the end and **excluded** from the aggregate set: `ts-voucher-service`, `ts-food-delivery-service`, `ts-seat-service`, `ts-ticket-office-service`, `ts-preserve-service`, `ts-preserve-other-service`, `ts-execute-service`, `ts-rebook-service`, `ts-cancel-service`, `ts-basic-service`, `ts-wait-order-service`, `ts-admin-*`, `ts-gateway-service`, `ts-ui-dashboard`, `ts-auth-service`, `ts-security-service`, `ts-verification-code-service`, `ts-news-service`, `ts-notification-service`, `ts-avatar-service`.

## Order  (ts-order-service)
- **Root:** `Order` (`@Id id:String`). Owns its lifecycle (`status`) and is the aggregate that every payment / cancel / rebook flow mutates.
- **Child/value objects:** `OrderAlterInfo` (snapshot of a prior order kept for rebook audit); `OrderInfo` is a query DTO, not part of the aggregate.
- **Purpose:** Represents a purchased ticket with travel, contact, seat and pricing data.

## OrderOther  (ts-order-other-service)
- **Root:** `Order` (same schema as Order aggregate above).
- **Purpose:** A second partition of Order used for sharding/legacy storage.
- **⚠ Gap — duplicate aggregate.** `ts-order-other-service` exposes the same `Order` entity as `ts-order-service` with a different URL prefix. Options:
  1. Model as a single `Order` aggregate and drop the partition distinction (recommended unless partitioning is semantically meaningful).
  2. Model as `Order` + `OrderOther` with an explicit discriminator field.
  Recommend option 1; confirm with user.

## Trip  (ts-travel-service)
- **Root:** `Trip` (`@Id tripId:String`) — trip identity drives the service. `Travel` and `TripAllDetail` are request/response DTOs composing Trip with left-tickets/route info, not part of the aggregate.
- **Purpose:** A scheduled train run: route, train-number, stations, start/end times.

## Trip2  (ts-travel2-service)
- **⚠ Gap — duplicate aggregate.** Identical `Trip` entity as `ts-travel-service` under a different URL prefix (classic TrainTicket "two travel partitions" pattern: certain `trainNumber` prefixes route to travel vs travel2). Same two options as Order/OrderOther. Recommend collapsing into a single `Trip` aggregate.

## TrainType  (ts-train-service)
- **Root:** `TrainType` (`@Id id:String`). `name` is unique.
- **Purpose:** Catalog of train types with capacity (economy / comfort class seats) and average speed.

## Station  (ts-station-service)
- **Root:** `Station` (`@Id id:String`). `name` is unique.
- **Purpose:** Station master data with dwell time.

## Route  (ts-route-service)
- **Root:** `Route` (`@Id id:String`). `RouteInfo` is a DTO.
- **Purpose:** Ordered station sequence and inter-station distances.

## Contacts  (ts-contacts-service)
- **Root:** `Contacts` (`@Id id:String`) — a passenger's travel document bound to an account.
- **Purpose:** Named passenger records reusable across orders.

## User  (ts-user-service)
- **Root:** `User` (`@Id userId:String`).
- **Purpose:** Application user / account.

## PriceConfig  (ts-price-service)
- **Root:** `PriceConfig` (`@Id id:String`). Keyed by `(trainType, routeId)` business-uniqueness.
- **Purpose:** Per-train-type-per-route basic + first-class multipliers.

## FoodOrder  (ts-food-service)
- **Root:** `FoodOrder` (`@Id id:String`) — a food line-item attached to an Order.
- **Purpose:** A food purchase (train-food or station-food) tied to an order.
- **⚠ Gap — root choice.** `FoodOrder` here is closer to a child of `Order`. If the DSL supports aggregate composition via reference we can keep it separate; otherwise consider modelling as `Order.foodItems`.

## StationFoodStore  (ts-station-food-service)
- **Root:** `StationFoodStore` (`@Id id:String`).
- **Purpose:** Food store on a station with business hours.

## TrainFood  (ts-train-food-service)
- **Root:** `TrainFood` (`@Id id:String`).
- **Purpose:** Menu item available on a specific train number.

## Assurance  (ts-assurance-service)
- **Root:** `Assurance` (`@Id id:String`).
- **Purpose:** Optional insurance attached to an order (enum type: TRAFFIC_ACCIDENT / DELAYED_MONEY).

## Consign  (ts-consign-service)
- **Root:** `Consign` (`@Id id:String`). `ConsignRecord` tracks status history — likely a child entity within this aggregate.
- **Purpose:** Luggage shipment tied to an order.

## ConsignPrice  (ts-consign-price-service)
- **Root:** `PriceInfo` (`@Id id:String`) — effectively a singleton pricing configuration.
- **Purpose:** Weight-based consign pricing tiers.

## Payment  (ts-payment-service)
- **Root:** `Payment` (`@Id id:String`).
- **Purpose:** External/outside payment record for an order.
- **⚠ Gap — overlap with InsidePayment.** Two services model payments. Clarify below.

## InsidePayment  (ts-inside-payment-service)
- **Roots (two aggregates in one service):**
  1. `Payment` (`@Id id:String`) — inside (wallet) or outside payment record with type discriminator.
  2. `Balance` (`@Id id:String`) — wallet balance per account (`accountId` unique).
- **Purpose:** Wallet-backed payments and balance tracking.
- **⚠ Gap.** `Payment` here overlaps with `ts-payment-service.Payment`. Recommend: keep `InsidePayment` (wallet) and `Payment` (outside gateway) as distinct aggregates if both flows matter; otherwise collapse. Needs user decision.

## Config  (ts-config-service)
- **Root:** `Config` (`@Id name:String`).
- **Purpose:** Key/value runtime configuration read by other services (notably ts-seat-service).

## Delivery  (ts-delivery-service)
- **Root:** `Delivery` (`@Id id:String`; `orderId:UUID` is the business key).
- **Purpose:** Food-delivery dispatch record.
- **⚠ Gap — overlap with FoodOrder.** `Delivery`, `FoodOrder`, and the stateless `ts-food-delivery-service` together model one concept. Options: (a) keep `Delivery` as the fulfilment aggregate and `FoodOrder` as the purchase aggregate; (b) merge. Recommend (a); confirm.

---

## Aggregate count
**20 candidate aggregates** across 19 services (InsidePayment owns 2). After resolving duplicates (Order/OrderOther, Trip/Trip2) the normalised count is **18**.

## ⚠ Gaps in this step
- Order / OrderOther duplication (see Order section)
- Trip / Trip2 duplication (see Trip section)
- FoodOrder root ambiguity (see FoodOrder section)
- Payment vs InsidePayment.Payment overlap (see InsidePayment section)
- FoodOrder vs Delivery overlap (see Delivery section)
