# Methods and Custom Endpoints

This chapter covers the `Methods` block: defining custom business logic with action bodies, exposing methods as HTTP endpoints, publishing events, querying repositories, and working with collection elements.

> **Tied example:** [`showcase`](../../abstractions/showcase/): User, Room, and Booking aggregates with custom methods.

## The Methods Block

Every aggregate gets CRUD operations (create, read, update, delete) automatically. The `Methods` block adds custom business logic on top:

```nebula
Methods {
    signUp(String username, String email) {
        action { ... }
    }

    @PostMapping("/users/{userId}/loyalty")
    awardLoyaltyPoints(Integer userId, Integer points) {
        action { ... }
    }

    getActiveUsers() {
        query findActiveUserIds()
    }
}
```

Methods can have three kinds of bodies:
- **`action { ... }`** — imperative statements (create, load, assign, find)
- **`query repoMethod(args)`** — delegates to a repository method
- No body — generates a stub

## Action Bodies

Action bodies contain imperative statements that describe what the method does.

### `create` — Create a New Aggregate

```nebula
signUp(String username, String email) {
    action {
        create User {
            username: username,
            email: email,
            loyaltyPoints: 0,
            tier: BRONZE,
            active: true
        }
    }
}
```

Creates a new aggregate instance with the given field values. The generator emits:
- A DTO populated from the field assignments
- An `aggregateIdGeneratorService.getNewAggregateId()` call
- A factory `createUser(id, dto)` call
- A `unitOfWorkService.registerChanged(user, unitOfWork)` call

The method automatically returns a DTO of the created aggregate.

### `load` — Load an Existing Aggregate

```nebula
confirmBooking(Integer bookingId) {
    action {
        load Booking(bookingId) as booking
        booking.confirmed = true
    }
}
```

Loads an aggregate by ID using copy-on-write semantics:
1. `aggregateLoadAndRegisterRead(bookingId, unitOfWork)` — reads the current version
2. `createBookingFromExisting(old)` — creates a mutable copy
3. Assigns `booking.confirmed = true` via setter
4. `registerChanged(booking, unitOfWork)` — registers the modified copy

### `find` — Find an Element in a Collection

```nebula
renameAmenity(Integer roomId, Integer amenityCode, String newName) {
    action {
        load Room(roomId) as room
        find room.amenities where code == amenityCode as amenity
        amenity.name = newName
    }
}
```

Finds a single element in a collection property by matching a field value. Generates:

```java
var amenity = room.getAmenities().stream()
    .filter(el -> el.getCode() != null && el.getCode().equals(amenityCode))
    .findFirst()
    .orElseThrow(() -> new ShowcaseException("Element not found in collection"));
amenity.setName(newName);
unitOfWorkService.registerChanged(room, unitOfWork);
```

Modifications to the found element automatically register the parent aggregate as changed.

### Assignments

Assign values to loaded or found aliases:

```nebula
room.status = RESERVED          // Enum value (auto-resolved)
booking.confirmed = true        // Boolean literal
user.loyaltyPoints = points     // Method parameter
amenity.name = newName          // Method parameter
```

Enum values (like `RESERVED`, `OCCUPIED`, `AVAILABLE`) are resolved from the field's type — no need to qualify them.

## Preconditions

Validate inputs before executing the action body:

```nebula
awardLoyaltyPoints(Integer userId, Integer points) {
    precondition {
        check points > 0 error "Points awarded must be positive"
    }
    action {
        load User(userId) as user
        user.loyaltyPoints = points
    }
}
```

Preconditions generate `if` guards that throw a project-specific exception before any domain logic runs:

```java
if (!(points > 0)) {
    throw new ShowcaseException("Points awarded must be positive");
}
```

## Publishing Events

Methods can publish events using `publishes` after the action body:

```nebula
reserve(Integer roomId) {
    action {
        load Room(roomId) as room
        room.status = RESERVED
    }
    publishes RoomReservedEvent {
        roomNumber: room.roomNumber
    }
}
```

The event class is auto-generated with field types inferred from the assignment expressions. Events are registered on the Unit of Work and dispatched only on commit.

Multiple events can be published from a single method:

