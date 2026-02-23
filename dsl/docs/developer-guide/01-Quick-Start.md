# Quick Start Guide

Get up and running with Nebula DSL in 10 minutes.

## Prerequisites

Ensure you have the following installed:

```bash
node --version    # v18.0.0 or higher
npm --version     # 9.0.0 or higher
java --version    # Java 21 or higher
mvn --version     # Maven 3.9.9 or higher
```

## Step 1: Install Dependencies

Navigate to the Nebula DSL directory and install Node.js dependencies:

```bash
cd dsl/nebula
npm install
```

**Expected output:**
```
added 247 packages, and audited 248 packages in 15s
```

## Step 2: Build the DSL

Generate the Langium parser from the grammar and compile TypeScript:

```bash
npm run langium:generate
npm run build
```

**Expected output:**
```
Langium generator called
  Build succeeded
```

**What happens:**
1. `npm run langium:generate` - Reads `src/language/nebula.langium` and generates parser in `src/language/generated/`
2. `npm run build` - Compiles TypeScript to JavaScript in `out/` directory and copies templates

## Step 3: Verify CLI

Test that the CLI is working:

```bash
./bin/cli.js --help
```

**Expected output:**
```
Usage: cli [options] <command>

Options:
  -V, --version       output the version number
  -h, --help          display help for command

Commands:
  generate [options]  Generate code from Nebula abstractions
```

## Step 4: Generate Code

Generate Spring Boot code from the Answers abstractions:

```bash
./bin/cli.js generate ../abstractions/answers/
```

**Expected output:**
```
╔══════════════════════════════════════════════════════════════════╗
║           Nebula Code Generation - Aggregate Discovery           ║
╚══════════════════════════════════════════════════════════════════╝

📁 Searching for .nebula files in: /path/to/abstractions/answers
✓ Found 9 .nebula files

📦 Discovered Aggregates:
  • Answer
  • Course
  • Execution
  • Question
  • Quiz
  • Topic
  • Tournament
  • User
  • SharedEnums

╔══════════════════════════════════════════════════════════════════╗
║                    Starting Code Generation                      ║
╚══════════════════════════════════════════════════════════════════╝

✓ Answer - Generated 23 files
✓ Course - Generated 18 files
✓ Execution - Generated 21 files
...

╔══════════════════════════════════════════════════════════════════╗
║                    Generation Complete                           ║
╚══════════════════════════════════════════════════════════════════╝

📊 Summary:
  • Total Aggregates: 9
  • Total Files Generated: 187
  • Output Directory: /path/to/applications/answers
  • Generation Time: 18.4s
```

## Step 5: Verify Generated Code

Check that files were generated:

```bash
ls -R ../../applications/answers/src/main/java/pt/ulisboa/tecnico/socialsoftware/answers/
```

You should see:
```
microservices/
├── user/
│   ├── aggregate/
│   │   ├── User.java
│   │   ├── UserFactory.java
│   │   └── UserRepository.java
│   ├── service/
│   │   └── UserService.java
│   └── events/
│       └── publish/
│           └── UserDeletedEvent.java
├── course/
│   ├── aggregate/
│   ...
coordination/
├── functionalities/
│   └── UserFunctionalities.java
├── webapi/
│   └── UserController.java
└── eventProcessing/
    └── UserEventProcessing.java
shared/
├── dtos/
│   └── UserDto.java
└── enums/
    └── UserRole.java
sagas/
└── coordination/
    └── user/
        ├── CreateUserFunctionalitySagas.java
        └── DeleteUserFunctionalitySagas.java
```

## Step 6: Build Simulator Framework

Before compiling generated code, build the Microservices Simulator framework:

```bash
cd ../../../simulator
mvn clean install -DskipTests
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.2 s
```

This installs the simulator framework to your local Maven repository (`~/.m2/repository`).

## Step 7: Compile Generated Code

Compile the generated Spring Boot application:

```bash
cd ../applications/answers
mvn clean compile
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 12.3 s
```

If compilation succeeds, the generated code is valid!

## Step 8: Run Tests (Optional)

Run the generated application's tests:

```bash
mvn clean -Ptest-sagas test
```

**Expected output:**
```
Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Step 9: Run Application (Optional)

Start the application with Saga coordination:

```bash
mvn clean -Psagas spring-boot:run
```

**Expected output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.3)

AnswersSimulator       : Started AnswersSimulator in 8.742 seconds
```

The application is now running on `http://localhost:8080`.

## Step 10: Make Changes and Regenerate

Let's modify the DSL and regenerate:

### Modify DSL

Edit `dsl/abstractions/answers/user.nebula` and add a property:

```nebula
Aggregate User {
    @GenerateCrud
    Root Entity User {
        String name
        String username
        final UserRole role
        Boolean active
        String email          // ← Add this line
    }
}
```

