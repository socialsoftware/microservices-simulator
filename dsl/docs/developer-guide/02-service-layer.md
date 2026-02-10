# Chapter 02 · Service Layer & CRUD

[← Back: First Aggregate](01-first-aggregate.md) · [📚 Guide Index](00-index.md) · [Next: REST API →](03-rest-api.md)

---

## What You'll Build

A service layer that makes your Customer aggregate actually DO things: create, read, update, delete (CRUD).

**By the end of this chapter:**
- ✅ You'll add **one annotation** and get 5 CRUD methods
- ✅ You'll understand the Unit of Work pattern (transaction boundaries)
- ✅ You'll see the immutable aggregate pattern in action
- ✅ You'll get collection operations for free (add/remove/update loyalty cards)

**Time:** 20 minutes

---

## The Problem: Entities That Just Sit There

In Chapter 01, we created a Customer aggregate. It has entities, DTOs, a factory, and a repository. But it's all just... structure. No behavior.

```java
// What we have now
Customer customer = new Customer(1, customerDto);

// What can we do with it?
// ... nothing, really. We can't save it, update it, or delete it.
```

**We need a service layer** to orchestrate operations: load, modify, save, validate, publish events.

**This is where `@GenerateCrud` comes in.**

---

## Step 2.1: Enable CRUD Operations

### The DSL

Add **one annotation** to your aggregate:

**[`examples/02-service/abstractions/customer.nebula`](examples/02-service/abstractions/customer.nebula)**

```nebula
Aggregate Customer {
    @GenerateCrud;  // ← Magic happens here

    Root Entity Customer {
        String name;
        String email;
        Address address;
    }

    Entity Address {
        String street;
        String city;
        String zipCode;
    }
}
```

**That's it.** One line. Let's see what it generates.

### Generate and Check

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/developer-guide/examples/02-service/abstractions/
```

**New files generated:**
```
microservices/customer/service/
└── CustomerService.java           # ← 5 CRUD methods

coordination/functionalities/
└── CustomerFunctionalities.java   # ← Coordination layer
```

### The Generated Service

**[`examples/02-service/generated/CustomerService.java`](examples/02-service/generated/CustomerService.java)** (simplified)

```java
package pt.ulisboa.tecnico.socialsoftware.customer.microservices.customer.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitofwork.*;
import pt.ulisboa.tecnico.socialsoftware.customer.microservices.customer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.customer.shared.dtos.*;

@Service
public class CustomerService {
    @Autowired
    private CustomerFactory customerFactory;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    // 1. CREATE
    public CustomerDto createCustomer(CreateCustomerRequestDto dto, UnitOfWork unitOfWork) {
        Customer customer = customerFactory.createCustomer(null, dto);
        customer = unitOfWorkService.registerChanged(customer, unitOfWork);
        return customerFactory.createCustomerDto(customer);
    }

    // 2. READ BY ID
    public CustomerDto getCustomerById(Integer aggregateId, UnitOfWork unitOfWork) {
        Customer customer = unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        return customerFactory.createCustomerDto(customer);
    }

    // 3. READ ALL
    public List<CustomerDto> getAllCustomers(UnitOfWork unitOfWork) {
        return unitOfWorkService.aggregateLoadAllAndRegisterRead(unitOfWork).stream()
            .map(customer -> customerFactory.createCustomerDto((Customer) customer))
            .collect(Collectors.toList());
    }

