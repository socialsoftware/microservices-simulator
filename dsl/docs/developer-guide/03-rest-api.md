# Chapter 03 · REST API

[← Back: Service Layer](02-service-layer.md) · [📚 Guide Index](00-index.md) · [Next: Cross-Aggregate References →](04-cross-aggregate.md)

---

## What You'll Build

REST endpoints that expose your Customer service to the outside world via HTTP.

**By the end of this chapter:**
- ✅ You'll generate a Spring Boot controller with 5 REST endpoints
- ✅ You'll understand request/response DTOs
- ✅ You'll see the complete request flow (HTTP → Controller → Functionalities → Service)
- ✅ You'll test your API with curl

**Time:** 15 minutes

---

## The Problem: Services Locked in the Application Layer

In Chapter 02, we generated a service layer with 5 CRUD methods:

```java
customerService.createCustomer(dto, unitOfWork);
customerService.getCustomerById(id, unitOfWork);
customerService.getAllCustomers(unitOfWork);
customerService.updateCustomer(dto, unitOfWork);
customerService.deleteCustomer(id, unitOfWork);
```

**Problem:** These methods are locked inside the Java application. No one can call them from outside.

**Solution:** Expose them via REST API.

**The good news:** You already wrote everything needed. The `@GenerateCrud` annotation generates both the service AND the controller.

---

## Step 3.1: The Generated Controller

Remember this DSL from Chapter 02?

**[`examples/03-api/abstractions/customer.nebula`](examples/03-api/abstractions/customer.nebula)**

```nebula
Aggregate Customer {
    @GenerateCrud;  // ← Also generates REST controller

    Root Entity Customer {
        String name;
        String email;
        Boolean active;
        Address address;
    }

    Entity Address {
        String street;
        String city;
        String zipCode;
    }
}
```

Nebula already generated a controller for this. Let's look at it.

### The Generated Controller

**[`examples/03-api/generated/CustomerController.java`](examples/03-api/generated/CustomerController.java)**

```java
package pt.ulisboa.tecnico.socialsoftware.customer.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import pt.ulisboa.tecnico.socialsoftware.customer.coordination.functionalities.CustomerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.customer.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.customer.coordination.webapi.requestDtos.CreateCustomerRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CustomerController {
    @Autowired
    private CustomerFunctionalities customerFunctionalities;

    // 1. CREATE
    @PostMapping("/customers/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto createCustomer(@RequestBody CreateCustomerRequestDto request) {
        return customerFunctionalities.createCustomer(request);
    }

    // 2. READ BY ID
    @GetMapping("/customers/{customerAggregateId}")
    public CustomerDto getCustomerById(@PathVariable Integer customerAggregateId) {
        return customerFunctionalities.getCustomerById(customerAggregateId);
    }

    // 3. READ ALL
    @GetMapping("/customers")
    public List<CustomerDto> getAllCustomers() {
        return customerFunctionalities.getAllCustomers();
    }

    // 4. UPDATE
    @PutMapping("/customers")
    public CustomerDto updateCustomer(@RequestBody CustomerDto customerDto) {
        return customerFunctionalities.updateCustomer(customerDto);
    }

    // 5. DELETE
    @DeleteMapping("/customers/{customerAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Integer customerAggregateId) {
        customerFunctionalities.deleteCustomer(customerAggregateId);
    }
}
```

**5 endpoints from the same `@GenerateCrud` annotation.** No extra work required.

**What Nebula gave you:**
- ✅ `@RestController` and `@RequestMapping("/api")`
- ✅ HTTP method annotations (`@PostMapping`, `@GetMapping`, etc.)
- ✅ Path variables (`@PathVariable`) and request bodies (`@RequestBody`)
- ✅ HTTP status codes (`@ResponseStatus`)
- ✅ Calls to Functionalities layer (not Service directly)

**Notice:** The controller doesn't handle Unit of Work. That's the Functionalities layer's job (Chapter 02).

---

## Generated REST Endpoints

### Endpoint 1: Create Customer

```java
@PostMapping("/customers/create")
@ResponseStatus(HttpStatus.CREATED)
public CustomerDto createCustomer(@RequestBody CreateCustomerRequestDto request)
```

**HTTP Request:**
```http
POST /api/customers/create
Content-Type: application/json

{
  "name": "Alice",
  "email": "alice@coffee.com",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "zipCode": "12345"
  }
}
```

