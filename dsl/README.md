# Nebula DSL

Nebula is a Domain-Specific Language (DSL) designed to generate complete Java microservices from high-level domain models. The primary goal is to generate code that can be executed within the microservices simulator, enabling developers to test different architectural patterns and analyze system behavior.

## Purpose and Features

The Nebula DSL serves as a code generation tool that transforms domain abstractions into fully functional Spring Boot microservices ready for execution in the simulator environment. It supports multiple architectural patterns and generates comprehensive code including entities, services, repositories, REST APIs, and event handling mechanisms.

**Key Features:**
- **Multiple Architectures**: Support for standard microservices, causal-saga patterns, and monolithic architectures
- **Comprehensive Code Generation**: Automatic generation of JPA entities, DTOs, service layers, repositories, and REST controllers
- **Business Logic Integration**: Built-in support for invariants, business rules, and custom method implementations  
- **Event-Driven Architecture**: Native support for event handling, saga coordination, and distributed transactions
- **Template-Based System**: Flexible Handlebars-based templates for customization and extension
- **Validation and Error Handling**: Built-in DSL validation with comprehensive error reporting
- **Docker Integration**: Automatic generation of Docker configurations and compose files

## Quick Start

### Installation

```bash
npm install
npm run build
```

### Basic Usage

1. Create a `.nebula` file:

```nebula
aggregate User {
    entity User {
        isRoot = true
        
        Integer id;
        String username;
        String email;
        LocalDateTime createdAt;
        
        invariants {
            usernameNotEmpty: username.length() > 0;
            emailValid: email.contains("@");
        }
        
        methods {
            updateEmail(String newEmail): User;
            isActive(): Boolean;
        }
    }
}
```

2. Generate Java code:

```bash
./bin/cli.js generate user.nebula --architecture causal-saga
```

3. Generated code will be in `../../applications/`

## Supported Architectures

### Microservices
- Entity generation with JPA annotations
- DTO classes for data transfer
- Service layer with business logic
- Repository interfaces with Spring Data JPA
- REST API controllers
- Event handling

### Causal-Saga
- All microservices features
- Saga coordination for distributed transactions
- Event sourcing and processing
- Causal consistency patterns
- Workflow management

### Monolith
- Entity generation
- Service layer
- Repository layer
- Simple, monolithic architecture

## Creating Abstractions: Complete Language Guide

### Overview

The Nebula DSL allows you to define domain models as **abstractions** that represent microservice boundaries. Each abstraction is defined in a `.nebula` file and contains aggregates, entities, business logic, and coordination patterns.

### Basic Structure

Every Nebula file follows this basic structure:

```nebula
import User;
import Course;

@Metadata {
    requiresUserDto;
    requiresAggregateState;
    hasEventService;
    hasTransactions;
    hasJpa;
}

Aggregate AggregateName {
    // Entities, methods, workflows, repositories, etc.
}

exceptions {
    ERROR_CODE: "Error message with %s placeholder";
}
```

## Language Reference

### 1. Aggregates

Aggregates are the main building blocks that represent bounded contexts in your microservice architecture.

```nebula
Aggregate OrderManagement {
    // All entities, methods, and workflows go here
}
```

**Key Points:**
- Each `.nebula` file should contain exactly one aggregate
- Aggregate names should be descriptive and represent business domains
- Aggregates encapsulate related entities and business logic

### 2. Entities

Entities represent the core data structures in your domain.

#### Root Entities
Every aggregate must have exactly one root entity:

```nebula
Root Entity Order {
    String orderNumber;
    LocalDateTime orderDate;
    String status;
    BigDecimal totalAmount;
    Customer customer;
    Set<OrderItem> orderItems;
}
```

#### Regular Entities
Supporting entities within the aggregate:

```nebula
Entity Customer {
    String name;
    String email;
    String address;
}

Entity OrderItem {
    String productName;
    Integer quantity;
    BigDecimal unitPrice;
}
```

#### Entity Properties

