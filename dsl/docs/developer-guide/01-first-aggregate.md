# Chapter 01 · Your First Aggregate

[← Back to Welcome](00-welcome.md) · [📚 Guide Index](00-index.md) · [Next: Service Layer →](02-service-layer.md)

---

## What You'll Build

A **Customer** aggregate that evolves from a simple entity (name + email) to a complete aggregate with relationships and collections.

**By the end of this chapter:**
- ✅ You'll understand what an aggregate is (and why you need one)
- ✅ You'll write 18 lines of DSL that generate ~1,200 lines of Java
- ✅ You'll see how Nebula handles entities, relationships, and collections
- ✅ You'll have actual compiled Java code you can run

**Time:** 15 minutes

---

## What Is an Aggregate?

In Domain-Driven Design, an **aggregate** is a cluster of entities and value objects that we treat as a single unit for data changes.

**Simple version:** An aggregate is a boundary. Everything inside the aggregate can change together. Everything outside needs to go through a different aggregate.

**Coffee shop example:**
- **Customer** is an aggregate (has name, email, address, loyalty cards)
- **Order** is a different aggregate (has items, total, customer reference)
- Customer and Order are **separate** - they can't directly modify each other

**Why this matters:** Aggregates enforce consistency boundaries. When you update a Customer, you don't accidentally break an Order.

**If this sounds abstract:** Don't worry. It'll make sense once you see it.

---

## Step 1.1: The Simplest Aggregate

Let's start with the absolute minimum: a customer with a name and email.

### The DSL

Create `customer-v1.nebula`:

```nebula
Aggregate Customer {
    Root Entity Customer {
        String name;
        String email;
    }
}
```

**That's it.** Two fields. Let's break it down:

- `Aggregate Customer` - Declares the aggregate boundary
- `Root Entity Customer` - The main entity (one per aggregate)
- `String name; String email;` - Properties (primitives)

**Why "Root" Entity?** Because an aggregate can have multiple entities, but only ONE is the root. The root is the entry point - all operations go through it.

### Generate the Code

```bash
# From dsl/nebula/
./bin/cli.js generate ../docs/developer-guide/examples/01-customer/abstractions/
```

**What gets generated:**

```
applications/customer/src/main/java/
└── pt/ulisboa/tecnico/socialsoftware/customer/
    ├── microservices/customer/
    │   └── aggregate/
    │       ├── Customer.java           # ← The entity
    │       ├── CustomerFactory.java    # ← Entity/DTO conversion
    │       └── CustomerRepository.java # ← Spring Data repo
    └── shared/
        └── dtos/
            └── CustomerDto.java        # ← The DTO
```

**4 Java classes from 2 lines of DSL.** Not bad. ☕

### The Generated Entity

Let's look at what Nebula generated for `Customer.java`:

**[`examples/01-customer/generated/Customer.java`](examples/01-customer/generated/Customer.java)**

```java
package pt.ulisboa.tecnico.socialsoftware.customer.microservices.customer.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.customer.shared.dtos.CustomerDto;

@Entity
public class Customer extends Aggregate {
    private String name;
    private String email;

    // Protected no-arg constructor (JPA requires this)
    protected Customer() { }

    // Constructor from DTO
    public Customer(Integer aggregateId, CustomerDto customerDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(customerDto.getName());
        setEmail(customerDto.getEmail());
    }

    // Copy constructor (for immutable pattern)
    public Customer(Customer other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

**What Nebula gave you:**
- ✅ `@Entity` annotation (JPA)
- ✅ Extends `Aggregate` (framework base class)
- ✅ Three constructors (no-arg, from DTO, copy constructor)
- ✅ Getters and setters
- ✅ No `@Id` or `@GeneratedValue` (handled by `Aggregate` superclass)

**The `Aggregate` superclass provides:**
- `id` (Long) - Primary key
- `aggregateId` (Integer) - Business identifier
- `version` (Integer) - For optimistic locking
- `state` (AggregateState) - ACTIVE, INACTIVE, or DELETED
- `prev` (Long) - Points to previous version (immutable pattern)

**Why three constructors?**
1. **No-arg:** JPA requires it (never call it yourself)
2. **From DTO:** Create new aggregate from API data
3. **Copy constructor:** Create new version for updates (immutable pattern)

### The Generated DTO

**[`examples/01-customer/generated/CustomerDto.java`](examples/01-customer/generated/CustomerDto.java)**

```java
package pt.ulisboa.tecnico.socialsoftware.customer.shared.dtos;

