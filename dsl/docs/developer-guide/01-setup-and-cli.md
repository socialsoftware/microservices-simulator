# Step 1 · Environment & First Generation

[📚 Guide Index](00-index.md)

This guide walks through Nebula DSL from a developer's perspective. Each step adds another capability to the abstraction and points you to the next page when you are ready.

> **Goal of this step:** get the toolchain working and generate code from the simplest possible abstraction.

## Install & Build Once

Run everything from the repo root:

```bash
cd dsl/nebula
npm install
npm run langium:generate && npm run build
```

- `langium:generate` keeps the generated AST/types aligned with the grammar.
- `npm run build` transpiles the CLI, bundles the VS Code extension, and copies templates into `out/cli/`.

You only need to repeat these commands when you change the DSL or update dependencies.

## Project Layout & Naming

Keep abstractions inside `dsl/abstractions/<project>/`. One aggregate per `.nebula` file—create multiple files when your system needs multiple aggregates.

Example:

```
dsl/
└── abstractions/
    └── demo/
        ├── user.nebula
        └── car.nebula
```

## First CLI Run

From `dsl/nebula/`, run:

```bash
./bin/cli.js generate ../abstractions/demo/
```

This generates all microservice code into `../../applications/demo/`.

### CLI Options

```bash
./bin/cli.js generate <abstractions-path> [options]

Options:
  -o, --output <dir>    Output directory (default: ../../applications)
```

**Examples:**

```bash
# Generate from a folder
./bin/cli.js generate ../abstractions/demo/

# Custom output directory
./bin/cli.js generate ../abstractions/demo/ -o ./output
```

## What Gets Generated

After generation, check `applications/demo/` for:

```
applications/demo/
├── microservices/user/
│   ├── aggregate/          # JPA entities, factories
│   ├── repository/         # Spring Data repositories
│   ├── service/            # Business logic layer
│   └── events/             # Event publishing & handling
├── coordination/
│   ├── functionalities/    # Saga orchestration
│   └── webapi/             # REST controllers
├── sagas/                  # Distributed transaction workflows
├── shared/
│   └── dtos/               # Data transfer objects
├── pom.xml                 # Maven configuration
└── .gitignore
```

Next we will enrich the abstraction with multiple entities, DTO mappings, and cross-aggregate DTO reuse.

➡️ [Next → Step 2 · Entities & DTOs](02-entities-and-dtos.md)