**Supported Data Types:**
```nebula
String name;
Integer count;
Long id;
Boolean active;
BigDecimal price;
LocalDateTime createdAt;
LocalDate birthDate;
Set<OrderItem> items;
List<String> tags;
Customer customer;
```

**Special Properties:**
```nebula
Entity Customer {
    Long id key;
    String name;
    AggregateState state;
    Integer version;
}
```

### 3. Invariants

Invariants define business rules that must always be true:

```nebula
Root Entity Order {
    String orderNumber;
    LocalDateTime orderDate;
    LocalDateTime deliveryDate;
    BigDecimal totalAmount;
    
    invariants {
        check orderNumberNotEmpty {
            orderNumber.length() > 0;
        }
        
        check deliveryAfterOrder {
            deliveryDate.isAfter(orderDate);
        }
        
        check positiveAmount {
            totalAmount > 0;
        }
        
        check uniqueItems {
            orderItems.unique(productId);
        }
        
        check completedOrderHasDeliveryDate {
            status != "COMPLETED" || deliveryDate != null;
        }
    }
}
```

### 4. Business Rules

Business rules define complex business logic with triggers and conditions:

```nebula
Root Entity Order {
    String status;
    LocalDateTime orderDate;
    LocalDateTime deliveryDate;
    
    businessRules {
        rule "AUTO_SET_ORDER_DATE" {
            trigger: orderDate;
            affectedFields: [orderDate];
            conditions: [
                "orderDate == null"
            ];
            exception: "CANNOT_SET_ORDER_DATE";
        }
        
        rule "PREVENT_STATUS_CHANGE_AFTER_DELIVERY" {
            trigger: status;
            affectedFields: [status, deliveryDate];
            conditions: [
                "prev.getDeliveryDate() != null",
                "prev.getStatus().equals('DELIVERED')"
            ];
            exception: "CANNOT_CHANGE_DELIVERED_ORDER";
        }
    }
}
```

### 5. Methods

Methods define business operations on entities:

#### Simple Method Declarations
```nebula
Root Entity Order {
    methods {
        method createOrder(String orderNumber, Customer customer, UnitOfWork unitOfWork): Order;
        method addItem(OrderItem item, UnitOfWork unitOfWork): void;
        method calculateTotal(): BigDecimal;
        method cancel(UnitOfWork unitOfWork): void;
    }
}
```

#### Methods with Custom Implementation
```nebula
Root Entity Order {
    methods {
        method addItem(OrderItem item) {
            "if (this.status.equals('COMPLETED')) {
                throw new ProjectException(CANNOT_MODIFY_COMPLETED_ORDER, getAggregateId());
            }
            this.orderItems.add(item);
            item.setOrder(this);
            updateTotalAmount();"
        }
        
        method findItem(String productId): OrderItem {
            "return this.orderItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);"
        }
    }
}
```

### 6. Constructors

Define custom constructors for entities:

```nebula
Entity Customer {
    String name;
    String email;
    String address;
    
    constructor Customer(UserDto userDto) {
        "setName(userDto.getName());
        setEmail(userDto.getEmail());
        setAddress(userDto.getAddress());"
    }
}
```

### 7. Service Methods

Define service-level operations outside of entities:

```nebula
Aggregate OrderManagement {
    Root Entity Order { /* ... */ }
    
    method getOrdersByCustomer(Integer customerId, UnitOfWork unitOfWork): List<Order>;
    method getOrdersByDateRange(LocalDate startDate, LocalDate endDate, UnitOfWork unitOfWork): List<Order>;
    method searchOrdersByStatus(String status, UnitOfWork unitOfWork): List<Order>;
}
```

### 8. Saga Workflows

Define distributed transaction workflows for causal-saga architecture:

```nebula
Aggregate OrderManagement {
    saga workflow processPayment(Integer orderId, BigDecimal amount, UnitOfWork unitOfWork);
    saga workflow updateInventory(Integer orderId, List<OrderItem> items, UnitOfWork unitOfWork);
    saga workflow sendNotification(Integer orderId, String customerEmail, UnitOfWork unitOfWork);
    saga workflow cancelOrder(Integer orderId, String reason, UnitOfWork unitOfWork);
    saga workflow refundOrder(Integer orderId, BigDecimal amount, UnitOfWork unitOfWork);
}
```

