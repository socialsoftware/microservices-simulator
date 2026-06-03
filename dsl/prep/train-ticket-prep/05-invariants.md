# 05 — Intra-Invariants (train-ticket, booking core)

Rules expressible using **only the root entity's own fields**. Cross-aggregate rules live in
`04`.

> **train-ticket is invariant-poor.** The JPA entities carry almost no business validation —
> mostly `@NotNull`, `@Column(unique=...)` and unique `@Index`. The checks below are grounded
> in those annotations + obvious domain constraints. Anything not directly backed by source
> is marked ⚠ (inferred). **Uniqueness constraints are NOT single-root booleans** → all
> flagged ⚠ Gap (need repository-level / Sagas handling, not a `check`).

## User
```
check userNameNotBlank { userName != null && userName.length() > 0 }   # inferred ⚠
    error "User name cannot be blank"
check passwordNotBlank { password != null && password.length() > 0 }   # inferred ⚠
    error "Password cannot be blank"
```
- ⚠ No explicit validation in source `User`; above are reasonable defaults.

## Contacts
```
check nameNotBlank { name != null && name.length() > 0 }               # inferred ⚠
    error "Contact name cannot be blank"
check documentNumberNotBlank { documentNumber != null && documentNumber.length() > 0 }  # inferred ⚠
    error "Document number cannot be blank"
check documentTypeValid { documentType >= 0 && documentType <= 3 }     # from DocumentType enum (0..3)
    error "Invalid document type"
```
- ⚠ **Gap (uniqueness):** unique index on (`accountId`,`documentNumber`,`documentType`) —
  cross-instance, not a single-root boolean. Handle via repository lookup in create/update.

## Station
```
check nameNotBlank { name != null && name.length() > 0 }               # @NotNull on name
    error "Station name cannot be blank"
check stayTimeNonNegative { stayTime >= 0 }                            # inferred ⚠
    error "Station stay time cannot be negative"
```
- ⚠ **Gap (uniqueness):** `name` is `@Column(unique=true)` — handle at repository level.

## TrainType
```
check nameNotBlank { name != null && name.length() > 0 }               # @NotNull on name
    error "Train type name cannot be blank"
check economyClassNonNegative { economyClass >= 0 }                    # capacity, inferred ⚠
    error "Economy-class capacity cannot be negative"
check confortClassNonNegative { confortClass >= 0 }                    # capacity, inferred ⚠
    error "First-class capacity cannot be negative"
check averageSpeedNonNegative { averageSpeed >= 0 }                    # inferred ⚠
    error "Average speed cannot be negative"
```
- ⚠ **Gap (uniqueness):** `name` unique — repository level.

## Route
```
check startStationSet { startStation != null && startStation.length() > 0 }  # inferred ⚠
    error "Route must have a start station"
check endStationSet { endStation != null && endStation.length() > 0 }        # inferred ⚠
    error "Route must have an end station"
```
- ⚠ **Gap:** "`stations` and `distances` lists are aligned / non-empty" and
  "`startStation == stations.first`, `endStation == stations.last`" involve list indexing
  and may exceed simple boolean support — verify against grammar (`.size()`, indexing). If
  unsupported, enforce in service code.

## Trip
```
check trainTypeNameSet { trainTypeName != null && trainTypeName.length() > 0 }  # @NotNull
    error "Trip must have a train type"
check startStationSet { startStationName != null && startStationName.length() > 0 }  # @NotNull
    error "Trip must have a start station"
check terminalStationSet { terminalStationName != null && terminalStationName.length() > 0 }  # @NotNull
    error "Trip must have a terminal station"
check startTimeSet { startTime != null && startTime.length() > 0 }     # @NotNull
    error "Trip must have a start time"
check endTimeSet { endTime != null && endTime.length() > 0 }           # @NotNull
    error "Trip must have an end time"
```
- ⚠ `tripId` is an embedded value (`type` + `number`); model as embeddable or two String
  fields. Verify embedded-value support in grammar.

## PriceConfig
```
check basicPriceRateNonNegative { basicPriceRate >= 0 }                # inferred ⚠
    error "Basic price rate cannot be negative"
check firstClassPriceRateNonNegative { firstClassPriceRate >= 0 }      # inferred ⚠
    error "First-class price rate cannot be negative"
```
- ⚠ **Gap (uniqueness):** unique (`trainType`,`routeId`) — repository level.

## Order
```
check seatClassValid { seatClass >= 0 }                                # SeatClass code, inferred ⚠
    error "Invalid seat class"
check coachNumberPositive { coachNumber > 0 }                          # inferred ⚠
    error "Coach number must be positive"
check statusValid { status >= 0 && status <= 6 }                       # OrderStatus enum 0..6
    error "Invalid order status"
check priceSet { price != null && price.length() > 0 }                 # inferred ⚠
    error "Order must have a price"
```
- ⚠ `status`/`seatClass` are stored as `int` codes; if modelled as the `OrderStatus`/
  `SeatClass` enums (as `shared-enums.nebula` already does for OrderStatus), replace the
  numeric range checks with `status != null`.
- ⚠ **Gap (cross-aggregate, NOT here):** "seat available before booking" needs Trip capacity
  + sold-order counts → see `04` seat-availability gap.

---

## ⚠ Gaps summary (this file)
- All **uniqueness** constraints (Station.name, TrainType.name, Contacts triple,
  PriceConfig pair) are cross-instance → not `check` booleans; need repository/Sagas guards.
- Most "not blank / non-negative" checks are **inferred** — source entities lack explicit
  validation; confirm desired strictness.
- Route list-alignment and embedded `TripId` may exceed simple-boolean grammar support —
  verify, else push to service code.
- Numeric-code vs enum modelling (status, seatClass, documentType) affects which checks apply.
