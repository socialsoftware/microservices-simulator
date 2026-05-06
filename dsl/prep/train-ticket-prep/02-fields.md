# 02 — Field Classification (Train-Ticket)

Classification:
- **Owned** — aggregate is source of truth
- **Projected from X** — cached copy of data owned by aggregate X
- **Reference to X** — foreign id, not a data copy

Train-Ticket has **no event-based projections** in its source code (see `03-events.md`). Fields that name other aggregates are almost always **References** — but in several cases a *copied name* (e.g. `contactsName`, `startingStationName`) is present alongside an id-like field. Those copies are de-facto projections and are flagged as such with a **⚠ Gap** noting that Train-Ticket refreshes them by re-querying REST endpoints, not by subscribing to events.

## Order
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | aggregate id |
| boughtDate | String | Owned | |
| travelDate | String | Owned | |
| travelTime | String | Owned | |
| accountId | String | Reference to User | |
| contactsName | String | Projected from Contacts | **⚠ Gap** — copied from Contacts but no event; refresh story TBD |
| documentType | int | Projected from Contacts | same as above |
| contactsDocumentNumber | String | Projected from Contacts | same as above |
| trainNumber | String | Reference to TrainType | matches `TrainType.name` |
| coachNumber | int | Owned | |
| seatClass | int | Owned | |
| seatNumber | String | Owned | |
| from | String | Projected from Station | station name copy |
| to | String | Projected from Station | station name copy |
| status | int | Owned | lifecycle state |
| price | String | Owned | computed from PriceConfig at creation time |

## Trip
| Field | Type | Classification | Notes |
|---|---|---|---|
| tripId | String | Owned | |
| trainNumber | String | Reference to TrainType | matches `TrainType.name` |
| routeId | String | Reference to Route | |
| startingStationName | String | Projected from Station | name copy |
| stationsName | List&lt;String&gt; | Projected from Station | name copies along the route |
| terminalStationName | String | Projected from Station | name copy |
| startingTime | String | Owned | |
| endTime | String | Owned | |

## TrainType
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| name | String | Owned | unique business key |
| economyClass | int | Owned | capacity |
| confortClass | int | Owned | capacity (typo preserved from source) |
| averageSpeed | int | Owned | |

## Station
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| name | String | Owned | unique |
| stayTime | int | Owned | |

## Route
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| stations | List&lt;String&gt; | Reference to Station (list) | station ids |
| distances | List&lt;int&gt; | Owned | parallel to `stations` |
| startStation | String | Reference to Station | |
| endStation | String | Reference to Station | |

## Contacts
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| accountId | String | Reference to User | |
| name | String | Owned | |
| documentType | int | Owned | |
| documentNumber | String | Owned | |
| phoneNumber | String | Owned | |

## User
| Field | Type | Classification | Notes |
|---|---|---|---|
| userId | String | Owned | |
| userName | String | Owned | unique |
| password | String | Owned | hashed |
| gender | int | Owned | |
| documentType | int | Owned | |
| documentNum | String | Owned | |
| email | String | Owned | |

## PriceConfig
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| trainType | String | Reference to TrainType | matches `TrainType.name` |
| routeId | String | Reference to Route | |
| basicPriceRate | double | Owned | |
| firstClassPriceRate | double | Owned | |

## FoodOrder
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| orderId | String | Reference to Order | |
| foodType | int | Owned | 1=train, 2=store |
| stationName | String | Projected from Station | present only if foodType=2 |
| storeName | String | Projected from StationFoodStore | present only if foodType=2 |
| foodName | String | Projected from TrainFood or StationFoodStore | **⚠ Gap** — copy from whichever catalog applies |
| price | double | Projected at purchase | **⚠ Gap** — snapshot or live? Likely snapshot |

## StationFoodStore
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| stationName | String | Reference to Station | by-name reference |
| storeName | String | Owned | |
| telephone | String | Owned | |
| businessHours | String | Owned | |

## TrainFood
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| trainNumber | String | Reference to TrainType | |
| foodName | String | Owned | |
| price | double | Owned | |

## Assurance
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| orderId | String | Reference to Order | |
| type | enum | Owned | TRAFFIC_ACCIDENT / DELAYED_MONEY |

## Consign
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| orderId | String | Reference to Order | |
| accountId | String | Reference to User | |
| handleDate | String | Owned | |
| targetDate | String | Owned | |
| from | String | Projected from Station | |
| to | String | Projected from Station | |
| consignee | String | Owned | |
| phone | String | Owned | |
| weight | double | Owned | |
| isWithin | boolean | Owned | within-city flag (pricing input) |

## ConsignPrice
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| initialWeight | double | Owned | |
| withinPrice | double | Owned | |
| outPrice | double | Owned | |

## Payment (outside)
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| orderId | String | Reference to Order | |
| userId | String | Reference to User | |
| price | String | Projected from Order | **⚠ Gap** — snapshot at pay time |

## InsidePayment
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| orderId | String | Reference to Order | |
| userId | String | Reference to User | |
| price | String | Projected from Order | snapshot |
| type | enum | Owned | INSIDE / OUTSIDE |

## Balance
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| accountId | String | Reference to User | unique |
| balance | double | Owned | |

## Config
| Field | Type | Classification | Notes |
|---|---|---|---|
| name | String | Owned | aggregate id (primary key) |
| value | String | Owned | |
| description | String | Owned | |

## Delivery
| Field | Type | Classification | Notes |
|---|---|---|---|
| id | String | Owned | |
| orderId | UUID | Reference to Order | |
| foodName | String | Projected from TrainFood/StationFoodStore | snapshot |
| storeName | String | Projected from StationFoodStore | snapshot |
| stationName | String | Projected from Station | snapshot |

---

## ⚠ Gaps in this step
- Every "Projected from X" field above is populated by REST round-trip, not event subscription. Without events these are really **cached at write time** values. For DSL modelling decide per field: model as `projection` (and invent an event in Step 3) or as `owned snapshot` (copy at creation, never refreshed).
- `Order.contactsName / documentType / contactsDocumentNumber` — snapshot vs live is a domain decision (changing a contact after purchase: does the order reflect it?).
- `FoodOrder.foodName`, `FoodOrder.price` — likely snapshot at purchase; confirm.
- `Payment.price`, `InsidePayment.price` — almost certainly snapshot.