### 9. Custom Repositories

Define custom database queries:

```nebula
Aggregate OrderManagement {
    CustomRepository {
        Optional<Integer> findOrderIdByOrderNumber(String orderNumber);
        List<Order> findOrdersByCustomerAndStatus(Integer customerId, String status);
        BigDecimal calculateTotalRevenueByDateRange(LocalDate startDate, LocalDate endDate);
        Set<Integer> findAllActiveOrderIds();
    }
}
```

### 10. Services with Annotations

Define service classes with specific behaviors:

```nebula
Aggregate OrderManagement {
    Service OrderService {
        @GenerateCrud;
        @Transactional;
        methods {
            createOrder(Order order, UserDto user): Order;
            findOrderById(Integer id): Order;
            updateOrderStatus(Integer id, String status): Order;
            deleteOrder(Integer id): void;
            findOrdersByCustomer(Integer customerId): List<Order>;
        }
    }
}
```

### 11. Web API Endpoints

Define REST API endpoints:

```nebula
Aggregate OrderManagement {
    WebAPIEndpoints {
        Endpoint createOrder {
            httpMethod: POST
            path: "/orders/create"
            methodName: createOrder
            parameters: [
                customerId: Integer: "@RequestParam",
                orderDto: Order: "@RequestBody"
            ]
            returnType: Order
            desc: "Create a new order"
            throwsException: true
        }
        
        Endpoint getOrdersByCustomer {
            httpMethod: GET
            path: "/customers/{customerId}/orders"
            methodName: getOrdersByCustomer
            parameters: [
                customerId: Integer: "@PathVariable"
            ]
            returnType: List<Order>
            desc: "Get all orders for a specific customer"
            throwsException: false
        }
    }
}
```

### 12. Metadata Annotations

Control code generation features:

```nebula
@Metadata {
    requiresUserDto;
    requiresAggregateState;
    hasEventService;
    hasTransactions;
    hasJpa;
    generateValidation;
    generateSwagger;
}
```

### 13. Imports

Reference other aggregates:

```nebula
import User;
import Course;
import Topic;

Aggregate Quiz {
    Entity QuizCreator {
        Integer userAggregateId;
        String userName;
        String userEmail;
    }
}
```

### 14. Exception Definitions

Define custom exception messages:

```nebula
exceptions {
    ORDER_NOT_FOUND: "Order with id %d does not exist";
    INSUFFICIENT_INVENTORY: "Product %s has only %d items available, requested %d";
    CANNOT_MODIFY_COMPLETED_ORDER: "Cannot modify order %d as it is already completed";
    INVALID_ORDER_STATUS_TRANSITION: "Cannot change order status from %s to %s";
}

## Step-by-Step Guide: Creating Your First Abstraction

### Step 1: Create Project Structure

Abstractions must be organized within the `dsl/abstractions/` directory. Each project should have its own folder containing the `.nebula` files:

```
dsl/
└── abstractions/
    └── your-project-name/          # Create a folder for your project
        ├── user.nebula            # Your abstraction files
        ├── order.nebula
        └── product.nebula