**HTTP Response:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "aggregateId": 1,
  "version": 1,
  "state": "ACTIVE",
  "name": "Alice",
  "email": "alice@coffee.com",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "zipCode": "12345"
  }
}
```

**Notice:**
- Request uses `CreateCustomerRequestDto` (no aggregateId, version, state)
- Response uses `CustomerDto` (includes aggregateId, version, state)
- Status is `201 Created`

### Endpoint 2: Get Customer by ID

```java
@GetMapping("/customers/{customerAggregateId}")
public CustomerDto getCustomerById(@PathVariable Integer customerAggregateId)
```

**HTTP Request:**
```http
GET /api/customers/1
```

**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "aggregateId": 1,
  "version": 1,
  "state": "ACTIVE",
  "name": "Alice",
  "email": "alice@coffee.com",
  "address": { ... }
}
```

**Path variable:** `{customerAggregateId}` becomes method parameter `customerAggregateId`.

### Endpoint 3: Get All Customers

```java
@GetMapping("/customers")
public List<CustomerDto> getAllCustomers()
```

**HTTP Request:**
```http
GET /api/customers
```

**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "aggregateId": 1,
    "version": 1,
    "state": "ACTIVE",
    "name": "Alice",
    "email": "alice@coffee.com",
    "address": { ... }
  },
  {
    "aggregateId": 2,
    "version": 1,
    "state": "ACTIVE",
    "name": "Bob",
    "email": "bob@coffee.com",
    "address": { ... }
  }
]
```

**Returns only ACTIVE customers** (skips DELETED/INACTIVE).

### Endpoint 4: Update Customer

```java
@PutMapping("/customers")
public CustomerDto updateCustomer(@RequestBody CustomerDto customerDto)
```

**HTTP Request:**
```http
PUT /api/customers
Content-Type: application/json

{
  "aggregateId": 1,
  "version": 1,
  "state": "ACTIVE",
  "name": "Alice Updated",
  "email": "alice.new@coffee.com",
  "address": { ... }
}
```

**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "aggregateId": 1,
  "version": 2,  ← Incremented!
  "state": "ACTIVE",
  "name": "Alice Updated",
  "email": "alice.new@coffee.com",
  "address": { ... }
}
```

**Notice:**
- Request uses full `CustomerDto` (includes aggregateId to identify which customer)
- Response has `version: 2` (immutable pattern created new version)

### Endpoint 5: Delete Customer

```java
@DeleteMapping("/customers/{customerAggregateId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteCustomer(@PathVariable Integer customerAggregateId)
```

**HTTP Request:**
```http
DELETE /api/customers/1
```

**HTTP Response:**
```http
HTTP/1.1 204 No Content
```

**Soft delete:** Customer's state is set to DELETED. The row stays in the database.

---

## Request vs Response DTOs

You might have noticed two DTO types:

### CreateCustomerRequestDto

**[`examples/03-api/generated/CreateCustomerRequestDto.java`](examples/03-api/generated/CreateCustomerRequestDto.java)**

```java
public class CreateCustomerRequestDto {
    private String name;
    private String email;
    private Boolean active;
    private AddressDto address;

    // No aggregateId, version, or state
    // Those are set by the system

    // Getters and setters...
}
```

**Used for:** Creating new customers.

**Why separate from CustomerDto?**
- Client doesn't provide aggregateId (system generates it)
- Client doesn't provide version (starts at 1)
- Client doesn't provide state (defaults to ACTIVE)

**This prevents clients from:**
- Creating Customer with aggregateId = 999 (wrong!)
- Creating Customer with version = 100 (nonsense!)
- Creating Customer with state = DELETED (why??)

### CustomerDto

```java
public class CustomerDto {
    private Integer aggregateId;  // ← Included
    private Integer version;      // ← Included
    private String state;         // ← Included
    private String name;
    private String email;
    private Boolean active;
    private AddressDto address;

    // Getters and setters...
}
```

**Used for:** Reading and updating customers.

**For updates:** Client sends full DTO including aggregateId (identifies which customer) and version (optimistic locking).

---

## The Complete Request Flow

Let's trace a complete request through all layers:

### Flow for Create Customer

```
HTTP Request
    ↓
CustomerController.createCustomer()
    ↓
CustomerFunctionalities.createCustomer()
    ├─ Creates UnitOfWork
    ├─ Calls CustomerService.createCustomer()
    │   ├─ Factory creates entity from DTO
    │   ├─ Registers entity as changed
    │   └─ Converts entity back to DTO
    ├─ Commits UnitOfWork
    │   ├─ Saves to database
    │   └─ Publishes events (Chapter 07)
    └─ Returns DTO
    ↓
CustomerController returns DTO
    ↓
Spring converts DTO to JSON
    ↓
HTTP Response
```

