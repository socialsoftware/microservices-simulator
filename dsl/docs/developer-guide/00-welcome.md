# Welcome to Nebula DSL ☕

**The Code Generator That Writes Your Microservices (So You Don't Have To)**

---

## What Is This?

Nebula is a Domain-Specific Language (DSL) for generating Spring Boot microservices. You write high-level abstractions that describe your domain, and Nebula generates production-ready Java code.

**The pitch:**
- Write 5 lines of DSL → Get 200 lines of Java
- Define an aggregate → Get entities, DTOs, repositories, services, and REST controllers
- Add events → Get pub/sub infrastructure with handlers
- Need sagas → Get distributed transaction workflows with compensation

If you've ever thought "I wish I never had to write another getter/setter/constructor/repository/controller again," you're in the right place.

---

## Who Is This Guide For?

**You should read this guide if you:**
- ✅ Know Java and Spring Boot (at least the basics)
- ✅ Understand what microservices are (or think you do)
- ✅ Have written REST APIs and are tired of the boilerplate
- ✅ Want to build distributed systems without losing your mind

**This guide assumes you know:**
- What JPA entities and repositories are
- How Spring Boot controllers work
- What a DTO is and why we pretend to like them
- That microservices are hard (if you think they're simple, congratulations - you've never built one)

**This guide does NOT assume you know:**
- Domain-Driven Design (we'll teach you enough)
- Saga patterns (we'll explain when you need them)
- How code generators work (that's our problem, not yours)

---

## Why Use Nebula?

### The Problem

Building microservices involves writing the same patterns over and over:

```java
// Repeat this 47 times across 12 microservices
@Entity
public class Customer {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String email;

    // 40 more lines of constructors, getters, setters, equals, hashCode...
}

// Then write the DTO
public class CustomerDto {
    // Copy-paste all the same fields again
}

// Then write the repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Custom queries here
}

// Then write the service
@Service
public class CustomerService {
    // CRUD methods here
}

// Then write the controller
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    // Map HTTP to service calls
}

// Then realize you need to change ONE field and update 5 files
```

This is what we call "busywork." It's correct, it's necessary, but it's soul-crushing.

### The Nebula Solution

```nebula
Aggregate Customer {
    @GenerateCrud;

    Root Entity Customer {
        String name;
        String email;
    }
}
```

**5 lines of DSL** → **Generates:**
- `Customer.java` (entity with JPA annotations)
- `CustomerDto.java` (DTO with all fields)
- `CustomerFactory.java` (entity ↔ DTO conversion)
- `CustomerRepository.java` (Spring Data interface)
- `CustomerService.java` (5 CRUD methods)
- `CustomerController.java` (5 REST endpoints)
- `CustomerFunctionalities.java` (coordination layer)
- Saga support classes
- Event handling infrastructure

**That's ~800 lines of Java you didn't have to write, debug, or maintain.**

---

## How This Guide Works

### The Story: Building a Coffee Shop API

We'll build a microservices system for a coffee shop. You'll start with customers, add orders, handle inventory, and eventually orchestrate everything with sagas.

Why a coffee shop? Because:
1. It's more interesting than "Foo/Bar/Baz"
2. Everyone understands customers and orders
3. Coffee keeps us awake while writing code generators

### Progressive Learning

Each chapter adds ONE new concept:

| Chapter | You Learn | You Build |
|---------|-----------|-----------|
| **01** | First Aggregate | Customer with name/email |
| **02** | Service Layer | CRUD operations |
| **03** | REST API | Controllers and endpoints |
| **04** | Cross-Aggregate | Orders referencing Customers |
| **05** | Custom Queries | Repository queries with JPQL |
| **06** | Invariants | Business rules validation |
| **07** | Events | Pub/sub for eventual consistency |
| **08** | Sagas | Distributed transactions |
| **09** | Complete System | All 4 aggregates together |

By Chapter 9, you'll have a complete microservices system with events, sagas, and proper aggregate boundaries. All from ~200 lines of DSL.

### Code Examples

Every chapter shows:
- ✅ The DSL abstraction you write
- ✅ The Java code Nebula generates (actual files, not snippets)
- ✅ How to compile and run it
- ✅ Common mistakes and how to avoid them

All generated code examples are in `examples/NN-chapter-name/generated/` so you can reference real files.

---

## Prerequisites

### Required

**Java 21+**
```bash
java -version
# Should show: java version "21.x.x" or higher
```

**Maven 3.9+**
```bash
mvn -version
# Should show: Apache Maven 3.9.x or higher
```

**Node.js 18+** (for the DSL toolchain)
```bash
node --version
# Should show: v18.x.x or higher
```

**Spring Boot Knowledge**
- You should know what `@Entity`, `@Service`, `@RestController` mean
- If you've never written a Spring Boot app, [do that first](https://spring.io/guides/gs/spring-boot/)

**PostgreSQL 14+** (for running generated apps)
- The generator targets PostgreSQL
- Docker is easiest: `docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:14`

### Optional (But Helpful)

- **Domain-Driven Design basics:** Aggregates, entities, value objects
- **Event-driven architecture:** Pub/sub, eventual consistency
- **Distributed transactions:** Two-phase commit, saga pattern

Don't panic if you don't know these - we'll teach you enough to be dangerous.

---

## Setup

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd microservices-simulator
```

### Step 2: Build the Simulator Framework

The generated code depends on the simulator framework. Build it once:

```bash
cd simulator
mvn clean install
```

This installs the framework JAR to your local Maven repository.

### Step 3: Build the DSL Toolchain

```bash
cd ../dsl/nebula
npm install
npm run langium:generate && npm run build
```

**What these commands do:**
- `npm install` - Downloads dependencies
- `langium:generate` - Generates the parser from the grammar
- `npm run build` - Compiles TypeScript, bundles templates

**When to re-run:**
- After pulling new changes
- After modifying the DSL grammar (you probably won't)
- If generation mysteriously breaks (try rebuilding first)

### Step 4: Verify Installation

```bash
./bin/cli.js --version
```

If you see a version number, you're ready! If not, check that Node.js is installed and the build succeeded.

---

## Your First Generation (30 Seconds)

Let's generate code from an existing example to verify everything works:

```bash
# From dsl/nebula/
./bin/cli.js generate ../abstractions/answers/

# Generated code appears in ../../applications/answers/
# Let's compile it
cd ../../applications/answers
mvn clean compile
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Compiled 349 source files
```

**If it fails:**
- Check that simulator was built (`mvn clean install` in simulator/)
- Check for Java version issues (need 21+)
- Check Maven settings

**If it succeeds:** 🎉 You just generated a complete microservices system with 9 aggregates, event handling, and saga workflows. You didn't write a single line of Java.

---

## How to Use This Guide

### If You're New to Nebula
Start with **Chapter 01** and work sequentially. Each chapter builds on the previous one.

### If You're Experienced
Jump to the chapter you need:
- **Chapter 04** - Cross-aggregate references (`uses dto` syntax)
- **Chapter 06** - Invariants (business rules)
- **Chapter 07** - Events (pub/sub)
- **Chapter 08** - Sagas (distributed transactions)

### If You're Stuck
- Check **Common Mistakes** sections in each chapter
- Look at generated code in `examples/*/generated/`
- Compare with working examples in `dsl/abstractions/answers/`
- File an issue if it's truly broken (we're only human)

---

## What You Won't Learn Here

This guide focuses on **using** Nebula, not **extending** it. We don't cover:
- How to modify the DSL grammar
- How to write custom generators
- How code generation works internally
- How to add new language features

If you need that, check the [architecture docs](../../README.md) and the source code.

---

## A Word on Humor

This guide contains occasional jokes. They're subtle (we hope). If you don't find them funny, that's okay - the code will still work.

**Example:**
> "This creates 47 lines of code you'll never debug at 2 AM. That's the real value proposition."

If this made you smile: great! If not: the technical content is still solid.

We believe documentation can be both correct AND readable. Life's too short for dry manuals.

---

## Ready?

Let's build a coffee shop. ☕

**Next: [Chapter 01 - Your First Aggregate](01-first-aggregate.md)**

---

## Quick Reference

### CLI Commands
```bash
# Generate from abstractions folder
./bin/cli.js generate <path-to-abstractions>/

# Generate to custom output directory
./bin/cli.js generate <path-to-abstractions/> -o <output-dir>

# Check version
./bin/cli.js --version
```

### Project Structure
```
dsl/
├── abstractions/          # Your .nebula files go here
│   └── coffee-shop/       # Example: your project folder
│       ├── customer.nebula
│       └── nebula.config.json
├── nebula/                # DSL toolchain (don't touch unless you know why)
│   ├── bin/cli.js         # The generator
│   └── src/               # Generator source code
└── docs/
    └── developer-guide/   # You are here

applications/              # Generated code appears here
└── coffee-shop/           # Your generated Spring Boot app
    └── src/main/java/     # Generated entities, services, controllers
```

### Getting Help
- **Example abstractions:** `dsl/abstractions/answers/`
- **Generated code examples:** `dsl/docs/developer-guide/examples/`
- **Grammar reference:** `dsl/nebula/src/language/nebula.langium`
- **Issues:** [GitHub Issues](../../issues) (if this is in a repo)

---

**Let's write some DSL.** →