```

For example, for an e-commerce project:
```
dsl/abstractions/ecommerce/
├── user.nebula
├── order.nebula
└── product.nebula
```

### Step 2: Create a Nebula File

Create a new file with `.nebula` extension within your project folder:

```nebula
// order-management.nebula
Aggregate OrderManagement {
    Root Entity Order {
        String orderNumber;
        LocalDateTime orderDate;
        String status;
        BigDecimal totalAmount;
        Customer customer;
        Set<OrderItem> orderItems;
        
        invariants {
            check orderNumberNotEmpty {
                orderNumber.length() > 0;
            }
            
            check positiveAmount {
                totalAmount > 0;
            }
        }
        
        methods {
            method createOrder(String orderNumber, Customer customer, UnitOfWork unitOfWork): Order;
            method addItem(OrderItem item, UnitOfWork unitOfWork): void;
            method calculateTotal(): BigDecimal;
        }
    }
    
    Entity Customer {
        String name;
        String email;
        String address;
    }
    
    Entity OrderItem {
        String productName;
        Integer quantity;
        BigDecimal unitPrice;
    }
    
    // Service methods
    method getOrdersByCustomer(Integer customerId, UnitOfWork unitOfWork): List<Order>;
    method getOrdersByStatus(String status, UnitOfWork unitOfWork): List<Order>;
}
```

### Step 3: Navigate to the Nebula Directory

```bash
cd dsl/nebula
```

### Step 4: Generate Code

Use the CLI to generate Java microservice code from your project folder:

```bash
# Generate from your project folder
./bin/cli.js generate ../abstractions/your-project-name/

# Or generate a specific file
./bin/cli.js generate ../abstractions/your-project-name/order.nebula
```

## Command Line Interface Reference

### Basic Syntax
```bash
./bin/cli.js generate <input> [options]
```

### Parameters

#### `<input>` (Required)
Path to your `.nebula` file or directory containing multiple `.nebula` files.

**Examples:**
```bash
# Single file
./bin/cli.js generate ../abstractions/answers/user.nebula

# Directory (processes all .nebula files)
./bin/cli.js generate ../abstractions/answers/

# Absolute path
./bin/cli.js generate /Users/username/project/my-domain.nebula
```

### Options

#### `-d, --destination <dir>`
**Default:** `../../applications`
**Description:** Output directory for generated Java code

```bash
# Generate to default location
./bin/cli.js generate user.nebula

# Generate to custom location
./bin/cli.js generate user.nebula -d ./output
./bin/cli.js generate user.nebula --destination /path/to/output
```

#### `-n, --name <name>`
**Description:** Project name for the generated microservice

```bash
./bin/cli.js generate user.nebula --name user-service
```

#### `-a, --architecture <arch>`
**Default:** `default`
**Options:** `default`, `causal-saga`, `external-dto-removal`
**Description:** Architecture pattern to use for code generation

```bash
# Default microservices architecture
./bin/cli.js generate user.nebula --architecture default

# Causal-saga pattern for distributed transactions
./bin/cli.js generate user.nebula --architecture causal-saga

# External DTO removal pattern
./bin/cli.js generate user.nebula --architecture external-dto-removal
```

**Architecture Details:**
- **`default`**: Standard microservices with REST APIs, services, repositories
- **`causal-saga`**: Adds saga coordination, event sourcing, distributed transaction support
- **`external-dto-removal`**: Optimized for external DTO handling and removal patterns

#### `-f, --features <features>`
**Default:** `events,validation,webapi,coordination`
**Description:** Comma-separated list of features to generate

```bash
# Default features
./bin/cli.js generate user.nebula --features events,validation,webapi,coordination

# Only entities and services
./bin/cli.js generate user.nebula --features entities,services

# All features
./bin/cli.js generate user.nebula --features events,validation,webapi,coordination,saga,testing
```

**Available Features:**
- **`entities`**: JPA entities with annotations
- **`services`**: Service layer classes
- **`repositories`**: Repository interfaces
- **`webapi`**: REST controllers and DTOs
- **`events`**: Event handling and publishing
- **`validation`**: Bean validation annotations
- **`coordination`**: Service coordination logic
- **`saga`**: Saga patterns and workflows (requires causal-saga architecture)
- **`testing`**: Unit and integration tests

#### `--validate`
**Description:** Validate DSL files before generation (recommended)

```bash
./bin/cli.js generate user.nebula --validate
```

### Complete Examples

#### Example 1: Simple User Service
```bash
# Generate a basic user service with default settings
./bin/cli.js generate ../abstractions/answers/user.nebula \
    --name user-service \
    --architecture default \
    --features entities,services,webapi,validation