### Regenerate Code

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/answers/
```

### Verify Changes

Check that the new field was added:

```bash
grep -n "email" ../../applications/answers/src/main/java/pt/ulisboa/tecnico/socialsoftware/answers/microservices/user/aggregate/User.java
```

**Expected output:**
```
28:    private String email;
62:    public String getEmail() {
66:    public void setEmail(String email) {
```

The `email` field is now part of the entity, DTO, and all CRUD operations!

## Development Workflow

Now that you're set up, here's the typical development cycle:

### Watch Mode (Optional)

For continuous development, use watch mode to automatically rebuild when grammar changes:

```bash
cd dsl/nebula
npm run watch
```

This monitors `src/language/nebula.langium` and automatically runs `langium:generate` and `build`.

### Modify → Regenerate → Test

1. **Modify DSL**: Edit `.nebula` files in `dsl/abstractions/`
2. **Regenerate**: Run `./bin/cli.js generate ../abstractions/myproject/`
3. **Test**: Run `mvn clean -Ptest-sagas test` in the generated application

### Modify Generator → Build → Regenerate

1. **Modify generator**: Edit TypeScript files in `dsl/nebula/src/cli/generators/`
2. **Build**: Run `npm run build`
3. **Regenerate**: Run `./bin/cli.js generate ...`
4. **Verify**: Check generated code

### Modify Grammar → Generate Parser → Build → Regenerate

1. **Modify grammar**: Edit `dsl/nebula/src/language/nebula.langium`
2. **Generate parser**: Run `npm run langium:generate`
3. **Build**: Run `npm run build`
4. **Regenerate**: Run `./bin/cli.js generate ...`
5. **Verify**: Check generated code

## Common Commands Reference

### DSL Development

```bash
cd dsl/nebula

npm install                 # Install dependencies
npm run langium:generate   # Generate parser from grammar
npm run build              # Compile TypeScript
npm run watch              # Watch mode for grammar changes
npm run langium:watch      # Watch mode (grammar only)

./bin/cli.js generate <path>                    # Generate code
./bin/cli.js generate <path> -o <output>        # Custom output
```

### Generated Application

```bash
cd applications/answers

mvn clean compile                  # Compile generated code
mvn clean -Psagas test            # Run Saga tests
mvn clean -Ptcc test              # Run TCC tests
mvn clean -Psagas spring-boot:run # Run Saga application
mvn clean -Ptcc spring-boot:run   # Run TCC application
```

### Simulator Framework

```bash
cd simulator

mvn clean install                  # Install to local Maven repo
mvn clean install -DskipTests     # Install without running tests
```

## Directory Navigation Quick Reference

```
/thesis/microservices-simulator/
├── dsl/nebula/                     ← DSL implementation
│   ├── src/language/               ← Grammar files
│   ├── src/cli/                    ← Generators
│   └── bin/cli.js                  ← CLI
├── dsl/abstractions/               ← DSL source files
│   ├── answers/                    ← Answers project
│   └── teastore/                   ← TeaStore project
├── applications/                   ← Generated code
│   ├── answers/                    ← Generated from answers/
│   └── teastore/                   ← Generated from teastore/
└── simulator/                      ← Framework library
```

## Troubleshooting

### Build Fails with "Cannot find module"

**Problem:** TypeScript compiler can't find imports.

**Solution:**
```bash
cd dsl/nebula
npm install
npm run build
```

### Generation Fails with "No .nebula files found"

**Problem:** Path to abstractions is incorrect.

**Solution:** Use absolute or relative path:
```bash
./bin/cli.js generate /absolute/path/to/abstractions/answers/
./bin/cli.js generate ../abstractions/answers/
```

### Generated Code Won't Compile

**Problem:** Simulator framework not installed.

**Solution:**
```bash
cd simulator
mvn clean install -DskipTests
```

### Changes Not Reflected in Generated Code

**Problem:** Forgot to rebuild DSL after modifying generators.

**Solution:**
```bash
cd dsl/nebula
npm run build
./bin/cli.js generate ../abstractions/answers/
```

### Grammar Changes Not Taking Effect

**Problem:** Parser not regenerated.

**Solution:**
```bash
cd dsl/nebula
npm run langium:generate
npm run build
```

## What's Next?

Now that you can generate code, dive deeper:

- **[02-Project-Structure](02-Project-Structure.md)** - Understand the codebase organization
- **[03-Grammar-Overview](03-Grammar-Overview.md)** - Learn the Nebula DSL syntax
- **[04-DSL-Features](04-DSL-Features.md)** - Explore DSL capabilities

Or jump to practical guides:

- **[09-Adding-DSL-Features](09-Adding-DSL-Features.md)** - Extend the grammar
- **[10-Adding-Generators](10-Adding-Generators.md)** - Create new generators

---

**Previous:** [00-Introduction](00-Introduction.md) | **Next:** [02-Project-Structure](02-Project-Structure.md)