```nebula
bookRoom(BookingUser user, BookingRoom room, ...) {
    action {
        create Booking { ... }
    }
    publishes BookingCreatedEvent {
        userAggregateId: user.userAggregateId,
        roomAggregateId: room.roomAggregateId,
        totalPrice: price
    }
}
```

## Query Methods

Delegate to a repository method for read-only operations:

```nebula
getActiveUsers() {
    query findActiveUserIds()
}
```

This generates a service method that calls the repository, loads each aggregate by ID, and returns a list of DTOs:

```java
public java.util.List<UserDto> getActiveUsers(UnitOfWork unitOfWork) {
    Set<Integer> aggregateIds = userRepository.findActiveUserIds();
    return aggregateIds.stream()
        .map(id -> unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
        .map(user -> userFactory.createUserDto((User) user))
        .collect(Collectors.toList());
}
```

The repository method must be declared in the `Repository` block:

```nebula
Repository {
    @Query("select u.aggregateId from User u where u.state != 'DELETED' and u.active = true")
    Set<Integer> findActiveUserIds()
}
```

## HTTP Endpoints with `@PostMapping`

Add `@PostMapping` to expose a method as an HTTP endpoint:

```nebula
@PostMapping("/rooms/{roomId}/reserve")
reserve(Integer roomId) {
    action { ... }
}
```

This generates:
- A controller method on the aggregate's `@RestController`
- A functionalities method that creates a saga unit of work and delegates to the service
- A saga functionality class that wraps the call in a `SagaStep`

Path variables (`{roomId}`) are mapped to method parameters by name. Request parameters use `@RequestParam`:

```nebula
@PostMapping("/users/signup")
signUp(String username, String email) { ... }
// Generates: POST /users/signup?username=...&email=...
```

Methods **without** `@PostMapping` are internal — callable from workflows and other aggregates but not exposed as HTTP endpoints.

## Complete Example: Showcase Methods

### User — create, update, and query

```nebula
Methods {
    @PostMapping("/users/signup")
    signUp(String username, String email) {
        action {
            create User {
                username: username, email: email,
                loyaltyPoints: 0, tier: BRONZE, active: true
            }
        }
    }

    @PostMapping("/users/{userId}/loyalty")
    awardLoyaltyPoints(Integer userId, Integer points) {
        precondition {
            check points > 0 error "Points awarded must be positive"
        }
        action {
            load User(userId) as user
            user.loyaltyPoints = points
        }
        publishes UserLoyaltyAwardedEvent {
            userAggregateId: user.aggregateId,
            pointsAwarded: points
        }
    }

    getActiveUsers() {
        query findActiveUserIds()
    }
}
```

### Room — state transitions and collection operations

```nebula
Methods {
    @PostMapping("/rooms/{roomId}/reserve")
    reserve(Integer roomId) {
        action {
            load Room(roomId) as room
            room.status = RESERVED
        }
        publishes RoomReservedEvent { roomNumber: room.roomNumber }
    }

    @PostMapping("/rooms/{roomId}/amenities/{amenityCode}/rename")
    renameAmenity(Integer roomId, Integer amenityCode, String newName) {
        action {
            load Room(roomId) as room
            find room.amenities where code == amenityCode as amenity
            amenity.name = newName
        }
    }
}
```

### Booking — create with cross-aggregate projections

```nebula
Methods {
    bookRoom(BookingUser user, BookingRoom room, String checkIn, String checkOut, Integer nights, Double price) {
        action {
            create Booking {
                user: user, room: room,
                checkInDate: checkIn, checkOutDate: checkOut,
                numberOfNights: nights, totalPrice: price,
                paymentMethod: CREDIT_CARD, confirmed: false
            }
        }
        publishes BookingCreatedEvent {
            userAggregateId: user.userAggregateId,
            roomAggregateId: room.roomAggregateId,
            totalPrice: price
        }
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    confirmBooking(Integer bookingId) {
        action {
            load Booking(bookingId) as booking
            booking.confirmed = true
        }
    }
}
```

---

**Previous:** [09-Advanced-Patterns](09-Advanced-Patterns.md) | **Next:** [11-Workflows-Sagas](11-Workflows-Sagas.md)