```

#### Example 2: Complex Order Management with Saga
```bash
# Generate order management with full saga support
./bin/cli.js generate order-management.nebula \
    --name order-service \
    --architecture causal-saga \
    --features events,validation,webapi,coordination,saga \
    --destination ./services/order \
    --validate
```

#### Example 3: Multiple Abstractions
```bash
# Process all .nebula files in a directory
./bin/cli.js generate ../abstractions/answers/ \
    --architecture causal-saga \
    --destination ../../applications/generated \
    --validate
```

#### Example 4: Testing and Development
```bash
# Generate with testing support for development
./bin/cli.js generate quiz.nebula \
    --architecture default \
    --features entities,services,webapi,validation,testing \
    --name quiz-service \
    --destination ./dev-output
```

### Generated Output Structure

When you run the generator, it creates a complete Spring Boot microservice:

```
applications/
└── your-service-name/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── pt/ulisboa/tecnico/socialsoftware/
    │   │   │       ├── entities/           # JPA entities
    │   │   │       ├── dtos/               # Data transfer objects
    │   │   │       ├── services/           # Business logic services
    │   │   │       ├── repositories/       # Data access repositories
    │   │   │       ├── controllers/        # REST API controllers
    │   │   │       ├── events/             # Event handling (if enabled)
    │   │   │       ├── sagas/              # Saga coordination (causal-saga only)
    │   │   │       └── config/             # Configuration classes
    │   │   └── resources/
    │   │       ├── application.properties  # Spring configuration
    │   │       └── application.yml         # YAML configuration
    │   └── test/
    │       └── groovy/                     # Spock tests (if testing enabled)
    ├── Dockerfile                          # Docker configuration
    ├── docker-compose.yml                  # Docker Compose setup
    └── pom.xml                            # Maven dependencies
```

### Validation and Error Handling

Always use `--validate` to catch errors early:

```bash
./bin/cli.js generate user.nebula --validate
```

Common validation errors:
- Missing root entity
- Invalid invariant syntax
- Undefined entity references
- Circular dependencies
- Invalid method signatures

### Integration with Existing Projects

To integrate generated code with existing applications:

1. **Generate to temporary directory:**
   ```bash
   ./bin/cli.js generate user.nebula --destination ./temp-output
   ```

2. **Review generated code**

3. **Copy relevant files to your project**

4. **Update Maven/Gradle dependencies from generated `pom.xml`**

## Generated Code Features

### Entities
- JPA annotations (@Entity, @Id, @Column, etc.)
- Property declarations with proper types
- Constructors (default, parameterized, copy)
- Getter/Setter methods
- Invariant validation methods
- Business rule methods
- DTO conversion methods

### DTOs
- Data transfer object classes
- Field declarations
- Constructor generation
- Builder pattern implementation
- Equals/HashCode methods
- toString method

### Services
- Spring service classes (@Service)
- Business method implementations
- Transaction management (@Transactional)
- Dependency injection (@Autowired)
- Event handling methods
- Validation integration

### Repositories
- Spring Data JPA repository interfaces
- Custom query methods
- JPA query annotations (@Query)
- Pagination and sorting support

### REST APIs
- REST controller classes (@RestController)
- HTTP endpoint methods (@GetMapping, @PostMapping, etc.)
- Request/Response DTOs
- Validation annotations
- Error handling methods
- Swagger documentation support

### Event Handling
- Domain event classes
- Event handler classes
- Event publishing methods
- Event subscription methods
- Event processing logic

### Saga Coordination
- Saga coordination classes
- Workflow definition
- Compensation logic
- State management
- Event handling methods

## Project Structure

```
nebula/
├── src/
│   ├── cli/                    # Command line interface
│   │   ├── generator/          # Code generators
│   │   │   ├── core/           # Core utilities and base classes
│   │   │   ├── generators/     # Individual generators
│   │   │   ├── templates/      # Template engine and storage
│   │   │   └── validation/     # Validation generators
│   │   └── main.ts            # Main CLI entry point
│   └── language/              # DSL language definition
│       ├── generated/         # Generated AST and grammar
│       └── nebula-validator.ts # DSL validation
├── docs/                      # Documentation
│   ├── dsl-guide.md          # DSL syntax guide
│   └── api-reference.md      # API reference
├── examples/                  # Example DSL files
└── templates/                 # Code generation templates
```

## Development

### Building

```bash
npm run build
```

### Testing

```bash
npm test
```

### Development Mode

```bash
npm run dev
```

## Configuration

### Global Configuration

```typescript
import { ConfigManager } from './generator/core/config.js';

