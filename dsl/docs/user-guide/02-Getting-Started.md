# Getting Started

Get up and running with Nebula DSL in 10 minutes.

## Prerequisites

Ensure you have the following installed:

```bash
node --version    # v18.0.0 or higher
npm --version     # 9.0.0 or higher
java --version    # Java 21 or higher
mvn --version     # Maven 3.9.9 or higher
```

## Building the CLI

Navigate to the Nebula DSL directory and build:

```bash
cd dsl/nebula

# Install dependencies
npm install

# Generate the Langium parser from grammar
npm run langium:generate

# Compile TypeScript and copy templates
npm run build
```

Verify the CLI is working:

```bash
./bin/cli.js --help
```

Expected output:
```
Usage: cli [options] [command]

Commands:
  generate [options] <abstractions-path>  generates Java microservices code from Nebula DSL abstractions
```

## Abstractions Directory

Nebula reads `.nebula` files from an abstractions directory. Each file typically defines one aggregate:

```
dsl/docs/examples/abstractions/
├── 01-helloworld/        # Simplest example, start here (Ch 03)
│   ├── nebula.config.json
│   └── task.nebula
├── 02-typesenums/        # Types and enumerations (Ch 04)
│   ├── nebula.config.json
│   └── contact.nebula
├── 03-businessrules/     # Invariants and repositories (Ch 05)
│   ├── nebula.config.json
│   └── product.nebula
├── 04-crossrefs/         # Cross-aggregate references (Ch 06)
│   ├── nebula.config.json
│   ├── teacher.nebula
│   ├── course.nebula
│   └── enrollment.nebula
├── 05-eventdriven/       # Event publishing and subscribing (Ch 07)
│   ├── nebula.config.json
│   ├── author.nebula
│   └── post.nebula
├── 06-tutorial/          # Library system tutorial (Ch 08)
│   ├── nebula.config.json
│   ├── shared-enums.nebula
│   ├── member.nebula
│   ├── book.nebula
│   └── loan.nebula
└── 07-advanced/          # Advanced patterns (Ch 09)
    ├── nebula.config.json
    ├── shared-enums.nebula
    ├── customer.nebula
    ├── product.nebula
    ├── order.nebula
    ├── invoice.nebula
    └── exceptions.nebula

dsl/abstractions/
├── answers/              # Answers case study (8 aggregates)
│   ├── nebula.config.json
│   ├── user.nebula
│   ├── course.nebula
│   ├── execution.nebula
│   ├── topic.nebula
│   ├── question.nebula
│   ├── quiz.nebula
│   ├── tournament.nebula
│   ├── answer.nebula
│   ├── shared-enums.nebula
│   └── exceptions.nebula
└── teastore/             # TeaStore case study (5 aggregates)
    ├── nebula.config.json
    ├── user.nebula
    ├── category.nebula
    ├── product.nebula
    ├── cart.nebula
    ├── order.nebula
    └── shared-enums.nebula
```

> **Note:** Examples 01-07 map directly to guide chapters 03-09. Each chapter references its tied example with generation commands.

### Project Configuration

Each abstractions directory must contain a `nebula.config.json` file that specifies the base Java package:

```json
{
    "basePackage": "pt.ulisboa.tecnico.socialsoftware",
    "framework": {
        "version": "3.1.0-SNAPSHOT"
    }
}
```

- `basePackage`:the root Java package for all generated code
- `framework.version`:the Microservices Simulator version to use as a Maven dependency

The project name is derived from the abstractions folder name (e.g., `06-tutorial/` produces project name `tutorial`), and the generated Java packages follow the pattern `<basePackage>.<projectName>`.

## Generating Code

Generate Spring Boot code from an abstractions directory. Let's start with the simplest example:

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/01-helloworld/ -o ../docs/examples/generated
```

For a more complete example, try the tutorial project:

```bash
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ../docs/examples/generated
```

Expected output:
```
Starting generation for: ../docs/examples/abstractions/06-tutorial/
Found 4 Nebula files
Validating DSL files... OK

Generating code...
  Book                13 files
  Loan                27 files
  Member              14 files

Generated project files (integration, pom.xml, .gitignore, 1 shared enum)