public class CustomerDto {
    private Integer aggregateId;
    private Integer version;
    private String state;
    private String name;
    private String email;

    public CustomerDto() { }

    // Getters and setters for all fields
    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ... version, state getters/setters
}
```

**DTOs are the API boundary.** They go over HTTP, get serialized to JSON, and shield your entities from the outside world.

### The Generated Factory

**[`examples/01-customer/generated/CustomerFactory.java`](examples/01-customer/generated/CustomerFactory.java)** (excerpt)

```java
@Component
public class CustomerFactory {
    // Entity from DTO
    public Customer createCustomer(Integer aggregateId, CustomerDto customerDto) {
        return new Customer(aggregateId, customerDto);
    }

    // DTO from entity
    public CustomerDto createCustomerDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setAggregateId(customer.getAggregateId());
        dto.setVersion(customer.getVersion());
        dto.setState(customer.getState().toString());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        return dto;
    }

    // Create new version from existing (immutable pattern)
    public Customer createCustomerFromExisting(Customer customer) {
        return new Customer(customer);
    }
}
```

**Factory pattern:** The factory knows how to convert between entities and DTOs. Services use the factory, never construct entities directly.

### What You Learned

**From 2 lines of DSL, you got:**
- ✅ JPA entity with proper annotations
- ✅ DTO for API communication
- ✅ Factory for entity/DTO conversion
- ✅ Repository interface (Spring Data)
- ✅ Copy constructor for immutable updates
- ✅ All the boilerplate you never wanted to write

**Primitive types supported:**
- `String`, `Integer`, `Long`, `Float`, `Double`, `Boolean`
- `LocalDateTime`, `LocalDate`
- `BigDecimal`

---

## Step 1.2: Add a Supporting Entity

A customer without an address is just a name floating in cyberspace. Let's fix that.

### The DSL (v2)

**[`examples/01-customer/abstractions/customer-v2.nebula`](examples/01-customer/abstractions/customer-v2.nebula)**

```nebula
Aggregate Customer {
    Root Entity Customer {
        String name;
        String email;
        Address address;  // ← New relationship
    }

    Entity Address {      // ← Supporting entity
        String street;
        String city;
        String zipCode;
    }
}
```

**What changed:**
- Added `Address address` to Customer (one-to-one relationship)
- Declared `Entity Address` (NOT "Root Entity" - it's a supporting entity)

**Why Address isn't a Root Entity:** Because Address can't exist without Customer. It's owned by Customer. No other aggregate can reference Address directly.

**Analogy:** Customer is Batman. Address is Robin. Robin can't exist without Batman. (Wait, can Robin exist without Batman? Forget this analogy.)

### The Generated Code

**[`examples/01-customer/generated/Address.java`](examples/01-customer/generated/Address.java)** (excerpt)

```java
@Entity
public class Address {
    @Id
    @GeneratedValue
    private Long id;

    private String street;
    private String city;
    private String zipCode;

    protected Address() { }

    public Address(AddressDto addressDto) {
        setStreet(addressDto.getStreet());
        setCity(addressDto.getCity());
        setZipCode(addressDto.getZipCode());
    }

    public Address(Address other) {
        setStreet(other.getStreet());
        setCity(other.getCity());
        setZipCode(other.getZipCode());
    }

    // Getters and setters...
}
```

**Notice:** Address is a plain `@Entity`, not extending `Aggregate`. Supporting entities don't get aggregate machinery (no version, no state, no prev pointer).

**Updated Customer.java:**

```java
@Entity
public class Customer extends Aggregate {
    private String name;
    private String email;

    @OneToOne(cascade = CascadeType.ALL)  // ← Nebula figured this out
    private Address address;