    // 4. UPDATE
    public CustomerDto updateCustomer(CustomerDto dto, UnitOfWork unitOfWork) {
        Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(dto.getAggregateId(), unitOfWork);
        Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

        newCustomer.setName(dto.getName());
        newCustomer.setEmail(dto.getEmail());

        if (dto.getAddress() != null) {
            newCustomer.setAddress(new Address(dto.getAddress()));
        }

        newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);
        return customerFactory.createCustomerDto(newCustomer);
    }

    // 5. DELETE
    public void deleteCustomer(Integer aggregateId, UnitOfWork unitOfWork) {
        Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

        newCustomer.remove();  // Sets state to DELETED

        unitOfWorkService.registerChanged(newCustomer, unitOfWork);
    }
}
```

**5 methods from 1 annotation.** That's ~150 lines of service code you didn't write.

**Notice the pattern:**
1. Load aggregate (if updating/deleting)
2. Create new version (`createFromExisting`)
3. Apply changes
4. Register changes (`registerChanged`)
5. Return DTO

**This is the immutable aggregate pattern.** Let's understand it.

---

## Understanding Unit of Work

Every service method takes a `UnitOfWork` parameter. What is it?

**Unit of Work** = Transaction boundary + change tracking + event collection.

**Think of it as a shopping cart:**
- You add items (aggregate changes, events)
- At checkout (commit), everything is persisted to the database
- If checkout fails, the cart is cleared (rollback)

### Unit of Work Lifecycle

```
1. CREATE unit of work
   ↓
2. LOAD aggregates (register reads)
   ↓
3. MODIFY aggregates (create new versions)
   ↓
4. REGISTER changes (add to unit of work)
   ↓
5. COMMIT (save to DB + publish events)
```

**Code example:**

```java
// 1. Create unit of work (transaction boundary)
UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("update-customer");

// 2. Load aggregate
Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(1, unitOfWork);

// 3. Create new version (immutable pattern)
Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

// 4. Modify
newCustomer.setEmail("newemail@example.com");

// 5. Register changes
unitOfWorkService.registerChanged(newCustomer, unitOfWork);