**Layers:**
1. **Controller** (HTTP boundary) - Handles HTTP, delegates to Functionalities
2. **Functionalities** (Coordination) - Manages transactions, calls Service
3. **Service** (Business logic) - Pure logic, no transactions
4. **Repository** (Data access) - Spring Data JPA

**Why so many layers?**
- Clear separation of concerns
- Service is testable without HTTP or transactions
- Functionalities can be reused by different controllers
- Controller can be swapped (REST → GraphQL) without touching Service

---

## Testing the API

Let's test our generated API with curl.

### Start the Application

```bash
# Compile and run
cd applications/customer
mvn spring-boot:run
```

**Wait for:**
```
Started CustomerApplication in 3.5 seconds
```

### Test 1: Create Customer

```bash
curl -X POST http://localhost:8080/api/customers/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@coffee.com",
    "active": true,
    "address": {
      "street": "123 Main St",
      "city": "Springfield",
      "zipCode": "12345"
    }
  }'
```

**Expected response:**
```json
{
  "aggregateId": 1,
  "version": 1,
  "state": "ACTIVE",
  "name": "Alice",
  "email": "alice@coffee.com",
  "active": true,
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "zipCode": "12345"
  }
}
```

**Check the database:**
```sql
SELECT * FROM Customer WHERE aggregateId = 1;
```

You'll see one row with version 1, prev = null.

### Test 2: Get Customer

```bash
curl http://localhost:8080/api/customers/1
```

**Response:** Same as create response.

### Test 3: Update Customer

```bash
curl -X PUT http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": 1,
    "version": 1,
    "state": "ACTIVE",
    "name": "Alice Updated",
    "email": "alice.new@coffee.com",
    "active": true,
    "address": {
      "street": "456 Oak Ave",
      "city": "Springfield",
      "zipCode": "12345"
    }
  }'
```

**Expected response:**
```json
{
  "aggregateId": 1,
  "version": 2,  ← Incremented
  "state": "ACTIVE",
  "name": "Alice Updated",
  "email": "alice.new@coffee.com",
  ...
}
```

**Check the database:**
```sql
SELECT id, aggregateId, version, prev, name FROM Customer WHERE aggregateId = 1;
```

**Result:**
```
id | aggregateId | version | prev | name
---+-------------+---------+------+--------------
1  | 1           | 1       | null | Alice
2  | 1           | 2       | 1    | Alice Updated
```

**Version chain!** Row 2 points back to row 1 via `prev`.

### Test 4: Delete Customer

```bash
curl -X DELETE http://localhost:8080/api/customers/1
```

**Expected response:**
```
HTTP/1.1 204 No Content
```

**Check the database:**
```sql
SELECT id, aggregateId, version, state, prev FROM Customer WHERE aggregateId = 1;
```

**Result:**
```
id | aggregateId | version | state   | prev
---+-------------+---------+---------+------
1  | 1           | 1       | ACTIVE  | null
2  | 1           | 2       | ACTIVE  | 1
3  | 1           | 3       | DELETED | 2     ← Soft deleted
```

**Soft delete!** Row 3 has state = DELETED. The customer is "gone" but data is preserved.

### Test 5: Try to Get Deleted Customer

```bash
curl http://localhost:8080/api/customers/1
```

**Expected response:**
```
HTTP/1.1 404 Not Found
```

**Why?** The repository only loads ACTIVE aggregates. Deleted customers are invisible to the API.

---

## Common Mistakes

### ❌ Mistake 1: Missing aggregateId in Update Request

```json
{
  "name": "Alice Updated",
  "email": "alice@example.com"
  // Missing aggregateId!
}
```

**Error:** `400 Bad Request` or `NullPointerException`

**Fix:** Include aggregateId:
```json
{
  "aggregateId": 1,
  "name": "Alice Updated",
  ...
}
```

### ❌ Mistake 2: Sending aggregateId in Create Request

```json
{
  "aggregateId": 999,  // ← Don't do this!
  "name": "Alice",
  "email": "alice@example.com"
}
```

**Problem:** Create uses `CreateCustomerRequestDto` which doesn't have aggregateId field. The value is ignored.