    // Constructors updated to include address
    public Customer(Integer aggregateId, CustomerDto customerDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(customerDto.getName());
        setEmail(customerDto.getEmail());

        if (customerDto.getAddress() != null) {
            setAddress(new Address(customerDto.getAddress()));
        }
    }

    public Customer(Customer other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());

        if (other.getAddress() != null) {
            setAddress(new Address(other.getAddress()));  // Deep copy
        }
    }

    // Getters and setters...
}
```

**What Nebula did:**
- ✅ Added `@OneToOne(cascade = CascadeType.ALL)` (delete Customer → delete Address)
- ✅ Updated constructors to handle Address
- ✅ Deep copy in copy constructor (important!)

**Why deep copy?** Because the immutable pattern requires each version to have its own copy of nested entities. Shallow copy would break the version chain.

### DTO Nesting

**CustomerDto now includes AddressDto:**

```java
public class CustomerDto {
    private Integer aggregateId;
    private String name;
    private String email;
    private AddressDto address;  // ← Nested DTO

    // Getters and setters...
}
```

**JSON representation:**
```json
{
  "aggregateId": 1,
  "name": "Alice",
  "email": "alice@coffee.com",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "zipCode": "12345"
  }
}
```

### What You Learned

- ✅ Supporting entities (non-root) for composition
- ✅ One-to-one relationships (Customer has one Address)
- ✅ `@OneToOne(cascade = CascadeType.ALL)` (delete cascades)
- ✅ Deep copying in copy constructors
- ✅ Nested DTOs (AddressDto inside CustomerDto)

**JPA Cascade Types:**
- `CascadeType.ALL` - Persist, merge, remove, refresh, detach all propagate
- Delete Customer → Address gets deleted automatically
- No orphan Address entities in the database

---

## Step 1.3: Add a Collection

One address per customer is fine, but our coffee shop has a loyalty program. Customers can have multiple loyalty cards.

### The DSL (v3)

**[`examples/01-customer/abstractions/customer-v3.nebula`](examples/01-customer/abstractions/customer-v3.nebula)**

```nebula
Aggregate Customer {
    Root Entity Customer {
        String name;
        String email;
        Address address;
        Set<LoyaltyCard> loyaltyCards;  // ← Collection!
    }

    Entity Address {
        String street;
        String city;
        String zipCode;
    }

    Entity LoyaltyCard {
        String cardNumber;
        Integer points;
        LocalDateTime expiresAt;
    }
}
```

**What changed:**
- Added `Set<LoyaltyCard> loyaltyCards` (one-to-many)
- Declared `Entity LoyaltyCard` with temporal field

**Why `Set` and not `List`?**
- `Set` - No duplicates, no guaranteed order (use this most of the time)
- `List` - Allows duplicates, preserves order (use when order matters)

**Coffee shop rule:** Each card number is unique, so `Set` makes sense. Can't have two cards with the same number.

### The Generated Code

**[`examples/01-customer/generated/LoyaltyCard.java`](examples/01-customer/generated/LoyaltyCard.java)** (excerpt)

```java
@Entity
public class LoyaltyCard {
    @Id
    @GeneratedValue
    private Long id;

    private String cardNumber;
    private Integer points;
    private LocalDateTime expiresAt;

    protected LoyaltyCard() { }

    public LoyaltyCard(LoyaltyCardDto loyaltyCardDto) {
        setCardNumber(loyaltyCardDto.getCardNumber());
        setPoints(loyaltyCardDto.getPoints());
        setExpiresAt(loyaltyCardDto.getExpiresAt());
    }

    public LoyaltyCard(LoyaltyCard other) {
        setCardNumber(other.getCardNumber());
        setPoints(other.getPoints());
        setExpiresAt(other.getExpiresAt());
    }

    // Getters and setters...
}
```

**Nothing special here** - it's a supporting entity like Address.

**Updated Customer.java:**

```java
@Entity
public class Customer extends Aggregate {
    private String name;
    private String email;

