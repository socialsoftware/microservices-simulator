# 05 — Intra-Aggregate Invariants (Train-Ticket)

Rules expressible with only root-entity fields (no cross-aggregate reads). Train-Ticket has very little explicit validation in its Java sources — most of what follows is **inferred from field semantics** and labelled with a comment when so. Flag as **⚠ Gap** where the rule is speculative.

## Order
```
check statusWithinRange { status >= 0 && status <= 6 }
    error "Invalid order status"
check priceNonNegative { Double.parseDouble(price) >= 0 }
    error "Price cannot be negative"
check seatsConsistent { (coachNumber > 0 && seatNumber != null) || (coachNumber == 0 && seatNumber == null) }
    error "Seat and coach must be set together"
check fromAndToDiffer { !from.equals(to) }
    error "Origin and destination cannot be the same"
check travelTimeAfterBought { travelDate >= boughtDate }
    error "Travel date must be on or after booking date"
```
**⚠ Gap** — exact status-range upper bound is speculative; Train-Ticket encodes statuses as bare ints with no enum. The `fromAndToDiffer` and `travelTimeAfterBought` rules are not enforced in source but are domain-reasonable.

## Trip
```
check startBeforeEnd { startingTime < endTime }
    error "Starting time must be before end time"
check startAndTerminalDiffer { !startingStationName.equals(terminalStationName) }
    error "Starting and terminal stations must differ"
check hasIntermediateOrDirect { stationsName.size() >= 2 }
    error "Trip must include at least starting and terminal stations"
```
**⚠ Gap** — not enforced in source; inferred.

## TrainType
```
check capacityPositive { economyClass >= 0 && confortClass >= 0 }
    error "Capacity must be non-negative"
check nameNotBlank { name != null && !name.isBlank() }
    error "Train type name is required"
check speedPositive { averageSpeed > 0 }
    error "Average speed must be positive"
```

## Station
```
check nameNotBlank { name != null && !name.isBlank() }
    error "Station name is required"
check stayTimeNonNegative { stayTime >= 0 }
    error "Stay time cannot be negative"
```

## Route
```
check stationsAndDistancesConsistent { distances.size() == stations.size() - 1 }
    error "Distances list must have one fewer entry than stations"
check atLeastTwoStations { stations.size() >= 2 }
    error "A route needs at least start and end stations"
check endpointsInStations { stations.contains(startStation) && stations.contains(endStation) }
    error "Start and end stations must appear in the station list"
check distancesPositive { distances.stream().allMatch(d -> d > 0) }
    error "Distances must be positive"
```

## Contacts
```
check nameNotBlank { name != null && !name.isBlank() }
    error "Contact name is required"
check documentNumberNotBlank { documentNumber != null && !documentNumber.isBlank() }
    error "Document number is required"
check phoneNumberFormat { phoneNumber.matches("\\d{7,15}") }
    error "Phone number must be 7–15 digits"
```
**⚠ Gap** — phone regex is illustrative.

## User
```
check userNameNotBlank { userName != null && !userName.isBlank() }
    error "Username is required"
check passwordNotBlank { password != null && !password.isBlank() }
    error "Password is required"
check emailFormat { email.matches("^[^@]+@[^@]+\\.[^@]+$") }
    error "Invalid email"
check genderInRange { gender >= 0 && gender <= 2 }
    error "Invalid gender code"
```

## PriceConfig
```
check ratesPositive { basicPriceRate > 0 && firstClassPriceRate > 0 }
    error "Price rates must be positive"
check firstClassHigherThanBasic { firstClassPriceRate >= basicPriceRate }
    error "First-class rate must be at least as high as basic"
```
**⚠ Gap** — second rule is a domain assumption.

## FoodOrder
```
check foodTypeValid { foodType == 1 || foodType == 2 }
    error "foodType must be 1 (train) or 2 (station)"
check stationFieldsWhenStationFood { foodType != 2 || (stationName != null && storeName != null) }
    error "Station food requires stationName and storeName"
check priceNonNegative { price >= 0 }
    error "Price cannot be negative"
```

## StationFoodStore
```
check storeNameNotBlank { storeName != null && !storeName.isBlank() }
    error "Store name required"
check businessHoursFormat { businessHours.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}") }
    error "Business hours must be HH:MM-HH:MM"
```
**⚠ Gap** — regex speculative.

## TrainFood
```
check priceNonNegative { price >= 0 }
    error "Price cannot be negative"
check foodNameNotBlank { foodName != null && !foodName.isBlank() }
    error "Food name required"
```

## Assurance
```
check typeIsKnown { type == AssuranceType.TRAFFIC_ACCIDENT || type == AssuranceType.DELAYED_MONEY }
    error "Unknown assurance type"
```

## Consign
```
check weightPositive { weight > 0 }
    error "Weight must be positive"
check fromAndToDiffer { !from.equals(to) }
    error "Origin and destination must differ"
check phoneNotBlank { phone != null && !phone.isBlank() }
    error "Phone is required"
check targetAfterHandle { targetDate >= handleDate }
    error "Target date must be on or after handle date"
```

## ConsignPrice
```
check weightPositive { initialWeight > 0 }
    error "Initial weight must be positive"
check pricesNonNegative { withinPrice >= 0 && outPrice >= 0 }
    error "Prices must be non-negative"
```

## Payment / InsidePayment
```
check priceNonNegative { Double.parseDouble(price) >= 0 }
    error "Price cannot be negative"
```

## Balance
```
check balanceNonNegative { balance >= 0 }
    error "Balance cannot go negative"
```
**⚠ Gap** — depends on whether overdraft is permitted; Train-Ticket source doesn't say.

## Config
```
check nameNotBlank { name != null && !name.isBlank() }
    error "Config name required"
```

## Delivery
```
check orderIdNotNull { orderId != null }
    error "orderId required"
```

---

## ⚠ Gaps in this step
- Most invariants above are **inferred**; Train-Ticket has no explicit validation layer. User must confirm each before codifying.
- `Order.status` range and semantics are unclear — the entire order state machine needs to be documented before we can write lifecycle invariants.
- Several regex/format rules (phone, email, business hours) are illustrative.
- The rule `stations.contains(startStation)` on Route uses a business-key list — grammar may or may not allow `.contains` on a list field; may require refactor into a computed-field or pre-validation.
- The rule `Travel.fromAndToDiffer` on Order could instead be enforced at order-creation as a pre-condition rather than an invariant on the entity state.