Code generation completed successfully!
Output: ../docs/examples/generated/06-tutorial
```

### CLI Options

```bash
# Generate tutorial to examples directory
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ../docs/examples/generated

# Generate to custom output directory
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ./output

# Generate a larger project
./bin/cli.js generate ../abstractions/answers/ -o ../../applications/answers
```

## Verifying Generated Code

### Build the Simulator Framework

Before compiling generated code, build the Microservices Simulator framework (required once):

```bash
cd simulator
mvn clean install -DskipTests
```

### Compile Generated Code

```bash
cd dsl/docs/examples/generated/06-tutorial
mvn clean compile
```

If compilation succeeds, the generated code is valid.

### Run the Generated App

The generated project is a standard Spring Boot application. Its `application.properties` is configured for **PostgreSQL** by default, but H2 is also on the classpath as a runtime dependency, so you can use it for quick smoke tests when you don't want to spin up a database.

#### Option A: PostgreSQL (the intended target)

Each project expects a database named after the project itself (e.g. `tutorial_db` for `06-tutorial`, `answers_db` for `answers`).

**1. Start PostgreSQL and create the database**

```bash
# From the repository root
docker compose up -d postgres
docker exec -i postgres psql -U postgres -c "CREATE DATABASE tutorial_db;"
```

**2. Start the app**

```bash
cd dsl/docs/examples/generated/06-tutorial
mvn spring-boot:run
```

This activates the `sagas` Maven profile by default, which sets `spring.profiles.active=sagas,local`, i.e. the saga transaction model with in-process command transport. Equivalent explicit form:

```bash
mvn spring-boot:run -P sagas
```

> **Note:** The generated `application.properties` points at host `postgres` (the docker-compose service name). If you are running the app on your host machine instead of inside docker-compose, override the host and password on the command line:
>
> ```bash
> mvn spring-boot:run -Dspring-boot.run.arguments="\
>     --spring.datasource.url=jdbc:postgresql://localhost:5432/tutorial_db \
>     --spring.datasource.password=postgres"
> ```

When you see `Started TutorialSimulator in N seconds`, the app is ready. You can now hit the generated REST endpoints (see the controllers under `coordination/webapi/`).

#### Option B: H2 (in-memory smoke test, no infra required)

H2 is a tiny embedded SQL database that lives in your JVM's memory. It's useful for verifying the app boots and the bean wiring is correct without installing or starting Postgres. Override the datasource on the command line:

```bash
cd dsl/docs/examples/generated/06-tutorial
mvn spring-boot:run -Dspring-boot.run.arguments="\
    --spring.datasource.url=jdbc:h2:mem:tutorial \
    --spring.datasource.driver-class-name=org.h2.Driver \
    --spring.datasource.username=sa \
    --spring.datasource.password= \
    --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
```

> **H2 is a smoke-test convenience, not a substitute for PostgreSQL.** It's fine for quickly checking that an app boots, but the simulator's saga semantics (transaction isolation, locking, native query support) are written against PostgreSQL. Use H2 to verify wiring; use PostgreSQL for anything you actually want to trust the results of.

### Run Tests

```bash
cd dsl/docs/examples/generated/06-tutorial
mvn test -P test-sagas
```

The `test-sagas` profile activates `test,sagas,local` and enables the Groovy/Spock test compiler. The generated app ships with two Maven profiles only:

| Profile        | `spring.profiles.active` | Purpose                                                  |
| -------------- | ------------------------ | -------------------------------------------------------- |
| `sagas`        | `sagas,local`            | Run the app (saga model + in-process transport, default) |
| `test-sagas`   | `test,sagas,local`       | Compile and run Spock/Groovy tests                       |

> **Nebula does not generate tests for you.** The `test-sagas` profile is plumbing: it sets up the Spock/Groovy compiler so that *your own* tests can run. A freshly generated project contains no test files, so `mvn test -P test-sagas` will succeed with zero tests executed until you add them under `src/test/groovy/`. See the hand-written `applications/quizzes` and `applications/answers` projects for examples of Spock test files you can use as templates.

For distributed transports (`stream`, `grpc`) or microservices-mode deployments, see the [project README](../../../README.md). Those concerns belong to the simulator framework rather than the DSL.

## Making Changes and Regenerating

### Modify a `.nebula` file

Edit `dsl/docs/examples/abstractions/06-tutorial/member.nebula` and add a property:

```nebula
Aggregate Member {
    @GenerateCrud

    Root Entity Member {
        String name
        String email
        MembershipType membership
        String phone              // <-- Add this line
    }
}
```

### Regenerate

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ../docs/examples/generated
```