const config = new ConfigManager({
    projectName: 'myproject',
    architecture: 'causal-saga',
    features: ['entities', 'services', 'webapi'],
    javaVersion: '17',
    springBootVersion: '3.0.0'
});
```

### Architecture Configuration

```typescript
import { ArchitectureConfig } from './generator/core/config.js';

const customArchitecture: ArchitectureConfig = {
    name: 'custom',
    requiredFeatures: ['entities', 'services'],
    optionalFeatures: ['webapi', 'validation'],
    defaultTemplates: {
        entities: 'custom-entity.template',
        services: 'custom-service.template'
    },
    validation: {
        requiresRootEntity: true,
        allowsMultipleRoots: false,
        requiredProperties: ['id'],
        requiredMethods: []
    }
};

config.registerArchitecture(customArchitecture);
```

## Error Handling

### Error Types

- **Validation**: DSL syntax and semantic errors
- **Template**: Template rendering errors
- **File System**: File I/O errors
- **Compilation**: Java compilation errors
- **Configuration**: Configuration errors
- **Architecture**: Architecture compliance errors

### Error Reporting

```typescript
import { ErrorHandler } from './generator/core/error-handler.js';

const errorHandler = new ErrorHandler();

// Add errors
errorHandler.addError('VALIDATION', 'DUPLICATE_NAME', 'Duplicate entity name: User');

// Check for errors
if (errorHandler.hasErrors()) {
    console.error(errorHandler.generateSummary());
}
```

## Logging

### Log Levels

- **DEBUG**: Detailed debugging information
- **INFO**: General information messages
- **WARN**: Warning messages
- **ERROR**: Error messages
- **SILENT**: No logging

### Usage

```typescript
import { Logger, LogLevel } from './generator/core/logger.js';

const logger = new Logger(LogLevel.INFO);
logger.info('Starting code generation');
logger.debug('Processing entity', { entityName: 'User' });
logger.warn('Deprecated feature used');
logger.error('Generation failed', { error: 'Template not found' });
```

## Complete Working Examples

### Example 1: Simple User Management

Here's a complete example from the existing abstractions:

**File: `user.nebula`**
```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }
    
    CustomRepository {
        Optional<Integer> findUserIdByUsername(String username);
    }
}
```

**Generate the code:**
```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/answers/user.nebula --validate
```

**Generated structure:**
```
applications/user/
├── src/main/java/pt/ulisboa/tecnico/socialsoftware/
│   ├── entities/User.java                    # JPA entity
│   ├── dtos/UserDto.java                     # Data transfer object
│   ├── services/UserService.java             # Business service
│   ├── repositories/UserRepository.java      # JPA repository
│   └── controllers/UserController.java       # REST controller
└── pom.xml                                   # Maven configuration
```

### Running the Generated Code

After generation, you can run the microservice:

```bash
# Navigate to generated service
cd ../../applications/user  # or your service name

# Build and run with Maven
mvn clean install
mvn spring-boot:run

# Or with Docker
docker-compose up --build
```

The service will start on `http://localhost:8080` with:
- REST API endpoints
- Swagger documentation at `/swagger-ui.html`
- Database connectivity (H2 for development, configurable)
- Event handling (if causal-saga architecture)

## Troubleshooting and Best Practices

### Common Issues and Solutions

#### 1. "Root entity not found" Error
```
Error: Aggregate must have exactly one root entity
```

