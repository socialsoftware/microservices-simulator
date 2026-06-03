# 02 — Field Classification (train-ticket, booking core)

Classification key: **Owned** (this aggregate is source of truth) · **Projected from X**
(cached copy of X's data) · **Reference to X** (foreign id/name, not a data copy).

> **train-ticket links by *name*, not id, in several places** (station `name`, train-type
> `name`). The existing `order.nebula` already projects those names. References below note
> whether the link is by id or by name.

DB-level surrogate `id` (UUID) fields are omitted — Nebula supplies `aggregateId`.

## User
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| userId | String | Owned | business identity (UUID); the "accountId" everyone else references |
| userName | String | Owned | |
| password | String | Owned | |
| gender | Integer | Owned | |
| documentType | Integer | Owned | code into DocumentType enum |
| documentNum | String | Owned | |
| email | String | Owned | |

## Contacts
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| accountId | String | Reference to **User** (`User.userId`) | owning account |
| name | String | Owned | |
| documentType | Integer | Owned | DocumentType code |
| documentNumber | String | Owned | |
| phoneNumber | String | Owned | |

Already modelled in `contacts.nebula` (projects `User.userId as accountId`). ⚠ existing file
treats `user` as a projection entity; source stores only the raw `accountId` string — either
is fine, just be consistent (see `04`).

## Station
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| name | String | Owned | unique; normalised lowercase/space-stripped |
| stayTime | Integer | Owned | dwell time |

## TrainType
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| name | String | Owned | unique; the "trainNumber" others reference by name |
| economyClass | Integer | Owned | second-class seat capacity |
| confortClass | Integer | Owned | first-class seat capacity (source spelling "confort") |
| averageSpeed | Integer | Owned | |

## Route
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| stations | List\<String\> | Reference to **Station** (by `name`) | ⚠ ordered list of station *names*; soft reference, no FK |
| distances | List\<Integer\> | Owned | parallel to `stations`; cumulative distances |
| startStation | String | Reference to **Station** (by name) | == stations.first |
| endStation | String | Reference to **Station** (by name) | == stations.last |

## Trip
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| tripId | TripId (embedded) | Owned | value object: `type` (enum) + `number`; from trainNumber |
| trainTypeName | String | Reference to **TrainType** (by `name`) | |
| routeId | String | Reference to **Route** (by id) | |
| startStationName | String | Reference to **Station** (by name) | |
| stationsName | String | Reference to **Station** (by name) | intermediate stops, comma-joined |
| terminalStationName | String | Reference to **Station** (by name) | |
| startTime | String | Owned | |
| endTime | String | Owned | |

## PriceConfig
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| trainType | String | Reference to **TrainType** (by `name`) | unique with routeId |
| routeId | String | Reference to **Route** (by id) | unique with trainType |
| basicPriceRate | Double | Owned | |
| firstClassPriceRate | Double | Owned | |

## Order
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| boughtDate | String | Owned | |
| travelDate | String | Owned | |
| travelTime | String | Owned | copied from Trip.startTime at creation |
| accountId | String | Reference to **User** (`userId`) | buyer |
| contactsName | String | Projected from **Contacts** (`name`) | snapshot at booking |
| documentType | Integer | Projected from **Contacts** (`documentType`) | |
| contactsDocumentNumber | String | Projected from **Contacts** (`documentNumber`) | |
| trainNumber | String | Reference to **TrainType** (by `name`) | also implies Trip |
| coachNumber | Integer | Owned | seat allocation result |
| seatClass | Integer | Owned | SeatClass code (chosen at booking) |
| seatNumber | String | Owned | seat allocation result |
| from | String | Projected from **Station** (`name`) | mapped `fromName` in order.nebula |
| to | String | Projected from **Station** (`name`) | mapped `toName` in order.nebula |
| status | Integer | Owned | OrderStatus enum code |
| price | String | Owned | computed from PriceConfig at booking; stored as String |

⚠ **Gap — projection vs reference for Order:** the existing `order.nebula` models
`user/contacts/train/fromStation/toStation` as **projection entities** (`Entity X from Y`).
That is reasonable: contact name/document and station/train *names* are snapshotted into the
order at booking time and shouldn't change retroactively. Confirm whether `from`/`to`/
`trainNumber` should be **frozen snapshots** (projections — recommended) or **live
references** (re-resolved). Recommended: keep as projections (booking-time snapshot).
