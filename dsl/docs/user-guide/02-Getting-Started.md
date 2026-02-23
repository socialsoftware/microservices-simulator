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
Usage: cli [options] <command>

Commands:
  generate [options]  Generate code from Nebula abstractions
```

## Abstractions Directory

Nebula reads `.nebula` files from an abstractions directory. Each file typically defines one aggregate:

```
dsl/abstractions/
├── answers/              # Answers case study (9 aggregates)
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
└── teastore/             # TeaStore case study (6 aggregates)
    ├── user.nebula
    ├── category.nebula
    ├── product.nebula
    ├── cart.nebula
    ├── order.nebula
    └── shared-enums.nebula
```

## Generating Code

Generate Spring Boot code from an abstractions directory:

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/answers/
```

Expected output:
```
╔══════════════════════════════════════════════════════╗
║       Nebula Code Generation - Aggregate Discovery   ║
╚══════════════════════════════════════════════════════╝

📁 Searching for .nebula files
✓ Found 9 .nebula files

📦 Discovered Aggregates:
  • Answer, Course, Execution, Question, Quiz, Topic, Tournament, User

╔══════════════════════════════════════════════════════╗
║              Generation Complete                      ║
╚══════════════════════════════════════════════════════╝

📊 Summary:
  • Total Aggregates: 9
  • Total Files Generated: 187
  • Generation Time: 18.4s
```

### CLI Options

```bash
# Generate to default output directory (applications/<project>/)
./bin/cli.js generate ../abstractions/answers/

# Generate to custom output directory
./bin/cli.js generate ../abstractions/answers/ -o ./output

# Generate a specific project
./bin/cli.js generate ../abstractions/teastore/ -o ../../applications/teastore
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
cd applications/answers
mvn clean compile
```

If compilation succeeds, the generated code is valid.

### Run Tests

```bash
# Saga tests
mvn clean -Ptest-sagas test

# TCC tests
mvn clean -Ptest-tcc test
```

### Run the Application

```bash
# Start with Saga coordination
mvn clean -Psagas spring-boot:run

# Start with TCC coordination
mvn clean -Ptcc spring-boot:run
```

The application runs on `http://localhost:8080`.

## Making Changes and Regenerating

### Modify a `.nebula` file

Edit `dsl/abstractions/answers/user.nebula` and add a property:

```nebula
Aggregate User {
    @GenerateCrud
    Root Entity User {
        String name
        String username
        final UserRole role
        Boolean active
        String email          // <-- Add this line
    }
}
```

### Regenerate

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/answers/
```

The `email` field is now part of the entity, DTO, and all CRUD operations.

## Development Workflow Summary

### Modifying abstractions (`.nebula` files)

1. Edit `.nebula` files in `dsl/abstractions/`
2. Regenerate: `./bin/cli.js generate ../abstractions/myproject/`
3. Test: `mvn clean -Ptest-sagas test`

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
cd applications/answers
mvn clean compile                   # Compile generated code
mvn clean -Ptest-sagas test         # Run Saga tests
mvn clean -Ptest-tcc test           # Run TCC tests
mvn clean -Psagas spring-boot:run   # Run Saga application
mvn clean -Ptcc spring-boot:run     # Run TCC application
```

### Simulator Framework

```bash
cd simulator
mvn clean install                   # Install to local Maven repo
mvn clean install -DskipTests       # Install without tests
```

## VSCode Extension

Nebula includes a VSCode extension for syntax highlighting:

```bash
code --install-extension dsl/nebula/nebula-0.0.8.vsix
```

Features: syntax highlighting, bracket matching, comment toggling, error detection.

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

**Previous:** [01-Introduction](01-Introduction.md) | **Next:** [03-DSL-Syntax](03-DSL-Syntax.md)