The `phone` field is now part of the entity, DTO, and all CRUD operations.

## Development Workflow Summary

### Modifying abstractions (`.nebula` files)

1. Edit `.nebula` files in `dsl/abstractions/`
2. Regenerate: `./bin/cli.js generate ../abstractions/myproject/`
3. Compile: `mvn clean compile`

### After DSL grammar changes

If the DSL itself has been updated (grammar or generators), rebuild first:

```bash
cd dsl/nebula
npm run langium:generate   # If grammar changed
npm run build              # Always needed after generator changes
./bin/cli.js generate ../abstractions/myproject/
```

## Common Commands Reference

### DSL

```bash
cd dsl/nebula
npm install                 # Install dependencies
npm run langium:generate    # Generate parser from grammar
npm run build               # Compile TypeScript + copy templates
npm run watch               # Watch mode (auto-rebuild on changes)
./bin/cli.js generate <path>              # Generate code
./bin/cli.js generate <path> -o <output>  # Custom output directory
```

### Generated Application

```bash
cd dsl/docs/examples/generated/06-tutorial   # or applications/answers, etc.
mvn clean compile                   # Compile generated code
```

### Simulator Framework

```bash
cd simulator
mvn clean install                   # Install to local Maven repo
mvn clean install -DskipTests       # Install without tests
```

## VSCode Extension

Nebula includes a VSCode extension that provides language support for `.nebula` files.

### Installing the Extension

Pre-built `.vsix` packages are available in the `dsl/extensions/` directory:

```bash
# Install the latest packaged extension
code --install-extension dsl/extensions/nebula-extension-0.0.2.vsix
```

After installation, restart VSCode. The extension activates automatically when you open a `.nebula` file.

### Features

| Feature | Description |
|---------|-------------|
| **Syntax highlighting** | Keywords, strings, comments, and annotations are color-coded |
| **Error detection** | Invalid syntax and semantic errors are underlined in the editor |
| **Code completion** | Suggestions for keywords, types, and cross-aggregate references |
| **Bracket matching** | Automatic matching of `{}`  |
| **Comment toggling** | `Ctrl+/` toggles line comments |

### Building the Extension from Source

If you need to rebuild the extension (e.g., after grammar changes):

```bash
cd dsl/nebula

# Build and package a new version (bumps patch version automatically)
npm run release

# Or for minor/major version bumps
npm run release:minor
npm run release:major
```

The packaged `.vsix` file is output to `dsl/extensions/`. Install it with:

```bash
code --install-extension dsl/extensions/nebula-extension-<version>.vsix
```

### Verifying the Extension Works

1. Open a `.nebula` file in VSCode
2. Keywords like `Aggregate`, `Entity`, `Root` should be highlighted
3. Syntax errors should appear as red underlines
4. Type an aggregate name after `from` to see cross-file references resolve

For details on the extension architecture and development, see the [Developer Guide: VSCode Extension](../developer-guide/07-VSCode-Extension.md).

## Troubleshooting

### Build Fails with "Cannot find module"

```bash
cd dsl/nebula && npm install && npm run build
```

### Generation Fails with "No .nebula files found"

Use absolute or correct relative path:
```bash
./bin/cli.js generate ../abstractions/answers/
```

### Generated Code Won't Compile

Build the simulator framework first:
```bash
cd simulator && mvn clean install -DskipTests
```

### Grammar Changes Not Taking Effect

Regenerate the parser:
```bash
cd dsl/nebula && npm run langium:generate && npm run build
```

---

**Previous:** [01-Introduction](01-Introduction.md) | **Next:** [03-Your-First-Aggregate](03-Your-First-Aggregate.md)