    @OneToOne(cascade = CascadeType.ALL)
    private Address address;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LoyaltyCard> loyaltyCards = new HashSet<>();  // ← Collection

    // Constructor from DTO
    public Customer(Integer aggregateId, CustomerDto customerDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(customerDto.getName());
        setEmail(customerDto.getEmail());

        if (customerDto.getAddress() != null) {
            setAddress(new Address(customerDto.getAddress()));
        }

        if (customerDto.getLoyaltyCards() != null) {
            setLoyaltyCards(customerDto.getLoyaltyCards().stream()
                .map(LoyaltyCard::new)
                .collect(Collectors.toSet()));
        }
    }

    // Copy constructor
    public Customer(Customer other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());

        if (other.getAddress() != null) {
            setAddress(new Address(other.getAddress()));
        }

        if (other.getLoyaltyCards() != null) {
            setLoyaltyCards(other.getLoyaltyCards().stream()
                .map(LoyaltyCard::new)  // Deep copy each card
                .collect(Collectors.toSet()));
        }
    }

    // Getters and setters...
}
```

**What Nebula did:**
- ✅ Added `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`
- ✅ Initialized collection (`= new HashSet<>()`)
- ✅ Stream-based deep copy in constructors
- ✅ `orphanRemoval = true` (remove card from set → delete from database)

**Why stream copy?**
```java
other.getLoyaltyCards().stream()
    .map(LoyaltyCard::new)  // Call copy constructor on each
    .collect(Collectors.toSet());
```

This creates a **new set with new objects**. Each LoyaltyCard is copied. Changes to the old set don't affect the new set.

### Collection Operations (Free!)

When you define a collection, Nebula generates service methods for collection management. We'll see these in Chapter 02, but here's a preview:

**Generated service methods (Chapter 02):**
- `addLoyaltyCard(customerId, loyaltyCardDto)`
- `removeLoyaltyCard(customerId, cardNumber)`
- `updateLoyaltyCard(customerId, loyaltyCardDto)`
- `getLoyaltyCard(customerId, cardNumber)`

**You didn't write these. Nebula did.** ☕

### What You Learned

- ✅ Collections: `Set<T>` for unique items, `List<T>` for ordered items
- ✅ One-to-many relationships (Customer has many LoyaltyCards)
- ✅ `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`
- ✅ Stream-based deep copying for collections
- ✅ Collection CRUD operations auto-generated (Chapter 02)
- ✅ `LocalDateTime` for temporal data

**JPA orphanRemoval:**
```java
customer.getLoyaltyCards().remove(card);
// → Card is deleted from database (orphan removal)
```

Without `orphanRemoval = true`, the card would stay in the database (orphaned). We don't want that.

---

## Compile and Run

Let's verify everything compiles:

```bash
# Generate code (from v3 - the most complete version)
cd dsl/nebula
./bin/cli.js generate ../docs/developer-guide/examples/01-customer/abstractions/

# The output is in applications/customer/
cd ../../applications/customer