// 6. Commit happens automatically at end of transaction
// (Spring manages this for us)
```

**What `registerChanged` does:**
- Validates invariants (Chapter 06)
- Creates new database row (immutable pattern)
- Links new version to old version (sets `prev` pointer)
- Tracks change in unit of work
- Prepares events for publishing

**Why this matters:**
- Transaction safety: All-or-nothing persistence
- Event consistency: Events only published if transaction succeeds
- Saga compensation: Version chain enables automatic rollback
- Audit trail: Every change is a new row

---

## The Immutable Aggregate Pattern

**Key rule:** Never mutate a loaded aggregate directly. Always create a new version.

### ❌ The Wrong Way

```java
// DON'T DO THIS
Customer customer = loadCustomer(1);
customer.setEmail("new@example.com");  // ← Mutating loaded aggregate!
registerChanged(customer);
```

**Why wrong?**
- Breaks version chain
- No audit trail
- Saga compensation can't rollback
- Concurrent modifications collide

### ✅ The Right Way

```java
// DO THIS
Customer oldCustomer = loadCustomer(1);
Customer newCustomer = createFromExisting(oldCustomer);  // ← New version
newCustomer.setEmail("new@example.com");
registerChanged(newCustomer);
```

**Why right?**
- Creates new database row with `prev` pointer to old row
- Old version stays in database (audit trail)
- Saga can rollback by following `prev` chain
- Concurrent transactions work on different versions

### The Version Chain

**Database after creating and updating a customer twice:**

```
id | aggregateId | version | state  | prev | name    | email
---+-------------+---------+--------+------+---------+------------------
1  | 100         | 1       | ACTIVE | null | Alice   | alice@coffee.com
2  | 100         | 2       | ACTIVE | 1    | Alice   | alice@example.com
3  | 100         | 3       | ACTIVE | 2    | Alice B | alice@example.com
```

**Each row is immutable.** You can't change row 1. You can only create row 2 that points to row 1.

**For saga compensation:**
```
Transaction fails → Follow prev chain → Restore row 1 → Rollback complete
```

**This is why Nebula generates copy constructors.** They create the new version.

---

## Step 2.2: Understanding Generated CRUD Methods

Let's examine each generated method in detail.

### Method 1: Create

```java
public CustomerDto createCustomer(CreateCustomerRequestDto dto, UnitOfWork unitOfWork) {
    // 1. Factory creates entity from DTO
    Customer customer = customerFactory.createCustomer(null, dto);

    // 2. Register as changed (will be INSERTed)
    customer = unitOfWorkService.registerChanged(customer, unitOfWork);

    // 3. Convert to DTO and return
    return customerFactory.createCustomerDto(customer);
}
```

**Why `aggregateId` is null:** The database generates it on INSERT. After `registerChanged`, the customer has an ID.

**What `registerChanged` does on create:**
- Validates invariants (if defined)
- Assigns next `aggregateId`
- Sets `version = 1`
- Sets `prev = null` (first version)
- Adds to unit of work for INSERT

### Method 2: Read by ID

```java
public CustomerDto getCustomerById(Integer aggregateId, UnitOfWork unitOfWork) {
    // 1. Load aggregate by business ID
    Customer customer = unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);

    // 2. Convert to DTO
    return customerFactory.createCustomerDto(customer);
}
```

**Why register reads?** The unit of work tracks what was read for potential saga compensation. If the transaction fails, the saga knows what to "un-read."

**Load strategy:** Loads the **latest version** where `state = ACTIVE`.

```sql
SELECT * FROM Customer
WHERE aggregateId = ? AND state = 'ACTIVE'
ORDER BY version DESC
LIMIT 1
```

### Method 3: Read All

```java
public List<CustomerDto> getAllCustomers(UnitOfWork unitOfWork) {
    return unitOfWorkService.aggregateLoadAllAndRegisterRead(unitOfWork).stream()
        .map(customer -> customerFactory.createCustomerDto((Customer) customer))
        .collect(Collectors.toList());
}
```

**Loads all ACTIVE aggregates.** Skips DELETED or INACTIVE.

```sql
SELECT DISTINCT ON (aggregateId) *
FROM Customer
WHERE state = 'ACTIVE'
ORDER BY aggregateId, version DESC
```

**Result:** One row per `aggregateId` (the latest version).

### Method 4: Update

```java
public CustomerDto updateCustomer(CustomerDto dto, UnitOfWork unitOfWork) {
    // 1. Load current version
    Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(dto.getAggregateId(), unitOfWork);

    // 2. Create new version (copy constructor)
    Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

    // 3. Apply changes from DTO
    newCustomer.setName(dto.getName());
    newCustomer.setEmail(dto.getEmail());

    if (dto.getAddress() != null) {
        newCustomer.setAddress(new Address(dto.getAddress()));
    }

    // 4. Register new version
    newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);

    // 5. Return updated DTO
    return customerFactory.createCustomerDto(newCustomer);
}
```

**The immutable pattern in action:**
- Load version N
- Create version N+1 (copy constructor)
- Modify version N+1
- Save version N+1 with `prev = version N's ID`

**Database result:**
```
Before: id=1, version=1, prev=null, name="Alice"
After:  id=1, version=1, prev=null, name="Alice"  (unchanged)
        id=2, version=2, prev=1,    name="Alice B" (new row)
```

### Method 5: Delete

```java
public void deleteCustomer(Integer aggregateId, UnitOfWork unitOfWork) {
    // 1. Load current version
    Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);

    // 2. Create new version
    Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

    // 3. Mark as deleted (sets state = DELETED)
    newCustomer.remove();

    // 4. Register deletion
    unitOfWorkService.registerChanged(newCustomer, unitOfWork);
}
```

**Soft delete, not hard delete.** The row stays in the database with `state = DELETED`.

**Database result:**
```
id=1, version=1, state=ACTIVE,  prev=null, name="Alice"
id=2, version=2, state=DELETED, prev=1,    name="Alice"  (marked deleted)
```

**Why soft delete?**
- Audit trail preserved
- Saga compensation can restore
- Other aggregates can check if Customer was deleted (via events)

**Future queries skip DELETED rows** (via `WHERE state = 'ACTIVE'`).

---

## Step 2.3: Collection Operations (Bonus!)

Remember the `Set<LoyaltyCard> loyaltyCards` from Chapter 01? Nebula generated collection management methods too.