**Solution:** Ensure your aggregate has one entity marked as `Root Entity`:
```nebula
Aggregate MyAggregate {
    Root Entity MyRootEntity {  // ← Must have "Root" keyword
        String name;
    }
}
```

#### 2. "Invalid invariant syntax" Error
```
Error: Invalid invariant condition syntax
```

**Solution:** Check invariant syntax - use `check` keyword and proper condition format:
```nebula
invariants {
    check nameNotEmpty {           // ← Use "check" keyword
        name.length() > 0;         // ← Proper condition syntax
    }
}
```

#### 3. "Undefined entity reference" Error
```
Error: Entity 'Customer' is not defined
```

**Solution:** Define all referenced entities within the aggregate:
```nebula
Aggregate Order {
    Root Entity Order {
        Customer customer;         // ← References Customer entity
    }
    
    Entity Customer {              // ← Must be defined in same aggregate
        String name;
    }
}
```

#### 4. "Cannot find CLI" Error
```
bash: ./bin/cli.js: No such file or directory
```

**Solution:** Ensure you're in the correct directory and CLI is built:
```bash
cd dsl/nebula                     # Navigate to nebula directory
npm install                       # Install dependencies
npm run build                     # Build the CLI
./bin/cli.js --help              # Verify CLI works
```

#### 5. "Template not found" Error
```
Error: Template file not found: entity.hbs
```

**Solution:** Rebuild the project to ensure all templates are copied:
```bash
cd dsl/nebula
npm run clean
npm run build
```

### Best Practices

#### 1. Aggregate Design
- **Keep aggregates small and focused** - Each aggregate should represent a single business concept
- **Use meaningful names** - Aggregate and entity names should clearly describe their purpose
- **One root entity per aggregate** - This enforces proper domain boundaries

```nebula
// Good: Focused aggregate
Aggregate OrderManagement {
    Root Entity Order { /* ... */ }
    Entity OrderItem { /* ... */ }
}

// Avoid: Overly broad aggregate
Aggregate EverythingAggregate {
    Root Entity Order { /* ... */ }
    Entity Customer { /* ... */ }
    Entity Product { /* ... */ }
    Entity Invoice { /* ... */ }
}
```

#### 2. Entity Relationships
- **Use composition over inheritance** - Prefer entity references over complex inheritance
- **Define clear boundaries** - Related entities should be in the same aggregate
- **Use meaningful property names** - Avoid abbreviations and use descriptive names

```nebula
// Good: Clear relationships
Root Entity Order {
    String orderNumber;           // Clear, descriptive name
    Customer customer;            // Direct entity reference
    Set<OrderItem> items;         // Collection of related entities
}

Entity Customer {
    String customerName;          // Descriptive property name
    String emailAddress;
}

// Avoid: Unclear relationships
Root Entity Order {
    String ordNum;                // Abbreviated name
    Integer custId;               // ID reference instead of entity
    List<Object> stuff;           // Vague collection type
}
```

#### 3. Business Logic
- **Use invariants for simple validation** - Data consistency rules
- **Use business rules for complex logic** - State transitions and business processes
- **Implement custom methods for complex operations** - When generated methods aren't sufficient

```nebula
Root Entity Order {
    String status;
    LocalDateTime orderDate;
    BigDecimal totalAmount;
    
    // Simple validation
    invariants {
        check positiveAmount {
            totalAmount > 0;
        }
    }
    
    // Complex business logic
    businessRules {
        rule "PREVENT_MODIFICATION_AFTER_SHIPPING" {
            trigger: status;
            affectedFields: [status, totalAmount];
            conditions: ["prev.getStatus().equals('SHIPPED')"];
            exception: "CANNOT_MODIFY_SHIPPED_ORDER";
        }
    }
    
    // Custom implementation
    methods {
        method calculateShipping(): BigDecimal {
            "// Custom shipping calculation logic
            if (totalAmount > 100) return BigDecimal.ZERO;
            return new BigDecimal('9.99');"
        }
    }
}
```