# Compile
mvn clean compile
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Compiling XX source files
```

**If it fails:**
- Check that simulator was built (`cd simulator && mvn clean install`)
- Check Java version (`java -version` should be 21+)
- Check for typos in your .nebula file

**If it succeeds:** You just generated a complete aggregate with JPA entities, DTOs, factories, and repositories. From 18 lines of DSL.

---

## Summary: What You Built

**18 lines of DSL:**
```nebula
Aggregate Customer {
    Root Entity Customer {
        String name;
        String email;
        Address address;
        Set<LoyaltyCard> loyaltyCards;
    }

    Entity Address {
        String street;
        String city;
        String zipCode;
    }

    Entity LoyaltyCard {
        String cardNumber;
        Integer points;
        LocalDateTime expiresAt;
    }
}
```

**Generated Java files:**
- `Customer.java` (~100 lines) - Root entity with JPA annotations
- `Address.java` (~50 lines) - Supporting entity
- `LoyaltyCard.java` (~60 lines) - Supporting entity
- `CustomerDto.java` (~80 lines) - API DTO
- `AddressDto.java` (~40 lines) - Nested DTO
- `LoyaltyCardDto.java` (~50 lines) - Nested DTO
- `CustomerFactory.java` (~150 lines) - Entity/DTO conversion
- `CustomerRepository.java` (~20 lines) - Spring Data interface

**Total: ~550 lines of Java you didn't write.**

And we haven't even added service methods, REST endpoints, or event handling yet. Those come next.

---

## Common Mistakes

### ❌ Mistake 1: Forgetting "Root" on Main Entity

```nebula
Aggregate Customer {
    Entity Customer {  // ← Missing "Root"!
        String name;
    }
}
```

**Error:** Nebula won't know which entity is the aggregate root.

**Fix:** Add `Root`:
```nebula
Root Entity Customer { ... }
```

### ❌ Mistake 2: Multiple Root Entities

```nebula
Aggregate Customer {
    Root Entity Customer { ... }
    Root Entity Address { ... }  // ← Can't have two roots!
}
```

**Error:** One aggregate = one root entity. That's the rule.

**Fix:** Make Address a supporting entity:
```nebula
Entity Address { ... }  // No "Root"
```

### ❌ Mistake 3: Circular References

```nebula
Aggregate Customer {
    Root Entity Customer {
        Address address;
    }

    Entity Address {
        Customer customer;  // ← Circular reference!
    }
}
```

**Error:** JPA will create an infinite loop.

**Fix:** Don't reference back to the root. The root owns the supporting entity, not the other way around.

### ❌ Mistake 4: Using List When You Mean Set

```nebula
List<LoyaltyCard> loyaltyCards;  // Allows duplicates!
```

**Problem:** Lists allow duplicate objects. You could end up with two cards with the same number.

**Fix:** Use `Set` for unique collections:
```nebula
Set<LoyaltyCard> loyaltyCards;
```

---

## What You Learned

**Concepts:**
- ✅ Aggregates are consistency boundaries
- ✅ One root entity per aggregate
- ✅ Supporting entities for composition
- ✅ Root entities extend `Aggregate`, supporting entities don't
- ✅ JPA relationships: `@OneToOne`, `@OneToMany`
- ✅ Cascade types and orphan removal

**Nebula DSL:**
- ✅ `Aggregate Name { }` - Declares aggregate
- ✅ `Root Entity Name { }` - Declares root entity
- ✅ `Entity Name { }` - Declares supporting entity
- ✅ `Type field;` - Declares property
- ✅ `Set<Type>` or `List<Type>` - Declares collection
- ✅ Primitive types: String, Integer, Boolean, LocalDateTime, etc.

**Generated Code:**
- ✅ JPA entities with proper annotations
- ✅ DTOs for API boundaries
- ✅ Factories for entity/DTO conversion
- ✅ Repositories (Spring Data)
- ✅ Copy constructors for immutable pattern
- ✅ Deep copying for nested objects and collections

---

## Next Steps

You have entities and DTOs, but they just sit there. In **Chapter 02**, we'll add:
- ✅ Service layer with CRUD operations (`@GenerateCrud`)
- ✅ Unit of Work pattern (transaction boundaries)
- ✅ Collection management (add/remove/update cards)
- ✅ Immutable aggregate updates

**Your Customer can now DO things instead of just BEING things.**

---

**Next: [Chapter 02 - Service Layer](02-service-layer.md) →**

---

## Quick Reference

### Aggregate Structure
```nebula
Aggregate Name {
    Root Entity Name {
        // Properties, relationships, collections
    }

    Entity SupportingEntity1 { }
    Entity SupportingEntity2 { }
}
```

### Relationship Types
```nebula
Address address;              // One-to-one
Set<LoyaltyCard> cards;       // One-to-many (unique)
List<Order> orders;           // One-to-many (ordered)
```

### Primitive Types
```nebula
String name;
Integer age;
Long id;
Boolean active;
Double price;
BigDecimal balance;
LocalDateTime createdAt;
LocalDate birthDate;
```

### Collection Types
```nebula
Set<Type> items;              // No duplicates, no order
List<Type> items;             // Allows duplicates, preserves order
```