**Generated methods (not shown in excerpts, but they're there):**

### Add a Loyalty Card

```java
public CustomerDto addLoyaltyCard(Integer customerAggregateId, LoyaltyCardDto cardDto, UnitOfWork unitOfWork) {
    // 1. Load customer
    Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(customerAggregateId, unitOfWork);

    // 2. Create new version
    Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

    // 3. Add card to collection
    LoyaltyCard newCard = new LoyaltyCard(cardDto);
    newCustomer.getLoyaltyCards().add(newCard);

    // 4. Register change
    newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);

    return customerFactory.createCustomerDto(newCustomer);
}
```

### Remove a Loyalty Card

```java
public CustomerDto removeLoyaltyCard(Integer customerAggregateId, String cardNumber, UnitOfWork unitOfWork) {
    Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(customerAggregateId, unitOfWork);
    Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

    // Find and remove card
    newCustomer.getLoyaltyCards().removeIf(card -> card.getCardNumber().equals(cardNumber));

    newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);
    return customerFactory.createCustomerDto(newCustomer);
}
```

### Update a Loyalty Card

```java
public CustomerDto updateLoyaltyCard(Integer customerAggregateId, LoyaltyCardDto cardDto, UnitOfWork unitOfWork) {
    Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(customerAggregateId, unitOfWork);
    Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

    // Find and update card
    LoyaltyCard cardToUpdate = newCustomer.getLoyaltyCards().stream()
        .filter(card -> card.getCardNumber().equals(cardDto.getCardNumber()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Card not found"));

    cardToUpdate.setPoints(cardDto.getPoints());
    cardToUpdate.setExpiresAt(cardDto.getExpiresAt());

    newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);
    return customerFactory.createCustomerDto(newCustomer);
}
```

**You didn't write any of this.** Nebula detected the collection and generated CRUD for it.

**Generated for EVERY collection in your aggregate:**
- `add<CollectionElement>`
- `remove<CollectionElement>`
- `update<CollectionElement>`
- `get<CollectionElement>`

---

## The Functionalities Layer

You might have noticed Nebula also generated `CustomerFunctionalities.java`. What's that?

**[`examples/02-service/generated/CustomerFunctionalities.java`](examples/02-service/generated/CustomerFunctionalities.java)** (excerpt)

```java
@Component
public class CustomerFunctionalities {
    @Autowired
    private CustomerService customerService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CustomerDto createCustomer(CreateCustomerRequestDto dto) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("CreateCustomer");
        CustomerDto result = customerService.createCustomer(dto, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CustomerDto getCustomerById(Integer aggregateId) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("GetCustomerById");
        return customerService.getCustomerById(aggregateId, unitOfWork);
    }

    // ... other methods
}
```

**Functionalities = Coordination Layer**

**What it does:**
- Creates Unit of Work
- Calls service method
- Commits Unit of Work
- Wraps everything in `@Transactional`

**Why separate from Service?**
- Service layer = pure business logic (testable without transactions)
- Functionalities layer = transaction boundaries + coordination
- Clean separation of concerns

**Controllers call Functionalities, not Service directly.**

---

## Common Mistakes

### ❌ Mistake 1: Mutating Loaded Aggregate

```java
// WRONG
Customer customer = loadCustomer(1);
customer.setEmail("new@example.com");  // Mutating!
registerChanged(customer);
```

**Problem:** Breaks version chain, no audit trail, saga can't compensate.

**Fix:**
```java
// RIGHT
Customer oldCustomer = loadCustomer(1);
Customer newCustomer = createFromExisting(oldCustomer);
newCustomer.setEmail("new@example.com");
registerChanged(newCustomer);
```

### ❌ Mistake 2: Forgetting to Register Changes

```java
// WRONG
Customer newCustomer = createFromExisting(oldCustomer);
newCustomer.setEmail("new@example.com");
// Forgot registerChanged!
return createDto(newCustomer);
```

**Problem:** Changes aren't saved to database. The new version vanishes.

**Fix:** Always call `registerChanged`:
```java
newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);
```

### ❌ Mistake 3: Calling Service Without Unit of Work

```java
// WRONG
customerService.createCustomer(dto, null);  // null UnitOfWork!
```

**Problem:** NullPointerException. Services need a unit of work.

**Fix:** Use Functionalities (it creates the unit of work):
```java
customerFunctionalities.createCustomer(dto);
```

Or create one manually (rare):
```java
UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("MyOperation");
customerService.createCustomer(dto, unitOfWork);
unitOfWorkService.commit(unitOfWork);
```

### ❌ Mistake 4: Shallow Copy in Collections

```java
// WRONG (in copy constructor)
this.loyaltyCards = other.getLoyaltyCards();  // Shallow copy!
```

**Problem:** Both old and new customer point to the SAME collection. Changes to one affect the other.

**Fix:** Deep copy with streams:
```java
this.loyaltyCards = other.getLoyaltyCards().stream()
    .map(LoyaltyCard::new)  // Copy constructor
    .collect(Collectors.toSet());
```

---

## What You Learned

**Concepts:**
- ✅ `@GenerateCrud` generates 5 CRUD methods + collection operations
- ✅ Unit of Work = transaction boundary + change tracking + event collection
- ✅ Immutable aggregate pattern: load → copy → modify → register
- ✅ Version chains enable audit trail and saga compensation
- ✅ Soft delete (state = DELETED) preserves history
- ✅ Functionalities layer handles transactions and coordination

**Generated Code:**
- ✅ `CustomerService.java` with 5 CRUD methods
- ✅ Collection CRUD (add/remove/update) for every collection
- ✅ `CustomerFunctionalities.java` for transaction management
- ✅ Copy constructors in entities (deep copy)

**Patterns:**
- ✅ Factory pattern for entity creation
- ✅ Unit of Work pattern for transactions
- ✅ Immutable aggregate pattern for safe updates
- ✅ Soft delete pattern for audit trail

**From 1 annotation** (`@GenerateCrud`), you got:
- ~150 lines of service code
- ~100 lines of functionalities code
- 5 CRUD methods
- 4 collection methods per collection
- Transaction management
- Event handling infrastructure (Chapter 07)

**Total generated: ~250 lines you didn't write.**

---

## Next Steps

Your Customer can now be created, read, updated, and deleted. But it's locked inside the application layer.

In **Chapter 03**, we'll expose it via REST API:
- ✅ HTTP endpoints (POST, GET, PUT, DELETE)
- ✅ Controllers with Spring annotations
- ✅ Request/Response DTOs
- ✅ OpenAPI documentation (bonus)

**Time to let the outside world talk to your Customer.** ☕

---

**Next: [Chapter 03 - REST API](03-rest-api.md) →**

---

## Quick Reference

### Enable CRUD
```nebula
Aggregate Customer {
    @GenerateCrud;  // ← Generates 5 CRUD methods

    Root Entity Customer { ... }
}
```

### Generated Service Methods
```java
createCustomer(dto, unitOfWork)
getCustomerById(id, unitOfWork)
getAllCustomers(unitOfWork)
updateCustomer(dto, unitOfWork)
deleteCustomer(id, unitOfWork)
```

### Collection Methods (Auto-Generated)
```java
addLoyaltyCard(customerId, cardDto, unitOfWork)
removeLoyaltyCard(customerId, cardNumber, unitOfWork)
updateLoyaltyCard(customerId, cardDto, unitOfWork)
getLoyaltyCard(customerId, cardNumber, unitOfWork)
```

### Immutable Update Pattern
```java
// 1. Load
Customer oldCustomer = unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);

// 2. Copy
Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);

// 3. Modify
newCustomer.setEmail("new@example.com");

// 4. Register
newCustomer = unitOfWorkService.registerChanged(newCustomer, unitOfWork);
```

### Unit of Work Lifecycle
```java
UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("OperationName");
// ... service operations ...
unitOfWorkService.commit(unitOfWork);  // or rollback() on error
```