#### 4. Architecture Selection
- **Use `default` for simple microservices** - Standard CRUD operations
- **Use `causal-saga` for distributed systems** - When you need event sourcing and saga patterns
- **Start simple and evolve** - Begin with default architecture, upgrade as needed

```bash
# Start with simple architecture
./bin/cli.js generate order.nebula --architecture default

# Upgrade to saga when needed
./bin/cli.js generate order.nebula --architecture causal-saga --features events,saga,coordination
```

#### 5. Feature Selection
- **Include only needed features** - Avoid generating unnecessary code
- **Always include validation** - Helps catch runtime errors
- **Use testing in development** - Generate test scaffolding for faster development

```bash
# Minimal feature set
./bin/cli.js generate simple.nebula --features entities,services

# Full-featured microservice
./bin/cli.js generate complex.nebula --features events,validation,webapi,coordination,saga,testing
```

#### 6. Development Workflow
1. **Start with DSL validation**
   ```bash
   ./bin/cli.js generate myservice.nebula --validate
   ```

2. **Generate to temporary directory first**
   ```bash
   ./bin/cli.js generate myservice.nebula --destination ./temp --validate
   ```

3. **Review generated code before integration**

4. **Use version control** - Commit your `.nebula` files and track changes

5. **Iterate incrementally** - Make small changes and regenerate frequently

#### 7. Testing Generated Code
```bash
# Generate with testing support
./bin/cli.js generate myservice.nebula --features entities,services,webapi,validation,testing

# Navigate to generated service
cd ../../applications/myservice

# Run tests
mvn test

# Run integration tests
mvn verify
```

#### 8. Performance Considerations
- **Use appropriate collection types** - `Set` for uniqueness, `List` for ordering
- **Consider database indexes** - Custom repository methods may need indexes
- **Monitor saga performance** - Causal-saga architecture has additional overhead

```nebula
Root Entity Order {
    Set<OrderItem> items;         // Use Set for unique items
    List<StatusHistory> history;  // Use List for ordered history
}

CustomRepository {
    // Consider database indexes for these queries
    List<Order> findOrdersByCustomerAndDateRange(Integer customerId, LocalDate start, LocalDate end);
}
```

## Current Limitations and Missing Features

While Nebula DSL provides comprehensive code generation capabilities, there are some limitations and features that are currently under development or not yet implemented:

### Language Features
- **Inheritance Support**: Entity inheritance and abstract entities are not currently supported
- **Value Objects**: Dedicated value object types are not yet implemented
- **Enums**: Enumeration types need to be defined as String properties with validation
- **Complex Relationships**: Many-to-many relationships require manual implementation
- **Computed Properties**: Calculated fields must be implemented in custom methods

### Architecture Support
- **Event Sourcing**: Full event sourcing patterns are partially implemented in causal-saga architecture
- **CQRS**: Command Query Responsibility Segregation patterns require manual implementation
- **Message Queues**: Integration with external message brokers is not automatically generated
- **Distributed Caching**: Cache configuration and management is not included

### Code Generation
- **Frontend Generation**: Only backend Java code is generated; frontend applications must be built separately
- **API Documentation**: Swagger/OpenAPI documentation generation is basic and may need manual enhancement
- **Database Migrations**: Schema migration scripts are not automatically generated
- **Integration Tests**: Generated tests are limited and may require additional test scenarios

### Tooling
- **IDE Integration**: Advanced IDE features like syntax highlighting and auto-completion are limited
- **Debugging Support**: Debugging generated code requires understanding of the template system
- **Performance Monitoring**: Generated code does not include built-in monitoring or metrics
- **Deployment Automation**: CI/CD pipeline generation is not included

### Development Workflow
- **Hot Reload**: Changes to DSL files require full regeneration; incremental updates are not supported
- **Code Customization**: Customizing generated code without losing changes on regeneration requires careful planning
- **Version Management**: Managing different versions of abstractions and their generated code needs manual coordination

These limitations are being addressed in future releases. Contributions and feedback are welcome to help prioritize development efforts.