**Fix:** Don't send aggregateId for create. Let the system generate it.

### ❌ Mistake 3: Using Wrong HTTP Method

```bash
# WRONG: Using POST for update
curl -X POST http://localhost:8080/api/customers \
  -d '{"aggregateId": 1, ...}'
```

**Error:** `404 Not Found` (wrong endpoint)

**Fix:** Use PUT for updates:
```bash
curl -X PUT http://localhost:8080/api/customers \
  -d '{"aggregateId": 1, ...}'
```

### ❌ Mistake 4: Forgetting Content-Type Header

```bash
# WRONG: No Content-Type
curl -X POST http://localhost:8080/api/customers/create \
  -d '{"name": "Alice", ...}'
```

**Error:** `415 Unsupported Media Type`

**Fix:** Add Content-Type header:
```bash
curl -X POST http://localhost:8080/api/customers/create \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", ...}'
```

---

## What You Learned

**Concepts:**
- ✅ `@GenerateCrud` generates both Service AND Controller
- ✅ Controllers call Functionalities, not Service directly
- ✅ Request DTOs (CreateXRequestDto) vs Response DTOs (XDto)
- ✅ HTTP status codes (201 Created, 204 No Content, 404 Not Found)
- ✅ Path variables (`@PathVariable`) vs request body (`@RequestBody`)
- ✅ Complete request flow: HTTP → Controller → Functionalities → Service → Repository

**Generated Code:**
- ✅ `CustomerController.java` with 5 REST endpoints
- ✅ `CreateCustomerRequestDto.java` for create operations
- ✅ Spring annotations (`@RestController`, `@PostMapping`, etc.)
- ✅ HTTP method mapping (POST, GET, PUT, DELETE)
- ✅ Proper status codes

**REST API:**
```
POST   /api/customers/create       → Create customer (201 Created)
GET    /api/customers/{id}         → Get customer by ID (200 OK)
GET    /api/customers              → Get all customers (200 OK)
PUT    /api/customers              → Update customer (200 OK)
DELETE /api/customers/{id}         → Delete customer (204 No Content)
```

**From 1 annotation** (`@GenerateCrud`), you got:
- 5 REST endpoints
- Controller with Spring annotations
- Request/Response DTOs
- Proper HTTP status codes
- Complete HTTP ↔ Database flow

**Total: ~200 lines of controller code you didn't write.**

---

## Complete Stack So Far

**DSL (18 lines):**
```nebula
Aggregate Customer {
    @GenerateCrud;

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

**Generated Java (~1,500 lines):**
- ✅ Customer.java, Address.java (entities)
- ✅ CustomerDto.java, AddressDto.java (DTOs)
- ✅ CreateCustomerRequestDto.java (request DTO)
- ✅ CustomerFactory.java (conversions)
- ✅ CustomerRepository.java (data access)
- ✅ CustomerService.java (5 CRUD methods)
- ✅ CustomerFunctionalities.java (transactions)
- ✅ CustomerController.java (5 REST endpoints)

**From 18 lines to a complete REST API.** ☕

---

## Next Steps

Your Customer aggregate is now fully exposed via REST API. But customers exist in isolation. In the real world, customers place orders.

In **Chapter 04**, we'll tackle **cross-aggregate references**:
- ✅ Order aggregate that references Customer
- ✅ `uses dto` syntax (projections)
- ✅ Maintaining aggregate boundaries
- ✅ Avoiding tight coupling

**How do aggregates talk to each other without breaking encapsulation?**

That's next.

---

**Next: [Chapter 04 - Cross-Aggregate References](04-cross-aggregate.md) →**

---

## Quick Reference

### Generated Endpoints
```
POST   /api/customers/create       # Create (201)
GET    /api/customers/{id}         # Get by ID (200)
GET    /api/customers              # Get all (200)
PUT    /api/customers              # Update (200)
DELETE /api/customers/{id}         # Delete (204)
```

### Create Request (curl)
```bash
curl -X POST http://localhost:8080/api/customers/create \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com"}'
```

### Update Request (curl)
```bash
curl -X PUT http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"aggregateId": 1, "name": "Alice Updated", ...}'
```

### Delete Request (curl)
```bash
curl -X DELETE http://localhost:8080/api/customers/1
```

### Request Flow
```
HTTP → Controller → Functionalities → Service → Repository → Database
       (HTTP)      (Transaction)    (Logic)   (JPA)
```
